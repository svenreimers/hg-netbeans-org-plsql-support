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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;

import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.spi.project.CacheDirectoryProvider;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

@ActionID(id = "org.netbeans.modules.plsql.execution.actions.DeploySelectedCodeAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_DeploySelectedCodeAction")
@ActionReference(path = "Editors/text/x-plsql/Popup", name = "org-netbeans-modules-plsql-execution-action-DeploySelectedCodeAction", position = 280)
public final class DeploySelectedCodeAction extends CookieAction {

    private Node[] activatedNodes;
    private DataObject dataObject = null;
    private Project project = null;
    private DatabaseConnection connection = null;
    private static final String DATABASE_CONNECTION_KEY = "databaseConnection";
    private final String TEMP_SQL_FILE_PREFIX = "Tempory";

    /**
     * Create a sql execution window for the selected methoad
     * @param activatedNodes
     */
    @Override
    protected void performAction(Node[] activatedNodes) {
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(DeploySelectedCodeAction.class, "CTL_DeploySelectedCodeAction");
    }

    @Override
    protected Class[] cookieClasses() {
        return new Class[]{DataObject.class, EditorCookie.class};
    }

    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    /**
     * Enable this action when right clicked on procedures or functions
     * @param arg0
     * @return
     */
    @Override
    protected boolean enable(Node[] activatedNodes) {
        this.activatedNodes = activatedNodes;
        if (!super.enable(activatedNodes)) {
            return false;
        }

        EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
        if (editorCookie == null) {
            return false;
        }

        int offset = -1;
        int start = -1;
        int end = -1;

        JEditorPane[] panes = editorCookie.getOpenedPanes();
        if ((panes != null) && (panes.length != 0)) {
            Caret caret = panes[0].getCaret();
            offset = caret.getDot();
            start = Math.min(caret.getDot(), caret.getMark());
            end = Math.max(caret.getDot(), caret.getMark());
        }

        //If we are able to take the selected position get data object and get the block factory
        if (offset != -1) {
            dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
            if (dataObject != null) {
                project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
                if (project == null || DatabaseConnectionManager.getInstance(project) == null) {
                    return false;
                }
                if ((end - start) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public JMenuItem getPopupPresenter() {
        if (project == null) {
            return super.getPopupPresenter();
        }

        DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);

        JMenu menu = new JMenu(getName());
        ActionListener buttonListener = new ButtonListener();
        DatabaseConnection[] databaseConnections = connectionProvider.getDatabaseConnections();
        for (int i = 0; i < databaseConnections.length; i++) {
            JMenuItem item = new JMenuItem(databaseConnections[i].getName());
            item.putClientProperty(DATABASE_CONNECTION_KEY, databaseConnections[i]);
            item.addActionListener(buttonListener);
            menu.add(item);
            if (i == 0 && databaseConnections.length > 1) {
                menu.add(new JSeparator());
            }
        }
        return menu;
    }

    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem) e.getSource();
            String output = "";
            connection = (DatabaseConnection) item.getClientProperty(DATABASE_CONNECTION_KEY);
            DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(project);
            EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
            try {
                output = replaceAliases(getSelection(editorCookie), dataObject.getLookup().lookup(PlsqlBlockFactory.class), '&');
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
            File tmpFile = null;
            try {
                tmpFile = File.createTempFile(TEMP_SQL_FILE_PREFIX, ".sql",
                        FileUtil.toFile(project.getLookup().lookup(CacheDirectoryProvider.class).getCacheDirectory()));
                tmpFile.deleteOnExit();
                FileWriter writer = new FileWriter(tmpFile);
                writer.write(output);
                writer.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
            File[] files = {tmpFile};
            try {
                DeployFilesAction.execute(connection, connectionProvider, files, true);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        private String getSelection(EditorCookie editorCookie) throws BadLocationException {
            JEditorPane[] panes = editorCookie.getOpenedPanes();
            Document document = editorCookie.getDocument();
            String selection = "";
            if ((panes != null) && (panes.length != 0)) {
                Caret caret = panes[0].getCaret();
                int start = Math.min(caret.getDot(), caret.getMark());
                int end = Math.max(caret.getDot(), caret.getMark());
                selection = document.getText(start, (end - start));
            }
            return selection;
        }

        private String replaceAliases(String plsqlString, PlsqlBlockFactory blockFac, char define) {
            if (plsqlString.indexOf(define) < 0) {
                return plsqlString;
            }

            StringBuilder newString = new StringBuilder();
            for (int i = 0; i < plsqlString.length(); i++) {
                char c = plsqlString.charAt(i);
                if (c == define) {
                    for (int j = i + 1; j < plsqlString.length(); j++) {
                        char nextChar = plsqlString.charAt(j);
                        if (Character.isJavaIdentifierPart(nextChar) && j == plsqlString.length() - 1) { //we have reached the end of the text
                            nextChar = '.'; //this will make sure that the correct sustitution is made below by emulating an additional character
                            j = j + 1;
                        }
                        if (!Character.isJavaIdentifierPart(nextChar)) { //potential end of substitutionvariable
                            if (j > i + 1) { //substituion variable found
                                String name = plsqlString.substring(i, j);
                                String value = blockFac.getDefine(name);
                                newString.append(value);
                                if (nextChar == '.') {
                                    i = j;
                                } else {
                                    i = j - 1;
                                }
                            } else {
                                newString.append(c);
                            }
                            break;
                        }
                    }
                } else {
                    newString.append(c);
                }
            }
            return newString.toString();
        }
    }
}
