/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsqlsupport.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.db.explorer.DatabaseConnection;

/**
 *
 * @author chrlse
 */
public class DatabaseConnectionAdapter {

    private static final Logger LOG = Logger.getLogger(DatabaseConnectionAdapter.class.getName());
    private DatabaseConnection connection;

    public DatabaseConnectionAdapter(DatabaseConnection connection) {
        this.connection = connection;
    }

    public DatabaseConnection getConnection() {
        return connection;
    }

    public void setConnection(DatabaseConnection connection) {
        this.connection = connection;
    }

    public Connection getJDBCConnection() {
        if (connection == null) {
            return null;
        }

        return connection.getJDBCConnection();
    }

    public boolean isConnected() {
        try {
            if (connection != null && connection.getJDBCConnection() != null && connection.getJDBCConnection().isValid(1000)) {
                return true;
            }
        } catch (SQLException ex) {
            LOG.log(Level.FINE, null, ex);
        }
        return false;
    }

    public CallableStatement prepareCall(String sqlProc) throws SQLException {
        if (!isConnected()) {
            return null;
        }
        return connection.getJDBCConnection().prepareCall(sqlProc);
    }

    public boolean commitTransactions() throws SQLException {
        if (!isConnected()) {
            return false;
        }
        connection.getJDBCConnection().commit();
        return true;
    }

    public boolean rollbackTransactions() throws SQLException {
        if (!isConnected()) {
            return false;
        }
        connection.getJDBCConnection().rollback();
        return true;
    }

    public String getDisplayName() {
        if (connection == null) {
            return "";
        }

        return connection.getDisplayName();
    }

    public boolean equals(DatabaseConnection other) {
        if (connection == null && other == null) {
            return true;
        }
        if (other == null || connection == null || !connection.getName().equals(other.getName())) {
            return false;
        }
        return true;
    }
}
