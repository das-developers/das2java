/* File: TablePeakRebinner.java
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

package org.das2.dataset;

import org.das2.datum.Units;

/**
 *
 * @author  Edward West
 */
public class PeakTableRebinner implements DataSetRebinner {
    
    public PeakTableRebinner() {
    }
    
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException {
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException();
        }
        TableDataSet tds = (TableDataSet)ds;
        long timer= System.currentTimeMillis();
        
        int nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        double[][] rebinData= new double[nx][ny];
        double[][] rebinWeights= new double[nx][ny];

        peaks(tds, rebinData, ddX, ddY);
        
        double[] xTags;
        if (ddX != null) {
            xTags = ddX.binCenters();
        }
        else {
            xTags = new double[tds.getXLength()];
            for (int i = 0; i < xTags.length; i++) {
                xTags[i] = tds.getXTagDouble(i, tds.getXUnits());
            }
        }
        double[][] yTags;
        if (ddY != null) {
            yTags = new double[][]{ddY.binCenters()};
        }
        else {
            yTags = new double[0][tds.getYLength(0)];
            for (int j = 0; j < yTags[0].length; j++) {
                yTags[0][j] = tds.getYTagDouble(0, j, tds.getYUnits());
            }
        }
        double[][][] zValues = {rebinData};
        int[] tableOffsets = {0};
        Units[] zUnits = {tds.getZUnits()};
        String[] planeIDs = {""};

        return new DefaultTableDataSet(xTags, tds.getXUnits(), yTags, tds.getYUnits(), zValues, zUnits, planeIDs, tableOffsets, java.util.Collections.EMPTY_MAP);
    }
    
    static void peaks(TableDataSet tds, double[][] rebinData, RebinDescriptor ddX, RebinDescriptor ddY) {
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
        
        for (int i = 0; i < rebinData.length; i++) {
            java.util.Arrays.fill(rebinData[i], Double.NaN);
        }
        
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
                        double value = tds.getDouble(i, j, tds.getZUnits());
                        if (Double.isNaN(rebinData[ibinx][ibiny[j]])) {
                            rebinData[ibinx][ibiny[j]] = value;
                        }
                        else {
                            rebinData[ibinx][ibiny[j]] = Math.max(value, rebinData[ibinx][ibiny[j]]);
                        }
                    }
                }
            }
        }
    }
}
