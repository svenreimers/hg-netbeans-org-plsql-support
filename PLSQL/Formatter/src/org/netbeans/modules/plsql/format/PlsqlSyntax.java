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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.TokenID;
import org.openide.util.NbBundle;

/*
 * Class description
 *
 * Created on February 13, 2006, 9:42 AM
 *
 * @author IFS
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
public class PlsqlSyntax extends Syntax {
    
    private static final int ISI_WHITESPACE = 2; // inside white space
    private static final int ISI_LINE_COMMENT = 4; // inside line comment --
    private static final int ISI_BLOCK_COMMENT = 5; // inside block comment /* ... */
    private static final int ISI_STRING = 6; // inside string constant
    private static final int ISI_STRING_A_QUOTE = 7; // inside string constant after '
    private static final int ISI_IDENTIFIER = 10; // inside identifier
    private static final int ISA_SLASH = 11; // slash char
    private static final int ISA_OPERATOR = 12; // after '=', '/', '+'
    private static final int ISA_MINUS = 13;
    private static final int ISA_STAR = 20; // after '*'
    private static final int ISA_STAR_I_BLOCK_COMMENT_END = 21; // after '*' in a block comment
    private static final int ISA_EXCLAMATION = 26; // after '!'
    private static final int ISA_ZERO = 27; // after '0'
    private static final int ISI_INT = 28; // integer number
    private static final int ISI_DOUBLE = 30; // double number
    private static final int ISA_DOT = 33; // after '.'
    private static final int ISA_COMMA = 34; // after ','
    private static final int ISA_SEMICOLON = 35; //after ';'
    private static final int ISA_LPAREN = 36; //after (
    private static final int ISA_RPAREN = 37; //after )
    private static final int ISI_CHAR = 38; //after ''

    /**
     * A hashset of keywords
     */
    private static HashSet<String> keywords = new HashSet<String>();
    static {
        populateKeywords();
    }
    
    /** 
     * Creates a new instance of PLSQLSyntax 
     */
    public PlsqlSyntax() {
        tokenContextPath = PlsqlTokenContext.contextPath;
    }
    
    /**
     * populates the hashset of keywords from the property in the
     * resource bundle
     */
    private static void populateKeywords() {
        String fullList = NbBundle.getBundle(PlsqlSyntax.class).getString
                ("LIST_PLSQLKeywords");
        StringTokenizer st = new StringTokenizer(fullList, ","); // NOI18N
        while(st.hasMoreTokens()) {
            String token = st.nextToken();
            token = token.toUpperCase(Locale.ENGLISH).trim();
            
            if(!keywords.contains(token)) {
                keywords.add(token);
            }
        }
    }
    
    /**
     * Returns an alphabetically sorted list of strings for the PLSQL keywords
     * @return returns the array of PLSQL keywords.
     */
    public static String[] getKeywordList() {
        String[] keywordArray = new String[keywords.size()];
        ArrayList result = new ArrayList();
        
        Iterator iter = keywords.iterator();
        int index = 0;
        while(iter.hasNext()) {
            String keyword = (String) iter.next();           
            keywordArray[index] = keyword;
            index++;
        }  
        Arrays.sort(keywordArray);      
        return keywordArray;
    }
    
    /**
     * Parse the next token
     * @return returns the token identified by the parser
     */
    protected TokenID parseToken() {
        char actChar; //the current character
        
        //while we still have stuff to parse, do so
        while(offset < stopOffset) {
            actChar = buffer[offset];

            //do the appropriate stuff based on what state the parser is in
            switch (state) {
                //the initial state (start of a new token)
                case INIT:
                    switch (actChar) {
                                                    
                        case '\"' :
                            state = ISI_STRING;
                            break;
                            
                        case '\'' : // NOI18N
                            state = ISI_CHAR;
                            break;
                            
                        case '/' :
                            state = ISA_SLASH;                          
                             break;
                             
                        case '(':
                                offset++;
                                return PlsqlTokenContext.LPAREN;
                        case ')':
                                offset++;
                                return PlsqlTokenContext.RPAREN;
                        case ']':
                                offset++;
                                return PlsqlTokenContext.RBRACKET;
                        case '[':
                                offset++;
                                return PlsqlTokenContext.LBRACKET;
                        case '{':
                                offset++;
                                return PlsqlTokenContext.LBRACE;
                        case '}':
                                offset++;
                                return PlsqlTokenContext.RBRACE;

                        case '=':
                        case ':':
                        case '>':
                        case '<':
                        case '+':
                        case ',':
                        case ';':
                        case '*':
                        case '!':
                            offset++;
                            state = INIT;
                            return PlsqlTokenContext.OPERATOR;
                        case '-':
                            state = ISA_MINUS;
                            break;
                        case '0':
                            state = ISA_ZERO;
                            break;
                        case '.':
                            state = ISA_DOT;
                            break;
                        default:
                            // Check for whitespace
                            if (Character.isWhitespace(actChar)) {
                                state = ISI_WHITESPACE;
                                break;
                            }

                            // Check for digit
                            if (Character.isDigit(actChar)) {
                                state = ISI_INT;
                                break;
                            }

                            // otherwise it's an identifier
                            state = ISI_IDENTIFIER;
                            break;
                    }
                    break;
                //if we are currently in a whitespace token
                case ISI_WHITESPACE: // white space
                    if (!Character.isWhitespace(actChar)) {
                        state = INIT;
                        return PlsqlTokenContext.WHITESPACE;
                    }
                    break;

                //if we are currently in a line comment
                case ISI_LINE_COMMENT:
                    if (actChar == '\n') {
                        state = INIT;
                        return PlsqlTokenContext.LINE_COMMENT;
                    }
                    break;

                //if we are currently in a block comment
                case ISI_BLOCK_COMMENT:
                    if(actChar =='*') {
                        state = ISA_STAR_I_BLOCK_COMMENT_END;
                    }
                    break;
                    
                 case ISI_CHAR:
                    switch (actChar) {
                        case '\'': // NOI18N
                            offset++;
                            state = INIT;
                            return PlsqlTokenContext.CHAR_LITERAL; // Character
                    }  
                    break;
                
                //if we are currently in a string literal
                case ISI_STRING:
                    switch (actChar) { 
                        case '\n':
                            state = INIT;
                            return PlsqlTokenContext.STRING_LITERAL;/*INCOMPLETE_STRING; */
                        case '\"': // NOI18N
                            offset++;
                            state = INIT;
                            return PlsqlTokenContext.STRING_LITERAL; /*STRING;*/
                    }
                    break;
                    
                //if we are currently in an identifier (e.g. a variable name)
                case ISI_IDENTIFIER:
                    if (!Character.isLetterOrDigit(actChar) && actChar != '_') {
                        state = INIT;
                        TokenID tid = matchKeyword(buffer, tokenOffset, offset - 
                                tokenOffset);
                        if (tid != null) {
                            return tid;
                        } else {
                            return PlsqlTokenContext.IDENTIFIER;
                        }
                    }
                    break;

                //if we are after a slash (/)
                case ISA_SLASH:
                    switch (actChar) {
                        case '*':
                            state = ISI_BLOCK_COMMENT;
                            break;
                        default:
                            state = INIT;
                            return PlsqlTokenContext.OPERATOR;
                    }
                    break;

                //if we are after a -
                case ISA_MINUS:
                    switch (actChar) {
                        case '-':
                            state = ISI_LINE_COMMENT;
                            break;
                        default:
                            state = INIT;
                            return PlsqlTokenContext.OPERATOR;
                    }
                    break;
                
                //if we are in the middle of a possible block comment end token
                case ISA_STAR_I_BLOCK_COMMENT_END:
                    switch (actChar) {
                        case '/':
                            offset++;
                            state = INIT;
                            return PlsqlTokenContext.BLOCK_COMMENT;
                        default:
                            offset--;
                            state = ISI_BLOCK_COMMENT;
                            break;
                    }
                    break;
                       
                //if we are after a 0
                case ISA_ZERO:
                    switch (actChar) {
                        case '.':
                            state = ISI_DOUBLE;
                            break;
                        default:
                            if (Character.isDigit(actChar)) { 
                                state = ISI_INT;
                                break;
                            } else {
                                state = INIT;
                                return PlsqlTokenContext.INT_LITERAL;
                            }
                    }
                    break;

                //if we are after an integer
                case ISI_INT:
                    switch (actChar) {
                        case '.':
                            state = ISI_DOUBLE;
                            break;
                        default:
                            if (Character.isDigit(actChar)) { 
                                state = ISI_INT;
                                break;
                            } else {
                                state = INIT;
                                return PlsqlTokenContext.INT_LITERAL;
                            }
                    }
                    break;

                //if we are in the middle of what we believe is a floating point
                //number
                case ISI_DOUBLE:
                    if (actChar >= '0' && actChar <= '9') {
                        state = ISI_DOUBLE;
                        break;
                    } else {
                        state = INIT;
                        return PlsqlTokenContext.DOUBLE_LITERAL;
                    }

                //if we are after a period
                case ISA_DOT:
                    if (Character.isDigit(actChar)) {
                        state = ISI_DOUBLE;
                    } else { // only single dot
                        state = INIT;
                        return PlsqlTokenContext.DOT;
                    }
                    break;

            } // end of switch(state)

            offset++;
        } // end of while(offset...)

        /* 
         * At this stage there's no more text in the scanned buffer.
         * Scanner first checks whether this is completely the last
         * available buffer.
         */
        if (lastBuffer) {
            switch(state) {
            case ISI_WHITESPACE:
                state = INIT;
                    return PlsqlTokenContext.WHITESPACE;
            case ISI_IDENTIFIER:
                state = INIT;
                TokenID tid = 
                        matchKeyword(buffer, tokenOffset, offset - tokenOffset);
                if(tid != null) {
                    return tid;
                }
                else {
                    return PlsqlTokenContext.IDENTIFIER;
                }
            case ISI_LINE_COMMENT:
                // stay in line-comment state
                return PlsqlTokenContext.LINE_COMMENT; 
            case ISI_BLOCK_COMMENT:
            case ISA_STAR_I_BLOCK_COMMENT_END:
                // stay in block-comment state
                return PlsqlTokenContext.BLOCK_COMMENT; 
            case ISI_STRING:
                return PlsqlTokenContext.STRING_LITERAL; /*STRING;*/ // hold the state
            case ISA_ZERO:
            case ISI_INT:
                state = INIT;
                return PlsqlTokenContext.INT_LITERAL; 
            case ISI_DOUBLE:
                state = INIT;
                return PlsqlTokenContext.DOUBLE_LITERAL; 
            case ISA_DOT:
                state = INIT;
                return PlsqlTokenContext.DOT; 
            case ISA_SLASH:
                state = INIT;
                return PlsqlTokenContext.OPERATOR; 
            case ISI_CHAR:
                return PlsqlTokenContext.CHAR_LITERAL;//sadhlk
            }
        }

        /* 
         * At this stage there's no more text in the scanned buffer, but
         * this buffer is not the last so the 
         * scan will continue on another buffer.
         * The scanner tries to minimize the amount of characters
         * that will be prescanned in the next buffer by returning the token
         * where possible.
         */

        switch (state) {
            case ISI_WHITESPACE:
                    return PlsqlTokenContext.WHITESPACE; 
        }

        return null; // nothing found
    }

    /**
     * Returns the state name for the state id
     * @param stateNumber the int value of the token
     * @return returns the String value for the tokens
     */
    public String getStateName(int stateNumber) {
        switch(stateNumber) {
        case ISI_WHITESPACE:
            return "ISI_WHITESPACE"; // NOI18N
        case ISI_LINE_COMMENT:
            return "ISI_LINE_COMMENT"; // NOI18N
        case ISI_BLOCK_COMMENT:
            return "ISI_BLOCK_COMMENT"; // NOI18N
        case ISI_STRING:
            return "ISI_STRING"; // NOI18N
        case ISI_STRING_A_QUOTE:
            return "ISI_STRING_A_QUOTE"; // NOI18N
        case ISI_IDENTIFIER:
            return "ISI_IDENTIFIER"; // NOI18N
        case ISA_SLASH:
            return "ISA_SLASH"; // NOI18N
        case ISA_OPERATOR:
            return "ISA_OPERATOR"; // NOI18N
        case ISA_MINUS:
            return "ISA_MINUS"; // NOI18N
        case ISA_STAR:
            return "ISA_STAR"; // NOI18N
        case ISA_STAR_I_BLOCK_COMMENT_END:
            return "ISA_STAR_I_BLOCK_COMMENT_END"; // NOI18N
        case ISA_ZERO:
            return "ISA_ZERO"; // NOI18N
        case ISI_INT:
            return "ISI_INT"; // NOI18N
        case ISI_DOUBLE:
            return "ISI_DOUBLE"; // NOI18N
        case ISA_DOT:
            return "ISA_DOT"; // NOI18N
        case ISA_COMMA:
            return "ISA_COMMA"; // NOI18N

        default:
            return super.getStateName(stateNumber);
        }
    }

    /**
     * Tries to match the specified sequence of characters to a PLSQL
     * keyword.
     * @param buffer character array which holds the characters to be parsed
     * @param offset start position 
     * @param len length of the character array
     * @return the KEYWORD id or null if no match was found
     */
    public TokenID matchKeyword(char[] buffer, int offset, int len) {
        String keywordCandidate = new String(buffer, offset, len);
        keywordCandidate = keywordCandidate.toUpperCase(Locale.ENGLISH);
        
        if(keywords.contains(keywordCandidate)) {
            return PlsqlTokenContext.KEYWORD;
        }
        
        return null;
    }
}

