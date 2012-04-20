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
package org.netbeans.modules.plsqlsupport.options;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import javax.swing.JCheckBox;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import org.openide.util.NbPreferences;

public final class PLSQLAnnotationsPanel extends javax.swing.JPanel implements TreeCellRenderer {

   private DefaultTreeCellRenderer dr = new DefaultTreeCellRenderer();
   private final PLSQLAnnotationsOptionsPanelController controller;
   private JCheckBox renderer = new JCheckBox();
   private DefaultMutableTreeNode root = new DefaultMutableTreeNode();

   PLSQLAnnotationsPanel(PLSQLAnnotationsOptionsPanelController controller) {
      this.controller = controller;
      initComponents();

      treeAnnotations.setCellRenderer(this);
      treeAnnotations.setRootVisible(false);
      treeAnnotations.setShowsRootHandles(true);
      treeAnnotations.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
      treeAnnotations.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
              //  throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void mousePressed(MouseEvent e) {
              valueChanged(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
              //  throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
             //   throw new UnsupportedOperationException("Not supported yet.");
            }

            @Override
            public void mouseExited(MouseEvent e) {
            //    throw new UnsupportedOperationException("Not supported yet.");
            }
      });
   }

    private void valueChanged(MouseEvent e) {
        Object obj = e.getSource();
        if (obj != null && obj instanceof JTree) {
            JTree tree = (JTree) obj;
            int row = tree.getRowForLocation(e.getX(), e.getY());
            TreePath path = tree.getPathForRow(row);
            if (path != null) {
                DefaultMutableTreeNode node = getSelectedNode(root, path);
                if (node != null) {
                    Object tmp = node.getUserObject();
                    if (tmp != null && tmp instanceof PrefNode) {
                        if (((PrefNode) tmp).enabled) {
                            ((PrefNode) tmp).selected = !((PrefNode) tmp).selected;
                            setChildrenEnabled(node, false, ((PrefNode) tmp).selected);
                            setChildrenEnabled(node, true, ((PrefNode) tmp).selected);
                            updateUI();
                        }
                    }
                }
            }
        }
    }

   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      spAnnotations = new javax.swing.JScrollPane();
      treeAnnotations = new javax.swing.JTree();
      cbPlsqlAnnotations = new javax.swing.JCheckBox();

      setLayout(new java.awt.BorderLayout());

      spAnnotations.setViewportView(treeAnnotations);

      add(spAnnotations, java.awt.BorderLayout.CENTER);

      org.openide.awt.Mnemonics.setLocalizedText(cbPlsqlAnnotations, org.openide.util.NbBundle.getMessage(PLSQLAnnotationsPanel.class, "PLSQLAnnotationsPanel.cbPlsqlAnnotations.text")); // NOI18N
      cbPlsqlAnnotations.addItemListener(new java.awt.event.ItemListener() {
         public void itemStateChanged(java.awt.event.ItemEvent evt) {
            cbPlsqlAnnotationsItemStateChanged(evt);
         }
      });
      add(cbPlsqlAnnotations, java.awt.BorderLayout.PAGE_START);
   }// </editor-fold>//GEN-END:initComponents

   private void cbPlsqlAnnotationsItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_cbPlsqlAnnotationsItemStateChanged
      if (evt.getStateChange() == ItemEvent.SELECTED) {
         setChildrenEnabled(root, true, true);
      } else if (evt.getStateChange() == ItemEvent.DESELECTED) {
         setChildrenEnabled(root, true, false);
      }
      updateUI();
   }//GEN-LAST:event_cbPlsqlAnnotationsItemStateChanged

   /**
    * Recursively set the property of the children objects
    * @param parent
    * @param isEnabled if true set enabled property, else set selected property
    * @param value
    */
   private void setChildrenEnabled(DefaultMutableTreeNode parent, boolean isEnabled, boolean value) {
      Enumeration children = parent.children();
      while (children.hasMoreElements()) {
         Object obj = children.nextElement();
         if (obj != null && obj instanceof DefaultMutableTreeNode) {
            Object tmp = ((DefaultMutableTreeNode) obj).getUserObject();
            if (tmp != null && tmp instanceof PrefNode) {
               if (isEnabled) {
                  ((PrefNode) tmp).enabled = value;
               } else {
                  ((PrefNode) tmp).selected = value;
               }
            }
            setChildrenEnabled(((DefaultMutableTreeNode) obj), isEnabled, value);
         }
      }
   }

   void load() {
      cbPlsqlAnnotations.setSelected(NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(OptionsUtilities.PLSQL_ANNOTATIONS_ENABLED_KEY, true));
      root.removeAllChildren();
      DefaultMutableTreeNode generalNode = new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_GENERAL_KEY));
      root.add(generalNode);

      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_CURSOR_WHERE_KEY)));
      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_FUNCTION_RETURN_KEY)));
      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_IF_NULL_KEY)));
      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_MISSING_END_NAME_KEY)));
      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_UNREACHABLE_KEY)));
      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_WRONG_END_NAME_KEY)));
      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_WRONG_FUNC_PARAM_KEY)));
      generalNode.add(new DefaultMutableTreeNode(new PrefNode(OptionsUtilities.PLSQL_ANNOTATIONS_WRONG_PARAM_ORDER_KEY)));
      DefaultTreeModel model = new DefaultTreeModel(root);
      treeAnnotations.setModel(model);
      setChildrenEnabled(root, true, cbPlsqlAnnotations.isSelected());
   }

   void store() {
      NbPreferences.forModule(PLSQLAnnotationsPanel.class).putBoolean(OptionsUtilities.PLSQL_ANNOTATIONS_ENABLED_KEY, cbPlsqlAnnotations.isSelected());
      savePreferences(root);
   }

   boolean valid() {
      // TODO check whether form is consistent and complete
      return true;
   }
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JCheckBox cbPlsqlAnnotations;
   private javax.swing.JScrollPane spAnnotations;
   private javax.swing.JTree treeAnnotations;
   // End of variables declaration//GEN-END:variables

   public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      renderer.setBackground(selected ? dr.getBackgroundSelectionColor() : dr.getBackgroundNonSelectionColor());
      renderer.setForeground(selected ? dr.getTextSelectionColor() : dr.getTextNonSelectionColor());
      if (value instanceof DefaultMutableTreeNode) {
         Object obj = ((DefaultMutableTreeNode) value).getUserObject();
         if (obj != null && obj instanceof PrefNode) {
            renderer.setEnabled(((PrefNode) obj).enabled);
            renderer.setSelected(((PrefNode) obj).selected);
         }
      }
      renderer.setText(value.toString());
      return renderer;
   }

   private DefaultMutableTreeNode getSelectedNode(DefaultMutableTreeNode parent, TreePath selectionPath) {
      if (selectionPath == null)
         return null;

      Enumeration children = parent.children();
      while (children.hasMoreElements()) {
         Object obj = children.nextElement();
         if (obj != null && obj instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) obj;
            TreeNode[] path = node.getPath();
            if (isEqual(selectionPath.getPath(), path)) {
               return node;
            } else {
               node = getSelectedNode(node, selectionPath);
               if (node != null) {
                  return node;
               }
            }
         }
      }
      return null;
   }

   private boolean isEqual(Object[] path, TreeNode[] pathSelected) {
      if (path.length != pathSelected.length) {
         return false;
      }

      boolean isEqual = true;
      for (int i = 0; i < pathSelected.length; i++) {
         if (!pathSelected[i].equals(path[i])) {
            isEqual = false;
            break;
         }
      }

      return isEqual;
   }

   private void savePreferences(DefaultMutableTreeNode parent) {
      Enumeration children = parent.children();
      while (children.hasMoreElements()) {
         Object obj = children.nextElement();
         if (obj != null && obj instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
            Object tmp = node.getUserObject();
            if (tmp != null && tmp instanceof PrefNode) {
               ((PrefNode)tmp).savePreference();
            }
            savePreferences(node);
         }
      }
   }

   class PrefNode {

      public String displayName = "";
      public String key;
      public boolean selected = false;
      public boolean enabled = true;

      public PrefNode(String key) {
         this.key = key;
         this.displayName = org.openide.util.NbBundle.getMessage(PLSQLAnnotationsPanel.class, key);
         this.selected = NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(key, true);
      }

      @Override
      public String toString() {
         return displayName;
      }

      public void setEnabled(boolean enabled) {
         this.enabled = enabled;
      }

      public void savePreference() {
         NbPreferences.forModule(PLSQLAnnotationsPanel.class).putBoolean(key, selected);
      }
   }
}
