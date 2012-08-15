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

import org.netbeans.modules.plsql.annotation.*;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.Action;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.util.NbBundle;

/**
 * Check for correct method param oder  (OUT, IN OUT or IN, DEFAULT) and warn
 */
public class PlsqlWrongParamOrderAnnotation extends PlsqlBlockAnnotation {

   private PlsqlWrongParamOrderAnnotation() {//NOPMD - Used for configuration only
   }

   public static PlsqlWrongParamOrderAnnotation getDummyInstance() {
      return new PlsqlWrongParamOrderAnnotation();
   }

   public PlsqlWrongParamOrderAnnotation(final int offset, final int ignoreOffset) {
      super();
      this.offset = offset;
      this.ignoreOffset = ignoreOffset;
      this.category = GENERAL;
   }

   @Override
   public String getErrorToolTip() {
      return NbBundle.getMessage(this.getClass(), "wrong_param_order_annotation");
   }

   @Override
   public String getAnnotationType() {
      return "Plsql-wrong-param-order-annotation";
   }

   @Override
   public String getShortDescription() {
      return NbBundle.getMessage(this.getClass(), "wrong_param_order_annotation");
   }

   @Override
   public void evaluateAnnotation(final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, final Document doc, final PlsqlBlock block, final Document specDoc, final PlsqlBlockFactory specBlockFac) {
      final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      final TokenSequence<PlsqlTokenId> tokens = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      final int ignoreMarkerOffset = isIgnoreSpecified(doc, block.getStartOffset(), getIgnoreKey(), true);
      boolean exists = false;

      if (tokens != null) {
         int startParam = -1;
         int endParam = -1;
         tokens.move(block.getStartOffset());
         Token<PlsqlTokenId> token = tokens.token();
         String param = "";
         final List<Integer> typeOrder = new ArrayList<Integer>();
         while (tokens.moveNext() && tokens.offset() < block.getEndOffset()) {
            token = tokens.token();
            if ((token.text().toString().equals(",")) || (token.text().toString().equals(")"))) {
               final int type = getParamOrderNo(param);
               typeOrder.add(type);
               param = "";
            } else if (token.toString().equals("(")) {
               startParam = tokens.offset();
            }

            if ((token.text().toString().equalsIgnoreCase("IS")) || (token.text().toString().equals(")")) || (token.text().toString().equals(";"))) {
               endParam = tokens.offset();
               break;
            }

            if (token.id() == PlsqlTokenId.KEYWORD) {
               param = param + " " + token.text().toString().toUpperCase(Locale.ENGLISH);
            }
         }

         //compare type order
         int pre = 0;
         for (Integer type : typeOrder) {
            if (pre > type) {
               //Means incorrect order
               if (startParam != -1 && endParam != -1) {
                  exists = true;
                  final PlsqlWrongParamOrderAnnotation annotation = new PlsqlWrongParamOrderAnnotation(block.getStartOffset(), block.getStartOffset());
                  if (!isIgnoreAlowed() || -1 == ignoreMarkerOffset) {
                     PlsqlAnnotationUtil.addAnnotation(annotation, annotationsToAdd);
                  }
                  checkIgnoreAnnotation(annotationsToAdd, getIgnoreKey(), ignoreMarkerOffset, exists);
                  break;
               }
            }
            pre = type;
         }
      }      
   }

   private int getParamOrderNo(final String param) {
      int type = 2;  //If IN/OUT/IN OUT is not specified then its an IN param
      if (param.contains("IN OUT") || param.contains("IN")) {
         type = 2;
      } else if (param.contains("OUT")) {
         type = 1;
      }

      //DEFAULT parameters should be there at the end
      if (param.contains("DEFAULT")) {
         type = 3;
      }

      return type;
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
      return "WrongParamOrder";
   }
}
