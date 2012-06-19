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
package org.netbeans.modules.plsql.execution.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.swing.JFileChooser;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.modules.plsql.execution.PlsqlExecutableBlocksMaker;
import org.netbeans.modules.plsql.execution.PlsqlFileExecutor;
import org.netbeans.modules.plsql.utilities.PlsqlExecutorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

/**
 *
 * @author csamlk
 */
public abstract class DeployFilesAction extends CookieAction {

    private static final PlsqlExecutorService executorService = Lookup.getDefault().lookup(PlsqlExecutorService.class);
    private static final RequestProcessor RP = new RequestProcessor(DeployFilesAction.class);
    private Project project;
    private Node[] activatedNodes;

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[]{Project.class};
    }

    protected abstract File[] getFilesToDeploy();

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    protected void performAction(Node[] arg0) {
        File[] files = getFilesToDeploy();
        if (files != null && files.length > 0) {
            try {
                execute(files);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    @Override
    public abstract String getName();

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        this.activatedNodes = activatedNodes;
        if (!super.enable(activatedNodes)) {
            return false;
        }

        Project p = activatedNodes[0].getLookup().lookup(Project.class);

        return DatabaseConnectionManager.getInstance(p).isOnline();
    }

    static void execute(DatabaseConnection connection, DatabaseConnectionManager connectionProvider, File[] files, boolean isTemporyFile) throws IOException {
        if (connection == null || connection.getJDBCConnection() == null) {
            return;
        }
        RP.post(new MultipleDbFileExecutionHandler(connection, connectionProvider, files, isTemporyFile));
    }

    private void execute(File[] files) throws IOException {
        project = activatedNodes[0].getLookup().lookup(Project.class);

        DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(project);
        RP.post(new MultipleDbFileExecutionHandler(connectionProvider, files));
    }

    private static class DeploymentError implements OutputListener {

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

    private static class MultipleDbFileExecutionHandler implements Runnable, Cancellable {

        private PlsqlFileExecutor executor;
        private boolean cancelDeployment = false;
        private final DatabaseConnectionManager connectionProvider;
        private DatabaseConnection connection = null;
        private final File[] files;
        private boolean isTemporyFile = false;

        public MultipleDbFileExecutionHandler(DatabaseConnectionManager connectionProvider, File[] files) {
            this.connectionProvider = connectionProvider;
            this.connection = null;
            this.files = files;
        }

        public MultipleDbFileExecutionHandler(DatabaseConnection connection, DatabaseConnectionManager connectionProvider, File[] files, boolean isTemporyFile) {
            this.connectionProvider = connectionProvider;
            this.connection = connection;
            this.files = files;
            this.isTemporyFile = isTemporyFile;
        }

        @Override
        public void run() {
            List<String> executionOrder = executorService.getExecutionOrder();
            ProgressHandle handle = ProgressHandleFactory.createHandle("Deploying to database...", (Cancellable) this);
            cancelDeployment = false;
            //check if we're deploying to the "main" database. If so get a connection from the cache instead of using the "main" connection.
            if (connectionProvider != null && (connection == null || connection == connectionProvider.getTemplateConnection())) {
                connection = connectionProvider.getPooledDatabaseConnection(false);
            }
            try {
                handle.start();
                int count = 0;
                InputOutput io = IOProvider.getDefault().getIO("Deployment summary", false);
                io.getOut().reset();
                if (isTemporyFile) {
                    io.getOut().println("Deploying to database");
                } else {
                    io.getOut().println("Deploying database files");
                }
                executor = new PlsqlFileExecutor(connectionProvider, connection);
                for (int typeIndex = 0; !cancelDeployment && typeIndex < executionOrder.size(); typeIndex++) {
                    for (int i = 0; !cancelDeployment && i < files.length; i++) {
                        String fileName = files[i].getAbsolutePath();
                        DataObject obj = DataObject.find(FileUtil.toFileObject(files[i]));
                        if (obj != null) {
                            if (fileName.toLowerCase(Locale.ENGLISH).endsWith(executionOrder.get(typeIndex))) {
                                //Load the editor cookier and allow parsing
                                EditorCookie ec = obj.getCookie(EditorCookie.class);
                                Task task = ec.prepareDocument();
                                task.waitFinished();
                                Document doc = ec.getDocument();
                                PlsqlExecutableBlocksMaker blockMaker = new PlsqlExecutableBlocksMaker(doc);
                                final List exeBlocks = blockMaker.makeExceutableObjects();
                                if (isTemporyFile) {
                                    io.getOut().print("> Deploying...");
                                } else {
                                    io.getOut().print("> Deploying " + fileName + "...");
                                }
                                handle.setDisplayName("Deploying files to database (" + (++count) + ")");
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
                if (connectionProvider != null) {
                    connectionProvider.releaseDatabaseConnection(connection);
                }
                handle.finish();
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

    /*
     * Displays a file chooser dialog which can be used to browse and select arbitrary
     * files from disk to be deployed to the database.
     */
    
    public static class GenericDeployFilesAction extends DeployFilesAction {

        private static File lastLocation = null;

        @Override
        protected final File[] getFilesToDeploy() {
            final JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(true);
            fc.setCurrentDirectory(lastLocation);
            fc.setDialogTitle(NbBundle.getMessage(DeployFilesAction.class, "LBL_DeployDialogTitle"));
            fc.setApproveButtonText("OK");
            int returnVal = fc.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File[] files = fc.getSelectedFiles();
                lastLocation = fc.getCurrentDirectory();
                if (files != null) {
                    return files;
                }
            }
            return new File[0];
        }

        @Override
        public String getName() {
            return NbBundle.getMessage(DeployFilesAction.class, "CTL_DeployFilesAction");
        }
    }

    /*
     * Displays a dialog listing files currently open in the editor which makes it 
     * easy to select and deploy files to the database that are being worked on.
     */
    public static class DeployFilesOpenInEditorAction extends DeployFilesAction {

        @Override
        protected final File[] getFilesToDeploy() {
            OpenPlsqlFilesSelectorPanel editorSelector = new OpenPlsqlFilesSelectorPanel();
            DialogDescriptor openFilesDialog = new DialogDescriptor(editorSelector, NbBundle.getMessage(DeployFilesOpenInEditorAction.class, "LBL_DeployDialogTitle"));
            DialogDisplayer.getDefault().notify(openFilesDialog);
            if (openFilesDialog.getValue().equals(DialogDescriptor.OK_OPTION)) {
                List<File> filesToDeploy = editorSelector.getOpenFiles();
                if (!filesToDeploy.isEmpty()) {
                    return filesToDeploy.toArray(new File[filesToDeploy.size()]);
                }
            }
            return new File[0];
        }

        @Override
        public String getName() {
            return NbBundle.getMessage(DeployFilesAction.class, "CTL_DeployFilesOpenInEditorAction");
        }
    }
}
