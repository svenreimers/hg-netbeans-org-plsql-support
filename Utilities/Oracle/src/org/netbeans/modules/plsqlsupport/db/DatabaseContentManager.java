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

import java.beans.ExceptionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;

public class DatabaseContentManager {

   private static final String cacheDir = System.getProperty("netbeans.user") + "/var/cache/plsql";
   private static final String sequenceCacheFile = "all_sequences.cache";
   private static final String tableCacheFile = "all_tables.cache";
   private static final String packageCacheFile = "all_packages.cache";
   private static final String ownerCacheFile = "all_object_owners.cache";
   private static final String viewCacheFile = "all_views.cache";
   private static final String moduleCacheFile = "all_modules.cache";
   private static final String luCacheFile = "all_lus.cache";
   private static final String enumerationCacheFile = "all_enumerations.cache";
   private static final String synonymCacheFile = "all_synonyms.cache";
   private static final String functionCacheFile = "all_functions.cache";
   private static final String procedureCacheFile = "all_procedures.cache";
   private static final String LAST_FETCH_TIME = "LFT";
   private static final String TABLES = "TABLES";
   private static final String VIEWS = "VIEWS";
   private static final String PACKAGES = "PKGS";
   private static final String PACKAGE_INFO = "PKG_INFO";
   private static final String SEQUENCES = "SEQ";
   private static final String ENUMERATIONS = "ENUMERATIONS";
   private static final String LUS = "LUS";
   private static final String VIEW_SYNONYMS = "VIEW_SYNONYMS";
   private static final String TABLE_SYNONYMS = "TABLE_SYNONYMS";
   private static final String PKG_SYNONYMS = "PKG_SYNONYMS";
   private static final String FUNCTIONS = "FUNCTIONS";
   private static final String PROCEDURES = "PROCEDURES";
   private static final String BEGINNING_OF_TIME = "1900/01/01:00:00:00";
   private static Map<String, DatabaseContentManager> instances = new HashMap<String, DatabaseContentManager>();
   private String directory;
   private String user;
   private String lastPackageSyncTime = BEGINNING_OF_TIME;
   private String lastViewSyncTime = BEGINNING_OF_TIME;
   private String lastTableSyncTime = BEGINNING_OF_TIME;
   private String lastSequenceSyncTime = BEGINNING_OF_TIME;
   private String lastEnumerationSyncTime = BEGINNING_OF_TIME;
   private String lastLUSyncTime = BEGINNING_OF_TIME;
   private String lastSynonymSyncTime = BEGINNING_OF_TIME;
   private String lastFunctionSyncTime = BEGINNING_OF_TIME;
   private String lastProcedureSyncTime = BEGINNING_OF_TIME;
   private Map<String, DatabaseObjectInfo> tableNameMap = new HashMap<String, DatabaseObjectInfo>();
   private Map<String, DatabaseObjectInfo> viewNameMap = new HashMap<String, DatabaseObjectInfo>();
   private Map<String, DatabaseObjectInfo> pkgNameMap = new HashMap<String, DatabaseObjectInfo>();
   private Map<String, Map<String, String>> pkgInfoMap = new HashMap<String, Map<String, String>>();
   private Map<String, DatabaseObjectInfo> seqMap = new HashMap<String, DatabaseObjectInfo>();
   private Map<String, String> ownerMap = new HashMap<String, String>();
   private final Set<String> schemaSet = new HashSet<String>();
   private Set<String> enumerationSet = new HashSet<String>();
   private Set<String> logicalUnitSet = new HashSet<String>();
   private Map<String, String> viewSynonyms = new HashMap<String, String>();
   private Map<String, String> tableSynonyms = new HashMap<String, String>();
   private Map<String, String> packageSynonyms = new HashMap<String, String>();
   private Map<String, String> moduleVersionMap = new HashMap<String, String>();
   private final Map<String, DatabaseObjectInfo> functionNameMap = new HashMap<String, DatabaseObjectInfo>();
   private final Map<String, DatabaseObjectInfo> procedureNameMap = new HashMap<String, DatabaseObjectInfo>();
   private RequestProcessor.Task updateThread = null;
   private static final RequestProcessor PROCESSOR = new RequestProcessor(DatabaseContentManager.class.getName());
   private boolean disconnecting = false;
   private final Object updateLock = new Object();
   private final List<ExceptionListener> listeners = new ArrayList<ExceptionListener>();

   private DatabaseContentManager(DatabaseConnection connection) {
      String databaseURL = connection.getDatabaseURL();
      int pos = connection.getDatabaseURL().lastIndexOf("@");
      if (pos > 0) {
         databaseURL = databaseURL.substring(pos + 1);
      }
      if (databaseURL.startsWith("//")) {
         databaseURL = databaseURL.substring(2);
      }
      databaseURL = databaseURL.replaceAll("[:/]", ".");
      this.user = connection.getUser().toUpperCase(Locale.ENGLISH);
      this.directory = cacheDir + "/" + this.user + "@" + databaseURL.toUpperCase(Locale.ENGLISH);
   }

   public static DatabaseContentManager getInstance(DatabaseConnection connection) {
      if (connection == null) {
         return null;
      }
      String key = connection.getUser() + "@" + connection.getDatabaseURL();
      DatabaseContentManager instance = instances.get(key);
      if (instance == null) {
         instance = new DatabaseContentManager(connection);
         instances.put(key, instance);
      }
      return instance;
   }

   public void disconnectingFromDatabase() {
      disconnecting = true;
   }

   public void initializeCache(DatabaseConnectionManager connectionManager) {
      updateCache(connectionManager, false);
   }

   public void updateCache(DatabaseConnectionManager connectionManager) {
      updateCache(connectionManager, false);
   }

   private void extractSchemas(Collection<String> schemas) {
      for (String schema : schemas) {
         addSchema(schema);
      }
   }

   private void addSchema(String schema) {
      if (schema == null) {
         return;
      }
      schemaSet.add(schema);
   }

   @SuppressWarnings("unchecked")
   public void updateCache(final DatabaseConnectionManager connectionManager, final boolean forceUpdate) {
      if (updateThread == null || updateThread.isFinished()) {
         updateThread = PROCESSOR.post(new Runnable() {

            @Override
            public void run() {
               if (ownerMap == null || ownerMap.isEmpty()) {
                  Map map = readMapFromFile(getCacheDir(), ownerCacheFile);
                  if (map != null) {
                     ownerMap = map;
                  }
               }
               DatabaseConnection connection = ((forceUpdate || ownerMap == null || ownerMap.isEmpty()) && connectionManager != null) ? connectionManager.getPooledDatabaseConnection(true) : null;
               try {
                  synchronousCacheUpdate(connection, forceUpdate);
                  extractSchemas(ownerMap.values());
               } finally {
                  if (connectionManager != null) {
                     connectionManager.releaseDatabaseConnection(connection);
                  }
               }
            }
         });
      }
   }

   private void synchronousCacheUpdate(final DatabaseConnection connection, final boolean forceUpdate) {
      ProgressHandle handle = ProgressHandleFactory.createHandle("Updating database cache...");
      handle.start(10);
      try {
         disconnecting = false;
         synchronized (updateLock) {
            try {
               int workunit = 0;
               String verb = forceUpdate ? "Updating" : "Initializing";
               handle.progress(verb + " package cache", workunit++);
               updatePackageCache(connection, forceUpdate);
               handle.progress(verb + " sequence cache", workunit++);
               updateSequenceCache(connection, forceUpdate);
               handle.progress(verb + " view cache", workunit++);
               updateViewCache(connection, forceUpdate);
               handle.progress(verb + " table cache", workunit++);
               updateTableCache(connection, forceUpdate);
               handle.progress(verb + " logical unit cache", workunit++);
               updateLUCache(connection, forceUpdate);
               handle.progress(verb + " enumeration type cache", workunit++);
               updateEnumerationCache(connection, forceUpdate);
               handle.progress(verb + " synonym cache", workunit++);
               updateSynonymCache(connection, forceUpdate);
               handle.progress(verb + " module cache", workunit++);
               updateModuleCache(connection, forceUpdate);
               handle.progress(verb + " function cache", workunit++);
               updateFunctionCache(connection, forceUpdate);
               handle.progress(verb + " procedure cache", workunit++);
               updateProcedureCache(connection, forceUpdate);
               handle.progress("Done", workunit++);
            } catch (RuntimeException e) {
               //Q&D fix: if we're disconnecting from the database we will get an SQLException.
               //If so ignore it, otherwise throw the exception again
               if (!disconnecting) {
                  throw (e);
               }
            }
         }
      } finally {
         handle.finish();
      }
   }

   public String getCacheDir() {
      return directory;
   }

   public void clearCache() {
      lastPackageSyncTime = BEGINNING_OF_TIME;
      lastViewSyncTime = BEGINNING_OF_TIME;
      lastTableSyncTime = BEGINNING_OF_TIME;
      lastSequenceSyncTime = BEGINNING_OF_TIME;
      lastEnumerationSyncTime = BEGINNING_OF_TIME;
      lastLUSyncTime = BEGINNING_OF_TIME;
      lastSynonymSyncTime = BEGINNING_OF_TIME;
      removeCacheFile(tableCacheFile);
      removeCacheFile(viewCacheFile);
      removeCacheFile(packageCacheFile);
      removeCacheFile(sequenceCacheFile);
      removeCacheFile(ownerCacheFile);
      removeCacheFile(enumerationCacheFile);
      removeCacheFile(luCacheFile);
      removeCacheFile(moduleCacheFile);
      removeCacheFile(synonymCacheFile);
      tableNameMap = new HashMap<String, DatabaseObjectInfo>();
      viewNameMap = new HashMap<String, DatabaseObjectInfo>();
      pkgNameMap = new HashMap<String, DatabaseObjectInfo>();
      pkgInfoMap = new HashMap<String, Map<String, String>>();
      moduleVersionMap = new HashMap<String, String>();
      seqMap = new HashMap<String, DatabaseObjectInfo>();
      ownerMap = new HashMap<String, String>();
      logicalUnitSet = new HashSet<String>();
      enumerationSet = new HashSet<String>();
      viewSynonyms = new HashMap<String, String>();
      tableSynonyms = new HashMap<String, String>();
      packageSynonyms = new HashMap<String, String>();
   }

   private static boolean writeObjectToDisk(String dir, String file, Object obj) {
      FileOutputStream writer = null;
      ObjectOutputStream out = null;
      try {
         File test = new File(dir);
         if (!test.exists()) {
            test.mkdirs();
         }

         File f = new File(dir, file);
         if (!f.exists()) {
            f.createNewFile();
         }
         writer = new FileOutputStream(f);
         out = new ObjectOutputStream(writer);
         out.writeObject(obj);
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
         return false;
      } finally {
         try {
            if (out != null) {
               out.close();
            }
            if (writer != null) {
               writer.close();
            }
         } catch (IOException e) {
            Exceptions.printStackTrace(e);
         }
      }
      return true;
   }

   @SuppressWarnings("unchecked")
   private static Map<String, String> readMapFromFile(String dir, String file) {
      File f = new File(dir, file);
      if (f.exists()) {
         boolean deleteFile = false;
         FileInputStream reader = null;
         {
            ObjectInputStream in = null;
            try {
               reader = new FileInputStream(f);
               in = new ObjectInputStream(reader);
               Map map = (Map) in.readObject();
               return map;
            } catch (IOException ex) {
               Exceptions.printStackTrace(ex);
            } catch (ClassNotFoundException ex) {
               //file currupt, try to delete the file;
               deleteFile = true;
            } finally {
               try {
                  reader.close();
                  if (deleteFile) {
                     f.delete();
                  }
               } catch (IOException ex) {
                  Exceptions.printStackTrace(ex);
               }
               try {
                  in.close();
               } catch (IOException ex) {
                  Exceptions.printStackTrace(ex);
               }
            }
         }
      }
      return null;
   }

   private static String formatCacheFileName(String type, String name) {
      if (name != null) {
         return type + "_" + name + ".cache";
      }
      return type;
   }

   private void removeCacheFiles(String[] types, String name) {
      for (String type : types) {
         removeCacheFile(formatCacheFileName(type, name));
      }
   }

   private void removeCacheFile(String fileName) {
      File f = new File(getCacheDir(), fileName);
      if (f.exists()) {
         f.delete();
      }

   }

   /**
    * Get complete list of database tables
    * @return List of all tables in the cache
    */
   public Collection<String> getAllTables() {
      return getAllTables(null);
   }

   /**
    * Get complete list of database tables
    * @schema Optional schema name to return objects for
    * @return List of all tables in the cache
    */
   public Collection<String> getAllTables(String schema) {
      return getAllObjects(schema, tableNameMap, tableSynonyms);
   }

   /**
    * Get complete list of database packages
    * @return List of all packages in the cache
    */
   public Collection<String> getAllPackages() {
      return getAllPackages(null);
   }

   /**
    * Get complete list of database packages
    * @schema Optional schema name to return objects for
    * @return List of all packages in the cache
    */
   public Collection<String> getAllPackages(String schema) {
      return getAllObjects(schema, pkgNameMap, packageSynonyms);
   }

   /**
    * Get complete list of database views
    * @return List of all views in the cache
    */
   public Collection<String> getAllViews() {
      return getAllViews(null);
   }

   /**
    * Get complete list of database views
    * @schema Optional schema name to return objects for
    * @return List of all views in the cache
    */
   public Collection<String> getAllViews(String schema) {
      return getAllObjects(schema, viewNameMap, viewSynonyms);
   }

   /**
    * Get complete list of database sequences
    * @return List of all sequences in the cache
    */
   public Collection<String> getAllSequences() {
      return getAllSequences(null);
   }

   /**
    * Get complete list of database sequences
    * @schema Optional schema name to return objects for
    * @return List of all sequences in the cache
    */
   public Collection<String> getAllSequences(String schema) {
      return getAllObjects(schema, seqMap, null);
   }

   /**
    * Get complete list of database functions
    * @return List of all functions in the cache
    */
   public Collection<String> getAllFunctions() {
      return getAllFunctions(null);
   }

   /**
    * Get complete list of database functions
    * @schema Optional schema name to return objects for
    * @return List of all functions in the cache
    */
   public Collection<String> getAllFunctions(String schema) {
      return getAllObjects(schema, functionNameMap, null);
   }

   /**
    * Get complete list of database procedures
    * @return List of all procedures in the cache
    */
   public Collection<String> getAllProcedures() {
      return getAllProcedures(null);
   }

   /**
    * Get complete list of database functions
    * @schema Optional schema name to return objects for
    * @return List of all functions in the cache
    */
   public Collection<String> getAllProcedures(String schema) {
      return getAllObjects(schema, procedureNameMap, null);
   }

   private Collection<String> getAllObjects(String schema, Map<String, DatabaseObjectInfo> objectMap, Map<String, String> synonyms) {
      HashSet<String> result = new HashSet<String>();
      if (schema == null) {
         schema = user;
         if (synonyms != null) { //add synonyms if there's no owner prefix
            result.addAll(synonyms.keySet());
         }
      }
      if (!schema.startsWith("\"")) {
         schema = schema.toUpperCase(Locale.ENGLISH);
      } else if ("&AO".equals(schema)) { //IFS specific workaround for appowner prefix as used in client code
         schema = "IFSAPP";
      }

      for (String name : objectMap.keySet()) {
         DatabaseObjectInfo objectInfo = objectMap.get(name);
         if (objectInfo.getOwner().equals(schema)) {
            result.add(name);
         }
      }
      return result;
   }

   /**
    * Get list of database tables modified after lastFetchDate
    * @return List of newly updated tables
    */
   @SuppressWarnings("unchecked")
   private void updateTableCache(DatabaseConnection connection, boolean forceUpdate) {
      ObjectCacheUpdater cacheUpdater = new ObjectCacheUpdater() {

         @Override
         public void FetchObjectsFromDatabase(String lastSyncTime, List<String> objects, List<DatabaseObjectInfo> objectInfoList, DatabaseConnection connection) throws SQLException {
            DatabaseContentUtilities.getTableNames(lastSyncTime, objects, objectInfoList, connection.getJDBCConnection());
         }
      };
      lastTableSyncTime = cacheUpdater.updateObjectCache(tableCacheFile, tableNameMap, TABLES, lastTableSyncTime, connection, forceUpdate, new String[]{TABLES});
   }

   /**
    * Get list of enumeration LUs
    * @return List of enumerations
    */
   @SuppressWarnings("unchecked")
   private void updateSynonymCache(DatabaseConnection connection, boolean forceUpdate) {
      synchronized (synonymCacheFile) {
         if (viewSynonyms.isEmpty() && tableSynonyms.isEmpty() && packageSynonyms.isEmpty()) {
            Map cache = readMapFromFile(getCacheDir(), synonymCacheFile);
            if (cache != null) {
               lastSynonymSyncTime = (String) cache.get(LAST_FETCH_TIME);
               viewSynonyms = (HashMap) cache.get(VIEW_SYNONYMS);
               if (viewSynonyms == null) {
                  viewSynonyms = new HashMap<String, String>();
               }
               tableSynonyms = (HashMap) cache.get(TABLE_SYNONYMS);
               if (tableSynonyms == null) {
                  tableSynonyms = new HashMap<String, String>();
               }
               packageSynonyms = (HashMap) cache.get(PKG_SYNONYMS);
               if (packageSynonyms == null) {
                  packageSynonyms = new HashMap<String, String>();
               }
            }
         }
         if (lastSynonymSyncTime == null) {
            lastSynonymSyncTime = BEGINNING_OF_TIME;
         }
         if (connection != null && connection.getJDBCConnection() != null && (forceUpdate || BEGINNING_OF_TIME.equals(lastSynonymSyncTime))) {
            try {
               Map<String, String> result = DatabaseContentUtilities.getSynonyms(connection.getJDBCConnection(), lastSynonymSyncTime);
               for (Entry<String, String> entry : result.entrySet()) {
                  String synonym = entry.getKey().toLowerCase(Locale.ENGLISH);
                  String objectName = entry.getValue().toLowerCase(Locale.ENGLISH);
                  if (viewNameMap.containsKey(objectName)) {
                     viewSynonyms.put(synonym, objectName);
                  } else if (tableNameMap.containsKey(objectName)) {
                     tableSynonyms.put(synonym, objectName);
                  } else {
                     objectName = DatabaseContentUtilities.formatPlsqlName(entry.getValue());
                     if (pkgNameMap.containsKey(objectName)) {
                        packageSynonyms.put(DatabaseContentUtilities.formatPlsqlName(entry.getKey()), objectName);
                     }
                  }
               }

               String timestamp = DatabaseContentUtilities.getCurrentTime(connection.getJDBCConnection());
               Map<String, Object> cache = new HashMap<String, Object>();
               cache.put(LAST_FETCH_TIME, timestamp);
               cache.put(VIEW_SYNONYMS, viewSynonyms);
               cache.put(TABLE_SYNONYMS, tableSynonyms);
               cache.put(PKG_SYNONYMS, packageSynonyms);
               writeObjectToDisk(getCacheDir(), synonymCacheFile, cache);
               lastSynonymSyncTime = timestamp;
            } catch (SQLException ex) {
               //database exception => go offline (this should only happen when the database connection is lost
               //NB! This code will supress sql syntax errors
               fireExceptionThrown(ex);
            }
         }
      }
   }

   /**
    * Get list of enumeration LUs
    */
   @SuppressWarnings("unchecked")
   private void updateEnumerationCache(DatabaseConnection connection, boolean forceUpdate) {
      synchronized (enumerationCacheFile) {
         if (enumerationSet.isEmpty()) {
            Map cache = readMapFromFile(getCacheDir(), enumerationCacheFile);
            if (cache != null) {
               lastEnumerationSyncTime = (String) cache.get(LAST_FETCH_TIME);
               enumerationSet = (HashSet) cache.get(ENUMERATIONS);
               if (enumerationSet == null) {
                  enumerationSet = new HashSet<String>();
               }
            } else {
               lastEnumerationSyncTime = BEGINNING_OF_TIME;
            }
         }
         if (lastEnumerationSyncTime == null) {
            lastEnumerationSyncTime = BEGINNING_OF_TIME;         //update list with latest updates from database
         }
         if (connection != null && connection.getJDBCConnection() != null && (forceUpdate || BEGINNING_OF_TIME.equals(lastPackageSyncTime))) {
            try {
               ArrayList<String> enumerations = new ArrayList<String>();
               DatabaseContentUtilities.getEnumerationTypes(lastEnumerationSyncTime, enumerations, connection);
               for (int i = 0; i < enumerations.size(); i++) {
                  enumerationSet.add(enumerations.get(i));
               }
               if (enumerations.size() > 0) {
                  String timestamp = DatabaseContentUtilities.getCurrentTime(connection.getJDBCConnection());
                  Map<String, Object> cache = new HashMap<String, Object>();
                  cache.put(LAST_FETCH_TIME, timestamp);
                  cache.put(ENUMERATIONS, enumerationSet);
                  writeObjectToDisk(getCacheDir(), enumerationCacheFile, cache);
                  lastEnumerationSyncTime = timestamp;
               }
            } catch (SQLException ex) {
               //Ignore SQL Exceptions. This will happen if we use an older version of IFS Applications (before Apps7)
            }
         }
      }
   }

   /**
    * Get list of "regular" LUs
    */
   @SuppressWarnings("unchecked")
   private void updateLUCache(DatabaseConnection connection, boolean forceUpdate) {
      synchronized (luCacheFile) {
         if (logicalUnitSet.isEmpty()) {
            Map cache = readMapFromFile(getCacheDir(), luCacheFile);
            if (cache != null) {
               lastLUSyncTime = (String) cache.get(LAST_FETCH_TIME);
               logicalUnitSet = (HashSet) cache.get(LUS);
               if (logicalUnitSet == null) {
                  logicalUnitSet = new HashSet<String>();
               }
            } else {
               lastLUSyncTime = BEGINNING_OF_TIME;
            }
         }
         if (lastLUSyncTime == null) {
            lastLUSyncTime = BEGINNING_OF_TIME;
         }
         //update list with latest updates from database
         if (connection != null && connection.getJDBCConnection() != null && (forceUpdate || BEGINNING_OF_TIME.equals(lastPackageSyncTime))) {
            try {
               ArrayList<String> LUs = new ArrayList<String>();
               DatabaseContentUtilities.getLogicalUnits(lastLUSyncTime, LUs, connection);
               for (int i = 0; i < LUs.size(); i++) {
                  logicalUnitSet.add(LUs.get(i));
               }
               if (LUs.size() > 0) {
                  String timestamp = DatabaseContentUtilities.getCurrentTime(connection.getJDBCConnection());
                  HashMap<String, Object> cache = new HashMap<String, Object>();
                  cache.put(LAST_FETCH_TIME, timestamp);
                  cache.put(LUS, logicalUnitSet);
                  writeObjectToDisk(getCacheDir(), luCacheFile, cache);
                  lastLUSyncTime = timestamp;
               }
            } catch (SQLException ex) {
               //Ignore SQL Exceptions. This will happen if we use an older version of IFS Applications (before Apps7)
            }
         }
      }
   }

   /**
    * Get list of Modules installed in the database
    */
   @SuppressWarnings("unchecked")
   private synchronized void updateModuleCache(DatabaseConnection connection, boolean forceUpdate) {
      synchronized (moduleCacheFile) {
         Map<String, String> cachedData = readMapFromFile(getCacheDir(), moduleCacheFile);
         if (cachedData != null) {
            moduleVersionMap = cachedData;
         }
         if (connection != null && connection.getJDBCConnection() != null && (forceUpdate || cachedData == null)) {
            DatabaseContentUtilities.getModules(connection, moduleVersionMap);
            if (moduleVersionMap.size() > 0) {
               writeObjectToDisk(getCacheDir(), moduleCacheFile, moduleVersionMap);
            }
         }
      }
   }

   /**
    * Update the function cache with newly modified objects
    */
   @SuppressWarnings("unchecked")
   private void updateFunctionCache(DatabaseConnection connection, boolean forceUpdate) {
      ObjectCacheUpdater cacheUpdater = new ObjectCacheUpdater() {

         @Override
         public void FetchObjectsFromDatabase(String lastSyncTime, List<String> objects, List<DatabaseObjectInfo> objectInfoList, DatabaseConnection connection) throws SQLException {
            DatabaseContentUtilities.getFunctions(lastSyncTime, objects, objectInfoList, connection.getJDBCConnection());
         }
      };
      lastFunctionSyncTime = cacheUpdater.updateObjectCache(functionCacheFile, functionNameMap, FUNCTIONS, lastFunctionSyncTime, connection, forceUpdate, null);
   }

   /**
    * Update the procedure cache with newly modified objects
    */
   @SuppressWarnings("unchecked")
   private void updateProcedureCache(DatabaseConnection connection, boolean forceUpdate) {
      ObjectCacheUpdater cacheUpdater = new ObjectCacheUpdater() {

         @Override
         public void FetchObjectsFromDatabase(String lastSyncTime, List<String> objects, List<DatabaseObjectInfo> objectInfoList, DatabaseConnection connection) throws SQLException {
            DatabaseContentUtilities.getProcedures(lastSyncTime, objects, objectInfoList, connection.getJDBCConnection());
         }
      };
      lastProcedureSyncTime = cacheUpdater.updateObjectCache(procedureCacheFile, procedureNameMap, PROCEDURES, lastProcedureSyncTime, connection, forceUpdate, null);
   }

   /**
    * Update the view cache with newly modified objects
    */
   @SuppressWarnings("unchecked")
   private void updateViewCache(DatabaseConnection connection, boolean forceUpdate) {
      ObjectCacheUpdater cacheUpdater = new ObjectCacheUpdater() {

         @Override
         public void FetchObjectsFromDatabase(String lastSyncTime, List<String> objects, List<DatabaseObjectInfo> objectInfoList, DatabaseConnection connection) throws SQLException {
            DatabaseContentUtilities.getViewNames(lastSyncTime, objects, objectInfoList, connection.getJDBCConnection());
         }
      };
      lastViewSyncTime = cacheUpdater.updateObjectCache(viewCacheFile, viewNameMap, VIEWS, lastViewSyncTime, connection, forceUpdate, new String[]{VIEWS});
   }

   /**
    * Get only the columns of the given table
    * @param String name of view/table
    * @return Map
    */
   public Map<String, String> getColumnObjects(String view, DatabaseConnection databaseConnection) {
      if (!view.startsWith("\"")) {
         view = view.toLowerCase(Locale.ENGLISH);
      }
      if (isUpdateRunning()) {
         if (databaseConnection == null || databaseConnection.getJDBCConnection() == null) {
            return new HashMap<String, String>();
         }
         try {
            return DatabaseContentUtilities.getColumnNames(view, databaseConnection.getSchema(), databaseConnection.getJDBCConnection());
         } catch (SQLException ex) {
            fireExceptionThrown(ex);
            return new HashMap<String, String>();
         }
      }
      synchronized (tableCacheFile) {
         boolean isTable = isTable(view);
         Map<String, DatabaseObjectInfo> map = isTable ? tableNameMap : viewNameMap;
         if (isTable) {
            if (tableSynonyms.containsKey(view)) {
               view = tableSynonyms.get(view);
            }
         } else {
            if (viewSynonyms.containsKey(view)) {
               view = viewSynonyms.get(view);
            }
         }


         DatabaseObjectInfo objectInfo = map.get(view);
         if (objectInfo == null) //not a view/table
         {
            return new HashMap<String, String>();
         }
         Map<String, String> columns = objectInfo.getObjectData();
         if (columns == null || columns.isEmpty()) { //first look for the cache file
            columns = readMapFromFile(getCacheDir(), formatCacheFileName(isTable ? TABLES : VIEWS, view));
            try {
               if (databaseConnection != null && databaseConnection.getJDBCConnection() != null) {
                  if (columns == null || columns.isEmpty()) {
                     String owner = getOwner(view);
                     columns = DatabaseContentUtilities.getColumnNames(view, owner, databaseConnection.getJDBCConnection());
                     objectInfo.setObjectData(columns);
                     writeObjectToDisk(getCacheDir(), formatCacheFileName(isTable ? TABLES : VIEWS, view), columns);
                  }
               }
            } catch (SQLException ex) {
               //database exception => go offline (this should only happen when the database connection is lost
               //NB! This code will supress sql syntax errors
               fireExceptionThrown(ex);
            }
            objectInfo.setObjectData(columns);
         }

         return columns != null ? columns : new HashMap<String, String>();
      }
   }

   /**
    * Update the package cache
    * @return List of new/modified packages
    */
   @SuppressWarnings("unchecked")
   private void updatePackageCache(DatabaseConnection connection, boolean forceUpdate) {
      ObjectCacheUpdater cacheUpdater = new ObjectCacheUpdater() {

         @Override
         public void FetchObjectsFromDatabase(String lastSyncTime, List<String> objects, List<DatabaseObjectInfo> objectInfoList, DatabaseConnection connection) throws SQLException {
            DatabaseContentUtilities.getPackageNames(lastSyncTime, objects, objectInfoList, connection.getJDBCConnection());
         }
      };
      lastPackageSyncTime = cacheUpdater.updateObjectCache(packageCacheFile, pkgNameMap, PACKAGES, lastPackageSyncTime, connection, forceUpdate, new String[]{PACKAGES, PACKAGE_INFO});
   }

   private void writeCacheFileToDisk(String timestamp, String mapName, Map objectMap, String cacheFile) {
      Map<String, Object> cache = new HashMap<String, Object>();
      cache.put(LAST_FETCH_TIME, timestamp);
      cache.put(mapName, objectMap);
      writeObjectToDisk(getCacheDir(), cacheFile, cache);
   }

   /**
    * Get only the functions/procedures belonging to the given owner, of the given package
    * @param pkgName
    * @return Map of methods
    */
   public Map<String, String> getMethodObjects(String pkgName, DatabaseConnection databaseConnection) {
      if (isUpdateRunning()) {
         Connection con = databaseConnection.getJDBCConnection();
         if (databaseConnection == null || databaseConnection.getJDBCConnection() == null) {
            return new HashMap<String, String>();
         }
         try {
            return DatabaseContentUtilities.getMethodNames(pkgName, databaseConnection.getSchema(), con);
         } catch (SQLException ex) {
            fireExceptionThrown(ex);
            return new HashMap<String, String>();
         }
      }
      synchronized (packageCacheFile) {
         if (!pkgName.startsWith("\"")) {
            pkgName = DatabaseContentUtilities.formatPlsqlName(pkgName);
         }
         if (packageSynonyms.containsKey(pkgName)) {
            pkgName = packageSynonyms.get(pkgName);
         }
         DatabaseObjectInfo objectInfo = pkgNameMap.get(pkgName);
         if (objectInfo == null) //not a view/table
         {
            return new HashMap<String, String>();
         }
         Map<String, String> methods = objectInfo.getObjectData();
         if (methods == null || methods.isEmpty()) { //first look for the cache file
            methods = readMapFromFile(getCacheDir(), formatCacheFileName(PACKAGES, pkgName));
            try {
               if (databaseConnection != null && databaseConnection.getJDBCConnection() != null) {
                  if (methods == null || methods.isEmpty()) { //if no cache file - fetch info from database
                     String owner = getOwner(pkgName);
                     methods = DatabaseContentUtilities.getMethodNames(pkgName, owner, databaseConnection.getJDBCConnection());
                     writeObjectToDisk(getCacheDir(), formatCacheFileName(PACKAGES, pkgName), methods);
                  }
               }
            } catch (SQLException ex) {
               //database exception => go offline (this should only happen when the database connection is lost
               //NB! This code will supress sql syntax errors
               fireExceptionThrown(ex);
            }
            objectInfo.setObjectData(methods);
         }
         return methods != null ? methods : new HashMap<String, String>();
      }
   }

   /**
    * Get exceptions, types and constants in a given package
    * @param pkgName
    * @return Map of objects
    */
   public Map<String, String> getPackageMembers(String pkgName, DatabaseConnection databaseConnection) {
      if (isUpdateRunning()) {
         Connection con = databaseConnection.getJDBCConnection();
         if (databaseConnection == null || databaseConnection.getJDBCConnection() == null) {
            return new HashMap<String, String>();
         }
         try {
            return DatabaseContentUtilities.getPackageMembers(pkgName, databaseConnection.getSchema(), con);
         } catch (SQLException ex) {
            fireExceptionThrown(ex);
            return new HashMap<String, String>();
         }
      }
      synchronized (packageCacheFile) {
         if (!pkgName.startsWith("\"")) {
            pkgName = DatabaseContentUtilities.formatPlsqlName(pkgName);
         }
         if (packageSynonyms.containsKey(pkgName)) {
            pkgName = packageSynonyms.get(pkgName);
         }
         Map<String, String> data = pkgInfoMap.get(pkgName);
         if (data == null || data.isEmpty()) { //first look for the cache file
            data = readMapFromFile(getCacheDir(), formatCacheFileName(PACKAGE_INFO, pkgName));
            try {
               if (data == null || data.isEmpty()) { //if no cache file - fetch info from database
                  if (databaseConnection != null && databaseConnection.getJDBCConnection() != null) {
                     String owner = getOwner(pkgName);
                     data = DatabaseContentUtilities.getPackageMembers(pkgName, owner, databaseConnection.getJDBCConnection());
                     writeObjectToDisk(getCacheDir(), formatCacheFileName(PACKAGE_INFO, pkgName), data);
                  }
               }
            } catch (SQLException ex) {
               //database exception => go offline (this should only happen when the database connection is lost
               //NB! This code will supress sql syntax errors
               fireExceptionThrown(ex);
            }
            pkgInfoMap.put(pkgName, data);
         }
         return data != null ? data : new HashMap<String, String>();
      }
   }

   /**
    * Update cache of database sequences
    * @return List of newly updated sequences
    */
   @SuppressWarnings("unchecked")
   private void updateSequenceCache(DatabaseConnection connection, boolean forceUpdate) {
      ObjectCacheUpdater cacheUpdater = new ObjectCacheUpdater() {

         @Override
         public void FetchObjectsFromDatabase(String lastSyncTime, List<String> objects, List<DatabaseObjectInfo> objectInfoList, DatabaseConnection connection) throws SQLException {
            DatabaseContentUtilities.getSequences(lastSyncTime, objects, objectInfoList, connection.getJDBCConnection());
         }
      };
      lastSequenceSyncTime = cacheUpdater.updateObjectCache(sequenceCacheFile, seqMap, SEQUENCES, lastSequenceSyncTime, connection, forceUpdate, null);
   }

   public void refreshItem(String objectName, DatabaseConnection currentConnection) {
      Map<String, DatabaseObjectInfo> map = null;
      String[] types = null;
      String objectNameInCache = objectName.startsWith("\"") ? objectName : objectName.toLowerCase();
      String camelCaseNameInCache = objectName.startsWith("\"") ? objectName : DatabaseContentUtilities.formatPlsqlName(objectName);
      if (tableNameMap.containsKey(objectNameInCache)) {
         map = tableNameMap;
         types = new String[]{TABLES};
      } else if (viewNameMap.containsKey(objectNameInCache)) {
         map = viewNameMap;
         types = new String[]{VIEWS};
      } else if (pkgNameMap.containsKey(camelCaseNameInCache)) {
         objectName = camelCaseNameInCache;
         map = pkgNameMap;
         if (pkgInfoMap.containsKey(camelCaseNameInCache)) {
            pkgInfoMap.remove(camelCaseNameInCache);
         }
         types = new String[]{PACKAGES, PACKAGE_INFO};
      } else if (tableSynonyms.containsKey(objectNameInCache)) {
         objectName = tableSynonyms.get(objectName);
         map = tableNameMap;
         types = new String[]{TABLES};
      } else if (viewSynonyms.containsKey(objectNameInCache)) {
         objectName = viewSynonyms.get(objectName);
         map = viewNameMap;
         types = new String[]{VIEWS};
      } else if (functionNameMap.containsKey(camelCaseNameInCache)) {
         objectName = camelCaseNameInCache;
         map = functionNameMap;
         types = new String[]{FUNCTIONS};
      } else if (procedureNameMap.containsKey(camelCaseNameInCache)) {
         objectName = camelCaseNameInCache;
         map = procedureNameMap;
         types = new String[]{PROCEDURES};
      }
      if (map != null) {
         removeCacheFiles(types, objectName);
         DatabaseObjectInfo objectInfo = map.get(objectName);
         objectInfo.setObjectData(null);
      } else { //check to see if this is a newly created object (and if so add it to the cache)
         if (objectName == null || objectName.length() == 0) {
            //full update - run the cache update
            synchronousCacheUpdate(currentConnection, true);
         } else {
            boolean packageFound = false;
            boolean viewFound = false;
            boolean tableFound = false;
            boolean functionFound = false;
            boolean procedureFound = false;
            boolean synonymFound = false;
            Map<String, DatabaseObjectInfo> databaseObjects = DatabaseContentUtilities.getObjectInfo(objectName, currentConnection, lastPackageSyncTime, lastViewSyncTime, lastTableSyncTime, lastSynonymSyncTime);
            for (String actualName : databaseObjects.keySet()) {
               DatabaseObjectInfo objectInfo = databaseObjects.get(actualName);
               if (objectInfo != null && objectInfo.getObjectType() != null) {
                  String objectType = objectInfo.getObjectType();
                  if (DatabaseContentUtilities.isValidOracleUppercaseIdentifier(actualName)) {
                     objectNameInCache = actualName.toLowerCase(Locale.ENGLISH);
                  } else {
                     objectNameInCache = "\"" + actualName + "\"";
                  }
                  addSchema(objectInfo.getOwner());
                  if (objectType.startsWith("PACKAGE")) {
                     objectName = objectNameInCache.startsWith("\"") ? objectNameInCache : DatabaseContentUtilities.formatPlsqlName(actualName);
                     if (!isPackage(objectName, currentConnection)) {
                        pkgNameMap.put(objectName, objectInfo);
                        ownerMap.put(objectNameInCache, objectInfo.getOwner());
                        packageFound = true;
                     }
                  } else if (objectType.equals("VIEW")) {
                     if (!isView(objectNameInCache)) {
                        viewNameMap.put(objectNameInCache, objectInfo);
                        ownerMap.put(objectNameInCache, objectInfo.getOwner());
                        viewFound = true;
                     }
                  } else if (objectType.equals("TABLE")) {
                     if (!isTable(objectNameInCache)) {
                        tableFound = true;
                        tableNameMap.put(objectNameInCache, objectInfo);
                        ownerMap.put(objectNameInCache, objectInfo.getOwner());
                     }
                  } else if (objectType.equals("FUNCTION")) {
                     objectName = objectNameInCache.startsWith("\"") ? objectNameInCache : DatabaseContentUtilities.formatPlsqlName(actualName);
                     if (!isFunction(objectName)) {
                        functionFound = true;
                        functionNameMap.put(objectName, objectInfo);
                        ownerMap.put(objectNameInCache, objectInfo.getOwner());
                     }
                  } else if (objectType.equals("PROCEDURE")) {
                     objectName = objectNameInCache.startsWith("\"") ? objectNameInCache : DatabaseContentUtilities.formatPlsqlName(actualName);
                     if (!isProcedure(objectName)) {
                        procedureFound = true;
                        procedureNameMap.put(objectName, objectInfo);
                        ownerMap.put(objectNameInCache, objectInfo.getOwner());
                     }
                  } else if (objectType.equals("SYNONYM")) {
                     synonymFound = true;
                     //for now just refresh all synonyms if this happens
                  }
               }
            }
            if (packageFound) {
               writeCacheFileToDisk(lastPackageSyncTime, PACKAGES, pkgNameMap, packageCacheFile);
            }
            if (tableFound) {
               writeCacheFileToDisk(lastTableSyncTime, TABLES, tableNameMap, tableCacheFile);
            }
            if (viewFound) {
               writeCacheFileToDisk(lastViewSyncTime, VIEWS, viewNameMap, viewCacheFile);
            }
            if (functionFound) {
               writeCacheFileToDisk(lastFunctionSyncTime, FUNCTIONS, functionNameMap, functionCacheFile);
            }
            if (procedureFound) {
               writeCacheFileToDisk(lastProcedureSyncTime, PROCEDURES, procedureNameMap, procedureCacheFile);
            }
            if (packageFound || tableFound || viewFound || procedureFound || functionFound) {
               writeObjectToDisk(getCacheDir(), ownerCacheFile, ownerMap);
            }
            if (synonymFound) {
               updateSynonymCache(currentConnection, true);
            }
         }
      }
   }

   public boolean isTable(String objectName) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = objectName.toLowerCase(Locale.ENGLISH);
      }
      return tableNameMap.containsKey(objectName) || tableSynonyms.containsKey(objectName);
   }

   public boolean isView(String objectName) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = objectName.toLowerCase(Locale.ENGLISH);
      }
      return viewNameMap.containsKey(objectName) || viewSynonyms.containsKey(objectName);
   }

   public boolean isViewSynonym(String objectName) {
      if (objectName == null) {
         return false;
      }
      return viewSynonyms.containsKey(objectName.toLowerCase(Locale.ENGLISH));
   }

   public String getViewForSynonym(String objectName) {
      if (objectName == null) {
         return null;
      }
      if (!objectName.startsWith("\"")) {
         objectName = objectName.toLowerCase(Locale.ENGLISH);
      }
      return viewSynonyms.get(objectName);
   }

   public boolean isTableSynonym(String objectName) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = objectName.toLowerCase(Locale.ENGLISH);
      }
      return tableSynonyms.containsKey(objectName);
   }

   public String getTableForSynonym(String objectName) {
      if (objectName == null) {
         return null;
      }
      if (!objectName.startsWith("\"")) {
         objectName = objectName.toLowerCase(Locale.ENGLISH);
      }
      return tableSynonyms.get(objectName);
   }

   public boolean isPackageSynonym(String objectName) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = DatabaseContentUtilities.formatPlsqlName(objectName);
      }
      return packageSynonyms.containsKey(objectName);
   }

   public String getPackageForSynonym(String objectName) {
      if (objectName == null) {
         return null;
      }
      if (!objectName.startsWith("\"")) {
         objectName = DatabaseContentUtilities.formatPlsqlName(objectName);
      }
      return packageSynonyms.get(objectName);
   }

   public boolean isPackage(String objectName, DatabaseConnection currentConnection) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = DatabaseContentUtilities.formatPlsqlName(objectName);
      }
      return pkgNameMap.containsKey(objectName) || packageSynonyms.containsKey(objectName);
   }

   public boolean isLogicalUnit(String objectName) {
      if (objectName == null) {
         return false;
      }
      return logicalUnitSet != null && logicalUnitSet.contains(objectName);
   }

   public boolean isEnumerationType(String objectName) {
      if (objectName == null) {
         return false;
      }
      return enumerationSet != null && enumerationSet.contains(objectName);
   }

   public boolean isSequence(String objectName) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = objectName.toLowerCase(Locale.ENGLISH);
      }
      return this.seqMap.containsKey(objectName);
   }

   public boolean isFunction(String objectName) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = DatabaseContentUtilities.formatPlsqlName(objectName);
      }
      return this.functionNameMap.containsKey(objectName);
   }

   public boolean isProcedure(String objectName) {
      if (objectName == null) {
         return false;
      }
      if (!objectName.startsWith("\"")) {
         objectName = DatabaseContentUtilities.formatPlsqlName(objectName);
      }
      return this.procedureNameMap.containsKey(objectName);
   }

   public boolean isSchema(String objectName) {
      if (objectName == null) {
         return false;
      }
      //IFS specific workaround for appowner prefix as used in client code
      if ("&AO".equals(objectName)) {
         objectName = "IFSAPP";
      }
      if (!objectName.startsWith("\"")) {
         objectName = objectName.toUpperCase(Locale.ENGLISH);
      }
      return schemaSet.contains(objectName);
   }

   public Set<String> listLogicalUnits() {
      return logicalUnitSet;
   }

   public Set<String> listEnumerationTypes() {
      return enumerationSet;
   }

   public DatabaseObjectInfo getViewInfo(String viewName, DatabaseConnection databaseConnection) {
      if (!updateThread.isFinished()) {
         return DatabaseContentUtilities.getViewInfo(viewName, databaseConnection);
      }
      return viewNameMap.get(viewName.toLowerCase(Locale.ENGLISH));
   }

   public DatabaseObjectInfo getPackageInfo(String pkgName, DatabaseConnection databaseConnection) {
      DatabaseObjectInfo info = pkgNameMap.get(DatabaseContentUtilities.formatPlsqlName(pkgName));
      if ((info == null || info.getLuName() == null) && databaseConnection != null) { //data not cached - fetched from the database
         if (info == null) {
            info = new DatabaseObjectInfo(databaseConnection.getSchema());
         }
         DatabaseContentUtilities.getMetaDataForPackage(databaseConnection.getJDBCConnection(), pkgName, info);
      }
      return info;
   }

   public String getModuleVersion(String moduleName) {
      return moduleVersionMap.get(moduleName.toLowerCase(Locale.ENGLISH));
   }

   public String getOwner(String objectName) {
      String owner = ownerMap.get(objectName.toLowerCase(Locale.ENGLISH));
      return (owner == null ? "" : owner);
   }

   public String getLastPackageSyncTime() {
      return lastPackageSyncTime;
   }

   public String getLastTableSyncTime() {
      return lastTableSyncTime;
   }

   public String getLastViewSyncTime() {
      return lastViewSyncTime;
   }

   public boolean isUpdateRunning() {
      return (updateThread != null && !updateThread.isFinished());
   }

   public void addExceptionListener(ExceptionListener listener) {
      listeners.add(listener);
   }

   public void removeExceptionListener(ExceptionListener listener) {
      listeners.remove(listener);
   }

   private void fireExceptionThrown(Exception e) {
      for (ExceptionListener listener : listeners) {
         listener.exceptionThrown(e);
      }
   }

   public String getFndbasVersion() {
      return getModuleVersion("fndbas");
   }

   private abstract class ObjectCacheUpdater {

      public String updateObjectCache(String fileName, Map objectMap, String cacheEntryName, String lastSyncTime, DatabaseConnection connection, boolean forceUpdate, String[] detailFileTypes) {
         synchronized (fileName) {
            if (objectMap.isEmpty()) {
               Map cache = readMapFromFile(getCacheDir(), fileName);
               if (cache != null) {
                  lastSyncTime = (String) cache.get(LAST_FETCH_TIME);
                  Map tempMap = (Map) cache.get(cacheEntryName);
                  if (tempMap != null) {
                     objectMap.putAll(tempMap);
                  }
               } else {
                  lastSyncTime = BEGINNING_OF_TIME;
               }
            }
            if (lastSyncTime == null) {
               lastSyncTime = BEGINNING_OF_TIME; //update list with latest updates from database
            }
            if (connection != null && connection.getJDBCConnection() != null && (forceUpdate || BEGINNING_OF_TIME.equals(lastSyncTime))) {
               try {
                  List<String> objects = new ArrayList<String>();
                  List<DatabaseObjectInfo> objectInfoList = new ArrayList<DatabaseObjectInfo>();
                  FetchObjectsFromDatabase(lastSyncTime, objects, objectInfoList, connection);
                  for (int i = 0; i < objects.size(); i++) {
                     String name = objects.get(i);
                     if (detailFileTypes != null) {
                        removeCacheFiles(detailFileTypes, name);
                     }
                     DatabaseObjectInfo info = objectInfoList.isEmpty() ? null : objectInfoList.get(i);
                     if (info != null) {
                        ownerMap.put(name.toLowerCase(Locale.ENGLISH), info.getOwner());
                     }
                     objectMap.put(name, info);
                  }
                  if (objects.size() > 0) {
                     String timestamp = DatabaseContentUtilities.getCurrentTime(connection.getJDBCConnection());
                     writeCacheFileToDisk(timestamp, cacheEntryName, objectMap, fileName);
                     if (!objectInfoList.isEmpty()) {
                        writeObjectToDisk(getCacheDir(), ownerCacheFile, ownerMap);
                     }
                     lastSyncTime = timestamp;
                  }
               } catch (SQLException ex) {
                  //database exception => go offline (this should only happen when the database connection is lost
                  //NB! This code will supress sql syntax errors
                  fireExceptionThrown(ex);
               }
            }
         }
         return lastSyncTime;
      }

      public abstract void FetchObjectsFromDatabase(String lastSyncTime, List<String> objects, List<DatabaseObjectInfo> objectInfoList, DatabaseConnection connection) throws SQLException;
   }
}
