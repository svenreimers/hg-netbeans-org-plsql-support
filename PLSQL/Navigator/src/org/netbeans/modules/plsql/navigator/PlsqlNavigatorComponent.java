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
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.ErrorManager;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.RequestProcessor;
import org.openide.windows.TopComponent;

/**
 *
 * @author malolk
 */
public class PlsqlNavigatorComponent extends NavigatorTopComponent {

    /**
     * Setting up a MouseListner for the navigator. This activates when the
     * user clicked on the navigator area.
     */
    public PlsqlNavigatorComponent() {
        super();
        initialize();
        navigatorMouseListner();
        navigatorButtonListner();
    }

    public void initialize() {
        root = new DefaultMutableTreeNode("Navigator Tree");
        treeModel = new DefaultTreeModel(root);
        jTree1 = new javax.swing.JTree(treeModel);
        jTree1.setToggleClickCount(Integer.MAX_VALUE);
        jScrollPane1.getViewport().setView(jTree1);

        if (getDocument() == null) {
            JTextComponent component = Utilities.getFocusedComponent();
            if (component != null)
                setDocument(Utilities.getDocument(component));
        }

        customizeIcon();
    }

    /**
     * Changes the Icons of the Navigator window according to what the user clicks
     */
    protected void customizeIcon() {
        if (pkgIcon != null) {
            NavTreeRenderer renderer = new NavTreeRenderer();
            jTree1.setCellRenderer(renderer);
        } else {
            System.err.println("icon missing; using default.");
        }
    }


    public void navigatorButtonListner() {
        ActionListener al = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JToggleButton sort = (JToggleButton) e.getSource();
                String s = sort.getName();
                if (sort.getName().equals("SORT_SOURCE")) {
                    btnSortAlpha.setSelected(false);
                    btnSortPosition.setSelected(true);
                    if (!sortBySource) {
                        sortBySource = true;
                        sortTreeModel();
                    }

                } else {
                    btnSortPosition.setSelected(false);
                    btnSortAlpha.setSelected(true);
                    if (sortBySource) {
                        sortBySource = false;
                        sortTreeModel();
                    }
                }
            }
        };
        btnSortPosition.addActionListener(al);
        btnSortAlpha.addActionListener(al);
    }

    public void navigatorMouseListner() {
        MouseListener ml = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = jTree1.getRowForLocation(e.getX(), e.getY());
                if (selRow != -1) {
                    if (e.getClickCount() == 2) {
                        try {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) jTree1.getLastSelectedPathComponent();
                            if (node == null) {
                                return;
                            }

                            Object nodeInfo = node.getUserObject();
                            if (nodeInfo instanceof NodeInfo) {
                                NodeInfo block = (NodeInfo) nodeInfo;
                                startOffset = block.startOffset;
                                openAndFocusElement();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        };
        jTree1.addMouseListener(ml);
    }

    /**
     * Method called to update the tree on document changes
     * @param blockFactory
     */
    public void update(PlsqlBlockFactory blockFactory) {
       final List<PlsqlBlock> newBlocks = new ArrayList<PlsqlBlock>(blockFactory.getNewBlocks());
       final List<PlsqlBlock> changedBlocks = new ArrayList<PlsqlBlock>(blockFactory.getChangedBlocks());
       final List<PlsqlBlock> removedBlocks = new ArrayList<PlsqlBlock>(blockFactory.getRemovedBlocks());
       loadAndExpandTree(root, treeModel, newBlocks, changedBlocks, removedBlocks);
    }

    /**
     * Method called to load the initial tree
     * @param doc
     * @param data
     * @param lst
     */
    public void initTree(Document doc, DataObject data, List<PlsqlBlock> blocks) {
        try {
            setDocument(doc);
            loadAndExpandInitialTree(root, treeModel, data, blocks);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Method that will add children nodes to the given parent node based on the type
     * @param parent
     * @param childBlocks
     * @param parentType
     */
    private void addChildNodes(DefaultMutableTreeNode parent, List childBlocks, PlsqlBlockType parentType) {
        int count = childBlocks.size();

        for (int i = 0; i < count; i++) {
            PlsqlBlock temp = (PlsqlBlock) childBlocks.get(i);
            addChildNode(parent, temp, parentType);
        }
    }

    /**
     * Method that will add given child to the parent node based on the type
     * @param parent
     * @param child
     * @param parentType
     */
    private void addChildNode(DefaultMutableTreeNode parent, PlsqlBlock child, PlsqlBlockType parentType) {
        PlsqlBlockType type = child.getType();
         int index = 0;
        //We are looking for child blocks and adding now
        if (type == PlsqlBlockType.FUNCTION_IMPL) { //Leaf node

            NodeInfo nodeInfo = new NodeInfo(child.getStartOffset(), child.getEndOffset(),
                    child.getName(), child.getAlias(), child.getType());
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
            treeModel.insertNodeInto(node, parent, getChildIndex(parent, node));
        } else if (type == PlsqlBlockType.PROCEDURE_IMPL) { //Leaf node

            NodeInfo nodeInfo = new NodeInfo(child.getStartOffset(), child.getEndOffset(),
                    child.getName(), child.getAlias(), child.getType());
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
            treeModel.insertNodeInto(node, parent, getChildIndex(parent, node));
        } else if ((type == PlsqlBlockType.PROCEDURE_DEF) && (parentType == PlsqlBlockType.PACKAGE)) { //Leaf node

            NodeInfo nodeInfo = new NodeInfo(child.getStartOffset(), child.getEndOffset(),
                    child.getName(), child.getAlias(), child.getType());
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
            treeModel.insertNodeInto(node, parent, getChildIndex(parent, node));
        } else if ((type == PlsqlBlockType.FUNCTION_DEF) && (parentType == PlsqlBlockType.PACKAGE)) { //Leaf node

            NodeInfo nodeInfo = new NodeInfo(child.getStartOffset(), child.getEndOffset(),
                    child.getName(), child.getAlias(), child.getType());
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
            treeModel.insertNodeInto(node, parent, getChildIndex(parent, node));
        }
    }


    private void sortTreeModel(){
         int count = root.getChildCount();
        if (count > 0) {
            int i = 0;
            ArrayList parent = Collections.list(root.children());
            Collections.sort(parent, new CompareNodes(sortBySource));
            // for main nodes
            while(i < count){
                treeModel.removeNodeFromParent((DefaultMutableTreeNode)root.getChildAt(0));
                i++;
            }
            // for child nodes
            i =0;
            while (i < count) {
                DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.get(i);
                if (child.getChildCount() > 1) {
                    ArrayList childNode = Collections.list(child.children());
                    Collections.sort(childNode, new CompareNodes(sortBySource));
                    child.removeAllChildren();
                    treeModel.insertNodeInto(child, root, getChildIndex(root, child));
                    int j = 0;
                    int childCount = childNode.size();
                    while (j < childCount) {
                        DefaultMutableTreeNode childList = (DefaultMutableTreeNode) childNode.get(j);
                        treeModel.insertNodeInto(childList, child, getChildIndex(child, childList));
                        j++;
                    }
                } else {
                    treeModel.insertNodeInto(child, root, getChildIndex(root, child));
                }
                i++;
            }
        }
         expandAll(jTree1, new TreePath(root), true);
        jTree1.updateUI();
    }

  
    /**
     * Get the parent node for the given block
     * @param root
     * @param block
     * @return
     */
    private DefaultMutableTreeNode getParentNode(DefaultMutableTreeNode root, PlsqlBlock block) {
        DefaultMutableTreeNode parentNode = null;
        int count = root.getChildCount();
        for (int i = 0; i < count; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(i);
            if (node.getUserObject() instanceof NodeInfo) {
                NodeInfo nodeInfo = (NodeInfo) node.getUserObject();

                if ((nodeInfo.startOffset < block.getStartOffset()) &&
                        (nodeInfo.endOffset > block.getEndOffset())) {
                    parentNode = node;
                    break;
                }
            }
        }

        return parentNode;
    }

    /**
     * Method that will load and expand the initial tree. i.e. add all the elements in
     * the given list
     * @param root
     * @param treeModel
     * @param dataObject
     * @param blockhierachy
     */
    private synchronized void loadAndExpandInitialTree(DefaultMutableTreeNode root, DefaultTreeModel treeModel, DataObject dataObject, List<PlsqlBlock> blockhierarchy) {
        List<PlsqlBlock> blocks = new ArrayList<PlsqlBlock>(blockhierarchy);
       Collections.sort(blocks, new CompareBlocks(sortBySource));
        //Remove all the previous nodes
        if (root.getChildCount() > 0) {
            while (root.getChildCount() > 0) {
                treeModel.removeNodeFromParent((MutableTreeNode) root.getFirstChild());
            }
        }

        if (!dataObject.isValid())
           return;

        //Set root name
        String rootName = "Navigator Tree";
        if (dataObject != null) {
            rootName = dataObject.getNodeDelegate().getDisplayName();
        }

        root.setUserObject(rootName);

        //Add nodes from block hierarchy now
        if (blocks != null) {
            DefaultMutableTreeNode viewsNode = null;
            DefaultMutableTreeNode packagesNode = null;
            DefaultMutableTreeNode methodsNode = null;

            int count = blocks.size();

            for (int i = 0; i < count; i++) {
                PlsqlBlock temp = blocks.get(i);
                PlsqlBlockType type = temp.getType();

                //We are looking for parent blocks and adding now
                if (type == PlsqlBlockType.VIEW) { //Leaf node

                    if (viewsNode == null) {
                        viewsNode = new DefaultMutableTreeNode("Views");
                        treeModel.insertNodeInto(viewsNode, root, 0);
                    }

                    NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                            temp.getName(), temp.getAlias(), temp.getType());
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                    treeModel.insertNodeInto(node, viewsNode, getChildIndex(viewsNode, node));
                } else if (type == PlsqlBlockType.PACKAGE) { //Parent node

                    if (packagesNode == null) {
                        packagesNode = new DefaultMutableTreeNode("Packages");
                    }

                    NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                            temp.getName(), temp.getAlias(), temp.getType());
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                    treeModel.insertNodeInto(node, packagesNode, getChildIndex(packagesNode, node));
                    addChildNodes(node, temp.getChildBlocks(), temp.getType());
                } else if (type == PlsqlBlockType.PACKAGE_BODY) { //Parent node

                    if (packagesNode == null) {
                        packagesNode = new DefaultMutableTreeNode("Packages");
                    }

                    NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                            temp.getName(), temp.getAlias(), temp.getType());
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                    treeModel.insertNodeInto(node, packagesNode, getChildIndex(packagesNode, node));
                    addChildNodes(node, temp.getChildBlocks(), temp.getType());
                } else if (type == PlsqlBlockType.FUNCTION_IMPL) { //Leaf node

                    if (methodsNode == null) {
                        methodsNode = new DefaultMutableTreeNode("Methods");
                    }

                    NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                            temp.getName(), temp.getAlias(), temp.getType());
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                    treeModel.insertNodeInto(node, methodsNode, getChildIndex(methodsNode, node));
                } else if (type == PlsqlBlockType.PROCEDURE_IMPL) { //Leaf node

                    if (methodsNode == null) {
                        methodsNode = new DefaultMutableTreeNode("Methods");
                    }

                    NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                            temp.getName(), temp.getAlias(), temp.getType());
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                    treeModel.insertNodeInto(node, methodsNode, getChildIndex(methodsNode, node));
                }
            }

            //Add packages and parent methods
            if (packagesNode != null) {
                while (packagesNode.getChildCount() > 0) {
                    DefaultMutableTreeNode temp = (DefaultMutableTreeNode) packagesNode.getFirstChild();
                    treeModel.insertNodeInto(temp, root, root.getChildCount());
                }
            }

            if (methodsNode != null) {
                while (methodsNode.getChildCount() > 0) {
                    DefaultMutableTreeNode temp = (DefaultMutableTreeNode) methodsNode.getFirstChild();
                    treeModel.insertNodeInto(temp, root, root.getChildCount());
                }
            }
        }

        expandAll(jTree1, new TreePath(root), true);
        jTree1.updateUI();
    }

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
        //workaround to make sure that icons are updated properly.
        customizeIcon();
        // Traverse children
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    private boolean isBlockShownInNavigator(PlsqlBlock block, DefaultMutableTreeNode parentNode) {
       PlsqlBlockType parentType = null;
       if (parentNode!=null && parentNode.getUserObject() instanceof NodeInfo)
           parentType = ((NodeInfo)parentNode.getUserObject()).type;
       return (block.getType() == PlsqlBlockType.PACKAGE ||
          block.getType() == PlsqlBlockType.PACKAGE_BODY ||
          block.getType() == PlsqlBlockType.VIEW ||
          ((parentType==null || parentType == PlsqlBlockType.PACKAGE) &&
             (block.getType() == PlsqlBlockType.FUNCTION_DEF ||
              block.getType() == PlsqlBlockType.PROCEDURE_DEF)) ||
          ((parentType==null || parentType == PlsqlBlockType.PACKAGE_BODY) &&
             (block.getType() == PlsqlBlockType.FUNCTION_IMPL ||
              block.getType() == PlsqlBlockType.PROCEDURE_IMPL)));
    }


   private static class CompareBlocks implements Comparator<PlsqlBlock> {
      private boolean byPosition;
      public CompareBlocks(boolean byPosition) {
         this.byPosition = byPosition;
      }

      public int compare(PlsqlBlock o1, PlsqlBlock o2) {
         //first sort by type
         int result = o1.getType().compareTo(o2.getType());
         if(result!=0 && !((o1.getType()==PlsqlBlockType.FUNCTION_DEF  || o1.getType()==PlsqlBlockType.PROCEDURE_DEF ||
                            o1.getType()==PlsqlBlockType.FUNCTION_IMPL || o1.getType()==PlsqlBlockType.PROCEDURE_IMPL) &&
                           (o2.getType()==PlsqlBlockType.PROCEDURE_DEF || o2.getType()==PlsqlBlockType.FUNCTION_DEF ||
                            o2.getType()==PlsqlBlockType.PROCEDURE_IMPL || o2.getType()==PlsqlBlockType.FUNCTION_IMPL)))
            return result;

         if(byPosition) {
            Integer o1pos, o2pos;
            if(o1.getPreviousStart()>-1 && o2.getPreviousStart()>-1) {
               o1pos = new Integer(o1.getPreviousStart());
               o2pos = new Integer(o2.getPreviousStart());
            } else if(o1.getPreviousEnd()>-1 && o2.getPreviousEnd()>-1) {
               o1pos = new Integer(o1.getPreviousEnd());
               o2pos = new Integer(o2.getPreviousEnd());
            } else if(o1.getStartOffset()>-1 && o2.getStartOffset()>-1){
               o1pos = new Integer(o1.getStartOffset());
               o2pos = new Integer(o2.getStartOffset());
            } else {
               o1pos = new Integer(o1.getEndOffset());
               o2pos = new Integer(o2.getEndOffset());
            }
            return o1pos.compareTo(o2pos);
         } else { //sort by name
            return o1.getName().compareToIgnoreCase(o2.getName());
         }
      }

   }

   
    private static class CompareNodes implements Comparator<DefaultMutableTreeNode> {

        private boolean byPosition;

        public CompareNodes(boolean byPosition) {
            this.byPosition = byPosition;
        }

        public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {

            NodeInfo ni1;
            NodeInfo ni2;
            String s1;
            String s2;

            if (!(o1.getUserObject() instanceof NodeInfo) && (o2.getUserObject() instanceof NodeInfo)) {
                if (byPosition) {
                    o1 = (DefaultMutableTreeNode) o1.getChildAt(0);
                } else {
                    s1 = (String) o1.getUserObject();
                    ni2 = (NodeInfo) o2.getUserObject();
                    return s1.compareToIgnoreCase(ni2.name);
                }
            } else if ((o1.getUserObject() instanceof NodeInfo) && !(o2.getUserObject() instanceof NodeInfo)) {
                if (byPosition) {
                    o2 = (DefaultMutableTreeNode) o2.getChildAt(0);
                } else {
                    ni1 = (NodeInfo) o1.getUserObject();
                    s2 = (String) o2.getUserObject();
                    return ni1.name.compareToIgnoreCase(s2);
                }
            } else if (!(o1.getUserObject() instanceof NodeInfo) && !(o2.getUserObject() instanceof NodeInfo)) {
                s1 = (String) o1.getUserObject();
                s2 = (String) o2.getUserObject();
                return s1.compareToIgnoreCase(s2);
            }

            ni1 = (NodeInfo) o1.getUserObject();
            ni2 = (NodeInfo) o2.getUserObject();
            if (byPosition) {
                Integer o1pos, o2pos;

                if (ni1.startOffset > -1 && ni2.startOffset > -1) {
                    o1pos = ni1.startOffset;
                    o2pos = ni2.startOffset;
                } else {
                    o1pos = ni1.endOffset;
                    o2pos = ni2.endOffset;
                }
                return o1pos.compareTo(o2pos);
            } else { //sort by name
                return ni1.name.compareToIgnoreCase(ni2.name);
            }
        }
    }
  
    /**
     * Method that will be called to update the tree structure on document events
     * @param root
     * @param treeModel
     * @param blockFactory
     */
   private synchronized void loadAndExpandTree(DefaultMutableTreeNode root, DefaultTreeModel treeModel, List<PlsqlBlock> newBlocks, List<PlsqlBlock> changedBlocks, List<PlsqlBlock> removedBlocks) {
  
      Collections.sort(newBlocks, new CompareBlocks(sortBySource));
      Collections.sort(changedBlocks, new CompareBlocks(sortBySource));
      Collections.sort(removedBlocks, new CompareBlocks(sortBySource));
      //Update offsets of the changed blocksparentNodeparentNode

      Enumeration topLevelNodes = root.children();
      if (topLevelNodes.hasMoreElements()) {
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)topLevelNodes.nextElement();
         Enumeration currentNodeList = topLevelNodes;
         DefaultMutableTreeNode parentNode = null;
         for (int i = 0; i < changedBlocks.size(); i++) {
            PlsqlBlock block = changedBlocks.get(i);
            if (isBlockShownInNavigator(block, parentNode)) {
               boolean childBlock = (block.getType() != PlsqlBlockType.PACKAGE) && (block.getType() != PlsqlBlockType.PACKAGE_BODY);
               boolean matchFound = false;
               boolean fromTheTop = false;
               while(node!=null && !matchFound) {
                  if (node.getUserObject() instanceof NodeInfo) {
                     NodeInfo nodeInfo = (NodeInfo) node.getUserObject();
                     if ((nodeInfo.type == block.getType()) &&
                           ((nodeInfo.type==PlsqlBlockType.PACKAGE_BODY && nodeInfo.name.equals(block.getName())) || //workaround for issue with packages... Should really be fixed in a different way...
                             (((nodeInfo.startOffset != block.getStartOffset()) &&
                             (nodeInfo.startOffset == block.getPreviousStart())) ||
                             ((nodeInfo.endOffset != block.getEndOffset()) &&
                             (nodeInfo.endOffset == block.getPreviousEnd()))))) {
                        //we found the match
                        nodeInfo.startOffset = block.getStartOffset();
                        nodeInfo.endOffset = block.getEndOffset();
                        nodeInfo.name = block.getName();
                        nodeInfo.alias = block.getAlias();
                        node.setUserObject(nodeInfo);
                        matchFound = true;
                     }
                  }
                  if (currentNodeList==topLevelNodes) {
                     currentNodeList = node.children();
                     parentNode = node;
                  }
                  if(!currentNodeList.hasMoreElements() || (!childBlock && !matchFound)) {
                     currentNodeList=topLevelNodes;
                     parentNode = null;
                  }
                  if(currentNodeList.hasMoreElements())
                     node = (DefaultMutableTreeNode)currentNodeList.nextElement();
                  else if(matchFound || fromTheTop)
                     node = null;
                  else {
                     //this shouldn't happen if there are any remaining "changed" blocks. However, it could happen in some strange scenario so we'll try from the start again
                     topLevelNodes = root.children();
                     node = (DefaultMutableTreeNode)topLevelNodes.nextElement();
                     currentNodeList = topLevelNodes;
                     parentNode = null;
                     fromTheTop=true;
                  }
               }
            }
         }
      }

      //find nodes to remove (but don't actually remove them...)
      topLevelNodes = root.children();
      List<DefaultMutableTreeNode> nodesToRemove = new ArrayList<DefaultMutableTreeNode>();
      if (topLevelNodes.hasMoreElements()) {
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)topLevelNodes.nextElement();
         DefaultMutableTreeNode parentNode = null;
         Enumeration currentNodeList = topLevelNodes;
         for (int i = 0; i < removedBlocks.size(); i++) {
            PlsqlBlock block = removedBlocks.get(i);
            if (isBlockShownInNavigator(block, parentNode)) {
               boolean childBlock = (block.getType() != PlsqlBlockType.PACKAGE) && (block.getType() != PlsqlBlockType.PACKAGE_BODY);
               boolean matchFound = false;
               boolean fromTheTop = false;
               while(node!=null && !matchFound) {
                  if (node.getUserObject() instanceof NodeInfo) {
                     NodeInfo nodeInfo = (NodeInfo) node.getUserObject();
                     if ((nodeInfo.type == block.getType()) &&
                           (nodeInfo.name.equals(block.getName())) &&
                           (nodeInfo.type==PlsqlBlockType.PACKAGE_BODY || //workaround for issue with packages... Should really be fixed in a different way...
                           ((nodeInfo.startOffset == block.getStartOffset()) ||
                           (nodeInfo.endOffset == block.getEndOffset()) ||
                           (nodeInfo.startOffset == block.getPreviousStart()) || 
                           (nodeInfo.endOffset == block.getPreviousEnd())))) {
                        nodesToRemove.add(node);
                        matchFound = true;
                     }
                  }
                  //go to next candidate node
                  if (currentNodeList==topLevelNodes) {
                     currentNodeList = node.children();
                     parentNode = node;
                  }
                  if(!currentNodeList.hasMoreElements() || (!childBlock && !matchFound)) {
                     currentNodeList=topLevelNodes;
                     parentNode = null;
                  }
                  if(currentNodeList.hasMoreElements())
                     node = (DefaultMutableTreeNode)currentNodeList.nextElement();
                  else if(matchFound || fromTheTop)
                     node = null;
                  else {
                     //this shouldn't happen if there are any remaining "removed" blocks. However, it could happen in some strange scenario so we'll try from the start again
                     topLevelNodes = root.children();
                     node = (DefaultMutableTreeNode)topLevelNodes.nextElement();
                     currentNodeList = topLevelNodes;
                     parentNode = null;
                     fromTheTop=true;
                  }
               }
            }
         }
      }
      //actually remove the nodes
      for(DefaultMutableTreeNode node : nodesToRemove)
         treeModel.removeNodeFromParent(node);

      //Now we can add new blocks with their children
      for (int i = 0; i < newBlocks.size(); i++) {
         PlsqlBlock temp = newBlocks.get(i);
         int lastInsertionIndex = 0;
         if (isBlockShownInNavigator(temp, null)) {
            DefaultMutableTreeNode parentNode = getParentNode(root, temp);
            if (parentNode != null) {
               //if this is a child of a leaf ignore
               DefaultMutableTreeNode leafParent = getParentNode(parentNode, temp);
               if (leafParent == null) {
                  //add to parent
                  boolean expand = parentNode.isLeaf();
                  addChildNode(parentNode, temp, ((NodeInfo) parentNode.getUserObject()).type);
                  if (expand) {
                     expandAll(jTree1, new TreePath(treeModel.getPathToRoot(parentNode)), true);
                  }
               }
            } else {
               //If a view add to views
               PlsqlBlockType type = temp.getType();
               if (type == PlsqlBlockType.VIEW) {
                  boolean expand = false;
                  DefaultMutableTreeNode viewsNode = null;
                  int count = root.getChildCount();
                  for (int x = 0; x < count; x++) {
                     DefaultMutableTreeNode node = (DefaultMutableTreeNode) root.getChildAt(x);
                     if (node.toString().equals("Views")) {
                        viewsNode = node;

                        //Check whether a leaf node
                        if (viewsNode.isLeaf()) {
                           expand = true;
                        }
                        break;
                     }
                  }

                  if (viewsNode == null) {
                     viewsNode = new DefaultMutableTreeNode("Views");
                     treeModel.insertNodeInto(viewsNode, root, 0);
                     expand = true;
                  }

                  NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                          temp.getName(), temp.getAlias(), temp.getType());
                  DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                  treeModel.insertNodeInto(node, viewsNode, getChildIndex(viewsNode, node));

                  //If the 'Views' node is newly added expand that
                  if (expand) {
                     expandAll(jTree1, new TreePath(treeModel.getPathToRoot(viewsNode)), true);
                  }
               } else if (type == PlsqlBlockType.PACKAGE) { //Parent node

                  NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                          temp.getName(), temp.getAlias(), temp.getType());
                  DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                  lastInsertionIndex = getRootIndex(root, node, lastInsertionIndex);
                  treeModel.insertNodeInto(node, root, lastInsertionIndex);

                  //Remove child nodes if there are in the parent level first
//                  removeChildNodes(root, temp.getChildBlocks());
                  //Add child nodes and expand the node
                  addChildNodes(node, temp.getChildBlocks(), temp.getType());
                  expandAll(jTree1, new TreePath(treeModel.getPathToRoot(node)), true);
               } else if (type == PlsqlBlockType.PACKAGE_BODY) { //Parent node

                  NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                          temp.getName(), temp.getAlias(), temp.getType());
                  DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                  lastInsertionIndex = getRootIndex(root, node, lastInsertionIndex);
                  treeModel.insertNodeInto(node, root, lastInsertionIndex);

                  //Remove child nodes if there are in the parent level first
//                  removeChildNodes(root, temp.getChildBlocks());
                  //Add child nodes and expand the node
                  addChildNodes(node, temp.getChildBlocks(), temp.getType());
                  expandAll(jTree1, new TreePath(treeModel.getPathToRoot(node)), true);
               } else if (type == PlsqlBlockType.FUNCTION_IMPL) { //Leaf node

                  NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                          temp.getName(), temp.getAlias(), temp.getType());
                  DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                  lastInsertionIndex = getRootIndex(root, node, lastInsertionIndex);
                  treeModel.insertNodeInto(node, root, lastInsertionIndex);
               } else if (type == PlsqlBlockType.PROCEDURE_IMPL) { //Leaf node

                  NodeInfo nodeInfo = new NodeInfo(temp.getStartOffset(), temp.getEndOffset(),
                          temp.getName(), temp.getAlias(), temp.getType());
                  DefaultMutableTreeNode node = new DefaultMutableTreeNode(nodeInfo);
                  lastInsertionIndex = getRootIndex(root, node, lastInsertionIndex);
                  treeModel.insertNodeInto(node, root, lastInsertionIndex);
               }
            }
         }
      }

//      jTree1.updateUI();
   }

    /**
     * Opens and focusses on the appropriate location of the document when the
     * user clicks on the navigator.
     * for an example, if the user clicks on a function name in the navigator,
     * the cursor points to the start position of that particular function in
     * the document
     */
    private void openAndFocusElement() {
        BaseDocument bdoc = (BaseDocument) getDocument();
        DataObject dobj = NbEditorUtilities.getDataObject(bdoc);

        if (dobj == null) {
            return;
        }
        final EditorCookie.Observable ec = dobj.getCookie(EditorCookie.Observable.class);

        if (ec == null) {
            return;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                JEditorPane[] panes = ec.getOpenedPanes();
                if (panes != null && panes.length > 0) {

                    // editor already opened, so just select
                    selectElementInPane(panes[0]);
                } else {

                    // editor not opened yet
                    ec.open();
                    try {
                        ec.openDocument(); //wait to editor to open

                        panes = ec.getOpenedPanes();
                        if (panes != null && panes.length > 0) {
                            selectElementInPane(panes[0]);
                        }
                    } catch (IOException ioe) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, ioe);
                    }
                }
            }
        });
    }

    /**
     * Method that will remove the given block
     * @param parentNode
     * @param block
     * @Returns
     */
    private boolean removeBlock(DefaultMutableTreeNode parentNode, PlsqlBlock block) {
        int index = parentNode.getChildCount();
        for (int i = 0; i < index; i++) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) parentNode.getChildAt(i);
            if (node.getUserObject() instanceof NodeInfo) {
                NodeInfo nodeInfo = (NodeInfo) node.getUserObject();
                if ((nodeInfo.name.equals(block.getName())) &&
                        (nodeInfo.startOffset == block.getStartOffset()) &&
                        (nodeInfo.endOffset == block.getEndOffset())) {
                    //we found the match
                    treeModel.removeNodeFromParent(node);
                    return true;
                }
            }

            //If the block that we are looking for is a parent no need to look in to children
            if ((block.getType() != PlsqlBlockType.PACKAGE) || (block.getType() != PlsqlBlockType.PACKAGE_BODY)) {
                if (removeBlock(node, block)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Remove these child blocks if there in the given node as children
     * @param parent
     * @param childBlocks
     */
    private void removeChildNodes(DefaultMutableTreeNode parent, List childBlocks) {
        int count = childBlocks.size();

        for (int i = 0; i < count; i++) {
            PlsqlBlock temp = (PlsqlBlock) childBlocks.get(i);
            removeBlock(parent, temp);
        }
    }

    /**
     * Selects the editor pane which the document exists.
     * @param pane The the editor pane which the document exists.
     */
    private void selectElementInPane(final JEditorPane pane) {
        RequestProcessor.getDefault().post(new Runnable() {

            public void run() {
                BaseDocument bdoc = (BaseDocument) getDocument();
                try {
                    pane.setCaretPosition(Utilities.getRowStart(bdoc, startOffset));
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });

        Container temp = pane;
        while (!(temp instanceof TopComponent)) {
            temp = temp.getParent();
        }
        ((TopComponent) temp).requestActive();
    }

    /**
     * To add the new node to the correct place in alphabetical order get the index
     * @param parent
     * @param child
     * @return
     */
    private int getChildIndex(DefaultMutableTreeNode parent, DefaultMutableTreeNode child) {
        int index = parent.getChildCount();
        //changed to sort by source.
        if (sortBySource) {
            return index++;
        } else {
            for (int i = 0; i < index; i++) {
                DefaultMutableTreeNode temp = (DefaultMutableTreeNode) parent.getChildAt(i);
                if (temp.getUserObject().toString().equals("Views")) {
                    continue;
                }

                if (temp.getUserObject().toString().compareToIgnoreCase(child.getUserObject().toString()) > 0) {
                    index = i;
                    break;
                }
            }
            return index;
        }
    }

    /**
     * To add the new node to the correct place in alphabetical order get the index
     * Parent node is the root node here. There are some things to consider here
     * @param parent
     * @param child
     * @return
     */
    private int getRootIndex(DefaultMutableTreeNode parent, DefaultMutableTreeNode child, int startIndex) {
        int index = parent.getChildCount();
        for (int i = startIndex; i < index; i++) {
            DefaultMutableTreeNode temp = (DefaultMutableTreeNode) parent.getChildAt(i);
            Object userObj = temp.getUserObject();
            Object childUserObj = child.getUserObject();
            //Views node should be the firstn node
            if (userObj.toString().equals("Views")) {
                continue;
            }

            if ((userObj instanceof NodeInfo) && (childUserObj instanceof NodeInfo)) {
                if ((((NodeInfo) userObj).type == PlsqlBlockType.PACKAGE) ||
                        (((NodeInfo) userObj).type == PlsqlBlockType.PACKAGE_BODY)) {
                    if ((((NodeInfo) childUserObj).type != PlsqlBlockType.PACKAGE) &&
                            (((NodeInfo) childUserObj).type != PlsqlBlockType.PACKAGE_BODY)) {
                        continue;
                    }
                }
            }

            //Functions would come sorted last
            if (userObj.toString().compareToIgnoreCase(childUserObj.toString()) > 0) {
                index = i;
                break;
            }
        }

        return index;
    }

    class NavTreeRenderer extends DefaultTreeCellRenderer {

        public NavTreeRenderer() {
        }

        @Override
        public Component getTreeCellRendererComponent(
                JTree tree,
                Object value,
                boolean sel,
                boolean expanded,
                boolean leaf,
                int row,
                boolean hasFocus) {

            super.getTreeCellRendererComponent(
                    tree, value, sel,
                    expanded, leaf, row,
                    hasFocus);
            setIcon(getNodeIcon(value, leaf));

            return this;
        }

        protected Icon getNodeIcon(Object value, boolean leaf) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (node.getUserObject() instanceof NodeInfo) {
                NodeInfo block = (NodeInfo) (node.getUserObject());
                if (PlsqlBlockType.VIEW == block.type) {
                    return viewIcon;
                } else if ((PlsqlBlockType.PROCEDURE_IMPL == block.type) ||
                        (PlsqlBlockType.FUNCTION_IMPL == block.type) ||
                        (PlsqlBlockType.FUNCTION_DEF == block.type) ||
                        (PlsqlBlockType.PROCEDURE_DEF == block.type)) {
                    if (block.name.endsWith("___")) {
                        return implementationMethodIcon;
                    } else if (block.name.endsWith("___")) {
                        return implementationMethodIcon;
                    } else if (block.name.endsWith("__")) {
                        return privateMethodIcon;
                    } else if (block.name.endsWith("_")) {
                        return protectedMethodIcon;
                    } else {
                        return publicMethodIcon;
                    }
                } else if (PlsqlBlockType.PACKAGE == block.type ||
                        PlsqlBlockType.PACKAGE_BODY == block.type) {
                    return pkgIcon;
                }
            }

            if (node.isRoot()) {
                return dbFileIcon;
            } else if ("Views".equalsIgnoreCase(node.getUserObject().toString())) {
                return viewsIcon;
            } else {
                return dbFileIcon; //unknown object...

            }
        }
    }

    /**
     * Private class that holds some node info
     */
    public class NodeInfo {

        public PlsqlBlockType type;
        public String name;
        public String alias;
        public int startOffset;
        public int endOffset;

        private NodeInfo(int start, int end, String name, String alias, PlsqlBlockType type) {
            this.startOffset = start;
            this.endOffset = end;
            this.name = name;
            this.alias = alias;
            this.type = type;
        }

        public String toString() {
            return name;
        }
    }
}
