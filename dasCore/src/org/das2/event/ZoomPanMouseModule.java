/*
 * ZoomPanMouseModule.java
 *
 * Created on August 7, 2007, 8:53 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.event;

import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.UnitsUtil;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.TickVDescriptor;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 *
 * @author jbf
 */
public class ZoomPanMouseModule extends MouseModule {

    DasAxis xAxis;
    DasAxis yAxis;
    DasAxis.Lock xAxisLock;
    DasAxis.Lock yAxisLock;
    Point p0;
    DatumRange xAxisRange0;
    DatumRange yAxisRange0;
    long t0,tbirth;

    /** Creates a new instance of ZoomPanMouseModule */
    public ZoomPanMouseModule(DasCanvasComponent parent, DasAxis horizontalAxis, DasAxis verticalAxis) {
        super(parent);
        setLabel("Zoom Pan");
        this.xAxis = horizontalAxis;
        this.yAxis = verticalAxis;
        t0= System.nanoTime();
        tbirth= System.nanoTime();
    }

    private boolean axisIsAdjustable(DasAxis axis) {
        return axis != null && (UnitsUtil.isIntervalMeasurement(axis.getUnits()) || UnitsUtil.isRatioMeasurement(axis.getUnits()));
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
    public void mouseWheelMoved(MouseWheelEvent e) {
        double nmin, nmax;
        
        if ( e.isControlDown() || e.isShiftDown() ) {
            if (e.getWheelRotation() < 0) {
                nmin = -0.20;
                nmax = +0.80;
            } else {
                nmin = +0.20;
                nmax = +1.20;
            }            
        } else {
            if (e.getWheelRotation() < 0) {
                nmin = 0.20;
                nmax = 0.80;
            } else {
                nmin = -0.25;
                nmax = 1.25;
            }
        }
        //int clickMag= Math.abs(e.getWheelRotation());
        int clickMag = 1;
        final long t1 = System.nanoTime();
        long limitNanos= (long)20e6;
        if ( ( t1 - t0 ) / clickMag < limitNanos ) {
            clickMag= (int) Math.floor( ( t1 - t0 ) / limitNanos );
        }
        
        if ( clickMag==0 ) return;
        t0= System.nanoTime();
            
        //System.err.println(":ns:  "+(System.nanoTime()-tbirth)+"  "+clickMag);
        if (axisIsAdjustable(xAxis)) {
            DatumRange dr = xAxis.getDatumRange();
            for (int i = 0; i < clickMag; i++) {
                if (xAxis.isLog()) {
                    dr = DatumRangeUtil.rescaleLog(dr, nmin, nmax);
                } else {
                    dr = DatumRangeUtil.rescale(dr, nmin, nmax);
                }
            }
            xAxis.setDatumRange(dr);
        }
        if (axisIsAdjustable(yAxis)) {
            DatumRange dr = yAxis.getDatumRange();
            for (int i = 0; i < clickMag; i++) {

                if (yAxis.isLog()) {
                    dr = DatumRangeUtil.rescaleLog(dr, nmin, nmax);
                } else {
                    dr = DatumRangeUtil.rescale(dr, nmin, nmax);
                }
            }
            yAxis.setDatumRange(dr);
        }

        super.mouseWheelMoved(e);
    }

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
        doPan(e, false);
        parent.getCanvas().getGlassPane().setCursor(null);
    }

    /**
     * round to a nice boundaries.
     */
    private static DatumRange doRound(DatumRange dr, DasAxis axis) {
        TickVDescriptor ticks;
        if (dr.getUnits() instanceof TimeLocationUnits) {
            ticks = TickVDescriptor.bestTickVTime(dr.min(), dr.max(), axis.getDLength() / 2, axis.getDLength(), true);
        } else if (axis.isLog()) {
            ticks = TickVDescriptor.bestTickVLogNew(dr.min(), dr.max(), axis.getDLength() / 2, axis.getDLength(), true);
        } else {
            ticks = TickVDescriptor.bestTickVLinear(dr.min(), dr.max(), axis.getDLength() / 2, axis.getDLength(), true);
        }
        return ticks.enclosingRange(dr, true);
    }

    private void doPan(final MouseEvent e, boolean round) {
        Point p2 = e.getPoint();
        if (axisIsAdjustable(xAxis)) {
            DatumRange dr;
            if (xAxis.isLog()) {
                Datum delta = xAxis.invTransform(p0.getX()).divide(xAxis.invTransform(p2.getX()));
                dr = new DatumRange(xAxisRange0.min().multiply(delta), xAxisRange0.max().multiply(delta));
            } else {
                Datum delta = xAxis.invTransform(p0.getX()).subtract(xAxis.invTransform(p2.getX()));
                dr = new DatumRange(xAxisRange0.min().add(delta), xAxisRange0.max().add(delta));
            }
            if (round) {
                dr = doRound(dr, xAxis);
            }
            xAxis.setDatumRange(dr);
        }
        if (axisIsAdjustable(yAxis)) {
            DatumRange dr;
            if (yAxis.isLog()) {
                Datum ydelta = yAxis.invTransform(p0.getY()).divide(yAxis.invTransform(p2.getY()));
                dr = new DatumRange(yAxisRange0.min().multiply(ydelta), yAxisRange0.max().multiply(ydelta));
            } else {
                Datum ydelta = yAxis.invTransform(p0.getY()).subtract(yAxis.invTransform(p2.getY()));
                dr = new DatumRange(yAxisRange0.min().add(ydelta), yAxisRange0.max().add(ydelta));
            }
            if (round) {
                dr = doRound(dr, yAxis);
            }
            yAxis.setDatumRange(dr);
        }
    }

    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        doPan(e, false);
    }

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
        parent.getCanvas().getGlassPane().setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
