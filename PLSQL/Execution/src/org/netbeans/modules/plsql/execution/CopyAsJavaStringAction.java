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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.util.datatransfer.ExClipboard;

@ActionID(id = "org.netbeans.modules.plsql.execution.CopyAsJavaStringAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_CopyAsJavaStringAction")
@ActionReference(path = "Editors/text/x-plsql/Popup", position = 3995)
public class CopyAsJavaStringAction extends CookieAction {

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    protected Class<?>[] cookieClasses() {
        return new Class[]{DataObject.class, EditorCookie.class};
    }

    @Override
    protected void performAction(Node[] activatedNodes) {
        DataObject dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
        EditorCookie ec = dataObject.getCookie(EditorCookie.class);
        final Document doc = ec.getDocument();
        JEditorPane[] panes = ec.getOpenedPanes();
        String selectedText = "";

        //first check if the user has selected some text
        if ((panes != null) && (panes.length != 0)) {
            selectedText = panes[0].getSelectedText();
        }
        //if no selection then consider the contents of the whole document
        if (selectedText == null || selectedText.equals("")) {
            try {
                selectedText = doc.getText(doc.getStartPosition().getOffset(), doc.getLength());
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        //convert the db stmt to java format
        if (selectedText != null && !selectedText.equals("")) {
            StringBuilder sb = new StringBuilder();
            //perhaps this can be done using a single regexp?
            String[] lines = selectedText.trim().split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].replaceAll("\"", "\\\\\""); //escape any " found in the SQL command
                if (i == 0) { //first line
                    if (i == lines.length - 1) //single line SQL command
                    {
                        sb.append("String dbStmt = \"").append(line.replaceAll("\\s+$", "")).append("\";");
                    } else {
                        sb.append("String dbStmt = \"").append(line.replaceAll("\\s+$", "")).append(" \"\n\t\t");
                    }
                } else if (i == lines.length - 1) { //last line when multiple lines exist
                    sb.append(" +\"").append(line.replaceAll("\\s+$", "")).append("\";");
                } else {
                    sb.append(" +\"").append(line.replaceAll("\\s+$", "")).append(" \"\n\t\t");
                }
            }

            String formattedText = sb.toString();
            Clipboard clipboard = Lookup.getDefault().lookup(ExClipboard.class);
            //should we check for system clipboard as well?
            if (clipboard != null) {
                clipboard.setContents(new StringSelection(formattedText), null);
            }
        }
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(CopyAsJavaStringAction.class, "CTL_CopyAsJavaStringAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean enable(Node[] activatedNodes) {
        return true;
    }

    @Override
    protected boolean asynchronous() {
        return false; //should always be false
    }
}
