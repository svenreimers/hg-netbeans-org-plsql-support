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
import static org.netbeans.modules.plsql.utilities.TestUtils.*;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidator;
import org.netbeans.modules.plsql.utilities.PlsqlSearchObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.junit.MockServices;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Lookup;

/**
 *
 * @author chrlse
 */
public class PlsqlFileLocatorServiceImplTest extends NbTestCase {

   public PlsqlFileLocatorServiceImplTest(String name) {
      super(name);
   }

   @Before
   @Override
   public void setUp() throws Exception {
      super.setUp();
      System.setProperty("netbeans.user", getWorkDirPath());
      MockServices.setServices(PlsqlFileValidatorMock.class);
   }

   /**
    * Test of registerFolder method, of class PlsqlFileLocatorServiceImpl.
    */
   @Test
   public void testRegisterFolder() {
      System.out.println("registerFolder");
      final FileObject rootFolderA = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_A));
      PlsqlFileLocatorServiceImpl instance = new PlsqlFileLocatorServiceImpl();
      instance.registerFolder(PROJECT_A, rootFolderA);

      try {
         instance.registerFolder(PROJECT_A, null);
         fail("should throw IllegalArgumentException");
      } catch (IllegalArgumentException e) {
         //correct
      }
      try {
         instance.registerFolder(null, rootFolderA);
         fail("should throw IllegalArgumentException");
      } catch (IllegalArgumentException e) {
         //correct
      }
      Project mockProject = mockProject(PROJECT_A);
      PlsqlBlockType plsqlType = instance.getPlsqlType(mockProject, "Error_SYS");
      assertTrue("check plsqlType", plsqlType == PlsqlBlockType.PACKAGE || plsqlType == PlsqlBlockType.PACKAGE_BODY);
      plsqlType = instance.getPlsqlType(mockProject, "NotFound");
      assertNull("check null", plsqlType);
   }

   /**
    * Test of unregisterProject method, of class PlsqlFileLocatorServiceImpl.
    */
   @Test
   public void testUnregisterProject() {
      System.out.println("unregisterProject");
      PlsqlFileLocatorServiceImpl instance = new PlsqlFileLocatorServiceImpl();
      instance.unregisterProject(PROJECT_A);
      instance.unregisterProject(null);
   }

   /**
    * Test of registerFolder method, of class PlsqlFileParserImpl.
    */
   @Test
   public void testregisterFolder() throws IOException {
      System.out.println("registerFolder");
      PlsqlFileLocatorServiceImpl instance = new PlsqlFileLocatorServiceImpl();

      try {
         instance.registerFolder(null, null);
         fail("IllegalArgumentException should be thrown");
      } catch (IllegalArgumentException e) {
         //correct
      }
      try {
         instance.registerFolder("notNull", null);
         fail("IllegalArgumentException should be thrown");
      } catch (IllegalArgumentException e) {
         //correct
      }
      try {
         instance.registerFolder(null, FileUtil.createData(File.createTempFile("test", "tmp")));
         fail("IllegalArgumentException should be thrown");
      } catch (IllegalArgumentException e) {
         //correct
      }

      final FileObject rootFolderA = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_A));
      final FileObject rootFolderB = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_B));
      assertTrue(rootFolderA.isValid() && rootFolderB.isValid());
      Date startTime = Calendar.getInstance().getTime();
      System.out.println("Start scan at: " + startTime);
      instance.registerFolder(PROJECT_A, rootFolderA);
      assertEquals(1, instance.size());
      assertEquals(1815, instance.size(PROJECT_A));
      Date endTime = Calendar.getInstance().getTime();
      System.out.println("End scan at: " + endTime);
      System.out.println("Taking " + (endTime.getTime() - startTime.getTime()) + " milliseconds");

      instance.registerFolder(PROJECT_B, rootFolderB);
      assertEquals(2, instance.size());
      assertEquals(24, instance.size(PROJECT_B));
      instance.registerFolder(PROJECT_B, rootFolderB);
      assertEquals(2, instance.size());
      assertEquals(24, instance.size(PROJECT_B));


   }

   /**
    * Test of addFileToCache method, of class PlsqlFileLocatorServiceImpl.
    */
   @Test
   public void testAddFileToCache() throws Exception {
      System.out.println("addFileToCache");
      final FileObject rootFolderB = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_B));
      PlsqlFileLocatorServiceImpl instance = new PlsqlFileLocatorServiceImpl();
      instance.registerFolder(PROJECT_B, rootFolderB);
      Project project = mockProject(PROJECT_B);
      FileObject fileObject = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_B + CLIENT_APY_PATH));
      instance.addFileToCache(project, fileObject);
//      fileObject = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_A + CLIENT_APY_PATH));
      instance.addFileToCache(null, fileObject);
      instance.addFileToCache(project, null);
      instance.addFileToCache(null, null);
      instance.unregisterProject(PROJECT_B);
      instance.addFileToCache(project, fileObject);
   }

   /**
    * Test of findFile method, of class PlsqlFileParserImpl.
    */
   @Test
   public void testFindFile() {
      System.out.println("findFile");
      PlsqlFileLocatorServiceImpl instance = new PlsqlFileLocatorServiceImpl();
      try {
         instance.findFile(null, null);
         fail("IllegalArgumentException should be thrown");
      } catch (IllegalArgumentException e) {
         //correct
      }
      try {
         instance.findFile(null, new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "FND_SECURITY_PER_USER_RPI"));
         fail("IllegalArgumentException should be thrown");
      } catch (IllegalArgumentException e) {
         //correct
      }
      try {
         instance.findFile("notNull", null);
         fail("IllegalArgumentException should be thrown");
      } catch (IllegalArgumentException e) {
         //correct
      }

      final FileObject rootFolderA = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_A));
      instance.registerFolder(PROJECT_A, rootFolderA);


      PlsqlSearchObject searchObject = new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "FND_SECURITY_PER_USER_RPI");
      String expResult = PROJECT_A + File.separator + "workspace" + File.separator + "fndbas" + File.separator + "database" + File.separator + "fndbas" + File.separator + "FndSecurityPerUser.rdf";
      FileObject result = instance.findFile(PROJECT_A, searchObject);
      assertTrue(result.getPath().endsWith(expResult));
   }

   public static class PlsqlFileValidatorMock implements PlsqlFileValidator {

      @Override
      public boolean validate(String extension) {
         return true;
      }

      @Override
      public boolean validateSpec(String extension) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public boolean validateBody(String extension) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public Collection<String> getAllExt() {
         List<String> list = new ArrayList<String>();
         list.add("api");
         list.add("apy");
         list.add("rdf");
         return list;
      }

      @Override
      public String getSpecExt(String bodyExt) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public String getBodyExt(String specExt) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public String getExtForSibling(String siblingExt) {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public String getPackageBodyExt() {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public String getPackageExt() {
         throw new UnsupportedOperationException("Not supported yet.");
      }

      @Override
      public String getFileExt(PlsqlBlockType blockType) {
         throw new UnsupportedOperationException("Not supported yet.");
      }
   }

   /**
    * Test of getExistingDataObject method, of class PlsqlFileLocatorServiceImpl.
    */
   @Test
   public void testGetExistingDataObject() throws DataObjectNotFoundException {
      System.out.println("getExistingDataObject");
      final FileObject rootFolderA = FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_A));
      PlsqlFileLocatorServiceImpl instance = new PlsqlFileLocatorServiceImpl();
      instance.registerFolder(PROJECT_A, rootFolderA);
      DataObject dataObject = DataObject.find(FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_A + FNDSECURITYPERUSER_RDF)));
      String objectName = "FND_SECURITY_PER_USER_RPI";
      PlsqlBlockType objectType = PlsqlBlockType.PACKAGE;
      Project project = mockProject(PROJECT_A);
      DatabaseConnectionManager mockedConnectionManager = mock(DatabaseConnectionManager.class);
      when(mockedConnectionManager.getInstance(project)).thenReturn(mockedConnectionManager);
      DataObject result = instance.getExistingDataObject(null, objectName, objectType, project);
      assertEquals(dataObject, result);
      result = instance.getExistingDataObject(dataObject, objectName, objectType, project);
      assertEquals(dataObject, result);
      result = instance.getExistingDataObject(dataObject, "nonExisting", PlsqlBlockType.PACKAGE, project);
      assertNull(result);
      result = instance.getExistingDataObject(dataObject, objectName, objectType, null);
      assertNull(result);
      dataObject = DataObject.find(FileUtil.toFileObject(new File(getDataDir(), WORKSPACE_A + FNDUSER_BODY_PATH)));
      result = instance.getExistingDataObject(dataObject, "FndUser", PlsqlBlockType.PACKAGE_BODY, project);
      assertEquals(dataObject, result);
      result = instance.getExistingDataObject(dataObject, "FndUser", PlsqlBlockType.PACKAGE, project);
      assertNull(result);
   }

   private Project mockProject(final String projectName) {
      Project mockProj = mock(Project.class);
      Lookup mockLookup = mock(Lookup.class);
      ProjectInformation mockInfo = mock(ProjectInformation.class);
      when(mockProj.getLookup()).thenReturn(mockLookup);
      when(mockLookup.lookup(ProjectInformation.class)).thenReturn(mockInfo);
      when(mockInfo.getDisplayName()).thenReturn(projectName);
      return mockProj;
   }
}
