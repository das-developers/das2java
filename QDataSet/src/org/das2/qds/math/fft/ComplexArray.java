/*
 * ComplexArray.java
 *
 * Created on November 29, 2004, 9:34 PM
 */

package org.das2.qds.math.fft;

/**
 * Interface for passing complex arrays to and from FFT routines.  The intent is
 * that the complex array can be backed by data in any format.  Each elements is
 * readable and writeable via get and set methods for the real and imaginary components.
 * @author Jeremy
 */
public class ComplexArray {
    /**
     * A complex array that is accessed by floats
     */
    public interface Float {
        /**
         *
         * @param i
         * @return the real component of the complex element i.
         */
        float getReal(int i);
        /**
         *
         * @param i
         * @return the imaginary component of the complex element i.
         */
        float getImag(int i);
        /**
         *
         * @param i the element index
         * @param value the real component, the imaginary is not changed.
         */
        void setReal(int i,float value);
        /**
         *
         * @param i the element index
         * @param value the imaginary component, the real is not changed.
         */
        void setImag(int i,float value);
        /**
         * returns the number of elements
         * @return the number of elements in the array
         */
        int length();
    }
    
    /**
     * ComplexArray that is accessed by doubles
     */
    public interface Double {
        /**
         *
         * @param i
         * @return the real component of the complex element i.
         */
        double getReal(int i);
        
        /**
         *
         * @param i
         * @return the imaginary component of the complex element i.
         */
        double getImag(int i);
        /**
         *
         * @param i the element index
         * @param value the real component, the imaginary is not changed.
         */
        void setReal(int i,double value);
        /**
         *
         * @param i the element index
         * @param value the imaginary component, the real is not changed.
         */
        void setImag(int i,double value);
        /**
         * @return the number of elements in the array
         */
        int length();
    }
    
    /**
     * Implements ComplexArray that is backed by two float arrays.
     */
    public static final class ComplexArrayDoubleDouble implements Double {
        final double[] real;
        final double[] imaginary;
        private ComplexArrayDoubleDouble( final double[] real, final double[] imaginary ) {
            this.real= real;
            this.imaginary= imaginary;
        }
        
        /**
         *
         * @param i
         * @return the imaginary component of the complex element i.
         */
        public double getImag(int i) {
            return imaginary[i];
        }
        
        /**
         *
         * @param i
         * @return the real component of the complex element i.
         */
        public double getReal(int i) {
            return real[i];
        }
        
        /**
         *
         * @param i the element index
         * @param value the imaginary component, the real is not changed.
         */
        public void setImag(int i, double value) {
            imaginary[i]= value;
        }
        
        /**
         *
         * @param i the element index
         * @param value the real component, the imaginary is not changed.
         */
        public void setReal(int i, double value) {
            real[i]= value;
        }
        
        /**
         * @return the number of elements in the array
         */
        public int length() {
            return real.length;
        }
        
    }
    
    /**
     * Implements ComplexArray that is backed by two float arrays.
     */
    public static final class ComplexArrayFloatFloat implements Float {
        final float[] real;
        final float[] imaginary;
        private ComplexArrayFloatFloat( final float[] real, final float[] imaginary ) {
            this.real= real;
            this.imaginary= imaginary;
        }
        public float getImag(int i) {
            return imaginary[i];
        }
        
        public float getReal(int i) {
            return real[i];
        }
        
        public void setImag(int i, float value) {
            imaginary[i]= value;
        }
        
        public void setReal(int i, float value) {
            real[i]= value;
        }
        public int length() {
            return real.length;
        }
        
    }
    
    /**
     * Creates a new ComplexArray from an array of real numbers.  The complex
     * components of each element in the resulting array is zero.
     */
    public static ComplexArray.Double newArray( double[] real ) {
        double[] imag= new double[real.length];
        return new ComplexArrayDoubleDouble( real, imag );
    }
    
    /**
     * Creates a new ComplexArray from a float array representing real numbers, but
     * copies the original array so that it is not modified.
     */
    public static ComplexArray.Double newArrayCopy( double[] real ) {
        double[] imag= new double[real.length];
        double[] realCopy= new double[real.length];
        for (int i=0; i<real.length;i++ ) { realCopy[i]= real[i]; }
        return new ComplexArrayDoubleDouble( realCopy, imag );
    }
    
    /**
     * Creates a new ComplexArray from an array representing real numbers and
     * an array representing the corresponding complex components.
     */
    public static ComplexArray.Double newArray( double[] real, double[] imag ) {
        return new ComplexArrayDoubleDouble( real, imag );
    }
    
    /**
     * Creates a new ComplexArray from an array of real numbers.  The complex
     * components of each element in the resulting array is zero.
     */
    public static ComplexArray.Float newArray( float[] real ) {
        float[] imag= new float[real.length];
        return new ComplexArrayFloatFloat( real, imag );
    }
    
    /**
     * Creates a new ComplexArray from an array representing real numbers and
     * an array representing the corresponding complex components.
     */
    public static ComplexArray.Float newArray( float[] real, float[] imag ) {
        return new ComplexArrayFloatFloat( real, imag );
    }
    
    /**
     * converts a ComplexArray into an array for debugging purposes.
     */
    public static String toString( ComplexArray.Float array ) {
        StringBuffer buf= new StringBuffer();
        for ( int i=0; i<array.length(); i++ ) {
            buf.append( "("+array.getReal(i)+", "+array.getImag(i)+") " );
        }
        return buf.toString();
    }
    
    /**
     * converts a ComplexArray into an array for debugging purposes.
     */
    public static String toString( ComplexArray.Double array ) {
        StringBuffer buf= new StringBuffer();
        for ( int i=0; i<array.length(); i++ ) {
            buf.append( "("+array.getReal(i)+", "+array.getImag(i)+") " );
        }
        return buf.toString();
    }
    
    /**
     * returns the real parts of each element in an array.
     */
    public static double[] realPart( ComplexArray.Double array ) {
        double[] result= new double[array.length()];
        for ( int i=0; i<array.length(); i++ ) {
            result[i]= array.getReal(i);
        }
        return result;
    }
    
    /**
     * returns the magnitudes of each element in an array
     */
    public static double[] magnitude( ComplexArray.Double array ) {
        double[] result= new double[array.length()];
        for ( int i=0; i<array.length(); i++ ) {
            result[i]= magnitude( array, i );
        }
        return result;
    }
    
    /**
     * returns the magnitude of ah element in an array
     */
    final public static double magnitude2( ComplexArray.Double array, int i ) {
        return Math.pow( array.getReal(i),2 ) + Math.pow( array.getImag(i), 2 );
    }
    
    /**
     * returns the magnitude of ah element in an array
     */
    final public static double magnitude( ComplexArray.Double array, int i ) {
        return Math.sqrt( Math.pow( array.getReal(i),2 ) + Math.pow( array.getImag(i), 2 ) );
    }
    
}
