/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.sql.SQLException;
import java.sql.Statement;
import org.openide.util.Exceptions;

/**
 * Used to hold {@link java.sql.Statement} to be able to access and cancel already executing statement from another
 * thread.
 *
 * @author ChrLSE
 */
class StatementHolder {

    private Statement statement;

    public void setStatement(Statement statement) {
        this.statement = statement;
    }

    void cancel() {
        try {
            statement.cancel();
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
