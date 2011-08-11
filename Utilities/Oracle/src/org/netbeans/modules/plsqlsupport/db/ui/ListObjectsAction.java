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
package org.netbeans.modules.plsqlsupport.db.ui;

import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import javax.swing.*;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.CacheDirectoryProvider;
import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.util.actions.CookieAction;

public abstract class ListObjectsAction extends CookieAction {

   public ListObjectsAction() {}

   protected void performAction(Node[] activatedNodes) {
      Project project = activatedNodes[0].getLookup().lookup(Project.class);
      DatabaseConnectionManager connectionProvider = project.getLookup().lookup(DatabaseConnectionManager.class);
      DatabaseContentManager cache = DatabaseContentManager.getInstance(connectionProvider.getTemplateConnection());
      if (cache.isUpdateRunning()) {
         JOptionPane.showMessageDialog(null, "Wait until database cache update finishes.", "Cache update in progress", JOptionPane.OK_OPTION);
         return;
      }
      try {
         //Getting time to the nearest minute
         DateFormat format = new SimpleDateFormat("yyyy/MM/dd:HH:mm");
         Date timeStamp = format.parse(cache.getLastPackageSyncTime());

         long lastModified = timeStamp.getTime();
         boolean fileCreated = false;
         FileObject cacheDirectory = project.getLookup().lookup(CacheDirectoryProvider.class).getCacheDirectory();
         File file = new File(cacheDirectory.getPath(), getTemporaryFilename());
         if (file.exists()) {
            if (file.lastModified() != lastModified) {
               file.delete();
               fileCreated = file.createNewFile();
            }
         } else
            fileCreated = file.createNewFile();

         boolean canOpen = true;
         if (fileCreated) {
            List<String> objects = new ArrayList<String>(getObjects(cache));
            Collections.sort(objects);
            Object[] objectsArray = objects.toArray();
            canOpen = writeObjectsToDisk(file, objectsArray);
            if (lastModified > 0)
               file.setLastModified(lastModified);
            file.setReadOnly();
         }
         if (canOpen) {
            FileObject fileObject = FileUtil.toFileObject(file);
            if (fileObject != null) {
               DataObject dataObject = DataFolder.find(fileObject);
               OpenCookie openCookie = dataObject.getCookie(OpenCookie.class);
               if (openCookie != null)
                  openCookie.open();
            }
         }
      } catch (ParseException ex) {
         Exceptions.printStackTrace(ex);
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
      }
   }

   protected abstract Collection<String> getObjects(DatabaseContentManager cache);

   protected abstract String getTemporaryFilename();

   @Override
   protected void initialize() {
      super.initialize();
      putValue("noIconInMenu", Boolean.TRUE);
   }

   public HelpCtx getHelpCtx() {
      return HelpCtx.DEFAULT_HELP;
   }

   @Override
   protected boolean asynchronous() {
      return false;
   }

   @Override
   protected int mode() {
      return CookieAction.MODE_EXACTLY_ONE;
   }

   @Override
   protected Class<?>[] cookieClasses() {
      return new Class[] {Project.class};
   }

   @Override
   protected boolean enable(Node[] activatedNodes) {
      if (!super.enable(activatedNodes))
         return false;
      Project project = activatedNodes[0].getLookup().lookup(Project.class);
      DatabaseConnectionManager provider = project.getLookup().lookup(DatabaseConnectionManager.class);
      return provider != null && provider.hasConnection() && provider.isOnline();
   }

   //return true if written to a file, false otherwise
   private static boolean writeObjectsToDisk(File f, Object[] objArray) {
      if (!f.exists())
         return false;
      FileOutputStream writer = null;
      BufferedOutputStream out = null;
      try {
         writer = new FileOutputStream(f);
         out = new BufferedOutputStream(writer);
         StringBuilder sb = new StringBuilder();
         for (int i = 0; i < objArray.length; i++) {
            sb.append((String) objArray[i] + "\n");
         }
         out.write((new String(sb)).getBytes());
         out.flush();
      } catch (IOException ex) {
         Exceptions.printStackTrace(ex);
         return false;
      } finally {
         try {
            if (out != null) {
               out.close();
            }
            if (writer != null) {
               writer.close();
            }
         } catch (IOException e) {
            Exceptions.printStackTrace(e);
         }
      }
      return true;
   }

   public static class Packages extends ListObjectsAction {

      public Packages() {}

      public String getName() {
         return NbBundle.getMessage(ListObjectsAction.class, "CTL_ShowAllDbPackagesAction");
      }

      @Override
      protected Collection<String> getObjects(DatabaseContentManager cache) {
         return cache.getAllPackages();
      }

      @Override
      protected String getTemporaryFilename() {
         return "DatabasePackages.dbl";
      }
   }

   public static class Tables extends ListObjectsAction {

      public Tables() {}

      public String getName() {
         return NbBundle.getMessage(ListObjectsAction.class, "CTL_ShowAllDbTablesAction");
      }

      @Override
      protected Collection<String> getObjects(DatabaseContentManager cache) {
         return cache.getAllTables();
      }

      @Override
      protected String getTemporaryFilename() {
         return "DatabaseTables.dbl";
      }
   }

   public static class Views extends ListObjectsAction {

      public Views() {}

      public String getName() {
         return NbBundle.getMessage(ListObjectsAction.class, "CTL_ShowAllDbViewsAction");
      }

      @Override
      protected Collection<String> getObjects(DatabaseContentManager cache) {
         return cache.getAllViews();
      }

      @Override
      protected String getTemporaryFilename() {
         return "DatabaseViews.dbl";
      }
   }
}
