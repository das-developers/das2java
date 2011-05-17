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
import javax.swing.SwingUtilities;
import org.das2.datum.DomainDivider;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.graph.DasDevicePosition;

/**
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

    private Datum MIN_TIME= TimeUtil.createTimeDatum( 1000, 1, 1, 0, 0, 0, 0 ); // I know these are found elsewhere in the code, but I can't find it.
    private Datum MAX_TIME= TimeUtil.createTimeDatum( 3000, 1, 1, 0, 0, 0, 0 );

    /** Creates a new instance of ZoomPanMouseModule */
    public ZoomPanMouseModule(DasCanvasComponent parent, DasAxis horizontalAxis, DasAxis verticalAxis) {
        super(parent);
        setLabel("Zoom Pan");
        this.xAxis = horizontalAxis;
        this.yAxis = verticalAxis;
        t0 = System.nanoTime();
        tbirth = System.nanoTime();
    }

    private boolean axisIsAdjustable(DasAxis axis) {
        return axis != null && (UnitsUtil.isIntervalMeasurement(axis.getUnits()) || UnitsUtil.isRatioMeasurement(axis.getUnits()));
    }

    /**
     * round to the nearest nice interval by looking for a DomainDivider in the axis.
     * 
     * @param xAxis
     * @param dr
     * @return
     */
    private DatumRange maybeRound(DasAxis xAxis, DatumRange dr) {
        DomainDivider div= xAxis.getMinorTicksDomainDivider();
        if ( div!=null ) {
            try {
                int px= 999;
                while ( px>2 ) {
                    div= div.finerDivider(false);
                    DatumRange minDr= div.rangeContaining(dr.min());
                    px= (int)Math.abs( xAxis.transform(minDr.max()) - xAxis.transform(minDr.min()) );
                }
                DatumRange minDr= div.rangeContaining(dr.min());
                DatumRange maxDr= div.rangeContaining(dr.max());
                Datum min= DatumRangeUtil.normalize( minDr, dr.min() ) < 0.5 ? minDr.min() : minDr.max();
                Datum max= DatumRangeUtil.normalize( maxDr, dr.max() ) < 0.5 ? maxDr.min() : maxDr.max();
                DatumRange drRound= new DatumRange( min, max );

                double d= DatumRangeUtil.normalize( dr, drRound.min() );
                dr= drRound;
            } catch ( InconvertibleUnitsException ex ) {
                // it's okay to do nothing, this is a transient state
            }
        }
        return dr;
    }

    private enum Pos {
        _null, beyondMin, min, middle, max, beyondMax
    };

    private Pos position(DasDevicePosition ddp, int pos, int threshold) {
        int max = ddp.getDMaximum();
        int min = ddp.getDMinimum();
        if (((max - min) / threshold) < 3) threshold = (max - min) / 3;
        if (pos < min) {
            return Pos.beyondMin;
        } else if (pos < min + threshold ) {
            return Pos.min;
        } else if (pos <= max - threshold) {
            return Pos.middle;
        } else if (pos <= max) {
            return Pos.max;
        } else {
            return Pos.beyondMax;
        }
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

            if (e.getWheelRotation() < 0) {
                nmin = 0.20; // zoom in
                nmax = 0.80;
            } else {
                nmin = -0.25; // zoom out
                nmax = 1.25;
            }
            switch (xpos) {
                case min:
                    xshift = -nmin;
                    break;
                case max:
                    xshift = nmin;
                    break;
            }
            switch (ypos) {
                case min:
                    yshift = nmin;
                    break;
                case max:
                    yshift = -nmin;
                    break;
            }
        }

        //int clickMag= Math.abs(e.getWheelRotation());
        int clickMag = 1;
        final long t1 = System.nanoTime();
        long limitNanos = (long) 40e6;
        if ((t1 - t0) / clickMag < limitNanos) {
            clickMag = (int) Math.floor( (t1 - t0) / limitNanos );
        }
        
        if (clickMag == 0) return;
        t0 = System.nanoTime();

        // these will be non-null if they should be used.
        DatumRange xdrnew=null;
        DatumRange ydrnew=null;

        //System.err.println(":ns:  "+(System.nanoTime()-tbirth)+"  "+clickMag);
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

    private void doPan(final MouseEvent e) {
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
            dr= maybeRound( xAxis, dr );
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
