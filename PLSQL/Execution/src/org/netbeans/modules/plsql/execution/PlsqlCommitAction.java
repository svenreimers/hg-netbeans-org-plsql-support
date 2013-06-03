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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Locale;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.openide.awt.ActionID;
import org.openide.awt.ActionRegistration;
import org.openide.awt.DropDownButtonFactory;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.loaders.DataObject;
import org.openide.util.*;
import org.openide.util.actions.Presenter;

@ActionID(id = "org.netbeans.modules.plsql.execution.PlsqlCommitAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_PlsqlCommit", lazy = false)
public class PlsqlCommitAction extends AbstractAction implements ContextAwareAction, Presenter.Toolbar {

    private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
    private final DataObject dataObject;
    private DatabaseConnectionMediator mediator;
    private JButton button;
    private final PropertyChangeListener changeListener = new EnableCommit();

    public PlsqlCommitAction() {
        this(Utilities.actionsGlobalContext());
    }

    public PlsqlCommitAction(Lookup context) {
        putValue(SHORT_DESCRIPTION, NbBundle.getMessage(PlsqlCommitAction.class, "CTL_PlsqlCommit"));
        putValue(SMALL_ICON, new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/execution/database_commit.png")));

        dataObject = context.lookup(DataObject.class);
        if (dataObject != null) {
            mediator = dataObject.getLookup().lookup(DatabaseConnectionMediator.class);
        }
    }

    @Override
    public boolean isEnabled() {
        //Enable execution for .spec .body files in workspace (copied using 'Copy to Workspace Folder')
        if (dataObject != null) {
            if (validator.isValidPackageDefault(dataObject) || dataObject.getPrimaryFile().getExt().toLowerCase(Locale.ENGLISH).equals("db")) {
                if (!dataObject.getPrimaryFile().canWrite()) {
                    return false;
                }
            }
            if (!validator.isValidTDB(dataObject)) {
                return false;
            }

            if (dataObject.getLookup().lookup(EditorCookie.class) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Action createContextAwareInstance(Lookup context) {
        return new PlsqlCommitAction(context);
    }

    private void prepareConnection() {
        if (dataObject != null) {
            mediator = dataObject.getLookup().lookup(DatabaseConnectionMediator.class);
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        prepareConnection();

        saveIfModified(dataObject);
        mediator.commitTransaction();
    }

    @Override
    public Component getToolbarPresenter() {
        if (!isEnabled()) {
            return null;
        }
        button = DropDownButtonFactory.createDropDownButton(
                new ImageIcon(new BufferedImage(32, 32, BufferedImage.TYPE_BYTE_GRAY)), null);
        button.setAction(this);
        button.setEnabled(false);
        button.setDisabledIcon(new ImageIcon(ImageUtilities.loadImage("org/netbeans/modules/plsql/execution/database_commit_disable.png")));
        mediator.addTransactionListener(changeListener);
        return button;
    }

    private void saveIfModified(DataObject dataObj) {
        try {
            SaveCookie saveCookie = dataObj.getLookup().lookup(SaveCookie.class);
            if (saveCookie != null) {
                saveCookie.save();
            }
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private class EnableCommit implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (!OptionsUtilities.isCommandWindowAutoCommitEnabled()) {
                button.setEnabled((Boolean) event.getNewValue());
            }
        }
    }
}
