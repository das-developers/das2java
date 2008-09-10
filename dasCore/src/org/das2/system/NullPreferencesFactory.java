/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.system;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Creates NullPreferences for use with applets.  The system property 
 * java.util.prefs.Preferences should be set to 
 * org.das2.system.NullPreferencesFactory 
 * (or soon org.das2.system.NullPreferencesFactory) to use this class.
 * @author jbf
 */
public class NullPreferencesFactory implements PreferencesFactory {

    public Preferences systemRoot() {
        return new NullPreferences();
    }

    public Preferences userRoot() {
        return new NullPreferences();
    }

}
