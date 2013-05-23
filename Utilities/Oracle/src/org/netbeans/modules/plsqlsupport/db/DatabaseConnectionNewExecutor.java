/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsqlsupport.db;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import javax.swing.JOptionPane;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.filesystems.FileObject;
import org.openide.windows.IOProvider;

/**
 * reference usage holder
 *
 * @author chrlse
 */
public class DatabaseConnectionNewExecutor implements DatabaseConnectionExecutor {

    public static DatabaseConnectionNewExecutor create(DatabaseConnection connection, FileObject fileObject) {
        return new DatabaseConnectionNewExecutor(connection, new DatabaseTransaction(connection, IOProvider.getDefault().getIO(fileObject.getNameExt(), false)));
    }
    private DatabaseConnection connection;
    private DatabaseTransaction transaction;

    private DatabaseConnectionNewExecutor(DatabaseConnection connection, DatabaseTransaction transaction) {
        this.connection = connection;
        this.transaction = transaction;
    }

    public DatabaseConnection getConnection() {
        return connection;
    }

    /**
     *
     * @param newConnection the new {@link DatabaseConnection} to be used.
     * @return true if the given connection is the same as the old. true if given is a new different connection. false
     * if open transaction exist on old connection and user selects cancel.
     */
    public boolean updateConnection(DatabaseConnection newConnection) {
        if (!connectionHasChanged(newConnection)) {
            return true;
        }
        if (closeOpenTransaction()) {
            connection = newConnection;
            DatabaseConnectionManager.setModuleInOracle(connection);
            transaction.setConnection(connection);
            return true;
        }
        return false;
    }

    private boolean connectionHasChanged(DatabaseConnection newConnection) {
        return connection != null && !connection.getName().equals(newConnection.getName());
    }

    DatabaseTransaction getTransaction() {
        return transaction;
    }

    public String getDisplayName() {
        return "Using DB: " + connection.getDisplayName() + " [" + connection.getName() + "]";
    }

    public boolean hasOpenTransaction() {
        return transaction.hasOpenTransaction();
    }

//    public void openTransaction() {
//        transaction.open();
//    }
    public Connection getJDBCConnection() {
        return connection.getJDBCConnection();
    }

    public void addTransactionListener(PropertyChangeListener changeListener) {
        transaction.addPropertyChangeListener(changeListener);
    }

    public void commitTransaction() {
        transaction.commitTransaction();
    }

    public void rollbackTransaction() {
        transaction.rollbackTransaction();
    }

    /**
     * Close transaction if open, either by commit or rollback based on feedback from user.
     *
     * @return
     */
    public boolean closeOpenTransaction() {
        if (!OptionsUtilities.isDeployNoPromptEnabled() && transaction.hasOpenTransaction()) {
            String msg = "Commit open transactions for " + connection.getDisplayName() + "?";
            String title = "Commit open transaction?";
            int dialogAnswer = JOptionPane.showOptionDialog(null, msg, title, JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, null, null);
            if (dialogAnswer == JOptionPane.YES_OPTION) {
                transaction.commitTransaction();
            } else if (dialogAnswer == JOptionPane.NO_OPTION) {
                transaction.rollbackTransaction();
            } else {
                return false;
            }
        }
        return true;
    }
}
