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
package org.netbeans.modules.plsql.filetype;

import org.netbeans.modules.plsqlsupport.options.IfsOptionsUtilities;
import org.netbeans.modules.plsqlsupport.options.PLSQLAnnotationsPanel;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import java.io.IOException;
import java.util.prefs.PreferenceChangeEvent;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObjectExistsException;
import org.openide.loaders.MultiDataObject;
import org.openide.nodes.CookieSet;
import org.openide.nodes.Node;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.annotation.PlsqlAnnotationManager;
import org.netbeans.modules.plsql.utilities.PlsqlFileLocatorService;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeListener;
import org.openide.util.Lookup;
import org.openide.util.NbPreferences;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;
import org.netbeans.api.db.explorer.DatabaseConnection;

public class PlsqlDataObject extends MultiDataObject {

   private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
   private static final PlsqlFileLocatorService locator = Lookup.getDefault().lookup(PlsqlFileLocatorService.class);
   private Lookup lookup;
   private PreferenceChangeListener listener;
   private PlsqlBlockFactory blockFactory = null;
   private PlsqlAnnotationManager annotationManager = null;
   private boolean isAnnotationsEnabled = false;
   private final PlsqlEditorSupport editorSupport;
   private DatabaseConnection databaseConnection;
   private StatementExecutionHistory statementExecutionHistory;

   public PlsqlDataObject(final FileObject fileObject, final PlsqlDataLoader loader) throws DataObjectExistsException, IOException {
      super(fileObject, loader);
      if (fileObject == null) {
         throw new IllegalArgumentException("FileObject cannot be null");
      }
      statementExecutionHistory = new StatementExecutionHistory();
      editorSupport = new PlsqlEditorSupport(this);
      getCookieSet().add(editorSupport);
      blockFactory = new PlsqlBlockFactory();
      final Project project = FileOwnerQuery.getOwner(fileObject);

      //Init annotations when the file is shown for spec and body files
      if (validator.isValidPackage(fileObject)) {
         if (project != null) {
            locator.addFileToCache(project, fileObject);
         }
         for (PlsqlAnnotationManager plsqlAnnotationManager : Lookup.getDefault().lookupAll(PlsqlAnnotationManager.class)) {
            annotationManager = plsqlAnnotationManager;
         }

         blockFactory.addObserver(annotationManager);

         //Add preference change listener to IFS Options panel
         //addPreferenceListener();
         //This will handle by the PlsqlAnnotationManager
      }
      // TODO: if file not in project?!?
      if (project != null) {
         databaseConnection = DatabaseConnectionManager.getInstance(project).getTemplateConnection();
      }
      createLookup();
   }

   @Override
   protected Node createNodeDelegate() {
      return new PlsqlDataNode(this);
   }

   @Override
   public Lookup getLookup() {
      if (lookup == null) {
         createLookup();
      }
      return lookup;
   }

   boolean isTemporary() {
      return getPrimaryFile().getExt().equalsIgnoreCase("tdb");
   }

   /**
    * Return cookie set, to be used in editor support
    * @return
    */
   CookieSet getCookieSet0() {
      return getCookieSet();
   }

   private void addPreferenceListener() {
      listener = new PreferenceChangeListener() {

         @Override
         public void preferenceChange(PreferenceChangeEvent evt) {
            if (evt.getKey().equals(IfsOptionsUtilities.plsql_annotations_enabled_key)) {
               manageAnnotations();
            }
         }
      };
      NbPreferences.forModule(PLSQLAnnotationsPanel.class).addPreferenceChangeListener(listener);
   }

   private void manageAnnotations() {
      if (IfsOptionsUtilities.isPlSqlAnnotationsEnabled() && !isAnnotationsEnabled) {
         blockFactory.addObserver(annotationManager);
         isAnnotationsEnabled = true;
         modifyLookupAnnotationManager(annotationManager);
         annotationManager.initAnnotations(this);
      } else if (!IfsOptionsUtilities.isPlSqlAnnotationsEnabled() && isAnnotationsEnabled) {
         blockFactory.deleteObserver(annotationManager);
         annotationManager.clearAnnotations();
         isAnnotationsEnabled = false;
         modifyLookupAnnotationManager(annotationManager);
      }
   }

   private void createLookup() {
      List<Object> objects = new ArrayList<Object>();
      objects.add(blockFactory);
      objects.add(statementExecutionHistory);
      
       if (annotationManager != null) {
           objects.add(annotationManager);
       }
      
      if (databaseConnection != null) {
         objects.add(databaseConnection);
      }
      Lookup fixed = Lookups.fixed(objects.toArray());
      lookup = new ProxyLookup(new Lookup[]{getCookieSet().getLookup(), fixed});
   }

   public void modifyLookupDatabaseConnection(DatabaseConnection Connection) {
      databaseConnection = Connection;
      createLookup();
   }

   public void modifyLookupAnnotationManager(PlsqlAnnotationManager annotationManager) {
      this.annotationManager = annotationManager;
      createLookup();
   }

   public void disposeFactoryAndManager() {
      blockFactory = null;
      annotationManager = null;
      lookup = null;
      listener = null;
      getCookieSet().remove(editorSupport);
   }
}
