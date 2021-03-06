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
package org.netbeans.modules.plsql.execution;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author  YaDhLK
 */
public class PromptDialog extends javax.swing.JDialog {

   static final Map<String, String> previousValues = new HashMap<String, String>();
   String variableName = null;

   /** Creates new form PromptValue */
   public PromptDialog(java.awt.Frame parent, String variable, boolean modal) {
      super(parent, modal);
      initComponents();
      getRootPane().setDefaultButton(okButton);
      this.setLocation(325, 125);
      this.setTitle("Enter Value");
      this.lblPrompt.setText("Enter value for " + variable);
      this.variableName = variable;
      if(previousValues.containsKey(variable))
         fieldSubstitutionValue.setText(previousValues.get(variable));
   }
   
    /**
    * Return the value of the text field
    * @return
    */
   public String getValue() {
      String value = fieldSubstitutionValue.getText();
      previousValues.put(this.variableName, value);
      return value;
   }
   
   /** This method is called from within the constructor to
    * initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is
    * always regenerated by the Form Editor.
    */
   // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
   private void initComponents() {

      okButton = new javax.swing.JButton();
      lblPrompt = new javax.swing.JLabel();
      fieldSubstitutionValue = new javax.swing.JTextField();

      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

      okButton.setText(org.openide.util.NbBundle.getMessage(PromptDialog.class, "PromptDialog.okButton.text")); // NOI18N
      okButton.addActionListener(new java.awt.event.ActionListener() {
         public void actionPerformed(java.awt.event.ActionEvent evt) {
            okButtonActionPerformed(evt);
         }
      });

      lblPrompt.setText(org.openide.util.NbBundle.getMessage(PromptDialog.class, "PromptDialog.lblPrompt.text")); // NOI18N

      fieldSubstitutionValue.setText(org.openide.util.NbBundle.getMessage(PromptDialog.class, "PromptDialog.fieldSubstitutionValue.text")); // NOI18N

      org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
      getContentPane().setLayout(layout);
      layout.setHorizontalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(layout.createSequentialGroup()
            .add(26, 26, 26)
            .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
               .add(okButton)
               .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                  .add(fieldSubstitutionValue)
                  .add(lblPrompt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)))
            .addContainerGap(28, Short.MAX_VALUE))
      );
      layout.setVerticalGroup(
         layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
         .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
            .add(21, 21, 21)
            .add(lblPrompt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(fieldSubstitutionValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
            .add(okButton)
            .add(16, 16, 16))
      );

      pack();
   }// </editor-fold>//GEN-END:initComponents

   private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
      // TODO add your handling code here:
      this.setVisible(false);
      this.dispose();
}//GEN-LAST:event_okButtonActionPerformed
  
  
   // Variables declaration - do not modify//GEN-BEGIN:variables
   private javax.swing.JTextField fieldSubstitutionValue;
   private javax.swing.JLabel lblPrompt;
   private javax.swing.JButton okButton;
   // End of variables declaration//GEN-END:variables
   
}
