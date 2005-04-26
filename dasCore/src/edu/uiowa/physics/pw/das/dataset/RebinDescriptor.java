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
    
    private int outOfBoundsAction= EXTRAPOLATE;
    
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
    
    public DatumVector binCentersDV() {
        double [] result= binCenters();
        return DatumVector.newDatumVector(result, units);
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
    
    public double binCenter(int ibin,Units units) {
        UnitsConverter uc= this.units.getConverter(units);
        double result= start+((ibin+0.5)/(double)(nBin)*(end-start));
        if ( isLog ) return uc.convert( Math.exp(result) ); else return uc.convert( result );
    }
    
    public Datum binCenter(int ibin) {
        return Datum.create( binCenter( ibin, units ), units );
    }
    
    public Datum binStart( int ibin ) {
        return Datum.create( binStart( ibin, units ), units );
    }
    
    public double binStart( int ibin, Units units ) {
        if ( this.outOfBoundsAction!=RebinDescriptor.EXTRAPOLATE ) {
            if ( ibin<0 || ibin >= numberOfBins() ) {
                throw new IllegalArgumentException("bin "+ibin+" is out of bounds");
            }
        }
        double result= start+((ibin)/(double)(nBin)*(end-start));
        UnitsConverter uc= this.units.getConverter(units);
        if ( isLog ) {
            return uc.convert(Math.exp(result));
        } else {
            return uc.convert(result);
        }
    }
    
    public Datum binStop( int ibin ) {
        return Datum.create( binStop( ibin, units ), units );
    }
    
    public double binStop( int ibin, Units units ) {
        if ( this.outOfBoundsAction!=RebinDescriptor.EXTRAPOLATE ) {
            if ( ibin<0 || ibin >= numberOfBins() ) {
                throw new IllegalArgumentException("bin "+ibin+" is out of bounds");
            }
        }
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
    
    public void setOutOfBoundsAction(int action) {
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
        if ( i0< -10000000 ) {
            throw new IllegalArgumentException( "ymin would result in impossibly large rebin descriptor (ymin="+ymin+" falls in bin number "+i0+")" );
        }
        
        int i1= dd.whichBin( ymax.doubleValue(units), units );
        if ( i1<dd.numberOfBins() ) {
            i1= dd.numberOfBins();
            ymax= units.createDatum(dd.binStop(dd.numberOfBins()-1,units));
        }
        
        if ( i0> 10000000 ) {
            throw new IllegalArgumentException( "ymax would result in impossibly large rebin descriptor (ymax="+ymax+" falls in bin number "+i0+")" );
        }
        
        int nbins= i1-i0+1;
        
        return new RebinDescriptor( units.createDatum(dd.binStart(i0,units)), units.createDatum(dd.binStop(i1,units)), nbins, dd.isLog() );
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
        return "["+units.createDatum(start)+" - "+units.createDatum(end)+" in "+nBin+" bins "+(isLog?"Log":"")+"]";
    }
    
}
