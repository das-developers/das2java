/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.uiowa.physics.pw.das.system;

import java.util.Map;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * NullPreferences object for allowing applications to work in applet environment.
 * Changes that would have been stored are simply ignored.
 * @author jbf
 */
public class NullPreferences extends AbstractPreferences {

    Map<String,String> values;

    NullPreferences() {
        super(null, "");
    }

    protected void putSpi(String key, String value) {
        values.put(key, value);
    }

    protected String getSpi(String key) {
        return  values.get(key);
    }

    protected void removeSpi(String key) {
        // do nothing
    }

    protected void removeNodeSpi() throws BackingStoreException {
        // do nothing
    }

    protected String[] keysSpi() throws BackingStoreException {
        return values.keySet().toArray(new String[values.size()]);
    }

    protected String[] childrenNamesSpi() throws BackingStoreException {
        return new String[0];
    }

    protected AbstractPreferences childSpi(String name) {
        return null;
    }

    protected void syncSpi() throws BackingStoreException {
        // do nothing
    }

    protected void flushSpi() throws BackingStoreException {
        // do nothing
    }
    
}
