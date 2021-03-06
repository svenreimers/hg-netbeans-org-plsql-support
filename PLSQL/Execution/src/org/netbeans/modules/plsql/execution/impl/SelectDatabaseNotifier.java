/*
 *                  IFS Research & Development
 *
 * This program is protected by copyright law and by international
 * conventions. All licensing, renting, lending or copying (including
 * for private use), and all other use of the program, which is not
 * expressively permitted by IFS Research & Development (IFS), is a
 * violation of the rights of IFS. Such violations will be reported to the
 * appropriate authorities.
 *
 * VIOLATIONS OF ANY COPYRIGHT IS PUNISHABLE BY LAW AND CAN LEAD
 * TO UP TO TWO YEARS OF IMPRISONMENT AND LIABILITY TO PAY DAMAGES.
 */
package org.netbeans.modules.plsql.execution.impl;

import org.openide.awt.Notification;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.ImageUtilities;

/**
 * The balloon that is shown when a database is needed.
 *
 * @author chrlse
 */
public class SelectDatabaseNotifier {

    private static Notification notification = null;

    /**
     *
     * @param filename 
     */
    public void notify(final String filename) {
        synchronized (SelectDatabaseNotifier.class) {
            if (notification != null) {
                notification.clear();
                notification = null;
            }

            notification = NotificationDisplayer.getDefault().notify("No Database Connection Selected for " + filename,
                    ImageUtilities.loadImageIcon("org/netbeans/modules/autoupdate/pluginimporter/resources/warning.gif",
                    false), // NOI18N 
                    "Select Connection in Combo box and try again.", null);
        }
    }
}
