/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.JOptionPane;
import org.netbeans.api.db.explorer.ConnectionManager;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionAdapter;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionExecutor;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionIO;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.filesystems.FileObject;

/**
 *
 * @author ChrLSE
 */
class DatabaseConnectionMediatorDefault implements DatabaseConnectionMediator {

    private final FileObject fileObject;
    private DatabaseConnectionManager manager;
    private DatabaseConnectionAdapter connection;
    private final DatabaseTransaction transaction;
    private final DatabaseConnectionIO io;

    public DatabaseConnectionMediatorDefault(FileObject fileObject, DatabaseConnectionManager manager, DatabaseConnectionAdapter connection, DatabaseTransaction transaction, DatabaseConnectionIO io) {
        this.fileObject = fileObject;
        this.manager = manager;
        this.connection = connection;
        this.transaction = transaction;
        this.io = io;
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
        return connection;
    }

    @Override
    public DatabaseConnectionExecutor getExecutor() {
        if (manager != null && connection.getConnection() == manager.getTemplateConnection()) {
            DatabaseConnection pooledDatabaseConnection = manager.getPooledDatabaseConnection(false, true);
            if (pooledDatabaseConnection == null) {
                return null;
            }
            connection.setConnection(pooledDatabaseConnection);
        }
        reconnectIfNeeded();
        return new PlsqlExecutor(this, connection, new StatementHolder());
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
                String title = "Commit open transaction?";
                int dialogAnswer = JOptionPane.showOptionDialog(null, msg, title, JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE, null, null, null);
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
}
