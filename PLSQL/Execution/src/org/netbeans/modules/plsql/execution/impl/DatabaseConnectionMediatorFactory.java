/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionAdapter;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = DatabaseConnectionMediator.Factory.class)
public class DatabaseConnectionMediatorFactory implements DatabaseConnectionMediator.Factory {

    private static final Logger LOG = Logger.getLogger(DatabaseConnectionMediatorFactory.class.getName());

    @Override
    public DatabaseConnectionMediator create(FileObject fileObject) {
        DatabaseConnectionManager connectionManager = DatabaseConnectionManager.getInstance(fileObject, false);
        DatabaseConnection connection = null;
        if (connectionManager != null) {
            connection = connectionManager.getDatabaseConnection(false);
        } else {
            LOG.log(Level.FINE, "connectionManager is null for fileObject={0}", fileObject);
        }
        if (connection == null) {
            LOG.log(Level.FINE, "connection is null for connectionManager={0}", connectionManager);
        }
        return create(connectionManager, new DatabaseConnectionAdapter(connection), fileObject);

    }

    @Override
    public DatabaseConnectionMediator create(DatabaseConnectionManager connectionManager, DatabaseConnectionAdapter connectionAdapter,
            FileObject fileObject) {
        DatabaseConnectionIO io = new DatabaseConnectionIO();
        DatabaseTransaction databaseTransaction = DatabaseTransaction.Factory.create(io, connectionAdapter, fileObject);
        return new DatabaseConnectionMediatorDefault(fileObject, connectionManager, connectionAdapter, databaseTransaction, io);
    }
}
