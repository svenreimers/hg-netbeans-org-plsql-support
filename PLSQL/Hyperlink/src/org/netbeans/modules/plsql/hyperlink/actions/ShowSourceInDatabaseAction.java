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
import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import java.awt.EventQueue;
import java.awt.Frame;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author csamlk
 */
public class ShowSourceInDatabaseAction extends CookieAction {

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
            DataObject dataObj = null;
            Frame mainWindow = WindowManager.getDefault().getMainWindow();
            ShowDbObjectDialog findDlg = new ShowDbObjectDialog(mainWindow, project, "", true);
            findDlg.setLocationRelativeTo(mainWindow);
            findDlg.setTitle(NbBundle.getMessage(ShowDatabaseObjectAction.class, "LBL_ShowSourceInDatabaseTitle"));
            findDlg.setLable("Database View or Package Name:");
            findDlg.setVisible(true);
            if (!findDlg.isCancelled()) {
               DatabaseConnection databaseConnection = connectionProvider.getPooledDatabaseConnection(false);
               try {
                  objName = findDlg.getInputText();
                  if (cache.isView(objName, databaseConnection)) {
                     try {
                        PlsqlHyperlinkUtil.openAsTempFile(objName, VIEW, databaseConnection, project, null);
                     } catch (NotConnectedToDbException ex) {
                        Exceptions.printStackTrace(ex);
                     }
                  } else if (cache.isTable(objName, databaseConnection)) {
                     try {
                        PlsqlHyperlinkUtil.openAsTempFile(objName, TABLE, databaseConnection, project, null);
                     } catch (NotConnectedToDbException ex) {
                        Exceptions.printStackTrace(ex);
                     }
                  } else if (cache.isPackage(objName, databaseConnection)) {
                     try {
                        dataObj = PlsqlFileUtil.fetchAsTempFile(objName, PACKAGE_BODY, databaseConnection, project, null);
                     } catch (NotConnectedToDbException ex) {
                        Exceptions.printStackTrace(ex);
                     }
                     try {
                        goToPackageImpl(dataObj, 1);
                     } catch (NotConnectedToDbException ex) {
                        Exceptions.printStackTrace(ex);
                     } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
                     }
                  } else {
                     errorMsg = "[" + objName + "]View or Package not exists.";
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

   private boolean goToPackageImpl(DataObject dataObj, int lineNumber) throws NotConnectedToDbException, BadLocationException {
      if (dataObj != null) {
         EditorCookie ec = dataObj.getCookie(EditorCookie.class);
         ec.open();
         int offset = 0;

         if (ec != null) {
            PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(dataObj);
            blockFactory.initHierarchy(ec.getDocument());
            List blockHier = blockFactory.getBlockHierarchy();
            if (blockFactory != null) {
               for (int i = 0; i < blockHier.size(); i++) {
                  PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
                  if (temp.getType() == PlsqlBlockType.PACKAGE_BODY) {
                     offset = temp.getStartOffset();
                     break;
                  }
               }
            }
            if (lineNumber >= 0) { //add to the offset - line number is given as a line number in the block - not relative to the file
               BaseDocument doc = (BaseDocument) ec.getDocument();
               if (doc == null)
                  return false;
               
               try {
                  int firstLine = Utilities.getLineOffset(doc, offset);
                  offset = Utilities.getRowStartFromLineOffset(doc, firstLine + lineNumber);
               } catch (BadLocationException ex) {
                  Exceptions.printStackTrace(ex);
               }
            }

            JEditorPane[] panes = ec.getOpenedPanes();
            if (panes.length > 0) {
               JEditorPane pane = panes[0];
               pane.setCaretPosition(offset);
               return true;
            }
         }
      }
      return false;
   }

   @Override
   public String getName() {
      return NbBundle.getMessage(ShowSourceInDatabaseAction.class, "CTL_ShowSourceInDatabaseAction");
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
