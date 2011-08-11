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
package org.netbeans.modules.plsql.utilities;

import java.io.File;

/**
 *
 * @author chrlse
 */
public class TestUtils {

   public static final String PROJECT_A = "proj_a";
   public static final String PROJECT_B = "proj_b";
   public static final String WORKSPACE_A = PROJECT_A + File.separator + "workspace";
   public static final String WORKSPACE_B = PROJECT_B + File.separator + "workspace";
   public static final String FNDADM = "fndadm";
   public static final String FNDADM_PATH = File.separator + FNDADM
           + File.separator + "database" + File.separator + FNDADM + File.separator;
   public static final String SECURITYCHECKPOINTSTATE_API = FNDADM_PATH + "SecurityCheckpointState.api";
   public static final String SECURITYCHECKPOINTSTATE_APY = FNDADM_PATH + "SecurityCheckpointState.apy";
   public static final String FNDBAS = "fndbas";
   public static final String FNDBAS_PATH = File.separator + FNDBAS
           + File.separator + "database" + File.separator + FNDBAS + File.separator;
   public static final String MODULE_RDF = "Module.rdf";
   public static final String MODULE_RDF_PATH = FNDBAS_PATH + MODULE_RDF;
   public static final String CLIENT_APY = "Client.apy";
   public static final String CLIENT_APY_PATH = FNDBAS_PATH + MODULE_RDF;
   public static final String FNDSECURITYPERUSER_RDF = FNDBAS_PATH + "FndSecurityPerUser.rdf";
   public static final String FNDUSER_SPEC_PATH = FNDBAS_PATH + "FndUser.spec";
   public static final String FNDUSER_BODY_PATH = FNDBAS_PATH + "FndUser.body";
   public static final String ACTOR_GEN_APY_PATH = FNDBAS_PATH + File.separator + ".ifs" + File.separator + "Actor.gen.apy";
//   public static final File SECURITYCHECKPOINTSTATE_API_FILE = new File(SECURITYCHECKPOINTSTATE_API);
//   public static final File SECURITYCHECKPOINTSTATE_APY_FILE = new File(SECURITYCHECKPOINTSTATE_APY);
}
