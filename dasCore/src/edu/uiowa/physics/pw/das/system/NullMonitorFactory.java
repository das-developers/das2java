/*
 * NullMonitorFactory.java
 *
 * Created on November 13, 2006, 11:59 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.system;

import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;

/**
 * MonitorFactory implementation that always returns a null monitor.
 * @author jbf
 */
public class NullMonitorFactory implements MonitorFactory {
    
    public NullMonitorFactory() {
    }
    
    private DasProgressMonitor createNullMonitor() {
        return new DasProgressMonitor() {
            public void setTaskSize(long taskSize) {} ;
            public long getTaskSize( ) { return 1; }
            public void setTaskProgress(long position) throws IllegalArgumentException {};
            public void setProgressMessage( String message ) {} ;
            public long getTaskProgress() { return 0; };
            public void started() {};
            public void finished() {};
            public void cancel() {};
            public boolean isCancelled() { return false; };
            public void setAdditionalInfo(String s) { };
            public void setLabel( String s ) { };
            public String getLabel() { return ""; }
        };
    }
    
    public DasProgressMonitor getMonitor(DasCanvasComponent context, String label, String description) {
        // TODO: check to see if DasProgressMonitor.NULL can be used.  I think the progress monitor is used
        // in some places to identify a process.
        return createNullMonitor();
    }
    
    public DasProgressMonitor getMonitor(String label, String description) {
        return createNullMonitor();
    }
    
}
