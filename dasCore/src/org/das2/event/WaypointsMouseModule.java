
package org.das2.event;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import static org.das2.event.MouseModule.logger;
import org.das2.graph.DasCanvasComponent;
import org.das2.graph.DasPlot;
import org.das2.qds.QDataSet;

/**
 * allows a thick highway to be used for digitizing, to be used with 
 * WaypointsDragRenderer.
 * 
 * @author jbf
 */
public class WaypointsMouseModule extends MouseModule {
    
    public WaypointsMouseModule( DasCanvasComponent parent, String label ) {
        super( parent );
        WaypointsDragRenderer dr= new WaypointsDragRenderer(parent);
        this.dragRenderer= dr;
        this.setLabel(label);
        dr.setParent(parent);
    }
            
    @Override
    public String getDirections() {
        WaypointsDragRenderer r= (WaypointsDragRenderer)super.getDragRenderer();
        r.clear(null);
        return "Press P to pin, 1-9 set thickness, w to add a way point.";
    }

    @Override
    public void keyTyped(KeyEvent keyEvent) {
        logger.log(Level.FINE, "keyTyped {0} {1}", new Object[]{keyEvent.getKeyChar(), keyEvent.isMetaDown()});
        if ( keyEvent.getKeyChar()>='1' && keyEvent.getKeyChar()<='9' ) { 
            WaypointsDragRenderer r= (WaypointsDragRenderer)super.getDragRenderer();
            r.setWidth( ( keyEvent.getKeyChar()-'0' ) * 5 );
            parent.repaint();
        } else if ( keyEvent.getKeyChar()=='w' ) { 
            WaypointsDragRenderer r= (WaypointsDragRenderer)super.getDragRenderer();
            r.addWayPoint( );
            parent.repaint();
        }
    }
    
    private javax.swing.event.EventListenerList listenerList = new javax.swing.event.EventListenerList();
    
    /** Registers BoxSelectionListener to receive events.  Though a BoxSelectionEvent is 
     * fired, clients should use the whereWithin method to query which points are within the 
     * bounding box drawn.
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
    @Override
    public void mouseRangeSelected(MouseDragEvent e) {
        Rectangle boundingBox= ((WaypointsDragRenderer)getDragRenderer()).getBoundingBox();
        DasPlot p= (DasPlot)getParent();
        DatumRange xrange= new DatumRange( p.getXAxis().invTransform( boundingBox.x ), 
            p.getXAxis().invTransform( boundingBox.x+boundingBox.width ) );
        DatumRange yrange= DatumRangeUtil.union( p.getYAxis().invTransform( boundingBox.y ), 
            p.getYAxis().invTransform( boundingBox.y+boundingBox.height ) );
        
        BoxSelectionEvent evt= new BoxSelectionEvent( parent, xrange, yrange );
            
        fireBoxSelectionListenerBoxSelected(evt);
    }

    /**
     * returns a list of indices where the rank 1 yy is within the pathway, or null if none are found.
     * @param xx the rank 0 point for the data
     * @param yy the rank 1 data points
     * @return a list of indices into yy.
     */
    public QDataSet whereWithin( QDataSet xx, QDataSet yy ) {
        if ( getParent() instanceof DasPlot ) {
            DasPlot p= (DasPlot)getParent();
            QDataSet result= ((WaypointsDragRenderer)getDragRenderer()).whereWithin( p.getXAxis(), p.getYAxis(), xx, yy );
            return result;
        } else {
            throw new IllegalArgumentException("parent must be a DasPlot to use whereWithin");
        }
    }
    
}
