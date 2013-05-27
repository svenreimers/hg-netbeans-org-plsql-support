/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionExecutor;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = DatabaseConnectionExecutor.Factory.class)
public class DatabaseConnectionExecutorFactory implements DatabaseConnectionExecutor.Factory {

    private static final Logger LOG = Logger.getLogger(DatabaseConnectionExecutorFactory.class.getName());

    @Override
    public DatabaseConnectionExecutor create(DatabaseConnectionManager connectionProvider, DatabaseConnection connection, FileObject fileObject) {
        DatabaseConnectionIO io = new DatabaseConnectionIO();
        DatabaseTransaction databaseTransaction = DatabaseTransaction.create(io, connection, fileObject);
        return new PlsqlExecutor(connectionProvider, io, connection, databaseTransaction);
    }

    @Override
    public DatabaseConnectionExecutor create(FileObject fileObject) {
        final Project project = FileOwnerQuery.getOwner(fileObject);

        // TODO: if file not in project?!?
        if (project == null) {
            LOG.log(Level.WARNING, "project is null for fileObject={0}", fileObject.getNameExt());
            return null;
        }
        DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance(project);
        if (connectionManager == null) {
            LOG.log(Level.WARNING, "connectionManager is null for project={0}", project);
            return null;
        }
        final DatabaseConnection connection = connectionManager.getTemplateConnection();
        if (connection == null) {
            LOG.log(Level.WARNING, "connection is null for connectionManager={0}", connectionManager);
            return null;
        }

        return create(connectionManager, connection, fileObject);
    }
}
