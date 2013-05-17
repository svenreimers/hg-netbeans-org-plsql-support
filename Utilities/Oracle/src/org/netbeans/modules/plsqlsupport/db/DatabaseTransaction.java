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
package org.netbeans.modules.plsqlsupport.db;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author SubSLK
 * @author chrlse
 */
public class DatabaseTransaction {

    private final static List<DatabaseTransaction> instance = new ArrayList<DatabaseTransaction>();
    private boolean open;
    private final DataObject dataObject;
    public static final String PROP_commit = "PlsqlCommit";
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private final InputOutput io;

    public DatabaseTransaction(DataObject dataObject, InputOutput io) {
        open = false;
        this.dataObject = dataObject;
        this.io = io;
    }

    public static DatabaseTransaction getInstance(DataObject object) {
        if (object == null) {
            return null;
        }
        InputOutput io = IOProvider.getDefault().getIO(object.getPrimaryFile().getNameExt(), false);

        if (instance.isEmpty()) {
            instance.add(new DatabaseTransaction(object, io));
            return instance.get(0);
        } else {
            for (int i = 0; i < instance.size(); i++) {
                DatabaseTransaction plsqlCommit = instance.get(i);
                if (plsqlCommit.dataObject.equals(object)) {
                    return plsqlCommit;
                }
            }
            DatabaseTransaction transaction = new DatabaseTransaction(object, io);
            instance.add(transaction);
            return transaction;
        }
    }

    public void open() {
        setOpen(true);
    }

    public void close() {
        setOpen(false);
    }

    private void setOpen(boolean newOpen) {
        boolean oldOpen = open;
        open = newOpen;
        changeSupport.firePropertyChange(PROP_commit, oldOpen, open);
    }

    public boolean isOpen() {
        return open;
    }

    public void commitTransaction(DatabaseConnection connection, DatabaseConnectionManager connectionProvider) {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Commit database file...");
        handle.start();

        try {
            if (connection.getJDBCConnection() != null && connectionProvider.hasDataToCommit(connection)) {
                connectionProvider.commitRollbackTransactions(connection, true);
            }
            close();
            if (!io.isClosed()) {
                io.getOut().println((new StringBuilder()).append("> Commit Statement successfully"));
            }
        } catch (Exception ex) {
            io.getOut().println((new StringBuilder()).append(">!!! Error Commit Statement"));
            Exceptions.printStackTrace(ex);
        } finally {
            handle.finish();
        }
    }

    public void rollbackTransaction(DatabaseConnection connection, DatabaseConnectionManager connectionProvider) {
        ProgressHandle handle = ProgressHandleFactory.createHandle("Rollback database file...");
        handle.start();

        try {
            if (connection.getJDBCConnection() != null && connectionProvider.hasDataToCommit(connection)) {
                connectionProvider.commitRollbackTransactions(connection, false);
            }
            close();
            if (!io.isClosed()) {
                io.getOut().println((new StringBuilder()).append("> Rollback Statement successfully"));
            }

        } catch (Exception ex) {
            io.getOut().println((new StringBuilder()).append(">!!! Error Rollback Statement"));
            Exceptions.printStackTrace(ex);
        } finally {
            handle.finish();
        }
    }

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
}
