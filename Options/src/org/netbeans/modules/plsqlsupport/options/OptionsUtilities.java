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
package org.netbeans.modules.plsqlsupport.options;

import org.openide.util.NbPreferences;

public class OptionsUtilities {

   public static final String AUTO_DEPLOY_ALL_DATABASES_KEY = "auto.deploy.all.databases";
   public static final String PLSQL_ANNOTATIONS_ENABLED_KEY = "plsql.annotations.enabled";
   public static final String PLSQL_EXPAND_FOLDS_KEY = "plsql.folds.expand";
   public static final String COMMAND_WINDOW_AUTO_COMMIT_KEY = "command.window.auto.commit";
   public static final String COMMAND_WINDOW_AUTO_SELECT_KEY = "command.window.auto.select";
   public static final String PLSQL_ANNOTATIONS_GENERAL_KEY = "plsql.annotations.general";
   public static final String PLSQL_ANNOTATIONS_IFS_KEY = "plsql.annotations.ifs";
   public static final String PLSQL_ANNOTATIONS_CURSOR_WHERE_KEY = "plsql.annotations.cursor.where";
   public static final String PLSQL_ANNOTATIONS_FUNCTION_RETURN_KEY = "plsql.annotations.function.return";
   public static final String PLSQL_ANNOTATIONS_IF_NULL_KEY = "plsql.annotations.if.null";
   public static final String PLSQL_ANNOTATIONS_MISSING_END_NAME_KEY = "plsql.annotations.missing.endname";
   public static final String PLSQL_ANNOTATIONS_UNREACHABLE_KEY = "plsql.annotations.unreachable";
   public static final String PLSQL_ANNOTATIONS_WRONG_END_NAME_KEY = "plsql.annotations.wrong.endname";
   public static final String PLSQL_ANNOTATIONS_WRONG_FUNC_PARAM_KEY = "plsql.annotations.wrong.func.param";
   public static final String PLSQL_ANNOTATIONS_WRONG_PARAM_ORDER_KEY = "plsql.annotations.wrong.param.order";

   private OptionsUtilities() {
   } //singleton

   public static boolean isDeployNoPromptEnabled() {
      return !NbPreferences.forModule(GeneralPanel.class).getBoolean(AUTO_DEPLOY_ALL_DATABASES_KEY, true);
   }

   public static boolean isPlSqlAnnotationsEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_ENABLED_KEY, true);
   }

   public static boolean isPlSqlExpandFolds() {
      return NbPreferences.forModule(GeneralPanel.class).getBoolean(PLSQL_EXPAND_FOLDS_KEY, false);
   }

   public static boolean isCommandWindowAutoCommitEnabled() {
      return NbPreferences.forModule(GeneralPanel.class).getBoolean(COMMAND_WINDOW_AUTO_COMMIT_KEY, false);
   }
   
   public static boolean isCommandWindowAutoSelectEnabled() {
      return NbPreferences.forModule(GeneralPanel.class).getBoolean(COMMAND_WINDOW_AUTO_SELECT_KEY, false);
   }

   public static boolean isPlSqlAnnotationCursorWhereEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_CURSOR_WHERE_KEY, true);
   }

   public static boolean isPlSqlAnnotationFunctionReturnEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_FUNCTION_RETURN_KEY, true);
   }

   public static boolean isPlSqlAnnotationIfNullEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_IF_NULL_KEY, true);
   }

   public static boolean isPlSqlAnnotationMissingEndNameEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_MISSING_END_NAME_KEY, true);
   }

   public static boolean isPlSqlAnnotationUnreachableEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_UNREACHABLE_KEY, true);
   }

   public static boolean isPlSqlAnnotationWrongEndNameEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_WRONG_END_NAME_KEY, true);
   }

   public static boolean isPlSqlAnnotationWrongFuncParamEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_WRONG_FUNC_PARAM_KEY, true);
   }

   public static boolean isPlSqlAnnotationWrongParamOrderEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(PLSQL_ANNOTATIONS_WRONG_PARAM_ORDER_KEY, true);
   }
}
