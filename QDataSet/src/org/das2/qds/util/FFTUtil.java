/*
 * FFTUtil.java
 *
 * Created on December 1, 2004, 9:11 PM
 */

package org.das2.qds.util;

import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.qds.AbstractDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.IndexGenDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.das2.qds.math.fft.ComplexArray;
import org.das2.qds.math.fft.GeneralFFT;

/**
 * Utilities for FFT operations, such as getting the frequencies for each bin
 * and fftPower.
 * @author  Jeremy
 */
public class FFTUtil {
    
    public static QDataSet fftPower( GeneralFFT fft, final QDataSet vds ) {
        return fftPower( fft, vds, getWindowUnity(vds.length()) );
    }

    /**
     * returns a rank 2 dataset from the rank 1 dataset, where the
     * FFT would be run on each of the datasets.
     * @param ds rank 1 dataset of length N
     * @param size size of each FFT.
     * @return rank 2 dataset[N/size,size]
     */
    public static QDataSet window( QDataSet ds, int size ) {
        JoinDataSet jds= new JoinDataSet(2);
        JoinDataSet dep1= new JoinDataSet(2);

        int idx=0;
        DDataSet ttags= DDataSet.createRank1( ds.length()/size );
        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( dep0==null ) {
            dep0= Ops.dindgen(ds.length());
        }
        ttags.putProperty( QDataSet.UNITS, SemanticOps.getUnits(dep0) );

        DDataSet offsets=null;
        boolean qube= true;
        while ( idx+size<ds.length() ) {
            DDataSet offsets1= DDataSet.createRank1(size);
            for ( int i=0; i<size; i++ ) {
                offsets1.putValue(i,dep0.value(idx+i)-dep0.value(idx));
                if ( offsets!=null && offsets.value(i)!=offsets1.value(i) ) {
                    qube= false;
                }
                offsets= offsets1;
            }
            offsets1.putProperty( QDataSet.UNITS, SemanticOps.getUnits(dep0).getOffsetUnits() );

            jds.join( DataSetOps.trim( ds, idx, size ) );
            dep1.join( offsets1 );
            ttags.putValue(idx/size,dep0.value(idx));
            idx+=size;
        }
        jds.putProperty(QDataSet.DEPEND_0, ttags );
        if ( qube ) {
            jds.putProperty(QDataSet.DEPEND_1, offsets );
        } else {
            jds.putProperty(QDataSet.DEPEND_1, dep1 );
        }

        return jds;
    }
    
    /**
     * Window that is all ones, also called a boxcar.
     * @param size the window size
     * @return window
     */
    public static QDataSet getWindowUnity( final int size ) {
        QDataSet unity= new AbstractDataSet() {
            @Override
            public int rank() {
                return 1;
            }

            @Override
            public int length() {
                return size;
            }

            @Override
            public double value(int i) {
                return 1.0;
            }

        };
        return unity;
    }
    
    /**
     * Window with ones in the middle, and then the last 10% taper with cos.
     * @param size the window size
     * @return window
     */
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
        return DDataSet.wrap(ww);
    }

    /**
     * return a "Hanning" (Hann) window of the given size.
     * @param size
     * @return 
     */
    public static QDataSet getWindowHanning( final int size ) {
        final int n= size;
        final double[] ww= new double[n];

        for ( int k=0; k<size; k++ ) {
            double eta= (double)(k) /(double)(size-1);
            ww[k] = 1.0 - Math.cos( 2 * Math.PI * eta );
        }
        return DDataSet.wrap(ww);
    }
        
    /**
     * Produces the power spectrum of the dataset.  This is the length of the Fourier
     * components squared, normalized by the bandwidth.  The result dataset has dimensionless yunits.
     * It's assumed that all the data is valid.
     * Note when the input is in mV/m, the result will be in (V/m)^2/Hz.
     * @param fft FFT engine
     * @param vds rank 1 dataset with depend 0 units TimeLocationUnits.
     * @param weights rank 1 datasets that is the window to apply to the data.
     * @return the rank 2 FFT
     */
    public static QDataSet fftPower( GeneralFFT fft, QDataSet vds, QDataSet weights ) {
        return fftPower( fft, vds, weights, null );
    }
    
    /**
     * Produces the power spectrum of the dataset.  This is the length of the Fourier
     * components squared, normalized by the bandwidth.  The result dataset has dimensionless yunits.
     * It's assumed that all the data is valid.
     * Note when the input is in mV/m, the result will be in (V/m)^2/Hz.
     * @param fft FFT engine
     * @param vds rank 1 dataset with depend 0 units TimeLocationUnits.
     * @param weights rank 1 datasets that is the window to apply to the data.
     * @param powxTags if non-null, then use these xtags instead of calculating them for each record.
     * @return the rank 2 FFT
     */
    public static QDataSet fftPower( GeneralFFT fft, QDataSet vds, QDataSet weights, QDataSet powxTags ) {
        double [] yreal= new double[ fft.size() ];
        
        for ( int i=0; i<fft.size(); i++ ) yreal[i]= vds.value( i ) * weights.value( i );

        ComplexArray.Double ca= ComplexArray.newArray(yreal);
        fft.transform( ca );  //TODO: get rid of ComplexArray, which can be represented as QDataSet.

        double binsize;
        if ( powxTags==null ) {
            QDataSet dep0= (QDataSet) vds.property( QDataSet.DEPEND_0 );
            if ( dep0==null ) {
                dep0= new IndexGenDataSet( vds.length() );
            }
            powxTags= getFrequencyDomainTagsForPower(dep0);
        } else {
            
        }
        
        Units xUnits= (Units)powxTags.property( QDataSet.UNITS );
        if ( xUnits!=null && xUnits.isConvertibleTo(Units.hertz) ) {
            UnitsConverter uc= xUnits.getConverter(Units.hertz);
            binsize= ( uc.convert( powxTags.value( 1 ) - powxTags.value(0) ) ) ;
        } else {
            binsize= powxTags.value(1) - powxTags.value(0) ;
        }
        
        DDataSet result= DDataSet.createRank1( powxTags.length() );
        
        int i1;
        for ( i1=0; i1<result.length(); i1++ ) {
            result.putValue(i1, 2 * ComplexArray.magnitude2(ca,i1+1) / binsize );
        }
        
        Units u= (Units) vds.property( QDataSet.UNITS );
        if ( u!=null && u.toString().equalsIgnoreCase("mV/m" ) ) { // kludge to support RPWS H7 H8 H9 files.
            for ( int i=0; i<result.length(); i++ ) {
                result.putValue( i, result.value(i) / 1e6 );
            }
            result.putProperty( QDataSet.UNITS, Units.lookupUnits("(V/m)^2/Hz") );
        }

        result.putProperty( QDataSet.DEPEND_0, powxTags );
    
        return result;
    }

    /**
     * Perform the fft to get real and imaginary components for intervals.  
     * @param fft FFT code to use, such as GeneralFFT.newDoubleFFT(len)
     * @param vds QDataSet rank 1 dataset with depend 0 units TimeLocationUnits.
     * @param weights QDataSet rank 1 dataset containing weights, as in hanning.  null indicates no weights.
     * @return the rank 2 FFT
     */
    public static QDataSet fft( GeneralFFT fft, QDataSet vds, QDataSet weights ) {
        double [] yreal= new double[ fft.size() ];
        
        if ( weights==null ) {
            for ( int i=0; i<fft.size(); i++ ) yreal[i]= vds.value( i );
        } else {
            for ( int i=0; i<fft.size(); i++ ) yreal[i]= vds.value( i ) * weights.value( i );
        }

        ComplexArray.Double ca= ComplexArray.newArray(yreal);
        fft.transform( ca );  //TODO: get rid of ComplexArray, which can be represented as QDataSet.

        QDataSet dep0= (QDataSet) vds.property( QDataSet.DEPEND_0 );
        if ( dep0==null ) {
            dep0= new IndexGenDataSet( vds.length() );
        }

        QDataSet xtags= getFrequencyDomainTags( dep0 );

        Units xUnits= (Units)xtags.property( QDataSet.UNITS );
        double binsize;
        if ( xUnits.isConvertibleTo(Units.hertz) ) {
            UnitsConverter uc= xUnits.getConverter(Units.hertz);
            binsize= uc.convert( xtags.value(1) - xtags.value(0) );
        } else {
            binsize= ( xtags.value(1) - xtags.value(0) );
        }

        DDataSet result= DDataSet.createRank2(xtags.length(),2);

        for ( int i=1; i<xtags.length(); i++ ) {
            result.putValue(i,0, ca.getReal(i) / binsize );
            result.putValue(i,1, ca.getImag(i) / binsize );
        }

        result.putProperty( QDataSet.DEPEND_0, xtags );
        return result;
    }
    
    /**
     * Perform the inverse fft to get real and imaginary components for intervals.  
     * @param fft FFT code to use, such as GeneralFFT.newDoubleFFT(len)
     * @param vds QDataSet rank 2 dataset with depend 0 units TimeLocationUnits and depend_1=['real','imaginary'].
     * @param weights QDataSet rank 1 dataset containing weights, as in hanning.  null indicates no weights.
     * @return the rank 2 FFT
     */
    public static QDataSet ifft( GeneralFFT fft, QDataSet vds, QDataSet weights ) {
        
        double [] yreal= new double[ fft.size() ];
        double [] yimag= new double[ fft.size() ];
        
        if ( weights==null ) {
            for ( int i=0; i<fft.size(); i++ ) {
                yreal[i]= vds.value( i, 0 );
                yimag[i]= vds.value( i, 1 );
            }
        } else {
            for ( int i=0; i<fft.size(); i++ ) {
                yreal[i]= vds.value( i, 0 ) * weights.value( i );
                yimag[i]= vds.value( i, 1 ) * weights.value( i );
            }
        }

        ComplexArray.Double ca= ComplexArray.newArray(yreal,yimag);
        fft.invTransform( ca );  

        QDataSet dep0= (QDataSet) vds.property( QDataSet.DEPEND_0 );
        if ( dep0==null ) {
            dep0= new IndexGenDataSet( vds.length() );
        }

        QDataSet xtags= getTimeDomainTags( dep0 );

        Units xUnits= (Units)xtags.property( QDataSet.UNITS );

        // this code was derived by experiment.  
        // TODO: find someone that understands this better and have them verify.
        double binsize;
        if ( xUnits.isConvertibleTo(Units.seconds) ) {
            UnitsConverter uc= xUnits.getConverter(Units.seconds);
            binsize= 1 / ( uc.convert( ( dep0.value(1) - dep0.value(0) ) ) );
        } else {
            binsize= 1 / ( dep0.value(1) - dep0.value(0) );
        }
        
        DDataSet result= DDataSet.createRank2(xtags.length(),2);

        for ( int i=1; i<xtags.length(); i++ ) {
            result.putValue(i,0, ca.getReal(i) / binsize );
            result.putValue(i,1, ca.getImag(i) / binsize );
        }

        result.putProperty( QDataSet.DEPEND_0, xtags );
        return result;
    }
    
    
    private static class TTagBufElement {
        QDataSet data;
        double dt;
        double ddt;
        int n;
        Units units;
    }

    /*
     * keep track of one result, since one spectrogram will have thousands of these.  This is mostly to conserve space, but
     * we should see some performance gain as well.
     */
    private static transient TTagBufElement freqDomainTagsForPowerBuf= null;

    /**
     * get the frequency tags, for use when calculating the power in each
     * channel.  This removes the DC channel, and folds over the negative 
     * frequencies.  This also keeps a cache for performance.
     * @param dep0 the timetags.
     * @return the frequency tags.
     */
    public static QDataSet getFrequencyDomainTagsForPower( QDataSet dep0 ) {
        Units xunits= SemanticOps.getUnits(dep0);
        if ( dep0.length()<2 ) {
             throw new IllegalArgumentException("dep0 must be two or more elements");
        }

        synchronized (FFTUtil.class) {  // findbugs okay
            if ( freqDomainTagsForPowerBuf!=null ) {
                if ( Math.abs(  freqDomainTagsForPowerBuf.dt - ( dep0.value(1) - dep0.value(0) ) ) < freqDomainTagsForPowerBuf.ddt
                    && freqDomainTagsForPowerBuf.n == dep0.length()
                    && freqDomainTagsForPowerBuf.units== xunits ) {
                    return freqDomainTagsForPowerBuf.data;
                }
            }
        }
        QDataSet xtags= getFrequencyDomainTags( dep0 );
        Units xUnits= (Units)xtags.property( QDataSet.UNITS );
        DDataSet powTags= DDataSet.createRank1(xtags.length()/2-1);
        for ( int i=1; i<xtags.length()/2; i++ ) {
            powTags.putValue(i-1,xtags.value(i));
        }
        powTags.putProperty( QDataSet.UNITS, xUnits );
        powTags.putProperty( QDataSet.CADENCE, xtags.property(QDataSet.CADENCE) );
        synchronized (FFTUtil.class ) {
            TTagBufElement buf= new TTagBufElement();
            buf.data= powTags;
            buf.dt= ( dep0.value(1) - dep0.value(0) );
            buf.ddt= buf.dt / 1e7;
            buf.n = dep0.length();
            buf.units= xunits;
            freqDomainTagsForPowerBuf= buf; // findbugs  LI_LAZY_INIT_UPDATE_STATIC
        }
        return powTags;
    }

    /**
     * helper method for doing FFT of a QDataSet.  This forward FFT 
     * returns normalized data.  GeneralFFT.newDoubleFFT(ds.length());
     * @param fft the FFT engine
     * @param vds rank 1 ds[n] or rank 2 ds[n,2]
     * @return 
     * @see Ops#fft(org.das2.qds.QDataSet) 
     */
    public static ComplexArray.Double fft( GeneralFFT fft, QDataSet vds ) {
        ComplexArray.Double ca;
        if ( vds.rank()==2 ) {
            double [] yreal= new double[ vds.length() ];
            double [] yimag= new double[ vds.length() ];
            for ( int i=0; i<vds.length(); i++ ) {
                yreal[i]= vds.value( i, 0 );
                yimag[i]= vds.value( i, 1 );
            }
            ca= ComplexArray.newArray(yreal,yimag);
        } else {
            double [] yreal= new double[ vds.length() ];
            for ( int i=0; i<vds.length(); i++ ) yreal[i]= vds.value( i );
            ca= ComplexArray.newArray(yreal);
        }
        fft.transform( ca );
        return ca;
    }

    /**
     * helper method for doing inverse FFT of a QDataSet. 
     * @param fft the FFT engine
     * @param vds rank 2 dataset[n;real,complex]
     * @return rank 2 dataset[n;real,complex]
     * @see Ops#ifft(org.das2.qds.QDataSet) 
     */
    public static ComplexArray.Double ifft( GeneralFFT fft, QDataSet vds ) {
        if ( vds.rank()!=2 ) throw new IllegalArgumentException("input must be rank 2: dataset[n;real,complex]");
        double [] yreal= new double[ vds.length() ];
        for ( int i=0; i<vds.length(); i++ ) yreal[i]= vds.value( i, 0 );
        double [] yimag= new double[ vds.length() ];
        for ( int i=0; i<vds.length(); i++ ) yimag[i]= vds.value( i, 1 );
        ComplexArray.Double ca= ComplexArray.newArray(yreal,yimag);
        fft.invTransform( ca );
        return ca;
    }
    
    /**
     * @return the frequencies of the bins 
     * @param fs the sampling frequency
     * @param size the size of the time domain data.
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
        
    /**
     * return the time domain tags for inverse fft.
     * @param frequencyDomainTags
     * @return the time Domain Tags
     */
    public static QDataSet getTimeDomainTags( QDataSet frequencyDomainTags ) {
        
        QDataSet nyquistFreq= frequencyDomainTags.slice(frequencyDomainTags.length()/2);
        
        Datum dt= Ops.datum( Ops.divide( 0.5,nyquistFreq ) );
        
        return Ops.taggen( 0, dt.value(), frequencyDomainTags.length(), dt.getUnits() );
    }
    
    /**
     * limit the number of times the warning "timetags do not appear to be uniform" is printed
     */
    private static int debugPrintCount=12;
    
    /**
     * return the frequency tags for the given time offset tags, so 
     * <code>f[i]=  i / n*T</code>
     * where <code>T= time[1] - time[0]</code> and <code>n= len(time)</code>.
     * for the first n/2 points and
     * <code>f[i]= (n21-n+i) / ( n*T )</code> for the second half.
     * When units have a known inverse, this is returned.
     * @param timeDomainTags
     * @return the frequency domain tags.
     */
    public static QDataSet getFrequencyDomainTags( QDataSet timeDomainTags ) {
        Units timeUnit= (Units) timeDomainTags.property( QDataSet.UNITS );
        if ( timeUnit==null ) timeUnit= Units.dimensionless;
        QDataSet x= timeDomainTags;
        double[] result= new double[x.length()];
        result[0]= 0.;
        int n= x.length();
        double T,Tcheck;
        if ( n>120 ) {
            T= ( x.value(n-1)-x.value(0) ) / (n-1);
            Tcheck= ( x.value(60)-x.value(0) ) / 60 ;
        } else {
            T= ( x.value(n-1)-x.value(0) ) / (n-1);
            Tcheck= x.value(1)-x.value(0);
        }
        if ( Math.abs( ( T-Tcheck ) / ( T ) ) > 0.001 ) {
            if ( debugPrintCount>0 ) {
                debugPrintCount--;
                System.err.println("WARNING: timetags do not appear to be uniform: "+x );
                System.err.println( String.format( "t[0]=%s t[1]=%s t[%d]=%s", 
                        Ops.subtract( timeDomainTags.slice(0), timeDomainTags.slice(0) ),
                        Ops.subtract( timeDomainTags.slice(1), timeDomainTags.slice(0) ),
                        timeDomainTags.length()-1,
                        Ops.subtract( timeDomainTags.slice(timeDomainTags.length()-1), timeDomainTags.slice(0) ) ) );
            }
        }
        int n21= n/2+1;
        Units frequencyUnit= UnitsUtil.getInverseUnit( timeUnit.getOffsetUnits() );
        if ( T>0.5 ) {
            if ( frequencyUnit==Units.megaHertz ) {
                if ( T>1000 ) {
                    frequencyUnit= Units.hertz;
                    T= T/1000000;                    
                } else {
                    frequencyUnit= Units.kiloHertz;
                    T= T/1000;
                }
            } else if ( frequencyUnit==Units.gigaHertz ) {
                if ( T>1000000 ) {
                    frequencyUnit= Units.hertz;
                    T= T/1000000000;    
                } else {
                    frequencyUnit= Units.kiloHertz;
                    T= T/1000000;
                }
            }
        }
        for ( int i=0; i<n21; i++ ) {
            result[i]= i / ( n*T );
        }
        for ( int i=0; i<n21-2; i++ ) {
            result[i+n21]= (n21-n+i) / ( n*T );
        }
        MutablePropertyDataSet r= DDataSet.wrap(result);  //TODO: we go through here too many times...
        r.putProperty( QDataSet.CADENCE, DataSetUtil.asDataSet( 1/(n*T),frequencyUnit) );
        r.putProperty( QDataSet.UNITS, frequencyUnit );
        return r;
    }
}
