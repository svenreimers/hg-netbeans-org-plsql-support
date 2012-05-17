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

import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import java.util.HashMap;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.openide.util.Exceptions;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.netbeans.api.lexer.Token;
import org.openide.util.Lookup;

/**
 *
 * @author jocase
 */
public class PlsqlContext {
   
   private BaseDocument doc;
   private int caretOffset;
   private int lineStartPos;
   
   private boolean showColumns;
   private boolean showFunctions;
   private boolean showProcedures;
   private boolean showAllTables;
   private boolean showAllViews;
   private boolean showExceptions;
   private boolean showTypes;
   private boolean showOnlyViewsAndTables;
   private boolean showSequences;
   private HashMap tableAlias;
   private List<String> selectViewsWithoutAlias = null;
   private static final boolean contextSensitive = !"false".equalsIgnoreCase(System.getProperty("contextSensitiveCodeCompletion"));


   public class MethodCallContext {
      public String pkgName;
      public String methodName;
      public int argPos;
      
      public MethodCallContext() {
         pkgName = methodName = null;
         argPos = -1;
      };
   }
   
   
   public PlsqlContext(Document document, int caretOffset) {
      showColumns=false;
      showFunctions=false;
      showProcedures=false;
      showAllTables=false;
      showAllViews=false;
      showSequences=false;
      showExceptions=false;
      showTypes=false;
      tableAlias = new HashMap();
      selectViewsWithoutAlias = new ArrayList<String>();
      try {
         this.doc = (BaseDocument) document;
         this.caretOffset = caretOffset;
         this.lineStartPos = Utilities.getRowStart(doc, caretOffset);
         String line = removePrefix(doc.getText(lineStartPos, (caretOffset - lineStartPos)));
         String trimmedLine = line.trim();
         String upper = trimmedLine.toUpperCase(Locale.ENGLISH);
         
         tableAlias = createAliasMap(caretOffset);
         if(contextSensitive && !isDynamicSQL()) {
            showOnlyViewsAndTables = showAllViews = showAllTables = showTables(line); //check if the previous keyword is from/into/etc

            if(!showAllViews) {
               if(trimmedLine.length()==0) { 
                  showProcedures = true; //this should really be changed to check for previous operator==";" or keywords THEN, BEGIN, etc
                  showFunctions = true;
                  showAllViews = true;
                  showSequences = true;
                  showAllTables = true;
               } else if(isAssignment(line)) {
                  showFunctions = true;
                  showSequences = true;
               } else {
                  showProcedures = true;                  
               }
            }
            if(upper.endsWith("_") || upper.endsWith(" OUT") || upper.endsWith(" IN") || upper.endsWith(" RETURN")) {
               showAllViews = showAllTables = showTypes = true;
            } else if(upper.endsWith("EXECUTE") || upper.endsWith("EXEC")) {
               showProcedures = true;
            } else if(upper.endsWith("WHEN")) {
               showExceptions = isExceptionStmt(caretOffset);
            }
            showColumns = isSelectStmt(caretOffset);
         } else {
            showAllViews = showAllTables = showProcedures = showFunctions = true;
         }
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
   }

   public int getCaretOffset() {
      return caretOffset;
   }
   
   public BaseDocument getDocument() {
      return doc;
   }
   
   @SuppressWarnings("unchecked")
   public TokenSequence<PlsqlTokenId> getTokenSequence() {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);      
      return tokenHierarchy.tokenSequence(PlsqlTokenId.language());
   }
   
   public PlsqlBlockFactory getBlockFactory() {
      Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof Lookup.Provider)
         return ((Lookup.Provider)obj).getLookup().lookup(PlsqlBlockFactory.class);
      return null;
   }
   
   private int findNextFrom(TokenSequence<PlsqlTokenId> ts) {
      while (ts.moveNext()) {
         Token<PlsqlTokenId> token = ts.token();
         PlsqlTokenId tokenID = token.id();
         String value = token.toString();
         if(tokenID == PlsqlTokenId.OPERATOR && ";".equals(value)) {
            return -1;
         } else if (tokenID == PlsqlTokenId.KEYWORD) {
            if("NULL".equalsIgnoreCase(value)) {
               //ignore NULL since this is a valid SQL keyword
            } else if("FROM".equalsIgnoreCase(value)) {
               return ts.offset()+5;
            } 
         }
      }
      return -1;
   }

   /*Find the position of FROM/UPDATE/INSERT INTO
    * 
    */
   private int findTableAndViewListStartPosition(TokenSequence<PlsqlTokenId> ts) {
      boolean whereFound = false;
      boolean setFound = false;
      int rParenCount=0;
      int lParenCount=0;
      PlsqlTokenId previousKeyWordTokenId = null;
      while (ts.movePrevious()) {
         Token<PlsqlTokenId> token = ts.token();
         PlsqlTokenId tokenId = token.id();
         String value = token.toString();
         if(tokenId == PlsqlTokenId.OPERATOR && ";".equals(value)) {
            return -1;
         } else if(tokenId == PlsqlTokenId.RPAREN) {
            rParenCount++;
         } else if(tokenId == PlsqlTokenId.LPAREN) {
            if(rParenCount>0)
               rParenCount--;
            else {
               lParenCount++;
               previousKeyWordTokenId = null;
            }
         } else if(tokenId == PlsqlTokenId.KEYWORD || "SET".equalsIgnoreCase(value)) {
            if(rParenCount==0) {
               if("WHERE".equalsIgnoreCase(value) || "BY".equalsIgnoreCase(value)) {
                  //set for where as well as order by and group by
                  whereFound = true;
               } else if("SET".equalsIgnoreCase(value)) {
                   if(whereFound){
                    return ts.offset()+5; 
                   }
                  setFound = true;
               } else if("FROM".equalsIgnoreCase(value)) {
                 if(whereFound)
                    return ts.offset()+5; 
                 else 
                    return -1; //found from before where/order by/group by)...
               } else if("UPDATE".equalsIgnoreCase(value)) {
                 if(whereFound || setFound)
                    return ts.offset()+7; 
                 else 
                    return -1; //found update before where or set...
               } else if(lParenCount==1 && "INTO".equalsIgnoreCase(value)) { //find "INSERT INTO xxx (" type of statements
                  if(previousKeyWordTokenId==null) //no other keywords between INTO and the parenthesis 
                     return ts.offset()+5;
               }
            }
            previousKeyWordTokenId = tokenId;
         }
      }
      return -1;
   }

   private int findSubSelectStart(TokenSequence<PlsqlTokenId> ts) {
      if(findStmtStart(ts, "SELECT")>-1) {
         if(ts.movePrevious()) {
            Token<PlsqlTokenId> token = ts.token();
            while(token.id()==PlsqlTokenId.WHITESPACE && ts.movePrevious())
               token = ts.token();
            if( token.id()==PlsqlTokenId.LPAREN) {
               return ts.offset();
            }
         }
      } 
      return -1;
   }
   
   private int findStmtStart(TokenSequence<PlsqlTokenId> ts, String keyWord) {
      int parenCount=0;
      while (ts.movePrevious()) {
         Token<PlsqlTokenId> token = ts.token();
         PlsqlTokenId tokenID = token.id();
         String value = token.toString();
         if(tokenID == PlsqlTokenId.OPERATOR && ";".equals(value)) {
            return -1;
         } else if(tokenID == PlsqlTokenId.RPAREN) {
            parenCount++;
         } else if(tokenID == PlsqlTokenId.LPAREN) {
            if(parenCount>0)
               parenCount--;
         } else if (parenCount==0 && tokenID == PlsqlTokenId.KEYWORD) {
            if(keyWord.equalsIgnoreCase(value)) {
               return ts.offset();
            } 
         }
      }
      return -1;
   }
   
   private HashMap<String, String> extractAliases(TokenSequence<PlsqlTokenId> ts) {
      HashMap<String, String> map = new HashMap<String, String>();
      int parenCount = 0;
      String viewName = null;
      String alias = null;
      final String SELECT_STATEMENT="select...";
        if(isUpdateStmt(ts.offset())){
            ts.moveIndex(1);            
        }
      while (ts.moveNext()) {
         Token<PlsqlTokenId> token = ts.token();
         PlsqlTokenId tokenID = token.id();
         String value = token.toString();
         if(tokenID == PlsqlTokenId.OPERATOR && (";".equals(value) || "=".equals(value))) {
            if("=".equals(value)) 
                viewName = null;
            else
                break;
         } else if(tokenID == PlsqlTokenId.DOT && viewName!=null) {
            //schema prefix for the object - ignore the schema
             if(!isInsertStmt(ts.offset()))
                viewName = null;
         } else if(tokenID == PlsqlTokenId.LPAREN) {
            parenCount++;
         } else if(tokenID == PlsqlTokenId.RPAREN) {
            if(parenCount==0) //end of statement
               break;
            parenCount--;
            if(parenCount==0 && viewName==null) //closing parenthesis. If viewName != null this is probably an insert into statement...
               viewName = SELECT_STATEMENT;
            else
                break;
         } else if(parenCount==0) {
            if(tokenID == PlsqlTokenId.IDENTIFIER || tokenID==PlsqlTokenId.STRING_LITERAL) {
               if(viewName==null) {
                  viewName = value;
               } else {
                  alias = value.startsWith("\"") ? value : value.toLowerCase(Locale.ENGLISH);
               }
            } else if(tokenID == PlsqlTokenId.OPERATOR && ",".equals(value)) {
               if(alias!=null)
                  map.put(alias, viewName);
               else if(viewName!=null && viewName!=SELECT_STATEMENT) //Yes - this should be "==..." and not equals(...)
                  selectViewsWithoutAlias.add(viewName);
               viewName = alias = null;
            } else if(tokenID==PlsqlTokenId.KEYWORD || "SET".equals(value)) {
                   break;
            }
         }
      }
      if(alias!=null)
         map.put(alias, viewName);
      else if(viewName!=null && viewName!=SELECT_STATEMENT)
         selectViewsWithoutAlias.add(viewName);
      return map;
   }
  
   private boolean isSelectStmt(int caretOffset) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      ts.move(caretOffset);
      return findStmtStart(ts, "SELECT")>-1;
   }
   
   private boolean isUpdateStmt(int caretOffset) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      ts.move(caretOffset);
      return findStmtStart(ts, "UPDATE")>-1;
   }
   
   private boolean isInsertStmt(int caretOffset) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      ts.move(caretOffset);
      return findStmtStart(ts, "INSERT")>-1;
   }

   private boolean isExceptionStmt(int caretOffset) {
      //for now just return true...
      return true;
   }
   
   private HashMap createAliasMap(int caretOffset) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      return createAliasMap(ts, caretOffset);
   }

   
   private HashMap<String, String> createAliasMap(TokenSequence<PlsqlTokenId> ts, int caretOffset) {
      if (ts != null) {
         //move offset
         ts.move(caretOffset);
         int startpos = findNextFrom(ts);
         if(startpos<0) {
            ts.move(caretOffset);
            startpos = findTableAndViewListStartPosition(ts);
         }
         if(startpos > -1) {
            HashMap<String, String> result = extractAliases(ts);
            ts.move(caretOffset);
            startpos = findSubSelectStart(ts);
            if(startpos > -1) { //
               result.putAll(createAliasMap(ts, startpos));
            }
            return result;
         }
      }
      return new HashMap<String, String>();
   }

   private void printTokens(TokenSequence<PlsqlTokenId> ts) {
        Token<PlsqlTokenId> token = null;
        while (ts.moveNext()) {
            token = ts.token();
            System.out.println("Token " + token.id().name() + " Text:" + token.toString() + " Offset:"+ts.offset());
        }
    }

   private boolean showTables(String line) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      ts.moveStart();
      if (ts != null) {
         //move offset
         int index = ts.move(caretOffset);
         //move to the correct token offset (not to the middle of a token)
         ts.move(caretOffset+index);
         int parenCount = 0;
         int whitespaceCount = 0;
         while (ts.movePrevious()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            String value = token.toString();
            if(tokenID == PlsqlTokenId.RPAREN) {
               parenCount++;
            } else if(tokenID == PlsqlTokenId.LPAREN) {
               if(parenCount==0) //end of statement
                  return false;
               parenCount--;
            } else if(tokenID == PlsqlTokenId.OPERATOR && (";".equals(value) || "IN".equalsIgnoreCase(value))) {               
               return false;
            } else if(parenCount==0 && whitespaceCount>0 && tokenID==PlsqlTokenId.KEYWORD) { //Added whitespace count to prevent matching uncompleted words which are keywords
               return ("FROM".equalsIgnoreCase(value) ||
                       "INTO".equalsIgnoreCase(value) ||
                       "VIEW".equalsIgnoreCase(value) ||
                       "TABLE".equalsIgnoreCase(value) ||
                       "COLUMN".equalsIgnoreCase(value) ||
                       "UPDATE".equalsIgnoreCase(value) ||
                       "DESC".equalsIgnoreCase(value) ||
                       "DESCRIBE".equalsIgnoreCase(value));
            }

            if (tokenID == PlsqlTokenId.WHITESPACE) {
               whitespaceCount++;
            }
         }
      }
      //keyword search is a bit dodgy. Use string comparison as well...
      line = line.trim().toUpperCase(Locale.ENGLISH);
      return line.endsWith("INTO") || line.endsWith("FROM") || line.endsWith("UPDATE") || line.endsWith("DESC") || line.endsWith("DESCRIBE");
   }
   
   public String getDefine(String define) {
      if(define.startsWith("&")) {
         PlsqlBlockFactory blocks = getBlockFactory();
         if(blocks!=null) {
            String defineValue = blocks.getDefine(define);
            if(defineValue!=null)
               return defineValue.toLowerCase(Locale.ENGLISH);
         }
      }
      return null;
   } 
   

   //Find the type of the variable - if this is indeed a variable...
   //A variable declaration will be identified as anything that looks like "name type;" 
   //This might of course not always be a variable declaration, but it will work in most cases.
   //The functionality will only cater to local variables and method parameters.
   //@param name of variable to locate
   //@return type of the variable - or null if no declaration found
   private String findVariableDeclaration(String variableName) {
      String typeDeclaration = "";
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      PlsqlTokenId previousTokenID = null;
      int matchCount=0;
      if (ts != null) {
         //move offset
         ts.move(caretOffset);
         while (ts.movePrevious()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            String txt = token.toString();
            if(tokenID == PlsqlTokenId.OPERATOR) {
               if(";".equals(txt) || ",".equals(txt))
                  typeDeclaration=";";
               else if("%".equals(txt))
                  typeDeclaration = txt + typeDeclaration;
               else
                  typeDeclaration="";
            } else if(tokenID == PlsqlTokenId.RPAREN) {
               typeDeclaration=";";
            } else if(tokenID == PlsqlTokenId.IDENTIFIER) {
               //potential declaration found - check to see if the typeDeclaration matches our pattern
               //The pattern would be: SPACE + [IN|OUT|IN OUT] TYPE;
               //if it doesn't match then simply add the identitier to the declarationpattern.
               if(variableName.equalsIgnoreCase(txt)) {
                  if(typeDeclaration.length()>0 && typeDeclaration.endsWith(";") && typeDeclaration.startsWith(" ")) {
                     String type = typeDeclaration.substring(1, typeDeclaration.length()-1);
                     if(type.startsWith("IN "))
                        type = type.substring(3);
                     if(type.startsWith("OUT "))
                        type = type.substring(4);
                     if(type.indexOf(" ")==-1)
                        return type;
                  }
                  typeDeclaration = "";
               } else 
                  typeDeclaration = txt + typeDeclaration;
            } else if(tokenID == PlsqlTokenId.WHITESPACE) {
               if(previousTokenID!=PlsqlTokenId.WHITESPACE && !";".equals(typeDeclaration)) {
                  typeDeclaration = " " + typeDeclaration;
               }
            } else if(tokenID == PlsqlTokenId.KEYWORD) {
               if("FUNCTION".equalsIgnoreCase(txt) || "PROCEDURE".equalsIgnoreCase(txt)) //break at the start of a function/procedure
                  return null;
               else 
                  typeDeclaration = txt+typeDeclaration;
            } else if(tokenID == PlsqlTokenId.DOT) {
               typeDeclaration = txt+typeDeclaration;
            } else {
               typeDeclaration="";
            }     
            previousTokenID = tokenID;
         }
      }
      return null;
   }

   //Translate an alias or variable name to the corresponding table/view name
   //@param parent alias and/or variable name to translate
   //@return translated value (e.g. table name instead of the table alias). Same as input if no matching value found
   String translateAlias(String parent) {
      if(parent.startsWith("&"))
         return getDefine(parent);
      String str = (String)tableAlias.get(parent.toLowerCase(Locale.ENGLISH));
      if(str!=null) {
         if(str.startsWith("&"))
            return getDefine(str.toUpperCase(Locale.ENGLISH));
         return str;
      }
      str = findVariableDeclaration(parent);
      if(str!=null) {
         if(str.toUpperCase(Locale.ENGLISH).endsWith("%ROWTYPE"))
            str = str.substring(0, str.length()-8);
         if(str.startsWith("&"))
             return getDefine(str.toUpperCase(Locale.ENGLISH));
         return str;
      }
      return null;
   }

   String translateAliases(String plsqlString) {
        if (plsqlString.indexOf("&") < 0) {
            return plsqlString;
        }

        StringBuilder newString = new StringBuilder();
        for (int i = 0; i < plsqlString.length(); i++) {
            char c = plsqlString.charAt(i);
            if (c == '&') {
                for (int j = i + 1; j < plsqlString.length(); j++) {
                    char nextChar = plsqlString.charAt(j);
                    if (Character.isJavaIdentifierPart(nextChar) && j == plsqlString.length() - 1) { //we have reached the end of the text

                        nextChar = '.'; //this will make sure that the correct sustitution is made below by emulating an additional character

                        j = j + 1;
                    }
                    if (!Character.isJavaIdentifierPart(nextChar)) { //potential end of substitutionvariable

                        if (j > i + 1) { //substituion variable found

                            String name = plsqlString.substring(i + 1, j);
                            String value = getDefine('&' + name);
                            if (value != null)
                                name = value;
                            
                            newString.append(name);
                            if (nextChar == '.') {
                                i = j;
                            } else {
                                i = j - 1;
                            }
                        } else {
                            newString.append(c);
                        }
                        break;
                    }
                }
            } else {
                newString.append(c);
            }
        }
        return newString.toString();
    }

   //Remove last word on the line (or two words if seperated by a dot)
   //Example: 
   //input:   "user := Fnd_User_API.Get"
   //output:  "user := "
   //@param str The input string
   //@return Trimmed string
   private String removePrefix(String str) {
      boolean ignoreDot = true;
      char previousChar = 0;
      for(int i = str.length()-1; i>=0; i--) {
         char ch = str.charAt(i);
         if(!Character.isJavaIdentifierPart(ch)) {
            if(ch!='.' || !ignoreDot) {
               if(Character.isJavaIdentifierStart(previousChar))
                  return str.substring(0, i+1);
               else
                  return str;
            }
            ignoreDot=false;
         }
         previousChar = ch;
      }
      return "";
   }
   
   
//   private String extractStatement() {
//         doc.f
//         this.caretOffset = caretOffset;
//         this.lineStartPos = Utilities.getRowStart(doc, caretOffset);
//         String line = removePrefix(doc.getText(lineStartPos, (caretOffset - lineStartPos)));
//   }
   
   
   public String getFilterString() {
      try {
         String line = doc.getText(lineStartPos, caretOffset - lineStartPos);

         if (line.length() == 0) {
            return "";
         }
         for (int i = line.length() - 1; i >= 0; i--) {
            if (!Character.isJavaIdentifierPart(line.charAt(i))) {
               return line.substring(i + 1);
            }
         }
         return line;
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
      return "";
   }
      
   public String getParentObject() {
      try {
         String line = doc.getText(lineStartPos, caretOffset - lineStartPos);

         boolean dotFound = false;
         boolean doubleDot = false;
         int endPos = line.length();
         int startPos = 0;
         char previousChar = 0;
         for(int i = line.length()-1; i>=0; i--) {
            char ch = line.charAt(i);
            if(!Character.isJavaIdentifierPart(ch) && ch!='"') {
               if(ch=='.' && !dotFound) {
                  dotFound = true;
                  if(i>0 && line.charAt(i-1)=='.') { //allow double . for define statements
                     doubleDot = true;
                     i=i-1;
                  }
                  endPos = i;
               } else {
                  if(!Character.isJavaIdentifierStart(previousChar) && !Character.isDigit(previousChar)) {
                     return null;
                  }
                  if(ch=='&') { //include & to get define statement substitution
                     if(!doubleDot) //define substitution only works if you have ..
                        return null;
                     startPos = i;
                  } else {
                     startPos = i+1;
                  }
                  break;
               }
            }
            previousChar = ch;
         }
         if(dotFound) {
            String name = line.substring(startPos, endPos);
            return name.length()>0 ? name : null;
         }            
      } catch (BadLocationException ex) {
         Exceptions.printStackTrace(ex);
      }
      return null;
   }
   
   
   private boolean isAssignment(String line) {
      String trimmedLine = line.trim().toUpperCase(Locale.ENGLISH);
      boolean endsWithSpace = Character.isWhitespace(line.charAt(line.length()-1));
      return ((trimmedLine.endsWith("=") || trimmedLine.endsWith("||") || trimmedLine.endsWith("+") ||
              trimmedLine.endsWith("-") || trimmedLine.endsWith("/") || trimmedLine.endsWith("*") ||
              trimmedLine.endsWith("(") || trimmedLine.endsWith(",") ||
              trimmedLine.endsWith("<>") || trimmedLine.endsWith("<") || trimmedLine.endsWith(">") ||
              (endsWithSpace && (trimmedLine.endsWith("AND") || trimmedLine.endsWith("OR") || trimmedLine.endsWith("SELECT") || trimmedLine.endsWith("IF") || trimmedLine.endsWith("WHERE") || trimmedLine.endsWith("BY") || trimmedLine.endsWith("RETURN")))));
   }
   
   public boolean showPackages() {
      return showFunctions || showProcedures || showExceptions;
   }
   
   public boolean showPackage(PlsqlCodeCompletionItem pkg) {
      return showFunctions || showProcedures;
   }

   public boolean showMethod(PlsqlCodeCompletionMethodItem method) {
      if(method.getType()==CompletionItemType.FUNCTION)
         return showFunctions;
      else 
         return showProcedures;
   }

   public boolean showExceptions() {
      return showExceptions;
   }
   
   public boolean showConstants() {
      return true;
   }
   
   public boolean showTypes() {
      return showTypes;
   }
   
   public boolean showSequences() {
      return showSequences;
   }

   public boolean showOnlyViewsAndTables() {
      return showOnlyViewsAndTables;
   }

   public boolean showViews() {
      return showAllViews;
   }

   public boolean showTables() {
      return showAllTables;
   }
   
   
   public boolean showView(PlsqlCodeCompletionItem view) {
      return showAllViews || tableAlias.containsKey(view.getText());
   }
   
   public boolean showTable(PlsqlCodeCompletionItem table) {
      return showAllTables || tableAlias.containsKey(table.getText());
   }
   
   public boolean showColums() {
      return showColumns;
   }
   
   public boolean showColumn(PlsqlCodeCompletionColumnItem column) {
      return showColumns;
   }
   
   public boolean showTableAliases() {
      return tableAlias.size()>0;
   }

   public HashMap getTableAliasMap() {
      return tableAlias;
   }
   
   public List getSelectListWithoutAlias() {
      return selectViewsWithoutAlias;
   }
   
   public MethodCallContext getContainingMethodCallContext() {
      //search backwards to find the enclosing method call
      MethodCallContext ctx = new MethodCallContext();
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         //move offset
         ts.move(caretOffset);
         int pCount = 0; //parenthesis level used to skip inner function calls
         int argCount = 0; //current argument position
         boolean insideString = false;
         while (ts.movePrevious()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            String txt = token.toString();
            if(tokenID == PlsqlTokenId.OPERATOR && ";".equals(txt))
               break;
            else if(tokenID == PlsqlTokenId.OPERATOR && ",".equals(txt))
               argCount++;
            else if(tokenID == PlsqlTokenId.RPAREN)
               pCount++;
            else if(tokenID == PlsqlTokenId.LPAREN) {
               if(pCount>0)
                  pCount--;
               else { //now we've found the method call
                  if(ts.movePrevious()) {
                     token = ts.token();
                     //skip white space
                     while(token.id()==PlsqlTokenId.WHITESPACE) {
                        ts.movePrevious();
                        token = ts.token();
                     }
                     //then identify method call
                     if(token.id()==PlsqlTokenId.IDENTIFIER) {
                        ctx.methodName = token.toString();
                        if(ts.movePrevious()) { 
                           token = ts.token();
                           if(token.id()==PlsqlTokenId.DOT) { //check if this is a package.method call
                              if(ts.movePrevious()) {
                                 token = ts.token();
                                 if(token.id()==PlsqlTokenId.IDENTIFIER) {
                                    ctx.pkgName = token.toString();
                                 }
                              }
                           } 
                           ctx.argPos = argCount;
                           return ctx;
                           
                        }
                     }
                     return null;
                  }
               }
            }
         }
      }
      
      return null;
   }

   private boolean isDynamicSQL() {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         //move offset
         ts.move(caretOffset);
         ts.moveNext();
         Token<PlsqlTokenId> token = ts.token();
         if (token != null && (token.id() == PlsqlTokenId.STRING_LITERAL || token.id() == PlsqlTokenId.INCOMPLETE_STRING))
            return true;
      }

      return false;
   }
}
