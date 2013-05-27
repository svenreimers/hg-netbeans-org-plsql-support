/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import java.io.IOException;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author chrlse
 */
class DatabaseConnectionIO {

    private InputOutput io = null;

    void initializeIO(String fileName, String displayName, PlsqlExecutableObject executableObject) {
        displayName = getIOTabName(executableObject, fileName, displayName);
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
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
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

    void println(Object object) {
        if (!io.isClosed()) {
            io.getOut().println(object);
        }
    }

    InputOutput getIO() {
        return io;
    }
}
