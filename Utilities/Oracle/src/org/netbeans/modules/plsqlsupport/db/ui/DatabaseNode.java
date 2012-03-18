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
package org.netbeans.modules.plsqlsupport.db.ui;

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import java.awt.Image;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

public class DatabaseNode extends AbstractNode {

    private static final Image ONLINE_ICON = ImageUtilities.loadImage("org/netbeans/modules/plsqlsupport/db/ui/resources/database_online.gif");
    private static final Image OFFLINE_ICON = ImageUtilities.loadImage("org/netbeans/modules/plsqlsupport/db/ui/resources/database_offline.gif");
    private static final Image NO_CONNECTION_ICON = ImageUtilities.loadImage("org/netbeans/modules/plsqlsupport/db/ui/resources/database_notconnected.gif");
    private Project project;

    public DatabaseNode(Project project) {
        super(Children.LEAF, Lookups.fixed(project));
        this.project = project;
        setName("Database");
        DatabaseConnectionManager dbConnectionProvider = project.getLookup().lookup(DatabaseConnectionManager.class);
        if (dbConnectionProvider != null) {
            dbConnectionProvider.addPropertyChangeListener(new ConnectionChangeListener());
        }
    }

    public static NodeFactory createFactory() {
        return new Factory();
    }

    @Override
    public Image getIcon(int type) {
        DatabaseConnectionManager dbConnectionProvider = project.getLookup().lookup(DatabaseConnectionManager.class);
        if (!dbConnectionProvider.hasConnection()) {
            return NO_CONNECTION_ICON;
        }
        return dbConnectionProvider.isOnline() ? ONLINE_ICON : OFFLINE_ICON;
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public Action[] getActions(boolean context) {
        List<? extends Action> actions = Utilities.actionsForPath("Databases/Nodes/Oracle");
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public String getHtmlDisplayName() {
        return getDisplayName(true);
    }

    @Override
    public String getDisplayName() {
        return getDisplayName(false);
    }

    private String getDisplayName(boolean addHTML) {
        String name = NbBundle.getMessage(getClass(), "LBL_DatabaseNodeName"); // NOI18N
        DatabaseConnectionManager connectionProvider = project.getLookup().lookup(DatabaseConnectionManager.class);
        List<DatabaseConnection> connections = connectionProvider.getDatabaseConnections();
        if (!connections.isEmpty()) {
            String url = connections.get(0).getDatabaseURL();
            String alias = connections.get(0).getDisplayName();
            if (alias != null && !alias.equals(connections.get(0).getName())) {
                name = name + " " + alias;
            }
            url = connections.get(0).getUser() + "@" + url.substring(url.lastIndexOf("@") + 1);
            if (connectionProvider.isOnline()) {
                name = name + " [" + url + "]";
            } else if (addHTML) {
                name = name + " [<s>" + url + "</s>]";
            }
        } else if (addHTML) {
            name = "<font color='AAAAAA'>" + name + "</font>";
        }
        return name;
    }

    private void updateCache() {
        DatabaseConnectionManager connectionProvider = project.getLookup().lookup(DatabaseConnectionManager.class);
        List<DatabaseConnection> connections = connectionProvider.getDatabaseConnections();
        if (!connections.isEmpty()) {
            DatabaseContentManager dbCache = DatabaseContentManager.getInstance(connections.get(0));
            if (dbCache != null) {
                dbCache.updateCache(connectionProvider);
            }
        }
    }

    private static class Factory implements NodeFactory {

        @Override
        public NodeList createNodes(Project project) {
            return NodeFactorySupport.fixedNodeList(new DatabaseNode(project));
        }
    }

    private class ConnectionChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run() {
                    fireIconChange();
                    setDisplayName(getHtmlDisplayName());
                    updateCache();
                }
            });
        }
    }
}
