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
package org.netbeans.modules.plsql.hyperlink.target;

import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.PlsqlFileLocatorService;
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.util.List;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author ChrLSE
 */
final public class PlsqlGotoTargetFactory {

   public static PlsqlGotoTargetFactory instance = new PlsqlGotoTargetFactory();
   private final PlsqlFileValidatorService fileValidator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
   private final PlsqlFileLocatorService fileLocator = Lookup.getDefault().lookup(PlsqlFileLocatorService.class);

   private PlsqlGotoTargetFactory() {
   }

   public PlsqlGotoTarget getTargetForSpec(final Lookup lookup) {
      final DataObject dataObject = lookup.lookup(DataObject.class);
      final EditorCookie editorCookie = lookup.lookup(EditorCookie.class);
      final Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
      final int caretPosition = PlsqlHyperlinkUtil.getCaretPosition(editorCookie);
      final Document doc = editorCookie.getDocument();
      if (doc == null || dataObject == null || project == null) {
         return new PlsqlNullTarget();
      }
      final PlsqlBlockFactory fac = dataObject.getLookup().lookup(PlsqlBlockFactory.class);

      final DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
      final DatabaseConnection databaseConnection = connectionProvider != null ? connectionProvider.getDatabaseConnection(false) : null;
      final DatabaseContentManager cache = DatabaseContentManager.getInstance(databaseConnection);

      //Get the current token
      final CurrentToken current = new CurrentToken(doc, caretPosition, fac);
      //If parent is null can be a method in the same file or package name
      final List blockHier = dataObject.getLookup().lookup(PlsqlBlockFactory.class).getBlockHierarchy();
      if (!current.hasParent()) {
         if (current.name != null) {
            if (fac.isDefine(current.name)) {
               return new PlsqlDefineTarget(current.name, caretPosition, dataObject, project, doc);
            } //If user has clicked on a alias
            final String packageNm = PlsqlParserUtil.getPackageName(fac, caretPosition);
            PlsqlBlock block = PlsqlHyperlinkUtil.getMethodBlock(blockHier, current.name, packageNm, true);
            if (block == null) {
               block = PlsqlHyperlinkUtil.getMethodBlock(blockHier, current.name, packageNm, false);
            }
            if (block != null) {
               return new PlsqlMethodTarget(current.name, caretPosition, dataObject, project, doc);
            }
            if (isPackageInCache(cache, current.name, databaseConnection)) {
               return new PlsqlPackageTarget(current.name, caretPosition, dataObject, project, doc);
            }
         }
      } else {
         if (current.notEmpty()) {
            if (isPackageInCache(cache, current.parent, databaseConnection)) {
               return new PlsqlMethodTarget(current.parent, current.name, caretPosition, dataObject, project, doc);
            }
         }
      }

      //Check whether we have selected an offset within a method definition
      final PlsqlBlock block = getEnclosingMethod(blockHier, caretPosition);
      if (block != null && block.getParent() != null) {
         return new PlsqlMethodTarget(block.getParent().getName(), block.getName(), caretPosition, dataObject, project, doc);
      }

      //Check whether this is within the package body block
      for (int i = 0; i < blockHier.size(); i++) {
         final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
         if ((caretPosition <= temp.getEndOffset() && caretPosition >= temp.getStartOffset()) && temp.getType() == PACKAGE_BODY) {
            return new PlsqlPackageTarget(temp.getName(), caretPosition, dataObject, project, doc);
         }
      }

      if (fileValidator.isValidPackage(dataObject)) {
         return new PlsqlPackageTarget(PlsqlHyperlinkUtil.getPackageName(dataObject), caretPosition, dataObject, project, doc);
      }

      return new PlsqlNullTarget();
   }

   private boolean isPackageInCache(final DatabaseContentManager cache, final String objectName, final DatabaseConnection databaseConnection) {
      if (cache == null) {
         return false;
      }
      return cache.isPackage(objectName, databaseConnection);
   }

   public PlsqlGotoTarget getTargetForBody(final Lookup lookup) {
      final DataObject dataObject = lookup.lookup(DataObject.class);
      final EditorCookie editorCookie = lookup.lookup(EditorCookie.class);
      final Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
      final int caretPosition = PlsqlHyperlinkUtil.getCaretPosition(editorCookie);
      final Document doc = editorCookie.getDocument();
      if (doc == null || dataObject == null || project == null) {
         return new PlsqlNullTarget();
      }
      final PlsqlBlockFactory fac = dataObject.getLookup().lookup(PlsqlBlockFactory.class);

      final DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(dataObject);
      final DatabaseConnection databaseConnection = connectionProvider != null ? connectionProvider.getDatabaseConnection(false) : null;
      final DatabaseContentManager cache = DatabaseContentManager.getInstance(databaseConnection);

      //Get the current token
      final CurrentToken current = new CurrentToken(doc, caretPosition, fac);

      final List blockHier = dataObject.getLookup().lookup(PlsqlBlockFactory.class).getBlockHierarchy();
      if (current.name != null) {
         //If user has clicked on a placeholder
         current.name = fac.getDefine(current.name);

         if (cache != null) {
            final String aliasOf = cache.getTableForSynonym(current.name);
            if (aliasOf != null) {
               current.name = aliasOf;
            }
         }

         //If parent is null can be a method in the same file or package name
         if (current.hasParent()) {
            final PlsqlBlockType plsqlType = fileLocator.getPlsqlType(project, current.parent);
            if (plsqlType == PACKAGE || plsqlType == PACKAGE_BODY) {
               return new PlsqlMethodTarget(current.parent, current.name, caretPosition, dataObject, project, doc);
            }

            if (cache != null) {
               //identity if the parent is a package
               if (cache.isPackage(current.parent, databaseConnection)) {
                  //Possibly a method call
                  return new PlsqlMethodTarget(current.parent, current.name, caretPosition, dataObject, project, doc);
               }
            }
         }
         final String packageNm = PlsqlParserUtil.getPackageName(fac, caretPosition);
         PlsqlBlock block = PlsqlHyperlinkUtil.getMethodBlock(blockHier, current.name, packageNm, false);
         if (block == null) {
            block = PlsqlHyperlinkUtil.getMethodBlock(blockHier, current.name, packageNm, true);
         }
         if (block != null) {
            return new PlsqlMethodTarget(packageNm, current.name, caretPosition, dataObject, project, doc);
         }

         if (isViewName(blockHier, current.name)) {
            return new PlsqlViewTarget(current.name, caretPosition, dataObject, project, doc);
         }

         if (cache != null) {
            //identity if the current.name is a package
            if (cache.isPackage(current.name, databaseConnection)) {
               return new PlsqlPackageBodyTarget(current.name, caretPosition, dataObject, project, doc);
            } else if (cache.isTable(current.name, databaseConnection)) {
               return new PlsqlTableTarget(current.name, caretPosition, dataObject, project, doc);
            } else if (cache.isView(current.name, databaseConnection)) {
               return new PlsqlViewTarget(current.name, caretPosition, dataObject, project, doc);
            } else if (cache.isFunction(current.name) || cache.isProcedure(current.name)) {
               return new PlsqlStandaloneMethodTarget(current.name, caretPosition, dataObject, project, doc);
            }
         }
      }
      //Check whether we have selected an offset within a method definition
      final PlsqlBlock block = getEnclosingMethod(blockHier, caretPosition);
      if (block != null && block.getParent() != null) {
         return new PlsqlMethodTarget(block.getParent().getName(), block.getName(), caretPosition, dataObject, project, doc);
      }

      //Check whether this is within the package def block
      for (int i = 0; i < blockHier.size(); i++) {
         final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
         if ((caretPosition <= temp.getEndOffset() && caretPosition >= temp.getStartOffset()) && temp.getType() == PACKAGE) {
            return new PlsqlPackageBodyTarget(temp.getName(), caretPosition, dataObject, project, doc);
         }
      }

      //Check whether we have selected spec file
      if (fileValidator.isValidPackage(dataObject)) {
         return new PlsqlPackageBodyTarget(PlsqlHyperlinkUtil.getPackageName(dataObject), caretPosition, dataObject, project, doc);
      }
      return new PlsqlNullTarget();
   }

   public PlsqlGotoTarget getTargetForDb(final Lookup lookup) {
      return getTargetForBody(lookup);
   }

   /**
    * Check whether a view block with the given name exists in the given hierarchy
    * @param blockHier
    * @param name
    * @return
    */
   protected boolean isViewName(final List blockHier, final String name) {
      for (int i = 0; i < blockHier.size(); i++) {
         final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
         if (temp.getName().equalsIgnoreCase(name) && temp.getType() == VIEW) {
            return true;
         }
      }
      return false;
   }

   /**
    * Method that will get the method name that encloses the given offset
    * @param blockHier
    * @param offset
    * @return
    */
   protected PlsqlBlock getEnclosingMethod(final List blockHier, final int offset) {
      for (int i = 0; i < blockHier.size(); i++) {
         final PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
         if ((offset <= temp.getEndOffset()) && (offset >= temp.getStartOffset())) {
            //Offset is within the block, if this is not a procedure look at the children and return
            if (temp.getType() == FUNCTION_DEF
                    || temp.getType() == PROCEDURE_DEF) {
               return temp;
            } else if ((temp.getType() == PACKAGE_BODY)
                    || (temp.getType() == PACKAGE)) {
               final PlsqlBlock block = getEnclosingMethod(temp.getChildBlocks(), offset);
               if (block != null) {
                  return block;
               }
            }
            break;
         }
      }
      return null;
   }

   class CurrentToken {

      String name;
      String parent;

      CurrentToken(final Document doc, final int caretPosition, final PlsqlBlockFactory fac) {
         final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
         @SuppressWarnings("unchecked")
         final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

         if (ts != null) {
            ts.move(caretPosition);
            Token<PlsqlTokenId> token = ts.token();
            if (ts.moveNext()) {
               token = ts.token();
               final PlsqlTokenId tokenID = token.id();

               if (tokenID == PlsqlTokenId.IDENTIFIER) {
                  name = token.text().toString();
                  if (ts.movePrevious()) {
                     token = ts.token();
                     if (token.id() == PlsqlTokenId.DOT) {
                        if (ts.movePrevious()) {
                           token = ts.token();
                        }
                        if (token.id() == PlsqlTokenId.IDENTIFIER) {
                           parent = token.toString();
                        } else if (token.id() == PlsqlTokenId.DOT) {
                           if (ts.movePrevious()) {
                              token = ts.token();
                           }
                           if (token.id() == PlsqlTokenId.IDENTIFIER && token.toString().startsWith("&")) {
                              parent = fac.getDefine(token.toString());
                           }
                        }
                     }
                  }
               } else if (tokenID == PlsqlTokenId.BLOCK_COMMENT
                       || tokenID == PlsqlTokenId.LINE_COMMENT
                       || tokenID == PlsqlTokenId.STRING_LITERAL) {
                  final PlsqlHyperlinkUtil.ImageInfo info = PlsqlHyperlinkUtil.getCurrentWordInfo(doc, caretPosition);
                  if (info != null) {
                     name = info.image;
                     parent = info.parentImage;
                  }
               }
            }
         }
      }

      public String getName() {
         return name;
      }

      boolean hasParent() {
         if (parent != null && !parent.isEmpty()) {
            return true;
         }
         return false;
      }

      public String getParent() {
         return parent;
      }

      private boolean notEmpty() {
         return name != null && parent != null;
      }
   }
}
