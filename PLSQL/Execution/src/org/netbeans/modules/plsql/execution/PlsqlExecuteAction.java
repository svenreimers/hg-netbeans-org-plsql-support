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

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsql.filetype.PlsqlDataObject;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.awt.DropDownButtonFactory;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Cancellable;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

public class PlsqlExecuteAction extends AbstractAction implements ContextAwareAction, Presenter.Toolbar {

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

    public PlsqlExecuteAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PlsqlExecuteAction(Lookup context) {
        putValue(NAME, NbBundle.getMessage(PlsqlExecuteAction.class, "CTL_fileExecution"));
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(PlsqlExecuteAction.class, "CTL_fileExecution"));
        putValue(SMALL_ICON, new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/execution/execute.png")));

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
            connection = dataObject.getLookup().lookup(DatabaseConnection.class);
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
        if (autoCommit) {
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

        if (autoCommit && !connectionProvider.isDefaultDatabase(connection)) {
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
                connectionProvider.connect(connection);
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
            //to reconnect if the connection is gone. 
            if (connection.getJDBCConnection() == null) {
                connectionProvider.connect(connection);
            }
        }

        //if the user has selected any text in the window, create exec block using selected text only
        if (validator.isValidTDB(dataObject)) {
            JEditorPane[] panes = edCookie.getOpenedPanes();
            if ((panes != null) && (panes.length > 0)) {
                String selectedSql = panes[0].getSelectedText();
                if (selectedSql != null && !selectedSql.trim().equals("")) { //some text has been selected
                    //create executable block with selected sql
                    blocks = new ArrayList<PlsqlExecutableObject>();
                    blocks.add(new PlsqlExecutableObject(0, selectedSql, "SQL", PlsqlExecutableObjectType.STATEMENT, 0, selectedSql.length() - 1));
                }
            }
        }

        //if blocks were not created using selected text, use entire document to create exec blocks
        if (blocks == null) {
            PlsqlExecutableBlocksMaker blockMaker = new PlsqlExecutableBlocksMaker(document);
            blocks = blockMaker.makeExceutableObjects();
        }
        String extension = file.getExt();
        if (blocks.size() > 0 && "tdb".equalsIgnoreCase(extension) && (dataObject.getNodeDelegate().getDisplayName() == null || !dataObject.getNodeDelegate().getDisplayName().contains(TEST_BLOCK_NAME_PREFIX))) {
            String str = blocks.get(0).getPlsqlString().replaceAll("\n", " ");
            dataObject.getNodeDelegate().setDisplayName(str.length() > 30 ? str.substring(0, 30) + "..." : str);
        }
        RequestProcessor processor = RequestProcessor.getDefault();
        processor.post(new ExecutionHandler(connectionProvider, connection, blocks, document));

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
            connection = (DatabaseConnection) item.getClientProperty(DATABASE_CONNECTION_KEY);
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
