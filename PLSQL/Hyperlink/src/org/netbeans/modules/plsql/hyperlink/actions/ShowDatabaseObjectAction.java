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
package org.netbeans.modules.plsql.hyperlink.actions;

import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.hyperlink.PlsqlGoToImplAction;
import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import java.awt.EventQueue;
import java.awt.Frame;
import javax.swing.JOptionPane;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.netbeans.modules.plsql.utilities.ui.DbObjectPresenterPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.util.actions.SystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author csamlk
 */
public class ShowDatabaseObjectAction extends CookieAction {

   private Node[] activatedNodes;
   private Project project;
   private String objName;
   private Object errorMsg;

   @Override
   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   @Override
   protected Class<?>[] cookieClasses() {
      return new Class[]{Project.class};
   }

   @Override
   protected void performAction(Node[] arg0) {
      project = activatedNodes[0].getLookup().lookup(Project.class);
      final DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(project);

      final DatabaseContentManager cache = DatabaseContentManager.getInstance(connectionProvider.getTemplateConnection());

      EventQueue.invokeLater(new Runnable() {

         @Override
         public void run() {            
            DbObjectPresenterPanel dbObjPanel = new DbObjectPresenterPanel(project, "Database Table, View or Package Name:", "");
            DialogDescriptor dbObjDlg = new DialogDescriptor(dbObjPanel.getPanel(), NbBundle.getMessage(ShowDatabaseObjectAction.class, "LBL_ShowDatabaseDialogTitle"));
            if (DialogDescriptor.OK_OPTION == DialogDisplayer.getDefault().notify(dbObjDlg)) {
               objName = dbObjPanel.getInputText();
               DatabaseConnection databaseConnection = connectionProvider.getPooledDatabaseConnection(false);
               try {
                  if (cache.isView(objName)) {
                     try {
                        DataObject obj = null;
                        String aliasOf = cache.getViewForSynonym(objName);
                        if (aliasOf != null) {
                           objName = aliasOf;
                        }
                        obj = PlsqlFileUtil.openExistingFile(null, objName, VIEW, project);
                        if (obj == null) {
                           PlsqlHyperlinkUtil.openAsTempFile(objName, VIEW, databaseConnection, project, null);
                        } else {
                           PlsqlHyperlinkUtil.setCaretOfView(obj, objName);
                        }
                     } catch (NotConnectedToDbException ex) {
                        Exceptions.printStackTrace(ex);
                     }
                  } else if (cache.isPackage(objName, databaseConnection)) {
                     PlsqlGoToImplAction action = SystemAction.get(PlsqlGoToImplAction.class);
                     action.goToPackage(objName, project, "", 1);
                  } else if (cache.isTable(objName)) {
                     try {
                        if(PlsqlFileUtil.openExistingFile(null, objName, TABLE, project)==null) {
                           PlsqlHyperlinkUtil.openAsTempFile(objName, TABLE, databaseConnection, project, null);
                        }
                     } catch (NotConnectedToDbException ex) {
                        Exceptions.printStackTrace(ex);
                     }
                  } else {
                     errorMsg = "[" + objName + "]Table, View or Package not exists.";
                     JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(),
                             errorMsg, "Error", JOptionPane.ERROR_MESSAGE);

                  }
               } finally {
                  connectionProvider.releaseDatabaseConnection(databaseConnection);
               }
            }
         }
      });



   }

   @Override
   public String getName() {
      return NbBundle.getMessage(ShowDatabaseObjectAction.class, "CTL_ShowDatabaseObjectAction");
   }

   @Override
   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   @Override
   protected boolean enable(Node[] activatedNodes) {
      this.activatedNodes = activatedNodes;
      if (!super.enable(activatedNodes)) {
         return false;
      }

      Project p = activatedNodes[0].getLookup().lookup(Project.class);

      return DatabaseConnectionManager.getInstance(p).isOnline();
   }
}
