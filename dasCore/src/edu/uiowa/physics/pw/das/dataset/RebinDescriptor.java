/* File: RebinDescriptor.java
 * Copyright (C) 2002-2003 University of Iowa
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

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;

/**
 *
 * @author  jbf
 */
public class RebinDescriptor {
    
    Units units;
    protected double start = 0.;
    protected double end = 10.;
    protected int nBin = 10;
    protected boolean isLog = false;
    
    public static final int FIRSTORLAST=-2;  // return the closest valid bin, first or last
    public static final int MINUSONE= -3;    // return sentinel -1.
    public static final int EXTRAPOLATE= -4; // return negative or >nBin.
    
    private int outOfBoundsAction= MINUSONE;
    
    /** Creates a new instance of RebinDescriptor */
    public RebinDescriptor() {
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
        this(start.getValue(),end.getValue(),start.getUnits(),nBin,isLog);
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
    
    public void setOutOfBoundsAction(int action) {
        outOfBoundsAction= action;
    }
    
    public double binWidth() {
        return (end-start)/(double)nBin;
    }
    
    public Units getUnits() {
        return units;
    }
    
    public String toString() {
        return "["+start+"-"+end+" in "+nBin+" bins "+(isLog?"Log":"")+"]";
    }
    
}
