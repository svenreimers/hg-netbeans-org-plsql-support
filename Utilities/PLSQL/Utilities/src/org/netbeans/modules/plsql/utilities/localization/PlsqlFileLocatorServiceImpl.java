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
package org.netbeans.modules.plsql.utilities.localization;

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlFileLocatorService;
import org.netbeans.modules.plsql.utilities.PlsqlSearchObject;
import org.netbeans.modules.plsql.utilities.validation.PlsqlFileValidatorImpl;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author chrlse
 */
@ServiceProvider(service = PlsqlFileLocatorService.class)
public class PlsqlFileLocatorServiceImpl implements PlsqlFileLocatorService {

   private static final String cacheDir = System.getProperty("netbeans.user") + "/var/cache/localplsql";
   private final Map<String, PlsqlProjectFileCacheManager> cachePerProject = new HashMap<String, PlsqlProjectFileCacheManager>(); //NOPMD
   private final static Logger logger = Logger.getLogger(PlsqlFileLocatorServiceImpl.class.getName());

   @Override
   public void registerFolder(final String projectName, final FileObject rootFolder) {
      if (projectName == null || rootFolder == null) {
         throw new IllegalArgumentException("arguments cannot be null");
      }
      logger.log(Level.INFO, "Register for project [{0}] with rootFolder [{1}]", new Object[]{projectName, rootFolder});
      PlsqlProjectFileCacheManager pfl = cachePerProject.get(projectName);
      if (pfl == null) {
         pfl = new PlsqlProjectFileCacheManager(FileUtil.toFile(rootFolder), new File(cacheDir, projectName + ".cache"));
      }
      pfl.init();
      cachePerProject.put(projectName, pfl);
   }

   @Override
   public void rebuildCache(final Project project) {
      if (project == null || !cachePerProject.containsKey(getProjectName(project))) {
         throw new IllegalArgumentException("arguments cannot be null");
      }
      logger.log(Level.INFO, "Rebuild cache for project [{0}] ", new Object[]{getProjectName(project)});
      final PlsqlProjectFileCacheManager pfl = cachePerProject.get(getProjectName(project));
      pfl.rebuild();
   }

   @Override
   public void unregisterProject(final String projectName) {
      logger.log(Level.INFO, "Remove project [{0}] ", projectName);
      cachePerProject.remove(projectName);
   }

   @Override
   public DataObject getExistingDataObject(final DataObject sourceDataObject, final String objectName, final PlsqlBlockType objectType, final Project project) {
      DataObject targetDataObject = null;
      if (project != null) {
         final String projectName = getProjectName(project);
         final PlsqlSearchObject plsqlSearchObject = new PlsqlSearchObject(objectType, objectName);
         targetDataObject = getDataObject(findFile(projectName, plsqlSearchObject), sourceDataObject, project);
      }
      if (targetDataObject == null) {
         final PlsqlFileValidatorImpl validator = new PlsqlFileValidatorImpl();
         final String fileName = validator.formatFileName(sourceDataObject, objectName, objectType);

         if (fileName != null) {
            targetDataObject = getDataObject(FileUtil.toFileObject(new File(fileName)), sourceDataObject, project);
         }
      }
      return targetDataObject;
   }

   private DataObject getDataObject(final FileObject fileObject, final DataObject sourceDataObject, final Project project) {
      if (fileObject == null) {
         return null;
      }
      try {
         final DataObject result = DataFolder.find(fileObject);
         if (project == null || DatabaseConnectionManager.getInstance(project) == null) {
            DatabaseConnectionManager.copyProvider(sourceDataObject, result);
         }
         return result;
      } catch (DataObjectNotFoundException ex) {
         Exceptions.printStackTrace(ex);
      }
      return null;
   }

   FileObject findFile(final String projectName, final PlsqlSearchObject searchObject) {
      if (projectName == null || searchObject == null) {
         throw new IllegalArgumentException("arguments cannot be null");
      }
      final FileObject result = cachePerProject.get(projectName).get(searchObject);
      logger.log(Level.FINE, "Looking for project [{0}] with searchObject [{1}] found [{2}]", new Object[]{projectName, searchObject, result});
      return result;
   }

   int size() {
      return cachePerProject.size();
   }

   int size(final String projectName) {
      return cachePerProject.get(projectName).numberPlsqlObjects();
   }

   private String getProjectName(final Project project) {
      final ProjectInformation projectInformation = ProjectUtils.getInformation(project);
      return projectInformation.getDisplayName();
   }

   @Override
   public PlsqlBlockType getPlsqlType(final Project project, final String parent) {
      final PlsqlProjectFileCacheManager fileCache = cachePerProject.get(getProjectName(project));
      return fileCache.getPlsqlType(parent);
   }

   @Override
   public void addFileToCache(final Project project, final FileObject fileObject) throws IOException {
      if (project != null && fileObject != null) {
         logger.log(Level.FINE, "addFileToCache for project [{0}] with fileObject [{1}]", new Object[]{getProjectName(project), fileObject});
         final PlsqlProjectFileCacheManager fileCache = cachePerProject.get(getProjectName(project));
         if (fileCache != null) {
            fileCache.addFileToCache(fileObject);
         } else {
            logger.log(Level.WARNING, "PlsqlProjectFileCacheManager == null for project [{0}] with fileObject [{1}]", new Object[]{getProjectName(project), fileObject});
         }
      }
   }
}
