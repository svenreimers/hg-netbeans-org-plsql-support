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
package org.netbeans.modules.plsql.hyperlink;

import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import java.util.List;
import java.util.Locale;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.Utilities;
import org.netbeans.editor.BaseDocument;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.actions.CookieAction;

public abstract class AbstractPlsqlGoToAction extends CookieAction {

   protected final PlsqlFileValidatorService fileValidator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);

   @Override
   protected boolean enable(final Node[] activatedNodes) {
      if (activatedNodes == null || activatedNodes.length == 0) {
         return false; //This happends when using the diff window (.gen <--> actual)
      }
      final EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
      if (editorCookie != null) {
         final DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
         final Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
         if (project == null) {
            return false;
         }
         if (PlsqlHyperlinkUtil.getCaretPosition(editorCookie) == -1) {
            return false;
         }
      }
      return true;
   }

   protected String getFileName(final DataObject dataObject) {
      return dataObject.getPrimaryFile().getNameExt().toLowerCase(Locale.ENGLISH);
   }

   @Override
   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   @Override
   protected Class[] cookieClasses() {
      return new Class[]{DataObject.class};
   }

   @Override
   protected void initialize() {
      super.initialize();
      // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
      putValue("noIconInMenu", Boolean.TRUE);
   }

   @Override
   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   @Override
   protected boolean asynchronous() {
      return false;
   }

   /**
    * Open corresponding body file
    * @param packageName
    * @param project
    * @return DataObject
    */
   public DataObject getPackageDataObject(String packageName, Project project, boolean dbSourceOnly) {
      DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
      DatabaseConnection dbConnection = dbConnectionProvider.getPooledDatabaseConnection(true, dbSourceOnly);
      if (!dbSourceOnly || dbConnection != null) {
         try {
            DataObject data = null;
            if (!dbSourceOnly) {
               data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE_BODY, project);
               if (data == null) //check to see if the user has a local version of this file...
               {
                  data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE_BODY, project);
               }
            }
            if (data == null) //fetch file from database (or cache)
            {
               data = PlsqlFileUtil.fetchAsTempFile(packageName, PACKAGE_BODY, dbConnection, project, null);
            }
            return data;
         } catch (NotConnectedToDbException ex) {
            Exceptions.printStackTrace(ex);
         } finally {
            dbConnectionProvider.releaseDatabaseConnection(dbConnection);
         }
      }
      return null;
   }

   /**
    * Open corresponding procedure/function file
    * @param methodName
    * @param type
    * @param project
    * @return DataObject
    */
   public DataObject getMethodDataObject(String packageName, PlsqlBlockType type, Project project) {
      DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
      DatabaseConnection dbConnection = dbConnectionProvider.getPooledDatabaseConnection(true, true);
      if (dbConnection != null) {
         try {
            DataObject data = null;
            data = PlsqlFileUtil.fetchAsTempFile(packageName, type, dbConnection, project, null);
            return data;
         } catch (NotConnectedToDbException ex) {
            Exceptions.printStackTrace(ex);
         } finally {
            dbConnectionProvider.releaseDatabaseConnection(dbConnection);
         }
      }
      return null;
   }

   /**
    * Find the line number in the file where the package definition starts
    * @param packageName
    * @param project
    * @return DataObject
    */
   public int getPackageBodyOffset(String packageName, DataObject dataObject) {
      if (dataObject == null) {
         return 0;
      }
      try {
         EditorCookie ec = dataObject.getCookie(EditorCookie.class);
         if (!PlsqlFileUtil.prepareDocument(ec)) {
            return 0;
         }

         if (ec != null) {
            BaseDocument doc = (BaseDocument) ec.getDocument();
            PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(dataObject);
            blockFactory.initHierarchy(ec.getDocument());
            List blockHier = blockFactory.getBlockHierarchy();
            if (blockFactory != null) {
               for (int i = 0; i < blockHier.size(); i++) {
                  PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
                  if (temp.getType() == PACKAGE_BODY && temp.getName().equalsIgnoreCase(packageName)) {
                     return Utilities.getLineOffset(doc, temp.getStartOffset());
                  }
               }
            }
         }
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
      return 0;
   }

   /**
    * Open corresponding body file
    * @param dataObject
    * @return
    */
   public abstract boolean goToPackage(String packageName, Project project, String methodName, int lineNumber);

   /**
    * Open corresponding pls file
    * @param dataObject
    * @return
    */
   protected boolean goToMethodImpl(String methodName, Project project, int lineNumber, PlsqlBlockType type) {
      DatabaseConnectionManager provider = DatabaseConnectionManager.getInstance(project);
      DatabaseConnection databaseConnection = provider == null ? null : provider.getPooledDatabaseConnection(false);
      if (databaseConnection == null) {
         return false;
      }
      try {
         DataObject data = PlsqlFileUtil.fetchAsTempFile(methodName, type, databaseConnection, project, null);
         EditorCookie ec = data.getCookie(EditorCookie.class);
         ec.open();
         PlsqlHyperlinkUtil.setCaretPos(data, lineNumber);
      } catch (NotConnectedToDbException ex) {
         return false;
      } finally {
         provider.releaseDatabaseConnection(databaseConnection);
      }
      return true;
   }

   /**
    * Open corresponding database file
    * @param dataObject
    * @return
    */
   public boolean goToFunction(String methodName, Project project, int lineNumber) {
      return goToMethodImpl(methodName, project, lineNumber, FUNCTION);
   }

   /**
    * Open corresponding database file
    * @param dataObject
    * @return
    */
   public boolean goToProcedure(String methodName, Project project, int lineNumber) {
      return goToMethodImpl(methodName, project, lineNumber, PROCEDURE);
   }

   /**
    * Open corresponding body file
    * @param dataObject
    * @return
    */
   protected boolean goToPackageImpl(String packageName, Project project, String methodName, int lineNumber, boolean dbSourceOnly) {
      DataObject data = getPackageDataObject(packageName, project, dbSourceOnly);
      if (data == null) {
         return false;
      }
      try {
         return goToPackageImpl(data, packageName, lineNumber - 1);
      } catch (NotConnectedToDbException ex) {
         Exceptions.printStackTrace(ex);
      }
      return false;
   }

   /**
    * Open corresponding body file
    * @param dataObject
    * @return
    */
   private boolean goToPackageImpl(DataObject dataObj, String packageName, int lineNumber) throws NotConnectedToDbException {
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
                  if (temp.getType() == PACKAGE_BODY
                          && temp.getName().equalsIgnoreCase(packageName)) {
                     offset = temp.getStartOffset();
                     break;
                  }
               }
            }
            if (lineNumber >= 0) { //add to the offset - line number is given as a line number in the block - not relative to the file
               BaseDocument doc = (BaseDocument) ec.getDocument();
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
}
