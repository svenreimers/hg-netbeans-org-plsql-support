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
   private static final RequestProcessor RP = new RequestProcessor(NewPlsqlFoldManager.class.getName(), 1, false, false);
   private static final int TASK_DELAY = 1000;
   private final RequestProcessor.Task task = RP.create(this);
   private FoldOperation operation;
   private Document doc;
   // Note: FoldSearchObject need to be in a List, otherwise contains doesn't work. Seems HashSet keeps internal list of hash.
   private final List<FoldSearchObject> foldSearchObjects = new ArrayList<FoldSearchObject>();
   private List<Fold> removedFoldList = new ArrayList<Fold>(3);
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
      task.schedule(TASK_DELAY);
   }

   @Override
   public void insertUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      if (LOG.isLoggable(Level.FINER)) {
         LOG.log(Level.FINER, "insertUpdate: {0}", System.identityHashCode(this));
      }
      processRemovedFolds(transaction);
   }

   @Override
   public void removeUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
      if (LOG.isLoggable(Level.FINER)) {
         LOG.log(Level.FINER, "removeUpdate: {0}", System.identityHashCode(this));
      }
      processRemovedFolds(transaction);
   }

   @Override
   public void changedUpdate(DocumentEvent evt, FoldHierarchyTransaction transaction) {
   }

   @Override
   public void removeEmptyNotify(Fold emptyFold) {
      if (LOG.isLoggable(Level.FINER)) {
         LOG.log(Level.FINER, "removeEmptyNotify: {0}", System.identityHashCode(this));
      }
      removeFoldNotify(emptyFold);
   }

   @Override
   public void removeDamagedNotify(Fold damagedFold) {
      if (LOG.isLoggable(Level.FINER)) {
         LOG.log(Level.FINER, "removeDamagedNotify: {0}", System.identityHashCode(this));
      }
      removeFoldNotify(damagedFold);
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
      if (LOG.isLoggable(Level.FINER)) {
         LOG.log(Level.FINER, "update: {0}", System.identityHashCode(this));
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
      removedFoldList.add(removedFold);
   }

   private void processRemovedFolds(FoldHierarchyTransaction transaction) {
      for (Fold removedFold : removedFoldList) {
         boolean remove = foldSearchObjects.remove(new FoldSearchObject(new FoldAdapter(removedFold)));
         if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Fold={0} removed={1}", new Object[]{removedFold, remove});
         }
      }
      removedFoldList.clear();
   }

   private void updateFolds(List<PlsqlBlock> blocks, FoldHierarchyTransaction transaction) {
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

               final FoldSearchObject foldSearchObject = new FoldSearchObject(block.getStartOffset(), block.getEndOffset());
               if (!foldSearchObjects.contains(foldSearchObject)) {
                  try {
                     final Fold fold = operation.addToHierarchy(foldType, description, isCollapsed(foldType), block.getStartOffset(), block.getEndOffset(), 0, 0, null, transaction);
                     foldSearchObjects.add(new FoldSearchObject(new FoldAdapter(fold)));
                  } catch (BadLocationException ex) {
                     if (LOG.isLoggable(Level.FINE)) {
                        LOG.log(Level.FINE, "Ignore BadLocationException", ex);
                     }
                  }
               }
               updateFolds(block.getChildBlocks(), transaction);
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
