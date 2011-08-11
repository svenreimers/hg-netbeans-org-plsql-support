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
package org.netbeans.modules.plsql.filetype;

import org.netbeans.modules.plsql.lexer.PlsqlBlockFactory;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import javax.swing.JPanel;
import org.openide.cookies.EditCookie;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.OpenCookie;
import org.openide.cookies.PrintCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileLock;
import org.openide.nodes.Node.Cookie;
import org.openide.text.CloneableEditor;
import org.openide.text.DataEditorSupport;
import org.openide.windows.CloneableOpenSupport;

/*
 * Class description
 *
 * Created on February 13, 2006, 9:15 AM
 *
 * @author IFS
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
public final class PlsqlEditorSupport extends DataEditorSupport implements OpenCookie,
      EditCookie, EditorCookie.Observable, PrintCookie {

   /** Constructor. */
   PlsqlEditorSupport(PlsqlDataObject obj) {
      super(obj, new Environment(obj));
      setMIMEType("text/x-plsql");
   }
   static final String EDITOR_CONTAINER = "plsqlEditorContainer";
   private final SaveCookie saveCookie = new SaveCookie() {

      /** Implements <code>SaveCookie</code> interface. */
      public void save() throws IOException {
         PlsqlDataObject obj = (PlsqlDataObject)PlsqlEditorSupport.this.getDataObject();
         if (obj.isTemporary())
            //If auto save force saving the document
            obj.setModified(true);
         PlsqlBlockFactory blockFactory = obj.getLookup().lookup(PlsqlBlockFactory.class);
         blockFactory.beforeSave(PlsqlEditorSupport.this.getDocument());
         PlsqlEditorSupport.this.saveDocument();
         blockFactory.afterSave(PlsqlEditorSupport.this.getDocument());
         obj.setModified(false);
      }
   };

   /** Helper method. Adds save cookie to the data object. */
   private void addSaveCookie() {
      PlsqlDataObject obj = (PlsqlDataObject) getDataObject();

      // Adds save cookie to the data object.
      if (obj.getCookie(SaveCookie.class) == null) {
         obj.getCookieSet0().add(saveCookie);
         if (!((PlsqlDataObject)getDataObject()).isTemporary()) {
            obj.setModified(true);
         }
      }
   }

   /**
    * Overrides superclass method. Adds adding of save cookie if the document has been marked modified.
    * @return true if the environment accepted being marked as modified
    *    or false if it has refused and the document should remain unmodified
    */
   protected boolean notifyModified() {
      if (!((PlsqlDataObject)getDataObject()).isTemporary()) {
         if (!super.notifyModified()) {
            return false;
         }
      }
      addSaveCookie();
      return true;
   }

   /** Overrides superclass method. Adds removing of save cookie. */
   protected void notifyUnmodified() {
      if (!((PlsqlDataObject)getDataObject()).isTemporary()) {
         super.notifyUnmodified();
      }
      removeSaveCookie();
   }

   /** Helper method. Removes save cookie from the data object. */
   private void removeSaveCookie() {
      PlsqlDataObject obj = (PlsqlDataObject) getDataObject();

      // Remove save cookie from the data object.
      Cookie cookie = obj.getCookie(SaveCookie.class);

      if (cookie != null && cookie.equals(saveCookie)) {
         obj.getCookieSet0().remove(saveCookie);
         if (!((PlsqlDataObject)getDataObject()).isTemporary()) {
            obj.setModified(false);
         }
      }
   }

   /** Nested class. Environment for this support. Extends <code>DataEditorSupport.Env</code> */
   private static class Environment extends DataEditorSupport.Env {

      private static final long serialVersionUID = 3035543168452715818L;

      /** Constructor. */
      public Environment(PlsqlDataObject obj) {
         super(obj);
      }

      /** Implements abstract superclass method. */
      protected FileObject getFile() {
         return getDataObject().getPrimaryFile();
      }

      /** Implements abstract superclass method.*/
      protected FileLock takeLock() throws IOException {
         return ((PlsqlDataObject) getDataObject()).getPrimaryEntry().takeLock();
      }

      /**
       * Overrides superclass method.
       * @return text editor support (instance of enclosing class)
       */
      public CloneableOpenSupport findCloneableOpenSupport() {
         return getDataObject().getCookie(PlsqlEditorSupport.class);
      }
   } // End of nested Environment class.

   /** A method to create a new component. Overridden in subclasses.
    * @return the {@link PLSQLEditor} for this support
    */
   protected CloneableEditor createCloneableEditor() {
      return new PlsqlEditor(this);
   }

   protected Component wrapEditorComponent(Component editor) {
      JPanel container = new JPanel(new BorderLayout());
      container.setName(EDITOR_CONTAINER); // NOI18N
      container.add(editor, BorderLayout.CENTER);
      return container;
   }
}
