/*
 * SinWaveStream.java
 *
 * Created on January 5, 2004, 12:03 PM
 */

package edu.uiowa.physics.pw.das.stream.test;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.dataset.test.*;
import edu.uiowa.physics.pw.das.datum.*;

/**
 *
 * @author  Jeremy
 */

/* dumps out a sine wave */
public class SineWaveStream {          
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {        
        Datum start, end, resolution;
        
        //if ( args.length!=3 ) {
            start= TimeUtil.createValid( "1979-3-4" );
            end= TimeUtil.createValid( "1979-3-4T04:00" );
            resolution= Datum.create(1.,Units.seconds);
        //}   
            
        DataSetDescriptor dsd= new SineWaveDataSetDescriptor( Datum.create(1), Units.seconds.createDatum(120) );
        DataSet ds= dsd.getDataSet(start, end, resolution, null );                
        
        VectorUtil.dumpToAsciiStream((VectorDataSet)ds, System.out );
    }
    
}
