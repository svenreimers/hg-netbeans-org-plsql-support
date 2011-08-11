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
package org.netbeans.modules.plsql.utilities;

import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.io.File;
import javax.swing.text.Document;
import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;

/**
 *
 * @author ChrLSE
 */
public class PlsqlFileUtilTest_DISABLED {
   
   public PlsqlFileUtilTest_DISABLED() {
   }

   /**
    * Test of fetchAsTempFile method, of class PlsqlFileUtil.
    */
   @Test
   public void testFetchAsTempFile() throws Exception {
      System.out.println("fetchAsTempFile");
      String objectName = "";
      PlsqlBlockType type = null;
      DatabaseConnection databaseConnection = null;
      Project project = null;
      DataObject sourceObject = null;
      DataObject expResult = null;
      DataObject result = PlsqlFileUtil.fetchAsTempFile(objectName, type, databaseConnection, project, sourceObject);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of openExistingFile method, of class PlsqlFileUtil.
    */
   @Test
   public void testOpenExistingFile() throws Exception {
      System.out.println("openExistingFile");
      Document doc = null;
      String objectName = "";
      PlsqlBlockType objectType = null;
      String fileExtension = "";
      DatabaseConnection connection = null;
      Project project = null;
      DataObject expResult = null;
      DataObject result = PlsqlFileUtil.openExistingFile(doc, objectName, objectType, project);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of getDataObject method, of class PlsqlFileUtil.
    */
   @Test
   public void testGetDataObject() {
      System.out.println("getDataObject");
      String path = "";
      DataObject expResult = null;
      DataObject result = PlsqlFileUtil.getDataObject(path);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of getDocument method, of class PlsqlFileUtil.
    */
   @Test
   public void testGetDocument() {
      System.out.println("getDocument");
      DataObject dataObject = null;
      Document expResult = null;
      Document result = PlsqlFileUtil.getDocument(dataObject);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of writeToFile method, of class PlsqlFileUtil.
    */
   @Test
   public void testWriteToFile() throws Exception {
      System.out.println("writeToFile");
      File tmpFile = null;
      String body = "";
      long lastModified = 0L;
      PlsqlFileUtil.writeToFile(tmpFile, body, lastModified);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of fileChanged method, of class PlsqlFileUtil.
    */
   @Test
   public void testFileChanged() {
      System.out.println("fileChanged");
      File tmpFile = null;
      long lastModified = 0L;
      boolean expResult = false;
      boolean result = PlsqlFileUtil.fileChanged(tmpFile, lastModified);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }

   /**
    * Test of prepareDocument method, of class PlsqlFileUtil.
    */
   @Test
   public void testPrepareDocument() {
      System.out.println("prepareDocument");
      EditorCookie ec = null;
      boolean expResult = false;
      boolean result = PlsqlFileUtil.prepareDocument(ec);
      assertEquals(expResult, result);
      // TODO review the generated test code and remove the default call to fail.
      fail("The test case is a prototype.");
   }
}
