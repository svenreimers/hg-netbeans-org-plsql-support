/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.mockito.Mockito.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionAdapter;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionIO;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.openide.filesystems.FileObject;

/**
 *
 * @author chrlse
 */
public class DatabaseConnectionMediatorDefaultTest {

    FileObject fileObject = mock(FileObject.class);
    DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);
    DatabaseConnectionAdapter connection = mock(DatabaseConnectionAdapter.class);
    DatabaseConnectionIO io = mock(DatabaseConnectionIO.class);
    DatabaseTransaction transaction = mock(DatabaseTransaction.class);
    SelectDatabaseNotifier notifier = mock(SelectDatabaseNotifier.class);

    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test of getConnectionManager method, of class DatabaseConnectionMediatorDefault.
     */
    @Test
    public void testShouldReturnFalseWhenManagerIsNull() {
        System.out.println("testShouldReturnFalseWhenManagerIsNull");
        DatabaseConnectionMediatorDefault instance = new DatabaseConnectionMediatorDefault(null, null, connection, transaction, io, notifier);
        assertFalse(instance.hasProjectDatabaseConnections());
        assertFalse(instance.isDefaultDatabase(null));
    }

    /**
     * Test of getConnectionManager method, of class DatabaseConnectionMediatorDefault.
     */
    @Test
    public void testShouldReturnExceptionWhenConnectionIsNull() {
        System.out.println("testShouldReturnExceptionWhenConnectionIsNull");
        DatabaseConnectionMediatorDefault instance = new DatabaseConnectionMediatorDefault(fileObject, null, connection, transaction, io, notifier);
        when(connection.getConnection()).thenReturn(null);
        assertNull(instance.getExecutor());
    }
}