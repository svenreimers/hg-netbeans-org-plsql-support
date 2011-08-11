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
package org.netbeans.modules.plsql.utilities;

import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.CacheDirectoryProvider;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Task;
import org.openide.util.UserQuestionException;
import org.openide.windows.WindowManager;

public class PlsqlFileUtil {

   private static final String SOURCE_CACHE_DIRECTORY = "source";
   private static final PlsqlFileLocatorService fileLocator = Lookup.getDefault().lookup(PlsqlFileLocatorService.class);

   /**
    * Method that will fetch the PACKAGE BODY from database,
    * save it at a tmp directory and open it
    * @param packageName
    * @param type PACKAGE/PACKAGE BODY
    * @param conn
    * @param project
    * @return
    */
   public static DataObject fetchAsTempFile(final String objectName, final PlsqlBlockType type, final DatabaseConnection databaseConnection, final Project project, final DataObject sourceObject) throws NotConnectedToDbException {
      if (databaseConnection == null) {
         return null;
      }
      DataObject dataObj = null;
      DatabaseConnectionManager provider = null;

      if (project != null) {
         provider = DatabaseConnectionManager.getInstance(project);
      }

      final boolean belongToProject = (provider != null);
      if (provider == null) {
         provider = DatabaseConnectionManager.getInstance(sourceObject);
      }

      final DatabaseContentManager cache = databaseConnection != null ? DatabaseContentManager.getInstance(databaseConnection) : null;

      try {
         final long lastModified = PlsqlCommonDbAccessor.getLastModifiedTime(objectName, type, databaseConnection);
         String suffix;
         final String fileName = objectName.toUpperCase(Locale.ENGLISH);
         switch (type) {
            case PACKAGE:
               suffix = ".spec";
               break;
            case PACKAGE_BODY:
               suffix = ".body";
               break;
            case MATERIALIZED_VIEW:
               suffix = ".db";
               break;
            case VIEW:
               suffix = ".db";
               break;
            case METHOD:
            case FUNCTION:
            case PROCEDURE:
               suffix = ".pls";
               break;
            default:
               suffix = ".db";
         }

         File ownerDir;
         if (belongToProject) {
            ownerDir = FileUtil.toFile(project.getLookup().lookup(CacheDirectoryProvider.class).getCacheDirectory());
         } else {
            ownerDir = new File(cache.getCacheDir());
         }

         final File tmpFile = new File(ownerDir, SOURCE_CACHE_DIRECTORY + File.separator + fileName + suffix);

         //Delete file if existing and changed
         boolean create = fileChanged(tmpFile, lastModified);
         if (create && belongToProject) {
            //If we need to create the file & file is in project cache first we have to look at the common cache
            final File commonFile = new File(new File(cache.getCacheDir()), SOURCE_CACHE_DIRECTORY + File.separator + fileName + suffix);
            if (!fileChanged(commonFile, lastModified)) {
               //latest file exists in common db cache
               create = false;
               final File dirFile = tmpFile.getParentFile();
               if (!dirFile.exists()) {
                  dirFile.mkdirs();
               }

               tmpFile.createNewFile();
               final OutputStream out = new FileOutputStream(tmpFile);
               final InputStream in = new FileInputStream(commonFile);
               FileUtil.copy(in, out);
               in.close();
               out.close();
               tmpFile.setLastModified(lastModified);
               tmpFile.setReadOnly();
            }
         }

         if (create) {
            final Connection conn = databaseConnection != null ? databaseConnection.getJDBCConnection() : null;
            if (conn == null) {
               throw new NotConnectedToDbException();
            }

            String body = "";
            if (type == PACKAGE || type == PACKAGE_BODY || type == FUNCTION || type == PROCEDURE) {
               body = PlsqlCommonDbAccessor.getObjectSource(objectName, type, conn, project, sourceObject);
            } else if (type == VIEW) {
               body = PlsqlCommonDbAccessor.getViewDef(objectName, conn, project, sourceObject);
            } else if (type == MATERIALIZED_VIEW) {
               body = PlsqlCommonDbAccessor.getMaterializedViewDef(objectName, conn, project, sourceObject);
            } else {
               body = PlsqlCommonDbAccessor.getTableDef(objectName, conn, project, sourceObject);
            }

            writeToFile(tmpFile, body, lastModified);

            if (belongToProject) {
               //If this file belongs to a project copy the fetched file to commom db cache
               final File commonFile = new File(new File(cache.getCacheDir()), SOURCE_CACHE_DIRECTORY + File.separator + fileName + suffix);
               writeToFile(commonFile, body, lastModified);
            }
         }

         if (tmpFile.exists()) {
            dataObj = DataFolder.find(FileUtil.toFileObject(tmpFile));
         }

         if (!belongToProject && dataObj != null) //connect the new file to the same database as the source document
         {
            DatabaseConnectionManager.copyProvider(sourceObject, dataObj);
         }
      } catch (DataObjectNotFoundException ex) {
         Exceptions.printStackTrace(ex);
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
      } catch (SQLException ex) {
         if (project != null) {
            DatabaseConnectionManager dbConnection = DatabaseConnectionManager.getInstance(sourceObject);
            if (dbConnection != null) {
               dbConnection.setOnline(false);
            }
         }
         new LogInWarningDialog(null, true).setVisible(true);
      }

      return dataObj;
   }

   /**
    * Method that will return the existing file's data object
    * @param doc
    * @param objectName
    * @param objectType
    * @param project
    * @return
    */
   public static DataObject openExistingFile(final Document doc, final String objectName, final PlsqlBlockType objectType, final Project project) throws NotConnectedToDbException {
      //TODO use PlsqlFileLocatorService to find file with both IFS specific and default (fallback) version.
      DataObject dataObject = null;
      if (doc != null) {
         final Object obj = doc.getProperty(Document.StreamDescriptionProperty);
         if (obj instanceof DataObject) {
            dataObject = (DataObject) obj;
            if (dataObject == null) {
               return null;
            }
         }
      }

      return fileLocator.getExistingDataObject(dataObject, objectName, objectType, project);
   }

   public static DataObject getDataObject(final String path) {
      final File file = new File(path);
      DataObject dObject = null;
      if (file.exists()) {
         try {
            dObject = DataFolder.find(FileUtil.toFileObject(file));
         } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
         }
      }

      return dObject;
   }

   public static Document getDocument(final DataObject dataObject) {
      final EditorCookie ec = dataObject.getCookie(EditorCookie.class);
      if (ec != null) {
         final Task task = ec.prepareDocument();
         task.waitFinished();
         return ec.getDocument();
      }
      return null;
   }

   public static void writeToFile(final File tmpFile, final String body, final long lastModified) throws IOException {
      //create directory is not existing
      final File dirFile = tmpFile.getParentFile();
      if (!dirFile.exists()) {
         dirFile.mkdirs();
      }

      if (!body.equals("")) {
         tmpFile.createNewFile();
         final FileWriter fileWriter = new FileWriter(tmpFile);
         final BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
         bufferedWriter.write(body);
         bufferedWriter.flush();
         fileWriter.close();
         bufferedWriter.close();
         tmpFile.setLastModified(lastModified);
         tmpFile.setReadOnly();
      }
   }

   public static boolean fileChanged(final File tmpFile, final long lastModified) {
      if (tmpFile.exists()) {
         if (lastModified == -1) {
            return false;
         } else if (tmpFile.lastModified() != lastModified) {
            return tmpFile.delete();
         } else if (tmpFile.lastModified() == lastModified) {
            return false;
         }
      }

      return true;
   }

   /*
    * Load the document in memory without actually opening it
    */
   public static boolean prepareDocument(final EditorCookie ec) {
      if (ec == null) {
         return false;
      }

      try {
         ec.openDocument();
      } catch (UserQuestionException ex) {
         final int result = JOptionPane.showConfirmDialog(WindowManager.getDefault().getMainWindow(), ex.getLocalizedMessage(), "Warning", JOptionPane.OK_CANCEL_OPTION);
         if (result == JOptionPane.OK_OPTION) {
            try {
               ex.confirmed();
            } catch (IOException ex1) {
               Exceptions.printStackTrace(ex1);
               return false;
            }
         } else {
            return false;
         }
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
         return false;
      }

      return true;
   }
}
