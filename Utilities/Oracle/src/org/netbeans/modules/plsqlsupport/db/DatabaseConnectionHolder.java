/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsqlsupport.db;

import org.netbeans.api.db.explorer.DatabaseConnection;

/**
 *
 * @author chrlse
 */
public class DatabaseConnectionHolder {

    private DatabaseConnection databaseConnection;
    private DatabaseTransaction transaction;

    public DatabaseConnectionHolder() {
    }

    public DatabaseConnectionHolder(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public DatabaseConnection getDatabaseConnection() {
        return databaseConnection;
    }

    public void setDatabaseConnection(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public DatabaseTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(DatabaseTransaction transaction) {
        this.transaction = transaction;
    }
    
}
