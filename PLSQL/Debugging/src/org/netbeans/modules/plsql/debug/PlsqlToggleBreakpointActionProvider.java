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
package org.netbeans.modules.plsql.debug;

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlBlockUtilities;
import java.awt.EventQueue;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Set;
import java.util.Collections;
import java.util.Locale;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.api.debugger.jpda.MethodBreakpoint;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.WeakListeners;
import org.openide.windows.TopComponent;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ProjectUtils;

public class PlsqlToggleBreakpointActionProvider extends ActionsProviderSupport
      implements PropertyChangeListener {

   private final static Set<Object> ACTIONS = Collections.singleton(ActionsManager.ACTION_TOGGLE_BREAKPOINT);
   private static Project project=null;

   public PlsqlToggleBreakpointActionProvider() {
      setEnabled(ActionsManager.ACTION_TOGGLE_BREAKPOINT, true);
      TopComponent.getRegistry().addPropertyChangeListener(
            WeakListeners.propertyChange(this, TopComponent.getRegistry()));
   }

   public static Project getProject() {
      return project;
   }

   public static void setProject(Project debugProject) {
      project = debugProject;
      //enable all breakpoints in this project, disable all other breakpoints
      Breakpoint[] breakpoints = DebuggerManager.getDebuggerManager().getBreakpoints();
      String projectName = ProjectUtils.getInformation(project).getName();
      for(int i=0; i<breakpoints.length; i++) {
         Breakpoint breakpoint = breakpoints[i];
         if(breakpoint.getGroupName()!=null && breakpoint.getGroupName().equals(projectName)) {
            breakpoint.enable();
         } else {
            breakpoint.disable();
         }
      }
   }

   public void doAction(Object action) {
      Node node = getActivatedPlsqlNode();
      if (node == null)
         return;

      DataObject dataObject = node.getLookup().lookup(DataObject.class);
      EditorCookie editorCookie = node.getLookup().lookup(EditorCookie.class);
      if (dataObject == null || editorCookie == null)
         return;
      project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
      if(project==null) //don't allow debugging outside of projects...
         return;

      JEditorPane editorPane = getEditorPane(editorCookie);
      StyledDocument document = editorCookie.getDocument();
      if (editorPane == null || document == null)
         return;

      int caretDot = editorPane.getCaret().getDot();
      int lineNumber = NbDocument.findLineNumber(document, caretDot);
      if (lineNumber == -1)
         return;

      FileObject fileObject = node.getLookup().lookup(FileObject.class);
      String url;
      try {
         url = fileObject.getURL().toString();
      } catch (FileStateInvalidException ex) {
         Exceptions.printStackTrace(ex);
         return;
      }

      String className = null;
      String methodName = null;
      PlsqlBlock block = PlsqlBlockUtilities.getCurrentBlock(caretDot, dataObject.getLookup().lookup(PlsqlBlockFactory.class).getBlockHierarchy());
      if (block != null && lineNumber == NbDocument.findLineNumber(document, block.getStartOffset())) {
         if (block.getType() == PlsqlBlockType.PROCEDURE_DEF || block.getType() == PlsqlBlockType.PROCEDURE_IMPL)
            methodName = block.getName().toUpperCase(Locale.ENGLISH);
         else if (block.getType() == PlsqlBlockType.FUNCTION_DEF || block.getType() == PlsqlBlockType.FUNCTION_IMPL)
            methodName = block.getName().toUpperCase(Locale.ENGLISH);
         else if (block.getType() == PlsqlBlockType.PACKAGE || block.getType() == PlsqlBlockType.PACKAGE_BODY)
            methodName = ""; // All methods

         if (methodName != null) {
            DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
            if (connectionProvider == null)
               return;
            DatabaseConnection databaseConnection = connectionProvider.getTemplateConnection();
            if (databaseConnection == null)
               return;

            className = PlsqlDebuggerUtilities.getClassName(block, databaseConnection);
            if (className.startsWith("$Oracle.Procedure"))
               methodName = ""; // All methods
         }
      }

      Breakpoint oldBreakpoint = null;
      String[] classNames = new String[] {className};
      for (Breakpoint point : DebuggerManager.getDebuggerManager().getBreakpoints())
         if (className != null && point instanceof MethodBreakpoint) {
            MethodBreakpoint methodBreakpoint = (MethodBreakpoint)point;
            if (methodName.equals(methodBreakpoint.getMethodName())
                  && Arrays.deepEquals(classNames, methodBreakpoint.getClassFilters())) {
               oldBreakpoint = methodBreakpoint;
               break;
            }
         } else if (point instanceof LineBreakpoint) {
            LineBreakpoint lineBreakpoint = (LineBreakpoint)point;
            if (lineBreakpoint.getURL().equals(url) &&
                  lineBreakpoint.getLineNumber() == lineNumber + 1) {
               oldBreakpoint = lineBreakpoint;
               break;
            }
         }

      if (oldBreakpoint != null)
         DebuggerManager.getDebuggerManager().removeBreakpoint(oldBreakpoint);
      else {
         Breakpoint breakpoint = null;
         if (className != null)
            breakpoint = MethodBreakpoint.create(className, methodName);
         else
            breakpoint = LineBreakpoint.create(url, lineNumber + 1);
         breakpoint.setGroupName(ProjectUtils.getInformation(project).getName());
         DebuggerManager.getDebuggerManager().addBreakpoint(breakpoint);
      }
   }

   public Set getActions() {
      return ACTIONS;
   }

   public void propertyChange(PropertyChangeEvent evt) {
      setEnabled(ActionsManager.ACTION_TOGGLE_BREAKPOINT, getActivatedPlsqlNode() != null);
   }

   private static Node getActivatedPlsqlNode() {
      Node[] nodes = TopComponent.getRegistry().getCurrentNodes();
      if (nodes != null && nodes.length == 1) {
         FileObject file = nodes[0].getLookup().lookup(FileObject.class);
         if (file != null && file.getMIMEType().equals("text/x-plsql"))
            return nodes[0];
      }
      return null;
   }

   private static JEditorPane getEditorPane(final EditorCookie editorCookie) {
      if (SwingUtilities.isEventDispatchThread()) {
         JEditorPane[] panes = editorCookie.getOpenedPanes();
         return panes != null && panes.length == 1 ? panes[0] : null;
      } else {
         final JEditorPane[] paneArray = new JEditorPane[1];
         try {
            EventQueue.invokeAndWait(new Runnable() {
               public void run() {
                  JEditorPane[] panes = editorCookie.getOpenedPanes();
                  paneArray[0] = panes != null && panes.length == 1 ? panes[0] : null;
               }
            });
         } catch (InvocationTargetException ex) {
            ex.printStackTrace();
         } catch (InterruptedException ex) {
            ex.printStackTrace();
         }
         return paneArray[0];
      }
   }
}
