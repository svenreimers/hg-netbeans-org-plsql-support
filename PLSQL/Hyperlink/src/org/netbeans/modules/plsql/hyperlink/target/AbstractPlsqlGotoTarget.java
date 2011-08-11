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
package org.netbeans.modules.plsql.hyperlink.target;

import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.utilities.PlsqlFileLocatorService;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidator;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.LogInWarningDialog;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.util.Locale;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author ChrLSE
 */
public abstract class AbstractPlsqlGotoTarget implements PlsqlGotoTarget {

   protected final PlsqlFileValidatorService globalFileValidator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
   protected final PlsqlFileLocatorService globalFileLocator = Lookup.getDefault().lookup(PlsqlFileLocatorService.class);
   private final String name;
   protected final int position;
   protected String packageName;
   protected String synonym;
   protected PlsqlBlockType type;
   protected final DataObject sourceDataObject;
   protected DataObject targetDataObject;
   protected final Project project;
   protected final Document sourceDocument;
   protected DatabaseConnectionManager connectionManager;
   protected final PlsqlFileValidator projectFileValidator;
   protected DatabaseContentManager cache;

   public AbstractPlsqlGotoTarget(final String name, final int position, final DataObject sourceDataObject,
           final Project project, final Document sourceDocument) {
      this.name = name;
      this.position = position;
      this.sourceDataObject = sourceDataObject;
      this.project = project;
      projectFileValidator = project.getLookup().lookup(PlsqlFileValidator.class);
      this.sourceDocument = sourceDocument;
      if (sourceDataObject != null) {
         connectionManager = DatabaseConnectionManager.getInstance(sourceDataObject);
         final DatabaseConnection templateConnection = connectionManager.getTemplateConnection();
         if (templateConnection == null) {
            throw new IllegalArgumentException("connectionManager.getTemplateConnection() returns null");
         }
         cache = DatabaseContentManager.getInstance(templateConnection);
         if (cache == null) {
            throw new IllegalArgumentException("DatabaseContentManager.getInstance(templateConnection) returns null");
         }
      }
   }

   public String getName() {
      if (hasSynonym()) {
         return synonym;
      }
      return name;
   }

   public String getSynonym() {
      return synonym;
   }

   public boolean hasSynonym() {
      return synonym != null && !synonym.isEmpty();
   }

   public boolean isPackage() {
      return packageName != null;
   }

   public String getPackageName() {
      return packageName;
   }

   public void setPackageName(final String packageName) {
      this.packageName = packageName;
   }

   public PlsqlBlockType getType() {
      return type;
   }

   public int getPosition() {
      return position;
   }

   @Override
   public void gotoSpec() {
      final DataObject dataObject = getSpec();
      if (dataObject != null) {
         dataObject.getCookie(OpenCookie.class).open();
      }
   }

   @Override
   public DataObject getSpec() {
      final DatabaseConnection connection = getPooledConnection();
      try {
         if (!isPackage()) {
            targetDataObject = globalFileValidator.getSiblingExt(sourceDataObject);
         }
         if (targetDataObject == null) {

            //If file is not found try fetching the Package from database
            targetDataObject = PlsqlFileUtil.openExistingFile(PlsqlFileUtil.getDocument(sourceDataObject), packageName, PACKAGE, project);
            if (targetDataObject == null) {
               targetDataObject = PlsqlFileUtil.fetchAsTempFile(packageName, PACKAGE, connection, project, sourceDataObject);
            }
         }
      } catch (NotConnectedToDbException ex) {
         Exceptions.printStackTrace(ex);
      } finally {
         connectionManager.releaseDatabaseConnection(connection);
      }
      return targetDataObject;
   }

   @Override
   public void gotoBody() {
      final DataObject dataObject = getBody();
      if (dataObject != null) {
         dataObject.getCookie(OpenCookie.class).open();
      }
   }

   @Override
   public DataObject getBody() {
      final DatabaseConnection connection = getPooledConnection();
      try {

         //If the other file is needed get it
         targetDataObject = globalFileValidator.getSiblingExt(sourceDataObject);
         if (targetDataObject == null) {

            //If file is not found try fetching the Package Body from database
            targetDataObject = PlsqlFileUtil.openExistingFile(PlsqlFileUtil.getDocument(sourceDataObject), packageName, PACKAGE_BODY, project);
            if (targetDataObject == null) {
               targetDataObject = PlsqlFileUtil.fetchAsTempFile(packageName, PACKAGE_BODY, connection, project, sourceDataObject);
            }
         }

      } catch (NotConnectedToDbException ex) {
         new LogInWarningDialog(null, true).setVisible(true);
      } finally {
         connectionManager.releaseDatabaseConnection(connection);
      }
      return targetDataObject;
   }

   @Override
   public void gotoDbBody() {
      final DatabaseConnection connection = getPooledConnection();
      try {
         openDbBody(connection);
      } catch (NotConnectedToDbException ex) {
         new LogInWarningDialog(null, true).setVisible(true);
      } finally {
         connectionManager.releaseDatabaseConnection(connection);
      }
   }

   protected DatabaseConnection getPooledConnection() {
       DatabaseConnection pooledConnection = null;
      if (connectionManager.isOnline()) {
         pooledConnection = connectionManager.getPooledDatabaseConnection(true, true);
      }
      return pooledConnection;
   }

   /**
    * Method that will return the data object of the body file (of this file)
    * @param dataObject
    * @param conn
    * @param project
    * @return
    */
   protected DataObject getBodyDataObject(DataObject dataObject, DatabaseConnection conn, Project project) throws NotConnectedToDbException {
      DataObject tarDataObject = globalFileValidator.getSiblingExt(sourceDataObject);
      if (tarDataObject == null) {

         //If file is not found try fetching the Package Body from database
         DataObject dataObj = PlsqlFileUtil.openExistingFile(PlsqlFileUtil.getDocument(dataObject), packageName, PACKAGE_BODY, project);
         if (dataObj == null) {
            dataObj = PlsqlFileUtil.fetchAsTempFile(packageName, PACKAGE_BODY, conn, project, dataObject);
         }
         return dataObj;
      }

      return null;
   }

   private void openDbBody(final DatabaseConnection connection) throws NotConnectedToDbException {
      PlsqlHyperlinkUtil.openAsTempFile(getName(), getType(), connection, project, sourceDocument);
   }

   public final boolean hasType() {
      return getType() != null;
   }

   protected final boolean isSpec() {
      return globalFileValidator.isValidPackageSpec(sourceDataObject);
   }

   protected final boolean isBody() {
      return globalFileValidator.isValidPackageBody(sourceDataObject);
   }

   protected String getFileName(final DataObject dataObject) {
      return dataObject.getPrimaryFile().getNameExt().toLowerCase(Locale.ENGLISH);
   }

   protected void setCaretPosition(final DataObject data, final boolean isImpl) {
      if (data == null) {
         return;
      }
      //Prepare Hierarchy if not already done
      final EditorCookie editorCookie = data.getCookie(EditorCookie.class);
      editorCookie.open();
      if (editorCookie != null) {
         final PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(data);
         blockFactory.initHierarchy(editorCookie.getDocument());
      }

      //There are multiple method implementations. We have to find the correct one
      final boolean isUsage = !PlsqlHyperlinkUtil.isMethodDefinition(sourceDocument, getName(), position);
      final PlsqlBlock selectedMethod = PlsqlParserUtil.findMatchingBlock(data.getLookup().lookup(PlsqlBlockFactory.class).getBlockHierarchy(),
              sourceDocument, PlsqlFileUtil.getDocument(data), getName(), getPackageName(), position, isUsage, isImpl, true);
      if (selectedMethod != null) {
         PlsqlHyperlinkUtil.setCaretPos(data, selectedMethod.getStartOffset());
      }
   }
}
