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
 *  Rifki Razick
 *
 * Portions Copyrighted 2011 Sun Microsystems, Inc.
 */

package org.netbeans.modules.plsql.utilities.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.editor.Utilities;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;

public class PlSqlMethodPresenterPanel {

   private JPanel panel;
   private JTextComponent editorPane;
   private String method;
   private static String mimeType = "text/x-plsql";
   private Project project;
   private String label;

   public PlSqlMethodPresenterPanel(Project project, String method, String label) {
      this.method = method;
      this.project = project;
      this.label = label;
   }

   public JComponent getPanel() {
      if (panel != null) {
         return panel;
      }

      panel = new JPanel();            
      JLabel textLabel = new JLabel(label);      
      textLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
      panel.setLayout(new BorderLayout());
      panel.setBorder(new EmptyBorder(11, 12, 1, 11));
      panel.add(BorderLayout.NORTH, textLabel);

      //Add JEditorPane and context
      JComponent[] editorComponents = Utilities.createSingleLineEditor(mimeType);
      JScrollPane sp = (JScrollPane) editorComponents[0];
      editorPane = (JTextComponent) editorComponents[1];
      int h = sp.getPreferredSize().height;
      int w = Math.min(70 * editorPane.getFontMetrics(editorPane.getFont()).charWidth('a'),
              org.openide.windows.WindowManager.getDefault().getMainWindow().getSize().width);
      sp.setPreferredSize(new Dimension(w, h));
      
      try {
         //put a plsql type DataObject to the editor in order to get a DB connection for code completion
         File tmpFile = File.createTempFile("plsql_dlg", ".tdb");
         tmpFile.deleteOnExit();
         FileObject tmpObj = FileUtil.createData(FileUtil.normalizeFile(tmpFile));
         FileOwnerQuery.markExternalOwner(tmpObj, project, FileOwnerQuery.EXTERNAL_ALGORITHM_TRANSIENT);
         DataObject data = DataObject.find(tmpObj);
         editorPane.getDocument().putProperty(Document.StreamDescriptionProperty, data);
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
      }      

      panel.add(BorderLayout.CENTER, sp);      
      editorPane.setText(method);
      editorPane.selectAll();
      textLabel.setLabelFor(editorPane);
      editorPane.requestFocus();
      return panel;
   }

   /*
    * returns the pl/sql method selected by the user
    */
   public String getMethod() {
      return editorPane.getText().trim();
   }
}
