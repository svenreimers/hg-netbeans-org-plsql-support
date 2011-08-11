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

import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.Utilities;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

public class FileExecutionUtil {

   public FileExecutionUtil() {
   }

   public static List<String> splitStringToVector(String s) {
      List<String> lines = new ArrayList<String>();
      if (s == null) {
         throw new RuntimeException("STRING IS NULL");
      }
      int nextPos;
      for (int startPos = 0; startPos < s.length(); startPos = nextPos + 1) {
         nextPos = s.indexOf("\n", startPos);
         if (nextPos == -1) {
            nextPos = s.length();
            String obj = s.substring(startPos, nextPos - 1);
            lines.add(obj);
         } else {
            String obj = s.substring(startPos, nextPos);
            lines.add(obj);
         }
      }

      return lines;
   }

   public static Document getActiveDocument() {
      JTextComponent component = Utilities.getFocusedComponent();
      if (component != null) {
         return Utilities.getDocument(component);
      }

      return null;
   }

   public static DataObject getDataObject(Document doc) {
      Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof DataObject) {
         DataObject dataObj = (DataObject) obj;
         return dataObj;
      }
      return null;
   }

   public static String getActivatedFileName(DataObject dataObj) {
      FileObject fileObject = dataObj.getPrimaryFile();
      String fileName = FileUtil.getFileDisplayName(fileObject);
      return fileName;
   }

   public static int getLineNoForOffset(Document doc, int offset) {
      int count = 1;
      try {
         int nextPos;
         String s = doc.getText(0, doc.getLength());
         for (int startPos = 0; startPos < s.length(); startPos = nextPos + 1) {
            nextPos = s.indexOf("\n", startPos);
            if (((offset >= startPos) && (offset <= nextPos)) || (nextPos == -1)) {
               break;
            }
            
            count++;
         }
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
      return count;
   }

   public static String readLine(TokenSequence<PlsqlTokenId> ts, Token<PlsqlTokenId> token) {
      String line = token.toString();
      while (ts.moveNext()) {
         token = ts.token();
         if (token.id() == PlsqlTokenId.WHITESPACE && token.text().toString().contains("\n")) {
            ts.movePrevious();
            break;
         }

         line = line + token.toString();
      }
      
      return line;
   }
}
