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
package org.netbeans.modules.plsql.annotation.annotations;

import org.netbeans.modules.plsql.annotation.PlsqlAnnotationUtil;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.util.NbBundle;

/**
 * Check whether there are invalid conditions in cursor, where clause
 * e.g.  <column> <operator> <column> where operators can be “=”, “!=”, “<>”, “>=”, “>”, “<=”, “<”
 * @author YADHLK
 */
public class PlsqlCursorWhereAnnotation extends PlsqlBlockAnnotation {

   public PlsqlCursorWhereAnnotation(int offset, int ignoreOffset) {
      this.offset = offset;
      this.ignoreOffset = ignoreOffset;
      this.severity = ERROR;
      this.category = GENERAL;
   }

   public static PlsqlCursorWhereAnnotation getDummyInstance() {
      return new PlsqlCursorWhereAnnotation();
   }

   private PlsqlCursorWhereAnnotation() {
   } //Used for configuration only

   @Override
   public String getErrorToolTip() {
      return NbBundle.getMessage(this.getClass(), "cursor_where_clause_annotation");
   }

   @Override
   public String getAnnotationType() {
      return "Plsql-cursor-where-clause-annotation";
   }

   @Override
   public String getShortDescription() {
      return NbBundle.getMessage(this.getClass(), "cursor_where_clause_annotation");
   }

   @Override
   public void evaluateAnnotation(Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, Document doc, PlsqlBlock block, Document specDoc, PlsqlBlockFactory specBlockFac) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      int ignoreMarkerOffset = isIgnoreSpecified(doc, block.getStartOffset(), getIgnoreKey(), true);
      boolean exists = false;

      if (ts != null) {
         ts.move(block.getStartOffset());
         Token<PlsqlTokenId> token = ts.token();
         String param1 = "";
         String param2 = "";
         boolean isCheck = false;
         while (ts.moveNext() && ts.offset() < block.getEndOffset()) {
            token = ts.token();
            if (token.text().toString().equalsIgnoreCase("AND") ||
                    token.text().toString().equalsIgnoreCase("OR")) {
               param1 = "";
               param2 = "";
               isCheck = true;
            } else if (token.text().toString().equalsIgnoreCase("PRIOR")) {
               isCheck = false;
            } else if (isCheck && param1.equals("") && token.id() != PlsqlTokenId.OPERATOR) {
               param1 = getColumn(ts, block.getEndOffset());
            } else if (isCheck && param2.equals("") && token.id() != PlsqlTokenId.OPERATOR) {
               param2 = getColumn(ts, block.getEndOffset());
               if (param1.equalsIgnoreCase(param2) && isIdentifier(param1)) {
                  exists = true;
                  PlsqlCursorWhereAnnotation annotation = new PlsqlCursorWhereAnnotation(getOffset(ts, block.getStartOffset()), block.getStartOffset());
                  if (!isIgnoreAlowed() || -1 == ignoreMarkerOffset) {
                     PlsqlAnnotationUtil.addAnnotation(annotation, annotationsToAdd);
                  }
                  checkIgnoreAnnotation(annotationsToAdd, getIgnoreKey(), ignoreMarkerOffset, exists);
               }
               param1 = "";
               param2 = "";
               isCheck = false;
            } else if (token.text().toString().equalsIgnoreCase("WHERE")) {
               isCheck = true;
            }
         }
      }     
   }

   @Override
   public Action[] getActions() {
      if (isIgnoreAlowed()) {
         return new Action[]{new AddIgnoreMarkerAction()};
      } else {
         return new Action[0];
      }
   }

   @Override
   public boolean isIgnoreAlowed() {
      return true;
   }

   @Override
   public String getIgnoreKey() {
      return "CursorWhereClause";
   }

   private String getColumn(TokenSequence<PlsqlTokenId> ts, int endOff) {
      String param = "";
      Token<PlsqlTokenId> token;
      while (ts.moveNext() && ts.offset() < endOff) {
         token = ts.token();
         if (token.id() == PlsqlTokenId.OPERATOR ||
                 token.id() == PlsqlTokenId.KEYWORD) {
            ts.movePrevious(); //Otherwise we will miss AND/OR
            break;
         }
         if (token.id() != PlsqlTokenId.WHITESPACE &&
                 token.id() != PlsqlTokenId.LPAREN &&
                 token.id() != PlsqlTokenId.RPAREN) {
            param = param + token.toString();
         }
      }

      return param;
   }

   private boolean isIdentifier(String param1) {
      char[] charArray = param1.toCharArray();
      for (char ch : charArray) {
         if (!Character.isJavaIdentifierPart(ch) && ch != '.') {
            return false;
         }
      }
      return true;
   }

   private int getOffset(TokenSequence<PlsqlTokenId> ts, final int startOffset) {
      Token<PlsqlTokenId> token = ts.token();
      while (ts.movePrevious() && ts.offset() >= startOffset) {
         token = ts.token();
         if (token.id() != PlsqlTokenId.WHITESPACE) {
            return ts.offset();
         }
      }

      return startOffset;
   }
}
