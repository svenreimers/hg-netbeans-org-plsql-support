/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.netbeans.api.db.explorer.ConnectionManager;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.db.explorer.DatabaseException;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionAdapter;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionExecutor;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionIO;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 *
 * @author ChrLSE
 */
class DatabaseConnectionMediatorDefault implements DatabaseConnectionMediator {

    private static final RequestProcessor RP = new RequestProcessor(DatabaseConnectionMediatorDefault.class);
    private static final Logger LOG = Logger.getLogger(DatabaseConnectionMediatorDefault.class.getName());
    private final FileObject fileObject;
    private final DatabaseConnectionManager manager;
    private final DatabaseConnectionAdapter connection;
    private final DatabaseTransaction transaction;
    private final DatabaseConnectionIO io;
    private final SelectDatabaseNotifier notifier;

    public DatabaseConnectionMediatorDefault(FileObject fileObject, DatabaseConnectionManager manager, DatabaseConnectionAdapter connection, DatabaseTransaction transaction, DatabaseConnectionIO io, SelectDatabaseNotifier notifier) {
        this.fileObject = fileObject;
        this.manager = manager;
        this.connection = connection;
        this.transaction = transaction;
        this.io = io;
        this.notifier = notifier;
    }

    @Override
    public boolean hasProjectDatabaseConnections() {
        return manager != null;
    }

    @Override
    public List<DatabaseConnection> getDatabaseConnections() {
        if (manager != null) {
            return manager.getDatabaseConnections();
        }
        List<DatabaseConnection> connections = new ArrayList<DatabaseConnection>();
        DatabaseConnection[] dcs = ConnectionManager.getDefault().getConnections();
        for (DatabaseConnection dc : dcs) {
            if (dc.getDriverClass().endsWith("OracleDriver")) {
                connections.add(dc);
            }
        }
        sortConnections(connections);
        return connections;
    }

    void sortConnections(List<DatabaseConnection> connectionList) {
        Collections.sort(connectionList, new Comparator<DatabaseConnection>() {
            @Override
            public int compare(DatabaseConnection o1, DatabaseConnection o2) {
                return o1.getDisplayName().compareTo(o2.getDisplayName());
            }
        });
    }

    @Override
    public DatabaseConnectionAdapter getConnection() {
        connect();
        return connection;
    }

    @Override
    public DatabaseConnectionExecutor getExecutor() {
        if (connection.getConnection() == null) {
            notifier.notify(fileObject.getNameExt());
            return null;
        }
        if (manager != null && connection.getConnection() == manager.getTemplateConnection()) {
            DatabaseConnection pooledDatabaseConnection = manager.getPooledDatabaseConnection(false, true);
            if (pooledDatabaseConnection == null) {
                return null;
            }
            connection.setConnection(pooledDatabaseConnection);
        }
        reconnectIfNeeded();
        return new PlsqlExecutor(this, connection, new StatementHolder(), io);
    }

    private void reconnectIfNeeded() {
        //to reconnect if the connection is gone. 
        if (connection.getJDBCConnection() == null) {
            if (manager != null) {
                manager.connect(connection.getConnection());
            } else {
                ConnectionManager.getDefault().showConnectionDialog(connection.getConnection());
            }
        }
        if (connection.getJDBCConnection() == null) {
            JOptionPane.showMessageDialog(null, "Connect to the Database");
        }
    }

    @Override
    public void addTransactionListener(PropertyChangeListener changeListener) {
        transaction.addPropertyChangeListener(changeListener);
    }

    @Override
    public void commitTransaction() {
        transaction.commitTransaction();
        finish();
    }

    @Override
    public void rollbackTransaction() {
        transaction.rollbackTransaction();
        finish();
    }

    @Override
    public boolean closeOpenTransaction() {
        if (transaction.hasOpenTransaction()) {
            if (OptionsUtilities.isCommandWindowAutoCommitEnabled()) {
                transaction.commitTransaction();
            } else {
                String msg = "Commit open transactions for " + connection.getDisplayName() + "?";
                String title = "Open transactions!";
                Object[] options = {"Commit", "Rollback", "Cancel"};
                int dialogAnswer = JOptionPane.showOptionDialog(null, msg, title, JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, options, null);
                if (dialogAnswer == JOptionPane.YES_OPTION) {
                    commitTransaction();
                } else if (dialogAnswer == JOptionPane.NO_OPTION) {
                    rollbackTransaction();
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean hasOpenTransaction() {
        return transaction.hasOpenTransaction();
    }

    @Override
    public boolean updateConnection(DatabaseConnection newConnection) {
        if (!connection.equals(newConnection) && closeOpenTransaction()) {
            // XXX: is set in connectionProvider.getDatabaseConnectionFromPool
//            DatabaseConnectionManager.setModuleInOracle(connection);
            connection.setConnection(newConnection);
        }
        return false;
    }

    DatabaseTransaction getTransaction() {
        return transaction;
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public DatabaseConnectionIO getIo() {
        return io;
    }

    @Override
    public void finish() {
        if (transaction.autoCommit()) {
            transaction.commitTransaction();
        }
        if (!hasOpenTransaction()) {
            if (manager != null) {
                manager.releaseDatabaseConnection(connection.getConnection());
            }
        }
    }

    @Override
    public boolean isDefaultDatabase(DatabaseConnection connection) {
        if (manager == null) {
            return false;
        }
        return manager.isDefaultDatabase(connection);
    }

    private void connect() {
        if (connection.getConnection() == null || isConnected()) {
            return;
        }
        RequestProcessor.Task request = RP.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!ConnectionManager.getDefault().connect(connection.getConnection())) {
                        LOG.log(Level.FINE, "not able to connect silently to {0}", connection);

                        SwingUtilities.invokeAndWait(new Runnable() {
                            @Override
                            public void run() {
                                LOG.log(Level.FINER, "running ConnectionManager.getDefault().showConnectionDialog(connection.getConnection()) "
                                        + "in invokeAndWait for connection {0}", connection);
                                ConnectionManager.getDefault().showConnectionDialog(connection.getConnection());
                            }
                        });
                    }
                } catch (DatabaseException ex) {
                    LOG.log(Level.INFO, "not able to connect to " + connection, ex);
                } catch (InterruptedException ex) {
                    LOG.log(Level.FINE, "not able to connect to " + connection, ex);
                } catch (InvocationTargetException ex) {
                    LOG.log(Level.FINE, "not able to connect to " + connection, ex);
                }
                setModuleInOracle();
            }
        });
        try {
            request.waitFinished(10000);
        } catch (InterruptedException ex) {
            Exceptions.printStackTrace(ex);
        }

    }

    /**
     * This method attempts to set the module/program that is connecting to the database using Oracle's
     * 'Dbms_Application_Info.Set_Module' procedure. This will be useful for tracing current connections in the DB
     * created by NetBeans by querying 'v$sessions'
     */
    private void setModuleInOracle() {
        try {
            if (connection == null || connection.getJDBCConnection() == null) {
                return;
            }
            String appName;
            try {
                appName = NbBundle.getBundle("org.netbeans.core.windows.view.ui.Bundle").getString("CTL_MainWindow_Title_No_Project");
            } catch (MissingResourceException x) {
                appName = "NetBeans"; // NOI18N
            }
            String sqlProc = "{call Dbms_Application_Info.Set_Module(?,?)}";
            CallableStatement stmt;
            stmt = connection.getJDBCConnection().prepareCall(sqlProc);
            stmt.setString(1, appName);
            stmt.setString(2, "Main Program");
            stmt.executeUpdate();
        } catch (SQLException ex) {
            // Exceptions.printStackTrace(ex);
            LOG.log(Level.WARNING, "Error when adding Module in v$Session");
        }
    }
}
