/*
 * SubTaskMonitor.java
 *
 * Created on August 18, 2005, 4:01 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util;

/**
 *
 * @author Jeremy
 */
public class SubTaskMonitor implements DasProgressMonitor {
    DasProgressMonitor parent;
    long min, max, progress, size;
    String label;
    
    private SubTaskMonitor( DasProgressMonitor parent, long min, long max ) {
        this.parent= parent;
        this.min= min;
        this.max= max;
        this.size= -1;
    }
    
    public static SubTaskMonitor create( DasProgressMonitor parent, long min, long max ) {
        return new SubTaskMonitor( parent, min, max );
    }
        
    public void cancel() {
        parent.cancel();
    }

    public void finished() {        
        // ignore
    }

    public long getTaskProgress() {
        return progress;
    }

    public boolean isCancelled() {
        return parent.isCancelled();
    }

    public void setAdditionalInfo(String s) {
        // ignore
    }

    public void setTaskProgress(long position) throws IllegalArgumentException {
        this.progress= position;
        if ( size==-1 ) {
            parent.setTaskProgress( parent.getTaskProgress() + 1 );
        } else {
            parent.setTaskProgress( min + ( max - min ) * position / size );
        }
    }

    public void setTaskSize(long taskSize) {
        this.size= taskSize;
    }

    public long getTaskSize() {
        return this.size;
    }
    
    public void started() {
        // ignore
    }

    public void setLabel(String label) {
        this.label= label;
    }

    public String getLabel() {
        return label;
    }
    
    public String toString() {
        return parent.toString()+">"+label;
    }
}
