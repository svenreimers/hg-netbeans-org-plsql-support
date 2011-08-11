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
package org.netbeans.modules.plsql.utilities.validation;

import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;

/**
 *
 * @author CORPNET\chrlse
 */
public class PlsqlFileValidatorServiceImplTest {

   public PlsqlFileValidatorServiceImplTest() {
   }

//   /**
//    * Test of isValid method, of class PlsqlFileValidatorServiceImpl.
//    */
//   public void testIsValid_FileObject() {
//      System.out.println("isValid");
//      FileObject fileObject = null;
//      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
//      boolean expResult = false;
//      boolean result = instance.isValidPackage(fileObject);
//      assertEquals(expResult, result);
//      // TODO review the generated test code and remove the default call to fail.
//      fail("The test case is a prototype.");
//   }
//
//   /**
//    * Test of getSiblingExt method, of class PlsqlFileValidatorServiceImpl.
//    */
//   public void testGetSiblingExt_DataObject() throws IOException {
//      System.out.println("getSiblingExt");
//      FileSystem fs = FileUtil.createMemoryFileSystem();
//      FileObject specFileObject = fs.getRoot().createData("test", "spec");
//      FileObject bodyFileObject = fs.getRoot().createData("test", "body");
//      DataObject specDataObject = DataObject.find(specFileObject);
//      DataObject bodyDataObject = DataObject.find(bodyFileObject);
//      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
//
//      DataObject siblingDataObject = instance.getSiblingExt(specDataObject);
//      assertTrue(instance.isValidPackageBody(siblingDataObject));
//
//      siblingDataObject = instance.getSiblingExt(bodyDataObject);
//      assertTrue(instance.isValidPackageSpec(siblingDataObject));
//   }

   /**
    * Test of getSiblingExt method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testGetSiblingExt_String() {
      System.out.println("getSiblingExt");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      String siblingExt = "body";
      String expResult = "spec";
      String result = instance.getSiblingExt(siblingExt);
      assertEquals(expResult, result);

      siblingExt = "spec";
      expResult = "body";
      result = instance.getSiblingExt(siblingExt);
      assertEquals(expResult, result);

      siblingExt = "speC";
      expResult = "body";
      result = instance.getSiblingExt(siblingExt);
      assertEquals(expResult, result);

      siblingExt = "BoDy";
      expResult = "spec";
      result = instance.getSiblingExt(siblingExt);
      assertEquals(expResult, result);

   }

   /**
    * Test of getDefaultBodyExt method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testGetDefaultBodyExt() {
      System.out.println("getDefaultBodyExt");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      String expResult = "body";
      String result = instance.getDefaultPackageBodyExt();
      assertEquals(expResult, result);
   }

   /**
    * Test of getDefaultSpecExt method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testGetDefaultSpecExt() {
      System.out.println("getDefaultSpecExt");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      String expResult = "spec";
      String result = instance.getDefaultPackageSpecExt();
      assertEquals(expResult, result);
   }

   /**
    * Test of isValidPackage method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidPackage_DataObject() throws IOException {
      System.out.println("isValidPackage");
      FileSystem fs = FileUtil.createMemoryFileSystem();
      FileObject fileObject = fs.getRoot().createData("test", "spec");
      DataObject dataObject = DataObject.find(fileObject);
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      boolean expResult = true;
      boolean result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "SPEC");
      dataObject = DataObject.find(fileObject);
      expResult = true;
      result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "SPec");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);

      dataObject = null;
      expResult = false;
      result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "body");
      dataObject = DataObject.find(fileObject);
      expResult = true;
      result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "BODY");
      dataObject = DataObject.find(fileObject);
      expResult = true;
      result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "BodY");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackage(dataObject);
      assertEquals(expResult, result);
   }

   /**
    * Test of isValidPackage method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidPackage_FileObject() throws IOException {
      System.out.println("isValidPackage");
      FileSystem fs = FileUtil.createMemoryFileSystem();
      FileObject fileObject = fs.getRoot().createData("test", "spec");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      boolean expResult = true;
      boolean result = instance.isValidPackage(fileObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "SPec");
      expResult = true;
      result = instance.isValidPackage(fileObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "");
      expResult = false;
      result = instance.isValidPackage(fileObject);
      assertEquals(expResult, result);

      fileObject = null;
      expResult = false;
      result = instance.isValidPackage(fileObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "BodY");
      expResult = true;
      result = instance.isValidPackage(fileObject);
      assertEquals(expResult, result);
   }

   /**
    * Test of isValidPackageSpec method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidPackageSpec_DataObject() throws IOException {
      System.out.println("isValidPackageSpec");
      FileSystem fs = FileUtil.createMemoryFileSystem();
      FileObject fileObject = fs.getRoot().createData("test", "spec");
      DataObject dataObject = DataObject.find(fileObject);
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      boolean expResult = true;
      boolean result = instance.isValidPackageSpec(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "SPEC");
      dataObject = DataObject.find(fileObject);
      expResult = true;
      result = instance.isValidPackageSpec(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "SPec");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackageSpec(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackageSpec(dataObject);
      assertEquals(expResult, result);

      dataObject = null;
      expResult = false;
      result = instance.isValidPackageSpec(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "body");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackageSpec(dataObject);
      assertEquals(expResult, result);
   }

   /**
    * Test of isValidPackageSpec method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidPackageSpec_String() {
      System.out.println("isValidPackageSpec");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      assertTrue(instance.isValidPackageSpec("spec"));
      assertTrue(instance.isValidPackageSpec("Spec"));
      assertFalse(instance.isValidPackageSpec("body"));
      assertFalse(instance.isValidPackageSpec("db"));
      assertFalse(instance.isValidPackageSpec(""));
      assertFalse(instance.isValidPackageSpec((String) null));
   }

   /**
    * Test of isValidPackageBody method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidPackageBody_DataObject() throws IOException {
      System.out.println("isValidPackageBody");
      FileSystem fs = FileUtil.createMemoryFileSystem();
      FileObject fileObject = fs.getRoot().createData("test", "spec");
      DataObject dataObject = DataObject.find(fileObject);
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      boolean expResult = false;
      boolean result = instance.isValidPackageBody(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "SPec");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackageBody(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackageBody(dataObject);
      assertEquals(expResult, result);

      dataObject = null;
      expResult = false;
      result = instance.isValidPackageBody(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "body");
      dataObject = DataObject.find(fileObject);
      expResult = true;
      result = instance.isValidPackageBody(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "BODY");
      dataObject = DataObject.find(fileObject);
      expResult = true;
      result = instance.isValidPackageBody(dataObject);
      assertEquals(expResult, result);

      fileObject = fs.getRoot().createData("test", "BodY");
      dataObject = DataObject.find(fileObject);
      expResult = false;
      result = instance.isValidPackageBody(dataObject);
      assertEquals(expResult, result);
   }

   /**
    * Test of isValidPackageBody method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidPackageBody_String() {
      System.out.println("isValidPackageBody");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      assertTrue(instance.isValidPackageBody("body"));
      assertTrue(instance.isValidPackageBody("BoDy"));
      assertFalse(instance.isValidPackageBody("spec"));
      assertFalse(instance.isValidPackageBody("db"));
      assertFalse(instance.isValidPackageBody(""));
      assertFalse(instance.isValidPackageBody((String) null));
   }

   /**
    * Test of isValidTDB method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidTDB() throws IOException {
      System.out.println("isValidTDB");
      FileSystem fs = FileUtil.createMemoryFileSystem();
      FileObject fileObject = fs.getRoot().createData("test", "tdb");
      DataObject dataObject = DataObject.find(fileObject);
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      assertTrue(instance.isValidTDB(dataObject));

      fileObject = fs.getRoot().createData("test", "SPec");
      dataObject = DataObject.find(fileObject);
      assertFalse(instance.isValidTDB(dataObject));

      dataObject = null;
      assertFalse(instance.isValidTDB(dataObject));
   }

   /**
    * Test of getDefaultPackageBodyExt method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testGetDefaultPackageBodyExt() {
      System.out.println("getDefaultPackageBodyExt");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      String expResult = "body";
      String result = instance.getDefaultPackageBodyExt();
      assertEquals(expResult, result);
   }

   /**
    * Test of getDefaultPackageSpecExt method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testGetDefaultPackageSpecExt() {
      System.out.println("getDefaultPackageSpecExt");
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      String expResult = "spec";
      String result = instance.getDefaultPackageSpecExt();
      assertEquals(expResult, result);
   }

   /**
    * Test of isValidPackageDefault method, of class PlsqlFileValidatorServiceImpl.
    */
   @Test
   public void testIsValidPackageDefault() throws IOException {
      System.out.println("isValidPackageDefault");
      FileSystem fs = FileUtil.createMemoryFileSystem();
      PlsqlFileValidatorServiceImpl instance = new PlsqlFileValidatorServiceImpl();
      FileObject fileObject = fs.getRoot().createData("test", "spec");
      DataObject dataObject = DataObject.find(fileObject);
      assertTrue(instance.isValidPackageDefault(dataObject));

      fileObject = fs.getRoot().createData("test", "body");
      dataObject = DataObject.find(fileObject);
      assertTrue(instance.isValidPackageDefault(dataObject));

      fileObject = fs.getRoot().createData("test", "false");
      dataObject = DataObject.find(fileObject);
      assertFalse(instance.isValidPackageDefault(dataObject));

      dataObject = null;
      assertFalse(instance.isValidPackageDefault(dataObject));
   }
}
