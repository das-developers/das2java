/*
 * TableUtil.java
 *
 * Created on November 14, 2003, 6:47 PM
 */

package org.das2.dataset;

import org.das2.datum.LocationUnits;
import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.Datum;
import org.das2.datum.UnitsUtil;
import org.das2.datum.TimeUtil;
import org.das2.stream.StreamProducer;
import org.das2.stream.DataTransferType;
import org.das2.stream.StreamYScanDescriptor;
import org.das2.stream.StreamDescriptor;
import org.das2.stream.StreamXDescriptor;
import org.das2.stream.StreamException;
import org.das2.stream.PacketDescriptor;
import org.das2.util.FixedWidthFormatter;
import java.io.*;
import java.nio.channels.*;
import java.text.*;
import java.util.*;
import java.util.Map.Entry;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

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
        return guessYTagWidth( table, 0 );
    }
    
    /**
     * guess the y tag cadence by returning the difference of the first two tags.
     * If the tags appear to be log spaced, then a ratiometric unit (e.g. percentIncrease)
     * is returned.  monotonically decreasing is handled, in which case a positive tag cadence
     * is returned.
     * @param table
     * @param itable the table index.
     * @return the nominal cadence of the tags.
     */
    public static Datum guessYTagWidth( TableDataSet table, int itable ) {
        // cheat and check for logarithmic scale.  If logarithmic, then return YTagWidth as percent.
        double y0= table.getYTagDouble( itable, 0, table.getYUnits());
        double y1= table.getYTagDouble( itable, 1, table.getYUnits());
        int n= table.getYLength(itable)-1;
        double yn= table.getYTagDouble( itable, n, table.getYUnits() );
        double cycles= (yn-y0) / ( (y1-y0 ) * n );
        if ( y1<y0 ) {
            double t= y0; y0= y1; y1= t;
        }
        if (  cycles > 10. ) {
            return Units.log10Ratio.createDatum( Math.log10(y1/y0) );
        } else {
            if ( (yn-y0)/n > (y1-y0) ) {
                return table.getYUnits().createDatum((yn-y0)/n); // the average is bigger than the first.  maybe return the last.
            } else {
                return table.getYUnits().createDatum(y1-y0);
            }
        }
    }
    public static double tableMax( TableDataSet tds, Units units ) {
        double result= Double.NEGATIVE_INFINITY;
        
        for ( int itable=0; itable<tds.tableCount(); itable++ ) {
            int ny= tds.getYLength(itable);
            for (int i=tds.tableStart(itable); i<tds.tableEnd(itable); i++) {
                for (int j=0; j<ny; j++) {
                    if ( tds.getDouble(i,j,units) > result ) {
                        result= tds.getDouble(i,j,units);
                    }
                }
            }
        }
        return result;
    }
    
    public static void checkForNaN( TableDataSet tds ) {
        for ( int i=0; i<tds.getXLength(); i++ ) {
            for ( int j=0; j<16; j++ ) {
                double zz= tds.getDouble(i,j, tds.getZUnits() );
                if ( Double.isNaN( zz ) ) {
                    System.out.println("found NaN at "+i+","+j );
                    if ( tds.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS)!=null ) {
                        System.out.println("  weight: "+((TableDataSet)tds.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS)).getDouble(i, j, Units.dimensionless ) );
                    }
                } else {
                    // System.out.println("zz="+zz );
                }
            }
        }
    }
    
    protected static void checkForNaN( double[][] t ) {
        for ( int i=0; i<t.length; i++ ) {
            for ( int j=0; j<t[0].length; j++ ) {
                double zz= t[i][j];
                if ( Double.isNaN( zz ) ) {
                    System.out.println("found NaN at "+i+","+j );
                } else {
                    // System.out.println("zz="+zz );
                }
            }
        }
    }
    
    public static String toString(TableDataSet tds) {
        StringBuffer buffer= new StringBuffer();
        if ( tds.tableCount()>0 ) buffer.append( tds.getYLength(0) );
        int tableCountLimit=3;
        for ( int i=1; i<tds.tableCount() && i<tableCountLimit; i++ ) {
            buffer.append( ", "+tds.getYLength(i) );
        }
        return "["+tds.getXLength()+" xTags, "+buffer.toString()+" yTags]";
    }
    
    public static DatumVector getDatumVector( TableDataSet tds, int i ) {
        Units zunits= tds.getZUnits();
        double[] array= new double[tds.getYLength(tds.tableOfIndex(i))];
        for ( int j=0; j<array.length; j++ ) array[j]= tds.getDouble( i,j,zunits );
        return DatumVector.newDatumVector(array, zunits);
    }
    
    public static DatumVector getYTagsDatumVector( TableDataSet tds, int itable ) {
        Units yunits= tds.getYUnits();
        DatumVector result= DatumVector.newDatumVector( TableUtil.getYTagArrayDouble(tds, itable, yunits), yunits );
        return result;
    }
    
    public static void dumpToAsciiStream( TableDataSet tds, Datum xmin, Datum xmax, OutputStream out ) {
        PrintStream pout= new PrintStream(out);
        
        Datum base=null;
        Units offsetUnits= null;
        
        pout.print("This is not a das2 stream, even though it looks like it.");
        pout.print("[00]");
        pout.println("<stream start=\""+xmin+"\" end=\""+xmax+"\" >");
        pout.println("<comment>Stream creation date: "+TimeUtil.now().toString()+"</comment>");
        pout.print("</stream>");
        
        if ( tds.getXUnits() instanceof LocationUnits ) {
            base= xmin;
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
        
        StringBuilder yTagsString= new StringBuilder( );
        yTagsString.append( tds.getYTagDatum(0,0) );
        for ( int j=1; j<tds.getYLength(0); j++ ) {
            yTagsString.append( ", " ).append( tds.getYTagDatum(0,j) );
        }
        pout.println("<yscan type=\"asciiTab10\" zUnits=\""+tds.getZUnits()+"\" yTags=\""+yTagsString+"\"/>");
        pout.print("</packet>");
        
        NumberFormat xnf= new DecimalFormat("00000.000");
        NumberFormat ynf= new DecimalFormat("0.00E00");
        
        double dx= xmax.subtract(xmin).doubleValue(offsetUnits);
        for (int i=0; i<tds.getXLength(); i++) {
            double x;
            if ( base!=null ) {
                x= tds.getXTagDatum(i).subtract(base).doubleValue(offsetUnits);
            } else {
                x= tds.getXTagDouble(i,tds.getXUnits());
            }
            if ( x>=0 && x<dx ) {
                pout.print(":01:");
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
        }
        
        pout.close();
    }
    
    public static void dumpToAsciiStream( TableDataSet tds, OutputStream out ) {
        dumpToAsciiStream(tds, Channels.newChannel(out));
    }
    
    public static void dumpToAsciiStream(TableDataSet tds, WritableByteChannel out) {
        dumpToDas2Stream( tds, out, true, true );
    }
    
    public static void dumpToBinaryStream( TableDataSet tds, OutputStream out ) {
        dumpToDas2Stream(tds, Channels.newChannel(out), false, true );
    }

    /**
     * Write das2stream directly from QDataSet.
     * @param tds rank 2 table or rank 3 join of tables.
     * @param out
     * @param asciiTransferTypes
     * @param sendStreamDescriptor 
     */
    public static void dumpToDas2Stream( QDataSet tds, WritableByteChannel out, boolean asciiTransferTypes, boolean sendStreamDescriptor ) {
        try {
            
            if ( tds.rank()==2 ) { // this way the code can always work with rank 3 datasets.
                tds= Ops.join(null,tds);
            }
            
            QDataSet xds= SemanticOps.xtagsDataSet(tds);
            QDataSet yds= SemanticOps.ytagsDataSet(tds);
            
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            
            Map<String,Object> properties= new LinkedHashMap<>();
            
            Object o;
            
            Units xunits= SemanticOps.getUnits(xds);
            Units yunits= SemanticOps.getUnits(yds);
            Units zunits= SemanticOps.getUnits(tds);
            properties.put( "xUnits", xunits );
            properties.put( "yUnits", yunits );
            properties.put( "zUnits", zunits );
            o= xds.property(QDataSet.LABEL);
            properties.put( DataSet.PROPERTY_X_LABEL, o );
            o= yds.property(QDataSet.LABEL);
            properties.put( DataSet.PROPERTY_Y_LABEL, o );
            o= tds.property(QDataSet.LABEL);
            properties.put( DataSet.PROPERTY_Z_LABEL, o );
            
            DataTransferType zTransferType;
            DataTransferType xTransferType;
            
            if ( asciiTransferTypes ) {
                if ( UnitsUtil.isTimeLocation( SemanticOps.getUnits(xds) ) ) {
                    xTransferType= DataTransferType.getByName("time24");                  
                } else {
                    xTransferType= DataTransferType.getByName("ascii24");
                }
                zTransferType= DataTransferType.getByName("ascii10");
            } else {
                zTransferType= DataTransferType.getByName("sun_real4");
                xTransferType= DataTransferType.getByName("sun_real8");
            }
            
            if ( sendStreamDescriptor ) producer.streamDescriptor(sd);
            DatumVector[] zValues = new DatumVector[1];
            
            for (int table = 0; table < tds.length(); table++) {
                QDataSet tds1= tds.slice(table);
                QDataSet xds1= SemanticOps.xtagsDataSet(tds1);
                QDataSet yds1= SemanticOps.ytagsDataSet(tds1);

                StreamXDescriptor xDescriptor = new StreamXDescriptor();
                xDescriptor.setUnits(xunits);
                xDescriptor.setDataTransferType(xTransferType);                
                StreamYScanDescriptor yDescriptor = new StreamYScanDescriptor();
                yDescriptor.setDataTransferType(zTransferType);
                yDescriptor.setZUnits(zunits);
                yDescriptor.setYCoordinates(org.das2.qds.DataSetUtil.asDatumVector(yds1) );
                PacketDescriptor pd = new PacketDescriptor();
                pd.setXDescriptor(xDescriptor);
                pd.addYDescriptor(yDescriptor);
                producer.packetDescriptor(pd);
                for (int i = 0; i<tds1.length(); i++ ) {
                    Datum xTag = xunits.createDatum( xds1.value(i) );
                    zValues[0] = org.das2.qds.DataSetUtil.asDatumVector( tds1.slice(i) );
                    producer.packet(pd, xTag, zValues);
                }
            }
            if ( sendStreamDescriptor ) producer.streamClosed(sd);
        } catch (StreamException se) {
            throw new RuntimeException(se);
        }
        
    }
    
    /**
     * write the data to a das2Stream
     * @param tds
     * @param out
     * @param asciiTransferTypes
     * @param sendStreamDescriptor if false, then don't send the stream and don't close.
     */
    public static void dumpToDas2Stream( TableDataSet tds, WritableByteChannel out, boolean asciiTransferTypes, boolean sendStreamDescriptor ) {
        try {
            StreamProducer producer = new StreamProducer(out);
            StreamDescriptor sd = new StreamDescriptor();
            
            Map<String,Object> properties= tds.getProperties();
            for ( Entry<String,Object> e: properties.entrySet() ) {
                String key= e.getKey();
                sd.setProperty(key, e.getValue() );
            }
            
            DataTransferType zTransferType;
            DataTransferType xTransferType;
            
            if ( asciiTransferTypes ) {
                if ( UnitsUtil.isTimeLocation(tds.getXUnits()) ) {
                    xTransferType= DataTransferType.getByName("time24");                  
                } else {
                    xTransferType= DataTransferType.getByName("ascii24");
                }
                zTransferType= DataTransferType.getByName("ascii10");
            } else {
                zTransferType= DataTransferType.getByName("sun_real4");
                xTransferType= DataTransferType.getByName("sun_real8");
            }
            
            if ( sendStreamDescriptor ) producer.streamDescriptor(sd);
            DatumVector[] zValues = new DatumVector[1];
            for (int table = 0; table < tds.tableCount(); table++) {
                StreamXDescriptor xDescriptor = new StreamXDescriptor();
                xDescriptor.setUnits(tds.getXUnits());
                xDescriptor.setDataTransferType(xTransferType);                
                StreamYScanDescriptor yDescriptor = new StreamYScanDescriptor();
                yDescriptor.setDataTransferType(zTransferType);
                yDescriptor.setZUnits(tds.getZUnits());
                yDescriptor.setYCoordinates(tds.getYTags(table));
                PacketDescriptor pd = new PacketDescriptor();
                pd.setXDescriptor(xDescriptor);
                pd.addYDescriptor(yDescriptor);
                producer.packetDescriptor(pd);
                for (int i = tds.tableStart(table); i < tds.tableEnd(table); i++) {
                    Datum xTag = tds.getXTagDatum(i);
                    zValues[0] = tds.getScan(i);
                    producer.packet(pd, xTag, zValues);
                }
            }
            if ( sendStreamDescriptor ) producer.streamClosed(sd);
        } catch (StreamException se) {
            throw new RuntimeException(se);
        }
    }
    
    /**
     * return the first row before the datum.  Handles mono decreasing.
     * @return the row which is less than or equal to the datum
     */
    public static int getPreviousRow( TableDataSet ds, int itable, Datum datum ) {
        int i= closestRow( ds, itable, datum );
        Units units= ds.getYUnits();
        double dir= ds.getYTagDouble(itable, 1, units ) - ds.getYTagDouble(itable, 0, units );
        double dd= ds.getYTagDouble(itable,i,units) - datum.doubleValue(units);
        if ( i>0 && ( dir * dd > 0 ) ) {
            return i-1;
        } else {
            return i;
        }
    }
    
    /**
     * return the first row after the datum.  Handles mono decreasing.
     * @return the row which is greater than or equal to the datum
     */
    public static int getNextRow( TableDataSet ds, int itable, Datum datum ) {
        int i= closestRow( ds, itable, datum );
        Units units= ds.getYUnits();
        double dir= ds.getYTagDouble(itable, 1, units ) - ds.getYTagDouble(itable, 0, units );
        double dd= ds.getYTagDouble(itable,i,units) - datum.doubleValue(units);
        if ( i<ds.getYLength(itable)-1 && ( dir * dd < 0 ) ) {
            return i+1;
        } else {
            return i;
        }
    }
    
    public static VectorDataSet collapse( TableDataSet ds, int offset, int length ) {
        int itable= ds.tableOfIndex(offset);
        if ( ds.tableOfIndex(offset+length-1) != itable ) {
            throw new IllegalArgumentException( "collapse can't span multiple tables!" );
        }
        int n= ds.getYLength(itable);
        
        Units zunits= ds.getZUnits();
        Units yunits= ds.getYUnits();
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( ds.getYUnits(), ds.getZUnits() );
        
        TableDataSet weights= WeightsTableDataSet.create(ds);
       
        for ( int j=0; j<n; j++ ) {
            double avg=0.;
            double weight=0.;            
            for ( int i=offset; i<offset+length; i++ ) {
                double w= weights.getDouble( i, j, Units.dimensionless );
                avg+= ds.getDouble(i, j, zunits ) * w;
                weight+= w;
            }
            double d=  ( weight==0 ? zunits.getFillDouble() : avg / weight );
            builder.insertY( ds.getYTagDouble( itable, j, yunits ), d );
        }
        return builder.toVectorDataSet();
    }
}