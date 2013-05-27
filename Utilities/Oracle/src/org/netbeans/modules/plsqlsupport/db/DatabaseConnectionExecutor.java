package org.netbeans.modules.plsqlsupport.db;

import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.filesystems.FileObject;

/**
 *
 * @author chrlse
 */
public interface DatabaseConnectionExecutor {

    public boolean updateConnection(DatabaseConnection databaseConnection);

    public DatabaseConnection getConnection();

    public boolean closeOpenTransaction();

    public void execute(List<PlsqlExecutableObject> blocks, Document document);

    public void addTransactionListener(PropertyChangeListener changeListener);

    public void commitTransaction();

    public void rollbackTransaction();

    public static interface Factory {

        public DatabaseConnectionExecutor create(FileObject fileObject);

        public DatabaseConnectionExecutor create(DatabaseConnectionManager connectionProvider, DatabaseConnection connection, FileObject fileObject);
    }
}
