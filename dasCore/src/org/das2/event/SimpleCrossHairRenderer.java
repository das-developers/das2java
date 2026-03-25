
package org.das2.event;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import org.das2.components.propertyeditor.Editable;

/**
 * Draw a cross hair.  By default, this will send off update events during the drag.  If the
 * mouse module also 
 * @author jbf
 */
public class SimpleCrossHairRenderer extends AbstractDragRenderer implements DragRenderer, Editable { 
    
    /**
     * create the SimpleCrossHairRenderer, requesting events be fired during the drag.
     */
    public SimpleCrossHairRenderer( ) {
        this(true);
    }
    
    /**
     * create the SimpleCrossHairRenderer, possibly firing events.
     * @param dragEvents if true, will request that events be fired during the drag.
     */
    public SimpleCrossHairRenderer( boolean dragEvents ) {
        super(dragEvents);
    }

    @Override
    public Rectangle[] renderDrag(Graphics g, Point p1, Point p2) {
        Rectangle clip= g.getClipBounds();
        g.drawLine( clip.x, p2.y, clip.x+clip.width, p2.y );
        g.drawLine( p2.x, clip.y, p2.x, clip.y+clip.height );
        return null;
    }
}
