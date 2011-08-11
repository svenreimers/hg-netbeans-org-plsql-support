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
package org.netbeans.modules.plsql.completion;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.Locale;
import javax.swing.ImageIcon;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.completion.Completion;
import org.netbeans.editor.BaseDocument;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.netbeans.spi.editor.completion.support.CompletionUtilities;
import org.openide.ErrorManager;
import org.openide.util.ImageUtilities;

/**
 *
 * @author chawlk
 */
public class PlsqlCodeCompletionItem implements CompletionItem {

   private static Color fieldColor = Color.decode("0x0000B2");
   private static final ImageIcon tableIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/table.png"));
   private static final ImageIcon viewIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/view.png"));
   private static final ImageIcon columnIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/column.png"));
   private static final ImageIcon packageIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/package.png"));
   private static final ImageIcon sequenceIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/sequence.png"));
   private static final ImageIcon privateMethodIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/private_method.png"));
   private static final ImageIcon protectedMethodIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/protected_method.png"));
   private static final ImageIcon publicMethodIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/public_method.png"));
   private static final ImageIcon implementationMethodIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/implementation_method.png"));
   private static final ImageIcon cursorIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/cursor.png"));
   private static final ImageIcon typeIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/type.png"));
   private static final ImageIcon variableIcon = new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/completion/resources/variable.png"));
   private String text;
   private CompletionItemType type;
   private String documentation;
   private String alternativeType = null;

   /** Creates a new instance of PLSQLCodeCompletionItem */
   public PlsqlCodeCompletionItem(String text, CompletionItemType type) {
      this.setText(text);
      this.type = type;
   }

   protected Color getFielColor() {
      return fieldColor;
   }

   protected ImageIcon getIcon() {
      if (type == CompletionItemType.TABLE) {
         return tableIcon;
      } else if (type == CompletionItemType.VIEW) {
         return viewIcon;
      } else if (type == CompletionItemType.COLUMN) {
         return columnIcon;
      } else if (type == CompletionItemType.SEQUENCE) {
         return sequenceIcon;
      } else if (type == CompletionItemType.PACKAGE) {
         return packageIcon;
      } else if (type == CompletionItemType.CURSOR) {
         return cursorIcon;
      } else if (type == CompletionItemType.TYPE) {
         return typeIcon;
      } else if (type == CompletionItemType.VARIABLE || type == CompletionItemType.PARAMETER || type == CompletionItemType.CONSTANT) {
         return variableIcon;
      } else if (type == CompletionItemType.FUNCTION || type == CompletionItemType.PROCEDURE) {
         if (text.endsWith("___")) {
            return implementationMethodIcon;
         } else if (text.endsWith("__")) {
            return privateMethodIcon;
         } else if (text.endsWith("__")) {
            return protectedMethodIcon;
         } else {
            return publicMethodIcon;
         }
      }
      return null;
   }

   protected void doSubstitute(final JTextComponent component) {
      final BaseDocument doc = (BaseDocument) component.getDocument();
      doc.runAtomic(new Runnable() {

         public void run() {
            try {
               int start = component.getSelectionStart();
               String filterWord = PlsqlCodeCompletionProvider.getFilterWord();
               int filterWordLength = (filterWord == null ? 0 : filterWord.length());
               int pos = start - filterWordLength;
               String substitutionValue = text;
               if(text.startsWith("\"") && doc.getChars(pos-1, 1)[0]=='"') { //beginning of String already there
                  substitutionValue = text.substring(1); 
               }
               doc.remove(pos, filterWordLength + component.getSelectionEnd() - start);
               doc.insertString(pos, substitutionValue, null);
               component.setCaretPosition(component.getCaretPosition());
            } catch (BadLocationException e) {
               ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
         }
      });
   }

   public void defaultAction(JTextComponent component) {
      doSubstitute(component);
      Completion.get().hideAll();
   }

   public void processKeyEvent(KeyEvent keyEvent) {
   }

   public int getPreferredWidth(Graphics graphics, Font font) {
      return CompletionUtilities.getPreferredWidth(getText(), type.toString(), graphics, font);
   }

   public void render(Graphics graphics, Font font, Color color, Color color0, int width, int height, boolean selected) {
      CompletionUtilities.renderHtml(getIcon(), text, type.toString(), graphics, font,
              (selected ? Color.white : fieldColor), width, height, selected);
   }

   public CompletionTask createToolTipTask() {
      return null;
   }

   public boolean instantSubstitution(JTextComponent component) {
      //doSubstitute(component);
      return false;
   }

   public int getSortPriority() {
      if (type == CompletionItemType.VARIABLE  || type == CompletionItemType.PARAMETER || type == CompletionItemType.CURSOR || type == CompletionItemType.CONSTANT || type == CompletionItemType.EXCEPTION) {
         return 2;
      }

      return 4;
   }

   public CharSequence getSortText() {
      return getText().toUpperCase(Locale.ENGLISH);
   }

   public CharSequence getInsertPrefix() {
      return getText();
   }

   public CompletionItemType getType() {
      return type;
   }

   public String getText() {
      return text.startsWith("\"") ? text.substring(1, text.length()-1) : text;
   }

   public void setText(String text) {
      this.text = text;
   }

   public String getDocumentation() {
      return documentation;
   }

   public void setDocumentation(String documentation) {
      this.documentation = documentation;
   }

   public int hashCode() {
      return getText().hashCode();
   }

   public CompletionTask createDocumentationTask() {
      if (documentation == null) {
         return null;
      }

      return new AsyncCompletionTask(new AsyncCompletionQuery() {

         protected void query(CompletionResultSet completionResultSet, Document document, int i) {
            completionResultSet.setDocumentation(new PlsqlCodeCompletionItemDocumentation(PlsqlCodeCompletionItem.this));
            completionResultSet.finish();
         }
      });

   }
}
