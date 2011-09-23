/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.plsql.annotation;

import org.netbeans.modules.plsql.annotation.annotations.PlsqlAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlTokenAnnotation;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Util class for annotations added to the file
 * @author YADHLK
 */
public class PlsqlFileAnnotationUtil {    
    
    public static void getFileAnnotations(final PlsqlAnnotationManager manager, final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, final Document doc, final int startParse, final int endParse, final int change) {
        TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
        boolean isErrorSys = false;
        final Map<Integer, String> errorMsgs = new HashMap<Integer, String>();

        //check whether Error_SYS annotations are there in configuration
        final Set<PlsqlAnnotation> annotations = manager.getConfiguration(Annotation.TOKEN_ERROR_SYS);
        if (annotations != null && annotations.size() > 0) {
            isErrorSys = true;
        }

        if (ts != null) {
            ts.move(startParse);
            Token<PlsqlTokenId> token = ts.token();
            String preText = "";
            String message = "";
            while (ts.moveNext() && ts.offset() < endParse) {
                token = ts.token();
                if (token.id() == PlsqlTokenId.KEYWORD && preText.toString().contains("\n")) {
                    //Call token annotations
                    callTokenAnnotation(manager, annotationsToAdd, doc, token, ts.offset(), endParse, null, Annotation.TOKEN_START_KEYWORD);
                } else if (token.toString().equalsIgnoreCase("ERROR_SYS") && isErrorSys) {
                    int offset = ts.offset();
                    boolean isString = false;
                    //check whether there is a message
                    while (ts.moveNext() && ts.offset() < endParse) {
                        token = ts.token();
                        if (token.toString().equalsIgnoreCase(")") || token.toString().equalsIgnoreCase(";")) {
                            break;
                        } else if (!isString && token.id() == PlsqlTokenId.STRING_LITERAL) {
                            if (token.toString().indexOf(":") != -1) {
                                isString = true;
                                message = token.toString().substring(1, token.toString().length() - 1);
                            } else {
                                break;
                            }
                        } else if (isString && token.toString().equalsIgnoreCase(",")) {
                            break;
                        } else if (isString && token.id() == PlsqlTokenId.STRING_LITERAL) {
                            message = message + token.toString().substring(1, token.toString().length() - 1);
                        }
                    }

                    if (isString) {
                        errorMsgs.put(offset, message);
                    }
                }

                if (token.id() == PlsqlTokenId.WHITESPACE && preText.trim().equals("")) {
                    preText = preText + token.toString();
                } else {
                    preText = token.toString();
                }
            }

            //Update error sys calls set
            manager.resetErrorSysCalls(doc, startParse, endParse, change, errorMsgs);

            //Process ERROR_SYS tokens
            callTokenAnnotation(manager, annotationsToAdd, doc, null, startParse, endParse, manager.getErrorSysCalls(), Annotation.TOKEN_ERROR_SYS);
        }
    }

    public static boolean removeLineOfOffset(final Document doc, final int offset, final String text) {
        if (PlsqlAnnotationUtil.isFileReadOnly(doc)) {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "File is read-only", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            doc.remove(offset, text.length());
            return true;
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }

        return false;
    }

    public static boolean changeLineOfOffset(final Document doc, final int offset, final String removeText, final String insertText) {
        if (PlsqlAnnotationUtil.isFileReadOnly(doc)) {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "File is read-only", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            doc.remove(offset, removeText.length());
            doc.insertString(offset, insertText, null);
            return true;
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        }

        return false;
    }
    
    public static void getBlockAnnotations(final PlsqlAnnotationManager manager, final List<PlsqlBlock> blocks, final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, final Document doc) {
        for (PlsqlBlock block : blocks) {
            if (block.getType() == PlsqlBlockType.PROCEDURE_DEF || block.getType() == PlsqlBlockType.PROCEDURE_IMPL || block.getType() == PlsqlBlockType.FUNCTION_DEF
                    || block.getType() == PlsqlBlockType.FUNCTION_IMPL || block.getType() == PlsqlBlockType.IF || block.getType() == PlsqlBlockType.CURSOR
                    || block.getType() == PlsqlBlockType.STATEMENT ) {
                PlsqlAnnotationUtil.callBlockAnnotations(manager, annotationsToAdd, doc, block, null, null, PlsqlAnnotationManager.annotation.getType(block));
            }

            //check for child comments
             getBlockAnnotations(manager, block.getChildBlocks(), annotationsToAdd, doc);
        }
    }

    public static boolean changeDefineValue(final Document doc, final String define, final String newValue, final int offset) {
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
        if (ts != null) {
            ts.moveStart();
            Token<PlsqlTokenId> token = ts.token();
            while (ts.moveNext() && ts.offset() < offset) {
                token = ts.token();
                if (token.id() == PlsqlTokenId.SQL_PLUS && token.toString().equalsIgnoreCase("DEFINE")) {
                    if (PlsqlParserUtil.getNextNonWhitespace(ts, true)) {
                        token = ts.token();
                        if (token.toString().equalsIgnoreCase(define)) {
                            if (PlsqlParserUtil.getNextNonWhitespace(ts, true)) { // = mark
                                if (PlsqlParserUtil.getNextNonWhitespace(ts, true)) { //value
                                    token = ts.token();
                                    if (PlsqlFileAnnotationUtil.changeLineOfOffset(doc, ts.offset(), token.toString(), newValue)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void callTokenAnnotation(final PlsqlAnnotationManager manager, final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd,
            final Document doc, final Token<PlsqlTokenId> token, final int tokenOffset, final int endParse, final Object obj, final String type) {
        final Set<PlsqlAnnotation> annotations = manager.getConfiguration(type);
        if (annotations != null) {
            for (PlsqlAnnotation temp : annotations) {
                if (temp instanceof PlsqlTokenAnnotation) {
                    ((PlsqlTokenAnnotation) temp).evaluateAnnotation(annotationsToAdd, doc, token, tokenOffset, endParse, obj);
                }
            }
        }
    }

    public static List<Integer> getOtherOffset(final Map<Integer, String> errors, final Integer errorOffset, final String existing, final String value) {
        final List<Integer> offsets = new ArrayList<Integer>();
        for (Integer off : errors.keySet()) {
            if (off != errorOffset && (errors.get(off).equals(value) || errors.get(off).equals(existing))) {
                offsets.add(off);
            }
        }

        return offsets;
    }
}
