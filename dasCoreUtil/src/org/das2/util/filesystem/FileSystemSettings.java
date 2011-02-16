/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.filesystem;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.logging.Logger;

/**
 * controls for file systems.  
 * @author jbf
 */
public class FileSystemSettings {

    private static Logger logger= Logger.getLogger("org.das2.util.filesystem");

    /**
     * check the security manager to see if all permissions are allowed,
     * True indicates is not an applet running in a sandbox.
     *
     * copy of DasAppliction.hasAllPermission
     * @return true if all permissions are allowed
     */
    public static boolean hasAllPermission() {
        try {
            if ( restrictPermission==true ) return false;
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                sm.checkPermission(new java.security.AllPermission());
            }
            return true;
        } catch ( SecurityException ex ) {
            return false;
        }
    }

    private static boolean restrictPermission= false;

    /**
     * see DasApplication.setRestrictPermission
     * @param v
     */
    public static void setRestrictPermission( boolean v ) {
        restrictPermission= v;
    }

    /**
     * this should only be called by FileSystem.  Use FileSystem.settings().
     */
    protected FileSystemSettings() {
        File local;
        if ( !hasAllPermission() ) {
            local= new File("applet_mode");  // this should not be opened.
        } else {
            if (System.getProperty("user.name").equals("Web")) {
                local = new File("/tmp");
            } else {
                local = new File(System.getProperty("user.home"));
            }
            local = new File(local, ".das2/fsCache/wfs/");
        }
        localCacheDir = local;
    }

    public enum Persistence {
        /**
         * No persistence.  No files are cached locally.
         */
        NONE, 
        /**
         * Within a session, files are cached locally.  This is the default.
         */
        SESSION, 
        /**
         * Files persist until a new version is available on the remote cache.
         */
        EXPIRES, 
        /**
         * Files persist indefinately, and the server is only contacted when a file
         * is not available locally.
         */
        ALWAYS
    }
    
    protected File localCacheDir = null;
    /**
     * setting for the location of where the local cache is kept.
     */
    public static final String PROP_LOCALCACHEDIR = "localCacheDir";

    public File getLocalCacheDir() {
        return localCacheDir;
    }

    public void setLocalCacheDir(File localCacheDir) {
        File oldLocalCacheDir = this.localCacheDir;
        this.localCacheDir = localCacheDir;
        logger.fine( "setLocalCacheDir("+localCacheDir+")" );
        propertyChangeSupport.firePropertyChange(PROP_LOCALCACHEDIR, oldLocalCacheDir, localCacheDir);
    }
    protected Persistence persistence = Persistence.SESSION;
    /**
     * setting for how long files should be kept and using in the cache.
     */
    public static final String PROP_PERSISTENCE = "persistence";

    public Persistence getPersistence() {
        return persistence;
    }

    public void setPersistence(Persistence persistence) {
        Persistence oldPersistence = this.persistence;
        this.persistence = persistence;
        propertyChangeSupport.firePropertyChange(PROP_PERSISTENCE, oldPersistence, persistence);
    }
    protected boolean allowOffline = true;
    /**
     * allow use of persistent, cached files when the file system is not accessible.
     * FileSystem implementations will throw FileNotFound exception when remote
     * resources are not available, and FileSystemOfflineExceptions are not
     * thrown.
     */
    public static final String PROP_ALLOWOFFLINE = "allowOffline";

    public boolean isAllowOffline() {
        return allowOffline;
    }

    public void setAllowOffline(boolean allowOffline) {
        boolean oldAllowOffline = allowOffline;
        this.allowOffline = allowOffline;
        propertyChangeSupport.firePropertyChange(PROP_ALLOWOFFLINE, oldAllowOffline, allowOffline);
    }

    public static final String PROP_OFFLINE= "offline";

    private boolean offline= false;

    public boolean isOffline() {
        return offline;
    }

    /**
     * @param offline
     */
    protected void setOffline( boolean offline ) {
        boolean v= this.offline;
        this.offline= offline;
        propertyChangeSupport.firePropertyChange( PROP_OFFLINE, v, offline);
    }

    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public synchronized void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

}
