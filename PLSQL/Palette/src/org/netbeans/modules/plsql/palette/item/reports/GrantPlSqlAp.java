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
package org.netbeans.modules.plsql.palette.item.reports;

import org.netbeans.modules.plsql.palette.PaletteItem;

/*
 * Class description
 *
 * Created on January 23, 2012, 6:11 PM
 *
 * @author IFS
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
public class GrantPlSqlAp extends PaletteItem{

    private final String grantPlSqlApBlock =  "DECLARE \n"
                                             +"   acl_ VARCHAR2(100) := 'IFSAPP-PLSQLAP-Permission-${machine.name}.xml'; \n"
                                             +"   privilege_ VARCHAR2(20) := 'connect'; \n"
                                             +"   desc_ VARCHAR2(100) := 'Permission for Foundation1 users to run HTTP from the database.'; \n"
                                             +"BEGIN \n"
                                             +"   Dbms_Network_Acl_Admin.Create_Acl( \n"
                                             +"      acl_, \n"
                                             +"      desc_, \n"
                                             +"      'IFSAPP', \n"
                                             +"      TRUE, \n"
                                             +"      privilege_); \n"
                                             +"   Dbms_Network_Acl_Admin.Add_Privilege( \n"
                                             +"      acl_, \n"
                                             +"      'IFSSYS', \n"
                                             +"      TRUE, \n"
                                             +"      privilege_); \n"
                                             +"   Dbms_Network_Acl_Admin.Assign_Acl( \n"
                                             +"      acl_, \n"
                                             +"      '${machine.name}.corpnet.ifsworld.com', \n"
                                             +"      8080, \n"
                                             +"      8080); \n"
                                             +"END;";
    
    @Override
    public String createBody() {
        return grantPlSqlApBlock;
    }
    
}
