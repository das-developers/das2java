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
 * not thread safe!!!
 * @author  Jeremy Faden
 */
public class NewAverageTableRebinner implements DataSetRebinner {
    
    private static final Logger logger = DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG);
    
    /**
     * Holds value of property interpolate.
     */
    private boolean interpolate= true;
    private boolean enlargePixels = true;
    
    /* not thread safe--ny is set in rebin and then used by the other routines. */
    private final int ny;
    private final int nx;
    TableDataSet tds;
    RebinDescriptor ddX, ddY;
    
    /** Creates a new instance of TableAverageRebinner */
    public NewAverageTableRebinner( DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY ) {
        if (ds == null) {
            throw new NullPointerException("null data set");
        }
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException("Data set must be an instanceof TableDataSet: " + ds.getClass().getName());
        }
        this.tds = (TableDataSet)ds;
        this.ddX= ddX;
        this.ddY= ddY;
        nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException, DasException {
        if ( ds!=this.tds ) throw new IllegalArgumentException("already set for another dataset");
        if ( ddX!=this.ddX ) throw new IllegalArgumentException("already set for another X rebin descriptor");        
        if ( ddY!=this.ddY ) throw new IllegalArgumentException("already set for another Y rebin descriptor");
        
        TableDataSet weights = (TableDataSet)tds.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
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
        
        
        
        logger.finest("Allocating rebinData and rebinWeights: " + nx + " x " + ny);
        
        double[] rebinData= new double[nx*ny];  // Y's are adjacent
        double[] rebinWeights= new double[nx*ny];
        
        average(tds, weights, rebinData, rebinWeights, ddX, ddY);
        
        double[] xTags;
        if (ddX != null) {
            xTags = ddX.binCenters();
        } else {
            xTags = new double[nx];
            for (int i = 0; i < nx; i++) {
                xTags[i] = tds.getXTagDouble(i, tds.getXUnits());
            }
        }
        
        double[] yTags;
        if (ddY != null) {
            yTags = ddY.binCenters();
        } else {
            yTags = new double[ny];
            for (int j = 0; j < ny; j++) {
                yTags[j] = tds.getYTagDouble(0, j, tds.getYUnits());
            }
        }
        
        /* TODO: handle xTagWidth yTagWidth properties.  Pass on unrelated properties on to the
         * new dataset.
         */
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
        
        TableDataSet weightsTDS= new SimpleTableDataSet( xTags, yTags, rebinWeights, resultXUnits, resultYUnits, Units.dimensionless );
        TableDataSet result= new SimpleTableDataSet( xTags, yTags, rebinData, resultXUnits, resultYUnits, Units.dimensionless, DataSet.PROPERTY_PLANE_WEIGHTS, weightsTDS );
        
        return result;
    }
    
    private final int indexOf( int i, int j ) {
        return i*ny + j;
    }
    
    void average(TableDataSet tds, TableDataSet weights, double[] rebinData, double[] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        double[] ycoordinate;
        int nTables;
        Units xUnits, zUnits;
        
        double[][] hInterpData, hInterpWeights, vInterpData, vInterpWeights;
        int[][] hInterpIndex, vInterpIndex;
        
        xUnits = tds.getXUnits();
        zUnits= tds.getZUnits();
        
        hInterpData = new double[nx][2];
        hInterpWeights = new double[nx][2];
        hInterpIndex = new int[nx][2];
        vInterpData = new double[2][ny];
        vInterpWeights = new double[2][ny];
        vInterpIndex = new int[2][ny];
        
        for (int i = 0; i < hInterpIndex.length; i++) {
            Arrays.fill(hInterpIndex[i], -1);
        }
        for (int i = 0; i < vInterpIndex.length; i++) {
            Arrays.fill(vInterpIndex[i], -1);
        }
        
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
            int yLength= tds.getYLength(iTable);
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
                
                if (ibinx < 0) {     
                    for (int j = 0; j < yLength; j++) {
                        if (ibiny[j] < 0 || ibiny[j] >= ny) {
                            continue;
                        }
                        double z = tds.getDouble(i, j, zUnits);
                        double w = weights == null
                                ? (zUnits.isFill(z) ? 0. : 1.)
                                : weights.getDouble(i, j, Units.dimensionless);
                        if (vInterpIndex[0][ibiny[j]] == -1
                                || ibinx > vInterpIndex[0][ibiny[j]]) {
                            vInterpData[0][ibiny[j]] = z * w;
                            vInterpWeights[0][ibiny[j]] = w;
                            vInterpIndex[0][ibiny[j]] = ibinx;
                        } else if (ibinx == vInterpIndex[0][ibiny[j]]) {
                            vInterpData[0][ibiny[j]] += z * w;
                            vInterpWeights[0][ibiny[j]] += w;
                        }
                    }
                } else if (ibinx >= nx) {
                    for (int j = 0; j < yLength; j++) {
                        if (ibiny[j] < 0 || ibiny[j] >= ny) {
                            continue;
                        }
                        double z = tds.getDouble(i, j, zUnits);
                        double w = weights == null
                                ? (zUnits.isFill(z) ? 0. : 1.)
                                : weights.getDouble(i, j, Units.dimensionless);
                        if (vInterpIndex[1][ibiny[j]] == -1
                                || ibinx < vInterpIndex[1][ibiny[j]]) {
                            vInterpData[1][ibiny[j]] = z * w;
                            vInterpWeights[1][ibiny[j]] = w;
                            vInterpIndex[1][ibiny[j]] = ibinx;
                        } else {
                            vInterpData[1][ibiny[j]] += z * w;
                            vInterpWeights[1][ibiny[j]] += w;
                        }
                    }
                } else { //if (ibinx>=0 && ibinx<nx) {
                    for (int j = 0; j < yLength; j++) {
                        double z = tds.getDouble(i,j,zUnits);
                        double w = weights == null
                                ? (zUnits.isFill(z) ? 0. : 1.)
                                : weights.getDouble(i, j, Units.dimensionless);
                        if (ibiny[j] < 0) {
                            if (hInterpIndex[ibinx][0] == -1
                                    || ibiny[j] > hInterpIndex[ibinx][0]) {
                                hInterpData[ibinx][0] = z * w;
                                hInterpWeights[ibinx][0] = w;
                                hInterpIndex[ibinx][0] = ibiny[j];
                            } else if (ibiny[j] == hInterpIndex[ibinx][0]) {
                                hInterpData[ibinx][0] += z * w;
                                hInterpWeights[ibinx][0] += w;
                            }
                        } else if (ibiny[j] >= ny) {
                            if (hInterpIndex[ibinx][1] == -1
                                    || ibiny[j] < hInterpIndex[ibinx][1]) {
                                hInterpData[ibinx][1] = z * w;
                                hInterpWeights[ibinx][1] = w;
                                hInterpIndex[ibinx][1] = ibiny[j];
                            } else if (ibiny[j] == hInterpIndex[ibinx][1]) {
                                hInterpData[ibinx][1] += z * w;
                                hInterpWeights[ibinx][1] += w;
                            }
                        } else { //if (ibiny[j] >= 0 && ibiny[j] < ny) {
                            rebinData[ indexOf( ibinx,ibiny[j] ) ] += z * w;
                            rebinWeights[ indexOf(ibinx,ibiny[j] ) ] += w;
                        }
                    }
                }
            }
        }
        
        multiplyWeights(rebinData, rebinWeights, zUnits);
    }
    
    private final double linearlyInterpolate(int i0, double z0, int i1, double z1, int i) {
        double r = ((double)(i - i0))/(i1 - i0);
        return z0 + r * (z1 - z0);
    }
    
    private final void multiplyWeights(double[] data, double[] weights, Units zUnits) {
        for (int index = 0; index < data.length; index++) {
            if (weights[index] > 0.0) {
                data[index] = data[index] / weights[index];
            } else {
                data[index] = zUnits.getFillDouble();
            }
        }
    }
    
    void fillInterpolateX(final double[] data, final double[] weights, final double[] xTags, final double xSampleWidth) {
        
        final int[] i1= new int[nx];
        final int[] i2= new int[nx];
        double a1;
        double a2;
        
        for (int j = 0; j < ny; j++) {
            int ii1 = -1;
            int ii2 = -1;
            for (int i = 0; i < nx; i++) {
                if (weights[ indexOf(i,j) ] > 0. && ii1 == (i-1)) { // ho hum another valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                } else if (weights[ indexOf(i,j) ] > 0. && ii1 == -1) { // first valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                } else if (weights[indexOf(i,j)] > 0. && ii1 < (i-1)) { // bracketed a gap, interpolate
                    if (ii1 > -1) {
                        i1[i] = -1;
                        i2[i] = -1;
                        for (int ii = i - 1; ii >= ii1; ii--) {
                            ii2 = i;
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
                
                if ((i1[i] != -1) && (xTags[i2[i]] - xTags[i1[i]]) < xSampleWidth * 1.5 ) {
                    a2 = (float)((xTags[i] - xTags[i1[i]]) / (xTags[i2[i]] - xTags[i1[i]]));
                    a1 = 1.f - a2;
                    data[indexOf(i,j)] = data[indexOf(i1[i],j)] * a1 + data[indexOf(i2[i],j)] * a2;
                    weights[indexOf(i,j)] = weights[indexOf(i1[i],j)] * a1 + weights[indexOf(i2[i],j)] * a2; //approximate
                }
            }
        }
    }
    
    void fillInterpolateY(final double[] data, final double[] weights, RebinDescriptor ddY, Datum yTagWidth ) {
        
        final int[] i1= new int[ny];
        final int[] i2= new int[ny];
        final double [] y_temp= new double[ddY.numberOfBins()];
        float a1;
        float a2;
        
        final double[] yTags= ddY.binCenters();
        final Units yTagUnits= ddY.getUnits();
        final boolean log= ddY.isLog();
        
        if (log) {
            for (int j=0; j<ny; j++) y_temp[j]= Math.log(yTags[j]);
        } else {
            for (int j=0; j<ny; j++) y_temp[j]= yTags[j];
        }
        
        double[] ySampleWidth= new double[ny];
        double fudge= 2.0;
        if ( yTagWidth==null ) {
            double d= Double.MAX_VALUE / 4;  // avoid roll-over when *1.5
            for ( int j=0; j<ny; j++ ) {
                ySampleWidth[j]= d;
            }
        } else {
            if ( UnitsUtil.isRatiometric( yTagWidth.getUnits() ) ) {
                double perc= yTagWidth.doubleValue(Units.percentIncrease);
                for ( int j=0; j<ny; j++ ) {
                    ySampleWidth[j]= yTags[j] * perc / 100 * fudge;
                }
            } else {
                double d= yTagWidth.doubleValue(yTagUnits.getOffsetUnits());
                for ( int j=0; j<ny; j++ ) {
                    ySampleWidth[j]= d * fudge;
                }
            }
        }
        
        
        for (int i = 0; i < nx; i++) {
            int ii1= -1;
            int ii2= -1;
            for (int j = 0; j < ny; j++) {
                if (weights[indexOf(i,j)] > 0. && ii1 == (j-1)) { // ho hum another valid point
                    i1[j] = -1;
                    i2[j] = -1;
                    ii1 = j;
                } else if (weights[indexOf(i,j)] > 0. && ii1 == -1) { // first valid point
                    i1[j] = -1;
                    i2[j] = -1;
                    ii1=j;
                } else if (weights[indexOf(i,j)] > 0. && ii1 < (j-1)) { // bracketed a gap, interpolate
                    if ((ii1 > -1)) {   // need restriction on Y gap size
                        i1[j] = -1;
                        i2[j] = -1;
                        for (int jj=j-1; jj>=ii1; jj--) {
                            ii2 = j;
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
                if ( (i1[j] != -1) && ( ( yTags[i2[j]] - yTags[i1[j]] ) < ySampleWidth[j] ) ) {
                    a2 = (float)((y_temp[j] - y_temp[i1[j]]) / (y_temp[i2[j]] - y_temp[i1[j]]));
                    a1 = 1.f - a2;
                    data[indexOf(i,j)] = data[indexOf(i,i1[j])] * a1 + data[indexOf(i,i2[j])] * a2;
                    weights[indexOf(i,j)] = weights[indexOf(i,i1[j])] * a1 + weights[indexOf(i,i2[j])] * a2; //approximate
                }
            }
        }
    }
    
    private void enlargePixels( double[] rebinData, double[] rebinWeights ) {
        for ( int ii=0; ii<nx-1; ii++ ) {
            for ( int jj=0; jj<ny; jj++ ) {
                if ( rebinWeights[indexOf(ii,jj)]==0 ) {
                    rebinData[indexOf(ii,jj)]=rebinData[indexOf(ii+1,jj)];
                    rebinWeights[indexOf(ii,jj)]=rebinWeights[indexOf(ii+1,jj)];
                }
            }
        }
        for ( int ii=nx-1; ii>0; ii-- ) {
            for ( int jj=0; jj<ny; jj++ ) {
                if ( rebinWeights[indexOf(ii,jj)]==0 ) {
                    rebinData[indexOf(ii,jj)]=rebinData[indexOf(ii-1,jj)];
                    rebinWeights[indexOf(ii,jj)]=rebinWeights[indexOf(ii-1,jj)];
                }
            }
        }
        for ( int jj=0; jj<nx-1; jj++ ) {
            for ( int ii=0; ii<ny; ii++ ) {
                if ( rebinWeights[indexOf(ii,jj)]==0 ) {
                    rebinData[indexOf(ii,jj)]=rebinData[indexOf(ii,jj+1)];
                    rebinWeights[indexOf(ii,jj)]=rebinWeights[indexOf(ii,jj+1)];
                }
            }
        }
        for ( int jj=ny-1; jj>0; jj-- ) {
            for ( int ii=0; ii<rebinData.length; ii++ ) {
                if ( rebinWeights[indexOf(ii,jj)]==0 ) {
                    rebinData[indexOf(ii,jj)]=rebinData[indexOf(ii,jj-1)];
                    rebinWeights[indexOf(ii,jj)]=rebinWeights[indexOf(ii,jj-1)];
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