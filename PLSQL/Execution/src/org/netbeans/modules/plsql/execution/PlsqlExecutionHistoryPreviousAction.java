/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import org.netbeans.modules.plsql.filetype.StatementExecutionHistory;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.*;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlExecutionHistoryPreviousAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlExecutionHistoryPreviousAction")
@ActionReference(path = "Shortcuts", name = "AS-P")
public final class PlsqlExecutionHistoryPreviousAction extends AbstractAction implements ContextAwareAction {

    private DataObject dataObject;

    public PlsqlExecutionHistoryPreviousAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PlsqlExecutionHistoryPreviousAction(Lookup context) {
        putValue(NAME, getName());
        putValue(SHORT_DESCRIPTION, getName());
        dataObject = context.lookup(DataObject.class);
        setEnabled(dataObject != null && dataObject.getPrimaryFile().getNameExt().startsWith(SQLCommandWindow.SQL_EXECUTION_FILE_PREFIX));
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PlsqlExecutionHistoryPreviousAction(context);
    }

    private String getName() {
        return NbBundle.getMessage(PlsqlExecutionHistoryPreviousAction.class, "CTL_PlsqlExecutionHistoryPreviousAction");
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StatementExecutionHistory history = dataObject.getLookup().lookup(StatementExecutionHistory.class);
        if (history != null && history.getSize() > 0) {
            boolean moveOk = history.movePrevious();
            StyledDocument document = dataObject.getLookup().lookup(EditorCookie.class).getDocument();
            try {
                if (moveOk) {
                    document.remove(0, document.getLength());
                    document.insertString(0, history.getSelectedEntry(), null);
                }
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
