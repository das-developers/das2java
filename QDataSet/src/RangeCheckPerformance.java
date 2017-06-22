
/**
 * See if it is really worthwhile to not check indeces in QDataSet.
 * This demonstrated that:
 * 1. the performance impact is trivial (&lt;0.2 percent)
 * 2. a Java loop is about 1700 times faster than Jython (12.340 / 0.007) see /home/jbf/ct/autoplot/users/2016/ivar/20160902/rangeCheckPerformance.jy
 * @author jbf
 */
import org.das2.qds.DDataSet;
import static org.das2.qds.ops.Ops.*;

public class RangeCheckPerformance {
    
    public static void main( String[] args ) {
        for ( int k=0; k<100; k++ ) {
            doit();
        }
    }
    
    private static void doit() {
        DDataSet ds1 = (DDataSet)zeros(20000,10000);
        long t0= System.currentTimeMillis();

        for ( int i=0; i<20000; i++ ) {
            for ( int j=0; j<10000; j++ ) {
                ds1.putValue(i,j,0.);
            }
        }
        System.err.println( ( System.currentTimeMillis()-t0 ) / 1000. );
    }
}
