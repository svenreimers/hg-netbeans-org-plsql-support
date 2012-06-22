/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.navigator;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.utilities.PlsqlFileLocatorService;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;
import org.openide.cookies.OpenCookie;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public class PlsqlSearchProvider implements SearchProvider {

    final static String MODEL_DIRECTORY_PATH = "#COMPONENT#" + File.separator + "database" + File.separator + "#COMPONENT#";
    private final PlsqlFileLocatorService locatorService = Lookup.getDefault().lookup(PlsqlFileLocatorService.class);

    /**
     * Method is called by infrastructure when search operation was requested. Implementors should evaluate given
     * request and fill response object with appropriate results
     *
     * @param request Search request object that contains information what to search for
     * @param response Search response object that stores search results. Note that it's important to react to return
     * value of SearchResponse.addResult(...) method and stop computation if false value is returned.
     */
    @Override
    public void evaluate(SearchRequest request, SearchResponse response) {
        Project[] projects = OpenProjects.getDefault().getOpenProjects();

        String query = request.getText().toLowerCase();
        boolean useRegExp = query.contains("*");
        if (useRegExp) {
            query = query.replaceAll("\\.", "\\.");
            query = query.replaceAll("\\*", ".*");
        }

        for (Project project : projects) {
            //For Local Files
            Collection<File> allObjects = locatorService.getAllPlsqlFiles(project);
            for (File fileObj : allObjects) {
                String name = fileObj.getName().toLowerCase().substring(0, fileObj.getName().indexOf("."));
                boolean match = useRegExp ? Pattern.matches(query, name) : name.contains(query);
                if (match) {
                    if (!response.addResult(new OpenLocalPlsqlFile(fileObj.getPath()), fileObj.getName() + "(" + project.getProjectDirectory().getName() + ")")) {
                        return;
                    }
                }
            }

            //for Package files in DB. Do not consider tables, views, etc
            Set<String> DBFiles = getPlsqlFilesFromDB(project);
            for (String DBFile : DBFiles) {
                boolean match = useRegExp ? Pattern.matches(query, DBFile.toLowerCase()) : DBFile.toLowerCase().contains(query);
                if (match) {
                    if (!response.addResult(new OpenPlsqlFileFromDB(DBFile, project), DBFile.toLowerCase() + " (" + project.getProjectDirectory().getName() + ")")) {
                        return;
                    }
                }
            }
        }
    }

    private Set<String> getPlsqlFilesFromDB(Project project) {
        DatabaseConnectionManager connectionProvider = project.getLookup().lookup(DatabaseConnectionManager.class);
        DatabaseContentManager cache = DatabaseContentManager.getInstance(connectionProvider.getTemplateConnection());
        Set<String> allPackages = new HashSet<String>();
        if (cache != null) {
            allPackages = (Set<String>) cache.getAllPackages();
        }
        return allPackages;
    }

    private static class OpenLocalPlsqlFile implements Runnable {

        String filePath;

        public OpenLocalPlsqlFile(String filePath_) {
            filePath = filePath_;
        }

        @Override
        public void run() {
            DataObject dataObj = PlsqlFileUtil.getDataObject(filePath);
            if (dataObj != null) {
                OpenCookie openCookie = dataObj.getCookie(OpenCookie.class);
                openCookie.open();
            }
        }
    }

    private static class OpenPlsqlFileFromDB implements Runnable {

        String packageName;
        Project project;

        public OpenPlsqlFileFromDB(String packageName_, Project project_) {
            packageName = packageName_;
            project = project_;
        }

        @Override
        public void run() {
            final DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(project);
            DatabaseConnection databaseConnection = connectionProvider.getPooledDatabaseConnection(false);
            DataObject dataObj;
            try {
                dataObj = PlsqlFileUtil.fetchAsTempFile(packageName, PlsqlBlockType.PACKAGE_BODY, databaseConnection, project, null);

                if (dataObj != null) {
                    OpenCookie openCookie = dataObj.getCookie(OpenCookie.class);
                    openCookie.open();
                }
            } catch (NotConnectedToDbException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
