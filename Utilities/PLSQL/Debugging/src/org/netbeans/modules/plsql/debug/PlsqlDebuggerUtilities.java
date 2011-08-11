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
package org.netbeans.modules.plsql.debug;

import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.util.Locale;
import org.netbeans.api.db.explorer.DatabaseConnection;

public class PlsqlDebuggerUtilities {

    private PlsqlDebuggerUtilities() {
    }

    /**
     * Returns the Java class name used in the Oracle database to represent the
     * given PL/SQL block in the given schema.
     * @param block the PL/SQL block to debug
     * @param schema the current database schema
     * @return the Java class name, or null if the block is neither contained in
     * a package nor a function/procedure
     */
    public static String getClassName(PlsqlBlock block, DatabaseConnection databaseConnection) {
        DatabaseContentManager cache = DatabaseContentManager.getInstance(databaseConnection);
        PlsqlBlock ancestor = block;
        while (ancestor.getParent() != null) {
            ancestor = ancestor.getParent();
        }
        if (ancestor.getType() == PlsqlBlockType.PACKAGE
                || ancestor.getType() == PlsqlBlockType.PACKAGE_BODY) {
            String name = ancestor.getName().toUpperCase(Locale.ENGLISH);
            String schema = cache != null ? cache.getOwner(name) : null;
            if (schema == null) {
                schema = databaseConnection.getSchema();
            }
            return "$Oracle.PackageBody." + schema + "." + name;
        } else if (block.getType() == PlsqlBlockType.PROCEDURE_DEF
                || block.getType() == PlsqlBlockType.PROCEDURE_IMPL
                || block.getType() == PlsqlBlockType.FUNCTION_DEF
                || block.getType() == PlsqlBlockType.FUNCTION_IMPL) {
            String name = block.getName().toUpperCase(Locale.ENGLISH);
            String schema = cache != null ? cache.getOwner(name) : null;
            if (schema == null) {
                schema = databaseConnection.getSchema();
            }
            return "$Oracle.Procedure." + schema + "." + block.getName().toUpperCase(Locale.ENGLISH);
        }
        return null;
    }
}
