/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.beans.PropertyChangeListener;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.filesystems.FileObject;

/**
 *
 * @author ChrLSE
 */
public interface DatabaseTransaction {

    void addPropertyChangeListener(PropertyChangeListener listener);

    /**
     * Commit transaction if connection has Open Transaction
     *
     */
    void commitTransaction();

    /**
     *
     *
     * @return True if there are on going transaction for a connection.
     */
    boolean hasOpenTransaction();

    /**
     * Rollback transaction if connection has Open Transaction
     *
     */
    void rollbackTransaction();

    /**
     *
     * @return true if auto commit is enabled.
     */
    boolean autoCommit();

    public void setConnection(DatabaseConnection connection);

    public void checkForOpenTransaction();

    static class Factory {

        static DatabaseTransaction create(DatabaseConnectionIO io, DatabaseConnection connection, FileObject fileObject) {
            if (!fileObject.getExt().equalsIgnoreCase("tdb")) {
                return new DatabaseTransactionAutoCommit(io, connection);
            }
            return new DatabaseTransactionDefault(io, connection);
        }
    }
}
