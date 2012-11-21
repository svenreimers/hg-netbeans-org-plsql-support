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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;

/**
 *
 * @author chrlse
 */
class PlsqlProjectFileCacheManager extends FileChangeAdapter implements Serializable {

   private static final Logger logger = Logger.getLogger(PlsqlProjectFileCacheManager.class.getName());
   private static final long serialVersionUID = 1L;
   private final File rootFolder;
   private final File cacheFile;
   private final SimplePlsqlFileParser fileParser = SimplePlsqlFileParser.getInstance();
   private PlsqlFileCache cache;

   PlsqlProjectFileCacheManager(final File rootFolder, final File cacheFile) {
      this.rootFolder = rootFolder;
      this.cacheFile = cacheFile;
   }

   void init() {
      if (cacheFile.canRead()) {
         cache = readCacheFromFile(cacheFile);
         if (cache != null) {
            cache.addListener(this);
         }
      }
      if (cache == null) {
         cache = new PlsqlFileCache(fileParser);
         cache.addListener(this);
         SwingUtilities.invokeLater(new Runnable() {

              @Override
              public void run() {
                  buildCache();
              }
          });
      }
   }

   void rebuild() {
      cache.clear();
      buildCache();
   }

   private void buildCache() {
      final PlsqlFileFinder scanner = new PlsqlFileFinder(rootFolder);
      final Collection<File> listFiles = scanner.findPlsqlFiles();
      cache.parse(listFiles);
      writeCacheToFile(cache, cacheFile);
   }

   FileObject get(final PlsqlSearchObject searchObject) {
      return cache.get(searchObject);
   }
   
   Collection<File> getAllPlsqlObjects(){
   final PlsqlFileFinder scanner = new PlsqlFileFinder(rootFolder);
      final Collection<File> listFiles = scanner.findPlsqlFiles();
      return listFiles;
   }

   int numberPlsqlObjects() {
      return cache.numberPlsqlObjects();
   }

   int numberFileObjects() {
      return cache.numberFileObjects();
   }

   @Override
   public void fileChanged(final FileEvent fe) {
      logger.log(Level.FINER, "fileChanged: {0}", fe.getFile().getNameExt());
      super.fileChanged(fe);
      cache.remove(fe.getFile());
      try {
         cache.parse(fe.getFile());
         writeCacheToFile(cache, cacheFile);
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
      }
   }

   @Override
   public void fileDeleted(final FileEvent fe) {
      logger.log(Level.FINER, "fileDeleted: {0}", fe.getFile().getNameExt());
      super.fileDeleted(fe);
      cache.remove(fe.getFile());
      writeCacheToFile(cache, cacheFile);
   }

   PlsqlBlockType getPlsqlType(final String parent) {
      return cache.getPlsqlType(parent);
   }

   void addFileToCache(final FileObject fileObject) throws IOException {
      if (isValidFileObject(fileObject)) {
         cache.parse(fileObject);
         writeCacheToFile(cache, cacheFile);
      }
   }

   private boolean isValidFileObject(final FileObject fileObject) throws IOException {
      return isFileInRootFolder(fileObject) && !cache.contains(fileObject)
              && !FileUtil.toFile(fileObject).getPath().contains(File.separator + ".");

   }

   private boolean isFileInRootFolder(final FileObject fileObject) throws IOException {
      return FileUtil.toFile(fileObject).getPath().startsWith(rootFolder.getCanonicalPath());
   }

   private boolean writeCacheToFile(final PlsqlFileCache cache, final File file) {
      FileOutputStream writer = null;
      ObjectOutputStream out = null;
      try {
         final File test = file.getParentFile();
         if (!test.exists()) {
            test.mkdirs();
         }

         if (!file.exists()) {
            file.createNewFile();
         }
         writer = new FileOutputStream(file);
         out = new ObjectOutputStream(writer);
         out.writeObject(cache);
         logger.log(Level.FINE, "cache [{0}] written to File: [{1}]", new Object[]{cache.toString(), file.getCanonicalPath()});
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
         return false;
      } finally {
         try {
            if (out != null) {
               out.close();
            }
            if (writer != null) {
               writer.close();
            }
         } catch (IOException e) {
            Exceptions.printStackTrace(e);
         }
      }
      return true;
   }

   @SuppressWarnings("unchecked")
   private PlsqlFileCache readCacheFromFile(final File file) {
      boolean deleteFile = false;
      FileInputStream reader = null;
      {
         ObjectInputStream in = null;
         try {
            reader = new FileInputStream(file);
            in = new ObjectInputStream(reader);
            final PlsqlFileCache readCache = (PlsqlFileCache) in.readObject();
            logger.log(Level.INFO, "cache read from File: [{0}]", new Object[]{file.getCanonicalPath()});
            return readCache;
         } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
         } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
            //file currupt, try to delete the file;
            deleteFile = true;
         } finally {
            try {
               reader.close();
               if (deleteFile) {
                  file.delete();
               }
            } catch (IOException ex) {
               Exceptions.printStackTrace(ex);
            }
            try {
               in.close();
            } catch (IOException ex) {
               Exceptions.printStackTrace(ex);
            }
         }
      }
      return null;
   }
}
