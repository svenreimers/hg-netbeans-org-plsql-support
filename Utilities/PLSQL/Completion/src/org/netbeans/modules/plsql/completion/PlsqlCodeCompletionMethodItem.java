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
package org.netbeans.modules.plsql.completion;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import javax.swing.ImageIcon;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;

/**
 *
 * @author chawlk
 */
public class PlsqlCodeCompletionMethodItem extends PlsqlCodeCompletionItem {

   private String returnType = "";
   private boolean argument = false;
   
   /** Creates a new instance of PLSQLCodeCompletionItem */
   public PlsqlCodeCompletionMethodItem(String text, CompletionItemType type, String documentation) {
      super(text, type);
      setDocumentation(documentation);
      returnType = extractReturnType(documentation);
   }

   private String extractReturnType(String documentation) {
      //the syntax we're looking for is:
      //&NBSP;RETURN&NBSP;&NBSP;</font><font color="blue">VARCHAR2</font>
      int pos = documentation.lastIndexOf("&NBSP;RETURN&NBSP;");
      int endOfStatement=-1;
      if(pos>-1) {
         pos = documentation.indexOf("<font color=\"blue\">", pos);
         if(pos>-1)
            endOfStatement = documentation.indexOf("</font>", pos);
      }
      if(pos<0 || endOfStatement<0)
         return "";
      return documentation.substring(pos+"<font color=\"blue\">".length(), endOfStatement).trim();
   }
   
   @Override
   public void render(Graphics graphics, Font font, Color color, Color color0, int width, int height, boolean selected) {
      ImageIcon icon = null;
      if(!argument)
         icon = getIcon();
      CompletionUtilities.renderHtml(argument? null : icon, getText(), returnType, graphics, font,
                                     (selected ? Color.white : getFielColor()), width, height, selected);
   }
   
   public int getSortPriority() {
      return argument?0:3;
   }   
   
   public PlsqlCodeCompletionMethodItem cloneToArgumentVersion(int arg) {
      //parse documentation to find argument in question - later change storage of doc to have the list here and not just the text blob
      String documentation = getDocumentation();
      int pos = documentation.indexOf("<tr>");
      while(pos>0&& arg>-1) {
         if(arg==0) { //correct arg
            int endPos = documentation.indexOf("</td>", pos);
            StringBuilder newDoc = new StringBuilder();
            newDoc.append(documentation.substring(0, pos+4)); //include <tr>
            newDoc.append("<td>=></td>"); //replace empty column with marker
            pos = documentation.indexOf("<td>", pos+8); 
            endPos = documentation.indexOf("</td>", pos);
            String argName = documentation.substring(pos+4, endPos);
            newDoc.append("<td><b>");
            newDoc.append(argName);
            newDoc.append("</b>");
            newDoc.append(documentation.substring(endPos));
            PlsqlCodeCompletionMethodItem item = new PlsqlCodeCompletionMethodItem(argName, getType(), newDoc.toString());
            item.setArgument();
            return item;
         }
         arg--;
         pos = documentation.indexOf("<tr>", pos+1);
      }
      //if correct argument not found return the entire method
      return null;
   }

   private void setArgument() {
      argument = true;
      returnType = "";
   }
}
