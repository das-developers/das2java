/*
 * AsciiParserDemo.java
 *
 * Created on December 5, 2007, 10:34 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package test;

import edu.uiowa.physics.pw.das.datum.Units;
import java.net.MalformedURLException;
import org.das2.util.monitor.NullProgressMonitor;
import edu.uiowa.physics.pw.das.util.TimeParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import org.virbo.dataset.WritableDataSet;
import org.virbo.dsutil.AsciiParser;

/**
 *
 * @author jbf
 */
public class AsciiParserDemo {
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        
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
        URL testFile = new URL("file:///N:/data/examples/asciitable/omni2_1965.dat");
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
    
}
