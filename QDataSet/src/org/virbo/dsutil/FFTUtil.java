/*
 * FFTUtil.java
 *
 * Created on December 1, 2004, 9:11 PM
 */

package org.virbo.dsutil;

import org.das2.datum.Units;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.IndexGenDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import org.virbo.math.fft.ComplexArray;
import org.virbo.math.fft.GeneralFFT;

/**
 *
 * @author  Jeremy
 */
public class FFTUtil {
    
    public static QDataSet fftPower( GeneralFFT fft, final QDataSet vds ) {
        return fftPower( fft, vds, getWindowUnity(vds.length()) );
    }

    /**
     * returns a rank 2 dataset from the rank 1 dataset, where the
     * fft would be run on each of the datasets.
     * @param ds rank 1 dataset of length N
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

    public static QDataSet getWindowHanning( final int size ) {
        final int n= size;
        final double[] ww= new double[n];

        int halfsize= size/2;
        for ( int k=0; k<size; k++ ) {
            ww[k] = 1.0 + Math.cos( 2.0*Math.PI*(double)(k-halfsize) /(double)size );
        }
        return DDataSet.wrap(ww);
    }
        
    /**
     * Produces the power spectrum of the dataset.  This is the length of the fourier
     * components squared, normalized by the bandwidth.  The result dataset has dimensionless yunits.
     * It's assumed that all the data is valid.
     * Note when the input is in mV/m, the result will be in (V/m)^2/Hz.
     * @param vds QDataSet rank 1 dataset with depend 0 units TimeLocationUnits.
     */
    public static QDataSet fftPower( GeneralFFT fft, QDataSet vds, QDataSet weights ) {
        double [] yreal= new double[ fft.size() ];
        
        for ( int i=0; i<fft.size(); i++ ) yreal[i]= vds.value( i ) * weights.value( i );

        ComplexArray.Double ca= ComplexArray.newArray(yreal);
        fft.transform( ca );  //TODO: get rid of ComplexArray, which can be represented as QDataSet.

        QDataSet dep0= (QDataSet) vds.property( QDataSet.DEPEND_0 );
        if ( dep0==null ) {
            dep0= new IndexGenDataSet( vds.length() );
        }

        QDataSet xtags= getFrequencyDomainTags( dep0 );//TODO: use tags for power to reduce code

        Units xUnits= (Units)xtags.property( QDataSet.UNITS );
        double binsize;
        if ( xUnits.isConvertableTo(Units.hertz) ) {
            UnitsConverter uc= xUnits.getConverter(Units.hertz);
            binsize= 2 * ( uc.convert( xtags.value( xtags.length()/2 ) ) ) / fft.size();
        } else {
            binsize= 2 * ( xtags.value( xtags.length()/2 ) ) / fft.size();
        }

        DDataSet result= DDataSet.createRank1(xtags.length()/2-1);
        QDataSet powTags= getFrequencyDomainTagsForPower(dep0);
        for ( int i=1; i<xtags.length()/2; i++ ) {
            result.putValue(i-1,4*ComplexArray.magnitude2(ca,i) / binsize );
        }

        Units u= (Units) vds.property( QDataSet.UNITS );
        if ( u!=null && u.toString().equalsIgnoreCase("mV/m" ) ) { // kludge to support RPWS H7 H8 H9 files.
            for ( int i=0; i<result.length(); i++ ) {
                result.putValue( i, result.value(i) / 1e6 );
            }
            result.putProperty( QDataSet.UNITS, SemanticOps.lookupUnits("(V/m)^2/Hz") );
        }

        result.putProperty( QDataSet.DEPEND_0, powTags );
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
    private static TTagBufElement freqDomainTagsForPowerBuf= null;

    public static QDataSet getFrequencyDomainTagsForPower( QDataSet dep0 ) {
        Units xunits= SemanticOps.getUnits(dep0);
        if ( dep0.length()<2 ) {
             throw new IllegalArgumentException("dep0 must be two or more elements");
        }

        synchronized (FFTUtil.class) {
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
        synchronized (FFTUtil.class ) {
            freqDomainTagsForPowerBuf= new TTagBufElement();
            freqDomainTagsForPowerBuf.data= powTags;
            freqDomainTagsForPowerBuf.dt= ( dep0.value(1) - dep0.value(0) );
            freqDomainTagsForPowerBuf.ddt= freqDomainTagsForPowerBuf.dt / 1e7;
            freqDomainTagsForPowerBuf.n = dep0.length();
            freqDomainTagsForPowerBuf.units= xunits;
        }
        return powTags;
    }

    public static ComplexArray.Double fft( GeneralFFT fft, QDataSet vds ) {
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
        
    public static QDataSet getFrequencyDomainTags( QDataSet timeDomainTags ) {
        Units timeUnit= (Units) timeDomainTags.property( QDataSet.UNITS );
        if ( timeUnit==null ) timeUnit= Units.dimensionless;
        QDataSet x= timeDomainTags;
        double[] result= new double[x.length()];
        result[0]= 0.;
        int n= x.length();
        double T= ( x.value(n-1)-x.value(0) ) / (n-1);
        double Tcheck= x.value(1)-x.value(0);
        if ( Math.abs( ( T-Tcheck ) / ( T ) ) > 0.001 ) {
            System.err.println("WARNING: timetags do not appear to be uniform: "+x );
        }
        int n21= n/2+1;
        Units frequencyUnit= UnitsUtil.getInverseUnit( timeUnit.getOffsetUnits() );
        if ( T>0.5 ) {
            if ( frequencyUnit==Units.megaHertz ) {
                frequencyUnit= Units.kiloHertz;
                T= T/1000;
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
        MutablePropertyDataSet r= DDataSet.wrap(result);
        r.putProperty( QDataSet.UNITS, frequencyUnit );
        return r;
    }
}
