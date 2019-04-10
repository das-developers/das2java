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
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 *
 * @author  Edward West
 */
public class PeakTableRebinner implements DataSetRebinner {
    
    public PeakTableRebinner() {
    }
    
    @Override
    public QDataSet rebin(QDataSet ds, RebinDescriptor ddX, RebinDescriptor ddY, RebinDescriptor ddZ ) throws IllegalArgumentException {
        if (!(ds.rank()==2 )) {
            throw new IllegalArgumentException("dataset must be rank 2");
        }
        
        QDataSet tds = ds;
        
        int nx= (ddX == null ? tds.length() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.length(0) : ddY.numberOfBins());
        
        double[][] rebinData= new double[nx][ny];

        peaks( tds, rebinData, ddX, ddY);
        
        double[] dd= new double[nx*ny];
        AveragePeakTableRebinner.flatten( rebinData, dd, 0, nx, ny );
        DDataSet result= DDataSet.wrap( dd, nx, ny );
        org.das2.qds.DataSetUtil.copyDimensionProperties( tds, result );
        
        return result;
        
    }
    
    static void peaks( QDataSet tds, double[][] rebinData, RebinDescriptor ddX, RebinDescriptor ddY) {

        int nx= (ddX == null ? tds.length() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.length(0) : ddY.numberOfBins());
        
        for (int i = 0; i < rebinData.length; i++) {
            java.util.Arrays.fill(rebinData[i], Double.NaN);
        }
        
        QDataSet ytds= SemanticOps.ytagsDataSet(tds);
        Units yunits= SemanticOps.getUnits(ytds);
        QDataSet xtds= SemanticOps.xtagsDataSet(tds);
        Units xunits= SemanticOps.getUnits(xtds);
        
        int [] ibiny= new int[tds.length(0)];
        for (int j=0; j < ibiny.length; j++) {
            if (ddY != null) {
                ibiny[j]= ddY.whichBin( ytds.value(j), yunits );
            }
            else {
                ibiny[j] = j;
            }
        }

        for (int i=0; i < tds.length(); i++) {
            int ibinx;
            if (ddX != null) {
                ibinx= ddX.whichBin( xtds.value(i), xunits );
            }
            else {
                ibinx = i;
            }
            if (ibinx>=0 && ibinx<nx) {
                for (int j = 0; j < ibiny.length; j++) {
                    if (ibiny[j] >= 0 && ibiny[j] < ny) {
                        double value = tds.value(i,j);
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
