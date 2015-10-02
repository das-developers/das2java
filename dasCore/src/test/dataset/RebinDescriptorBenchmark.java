/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test.dataset;

import org.das2.dataset.RebinDescriptor;
import org.das2.datum.Units;

/**
 * Benchmarks for the UnitsConverter class as used in the SpectrogramRenderer.
 * See sftp://jfaden.net/home/jbf/ct/autoplot/performance/20151002/
 * 
 * This showed a factor of two increase when a units conversion is needed.
 * 
 * @author jbf
 */
public class RebinDescriptorBenchmark {
    public static void main( String[] args ) {
        RebinDescriptor ddx= new RebinDescriptor( 0, 86400e6, Units.us2000, 1000, false );
        
        long t0;
        int n1= 2000000;
        int n2= 10;
        
        for ( int j=0; j<n2; j++ ) {
            t0= System.currentTimeMillis();
            for ( int i=0; i<n1; i++ ) {
                ddx.whichBin( i*86400e6/n1, Units.us2000 );
            }
            System.err.println("No Units Conversion: "+ ( System.currentTimeMillis()-t0 ) );

            t0= System.currentTimeMillis();
            for ( int i=0; i<n1; i++ ) {
                ddx.whichBin( i*86400e9/n1, Units.cdfTT2000 );
            }
            System.err.println("CDFTT2000 Units Conversion: "+ ( System.currentTimeMillis()-t0 ) );
        }
    }
}
