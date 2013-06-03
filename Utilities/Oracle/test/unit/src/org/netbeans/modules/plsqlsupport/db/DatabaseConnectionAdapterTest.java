/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsqlsupport.db;

import java.sql.SQLException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author chrlse
 */
public class DatabaseConnectionAdapterTest {

    /**
     * Test of getConnection method, of class DatabaseConnectionAdapter.
     */
    @Test
    public void testShouldNotFailGivenConnectionIsNull() throws SQLException {
        System.out.println("testShouldNotFailGivenConnectionIsNull");
        DatabaseConnectionAdapter adapter = new DatabaseConnectionAdapter(null);
        assertNull(adapter.getConnection());
        assertNull(adapter.getJDBCConnection());
        assertEquals("", adapter.getDisplayName());
        assertFalse(adapter.isConnected());
        assertFalse(adapter.commitTransactions());
        assertFalse(adapter.rollbackTransactions());
    }

    /**
     * Test of equals method, of class DatabaseConnectionAdapter.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
//        DatabaseConnection other = new DatabaseConnection
        DatabaseConnectionAdapter adapter = new DatabaseConnectionAdapter(null);
        assertTrue(adapter.equals(null));
        assertFalse(adapter.equals(new Object()));
    }
}