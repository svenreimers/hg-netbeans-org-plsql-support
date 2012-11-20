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
package org.netbeans.modules.plsql.annotation.annotations;

import org.netbeans.modules.plsql.annotation.PlsqlAnnotationManager;
import org.netbeans.modules.plsql.annotation.PlsqlAnnotationUtil;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.text.Annotation;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;

/**
 * Common Annotation class for PL/SQL annotations
 */
public abstract class PlsqlAnnotation extends Annotation {

   public static final String GENERAL = "General PL/SQL Rules";
   public static final String IFS_SPECIFIC = "IFS specific PL/SQL Rules";
   public static final int ERROR = 1;
   public static final int WARNING = 2;
   public int offset = 0;
   public int ignoreOffset = -1;
   public int severity = WARNING;
   String category = IFS_SPECIFIC;

   public abstract String getErrorToolTip();

   public abstract Action[] getActions();

   public abstract boolean isIgnoreAlowed();

   public abstract String getIgnoreKey();

   public int getOffset() {
      return offset;
   }

   public void setOffset(int offset) {
      this.offset = offset;
   }

   public int getSeverity() {
      return severity;
   }

   public String getCategory() {
      return category;
   }

   public static int isIgnoreSpecified(Document doc, int offset, String ignoreName, boolean isIgnore) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         ts.move(offset);
         Token<PlsqlTokenId> token = ts.token();
         while (ts.movePrevious()) {
            token = ts.token();
            if (token.id() == PlsqlTokenId.IGNORE_MARKER) {
               if (isIgnore && token.toString().trim().startsWith("--@Ignore" + ignoreName)) {
                  return ts.offset();
               } else if (!isIgnore && token.toString().trim().startsWith("--@" + ignoreName)) {
                  return ts.offset();
               }
            } else if (token.id() != PlsqlTokenId.WHITESPACE) {
               break;
            }
         }
      }
      return -1;
   }

   public class AddIgnoreMarkerAction extends AbstractAction {

      @Override
      public String toString() {
         if (getIgnoreKey().startsWith("Approve")) {
            return "Add an " + getIgnoreKey() + "(date, user)";
         } else {
            return "Add an Ignore" + getIgnoreKey();
         }
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         JTextComponent comp = EditorRegistry.lastFocusedComponent();
         if (comp == null) {
            return;
         }
         Document doc = comp.getDocument();
         if (doc == null) {
            return;
         }
         if (PlsqlAnnotationUtil.isFileReadOnly(doc)) {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "File is read-only", "Error", JOptionPane.ERROR_MESSAGE);
            return;
         }

         PlsqlAnnotationManager annotationManager = PlsqlAnnotationUtil.getAnnotationManager(doc);
         if (annotationManager != null) {
            if (getIgnoreKey().startsWith("Approve") ? addApproveMarker(doc, ignoreOffset) : addMarker(doc, "--@Ignore" + getIgnoreKey() + "\n", ignoreOffset)) {
               annotationManager.removeAnnotation(offset, getAnnotationType());
            }
         }
      }

      private boolean addApproveMarker(Document doc, int ignoreOffset) {
         String key = "--@" + getIgnoreKey() + "(" + getDate() + "," + System.getProperty("user.name") + ")\n";
         return addMarker(doc, key, ignoreOffset);
      }

      private String getDateTime() {
         DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
         Date date = new Date();
         return dateFormat.format(date);
      }
      
      private String getDate() {
         DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
         Date date = new Date();
         return dateFormat.format(date);
      }
   }

   protected boolean addMarker(Document doc, String marker, int ignoreOffset) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         ts.move(ignoreOffset);
         Token<PlsqlTokenId> token = ts.token();
         int offsetToEnter = -1;
         while (ts.movePrevious()) {
            token = ts.token();
            if (token.id() == PlsqlTokenId.WHITESPACE && token.toString().contains("\n")) {
               offsetToEnter = ts.offset() + token.length();
               if (ts.moveNext()) {
                  token = ts.token();
                  if (token.id() == PlsqlTokenId.WHITESPACE) {
                     offsetToEnter = ts.offset();
                  }
               }
               break;
            } else if (token.id() != PlsqlTokenId.WHITESPACE) {
               break;
            }
         }
         try {
            if (offsetToEnter != -1) {
               doc.insertString(offsetToEnter, marker, null);
               return true;
            }
         } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
         }
      }
      return false;
   }
}
