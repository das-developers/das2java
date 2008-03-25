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
import org.das2.util.monitor.DasProgressMonitor;
import org.das2.util.monitor.NullProgressMonitor;

/**
 * MonitorFactory implementation that always returns a null monitor.
 * @author jbf
 */
public class NullMonitorFactory implements MonitorFactory {
    
    public NullMonitorFactory() {
    }
    
    private DasProgressMonitor createNullMonitor() {
        return new NullProgressMonitor();
    }
    
    public DasProgressMonitor getMonitor(DasCanvasComponent context, String label, String description) {
        return createNullMonitor();
    }
    
    public DasProgressMonitor getMonitor(String label, String description) {
        return createNullMonitor();
    }
    
}
