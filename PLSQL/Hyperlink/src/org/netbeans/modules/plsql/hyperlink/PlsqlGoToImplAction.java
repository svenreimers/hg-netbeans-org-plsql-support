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
import org.netbeans.modules.plsql.hyperlink.target.PlsqlGotoTarget;
import org.netbeans.modules.plsql.hyperlink.target.PlsqlGotoTargetFactory;
import org.netbeans.api.project.Project;
import org.netbeans.editor.JumpList;
import org.netbeans.editor.Utilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

@ActionID(id = "org.netbeans.modules.plsql.hyperlink.PlsqlGoToImplAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlGoToImplAction")
@ActionReferences({
   @ActionReference(path = "Editors/text/x-plsql/Popup", position = 110),
   @ActionReference(path = "Shortcuts", name = "AS-I")
})
public final class PlsqlGoToImplAction extends AbstractPlsqlGoToAction {

   @Override
   protected void performAction(final Node[] activatedNodes) {
      final Lookup lookup = activatedNodes[0].getLookup();

      final PlsqlGotoTarget target = PlsqlGotoTargetFactory.instance.getTargetForBody(lookup);

      //Add to jump list
      final EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
      JumpList.addEntry(Utilities.getFocusedComponent(), PlsqlHyperlinkUtil.getCaretPosition(editorCookie));
      target.gotoBody();
   }

   @Override
   public String getName() {
      return NbBundle.getMessage(AbstractPlsqlGoToAction.class, "CTL_PlsqlGoToImplAction");
   }

   @Override
   public boolean goToPackage(final String packageName, final Project project, final String methodName, final int lineNumber) {
      return goToPackageImpl(packageName, project, methodName, lineNumber, false);
   }
}
