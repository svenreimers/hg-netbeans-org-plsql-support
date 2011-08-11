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

import org.netbeans.modules.plsqlsupport.options.PLSQLAnnotationsPanel;
import org.netbeans.modules.plsql.annotation.annotations.PlsqlAnnotation;
import org.netbeans.modules.plsql.lexer.PlsqlBlock;
import java.util.Observable;
import java.util.Observer;
import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.StyledDocument;
import org.openide.cookies.EditorCookie;
import org.openide.text.NbDocument;
import org.openide.util.Exceptions;
import org.openide.loaders.DataObject;
import org.openide.util.NbPreferences;
import org.openide.util.Task;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service=PlsqlAnnotationManager.class)
public class PlsqlAnnotationManager implements Observer {

   public Document doc;
   public final Map<Integer, List<PlsqlAnnotation>> plsqlAnnotations;
   public final Map<Integer, String> errorSysCalls;
   public static Map<String, Set<PlsqlAnnotation>> configuration;
   public final Map<Integer, Set<String>> allowedTablesOrViews;

   public static Annotation annotation = new GenericPlsqlAnnotations();
   
   public PlsqlAnnotationManager() {
      annotation.loadConfiguration();
      
      final PreferenceChangeListener listener = new PreferenceChangeListener() {

         @Override
         public void preferenceChange(final PreferenceChangeEvent evt) {
            if (evt.getKey().startsWith("plsql.annotations.")) {
               annotation.loadConfiguration();
               if(doc != null){
                   configuration = null;
                   configuration = annotation.getConfiguration();
                   DataObject dataObj = (DataObject) doc.getProperty(Document.StreamDescriptionProperty);
                   PlsqlBlockFactory apiBlockFac = dataObj.getLookup().lookup(PlsqlBlockFactory.class);
                   initAnnotations(dataObj);
                   update(apiBlockFac,doc);                   
               }
            }
         }
      };
      NbPreferences.forModule(PLSQLAnnotationsPanel.class).addPreferenceChangeListener(listener);
      
      plsqlAnnotations = new ConcurrentHashMap<Integer, List<PlsqlAnnotation>>();
      errorSysCalls = new ConcurrentHashMap<Integer, String>();
      allowedTablesOrViews = new ConcurrentHashMap<Integer, Set<String>>();
      configuration = annotation.getConfiguration();     
   }

   @Override
   public void update(final Observable obj1, final Object obj2) {
      if ((obj1 instanceof PlsqlBlockFactory) && (obj2 instanceof Document)) {
         final PlsqlBlockFactory blockFactory = (PlsqlBlockFactory) obj1;
         doc = (Document) obj2;

         //Update start offsets of the changed annotations
         updateAnnotations(blockFactory.getStartParse(), blockFactory.getChangedLength());

         //Remove annotations relevant to removed blocks
         removeAnnotations(blockFactory.getStartParse(), blockFactory.getEndParse());

         //Add all annotation types relevant to new blocks which are added
         final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd = new HashMap<Integer, List<PlsqlAnnotation>>();
         
         updateAllowedTableOrViewOffsets(blockFactory.getStartParse(), blockFactory.getChangedLength());
         PlsqlPackageAnnotationUtil.getPackageAnnotations(this, blockFactory.getBlockHierarchy(), annotationsToAdd, doc); //New blocks wasn't passed since package spec is not reparsed
         PlsqlFileAnnotationUtil.getBlockAnnotations(this, blockFactory.getNewBlocks(), annotationsToAdd, doc);
         PlsqlFileAnnotationUtil.getFileAnnotations(this, annotationsToAdd, doc, blockFactory.getStartParse(), blockFactory.getEndParse(), blockFactory.getChangedLength());

         //Since we are rechecking package annotations we have to remove the existing ones
         removePackageAnnotations(blockFactory.getBlockHierarchy());
         attachAnnotations(annotationsToAdd);
      }
   }

   public void clearAllAnnotations() {
      for (List<PlsqlAnnotation> lstAnnotation : plsqlAnnotations.values()) {
         for (PlsqlAnnotation temp : lstAnnotation) {
            detachAnnotation(temp);
         }
      }
      errorSysCalls.clear();
      allowedTablesOrViews.clear();
   }

   public void initAnnotations(final DataObject dataObject) {
      clearAllAnnotations();
      final EditorCookie editorCookie = dataObject.getLookup().lookup(EditorCookie.class);
      if (editorCookie == null) {
         return;
      }
      final Task task = editorCookie.prepareDocument();
      task.waitFinished();
      doc = editorCookie.getDocument();
      if (doc == null) {
         return;
      }

      final Object obj = doc.getProperty("INIT_ANNOTATIONS");
      if ((obj == null) || (!obj.equals("YES"))) {
         final PlsqlBlockFactory blockFactory = dataObject.getLookup().lookup(PlsqlBlockFactory.class);
         blockFactory.initHierarchy(doc);
         blockFactory.addObserver(this);
         final List<PlsqlBlock> blockhierarchy = blockFactory.getBlockHierarchy();
         if (blockhierarchy != null) {
            //Init annotations for all the annotation types
            plsqlAnnotations.clear();
            final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd = new HashMap<Integer, List<PlsqlAnnotation>>();
            final int docStartOffset = doc.getStartPosition().getOffset();
            final int docEndOffset = doc.getEndPosition().getOffset();
            PlsqlFileAnnotationUtil.getFileAnnotations(this, annotationsToAdd, doc, docStartOffset, docEndOffset, 0);
            
            PlsqlPackageAnnotationUtil.getPackageAnnotations(this, blockhierarchy, annotationsToAdd, doc);
            PlsqlFileAnnotationUtil.getBlockAnnotations(this, blockhierarchy, annotationsToAdd, doc);
            attachAnnotations(annotationsToAdd);
         }
         doc.putProperty("INIT_ANNOTATIONS", "YES");
      } else {
         //We need to attach the annotation to the document
         for (Integer offset : plsqlAnnotations.keySet()) {
            final List<PlsqlAnnotation> lstAnnotations = plsqlAnnotations.get(offset);
            final int anoOffset = offset;
            final Position position = new Position() {

               @Override
               public int getOffset() {
                  return anoOffset;
               }
            };
            try {
               for (PlsqlAnnotation annotation : lstAnnotations) {
                  attachAnnotation(position, annotation);
               }
            } catch (BadLocationException ex) {
               Exceptions.printStackTrace(ex);
            }
         }
      }
   }

   /**
    * Remove annotation upon annotation action
    * @param lineOffset
    * @param type
    * @return
    */
   public boolean removeAnnotation(final int lineOffset, final String type) {
      final List<PlsqlAnnotation> lstAnnotation = plsqlAnnotations.get(lineOffset);
      boolean found = false;
      if (lstAnnotation != null) {
         for (PlsqlAnnotation temp : lstAnnotation) {
            if (type.equals(temp.getAnnotationType())) {
               found = true;
            }

            if (found) {
               lstAnnotation.remove(temp);
               detachAnnotation(temp);

               if (lstAnnotation.isEmpty()) {
                  plsqlAnnotations.remove(lineOffset);
               }
               return true;
            }
         }
      }

      return false;
   }

   public void removeAnnotations(final int start, final int end) {
      //Remove matching annotations
      final Set<Integer> keys = plsqlAnnotations.keySet();
      for (Integer offset : keys) {
         if (offset <= end && offset >= start) {
            final List<PlsqlAnnotation> lstAnnotation = plsqlAnnotations.remove(offset);
            if (lstAnnotation != null) {
               for (PlsqlAnnotation tempAnno : lstAnnotation) {
                  detachAnnotation(tempAnno);
               }
            }
         }
      }
   }

   public void removeAnnotations(final int start, final int end, final String type) {
      //Remove matching annotations
      final Set<Integer> keys = plsqlAnnotations.keySet();
      for (Integer offset : keys) {
         if (offset <= end && offset >= start) {
            final List<PlsqlAnnotation> lstAnnotation = plsqlAnnotations.remove(offset);
            if (lstAnnotation != null) {
               for (int i = lstAnnotation.size() - 1; i >= 0; i--) {
                  final PlsqlAnnotation tempAnno = lstAnnotation.get(i);
                  if (tempAnno.getAnnotationType().equals(type)) {
                     lstAnnotation.remove(tempAnno);
                     detachAnnotation(tempAnno);
                  }
               }

               if (!lstAnnotation.isEmpty()) {
                  plsqlAnnotations.put(offset, lstAnnotation);
               }
            }
         }
      }
   }

   /**
    * Get annotations attached to the given offset
    * @param offset
    * @return
    */
   public List<PlsqlAnnotation> getAnnotations(final int offset) {
      List<PlsqlAnnotation> lstAnnotation = plsqlAnnotations.get(offset);
      if (lstAnnotation == null) {
         lstAnnotation = new ArrayList<PlsqlAnnotation>();
      }

      return lstAnnotation;
   }

   public Map<Integer, List<PlsqlAnnotation>> getAnnotations() {
      return Collections.unmodifiableMap(plsqlAnnotations);
   }

   public void attachAnnotation(final Position line, final PlsqlAnnotation a) throws BadLocationException {
      NbDocument.addAnnotation((StyledDocument) doc, line, -1, a);
   }

   private void detachAnnotation(final PlsqlAnnotation a) {
      if (doc != null) {
         NbDocument.removeAnnotation((StyledDocument) doc, a);
      }
   }

   public void attachAnnotations(final Map<Integer, List<PlsqlAnnotation>> annotationsToAdd) {
      for (Integer offset : annotationsToAdd.keySet()) {
         final List<PlsqlAnnotation> lstNewAnnotation = annotationsToAdd.get(offset);
         List<PlsqlAnnotation> lstExisting = plsqlAnnotations.get(offset);
         if (lstExisting == null) {
            lstExisting = new ArrayList<PlsqlAnnotation>();
            plsqlAnnotations.put(offset, lstExisting);
         }

         final int anoOffset = offset;
         final Position position = new Position() {

            @Override
            public int getOffset() {
               return anoOffset;
            }
         };
         try {
            for (PlsqlAnnotation plsqlAnnotation : lstNewAnnotation) {
               boolean isExisting = false;
               for (PlsqlAnnotation existing : lstExisting) {
                  if (plsqlAnnotation.getAnnotationType().equals(existing.getAnnotationType())) {
                     isExisting = true;
                     break;
                  }
               }

               if (!isExisting) {
                  lstExisting.add(plsqlAnnotation);
                  attachAnnotation(position, plsqlAnnotation);
               }
            }
         } catch (BadLocationException ex) {
            Exceptions.printStackTrace(ex);
         }
      }
   }

   public void updateAnnotations(final int endParse, final int changedLength) {
      final List<Integer> lstOffset = new ArrayList<Integer>();
      for (Integer offset : plsqlAnnotations.keySet()) {
         if (offset > endParse) {
            lstOffset.add(offset);
         }
      }

      for (Integer temp : lstOffset) {
         final List<PlsqlAnnotation> lstAnnotations = plsqlAnnotations.remove(Integer.valueOf(temp));
         if (lstAnnotations != null) {
            final int newOff = temp + changedLength;
            for (PlsqlAnnotation tmpAnn : lstAnnotations) {
               tmpAnn.setOffset(newOff);
            }
            plsqlAnnotations.put(Integer.valueOf(newOff), lstAnnotations);
         }
      }
   }

   private void removePackageAnnotations(final List<PlsqlBlock> blockHierarchy) {}
   
   public void resetAllowedTablesOrViews(final Document doc, final int offset) {}

   private void removeMethodAnnotations(final List<PlsqlBlock> blockHierarchy) {
      for (PlsqlBlock block : blockHierarchy) {
         if (block.getType() == PlsqlBlockType.PACKAGE_BODY) {
            removeMethodAnnotations(block.getChildBlocks());
         }
      }
   }

   public Set<PlsqlAnnotation> getConfiguration(final String type) {
      return configuration.get(type);
   }

   public Map<Integer, String> getErrorSysCalls() {
      return errorSysCalls;
   }

   public boolean isTableOrViewAllowed(final int offset, final String name) {
      final Set<String> tablesOrViews = allowedTablesOrViews.get(offset);
      if (tablesOrViews != null) {
         return tablesOrViews.contains(name.toUpperCase(Locale.ENGLISH));
      }

      return false;
   }

   public void addTableOrView(final int offset, final String name) {
      Set<String> tablesOrViews = allowedTablesOrViews.get(offset);
      if (tablesOrViews == null) {
         tablesOrViews = new HashSet<String>();
         allowedTablesOrViews.put(offset, tablesOrViews);
      }

      tablesOrViews.add(name.toUpperCase(Locale.ENGLISH));
   }

   public void resetErrorSysCalls(final Document doc, final int startParse, final int endParse, final int change, final Map<Integer, String> errorMsgs) {
      if (startParse == doc.getStartPosition().getOffset() && endParse == doc.getEndPosition().getOffset()) {
         errorSysCalls.clear();
      } else {
         final Map<Integer, String> changed = new HashMap<Integer, String>();
         for (Iterator<Integer> iter = errorSysCalls.keySet().iterator(); iter.hasNext();) {
            final Integer errorOffset = iter.next();
            if (errorOffset >= startParse && errorOffset <= endParse) {
               iter.remove();
            } else if (errorOffset > startParse) {
               changed.put(errorOffset + change, errorSysCalls.get(errorOffset));
               iter.remove();
            }
         }

         errorSysCalls.putAll(changed);
      }
      errorSysCalls.putAll(errorMsgs);
   }

   /**
    * Reset everything so that when annotations are enabled again everything will be reparsed
    */
   public void clearAnnotations() {
      clearAllAnnotations();
      plsqlAnnotations.clear();
      if (doc != null) {
         doc.putProperty("INIT_ANNOTATIONS", "NO");
      }
   }

   public void updateAllowedTableOrViewOffsets(final int startParse, final int change) {
      final Map<Integer, Set<String>> changed = new HashMap<Integer, Set<String>>();
      for (Iterator<Integer> iter = allowedTablesOrViews.keySet().iterator(); iter.hasNext();) {
         final Integer packageIOffet = iter.next();
         if (packageIOffet > startParse) {
            changed.put(packageIOffet + change, allowedTablesOrViews.get(packageIOffet));
            iter.remove();
         }
      }

      allowedTablesOrViews.putAll(changed);
   }
}
