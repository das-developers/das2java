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
        
    public static double[] getYTagArrayDouble( TableDataSet table, int itable, Units units ) {
        double[] yy= new double[table.getYLength(itable)];
        for ( int j=0; j<yy.length; j++ ) {
            yy[j]= table.getYTagDouble(itable,j,units);
        }
        return yy;
    }
     
    public static Datum getLargestYTag( TableDataSet tds ) {
        Datum result= tds.getYTagDatum( 0, tds.getYLength(0)-1 );
        for ( int itable=1; itable<tds.tableCount(); itable++ ) {
            Datum r= tds.getYTagDatum( itable, tds.getYLength(itable)-1 );
            if ( r.gt(result) ) result= r;
        }            
        return result;
    }
    
    public static Datum getSmallestYTag( TableDataSet tds ) {
        Datum result= tds.getYTagDatum( 0, 0 );
        for ( int itable=1; itable<tds.tableCount(); itable++ ) {
            Datum r= tds.getYTagDatum( itable, 0 );
            if ( r.lt(result) ) result= r;
        }                   
        return result;
    }
    
    public static int closestRow( TableDataSet table, int itable, Datum datum ) {
        return closestRow( table, itable, datum.doubleValue(datum.getUnits()), datum.getUnits() );
    }
    
    public static int closestRow( TableDataSet table, int itable, double x, Units units ) {
        double [] xx= getYTagArrayDouble( table, itable, units );
        return DataSetUtil.closest( xx, x );
    }
    
    public static Datum closestDatum( TableDataSet table, Datum x, Datum y ) {
        int i= DataSetUtil.closestColumn( table, x );
        int j= closestRow( table, table.tableOfIndex(i), y );
        return table.getDatum(i,j);
    }
    
    public static int tableIndexAt( TableDataSet table, int i ) {
        int itable=0;
        while ( table.tableEnd(itable)<=i ) itable++;
        return itable;
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
    
    public static String toString(TableDataSet tds) {
        StringBuffer buffer= new StringBuffer();
        buffer.append( tds.getYLength(0) );
        for ( int i=1; i<tds.tableCount(); i++ ) {
            buffer.append( ", "+tds.getYLength(i) );
        }        
        return "["+tds.getXLength()+" xTags, "+buffer.toString()+" yTags]";        
    }
    
    public static void dumpToAsciiStream( TableDataSet tds, OutputStream out ) {
        PrintStream pout= new PrintStream(out);
        
        Datum base=null;
        Units offsetUnits= null;
        
        pout.print("This is not a das2 stream, even though it looks like it.");
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
        
        String yTagsString= ""+tds.getYTagDatum(0,0);
        for ( int j=1; j<tds.getYLength(0); j++ ) {
            yTagsString+= ", "+tds.getYTagDatum(0, j);
        }        
        pout.println("<yscan type=\"asciiTab10\" zUnits=\""+tds.getZUnits()+"\" yTags=\""+yTagsString+"\"/>");
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

    public static int getPreviousRow( TableDataSet ds, int itable, Datum datum ) {
        int i= closestRow( ds, itable, datum );
        if ( i>0 && ds.getYTagDatum(itable,i).gt(datum) ) {
            return i-1;
        } else {
            return i;
        }
    }
    
    public static int getNextRow( TableDataSet ds, int itable, Datum datum ) {
        int i= closestRow( ds, itable, datum );
        if ( i<ds.getXLength()-1 && ds.getYTagDatum(itable,i).lt(datum) ) {
            return i+1;
        } else {
            return i;
        }
    }
}