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

import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.windows.WindowManager;

/**
 * Util class for annotations added for methods
 *
 * @author YADHLK
 */
public class PlsqlMethodAnnotationUtil {

    static int hasReturn = 0;
    private static final int HAS_RETURNS = 1;
    private static final int NO_RETURNS = 2;
    private static Comparator<PlsqlBlock> comparator = new Comparator<PlsqlBlock>() {
        @Override
        public int compare(PlsqlBlock o1, PlsqlBlock o2) {
            Integer o1pos, o2pos;
            if (o1.getStartOffset() > -1 && o2.getStartOffset() > -1) {
                o1pos = new Integer(o1.getStartOffset());
                o2pos = new Integer(o2.getStartOffset());
            } else {
                o1pos = new Integer(o1.getEndOffset());
                o2pos = new Integer(o2.getEndOffset());
            }
            return o1pos.compareTo(o2pos);
        }
    };

    public static int getOffsetToInsert(final Document doc, final int startOffset, final int endOffset) {
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
        int offset = -1;

        if (ts != null) {
            ts.move(startOffset);
            Token<PlsqlTokenId> token = ts.token();
            while (ts.moveNext() && ts.offset() < endOffset) {
                token = ts.token();
                if ((token.id() == PlsqlTokenId.KEYWORD)
                        && (token.text().toString().equalsIgnoreCase("BEGIN"))) {
                    if (ts.moveNext()) {
                        offset = ts.offset();
                    }
                    break;
                }
            }
        }
        return offset;
    }

    public static PlsqlBlock findMatchingMethod(final List<PlsqlBlock> blockHier, final Document source,
            final Document dest, final PlsqlBlock sourceBlock) {
        PlsqlBlock match = null;
        final List<PlsqlBlock> matchList = new ArrayList<PlsqlBlock>();
        PlsqlParserUtil.findMatchingDefBlocks(blockHier, sourceBlock.getName(), matchList);

        //There are several methods with the same name. Have to check the signature
        final List<String> usageParams = PlsqlParserUtil.fetchMethodDefParamTypes(source, sourceBlock.getStartOffset());

        //Take PlsqlBlock one by one and compare the parameters
        for (int x = 0; x < matchList.size(); x++) {
            final PlsqlBlock block = matchList.get(x);
            if (!(block.getParent() != null && sourceBlock.getParent() != null ? block.getParent().getName().equalsIgnoreCase(sourceBlock.getParent().getName()) : true)) {
                continue; //If the parent block name is not the same this is not a match
            }

            final List<String> params = PlsqlParserUtil.fetchMethodDefParamTypes(dest, block.getStartOffset());
            final int defaultNo = PlsqlParserUtil.fetchDefaultParams(dest, block.getStartOffset());
            if (PlsqlParserUtil.compareParameters(usageParams, params, defaultNo)) {
                match = block;
                break;
            }
        }

        return match;
    }

    public static String getMethodSpecification(final Document doc, final PlsqlBlock block) {
        String methodSpec = "";
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(block.getStartOffset());
            Token<PlsqlTokenId> token = ts.token();
            while (ts.moveNext() && block.getEndOffset() > ts.offset()) {
                token = ts.token();
                if (token.text().toString().equalsIgnoreCase("IS")) {
                    break;
                }
                methodSpec = methodSpec + token.toString();
            }
        }
        return methodSpec.trim();
    }

    public static PlsqlBlock findMethod(final List<PlsqlBlock> specBlockHier, final String packageName, final String methodName) {
        PlsqlBlock match = null;
        if (!packageName.equals("")) {
            PlsqlBlock packageBlock = getPackageBody(specBlockHier, PlsqlBlockType.PACKAGE_BODY, packageName);
            if (packageBlock != null) {
                for (int i = 0; i < packageBlock.getChildCount(); i++) {
                    final PlsqlBlock temp = packageBlock.getChildBlocks().get(i);
                    if (temp.getName().equals(methodName)) {
                        match = temp;
                        break;
                    }
                }
            }
        }
        return match;
    }

    public static PlsqlBlock getPackageBody(final List<PlsqlBlock> specBlockHier, final PlsqlBlockType blockType, final String packageName) {
        PlsqlBlock packageBlock = null;
        for (int i = 0; i < specBlockHier.size(); i++) {
            final PlsqlBlock temp = specBlockHier.get(i);
            if (temp.getType() == blockType && temp.getName().equalsIgnoreCase(packageName)) {
                packageBlock = temp;
                break;
            }
        }
        return packageBlock;
    }

    public static int getOffsetToInsert(final Document doc, final List<PlsqlBlock> specBlockHier, final String packageName, final PlsqlBlock searchBlock, final int searchPlace) throws BadLocationException {
        int offset = -1;
        //Get package block
        if (!packageName.equals("")) {
            PlsqlBlock packageBlock = getPackageBody(specBlockHier, PlsqlBlockType.PACKAGE, packageName);

            if (packageBlock != null) {
                for (int i = 0; i < packageBlock.getChildCount(); i++) {
                    final PlsqlBlock temp = packageBlock.getChildBlocks().get(i);
                    if (!temp.getType().equals(PlsqlBlockType.COMMENT) && (temp.getType().equals(PlsqlBlockType.FUNCTION_DEF) || temp.getType().equals(PlsqlBlockType.PROCEDURE_DEF))) {
                        if (temp.getName().contains(searchBlock.getName())) {
                            if (searchPlace == -1) {
                                offset = temp.getStartOffset();
                            } else {
                                offset = packageBlock.getChildBlocks().get(i + 1).getStartOffset() - 1;
                            }
                            break;
                        }
                    } else if (temp.getType().equals(PlsqlBlockType.COMMENT) && searchBlock.getType().equals(PlsqlBlockType.COMMENT)) {

                        //Get block content and check; comments can be merged to one comment block
                        final String text = doc.getText(temp.getStartOffset(), temp.getEndOffset() - temp.getStartOffset());
                        int index = text.indexOf(searchBlock.getName());
                        if (index != -1) {
                            index = text.indexOf("\n", index);
                            if (index != -1) {
                                index = text.indexOf("\n", index + 1);
                                if (index != -1) {
                                    offset = temp.getStartOffset() + index;
                                } else {
                                    offset = temp.getEndOffset();
                                }

                                break;
                            }
                        }
                    }
                }

                //If the comment is not found insert some where
                if (offset == -1) {
                    offset = packageBlock.getChildBlocks().get(0).getEndOffset();
                }
            }
        }

        return offset;
    }

    public static boolean changeParam(final Document doc, final int offset, final String methodName) {
        if (PlsqlAnnotationUtil.isFileReadOnly(doc)) {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "File is read-only", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(offset);
            ts.movePrevious();
            Token<PlsqlTokenId> token = ts.token();
            while (token.id() == PlsqlTokenId.WHITESPACE && ts.movePrevious()) {
                token = ts.token();
            }
            //We have the token now
            if (PlsqlFileAnnotationUtil.changeLineOfOffset(doc, ts.offset(), token.toString(), "'" + methodName + "'")) {
                return true;
            }
        }
        return false;
    }

    public static int isReturnExist(final Document doc, final PlsqlBlock block) {
        hasReturn = 0;
        boolean isMissing = isReturn(doc, block);

        if (!isMissing) {
            if (hasReturn == HAS_RETURNS) {
                return HAS_RETURNS;
            } else {
                return NO_RETURNS;
            }
        } else {
            return 0;
        }
    }

    public static boolean isReturn(final Document doc, final PlsqlBlock block) {
        boolean isReturn = false;
        final List<PlsqlBlock> children = block.getChildBlocks();
        Collections.sort(children, comparator);

        int startOffset = findBlockImplStart(doc, block);
        for (PlsqlBlock child : children) {
            if (child.getType() != PlsqlBlockType.CURSOR
                    && child.getType() != PlsqlBlockType.CUSTOM_FOLD
                    && startOffset < child.getStartOffset()) {
                if (!isReturnMissing(startOffset, child.getStartOffset(), doc, true)) {
                    isReturn = true;
                    break;
                }
            }
            startOffset = child.getEndOffset();
        }

        if (!isReturn) {
            isReturn = checkReturnInChildren(children, doc);
        }

        //Return not complete in child blocks, check for default return at the end
        //check for exception block, it is assumed that there will be no child blocks after EXCEPTION
        if (children.size() > 0) {
            startOffset = children.get(children.size() - 1).getEndOffset();
        }

        isReturn = !isReturnMissing(startOffset, block.getEndOffset(), doc, !isReturn);

        return isReturn;
    }

    private static int checkExceptionBlock(final Document doc, final PlsqlBlock block) {

        int startOffset = findBlockImplStart(doc, block);
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(startOffset);
            Token<PlsqlTokenId> token = ts.token();
            while (ts.moveNext() && ts.offset() < block.getEndOffset()) {
                token = ts.token();
                if (token.toString().equalsIgnoreCase("EXCEPTION")) {
                    return ts.offset();
                }
            }
        }
        return 0;
    }

    private static boolean checkReturnInChildren(final List<PlsqlBlock> children, final Document doc) {
        boolean isConstrusctReturn = false;

        for (PlsqlBlock child : children) {
            if ((child.getType() == PlsqlBlockType.IF && child.getName().toUpperCase(Locale.ENGLISH).startsWith("IF"))
                    || (child.getType() == PlsqlBlockType.CASE && child.getName().toUpperCase(Locale.ENGLISH).startsWith("CASE"))) {
                isConstrusctReturn = false;
                isConstrusctReturn = isReturn(doc, child);
            } else if ((child.getType() == PlsqlBlockType.IF && child.getName().toUpperCase(Locale.ENGLISH).startsWith("ELSIF"))
                    || (child.getType() == PlsqlBlockType.IF && child.getName().toUpperCase(Locale.ENGLISH).startsWith("WHEN"))) {
                if (isConstrusctReturn) {
                    isConstrusctReturn = isReturn(doc, child);
                }
            } else if ((child.getType() == PlsqlBlockType.IF || child.getType() == PlsqlBlockType.CASE)
                    && child.getName().equalsIgnoreCase("ELSE")) {
                if (isConstrusctReturn) {
                    isConstrusctReturn = isReturn(doc, child);
                    //If is returning and else if also returning
                    if (isConstrusctReturn) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isReturnMissing(final int startOffset, final int endOffset, final Document doc, boolean isReturnMissing) {
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(startOffset);
            Token<PlsqlTokenId> token = ts.token();
            boolean isException = false;

            while (ts.moveNext() && ts.offset() < endOffset) {
                token = ts.token();

                if (token.toString().equalsIgnoreCase("RETURN")
                        || token.toString().equalsIgnoreCase("RAISE")) {
                    if (moveToReturnEnd(ts, endOffset)) {
                        isReturnMissing = false;
                        hasReturn = HAS_RETURNS;
                    }
                } else if (token.toString().equalsIgnoreCase("ERROR_SYS")
                        || token.toString().equalsIgnoreCase("APPLICATION_SEARCH_SYS")) {
                    if (ts.moveNext()) {
                        token = ts.token();
                        if (token.id() == PlsqlTokenId.DOT) {
                            if (ts.moveNext()) {
                                token = ts.token();
                                if (!token.toString().toLowerCase(Locale.ENGLISH).startsWith("check")) {
                                    if (moveToReturnEnd(ts, endOffset)) {
                                        isReturnMissing = false;
                                        hasReturn = HAS_RETURNS;
                                    }
                                }
                            }
                        }
                    }
                } else if (token.toString().equalsIgnoreCase("EXCEPTION")) {
                    isException = true;
                } else if (token.toString().equalsIgnoreCase("WHEN") && isException) {
                    if (!isReturnMissing) {
                        if (PlsqlParserUtil.getNextNonWhitespace(ts, true)) { //Exception name
                            if (PlsqlParserUtil.getNextNonWhitespace(ts, true)) {
                                token = ts.token();
                                if (token.toString().equalsIgnoreCase("THEN")) {
                                    isReturnMissing = true;
                                }
                            }
                        }
                    } else {
                        //return missing in above WHEN
                        break;
                    }
                }
            }
        }

        return isReturnMissing;
    }

    public static boolean getUnreachableOffsets(final Document doc, final PlsqlBlock block, final List<Integer> lstUnreachable) throws BadLocationException {
        final List<PlsqlBlock> children = block.getChildBlocks();
        Collections.sort(children, comparator);
        boolean isConstrusctReturn = false;
        boolean isChildReturn = false;
        boolean isReturn = false;
        boolean isException = false;
        int startOffset = findBlockImplStart(doc, block);
        for (PlsqlBlock child : children) {
            if (child.getType() != PlsqlBlockType.CURSOR
                    && child.getType() != PlsqlBlockType.CUSTOM_FOLD) {
                if (startOffset < child.getStartOffset()) {
                    if (getUnreachableOffsets(startOffset, child.getStartOffset(), doc, lstUnreachable, isReturn, isException)) {
                        isReturn = true;
                    } else {
                        isReturn = false;
                    }
                }

                isChildReturn = getUnreachableOffsets(doc, child, lstUnreachable);

                if ((child.getType() == PlsqlBlockType.IF && child.getName().toUpperCase(Locale.ENGLISH).startsWith("IF"))
                        || (child.getType() == PlsqlBlockType.CASE && child.getName().toUpperCase(Locale.ENGLISH).startsWith("CASE"))) {
                    isConstrusctReturn = isChildReturn;
                } else if ((child.getType() == PlsqlBlockType.IF && child.getName().toUpperCase(Locale.ENGLISH).startsWith("ELSIF"))
                        || (child.getType() == PlsqlBlockType.IF && child.getName().toUpperCase(Locale.ENGLISH).startsWith("WHEN"))) {
                    if (isConstrusctReturn) {
                        isConstrusctReturn = isChildReturn;
                    }
                } else if ((child.getType() == PlsqlBlockType.IF || child.getType() == PlsqlBlockType.CASE)
                        && child.getName().equalsIgnoreCase("ELSE")) {
                    if (isConstrusctReturn) {
                        isConstrusctReturn = isChildReturn;
                        if (isConstrusctReturn) {
                            isReturn = true;
                        }
                    }
                }
            }
            startOffset = child.getEndOffset();
        }
        if (checkExceptionBlock(doc, block) < startOffset) {
            isException = true;
        }
        isConstrusctReturn = getUnreachableOffsets(startOffset, block.getEndOffset(), doc, lstUnreachable, isReturn, isException);
        if (!isReturn) {
            isReturn = isConstrusctReturn;
        }

        return isReturn;
    }

    private static boolean getUnreachableOffsets(final int startOffset, final int endOffset, final Document doc, final List<Integer> lstUnreachable, boolean isReturn, boolean isException) throws BadLocationException {
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
        int endLineCount = 0;
        String previous = null;
        if (ts != null) {
            ts.move(startOffset);
            Token<PlsqlTokenId> token = ts.token();
            boolean isRaised = false;

            while (ts.moveNext() && ts.offset() < endOffset) {
                token = ts.token();

                if (token.toString().equalsIgnoreCase("RETURN")) {
                    if (moveToReturnEnd(ts, endOffset)) {
                        isReturn = true;
                    }
                } else if (token.toString().equalsIgnoreCase("RAISE")) {
                    if (moveToReturnEnd(ts, endOffset)) {
                        isRaised = true;
                    }
                } else if (token.toString().equalsIgnoreCase("EXCEPTION")) {
                    isException = true;
                    isReturn = false;
                    isRaised = false;
                } else if (token.toString().equalsIgnoreCase("WHEN") && isException) {
                    isReturn = false;
                    isRaised = false;
                    if (PlsqlParserUtil.getNextNonWhitespace(ts, true)) { //Exception name
                        if (PlsqlParserUtil.getNextNonWhitespace(ts, true)) {
                            token = ts.token();
                            if (token.toString().equalsIgnoreCase("THEN") || token.toString().equalsIgnoreCase("OR")) {
                                isReturn = false;
                                isRaised = false;
                                endLineCount = 0;
                            }
                        }
                    }
                } else if ((isReturn || isRaised) && token.toString().contains("\n")) {
                    endLineCount++;
                } else if (endLineCount > 0 && token.toString().toUpperCase(Locale.ENGLISH).contains("END") || isRaised) {
                    endLineCount = 0;
                } else if (endLineCount > 0 && token.toString().contains(";")) {
                    lstUnreachable.add(ts.offset());
                }

                if (token.toString().contains(";") && (previous != null && previous.equals("END"))) {
                    isException = false;
                    isReturn = false;
                    isRaised = false;
                }

                previous = token.toString();
            }
        }
        return isReturn;
    }

    private static int findBlockImplStart(final Document doc, final PlsqlBlock block) {
        int startOffset = block.getStartOffset();
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(startOffset);
            Token<PlsqlTokenId> token = ts.token();
            while (ts.moveNext() && ts.offset() < block.getEndOffset()) {
                token = ts.token();

                if (token.toString().equalsIgnoreCase("BEGIN")) {
                    startOffset = ts.offset();
                    break;
                }
            }
        }

        return startOffset;
    }

    private static boolean moveToReturnEnd(final TokenSequence<PlsqlTokenId> ts, final int endOffset) {
        Token<PlsqlTokenId> token = ts.token();
        while (ts.moveNext() && ts.offset() < endOffset) {
            token = ts.token();
            if (token.toString().equals(";")) {
                return true;
            }
        }

        return false;
    }

    public static PlsqlBlock findMatchingImpl(final List<PlsqlBlock> blockHier, final Document source,
            final Document dest, final PlsqlBlock sourceBlock) {
        PlsqlBlock match = null;
        final List<PlsqlBlock> matchList = new ArrayList<PlsqlBlock>();
        PlsqlParserUtil.findMatchingImplBlocks(blockHier, sourceBlock.getName(), matchList);

        //There are several methods with the same name. Have to check the signature
        final List<String> usageParams = PlsqlParserUtil.fetchMethodDefParamTypes(source, sourceBlock.getStartOffset());

        //Take PlsqlBlock one by one and compare the parameters
        for (int x = 0; x < matchList.size(); x++) {
            final PlsqlBlock block = matchList.get(x);
            if (!(block.getParent() != null && sourceBlock.getParent() != null ? block.getParent().getName().equals(sourceBlock.getParent().getName()) : true)) {
                continue; //If the parent block name is not the same this is not a match
            }
            final List<String> params = PlsqlParserUtil.fetchMethodDefParamTypes(dest, block.getStartOffset());
            final int defaultNo = PlsqlParserUtil.fetchDefaultParams(dest, block.getStartOffset());
            if (PlsqlParserUtil.compareParameters(usageParams, params, defaultNo)) {
                match = block;
                break;
            }
        }

        return match;
    }
}
