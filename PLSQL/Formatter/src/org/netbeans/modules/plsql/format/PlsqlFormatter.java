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
package org.netbeans.modules.plsql.format;

import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.GuardedException;
import org.netbeans.editor.TokenContextPath;
import org.netbeans.editor.TokenID;
import org.netbeans.editor.TokenItem;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.ext.AbstractFormatLayer;
import org.netbeans.editor.ext.ExtFormatSupport;
import org.netbeans.editor.ext.ExtFormatter;
import org.netbeans.editor.ext.FormatSupport;
import org.netbeans.editor.ext.FormatTokenPosition;
import org.netbeans.editor.ext.FormatWriter;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle;

/**
 *
 * @author YaDhLK
 */
public class PlsqlFormatter extends ExtFormatter {

    private boolean isIndentOnly = false;
    private boolean isTyping = false;

    public PlsqlFormatter(Class kitClass) {
        super(kitClass);
    }

    protected void initFormatLayers() {
        addFormatLayer(new PlsqlLayer());
    }

    @Override
    public int indentLine(Document doc, int offset) {
        if (doc instanceof BaseDocument) {
            try {
                BaseDocument bdoc = (BaseDocument) doc;
                int lineStart = Utilities.getRowStart(bdoc, offset);
                int prevLineStart = Utilities.getRowStart(bdoc, offset, -1);
                if (prevLineStart < 0) { // start of the doc
                    prevLineStart = 0;
                }
                reformat(bdoc, prevLineStart, lineStart, false); // reformat the previous line
                int nextLineStart = Utilities.getRowStart(bdoc, prevLineStart, 1);
                if (nextLineStart < 0) { // end of doc
                    nextLineStart = bdoc.getLength();
                }
                int nextLineEnd = Utilities.getRowEnd(bdoc, nextLineStart);
                reformat(bdoc, nextLineStart, nextLineEnd, false); // reformat the current line
                return Utilities.getRowEnd(bdoc, nextLineStart);
            } catch (GuardedException e) {
                java.awt.Toolkit.getDefaultToolkit().beep();

            } catch (BadLocationException e) {
                Utilities.annotateLoggable(e);
            } catch (IOException e) {
                Utilities.annotateLoggable(e);
            }

            return offset;

        }
        return super.indentLine(doc, offset);
    }

    /**
     * Implements the base class's abstract method.
     * Inserts new line at given position and indents the new line with spaces.
     * @param doc the document to work on
     * @param offset the offset of a character on the line
     * @return new offset to place the cursor
     */
    public int indentNewLine(Document doc, int offset) {
        try {
            Element rootElem = doc.getDefaultRootElement();
            // Offset should be valid -> no check for lineIndex -1
            int lineIndex = rootElem.getElementIndex(offset);
            String lineText;
            int whitespaceEndIndex;

            do {
                Element lineElem = rootElem.getElement(lineIndex);
                lineText = doc.getText(lineElem.getStartOffset(),
                        lineElem.getEndOffset() - lineElem.getStartOffset() - 1); // strip ending '\n'

                whitespaceEndIndex = 0;

                while (whitespaceEndIndex < lineText.length()) {
                    // Break on non-whitespace char
                    if (!Character.isWhitespace(lineText.charAt(whitespaceEndIndex))) {
                        lineIndex = 0; // stop outer loop

                        break;
                    }
                    whitespaceEndIndex++;
                }
                lineIndex--; // continue to search for previous non-whitespace line


            } while (lineIndex >= 0);

            lineText = lineText.trim().toUpperCase();
            // To indent the line after keywords
            if ((lineText.startsWith("INSERT"))
                    || (lineText.startsWith("UPDATE"))
                    || (lineText.startsWith("DELETE"))
                    || (lineText.startsWith("MERGER"))
                    || (lineText.startsWith("SELECT"))
                    || (lineText.startsWith("EXCEPTION"))
                    || (lineText.startsWith("THEN"))
                    || (lineText.startsWith("BEGIN"))
                    || (lineText.startsWith("DECLARE"))
                    || (lineText.startsWith("IF"))
                    || (lineText.startsWith("ELSIF"))
                    || (lineText.startsWith("ELSE"))
                    || (lineText.startsWith("$IF"))
                    || (lineText.startsWith("$ELSIF"))
                    || (lineText.startsWith("$ELSE"))
                    || (lineText.startsWith("$THEN"))
                    || (lineText.startsWith("$END"))
                    || (lineText.startsWith("$ERROR"))
                    || (lineText.startsWith("CASE"))
                    || (lineText.startsWith("WHEN"))
                    || (lineText.startsWith("MATCHED"))
                    || (lineText.startsWith("RECORD"))
                    || (lineText.startsWith("LOOP")
                    && (!lineText.startsWith("END")))) {
                whitespaceEndIndex = whitespaceEndIndex + getTabSize();
            }

            char[] spaces = new char[whitespaceEndIndex];

            Arrays.fill(spaces, ' ');
            String nlPlusIndent = "\n" + String.valueOf(spaces);

            // String nlPlusIndent = "\n" + lineText.substring(0, whitespaceEndIndex);

            doc.insertString(offset, nlPlusIndent, null);
            offset += nlPlusIndent.length();
        } catch (BadLocationException ex) {
            // ignore
        }

        return offset;
    }

    /**
     * Format characters when entered
     * @param arg0
     * @param arg1
     * @return
     */
    @Override
    public int[] getReformatBlock(JTextComponent target, String arg1) {
        if (isIndentOnly || arg1.length() != 1) //We dont need to consider spaces
        {
            return null;
        }
        char character = arg1.charAt(0);
        if (Character.isJavaIdentifierPart(character)) //Ignore java identifier characters
        {
            return null;
        }

        int dotPos = target.getCaret().getDot();
        isTyping = true;  //We don't need indentation here
        return new int[]{Math.max(dotPos - arg1.length(), 0), dotPos};
    }

    public class PlsqlLayer extends AbstractFormatLayer {

        public PlsqlLayer() {
            super("plsql-layer");
        }

        protected FormatSupport createFormatSupport(FormatWriter fw) {
            return new PlsqlFormatSupport(fw);
        }

        public void format(FormatWriter fw) {
            Document doc = fw.getDocument();
            DataObject dataObj = null;
            Object obj = doc.getProperty(Document.StreamDescriptionProperty);
            if (obj instanceof DataObject) {
               dataObj = (DataObject) obj;
            }

            if (dataObj == null) {
                return;
            }

            PlsqlBlockFactory blockFactory = dataObj.getLookup().lookup(PlsqlBlockFactory.class);
            try {
//             blockFactory.beforeSave(doc); //we want to do this when formatting a file, but not for every edit!

                //Get indent only value from the preferences
                Preferences prefs = MimeLookup.getLookup(PlsqlEditorKit.MIME_TYPE).lookup(Preferences.class);
                isIndentOnly = prefs.getBoolean("indentOnly", false);

                try {
                    PlsqlFormatSupport plsqlFormatSup = (PlsqlFormatSupport) createFormatSupport(fw);
                    FormatTokenPosition pos = plsqlFormatSup.getFormatStartPosition();

                    final EditorCookie cookie = dataObj.getCookie(EditorCookie.class);
                    JEditorPane pane = getEditorPane(cookie);
                    int startOffset = -1;
                    int lineNo = -1;
                    int colStart = -1;
                    if (pane != null) {
                        startOffset = pane.getCaretPosition();
                        lineNo = NbEditorUtilities.getLine(pane.getDocument(), startOffset, false).getLineNumber();
                        colStart = Utilities.getRowStartFromLineOffset((BaseDocument) pane.getDocument(), lineNo);
                    }

                    //If this is triggered while typing from getReformatBlock method look at the
                    //preceeding word only
                    if (isTyping) {

                        if (pos != null && pos.getToken() != null) {
                            //if a new line indent
                            if (!isIndentOnly) {
                                plsqlFormatSup.formatKeyWords(plsqlFormatSup, pos, blockFactory, startOffset - colStart);
                                if (startOffset != -1 && pane != null) {
                                    pane.setCaretPosition(startOffset);  // set the correct caret position                           
                                }
                            }
                        }
                        isTyping = false;
                        return;
                    }
                    if (!isIndentOnly) {
                        if (startOffset == -1) {
                            plsqlFormatSup.formatLine(plsqlFormatSup, pos, blockFactory);
                        } else {
                            plsqlFormatSup.formatKeyWords(plsqlFormatSup, pos, blockFactory, startOffset - colStart);
                        }
                    }
                    while (pos != null) {
                        // Indent the current line

                        plsqlFormatSup.indentLine(pos);

                        // Goto next line
                        FormatTokenPosition pos2 = plsqlFormatSup.findLineEnd(pos);
                        if (pos2 == null || pos2.getToken() == null) {
                            break;
                        } // the last line was processed

                        pos = plsqlFormatSup.getNextPosition(pos2, javax.swing.text.Position.Bias.Forward);
                        if (pos == pos2) {
                            break;
                        } // in case there is no next position

                        if (pos == null || pos.getToken() == null) {
                            break;
                        } // there is nothing after the end of line

                        FormatTokenPosition fnw = plsqlFormatSup.findLineFirstNonWhitespace(pos);
                        if (fnw != null) {
                            pos = fnw;
                        } else { // no non-whitespace char on the line

                            pos = plsqlFormatSup.findLineStart(pos);
                        }
                    }
                } catch (IllegalStateException e) {
                }

                if (isTyping) //Sometimes indent line while typing return without coming to the next line. This is a temp fix for that
                {
                    isTyping = false;
                }
            } finally {
               
            }
        }
    }

    public class PlsqlFormatSupport extends ExtFormatSupport {

        private FormatWriter formatWriter;

        public PlsqlFormatSupport(FormatWriter formatWriter) {
            super(formatWriter);
            this.formatWriter = formatWriter;
        }

        @Override
        public TokenID getWhitespaceTokenID() {
            return PlsqlTokenContext.WHITESPACE;
        }

        public TokenContextPath getWhitespaceTokenContextPath() {
            return PlsqlTokenContext.contextPath;
        }

        /**
         * Method that will perform the indentation of the line containing the given position
         * @param pos
         * @return
         */
        public FormatTokenPosition indentLine(FormatTokenPosition pos) {
            int indent = 0; // Desired indent

            // Get the first non-whitespace position on the line
            FormatTokenPosition firstNWS = findLineFirstNonWhitespace(pos);
            if (firstNWS != null) { // some non-WS on the line
                // if the firstNWS is a character literal and contains \n, which means it has multiple lines, so indentation should avoid in such lines
                if (firstNWS.getToken() != null && firstNWS.getToken().getTokenID().getNumericID() == PlsqlTokenContext.CHAR_LITERAL_ID && firstNWS.getToken().getImage().contains("\n")) {
                    return pos;
                }

                indent = findIndent(firstNWS.getToken());
            } else { // whole line is WS

                TokenItem token = pos.getToken();

                if (token == null) {
                    token = findLineStart(pos).getToken();
                    if (token == null) { // empty line

                        token = getLastToken();
                    }
                }
                indent = findIndent(token);
            }

            // For indent-only always indent
            return changeLineIndent(pos, indent);

        }

        /**
         * Format the just the keyword to uppercase
         * @param plsqlFormatSup
         * @param pos
         * @param blockFactory
         * @param caretPos
         */
        protected void formatKeyWords(PlsqlFormatSupport plsqlFormatSup, FormatTokenPosition pos, PlsqlBlockFactory blockFactory, int caretPos) {
            TokenItem token = pos.getToken();
            if (token == null) {
                pos = plsqlFormatSup.getPreviousPosition(pos);
                token = pos.getToken();
                if (token == null) {
                    return;
                }
            }

            int offset = token != null ? token.getImage().length() : 0;
            if (caretPos != offset && caretPos != 0) {
                while (token != null && !token.getImage().contains("\n")) {
                    if (token.getNext() == null) {
                        return;
                    } else {
                        token = token.getNext();
                        offset += token.getImage().length();
                    }
                    // second part of the || condition is to get the correct format if the keyword is immediately followed by serveral whitespaces
                    // as if there are whitespaces next to each other, all of them taken as one token. Hence offset calculation gets wrong.
                    if (offset == caretPos || (token.getImage().trim().length() == 0 && (caretPos == offset - token.getImage().length() + 1))) {
                        break;
                    }
                }
            }
            token = token.getPrevious();
            if (token == null) {
                return;
            }
            TokenItem tokenPre = token.getPrevious();
            //to get the \n in the middle
            if (tokenPre != null
                    && token.getTokenID().getNumericID() == PlsqlTokenContext.WHITESPACE_ID
                    && tokenPre.getTokenID().getNumericID() == PlsqlTokenContext.WHITESPACE_ID
                    && tokenPre.getImage().contains("\n")) {
                token = tokenPre.getPrevious();
                if (token == null) {
                    return;
                }
                tokenPre = token.getPrevious();
            }

            //to format the keywords with \n
            if (tokenPre != null
                    && token.getTokenID().getNumericID() == PlsqlTokenContext.WHITESPACE_ID
                    && (tokenPre.getTokenID().getNumericID() == PlsqlTokenContext.KEYWORD_ID
                    || tokenPre.getTokenID().getNumericID() == PlsqlTokenContext.OPERATOR_ID)
                    && token.getImage().contains("\n")) {

                token = token.getPrevious();
            }

            String image = token.getImage();
            String upperCaseImage = image.toUpperCase(Locale.ENGLISH);
            if (((token.getTokenID().getNumericID() == PlsqlTokenContext.KEYWORD_ID)
                    && ((tokenPre == null) || (tokenPre.getTokenID().getNumericID() != PlsqlTokenContext.DOT_ID)) || sqlPlus.contains(upperCaseImage))
                    && !image.equals(upperCaseImage)) {
                try {
                    if (blockFactory != null) {
                        blockFactory.beforeCaseChange();
                    }
                    if (!image.equals(upperCaseImage) && checkUpperCaseAllowed(token)) {
                        if (plsqlFormatSup.canReplaceToken(token)) {
                            plsqlFormatSup.replaceToken(token, token.getTokenID(), token.getTokenContextPath(), image.toUpperCase(Locale.ENGLISH));
                        } else {
                            try {
                                plsqlFormatSup.formatWriter.getDocument().remove(token.getOffset(), image.length());
                                plsqlFormatSup.formatWriter.getDocument().insertString(token.getOffset(), image.toUpperCase(Locale.ENGLISH), null);
                            } catch (BadLocationException exp) {
                            }
                        }
                    }
                } finally {
                    if (blockFactory != null) {
                        blockFactory.afterCaseChange();
                        //plsqlFormatSup.indentLine(pos);
                    }
                }
            }
        }

        /**
         * Format the keywords to uppercase only
         * @param plsqlFormatSup
         * @param pos
         */
        protected void formatLine(PlsqlFormatSupport plsqlFormatSup, FormatTokenPosition pos, PlsqlBlockFactory blockFactory) {
            TokenItem token = pos.getToken();
            if (token == null) {
                token = findLineStart(pos).getToken();
                if (token == null) { // empty line

                    token = getLastToken();
                }
                if (token == null) {
                    return;
                }
            }
            TokenItem tokenPre = token.getPrevious();
            //to get the \n in the middle
            if (tokenPre != null
                    && token.getTokenID().getNumericID() == PlsqlTokenContext.WHITESPACE_ID
                    && tokenPre.getTokenID().getNumericID() == PlsqlTokenContext.WHITESPACE_ID
                    && tokenPre.getImage().contains("\n")) {
                token = tokenPre.getPrevious();
                if (token == null) {
                    return;
                }
                tokenPre = token.getPrevious();
            }

            //to format the keywords with \n
            if (tokenPre != null
                    && token.getTokenID().getNumericID() == PlsqlTokenContext.WHITESPACE_ID
                    && (tokenPre.getTokenID().getNumericID() == PlsqlTokenContext.KEYWORD_ID
                    || tokenPre.getTokenID().getNumericID() == PlsqlTokenContext.OPERATOR_ID)
                    && token.getImage().contains("\n")) {

                token = token.getPrevious();
            }

            while (token != null) {
                String image = token.getImage();
                String upperCaseImage = image.toUpperCase(Locale.ENGLISH);
                if (((token.getTokenID().getNumericID() == PlsqlTokenContext.KEYWORD_ID)
                        && ((tokenPre == null) || (tokenPre.getTokenID().getNumericID() != PlsqlTokenContext.DOT_ID)) || sqlPlus.contains(upperCaseImage))) {
                    try {
                        if (blockFactory != null) {
                            blockFactory.beforeCaseChange();
                        }
                        if (!image.equals(upperCaseImage) && checkUpperCaseAllowed(token)) {
                            if (plsqlFormatSup.canReplaceToken(token)) {
                                plsqlFormatSup.replaceToken(token, token.getTokenID(), token.getTokenContextPath(), image.toUpperCase(Locale.ENGLISH));
                            } else {
                                try {
                                    plsqlFormatSup.formatWriter.getDocument().remove(token.getOffset(), image.length());
                                    plsqlFormatSup.formatWriter.getDocument().insertString(token.getOffset(), image.toUpperCase(Locale.ENGLISH), null);
                                } catch (BadLocationException exp) {
                                }
                            }
                        }
                    } finally {
                        if (blockFactory != null) {
                            blockFactory.afterCaseChange();
                        }
                    }
                }
                tokenPre = token;
                token = token.getNext();
            }
        }

        private boolean checkUpperCaseAllowed(TokenItem token) {
            TokenItem previousToken = getPreviousNonWhiteSpaceToken(token);
            if (previousToken != null && (previousToken.getImage().trim().equalsIgnoreCase("PROCEDURE") || previousToken.getImage().trim().equalsIgnoreCase("FUNCTION") || (previousToken.getImage().trim().equalsIgnoreCase("END") && !(token.getImage().trim().equalsIgnoreCase("IF") || token.getImage().trim().equalsIgnoreCase("$IF") || token.getImage().trim().equalsIgnoreCase("LOOP") || token.getImage().trim().equalsIgnoreCase("CASE"))))) {
                return false;
            } else {
                return true;
            }
        }

        /** Find the indentation for the first token on the line.
         * The given token is also examined in some cases.
         */
        public int findIndent(TokenItem token/*FormatTokenPosition pos*/) {
            int indent = 0; // assign invalid indent

            TokenItem previousNWS = token;//pos.getToken();

            do {
                previousNWS = getPreviousToken(previousNWS);
                if ((previousNWS != null) && (previousNWS.getTokenID() != PlsqlTokenContext.WHITESPACE)) {
                    /*if the previousNWS is a line comment or a block comment, we should get previous non-whitespace token to
                     * indent correctly.
                     */
                    if (previousNWS.getTokenID() == PlsqlTokenContext.LINE_COMMENT || previousNWS.getTokenID() == PlsqlTokenContext.BLOCK_COMMENT) {
                        previousNWS = findLineFirstNonWhitespace(getPosition(previousNWS, 0)).getToken();
                    }
                    break;
                }
            } while (previousNWS != null);

            if (previousNWS != null) {
                //Check whether there is a backward indentation
                int parentIndent = getIndentDiffOnCurrent(previousNWS, token/*pos.getToken()*/);
                if (parentIndent == -9999) {
                    //Get forward indentation difference
                    parentIndent = getIndentDiffOnPrevious(previousNWS, token/*pos.getToken()*/);
                }
                if (!previousNWS.getImage().equals("/")) {
                    indent = getLineIndent(getPosition(previousNWS, 0), true);
                }
                indent = indent + parentIndent;
            }

            if (indent <= 0) {
                indent = 0;
            }

            return indent;
        }

        /**
         * If the token is a PLSQLToken allow modification of whitespaces
         * @param inToken
         * @return
         */
        public boolean canModifyWhitespace(TokenItem inToken) {
            if (inToken.getTokenContextPath() == PlsqlTokenContext.contextPath) {
                return true;
            }

            return false;
        }

        /**
         * Get IS or DECLARE if there is before a BEGIN
         * @param previous
         * @return
         */
        private TokenItem getBeginParent(TokenItem previous) {
            TokenItem tokenTemp = previous;
            TokenItem tokenParent = null;

            while (tokenTemp != null) {
                String imageTmp = tokenTemp.getImage().trim();

                if (imageTmp.equalsIgnoreCase("DECLARE")) {
                    tokenParent = tokenTemp;
                    break;
                } else if (imageTmp.equalsIgnoreCase("IS")) {
                    //Check whether this is CURSOR
                    TokenItem tmp = getIsParent(tokenTemp.getPrevious());
                    if ((tmp.getImage().trim().equalsIgnoreCase("CURSOR"))
                            || (tmp.getImage().trim().equalsIgnoreCase("TYPE"))) {
                        tokenTemp = getPreviousToken(tokenTemp);
                    } else { // if it's not the cursor, first check whether previousToken is a ')', in cases where CURSOR consists of parameters in multiple lines
                        TokenItem previousToken = getPreviousNonWhiteSpaceToken(tokenTemp);
                        if (previousToken != null && previousToken.getTokenID().getNumericID() == PlsqlTokenContext.RPAREN_ID) {
                            FormatTokenPosition cursorStartLine = getMethodStartPosition(previousToken);
                            TokenItem tmpCursorItem = findLineFirstNonWhitespace(cursorStartLine).getToken();
                            if ((tmpCursorItem.getImage().trim().equalsIgnoreCase("CURSOR"))
                                    || (tmpCursorItem.getImage().trim().equalsIgnoreCase("TYPE"))) {
                                tokenTemp = getPreviousToken(tokenTemp);
                            } else { // still it's not the 'CURSOR', and it's 'IS' or "PROCEDURE" or "FUNCTION", it's the parent
                                if (tmp.getImage().trim().equalsIgnoreCase("IS")) {
                                    tokenParent = tokenTemp;
                                    break;
                                } else if (tmp.getImage().trim().equalsIgnoreCase("PROCEDURE") || tmp.getImage().trim().equalsIgnoreCase("FUNCTION")) {
                                    tokenParent = tmp;
                                    break;
                                } else {
                                    tokenTemp = getPreviousToken(tokenTemp);
                                }
                            }
                        } else { // if the token is 'IS' or "PROCEDURE" or "FUNCTION", it should be the parent line
                            if (tmp.getImage().trim().equalsIgnoreCase("IS") || tmp.getImage().trim().equalsIgnoreCase("PROCEDURE") || tmp.getImage().trim().equalsIgnoreCase("FUNCTION")) {
                                tokenParent = tmp;
                                break;
                            } else {
                                tokenTemp = getPreviousToken(tokenTemp);
                            }
                        }
                    }
                } else if (imageTmp.equalsIgnoreCase("BEGIN")) {
                    break;
                } else if (imageTmp.equalsIgnoreCase("END")) {
                    TokenItem nextToken = getNextNonWhiteSpaceToken(tokenTemp);

                    //If this is an end of a function/procedure declaration inside function
                    TokenItem temp = getPreviousToken(tokenTemp);

                    if (temp != null) {
                        tokenTemp = getEndParent(temp);
                        if (tokenTemp != null && nextToken != null) {
                            // check whether this is not just BEGIN/END block. i.e it's part of function/procedure BEGIN
                            if (tokenTemp.getImage().trim().equalsIgnoreCase("BEGIN") && !nextToken.getImage().trim().equals(";")) {
                                temp = getPreviousToken(tokenTemp);

                                if (temp != null) {
                                    tokenTemp = getBeginParent(temp);
                                    tokenTemp = getPreviousToken(tokenTemp);
                                } else {
                                    break;
                                }
                            }
                        }
                    } else {
                        break;
                    }
                } else {
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            return tokenParent;
        }

        /**
         * Get the parent of the END
         * @param previous
         * @return
         */
        private TokenItem getEndParent(TokenItem previous) {
            TokenItem tokenTemp = previous;
            TokenItem tokenTempPre = previous;
            TokenItem tokenParent = null;
            int endNo = 0;

            while (tokenTemp != null) {
                String imageTmp = tokenTemp.getImage().trim();
                String imageTmpPre = tokenTempPre.getImage().trim();
                boolean isParent = false;

                if ((imageTmp.equalsIgnoreCase("IF")
                        && (!imageTmpPre.equalsIgnoreCase(";")))
                        || (imageTmp.equalsIgnoreCase("$IF")
                        && (!imageTmpPre.equalsIgnoreCase(";")))
                        || (imageTmp.equalsIgnoreCase("$ERROR")
                        && (!imageTmpPre.equalsIgnoreCase(";")))
                        || (imageTmp.equalsIgnoreCase("LOOP")
                        && (!imageTmpPre.equalsIgnoreCase(";")))
                        || (imageTmp.equalsIgnoreCase("CASE")
                        && (!imageTmpPre.equalsIgnoreCase(";")))
                        || (imageTmp.equalsIgnoreCase("BEGIN"))) {
                    isParent = true;
                }

                //If this is an end parent and no pending END's to pass, break
                if (isParent && (endNo == 0)) {
                    tokenParent = tokenTemp;
                    break;
                } else {
                    //If we have reached a parent note that
                    if ((isParent) && (!imageTmp.equalsIgnoreCase("EXCEPTION"))) /*Since EXCEPTION block will  come only after BEGIN
                     * Anyway it was here because of the exception block template*/ {
                        --endNo;
                    }
                    //If we have reached another end note that
                    if (imageTmp.equalsIgnoreCase("END") || imageTmp.equalsIgnoreCase("$END")) {
                        ++endNo;
                    }

                    tokenTempPre = tokenTemp;
                    tokenTemp = getPreviousNonWhiteSpaceToken(tokenTemp);
                }
            }

            return tokenParent;
        }

        /**
         * Get the backward indentation for the current token
         * based on the previous token and the current token.
         * @param previousNWS
         * @param token
         * @return indentaion difference
         */
        private int getIndentDiffOnCurrent(TokenItem previousNWS, TokenItem token) {
            int indent = -9999;
            int currentTokenID = token.getTokenID().getNumericID();
            String currentImage = token.getImage().trim();

            if (currentTokenID == PlsqlTokenContext.KEYWORD_ID) {

                if (currentImage.equalsIgnoreCase("WHEN")) {
                    //WHEN can come after a THEN of another WHEN-THEN (used for palette items)
                    TokenItem previousWhen = getPreviousWhenThenBlock(previousNWS);

                    if (previousWhen != null) {
                        indent = getIndentationDiff(previousWhen, previousNWS);
                    }
                } else if (currentImage.equalsIgnoreCase("END") || currentImage.equalsIgnoreCase("$END")) {
                    //END can be ending BEGIN, EXCEPTION, LOOP, IF
                    //need the corresponding one
                    TokenItem endParent = getEndParent(previousNWS);

                    if (endParent != null) {
                        indent = getIndentationDiff(endParent, previousNWS);
                    }
                } else if (currentImage.equalsIgnoreCase("IS")) {
                    //IS can come after a FUNCTION/PROCEDURE/PACKAGE/CURSOR
                    TokenItem isParent = getIsParent(previousNWS);

                    if ((isParent != null) && (!isParent.getImage().equalsIgnoreCase("COMMENT"))) {
                        indent = getIndentationDiff(isParent, previousNWS);
                    } else {
                        indent = getTabSize();
                        //Check whether previous line is a COMMENT declaration
                    }
                } else if (currentImage.equalsIgnoreCase("ELSIF") || currentImage.equalsIgnoreCase("$ELSIF")) {
                    //will come after a IF, get the corresponding IF
                    TokenItem parentIf = getParentIf(previousNWS);

                    if (parentIf != null) {
                        indent = getIndentationDiff(parentIf, previousNWS);
                    }
                } else if (currentImage.equalsIgnoreCase("ELSE") || currentImage.equalsIgnoreCase("$ELSE")) {
                    //will come after a IF, get the corresponding IF
                    TokenItem parent = getParentIf(previousNWS);

                    if (parent == null) {
                        parent = getPreviousWhenThenBlock(previousNWS);
                    }
                    if (parent != null) {
                        indent = getIndentationDiff(parent, previousNWS);
                    }
                } else if (currentImage.equalsIgnoreCase("EXCEPTION")) {
                    //Assumed that an EXCEPTION block will be declared only after BEGIN
               /* Since there can be other BEGIN blocks have to get the corresponding 
                     * block, which is similar to looking for the parent of an END
                     */
                    TokenItem endParent = getEndParent(previousNWS);

                    if ((endParent != null) && (endParent.getImage().trim().equalsIgnoreCase("BEGIN"))) {
                        indent = getIndentationDiff(endParent, previousNWS);
                    }
                } else if (currentImage.equalsIgnoreCase("BEGIN")) {
                    //Can have a IS/DECLARE
               /*It would be enough to look for a IS/DECLARE before the previous 
                     * BEGIN - to ignore BEGIN in side a BEGIN
                     * END - means inside a code block*/
                    TokenItem beginParent = getBeginParent(previousNWS);

                    if (beginParent != null) {
                        indent = getIndentationDiff(beginParent, previousNWS);
                    }
                } else if (currentImage.equalsIgnoreCase("SET")) {
                    //IF parent is an Update need a tab
                    TokenItem beginParent = findLineFirstNonWhitespace(getPosition(previousNWS, 0)).getToken();

                    if ((beginParent != null) && (beginParent.getImage().trim().equalsIgnoreCase("UPDATE"))) {
                        indent = getTabSize();
                    }
                } else if (((currentImage.equalsIgnoreCase("FROM"))
                        || (currentImage.equalsIgnoreCase("INTO")))
                        && (!(previousNWS.getImage().trim().equalsIgnoreCase(">")))) {
                    //IF select which is not SELECT <table> yet
                    TokenItem beginParent = findStatementStart(previousNWS);//findLineFirstNonWhitespace(getPosition(previousNWS, 0)).getToken();
                    if (beginParent == null) {
                        beginParent = findLineFirstNonWhitespace(getPosition(previousNWS, 0)).getToken();
                    }
                    if (beginParent != null) {
                        if (beginParent.getImage().trim().equalsIgnoreCase("FETCH")) {
                            indent = getTabSize();
                        } else if (beginParent.getImage().trim().equalsIgnoreCase("SELECT")) {
                            if (!getPosition(beginParent, 0).equals(getPosition(findLineFirstNonWhitespace(getPosition(beginParent, 0)).getToken(), 0)) && (findLineEnd(getPosition(beginParent, 0))).equals(findLineEnd(findLineFirstNonWhitespace(getPosition(previousNWS, 0))))) {
                                indent = getTabSize();
                            } else {
                                indent = getIndentationDiff(beginParent, previousNWS);
                            }
                        } else {
                            //Get statement start
                            beginParent = findStatementStart(previousNWS);
                            if (beginParent != null && beginParent.getImage().trim().equalsIgnoreCase("SELECT")) {
                                indent = getIndentationDiff(beginParent, previousNWS);
                            }
                        }
                    }
                } else if (currentImage.equalsIgnoreCase("WHERE")) {
                    //Get nearest FROM or SET
                    TokenItem tokenP = getPreviousTokenWithImage(previousNWS, "SET", ";");
                    if (tokenP != null) {
                        indent = getIndentationDiff(tokenP, previousNWS);
                    } else {
                        tokenP = getPreviousTokenWithImage(previousNWS, "FROM", ";");
                        if (tokenP != null) {
                            if (!getPosition(tokenP, 0).equals(getPosition(findLineFirstNonWhitespace(getPosition(tokenP, 0)).getToken(), 0)) && !findLineFirstNonWhitespace(getPosition(tokenP, 0)).getToken().getImage().trim().equalsIgnoreCase("SELECT")) {
                                indent = getTabSize();
                            } else {
                                indent = getIndentationDiff(tokenP, previousNWS);
                            }
                        } else {
                            //Get previous line start keyword
                            tokenP = findStatementStart(previousNWS);
                            if (tokenP != null && tokenP.getImage().trim().equalsIgnoreCase("SELECT")) {
                                //  if (tokenP.equals(findLineFirstNonWhitespace(getPosition(tokenP, 0)).getToken())) {
                                if (!getPosition(tokenP, 0).equals(getPosition(findLineFirstNonWhitespace(getPosition(tokenP, 0)).getToken(), 0)) && (findLineEnd(getPosition(tokenP, 0))).equals(findLineEnd(findLineFirstNonWhitespace(getPosition(previousNWS, 0))))) {
                                    indent = getTabSize();
                                } else {
                                    indent = getIndentationDiff(tokenP, previousNWS);
                                }
                            }
                        }
                    }
                } else if ((currentImage.equalsIgnoreCase("WITH"))
                        || (currentImage.equalsIgnoreCase("FOR"))
                        || (currentImage.equalsIgnoreCase("ORDER"))
                        || (currentImage.equalsIgnoreCase("GROUP"))
                        || (currentImage.equalsIgnoreCase("HAVING"))) {
                    //Get statement start
                    TokenItem beginParent = findStatementStart(previousNWS);
                    if (beginParent != null && beginParent.getImage().trim().equalsIgnoreCase("SELECT")) {
                        indent = getIndentationDiff(beginParent, previousNWS);
                    }
                }
            }

            return indent;
        }

        private TokenItem findStatementStart(TokenItem current) {
            TokenItem previous = current.getPrevious();
            while (previous != null) {
                String image = previous.getImage().trim();
                if (previous.getTokenID().getNumericID() == PlsqlTokenContext.KEYWORD_ID) {
                    if ((image.equalsIgnoreCase("SELECT"))
                            || image.equalsIgnoreCase("INSERT")
                            || image.equalsIgnoreCase("UPDATE")
                            || image.equalsIgnoreCase("MERGE")
                            || image.equalsIgnoreCase("DELETE")) {
                        return previous;
                    }
                } else if (previous.getTokenID().getNumericID() == PlsqlTokenContext.OPERATOR_ID
                        && image.equals(";")) {
                    break;
                }

                previous = previous.getPrevious();
            }

            return null;
        }

        private int getParenthesesStartIndent(TokenItem current) {
            TokenItem previous = current.getPrevious();
            int pCount = 1;
            while (previous != null) {
                if (previous.getTokenID().getNumericID() == PlsqlTokenContext.LPAREN_ID) {
                    pCount--;
                } else if (previous.getTokenID().getNumericID() == PlsqlTokenContext.RPAREN_ID) {
                    pCount++;
                }
                if (pCount == 0) {
                    return getIndentationDiff(previous, current);
                }

                previous = previous.getPrevious();
            }

            return 0;
        }

        /**
         * Get the indent for the current token based on the previous token.
         * @param previousNWS
         * @param token
         * @return
         */
        private int getIndentDiffOnPrevious(TokenItem previousNWS, TokenItem token) {
            int indent = 0;
            int tokenID = previousNWS.getTokenID().getNumericID();

            if (tokenID == PlsqlTokenContext.KEYWORD_ID) {
                //See whether previous keyword is a keyword after which we need a tab
                String tokenImage = previousNWS.getImage().trim();
                if ((tokenImage.equalsIgnoreCase("INSERT"))
                        || (tokenImage.equalsIgnoreCase("UPDATE"))
                        || (tokenImage.equalsIgnoreCase("MERGE"))
                        || (tokenImage.equalsIgnoreCase("DELETE"))
                        || (tokenImage.equalsIgnoreCase("SELECT"))
                        || (tokenImage.equalsIgnoreCase("EXCEPTION"))
                        || (tokenImage.equalsIgnoreCase("THEN"))
                        || (tokenImage.equalsIgnoreCase("BEGIN"))
                        || (tokenImage.equalsIgnoreCase("DECLARE"))
                        || (tokenImage.equalsIgnoreCase("IF"))
                        || (tokenImage.equalsIgnoreCase("ELSIF"))
                        || (tokenImage.equalsIgnoreCase("ELSE"))
                        || (tokenImage.startsWith("$IF"))
                        || (tokenImage.startsWith("$THEN"))
                        || (tokenImage.startsWith("$ELSIF"))
                        || (tokenImage.startsWith("$ELSE"))
                        || (tokenImage.startsWith("$ERROR"))
                        || (tokenImage.equalsIgnoreCase("CASE"))
                        || (tokenImage.equalsIgnoreCase("WHEN"))
                        || (tokenImage.equalsIgnoreCase("MATCHED"))
                        || (tokenImage.equalsIgnoreCase("RECORD"))
                        || (tokenImage.equalsIgnoreCase("LOOP")
                        && (!token.getImage().trim().equalsIgnoreCase("END")))) {
                    return getTabSize();
                } else {
                    if (tokenImage.equalsIgnoreCase("IS")) {
                        /*for select statements in side cursors*/
                        if (token.getImage().equalsIgnoreCase("SELECT")) {
                            return getTabSize();
                        }
                        TokenItem preKey = getPreviousKeyword(previousNWS.getPrevious());
                        if (preKey != null) {
                            TokenItem firstToken = findLineFirstNonWhitespace(getPosition(previousNWS, 0)).getToken();
                            if (token.getTokenID() != PlsqlTokenContext.KEYWORD && firstToken != previousNWS) {
                                return 0;
                            }
                            String image = preKey.getImage().trim();
                            if ((!token.getImage().trim().equalsIgnoreCase("BEGIN"))
                                    && (!((image.equalsIgnoreCase("PACKAGE")) || (image.equalsIgnoreCase("BODY"))))) {
                                return getTabSize();
                            }
                        }
                    }
                }
                if (tokenImage.equalsIgnoreCase("DISTINCT")) {
                    return getIndentForSelect(previousNWS);
                }
            } else if (tokenID == PlsqlTokenContext.LPAREN_ID) {
                //keep tab after '(' e.x in procedure and function declarations
                return getTabSize();
            } else if (tokenID == PlsqlTokenContext.RPAREN_ID /*&& ((token.getImage().trim().equalsIgnoreCase("VALUES"))
                    || (token.getImage().trim().equalsIgnoreCase("RETURNING")))*/) {
                return getParenthesesStartIndent(previousNWS);
                //reduce tab after ')' e.x in '<columns>)' in 'INSERT INTO'
                // return (-getTabSize());
            } else if (tokenID == PlsqlTokenContext.OPERATOR_ID) {
                if (previousNWS.getImage().trim().equalsIgnoreCase(";")) {
                    /*When a statement is ended by ';' in some statements
                     * if the statement is multi-lined we need to go to the beginning to
                     * get the indentation for the next*/
                    TokenItem previousToken = getPreviousNonWhiteSpaceToken(previousNWS);
                    /* if the previous Token is a ')' then we should find the begining of the statement */
                    if (previousToken != null && previousToken.getTokenID().getNumericID() == PlsqlTokenContext.RPAREN_ID) {
                        int multiLineIndent = getMethodIndent(previousToken);
                        if (multiLineIndent != -1 && multiLineIndent != 0 && getLineIndent(getPosition(previousNWS, 0), true) != multiLineIndent) {
                            return multiLineIndent - getLineIndent(getPosition(previousNWS, 0), true);
                        }
                    } // if previousToken is a character literal, we need to find the starting line of the string
                    else if (previousToken != null && previousToken.getTokenID().getNumericID() == PlsqlTokenContext.CHAR_LITERAL_ID) {
                        TokenItem preCharToken = getPreviousNonCharLiteralToken(previousToken);
                        FormatTokenPosition tmp1 = getPosition(preCharToken, 0);
                        FormatTokenPosition tmp2 = getPosition(previousNWS, 0);
                        if (!(findLineEnd(tmp1)).equals(findLineEnd(tmp2))) {
                            return (getLineIndent(tmp1, true) - getLineIndent(tmp2, true));
                        }
                    }
                    int diff[] = {0};
                    String previousBlockKey = getPreviousStmtKey(previousNWS, diff);

                    if ((previousBlockKey.equalsIgnoreCase("SELECT"))
                            || (previousBlockKey.equalsIgnoreCase("UPDATE"))
                            || (previousBlockKey.equalsIgnoreCase("INSERT"))
                            || (previousBlockKey.equalsIgnoreCase("MERGE"))
                            || (previousBlockKey.equalsIgnoreCase("DELETE"))
                            || (previousBlockKey.equalsIgnoreCase("TYPE"))
                            || (previousBlockKey.equalsIgnoreCase("COMMENT"))
                            || (previousBlockKey.equalsIgnoreCase("FETCH"))
                            || (previousBlockKey.equalsIgnoreCase("CURSOR"))) {
                        return (diff[0]);
                    } else if (previousBlockKey.equalsIgnoreCase("FUNCTION")) {
                        //check whether this is a function implamentation
                        TokenItem parentFunc = getPreviousTokenWithImage(previousNWS, "IS", "FUNCTION");
                        if (parentFunc == null) {
                            return (diff[0]);
                        }
                    } else if (previousBlockKey.equalsIgnoreCase("PROCEDURE")) {
                        //check whether this is a procedure implamentation
                        TokenItem parentFunc = getPreviousTokenWithImage(previousNWS, "IS", "PROCEDURE");
                        if (parentFunc == null) {
                            return (diff[0]);
                        }
                    } else {
                        //Check whether there is a PACKAGE declaration or VIEW before
                        TokenItem parentView = getPreviousTokenWithImage(getPreviousToken(previousNWS), "VIEW", ";");
                        if (parentView != null) {
                            indent = getIndentationDiff(parentView, previousNWS);
                        } else {
                            //Check whether this is the first in a package declaration/body
                            indent = isPackageFirst(previousNWS);
                        }
                    }
                } else if (previousNWS.getImage().trim().equalsIgnoreCase(">")) {
                    //This will be useful for only the placeholders inside a template
                    if (getPreviousKeyword(previousNWS) != null) {
                        String previousKeyword = getPreviousKeyword(previousNWS).getImage().trim();

                        if ((previousKeyword.equalsIgnoreCase("SELECT"))
                                || (previousKeyword.equalsIgnoreCase("UPDATE"))) {
                            return getTabSize();
                        }
                    }
                } else if (previousNWS.getImage().trim().equalsIgnoreCase(",")) {
                    //This will be useful for SELECT , UPDATE , INTO variables
                    TokenItem first = findLineFirstNonWhitespace(getPosition(previousNWS, 0)).getToken();
                    String previousKeyword = first.getImage().trim();
                    TokenItem findStatementStart = findStatementStart(first);
                    if (findStatementStart != null) {
                        previousKeyword = findStatementStart.getImage().trim();
                    }

                    if (previousKeyword.equalsIgnoreCase("SELECT") || (previousKeyword.equalsIgnoreCase("FROM"))) {
                        return getIndentForSelect(previousNWS);
                    }

                    if ((previousKeyword.equalsIgnoreCase("INTO"))
                            || (previousKeyword.equalsIgnoreCase("SET"))
                            || (previousKeyword.equalsIgnoreCase("CURSOR"))
                            || (previousKeyword.equalsIgnoreCase("IS"))) {
                        return getTabSize();
                    }
                } else if (previousNWS.getImage().trim().equalsIgnoreCase("*")) {
                    //Used for a case like 'Select *'
                    if (getPreviousKeyword(previousNWS).getImage().trim().equalsIgnoreCase("SELECT")) {
                        return getTabSize();
                    }
                }
            }
            return indent;
        }

        /**
         * Get the previous non-whitespace token
         * @param token
         * @return TokenItem.
         */
        private TokenItem getPreviousNonWhiteSpaceToken(TokenItem token) {
            token = token.getPrevious();
            while (token != null) {
                if (token.getTokenID().getNumericID() != PlsqlTokenContext.WHITESPACE_ID) {
                    return token;
                }
                token = token.getPrevious();
            }
            return token;
        }

        /**
         * Get the next non-whitespace token
         * @param token
         * @return TokenItem.
         */
        private TokenItem getNextNonWhiteSpaceToken(TokenItem token) {
            token = token.getNext();
            while (token != null) {
                if (token.getTokenID().getNumericID() != PlsqlTokenContext.WHITESPACE_ID) {
                    return token;
                }
                token = token.getNext();
            }
            return token;
        }

        /**
         * Get the previous token Item which is not a character literal
         * @param token
         * @return TokenItem which is not a character literal.
         */
        private TokenItem getPreviousNonCharLiteralToken(TokenItem token) {
            token = token.getPrevious();
            while (token != null) {
                if (token.getTokenID().getNumericID() != PlsqlTokenContext.WHITESPACE_ID && token.getTokenID().getNumericID() != PlsqlTokenContext.CHAR_LITERAL_ID) {
                    return token;
                }
                token = token.getPrevious();
            }
            return token;
        }

        /**
         * Get the indentation of a method which has multiple lines, it will simply matches
         * brace count and return indentation of the method's first line.
         * @param token, which has image of ')'
         * @return indentation, indentation of the line that consist the method's starting '(' if a match found else 0.
         */
        private int getMethodIndent(TokenItem token) {
            TokenItem tempToken = getPreviousNonWhiteSpaceToken(token);
            FormatTokenPosition tmp1 = getPosition(tempToken, 0);
            FormatTokenPosition tmp2 = null;
            int brace_count = 1;
            while (tempToken != null) {
                if (tempToken.getTokenID().getNumericID() == PlsqlTokenContext.LPAREN_ID) {
                    brace_count--;
                } else if (tempToken.getTokenID().getNumericID() == PlsqlTokenContext.RPAREN_ID) {
                    brace_count++;
                }
                if (brace_count == 0) {
                    tmp2 = getPosition(tempToken, 0);
                    break;
                }
                tempToken = getPreviousNonWhiteSpaceToken(tempToken);

            }
            if (tmp1 != null && tmp2 != null && !(findLineEnd(tmp1)).equals(findLineEnd(tmp2))) {
                return getPreviousStmtIndent(tmp2.getToken());
            } else {
                return -1;
            }
        }

        /**
         * Method to get the indentation of the previous statement
         * @param previousNWS
         * @param diff
         * @return
         */
        private int getPreviousStmtIndent(TokenItem previousNWS) {
            int diff = 0;
            TokenItem tokenTempPre = null;
            TokenItem tokenTemp = getPreviousToken(previousNWS);

            while (tokenTemp != null) {
                String image = tokenTemp.getImage().trim();
                if ((image.equalsIgnoreCase(";"))
                        || (image.equalsIgnoreCase("BEGIN"))
                        || (image.equalsIgnoreCase("LOOP"))
                        || (image.equalsIgnoreCase("ELSE"))
                        || (image.equalsIgnoreCase("$ELSE"))
                        || (image.equalsIgnoreCase("THEN"))
                        || (image.equalsIgnoreCase("$THEN"))
                        || (image.equalsIgnoreCase("CURSOR"))) {
                    //METHOD declarations inside a
                    break;
                } else {
                    if (tokenTemp.getTokenID().getNumericID() == PlsqlTokenContext.KEYWORD_ID) {
                        tokenTempPre = tokenTemp;
                    }
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            if (tokenTempPre != null) {
                FormatTokenPosition tmp1 = getPosition(tokenTempPre, 0);
                FormatTokenPosition tmp2 = getPosition(previousNWS, 0);

                //if same line ignore
                if ((findLineEnd(tmp1)).equals(findLineEnd(tmp2))) {
                    return diff;
                }

                diff = getLineIndent(tmp1, true);
            } else {
                FormatTokenPosition tmp1 = getPosition(getNextNonWhiteSpaceToken(tokenTemp), 0);
                diff = getLineIndent(tmp1, true);
            }

            return diff;
        }

        /**
         * Get the starting line position of a method which has multiple lines, it will simply matches
         * brace count and return the position of the matching token.
         * @param token, which has image of ')'
         * @return format token Position, position of '(' if a match found else null.
         */
        private FormatTokenPosition getMethodStartPosition(TokenItem token) {
            TokenItem tempToken = getPreviousNonWhiteSpaceToken(token);
            FormatTokenPosition tmp1 = getPosition(tempToken, 0);
            FormatTokenPosition tmp2 = null;
            int brace_count = 1;
            while (tempToken != null) {
                if (tempToken.getTokenID().getNumericID() == PlsqlTokenContext.LPAREN_ID) {
                    brace_count--;
                } else if (tempToken.getTokenID().getNumericID() == PlsqlTokenContext.RPAREN_ID) {
                    brace_count++;
                }
                if (brace_count == 0) {
                    tmp2 = getPosition(tempToken, 0);
                    break;
                }
                tempToken = getPreviousNonWhiteSpaceToken(tempToken);

            }
            if (tmp1 != null && tmp2 != null && !(findLineEnd(tmp1)).equals(findLineEnd(tmp2))) {
                return tmp2;
            } else {
                return tmp1;
            }
        }

        /**
         * Get the parent of the IS, can be FUNCTION/ PROCEDURE/PACKAGE/CURSOR
         * @param previous
         * @return
         */
        private TokenItem getIsParent(TokenItem previous) {
            TokenItem tokenTemp = previous;
            TokenItem tokenParent = null;

            while (tokenTemp != null) {
                String imageTmp = tokenTemp.getImage().trim();

                if ((imageTmp.equalsIgnoreCase("FUNCTION"))
                        || (imageTmp.equalsIgnoreCase("PROCEDURE"))
                        || (imageTmp.equalsIgnoreCase("PACKAGE"))
                        || (imageTmp.equalsIgnoreCase("CURSOR"))
                        || (imageTmp.equalsIgnoreCase("COMMENT"))) {
                    tokenParent = tokenTemp;
                    break;
                } else {
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            return tokenParent;
        }

        /**
         * Get parent IF of the given ELSE/ELSIF
         * @param previous
         * @return
         */
        private TokenItem getParentIf(TokenItem previous) {
            TokenItem tokenTemp = previous;
            TokenItem tokenTempPre = previous;
            TokenItem tokenParent = null;
            int endNo = 0;

            while (tokenTemp != null) {
                String imageTmp = tokenTemp.getImage().trim();
                String imageTmpPre = tokenTempPre.getImage().trim();
                boolean isParent = false;

                //Ignore END IF;
                if ((imageTmp.equalsIgnoreCase("IF") || imageTmp.equalsIgnoreCase("$IF"))
                        && (!imageTmpPre.equalsIgnoreCase(";"))) {
                    isParent = true;
                }

                //If this is an IF and no pending END IF's to pass, break
                if (isParent && (endNo == 0)) {
                    tokenParent = tokenTemp;
                    break;
                } else {
                    //If we have reached a parent note that
                    if (isParent) {
                        --endNo;
                    }

                    //If we have reached another END IF that
                    if (imageTmp.equalsIgnoreCase("END") && (imageTmpPre.equalsIgnoreCase("IF") || imageTmpPre.equalsIgnoreCase("$IF"))) {
                        ++endNo;
                    }

                    //get previous non white space token only
                    if (tokenTemp.getTokenID().getNumericID() != PlsqlTokenContext.WHITESPACE_ID) {
                        tokenTempPre = tokenTemp;
                    }
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            return tokenParent;
        }

        /**
         * Method to get the begining key word of the previous statement
         * @param previousNWS
         * @param diff
         * @return
         */
        private String getPreviousStmtKey(TokenItem previousNWS, int[] diff) {
            String key = "";
            TokenItem tokenTempPre = null;
            TokenItem tokenTemp = getPreviousToken(previousNWS);

            while (tokenTemp != null) {
                String image = tokenTemp.getImage().trim();
                if ((image.equalsIgnoreCase(";"))
                        || (image.equalsIgnoreCase("BEGIN"))
                        || (image.equalsIgnoreCase("LOOP"))
                        || (image.equalsIgnoreCase("ELSE"))
                        || (image.equalsIgnoreCase("$ELSE"))
                        || (image.equalsIgnoreCase("THEN"))
                        || (image.equalsIgnoreCase("$THEN"))
                        || (image.equalsIgnoreCase("CURSOR"))) {
                    if (image.equalsIgnoreCase("CURSOR")) {
                        tokenTempPre = tokenTemp;
                    }
                    //METHOD declarations inside a
                    break;
                } else {
                    if (tokenTemp.getTokenID().getNumericID() == PlsqlTokenContext.KEYWORD_ID) {
                        tokenTempPre = tokenTemp;
                    }
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            if (tokenTempPre != null) {
                FormatTokenPosition tmp1 = getPosition(tokenTempPre, 0);
                FormatTokenPosition tmp2 = getPosition(previousNWS, 0);

                //if same line ignore
                if ((findLineEnd(tmp1)).equals(findLineEnd(tmp2))) {
                    return "";
                }

                diff[0] = getLineIndent(tmp1, true) - getLineIndent(tmp2, true);
                key = tokenTempPre.getImage().trim();
            }

            return key;
        }

        /**
         * Get the keyword token before this token and retuen the image
         * @param previousNWS
         * @return
         */
        private TokenItem getPreviousKeyword(TokenItem previousNWS) {
            TokenItem tokenTemp = previousNWS;
            TokenItem tokenTempPre = previousNWS;

            while (tokenTemp != null) {
                if ((tokenTemp.getTokenID().getNumericID() != PlsqlTokenContext.KEYWORD_ID) || (tokenTempPre.getImage().equals(">"))) { //avaid getting placeholder names which are key words e.g <table>

                    tokenTempPre = tokenTemp;
                    tokenTemp = getPreviousToken(tokenTemp);
                } else {
                    break;
                }
            }

            return tokenTemp;
        }

        /**
         * Get previous token with the given image id before the image id endImage
         * @param token
         * @param imageId
         * @param endImage
         * @return token item, null if not found
         */
        private TokenItem getPreviousTokenWithImage(TokenItem token, String imageId, String endImage) {
            TokenItem tokenTemp = token;
            TokenItem tokenRes = null;

            while ((tokenTemp != null) && (!tokenTemp.getImage().trim().equalsIgnoreCase(endImage))) {
                if (tokenTemp.getImage().trim().equalsIgnoreCase(imageId)) {
                    tokenRes = tokenTemp;
                    break;
                } else {
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            return tokenRes;
        }

        /**
         * Get the correct indent for Select statements 
         * @param previousNWS
         * @return token item, null if not found
         */
        private int getIndentForSelect(TokenItem previousNWS) {

            int indent = getTabSize();
            TokenItem first = findLineFirstNonWhitespace(getPosition(previousNWS, 0)).getToken();

            int parent = getLineIndent(getPosition(previousNWS, 0), true);
            TokenItem findStatementStart = findStatementStart(first);

            if (findStatementStart != null) {
                first = findStatementStart;
            }

            do {
                first = first.getNext();
                if ((first != null) && (first.getTokenID() != PlsqlTokenContext.WHITESPACE) && (first.getTokenID() != PlsqlTokenContext.BLOCK_COMMENT)
                        && (first.getTokenID() != PlsqlTokenContext.LINE_COMMENT)) {
                    return getVisualColumnOffset(getPosition(first, 0)) - parent;
                }
            } while (first != null);
            return indent;
        }

        /**
         * Method to calculate indentation difference between two token items
         * belonging to multiple lines
         * @param token1
         * @param token2
         * @return
         */
        private int getIndentationDiff(TokenItem token1, TokenItem token2) {
            int indent = 0;
            FormatTokenPosition tmp1 = getPosition(token1, 0);
            FormatTokenPosition tmp2 = getPosition(token2, 0);

            //if same line ignore
            if (!(findLineEnd(tmp1)).equals(findLineEnd(tmp2))) {
                indent = getLineIndent(tmp1, true) - getLineIndent(tmp2, true);
            }

            return indent;
        }

        /**
         * Method to get the previous when then block in the same level
         * @param previous
         * @return
         */
        private TokenItem getPreviousWhenThenBlock(TokenItem previous) {
            TokenItem tokenTemp = previous;
            TokenItem tokenWhen = null;
            TokenItem tokenThen = null;

            while (tokenTemp != null) {
                String imageTmp = tokenTemp.getImage().trim();

                //If we have reached the beginning of a block stop
                if ((imageTmp.equalsIgnoreCase("$ERROR"))
                        || (imageTmp.equalsIgnoreCase("BEGIN"))
                        || (imageTmp.equalsIgnoreCase("EXCEPTION"))
                        || (imageTmp.equalsIgnoreCase("LOOP"))) {
                    break;
                } else if ((imageTmp.equalsIgnoreCase(";"))
                        && (tokenThen != null)) {
                    //Don't catch IF-THEN
                    break;
                } else if ((imageTmp.equalsIgnoreCase("WHEN"))
                        && (tokenThen != null)) {
                    tokenWhen = tokenTemp;
                    break;
                } else {
                    if ((imageTmp.equalsIgnoreCase("THEN") || imageTmp.equalsIgnoreCase("$THEN"))
                            && (tokenWhen == null)) {
                        //Get previous "THEN"
                        tokenThen = tokenTemp;
                    }
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            return tokenWhen;
        }

        /**
         * Method to check whether the given statement which ends by ';'
         * is the first element in a package declaration/ implementation
         * @param previous
         * @return
         */
        private int isPackageFirst(TokenItem previous) {
            int indent = 0;
            TokenItem tokenTemp = getPreviousToken(previous);
            TokenItem tokenTempPre = null;

            while (tokenTemp != null) {
                String imageTmp = tokenTemp.getImage().trim();

                //If we have reached the beginning of a block stop
                if ((imageTmp.equalsIgnoreCase(";"))
                        || (imageTmp.equalsIgnoreCase("PACKAGE"))
                        || (imageTmp.equalsIgnoreCase("BEGIN"))) {
                    break;
                } else {
                    if ((imageTmp.equalsIgnoreCase("PROCEDURE"))
                            || (imageTmp.equalsIgnoreCase("FUNCTION"))
                            || (imageTmp.equalsIgnoreCase("SELECT"))
                            || (imageTmp.equalsIgnoreCase("UPDATE"))
                            || (imageTmp.equalsIgnoreCase("INSERT"))
                            || (imageTmp.equalsIgnoreCase("MERGE"))
                            || (imageTmp.equalsIgnoreCase("DELETE"))
                            || (imageTmp.equalsIgnoreCase("CURSOR"))
                            || (imageTmp.equalsIgnoreCase("TYPE"))) {
                        tokenTempPre = tokenTemp;
                    }
                    tokenTemp = getPreviousToken(tokenTemp);
                }
            }

            if ((tokenTemp != null) && (tokenTemp.getImage().trim().equalsIgnoreCase("PACKAGE"))) {
                if (tokenTempPre != null && !"FUNCTION".equalsIgnoreCase(tokenTempPre.getImage()) && !"PROCEDURE".equalsIgnoreCase(tokenTempPre.getImage())) {
                    indent = getIndentationDiff(tokenTempPre, previous);
                }
            }
            return indent;
        }
    }
    private static HashSet<String> sqlPlus = new HashSet<String>();

    static {
        pupulateSQLPlus();
    }

    /**
     * populates the hashset of keywords from the property in the
     * resource bundle
     */
    private static void pupulateSQLPlus() {
        String fullList = NbBundle.getBundle(PlsqlFormatter.class).getString("LIST_SQLPLUS");
        StringTokenizer st = new StringTokenizer(fullList, ","); // NOI18N
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            token = token.toUpperCase(Locale.ENGLISH).trim();

            if (!sqlPlus.contains(token)) {
                sqlPlus.add(token);
            }
        }
    }

    private static JEditorPane getEditorPane(final EditorCookie editorCookie) {
        if (SwingUtilities.isEventDispatchThread()) {
            JEditorPane[] panes = editorCookie.getOpenedPanes();
            return panes != null && panes.length == 1 ? panes[0] : null;
        } else {
            return null;
        }
    }
}
