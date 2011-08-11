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
package org.netbeans.modules.plsql.lexer;

import java.util.HashMap;
import org.netbeans.api.lexer.Token;
import org.netbeans.spi.lexer.Lexer;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;
import org.netbeans.spi.lexer.TokenFactory;
import java.util.StringTokenizer;
import org.openide.util.NbBundle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class PlsqlLexer implements Lexer<PlsqlTokenId> {

   private static final int EOF = LexerInput.EOF;
   private final static Map<Character, Character> delimiterMap = new HashMap<Character, Character>();
   private final LexerInput input;
   private final TokenFactory<PlsqlTokenId> tokenFactory;
   private Token<PlsqlTokenId> lastToken = null;

   static {
      delimiterMap.put('(', ')');
      delimiterMap.put('<', '>');
      delimiterMap.put('[', ']');
      delimiterMap.put('{', '}');
   }

   @Override
   public Object state() {
      // autoconversion uses Integer.valueOf() which caches <-127,127>
      return null;
   }

   public PlsqlLexer(final LexerRestartInfo<PlsqlTokenId> info) {
      this.input = info.input();
      this.tokenFactory = info.tokenFactory();
   }

   @Override
   public Token<PlsqlTokenId> nextToken() {
      int c = '\n'; //Initially hope the best to be
      while (true) {
         int pre = c;
         c = input.read();
         if ('q' == c || 'Q' == c) {
            final int next = input.read();
            if ('\'' == next) {
               pre = c;
               c = next;
            } else {
               input.backup(1);
            }
         }

         switch (c) {
            case '_':
            case '&':
            case '$':
            case '#':
               return finishIdentifier();

            case '\t':
            case '\n':
            case 0x0b:
            case '\f':
            case '\r':
            case 0x1c:
            case 0x1d:
            case 0x1e:
            case 0x1f:
               return finishWhitespace();

            case ' ':
               c = input.read();
               if (c == EOF || !Character.isWhitespace(c)) { // Return single space as flyweight token
                  input.backup(1);
                  lastToken = tokenFactory.getFlyweightToken(PlsqlTokenId.WHITESPACE, " ");
                  return lastToken;
               }
               return finishWhitespace();

            case '(':
               return token(PlsqlTokenId.LPAREN);
            case ')':
               return token(PlsqlTokenId.RPAREN);
            case ']':
               return token(PlsqlTokenId.RBRACKET);
            case '[':
               return token(PlsqlTokenId.LBRACKET);
            case '{':
               return token(PlsqlTokenId.LBRACE);
            case '}':
               return token(PlsqlTokenId.RBRACE);

            case '=':
            case ':':
            case '>':
            case '<':
            case '+':
            case '|':
            case ',':
            case ';':
            case '%':
               return token(PlsqlTokenId.OPERATOR);

            case '*':
            case '!':
               return checkIdentifierOrOperator(pre);

            case '-':
               switch (input.read()) {
                  case '-': // in single-line comment
                     final String line = readLine();
                     if (line.startsWith("@Ignore") || line.startsWith("@Approve") || line.startsWith("@Allow")) {
                        return token(PlsqlTokenId.IGNORE_MARKER);
                     } else {
                        return token(PlsqlTokenId.LINE_COMMENT);
                     }
                  default:
                     input.backup(1);
                     return token(PlsqlTokenId.OPERATOR);
               }

            case '/':
               switch (input.read()) {
                  case '*': // in block comment
                     return readBlockComment();
                  default:
                     input.backup(1);
                     return token(PlsqlTokenId.OPERATOR);
               }

            case 'a':
            case 'b':
            case 'c':
            case 'd':
            case 'e':
            case 'f':
            case 'g':
            case 'h':
            case 'i':
            case 'j':
            case 'k':
            case 'l':
            case 'm':
            case 'n':
            case 'o':
            case 'p':
            case 'q':
            case 'r':
            case 's':
            case 't':
            case 'u':
            case 'v':
            case 'w':
            case 'x':
            case 'y':
            case 'z':
            case 'A':
            case 'B':
            case 'C':
            case 'D':
            case 'E':
            case 'F':
            case 'G':
            case 'H':
            case 'I':
            case 'J':
            case 'K':
            case 'L':
            case 'M':
            case 'N':
            case 'O':
            case 'P':
            case 'Q':
            case 'R':
            case 'S':
            case 'T':
            case 'U':
            case 'V':
            case 'W':
            case 'X':
            case 'Y':
            case 'Z':
               final String word = getWordToken(c);
               return token(matchToken(word));

            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
               while (true) {
                  c = input.read();
                  switch (c) {
                     case '.':
                        if (checkDouble()) {
                           return token(PlsqlTokenId.DOUBLE_LITERAL);
                        } else {
                           //something like 788.  will return double for the moment
                           return token(PlsqlTokenId.DOUBLE_LITERAL);

                        }
                     case EOF:
                        return token(PlsqlTokenId.INT_LITERAL);
                     default:
                        if (!Character.isDigit(c)) {
                           input.backup(1);
                           return token(PlsqlTokenId.INT_LITERAL);
                        }
                  }
               }

            case '.':
               if (checkDouble()) {
                  return token(PlsqlTokenId.DOUBLE_LITERAL);
               } else {
                  return token(PlsqlTokenId.DOT);
               }
            case '\'':
               if ('q' == pre
                       || 'Q' == pre) {
                  int delimiter = getDelimiter();
                  while (true) {
                     final int next = input.read();
                     if (next == delimiter) {
                        switch (input.read()) {
                           case '\'':
                              //string
                              return token(PlsqlTokenId.STRING_LITERAL);
                           case EOF:
                              //Incomplete string
                              input.backup(1);
                              return token(PlsqlTokenId.INCOMPLETE_STRING);
                        }
                        input.backup(1);
                     } else if (next == EOF) {
                        //Incomplete string
                        input.backup(1);
                        return token(PlsqlTokenId.INCOMPLETE_STRING);
                     }
                  }
               } else {
                  while (true) {
                     switch (input.read()) {
                        case '\'':
                           //string
                           return token(PlsqlTokenId.STRING_LITERAL);
                        case EOF:
                           //Incomplete string
                           input.backup(1);
                           return token(PlsqlTokenId.INCOMPLETE_STRING);
                     }
                  }
               }
            case '"':
               while (true) {
                  switch (input.read()) {
                     case '"':
                        //string
                        return token(PlsqlTokenId.STRING_LITERAL);
                     case EOF:
                        //Incomplete string
                        input.backup(1);
                        return token(PlsqlTokenId.INCOMPLETE_STRING);
                  }
               }

            case EOF:
               return null;

            default:
               if (c >= 0x80) { // lowSurr ones already handled above
                  c = translateSurrogates(c);
                  if (Character.isJavaIdentifierStart(c)) {
                     return finishIdentifier();
                  }
                  if (Character.isWhitespace(c)) {
                     return finishWhitespace();
                  }
               }

               // Invalid char
               return token(PlsqlTokenId.ERROR);
         }
      }
   }

   private int getDelimiter() {
      char delimiter = (char) input.read();
      if (delimiterMap.containsKey(delimiter)) {
         delimiter = delimiterMap.get(delimiter);
      }
      return delimiter;
   }

   private PlsqlTokenId parseJavaSource() {
      int c = 0;
      String line = "";
      while (c != EOF) {
         c = input.read();
         if (c == '\n') {
            line = "";
         } else {
            line = line + (char) c;
         }

         if (c == '/') {
            final String rest = readLine();
            line = line + rest;
            if (line.trim().equals("/")) {
               input.backup(1);
               return PlsqlTokenId.JAVA_SOUCE;
            }
         }
      }

      return PlsqlTokenId.JAVA_SOUCE;
   }

   /**
    * Method that will read block comments
    * @return
    */
   private Token<PlsqlTokenId> readBlockComment() {
      int c = 0;
      while (c != EOF) {
         c = input.read();
         if (c == '*') {
            switch (input.read()) {
               case '/': // in block comment
                  return token(PlsqlTokenId.BLOCK_COMMENT);
               default:
                  input.backup(1);
            }
         }
      }

      return token(PlsqlTokenId.INVALID_COMMENT_END);
   }

   /**
    * Method that will read through the comment line
    */
   private String readLine() {
      int c = 0;
      String line = "";
      while ((input.consumeNewline() == false) && (c != EOF)) {
         c = input.read();
         line = line + (char) c;
      }
      //get consumed \n
      if (c != EOF) {
         input.backup(1);
      }
      return line;
   }

   /**
    * Method that will check the end of the double literal
    * If the following character is not a digit will return the token as
    * dot 
    * @return true if double
    */
   private boolean checkDouble() {
      int ch = input.read();
      int count = 0;

      while (Character.isDigit(ch)) {
         ch = input.read();
         count++;
      }

      //we need to back up the last character that we read      
      input.backup(1);
      if (count > 0) {
         return true;
      } else {
         return false;
      }
   }

   /**
    * Check whether this is an identifier or operator
    * @param pre
    * @return
    */
   private Token<PlsqlTokenId> checkIdentifierOrOperator(final int pre) {
      //We need to get the previous character of * or !
      if ((pre == '\t') || (pre == '\n')
              || (pre == 0x0b) || (pre == '\f')
              || (pre == '\r') || (pre == 0x1c)
              || (pre == 0x1d) || (pre == 0x1e)
              || (pre == 0x1f)) {
         final int ch = input.read();
         if (!Character.isJavaIdentifierPart(ch)) {
            input.backup(1);
            return token(PlsqlTokenId.OPERATOR);
         } else {
            return finishIdentifier();
         }
      }
      return token(PlsqlTokenId.OPERATOR);
   }

   private int translateSurrogates(int c) {
      if (Character.isHighSurrogate((char) c)) {
         final int lowSurr = input.read();
         if (lowSurr != EOF && Character.isLowSurrogate((char) lowSurr)) {
            // c and lowSurr form the integer unicode char.
            c = Character.toCodePoint((char) c, (char) lowSurr);
         } else {
            // Otherwise it's error: Low surrogate does not follow the high one.
            // Leave the original character unchanged.
            // As the surrogates do not belong to any
            // specific unicode category the lexer should finally
            // categorize them as a lexical error.
            input.backup(1);
         }
      }
      return c;
   }

   private Token<PlsqlTokenId> finishWhitespace() {
      while (true) {
         final int c = input.read();
         // There should be no surrogates possible for whitespace
         // so do not call translateSurrogates()
         if (c == EOF || !Character.isWhitespace(c)) {
            input.backup(1);
            return token(PlsqlTokenId.WHITESPACE);
         }
      }
   }

   private Token<PlsqlTokenId> finishIdentifier() {
      return finishIdentifier(input.read());
   }

   private Token<PlsqlTokenId> finishIdentifier(int c) {
      while (true) {
         if (c == EOF || !Character.isJavaIdentifierPart(c = translateSurrogates(c))) {
            // For surrogate 2 chars must be backed up
            input.backup((c >= Character.MIN_SUPPLEMENTARY_CODE_POINT) ? 2 : 1);
            return token(PlsqlTokenId.IDENTIFIER);
         }
         c = input.read();
      }
   }

   private Token<PlsqlTokenId> token(final PlsqlTokenId id) {
      final Token<PlsqlTokenId> t = tokenFactory.createToken(id);
      lastToken = t;
      return t;
   }

   @Override
   public void release() {
   }
   /**
    * A Set of keywords
    */
   private static Set<String> keywords = new HashSet<String>();
   private static Set<String> sqlPlus = new HashSet<String>();

   static {
      populateKeywords();
      populateSQLPlus();
   }

   /**
    * populates the Set of keywords from the property in the
    * resource bundle
    */
   private static void populateKeywords() {
      final String fullList = NbBundle.getBundle(PlsqlLexer.class).getString("LIST_PLSQLKeywords");
      final StringTokenizer st = new StringTokenizer(fullList, ","); // NOI18N
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         token = token.toUpperCase(Locale.ENGLISH).trim();

         if (!keywords.contains(token)) {
            keywords.add(token);
         }
      }
   }

   /**
    * populates the Set of keywords from the property in the
    * resource bundle
    */
   private static void populateSQLPlus() {
      final String fullList = NbBundle.getBundle(PlsqlLexer.class).getString("LIST_SQLPLUS");
      final StringTokenizer st = new StringTokenizer(fullList, ","); // NOI18N
      while (st.hasMoreTokens()) {
         String token = st.nextToken();
         token = token.toUpperCase(Locale.ENGLISH).trim();

         if (!sqlPlus.contains(token)) {
            sqlPlus.add(token);
         }
      }
   }

   /**
    * Tries to match the specified sequence of characters to a Plsql
    * keyword.
    * @param candidate
    * @return
    */
   private PlsqlTokenId matchToken(String candidate) {
      candidate = candidate.toUpperCase(Locale.ENGLISH);

      if (keywords.contains(candidate)) {
         if (candidate.equalsIgnoreCase("JAVA")) {
            final int c = input.read();
            final String word = getWordToken(c);
            if (word.trim().equalsIgnoreCase("SOURCE")) {
               return parseJavaSource();
            } else {
               input.backup(word.length());
            }
         }
         return PlsqlTokenId.KEYWORD;
      } else if (sqlPlus.contains(candidate)) {
         if (lastToken == null || lastToken.text() == null || lastToken.toString().contains("\n")) {
            if (candidate.equalsIgnoreCase("EXECUTE")) {
               int c = input.read();
               final String word = getWordToken(c);
               if (word.trim().equalsIgnoreCase("IMMEDIATE")) {
                  input.backup(word.length());
                  return PlsqlTokenId.KEYWORD;
               }
               input.backup(word.length()); //If line break is before the next word
            }
            return PlsqlTokenId.SQL_PLUS;
         } else {
            return PlsqlTokenId.KEYWORD;
         }
      } else if (candidate.equalsIgnoreCase("REM")) {
         //REM is a line comment
         readLine();
         return PlsqlTokenId.LINE_COMMENT;
      }

      return PlsqlTokenId.IDENTIFIER;
   }

   /**
    * Return character stream containing 'a-z' 'A-Z' '_'
    * @param c
    * @return
    */
   private String getWordToken(final int c) {
      String word = "";
      word = Character.toString((char) c);
      int ch = input.read();

      while ((Character.isLetter(ch)) || (Character.isDigit(ch)) || (ch == '_')
              || (ch == '$') || (ch == '&') || (ch == '#')) {
         word = word + Character.toString((char) ch);
         ch = input.read();
      }

      //we need to back up the last character that we read      
      input.backup(1);
      return word;
   }
}
