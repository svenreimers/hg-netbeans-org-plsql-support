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

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

public final class CurrentDatabaseToolbarInfo extends AbstractAction implements ContextAwareAction, Presenter.Toolbar {
   private JLabel label;
   private DataObject dataObject;
   private static final String NO_CONNECTION = "no database connection";

   public CurrentDatabaseToolbarInfo() {
      this(Utilities.actionsGlobalContext());
   }

   public CurrentDatabaseToolbarInfo(Lookup context) {
      putValue(SHORT_DESCRIPTION, getName());
      dataObject = context.lookup(DataObject.class);
   }

   public Action createContextAwareInstance(Lookup context) {
      return new CurrentDatabaseToolbarInfo(context);
   }

   private String getName() {
      return NbBundle.getMessage(CurrentDatabaseToolbarInfo.class, "CTL_CurrentDatabaseToolbarInfo");
   }


   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   private String getDatabaseConnectionText() {
      if(dataObject!=null) {
         DatabaseConnectionManager dbConnectionManager = DatabaseConnectionManager.getInstance(dataObject);
         if(dbConnectionManager!=null) {
            return dbConnectionManager.formatDatabaseConnectionInfo();
         }
      }
      return NO_CONNECTION;
   }

   @Override
   public Component getToolbarPresenter() {
      label = new JLabel(getDatabaseConnectionText());
      return label;
   }

   @Override
   public void actionPerformed(ActionEvent e) {
     //do nothing...
   }
}
