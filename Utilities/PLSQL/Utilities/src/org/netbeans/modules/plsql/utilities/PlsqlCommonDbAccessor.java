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

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.netbeans.api.project.Project;

public final class PlsqlCommonDbAccessor {

   private PlsqlCommonDbAccessor() {
      //singleton
   }

   public static class MetaData {

      private final String module;
      private final String luName;

      public MetaData(String module, String luName) {
         this.module = module;
         this.luName = luName;
      }

      public String getModule() {
         return module;
      }

      public String getLuName() {
         return luName;
      }
   }

   /**
    * Method that will return the meta data (module+LU name) for the given package_name
    * @param packageName
    * @param conn
    * @param project
    * @return
    */
   public static MetaData getMetaDataOfPackage(String packageName, Connection conn, Project project, Document doc) throws SQLException, NotConnectedToDbException {
      String module = "";
      String luName = "";

      //If connected get the logical unit Name from Database
      if (conn != null) {
         ResultSet rs = null;

         packageName = packageName.toUpperCase(Locale.ENGLISH);
         PreparedStatement stmt = null;
         DatabaseConnectionManager connectionProvider;
         if (project != null) {
            connectionProvider = DatabaseConnectionManager.getInstance(project);
         } else {
            connectionProvider = DatabaseConnectionManager.getInstance(doc);
         }
         DatabaseConnection databaseConnection = connectionProvider != null ? connectionProvider.getPooledDatabaseConnection(false) : null;
         try {
            DatabaseContentManager cache = databaseConnection != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
            if (cache == null || !connectionProvider.isOnline()) {
               throw new NotConnectedToDbException();
            }
            String query = "SELECT ltrim(text) TEXT FROM all_source a WHERE name = ? AND type = 'PACKAGE' AND (ltrim(text) like 'lu_name_%:=%' OR ltrim(text) like 'service_%:=%' OR ltrim(text) like 'module_%:=%') AND (line between 2 and 6) AND OWNER = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, packageName.toUpperCase(Locale.ENGLISH));
            stmt.setString(2, cache.getOwner(packageName));
            rs = stmt.executeQuery();
            while (rs.next()) {
               String text = rs.getString("TEXT");
               int pos = text.indexOf("'") + 1;
               int end = text.indexOf("'", pos);
               if (end > pos) {
                  if (text.startsWith("lu_name_ ") || text.startsWith("service_ ")) {
                     luName = text.substring(pos, end);
                  } else {
                     module = text.substring(pos, end);
                  }
               }

            }
         } finally {
            if (connectionProvider != null) {
               connectionProvider.releaseDatabaseConnection(databaseConnection);
            }
            if (stmt != null) {
               stmt.close();
            }
         }
      }
      return new MetaData(module, luName);
   }

   private static String extractCommentValue(final String comment, String key) {
      int pos = -1;
      String upperCaseComment = comment.toUpperCase(Locale.ENGLISH);
      key = key.toUpperCase(Locale.ENGLISH) + "=";
      if (upperCaseComment.startsWith(key)) {
         pos = 0;
      } else {
         pos = upperCaseComment.indexOf("^" + key);
         if (pos > -1) {
            pos = pos + 1;
         }
      }
      if (pos > -1) {
         int end = comment.indexOf("^", pos + 1);
         if (end < 0) {
            return comment.substring(pos + 3);
         } else {
            return comment.substring(pos + 3, end);
         }
      }
      return "";
   }

   /**
    * Method that will return the meta data (module+LU name) for the given database view
    * @param viewName
    * @param conn
    * @param project
    * @return
    */
   public static MetaData getMetaDataOfView(String viewName, final Connection conn, final Project project, final Document doc) throws SQLException, NotConnectedToDbException {
      String module = "";
      String luName = "";
      //If connected get the logical unit Name from Database
      if (conn != null) {
         ResultSet rs = null;

         viewName = viewName.toUpperCase(Locale.ENGLISH);
         PreparedStatement stmt = null;
         DatabaseConnectionManager connectionProvider;
         if (project != null) {
            connectionProvider = DatabaseConnectionManager.getInstance(project);
         } else {
            connectionProvider = DatabaseConnectionManager.getInstance(doc);
         }
         final DatabaseConnection databaseConnection = connectionProvider != null ? connectionProvider.getPooledDatabaseConnection(false) : null;
         try {
            DatabaseContentManager cache = connectionProvider != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
            if (cache == null || !connectionProvider.isOnline()) {
               throw new NotConnectedToDbException();
            }
            String query = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE COMMENTS IS NOT NULL AND TABLE_NAME=? AND OWNER=?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, viewName);
            stmt.setString(2, cache.getOwner(viewName));
            rs = stmt.executeQuery();
            while (rs.next()) {
               String comment = rs.getString("COMMENTS");
               luName = extractCommentValue(comment, "LU");
               module = extractCommentValue(comment, "MODULE");
            }
         } finally {
            if (connectionProvider != null) {
               connectionProvider.releaseDatabaseConnection(databaseConnection);
            }
            if (stmt != null) {
               stmt.close();
            }
         }
      }
      return new MetaData(module, luName);
   }

   /**
    * Method that will return the package body/ package of the given package
    * @param packageName
    * @param type
    * @param conn
    * @param project
    * @return
    */
   public static String getObjectSource(final String packageName, final PlsqlBlockType type,
           final Connection conn, final Project project, final DataObject dataObj) 
           throws NotConnectedToDbException, SQLException {
      if (conn == null) {
         throw new NotConnectedToDbException();
      } else {
         CallableStatement plstmt = null;
         DatabaseConnectionManager dbConnectionProvider;
         if (project != null) {
            dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
         } else {
            dbConnectionProvider = DatabaseConnectionManager.getInstance(dataObj);
         }
         DatabaseConnection databaseConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(false) : null;
         try {
            final DatabaseContentManager cache = databaseConnection != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
            if (cache == null || !dbConnectionProvider.isOnline()) {
               throw new NotConnectedToDbException();
            }
            String block = "DECLARE "
                    + "   txt_ CLOB := to_clob('CREATE OR REPLACE '); "
                    + "   CURSOR get_text(name_ VARCHAR2, type_ VARCHAR2, owner_ VARCHAR2) IS "
                    + "      SELECT TEXT "
                    + "      FROM ALL_SOURCE "
                    + "      WHERE NAME = name_ "
                    + "      AND TYPE = type_ "
                    + "      AND OWNER = owner_ "
                    + "      ORDER BY LINE; "
                    + "BEGIN "
                    + "   FOR rec_ IN get_text(?,?,?) LOOP "
                    + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                    + "   END LOOP; "
                    + "   ? := txt_; "
                    + "END;";
            plstmt = conn.prepareCall(block);
            plstmt.setString(1, packageName.toUpperCase(Locale.ENGLISH));
            plstmt.setString(2, type.withWhiteSpace());
            plstmt.setString(3, cache.getOwner(packageName));
            plstmt.registerOutParameter(4, Types.CLOB);
            plstmt.execute();
            return plstmt.getString(4);
         } finally {
            if (dbConnectionProvider != null) {
               dbConnectionProvider.releaseDatabaseConnection(databaseConnection);
            }
            if (plstmt != null) {
               plstmt.close();
            }
         }
      }

   }

   /**
    * Method that will return the view definition
    * @param viewName
    * @param conn
    * @param project
    * @return
    */
   public static String getViewDef(String viewName, final Connection conn, final Project project, final DataObject dataObj) throws NotConnectedToDbException, SQLException {
      final StringBuilder viewDef = new StringBuilder();
      viewName = viewName.toUpperCase(Locale.ENGLISH);
      //If connected get the logical unit Name from Database
      if (conn == null) {
         throw new NotConnectedToDbException();
      } else {
         DatabaseConnectionManager connectionProvider;
         if (project != null) {
            connectionProvider = DatabaseConnectionManager.getInstance(project);
         } else {
            connectionProvider = DatabaseConnectionManager.getInstance(dataObj);
         }
         final DatabaseConnection databaseConnection = connectionProvider != null ? connectionProvider.getPooledDatabaseConnection(false) : null;
         PreparedStatement stmt = null;
         try {
            DatabaseContentManager cache = connectionProvider != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
            if (cache == null || !connectionProvider.isOnline()) {
               throw new NotConnectedToDbException();
            }
            ResultSet rs = null;
            String owner = cache.getOwner(viewName);
            String query = "SELECT TEXT FROM ALL_VIEWS WHERE VIEW_NAME =? AND OWNER = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, viewName);
            stmt.setString(2, owner);
            rs = stmt.executeQuery();

            if (rs.next()) {
               viewDef.append("CREATE OR REPLACE VIEW ").append(viewName).append(" AS\n");
               viewDef.append(rs.getString("TEXT"));
               viewDef.append(";");
               //fetch and add view/column comments
               query = "SELECT COMMENTS FROM ALL_TAB_COMMENTS WHERE COMMENTS IS NOT NULL AND TABLE_NAME=? AND OWNER=?";
               stmt = conn.prepareStatement(query);
               stmt.setString(1, viewName);
               stmt.setString(2, owner);
               rs = stmt.executeQuery();
               if (rs.next()) {
                  viewDef.append("\n\n").append("COMMENT ON TABLE ").append(viewName).append("\n");
                  viewDef.append("   IS '").append(rs.getString("COMMENTS")).append("';\n\n");
               }

               query = "SELECT COLUMN_NAME, COMMENTS FROM ALL_COL_COMMENTS WHERE COMMENTS IS NOT NULL AND TABLE_NAME=? AND OWNER = ?";
               stmt = conn.prepareStatement(query);
               stmt.setString(1, viewName);
               stmt.setString(2, owner);
               rs = stmt.executeQuery();
               while (rs.next()) {
                  viewDef.append("COMMENT ON COLUMN ").append(viewName).append(".");
                  viewDef.append(rs.getString("COLUMN_NAME").toLowerCase(Locale.ENGLISH)).append("\n");
                  viewDef.append("   IS '").append(rs.getString("COMMENTS")).append("';\n");
               }
            }
         } finally {
            if (connectionProvider != null) {
               connectionProvider.releaseDatabaseConnection(databaseConnection);
            }
            if (stmt != null) {
               stmt.close();
            }
         }
      }

      return viewDef.toString();
   }

   /**
    * Method that will return the materialized view definition
    * @param viewName
    * @param conn
    * @param project
    * @return
    */
   public static String getMaterializedViewDef(String viewName, final Connection conn, final Project project, final DataObject dataObj) throws NotConnectedToDbException, SQLException {
      final StringBuilder viewDef = new StringBuilder();
      viewName = viewName.toUpperCase(Locale.ENGLISH);
      //If connected get the logical unit Name from Database
      if (conn == null) {
         throw new NotConnectedToDbException();
      } else {
         DatabaseConnectionManager connectionProvider;
         if (project != null) {
            connectionProvider = DatabaseConnectionManager.getInstance(project);
         } else {
            connectionProvider = DatabaseConnectionManager.getInstance(dataObj);
         }
         final DatabaseConnection databaseConnection = connectionProvider != null ? connectionProvider.getPooledDatabaseConnection(false) : null;
         PreparedStatement stmt = null;
         try {
            final DatabaseContentManager cache = connectionProvider != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
            if (cache == null || !connectionProvider.isOnline()) {
               throw new NotConnectedToDbException();
            }
            ResultSet rs = null;
            final String owner = cache.getOwner(viewName);
            final String query = "SELECT QUERY,BUILD_MODE,REFRESH_MODE,REFRESH_METHOD FROM ALL_MVIEWS WHERE MVIEW_NAME =? AND OWNER = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, viewName);
            stmt.setString(2, owner);
            rs = stmt.executeQuery();

            if (rs.next()) {
               viewDef.append("CREATE OR REPLACE MATERIALIZED VIEW ").append(viewName).append("\n");
               viewDef.append("BUILD ").append(rs.getString("BUILD_MODE")).append("\n");
               viewDef.append("REFRESH ").append(rs.getString("REFRESH_METHOD"));
               viewDef.append(" ON ").append(rs.getString("REFRESH_MODE")).append("\n");
               viewDef.append("AS\n");
               viewDef.append(rs.getString("QUERY"));
               viewDef.append(";");
            }
         } finally {
            if (connectionProvider != null) {
               connectionProvider.releaseDatabaseConnection(databaseConnection);
            }
            if (stmt != null) {
               stmt.close();
            }
         }
      }

      return viewDef.toString();
   }

   private static String pad(final String str, final int size) {
      int length = size - str.length();
      final StringBuilder out = new StringBuilder(length);
      while (length > 0) {
         out.append(" ");
         length--;
      }
      return out.toString();
   }

   /**
    * Method that will return the table definition
    * @param tableName
    * @param conn
    * @param project
    * @return
    */
   public static String getTableDef(final String tableName, final Connection conn, final Project project, final DataObject dataObj) throws NotConnectedToDbException, SQLException {
      String table_def = "";

      //If connected get the logical unit Name from Database
      if (conn == null) {
         throw new NotConnectedToDbException();
      } else {
         final StringBuilder indexBlock = new StringBuilder();
         final List<String> columns = new ArrayList<String>();

         PreparedStatement stmt = null;
         PreparedStatement ddlstmt = null;
         String query = "SELECT COLUMN_NAME AS NAME, "
                 + "data_type||decode(data_type,'VARCHAR2','('||char_length||')', "
                 + "'DATE','','NUMBER',decode(data_precision,null,'','('||data_precision||"
                 + "decode(data_scale,0,'',null,'',','||data_scale)||')'),'') DECLARE_TYPE,"
                 + "decode(nullable,'Y','NULL','NOT NULL') NULLABLE "
                 + "FROM ALL_TAB_COLUMNS "
                 + "WHERE TABLE_NAME = ? "
                 + "AND OWNER = ? "
                 + "ORDER BY COLUMN_ID";
         DatabaseConnectionManager connectionProvider;
         if (project != null) {
            connectionProvider = DatabaseConnectionManager.getInstance(project);
         } else {
            connectionProvider = DatabaseConnectionManager.getInstance(dataObj);
         }
         final DatabaseConnection databaseConnection = connectionProvider != null ? connectionProvider.getPooledDatabaseConnection(false) : null;
         try {
            final DatabaseContentManager cache = connectionProvider != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
            if (cache == null || !connectionProvider.isOnline()) {
               throw new NotConnectedToDbException();
            }
            String tableOwner = cache.getOwner(tableName);
            stmt = conn.prepareStatement(query);
            stmt.setString(1, tableName.toUpperCase(Locale.ENGLISH));
            stmt.setString(2, tableOwner);
            ResultSet codeSet = stmt.executeQuery();
            while (codeSet.next()) {
               String name = codeSet.getString("NAME");
               String type = codeSet.getString("DECLARE_TYPE");
               String column = "   " + name + pad(name, 35) + type + pad(type, 20);
               column = column + codeSet.getString("NULLABLE");
               columns.add(column);
            }
            stmt.close();
            query = "SELECT INDEX_NAME FROM ALL_INDEXES WHERE TABLE_NAME = ? AND OWNER = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, tableName.toUpperCase(Locale.ENGLISH));
            stmt.setString(2, tableOwner);
            ResultSet indexes = stmt.executeQuery();
            while (indexes.next()) {
               String indexRequest = "SELECT dbms_metadata.get_ddl('INDEX', ?, ?) DDL FROM DUAL";
               ddlstmt = conn.prepareStatement(indexRequest);
               ddlstmt.setString(1, indexes.getString("INDEX_NAME"));
               ddlstmt.setString(2, tableOwner);
               ResultSet indexDDL = ddlstmt.executeQuery();
               if (indexDDL.next()) {
                  String index = indexDDL.getString("DDL").trim();
                  index = index.replaceAll("\"", ""); //remove quotes
                  index = index.replaceAll(tableOwner + ".", ""); //remove appowner prefix
                  indexBlock.append("\n").append(index).append(";\n");
               }
            }
         } finally {
            if (connectionProvider != null) {
               connectionProvider.releaseDatabaseConnection(databaseConnection);
            }
            if (stmt != null) {
               stmt.close();
            }
            if (ddlstmt != null) {
               ddlstmt.close();
            }
         }

         if (columns.size() > 0) {
            table_def = "CREATE TABLE " + tableName.toUpperCase(Locale.ENGLISH) + "\n(\n";
         }
         for (int i = 0; i < columns.size(); i++) {
            table_def = table_def + columns.get(i);
            if (i != (columns.size() - 1)) {
               table_def += ",\n";
            }
         }
         if (columns.size() > 0) {
            table_def = table_def + "\n)";
         }
         if (indexBlock.length() > 0) {
            table_def += "\n" + indexBlock.toString();
         }
      }

      if (table_def == null) {
         table_def = "";
      }

      return table_def;
   }

   /**
    * Method that will return the last modified time of the given object
    * @param name
    * @param type
    * @param conn
    * @param project
    * @return
    */
   public static long getLastModifiedTime(final String name, final PlsqlBlockType type, 
           final DatabaseConnection databaseConnection) throws SQLException, NotConnectedToDbException {
      {
         Date timeStamp = null;
         DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

         //If connected get the logical unit Name from Database
         if (databaseConnection != null) {
            try {
               final List<String> objList = new ArrayList<String>();
               ResultSet objSet = null;

               final String query = "SELECT to_char(LAST_DDL_TIME, 'yyyy-MM-dd HH:mm:ss') last_ddl_time FROM ALL_OBJECTS A WHERE OBJECT_TYPE = ? AND OBJECT_NAME= ? AND OWNER = ?";
               PreparedStatement stmt = null;
               try {
                  final DatabaseContentManager cache = databaseConnection != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
                  if (cache == null) {
                     throw new NotConnectedToDbException();
                  }

                  stmt = databaseConnection.getJDBCConnection().prepareStatement(query);
                  stmt.setString(1, type.withWhiteSpace());
                  stmt.setString(2, name.toUpperCase(Locale.ENGLISH));
                  stmt.setString(3, cache.getOwner(name));
                  objSet = stmt.executeQuery();
                  while (objSet.next()) {
                     objList.add(objSet.getString("LAST_DDL_TIME"));
                  }
               } finally {
                  if (stmt != null) {
                     stmt.close();
                  }
               }
               if (!objList.isEmpty()) {
                  timeStamp = dfm.parse(objList.get(0));
               }
            } catch (ParseException ex) {
               Exceptions.printStackTrace(ex);
            }
         }

         if (timeStamp == null) {
            return -1;
         } else {
            return timeStamp.getTime();
         }
      }
   }
}
