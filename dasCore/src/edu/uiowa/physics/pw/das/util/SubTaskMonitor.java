/*
 * SubTaskMonitor.java
 *
 * Created on August 18, 2005, 4:01 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util;

/**
 * creates a ProgressMonitor that maps its progress to a parent's progress.
 * For example, if a process takes a progress monitor, but is implemented in
 * two steps that each take a progress monitor, then two subtask monitors can
 * be created to monitor each step, and the client who passed in the monitor 
 * will see the whole deal as one process.
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

    private boolean finished= false;
    
    public void finished() {        
        this.finished= true; // only to support the bean property
    }

    public boolean isFinished() {
        return this.finished;
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
            parent.setTaskProgress( min );
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
    
    boolean started= false;
    
    public void started() {
        this.started= true;
    }

    public boolean isStarted() {
        return started;
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

    public void setProgressMessage(String message) {
        parent.setProgressMessage(message);
    }
}
