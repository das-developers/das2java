
package org.das2.qds;

import org.das2.qds.ops.Ops;

/**
 * Abstract class implements values and exampleOutput based on
 * value and exampleInput.
 *
 * @author jbf
 */
public abstract class AbstractQFunction implements QFunction {

    @Override
    public abstract QDataSet value(QDataSet parm);

    /**
     * calculate the values by calling the {@code value} function for
     * each element of {@code vs}.  A check is made for each call to the {@code value}
     * function that a dataset with the same rank and length is returned.
     * @param vs rank N+1 set of values, where {@code exampleInput} returns rank N.
     * @return rank M+1 set of values, where {@code value} returns rank M.
     */
    @Override
    public QDataSet values(QDataSet vs ) {
        QDataSet v1= value( vs.slice(0) );
        int rank= v1.rank();
        int len= v1.length();
        JoinDataSet result= new JoinDataSet( v1 );
        for ( int i=1; i<vs.length(); i++ ) {
            v1= value( vs.slice(i) );
//            //vap+das2server:http://planet.physics.uiowa.edu/das/das2Server?dataset=Voyager/1/Ephemeris/Heliographic_Inertial&start_time=1998-09-16T17:53:30.402Z&end_time=2001-01-31T14:08:19.855Z&interval=60
//            if ( Ops.subtract( vs.slice(i).slice(0), Ops.dataset("2000-01-01T00:00:00Z" ) ).value()==0.0 ) {
//                System.err.println( String.format( "%s: %s", vs.slice(i).slice(0).svalue(), v1.slice(0).svalue() ) );
//                if ( !v1.slice(0).svalue().startsWith("76.1595") ) {
//                    v1= value( vs.slice(i) );
//                }
//            }
            if ( v1.rank()!=rank ) {
                throw new IllegalArgumentException("incompatible datasets: two value calls result in datasets of differing rank");
            }
            if ( v1.length()!=len ) {
                throw new IllegalArgumentException("incompatible datasets: two value calls result in datasets of differing lengths");
            }
            result.join( v1 );
        }
        return result;
    }

    @Override
    public abstract QDataSet exampleInput();

    /**
     * this simply calls {@code value( exampleInput() )}.
     * @return 
     */
    @Override
    public QDataSet exampleOutput() {
        return value( exampleInput() );
    }

}
