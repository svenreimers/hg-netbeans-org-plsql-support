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
package org.netbeans.modules.plsql.annotation;

import org.netbeans.modules.plsql.annotation.annotations.PlsqlAnnotation;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 * Action that valitates files in the component folder and provide a summary
 * @author YADHLK
 */
public class PlsqlValidateFilesSummary extends CookieAction {

   @Override
   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   @Override
   protected Class<?>[] cookieClasses() {
      return new Class[]{DataObject.class};
   }

   @Override
   protected void performAction(final Node[] activatedNodes) {
      RequestProcessor.getDefault().post(new Runnable() {

         @Override
         public void run() {
            ProgressHandle handle = ProgressHandleFactory.createHandle("Validating files of the component...");
            handle.start();
            try {
               InputOutput io = IOProvider.getDefault().getIO(NbBundle.getMessage(PlsqlValidateFilesSummary.class, "CTL_PlsqlValidateFilesSummary"), false);
               io.select();
               io.getOut().reset();
               io.getErr().reset();

               for (Node node : activatedNodes[0].getChildren().getNodes()) {
                  //Init annotations
                  DataObject dataObject = node.getLookup().lookup(DataObject.class);
                  if (dataObject == null) {
                     continue;
                  }

                  PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
                  if (annotationManager != null) {
                     io.getOut().print(dataObject.getPrimaryFile().getNameExt());
                     annotationManager.initAnnotations(dataObject);
                     Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
                     if (annotations.isEmpty()) {
                        io.getOut().print(" no errors/warnings found.\n");
                     } else {
                        printAnnotations(annotations, io);
                     }
                  }
               }
            } catch (IOException ex) {
               Exceptions.printStackTrace(ex);
            } catch (RuntimeException e) {
            } finally {
               handle.finish();
            }
         }
      });
   }

   @Override
   public String getName() {
      return NbBundle.getMessage(PlsqlValidateFilesAction.class, "CTL_PlsqlValidateFilesSummary");
   }

   @Override
   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   private void printAnnotations(Map<Integer, List<PlsqlAnnotation>> annotations, InputOutput io) {
      int errors = 0;
      int warnings = 0;
      for (Integer offset : annotations.keySet()) {
         List<PlsqlAnnotation> lstAnnotation = annotations.get(offset);
         for (PlsqlAnnotation annotation: lstAnnotation) {
            int severity = annotation.getSeverity();
            if (severity == PlsqlAnnotation.ERROR)
               errors++;
            else
               warnings++;
         }         
      }
      io.getOut().print(" " + errors + " errors and " + warnings + " warnings found.\n");
   }
}
