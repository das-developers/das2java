/*
 * DasCanvasComponentInterface.java
 *
 * Created on November 4, 2004, 5:26 PM
 */

package edu.uiowa.physics.pw.das.graph;

import java.awt.*;

/**
 * All entities on the DasCanvas are DasCanvasComponents.
 * @author Jeremy
 */
public interface DasCanvasComponentInterface {
    
    /**
     * this paints the component, the point 0,0 always refers to the upper-left corner
     * of the canvas.
     * @param g
     */
    void paintComponent( Graphics g );
    
    /**
     * This is called when the canvas is resized or something has happened to make the
     * boundries change.  This code should call the setBounds( Rectangle )
     */
    void resize();
    
    
    
}
