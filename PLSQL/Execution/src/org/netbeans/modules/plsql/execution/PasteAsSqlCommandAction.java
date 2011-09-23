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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplate;
import org.netbeans.lib.editor.codetemplates.api.CodeTemplateManager;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.util.datatransfer.ExClipboard;

public class PasteAsSqlCommandAction extends CookieAction {

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
        Clipboard clipboard = Lookup.getDefault().lookup(ExClipboard.class);
        Transferable trn = clipboard.getContents(null);
        if (trn != null) {
             try {
                    String contents = (String) trn.getTransferData(DataFlavor.stringFlavor);
                    StringBuilder sb = new StringBuilder();
                    String[] lines = contents.trim().split("\n");
                    int paramCount = 0;  //counter for '?' in SQL
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i].replaceAll("\\s+$", ""); //remove trailing space
                        line = line.replace("\\\"" , "\"");  // remove java escape (i.e. \) for double quotes used for column aliases in selects
                        line = line.replaceFirst("^\\s*\\+?\\s*\"", ""); // remove lines starting with " or +"
                        line = line.replaceAll("\"\\s*\\+?$", ""); // remove " and "+ from eol
                        line = line.replaceAll("\"\\s*;+$", ""); // remove "; from eol
                        //replace ? with code template place holders
                        for (;line.indexOf("?")>0;++paramCount){
                          line = line.replaceFirst("\\?", "\\${<value_"+paramCount+">}");
                        }
                        
                        if (line.trim().startsWith("\"")) {
                            line = line.replaceFirst("\"", ""); //preserves spaces at begining
                        }
                        
                        //finally append line to string builder
                        if (i == lines.length - 1) {
                            sb.append(line);
                        } else {
                            sb.append(line).append("\n");
                        }
                    }
                    JEditorPane[] panes = ec.getOpenedPanes();
                    if (panes != null && panes.length > 0) {
                        insert(sb.toString(), panes[0], doc);
                    }

                } catch (UnsupportedFlavorException ex) {
                    Exceptions.printStackTrace(ex);
                } catch (Exception ex) {
                    Exceptions.printStackTrace(ex);
                }
        }
    }
    
    private static void insert(String s, final JTextComponent target, final Document doc) {        
        try {
            //at first, find selected text range
            Caret caret = target.getCaret();
            int p0 = Math.min(caret.getDot(), caret.getMark());
            int p1 = Math.max(caret.getDot(), caret.getMark());
            doc.remove(p0, p1 - p0);            
            CodeTemplate ct = CodeTemplateManager.get(target.getDocument()).createTemporary(s);
            ct.insert(target);              
        } catch (BadLocationException ble) {
            Exceptions.printStackTrace(ble);
        }        
    }

    @Override
    public String getName() {        
        return NbBundle.getMessage(PasteAsSqlCommandAction.class, "CTL_PasteAsSqlCommandAction");
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
