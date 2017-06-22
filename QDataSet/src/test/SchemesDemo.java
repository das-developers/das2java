
package test;

import org.das2.qds.examples.Schemes;

/**
 *
 * @author faden@cottagesystems.com
 */
public class SchemesDemo {
    
    private static void test1() {
        if ( ! Schemes.isTrajectory( Schemes.trajectory() ) ) throw new IllegalArgumentException();
        if ( ! Schemes.isRank1AlongTrajectory( Schemes.rank1AlongTrajectory() ) ) throw new IllegalArgumentException();
        if ( ! Schemes.isAngleDistribution( Schemes.angleDistribution() ) ) throw new IllegalArgumentException();        
        if ( ! Schemes.isScalarTimeSeries( Schemes.scalarTimeSeries() ) ) throw new IllegalArgumentException();
        if ( ! Schemes.isSimpleSpectrogramTimeSeries( Schemes.simpleSpectrogramTimeSeries() ) ) throw new IllegalArgumentException();
    }
        
    public static void main( String[] args ) {
        test1();
    }
}
