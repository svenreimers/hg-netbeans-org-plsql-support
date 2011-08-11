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
package org.netbeans.modules.plsql.annotation;

import org.netbeans.modules.plsql.annotation.annotations.PlsqlAnnotation;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlTokenId;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
import static org.junit.Assert.*;

/**
 *
 * @author YADHLK
 */
public class PlsqlAnnotationsTest {

    private static final String WRONG_PARAM_ORDER = "Plsql-wrong-param-order-annotation";
    private static final String WRONG_FUNCTION_PARAM = "Plsql-wrong-function-param-annotation";
    private static final String UNREACHABLE = "Plsql-unreachable-annotation";
    private static final String MISSING_END_NAME = "Plsql-missing-end-name-annotation";
    private static final String WRONG_END_NAME = "Plsql-wrong-end-name-annotation";
    private static final String IF_NULL = "Plsql-if-null-annotation";
    private static final String CURSOR_WHERE = "Plsql-cursor-where-clause-annotation";
    
    
    private String testApyClass = "resources/appsrv/TechnicalSpecification.apy";
    private String testCursorWhere = "resources/shpord/ShopOrd.apy";
    private String testIfNull = "resources/appsrv/Formula.apy";
    private String testMissingEnd = "resources/appsrv/FormulaItem.apy";
    private String testUnreachable = "resources/appsrv/TechnicalAttrib.apy";
    private String testWrongEnd = "resources/shpord/MachOperationLoad.apy";
    private String testWFuncParam = "resources/shpord/ShopOperClocking.apy";
    private String testWParamOrd = "resources/shpord/ShopMaterialAlloc.apy";
    
    private DataObject dataObject = null;
    private FileObject fileObject = null;
    private FileObject parentObject = null;
    private static FileSystem fs = null;

    public PlsqlAnnotationsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        fs = FileUtil.createMemoryFileSystem();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        assertNotNull(fs);
    }

    @After
    public void tearDown() throws IOException {
    }

    /**
     * Test of annotations of the apy file
     */
//    @Test
//    public void testApyAnnotations() throws IOException, BadLocationException {
//        try {
//            System.out.println("Testing Apy annotations");
//            createFile("appsrv", "TechnicalSpecification.apy");
//            assertNull(dataObject);
//            dataObject = loadAsTmpFile(fileObject, testApyClass);
//            assertNotNull(dataObject);
//
//            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
//            assertNotNull(annotationManager);
//            annotationManager.initAnnotations(dataObject);
//
//            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
//            System.out.println(annotations.toString());
//            assertNotNull(annotations);
//            //printAnnotations(annotations);
//            assertTrue(annotations.size() == 7);
//
//           // assertAnnotation(annotations, 2673, COMMENT_REF);
//            assertAnnotation(annotations, 4287, WRONG_FUNCTION_PARAM);
//           // assertAnnotation(annotations, 5834, SELECT_FROM);
//           // assertAnnotation(annotations, 6663, UNIQUE_TAG);
//           // assertAnnotation(annotations, 6818, UNIQUE_TAG);
//           // assertAnnotation(annotations, 8296, SAME_TAG);
//           // assertAnnotation(annotations, 8444, SAME_TAG);
//            //assertAnnotation(annotations, 9266, FUNCTION_RETURN);
//            assertAnnotation(annotations, 10138, UNREACHABLE);
//            assertAnnotation(annotations, 11152, CURSOR_WHERE);
//            //assertAnnotation(annotations, 11380, COMMENTS);
//            assertAnnotation(annotations, 13111, IF_NULL);
//            assertAnnotation(annotations, 17321, MISSING_END_NAME);
//            assertAnnotation(annotations, 18534, WRONG_END_NAME);
//            //assertAnnotation(annotations, 21412, WRONG_PARAM);
//            assertAnnotation(annotations, 21752, WRONG_PARAM_ORDER);
//        } finally {
//            if (parentObject != null) {
//                parentObject.delete();
//            }
//        }
//    }

    private DataObject loadAsTmpFile(FileObject fileObj, String testApyClass) throws IOException, BadLocationException {
        InputStreamReader indexReader = null;
        InputStream inputStream = null;
        BufferedReader input = null;
        String content = "";
        inputStream = PlsqlValidateFilesAction.class.getResourceAsStream(testApyClass);
        indexReader = new InputStreamReader(inputStream);
        input = new BufferedReader(indexReader);
        while (input.ready()) {
            content = content + input.readLine() + "\n";
        }
        input.close();
        inputStream.close();
        indexReader.close();
        writeFile(content, fileObj);

        DataObject dataObj = null;
        dataObj = DataFolder.find(fileObj);
        EditorCookie ec = dataObj.getCookie(EditorCookie.class);
        assertNotNull(ec);
        Task task = ec.prepareDocument();
        task.waitFinished();
        PlsqlBlockFactory blockFac = dataObj.getLookup().lookup(PlsqlBlockFactory.class);
        assertNotNull(blockFac);
        Document doc = ec.getDocument();
        doc.putProperty(Language.class, PlsqlTokenId.language());
        assertNotNull(doc);

        TokenHierarchy tokenHier = TokenHierarchy.get(doc);
        assertNotNull(tokenHier);
        TokenSequence<PlsqlTokenId> ts = tokenHier.tokenSequence(PlsqlTokenId.language());
        assertNotNull(ts);
        blockFac.initHierarchy(doc);

        return dataObj;
    }

    private void printAnnotations(Map<Integer, List<PlsqlAnnotation>> annotations) {
        System.out.println("Annotations size" + annotations.size());
        Integer[] keys = annotations.keySet().toArray(new Integer[0]);
        Arrays.sort(keys);
        for (Integer offset : keys) {
            List<PlsqlAnnotation> offsetAnnotations = annotations.get(offset);
            System.out.println("   Offset: " + offset);
            for (PlsqlAnnotation annotation : offsetAnnotations) {
                System.out.println("      " + annotation.getErrorToolTip());
                System.out.println("      " + annotation.getAnnotationType());
            }
        }
    }

    private void writeFile(String content, FileObject file) throws IOException {
        OutputStream os = file.getOutputStream();
        os.write(content.getBytes("UTF-8"));
        os.close();
    }

    private void assertAnnotation(Map<Integer, List<PlsqlAnnotation>> annotations, int offset, String type) {
        try {
            List<PlsqlAnnotation> offsetAnnotations = annotations.get(offset);
            assertNotNull(offsetAnnotations);
            PlsqlAnnotation annotation = offsetAnnotations.get(0);
            assertNotNull(annotation);
            assertTrue(annotation.getAnnotationType().equals(type));
        } catch (AssertionError e) {
            System.err.println("failed on offset " + offset);
            System.err.println("expected " + type);
            throw e;
        }
    }

    private void createFile(String parent, String fileName) throws IOException {
        assertNotNull(fs);
        parentObject = fs.getRoot().createFolder(parent);
        assertNotNull(parentObject);
        fileObject = parentObject.createData(fileName);
        assertNotNull(fileObject);
    }

    @Test
    public void testCursorWhere() throws IOException, BadLocationException {
        try {
            System.out.println("Testing annotations on common typing mistakes of the cursor where clause");
            createFile("shpord", "ShopOrd.apy");
            assertNull(dataObject);
            dataObject = loadAsTmpFile(fileObject, testCursorWhere);
            assertNotNull(dataObject);

            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
            assertNotNull(annotationManager);
            annotationManager.initAnnotations(dataObject);

            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
            assertNotNull(annotations);
            //printAnnotations(annotations);
            assertTrue(annotations.size() == 4);

            assertAnnotation(annotations, 22701, CURSOR_WHERE);
            assertAnnotation(annotations, 22882, CURSOR_WHERE);
            assertAnnotation(annotations, 22972, CURSOR_WHERE);
            assertAnnotation(annotations, 23053, CURSOR_WHERE);
        } finally {
            if (parentObject != null) {
                parentObject.delete();
            }
        }
    }

    @Test
    public void testIfNull() throws IOException, BadLocationException {
        try {
            System.out.println("Testing annotations on If condition");
            createFile("appsrv", "Formula.apy");
            assertNull(dataObject);
            dataObject = loadAsTmpFile(fileObject, testIfNull);
            assertNotNull(dataObject);

            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
            assertNotNull(annotationManager);
            annotationManager.initAnnotations(dataObject);

            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
            assertNotNull(annotations);
            //printAnnotations(annotations);
            assertTrue(annotations.size() == 2);

            assertAnnotation(annotations, 8951, IF_NULL);
            assertAnnotation(annotations, 12578, IF_NULL);
        } finally {
            if (parentObject != null) {
                parentObject.delete();
            }
        }
    }

    @Test
    public void testMissingEnd() throws IOException, BadLocationException {
        try {
            System.out.println("Testing annotations on Missing end name of the method");
            createFile("appsrv", "FormulaItem.apy");
            assertNull(dataObject);
            dataObject = loadAsTmpFile(fileObject, testMissingEnd);
            assertNotNull(dataObject);

            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
            assertNotNull(annotationManager);
            annotationManager.initAnnotations(dataObject);

            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
            assertNotNull(annotations);
            //printAnnotations(annotations);
            assertTrue(annotations.size() == 2);

            assertAnnotation(annotations, 4990, MISSING_END_NAME);
            assertAnnotation(annotations, 5609, MISSING_END_NAME);
        } finally {
            if (parentObject != null) {
                parentObject.delete();
            }
        }
    }


    @Test
    public void testUnreachable() throws IOException, BadLocationException {
        try {
            System.out.println("Testing annotations on unreachable statements");
            createFile("appsrv", "TechnicalAttrib.apy");
            assertNull(dataObject);
            dataObject = loadAsTmpFile(fileObject, testUnreachable);
            assertNotNull(dataObject);

            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
            assertNotNull(annotationManager);
            annotationManager.initAnnotations(dataObject);

            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
            assertNotNull(annotations);
            //printAnnotations(annotations);
            assertTrue(annotations.size() == 2);

            assertAnnotation(annotations, 4589, UNREACHABLE);
            assertAnnotation(annotations, 5917, UNREACHABLE);
        } finally {
            if (parentObject != null) {
                parentObject.delete();
            }
        }
    }

    @Test
    public void testWrongEnd() throws IOException, BadLocationException {
        try {
            System.out.println("Testing annotations on Wrong method end names");
            createFile("shpord", "MachOperationLoad.apy");
            assertNull(dataObject);
            dataObject = loadAsTmpFile(fileObject, testWrongEnd);
            assertNotNull(dataObject);

            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
            assertNotNull(annotationManager);
            annotationManager.initAnnotations(dataObject);

            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
            assertNotNull(annotations);
            //printAnnotations(annotations);
            assertTrue(annotations.size() == 2);

            assertAnnotation(annotations, 5866, WRONG_END_NAME);
            assertAnnotation(annotations, 7083, WRONG_END_NAME);
        } finally {
            if (parentObject != null) {
                parentObject.delete();
            }
        }
    }

    @Test
    public void testFunctionParam() throws IOException, BadLocationException {
        try {
            System.out.println("Testing annotations on wrong function parameter types");
            createFile("shpord", "ShopOperClocking.apy");
            assertNull(dataObject);
            dataObject = loadAsTmpFile(fileObject, testWFuncParam);
            assertNotNull(dataObject);

            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
            assertNotNull(annotationManager);
            annotationManager.initAnnotations(dataObject);

            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
            assertNotNull(annotations);
            //printAnnotations(annotations);
            assertTrue(annotations.size() == 2);

            assertAnnotation(annotations, 30929, WRONG_FUNCTION_PARAM);
            assertAnnotation(annotations, 31531, WRONG_FUNCTION_PARAM);
        } finally {
            if (parentObject != null) {
                parentObject.delete();
            }
        }
    }

    @Test
    public void testWrongParamOrder() throws IOException, BadLocationException {
        try {
            System.out.println("Testing annotations on the order of method parameters");
            createFile("shpord", "ShopMaterialAlloc.apy");
            assertNull(dataObject);
            dataObject = loadAsTmpFile(fileObject, testWParamOrd);
            assertNotNull(dataObject);

            PlsqlAnnotationManager annotationManager = dataObject.getLookup().lookup(PlsqlAnnotationManager.class);
            assertNotNull(annotationManager);
            annotationManager.initAnnotations(dataObject);

            Map<Integer, List<PlsqlAnnotation>> annotations = annotationManager.getAnnotations();
            assertNotNull(annotations);
            //printAnnotations(annotations);
            assertTrue(annotations.size() == 2);

            assertAnnotation(annotations, 18912, WRONG_PARAM_ORDER);
            assertAnnotation(annotations, 61701, WRONG_PARAM_ORDER);
        } finally {
            if (parentObject != null) {
                parentObject.delete();
            }
        }
    }
}
