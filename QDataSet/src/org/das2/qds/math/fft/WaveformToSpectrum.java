/*
 * WaveformToSpectrum.java
 *
 * Created on March 25, 2004, 9:09 PM
 * Translated to QDataSet Oct 11, 2010.
 */

package org.das2.qds.math.fft;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.UnitsUtil;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.util.DataSetBuilder;

/**
 *
 * @author  jbf
 */
public class WaveformToSpectrum {
    
    static class UnitsInverter {
        static Units getInverseUnit( Units unit ) {
            if ( unit==Units.seconds ) {
                return Units.hertz;
            } else if ( unit==Units.dimensionless ) {
                return Units.dimensionless;
            } else {
                throw new IllegalArgumentException( "units not supported: "+unit );
            }
        }
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
        
        Units frequencyUnit;
        double unitMult= 1.0;
        if ( timeUnit.isConvertibleTo(Units.seconds ) ) {
            unitMult= timeUnit.getConverter(Units.seconds).convert(1.0);
            frequencyUnit= Units.hertz;
            for ( int i=0; i< result.length; i++ ) {
                result[i]= result[i]/unitMult;
            }
        } else {
            frequencyUnit= UnitsInverter.getInverseUnit( timeUnit );
        }
        return DatumVector.newDatumVector( result, frequencyUnit );
    }
    
    private static final double LOG_2 = Math.log(2);
    
    private static boolean checkXTagsGrid( QDataSet ds, int st, int en ) {
        if ( ds.length()<1 ) {
            return false;
        } else {
            double base= ds.value( st );
            double delta= ( ds.value( en-1 ) - base ) / ( en-st-1 );
            for ( int i=st; i<en; i++ ) {
                double rr= ( ( ds.value(i) - base ) / delta ) % 1.;
                if ( rr > 0.01 && rr < 0.09 ) {
//                    try {
//                        PrintWriter fw = new PrintWriter("/tmp/foo.dat");
//                        for ( int ii=st; ii<en; ii++ ) {
//                            fw.printf( "%6d %16.12f %16.12qf\n", ii, ds.value(ii), ds.value(ii)-base );
//                        }
//                        fw.close();
//                    } catch (IOException ex) {
//                        Logger.getLogger(WaveformToSpectrum.class.getName()).log(Level.SEVERE, null, ex);
//                    }

                    return false;
                }
            }
            return true;
        }
    }
    
    public static double[][] fft(  double[][] array ) {
        double  u_r,u_i, w_r,w_i, t_r,t_i;
        int     ln, nv2, k, l, le, le1, j, ip, i, n;
        
        n = array[0].length;
        ln = (int)( Math.log( (double)n )/LOG_2 + 0.5 );
        if ( !(Math.pow(2,ln)==n) ) {
            throw new IllegalArgumentException( "input array (["+array.length+"]["+n+"]) is not [2][2^k]" );
        }
        nv2 = n / 2;
        j = 1;
        for (i = 1; i < n; i++ ) {
            if (i < j) {
                t_r = array[0][i - 1];
                t_i = array[1][i - 1];
                array[0][i - 1] = array[0][j - 1];
                array[1][i - 1] = array[1][j - 1];
                array[0][j - 1] = t_r;
                array[1][j - 1] = t_i;
            }
            k = nv2;
            while (k < j) {
                j = j - k;
                k = k / 2;
            }
            j = j + k;
        }
        
        for (l = 1; l <= ln; l++) {/* loops thru stages */
            le = (int)(Math.exp( (double)l * LOG_2 ) + 0.5 );
            le1 = le / 2;
            u_r = 1.0;
            u_i = 0.0;
            w_r =  Math.cos( Math.PI / (double)le1 );
            w_i = -Math.sin( Math.PI / (double)le1 );
            for (j = 1; j <= le1; j++) {/* loops thru 1/2 twiddle values per stage */
                for (i = j; i <= n; i += le) {/* loops thru points per 1/2 twiddle */
                    ip = i + le1;
                    t_r = array[0][ip - 1] * u_r - u_i * array[1][ip - 1];
                    t_i = array[1][ip - 1] * u_r + u_i * array[0][ip - 1];
                    
                    array[0][ip - 1] = array[0][i - 1] - t_r;
                    array[1][ip - 1] = array[1][i - 1] - t_i;
                    
                    array[0][i - 1] =  array[0][i - 1] + t_r;
                    array[1][i - 1] =  array[1][i - 1] + t_i;
                }
                t_r = u_r * w_r - w_i * u_i;
                u_i = w_r * u_i + w_i * u_r;
                u_r = t_r;
            }
        }
        return array;
    }
    
    public static QDataSet getTableDataSet2( QDataSet vds, int windowSize ) {
        GeneralFFT fft= GeneralFFT.newDoubleFFT( windowSize );

        QDataSet xvds= (QDataSet) vds.property(QDataSet.DEPEND_0);

        if ( !checkXTagsGrid(xvds,0,xvds.length()) ) {
            throw new IllegalArgumentException( "xtags don't appear to be gridded" );
        }
        
        Units xUnits= SemanticOps.getUnits(xvds);
        
        double[] yt= FFTUtil.getFrequencyDomainTags( 1 / ( xvds.value(1) - xvds.value(0) ), windowSize/2 );
        DatumVector yTags= DatumVector.newDatumVector(yt,UnitsUtil.getInverseUnit(xUnits.getOffsetUnits()));
        
        DataSetBuilder tdsb= new DataSetBuilder( 2, 100, yTags.getLength() );
        DataSetBuilder xdsb= new DataSetBuilder( 1, 100 );
        xdsb.putProperty( QDataSet.UNITS, xUnits );
        
        int nTableXTags= xvds.length() / windowSize;
        
        for ( int i=0; i<nTableXTags; i++ ) {
            QDataSet ds= FFTUtil.fftPower( fft, vds.trim( i*windowSize, (i+1)*windowSize ) );
            for ( int k=0; k<ds.length(); k++ ) {
                tdsb.putValue( -1, k, ds.value(k) );
            }
            xdsb.putValue( -1, xvds.value( (int)((i+0.5)*windowSize) ) );
            tdsb.nextRecord();
            xdsb.nextRecord();
        }

        DDataSet ytagds= DDataSet.createRank1(yTags.getLength());
        for ( int i=0; i<ytagds.length(); i++ ) ytagds.putValue( i, yTags.doubleValue( i, yTags.getUnits() ) );
        ytagds.putProperty( QDataSet.UNITS, yTags.getUnits() );

        tdsb.putProperty( QDataSet.DEPEND_1, ytagds );
        tdsb.putProperty( QDataSet.DEPEND_0, xdsb.getDataSet() );
        
        return tdsb.getDataSet();
    }
    
    public static QDataSet getTableDataSet( QDataSet vds, int windowSize ) {

        if ( vds.rank()!=1 ) {
            throw new IllegalArgumentException("input dataset should be rank 1");
        }

        int n21= windowSize/2+1;
        DatumVector yTags;
        
        Units xUnits;
        Units xOffsetUnits;

        QDataSet xvds= SemanticOps.xtagsDataSet( vds );

        xUnits= SemanticOps.getUnits(xvds);
        xOffsetUnits= xUnits.getOffsetUnits();
        Units timeDomainUnits= xOffsetUnits;

        double[] ybuf= new double[windowSize];
        double base= xvds.value( 0 );
        for ( int i=0; i<windowSize; i++ ) {
            ybuf[i]= xOffsetUnits.convertDoubleTo( timeDomainUnits, xvds.value( i ) - base );
        }
        //System.out.println(getFrequencyDomainTags(DatumVector.newDatumVector(buf,timeDomainUnits)));
        yTags= getFrequencyDomainTags(DatumVector.newDatumVector(ybuf,timeDomainUnits)).getSubVector(1,n21);
        
        Units zUnits= SemanticOps.getUnits(vds);
        DataSetBuilder tdsb= new DataSetBuilder( 2, 100, yTags.getLength() );
        QDataSet wvds= DataSetUtil.weightsDataSet(vds);
        DataSetBuilder xdsb= new DataSetBuilder( 1, 100 );
        xdsb.putProperty( QDataSet.UNITS, xUnits );

        double [][] buf= new double[2][windowSize];

        int ngood=0;
        int nTableXTags= vds.length() / windowSize;
        for ( int i=0; i<nTableXTags; i++ ) {
            boolean fill= false;
            if ( ! checkXTagsGrid( xvds,i*windowSize,(i+1)*windowSize ) ) {
                continue;
            }
            ngood++;
            for ( int j=0; j<windowSize; j++ ) {
                buf[0][j]= vds.value( i*windowSize+j );
                if ( wvds.value( i*windowSize+j )==0 ) fill= true;
                buf[1][j]= 0.;
            }

            double[] zBuf= new double[n21-1];
            if ( fill ) {
                for ( int j=1; j<n21; j++ ) {
                    zBuf[j-1]= zUnits.getFillDouble();
                }
            } else {
                fft(buf);
                for ( int j=1; j<n21; j++ ) {
                    zBuf[j-1]= Math.sqrt( buf[0][j]*buf[0][j] + buf[1][j]*buf[1][j]  );
                }
            }
            for ( int k=0; k<zBuf.length; k++ ) {
                tdsb.putValue( -1, k, zBuf[k] );
            }
            tdsb.nextRecord();
            xdsb.putValue( -1, xvds.value((int)((i+0.5)*windowSize)) );
            xdsb.nextRecord();
            //tdsb.insertYScan( vds.getXTagDatum((int)((i+0.5)*windowSize)), yTags, DatumVector.newDatumVector( zBuf, zUnits ) );
        }

        if ( ngood==0 ) {
            throw new IllegalArgumentException( "xtags don't appear to be gridded" );
        }

        DDataSet ytagds= DDataSet.createRank1(yTags.getLength());
        for ( int i=0; i<ytagds.length(); i++ ) ytagds.putValue( i, yTags.doubleValue( i, yTags.getUnits() ) );
        ytagds.putProperty( QDataSet.UNITS, yTags.getUnits() );

        tdsb.putProperty( QDataSet.DEPEND_0, xdsb.getDataSet() );
        tdsb.putProperty( QDataSet.DEPEND_1, ytagds );
        
        return tdsb.getDataSet();

    }
    
    private WaveformToSpectrum() {
        // utility class should not be instantiated
    }
    
}
