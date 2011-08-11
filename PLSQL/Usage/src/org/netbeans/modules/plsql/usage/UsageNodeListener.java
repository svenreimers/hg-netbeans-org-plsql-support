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

import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JEditorPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Task;

public class UsageNodeListener implements MouseListener, KeyListener {

   public static Project project;

   @Override
   public void mouseClicked(final MouseEvent event) {
      final JTree tree = (JTree) event.getSource();
      final int row = tree.getRowForLocation(event.getX(), event.getY());
      final TreePath path = tree.getPathForRow(row);
      if (path != null && event.getClickCount() == 2) {
         gotoSource(path);
      }
   }

   private void gotoSource(final TreePath path) {
      final UsageNode node = (UsageNode) path.getLastPathComponent();
      final Object object = node.getUserObject();
      if (object instanceof PlsqlElement) {
         final PlsqlElement element = (PlsqlElement) object;
         findInSource(element.getUsageName(), element);
      }
   }

   @Override
   public void mouseEntered(final MouseEvent event) {
      //ignore
   }

   @Override
   public void mouseExited(final MouseEvent event) {
      //ignore
   }

   @Override
   public void mousePressed(final MouseEvent event) {
      //ignore
   }

   @Override
   public void mouseReleased(final MouseEvent event) {
      //ignore
   }

   @Override
   public void keyPressed(final KeyEvent event) {
      //ignore
   }

   @Override
   public void keyReleased(final KeyEvent event) {
      final int keyCode = event.getKeyCode();
      if (keyCode == KeyEvent.VK_ENTER) {
         // Enter key was pressed, find the reference in document
         final JTree tree = (JTree) event.getSource();
         final TreePath path = tree.getSelectionPath();
         if (path != null) {
            gotoSource(path);
         }
      }
   }

   @Override
   public void keyTyped(final KeyEvent event) {
      //ignore
   }

   public static void findInSource(final String usageName, final PlsqlElement element) {
      final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
      try {
         DataObject dataObj = null;
         if (element.getParent() != null) {
            if ("PACKAGE".equals(element.getObjType())) {
               dataObj = PlsqlUsageUtil.getPackageSpecFromDB(element.getParent(), project);

            } else {
               dataObj = PlsqlUsageUtil.getPackageBodyFromDB(element.getParent(), project);

            }
         }
         final int line_no = (validator.isValidPackage(dataObj)) ? element.getModifiedLine() : element.getLine();
         if (line_no < 0) {
            return;
         }
         final int col_no = element.getColumn();
         final EditorCookie cookie = dataObj.getCookie(EditorCookie.class);
         final Task task = cookie.prepareDocument();
         task.waitFinished(1000);
         if (cookie != null) {
            final BaseDocument document = (BaseDocument) cookie.getDocument();
            final int col_start = Utilities.getRowStartFromLineOffset(document, line_no - 1);
            final OpenCookie openCookie = dataObj.getCookie(OpenCookie.class);
            openCookie.open();
            final JEditorPane[] panes = cookie.getOpenedPanes();
            if (panes.length > 0) {
               final JEditorPane pane = panes[0];
               final int startPosition = col_start + col_no - 1;
               pane.setCaretPosition(startPosition + usageName.length());
               pane.moveCaretPosition(startPosition);
            }
         }
      } catch (Exception e) {
         Exceptions.printStackTrace(e);
      }
   }

   static void selectNextPrev(final boolean next, final JTree tree) {
      final int[] rows = tree.getSelectionRows();
      int newRow = rows == null || rows.length == 0 ? 0 : rows[0];
      int maxcount = tree.getRowCount();
      UsageNode node;
      do {
         if (next) {
            newRow++;
            if (newRow >= maxcount) {
               newRow = 0;
            }
         } else {
            newRow--;
            if (newRow < 0) {
               newRow = maxcount - 1;
            }
         }
         final TreePath path = tree.getPathForRow(newRow);
         node = (UsageNode) path.getLastPathComponent();
         if (!node.isLeaf()) {
            tree.expandRow(newRow);
            maxcount = tree.getRowCount();
         }
      } while (!node.isLeaf());
      tree.setSelectionRow(newRow);
      tree.scrollRowToVisible(newRow);
      final Object object = node.getUserObject();
      if (object instanceof PlsqlElement) {
         final PlsqlElement element = (PlsqlElement) object;
         UsageNodeListener.findInSource(element.getUsageName(), element);
      }
   }
}
