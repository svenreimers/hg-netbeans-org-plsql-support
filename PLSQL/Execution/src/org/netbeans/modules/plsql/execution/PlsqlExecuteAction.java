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
package org.netbeans.modules.plsql.execution;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.plsql.filetype.PlsqlDataObject;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.awt.*;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlExecuteAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_Execution", iconBase = "org/netbeans/modules/plsql/execution/execute.png")
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "DS-E"),
    @ActionReference(path = "Shortcuts", name = "OS-E"),
    @ActionReference(path = "Editors/text/x-plsql/Popup", name = "org-netbeans-modules-plsql-execution-PlsqlExecuteAction",
    position = 405, separatorBefore = 404)
})
public class PlsqlExecuteAction extends AbstractAction implements ContextAwareAction, Presenter.Toolbar {

    private static final String ICON_PATH = "org/netbeans/modules/plsql/execution/execute.png";
    private static final RequestProcessor RP = new RequestProcessor(PlsqlExecuteAction.class);
    private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
    private static final String DATABASE_CONNECTION_KEY = "databaseConnection";
    private static final String TEST_BLOCK_NAME_PREFIX = "TestBlock:";
    private DataObject dataObject;
    private PlsqlDataObject plsqlDataobject;
    private DatabaseConnectionManager connectionProvider;
    private DatabaseConnection connection;
    private PopupMenuPopulator popupMenuPopulator = null;
    private JPopupMenu popup;
    private JButton button;
    private ActionListener buttonListener = new ButtonListener();
    private boolean autoCommit = true;
    PlsqlCommit commit;

    public PlsqlExecuteAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PlsqlExecuteAction(Lookup context) {
        putValue(NAME, NbBundle.getMessage(PlsqlExecuteAction.class, "CTL_Execution"));
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(PlsqlExecuteAction.class, "CTL_ExecutionDescription"));
//        putValue(LONG_DESCRIPTION, NbBundle.getMessage(PlsqlExecuteAction.class, "CTL_ExecutionDescription"));
        putValue(SMALL_ICON, new ImageIcon(ImageUtilities.loadImage(ICON_PATH)));

        dataObject = context.lookup(DataObject.class);

        //Enable execution for .spec .body files in workspace (copied using 'Copy to Workspace Folder')
        if (dataObject != null && (validator.isValidPackageDefault(dataObject)
                || dataObject.getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH).equals("db"))) {
            if (!dataObject.getPrimaryFile().canWrite()) {
                dataObject = null;
            }
        }

        if (dataObject != null && dataObject.getLookup().lookup(EditorCookie.class) == null) {
            dataObject = null;
        }

        setEnabled(dataObject != null);
        if (validator.isValidTDB(dataObject)) {
            autoCommit = OptionsUtilities.isCommandWindowAutoCommitEnabled();
        }
        if (dataObject != null) {
            commit = PlsqlCommit.getInstance(dataObject);
        }
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PlsqlExecuteAction(context);
    }

    private void prepareConnection() {
        if (dataObject != null) {
            connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
            if (connectionProvider != null) {
                if (popupMenuPopulator == null) {
                    popupMenuPopulator = new PopupMenuPopulator();
                    connectionProvider.addPropertyChangeListener(popupMenuPopulator);
                }
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        if (connectionProvider == null) {
            prepareConnection();
        }
        if (connectionProvider == null) {
            return;
        }

        // If autocommit OFF - take the connection from data object.
        if (!autoCommit) {
            setConnection(connectionProvider.getTemplateConnection());
        }

        if (connection == null) {
            connection = connectionProvider.getTemplateConnection();
        }

        if (connection == null) {
            return;
        }
        saveAndExecute();
    }

    @Override
    public Component getToolbarPresenter() {
        if (!isEnabled()) {
            return null;
        }

        popup = new JPopupMenu();
        if (connectionProvider == null) {
            prepareConnection();
        }

        if (connectionProvider != null) {
            populatePopupMenu();
        }

        button = DropDownButtonFactory.createDropDownButton(
                new ImageIcon(new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY)), popup);
        button.setAction(this);
        button.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    popup.show(button, 0, button.getHeight());
                }
            }
        });

        popup.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                button.setSelected(false);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                button.setSelected(false);
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });

        return button;
    }

    private void populatePopupMenu() {
        popup.removeAll();
        for (DatabaseConnection c : connectionProvider.getDatabaseConnections()) {
            String url = c.getDatabaseURL();
            String schema = c.getUser();
            int pos = url.indexOf("@") + 1;
            if (pos > 0) {
                url = url.substring(pos);
            }
            url = schema + "@" + url;
            String alias = c.getDisplayName();
            if (alias != null && !alias.equals(c.getName())) {
                url = alias + " [" + url + "]";
            }
            JMenuItem item = new JMenuItem(url);
            item.putClientProperty(DATABASE_CONNECTION_KEY, c);
            item.addActionListener(buttonListener);
            popup.add(item);
        }
    }

    private void setConnection(DatabaseConnection newConnection) {
        if (connection != null && connection.getName().equals(newConnection.getName())) {
            connection = dataObject.getLookup().lookup(DatabaseConnection.class);
        } else {
            if (connection != null) {
                connection = dataObject.getLookup().lookup(DatabaseConnection.class);
                if (commit.getCommit()) {
                    if (!OptionsUtilities.isDeployNoPromptEnabled()) {

                        String msg = "Commit transactions for " + connection.getDisplayName() + " ?";
                        String title = "Confirm!";
                        int showOptionDialog = JOptionPane.showOptionDialog(null,
                                msg,
                                title,
                                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                                null, null, null);

                        if (showOptionDialog == JOptionPane.YES_OPTION) {
                            commit.commitTransaction(dataObject, connection, connectionProvider);
                        } else if (showOptionDialog == JOptionPane.NO_OPTION) {
                            commit.rollbackTransaction(dataObject, connection, connectionProvider);
                        } else {
                            return;
                        }
                    }
                }
            }
            if (!connectionProvider.isDefaultDatabase(newConnection)) {
                if (!OptionsUtilities.isDeployNoPromptEnabled()) {
                    String msg = "You are now connecting to a secondary database.";
                    String title = "Connecting to a Secondary Database!";
                    if (JOptionPane.showOptionDialog(null,
                            msg,
                            title,
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                            null, null, null) == JOptionPane.NO_OPTION) {
                        return;
                    }
                    connection = newConnection;
                    try {
                        Connection jdbcConnection = connection.getJDBCConnection();
                        if (jdbcConnection == null || !jdbcConnection.isValid(1000)) {
                            return;
                        }
                    } catch (SQLException ex) {
                        return;
                    }
                }
            } else {
                connection = newConnection;
            }
        }
    }

    public void saveAndExecute() {
        if (connectionProvider == null) {
            prepareConnection();
        }

        EditorCookie edCookie = dataObject.getLookup().lookup(EditorCookie.class);
        Document document = edCookie.getDocument();
        saveIfModified(dataObject);
        List<PlsqlExecutableObject> blocks = null;

        DataObject obj = FileExecutionUtil.getDataObject(document);
        FileObject file = obj.getPrimaryFile();
        if (file == null) {
            return;
        }
        //to reconnect if the connection is gone. 
        if (connection.getJDBCConnection() == null) {
            connectionProvider.connect(connection);
        }

        PlsqlExecutableBlocksMaker blockMaker = new PlsqlExecutableBlocksMaker(document);
        blocks = blockMaker.makeExceutableObjects();
                
        //if the user has selected any text in the window, create exec block using selected text only
        if (validator.isValidTDB(dataObject)) {
            JEditorPane[] panes = edCookie.getOpenedPanes();
            if ((panes != null) && (panes.length > 0)) {
                String selectedSql = panes[0].getSelectedText();
                if (selectedSql != null && !selectedSql.trim().equals("")) { //some text has been selected
                    //create executable block with selected sql
                    List<PlsqlExecutableObject> newblocks = new ArrayList<PlsqlExecutableObject>();
                    int selectionStart = panes[0].getSelectionStart();
                    int selectionEnd = panes[0].getSelectionEnd();
                    for (PlsqlExecutableObject block : blocks) {
                        if ((selectionStart <= block.getStartOffset()) && (selectionEnd >= block.getEndOffset())) {
                            newblocks.add(block);
                        }
                    }
                    if (!newblocks.isEmpty()) {
                        blocks = newblocks;
                    }else {
                        blocks = new ArrayList<PlsqlExecutableObject>();
                        blocks.add(new PlsqlExecutableObject(0, selectedSql, "SQL", PlsqlExecutableObjectType.STATEMENT, 0, selectedSql.length() - 1));
                    }
                }else if (OptionsUtilities.isCommandWindowAutoSelectEnabled()) {
                    List<PlsqlExecutableObject> newblocks = new ArrayList<PlsqlExecutableObject>();

                    int caretPos = 0;
                    if ((panes != null) && (panes.length > 0)) {
                        caretPos = panes[0].getCaretPosition();
                    }
                    for (PlsqlExecutableObject block : blocks) {
                        if (caretPos >= block.getStartOffset() && caretPos <= block.getEndOffset()) {
                            if (block.getPlsqlString().startsWith("SELECT")) {
                                newblocks.add(block);
                            }
                        }
                    }
                    if (!newblocks.isEmpty()) {
                        blocks = newblocks;
                    }
                }
            }
        }
        String extension = file.getExt();
        if (blocks.size() > 0 && "tdb".equalsIgnoreCase(extension) && (dataObject.getNodeDelegate().getDisplayName() == null || !dataObject.getNodeDelegate().getDisplayName().contains(TEST_BLOCK_NAME_PREFIX))) {
            String str = blocks.get(0).getPlsqlString().replaceAll("\n", " ");
            dataObject.getNodeDelegate().setDisplayName(str.length() > 30 ? str.substring(0, 30) + "..." : str);
        }
        RP.post(new ExecutionHandler(connectionProvider, connection, blocks, document));

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

    private class ExecutionHandler implements Runnable, Cancellable {

        private DatabaseConnectionManager connectionProvider;
        private DatabaseConnection connection;
        private List<PlsqlExecutableObject> blocks;
        private Document document;
        private PlsqlFileExecutor executor;

        public ExecutionHandler(DatabaseConnectionManager connectionProvider, DatabaseConnection connection,
                List<PlsqlExecutableObject> blocks, Document doc) {
            this.connectionProvider = connectionProvider;
            this.connection = connection;
            this.blocks = blocks;
            this.document = doc;
        }

        @Override
        public void run() {
            ProgressHandle handle = ProgressHandleFactory.createHandle("Executing database file...", this);
            DataObject obj = null;
            try {
                handle.start();
                if (connection == connectionProvider.getTemplateConnection()) {
                    connection = connectionProvider.getPooledDatabaseConnection(false, true);
                    if (connection == null) {
                        return;
                    }
                }

                obj = FileExecutionUtil.getDataObject(document);
                FileObject file = obj.getPrimaryFile();
                if (file == null) {
                    return;
                }

                executor = new PlsqlFileExecutor(connectionProvider, connection);
                executor.executePLSQL(blocks, document, false, autoCommit);

            } finally {
                if (autoCommit) {
                    connectionProvider.releaseDatabaseConnection(connection);
                } else {
                    //set connection to the lookup when autocommit is OFF
                    modifyConnection();
                }
                handle.finish();
            }
        }

        @Override
        public boolean cancel() {
            if (executor != null) {
                executor.cancel();
            }
            return true;
        }

        private void modifyConnection() {

            plsqlDataobject = (PlsqlDataObject) dataObject;
            plsqlDataobject.modifyLookupDatabaseConnection(connection);
            dataObject = plsqlDataobject;
        }
    }

    private class ButtonListener implements ActionListener {

        public ButtonListener() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {

            JMenuItem item = (JMenuItem) e.getSource();
            DatabaseConnection newConnection = (DatabaseConnection) item.getClientProperty(DATABASE_CONNECTION_KEY);
            setConnection(newConnection);
            saveAndExecute();

        }
    };

    private class PopupMenuPopulator implements PropertyChangeListener {

        public PopupMenuPopulator() {
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (popup != null) {
                prepareConnection();
                populatePopupMenu();
            }
        }
    }
}