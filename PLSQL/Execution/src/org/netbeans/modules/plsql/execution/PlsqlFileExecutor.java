/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Date;
import java.util.*;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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
import org.netbeans.modules.db.sql.history.SQLHistoryEntry;
import org.netbeans.modules.db.sql.history.SQLHistoryManager;
import org.netbeans.modules.plsql.filetype.PlsqlEditor;
import org.netbeans.modules.plsql.filetype.StatementExecutionHistory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.datatransfer.ExClipboard;
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
    private final String connectionDisplayName;

    public PlsqlFileExecutor(DatabaseConnectionManager connectionProvider, DatabaseConnection connection) {
        this.connectionDisplayName = "Using DB: " + connection.getDisplayName() + " [" + connection.getName() + "]";
        this.connection = connection;
        this.preparedIO = null;
        this.connectionProvider = connectionProvider;
        this.cache = DatabaseContentManager.getInstance(connection);
    }

    public PlsqlFileExecutor(DatabaseConnectionManager connectionProvider, Connection debugConnection, InputOutput io) {
        this.connection = null;
        this.connectionDisplayName = null;
        this.debugConnection = debugConnection;
        this.preparedIO = io;
        this.connectionProvider = connectionProvider;
//        this.cache = DatabaseContentManager.getInstance(connection);
    }

    public void cancel() {
        cancel = true;
    }

    /**
     * Method that will execute Dbmb_Output.Enable();
     *
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

        String querryString = nonCommentQuery.toString().trim();
        if (querryString.endsWith(";")) {
            querryString = querryString.substring(0, querryString.lastIndexOf(";"));
        }

        String newQuery = "";
        String token;
        boolean format = false;

        StringTokenizer tokenizer = new StringTokenizer(querryString, " \t\n");
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
                io.getOut().flush();
                io.getErr().flush();
            }
            io.select();
            io.getOut().println(connectionDisplayName);
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

    public InputOutput executePLSQL(List<PlsqlExecutableObject> executableObjs, Document doc, boolean hidden, boolean autoCommit) {
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
        List<Character> define = Arrays.asList('&', ':');
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
        Connection con;
        Statement stm = null;
        String firstWord = null;
        PlsqlCommit commit = PlsqlCommit.getInstance((DataObject) object);

        //quick & dirty fix to avoid having output tabs for the SQL Execution window (unless there's an exception)
        //first check to see if this is a simple select statement and if so treat it separately.
        if (executableObjs.size() == 1) {
            PlsqlExecutableObject executionObject = executableObjs.get(0);
            if (executionObject.getType() == PlsqlExecutableObjectType.STATEMENT || executionObject.getType() == PlsqlExecutableObjectType.UNKNOWN) {
                String plsqlText = executionObject.getPlsqlString();
                try {
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
                        executeSelect(plsqlText, connection, doc, null);
                        return null;
                    } else if (firstWord.equalsIgnoreCase("DESC") || firstWord.equalsIgnoreCase("DESCRIBE")) {
                        describeObject(connection, doc, tokenizer, io);
                        return null;
                    }

                } catch (SQLException sqlEx) {
                    try {
                        io = initializeIO(fileName, getIOTabName(executableObjs.get(0), fileName, dataObj.getNodeDelegate().getDisplayName()), dataObj);
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


            io = initializeIO(fileName, getIOTabName(executableObjs.get(0), fileName, dataObj.getNodeDelegate().getDisplayName()), dataObj);
            con = debugConnection != null ? debugConnection : connection.getJDBCConnection();
            con.setAutoCommit(false);
            enableDbmsOut(con);
            firstSavepoint = con.setSavepoint();
            //savepointsCreated.put(new Integer(firstSavepoint.getSavepointId()),firstSavepoint);

            stm = con.createStatement();
            stm.setEscapeProcessing(false);
            boolean firstSelectStatement = true;

            for (PlsqlExecutableObject exeObj : executableObjs) {
                if (cancel) {
                    io.getErr().println("!!!Execution cancelled. Performing rollback");
                    // con.rollback();
                    con.rollback(firstSavepoint);
                    return io;
                }
                int lineNumber = exeObj.getStartLineNo();
                String plsqlText = exeObj.getPlsqlString();

                String exeObjName = exeObj.getExecutableObjName().toUpperCase(Locale.ENGLISH);
                if ((exeObj.getType() != PlsqlExecutableObjectType.UNKNOWN) && (exeObj.getType() != PlsqlExecutableObjectType.COMMENT)) {
                    //replace aliases since there are aliases in PROMPTS
                    if (!ignoreDefines) {
                        if (exeObj.getType() == PlsqlExecutableObjectType.TRIGGER) {
                            define = Arrays.asList('&');
                        }
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
                        StringTokenizer tokenizer = new StringTokenizer(plsqlText, " \t\n;");
                        if (tokenizer.hasMoreTokens()) {
                            firstWord = tokenizer.nextToken();
                        } else {
                            firstWord = plsqlText;
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
                        if (firstWord.startsWith("@") || firstWord.startsWith("@@") || firstWord.equalsIgnoreCase("START")) {
                            executeSqlPlusStart(plsqlText, firstWord, doc, io);
                            continue;
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
                                                define = Arrays.asList(token.charAt(0));
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
                List<PlsqlErrorObject> errLst;
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
                            PlsqlErrorObject errObj = errLst.get(a);
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
                            PlsqlErrorObject errObj = errLst.get(a);
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
                            PlsqlErrorObject errObj = errLst.get(a);
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
                        PlsqlErrorObject errObj = errLst.get(a);
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
                if (!deploymentOk && !(firstWord != null
                        && (firstWord.equalsIgnoreCase("INSERT") || firstWord.equalsIgnoreCase("UPDATE") || firstWord.equalsIgnoreCase("DELETE")))) {
                    con.commit();
                } else {
                    if (deploymentOk && (firstWord != null
                            && (firstWord.equalsIgnoreCase("INSERT") || firstWord.equalsIgnoreCase("UPDATE") || firstWord.equalsIgnoreCase("DELETE")))) {
                        commit.setCommit(true);
                    }
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
                    io.getOut().println(connectionDisplayName);
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

    private List<PlsqlErrorObject> getPackageerrors(String packageName, String packageType) {
        List<PlsqlErrorObject> errorObjects = new ArrayList<PlsqlErrorObject>();
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
                    errorObjects.add(errObj);
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
        return errorObjects;
    }

    /**
     * Method that will return the error line number in the given SQL error message
     *
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
     *
     * @param definesMap
     * @param doc
     * @param start
     * @param end
     * @param define
     * @param io
     * @return
     */
    private List<Character> getAliases(HashMap<String, String> definesMap, Document doc, int start, int end, List<Character> define, InputOutput io) {
        TokenHierarchy<Document> tokenHierarchy = TokenHierarchy.get(doc);
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

                        Iterator<Character> itre = define.iterator();
                        while (itre.hasNext()) {
                            if (value.indexOf(itre.next()) >= 0) {
                                value = replaceAliases(value, definesMap, define, io);
                                break;
                            }
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
                }
            }
            moveNext = ts.moveNext();
            token = ts.token();
        }

        return define;
    }

    /**
     * Replace aliases in the given string
     *
     * @param plsqlString
     * @param definesMap
     * @param define
     * @param io
     * @return
     */
    public String replaceAliases(String plsqlString, HashMap<String, String> definesMap, Collection<Character> define, InputOutput io) {
        boolean exists = false;
        Iterator<Character> iter = define.iterator();
        while (iter.hasNext()) {
            if (plsqlString.indexOf(iter.next()) >= 0) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            return plsqlString;
        }

        boolean insideString = false;
        boolean insideComment = false;

        StringBuilder newString = new StringBuilder();
        for (int i = 0; i < plsqlString.length(); i++) {
            char c = plsqlString.charAt(i);
            char defineValue = 0;

            //check for - inside strings
            if (!insideString && i + 1 < plsqlString.length() && !Character.toString(plsqlString.charAt(i + 1)).equals("'")) {
                if (!insideString && !Character.toString(plsqlString.charAt(i + 1)).equals("'")) {
                    insideString = !insideString;
                }
            }

            //Check for - Comments           
            if (!insideComment && Character.toString(c).equals("-") && i + 1 < plsqlString.length() && Character.toString(plsqlString.charAt(i + 1)).equals("-")) {
                insideComment = true;
            } else if (insideComment && Character.toString(c).equals("\n")) {
                insideComment = false;
            }

            iter = define.iterator();
            while (iter.hasNext()) {
                if (c == iter.next()) {
                    defineValue = c;
                    break;
                }
            }
            if (defineValue != 0 && !insideComment) {
                if (insideString && !Character.toString(defineValue).equals("&")) {
                    newString.append(c);
                } else {
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
                                if (value == null || value.startsWith(Character.toString(defineValue))) {
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
                                if ("IFSAPP".equals(connection.getSchema())) {
                                    fixDataTablePopupMenu(result.getDataView());
                                }
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

    private void executeSqlPlusStart(String plsqlText, String firstWord, Document doc, InputOutput io) {
        try {
            String fileName = null;
            if (firstWord.equalsIgnoreCase("START")) {
                fileName = plsqlText.substring(5).trim();
            } else if (firstWord.startsWith("@@")) {
                Object object = doc.getProperty(Document.StreamDescriptionProperty);
                if (object instanceof DataObject) {
                    FileObject fo = ((DataObject) object).getPrimaryFile();
                    fileName = fo.getPath().substring(0, fo.getPath().lastIndexOf("/")) + "/" + plsqlText.substring(2);
                }
            } else {
                fileName = plsqlText.substring(1).trim();
            }
            File file = new File(fileName);
            if (file.exists()) {

                DataObject obj = DataObject.find(FileUtil.toFileObject(file));

                if (obj != null) {
                    //Load the editor cookier and allow parsing
                    Document document = PlsqlFileUtil.getDocument(obj);
                    PlsqlExecutableBlocksMaker blockMaker = new PlsqlExecutableBlocksMaker(document);
                    executePLSQL(blockMaker.makeExceutableObjects(), document, true, true);
                }
            } else {
                io.getOut().println("!!!Error opening " + fileName);
            }
        } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    private final class SQLExecutor implements Runnable, Cancellable {

        private static final int DEFAULT_PAGE_SIZE = 100;
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
         *
         * @param sql
         * @param i
         * @param i0
         * @param databaseConenction
         * @return
         */
        private SQLExecutionResults execute(String sqlScript, int startOffset, int endOffset, DatabaseConnection conn) {
            boolean cancelled = false;
            List<StatementInfo> statements = getStatements(sqlScript, startOffset, endOffset);
            List<SQLExecutionResult> results = new ArrayList<SQLExecutionResult>();
            String url = conn.getDatabaseURL();
            for (StatementInfo info : statements) {
                cancelled = Thread.currentThread().isInterrupted();
                if (cancelled) {
                    break;
                }

                String sql = info.getSQL();

                DataView view = DataView.create(conn, sql, DEFAULT_PAGE_SIZE);

                // Save SQL statements executed for the SQLHistoryManager
                SQLHistoryManager.getInstance().saveSQL(new SQLHistoryEntry(url, sql, new Date()));
                results.add(new SQLExecutionResult(info, view));
            }

            // Persist SQL executed
            SQLHistoryManager.getInstance().save();

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
         *
         * @param results
         */
        private void displayErrors(SQLExecutionResults results) {
            if (results != null) {
                for (SQLExecutionResult result : results.getResults()) {
                    for (Throwable excep : result.getExceptions()) {
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
    private String getIOTabName(PlsqlExecutableObject executionObject, String fileName, String displayName) {
        if (fileName.equals(displayName)) {
            if (executionObject != null && fileName.endsWith(".tdb")) {
                String str = executionObject.getPlsqlString().replaceAll("\n", " ");
                fileName = str.length() > 30 ? str.substring(0, 30) + "..." : str;
            }
        } else if (displayName != null) {
            fileName = displayName;
        }
        return fileName;
    }

    private void fixDataTablePopupMenu(DataView view) {
        try {
            // Find the popup menu
            Field field = view.getClass().getDeclaredField("delegate");
            field.setAccessible(true);
            final org.netbeans.modules.db.dataview.output.DataView delegate = (org.netbeans.modules.db.dataview.output.DataView) field.get(view);
            field = delegate.getClass().getDeclaredField("dataViewUI");
            field.setAccessible(true);
            final Object dataViewUI = field.get(delegate);
            field = dataViewUI.getClass().getDeclaredField("dataPanel");
            field.setAccessible(true);
            final Object dataPanel = field.get(dataViewUI);
            field = dataPanel.getClass().getDeclaredField("tableUI");
            field.setAccessible(true);
            final Object tableUI = field.get(dataPanel);
            field = tableUI.getClass().getDeclaredField("tablePopupMenu");
            field.setAccessible(true);
            JPopupMenu menu = (JPopupMenu) field.get(tableUI);
            // Change the "Truncate Table" icon to do nothing
            field = dataViewUI.getClass().getDeclaredField("truncateButton");
            field.setAccessible(true);
            final JButton truncateButton = (JButton) field.get(dataViewUI);
            for (ActionListener listener : truncateButton.getActionListeners()) {
                truncateButton.removeActionListener(listener);
            }
            truncateButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final NotifyDescriptor d = new NotifyDescriptor.Message("Truncate table is disabled for IFS Applications databases (schema IFSAPP).",
                            NotifyDescriptor.INFORMATION_MESSAGE);
                    DialogDisplayer.getDefault().notify(d);
                }
            });
            // Change the "Truncate Table" menu item to "Copy insert block ..."
            for (Component menuItem : menu.getComponents()) {
                if (menuItem instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem) menuItem;
                    if (item.getText().startsWith("Truncate")) {
                        item.setText("Copy insert block to clipboard");
                        for (ActionListener listener : item.getActionListeners()) {
                            item.removeActionListener(listener);
                        }
                        item.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                try {
                                    //Get table name
                                    Method method = delegate.getClass().getMethod("getDataViewDBTable");
                                    method.setAccessible(true);
                                    Object dataViewTable = method.invoke(delegate);
                                    method = dataViewTable.getClass().getMethod("geTable", Integer.TYPE);
                                    method.setAccessible(true);
                                    Object table = method.invoke(dataViewTable, 0);
                                    method = table.getClass().getMethod("getName");
                                    method.setAccessible(true);
                                    String tableName = (String) method.invoke(table);
                                    //Get column names and initial NULL value
                                    method = dataViewTable.getClass().getMethod("getColumnCount");
                                    method.setAccessible(true);
                                    int columnCount = (Integer) method.invoke(dataViewTable);
                                    method = dataViewTable.getClass().getMethod("getColumnName", Integer.TYPE);
                                    method.setAccessible(true);
                                    int maxColumnNameSize = 0;
                                    for (int i = 0; i < columnCount; i++) {
                                        String columnName = ((String) method.invoke(dataViewTable, i)).toLowerCase(Locale.ENGLISH);
                                        maxColumnNameSize = Math.max(maxColumnNameSize, columnName.length());
                                    }
                                    if (maxColumnNameSize > 30) {
                                        maxColumnNameSize = 30;
                                    }
                                    List<String> columnNames = new ArrayList<String>();
                                    Map<String, String> columnValues = new TreeMap<String, String>();
                                    for (int i = 0; i < columnCount; i++) {
                                        String columnName = ((String) method.invoke(dataViewTable, i)).toLowerCase(Locale.ENGLISH);
                                        if (maxColumnNameSize - columnName.length() > 0) {
                                            columnName = columnName + "                             ".substring(0, maxColumnNameSize - columnName.length());
                                        }
                                        columnNames.add(columnName);
                                        columnValues.put(columnName, "NULL");
                                    }
                                    //Format declare section
                                    StringBuilder insertSQL = new StringBuilder();
                                    insertSQL.append("DECLARE\n");
                                    insertSQL.append("   rec_ ").append(tableName.toLowerCase()).append("%ROWTYPE;\n");
                                    insertSQL.append("BEGIN\n");
                                    //Get all rows
                                    method = delegate.getClass().getDeclaredMethod("getDataViewPageContext");
                                    method.setAccessible(true);
                                    Object pageContext = method.invoke(delegate);
                                    method = pageContext.getClass().getDeclaredMethod("getCurrentRows");
                                    method.setAccessible(true);
                                    @SuppressWarnings("unchecked")
                                    List<Object[]> currentRows = (List<Object[]>) method.invoke(pageContext);
                                    //Get selected row numbers
                                    method = tableUI.getClass().getMethod("getSelectedRows");
                                    method.setAccessible(true);
                                    int[] rows = (int[]) method.invoke(tableUI);
                                    for (int j = 0; j < rows.length; j++) {
                                        Object[] insertRow = currentRows.get(rows[j]);
                                        for (int i = 0; i < insertRow.length; i++) {
                                            Object object = insertRow[i];
                                            String columnName = columnNames.get(i);
                                            String value;
                                            if (object == null) {
                                                value = "NULL";
                                            } else if (object instanceof BigDecimal) {
                                                value = object.toString();
                                            } else if (object instanceof Timestamp) {
                                                String temp = object.toString();
                                                if (temp.endsWith("00:00:00.0")) {
                                                    value = "to_date('" + temp.substring(0, 10) + "', 'YYYY-MM-DD')";
                                                } else {
                                                    value = "to_date('" + temp.substring(0, 19) + "', 'YYYY-MM-DD HH24:MI:SS')";
                                                }
                                            } else {
                                                value = "'" + object.toString().replace("'", "''") + "'";
                                            }
                                            String oldValue = columnValues.put(columnName, value);
                                            if ((oldValue == null && value != null) || (oldValue != null && !oldValue.equals(value))) {
                                                insertSQL.append("   rec_.").append(columnName).append(" := ");
                                                insertSQL.append(value).append(";\n");
                                            }
                                        }
                                        insertSQL.append("   INSERT INTO ").append(tableName.toLowerCase()).append(" VALUES rec_;\n");
                                    }
                                    //Format end of statement
                                    insertSQL.append("END;\n");
                                    //Copy to clipboard
                                    Clipboard clipboard = Lookup.getDefault().lookup(ExClipboard.class);
                                    if (clipboard != null) {
                                        clipboard.setContents(new StringSelection(insertSQL.toString()), null);
                                    }
                                } catch (IllegalAccessException ex) {
                                    Exceptions.printStackTrace(ex);
                                } catch (IllegalArgumentException ex) {
                                    Exceptions.printStackTrace(ex);
                                } catch (InvocationTargetException ex) {
                                    Exceptions.printStackTrace(ex);
                                } catch (NoSuchMethodException ex) {
                                    Exceptions.printStackTrace(ex);
                                } catch (SecurityException ex) {
                                    Exceptions.printStackTrace(ex);
                                }
                            }
                        });
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IllegalAccessException ex) {
            Exceptions.printStackTrace(ex);
        } catch (NoSuchFieldException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SecurityException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
