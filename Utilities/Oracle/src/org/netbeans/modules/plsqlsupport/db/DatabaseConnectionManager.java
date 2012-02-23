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

import org.netbeans.modules.plsqlsupport.db.ui.DatabaseConnectionPanel;
import java.beans.ExceptionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.ConnectionManager;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.db.explorer.DatabaseException;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

public class DatabaseConnectionManager {

    public static final String ORACLE_DRIVER_CLASS_NAME = "oracle.jdbc.OracleDriver";
    public static final String PROP_DATABASE_CONNECTIONS = "databaseConnections";
    public static final String PROP_CW_DATABASE_CONNECTIONS = "commandWindowDatabaseConnections";
    public static final String PROP_ONLINE = "online";
    private static Map<FileObject, DatabaseConnectionManager> instances = new HashMap<FileObject, DatabaseConnectionManager>();
    private DatabaseConnection[] connections = new DatabaseConnection[]{};
    private final Stack<DatabaseConnection> connectionPool = new Stack<DatabaseConnection>();
    private DatabaseConnection templateConnection;
    private Connection debugConnection = null;
    private String debugConnectionInfo = null;
    private boolean online = true;
    private boolean usagesEnabled = false;
    private ExceptionListener connectionErrorListener = new ConnectionErrorListener();
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private static FileObject tempFolder = null;
    private static final Logger logger = Logger.getLogger(DatabaseConnectionManager.class.getName());
    private static final RequestProcessor RP = new RequestProcessor(DatabaseConnectionManager.class);

    static {
        String tempDir = System.getProperty("java.io.tmpdir");
        if (tempDir != null) {
            tempFolder = FileUtil.toFileObject(FileUtil.normalizeFile(new File(tempDir)));
        }
    }

    public DatabaseConnectionManager() {
    }

    public static DatabaseConnectionManager getInstance(Project project) {
        if (project == null) {
            return null;
        }
        return project.getLookup().lookup(DatabaseConnectionManager.class);
    }

    public static DatabaseConnectionManager getInstance(Document document) {
        Object origin = document.getProperty(Document.StreamDescriptionProperty);
        return origin instanceof DataObject ? getInstance((DataObject) origin) : null;
    }

    public static DatabaseConnectionManager getInstance(DataObject dataObject) {
        FileObject fileObject = dataObject.getPrimaryFile();
        Project project = FileOwnerQuery.getOwner(fileObject);
        if (project != null) {
            DatabaseConnectionManager provider = project.getLookup().lookup(DatabaseConnectionManager.class);
            if (provider != null) {
                return provider;
            }
        }

        if (instances.containsKey(fileObject)) {
            return instances.get(fileObject);
        }

        //workaround for temp files created by NB 'Local History' functionality, these files when loaded
        //will prompt for DB connection since they dont belong to the project.
        if (FileUtil.isParentOf(tempFolder, fileObject)) {
            return null;
        }

        DatabaseConnectionManager provider = new DatabaseConnectionManager();
        if (provider.getDatabaseConnection(true) == null) { // prompt connection dialog for files outside the project structure
            return null;
        }
        instances.put(fileObject, provider);
        return provider;
    }

    public static void copyProvider(Document source, DataObject target) {
        Object origin = source.getProperty(Document.StreamDescriptionProperty);
        if (origin instanceof DataObject) {
            copyProvider((DataObject) origin, target);
        }
    }

    public static void copyProvider(DataObject source, DataObject target) {
        FileObject fileObject = source.getPrimaryFile();
        DatabaseConnectionManager provider = instances.containsKey(fileObject) ? instances.get(fileObject) : new DatabaseConnectionManager();
        if (provider.getTemplateConnection() != null) {
            instances.put(target.getPrimaryFile(), provider);
        }
    }

    public boolean isConnected() {
        return templateConnection != null && templateConnection.getJDBCConnection() != null;
    }

    public DatabaseConnection[] getDatabaseConnections() {
        return connections;
    }

    /**
     *
     * @return The database URL of the primary database connection, empty String
     * if none is found.
     */
    public String getPrimaryConnectionURL() {
        String result = "";
        if (connections.length > 0) {
            result = connections[0].getDatabaseURL();
        }
        return result;
    }

    public boolean isDefaultDatabase(DatabaseConnection connection) {
       return databaseConnectionsAreEqual(connection, templateConnection);
    }
    
    private boolean databaseConnectionsAreEqual(DatabaseConnection a, DatabaseConnection b) {
        return (a != null && b != null
                && a.getDatabaseURL().equals(b.getDatabaseURL())
                && a.getUser().equals(b.getUser()));
    }

    public void releaseDatabaseConnection(DatabaseConnection connection) {
        if (connection == null) {
            return;
        }
        if (connection == templateConnection) { //make sure we don't pool the templateConnection
            try {
                if (connection.getJDBCConnection() != null && connection.getJDBCConnection().isClosed()) {
                    setOnline(false);
                }
            } catch (SQLException ex) {
                Exceptions.printStackTrace(ex);
            }
            return;
        }
        Connection jdbcConnection = connection.getJDBCConnection();
        //verify the connection. If closed or invalid then discard.
        try {
            if (jdbcConnection == null || jdbcConnection.isClosed()) {
                logger.log(Level.FINEST, "Discarding closed connection {0}", connection.hashCode());
                return;
            }
        } catch (SQLException ex) {
            return;
        }

        synchronized (connectionPool) {
            //if main database is still the same add the connection to the connection pool. Otherwise close and discard.
            if (databaseConnectionsAreEqual(connection, this.templateConnection)) {
               if(!connectionPool.contains(connection)) {
                  connectionPool.push(connection);
                  logger.log(Level.FINEST, "Returning connection {0} to cache. Number of pooled connections={1}", new Object[]{connection.hashCode(), connectionPool.size()});
               }
            } else {
                if (connection != null) {
                    logger.log(Level.FINEST, "Returning connection {0} to cache. Discard because connected to different database", connection.hashCode());
                } else {
                    logger.finest("Returning connection NULL to cache. Discard!");
                }
            }
        }
    }

    public void commitRollbackTransactions(DatabaseConnection connection, boolean commit) {
        try {
            if (connection.getJDBCConnection() == null) {
                return;
            }
            Connection con = connection.getJDBCConnection();

            if (commit) {
                con.commit();
            } else {
                con.rollback();
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

   /**
     *
     * @return True if there in on going transactions for a command window.
     */
   public boolean hasDataToCommit(DatabaseConnection connection) {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        String commitData = null;
        if (connection == null || connection.getJDBCConnection() == null) {
            return false;
        }
       
        try {
            String sqlSelect = " SELECT taddr FROM   v$session WHERE  AUDsid=userenv('SESSIONID')";
            stmt = connection.getJDBCConnection().prepareStatement(sqlSelect);
            rs = stmt.executeQuery();
            if (rs.next()) {
                commitData = rs.getString(1);
            }
            if (commitData != null) {
                return true;
            }else{
                return false;
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
            return false;
   }

    private void clearConnectionPool() {
        synchronized (connectionPool) {
            while (!connectionPool.isEmpty()) {
                DatabaseConnection connection = connectionPool.pop();
                if (testConnection(connection)) {
                    ConnectionManager.getDefault().disconnect(connection);
                }
            }
        }
    }
    static int connectionCount = 0;

    private DatabaseConnection getDatabaseConnectionFromPool() {
        logger.log(Level.FINEST, "Getting connection from the pool. Number of pooled connections={0}", connectionPool.size());
        synchronized (connectionPool) {
            DatabaseConnection connection = null;
            while (!connectionPool.isEmpty()) {
                connection = connectionPool.pop();
                if (testConnection(connection)) {
                    return connection;
                }
            }
            logger.log(Level.FINEST, "Creating new connection. Total number of connections created={0}", ++connectionCount);
            return DatabaseConnection.create(templateConnection.getJDBCDriver(), templateConnection.getDatabaseURL(), templateConnection.getUser(), templateConnection.getSchema(), templateConnection.getPassword(), true, templateConnection.getDisplayName());
        }
    }

    public DatabaseConnection getTemplateConnection() {
        if (templateConnection != null) {
            return templateConnection;
        }
        return getDatabaseConnection(true, false, false);
    }

    public DatabaseConnection getDatabaseConnection(boolean prompt) {
        return getDatabaseConnection(prompt, false, false);
    }

    public DatabaseConnection getDatabaseConnection(boolean prompt, boolean force) {
        return getDatabaseConnection(prompt, force, false);
    }

    public DatabaseConnection getPooledDatabaseConnection(boolean prompt) {
        return getDatabaseConnection(prompt, false, true);
    }

    public DatabaseConnection getPooledDatabaseConnection(boolean prompt, boolean force) {
        return getDatabaseConnection(prompt, force, true);
    }

    private synchronized DatabaseConnection getDatabaseConnection(boolean prompt, boolean force, final boolean useConnectionPool) {
        if (!online && !force) {
            return null;
        }
        if (prompt) {
            if (templateConnection == null) {
                templateConnection = new DatabaseConnectionPanel().showDialog();
                connections = templateConnection != null ? new DatabaseConnection[]{templateConnection} : new DatabaseConnection[]{};
                if (templateConnection != null) {
                    usagesEnabled = isFindUsagesEnabled();
                    DatabaseContentManager contentManager = DatabaseContentManager.getInstance(templateConnection);
                    contentManager.addExceptionListener(connectionErrorListener);
                    contentManager.updateCache(this);
                }
            }
        }

        if (!online) {
            connect(templateConnection);
        }
        if (!online) {
            return null;
        }
        DatabaseConnection connection = templateConnection;
        if (useConnectionPool) {
            connection = getDatabaseConnectionFromPool();
            if(connection!=null && connection.getJDBCConnection()==null) {
               connect(connection);
            }
        }

        return online ? connection : null;
    }

    private String formatDebugInfoString(String url, String user, String password) {
        return (user + "/" + password + "@" + url);
    }

    public String formatDatabaseConnectionInfo() {
        if (templateConnection != null) {
            String url = templateConnection.getDatabaseURL();
            String schema = templateConnection.getUser();
            int pos = url.indexOf("@") + 1;
            if (pos > 0) {
                url = url.substring(pos);
            }
            url = schema + "@" + url;
            String alias = templateConnection.getDisplayName();
            if (alias != null && !alias.equals(templateConnection.getName())) {
                url = alias + " (" + url + ")";
            }
            return url;
        }
        return "Not connected to database";
    }

    public Connection getDebugConnection() {
        try {
            DatabaseConnection databaseConnection = getTemplateConnection();
            if (databaseConnection == null) {
                return null;
            }
            String url = databaseConnection.getDatabaseURL();
            String user = databaseConnection.getUser();
            String password = databaseConnection.getPassword();
            if (debugConnection != null) {
                try {
                    if (debugConnection.isClosed() || !debugConnection.isValid(200)) {
                        debugConnection = null;
                    }
                } catch (SQLException ex) {
                    debugConnection = null;
                }
            }
            if (debugConnection == null || !(formatDebugInfoString(url, user, password).equalsIgnoreCase(debugConnectionInfo))) {
                if (debugConnection != null) {
                    debugConnection.close();
                }
                Driver driver = databaseConnection.getJDBCDriver().getDriver();
                Properties properties = new Properties();
                properties.put("user", user);
                properties.put("password", password);
                debugConnection = driver.connect(url, properties);
                debugConnectionInfo = formatDebugInfoString(url, user, password);
            }
        } catch (DatabaseException ex) {
            debugConnection = null;
            Exceptions.printStackTrace(ex);
        } catch (SQLException ex) {
            debugConnection = null;
            Exceptions.printStackTrace(ex);
        }
        return debugConnection;
    }

    public void setDatabaseConnections(DatabaseConnection[] connections) {
        DatabaseConnection[] oldConnections = this.connections;
        this.connections = connections;
        if (templateConnection != null) {
            DatabaseContentManager.getInstance(templateConnection).removeExceptionListener(connectionErrorListener);
        }
        templateConnection = connections.length > 0 ? connections[0] : null;
        if (templateConnection != null) {
            DatabaseContentManager.getInstance(templateConnection).addExceptionListener(connectionErrorListener);
        }

        //clear the connection pool and the debug connection when the main database changes
        clearConnectionPool();
        this.debugConnection = null;
        changeSupport.firePropertyChange(PROP_DATABASE_CONNECTIONS, oldConnections, connections);
    }

    public synchronized void connect(final DatabaseConnection connection) {
        if (connection == null) {
            return;
        }
        boolean onlineBeforeConnect = isOnline();
        boolean failed = false;
        try {
            if (onlineBeforeConnect && connection.getJDBCConnection() != null && connection.getJDBCConnection().isValid(1000)) {
                //connection is ok.
                if (!onlineBeforeConnect && databaseConnectionsAreEqual(connection, templateConnection)) {
                    setOnline(true);
                    usagesEnabled = isFindUsagesEnabled();
                }
                return;
            } else {
               if (SwingUtilities.isEventDispatchThread()) {
                  try {
                     ConnectionManager.getDefault().showConnectionDialog(connection);
                  } catch (NullPointerException e) {
                     failed = true;
                  } catch (IllegalStateException e) {
                     failed = true;
                  }
               } else {
                  try {
                     SwingUtilities.invokeAndWait(new Runnable() {

                        @Override
                        public void run() {
                           ConnectionManager.getDefault().showConnectionDialog(connection);
                        }
                     });
                  } catch (InterruptedException e) {
                     failed = true;
                  } catch (InvocationTargetException e) {
                     failed = true;
                  }
               }
               if ((connection.getJDBCConnection() == null || connection.getJDBCConnection().isClosed())) {
                 if (SwingUtilities.isEventDispatchThread()) {
                    Task request = RP.post(new Runnable() {

                       @Override
                       public void run() {
                          try {
                             ConnectionManager.getDefault().connect(connection);
                          } catch (DatabaseException ex) {
                          }
                       }
                    });
                    try {
                       request.waitFinished(10000);
                    } catch (InterruptedException ex) {
                       Exceptions.printStackTrace(ex);
                    }
                 } else {
                    try {
                       ConnectionManager.getDefault().connect(connection);
                    } catch (DatabaseException ex) {
                       failed = true;
                    }
                 }
              }
           }
        } catch (SQLException ex) {
            failed = true;
        }
        failed = failed || !testConnection(connection);
        if (failed) {
            JOptionPane.showMessageDialog(null, "Can't connect to the database.");
        }
        if (databaseConnectionsAreEqual(connection, templateConnection)) {
            setOnline(!failed);
            if (!failed && !onlineBeforeConnect) {
                usagesEnabled = isFindUsagesEnabled();
            }
        }
    }

    public boolean hasConnection() {
        return templateConnection != null;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        if (this.online != online) {
            if (!online) {
                //ConnectionManager.getDefault().disconnect(templateConnection);
                //reset the connection
                templateConnection = DatabaseConnection.create(templateConnection.getJDBCDriver(), templateConnection.getDatabaseURL(), templateConnection.getUser(), templateConnection.getSchema(), templateConnection.getPassword(), true, templateConnection.getDisplayName());
                clearConnectionPool();
                this.debugConnection = null;
                DatabaseConnection[] oldConnections = new DatabaseConnection[connections.length];
                System.arraycopy(connections, 0, oldConnections, 0, connections.length);
                connections[0] = templateConnection;
                changeSupport.firePropertyChange(PROP_DATABASE_CONNECTIONS, oldConnections, connections);
            }
            this.online = online;
            changeSupport.firePropertyChange(PROP_ONLINE, !online, online);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
       PropertyChangeListener[] listeners = changeSupport.getPropertyChangeListeners();
       for(int i = 0; i<listeners.length; i++) {
          if(listeners[i]==listener) //don't register the same listener more than once
             return;
       }
       changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public boolean testConnection(DatabaseConnection connection) {
        Connection con = connection.getJDBCConnection();
        if (con == null) {
            return false;
        }
        try {
            Statement stmt = con.createStatement();
            boolean success = stmt.execute("select 1 from dual");
            stmt.close();
            return success;
        } catch (SQLException ex) {
            return false;
        } catch (Exception ex) { //ugly way to catch IOExceptions, etc
            return false;
        }
    }

    private class ConnectionErrorListener implements ExceptionListener {

        public ConnectionErrorListener() {
        }

        @Override
        public void exceptionThrown(Exception e) {
            setOnline(false);
        }
    }

    public boolean isUsagesEnabled() {
        return usagesEnabled;
    }

    private boolean isFindUsagesEnabled() {
        if (!isOnline()) {
            return false;
        }
        ResultSet rs = null;
        PreparedStatement stmt = null;
        String version = null;
        if (templateConnection == null || templateConnection.getJDBCConnection() == null) {
            return false;
        }

        try {
            String sqlSelect = "SELECT banner FROM v$version WHERE banner LIKE 'Oracle%'";
            stmt = templateConnection.getJDBCConnection().prepareStatement(sqlSelect);
            rs = stmt.executeQuery();
            if (rs.next()) {
                version = rs.getString(1);
            }

            if (version != null && checkOracleVersion(version) >= 11) {
                return true;
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
        return false;
    }

    private int checkOracleVersion(String version) {
        int lastIndex = version.indexOf('.');

        if (lastIndex > 0) {
            version = version.substring(0, lastIndex);
        } else {
            return -1;
        }
        int beginIndex = version.lastIndexOf(' ');
        if (beginIndex == -1) {
            return -1;
        }
        return Integer.parseInt(version.substring(beginIndex + 1));
    }
}
