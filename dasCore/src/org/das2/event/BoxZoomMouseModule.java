/*
 * BoxZoomMouseModule.java
 *
 * Created on May 20, 2005, 12:21 PM
 */

package org.das2.event;

import org.das2.graph.DasCanvasComponent;
import org.das2.graph.GraphUtil;
import org.das2.graph.DasAxis;
import org.das2.dataset.DataSetConsumer;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.DatumRangeUtil;
import javax.swing.*;

/**
 *
 * @author Jeremy
 */
public class BoxZoomMouseModule extends BoxRangeSelectorMouseModule {
    
    DatumRange xrange, yrange;
    JLabel xrangeLabel, yrangeLabel;
    JCheckBox autoUpdateCB, constrainProportionsCB;
    
    boolean autoUpdate= true;
    boolean constrainProportions= false;
    
    /** Creates a new instance of BoxZoomMouseModule */
    public BoxZoomMouseModule( DasCanvasComponent parent, DataSetConsumer consumer, DasAxis xAxis, DasAxis yAxis ) {
        super( parent, consumer, xAxis, yAxis );
        setLabel("Box Zoom");
    }
        
    protected void zoomBox() {
        if ( yrange!=null ) yAxis.setDatumRange(yrange);
        if ( xrange!=null ) xAxis.setDatumRange(xrange);
    }
    
    @Override
    public void mouseRangeSelected(MouseDragEvent e0) {
        if ( e0 instanceof MouseBoxEvent ) {
            MouseBoxEvent e= (MouseBoxEvent)e0;
            
            xrange= GraphUtil.invTransformRange( xAxis, e.getXMinimum(), e.getXMaximum() );
            yrange= GraphUtil.invTransformRange( yAxis, e.getYMinimum(), e.getYMaximum() );
            
            if ( constrainProportions ) {
                double aspect= yAxis.getHeight() / (double)xAxis.getWidth();
                DatumRange mx= new DatumRange( e.getXMinimum(), e.getXMaximum(), Units.dimensionless );
                DatumRange my= new DatumRange( e.getYMinimum(), e.getYMaximum(), Units.dimensionless );
                double mouseAspect= my.width().divide(mx.width()).doubleValue(Units.dimensionless);
                if ( mouseAspect > aspect ) {
                    double f= mouseAspect / aspect;
                    mx= DatumRangeUtil.rescale(my, 0.5-f/2, 0.5+f/2 );
                } else {
                    double f= aspect / mouseAspect;
                    my= DatumRangeUtil.rescale(my, 0.5-f/2, 0.5+f/2 );
                }
                xrange= GraphUtil.invTransformRange( xAxis, mx.min().doubleValue(Units.dimensionless),
                        mx.max().doubleValue(Units.dimensionless) );
                yrange= GraphUtil.invTransformRange( yAxis, my.max().doubleValue(Units.dimensionless),
                        my.min().doubleValue(Units.dimensionless) );
            } else {
                xrange= GraphUtil.invTransformRange(  xAxis, e.getXMinimum(), e.getXMaximum() );
                yrange= GraphUtil.invTransformRange(  yAxis, e.getYMaximum(), e.getYMinimum() );
            }
            
            zoomBox();
            
        } else if ( e0.isGesture() ) {
            if ( e0.getGesture()==Gesture.ZOOMOUT ) {
                xAxis.setDataRangeZoomOut();
                yAxis.setDataRangeZoomOut();
            } else if ( e0.getGesture()==Gesture.BACK ) {
                xAxis.setDataRangePrev();
                yAxis.setDataRangePrev();
            } else if ( e0.getGesture()==Gesture.FORWARD ) {
                xAxis.setDataRangeForward();
                yAxis.setDataRangeForward();
            }
        }
        
    }
	
	/**
     * Getter for property autoUpdate.
     * @return Value of property autoUpdate.
     */
    public boolean isAutoUpdate() {
        return this.autoUpdate;
    }
    
    /**
     * Setter for property autoUpdate.
     * @param autoUpdate New value of property autoUpdate.
     */
    public void setAutoUpdate(boolean autoUpdate) {
        this.autoUpdate = autoUpdate;
    }
    
    /**
     * Getter for property constrainProportions.
     * @return Value of property constrainProportions.
     */
    public boolean isConstrainProportions() {
        return this.constrainProportions;
    }
    
    /**
     * Setter for property constrainProportions.
     * @param constrainProportions New value of property constrainProportions.
     */
    public void setConstrainProportions(boolean constrainProportions) {
        this.constrainProportions = constrainProportions;
    }
        
}
