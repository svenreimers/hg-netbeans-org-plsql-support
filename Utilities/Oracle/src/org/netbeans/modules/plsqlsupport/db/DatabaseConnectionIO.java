/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.plsqlsupport.db;

import org.openide.windows.InputOutput;

/**
 * Used to gather all IO related parts. Same IO can be used from several places, e.g. transaction and execution.
 *
 * @author ChrLSE
 */
public interface DatabaseConnectionIO {

    /**
     * Setting the summery (e.g. the content of the file). Has to be set before calling {@link initialize()}.
     *
     * @param summery
     */
    public void setSummery(String summery);

    /**
     * Will open the tab in the Output window. Will have to call {@link setSummery()} first.
     */
    public void initialize();

    public InputOutput getIO();

    public void println(Object object);

    /**
     * Get the suitable Display name, either the filename or the summery (e.g. the content of the file).
     *
     * @return Display name of
     */
    public String executionDisplayName();
}
