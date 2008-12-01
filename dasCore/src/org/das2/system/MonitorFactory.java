/*
 * MonitorFactory.java
 *
 * Created on November 13, 2006, 11:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.system;

import org.das2.graph.DasCanvas;
import org.das2.graph.DasCanvasComponent;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author jbf
 */
public interface MonitorFactory {

    ProgressMonitor getMonitor(DasCanvas canvas, String string, String string0);
    
    /**
     * returns a monitor in the given context.  For example, if the user is waiting for a DasPlot to be drawn, then
     * the context is the plot, and therefore a DasProgressPanel will be added on top of the plot.
     */
    ProgressMonitor getMonitor(DasCanvasComponent context, String label, String description);

    /**
     * returns a monitor without regard to context.  
     */
    ProgressMonitor getMonitor(String label, String description);
    
}
