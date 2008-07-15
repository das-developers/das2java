/*
 * DataPointSelectorMouseModule.java
 *
 * Created on November 3, 2005, 2:53 PM
 *
 *
 */
package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.dataset.DataSetConsumer;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.DatumRangeUtil;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;

/**
 * General purpose mouse module for getting data point selections.  The client
 * provides the DragRenderer, generally a vertical line, horizontal line or a
 * crosshair.
 *
 * Three properties control when BoxSelectionEvents are to be fired:
 *   dragEvents     as the mouse is dragged,
 *   keyEvents      when a key is pressed.  (The key is the "keyChar" plane of the event)
 *   releaseEvents  when the mouse is released.  (false by default)
 *
 * @see BoxRenderer
 * @author Jeremy
 */
public class BoxSelectorMouseModule extends MouseModule {

    DasAxis xaxis, yaxis;
    DataSetConsumer dataSetConsumer;
    javax.swing.event.EventListenerList listenerList = null;
    MouseDragEvent lastMouseEvent;
    /** when true, box selections are remembered, and tweaks to corners are allowed. */
    boolean tweakable = false;
    BoxSelectionEvent lastSelectionEvent = null;

    public BoxSelectorMouseModule(DasCanvasComponent parent, DasAxis xAxis, DasAxis yAxis,
            DataSetConsumer consumer,
            DragRenderer dragRenderer, String label) {
        super(parent, dragRenderer, label);
        this.xaxis = xAxis;
        this.yaxis = yAxis;
        this.dataSetConsumer = consumer;
    }

    /**
     * allow the last selection to be tweaked.  It's the client's responsibility
     * to draw the current selection.
     * @param b
     */
    public void setTweakable(boolean b) {
        this.tweakable = b;
    }

    private Datum[] checkTweak(Point p) {
        double nx = DatumRangeUtil.normalize(lastSelectionEvent.getXRange(), xaxis.invTransform(p.getX()));
        double ny = DatumRangeUtil.normalize(lastSelectionEvent.getYRange(), yaxis.invTransform(p.getY()));

        System.err.println("" + nx + " " + ny);
        Datum otherx = null;
        Datum othery = null;

        if (nx >= 0.0 && nx < 0.1) {
            otherx = lastSelectionEvent.getXRange().max();
        } else if (nx > 0.9 && nx < 1.0) {
            otherx = lastSelectionEvent.getXRange().min();
        }

        if (ny >= 0.0 && ny < 0.1) {
            othery = lastSelectionEvent.getYRange().max();
        } else if (ny > 0.9 && ny < 1.0) {
            othery = lastSelectionEvent.getYRange().min();
        }

        Datum[] otherCorner = new Datum[2];

        otherCorner[0] = otherx;
        otherCorner[1] = othery;

        return otherCorner;

    }

    @Override
    public void mousePressed(MouseEvent e) {
        
        if (tweakable && lastSelectionEvent != null) {
            Point p = new Point(e.getPoint());
            p.translate(e.getComponent().getX(), e.getComponent().getY());
            
            Datum[] otherCorner= checkTweak( p );
            if (otherCorner[0] != null && otherCorner[1] != null) { // we're tweaking
                double p1x = xaxis.transform(otherCorner[0]);
                double p2x = yaxis.transform(otherCorner[1]);
                Point p1 = new Point((int) p1x, (int) p2x);
                ((BoxRenderer) dragRenderer).setDragStart(p1);
            } else {
                ((BoxRenderer) dragRenderer).setDragStart(null);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Cursor c= null;
        if (tweakable && lastSelectionEvent != null) {
            Point p = new Point(e.getPoint());
            p.translate(e.getComponent().getX(), e.getComponent().getY());
            
            Datum[] otherCorner= checkTweak( p );
            if (otherCorner[0] != null && otherCorner[1] != null) { // we're tweaking
                c= new Cursor( Cursor.MOVE_CURSOR );
            }
        }        
        parent.getCanvas().setCursor(c);
    }

    private BoxSelectionEvent getBoxSelectionEvent(MouseDragEvent mde) {

        MouseBoxEvent e = (MouseBoxEvent) mde;

        DatumRange xrange = null;
        DatumRange yrange = null;

        if (xaxis != null) {
            Datum min = xaxis.invTransform(e.getXMinimum());
            Datum max = xaxis.invTransform(e.getXMaximum());
            if (min.gt(max)) {
                Datum t = min;
                min = max;
                max = t;
            }
            xrange = new DatumRange(min, max);
        }

        if (yaxis != null) {
            Datum min = yaxis.invTransform(e.getYMinimum());
            Datum max = yaxis.invTransform(e.getYMaximum());
            if (min.gt(max)) {
                Datum t = min;
                min = max;
                max = t;
            }
            yrange = new DatumRange(min, max);
        }

        BoxSelectionEvent evt = new BoxSelectionEvent(this, xrange, yrange);
        this.lastSelectionEvent = evt;
        return evt;
    }

    public void mouseRangeSelected(MouseDragEvent e) {
        lastMouseEvent = e;
        if (keyEvents) {
            parent.requestFocus();
        }
        if (dragEvents) {
            fireBoxSelectionListenerBoxSelected(getBoxSelectionEvent(e));
        }
    }

    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (lastMouseEvent != null) {
            BoxSelectionEvent dpse = getBoxSelectionEvent(lastMouseEvent);
            HashMap planes = new HashMap();
            planes.put("keyChar", String.valueOf(e.getKeyChar()));
            dpse = new BoxSelectionEvent(this, dpse.getXRange(), dpse.getYRange(), planes);
            fireBoxSelectionListenerBoxSelected(dpse);
        }
    }

    /** Registers BoxSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addBoxSelectionListener(edu.uiowa.physics.pw.das.event.BoxSelectionListener listener) {
        if (listenerList == null) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.BoxSelectionListener.class, listener);
    }

    /** Removes BoxSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeBoxSelectionListener(edu.uiowa.physics.pw.das.event.BoxSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.BoxSelectionListener.class, listener);
    }

    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    protected void fireBoxSelectionListenerBoxSelected(BoxSelectionEvent event) {
        if (listenerList == null) {
            return;
        }
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == edu.uiowa.physics.pw.das.event.BoxSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.BoxSelectionListener) listeners[i + 1]).BoxSelected(event);
            }
        }
    }
    /**
     * Holds value of property dragEvents.
     */
    private boolean dragEvents = false;

    /**
     * Getter for property dragEvents.
     * @return Value of property dragEvents.
     */
    public boolean isDragEvents() {
        return this.dragEvents;
    }

    /**
     * Setter for property dragEvents.
     * @param dragEvents New value of property dragEvents.
     */
    public void setDragEvents(boolean dragEvents) {
        this.dragEvents = dragEvents;
    }
    /**
     * Holds value of property keyEvents.
     */
    private boolean keyEvents = false;

    /**
     * Getter for property keyEvents.
     * @return Value of property keyEvents.
     */
    public boolean isKeyEvents() {

        return this.keyEvents;
    }

    /**
     * Setter for property keyEvents.
     * @param keyEvents New value of property keyEvents.
     */
    public void setKeyEvents(boolean keyEvents) {
        this.keyEvents = keyEvents;
    }

    public void mouseReleased(java.awt.event.MouseEvent e) {
        super.mouseReleased(e);
        if (releaseEvents) {
            fireBoxSelectionListenerBoxSelected(getBoxSelectionEvent(lastMouseEvent));
        }
    }
    /**
     * Holds value of property releaseEvents.
     */
    private boolean releaseEvents = true;

    /**
     * Getter for property releaseEvents.
     * @return Value of property releaseEvents.
     */
    public boolean isReleaseEvents() {

        return this.releaseEvents;
    }

    /**
     * Setter for property releaseEvents.
     * @param releaseEvents New value of property releaseEvents.
     */
    public void setReleaseEvents(boolean releaseEvents) {

        this.releaseEvents = releaseEvents;
    }
}
