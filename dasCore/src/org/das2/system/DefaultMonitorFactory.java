/*
 * MonitorFactory.java
 *
 * Created on December 1, 2005, 4:59 PM
 *
 *
 */

package org.das2.system;

import java.util.Map;
import org.das2.components.DasProgressPanel;
import org.das2.graph.DasCanvas;
import org.das2.graph.DasCanvasComponent;
import org.das2.util.monitor.ProgressMonitor;
import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * Provided so we could look at monitors leftover when debugging.  Note Autoplot appears to get old monitors from here
 * @author Jeremy
 */
public class DefaultMonitorFactory implements MonitorFactory {
    LinkedHashMap monitors= new LinkedHashMap(SIZE) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            boolean removed = super.removeEldestEntry(eldest);
            return ( size()>SIZE );
        }
    };

    protected static final int SIZE = 10;
    public static final String PROP_SIZE = "size";

    public int getSize() {
        return SIZE;
    }
    
    public class MonitorEntry {
        ProgressMonitor monitor;
        String description;
        MonitorEntry( ProgressMonitor monitor, String description ) {
            this.monitor= monitor;
            this.description= description;
        }
        public String toString() {
            String desc= description;
            if ( desc.length() > 50 ) 
                desc= "..."+description.substring( description.length() - 50 );
            return String.valueOf(monitor)+" "+desc;
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
    
    private void putMonitor( ProgressMonitor monitor, String label, String description ) {
        Long key= System.currentTimeMillis();
        monitors.put( key, new MonitorEntry( monitor, description ) );
    }
    
    public MonitorEntry[] getMonitors() {
        Collection set= monitors.values();
        return (MonitorEntry[])set.toArray( new MonitorEntry[ set.size() ] );
    }
    
    public MonitorEntry getMonitors( int i ) {
        return getMonitors()[i];
    }
    
    public void setClear( boolean clear ) {
        if ( clear ) {
            monitors.keySet().removeAll(monitors.keySet());
        }
    }
    
    public boolean isClear() {
        return false;
    }

    
    
}
