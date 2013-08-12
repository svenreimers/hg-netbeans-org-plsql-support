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
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AbstractDocument;
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
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;

/**
 *
 * @author chrlse
 */
public class NewPlsqlFoldManager implements FoldManager, Runnable, Observer {

    private static final Logger LOG = Logger.getLogger(NewPlsqlFoldManager.class.getName());
    private static final RequestProcessor RP = new RequestProcessor(NewPlsqlFoldManager.class.getName(), 1, false, false);
    private static final int TASK_DELAY = 100;
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
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "changeUpdate: {0}", System.identityHashCode(this));
        }
        processRemovedFolds(transaction);
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

                        final Fold root = hierarchy.getRootFold();

                        if (initial) {
                            List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
                            updateFolds(blocks, transaction, null);
                            //Add custom fold blocks
                            updateFolds(blockFactory.getCustomFolds(), transaction, null);
                            initial = false;
                        } else {
                            final List<FoldSearchObject> collapsedFolds = new ArrayList<FoldSearchObject>();
                            getCollapsedFolds(root, collapsedFolds);
                            final List<PlsqlBlock> oldBlocks = blockFactory.getRemovedBlocks();

                            //Remove non existing blocks   
                            if (!oldBlocks.isEmpty()) {
                                final int childCount = root.getFoldCount();
                                for (int i = (childCount - 1); i >= 0; i--) {
                                    final Fold child = root.getFold(i);
                                    removeWithChildren(child, oldBlocks, transaction);
                                }
                            }
                            //Add new blocks to the hierarchy
                            List<PlsqlBlock> blocks = blockFactory.getNewBlocks();
                            updateFolds(blocks, transaction, collapsedFolds);
                            //Add custom fold blocks
                            updateFolds(blockFactory.getCustomFolds(), transaction, collapsedFolds);
                        }
                    } catch (BadLocationException ex) {
                        Exceptions.printStackTrace(ex);
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

    private void getCollapsedFolds(final Fold parent, final List<FoldSearchObject> foldInfoLst) {
        if (parent.isCollapsed()) {
            final FoldSearchObject tempInfo = new FoldSearchObject(parent.getStartOffset(), parent.getEndOffset(), parent.getType());
            foldInfoLst.add(tempInfo);
        }
        final int count = parent.getFoldCount();
        for (int i = 0; i < count; i++) {
            final Fold temp = parent.getFold(i);
            getCollapsedFolds(temp, foldInfoLst);
        }
    }

    /**
     * Remove fold with its children
     *
     * @param fold
     * @param blockHier
     * @param transaction
     * @return true if removed all the children
     */
    private void removeWithChildren(final Fold fold, final List<PlsqlBlock> blockHier, final FoldHierarchyTransaction transaction) {

        final int childCount = fold.getFoldCount();
        for (int i = (childCount - 1); i >= 0; i--) {
            final Fold child = fold.getFold(i);
            removeWithChildren(child, blockHier, transaction);
        }

        //If a custom fold remove
        if (fold.getType() == PlsqlFoldTypes.CUSTOM || checkExists(fold, blockHier)) {
            operation.removeFromHierarchy(fold, transaction);
            foldSearchObjects.remove(new FoldSearchObject(new FoldAdapter(fold)));
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
            final PlsqlBlock block = blockHier.get(i);
            if (block.getStartOffset() <= fold.getStartOffset() && block.getEndOffset() >= fold.getEndOffset()) {
                return true;
            }
            if (block.getPreviousStart() <= fold.getStartOffset() && block.getPreviousEnd() >= fold.getEndOffset()) {
                return true;
            }
            if ((block.getEndOffset() == fold.getEndOffset() || fold.getEndOffset() == block.getPreviousEnd()
                    || block.getStartOffset() == fold.getStartOffset() || block.getPreviousStart() == fold.getStartOffset())
                    && (getFoldType(block.getType()).equals(fold.getType()))
                    && (getFoldDescription(block).equals(fold.getDescription()))) {
                return true;
            }

            if (checkExists(fold, block.getChildBlocks(), comparator)) {
                return true;
            }
        }

        return false;
    }

    private FoldType getFoldType(final PlsqlBlockType blockType) {
        switch (blockType) {
            case VIEW:
                return PlsqlFoldTypes.VIEW;
            case TABLE_COMMENT:
                return PlsqlFoldTypes.TABLECOMMENT;
            case COLUMN_COMMENT:
                return PlsqlFoldTypes.COLUMNCOMMENT;
            case COMMENT:
                return PlsqlFoldTypes.COMMENT;
            case PACKAGE:
                return PlsqlFoldTypes.PACKAGE;
            case PACKAGE_BODY:
                return PlsqlFoldTypes.PACKAGEBODY;
            case PROCEDURE_IMPL:
                return PlsqlFoldTypes.PROCEDUREIMPL;
            case FUNCTION_IMPL:
                return PlsqlFoldTypes.FUNCTIONIMPL;
            case PROCEDURE_DEF:
                return PlsqlFoldTypes.PROCEDUREDEF;
            case FUNCTION_DEF:
                return PlsqlFoldTypes.FUNCTIONDEF;
            case DECLARE_END:
                return PlsqlFoldTypes.DECLAREEND;
            case BEGIN_END:
                return PlsqlFoldTypes.BEGINEND;
            case TRIGGER:
                return PlsqlFoldTypes.TRIGGER;
            case IF:
                return PlsqlFoldTypes.IF;
            case CASE:
                return PlsqlFoldTypes.CASE;
            case WHILE_LOOP:
                return PlsqlFoldTypes.WHILELOOP;
            case FOR_LOOP:
                return PlsqlFoldTypes.FORLOOP;
            case LOOP:
                return PlsqlFoldTypes.LOOP;
            case CUSTOM_FOLD:
                return PlsqlFoldTypes.CUSTOM;
            case STATEMENT:
                return PlsqlFoldTypes.STATEMENT;
            case CURSOR:
                return PlsqlFoldTypes.CURSOR;
            case JAVA_SOURCE:
                return PlsqlFoldTypes.JAVASOURCE;
            default:
                return null;
        }
    }

    /**
     * Method that will return the fold description given the Plsql block Used
     * when changing the descriptions only.
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

    private void removeFoldNotify(Fold removedFold) {
        removedFoldList.add(removedFold);
    }

    private void processRemovedFolds(FoldHierarchyTransaction transaction) {
        for (Fold removedFold : removedFoldList) {
            operation.removeFromHierarchy(removedFold, transaction);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "Fold={0} removed={1}", new Object[]{removedFold, true});
            }
        }
        removedFoldList.clear();
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

    private void updateFolds(final List<PlsqlBlock> blockHier, final FoldHierarchyTransaction transaction, final List<FoldSearchObject> collapsedFolds) throws BadLocationException {
        final int count = blockHier.size();
        Document doc = getDocument();
        for (int i = 0; i < count; i++) {
            final PlsqlBlock block = blockHier.get(i);
            FoldType foldType = null;
            final PlsqlBlockType type = block.getType();
            String description = "";

            if (!(type == PlsqlBlockType.COMMENT && doc.getText(block.getStartOffset(), block.getEndOffset() - block.getStartOffset()).indexOf("\n") == -1)) { // check for single line comments
                foldType = getFoldType(type);
                description = getFoldDescription(block);

                if (doc.getEndPosition().getOffset() >= block.getEndOffset()) {                    
                    operation.addToHierarchy(foldType, description, isCollapsed(block, foldType, collapsedFolds),
                            block.getStartOffset(), block.getEndOffset(), 0, 0, null, transaction);
                    //check for any child folds and add them also
                    updateFolds(block.getChildBlocks(), transaction, collapsedFolds);
                }
            }
        }
    }

    /**
     * Method that will select and return the corresponding fold to parent from
     * oldRoot fold hierarchy
     *
     * @param parent
     * @param foldInfoLst
     * @return
     */
    private boolean isCollapsed(final PlsqlBlock block, final FoldType foldType, final List<FoldSearchObject> foldInfoLst) {
        if (OptionsUtilities.isPlSqlExpandFolds()) {
            return false;
        }
        if (foldInfoLst == null) {
            return foldType == PlsqlFoldTypes.COMMENT;
        }
        final int size = foldInfoLst.size();
        for (int i = 0; i < size; i++) {
            final FoldSearchObject temp = foldInfoLst.get(i);

            if ((temp.getFoldType() == foldType)
                    && (temp.getStartOffset() == block.getPreviousStart())
                    && (temp.getEndOffset() == block.getPreviousEnd())) {
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
}
