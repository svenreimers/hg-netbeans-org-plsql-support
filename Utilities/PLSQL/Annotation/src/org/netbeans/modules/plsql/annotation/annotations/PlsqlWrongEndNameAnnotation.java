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
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.util.NbBundle;

/**
 * Check whether the method name is wrong at the end of a method
 * @author YADHLK
 */
public class PlsqlWrongEndNameAnnotation extends PlsqlBlockAnnotation {

   private String methodName;
   private String existingName;

   public PlsqlWrongEndNameAnnotation(String methodName, String existingName, int offset, int ignoreOffset) {
      this.methodName = methodName;
      this.existingName = existingName;
      this.offset = offset;
      this.ignoreOffset = ignoreOffset;
      this.severity = ERROR;
      this.category = GENERAL;
   }

   public static PlsqlWrongEndNameAnnotation getDummyInstance() {
      return new PlsqlWrongEndNameAnnotation();
   }

   private PlsqlWrongEndNameAnnotation() {
   } //Used for configuration only

   @Override
   public String getErrorToolTip() {
      return NbBundle.getMessage(this.getClass(), "wrong_end_name_annotation");
   }

   @Override
   public String getAnnotationType() {
      return "Plsql-wrong-end-name-annotation";
   }

   @Override
   public String getShortDescription() {
      return NbBundle.getMessage(this.getClass(), "wrong_end_name_annotation");
   }

   @Override
   public void evaluateAnnotation(Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, Document doc, PlsqlBlock block, Document specDoc, PlsqlBlockFactory specBlockFac) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      PlsqlBlockFactory fac = PlsqlParserUtil.getBlockFactory(doc);
      int ignoreMarkerOffset = isIgnoreSpecified(doc, block.getStartOffset(), getIgnoreKey(), true);
      boolean exists = false;

      if (ts != null && fac != null) {
         ts.move(block.getEndOffset());
         Token<PlsqlTokenId> token = ts.token();
         String existing = null;
         int tokenOffset = -1;

         while (ts.movePrevious() && ts.offset() > block.getStartOffset()) {
            token = ts.token();

            if (token.toString().equalsIgnoreCase("END")) {
               if (existing != null && !(existing.equals(block.getName()) || existing.equals(block.getAlias()))) {
                  exists = true;
                  PlsqlWrongEndNameAnnotation endNameAnnotation = new PlsqlWrongEndNameAnnotation(block.getName(), existing, tokenOffset, block.getStartOffset());
                  if (!isIgnoreAlowed() || -1 == ignoreMarkerOffset) {
                     PlsqlAnnotationUtil.addAnnotation(endNameAnnotation, annotationsToAdd);
                  }
               }
               break;
            } else if (token.id() == PlsqlTokenId.IDENTIFIER || token.id() == PlsqlTokenId.KEYWORD) { //because there are some functions with keyword names (close,cancel etc)
               existing = token.toString();
               tokenOffset = ts.offset();
            }
         }
      }

      checkIgnoreAnnotation(annotationsToAdd, getIgnoreKey(), ignoreMarkerOffset, exists);
   }

   @Override
   public Action[] getActions() {
      if (isIgnoreAlowed()) {
         return new Action[]{new ChangeNameAction(), new AddIgnoreMarkerAction()};
      } else {
         return new Action[]{new ChangeNameAction()};
      }
   }

   @Override
   public boolean isIgnoreAlowed() {
      return true;
   }

   @Override
   public String getIgnoreKey() {
      return "WrongEndName";
   }

   private class ChangeNameAction extends AbstractAction {

      @Override
      public String toString() {
         return "Change the function/procedure end name";
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         JTextComponent comp = EditorRegistry.lastFocusedComponent();
         if (comp == null) {
            return;
         }
         Document doc = comp.getDocument();
         if (doc == null) {
            return;
         }

         PlsqlAnnotationManager annotationManager = PlsqlAnnotationUtil.getAnnotationManager(doc);
         if (annotationManager != null) {
            if (PlsqlFileAnnotationUtil.changeLineOfOffset(doc, offset, existingName, methodName)) {
               annotationManager.removeAnnotation(offset, getAnnotationType());
            }
         }
      }
   }
}
