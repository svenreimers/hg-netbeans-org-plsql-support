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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.db.api.sql.execute.SQLExecution;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.awt.Mnemonics;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

@ActionID(id = "org.netbeans.modules.plsql.execution.ConnectionAction", category = "PLSQL")
@ActionRegistration(displayName = "#LBL_ConnectionAction", lazy = false)
@ActionReferences({
    @ActionReference(path = "Editors/text/x-plsql/Toolbars/Default",
            name = "org-netbeans-modules-plsql-execution-ConnectionAction", position = 100)
})
public final class ConnectionAction extends AbstractAction implements ContextAwareAction, Presenter.Toolbar {

    private final Lookup actionContext;
    private ToolbarPresenter toolbarPresenter;
    private DataObject dataObject;
    private static final String NO_CONNECTION = "no database connection";

    public ConnectionAction() {
        this(Utilities.actionsGlobalContext());
    }

    public ConnectionAction(Lookup context) {
        putValue(SHORT_DESCRIPTION, getName());
        this.actionContext = context;
        dataObject = context.lookup(DataObject.class);
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new ConnectionAction(context);
    }

    private String getName() {
        return NbBundle.getMessage(ConnectionAction.class, "LBL_ConnectionAction");
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

//    private String getDatabaseConnectionText() {
//        if (dataObject != null) {
//            DatabaseConnectionManager dbConnectionManager = DatabaseConnectionManager.getInstance(dataObject);
//            if (dbConnectionManager != null) {
//                return dbConnectionManager.formatDatabaseConnectionInfo();
//            }
//        }
//        return NO_CONNECTION;
//    }
    @Override
    public Component getToolbarPresenter() {
        final DatabaseConnectionMediator mediator = dataObject.getLookup().lookup(DatabaseConnectionMediator.class);
        if (mediator == null) {
            return null;
        }
        toolbarPresenter = new ToolbarPresenter(actionContext, mediator);
        return toolbarPresenter;

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        //do nothing...
    }

    private static final class ToolbarPresenter extends JPanel {

        private final Lookup actionContext;
        private final DatabaseConnectionMediator mediator;
        private JComboBox combo;
        private JLabel comboLabel;
        private DatabaseConnectionModel model;
        private boolean waiting;
        private static final RequestProcessor RP = new RequestProcessor(ToolbarPresenter.class);

        public ToolbarPresenter(final Lookup actionContext, DatabaseConnectionMediator connectionMediator) {
            this.mediator = connectionMediator;
            initComponents();
            waiting = true;
            RP.post(new Runnable() {
                @Override
                public void run() {
                    model = new DatabaseConnectionModel(mediator);
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            waiting = false;
                            combo.setModel(model);
                            setEnabled(true);
                        }
                    });
                }
            });
            this.actionContext = actionContext;
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension dim = super.getMinimumSize();
            int minWidth = comboLabel.getWidth() * 2;
            return new Dimension(minWidth, dim.height);
        }

        private void initComponents() {
            setLayout(new BorderLayout(4, 0));
            setBorder(new EmptyBorder(0, 2, 0, 8));
            setOpaque(false);
            setFocusTraversalPolicyProvider(true);
            setFocusTraversalPolicy(new DefaultFocusTraversalPolicy() {
                @Override
                public Component getDefaultComponent(Container aContainer) {
                    if (!SwingUtilities.isEventDispatchThread()) {
                        return null;
                    }
                    final EditorCookie ec = actionContext.lookup(
                            EditorCookie.class);
                    if (ec != null) {
                        JEditorPane[] panes = ec.getOpenedPanes();
                        if (panes != null) {
                            for (JEditorPane pane : panes) {
                                if (pane.isShowing()) {
                                    return pane;
                                }
                            }
                        }
                    }

                    return null;
                }
            });

            combo = new JComboBox();
            combo.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    DatabaseConnection dbconn = (DatabaseConnection) combo.getSelectedItem();
                    combo.setToolTipText(dbconn != null ? dbconn.getDisplayName() : null);
                }
            });
            combo.setOpaque(false);
            combo.setModel(new DefaultComboBoxModel(
                    new String[]{NbBundle.getMessage(ToolbarPresenter.class, "ConnectionAction.ToolbarPresenter.LoadingConnections")}));
            setEnabled(false);
            combo.setRenderer(new DatabaseConnectionRenderer(mediator));
            String accessibleName = NbBundle.getMessage(ConnectionAction.class, "LBL_DatabaseConnection");
            combo.getAccessibleContext().setAccessibleName(accessibleName);
            combo.getAccessibleContext().setAccessibleDescription(accessibleName);
            combo.setPreferredSize(new Dimension(200, combo.getPreferredSize().height));

            add(combo, BorderLayout.CENTER);

            comboLabel = new JLabel();
            Mnemonics.setLocalizedText(comboLabel, NbBundle.getMessage(ConnectionAction.class, "LBL_ConnectionAction"));
            comboLabel.setOpaque(false);
            comboLabel.setLabelFor(combo);
            add(comboLabel, BorderLayout.WEST);
        }

        @Override
        public void setEnabled(boolean enabled) {
            combo.setEnabled(enabled && !waiting);
            super.setEnabled(enabled && !waiting);
        }
    }

    private static final class DatabaseConnectionModel extends AbstractListModel implements ComboBoxModel, PropertyChangeListener {

//        private ConnectionListener listener;
        private List<DatabaseConnection> connectionList; // must be ArrayList
        private DatabaseConnectionMediator mediator;
//        private DatabaseConnectionManager connectionManager;

        @SuppressWarnings("LeakingThisInConstructor")
        public DatabaseConnectionModel(DatabaseConnectionMediator connectionMediator) {
            this.mediator = connectionMediator;
//            listener = WeakListeners.create(ConnectionListener.class, this, ConnectionManager.getDefault());
//            ConnectionManager.getDefault().addConnectionListener(listener);
//                                connectionProvider.addPropertyChangeListener(listener);

            connectionList = new ArrayList<DatabaseConnection>();
//            if (mediator.hasProjectDatabaseConnections()) {
            connectionList.addAll(mediator.getDatabaseConnections());
//            } else {
//                connectionList.addAll(Arrays.asList(ConnectionManager.getDefault().getConnections()));
//            }
//            sortConnections();
        }

        @Override
        public Object getElementAt(int index) {
            return connectionList.get(index);
        }

        @Override
        public int getSize() {
            return connectionList.size();
        }

        @Override
        public void setSelectedItem(Object object) {
            if (mediator != null) {
                mediator.updateConnection((DatabaseConnection) object);
            }
        }

        @Override
        public Object getSelectedItem() {
            return mediator != null ? mediator.getConnection().getConnection() : null;
        }

//        public void setSQLExecution(DatabaseConnectionMediator connectionMediator) {
//            // XXX: should add listeners 
////            if (this.executor != null) {
////                this.executor.removePropertyChangeListener(this);
////            }
//            this.mediator = connectionMediator;
////            if (this.executor != null) {
////                this.executor.addPropertyChangeListener(this);
////            }
//            fireContentsChanged(this, 0, 0); // because the selected item might have changed
//        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (propertyName == null || propertyName.equals(SQLExecution.PROP_DATABASE_CONNECTION)) {
                Mutex.EVENT.readAccess(new Runnable() {
                    @Override
                    public void run() {
                        fireContentsChanged(this, 0, 0); // because the selected item might have changed
                    }
                });
            }
        }
//        @Override
//        public void connectionsChanged() {
//            Mutex.EVENT.readAccess(new Runnable() {
//                @Override
//                public void run() {
//                    connectionList.clear();
//
////                    connectionList.addAll(Arrays.asList(ConnectionManager.getDefault().getConnections()));
//                    connectionList.addAll(connectionProvider.getDatabaseConnections());
//                    sortConnections();
//
//                    DatabaseConnection selectedItem = (DatabaseConnection) getSelectedItem();
//                    if (selectedItem != null && !connectionList.contains(selectedItem)) {
//                        setSelectedItem(null);
//                    }
//                    fireContentsChanged(this, 0, connectionList.size());
//                }
//            });
//        }
//        void sortConnections() {
//            Collections.sort(connectionList, new Comparator<DatabaseConnection>() {
//                @Override
//                public int compare(DatabaseConnection o1, DatabaseConnection o2) {
//                    return o1.getDisplayName().compareTo(o2.getDisplayName());
//                }
//            });
//        }
    }

    private static final class DatabaseConnectionRenderer extends DefaultListCellRenderer {

        private final DatabaseConnectionMediator mediator;

        public DatabaseConnectionRenderer(DatabaseConnectionMediator connectionMediator) {
            this.mediator = connectionMediator;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Object displayName;
            String tooltipText = null;

            DatabaseConnection connection = null;
            if (value instanceof DatabaseConnection) {
                connection = (DatabaseConnection) value;
                //XXX: should have url instead, see execution action dropdownbutton
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
                displayName = connection.getDisplayName();
                tooltipText = url;
            } else {
                displayName = value;
            }
            JLabel component = (JLabel) super.getListCellRendererComponent(list, displayName, index, isSelected, cellHasFocus);
            component.setToolTipText(tooltipText);
            if (mediator != null && mediator.isDefaultDatabase(connection)) {
                component.setFont(component.getFont().deriveFont(Font.BOLD));
            }

            return component;
        }
    }
}
