
package org.das2.qstream.filter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qstream.FormatStreamHandler;
import org.das2.qstream.StreamException;
import org.das2.qstream.StreamTool;

/**
 * Allow filters to be used from the command line.  Currently this just uses the reduce filter.  
 * @author jbf
 */
public class PipeFilter {
    public static void doit( InputStream in, OutputStream out, Datum cadence ) {

        ReduceFilter pipe= new ReduceFilter();
        pipe.setCadence( cadence );

        ReadableByteChannel rin= java.nio.channels.Channels.newChannel( in );

        FormatStreamHandler fsh= new FormatStreamHandler();
        fsh.setOutputStream( out );
        
        pipe.sink= fsh;

        try {
            StreamTool.readStream( rin, pipe );
        } catch ( StreamException ex ) {
            // the exception has already been forwarded onto the output stream.
        }

    }
    
    public static void main( String[] args ) throws StreamException, MalformedURLException, IOException, ParseException {
        if ( args.length<1 || args[0].trim().length()==0 ) {
            System.err.println("java -jar autoplot.jar org.qstream.filter.PipeFilter <seconds> [urlin] [fileout]");
            System.exit(-1);
        }
        InputStream in= System.in;
        OutputStream out= System.out;

        if ( args.length>1 ) {
            if ( args[1].startsWith("/") ) {
                in= new FileInputStream(args[1]);
                System.err.println("reading "+args[1] );
            } else {
                URL urlin= new java.net.URL(args[1]);
                in= urlin.openStream();
                System.err.println("reading "+urlin );
            }
        }
        if ( args.length>2 ) {
            out= new FileOutputStream(args[2]);
            System.err.println("writing "+args[2] );
        }
        Datum cadence= Units.seconds.parse(args[0]);

        doit( in, out, cadence );
        
        //doit( new java.net.URL("file:///home/jbf/project/autoplot/vap/pw/bill/20130506/notreduced.okay.qds").openStream(), new java.io.FileOutputStream("/home/jbf/project/autoplot/vap/pw/bill/20130506/reduced.okay.reduced.qds"), cadence );
        //doit( new java.net.URL("file:///home/jbf/project/autoplot/vap/pw/bill/20130506/notreduced.notokay.qds").openStream(), new java.io.FileOutputStream("/home/jbf/project/autoplot/vap/pw/bill/20130506/reduced.notokay.reduced.qds"), cadence );
        
        if ( in!=System.in ) in.close();
        if ( out!=System.out ) out.close();

        //doit( new java.net.URL("file:///tmp/0B000800408DD710.20120302.qds").openStream(), new FileOutputStream("/tmp/0B000800408DD710.20120302.reduce.qds"), cadence );
        //doit( new java.net.URL("file:///tmp/po_h0_hyd_20000128.qds").openStream(), new FileOutputStream("/tmp/po_h0_hyd_20000128.reduce.qds"), cadence );
        //doit( new java.net.URL("file:///home/jbf/data.nobackup/qds/doesntReduce.ascii.qds").openStream(), new java.io.FileOutputStream("/home/jbf/data.nobackup/qds/doesntReduce.reduced.qds"), cadence );
    }


}
