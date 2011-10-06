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
package org.netbeans.modules.plsqlsupport.db;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.Types;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.util.Exceptions;

public class DatabaseContentUtilities {

   private static final String ORACLE_UPPERCASE_IDENTIFIER = "[A-Z]{1}[A-Z0-9_#\\$]{0,29}";
   private static final String TIMESTAMP_FORMAT = "'yyyy/mm/dd:hh24:mi:ss'";
   private static final String IN_OUT = "IN_OUT";
   public static final String RECORD_SEPARATOR = String.valueOf((char)30);
   public static final String FIELD_SEPARATOR = String.valueOf((char)31);
   static DatabaseObjectInfo getViewInfo(String viewName, DatabaseConnection connection) {
      DatabaseObjectInfo info = new DatabaseObjectInfo(connection.getSchema());
      Connection jdbcConnection = connection!=null ? connection.getJDBCConnection() : null;
      PreparedStatement stmt = null;
      String query = "SELECT "
            + "  TABLE_NAME, "
            + "  OWNER, "
            + "  substr(COMMENTS, instr(COMMENTS, 'MODULE=')+7,"
            + "                        instr(COMMENTS, '^', instr(COMMENTS, 'MODULE=')+7) - (instr(COMMENTS, 'MODULE=')+7)) MODULE,"
            + "  substr(COMMENTS, instr(COMMENTS, 'LU=')+3,"
            + "                        instr(COMMENTS, '^', instr(COMMENTS, 'LU=')+3) - (instr(COMMENTS, 'LU=')+3)) LU_NAME "
            + "FROM ALL_TAB_COMMENTS  "
            + "WHERE TABLE_NAME = ? "
            + "  AND TABLE_TYPE = 'VIEW'"
            + "  AND OWNER = ?";
      try {
         if (jdbcConnection != null) {
            stmt = jdbcConnection.prepareStatement(query);
            stmt.setString(1, viewName.toUpperCase(Locale.ENGLISH));
            stmt.setString(2, connection.getSchema());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
               //String owner = rs.getString(2);
               String module = rs.getString(3);
               String luName = rs.getString(4);
               info.setLuName(luName);
               info.setModule(module);
            }
            rs.close();
            stmt.close();
         }
      } catch (SQLException ex) {
         Exceptions.printStackTrace(ex);
      } finally {
         if (stmt != null) {
            try {
               stmt.close();
            } catch (SQLException ex) {
               Exceptions.printStackTrace(ex);
            }
         }
      }
      return info;
   }

   private DatabaseContentUtilities() {
   }

   public static String getFndbasVersion(DatabaseConnection connection) throws SQLException {
      return getFndbasVersion(connection.getJDBCConnection(), connection.getSchema());
   }

   public static String getFndbasVersion(Connection connection, String schema) throws SQLException {
      String version = null;
      Statement stmt = connection.createStatement();
      ResultSet columnSet = null;
      try {
         columnSet = stmt.executeQuery("SELECT substr(version,0,5) VERSION FROM " + schema + ".module WHERE module='FNDBAS'");
         if (columnSet.next()) {
            version = columnSet.getString(1);
         }
      } catch (SQLException ex) {
         return "4.1.0"; //work around for when the database isn't an IFS database
      } finally {
         stmt.close();
      }
      return version;
   }

   /**
    * Get the current database time
    * @return
    */
   public static String getCurrentTime(Connection connection) throws SQLException {
      String query = "SELECT TO_CHAR(SYSDATE, " + TIMESTAMP_FORMAT + ") CURRENT_TIME FROM DUAL";
      ResultSet tableSet = null;
      try {
         tableSet = connection.createStatement().executeQuery(query);
         while (tableSet.next()) {
            return tableSet.getString(1);
         }
      } finally {
         if (tableSet != null) {
            tableSet.close();
         }
      }
      return null;
   }

   /**
    * Get project name from database
    * @param conn
    * @param modules
    * @return
    */
   public static void getModules(DatabaseConnection connection, Map<String, String> modules) {
      if (connection != null) {
         ResultSet rs = null;
         PreparedStatement stmt = null;
         try {
            String query = "select lower(module) module, version from " + connection.getSchema() + ".module";
            stmt = connection.getJDBCConnection().prepareStatement(query);
            rs = stmt.executeQuery();
            while (rs.next()) {
               String module = rs.getString(1);
               String version = rs.getString(2);
               modules.put(module, version);
            }
         } catch (SQLException ex) {
            return; //work around for databases without fndbas
//            throw new RuntimeException(ex);
         } finally {
            try {
               if (stmt != null) {
                  stmt.close();
               }
            } catch (SQLException ex) {
               throw new RuntimeException(ex);
            }
         }
      }
   }

   /**
    * Get all the table names of the appowner
    * @param owner
    * @return
    */
   public static void getTableNames(String lastFetchDate, List<String> tables, List<DatabaseObjectInfo> objectInfoList, Connection connection) throws SQLException {
      getTableOrViewInfo("TABLE", lastFetchDate, tables, objectInfoList, connection);
   }

   /**
    * Get all the view names of the appowner
    * @param owner
    * @return
    */
   public static void getViewNames(String lastFetchDate, List<String> views, List<DatabaseObjectInfo> objectInfoList, Connection connection) throws SQLException {
      getTableOrViewInfo("VIEW", lastFetchDate, views, objectInfoList, connection);
   }

   /**
    * Get all the column names of the give table
    * @param name
    * @return
    */
   public static Map<String, String> getColumnNames(String name, String owner, Connection connection) throws SQLException {
      Map<String, String> columns = new LinkedHashMap<String, String>();
      PreparedStatement stmt = null;
      String query = "SELECT COLUMN_NAME,DATA_TYPE FROM ALL_TAB_COLUMNS WHERE TABLE_NAME=? AND OWNER=?";
      if(!name.startsWith("\"")) {
         name = name.toUpperCase(Locale.ENGLISH);
      } else {
         name = name.substring(1, name.length()-1);
      }
      try {
         if (connection != null) {
            stmt = connection.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setString(2, owner.toUpperCase(Locale.ENGLISH));
            ResultSet columnSet = stmt.executeQuery();
            while (columnSet.next()) {
               String column = columnSet.getString(1);
               if(isCaseInsensitiveDbName(column)) {
                  column = column.toLowerCase(Locale.ENGLISH);
               } else {
                  column = "\"" + column + "\"";
               }
               String datatype = columnSet.getString(2);
               columns.put(column, datatype);
            }
            columnSet.close();
         }
      } finally {
         if (stmt != null) {
            stmt.close();
         }
      }

      return columns;
   }

   public static Map<String, String> getColumnDataTypeLengths(String name, String owner, Connection connection) throws SQLException {
      Map<String, String> columns = new LinkedHashMap<String, String>();
      PreparedStatement stmt = null;
      String query = "SELECT COLUMN_NAME,DATA_LENGTH FROM ALL_TAB_COLUMNS WHERE TABLE_NAME=? AND OWNER=?";
      if(!name.startsWith("\"")) {
         name = name.toUpperCase(Locale.ENGLISH);
      } else {
         name = name.substring(1, name.length()-1);
      }
      try {
         if (connection != null) {
            stmt = connection.prepareStatement(query);
            stmt.setString(1, name);
            stmt.setString(2, owner.toUpperCase(Locale.ENGLISH));
            ResultSet columnSet = stmt.executeQuery();
            while (columnSet.next()) {
               String column = columnSet.getString(1);
               if(isCaseInsensitiveDbName(column)) {
                  column = column.toLowerCase(Locale.ENGLISH);
               } else {
                  column = "\"" + column + "\"";
               }
               int datatypeLength = columnSet.getInt(2);
               columns.put(column, Integer.toString(datatypeLength));
            }
            columnSet.close();
         }
      } finally {
         if (stmt != null) {
            stmt.close();
         }
      }

      return columns;
   }

   public static void getObjectNames(String objectType, String lastFetchDate, List<String> objects, List<DatabaseObjectInfo> owners, Connection connection) throws SQLException {
      CallableStatement plstmt = null;
      try {
         if (connection != null) {
            String block = "DECLARE "
                  + "   txt_ CLOB := to_clob('" + RECORD_SEPARATOR + "'); "
                  + "   CURSOR get_text(type_ VARCHAR2, last_fetch_date_ VARCHAR2) IS "
                  + "      SELECT OWNER||'" + FIELD_SEPARATOR + "'||OBJECT_NAME||'" + RECORD_SEPARATOR + "' text "
                  + "      FROM ALL_OBJECTS "
                  + "      WHERE object_type = type_ "
                  + "      AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + ");"
                  + "BEGIN "
                  + "   FOR rec_ IN get_text(?,?) LOOP "
                  + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                  + "   END LOOP; "
                  + "   ? := txt_; "
                  + "END;";
            plstmt = connection.prepareCall(block);
            plstmt.setString(1, objectType);
            plstmt.setString(2, lastFetchDate);
            plstmt.registerOutParameter(3, Types.CLOB);
            plstmt.execute();
            String[] result = plstmt.getString(3).split(RECORD_SEPARATOR);
            for (String item : result) {
               if (item != null && item.length() > 0) {
                  String objectName = item.substring(item.indexOf(FIELD_SEPARATOR)+1);
                  if(isCaseInsensitiveDbName(objectName)) {
                     objectName = DatabaseContentUtilities.formatPlsqlName(objectName);
                  } else {
                     objectName = "\"" + objectName + "\"";
                  }
                  owners.add(new DatabaseObjectInfo(item.substring(0, item.indexOf(FIELD_SEPARATOR))));
                  objects.add(objectName);
               }
            }
         }
      } finally {
         if (plstmt != null) {
            plstmt.close();
         }
      }
   }

   public static void getLogicalUnits(String lastFetchDate, List<String> objects, DatabaseConnection connection) throws SQLException {
      CallableStatement plstmt = null;
      try {
         if (connection != null) {
            String block = "DECLARE "
                  + "   txt_ CLOB := to_clob(':'); "
                  + "   CURSOR get_text(last_fetch_date_ VARCHAR2) IS "
                  + "      SELECT distinct(LU_NAME)||':' text "
                  + "      FROM " + connection.getSchema() + ".DICTIONARY_SYS_VIEW "
                  + "      WHERE VIEW_TYPE='B' "
                  + "      AND VIEW_NAME IN ("
                  + "         SELECT OBJECT_NAME "
                  + "            FROM ALL_OBJECTS "
                  + "            WHERE OBJECT_TYPE='VIEW' "
                  + "            AND OWNER=USER "
                  + "            AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + ") "
                  + "         UNION ALL "
                  + "         SELECT OBJECT_NAME "
                  + "            FROM ALL_OBJECTS "
                  + "            WHERE OBJECT_TYPE='VIEW' "
                  + "            AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + ") "
                  + "            AND OWNER <> USER "
                  + "            AND OBJECT_NAME NOT IN ( "
                  + "               SELECT OBJECT_NAME "
                  + "               FROM ALL_OBJECTS "
                  + "               WHERE OBJECT_TYPE='VIEW' "
                  + "               AND OWNER=USER));"
                  + "BEGIN "
                  + "   FOR rec_ IN get_text(?) LOOP "
                  + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                  + "   END LOOP; "
                  + "   ? := txt_; "
                  + "END;";
            plstmt = connection.getJDBCConnection().prepareCall(block);
            plstmt.setString(1, lastFetchDate);
            plstmt.registerOutParameter(2, Types.CLOB);
            plstmt.execute();
            String[] result = plstmt.getString(2).split(":");
            for (String item : result) {
               if (item != null && item.length() > 0) {
                  objects.add(item);
               }
            }
         }
      } catch (SQLException ex) {
         return; //work around for non-ifs databases
      } finally {
         if (plstmt != null) {
            plstmt.close();
         }
      }
   }

   public static void getEnumerationTypes(String lastFetchDate, List<String> objects, DatabaseConnection connection) throws SQLException {
      CallableStatement plstmt = null;
      try {
         if (connection != null) {
            String block = "DECLARE "
                  + "   txt_ CLOB := to_clob(':'); "
                  + "   CURSOR get_text IS "
                  + "      SELECT distinct(LU_NAME)||':' text "
                  + "      FROM " + connection.getSchema() + ".DICTIONARY_SYS_DOMAIN; "
                  + "BEGIN "
                  + "   FOR rec_ IN get_text LOOP "
                  + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                  + "   END LOOP; "
                  + "   ? := txt_; "
                  + "END;";
            plstmt = connection.getJDBCConnection().prepareCall(block);
            plstmt.registerOutParameter(1, Types.CLOB);
            plstmt.execute();
            String[] result = plstmt.getString(1).split(":");
            for (String item : result) {
               if (item != null && item.length() > 0) {
                  objects.add(item);
               }
            }
         }
      } catch (SQLException ex) {
         return; //work around for non-ifs databases
      } finally {
         if (plstmt != null) {
            plstmt.close();
         }
      }
   }

   public static void getTableOrViewInfo(String objectType, String lastFetchDate, List<String> objects, List<DatabaseObjectInfo> objectInfoList, Connection connection) throws SQLException {
      CallableStatement plstmt = null;
      try {
         if (connection != null) {
            String block = "DECLARE "
                  + "   txt_ CLOB := to_clob(':'); "
                  + "   CURSOR get_text(type_ VARCHAR2, last_fetch_date_ VARCHAR2) IS "
                  + "      SELECT A.OWNER||'.'||A.OBJECT_NAME "
                  + "             ||'. '||substr(COMMENTS, instr(COMMENTS, 'MODULE=')+7,"
                  + "                        instr(COMMENTS, '^', instr(COMMENTS, 'MODULE=')+7) - (instr(COMMENTS, 'MODULE=')+7)) "
                  + "             ||'. '||substr(COMMENTS, instr(COMMENTS, 'LU=')+3,"
                  + "                        instr(COMMENTS, '^', instr(COMMENTS, 'LU=')+3) - (instr(COMMENTS, 'LU=')+3))||':' text "
                  + "      FROM ALL_TAB_COMMENTS B , "
                  + "           (SELECT OBJECT_NAME, OWNER FROM ALL_OBJECTS "
                  + "            WHERE OBJECT_TYPE = type_ "
                  + "            AND OWNER = USER "
                  + "            AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + ") "
                  + "            UNION ALL "
                  + "            SELECT OBJECT_NAME, OWNER FROM ALL_OBJECTS "
                  + "            WHERE OBJECT_TYPE = type_ "
                  + "            AND OWNER <> USER "
                  + "            AND OBJECT_NAME NOT IN (SELECT OBJECT_NAME "
                  + "               FROM ALL_OBJECTS "
                  + "               WHERE OBJECT_TYPE = type_ "
                  + "               AND OWNER= USER) "
                  + "               AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + ")) A "
                  + "      WHERE A.OBJECT_NAME = B.TABLE_NAME "
                  + "      AND A.OWNER = B.OWNER;"
                  + "BEGIN "
                  + "   FOR rec_ IN get_text(?,?) LOOP "
                  + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                  + "   END LOOP; "
                  + "   ? := txt_; "
                  + "END;";
            plstmt = connection.prepareCall(block);
            plstmt.setString(1, objectType);
            plstmt.setString(2, lastFetchDate);
            plstmt.registerOutParameter(3, Types.CLOB);
            plstmt.execute();
            String[] result = plstmt.getString(3).split(":");
            for (String item : result) {
               if (item != null && item.length() > 0) {
                  String owner = item.split("\\.")[0];
                  String name = item.split("\\.")[1];
                  if(isCaseInsensitiveDbName(name)) {
                     name = name.toLowerCase(Locale.ENGLISH);
                  } else {
                     name = "\"" + name + "\"";
                  }
                  String module = item.split("\\.")[2].trim();
                  String luName = item.split("\\.")[3].trim();
                  objects.add(name);
                  DatabaseObjectInfo objectInfo = new DatabaseObjectInfo(owner, module, luName);
                  objectInfoList.add(objectInfo);
               }
            }
         }
      } finally {
         if (plstmt != null) {
            plstmt.close();
         }
      }
   }

   /**
    * Get all the sequences of the app owner
    * @param owner
    * @return
    */
   public static void getPackageNames(String lastFetchDate, List<String> pkgs, List<DatabaseObjectInfo> objectInfoList, Connection connection) throws SQLException {
      CallableStatement plstmt = null;
      try {
         if (connection != null) {
            String block = "DECLARE "
                  + "   txt_ CLOB := to_clob(':'); "
                  + "   CURSOR get_text(type_ VARCHAR2,last_fetch_date_ VARCHAR2) IS "
                  + "      SELECT OWNER||'.'||OBJECT_NAME||':' text "
                  + "      FROM ALL_OBJECTS "
                  + "      WHERE object_type = type_ "
                  + "      AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + ") "
                  + "      AND OWNER = USER "
                  + "      UNION ALL "
                  + "      SELECT OWNER||'.'||OBJECT_NAME||':' "
                  + "      FROM ALL_OBJECTS "
                  + "      WHERE OBJECT_TYPE = type_ "
                  + "      AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + ") "
                  + "      AND OWNER <> USER "
                  + "      AND OBJECT_NAME NOT IN ( "
                  + "         SELECT OBJECT_NAME "
                  + "         FROM ALL_OBJECTS "
                  + "         WHERE OBJECT_TYPE = type_ "
                  + "         AND OWNER= USER);"
                  + "BEGIN "
                  + "   FOR rec_ IN get_text('PACKAGE',?) LOOP "
                  + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                  + "   END LOOP; "
                  + "   ? := txt_; "
                  + "END;";
            plstmt = connection.prepareCall(block);
            plstmt.setString(1, lastFetchDate);
            plstmt.registerOutParameter(2, Types.CLOB);
            plstmt.execute();
            String[] result = plstmt.getString(2).split(":");
            DatabaseObjectInfo objectInfo = null;
            for (String item : result) {
               if (item != null && item.length() > 0) {
                  String owner = item.substring(0, item.indexOf('.'));
                  String name = item.substring(item.indexOf('.') + 1);
                  if(isCaseInsensitiveDbName(name)) {
                     name = DatabaseContentUtilities.formatPlsqlName(name);
                  } else {
                     name = "\"" + name + "\"";
                  }
                  pkgs.add(name);
                  objectInfo = new DatabaseObjectInfo(owner);
                  objectInfoList.add(objectInfo);
               }
            }
         }
      } finally {
         if (plstmt != null) {
            plstmt.close();
         }
      }
   }

   /**
    * Method that will return the meta data (module+LU name) for the given package_name
    * @param conn
    * @param packageName
    * @return
    */
   public static void getMetaDataForPackage(Connection conn, String packageName, DatabaseObjectInfo info) {
      String module = "";
      String luName = "";
      //If connected get the logical unit Name from Database
      ResultSet rs = null;
      packageName = packageName.toUpperCase(Locale.ENGLISH);
      PreparedStatement stmt = null;
      try {
         String query = "SELECT ltrim(text) TEXT FROM all_source a WHERE owner = ? AND name = ? "
               + "AND (type = 'PACKAGE BODY' OR type = 'PACKAGE') AND "
               + "(ltrim(text) like 'service_%:=%' OR ltrim(text) like 'lu_name_%:=%' OR ltrim(text) like 'module_%:=%') AND (line between 2 and 6)";
         stmt = conn.prepareStatement(query);
         stmt.setString(1, info.getOwner());
         stmt.setString(2, packageName);
         rs = stmt.executeQuery();
         while (rs.next()) {
            String text = rs.getString(1);
            int pos = text.indexOf("'") + 1;
            int end = text.indexOf("'", pos);
            if (end > pos) {
               String lowerCaseText = text.toLowerCase(Locale.ENGLISH);
               if (lowerCaseText.startsWith("module_ ")) {
                  module = (text.substring(pos, end)).toLowerCase(Locale.ENGLISH);
               } else if (lowerCaseText.startsWith("lu_name_ ") || lowerCaseText.startsWith("service_ ")) {
                  luName = text.substring(pos, end);
               }
            }
         }
      } catch (SQLException ex) {
         throw new RuntimeException(ex);
      } finally {
         try {
            if (stmt != null) {
               stmt.close();
            }
         } catch (SQLException ex) {
            throw new RuntimeException(ex);
         }
      }

      info.setLuName(luName);
      info.setModule(module);
   }

   
   public static Map<String, String> getPackageMembers(String packageName, String owner, Connection connection) throws SQLException {
      Map<String, String> result = new HashMap<String, String>();
      CallableStatement plstmt = null;
      try {
         if (connection != null) {
            String block = "DECLARE "
                  + "   txt_ CLOB := to_clob('" + RECORD_SEPARATOR + "'); "
                  + "   CURSOR get_text(package_name_ VARCHAR2, owner_ VARCHAR2) IS "
                  + "      SELECT object_type ||'" + FIELD_SEPARATOR + "' || ltrim(rtrim(name)) || '" + RECORD_SEPARATOR + "' data "
                  + "      FROM ("
                  + "      SELECT substr(text, 0, instr(text, ' ')) name, 'EXCEPTION' object_type "
                  + "      FROM (SELECT ltrim(s.text) TEXT "
                  + "            FROM ALL_OBJECTS ao, ALL_SOURCE s "
                  + "            WHERE ao.object_type = 'PACKAGE' "
                  + "            AND   s.type='PACKAGE' "
                  + "            AND   s.owner=owner_ "
                  + "            AND   s.owner=ao.owner "
                  + "            AND   s.name=ao.object_name "
                  + "            AND   upper(s.text) like '% EXCEPTION;%' "
                  + "            AND s.name=package_name_) "
                  + "      UNION "
                  + "      SELECT substr(text, 0, instr(text, ' ')) name, 'CONSTANT' object_type "
                  + "      FROM (SELECT ltrim(s.text) TEXT "
                  + "            FROM ALL_OBJECTS ao, ALL_SOURCE s "
                  + "            WHERE ao.object_type = 'PACKAGE' "
                  + "            AND   s.type='PACKAGE' "
                  + "            AND   s.owner=owner_ "
                  + "            AND   s.owner=ao.owner "
                  + "            AND   s.name=ao.object_name "
                  + "            AND   upper(s.text) like '% CONSTANT%' "
                  + "            AND s.name=package_name_) "
                  + "      UNION "
                  + "      SELECT substr(text, instr(text, ' ')+1, instr(upper(text), ' IS', instr(text, ' '))-instr(text, ' ')) name, 'CURSOR' object_type "
                  + "      FROM (SELECT ltrim(s.text) TEXT "
                  + "            FROM ALL_OBJECTS ao, ALL_SOURCE s "
                  + "            WHERE ao.object_type = 'PACKAGE' "
                  + "            AND   s.type='PACKAGE' "
                  + "            AND   s.owner=owner_ "
                  + "            AND   s.owner=ao.owner "
                  + "            AND   s.name=ao.object_name "
                  + "            AND   upper(ltrim(s.text)) like 'CURSOR %' "
                  + "            AND   s.name=package_name_)"
                  + "      UNION "
                  + "      SELECT substr(text, instr(text, ' ')+1, instr(upper(text), ' IS', instr(text, ' '))-instr(text, ' ')) name, 'TYPE' object_type "
                  + "      FROM (SELECT ltrim(s.text) TEXT "
                  + "            FROM ALL_OBJECTS ao, ALL_SOURCE s "
                  + "            WHERE ao.object_type = 'PACKAGE' "
                  + "            AND   s.type='PACKAGE' "
                  + "            AND   s.owner=owner_ "
                  + "            AND   s.owner=ao.owner "
                  + "            AND   s.name=ao.object_name "
                  + "            AND   upper(ltrim(s.text)) like 'TYPE %' "
                  + "            AND   s.name=package_name_))"
                  + "    ORDER BY OBJECT_TYPE; "
                  + "BEGIN "
                  + "   FOR rec_ IN get_text(?, ?) LOOP "
                  + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.data), rec_.data); "
                  + "   END LOOP; "
                  + "   ? := txt_; "
                  + "END;";
            plstmt = connection.prepareCall(block);
            if(packageName.startsWith("\"")) {
               packageName = packageName.substring(1, packageName.length()-1);
            } else {
               packageName=packageName.toUpperCase(Locale.ENGLISH);
            }
            plstmt.setString(1, packageName);
            plstmt.setString(2, owner);
            plstmt.registerOutParameter(3, Types.CLOB);
            plstmt.execute();
            String[] records = plstmt.getString(3).split(RECORD_SEPARATOR);
            for (String item : records) {
               if (item != null && item.length() > 0) {
                  String[] data = item.split(FIELD_SEPARATOR);
                  if(data.length==2) {
                     result.put(data[1], data[0]);
                  }
               }
            }
         }
      } finally {
         if (plstmt != null) {
            plstmt.close();
         }
      }
      return result;
   }
   
   /**
    * Check to see if an object is a database table
    * @param String name
    * @param Databaseconnection connection
    * @return
    */
   public static boolean isTable(String name, DatabaseConnection connection) {
      if (name == null) {
         return false;
      }
      if(!name.startsWith("\"")) {
         name = name.toUpperCase(Locale.ENGLISH);
      }
      String query = "SELECT 1 FROM ALL_TABLES "
            + "WHERE TABLE_NAME = ? "
            + "AND   ROWNUM<2";
      return runExistStatement(query, name, connection);
   }

   /**
    * Check to see if an object is a database view
    * @param String name
    * @param Databaseconnection connection
    * @return
    */
   public static boolean isView(String name, DatabaseConnection connection) {
      if (name == null) {
         return false;
      }
      if(!name.startsWith("\"")) {
         name = name.toUpperCase(Locale.ENGLISH);
      }
      String query = "SELECT 1 FROM ALL_VIEWS "
            + "WHERE VIEW_NAME = ? "
            + "AND   ROWNUM<2";
      return runExistStatement(query, name, connection);
   }

   /**
    * Check to see if an object is a database package
    * @param String name
    * @param Databaseconnection connection
    * @return
    */
   public static boolean isPackage(String name, DatabaseConnection connection) {
      if (name == null) {
         return false;
      }
      if(!name.startsWith("\"")) {
         name = name.toUpperCase(Locale.ENGLISH);
      }
      String query = "SELECT 1 FROM ALL_OBJECTS "
            + "WHERE OBJECT_NAME = ? "
            + "AND OBJECT_TYPE='PACKAGE' "
            + "AND   ROWNUM<2";
      return runExistStatement(query, name, connection);
   }

   /**
    * Check to see if an object is a database package
    * @param String name
    * @param Databaseconnection connection
    * @return
    */
   public static boolean isPackageSynonym(String name, DatabaseConnection connection) {
      if (name == null) {
         return false;
      }
      if(!name.startsWith("\"")) {
         name = name.toUpperCase(Locale.ENGLISH);
      }
      String query = "SELECT 1 FROM ALL_OBJECTS "
            + "WHERE OBJECT_NAME = ? "
            + "AND OBJECT_TYPE='PACKAGE' "
            + "AND   ROWNUM<2";
      return runExistStatement(query, name, connection);
   }

   /**
    * Check to see if an object is an enumeration type
    * @param String name
    * @param Databaseconnection connection
    * @return
    */
   public static boolean isEnumerationType(String name, DatabaseConnection connection) {
      if (name == null) {
         return false;
      }
      name = formatPlsqlName(name);
      String query = "SELECT 1 FROM " + connection.getSchema() + ".DICTIONARY_SYS_DOMAIN "
            + "WHERE LU_NAME = ? "
            + "AND   ROWNUM<2";
      return runExistStatement(query, name, connection);
   }

   public static Map<String, DatabaseObjectInfo> getObjectInfo(String name, DatabaseConnection connection, String lastPackageFetchTime, String lastViewFetchTime, String lastTableFetchTime, String lastSynonymSyncTime) {
      CallableStatement stmt = null;
      Map<String, DatabaseObjectInfo> databaseObjects = new HashMap<String, DatabaseObjectInfo>();
      if (name == null) {
         return null;
      }
      if(name.startsWith("\"")) {
         name = name.substring(1);
         if(name.endsWith("\"")) {
            name = name.substring(0, name.length()-1);
         } else {
            name = name + "%";
         }
      } else {
         name = name.toUpperCase(Locale.ENGLISH)+"%";
      }
      try {
         String block =   "DECLARE "
                        + "   txt_ CLOB := to_clob('"+RECORD_SEPARATOR+"'); "
                        + "   CURSOR get_text(name_ VARCHAR2,last_fetch_date_1_ VARCHAR2, last_fetch_date_2_ VARCHAR2, last_fetch_date_3_ VARCHAR2, last_fetch_date_4_ VARCHAR2) IS "
                        + "      SELECT A.OWNER || '"+FIELD_SEPARATOR+"' || A.OBJECT_NAME || '"+FIELD_SEPARATOR+"' || A.OBJECT_TYPE || '"+FIELD_SEPARATOR+"' || NVL(B.TABLE_NAME, '') TEXT FROM ALL_OBJECTS A, ALL_SYNONYMS B "
                        + "      WHERE A.OBJECT_NAME LIKE name_ "
                        + "      AND A.OBJECT_TYPE IN ('PACKAGE', 'VIEW', 'TABLE', 'FUNCTION', 'PROCEDURE', 'SYNONYM') "
                        + "      AND to_date(TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS') > LEAST(TO_DATE(last_fetch_date_1_, " + TIMESTAMP_FORMAT + "), TO_DATE(last_fetch_date_2_, " + TIMESTAMP_FORMAT + "), TO_DATE(last_fetch_date_3_, " + TIMESTAMP_FORMAT + "), TO_DATE(last_fetch_date_4_, " + TIMESTAMP_FORMAT + ")) "
                        + "      AND A.OBJECT_NAME=B.SYNONYM_NAME(+);"
                        + " BEGIN "
                        + "   FOR rec_ IN get_text(?, ?, ?, ?, ?) LOOP "
                        + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                        + "   END LOOP; "
                        + "   ? := txt_; "
                        + " END;";
         if (connection != null && connection.getJDBCConnection()!=null) {
            stmt = connection.getJDBCConnection().prepareCall(block);
            stmt.setString(1, name);
            stmt.setString(2, lastPackageFetchTime);
            stmt.setString(3, lastViewFetchTime);
            stmt.setString(4, lastTableFetchTime);
            stmt.setString(5, lastSynonymSyncTime);
            stmt.registerOutParameter(6, Types.CLOB);
            stmt.execute();
            String[] result = stmt.getString(6).split(RECORD_SEPARATOR);
            for (String item : result) {
               if (item != null && item.length() > 0) {
                  String[] entries = item.split(FIELD_SEPARATOR);
                  String owner = entries[0];
                  String objectName = entries[1];
                  String type = entries[2];
                  DatabaseObjectInfo dbObject = new DatabaseObjectInfo(owner, type);
                  databaseObjects.put(objectName, dbObject);
                  if("SYNONYM".equals(type) && entries.length>3 && entries[3]!=null && entries[3].length()>0) {
                     //transport the synonym "target" in the valuemap
                     Map values = new HashMap();
                     values.put("TABLE_NAME", entries[3]);
                     dbObject.setObjectData(values);
                  }

               }
            }
         }
      } catch (SQLException ex) {
         Exceptions.printStackTrace(ex);
      } finally {
         try {
            if (stmt != null) {
               stmt.close();
            }
         } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
         }
      }
      return databaseObjects;
   }

   private static boolean runExistStatement(String query, String name, DatabaseConnection connection) {
      PreparedStatement stmt = null;
      ResultSet rs = null;

      try {
         if (connection != null && connection.getJDBCConnection()!=null) {
            stmt = connection.getJDBCConnection().prepareStatement(query);
            stmt.setString(1, name);
            rs = stmt.executeQuery();
            return rs.next();
         }
      } catch (SQLException ex) {
         Exceptions.printStackTrace(ex);
      } finally {
         try {
            if (rs != null) {
               rs.close();
            }
            if (stmt != null) {
               stmt.close();
            }
         } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
         }
      }
      return false;
   }

   private static String formatType(String datatype, String typename, String subtype) {
      StringBuilder documentation = new StringBuilder();
      if (typename == null) {
         documentation.append("<font color=\"blue\">");
         documentation.append(datatype.toUpperCase(Locale.ENGLISH));
         documentation.append("</font>");
      } else {
         documentation.append(typename);
         if (subtype != null) {
            documentation.append(".").append(subtype);
         }
      }
      return documentation.toString();
   }

   private static StringBuilder formatArgument(StringBuilder documentation, String argument, String direction, String datatype, String typename, String subtype) {
      if (argument == null || "null".equalsIgnoreCase(argument)) //check for null name (== method with no arguments...)
      {
         return documentation;
      }
      if (documentation == null) {
         documentation = new StringBuilder();
         documentation.append("<table cellpadding=\"0\" cellspacing = \"3\"><tr><td>&nbsp;&nbsp;&nbsp;</td><td>");
      } else {
         documentation.append(",</td><tr><td>&nbsp;&nbsp;&nbsp;</td><td>");
      }
      documentation.append(argument.toLowerCase(Locale.ENGLISH));
      documentation.append("</td>");
      documentation.append("<td>&nbsp;&nbsp;&nbsp;<font color=\"blue\">");
      if ("IN/OUT".equals(direction)) {
         direction = IN_OUT;
      }
      documentation.append(direction);
      documentation.append("</font></td>");
      documentation.append("<td>&nbsp;&nbsp;&nbsp;");
      documentation.append(formatType(datatype, typename, subtype));
      return documentation;
   }

   private static StringBuilder appendKeyWord(StringBuilder doc, String keyword) {
      doc.append("<font color=\"blue\">");
      doc.append(keyword.toUpperCase(Locale.ENGLISH));
      doc.append("</font>");
      return doc;
   }

   private static boolean isKeyWord(String str) {
      str = str.toUpperCase(Locale.ENGLISH);
      return (str.equals("VARCHAR2")
            || str.equals("BOOLEAN")
            || str.equals("DATE")
            || str.equals("NUMBER")
            || str.equals("TRUE")
            || str.equals("FALSE")
            || str.equals("RAW")
            || str.equals("LONG")
            || str.equals("BLOB")
            || str.equals("CLOB")
            || str.equals("INTEGER")
            || str.equals("VARCHAR")
            || str.equals("CHAR")
            || str.equals("NULL"));
   }

   private static StringBuilder append(StringBuilder doc, String str) {
      if (isKeyWord(str)) {
         appendKeyWord(doc, str.toUpperCase(Locale.ENGLISH));
      } else {
         doc.append(str);
      }
      return doc;
   }

   //This method will be called with methodBody from files as well as from the database and may contain syntax errors
   //We need to handle this and not assume that the methodBody is properly formated.
   public static String formatMethodDoc(String packageName, String methodName, String methodBody, String owner, Connection connection) throws SQLException {
      if (methodBody == null || methodBody.length() == 0) {
         return "";
      }
      String dbDoc = "";
      if(packageName != null && owner != null) {
         dbDoc = getMethodDocumentation(packageName, methodName, owner, connection);
      }
      return formatMethodDoc(packageName, methodName, methodBody, dbDoc);
   }

   public static String formatMethodDoc(String packageName, String methodName, String methodBody, String methodDocumentation)  {
      if (methodBody == null || methodBody.length() == 0) {
         return "";
      }
      methodBody = methodBody.toUpperCase(Locale.ENGLISH);
      StringBuilder documentation = new StringBuilder();
      String returnType = null;
      if (methodBody.substring(0, 1).equalsIgnoreCase("F")) {
         appendKeyWord(documentation, "FUNCTION");
      } else {
         appendKeyWord(documentation, "PROCEDURE");
      }
      documentation.append("&nbsp;<b>").append(methodName).append("</b>");

      int paramStart = methodBody.indexOf('(');
      int paramEnd = methodBody.lastIndexOf(')');
      int returnPos = methodBody.lastIndexOf("RETURN");
      if (returnPos > paramEnd) {
         returnType = methodBody.substring(returnPos + 7).trim();
         int pos = returnType.indexOf(";");
         if (pos > 0) {
            returnType = returnType.substring(0, pos);
         }
      }
      if (paramStart > paramEnd) {
         documentation.append("<font color=\"red\"><br>Syntax error - could not find ) after last parameter</font>");
      }
      String errorTxt = null;
      if (paramStart > -1 && paramEnd > -1) {
         documentation.append("&nbsp;(<br>");
         documentation.append("<table cellpadding=\"0\" cellspacing = \"3\"><tr><td>&nbsp;&nbsp;&nbsp;</td><td>");
         String parameters = methodBody.substring(paramStart + 1, paramEnd).trim();
         StringTokenizer tokens = new StringTokenizer(parameters, ",");
         while (tokens.hasMoreTokens()) {
            StringTokenizer parts = new StringTokenizer(tokens.nextToken().trim(), " ");
            //arg IN/OUT TYPE DEFAULT 20
            String name = parts.nextToken();
            String direction = null;
            if (!parts.hasMoreTokens()) { //syntax error
               errorTxt = "<font color=\"red\"><br>Syntax error - expected direction and type after parameter \"" + name + "\"</font>";
            } else {
               direction = parts.nextToken();
            }
            String type = null;
            if (parts.hasMoreTokens()) { //parameters defined without direction are IN parameters
               type = parts.nextToken();
            } else if (errorTxt == null) {
               if (direction.equalsIgnoreCase("IN") || direction.equalsIgnoreCase("OUT")) { //syntax error
                  errorTxt = "<font color=\"red\"><br>Syntax error - no type given for parameter \"" + name + "\"</font>";
               }
               type = direction;
               direction = "IN";
            }

            if (errorTxt == null && "OUT".equalsIgnoreCase(type)) {
               direction = "IN OUT";
               if (!parts.hasMoreTokens()) { //syntax error
                  errorTxt = "<font color=\"red\"><br>Syntax error - no type given for parameter \"" + name + "\"</font>";
               } else {
                  type = parts.nextToken();
               }
            }
            documentation.append(name.toLowerCase(Locale.ENGLISH));
            documentation.append("</td><td>&nbsp;&nbsp;&nbsp;");
            if (errorTxt == null) {
               appendKeyWord(documentation, direction);
            }
            documentation.append("</td><td>&nbsp;&nbsp;&nbsp;");
            if (errorTxt != null) {
               break;
            }
            int assignPos = type.indexOf(":=");
            String assignValue = null;
            if (assignPos > 0) {
               assignValue = type.substring(assignPos + 2).trim();
               type = type.substring(0, assignPos);
            }
            append(documentation, type);

            if (assignValue != null) {
               documentation.append(" := ");
               append(documentation, assignValue);
            }

            if (parts.hasMoreTokens()) {
               String next = parts.nextToken();
               if (next.equalsIgnoreCase("DEFAULT")) {
                  if (parts.hasMoreTokens()) {

                     appendKeyWord(documentation, "&nbsp;DEFAULT&nbsp;");
                     String defaulting = parts.nextToken();
                     append(documentation, defaulting);
                  }
               } else {
                  append(documentation, next);
               }
               while (parts.hasMoreTokens()) {
                  String value = parts.nextToken();
                  documentation.append(" ");
                  append(documentation, value);
               }
            }

            if (tokens.hasMoreTokens()) {
               documentation.append(",</td>");
               if (returnType != null) { //add empty column for return statement
                  documentation.append("<td></td>");
               }
               documentation.append("</tr><tr><td>&nbsp;&nbsp;&nbsp;</td><td>");
            }
         }
      }
      if (returnType != null) {
         if (paramStart > -1) {
            documentation.append("&nbsp;)</td><td>");
         }
         appendKeyWord(documentation, "&nbsp;&nbsp;RETURN&nbsp;&nbsp;");
         append(documentation, returnType);
         documentation.append(";");
      } else if (paramStart > -1 && paramEnd > -1) {
         documentation.append("&nbsp;);</td><td>");
      }

      if (paramStart > -1 && paramEnd > -1) {
         documentation.append("</td></tr></table>");
      }
      if (errorTxt != null) {
         documentation.append(errorTxt).append("<br>");
      } else if(methodDocumentation.length()>0) {
         documentation.append("<br>").append(methodDocumentation);
      }

      return documentation.toString();
   }

   /**
    * Convert specified name to IFS PL/SQL Syntax.
    * @param name upper case string to be converted to mixed "IFS" case
    * @return the specified string converted to mixed case
    */
   public static String formatPlsqlName(String name) {
      int len = name.length();
      StringBuilder buf = new StringBuilder(len);
      boolean upper = true;

      name = name.toUpperCase(Locale.ENGLISH);
      String suffix = null;
      if (name.endsWith("_API")) {
         suffix = "_API";
         len = len - 4;
      } else if (name.endsWith("_SYS")) {
         suffix = "_SYS";
         len = len - 4;
      } else if (name.endsWith("_RPI")) {
         suffix = "_RPI";
         len = len - 4;
      }

      for (int i = 0; i < len; i++) {
         char ch = name.charAt(i);
         if (ch == '_') {
            upper = true;
         } else if (upper) {
            ch = Character.toUpperCase(ch);
            upper = false;
         } else {
            ch = Character.toLowerCase(ch);
         }

         buf.append(ch);
      }

      if (suffix != null) {
         buf.append(suffix);
      }
      return buf.toString();
   }

   /**
    * Get method documentation for the given package/method
    * @param packageName
    *  @param methodname
    * @return pl/sql doc for method
    */
   public static String getMethodDocumentation(String packageName, String methodName, String owner, Connection connection) throws SQLException {
      if (packageName == null) {
         return "";
      }
      StringBuilder doc = new StringBuilder();
      packageName = packageName.toUpperCase(Locale.ENGLISH);
      methodName = methodName.toUpperCase(Locale.ENGLISH);
      PreparedStatement stmt = null;
      if (methodName.indexOf("'") > -1 || methodName.indexOf('"') > -1) {
         return "";
      }
      try {
         String query = "SELECT max(line) line FROM all_source a WHERE name = ? AND type = 'PACKAGE BODY' AND upper(translate(text,'A'||chr(10)||chr(13),'A')) = '-- '|| ? AND OWNER = ?";
         if (connection != null) {
            stmt = connection.prepareStatement(query);
            stmt.setString(1, packageName);
            stmt.setString(2, methodName);
            stmt.setString(3, owner);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
               int line = rs.getInt(1);
               rs.close();
               stmt.close();
               query = "SELECT translate(text,'A'||chr(10)||chr(13),'A')||'    ' text FROM all_source a WHERE name = ? AND type = 'PACKAGE BODY' AND line BETWEEN ? and ? and OWNER = ?";
               stmt = connection.prepareStatement(query);
               stmt.setString(1, packageName);
               stmt.setInt(2, line + 1);
               stmt.setInt(3, line + 50);
               stmt.setString(4, owner);
               rs = stmt.executeQuery();
               while (rs.next()) {
                  String text = rs.getString(1);
                  if (text.length() < 5 || !text.substring(3, 4).equalsIgnoreCase(" ")) {
                     break;
                  }
                  text = text.substring(4);
                  int i = 0;
                  for (; i < text.length() - 1 && text.charAt(i) == ' '; i++) {
                     doc.append("&nbsp;");
                  }
                  doc.append(text.substring(i));
                  doc.append("<br>");
               }
               rs.close();
            }
         }
      } finally {
         if (stmt != null) {
            stmt.close();
         }
      }
      return doc.toString();
   }

   /**
    * Get all the method names of the given package
    * @param packageName
    * @return Map of methods in the package
    */
   public static Map<String, String> getMethodNames(String packageName, String owner, Connection connection) throws SQLException {
      Map<String, String> methods = new HashMap<String, String>();
      CallableStatement stmt = null;
      String appPkgName = packageName.startsWith("\"") ? packageName.substring(1, packageName.length()-1) : packageName.toUpperCase(Locale.ENGLISH);

      try { 
         if (connection != null) {
            String query = "DECLARE "+
                           "   CURSOR get_methods(owner_ VARCHAR2, package_name_ VARCHAR2) IS "+
                           "      SELECT line, ltrim(rtrim(translate(text,'A'||chr(10)||chr(13),'A'))) text "+
                           "      FROM all_source a "+
                           "      WHERE name = package_name_ "+
                           "      AND TYPE = 'PACKAGE' "+
                           "      AND (upper(substr(ltrim(text), 0, 10))='PROCEDURE ' OR upper(substr(ltrim(text), 0, 9))='FUNCTION ') "+
                           "      AND OWNER = owner_; "+
                           "   CURSOR get_method_details(owner_ VARCHAR2, package_name_ VARCHAR2, line_no_ NUMBER) IS "+
                           "      SELECT translate(text,'A'||chr(10)||chr(13),'A') text "+
                           "      FROM all_source a "+
                           "      WHERE name = package_name_ "+
                           "      AND TYPE = 'PACKAGE' "+
                           "      AND line > line_no_ "+
                           "      AND substr(text, 0, 2)<>'--' "+
                           "      AND OWNER = owner_; "+
                           "   CURSOR get_method_docs_start(owner_ VARCHAR2, package_name_ VARCHAR2, method_name_ VARCHAR2) IS "+
                           "      SELECT max(line) line "+
                           "      FROM all_source a "+
                           "      WHERE name = package_name_ "+
                           "      AND TYPE = 'PACKAGE BODY' "+
                           "      AND upper(translate(text,'A'||chr(10)||chr(13),'A')) = '-- '|| method_name_ "+
                           "      AND OWNER = owner_; "+
                           "   CURSOR get_method_docs(owner_ VARCHAR2, package_name_ VARCHAR2, line_no_ NUMBER) IS "+
                           "      SELECT text"+
                           "      FROM all_source a "+
                           "      WHERE name = package_name_ "+
                           "      AND TYPE = 'PACKAGE BODY' "+
                           "      AND line BETWEEN line_no_+1 AND line_no_+50 "+
                           "      AND OWNER = owner_; "+
                           "   pos1 NUMBER; "+
                           "   pos2 NUMBER; "+
                           "   line_no NUMBER; "+
                           "   method_name VARCHAR2(100); "+
                           "   owner_        VARCHAR2(100):=?; "+
                           "   package_name_ VARCHAR2(100):=?; "+
                           "   txt_ CLOB := to_clob('" + RECORD_SEPARATOR + "'); "+
                           "   first_char VARCHAR2(1); "+
                           "BEGIN "+
                           "   FOR methods_ IN get_methods(owner_, package_name_) LOOP "+
                           "      pos1 := instr(methods_.text, ' '); "+
                           "      pos2 := instr(methods_.text, '(', pos1); "+
                           "      IF pos2=0 THEN "+
                           "         pos2 := instr(methods_.text, ' ', pos1+1); "+
                           "      END IF; "+
                           "      IF pos2=0 THEN "+
                           "         pos2 := instr(methods_.text, ';', pos1); "+
                           "      END IF; "+
                           "      IF pos2=0 THEN "+
                           "         pos2 := length(methods_.text); "+
                           "      END IF; "+
                           "      method_name := ltrim(rtrim(substr(methods_.text, pos1, pos2-pos1))); "+
                           "      IF length(method_name)>0 THEN  "+
                           "         first_char := substr(method_name, 0, 1); "+
                           "         IF first_char<>'''' AND first_char<>'\"' THEN "+
                           "            Dbms_LOB.writeappend (txt_,length(method_name), upper(method_name)); "+
                           "            Dbms_LOB.writeappend (txt_, 1, '" + FIELD_SEPARATOR + "'); "+
                           "            Dbms_LOB.writeappend (txt_,length(methods_.text), methods_.text); "+
                           "            pos1:=instr(methods_.text, ';'); "+
                           "            pos2:=instr(methods_.text, '--'); "+
                           "            IF pos1=0 OR (pos2>0 AND pos2<pos1) THEN "+
                           "               FOR rec_ IN get_method_details(owner_, package_name_, methods_.line) LOOP "+
                           "                  Dbms_LOB.writeappend (txt_,length(rec_.text), rec_.text); "+
                           "                  pos1:=instr(rec_.text, ';'); "+
                           "                  pos2:=instr(rec_.text, '--'); "+
                           "                  IF pos1>0 AND (pos2=0 OR pos1<pos2) THEN "+
                           "                     EXIT; "+
                           "                  END IF; "+
                           "               END LOOP; "+
                           "            END IF; "+
                           "            OPEN get_method_docs_start(owner_, package_name_, upper(method_name)); "+
                           "            FETCH get_method_docs_start INTO line_no; "+
                           "            IF get_method_docs_start%FOUND AND line_no IS NOT NULL THEN "+
                           "               Dbms_LOB.writeappend (txt_, 1, '" + FIELD_SEPARATOR + "'); "+
                           "               FOR lines_ IN get_method_docs(owner_, package_name_, line_no) LOOP "+
                           "                  IF length(lines_.text)<5 OR instr(lines_.text, '--  ')=0 THEN "+
                           "                     EXIT; "+
                           "                  END IF; "+
                           "                  Dbms_LOB.writeappend (txt_,length(lines_.text)-4, substr(lines_.text, 4)); "+
                           "                  Dbms_LOB.writeappend (txt_,4, '<br>'); "+
                           "               END LOOP; "+
                           "            END IF; "+
                           "            CLOSE get_method_docs_start; "+
                           "            Dbms_LOB.writeappend (txt_, 1, '" + RECORD_SEPARATOR + "'); "+
                           "         END IF; "+
                           "      END IF; "+
                           "   END LOOP; "+
                           "   ? := txt_; "+
                           "END; ";

            stmt = connection.prepareCall(query);
            stmt.setString(1, owner);
            stmt.setString(2, appPkgName);
            stmt.registerOutParameter(3, Types.CLOB);
            stmt.execute();
            String text = stmt.getString(3); //the full text with all required info
            String methodBlocks[] = text.split(RECORD_SEPARATOR);
            for(int i=0; i<methodBlocks.length; i++) {
               String methodFields[] = methodBlocks[i].split(FIELD_SEPARATOR);
               //first line contains the method name
               if(methodFields.length>1) {
                  String methodName = formatPlsqlName(methodFields[0]);
                  String methodBody = methodFields[1];
                  String documentation = methodFields.length==3 ? methodFields[2] : "";
                  documentation = documentation.replaceAll(" ", "&nbsp;");
                  String uniqueName = methodName;
                  if (methods.containsKey(uniqueName)) { // overloading
                     int index = 0;
                     while (methods.containsKey(methodName + ":" + Integer.toString(index))) {
                        index++;
                     }
                     uniqueName = methodName + ":" + Integer.toString(i);
                  }
                  methods.put(uniqueName, formatMethodDoc(packageName, methodName, methodBody, documentation));
               }
            }
         }
      } finally {
         if (stmt != null) {
            stmt.close();
         }
      }
      return methods;
   }

   public static Map<String, String> getSynonyms(Connection connection, String lastDDLTime) throws SQLException {
      Map<String, String> result = new HashMap<String, String>();
      CallableStatement plstmt = null;
      try {
         if (connection != null) {
            String block = "DECLARE "
                  + "   txt_ CLOB := to_clob('" + RECORD_SEPARATOR + "'); "
                  + "   CURSOR get_text(last_fetch_date_ VARCHAR2) IS "
                  + "      SELECT a.synonym_name||'" + FIELD_SEPARATOR + "'||a.table_name||'" + RECORD_SEPARATOR + "' text "
                  + "      FROM  all_synonyms a, all_objects b "
                  + "      WHERE a.synonym_name=b.object_name "
                  + "      AND   b.object_type='SYNONYM' "
                  + "      AND   to_date(b.TIMESTAMP, 'YYYY-MM-DD:HH24:MI:SS')>TO_DATE(last_fetch_date_, " + TIMESTAMP_FORMAT + "); "
                  + "BEGIN "
                  + "   FOR rec_ IN get_text(?) LOOP "
                  + "      DBMS_LOB.writeappend (txt_,LENGTH (rec_.text), rec_.text); "
                  + "   END LOOP; "
                  + "   ? := txt_; "
                  + "END;";
            plstmt = connection.prepareCall(block);
            plstmt.setString(1, lastDDLTime);
            plstmt.registerOutParameter(2, Types.CLOB);
            plstmt.execute();
            String[] items = plstmt.getString(2).split(RECORD_SEPARATOR);
            for (String item : items) {
               if (item != null && item.length() > 0) {
                  String name = item.substring(0, item.indexOf(FIELD_SEPARATOR));
                  if(isCaseInsensitiveDbName(name)) {
                     name = name.toLowerCase(Locale.ENGLISH);
                  } else {
                     name = "\"" + name + "\"";
                  }
                  String table = item.substring(item.indexOf(FIELD_SEPARATOR) + 1);
                  result.put(name, table);
               }
            }
         }
      } finally {
         if (plstmt != null) {
            plstmt.close();
         }
      }
      return result;
   }

   public static boolean isCaseInsensitiveDbName(String name) {
      return isValidOracleUppercaseIdentifier(name);
   }


   public static boolean isValidOracleUppercaseIdentifier(final String value) {
      return value.matches(ORACLE_UPPERCASE_IDENTIFIER);
   }
   
   /**
    * Get all the functions belonging to the app owner
    * @param owner
    * @return
    */
   public static void getFunctions(String lastFetchDate, List<String> sequences, List<DatabaseObjectInfo> owners, Connection connection) throws SQLException {
      getObjectNames("FUNCTION", lastFetchDate, sequences, owners, connection);
   }
   /**
    * Get all the procedures belonging to the app owner
    * @param owner
    * @return
    */
   public static void getProcedures(String lastFetchDate, List<String> sequences, List<DatabaseObjectInfo> owners, Connection connection) throws SQLException {
      getObjectNames("PROCEDURE", lastFetchDate, sequences, owners, connection);
   }

   /**
    * Get all the sequences belonging to the app owner
    * @param owner
    * @return
    */
   public static void getSequences(String lastFetchDate, List<String> sequences, List<DatabaseObjectInfo> owners, Connection connection) throws SQLException {
      getObjectNames("SEQUENCE", lastFetchDate, sequences, owners, connection);
   }

   /**
    * This method will return the Meta data information of the resultset of given sql
    * @param sqlStmt
    * @return
    */
   public static String[] getMetaData(String sqlStmt, Connection connection) throws SQLException {
      if (connection != null) {
         PreparedStatement pstmt = connection.prepareStatement(sqlStmt);
         pstmt.setFetchSize(1);
         pstmt.execute();
         ResultSetMetaData rsMetaData = pstmt.getMetaData();
         if (rsMetaData != null) {
            int colCount = rsMetaData.getColumnCount();
            String[] columns = new String[colCount];
            for (int i = 0; i < colCount; i++) {
               columns[i] = rsMetaData.getColumnName(i + 1);
            }

            return columns;
         }
      }
      return null;
   }

   /**
    * Get model
    * @param objectName
    * @param type
    * @param connection
    * @return
    */
   public static DatabaseModelObjectInfo getModelObject(String objectName, String type, DatabaseConnection connection) throws SQLException {
      PreparedStatement stmt = null;
      DatabaseModelObjectInfo modelObject = null;
      //FETCH MODEL
      String query = "SELECT COMPONENT, MODEL_FILE FROM " + connection.getSchema() + ".FNDBAS_MODEL_OBJECT_TAB WHERE OBJECT_TYPE=? AND OBJECT_NAME=?";
      try {
         if (connection != null) {
            stmt = connection.getJDBCConnection().prepareStatement(query);
            stmt.setString(1, type);
            stmt.setString(2, objectName);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
               modelObject = new DatabaseModelObjectInfo(objectName, rs.getString(1), type, rs.getBytes(2));
            }
            rs.close();
            stmt.close();
         }
      } catch (SQLException ex) {
         return null; //work around for non-ifs databases
      } finally {
         if (stmt != null) {
            stmt.close();
         }
      }
      return modelObject;
   }

   /**
    * Get models updated after lastFetchDate
    * @param lastFetchDate
    * @param type
    * @param models
    * @param connection
    * @return lastImportFileDate
    */
   public static String getModelNames(String lastFetchDate, Map<String, Set<String>> models, DatabaseConnection connection) throws SQLException {
      PreparedStatement stmt = null;

      //FETCH MODEL NAMES
      String query = "SELECT object_name, object_type, to_char(import_file_stamp, " + TIMESTAMP_FORMAT + ") import_file_stamp "
            + "FROM " + connection.getSchema() + ".fndbas_model_object_tab "
            + "WHERE import_file_stamp > to_date(?, " + TIMESTAMP_FORMAT + ") "
            + "ORDER BY import_file_stamp DESC";
      try {
         String timestamp = null;
         if (connection != null) {
            stmt = connection.getJDBCConnection().prepareStatement(query);
            stmt.setString(1, lastFetchDate);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
               Set<String> modelList = models.get(rs.getString(2));
               if (modelList == null) {
                  modelList = new HashSet<String>();
                  models.put(rs.getString(2), modelList);
               }
               modelList.add(rs.getString(1));
               if (timestamp == null) {
                  timestamp = rs.getString(3);
               }
            }
            rs.close();
            stmt.close();
         }
         return timestamp != null ? timestamp : lastFetchDate;
      } catch (SQLException ex) {
         return null; //work around for non-ifs databases
      } finally {
         if (stmt != null) {
            stmt.close();
         }
      }
   }

   /**
    * Method that will return the last modified time of the given model object
    * @param name
    * @param type
    * @param connection
    * @return
    */
   public static long getLastModifiedTime(String name, String type, DatabaseConnection connection) throws SQLException {
      {
         Date timeStamp = null;
         DateFormat dfm = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

         if (connection != null) {
            try {
               List<String> objList = new ArrayList<String>();
               ResultSet objSet = null;

               String query = "SELECT FILE_TIME_STAMP FROM FNDBAS_MODEL_OBJECT_TAB A WHERE OBJECT_TYPE = ? AND OBJECT_NAME= ?";
               PreparedStatement stmt = null;
               try {
                  stmt = connection.getJDBCConnection().prepareStatement(query);
                  stmt.setString(1, type);
                  stmt.setString(2, name);
                  objSet = stmt.executeQuery();
                  while (objSet.next()) {
                     objList.add(objSet.getString("FILE_TIME_STAMP"));
                  }
               } finally {
                  if (stmt != null) {
                     stmt.close();
                  }
               }
               if (objList.size() > 0) {
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
