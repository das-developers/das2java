package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;
import java.io.*;
import java.text.*;
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
    
    public static void dumpToAsciiStream( VectorDataSet vds, OutputStream out ) {
        PrintStream pout= new PrintStream(out);
        
        Datum base=null;
        Units offsetUnits= null;                 
        
        pout.print("[00]");
        pout.println("<stream start=\""+vds.getXTagDatum(0)+"\" end=\""+vds.getXTagDatum(vds.getXLength()-1)+"\" >");
        pout.println("<comment>Stream creation date: "+TimeUtil.now().toString()+"</comment>");
        pout.print("</stream>");
        
        if ( vds.getXUnits() instanceof LocationUnits ) {
            base= vds.getXTagDatum(0);
            offsetUnits= ((LocationUnits)base.getUnits()).getOffsetUnits();
            if ( offsetUnits==Units.microseconds ) {
                offsetUnits= Units.seconds;
            }                       
        } 

        
        pout.print("[01]<packet>\n");
        pout.print("<x type=\"asciiTab10\" ");
        if ( base!=null ) {
            pout.print("base=\""+base+"\" ");
            pout.print(" xUnits=\""+offsetUnits+"\" ");            
        } else {
            pout.print(" xUnits=\""+vds.getXUnits());
        }
        pout.println(" />");
        
        pout.print("<y type=\"asciiTab10\" yUnits=\""+vds.getYUnits()+"\" />");
                                
        NumberFormat xnf= new DecimalFormat("00000.000");
        NumberFormat ynf= new DecimalFormat("0.00E00");                
        
        for (int i=0; i<vds.getXLength(); i++) {
            pout.print(":01:");
            double x;
            if ( base!=null ) {
                x= vds.getXTagDatum(i).subtract(base).doubleValue(offsetUnits);
            } else {
                x= vds.getXTagDouble(i,vds.getXUnits());
            }
            pout.print(xnf.format(x)+" ");
            pout.print(FixedWidthFormatter.format(ynf.format(vds.getDouble(i,vds.getYUnits())),9)+"\n");    
        }
        
        pout.close();
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