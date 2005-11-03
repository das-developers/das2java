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
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;

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
 * @author Jeremy
 */
public class DataPointSelectorMouseModule extends MouseModule {
    DasAxis xaxis, yaxis;
    DataSetConsumer dataSetConsumer;
    javax.swing.event.EventListenerList listenerList =  null;
    MousePointSelectionEvent lastMousePoint;
    
    public DataPointSelectorMouseModule( DasPlot parent,
            DataSetConsumer consumer,
            DragRenderer dragRenderer, String label ) {
        super( parent, dragRenderer, label );
        this.xaxis= parent.getXAxis();
        this.yaxis= parent.getYAxis();
        this.dataSetConsumer= consumer;
        parent.addKeyListener(getKeyListener());
    }
    
    private DataPointSelectionEvent getDataPointSelectionEvent(MousePointSelectionEvent e) {
        Datum x= xaxis.invTransform(e.getX());
        Datum y= yaxis.invTransform(e.getY());
        DataPointSelectionEvent de= new DataPointSelectionEvent( this, x, y );
        return de;
    }
    
    public void mousePointSelected(MousePointSelectionEvent e) {
        lastMousePoint= e;
        if ( keyEvents ) parent.requestFocus();
        if ( dragEvents ) fireDataPointSelectionListenerDataPointSelected(getDataPointSelectionEvent(e));
    }
    
    private KeyListener getKeyListener() {
        return new KeyListener() {
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
                            }
                        } catch ( java.awt.AWTException e1 ) {
                            edu.uiowa.physics.pw.das.util.DasDie.println(e1.getMessage());
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
            
            public void keyReleased(KeyEvent e) {
            }
            
            public void keyTyped(KeyEvent e) {
            }
        };
    }
    
    /** Registers DataPointSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addDataPointSelectionListener(edu.uiowa.physics.pw.das.event.DataPointSelectionListener listener) {
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }
    
    /** Removes DataPointSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeDataPointSelectionListener(edu.uiowa.physics.pw.das.event.DataPointSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    protected void fireDataPointSelectionListenerDataPointSelected(DataPointSelectionEvent event) {
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.DataPointSelectionListener.class) {
                ((edu.uiowa.physics.pw.das.event.DataPointSelectionListener)listeners[i+1]).DataPointSelected(event);
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
