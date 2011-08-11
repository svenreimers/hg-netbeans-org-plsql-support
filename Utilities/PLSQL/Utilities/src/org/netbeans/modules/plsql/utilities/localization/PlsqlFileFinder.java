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
package org.netbeans.modules.plsql.utilities.localization;

import org.netbeans.modules.plsql.utilities.PlsqlFileValidator;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.openide.util.Lookup;

/**
 *
 * @author CORPNET\chrlse
 */
public class PlsqlFileFinder {

   private static final Logger logger = Logger.getLogger(PlsqlFileFinder.class.getName());
   private final File rootFolder;

   public PlsqlFileFinder(final File rootFolder) {
      this.rootFolder = rootFolder;
   }

   public Collection<File> findPlsqlFiles() {
      final List<String> suffixes = new ArrayList<String>();
      final Collection<? extends PlsqlFileValidator> validators = Lookup.getDefault().lookupAll(PlsqlFileValidator.class);
      for (PlsqlFileValidator fileValidator : validators) {
         suffixes.addAll(fileValidator.getAllExt());
      }
      final IOFileFilter fileFilter = new SuffixFileFilter(suffixes, IOCase.SYSTEM);
      final IOFileFilter dirFilter = buildDirFileFilter();
      final String[] extArray = suffixes.toArray(new String[suffixes.size()]);
      final Date startTime = Calendar.getInstance().getTime();
      logger.log(Level.INFO, "file search started looking for extensions: {0}", Arrays.toString(extArray));
      final Collection<File> result = FileUtils.listFiles(rootFolder, fileFilter, dirFilter);
      final Date endTime = Calendar.getInstance().getTime();
      logger.log(Level.INFO, "file search ended, taking {0} ms for {1} files", new Object[]{endTime.getTime() - startTime.getTime(), result.size()});
      return result;
   }

   private IOFileFilter buildDirFileFilter() {
      IOFileFilter nameFileFilter = FileFilterUtils.notFileFilter(new PrefixFileFilter("."));
      nameFileFilter = FileFilterUtils.makeSVNAware(nameFileFilter);
      return nameFileFilter;
   }
}
