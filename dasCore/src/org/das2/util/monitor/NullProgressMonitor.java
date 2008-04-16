/*
 * NullProgressMonitor.java
 *
 * Created on October 23, 2007, 10:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.util.monitor;

/**
 * This is a progress monitor to use when we don't care about the progress.
 * This replaces ProgressMonitor.NULL, which has the problem that it was
 * stateless and there assume that progress monitors have state.
 *
 * Further, this can act as a base class for other monitor types.
 *
 * @author jbf
 */
public class NullProgressMonitor implements ProgressMonitor {
    
    public NullProgressMonitor() {
    }
    
    private long taskSize=-1 ;
    
    public void setTaskSize(long taskSize) {
        this.taskSize= taskSize;
    }
    
    public long getTaskSize( ) { 
        return taskSize; 
    }
    
    public void setProgressMessage( String message ) {} ;
        
    private long position=0;
    
    public void setTaskProgress(long position) throws IllegalArgumentException {
        this.position= position;
    }
        
    public long getTaskProgress() { 
        return position; 
    }
    
    private boolean started= false;
    
    public void started() {  
        this.started= false;
    }
    
    public boolean isStarted() {
        return started;
    }
    
    private boolean finished= false;
    
    public void finished() {
        finished= true;
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    private boolean cancelled= false;
    
    public void cancel() {
        cancelled= true;
    }
    
    public boolean isCancelled() { 
        return cancelled; 
    }
    
    public void setAdditionalInfo(String s) { };
    
    private String label;
    
    public void setLabel( String s ) { 
        this.label= label;
    }
    
    public String getLabel() { 
        return label; 
    }
    
}
