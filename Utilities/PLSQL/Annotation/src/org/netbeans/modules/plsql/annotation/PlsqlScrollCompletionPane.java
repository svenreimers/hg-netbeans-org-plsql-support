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

import java.awt.Color;
import java.awt.Dimension;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.BorderFactory;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeLookup;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.openide.util.Exceptions;

public class PlsqlScrollCompletionPane extends JScrollPane {

   private PlsqlListCompletionView view;
   private JLabel topLabel;
   private Dimension minSize;
   private Dimension maxSize;
   private Dimension scrollBarSize;

   public PlsqlScrollCompletionPane(PlsqlListCompletionView lst, JTextComponent component, String title, ListSelectionListener listener, Dimension maxSize) {

      // Compute size of the scrollbars
      Dimension smallSize = super.getPreferredSize();
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
      scrollBarSize = super.getPreferredSize();
      scrollBarSize.width -= smallSize.width;
      scrollBarSize.height -= smallSize.height;
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
      setBorder(
              BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(java.awt.SystemColor.controlDkShadow),
              BorderFactory.createEmptyBorder(4, 4, 4, 4)));
      setViewportBorder(null);


      // initialize sizes, why are we using the same values as for the CC popup ??
      String mimeType = org.netbeans.lib.editor.util.swing.DocumentUtilities.getMimeType(component);
      Preferences prefs = MimeLookup.getLookup(mimeType).lookup(Preferences.class);
      minSize = parseDimension(prefs.get(SimpleValueNames.COMPLETION_PANE_MIN_SIZE, null), new Dimension(60, 17));
      setMinimumSize(minSize);

      //Resize upto edge of screenborder, not COMPLETION_PANE_MAX_SIZE
      //maxSize = parseDimension(prefs.get(SimpleValueNames.COMPLETION_PANE_MAX_SIZE, null), new Dimension(400, 300));
      if (maxSize != null) {
         this.maxSize = maxSize;
         setMaximumSize(maxSize);
      }

      // Add the completion view
      view = lst;
      setBackground(view.getBackground());
      resetViewSize();
      setViewportView(view);
      this.setTitle(title);
      setFocusable(false);
      view.setFocusable(false);
   }

   public PlsqlListCompletionView getView() {
      return view;
   }

   public
   @Override
   Dimension getPreferredSize() {
      Dimension ps = super.getPreferredSize();

      /* Add size of the vertical scrollbar by default. This could be improved
       * to be done only if the height exceeds the bounds. */
      int width = ps.width + scrollBarSize.width;
      boolean displayHorizontalScrollbar = width > maxSize.width;
      width = Math.max(Math.max(width, minSize.width),
              getTitleComponentPreferredSize().width);
      width = Math.min(width, maxSize.width);

      int height = displayHorizontalScrollbar ? ps.height + scrollBarSize.height : ps.height;
      height = Math.min(height, maxSize.height);
      height = Math.max(height, minSize.height);
      return new Dimension(width, height);
   }

   private void resetViewSize() {
      Dimension viewSize = view.getPreferredSize();
      if (viewSize.width > maxSize.width - scrollBarSize.width) {
         viewSize.width = maxSize.width - scrollBarSize.width;
         view.setPreferredSize(viewSize);
      }
   }

   private void setTitle(String title) {
      if (title == null) {
         if (topLabel != null) {
            setColumnHeader(null);
            topLabel = null;
         }
      } else {
         if (topLabel != null) {
            topLabel.setText(title);
         } else {
            topLabel = new JLabel(title);
            topLabel.setForeground(Color.blue);
            topLabel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            setColumnHeaderView(topLabel);
         }
      }
   }

   private Dimension getTitleComponentPreferredSize() {
      return topLabel != null ? topLabel.getPreferredSize() : new Dimension();
   }

   private static Dimension parseDimension(String s, Dimension d) {
      int arr[] = new int[2];
      int i = 0;

      if (s != null) {
         StringTokenizer st = new StringTokenizer(s, ","); // NOI18N

         while (st.hasMoreElements()) {
            if (i > 1) {
               return d;
            }
            try {
               arr[i] = Integer.parseInt(st.nextToken());
            } catch (NumberFormatException nfe) {
               Exceptions.printStackTrace(nfe);
               return d;
            }
            i++;
         }
      }
      if (i != 2) {
         return d;
      } else {
         return new Dimension(arr[0], arr[1]);
      }
   }
}
