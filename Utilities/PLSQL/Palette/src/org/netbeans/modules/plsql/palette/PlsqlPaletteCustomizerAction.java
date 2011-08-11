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
package org.netbeans.modules.plsql.palette;

import java.io.IOException;
import org.openide.ErrorManager;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

/*
 * Class description
 *
 * Created on March 30, 2006, 8:39 AM
 *
 * @author IFS
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
public class PlsqlPaletteCustomizerAction extends CallableSystemAction {
    
    /** holds the name of the menu item which opens the palette manager */ 
    private static String name;
    
    public PlsqlPaletteCustomizerAction () {
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }

    protected boolean asynchronous() {
        return false;
    }

    /** Human presentable name of the action. This should be
     * presented as an item in a menu.
     * @return the name of the action
     */
    public String getName() {
        if (name == null)
            name = NbBundle.getBundle(PlsqlPaletteCustomizerAction.class)
                   .getString("ACT_OpenPlsqlCustomizer"); // NOI18N
        
        return name;
    }

    /** Help context where to find more about the action.
     * @return the help context for this action
     */
    public HelpCtx getHelpCtx() {
        return null;
    }

    /** This method is called by one of the "invokers" as a result of
     * some user's action that should lead to actual "performing" of the action.
     */
    public void performAction() {
        try {
            PlsqlPaletteFactory.getPalette().showCustomizer();
        }
        catch (IOException ioe) {
            ErrorManager.getDefault().notify(ErrorManager.EXCEPTION, ioe);
        }
    }    
}
