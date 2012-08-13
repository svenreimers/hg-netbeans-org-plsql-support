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
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.text.Document;
import org.openide.util.NbBundle;

/**
 * Check for missing return statements in functions
 * @author YADHLK
 */
public class PlsqlFunctionReturnAnnotation extends PlsqlBlockAnnotation {

   private PlsqlFunctionReturnAnnotation() {
   } //Used for configuration only

   public static PlsqlFunctionReturnAnnotation getDummyInstance() {
      return new PlsqlFunctionReturnAnnotation();
   }

   public PlsqlFunctionReturnAnnotation(int offset, int ignoreOffset, int severity) {
      this.offset = offset;
      this.ignoreOffset = ignoreOffset;
      this.severity = severity;
      this.category = GENERAL;
   }

   @Override
   public String getErrorToolTip() {
      return NbBundle.getMessage(this.getClass(), "function_return_annotation");
   }

   @Override
   public String getAnnotationType() {
      return "Plsql-function-return-annotation";
   }

   @Override
   public String getShortDescription() {
       if (this.severity == WARNING) {
           return NbBundle.getMessage(this.getClass(), "function_return_annotation_warning");
       } else {
           return NbBundle.getMessage(this.getClass(), "function_return_annotation");
       }
   }

    @Override
    public void evaluateAnnotation(Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, Document doc, PlsqlBlock block, Document specDoc, PlsqlBlockFactory specBlockFac) {
        int isReturn = PlsqlMethodAnnotationUtil.isReturnExist(doc, block);
        int ignoreMarkerOffset = isIgnoreSpecified(doc, block.getStartOffset(), getIgnoreKey(), true);
        boolean exists = false;

        if (isReturn > 0) {
            exists = true;
            PlsqlFunctionReturnAnnotation annotation = new PlsqlFunctionReturnAnnotation(block.getStartOffset(), block.getStartOffset(), isReturn);
            if (annotation != null) {
                if (!isIgnoreAlowed() || -1 == ignoreMarkerOffset) {
                    PlsqlAnnotationUtil.addAnnotation(annotation, annotationsToAdd);
                }
                checkIgnoreAnnotation(annotationsToAdd, getIgnoreKey(), ignoreMarkerOffset, exists);
            }
        }
    }

   @Override
   public Action[] getActions() {
      if (isIgnoreAlowed())
         return new Action[]{new AddIgnoreMarkerAction()};
      else
         return new Action[0];
   }

   @Override
   public boolean isIgnoreAlowed() {
      return true;
   }

   @Override
   public String getIgnoreKey() {
      return "FunctionReturn";
   }
}
