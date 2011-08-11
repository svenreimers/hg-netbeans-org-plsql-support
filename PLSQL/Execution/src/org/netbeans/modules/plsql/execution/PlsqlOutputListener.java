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
package org.netbeans.modules.plsql.execution;

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.hyperlink.PlsqlGoToDbImplAction;
import java.io.File;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.*;
import org.openide.nodes.Node;
import org.openide.util.actions.SystemAction;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

public class PlsqlOutputListener  implements OutputListener {

   private List docLinesArray;
   private int lineNo;
   private int position;
   private String originalFileName=null;
   private String objectName=null;
   private Project project;
   private DatabaseContentManager cache;

   public PlsqlOutputListener() {
   }

   public PlsqlOutputListener(Project project, String objectName, int lineNo) {
      this.project = project;
      this.objectName = objectName;
      this.lineNo = lineNo;
      DatabaseConnectionManager provider = DatabaseConnectionManager.getInstance(project);
      this.cache = provider!=null ? DatabaseContentManager.getInstance(provider.getTemplateConnection()) : null;
   }
   
   public void outputLineSelected(OutputEvent outputevent) {
   }

   public void outputLineAction(OutputEvent outputEvent) {
      if (originalFileName != null) {
         try {
            File file = new File(originalFileName);
            if (file.exists()) {
               DataObject dObject = DataFolder.find(FileUtil.toFileObject(file));
               OpenCookie openCookie = dObject.getCookie(OpenCookie.class);
               openCookie.open();
               Node n = dObject.getNodeDelegate();
               EditorCookie ec = dObject.getCookie(EditorCookie.class);        
               if (ec != null) {
                  JEditorPane panes[] = ec.getOpenedPanes();
                  if (panes.length > 0) {
                     JEditorPane pane = panes[0];
                     int caretPos = getCaretPositionFromLineNumber(lineNo, pane.getDocument());
                     pane.setCaretPosition(caretPos);
                  }
               }
            } else {
               JOptionPane.showMessageDialog(null, "Required File Not found");
            }
         } catch (DataObjectNotFoundException ex) {
            ex.printStackTrace();
         }
      } else if(objectName!=null && cache!=null && project!=null) { //navigate to object
         PlsqlGoToDbImplAction action = SystemAction.get(PlsqlGoToDbImplAction.class);
         if(cache.isFunction(objectName)) {
            action.goToFunction(objectName, project, lineNo);
         } else if(cache.isProcedure(objectName)) {
            action.goToProcedure(objectName, project, lineNo);            
         } else {
            action.goToPackage(objectName, project, null, lineNo);            
         }
      }
   }

   public void outputLineCleared(OutputEvent outputevent) {
   }

   public int getCaretPositionFormLineNumber(int lineNo, int pos) {
      if (getDocLinesArray() == null) {
         throw new RuntimeException("DocLinesArray is Null");
      }
      int caretPos = 0;
      for (int i = 0; i < lineNo; i++) {
         String line = (String) getDocLinesArray().get(i);
         caretPos += line.length();
      }

      caretPos += pos;
      return caretPos;
   }

   public int getCaretPositionFromLineNumber(int lineNo, Document doc) {
      String docText = "";
      try {
         docText = doc.getText(0, doc.getLength());
      } catch (BadLocationException ex) {
         ex.printStackTrace();
      } catch (NullPointerException ex) {
         ex.printStackTrace();
      }
      
      int caret = 0;
      int linebrakCount = 0;
      for (int i = 0; i < docText.length() && linebrakCount + 1 < lineNo; i++) {
         caret++;
         if (docText.charAt(i) == '\n') {
            linebrakCount++;
         }
      }

      return caret;
   }

   public List getDocLinesArray() {
      return docLinesArray;
   }

   public void setDocLinesArray(List docLinesArray) {
      this.docLinesArray = docLinesArray;
   }

   public int getLineNo() {
      return lineNo;
   }

   public void setLineNo(int lineNo) {
      this.lineNo = lineNo;
   }

   public int getPosition() {
      return position;
   }

   public void setPosition(int position) {
      this.position = position;
   }

   public String getOriginalFileName() {
      return originalFileName;
   }

   public void setOriginalFileName(String originalFileName) {
      this.originalFileName = originalFileName;
   }
}
