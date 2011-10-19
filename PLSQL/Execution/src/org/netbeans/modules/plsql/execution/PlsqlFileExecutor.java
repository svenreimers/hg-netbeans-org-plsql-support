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

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.netbeans.modules.plsql.filetype.PlsqlEditor;
import org.netbeans.modules.plsql.filetype.StatementExecutionHistory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import java.awt.Component;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.db.dataview.api.DataView;
import org.netbeans.modules.db.sql.execute.SQLExecutionResult;
import org.netbeans.modules.db.sql.execute.SQLExecutionResults;
import org.netbeans.modules.db.sql.execute.StatementInfo;
import org.netbeans.modules.db.sql.history.SQLHistory;
import org.netbeans.modules.db.sql.history.SQLHistoryManager;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;
import org.openide.windows.TopComponent;

public class PlsqlFileExecutor {
    
    private boolean cancel = false;
    private final RequestProcessor rp = new RequestProcessor("SQLExecution", 1, true);
    // execution results. Not synchronized since accessed only from rp of throughput 1.
    private SQLExecutionResults executionResults;
    private final DatabaseConnection connection;
    private Connection debugConnection;
    private DatabaseContentManager cache;
    private PlsqlEditor plsqlEditor;
    private final InputOutput preparedIO;
    private final DatabaseConnectionManager connectionProvider;
    private Savepoint firstSavepoint = null;
    
    public PlsqlFileExecutor(DatabaseConnectionManager connectionProvider, DatabaseConnection connection) {
        this.connection = connection;
        this.preparedIO = null;
        this.connectionProvider = connectionProvider;
        this.cache = DatabaseContentManager.getInstance(connection);
    }
    
    public PlsqlFileExecutor(DatabaseConnectionManager connectionProvider, Connection debugConnection, InputOutput io) {
        this.connection = null;
        this.debugConnection = debugConnection;
        this.preparedIO = io;
        this.connectionProvider = connectionProvider;
        this.cache = DatabaseContentManager.getInstance(connection);
    }
    
    public void cancel() {
        cancel = true;
    }

    /**
     * Method that will execute Dbmb_Output.Enable();
     * @param con
     */
    private void enableDbmsOut(Connection con) {
        Statement stm = null;
        try {
            stm = con.createStatement();
            stm.setEscapeProcessing(false);
            stm.execute("DECLARE\nBEGIN\nDbms_Output.Enable;\nEND;");
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }
    
    private boolean executeSelect(String query, DatabaseConnection con, Document doc, String label) throws SQLException {
        String formattedQuery = formatQuery(query, con);
        DataObject obj = FileExecutionUtil.getDataObject(doc);
        SQLExecutor executor = new SQLExecutor(obj, con, formattedQuery, label);
        RequestProcessor.Task task = rp.create(executor);
        executor.setTask(task);
        task.run();
        return false;
    }
    final static int NORMAL = 0;
    final static int IN_LITERAL = 1;
    final static int COMMENT_CANDIDATE = 3;
    
    private String formatQuery(final String query, DatabaseConnection databaseConnection) {
        StringBuilder nonCommentQuery = new StringBuilder();
        String[] lines = query.split("\\n");
        char escapeCharacter = (char) 0;
        for (String line : lines) {
            int state = NORMAL;
            for (int i = 0; i < line.length(); i++) {
                if (state == NORMAL) {
                    if (line.charAt(i) == '\'') {
                        state = IN_LITERAL;
                        if (i > 0 && i < line.length() - 1) {
                            char pre = line.charAt(i - 1);
                            if (pre == 'q' || pre == 'Q') {
                                escapeCharacter = line.charAt(i + 1);
                            } else {
                                escapeCharacter = (char) 0;
                            }
                        }
                    } else if (line.charAt(i) == '-') {
                        state = COMMENT_CANDIDATE;
                    }
                } else if (state == IN_LITERAL) {
                    if (line.charAt(i) == '\'') {
                        if (escapeCharacter == (char) 0 || line.charAt(i - 1) == escapeCharacter) {
                            state = NORMAL;
                        }
                    } else if (line.charAt(i) == '\\') {
                        i++;
                    }
                } else if (state == COMMENT_CANDIDATE) {
                    if (line.charAt(i) == '-') {
                        if (i > 1) {
                            nonCommentQuery.append(line.substring(0, i - 1)).append("\n");
                        }
                        break;
                    }
                    state = NORMAL;
                }
            }
            if (state == NORMAL) {
                nonCommentQuery.append(line).append("\n");
            }
        }
        nonCommentQuery.deleteCharAt(nonCommentQuery.length() - 1);
        
        String newQuery = "";
        String token;
        boolean format = false;
        
        StringTokenizer tokenizer = new StringTokenizer(nonCommentQuery.toString(), " \t\n");
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            
            if (token.equalsIgnoreCase("FROM")) {
                format = true;
            } else if (token.equalsIgnoreCase("WHERE") || token.equalsIgnoreCase("ORDER")
                    || token.equalsIgnoreCase("GROUP") || token.equalsIgnoreCase("HAVING")) {
                format = false;
            }
            
            if (format) {
                boolean isUpper = false;
                //Tokenize with '.'
                StringTokenizer dotTokenizer = new StringTokenizer(token, ".");
                String dotToken;
                while (dotTokenizer.hasMoreTokens()) {
                    dotToken = dotTokenizer.nextToken();
                    //We can safely assume that we have connected to the database here
                    if (cache.isTable(dotToken, databaseConnection) || cache.isView(dotToken, databaseConnection)) {
                        isUpper = true;
                    }
                }
                
                if (!isUpper) {
                    //Tokenize with ','
                    StringTokenizer otherTokenizer = new StringTokenizer(token, ",");
                    String otherToken;
                    while (otherTokenizer.hasMoreTokens()) {
                        otherToken = otherTokenizer.nextToken();
                        //We can safely assume that we have connected to the database here
                        if (!otherToken.startsWith("'")
                                && cache.isTable(otherToken, databaseConnection) || cache.isView(otherToken, databaseConnection)) {
                            isUpper = true;
                        }
                    }
                }
                
                if (isUpper) {
                    token = token.toUpperCase(Locale.ENGLISH);
                }
            }
            
            newQuery = newQuery + token + " ";
        }
        
        return newQuery;
    }
    
    private boolean describeObject(DatabaseConnection con, Document doc, StringTokenizer tokenizer, InputOutput io) throws SQLException {
        if (!tokenizer.hasMoreTokens()) {
            io.getErr().println("Syntax: DESCRIBE [object]");
            return false;
        }
        String objectName = tokenizer.nextToken().toUpperCase(Locale.ENGLISH);
        String objectOwner = null;
        
        if (objectName.contains(".")) { //handle schema.object format e.g. ifsapp.customer
            final String[] result = objectName.split("\\.");
            if (result != null && result.length == 2) {
                objectOwner = result[0];
                objectName = result[1];
            }
        }
        
        String query = "SELECT t.COLUMN_NAME \"Name\", "
                + "t.data_type||decode(t.data_type,'VARCHAR2','('||t.char_length||')', "
                + "'DATE','','NUMBER',decode(t.data_precision,null,'','('||t.data_precision||"
                + "decode(t.data_scale,0,'',null,'',','||t.data_scale)||')'),'') \"Type\","
                + "decode(t.nullable,'Y',' ','NOT NULL') \"Nullable\", "
                + "t.data_default \"Default\", "
                + "substr(to_char(t.column_id), 1, 5) \"Id\", "
                + "c.comments \"Comments\" "
                + "FROM ALL_TAB_COLUMNS t, ALL_COL_COMMENTS c "
                + "WHERE t.TABLE_NAME = c.TABLE_NAME "
                + "AND t.COLUMN_NAME = c.COLUMN_NAME "
                + "AND t.OWNER = c.OWNER "
                + ((objectOwner != null && !objectOwner.equals("")) ? "AND t.OWNER = '" + objectOwner + "' " : "") //set owner only if given by user
                + "AND t.TABLE_NAME = '" + objectName + "' ORDER BY t.COLUMN_ID";
        executeSelect(query, con, doc, objectName);
        return true;
    }
    
    private void processDbmsOutputMessages(Connection con, OutputWriter out) {
        String text = "BEGIN DBMS_OUTPUT.GET_LINE(?, ?); END;";
        CallableStatement stmt = null;
        try {
            stmt = con.prepareCall(text);
            stmt.registerOutParameter(1, java.sql.Types.VARCHAR);
            stmt.registerOutParameter(2, java.sql.Types.NUMERIC);
            int status = 0;
            while (status == 0) {
                stmt.execute();
                String output = stmt.getString(1);
                status = stmt.getInt(2);
                if (status == 0) {
                    out.println(output);
                }
            }
        } catch (SQLException e) {
            out.println("!!!Errors while fetching dbms_output:");
            out.println(e.toString());
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }
    
    private InputOutput initializeIO(String fileName, String displayName, DataObject dataObj) {
        if (this.preparedIO != null) {
            return this.preparedIO;
        }
        String startMsg = "Deploying " + FileExecutionUtil.getActivatedFileName(dataObj);
        if (fileName.endsWith(".tdb")) {
            startMsg = "Executing " + fileName;
        }
        InputOutput io = null;
        try {
            io = IOProvider.getDefault().getIO(displayName, false);
            //if the window is a pl/sql test window keep the old output. Otherwise flush
            if (fileName.startsWith(SQLCommandWindow.SQL_EXECUTION_FILE_PREFIX)) {
                if (io.isClosed()) {
                    //If closed previously reset and flush
                    io.getOut().reset();
                    io.getErr().reset();
                    io.getOut().flush();
                    io.getErr().flush();
                }
                io.getOut().println();
            } else {
                io.getOut().reset();
                io.getErr().reset();
                io.getOut().flush();
                io.getErr().flush();
            }
            io.select();
            io.getOut().println(startMsg);
            io.getOut().println("-------------------------------------------------------------");
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return io;
    }

    //store in object history if this is an SQL Command window (*.tdb)
    private void addToHistory(Document doc) {
        DataObject obj = FileExecutionUtil.getDataObject(doc);
        FileObject file = obj.getPrimaryFile();
        if (file == null) {
            return;
        }
        String extension = file.getExt();
        if ("tdb".equalsIgnoreCase(extension)) {
            StatementExecutionHistory history = obj.getLookup().lookup(StatementExecutionHistory.class);
            try {
                history.addEntry(doc.getText(0, doc.getLength()));
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        
    }
    
    public InputOutput executePLSQL(List executableObjs, Document doc, boolean hidden, boolean autoCommit) {
        final PlsqlFileValidatorService validator = Lookup.getDefault().lookup(PlsqlFileValidatorService.class);
        Project project = null;
        Object object = doc.getProperty(Document.StreamDescriptionProperty);
        if (object instanceof DataObject) {
            FileObject fo = ((DataObject) object).getPrimaryFile();
            project = FileOwnerQuery.getOwner(fo);
        }

        //store in object history if this is an SQL Command window (*.tdb)
        boolean ignoreDefines = false;
        addToHistory(doc);
        cancel = false;
        InputOutput io = null;
        boolean deploymentOk = true;
        if (debugConnection == null && connection.getJDBCConnection() == null) {
            JOptionPane.showMessageDialog(null, "Connect to the Database");
            return null;
        }

        //Defines and define
        HashMap<String, String> definesMap = new HashMap<String, String>();
        char define = '&';
        DataObject dataObj = FileExecutionUtil.getDataObject(doc);
        plsqlEditor = getPlsqlEditor(dataObj);
        String endMsg = "Done deploying " + FileExecutionUtil.getActivatedFileName(dataObj);
        long startTime = System.currentTimeMillis();
        String fileName = dataObj.getPrimaryFile().getNameExt();
        boolean moreRowsToBeFetched = false;
        //Check whether this is the excution window file
        if (fileName.endsWith(".tdb")) {
            endMsg = "Finished executing command ";
        }
        Connection con = null;
        Statement stm = null;
        String firstWord = null;
        boolean commit = false;

        //quick & dirty fix to avoid having output tabs for the SQL Execution window (unless there's an exception)
        //first check to see if this is a simple select statement and if so treat it separately.
        if (executableObjs.size() == 1) {
            PlsqlExecutableObject executionObject = (PlsqlExecutableObject) executableObjs.get(0);
            if (executionObject.getType() == PlsqlExecutableObjectType.STATEMENT || executionObject.getType() == PlsqlExecutableObjectType.UNKNOWN) {
                String plsqlText = executionObject.getPlsqlString();
                try {
                    // String firstWord;
                    firstWord = null;
                    StringTokenizer tokenizer = new StringTokenizer(plsqlText, " \t\n");
                    if (tokenizer.hasMoreTokens()) {
                        firstWord = tokenizer.nextToken();
                    } else {
                        firstWord = plsqlText;
                    }
                    if (firstWord.equalsIgnoreCase("SELECT")) {
                        if (plsqlEditor != null) {
                            plsqlEditor.closeResultSetTabs();
                        }
                        //replace aliases since there are aliases in PROMPTS
                        if (!ignoreDefines) {
                            plsqlText = replaceAliases(plsqlText, definesMap, define, io);
                        }
                        moreRowsToBeFetched = executeSelect(plsqlText, connection, doc, null);
                        return null;
                    } else if (firstWord.equalsIgnoreCase("DESC") || firstWord.equalsIgnoreCase("DESCRIBE")) {
                        describeObject(connection, doc, tokenizer, io);
                        return null;
                    }
                    
                } catch (SQLException sqlEx) {
                    try {
                        io = initializeIO(fileName,getIOTabName(executableObjs, fileName, dataObj.getNodeDelegate().getDisplayName()), dataObj);
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = executionObject.getStartLineNo() + errLine - 1;
                        String msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        PlsqlOutputListener outList = new PlsqlOutputListener();
                        outList.setDocLinesArray(executionObject.getDocLinesArray());
                        outList.setOriginalFileName(executionObject.getOriginalFileName());
                        outList.setLineNo(outLine);
                        io.getErr().println((new StringBuilder()).append("!!!Error Executing Statement ").append(executionObject).toString());
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        io.getOut().close();
                        io.getErr().close();
                        return io;
                    } catch (IOException ex) {
                        connectionProvider.setOnline(false);
                        return null;
                    }
                }
            }
        }
        try {
            
            
            io = initializeIO(fileName, getIOTabName(executableObjs, fileName,dataObj.getNodeDelegate().getDisplayName()), dataObj);
            con = debugConnection != null ? debugConnection : connection.getJDBCConnection();
            con.setAutoCommit(false);
            enableDbmsOut(con);
            firstSavepoint = con.setSavepoint();
            //savepointsCreated.put(new Integer(firstSavepoint.getSavepointId()),firstSavepoint);

            stm = con.createStatement();
            stm.setEscapeProcessing(false);
            if (connectionProvider.isUsagesEnabled() && validator.isValidPackage(dataObj)) {
                String plsqlText = "ALTER SYSTEM SET PLSCOPE_SETTINGS = 'IDENTIFIERS:ALL' \t\n";
                try {
                    //io.getOut().println((new StringBuilder()).append("> Enabling Usages for ").append(fileName));
                    stm.execute(plsqlText);
                } catch (SQLException sqlEx) {
                    int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                    int outLine = errLine - 1;
                    String msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                    // io.getErr().println((new StringBuilder()).append("!!!Error Creating View ").append(exeObjName).toString());
                    io.getOut().println(plsqlText);
                    deploymentOk = false;
                }
            }
            boolean firstSelectStatement = true;
            for (int i = 0; i < executableObjs.size(); i++) {
                if (cancel) {
                    io.getErr().println("!!!Execution cancelled. Performing rollback");
                    // con.rollback();
                    con.rollback(firstSavepoint);
                    return io;
                }
                PlsqlExecutableObject exeObj = (PlsqlExecutableObject) executableObjs.get(i);
                int lineNumber = exeObj.getStartLineNo();
                String plsqlText = exeObj.getPlsqlString();
                
                String exeObjName = exeObj.getExecutableObjName().toUpperCase(Locale.ENGLISH);
                if ((exeObj.getType() != PlsqlExecutableObjectType.UNKNOWN) && (exeObj.getType() != PlsqlExecutableObjectType.COMMENT)) {
                    //replace aliases since there are aliases in PROMPTS
                    if (!ignoreDefines) {
                        plsqlText = replaceAliases(plsqlText, definesMap, define, io);
                        exeObjName = replaceAliases(exeObjName, definesMap, define, io);
                    }
                }
                PlsqlOutputListener outList = new PlsqlOutputListener();
                outList.setDocLinesArray(exeObj.getDocLinesArray());
                outList.setOriginalFileName(exeObj.getOriginalFileName());
                String msg;
                if (exeObj.getType() == PlsqlExecutableObjectType.VIEW) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("> PL/SQL View ").append(exeObjName).append(" Created Successfully").toString());
                        continue;
                    } catch (SQLException sqlEx) {
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = errLine == -1 ? lineNumber : lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getErr().println((new StringBuilder()).append("!!!Error Creating View ").append(exeObjName).toString());
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.STATEMENT) {
                    try {
                        // String firstWord;
                        firstWord = null;
                        StringTokenizer tokenizer = new StringTokenizer(plsqlText, " \t\n;");
                        if (tokenizer.hasMoreTokens()) {
                            firstWord = tokenizer.nextToken();
                        } else {
                            firstWord = plsqlText;
                        }
                        
                        if (plsqlText.toUpperCase().contains("INSERT") || plsqlText.toUpperCase().contains("UPDATE") || plsqlText.toUpperCase().contains("DELETE")) {
                            commit = true;
                        }
                        
                        if (firstWord.equalsIgnoreCase("SELECT")) {
                            //this should really never happen... Unless there are multiple parts of a file and some sections are select statements
                            if (plsqlEditor != null && firstSelectStatement) {
                                plsqlEditor.closeResultSetTabs();
                                firstSelectStatement = false;
                            }
                            moreRowsToBeFetched = executeSelect(plsqlText, connection, doc, null);
                            continue;
                        } else {
                            io.select();
                            io.getOut().println((new StringBuilder()).append("> Executing Statement "));
                            io.getOut().println("   " + plsqlText.replaceAll("\n", "\n   "));
                            stm.execute(plsqlText);
                        }
                        continue;
                    } catch (SQLException sqlEx) {
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getErr().println((new StringBuilder()).append("!!!Error Executing Statement ").append(exeObjName).toString());
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.TRIGGER) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("> PL/SQL Trigger ").append(exeObjName).append(" Created Successfully").toString());
                        continue;
                    } catch (SQLException sqlEx) {
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getErr().println((new StringBuilder()).append("!!!Error Creating Trigger ").append(exeObjName).toString());
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.JAVASOURCE) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("> Java Source ").append(exeObjName).append(" Deployed Successfully").toString());
                        continue;
                    } catch (SQLException sqlEx) {
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getErr().println((new StringBuilder()).append("!!!Error Deploying Java Source ").append(exeObjName).toString());
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.TABLECOMMENT) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("> Table Comments On ").append(exeObjName).append(" Added Successfully").toString());
                        continue;
                    } catch (SQLException sqlEx) {
                        io.getErr().println((new StringBuilder()).append("!!!Error Adding Table Comments On ").append(exeObjName).toString());
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.COLUMNCOMMENT) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("> Column Comments On ").append(exeObjName).append(" Added Successfully").toString());
                        continue;
                    } catch (SQLException sqlEx) {
                        io.getErr().println((new StringBuilder()).append("!!!Error Adding Column Comments On ").append(exeObjName).toString());
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.DECLAREEND) {
                    try {
                        stm.executeUpdate(plsqlText);
                        processDbmsOutputMessages(con, io.getOut());
                        io.getOut().println("> PL/SQL Block Executed Successfully");
                        continue;
                    } catch (SQLException sqlEx) {
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getErr().println("!!!Error Occurred While Executing PL/SQL Block");
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        io.getOut().println("!!!Error detected. Performing Rollback");
                        deploymentOk = false;
                        try {
                            // con.rollback();
                            con.rollback(firstSavepoint);
                        } catch (SQLException ex) {
                        }
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.BEGINEND) {
                    if (plsqlText.toUpperCase().contains("INSERT") || plsqlText.toUpperCase().contains("UPDATE") || plsqlText.toUpperCase().contains("DELETE")) {
                        commit = true;
                    }
                    try {
                        stm.executeUpdate(plsqlText);
                        processDbmsOutputMessages(con, io.getOut());
                        io.getOut().println("> PL/SQL Block Executed Successfully");
                        continue;
                    } catch (SQLException sqlEx) {
                        int errLine = getLineNumberFromMsg(sqlEx.getMessage());
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        outList.setLineNo(outLine);
                        io.getErr().println("!!!Error Occurred While Executing PL/SQL Block");
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.UNKNOWN) {
                    if (plsqlText.toUpperCase().contains("INSERT") || plsqlText.toUpperCase().contains("UPDATE") || plsqlText.toUpperCase().contains("DELETE")) {
                        commit = true;
                    }
                    //Parse aliases
                    define = getAliases(definesMap, doc, exeObj.getStartOffset(), exeObj.getEndOffset(), define, io);
                    //Replace aliases
                    if (!ignoreDefines) {
                        plsqlText = replaceAliases(plsqlText, definesMap, define, io);
                    }
                    plsqlText = plsqlText.trim();
                    // String firstWord;
                    firstWord = null;
                    StringTokenizer tokenizer = new StringTokenizer(plsqlText, " \t\n");
                    try {
                        if (tokenizer.hasMoreTokens()) {
                            firstWord = tokenizer.nextToken();
                        } else {
                            firstWord = plsqlText;
                        }
                        if (firstWord.equalsIgnoreCase("SELECT")) {
                            //this should really never happen... Unless there are multiple parts of a file and some sections are select statements
                            if (plsqlEditor != null) {
                                plsqlEditor.closeResultSetTabs();
                            }
                            moreRowsToBeFetched = executeSelect(plsqlText, connection, doc, null);
                            continue;
                        } else {
                            io.select();
                            if (firstWord.equalsIgnoreCase("DESC") || firstWord.equalsIgnoreCase("DESCRIBE")) {
                                describeObject(connection, doc, tokenizer, io);
                                continue;
                            } else if (firstWord.equalsIgnoreCase("PROMPT")) {
                                if (plsqlText.length() > 7) {
                                    io.getOut().println(plsqlText.substring(7));
                                }
                                continue;
                            } else if (firstWord.equalsIgnoreCase("SHOW")) {
                                continue;
                            } else if (firstWord.equalsIgnoreCase("SET")) {
                                if (tokenizer.hasMoreTokens()) {
                                    String setting = tokenizer.nextToken();
                                    if ("DEFINE".equalsIgnoreCase(setting)) {
                                        if (tokenizer.hasMoreElements()) {
                                            String token = tokenizer.nextToken();
                                            ignoreDefines = "OFF".equalsIgnoreCase(token);
                                            if (!ignoreDefines && token.length() == 1) {
                                                define = token.charAt(0);
                                            }
                                        }
                                    }
                                    
                                }
                                continue;
                            } else if (firstWord.equalsIgnoreCase("DEFINE") || firstWord.equalsIgnoreCase("DEF") || firstWord.equalsIgnoreCase("DEFI") || firstWord.equalsIgnoreCase("DEFIN")) {
                                continue;
                            } else if (firstWord.equalsIgnoreCase("UNDEFINE") || firstWord.equalsIgnoreCase("UNDEF")) {
                                continue;
                            } else if (firstWord.equalsIgnoreCase("EXECUTE") || firstWord.equalsIgnoreCase("EXEC")) {
                                if (!plsqlText.trim().endsWith(";")) {
                                    plsqlText += ";";
                                }
                                plsqlText = "BEGIN\n" + plsqlText.substring(firstWord.length()) + "\nEND;";
                            } else if (plsqlText.equals(";") || plsqlText.equals("/")) {
                                continue;
                            }
                        }
                        //Ingore ';' '/' which are added manually
                        if ((plsqlText.equals(";")) || (plsqlText.equals("/")) || (plsqlText.equals(""))) {
                            continue;
                        }
                        io.getOut().println("> Executing Statement:");
                        io.getOut().println("   " + plsqlText.replaceAll("\n", "\n   "));
                        stm.executeUpdate(plsqlText);
                        SQLWarning warn = stm.getWarnings();
                        processDbmsOutputMessages(con, io.getOut());
                        if (warn == null) {
                            stm.clearWarnings();
                            continue;
                        } else {
                            int errLine = getLineNumberFromMsg(warn.getMessage());
                            int outLine = lineNumber + errLine - 1;
                            msg = getmodifiedErorrMsg(warn.getMessage(), outLine);
                            outList.setLineNo(outLine);
                            io.getErr().println("---Warning Occurred While Executing Statement ");
                            io.getErr().println(msg, outList);
                            break;
                        }
                    } catch (SQLException sqlEx) {
                        io.getErr().println("!!!Error Occurred While Executing Statement ");
                        String completeMsg = sqlEx.getMessage();
                        if (completeMsg != null) {
                            String[] lines = completeMsg.split("\n");
                            for (String line : lines) {
                                int errLine = getLineNumberFromMsg(line);
                                if (errLine == -1) {
                                    io.getOut().println(line);
                                } else {
                                    String[] nameCandidate = line.split("\"");
                                    PlsqlOutputListener listener = outList;
                                    if (nameCandidate.length == 3) { //object name included
                                        String objectName = nameCandidate[1];
                                        if (objectName.contains(".")) {
                                            objectName = objectName.substring(objectName.lastIndexOf(".") + 1);
                                        }
                                        listener = new PlsqlOutputListener(project, objectName, errLine);
                                    } else {
                                        listener.setLineNo(lineNumber + errLine - 1);
                                    }
                                    io.getErr().println(line, listener);
                                }
                            }
                        }
//                  int errLine = getLineNumberFromMsg(sqlEx.getMessage());
//                  int outLine = lineNumber + errLine - 1;
//                  msg = getmodifiedErorrMsg(sqlEx.getMessage(), outLine);
                        if (!autoCommit) {
                            try {
                                io.getOut().println("!!!Error detected. Performing rollback");
                                con.rollback(firstSavepoint);
                            } catch (SQLException ex) {
                            }
                        }
                        deploymentOk = false;
                        break;
                    }
                }
                List errLst;
                boolean exception = false;
                String excMessage = "";
                if (exeObj.getType() == PlsqlExecutableObjectType.PROCEDURE) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("PL/SQL Procedure ").append(exeObjName).append(" Created Successfully").toString());
                    } catch (SQLException sqlEx) {
                        exception = true;
                        excMessage = sqlEx.getMessage();
                    }
                    errLst = getPackageerrors(exeObjName, "PROCEDURE");
                    if ((errLst.isEmpty()) && (!exception)) {
                        continue;
                    } else if ((errLst.isEmpty()) && (exception)) {
                        io.getErr().println((new StringBuilder()).append("!!!Procedure ").append(exeObjName).append(" Created With Compilation Errors").toString());
                        int errLine = getLineNumberFromMsg(excMessage);
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(excMessage, outLine);
                        outList.setLineNo(outLine);
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    } else {
                        io.getErr().println((new StringBuilder()).append("!!!Procedure ").append(exeObjName).append(" Created With Compilation Errors").toString());
                        for (int a = 0; a < errLst.size(); a++) {
                            PlsqlErrorObject errObj = (PlsqlErrorObject) errLst.get(a);
                            int stNo = exeObj.getStartLineNo();
                            int errNo = errObj.getLineNumber();
                            if (errNo <= 0) {
                                errNo = 1;
                            }
                            int lineNo = stNo + errNo - 1;
                            msg = errObj.getErrorMsg();
                            msg = getmodifiedErorrMsg(msg, lineNo);
                            outList = new PlsqlOutputListener();
                            outList.setDocLinesArray(exeObj.getDocLinesArray());
                            outList.setLineNo(lineNo);
                            outList.setOriginalFileName(exeObj.getOriginalFileName());
                            io.getErr().println(msg, outList);
                        }
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.FUNCTION) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("PL/SQL Function ").append(exeObjName).append(" Created Successfully").toString());
                    } catch (SQLException sqlEx) {
                        exception = true;
                        excMessage = sqlEx.getMessage();
                    }
                    errLst = getPackageerrors(exeObjName, "FUNCTION");
                    if ((errLst.isEmpty()) && (!exception)) {
                        continue;
                    } else if ((errLst.isEmpty()) && (exception)) {
                        io.getErr().println((new StringBuilder()).append("!!!Function ").append(exeObjName).append(" Created With Compilation Errors").toString());
                        int errLine = getLineNumberFromMsg(excMessage);
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(excMessage, outLine);
                        outList.setLineNo(outLine);
                        io.getOut().println(plsqlText);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    } else {
                        io.getErr().println((new StringBuilder()).append("!!!Function ").append(exeObjName).append(" Created With Compilation Errors").toString());
                        for (int a = 0; a < errLst.size(); a++) {
                            PlsqlErrorObject errObj = (PlsqlErrorObject) errLst.get(a);
                            int stNo = exeObj.getStartLineNo();
                            int errNo = errObj.getLineNumber();
                            if (errNo <= 0) {
                                errNo = 1;
                            }
                            int lineNo = stNo + errNo - 1;
                            msg = errObj.getErrorMsg();
                            msg = getmodifiedErorrMsg(msg, lineNo);
                            outList = new PlsqlOutputListener();
                            outList.setDocLinesArray(exeObj.getDocLinesArray());
                            outList.setLineNo(lineNo);
                            outList.setOriginalFileName(exeObj.getOriginalFileName());
                            io.getErr().println(msg, outList);
                        }
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() == PlsqlExecutableObjectType.PACKAGE) {
                    try {
                        stm.execute(plsqlText);
                        io.getOut().println((new StringBuilder()).append("PL/SQL Package ").append(exeObjName).append(" Created Successfully").toString());
                    } catch (SQLException sqlEx) {
                        exception = true;
                        excMessage = sqlEx.getMessage();
                    }
                    errLst = getPackageerrors(exeObjName, "PACKAGE");
                    if ((errLst.isEmpty()) && (!exception)) {
                        continue;
                    } else if ((errLst.isEmpty()) && (exception)) {
                        io.getErr().println((new StringBuilder()).append("!!!Package ").append(exeObjName).append(" Created With Compilation Errors").toString());
                        int errLine = getLineNumberFromMsg(excMessage);
                        int outLine = lineNumber + errLine - 1;
                        msg = getmodifiedErorrMsg(excMessage, outLine);
                        outList.setLineNo(outLine);
                        io.getErr().println(msg, outList);
                        deploymentOk = false;
                        break;
                    } else {
                        io.getErr().println((new StringBuilder()).append("!!!Package ").append(exeObjName).append(" Created With Compilation Errors").toString());
                        for (int a = 0; a < errLst.size(); a++) {
                            PlsqlErrorObject errObj = (PlsqlErrorObject) errLst.get(a);
                            int stNo = exeObj.getStartLineNo();
                            int errNo = errObj.getLineNumber();
                            if (errNo <= 0) {
                                errNo = 1;
                            }
                            int lineNo = stNo + errNo - 1;
                            msg = errObj.getErrorMsg();
                            msg = getmodifiedErorrMsg(msg, lineNo);
                            outList = new PlsqlOutputListener();
                            outList.setDocLinesArray(exeObj.getDocLinesArray());
                            outList.setLineNo(lineNo);
                            outList.setOriginalFileName(exeObj.getOriginalFileName());
                            io.getErr().println(msg, outList);
                        }
                        deploymentOk = false;
                        break;
                    }
                }
                if (exeObj.getType() != PlsqlExecutableObjectType.PACKAGEBODY) {
                    continue;
                }
                try {
                    stm.execute(plsqlText);
                    io.getOut().println((new StringBuilder()).append("PL/SQL Package Body ").append(exeObjName).append(" Created Successfully").toString());
                } catch (SQLException sqlEx) {
                    exception = true;
                    excMessage = sqlEx.getMessage();
                }
                errLst = getPackageerrors(exeObjName, "PACKAGE BODY");
                if ((errLst.isEmpty()) && (!exception)) {
                    continue;
                } else if ((errLst.isEmpty()) && (exception)) {
                    io.getErr().println((new StringBuilder()).append("!!!Package Body ").append(exeObjName).append(" Created With Compilation Errors").toString());
                    int errLine = getLineNumberFromMsg(excMessage);
                    int outLine = lineNumber + errLine - 1;
                    msg = getmodifiedErorrMsg(excMessage, outLine);
                    outList.setLineNo(outLine);
                    io.getErr().println(msg, outList, true);
                    deploymentOk = false;
                    break;
                } else {
                    io.getErr().println((new StringBuilder()).append("!!!Package Body ").append(exeObjName).append(" Created With Compilation Errors").toString());
                    for (int a = 0; a < errLst.size(); a++) {
                        PlsqlErrorObject errObj = (PlsqlErrorObject) errLst.get(a);
                        int stNo = exeObj.getStartLineNo();
                        int errNo = errObj.getLineNumber();
                        if (errNo <= 0) {
                            errNo = 1;
                        }
                        int lineNo = stNo + errNo - 1;
                        msg = errObj.getErrorMsg();
                        msg = getmodifiedErorrMsg(msg, lineNo);
                        outList = new PlsqlOutputListener();
                        outList.setDocLinesArray(exeObj.getDocLinesArray());
                        outList.setLineNo(lineNo);
                        outList.setOriginalFileName(exeObj.getOriginalFileName());
                        io.getErr().println(msg, outList, true);
                        a++;
                    }
                    deploymentOk = false;
                    break;
                }
            }
            
            if (fileName.endsWith(".tdb") && !autoCommit) {
                if (!deploymentOk && !commit && !(firstWord != null
                        && (firstWord.equalsIgnoreCase("INSERT") || firstWord.equalsIgnoreCase("UPDATE") || firstWord.equalsIgnoreCase("DELETE")))) {
                    con.commit();
                }
            } else {
                con.commit();
            }
            
            if (!moreRowsToBeFetched) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                String totalTime = Long.toString(duration / 1000);
                if (duration < 10000) {
                    totalTime += "." + Long.toString((duration % 1000) / 100);
                }
                if (preparedIO == null) {
                    io.getOut().println("-------------------------------------------------------------");
                    io.getOut().println(endMsg + " (Total times: " + totalTime + "s)");
                    io.getOut().println(new Timestamp(endTime).toString());
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "CONNECTION CREATION PROBLEM");
            deploymentOk = false;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(null, "CONNECTION CREATION PROBLEM");
            deploymentOk = false;
        } finally {
            if (stm != null) {
                try {
                    stm.close();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
            if (io != null) {
                io.getOut().close();
                io.getErr().close();
                // IOPosition.currentPosition(io).scrollTo();
            }
        }
        return deploymentOk ? null : io;
    }
    
    public String getmodifiedErorrMsg(String msg, int lineNumber) {
        int index = msg.indexOf("\n");
        if (index >= 0) {
            msg = msg.replaceAll("\n", "");
        }
        msg = (new StringBuilder()).append(msg).append(" error at line no :").append(lineNumber).toString();
        return msg;
    }
    
    private List getPackageerrors(String packageName, String packageType) {
        List<PlsqlErrorObject> lst = new ArrayList<PlsqlErrorObject>();
        Connection con = debugConnection != null ? debugConnection : connection.getJDBCConnection();
        if (con != null) {
            Statement stm = null;
            try {
                stm = con.createStatement();
                String query = (new StringBuilder()).append("SELECT LINE, POSITION, TEXT FROM USER_ERRORS WHERE TYPE = '").append(packageType).append("' AND NAME = '").append(packageName).append("'").toString();
                PlsqlErrorObject errObj;
                for (ResultSet rs = stm.executeQuery(query); rs.next();) {
                    int lineNo = rs.getInt("LINE");
                    int pos = rs.getInt("POSITION");
                    String msg = rs.getString("TEXT");
                    errObj = new PlsqlErrorObject();
                    errObj.setLineNumber(lineNo);
                    errObj.setPosition(pos);
                    errObj.setErrorMsg(msg);
                    lst.add(errObj);
                }
            } catch (SQLException e) {
            } finally {
                try {
                    if (stm != null) {
                        stm.close();
                    }
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        } else {
            throw new RuntimeException("Not Connected to Database to Get USER_ERRORS");
        }
        return lst;
    }

    /**
     * Method that will return the error line number in the given SQL error message
     * @param message
     * @return
     */
    private int getLineNumberFromMsg(String message) {
        int indexLine = message.lastIndexOf(" line ");
        if (indexLine >= 0) {
            try {
                return Integer.parseInt(message.substring(indexLine + 6).trim());
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * Method that will parse the document and initialize the aliases
     * @param definesMap
     * @param doc
     * @param start
     * @param end
     * @param define
     * @param io
     * @return
     */
    private char getAliases(HashMap<String, String> definesMap, Document doc, int start, int end, char define, InputOutput io) {
        TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
        @SuppressWarnings("unchecked")
        TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());
        ts.move(start);
        boolean moveNext = ts.moveNext();
        
        Token<PlsqlTokenId> token = ts.token();

        //Get the difine by the name
        while (moveNext) {
            
            if (token.offset(tokenHierarchy) >= end) {
                break;         //Check whether this is DEFINE

            }
            //Check whether this is DEFINE
            PlsqlTokenId tokenId = token.id();
            if (tokenId == PlsqlTokenId.SQL_PLUS) {
                String tokenTxt = FileExecutionUtil.readLine(ts, token);
                if ((tokenTxt.toUpperCase(Locale.ENGLISH).startsWith("DEF "))
                        || (tokenTxt.toUpperCase(Locale.ENGLISH).startsWith("DEFI "))
                        || (tokenTxt.toUpperCase(Locale.ENGLISH).startsWith("DEFIN "))
                        || (tokenTxt.toUpperCase(Locale.ENGLISH).startsWith("DEFINE "))) {
                    if (!tokenTxt.contains(" = ") && tokenTxt.contains("=")) {
                        tokenTxt = tokenTxt.substring(0, tokenTxt.indexOf("=")) + " = " + tokenTxt.substring(tokenTxt.indexOf("=") + 1);
                    }
                    
                    StringTokenizer tokenizer = new StringTokenizer(tokenTxt);
                    tokenizer.nextToken();
                    String alias;
                    String value = "";
                    boolean isNext = tokenizer.hasMoreTokens();

                    //alias
                    if (isNext) {
                        alias = tokenizer.nextToken();
                    } else {
                        break;
                    }
                    
                    isNext = tokenizer.hasMoreTokens();
                    
                    if ((isNext) && (tokenizer.nextToken().equals("="))) {
                        boolean isComment = false;
                        while (tokenizer.hasMoreTokens() && !isComment) {
                            String temp = tokenizer.nextToken();
                            if (temp.startsWith("--") || temp.startsWith("/*")) {
                                isComment = true;
                            } else {
                                value = value + " " + temp;
                            }
                        }
                        
                        value = value.trim();
                        
                        if ((value.startsWith("\"") && value.endsWith("\""))
                                || (value.startsWith("\'") && value.endsWith("\'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        
                        if (value.indexOf(define) >= 0) {
                            value = replaceAliases(value, definesMap, define, io);
                        }
                        definesMap.put(alias.toUpperCase(Locale.ENGLISH), value);
                    }
                } else if (tokenTxt.toUpperCase(Locale.ENGLISH).startsWith("SET ")) {
                    StringTokenizer tokenizer = new StringTokenizer(tokenTxt);
                    tokenizer.nextToken();
                    String alias;
                    boolean isNext = tokenizer.hasMoreTokens();
                    tokenizer.nextToken();
                    isNext = tokenizer.hasMoreTokens();
                    //alias
                    if (isNext) {
                        alias = tokenizer.nextToken();
                    } else {
                        break;
                    }
                    
                    if (alias.length() == 1) {
                        define = alias.charAt(0); //If define changed we catch it here
                    }
                }
            }
            moveNext = ts.moveNext();
            token = ts.token();
        }
        
        return define;
    }

    /**
     * Replace aliases in the given string
     * @param plsqlString
     * @param definesMap
     * @param define
     * @param io
     * @return
     */
    public String replaceAliases(String plsqlString, HashMap<String, String> definesMap, char define, InputOutput io) {
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

                            String name = plsqlString.substring(i + 1, j);
                            String value = definesMap.get(name.toUpperCase(Locale.ENGLISH));
                            if (value == null || value.startsWith(Character.toString(define))) {
                                PromptDialog prompt = new PromptDialog(null, name, true);
                                prompt.setVisible(true);
                                value = prompt.getValue();
                                definesMap.put(name.toUpperCase(Locale.ENGLISH), value);
                                if (io != null) {
                                    io.getOut().println((new StringBuilder()).append("> Variable ").append(name).append(" = \"").append(value).append("\"").toString());
                                }
                            }
                            value = replaceAliases(value, definesMap, define, io);
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
    
    private void setExecutionResults(SQLExecutionResults executionResults) {
        this.executionResults = executionResults;
    }
    
    private void setResultsToEditors(final SQLExecutionResults results, final DataObject obj, final String label) {
        if (results != null) {
            final List<Component> components = new ArrayList<Component>();
            final List<String> toolTips = new ArrayList<String>();
            
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    
                    @Override
                    public void run() {
                        for (SQLExecutionResult result : results.getResults()) {
                            for (Component component : result.getDataView().createComponents()) {
                                if (label != null) {
                                    component.setName(label);
                                }
                                components.add(component);
                                toolTips.add("<html>" + result.getStatementInfo().getSQL().replaceAll("\n", "<br>"));
                            }
                        }
                        
                        if (plsqlEditor != null) {
                            plsqlEditor.setResults(components, toolTips);
                        }
                    }
                });
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            } catch (InvocationTargetException ex) {
                Exceptions.printStackTrace(ex);
            }
            
        }
    }
    
    public PlsqlEditor getPlsqlEditor(DataObject dataObj) {
        //Get all the data objects and save dataObjects that require auto Save
        TopComponent.Registry registry = TopComponent.getRegistry();
        Object[] topComponentSet = registry.getOpened().toArray();
        for (int i = 0; i < topComponentSet.length; i++) {
            TopComponent component = (TopComponent) topComponentSet[i];
            if (component instanceof PlsqlEditor) {
                PlsqlEditor editor = (PlsqlEditor) component;
                DataObject obj = editor.getLookup().lookup(DataObject.class);
                if (obj != null && dataObj != null && obj == dataObj) {
                    return editor;
                }
            }
        }
        
        return null;
    }
    
    private void closeExecutionResult() {
        if (executionResults != null) {
            executionResults = null;
        }
    }
    
    private final class SQLExecutor implements Runnable, Cancellable {
        
        private static final int DEFAULT_PAGE_SIZE = 100;
        private FileObject USERDIR = FileUtil.getConfigRoot();
        private final DatabaseConnection dbconn;
        private final String sqlStmt;
        private String label;
        private RequestProcessor.Task task;
        private DataObject parent;
        
        public SQLExecutor(DataObject dataObj, DatabaseConnection dbconn, String sqlStmt, String label) {
            this.dbconn = dbconn;
            this.sqlStmt = sqlStmt + ";";
            this.parent = dataObj;
            this.label = label;
        }
        
        public void setTask(RequestProcessor.Task task) {
            this.task = task;
        }
        
        @Override
        public void run() {
            assert task != null : "Should have called setTask()"; // NOI18N

            ProgressHandle handle = ProgressHandleFactory.createHandle("Executing Statements");
            handle.start();
            try {
                handle.switchToIndeterminate();
                //closeExecutionResult();
                SQLExecutionResults executionResults = execute(sqlStmt, 0, sqlStmt.length() - 1, dbconn);
                handleExecutionResults(executionResults);
            } finally {
                handle.finish();
            }
        }

        /**
         * Execute the sql
         * @param sql
         * @param i
         * @param i0
         * @param databaseConenction
         * @return
         */
        private SQLExecutionResults execute(String sqlScript, int startOffset, int endOffset,
                DatabaseConnection conn) {
            boolean cancelled = false;
            List<StatementInfo> statements = getStatements(sqlScript, startOffset, endOffset);
            List<SQLExecutionResult> results = new ArrayList<SQLExecutionResult>();
            String url = conn.getDatabaseURL();
            
            for (Iterator i = statements.iterator(); i.hasNext();) {
                cancelled = Thread.currentThread().isInterrupted();
                if (cancelled) {
                    break;
                }
                
                StatementInfo info = (StatementInfo) i.next();
                String sql = info.getSQL();
                
                SQLExecutionResult result = null;
                DataView view = DataView.create(conn, sql, DEFAULT_PAGE_SIZE);

                // Save SQL statements executed for the SQLHistoryManager
                SQLHistoryManager.getInstance().saveSQL(new SQLHistory(url, sql, new Date()));
                result = new SQLExecutionResult(info, view);
                results.add(result);
            }

            // Persist SQL executed
            SQLHistoryManager.getInstance().save(USERDIR);
            
            if (!cancelled) {
                return new SQLExecutionResults(results);
            } else {
                return null;
            }
        }
        
        private List<StatementInfo> getStatements(String script, int startOffset, int endOffset) {
            List<StatementInfo> allStatements = new ArrayList<StatementInfo>();
            if (script.endsWith(";")) {
                script = script.substring(0, script.length() - 1);
            }
            allStatements.add(new StatementInfo(script, startOffset, startOffset, startOffset, startOffset, endOffset, script.length()));
            return allStatements;
        }
        
        private void handleExecutionResults(SQLExecutionResults executionResults) {
            if (executionResults == null) {
                // execution cancelled
                return;
            }
            
            setExecutionResults(executionResults);
            
            if (executionResults.size() <= 0) {
                // no results, but successfull
                return;
            }
            
            if (executionResults.hasExceptions()) {
                // there was at least one exception
                displayErrors(executionResults);
            } else {
                setResultsToEditors(executionResults, parent, label);
            }
        }

        /**
         * Display the exceptions
         * @param results
         */
        private void displayErrors(SQLExecutionResults results) {
            if (results != null) {
                for (SQLExecutionResult result : results.getResults()) {
                    Collection<Throwable> collect = result.getExceptions();
                    for (Iterator it = collect.iterator(); it.hasNext();) {
                        Throwable excep = (Throwable) it.next();
                        NotifyDescriptor descriptor = new NotifyDescriptor.Message(excep.getLocalizedMessage(), NotifyDescriptor.ERROR_MESSAGE);
                        DialogDisplayer.getDefault().notifyLater(descriptor);
                    }
                }
            }
        }
        
        @Override
        public boolean cancel() {
            return task.cancel();
        }
    }

    /*
     * Get the suitable IO tab name, according to the content
     * of the file.
     */
    private String getIOTabName(List executableObjs, String fileName, String displayName) {
        PlsqlExecutableObject executionObject = (PlsqlExecutableObject) executableObjs.get(0);
        if (fileName.equals(displayName)) {
            if (executableObjs.size() > 0 && fileName.endsWith(".tdb")) {
                String str = executionObject.getPlsqlString().replaceAll("\n", " ");
                fileName = str.length() > 30 ? str.substring(0, 30) + "..." : str;
            }
        } else if (displayName != null) {
            fileName = displayName;
        }
        return fileName;
    }
}
