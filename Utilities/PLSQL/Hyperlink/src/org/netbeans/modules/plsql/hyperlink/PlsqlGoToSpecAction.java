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
package org.netbeans.modules.plsql.hyperlink;

import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.hyperlink.target.PlsqlGotoTarget;
import org.netbeans.modules.plsql.hyperlink.target.PlsqlGotoTargetFactory;
import org.netbeans.modules.plsql.hyperlink.target.PlsqlPackageTarget;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import org.netbeans.api.project.Project;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.JumpList;
import org.netbeans.editor.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

@ActionID(id = "org.netbeans.modules.plsql.hyperlink.PlsqlGoToSpecAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlGoToSpecAction")
@ActionReferences({
   @ActionReference(path = "Editors/text/x-plsql/Popup", position = 100),
   @ActionReference(path = "Shortcuts", name = "AS-S")
})
public final class PlsqlGoToSpecAction extends AbstractPlsqlGoToAction {

   @Override
   protected void performAction(final Node[] activatedNodes) {
      final Lookup lookup = activatedNodes[0].getLookup();

      final PlsqlGotoTarget target = PlsqlGotoTargetFactory.instance.getTargetForSpec(lookup);
      //Add to jump list
      final EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
      JumpList.addEntry(Utilities.getFocusedComponent(), PlsqlHyperlinkUtil.getCaretPosition(editorCookie));
      target.gotoSpec();
   }

   @Override
   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   @Override
   public String getName() {
      return NbBundle.getMessage(PlsqlGoToSpecAction.class, "CTL_PlsqlGoToSpecAction");
   }

   @Override
   protected void initialize() {
      super.initialize();
      // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
      putValue("noIconInMenu", Boolean.TRUE);
   }

   /**
    * Open corresponding body file
    * @param packageName
    * @param project
    * @return DataObject
    */
   public DataObject getPackageDataObject(final String packageName, final Project project) {
      final PlsqlGotoTarget target = new PlsqlPackageTarget(packageName, 0, null, project, null);
      return target.getSpec();
   }

   /**
    * Find the line number in the file where the package definition starts
    * @param packageName
    * @param project
    * @return DataObject
    */
   public int getPackageOffset(String packageName, DataObject dataObject) {
      try {
         EditorCookie ec = dataObject.getCookie(EditorCookie.class);
         if (!PlsqlFileUtil.prepareDocument(ec)) {
            return 0;
         }

         if (ec != null) {
            BaseDocument doc = (BaseDocument) ec.getDocument();
            PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(dataObject);
            blockFactory.initHierarchy(ec.getDocument());
            List blockHier = blockFactory.getBlockHierarchy();
            if (blockFactory != null) {
               for (int i = 0; i < blockHier.size(); i++) {
                  PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
                  if (temp.getType() == PACKAGE && temp.getName().equalsIgnoreCase(packageName)) {
                     return Utilities.getLineOffset(doc, temp.getStartOffset());
                  }
               }
            }
         }
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
      return 0;
   }

   /**
    * Open corresponding spec file
    * @param dataObject
    * @return
    */
   @Override
   public boolean goToPackage(String packageName, Project project, String methodName, int lineNumber) {
      try {
         DataObject data = getPackageDataObject(packageName, project);
         if (data == null) {
            return false;
         }
         return goToPackageSpec(data, packageName, lineNumber - 1);
      } catch (NotConnectedToDbException ex) {
         Exceptions.printStackTrace(ex);
      }
      return false;
   }

   /**
    * Open corresponding spec file
    * @param dataObject
    * @return
    */
   private boolean goToPackageSpec(DataObject dataObj, String packageName, int lineNumber) throws NotConnectedToDbException {
      if (dataObj != null) {
         EditorCookie ec = dataObj.getCookie(EditorCookie.class);
         ec.open();
         int offset = 0;

         if (ec != null) {
            PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(dataObj);
            List blockHier = blockFactory.getBlockHierarchy();
            if (blockFactory != null) {
               for (int i = 0; i < blockHier.size(); i++) {
                  PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
                  if (temp.getType() == PACKAGE
                          && temp.getName().equalsIgnoreCase(packageName)) {
                     offset = temp.getStartOffset();
                     break;
                  }
               }
            }
            if (lineNumber >= 0) { //add to the offset - line number is given as a line number in the block - not relative to the file
               BaseDocument doc = (BaseDocument) ec.getDocument();
               try {
                  int firstLine = Utilities.getLineOffset(doc, offset);
                  offset = Utilities.getRowStartFromLineOffset(doc, firstLine + lineNumber);
               } catch (BadLocationException ex) {
                  Exceptions.printStackTrace(ex);
               }
            }

            JEditorPane[] panes = ec.getOpenedPanes();
            if (panes.length > 0) {
               JEditorPane pane = panes[0];
               pane.setCaretPosition(offset);
               return true;
            }
         }
      }
      return false;
   }
}
