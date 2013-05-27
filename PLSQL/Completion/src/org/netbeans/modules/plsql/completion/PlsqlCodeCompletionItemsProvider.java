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

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.openide.util.Exceptions;

/**
 *
 * @author chawlk
 */
public class PlsqlCodeCompletionItemsProvider {

//   private static String dbConnectionString = null;
//   private static long lastPackageSyncTime = 0;
//   private static long lastViewSyncTime = 0;
//   private static long lastTableSyncTime = 0;
//   private static long lastSequenceSyncTime = 0;
//   private static HashMap<String, PlsqlCodeCompletionItem> tableItems = new HashMap<String, PlsqlCodeCompletionItem>();
//   private static HashMap<String, PlsqlCodeCompletionItem> viewItems = new HashMap<String, PlsqlCodeCompletionItem>();
//   private static HashMap<String, PlsqlCodeCompletionItem> pkgItems = new HashMap<String, PlsqlCodeCompletionItem>();
//   private static HashMap<String, PlsqlCodeCompletionItem> seqItems = new HashMap<String, PlsqlCodeCompletionItem>();
   private DatabaseContentManager cache = null;
   private DatabaseConnectionManager connectionProvider = null;

   /** Creates a new instance of PLSQLCodeCompletionItemsProvider */
   public PlsqlCodeCompletionItemsProvider(Document doc) {

      connectionProvider = DatabaseConnectionManager.getInstance(doc);
      if (connectionProvider != null) {
         DatabaseConnection connection = connectionProvider.getTemplateConnection();
         if (connection != null) {
            cache = DatabaseContentManager.getInstance(connection);
         }
      }
   }

   private void updateCompletionItemCollection(Collection<PlsqlCodeCompletionItem> map, Collection records, CompletionItemType type) {
      Iterator it = records.iterator();
      while (it.hasNext()) {
         map.add(new PlsqlCodeCompletionItem((String) it.next(), type));
      }
   }

   /**
    * Get all database packages
    * @return Collection of Code Completion Items for all database packages
    */
   public Collection<PlsqlCodeCompletionItem> getPackages(String schema) {
      Collection<PlsqlCodeCompletionItem> items = new ArrayList<PlsqlCodeCompletionItem>();
      updateCompletionItemCollection(items, cache.getAllPackages(schema), CompletionItemType.PACKAGE);
      return items;
   }

   /**
    * Get all database tables
    * @return Collection of Code Completion Items for all database tables
    */
   public Collection<PlsqlCodeCompletionItem> getTables(String schema) {
      Collection<PlsqlCodeCompletionItem> items = new ArrayList<PlsqlCodeCompletionItem>();
      updateCompletionItemCollection(items, cache.getAllTables(schema), CompletionItemType.TABLE);
      return items;
   }

   /**
    * Get all database views
    * @return Collection of Code Completion Items for all database views
    */
   public Collection<PlsqlCodeCompletionItem> getViews(String schema) {
      Collection<PlsqlCodeCompletionItem> items = new ArrayList<PlsqlCodeCompletionItem>();
      updateCompletionItemCollection(items, cache.getAllViews(schema), CompletionItemType.VIEW);
      return items;
   }

   /**
    * Get all database sequences
    * @return Collection of Code Completion Items for all database sequences
    */
   public Collection<PlsqlCodeCompletionItem> getSequences(String schema) {
      Collection<PlsqlCodeCompletionItem> items = new ArrayList<PlsqlCodeCompletionItem>();
      updateCompletionItemCollection(items, cache.getAllSequences(schema), CompletionItemType.SEQUENCE);
      return items;
   }

   /**
    * Get all database functions
    * @return Collection of Code Completion Items for all database functions
    */
   public Collection<PlsqlCodeCompletionItem> getFunctions(String schema, DatabaseConnection databaseConnection) {
      Collection<PlsqlCodeCompletionItem> items = new ArrayList<PlsqlCodeCompletionItem>();
      updateCompletionItemCollection(items, cache.getAllFunctions(schema), CompletionItemType.FUNCTION);
      List<PlsqlCodeCompletionMethodItem> standardMethods = getMethodObjectsMerged("Standard", databaseConnection);
      for(PlsqlCodeCompletionMethodItem item : standardMethods) {
         if(item.getType()==CompletionItemType.FUNCTION) {
            items.add(item);
         }
      }
      return items;
   }

   /**
    * Get all database procedures
    * @return Collection of Code Completion Items for all database procedures
    */
   public Collection<PlsqlCodeCompletionItem> getProcedures(String schema, DatabaseConnection databaseConnection) {
      Collection<PlsqlCodeCompletionItem> items = new ArrayList<PlsqlCodeCompletionItem>();
      updateCompletionItemCollection(items, cache.getAllProcedures(schema), CompletionItemType.PROCEDURE);
      List<PlsqlCodeCompletionMethodItem> standardMethods = getMethodObjectsMerged("Standard", databaseConnection);
      for(PlsqlCodeCompletionMethodItem item : standardMethods) {
         if(item.getType()==CompletionItemType.PROCEDURE) {
            items.add(item);
         }
      }      
      return items;
   }


   /**
    * Get pseudo columns for database sequences
    * @return List of code completion items for sequence psuedo columns. (i.e. 'nextval' and 'currval')
    */
   public List<PlsqlCodeCompletionItem> getSequencePseudoColumns() {
     List<PlsqlCodeCompletionItem> columnList = new ArrayList<PlsqlCodeCompletionItem>();
     columnList.add(new PlsqlCodeCompletionItem("nextval", CompletionItemType.COLUMN));
     columnList.add(new PlsqlCodeCompletionItem("currval", CompletionItemType.COLUMN));
     return columnList;
   }

   /**
    * Get the columns of the given table/view
    * @param name of view/table
    * @return List of code completion items for the columns of the specified table/view
    */
   public List<PlsqlCodeCompletionColumnItem> getColumnObjects(String owner, DatabaseConnection databaseConnection) {
      List<PlsqlCodeCompletionColumnItem> columnList = new ArrayList<PlsqlCodeCompletionColumnItem>();
      Map<String, String> columns = cache.getColumnObjects(owner, databaseConnection);
      for (Map.Entry<String, String> entry : columns.entrySet()) {
         columnList.add(new PlsqlCodeCompletionColumnItem(entry.getKey(), CompletionItemType.COLUMN, entry.getValue(), owner));
      }
      return columnList;
   }

   /**
    * Get only the functions/procedures belonging to the given owner, of the given package
    * @param owner
    * @return
    */
   public List<PlsqlCodeCompletionMethodItem> getMethodObjects(String pkg, DatabaseConnection databaseConnection) {
      return getMethodObjects(pkg, databaseConnection, false);
   }
   
   /**
    * Get only the functions/procedures belonging to the given owner, of the given package
    * @param owner
    * @return
    */
   public List<PlsqlCodeCompletionMethodItem> getMethodObjectsMerged(String pkg, DatabaseConnection databaseConnection) {
      return getMethodObjects(pkg, databaseConnection, true);
   }
   
   /**
    * Get only the functions/procedures belonging to the given owner, of the given package
    * @param owner
    * @return
    */
   private List<PlsqlCodeCompletionMethodItem> getMethodObjects(String pkg, DatabaseConnection databaseConnection, boolean merged) {
      Map<String, PlsqlCodeCompletionMethodItem> uniqueList = new HashMap<String, PlsqlCodeCompletionMethodItem>();
      List<PlsqlCodeCompletionMethodItem> methodList = new ArrayList<PlsqlCodeCompletionMethodItem>();
      Map<String, String> methods = cache.getMethodObjects(pkg, databaseConnection);
      for (Map.Entry<String, String> methodEntry : methods.entrySet()) {
         String name = methodEntry.getKey();
         String documentation = methodEntry.getValue();
         int i = name.indexOf(":");
         if (i > 0) {
            name = name.substring(0, i);
         }
         if(merged) {
            PlsqlCodeCompletionMethodItem item = uniqueList.get(name);
            if(item!=null) {
               item.setDocumentation(item.getDocumentation()+"<br>---<br>"+documentation);
            } else {
               CompletionItemType type = documentation.indexOf(">FUNCTION<") > 0 ? CompletionItemType.FUNCTION : CompletionItemType.PROCEDURE;
               item = new PlsqlCodeCompletionMethodItem(PlsqlDataAccessor.formatPlsqlName(name), type, documentation);
               methodList.add(item);
               uniqueList.put(name, item);
            }
         } else {
            CompletionItemType type = documentation.indexOf(">FUNCTION<") > 0 ? CompletionItemType.FUNCTION : CompletionItemType.PROCEDURE;
            methodList.add(new PlsqlCodeCompletionMethodItem(PlsqlDataAccessor.formatPlsqlName(name), type, documentation));
         }
      }
      return methodList;
   }

   /**
    * Get the exceptions belonging to the given owner, of the given package
    * @param package
    * @return
    */
   public List<PlsqlCodeCompletionItem> getPackageExceptions(String pkg, DatabaseConnection databaseConnection) {
      return getPackageMembers(pkg, CompletionItemType.EXCEPTION, databaseConnection);
   }

   /**
    * Get the exceptions belonging to the given owner, of the given package
    * @param package
    * @return
    */
   public List<PlsqlCodeCompletionItem> getPackageCursors(String pkg, DatabaseConnection databaseConnection) {
      return getPackageMembers(pkg, CompletionItemType.CURSOR, databaseConnection);
   }

   /**
    * Get the constants belonging to the given owner, of the given package
    * @param package
    * @return
    */
   public List<PlsqlCodeCompletionItem> getPackageConstants(String pkg, DatabaseConnection databaseConnection) {
      return getPackageMembers(pkg, CompletionItemType.CONSTANT, databaseConnection);
   }

   /**
    * Get the types belonging to the given owner, of the given package
    * @param package
    * @return
    */
   public List<PlsqlCodeCompletionItem> getPackageTypes(String pkg, DatabaseConnection databaseConnection) {
      return getPackageMembers(pkg, CompletionItemType.TYPE, databaseConnection);
   }

   
   private List<PlsqlCodeCompletionItem> getPackageMembers(String pkg, CompletionItemType memberType, DatabaseConnection databaseConnection) {
      List<PlsqlCodeCompletionItem> methodList = new ArrayList<PlsqlCodeCompletionItem>();
      Map<String, String> methods = cache.getPackageMembers(pkg, databaseConnection);
      for (Map.Entry<String, String> methodEntry : methods.entrySet()) {
         String name = methodEntry.getKey();
         String type = methodEntry.getValue();
         if(CompletionItemType.valueOf(type)==memberType) {
            methodList.add(new PlsqlCodeCompletionItem(name, memberType));
         }
      }
      return methodList;
   }
      
   String extractMethodHeader(PlsqlBlock block, PlsqlContext context) {
      BaseDocument document = context.getDocument();
      int start = block.getStartOffset();
      int end = block.getEndOffset();
      TokenSequence<PlsqlTokenId> ts = context.getTokenSequence();
      ts.move(start);
      //find the IS part of the method and use that as the end position
      while (ts.moveNext()) {
         Token<PlsqlTokenId> token = ts.token();
         PlsqlTokenId tokenID = token.id();
         if ((tokenID == PlsqlTokenId.KEYWORD && "IS".equalsIgnoreCase(token.toString()))
                 || (tokenID == PlsqlTokenId.OPERATOR && ";".equalsIgnoreCase(token.toString()))) {
            end = ts.offset();
            break;
         }
      }
      try {
         return document.getText(start, end - start).replaceAll("\n", " ");
      } catch (BadLocationException ex) {
         return "";
      }
   }

   private List<PlsqlCodeCompletionMethodItem> extractMethodsFromPackage(PlsqlBlock parent, PlsqlContext context) {
      List<PlsqlBlock> childBlocks = parent.getChildBlocks();
      List<PlsqlCodeCompletionMethodItem> methods = new ArrayList<PlsqlCodeCompletionMethodItem>();

      for (int i = 0; i < childBlocks.size(); i++) {
         PlsqlBlock block = childBlocks.get(i);
         if (block.getType() == PlsqlBlockType.FUNCTION_IMPL) {
            String body = extractMethodHeader(block, context);
            if (body != null && body.length() > 0) {
               String doc = PlsqlDataAccessor.formatMethodDoc(null, block.getName(), body, null, null);
               methods.add(new PlsqlCodeCompletionMethodItem(block.getName(), CompletionItemType.FUNCTION, doc));
            }
         } else if (block.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            String body = extractMethodHeader(block, context);
            if (body != null && body.length() > 0) {
               String doc = PlsqlDataAccessor.formatMethodDoc(null, block.getName(), body, null, null);
               methods.add(new PlsqlCodeCompletionMethodItem(block.getName(), CompletionItemType.PROCEDURE, doc));
            }
         }
      }
      return methods;
   }

   /**
    * Get only the functions/procedures belonging to the current package
    * @param call context
    * @return
    */
   public List<PlsqlCodeCompletionMethodItem> getLocalMethodObjects(PlsqlContext context) {
      PlsqlBlockFactory blockFactory = context.getBlockFactory();
      if (blockFactory != null) {
         List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
         for (int i = 0; i < blocks.size(); i++) {
            PlsqlBlock block = blocks.get(i);
            if (block.getType() == PlsqlBlockType.PACKAGE_BODY) {
               //candidate. Check if the cursor is inside this package
               int pos = context.getCaretOffset();
               if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
                  return extractMethodsFromPackage(block, context);
               }
            }
         }
      }
      return new ArrayList<PlsqlCodeCompletionMethodItem>();
   }

   /**
    * Get only the cursor belonging to the current scope
    * @param call context
    * @return
    */
   public List<PlsqlCodeCompletionItem> getRelevantCursors(PlsqlContext context) {
      ArrayList<PlsqlCodeCompletionItem> cursors = new ArrayList<PlsqlCodeCompletionItem>();
      PlsqlBlockFactory blockFactory = context.getBlockFactory();
      if (blockFactory != null) {
         List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
         for (int i = 0; i < blocks.size(); i++) {
            PlsqlBlock block = blocks.get(i);
            if (block.getType() == PlsqlBlockType.PACKAGE_BODY) {
               //candidate. Check if the cursor is inside this package
               int pos = context.getCaretOffset();
               if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
                  extractCursorsFromBlock(block, pos, cursors);
               }
            }
         }
      }
      return cursors;
   }

   private void extractCursorsFromBlock(PlsqlBlock parent, int pos, List<PlsqlCodeCompletionItem> cursors) {
      List<PlsqlBlock> childBlocks = parent.getChildBlocks();
      for (int i = 0; i < childBlocks.size(); i++) {
         PlsqlBlock block = childBlocks.get(i);
         if (block.getType() == PlsqlBlockType.CURSOR && block.getEndOffset() < pos) {
            cursors.add(new PlsqlCodeCompletionItem(block.getName(), CompletionItemType.CURSOR));
         } else if (block.getType() == PlsqlBlockType.FUNCTION_IMPL
                 || block.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
               extractCursorsFromBlock(block, pos, cursors);
            }
         }
      }
   }

   /**
    * Get only the type declarations belonging to the current scope
    * @param context
    * @return
    */
   public List<PlsqlCodeCompletionItem> getRelevantTypes(PlsqlContext context) {
      ArrayList<PlsqlCodeCompletionItem> types = new ArrayList<PlsqlCodeCompletionItem>();
      PlsqlBlockFactory blockFactory = context.getBlockFactory();
      if (blockFactory != null) {
         List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
         for (int i = 0; i < blocks.size(); i++) {
            PlsqlBlock block = blocks.get(i);
            if (block.getType() == PlsqlBlockType.PACKAGE_BODY) {
               //candidate. Check if the cursor is inside this package
               int pos = context.getCaretOffset();
               if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
                  extractTypesFromBlock(block, context, types);
               }
            }
         }
      }
      return types;
   }

   private void extractTypesFromBlock(PlsqlBlock parent, PlsqlContext context, List<PlsqlCodeCompletionItem> types) {
      //Get the type declarations that are there in the block.
      int pos = context.getCaretOffset();
      TokenSequence<PlsqlTokenId> ts = context.getTokenSequence();
      ts.move(parent.getStartOffset());
      Token<PlsqlTokenId> tokenPre = null;

      //If the block is a package body we will parse upto the first method implementation for type declarations
      if (parent.getType() == PlsqlBlockType.PACKAGE_BODY) {
         int endOffset = getStartMethodOffset(parent);
         if (pos < endOffset) {
            endOffset = pos;
         }
         //find the IS part of the method and use that as the end position
         while (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            if (ts.offset() >= endOffset) {
               break;
            }

            if (tokenID == PlsqlTokenId.KEYWORD && "TYPE".equalsIgnoreCase(token.toString()) && (tokenPre == null || tokenPre.id() == PlsqlTokenId.WHITESPACE)) {
               if (getNextNonWhitespace(ts)) {
                  token = ts.token();
                  types.add(new PlsqlCodeCompletionItem(token.toString(), CompletionItemType.TYPE));
               }
            }
            tokenPre = token;
         }
      } else {
         //This block is a method implementation. We will parse upto the 'BEGIN' clause here.
         while (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            if (ts.offset() >= pos) { //Dont search beyond the offset of the current context
               break;
            }

            if (tokenID == PlsqlTokenId.KEYWORD && "BEGIN".equalsIgnoreCase(token.toString())) {
               break;
            }

            if (tokenID == PlsqlTokenId.KEYWORD && "TYPE".equalsIgnoreCase(token.toString()) && (tokenPre == null || tokenPre.id() == PlsqlTokenId.WHITESPACE)) {
               if (getNextNonWhitespace(ts)) {
                  token = ts.token();
                  types.add(new PlsqlCodeCompletionItem(token.toString(), CompletionItemType.TYPE));
               }
            }
            tokenPre = token;
         }
      }

      List<PlsqlBlock> childBlocks = parent.getChildBlocks();
      for (int i = 0; i < childBlocks.size(); i++) {
         PlsqlBlock block = childBlocks.get(i);
         if (block.getType() == PlsqlBlockType.FUNCTION_IMPL
                 || block.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
               extractTypesFromBlock(block, context, types);
            }
         }
      }
   }

   /**
    * Get the start offset of the first child method implementation
    * @param parent
    * @return
    */
   private int getStartMethodOffset(PlsqlBlock parent) {
      int start = parent.getEndOffset();
      List<PlsqlBlock> childBlocks = parent.getChildBlocks();
      for (int i = 0; i < childBlocks.size(); i++) {
         PlsqlBlock block = childBlocks.get(i);
         if (block.getType() == PlsqlBlockType.FUNCTION_IMPL
                 || block.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            if (block.getStartOffset() < start) {
               start = block.getStartOffset();
            }
         }
      }

      return start;
   }

   /**
    * Get the columns of the type declaration
    * @param parent
    * @param context
    * @return
    */
   public List getTypeColumns(String parent, PlsqlContext context) {
      List<PlsqlCodeCompletionItem> columns = new ArrayList<PlsqlCodeCompletionItem>();
      PlsqlBlockFactory blockFactory = context.getBlockFactory();
      if (blockFactory != null) {
         List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
         for (int i = 0; i < blocks.size(); i++) {
            PlsqlBlock block = blocks.get(i);
            if (block.getType() == PlsqlBlockType.PACKAGE_BODY) {
               //candidate. Check if the cursor is inside this package
               int pos = context.getCaretOffset();
               if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
                  extractColumnsFromType(block, context, columns, parent);
               }
            }
         }
      }
      return columns;
   }

   private boolean extractColumnsFromType(PlsqlBlock parent, PlsqlContext context, List<PlsqlCodeCompletionItem> columns, String name) {
      //Get the type declarations that are there in the block.
      int pos = context.getCaretOffset();
      TokenSequence<PlsqlTokenId> ts = context.getTokenSequence();
      ts.move(parent.getStartOffset());
      Token<PlsqlTokenId> tokenPre = null;

      //If the block is a package body we will parse upto the first method implementation for type declarations
      if (parent.getType() == PlsqlBlockType.PACKAGE_BODY) {
         int endOffset = getStartMethodOffset(parent);
         if (pos < endOffset) {
            endOffset = pos;
         }
         //find the IS part of the method and use that as the end position
         while (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            if (ts.offset() >= endOffset) {
               break;
            }

            if (tokenID == PlsqlTokenId.KEYWORD && "TYPE".equalsIgnoreCase(token.toString()) && (tokenPre == null || tokenPre.id() == PlsqlTokenId.WHITESPACE)) {
               if (getNextNonWhitespace(ts)) {
                  token = ts.token();

                  if (token.toString().equals(name)) {
                     extractColumns(ts, columns, context);
                     return true;
                  }
               }
            }
            tokenPre = token;
         }
      } else {
         //This block is a method implementation. We will parse upto the 'BEGIN' clause here.
         while (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            if (ts.offset() >= pos) { //Dont search beyond the offset of the current context
               break;
            }

            if (tokenID == PlsqlTokenId.KEYWORD && "BEGIN".equalsIgnoreCase(token.toString())) {
               break;
            }

            if (tokenID == PlsqlTokenId.KEYWORD && "TYPE".equalsIgnoreCase(token.toString()) && (tokenPre == null || tokenPre.id() == PlsqlTokenId.WHITESPACE)) {
               if (getNextNonWhitespace(ts)) {
                  token = ts.token();

                  if (token.toString().equals(name)) {
                     extractColumns(ts, columns, context);
                     return true;
                  }
               }
            }
            tokenPre = token;
         }
      }

      List<PlsqlBlock> childBlocks = parent.getChildBlocks();
      for (int i = 0; i < childBlocks.size(); i++) {
         PlsqlBlock block = childBlocks.get(i);
         if (block.getType() == PlsqlBlockType.FUNCTION_IMPL
                 || block.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
               if (extractColumnsFromType(block, context, columns, name)) {
                  return true;
               }
            }
         }
      }
      return false;
   }

   /**
    * Method that will actually extract the columns from the type declaration
    * @param ts
    * @param columns
    * @param context
    */
   private void extractColumns(TokenSequence<PlsqlTokenId> ts, List<PlsqlCodeCompletionItem> columns, PlsqlContext context) {
      DatabaseConnection databaseConnection = connectionProvider.getPooledDatabaseConnection(false);
      try {
         while (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenId = token.id();

            if (tokenId == PlsqlTokenId.KEYWORD && "RECORD".equalsIgnoreCase(token.toString())) {
               while (ts.moveNext()) {
                  token = ts.token();
                  tokenId = token.id();

                  if (tokenId == PlsqlTokenId.LPAREN || (tokenId == PlsqlTokenId.OPERATOR && token.toString().equals(","))) {
                     if (getNextNonWhitespace(ts)) {
                        token = ts.token();
                        columns.add(new PlsqlCodeCompletionItem(token.toString(), CompletionItemType.COLUMN));
                     }
                  }

                  if (tokenId == PlsqlTokenId.OPERATOR && token.toString().equals(";")) {
                     break;
                  }
               }

               break;
            } else if (tokenId == PlsqlTokenId.KEYWORD && "TABLE".equalsIgnoreCase(token.toString())) {
               if (getNextNonWhitespace(ts)) {
                  token = ts.token();
                  if (tokenId == PlsqlTokenId.KEYWORD && "OF".equalsIgnoreCase(token.toString())) {
                     if (getNextNonWhitespace(ts)) {
                        token = ts.token();
                        tokenId = token.id();
                        String text = token.toString();

                        if (tokenId == PlsqlTokenId.IDENTIFIER) {
                           //Can be a table%rowtype or another type declaration
                           if (ts.moveNext()) {
                              token = ts.token();
                              tokenId = token.id();
                              if (tokenId == PlsqlTokenId.OPERATOR && token.toString().equals("%")) {
                                 if (ts.moveNext()) {
                                    token = ts.token();
                                    tokenId = token.id();
                                    if (tokenId == PlsqlTokenId.KEYWORD && token.toString().equals("ROWTYPE")) {
                                       if (cache.isView(text) || cache.isTable(text)) {
                                          //Table or view
                                          List<PlsqlCodeCompletionColumnItem> items = getColumnObjects(text, databaseConnection);
                                          for (int i = 0; i < items.size(); i++) {
                                             columns.add(items.get(i));
                                          }
                                       } else {
                                          //Can be a cursor
                                          List results = getCursorColumns(text, context);
                                          for (int i = 0; i < results.size(); i++) {
                                             columns.add((PlsqlCodeCompletionItem) results.get(i));
                                          }
                                       }
                                    }
                                 }
                              } else {
                                 //Check for another type declaration
                                 List items = getTypeColumns(text, context);
                                 for (int i = 0; i < items.size(); i++) {
                                    columns.add((PlsqlCodeCompletionItem) items.get(i));
                                 }
                              }
                           }
                        }
                     }
                  }
               }

               break;
            }
         }
      } finally {
         connectionProvider.releaseDatabaseConnection(databaseConnection);
      }
   }


      /**
       * Get Return next non whitespace token
       * @param ts
       * @return
       */
   private

    boolean getNextNonWhitespace(TokenSequence<PlsqlTokenId> ts) {
      boolean moveNext = ts.moveNext();
      Token<PlsqlTokenId> tmp = ts.token();


      while (moveNext) {
         if (tmp.id() == PlsqlTokenId.WHITESPACE) {
            moveNext = ts.moveNext();
            tmp = ts.token();
         } else {
            break;
         }
      }

      return moveNext;
   }

   /**
    * Get the columns in the select list of the cursor
    * @param parent
    * @param context
    * @return
    */
   public List getCursorColumns(String parent, PlsqlContext context) {
      List<PlsqlCodeCompletionItem> columns = new ArrayList<PlsqlCodeCompletionItem>();
      PlsqlBlockFactory blockFactory = context.getBlockFactory();
      if (blockFactory != null) {
         List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
         for (int i = 0; i < blocks.size(); i++) {
            PlsqlBlock block = blocks.get(i);
            if (block.getType() == PlsqlBlockType.PACKAGE_BODY) {
               //candidate. Check if the cursor is inside this package
               int pos = context.getCaretOffset();
               if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
                  extractColumnsFromCursor(block, context, columns, parent);
               }
            }
         }
      }
      return columns;
   }

   private boolean extractColumnsFromCursor(PlsqlBlock parent, PlsqlContext context, List<PlsqlCodeCompletionItem> columns, String name) {
      List<PlsqlBlock> childBlocks = parent.getChildBlocks();
      int pos = context.getCaretOffset();
      for (int i = 0; i < childBlocks.size(); i++) {
         PlsqlBlock block = childBlocks.get(i);
         if (block.getType() == PlsqlBlockType.CURSOR && block.getEndOffset() < pos && block.getName().equals(name)) {
            getColumnsOfCursor(block, context, columns);
            return true;
         } else if (block.getType() == PlsqlBlockType.FUNCTION_IMPL
                 || block.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
               if (extractColumnsFromCursor(block, context, columns, name)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void getColumnsOfCursor(PlsqlBlock block, PlsqlContext context, List<PlsqlCodeCompletionItem> columns) {
      //Find the end offset that we need to consider, we ignore the WHERE clause which is not required for our purpose
      TokenSequence<PlsqlTokenId> ts = context.getTokenSequence();
      ts.move(block.getStartOffset());
      int end = block.getEndOffset();
      int start = block.getStartOffset();
      while (ts.moveNext()) {
         Token<PlsqlTokenId> token = ts.token();
         PlsqlTokenId tokenId = token.id();

         if (ts.offset() >= block.getEndOffset()) {
            break;
         }

         if (tokenId == PlsqlTokenId.KEYWORD && "SELECT".equalsIgnoreCase(token.toString())) {
            start = ts.offset();
         }

         if (tokenId == PlsqlTokenId.KEYWORD && "WHERE".equalsIgnoreCase(token.toString())) {
            ts.movePrevious();
            end = ts.offset();
            break;
         }
      }

      BaseDocument doc = context.getDocument();
      String sql = "";
      try {
         sql = doc.getText(start, end - start);
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }

      if (!sql.equals("")) {
         sql = context.translateAliases(sql);
         DatabaseConnection dbConnection = connectionProvider.getPooledDatabaseConnection(true);
         try {
            String[] results = PlsqlDataAccessor.getMetaData(dbConnection.getJDBCConnection(), sql);
            if (results != null) {
               for (int i = 0; i < results.length; i++) {
                  columns.add(new PlsqlCodeCompletionItem(results[i].toString().toLowerCase(Locale.ENGLISH), CompletionItemType.COLUMN));
               }
            }
         } finally {
            connectionProvider.releaseDatabaseConnection(dbConnection);
         }
      }
   }

   /**
    * Get only the parameters if the current scope is a method implementation
    * @param call context
    * @return
    */
   public List<PlsqlCodeCompletionItem> getVariables(PlsqlContext context) {
      ArrayList<PlsqlCodeCompletionItem> params = new ArrayList<PlsqlCodeCompletionItem>();
      PlsqlBlockFactory blockFactory = context.getBlockFactory();
      if (blockFactory != null) {
         List<PlsqlBlock> blocks = blockFactory.getBlockHierarchy();
         for (int i = 0; i < blocks.size(); i++) {
            PlsqlBlock block = blocks.get(i);
            if (block.getType() == PlsqlBlockType.PACKAGE_BODY) {
               int pos = context.getCaretOffset();
               if (pos > block.getStartOffset() && pos < block.getEndOffset()) {
                  TokenSequence<PlsqlTokenId> ts = context.getTokenSequence();
                  int start = block.getStartOffset();
                  int end = getStartMethodOffset(block);
                  if (pos < end) {
                     end = pos;
                  }
                  ts.move(start);

                  //Get variable declarations of the package body
                  while (ts.moveNext()) {
                     Token<PlsqlTokenId> token = ts.token();
                     PlsqlTokenId tokenId = token.id();

                     if (ts.offset() >= end) {
                        break;
                     }

                     //look for local variables now
                     if (tokenId == PlsqlTokenId.KEYWORD && ("NUMBER".equalsIgnoreCase(token.toString())
                             || "VARCHAR2".equalsIgnoreCase(token.toString()) || "DATE".equalsIgnoreCase(token.toString())
                             || "BOOLEAN".equalsIgnoreCase(token.toString()) || "EXCEPTION".equalsIgnoreCase(token.toString())
                             || "TYPE".equalsIgnoreCase(token.toString()) || "ROWTYPE".equalsIgnoreCase(token.toString()))) {
                        int offset = ts.offset();
                        if (getNextNonWhitespace(ts)) {
                           token = ts.token();
                           tokenId = token.id();

                           if (tokenId == PlsqlTokenId.LPAREN || tokenId == PlsqlTokenId.OPERATOR
                                   && (";".equals(token.toString()) || ":".equals(token.toString()))) {
                              ts.move(offset);

                              boolean isVariableName = false;
                              while (ts.movePrevious()) {
                                 token = ts.token();
                                 tokenId = token.id();

                                 if (tokenId == PlsqlTokenId.WHITESPACE) {
                                    isVariableName = true;
                                 }

                                 if (isVariableName && tokenId == PlsqlTokenId.IDENTIFIER) {
                                    params.add(new PlsqlCodeCompletionItem(token.toString(), CompletionItemType.VARIABLE));
                                    break;
                                 }

                                 if (isVariableName && tokenId != PlsqlTokenId.WHITESPACE
                                         && (!"CONSTANT".equalsIgnoreCase(token.toString()))) {
                                    //not a white space or an identifier
                                    break;
                                 }
                              }

                              //Reset token hierarchy
                              ts.move(offset);
                              ts.moveNext();
                           }
                        }
                     }
                  }

                  List<PlsqlBlock> childBlocks = block.getChildBlocks();
                  for (int j = 0; j < childBlocks.size(); j++) {
                     PlsqlBlock temp = childBlocks.get(j);
                     if (temp.getType() == PlsqlBlockType.FUNCTION_IMPL
                             || temp.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
                        if (pos > temp.getStartOffset() && pos < temp.getEndOffset()) {
                           extractVariablesFromMethod(temp, context, params);
                        }
                     }
                  }
               }
            }
         }
      }

      return params;
   }

   /**
    * Extract the parameters from the given block and it's child methods
    * @param block
    * @param context
    * @param params
    */
   private void extractVariablesFromMethod(PlsqlBlock block, PlsqlContext context, ArrayList<PlsqlCodeCompletionItem> params) {
      TokenSequence<PlsqlTokenId> ts = context.getTokenSequence();
      int start = block.getStartOffset();
      int end = context.getCaretOffset();
      ts.move(start);
      boolean isParams = true;

      while (ts.moveNext()) {
         Token<PlsqlTokenId> token = ts.token();
         PlsqlTokenId tokenId = token.id();

         if (ts.offset() >= end
                 || (tokenId == PlsqlTokenId.KEYWORD && "BEGIN".equalsIgnoreCase(token.toString()))) {
            break;
         }

         //finish looking for parameters
         if (tokenId == PlsqlTokenId.RPAREN
                 || (tokenId == PlsqlTokenId.KEYWORD && "IS".equalsIgnoreCase(token.toString()))) {
            if (isParams) {
               isParams = false;
            }
         }

         //possible parameter
         if (isParams && (tokenId == PlsqlTokenId.LPAREN
                 || (tokenId == PlsqlTokenId.OPERATOR && ",".equals(token.toString())))) {
            if (getNextNonWhitespace(ts)) {
               token = ts.token();
               params.add(new PlsqlCodeCompletionItem(token.toString(), CompletionItemType.PARAMETER));
            }
         }

         //look for local variables now
         if ((!isParams) && tokenId == PlsqlTokenId.KEYWORD && ("NUMBER".equalsIgnoreCase(token.toString())
                 || "VARCHAR2".equalsIgnoreCase(token.toString()) || "DATE".equalsIgnoreCase(token.toString())
                 || "BOOLEAN".equalsIgnoreCase(token.toString()) || "EXCEPTION".equalsIgnoreCase(token.toString())
                 || "TYPE".equalsIgnoreCase(token.toString()) || "ROWTYPE".equalsIgnoreCase(token.toString()))) {
            int offset = ts.offset();
            if (getNextNonWhitespace(ts)) {
               token = ts.token();
               tokenId = token.id();

               if (tokenId == PlsqlTokenId.LPAREN || tokenId == PlsqlTokenId.OPERATOR && ";".equals(token.toString())) {
                  ts.move(offset);

                  boolean isVariableName = false;
                  while (ts.movePrevious()) {
                     token = ts.token();
                     tokenId = token.id();

                     if (tokenId == PlsqlTokenId.WHITESPACE) {
                        isVariableName = true;
                     }

                     if (isVariableName && tokenId == PlsqlTokenId.IDENTIFIER) {
                        params.add(new PlsqlCodeCompletionItem(token.toString(), CompletionItemType.VARIABLE));
                        break;
                     }

                     if (isVariableName && tokenId != PlsqlTokenId.WHITESPACE) {
                        //not a white space or an identifier
                        break;
                     }
                  }

                  //Reset token hierarchy
                  ts.move(offset);
                  ts.moveNext();
               }
            }
         }
      }

      List<PlsqlBlock> childBlocks = block.getChildBlocks();
      for (int j = 0; j < childBlocks.size(); j++) {
         PlsqlBlock temp = childBlocks.get(j);
         if (temp.getType() == PlsqlBlockType.FUNCTION_IMPL
                 || temp.getType() == PlsqlBlockType.PROCEDURE_IMPL) {
            int pos = context.getCaretOffset();
            if (pos > temp.getStartOffset() && pos < temp.getEndOffset()) {
               extractVariablesFromMethod(temp, context, params);
            }
         }
      }
   }
}
   
