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
package org.netbeans.modules.plsql.execution;

import org.netbeans.modules.plsql.filetype.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import javax.swing.JFileChooser;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlSaveCommandAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlSaveAsAction")
public final class PlsqlSaveCommandAction extends CookieAction {

    @Override
    protected void performAction(Node[] activatedNodes) {
        try {
            DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
            if (dataObject == null) {
                return;
            }

            PlsqlEditorSupport editorSupport = dataObject.getCookie(PlsqlEditorSupport.class);
            if (editorSupport == null) {
                return;
            }

            //Select folder
            JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(false);
            fc.setDialogTitle(NbBundle.getMessage(PlsqlSaveCommandAction.class, "CTL_PlsqlSaveAsAction"));
            fc.setApproveButtonText("Save");
            fc.setSelectedFile(new File("Command.sql"));
            File selected = null;
            int returnVal = fc.showOpenDialog(null);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selected = fc.getSelectedFile();
            } else {
                return;
            }

            //Write the contents to the new file
            FileObject newFile = FileUtil.createData(selected);
            Document doc = editorSupport.getDocument();
            String txt = doc.getText(doc.getStartPosition().getOffset(), doc.getLength());
            OutputStream output = newFile.getOutputStream();
            OutputStreamWriter osWriter = new OutputStreamWriter(output);
            osWriter.write(txt, 0, txt.length());
            osWriter.flush();
            osWriter.close();
            output.close();
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(PlsqlSaveCommandAction.class, "CTL_PlsqlSaveAsAction");
    }

    @Override
    protected Class[] cookieClasses() {
        return new Class[]{DataObject.class};
    }

    @Override
    protected String iconResource() {
        return "org/netbeans/modules/plsql/execution/saveAs.png";
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (!super.enable(activatedNodes)) {
            return false;
        }
        return activatedNodes[0].getLookup().lookup(DataObject.class).getPrimaryFile().getNameExt().endsWith(".tdb");  //Temp database file
    }
}
