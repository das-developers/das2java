/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.filesystem;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * controls for file systems.  
 * @author jbf
 */
public class FileSystemSettings {

    private static final Logger logger= org.das2.util.LoggerManager.getLogger("org.das2.util.filesystem");

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
     * true means don't attempt to gain access to applet-restricted functions.
     * @param v true means don't attempt to gain access to applet-restricted functions.
     * @see org.das2.DasApplication#setRestrictPermission
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

    private int connectTimeoutMs = 5000;
    
    /**
     * return the connection timeout in milliseconds.
     * @return the connection timeout in milliseconds.
     */
    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    /**
     * reset the connect timeout.  Setting to 
     * 0 or a negative number will reset it to the
     * default of 5000ms.
     * @param millis 
     */
    public void setConnectTimeoutMs( int millis ) {
        if ( millis<=0 ) millis= 5000;
        connectTimeoutMs= millis;
    }
    
    private int readTimeoutMs = 60000;
    
    /**
     * return the read timeout in milliseconds.
     * @return
     */
    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }
    
    /**
     * reset the time read timeout.  Setting to 
     * 0 or a negative number will reset it to the
     * default of 60000ms.
     * @param millis number of milliseconds
     */
    public void setReadTimeoutMs( int millis ) {
        if ( millis<=0 ) millis= 60000;
        this.readTimeoutMs= millis;
    }
    
    /**
     * return the number of seconds that an unused temporary file will
     * be left on the system before it may be deleted.  This presumes
     * that the code can determine if a temporary file is in use, which is
     * not really the case.
     * @return the number of seconds.
     */
    public int getTemporaryFileTimeoutSeconds() {
        return 3600;
    }
    
    // NOTE WebFileSystem contains some settings as well!!

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
         * Files persist indefinitely, and the server is only contacted when a file
         * is not available locally.
         */
        ALWAYS
    }
    
    protected File localCacheDir = null;
    /**
     * setting for the location of where the local cache is kept.
     */
    public static final String PROP_LOCALCACHEDIR = "localCacheDir";

    /**
     * setting for the location of where the local cache is kept.
     * @return File that is the local cache directory.
     */
    public File getLocalCacheDir() {
        return localCacheDir;
    }

    public void setLocalCacheDir(File localCacheDir) {
        File oldLocalCacheDir = this.localCacheDir;
        this.localCacheDir = localCacheDir;
        logger.log( Level.FINE, "setLocalCacheDir({0})", localCacheDir);
        propertyChangeSupport.firePropertyChange(PROP_LOCALCACHEDIR, oldLocalCacheDir, localCacheDir);
    }
    protected Persistence persistence = Persistence.SESSION;
    /**
     * setting for how long files should be kept and using in the cache.
     */
    public static final String PROP_PERSISTENCE = "persistence";

    /**
     * get the setting for how long files should be kept and using in the cache,
     * e.g. Persistence.SESSION means during the session.
     * 
     * @return the setting
     */
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

    /**
     * true indicate file system is offline and cached files should be used.
     * @return 
     */
    public boolean isOffline() {
        return offline;
    }

    /**
     * If true, then force the filesystems to be offline.  If false, then use each filesystem's status.
     * FileSystem.reset() should be called after this.
     * @param offline
     */
    public void setOffline( boolean offline ) {
        boolean v= this.offline;
        this.offline= offline;
        propertyChangeSupport.firePropertyChange( PROP_OFFLINE, v, offline);
    }

    /**
     * the longest amount of time we'll wait for an external process to make progress downloading.
     */
    protected static final long allowableExternalIdleMs= 60000;
    
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

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
