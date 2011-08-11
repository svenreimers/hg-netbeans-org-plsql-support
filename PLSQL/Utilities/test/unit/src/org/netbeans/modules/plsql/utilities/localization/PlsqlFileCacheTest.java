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
import java.io.IOException;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.apache.commons.io.FileUtils;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlSearchObject;
import org.netbeans.modules.plsql.utilities.localization.PlsqlFileLocatorServiceImplTest.PlsqlFileValidatorMock;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.junit.NbTestCase;
import org.netbeans.junit.MockServices;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author chrlse
 */
public class PlsqlFileCacheTest extends NbTestCase {

   public PlsqlFileCacheTest(String name) {
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
    * Test of parse method, of class PlsqlFileCache.
    */
   @Test
   public void testParse_FileObject() throws Exception {
      System.out.println("parse");
      final SimplePlsqlFileParser fileParser = SimplePlsqlFileParser.getInstance();
      final FileChangeListener fileListener = mock(FileChangeListener.class);
      PlsqlFileCache cache = new PlsqlFileCache(fileParser);
      System.out.println("fileChanged");
      FileObject fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      try {
         cache.parse(fileObject);
         fail("should throw IllegalStateException");
      } catch (IllegalStateException ise) {
      }
      cache.addListener(fileListener);
      cache.parse(fileObject);
//      final File rootFolderB = new File(getWorkDir(), WORKSPACE_B);
//      PlsqlProjectFileCacheManager cache = new PlsqlProjectFileCacheManager(rootFolderB);
//      cache.init();

      assertEquals(3, cache.numberPlsqlObjects());
      assertEquals(1, cache.numberFileObjects());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_REP"));
      assertTrue(fileObject.isValid());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "MODULE_RPI"));
      assertTrue(fileObject.isValid());
      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "MODULE_RPI"));
      assertTrue(fileObject.isValid());

      changeFile();
      cache.remove(fileObject);
      cache.parse(fileObject);
      verify(fileListener, times(1)).fileChanged(any(FileEvent.class));

      changeFile();
      cache.remove(fileObject);
      cache.parse(fileObject);
      verify(fileListener, times(2)).fileChanged(any(FileEvent.class));

      changeFile();
      cache.remove(fileObject);
      cache.parse(fileObject);
      verify(fileListener, times(3)).fileChanged(any(FileEvent.class));
//      assertEquals(2, cache.numberPlsqlObjects());
//      assertEquals(1, cache.numberFileObjects());
//      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_REP"));
//      assertNull(fileObject);
//      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "MODULE_RPI"));
//      assertNull(fileObject);
//      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "MODULE_RPI"));
//      assertTrue(fileObject.isValid());
//      fileObject = cache.get(new PlsqlSearchObject(PlsqlBlockType.VIEW, "MODULE_VIEW"));
//      assertTrue(fileObject.isValid());
//      System.out.println("isLocked: " + fileObject.isLocked());


   }

   private void changeFile() throws IOException {
      //      fileObject = FileUtil.toFileObject(new File(getWorkDir(), MODULE_RDF));
      //      FileObject folder = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + FNDBAS_PATH));
      //      fileObject.copy(folder, "Module", "rdf");
      //      FileUtils.copyFile(new File(getWorkDir(), MODULE_RDF), new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      FileObject fileObject = FileUtil.toFileObject(new File(getWorkDir(), WORKSPACE_B + MODULE_RDF_PATH));
      InputStream inputStream = FileUtil.toFileObject(new File(getWorkDir(), MODULE_RDF)).getInputStream();
      FileLock lock = fileObject.lock();
      OutputStream outputStream = fileObject.getOutputStream(lock);
      IOUtils.copy(inputStream, outputStream);
      IOUtils.closeQuietly(outputStream);
      IOUtils.closeQuietly(inputStream);
      lock.releaseLock();
      System.out.println("isLocked: " + fileObject.isLocked());
   }
}
