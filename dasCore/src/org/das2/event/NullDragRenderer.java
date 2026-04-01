
package org.das2.event;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import org.das2.graph.DasCanvasComponent;

/**
 * Drag Renderer which doesn't draw anything during the drag.
 * @author jbf
 */
public class NullDragRenderer extends AbstractDragRenderer {

    public NullDragRenderer(){
        this(true);
    }
       
    public NullDragRenderer(DasCanvasComponent parent) {
        this(parent,true);
    }

    public NullDragRenderer( boolean dragEvents ){
        super(dragEvents);
    }
       
    public NullDragRenderer(DasCanvasComponent parent, boolean dragEvents ) {
        super(parent,dragEvents);
    }   
    
    /**
     * don't draw anything during the drag.
     * @param g the graphics context
     * @param p1 the initial press point.
     * @param p2 the current mouse point during the drag
     * @return 
     */
    @Override
    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {
        return null;
    }
    
}
