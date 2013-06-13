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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.text.Document;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionExecutor;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObjectType;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.awt.*;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.*;
import org.openide.util.actions.CookieAction;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlExecuteAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlExecute", lazy = false)
@ActionReferences({
    @ActionReference(path = "Shortcuts", name = "DS-E"),
    @ActionReference(path = "Shortcuts", name = "OS-E"),
    @ActionReference(path = "Editors/text/x-plsql/Popup", name = "org-netbeans-modules-plsql-execution-PlsqlExecuteAction",
            position = 405, separatorBefore = 404),
    @ActionReference(path = "Editors/text/x-plsql/Toolbars/Default", name = "org-netbeans-modules-plsql-execution-PlsqlExecuteAction",
            position = 200)
})
public class PlsqlExecuteAction extends CookieAction {

    private static final Logger LOG = Logger.getLogger(PlsqlExecuteAction.class.getName());
    private static final String ICON_PATH = "org/netbeans/modules/plsql/execution/execute.png";
    private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
    private static final String TEST_BLOCK_NAME_PREFIX = "TestBlock:";
    private DataObject dataObject;

    public PlsqlExecuteAction() {
        super();
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(PlsqlExecuteAction.class, "CTL_PlsqlExecuteDescription"));
    }

    @Override
    public boolean isEnabled() {
        //Enable execution for .spec .body files in workspace (copied using 'Copy to Workspace Folder')
        if (dataObject != null) {
            if (validator.isValidPackageDefault(dataObject) || dataObject.getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH).equals("db")) {
                if (!dataObject.getPrimaryFile().canWrite()) {
                    return false;
                }
            }

            if (dataObject.getLookup().lookup(EditorCookie.class) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[]{DataObject.class};
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(PlsqlExecuteAction.class, "CTL_PlsqlExecute");
    }

    @Override
    protected String iconResource() {
        return ICON_PATH;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
        final DatabaseConnectionExecutor executor = dataObject.getLookup().lookup(DatabaseConnectionMediator.class).getExecutor();
        if (executor == null) {
            LOG.log(Level.FINE, "DatabaseConnectionExecutor is null");
            return;
        }
        saveIfModified(dataObject);

        EditorCookie edCookie = dataObject.getLookup().lookup(EditorCookie.class);
        Document document = edCookie.getDocument();

        PlsqlExecutableBlocksMaker blockMaker = new PlsqlExecutableBlocksMaker(document);
        List<PlsqlExecutableObject> blocks = blockMaker.makeExceutableObjects();

        //if the user has selected any text in the window, create exec block using selected text only
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
                    } else {
                        blocks = new ArrayList<PlsqlExecutableObject>();
                        blocks.add(new PlsqlExecutableObject(0, selectedSql, "SQL", PlsqlExecutableObjectType.STATEMENT, 0, selectedSql.length() - 1));
                    }
                } else if (OptionsUtilities.isCommandWindowAutoSelectEnabled()) {
                    List<PlsqlExecutableObject> newblocks = new ArrayList<PlsqlExecutableObject>();

                    int caretPos = 0;
                    if (panes.length > 0) {
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
            if (blocks.size() > 0 && validator.isValidTDB(dataObject) && (dataObject.getNodeDelegate().getDisplayName() == null || !dataObject.getNodeDelegate().getDisplayName().contains(TEST_BLOCK_NAME_PREFIX))) {
                String str = blocks.get(0).getPlsqlString().replaceAll("\n", " ");
                dataObject.getNodeDelegate().setDisplayName(str.length() > 30 ? str.substring(0, 30) + "..." : str);
            }
        executor.execute(blocks, document);
    }

    private void saveIfModified(DataObject dataObj) {
        try {
            SaveCookie saveCookie = dataObj.getLookup().lookup(SaveCookie.class);
            if (saveCookie != null) {
                saveCookie.save();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}