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
import java.util.*;
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
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException, DasException {
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException();
        }
        TableDataSet tds = (TableDataSet)ds;
        TableDataSet weights = (TableDataSet)ds.getPlanarView("weights");
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
        
        return result;
    }
    
    static void average(TableDataSet tds, TableDataSet weights, double[][] rebinData, double[][] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        double[] ycoordinate;
        int nTables;
        Units xUnits, zUnits;
        int nx, ny;
        double[][] hInterpData, hInterpWeights, vInterpData, vInterpWeights;
        int[][] hInterpIndex, vInterpIndex;

        xUnits = tds.getXUnits();
        zUnits= tds.getZUnits();
        
        nx = (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        ny = (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
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
        }
        else {
            ycoordinate = new double[tds.getYLength(0)];
            for (int j = 0; j < ycoordinate.length; j++) {
                ycoordinate[j] = tds.getDouble(0, j, tds.getYUnits());
            }
        }
        
        nTables = tds.tableCount();
        for (int iTable = 0; iTable < nTables; iTable++) {
            int [] ibiny= new int[tds.getYLength(iTable)];
            for (int j=0; j < ibiny.length; j++) {
                if (ddY != null) {
                    ibiny[j]= ddY.whichBin(tds.getYTagDouble(iTable, j, tds.getYUnits()), tds.getYUnits());
                }
                else {
                    ibiny[j] = j;
                }
            }
            for (int i=tds.tableStart(iTable); i < tds.tableEnd(iTable); i++) {
                int ibinx;
                if (ddX != null) {
                    ibinx= ddX.whichBin(tds.getXTagDouble(i, xUnits), xUnits);
                }
                else {
                    ibinx = i;
                }
                
                if (ibinx < 0) {
                    for (int j = 0; j < tds.getYLength(iTable); j++) {
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
                        }
                        else if (ibinx == vInterpIndex[0][j]) {
                            vInterpData[0][ibiny[j]] += z * w;
                            vInterpWeights[0][ibiny[j]] += w;
                        }
                    }
                }
                else if (ibinx >= nx) {
                    for (int j = 0; j < tds.getYLength(iTable); j++) {
                        if (ibiny[j] < 0 || ibiny[j] >= ny) {
                            continue;
                        }
                        double z = tds.getDouble(i, j, zUnits);
                        double w = weights == null
                            ? (zUnits.isFill(z) ? 0. : 1.)
                            : weights.getDouble(i, j, Units.dimensionless);
                        if (vInterpIndex[1][j] == -1
                            || ibinx < vInterpIndex[1][ibiny[j]]) {
                                vInterpData[1][ibiny[j]] = z * w;
                                vInterpWeights[1][ibiny[j]] = w;
                                vInterpIndex[1][ibiny[j]] = ibinx;
                        }
                        else {
                            vInterpData[1][ibiny[j]] += z * w;
                            vInterpWeights[1][ibiny[j]] += w;
                        }
                    }
                }
                else { //if (ibinx>=0 && ibinx<nx) {
                    for (int j = 0; j < tds.getYLength(iTable); j++) {
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
                            }
                            else if (ibiny[j] == hInterpIndex[ibinx][0]) {
                                hInterpData[ibinx][0] += z * w;
                                hInterpWeights[ibinx][0] += w;
                            }
                        }
                        else if (ibiny[j] >= ny) {
                            if (hInterpIndex[ibinx][1] == -1
                                || ibiny[j] < hInterpIndex[ibinx][1]) {
                                    hInterpData[ibinx][1] = z * w;
                                    hInterpWeights[ibinx][1] = w;
                                    hInterpIndex[ibinx][1] = ibiny[j];
                            }
                            else if (ibiny[j] == hInterpIndex[ibinx][1]) {
                                hInterpData[ibinx][1] += z * w;
                                hInterpWeights[ibinx][1] += w;
                            }
                        }
                        else { //if (ibiny[j] >= 0 && ibiny[j] < ny) {
                            rebinData[ibinx][ibiny[j]] += z * w;
                            rebinWeights[ibinx][ibiny[j]] += w;
                        }
                    }
                }
            }
        }
        
        multiplyWeights(rebinData, rebinWeights, zUnits);
        multiplyWeights(hInterpData, hInterpWeights, zUnits);
        multiplyWeights(vInterpData, vInterpWeights, zUnits);
        
        for (int i = 0; i < nx; i++) {
            if (rebinWeights[i][0] == 0.0 && hInterpWeights[i][0] != 0.0) {
                int j0 = 0;
                while (j0 < ny && rebinWeights[i][j0] == 0.0) j0++;
                if (j0 < ny) {
                    rebinData[i][0] = linearlyInterpolate
                        (hInterpIndex[i][0], hInterpData[i][0], j0, rebinData[i][j0], 0);
                    rebinWeights[i][0] = linearlyInterpolate
                        (hInterpIndex[i][0], hInterpWeights[i][0], j0, rebinWeights[i][j0], 0);
                }
            }
            if (rebinWeights[i][ny - 1] == 0.0 && hInterpWeights[i][1] != 0.0) {
                int j0 = ny - 1;
                while (j0 >= 0 && rebinWeights[i][j0] == 0.0) j0--;
                if (j0 >= 0) {
                    rebinData[i][ny - 1] = linearlyInterpolate
                        (j0, rebinData[i][j0], hInterpIndex[i][1], hInterpData[i][1], ny - 1);
                    rebinWeights[i][ny - 1] = linearlyInterpolate
                        (j0, rebinWeights[i][j0], hInterpIndex[i][1], hInterpWeights[i][1], ny - 1);
                }
            }
        }
        
        for (int j = 0; j < ny; j++) {
            if (rebinWeights[0][j] == 0.0 && vInterpWeights[0][j] != 0.0) {
                int i0 = 0;
                while (i0 < nx && rebinWeights[i0][j] == 0.0) i0++;
                if (i0 < nx) {
                    rebinData[0][j] = linearlyInterpolate
                        (vInterpIndex[0][j], vInterpData[0][j], i0, rebinData[i0][j], 0);
                    rebinWeights[0][j] = linearlyInterpolate
                        (vInterpIndex[0][j], vInterpWeights[0][j], i0, rebinWeights[i0][j], 0);
                }
            }
            if (rebinWeights[nx - 1][j] == 0.0 && vInterpWeights[1][j] != 0.0) {
                int i0 = nx - 1;
                while (i0 >= 0 & rebinWeights[i0][j] == 0.0) i0--;
                if (i0 >= 0) {
                    rebinData[nx - 1][j] = linearlyInterpolate
                        (i0, rebinData[i0][j], vInterpIndex[1][j], vInterpData[1][j], nx - 1);
                    rebinWeights[nx - 1][j] = linearlyInterpolate
                        (i0, rebinWeights[i0][j], vInterpIndex[1][j], vInterpWeights[1][j], nx - 1);
                }
            }
        }
    }
    
    private static double linearlyInterpolate(int i0, double z0, int i1, double z1, int i) {
        double r = ((double)(i - i0))/(i1 - i0);
        return z0 + r * (z1 - z0);
    }
    
    private static void multiplyWeights(double[][] data, double[][] weights, Units zUnits) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[i].length; j++) {
                if (weights[i][j] > 0.0) {
                    data[i][j] = data[i][j] / weights[i][j];
                } else {
                    data[i][j] = zUnits.getFillDouble();
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