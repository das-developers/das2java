package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.datum.*;

/** A TickVDescriptor describes the position that ticks
 * should be drawn, so that a fairly generic tick drawing routine
 * can be used for multiple types of axes.
 *
 */
public class TickVDescriptor {
    
    double[] tickV = null;
    double[] minorTickV = null;
    Units units = null;
    
    
    /** This constructor is to support the use when tickVDescriptor was
     * internal to DasAxis.
     */    
    protected TickVDescriptor() {
    }
    
    public TickVDescriptor( double[] minorTicks, double[] ticks, Units units ) {
        this.tickV= ticks;
        this.minorTickV= minorTicks;
        this.units= units;
    }
    
    /** Returns a String representation of the TickVDescriptor.
     * @return a String representation of the TickVDescriptor.
     *
     */
    public String toString() {
        String s="tickV=[";
        for (int i=0; i<tickV.length; i++) s+=tickV[i]+", ";
        s+="],minor=";
        for (int i=0; i<minorTickV.length; i++) s+=minorTickV[i]+", ";
        s+="]";
        return s;
    }
    
}

