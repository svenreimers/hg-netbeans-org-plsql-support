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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeModel;
import org.netbeans.api.project.Project;
import org.openide.ErrorManager;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

public class PlsqlUsagePanel extends javax.swing.JPanel {

   private transient boolean isVisible = false;
   private final List<PlsqlElement> usageElements;
   private transient JTree tree = null;
   private transient JScrollPane scrollPane = null;
   private JPanel left;
   public JSplitPane splitPane;
   private final String usageName;
   private final Project project;
   private final int usageCount;
   private transient ButtonListener buttonListener = null;
   private transient JToggleButton expandButton = null;
   private JToolBar toolBar = null;
   private transient JButton prevMatch = null;
   private transient JButton nextMatch = null;

   public PlsqlUsagePanel(List<PlsqlElement> elements, String usageName, int usageCount, Project project) {
      // this.isQuery = true;
      this.usageElements = elements;
      this.usageName = usageName;
      //this.packageName = packageName;
      this.usageCount = usageCount;
      this.project = project;
      initComponents();
      initialize();
      refresh();
   }

   public static void checkEventThread() {
      if (!SwingUtilities.isEventDispatchThread()) {
         ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, new IllegalStateException("This must happen in event thread!")); //NOI18N
      }
   }

   private void initialize() {
      checkEventThread();
      setFocusCycleRoot(true);
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      left = new JPanel();
      splitPane.setLeftComponent(left);
      left.setLayout(new BorderLayout());
      setLayout(new BorderLayout());
      add(splitPane, BorderLayout.CENTER);
      JToolBar toolBar = getToolBar();
      if (toolBar != null) {
         left.add(toolBar, BorderLayout.WEST);
      }
      validate();
   }

   private void refresh() {
      checkEventThread();
      final List<PlsqlElement> elements = this.usageElements;

      RequestProcessor.getDefault().post(new Runnable() {

         @Override
         public void run() {
            setName("Usages of " + usageName);

            final StringBuilder usageDesc = new StringBuilder();
            usageDesc.append(" [").append(usageCount); // NOI18N
            usageDesc.append(' ');
            usageDesc.append(usageCount == 1 ? NbBundle.getMessage(PlsqlUsagePanel.class, "LBL_Occurence") : NbBundle.getMessage(PlsqlUsagePanel.class, "LBL_Occurences"));
            usageDesc.append(']');


            final UsageNode root = new UsageNode(null, "Usages of <b>" + usageName + "</b>" + usageDesc.toString(), ImageUtilities.loadImageIcon("org/netbeans/modules/plsql/usage/resources/findusages.png", false));
            final Map nodes = new HashMap();

            final Cursor old = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            int i = 0;
            try {
               for (Iterator it = elements.iterator(); it.hasNext(); i++) {
                  createNode((PlsqlElement) it.next(), nodes, root);
               }

            } finally {
               setCursor(old);
            }
            SwingUtilities.invokeLater(new Runnable() {

               @Override
               public void run() {
                  if (tree == null) {
                     // add panel with appropriate content
                     tree = new JTree(root);
                     if ("Aqua".equals(UIManager.getLookAndFeel().getID())) { //NOI18N
                        tree.setBackground(UIManager.getColor("NbExplorerView.background")); //NOI18N
                     }
                     ToolTipManager.sharedInstance().registerComponent(tree);
                     tree.setCellRenderer(new UsageTreeCellRenderer());
                     final String text = NbBundle.getMessage(PlsqlUsagePanel.class, "ACSD_usagesTree"); // NOI18N
                     tree.getAccessibleContext().setAccessibleDescription(text);
                     tree.getAccessibleContext().setAccessibleName(text);
                     final UsageNodeListener listener = new UsageNodeListener();
                     UsageNodeListener.project = project;
                     tree.addMouseListener(listener);
                     tree.addKeyListener(listener);
                     tree.setToggleClickCount(0);
                     scrollPane = new JScrollPane(tree);
                     PlsqlUsagePanel.this.left.add(scrollPane, BorderLayout.CENTER);
                     PlsqlUsagePanel.this.validate();
                  } else {
                     tree.setModel(new DefaultTreeModel(root));
                  }

                  if (expandButton.isSelected()) {
                     expandAll();

                  } else {
                     expandButton.setSelected(false);
                  }
                  tree.setSelectionRow(0);
                  requestFocus();
               }
            });
         }
      });
      if (!isVisible) {
         // dock it into output window area and display
         final UsagePanelContainer cont = UsagePanelContainer.getUsagesComponent();
         cont.open();
         cont.requestActive();
         cont.addPanel(this);
         isVisible = true;
      }
   }

   @SuppressWarnings("unchecked")
   private UsageNode createNode(final PlsqlElement element, final Map nodes, final UsageNode root) {
      UsageNode node = null;
      node = (UsageNode) nodes.get(element);
      if (node != null) {
         return node;
      }
      final PlsqlElement parent = getParent(element.getParent());
      node = new UsageNode(element, element.getName(), (parent == null ? ImageUtilities.loadImageIcon("org/netbeans/modules/plsql/usage/resources/db_file.png", false) : null));
      final UsageNode parentNode = parent == null ? root : createNode(parent, nodes, root);
      parentNode.add(node);
      nodes.put(element, node);
      return node;
   }

   private PlsqlElement getParent(final String parentName) {
      if (parentName == null) {
         return null;
      }
      int i = 0;
      for (Iterator it = usageElements.iterator(); it.hasNext(); i++) {
         PlsqlElement plsqlElement = (PlsqlElement) it.next();
         if (parentName.equals(plsqlElement.getName()) && plsqlElement.getParent() == null) {
            return plsqlElement;
         }
      }
      return null;
   }

   void selectNextUsage() {
      UsageNodeListener.selectNextPrev(true, tree);
   }

   void selectPrevUsage() {
      UsageNodeListener.selectNextPrev(false, tree);
   }

   /**
    * Returns the toolbar. In this default implementation, toolbar is
    * oriented vertically in the west and contains 'expand tree' toggle
    * button.
    * Override this method and return null if you do not want toolbar
    * in your panel.
    *
    * @return  toolBar with actions
    */
   private JToolBar getToolBar() {
      checkEventThread();
      Dimension dim = new Dimension(24, 24);
      // expand button settings
      expandButton = new JToggleButton(
            ImageUtilities.loadImageIcon("org/netbeans/modules/plsql/usage/resources/expandTree.png", false));
      expandButton.setSelectedIcon(
            ImageUtilities.loadImageIcon("org/netbeans/modules/plsql/usage/resources/colapseTree.png", false));
      expandButton.setMaximumSize(dim);
      expandButton.setMinimumSize(dim);
      expandButton.setPreferredSize(dim);
      expandButton.setSelected(true);
      expandButton.setToolTipText(
            NbBundle.getMessage(PlsqlUsagePanel.class, "HINT_expandAll") // NOI18N
            );
      expandButton.setMnemonic(
            NbBundle.getMessage(PlsqlUsagePanel.class, "MNEM_expandAll").charAt(0) // NOI18N
            );
      expandButton.addActionListener(getButtonListener());
      // create toolbar
      toolBar = new JToolBar(JToolBar.VERTICAL);
      toolBar.setFloatable(false);

      nextMatch = new JButton(
            ImageUtilities.loadImageIcon("org/netbeans/modules/plsql/usage/resources/nextmatch.png", false));

      nextMatch.setMaximumSize(dim);
      nextMatch.setMinimumSize(dim);
      nextMatch.setPreferredSize(dim);
      nextMatch.setToolTipText(
            NbBundle.getMessage(PlsqlUsagePanel.class, "HINT_nextMatch") // NOI18N
            );
      nextMatch.addActionListener(getButtonListener());

      prevMatch = new JButton(
            ImageUtilities.loadImageIcon("org/netbeans/modules/plsql/usage/resources/prevmatch.png", false));

      prevMatch.setMaximumSize(dim);
      prevMatch.setMinimumSize(dim);
      prevMatch.setPreferredSize(dim);
      prevMatch.setToolTipText(
            NbBundle.getMessage(PlsqlUsagePanel.class, "HINT_prevMatch") // NOI18N
            );
      prevMatch.addActionListener(getButtonListener());

      toolBar.add(expandButton);
      toolBar.add(prevMatch);
      toolBar.add(nextMatch);

      return toolBar;
   }

   public void close() {
      UsagePanelContainer.getUsagesComponent().removePanel(this);
      closeNotify();
   }

   protected void closeNotify() {
      //ignore
   }

   @Override
   public void requestFocus() {
      super.requestFocus();
      if (tree != null) {
         tree.requestFocus();
      }
   }
   /*
    * Initializes button listener. The subclasses must not need this listener.
    * This is the reason of lazy initialization.
    */

   private ButtonListener getButtonListener() {
      if (buttonListener == null) {
         buttonListener = new ButtonListener();
      }

      return buttonListener;
   }

   /* expandAll nodes in the tree */
   public void expandAll() {
      checkEventThread();
      final Cursor old = getCursor();
      expandButton.setEnabled(false);
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

      int row = 0;
      while (row < tree.getRowCount()) {
         tree.expandRow(row);
         row++;
      }

      setCursor(old);
      expandButton.setEnabled(true);
      expandButton.setToolTipText(
            NbBundle.getMessage(PlsqlUsagePanel.class, "HINT_collapseAll") // NOI18N
            );
      requestFocus();
   }

   /* collapseAll nodes in the tree */
   public void collapseAll() {
      checkEventThread();
      expandButton.setEnabled(false);
      final Cursor old = getCursor();
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

      int row = tree.getRowCount() - 1;
      while (row > 0) {
         tree.collapseRow(row);
         row--;
      }

      setCursor(old);
      expandButton.setEnabled(true);
      expandButton.setToolTipText(
            NbBundle.getMessage(PlsqlUsagePanel.class, "HINT_expandAll") // NOI18N
            );
      requestFocus();
   }

   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

   ////////////////////////////////////////////////////////////////////////////////
   // INNER CLASSES
   //////////////////////////////////////////////////////////////////////////////
   private class ButtonListener implements ActionListener {

      @Override
      public void actionPerformed(final ActionEvent event) {
         final Object object = event.getSource();
         // expandAll button selected/deselected
         if (object.equals(expandButton) && tree != null) {
            if (expandButton.isSelected()) {
               expandAll();
            } else {
               collapseAll();
            }
         } else if (object.equals(nextMatch)) {
            selectNextUsage();
         } else if (object.equals(prevMatch)) {
            selectPrevUsage();
         }
      }
   }
}
