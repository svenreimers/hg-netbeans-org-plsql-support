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

import org.netbeans.modules.plsql.annotation.PlsqlAnnotationManager;
import org.netbeans.modules.plsql.annotation.PlsqlAnnotationUtil;
import org.netbeans.modules.plsql.annotation.PlsqlFileAnnotationUtil;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.awt.event.ActionEvent;
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
 * Annotation if an ignore marker is found at the wrong place
 * This annotation is not registered through user configuration
 * @author YADHLK
 */
public class PlsqlWrongIgnoreMarkerAnnotation extends PlsqlAnnotation {

   private String type;

   public PlsqlWrongIgnoreMarkerAnnotation(String type, int offset) {
      this.offset = offset;
      this.type = type;
   }

   @Override
   public String getErrorToolTip() {
      return NbBundle.getMessage(this.getClass(), "wrong_ignore_marker_annotation");
   }

   @Override
   public String getAnnotationType() {
      return "Plsql-wrong-ignore-marker-annotation";
   }

   @Override
   public String getShortDescription() {
      return NbBundle.getMessage(this.getClass(), "wrong_ignore_marker_annotation");
   }

   @Override
   public Action[] getActions() {
      return new Action[]{new RemoveMarkerAction()};
   }

   @Override
   public boolean isIgnoreAlowed() {
      return false;
   }

   @Override
   public String getIgnoreKey() {
      return "WrongIgnoreMarker";
   }

   private class RemoveMarkerAction extends AbstractAction {

      @Override
      public String toString() {
         return "Remove the marker";
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         //Change the parameter
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
            String text = getMarkerText(doc, offset);
            if (PlsqlFileAnnotationUtil.removeLineOfOffset(doc, offset, text)) {
               annotationManager.removeAnnotation(offset, getAnnotationType());
            }
         }
      }

      private String getMarkerText(Document doc, int offset) {
         String text = "";
         TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
         @SuppressWarnings("unchecked")
         TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
         
         if (ts != null) {
            ts.move(offset);
            Token<PlsqlTokenId> token = ts.token();
            while (ts.moveNext()) {
               token = ts.token();
               text = text + token.toString();
               if (token.toString().contains("\n"))
                  break;
            }
         }
         
         return text;
      }
   }
}
