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

package org.das2.dataset;

import org.das2.datum.DatumVector;
import org.das2.datum.UnitsConverter;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 * The RebinDescriptor will quickly look up which 1-D bin a Datum is
 * in.  This is not thread-safe, and must be used by only one thread during its
 * lifetime.
 * @author  jbf
 */
public final class RebinDescriptor {
    
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
        if (start.getUnits()!=end.getUnits()) 
            throw new IllegalArgumentException(
                    "start and end units differ: \""+start.getUnits()+ "\" \"" +end.getUnits()+ "\"" 
            );
    }
    
    public int numberOfBins() {
        return nBin;
    }
    
    private UnitsConverter uc;  // cache UnitsConverter
    private Units inUnits=null; // cache units.
    
    public int whichBin( double x, Units units ) {
        if ( units!=this.units ) { 
            if ( uc==null || units!=inUnits ) {  // small optimization doesn't seem to have a large effect.
                uc= Units.getConverter(units,this.units);
                inUnits= units;
            }
            x= uc.convert(x);
        }
        int result=0;
        if (isLog) x= Math.log(x);
        if ((x<start || x>=end) && outOfBoundsAction!=EXTRAPOLATE) {
            switch (outOfBoundsAction) {
                case FIRSTORLAST:
                    result= x<start ? 0 : nBin-1;
                    break;
                case MINUSONE:
                    result= -1;
                    break;
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
        UnitsConverter cu= this.units.getConverter(units);
        double result= start+((ibin+0.5)/(double)(nBin)*(end-start));
        if ( isLog ) return cu.convert( Math.exp(result) ); else return cu.convert( result );
    }
    
    public Datum binCenter(int ibin) {
        return Datum.create( binCenter( ibin, units ), units );
    }
    
    public Datum binStart( int ibin ) {
        return Datum.create( binStart( ibin, units ), units );
    }
    
    /**
     * return the smaller boundary of the bin.
     * @param ibin the bin number
     * @param units the units for the result.
     * @return the smaller boundary of the bin in the desired units.
     */
    public double binStart( int ibin, Units units ) {
        if ( this.outOfBoundsAction!=RebinDescriptor.EXTRAPOLATE ) {
            if ( ibin<0 || ibin >= numberOfBins() ) {
                throw new IllegalArgumentException("bin "+ibin+" is out of bounds");
            }
        }
        double result= start+((ibin)/(double)(nBin)*(end-start));
        UnitsConverter cu= this.units.getConverter(units);
        if ( isLog ) {
            return cu.convert(Math.exp(result));
        } else {
            return cu.convert(result);
        }
    }
    
    public Datum binStop( int ibin ) {
        return Datum.create( binStop( ibin, units ), units );
    }
    
    /**
     * return the bigger boundary of the bin.
     * @param ibin the bin number
     * @param units the units for the result.
     * @return the bigger boundary of the bin in the desired units.
     */    
    public double binStop( int ibin, Units units ) {
        if ( this.outOfBoundsAction!=RebinDescriptor.EXTRAPOLATE ) {
            if ( ibin<0 || ibin >= numberOfBins() ) {
                throw new IllegalArgumentException("bin "+ibin+" is out of bounds");
            }
        }
        double result= start+((ibin+1)/(double)(nBin)*(end-start));
        UnitsConverter cu= this.units.getConverter(units);
        if ( isLog ) {
            return cu.convert(Math.exp(result));
        } else {
            return cu.convert(result);
        }
    }
    
    /**
     * return the bin starts of all bins, in units of <tt>getUnits()</tt>
     * @return the bin starts of all bins
     */
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
    
    /**
     * return the bin stops of all bins, in units of <tt>getUnits()</tt>
     * @return the bin stops of all bins
     */    
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
    
    public Datum binWidthDatum() {
        return Datum.create( binWidth(), getUnits().getOffsetUnits() );
    }
    
    public boolean isLog() {
        return isLog;
    }
    
    public Units getUnits() {
        return units;
    }

    /**
     * taken from AverageTableRebinner
     * @param ds the original dataset
     * @param result the rank 2 rebin target.
     * @param ddX the descriptor, or null.
     * @param ddY the descriptor, or null.
     */
    public static void putDepDataSet( QDataSet ds, MutablePropertyDataSet result, RebinDescriptor ddX, RebinDescriptor ddY ) {
        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        MutablePropertyDataSet xx;
        if ( ddX!=null ) {
            DDataSet xxx= DDataSet.createRank1( ddX.numberOfBins() );
            for ( int i=0; i<xxx.length(); i++ ) xxx.putValue(i, ddX.binCenter(i,ddX.units));
            xxx.putProperty( QDataSet.UNITS, ddX.units );
            xx= xxx;
        } else {
            xx= DataSetOps.makePropertiesMutable(xds); //TODO: untested branch
        }

        QDataSet yds= SemanticOps.ytagsDataSet(ds);
        MutablePropertyDataSet yy;
        if ( ddY!=null ) {
            DDataSet yyy= DDataSet.createRank1( ddY.numberOfBins() );
            for ( int i=0; i<yyy.length(); i++ ) yyy.putValue(i, ddY.binCenter(i,ddY.units));
            yyy.putProperty( QDataSet.UNITS, ddY.units );
            yy= yyy;
        } else {
            yy= DataSetOps.makePropertiesMutable( yds );
        }

        String[] props= new String[] { QDataSet.NAME, QDataSet.LABEL, QDataSet.TITLE };
        for ( String s: props ) {
            if ( xds!=null && xds.property(s)!=null ) xx.putProperty(s,xds.property(s));
            if ( yds!=null && yds.property(s)!=null ) yy.putProperty(s,yds.property(s));
        }
        for ( String s: org.das2.qds.DataSetUtil.dimensionProperties() ) {
            if ( ds.property(s)!=null ) result.putProperty(s,ds.property(s));
        }
        
        if (ddX != null) {
            xx.putProperty(QDataSet.CADENCE, org.das2.qds.DataSetUtil.asDataSet(ddX.binWidthDatum()) );
        }
        if (ddY != null) {
            yy.putProperty(QDataSet.CADENCE, org.das2.qds.DataSetUtil.asDataSet(ddY.binWidthDatum()) );
        }

        result.putProperty( QDataSet.DEPEND_0, xx );
        result.putProperty( QDataSet.DEPEND_1, yy );
        
    }
    
    @Override
    public String toString() {
        if ( isLog() ) {
            return "["+units.createDatum(Math.exp(start))+" - "+units.createDatum(Math.exp(end))+" in "+nBin+" bins "+(isLog?"Log":"")+"]";
        } else {
            return "["+units.createDatum(start)+" - "+units.createDatum(end)+" in "+nBin+" bins "+(isLog?"Log":"")+"]";
        }
        
    }
    
}
