/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution.impl;

import org.junit.Test;
import static org.junit.Assert.*;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionIO;

/**
 *
 * @author chrlse
 */
public class DatabaseConnectionIODefaultTest {

    private static final String MOVIE_API_FILENAME = "Movie.api";
    private static final String COMMAND_FILE = "Command.tdb";
    private static final String SELECT_FROM_DUAL = "Select * from dual;";

    @Test
    public void testSetSummery() {
        System.out.println("setSummery");
        String summery = "";
        DatabaseConnectionIO instance = new DatabaseConnectionIODefault(MOVIE_API_FILENAME);
        try {
            instance.initialize();
            fail("should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
        }
        instance.setSummery(summery);
        instance.initialize();
    }

    @Test
    public void testDisplayNameShouldBeFileNameGivenNoTDBFileExtension() {
        System.out.println("testDisplayNameShouldBeFileNameGivenNoTDBFileExtension");
        DatabaseConnectionIO instance = new DatabaseConnectionIODefault(MOVIE_API_FILENAME);
        instance.setSummery(SELECT_FROM_DUAL);
        assertEquals(MOVIE_API_FILENAME, instance.executionDisplayName());
    }

    @Test
    public void testDisplayNameShouldBeSummeryGivenTDBFileExtension() {
        System.out.println("testDisplayNameShouldBeSummeryGivenTDBFileExtension");
        DatabaseConnectionIO instance = new DatabaseConnectionIODefault(COMMAND_FILE);
        instance.setSummery(SELECT_FROM_DUAL);
        assertEquals(SELECT_FROM_DUAL, instance.executionDisplayName());
    }
}