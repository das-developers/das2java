/*
 * WaveformToSpectrum.java
 *
 * Created on March 25, 2004, 9:09 PM
 */

package org.das2.math.fft;

import org.das2.dataset.ClippedVectorDataSet;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.TableDataSetBuilder;
import org.das2.dataset.WeightsVectorDataSet;
import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.UnitsUtil;

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
        if ( timeUnit.isConvertableTo(Units.seconds ) ) {
            unitMult= timeUnit.getConverter(Units.seconds).convert(1.0);
            timeUnit= Units.seconds;
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
    
    private static boolean checkXTagsGrid( DataSet ds, int st, int en ) {
        if ( ds.getXLength()<1 ) {
            return false;
        } else {
            Units units= ds.getXUnits();
            double base= ds.getXTagDouble( st, units );
            double delta= ( ds.getXTagDouble( en-1, units ) - base ) / ( en-st-1 );
            for ( int i=st; i<en; i++ ) {
                double rr= ( ( ds.getXTagDouble(i, units) - base ) / delta ) % 1.;
                if ( rr > 0.01 && rr < 0.09 ) {
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
    
    public static TableDataSet getTableDataSet2( VectorDataSet vds, int windowSize ) {
        GeneralFFT fft= GeneralFFT.newDoubleFFT( windowSize );
        
        if ( !checkXTagsGrid(vds,0,vds.getXLength()) ) {
            throw new IllegalArgumentException( "xtags don't appear to be gridded" );
        }
        
        Units xUnits= vds.getXUnits();
        
        double[] yt= FFTUtil.getFrequencyDomainTags( 1 / ( vds.getXTagDouble(1,xUnits) - vds.getXTagDouble(0,xUnits) ), windowSize/2 );
        DatumVector yTags= DatumVector.newDatumVector(yt,UnitsUtil.getInverseUnit(xUnits.getOffsetUnits()));
        
        Units zUnits= vds.getYUnits();
        TableDataSetBuilder tdsb= new TableDataSetBuilder( vds.getXUnits(), yTags.getUnits(), zUnits );
        
        int nTableXTags= vds.getXLength() / windowSize;
        
        VectorDataSet window= FFTUtil.getWindow10PercentEdgeCosine(windowSize);
        double[] d= new double[windowSize/2];
        for ( int i=0; i<nTableXTags; i++ ) {
            VectorDataSet ds= FFTUtil.fftPower( fft, new ClippedVectorDataSet( vds, i*windowSize, windowSize ) );
//            tdsb.insertYScan( vds.getXTagDatum((int)((i+0.5)*windowSize)), yTags, DatumVector.newDatumVector( zBuf, zUnits ) );
            Units u= ds.getYUnits();
            for ( int j=0; j<d.length; j++ ) d[j]= ds.getDouble(j,u);
            tdsb.insertYScan( vds.getXTagDatum((int)((i+0.5)*windowSize)),
                    yTags,
                    DatumVector.newDatumVector(d,u) );
        }
        
        return tdsb.toTableDataSet();
    }
    
    public static TableDataSet getTableDataSet( VectorDataSet vds, int windowSize ) {
        
        int n21= windowSize/2+1;
        DatumVector yTags;
        {
            Units xUnits;
            Units xOffsetUnits;
            xUnits= vds.getXUnits();
            xOffsetUnits= xUnits.getOffsetUnits();
            Units timeDomainUnits= xOffsetUnits;
            
            double[] buf= new double[windowSize];
            double base= vds.getXTagDouble( 0, vds.getXUnits() ); /* getDouble used here accidentally--API need work? */
            for ( int i=0; i<windowSize; i++ ) {
                buf[i]= xOffsetUnits.convertDoubleTo( timeDomainUnits, vds.getXTagDouble( i, xUnits ) - base );
            }
            //System.out.println(getFrequencyDomainTags(DatumVector.newDatumVector(buf,timeDomainUnits)));
            yTags= getFrequencyDomainTags(DatumVector.newDatumVector(buf,timeDomainUnits)).getSubVector(1,n21);
        }
        
        Units zUnits= vds.getYUnits();
        TableDataSetBuilder tdsb= new TableDataSetBuilder( vds.getXUnits(), yTags.getUnits(), zUnits );
        VectorDataSet wvds= (VectorDataSet) DataSetUtil.getWeightsDataSet(vds);

        double [][] buf= new double[2][windowSize];

        int ngood=0;
        int nTableXTags= vds.getXLength() / windowSize;
        for ( int i=0; i<nTableXTags; i++ ) {
            boolean fill= false;
            if ( ! checkXTagsGrid( vds,i*windowSize,(i+1)*windowSize ) ) {
                continue;
            }
            ngood++;
            for ( int j=0; j<windowSize; j++ ) {
                buf[0][j]= vds.getDouble( i*windowSize+j,zUnits );
                if ( wvds.getDouble( i*windowSize+j,Units.dimensionless )==0 ) fill= true;
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
            tdsb.insertYScan( vds.getXTagDatum((int)((i+0.5)*windowSize)), yTags, DatumVector.newDatumVector( zBuf, zUnits ) );
        }

        if ( ngood==0 ) {
            throw new IllegalArgumentException( "xtags don't appear to be gridded" );
        }
        return tdsb.toTableDataSet();
    }
    
    private WaveformToSpectrum() {
        // utility class cannot be instanciated
    }
    
}
