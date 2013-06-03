package org.netbeans.modules.plsqlsupport.db;

import java.beans.PropertyChangeListener;
import java.util.List;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.filesystems.FileObject;
import org.openide.windows.InputOutput;

/**
 * One DatabaseConnectionMediator per DataObject is responsible for DatabaseConnections (such as what to take from pool,
 * transactions).
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
    public DatabaseConnectionAdapter getConnection();

    /**
     * Returns true is method {@link getDatabaseConnections()} returns a list of {@link DatabaseConnection}s based on
     * the project the DataObject contains in.
     *
     * @return a List of {@link DatabaseConnection}s either based on project or NetBeans.
     */
    public boolean hasProjectDatabaseConnections();

    /**
     * Returns a list of {@link DatabaseConnection}s. Either based on the project the DataObject contains in, or the
     * list of connections registered in NetBeans.
     *
     * Use {@link hasProjectDatabaseConnections()} to find out which.
     *
     * @return a List of {@link DatabaseConnection}s either based on project or NetBeans.
     */
    public List<DatabaseConnection> getDatabaseConnections();

    /**
     * Should be used when executing PLSQL or SQL.
     *
     * @return will return DatabaseConnectionExecutor with pooled connection if possible.
     */
    public DatabaseConnectionExecutor getExecutor();

    public boolean isConnected();

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

    public boolean isDefaultDatabase(DatabaseConnection connection);

    public static interface Factory {

        public DatabaseConnectionMediator create(FileObject fileObject);

        public DatabaseConnectionMediator create(DatabaseConnectionManager manager, DatabaseConnectionAdapter connection,
                FileObject fileObject);
    }
}
