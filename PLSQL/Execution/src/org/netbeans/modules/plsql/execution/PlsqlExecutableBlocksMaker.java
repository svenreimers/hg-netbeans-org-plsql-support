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

import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.*;
import java.util.ArrayList;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

public class PlsqlExecutableBlocksMaker {

   public PlsqlExecutableBlocksMaker(Document doc) {
      try {
         this.doc = doc;
         dataObj = FileExecutionUtil.getDataObject(doc);
         docContent = doc.getText(0, doc.getLength());
         originalFileName = FileExecutionUtil.getActivatedFileName(dataObj);
         docLines = FileExecutionUtil.splitStringToVector(docContent);
         executableObjs = new ArrayList<PlsqlExecutableObject>();
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
   }

   /**
    * Method that will make executable objects of a plsql file
    */
   public List<PlsqlExecutableObject> makeExceutableObjects() {
      PlsqlBlockFactory blockFac = dataObj.getLookup().lookup(PlsqlBlockFactory.class);

      if (blockFac == null) {
         return null;
      }

      //check whether the initial parsing has happened
      Object object = doc.getProperty("Listener");
      if ((object == null) || (!object.equals("YES"))) {
         blockFac.initHierarchy(doc);
      } else {
         //force reparsing of the document
         blockFac.reParse(doc);
      }


      try {
         List<PlsqlBlock> blockHier = blockFac.getBlockHierarchy();
         for (int i = 0; i < blockHier.size(); i++) {
            PlsqlBlock block = blockHier.get(i);
            PlsqlBlockType type = block.getType();
            String name = block.getName();
            int start = block.getStartOffset();
            int startLine = FileExecutionUtil.getLineNoForOffset(doc, start);
            int end = block.getEndOffset();
            String content = doc.getText(start, end - start);
            PlsqlExecutableObject obj = null;

            if (type == PlsqlBlockType.VIEW) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.VIEW, start, end);
            } else if (type == PlsqlBlockType.TABLE_COMMENT) {
               executableObjs = createCommentBlocks(start, content, name, PlsqlExecutableObjectType.TABLECOMMENT, executableObjs, blockFac);
            } else if (type == PlsqlBlockType.COLUMN_COMMENT) {
               executableObjs = createCommentBlocks(start, content, name, PlsqlExecutableObjectType.COLUMNCOMMENT, executableObjs, blockFac);
            } else if (type == PlsqlBlockType.PACKAGE) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.PACKAGE, start, end);
            } else if (type == PlsqlBlockType.PACKAGE_BODY) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.PACKAGEBODY, start, end);
            } else if (type == PlsqlBlockType.PROCEDURE_IMPL) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.PROCEDURE, start, end);
            } else if (type == PlsqlBlockType.FUNCTION_IMPL) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.FUNCTION, start, end);
            } else if (type == PlsqlBlockType.DECLARE_END) {
               if (!content.trim().endsWith(";"))
                  content = content + ";";
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.DECLAREEND, start, end);
            } else if (type == PlsqlBlockType.BEGIN_END) {
               if (!content.trim().endsWith(";"))
                  content = content + ";";
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.BEGINEND, start, end);
            } else if (type == PlsqlBlockType.COMMENT) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.COMMENT, start, end);
            } else if (type == PlsqlBlockType.TRIGGER) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.TRIGGER, start, end);
            } else if (type == PlsqlBlockType.STATEMENT) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.STATEMENT, start, end);
            } else if (type == PlsqlBlockType.JAVA_SOURCE) {
               obj = new PlsqlExecutableObject(startLine, content, name, PlsqlExecutableObjectType.JAVASOURCE, start, end);
            }

            if (obj != null) {
               obj.setDocLinesArray(docLines);
               obj.setOriginalFileName(originalFileName);
               executableObjs.add(obj);
            }
         }

         //Now add the unknown objects and sort
         blockHier = sortBlockHier(blockHier);
         addOtherObjects(blockHier, executableObjs);
         sortExecutableObjects(executableObjs);
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }

      return executableObjs;
   }

   /**
    * Method that will add other objects (unrecognized blocks)
    * @param blockHier
    * @param executableObjs 
    */
   private void addOtherObjects(List<PlsqlBlock> blockHier, List<PlsqlExecutableObject> executableObjs) {
      int startDoc = doc.getStartPosition().getOffset();
      int endDocument = doc.getEndPosition().getOffset();
      try {
         for (int i = 0; i < blockHier.size(); i++) {
            PlsqlBlock block = blockHier.get(i);
            int startBlock = block.getStartOffset();
            int endBlock = block.getEndOffset();

            if (startDoc < startBlock) {
               //Create a block
               String content = doc.getText(startDoc, startBlock - startDoc);
               checkAndAddBlocks(executableObjs, content, startDoc, startBlock);
            }
            startDoc = endBlock;
         }

         //After the blocks if there is something left add that
         if (startDoc != endDocument) {
            String content = doc.getText(startDoc, endDocument - startDoc);
            checkAndAddBlocks(executableObjs, content, startDoc, endDocument);
         }
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
   }

   /**
    * Method that will add unknown blocks to executable objects
    * @param executableObjs
    * @param content
    * @param startoffset
    */
   private void addUnknownBlock(List<PlsqlExecutableObject> executableObjs, String content, int startDoc) {
      int startLine = FileExecutionUtil.getLineNoForOffset(doc, startDoc);

      //Ingore '/' and ';' which are added manually
      if ((content.trim().equals(";")) || (content.trim().equals("/")) || (content.trim().equals(""))) {
         return;
      }

      PlsqlExecutableObject obj = new PlsqlExecutableObject(startLine, content, "", PlsqlExecutableObjectType.UNKNOWN, startDoc, startDoc + content.length());
      obj.setDocLinesArray(docLines);
      obj.setOriginalFileName(originalFileName);
      executableObjs.add(obj);
   }

   /**
    * Method that will check and add unknown blocks
    * @param executableObjs
    * @param content
    * @param startDoc
    * @param endOff
    */
   private void checkAndAddBlocks(List<PlsqlExecutableObject> executableObjs, String content, int startDoc, int endOff) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      ts.move(startDoc);
      boolean moveNext = ts.moveNext();
      Token<PlsqlTokenId> token = ts.token();
      int offset = 0;
      boolean startOfBlock = true;

      //Get the define by the name
      while (moveNext) {
         String text = token.text().toString();
         if (ts.offset() >= endOff) {
            break;
         }

         if (startOfBlock && (token.id() == PlsqlTokenId.LINE_COMMENT || token.id() == PlsqlTokenId.BLOCK_COMMENT || token.id() == PlsqlTokenId.WHITESPACE)) {
            //skip comments at the beginning of a statement (and in between or after statement)
            offset = ts.offset() - startDoc + token.toString().length();
         } else if (token.id() == PlsqlTokenId.OPERATOR) {
            if (text.equals(";")) {
               int temp = ts.offset();
               addUnknownBlock(executableObjs, content.substring(offset, temp - startDoc), startDoc + offset);
               startOfBlock = true;
               offset = temp - startDoc + 1; //add 1 to avoid the ;

            } else if (text.equals("/")) {
               int temp = ts.offset();
               boolean isSeparator = true;

               Token<PlsqlTokenId> tokenPre = ts.token();
               while (ts.movePrevious()) {
                  tokenPre = ts.token();

                  if ((tokenPre.id() == PlsqlTokenId.WHITESPACE) &&
                          (tokenPre.text().toString().contains("\n"))) {
                     break;
                  } else {
                     isSeparator = false;
                     break;
                  }
               }

               ts.move(temp);
               ts.moveNext();
               if (!isSeparator) {
                  moveNext = ts.moveNext();
                  token = ts.token();
                  continue;
               }

               Token<PlsqlTokenId> tokenNext = ts.token();
               while (ts.moveNext()) {
                  tokenNext = ts.token();

                  if ((tokenNext.id() == PlsqlTokenId.WHITESPACE) &&
                          (tokenNext.text().toString().contains("\n"))) {
                     break;
                  } else {
                     isSeparator = false;
                     break;
                  }
               }

               ts.move(temp);
               ts.moveNext();
               if (!isSeparator) {
                  moveNext = ts.moveNext();
                  token = ts.token();
                  continue;
               }

               addUnknownBlock(executableObjs, content.substring(offset, temp - startDoc), startDoc + offset);
               startOfBlock = true;
               offset = temp - startDoc + 1; //add 1 to avoid the /

            } else {
               startOfBlock = false;
            }
         } else if (startOfBlock && token.id() == PlsqlTokenId.SQL_PLUS) {
            //find next line break (if end of stream the block will be included anyway so nothing to worry about
            String tokenText = FileExecutionUtil.readLine(ts, token);
            if (ts.moveNext()) { //Next token is line end - potentially plus whitespaces...
               int temp = ts.offset() + ts.token().text().length();
               addUnknownBlock(executableObjs, tokenText, startDoc + offset);
               startOfBlock = true;
               offset = temp - startDoc;
            }
         } else if (token.id() != PlsqlTokenId.WHITESPACE) {
               startOfBlock = false;
         }

         moveNext = ts.moveNext();
         token = ts.token();
      }

      //Add remainings as another block
      addUnknownBlock(executableObjs, content.substring(offset), startDoc + offset);
   }

   /**
    * Seperate individual comment blocks
    * @param startLine
    * @param content
    * @param name
    * @param blocks
    */
   private List<PlsqlExecutableObject> createCommentBlocks(int startLine, String content, String name, PlsqlExecutableObjectType type, List<PlsqlExecutableObject> blocks, PlsqlBlockFactory blockFac) {
      String blockContent;
      int start = 0;
      int nextPos = -1;
      PlsqlExecutableObject obj = null;
      int startPos = 0;

      for (; startPos < content.length(); startPos = nextPos + 1) {
         nextPos = content.indexOf(";", startPos);
         if (nextPos == -1) {
            int slash = content.indexOf("/", startPos);
            //check whether this index is the only one in the line
            while (slash != -1 && !isOnlyCharInLine(content, slash)) {
               slash = content.indexOf("/", slash + 1);
            }
            nextPos = slash;
         }
         if (nextPos != -1) {
            start = FileExecutionUtil.getLineNoForOffset(doc, startLine + startPos);
            blockContent = content.substring(startPos, nextPos);
            if (type == PlsqlExecutableObjectType.TABLECOMMENT) {
               name = getTableName(blockContent, blockFac);
            }
            obj = new PlsqlExecutableObject(start, blockContent, name, type, startLine + startPos, nextPos);
            obj.setDocLinesArray(docLines);
            obj.setOriginalFileName(originalFileName);
            blocks.add(obj);
         } else {
            break;
         }
      }

      //If there is only one block add that, or the last block
      if (startLine + startPos < startLine + content.length()) {
         start = FileExecutionUtil.getLineNoForOffset(doc, startLine + startPos);
         blockContent = content.substring(startPos);
         if (type == PlsqlExecutableObjectType.TABLECOMMENT)
               name = getTableName(blockContent, blockFac);
         obj = new PlsqlExecutableObject(start, blockContent, name, type,
                 startLine + startPos, startLine + startPos + blockContent.length());
         obj.setDocLinesArray(docLines);
         obj.setOriginalFileName(originalFileName);
         blocks.add(obj);
      }

      return blocks;
   }

   /**
    * Check whether the given index character is the only character in that line
    * @param content
    * @param slash
    * @return
    */
   private boolean isOnlyCharInLine(String content, int slash) {
      int startPos = slash + 1;
      boolean isEndOk = false;
      for (; startPos < content.length(); startPos++) {
         int ch = content.charAt(startPos);
         if (Character.isWhitespace(ch)) {
            if (ch == '\n') {
               isEndOk = true;
               break;
            }
         } else {
            break;
         }
      }

      startPos = slash - 1;
      boolean isStartOk = false;
      for (; startPos >= 0; startPos--) {
         int ch = content.charAt(startPos);
         if (Character.isWhitespace(ch)) {
            if (ch == '\n') {
               isStartOk = true;
               break;
            }
         } else {
            break;
         }
      }

      if (isStartOk && isEndOk)
         return true;
      
      return false;
   }

   private String getTableName(String content, PlsqlBlockFactory blockFac) {
      int start = content.toUpperCase(Locale.ENGLISH).indexOf("TABLE");
      if(start==-1)
         return "";
      //next word is the name of the table
      start=start+5;
      int end = -1;
      for(int i=start; i<content.length(); i++) {
         char ch = content.charAt(i);
         if(Character.isWhitespace(ch) && end!=-1) //whitespace after the next word...
            break;
         else if(!Character.isJavaIdentifierPart(ch) && ch!='&' && end!=-1)
            break;
         else
            end=i;
      }

      if(end == -1)
         return "";
      String name = content.substring(start, end+1).trim();
      
      return blockFac.getDefine(name);
   }

   /**
    * Method that will sort the given block hierarchy according to the offset
    * @param blockHier
    */
   private List<PlsqlBlock> sortBlockHier(List<PlsqlBlock> blockHier) {
      List<PlsqlBlock> sortedHier = new ArrayList<PlsqlBlock>();
      //Put all the objects into a hash map
      HashMap<Integer, PlsqlBlock> tmpList = new HashMap<Integer, PlsqlBlock>();

      for (int i = 0; i < blockHier.size(); i++) {
         PlsqlBlock block = blockHier.get(i);
         if (block != null) {
            tmpList.put(block.getStartOffset(), block);
         }
      }

      //Get the keyset and sort
      @SuppressWarnings("unchecked")
      HashMap<Integer, PlsqlBlock> clonedLst = (HashMap<Integer, PlsqlBlock>) tmpList.clone();
      Set<Integer> keys = clonedLst.keySet();
      List<Integer> sortedKeys = new ArrayList<Integer>();

      while (keys.size() > 0) {
         Integer obj = getMin(keys);
         sortedKeys.add(obj);
         keys.remove(obj);
      }

      //clear executable objects and add in the order
      for (int x = 0; x < sortedKeys.size(); x++) {
         int tmp = sortedKeys.get(x);
         PlsqlBlock block = tmpList.get(tmp);
         if (block != null) {
            sortedHier.add(block);
         }
      }

      return sortedHier;
   }

   /**
    * Method that will sort all the executable objects according to the offset
    * @param executableObjs 
    */
   private void sortExecutableObjects(List<PlsqlExecutableObject> executableObjs) {
      //Put all the objects into a hash map
      HashMap<Integer, PlsqlExecutableObject> tmpList = new HashMap<Integer, PlsqlExecutableObject>();

      for (int i = 0; i < executableObjs.size(); i++) {
         PlsqlExecutableObject exeObj = executableObjs.get(i);
         if (exeObj != null) {
            tmpList.put(exeObj.getStartOffset(), exeObj);
         }
      }

      //Get the keyset and sort
      @SuppressWarnings("unchecked")
      HashMap<Integer, PlsqlExecutableObject> clonedLst = (HashMap<Integer, PlsqlExecutableObject>) tmpList.clone();
      Set<Integer> keys = clonedLst.keySet();
      List<Integer> sortedKeys = new ArrayList<Integer>();

      while (keys.size() > 0) {
         Integer obj = getMin(keys);
         sortedKeys.add(obj);
         keys.remove(obj);
      }

      //clear executable objects and add in the order
      executableObjs.clear();
      for (int x = 0; x < sortedKeys.size(); x++) {
         int tmp = sortedKeys.get(x);
         PlsqlExecutableObject exeObj = tmpList.get(tmp);
         if (exeObj != null) {
            executableObjs.add(exeObj);
         }
      }
   }

   /**
    * Method that will return the minimum object of the given set
    * @param keys
    * @return
    */
   private Integer getMin(Set<Integer> keys) {
      int min = -1;

      for (Iterator it = keys.iterator(); it.hasNext();) {
         int tmp = (Integer) it.next();
         if ((min == -1) || (tmp < min)) {
            min = tmp;
         }
      }

      return min;
   }
   private String docContent;
   private List<String> docLines;
   private List<PlsqlExecutableObject> executableObjs;
   private DataObject dataObj;
   private Document doc;
   private String originalFileName;
}
