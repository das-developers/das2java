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
        
        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();
        
        double[] rebinData= new double[nx*ny];
        double[] rebinWeights= new double[nx*ny];

        average(tds, weights, rebinData, rebinWeights, ddX, ddY);
        
        fillInterpolateX(rebinData, rebinWeights, ddX.binCenters(), Double.POSITIVE_INFINITY);
        fillInterpolateY(rebinData, rebinWeights, ddY.binCenters(), Double.POSITIVE_INFINITY, ddY.isLog());

        double[] xTags = ddX.binCenters();
        double[][] yTags = {ddY.binCenters()};
        double[][][] zValues = {{rebinData},{rebinWeights}};
        int[] tableOffsets = {0};
        Units[] zUnits = {tds.getZUnits(), Units.dimensionless};
        String[] planeIDs = {"", "weights"};

        return new DefaultTableDataSet(xTags, tds.getXUnits(), yTags, tds.getYUnits(), zValues, zUnits, planeIDs, tableOffsets, java.util.Collections.EMPTY_MAP);
    }
    
    static void average(TableDataSet tds, TableDataSet weights, double[] rebinData, double[] rebinWeights, RebinDescriptor ddX, RebinDescriptor ddY) {
        double[] ycoordinate= ddY.binCenters();

        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();
        
        int [] ibiny= new int[tds.getYLength(0)];
        for (int j=0; j < ibiny.length; j++) {
            ibiny[j]= ddY.whichBin(tds.getYTagDouble(0, j, tds.getYUnits()), tds.getYUnits());
        }

        for (int i=0; i < tds.getXLength(); i++) {
            int ibinx= ddX.whichBin(tds.getXTagDouble(i, tds.getXUnits()), tds.getXUnits());
            if (ibinx>=0 && ibinx<nx) {
                for (int j = 0; j < ibiny.length; j++) {
                    if (ibiny[j] >= 0 && ibiny[j] < ny) {
                        if (weights != null) {
                            double w = weights.getDouble(i, j, Units.dimensionless);
                            rebinData[ibinx*ny + ibiny[j]] += tds.getDouble(i, j, tds.getZUnits()) * w;
                        }
                        else {
                            rebinData[ibinx*ny + ibiny[j]] += tds.getDouble(i, j, tds.getZUnits());
                            rebinWeights[ibinx*ny + ibiny[j]] += 1.0;
                        }
                    }
                }
            }
        }
        
        for (int index = 0; index < rebinData.length; index++) {
            if (rebinWeights[index] > 0.0) {
                rebinData[index] = rebinData[index] / rebinWeights[index];
            } else {
                rebinData[index] = Double.NaN;
            }
        }
    }
    
    static void fillInterpolateX(final double[] data, final double[] weights, final double[] xTags, final double xSampleWidth) {
        //boolean nearestNeighbor= isNnRebin();
        final int nx = xTags.length;
        final int ny = data.length / nx;
        final int[] i1= new int[nx];
        final int[] i2= new int[nx];
        double a1;
        double a2;

        for (int j = 0; j < ny; j++) {
            int ii1 = -1;
            int ii2 = -1;
            for (int i = 0; i < nx; i++) {
                
                if (weights[i * ny + j] > 0. && ii1 == (i-1)) { // ho hum another valid point
                    i1[i] = -1;
                    i2[i] = -1;
                    ii1 = i;
                } else if (weights[i * ny + j] > 0. && ii1 == -1) { // first valid point
                    ii1 = i;
                } else if (weights[i * ny + j] > 0. && ii1 < (i-1)) { // bracketed a gap, interpolate
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
                
                if ((i1[i] != -1) && (xTags[i2[i]] - xTags[i1[i]]) < xSampleWidth * 2) {
                    a2 = (float)((xTags[i] - xTags[i1[i]]) / (xTags[i2[i]] - xTags[i1[i]]));
                    //if (nearestNeighbor) a2= (a2<0.5f)?0.f:1.0f;
                    a1 = 1.f - a2;
                    data[i * ny + j] = data[i1[i] * ny + j] * a1 + data[i2[i] * ny + j] * a2;
                    weights[i * ny + j] = weights[i1[i] * ny + j] * a1 + weights[i2[i] * ny + j] * a2; //approximate
                }
            }
        }
    }

    static void fillInterpolateY(final double[] data, final double[] weights, final double[] yTags, final double ySampleWidth, final boolean log) {
        
        //boolean nearestNeighbor=isNnRebin();
        
        final int ny = yTags.length;
        final int nx = data.length / ny;
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
                if (weights[i * ny + j] > 0. && ii1 == (j-1)) { // ho hum another valid point
                    i1[j] = -1;
                    i2[j] = -1;
                    ii1 = j;
                } else if (weights[i * ny + j] > 0. && ii1 == -1) { // first valid point
                    ii1=j;
                } else if (weights[i * ny + j] > 0. && ii1 < (j-1)) { // bracketed a gap, interpolate
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
                    data[i * ny + j] = data[i * ny + i1[j]] * a1 + data[i * ny + i2[j]] * a2;
                    weights[i * ny + j] = weights[i * ny + i1[j]] * a1 + weights[i * ny + i2[j]] * a2; //approximate
                }
            }
        }
    }
    
}
