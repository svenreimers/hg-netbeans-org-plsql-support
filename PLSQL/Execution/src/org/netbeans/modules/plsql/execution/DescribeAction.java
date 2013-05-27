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

import java.util.ArrayList;
import java.util.List;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObjectType;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.CookieAction;

/**
 *
 * @author YADHLK
 */
@ActionID(id = "org.netbeans.modules.plsql.execution.DescribeAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_DescribeAction", lazy = true)
@ActionReference(path = "Editors/text/x-plsql/Popup", position = 440, separatorAfter = 450)
public class DescribeAction extends CookieAction {

    private static final RequestProcessor RP = new RequestProcessor(DescribeAction.class);
    private String viewName = null;

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[]{DataObject.class, EditorCookie.class};
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
        EditorCookie ec = dataObject.getLookup().lookup(EditorCookie.class);
        final Document doc = ec.getDocument();
        final DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
        final DatabaseConnection connection = connectionProvider.getPooledDatabaseConnection(true);
        if (connection == null) {
            return;
        }

        RP.post(new Runnable() {

            @Override
            public void run() {
                try {
                    String selectStatement = "DESC " + viewName;
                    PlsqlFileExecutor executor = new PlsqlFileExecutor(connectionProvider, connection);
                    List<PlsqlExecutableObject> exeBlocks = new ArrayList<PlsqlExecutableObject>();
                    exeBlocks.add(new PlsqlExecutableObject(0, selectStatement, "DESC", PlsqlExecutableObjectType.STATEMENT, 0, selectStatement.length() - 1));
                    executor.executePLSQL(exeBlocks, doc, true, true);
                } finally {
                    if (connection != null) {
                        connectionProvider.releaseDatabaseConnection(connection);
                    }
                }
            }
        });
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(ViewDataAction.class, "CTL_DescribeAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        viewName = null;

        if (!super.enable(activatedNodes)) {
            return false;
        }

        DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
        if (dataObject == null) {
            return false;
        }

        DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
        if (connectionProvider == null) {
            return false;
        }

        DatabaseConnection connection = connectionProvider.getDatabaseConnection(false);
        if (connection == null) {
            return false;
        }

        viewName = ViewDataAction.getSelectedViewOrTable(activatedNodes);
        if (viewName == null) {
            return false;
        }

        DatabaseContentManager dbCache = DatabaseContentManager.getInstance(connection);
        return dbCache.isView(viewName) || dbCache.isTable(viewName);
    }
}
