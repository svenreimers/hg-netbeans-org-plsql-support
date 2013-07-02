/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Task;

/**
 *
 * @author subslk
 */
public class FileExecutionUtilTest extends NbTestCase {

    private static FileSystem fs = null;

    public FileExecutionUtilTest(String name) {
        super(name);
    }

    @Before
    public void setUp() {
        fs = FileUtil.createMemoryFileSystem();
        assertNotNull(fs);
    }

    /**
     * Test of splitStringToVector method, of class FileExecutionUtil.
     */
    @Test
    public void testSplitStringToVector() {
        System.out.println("Split String To Vector");

        String stmt = "DECLARE \n"
                + "   action_enable_ VARCHAR2 := &action_enable_; \n"
                + " \n"
                + "BEGIN \n"
                + "   SELECT * \n"
                + "   FROM fnd_event_action eventAcc \n"
                + "   WHERE eventAcc.action_enable = &action_enable_; \n"
                + " \n"
                + "END;\n";

        List<String> expResult = new ArrayList<String>();
        expResult.add("DECLARE ");
        expResult.add("   action_enable_ VARCHAR2 := &action_enable_; ");
        expResult.add(" ");
        expResult.add("BEGIN ");
        expResult.add("   SELECT * ");
        expResult.add("   FROM fnd_event_action eventAcc ");
        expResult.add("   WHERE eventAcc.action_enable = &action_enable_; ");
        expResult.add(" ");
        expResult.add("END;");

        List result = FileExecutionUtil.splitStringToVector(stmt);
        assertEquals(expResult, result);

    }

    /**
     * Test of getLineNoForOffset method, of class FileExecutionUtil.
     */
    @Test
    public void testGetLineNoForOffset() throws IOException {
        System.out.println("Get Line No For Offset");

        final String plsqlFileName = "Actor.apy";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            //879 - begining of "PROMPT Creating &VIEW VIEW"
            int offset = 879;
            int expResult = 27;
            int result = FileExecutionUtil.getLineNoForOffset(doc, offset);
            assertEquals(expResult, result);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    /**
     * Test of readLine method, of class FileExecutionUtil.
     */
    @Test
    public void testReadLine() throws IOException {
        System.out.println("Read Line");

        final String plsqlFileName = "Actor.apy";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            TokenHierarchy tokenHierarchy = TokenHierarchy.get(doc);
            @SuppressWarnings("unchecked")
            TokenSequence<PlsqlTokenId> ts = tokenHierarchy.tokenSequence(PlsqlTokenId.language());

            ts.move(907); // CREATE
            ts.moveNext();
            Token<PlsqlTokenId> token = ts.token();

            String expResult = "CREATE OR REPLACE VIEW &VIEW AS";
            String result = FileExecutionUtil.readLine(ts, token);
            assertEquals(expResult, result);
        } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } finally {
            if (fileObject != null) {
                fileObject.delete();
            }
        }
    }

    private Document getDocument(FileObject fileObj, String fileName) throws IOException, BadLocationException {
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

        return doc;

    }

    private void writeFile(String content, FileObject file) throws IOException {
        OutputStream os = file.getOutputStream();
        os.write(content.getBytes("UTF-8"));
        os.close();
    }
}
