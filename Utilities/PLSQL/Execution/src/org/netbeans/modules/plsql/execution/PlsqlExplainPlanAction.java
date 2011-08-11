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
package org.netbeans.modules.plsql.execution;

import org.netbeans.modules.plsqlsupport.db.ui.SQLExecutionAction;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class PlsqlExplainPlanAction extends CookieAction {

   protected void performAction(Node[] activatedNodes) {
      EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
      DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
      if (editorCookie != null && dataObject != null) {
         try {
            DatabaseConnectionManager dbConnectionManager = DatabaseConnectionManager.getInstance(dataObject);
            if(dbConnectionManager==null) {
               JOptionPane.showMessageDialog(null, "Connect the project to a database");
               return;
            }
            DatabaseConnection dbConnection = dbConnectionManager.getPooledDatabaseConnection(true, true);
            try {
               Connection connection = dbConnection.getJDBCConnection();
               Document doc = editorCookie.getDocument();
               if (connection != null) {
                  explainPlan(doc.getText(0, doc.getLength()), connection);
               }
            } finally {
               dbConnectionManager.releaseDatabaseConnection(dbConnection);
            }
         } catch (BadLocationException ex) {
            ex.printStackTrace();
         }
      }
   }

   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   public String getName() {
      return NbBundle.getMessage(PlsqlExplainPlanAction.class, "CTL_PlsqlExplainPlanAction");
   }

   protected Class[] cookieClasses() {
      return new Class[]{DataObject.class};
   }

   @Override
   protected String iconResource() {
      return "org/netbeans/modules/plsql/execution/explain.png";
   }

   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   @Override
   protected boolean asynchronous() {
      return false;
   }

   /**
    * Enable this action only for the SQL execution window
    * @param nodes
    * @return
    */
   @Override
   protected boolean enable(Node[] activatedNodes) {
      if (!super.enable(activatedNodes))
         return false;
      return activatedNodes[0].getLookup().lookup(DataObject.class).getPrimaryFile().getNameExt().startsWith(SQLCommandWindow.SQL_EXECUTION_FILE_PREFIX);
   }

   private void explainPlan(String sql, Connection con) {
      InputOutput io = IOProvider.getDefault().getIO("Explain Plan", true);
      io.select();
      try {
         sql = sql.trim();
         OutputWriter out = io.getOut();
         Statement stmt = con.createStatement();
         out.println("Explain plan for:");
         out.println("   " + sql.replaceAll("\n", "\n   "));
         if(sql.endsWith(";"))
            sql = sql.substring(0, sql.length()-1);
         stmt.execute("explain plan for " + sql);
         ResultSet rs = stmt.executeQuery("select plan_table_output from table(dbms_xplan.display)");
         while (rs.next()) {
            out.println(rs.getString(1));
         }
         stmt.close();
         out.close();
         io.getErr().close();
      } catch (SQLException ex) {
         io.getErr().println(ex.getMessage());
      }
   }
}

