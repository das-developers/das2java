/*
 * FFTUtil.java
 *
 * Created on December 1, 2004, 9:11 PM
 */

package org.das2.qds.math.fft;

import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.AbstractDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.util.DataSetBuilder;

/**
 *
 * @author  Jeremy
 */
public class FFTUtil {
    
    public static QDataSet fftPower( GeneralFFT fft, final QDataSet vds ) {
        return fftPower( fft, vds, getWindowUnity( vds.length()) );
    }
    
    public static QDataSet getWindowUnity( final int size ) {
        QDataSet unity= new AbstractDataSet() {
            public int rank() {
                return 1;
            }
            @Override
            public double value(int i) {
                return 1.0;
            }
            @Override
            public int length() {
                return size;
            }
        };
        return unity;
    }
    
    public static QDataSet getWindow10PercentEdgeCosine( final int size ) {
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
        QDataSet result= new AbstractDataSet() {
            public int rank() {
                return 1;
            }
            @Override
            public double value(int i) {
                return  ww[i];
            }
            @Override
            public int length() {
                return size;
            }
        };
        return result;
    }
        
    /**
     * Produces the power spectrum of the dataset.  This is the length of the fourier
     * components squared, normalized by the bandwidth.  The result dataset has dimensionless yunits.
     * @param vds VectorDataSet with x units TimeLocationUnits.
     */
    public static QDataSet fftPower( GeneralFFT fft, QDataSet vds, QDataSet weights ) {
        if ( vds.length()>fft.n) vds= vds.trim(0,fft.n);
        double [] yreal= new double[ fft.n ];
        for ( int i=0; i<fft.n; i++ ) yreal[i]= vds.value( i ) * weights.value( i );
        ComplexArray.Double ca= ComplexArray.newArray(yreal);
        fft.transform( ca );

        QDataSet xvds= (QDataSet) vds.property(QDataSet.DEPEND_0);

        QDataSet xtags= getFrequencyDomainTags( xvds );
        
        double binsize=  2 * ( xtags.value( xtags.length()/2 ) ) / fft.n;
        
        DataSetBuilder builder= new DataSetBuilder( 2, xvds.length()/2, 2 );
        
        for ( int i=0; i<xtags.length()/2; i++ ) {
            builder.putValue( i, 0, xtags.value(i) );
            builder.putValue( i, (i==0?1:4)*ComplexArray.magnitude2(ca,i) / binsize );
            builder.nextRecord();
        }

        return builder.getDataSet();
    }
    
    public static ComplexArray.Double fft( GeneralFFT fft, QDataSet vds, Units units ) {
        double [] yreal= new double[ vds.length() ];
        for ( int i=0; i<vds.length(); i++ ) yreal[i]= vds.value( i );
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
        
    static QDataSet getFrequencyDomainTags( QDataSet timeDomainTags ) {
        Units timeUnit=null;
        if ( timeDomainTags==null ) {
            throw new NullPointerException("null timeDomainTags passed into getFrequencyDomainTags");
        }
        timeUnit= (Units) timeDomainTags.property( QDataSet.UNITS );
        if ( timeUnit==null ) timeUnit= Units.dimensionless;

        double[] result= new double[timeDomainTags.length()];
        result[0]= 0.;
        double T= timeDomainTags.value(1) - timeDomainTags.value(0);
        int n= result.length;
        int n21= n/2+1;
        for ( int i=0; i<n21; i++ ) {
            result[i]= i / ( n*T );
        }
        for ( int i=0; i<n21-2; i++ ) {
            result[i+n21]= (n21-n+i) / ( n*T );
        }
        
        Units frequencyUnit= UnitsUtil.getInverseUnit( timeUnit.getOffsetUnits() );
        DDataSet dresult= DDataSet.wrap(result);
        dresult.putProperty(QDataSet.UNITS, frequencyUnit );
        return dresult;
    }
}
