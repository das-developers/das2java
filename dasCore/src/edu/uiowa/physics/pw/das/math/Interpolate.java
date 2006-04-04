/*
 * Interpolate.java
 *
 * Created on March 15, 2005, 9:00 AM
 */

package edu.uiowa.physics.pw.das.math;

/**
 *
 * @author Jeremy
 */
public class Interpolate {
    
    public interface DDoubleArray {
        double get( int i, int j );
        void put( int i, int j, double value );
        int rows();
        int columns();
    }
    
    public interface FDoubleArray {
        float get( int i, int j );
        void put( int i, int j, float value );
        int rows();
        int columns();
    }
    
    public static DDoubleArray newDDoubleArray( final double[][] array ) {
        return new DDoubleArray() {
            public double get( int i, int j ) { return array[i][j]; }
            public void put( int i, int j, double value ) { array[i][j]= value; };
            public int rows() { return array.length; }
            public int columns() { return array[0].length; }
        };
    }
    
    public static DDoubleArray newDDoubleArray( int columns, int rows ) {
        return new DDoubleArrayImpl( columns, rows );
    }
    
    private static final class DDoubleArrayImpl implements DDoubleArray {
        // return DDoubleArray backed by single array, storing the double array column major.
        double[] back;
        int rows; // index by i
        int columns;
        static final boolean boundsCheck= false;
        DDoubleArrayImpl( int columns, int rows ) {
            back= new double[ rows * columns ];
            this.columns= columns;
            this.rows= rows;
        }
        public double get( int i, int j ) {
            if ( boundsCheck ) {
                if ( i>=rows ) throw new ArrayIndexOutOfBoundsException("index i="+i+" out of bounds");
                if ( i<0 ) throw new ArrayIndexOutOfBoundsException("index i="+i+" out of bounds");
                if ( j>=columns ) throw new ArrayIndexOutOfBoundsException("index j="+j+" out of bounds");
            }
            return back[ j*rows + i ];
        }
        public void put( int i, int j, double value ) { 
            if ( boundsCheck ) {
                if ( i>=rows ) throw new ArrayIndexOutOfBoundsException("index i="+i+" out of bounds");
                if ( i<0 ) throw new ArrayIndexOutOfBoundsException("index i="+i+" out of bounds");
                if ( j>=columns ) throw new ArrayIndexOutOfBoundsException("index j="+j+" out of bounds");                
            }
            back[ j*rows + i ]= value; 
        };
        public int rows() { return rows; }
        public int columns() { return columns; }
    }
    
    public static DDoubleArray interpolate2( DDoubleArray source, float[] xFindex, float[] yFindex ) {
        
        DDoubleArray result= newDDoubleArray( xFindex.length, yFindex.length );
        double[] xfp0= new double[xFindex.length];
        double[] xfp1= new double[xFindex.length];
        int[] xip0= new int[xFindex.length];
        int[] xip1= new int[xFindex.length];
        for ( int i=0; i<xFindex.length; i++ ) {
            xip0[i]= (int)xFindex[i];            
            if ( xip0[i]==(source.rows()-1) ) {
                xip0[i]--;            
            }
            xip1[i]= 1+xip0[i];
            xfp0[i]= xFindex[i]-xip0[i];
            xfp1[i]= 1-xfp0[i];
        }
        
        double[] yfp0= new double[yFindex.length];
        double[] yfp1= new double[yFindex.length];
        int[] yip0= new int[yFindex.length];
        int[] yip1= new int[yFindex.length];
        for ( int i=0; i<yFindex.length; i++ ) {
            yip0[i]= (int)yFindex[i];
            if ( yip0[i]==(source.columns()-1) ) {
                yip0[i]--;            
            }
            yip1[i]= 1+yip0[i];
            yfp0[i]= yFindex[i]-yip0[i];
            yfp1[i]= 1-yfp0[i];
        }
        
        for ( int i=0; i<xFindex.length; i++ ) {
            for ( int j=0; j<yFindex.length; j++ ) {                
                double value=
                        source.get( xip0[i], yip0[j] ) * xfp1[i] * yfp1[j] +
                        source.get( xip0[i], yip1[j] ) * xfp1[i] * yfp0[j] +
                        source.get( xip1[i], yip0[j] ) * xfp0[i] * yfp1[j] +
                        source.get( xip1[i], yip1[j] ) * xfp0[i] * yfp0[j];
                result.put( i, j, value );
            }
        }
        return result;
    }
    
    public static DDoubleArray interpolate( DDoubleArray source, float[] xFindex, float[] yFindex ) {
        
        DDoubleArray result= newDDoubleArray( xFindex.length, yFindex.length );
        double[] xfp0= new double[xFindex.length];
        double[] xfp1= new double[xFindex.length];
        int[] xip0= new int[xFindex.length];
        int[] xip1= new int[xFindex.length];
        for ( int i=0; i<xFindex.length; i++ ) {
            xip0[i]= (int)xFindex[i];            
            if ( xip0[i]==(source.rows()-1) ) {
                xip0[i]--;            
            }
            xip1[i]= 1+xip0[i];
            xfp0[i]= xFindex[i]-xip0[i];
            xfp1[i]= 1-xfp0[i];
        }
        
        double[] yfp0= new double[yFindex.length];
        double[] yfp1= new double[yFindex.length];
        int[] yip0= new int[yFindex.length];
        int[] yip1= new int[yFindex.length];
        for ( int i=0; i<yFindex.length; i++ ) {
            yip0[i]= (int)yFindex[i];
            if ( yip0[i]==(source.columns()-1) ) {
                yip0[i]--;            
            }
            yip1[i]= 1+yip0[i];
            yfp0[i]= yFindex[i]-yip0[i];
            yfp1[i]= 1-yfp0[i];
        }
        
        for ( int i=0; i<xFindex.length; i++ ) {
            for ( int j=0; j<yFindex.length; j++ ) {                
                double a00= xfp0[i] * yfp0[j]; // the area of the 0,0 rectangle opposite point 1,1
                double a01= xfp0[i] * yfp1[j];
                double a10= xfp1[i] * yfp0[j];
                double a11= xfp1[i] * yfp1[j];
                double p00= source.get( xip0[i], yip0[j] );
                double p01= source.get( xip0[i], yip1[j] );
                double p10= source.get( xip1[i], yip0[j] );
                double p11= source.get( xip1[i], yip1[j] );
                double value=
                        p00 * a11 +
                        p01 * a10 +
                        p10 * a01 +
                        p11 * a00;
                result.put( i, j, value );
            }
        }
        return result;
    }
    
}
