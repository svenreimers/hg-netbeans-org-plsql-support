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
import org.netbeans.spi.editor.completion.support.CompletionUtilities;

/**
 *
 * @author chawlk
 */
public class PlsqlCodeCompletionColumnItem extends PlsqlCodeCompletionItem {

   private String datatype;
   private String viewName;
   private boolean showDatatype=false;
   
   /** Creates a new instance of PLSQLCodeCompletionItem */
   public PlsqlCodeCompletionColumnItem(String text, CompletionItemType type, String datatype, String viewName) {
      super(text, type);
      this.datatype=datatype;
      this.viewName=viewName;
      //setDocumentation("From: " + viewName);
   }
   
   public int getPreferredWidth(Graphics graphics, Font font) {
      return CompletionUtilities.getPreferredWidth(getText(), showDatatype ? datatype : viewName, graphics, font);
   }
   
   public void render(Graphics graphics, Font font, Color color, Color color0, int width, int height, boolean selected) {
      String extraInfo = showDatatype ? datatype : viewName;
      CompletionUtilities.renderHtml(getIcon(), getText(), extraInfo, graphics, font,
                                     (selected ? Color.white : getFielColor()), width, height, selected);
   }
   
   public int getSortPriority() {
      return 1;
   }   
   
   public void showDatatype() {
      showDatatype = true;
   }
   
   public void showViewOrTable() {
      showDatatype = false;
   }
}
