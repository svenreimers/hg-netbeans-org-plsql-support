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
package org.netbeans.modules.plsql.navigator;

import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.navigator.PlsqlNavigatorComponent.NodeInfo;
import java.io.IOException;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JComponent;
import org.netbeans.spi.navigator.NavigatorPanel;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import javax.swing.JEditorPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import org.netbeans.editor.Utilities;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;

/**
 * Basic dummy implementation of NavigatorPanel interface.
 */
//public class PlsqlNavigatorImpl implements NavigatorPanel, DocumentListener {
public class PlsqlNavigatorImpl implements NavigatorPanel, Observer {

   /** holds UI of this panel */
   private JComponent panelUI = null;
   /** template for finding data in given context.
    * Object used as example, replace with your own data source, for example JavaDataObject etc */
   private static final Lookup.Template<DataObject> MY_DATA = new Lookup.Template<DataObject>(DataObject.class);
   /** current context to work on */
   private Lookup.Result curContext = null;
   /** listener to context changes */
   private LookupListener contextL = null;
   private Observer oldObserver = null;
   private Document editorDocument = null;
   private JEditorPane editorPane = null;
   private PlsqlCaretListener caretListener = null;

   /** public no arg constructor needed for system to instantiate provider well */
   public PlsqlNavigatorImpl() {
   }

   public String getDisplayHint() {
      return "IFS PL/SQL Editor Navigator";
   }

   public String getDisplayName() {
      return "PL/SQL Navigator";
   }

   public JComponent getComponent() {
      if (panelUI == null) {
         panelUI = new PlsqlNavigatorComponent();
      // You can override requestFocusInWindow() on the component if desired.
      }
      return panelUI;
   }

   public void panelActivated(Lookup context) {
      // lookup context and listen to result to get notified about context changes
      curContext = context.lookup(MY_DATA);
      curContext.addLookupListener(getContextListener());

      // get actual data and recompute content
      Collection data = curContext.allInstances();
      setNewContent(data);
   }

   public void panelDeactivated() {
      if (curContext != null) {
         curContext.removeLookupListener(getContextListener());
         curContext = null;
      }

      if (editorPane != null) {
         if (caretListener != null) {
            editorPane.removeCaretListener(caretListener);
            caretListener = null;
         }
      }
   }

   public Lookup getLookup() {
      // go with default activated Node strategy
      //but first workaround for issue with newly opened documents...
      setPanelListeners();
      return null;
   }

   private void setPanelListeners() {
      if (editorPane == null) {
         //document opened after focusing in the navigator - listeners not set
         //Check if the document is now open...
         JTextComponent component = Utilities.getFocusedComponent();
         if (component != null) {
            Document doc = component.getDocument();
            if (doc == editorDocument && component instanceof JEditorPane) {
               editorPane = (JEditorPane) component;
               if (caretListener != null) {
                  editorPane.removeCaretListener(caretListener);
               }
               caretListener = new PlsqlCaretListener();
               editorPane.addCaretListener(caretListener);
            }
         }
      }
   }

   /************* non - public part ************/
   private void setNewContent(Collection newData) {
      // put your code here that grabs information you need from given
      // collection of data, recompute UI of your panel and show it.
      // Note - be sure to compute the content OUTSIDE event dispatch thread,
      // just final repainting of UI should be done in event dispatch thread.
      // Please use RequestProcessor and Swing.invokeLater to achieve this.

      if (newData.size() > 0) {
         final DataObject data = (DataObject) newData.iterator().next();
         final PlsqlBlockFactory blockFactory = data.getLookup().lookup(PlsqlBlockFactory.class);
         final EditorCookie ec = data.getLookup().lookup(EditorCookie.class);
         
         try {
            editorDocument = ec.openDocument();
            blockFactory.initHierarchy(editorDocument);
         } catch (IOException ex) {
            ex.printStackTrace();
         }
                  
         if (oldObserver != null) {
            blockFactory.deleteObserver(oldObserver);
         }
         blockFactory.addObserver(this);
         oldObserver = this;

         SwingUtilities.invokeLater(new Runnable() {

            public void run() {
               editorPane = null;
               if (ec != null) {
                  JEditorPane panes[] = ec.getOpenedPanes();
                  if (panes != null && panes.length > 0) {
                     editorPane = panes[0];
                  }
               }
               if (editorPane != null) {
                  //Add caret listener
                  if (caretListener != null) {
                     editorPane.removeCaretListener(caretListener);
                  }
                  caretListener = new PlsqlCaretListener();
                  editorPane.addCaretListener(caretListener);
               }

               ((PlsqlNavigatorComponent) panelUI).initTree(editorDocument, data, blockFactory.getBlockHierarchy());

               //Highlight current node in the navigator 
               if (editorPane != null) {
                  Caret caret = editorPane.getCaret();
                  if (caret != null) {
                     highlightCurrentPosition(((PlsqlNavigatorComponent) panelUI).treeModel,
                             ((PlsqlNavigatorComponent) panelUI).jTree1, caret.getDot());
                  }
               }
            }
         });
      }
   }

   /**
    * Method that will return the data obhect in the current context
    * @return
    */
   public DataObject getCurrentDataObject() {
      if (curContext != null) {
         Collection data = curContext.allInstances();
         if (data.size() > 0) {
            return (DataObject) data.iterator().next();
         }
      }

      return null;
   }

   /** Accessor for listener to context */
   private LookupListener getContextListener() {
      if (contextL == null) {
         contextL = new ContextListener();
      }
      return contextL;
   }

   /** Listens to changes of context and triggers proper action */
   private class ContextListener implements LookupListener {

      public void resultChanged(LookupEvent ev) {
         Collection newData = ((Lookup.Result) ev.getSource()).allInstances();
         setNewContent(newData);
      }
   } // end of ContextListener

   //Listens to the changes of the caret position in the pane and highlight the 
   //relevant node
   class PlsqlCaretListener implements CaretListener {

      public void caretUpdate(CaretEvent e) {
         if (panelUI == null) {
            return;
         }

         if (panelUI instanceof PlsqlNavigatorComponent) {
            DefaultTreeModel model = ((PlsqlNavigatorComponent) panelUI).treeModel;
            JTree tree = ((PlsqlNavigatorComponent) panelUI).jTree1;
            highlightCurrentPosition(model, tree, e.getDot());
         }
      }
   }

   /**
    * Method that will return the treePath containing the given offset
    * @param model
    * @param offset
    * @return
    */
   private TreePath getMatchingTreePath(DefaultTreeModel model, int offset) {
      TreePath path = null;
      DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
      int childCount = root.getChildCount();

      //Did this since there are only two levels
      for (int i = 0; i < childCount; i++) {
         DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
         if (node != null) {
            if (!node.isLeaf()) {
               int count = node.getChildCount();
               for (int x = 0; x < count; x++) {
                  DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(x);
                  if (child != null) {
                     Object userObj = child.getUserObject();
                     if ((userObj != null) && (userObj instanceof NodeInfo) && (((NodeInfo) userObj).startOffset <= offset) &&
                             (((NodeInfo) userObj).endOffset >= offset)) {
                        path = new TreePath(model.getPathToRoot(child));
                        break;
                     }
                  }
               }
            } else {
               Object userObj = node.getUserObject();
               if ((userObj != null) && (userObj instanceof NodeInfo) && (((NodeInfo) userObj).startOffset <= offset) &&
                       (((NodeInfo) userObj).endOffset >= offset)) {
                  path = new TreePath(model.getPathToRoot(node));
                  break;
               }
            }
         }
      }

      return path;
   }

   /**
    * Method that will highlight the current node in the navigator
    * @param model
    * @param tree
    * @param dot
    */
   public void highlightCurrentPosition(DefaultTreeModel model, JTree tree, int dot) {
      TreePath path = getMatchingTreePath(model, dot);
      if (path == null) {
         return;
      }

      tree.setSelectionPath(path);
      tree.scrollPathToVisible(path);
   }

   public static DataObject getDataObject(Document doc) {
      Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof DataObject) {
         DataObject dataObj = (DataObject) obj;
         return dataObj;
      }
      return null;
   }

   /**
    * Method where the document events are notified
    * @param obj1
    * @param obj2
    */
   public void update(Observable obj1, Object obj2) {
      if ((obj1 instanceof PlsqlBlockFactory) && (obj2 instanceof Document)) {
         final PlsqlBlockFactory blockFactory = (PlsqlBlockFactory) obj1;
         Document doc = (Document) obj2;

         //If Context listener has not updated the document yet return
         if (doc != editorDocument) {
            return;
         }

         if(blockFactory.isSaveInProgress())
            ((PlsqlNavigatorComponent) panelUI).initTree(doc, getDataObject(doc), blockFactory.getBlockHierarchy());
         else
            ((PlsqlNavigatorComponent) panelUI).update(blockFactory);
      }
   }
}
