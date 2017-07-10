/*
 * DataPointSelectorMouseModule.java
 *
 * Created on November 3, 2005, 2:53 PM
 *
 *
 */
package org.das2.event;

import java.util.logging.Level;
import org.das2.dataset.DataSetConsumer;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasPlot;
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
 * <ul>
 * <li> dragEvents,     as the mouse is dragged,
 * <li> keyEvents,      when a key is pressed.  (The key is the "keyChar" plane of the event)
 * <li> releaseEvents,  when the mouse is released.  (false by default)
 * </ul>
 * This is intended to be used as a base class for other slicers which need a
 * range to be selected in X, Y, or both.
 * @see BoxRenderer
 * @author Jeremy
 */
public class BoxSelectorMouseModule extends MouseModule {

    DasAxis xaxis, yaxis;
    DataSetConsumer dataSetConsumer;
    javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
    MouseDragEvent lastMouseEvent;
    /** when true, box selections are remembered, and tweaks to corners are allowed. */
    boolean tweakable = false;
    BoxSelectionEvent lastSelectionEvent = null;

    /**
     * create a BoxSelectorMouseModule
     * @param parent the plot component
     * @param label the label for this mouseModule.
     * @return new  BoxSelectorMouseModule
     */
    public static BoxSelectorMouseModule create( DasPlot parent, String label ) {
        return new BoxSelectorMouseModule( parent, parent.getXAxis(), parent.getYAxis(), null, new BoxRenderer(parent,true), label );
    }
    
    /**
     * create a new BoxSelectorMouseModule
     * @param parent the parent component
     * @param xAxis
     * @param yAxis
     * @param consumer used by some subclasses, such as CutoffMouseModule.
     * @param dragRenderer the drag renderer to use, typically a BoxRenderer but needn't be.
     * @param label the label for this mouseModule.
     */
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

        logger.log(Level.FINE, "{0} {1}", new Object[]{nx, ny});
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

        Datum x=null, y=null;
        Datum sx= null, sy=null;
        
        if (xaxis != null) {
            Datum min = xaxis.invTransform(e.getXMinimum());
            Datum max = xaxis.invTransform(e.getXMaximum());
            if (min.gt(max)) {
                Datum t = min;
                min = max;
                max = t;
            }
            xrange = new DatumRange(min, max);
            x= xaxis.invTransform(e.getPoint().x);
            sx= xaxis.invTransform(e.getPressPoint().x);
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
            y= yaxis.invTransform( e.getPoint().y );
            sy= yaxis.invTransform( e.getPressPoint().y );
        }

        BoxSelectionEvent evt = new BoxSelectionEvent(this, xrange, yrange);
        evt.setStart( sx,sy );
        evt.setFinish( x,y );
        
        
        this.lastSelectionEvent = evt;
        return evt;
    }

    @Override
    public void mouseRangeSelected(MouseDragEvent e) {
        lastMouseEvent = e;
        if (keyEvents) {
            parent.requestFocus();
        }
        if (dragEvents) {
            fireBoxSelectionListenerBoxSelected(getBoxSelectionEvent(e));
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

        if (lastMouseEvent != null) {
            BoxSelectionEvent dpse = getBoxSelectionEvent(lastMouseEvent);
            HashMap planes = new HashMap();
            planes.put("keyChar", String.valueOf(e.getKeyChar()));
            BoxSelectionEvent dpse2 = new BoxSelectionEvent(this, dpse.getXRange(), dpse.getYRange(), planes);
            dpse2.setStart( dpse.getStartX(), dpse.getStartY() );
            dpse2.setFinish( dpse.getFinishX(), dpse.getFinishY() );
            fireBoxSelectionListenerBoxSelected(dpse2);
        }
    }

    /** Registers BoxSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addBoxSelectionListener(org.das2.event.BoxSelectionListener listener) {
        listenerList.add(org.das2.event.BoxSelectionListener.class, listener);
    }

    /** Removes BoxSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeBoxSelectionListener(org.das2.event.BoxSelectionListener listener) {
        listenerList.remove(org.das2.event.BoxSelectionListener.class, listener);
    }

    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    protected void fireBoxSelectionListenerBoxSelected(BoxSelectionEvent event) {
        Object[] listeners;
        listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == org.das2.event.BoxSelectionListener.class) {
                ((org.das2.event.BoxSelectionListener) listeners[i + 1]).boxSelected(event);
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

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        super.mouseReleased(e);
        if (releaseEvents) {
            if ( lastMouseEvent!=null ) {
                fireBoxSelectionListenerBoxSelected(getBoxSelectionEvent(lastMouseEvent));
            } else {
                logger.fine("no mouse event to fire");
            }
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
