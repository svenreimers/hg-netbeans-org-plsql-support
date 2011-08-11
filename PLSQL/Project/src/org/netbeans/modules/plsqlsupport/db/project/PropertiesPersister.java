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
package org.netbeans.modules.plsqlsupport.db.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.util.Exceptions;

public abstract class PropertiesPersister<T> {

    protected static final String OBJECT_SEPARATOR = ";";
    protected static final String FIELD_SEPARATOR = ",";
    protected T manager;
    private AntProjectHelper helper;

    public PropertiesPersister(AntProjectHelper helper, T manager) {
        this.helper = helper;
        this.manager = manager;
        loadProperties(helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH));
    }

    protected abstract void loadProperties(EditableProperties properties);

    protected abstract void storeProperties(EditableProperties properties);

    protected static void setProperty(EditableProperties properties, String key, String value) {
        if (value != null) {
            properties.setProperty(key, value);
        } else {
            properties.remove(key);
        }
    }

    protected static void setProperty(EditableProperties properties, String key, String[] values) {
        if (values.length > 0) {
            String[] separatedValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                separatedValues[i] = values[i];
                if (i < values.length - 1) {
                    separatedValues[i] += OBJECT_SEPARATOR;
                }
            }
            properties.setProperty(key, separatedValues);
        } else {
            properties.remove(key);
        }
    }

    protected class ChangeListener implements PropertyChangeListener {

        public ChangeListener() {
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            EditableProperties properties = helper.getProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH);
            storeProperties(properties);
            helper.putProperties(AntProjectHelper.PROJECT_PROPERTIES_PATH, properties);
        }
    }

    protected class SavingChangeListener extends ChangeListener {

        public SavingChangeListener() {
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            super.propertyChange(event);
            try {
                ProjectManager.getDefault().saveProject(FileOwnerQuery.getOwner(helper.getProjectDirectory()));
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } catch (IllegalArgumentException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
