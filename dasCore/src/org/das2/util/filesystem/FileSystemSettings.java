/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;

/**
 * controls for file systems.  
 * @author jbf
 */
public class FileSystemSettings {

    protected FileSystemSettings() {
        File local;
        if (System.getProperty("user.name").equals("Web")) {
            local = new File("/tmp");
        } else {
            local = new File(System.getProperty("user.home"));
        }
        local = new File(local, ".das2/fsCache/wfs/");

        localCacheDir= local;
    }
    
    public enum Persistence { NONE, SESSION, EXPIRES, ALWAYS }
    
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

    
    protected boolean allowOffline = false;
    
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
    
    
    private PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }
    
}
