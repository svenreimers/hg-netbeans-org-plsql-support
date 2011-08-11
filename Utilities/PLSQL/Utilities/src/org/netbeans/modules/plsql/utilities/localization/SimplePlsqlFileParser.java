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

import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.PlsqlSearchObject;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author chrlse
 */
public class SimplePlsqlFileParser implements Serializable {

   private static final long serialVersionUID = 1L;
   private static final Logger logger = Logger.getLogger(SimplePlsqlFileParser.class.getName());
   private static SimplePlsqlFileParser instance;

   SimplePlsqlFileParser() {
   }

   public static SimplePlsqlFileParser getInstance() {
      if (instance == null) {
         instance = new SimplePlsqlFileParser();
      }
      return instance;
   }

   public List<PlsqlSearchObject> searchInFile(final FileObject fileObject) throws IOException {
      final LineIterator lines = FileUtils.lineIterator(FileUtil.toFile(fileObject), "UTF-8");
      final Map<String, String> defines = new HashMap<String, String>();
      final List<PlsqlSearchObject> searchObjects = new ArrayList<PlsqlSearchObject>();
      try {
         while (lines.hasNext()) {
            final String line = lines.nextLine();
            if (line.startsWith("DEFINE ")) {
               addToDefineMap(line, defines);
            }
            if (line.startsWith("CREATE ")) {
               try {
                  final PlsqlSearchObject key = parseCreateLine(line, defines);
                  logger.log(Level.FINE, "fileObjects.put({0}, {1}", new Object[]{key.toString(), fileObject.getName()});
                  searchObjects.add(key);
               } catch (IllegalArgumentException e) { //NOPMD
                  //just don't add when PlsqlBlockType is wrong
               }
            }
         }
      } finally {
         LineIterator.closeQuietly(lines);
      }
      return searchObjects;
   }

   private PlsqlSearchObject parseCreateLine(final String line, final Map<String, String> defines) {
      line.lastIndexOf("CREATE OR REPLACE ");
      final StringTokenizer tokens = new StringTokenizer(line);
      tokens.nextToken();
      String typeCandidate = tokens.nextToken();
      if (typeCandidate.equalsIgnoreCase("OR")) {
         tokens.nextToken();
         typeCandidate = tokens.nextToken();
      }
      String type = typeCandidate;
      String objectName = tokens.nextToken();
      if (objectName.equalsIgnoreCase("BODY")) {
         type = "PACKAGE_BODY";
         objectName = tokens.nextToken();
      }
      if (objectName.startsWith("&")) { //NOPMD
         objectName = defines.get(objectName.substring(1));
      }
      PlsqlBlockType blockType = null;
      blockType = PlsqlBlockType.valueOf(type);
      return new PlsqlSearchObject(blockType, objectName);
   }

   private void addToDefineMap(final String line, final Map<String, String> defines) {
      final StringTokenizer tokens = new StringTokenizer(line, " =");
      tokens.nextToken();
      final String defKey = tokens.nextToken();
      final String defValue = tokens.nextToken();
      defines.put(defKey, defValue);
   }
}
