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

public class IfsOptionsUtilities {

   public static final String auto_deploy_all_databases_key = "auto.deploy.all.databases";
   public static final String plsql_annotations_enabled_key = "plsql.annotations.enabled";
   public static final String plsql_expand_folds_key = "plsql.folds.expand";
   public static final String command_window_auto_commit_key = "command.window.auto.commit";
   public static final String plsql_annotations_general_key = "plsql.annotations.general";
   public static final String plsql_annotations_ifs_key = "plsql.annotations.ifs";
   public static final String plsql_annotations_comment_ref_key = "plsql.annotations.comment.ref";
   public static final String plsql_annotations_comments_key = "plsql.annotations.comments";
   public static final String plsql_annotations_cursor_where_key = "plsql.annotations.cursor.where";
   public static final String plsql_annotations_function_return_key = "plsql.annotations.function.return";
   public static final String plsql_annotations_generated_method_key = "plsql.annotations.generated.method";
   public static final String plsql_annotations_global_variable_key = "plsql.annotations.global.variable";
   public static final String plsql_annotations_if_null_key = "plsql.annotations.if.null";
   public static final String plsql_annotations_missing_end_name_key = "plsql.annotations.missing.endname";
   public static final String plsql_annotations_missing_lu_dec_key = "plsql.annotations.missing.ludec";
   public static final String plsql_annotations_missing_method_key = "plsql.annotations.missing.method";
   public static final String plsql_annotations_missing_module_dec_key = "plsql.annotations.missing.moduledec";
   public static final String plsql_annotations_missing_sysinit_key = "plsql.annotations.missing.sysinit";
   public static final String plsql_annotations_same_tag_key = "plsql.annotations.sametag";
   public static final String plsql_annotations_statements_key = "plsql.annotations.statements";
   public static final String plsql_annotations_table_or_view_key = "plsql.annotations.tableorview";
   public static final String plsql_annotations_unique_tag_key = "plsql.annotations.uniquetag";
   public static final String plsql_annotations_unreachable_key = "plsql.annotations.unreachable";
   public static final String plsql_annotations_wrong_end_name_key = "plsql.annotations.wrong.endname";
   public static final String plsql_annotations_wrong_func_param_key = "plsql.annotations.wrong.func.param";
   public static final String plsql_annotations_wrong_init_param_key = "plsql.annotations.wrong.initparam";
   public static final String plsql_annotations_wrong_lu_name_key = "plsql.annotations.wrong.luname";
   public static final String plsql_annotations_wrong_module_key = "plsql.annotations.wrong.module";
   public static final String plsql_annotations_wrong_param_order_key = "plsql.annotations.wrong.param.order";

   private IfsOptionsUtilities() {
   } //singleton     

   public static boolean isDeployNoPromptEnabled() {
      return !NbPreferences.forModule(GeneralPanel.class).getBoolean(auto_deploy_all_databases_key, true);
   }

   public static boolean isPlSqlAnnotationsEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_enabled_key, true);
   }

   public static boolean isPlSqlExpandFolds() {
      return NbPreferences.forModule(GeneralPanel.class).getBoolean(plsql_expand_folds_key, false);
   }

   public static boolean isCommandWindowAutoCommitEnabled() {
      return NbPreferences.forModule(GeneralPanel.class).getBoolean(command_window_auto_commit_key, true);
   }

   public static boolean isPlsqlAnnotationCommentRefEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_comment_ref_key, true);
   }

   public static boolean isPlSqlAnnotationCommentsEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_comments_key, true);
   }

   public static boolean isPlSqlAnnotationCursorWhereEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_cursor_where_key, true);
   }

   public static boolean isPlSqlAnnotationFunctionReturnEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_function_return_key, true);
   }

   public static boolean isPlSqlAnnotationGeneratedMethodEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_generated_method_key, true);
   }

   public static boolean isPlSqlAnnotationGlobalVariableEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_global_variable_key, true);
   }

   public static boolean isPlSqlAnnotationIfNullEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_if_null_key, true);
   }

   public static boolean isPlSqlAnnotationMissingEndNameEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_missing_end_name_key, true);
   }

   public static boolean isPlSqlAnnotationMissingLuDecEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_missing_lu_dec_key, true);
   }

   public static boolean isPlSqlAnnotationMissingMethodEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_missing_method_key, true);
   }

   public static boolean isPlSqlAnnotationMissingModuleDecEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_missing_module_dec_key, true);
   }

   public static boolean isPlSqlAnnotationMissingSysInitEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_missing_sysinit_key, true);
   }

   public static boolean isPlSqlAnnotationSameTagEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_same_tag_key, true);
   }

   public static boolean isPlSqlAnnotationStatementsEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_statements_key, true);
   }

   public static boolean isPlSqlAnnotationTableOrViewEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_table_or_view_key, true);
   }

   public static boolean isPlSqlAnnotationUniqueTagEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_unique_tag_key, true);
   }

   public static boolean isPlSqlAnnotationUnreachableEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_unreachable_key, true);
   }

   public static boolean isPlSqlAnnotationWrongEndNameEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_wrong_end_name_key, true);
   }

   public static boolean isPlSqlAnnotationWrongFuncParamEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_wrong_func_param_key, true);
   }

   public static boolean isPlSqlAnnotationWrongInitParamEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_wrong_init_param_key, true);
   }

   public static boolean isPlSqlAnnotationWrongLuNameEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_wrong_lu_name_key, true);
   }

   public static boolean isPlSqlAnnotationWrongModuleEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_wrong_module_key, true);
   }

   public static boolean isPlSqlAnnotationWrongParamOrderEnabled() {
      return NbPreferences.forModule(PLSQLAnnotationsPanel.class).getBoolean(plsql_annotations_wrong_param_order_key, true);
   }
}
