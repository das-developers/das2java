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

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.datum.*;
import java.io.*;
import java.util.Collections;
import java.util.logging.*;

/**
 *
 * @author  Edward West
 */
public class AverageTableRebinner implements DataSetRebinner {
    
    private static Logger logger = DasApplication.getDefaultApplication().getLogger();
    
    /**
     * Holds value of property interpolate.
     */
    private boolean interpolate= true;
    
    /** Creates a new instance of TableAverageRebinner */
    public AverageTableRebinner() {
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddXin, RebinDescriptor ddYin) throws IllegalArgumentException, DasException {
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException();
        }
        TableDataSet tds = (TableDataSet)ds;
        TableDataSet weights = (TableDataSet)ds.getPlanarView("weights");
        if (ddXin != null && tds.getXLength() > 0) {
            double start = tds.getXTagDouble(0, ddXin.getUnits());
            double end = tds.getXTagDouble(tds.getXLength() - 1, ddXin.getUnits());
            if (start > ddXin.end ) {
                throw new NoDataInIntervalException("data starts after range");
            } else if ( end < ddXin.start ) {
                throw new NoDataInIntervalException("data ends before range");
            }
        }
        
        long timer= System.currentTimeMillis();
        
        Units xunits= ddXin.getUnits();
        
        
        int ix0= DataSetUtil.getPreviousColumn( tds, xunits.createDatum( ddXin.binStart(0,xunits) ) );
        int ix1= DataSetUtil.getNextColumn( tds, xunits.createDatum( ddXin.binStop( ddXin.numberOfBins()-1, xunits ) ) );
        
        RebinDescriptor ddX= RebinDescriptor.createSubsumingRebinDescriptor( ddXin, tds.getXTagDatum(ix0), tds.getXTagDatum(ix1) );
        Datum yMin= TableUtil.getSmallestYTag(tds);
        Datum yMax= TableUtil.getLargestYTag(tds);
        RebinDescriptor ddY= RebinDescriptor.createSubsumingRebinDescriptor( ddYin, yMin, yMax );
        
        int nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        logger.finest("Allocating rebinData and rebinWeights: " + nx + " x " + ny);
        
        double[][] rebinData= new double[nx][ny];
        double[][] rebinWeights= new double[nx][ny];
        
        average(tds, weights, rebinData, rebinWeights, ddX, ddY);
        
        double[] xTags;
        if (ddX != null) {
            xTags = ddX.binCenters();
        }
        else {
            xTags = new double[nx];
            for (int i = 0; i < nx; i++) {
                xTags[i] = tds.getXTagDouble(i, tds.getXUnits());
            }
        }
        double[][] yTags;
        if (ddY != null) {
            yTags = new double[][]{ddY.binCenters()};
        }
        else {
            yTags = new double[1][ny];
            for (int j = 0; j < ny; j++) {
                yTags[0][j] = tds.getYTagDouble(0, j, tds.getYUnits());
            }
        }
        
        Datum xTagWidth= (Datum)ds.getProperty("xTagWidth");
        if ( xTagWidth==null ) {
            xTagWidth= DataSetUtil.guessXTagWidth(tds);
        }
        double xTagWidthDouble= xTagWidth.doubleValue(ddX.getUnits().getOffsetUnits());
        
        if ( this.interpolate ) {
            if ( ddX!=null ) fillInterpolateX(rebinData, rebinWeights, xTags, xTagWidthDouble );
            if ( ddY!=null ) fillInterpolateY(rebinData, rebinWeights, yTags[0], Double.POSITIVE_INFINITY, ddY.isLog());
        } else {
            enlargePixels( rebinData, rebinWeights );
        }
        
        double[][][] zValues = {rebinData,rebinWeights};
        
        int[] tableOffsets = {0};
        Units[] zUnits = {tds.getZUnits(), Units.dimensionless};
        String[] planeIDs = {"", "weights"};
        
        /* TODO: handle xTagWidth yTagWidth properties.  Pass on unrelated properties on to the
         * new dataset.
         */
        Units resultXUnits= ddX==null ? tds.getXUnits() : ddX.getUnits();
        Units resultYUnits= ddY==null ? tds.getYUnits() : ddY.getUnits();
        TableDataSet result= new DefaultTableDataSet(xTags, resultXUnits, yTags, resultYUnits, zValues, zUnits, planeIDs, tableOffsets, java.util.Collections.EMPTY_MAP);
        
        int xoffset= ddX.whichBin( ddXin.binCenter(0),xunits);
        int xlength= ddXin.numberOfBins();
        int yoffset;
        int ylength;
        if ( ddY!=null ) {
            Units yunits= ddYin.getUnits();
            yoffset= ddY.whichBin( ddYin.binCenter(0),yunits);
            ylength= ddYin.numberOfBins();
        } else {
            yoffset= 0;
            ylength= yTags.length;
        }
        return new ClippedTableDataSet( result, xoffset, xlength, yoffset, ylength );
    }
    
    static void average(TableDataSet tds, TableDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        double[] ycoordinate;
        if (ddY != null) {
            ycoordinate = ddY.binCenters();
        }
        else {
            ycoordinate = new double[tds.getYLength(0)];
            for (int j = 0; j < ycoordinate.length; j++) {
                ycoordinate[j] = tds.getDouble(0, j, tds.getYUnits());
            }
        }
        
        int nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        
        for (int table = 0; table < tds.tableCount(); table++) {
            int [] ibiny= new int[tds.getYLength(table)];
            for (int j=0; j < ibiny.length; j++) {
                if (ddY != null) {
                    ibiny[j]= ddY.whichBin(tds.getYTagDouble(table, j, tds.getYUnits()), tds.getYUnits());
                }
                else {
                    ibiny[j] = j;
                }
            }
            for (int i=tds.tableStart(table); i < tds.tableEnd(table); i++) {
                int ibinx;
                if (ddX != null) {
                    ibinx= ddX.whichBin(tds.getXTagDouble(i, tds.getXUnits()), tds.getXUnits());
                }
                else {
                    ibinx = i;
                }
                if (ibinx>=0 && ibinx<nx) {
                    for (int j = 0; j < tds.getYLength(table); j++) {
                        if (ibiny[j] >= 0 && ibiny[j] < ny) {
                            if (weights != null) {
                                double w = weights.getDouble(i, j, Units.dimensionless);
                                rebinData[ibinx][ibiny[j]] += tds.getDouble(i, j, tds.getZUnits()) * w;
                                rebinWeights[ibinx][ibiny[j]] += w;
                            }
                            else {
                                Units zUnits= tds.getZUnits();
                                double z= tds.getDouble(i,j,zUnits);
                                double w= zUnits.isFill(z) ? 0. : 1. ;
                                rebinData[ibinx][ibiny[j]] += z * w;
                                rebinWeights[ibinx][ibiny[j]] += w;
                            }
                        }
                    }
                }
            }
        }
        
        for (int i = 0; i < rebinData.length; i++) {
            for (int j = 0; j < rebinData[i].length; j++) {
                if (rebinWeights[i][j] > 0.0) {
                    rebinData[i][j] = rebinData[i][j] / rebinWeights[i][j];
                } else {
                    rebinData[i][j] = tds.getZUnits().getFillDouble();
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
                    ii1 = i;
                } else if (weights[i][j] > 0. && ii1 < (i-1)) { // bracketed a gap, interpolate
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
                    data[i][j] = data[i1[i]][j] * a1 + data[i2[i]][j] * a2;
                    weights[i][j] = weights[i1[i]][j] * a1 + weights[i2[i]][j] * a2; //approximate
                }
            }
        }
    }
    
    static void fillInterpolateY(final double[][] data, final double[][] weights, final double[] yTags, final double ySampleWidth, final boolean log) {
        
        //boolean nearestNeighbor=isNnRebin();
        
        final int nx = data.length;
        final int ny = yTags.length;
        final int[] i1= new int[ny];
        final int[] i2= new int[ny];
        final double [] y_temp= new double[yTags.length];
        float a1;
        float a2;
        
        if (log) {
            for (int j=0; j<ny; j++) y_temp[j]= Math.log(yTags[j]);
        } else {
            for (int j=0; j<ny; j++) y_temp[j]= yTags[j];
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
                    ii1=j;
                } else if (weights[i][j] > 0. && ii1 < (j-1)) { // bracketed a gap, interpolate
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
                if (i1[j] != -1) {
                    a2 = (float)((y_temp[j] - y_temp[i1[j]]) / (y_temp[i2[j]] - y_temp[i1[j]]));
                    //if (nearestNeighbor) a2= (a2<0.5f)?0.f:1.f;
                    a1 = 1.f - a2;
                    data[i][j] = data[i][i1[j]] * a1 + data[i][i2[j]] * a2;
                    weights[i][j] = weights[i][i1[j]] * a1 + weights[i][i2[j]] * a2; //approximate
                }
            }
        }
    }
    
    private void enlargePixels( double[][] rebinData, double[][] rebinWeights ) {
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
    
}