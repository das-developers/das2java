/*
 * DasEventsIndicator.java
 *
 * Created on April 6, 2004, 10:39 AM
 */

package edu.uiowa.physics.pw.das.graph;

import org.das2.dataset.DataSetUpdateEvent;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.DataSetUpdateListener;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 *
 * @author  Jeremy
 *
 * The DasEventsIndicator takes a DataSetDescriptor that produces VectorDataSets with a plane
 * "xTagWidth."  This plane should consist of Datums with the same Units as the xAxis offset Units.
 * The y values of the DataSet will be toString'ed and this value will be the tooltip of the bar.
 */
public class DasEventsIndicator extends DasPlot implements DataSetUpdateListener {
    EventsRenderer renderer;
    
    /** Creates a new instance of DasEventsIndicator */
    public DasEventsIndicator( DataSetDescriptor dsd, DasAxis axis, DasAxis yAxis, String planeId ) {
        super( axis, yAxis );
        renderer= new EventsRenderer( dsd ) ;
        addRenderer( renderer );
    }
    
    /**
     * This method replaces the old constructor.  This is unavoidable
     */
    public static DasEventsIndicator create( DataSetDescriptor dsd, DasAxis axis, String planeId ) {
        DasAxis yAxis= new DasAxis( new DatumRange( 0,1, Units.dimensionless ), DasAxis.VERTICAL );
        yAxis.setVisible(false);
        return new DasEventsIndicator( dsd, axis, yAxis, planeId );
    }
    
    
    public void setDataSetDescriptor( DataSetDescriptor dsd ) {
        renderer.setDataSetDescriptor(dsd);
    }
    
    public DataSetDescriptor getDataSetDescriptor( ) {
        return renderer.getDataSetDescriptor();
    }
    
    public void dataSetUpdated(DataSetUpdateEvent e) {
        ((XAxisDataLoader)renderer.getDataLoader()).dataSetUpdated(e);
    }
    
}
