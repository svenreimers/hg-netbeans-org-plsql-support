/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.beans.PropertyChangeListener;
import java.sql.Connection;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.plsql.execution.FileExecutionUtil;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionExecutor;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.loaders.DataObject;
import org.openide.util.Cancellable;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 * reference usage holder
 *
 * @author chrlse
 */
class PlsqlExecutor implements DatabaseConnectionExecutor {

    private static final Logger LOG = Logger.getLogger(PlsqlExecutor.class.getName());
    private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
    private static final RequestProcessor RP = new RequestProcessor(PlsqlExecutor.class.getName(), 4, true);
    private final DatabaseConnectionManager connectionProvider;
    private final DatabaseConnectionIO io;
    private DatabaseConnection connection;
    // is set to null if run from other than tdb (command window).
    private DatabaseTransaction transaction;

    //    private final boolean isCommandWindow;
    public PlsqlExecutor(DatabaseConnectionManager connectionProvider, DatabaseConnectionIO io, DatabaseConnection connection, DatabaseTransaction transaction) {
        this.connectionProvider = connectionProvider;
        this.io = io;
        this.connection = connection;
        this.transaction = transaction;
    }

    @Override
    public DatabaseConnection getConnection() {
        return connection;
    }

    private boolean startConnection(DatabaseConnection newConnection) {
        if (closeOpenTransaction()) {
            connection = newConnection;
            // XXX: is set in connectionProvider.getDatabaseConnectionFromPool
            DatabaseConnectionManager.setModuleInOracle(connection);
            transaction.setConnection(connection);
            return true;
        }
        return false;
    }

    /**
     *
     * @param newConnection the new {@link DatabaseConnection} to be used.
     * @return true if the given connection is the same as the old. true if given is a new different connection. false
     * if open transaction exist on old connection and user selects cancel.
     */
    @Override
    public boolean updateConnection(DatabaseConnection newConnection) {
        if (!connectionHasChanged(newConnection)) {
            return true;
        }
        return startConnection(newConnection);
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

    @Override
    public void addTransactionListener(PropertyChangeListener changeListener) {
        transaction.addPropertyChangeListener(changeListener);
    }

    @Override
    public void commitTransaction() {
        transaction.commitTransaction();
        connectionProvider.releaseDatabaseConnection(connection);
    }

    @Override
    public void rollbackTransaction() {
        transaction.rollbackTransaction();
        connectionProvider.releaseDatabaseConnection(connection);
    }

    /**
     * Close transaction if open, either by commit or rollback based on feedback from user.
     *
     * @return
     */
    @Override
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

    @Override
    public void execute(List<PlsqlExecutableObject> executableObjects, Document document) {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Executing database file...", new Cancellable() {
            @Override
            public boolean cancel() {
                return handleCancel();
            }
        });
//        try {
//            handle.start();
//            // If autocommit OFF - take the connection from data object.
//            if (autoCommit()) {
//                if (!updateConnection(connectionProvider.getTemplateConnection())) {
//                    return;
//                }
//            }
//
//            if (connection == connectionProvider.getTemplateConnection()) {
//                updateConnection(connectionProvider.getPooledDatabaseConnection(false, true));
//                if (connection == null) {
//                    return;
//                }
//            }
//            reconnectIfNeeded();
////                final DataObject obj = FileExecutionUtil.getDataObject(document);
////                FileObject file = obj.getPrimaryFile();
////                if (file == null) {
////                    return;
////                }
//            DataObject dataObj = FileExecutionUtil.getDataObject(document);
//            String fileName = dataObj.getPrimaryFile().getNameExt();
//            InputOutput io = initializeIO(fileName, dataObj.getNodeDelegate().getDisplayName(), dataObj, executableObjects.get(0));

        task = RP.post(new ExecutionTask(connection, executableObjects, document, handle));
//            task.waitFinished();
//                executor.executePLSQL(blocks, document);

//        } finally {
//            if (autoCommit()) {
//                connectionProvider.releaseDatabaseConnection(connection);
//            } else {
//                hasOpenTransaction();
//            }
//            handle.finish();
//        }
//        task = RP.post(new ExecutionTask(connectionProvider, connection, executableObjects, document));
    }
    private RequestProcessor.Task task;

    private boolean handleCancel() {
        LOG.info("handleCancel");
        if (null == task) {
            return false;
        }

        return task.cancel();
    }

    private boolean autoCommit() {
        return transaction == null || OptionsUtilities.isCommandWindowAutoCommitEnabled();
    }

    private class ExecutionTask implements Runnable {

        private DatabaseConnection connection;
        private final List<PlsqlExecutableObject> executableObjects;
        private final Document document;
        private PlsqlFileExecutor executor;
        private ProgressHandle handle;

        private ExecutionTask(DatabaseConnection connection, List<PlsqlExecutableObject> executableObjects, Document document, ProgressHandle handle) {
            this.connection = connection;
            this.executableObjects = executableObjects;
            this.document = document;
            this.handle = handle;
        }

        @Override
        public void run() {
//            ProgressHandle handle = ProgressHandleFactory.createHandle("Executing database file...", this);
            try {
                handle.start();
                // If autocommit OFF - take the connection from data object.
                if (autoCommit()) {
                    if (!updateConnection(connectionProvider.getTemplateConnection())) {
                        return;
                    }
                }

                DataObject dataObj = FileExecutionUtil.getDataObject(document);
                String fileName = dataObj.getPrimaryFile().getNameExt();
                io.initializeIO(fileName, dataObj.getNodeDelegate().getDisplayName(), executableObjects.get(0));
                if (connection == connectionProvider.getTemplateConnection()) {
                    connection = connectionProvider.getPooledDatabaseConnection(false, true);
                    if (connection == null) {
                        return;
                    }
                    startConnection(connection);
                }
//                final DataObject obj = FileExecutionUtil.getDataObject(document);
//                FileObject file = obj.getPrimaryFile();
//                if (file == null) {
//                    return;
//                }
                reconnectIfNeeded();
                executor = new PlsqlFileExecutor(connectionProvider, connection, executableObjects, document, io.getIO());
                executor.run();

            } finally {
                if (autoCommit()) {
                    connectionProvider.releaseDatabaseConnection(connection);
                } else {
                    hasOpenTransaction();
                }
                handle.finish();
            }
        }

//        @Override
//        public boolean cancel() {
//            if (executor != null) {
////                executor.cancel();
////                io.getErr().println("!!!Execution cancelled. Performing rollback");
////                jdbcConnection.rollback();
//            }
//            return true;
//        }
//    }
        private void reconnectIfNeeded() {
            //to reconnect if the connection is gone. 
            if (connection.getJDBCConnection() == null) {
                connectionProvider.connect(connection);
            }
            if (connection.getJDBCConnection() == null) {
                JOptionPane.showMessageDialog(null, "Connect to the Database");
            }
        }
    }
}