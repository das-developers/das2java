/* File: DasMath.java
 * Copyright (C) 2002-2003 University of Iowa
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
     
    public static void main(String[] args) {
        double x= 1e-18;
        edu.uiowa.physics.pw.das.util.DasDie.println("x:"+x);
        edu.uiowa.physics.pw.das.util.DasDie.println("roundNDigits:"+roundNDigits(x,3));
    }	
    
    public static double modp(double x, double t) {
        double result= x % t;
        return result >= 0 ? result : t + result;
    }
    
    public static double biggerOf(double x1, double x2) {
        return ( x1>x2 ) ? x1 : x2;
    }
    
}
