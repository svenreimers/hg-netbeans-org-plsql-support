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
package org.netbeans.modules.plsqlsupport.db.ui;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplate;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplateManager;
import org.netbeans.spi.project.CacheDirectoryProvider;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.text.CloneableEditor;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.TopComponent.Registry;

public class SQLCommandWindow {

   public static final String SQL_EXECUTION_FILE_PREFIX = "sqlexecutionwindow";

   public static DataObject createSQLCommandWindow(Node[] activatedNodes, String codeTemplate) {
      Project project = activatedNodes[0].getLookup().lookup(Project.class);
      if(project==null) {
         DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
         if(dataObject==null)
            return null;
         project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
         if (project == null)
            return null;
      }
      DataObject dataObj = null;
      try {
         File tmpFile;
         try {
            tmpFile = File.createTempFile(SQL_EXECUTION_FILE_PREFIX, ".tdb",
                  FileUtil.toFile(project.getLookup().lookup(CacheDirectoryProvider.class).getCacheDirectory()));
         } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
            return null;
         }

         dataObj = DataFolder.find(FileUtil.toFileObject(tmpFile));
         if (dataObj != null) {
               dataObj.getNodeDelegate().setDisplayName(NbBundle.getMessage(SQLCommandWindow.class, "LBL_SQLExecutionWindow"));

            //Get open cookie from the data object and open that
            OpenCookie openCookie = dataObj.getCookie(OpenCookie.class);
            if (openCookie != null) {
               openCookie.open();
               //Get the new pane and paste the code template there
               if(codeTemplate!=null) {
                  EditorCookie editorCookie = dataObj.getCookie(EditorCookie.class);
                  JEditorPane[] panes = editorCookie.getOpenedPanes();
                  if ((panes != null) && (panes.length != 0)) {
                     JEditorPane component = panes[0];
                     if (component != null) {
                        //Ugly workaround for a bug in code templates.
                        //This bug makes it impossible to insert code templates at the first position in a file.
                        //By adding a comment line at the start of the file we avoid this problem for our test blocks...
                        Document doc = component.getDocument();
                        try {
                           doc.insertString(0, "-- Enter values for your parameters. Use enter to move to the next parameter\n", null);
                        } catch (BadLocationException ex) {
                           Exceptions.printStackTrace(ex);
                        }
                        CodeTemplate ct = CodeTemplateManager.get(component.getDocument()).createTemporary(codeTemplate);
                        ct.insert(component);
                     }
                  }
               }
            }
         }
      } catch (DataObjectNotFoundException ex) {
         Exceptions.printStackTrace(ex);
         return null;
      }
      return dataObj;
   }

   private static String getCommandWindowSuffix() {
      int maxIndex = -1;
      String displayNamePrefix = NbBundle.getMessage(SQLCommandWindow.class, "LBL_SQLExecutionWindow");
      Registry registry = TopComponent.getRegistry();
      Set<TopComponent> topComponents = registry.getOpened();
      for(TopComponent component : topComponents) {
         if(component instanceof CloneableEditor && component.isVisible()) {
            String displayName = component.getDisplayName();
            if(displayName!=null && displayName.startsWith(displayNamePrefix)) {
               String suffix = displayName.substring(displayNamePrefix.length());
               try {
                  int candidate = suffix.length()>0 ? Integer.parseInt(suffix.trim()) : 0;
                  maxIndex = candidate > maxIndex ? candidate : maxIndex;
               } catch(NumberFormatException ex) {
                  //do nothing - this isn't one of "our" windows.
               }
            }
         }
      }
      return maxIndex==-1 ? "" : " " + Integer.toString(maxIndex+1) + "";
   }
}
