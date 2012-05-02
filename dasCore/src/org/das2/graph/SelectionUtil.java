/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 *
 * @author jbf
 */
public class SelectionUtil {

    public static final Shape NULL= new GeneralPath();

    /**
     * returns the selection area, or SelectionUtil.NULL.
     * @param r
     * @return
     */
    public static Shape getSelectionArea( Renderer r ) {
        if ( r instanceof SeriesRenderer ) {
            Shape a= ((SeriesRenderer)r).selectionArea();
            return a;
        } else if ( r instanceof SpectrogramRenderer ) {
            return ((SpectrogramRenderer)r).selectionArea();
        } else if ( r instanceof DigitalRenderer ) {
            return ((DigitalRenderer)r).selectionArea();
        } else if ( r instanceof EventsRenderer ) {
            return ((EventsRenderer)r).selectionArea();
        } else if ( r instanceof ImageVectorDataSetRenderer ) {
            return ((ImageVectorDataSetRenderer)r).selectionArea();
        } else {
            Rectangle rect= DasDevicePosition.toRectangle( r.getParent().getRow(), r.getParent().getColumn() );
            rect= new Rectangle( rect.x+5, rect.y+5, rect.width-10, rect.height-10 );
            return rect;
        }
    }

    public static Shape getSelectionArea( DasCanvasComponent cc ) {
        return cc.getActiveRegion();
    }
    
}
