/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.beans.PropertyChangeListener;
import org.netbeans.api.db.explorer.DatabaseConnection;

/**
 * Implementation that always has auto commit enabled.
 *
 * @author ChrLSE
 */
class DatabaseTransactionAutoCommit extends DatabaseTransactionDefault {

    public DatabaseTransactionAutoCommit(DatabaseConnectionIO io, DatabaseConnection connection) {
        super(io, connection);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public boolean autoCommit() {
        return true;
    }
}
