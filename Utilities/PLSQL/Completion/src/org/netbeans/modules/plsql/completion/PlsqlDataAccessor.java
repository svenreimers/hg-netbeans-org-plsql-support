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
package org.netbeans.modules.plsql.completion;

import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.util.Locale;
import java.util.StringTokenizer;
import org.openide.util.Exceptions;

/**
 *
 * @author chawlk
 */
public class PlsqlDataAccessor {

   //This method will be called with methodBody from files as well as from the database and may contain syntax errors
   //We need to handle this and not assume that the methodBody is properly formated.
   public static String formatMethodDoc(String packageName, String methodName, String methodBody, String owner, Connection connection) {
      StringBuilder documentation = new StringBuilder();
      String returnType = null;
      if (methodBody.substring(0, 1).equalsIgnoreCase("F")) {
         appendKeyWord(documentation, "FUNCTION");
      } else {
         appendKeyWord(documentation, "PROCEDURE");
      }
      documentation.append("&nbsp;<b>").append(methodName).append("</b>");

      int paramStart = methodBody.indexOf('(');
      int paramEnd = methodBody.lastIndexOf(')');
      int returnPos = methodBody.lastIndexOf("RETURN");
      if (returnPos > paramEnd) {
         returnType = methodBody.substring(returnPos + 7).trim();
         int pos = returnType.indexOf(";");
         if (pos > 0) {
            returnType = returnType.substring(0, pos);
         }
      }
      if (paramStart > paramEnd) {
         documentation.append("<font color=\"red\"><br>Syntax error - could not find ) after last parameter</font>");
      }
      String errorTxt = null;
      if (paramStart > -1 && paramEnd > -1) {
         documentation.append("&nbsp;(<br>");
         documentation.append("<table cellpadding=\"0\" cellspacing = \"3\"><tr><td>&nbsp;&nbsp;&nbsp;</td><td>");
         String parameters = methodBody.substring(paramStart + 1, paramEnd).trim();
         StringTokenizer tokens = new StringTokenizer(parameters, ",");
         while (tokens.hasMoreTokens()) {
            StringTokenizer parts = new StringTokenizer(tokens.nextToken().trim(), " ");
            //arg IN/OUT TYPE DEFAULT 20
            String name = parts.nextToken();
            String direction = null;
            if (!parts.hasMoreTokens()) { //syntax error
               errorTxt = "<font color=\"red\"><br>Syntax error - expected direction and type after parameter \"" + name + "\"</font>";
            } else {
               direction = parts.nextToken();
            }
            String type = null;
            if (parts.hasMoreTokens()) { //parameters defined without direction are IN parameters
               type = parts.nextToken();
            } else if (errorTxt == null) {
               if (direction.equalsIgnoreCase("IN") || direction.equalsIgnoreCase("OUT")) { //syntax error
                  errorTxt = "<font color=\"red\"><br>Syntax error - no type given for parameter \"" + name + "\"</font>";
               }
               type = direction;
               direction = "IN";
            }

            if (errorTxt == null && "OUT".equalsIgnoreCase(type)) {
               direction = "IN OUT";
               if (!parts.hasMoreTokens()) { //syntax error
                  errorTxt = "<font color=\"red\"><br>Syntax error - no type given for parameter \"" + name + "\"</font>";
               } else {
                  type = parts.nextToken();
               }
            }
            documentation.append(name.toLowerCase(Locale.ENGLISH));
            documentation.append("</td><td>&nbsp;&nbsp;&nbsp;");
            if (errorTxt == null) {
               appendKeyWord(documentation, direction);
            }
            documentation.append("</td><td>&nbsp;&nbsp;&nbsp;");
            if (errorTxt != null) {
               break;
            }
            int assignPos = type.indexOf(":=");
            String assignValue = null;
            if (assignPos > 0) {
               assignValue = type.substring(assignPos + 2).trim();
               type = type.substring(0, assignPos);
            }
            append(documentation, type);

            if (assignValue != null) {
               documentation.append(" := ");
               append(documentation, assignValue);
            }

            if (parts.hasMoreTokens()) {
               String next = parts.nextToken();
               if (next.equalsIgnoreCase("DEFAULT")) {
                  if (parts.hasMoreTokens()) {

                     appendKeyWord(documentation, "&nbsp;DEFAULT&nbsp;");
                     String defaulting = parts.nextToken();
                     append(documentation, defaulting);
                  }
               } else {
                  append(documentation, next);
               }
               while (parts.hasMoreTokens()) {
                  String value = parts.nextToken();
                  documentation.append(" ");
                  append(documentation, value);
               }
            }

            if (tokens.hasMoreTokens()) {
               documentation.append(",</td>");
               if (returnType != null) { //add empty column for return statement
                  documentation.append("<td></td>");
               }
               documentation.append("</tr><tr><td>&nbsp;&nbsp;&nbsp;</td><td>");
            }
         }
      }
      if (returnType != null) {
         if (paramStart > -1) {
            documentation.append("&nbsp;)</td><td>");
         }
         appendKeyWord(documentation, "&nbsp;&nbsp;RETURN&nbsp;&nbsp;");
         append(documentation, returnType);
         documentation.append(";");
      } else if (paramStart > -1 && paramEnd > -1) {
         documentation.append("&nbsp;);</td><td>");
      }

      if (paramStart > -1 && paramEnd > -1) {
         documentation.append("</td></tr></table>");
      }
      if (errorTxt != null) {
         documentation.append(errorTxt).append("<br>");
      } else if (packageName != null && owner != null) {
         String dbDoc = "";
         if (dbDoc.length() > 0) {
            documentation.append("<br>").append(dbDoc);
         }
      }

      return documentation.toString();
   }


   private static StringBuilder append(StringBuilder doc, String str) {
      if (isKeyWord(str)) {
         appendKeyWord(doc, str.toUpperCase(Locale.ENGLISH));
      } else {
         doc.append(str);
      }
      return doc;
   }
   private static StringBuilder appendKeyWord(StringBuilder doc, String keyword) {
      doc.append("<font color=\"blue\">");
      doc.append(keyword.toUpperCase(Locale.ENGLISH));
      doc.append("</font>");
      return doc;
   }

   private static boolean isKeyWord(String str) {
      str = str.toUpperCase(Locale.ENGLISH);
      return (str.equals("VARCHAR2") ||
            str.equals("BOOLEAN") ||
            str.equals("DATE") ||
            str.equals("NUMBER") ||
            str.equals("TRUE") ||
            str.equals("FALSE") ||
            str.equals("RAW") ||
            str.equals("LONG") ||
            str.equals("BLOB") ||
            str.equals("CLOB") ||
            str.equals("INTEGER") ||
            str.equals("VARCHAR") ||
            str.equals("CHAR") ||
            str.equals("NULL"));
   }

   /**
     * Convert specified name to IFS PL/SQL Syntax.
     * @param name upper case string to be converted to mixed "IFS" case
     * @return the specified string converted to mixed case
     */
    public static String formatPlsqlName(String name) {
        int len = name.length();
        StringBuilder buf = new StringBuilder(len);
        boolean upper = true;

        name = name.toUpperCase(Locale.ENGLISH);
        String suffix = null;
        if (name.endsWith("_API")) {
            suffix = "_API";
            len = len - 4;
        } else if (name.endsWith("_SYS")) {
            suffix = "_SYS";
            len = len - 4;
        } else if (name.endsWith("_RPI")) {
            suffix = "_RPI";
            len = len - 4;
        }

        for (int i = 0; i < len; i++) {
            char ch = name.charAt(i);
            if (ch == '_') {
                upper = true;
            } else if (upper) {
                ch = Character.toUpperCase(ch);
                upper = false;
            } else {
                ch = Character.toLowerCase(ch);
            }

            buf.append(ch);
        }

        if (suffix != null) {
            buf.append(suffix);
        }
        return buf.toString();
    }

    public static String[] getMetaData(Connection connection, String sqlStmt) {
        try {            
            if (connection != null) {
                PreparedStatement pstmt = connection.prepareStatement(sqlStmt);
                pstmt.setFetchSize(1);
                pstmt.execute();
                ResultSetMetaData rsMetaData = pstmt.getMetaData();
                if (rsMetaData != null) {
                    int colCount = rsMetaData.getColumnCount();
                    String[] columns = new String[colCount];
                    for (int i = 0; i < colCount; i++) {
                        columns[i] = rsMetaData.getColumnName(i + 1);
                    }

                    return columns;
                }
            }
        } catch (SQLException ex) {
            Exceptions.printStackTrace(ex);
        }
        return null;
    }
}
