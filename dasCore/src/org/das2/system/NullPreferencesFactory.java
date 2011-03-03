/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.system;

import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Creates NullPreferences for use with applets.  The system property 
 * java.util.prefs.PreferencesFactory should be set to
 * org.das2.system.NullPreferencesFactory to use this class.
 * 
 * System.setProperty( "java.util.prefs.PreferencesFactory", "org.das2.system.NullPreferencesFactory" ) doesn't
 * work in applets--security exception.
 *
 * Also this works: java  -Djava.util.prefs.PreferencesFactory=org.das2.system.NullPreferencesFactory ...
 * 
 * I'm not sure where I found this solution originally, but here is a nice writeup:
 *     http://www.allaboutbalance.com/articles/disableprefs/
 * 
 * @author jbf
 */
public class NullPreferencesFactory implements PreferencesFactory {

    public NullPreferencesFactory() {
        System.err.println("using NullPreferencesFactory");
    }
    
    public Preferences systemRoot() {
        return new NullPreferences();
    }

    public Preferences userRoot() {
        return new NullPreferences();
    }

}
