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
package org.netbeans.modules.plsqlsupport.db.project;

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import org.netbeans.api.db.explorer.ConnectionManager;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.db.explorer.support.DatabaseExplorerUIs;
import org.netbeans.spi.project.ui.support.ProjectCustomizer.Category;
import org.openide.util.NbBundle;

public class DatabasePanel extends JPanel {

   public static final String DATA_VALID = "dataValid";

   private Category category;
   private DefaultTableModel connectionTableModel;
   private DatabaseConnection mainConnection;

   private boolean valid = true;
   private ValidationInvoker validationInvoker = new ValidationInvoker();

   public DatabasePanel(Category category) {
      this.category = category;
      initComponents();
      connectionTableModel = (DefaultTableModel)connectionTable.getModel();
      TableColumn connectionTableColumn = connectionTable.getColumnModel().getColumn(0);
      connectionTableColumn.setCellEditor(new CellEditor());
      connectionTableColumn.setCellRenderer(new CellRenderer());
      connectionTable.setRowHeight(connectionTable.getRowHeight() + 4);
      connectionTableModel.addTableModelListener(validationInvoker);
      connectionTable.getColumnModel().getSelectionModel().addListSelectionListener(validationInvoker);
      performValidation();
   }

   public DatabaseConnection[] getDatabaseConnections() {
      List<DatabaseConnection> connections = new ArrayList<DatabaseConnection>();
      if (mainConnection != null)
         connections.add(mainConnection);
      for (int i = 0; i < connectionTableModel.getRowCount(); i++) {
         Object item = connectionTableModel.getValueAt(i, 0);
         if (item instanceof DatabaseConnection) {
            DatabaseConnection connection = (DatabaseConnection)item;
            if (!connections.contains(connection))
               connections.add(connection);
         }
      }
      return connections.toArray(new DatabaseConnection[connections.size()]);
   }

   public void setDatabaseConnections(DatabaseConnection[] connections) {
      connectionTableModel.setRowCount(0);
      for (DatabaseConnection connection : connections)
         connectionTableModel.addRow(new Object[] {connection});
      if (connections.length > 0)
         mainConnection = connections[0];
   }

   private void performValidation() {
      int row = connectionTable.getSelectedRow();
      boolean hasSelection = row != -1;
      boolean hasSelectedConnection = hasSelection && connectionTableModel.getRowCount() > row
            && connectionTableModel.getValueAt(row, 0) instanceof DatabaseConnection;
      btnRemove.setEnabled(hasSelection);
      btnSetAsMain.setEnabled(hasSelectedConnection);

      String errorMessage = "";

      DatabaseConnection[] connections = getDatabaseConnections();
      for (DatabaseConnection connection : connections)
         if (!connection.getDriverClass().equals(DatabaseConnectionManager.ORACLE_DRIVER_CLASS_NAME)) {
            errorMessage += NbBundle.getMessage(getClass(), "MSG_NonOracleDatabaseChosen");
            break;
         }

      boolean wasValid = valid;
      valid = errorMessage.equals("");
      category.setValid(valid);
      category.setErrorMessage(errorMessage);
      firePropertyChange(DATA_VALID, wasValid, valid);
   }

   @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      databaseConnectionLabel = new javax.swing.JLabel();
      btnAdd = new javax.swing.JButton();
      btnRemove = new javax.swing.JButton();
      connectionTableScrollPane = new javax.swing.JScrollPane();
      connectionTable = new javax.swing.JTable();
      btnSetAsMain = new javax.swing.JButton();

      databaseConnectionLabel.setLabelFor(connectionTableScrollPane);
      org.openide.awt.Mnemonics.setLocalizedText(databaseConnectionLabel, org.openide.util.NbBundle.getMessage(DatabasePanel.class, "DatabasePanel.databaseConnectionLabel.text")); // NOI18N

      org.openide.awt.Mnemonics.setLocalizedText(btnAdd, org.openide.util.NbBundle.getMessage(DatabasePanel.class, "DatabasePanel.btnAdd.text")); // NOI18N
      btnAdd.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnAddActionPerformed(evt);
         }
      });

      org.openide.awt.Mnemonics.setLocalizedText(btnRemove, org.openide.util.NbBundle.getMessage(DatabasePanel.class, "DatabasePanel.btnRemove.text")); // NOI18N
      btnRemove.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnRemoveActionPerformed(evt);
         }
      });

      connectionTableScrollPane.setName("selectedDatabases"); // NOI18N

      connectionTable.setModel(new javax.swing.table.DefaultTableModel(
         new Object [][] {

         },
         new String [] {
            "Database"
         }
      ));
      connectionTable.setTableHeader(null);
      connectionTableScrollPane.setViewportView(connectionTable);

      org.openide.awt.Mnemonics.setLocalizedText(btnSetAsMain, org.openide.util.NbBundle.getMessage(DatabasePanel.class, "DatabasePanel.btnSetAsMain.text")); // NOI18N
      btnSetAsMain.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            btnSetAsMainActionPerformed(evt);
         }
      });

      javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
      this.setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addComponent(connectionTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 437, Short.MAX_VALUE)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
               .addComponent(btnRemove, javax.swing.GroupLayout.DEFAULT_SIZE, 117, Short.MAX_VALUE)
               .addComponent(btnSetAsMain, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addComponent(btnAdd, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap())
         .addComponent(databaseConnectionLabel)
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
         .addGroup(layout.createSequentialGroup()
            .addComponent(databaseConnectionLabel)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
               .addGroup(layout.createSequentialGroup()
                  .addComponent(btnAdd)
                  .addGap(11, 11, 11)
                  .addComponent(btnRemove)
                  .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                  .addComponent(btnSetAsMain))
               .addComponent(connectionTableScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE))
            .addContainerGap())
      );
   }// </editor-fold>//GEN-END:initComponents

   private void btnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddActionPerformed
      connectionTableModel.addRow((Object[])null);
      connectionTable.editCellAt(connectionTableModel.getRowCount() - 1, 0);
   }//GEN-LAST:event_btnAddActionPerformed

   private void btnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRemoveActionPerformed
      int row = connectionTable.getSelectedRow();
      assert row != -1;
      TableCellEditor editor = connectionTable.getCellEditor();
      if (editor != null)
         editor.stopCellEditing();
      DatabaseConnection connection = (DatabaseConnection)connectionTableModel.getValueAt(row, 0);
      if (connection == mainConnection)
         mainConnection = null;
      connectionTableModel.removeRow(row);
   }//GEN-LAST:event_btnRemoveActionPerformed

   private void btnSetAsMainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSetAsMainActionPerformed
      int row = connectionTable.getSelectedRow();
      assert row != -1;
      Object item = connectionTableModel.getValueAt(row, 0);
      if (item instanceof DatabaseConnection) {
         mainConnection = (DatabaseConnection)item;
         connectionTableModel.fireTableDataChanged();
         TableCellEditor cellEditor = connectionTable.getCellEditor();
         if(cellEditor!=null)
            cellEditor.stopCellEditing();
      }
   }//GEN-LAST:event_btnSetAsMainActionPerformed

   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JButton btnAdd;
   private javax.swing.JButton btnRemove;
   private javax.swing.JButton btnSetAsMain;
   private javax.swing.JTable connectionTable;
   private javax.swing.JScrollPane connectionTableScrollPane;
   private javax.swing.JLabel databaseConnectionLabel;
   // End of variables declaration//GEN-END:variables

   private class ValidationInvoker implements TableModelListener, ListSelectionListener {

      public ValidationInvoker() {}

      public void tableChanged(TableModelEvent e) {
         performValidation();
      }

      public void valueChanged(ListSelectionEvent e) {
         performValidation();
      }
   }

   private static class CellEditor extends DefaultCellEditor {

      public CellEditor() {
         super(new JComboBox());
         DatabaseExplorerUIs.connect((JComboBox)editorComponent, ConnectionManager.getDefault());
      }

      @Override
      public Object getCellEditorValue() {
         Object item = ((JComboBox)editorComponent).getSelectedItem();
         return item instanceof DatabaseConnection ? (DatabaseConnection)item : null;
      }
   }

   private class CellRenderer extends DefaultTableCellRenderer {

      public CellRenderer() {}

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value,
                                                     boolean selected, boolean hasFocus, int row, int column) {
         super.getTableCellRendererComponent(table, value, selected, hasFocus, row, column);
         if (value != null) {
            setText(((DatabaseConnection)value).getName());
            if (value == mainConnection)
               setFont(getFont().deriveFont(Font.BOLD));
         }
         return this;
      }
   }
}
