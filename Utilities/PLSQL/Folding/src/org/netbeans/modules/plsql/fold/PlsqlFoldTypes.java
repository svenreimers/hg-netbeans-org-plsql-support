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
package org.netbeans.modules.plsql.fold;

import org.netbeans.api.editor.fold.FoldType;

public class PlsqlFoldTypes {
    
     private static final String FOLD_TYPE_PREFIX = "plsql-";
     
     /** Plsql package Folder Type */
     public static final FoldType PACKAGE = new FoldType(FOLD_TYPE_PREFIX + "package"); 
     
     /** Plsql package body Folder Type */
     public static final FoldType PACKAGEBODY = new FoldType(FOLD_TYPE_PREFIX + "packagebody"); 
     
     /** Plsql Procedure Folder implementation Type */
     public static final FoldType PROCEDUREIMPL = new FoldType(FOLD_TYPE_PREFIX + "procedureimpl"); 
     
     /** Plsql Function implementation Folder Type */
     public static final FoldType FUNCTIONIMPL = new  FoldType(FOLD_TYPE_PREFIX + "functionimpl"); 
     
     /** Plsql Procedure definition Folder Type */
     public static final FoldType PROCEDUREDEF = new FoldType(FOLD_TYPE_PREFIX + "proceduredef"); 
     
     /** Plsql Function definition Folder Type */
     public static final FoldType FUNCTIONDEF = new  FoldType(FOLD_TYPE_PREFIX + "functiondef"); 
     
     /** Plsql Comment Folder Type */
     public static final FoldType COMMENT = new FoldType(FOLD_TYPE_PREFIX + "comment"); 
     
     /** Plsql view Folder Type */
     public static final FoldType VIEW = new FoldType(FOLD_TYPE_PREFIX + "view"); 
     
     /** Plsql column comment Folder Type */
     public static final FoldType COLUMNCOMMENT = new FoldType(FOLD_TYPE_PREFIX + "columncomment"); 

     /** Plsql table comment Folder Type */
     public static final FoldType TABLECOMMENT = new FoldType(FOLD_TYPE_PREFIX + "tablecomment"); 
     
     /** Plsql declare end Folder Type */
     public static final FoldType DECLAREEND = new FoldType(FOLD_TYPE_PREFIX + "declareend"); 
     
     /** Plsql begin end Folder Type */
     public static final FoldType BEGINEND = new FoldType(FOLD_TYPE_PREFIX + "beginend"); 
     
     /** Plsql cursor Folder Type */
     public static final FoldType CURSOR = new FoldType(FOLD_TYPE_PREFIX + "cursor"); 
     
     /** Plsql trigger Folder Type */
     public static final FoldType TRIGGER = new FoldType(FOLD_TYPE_PREFIX + "trigger"); 
     
     /** Plsql if Folder Type */
     public static final FoldType IF = new FoldType(FOLD_TYPE_PREFIX + "if"); 
     
      /** Plsql for loop Folder Type */
     public static final FoldType FORLOOP = new FoldType(FOLD_TYPE_PREFIX + "forloop");
     
      /** Plsql while loop Folder Type */
     public static final FoldType WHILELOOP = new FoldType(FOLD_TYPE_PREFIX + "whileloop"); 
     
      /** Plsql loop Folder Type */
     public static final FoldType LOOP = new FoldType(FOLD_TYPE_PREFIX + "loop"); 
     
      /** Plsql custom Folder Type */
     public static final FoldType CUSTOM = new FoldType(FOLD_TYPE_PREFIX + "custom"); 
     
     /** Plsql statement Folder Type */
     public static final FoldType STATEMENT = new FoldType(FOLD_TYPE_PREFIX + "statement");

     /** Plsql Java Source Folder Type */
     public static final FoldType JAVASOURCE = new FoldType(FOLD_TYPE_PREFIX + "javasource");

     /** Plsql Case statement Folder Type */
     public static final FoldType CASE = new FoldType(FOLD_TYPE_PREFIX + "case");
}
