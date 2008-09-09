/*
 * FFTUtil.java
 *
 * Created on December 1, 2004, 9:11 PM
 */

package org.das2.math.fft;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  Jeremy
 */
public class FFTUtil {
    
    public static VectorDataSet fftPower( GeneralFFT fft, final VectorDataSet vds ) {
        return fftPower( fft, vds, getWindowUnity(vds.getXLength()) );
    }
    
    public static VectorDataSet getWindowUnity( final int size ) {
        VectorDataSet unity= new QuickVectorDataSet() {
            public double getDouble(int i, Units units) {
                return 1.0;
            }
            public int getXLength() {
                return size;
            }
            public double getXTagDouble(int i, Units units) {
                return i;
            }
            public Units getXUnits() {
                return Units.dimensionless;
            }
            public Units getYUnits() {
                return Units.dimensionless;
            }
        };
        return unity;
    }
    
    public static VectorDataSet getWindow10PercentEdgeCosine( final int size ) {
        final int n= size;
        int maxlim= 410;
        int lim= Math.min( n/10, maxlim );
        final double[] ww= new double[n];
        
        double step = Math.PI / lim;
        for ( int i=0; i<lim; i++ ) {
            ww[i]= (1. - Math.cos ( step * (i) ) ) / 2.;
            ww[n-i-1]= ww[i];
        }
        for ( int i=lim; i<size-lim; i++ ) {
            ww[i]= 1.0;
        }
        VectorDataSet result= new QuickVectorDataSet() {
            public double getDouble(int i, Units units) {
                return ww[i];
            }
            public int getXLength() {
                return n;
            }
            public double getXTagDouble(int i, Units units) {
                return i;
            }
            public Units getXUnits() {
                return Units.dimensionless;
            }
            public Units getYUnits() {
                return Units.dimensionless;
            }
        };
        return result;
    }
        
    /**
     * Produces the power spectrum of the dataset.  This is the length of the fourier
     * components squared, normalized by the bandwidth.  The result dataset has dimensionless yunits.
     * @param vds VectorDataSet with x units TimeLocationUnits.
     */
    public static VectorDataSet fftPower( GeneralFFT fft, VectorDataSet vds, VectorDataSet weights ) {
        vds= new ClippedVectorDataSet( vds, 0, fft.n );
        double [] yreal= new double[ fft.n ];
        Units yUnits= vds.getYUnits();
        double [] yimag= new double[ fft.n ];
        for ( int i=0; i<fft.n; i++ ) yreal[i]= vds.getDouble( i, yUnits ) * weights.getDouble( i, Units.dimensionless );
        ComplexArray.Double ca= ComplexArray.newArray(yreal);
        fft.transform( ca );
        DatumVector xtags= getFrequencyDomainTags( DataSetUtil.getXTags(vds) );
        Units xUnits= xtags.getUnits();
        
        double binsize=  2 * ( xtags.get( xtags.getLength()/2 ).doubleValue(Units.hertz) ) / fft.n;
        
        VectorDataSetBuilder builder= new VectorDataSetBuilder( xtags.getUnits(), Units.dimensionless );
        
        for ( int i=0; i<xtags.getLength()/2; i++ ) {
            builder.insertY(xtags.get(i).doubleValue(xUnits), (i==0?1:4)*ComplexArray.magnitude2(ca,i) / binsize );
        }
        return builder.toVectorDataSet();
    }
    
    public static ComplexArray.Double fft( GeneralFFT fft, VectorDataSet vds, Units units ) {
        double [] yreal= new double[ vds.getXLength() ];
        Units yUnits= units;
        double [] yimag= new double[ vds.getXLength() ];
        for ( int i=0; i<vds.getXLength(); i++ ) yreal[i]= vds.getDouble( i, yUnits );
        ComplexArray.Double ca= ComplexArray.newArray(yreal);
        fft.transform( ca );
        return ca;
    }
    
    /**
     * @return the frequencies of the bins 
     * @param fs the sampling frequency
     */
    public static double[] getFrequencyDomainTags( double fs, int size ) {
        double[] result= new double[size];
        
        int n= size;
        int n21= n/2+1;
        for ( int i=0; i<n21; i++ ) {
            result[i]= fs/n * i ;
        }
        for ( int i=0; i<n21-2; i++ ) {
            result[i+n21]= fs/n * (n21-n+i);
        }
        return result;
    }
        
    static DatumVector getFrequencyDomainTags( DatumVector timeDomainTags ) {
        Units timeUnit= timeDomainTags.getUnits();
        double[] x= timeDomainTags.toDoubleArray(timeUnit);
        double[] result= new double[x.length];
        result[0]= 0.;
        double T= x[1]-x[0];
        int n= x.length;
        int n21= n/2+1;
        for ( int i=0; i<n21; i++ ) {
            result[i]= i / ( n*T );
        }
        for ( int i=0; i<n21-2; i++ ) {
            result[i+n21]= (n21-n+i) / ( n*T );
        }
        
        Units frequencyUnit= UnitsUtil.getInverseUnit( timeUnit.getOffsetUnits() );
        return DatumVector.newDatumVector( result, frequencyUnit );
    }
}
