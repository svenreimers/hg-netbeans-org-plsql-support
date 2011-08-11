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
package org.netbeans.modules.plsqlsupport.db;

import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class DatabaseParseUtilitiesTest {

   public DatabaseParseUtilitiesTest() {
   }

   @BeforeClass
   public static void setUpClass() throws Exception {
   }

   @AfterClass
   public static void tearDownClass() throws Exception {
   }

   @Before
   public void setUp() {
   }

   @After
   public void tearDown() {
   }

   /**
    * Test of getColumnDefinition method, of class DatabaseParseUtilities.
    */
   @Test
   public void testGetColumnDefinition() {
      System.out.println("getColumnDefinition");
      String statement = "SELECT    A ,   B_API(user(1,2), owner_) B \n , C C\nFROM X";
      Map<String, String> expResult = new HashMap<String, String>();
      expResult.put("A", "A");
      expResult.put("B", "B_API(user(1,2), owner_)");
      expResult.put("C", "C");
      Map<String, String> result = DatabaseParseUtilities.getColumnDefinition(statement);
      assertEquals(expResult, result);
      statement = "SELECT country_code                   country_code,\n"
            + "Iso_Country_API.Decode(country_code) country,\n"
            + "display_layout                 display_layout,\n"
            + "edit_layout                    edit_layout,\n"
            + "default_display_layout         default_display_layout,\n"
            + "default_edit_layout            default_edit_layout,\n"
            + "rowid                         objid,\n"
            + "ltrim(lpad(to_char(rowversion),2000))                    objversion\n"
            + "FROM   address_presentation_tab\n"
            + "WITH   read only\n";
      expResult.clear();
      expResult.put("COUNTRY_CODE", "country_code");
      expResult.put("COUNTRY", "Iso_Country_API.Decode(country_code)");
      expResult.put("DISPLAY_LAYOUT", "display_layout");
      expResult.put("EDIT_LAYOUT", "edit_layout");
      expResult.put("DEFAULT_DISPLAY_LAYOUT", "default_display_layout");
      expResult.put("DEFAULT_EDIT_LAYOUT", "default_edit_layout");
      expResult.put("OBJID", "rowid");
      expResult.put("OBJVERSION", "ltrim(lpad(to_char(rowversion),2000))");
      result = DatabaseParseUtilities.getColumnDefinition(statement);
      assertEquals(expResult, result);
   }
}
