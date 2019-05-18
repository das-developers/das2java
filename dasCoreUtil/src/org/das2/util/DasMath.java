/* File: DasMath.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author  jbf
 */

public class DasMath {
    
    /** Creates a new instance of DasMath */
    public DasMath() {
    }
    
    private static final double log10=Math.log(10);
    
    public static double log10(double x) {
        return Math.log(x)/log10;
    }
    
    public static double exp10(double x) {
        double result= Math.pow(10,x);
        return result;
    }
    
    public static double exp10(int x) {
        double result= Math.pow(10,x);       
        return result;
    }
    
    public static double roundNFractionalDigits(double x,int n) {                
        double tenToN= Math.pow(10,n-1);
        return Math.round( x * tenToN ) / tenToN;        
    }
    
    public static double roundNSignificantDigits(double x,int n) {
        double sign= x<0 ? -1 : 1;
        double exp= Math.pow(10,Math.floor(Math.log10(sign*x)));
        double mant= x/exp;
        double tenToN= Math.pow(10,n-1);
        mant= Math.round( mant * tenToN ) / tenToN;
        return mant*exp;
    }
    
    public static double tanh( double x ) {
        double sinh = ( Math.exp(x) - Math.exp(-x) )/2;
        double cosh = ( Math.exp(x) + Math.exp(-x) )/2;
        double tanh = sinh / cosh;
        return tanh;
    }
    
    /** Interpolate just one point */
    public static double interpolate( double[] datay, double findex ) {
        int index= (int)findex;
        double alpha= findex - index;
        double result;
        if ( findex<0. ) return datay[0];
        if ( findex>datay.length-1. ) return datay[datay.length-1];
        if ( alpha == 0. ) { // optimization and handle findex=(data.length-1);
            result= datay[index];
        } else {
            result= datay[index] * ( 1.0 - alpha ) + datay[index+1] * alpha ;
        }
        return result;
    }
    
    /** interpolate an array of points. */
    public static double[] interpolate( double[] datay, double[] findex ) {
        double[] result= new double[ findex.length ];
        for ( int i=0; i<findex.length; i++ ) {
            result[i]= interpolate(datay, findex[i]);
        }
        return result;
    }
    
    /** Returns the "floating point indeces" of x within array datax.
     * A floating point index (or "findex") indicates the indeces
     * of the points that bracket and the weights of each bracketing
     * point to use.  A findex of 4.5 indicates that x is half-way
     * between index 4 and 5, so equal weights should be used.
     * floor( findex ) is the "left" bracket, ceil(findex) is the
     * "right" bracket, and fp( findex ) is the weight for the
     * right bracket.  See interpolate for an example of its use.
     */
    public static double[] findex( double[] datax, double[] x ) {
        double[] result= new double[x.length];
        int index= 0;
        for ( int i=0; i<result.length; i++ ) {
            result[i]= findex( datax, x[i], index );
            index= (int) result[i];
        }
        return result;
    }
    
    public final static double findex( double[] datax, double x, int guess ) {
        int index= Math.max( Math.min( guess, datax.length-1 ), 0 );
        while ( index<datax.length-1 && datax[index+1]<x ) index++;
        while ( index>0 && datax[index]>x ) index--;
        if ( index==datax.length-1 ) index--;
        return index + ( x - datax[index] ) / ( datax[index+1] - datax[index] );        
    }
    
    public static void main(String[] args) {
        double x= 1e-18;
        Logger.getLogger("das2.anon").log(Level.FINEST, "x:{0}", x);
        Logger.getLogger("das2.anon").log(Level.FINEST, "roundNDigits:{0}", roundNSignificantDigits(x,3));
        
        double[] x1= new double[] { 1,2,3,4,5 };
        double[] y1= new double[] { 4,6,7,3,1 };
        double[] interpx= new double [] { 1.0, 1.5, 4.5, 5.0, 1.5 };
        double[] interpy= interpolate( y1, findex( x1, interpx ) );
        for ( int i=0; i<interpx.length; i++ ) {
            System.out.println( ""+interpx[i]+ " " + interpy[i] );
        }
    }
    
    /**
     * just like modulo (%) function, but negative numbers return positive phase. 
     */
    public static double modp(double x, double t) {
        double result= x % t;
        return result >= 0 ? result : t + result;
    }

    /**
     * just like modulo (%) function, but negative numbers return positive phase. 
     */
    public static int modp( int x, int t) {
        int result= x % t;
        return result >= 0 ? result : t + result;
    }
    
    public static double biggerOf(double x1, double x2) {
        return ( x1>x2 ) ? x1 : x2;
    }
    
    private static double gcd( double a, double d, double error ) {
        
        if ( error>0 ) {
            a= Math.round( a/error );
            d= Math.round( d/error );
        }
        
        if ( a<d ) {
            double t= a;
            a= d;
            d= t;
        }
        
        if ( d==0 ) {
            if ( error>0 ) {
                return a * error;
            } else {
                return a;
            }
        }
        
        double r= a % d;                
        
        int iterations=0;
        
        while ( r > 0 && iterations<15 ) {
            d= r;
            r= a % d;
            iterations++;
        }
        
        if ( error>0 ) {
            return d * error;
        } else {
            return d;
        }
    }
    
    
   /*
    * Returns the greatest common divisor of a group of numbers.  This is useful for
    * a number of visualization techniques, for instance when you need to integerize
    * your data, the binsize should be the gcd.  An error parameter is provided to
    * avoid numerical noise, and in case there is a granularity that needn't be 
    * surpassed.
    *
    * org.das2.datum.DatumUtil has a private copy of this code.
    */
    public static double gcd( double[] A, double error ) {
        double guess= A[0];
        
        double result= guess;
        
        for ( int i=1; i<A.length; i++ ) {
            result= gcd( result, A[i], error );
        }
        
        return result;
    }
    
    /**
     * return the mean of the list. The first element's magnitude is
     * removed from each accumulated value so that items with large offset 
     * values (for example times) can be averaged.
     * @param A
     * @return 
     */
    public static double mean( double[] A ) {        
        double avgGuess= A[0];
        double sum=0.;
        
        for ( int i=0; i<A.length; i++ ) {
            sum+= A[i] - avgGuess;
        }
        
        return sum / A.length + avgGuess;        
    }
    
    /**
     * return the median of the list length N, which is the value at index N/2 of 
     * the sorted list.  This does not return the average for even-length lists.
     * and there is no checking for NaNs or fill values.
     * @param A
     * @return the median of the list
     */
    public static double median( double[] A ) {
        double[] sorted= sort( A );
        return sorted[A.length/2];
    }
    
    /**
     * return the maximum of the list
     * @param A the list
     * @return the maximum of the list
     */
    public static double max( double[] A ) {
        double max= A[0];
        for ( int i=0; i<A.length; i++ ) {
            max= ( max > A[i] ? max : A[i] );
        }
        return max;
    }
    
    /**
     * return the minimum of the list
     * @param A the list
     * @return the minimum of the list
     */
    public static double min( double[] A ) {
        double min= A[0];
        for ( int i=0; i<A.length; i++ ) {
            min= ( min < A[i] ? min : A[i] );
        }
        return min;
    }

    public static double[] sort( double[] A ) {
        double[] copy= new double[A.length];
        for ( int i=0; i<A.length; i++ ) {
            copy[i]= A[i];
        }
        java.util.Arrays.sort( copy );
        return copy;
    }
        
}
