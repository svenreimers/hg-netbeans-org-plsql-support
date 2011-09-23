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

import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.hyperlink.PlsqlGoToDbImplAction;
import org.netbeans.modules.plsql.hyperlink.PlsqlGoToSpecAction;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockUtilities;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.debugger.jpda.JPDAThread;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.spi.debugger.jpda.EditorContext;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.URLMapper;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.util.WeakListeners;
import org.netbeans.api.project.Project;
import org.openide.cookies.LineCookie;
import org.openide.text.Annotation;
import org.openide.text.Line;
import org.openide.util.actions.SystemAction;

public class PlsqlEditorContext extends EditorContext {

   private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
   private PropertyChangeListener dispatchListener = new EditorContextDispatchListener();
   private EditorContextDispatcher contextDispatcher = EditorContextDispatcher.getDefault();
   private static final String PACKAGE_BODY_NAMESPACE = "/$Oracle/PackageBody/";
   private static final String PACKAGE_NAMESPACE = "/$Oracle/Package/";
   private static final String FUNCTION_NAMESPACE = "/$Oracle/Function/";
   private static final String PROCEDURE_NAMESPACE = "/$Oracle/Procedure/";

   public PlsqlEditorContext() {
      contextDispatcher.addPropertyChangeListener("text/x-plsql",
              WeakListeners.propertyChange(dispatchListener, contextDispatcher));
   }

   @Override
   public boolean showSource(final String url, final int lineNumber, final Object timeStamp) {
      if (!SwingUtilities.isEventDispatchThread()) {
         try {
            SwingUtilities.invokeAndWait(new Runnable() {

               @Override
               public void run() {
                  showSource(url, lineNumber, timeStamp);
               }
            });
         } catch (InterruptedException e) {
            return false;
         } catch (InvocationTargetException e) {
            return false;
         }
      } else {
         if (url.contains(PACKAGE_NAMESPACE)) {
            return goToPackage(extractClassName(url, PACKAGE_NAMESPACE), extractMethodName(url, PACKAGE_NAMESPACE), lineNumber, PlsqlToggleBreakpointActionProvider.getProject());
         } else if (url.contains(PACKAGE_BODY_NAMESPACE)) {
            return goToPackageBody(extractClassName(url, PACKAGE_BODY_NAMESPACE), extractMethodName(url, PACKAGE_BODY_NAMESPACE), lineNumber, PlsqlToggleBreakpointActionProvider.getProject());
         } else if (url.contains(FUNCTION_NAMESPACE)) {
            return goToFunction(extractClassName(url, FUNCTION_NAMESPACE), lineNumber, PlsqlToggleBreakpointActionProvider.getProject());
         } else if (url.contains(PROCEDURE_NAMESPACE)) {
            return goToProcedure(extractClassName(url, PROCEDURE_NAMESPACE), lineNumber, PlsqlToggleBreakpointActionProvider.getProject());
         }
      }
      return false;
   }

   private String extractMethodName(String url, String namespace) {
      return null;
   }

   private String extractClassName(String url, String namespace) {
      int startPos = url.indexOf("/", url.indexOf(namespace) + namespace.length()) + 1;
      int endPos = url.indexOf("/", startPos);
      if (endPos == -1) {
         endPos = url.indexOf(".", startPos);
      }
      return url.substring(startPos, endPos);
   }

   private boolean goToProcedure(String methodName, int lineNumber, Project project) {
      PlsqlGoToDbImplAction action = SystemAction.get(PlsqlGoToDbImplAction.class);
      return action.goToProcedure(methodName, project, lineNumber);
   }

   private boolean goToFunction(String methodName, int lineNumber, Project project) {
      PlsqlGoToDbImplAction action = SystemAction.get(PlsqlGoToDbImplAction.class);
      return action.goToFunction(methodName, project, lineNumber);
   }

   private boolean goToPackageBody(String packageName, String methodName, int lineNumber, Project project) {
      PlsqlGoToDbImplAction action = SystemAction.get(PlsqlGoToDbImplAction.class);
      return action.goToPackage(packageName, project, methodName, lineNumber);
   }

   private boolean goToPackage(String packageName, String methodName, int lineNumber, Project project) {
      PlsqlGoToSpecAction action = SystemAction.get(PlsqlGoToSpecAction.class);
      return action.goToPackage(packageName, project, methodName, lineNumber);
   }

   @Override
   public void createTimeStamp(Object timeStamp) {
   }

   @Override
   public void disposeTimeStamp(Object timeStamp) {
   }

   @Override
   public void updateTimeStamp(Object timeStamp, String url) {
   }

   @Override
   /**
    * Removes given annotation.
    *
    * @return true if annotation has been successfully removed
    */
   public void removeAnnotation(
           Object a) {
      if (a instanceof Collection) {
         Collection annotations = ((Collection) a);
         for (Iterator it = annotations.iterator(); it.hasNext();) {
            removeSingleAnnotation((Annotation) it.next());
         }
      } else {
         removeSingleAnnotation((Annotation) a);
      }
   }

   private void removeSingleAnnotation(Annotation annotation) {
      annotation.detach();
   }

   @Override
   public int getLineNumber(Object annotation, Object timeStamp) {
      //return the actual Oracle object line number - not the line number in the file...
      DataObject dataObject = null;
      int lineNumber = -1;
      if (annotation instanceof LineBreakpoint) {
         LineBreakpoint breakpoint = (LineBreakpoint) annotation;
         dataObject = getDataObject(breakpoint.getURL());
         lineNumber = breakpoint.getLineNumber();
      }
      if (dataObject == null) {
         return -1;
      }
      EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
      if (editorCookie == null) {
         return -1;
      }
      return getOracleLineNumber(dataObject, editorCookie.getDocument(), lineNumber);
   }

   @Override
   public int getCurrentLineNumber() {
      return contextDispatcher.getCurrentLineNumber();
   }

   public String getCurrentClassDeclaration() {
      return null;
   }

   @Override
   public String getCurrentClassName() {
      FileObject file = contextDispatcher.getCurrentFile();
      JEditorPane editor = contextDispatcher.getCurrentEditor();
      if (file == null || editor == null) {
         return "";
      }

      DataObject dataObject = null;
      try {
         dataObject = DataObject.find(file);
      } catch (DataObjectNotFoundException ex) {
         Exceptions.printStackTrace(ex);
         return "";
      }

      StyledDocument document = dataObject.getLookup().lookup(EditorCookie.class).getDocument();
      int caretDot = editor.getCaret().getDot();
      int lineNumber = NbDocument.findLineNumber(document, caretDot);
      if (lineNumber == -1) {
         return "";
      }

      String className = getClassName(dataObject, document, lineNumber + 1);
      return className != null ? className : "";
   }

   @Override
   public String getCurrentURL() {
      return contextDispatcher.getCurrentURLAsString();
   }

   @Override
   public String getCurrentMethodName() {
      return "";
   }

   @Override
   public String getCurrentFieldName() {
      return null;
   }

   @Override
   public String getSelectedIdentifier() {
      return null;
   }

   @Override
   public String getSelectedMethodName() {
      return "";
   }

   @Override
   public int getFieldLineNumber(String url, String className, String fieldName) {
      return -1;
   }

   @Override
   public String getClassName(String url, int lineNumber) {
      DataObject dataObject = getDataObject(url);
      if (dataObject == null) {
         return null;
      }
      EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
      if (editorCookie == null) {
         return null;
      }
      return getClassName(dataObject, editorCookie.getDocument(), lineNumber);
   }

   @Override
   public String[] getImports(String url) {
      return new String[0];
   }

   @Override
   public Operation[] getOperations(String url, int lineNumber, BytecodeProvider bytecodeProvider) {
      return null;
   }

   @Override
   public void addPropertyChangeListener(PropertyChangeListener l) {
      changeSupport.addPropertyChangeListener(l);
   }

   @Override
   public void removePropertyChangeListener(PropertyChangeListener l) {
      changeSupport.removePropertyChangeListener(l);
   }

   @Override
   public void addPropertyChangeListener(String propertyName, PropertyChangeListener l) {
      changeSupport.addPropertyChangeListener(propertyName, l);
   }

   @Override
   public void removePropertyChangeListener(String propertyName, PropertyChangeListener l) {
      changeSupport.removePropertyChangeListener(propertyName, l);
   }

   private static DataObject getDataObject(String url) {
      FileObject file;
      try {
         file = URLMapper.findFileObject(new URL(url));
      } catch (MalformedURLException e) {
         return null;
      }

      if (file == null) {
         return null;
      }
      try {
         return DataObject.find(file);
      } catch (DataObjectNotFoundException ex) {
         return null;
      }
   }

   private static String getClassName(DataObject dataObject, StyledDocument document, int lineNumber) {
      PlsqlBlockFactory blockFactory = dataObject.getLookup().lookup(PlsqlBlockFactory.class);
      if (blockFactory == null) {
         return null;
      }

      int offset = NbDocument.findLineOffset(document, lineNumber - 1);
      PlsqlBlock block = PlsqlBlockUtilities.getCurrentBlock(offset, blockFactory.getBlockHierarchy());
      if (block == null) {
         return null;
      }

      DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
      if (connectionProvider == null) {
         return null;
      }
      DatabaseConnection databaseConnection = connectionProvider.getTemplateConnection();
      if (databaseConnection == null) {
         return null;
      }

      return PlsqlDebuggerUtilities.getClassName(block, databaseConnection);
   }

   private static int getOracleLineNumber(DataObject dataObject, StyledDocument document, int lineNumber) {
      PlsqlBlockFactory blockFactory = dataObject.getLookup().lookup(PlsqlBlockFactory.class);
      if (blockFactory == null) {
         return -1;
      }

      int offset = NbDocument.findLineOffset(document, lineNumber - 1);
      PlsqlBlock block = PlsqlBlockUtilities.getCurrentBlock(offset, blockFactory.getBlockHierarchy());
      if (block == null) {
         return -1;
      }

      //find the start of the package/procedure
      while (block.getParent() != null) {
         block = block.getParent();
      }
      return lineNumber - NbDocument.findLineNumber(document, block.getStartOffset());
   }

   private class EditorContextDispatchListener extends Object implements PropertyChangeListener {

      public EditorContextDispatchListener() {
      }

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
         changeSupport.firePropertyChange(org.openide.windows.TopComponent.Registry.PROP_CURRENT_NODES, null, null);
      }
   }

   @Override
   public Object annotate(
           String url,
           int lineNumber,
           String annotationType,
           Object timeStamp) {
      return annotate(url, lineNumber, annotationType, timeStamp, null);
   }

   private void getDataObject(final String url, final int lineNo, final List result) {
      if (!SwingUtilities.isEventDispatchThread()) {
         try {
            SwingUtilities.invokeAndWait(new Runnable() {

               @Override
               public void run() {
                  getDataObject(url, lineNo, result);
               }
            });
         } catch (InterruptedException e) {
            return;
         } catch (InvocationTargetException e) {
            return;
         }
      } else {
         DataObject dataObject = null;
         int offset = 0;
         Project project = PlsqlToggleBreakpointActionProvider.getProject();
         if (url.contains(PACKAGE_NAMESPACE)) {
            PlsqlGoToSpecAction action = SystemAction.get(PlsqlGoToSpecAction.class);
            dataObject = action.getPackageDataObject(extractClassName(url, PACKAGE_NAMESPACE), project);
            offset = action.getPackageOffset(extractClassName(url, PACKAGE_NAMESPACE), dataObject);
         } else if (url.contains(PACKAGE_BODY_NAMESPACE)) {
            PlsqlGoToDbImplAction action = SystemAction.get(PlsqlGoToDbImplAction.class);
            dataObject = action.getPackageDataObject(extractClassName(url, PACKAGE_BODY_NAMESPACE), project, true);
            offset = action.getPackageBodyOffset(extractClassName(url, PACKAGE_BODY_NAMESPACE), dataObject);
         } else if (url.contains(FUNCTION_NAMESPACE)) {
            PlsqlGoToDbImplAction action = SystemAction.get(PlsqlGoToDbImplAction.class);
            dataObject = action.getMethodDataObject(extractClassName(url, FUNCTION_NAMESPACE), FUNCTION, project);
         } else if (url.contains(PROCEDURE_NAMESPACE)) {
            PlsqlGoToDbImplAction action = SystemAction.get(PlsqlGoToDbImplAction.class);
            dataObject = action.getMethodDataObject(extractClassName(url, PROCEDURE_NAMESPACE), PROCEDURE, project);
         }
         if (dataObject != null) {
            result.add(new Integer(lineNo + offset));
            result.add(dataObject);
         }
      }
   }

   @Override
   public Object annotate(final String url,
           int lineNumber,
           String annotationType,
           Object timeStamp,
           JPDAThread thread) {
      if (url == null) {
         return null;
      }
      DataObject dataObject = getDataObject(url);
      if (dataObject == null) {
         final List result = new ArrayList();
         getDataObject(url, lineNumber, result);
         if (result.size() > 0) {
            lineNumber = ((Integer) result.get(0)).intValue();
            dataObject = (DataObject) result.get(1);
         }
      }
      if (dataObject == null || lineNumber < 1) {
         return null;
      }
      LineCookie lc = dataObject.getCookie(LineCookie.class);
      if (lc == null) {
         return null;
      }
      Line line = lc.getLineSet().getCurrent(lineNumber - 1);
      return new DebuggerAnnotation(annotationType, line, thread);
   }
}
