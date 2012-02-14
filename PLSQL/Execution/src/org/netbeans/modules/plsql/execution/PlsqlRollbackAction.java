/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.netbeans.modules.plsql.execution;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DropDownButtonFactory;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.loaders.DataObject;
import org.openide.util.*;
import org.openide.util.actions.Presenter;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlRollbackAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlRollback")
public class PlsqlRollbackAction extends AbstractAction implements ContextAwareAction, Presenter.Toolbar {

    private static final List<String> EXTENSIONS = Arrays.asList(new String[]{"tdb"});
    private DataObject dataObject;
    private DatabaseConnectionManager connectionProvider;
    private JButton button;
    public boolean autoCommit = true;
    private DatabaseConnection connection;

    public PlsqlRollbackAction() {
        this(Utilities.actionsGlobalContext());

    }

    public PlsqlRollbackAction(Lookup context) {
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(PlsqlRollbackAction.class, "CTL_PlsqlRollback"));
        putValue(SMALL_ICON, new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/execution/database_rollback.png")));

        dataObject = context.lookup(DataObject.class);

        //Enable execution for .spec .body files in workspace (copied using 'Copy to Workspace Folder')
        if (dataObject != null && (dataObject.getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH).equals("spec")
                || dataObject.getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH).equals("body")
                || dataObject.getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH).equals("db"))) {
            if (!dataObject.getPrimaryFile().canWrite()) {
                dataObject = null;
            }
        } else if (dataObject != null && !EXTENSIONS.contains(dataObject.getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH))) {
            dataObject = null;
        }

        if (dataObject != null && dataObject.getLookup().lookup(EditorCookie.class) == null) {
            dataObject = null;
        }

        if (dataObject != null) {
            setEnabled(true);
            autoCommit = OptionsUtilities.isCommandWindowAutoCommitEnabled();
        } else {
            setEnabled(false);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PlsqlRollbackAction(context);

    }

    private void prepareConnection() {
        if (dataObject != null) {
            connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
        }
        connection = dataObject.getLookup().lookup(DatabaseConnection.class);
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        prepareConnection();
        if (connectionProvider == null || connection == null) {
            return;
        }

        if (!connectionProvider.hasDataToCommit(connection)) {
            return;
        }

        EditorCookie edCookie = dataObject.getLookup().lookup(EditorCookie.class);
        Document document = edCookie.getDocument();
        saveIfModified(dataObject);

        InputOutput io = null;
        DataObject obj = FileExecutionUtil.getDataObject(document);
        ProgressHandle handle = ProgressHandleFactory.createHandle("Commit database file...", this);
        handle.start();

        try {
            io = IOProvider.getDefault().getIO(obj.getPrimaryFile().getNameExt(), false);
            if (!io.isClosed()) {
                io.getOut().println((new StringBuilder()).append("> Rollback Statement successfully"));
            }

            if (connection.getJDBCConnection() != null) {
                connectionProvider.commitRollbackTransactions(connection, false);
            }

        } catch (Exception ex) {
            io.getOut().println((new StringBuilder()).append(">!!! Error Rollback Statement "));
            Exceptions.printStackTrace(ex);
        } finally {
            handle.finish();
        }
    }

    @Override
    public Component getToolbarPresenter() {
        if (!isEnabled()) {
            return null;
        }
        button = DropDownButtonFactory.createDropDownButton(
                new ImageIcon(new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY)), null);
        button.setAction(this);
        button.setSelected(!OptionsUtilities.isCommandWindowAutoCommitEnabled());
        button.setEnabled(!OptionsUtilities.isCommandWindowAutoCommitEnabled());
        button.setDisabledIcon(new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/execution/database_rollback_disable.png")));
        return button;
    }

    private void saveIfModified(DataObject dataObj) {
        try {
            SaveCookie saveCookie = dataObj.getCookie(SaveCookie.class);
            if (saveCookie != null) {
                saveCookie.save();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
