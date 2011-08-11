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

import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlSearchObject;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author chrlse
 */
class PlsqlFileCache implements Serializable {

   private static final long serialVersionUID = 1L;
   private static final Logger logger = Logger.getLogger(PlsqlFileCache.class.getName());
   private final Map<PlsqlSearchObject, FileObject> findBySearchObject = new HashMap<PlsqlSearchObject, FileObject>();
   private final Map<String, PlsqlBlockType> findByPlsqlObject = new HashMap<String, PlsqlBlockType>();
   private final Map<FileObject, List<PlsqlSearchObject>> findByFileObject = new HashMap<FileObject, List<PlsqlSearchObject>>();
   private final SimplePlsqlFileParser fileParser;
   private transient FileChangeListener listener;

   PlsqlFileCache(final SimplePlsqlFileParser fileParser) {
      this.fileParser = fileParser;
   }

   void addListener(final FileChangeListener listener) {
      this.listener = listener;
      for (FileObject fileObject : findByFileObject.keySet()) {
         fileObject.addFileChangeListener(listener);
      }
   }

   void parse(final Collection<File> listFiles) {
      logger.log(Level.INFO, "parsing started");
      ProgressHandle handle = ProgressHandleFactory.createHandle("Parsing " + listFiles.size() + " database files...");
      handle.start(listFiles.size() / 100, listFiles.size() / 1000);
      final Date startTime = Calendar.getInstance().getTime();
      try {
         int workunit = 0;
         int currentFileNumber = 0;
         for (File file : listFiles) {
            currentFileNumber++;
            try {
               if (currentFileNumber % 100 == 0) {
                  handle.progress(workunit++);
               }
               parse(FileUtil.toFileObject(file));
            } catch (IOException ex) {
               Exceptions.printStackTrace(ex);
            }
         }
         handle.progress("Done", workunit++);
      } finally {
         handle.finish();
      }
      final Date endTime = Calendar.getInstance().getTime();
      logger.log(Level.INFO, "parsing ended, taking {0} ms for {1} files", new Object[]{endTime.getTime() - startTime.getTime(), listFiles.size()});
   }

   void parse(final FileObject fileObject) throws IOException {
      if (listener == null) {
         throw new IllegalStateException("listener cannot be null");
      }
      final List<PlsqlSearchObject> searchObjects = fileParser.searchInFile(fileObject);
      addToCollections(searchObjects, fileObject);
   }

   private void addToCollections(final List<PlsqlSearchObject> searchObjects, final FileObject fileObject) {
      for (PlsqlSearchObject pso : searchObjects) {
         findBySearchObject.put(pso, fileObject);
         addSearchObjectToMap(fileObject, pso);
         addPlsqlBlockTypeToMap(pso);
      }
   }

   FileObject get(final PlsqlSearchObject searchObject) {
      return findBySearchObject.get(searchObject);
   }

   PlsqlBlockType getPlsqlType(final String parent) {
      return findByPlsqlObject.get(parent.toUpperCase(Locale.ENGLISH));
   }

   int numberPlsqlObjects() {
      return findBySearchObject.size();
   }

   int numberFileObjects() {
      return findByFileObject.size();
   }

   private void addSearchObjectToMap(final FileObject fileObject, final PlsqlSearchObject searchObject) {
      List<PlsqlSearchObject> searchObjects = findByFileObject.get(fileObject);
      if (searchObjects == null) {
         searchObjects = new ArrayList<PlsqlSearchObject>();
         logger.log(Level.FINE, "addFileChangeListener to file: {0}", fileObject.getNameExt());
         //only add once
         fileObject.addFileChangeListener(listener);
      }
      searchObjects.add(searchObject);
      findByFileObject.put(fileObject, searchObjects);
   }

   public void remove(final FileObject fileObject) {
      if (listener == null) {
         throw new IllegalStateException("listener cannot be null");
      }
      logger.log(Level.FINER, "removeFileObject: {0}", fileObject.getNameExt());
      fileObject.removeFileChangeListener(listener);
      final List<PlsqlSearchObject> searchObjects = findByFileObject.remove(fileObject);
      for (PlsqlSearchObject searchObject : searchObjects) {
         findBySearchObject.remove(searchObject);
         findByPlsqlObject.remove(searchObject.getObjectName());
         logger.log(Level.FINER, "searchObject: {0} removed", searchObject);
      }
   }

   private void addPlsqlBlockTypeToMap(final PlsqlSearchObject searchObject) {
      findByPlsqlObject.put(searchObject.getObjectName(), searchObject.getType());
   }

   boolean contains(final FileObject fileObject) {
      return findByFileObject.containsKey(fileObject);
   }

   void clear() {
      findByFileObject.clear();
      findByPlsqlObject.clear();
      findBySearchObject.clear();
   }
}
