/*
 * FFTUtil.java
 *
 * Created on December 1, 2004, 9:11 PM
 */

package edu.uiowa.physics.pw.das.math.fft;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  Jeremy
 */
public class FFTUtil {
    
    public static VectorDataSet fftPower( GeneralFFT fft, VectorDataSet vds ) {        
        vds= new ClippedVectorDataSet( vds, 0, fft.n );
        double [] yreal= new double[ fft.n ];
        Units yUnits= vds.getYUnits();
        double [] yimag= new double[ fft.n ];
        for ( int i=0; i<fft.n; i++ ) yreal[i]= vds.getDouble( i, yUnits );
        ComplexArray.Double ca= ComplexArray.newArray(yreal);
        fft.transform( ca );        
        DatumVector xtags= getFrequencyDomainTags( DataSetUtil.getXTags(vds) );        
        Units xUnits= xtags.getUnits();
        VectorDataSetBuilder builder= new VectorDataSetBuilder( xtags.getUnits(), yUnits );
        for ( int i=0; i<xtags.getLength()/2; i++ ) {
            builder.insertY(xtags.get(i).doubleValue(xUnits), (i==0?1:2)*ComplexArray.magnitude(ca,i) );
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
