/*
 * DataSetUtil.java
 *
 * Created on January 26, 2004, 6:03 PM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import javax.swing.*;

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
            edu.uiowa.physics.pw.das.graph.Renderer rend= new SymbolLineRenderer( ds );
            plot.addRenderer(rend);
        } else if (ds instanceof TableDataSet ) {
            Units zunits= ((TableDataSet)ds).getZUnits();
            DasColorBar colorbar= new DasColorBar( zunits.createDatum( -20 ), zunits.createDatum(20), false );
            edu.uiowa.physics.pw.das.graph.Renderer rend= new SpectrogramRenderer( new ConstantDataSetDescriptor(ds), colorbar );
            plot.addRenderer(rend);
        }
        
        return plot;
    }
    
    public static DasPlot visualize( DataSet ds, double xmin, double xmax, double ymin, double ymax ) {
        JFrame jframe= new JFrame("DataSetUtil.visualize");
        DasCanvas canvas= new DasCanvas(400,400);
        jframe.getContentPane().add( canvas );
        DasPlot result= guessPlot( ds );
        canvas.add( result, DasRow.create(canvas), DasColumn.create( canvas ) );
        Units xunits= result.getXAxis().getUnits();
        result.getXAxis().setDataRange(xunits.createDatum(xmin), xunits.createDatum(xmax) );
        Units yunits= result.getYAxis().getUnits();
        result.getYAxis().setDataRange(yunits.createDatum(ymin), yunits.createDatum(ymax) );
        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }
    
    public static DatumRange xRange( DataSet ds ) {
        int n=ds.getXLength();
        return new DatumRange( ds.getXTagDatum(0), ds.getXTagDatum(n-1) );
    }
    
    public static DatumRange yRange( DataSet ds ) {
        if ( ds instanceof VectorDataSet ) {
            VectorDataSet vds= ( VectorDataSet ) ds;
            DatumRange result= null;            
            for ( int i=1; i<ds.getXLength(); i++ ) {
                if ( !vds.getDatum(i).isFill() ) {
                    if ( result==null ) {
                        result= new DatumRange(vds.getDatum(i),vds.getDatum(i));
                    } else {
                        result= result.include(vds.getDatum(i));
                    }
                }
            }
            if ( result==null ) { result= new DatumRange(vds.getDatum(0),vds.getDatum(0)); } // fill,fill
            return result;
        } else if ( ds instanceof TableDataSet ) {
            TableDataSet tds= ( TableDataSet ) ds;
            int n=tds.getYLength(0);
            return new DatumRange( tds.getYTagDatum(0,0), tds.getYTagDatum(0,n-1) );
        } else throw new IllegalArgumentException("unsupported: "+ds);
    }
    
    
    public static DasPlot visualize( DataSet ds, boolean ylog ) {        
        DatumRange xRange= xRange( ds );
        DatumRange yRange= yRange( ds );
        JFrame jframe= new JFrame("DataSetUtil.visualize");
        DasCanvas canvas= new DasCanvas(400,400);
        jframe.getContentPane().add( canvas );
        DasPlot result= guessPlot( ds );
        canvas.add( result, DasRow.create(canvas), DasColumn.create( canvas ) );
        Units xunits= result.getXAxis().getUnits();
        result.getXAxis().setDataRange(xRange.zoomOut(1.1));
        Units yunits= result.getYAxis().getUnits();
        if ( ylog ) {
            result.getYAxis().setDataRange(yRange);
            result.getYAxis().setLog(true);
        } else {
            result.getYAxis().setDataRange(yRange.zoomOut(1.1));
        }
        jframe.pack();
        jframe.setVisible(true);
        jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return result;
    }
    
    public static Datum guessXTagWidth( DataSet table ) {
        if ( table.getXLength()>2 ) {
            Datum min= table.getXTagDatum(1).subtract( table.getXTagDatum(0) );
            for ( int i=2; i<table.getXLength(); i++ ) {
                Datum min0= table.getXTagDatum(i).subtract( table.getXTagDatum(i-1) );
                if ( min0.lt(min) ) min= min0;
            }
            return min;
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
    
    public static double[] getXTagArrayDouble( DataSet vds, Units units ) {        
        int ixmax= vds.getXLength();
        double[] xx= new double[ixmax];
        for ( int i=0; i<ixmax; i++ ) {
            xx[i]= vds.getXTagDouble(i,units);
        }
        return xx;
    }
    
    
    public static DatumVector getXTags( DataSet ds ) {
        double[] data= VectorUtil.getXTagArrayDouble(ds,ds.getXUnits());        
        return DatumVector.newDatumVector(data,ds.getXUnits());
    }
    
}
