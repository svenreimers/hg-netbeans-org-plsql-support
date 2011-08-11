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
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentUtilities;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import javax.swing.JEditorPane;
import javax.swing.text.Caret;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.actions.CookieAction;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;


public final class PlsqlDebugCompileAction extends CookieAction {

   String objectName;
    @Override
    protected int mode() {
       return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[]{EditorCookie.class};
    }

   @Override
   protected boolean asynchronous() {
      return true;
   }

   @Override
   protected void performAction(Node[] activatedNodes) {
      if (objectName == null) {
         return;
      }
      DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
      DatabaseConnectionManager provider = DatabaseConnectionManager.getInstance(dataObject);
      InputOutput io = IOProvider.getDefault().getIO("Debug Compile", false);
      io.select();
      OutputWriter out = io.getOut();
      if (objectName != null) {
         PreparedStatement stmt = null;
         DatabaseConnection dbcon = provider.getPooledDatabaseConnection(false);
         try {
            Connection con = dbcon != null ? dbcon.getJDBCConnection() : null;
            if (con == null) {
               out.write("Can't connect to the database");
               return;
            }
            stmt = con.prepareStatement("select type from all_plsql_object_settings where owner=user and name=? and type<>'PACKAGE BODY'");
            stmt.setString(1, objectName);
            ResultSet result = stmt.executeQuery();

            if (result.next()) {
               String type = result.getString("TYPE");
               String sql = "alter " + type + " " + objectName + " compile debug";
               out.write(sql + "...");
               stmt.execute(sql);
               out.write("Done\n");
            } else {
               out.write("Object " + objectName.toUpperCase(Locale.ENGLISH) + " does not exist in database\n");
            }
         } catch (SQLException ex) {
            io.getErr().println(ex.getMessage());
            Exceptions.printStackTrace(ex);
         } finally {
            if (stmt != null) {
               try {
                  stmt.close();
               } catch (SQLException ex) {
                  Exceptions.printStackTrace(ex);
               }
            }
            provider.releaseDatabaseConnection(dbcon);
         }
      }
   }

    @Override
    public String getName() {
        return NbBundle.getMessage(PlsqlDebugCompileAction.class, "CTL_PlsqlDebugCompileAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override    
   protected boolean enable(Node[] activatedNodes) {
       objectName = getObjectName(activatedNodes);
       return objectName!=null;
   }

   private String getObjectName(Node[] activatedNodes) {
      if (activatedNodes == null || activatedNodes.length == 0) {
         return null; //This happends when using the diff window (.gen <--> actual)
      }
      EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);

      if (editorCookie == null) {
         return null;
      }
      JEditorPane[] panes = editorCookie.getOpenedPanes();
      if ((panes == null) || (panes.length == 0)) {
         return null;
      }
      String selection = panes[0].getSelectedText();
      if(selection!=null) { //check that this is a valid oracle identifier
         if(selection.startsWith("\"") && selection.endsWith("\"")) {
            return selection;
         }
         String upper = selection.toUpperCase(Locale.ENGLISH);
         if(DatabaseContentUtilities.isValidOracleUppercaseIdentifier(upper)) {
            return upper;
         }
         return null;
      }
      Caret caret = panes[0].getCaret();
      int position = caret.getDot();
      //go through the parse tree and find the "top" node. This is the object we should debug complie
      DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
      PlsqlBlockFactory fac = dataObject.getLookup().lookup(PlsqlBlockFactory.class);
      List<PlsqlBlock> blockHierarchy = fac.getBlockHierarchy();
      String objectName = PlsqlParserUtil.getPackageName(fac, position);
      if(objectName==null || objectName.length()==0) {
         objectName = PlsqlParserUtil.getMethodName(fac, position);
      }
      if(objectName!=null && objectName.length()>0) {
         if(objectName.startsWith("\"")) {
            return objectName;
         } else {
            return objectName.toUpperCase(Locale.ENGLISH);
         }
      }
      return null;
   }
}
