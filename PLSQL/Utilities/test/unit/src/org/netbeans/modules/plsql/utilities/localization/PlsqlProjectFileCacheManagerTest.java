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

import static org.netbeans.modules.plsql.utilities.TestUtils.*;
import static org.junit.Assert.*;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.Before;
import org.netbeans.modules.plsql.utilities.PlsqlSearchObject;
import org.netbeans.modules.plsql.utilities.localization.PlsqlFileLocatorServiceImplTest.PlsqlFileValidatorMock;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.MockServices;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author chrlse
 */
public class PlsqlProjectFileCacheManagerTest extends NbTestCase {

   public PlsqlProjectFileCacheManagerTest(final String name) {
      super(name);
   }

   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      clearWorkDir();
      FileUtils.copyDirectoryToDirectory(new File(getDataDir(), PROJECT_B), getWorkDir());
      FileUtils.copyFileToDirectory(new File(getDataDir(), MODULE_RDF), getWorkDir());
      MockServices.setServices(PlsqlFileValidatorMock.class);
   }

   /**
    * Test of init method, of class PlsqlProjectFileCache.
    */
   @Test
   public void testInit() throws IOException {
      System.out.println("init");
      final File rootFolderB = new File(getWorkDir(), WORKSPACE_B);
      final File cacheFileB = new File(getWorkDir(), PROJECT_B + ".cache");
      PlsqlProjectFileCacheManager cache = new PlsqlProjectFileCacheManager(rootFolderB, cacheFileB);
      cache.init();

      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());

      cache = new PlsqlProjectFileCacheManager(rootFolderB, cacheFileB);
      cache.init();
      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());

      final FileObject fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      fileObject.delete();
      assertEquals(21, cache.numberPlsqlObjects());
      assertEquals(20, cache.numberFileObjects());

   }

   /**
    * Test of fileChanged method, of class PlsqlProjectFileCache.
    */
   @Test
   public void testFileChanged() throws IOException {
      System.out.println("fileChanged");
      final File rootFolderB = new File(getWorkDir(), WORKSPACE_B);
      final File cacheFileB = new File(getWorkDir(), PROJECT_B + ".cache");
      PlsqlProjectFileCacheManager cache = new PlsqlProjectFileCacheManager(rootFolderB, cacheFileB);
      cache.init();

      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());
      FileObject fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_REP"));
      assertTrue(fileObject.isValid());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "MODULE_RPI"));
      assertTrue(fileObject.isValid());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "MODULE_RPI"));
      assertTrue(fileObject.isValid());
//      fileObject = FileUtil.toFileObject(new File(getWorkDir(), MODULE_RDF));
//      FileObject folder = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + FNDBAS_PATH));
//      fileObject.copy(folder, "Module", "rdf");
//      FileUtils.copyFile(new File(getWorkDir(), MODULE_RDF), new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      InputStream inputStream = FileUtil.toFileObject(new File(getWorkDir(), MODULE_RDF)).getInputStream();

      FileLock lock = fileObject.lock();
      OutputStream outputStream = fileObject.getOutputStream(lock);
      IOUtils.copy(inputStream, outputStream);
      IOUtils.closeQuietly(outputStream);
      IOUtils.closeQuietly(inputStream);
      System.out.println("isLocked: " + fileObject.isLocked());
      lock.releaseLock();

      assertEquals(23, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_REP"));
      assertNull(fileObject);
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "MODULE_RPI"));
      assertNull(fileObject);
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "MODULE_RPI"));
      assertTrue(fileObject.isValid());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_VIEW"));
      assertTrue(fileObject.isValid());
      System.out.println("isLocked: " + fileObject.isLocked());

   }

   /**
    * Test of fileDeleted method, of class PlsqlProjectFileCache.
    */
   @Test
   public void testFileDeleted() throws IOException {
      System.out.println("fileDeleted");
      final File rootFolderB = new File(getWorkDir(), WORKSPACE_B);
      final File cacheFileB = new File(getWorkDir(), PROJECT_B + ".cache");
      PlsqlProjectFileCacheManager cache = new PlsqlProjectFileCacheManager(rootFolderB, cacheFileB);
      cache.init();

      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());
      FileObject fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_REP"));
      assertTrue(fileObject.isValid());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "MODULE_RPI"));
      assertTrue(fileObject.isValid());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "MODULE_RPI"));
      assertTrue(fileObject.isValid());
      fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      fileObject.delete();
      assertEquals(21, cache.numberPlsqlObjects());
      assertEquals(20, cache.numberFileObjects());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_REP"));
      assertNull(fileObject);
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "MODULE_RPI"));
      assertNull(fileObject);
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "MODULE_RPI"));
      assertNull(fileObject);
   }

   /**
    * Test of fileRenamed method, of class PlsqlProjectFileCache.
    */
   @Test
   public void testFileRenamed() throws IOException {
      final File rootFolderB = new File(getWorkDir(), WORKSPACE_B);
      final File cacheFileB = new File(getWorkDir(), PROJECT_B + ".cache");
      PlsqlProjectFileCacheManager cache = new PlsqlProjectFileCacheManager(rootFolderB, cacheFileB);
      cache.init();

      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());
      FileObject fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      FileLock lock = fileObject.lock();
      fileObject.rename(lock, "NewModule", "rdf");
      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_REP"));
      assertTrue(fileObject.isValid());
      assertEquals("NewModule.rdf", fileObject.getNameExt());
   }

   /**
    * Test of addFileToCache method, of class PlsqlProjectFileCacheManager.
    */
   @Test
   public void testAddFileToCache() throws Exception {
      System.out.println("addFileToCache");
      final File rootFolderB = new File(getWorkDir(), WORKSPACE_B);
      final File cacheFileB = new File(getWorkDir(), PROJECT_B + ".cache");
      PlsqlProjectFileCacheManager cache = new PlsqlProjectFileCacheManager(rootFolderB, cacheFileB);
      cache.init();

      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());
      FileUtils.copyFileToDirectory(new File(getDataDir(), PROJECT_B + File.separator + CLIENT_APY), getWorkDir());
      FileObject fileObject = FileUtil.toFileObject(new File(getWorkDir(), PROJECT_B + File.separator + CLIENT_APY));
      cache.addFileToCache(fileObject);
      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());

      fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      cache.addFileToCache(fileObject);
      assertEquals(24, cache.numberPlsqlObjects());
      assertEquals(21, cache.numberFileObjects());

      FileUtils.copyFileToDirectory(new File(getDataDir(), PROJECT_B + File.separator + CLIENT_APY), new File(getWorkDir(), WORKSPACE_B));
      fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + File.separator + CLIENT_APY));
      cache.addFileToCache(fileObject);
      assertEquals(25, cache.numberPlsqlObjects());
      assertEquals(22, cache.numberFileObjects());

      // file in .ifs should not be added
      fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + File.separator + ACTOR_GEN_APY_PATH));
      cache.addFileToCache(fileObject);
      assertEquals(25, cache.numberPlsqlObjects());
      assertEquals(22, cache.numberFileObjects());      
   }
}
