package org.netbeans.modules.plsql.execution.impl;

import java.io.IOException;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionIO;
import org.netbeans.modules.plsqlsupport.db.ui.SQLCommandWindow;
import org.openide.util.Exceptions;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

/**
 *
 * @author chrlse
 */
class DatabaseConnectionIODefault implements DatabaseConnectionIO {

    private String fileName;
    private String summery;
    private InputOutput io = null;

    DatabaseConnectionIODefault(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void setSummery(String summery) {
        this.summery = summery;
    }

    @Override
    public void initialize() {
        if (summery == null) {
            throw new IllegalArgumentException("summery not set");
        }
        try {
            io = IOProvider.getDefault().getIO(executionDisplayName(), false);
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

    @Override
    public void println(Object object) {
        if (!io.isClosed()) {
            io.getOut().println(object);
        }
    }

    @Override
    public InputOutput getIO() {
        return io;
    }

    @Override
    public String executionDisplayName() {
        if (fileName.endsWith(".tdb")) {
            return summery;
        }
        return fileName;
    }
}
