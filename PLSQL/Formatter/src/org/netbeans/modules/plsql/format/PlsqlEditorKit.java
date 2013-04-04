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
package org.netbeans.modules.plsql.format;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import org.netbeans.api.editor.fold.Fold;
import org.netbeans.api.editor.fold.FoldHierarchy;
import org.netbeans.api.editor.fold.FoldUtilities;
import org.netbeans.editor.BaseAction;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Formatter;
import org.netbeans.editor.Syntax;
import org.netbeans.editor.SyntaxSupport;
import org.netbeans.modules.editor.NbEditorKit;
import org.openide.util.NbBundle;

public class PlsqlEditorKit extends NbEditorKit {

   public static Document t;
   public static final String MIME_TYPE = "text/x-plsql";
   public static final String expandFolds = "expand-folds";
   public static final String collapseFolds = "collapse-folds";
   public static final String expandChildFolds = "expand-child-folds";
   public static final String collapseChildFolds = "collapse-child-folds";

   /**
    * Creates a new instance of PLSQLEditorKit
    */
   public PlsqlEditorKit() {
   }

   /**
    * Create a syntax object suitable for highlighting PLSQL file syntax
    *
    */
   @Override
   public Syntax createSyntax(Document doc) {
      return new PlsqlSyntax();
   }

   @Override
   public SyntaxSupport createSyntaxSupport(BaseDocument doc) {
      return new PlsqlSyntaxSupport(doc);
   }
   
   public Formatter createFormatter(){
      return new PlsqlFormatter(PlsqlEditorKit.class);
   }

   /**
    * Override create actions method to add our actions
    * @return
    */
   @Override
   protected Action[] createActions() {
      Action[] superActions = super.createActions();
      Action[] plsqlActions = new Action[]{
         new ExpandFolds(),
         new CollapseFolds(),
         new ExpandChildFolds(),
         new CollapseChildFolds(),
         new PlsqlGenerateFoldPopupAction(),};

      return TextAction.augmentList(superActions, plsqlActions);
   }

   /**
    * Retrieves the content type for this editor kit
    */
   @Override
   public String getContentType() {
      return MIME_TYPE;
   }

   /**
    * Class to expand folds action which expands folds within the selected area
    */
   public static class ExpandFolds extends BaseAction {

      public ExpandFolds() {
         super(expandFolds);
         putValue(SHORT_DESCRIPTION, NbBundle.getBundle(PlsqlEditorKit.class).getString("expand-folds"));
         putValue(BaseAction.POPUP_MENU_TEXT, NbBundle.getBundle(PlsqlEditorKit.class).getString("popup-expand-folds"));
      }

      @Override
      public void actionPerformed(ActionEvent evt, JTextComponent target) {
         int startSelect = -1;
         int endSelect = -1;

         Caret caret = target.getCaret();
         endSelect = Math.max(caret.getMark(), caret.getDot());
         startSelect = Math.min(caret.getMark(), caret.getDot());

         FoldHierarchy hierarchy = FoldHierarchy.get(target);

         //Take the fold array that belongs to the selected area
         List<Fold> lstFold = getSelectedFolds(hierarchy, startSelect, endSelect);
         if (lstFold != null && lstFold.size() > 0) {
            hierarchy.lock();
            for (int i = 0; i < lstFold.size(); i++) {
               Fold temp = lstFold.get(i);
               hierarchy.expand(temp);
            }
            hierarchy.unlock();
         }

      //If there are no enclosed blocks try default expand fold
//                (new ActionFactory.ExpandFold()).actionPerformed(null, panes[0]);
//                return;
      }
   }

   /**
    * Class to collapse folds action which collapses folds within the selected area
    */
   public static class CollapseFolds extends BaseAction {

      public CollapseFolds() {
         super(collapseFolds);
         putValue(SHORT_DESCRIPTION, NbBundle.getBundle(PlsqlEditorKit.class).getString("collapse-folds"));
         putValue(BaseAction.POPUP_MENU_TEXT, NbBundle.getBundle(PlsqlEditorKit.class).getString("popup-collapse-folds"));
      }

      @Override
      public void actionPerformed(ActionEvent evt, JTextComponent target) {
         int startSelect = -1;
         int endSelect = -1;

         Caret caret = target.getCaret();
         endSelect = Math.max(caret.getMark(), caret.getDot());
         startSelect = Math.min(caret.getMark(), caret.getDot());

         FoldHierarchy hierarchy = FoldHierarchy.get(target);

         //Take the fold array that belongs to the selected area
         List<Fold> lstFold = getSelectedFolds(hierarchy, startSelect, endSelect);
         if (lstFold != null && lstFold.size() > 0) {
            hierarchy.lock();
            for (int i = 0; i < lstFold.size(); i++) {
               Fold temp = lstFold.get(i);
               hierarchy.collapse(temp);
            }
            hierarchy.unlock();
         }

      //If there are no enclosed blocks try default expand fold
//                (new ActionFactory.CollapseFold()).actionPerformed(null, panes[0]);
//                return;
      }
   }

   /**
    * Class to expand child folds of the fold which has the caret position
    */
   public static class ExpandChildFolds extends BaseAction {

      public ExpandChildFolds() {
         super(expandChildFolds);
         putValue(SHORT_DESCRIPTION, NbBundle.getBundle(PlsqlEditorKit.class).getString("expand-child-folds"));
         putValue(BaseAction.POPUP_MENU_TEXT, NbBundle.getBundle(PlsqlEditorKit.class).getString("popup-expand-child-folds"));
      }

      @Override
      public void actionPerformed(ActionEvent evt, JTextComponent target) {
         int dot = target.getCaret().getDot();
         int mark = target.getCaret().getMark();

         //Take the fold that corresponds to the dot position
         FoldHierarchy hierarchy = FoldHierarchy.get(target);
         Fold parent = FoldUtilities.findOffsetFold(hierarchy, dot);
         if (parent == null) {
            return; // no success
         }

         if (mark != dot) {
            //Take the corresponding fold to the mark position
            Fold otherParent = FoldUtilities.findOffsetFold(hierarchy, mark);
            if (otherParent == null) {
               return;
            }

            //Either both folds should be the same or one should be a parent of the other
            if (parent != otherParent) {
               if (parent.getStartOffset() >= otherParent.getStartOffset() &&
                     parent.getEndOffset() <= otherParent.getEndOffset()) {
                  parent = otherParent;
               } else if (!(parent.getStartOffset() <= otherParent.getStartOffset() &&
                     parent.getEndOffset() >= otherParent.getEndOffset())) {
                  return;
               }
            }
         }

         hierarchy.lock();
         //Get the child folds and collapse them
         Fold[] lstFold = FoldUtilities.childrenToArray(parent);
         if (lstFold != null) {
            for (int i = 0; i < lstFold.length; i++) {
               Fold temp = lstFold[i];
               hierarchy.expand(temp);
            }
         }
         hierarchy.unlock();
      }
   }

   /**
    * Class to collapse child folds of the fold which has the caret position
    */
   public static class CollapseChildFolds extends BaseAction {

      public CollapseChildFolds() {
         super(collapseChildFolds);
         putValue(SHORT_DESCRIPTION, NbBundle.getBundle(PlsqlEditorKit.class).getString("collapse-child-folds"));
         putValue(BaseAction.POPUP_MENU_TEXT, NbBundle.getBundle(PlsqlEditorKit.class).getString("popup-collapse-child-folds"));
      }

      @Override
      public void actionPerformed(ActionEvent evt, JTextComponent target) {
         int dot = target.getCaret().getDot();
         int mark = target.getCaret().getMark();

         //Take the fold that corresponds to the dot position
         FoldHierarchy hierarchy = FoldHierarchy.get(target);
         Fold parent = FoldUtilities.findOffsetFold(hierarchy, dot);
         if (parent == null) {
            return; // no success
         }

         if (mark != dot) {
            //Take the corresponding fold to the mark position
            Fold otherParent = FoldUtilities.findOffsetFold(hierarchy, mark);
            if (otherParent == null) {
               return;
            }

            //Either both folds should be the same or one should be a parent of the other
            if (parent != otherParent) {
               if (parent.getStartOffset() >= otherParent.getStartOffset() &&
                     parent.getEndOffset() <= otherParent.getEndOffset()) {
                  parent = otherParent;
               } else if (!(parent.getStartOffset() <= otherParent.getStartOffset() &&
                     parent.getEndOffset() >= otherParent.getEndOffset())) {
                  return;
               }
            }
         }

         hierarchy.lock();
         //Get the child folds and collapse them
         Fold[] lstFold = FoldUtilities.childrenToArray(parent);
         if (lstFold != null) {
            for (int i = 0; i < lstFold.length; i++) {
               Fold temp = lstFold[i];
               hierarchy.collapse(temp);
            }
         }
         hierarchy.unlock();
      }
   }

   public static class PlsqlGenerateFoldPopupAction extends GenerateFoldPopupAction {

      @Override
      protected void addAdditionalItems(JTextComponent target, JMenu menu) {
         addAction(target, menu, collapseFolds);
         addAction(target, menu, expandFolds);
         setAddSeparatorBeforeNextAction(true);
         addAction(target, menu, collapseChildFolds);
         addAction(target, menu, expandChildFolds);
      }
   }

   /**
    * Method that will return the fold array encapsulated by the selected area
    * @param hierarchy
    * @param startSelect
    * @param endSelect
    * @return
    */
   private static List<Fold> getSelectedFolds(FoldHierarchy hierarchy, int startSelect, int endSelect) {
      List<Fold> lstfold = new ArrayList<Fold>();

      Fold tmp = FoldUtilities.findNearestFold(hierarchy, startSelect);
      int offset = startSelect;

      //collect all the fold while we are not at the end of the selection
      while ((tmp != null) && ((tmp.getStartOffset() <= endSelect) &&
            (tmp.getEndOffset() >= startSelect))) {
         if ((tmp.getStartOffset() >= startSelect) &&
               (tmp.getEndOffset() <= endSelect)) {
            lstfold.add(tmp);
            offset = tmp.getEndOffset();
         } else {
            offset = tmp.getStartOffset();
         }
         
         tmp = FoldUtilities.findNearestFold(hierarchy, offset + 1);
      }

      return lstfold;
   }
}
