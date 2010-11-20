/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.datum;
import java.text.ParseException;
import java.util.List;
import org.das2.datum.DatumRange;
import static org.das2.datum.DatumRangeUtil.*;

/**
 *
 * @author jbf
 */
public class TestDatumRangeUtil {
    public static void main( String[] args ) throws ParseException {
        //testIntersection();
        testParse8601();
    }

    public static void testIntersection( ) {
        List<DatumRange> list1= generateList( parseTimeRangeValid("2009"), parseTimeRangeValid("jan-2009" ) );
        List<DatumRange> list2= generateList( parseTimeRangeValid("2008-12-1 to 2010-3-1"), parseTimeRangeValid("jan-2009-10" ) );
        List<DatumRange> list3= generateList( parseTimeRangeValid("2009-6-1 to 2009-10-5"), parseTimeRangeValid("jan-2009-10" ) );
        list2.removeAll(list3);
        List<DatumRange> r= intersection( list1, list2, true );
        System.err.println("containers: "+r);
        System.err.println("not contained: "+list2);
    }

    public static void testParse8601_1( String test, String ref ) throws ParseException {
        DatumRange dr= parseISO8601Range(test);
        DatumRange drref= parseISO8601Range(ref);
        if ( drref.equals(dr) ) {
            System.err.println(test);
        } else {
            System.err.println( test + " != " + ref + ", " + dr + "!=" + drref );
            dr= parseISO8601Range(test); // for debugging
            drref= parseISO8601Range(ref);
        }
    }

    public static void testParse8601() throws ParseException {
        testParse8601_1( "2000-01-01T13:00Z/PT1H", "2000-01-01T13:00Z/2000-01-01T14:00" );
        testParse8601_1( "20000101T1300Z/PT1H", "2000-01-01T13:00Z/2000-01-01T14:00" );
        testParse8601_1( "2000-01-01T00:00Z/P1D", "2000-01-01T00:00Z/2000-01-01T24:00" );
        testParse8601_1( "2007-03-01T13:00:00Z/P1Y2M10DT2H30M", "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" );
        testParse8601_1( "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z", "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" );
        testParse8601_1( "P1Y2M10DT2H30M/2008-05-11T15:30:00Z", "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" );
    }
}
