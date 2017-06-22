
package org.das2.qds;

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
            if ( v1.rank()!=rank ) throw new IllegalArgumentException("incompatible datasets: two value calls result in datasets of differing rank");
            if ( v1.length()!=len ) throw new IllegalArgumentException("incompatible datasets: two value calls result in datasets of differing lengths");
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
