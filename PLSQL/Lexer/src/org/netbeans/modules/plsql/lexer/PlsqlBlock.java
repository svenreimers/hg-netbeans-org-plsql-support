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
package org.netbeans.modules.plsql.lexer;

import java.util.ArrayList;
import java.util.List;

/**
 * A block of PL/SQL code.
 */
public class PlsqlBlock {

   private int startOffset;
   private int endOffset;
   private int previousStart = -1;
   private int previousEnd = -1;
   private PlsqlBlockType type;
   private String name;
   private String alias;
   private String prefix = "";
   private PlsqlBlock parent;
   private List<PlsqlBlock> children = new ArrayList<PlsqlBlock>();

   public PlsqlBlock(int start, int end, String name, String alias, PlsqlBlockType type) {
      this.startOffset = start;
      this.endOffset = end;
      this.name = name;
      this.type = type;
      this.alias = alias;
   }

   public PlsqlBlock getParent() {
      return parent;
   }

   /**
    * Method used to clange Plsql Block name
    * @param name
    */
   public void setName(String name) {
      this.name = name;
   }

   /**
    * Method that will return the alias of the Plsql block
    * @return
    */
   public String getAlias() {
      return this.alias;
   }

   /**
    * Return start offset of the block
    * @return
    */
   public int getStartOffset() {
      return startOffset;
   }

   /**
    * Return block type
    * @return
    */
   public PlsqlBlockType getType() {
      return type;
   }

   /**
    * Return block name
    * @return
    */
   public String getName() {
      return name;
   }

   /**
    * Return end offset of the block
    */
   public int getEndOffset() {
      return endOffset;
   }

   /**
    * set start offset of the block
    * @param start
    */
   public void setStartOffset(int start) {
      startOffset = start;
   }

   /**
    * set end offset of the block
    * @param end
    */
   public void setEndOffset(int end) {
      endOffset = end;
   }

   /**
    * Add child block to this block
    * @param childBlock
    */
   public void addChild(PlsqlBlock child) {
      children.add(child);
      child.parent = this;
   }

   /**
    * Get child blocks
    * @return
    */
   public List<PlsqlBlock> getChildBlocks() {
      return children;
   }

   /**
    * Get child count
    * @return
    */
   public int getChildCount() {
      return children.size();
   }

   /**
    * Return previous start offset of this block
    * If not set will return -1
    * @return
    */
   public int getPreviousStart() {
      return previousStart;
   }

   /**
    * Method that will set previous start
    * @param preStart
    */
   public void setPreviousStart(int preStart) {
      this.previousStart = preStart;
   }

   /**
    * Return previous end offset of this block
    * If not set will return -1
    * @return
    */
   public int getPreviousEnd() {
      return previousEnd;
   }

   /**
    * Method that will set previous end
    * @param preEnd
    */
   public void setPreviousEnd(int preEnd) {
      this.previousEnd = preEnd;
   }

   /**
    * Method that will set the prefix of the block if any
    * @param prefix
    */
   public void setPrefix(String prefix) {
      this.prefix = prefix;
   }

   public void setParent(PlsqlBlock parent) {
      this.parent = parent;
   }

   /**
    * Method that will return the prefix of the block
    * @return
    */
   public String getPrefix() {
      return this.prefix;
   }

   @Override
   public String toString() {
      return toString(false);
   }

   public String toString(boolean recursive) {
      return toString(recursive, 0);
   }

   private String toString(boolean recursive, int level) {
      StringBuilder builder = new StringBuilder();
      builder.append(String.format("[%1$5d - %2$5d] ", startOffset, endOffset));
      for (int i = 0; i < level * 2; i++) {
         builder.append(" ");
      }
      builder.append(type).append(": \"").append(name).append("\"\n");
      if (recursive) {
         for (PlsqlBlock child : children) {
            builder.append(child.toString(true, level + 1));
         }
      }
      return builder.toString();
   }
}
