/*
 * FFTUtil.java
 *
 * Created on December 1, 2004, 9:11 PM
 */

package edu.uiowa.physics.pw.das.math.fft;

import edu.uiowa.physics.pw.apps.auralization.EZVectorDataSet;
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
        VectorDataSet unity= new EZVectorDataSet() {
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
        VectorDataSet result= new EZVectorDataSet() {
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
     * returns the power spectrum of the dataset.  This is the length of the fourier
     * components squared.  The result dataset has dimensionless yunits.
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
        VectorDataSetBuilder builder= new VectorDataSetBuilder( xtags.getUnits(), Units.dimensionless );
        for ( int i=0; i<xtags.getLength()/2; i++ ) {
            builder.insertY(xtags.get(i).doubleValue(xUnits), (i==0?1:2)*ComplexArray.magnitude2(ca,i) );
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
