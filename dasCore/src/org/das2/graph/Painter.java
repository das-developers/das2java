
package org.das2.graph;

import java.awt.Graphics2D;

/**
 * interface for objects that can paint on a graphics context from the canvas.
 * We introduce the interface so that objects can be painted without having
 * the overhead of full Swing components.
 * @author jbf
 */
public interface Painter {
    
    /**
     * the graphics context, in the canvas coordinates, is provided for additional
     * painting.
     * @param g the graphics context.
     */
    void paint( Graphics2D g );
}
