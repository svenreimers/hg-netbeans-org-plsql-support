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
package org.netbeans.modules.plsql.annotation;

import org.netbeans.modules.plsqlsupport.options.OptionsUtilities;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlCursorWhereAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlFunctionReturnAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlIfNullAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlMissingEndNameAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlUnreachableAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlWrongEndNameAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlWrongFunctionParamAnnotation;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlWrongParamOrderAnnotation;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author subslk
 */
public class GenericPlsqlAnnotations implements Annotation{

   public static final String BLOCK_FUNCTION_IMPL = "FUNCTION_IMPL";
   public static final String BLOCK_FUNCTION_DEF = "FUNCTION_DEF";
   public static final String BLOCK_PROCEDURE_IMPL = "PROCEDURE_IMPL";
   public static final String BLOCK_PROCEDURE_DEF = "PROCEDURE_DEF";
   public static final String BLOCK_IF = "IF";
   public static final String BLOCK_CURSOR = "CURSOR";
   public static final String DEFAULT_ANNOTATIONS = " DEFAULT_ANNOTATIONS";
   public static final String BLOCK_STATEMENT = "STATEMENT";

    @Override
    public void loadConfiguration() {

      final PlsqlWrongFunctionParamAnnotation wrongParamAnnotation = PlsqlWrongFunctionParamAnnotation.getDummyInstance();
      final PlsqlWrongParamOrderAnnotation wrongOrderAnnotation = PlsqlWrongParamOrderAnnotation.getDummyInstance();
      final PlsqlMissingEndNameAnnotation endNameAnnotation = PlsqlMissingEndNameAnnotation.getDummyInstance();
      final PlsqlWrongEndNameAnnotation wrongEndAnnotation = PlsqlWrongEndNameAnnotation.getDummyInstance();

      final Set<PlsqlAnnotation> procDefAnnotations = new HashSet<PlsqlAnnotation>();
      configuration.put(BLOCK_PROCEDURE_DEF, procDefAnnotations);

      final Set<PlsqlAnnotation> procImplAnnotations = new HashSet<PlsqlAnnotation>();
      configuration.put(BLOCK_PROCEDURE_IMPL, procImplAnnotations);

      final Set<PlsqlAnnotation> funcDefAnnotations = new HashSet<PlsqlAnnotation>();
      configuration.put(BLOCK_FUNCTION_DEF, funcDefAnnotations);

      final Set<PlsqlAnnotation> funcImplAnnotations = new HashSet<PlsqlAnnotation>();
      configuration.put(BLOCK_FUNCTION_IMPL, funcImplAnnotations);

      final Set<PlsqlAnnotation> ifAnnotations = new HashSet<PlsqlAnnotation>();
      configuration.put(BLOCK_IF, ifAnnotations);

      final Set<PlsqlAnnotation> cursorAnnotations = new HashSet<PlsqlAnnotation>();
      configuration.put(BLOCK_CURSOR, cursorAnnotations);

      if (OptionsUtilities.isPlSqlAnnotationWrongParamOrderEnabled()) {
         procDefAnnotations.add(wrongOrderAnnotation);
         procImplAnnotations.add(wrongOrderAnnotation);
         funcDefAnnotations.add(wrongOrderAnnotation);
         funcImplAnnotations.add(wrongOrderAnnotation);
      }

      if (OptionsUtilities.isPlSqlAnnotationMissingEndNameEnabled()) {
         procImplAnnotations.add(endNameAnnotation);
         funcImplAnnotations.add(endNameAnnotation);
      }

      if (OptionsUtilities.isPlSqlAnnotationWrongEndNameEnabled()) {
         procImplAnnotations.add(wrongEndAnnotation);
         funcImplAnnotations.add(wrongEndAnnotation);
      }

      if (OptionsUtilities.isPlSqlAnnotationWrongFuncParamEnabled()) {
         funcDefAnnotations.add(wrongParamAnnotation);
         funcImplAnnotations.add(wrongParamAnnotation);
      }

      if (OptionsUtilities.isPlSqlAnnotationFunctionReturnEnabled()) {
         funcImplAnnotations.add(PlsqlFunctionReturnAnnotation.getDummyInstance());
      }

      if (OptionsUtilities.isPlSqlAnnotationUnreachableEnabled()) {
         funcImplAnnotations.add(PlsqlUnreachableAnnotation.getDummyInstance());
      }

      if (OptionsUtilities.isPlSqlAnnotationIfNullEnabled()) {
         ifAnnotations.add(PlsqlIfNullAnnotation.getDummyInstance());
      }

      if (OptionsUtilities.isPlSqlAnnotationCursorWhereEnabled()) {
         cursorAnnotations.add(PlsqlCursorWhereAnnotation.getDummyInstance());
      }
   }

    @Override
    public Map<String, Set<PlsqlAnnotation>> getConfiguration() {
        return configuration;
    }

    @Override
    public String getType(PlsqlBlock block) {
        if (block == null) {
            return TOKEN_ERROR_SYS;
        }
        if (block.getType() == PlsqlBlockType.PROCEDURE_DEF) {
            return BLOCK_PROCEDURE_DEF;
        } else if (block.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            return BLOCK_PROCEDURE_IMPL;
        } else if (block.getType() == PlsqlBlockType.FUNCTION_DEF) {
            return BLOCK_FUNCTION_DEF;
        } else if (block.getType() == PlsqlBlockType.FUNCTION_IMPL) {
            return BLOCK_FUNCTION_IMPL;
        } else if (block.getType() == PlsqlBlockType.IF) {
            return BLOCK_IF;
        } else if (block.getType() == PlsqlBlockType.CURSOR) {
            return BLOCK_CURSOR;
        } else if (block.getType() == PlsqlBlockType.STATEMENT) {
            return BLOCK_STATEMENT;
        } else {
            return TOKEN_ERROR_SYS;
        }

    }

}
