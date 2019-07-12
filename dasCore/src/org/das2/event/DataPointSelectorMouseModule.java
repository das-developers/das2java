/*
 * DataPointSelectorMouseModule.java
 *
 * Created on November 3, 2005, 2:53 PM
 *
 *
 */

package org.das2.event;
import org.das2.dataset.DataSetConsumer;
import org.das2.datum.Datum;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * General purpose mouse module for getting data point selections.  The client
 * provides the DragRenderer, generally a vertical line, horizontal line or a
 * crosshair.
 *
 * Three properties control when DataPointSelectionEvents are to be fired:
 *   dragEvents     as the mouse is dragged,
 *   keyEvents      when a key is pressed.  (The key is the "keyChar" plane of the event)
 *   releaseEvents  when the mouse is released.  (false by default)
 *
 * @see CrossHairRenderer
 * @author Jeremy
 */
public class DataPointSelectorMouseModule extends MouseModule {
    DasAxis xaxis, yaxis;
    
    javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
    
    MousePointSelectionEvent lastMousePoint;
    
    public DataPointSelectorMouseModule( DasPlot parent,
            DataSetConsumer consumer,
            DragRenderer dragRenderer, String label ) {
        super( parent, dragRenderer, label );
        this.xaxis= parent.getXAxis();
        this.yaxis= parent.getYAxis();
    }
    
    private DataPointSelectionEvent getDataPointSelectionEvent(MousePointSelectionEvent e) {
        Datum x= xaxis.invTransform(e.getX());
        Datum y= yaxis.invTransform(e.getY());
        DataPointSelectionEvent de= new DataPointSelectionEvent( this, x, y );
        return de;
    }
    
    @Override
    public void mousePointSelected(MousePointSelectionEvent e) {
        lastMousePoint= e;
        if ( keyEvents ) parent.requestFocus();
        if ( dragEvents ) fireDataPointSelectionListenerDataPointSelected(getDataPointSelectionEvent(e));
    }
    
    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode= e.getKeyCode();
        
        if ( lastMousePoint!=null ) {
            if ( keyCode==KeyEvent.VK_LEFT || keyCode==KeyEvent.VK_RIGHT || keyCode==KeyEvent.VK_UP || keyCode==KeyEvent.VK_DOWN ) {
                int x=0;
                int y=0;
                try {
                    int xOff= parent.getLocationOnScreen().x-parent.getX();
                    int yOff= parent.getLocationOnScreen().y-parent.getY();
                    final java.awt.Robot robot= new java.awt.Robot();
                    switch ( keyCode ) {
                        case KeyEvent.VK_LEFT:
                            robot.mouseMove(lastMousePoint.getX()+xOff-1, lastMousePoint.getY()+yOff);
                            break;
                        case KeyEvent.VK_RIGHT:
                            robot.mouseMove(lastMousePoint.getX()+xOff+1, lastMousePoint.getY()+yOff);
                            break;
                        case KeyEvent.VK_UP:
                            robot.mouseMove(lastMousePoint.getX()+xOff, lastMousePoint.getY()+yOff-1);
                            break;
                        case KeyEvent.VK_DOWN:
                            robot.mouseMove(lastMousePoint.getX()+xOff, lastMousePoint.getY()+yOff+1);
                            break;
                        default:
                            logger.log(Level.FINE, "keypress: {0}", keyCode);
                    }
                } catch ( java.awt.AWTException e1 ) {
                    logger.log(Level.SEVERE,null,e1);
                }
                
            } else {
                
                DataPointSelectionEvent dpse= getDataPointSelectionEvent(lastMousePoint);
                HashMap planes= new HashMap();
                planes.put( "keyChar", String.valueOf( e.getKeyChar() ) );
                dpse= new DataPointSelectionEvent( this, dpse.getX(), dpse.getY(), planes );
                fireDataPointSelectionListenerDataPointSelected( dpse );
            }
        }
    }
    
    /** Registers DataPointSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public void addDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        listenerList.add(org.das2.event.DataPointSelectionListener.class, listener);
    }
    
    /** Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public void removeDataPointSelectionListener(org.das2.event.DataPointSelectionListener listener) {
        listenerList.remove(org.das2.event.DataPointSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    protected void fireDataPointSelectionListenerDataPointSelected(DataPointSelectionEvent event) {
        Object[] listeners;
        listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==org.das2.event.DataPointSelectionListener.class) {
                ((org.das2.event.DataPointSelectionListener)listeners[i+1]).dataPointSelected(event);
            }
        }
    }
    
    /**
     * Holds value of property dragEvents.
     */
    private boolean dragEvents= true;
    
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
    private boolean keyEvents= true;
    
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
        if ( releaseEvents ) {
            fireDataPointSelectionListenerDataPointSelected(getDataPointSelectionEvent(lastMousePoint));
        }
    }
    
    /**
     * Holds value of property releaseEvents.
     */
    private boolean releaseEvents= false;
    
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
