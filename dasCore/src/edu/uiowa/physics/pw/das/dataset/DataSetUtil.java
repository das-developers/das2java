/*
 * DataSetUtil.java
 *
 * Created on January 26, 2004, 6:03 PM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;

/**
 *
 * @author  Jeremy
 */
public class DataSetUtil {
    
    public static CacheTag guessCacheTag( DataSet ds ) {
        Datum start= ds.getXTagDatum(0);
        Datum end= ds.getXTagDatum( ds.getXLength()-1 );
        Datum resolution= ds.getXTagDatum(1).subtract( start );
        return new CacheTag( start, end, resolution );
    }
    
    public static DasAxis guessYAxis( DataSet ds ) {
        Units yunits= ds.getYUnits();
        return new DasAxis( yunits.createDatum(-20),
        yunits.createDatum(20), DasAxis.LEFT );
    }
    
    public static DasAxis guessXAxis( DataSet ds ) {
        Datum min= ds.getXTagDatum(0);
        Datum max= ds.getXTagDatum( ds.getXLength()-1 );
        return new DasAxis( min, max, DasAxis.BOTTOM );
    }
    
    public static DasPlot guessPlot(DataSet ds ) {
        DasAxis xaxis= guessXAxis( ds );
        DasAxis yaxis= guessYAxis( ds );
        DasPlot plot= new DasPlot( xaxis, yaxis );
        if ( ds instanceof VectorDataSet ) {
            Renderer rend= new SymbolLineRenderer( ds );
            plot.addRenderer(rend);
        } else if (ds instanceof TableDataSet ) {
            Units zunits= ((TableDataSet)ds).getZUnits();
            DasColorBar colorbar= new DasColorBar( zunits.createDatum( -20 ), zunits.createDatum(20), false );
            Renderer rend= new SpectrogramRenderer( new ConstantDataSetDescriptor(ds), colorbar );
            plot.addRenderer(rend);
        }
        
        return plot;
    }
    
    public static double[] getXTagArrayDouble( DataSet table, Units units ) {
        
        int ixmax= table.getXLength();
        double[] xx= new double[ixmax];
        for ( int i=0; i<ixmax; i++ ) {
            xx[i]= table.getXTagDouble(i,units);
        }
        return xx;
    }

    
    public static Datum guessXTagWidth( DataSet table ) {
        if ( table.getXLength()>2 ) {
            return table.getXTagDatum(2).subtract( table.getXTagDatum(0) ).divide(2.);
        } else {
            return table.getXUnits().getOffsetUnits().createDatum(0);
        }
    }

    
    protected static int closest( double[] xx, double x ) {
        if ( xx.length==0 ) {
            throw new IllegalArgumentException("array has no elements");
        }
        int result=0;
        while ( result<(xx.length-1) && xx[result]<x ) result++;
        while ( result>0 && xx[result]>x ) result--;
        if ( result<xx.length-2 ) {
            result= ( ( x-xx[result] ) / ( xx[result+1] - xx[result] ) < 0.5 ? result : result+1 );
        }
        return result;
    }
    
    public static int closestColumn( DataSet table, Datum datum ) {
        return closestColumn( table, datum.doubleValue(datum.getUnits()), datum.getUnits() );
    }
    
    public static int closestColumn( DataSet table, double x, Units units ) {
        double [] xx= getXTagArrayDouble( table, units );
        return closest( xx, x );
    }
    
    public static int getPreviousColumn( DataSet ds, Datum datum ) {
        int i= closestColumn( ds, datum );
        if ( i>0 && ds.getXTagDatum(i).ge(datum) ) {
            return i-1;
        } else {
            return i;
        }
    }
    
    public static int getNextColumn( DataSet ds, Datum datum ) {
        int i= closestColumn( ds, datum );
        if ( i<ds.getXLength()-1 && ds.getXTagDatum(i).le(datum) ) {
            return i+1;
        } else {
            return i;
        }
    }
    
    
}
