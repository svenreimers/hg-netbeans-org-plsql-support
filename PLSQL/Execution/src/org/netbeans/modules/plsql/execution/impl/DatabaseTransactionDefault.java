/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.util.Exceptions;

/**
 *
 * @author SubSLK
 * @author chrlse
 */
class DatabaseTransactionDefault implements DatabaseTransaction {

    private static final Logger LOG = Logger.getLogger(DatabaseTransactionDefault.class.getName());
    static final String PROP_TRANSACTION = "TransactionOpen";
    private final PropertyChangeSupport changeSupport;
    private final DatabaseConnectionIO io;
    private DatabaseConnection connection;
    private boolean open = false;
    private String transactionId;

    DatabaseTransactionDefault(DatabaseConnectionIO io, DatabaseConnection connection) {
        changeSupport = new PropertyChangeSupport(this);
        this.io = io;
        this.connection = connection;
    }

    @Override
    public void setConnection(DatabaseConnection connection) {
        this.connection = connection;
        close();
    }

    private void close() {
        setOpen(false);
    }

    private void setOpen(boolean newOpen) {
        boolean oldOpen = open;
        open = newOpen;
        changeSupport.firePropertyChange(PROP_TRANSACTION, oldOpen, open);
    }

    public boolean isOpen() {
        return open;
    }

    /**
     *
     *
     */
    @Override
    public void commitTransaction() {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Commit database file...");
        handle.start();

        try {
            if (hasOpenTransaction()) {
                commitRollbackTransactions(true);
            }
            close();
            io.println((new StringBuilder()).append("> Commit of Transaction ID = [")
                    .append(transactionId).append("] successful"));
        } catch (Exception ex) {
            io.println((new StringBuilder()).append(">!!! Error Commit Statement"));
            Exceptions.printStackTrace(ex);
        } finally {
            handle.finish();
        }
    }

    /**
     *
     *
     */
    @Override
    public void rollbackTransaction() {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Rollback database file...");
        handle.start();

        try {
            if (hasOpenTransaction()) {
                commitRollbackTransactions(false);
            }
            close();
            io.println((new StringBuilder()).append("> Rollback of Transaction ID = [")
                    .append(transactionId).append("] successful"));

        } catch (Exception ex) {
            io.println((new StringBuilder()).append(">!!! Error Rollback Statement"));
            Exceptions.printStackTrace(ex);
        } finally {
            handle.finish();
        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        PropertyChangeListener[] listeners = changeSupport.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == listener) {
                return;
            }
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        PropertyChangeListener[] listeners = changeSupport.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] == listener) {
                return;
            }
        }
        changeSupport.addPropertyChangeListener(propertyName, listener);
    }

    /**
     *
     *
     * @return True if there in on going transactions for a command window.
     */
    @Override
    public boolean hasOpenTransaction() {
        boolean isOpen = false;
        if (connection.getJDBCConnection() == null) {
            setOpen(isOpen);
            return isOpen;
        }

        try {
            String sqlProc = "{call ? := DBMS_TRANSACTION.local_transaction_id}";
            CallableStatement stmt = connection.getJDBCConnection().prepareCall(sqlProc);
            stmt.registerOutParameter(1, java.sql.Types.VARCHAR);
            stmt.executeUpdate();
            transactionId = stmt.getString(1);
            if (transactionId != null) {
                io.println(("Transaction open with ID = [" + transactionId + "]"));
                isOpen = true;
            } else {
                isOpen = false;
            }
        } catch (SQLException ex) {
            LOG.log(Level.WARNING, "", ex);
            isOpen = false;
        }
        setOpen(isOpen);
        return isOpen;
    }

    /**
     *
     * @param commit the value of commit
     */
    private void commitRollbackTransactions(boolean commit) {
        try {
            if (connection.getJDBCConnection() == null) {
                return;
            }
            Connection con = connection.getJDBCConnection();

            if (commit) {
                con.commit();
            } else {
                con.rollback();
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    public boolean autoCommit() {
        return OptionsUtilities.isCommandWindowAutoCommitEnabled();
    }
}
