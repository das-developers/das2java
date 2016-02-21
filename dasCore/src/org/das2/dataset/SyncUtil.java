/*
 * SyncUtil.java
 *
 * Created on April 7, 2004, 1:11 PM
 */

package org.das2.dataset;

import org.das2.datum.Datum;
import org.das2.datum.Units;
import java.util.Map;

/**
 *
 * @author  Jeremy
 */
public class SyncUtil {
        
    private static int[] calculateImap( DataSet source, DataSet target ) {
        int[] imap= new int[target.getXLength()];        
        Units xunits= source.getXUnits();
        
        Datum xTagWidth= (Datum)source.getProperty("xTagWidth");
        if ( xTagWidth==null ) xTagWidth= DataSetUtil.guessXTagWidth(source);
        
        for ( int i=0; i<imap.length; i++ ) {
            double xx= target.getXTagDouble(i,xunits);
            imap[i]= DataSetUtil.closestColumn( source, xx, xunits );
                      
            Datum xclose= source.getXTagDatum(imap[i]);
            
            if ( Math.abs(xclose.subtract(xx,xunits).doubleValue(xunits)) > xTagWidth.doubleValue(xunits)/2. ) {
                imap[i]=-1;
            }
        }
        return imap;
    }
    
    /* calculates imap when width tags are irregular, and possibly overlapping.
     */
    private static int[] calculateImapForWidthTags( DataSet source, DataSet target ) {
        int[] imap= new int[target.getXLength()];        
        Units xunits= source.getXUnits();
        Units xoffsetUnits= xunits.getOffsetUnits();
        String widthsPlane= "xTagWidth";
        
        VectorDataSet widthsDs= (VectorDataSet)source.getPlanarView(widthsPlane);
        
        int sourceLength= source.getXLength();
        
        for ( int i=0; i<imap.length; i++ ) {
            imap[i]= -1;
            double tt= target.getXTagDouble(i,xunits);
            double s1= -999;
            
            for ( int k=0; ( s1==-999 || s1<=tt ) && k<sourceLength; k++ ) {
                s1= source.getXTagDouble(k,xunits);
                double s2= s1 + widthsDs.getDouble( k, xoffsetUnits );
                if ( s1<=tt && tt<s2 ) {
                    imap[i]= k;
                }
            }
        }
        return imap;
    }
    
     /* return a data set with the timetags of target and the data of source, by using
      * nearest neighbor matching
      */
    private static class NearestNeighborVectorDataSet implements VectorDataSet {
        VectorDataSet source;
        int[] imap;
        double[] xtags;

        NearestNeighborVectorDataSet( VectorDataSet source, int[] imap, double[] xtags ) {
            this.source= source;
            this.imap= imap;
            this.xtags= xtags;
        }
        
        private static NearestNeighborVectorDataSet create( VectorDataSet source, DataSet target ) {            
            if ( source.getPlanarView("xTagWidth")!=null ) {
                return new NearestNeighborVectorDataSet( source, calculateImapForWidthTags(source,target), DataSetUtil.getXTagArrayDouble(target, source.getXUnits() ) );
            } else { 
                return new NearestNeighborVectorDataSet( source, calculateImap( source, target ), DataSetUtil.getXTagArrayDouble(target, source.getXUnits() ) );
            }
        }
                
        public Datum getDatum(int i) {
            if ( imap[i]!=-1 ) {
                return source.getDatum( imap[i] );
            } else {
                return source.getYUnits().createDatum( source.getYUnits().getFillDouble() );
            }
        }
        
        
        public double getDouble(int i, Units units) {
            if ( imap[i]!=-1 ) {
                return source.getDouble( imap[i], units );
            } else {
                return units.getFillDouble();
            }
        }
        
        public int getInt(int i, Units units) {
            if ( imap[i]!=-1 ) {
                return source.getInt( imap[i], units );
            } else {
                return units.getFillDatum().intValue(units);
            }
        }
        
        public org.das2.dataset.DataSet getPlanarView(String planeID) {
            return new NearestNeighborVectorDataSet( (VectorDataSet)source.getPlanarView(planeID), imap, xtags );
        }
        
        public String[] getPlaneIds() {
            return source.getPlaneIds();
        }
        
            
        public Object getProperty(String name) {
            return source.getProperty(name);
        }
        
        public Map getProperties() {
            return source.getProperties();
        }
                
        public int getXLength() {
            return imap.length;
        }
        
        public Datum getXTagDatum(int i) {
            return getXUnits().createDatum(xtags[i]);
        }
        
        public double getXTagDouble(int i, Units units) {
            return getXUnits().convertDoubleTo(units, imap[i] );
        }
        
        public int getXTagInt(int i, Units units) {
            throw new IllegalArgumentException("not implemented");
        }
        
        public Units getXUnits() {
            return source.getXUnits();
        }
        
        public Units getYUnits() {
            return source.getYUnits();
        }
        
        
    }
    
    public static DataSet synchronizeNearestNeighbor( DataSet source, DataSet target ) {
        if ( source instanceof VectorDataSet ) {
            return SyncUtil.NearestNeighborVectorDataSet.create( (VectorDataSet)source, target );
        } else {
            throw new IllegalArgumentException("Unsupported DataSet Type: "+source );
        }
    }
    
}
