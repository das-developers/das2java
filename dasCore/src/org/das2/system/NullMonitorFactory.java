/*
 * NullMonitorFactory.java
 *
 * Created on November 13, 2006, 11:59 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.system;

import org.das2.graph.DasCanvasComponent;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;

/**
 * MonitorFactory implementation that always returns a null monitor.
 * @author jbf
 */
public class NullMonitorFactory implements MonitorFactory {
    
    public NullMonitorFactory() {
    }
    
    private ProgressMonitor createNullMonitor() {
        return new NullProgressMonitor();
    }
    
    public ProgressMonitor getMonitor(DasCanvasComponent context, String label, String description) {
        return createNullMonitor();
    }
    
    public ProgressMonitor getMonitor(String label, String description) {
        return createNullMonitor();
    }
    
}
