package edu.uiowa.physics.pw.das.graph;

import java.awt.*;

public interface TickLabeller {
        /* Any initialization should be done here.  For example a synchronous data load might be initialized here.
         * (For asynchronous loads, some sort of update mechanism is intended.)
         */
    void init( TickVDescriptor ticks );
    
        /*
         * draw the tick label at the end (X2,Y2 ) of tickLine.  Return the bounding box
         * as the result.  This bounding box might be use to detect overlap of ticks labels,
         * and the overall bounds of the axis.
         */
    Rectangle labelMajorTick( Graphics g, int tickNumber, java.awt.geom.Line2D tickLine );
    
        /*
         * perform any cleanup operations here.
         */
    void finished();
}

