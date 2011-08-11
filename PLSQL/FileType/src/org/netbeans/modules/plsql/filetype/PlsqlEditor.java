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
package org.netbeans.modules.plsql.filetype;

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.modules.plsqlsupport.options.IfsOptionsUtilities;
import org.netbeans.modules.plsql.annotation.PlsqlAnnotationManager;
import java.awt.Color;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.mimelookup.MimePath;
import org.netbeans.api.editor.settings.FontColorSettings;
import org.netbeans.editor.Coloring;
import org.openide.awt.TabbedPaneFactory;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.CloneableEditor;
import org.openide.text.DataEditorSupport;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;


public class PlsqlEditor extends CloneableEditor {

   private JSplitPane splitter;
   private JTabbedPane resultComponent;

   public PlsqlEditor() {
   }

   /** Creates new editor */
   public PlsqlEditor(PlsqlEditorSupport support) {
      super(support);
   }

   @Override
    protected boolean closeLast() {
        PlsqlDataObject dataObject = (PlsqlDataObject) ((DataEditorSupport) cloneableEditorSupport()).getDataObject();
       if (dataObject.isTemporary()) {
           DatabaseConnection connection = dataObject.getLookup().lookup(DatabaseConnection.class);
           if (connection.getJDBCConnection() != null) {

               DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);

               if (DatabaseConnectionManager.getInstance(dataObject).hasDataToCommit(connection)) {
                   String msg = "There are pending transactions in the database. Do you want to commit?";
                   String title = dataObject.getNodeDelegate().getDisplayName();
                   int result = JOptionPane.showOptionDialog(null, msg, title, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);
                   if (result == JOptionPane.YES_OPTION) {
                       //commit
                       connectionProvider.commitRollbackTransactions(connection, true);
                   } else if (result == JOptionPane.NO_OPTION) {
                       //rollback
                       connectionProvider.commitRollbackTransactions(connection, false);
                   } else {
                       return false;
                   }
               }
           }
           FileUtil.toFile(dataObject.getPrimaryFile()).delete();
       }


       if (!super.closeLast()) {
           return false;
       }
       return true;
    }

   public void closeResultSetTabs() {
      if (resultComponent != null) {
         resultComponent.removeAll();
      }
   }

   /** Overrides superclass method to change the editor background color*/
   protected void componentShowing() {
      super.componentShowing();

      DataObject dataObject = ((DataEditorSupport) cloneableEditorSupport()).getDataObject();
      //Init annotations      
      PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
      if (annotationManager != null) {
         annotationManager.initAnnotations(dataObject);
      }
   }

   public void setResults(List<Component> results, List<String> toolTips) {
      if (resultComponent == null && results != null) {
         createResultComponent();
      }

      if (resultComponent != null) {
         populateResults(results, toolTips);
      }
   }

   private void populateResults(List<Component> components, List<String> toolTips) {
      if (components == null) {
         return;
      }

      //Add new components
      Iterator<String> it = toolTips.iterator();
      for (Component comp : components) {
         String tooltip = "<html>" + it.next().replaceAll("\n", "<br>") + "</html>";
         String title = comp.getName().length() > 30 ? comp.getName().substring(0, 27) + "...   " : comp.getName() + "   ";
         resultComponent.addTab(title, null, comp, tooltip);
      }

      // Put focus on the first result from the set
      if (components.size() > 0) {
         resultComponent.setSelectedComponent(components.get(0));
      }

      showResultComponent();
   }

   private void createResultComponent() {
      JPanel container = findContainer(this);
      if (container == null) // the editor has just been deserialized and has not been initialized yet
      // thus CES.wrapEditorComponent() has not been called yet
      {
         return;
      }

      resultComponent = TabbedPaneFactory.createCloseButtonTabbedPane();
      Component editor = container.getComponent(0);
      container.removeAll();

      splitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editor, resultComponent);
      splitter.setBorder(null);

      container.add(splitter);
      splitter.setDividerLocation(250);
      splitter.setDividerSize(7);

      showResultComponent();

      if (equals(TopComponent.getRegistry().getActivated())) // setting back the focus lost when removing the editor from the CloneableEditor
      {
         requestFocusInWindow(true);
      }

      resultComponent.addPropertyChangeListener(new PropertyChangeListener() {

         public void propertyChange(PropertyChangeEvent evt) {
            if (TabbedPaneFactory.PROP_CLOSE.equals(evt.getPropertyName())) {
               int selected = resultComponent.getSelectedIndex();
               resultComponent.remove((Component) evt.getNewValue());
               int tabCount = resultComponent.getTabCount();
               if (selected > 0) {
                  selected--;
               }
               if (selected >= 0 && selected < tabCount) {
                  resultComponent.setSelectedIndex(selected);
               }
               if (tabCount == 0) {
                  hideResultComponent();
               }
               revalidate();
            }
         }
      });
   }

   private void hideResultComponent() {
      if (splitter == null) {
         return;
      }

      splitter.setBottomComponent(null);
   }

   private void showResultComponent() {
      JPanel container = findContainer(this);
      if (container == null) // the editor has just been deserialized and has not been initialized yet
      // thus CES.wrapEditorComponent() has not been called yet
      {
         return;
      }

      if (splitter == null) {
         return;
      }

      if (splitter.getBottomComponent() == null) {
         splitter.setBottomComponent(resultComponent);
         splitter.setDividerLocation(250);
         splitter.setDividerSize(7);

         container.invalidate();
         container.validate();
         container.repaint();
      }
   }

   /* Finds the container component added by SQLEditorSupport.wrapEditorComponent.
    * Not very nice, but avoids the API change in #69466.
    */
   private JPanel findContainer(Component parent) {
      if (!(parent instanceof JComponent)) {
         return null;
      }
      Component[] components = ((JComponent) parent).getComponents();
      for (int i = 0; i < components.length; i++) {
         Component component = components[i];
         if (component instanceof JPanel && PlsqlEditorSupport.EDITOR_CONTAINER.equals(component.getName())) {
            return (JPanel) component;
         }
         JPanel container = findContainer(component);
         if (container != null) {
            return container;
         }
      }
      return null;
   }

   @Override
   public void writeExternal(ObjectOutput out) throws IOException {
      super.writeExternal(out);
      PlsqlDataObject dataObject = (PlsqlDataObject) ((DataEditorSupport) cloneableEditorSupport()).getDataObject();
      if (dataObject.isTemporary()) {
         out.writeObject(getDisplayName());
      }
   }

   @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        PlsqlDataObject dataObject = (PlsqlDataObject) ((DataEditorSupport) cloneableEditorSupport()).getDataObject();
        if (dataObject.isTemporary()) {
            String displayName = (String) in.readObject();
            if (displayName.equalsIgnoreCase(NbBundle.getMessage(PlsqlEditor.class, "LBL_SQLExecutionWindow"))) {
                try {
                    EditorCookie edCookie = dataObject.getLookup().lookup(EditorCookie.class);
                    Document document = edCookie.openDocument();
                    String name = document.getLength() > 30 ? document.getText(0, 30) + "..." : document.getText(0, document.getLength());
                    name.replaceAll("\n", " ");
                    if (name.length() > 0) {
                        displayName = name;
                    }
                } catch (BadLocationException ex) {
                }
            }
            setDisplayName(displayName);
            dataObject.getNodeDelegate().setDisplayName(displayName);
        }
    }
}
