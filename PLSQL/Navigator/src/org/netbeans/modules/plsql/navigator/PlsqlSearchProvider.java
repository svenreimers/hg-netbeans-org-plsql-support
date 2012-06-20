/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsql.navigator;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.netbeans.api.db.explorer.DatabaseConnection;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ui.OpenProjects;
import org.netbeans.modules.plsql.lexer.PlsqlBlockType;
import org.netbeans.modules.plsql.utilities.NotConnectedToDbException;
import org.netbeans.modules.plsql.utilities.PlsqlExecutorService;
import org.netbeans.modules.plsql.utilities.PlsqlFileUtil;
import org.netbeans.modules.plsqlsupport.db.DatabaseConnectionManager;
import org.netbeans.modules.plsqlsupport.db.DatabaseContentManager;
import org.netbeans.spi.quicksearch.SearchProvider;
import org.netbeans.spi.quicksearch.SearchRequest;
import org.netbeans.spi.quicksearch.SearchResponse;

import org.openide.cookies.OpenCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public class PlsqlSearchProvider implements SearchProvider {

    final static String MODEL_DIRECTORY_PATH = "#COMPONENT#" + File.separator + "database" + File.separator + "#COMPONENT#";
    private static final PlsqlExecutorService executorService = Lookup.getDefault().lookup(PlsqlExecutorService.class);

    /**
     * Method is called by infrastructure when search operation was requested.
     * Implementors should evaluate given request and fill response object with
     * apropriate results
     *
     * @param request Search request object that contains information what to search for
     * @param response Search response object that stores search results. Note that it's important to react to return value of SearchResponse.addResult(...) method and stop computation if false value is returned.
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

        final Set<String> fileExtentions = new HashSet<String>();
        fileExtentions.addAll(executorService.getExecutionOrder());
        fileExtentions.add(".body");
        fileExtentions.add("spec");

        for (Project project : projects) {
            //For Local Files
            for (String fileExtention : fileExtentions) {
                Map<String, String> plsqlObjects = getLocalPlsqlObjects(project, fileExtention);
                for (String plsqlObject : plsqlObjects.keySet()) {
                    boolean match = useRegExp ? Pattern.matches(query, plsqlObject) : plsqlObject.contains(query);
                    if (match) {
                        if (!response.addResult(new OpenLocalPLSQLFile(plsqlObjects.get(plsqlObject)), plsqlObject.toLowerCase() + "(" + project.getProjectDirectory().getName() + ")")) {
                            return;
                        }
                    }
                }
            }
            //for Package files in DB. Do not consider tables, views, etc
            Set<String> DBFiles = getPlsqlFilesFromDB(project);
            for (String DBFile : DBFiles) {
                boolean match = useRegExp ? Pattern.matches(query, DBFile.toLowerCase()) : DBFile.toLowerCase().contains(query);
                if (match) {
                    if (!response.addResult(new OpenPLSQLFileFromDB(DBFile, project), DBFile.toLowerCase() + "(" + project.getProjectDirectory().getName() + ")")) {
                        return;
                    }
                }
            }
        }
    }

    private Map<String, String> getLocalPlsqlObjects(Project project, final String fileExtention) {
        FileObject workspace = project.getProjectDirectory().getFileObject("workspace");
        Map<String, String> dbObj = new HashMap<String, String>();
        if (workspace != null && workspace.isFolder()) {
            final FileObject[] folders = workspace.getChildren();
            for (FileObject folder : folders) {
                if (folder.isFolder() && !folder.getName().startsWith(".")) {

                    File filePath = new File(workspace.getPath() + File.separator + MODEL_DIRECTORY_PATH.replaceAll("#COMPONENT#", folder.getName()));
                    if (filePath.exists()) {
                        final FileFilter filter = new FileFilter() {

                            @Override
                            public boolean accept(final File file) {
                                return (file.isFile() && file.getName().endsWith(fileExtention));
                            }
                        };
                        final File[] objectFiles = filePath.listFiles(filter);
                        if (objectFiles != null) {
                            for (File objectFile : objectFiles) {
                                dbObj.put(objectFile.getName().toString().toLowerCase(), objectFile.getPath().toString().toLowerCase());
                            }
                        }
                    }
                }
            }
            return dbObj;
        }
        return dbObj;
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

    private static class OpenLocalPLSQLFile implements Runnable {

        File objectFile;
        String filePath;

        public OpenLocalPLSQLFile(String filePath_) {
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

    private static class OpenPLSQLFileFromDB implements Runnable {

        File objectFile;
        String packageName;
        Project project;

        public OpenPLSQLFileFromDB(String packageName_, Project project_) {
            packageName = packageName_;
            project = project_;
        }

        @Override
        public void run() {
            final DatabaseConnectionManager connectionProvider = DatabaseConnectionManager.getInstance(project);
            DatabaseConnection databaseConnection = connectionProvider.getPooledDatabaseConnection(false);
            DataObject dataObj;
            try {
                dataObj = dataObj = PlsqlFileUtil.fetchAsTempFile(packageName, PlsqlBlockType.PACKAGE_BODY, databaseConnection, project, null);

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
