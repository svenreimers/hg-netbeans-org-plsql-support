/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.text.Document;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionAdapter;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionExecutor;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionIO;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
import org.openide.util.Cancellable;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.TaskListener;

/**
 * reference usage holder
 *
 * @author chrlse
 */
class PlsqlExecutor implements DatabaseConnectionExecutor {

    private static final Logger LOG = Logger.getLogger(PlsqlExecutor.class.getName());
    private static final RequestProcessor RP = new RequestProcessor(PlsqlExecutor.class.getName(), 4, true);
    private final DatabaseConnectionMediator connectionMediator;
    private final DatabaseConnectionAdapter connection;
    private final StatementHolder statementHolder;
    private DatabaseConnectionIO connectionIO;

    public PlsqlExecutor(DatabaseConnectionMediator connectionMediator, DatabaseConnectionAdapter connection, StatementHolder statementHolder) {
        this.connectionMediator = connectionMediator;
        this.connection = connection;
        this.statementHolder = statementHolder;
        this.connectionIO = connectionMediator.getIo();
    }

    @Override
    public void execute(List<PlsqlExecutableObject> executableObjects, Document document) {
        connectionIO.setSummery(executableObjects.get(0).getSummery());
        final ProgressHandle handle = ProgressHandleFactory.createHandle(connectionIO.executionDisplayName(), new Cancellable() {
            @Override
            public boolean cancel() {
                return handleCancel();
            }
        });
        task = RP.create(new ExecutionTask(connectionMediator, connection, executableObjects, document, handle));
        task.addTaskListener(new TaskListener() {
            @Override
            public void taskFinished(Task task) {
                handle.finish();
            }
        });
        task.schedule(0); //start the task
    }
    private RequestProcessor.Task task;

    private boolean handleCancel() {
        LOG.log(Level.INFO, "Cancel {0}", task.toString());
        if (null == task) {
            return false;
        }
        statementHolder.cancel();
        return task.cancel();
    }

    private class ExecutionTask implements Runnable {

        private final DatabaseConnectionMediator mediator;
        private final DatabaseConnectionAdapter connection;
        private final List<PlsqlExecutableObject> executableObjects;
        private final Document document;
        private final ProgressHandle handle;
        private PlsqlFileExecutor executor;

        public ExecutionTask(DatabaseConnectionMediator mediator, DatabaseConnectionAdapter connection, List<PlsqlExecutableObject> executableObjects, Document document, ProgressHandle handle) {
            this.mediator = mediator;
            this.connection = connection;
            this.executableObjects = executableObjects;
            this.document = document;
            this.handle = handle;
        }

        @Override
        public void run() {
            try {
                handle.start();

                connectionIO.initialize();
                executor = new PlsqlFileExecutor(statementHolder, connection.getConnection(), mediator.getIo().getIO());
                executor.executePLSQL(executableObjects, document);
            } catch (InterruptedException ex) {
                mediator.getIo().println("the task was CANCELLED");
            } finally {
                mediator.finish();
                handle.finish();
            }
        }
    }
}