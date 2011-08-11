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
package org.netbeans.modules.plsql.fold;

import org.netbeans.modules.plsqlsupport.options.IfsOptionsUtilities;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldType;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.editor.fold.FoldHierarchyTransaction;
import org.netbeans.spi.editor.fold.FoldManager;
import org.netbeans.spi.editor.fold.FoldOperation;
import org.openide.ErrorManager;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 *
 * @author yadhlk
 */
public class PlsqlFoldManager implements FoldManager, Observer {

   private FoldOperation operation;
   private static Observer observer = null;
   private boolean initial = true;

   protected FoldOperation getOperation() {
      return operation;
   }

   public void init(final FoldOperation operation) {
      this.operation = operation;
   }

   public void initFolds(final FoldHierarchyTransaction foldHierarchyTransaction) {
      final Document doc = getDocument();
      if (!(doc instanceof BaseDocument)) {
         return;
      }

      final PlsqlBlockFactory blockFactory = getBlockFactory();
      if (blockFactory != null) {
         try {

            if (observer != null) {
               blockFactory.deleteObserver(observer);
            }

            blockFactory.addObserver(this);
            observer = this;

            //Initialize document hierarchy
            blockFactory.initHierarchy(doc);
            //Add new blocks to the hierarchy
            List<PlsqlBlock> newBlocks;
            if (initial) {
               newBlocks = blockFactory.getBlockHierarchy();
               initial = false;
            } else {
               newBlocks = blockFactory.getNewBlocks();
            }
            addFolds(newBlocks, foldHierarchyTransaction, null);
            //Add custom fold blocks
            addFolds(blockFactory.getCustomFolds(), foldHierarchyTransaction, null);
         } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
         }
      }
   }

   /**
    * Add folds to the folder hieararchy (initialy)
    * @param blockHier
    * @param fhTransaction
    */
   private void addFolds(final List<PlsqlBlock> blockHier, final FoldHierarchyTransaction fhTransaction, final List<FoldInfo> collapsedFolds) throws BadLocationException {
      final int count = blockHier.size();

      for (int i = 0; i < count; i++) {
         final PlsqlBlock temp = blockHier.get(i);
         FoldType foldType = null;
         final PlsqlBlockType type = temp.getType();
         String description = "";

         if (type == PlsqlBlockType.VIEW) {
            foldType = PlsqlFoldTypes.VIEW;
            description = temp.getPrefix() + "VIEW " + temp.getName();
         } else if (type == PlsqlBlockType.TABLE_COMMENT) {
            foldType = PlsqlFoldTypes.TABLECOMMENT;
            description = "COMMENT ON TABLE " + temp.getName();
         } else if (type == PlsqlBlockType.COLUMN_COMMENT) {
            foldType = PlsqlFoldTypes.COLUMNCOMMENT;
            description = "COLUMN COMMENTS ON TABLE " + temp.getName();
         } else if (type == PlsqlBlockType.COMMENT) {
            foldType = PlsqlFoldTypes.COMMENT;
            description = temp.getName();
         } else if (type == PlsqlBlockType.PACKAGE) {
            foldType = PlsqlFoldTypes.PACKAGE;
            description = temp.getPrefix() + "PACKAGE " + temp.getName();
         } else if (type == PlsqlBlockType.PACKAGE_BODY) {
            foldType = PlsqlFoldTypes.PACKAGEBODY;
            description = temp.getPrefix() + "PACKAGE BODY " + temp.getName();
         } else if (type == PlsqlBlockType.PROCEDURE_IMPL) {
            foldType = PlsqlFoldTypes.PROCEDUREIMPL;
            description = temp.getPrefix() + "PROCEDURE IMPLEMENTATION " + temp.getName();
         } else if (type == PlsqlBlockType.FUNCTION_IMPL) {
            foldType = PlsqlFoldTypes.FUNCTIONIMPL;
            description = temp.getPrefix() + "FUNCTION IMPLEMENTATION " + temp.getName();
         } else if (type == PlsqlBlockType.PROCEDURE_DEF) {
            foldType = PlsqlFoldTypes.PROCEDUREDEF;
            description = "PROCEDURE DEFINITION " + temp.getName();
         } else if (type == PlsqlBlockType.FUNCTION_DEF) {
            foldType = PlsqlFoldTypes.FUNCTIONDEF;
            description = "FUNCTION DEFINITION " + temp.getName();
         } else if (type == PlsqlBlockType.DECLARE_END) {
            foldType = PlsqlFoldTypes.DECLAREEND;
            description = "DECLARE BLOCK";
         } else if (type == PlsqlBlockType.BEGIN_END) {
            foldType = PlsqlFoldTypes.BEGINEND;
            description = "BEGIN BLOCK";
         } else if (type == PlsqlBlockType.TRIGGER) {
            foldType = PlsqlFoldTypes.TRIGGER;
            description = temp.getPrefix() + "TRIGGER " + temp.getName();
         } else if (type == PlsqlBlockType.IF) {
            foldType = PlsqlFoldTypes.IF;
            description = temp.getName();
         } else if (type == PlsqlBlockType.CASE) {
            foldType = PlsqlFoldTypes.CASE;
            description = temp.getName();
         } else if (type == PlsqlBlockType.WHILE_LOOP) {
            foldType = PlsqlFoldTypes.WHILELOOP;
            description = "WHILE " + temp.getName();
         } else if (type == PlsqlBlockType.FOR_LOOP) {
            foldType = PlsqlFoldTypes.FORLOOP;
            description = "FOR " + temp.getName();
         } else if (type == PlsqlBlockType.LOOP) {
            foldType = PlsqlFoldTypes.LOOP;
            description = "LOOP ";
         } else if (type == PlsqlBlockType.CUSTOM_FOLD) {
            foldType = PlsqlFoldTypes.CUSTOM;
            description = temp.getName();
         } else if (type == PlsqlBlockType.STATEMENT) {
            foldType = PlsqlFoldTypes.STATEMENT;
            description = temp.getPrefix() + temp.getName();
         } else if (type == PlsqlBlockType.CURSOR) {
            foldType = PlsqlFoldTypes.CURSOR;
            description = "CURSOR " + temp.getName();
         } else if (type == PlsqlBlockType.JAVA_SOURCE) {
            foldType = PlsqlFoldTypes.JAVASOURCE;
            description = temp.getPrefix() + "JAVA SOURCE";
         }

         if (getDocument().getEndPosition().getOffset() >= temp.getEndOffset()) {
            operation.addToHierarchy(foldType, description, isCollapsed(temp, foldType, collapsedFolds),
                  temp.getStartOffset(), temp.getEndOffset(), 0, 0, null, fhTransaction);

            //check for any child folds and add them also
            addFolds(temp.getChildBlocks(), fhTransaction, collapsedFolds);
         }
      }
   }

   private void getBlockMap(final Map<String, PlsqlBlock> blockMap, final List<PlsqlBlock> blockHierarchy) {
      for (int i = 0; i < blockHierarchy.size(); i++) {
         final PlsqlBlock temp = blockHierarchy.get(i);
         blockMap.put(temp.getStartOffset() + "_" + temp.getEndOffset(), temp);

         getBlockMap(blockMap, temp.getChildBlocks());
      }
   }

   /**
    * Method that will add collapsed folds (FoldInfo objects)
    * in the fold hierarchy to the given list
    * @param parent
    * @param foldInfoLst
    */
   private void getCollapsedFolds(final Fold parent, final List<FoldInfo> foldInfoLst) {
      if (parent.isCollapsed()) {
         final FoldInfo tempInfo = new FoldInfo(parent.getStartOffset(), parent.getEndOffset(), parent.getType());
         foldInfoLst.add(tempInfo);
      }
      final int count = parent.getFoldCount();
      for (int i = 0; i < count; i++) {
         final Fold temp = parent.getFold(i);
         getCollapsedFolds(temp, foldInfoLst);
      }
   }

   /**
    * Method that will select and return the corresponding fold to parent
    * from oldRoot fold hierarchy
    * @param parent
    * @param foldInfoLst
    * @return
    */
   private boolean isCollapsed(final PlsqlBlock block, final FoldType foldType, final List<FoldInfo> foldInfoLst) {
      if (foldInfoLst == null) {
         if (IfsOptionsUtilities.isPlSqlExpandFolds()) {
            return false;
         } else {
            return foldType == PlsqlFoldTypes.COMMENT;
         }
      }
      final int size = foldInfoLst.size();
      for (int i = 0; i < size; i++) {
         final FoldInfo temp = foldInfoLst.get(i);

         if ((temp.foldType == foldType)
               && (temp.startOffset == block.getPreviousStart())
               && (temp.endOffset == block.getPreviousEnd())) {
            return true;
         }
      }

      return false;
   }

   /**
    * Method that will update the folds based on the changes to the document
    * @param fhTran
    */
   private synchronized void updateFolds(final FoldHierarchyTransaction fhTran) {
      try {
         final PlsqlBlockFactory blockFactory = getBlockFactory();
         if (blockFactory == null) {
            return;
         }

         final FoldHierarchy fh = getOperation().getHierarchy();
         try {
            //lock the hierarchy
            fh.lock();

            //Get folds from Block maker
            final List<PlsqlBlock> oldBlocks = blockFactory.getRemovedBlocks();

            final Fold root = fh.getRootFold();
            final List<FoldInfo> collapsedFolds = new ArrayList<FoldInfo>();
            getCollapsedFolds(root, collapsedFolds);

            //Remove non existing blocks
            final int childCount = root.getFoldCount();
            if (!oldBlocks.isEmpty()) {
               for (int i = (childCount - 1); i >= 0; i--) {
                  final Fold child = root.getFold(i);
                  removeWithChildren(child, oldBlocks, fhTran);
               }
            }

            //Add new blocks to the hierarchy
            final List<PlsqlBlock> newBlocks = blockFactory.getNewBlocks();
            addFolds(newBlocks, fhTran, collapsedFolds);
            //Add custom folds
            addFolds(blockFactory.getCustomFolds(), fhTran, collapsedFolds);

            if (blockFactory.isAliasesChanged()) {
               final Map<String, PlsqlBlock> blockMap = new HashMap<String, PlsqlBlock>();
               getBlockMap(blockMap, blockFactory.getBlockHierarchy());
               final int rootChildren = root.getFoldCount();
               for (int i = (rootChildren - 1); i >= 0; i--) {
                  final Fold child = root.getFold(i);
                  changeDescriptions(child, blockMap, fhTran);
               }
            }

            //compareFoldHierarchies(root, collapsedFolds, fhTran);
         } finally {
            fh.unlock();
         }
      } catch (Exception e) {
         ErrorManager.getDefault().notify(e);
      }
   }

   /**
    * Method that will change the fold descriptions if changed
    * @param parent
    * @param blockMap
    * @param fhTran
    */
   private void changeDescriptions(final Fold parent, final Map<String, PlsqlBlock> blockMap, final FoldHierarchyTransaction fhTran) throws BadLocationException {
      final PlsqlBlock block = getCorrespondingBlock(parent, blockMap);

      //check whether block name is an alias
      if ((block != null) && (!block.getAlias().equals(""))) {  //Aliases wont be there for COMMENT & DECLARE
         final String description = getFoldDescription(block);
         //check whether description is different
         if (!parent.getDescription().equals(description)) {
            operation.removeFromHierarchy(parent, fhTran);
            operation.addToHierarchy(parent.getType(), description, parent.isCollapsed(),
                  block.getStartOffset(), block.getEndOffset(), 0, 0, null, fhTran);
         }
      }

      final int childCount = parent.getFoldCount();
      for (int i = (childCount - 1); i >= 0; i--) {
         final Fold child = parent.getFold(i);
         //Ignore types that cannot have the name as an alias
         if ((child.getType() != PlsqlFoldTypes.BEGINEND)
               && (child.getType() != PlsqlFoldTypes.COMMENT)
               && (child.getType() != PlsqlFoldTypes.DECLAREEND)
               && (child.getType() != PlsqlFoldTypes.IF)
               && (child.getType() != PlsqlFoldTypes.CUSTOM)
               && (child.getType() != PlsqlFoldTypes.FORLOOP)
               && (child.getType() != PlsqlFoldTypes.LOOP)
               && (child.getType() != PlsqlFoldTypes.WHILELOOP)) {

            changeDescriptions(child, blockMap, fhTran);
         }
      }
   }

   /**
    * Get the matching block to the fold. Consider the type, start &  end offset
    * @param fold
    * @param blockMap
    * @return
    */
   private PlsqlBlock getCorrespondingBlock(final Fold fold, final Map<String, PlsqlBlock> blockMap) {

      final PlsqlBlock temp = blockMap.get(fold.getStartOffset() + "_" + fold.getEndOffset());
      if (temp != null && isCorrespondingType(temp.getType(), fold.getType())) {
         return temp;
      }

      return null;
   }

   /**
    * Method that will check whether the block type and the fold type are matching
    * @param blockType
    * @param foldType
    * @return
    */
   private boolean isCorrespondingType(final PlsqlBlockType blockType, final FoldType foldType) {
      switch (blockType) {
         case VIEW:
            return foldType == PlsqlFoldTypes.VIEW;
         case TABLE_COMMENT:
            return foldType == PlsqlFoldTypes.TABLECOMMENT;
         case COLUMN_COMMENT:
            return foldType == PlsqlFoldTypes.COLUMNCOMMENT;
         case COMMENT:
            return foldType == PlsqlFoldTypes.COMMENT;
         case PACKAGE:
            return foldType == PlsqlFoldTypes.PACKAGE;
         case PACKAGE_BODY:
            return foldType == PlsqlFoldTypes.PACKAGEBODY;
         case PROCEDURE_IMPL:
            return foldType == PlsqlFoldTypes.PROCEDUREIMPL;
         case FUNCTION_IMPL:
            return foldType == PlsqlFoldTypes.FUNCTIONIMPL;
         case PROCEDURE_DEF:
            return foldType == PlsqlFoldTypes.PROCEDUREDEF;
         case FUNCTION_DEF:
            return foldType == PlsqlFoldTypes.FUNCTIONDEF;
         case DECLARE_END:
            return foldType == PlsqlFoldTypes.DECLAREEND;
         case BEGIN_END:
            return foldType == PlsqlFoldTypes.BEGINEND;
         case TRIGGER:
            return foldType == PlsqlFoldTypes.TRIGGER;
         case IF:
            return foldType == PlsqlFoldTypes.IF;
         case CASE:
            return foldType == PlsqlFoldTypes.CASE;
         case WHILE_LOOP:
            return foldType == PlsqlFoldTypes.WHILELOOP;
         case FOR_LOOP:
            return foldType == PlsqlFoldTypes.FORLOOP;
         case LOOP:
            return foldType == PlsqlFoldTypes.LOOP;
         case CUSTOM_FOLD:
            return foldType == PlsqlFoldTypes.CUSTOM;
         case STATEMENT:
            return foldType == PlsqlFoldTypes.STATEMENT;
         case CURSOR:
            return foldType == PlsqlFoldTypes.CURSOR;
         case JAVA_SOURCE:
            return foldType == PlsqlFoldTypes.JAVASOURCE;
         default:
            return false;
      }
   }

   /**
    * Method that will return the fold description given the Plsql block
    * Used when changing the descriptions only.
    * @param block
    * @return
    */
   private String getFoldDescription(final PlsqlBlock block) {
      switch (block.getType()) {
         case VIEW:
            return block.getPrefix() + "VIEW " + block.getName();
         case TABLE_COMMENT:
            return "COMMENT ON TABLE " + block.getName();
         case COLUMN_COMMENT:
            return "COLUMN COMMENTS ON TABLE " + block.getName();
         case COMMENT:
            return block.getName();
         case PACKAGE:
            return block.getPrefix() + "PACKAGE " + block.getName();
         case PACKAGE_BODY:
            return block.getPrefix() + "PACKAGE BODY " + block.getName();
         case PROCEDURE_IMPL:
            return block.getPrefix() + "PROCEDURE IMPLEMENTATION " + block.getName();
         case FUNCTION_IMPL:
            return block.getPrefix() + "FUNCTION IMPLEMENTATION " + block.getName();
         case PROCEDURE_DEF:
            return "PROCEDURE DEFINITION " + block.getName();
         case FUNCTION_DEF:
            return "FUNCTION DEFINITION " + block.getName();
         case DECLARE_END:
            return "DECLARE BLOCK";
         case BEGIN_END:
            return "BEGIN BLOCK";
         case TRIGGER:
            return block.getPrefix() + "TRIGGER " + block.getName();
         case IF:
            return block.getName();
         case CASE:
            return block.getName();
         case WHILE_LOOP:
            return "WHILE " + block.getName();
         case FOR_LOOP:
            return "FOR " + block.getName();
         case LOOP:
            return "LOOP ";
         case CUSTOM_FOLD:
            return block.getName();
         case STATEMENT:
            return block.getPrefix() + block.getName();
         case CURSOR:
            return "CURSOR " + block.getName();
         case JAVA_SOURCE:
            return block.getPrefix() + "JAVA SOURCE";
         default:
            return "";
      }
   }

   //FoldManager Abstract Method
   @Override
   public void insertUpdate(final DocumentEvent documentEvent, final FoldHierarchyTransaction foldHierarchyTransaction) {
      //We are not using this event
   }

   //FoldManager Abstract Method
   @Override
   public void removeUpdate(final DocumentEvent documentEvent, final FoldHierarchyTransaction foldHierarchyTransaction) {
      //We are not using this event
   }

   //FoldManager Abstract Method
   @Override
   public void changedUpdate(final DocumentEvent documentEvent, final FoldHierarchyTransaction foldHierarchyTransaction) {
      //We are not using this event
   }

   //FoldManager Abstract Method
   @Override
   public void removeEmptyNotify(final Fold fold) {
      //do nothing
   }

   //FoldManager Abstract Method
   @Override
   public void removeDamagedNotify(final Fold fold) {
      //do nothing
   }

   //FoldManager Abstract Method
   @Override
   public void expandNotify(final Fold fold) {
      //do nothing
   }

   //FoldManager Abstract Method
   @Override
   public void release() {
      //do nothing
   }

   private Document getDocument() {
      Object obj = null;
      for (int i = 0; i < 10; i++) {
         obj = getOperation().getHierarchy().getComponent().getDocument();
         if (obj instanceof AbstractDocument) {
            return (Document) obj;
         }
         try {
            Thread.currentThread().sleep(i);
         } catch (InterruptedException e) {
         }
      }
      throw new IllegalStateException("[PLSQLFolding] PLSQLFoldManager.getDocument() NOT returned AbstractDocument, but " + obj.getClass() + "!. This is caused by not yet resolved issue #49497."); //NOI18N
   }

   /**
    * Remove fold with its children
    * @param fold
    * @param blockHier
    * @param fhTran
    * @return true if removed all the children
    */
   private void removeWithChildren(final Fold fold, final List<PlsqlBlock> blockHier, final FoldHierarchyTransaction fhTran) {

      final int childCount = fold.getFoldCount();
      for (int i = (childCount - 1); i >= 0; i--) {
         final Fold child = fold.getFold(i);
         removeWithChildren(child, blockHier, fhTran);
      }

      //If a custom fold remove
      if (fold.getType() == PlsqlFoldTypes.CUSTOM || checkExists(fold, blockHier)) {
         operation.removeFromHierarchy(fold, fhTran);
      }
   }

   /**
    * Method that will check whether given fold exists in block hier
    * @param fold
    * @param blockHier
    * @return
    */
   private boolean checkExists(final Fold fold, final List<PlsqlBlock> blockHier) {
      final Comparator<PlsqlBlock> comparator = new Comparator<PlsqlBlock>() {

         @Override
         public int compare(final PlsqlBlock o1, final PlsqlBlock o2) {
            Integer o1pos, o2pos;
            if (o1.getStartOffset() > -1 && o2.getStartOffset() > -1) {
               o1pos = Integer.valueOf(o1.getStartOffset());
               o2pos = Integer.valueOf(o2.getStartOffset());
            } else {
               o1pos = Integer.valueOf(o1.getEndOffset());
               o2pos = Integer.valueOf(o2.getEndOffset());
            }
            return o1pos.compareTo(o2pos);
         }
      };
      return checkExists(fold, blockHier, comparator);
   }

   /**
    * Method that will check whether given fold exists in block hier
    * @param fold
    * @param blockHier
    * @param comparator
    * @return
    */
   private boolean checkExists(final Fold fold, final List<PlsqlBlock> blockHier, final Comparator<PlsqlBlock> comparator) {
      final int size = blockHier.size();
      Collections.sort(blockHier, comparator);
      if (size == 0) {
         return false;
      }
      //make sure that the fold isn't before the first block or after the last block in the hierarchy.
      if (fold.getStartOffset() > blockHier.get(size - 1).getEndOffset()
            || fold.getEndOffset() < blockHier.get(0).getStartOffset()) {
         return false;
      }
      for (int i = 0; i < size; i++) {
         final PlsqlBlock tmp = blockHier.get(i);
         if (tmp.getStartOffset() <= fold.getStartOffset() && tmp.getEndOffset() >= fold.getEndOffset()) {
            return true;
         }
         if (tmp.getPreviousStart() <= fold.getStartOffset() && tmp.getPreviousEnd() >= fold.getEndOffset()) {
            return true;
         }
         if ((tmp.getEndOffset() == fold.getEndOffset() || fold.getEndOffset() == tmp.getPreviousEnd()
               || tmp.getStartOffset() == fold.getStartOffset() || tmp.getPreviousStart() == fold.getStartOffset())
               && (isCorrespondingType(tmp.getType(), fold.getType()))
               && (getFoldDescription(tmp).equals(fold.getDescription()))) {
            return true;
         }

         if (checkExists(fold, tmp.getChildBlocks(), comparator)) {
            return true;
         }
      }

      return false;
   }

   /**
    * Method that will return the relevant block factory
    * @return
    */
   private PlsqlBlockFactory getBlockFactory() {
      final Document doc = getDocument();
      final Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof Lookup.Provider) {
         return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
      }
      return null;
   }

   /**
    * Document event has occured
    * @param o
    * @param arg
    */
   @Override
   public void update(final Observable o, final Object arg) {
      final FoldHierarchyTransaction fhTran = getOperation().openTransaction();
      updateFolds(fhTran);
      fhTran.commit();
   }

   /**
    * Private class that holds some fold info
    * This is used to collapse folds after a change
    */
   private class FoldInfo {

      public FoldType foldType;
      public int startOffset;
      public int endOffset;

      private FoldInfo(final int start, final int end, final FoldType type) {
         this.startOffset = start;
         this.endOffset = end;
         this.foldType = type;
      }
   }
}
