/*
 * TableUtil.java
 *
 * Created on November 14, 2003, 6:47 PM
 */

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
public class TableUtil {
    
    //  maybe a cache to keep track of last finds
    
    public static double[] getXTagArrayDouble( TableDataSet table, Units units ) {
        
        int ixmax= table.tableEnd(table.tableCount()-1);
        double[] xx= new double[ixmax];
        for ( int i=0; i<ixmax; i++ ) {
            xx[i]= table.getXTagDouble(i,units);
        }
        return xx;
    }
    
    public static double[] getYTagArrayDouble( TableDataSet table, int itable, Units units ) {
        double[] yy= new double[table.getYLength(itable)];
        for ( int j=0; j<yy.length; j++ ) {
            yy[j]= table.getYTagDouble(itable,j,units);
        }
        return yy;
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
    
    public static int closestColumn( TableDataSet table, Datum datum ) {
        return closestColumn( table, datum.doubleValue(datum.getUnits()), datum.getUnits() );
    }
    
    public static int closestColumn( TableDataSet table, double x, Units units ) {
        double [] xx= getXTagArrayDouble( table, units );
        return closest( xx, x );
    }
    
    public static int closestRow( TableDataSet table, int itable, Datum datum ) {
        return closestRow( table, itable, datum.doubleValue(datum.getUnits()), datum.getUnits() );
    }
    
    public static int closestRow( TableDataSet table, int itable, double x, Units units ) {
        double [] xx= getYTagArrayDouble( table, itable, units );
        return closest( xx, x );
    }
    
    public static Datum closestDatum( TableDataSet table, Datum x, Datum y ) {
        int i= closestColumn( table, x );
        int j= closestRow( table, table.tableOfIndex(i), y );
        return table.getDatum(i,j);
    }
    
    public static int tableIndexAt( TableDataSet table, int i ) {
        int itable=0;
        while ( table.tableEnd(itable)<=i ) itable++;
        return itable;
    }
    
    public static Datum guessXTagWidth( TableDataSet table ) {
        if ( table.getXLength()>1 ) {
            return table.getXTagDatum(1).subtract( table.getXTagDatum(0) );
        } else {
            return table.getXUnits().getOffsetUnits().createDatum(0);
        }
    }
    
    public static Datum guessYTagWidth( TableDataSet table ) {
        // cheat and check for logarithmic scale.  If logarithmic, then return YTagWidth as percent.
        double y0= table.getYTagDouble( 0, 0, table.getYUnits());
        double y1= table.getYTagDouble( 0, 1, table.getYUnits());
        int n= table.getYLength(0)-1;
        double yn= table.getYTagDouble( 0, n, table.getYUnits() );
        if ( (yn-y0) / ( (y1-y0 ) * n ) > 10. ) {
            return Units.percent.createDatum( ( (y1/y0) - 1.0 ) * 100 );
        } else {
            return table.getYUnits().createDatum(y1-y0);
        }
    }
    
    public static double tableMax( TableDataSet tds ) {
        double result= Double.NEGATIVE_INFINITY;
        
        for ( int itable=0; itable<tds.tableCount(); itable++ ) {
            int ny= tds.getYLength(itable);
            for (int i=tds.tableStart(itable); i<tds.tableEnd(itable); i++) {
                for (int j=0; j<ny; j++) {
                    if ( tds.getDouble(i,j,tds.getZUnits()) > result ) {
                        result= tds.getDouble(i,j,tds.getZUnits());
                    }
                }
            }
        }
        return result;
    }
    
    public static void dumpToAsciiStream( TableDataSet tds, OutputStream out ) {
        PrintStream pout= new PrintStream(out);
        
        Datum base=null;
        Units offsetUnits= null;
        
        pout.print("[00]");
        pout.println("<stream start=\""+tds.getXTagDatum(0)+"\" end=\""+tds.getXTagDatum(tds.getXLength()-1)+"\" >");
        pout.println("<comment>Stream creation date: "+TimeUtil.now().toString()+"</comment>");
        pout.print("</stream>");
        
        if ( tds.getXUnits() instanceof LocationUnits ) {
            base= tds.getXTagDatum(0);
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
            pout.print(" xUnits=\""+tds.getXUnits());
        }
        pout.println(" />");
        
        pout.println("<yscan type=\"asciiTab10\" zUnits=\""+tds.getZUnits()+"\" />");
        pout.print("</packet>");
        
        NumberFormat xnf= new DecimalFormat("00000.000");
        NumberFormat ynf= new DecimalFormat("0.00E00");
        
        for (int i=0; i<tds.getXLength(); i++) {
            pout.print(":01:");
            double x;
            if ( base!=null ) {
                x= tds.getXTagDatum(i).subtract(base).doubleValue(offsetUnits);
            } else {
                x= tds.getXTagDouble(i,tds.getXUnits());
            }
            pout.print(xnf.format(x)+" ");
            int itable= tds.tableOfIndex(i);
            for ( int j=0; j<tds.getYLength(itable); j++ ) {
                String delim;
                if ( (j+1)==tds.getYLength(itable) ) {
                    delim= "\n";
                } else {
                    delim= " ";
                }
                pout.print(FixedWidthFormatter.format(ynf.format(tds.getDouble(i,j,tds.getZUnits())),9)+delim);
            }
        }
        
        pout.close();
    }
    
    public static void dumpToStream( TableDataSet table, OutputStream out ) {
        
        PrintStream pout= new PrintStream(out);
        
        Datum base=null;
        Units offsetUnits= null;
        
        if ( table.getXUnits() instanceof LocationUnits ) {
            base= table.getXTagDatum(0);
            offsetUnits= ((LocationUnits)base.getUnits()).getOffsetUnits();
            if ( offsetUnits==Units.microseconds ) {
                offsetUnits= Units.seconds;
            }
            pout.println("# X is first value, offset in "+offsetUnits+" from "+base);
        } else {
            pout.println("# X is first value, in "+table.getXUnits());
        }
        
        pout.println("# Z values follow, in "+table.getZUnits());
        pout.println("# This file contains "+table.tableCount()+" tables.");
        
        pout.println("#");
        pout.println("# File created on: "+TimeUtil.now().toString()+" UT");
        String tab= "\011";
        
        for ( int itable=0; itable< table.tableCount(); itable++ ) {
            pout.println("# Begin table "+itable);
            
            pout.print("# yValues ("+table.getYUnits()+"):"+tab);
            for (int j=0; j<table.getYLength(itable); j++ ) {
                pout.print(""+table.getYTagDouble(itable,j,table.getYUnits())+tab);
            }
            pout.println();
            
            for (int i=table.tableStart(itable); i<table.tableEnd(itable); i++) {
                double x;
                if ( base!=null ) {
                    x= table.getXTagDatum(i).subtract(base).doubleValue(offsetUnits);
                } else {
                    x= table.getXTagDouble(i,table.getXUnits());
                }
                pout.print(""+x+tab);
                for (int j=0; j<table.getYLength(itable); j++) {
                    pout.print(""+table.getDouble(i,j,table.getZUnits())+tab);
                }
                pout.println();
            }
            
        }
    }
}