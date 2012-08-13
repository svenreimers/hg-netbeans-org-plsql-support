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
package org.netbeans.modules.plsql.utilities;

import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.swing.text.Document;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

public class PlsqlParserUtil {

    /**
     * Method that will find & return the matching FUNCTION/ PROCEDURE of the body file
     * @param blockHier
     * @param source
     * @param dest
     * @param name
     * @param startOffset
     * @param isUsage       whether this is method usage
     * @param isImpl
     * @param findBestMatch   whether best match is OK
     * @return
     */
    public static PlsqlBlock findMatchingBlock(final List<PlsqlBlock> blockHier, final Document source,
            final Document dest, final String name, String packageName, final int startOffset, final boolean isUsage, final boolean isImpl, final boolean findBestMatch) {
        PlsqlBlock match = null;
        final List<PlsqlBlock> matchList = new ArrayList<PlsqlBlock>();
        if (isImpl) {
            findMatchingImplBlocks(blockHier, name, matchList);
        } else {
            findMatchingDefBlocks(blockHier, name, matchList);
        }

        //There are several methods with the same name. Have to check the signature
        List<String> usageParams;
        if (isUsage) {
            usageParams = fetchMethodParamTypes(source, startOffset);
        } else {
            usageParams = fetchMethodDefParamTypes(source, startOffset);
        }
  
        if (packageName == null || packageName.equals("")) {
            packageName = getPackageName(getBlockFactory(source), startOffset);
        }

        //Take PlsqlBlock one by one and compare the parameters
        for (int x = 0; x < matchList.size(); x++) {
            PlsqlBlock block = matchList.get(x);
            if (!(block.getParent() != null ? block.getParent().getName().equalsIgnoreCase(packageName) : true)) {
                continue; //If the parent block name is not the same this is not a match
            }

            List<String> params = fetchMethodDefParamTypes(dest, block.getStartOffset());
            int defaultNo = fetchDefaultParams(dest, block.getStartOffset());
            if (compareParameters(usageParams, params, defaultNo)) {
                match = block;
                break;
            }
        }
        //get method from user
        if(match == null && findBestMatch && matchList.size() > 0){
            if(matchList.size()==1){
                match = matchList.get(0);
            }
            else{
                match = getMethodFromUser(dest,matchList);
            }
        }

        return (match == null && findBestMatch && matchList.size() > 0) ? null : match;
    }
    
    private static PlsqlBlock getMethodFromUser(Document doc, List<PlsqlBlock> blocks) {
        SelectMethodDialog dialog = new SelectMethodDialog(null, true, doc, blocks);
        dialog.setVisible(true);
        return dialog.getSelectedPlsqlBlock();
    }
    
    /**
     * Method that will find & return list of matching FUNCTION/ PROCEDURE of the body file
     * @param blockHier
     * @param source
     * @param dest
     * @param name
     * @param startOffset
     * @param isUsage       whether this is method usage
     * @param isImpl
     * @param findBestMatch   whether best match is OK
     * @return
     */    
    public static List<PlsqlBlock> findMatchingBlocks(final List<PlsqlBlock> blockHier, final Document source,
            final Document dest, final String name, String packageName, final int startOffset, final boolean isUsage, final boolean isImpl, final boolean findBestMatch) {
        PlsqlBlock match = null;
        final List<PlsqlBlock> matchList = new ArrayList<PlsqlBlock>();
        if (isImpl) {
            findMatchingImplBlocks(blockHier, name, matchList);
        } else {
            findMatchingDefBlocks(blockHier, name, matchList);
        }

        //There are several methods with the same name. Have to check the signature
        List<String> usageParams;
        if (isUsage) {
            usageParams = fetchMethodParamTypes(source, startOffset);
        } else {
            usageParams = fetchMethodDefParamTypes(source, startOffset);
        }

        if (packageName == null || packageName.equals("")) {
            packageName = getPackageName(getBlockFactory(source), startOffset);
        }

        //Take PlsqlBlock one by one and compare the parameters
        for (int x = 0; x < matchList.size(); x++) {
            PlsqlBlock block = matchList.get(x);
            if (!(block.getParent() != null ? block.getParent().getName().equalsIgnoreCase(packageName) : true)) {
                continue; //If the parent block name is not the same this is not a match
            }

            List<String> params = fetchMethodDefParamTypes(dest, block.getStartOffset());
            int defaultNo = fetchDefaultParams(dest, block.getStartOffset());
            if (compareParameters(usageParams, params, defaultNo)) {
                match = block;
                break;
            }
        }
        if (match == null && findBestMatch) {
           List<PlsqlBlock> similarParaCountList = new ArrayList<PlsqlBlock>();
           int usageCount = fetchMethodParamsCount(source, startOffset);
           for (int x = 0; x < matchList.size(); x++) {
               PlsqlBlock block = matchList.get(x);
               if (!(block.getParent() != null ? block.getParent().getName().equalsIgnoreCase(packageName) : true)) {
                   continue; //If the parent block name is not the same this is not a match
               }

               int count = fetchMethodParamsCount(dest, block.getStartOffset());
               
               if (usageCount == count) {
                   similarParaCountList.add(block);
               }
            }
            if (similarParaCountList.size() > 0) {
                if (similarParaCountList.size() == 1) {
                    match = similarParaCountList.get(0);
                } else {
                    return similarParaCountList;
                }
            }
            if (match == null && matchList.size() > 0) {
                return matchList;
            }
            }
        List<PlsqlBlock> returnList = new ArrayList<PlsqlBlock>();
        if (match != null) {
            returnList.add(match);
        }
        return (match == null && findBestMatch && matchList.size() > 0) ? null : returnList;
    }        

    /**
     * Compare given parameter lists
     * @param usageParams
     * @param params
     * @param defaultNo
     * @return
     */
    public static boolean compareParameters(final List<String> usageParams, final List<String> params, final int defaultNo) {
        if (usageParams.size() > params.size()) {
            return false;
        }
        if ((usageParams.isEmpty()) && (params.isEmpty())) {
            return true;
        }

        int matchCount = 0;
        for (int i = 0; i < usageParams.size(); i++) {
            String usageParam = usageParams.get(i);
            String param = params.get(i);

            if (usageParam.equalsIgnoreCase(param)) {
                matchCount++;
            } else if (usageParam.equals("UNKNOWN")) //can happen when the usage param type cannot be found
            {
                matchCount++;
            }                //e.g table row is passed
        }

        if (matchCount == params.size() || (matchCount + defaultNo) == params.size()) {
            return true;
        }

        return false;
    }

    /**
     * Method that will access the parameter types of the function/procedure
     * @param doc
     * @param startOffset
     * @return
     */
    private static List<String> fetchMethodParamTypes(final Document doc, final int startOffset) {
        final List<String> params = new ArrayList<String>();
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(startOffset);
            ts.moveNext();
            Token<PlsqlTokenId> token = ts.token();
            Token<PlsqlTokenId> tokenPre = token;
            int paranCount = 0;
            String text = "";
            while (ts.moveNext()) {
                token = ts.token();
                if (token.text().toString().equals("(")) {
                    paranCount++;
                } else if (token.text().toString().equals(")")) {
                    paranCount--;
                }

                if ((token.text().toString().equals(",") && paranCount == 1)
                        || (token.text().toString().equals(")") && paranCount == 0)) {
                    if ((tokenPre.id() == PlsqlTokenId.DOUBLE_LITERAL) || (tokenPre.id() == PlsqlTokenId.INT_LITERAL)) {
                        params.add("NUMBER");
                    } else if (tokenPre.id() == PlsqlTokenId.STRING_LITERAL) {
                        params.add("VARCHAR2");
                    } else {
                        if (text.trim().toLowerCase(Locale.ENGLISH).startsWith("to_char")) {
                            params.add("VARCHAR2");
                        } else {
                            params.add(getDataType(tokenHierarchy, ts, tokenPre));
                            ts.move(token.offset(tokenHierarchy));
                            ts.moveNext(); //TO DO: Couldn't figure out the type, have to check the return types of the methods etc
                        }
                    }
                    text = "";
                } else if (!(paranCount == 1 && token.text().toString().equals("("))) { //Ignore the first '('
                    text = text + token.text().toString();
                }

                if ((token.text().toString().equals(";")) || (paranCount == 0 && token.text().toString().equals(")"))) {
                    break;
                }

                if ((token.id() != PlsqlTokenId.WHITESPACE) && (token.id() != PlsqlTokenId.LINE_COMMENT) && (token.id() != PlsqlTokenId.BLOCK_COMMENT)) {
                    tokenPre = token;
                }
            }
        }
        return params;
    }

    /**
     * Method that will access the parameter types of the function/procedure definition
     * @param doc
     * @param start
     * @return
     */
    public static List<String> fetchMethodDefParamTypes(final Document doc, final int start) {
        final List<String> params = new ArrayList<String>();
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(start);
            Token<PlsqlTokenId> token = ts.token();
            HashSet<String> keywords = new HashSet<String>();
            while (ts.moveNext()) {
                token = ts.token();
                if ((token.text().toString().equals(",")) || (token.text().toString().equals(")"))) {
                    String type = "";
                    if (keywords.contains("NUMBER")) {
                        type = "NUMBER";
                    } else if (keywords.contains("VARCHAR2")) {
                        type = "VARCHAR2";
                    } else if (keywords.contains("BOOLEAN")) {
                        type = "BOOLEAN";
                    } else if (keywords.contains("DATE")) {
                        type = "DATE";
                    } else if (keywords.contains("EXCEPTION")) {
                        type = "EXCEPTION";
                    } else if (keywords.contains("RECORD")) {
                        type = "RECORD";
                    } else if (keywords.contains("TYPE")) {
                        type = "TYPE";
                    } else if (keywords.contains("ROWTYPE")) {
                        type = "ROWTYPE";
                    }
                    params.add(type);
                    keywords.clear();
                }

                if ((token.text().toString().equalsIgnoreCase("IS")) || (token.text().toString().equals(")")) || (token.text().toString().equals(";"))) {
                    break;
                }

                if (token.id() == PlsqlTokenId.KEYWORD) {
                    keywords.add(token.text().toString().toUpperCase(Locale.ENGLISH));
                }
            }
        }
        return params;
    }
    
    private static int fetchMethodParamsCount(final Document doc, final int start) {
        int noOfParams = 0;
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(start);
            Token<PlsqlTokenId> token = ts.token();
            HashSet<String> keywords = new HashSet<String>();
            while (ts.moveNext()) {
                token = ts.token();
                if ((token.text().toString().equals(",")) || (token.text().toString().equals(")"))) {
                    noOfParams++;
                }

                if ((token.text().toString().equalsIgnoreCase("IS")) || (token.text().toString().equals(")")) || (token.text().toString().equals(";"))) {
                    break;
                }
            }
        }
        return noOfParams;
    }
    
    public static String fetchMethodHeader(final Document doc, final int start) {
        String header = "";
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(start);
            Token<PlsqlTokenId> token = ts.token();
            while (ts.moveNext()) {
                token = ts.token();
                header += token;
                if ((token.text().toString().equalsIgnoreCase("IS")) || (token.text().toString().equals(")")) || (token.text().toString().equals(";"))) {
                    break;
                }
            }
        }
        return header;
    }        

    /**
     * Method that will access the parameter types of the function/procedure definition
     * @param doc
     * @param start
     * @return
     */
    public static int fetchDefaultParams(final Document doc, final int start) {
        int defaultParam = 0;
        final TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        final TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

        if (ts != null) {
            ts.move(start);
            Token<PlsqlTokenId> token = ts.token();
            HashSet<String> keywords = new HashSet<String>();
            while (ts.moveNext()) {
                token = ts.token();
                if ((token.text().toString().equals(",")) || (token.text().toString().equals(")"))) {
                    if (keywords.contains("DEFAULT")) {
                        defaultParam++;
                    }
                    keywords.clear();
                }

                if ((token.text().toString().equalsIgnoreCase("IS")) || (token.text().toString().equals(")")) || (token.text().toString().equals(";"))) {
                    break;
                }

                if (token.id() == PlsqlTokenId.KEYWORD) {
                    keywords.add(token.text().toString().toUpperCase(Locale.ENGLISH));
                }
            }
        }
        return defaultParam;
    }

    /**
     * Get data type of the variable represented by the given token
     * @param tokenHierarchy
     * @param ts
     * @param match
     * @return
     */
    private static String getDataType(final TokenHierarchy tokenHierarchy, final TokenSequence<PlsqlTokenId> ts, final Token<PlsqlTokenId> match) {
        ts.move(match.offset(tokenHierarchy));
        ts.movePrevious();
        int defOffset = -1;
        Token<PlsqlTokenId> tokenPre = ts.token();

        while (ts.movePrevious()) {
            Token<PlsqlTokenId> token = ts.token();
            PlsqlTokenId tokenID = token.id();
            String image = token.text().toString();

            if ((tokenID == PlsqlTokenId.IDENTIFIER) && (image.equalsIgnoreCase(match.text().toString()))) {
                defOffset = token.offset(tokenHierarchy);

                //Check whether this can be a variable definition if so the
                //identifier must be surrounded by spaces
                boolean canBeVariableDef = false;
                Token<PlsqlTokenId> tmp = token;
                if (ts.moveNext()) {
                    tmp = ts.token();
                }
                if ((tmp.id() == PlsqlTokenId.WHITESPACE) || (tmp.id() == PlsqlTokenId.LINE_COMMENT)
                        || (tmp.id() == PlsqlTokenId.BLOCK_COMMENT)) {
                    canBeVariableDef = true;
                }
                ts.move(defOffset);
                ts.moveNext();

                if (canBeVariableDef) {
                    //Chech whether this is a variable declaration based on the context
                    if (tokenPre.text().toString().equalsIgnoreCase("IS")) {
                        //check whether next is IS
                        if (getPreviousNonWhitespace(ts, true)) {
                            token = ts.token();
                        }

                        if ((token.text().toString().equalsIgnoreCase("CURSOR"))
                                || (token.text().toString().equalsIgnoreCase("TYPE"))
                                || (token.text().toString().equalsIgnoreCase("SUBTYPE"))) {
                            return token.text().toString();
                        }
                    }

                    ts.move(defOffset);
                    ts.moveNext();

                    //To forward to find ';', ',' or ')'
                    Set<String> tokenSet = new HashSet<String>();
                    while (ts.moveNext()) {
                        token = ts.token();
                        if ((token.text().toString().equals(";")) || (token.text().toString().equals(",")) || (token.text().toString().equals(")"))) {
                            break;
                        }

                        if ((token.id() != PlsqlTokenId.WHITESPACE)
                                && (token.id() != PlsqlTokenId.LINE_COMMENT)
                                && (token.id() != PlsqlTokenId.BLOCK_COMMENT)) {
                            tokenSet.add(token.text().toString().toUpperCase(Locale.ENGLISH));
                        }
                    }

                    //Now we have got the tokens from the indentifier name to the
                    //first ';' or ',' or ')' check whether there is a datatype here
                    if (tokenSet.contains("NUMBER")) {
                        return "NUMBER";
                    } else if (tokenSet.contains("VARCHAR2")) {
                        return "VARCHAR2";
                    } else if (tokenSet.contains("BOOLEAN")) {
                        return "BOOLEAN";
                    } else if (tokenSet.contains("DATE")) {
                        return "DATE";
                    } else if (tokenSet.contains("EXCEPTION")) {
                        return "EXCEPTION";
                    } else if (tokenSet.contains("RECORD")) {
                        return "RECORD";
                    } else if ((tokenSet.contains("TYPE")) && (tokenSet.contains("%"))) {
                        return "TYPE";
                    } else if ((tokenSet.contains("ROWTYPE")) && (tokenSet.contains("%"))) {
                        return "ROWTYPE";
                    }

                    ts.move(defOffset);
                    ts.moveNext();
                }
            }

            if ((tokenID != PlsqlTokenId.WHITESPACE) && (tokenID != PlsqlTokenId.LINE_COMMENT)
                    && (tokenID != PlsqlTokenId.BLOCK_COMMENT)) {
                tokenPre = token;
            }
        }
        return "UNKNOWN";
    }

    /**
     * Method that will find & return the all the FUNCTIONs/ PROCEDUREs  of the plsql file
     * with the given name
     * @param blockHier
     * @param name
     * @param matchList
     */
    public static void findMatchingImplBlocks(final List<PlsqlBlock> blockHier, final String name, final List<PlsqlBlock> matchList) {
        PlsqlBlock matchTmp = null;

        for (int i = 0; i < blockHier.size(); i++) {
            PlsqlBlock temp = blockHier.get(i);
            if (temp.getName().equalsIgnoreCase(name)) {
                if ((temp.getType() == PlsqlBlockType.FUNCTION_IMPL)
                        || (temp.getType() == PlsqlBlockType.PROCEDURE_IMPL)) {
                    matchTmp = temp;
                    matchList.add(matchTmp);
                }
            }

            findMatchingImplBlocks(temp.getChildBlocks(), name, matchList);
        }
    }

    /**
     * Method that will find & return the all the FUNCTIONs/ PROCEDUREs  of the plsql file
     * with the given name
     * @param blockHier
     * @param name
     * @param matchList
     */
    public static void findMatchingDefBlocks(final List<PlsqlBlock> blockHier, final String name, final List<PlsqlBlock> matchList) {
        PlsqlBlock matchTmp = null;

        for (int i = 0; i < blockHier.size(); i++) {
            PlsqlBlock temp = blockHier.get(i);
            if (temp.getName().equalsIgnoreCase(name)) {
                if ((temp.getType() == PlsqlBlockType.FUNCTION_DEF)
                        || (temp.getType() == PlsqlBlockType.PROCEDURE_DEF)) {
                    matchTmp = temp;
                    matchList.add(matchTmp);
                }
            }

            findMatchingDefBlocks(temp.getChildBlocks(), name, matchList);
        }
    }

    /**
     * Return previous non whitespace token of files
     * @param ts
     * @param ignoreComment
     * @return
     */
    public static boolean getPreviousNonWhitespace(final TokenSequence<PlsqlTokenId> ts, final boolean ignoreComment) {
        boolean movePrevious = ts.movePrevious();
        Token<PlsqlTokenId> tmp = ts.token();

        while (movePrevious) {
            if (tmp.id() == PlsqlTokenId.WHITESPACE) {
                movePrevious = ts.movePrevious();
                tmp = ts.token();
            } else {
                if ((ignoreComment == true) && (tmp.id() == PlsqlTokenId.LINE_COMMENT
                        || tmp.id() == PlsqlTokenId.BLOCK_COMMENT)) {
                    movePrevious = ts.movePrevious();
                    tmp = ts.token();
                } else {
                    break;
                }

            }
        }
        return movePrevious;
    }

    public static List<PlsqlBlock> getBlockHierarchy(final DataObject dataObject) {
        final Document doc = PlsqlFileUtil.getDocument(dataObject);
        if (doc != null) {
            final PlsqlBlockFactory blockFactory = getBlockFactory(dataObject);
            blockFactory.initHierarchy(doc);
            return blockFactory.getBlockHierarchy();
        }

        return null;
    }

    /**
     * Method that will return the relevant block factory for the dataobject
     * @param obj
     * @return
     */
    public static PlsqlBlockFactory getBlockFactory(final DataObject obj) {
        return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
    }

    /**
     * Method that will return the relevant block factory for the document
     * @param doc
     * @return
     */
    public static PlsqlBlockFactory getBlockFactory(final Document doc) {
        final Object obj = doc.getProperty(Document.StreamDescriptionProperty);
        if (obj instanceof DataObject) {
            return ((Lookup.Provider) obj).getLookup().lookup(PlsqlBlockFactory.class);
        }

        return null;
    }

    /**
     * Get Return next non whitespace token for files
     * @param ts
     * @param ignoreComment: if true will ignore comments also
     * @return
     */
    public static boolean getNextNonWhitespace(final TokenSequence<PlsqlTokenId> ts, final boolean ignoreComment) {
        boolean moveNext = ts.moveNext();
        Token<PlsqlTokenId> tmp = ts.token();


        while (moveNext) {
            if (tmp.id() == PlsqlTokenId.WHITESPACE) {
                moveNext = ts.moveNext();
                tmp = ts.token();
            } else {
                if ((ignoreComment == true) && (tmp.id() == PlsqlTokenId.LINE_COMMENT
                        || tmp.id() == PlsqlTokenId.BLOCK_COMMENT)) {
                    moveNext = ts.moveNext();
                    tmp = ts.token();
                } else {
                    break;
                }

            }
        }

        return moveNext;
    }

    /**
     * Method that will return the package name containing the offset
     * @param lstBlock
     * @param offset
     * @return
     */
    public static String getPackageName(final PlsqlBlockFactory blockFac, final int offset) {
        final List<PlsqlBlock> lstBlock = blockFac.getBlockHierarchy();
        String packageName = null;
        int count = lstBlock.size();
        for (int i = 0; i < count; i++) {
            PlsqlBlock tmp = lstBlock.get(i);
            if ((tmp.getType() == PlsqlBlockType.PACKAGE || tmp.getType() == PlsqlBlockType.PACKAGE_BODY)
                    && (tmp.getStartOffset() <= offset) && (tmp.getEndOffset() >= offset)) {
                packageName = tmp.getName();
                break;
            }
        }

        return packageName;
    }

    /**
     * Method that will return the method name containing the offset
     * @param lstBlock
     * @param offset
     * @return
     */
    public static String getMethodName(final PlsqlBlockFactory blockFac, final int offset) {
        final List<PlsqlBlock> blocks = blockFac.getBlockHierarchy();
        int count = blocks.size();
        for (int i = 0; i < count; i++) {
            PlsqlBlock block = blocks.get(i);
            if ((block.getType() == PlsqlBlockType.FUNCTION_DEF || block.getType() == PlsqlBlockType.FUNCTION_IMPL || block.getType()==PlsqlBlockType.PROCEDURE_DEF || block.getType()==PlsqlBlockType.PROCEDURE_IMPL)
                    && (block.getStartOffset() <= offset) && (block.getEndOffset() >= offset)) {
                return block.getName();
            }
        }

        return null;
    }

    /**
     * Method that will return the package name
     * @param lstBlock
     * @return
     */
    public static String getPackageName(final PlsqlBlockFactory blockFac) {
        List<PlsqlBlock> lstBlock = blockFac.getBlockHierarchy();
        String packageName = "";
        int count = lstBlock.size();
        for (int i = 0; i < count; i++) {
            final PlsqlBlock tmp = lstBlock.get(i);
            if ((tmp.getType() == PlsqlBlockType.PACKAGE || tmp.getType() == PlsqlBlockType.PACKAGE_BODY)) {
                packageName = tmp.getName();
                break;
            }
        }

        return packageName;
    }
}
