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

package edu.uiowa.physics.pw.das.util;

/**
 *
 * @author  jbf
 */

public class DasMath {
    
    /** Creates a new instance of DasMath */
    public DasMath() {
    }
    
    public static final double log10=Math.log(10);
    
    public static double log10(double x) {
        return Math.log(x)/log10;
    }
    
    public static double exp10(double x) {
        double result= Math.pow(10,x);
        return result;
    }
    
    public static double exp10(int x) {
        double result= Math.pow(10,x);
        if (result<0.3) {  // round off numerical fuzz
            result= 1./Math.round(1/result);
        }
        return result;
    }
    
    public static double roundNDigits(double x,int n) {
        
        double exp= exp10((int)log10(x));
        double mant= x/exp;
        double tenToN= exp10(n-1);
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
            while ( index<datax.length && datax[index+1]<x[i] ) index++;
            while ( index>0 && datax[index]>x[i] ) index--;
            result[i]= index + ( x[i] - datax[index] ) / ( datax[index+1] - datax[index] );
        }
        return result;
    }
    
    public static void main(String[] args) {
        double x= 1e-18;
        edu.uiowa.physics.pw.das.util.DasDie.println("x:"+x);
        edu.uiowa.physics.pw.das.util.DasDie.println("roundNDigits:"+roundNDigits(x,3));
        
        double[] x1= new double[] { 1,2,3,4,5 };
        double[] y1= new double[] { 4,6,7,3,1 };
        double[] interpx= new double [] { 1.0, 1.5, 4.5, 5.0, 1.5 };
        double[] interpy= interpolate( y1, findex( x1, interpx ) );
        for ( int i=0; i<interpx.length; i++ ) {
            System.out.println( ""+interpx[i]+ " " + interpy[i] );
        }
    }
    
    public static double modp(double x, double t) {
        double result= x % t;
        return result >= 0 ? result : t + result;
    }
    
    public static double biggerOf(double x1, double x2) {
        return ( x1>x2 ) ? x1 : x2;
    }
    
}
