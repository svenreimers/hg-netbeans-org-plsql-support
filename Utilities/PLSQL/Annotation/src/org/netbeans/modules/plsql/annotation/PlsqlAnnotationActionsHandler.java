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
package org.netbeans.modules.plsql.annotation;

import org.netbeans.modules.plsql.annotation.annotations.PlsqlAnnotation;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.editor.AnnotationDesc;
import org.netbeans.editor.Annotations;
import org.netbeans.editor.BaseDocument;
import org.openide.util.Exceptions;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.loaders.DataObject;
import org.openide.text.Annotation;

public class PlsqlAnnotationActionsHandler implements AWTEventListener, ListSelectionListener {

   private static final PlsqlAnnotationActionsHandler instance = new PlsqlAnnotationActionsHandler();
   private static final String POPUP_NAME = "hintsPopup";
   private static final int POPUP_VERTICAL_OFFSET = 5;
   private JLabel hintIcon;
   private JLabel errorTooltip;
   private Popup listPopup;
   private Popup tooltipPopup;
   private PlsqlListCompletionView list;

   public static PlsqlAnnotationActionsHandler getDefault() {
      return instance;
   }

   boolean invokeDefaultAction(final boolean onlyActive) {
      final JTextComponent comp = EditorRegistry.lastFocusedComponent();
      if (comp == null) {
         return false;
      }
      final Document doc = comp.getDocument();
      if (doc instanceof BaseDocument) {
         final Annotations annotations = ((BaseDocument) doc).getAnnotations();

         try {
            final Rectangle carretRectangle = comp.modelToView(comp.getCaretPosition());
            final int line = NbEditorUtilities.getLine(doc, comp.getCaretPosition(), false).getLineNumber();
            final AnnotationDesc annoDesc = annotations.getActiveAnnotation(line);
            if (annoDesc == null) {
               return false;
            }

            final int startOffset = annoDesc.getOffset();
            final List<PlsqlAnnotation> lstAnnotations = new ArrayList<PlsqlAnnotation>();
            getRelevantAnnotations(lstAnnotations, doc, startOffset);

            if (lstAnnotations.isEmpty()) {
               return false;
            }

            Point p = comp.modelToView(Utilities.getRowStartFromLineOffset((BaseDocument) doc, line)).getLocation();
            p.y += carretRectangle.height;
            if (comp.getParent() instanceof JViewport) {
               p.x += ((JViewport) comp.getParent()).getViewPosition().x;
            }
            //Show popup for the active annotation for the line
            PlsqlAnnotation annotation = null;
            if (lstAnnotations.size() > 1) {
               for (PlsqlAnnotation tmp : lstAnnotations) {
                  if (tmp.getAnnotationType().equals(annoDesc.getAnnotationType())) {
                     annotation = tmp;
                     break;
                  }
               }
            } else {
               annotation = lstAnnotations.get(0);
            }
            showPopup(comp, p, annotation);

            return true;
         } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
         }
      }
      return true;
   }

   private void getRelevantAnnotations(final List<PlsqlAnnotation> lstAnnotation, final Document doc, final int offset) {
      final DataObject od = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
      if (od == null) {
         return;
      }

      final PlsqlAnnotationManager annotationManager = od.getLookup().lookup(PlsqlAnnotationManager.class);
      if (annotationManager != null) {
         for (Annotation a : annotationManager.getAnnotations(offset)) {
            lstAnnotation.add((PlsqlAnnotation) a);
         }
      }
   }
   private PopupFactory pf = null;

   private PopupFactory getPopupFactory() {
      if (pf == null) {
         pf = PopupFactory.getSharedInstance();
      }
      return pf;
   }

   private void removePopup() {
      if (tooltipPopup != null) {
         tooltipPopup.hide();
         tooltipPopup = null;
         errorTooltip = null;
      }

      if (listPopup != null) {
         listPopup.hide();
         list = null;
         listPopup = null;
      }
   }

   private Dimension getMaxSizeAt(final Point p, final JTextComponent comp) {

      Rectangle screenBounds = null;
      if (null != comp && null != comp.getGraphicsConfiguration()) {
         screenBounds = comp.getGraphicsConfiguration().getBounds();
      } else {
         screenBounds = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
      }
      Dimension maxSize = screenBounds.getSize();
      maxSize.width -= p.x - screenBounds.x;
      maxSize.height -= p.y - screenBounds.y;
      return maxSize;
   }

   @Override
   public void eventDispatched(final AWTEvent aWTEvent) {
      if (aWTEvent instanceof MouseEvent) {
         final MouseEvent mv = (MouseEvent) aWTEvent;
         if (mv.getID() == MouseEvent.MOUSE_CLICKED && mv.getClickCount() > 0) {
            if (!(aWTEvent.getSource() instanceof Component)) {
               removePopup();
               return;
            }
            final Component comp = (Component) aWTEvent.getSource();
            final Container par = SwingUtilities.getAncestorNamed(POPUP_NAME, comp); //NOI18N
            if (par == null) {
               removePopup();
            }
         }
      }
   }

   public void valueChanged(final ListSelectionEvent e) {
      if (e.getSource() == list) {
         final Object selected = list.getSelectedValue();
         final JTextComponent c = EditorRegistry.lastFocusedComponent();
         if (selected instanceof AbstractAction) {

            if (c != null) {
               c.requestFocus();
            }
            ((AbstractAction) selected).actionPerformed(null);
            removePopup();
         }
      }
   }

   private void checkOnMouseClick(MouseEvent e) {
      if (e.getSource() == list) {
         final Object selected = list.getSelectedValue();
         final JTextComponent c = EditorRegistry.lastFocusedComponent();
         if (selected != null && selected instanceof AbstractAction) {
            e.consume();
            if (c != null) {
               c.requestFocus();
            }
            ((AbstractAction) selected).actionPerformed(null);
            removePopup();
         }
      }
   }

   public void showPopup(final JTextComponent comp, final Point position, final PlsqlAnnotation annotation) {
      removePopup();
      Point p = new Point(position);
      SwingUtilities.convertPointToScreen(p, comp);

      if (hintIcon != null) {
         hintIcon.setToolTipText(null);
      }

      // be sure that hint will hide when popup is showing
      ToolTipManager.sharedInstance().setEnabled(false);
      ToolTipManager.sharedInstance().setEnabled(true);
      Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
      errorTooltip = new JLabel(annotation.getErrorToolTip());
      errorTooltip.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK), BorderFactory.createEmptyBorder(0, 3, 0, 3)));
      errorTooltip.addMouseListener(new MouseAdapter() {

         @Override
         public void mouseClicked(MouseEvent e) {
            checkOnMouseClick(e);
         }
      });

      int rowHeight = 14; //default

      Dimension popup = null;
      if (annotation.getActions().length > 0) {
         list = new PlsqlListCompletionView(annotation.getActions());
         popup = list.getPreferredSize();
      }

      final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      final boolean exceedsHeight = popup != null ? p.y + popup.height > screen.height : p.y > screen.height;

      try {
         final int pos = javax.swing.text.Utilities.getRowStart(comp, comp.getCaret().getDot());
         final Rectangle r = comp.modelToView(pos);
         rowHeight = r.height;
         int y;
         if (exceedsHeight) {
            y = p.y + POPUP_VERTICAL_OFFSET;
         } else {
            y = p.y - rowHeight - errorTooltip.getPreferredSize().height - POPUP_VERTICAL_OFFSET;
         }
         tooltipPopup = getPopupFactory().getPopup(comp, errorTooltip, p.x, y);
      } catch (BadLocationException blE) {
         Exceptions.printStackTrace(blE);
         errorTooltip = null;
      }

      if (list != null) {
         if (exceedsHeight) {
            p.y -= popup.height + rowHeight + POPUP_VERTICAL_OFFSET;
         }
         if (p.x + popup.width > screen.width) {
            //shift popup left necessary
            p.x -= p.x + popup.width - screen.width;
         }
         list.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
               checkOnMouseClick(e);
            }
         });

         list.addListSelectionListener(this);
         list.setName(POPUP_NAME);

         final PlsqlScrollCompletionPane scrollPane = new PlsqlScrollCompletionPane(list, comp, null, null, getMaxSizeAt(p, comp));
         listPopup = getPopupFactory().getPopup(comp, scrollPane, p.x, p.y);
         listPopup.show();
      }

      if (tooltipPopup != null) {
         tooltipPopup.show();
      }
   }
}
