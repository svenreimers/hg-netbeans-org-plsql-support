/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.windows.InputOutput;

/**
 *
 * @author chrlse
 */
class DatabaseConnectionSession {
    private DatabaseConnection connection;
    private InputOutput io;

    public DatabaseConnectionSession(DatabaseConnection connection, InputOutput io) {
        this.connection = connection;
        this.io = io;
    }

    public DatabaseConnection getConnection() {
        return connection;
    }

    public InputOutput getIo() {
        return io;
    }
    
}
