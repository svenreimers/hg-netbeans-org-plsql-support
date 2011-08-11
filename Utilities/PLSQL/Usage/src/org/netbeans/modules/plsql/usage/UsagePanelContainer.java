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
package org.netbeans.modules.plsql.usage;

import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import javax.swing.ActionMap;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.openide.awt.MouseUtils;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import org.openide.awt.TabbedPaneFactory;
import org.openide.util.NbBundle;

/**
 *
 * @author  Jan Becicka
 */
public class UsagePanelContainer extends TopComponent {

    private static UsagePanelContainer usages = null;
    private transient boolean isVisible = false;
    private JPopupMenu pop;
    /** Popup menu listener */
    private PopupListener listener;
    private CloseListener closeL;
   // private boolean isRefactoring;
   // private static Image REFACTORING_BADGE = ImageUtilities.loadImage("org/netbeans/modules/refactoring/api/resources/refactoringpreview.png"); // NOI18N
    private static Image USAGES_BADGE = ImageUtilities.loadImage("org/netbeans/modules/plsql/usage/resources/findusages.png"); // NOI18N

    private UsagePanelContainer() {
        this("");
    }

    /** Creates new form UsagePanelContainer */
    private UsagePanelContainer(String name) {
        setName(name);
        setToolTipText(name);
        setFocusable(true);
        setLayout(new java.awt.BorderLayout());
        setMinimumSize(new Dimension(1, 1));
        getAccessibleContext().setAccessibleDescription(
                NbBundle.getMessage(UsagePanelContainer.class, "ACSD_usagesPanel"));
        pop = new JPopupMenu();
        pop.add(new Close());
        pop.add(new CloseAll());
        pop.add(new CloseAllButCurrent());
        listener = new PopupListener();
        closeL = new CloseListener();
       // this.isRefactoring = isRefactoring;
        setFocusCycleRoot(true);
        JLabel label = new JLabel(NbBundle.getMessage(UsagePanelContainer.class, "LBL_NoUsages"));
        label.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        this.add(label, BorderLayout.CENTER);
        initActions();
    }

    void addPanel(JPanel panel) {
        PlsqlUsagePanel.checkEventThread();
        if (getComponentCount() == 0) {
            add(panel, BorderLayout.CENTER);
        } else {
            Component comp = getComponent(0);
            if (comp instanceof JTabbedPane) {
                ((JTabbedPane) comp).addTab(panel.getName() + "  ", null, panel, panel.getToolTipText()); //NOI18N
                ((JTabbedPane) comp).setSelectedComponent(panel);
                comp.validate();
            } else if (comp instanceof JLabel) {
                remove(comp);
                add(panel, BorderLayout.CENTER);
            } else {
                remove(comp);
                JTabbedPane pane = TabbedPaneFactory.createCloseButtonTabbedPane();
                pane.addMouseListener(listener);
                pane.addPropertyChangeListener(closeL);
                add(pane, BorderLayout.CENTER);
                pane.addTab(comp.getName() + "  ", null, comp, ((JPanel) comp).getToolTipText()); //NOI18N
                pane.addTab(panel.getName() + "  ", null, panel, panel.getToolTipText()); //NOI18N
                pane.setSelectedComponent(panel);
                pane.validate();
            }
        }
        if (!isVisible) {
            isVisible = true;
            open();
        }
        validate();
        requestActive();
    }

    @Override
    protected void componentActivated() {
        super.componentActivated();
        JPanel panel = getCurrentPanel();
        if (panel != null) {
            panel.requestFocus();
        }
    }

    void removePanel(JPanel panel) {
        //  UsagePanel.checkEventThread();
        Component comp = getComponentCount() > 0 ? getComponent(0) : null;
        if (comp instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) comp;
            if (panel == null) {
                panel = (JPanel) tabs.getSelectedComponent();
            }
            tabs.remove(panel);
            if (tabs.getComponentCount() == 1) {
                Component c = tabs.getComponent(0);
                tabs.removeMouseListener(listener);
                tabs.removePropertyChangeListener(closeL);
                remove(tabs);
                add(c, BorderLayout.CENTER);
            }
        } else {
            if (comp != null) {
                remove(comp);
            }
            isVisible = false;
            close();
        }
        validate();
    }

    void closeAllButCurrent() {
        Component comp = getComponent(0);
        if (comp instanceof JTabbedPane) {
            JTabbedPane tabs = (JTabbedPane) comp;
            Component current = tabs.getSelectedComponent();
            Component[] c = tabs.getComponents();
            for (int i = 0; i < c.length; i++) {
                if (c[i] != current) {
                    ((PlsqlUsagePanel) c[i]).close();
                }
            }
        }
    }

    public static synchronized UsagePanelContainer getUsagesComponent() {
        if (usages == null) {
            usages = (UsagePanelContainer) WindowManager.getDefault().findTopComponent("plsql-usages"); //NOI18N
            if (usages == null) {
                // #156401: WindowManager.findTopComponent may fail
                usages = createUsagesComponent();
            }
        }
        return usages;
    }

   /* public static synchronized UsagePanelContainer getRefactoringComponent() {
        if (refactorings == null) {
            refactorings = (UsagePanelContainer) WindowManager.getDefault().findTopComponent("refactoring-preview"); //NOI18N
            if (refactorings == null) {
                // #156401: WindowManager.findTopComponent may fail
                refactorings = createRefactoringComponent();
            }
        }
        return refactorings;
    }*/

   /* public static synchronized UsagePanelContainer createRefactoringComponent() {
        if (refactorings == null) {
            refactorings = new UsagePanelContainer(org.openide.util.NbBundle.getMessage(UsagePanelContainer.class, "LBL_Refactoring"), true);
        }
        return refactorings;
    }*/

    public static synchronized UsagePanelContainer createUsagesComponent() {
        if (usages == null) {
            usages = new UsagePanelContainer(org.openide.util.NbBundle.getMessage(UsagePanelContainer.class, "LBL_Usages"));
        }
        return usages;
    }

    @Override
    protected void componentClosed() {
        isVisible = false;
        if (getComponentCount() == 0) {
            return;
        }
        Component comp = getComponent(0);
        if (comp instanceof JTabbedPane) {
            JTabbedPane pane = (JTabbedPane) comp;
            Component[] c = pane.getComponents();
            for (int i = 0; i < c.length; i++) {
                ((PlsqlUsagePanel) c[i]).close();
            }
        } else if (comp instanceof PlsqlUsagePanel) {
            ((PlsqlUsagePanel) comp).close();
        }
    }

    @Override
    protected String preferredID() {
        return "PlsqlUsagePanel"; // NOI18N
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_ALWAYS;
    }

    private void initActions() {
        ActionMap map = getActionMap();

        map.put("jumpNext", new PrevNextAction(false)); // NOI18N
        map.put("jumpPrev", new PrevNextAction(true)); // NOI18N
    }

    public PlsqlUsagePanel getCurrentPanel() {
        if (getComponentCount() > 0) {
            Component comp = getComponent(0);
            if (comp instanceof JTabbedPane) {
                JTabbedPane tabs = (JTabbedPane) comp;
                return (PlsqlUsagePanel) tabs.getSelectedComponent();
            } else {
                if (comp instanceof PlsqlUsagePanel) {
                    return (PlsqlUsagePanel) comp;
                }
            }
        }
        return null;
    }

    private final class PrevNextAction extends javax.swing.AbstractAction {

        private boolean prev;

        public PrevNextAction(boolean prev) {
            this.prev = prev;
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            PlsqlUsagePanel panel = getCurrentPanel();
            if (panel != null) {
                if (prev) {
                    panel.selectPrevUsage();
                } else {
                    panel.selectNextUsage();
                }
            }
        }
    }

    private class CloseListener implements PropertyChangeListener {

        public void propertyChange(java.beans.PropertyChangeEvent evt) {
            if (TabbedPaneFactory.PROP_CLOSE.equals(evt.getPropertyName())) {
                removePanel((JPanel) evt.getNewValue());
            }
        }
    }

    /**
     * Class to showing popup menu
     */
    private class PopupListener extends MouseUtils.PopupMouseAdapter {

        /**
         * Called when the sequence of mouse events should lead to actual showing popup menu
         */
        protected void showPopup(MouseEvent e) {
            pop.show(UsagePanelContainer.this, e.getX(), e.getY());
        }
    } // end of PopupListener

    private class Close extends AbstractAction {

        public Close() {
            super(NbBundle.getMessage(UsagePanelContainer.class, "LBL_CloseWindow"));
        }

        public void actionPerformed(ActionEvent e) {
            removePanel(null);
        }
    }

    private final class CloseAll extends AbstractAction {

        public CloseAll() {
            super(NbBundle.getMessage(UsagePanelContainer.class, "LBL_CloseAll"));
        }

        public void actionPerformed(ActionEvent e) {
            close();
        }
    }

    private class CloseAllButCurrent extends AbstractAction {

        public CloseAllButCurrent() {
            super(NbBundle.getMessage(UsagePanelContainer.class, "LBL_CloseAllButCurrent"));
        }

        public void actionPerformed(ActionEvent e) {
            closeAllButCurrent();
        }
    }

    @Override
    public HelpCtx getHelpCtx() {
        return new HelpCtx(UsagePanelContainer.class.getName() + ".plsql-usages"); //NOI18N
    }

    @Override
    public java.awt.Image getIcon() {
            return USAGES_BADGE;
    }
}

