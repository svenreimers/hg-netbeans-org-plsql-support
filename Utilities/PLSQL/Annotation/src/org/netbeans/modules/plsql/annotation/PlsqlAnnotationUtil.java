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
import org.netbeans.modules.plsql.annotation.annotations.PlsqlBlockAnnotation;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;

/**
 * Utility class with some common methods
 */
public class PlsqlAnnotationUtil {

   public static Document getDocument() {
      final JTextComponent comp = EditorRegistry.lastFocusedComponent();
      return comp == null ? null : comp.getDocument();
   }

   public static DataObject getDataObject(final Document doc) {
      final Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof DataObject) {
         DataObject dataObj = (DataObject) obj;
         return dataObj;
      }
      return null;
   }

   public static FileObject getFileObject(final Document doc) {
      final Object obj = doc == null ? null : doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof DataObject) {
         return ((DataObject) obj).getPrimaryFile();
      }
      return null;
   }

   public static String getType(final Document doc) {
      final FileObject fileObject = PlsqlAnnotationUtil.getFileObject(doc);
      return fileObject == null ? null : fileObject.getExt();
   }

   public static void addAnnotation(final PlsqlAnnotation annotation, final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd) {
      if (annotation != null) {
         final int offset = annotation.getOffset();
         List<PlsqlAnnotation> lstAnnotations = annotationsToAdd.remove(Integer.valueOf(offset));
         if (lstAnnotations == null) {
            lstAnnotations = new ArrayList<PlsqlAnnotation>();
         }

         lstAnnotations.add(annotation);
         annotationsToAdd.put(Integer.valueOf(offset), lstAnnotations);
      }
   }

   public static PlsqlAnnotationManager getAnnotationManager(final Document doc) {
      final Object obj = doc.getProperty(Document.StreamDescriptionProperty);
      if (obj instanceof DataObject) {
         final DataObject dataObj = (DataObject) obj;
         final String contentType = dataObj.getLoader().getRepresentationClassName().trim();
         if ("org.netbeans.modules.plsql.filetype.PlsqlDataObject".equals(contentType)) {
            return dataObj.getLookup().lookup(PlsqlAnnotationManager.class);
         }
      }

      return null;
   }

   public static void callBlockAnnotations(final PlsqlAnnotationManager manager, final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd, final Document doc, final PlsqlBlock block, final Document specDoc, final PlsqlBlockFactory specBlockFac, final String type) {
      final Set<PlsqlAnnotation> annotations = manager.getConfiguration(type);
      if (annotations != null) {
         for (PlsqlAnnotation temp : annotations) {
            if (temp instanceof PlsqlBlockAnnotation) {
               ((PlsqlBlockAnnotation) temp).evaluateAnnotation(annotationsToAdd, doc, block, specDoc, specBlockFac);
            }
         }
      }
   }

   public static DataObject getGeneratedDataObject(final Document doc) {
      final FileObject fileObject = PlsqlAnnotationUtil.getFileObject(doc);
      if (fileObject != null) {
         final String fileName = fileObject.getParent().getPath() + "/.ifs/" + fileObject.getName() + ".gen." + fileObject.getExt();
         final File file = new File(fileName);
         if (file.exists()) {
            try {
               return DataFolder.find(FileUtil.toFileObject(file));
            } catch (DataObjectNotFoundException ex) {
               Exceptions.printStackTrace(ex);
            }
         }
      }

      return null;
   }

   public static boolean isFileReadOnly(final Document doc) {
      final FileObject fileObject = PlsqlAnnotationUtil.getFileObject(doc);
      return (fileObject != null && !fileObject.canWrite());
   }
}
