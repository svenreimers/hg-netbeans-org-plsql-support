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
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.netbeans.api.lexer.Language;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.junit.NbTestCase;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import org.netbeans.modules.plsqlsupport.db.PlsqlExecutableObject;
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
public class PlsqlExecutableBlocksMakerTest extends TestRecordPlayer {

    private static FileSystem fs = null;

    public PlsqlExecutableBlocksMakerTest(String name) {
        super(name);
    }
    
    @Before
    public void setUp() {
        fs = FileUtil.createMemoryFileSystem();
        assertNotNull(fs);
    }

    /**
     * Test of makeExceutableObjects method, of class PlsqlExecutableBlocksMaker.
     */
    @Test
    public void testMakeExceutableObjects() throws IOException {

        System.out.println(" Test Exceutable Objects creation");

        final String plsqlFileName = "Actor.apy";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            PlsqlExecutableBlocksMaker instance = new PlsqlExecutableBlocksMaker(doc);
            List<PlsqlExecutableObject> makeExceutableObjects = instance.makeExceutableObjects();
            
            int size = makeExceutableObjects.size();           
            assertEquals(31, size);
            processExecutableBLocksBlocks(plsqlFileName,makeExceutableObjects);
            
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
    
    @Test
    public void test1() throws IOException {

        System.out.println("test 1 - api file");

        final String plsqlFileName = "FndEvent.api";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            PlsqlExecutableBlocksMaker instance = new PlsqlExecutableBlocksMaker(doc);
            List<PlsqlExecutableObject> makeExceutableObjects = instance.makeExceutableObjects();
            
            int size = makeExceutableObjects.size(); 
            assertEquals(12, size);
            processExecutableBLocksBlocks(plsqlFileName,makeExceutableObjects);
            
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

    @Test
    public void test2() throws IOException {

        System.out.println("test 2 - apy file");

        final String plsqlFileName = "FndEvent.apy";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            PlsqlExecutableBlocksMaker instance = new PlsqlExecutableBlocksMaker(doc);
            List<PlsqlExecutableObject> makeExceutableObjects = instance.makeExceutableObjects();
            
            int size = makeExceutableObjects.size();           
            assertEquals(41, size);
            processExecutableBLocksBlocks(plsqlFileName,makeExceutableObjects);
            
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
    
    @Test
    public void test3() throws IOException {

        System.out.println("test 3 - rdf file");

        final String plsqlFileName = "FndSecurityPerObject.rdf";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            PlsqlExecutableBlocksMaker instance = new PlsqlExecutableBlocksMaker(doc);
            List<PlsqlExecutableObject> makeExceutableObjects = instance.makeExceutableObjects();
            
            int size = makeExceutableObjects.size();           
            assertEquals(37, size);
            processExecutableBLocksBlocks(plsqlFileName,makeExceutableObjects);
            
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
    
    @Test
    public void test4() throws IOException {

        System.out.println("test 4 - ins file");

        final String plsqlFileName = "FndSetting.ins";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            PlsqlExecutableBlocksMaker instance = new PlsqlExecutableBlocksMaker(doc);
            List<PlsqlExecutableObject> makeExceutableObjects = instance.makeExceutableObjects();
            
            int size = makeExceutableObjects.size();           
            assertEquals(7, size);
            processExecutableBLocksBlocks(plsqlFileName,makeExceutableObjects);
            
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
    
    @Test
    public void test5() throws IOException {

        System.out.println("test 5 - sql file");

        final String plsqlFileName = "fndbasdr.sql";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            PlsqlExecutableBlocksMaker instance = new PlsqlExecutableBlocksMaker(doc);
            List<PlsqlExecutableObject> makeExceutableObjects = instance.makeExceutableObjects();
            
            int size = makeExceutableObjects.size();           
            assertEquals(81, size);
            processExecutableBLocksBlocks(plsqlFileName,makeExceutableObjects);
            
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
    
    @Test
    public void test6() throws IOException {

        System.out.println("test 6 - cdb file");

        final String plsqlFileName = "event.cdb";
        Document doc = null;
        FileObject fileObject = null;
        try {
            fileObject = fs.getRoot().createData(plsqlFileName);
            doc = getDocument(fileObject, plsqlFileName);

            PlsqlExecutableBlocksMaker instance = new PlsqlExecutableBlocksMaker(doc);
            List<PlsqlExecutableObject> makeExceutableObjects = instance.makeExceutableObjects();
            
            int size = makeExceutableObjects.size();           
            assertEquals(9, size);
            processExecutableBLocksBlocks(plsqlFileName,makeExceutableObjects);
            
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
