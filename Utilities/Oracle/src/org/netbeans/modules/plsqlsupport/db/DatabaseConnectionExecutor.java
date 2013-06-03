package org.netbeans.modules.plsqlsupport.db;

import java.util.List;
import javax.swing.text.Document;
import org.openide.loaders.DataObject;

/**
 *
 * @author chrlse
 */
public interface DatabaseConnectionExecutor {

    public void execute(List<PlsqlExecutableObject> blocks, Document document);
}
