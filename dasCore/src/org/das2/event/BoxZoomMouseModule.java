/*
 * BoxZoomMouseModule.java
 *
 * Created on May 20, 2005, 12:21 PM
 */

package org.das2.event;

import java.awt.Point;
import java.awt.event.MouseWheelEvent;
import java.util.logging.Level;
import javax.swing.SwingUtilities;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.GraphUtil;
import org.das2.graph.DasAxis;
import org.das2.dataset.DataSetConsumer;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.DatumRangeUtil;
import static org.das2.event.MouseModule.logger;

/**
 * Provide a box zoom where drag to draw a box that will be the new range, and mouse wheel events
 * are zoom in and zoom out.  This is typically attached to the left mouse button.
 * 
 * @see ZoomPanMouseModule
 * 
 * @author jbf
 */
public class BoxZoomMouseModule extends BoxRangeSelectorMouseModule {
    
    DatumRange xrange, yrange;
    
    boolean autoUpdate= true;
    boolean constrainProportions= false;
    long t0, tbirth;
    
    /** 
     * Creates a new instance of BoxZoomMouseModule
     * @param parent component who owns the mouse module
     * @param consumer is the source context of the data set selection
     * @param xAxis the plot's xaxis.
     * @param yAxis the plot's yaxis.
     */
    public BoxZoomMouseModule( DasCanvasComponent parent, DataSetConsumer consumer, DasAxis xAxis, DasAxis yAxis ) {
        super( parent, consumer, xAxis, yAxis );
        setDragRenderer( new BoxZoomGesturesRenderer(parent) );
        setLabel("Box Zoom");
        tbirth = System.nanoTime();
    }
        
    protected void zoomBox() {
        if ( yrange!=null ) yAxis.setDatumRange(yrange);
        if ( xrange!=null ) xAxis.setDatumRange(xrange);
    }
   
    /**
     * mouse wheel events zoom or pan rapidly.  With a physical wheel, I (jbf) found
     * that I get 17ms per click, and this is managable.  With a touchpad on a mac,
     * these events come much faster, like 10ms per click, which can disorient the
     * operator.  So we limit the speed to 20ms per click, for now by dropping
     * rapid clicks.
     *
     * @param e
     */
    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double nmin, nmax; 
        
        double xshift = 0., yshift = 0.; // these are use to cancel out zoom/expand corrections to anchor a corner.

        if ((e.isControlDown() || e.isShiftDown())) {
            if (xAxis != null && yAxis != null) return; // this happens when mouse drifts onto plot during xaxis pan.
            if (e.getWheelRotation() < 0) {
                nmin = -0.20; // pan left on xaxis
                nmax = +0.80;
            } else {
                nmin = +0.20; // pan right on xaxis
                nmax = +1.20;
            }
        } else {
            Point ep= SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), parent.getCanvas() );
            
            //ep.translate( e.getComponent().getX(), e.getComponent().getY() );
            Pos xpos = xAxis == null ? Pos._null : position(xAxis.getColumn(), ep.x, 20);
            Pos ypos = yAxis == null ? Pos._null : position(yAxis.getRow(), ep.y, 20);

            //mac trackpads coast a while after release, so let's govern the speed a little more
            if (e.getWheelRotation() < 0) {
                nmin = 0.10; // zoom in
                nmax = 0.90;
            } else {
                nmin = -0.125; // zoom out
                nmax = 1.125;
            }
            switch (xpos) {
                case min:
                    xshift = -nmin; // this will cancel out nmin
                    break;
                case max:
                    xshift = nmin;
                    break;
                default:
                    logger.log(Level.FINE, "xpos: {0}", xpos);
            }
            switch (ypos) {
                case min:
                    yshift = nmin; // this will cancel out nmin
                    break;
                case max:
                    yshift = -nmin;
                    break;
                default:
                    logger.log(Level.FINE, "ypos: {0}", xpos);
            }
        }

        //int clickMag= Math.abs(e.getWheelRotation());
        int clickMag = 1;
        final long t1 = System.nanoTime();
        long limitNanos = (long) 40e6;
        if ((t1 - t0) / clickMag < limitNanos) {
            clickMag = (int) Math.floor( (double)(t1 - t0) / limitNanos );
        }

        if (clickMag == 0) return;
        t0 = System.nanoTime();

        // these will be non-null if they should be used.
        DatumRange xdrnew=null;
        DatumRange ydrnew=null;

        logger.log(Level.FINEST, ":ns:  {0}  {1}", new Object[]{System.nanoTime() - tbirth, clickMag});
        if (axisIsAdjustable(xAxis)) {
            DatumRange dr = xAxis.getDatumRange();
            for (int i = 0; i < clickMag; i++) {
                if (xAxis.isLog()) {
                    dr = DatumRangeUtil.rescaleLog(dr, nmin+xshift, nmax+xshift);
                } else {
                    dr = DatumRangeUtil.rescale(dr, nmin+xshift, nmax+xshift);
                }
            }
            dr= maybeRound( xAxis, dr );
            
            if ( ! DatumRangeUtil.isAcceptable( dr, xAxis.isLog() ) ) {
                dr= null;
            }
            xdrnew= dr;
        }
        
        if (axisIsAdjustable(yAxis)) {
            DatumRange dr = yAxis.getDatumRange();
            for (int i = 0; i < clickMag; i++) {
                if (yAxis.isLog()) {
                    dr = DatumRangeUtil.rescaleLog(dr, nmin+yshift, nmax+yshift);
                } else {
                    dr = DatumRangeUtil.rescale(dr, nmin+yshift, nmax+yshift);
                }
            }
            dr= maybeRound( yAxis, dr );
            // check bounds are still finite
            if ( ! DatumRangeUtil.isAcceptable(dr, yAxis.isLog() ) ) {
                dr= null;
            }
            ydrnew= dr;
        }

        if ( axisIsAdjustable(xAxis) && xdrnew==null ) return;
        if ( axisIsAdjustable(yAxis) && ydrnew==null ) return;

        if ( axisIsAdjustable(xAxis) ) xAxis.setDatumRange(xdrnew);
        if ( axisIsAdjustable(yAxis) ) yAxis.setDatumRange(ydrnew);

        super.mouseWheelMoved(e);
    }

    @Override
    public void mouseRangeSelected(MouseDragEvent e0) {
        if ( e0 instanceof MouseBoxEvent ) {
            MouseBoxEvent e= (MouseBoxEvent)e0;
            
            xrange= GraphUtil.invTransformRange( xAxis, e.getXMinimum(), e.getXMaximum() );
            yrange= GraphUtil.invTransformRange( yAxis, e.getYMinimum(), e.getYMaximum() );

            double boxAspect= ( e.getYMaximum() - e.getYMinimum() ) / (float)(  e.getXMaximum() - e.getXMinimum() );
            boolean edgeY= e.getYMaximum()>yAxis.getRow().getDMaximum() || e.getYMinimum()<yAxis.getRow().getDMinimum();
            boolean edgeX= e.getXMaximum()>xAxis.getColumn().getDMaximum() || e.getXMinimum()<xAxis.getColumn().getDMinimum();

            BoxZoomGesturesRenderer.Type t= BoxZoomGesturesRenderer.idType( parent, e.getPressPoint(), e.getPoint() );
            if ( t==BoxZoomGesturesRenderer.Type.XAXIS ) {
                yrange= yAxis.getDatumRange();
            }
            if ( t==BoxZoomGesturesRenderer.Type.YAXIS ) {
                xrange= xAxis.getDatumRange();
            }
            
            //check for narrow (<5px) boxes which we will treat as accidental in the narrow dimension
            if ( ( e.getYMaximum()-e.getYMinimum() )<5 || ( boxAspect<0.1 && edgeY ) ) {
                yrange= yAxis.getDatumRange();
            }
            if ( ( e.getXMaximum()-e.getXMinimum() )<5 || ( boxAspect>10 && edgeX ) ) {
                xrange= xAxis.getDatumRange();
            }
            //boxes along axes must only zoom along that axes.  The intent might have been to start the box on the axis instead of the plot.
            if ( edgeY && boxAspect<0.2 ) {
                yrange= yAxis.getDatumRange();
            }
            if ( edgeX && boxAspect>5 ) {
                xrange= xAxis.getDatumRange();
            }

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
