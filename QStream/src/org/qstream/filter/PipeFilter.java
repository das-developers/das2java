/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.qstream.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.virbo.qstream.FormatStreamHandler;
import org.virbo.qstream.StreamException;
import org.virbo.qstream.StreamTool;

/**
 * Allow filters to be used from the command line.  Currently this just uses the reduce filter.  
 * @author jbf
 */
public class PipeFilter {
    public static void doit( InputStream in, OutputStream out, Datum cadence ) throws StreamException {

        ReduceFilter pipe= new ReduceFilter();
        pipe.setCadence( cadence );

        StreamTool stin= new StreamTool();

        ReadableByteChannel rin= java.nio.channels.Channels.newChannel( in );

        FormatStreamHandler fsh= new FormatStreamHandler();
        fsh.setOutputStream( out );
        
        pipe.sink= fsh;

        stin.readStream( rin, pipe );

    }
    
    public static void main( String[] args ) throws StreamException, MalformedURLException, IOException, ParseException {
        if ( args.length!=1 ) {
            System.err.println("java -jar autoplot.jar org.qstream.filter.PipeFilter <seconds>");
        }
        Datum cadence= Units.seconds.parse(args[0]);
        doit( System.in, System.out, cadence );
        //doit( new java.net.URL("file:///tmp/0B000800408DD710.20120302.qds").openStream(), new FileOutputStream("/tmp/0B000800408DD710.20120302.reduce.qds"), cadence );
        //doit( new java.net.URL("file:///tmp/po_h0_hyd_20000128.qds").openStream(), new FileOutputStream("/tmp/po_h0_hyd_20000128.reduce.qds"), cadence );
    }


}
