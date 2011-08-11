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

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.db.explorer.ConnectionManager;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.db.explorer.DatabaseException;
import org.netbeans.api.db.explorer.JDBCDriver;
import org.netbeans.api.db.explorer.JDBCDriverManager;
import org.netbeans.spi.project.support.ant.AntProjectHelper;
import org.netbeans.spi.project.support.ant.EditableProperties;
import org.openide.util.Exceptions;

public class DatabasePropertiesPersister extends PropertiesPersister<DatabaseConnectionManager> {

    public static final String CONNECTIONS_KEY = "database.connections";
    public static final String DB_APPOWNER_KEY = "database.appowner";

    public DatabasePropertiesPersister(AntProjectHelper helper, DatabaseConnectionManager manager) {
        super(helper, manager);
        manager.addPropertyChangeListener(new ChangeListener());
    }

    @Override
    protected void loadProperties(EditableProperties properties) {
        List<DatabaseConnection> connections = new ArrayList<DatabaseConnection>();
        String connectionsProperty = properties.getProperty(CONNECTIONS_KEY);
        if (connectionsProperty != null) {
            for (String connectionString : connectionsProperty.split(OBJECT_SEPARATOR)) {
                String[] fields = connectionString.split(FIELD_SEPARATOR);
                if (fields.length == 4) {
                    DatabaseConnection connection = null;
                    for (DatabaseConnection c : ConnectionManager.getDefault().getConnections()) {
                        if (c.getDatabaseURL().equalsIgnoreCase(fields[0]) &&
                                c.getUser().equalsIgnoreCase(fields[1]) &&
                                c.getSchema().equalsIgnoreCase(fields[3])) {
                            connection = c;
                        }
                    }
                    if (connection == null) {
                        JDBCDriver driver = null;
                        for (JDBCDriver d : JDBCDriverManager.getDefault().getDrivers()) {
                            if (d.getClassName().equals(DatabaseConnectionManager.ORACLE_DRIVER_CLASS_NAME)) {
                                driver = d;
                            }
                        }
                        connection = DatabaseConnection.create(driver, fields[0], fields[1], fields[3], fields[2], true);
                        try {
                            ConnectionManager.getDefault().addConnection(connection);
                        } catch (DatabaseException e) {
                            Exceptions.printStackTrace(e);
                            continue;
                        }
                    }
                    connections.add(connection);
                }
            }
        }
        manager.setDatabaseConnections(connections.toArray(new DatabaseConnection[connections.size()]));
    }

    @Override
    protected void storeProperties(EditableProperties properties) {
        DatabaseConnection[] connections = manager.getDatabaseConnections();
        String[] connectionStrings = new String[connections.length];        
        for (int i = 0; i < connections.length; i++) {
            StringBuilder builder = new StringBuilder();
            DatabaseConnection connection = connections[i];
            if (connection.getDatabaseURL() != null) {
                builder.append(connection.getDatabaseURL());
            }
            builder.append(FIELD_SEPARATOR);
            if (connection.getUser() != null) {
                builder.append(connection.getUser());
            }
            builder.append(FIELD_SEPARATOR);
            if (connection.getPassword() != null) {
                builder.append(connection.getPassword());
            }
            builder.append(FIELD_SEPARATOR);
            if (connection.getSchema() != null) {
                builder.append(connection.getSchema());
            }
            connectionStrings[i] = builder.toString();            
        }
        setProperty(properties, CONNECTIONS_KEY, connectionStrings);
        //get appowner of main DB (i.e. the first connection)
        String appowner = "";
        if (connections.length > 0){ 
            appowner = connections[0].getSchema();
        }
        setProperty(properties, DB_APPOWNER_KEY, appowner);
    }
}
