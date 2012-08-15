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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.plsql.utilities.PlsqlExecutorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlMultipleExecuteAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlMultipleExecuteAction")
@ActionReference(path = "Loaders/text/x-plsql/Actions", position = 250)
public final class PlsqlMultipleExecuteAction extends CookieAction {

    private static final List<String> INVALID_EXTENSIONS = Arrays.asList(new String[]{"dbl"});
    private static final String DATABASE_CONNECTION_KEY = "databaseConnection";
    private static final PlsqlExecutorService executorService = Lookup.getDefault().lookup(PlsqlExecutorService.class);
    private DatabaseConnection connection;
    private Node[] activatedNodes;
    private Project project;
    private DataObject dataObject;

    @Override
    protected boolean enable(Node[] activatedNodes) {
        this.activatedNodes = activatedNodes;
        if (!super.enable(activatedNodes)) {
            return false;
        }

        for (Node node : activatedNodes) {
            if (INVALID_EXTENSIONS.contains(node.getLookup().lookup(DataObject.class).getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH))) {
                return false;
            }
        }

        project = FileOwnerQuery.getOwner(activatedNodes[0].getLookup().lookup(DataObject.class).getPrimaryFile());
        dataObject = project == null && activatedNodes.length == 1 ? activatedNodes[0].getLookup().lookup(DataObject.class) : null;
        if (project == null && activatedNodes.length > 1) {
            return false;
        }
        if (project != null && project.getLookup().lookup(DatabaseConnectionManager.class) == null) {
            return false;
        }
        for (int i = 1; i < activatedNodes.length; i++) {
            Project p = FileOwnerQuery.getOwner(activatedNodes[i].getLookup().lookup(DataObject.class).getPrimaryFile());
            if (p != project) {
                return false;
            }
        }

        return true;
    }

    private void execute() {
        DatabaseConnectionManager connectionProvider = project != null
                ? DatabaseConnectionManager.getInstance(project)
                : DatabaseConnectionManager.getInstance(dataObject);
        connectionProvider.connect(connection);
        if (connection.getJDBCConnection() == null) {
            return;
        }

        RequestProcessor processor = RequestProcessor.getDefault();
        processor.post(new ExecutionHandler(connectionProvider, connection, activatedNodes));
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        execute();
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_ALL;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(PlsqlMultipleExecuteAction.class, "CTL_PlsqlMultipleExecuteAction");
    }

    @Override
    protected Class[] cookieClasses() {
        return new Class[]{DataObject.class};
    }

    @Override
    protected void initialize() {
        super.initialize();
        putValue("noIconInMenu", Boolean.TRUE);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    private String formatDbConnectionName(DatabaseConnection connection) {
        if (connection != null) {
            String url = connection.getDatabaseURL();
            String schema = connection.getUser();
            int pos = url.indexOf("@") + 1;
            if (pos > 0) {
                url = url.substring(pos);
            }
            url = schema + "@" + url;
            String alias = connection.getDisplayName();
            if (alias != null && !alias.equals(connection.getName())) {
                url = alias + " [" + url + "]";
            }
            return url;
        }
        return "Unknown database";
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (project == null) {
            return super.getPopupPresenter(); //NOPMD
        }

        DatabaseConnectionManager connectionProvider = project.getLookup().lookup(DatabaseConnectionManager.class);

        JMenu menu = new JMenu(getName());
        ActionListener buttonListener = new ButtonListener();
        List<DatabaseConnection> databaseConnections = connectionProvider.getDatabaseConnections();
        for (int i = 0; i < databaseConnections.size(); i++) {
            JMenuItem item = new JMenuItem(formatDbConnectionName(databaseConnections.get(i)));
            item.putClientProperty(DATABASE_CONNECTION_KEY, databaseConnections.get(i));
            item.addActionListener(buttonListener);
            menu.add(item);
            if (i == 0 && databaseConnections.size() > 1) {
                menu.add(new JSeparator());
            }
        }
        return menu;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    private class DeploymentError implements OutputListener {

        private InputOutput errorTab;

        public DeploymentError(InputOutput errorTab) {
            this.errorTab = errorTab;
        }

        @Override
        public void outputLineSelected(OutputEvent arg0) {
            //do nothing
        }

        @Override
        public void outputLineAction(OutputEvent arg0) {
            if (errorTab != null && !errorTab.isClosed()) {
                errorTab.select();
            }
        }

        @Override
        public void outputLineCleared(OutputEvent arg0) {
            //do nothing
        }
    }

    private class ExecutionHandler implements Runnable, Cancellable {

        private DatabaseConnectionManager connectionProvider;
        private DatabaseConnection connection;
        private Node[] activatedNodes;
        private PlsqlFileExecutor executor;
        private boolean cancelDeployment = false;

        public ExecutionHandler(DatabaseConnectionManager connectionProvider, DatabaseConnection connection, Node[] activatedNodes) {
            this.connectionProvider = connectionProvider;
            this.connection = connection;
            this.activatedNodes = activatedNodes;
        }

        @Override
        public void run() {
            List<String> executionOrder = executorService.getExecutionOrder();
            ProgressHandle handle = ProgressHandleFactory.createHandle("Deploying files to database...", (Cancellable) this);
            cancelDeployment = false;
            try {
                if (connectionProvider != null && connectionProvider.isDefaultDatabase(connection)) { //only use pooled connections for the "main" database.
                    connection = connectionProvider.getPooledDatabaseConnection(false);
                } else {
                    if (!OptionsUtilities.isDeployNoPromptEnabled()) {
                        String msg = "You are now connecting to a secondary database.";
                        String title = "Connecting to a Secondary Database!";
                        if (JOptionPane.showOptionDialog(null,
                                msg,
                                title,
                                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                                null, null, null) == JOptionPane.NO_OPTION) {
                            cancelDeployment = true;
                        }
                    }
                }

                handle.start();
                int count = 0;
                InputOutput io = IOProvider.getDefault().getIO("File deployment summary", false);
                io.getOut().reset();
                if (cancelDeployment) {
                    io.getOut().println("Canceled deploying database files");
                } else {
                    io.getOut().println("Deploying " + activatedNodes.length + " database files");
                    executor = new PlsqlFileExecutor(connectionProvider, connection);
                }
                for (int typeIndex = 0; !cancelDeployment && typeIndex < executionOrder.size(); typeIndex++) {
                    for (int i = 0; !cancelDeployment && i < activatedNodes.length; i++) {
                        DataObject obj = activatedNodes[i].getLookup().lookup(DataObject.class);
                        if (obj != null) {
                            String fileName = obj.getPrimaryFile().getNameExt();
                            if (fileName.toLowerCase(Locale.ENGLISH).endsWith(executionOrder.get(typeIndex))) {
                                //Load the editor cookier and allow parsing
                                EditorCookie ec = obj.getCookie(EditorCookie.class);
                                Document doc = null;
                                try {
                                    doc = ec.openDocument();
                                } catch (UserQuestionException uqe) {
                                    uqe.confirmed();
                                    doc = ec.openDocument();
                                }
                                saveIfModified(obj);
                                PlsqlExecutableBlocksMaker blockMaker = new PlsqlExecutableBlocksMaker(doc);
                                final List exeBlocks = blockMaker.makeExceutableObjects();
                                io.getOut().print("> Deploying " + fileName + "...");
                                handle.setDisplayName("Deploying files to database (" + (++count) + "/" + activatedNodes.length + ")");
                                InputOutput errorTab = executor.executePLSQL(exeBlocks, doc, true, true);
                                if (errorTab == null) {
                                    io.getOut().println("Ok");
                                } else {
                                    io.getOut().println("Failed");
                                    try {
                                        io.getErr().println("!!!Error deploying file " + fileName, new DeploymentError(errorTab));
                                    } catch (IOException ex) {
                                        Exceptions.printStackTrace(ex);
                                    }
                                    cancelDeployment = true;
                                }
                            }
                        }
                    }
                }
                io.getOut().close();
                io.getErr().close();
                io.select();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                handle.finish();
                if (connectionProvider != null) {
                    connectionProvider.releaseDatabaseConnection(connection);
                }
            }
        }

        /**
         * Check whether this data object is modified if so save the object
         *
         * @param dataObj
         */
        private void saveIfModified(DataObject dataObj) {
            if (dataObj instanceof DataObject) {
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

        @Override
        public boolean cancel() {
            if (executor != null) {
                executor.cancel();
                cancelDeployment = true;
            }
            return true;
        }
    }

    private class ButtonListener implements ActionListener {

        public ButtonListener() {
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem) e.getSource();
            connection = (DatabaseConnection) item.getClientProperty(DATABASE_CONNECTION_KEY);
            execute();
        }
    }
}
