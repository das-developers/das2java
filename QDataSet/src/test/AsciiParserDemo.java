/*
 * AsciiParserDemo.java
 *
 * Created on December 5, 2007, 10:34 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test;

import java.io.FileInputStream;
import org.das2.datum.Units;
import java.net.MalformedURLException;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.datum.TimeParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.util.AsciiParser;

/**
 * Demonstration of the ascii parser.
 * @author jbf
 */
public class AsciiParserDemo {
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception sometimes when testing
     */
    public static void main(String[] args) throws Exception {

        testRich();

        //test1();
        
        System.err.println( ",hello,".split(",").length );
        System.err.println( " hello ".split("\\s+").length );
        
        System.err.println( TimeParser.iso8601String("2008-09-04T00:00:22") );
        System.err.println( TimeParser.iso8601String("2008-004 00:00") );
        System.err.println( TimeParser.iso8601String("2008-09-04T00:00:22.000000") );
        System.err.println( TimeParser.iso8601String("19951231T235959") );
    }

    private static void test1() throws IOException, MalformedURLException {
        AsciiParser parser = new AsciiParser();
        URL testFile = new URL("/tmp/omni2_1965.dat");
        parser.setDelimParser(new InputStreamReader(testFile.openStream()), "\\s+");

        WritableDataSet ds = parser.readStream(new InputStreamReader(testFile.openStream()), new NullProgressMonitor());

        final TimeParser p = TimeParser.create("%Y %j %H");
        final Units u = Units.t2000;
        // replace the first column with the datum time
        for (int i = 0; i < ds.length(); i++) {
            p.setDigit(0, (int) ds.value( i,0 ));
            p.setDigit(1, (int) ds.value( i,1 ));
            p.setDigit(2, (int) ds.value( i,2 ));
            ds.putValue(i, 0, p.getTime(Units.t2000));
        }

        System.err.println(ds);
    }
    
    private static void testRich() throws Exception {
        String sfile= "/home/jbf/ct/hudson/data.backup/dat/headers/CRRES_mod.txt";
        AsciiParser parser = new AsciiParser();
        parser.setKeepFileHeader(true);
        parser.setDelimParser(new InputStreamReader( new FileInputStream( sfile ) ), "\\s+");
        WritableDataSet ds = parser.readStream(new InputStreamReader( new FileInputStream( sfile ) ), new NullProgressMonitor() );

        QDataSet rgeo= DataSetOps.unbundle( ds, "Rgeo" );
        System.err.println(rgeo);

        QDataSet ii= DataSetOps.unbundle( ds, "I" );
        System.err.println(ii);

    }

}
