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
package org.netbeans.modules.plsql.utilities.validation;

import org.netbeans.modules.plsql.utilities.PlsqlFileValidator;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidatorService;
import java.io.File;
import java.util.Collection;
import java.util.Locale;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author chrlse
 */
@ServiceProvider(service = PlsqlFileValidatorService.class)
public class PlsqlFileValidatorServiceImpl implements PlsqlFileValidatorService {

   @Override
   public boolean isValidPackage(final DataObject dataObject) {
      if (isPlsqlDataObject(dataObject)) {
         return false;
      }
      return isValidPackage(dataObject.getPrimaryFile());
   }

   @Override
   public boolean isValidPackage(final FileObject fileObject) {
      if (fileObject == null) {
         return false;
      }
      Collection<? extends PlsqlFileValidator> validators = Lookup.getDefault().lookupAll(PlsqlFileValidator.class);
      for (PlsqlFileValidator validator : validators) {
         if (validator.validate(fileObject.getExt())) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean isValidPackageSpec(final DataObject dataObject) {
      if (isPlsqlDataObject(dataObject)) {
         return false;
      }
      return isValidPackageSpec(dataObject.getPrimaryFile().getExt());
   }

   @Override
   public boolean isValidPackageSpec(String fileExt) {
      if (fileExt == null || fileExt.isEmpty()) {
         return false;
      }
      Collection<? extends PlsqlFileValidator> validators = Lookup.getDefault().lookupAll(PlsqlFileValidator.class);
      for (PlsqlFileValidator validator : validators) {
         if (validator.validateSpec(fileExt)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean isValidPackageBody(final DataObject dataObject) {
      if (isPlsqlDataObject(dataObject)) {
         return false;
      }
      return isValidPackageBody(dataObject.getPrimaryFile().getExt());
   }

   @Override
   public boolean isValidPackageBody(String fileExt) {
      if (fileExt == null || fileExt.isEmpty()) {
         return false;
      }
      final Collection<? extends PlsqlFileValidator> validators = Lookup.getDefault().lookupAll(PlsqlFileValidator.class);
      for (PlsqlFileValidator validator : validators) {
         if (validator.validateBody(fileExt)) {
            return true;
         }
      }
      return false;
   }

   @Override
   public boolean isValidTDB(DataObject dataObject) {
      if (isPlsqlDataObject(dataObject)) {
         return false;
      }
      FileObject fileObject = dataObject.getPrimaryFile();
      return fileObject.isValid();
   }

   @Override
   public DataObject getSiblingExt(final DataObject sourceDataObject) {
      if (isPlsqlDataObject(sourceDataObject)) {
         return null;
      }
      final FileObject fileObject = sourceDataObject.getPrimaryFile();
      String fileName = FileUtil.getFileDisplayName(fileObject);

      final int last = fileName.lastIndexOf('.');
      if (last != -1) {
         fileName = fileName.substring(0, (last + 1)) + getSiblingExt(fileObject.getExt());
      }

      final File file = new File(fileName);
      if (file.exists()) {
         try {
            return DataFolder.find(FileUtil.toFileObject(file));
         } catch (DataObjectNotFoundException ex) {
            Exceptions.printStackTrace(ex);
         }
      }
      return null;
   }

   @Override
   public String getSiblingExt(final String siblingExt) {
      final Collection<? extends PlsqlFileValidator> validators = Lookup.getDefault().lookupAll(PlsqlFileValidator.class);
      for (PlsqlFileValidator validator : validators) {
         final String extForSibling = validator.getExtForSibling(siblingExt.toLowerCase(Locale.ENGLISH));
         if (extForSibling != null) {
            return extForSibling;
         }
      }
      return null;
   }

   @Override
   public String getDefaultPackageBodyExt() {
      final Collection<? extends PlsqlFileValidator> validators = Lookup.getDefault().lookupAll(PlsqlFileValidator.class);
      for (PlsqlFileValidator validator : validators) {
         if (validator instanceof PlsqlDefaultProvider) {
            return validator.getPackageBodyExt();
         }
      }
      return null;
   }

   @Override
   public String getDefaultPackageSpecExt() {
      final Collection<? extends PlsqlFileValidator> validators = Lookup.getDefault().lookupAll(PlsqlFileValidator.class);
      for (PlsqlFileValidator validator : validators) {
         if (validator instanceof PlsqlDefaultProvider) {
            return validator.getPackageExt();
         }
      }
      return null;
   }

   @Override
   public boolean isValidPackageDefault(DataObject dataObject) {
      if (isPlsqlDataObject(dataObject)) {
         return false;
      }
      String ext = dataObject.getPrimaryFile().getExt();
      if (getDefaultPackageBodyExt().equalsIgnoreCase(ext)) {
         return true;
      }
      if (getDefaultPackageSpecExt().equalsIgnoreCase(ext)) {
         return true;
      }
      return false;
   }

   private boolean isPlsqlDataObject(final DataObject dataObject) {
      return dataObject == null || !"org.netbeans.modules.plsql.filetype.PlsqlDataObject".equals(dataObject.getLoader().getRepresentationClassName().trim());
   }
}
