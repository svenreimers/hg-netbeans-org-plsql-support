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
package org.netbeans.modules.plsql.format.comment;

import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

public final class PlsqlCommentAction extends CookieAction {

   protected void performAction(Node[] activatedNodes) {
      try {   
         DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
         String contentType = dataObject.getLoader().getRepresentationClassName().trim();
         
         if (contentType.equals("org.netbeans.modules.plsql.filetype.PlsqlDataObject")) {
            JTextComponent component = Utilities.getFocusedComponent();            
            commentLines(component);
         } else 
            return;
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
   }

   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   public String getName() {
      return NbBundle.getMessage(PlsqlCommentAction.class, "CTL_CommentAction");
   }

   protected Class[] cookieClasses() {
      return new Class[]{EditorCookie.class};
   }

   @Override
   protected String iconResource() {
      return "org/netbeans/modules/plsql/format/comment/resources/comment.png";
   }

   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   @Override
   protected boolean asynchronous() {
      return false;
   }
   
   /**
    * Method that will comment lines in the selected area
    * @param target
    * @throws javax.swing.text.BadLocationException
    */
   public void commentLines(final JTextComponent target)
    throws BadLocationException {           
        final Document doc = target.getDocument();
        if (doc == null)
            
            return;
        
        if (doc instanceof BaseDocument)
            ((BaseDocument)doc).runAtomic(new Runnable() {

            public void run() {
                commentLines(target, doc);
            }
        });
    }

    /**
    * Method that will actually perform the commenting
    * @param target
    * @param doc
    */
   private void commentLines(JTextComponent target, Document doc) {
      int lineStart = -1;
        try {
            //at first, find selected text range
            Caret caret = target.getCaret();
            int currentDot = caret.getDot();
            int currentMark = caret.getMark();
            int start = Math.min(currentDot, currentMark);
            int end = Math.max(currentDot, currentMark);
            int docLength = doc.getLength();            
                              
            if (start == end) {//No selection
               if (docLength == end) //If we are going to the EOF ignore that
                  return;
               
               lineStart = Utilities.getRowStart(target, end);
               doc.insertString(lineStart, "--", null);
               
            } else {         
               lineStart = Utilities.getRowStart(target, start);
               target.setSelectionStart(lineStart);
               String originalText = target.getSelectedText();
               String text = "--" + originalText.substring(0, originalText.length()-1).replaceAll("\n", "\n--") + originalText.substring(originalText.length()-1);              
               target.replaceSelection(text);
               if(start!=lineStart) {
                  start=start+2;
               } 
               if(currentDot<currentMark) {
                  caret.setDot(lineStart+text.length());
                  caret.moveDot(start);
               } else {
                  caret.setDot(start);
                  caret.moveDot(lineStart+text.length());
               }
            }            
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
   }
}

