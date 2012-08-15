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

import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.List;
import java.util.Map;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.Token;
import org.netbeans.modules.plsql.annotation.PlsqlAnnotationManager;
import org.netbeans.modules.plsql.annotation.PlsqlAnnotationUtil;

/**
 * Annotation class for annotations based on tokens
 * @author YADHLK
 */
public abstract class PlsqlTokenAnnotation extends PlsqlAnnotation {

   public abstract void evaluateAnnotation(Map<Integer, List<PlsqlAnnotation>> annotationsToAdd,
           Document doc, Token<PlsqlTokenId> token, int tokenOffset, int endParse, Object obj);   
   
    /**
    * Check and remove/add the ignore marker annotation
    * @param annotationsToAdd
    * @param ignoreKey
    * @param ignoreMarkerOffset
    * @param exists
    */
   public void checkIgnoreAnnotation(final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, final String ignoreKey, final int ignoreMarkerOffset, final boolean exists) {
      if (!exists && -1 != ignoreMarkerOffset) {
         PlsqlAnnotationUtil.addAnnotation(new PlsqlWrongIgnoreMarkerAnnotation(ignoreKey, ignoreMarkerOffset), annotationsToAdd);
      } else if (exists && -1 != ignoreMarkerOffset) {
         //Remove the WrongIgnoreMarkerAnnotation if existing
         final JTextComponent comp = EditorRegistry.lastFocusedComponent();
         if (comp == null) {
            return;
         }

         final Document doc = comp.getDocument();
         if (doc == null) {
            return;
         }

         final PlsqlAnnotationManager annotationManager = PlsqlAnnotationUtil.getAnnotationManager(doc);
         if (annotationManager != null) {
            annotationManager.removeAnnotation(ignoreMarkerOffset, "Plsql-wrong-ignore-marker-annotation");
         }
      }
   }
}
