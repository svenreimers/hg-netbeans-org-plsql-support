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
package org.netbeans.modules.plsql.lexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.Task;

/**
 *
 * @author YADHLK
 */
public class PlsqlBlockFactoryTest extends TestRecordPlayer {

    private static FileSystem fs = null;

    @Before
    @Override
    public void setUp() {
        fs = FileUtil.createMemoryFileSystem();
        assertNotNull(fs);
    }

    public PlsqlBlockFactoryTest(String name) {
        super(name);
    }

    @Test
    public void testBlocksApy() throws IOException, BadLocationException {
        System.out.println("Testing blocks of an APY file");
        final String plsqlFileName = "test.apy";

        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> plsqlBlocks = blockFac.getBlockHierarchy();
            //printHierarchy(lstBlockFac, "");
            //generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(7, plsqlBlocks.size());
            processBlocks(plsqlFileName, plsqlBlocks);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testBlocksApi() throws IOException, BadLocationException {
        System.out.println("Testing blocks of an API file");
        final String plsqlFileName = "test.api";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
            //printHierarchy(lstBlockFac, "");
            assertEquals(3, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testBlocksUpg() throws IOException, BadLocationException {
        System.out.println("Testing blocks of an UPG file");
        final String plsqlFileName = "test.upg";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
            //printHierarchy(lstBlockFac, "");
            assertEquals(4, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced1() throws IOException, BadLocationException {
        System.out.println("Advanced test case 1");
        final String plsqlFileName = "test1.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
            //printHierarchy(lstBlockFac, "");
            //generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(17, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);

        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced2() throws IOException, BadLocationException {
        System.out.println("Advanced test case 2");
        final String plsqlFileName = "test2.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
            //printHierarchy(lstBlockFac, "");
            //generateAssert(lstBlockFac, "lstBlockFac", "false");
            processBlocks(plsqlFileName, lstBlockFac);

        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced3() throws IOException, BadLocationException {
        System.out.println("Advanced test case 3");
        final String plsqlFileName = "080617_75132_fndbas.cdb";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(lstBlockFac.size(), 5);
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced4() throws IOException, BadLocationException {
        System.out.println("Advanced test case 4");
        final String plsqlFileName = "081114_78488_VMOSFA.cdb";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(lstBlockFac.size(), 3);
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced5() throws IOException, BadLocationException {
        System.out.println("Advanced test case 5");
        final String plsqlFileName = "Dictionary.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(lstBlockFac.size(), 4);
            processBlocks(plsqlFileName, lstBlockFac);

        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced6() throws IOException, BadLocationException {
        System.out.println("Advanced test case 6");
        final String plsqlFileName = "FavoriteSchedules.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(lstBlockFac.size(), 8);
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced7() throws IOException, BadLocationException {
        System.out.println("Advanced test case 7");
        final String plsqlFileName = "FndEvent.api";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(lstBlockFac.size(), 3);
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced8() throws IOException, BadLocationException {
        System.out.println("Advanced test case 8");
        final String plsqlFileName = "FndEvent.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(lstBlockFac.size(), 8);
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced9() throws IOException, BadLocationException {
        System.out.println("Advanced test case 9");
        final String plsqlFileName = "IdentityPayInfo.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(54, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced10() throws IOException, BadLocationException {
        System.out.println("Advanced test case 10");
        final String plsqlFileName = "InstallationSite.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(11, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced11() throws IOException, BadLocationException {
        System.out.println("Advanced test case 11");
        final String plsqlFileName = "OpPersDiaryCalculation.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(4, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced12() throws IOException, BadLocationException {
        System.out.println("Advanced test case 12");
        final String plsqlFileName = "ScorecardInputValue.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(24, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced13() throws IOException, BadLocationException {
        System.out.println("Advanced test case 13");
        final String plsqlFileName = "Trainer.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(11, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced14() throws IOException, BadLocationException {
        System.out.println("Advanced test case 14");
        final String plsqlFileName = "fndbas_ora.cre";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(13, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced15() throws IOException, BadLocationException {
        System.out.println("Advanced test case 15");
        final String plsqlFileName = "person.ins";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced16() throws IOException, BadLocationException {
        System.out.println("Advanced test case 16");
        final String plsqlFileName = "sys_IalObjectSlave.api";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(3, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced17() throws IOException, BadLocationException {
        System.out.println("Advanced test case 17");
        final String plsqlFileName = "sys_IalObjectSlave.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(4, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    @Test
    public void testAdvanced18() throws IOException, BadLocationException {
        System.out.println("Advanced test case 18");
        final String plsqlFileName = "txtser.ins";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
//         generateAssert(lstBlockFac, "lstBlockFac", "false");
            assertEquals(4, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }
    
    @Test
    public void testSimpleCaseStatement() throws IOException, BadLocationException {
        System.out.println("testSimpleCaseStatement");
        final String plsqlFileName = "case.upg";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
//         printHierarchy(lstBlockFac, "");
 //          generateAssert(lstBlockFac, "lstBlockFac", "false");
 //          assertEquals(2, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }
    
    @Test
    public void testForComments() throws IOException, BadLocationException {
        System.out.println("Test for Comment blocks ");
        final String plsqlFileName = "TestComments.apy";
        FileObject fileObject = fs.getRoot().createData(plsqlFileName);
        assertNotNull(fileObject);
        try {
            PlsqlBlockFactory blockFac = loadAsTmpFile(fileObject, plsqlFileName);
            assertNotNull(blockFac);

            List<PlsqlBlock> lstBlockFac = blockFac.getBlockHierarchy();
            //printHierarchy(lstBlockFac, "");
            //assertEquals(2, lstBlockFac.size());
            processBlocks(plsqlFileName, lstBlockFac);

        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }
    
   
    private PlsqlBlockFactory loadAsTmpFile(FileObject fileObj, String fileName) throws IOException, BadLocationException {
        InputStream inputStream = new FileInputStream(new File(getDataDir(), fileName));
        InputStreamReader indexReader = new InputStreamReader(inputStream);
        BufferedReader input = new BufferedReader(indexReader);
        String content = "";
        while (input.ready()) {
            content = content + input.readLine() + "\n";
        }
        input.close();
        inputStream.close();
        indexReader.close();
        writeFile(content, fileObj);

        DataObject dataObj = DataFolder.find(fileObj);
        EditorCookie ec = dataObj.getLookup().lookup(EditorCookie.class);
        assertNotNull(ec);
        Task task = ec.prepareDocument();
        task.waitFinished();
        PlsqlBlockFactory blockFac = dataObj.getLookup().lookup(PlsqlBlockFactory.class);
        assertNotNull(blockFac);
        Document doc = ec.getDocument();
        assertNotNull(doc);
        doc.putProperty(Language.class, PlsqlTokenId.language());

        TokenHierarchy tokenHier = TokenHierarchy.get(doc);
        assertNotNull(tokenHier);
        TokenSequence<PlsqlTokenId> ts = tokenHier.tokenSequence(PlsqlTokenId.language());
        assertNotNull(ts);
        blockFac.initHierarchy(doc);

        return blockFac;
    }

    private void writeFile(String content, FileObject file) throws IOException {
        OutputStream os = file.getOutputStream();
        os.write(content.getBytes("UTF-8"));
        os.close();
    }

    private void printHierarchy(List<PlsqlBlock> lstBlockFac, String txt) {
        for (PlsqlBlock block : lstBlockFac) {
            System.out.println(txt + "Block Name:" + block.getName() + " Type:" + block.getType() + " Start:" + block.getStartOffset() + " End:" + block.getEndOffset() + " Children:" + block.getChildBlocks().size());
            printHierarchy(block.getChildBlocks(), txt + "\t");
        }
    }

    private void assertBlock(List<PlsqlBlock> lstBlock, int index, PlsqlBlockType type, String name, boolean parentExisting, int childCount) {
        PlsqlBlock block = lstBlock.get(index);
        assertNotNull(block);
        assertEquals(type, block.getType());
        assertEquals(name, block.getName());
        assertEquals(childCount, block.getChildCount());

        if (parentExisting) {
            assertNotNull(block.getParent());
        } else {
            assertNull(block.getParent());
        }
    }

    /**
     * Method that generates the assert block statements
     *
     * @param lstBlockFac
     * @param txt
     * @param isParent
     */
    private void generateAssert(List<PlsqlBlock> lstBlockFac, String txt, String isParent) {
        for (int i = 0; i < lstBlockFac.size(); i++) {
            PlsqlBlock block = lstBlockFac.get(i);
            System.out.println("assertBlock(" + txt + ", " + i + ", PlsqlBlockType." + block.getType() + ", \"" + toOneLine(block.getName())
                    + "\", " + isParent + ", " + block.getStartOffset() + ", " + block.getEndOffset() + ", " + block.getChildBlocks().size() + ");");
            generateAssert(block.getChildBlocks(), txt + ".get(" + i + ").getChildBlocks()", "true");
        }
    }

    private String toOneLine(String name) {
        String tmp = "";
        int index = name.indexOf("\n");
        if (index == -1) {
            return name;
        }

        while (index != -1) {
            tmp = tmp + name.substring(0, index) + "\\n";
            if (name.length() > index + 1) {
                name = name.substring(index + 1);
                index = name.indexOf("\n");
                if (index == -1) {
                    tmp = tmp + name + "\\n";  //\n is added here because we substring in the end
                }
            } else {
                break;
            }
        }

        tmp = tmp.substring(0, tmp.length() - 2);
        return tmp;
    }
}
