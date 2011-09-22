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
package org.netbeans.modules.plsql.annotation;

import org.netbeans.modules.plsql.annotation.annotations.PlsqlAnnotation;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Util class for annotations added to package def/body
 * @author YADHLK
 */
public class PlsqlPackageAnnotationUtil {
    
   public static void getPackageAnnotations(final PlsqlAnnotationManager manager, final List<PlsqlBlock> blocks, final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, final Document doc) {
      for (PlsqlBlock temp : blocks) {
         if (temp.getType() == PlsqlBlockType.PACKAGE || temp.getType() == PlsqlBlockType.PACKAGE_BODY) {
            manager.resetAllowedTablesOrViews(doc, temp.getStartOffset());
            PlsqlAnnotationUtil.callBlockAnnotations(manager, annotationsToAdd, doc, temp, null, null, PlsqlAnnotationManager.annotation.getType(temp));
         } 
//         else if (temp.getType() == PlsqlBlockType.PACKAGE_BODY) {
//            manager.resetAllowedTablesOrViews(doc, temp.getStartOffset());
//            PlsqlAnnotationUtil.callBlockAnnotations(manager, annotationsToAdd, doc, temp, null, null, manager.annotation.getType(temp));
//         }
      }
   }

   public static int checkForFirstChild(final PlsqlBlock temp) {
      int offset = temp.getEndOffset();
      final Comparator<PlsqlBlock> comparator = new Comparator<PlsqlBlock>() {

         @Override
         public int compare(final PlsqlBlock block1, final PlsqlBlock block2) {
            Integer o1pos, o2pos;
            if (block1.getStartOffset() > -1 && block2.getStartOffset() > -1) {
               o1pos = Integer.valueOf(block1.getStartOffset());
               o2pos = Integer.valueOf(block2.getStartOffset());
            } else {
               o1pos = Integer.valueOf(block1.getEndOffset());
               o2pos = Integer.valueOf(block2.getEndOffset());
            }
            return o1pos.compareTo(o2pos);
         }
      };

      final List<PlsqlBlock> blocks = temp.getChildBlocks();
      Collections.sort(blocks, comparator);
      for (PlsqlBlock child : blocks) {
         if (child.getType() != PlsqlBlockType.COMMENT) {
            offset = child.getStartOffset();
            break;
         }
      }

      return offset;
   }

   public static boolean insertPackageDeclaration(final DataObject dataObj, final Document doc, final String decType, final int offset) {
      if (PlsqlAnnotationUtil.isFileReadOnly(doc)) {
         JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "File is read-only", "Error", JOptionPane.ERROR_MESSAGE);
         return false;
      }

      final PlsqlBlockFactory blockFac = dataObj.getLookup().lookup(PlsqlBlockFactory.class);
      try {
         String text = "";
         String val = "";
         if (decType.equals("MODULE")) {
            text = "\n\nmodule_ CONSTANT VARCHAR2(25) := ";
            if (blockFac.getDefine("&MODULE").equals("&MODULE")) { //define not there
               val = JOptionPane.showInputDialog("Module:");
               if (val == null || val.trim().equals("")) {
                  return false;
               }
               val = "'" + val + "'";
            } else {
               val = "'&MODULE'";
            }
         } else {
            text = "\n\nlu_name_ CONSTANT VARCHAR2(25) := ";
            if (blockFac.getDefine("&LU").equals("&LU")) { //define not there
               val = JOptionPane.showInputDialog("LU Name:");
               if (val == null || val.trim().equals("")) {
                  return false;
               }
               val = "'" + val + "'";
            } else {
               val = "'&LU'";
            }
         }

         text = text + val + ";";

         doc.insertString(offset, text, null);
         return true;
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
      return false;
   }
}
