/*
 * ZoomPanMouseModule.java
 *
 * Created on August 7, 2007, 8:53 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.event;

import java.util.logging.Level;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;

/**
 * Provide navigation similar to Google Maps, where drag events result a pan on the axes, and mouse wheel events
 * are zoom in and zoom out.  This is typically attached to the middle mouse button.
 * 
 * @see BoxZoomMouseModule
 * 
 * @author jbf
 */
public class ZoomPanMouseModule extends MouseModule {

    DasAxis xAxis;
    DasAxis.Lock xAxisLock;
    DasAxis yAxis;
    DasAxis.Lock yAxisLock;

    Point p0;
    DatumRange xAxisRange0;
    DatumRange yAxisRange0;
    long t0, tbirth;

    /** Creates a new instance of ZoomPanMouseModule */
    public ZoomPanMouseModule(DasCanvasComponent parent, DasAxis horizontalAxis, DasAxis verticalAxis) {
        super(parent);
        setLabel("Zoom Pan");
        this.xAxis = horizontalAxis;
        this.yAxis = verticalAxis;
        t0 = System.nanoTime();
        tbirth = System.nanoTime();
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
        
        double xshift = 0., yshift = 0.;

        if ((e.isShiftDown()) ) {
            logger.fine("shift is down but no longer has any effect.  Use control to pan.");
            parent.getDasMouseInputAdapter().getFeedback().setMessage("shift has no effect, press control to pan");
            return;
        }
        if ((e.isControlDown() )) {  // shift no longer triggers because Mac Mouse pad.
            if (xAxis != null && yAxis != null) {
                parent.getDasMouseInputAdapter().getFeedback().setMessage("pan is disabled when there are two axes");
                return;
            } // this happens when mouse drifts onto plot during xaxis pan.
            double rot = e.getPreciseWheelRotation();
            if ( rot<-2.0 ) rot=-2.0;
            if ( rot>+2.0 ) rot=+2.0;
            nmin = 0.20*rot; // pan left on xaxis
            nmax = nmin + 1.0;

        } else {
            Point ep= SwingUtilities.convertPoint( e.getComponent(), e.getPoint(), parent.getCanvas() );
            
            //ep.translate( e.getComponent().getX(), e.getComponent().getY() );
            Pos xpos = xAxis == null ? Pos._null : position(xAxis.getColumn(), ep.x, 20);
            Pos ypos = yAxis == null ? Pos._null : position(yAxis.getRow(), ep.y, 20);

            //mac trackpads coast a while after release, so let's govern the speed a little more
            double rot = e.getPreciseWheelRotation();
            if ( rot<-2.0 ) rot=-2.0;
            if ( rot>+2.0 ) rot=+2.0;
            if ( rot < 0) {
                nmin = 0.10 * (-1*rot); // zoom in
                nmax = 1.0 - nmin;
            } else {
                nmin = -0.125 * rot; // zoom out
                nmax = 1.0 - nmin;
            }
            switch (xpos) {
                case min:
                    xshift = -nmin;
                    break;
                case max:
                    xshift = nmin;
                    break;
                default:
                    logger.log(Level.FINE, "xpos: {0}", xpos);
            }
            switch (ypos) {
                case min:
                    yshift = nmin;
                    break;
                case max:
                    yshift = -nmin;
                    break;
                default:
                    logger.log(Level.FINE, "ypos: {0}", ypos);
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
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        if (xAxis != null) {
            xAxisLock.unlock();
            xAxisLock = null;
        }
        if (yAxis != null) {
            yAxisLock.unlock();
            yAxisLock = null;
        }
        doPan(e);
        parent.getCanvas().getGlassPane().setCursor(null);
    }

    private void doPan(final MouseEvent e) {
        Point p2 = e.getPoint();
        if (axisIsAdjustable(xAxis)) {
            DatumRange dr;
            if (xAxis.isLog() ) {
                if ( UnitsUtil.isRatioMeasurement( xAxis.getUnits() ) ) {
                    Datum delta = xAxis.invTransform(p0.getX()).divide(xAxis.invTransform(p2.getX()));
                    dr = new DatumRange(xAxisRange0.min().multiply(delta), xAxisRange0.max().multiply(delta));
                } else {
                    dr = xAxisRange0;
                }
            } else {
                Datum delta = xAxis.invTransform(p0.getX()).subtract(xAxis.invTransform(p2.getX()));
                dr = new DatumRange(xAxisRange0.min().add(delta), xAxisRange0.max().add(delta));
            }
            dr= maybeRound( xAxis, dr );
            xAxis.setDatumRange(dr);
        }
        if (axisIsAdjustable(yAxis)) {
            DatumRange dr;
            if (yAxis.isLog()) {
                if ( UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    Datum ydelta = yAxis.invTransform(p0.getY()).divide(yAxis.invTransform(p2.getY()));
                    dr = new DatumRange(yAxisRange0.min().multiply(ydelta), yAxisRange0.max().multiply(ydelta));
                } else {
                    dr = yAxisRange0;
                }
            } else {
                Datum ydelta = yAxis.invTransform(p0.getY()).subtract(yAxis.invTransform(p2.getY()));
                dr = new DatumRange(yAxisRange0.min().add(ydelta), yAxisRange0.max().add(ydelta));
            }
            dr= maybeRound( yAxis, dr );
            yAxis.setDatumRange(dr);
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        doPan(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        p0 = e.getPoint();
        if (xAxis != null) {
            xAxisRange0 = xAxis.getDatumRange();
            xAxisLock = xAxis.mutatorLock();
            xAxisLock.lock();
        }
        if (yAxis != null) {
            yAxisRange0 = yAxis.getDatumRange();
            yAxisLock = yAxis.mutatorLock();
            yAxisLock.lock();
        }
        parent.getCanvas().getGlassPane().setCursor(new Cursor(Cursor.MOVE_CURSOR));
    }
}
