/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test;

import org.das2.datum.DatumRangeUtil;

/**
 *
 * @author jbf
 */
public class TestTimeParser {

    private static void test1( String test, String norm ) {
         if ( ! DatumRangeUtil.parseISO8601Range(norm).equals( DatumRangeUtil.parseISO8601Range(test) ) ) {
             throw new RuntimeException("fails to match: "+test);
         }
    }
    public static void main( String[] args ) {
        DatumRangeUtil.parseTimeRangeValid("P5D");
        test1( "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z", "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" );
        test1( "2007-03-01T13:00:00Z/P1Y2M10DT2H30M", "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" );
        test1( "P1Y2M10DT2H30M/2008-05-11T15:30:00Z", "2007-03-01T13:00:00Z/2008-05-11T15:30:00Z" );
        test1( "2007-03-01T00:00Z/P1D", "2007-03-01T00:00:00Z/2007-03-02T00:00:00Z" );

    }
}
