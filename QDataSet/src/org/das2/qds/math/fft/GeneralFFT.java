/*
 * GeneralFFT.java
 *
 * Created on November 29, 2004, 9:02 PM
 */

package org.das2.qds.math.fft;

import org.das2.qds.math.fft.jnt.ComplexDoubleFFT;
import org.das2.qds.math.fft.jnt.ComplexDoubleFFT_Mixed;
import org.das2.qds.math.fft.jnt.ComplexFloatFFT;
import org.das2.qds.math.fft.jnt.ComplexFloatFFT_Mixed;
import org.das2.qds.math.fft.jnt.RealDoubleFFT;
import org.das2.qds.math.fft.jnt.RealDoubleFFT_Even;


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
    RealDoubleFFT realDoubleFFT;
    boolean real;
    
    private static final double LOG_2 = Math.log(2);
    
    /**
     * Initialize the FFT object by constructing wave tables that can be repeatly used.
     */
    public GeneralFFT( int n, boolean doublePrecision, boolean real ) {
        this.n= n;
        this.doublePrecision= doublePrecision;
        if ( n<2 || n>Math.pow(2,100)) throw new IllegalArgumentException("n too big or too small, n="+n);
        //this.real= real;
        this.real= false;  // funny real,false...
        if ( doublePrecision ) {
            complexDoubleFFT= new ComplexDoubleFFT_Mixed(n);
        } else {
            complexFloatFFT= new ComplexFloatFFT_Mixed(n);
        }
        if ( this.real ) {
            if ( n%2 != 0 ) throw new IllegalArgumentException("n must be even");
            if ( doublePrecision ) {
                realDoubleFFT= new RealDoubleFFT_Even(n);
            } else {
                throw new UnsupportedOperationException("not implemented");
            }
        } else {
            if ( doublePrecision ) {
                complexDoubleFFT= new ComplexDoubleFFT_Mixed(n);
            } else {
                complexFloatFFT= new ComplexFloatFFT_Mixed(n);
            }
        }
    }
    
    /**
     * creates an FFT object that operates on a ComplexArray.Float of n elements.
     */
    public static GeneralFFT newFloatFFT( int n ) {
        return new GeneralFFT( n, false, false );
    }
    
    /**
     * creates an FFT object that operates on a ComplexArray.Double of n elements.
     */
    public static GeneralFFT newDoubleFFT( int n ) {
        return new GeneralFFT( n, true, false );
    }
    
    /**
     * perform the forward transform on the array in situ.
     */
    public void transform( ComplexArray.Double data ) {
        if ( !doublePrecision ) throw new IllegalArgumentException("expected float arrays, got doubles");
        double norm;
        if ( real ) {
            realDoubleFFT.transform( data );
            norm= realDoubleFFT.normalization();
        } else {
            complexDoubleFFT.transform( data );
            norm = complexDoubleFFT.normalization();
        }
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
    
    /**
     * return the number of points in the fft.
     * @return the number of points in the fft.
     */
    public int size() {
        return n;
    }
}
