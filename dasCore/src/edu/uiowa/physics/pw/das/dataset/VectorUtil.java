package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author  Owner
 */
public class VectorUtil {
    
    //  maybe a cache to keep track of last finds
    
    public static double[] getXTagArrayDouble( DataSet vds, Units units ) {
        
        int ixmax= vds.getXLength();
        double[] xx= new double[ixmax];
        for ( int i=0; i<ixmax; i++ ) {
            xx[i]= vds.getXTagDouble(i,units);
        }
        return xx;
    }
    
    private static int closest( double[] xx, double x ) {
        int result=0;
        while ( result<(xx.length-1) && xx[result]<x ) result++;
        while ( result>0 && xx[result]>x ) result--;
        if ( result<xx.length-2 ) {
            result= ( ( x-xx[result] ) / ( xx[result+1] - xx[result] ) < 0.5 ? result : result+1 );
        }        
        return result;
    }
    
    public static int closestXTag( DataSet ds, Datum datum ) {
        return closestXTag( ds, datum.doubleValue(datum.getUnits()), datum.getUnits() );
    }
    
    public static int closestXTag( DataSet ds, double x, Units units ) {
        double [] xx= getXTagArrayDouble( ds, units );
        return closest( xx, x );
    }
    
    public static void dumpToStream( VectorDataSet vds, OutputStream out ) {
        
        PrintStream pout= new PrintStream(out);
        
        Datum base=null;
        Units offsetUnits= null;
        
        if ( vds.getXUnits() instanceof LocationUnits ) {
            base= vds.getXTagDatum(0);
            offsetUnits= ((LocationUnits)base.getUnits()).getOffsetUnits();
            if ( offsetUnits==Units.microseconds ) {
                offsetUnits= Units.seconds;
            }
            pout.println("# X is first value, offset in "+offsetUnits+" from "+base);
        } else {
            pout.println("# X is first value, in "+vds.getXUnits());
        }
        
        pout.println("# Y values follow, in "+vds.getYUnits());
        
        pout.println("#");
        pout.println("# File created on: "+TimeUtil.now().toString()+" UT");
        String tab= "\011";
        
        for (int i=0; i<vds.getXLength(); i++) {
            double x;
            if ( base!=null ) {
                x= vds.getXTagDatum(i).subtract(base).doubleValue(offsetUnits);
            } else {
                x= vds.getXTagDouble(i,vds.getXUnits());
            }
            pout.print(""+x+tab);
            pout.print(""+vds.getDouble(i,vds.getYUnits()));
            pout.println();            
        }
        
        pout.close();
    }
}