package org.netbeans.modules.plsqlsupport.db;

import java.util.List;
import javax.swing.text.Document;

/**
 * DatabaseConnectionExecutor is responsible for executing SQL and other NB UI such as progress bar and threading.
 *
 * @author chrlse
 */
public interface DatabaseConnectionExecutor {

    public void execute(List<PlsqlExecutableObject> blocks, Document document);
}
