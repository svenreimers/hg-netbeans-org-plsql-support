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
package org.netbeans.modules.plsql.hyperlink.util;

import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 * Utility class containing  comman methods that can be used
 * by the hyperlink providers
 * @author YaDhLK
 */
public class PlsqlHyperlinkUtil {

    private PlsqlHyperlinkUtil() { } //singleton    
    
   /**
    * Get Return next non whitespace token for files
    * @param ts
    * @param ignoreComment: if true will ignore comments also
    * @return
    */
   public static boolean getNextNonWhitespace(TokenSequence<PlsqlTokenId> ts, boolean ignoreComment) {
      boolean moveNext = ts.moveNext();
      Token<PlsqlTokenId> tmp = ts.token();


      while (moveNext) {
         if (tmp.id() == PlsqlTokenId.WHITESPACE) {
            moveNext = ts.moveNext();
            tmp = ts.token();
         } else {
            if ((ignoreComment == true) && (tmp.id() == PlsqlTokenId.LINE_COMMENT
                    || tmp.id() == PlsqlTokenId.BLOCK_COMMENT)) {
               moveNext = ts.moveNext();
               tmp = ts.token();
            } else {
               break;
            }

         }
      }

      return moveNext;
   }

   /**
    * Method that will return the relevant block factory for the document
    * @param doc
    * @return
    */
   public static PlsqlBlockFactory getBlockFactory(Document doc) {
      Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof Lookup.Provider) {
         return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
      }
      return null;
   }

   /**
    * Method that will return the relevant block factory for the dataobject
    * @param obj
    * @return
    */
   public static PlsqlBlockFactory getBlockFactory(DataObject obj) {
      return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
   }

   /**
    * Method that will open the given contents in a temp file
    * @param objName
    * @param type TABLE/VIEW
    * @param conn
    * @param project
    */
   public static void openAsTempFile(String objName, PlsqlBlockType type, DatabaseConnection conn, Project project, Document doc) throws NotConnectedToDbException {
      Object obj = null;
      if (doc != null) {
         obj = doc.getProperty(Document.StreamDescriptionProperty);
      }
      if (obj == null || obj instanceof DataObject) {
         DataObject dataObj = PlsqlFileUtil.fetchAsTempFile(objName, type, conn, project, (DataObject) obj);

         if (dataObj != null) {
            OpenCookie openCookie = dataObj.getCookie(OpenCookie.class);
            openCookie.open();
         }
      }
   }

   /**
    * Method that will set caret position of the given dataobject
    * to the view identified by the tokenImage
    * @param dataObject
    * @param doc
    * @param viewName
    * @return
    */
   public static boolean setCaretOfView(DataObject dObject, String viewName) {
      EditorCookie ec = dObject.getCookie(EditorCookie.class);
      if (!PlsqlFileUtil.prepareDocument(ec)) {
         return false;
      }

      OpenCookie openCookie = dObject.getCookie(OpenCookie.class);
      if (ec != null) {
         int defOffset = -1;
         boolean isMatchFound = false;

         PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(dObject);
         blockFactory.initHierarchy(ec.getDocument());
         if (blockFactory != null) {
            PlsqlBlock block = findMatchingBlockForView(blockFactory.getBlockHierarchy(), viewName);
            if (block != null) {
               isMatchFound = true;
               defOffset = block.getStartOffset();
            }
         }

         if (isMatchFound) {
            openCookie.open();
            JEditorPane[] panes = ec.getOpenedPanes();
            if (panes.length > 0) {
               JEditorPane pane = panes[0];
               pane.setCaretPosition(defOffset);
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Method that will find & return the mathing View
    * @param blockHier
    * @return
    */
   private static PlsqlBlock findMatchingBlockForView(List<PlsqlBlock> blockHier, String viewName) {
      PlsqlBlock match = null;

      for (int i = 0; i < blockHier.size(); i++) {
         PlsqlBlock temp = blockHier.get(i);
         if (temp.getName().equalsIgnoreCase(viewName)) {
            if (temp.getType() == VIEW) {
               match = temp;
               break;
            }
         }

         match = findMatchingBlockForView(temp.getChildBlocks(), viewName);
         if (match != null) {
            break;
         }
      }

      return match;
   }

   public static String getWordAtPosition(String image, int offset, int position) {
      int start = position - offset;
      String token = "";
      if (start >= 0) {
         char ch = image.charAt(start);
         if (Character.isJavaIdentifierPart(ch)) {
            token = token + ch;
         }
         start++;
      }

      if (!token.equals("")) {
         //go forward
         while (start < image.length()) {
            char ch = image.charAt(start);
            if (Character.isJavaIdentifierPart(ch)) {
               token = token + ch;
            } else {
               break;
            }

            start++;
         }

         //go backward
         start = position - offset - 1;
         while (start >= 0) {
            char ch = image.charAt(start);
            if (Character.isJavaIdentifierPart(ch)) {
               token = ch + token;
            } else {
               break;
            }

            start--;
         }

         return token;
      }

      return null;
   }

   /**
    * Set caret position of the data object to the given offset
    * @param dataObject
    * @param offset
    */
   public static void setCaretPos(DataObject dataObject, int offset) {
      if (dataObject == null) {
         return;
      }

      OpenCookie openCookie = dataObject.getCookie(OpenCookie.class);
      openCookie.open();
      if (offset != -1) {
         EditorCookie ec = dataObject.getCookie(EditorCookie.class);
         JEditorPane[] panes = ec.getOpenedPanes();
         if (panes.length > 0) {
            JEditorPane pane = panes[0];
            pane.setCaretPosition(offset);
            return;
         }
      }
   }

   /**
    * Return whether this is the method definition/implementation or an usage of the method
    * @param doc
    * @param methodName
    * @param offset
    * @return
    */
   public static boolean isMethodDefinition(Document doc, String methodName, int offset) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         ts.move(offset);
         Token<PlsqlTokenId> token = ts.token();
         if (ts.moveNext()) {
            token = ts.token();
            PlsqlTokenId tokenID = token.id();

            if (tokenID == PlsqlTokenId.IDENTIFIER) {
               if (token.text().toString().equals(methodName)) {
                  while (ts.movePrevious()) {
                     token = ts.token();
                     if (token.text().toString().equalsIgnoreCase("FUNCTION")
                             || token.text().toString().equalsIgnoreCase("PROCEDURE")) {
                        return true;
                     } else if (token.id() != PlsqlTokenId.WHITESPACE) {
                        break;
                     }
                  }
               }
            }
         }
      }
      return false;
   }

   /**
    * Check whether a method block with the given name exists in the given hierarchy
    * @param blockHier
    * @param name
    * @param packageName
    * @param isDef
    * @return
    */
   public static PlsqlBlock getMethodBlock(List blockHier, String name, String packageName, boolean isDef) {
      for (int i = 0; i
              < blockHier.size(); i++) {
         PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
         //if this is not a procedure look at the children and return
         if (isDef && (temp.getName().equalsIgnoreCase(name)) && (temp.getType() == PROCEDURE_DEF
                 || temp.getType() == FUNCTION_DEF)) {
            return temp;
         } else if (!isDef && (temp.getName().equalsIgnoreCase(name)) && (temp.getType() == PROCEDURE_IMPL
                 || temp.getType() == FUNCTION_IMPL)) {
            return temp;
         } else if ((temp.getType() == PACKAGE_BODY
                 || temp.getType() == PACKAGE) && temp.getName().equalsIgnoreCase(packageName)) {
            return getMethodBlock(temp.getChildBlocks(), name, packageName, isDef);
         }
      }
      return null;
   }

   public static boolean isPackageExisting(DataObject dataObject, String packageName, boolean isDef) {
      PlsqlBlockFactory blockFac = PlsqlHyperlinkUtil.getBlockFactory(dataObject);
      if (blockFac != null) {
         List blockHier = blockFac.getBlockHierarchy();
         for (int i = 0; i
                 < blockHier.size(); i++) {
            PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
            if (!isDef && temp.getType() == PACKAGE_BODY && temp.getName().equalsIgnoreCase(packageName)) {
               return true;
            } else if (isDef && temp.getType() == PACKAGE && temp.getName().equalsIgnoreCase(packageName)) {
               return true;
            }
         }
      }
      return false;
   }

   public static ImageInfo getCurrentWordInfo(Document doc, int offset) {
      ImageInfo imageInfo = new ImageInfo();
      //for comments, prompt statements etc just find the word under the cursor
      int start = offset - 40 > 0 ? offset - 40 : 0;
      int length = offset + 40 > doc.getLength() ? doc.getLength() : 80;
      try {
         String candidate = doc.getText(start, length);
         //first find start of identifier
         int pos;
         for (pos = offset - start; pos > 0 && Character.isJavaIdentifierPart(candidate.charAt(pos - 1)); pos--);
         if (pos < 5) //identifier is too long to be an oracle object
         {
            return null;
         }
         imageInfo.startOffset = start + pos;
         //then find end of identifier
         for (pos = offset - start; pos < 80 && Character.isJavaIdentifierPart(candidate.charAt(pos)); pos++);
         imageInfo.endOffset = start + pos;
         if (imageInfo.endOffset - imageInfo.startOffset > 35) //identifier is too long to be an oracle object
         {
            return null;
         }
         imageInfo.image = candidate.substring(imageInfo.startOffset - start, pos);
         //Check if there's a parent object as well.
         if ('.' == candidate.charAt(imageInfo.startOffset - start - 1)) { //
            int end = imageInfo.startOffset - 1;
            start = end - 40 > 0 ? end - 40 : 0;
            length = end - start;
            candidate = doc.getText(start, length);
            for (start = length; start > 0 && Character.isJavaIdentifierPart(candidate.charAt(start - 1)); start--);
            imageInfo.parentImage = candidate.substring(start);
         }

         return imageInfo;
      } catch (BadLocationException ex) {
         //shouldn't happen...
      }

      return null;
   }

   public static class ImageInfo {

      public String image = "";
      public int startOffset = -1;
      public int endOffset = -1;
      public String parentImage = null;
   }

   public static int getCaretPosition(final EditorCookie editorCookie) {
      int result = -1;
      final JEditorPane[] panes = editorCookie.getOpenedPanes();
      if ((panes != null) && (panes.length != 0)) {
         final Caret caret = panes[0].getCaret();
         //Not a selection
         if (caret.getDot() == caret.getMark()) {
            result = caret.getDot();
         } else //a selection get the start of the selection
         {
            result = caret.getMark();
         }
      }
      return result;
   }

   /**
    * Method that will go to the DEFINE statement of the given alias
    * @param doc
    * @param target
    */
   public static void goToDefine(String tokenImage, Document doc, JTextComponent target) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         ts.moveStart();
         while (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();

            if (tokenID == PlsqlTokenId.SQL_PLUS) {
               String image = readLine(ts, token);
               if ((image.startsWith("DEF "))
                       || (image.startsWith("DEFI "))
                       || (image.startsWith("DEFIN "))
                       || (image.startsWith("DEFINE "))) {
                  int defOffset = token.offset(tokenHierarchy);

                  StringTokenizer tokenizer = new StringTokenizer(image);
                  tokenizer.nextToken();
                  if (tokenizer.hasMoreTokens()) {
                     image = tokenizer.nextToken();
                  }

                  if (image.equalsIgnoreCase(tokenImage.substring(1))) {
                     //We have found the define
                     target.setCaretPosition(defOffset);
                     return;
                  } else {
                     ts.move(defOffset);
                     ts.moveNext();
                  }
               }
            }
         }
      }
   }

   /**
    * Method that will return the package name of this file
    * @param dataObject
    * @return
    */
   public static String getPackageName(final DataObject dataObject) {
      final PlsqlBlockFactory blockFac = getBlockFactory(dataObject);
      return getPackageName(blockFac);
   }

   /**
    * Method that will return the package name of this file
    * @param doc
    * @return
    */
   public static String getPackageName(final Document doc) {
      final PlsqlBlockFactory blockFac = getBlockFactory(doc);
      return getPackageName(blockFac);
   }

   private static String getPackageName(final PlsqlBlockFactory blockFac) {
      String packageName = "";
      if (blockFac != null) {
         final List blockHier = blockFac.getBlockHierarchy();
         for (int i = 0; i < blockHier.size(); i++) {
            final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
            if (temp.getType() == PACKAGE || temp.getType() == PACKAGE_BODY) {
               packageName = temp.getName();
               break;
            }
         }
      }
      return packageName;
   }

   private static String readLine(final TokenSequence<PlsqlTokenId> ts, Token<PlsqlTokenId> token) {
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
