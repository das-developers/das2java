/* File: RebinDescriptor.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author  jbf
 */
public class RebinDescriptor {
    
    Units units;
    protected double start;
    protected double end;
    protected int nBin;
    protected boolean isLog = false;
    
    public static final int FIRSTORLAST=-2;  // return the closest valid bin, first or last
    public static final int MINUSONE= -3;    // return sentinel -1.
    public static final int EXTRAPOLATE= -4; // return negative or >nBin.
    
    private int outOfBoundsAction= MINUSONE;
    
    /** Creates a new instance of RebinDescriptor */
    private RebinDescriptor() {
    }
    
    public RebinDescriptor(double start, double end, Units units, int nBin, boolean isLog) {
        this.units= units;
        if (isLog) {
            this.start= Math.log(start);
            this.end= Math.log(end);
        } else {
            this.start= start;
            this.end= end;
        }
        this.nBin= nBin;
        this.isLog= isLog;
    }
    
    public RebinDescriptor( Datum start, Datum end, int nBin, boolean isLog) {
        this(start.doubleValue(start.getUnits()),end.doubleValue(end.getUnits()),start.getUnits(),nBin,isLog);
        if (start.getUnits()!=end.getUnits()) throw new IllegalArgumentException("start and end units differ");
    }
    
    public int numberOfBins() {
        return nBin;
    }
    
    public int whichBin( double x, Units units ) {
        if ( units!=this.units ) {
            x= Units.getConverter(units,this.units).convert(x);
        }
        int result=0;
        if (isLog) x= Math.log(x);
        if ((x<start || x>=end) && outOfBoundsAction!=EXTRAPOLATE) {
            switch (outOfBoundsAction) {
                case FIRSTORLAST:
                    result= x<start ? 0 : nBin-1;
                case MINUSONE:
                    result= -1;
            }
        } else {
            result= (int)((x-start)*nBin/(end-start));
        }
        return result;
    }
    
    public double[] binCenters() {
        double [] result= new double[nBin];
        for (int i=0; i<nBin; i++) {
            result[i]= start+((i+0.5)/(float)(nBin)*(end-start));
        }
        if (isLog) {
            for (int i=0; i<nBin; i++) result[i]= Math.exp(result[i]);
        }
        return result;
    }
    
    public double binCenter(int ibin) {
        double result= start+((ibin+0.5)/(double)(nBin)*(end-start));
        if ( isLog ) return Math.exp(result); else return result;
    }
    
    public double binStart( int ibin, Units units ) {
        double result= start+((ibin)/(double)(nBin)*(end-start));
        UnitsConverter uc= this.units.getConverter(units);
        if ( isLog ) {
            return uc.convert(Math.exp(result));
        } else {
            return uc.convert(result);
        }
    }
    
    public double binStop( int ibin, Units units ) {
        double result= start+((ibin+1)/(double)(nBin)*(end-start));
        UnitsConverter uc= this.units.getConverter(units);
        if ( isLog ) {
            return uc.convert(Math.exp(result));
        } else {
            return uc.convert(result);
        }
    }
    
    public double[] binStarts() {
        double [] result= new double[nBin];
        for (int i=0; i<nBin; i++) {
            result[i]= start+((i)/(float)(nBin)*(end-start));
        }
        if (isLog) {
            for (int i=0; i<nBin; i++) result[i]= Math.exp(result[i]);
        }
        return result;
    }
    
    public double[] binStops() {
        double [] result= new double[nBin];
        for (int i=0; i<nBin; i++) {
            result[i]= start+((i+1)/(float)(nBin)*(end-start));
        }
        if (isLog) {
            for (int i=0; i<nBin; i++) result[i]= Math.exp(result[i]);
        }
        return result;
    }
    
    private void setOutOfBoundsAction(int action) {
        outOfBoundsAction= action;
    }
    
    private Object clone( int outOfBoundsAction ) {
        RebinDescriptor result= new RebinDescriptor();
        result.units= this.units;
        result.start= this.start;
        result.end= this.end;
        result.nBin= this.nBin;
        result.isLog= this.isLog;
        result.outOfBoundsAction= outOfBoundsAction;
        return result;
    }
    
    /* create new rebinDescriptor that includes ddY plus additional channels to include ymin to ymax */
    public static RebinDescriptor createSubsumingRebinDescriptor( RebinDescriptor ddY, Datum ymin, Datum ymax ) {
        if ( ddY==null ) return null;
        RebinDescriptor dd= (RebinDescriptor)ddY.clone( RebinDescriptor.EXTRAPOLATE );
        Units units= ddY.getUnits();
        int i0= dd.whichBin( ymin.doubleValue(units), units );
        if ( i0>0 ) {
            i0= 0;
            ymin= units.createDatum(ddY.binStart(0, units));
        }
        
        int i1= dd.whichBin( ymax.doubleValue(units), units );
        if ( i1<ddY.numberOfBins() ) {
            i1= ddY.numberOfBins();
            ymax= units.createDatum(ddY.binStop(ddY.numberOfBins()-1,units));
        }
            
        int nbins= i1-i0;                        
                    
        return new RebinDescriptor( ymin, ymax, nbins, ddY.isLog() );
    }
    
    public double binWidth() {
        return (end-start)/(double)nBin;
    }
    
    public boolean isLog() {
        return isLog;
    }
    
    public Units getUnits() {
        return units;
    }
    
    public String toString() {
        return "["+start+"-"+end+" in "+nBin+" bins "+(isLog?"Log":"")+"]";
    }
    
}
