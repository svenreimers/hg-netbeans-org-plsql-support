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
package org.netbeans.modules.plsql.hyperlink;

import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsql.utilities.LogInWarningDialog;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsql.hyperlink.util.PlsqlHyperlinkUtil;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.JEditorPane;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.lib.editor.hyperlink.spi.HyperlinkProvider;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

/**
 * Class that will go to the relevant variable, method declaration
 * when ctrl pressed and clicked on a variable
 * @author YaDhLK
 */
public class PlsqlHyperlinkProvider implements HyperlinkProvider {

   protected final PlsqlFileValidatorService globalFileValidator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
   private int startOffset;
   private int endOffset;
   private String tokenImage;
   private String parentImage;
   private Document lastDocument;

   /**
    * Contructor
    */
   public PlsqlHyperlinkProvider() {
      reset();
   }

   /**
    * Method that will reset internal variables
    */
   private void reset() {
      startOffset = -1;
      endOffset = -1;
      tokenImage = "";
      parentImage = null;
      lastDocument = null;
   }

   /**
    * This method will determine whether there should be a hyperlink
    * at the given offset within the given document
    * @param doc
    * @param offset
    * @return
    */
   @SuppressWarnings("empty-statement")
   @Override
   public boolean isHyperlinkPoint(final Document doc, final int offset) {
      reset();
      // We want to work only BaseDocument:
      if (!(doc instanceof BaseDocument)) {
         return false;
      }

      final BaseDocument bdoc = (BaseDocument) doc;
      final JTextComponent target = Utilities.getFocusedComponent();

      // We want to work only with the open editor and
      // the editor has to be the active component:
      if ((target == null) || (target.getDocument() != bdoc)) {
         return false;
      }

      lastDocument = bdoc;

      //Get the current token
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         //move offset
         ts.move(offset);
         if (ts.moveNext()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();

            if (tokenID == PlsqlTokenId.IDENTIFIER) {
               startOffset = token.offset(tokenHierarchy);
               endOffset = startOffset + token.length();
               tokenImage = token.text().toString();
               return true;
            } else if (tokenID == PlsqlTokenId.BLOCK_COMMENT
                    || tokenID == PlsqlTokenId.LINE_COMMENT
                    || tokenID == PlsqlTokenId.STRING_LITERAL) {
               PlsqlHyperlinkUtil.ImageInfo info = PlsqlHyperlinkUtil.getCurrentWordInfo(doc, offset);
               if (info == null) {
                  return false;
               } else {
                  startOffset = info.startOffset;
                  endOffset = info.endOffset;
                  tokenImage = info.image;
                  parentImage = info.parentImage;

                  return true;
               }
            }
         }
      }

      return false;
   }

   /**
    * Method that determines the length of the hyperlink
    * @param doc
    * @param offset
    * @return
    */
   @Override
   public int[] getHyperlinkSpan(Document doc, int offset) {
      // First check that we are working with BaseDocument:
      if (!(doc instanceof BaseDocument)) {
         return null;
      }

      BaseDocument bdoc = (BaseDocument) doc;
      JTextComponent target = Utilities.getFocusedComponent();

      // We want to work only with the open editor
      // and the editor has to be the active component and
      // the document has to be the same as was used in the isHyperlinkPoint method:
      if ((target == null) || (lastDocument != bdoc)) {
         return null;
      }

      // Return the position that we defined in the isHyperlinkPoint method:
      return new int[]{startOffset, endOffset};
   }

   /**
    * Method that determines what happens when the hyperlink is clicked
    * @param doc
    * @param offset
    */
   @Override
   public void performClickAction(final Document doc, final int offset) {
      //Can be a define
      if (!(doc instanceof BaseDocument)) {
         return;
      }

      BaseDocument bdoc = (BaseDocument) doc;
      JTextComponent target = org.netbeans.editor.Utilities.getFocusedComponent();
      Object object = doc.getProperty(Document.StreamDescriptionProperty);
      Project project = null;
      if (object instanceof DataObject) {
         FileObject fo = ((DataObject) object).getPrimaryFile();
         project = FileOwnerQuery.getOwner(fo);
      }
      // We want to work only with the open editor and
      // the editor has to be active component and
      // the document has to be the same as was used in the isHyperlinkPoint method:
      if ((target == null) || (lastDocument != bdoc)) {
         return;
      }

      DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(doc);
      DatabaseConnection databaseConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(false) : null;
      try {
         DatabaseContentManager cache = databaseConnection != null ? DatabaseContentManager.getInstance(databaseConnection) : null;
         PlsqlBlockFactory blockFac = PlsqlHyperlinkUtil.getBlockFactory(doc);
         //Set wait cursor
         target.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         boolean isBody = true;
         if (object instanceof DataObject) {
            isBody = !globalFileValidator.isValidPackageSpec((DataObject) object);
         }
         String child = tokenImage;
         //Can be a define
         if (tokenImage.indexOf('&') == 0) {
            //Go to the relevant DEFINE
            PlsqlHyperlinkUtil.goToDefine(tokenImage, doc, target);
            target.setCursor(null);
         } else {
            if (isBody) {
               TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
               @SuppressWarnings(value = "unchecked")
               TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
               String parent = null;
               if (ts != null) {
                  ts.move(startOffset);
                  ts.moveNext();
                  Token<PlsqlTokenId> token = ts.token();
                  PlsqlTokenId tokenID = token.id();
                  if (tokenID == PlsqlTokenId.BLOCK_COMMENT
                          || tokenID == PlsqlTokenId.LINE_COMMENT
                          || tokenID == PlsqlTokenId.STRING_LITERAL) {
                     parent = parentImage;
                  } else if (ts.movePrevious()) {
                     token = ts.token();
                     if (token.id() == PlsqlTokenId.DOT) {
                        if (ts.movePrevious()) {
                           token = ts.token();
                        }
                        if (token.id() == PlsqlTokenId.IDENTIFIER) {
                           parent = token.toString();
                           if (cache.isSchema(parent)) {
                              parent = null;
                           }
                        } else if (token.id() == PlsqlTokenId.DOT) {
                           if (ts.movePrevious()) {
                              token = ts.token();
                           }
                           if (token.id() == PlsqlTokenId.IDENTIFIER && token.toString().startsWith("&")) {
                              parent = blockFac.getDefine(token.toString());
                           }
                        }
                     }
                     parentImage = parent;
                  }
               }
               if (parent == null) {
                  //If clicked on the package name go to the spec/body file
                  if (goToSpecOrBody(doc, isBody, databaseConnection, project)) {
                     target.setCursor(null);
                     return;
                  }
                  if (goToCurrentObjects(doc, target)) {
                     target.setCursor(null);
                     return;
                  }
                  //If clicked at End of a FUNCTION/PROCEDURE implementation fo to the beginning
                  if (goToBeginning(doc, target)) {
                     target.setCursor(null);
                     return;
                  }
                  //Can be a function/procedure declaration in the same file
                  if (goToMethodSameFile(doc, target)) {
                     target.setCursor(null);
                     return;
                  }
                  //Go to method specification
                  if (goToMethod(doc, isBody, databaseConnection, project)) {
                     target.setCursor(null);
                     return;
                  }
                  //can be a variable declaration of the same file
                  if (goToVariableSameFile(doc, target)) {
                     target.setCursor(null);
                     return;
                  }
                  //can be a variable declaration on the spec file
                  DataObject dObject = getSpecDataObject(doc, databaseConnection, project);
                  if (dObject != null) {
                     if (setCaretOfVariable(dObject, tokenImage)) {
                        target.setCursor(null);
                        return;
                     }
                  }
                  parent = child;
                  child = null;
               }
               if (cache == null) {
                  throw new NotConnectedToDbException();
               }
               //identity if the parent is a package, view or table
               //...but first check if it exists in the cache (and if not update the cache)
               if (dbConnectionProvider.isOnline() && !cache.isPackage(parent, databaseConnection)
                       && !cache.isView(parent, databaseConnection)
                       && !cache.isTable(parent, databaseConnection)
                       && !cache.isFunction(parent)
                       && !cache.isProcedure(parent)) {
                  cache.refreshItem(parent, databaseConnection);
               }
               if (cache.isPackage(parent, databaseConnection)) {
                  String aliasOf = cache.getPackageForSynonym(parent);
                  if (aliasOf != null) {
                     parent = aliasOf;
                  }
                  DataObject obj = PlsqlFileUtil.openExistingFile(doc, parent, PACKAGE_BODY, project);
                  if (obj == null) //check to see if the user has a local version of this file...
                  {
                     obj = PlsqlFileUtil.openExistingFile(null, parent, PACKAGE_BODY, project);
                  }
                  if (obj == null) {
                     obj = PlsqlFileUtil.fetchAsTempFile(parent, PACKAGE_BODY, databaseConnection, project, (DataObject) object);
                  }
                  if (obj != null && child == null) {
                     OpenCookie openCookie = obj.getCookie(OpenCookie.class);
                     openCookie.open();
                  } else {
                     if (obj == null || !setCaretOfMethod(obj, doc, child)) {
                        //check if it's a method
                        if (obj == null || !setCaretOfVariable(obj, child)) {
                           //or a cursor/variable
                           //can't find child in body - try with spec instead
                           obj = PlsqlFileUtil.openExistingFile(doc, parent, PACKAGE, project);
                           if (obj == null) //check to see if the user has a local version of this file...
                           {
                              obj = PlsqlFileUtil.openExistingFile(null, parent, PACKAGE, project);
                           }
                           if (obj == null) {
                              obj = PlsqlFileUtil.fetchAsTempFile(parent, PACKAGE, databaseConnection, project, (DataObject) object);
                           }
                           if (obj != null && !setCaretOfVariable(obj, child)) {
                              //try to navigate to child. If not found just open file
                              OpenCookie openCookie = obj.getCookie(OpenCookie.class);
                              openCookie.open();
                           }
                        }
                     }
                  }
               } else if (cache.isView(parent, databaseConnection)) {
                  String aliasOf = cache.getViewForSynonym(parent);
                  if (aliasOf != null) {
                     parent = aliasOf;
                  }
                  DataObject obj = PlsqlFileUtil.openExistingFile(doc, parent, VIEW, project);
                  if (obj == null) {
                     PlsqlHyperlinkUtil.openAsTempFile(parent, VIEW, databaseConnection, project, doc);
                  } else {
                     PlsqlHyperlinkUtil.setCaretOfView(obj, parent);
                  }
               } else if (cache.isTable(parent, databaseConnection)) {
                  String aliasOf = cache.getTableForSynonym(parent);
                  if (aliasOf != null) {
                     parent = aliasOf;
                  }
                  PlsqlHyperlinkUtil.openAsTempFile(parent, TABLE, databaseConnection, project, doc);
               } else if (cache.isFunction(parent)) {
                  PlsqlHyperlinkUtil.openAsTempFile(parent, FUNCTION, databaseConnection, project, doc);
               } else if (cache.isProcedure(parent)) {
                  PlsqlHyperlinkUtil.openAsTempFile(parent, PROCEDURE, databaseConnection, project, doc);
               } else if (databaseConnection != null) {
                  if (cache.getAllPackages().isEmpty() && cache.getAllViews().isEmpty() && cache.getAllTables().isEmpty()) {
                     //try whether a view
                     DataObject obj = PlsqlFileUtil.openExistingFile(doc, parent, VIEW, project);
                     if (obj != null) {
                        if (PlsqlHyperlinkUtil.setCaretOfView(obj, parent)) {
                           target.setCursor(null);
                           return;
                        }
                     }
                     //check whether a package
                     obj = PlsqlFileUtil.openExistingFile(doc, parent, PACKAGE_BODY, project);
                     if (obj != null && child == null) {
                        //no child item - just open the file
                        OpenCookie openCookie = obj.getCookie(OpenCookie.class);
                        openCookie.open();
                        return;
                     } else {
                        if (!setCaretOfMethod(obj, doc, child)) {
                           //check if it's a method
                           if (!setCaretOfVariable(obj, child)) {
                              //or a cursor/variable
                              //can't find child in body - try with spec instead
                              obj = PlsqlFileUtil.openExistingFile(doc, parent, PACKAGE, project);
                              if (setCaretOfVariable(obj, child)) {
                                 return;
                              }
                           } else {
                              return;
                           }
                        } else {
                           return;
                        }
                     }
                     //Coming here means we might not been able to find anything useful on disk
                     throw new NotConnectedToDbException();
                  }
               }
            } else {
               //.spec file
               //If clicked on the package name go to the body file
               if (goToSpecOrBody(doc, isBody, databaseConnection, project)) {
                  target.setCursor(null);
                  return;
               }
               //Go to method implementation
               if (goToMethod(doc, isBody, databaseConnection, project)) {
                  target.setCursor(null);
                  return;
               }
               //can be a variable declaration
               if (goToVariableSameFile(doc, target)) {
                  target.setCursor(null);
                  return;
               }
            }
         }
      } catch (NotConnectedToDbException e) {
         new LogInWarningDialog(null, true).setVisible(true);
      } finally {
         target.setCursor(null);
         dbConnectionProvider.releaseDatabaseConnection(databaseConnection);
      }
   }

   /**
    * Method that will check whether the selected identifier is a variable
    * & move there
    * @param doc
    * @param target
    * @return
    */
   private boolean goToVariableSameFile(Document doc, JTextComponent target) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      //If the current occurance is a variable declaration stop
      if (ts != null) {
         ts.move(startOffset);
         ts.moveNext();
         Token<PlsqlTokenId> token = ts.token();
         boolean isMatchFound = false;
         //Check whether this can be a variable definition if so the
         //identifier must be followed by spaces
         boolean canBeVariableDef = false;
         Token<PlsqlTokenId> tmp = token;
         if (ts.moveNext()) {
            tmp = ts.token();
         }
         if ((tmp.id() == PlsqlTokenId.WHITESPACE)
                 || (tmp.id() == PlsqlTokenId.LINE_COMMENT)
                 || (tmp.id() == PlsqlTokenId.BLOCK_COMMENT)
                 || (tmp.id() == PlsqlTokenId.STRING_LITERAL)) {
            canBeVariableDef = true;
         }
         ts.move(startOffset);
         ts.moveNext();

         if (canBeVariableDef) {
            //Get previous Non whitespace token
            if (PlsqlParserUtil.getPreviousNonWhitespace(ts, true)) {
               token = ts.token();
            }

            if ((token.text().toString().equalsIgnoreCase("CURSOR"))
                    || (token.text().toString().equalsIgnoreCase("TYPE"))
                    || (token.text().toString().equalsIgnoreCase("SUBTYPE"))) {
               //Get next non whitespace token
               ts.move(startOffset);
               ts.moveNext();
               if (PlsqlHyperlinkUtil.getNextNonWhitespace(ts, true)) {
                  token = ts.token();
               }

               if (token.text().toString().equalsIgnoreCase("IS")) {
                  isMatchFound = true;
               }
            } else if (token.text().toString().equalsIgnoreCase(".")) {
               return false;
            }

            if (!isMatchFound) {
               ts.move(startOffset);
               ts.moveNext(); //skip current word
               //To forward to find ';', ',' or ')'

               HashSet<String> tokenSet = new HashSet<String>();
               while (ts.moveNext()) {
                  token = ts.token();
                  if ((token.text().toString().equals(";")) || (token.text().toString().equals(","))
                          || (token.text().toString().equals(")")) || (token.text().toString().equalsIgnoreCase("CREATE"))
                          || (token.text().toString().equalsIgnoreCase("PACKAGE")) || (token.text().toString().equalsIgnoreCase("PROCEDURE"))
                          || (token.text().toString().equalsIgnoreCase("FUNCTION")) || (token.text().toString().equalsIgnoreCase("IS"))) { //Needed to ignore defines

                     break;
                  }

                  if ((token.id() != PlsqlTokenId.WHITESPACE)
                          && (token.id() != PlsqlTokenId.LINE_COMMENT)
                          && (token.id() != PlsqlTokenId.BLOCK_COMMENT)
                          && (token.id() != PlsqlTokenId.STRING_LITERAL)) {
                     tokenSet.add(token.text().toString().toUpperCase(Locale.ENGLISH));
                  }
               }

               //Now we have got the tokens from the indentifier name to the
               //first ';' or ',' or ')' check whether there is a datatype here
               if (((tokenSet.contains("NUMBER")) || (tokenSet.contains("VARCHAR2"))
                       || (tokenSet.contains("BOOLEAN")) || (tokenSet.contains("DATE"))
                       || (tokenSet.contains("EXCEPTION")) || (tokenSet.contains("RECORD")))
                       || ((tokenSet.contains("TYPE")) && (tokenSet.contains("%")))
                       || ((tokenSet.contains("ROWTYPE")) && (tokenSet.contains("%")))
                       || ((tokenSet.contains(".")) && (!tokenSet.contains(":")) && (!tokenSet.contains("=")) && (tokenSet.size() == 3)) && //for custom variables
                       (!tokenSet.contains(tokenImage.toUpperCase(Locale.ENGLISH)))) {
                  isMatchFound = true;
               }
            }
         }

         //Since this is a variable declaration return
         if (isMatchFound) {
            return true;
         }
      }

      if (ts != null) {
         Token<PlsqlTokenId> tokenPre = null;
         ts.move(startOffset);
         int defOffset = -1;
         boolean isMatchFound = false;

         while (ts.movePrevious()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            String image = token.text().toString();

            if ((tokenID == PlsqlTokenId.IDENTIFIER) && (image.equalsIgnoreCase(tokenImage))) {
               defOffset = token.offset(tokenHierarchy);

               //We must have found this first occurance before the offset
               if (defOffset >= startOffset) {
                  return false;
               }

               //Check whether this can be a variable definition if so the
               //identifier must be surrounded by spaces
               boolean canBeVariableDef = false;
               Token<PlsqlTokenId> tmp = token;
               if (ts.moveNext()) {
                  tmp = ts.token();
               }
               if ((tmp.id() == PlsqlTokenId.WHITESPACE)
                       || (tmp.id() == PlsqlTokenId.LINE_COMMENT)
                       || (tmp.id() == PlsqlTokenId.BLOCK_COMMENT)
                       || (tmp.id() == PlsqlTokenId.STRING_LITERAL)) {
                  canBeVariableDef = true;
               }
               ts.move(defOffset);
               ts.moveNext();

               if (canBeVariableDef) {
                  //Chech whether this is a variable declaration based on the context
                  if ((tokenPre != null) && (tokenPre.text().toString().equalsIgnoreCase("IS"))) {
                     //check whether next is IS
                     if (PlsqlParserUtil.getPreviousNonWhitespace(ts, true)) {
                        token = ts.token();
                     }

                     if ((token.text().toString().equalsIgnoreCase("CURSOR"))
                             || (token.text().toString().equalsIgnoreCase("TYPE"))
                             || (token.text().toString().equalsIgnoreCase("SUBTYPE"))) {
                        if (checkScopeofMatch(doc, defOffset)) {
                           isMatchFound = true;
                           break;
                        }
                     }
                  }

                  ts.move(defOffset);
                  ts.moveNext();

                  //To forward to find ';', ',' or ')'
                  Set<String> tokenSet = new HashSet<String>();
                  while (ts.moveNext()) {
                     token = ts.token();
                     if ((token.text().toString().equals(";")) || (token.text().toString().equals(","))
                             || (token.text().toString().equals(")")) || (token.text().toString().equalsIgnoreCase("CREATE"))
                             || (token.text().toString().equalsIgnoreCase("PACKAGE")) || (token.text().toString().equalsIgnoreCase("PROCEDURE"))
                             || (token.text().toString().equalsIgnoreCase("FUNCTION")) || (token.text().toString().equalsIgnoreCase("IS"))) { //Needed to ignore defines

                        break;
                     }

                     if ((token.id() != PlsqlTokenId.WHITESPACE)
                             && (token.id() != PlsqlTokenId.LINE_COMMENT)
                             && (token.id() != PlsqlTokenId.BLOCK_COMMENT)
                             && (token.id() != PlsqlTokenId.STRING_LITERAL)) {
                        tokenSet.add(token.text().toString().toUpperCase(Locale.ENGLISH));
                     }
                  }

                  //Now we have got the tokens from the indentifier name to the
                  //first ';' or ',' or ')' check whether there is a datatype here
                  if (((tokenSet.contains("NUMBER")) || (tokenSet.contains("VARCHAR2"))
                          || (tokenSet.contains("BOOLEAN")) || (tokenSet.contains("DATE"))
                          || (tokenSet.contains("EXCEPTION")) || (tokenSet.contains("RECORD")))
                          || ((tokenSet.contains("TYPE")) && (tokenSet.contains("%")))
                          || ((tokenSet.contains("ROWTYPE")) && (tokenSet.contains("%")))
                          || ((tokenSet.contains(".")) && (!tokenSet.contains(":")) && (!tokenSet.contains("=")) && (tokenSet.size() == 3)) && //for custom variables
                          (!tokenSet.contains(tokenImage.toUpperCase(Locale.ENGLISH)))) {
                     if (checkScopeofMatch(doc, defOffset)) {
                        isMatchFound = true;
                        break;
                     }
                  }

                  ts.move(defOffset);
                  ts.moveNext();
               }
            }

            if ((tokenID != PlsqlTokenId.WHITESPACE)
                    && (tokenID != PlsqlTokenId.LINE_COMMENT)
                    && (tokenID != PlsqlTokenId.BLOCK_COMMENT)
                    && (tokenID != PlsqlTokenId.STRING_LITERAL)) {
               tokenPre = token;
            }
         }

         if (isMatchFound) {
            target.setCaretPosition(defOffset);
            return true;
         }
      }
      return false;
   }

   /**
    * Method that will return the dataobject of the spec file (of this file)
    * @param doc
    * @param conn
    * @param project
    * @return
    */
   private DataObject getSpecDataObject(Document doc, DatabaseConnection conn, Project project) throws NotConnectedToDbException {
      DataObject sibling = null;
      //Get the spec of this file
      Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof DataObject) {
         DataObject dataObj = (DataObject) obj;
         sibling = globalFileValidator.getSiblingExt(dataObj);
         if (sibling == null) {
            //If file is not found try fetching the Package Body from database
            String packageName = PlsqlHyperlinkUtil.getPackageName(doc);
            if (!packageName.isEmpty()) {
               DataObject dataObject = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE, project);
               if (dataObject == null) {
                  dataObject = PlsqlFileUtil.fetchAsTempFile(packageName, PACKAGE, conn, project, (DataObject) obj);
               }
               if (dataObject != null) {
                  return dataObject;
               }
            }
         }
      }
      return sibling;
   }

   /**
    * Method that will return the DataObject of the body file (of this file)
    * @param doc
    * @param conn
    * @return
    */
   private DataObject getBodyDataObject(Document doc, DatabaseConnection conn, Project project) throws NotConnectedToDbException {
      DataObject sibling = null;

      //Get the body of this file
      Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof DataObject) {
         DataObject dataObj = (DataObject) obj;
          sibling = globalFileValidator.getSiblingExt(dataObj);
         if (sibling == null) {
            //If file is not found try fetching the Package Body from database
            String packageName = PlsqlHyperlinkUtil.getPackageName(doc);
            if (!packageName.equals("")) {
               DataObject dataObject = PlsqlFileUtil.openExistingFile(null, packageName, PACKAGE_BODY, project);
               if (dataObject == null) {
                  dataObject = PlsqlFileUtil.fetchAsTempFile(packageName, PACKAGE_BODY, conn, project, (DataObject) obj);
               }
               if (dataObject != null) {
                  return dataObject;
               }
            }
         }
      }

      return sibling;
   }

   /**
    * Go to FUNCTION/PROCEDURE definition in same file
    * @param doc
    * @param target
    * @return
    */
   private boolean goToMethodSameFile(Document doc, JTextComponent target) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
      boolean isUsage = true;
      //If the current occurance is a variable declaration stop
      if (ts != null) {
         ts.move(startOffset);
         ts.moveNext();
         Token<PlsqlTokenId> token = ts.token();

         if (PlsqlParserUtil.getPreviousNonWhitespace(ts, true)) {
            token = ts.token();
         }

         if ((token.id() == PlsqlTokenId.KEYWORD)
                 && ((token.text().toString().equalsIgnoreCase("PROCEDURE"))
                 || (token.text().toString().equalsIgnoreCase("FUNCTION")))) {
            isUsage = false;
         }
      }

      int defOffset = -1;
      boolean isMatchFound = false;
      PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(doc);
      if (blockFactory != null) {
         PlsqlBlock block = PlsqlParserUtil.findMatchingBlock(blockFactory.getBlockHierarchy(), doc,
                 doc, tokenImage, parentImage, startOffset, isUsage, true, true);
         if (block != null) {
            if (!isUsage && block.getStartOffset() < startOffset && block.getEndOffset() > startOffset) {
               //We have clicked on the method implementation, so try to find the definition in the same file
               block = PlsqlParserUtil.findMatchingBlock(blockFactory.getBlockHierarchy(), doc,
                       doc, tokenImage, parentImage, startOffset, isUsage, false, true);
               if (block != null) {
                  isMatchFound = true;
                  defOffset = block.getStartOffset();
               }
            } else {
               isMatchFound = true;
               defOffset = block.getStartOffset();
            }
         }
      }

      if (isMatchFound) {
         target.setCaretPosition(defOffset);
         return true;
      }

      return false;
   }

   /**
    * Method that will go to the beginning of a FUNCTION/PROCEDURE implementation
    * @param doc
    * @param target
    */
   private boolean goToBeginning(Document doc, JTextComponent target) {
      TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
      @SuppressWarnings("unchecked")
      TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

      if (ts != null) {
         ts.move(startOffset);
         int defOffset = -1;
         boolean isMatchFound = false;
         Token<PlsqlTokenId> token = ts.token();

         if (PlsqlParserUtil.getPreviousNonWhitespace(ts, true)) {
            token = ts.token();
         }

         if ((token == null) || (!token.text().toString().equalsIgnoreCase("END"))) {
            return false;
         }

         while (ts.movePrevious()) {
            token = ts.token();
            PlsqlTokenId tokenID = token.id();
            String image = token.text().toString();

            if ((tokenID == PlsqlTokenId.IDENTIFIER) && (image.equalsIgnoreCase(tokenImage))) {
               defOffset = token.offset(tokenHierarchy);

               //We must have found this first occurance before the offset
               if (defOffset >= startOffset) {
                  return false;
               }

               //check whether previous is FUNCTION/PROCEDURE/PACKAGE/BODY
               if (PlsqlParserUtil.getPreviousNonWhitespace(ts, true)) {
                  token = ts.token();
               }

               if ((token.text().toString().equalsIgnoreCase("FUNCTION"))
                       || (token.text().toString().equalsIgnoreCase("PROCEDURE"))
                       || (token.text().toString().equalsIgnoreCase("PACKAGE"))
                       || (token.text().toString().equalsIgnoreCase("BODY"))) {
                  isMatchFound = true;
                  break;
               }

               ts.move(defOffset);
               ts.moveNext();
            }
         }

         if (isMatchFound) {
            target.setCaretPosition(defOffset);
            return true;
         }
      }
      return false;
   }

   /**
    * Method that will go to the Package/ view begin line
    * if current package/view name is selected
    * @param doc
    * @param target
    */
   private boolean goToCurrentObjects(Document doc, JTextComponent target) {
      int offset = -1;
      PlsqlBlock viewBlock = getBlock(doc, PlsqlBlockType.VIEW);
      if (viewBlock != null) {
         if (tokenImage.equals(viewBlock.getName())) {
            offset = viewBlock.getStartOffset();
         }
      }

      if (offset == -1) {
         PlsqlBlock packageBlock = getBlock(doc, PlsqlBlockType.PACKAGE_BODY);
         if (packageBlock != null) {
            if (tokenImage.equals(packageBlock.getName())) {
               offset = packageBlock.getStartOffset();
            }
         }
      }

      //selected on the package/ view name
      if (offset != -1) {
         target.setCaretPosition(offset);
         return true;
      }

      return false;
   }

   /**
    * Method that will set caret position of the given dataobject
    * to the procedure/function identified by the tokenImage
    * @param doc
    * @param dataObject
    * @param methodName
    * @return
    */
   private boolean setCaretOfMethod(DataObject dObject, Document doc, String methodName) {
      if (dObject == null || methodName == null) {
         return false;
      }

      EditorCookie ec = dObject.getCookie(EditorCookie.class);
      if (!PlsqlFileUtil.prepareDocument(ec)) {
         return false;
      }
      OpenCookie openCookie = dObject.getCookie(OpenCookie.class);
      if (ec != null) {
         int defOffset = 0;
         boolean isMatchFound = false;

         PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(dObject);
         blockFactory.initHierarchy(ec.getDocument());
         if (blockFactory != null) {
            PlsqlBlock block = PlsqlParserUtil.findMatchingBlock(blockFactory.getBlockHierarchy(), doc,
                    ec.getDocument(), methodName, parentImage, startOffset, true, true, true); //We look for implementations here

            if (block != null) {
               isMatchFound = true;
               defOffset = block.getStartOffset();
            }
         }

         if (isMatchFound) {
            openCookie.open();
            JEditorPane[] panes = ec.getOpenedPanes();
            if (panes.length > 0) {
               JEditorPane pane = panes[0];
               pane.setCaretPosition(defOffset);
               return true;
            }
         }
      }

      return false;
   }

   /**
    * Method that will set caret position of the given dataobject
    * to the variable identified by the tokenImage (.body file)
    * @param dataObject
    * @param doc
    * @param name
    * @return
    */
   private boolean setCaretOfVariable(DataObject dObject, String name) {
      if (dObject == null || name == null) {
         return false;
      }

      EditorCookie ec = dObject.getCookie(EditorCookie.class);
      if (!PlsqlFileUtil.prepareDocument(ec)) {
         return false;
      }
      OpenCookie openCookie = dObject.getCookie(OpenCookie.class);
      if (ec != null) {
         int defOffset = 0;
         boolean isMatchFound = false;

         //Search for the variable
         TokenHierarchy tokenHierarchy = TokenHierarchy.get(ec.getDocument());
         @SuppressWarnings("unchecked")
         TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
         if (ts != null) {
            Token<PlsqlTokenId> tokenPre = null;
            ts.moveStart();

            while (ts.moveNext()) {
               Token<PlsqlTokenId> token = ts.token();
               PlsqlTokenId tokenID = token.id();
               String image = token.text().toString();

               if ((tokenID == PlsqlTokenId.IDENTIFIER) && (image.equalsIgnoreCase(name))) {
                  defOffset = token.offset(tokenHierarchy);

                  //Check whether this can be a variable definition if so the
                  //identifier must be surrounded by spaces
                  boolean canBeVariableDef = false;
                  Token<PlsqlTokenId> tmp = token;
                  if (ts.moveNext()) {
                     tmp = ts.token();
                  }
                  if ((tmp.id() == PlsqlTokenId.WHITESPACE)
                          || (tmp.id() == PlsqlTokenId.LINE_COMMENT)
                          || (tmp.id() == PlsqlTokenId.BLOCK_COMMENT)
                          || (tmp.id() == PlsqlTokenId.STRING_LITERAL)) {
                     canBeVariableDef = true;
                  }

                  ts.move(defOffset);
                  ts.moveNext();

                  if (canBeVariableDef) {
                     //Chech whether this is a variable declaration based on the context
                     if ((tokenPre != null) && ((tokenPre.text().toString().equalsIgnoreCase("CURSOR"))
                             || (tokenPre.text().toString().equalsIgnoreCase("TYPE"))
                             || (tokenPre.text().toString().equalsIgnoreCase("SUBTYPE")))) {
                        //check whether next is IS
                        if (PlsqlHyperlinkUtil.getNextNonWhitespace(ts, true)) {
                           token = ts.token();
                        }

                        if (token.text().toString().equalsIgnoreCase("IS")) {
                           isMatchFound = true;
                           break;
                        }
                     } else {
                        //If next token is %ignore e.x. tokenImage%TYPE;
                        if (ts.moveNext()) {
                           token = ts.token();
                        }

                        if (token.text().toString().equals("%")) {
                           break;
                        }
                     }

                     ts.move(defOffset);
                     ts.moveNext();

                     //To forward to find ';', ',' or ')'
                     HashSet<String> tokenSet = new HashSet<String>();
                     while (ts.moveNext()) {
                        token = ts.token();
                        if ((token.text().toString().equals(";")) || (token.text().toString().equals(","))
                                || (token.text().toString().equals(")")) || (token.text().toString().equalsIgnoreCase("CREATE"))
                                || (token.text().toString().equalsIgnoreCase("PACKAGE")) || (token.text().toString().equalsIgnoreCase("PROCEDURE"))
                                || (token.text().toString().equalsIgnoreCase("FUNCTION")) || (token.text().toString().equalsIgnoreCase("IS"))) { //Needed to ignore defines

                           break;
                        }

                        if ((token.id() != PlsqlTokenId.WHITESPACE)
                                && (token.id() != PlsqlTokenId.LINE_COMMENT)
                                && (token.id() != PlsqlTokenId.BLOCK_COMMENT)
                                && (token.id() != PlsqlTokenId.STRING_LITERAL)) {
                           tokenSet.add(token.text().toString().toUpperCase(Locale.ENGLISH));
                        }
                     }

                     //Now we have got the tokens from the indentifier name to the
                     //first ';' or ',' or ')' check whether there is a datatype here
                     if (((tokenSet.contains("NUMBER")) || (tokenSet.contains("VARCHAR2"))
                             || (tokenSet.contains("BOOLEAN")) || (tokenSet.contains("DATE"))
                             || (tokenSet.contains("EXCEPTION")) || (tokenSet.contains("RECORD")))
                             || ((tokenSet.contains("TYPE")) && (tokenSet.contains("%")))
                             || ((tokenSet.contains("ROWTYPE")) && (tokenSet.contains("%")))
                             || ((tokenSet.contains(".")) && (!tokenSet.contains(":")) && (!tokenSet.contains("=")) && (tokenSet.size() == 3)) && //for custom variables
                             (!tokenSet.contains(name.toUpperCase(Locale.ENGLISH)))) {
                        isMatchFound = true;
                        break;
                     }

                     ts.move(defOffset);
                     ts.moveNext();
                  }
               }

               if ((tokenID != PlsqlTokenId.WHITESPACE)
                       && (tokenID != PlsqlTokenId.LINE_COMMENT)
                       && (tokenID != PlsqlTokenId.BLOCK_COMMENT)
                       && (tokenID != PlsqlTokenId.STRING_LITERAL)) {
                  tokenPre = token;
               }
            }
         }

         if (isMatchFound) {
            openCookie.open();
            JEditorPane[] panes = ec.getOpenedPanes();
            if (panes.length > 0) {
               JEditorPane pane = panes[0];
               pane.setCaretPosition(defOffset);
               return true;
            }
         }
      }

      return false;
   }

   /**
    * If checked on the package name go to the spec/body file
    * @param doc
    * @param isBody
    * @param conn
    * @param project
    * @return
    */
   private boolean goToSpecOrBody(Document doc, boolean isBody, DatabaseConnection conn, Project project) throws NotConnectedToDbException {
      boolean isPackageName = false;

      //check whether this is preceeded by PACKAGE BODY
      String packageName = PlsqlHyperlinkUtil.getPackageName(doc);
      if (!packageName.equals("")) {
         if (packageName.equals(tokenImage)) {
            isPackageName = true;
         }
      }

      if (isPackageName) {
         DataObject dataObject = null;
         if (isBody) {
            dataObject = getSpecDataObject(doc, conn, project);
         } else {
            dataObject = getBodyDataObject(doc, conn, project);
         }

         if (dataObject != null) {
            OpenCookie openCookie = dataObject.getCookie(OpenCookie.class);
            openCookie.open();
            return true;
         }
      }

      return false;
   }

   /**
    * Method that will return the block with the given type
    * @param doc
    * @param type //only block types that are parent blocks will be returned from this
    * @return
    */
   private PlsqlBlock getBlock(Document doc, PlsqlBlockType type) {
      PlsqlBlock block = null;
      PlsqlBlockFactory blockFac = PlsqlHyperlinkUtil.getBlockFactory(doc);
      if (blockFac != null) {
         List<PlsqlBlock> blockHier = blockFac.getBlockHierarchy();
         for (int i = 0; i < blockHier.size(); i++) {
            PlsqlBlock temp = blockHier.get(i);
            if (temp.getType() == type) {
               block = temp;
               break;
            }
         }
      }

      return block;
   }

   /**
    * Method that will go to the method implementation
    * on the body file if clicked on the method name of the spec and vice versa
    * @param doc
    * @param isBody
    * @param conn
    * @param project
    * @return
    */
   private boolean goToMethod(Document doc, boolean isBody, DatabaseConnection conn, Project project) throws NotConnectedToDbException {
      boolean isFound = false;
      PlsqlBlockFactory blockFactory = PlsqlHyperlinkUtil.getBlockFactory(doc);
      if (blockFactory != null) {
         List<PlsqlBlock> matchList = new ArrayList<PlsqlBlock>();
         if (isBody) {
            PlsqlParserUtil.findMatchingImplBlocks(blockFactory.getBlockHierarchy(), tokenImage, matchList);
         } else {
            PlsqlParserUtil.findMatchingDefBlocks(blockFactory.getBlockHierarchy(), tokenImage, matchList);
         }
         if (matchList.size() >= 1) {
            isFound = true;
         }
      }

      if (isFound) {
         //Ok we have clicked on a method name, now we have to fetch
         DataObject dataObj = null;
         if (isBody) {
            dataObj = getSpecDataObject(doc, conn, project);
         } else {
            dataObj = getBodyDataObject(doc, conn, project);
         }
         if (dataObj != null) {
            EditorCookie ec = dataObj.getCookie(EditorCookie.class);
            if (!PlsqlFileUtil.prepareDocument(ec)) {
               return false;
            }
            OpenCookie openCookie = dataObj.getCookie(OpenCookie.class);
            if (ec != null) {
               PlsqlBlockFactory bodyBlockFac = PlsqlHyperlinkUtil.getBlockFactory(dataObj);
               if (bodyBlockFac != null) {
                  bodyBlockFac.initHierarchy(ec.getDocument());
                  PlsqlBlock block = PlsqlParserUtil.findMatchingBlock(bodyBlockFac.getBlockHierarchy(), doc, ec.getDocument(), tokenImage, parentImage, startOffset, false, !isBody, true);
                  if (block != null) {
                     openCookie.open();
                     JEditorPane[] panes = ec.getOpenedPanes();
                     if (panes.length > 0) {
                        JEditorPane pane = panes[0];
                        pane.setCaretPosition(block.getStartOffset());
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   /**
    * This method will check the scope of the found variable
    * @param doc
    * @param offset
    * @return
    */
   private boolean checkScopeofMatch(Document doc, int offset) {
      boolean isOffsetOk = false;
      PlsqlBlockFactory blockFac = PlsqlHyperlinkUtil.getBlockFactory(doc);

      //Get blocks of the given offsets (leaf nodes in the block hierarchy)
      PlsqlBlock usageBlock = getBlockForOffset(blockFac.getBlockHierarchy(), startOffset);
      PlsqlBlock defBlock = getBlockForOffset(blockFac.getBlockHierarchy(), offset);

      if (usageBlock != null && defBlock != null) {
         //def has to be a parent of usage
         if ((defBlock.getStartOffset() <= usageBlock.getStartOffset())
                 && (defBlock.getEndOffset() >= usageBlock.getEndOffset())) {
            isOffsetOk = true;
         } else if (defBlock.getType().equals(PlsqlBlockType.CURSOR) && defBlock.getStartOffset() <= offset) {
            isOffsetOk = true;
         }
      } else if (defBlock == null) {
         isOffsetOk = true;
      }
      return isOffsetOk;
   }

   /**
    * Method that will get the block corresponding to the given offset
    * @param lstBlock
    * @param offset
    * @return
    */
   private PlsqlBlock getBlockForOffset(List<PlsqlBlock> lstBlock, int offset) {
      PlsqlBlock block = null;
      int count = lstBlock.size();
      for (int i = 0; i < count; i++) {
         PlsqlBlock tmp = lstBlock.get(i);
         if ((tmp.getStartOffset() <= offset) && (tmp.getEndOffset() >= offset)) {
            PlsqlBlock child = getBlockForOffset(tmp.getChildBlocks(), offset);
            if (child != null) {
               block = child;
            } else {
               block = tmp;
            }

            break;
         }
      }

      return block;
   }
}
