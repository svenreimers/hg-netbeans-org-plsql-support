package org.netbeans.modules.plsql.execution;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static junit.framework.Assert.assertEquals;
import org.apache.commons.io.FileUtils;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObjectType;

/**
 * This class contains both the framework for template testing and the test settings.
 *
 * RUN_LAYER_TEST map states if tests for that particular layer should be run or not. true = all tests for this layer
 * are executed, both for test data collection and for test output verification false = the layer is ignored completely,
 * both for testing and data collection
 *
 * RecordTestData is a switch for testing mode true = record test data and write expected data files to their correct
 * folders. those files should then be committed to the SVN repository NOTE: this flag should only be set when
 * collecting data and the be reset again. Never commit this class to SVN with this flag set to true !!! false = run the
 * test as normal
 */
public class TestRecordPlayer extends NbTestCase {

    private boolean recordTestData = false; //NOTE: Must be false when committed  !!!

    public TestRecordPlayer(String name) {
        super(name);
    }

    public void processExecutableBLocksBlocks(String plsqlFileName, List<PlsqlExecutableObject> exceutableObjects) throws IOException {
        List<String> expectedExceutableObjTypes = new ArrayList<String>();
        populateListOfExceutableObjects(exceutableObjects, expectedExceutableObjTypes);
        if (recordTestData) {
            final File expectedDir = new File(getExpectedDir().replace(File.separator + "build" + File.separator, File.separator));
            expectedDir.mkdirs();
            FileUtils.writeLines(new File(expectedDir, plsqlFileName + ".structure"), expectedExceutableObjTypes);
        } else {
            File expectedFile = new File(getExpectedDir(), plsqlFileName + ".structure");
            final File actualFile = new File(getWorkDir(), plsqlFileName + ".structure");
            FileUtils.writeLines(actualFile, expectedExceutableObjTypes);
            assertStructure(expectedFile, actualFile);
        }
    }

    private void populateListOfExceutableObjects(List<PlsqlExecutableObject> exceutableObjects, List<String> expectedExceutableObjTypes) {

        for (PlsqlExecutableObject exceutableObject : exceutableObjects) {
            PlsqlExecutableObjectType type = exceutableObject.getType();
            if(type == PlsqlExecutableObjectType.BEGINEND) {
                expectedExceutableObjTypes.add("BEGINEND");
            }
            if(type == PlsqlExecutableObjectType.COLUMNCOMMENT) {
                expectedExceutableObjTypes.add("COLUMNCOMMENT");
            }
            if(type == PlsqlExecutableObjectType.COMMENT) {
                expectedExceutableObjTypes.add("COMMENT");
            }
            if(type == PlsqlExecutableObjectType.DECLAREEND) {
                expectedExceutableObjTypes.add("DECLAREEND");
            }
            if(type == PlsqlExecutableObjectType.FUNCTION) {
                expectedExceutableObjTypes.add("FUNCTION");
            }
            if(type == PlsqlExecutableObjectType.JAVASOURCE) {
                expectedExceutableObjTypes.add("JAVASOURCE");
            }
            if(type == PlsqlExecutableObjectType.PACKAGE) {
                expectedExceutableObjTypes.add("PACKAGE");
            }
            if(type == PlsqlExecutableObjectType.PACKAGEBODY) {
                expectedExceutableObjTypes.add("PACKAGEBODY");
            }
            if(type == PlsqlExecutableObjectType.PROCEDURE) {
                expectedExceutableObjTypes.add("PROCEDURE");
            }
            if(type == PlsqlExecutableObjectType.STATEMENT) {
                expectedExceutableObjTypes.add("STATEMENT");
            }
            if(type == PlsqlExecutableObjectType.TABLECOMMENT) {
                expectedExceutableObjTypes.add("TABLECOMMENT");
            }
            if(type == PlsqlExecutableObjectType.TRIGGER) {
                expectedExceutableObjTypes.add("TRIGGER");
            }
            if(type == PlsqlExecutableObjectType.UNKNOWN) {
                expectedExceutableObjTypes.add("UNKNOWN");
            }
            if(type == PlsqlExecutableObjectType.VIEW) {
                expectedExceutableObjTypes.add("VIEW");
            }
        }
        
    }

    public void assertStructure(File expectedFile, File actualFile) throws IOException {
        assertLines(FileUtils.readFileToString(expectedFile, "utf-8"), FileUtils.readFileToString(actualFile, "utf-8"));
    }

    protected String getExpectedDir() {
        return getDataDir().getAbsolutePath() + File.separator + "expected" + File.separator;
    }

    public static void assertLines(String expResult, String result) {
        String[] expectedLine = expResult.replace("\r", "").split("[\\r\\n]");
        String[] resultLine = result.replace("\r", "").split("[\\r\\n]");
        for (int i = 0; i < resultLine.length && i < expectedLine.length; i++) {
            try {
                assertEquals(expectedLine[i].replaceAll("\\s+", " ").trim(), resultLine[i].replaceAll("\\s+", " ").trim());
            } catch (AssertionError e) {
                System.out.println("failed on line " + i);
                System.out.println("EXPECTED==========================================================");
                for (int j = -5; j < 8; j++) {
                    if (i < expectedLine.length - j && i + j >= 0) {
                        System.out.println((j == 0 ? "--> " : "    ") + expectedLine[i + j]);
                    }
                }
                System.out.println("RESULT============================================================");
                for (int j = -5; j < 8; j++) {
                    if (i < resultLine.length - j && i + j >= 0) {
                        System.out.println((j == 0 ? "--> " : "    ") + resultLine[i + j]);
                    }
                }
                System.out.println("==================================================================");
                throw e;
            }
        }
        assertEquals(expectedLine.length, resultLine.length);
    }
}
