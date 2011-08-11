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

import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlFileValidator;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author chrlse
 */
//@ServiceProviders(value = {
//   @ServiceProvider(service = PlsqlFileValidator.class),
//   @ServiceProvider(service = PlsqlFileLocator.class)})
@ServiceProvider(service = PlsqlFileValidator.class)
public class PlsqlFileValidatorImpl implements PlsqlFileValidator, PlsqlDefaultProvider {

   private static final String BODY = "body";
   private static final String SPEC = "spec";
   private final Map<String, String> specExtensions = new LinkedHashMap<String, String>();
   private final Map<String, String> bodyExtensions = new LinkedHashMap<String, String>();

   public PlsqlFileValidatorImpl() {
      specExtensions.put(SPEC, BODY);
      bodyExtensions.put(BODY, SPEC);
   }

   @Override
   public boolean validate(final String extension) {
      return validateSpec(extension) || validateBody(extension);
   }

   @Override
   public boolean validateSpec(final String extension) {
      return specExtensions.keySet().contains(extension.toLowerCase(Locale.ENGLISH));
   }

   @Override
   public boolean validateBody(final String extension) {
      return bodyExtensions.keySet().contains(extension.toLowerCase(Locale.ENGLISH));
   }

   @Override
   public String getSpecExt(final String bodyExt) {
      return bodyExtensions.get(bodyExt);
   }

   @Override
   public String getBodyExt(final String specExt) {
      return specExtensions.get(specExt);
   }

   @Override
   public String getExtForSibling(final String siblingExt) {
      if (specExtensions.containsKey(siblingExt)) {
         return specExtensions.get(siblingExt);
      }
      return bodyExtensions.get(siblingExt);
   }

   @Override
   public String getPackageBodyExt() {
      return BODY;
   }

   @Override
   public String getPackageExt() {
      return SPEC;
   }

   public String formatFileName(final DataObject dataObject, String objectName, PlsqlBlockType plsqlBlockType) {
      String fileName = null;
      if (dataObject != null) {
         final String ext = getFileExt(plsqlBlockType);

         final FileObject fileObject = dataObject.getPrimaryFile();
         final String currentFileName = FileUtil.getFileDisplayName(fileObject);
         final int last = currentFileName.lastIndexOf(File.separator);
         //try current directory first
         if (last != -1) {
            fileName = currentFileName.substring(0, (last + 1)) + objectName + "." + ext;
         }
      }
      return fileName;
   }

   @Override
   public String getFileExt(PlsqlBlockType plsqlBlockType) {
      String result;
      switch (plsqlBlockType) {
         case PACKAGE:
            result = getPackageExt();
            break;
         case PACKAGE_BODY:
            result = getPackageBodyExt();
            break;
         case VIEW:
            result = getPackageBodyExt();
            break;
         default:
            throw new AssertionError();
      }
      return result;
   }

   @Override
   public Collection<String> getAllExt() {
      ArrayList<String> list = new ArrayList<String>();
      list.add(BODY);
      list.add(SPEC);
      return list;
   }
}
