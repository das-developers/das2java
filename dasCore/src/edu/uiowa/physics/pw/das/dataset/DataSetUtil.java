/*
 * DataSetUtil.java
 *
 * Created on January 26, 2004, 6:03 PM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.DasMath;
import java.util.*;
import javax.swing.*;

/**
 *
 * @author  Jeremy
 */
public class DataSetUtil {
    
    public static CacheTag guessCacheTag( DataSet ds ) {
        if ( ds.getProperty(DataSet.PROPERTY_CACHE_TAG)!=null ) {
            return (CacheTag)ds.getProperty(DataSet.PROPERTY_CACHE_TAG);
        } else {
            Datum start= ds.getXTagDatum(0);
            Datum end= ds.getXTagDatum( ds.getXLength()-1 );
            Datum resolution= ds.getXTagDatum(1).subtract( start );
            return new CacheTag( start, end, resolution );
        }
    }
                
    public static DatumRange xRange( DataSet ds ) {
        int n=ds.getXLength();
        return new DatumRange( ds.getXTagDatum(0), ds.getXTagDatum(n-1) );
    }
    
    public static DatumRange yRange( DataSet ds ) {
        if ( ds.getProperty(DataSet.PROPERTY_Y_RANGE)!=null ) return (DatumRange)ds.getProperty(DataSet.PROPERTY_Y_RANGE);
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
    
    public static DasPlot visualize( DataSet ds ) {
        return GraphUtil.visualize( ds );        
    }
    
    public static DasPlot visualize( DataSet ds, boolean ylog ) {    
        return GraphUtil.visualize( ds, ylog );
    }
    
    /**
     * Provide a reasonable xTagWidth either by returning the specified xTagWidth property,
     * or by statistically looking at the X tags.
     */
    public static Datum guessXTagWidth( DataSet table ) {
        if ( table.getProperty(DataSet.PROPERTY_X_TAG_WIDTH)!=null ) return (Datum)table.getProperty(DataSet.PROPERTY_X_TAG_WIDTH);
        if ( table.getXLength()>2 ) {
            Datum min= table.getXTagDatum(1).subtract( table.getXTagDatum(0) );
            for ( int i=2; i<table.getXLength(); i++ ) {
                Datum min0= table.getXTagDatum(i).subtract( table.getXTagDatum(i-1) );
                if ( min0.lt(min) ) min= min0;
            }
            return min;
        } else {
            // We're in trouble now!
            return table.getXUnits().getOffsetUnits().createDatum(0);
        }
    }
    
    /* finds the element closest to x in xx.  xx must be sorted!!! */
    protected static int closest( double[] xx, double x ) {
        if ( xx.length==0 ) {
            throw new IllegalArgumentException("array has no elements");
        }
        long t0= System.currentTimeMillis();
        int result= Arrays.binarySearch( xx, x );
        if (result == -1) {
            result = 0; //insertion point is 0
        } else if (result < 0) {
            result= ~result; // usually this is the case
            if ( result >= xx.length-1 ) {
                result= xx.length-1;
            } else {
                result= ( ( x-xx[result-1] ) / ( xx[result] - xx[result-1] ) < 0.5 ? result-1 : result );
            }
        }
        return result;
    }
    
    /**
     * returns the index of a tag, or the  <tt>(-(<i>insertion point</i>) - 1)</tt>.  (See Arrays.binarySearch)
     */
    public static int xTagBinarySearch( DataSet ds, Datum datum, int low, int high ) {
        Units units= datum.getUnits();
        double key= datum.doubleValue(units);
        while (low <= high) {
            int mid = (low + high) >> 1;
            double midVal = ds.getXTagDouble(mid,units);
            int cmp;
            if (midVal < key) {
                cmp = -1;   // Neither val is NaN, thisVal is smaller
            } else if (midVal > key) {
                cmp = 1;    // Neither val is NaN, thisVal is larger
            } else {
                long midBits = Double.doubleToLongBits(midVal);
                long keyBits = Double.doubleToLongBits(key);
                cmp = (midBits == keyBits ?  0 : // Values are equal
                    (midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
                        1));                     // (0.0, -0.0) or (NaN, !NaN)
            }
            
            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1);  // key not found.
    }
    
    
    public static int closestColumn( DataSet table, Datum datum ) {
        int result= xTagBinarySearch( table, datum, 0, table.getXLength()-1 );
        if (result == -1) {
            result = 0; //insertion point is 0
        } else if (result < 0) {
            result= ~result; // usually this is the case
            if ( result >= table.getXLength()-1 ) {
                result= table.getXLength()-1;
            } else {
                double x= datum.doubleValue( datum.getUnits() );
                double x0= table.getXTagDouble(result-1, datum.getUnits() );
                double x1= table.getXTagDouble(result, datum.getUnits() );
                result= ( ( x-x0 ) / ( x1 - x0 ) < 0.5 ? result-1 : result );
            }
        }
        return result;
    }
    
    public static int closestColumn( DataSet table, double x, Units units ) {
        return closestColumn( table, units.createDatum(x) );
    }
    
    public static int closestColumn( DataSet table, Datum xdatum, int guessIndex ) {
        int result= guessIndex;
        int len= table.getXLength();
        if ( result>=len ) result=len-1;
        if ( result<0 ) result=0;
        Units units= xdatum.getUnits();
        double x= xdatum.doubleValue(units);
        while ( result<(len-1) && table.getXTagDouble(result,units)<x ) result++;
        while ( result>0 && table.getXTagDouble(result,units)>x ) result--;
        if ( result<len-2 ) {
            double alpha= ( x - table.getXTagDouble( result, units ) ) /
                    ( table.getXTagDouble( result+1, units ) - table.getXTagDouble( result, units ) );
            result= ( alpha < 0.5 ? result : result+1 );
        }
        return result;
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
    
    public static DatumRange zRange( DataSet ds ) {
        if ( !( ds instanceof TableDataSet ) ) {
            throw new UnsupportedOperationException("only TableDataSets supported");
        }
        
        if ( ds.getProperty(DataSet.PROPERTY_Z_RANGE)!=null ) {
            return (DatumRange)ds.getProperty(DataSet.PROPERTY_Z_RANGE);
        }
        
        TableDataSet tds= (TableDataSet)ds;
        
        double max= Double.NEGATIVE_INFINITY;
        double min= Double.POSITIVE_INFINITY;
        
        double fill= tds.getZUnits().getFillDouble();
        Units zunits= tds.getZUnits();
        
        for ( int itable=0; itable<tds.tableCount(); itable++ ) {
            int ny= tds.getYLength(itable);
            for (int i=tds.tableStart(itable); i<tds.tableEnd(itable); i++) {
                for (int j=0; j<ny; j++) {
                    double d= tds.getDouble(i,j,zunits);
                    if ( d!=fill ) {
                        max= Math.max( d, max );
                        min= Math.min( d, min );
                    }
                }
            }
        }
        if ( max==Double.NEGATIVE_INFINITY ) {
            max= fill;
            min= fill;
        }
        return DatumRange.newDatumRange( min, max, zunits );
        
    }
    
    /* Give an estimate of the size of the data set, or PROPERTY_SIZE_BYTES if available.
     * Generally this would be used as a penalty against the dataset, and it's probably
     * better to overestimate the dataset size.
     */
    public static long guessSizeBytes( DataSet ds ) {
        Long o= (Long)ds.getProperty( DataSet.PROPERTY_SIZE_BYTES );
        if ( o != null ) {
            return o.longValue();
        } else {
            long planeCount= ds.getPlaneIds().length;
            int datumBytes= 8; // storage size for each datum
            long sizeXTags= ds.getXLength() * datumBytes;
            if ( ds instanceof TableDataSet ) {
                TableDataSet tds= (TableDataSet)ds;
                long sizeBytes=0;
                for ( int i=0; i<tds.tableCount(); i++ ) {
                    sizeBytes+= ( tds.tableEnd(i)-tds.tableStart(i) ) * tds.getYLength(i) * planeCount * datumBytes;
                }
                return sizeBytes + sizeXTags;
            } else {
                return ds.getXLength() * planeCount * datumBytes + sizeXTags;
            }
        }
    }
    
    /**
     * provides convenient method for appending datasets together.  The first
     * dataset may be null, in which case the second is trivially returned.
     * Presently a builder is used to create the new dataset, but in the future
     * more efficient methods will be used.  ds2 must not be null!
     */
    public static DataSet append( DataSet ds1, DataSet ds2, CacheTag ct ) {
        CacheTag resultTag=null;
        if ( ct!=null ) {
            resultTag= ct;
        } else {
            if ( ds2!=null ) resultTag= DataSetUtil.guessCacheTag( ds2 );
        }
        if ( ds1!=null ) {
            if ( resultTag==null ) {
                resultTag= DataSetUtil.guessCacheTag( ds1 );
            } else {
                resultTag= CacheTag.append( DataSetUtil.guessCacheTag( ds1 ), resultTag );
            }
        }
        if ( ds2==null ) {
            return ds1;
        } else {
            if ( ds2 instanceof TableDataSet ) {
                TableDataSetBuilder builder= new TableDataSetBuilder( ds2.getXUnits(), ds2.getYUnits(), ((TableDataSet)ds2).getZUnits() );
                if ( ds1!=null ) builder.append( (TableDataSet) ds1 );
                builder.append( (TableDataSet) ds2 );
                builder.setProperty( DataSet.PROPERTY_CACHE_TAG, resultTag );
                return builder.toTableDataSet();
            } else {
                VectorDataSetBuilder builder= new VectorDataSetBuilder( ds2.getXUnits(), ds2.getYUnits() );
                if ( ds1!=null ) builder.append( (VectorDataSet) ds1 );
                builder.append( (VectorDataSet) ds2 );
                builder.setProperty( DataSet.PROPERTY_CACHE_TAG, resultTag );
                return builder.toVectorDataSet();
            }
        }
    }
    
    public static DataSet append( DataSet ds1, DataSet ds2 ) {
        return append( ds1, ds2, null );
    }
    
    public static VectorDataSet log10( VectorDataSet ds ) {
        VectorDataSetBuilder builder= new VectorDataSetBuilder( ds.getXUnits(), Units.dimensionless );
        Units yunits= ds.getYUnits();
        Units xunits= ds.getXUnits();
        for ( int i=0; i<ds.getXLength(); i++ ) {
            builder.insertY( ds.getXTagDouble(i,xunits), DasMath.log10(ds.getDouble(i,yunits)) );
        }
        return builder.toVectorDataSet();
    }
    
    /**
     * returns all planes, including the default plane "".  This is to take care of
     * inconsistent behavior of the ds.getPlaneIds() implementations.
     */
    public static String[] getAllPlaneIds( DataSet ds ) {
        String[] planes= ds.getPlaneIds();
        boolean haveDefault=false;
        for ( int i=0; i<planes.length; i++ ) {
            if ( planes[i].equals("") ) haveDefault=true;
        }
        if ( haveDefault ) {
            return planes;
        } else {
            String[] newPlanes= new String[ planes.length+1] ;
            newPlanes[0]= "";
            for ( int i=0; i<planes.length; i++ ) {
                newPlanes[i+1]= planes[i];
            }
            return newPlanes;
        }
    }
}
