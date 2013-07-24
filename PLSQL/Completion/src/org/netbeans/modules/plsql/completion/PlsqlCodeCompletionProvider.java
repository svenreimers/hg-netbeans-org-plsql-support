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
package org.netbeans.modules.plsql.completion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.plsql.completion.PlsqlContext.MethodCallContext;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionMediator;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.spi.editor.completion.CompletionItem;
import org.netbeans.spi.editor.completion.CompletionProvider;
import org.netbeans.spi.editor.completion.CompletionResultSet;
import org.netbeans.spi.editor.completion.CompletionTask;
import org.netbeans.spi.editor.completion.support.AsyncCompletionQuery;
import org.netbeans.spi.editor.completion.support.AsyncCompletionTask;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 *
 * @author chawlk
 */
public class PlsqlCodeCompletionProvider implements CompletionProvider {

   private static String filterWord = "";
   private static AsyncCompletionTask queryTask;
   //NOTE: The IGNORE_TOKENS array must be sorted in order for the binarySearch operation to work. Make sure to add entries in the same order as in the PlsqlTokenId enum class.
   private static final PlsqlTokenId[] IGNORE_TOKENS = {PlsqlTokenId.LINE_COMMENT, PlsqlTokenId.BLOCK_COMMENT, PlsqlTokenId.INT_LITERAL, PlsqlTokenId.DOUBLE_LITERAL};

   @Override
   public CompletionTask createTask(int queryType, JTextComponent component) {
      if (queryType != CompletionProvider.COMPLETION_QUERY_TYPE
              && queryType != CompletionProvider.COMPLETION_ALL_QUERY_TYPE) {
         return null;
      }

      component.addKeyListener(new CodeCompletionKeyDelegator());
      BaseDocument doc = (BaseDocument) component.getDocument();
      return new AsyncCompletionTask(new Query(doc, queryType), component);
   }

   @Override
   public int getAutoQueryTypes(JTextComponent jTextComponent, String string) {
      if (string != null && string.endsWith(".")) {
         return COMPLETION_QUERY_TYPE;
      }
      return 0;
   }

   public List<CompletionItem> queryCompletionItems(BaseDocument doc, DatabaseConnectionManager dbConnectionProvider, int caretOffset) {
      Query query = new Query(doc, dbConnectionProvider);
      return query.actualQuery(null, doc, caretOffset);
   }

   static class Query extends AsyncCompletionQuery {

      private DatabaseConnectionManager dbConnectionProvider;
      private DatabaseContentManager cache;
      private boolean updateCache;
      private DatabaseConnection databaseConnection = null;

      public Query(BaseDocument doc, int queryType) {
         this.updateCache = (queryType == CompletionProvider.COMPLETION_ALL_QUERY_TYPE);
         Object origin = doc.getProperty(Document.StreamDescriptionProperty);
         if (origin instanceof DataObject) {
            DataObject dataObject = (DataObject) origin;
            DatabaseConnectionMediator mediator = dataObject.getLookup().lookup(DatabaseConnectionMediator.class);
             if (mediator == null) {
                 FileObject primaryFile = dataObject.getPrimaryFile();
                 DatabaseConnectionMediator.Factory factory = Lookup.getDefault().lookup(DatabaseConnectionMediator.Factory.class);
                 if (factory != null) {
                     mediator = factory.create(primaryFile);
                 }
             }
            databaseConnection = mediator.getConnection().getConnection();
         }
         this.cache = DatabaseContentManager.getInstance(databaseConnection);
      }

      public Query(BaseDocument doc, DatabaseConnectionManager dbConnectionProvider) {
         this.dbConnectionProvider = dbConnectionProvider;
         if (dbConnectionProvider != null && dbConnectionProvider.hasConnection()) {
            databaseConnection = dbConnectionProvider.getTemplateConnection();
         }
         this.cache = DatabaseContentManager.getInstance(databaseConnection);
      }

      private void addCompletionItems(Collection<PlsqlCodeCompletionItem> items, String filterWord, List<CompletionItem> codeCompletionItems) {
         if (filterWord != null && !filterWord.startsWith("\"")) {
            filterWord = filterWord.toLowerCase(Locale.ENGLISH);
         }
         for (PlsqlCodeCompletionItem item : items) {
            if (item.getText().toLowerCase(Locale.ENGLISH).startsWith(filterWord)) {
               codeCompletionItems.add(item);
            }
         }
      }

      private void findAndCloneToArgumentVersion(List localMethods, MethodCallContext callContext, List<CompletionItem> codeCompletionItems) {
         for (int i = 0; i < localMethods.size(); i++) {
            PlsqlCodeCompletionMethodItem item = (PlsqlCodeCompletionMethodItem) localMethods.get(i);
            if (item.getText().equalsIgnoreCase(callContext.methodName)) {
               item = item.cloneToArgumentVersion(callContext.argPos);
               if (item != null) //clone return null if method doesn't have enough arguments
               {
                  codeCompletionItems.add(item);
               }
            }
         }
      }

      private CompletionItemType findItemType(String name, DatabaseConnection connection) {
         for (int i = 0; i < 2; i++) {
            if (cache.isPackage(name, connection)) {
               return CompletionItemType.PACKAGE;
            } else if (cache.isView(name)) {
               return CompletionItemType.VIEW;
            } else if (cache.isTable(name)) {
               return CompletionItemType.TABLE;
            } else if (cache.isSequence(name)) {
               return CompletionItemType.SEQUENCE;
            }
         }
         return null;
      }

      /**
       * Check whether the given name is a cursor name
       *
       * @param itemProvider
       * @param context
       * @param name
       * @return
       */
      private boolean isCursor(PlsqlCodeCompletionItemsProvider itemProvider, PlsqlContext context, String name) {
         List cursors = itemProvider.getRelevantCursors(context);
         for (int i = 0; i < cursors.size(); i++) {
            PlsqlCodeCompletionItem item = (PlsqlCodeCompletionItem) cursors.get(i);
            if (item.getText().equalsIgnoreCase(name)) {
               return true;
            }
         }
         return false;
      }

      public void queryCompletionItems(CompletionResultSet completionResultSet, Document document, int caretOffset) {
      }

      @SuppressWarnings("deprecation")
      @Override
      protected void query(CompletionResultSet completionResultSet, Document document, int caretOffset) {
         actualQuery(completionResultSet, document, caretOffset);
      }

      protected List<CompletionItem> actualQuery(CompletionResultSet completionResultSet, Document document, int caretOffset) {

         List<CompletionItem> codeCompletionItems = new ArrayList<CompletionItem>();

         BaseDocument doc = (BaseDocument) document;

         //If no database connection defined for the file there will be no results in the lookup
         if (databaseConnection == null) {
            if (completionResultSet != null) {
               completionResultSet.finish();
            }
            return codeCompletionItems;
         }

         if (updateCache) {
            if (dbConnectionProvider != null && !dbConnectionProvider.isOnline()) {
               dbConnectionProvider.setOnline(true);
            }
         } else if (completionResultSet != null) {
            completionResultSet.setHasAdditionalItems(true);
            completionResultSet.setHasAdditionalItemsText("Showing cached items.");
         }
//         DatabaseConnection databaseConnection = dbConnectionProvider.getPooledDatabaseConnection(false);
         try {
            doc.readLock();
            //first check if we're inside a comment or a string. If so disable code completion.
            if (caretOffset > 0) {
               TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
               @SuppressWarnings("unchecked")
               TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
               ts.move(caretOffset);
               if (!ts.moveNext()) { //we're at the end of the file
                  ts.movePrevious();
                  ts.moveNext();
               }
               Token<PlsqlTokenId> token = ts.token();
               if (caretOffset > ts.offset()) { //we're inside the token - not at the start
                  if (Arrays.binarySearch(IGNORE_TOKENS, token.id()) > -1) {
                     return codeCompletionItems;
                  }
               } else if (caretOffset == ts.offset() && PlsqlTokenId.WHITESPACE == token.id()) {
                  //check to see if we're at the end of a line comment or open string literal (just before the line feed)
                  if (ts.movePrevious() && (ts.token() != null) && (PlsqlTokenId.LINE_COMMENT == ts.token().id() || PlsqlTokenId.INCOMPLETE_STRING == ts.token().id())) {
                     return codeCompletionItems;
                  }
               }
            }
            if (dbConnectionProvider != null && dbConnectionProvider.isOnline() && completionResultSet != null) {
               completionResultSet.setWaitText("Fetching objects from database. Please wait...");
            }

            PlsqlContext context = new PlsqlContext(doc, caretOffset);
            String parent = context.getParentObject();
            if (parent != null) { //check to see if the dot is preceded by number...
               boolean isNumber = true;
               try {
                  Integer.parseInt(parent);
               } catch (NumberFormatException ex) {
                  isNumber = false;
               }
               if (isNumber) {
                  return codeCompletionItems;
               }
            }
            String filterWord = context.getFilterString();
            setFilterWord(filterWord);

            PlsqlCodeCompletionItemsProvider itemProvider = new PlsqlCodeCompletionItemsProvider(databaseConnection);
            String schema = null;
            if (parent != null && cache.isSchema(parent)) {
               schema = parent;
               parent = null;
            }
            if (parent != null) {
               CompletionItemType type = findItemType(parent, databaseConnection);
               String alias = null;
               if (type == null || (type == CompletionItemType.TABLE || type == CompletionItemType.VIEW)) {
                  alias = context.translateAlias(parent);
                  if (alias != null) {
                     parent = alias;
                     type = findItemType(parent, databaseConnection);
                  }
               }
               if (updateCache) {
                  cache.refreshItem(parent, databaseConnection);
               }

               if (type == CompletionItemType.TABLE || type == CompletionItemType.VIEW) {
                  /* Table and View Column handeling Start */
                  List items = itemProvider.getColumnObjects(parent, databaseConnection);
                  filterWord = filterWord.toLowerCase(Locale.ENGLISH);
                  for (int i = 0; i < items.size(); i++) {
                     PlsqlCodeCompletionColumnItem item = (PlsqlCodeCompletionColumnItem) items.get(i);
                     if (alias == null) {
                        item.showDatatype();
                     } else {
                        item.showViewOrTable();
                     }
                     if (item.getText().startsWith(filterWord)) {
                        codeCompletionItems.add(item);
                     }
                  }
               } else if (type == CompletionItemType.PACKAGE) {
                  if (context.showExceptions()) {
                     addCompletionItems(itemProvider.getPackageExceptions(parent, databaseConnection), filterWord, codeCompletionItems);
                  } else if (context.showTypes()) {
                     addCompletionItems(itemProvider.getPackageTypes(parent, databaseConnection), filterWord, codeCompletionItems);
                  } else {
                     /* Package method handeling Start */
                     filterWord = PlsqlDataAccessor.formatPlsqlName(filterWord);
                     List methods = itemProvider.getMethodObjects(parent, databaseConnection);
                     for (int i = 0; i < methods.size(); i++) {
                        PlsqlCodeCompletionMethodItem item = (PlsqlCodeCompletionMethodItem) methods.get(i);
                        if (context.showMethod(item) && item.getText().startsWith(filterWord)) {
                           codeCompletionItems.add(item);
                        }
                     }
                     addCompletionItems(itemProvider.getPackageConstants(parent, databaseConnection), filterWord, codeCompletionItems);
                  }
               } else if (type == CompletionItemType.SEQUENCE) {
                  /* Just get nextval, currval for sequence */
                  addCompletionItems(itemProvider.getSequencePseudoColumns(), filterWord, codeCompletionItems);
               } else {
                  filterWord = PlsqlDataAccessor.formatPlsqlName(filterWord);
                  //Check whether this is a cursor
                  if (isCursor(itemProvider, context, parent)) {
                     addCompletionItems(itemProvider.getCursorColumns(parent, context), filterWord, codeCompletionItems);
                  } else {
                     //Can be a type here, get the fields of the type if any
                     addCompletionItems(itemProvider.getTypeColumns(parent, context), filterWord, codeCompletionItems);
                  }
               }
            } else {
               if (context.showExceptions()) {
                  Collection<PlsqlCodeCompletionItem> exceptions = itemProvider.getPackageExceptions("Standard", databaseConnection);
                  exceptions.add(new PlsqlCodeCompletionItem("OTHERS", CompletionItemType.EXCEPTION));
                  addCompletionItems(exceptions, filterWord, codeCompletionItems);
                  //add OTHERS (standard exception type)


               }
               List selectList = context.getSelectListWithoutAlias();
               if (selectList != null && selectList.size() > 0) {
                  for (int i = 0; i < selectList.size(); i++) {
                     String view = (String) selectList.get(i);
                     if (view.startsWith("&")) {
                        view = context.getDefine(view.toUpperCase(Locale.ENGLISH));
                     }
                     if (updateCache) {
                        cache.refreshItem(view, databaseConnection);
                     }
                     List items = itemProvider.getColumnObjects(view, databaseConnection);
                     filterWord = filterWord.toLowerCase(Locale.ENGLISH);
                     for (int j = 0; j < items.size(); j++) {
                        PlsqlCodeCompletionColumnItem comItem = (PlsqlCodeCompletionColumnItem) items.get(j);
                        comItem.showViewOrTable();
                        if (comItem.getText().startsWith(filterWord)) {
                           codeCompletionItems.add(comItem);
                        }
                     }
                  }
               }
               //first check if we're inside a method call and in that case first list the method in question
               PlsqlContext.MethodCallContext callContext = context.getContainingMethodCallContext();
               List localMethods = null;
               if (callContext != null) {
                  if (callContext.pkgName != null) {
                     if (updateCache) {
                        cache.refreshItem(callContext.pkgName, databaseConnection);
                     }
                     findAndCloneToArgumentVersion(itemProvider.getMethodObjects(callContext.pkgName, databaseConnection), callContext, codeCompletionItems);
                  } else { //check local method call
                     findAndCloneToArgumentVersion(itemProvider.getLocalMethodObjects(context), callContext, codeCompletionItems);
                     findAndCloneToArgumentVersion(itemProvider.getMethodObjects("Standard", databaseConnection), callContext, codeCompletionItems);
                  }

               } else {
                  if (updateCache && (context.getSelectListWithoutAlias() == null || context.getSelectListWithoutAlias().isEmpty())) { //don't update table/view/package list when entering column names. Yes this will prevent new packages from showing up but it will also speed up performance...
                     cache.refreshItem(filterWord, databaseConnection);
                  }
               }
               if (context.showTableAliases()) {
                  HashMap aliasMap = context.getTableAliasMap();
                  Iterator keys = aliasMap.keySet().iterator();
                  filterWord = filterWord.toLowerCase(Locale.ENGLISH);
                  while (keys.hasNext()) {
                     String alias = ((String) keys.next()).toLowerCase(Locale.ENGLISH);
                     if (alias.startsWith(filterWord)) {
                        String realName = (String) aliasMap.get(alias);
                        CompletionItemType type = cache.isTable(realName) ? CompletionItemType.TABLE : CompletionItemType.VIEW;
                        codeCompletionItems.add(new PlsqlCodeCompletionAliasItem(alias, type, realName));
                     }
                  }
               }
               //List local methods - but not within select statements!!
               if (context.showPackages() && !context.showColums()) {
                  if (localMethods == null) {
                     localMethods = itemProvider.getLocalMethodObjects(context);
                  }
                  addCompletionItems(localMethods, filterWord, codeCompletionItems);
                  addCompletionItems(itemProvider.getProcedures(schema, databaseConnection), filterWord, codeCompletionItems);
               }

               if (context.showPackages()) {
                  addCompletionItems(itemProvider.getPackages(schema), filterWord, codeCompletionItems);
               }
               if (context.showViews()) {
                  addCompletionItems(itemProvider.getViews(schema), filterWord, codeCompletionItems);
               }
               if (context.showTables()) {
                  addCompletionItems(itemProvider.getTables(schema), filterWord, codeCompletionItems);
               }
               if (context.showSequences()) {
                  addCompletionItems(itemProvider.getSequences(schema), filterWord, codeCompletionItems);
                  addCompletionItems(itemProvider.getFunctions(schema, databaseConnection), filterWord, codeCompletionItems);
               }

               if (!context.showOnlyViewsAndTables()) { //don't show variables, etc in from statements
                  //Add cursors
                  addCompletionItems(itemProvider.getRelevantCursors(context), filterWord, codeCompletionItems);
                  //Add types
                  addCompletionItems(itemProvider.getRelevantTypes(context), filterWord, codeCompletionItems);
                  //Add local variables
                  addCompletionItems(itemProvider.getVariables(context), filterWord, codeCompletionItems);
               }
            }
            if (codeCompletionItems.isEmpty()) //add dummy entry to enable the has additional items text...
            {
               codeCompletionItems.add(new PlsqlCodeCompletionNoSuggestion());
            }

            if (completionResultSet != null) {
               completionResultSet.addAllItems(codeCompletionItems);
            }

         } finally {
            if (dbConnectionProvider != null) {
               dbConnectionProvider.releaseDatabaseConnection(databaseConnection);
            }
            if (completionResultSet != null) {
               completionResultSet.finish();
            }
            doc.readUnlock();
         }
         return codeCompletionItems;
      }
   }

   public static String getFilterWord() {
      return filterWord;
   }

   public static void setFilterWord(String aFilterWord) {
      if (aFilterWord != null) {
         filterWord = aFilterWord;
      } else {
         filterWord = "";
      }
   }
}
