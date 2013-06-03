/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.beans.PropertyChangeListener;
import javax.swing.JOptionPane;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionAdapter;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionExecutor;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.filesystems.FileObject;
import org.openide.windows.InputOutput;

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
    public DatabaseConnectionManager getConnectionManager() {
        if (manager == null) {
            manager = DatabaseConnectionManager.getInstance(fileObject, true);
        }
        return manager;
    }

    @Override
    public DatabaseConnectionAdapter getConnection() {
        return connection;
    }

    @Override
    public DatabaseConnectionExecutor getExecutor() {
        if (connection.getConnection() == getConnectionManager().getTemplateConnection()) {
            DatabaseConnection pooledDatabaseConnection = getConnectionManager().getPooledDatabaseConnection(false, true);
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
            getConnectionManager().connect(connection.getConnection());
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

//    public String getDisplayName() {
//        return "Using DB: " + connection.getDisplayName() + " [" + connection.getName() + "]";
//    }
    DatabaseTransaction getTransaction() {
        return transaction;
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void initializeIO(String fileName, String displayName, PlsqlExecutableObject executableObject) {
        io.initializeIO(fileName, displayName, executableObject);
    }

    @Override
    public InputOutput getIo() {
        return io.getIO();
    }

    @Override
    public void finish() {
        if (transaction.autoCommit()) {
            transaction.commitTransaction();
        }
        if (!hasOpenTransaction()) {
            getConnectionManager().releaseDatabaseConnection(connection.getConnection());
        }
    }
}
