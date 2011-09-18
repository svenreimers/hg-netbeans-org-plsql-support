/*
 * To change this template, choose Tools | Templates and open the template in
 * the editor.
 */
package org.netbeans.modules.plsql.filetype;

import java.io.IOException;
import org.netbeans.core.spi.multiview.MultiViewElement;
import org.netbeans.core.spi.multiview.text.MultiViewEditorElement;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.loaders.MultiFileLoader;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;

public class PrototypeDataObject extends MultiDataObject {

    public PrototypeDataObject(FileObject pf, MultiFileLoader loader) throws DataObjectExistsException, IOException {
        super(pf, loader);
        registerEditor("text/x-plsql-prototype", true);
    }

    @Override
    protected int associateLookup() {
        return 1;
    }

    @MultiViewElement.Registration(displayName = "#LBL_Prototype_EDITOR",
    iconBase = "SET/PATH/TO/ICON/HERE",
    mimeType = "text/x-plsql-prototype",
    persistenceType = TopComponent.PERSISTENCE_ONLY_OPENED,
    preferredID = "Prototype",
    position = 1000)
    public static MultiViewEditorElement createEditor(Lookup lkp) {
        return new MultiViewEditorElement(lkp);
    }
}
