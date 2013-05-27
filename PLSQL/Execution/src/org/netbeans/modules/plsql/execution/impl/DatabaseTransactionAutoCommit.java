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
class DatabaseTransactionAutoCommit implements DatabaseTransaction {

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void commitTransaction() {
    }

    @Override
    public boolean hasOpenTransaction() {
        return false;
    }

    @Override
    public void rollbackTransaction() {
    }

    @Override
    public boolean autoCommit() {
        return true;
    }

    @Override
    public void setConnection(DatabaseConnection connection) {
    }
}
