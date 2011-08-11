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

import junit.framework.TestCase;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.lexer.TokenUtilities;
import org.netbeans.lib.lexer.LexerUtilsConstants;

/**
 * Various utilities related to lexer's and token testing.
 *
 * @author mmetelka
 */
public final class LexerTestUtilities {

    /**
     * Compare <code>TokenSequence.token()</code> to the given
     * token id, text and offset.
     *
     * @param offset expected offset. It may be -1 to prevent offset testing.
     */
    public static void assertTokenEquals(String message, TokenSequence<?> ts, TokenId id, String text, int offset) {
        message = messagePrefix(message);
        Token<?> t = ts.token();
        TestCase.assertNotNull("Token is null", t);
        TokenId tId = t.id();
        TestCase.assertEquals(message + "Invalid token.id() for text=\"" + debugTextOrNull(t.text()) + '"', id, tId);
        CharSequence tText = t.text();
        if (text != null) {
            assertTextEquals(message + "Invalid token.text() for id=" + LexerUtilsConstants.idToString(id), text, tText);
            // The token's length must correspond to text.length()
            TestCase.assertEquals(message + "Invalid token.length()", text.length(), t.length());
        }

        if (offset != -1) {
            int tsOffset = ts.offset();
            TestCase.assertEquals(message + "Invalid tokenSequence.offset()", offset, tsOffset);

            // It should also be true that if the token is non-flyweight then
            // ts.offset() == t.offset()
            // and if it's flyweight then t.offset() == -1
            int tOffset = t.offset(null);
            assertTokenOffsetMinusOneForFlyweight(t.isFlyweight(), tOffset);
            if (!t.isFlyweight()) {
                assertTokenOffsetsEqual(message, tOffset, offset);
            }
        }
    }

    public static void assertTokenOffsetsEqual(String message, int offset1, int offset2) {
        if (offset1 != -1 && offset2 != -1) { // both non-flyweight
            TestCase.assertEquals(messagePrefix(message)
                    + "Offsets equal", offset1, offset2);
        }
    }

    private static void assertTokenOffsetMinusOneForFlyweight(boolean tokenFlyweight, int offset) {
        if (tokenFlyweight) {
            TestCase.assertEquals("Flyweight token => token.offset()=-1", -1, offset);
        } else { // non-flyweight
            TestCase.assertTrue("Non-flyweight token => token.offset()!=-1 but " + offset, (offset != -1));
        }
    }

    /**
     * Assert that the next token in the token sequence
     */
    public static void assertNextTokenEquals(TokenSequence<?> ts, TokenId id, String text) {
        assertNextTokenEquals(null, ts, id, text);
    }

    /**
     * Assert that the next token in the token sequence
     */
    public static void assertNextTokenEquals(TokenSequence<?> ts, TokenId id) {
        assertNextTokenEquals(null, ts, id, null);
    }

    public static void assertNextTokenEquals(String message, TokenSequence<?> ts, TokenId id, String text) {
        String messagePrefix = messagePrefix(message);
        TestCase.assertTrue(messagePrefix + "No next token available", ts.moveNext());
        assertTokenEquals(message, ts, id, text, -1);
    }

    /**
     * Compare whether the two character sequences represent the same text.
     */
    public static boolean textEquals(CharSequence text1, CharSequence text2) {
        return TokenUtilities.equals(text1, text2);
    }

    public static void assertTextEquals(String message, CharSequence expected, CharSequence actual) {
        if (!textEquals(expected, actual)) {
            TestCase.fail(messagePrefix(message)
                    + " expected:\"" + expected + "\" but was:\"" + actual + "\"");
        }
    }

    private static String messagePrefix(String message) {
        if (message != null) {
            message = message + ": ";
        } else {
            message = "";
        }
        return message;
    }

    /**
     * Return the given text as String
     * translating the special characters (and '\') into escape sequences.
     *
     * @param text non-null text to be debugged.
     * @return non-null string containing the debug text.
     */
    public static String debugText(CharSequence text) {
        return TokenUtilities.debugText(text);
    }

    /**
     * Return the given text as String
     * translating the special characters (and '\') into escape sequences.
     *
     * @param text non-null text to be debugged.
     * @return non-null string containing the debug text or "<null>".
     */
    public static String debugTextOrNull(CharSequence text) {
        return (text != null) ? debugText(text) : "<null>";
    }
}
