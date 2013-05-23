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

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.modules.plsql.annotation.PlsqlAnnotationManager;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionNewExecutor;
import org.openide.awt.TabbedPaneFactory;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.text.CloneableEditor;
import org.openide.text.DataEditorSupport;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;

public class PlsqlEditor extends CloneableEditor {

   private static final Logger LOG = Logger.getLogger(PlsqlEditor.class.getName());
   private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
   private JSplitPane splitter;
   private JTabbedPane resultComponent;

   public PlsqlEditor() {
   }

   /**
    * Creates new editor
    */
   public PlsqlEditor(PlsqlEditorSupport support) {
      super(support);
   }

   @Override
   protected boolean closeLast() {
      final DataObject dataObject = ((DataEditorSupport) cloneableEditorSupport()).getDataObject();
      if (validator.isValidTDB(dataObject)) {
         DatabaseConnectionNewExecutor executor = dataObject.getLookup().lookup(DatabaseConnectionNewExecutor.class);
         if (executor.closeOpenTransaction()) {
            FileUtil.toFile(dataObject.getPrimaryFile()).delete();
         } else {
            return false;
         }
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

   /**
    * Overrides superclass method to change the editor background color
    */
   @Override
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
         @Override
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
      DataObject dataObject = ((DataEditorSupport) cloneableEditorSupport()).getDataObject();
      if (validator.isValidTDB(dataObject)) {
         out.writeObject(getDisplayName());
      }
   }

   @Override
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
      super.readExternal(in);
      DataObject dataObject = ((DataEditorSupport) cloneableEditorSupport()).getDataObject();
      if (validator.isValidTDB(dataObject)) {
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
               LOG.log(Level.FINEST, displayName, ex);
            }
         }
         setDisplayName(displayName);
         dataObject.getNodeDelegate().setDisplayName(displayName);
      }
   }
}
