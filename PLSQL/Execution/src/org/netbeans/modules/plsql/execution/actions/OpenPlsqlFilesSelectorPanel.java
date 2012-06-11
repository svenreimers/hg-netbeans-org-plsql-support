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
package org.netbeans.modules.plsql.execution.actions;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.netbeans.modules.plsql.filetype.PlsqlDataLoader;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author riralk 
 * based on org.netbeans.modules.diff.EditorBufferSelectorPanel by Maros Sandor * 
 */
class OpenPlsqlFilesSelectorPanel extends JPanel implements ListSelectionListener {
    
    private JList elementsList;
    private List<File> openFiles = new ArrayList<File>();
   
    public OpenPlsqlFilesSelectorPanel() {       
        initComponents();
        initEditorDocuments();
    }

    private void initEditorDocuments() {
        elementsList = new JList() {
            @Override
            public String getToolTipText(MouseEvent event) {
                int index = locationToIndex(event.getPoint());
                if (index != -1) {
                    PlsqlFileListElement element = (PlsqlFileListElement) elementsList.getModel().getElementAt(index);
                    return element.fileObject.getPath();
                }
                return null;
            }                        
        };
        
        List<PlsqlFileListElement> elements = new ArrayList<PlsqlFileListElement>();

        WindowManager wm = WindowManager.getDefault();
        Set<? extends Mode> modes = wm.getModes();
        for (Mode mode : modes) {
            if (wm.isEditorMode(mode)) {
                TopComponent[] tcs = mode.getTopComponents();
                for (TopComponent tc : tcs) {
                    Lookup lukap = tc.getLookup();
                    FileObject fo = lukap.lookup(FileObject.class);                    
                    if (fo == null) {
                        DataObject dobj = lukap.lookup(DataObject.class);                        
                        if (dobj != null) {
                            fo = dobj.getPrimaryFile();
                        }
                    }
                    if (fo != null && fo.getMIMEType().equals(PlsqlDataLoader.REQUIRED_MIME) && !fo.getExt().equals("tdb")) {
                        if (tc.getHtmlDisplayName() != null) {
                            elements.add(new PlsqlFileListElement(fo, tc.getHtmlDisplayName(), true));
                        } else {
                            elements.add(new PlsqlFileListElement(fo, tc.getName(), false));
                        }   
                    }
                }
            }
        }

        elementsList.setListData(elements.toArray(new PlsqlFileListElement[elements.size()]));
        elementsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        elementsList.addListSelectionListener(this);
        elementsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent (JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (isSelected && value instanceof PlsqlFileListElement && ((PlsqlFileListElement) value).isHtml()) {
                    value = stripHtml(((PlsqlFileListElement) value).toString());
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }

            private String stripHtml (String htmlText) {
                if (null == htmlText) {
                    return null;
                }
                String res = htmlText.replaceAll("<[^>]*>", ""); // NOI18N // NOI18N
                res = res.replaceAll("&nbsp;", " "); // NOI18N // NOI18N
                res = res.trim();
                return res;
            }
        });

        JScrollPane sp = new JScrollPane(elementsList);
        jPanel1.add(sp);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
            Object[] elements = elementsList.getSelectedValues();
            //reinitialize list
            openFiles = new ArrayList<File>();           
            if (elements != null && elements.length > 0) {
                for (int i = 0; i < elements.length; i++) {
                    PlsqlFileListElement el = (PlsqlFileListElement) elements[i];
                    openFiles.add(FileUtil.normalizeFile(FileUtil.toFile(el.fileObject)));                   
                }               
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblOpenFiles = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();

        org.openide.awt.Mnemonics.setLocalizedText(lblOpenFiles, org.openide.util.NbBundle.getMessage(OpenPlsqlFilesSelectorPanel.class, "OpenPlsqlFilesSelectorPanel.lblOpenFiles.text")); // NOI18N

        jPanel1.setLayout(new java.awt.BorderLayout());

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 237, Short.MAX_VALUE)
                    .add(lblOpenFiles))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(lblOpenFiles)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 179, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jPanel1;
    private javax.swing.JLabel lblOpenFiles;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the openFileSet
     */
    public List<File> getOpenFiles() {
        return Collections.unmodifiableList(openFiles);
    }

    
    private static class PlsqlFileListElement {
        FileObject      fileObject;
        String          displayName;
        private final boolean html;

        PlsqlFileListElement(FileObject tc, String displayName, boolean isHtml) {
            this.fileObject = tc;
            this.displayName = displayName;
            this.html = isHtml;
        }

        @Override
        public String toString() {
            return displayName;
        }
        
        public boolean isHtml () {
            return html;
        }
    }
}
