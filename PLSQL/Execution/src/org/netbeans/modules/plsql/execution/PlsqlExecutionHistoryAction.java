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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;

import org.netbeans.modules.plsql.filetype.StatementExecutionHistory;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DropDownButtonFactory;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlExecutionHistoryAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlExecutionHistoryAction")
public final class PlsqlExecutionHistoryAction extends AbstractAction implements ContextAwareAction, Presenter.Toolbar {

    private JPopupMenu popup;
    private JButton button;
    private ActionListener buttonListener = new ButtonListener();
    private DataObject dataObject;

    public PlsqlExecutionHistoryAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PlsqlExecutionHistoryAction(Lookup context) {
        putValue(SHORT_DESCRIPTION, getName());
        putValue(SMALL_ICON, new ImageIcon(ImageUtilities.loadImage(iconResource())));

        dataObject = context.lookup(DataObject.class);

        setEnabled(dataObject != null && dataObject.getPrimaryFile().getNameExt().startsWith(SQLCommandWindow.SQL_EXECUTION_FILE_PREFIX));
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PlsqlExecutionHistoryAction(context);
    }

    private String getName() {
        return NbBundle.getMessage(PlsqlExecutionHistoryAction.class, "CTL_PlsqlExecutionHistoryAction");
    }

    private String iconResource() {
        return "org/netbeans/modules/plsql/execution/history.png";
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    public Component getToolbarPresenter() {
        if (!isEnabled()) {
            return null;
        }

        popup = new JPopupMenu();
        populatePopupMenu();

        button = DropDownButtonFactory.createDropDownButton(
                new ImageIcon(new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY)), popup);
        button.setAction(this);
        button.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    popup.show(button, 0, button.getHeight());
                }
            }
        });

        popup.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                button.setSelected(false);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                button.setSelected(false);
            }

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                populatePopupMenu();
            }
        });

        return button;
    }

    private void populatePopupMenu() {
        popup.removeAll();
        if (dataObject != null) {
            StatementExecutionHistory history = dataObject.getLookup().lookup(StatementExecutionHistory.class);
            List<String> statements = history != null ? history.getStatementHistory() : new ArrayList<String>();
            JMenuItem item = new JMenuItem("Clear History");
            item.addActionListener(buttonListener);
            popup.add(item);
            popup.addSeparator();
            for (String statement : statements) {
                String text = statement.length() > 40 ? statement.substring(0, 40) + "..." : statement;
                item = new JMenuItem(text.replaceAll("\n", " "));
                item.putClientProperty("STATEMENT", statement);
                item.addActionListener(buttonListener);
                item.setToolTipText("<html>" + statement.trim().replaceAll("\n", "<br>"));
                popup.add(item);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        StatementExecutionHistory history = dataObject.getLookup().lookup(StatementExecutionHistory.class);
        List<String> statements = history != null ? history.getStatementHistory() : new ArrayList<String>();
        InputOutput io = IOProvider.getDefault().getIO("Execution History", true);
        io.select();
        OutputWriter out = io.getOut();
        out.println(">>> Execution History: <<<");
        for (int i = 0; i < statements.size(); i++) {
            out.println(statements.get(i).trim());
            out.println("===");
        }
        out.println(">>> End of history list <<<");
        out.close();
    }

    private class ButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JMenuItem item = (JMenuItem) e.getSource();
            String statement = (String) item.getClientProperty("STATEMENT");
            if (statement == null) { //Clear history
                StatementExecutionHistory history = dataObject.getLookup().lookup(StatementExecutionHistory.class);
                if (history != null) {
                    history.clear();
                }
            } else {
                StyledDocument document = dataObject.getLookup().lookup(EditorCookie.class).getDocument();
                try {
                    document.remove(0, document.getLength());
                    document.insertString(0, statement, null);
                } catch (BadLocationException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    };
}
