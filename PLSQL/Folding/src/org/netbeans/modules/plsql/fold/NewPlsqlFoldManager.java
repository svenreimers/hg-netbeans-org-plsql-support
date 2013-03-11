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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
//   public static final FoldType CUSTOM_FOLD_TYPE = new FoldType("custom-fold"); // NOI18N
   private FoldOperation operation;
   private Document doc;
   private org.netbeans.editor.GapObjectArray markArray = new org.netbeans.editor.GapObjectArray();
   private int minUpdateMarkOffset = Integer.MAX_VALUE;
   private int maxUpdateMarkOffset = -1;
   private List<Fold> removedFoldList;
   private Map<String, Boolean> customFoldId = new HashMap<String, Boolean>();
   private static final RequestProcessor RP = new RequestProcessor(NewPlsqlFoldManager.class.getName(),
           1, false, false);
   private final RequestProcessor.Task task = RP.create(this);
//   private NewPlsqlFoldManager observer;
   private boolean initial = true;

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
      final PlsqlBlockFactory blockFactory = getBlockFactory();
      if (blockFactory != null) {
//         if (observer != null) {
//            blockFactory.deleteObserver(observer);
//         }
         blockFactory.addObserver(this);
//         observer = this;
      }
      task.schedule(300);
   }

   //XXX: seems NewPlsqlFoldManager needs to listen to PlsqlBlockFactory changes, instead of directly to document through FoldManager interface 
   @Override
   public void insertUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      processRemovedFolds(transaction);
//      myRemoveFolds(transaction);
//      task.schedule(300);
   }

   @Override
   public void removeUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      processRemovedFolds(transaction);
//      myRemoveFolds(transaction);
      removeAffectedMarks(evt, transaction);
//      task.schedule(300);
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

         final PlsqlBlockFactory blockFactory = getBlockFactory();
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
               }
               FoldHierarchyTransaction transaction = getOperation().openTransaction();
               try {
                  //Initialize document hierarchy
                  blockFactory.initHierarchy(doc);
                  //Add new blocks to the hierarchy
                  List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
//                  List<PlsqlBlock> blocks = blockFactory.getNewBlocks();
//                  if (initial) {
//                     blocks = blockFactory.getBlockHierarchy();
//                     initial = false;
//                  }
//                  List<PlsqlBlock> removedBlocks = blockFactory.getRemovedBlocks();

//                  final Fold root = hierarchy.getRootFold();
//                  final List<FoldInfo> collapsedFolds = new ArrayList<FoldInfo>();
//                  getCollapsedFolds(root, collapsedFolds);
//                  addFolds(newBlocks, transaction, collapsedFolds);
//                  addFolds(blockFactory.getCustomFolds(), transaction, collapsedFolds);

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
   }

   private int getMarkCount() {
      return markArray.getItemCount();
   }

   private void removeMark(int index) {
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "Removing mark from ind={0}: {1}", new Object[]{index, getMark(index)}); // NOI18N
      }
      markArray.remove(index, 1);
   }

   private void insertMark(int index, FoldMarkInfo mark) {
      markArray.insertItem(index, mark);
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "Inserted mark at ind={0}: {1}", new Object[]{index, mark}); // NOI18N
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
//      for (PlsqlBlock block : blocks) {
//         Token token = blocks.token();
//         List<FoldMarkInfo> info;
      try {
         scanToken(markList, blocks);
      } catch (BadLocationException e) {
         LOG.log(Level.WARNING, null, e);
//            info = null;
      }

//         if (info != null) {
//            if (markList == null) {
//               markList = new ArrayList<FoldMarkInfo>();
//            }
//            markList.addAll(info);
//         }
//      }

      return markList;
   }

   private void processTokenList(List<PlsqlBlock> blocks, FoldHierarchyTransaction transaction) {
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
               if (LOG.isLoggable(Level.FINE)) {
                  LOG.log(Level.FINE, "Removed dup mark from ind={0}: {1}", new Object[]{arrayMarkIndex, arrayMark}); // NOI18N
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
            if (LOG.isLoggable(Level.FINE)) {
               LOG.log(Level.FINE, "Inserted mark at ind={0}: {1}", new Object[]{arrayMarkIndex, listMark}); // NOI18N
            }
            arrayMarkIndex++;
         }
      }
   }

   private void updateFolds(List<PlsqlBlock> blocks, FoldHierarchyTransaction transaction) {

      if (blocks != null && !blocks.isEmpty()) {
         processTokenList(blocks, transaction);
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
   private static Pattern pattern = Pattern.compile(
           "(<\\s*editor-fold"
           + // id="x"[opt] defaultstate="y"[opt] desc="z"[opt] defaultstate="a"[opt]
           // id must be first, the rest of attributes in random order
           "(?:(?:\\s+id=\"(\\S*)\")?(?:\\s+defaultstate=\"(\\S*?)\")?(?:\\s+desc=\"([\\S \\t]*?)\")?(?:\\s+defaultstate=\"(\\S*?)\")?)"
           + "\\s*>)|(?:</\\s*editor-fold\\s*>)"); // NOI18N

   private void scanToken(List<FoldMarkInfo> list, List<PlsqlBlock> blocks) throws BadLocationException {
      // ignore any token that is not comment
//      if (token.id().primaryCategory() != null && token.id().primaryCategory().startsWith("comment")) { //NOI18N
//         Matcher matcher = pattern.matcher(token.text());
//         if (matcher.find()) {
//            if (matcher.group(1) != null) { // fold's start mark found
//               boolean state;
//               if (matcher.group(3) != null) {
//                  state = "collapsed".equals(matcher.group(3)); // remember the defaultstate // NOI18N
//               } else {
//                  state = "collapsed".equals(matcher.group(5));
//               }
//
//               if (matcher.group(2) != null) { // fold's id exists
//                  Boolean collapsed = (Boolean) customFoldId.get(matcher.group(2));
//                  if (collapsed != null) {
//                     state = collapsed.booleanValue(); // fold's state is already known from the past
//                  } else {
//                     customFoldId.put(matcher.group(2), Boolean.valueOf(state));
//                  }
//               }
//               return new FoldMarkInfo(true, token.offset(null), matcher.end(0), matcher.group(2), state, matcher.group(4)); // NOI18N
//            } else { // fold's end mark found
//               return new FoldMarkInfo(false, token.offset(null), matcher.end(0), null, false, null);
//            }
//         }
//      }
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
            final int length = block.getPrefix().length() + block.getName().length();
//         List<FoldMarkInfo> list = new ArrayList<FoldMarkInfo>();
            //XXX: find out what offsets are needed (check CustomFoldManager when .php is run):
            list.add(new FoldMarkInfo(foldType, true, block.getStartOffset(), length, block.getName(), false, description));
            list.add(new FoldMarkInfo(foldType, false, block.getEndOffset(), 0, null, false, null));
            scanToken(list, block.getChildBlocks());
//         return list;
         }
      }
//      return null;
   }

   private boolean myRemoveFolds(FoldHierarchyTransaction transaction) {
      final PlsqlBlockFactory blockFactory = getBlockFactory();
      if (blockFactory == null) {
         return true;
      }
      final FoldHierarchy fh = getOperation().getHierarchy();
      //Get folds from Block maker
      final Fold root = fh.getRootFold();
      final List<PlsqlBlock> oldBlocks = blockFactory.getRemovedBlocks();
      //Remove non existing blocks
      final int childCount = root.getFoldCount();
      if (!oldBlocks.isEmpty()) {
         for (int i = (childCount - 1); i >= 0; i--) {
            final Fold child = root.getFold(i);
            removeWithChildren(child, oldBlocks, transaction);
         }
      }
      return false;
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
//   @Override
//   public void initFolds(final FoldHierarchyTransaction foldHierarchyTransaction) {
//      final Document doc = getDocument();
//      if (!(doc instanceof BaseDocument)) {
//         return;
//      }
//
//      final PlsqlBlockFactory blockFactory = getBlockFactory();
//      if (blockFactory != null) {
//         try {
//
//            //Initialize document hierarchy
//            blockFactory.initHierarchy(doc);
//            //Add new blocks to the hierarchy
//            List<PlsqlBlock> newBlocks;
//            if (initial) {
//               newBlocks = blockFactory.getBlockHierarchy();
//               initial = false;
//            } else {
//               newBlocks = blockFactory.getNewBlocks();
//            }
//            addFolds(newBlocks, foldHierarchyTransaction, null);
//            //Add custom fold blocks
//            addFolds(blockFactory.getCustomFolds(), foldHierarchyTransaction, null);
//         } catch (BadLocationException ex) {
//            Exceptions.printStackTrace(ex);
//         }
//      }
//   }
//

   /**
    * Add folds to the folder hierarchy (initial)
    *
    * @param blockHier
    * @param fhTransaction
    */
   private void addFolds(final List<PlsqlBlock> blockHier, final FoldHierarchyTransaction fhTransaction, final List<FoldInfo> collapsedFolds) {
      final int count = blockHier.size();
      for (int i = 0; i < count; i++) {
         try {
            final PlsqlBlock temp = blockHier.get(i);
            FoldType foldType = null;
            final PlsqlBlockType type = temp.getType();
            String description = "";

            if (!(type == PlsqlBlockType.COMMENT && doc.getText(temp.getStartOffset(), temp.getEndOffset() - temp.getStartOffset()).indexOf("\n") == -1)) { // check for single line comments
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

               if (doc.getEndPosition().getOffset() >= temp.getEndOffset()) {
                  operation.addToHierarchy(foldType, description, isCollapsed(temp, foldType, collapsedFolds),
                          temp.getStartOffset(), temp.getEndOffset(), 0, 0, null, fhTransaction);

                  //check for any child folds and add them also
                  addFolds(temp.getChildBlocks(), fhTransaction, collapsedFolds);
               }
            }
         } catch (BadLocationException ex) {
            LOG.log(Level.FINE, "BadLocationException thrown in addFolds", ex);
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
    * Method that will add collapsed folds (FoldInfo objects) in the fold hierarchy to the given list
    *
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
    * Method that will select and return the corresponding fold to parent from oldRoot fold hierarchy
    *
    * @param parent
    * @param foldInfoLst
    * @return
    */
   private boolean isCollapsed(final PlsqlBlock block, final FoldType foldType, final List<FoldInfo> foldInfoLst) {
      if (foldInfoLst == null) {
         if (OptionsUtilities.isPlSqlExpandFolds()) {
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

//   /**
//    * Method that will update the folds based on the changes to the document
//    *
//    * @param fhTran
//    */
//   private synchronized void updateFolds(final FoldHierarchyTransaction fhTran) {
//      try {
//         final PlsqlBlockFactory blockFactory = getBlockFactory();
//         if (blockFactory == null) {
//            return;
//         }
//
//         final FoldHierarchy fh = getOperation().getHierarchy();
//         try {
//            //lock the hierarchy
//            fh.lock();
//
//            final Fold root = fh.getRootFold();
//            final List<FoldInfo> collapsedFolds = new ArrayList<FoldInfo>();
//            getCollapsedFolds(root, collapsedFolds);
//
//            //Get folds from Block maker
//            final List<PlsqlBlock> oldBlocks = blockFactory.getRemovedBlocks();
//
//            //Remove non existing blocks
//            final int childCount = root.getFoldCount();
//            if (!oldBlocks.isEmpty()) {
//               for (int i = (childCount - 1); i >= 0; i--) {
//                  final Fold child = root.getFold(i);
//                  removeWithChildren(child, oldBlocks, fhTran);
//               }
//            }
//
//            //Add new blocks to the hierarchy
//            final List<PlsqlBlock> newBlocks = blockFactory.getNewBlocks();
//            addFolds(newBlocks, fhTran, collapsedFolds);
//            //Add custom folds
//            addFolds(blockFactory.getCustomFolds(), fhTran, collapsedFolds);
//
//            if (blockFactory.isAliasesChanged()) {
//               final Map<String, PlsqlBlock> blockMap = new HashMap<String, PlsqlBlock>();
//               getBlockMap(blockMap, blockFactory.getBlockHierarchy());
//               final int rootChildren = root.getFoldCount();
//               for (int i = (rootChildren - 1); i >= 0; i--) {
//                  final Fold child = root.getFold(i);
//                  changeDescriptions(child, blockMap, fhTran);
//               }
//            }
//
//            //compareFoldHierarchies(root, collapsedFolds, fhTran);
//         } finally {
//            fh.unlock();
//         }
//      } catch (Exception e) {
//         ErrorManager.getDefault().notify(e);
//      }
//   }
//
//   /**
//    * Method that will change the fold descriptions if changed
//    *
//    * @param parent
//    * @param blockMap
//    * @param fhTran
//    */
//   private void changeDescriptions(final Fold parent, final Map<String, PlsqlBlock> blockMap, final FoldHierarchyTransaction fhTran) throws BadLocationException {
//      final PlsqlBlock block = getCorrespondingBlock(parent, blockMap);
//
//      //check whether block name is an alias
//      if ((block != null) && (!block.getAlias().equals(""))) {  //Aliases wont be there for COMMENT & DECLARE
//         final String description = getFoldDescription(block);
//         //check whether description is different
//         if (!parent.getDescription().equals(description)) {
//            operation.removeFromHierarchy(parent, fhTran);
//            operation.addToHierarchy(parent.getType(), description, parent.isCollapsed(),
//                    block.getStartOffset(), block.getEndOffset(), 0, 0, null, fhTran);
//         }
//      }
//
//      final int childCount = parent.getFoldCount();
//      for (int i = (childCount - 1); i >= 0; i--) {
//         final Fold child = parent.getFold(i);
//         //Ignore types that cannot have the name as an alias
//         if ((child.getType() != PlsqlFoldTypes.BEGINEND)
//                 && (child.getType() != PlsqlFoldTypes.COMMENT)
//                 && (child.getType() != PlsqlFoldTypes.DECLAREEND)
//                 && (child.getType() != PlsqlFoldTypes.IF)
//                 && (child.getType() != PlsqlFoldTypes.CUSTOM)
//                 && (child.getType() != PlsqlFoldTypes.FORLOOP)
//                 && (child.getType() != PlsqlFoldTypes.LOOP)
//                 && (child.getType() != PlsqlFoldTypes.WHILELOOP)) {
//
//            changeDescriptions(child, blockMap, fhTran);
//         }
//      }
//   }
//
   /**
    * Get the matching block to the fold. Consider the type, start & end offset
    *
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
    *
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
    * Method that will return the fold description given the Plsql block Used when changing the descriptions only.
    *
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

   /**
    * Remove fold with its children
    *
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
    *
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
    *
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
//
//   /**
//    * Document event has occurred
//    *
//    * @param o
//    * @param arg
//    */
//   public void update(final Observable o, final Object arg) {
//      final FoldHierarchyTransaction fhTran = getOperation().openTransaction();
//      updateFolds(fhTran);
//      // allready commited at this time
//      //fhTran.commit();
//   }

   /**
    * Private class that holds some fold info This is used to collapse folds after a change
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
