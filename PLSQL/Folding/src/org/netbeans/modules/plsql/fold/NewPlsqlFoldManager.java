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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldType;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.netbeans.spi.editor.fold.FoldHierarchyTransaction;
import org.netbeans.spi.editor.fold.FoldManager;
import org.netbeans.spi.editor.fold.FoldOperation;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author chrlse
 */
public class NewPlsqlFoldManager implements FoldManager, Runnable, Observer {

   private static final Logger LOG = Logger.getLogger(NewPlsqlFoldManager.class.getName());
   private FoldOperation operation;
   private Document doc;
   // XXX replace with Map to be able to test if mark already exist before adding new fold.
//   private Map<Integer, FoldMarkInfo> markArray = new HashMap<Integer, FoldMarkInfo>();
   private org.netbeans.editor.GapObjectArray markArray = new org.netbeans.editor.GapObjectArray();
   private int minUpdateMarkOffset = Integer.MAX_VALUE;
   private int maxUpdateMarkOffset = -1;
   private List<Fold> removedFoldList;
   private Map<String, Boolean> customFoldId = new HashMap<String, Boolean>();
   private static final RequestProcessor RP = new RequestProcessor(NewPlsqlFoldManager.class.getName(), 1, false, false);
   private final RequestProcessor.Task task = RP.create(this);
   private boolean initial = true;
   private PlsqlBlockFactory blockFactory;

   @Override
   public void init(FoldOperation operation) {
      this.operation = operation;
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "Initialized: {0}", System.identityHashCode(this));
      }
   }

   private FoldOperation getOperation() {
      return operation;
   }

   @Override
   public void initFolds(FoldHierarchyTransaction transaction) {
      doc = getOperation().getHierarchy().getComponent().getDocument();
      blockFactory = getBlockFactory();
      if (blockFactory != null) {
         blockFactory.addObserver(this);
      }
      task.schedule(300);
   }

   @Override
   public void insertUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      processRemovedFolds(transaction);
   }

   @Override
   public void removeUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      processRemovedFolds(transaction);
      removeAffectedMarks(evt, transaction);
   }

   @Override
   public void changedUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
   }

   @Override
   public void removeEmptyNotify(Fold emptyFold) {
      removeFoldNotify(emptyFold);
   }

   @Override
   public void removeDamagedNotify(Fold damagedFold) {
      removeFoldNotify(damagedFold);
   }

   @Override
   public void expandNotify(Fold expandedFold) {
   }

   @Override
   public void release() {
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "Released: {0}", System.identityHashCode(this));
      }
   }

   @Override
   public void update(Observable o, Object arg) {
      task.schedule(300);
   }

   @Override
   public void run() {
      if (operation.isReleased()) {
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Update skipped, already released: {0}", System.identityHashCode(this));
         }
         return;
      }
      ((BaseDocument) doc).readLock();
      try {

         if (blockFactory != null) {
            FoldHierarchy hierarchy = getOperation().getHierarchy();
            hierarchy.lock();
            try {
               if (operation.isReleased()) {
                  if (LOG.isLoggable(Level.FINE)) {
                     LOG.log(Level.FINE, "Update skipped, already released: {0}", System.identityHashCode(this));
                  }
                  return;
               }
               if (LOG.isLoggable(Level.FINE)) {
                  LOG.log(Level.FINE, "Updating: {0}", System.identityHashCode(this));
                  LOG.log(Level.FINE, "blockFactory.getBlockHierarchy().size(): {0}", blockFactory.getBlockHierarchy().size());
                  LOG.log(Level.FINE, "blockFactory.getNewBlocks().size(): {0}", blockFactory.getNewBlocks().size());
                  LOG.log(Level.FINE, "blockFactory.getRemovedBlocks().size(): {0}", blockFactory.getRemovedBlocks().size());
               }
               FoldHierarchyTransaction transaction = getOperation().openTransaction();
               try {
                  //Add new blocks to the hierarchy
                  List<PlsqlBlock> blocks = blockFactory.getNewBlocks();
                  if (initial) {
                     blocks = blockFactory.getBlockHierarchy();
                     initial = false;
                  }

                  updateFolds(blocks, transaction);
                  //Add custom fold blocks
                  updateFolds(blockFactory.getCustomFolds(), transaction);
               } finally {
                  transaction.commit();
               }
            } finally {
               hierarchy.unlock();
            }
         }
      } finally {
         ((BaseDocument) doc).readUnlock();
      }
   }

   private void removeFoldNotify(Fold removedFold) {
      if (removedFoldList == null) {
         removedFoldList = new ArrayList<Fold>(3);
      }
      removedFoldList.add(removedFold);
   }

   private void removeAffectedMarks(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      int removeOffset = evt.getOffset();
      int markIndex = findMarkIndex(removeOffset);
      if (markIndex < getMarkCount()) {
         FoldMarkInfo mark;
         while (markIndex >= 0 && (mark = getMark(markIndex)).getOffset() == removeOffset) {
            mark.release(false, transaction);
            removeMark(markIndex);
            markIndex--;
         }
      }
   }

   private void processRemovedFolds(FoldHierarchyTransaction transaction) {
      if (removedFoldList != null) {
         for (int i = removedFoldList.size() - 1; i >= 0; i--) {
            Fold removedFold = removedFoldList.get(i);
            FoldMarkInfo startMark = (FoldMarkInfo) getOperation().getExtraInfo(removedFold);
            if (startMark.getId() != null) {
               customFoldId.put(startMark.getId(), Boolean.valueOf(removedFold.isCollapsed())); // remember the last fold's state before remove
            }
            FoldMarkInfo endMark = startMark.getPairMark(); // get prior releasing
            if (getOperation().isStartDamaged(removedFold)) { // start mark area was damaged
               startMark.release(true, transaction); // forced remove
            }
            if (getOperation().isEndDamaged(removedFold)) {
               endMark.release(true, transaction);
            }
         }
      }
      removedFoldList = null;
   }

   private void markUpdate(FoldMarkInfo mark) {
      markUpdate(mark.getOffset());
   }

   private void markUpdate(int offset) {
      if (offset < minUpdateMarkOffset) {
         minUpdateMarkOffset = offset;
      }
      if (offset > maxUpdateMarkOffset) {
         maxUpdateMarkOffset = offset;
      }
   }

   private FoldMarkInfo getMark(int index) {
      return (FoldMarkInfo) markArray.getItem(index);
//      return markArray.get(index);
   }

   private int getMarkCount() {
      return markArray.getItemCount();
//      return markArray.size();
   }

   private void removeMark(int index) {
      if (LOG.isLoggable(Level.FINER)) {
         LOG.log(Level.FINER, "Removing mark from ind={0}: {1}", new Object[]{index, getMark(index)}); // NOI18N
      }
      markArray.remove(index, 1);
//      markArray.remove(index);
   }

   private void insertMark(int index, FoldMarkInfo mark) {
//      markArray.put(index, mark);
      markArray.insertItem(index, mark);
      if (LOG.isLoggable(Level.FINER)) {
         LOG.log(Level.FINER, "Inserted mark at ind={0}: {1}", new Object[]{index, mark}); // NOI18N
      }
   }

   private int findMarkIndex(int offset) {
      int markCount = getMarkCount();
      int low = 0;
      int high = markCount - 1;

      while (low <= high) {
         int mid = (low + high) / 2;
         int midMarkOffset = getMark(mid).getOffset();

         if (midMarkOffset < offset) {
            low = mid + 1;
         } else if (midMarkOffset > offset) {
            high = mid - 1;
         } else {
            // mark starting exactly at the given offset found
            // If multiple -> find the one with highest index
            mid++;
            while (mid < markCount && getMark(mid).getOffset() == offset) {
               mid++;
            }
            mid--;
            return mid;
         }
      }
      return low; // return higher index (e.g. for insert)
   }

   private List<FoldMarkInfo> getMarkList(List<PlsqlBlock> blocks) {
      List<FoldMarkInfo> markList = new ArrayList<FoldMarkInfo>();
      try {
         scanBlocks(markList, blocks);
      } catch (BadLocationException e) {
         LOG.log(Level.WARNING, null, e);
      }
      return markList;
   }

   private void processBlocks(List<PlsqlBlock> blocks, FoldHierarchyTransaction transaction) {
      List<FoldMarkInfo> markList = getMarkList(blocks);
      int markListSize;
      if (markList != null && ((markListSize = markList.size()) > 0)) {
         // Find the index for insertion
         int offset = (markList.get(0)).getOffset();
         int arrayMarkIndex = findMarkIndex(offset);
         // Remember the corresponding mark in the array as well
         FoldMarkInfo arrayMark;
         int arrayMarkOffset;
         if (arrayMarkIndex < getMarkCount()) {
            arrayMark = getMark(arrayMarkIndex);
            arrayMarkOffset = arrayMark.getOffset();
         } else { // at last mark
            arrayMark = null;
            arrayMarkOffset = Integer.MAX_VALUE;
         }

         for (int i = 0; i < markListSize; i++) {
            FoldMarkInfo listMark = markList.get(i);
            int listMarkOffset = listMark.getOffset();
            if (i == 0 || i == markListSize - 1) {
               // Update the update-offsets by the first and last marks in the list
               markUpdate(listMarkOffset);
            }
            while (listMarkOffset >= arrayMarkOffset) {
               if (listMarkOffset == arrayMarkOffset) {
                  // At the same offset - likely the same mark
                  //   -> retain the collapsed state
                  listMark.setCollapsed(arrayMark.isCollapsed());
               }
               if (!arrayMark.isReleased()) { // make sure that the mark is released
                  arrayMark.release(false, transaction);
               }
               removeMark(arrayMarkIndex);
               if (LOG.isLoggable(Level.FINER)) {
                  LOG.log(Level.FINER, "Removed dup mark from ind={0}: {1}", new Object[]{arrayMarkIndex, arrayMark}); // NOI18N
               }
               if (arrayMarkIndex < getMarkCount()) {
                  arrayMark = getMark(arrayMarkIndex);
                  arrayMarkOffset = arrayMark.getOffset();
               } else { // no more marks
                  arrayMark = null;
                  arrayMarkOffset = Integer.MAX_VALUE;
               }
            }
            // Insert the listmark
            insertMark(arrayMarkIndex, listMark);
            if (LOG.isLoggable(Level.FINER)) {
               LOG.log(Level.FINER, "Inserted mark at ind={0}: {1}", new Object[]{arrayMarkIndex, listMark}); // NOI18N
            }
            arrayMarkIndex++;
         }
      }
   }

   private void updateFolds(List<PlsqlBlock> blocks, FoldHierarchyTransaction transaction) {

      if (blocks != null && !blocks.isEmpty()) {
         processBlocks(blocks, transaction);
      }

      if (maxUpdateMarkOffset == -1) { // no updates
         return;
      }

      // Find the first mark to update and init the prevMark and parentMark prior the loop
      int index = findMarkIndex(minUpdateMarkOffset);
      FoldMarkInfo prevMark;
      FoldMarkInfo parentMark;
      if (index == 0) { // start from begining
         prevMark = null;
         parentMark = null;
      } else {
         prevMark = getMark(index - 1);
         parentMark = prevMark.getParentMark();
      }

      // Iterate through the changed marks in the mark array 
      int markCount = getMarkCount();
      while (index < markCount) { // process the marks
         FoldMarkInfo mark = getMark(index);

         // If the mark was released then it must be removed
         if (mark.isReleased()) {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.log(Level.FINE, "Removing released mark at ind={0}: {1}", new Object[]{index, mark}); // NOI18N
            }
            removeMark(index);
            markCount--;
            continue;
         }

         // Update mark's status (folds, parentMark etc.)
         if (mark.isStartMark()) { // starting a new fold
            if (prevMark == null || prevMark.isStartMark()) { // new level
               mark.setParentMark(prevMark); // prevMark == null means root level
               parentMark = prevMark;

            } // same level => parent to the parent of the prevMark

         } else { // end mark
            if (prevMark != null) {
               if (prevMark.isStartMark()) { // closing nearest fold
                  prevMark.setEndMark(mark, false, transaction);

               } else { // prevMark is end mark - closing its parent fold
                  if (parentMark != null) {
                     // mark's parent gets set as well
                     parentMark.setEndMark(mark, false, transaction);
                     parentMark = parentMark.getParentMark();

                  } else { // prevMark's parentMark is null (top level)
                     mark.makeSolitaire(false, transaction);
                  }
               }

            } else { // prevMark is null
               mark.makeSolitaire(false, transaction);
            }
         }

         // Set parent mark of the mark
         mark.setParentMark(parentMark);


         prevMark = mark;
         index++;
      }

      minUpdateMarkOffset = Integer.MAX_VALUE;
      maxUpdateMarkOffset = -1;

      if (LOG.isLoggable(Level.FINEST)) {
         LOG.log(Level.FINEST, "MARKS DUMP:\n{0}", this); //NOI18N
      }
   }

   public @Override
   String toString() {
      StringBuffer sb = new StringBuffer();
      int markCount = getMarkCount();
      int markCountDigitCount = Integer.toString(markCount).length();
      for (int i = 0; i < markCount; i++) {
         sb.append("["); // NOI18N
         String iStr = Integer.toString(i);
         appendSpaces(sb, markCountDigitCount - iStr.length());
         sb.append(iStr);
         sb.append("]:"); // NOI18N
         FoldMarkInfo mark = getMark(i);

         // Add extra indent regarding the depth in hierarchy
         int indent = 0;
         FoldMarkInfo parentMark = mark.getParentMark();
         while (parentMark != null) {
            indent += 4;
            parentMark = parentMark.getParentMark();
         }
         appendSpaces(sb, indent);

         sb.append(mark);
         sb.append('\n');
      }
      return sb.toString();
   }

   private static void appendSpaces(StringBuffer sb, int spaces) {
      while (--spaces >= 0) {
         sb.append(' ');
      }
   }

   private void scanBlocks(List<FoldMarkInfo> list, List<PlsqlBlock> blocks) throws BadLocationException {
      for (PlsqlBlock block : blocks) {

         FoldType foldType = null;
         final PlsqlBlockType type = block.getType();
         String description = null;

         if (!(type == PlsqlBlockType.COMMENT && doc.getText(block.getStartOffset(), block.getEndOffset() - block.getStartOffset()).indexOf("\n") == -1)) { // check for single line comments
            if (type == PlsqlBlockType.VIEW) {
               foldType = PlsqlFoldTypes.VIEW;
               description = block.getPrefix() + "VIEW " + block.getName();
            } else if (type == PlsqlBlockType.TABLE_COMMENT) {
               foldType = PlsqlFoldTypes.TABLECOMMENT;
               description = "COMMENT ON TABLE " + block.getName();
            } else if (type == PlsqlBlockType.COLUMN_COMMENT) {
               foldType = PlsqlFoldTypes.COLUMNCOMMENT;
               description = "COLUMN COMMENTS ON TABLE " + block.getName();
            } else if (type == PlsqlBlockType.COMMENT) {
               foldType = PlsqlFoldTypes.COMMENT;
               description = block.getName();
            } else if (type == PlsqlBlockType.PACKAGE) {
               foldType = PlsqlFoldTypes.PACKAGE;
               description = block.getPrefix() + "PACKAGE " + block.getName();
            } else if (type == PlsqlBlockType.PACKAGE_BODY) {
               foldType = PlsqlFoldTypes.PACKAGEBODY;
               description = block.getPrefix() + "PACKAGE BODY " + block.getName();
            } else if (type == PlsqlBlockType.PROCEDURE_IMPL) {
               foldType = PlsqlFoldTypes.PROCEDUREIMPL;
               description = block.getPrefix() + "PROCEDURE IMPLEMENTATION " + block.getName();
            } else if (type == PlsqlBlockType.FUNCTION_IMPL) {
               foldType = PlsqlFoldTypes.FUNCTIONIMPL;
               description = block.getPrefix() + "FUNCTION IMPLEMENTATION " + block.getName();
            } else if (type == PlsqlBlockType.PROCEDURE_DEF) {
               foldType = PlsqlFoldTypes.PROCEDUREDEF;
               description = "PROCEDURE DEFINITION " + block.getName();
            } else if (type == PlsqlBlockType.FUNCTION_DEF) {
               foldType = PlsqlFoldTypes.FUNCTIONDEF;
               description = "FUNCTION DEFINITION " + block.getName();
            } else if (type == PlsqlBlockType.DECLARE_END) {
               foldType = PlsqlFoldTypes.DECLAREEND;
               description = "DECLARE BLOCK";
            } else if (type == PlsqlBlockType.BEGIN_END) {
               foldType = PlsqlFoldTypes.BEGINEND;
               description = "BEGIN BLOCK";
            } else if (type == PlsqlBlockType.TRIGGER) {
               foldType = PlsqlFoldTypes.TRIGGER;
               description = block.getPrefix() + "TRIGGER " + block.getName();
            } else if (type == PlsqlBlockType.IF) {
               foldType = PlsqlFoldTypes.IF;
               description = block.getName();
            } else if (type == PlsqlBlockType.CASE) {
               foldType = PlsqlFoldTypes.CASE;
               description = block.getName();
            } else if (type == PlsqlBlockType.WHILE_LOOP) {
               foldType = PlsqlFoldTypes.WHILELOOP;
               description = "WHILE " + block.getName();
            } else if (type == PlsqlBlockType.FOR_LOOP) {
               foldType = PlsqlFoldTypes.FORLOOP;
               description = "FOR " + block.getName();
            } else if (type == PlsqlBlockType.LOOP) {
               foldType = PlsqlFoldTypes.LOOP;
               description = "LOOP ";
            } else if (type == PlsqlBlockType.CUSTOM_FOLD) {
               foldType = PlsqlFoldTypes.CUSTOM;
               description = block.getName();
            } else if (type == PlsqlBlockType.STATEMENT) {
               foldType = PlsqlFoldTypes.STATEMENT;
               description = block.getPrefix() + block.getName();
            } else if (type == PlsqlBlockType.CURSOR) {
               foldType = PlsqlFoldTypes.CURSOR;
               description = "CURSOR " + block.getName();
            } else if (type == PlsqlBlockType.JAVA_SOURCE) {
               foldType = PlsqlFoldTypes.JAVASOURCE;
               description = block.getPrefix() + "JAVA SOURCE";
            }
            //XXX: find out what offsets are needed (check CustomFoldManager when .php is run):
            final FoldMarkInfo foldMarkInfo = new FoldMarkInfo(foldType, true, block.getStartOffset(), 0, block.getName(), isCollapsed(foldType), description);
            // XXX: check if mark exist
            list.add(foldMarkInfo);
            list.add(new FoldMarkInfo(foldType, false, block.getEndOffset(), 0, null, false, null));
            scanBlocks(list, block.getChildBlocks());
         }
      }
   }

   private final class FoldMarkInfo {

      private boolean startMark;
      private Position pos;
      private int length;
      private String id;
      private boolean collapsed;
      private String description;
      /**
       * Matching pair mark used for fold construction
       */
      private FoldMarkInfo pairMark;
      /**
       * Parent mark defining nesting in the mark hierarchy.
       */
      private FoldMarkInfo parentMark;
      /**
       * Fold that corresponds to this mark (if it's start mark). It can be null if this mark is end mark or if it
       * currently does not have the fold assigned.
       */
      private Fold fold;
      private boolean released;
      private final FoldType foldType;

      private FoldMarkInfo(FoldType foldType, boolean startMark, int offset,
              int length, String id, boolean collapsed, String description)
              throws BadLocationException {
         this.foldType = foldType;
         this.startMark = startMark;
         this.pos = doc.createPosition(offset);
         this.length = length;
         this.id = id;
         this.collapsed = collapsed;
         this.description = description;
      }

      public String getId() {
         return id;
      }

      public String getDescription() {
         return description;
      }

      public boolean isStartMark() {
         return startMark;
      }

      public int getLength() {
         return length;
      }

      public int getOffset() {
         return pos.getOffset();
      }

      public int getEndOffset() {
         return getOffset() + getLength();
      }

      public boolean isCollapsed() {
         return (fold != null) ? fold.isCollapsed() : collapsed;
      }

      public boolean hasFold() {
         return (fold != null);
      }

      public void setCollapsed(boolean collapsed) {
         this.collapsed = collapsed;
      }

      public boolean isSolitaire() {
         return (pairMark == null);
      }

      public void makeSolitaire(boolean forced, FoldHierarchyTransaction transaction) {
         if (!isSolitaire()) {
            if (isStartMark()) {
               setEndMark(null, forced, transaction);
            } else { // end mark
               getPairMark().setEndMark(null, forced, transaction);
            }
         }
      }

      public boolean isReleased() {
         return released;
      }

      /**
       * Release this mark and mark for update.
       */
      public void release(boolean forced, FoldHierarchyTransaction transaction) {
         if (!released) {
            makeSolitaire(forced, transaction);
            released = true;
            markUpdate(this);
         }
      }

      public FoldMarkInfo getPairMark() {
         return pairMark;
      }

      private void setPairMark(FoldMarkInfo pairMark) {
         this.pairMark = pairMark;
      }

      public void setEndMark(FoldMarkInfo endMark, boolean forced,
              FoldHierarchyTransaction transaction) {
         if (!isStartMark()) {
            throw new IllegalStateException("Not start mark"); // NOI18N
         }
         if (pairMark == endMark) {
            return;
         }

         if (pairMark != null) { // is currently paired to an end mark
            releaseFold(forced, transaction);
            pairMark.setPairMark(null);
         }

         pairMark = endMark;
         if (endMark != null) {
            if (!endMark.isSolitaire()) { // make solitaire first
               endMark.makeSolitaire(false, transaction); // not forced here
            }
            endMark.setPairMark(this);
            endMark.setParentMark(this.getParentMark());
            ensureFoldExists(transaction);
         }
      }

      public FoldMarkInfo getParentMark() {
         return parentMark;
      }

      public void setParentMark(FoldMarkInfo parentMark) {
         this.parentMark = parentMark;
      }

      private void releaseFold(boolean forced, FoldHierarchyTransaction transaction) {
         if (isSolitaire() || !isStartMark()) {
            throw new IllegalStateException();
         }

         if (fold != null) {
            setCollapsed(fold.isCollapsed()); // serialize the collapsed info
            if (!forced) {
               getOperation().removeFromHierarchy(fold, transaction);
            }
            fold = null;
         }
      }

      public Fold getFold() {
         if (isSolitaire()) {
            return null;
         }
         if (!isStartMark()) {
            return pairMark.getFold();
         }
         return fold;
      }

      public void ensureFoldExists(FoldHierarchyTransaction transaction) {
         if (isSolitaire() || !isStartMark()) {
            throw new IllegalStateException();
         }

         if (fold == null) {
            try {
               if (!startMark) {
                  throw new IllegalStateException("Not start mark: " + this); // NOI18N
               }
               if (pairMark == null) {
                  throw new IllegalStateException("No pairMark for mark:" + this); // NOI18N
               }
               int startOffset = getOffset();
               int startGuardedLength = getLength();
               int endGuardedLength = pairMark.getLength();
               int endOffset = pairMark.getOffset() + endGuardedLength;
               // XXX: check if Fold exist before adding
               fold = getOperation().addToHierarchy(
                       foldType, getDescription(), collapsed,
                       startOffset, endOffset,
                       startGuardedLength, endGuardedLength,
                       this,
                       transaction);
            } catch (BadLocationException e) {
               LOG.log(Level.WARNING, null, e);
            }
         }
      }

      @Override
      public String toString() {
         StringBuilder sb = new StringBuilder();
         sb.append(isStartMark() ? 'S' : 'E');  // NOI18N

         // Check whether this mark (or its pair) has fold
         if (hasFold() || (!isSolitaire() && getPairMark().hasFold())) {
            sb.append("F"); // NOI18N

            // Check fold's status
            if (isStartMark() && (isSolitaire()
                    || getOffset() != fold.getStartOffset()
                    || getPairMark().getEndOffset() != fold.getEndOffset())) {
               sb.append("!!<"); // NOI18N
               sb.append(fold.getStartOffset());
               sb.append(","); // NOI18N
               sb.append(fold.getEndOffset());
               sb.append(">!!"); // NOI18N
            }
         }

         // Append mark's internal status
         sb.append(" ("); // NOI18N
         sb.append("o="); // NOI18N
         sb.append(pos.getOffset());
         sb.append(", l="); // NOI18N
         sb.append(length);
         sb.append(", d='"); // NOI18N
         sb.append(description);
         sb.append('\'');
         if (getPairMark() != null) {
            sb.append(", <->"); // NOI18N
            sb.append(getPairMark().getOffset());
         }
         if (getParentMark() != null) {
            sb.append(", ^"); // NOI18N
            sb.append(getParentMark().getOffset());
         }
         sb.append(')');

         return sb.toString();
      }
   }

   /**
    * Method that will select and return the corresponding fold to parent from oldRoot fold hierarchy
    *
    * @param parent
    * @param foldInfoLst
    * @return
    */
   private boolean isCollapsed(final FoldType foldType) {
      if (OptionsUtilities.isPlSqlExpandFolds()) {
         return false;
      }
      return foldType == PlsqlFoldTypes.COMMENT;
   }

   /**
    * Method that will return the relevant block factory
    *
    * @return
    */
   private PlsqlBlockFactory getBlockFactory() {
      final Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof Lookup.Provider) {
         return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
      }
      return null;
   }
}
