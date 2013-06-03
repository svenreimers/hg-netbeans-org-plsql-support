package org.netbeans.modules.plsqlsupport.db;

import java.beans.PropertyChangeListener;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.filesystems.FileObject;
import org.openide.windows.InputOutput;

/**
 *
 * @author chrlse
 */
public interface DatabaseConnectionMediator {

    /**
     *
     * @param newConnection the new {@link DatabaseConnection} to be used.
     * @return true if the connection is updated with new connection.
     */
    public boolean updateConnection(DatabaseConnection databaseConnection);

    /**
     * Should be used when updating UI.
     *
     * @return will return DatabaseConnectionAdapter for selected used by UI.
     */
    DatabaseConnectionAdapter getConnection();

    /**
     * Should be used when executing PLSQL or SQL.
     *
     * @return will return DatabaseConnectionExecutor with pooled connection if possible.
     */
    DatabaseConnectionExecutor getExecutor();

    DatabaseConnectionManager getConnectionManager();

    boolean isConnected();

    /**
     * Close transaction if open, either by commit or rollback based on feedback from user. If user cancels, false will
     * be returned to indicate that transaction is not closed.
     *
     * @return true if transaction was finished in any way, either by commit or rollback.
     */
    public boolean closeOpenTransaction();

    public void commitTransaction();

    public void rollbackTransaction();

    public void addTransactionListener(PropertyChangeListener changeListener);

    public void initializeIO(String fileName, String displayName, PlsqlExecutableObject get);

    /**
     * Has to be called, will close any loose ends. Such as release pooled connections if needed.
     */
    public void finish();

    public InputOutput getIo();

    public static interface Factory {

        public DatabaseConnectionMediator create(FileObject fileObject);

        public DatabaseConnectionMediator create(DatabaseConnectionManager manager, DatabaseConnectionAdapter connection,
                FileObject fileObject);
    }
}
