/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.datum;
import java.util.List;
import org.das2.datum.DatumRange;
import static org.das2.datum.DatumRangeUtil.*;

/**
 *
 * @author jbf
 */
public class TestDatumRangeUtil {
    public static void main( String[] args ) {
        testIntersection();
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
}
