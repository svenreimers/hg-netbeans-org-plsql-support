package org.netbeans.modules.plsql.lexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static junit.framework.Assert.assertEquals;
import org.apache.commons.io.FileUtils;
import org.netbeans.junit.NbTestCase;

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

    public void processBlocks(String plsqlFileName, List<PlsqlBlock> plsqlBlocks) throws IOException {
        List<PlsqlBlockType> expectedBlockTypes = new ArrayList<PlsqlBlockType>();
        populateListOfPlsqlBlocks(plsqlBlocks, expectedBlockTypes);
        if (recordTestData) {
            final File expectedDir = new File(getExpectedDir().replace(File.separator + "build" + File.separator, File.separator));
            expectedDir.mkdirs();
            FileUtils.writeLines(new File(expectedDir, plsqlFileName + ".structure"), expectedBlockTypes);
        } else {
            File expectedFile = new File(getExpectedDir(), plsqlFileName + ".structure");
            final File actualFile = new File(getWorkDir(), plsqlFileName + ".structure");
            FileUtils.writeLines(actualFile, expectedBlockTypes);
            assertStructure(expectedFile, actualFile);
        }
    }

    private void populateListOfPlsqlBlocks(List<PlsqlBlock> plsqlBlocks, List<PlsqlBlockType> blockTypes) {

        for (PlsqlBlock plsqlBlock : plsqlBlocks) {
            blockTypes.add(plsqlBlock.getType());
            final List<PlsqlBlock> childBlocks = plsqlBlock.getChildBlocks();
            if (childBlocks != null && !childBlocks.isEmpty()) {
                populateListOfPlsqlBlocks(childBlocks, blockTypes);
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
