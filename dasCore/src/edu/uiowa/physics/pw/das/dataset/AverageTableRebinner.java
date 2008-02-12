/* File: TableAverageRebinner.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 5, 2003, 10:31 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
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
import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.util.*;
import java.util.logging.*;

/**
 *
 * @author  Edward West
 */
public class AverageTableRebinner implements DataSetRebinner {
    
    private static Logger logger = DasLogger.getLogger( DasLogger.DATA_OPERATIONS_LOG );
    
    /**
     * Holds value of property interpolate.
     */
    private boolean interpolate= true;
    private boolean enlargePixels = true;
    
    /** Creates a new instance of TableAverageRebinner */
    public AverageTableRebinner() {
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException, DasException {
        logger.finest("enter AverageTableRebinner.rebin");
        
        if (ds == null) {
            throw new NullPointerException("null data set");
        }
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException("Data set must be an instanceof TableDataSet: " + ds.getClass().getName());
        }
        TableDataSet tds = (TableDataSet)ds;
        TableDataSet weights = (TableDataSet)ds.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        if (ddX != null && tds.getXLength() > 0) {
            double start = tds.getXTagDouble(0, ddX.getUnits());
            double end = tds.getXTagDouble(tds.getXLength() - 1, ddX.getUnits());
            if (start > ddX.end ) {
                throw new NoDataInIntervalException("data starts after range");
            } else if ( end < ddX.start ) {
                throw new NoDataInIntervalException("data ends before range");
            }
        }
        
        long timer= System.currentTimeMillis();
        
        Units xunits= ddX.getUnits();
        
        int nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        logger.finest("Allocating rebinData and rebinWeights: " + nx + " x " + ny);
        
        double[][] rebinData= new double[nx][ny];
        double[][] rebinWeights= new double[nx][ny];
        
        average( tds, weights, rebinData, rebinWeights, ddX, ddY );
        if ( interpolate ) {
            doBoundaries2RL( tds, weights, rebinData, rebinWeights, ddX, ddY );
            doBoundaries2TB( tds, weights, rebinData, rebinWeights, ddX, ddY );
            doCorners( tds,  weights, rebinData, rebinWeights, ddX, ddY );
        }
        
        double[] xTags;
        if (ddX != null) {
            xTags = ddX.binCenters();
        } else {
            xTags = new double[nx];
            for (int i = 0; i < nx; i++) {
                xTags[i] = tds.getXTagDouble(i, tds.getXUnits());
            }
        }
        double[][] yTags;
        if (ddY != null) {
            yTags = new double[][]{ddY.binCenters()};
        } else {
            yTags = new double[1][ny];
            for (int j = 0; j < ny; j++) {
                yTags[0][j] = tds.getYTagDouble(0, j, tds.getYUnits());
            }
        }
        
        Units resultXUnits= ddX==null ? tds.getXUnits() : ddX.getUnits();
        Units resultYUnits= ddY==null ? tds.getYUnits() : ddY.getUnits();
        
        if ( this.interpolate ) {
            Datum xTagWidth= (Datum)ds.getProperty("xTagWidth");
            if ( xTagWidth==null ) {
                xTagWidth= DataSetUtil.guessXTagWidth(tds);
            }
            double xTagWidthDouble= xTagWidth.doubleValue(ddX.getUnits().getOffsetUnits());
            
            Datum yTagWidth= (Datum)ds.getProperty("yTagWidth");
            
            if ( ddX!=null ) fillInterpolateX(rebinData, rebinWeights, xTags, xTagWidthDouble );
            if ( ddY!=null ) fillInterpolateY(rebinData, rebinWeights, ddY, yTagWidth );
        } else if (enlargePixels) {
            enlargePixels( rebinData, rebinWeights );
        }
        
        double[][][] zValues = {rebinData,rebinWeights};
        
        int[] tableOffsets = {0};
        Units[] zUnits = {tds.getZUnits(), Units.dimensionless};
        String[] planeIDs = {"", DataSet.PROPERTY_PLANE_WEIGHTS};
        
        Map properties= new HashMap(ds.getProperties());
        
        if ( ddX!=null ) properties.put( DataSet.PROPERTY_X_TAG_WIDTH, ddX.binWidthDatum() );
        if ( ddY!=null ) properties.put( DataSet.PROPERTY_Y_TAG_WIDTH, ddY.binWidthDatum() );
        
        TableDataSet result= new DefaultTableDataSet( xTags, resultXUnits, yTags, resultYUnits, zValues, zUnits, planeIDs, tableOffsets, properties );
        logger.finest("done, AverageTableRebinner.rebin");
        return result;
    }
    
    static void doBoundaries2RL( TableDataSet tds, TableDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        Units yunits= tds.getYUnits();
        Units zunits= tds.getZUnits();
        Units wunits= Units.dimensionless;
        TableDataSet wds= WeightsTableDataSet.create(tds);
        for ( int i=0; i<2; i++ ) {
            int ix= i==0 ? 0 : ddX.numberOfBins()-1;
            Datum xx= i==0 ? ddX.binCenter(0) : ddX.binCenter(ix);
            
            int i0= DataSetUtil.getPreviousColumn( tds, xx );
            int i1= DataSetUtil.getNextColumn( tds, xx );
            
            int itable= tds.tableOfIndex(i0);
            if ( itable==tds.tableOfIndex(i1) && ( i1!=i0 ) ) {
                DatumRange dr= new DatumRange( tds.getXTagDatum(i0), tds.getXTagDatum(i1) );
                if ( dr.width().gt(DataSetUtil.guessXTagWidth(tds).multiply(0.9) ) ) {
                    double alpha=dr.normalize(xx);
                    int ny= ddY==null ? tds.getYLength(itable) : ddY.numberOfBins();
                    for ( int j=0; j<tds.getYLength(itable); j++ ) {
                        int jj= ddY==null ? j : ddY.whichBin( tds.getYTagDouble(itable,j,yunits), yunits );
                        if ( jj>=0 && jj<ny ) {
                            if ( rebinWeights[ix][jj]>0.0 ) continue;
                            if ( wds.getDouble(i0,j,wunits ) * wds.getDouble(i1,j,wunits ) == 0. ) continue;
                            rebinData[ix][jj]= ( 1-alpha ) * tds.getDouble(i0,j,zunits) +
                                    alpha * tds.getDouble(i1,j,zunits)  ;
                            rebinWeights[ix][jj]= 1.0;
                        }
                    }
                }
            }
        }
        
    }
    
    
    static void doBoundaries2TB( TableDataSet tds, TableDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        
        if ( ddY==null ) return;
        
        Units yunits= tds.getYUnits();
        Units zunits= tds.getZUnits();
        Units xunits= tds.getXUnits();
        Units wunits= Units.dimensionless;
        
        TableDataSet wds=  WeightsTableDataSet.create(tds);
        for ( int itable=0; itable<tds.tableCount(); itable++ ) {
            for ( int i=0; i<2; i++ ) {
                int iy= i==0 ? 0 : ddY.numberOfBins()-1;
                Datum yy= i==0 ? ddY.binCenter(0) : ddY.binCenter(iy);
                
                int j0= TableUtil.getPreviousRow( tds, itable, yy );
                int j1= TableUtil.getNextRow( tds, itable, yy );
                
                if ( j1!=j0 ) {
                    
                    DatumRange dr;
                    if ( tds.getYTagDatum(itable,j0).le( tds.getYTagDatum(itable,j1) ) ) {
                        dr= new DatumRange( tds.getYTagDatum(itable,j0), tds.getYTagDatum(itable,j1) );
                    } else {
                        dr= new DatumRange( tds.getYTagDatum(itable,j1), tds.getYTagDatum(itable,j0) );
                    }
                    Datum dsWidth= TableUtil.guessYTagWidth(tds,itable);
                    if ( ddY.isLog() ) {
                        Units u= dr.getUnits();
                        double d= dr.min().doubleValue(u);
                        double d0= Math.log( dr.min().doubleValue(u) / d );
                        double d1= Math.log( dr.max().doubleValue(u) / d );
                        dr= new DatumRange( d0, d1, Units.logERatio );
                        yy= Units.logERatio.createDatum( Math.log( tds.getYTagDouble(itable,j0,u) / d0) );
                        // TODO: infinity
                    }
                    DatumRange xdr= new DatumRange( ddX.binCenter(0), ddX.binCenter(ddX.numberOfBins()-1) );
                    double alpha=dr.normalize(yy);
                    int nx= ddX.numberOfBins();
                    for ( int ix=tds.tableStart(itable); ix<tds.tableEnd(itable); ix++ ) {
                        int ii= ddX.whichBin( tds.getXTagDouble(ix,xunits), xunits );
                        if ( ii>=0 && ii<nx ) {
                            if ( rebinWeights[ii][iy]>0.0 ) continue;
                            if ( wds.getDouble(ix,j0,wunits) * wds.getDouble(ix,j1,wunits) == 0. ) continue;
                            rebinData[ii][iy]= ( 1-alpha ) * tds.getDouble(ix,j0,zunits)
                                    + alpha * tds.getDouble(ix,j1,zunits) ;
                            rebinWeights[ii][iy]= 1.0;
                        }
                    }
                }
            }
        }
    }
    
    static void doCorners( TableDataSet tds, TableDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        if ( ddY==null ) return;
        Units yunits= tds.getYUnits();
        Units zunits= tds.getZUnits();
        Units xunits= tds.getXUnits();
        Units wunits= Units.dimensionless;
        TableDataSet wds=  WeightsTableDataSet.create(tds);
        for ( int i=0; i<2; i++ ) {
            int ix= i==0 ? 0 : ddX.numberOfBins()-1;
            Datum xx= ddX.binCenter(ix);
            int i0= DataSetUtil.getPreviousColumn( tds, xx );
            int i1= DataSetUtil.getNextColumn( tds, xx );
            
            int itable= tds.tableOfIndex(i0);
            if ( itable!=tds.tableOfIndex(i1) ) continue;
            
            if ( i0==i1 ) continue;
            
            for ( int j=0; j<2; j++ ) {
                int iy= j==0 ? 0 : ddY.numberOfBins()-1;
                Datum yy= ddY.binCenter(iy);
                
                int j0= TableUtil.getPreviousRow( tds, itable, yy );
                int j1= TableUtil.getNextRow( tds, itable, yy );
                
                if ( j0!=j1 ) {
                    DatumRange xdr= new DatumRange( tds.getXTagDatum(i0), tds.getXTagDatum(i1) );
                    DatumRange ydr= new DatumRange( tds.getYTagDatum(itable,j0), tds.getYTagDatum(itable,j1) );
                    if ( xdr.width().lt( DataSetUtil.guessXTagWidth(tds).multiply(1.1) ) ) {
                        DatumRange xdr1= new DatumRange( ddX.binCenter(0), ddX.binCenter(ddX.numberOfBins()-1) );
                        double yalpha=ydr.normalize(yy);
                        double xalpha=xdr.normalize(xx);
                        
                        if ( rebinWeights[ix][iy]>0.0 ) continue;
                        if ( wds.getDouble( i1, j1, wunits ) *
                                wds.getDouble( i0, j0, wunits )*
                                wds.getDouble( i1, j0, wunits )*
                                wds.getDouble( i0, j1, wunits ) == 0. ) continue ;
                        rebinData[ix][iy]=
                                tds.getDouble( i1, j1, zunits ) * xalpha * yalpha  +
                                tds.getDouble( i0, j0, zunits ) * ( 1-xalpha ) * ( 1-yalpha ) +
                                tds.getDouble( i1, j0, zunits ) * xalpha * ( 1-yalpha ) +
                                tds.getDouble( i0, j1, zunits ) * ( 1-xalpha ) * yalpha ;
                        rebinWeights[ix][iy]= 1.0;
                    }
                }
            }
        }
    }
    
    static void average(TableDataSet tds, TableDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        double[] ycoordinate;
        int nTables;
        Units xUnits, zUnits;
        int nx, ny;
        
        xUnits = tds.getXUnits();
        zUnits= tds.getZUnits();
        
        nx = (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        ny = (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        
        if (ddY != null) {
            ycoordinate = ddY.binCenters();
        } else {
            ycoordinate = new double[tds.getYLength(0)];
            for (int j = 0; j < ycoordinate.length; j++) {
                ycoordinate[j] = tds.getDouble(0, j, zUnits );
            }
        }
        
        nTables = tds.tableCount();
        for (int iTable = 0; iTable < nTables; iTable++) {
            int [] ibiny= new int[tds.getYLength(iTable)];
            for (int j=0; j < ibiny.length; j++) {
                if (ddY != null) {
                    ibiny[j]= ddY.whichBin(tds.getYTagDouble(iTable, j, tds.getYUnits()), tds.getYUnits());
                } else {
                    ibiny[j] = j;
                }
            }
            for (int i=tds.tableStart(iTable); i < tds.tableEnd(iTable); i++) {
                int ibinx;
                if (ddX != null) {
                    ibinx= ddX.whichBin(tds.getXTagDouble(i, xUnits), xUnits);
                } else {
                    ibinx = i;
                }
                
                if (ibinx>=0 && ibinx<nx) {
                    for (int j = 0; j < tds.getYLength(iTable); j++) {
                        double z = tds.getDouble(i,j,zUnits);
                        double w = weights == null
                                ? (zUnits.isFill(z) ? 0. : 1.)
                                : weights.getDouble(i, j, Units.dimensionless);
                        if (ibiny[j] >= 0 && ibiny[j] < ny) {
                            rebinData[ibinx][ibiny[j]] += z * w;
                            rebinWeights[ibinx][ibiny[j]] += w;
                        }
                    }
                }
            }
        }
        multiplyWeights(rebinData, rebinWeights, zUnits.getFillDouble()  );
    }
    
    private final static double linearlyInterpolate(int i0, double z0, int i1, double z1, int i) {
        double r = ((double)(i - i0))/(i1 - i0);
        return z0 + r * (z1 - z0);
    }
    
    private final static void multiplyWeights(double[][] data, double[][] weights, double fill ) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (weights[i][j] > 0.0) {
                    data[i][j] = data[i][j] / weights[i][j];
                } else {
                    data[i][j] = fill;
                }
            }
        }
    }
    
    static void fillInterpolateX(final double[][] data, final double[][] weights, final double[] xTags, final double xSampleWidth) {
        
        final int nx = xTags.length;
        final int ny = data[0].length;
        final int[] i1= new int[nx];
        final int[] i2= new int[nx];
        double a1;
        double a2;
        
        for (int j = 0; j < ny; j++) {
            int ii1 = -1;
            int ii2 = -1;
            for (int i = 0; i < nx; i++) {
                if (weights[i][j] > 0. && ii1 == (i-1)) { // ho hum another valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                } else if (weights[i][j] > 0. && ii1 == -1) { // first valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                } else if (weights[i][j] > 0. && ii1 < (i-1)) { // bracketed a gap, interpolate
                    if (ii1 > -1) {
                        i1[i] = -1;
                        i2[i] = -1;
                        ii2 = i;
                        for ( int ii = ii1 +1; ii < i; ii++ ) {
                            i1[ii] = ii1;
                            i2[ii] = ii2;
                        }
                        ii1 = i;
                    }
                } else {
                    i1[i] = -1;
                    i2[i] = -1;
                }
            }
            
            for (int i = 0; i < nx; i++) {
                
                if (i1[i] != -1) { // needs to be interpolated
                    if ( (xTags[i2[i]] - xTags[i1[i]]) <= xSampleWidth * 1.5 ) {
                        a2 = (float)((xTags[i] - xTags[i1[i]]) / (xTags[i2[i]] - xTags[i1[i]]));
                        a1 = 1.f - a2;
                        data[i][j] = data[i1[i]][j] * a1 + data[i2[i]][j] * a2;
                        weights[i][j] = weights[i1[i]][j] * a1 + weights[i2[i]][j] * a2; //approximate
                    }
                }
            }
        }
    }
    
    static void fillInterpolateY(final double[][] data, final double[][] weights, RebinDescriptor ddY, Datum yTagWidth ) {
        
        final int nx = data.length;
        final int ny = ddY.numberOfBins();
        final int[] i1= new int[ny];
        final int[] i2= new int[ny];
        final double [] yTagTemp= new double[ddY.numberOfBins()];
        float a1;
        float a2;
        
        final double[] yTags= ddY.binCenters();
        final Units yTagUnits= ddY.getUnits();
        final boolean log= ddY.isLog();
        
        if (log) {
            for (int j=0; j<ny; j++) yTagTemp[j]= Math.log(yTags[j]);
        } else {
            for (int j=0; j<ny; j++) yTagTemp[j]= yTags[j];
        }
        
        double ySampleWidth;
        double fudge= 1.5;
        if ( yTagWidth==null ) {
            double d= Double.MAX_VALUE / 4;  // avoid roll-over when *1.5
            ySampleWidth= d;
        } else {
            if ( UnitsUtil.isRatiometric(yTagWidth.getUnits()) ) {
                double p= yTagWidth.doubleValue(Units.logERatio);
                ySampleWidth= p * fudge;
            } else {
                double d= yTagWidth.doubleValue(yTagUnits.getOffsetUnits());
                ySampleWidth= d * fudge;
            }
        }
        
        for (int i = 0; i < nx; i++) {
            int ii1= -1;
            int ii2= -1;
            for (int j = 0; j < ny; j++) {
                if (weights[i][j] > 0. && ii1 == (j-1)) { // ho hum another valid point
                    i1[j] = -1;
                    i2[j] = -1;
                    ii1 = j;
                } else if (weights[i][j] > 0. && ii1 == -1) { // first valid point
                    i1[j] = -1;
                    i2[j] = -1;
                    ii1=j;
                } else if (weights[i][j] > 0. && ii1 < (j-1)) { // bracketed a gap, interpolate
                    if ((ii1 > -1)) {   // need restriction on Y gap size
                        i1[j] = -1;
                        i2[j] = -1;
                        ii2 = j;
                        for (int jj=j-1; jj>=ii1; jj--) {
                            i1[jj] = ii1;
                            i2[jj] = ii2;
                        }
                        ii1 = j;
                    }
                } else {
                    i1[j] = -1;
                    i2[j] = -1;
                }
            }
            
            
            for (int j = 0; j < ny; j++) {
                if ( (i1[j] != -1) && ( ( yTagTemp[i2[j]] - yTagTemp[i1[j]] ) < ySampleWidth ) ) {
                    a2 = (float)((yTagTemp[j] - yTagTemp[i1[j]]) / (yTagTemp[i2[j]] - yTagTemp[i1[j]]));
                    a1 = 1.f - a2;
                    data[i][j] = data[i][i1[j]] * a1 + data[i][i2[j]] * a2;
                    weights[i][j] = weights[i][i1[j]] * a1 + weights[i][i2[j]] * a2; //approximate
                }
            }
        }
    }
    
    private void enlargePixels( double[][] rebinData, double[][] rebinWeights ) {
        int enlargeSize=5;
        for ( int aa=0; aa<enlargeSize; aa++ ) {
            for ( int ii=0; ii<rebinData.length-1; ii++ ) {
                for ( int jj=0; jj<rebinData[0].length; jj++ ) {
                    if ( rebinWeights[ii][jj]==0 ) {
                        rebinData[ii][jj]=rebinData[ii+1][jj];
                        rebinWeights[ii][jj]=rebinWeights[ii+1][jj];
                    }
                }
            }
            for ( int ii=rebinData.length-1; ii>0; ii-- ) {
                for ( int jj=0; jj<rebinData[0].length; jj++ ) {
                    if ( rebinWeights[ii][jj]==0 ) {
                        rebinData[ii][jj]=rebinData[ii-1][jj];
                        rebinWeights[ii][jj]=rebinWeights[ii-1][jj];
                    }
                }
            }
            for ( int jj=0; jj<rebinData[0].length-1; jj++ ) {
                for ( int ii=0; ii<rebinData.length; ii++ ) {
                    if ( rebinWeights[ii][jj]==0 ) {
                        rebinData[ii][jj]=rebinData[ii][jj+1];
                        rebinWeights[ii][jj]=rebinWeights[ii][jj+1];
                    }
                }
            }
            for ( int jj=rebinData[0].length-1; jj>0; jj-- ) {
                for ( int ii=0; ii<rebinData.length; ii++ ) {
                    if ( rebinWeights[ii][jj]==0 ) {
                        rebinData[ii][jj]=rebinData[ii][jj-1];
                        rebinWeights[ii][jj]=rebinWeights[ii][jj-1];
                    }
                }
            }
        }
    }
    /**
     * Getter for property interpolate.
     * @return Value of property interpolate.
     */
    public boolean isInterpolate() {
        return this.interpolate;
    }
    
    /**
     * Setter for property interpolate.
     * @param interpolate New value of property interpolate.
     */
    public void setInterpolate(boolean interpolate) {
        this.interpolate = interpolate;
    }
    
    public void setEnlargePixels(boolean enlargePixels) {
        this.enlargePixels = enlargePixels;
    }
    
    public boolean isEnlargePixels() {
        return enlargePixels;
    }
}