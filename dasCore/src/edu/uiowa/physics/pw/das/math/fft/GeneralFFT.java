/*
 * GeneralFFT.java
 *
 * Created on November 29, 2004, 9:02 PM
 */

package edu.uiowa.physics.pw.das.math.fft;

import edu.uiowa.physics.pw.das.math.fft.jnt.*;

/**
 * Provides forward and reverse FFT methods for any number of elements.  The FFTs
 * are implemented using a modified version of the jnt.FFT package from NIST.  This
 * version operates on ComplexArray objects that may be backed by das2 data sets
 * avoiding unnecessary copies.
 * @author Jeremy
 */
public class GeneralFFT {
    boolean doublePrecision;
    int n;
    ComplexDoubleFFT complexDoubleFFT;
    ComplexFloatFFT complexFloatFFT;
    private static final double LOG_2 = Math.log(2);
    

    
    /**
     * Initialize the FFT object by constructing wave tables that can be repeatly used.
     */
    public GeneralFFT( int n, boolean doublePrecision ) {
        this.n= n;
        this.doublePrecision= doublePrecision;
        if ( n<2 || n>Math.pow(2,100)) throw new IllegalArgumentException("n too big or too small, n="+n);
        if ( doublePrecision ) {
            complexDoubleFFT= new ComplexDoubleFFT_Mixed(n);
        } else {
            complexFloatFFT= new ComplexFloatFFT_Mixed(n);
        }
    }
    
    /**
     * creates an FFT object that operates on a ComplexArray.Float of n elements.
     */    
    public static GeneralFFT newFloatFFT( int n ) {
        return new GeneralFFT( n, false );
    }
    
     /**
     * creates an FFT object that operates on a ComplexArray.Double of n elements.
     */    
    public static GeneralFFT newDoubleFFT( int n ) {
        return new GeneralFFT( n, true );
    }
    
    /**
     * perform the forward transform on the array in situ.
     */    
    public void transform( ComplexArray.Double data ) {
        if ( !doublePrecision ) throw new IllegalArgumentException("expected float arrays, got doubles");
        complexDoubleFFT.transform( data );
        double norm = complexDoubleFFT.normalization();
        for (int i = 0; i < n; i++) {
            data.setReal( i, data.getReal(i) * norm );
            data.setImag( i, data.getImag(i) * norm );
        }
    }
    
    /**
     * perform the forward transform on the array in situ.
     */    
    public void transform( ComplexArray.Float data ) {
        if ( doublePrecision ) throw new IllegalArgumentException("expected double arrays, got floats");
        complexFloatFFT.transform( data );
        float norm = complexFloatFFT.normalization();
        for (int i = 0; i < n; i++) {
            data.setReal( i, data.getReal(i) * norm );
            data.setImag( i, data.getImag(i) * norm );
        }
    }
    
    /**
     * perform the inverse transform on the array in situ.
     */    
    public void invTransform( ComplexArray.Double data ) {
        if ( !doublePrecision ) throw new IllegalArgumentException("expected float arrays, got doubles");
        complexDoubleFFT.inverse( data );
    }
    
    /**
     * perform the inverse transform on the array in situ.
     */    
    public void invTransform(  ComplexArray.Float data ) {
        if ( doublePrecision ) throw new IllegalArgumentException("expected double arrays, got floats");
        complexFloatFFT.inverse( data );
    }
    
}
