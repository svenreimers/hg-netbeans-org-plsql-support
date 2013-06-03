/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import static org.mockito.Mockito.mock;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionAdapter;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;

/**
 *
 * @author chrlse
 */
public class DatabaseConnectionMediatorDefaultTest {
    
    public DatabaseConnectionMediatorDefaultTest() {
    }
    DatabaseConnectionManager connectionManager = mock(DatabaseConnectionManager.class);
    DatabaseConnectionAdapter connection = mock(DatabaseConnectionAdapter.class);
    DatabaseConnectionIO io = mock(DatabaseConnectionIO.class);
    DatabaseTransaction transaction = mock(DatabaseTransaction.class);

    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test of getConnectionManager method, of class DatabaseConnectionMediatorDefault.
     */
    @Test
    public void testShouldReturnFalseWhenManagerIsNull() {
        System.out.println("testShouldReturnFalseWhenManagerIsNull");
        DatabaseConnectionMediatorDefault instance = new DatabaseConnectionMediatorDefault(null, null, connection, transaction, io);
        assertFalse(instance.hasProjectDatabaseConnections());
        assertFalse(instance.isDefaultDatabase(null));
    }
}