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
package org.netbeans.modules.plsql.hyperlink.target;

import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.LogInWarningDialog;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import javax.swing.JOptionPane;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.openide.loaders.DataObject;
import org.openide.windows.WindowManager;

/**
 *
 * @author ChrLSE
 */
public class PlsqlPackageBodyTarget extends AbstractPlsqlGotoTarget {

   public PlsqlPackageBodyTarget(final String name, final int position, final DataObject sourceDataObject,
           final Project project, final Document sourceDocument) {
      super(name, position, sourceDataObject, project, sourceDocument);
      this.packageName = name;
      this.type = PlsqlBlockType.PACKAGE_BODY;
      this.synonym = cache.getPackageForSynonym(name);
   }

   @Override
   public DataObject getBody() {
      final DatabaseConnection connection = getPooledConnection();
      try {
         String packName = getPackageName();
         if (packName == null) {
            packName = PlsqlHyperlinkUtil.getPackageName(sourceDataObject);
         }
         if(packName==null || packName.length()==0) {
             packName=getName();
         }
         //check to see if the user has a local version of this file...
         targetDataObject = PlsqlFileUtil.openExistingFile(sourceDocument, packName, PACKAGE_BODY, project);
         if (targetDataObject == null) {
            targetDataObject = PlsqlFileUtil.fetchAsTempFile(packName, PACKAGE_BODY, connection, project, sourceDataObject);
         }
         if (targetDataObject == null) {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Go To Implementation action failed", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
         }

      } catch (NotConnectedToDbException ex) {
         new LogInWarningDialog(null, true).setVisible(true);
      } finally {
         connectionManager.releaseDatabaseConnection(connection);
      }
      return targetDataObject;
   }
}
