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
package org.netbeans.modules.plsql.usage;

public class PlsqlElement {

   private final String parent;
   private final String objName;
   private final String objType;
   private int line;
   private int modifiedLine;
   private int column;
   private String name;
   private final String usageName;
   private String lineText;

   public PlsqlElement(final String usageName, final String name, final boolean isParent, final String objName, final String objType, final int column, final int line) {
      this.usageName = usageName;
      this.name = name;
      this.objName = objName;
      this.objType = objType;
      this.column = column;
      this.line = line;
      this.parent = isParent ? null : objName;
   }

   public PlsqlElement(final String usageName, final boolean isParent, final String objName, final String objType) {
      this.usageName = usageName;
      this.objName = objName;
      this.objType = objType;
      if (!isParent) {
         this.parent = objName;
      } else {
         this.parent = null;
      }
   }

   public int getColumn() {
      return column;
   }

   public void setColumn(final int column) {
      this.column = column;
   }

   public int getLine() {
      return line;
   }

   public void setLine(final int line) {
      this.line = line;
   }

   public int getModifiedLine() {
      return modifiedLine == 0 ? line : modifiedLine;
   }

   public void setModifiedLine(final int modifiedLine) {
      this.modifiedLine = modifiedLine;
   }

   public String getName() {
      return name;
   }

   public void setName(final String name) {
      this.name = name;
   }

   public String getObjType() {
      return objType;
   }

   public String getParent() {
      return parent;
   }

   public String getObjName() {
      return objName;
   }

   public String getUsageName() {
      return usageName;
   }

   public String getLineText() {
      return lineText;
   }

   public void setLineText(final String lineText) {
      this.lineText = lineText;
   }

   @Override
   public String toString() {
      return name;
   }
}
