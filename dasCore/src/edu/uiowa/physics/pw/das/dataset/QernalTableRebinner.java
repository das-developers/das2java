/*
 * QernalTableRebinner.java
 *
 * Created on October 7, 2005, 11:34 AM
 *
 *
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;



/**
 *
 * @author Jeremy
 */
public class QernalTableRebinner implements DataSetRebinner {
    
    interface QernalFactory {
        Qernal getQernal( RebinDescriptor ddx, RebinDescriptor ddy, Datum xBinWidth, Datum yBinWidth );
    }
    
    interface Qernal {
        void apply( int x, int y, double value, double weight, double[][] s, double[][]w );
    }
    
    Logger logger= DasLogger.getLogger(DasLogger.DATA_OPERATIONS_LOG);
    QernalFactory factory;
    
    public QernalTableRebinner( QernalFactory factory ) {
        this.factory= factory;
    }
            
    public DataSet rebin(DataSet ds, RebinDescriptor ddX, RebinDescriptor ddY) throws IllegalArgumentException, edu.uiowa.physics.pw.das.DasException {
        logger.finest("enter QernalTableRebinner.rebin");
        
        if (ds == null) {
            throw new NullPointerException("null data set");
        }
        if (!(ds instanceof TableDataSet)) {
            throw new IllegalArgumentException("Data set must be an instanceof TableDataSet: " + ds.getClass().getName());
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
        
        Datum xBinWidth= DataSetUtil.guessXTagWidth(tds);
        
        long timer= System.currentTimeMillis();
        
        Units xUnits= ddX.getUnits();        
        Units zUnits= tds.getZUnits();
        
        int nx= (ddX == null ? tds.getXLength() : ddX.numberOfBins());
        int ny= (ddY == null ? tds.getYLength(0) : ddY.numberOfBins());
        
        logger.finest("Allocating rebinData and rebinWeights: " + nx + " x " + ny);
        
        double[][] rebinData= new double[nx][ny];
        double[][] rebinWeights= new double[nx][ny];
        
        int nTables = tds.tableCount();
        for (int iTable = 0; iTable < nTables; iTable++) {
            Datum yBinWidth= TableUtil.guessYTagWidth(tds,iTable);
            
            Qernal qernal= factory.getQernal( ddX, ddY, xBinWidth, yBinWidth );
            
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
                    ibinx= ddX.whichBin( tds.getXTagDouble(i, xUnits), xUnits );
                } else {
                    ibinx = i;
                }
                
                for (int j = 0; j < tds.getYLength(iTable); j++) {
                    double z = tds.getDouble(i,j,zUnits);
                    double w = weights == null
                            ? (zUnits.isFill(z) ? 0. : 1.)
                            : weights.getDouble(i, j, Units.dimensionless);
                    qernal.apply( ibinx, ibiny[j], z, w, rebinData, rebinWeights );
                }
            }
        }
        
        logger.finest("normalize sums by weights");
        for ( int i=0; i<nx; i++ ) {
            for ( int j=0; j<ny; j++ ) {
                if ( rebinWeights[i][j] > 0. ) {
                    rebinData[i][j]/= rebinWeights[i][j];
                } else {
                    rebinData[i][j]= zUnits.getFillDouble();
                }
            }
        }
        
        logger.finest( "create new DataSet" );
        
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
        
        double[][][] zValues = {rebinData,rebinWeights};
        
        int[] tableOffsets = {0};
        Units[] newZUnits = {tds.getZUnits(), Units.dimensionless};
        String[] planeIDs = {"", "weights"};
        
        Map properties= new HashMap(ds.getProperties());
        
        if ( ddX!=null ) properties.put( DataSet.PROPERTY_X_TAG_WIDTH, ddX.binWidthDatum() );
        if ( ddY!=null ) properties.put( DataSet.PROPERTY_Y_TAG_WIDTH, ddY.binWidthDatum() );
        
        TableDataSet result= new DefaultTableDataSet( xTags, resultXUnits, yTags, resultYUnits, zValues, newZUnits, planeIDs, tableOffsets, properties );
        logger.finest("done, QernalTableRebinner.rebin");
        return result;
    }
    
}
