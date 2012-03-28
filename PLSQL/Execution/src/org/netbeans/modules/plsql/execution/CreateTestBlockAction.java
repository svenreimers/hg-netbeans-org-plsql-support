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
package org.netbeans.modules.plsql.execution;

import static org.netbeans.modules.plsql.lexer.PlsqlBlockType.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsql.utilities.PlsqlParserUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.CookieAction;
import org.openide.windows.WindowManager;

@ActionID(id = "org.netbeans.modules.plsql.execution.CreateTestBlockAction", category = "PLSQL")
@ActionRegistration(displayName = "#CTL_CreateTestBlockAction")
@ActionReferences(value = {
   @ActionReference(path = "Shortcuts", name = "AS-B"),
   @ActionReference(path = "Editors/text/x-plsql/Popup", position = 281)})
public final class CreateTestBlockAction extends CookieAction {

    private static final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
    private static final String TEST_BLOCK_NAME_PREFIX = "TestBlock: ";
    private PlsqlBlock selectedBlock = null;
    private String selectedName = "";
    private String parentName = "";
    private DataObject dataObject = null;
    private Document doc = null;
    private int position = -1;

    /**
     * Create a SQL execution window for the selected method
     * @param activatedNodes
     */
    @Override
    protected void performAction(Node[] activatedNodes) {
        if (selectedName.equals("")) {
            return;
        }

        String tempTemplate = "-- Enter values for your parameters. Use enter to move to the next parameter\n";

        Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
        if (project == null) {
            return;
        }
        DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
        if (project == null || dbConnectionProvider == null) {
            return;
        }
        DatabaseConnection databaseConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(false) : null;
        try {
            if (selectedBlock != null && selectedBlock.getType() == VIEW) {
                tempTemplate = tempTemplate + "SELECT ${*} FROM " + selectedName + ";\n${cursor}";
            } else if (selectedBlock != null && selectedBlock.getType() == CURSOR) {
                try {
                    tempTemplate = tempTemplate + doc.getText(selectedBlock.getStartOffset(), selectedBlock.getEndOffset() - selectedBlock.getStartOffset());
                } catch (BadLocationException ex) {
                    //Failed to extract statement from cursor. This shouldn't happen, but if it does - do nothing;
                    return;
                }
                tempTemplate = tempTemplate.substring(tempTemplate.indexOf("SELECT"));
                tempTemplate = tempTemplate.replaceAll("(\\w*_\\b)", "\\${$0}") + "\n${cursor";
            } else {
                try {
                    if (selectedBlock == null && !parentName.equals("")) {
                        //Find the selected block
                        DataObject data = PlsqlFileUtil.openExistingFile(doc, parentName, PACKAGE, project);
                        if (data == null) {
                            data = PlsqlFileUtil.openExistingFile(doc, parentName, PACKAGE, project);
                        }
                        if (data == null) {
                            data = PlsqlFileUtil.fetchAsTempFile(parentName, PACKAGE, databaseConnection, project, dataObject);
                        }

                        if (data != null) {
                            Document referredDoc = PlsqlFileUtil.getDocument(data);
                            selectMatchingBlock(data, referredDoc, position);
                        }

                        //selected block in package body   
                    } else if (selectedBlock != null && (selectedBlock.getType() == PROCEDURE_IMPL
                            || selectedBlock.getType() == FUNCTION_IMPL)) {
                        //Find the definition of the method, we need to do this because if this is a implementation method,
                        //we cannot call it as Package.Method
                        //check whether the def block is in the same file
                        PlsqlBlock temp = selectedBlock;
                        selectedBlock = null;
                        DataObject dataObj = null;
                        PlsqlBlock block = PlsqlParserUtil.findMatchingBlock(PlsqlParserUtil.getBlockHierarchy(dataObject), doc, doc, selectedName, parentName, temp.getStartOffset(), false, false, true);
                        if (block == null) {
                            Document specDoc = getSpecDocument(doc);
                            if (specDoc == null) {
                                dataObj = PlsqlFileUtil.fetchAsTempFile(temp.getParent().getName(), PACKAGE, databaseConnection, project, dataObject);
                            }

                            if (specDoc != null || dataObj != null) {
                                if (specDoc != null) {
                                    dataObj = FileExecutionUtil.getDataObject(specDoc);
                                } else {
                                    specDoc = PlsqlFileUtil.getDocument(dataObj);
                                }
                                selectMatchingBlock(dataObj, specDoc, temp.getStartOffset());
                            }
                        } else {
                            selectedBlock = block;
                        }

                        if (selectedBlock == null) {
                            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(), "Failed creating the test block; selected method might not be there in the specification", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }

                    if (selectedBlock.getParent() != null) {
                        selectedName = selectedBlock.getParent().getName() + "." + selectedBlock.getName();
                    }
                    tempTemplate = tempTemplate + createMethodTemplate(doc);
                } catch (NotConnectedToDbException e) {
                    Exceptions.printStackTrace(e);
                }
            }
        } finally {
            if (databaseConnection != null) {
                dbConnectionProvider.releaseDatabaseConnection(databaseConnection);
            }
        }

        //Replace aliases where possible
        tempTemplate = replaceAliases(tempTemplate, dataObject.getLookup().lookup(PlsqlBlockFactory.class), '&');

        SQLCommandWindow.createSQLCommandWindow(activatedNodes, tempTemplate, TEST_BLOCK_NAME_PREFIX + selectedName);
    }

    @Override
    protected int mode() {
        return CookieAction.MODE_EXACTLY_ONE;
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(CreateTestBlockAction.class, "CTL_CreateTestBlockAction");
    }

    @Override
    protected Class[] cookieClasses() {
        return new Class[]{DataObject.class, EditorCookie.class};
    }

    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }

    /**
     * Enable this action when right clicked on procedures or functions
     * @param arg0
     * @return
     */
    @Override
    protected boolean enable(Node[] activatedNodes) {
        if (!super.enable(activatedNodes)) {
            return false;
        }

        EditorCookie editorCookie = activatedNodes[0].getLookup().lookup(EditorCookie.class);
        if (editorCookie == null) {
            return false;
        }

        //Reset class variables again
        selectedBlock = null;
        selectedName = "";
        parentName = "";
        position = -1;

        int dotOffset = -1;
        int markOffset = -1;

        JEditorPane[] panes = editorCookie.getOpenedPanes();
        if ((panes != null) && (panes.length != 0)) {
            Caret caret = panes[0].getCaret();
            dotOffset = caret.getDot();
            markOffset = caret.getMark();
        }

        //If we are able to take the selected position get data object and get the block factory
        if (dotOffset != -1 && markOffset != -1) {
            dataObject = activatedNodes[0].getLookup().lookup(DataObject.class);
            doc = editorCookie.getDocument();
            if (dataObject != null && doc != null) {
                Project project = FileOwnerQuery.getOwner(dataObject.getPrimaryFile());
                DatabaseConnectionManager dbConnectionProvider = DatabaseConnectionManager.getInstance(project);
                if (project == null || dbConnectionProvider == null) {
                    return false;
                }

                position = dotOffset < markOffset ? dotOffset : markOffset;
                List<PlsqlBlock> blockHier = dataObject.getLookup().lookup(PlsqlBlockFactory.class).getBlockHierarchy();
                //If there is a selection check whether the selection is a method name
                if (validator.isValidTDB(dataObject) || validator.isValidPackageBody(dataObject)) {
                    boolean isSucess = checkForMethodCall(doc, dbConnectionProvider, position);
                    if (isSucess) {
                        return true;
                    }
                }

                //If there is no selection or selection is not a method call
                String methodName = "";
                methodName = getEnclosingMethodName(blockHier, dotOffset, methodName);
                if (!methodName.equals("")) {
                    selectedName = methodName;
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method that will create the template for the selected method
     * @param doc
     * @return
     */
    private String createMethodTemplate(Document doc) {
        String tempTemplate = "";
        TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        //If the current occurance is a variable declaration stop
        if (ts != null) {
            ts.move(selectedBlock.getStartOffset());
            ts.moveNext();
            Token<PlsqlTokenId> token = ts.token();
            boolean isParamStart = false;
            List<String> keys = new ArrayList<String>();
            List<String> values = new ArrayList<String>();
            String returnType = "";
            String name = "";
            String valueText = "";

            while (ts.moveNext()) {
                token = ts.token();
                PlsqlTokenId id = token.id();
                if (id == PlsqlTokenId.LPAREN) {
                    isParamStart = true;
                } else if (id == PlsqlTokenId.RPAREN) {
                    if (!name.equals("") && !valueText.equals("")) {
                        keys.add(name);
                        values.add(valueText);
                    }

                    if ((selectedBlock.getType() == PROCEDURE_DEF)
                            || (selectedBlock.getType() == PROCEDURE_IMPL)) {
                        break;
                    }
                } else if (id == PlsqlTokenId.KEYWORD && token.text().toString().equalsIgnoreCase("RETURN")) {
                    valueText = "";
                    isParamStart = true;
                } else if (isParamStart && id == PlsqlTokenId.IDENTIFIER && name.equals("")) {
                    name = token.text().toString();
                } else if (id == PlsqlTokenId.OPERATOR && token.text().toString().equals(",")) {
                    keys.add(name);
                    values.add(valueText);
                    name = "";
                    valueText = "";
                } else if ((id == PlsqlTokenId.OPERATOR && token.text().toString().equals(";"))
                        || (id == PlsqlTokenId.KEYWORD && token.text().toString().equalsIgnoreCase("IS"))) {
                    if (!valueText.equals("")) {
                        returnType = valueText.trim();
                    }
                    break;
                } else if (isParamStart) {
                    valueText = valueText + token.text().toString();
                }
            }

            //Now we have got the parameters and types
            tempTemplate = tempTemplate +  "DECLARE\n";
            for (int i = 0; i < keys.size(); i++) {
                boolean out = false;
                boolean in = false;
                boolean isDefault = false;
                String defaultVal = null;
                String key = keys.get(i);
                String value = values.get(i);
                String type = "";

                StringTokenizer tokenizer = new StringTokenizer(value, " \t\n");
                if (tokenizer.countTokens() == 1) {
                    type = value;
                }

                while (tokenizer.hasMoreTokens()) {
                    String temp = tokenizer.nextToken();
                    if (temp.equalsIgnoreCase("OUT")) {
                        out = true;
                    } else if (temp.equalsIgnoreCase("IN")) {
                        in = true;
                    } else if (temp.equalsIgnoreCase("DEFAULT")) {
                        isDefault = true;
                    } else if (isDefault) {
                        defaultVal = temp;
                        isDefault = false;
                    } else {
                        type = type + " " + temp;
                    }
                }

                //if type is VARCHAR2 specify length
                if (type.trim().equalsIgnoreCase("VARCHAR2")) {
                    type = "VARCHAR2(50)";
                }

                //Now we are ready to declare variables
                if (out && !in) {
                    tempTemplate = tempTemplate + " " + key.trim() + " " + type.trim() + ";\n";
                } else {
                    if (defaultVal != null) {
                        tempTemplate = tempTemplate + " " + key.trim() + " " + type.trim() + ":= ${" + key + " default=\"" + defaultVal + "\"};\n";
                    } else {
                        tempTemplate = tempTemplate + " " + key.trim() + " " + type.trim() + ":= ${" + key + " default=\"&" + key + "\"};\n";
                    }
                }
            }

            //If we have a complex type define the result
            if ((!returnType.equals("")) && (!returnType.equalsIgnoreCase("NUMBER"))
                    && (!returnType.equalsIgnoreCase("VARCHAR2")) && (!returnType.equalsIgnoreCase("DATE"))) {
                tempTemplate = tempTemplate + "result_ " + "${" + returnType + " default=\"" + returnType + "\"};\n";
            }

            //Complete the template now
            tempTemplate = tempTemplate + "BEGIN\n\t${cursor}";
            if (returnType.equals("")) {
                tempTemplate = tempTemplate + selectedName + "(" + fillVariables(keys) + ");\n";
            } else if (returnType.equalsIgnoreCase("NUMBER") || returnType.equalsIgnoreCase("VARCHAR2")
                    || returnType.equalsIgnoreCase("DATE")) {
                tempTemplate = tempTemplate + "Dbms_Output.Put_Line('Return value: ' || " + selectedName + "(" + fillVariables(keys) + "));\n";
            } else if (returnType.equalsIgnoreCase("BOOLEAN")) {
                tempTemplate = tempTemplate + "result_ := " + selectedName + "(" + fillVariables(keys) + ");\n";
                tempTemplate = tempTemplate + "-- Convert false/true/null to 0/1/null\n";
                tempTemplate = tempTemplate + "Dbms_Output.Put_Line('Return value: ' || sys.diutil.bool_to_int(result_));\n";
            } else {
                //we have a complex type with us
                tempTemplate = tempTemplate + "result_ := " + selectedName + "(" + fillVariables(keys) + ");\n";
                tempTemplate = tempTemplate + "Dbms_Output.Put_Line(${output default =\"'<<<Put your output of the return variable here>>>'\"});\n";
            }

            //print variables
            for (int i = 0; i < keys.size(); i++) {
                tempTemplate = tempTemplate + "Dbms_Output.Put_Line('" + keys.get(i) + "= ' || " + keys.get(i) + ");\n";
            }

            tempTemplate = tempTemplate + "END;\n/";
        }

        return tempTemplate;
    }

    /**
     * Method that will fill variables inside the method
     * @param keys
     * @return
     */
    private String fillVariables(List<String> keys) {
        String variables = "";
        for (int i = 0; i < keys.size(); i++) {
            variables = variables + keys.get(i);
            if (i != keys.size() - 1) {
                variables = variables + ",";
            }
        }
        return variables;
    }

    /**
     * Method that will get the method name that encloses the given offset
     * @param blockHier
     * @param offset
     * @return
     */
    private String getEnclosingMethodName(List blockHier, int offset, String methodName) {
        for (int i = 0; i < blockHier.size(); i++) {
            PlsqlBlock temp = (PlsqlBlock) blockHier.get(i);
            if ((offset <= temp.getEndOffset()) && (offset >= temp.getStartOffset())) {
                //Offset is within the block, if this is not a procedure look at the children and return
                if ((temp.getType() == PROCEDURE_DEF)
                        || (temp.getType() == FUNCTION_DEF)
                        || (temp.getType() == CURSOR)
                        || (temp.getType() == VIEW)) {
                    if (methodName.equals("")) {
                        methodName = temp.getName();
                    } else {
                        methodName = methodName + "." + temp.getName();
                    }

                    //Otherwise if the action is performed have to look for the block again
                    selectedBlock = temp;
                } else if ((temp.getType() == FUNCTION_IMPL)
                        || (temp.getType() == PROCEDURE_IMPL)) {
                    //check to see if there's an inner cursor that should be tested
                    if (temp.getChildCount() > 0) {
                        List children = temp.getChildBlocks();
                        for (int j = 0; j < children.size(); j++) {
                            PlsqlBlock child = (PlsqlBlock) children.get(j);
                            if (child.getType() == CURSOR && (offset <= child.getEndOffset()) && (offset >= child.getStartOffset())) {
                                methodName = "Cursor";
                                selectedBlock = child;
                                return methodName;
                            }
                        }
                    }

                    methodName = temp.getName();
                    //Otherwise if the action is performed have to look for the block again
                    selectedBlock = temp;
                } else if ((temp.getType() == PACKAGE_BODY)
                        || (temp.getType() == PACKAGE)) {
                    methodName = temp.getName();
                    String tempName = getEnclosingMethodName(temp.getChildBlocks(), offset, methodName);
                    if (!tempName.equals(methodName)) {
                        methodName = tempName;
                    } else {
                        methodName = "";
                    }
                }
                break;
            }
        }
        return methodName;
    }

    /**
     * Replace aliases in the given string
     * @param plsqlString
     * @param blockFac
     * @param define
     * @return
     */
    public String replaceAliases(String plsqlString, PlsqlBlockFactory blockFac, char define) {
        if (plsqlString.indexOf(define) < 0) {
            return plsqlString;
        }

        StringBuilder newString = new StringBuilder();
        for (int i = 0; i < plsqlString.length(); i++) {
            char c = plsqlString.charAt(i);
            if (c == define) {
                for (int j = i + 1; j < plsqlString.length(); j++) {
                    char nextChar = plsqlString.charAt(j);
                    if (Character.isJavaIdentifierPart(nextChar) && j == plsqlString.length() - 1) { //we have reached the end of the text
                        nextChar = '.'; //this will make sure that the correct sustitution is made below by emulating an additional character
                        j = j + 1;
                    }
                    if (!Character.isJavaIdentifierPart(nextChar)) { //potential end of substitutionvariable
                        if (j > i + 1) { //substituion variable found
                            String name = plsqlString.substring(i, j);
                            String value = blockFac.getDefine(name);
                            newString.append(value);
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

    /**
     * Check whether the selection is a method call
     * @param document
     * @param dbConnectionProvider
     * @param project
     * @param startOffset
     * @return
     */
    private boolean checkForMethodCall(Document document, DatabaseConnectionManager dbConnectionProvider, int startOffset) {
        DatabaseConnection databaseConnection = dbConnectionProvider != null ? dbConnectionProvider.getPooledDatabaseConnection(false) : null;
        try {
            DatabaseContentManager cache = databaseConnection != null ? DatabaseContentManager.getInstance(databaseConnection) : null;

            String selected = "";
            String parent = "";

            //Get the current token
            TokenHierarchy tokenHierarchy = TokenHierarchy.get(document);
            @SuppressWarnings("unchecked")
            TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

            if (ts != null) {
                //move offset
                ts.move(startOffset);
                if (ts.moveNext()) {
                    Token<PlsqlTokenId> token = ts.token();
                    PlsqlTokenId tokenID = token.id();

                    if (tokenID == PlsqlTokenId.IDENTIFIER) {
                        selected = token.text().toString();
                        if (ts.movePrevious()) {
                            token = ts.token();
                            if (token.id() == PlsqlTokenId.DOT && ts.movePrevious()) {
                                parent = ts.token().toString();
                            }
                        }
                    }
                }
            }

            if (!parent.equals("")) {
                //If parent is a package we can think that this is a method call
                if (cache != null && cache.isPackage(parent, databaseConnection)) {
                    selectedName = selected;
                    parentName = parent;
                    return true;
                }
            } else {
                PlsqlBlock packageBlock = getEnclosingPackageBody(dataObject.getLookup().lookup(PlsqlBlockFactory.class).getBlockHierarchy(), startOffset);
                if (packageBlock != null && isMethodName(packageBlock.getChildBlocks(), selected, startOffset)) {
                    return true;
                }
            }

            return false;
        } finally {
            dbConnectionProvider.releaseDatabaseConnection(databaseConnection);
        }
    }

    private Document getSpecDocument(Document doc) {
        DataObject dataObj = FileExecutionUtil.getDataObject(doc);
        if (dataObj != null) {
            DataObject siblingDataObject = validator.getSiblingExt(dataObject);
            if (siblingDataObject != null) {
                return PlsqlFileUtil.getDocument(siblingDataObject);
            }
        }
        return null;
    }

    private boolean isMethodName(List<PlsqlBlock> blockHier, String selected, int offset) {
        if (blockHier != null) {
            for (PlsqlBlock block : blockHier) {
                if (block.getType() == PACKAGE_BODY) {
                    if (isMethodName(block.getChildBlocks(), selected, offset)) {
                        return true;
                    }
                } else if ((block.getType() == FUNCTION_IMPL
                        || block.getType() == PROCEDURE_IMPL)
                        && block.getParent() != null
                        && block.getName().equalsIgnoreCase(selected) && block.getStartOffset() == offset) {
                    selectedBlock = block;
                    selectedName = block.getName();
                    return true;
                }
            }
        }

        return false;
    }

    private PlsqlBlock getEnclosingPackageBody(List<PlsqlBlock> blockHierarchy, int offset) {
        for (PlsqlBlock block : blockHierarchy) {
            if (block.getType() == PACKAGE_BODY
                    && block.getStartOffset() < offset
                    && block.getEndOffset() > offset) {
                return block;
            }
        }

        return null;
    }

    private void selectMatchingBlock(DataObject dataObj, Document specDoc, int offset) {
        List<PlsqlBlock> newBlockHier = PlsqlParserUtil.getBlockHierarchy(dataObj);
        PlsqlBlock block = PlsqlParserUtil.findMatchingBlock(newBlockHier, doc, specDoc, selectedName, parentName, offset, false, false, true);
        if (block != null && block.getParent() != null) {
            selectedBlock = block;
            doc = specDoc;
            dataObject = dataObj;
        }
    }
}
