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

import java.util.Set;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.util.HashSet;
import org.junit.Test;
import static org.junit.Assert.*;

public class PlsqlSearchObjectTest {

   @Test(expected = IllegalArgumentException.class)
   public void testConstructor() {
      System.out.println("constructor");
      new PlsqlSearchObject(null, "notSame");
      new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, null);

   }

   /**
    * Test of toString method, of class PlsqlSearchObject.
    */
   @Test
   public void testToString() {
      System.out.println("toString");
      PlsqlSearchObject instance = new PlsqlSearchObject(PlsqlBlockType.VIEW, "Blapp");
      String expResult = "VIEW:BLAPP";
      String result = instance.toString();

      instance = new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "Client_SYS");
      expResult = "PACKAGE_BODY:CLIENT_SYS";
      result = instance.toString();
      assertEquals(expResult, result);
      instance = new PlsqlSearchObject(PlsqlBlockType.valueOf("PACKAGE_BODY"), "Client_SYS");
      expResult = "PACKAGE_BODY:CLIENT_SYS";
      result = instance.toString();
      assertEquals(expResult, result);
   }

   /**
    * Test of getKey method, of class PlsqlSearchObject.
    */
   @Test
   public void testGetKey() {
      System.out.println("getKey");
      PlsqlSearchObject instance = new PlsqlSearchObject(PlsqlBlockType.VIEW, "Blapp");
      String expResult = "VIEW:BLAPP";
      String result = instance.getKey();
      assertEquals(expResult, result);

   }

   /**
    * Test of hashCode method, of class PlsqlSearchObject.
    */
   @Test
   public void testHashCode() {
      System.out.println("hashCode");
//      PlsqlSearchObject instance = new PlsqlSearchObject(PlsqlBlockType.VIEW, "Blapp");
////      int expResult = 1325362819;
//      int result = instance.hashCode();
//      System.out.println("hashCode=" + result);
//      assertTrue(result > 11767);
//      instance = new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "same");
////      expResult = 588010373;
//      result = instance.hashCode();
//      System.out.println("hashCode=" + result);
//      assertTrue(result > 11767);

      PlsqlSearchObject instance1 = new PlsqlSearchObject(PlsqlBlockType.VIEW, "Client_SYS");
      PlsqlSearchObject instance2 = new PlsqlSearchObject(PlsqlBlockType.VIEW, "CLIENT_SYS");

      Set<PlsqlSearchObject> searchObjects = new HashSet<PlsqlSearchObject>();
      searchObjects.add(instance1);
      assertTrue(searchObjects.contains(instance1));
      assertTrue(searchObjects.contains(instance2));
   }

   /**
    * Test of equals method, of class PlsqlSearchObject.
    */
   @Test
   public void testEquals() {
      System.out.println("equals");
      PlsqlSearchObject object1 = new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "Client_SYS");
      PlsqlSearchObject object2 = new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "Client_SYS");
      assertEquals(object1, object2);

      assertTrue(object1.equals(object1));
      assertTrue(object1.equals(object2));
      assertTrue(object2.equals(object1));
      assertFalse(object1.equals(null));
      assertFalse(object1.equals(new Object()));
      assertFalse(object1.equals(new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "notSame")));
      assertFalse(object1.equals(new PlsqlSearchObject(PlsqlBlockType.PACKAGE, "notSame")));

      //mixed case
      object2 = new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "CLIENT_SYS");
      assertEquals("mixed case", object1, object2);

//      //searching without type
//      object1 = new PlsqlSearchObject(PlsqlBlockType.PACKAGE_BODY, "Client_SYS");
//      object2 = new PlsqlSearchObject(null, "Client_SYS");
//      assertEquals("searching without type", object1, object2);


//      Object obj = null;
//      PlsqlSearchObject instance = null;
//      boolean expResult = false;
//      boolean result = instance.equals(obj);
//      assertEquals(expResult, result);
   }
}
