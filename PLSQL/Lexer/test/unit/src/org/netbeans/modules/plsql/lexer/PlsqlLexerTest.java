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

import org.junit.Test;
import static org.netbeans.modules.plsql.lexer.PlsqlTokenId.*;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import static org.junit.Assert.*;

/**
 * Test case on PL/SQL token parser
 * @author YADHLK
 */
public class PlsqlLexerTest {

   String tokenText = "DEFINE OBJVERSION = \"ltrim(lpad(to_char(rowversion,'YYYYMMDDHH24MISS'),2000))\"\n"
           + "COMMENT ON COLUMN & VIEW..max_length\n"
           + "\tIS 'FLAGS=AMIU-^DATATYPE=NUMBER^PROMPT=Max Length^';\n"
           + "-----------------------------------------------------------------------------\n"
           + "-------------------- Test comment      --------------------------------------\n"
           + "PROMPT Creating &VIEW view\n"
           + "--@IgnoreGeneratedMethod\n"
           + "CREATE OR REPLACE JAVA SOURCE NAMED \"AvQuery\" AS\n"
           + "class WordData {\nString text;\nString fieldName;\n}\n/\n"
           + "--@ApproveTransactionStatement(yadhlk,2010/03/16 16:28:03)\nCOMMIT;\n/\n"
           + "~\ntemp_ VARCHAR2(20) := 'test';\ntestInt_ INTEGER := 10;\n"
           + "\ntestDoub_ NUMBER := 10.1;\n/*\nExample block comment */\n"
           + "/**\nThis is a block comment too */\n{}\n[]\n"
           + "--@IgnoreGeneratedMethod:with comment\n"
           + "--@AllowTableOrViewAccess ISO_COUNTRY";
   String incompleteComment = "/**\ntest comment *";
   String incompleteString = "\"test string'";

   public PlsqlLexerTest() {
   }

   @Test
   public void testCompleteTokens() {
      System.out.println("Testing complete PL/SQL tokens");
      TokenHierarchy<?> hi = TokenHierarchy.create(tokenText, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.SQL_PLUS, "DEFINE");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "OBJVERSION");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, "=");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.STRING_LITERAL, "\"ltrim(lpad(to_char(rowversion,'YYYYMMDDHH24MISS'),2000))\"");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "COMMENT");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "ON");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "COLUMN");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "&");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "VIEW");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.DOT);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.DOT);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "max_length");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "IS");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.STRING_LITERAL, "'FLAGS=AMIU-^DATATYPE=NUMBER^PROMPT=Max Length^'");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ";");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.LINE_COMMENT, "-----------------------------------------------------------------------------");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.LINE_COMMENT, "-------------------- Test comment      --------------------------------------");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.SQL_PLUS, "PROMPT");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "Creating");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "&VIEW");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "view");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IGNORE_MARKER, "--@IgnoreGeneratedMethod");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "CREATE");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "OR");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "REPLACE");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.JAVA_SOUCE, "JAVA SOURCE NAMED \"AvQuery\" AS\n"
              + "class WordData {\nString text;\nString fieldName;\n}\n");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, "/");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IGNORE_MARKER, "--@ApproveTransactionStatement(yadhlk,2010/03/16 16:28:03)");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "COMMIT");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ";");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, "/");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.ERROR, "~");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "temp_");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "VARCHAR2");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.LPAREN);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.INT_LITERAL, "20");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.RPAREN);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ":");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, "=");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.STRING_LITERAL, "'test'");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ";");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "testInt_");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "INTEGER");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ":");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, "=");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.INT_LITERAL, "10");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ";");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IDENTIFIER, "testDoub_");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.KEYWORD, "NUMBER");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ":");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, "=");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.DOUBLE_LITERAL, "10.1");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.OPERATOR, ";");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.BLOCK_COMMENT, "/*\nExample block comment */");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.BLOCK_COMMENT, "/**\nThis is a block comment too */");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.LBRACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.RBRACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.LBRACKET);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.RBRACKET);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IGNORE_MARKER, "--@IgnoreGeneratedMethod:with comment");
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.IGNORE_MARKER, "--@AllowTableOrViewAccess ISO_COUNTRY");
      assertFalse(ts.moveNext());
   }

   @Test
   public void testIncompleteTokens() {
      System.out.println("Testing incomplete PL/SQL tokens");
      TokenHierarchy<?> hi = TokenHierarchy.create(incompleteString, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.INCOMPLETE_STRING, "\"test string'");
      assertFalse(ts.moveNext());

      hi = TokenHierarchy.create(incompleteComment, PlsqlTokenId.language());
      ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, PlsqlTokenId.INVALID_COMMENT_END, "/**\ntest comment *");
      assertFalse(ts.moveNext());
   }

   @Test
   public void testAlternateDelimiter() {
      System.out.println("Testing alternate delimiter PL/SQL");
      final String alternateDelimiterString = "SELECT q'!Patrick's book!' \"alternative\", 'Patrick'||''''||'s book' \"Normal\" from dual";
      TokenHierarchy<?> hi = TokenHierarchy.create(alternateDelimiterString, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "SELECT");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "q'!Patrick's book!'");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "\"alternative\"");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, ",");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "'Patrick'");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, "|");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, "|");
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "''");
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "''");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, "|");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, "|");
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "'s book'");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "\"Normal\"");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "from");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, IDENTIFIER, "dual");
      assertFalse(ts.moveNext());
   }

   @Test
   public void testAlternateDelimiter2() {
      System.out.println("Testing alternate delimiter 2 PL/SQL");
      final String alternateDelimiterString = "SELECT q'!Patrick's ! book!!' \"patrick\" from dual;";
      TokenHierarchy<?> hi = TokenHierarchy.create(alternateDelimiterString, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "SELECT");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "q'!Patrick's ! book!!'");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "\"patrick\"");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "from");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, IDENTIFIER, "dual");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, ";");
      assertFalse(ts.moveNext());
   }

   @Test
   public void testAlternateDelimiterInequalitySigns() {
      System.out.println("Testing alternate delimiter inequality signs PL/SQL");
      final String alternateDelimiterString = "SELECT q'<Patrick's ! book!>' \"patrick\" from dual;";
      TokenHierarchy<?> hi = TokenHierarchy.create(alternateDelimiterString, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "SELECT");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "q'<Patrick's ! book!>'");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "\"patrick\"");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "from");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, IDENTIFIER, "dual");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, ";");
      assertFalse(ts.moveNext());
   }

   @Test
   public void testAlternateDelimiterSquareBrackets() {
      System.out.println("Testing alternate delimiter Square Brackets PL/SQL");
      final String alternateDelimiterString = "SELECT q'[Patrick's ! book!]' \"patrick\" from dual;";
      TokenHierarchy<?> hi = TokenHierarchy.create(alternateDelimiterString, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "SELECT");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "q'[Patrick's ! book!]'");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "\"patrick\"");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "from");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, IDENTIFIER, "dual");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, ";");
      assertFalse(ts.moveNext());
   }

   @Test
   public void testAlternateDelimiterRoundBrackets() {
      System.out.println("Testing alternate delimiter Round Brackets PL/SQL");
      final String alternateDelimiterString = "SELECT q'(Patrick's ! book!)' \"patrick\" from dual;";
      TokenHierarchy<?> hi = TokenHierarchy.create(alternateDelimiterString, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "SELECT");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "q'(Patrick's ! book!)'");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "\"patrick\"");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "from");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, IDENTIFIER, "dual");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, ";");
      assertFalse(ts.moveNext());
   }

   @Test
   public void testAlternateDelimiterCurlyBrackets() {
      System.out.println("Testing alternate delimiter Curly Brackets PL/SQL");
      final String alternateDelimiterString = "SELECT q'{Patrick's ! book!}' \"patrick\" from dual;";
      TokenHierarchy<?> hi = TokenHierarchy.create(alternateDelimiterString, PlsqlTokenId.language());
      TokenSequence<PlsqlTokenId> ts = hi.tokenSequence(PlsqlTokenId.language());
      assertNotNull(hi);
      assertNotNull(ts);
      //printTokens(ts);
      ts.moveStart();
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "SELECT");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "q'{Patrick's ! book!}'");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, STRING_LITERAL, "\"patrick\"");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, KEYWORD, "from");
      LexerTestUtilities.assertNextTokenEquals(ts, WHITESPACE);
      LexerTestUtilities.assertNextTokenEquals(ts, IDENTIFIER, "dual");
      LexerTestUtilities.assertNextTokenEquals(ts, OPERATOR, ";");
      assertFalse(ts.moveNext());
   }

   private void printTokens(TokenSequence<PlsqlTokenId> ts) {
      Token<PlsqlTokenId> token = null;
      while (ts.moveNext()) {
         token = ts.token();
         System.out.println("Token " + token.id().name() + " Text:" + token.toString());
      }
   }
}
