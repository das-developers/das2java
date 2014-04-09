/*
 * MonitorFactory.java
 *
 * Created on December 1, 2005, 4:59 PM
 *
 *
 */

package org.das2.system;

import org.das2.components.DasProgressPanel;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasCanvasComponent;
import org.das2.util.monitor.ProgressMonitor;
import java.util.LinkedList;

/**
 * Provided so we could look at monitors leftover when debugging.  Note Autoplot appears to get old monitors from here
 * @author Jeremy
 */
public class DefaultMonitorFactory implements MonitorFactory {
    LinkedList monitors= new LinkedList();

    protected int size = 10;

    public synchronized int getSize() {
        return size;
    }

    public synchronized void setSize( int size ) {
        this.size= size;
        while ( monitors.size()>size ) {
            monitors.remove(0);
        }
    }
    
    public static class MonitorEntry {
        ProgressMonitor monitor;
        String description;
        MonitorEntry( ProgressMonitor monitor, String description ) {
            this.monitor= monitor;
            this.description= description;
        }
        @Override
        public String toString() {
            String desc= description;
            if ( desc.length() > 50 ) 
                desc= "..."+description.substring( description.length() - 50 );
            return String.valueOf(monitor)+" "+desc;
        }
        public ProgressMonitor getMonitor() {
            return monitor;
        }
        
    }
    

    public ProgressMonitor getMonitor(DasCanvas canvas, String label, String description ) {
        ProgressMonitor result= DasProgressPanel.createComponentPanel( canvas, label );
        putMonitor( result, label, description );
        return result;
    }
    
    public ProgressMonitor getMonitor( DasCanvasComponent context, String label, String description ) {
        ProgressMonitor result= DasProgressPanel.createComponentPanel( context, label );
        putMonitor( result, label, description );
        return result;
    }
    
    public ProgressMonitor getMonitor( String label, String description ) {
        ProgressMonitor result= DasProgressPanel.createFramed( label );
        putMonitor( result, label, description );
        return result;
    }
    
    private synchronized void putMonitor( ProgressMonitor monitor, String label, String description ) {
        monitors.add( new MonitorEntry( monitor, description ) );
        while ( monitors.size()>size ) {
            monitors.remove(0);
        }
    }
    
    public synchronized MonitorEntry[] getMonitors() {
        return (MonitorEntry[])monitors.toArray( new MonitorEntry[ monitors.size() ] );
    }
    
    public MonitorEntry getMonitors( int i ) {
        return getMonitors()[i];
    }
    
    public synchronized void setClear( boolean clear ) {
        if ( clear ) {
            monitors.clear();
        }
    }
    
    public boolean isClear() {
        return false;
    }

    
    
}
