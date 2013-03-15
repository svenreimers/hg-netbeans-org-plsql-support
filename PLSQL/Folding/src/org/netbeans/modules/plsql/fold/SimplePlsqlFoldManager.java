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
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldType;
import org.netbeans.api.editor.fold.FoldUtilities;
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
public class SimplePlsqlFoldManager implements FoldManager, Runnable, Observer {

   private static final Logger LOG = Logger.getLogger(SimplePlsqlFoldManager.class.getName());
   private static final int TASK_DELAY = 500;
   private FoldOperation operation;
   private Document doc;
   private static final RequestProcessor RP = new RequestProcessor(SimplePlsqlFoldManager.class.getName(), 1, false, false);
   private final RequestProcessor.Task task = RP.create(this);
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
      task.schedule(TASK_DELAY);
   }

   @Override
   public void insertUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
//      task.schedule(300);
//      processRemovedFolds(transaction);
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "insertUpdate: {0}", System.identityHashCode(this));
      }
   }

   @Override
   public void removeUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "removeUpdate: {0}", System.identityHashCode(this));
      }
      task.schedule(TASK_DELAY);
//      processRemovedFolds(transaction);
//      removeAffectedMarks(evt, transaction);
   }

   @Override
   public void changedUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "changedUpdate: {0}", System.identityHashCode(this));
      }
//      task.schedule(300);
   }

   @Override
   public void removeEmptyNotify(Fold emptyFold) {
//      removeFoldNotify(emptyFold);
   }

   @Override
   public void removeDamagedNotify(Fold damagedFold) {
//      removeFoldNotify(damagedFold);
   }

   @Override
   public void expandNotify(Fold expandedFold) {
   }

   @Override
   public void release() {
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "release: {0}", System.identityHashCode(this));
      }
   }

   @Override
   public void update(Observable o, Object arg) {
      if (LOG.isLoggable(Level.FINE)) {
         LOG.log(Level.FINE, "update: {0}", System.identityHashCode(this));
      }
      task.schedule(TASK_DELAY);
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
                  ArrayList<Fold> existingFolds = new ArrayList<Fold>();
                  collectExistingFolds(hierarchy.getRootFold(), existingFolds);
                  for (Fold f : existingFolds) {
                     getOperation().removeFromHierarchy(f, transaction);
                  }
                  List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
                  createFolds(blocks, transaction);
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

   /**
    * Collects all folds from the hierarchy that were created by this manager and are not the root fold.
    *
    * @param fold
    * @param list
    */
   private void collectExistingFolds(Fold fold, List<Fold> list) {
      for (int i = 0; i < fold.getFoldCount(); i++) {
         collectExistingFolds(fold.getFold(i), list);
      }
      if (!FoldUtilities.isRootFold(fold) && getOperation().owns(fold)) {
         list.add(fold);
      }
   }

   /**
    * Creates a new fold and adds to the fold hierarchy.
    */
//    private Fold createFold(FoldType type, String description, boolean collapsed,
//            int startOffset, int endOffset, FoldHierarchyTransaction transaction)
//                throws BadLocationException {
//        Fold fold = null;
//        if ( startOffset >= 0 &&
//             endOffset >= 0 &&
//             startOffset < endOffset &&
//             endOffset <= getDocument().getLength() ) {
//            fold = getOperation().addToHierarchy(
//                    type,
//                    description.intern(), //save some memory
//                    collapsed,
//                    startOffset,
//                    endOffset,
//                    description.length(),
//                    0,
//                    null,
//                    transaction);
//        }
//        return fold;
//    }
   private void createFolds(List<PlsqlBlock> blocks, FoldHierarchyTransaction transaction) {
      for (PlsqlBlock block : blocks) {

         FoldType foldType = null;
         final PlsqlBlockType type = block.getType();
         String description = null;
         try {
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
               try {
                  operation.addToHierarchy(foldType, description, isCollapsed(foldType), block.getStartOffset(), block.getEndOffset(), 0, 0, null, transaction);
               } catch (BadLocationException ex) {
                  if (LOG.isLoggable(Level.FINE)) {
                     LOG.log(Level.FINE, "Ignore BadLocationException", ex);
                  }
               }
               createFolds(block.getChildBlocks(), transaction);
            }
         } catch (BadLocationException ex) {
            if (LOG.isLoggable(Level.FINE)) {
               LOG.log(Level.FINE, "Ignore BadLocationException", ex);
            }
         }
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
