/*
 * SyncUtil.java
 *
 * Created on April 7, 2004, 1:11 PM
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.datum.*;

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
    
    private static int[] calculateImapForWidthTags( DataSet source, DataSet target ) {
        int[] imap= new int[target.getXLength()];        
        Units xunits= source.getXUnits();
        String widthsPlane= "xTagWidth";
        
        VectorDataSet widthsDs= (VectorDataSet)source.getPlanarView(widthsPlane);
        
        for ( int i=0; i<imap.length; i++ ) {
            imap[i]= -1;
            Datum tt= target.getXTagDatum(i);
            Datum s1= null;
            for ( int k=0; ( s1==null || s1.le(tt) ) && k<source.getXLength(); k++ ) {
                s1= source.getXTagDatum(k);
                Datum s2= s1.add( widthsDs.getDatum(k) );                
                if ( s1.le(tt) && tt.lt(s2) ) {
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
                
        public edu.uiowa.physics.pw.das.datum.Datum getDatum(int i) {
            if ( imap[i]!=-1 ) {
                return source.getDatum( imap[i] );
            } else {
                return source.getYUnits().createDatum( source.getYUnits().getFillDouble() );
            }
        }
        
        
        public double getDouble(int i, edu.uiowa.physics.pw.das.datum.Units units) {
            if ( imap[i]!=-1 ) {
                return source.getDouble( imap[i], units );
            } else {
                return units.getFillDouble();
            }
        }
        
        public int getInt(int i, edu.uiowa.physics.pw.das.datum.Units units) {
            if ( imap[i]!=-1 ) {
                return source.getInt( imap[i], units );
            } else {
                return units.getFillInt();
            }
        }
        
        public edu.uiowa.physics.pw.das.dataset.DataSet getPlanarView(String planeID) {
            return new NearestNeighborVectorDataSet( (VectorDataSet)source.getPlanarView(planeID), imap, xtags );
        }
        
        public Object getProperty(String name) {
            return source.getProperty(name);
        }
        
        public int getXLength() {
            return imap.length;
        }
        
        public edu.uiowa.physics.pw.das.datum.Datum getXTagDatum(int i) {
            return getXUnits().createDatum(xtags[i]);
        }
        
        public double getXTagDouble(int i, edu.uiowa.physics.pw.das.datum.Units units) {
            return getXUnits().convertDoubleTo(units, imap[i] );
        }
        
        public int getXTagInt(int i, edu.uiowa.physics.pw.das.datum.Units units) {
            throw new IllegalArgumentException("not implemented");
        }
        
        public edu.uiowa.physics.pw.das.datum.Units getXUnits() {
            return source.getXUnits();
        }
        
        public edu.uiowa.physics.pw.das.datum.Units getYUnits() {
            return source.getYUnits();
        }
        
        
    }
    
    public static DataSet syncronizeNearestNeighbor( DataSet source, DataSet target ) {
        if ( source instanceof VectorDataSet ) {
            return SyncUtil.NearestNeighborVectorDataSet.create( (VectorDataSet)source, target );
        } else {
            throw new IllegalArgumentException("Unsupported DataSet Type: "+source );
        }
    }
    
}
