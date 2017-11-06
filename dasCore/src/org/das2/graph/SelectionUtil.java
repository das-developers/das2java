/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.graph;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;

/**
 * utilities for managing selection feedback, where we have a glow to indicate 
 * focus.
 * @author jbf
 */
public class SelectionUtil {

    public static final Shape NULL= new GeneralPath();

    /**
     * returns the selection area, or SelectionUtil.NULL.
     * @param r the renderer, one of a list of coded renderers
     * @return the renderer-specific selection area, or the current bounds of the renderer parent.
     * TODO: this should be part of the Renderer interface.
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
        } else if ( r instanceof RGBImageRenderer ) {
            return ((RGBImageRenderer)r).selectionArea();
        } else if ( r instanceof PolarPlotRenderer ) {
            return ((PolarPlotRenderer)r).selectionArea();
        } else if ( r instanceof TickCurveRenderer ) {
            return ((TickCurveRenderer)r).selectionArea();
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
