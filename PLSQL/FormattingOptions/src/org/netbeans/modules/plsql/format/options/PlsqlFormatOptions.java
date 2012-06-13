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
package org.netbeans.modules.plsql.format.options;

import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.editor.settings.SimpleValueNames;
import org.netbeans.modules.options.editor.spi.PreferencesCustomizer;
import org.netbeans.modules.options.editor.spi.PreviewProvider;
import org.openide.text.CloneableEditorSupport;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 * An panel for Options->Editor->Formatting to add 'Indents Only'
 * @author YADHLK
 */
public class PlsqlFormatOptions {

    public static final String expandTabToSpaces = SimpleValueNames.EXPAND_TABS;
    public static final String tabSize = SimpleValueNames.TAB_SIZE;
    public static final String spacesPerTab = SimpleValueNames.SPACES_PER_TAB;
    public static final String indentSize = SimpleValueNames.INDENT_SHIFT_WIDTH;
    public static final String autoIndent = "autoIndent"; //NOI18N
    public static final String autoUppercase = "autoUppercase";

    private PlsqlFormatOptions() {
    }

    public static int getDefaultAsInt(String key) {
        return Integer.parseInt(defaults.get(key));
    }

    public static boolean getDefaultAsBoolean(String key) {
        return Boolean.parseBoolean(defaults.get(key));
    }

    public static String getDefaultAsString(String key) {
        return defaults.get(key);
    }

    public static boolean isInteger(String optionID) {
        String value = defaults.get(optionID);

        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException numberFormatException) {
            numberFormatException.printStackTrace();
            return false;
        }
    }
    private static final String TRUE = "true";      // NOI18N
    private static final String FALSE = "false";    // NOI18N    
    private static Map<String, String> defaults;


    static {
        createDefaults();
    }

    private static void createDefaults() {
        String defaultValues[][] = {
            {expandTabToSpaces, TRUE}, //NOI18N
            {tabSize, "3"}, //NOI18N
            {spacesPerTab, "3"}, //NOI18N
            {indentSize, "3"}, //NOI18N
            {autoIndent, TRUE}, //NOI18N
            {autoUppercase, TRUE}, //NOI18N
        };

        defaults = new HashMap<String, String>();

        for (java.lang.String[] strings : defaultValues) {
            defaults.put(strings[0], strings[1]);
        }

    }

    // Support section ---------------------------------------------------------
    public static class CategorySupport implements ActionListener, DocumentListener, PreviewProvider, PreferencesCustomizer {

        public static final String OPTION_ID = "org.netbeans.modules.java.ui.FormatingOptions.ID";
        private static final int LOAD = 0;
        private static final int STORE = 1;
        private static final int ADD_LISTENERS = 2;
        private final String previewText;
        private final String id;
        protected final JPanel panel;
        private final List<JComponent> components = new LinkedList<JComponent>();
        private JEditorPane previewPane;
        private final Preferences preferences;
        private final Preferences previewPrefs;

        protected CategorySupport(Preferences preferences, String id, JPanel panel, String previewText) throws IOException {
            
            this.preferences = preferences;
            this.id = id;
            this.panel = panel;
            this.previewText =loadPreviewText(getClass().getClassLoader().getResourceAsStream("org/netbeans/modules/plsql/format/options/IndentationExample")); //NOI18N

            // Scan the panel for its components
            scan(panel, components);
            this.previewPrefs = preferences;

            // Load and hook up all the components
            loadFrom(preferences);
            addListeners();
        }

        protected void addListeners() {
            scan(ADD_LISTENERS, null);
        }

        protected void loadFrom(Preferences preferences) {
            scan(LOAD, preferences);
        }

        protected void storeTo(Preferences p) {
            scan(STORE, p);
        }

        protected void notifyChanged() {
            storeTo(preferences);
            refreshPreview();
        }

        // ActionListener implementation ---------------------------------------
        public void actionPerformed(ActionEvent e) {
            notifyChanged();
        }

        // DocumentListener implementation -------------------------------------
        public void insertUpdate(DocumentEvent e) {
            notifyChanged();
        }

        public void removeUpdate(DocumentEvent e) {
            notifyChanged();
        }

        public void changedUpdate(DocumentEvent e) {
            notifyChanged();
        }
        
        private static String loadPreviewText(InputStream is) throws IOException {
            BufferedReader r = new BufferedReader(new InputStreamReader(is));
            try {
                StringBuilder sb = new StringBuilder();
                for (String line = r.readLine(); line != null; line = r.readLine()) {
                    sb.append(line).append('\n'); 
                }
                return sb.toString();
            } finally {
                r.close();
            }
        }

        @Override
        public JComponent getPreviewComponent() {
            if (previewPane == null) {
                previewPane = new JEditorPane();
                previewPane.getAccessibleContext().setAccessibleName(NbBundle.getMessage(PlsqlFormatOptions.class, "AN_Preview")); //NOI18N
                previewPane.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(PlsqlFormatOptions.class, "AD_Preview")); //NOI18N
                previewPane.putClientProperty("HighlightsLayerIncludes", "^org\\.netbeans\\.modules\\.editor\\.lib2\\.highlighting\\.SyntaxHighlighting$"); //NOI18N
                previewPane.setEditorKit(CloneableEditorSupport.getEditorKit("text/x-plsql"));
                previewPane.setEditable(false);
                previewPane.setText(previewText);
            }
            return previewPane;
        }

        public void refreshPreview() {
            JEditorPane jep = (JEditorPane) getPreviewComponent();
            jep.setIgnoreRepaint(true);
            jep.setIgnoreRepaint(false);
            jep.scrollRectToVisible(new Rectangle(0, 0, 10, 10));
            jep.repaint(100);
        }

        // PreferencesCustomizer implementation --------------------------------
        public JComponent getComponent() {
            return panel;
        }

        public String getDisplayName() {
            return panel.getName();
        }

        public String getId() {
            return id;
        }

        public HelpCtx getHelpCtx() {
            return null;
        }

        // PreferencesCustomizer.Factory implementation ------------------------
        public static final class Factory implements PreferencesCustomizer.Factory {

            private final String id;
            private final Class<? extends JPanel> panelClass;
            private final String previewText;

            public Factory(String id, Class<? extends JPanel> panelClass, String previewText) {
                this.id = id;
                this.panelClass = panelClass;
                this.previewText = previewText;
            }

            public PreferencesCustomizer create(Preferences preferences) {
                try {
                    return new CategorySupport(preferences, id, panelClass.newInstance(), previewText);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        } // End of CategorySupport.Factory class

        // Private methods -----------------------------------------------------
        private void performOperation(int operation, JComponent jc, String optionID, Preferences p) {
            switch (operation) {
                case LOAD:
                    loadData(jc, optionID, p);
                    break;
                case STORE:
                    storeData(jc, optionID, p);
                    break;
                case ADD_LISTENERS:
                    addListener(jc);
                    break;
            }
        }

        private void scan(int what, Preferences p) {
            for (JComponent jc : components) {
                Object o = jc.getClientProperty(OPTION_ID);
                if (o instanceof String) {
                    performOperation(what, jc, (String) o, p);
                } else if (o instanceof String[]) {
                    for (String oid : (String[]) o) {
                        performOperation(what, jc, oid, p);
                    }
                }
            }
        }

        private void scan(Container container, List<JComponent> components) {
            for (Component c : container.getComponents()) {
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    Object o = jc.getClientProperty(OPTION_ID);
                    if (o instanceof String || o instanceof String[]) {
                        components.add(jc);
                    }
                }
                if (c instanceof Container) {
                    scan((Container) c, components);
                }
            }
        }

        /** Very smart method which tries to set the values in the components correctly
         */
        private void loadData(JComponent jc, String optionID, Preferences node) {

            if (jc instanceof JTextField) {
                JTextField field = (JTextField) jc;
                field.setText(node.get(optionID, getDefaultAsString(optionID)));
            } else if (jc instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) jc;
                boolean df = getDefaultAsBoolean(optionID);
                checkBox.setSelected(node.getBoolean(optionID, df));
            }
        }

        private void storeData(JComponent jc, String optionID, Preferences node) {
            if (jc instanceof JTextField) {
                JTextField field = (JTextField) jc;

                String text = field.getText();

                // XXX test for numbers
                if (isInteger(optionID)) {
                    try {
                        int i = Integer.parseInt(text);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                node.put(optionID, text);
            } else if (jc instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) jc;
                node.putBoolean(optionID, checkBox.isSelected());
            }
        }

        private void addListener(JComponent jc) {
            if (jc instanceof JTextField) {
                JTextField field = (JTextField) jc;
                field.addActionListener(this);
                field.getDocument().addDocumentListener(this);
            } else if (jc instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) jc;
                checkBox.addActionListener(this);
            } else if (jc instanceof JComboBox) {
                JComboBox cb = (JComboBox) jc;
                cb.addActionListener(this);
            }
        }
    }

    public static class PreviewPreferences extends AbstractPreferences {

        private Map<String, Object> map = new HashMap<String, Object>();

        public PreviewPreferences() {
            super(null, ""); // NOI18N
        }

        protected void putSpi(String key, String value) {
            map.put(key, value);
        }

        protected String getSpi(String key) {
            return (String) map.get(key);
        }

        protected void removeSpi(String key) {
            map.remove(key);
        }

        protected void removeNodeSpi() throws BackingStoreException {
        }

        protected String[] keysSpi() throws BackingStoreException {
            String array[] = new String[map.keySet().size()];
            return map.keySet().toArray(array);
        }

        protected String[] childrenNamesSpi() throws BackingStoreException {
            return new String[0];
        }

        protected AbstractPreferences childSpi(String name) {
            return null;
        }

        protected void syncSpi() throws BackingStoreException {
        }

        protected void flushSpi() throws BackingStoreException {
        }
    }
}
