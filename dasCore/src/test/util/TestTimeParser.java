/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.util;

import java.text.ParseException;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.TimeUtil;
import org.das2.datum.TimeUtil;
import org.das2.util.TimeParser;

/**
 *
 * @author jbf
 */
public class TestTimeParser {

    private static boolean doTest( String format, String str, Datum ref ) throws ParseException {
        TimeParser tp= TimeParser.create(format);
        DatumRange t= tp.parse(str).getTimeRange();
        if ( !ref.equals(t.min()) ) {
          throw new IllegalArgumentException("test fails: "+str+" "+format);
        }
        try {
            System.err.println( "okay: "+ tp.format( t.min(), t.max() ) );
        } catch ( IllegalArgumentException ex ) {
            throw ex;
        }
        return true;
    }

    public static void doTest2() throws ParseException {
        TimeParser tp= TimeParser.create("file:///home/jbf/product_%Y-%j.png");
        System.err.println(tp.toString());
        tp.parse("file:///home/jbf/product_2009-230.png");
        System.err.println(tp.getTimeRange());
    }

    public static void main( String[] args ) throws ParseException {
//        doTest( "%y %b %d %j:%H:%M:%S", "98 JAN  1   1:00:05:32", TimeUtil.createValid("19980101T00:05:32Z") );
        doTest( "%-1{j;Y=2008}", "1", TimeUtil.createValid("20080101T00:00:00Z") );
        doTest2();
    }
}
