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

import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Task;
import org.openide.util.actions.CookieAction;

public final class FindUsagesAction extends CookieAction implements Runnable {

   private static final RequestProcessor RP = new RequestProcessor(FindUsagesAction.class.getName(), 2);
   private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
   private String selectedName = "";
   private String packageName = "";
   private String signature = "";
   private int usageCount = 0;
   private List<String> usageFiles = new ArrayList<String>();
   private boolean isFunctionOrProcedure = false;
   private String fileExt = "";
   private String currentPackageName = "";
   private String object_type = "";
   private String context = "";
   private Node[] activatedNodes;

   @Override
   protected void performAction(final Node[] activatedNodes) {
      this.activatedNodes = activatedNodes;
      prepare(activatedNodes);
      RP.post(this);
   }

    @Override
    public void run() {
        final DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);

        final ProgressHandle progressHandle = ProgressHandleFactory.createHandle("Searching for usages...");

        if (dataObject != null) {

            final Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
            if (project != null) {
                final DatabaseConnectionManager provider = DatabaseConnectionManager.getInstance(dataObject);
                final DatabaseConnection connection = provider.getPooledDatabaseConnection(false, true);

                if (connection == null || connection.getJDBCConnection() == null) {
                    return;
                }

                    try {
                        progressHandle.start();

                        signature = getSignature(connection, packageName, object_type, context, false);

                        final List<PlsqlElement> list = getUsageList(connection, project);
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                PlsqlUsagePanel up = new PlsqlUsagePanel(list, selectedName, usageCount, project);
                            }
                        });

                    } catch (SQLException ex) {
                        if (provider.testConnection(connection)) {
                            final NotifyDescriptor d = new NotifyDescriptor.Message("Find usages not supported in this database. Please refer to the documentation for details.",
                                    NotifyDescriptor.INFORMATION_MESSAGE);
                            DialogDisplayer.getDefault().notify(d);
                        } else {
                            final NotifyDescriptor d = new NotifyDescriptor.Message("You are not connected to a database.",
                                    NotifyDescriptor.INFORMATION_MESSAGE);
                            DialogDisplayer.getDefault().notify(d);
                        }
                    } finally {
                        provider.releaseDatabaseConnection(connection);
                        progressHandle.finish();
                    }
            }
        }
    }

   @Override
   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   @Override
   public String getName() {
      return NbBundle.getMessage(FindUsagesAction.class, "CTL_FindUsagesAction");
   }

   @Override
   protected Class[] cookieClasses() {
      return new Class[]{EditorCookie.class};
   }

   @Override
   protected void initialize() {
      super.initialize();
      // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
      putValue("noIconInMenu", Boolean.TRUE);
   }

   @Override
   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   @Override
   protected boolean asynchronous() {
      return false;
   }

   @Override
   protected boolean enable(final Node[] activatedNodes) {
      if (!super.enable(activatedNodes)) {
         return false;
      }
      DataObject dataObj = activatedNodes[0].getLookup().lookup(DataObject.class);
      DatabaseConnectionManager provider = DatabaseConnectionManager.getInstance(dataObj);
      return provider != null;
   }

   protected boolean prepare(final Node[] activatedNodes) {
      if (!super.enable(activatedNodes)) {
         return false;
      }
      EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
      if (editorCookie == null) {
         return false;
      }
      DataObject dataObj = activatedNodes[0].getLookup().lookup(DataObject.class);

      if (editorCookie != null && dataObj != null) {

         Project project = FileOwnerQuery.getOwner(dataObj.getPrimaryFile());//activatedNodes[0].getLookup().lookup(Project.class);
         if (project != null) {
            DatabaseConnectionManager provider = DatabaseConnectionManager.getInstance(dataObj);
            if (provider == null) {
               return false;
            }
            selectedName = "";
            signature = "";
            packageName = "";
            usageFiles.clear();
            usageCount = 0;
            isFunctionOrProcedure = false;
            fileExt = dataObj.getPrimaryFile().getExt();
            currentPackageName = getPackageName(dataObj);
            object_type = "";
            context = "";
            try {
               int offset = -1;

               JEditorPane[] panes = editorCookie.getOpenedPanes();
               if ((panes != null) && (panes.length != 0)) {
                  Caret caret = panes[0].getCaret();
                  offset = caret.getDot();
                  if (offset == caret.getMark()) {
                     return false;
                  } else {
                     selectedName = panes[0].getSelectedText();
                  }


                  if (offset != -1) {

                     int line_no = NbEditorUtilities.getLine(panes[0].getDocument(), offset, false).getLineNumber();
                     int selectionStart = panes[0].getSelectionStart();

                     int col_start = Utilities.getRowStartFromLineOffset((BaseDocument) panes[0].getDocument(), line_no);
                     int col_no = selectionStart - col_start;
                     if ("(".equals(panes[0].getText(panes[0].getSelectionEnd(), 1))) {
                        isFunctionOrProcedure = true;
                     }
                     context = getSelectionContext(offset, dataObj);
                     if (selectedName.contains("\n") || selectedName.contains("\r")) {
                        selectedName = context;
                     }
                     if (context.equals(selectedName))//need to check further
                     {
                        isFunctionOrProcedure = true;
                     }

                     if (isFunctionOrProcedure && selectionStart > 0 && ".".equals(panes[0].getText(selectionStart - 1, 1))) {
                        StringBuilder sb = new StringBuilder(panes[0].getDocument().getText(0, selectionStart));

                        int endIndex = getPackageStartIndex(sb);
                        int length = (selectionStart - 1) - (endIndex + 1);
                        packageName = panes[0].getText(endIndex + 1, length);
                        if (packageName.contains("&")) {
                           packageName = replacePackageName(editorCookie.getDocument(), packageName.substring(0, packageName.length() - 1));
                        }
                     }

                     int dotIndex = selectedName.indexOf('.');
                     if (dotIndex > 0) {
                        selectedName = selectedName.substring(dotIndex + 1);
                        col_no = col_no + dotIndex + 1;
                     }

                     object_type = getPackageType(dataObj);
                     if ("UNSUPPORTED".equals(object_type)) {
                        return false;
                     }
                  }
               }
            } catch (BadLocationException ex) {
               Exceptions.printStackTrace(ex);
            }
            return true;
         }
      }
      return false;
   }

   protected List<PlsqlElement> getUsageList(final DatabaseConnection connection, final Project project) throws SQLException {
      ResultSet rs = null;
      PreparedStatement stmt = null;
      final List<PlsqlElement> list = new ArrayList<PlsqlElement>();
      try {
         String sqlSelect = "SELECT t.name, t.object_name, t.object_type , t.col, t.line, u.text FROM all_identifiers t, user_source u WHERE t.line = u.line AND t.object_name = u.name AND t.object_type = u.type AND t.signature='" + signature + "' AND t.usage !='DEFINITION' AND t.usage !='DECLARATION' ORDER BY t.line,t.col";
         stmt = connection.getJDBCConnection().prepareStatement(sqlSelect);
         rs = stmt.executeQuery();
         while (rs.next()) {

            DataObject dataObj = null;
            if (currentPackageName != null && currentPackageName.equals(rs.getString(2)) && isSameFile(rs.getString(2))) {
               dataObj = ("PACKAGE".equals(rs.getString(3))) ? getOpenedSpecDataObject(rs.getString(2), project) : getOpenedBodyDataObject(rs.getString(2), project);
            } else {
               dataObj = ("PACKAGE".equals(rs.getString(3))) ? getPackageSpecDataObject(rs.getString(2), project) : getPackageBodyDataObject(rs.getString(2), project);
            }

            final String parentName = rs.getString(2);
            final int line_no = rs.getInt(5);
            if (!usageFiles.contains(parentName)) {
               final PlsqlElement parentElement = new PlsqlElement(rs.getString(1), parentName, true, rs.getString(2), rs.getString(3), rs.getInt(4), rs.getInt(5)/*, null*/);
               list.add(parentElement);
               usageFiles.add(parentName);
            }

            PlsqlElement plsqlElement = new PlsqlElement(rs.getString(1), false, parentName, rs.getString(3)/*, dataObj*/);
            boolean correctLine = false;
            boolean isPackageBody = ("PACKAGE BODY".equals(rs.getString(3))) ? true : false;
            if (validator.isValidPackage(dataObj)) {
               correctLine = true;
            }
            plsqlElement.setLine(line_no);
            String lineTxt = (dataObj != null) ? getLineText(dataObj, line_no, correctLine, plsqlElement, rs.getString(2), isPackageBody, true) : rs.getString(6);
            boolean isLineDifferent = (lineTxt.contains("&")) ? true : false;

            int column = rs.getInt(4) - 1;
            if (isLineDifferent) {
               if (column != 0) {
                  if ((column + selectedName.length()) < lineTxt.length()) {
                     if (!selectedName.equals(lineTxt.substring(column, column + selectedName.length()))) {
                        column = getModifiedColumnNo(lineTxt.length(), rs.getInt(4), rs.getString(2), rs.getString(3), line_no, plsqlElement, project, rs.getString(6)) - 1;
                     }
                  } else {
                     column = getModifiedColumnNo(lineTxt.length(), rs.getInt(4), rs.getString(2), rs.getString(3), line_no, plsqlElement, project, rs.getString(6)) - 1;
                  }
               }
            }

            if (column == 0) {
               lineTxt = lineTxt.substring(column).replaceFirst(selectedName, "<b>" + selectedName + "</b>");
            } else {
               if (lineTxt.length() > column && column > 0) {
                  lineTxt = lineTxt.substring(0, column - 1) + lineTxt.substring(column - 1).replaceFirst(selectedName, "<b>" + selectedName + "</b>");
               }
            }
            int newColumn = lineTxt.indexOf("<b>") + 1;
            if (lineTxt != null) {
               lineTxt = lineTxt.trim();
            }
            plsqlElement.setName(lineTxt);
            plsqlElement.setColumn(newColumn);
            list.add(plsqlElement);
            usageCount++;
         }
      } finally {
         if (stmt != null) {
            stmt.close();
         }
      }
      return list;
   }

    protected String getSignature(final DatabaseConnection connection, final String packageName, final String object_type, final String context, final boolean typeKnown) throws SQLException {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        String sqlSelect = null;
        try {
            sqlSelect = "select a.signature, a.type from all_identifiers a, all_identifiers b "
                    + "where a.USAGE_CONTEXT_ID = b.USAGE_ID "
                    + "AND a.name='" + selectedName.toUpperCase(Locale.ENGLISH) + "' "
                    + "AND a.Object_Name='" + packageName.toUpperCase(Locale.ENGLISH) + "' "
                    + "AND a.OBJECT_NAME = b.OBJECT_NAME "
                    + "AND a.object_type='" + object_type.toUpperCase(Locale.ENGLISH) + "' "
                    + "AND a.OBJECT_TYPE =  b.OBJECT_TYPE "
                    + "AND (b.usage = 'DEFINITION' OR b.usage = 'DECLARATION') "
                    + ((isFunctionOrProcedure) ? "AND (a.type ='FUNCTION' OR a.type='PROCEDURE')" : "AND b.name = '" + context.toUpperCase(Locale.ENGLISH) + "'");

            stmt = connection.getJDBCConnection().prepareStatement(sqlSelect);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("SIGNATURE");
            } else if (!isFindUsagesEnabled(connection)) {
                final NotifyDescriptor d = new NotifyDescriptor.Message("Find usages disabled. The database setting PLSCOPE_SETTINGS must be set to 'IDENTIFIERS:ALL' to enable the functionality.",
                        NotifyDescriptor.INFORMATION_MESSAGE);
                DialogDisplayer.getDefault().notify(d);
            }
        } finally {
            if (stmt != null) {
                stmt.close();
            }
        }
        return null;
    }
   
    private boolean isFindUsagesEnabled(DatabaseConnection connection) {
        ResultSet rs = null;
        PreparedStatement stmt = null;
        String setting = null;

        try {
            String sqlSelect = "SELECT PLSCOPE_SETTINGS FROM ALL_PLSQL_OBJECT_SETTINGS WHERE NAME = \'SECURITY_SYS\' AND TYPE = \'PACKAGE\'";
            stmt = connection.getJDBCConnection().prepareStatement(sqlSelect);
            rs = stmt.executeQuery();
            if (rs.next()) {
                setting = rs.getString(1);
            }

            if (setting != null && setting.equalsIgnoreCase("IDENTIFIERS:ALL")) {
                return true;
            }
        } catch (SQLException ex) {
            // do nothing
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                       // do nothing
                }
            }
        }
        return false;
    }

   private String getPackageName(final DataObject dataObject) {
      String packName = "";
      final PlsqlBlockFactory blockFac = getBlockFactory(dataObject);
      if (blockFac != null) {
         final List blockHier = blockFac.getBlockHierarchy();
         for (int i = 0; i < blockHier.size(); i++) {
            final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
            if (temp.getType() == PlsqlBlockType.PACKAGE_BODY || temp.getType() == PlsqlBlockType.PACKAGE) {
               packName = temp.getName();
               break;
            }
         }
      }
      return packName;
   }

   private String getPackageType(final DataObject dataObject) {
      if (validator.isValidPackageSpec(dataObject)) {
         return "PACKAGE";
      } else if (validator.isValidPackageBody(dataObject)) {
         return "PACKAGE BODY";
      } else {
         return "UNSUPPORTED";
      }
   }

   private boolean isSameFile(final String packageType) {
      if ((validator.isValidPackageSpec(fileExt)) && "PACKAGE".equals(packageType)) {
         return true;
      } else if ((validator.isValidPackageBody(fileExt)) && "PACKAGE BODY".equals(packageType)) {
         return true;
      } else {
         return false;
      }
   }

   public DataObject getPackageBodyDataObject(final String packageName, final Project project) {
      try {
         final DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
         final DatabaseConnection dbConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(true) : null;
         try {
            DataObject data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE, project);
            if (data == null) //check to see if the user has a local version of this file...
            {
               data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE, project);
            }
            if (data == null) //fetch file from database (or cache)
            {
               data = null;//PlsqlFileUtil.fetchAsTempFile(packageName, "PACKAGE BODY", connection, project, null);
            }
            return data;
         } finally {
            if (dbConnectionProvider != null) {
               dbConnectionProvider.releaseDatabaseConnection(dbConnection);
            }
         }
      } catch (NotConnectedToDbException ex) {
         Exceptions.printStackTrace(ex);
      }
      return null;
   }

   public DataObject getPackageSpecDataObject(String packageName, Project project) {
      try {
         DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
         DatabaseConnection dbConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(true) : null;
         try {
            DataObject data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE, project);
            if (data == null) //check to see if the user has a local version of this file...
            {
               data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE, project);
            }
            if (data == null) //fetch file from database (or cache)
            {
               data = null;//PlsqlFileUtil.fetchAsTempFile(packageName, "PACKAGE", connection, project, null);
            }
            return data;
         } finally {
            if (dbConnectionProvider != null) {
               dbConnectionProvider.releaseDatabaseConnection(dbConnection);
            }
         }
      } catch (NotConnectedToDbException ex) {
         Exceptions.printStackTrace(ex);
      }
      return null;
   }

   public DataObject getOpenedSpecDataObject(String packageName, Project project) {
      try {
         DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
         DatabaseConnection dbConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(true) : null;
         try {
            DataObject data = null;
            data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE, project);
            return data;
         } finally {
            if (dbConnectionProvider != null) {
               dbConnectionProvider.releaseDatabaseConnection(dbConnection);
            }
         }
      } catch (NotConnectedToDbException ex) {
         Exceptions.printStackTrace(ex);
      }
      return null;
   }

   public DataObject getOpenedBodyDataObject(String packageName, Project project) {
      try {
         DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
         DatabaseConnection dbConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(true) : null;
         try {
            DataObject data = null;
            data = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE_BODY, project);
            return data;
         } finally {
            dbConnectionProvider.releaseDatabaseConnection(dbConnection);
         }
      } catch (NotConnectedToDbException ex) {
         Exceptions.printStackTrace(ex);
      }
      return null;
   }

   private String getLineText(final DataObject dataObject, int line_no, final boolean correctLine, final PlsqlElement element, final String context, final boolean isPkgBody, final boolean setLine) {
      try {
         if (line_no < 1 || dataObject == null) {
            return null;
         }
         final EditorCookie cookie = dataObject.getCookie(EditorCookie.class);
         final Task task = cookie.prepareDocument();
         task.waitFinished(1000);
         if (cookie != null) {
            final BaseDocument document = (BaseDocument) cookie.getDocument();
            if (correctLine) {
               if (isPkgBody) {
                  line_no = getModifiedLineNo(cookie.getDocument(), dataObject, line_no, context, true);
               } else {
                  line_no = getModifiedLineNo(cookie.getDocument(), dataObject, line_no, context, false);
               }
            }
            if (setLine) {
               element.setModifiedLine(line_no);
            }
            final int col_start = Utilities.getRowStartFromLineOffset(document, line_no - 1);
            return NbEditorUtilities.getLine(cookie.getDocument(), col_start, false).getText();
         }
      } catch (Exception e) {
         Exceptions.printStackTrace(e);
      }
      return null;
   }

   private String getSelectionContext(final int offset, final DataObject dataObject) {
      final PlsqlBlockFactory blockFac = getBlockFactory(dataObject);
      int startOffset = -1;
      int endOffset = -1;
      String contxt = "";
      if (blockFac != null) {
         final List blockHier = blockFac.getBlockHierarchy();
         for (int i = 0; i < blockHier.size(); i++) {
            final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
            startOffset = temp.getStartOffset();
            endOffset = temp.getEndOffset();

            if (startOffset < offset && endOffset > offset) {
               contxt = temp.getName();
               if ("".equals(packageName)) {
                  packageName = contxt;
               }
               String newContext = getMatchingChild(offset, temp, contxt);
               contxt = (newContext != null) ? newContext : contxt;
               break;
            }
         }
      }
      return contxt;
   }

   private String getMatchingChild(final int offset, final PlsqlBlock block, String context) {
      if (block.getChildCount() > 0) {
         final List<PlsqlBlock> tempChildren = block.getChildBlocks();
         final Iterator<PlsqlBlock> iterator = tempChildren.iterator();
         while (iterator.hasNext()) {
            final PlsqlBlock current = iterator.next();
            if (current.getStartOffset() < offset && current.getEndOffset() > offset) {
               final String tempContext = (isValidParent(current)) ? current.getName() : context;

               final String childContext = getMatchingChild(offset, current, tempContext);
               context = (childContext != null) ? childContext : tempContext;
               break;
            }
         }
      }
      return context;
   }

   private boolean isValidParent(final PlsqlBlock parent) {
      final PlsqlBlockType type = parent.getType();
      if (type == PlsqlBlockType.PACKAGE || type == PlsqlBlockType.PACKAGE_BODY || type == PlsqlBlockType.FUNCTION_DEF
              || type == PlsqlBlockType.FUNCTION_IMPL || type == PlsqlBlockType.PROCEDURE_DEF || type == PlsqlBlockType.PROCEDURE_IMPL) {
         return true;
      } else {
         return false;
      }
   }

   private int getPackageStartIndex(final StringBuilder text) {
      int index = text.lastIndexOf(" ");
      if (index < text.lastIndexOf("\t")) {
         index = text.lastIndexOf("\t");
      }
      if (index < text.lastIndexOf("\n")) {
         index = text.lastIndexOf("\n");
      }
      if (index < text.lastIndexOf("(")) {
         index = text.lastIndexOf("(");
      }
      return index;
   }

   private int getModifiedLineNo(final Document doc, final DataObject dataObject, int line_no, final String context, final boolean isPackageBody) {
      int lineOffset = -1;
      final PlsqlBlockType blockType = (isPackageBody) ? PlsqlBlockType.PACKAGE_BODY : PlsqlBlockType.PACKAGE;

      final PlsqlBlockFactory blockFac = getBlockFactory(dataObject);
      blockFac.initHierarchy(doc);
      if (blockFac != null) {
         final List blockHier = blockFac.getBlockHierarchy();
         for (int i = 0; i < blockHier.size(); i++) {
            final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
            if (temp.getType() == blockType) { //PlsqlBlockType.PACKAGE_BODY || temp.getType() == PlsqlBlockType.PACKAGE) {
               if (temp.getName().equals(context)) {
                  lineOffset = NbEditorUtilities.getLine(doc, temp.getStartOffset(), false).getLineNumber();
                  if (lineOffset > 0) {
                     line_no += lineOffset;
                  }
                  break;
               }
            }
         }
      }
      return line_no;
   }
///change this to add another parameter for get textline from database file

   private int getModifiedColumnNo(final int currentLength, int column, final String objName, final String objType, final int lineNo, final PlsqlElement plsqlElement, final Project project, final String dbText) {
      final String lineTxt = dbText;
      final int originalLength = lineTxt.length();
      if (currentLength < originalLength) {
         column -= (originalLength - currentLength);
      }

      return column;
   }

   private String replacePackageName(final Document doc, final String tokenImage) {
      final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         ts.moveStart();
         while (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            final PlsqlTokenId tokenID = token.id();

            if (tokenID == PlsqlTokenId.SQL_PLUS) {
               String image = readLine(ts, token);
               if ((image.startsWith("DEF "))
                       || (image.startsWith("DEFI "))
                       || (image.startsWith("DEFIN "))
                       || (image.startsWith("DEFINE "))) {
                  final int defOffset = token.offset(tokenHierarchy);

                  final StringTokenizer tokenizer = new StringTokenizer(image);
                  tokenizer.nextToken();
                  if (tokenizer.hasMoreTokens()) {
                     image = tokenizer.nextToken();
                  }

                  if (image.equalsIgnoreCase(tokenImage.substring(1))) {
                     //We have found the define
                     while (tokenizer.hasMoreTokens()) {
                        image = tokenizer.nextToken();
                     }
                     return image;
                  } else {
                     ts.move(defOffset);
                     ts.moveNext();
                     // return tokenImage;
                  }
               }
            }
         }
      }
      return tokenImage;
   }

   private String readLine(final TokenSequence<PlsqlTokenId> ts, Token<PlsqlTokenId> token) {
      String line = token.toString();
      while (ts.moveNext()) {
         token = ts.token();
         if (token.id() == PlsqlTokenId.WHITESPACE && token.text().toString().contains("\n")) {
            ts.movePrevious();
            break;
         }
         line = line + token.toString();
      }
      return line;
   }

   /**
    * Method that will return the relevant block factory for the dataobject
    * @param obj
    * @return
    */
   private PlsqlBlockFactory getBlockFactory(final DataObject obj) {
      return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
   }
}
