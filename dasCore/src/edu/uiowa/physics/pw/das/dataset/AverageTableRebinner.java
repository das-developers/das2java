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

import edu.uiowa.physics.pw.das.datum.*;
import java.io.*;

/**
 *
 * @author  Edward West
 */
public class AverageTableRebinner implements DataSetRebinner {
    
    /** Creates a new instance of TableAverageRebinner */
    public AverageTableRebinner() {
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException {
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException();
        }
        TableDataSet tds = (TableDataSet)ds;
        TableDataSet weights = (TableDataSet)ds.getPlanarView("weights");
        
        long timer= System.currentTimeMillis();
        
        int nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        double[][] rebinData= new double[nx][ny];
        double[][] rebinWeights= new double[nx][ny];

        average(tds, weights, rebinData, rebinWeights, ddX, ddY);
        
        Datum xTagWidth= (Datum)ds.getProperty("xTagWidth");
        if ( xTagWidth==null ) {
            xTagWidth= TableUtil.guessXTagWidth(tds);
        }

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

        fillInterpolateX(rebinData, rebinWeights, xTags, xTagWidth.doubleValue(ddX.getUnits().getOffsetUnits()));
        fillInterpolateY(rebinData, rebinWeights, yTags[0], Double.POSITIVE_INFINITY, ddY.isLog());

        double[][][] zValues = {rebinData,rebinWeights};
        int[] tableOffsets = {0};
        Units[] zUnits = {tds.getZUnits(), Units.dimensionless};
        String[] planeIDs = {"", "weights"};

        return new DefaultTableDataSet(xTags, tds.getXUnits(), yTags, tds.getYUnits(), zValues, zUnits, planeIDs, tableOffsets, java.util.Collections.EMPTY_MAP);
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
        

        int [] ibiny= new int[tds.getYLength(0)];
        for (int j=0; j < ibiny.length; j++) {
            if (ddY != null) {
                ibiny[j]= ddY.whichBin(tds.getYTagDouble(0, j, tds.getYUnits()), tds.getYUnits());
            }
            else {
                ibiny[j] = j;
            }
        }

        for (int i=0; i < tds.getXLength(); i++) {
            int ibinx;
            if (ddX != null) {
                ibinx= ddX.whichBin(tds.getXTagDouble(i, tds.getXUnits()), tds.getXUnits());
            }
            else {
                ibinx = i;
            }
            if (ibinx>=0 && ibinx<nx) {
                for (int j = 0; j < ibiny.length; j++) {
                    if (ibiny[j] >= 0 && ibiny[j] < ny) {
                        if (weights != null) {
                            double w = weights.getDouble(i, j, Units.dimensionless);
                            rebinData[ibinx][ibiny[j]] += tds.getDouble(i, j, tds.getZUnits()) * w;
                            rebinWeights[ibinx][ibiny[j]] += w;
                        }
                        else {
                            Datum z= tds.getDatum(i,j);
			    double w= z.isFill() ? 0. : 1. ;
                            rebinData[ibinx][ibiny[j]] += z.doubleValue(tds.getZUnits()) * w;
                            rebinWeights[ibinx][ibiny[j]] += w;
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
                    rebinData[i][j] = tds.getZUnits().getFill().doubleValue(tds.getZUnits());
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
                        for (int ii = i - 1; ii > ii1; ii--) {
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
                
                if ((i1[i] != -1) && (xTags[i2[i]] - xTags[i1[i]]) < xSampleWidth * 2. ) {
                    a2 = (float)((xTags[i] - xTags[i1[i]]) / (xTags[i2[i]] - xTags[i1[i]]));
                    //if (nearestNeighbor) a2= (a2<0.5f)?0.f:1.0f;
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
    
}
