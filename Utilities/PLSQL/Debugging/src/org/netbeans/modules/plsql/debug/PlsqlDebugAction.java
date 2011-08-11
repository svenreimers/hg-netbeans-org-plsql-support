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
import org.netbeans.modules.plsqlsupport.db.DatabaseContentUtilities;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.netbeans.modules.plsql.execution.PlsqlExecutableBlocksMaker;
import org.netbeans.modules.plsql.execution.PlsqlExecutableObject;
import org.netbeans.modules.plsql.execution.PlsqlFileExecutor;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockUtilities;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;
import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerInfo;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.jpda.LineBreakpoint;
import org.netbeans.api.debugger.jpda.ListeningDICookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.text.NbDocument;
import org.openide.util.RequestProcessor;
import org.openide.util.RequestProcessor.Task;

public final class PlsqlDebugAction extends CookieAction {

   private static final RequestProcessor RP = new RequestProcessor(PlsqlDebugAction.class.getName());
   private static final Logger log = Logger.getLogger(PlsqlDebugAction.class.getName());

   @Override
   protected void performAction(Node[] activatedNodes) {
      EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
      DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
      if (editorCookie != null && dataObject != null) {
         Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
         if (project != null) {
            PlsqlToggleBreakpointActionProvider.setProject(project);
            DatabaseConnectionManager dbConnectionManager = DatabaseConnectionManager.getInstance(dataObject);
            if (dbConnectionManager == null) {
               JOptionPane.showMessageDialog(null, "Connect the project to a database");
            }
            DatabaseConnection connection = dbConnectionManager.getPooledDatabaseConnection(true);
            try {
               Document doc = editorCookie.getDocument();
               debug(dbConnectionManager, dataObject, connection.getJDBCConnection(), doc);
            } finally {
               dbConnectionManager.releaseDatabaseConnection(connection);
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
      return NbBundle.getMessage(PlsqlDebugAction.class, "CTL_PlsqlDebugAction");
   }

   @Override
   protected Class[] cookieClasses() {
      return new Class[]{DataObject.class};
   }

   @Override
   protected String iconResource() {
      return "org/netbeans/modules/plsql/debug/debug.png";
   }

   @Override
   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   @Override
   protected boolean asynchronous() {
      return true;
   }

   /**
    * Enable this action only for the SQL execution window
    * @param nodes
    * @return
    */
   @Override
   protected boolean enable(Node[] activatedNodes) {
      if (!super.enable(activatedNodes)) {
         return false;
      }
      FileObject fo = activatedNodes[0].getLookup().lookup(DataObject.class).getPrimaryFile();
      Project project = FileOwnerQuery.getOwner(fo);
      return project != null && fo.getNameExt().startsWith(SQLCommandWindow.SQL_EXECUTION_FILE_PREFIX);

   }

   private void debug(DatabaseConnectionManager dbConnectionManager, DataObject dataobject, Connection con, Document doc) {
      InputOutput io = IOProvider.getDefault().getIO("PL/SQL Debug Output", true);
      io.select();
      OutputWriter out = io.getOut();
      Statement stmt = null;
      try {
         out.println("--- Starting debug listener");
         startNetbeansDebugListener();
         out.println("--- Preparing database for debug session");
         prepareDatabaseForDebugSession(out, con);
         stmt = con.createStatement();
         final JDBCURL url = new JDBCURL(dbConnectionManager.getPrimaryConnectionURL());
         final String connectDebuggerStmt = "Dbms_Debug_Jdwp.Connect_Tcp('" + getLocalHostAddress(url.getHost()) + "', '13406');";
         out.println("exec " + connectDebuggerStmt);
         stmt.execute("BEGIN\n" + connectDebuggerStmt + "\nEND;");
         out.println("--- Executing statements...");
         PlsqlFileExecutor executor = new PlsqlFileExecutor(dbConnectionManager, con, io);
         executeStatements(executor, dataobject, doc);
      } catch (IOException ex) {
         io.getErr().println("Can't find the IP address of your workstation.");
         Exceptions.printStackTrace(ex);
      } catch (SQLException ex) {
         io.getErr().println(ex.getMessage());
         Exceptions.printStackTrace(ex);
      } finally {
         if (stmt != null) {
            out.println("--- Disconnecting debugger");
            String disconnectDebuggerStmt = "Dbms_Debug_Jdwp.Disconnect;";
            out.println("exec " + disconnectDebuggerStmt);
            try {
               stmt.execute("BEGIN\n" + disconnectDebuggerStmt + "\nEND;");
            } catch (SQLException ex) {
               io.getErr().println(ex.getMessage());
               //session terminated by remote debugger (e.g. NetBeans). Connection broken and must be discarded.
               try {
                  con.close();
               } catch (SQLException ex1) {
                  Exceptions.printStackTrace(ex1);
               }
            }
            try {
               stmt.close();
            } catch (SQLException ex) {
               Exceptions.printStackTrace(ex);
            }
         }
      }
      out.println("--- Debug session completed.");
      out.close();
      io.getErr().close();
   }

   private String getLocalHostAddress(final String targetHostName) throws IOException {
      final Socket s = new Socket(targetHostName, 1521);
      final String hostAddress = s.getLocalAddress().getHostAddress();
      s.close();
      return hostAddress;
   }

   private void prepareDatabaseForDebugSession(OutputWriter out, Connection con) {
      for (Breakpoint breakpoint : DebuggerManager.getDebuggerManager().getBreakpoints()) {
         String url = null;
         int lineNumber = -1;
         if (breakpoint instanceof LineBreakpoint) {
            LineBreakpoint lbp = (LineBreakpoint) breakpoint;
            url = lbp.getURL();
            lineNumber = lbp.getLineNumber();
//         } else if(breakpoint instanceof MethodBreakpoint) {
//            MethodBreakpoint mbp = (MethodBreakpoint)breakpoint;
         }
         if (url != null && lineNumber > -1) {
            String objectName = getObjectName(url, lineNumber);
            if (objectName != null) {
               Statement stmt = null;
               try {
                  stmt = con.createStatement();
                  ResultSet result = stmt.executeQuery("select type, name from all_plsql_object_settings where owner=user and upper(name)='" + objectName.toUpperCase(Locale.ENGLISH) + "' and plsql_debug='FALSE'");
                  if (result.next()) {
                     String type = result.getString("TYPE");
                     if (type.startsWith("PACKAGE")) { //for package bodies compile the package...
                        type = "PACKAGE";
                     }
                     objectName = result.getString("NAME");
                     if (!DatabaseContentUtilities.isValidOracleUppercaseIdentifier(objectName)) {
                        objectName = "\"" + objectName + "\"";
                     }
                     String sql = "ALTER " + type + " " + objectName + " COMPILE DEBUG";
                     out.println(sql);
                     stmt.execute(sql);
                  }
               } catch (SQLException ex) {
                  Exceptions.printStackTrace(ex);
               } finally {
                  if (stmt != null) {
                     try {
                        stmt.close();
                     } catch (SQLException ex) {
                        Exceptions.printStackTrace(ex);
                     }
                  }
               }
            }
         }
      }
   }

   private String getObjectName(final String url, final int lineNumber) {
      String blockName = null;
      try {
         if (lineNumber == -1) {
            return null;
         }

         //convert url to regular file name
         //first strip the file:// part...
         String fileName = url.substring(6);
         //then decode the url...
         fileName = java.net.URLDecoder.decode(fileName, "UTF-8");
         final File file = new File(fileName);
         if (!file.exists()) {
            return null;
         }

         DataObject dataObject;
         try {
            dataObject = DataFolder.find(FileUtil.toFileObject(file));
         } catch (DataObjectNotFoundException ex) {
            return null;
         }

         final StyledDocument document = dataObject.getLookup().lookup(EditorCookie.class).getDocument();
         final PlsqlBlockFactory blockFactory = dataObject.getLookup().lookup(PlsqlBlockFactory.class);
         if (blockFactory == null) {
            return null;
         }

         final int offset = NbDocument.findLineOffset(document, lineNumber - 1);
         PlsqlBlock block = PlsqlBlockUtilities.getCurrentBlock(offset, blockFactory.getBlockHierarchy());
         if (block == null) {
            return null;
         }

         while (block.getParent() != null) {
            block = block.getParent();
         }

         blockName = block.getName();
      } catch (UnsupportedEncodingException ex) {
         Exceptions.printStackTrace(ex);
      }
      return blockName;
   }

   private void executeStatements(PlsqlFileExecutor executor, DataObject dataobject, Document doc) {
      Document document = dataobject.getLookup().lookup(EditorCookie.class).getDocument();
      PlsqlExecutableBlocksMaker blockMaker = new PlsqlExecutableBlocksMaker(document);
      List<PlsqlExecutableObject> blocks = blockMaker.makeExceutableObjects();
      if (blocks.size() > 0) { //set window title
         String str = blocks.get(0).getPlsqlString().replaceAll("\n", " ");
         dataobject.getNodeDelegate().setDisplayName(str.length() > 30 ? str.substring(0, 30) + "..." : str);
      }
      executor.executePLSQL(blocks, doc, false, true);
   }

   private boolean startNetbeansDebugListener() {
      RP.post(new DebuggerStarter());
      final Task task = RequestProcessor.getDefault().post(new DebuggerWaiter());
      try {
         //wait for debug listener to start...
         task.waitFinished(20000);
      } catch (InterruptedException ex) {
         Exceptions.printStackTrace(ex);
      }
      return true;
   }

   private class DebuggerStarter implements Runnable {

      private final DebuggerManager debugManager = DebuggerManager.getDebuggerManager();

      @Override
      public void run() {
         final Session[] sessions = debugManager.getSessions();
         final Object[] services = new Object[]{ListeningDICookie.create(13406), this};
         final DebuggerInfo debugInfo = DebuggerInfo.create(ListeningDICookie.ID, services);
         if (sessions.length == 0) {
            debugManager.startDebugging(debugInfo);
         }
      }
   }

   private class DebuggerWaiter implements Runnable {

      @Override
      public void run() {
         final DebuggerManager debugManager = DebuggerManager.getDebuggerManager();
         while (debugManager.getDebuggerEngines().length == 0) {
            try {
               log.log(Level.FINEST, "debugManager.getDebuggerEngines().length: {0}", debugManager.getDebuggerEngines().length);
               Thread.sleep(500);
            } catch (InterruptedException ex) {
               Exceptions.printStackTrace(ex);
            }
         }
         log.log(Level.FINE, "debugManager.getDebuggerEngines().length: {0}", debugManager.getDebuggerEngines().length);
      }
   }
}
