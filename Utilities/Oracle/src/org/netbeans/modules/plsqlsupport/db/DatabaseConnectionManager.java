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
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
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
import org.netbeans.modules.plsqlsupport.db.ui.DatabaseConnectionPanel;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

public class DatabaseConnectionManager {

    private static final boolean NB_IN_TEST_MODE = Boolean.getBoolean("netbeans.full.hack"); //NOI18N
    public static final String ORACLE_DRIVER_CLASS_NAME = "oracle.jdbc.OracleDriver";
    public static final String PROP_DATABASE_CONNECTIONS = "databaseConnections";
    public static final String PROP_CW_DATABASE_CONNECTIONS = "commandWindowDatabaseConnections";
    public static final String PROP_ONLINE = "online";
    private static final String REVERSE_DATABASE_KEY = "reverse.engineering.database";
    private static Map<FileObject, DatabaseConnectionManager> instances = new HashMap<FileObject, DatabaseConnectionManager>();
    private List<DatabaseConnection> connections = new ArrayList<DatabaseConnection>();
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
    private DatabaseConnection reverseConnection;
    private static List<String> failedConnections = new ArrayList<String>();

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
        return getInstance(dataObject.getPrimaryFile());
    }

    public static DatabaseConnectionManager getInstance(FileObject fileObject) {
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
        if (!NB_IN_TEST_MODE && provider.getDatabaseConnection(true) == null) { // prompt connection dialog for files outside the project structure
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

    public List<DatabaseConnection> getDatabaseConnections() {
        return connections;
    }

    /**
     *
     * @return The database URL of the primary database connection, empty String if none is found.
     */
    public String getPrimaryConnectionURL() {
        String result = "";
        if (!connections.isEmpty()) {
            result = connections.get(0).getDatabaseURL();
        }
        return result;
    }

    /**
     *
     * @param connection
     * @return true if given connection is the template connection (main).
     */
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
                if (!connectionPool.contains(connection)) {
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
            connection = DatabaseConnection.create(templateConnection.getJDBCDriver(), templateConnection.getDatabaseURL(), templateConnection.getUser(), templateConnection.getSchema(), templateConnection.getPassword(), true, templateConnection.getDisplayName());
            setModuleInOracle(connection);
            return connection;
        }
    }

    /**
     *
     * @return the Template Connection (main connection).
     */
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
                connections.clear();
                if (templateConnection != null) {
                    connections.add(templateConnection);
                    usagesEnabled = isFindUsagesEnabled();
                    DatabaseContentManager contentManager = DatabaseContentManager.getInstance(templateConnection);
                    contentManager.addExceptionListener(connectionErrorListener);
                    contentManager.updateCache(this);
                }
            }
        }

        if (!online) {
            if (force) {
                if (failedConnections.contains(templateConnection.getName())) {
                    failedConnections.remove(templateConnection.getName());
                }
            }
            connect(templateConnection);
        }
        if (!online) {
            return null;
        }
        DatabaseConnection connection = templateConnection;
        if (useConnectionPool) {
            connection = getDatabaseConnectionFromPool();
            if (connection != null && connection.getJDBCConnection() == null) {
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

    public void setDatabaseConnections(List<DatabaseConnection> newConnections) {
//      List<DatabaseConnection> oldConnections = new ArrayList<DatabaseConnection>(connections.size());
//      Collections.copy(oldConnections, connections);
//      this.connections.clear();
//      this.connections.addAll(newConnections);
        List<DatabaseConnection> oldConnections = connections;
        connections = newConnections;
        if (templateConnection != null) {
            DatabaseContentManager.getInstance(templateConnection).removeExceptionListener(connectionErrorListener);
        }
        templateConnection = !newConnections.isEmpty() ? newConnections.get(0) : null;
        if (templateConnection != null) {
            DatabaseContentManager.getInstance(templateConnection).addExceptionListener(connectionErrorListener);
        }

        //clear the connection pool and the debug connection when the main database changes
        clearConnectionPool();
        this.debugConnection = null;
        changeSupport.firePropertyChange(PROP_DATABASE_CONNECTIONS, oldConnections, newConnections);
    }

    /*
     * This method attempts to set the module/program that is connecting to the database
     * using Oracle's 'Dbms_Application_Info.Set_Module' procedure. This will be useful 
     * for tracing current connections in the DB created by NetBeans by querying 'v$sessions'
     */
    public static void setModuleInOracle(final DatabaseConnection connection) {
        try {
            ResultSet rs = null;
            CallableStatement stmt = null;
            if (connection == null || connection.getJDBCConnection() == null) {
                return;
            }
            String appName = "";
            try {
                appName = NbBundle.getBundle("org.netbeans.core.windows.view.ui.Bundle").getString("CTL_MainWindow_Title_No_Project");
            } catch (MissingResourceException x) {
                appName = "NetBeans"; // NOI18N
            }
            String sqlProc = "{call Dbms_Application_Info.Set_Module(?,?)}";
            stmt = connection.getJDBCConnection().prepareCall(sqlProc);
            stmt.setString(1, appName);
            stmt.setString(2, "Main Program");
            stmt.executeUpdate();
        } catch (SQLException ex) {
            // Exceptions.printStackTrace(ex);
            logger.log(Level.WARNING, "Error when adding Module in v$Session");
        }
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
                if (failedConnections.contains(connection.getName())) {
                    failedConnections.remove(connection.getName());
                }
                return;
            } else {
                if (!failedConnections.contains(connection.getName())) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        try {
                            ConnectionManager.getDefault().showConnectionDialog(connection);
                            setModuleInOracle(connection);
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
                                    setModuleInOracle(connection);
                                }
                            });
                        } catch (InterruptedException e) {
                            failed = true;
                        } catch (InvocationTargetException e) {
                            failed = true;
                        }
                    }
                }
                if ((connection.getJDBCConnection() == null || connection.getJDBCConnection().isClosed())) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        Task request = RP.post(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    ConnectionManager.getDefault().connect(connection);
                                    setModuleInOracle(connection);
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
                            setModuleInOracle(connection);
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
            if (!failedConnections.contains(connection.getName())) {
                JOptionPane.showMessageDialog(null, "Can't connect to the database.");
                failedConnections.add(connection.getName());
            }
        }
        if (databaseConnectionsAreEqual(connection, templateConnection)) {
            setOnline(!failed);
            if (!failed && !onlineBeforeConnect) {
                usagesEnabled = isFindUsagesEnabled();
            }
        }
        if (!failed) {
            if (failedConnections.contains(connection.getName())) {
                failedConnections.remove(connection.getName());
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
                List<DatabaseConnection> oldConnections = new ArrayList<DatabaseConnection>(connections.size());
                oldConnections.addAll(connections);
                connections.set(0, templateConnection);
                changeSupport.firePropertyChange(PROP_DATABASE_CONNECTIONS, oldConnections, connections);
            }
            this.online = online;
            changeSupport.firePropertyChange(PROP_ONLINE, !online, online);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        PropertyChangeListener[] listeners = changeSupport.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            //don't register the same listener more than once
            if (listeners[i] == listener) {
                return;
            }
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        PropertyChangeListener[] listeners = changeSupport.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == listener) //don't register the same listener more than once
            {
                return;
            }
        }
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public static boolean testConnection(DatabaseConnection connection) {
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

    /**
     * Set Reverse Engineering DatabaseConnection
     *
     * @param newConnection
     */
    public void setReverseConnection(DatabaseConnection newConnection) {
        DatabaseConnection oldConnection = this.reverseConnection;
        this.reverseConnection = newConnection;
        changeSupport.firePropertyChange(REVERSE_DATABASE_KEY, oldConnection, newConnection);
    }

    /**
     * Returns Reverse Engineering DatabaseConnection
     *
     * @return
     */
    public DatabaseConnection getReverseConnection() {
        return reverseConnection;
    }

    /**
     * Returns true is Reverse Engineering DatabaseConnection has been set.
     *
     * @return
     */
    public boolean hasReverseConnection() {
        return reverseConnection != null;
    }

    /**
     * Returns true is Reverse Engineering DatabaseConnection is online.
     *
     * @return
     */
    public boolean isReverseOnline() {
        try {
            return hasReverseConnection() && reverseConnection.getJDBCConnection() != null && reverseConnection.getJDBCConnection().isValid(1000);
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }

    private class ConnectionErrorListener implements ExceptionListener {

        public ConnectionErrorListener() {
        }

        @Override
        public void exceptionThrown(Exception e) {
            logger.log(Level.INFO, e.getMessage(), e);
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
