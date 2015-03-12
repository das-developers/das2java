/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

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
     * each element of vs
     * @param vs rank N+1 set of values, where {@code exampleInput} returns rank N.
     * @return rank M+1 set of values, where {@code value} returns rank M.
     */
    @Override
    public QDataSet values(QDataSet vs ) {
        QDataSet v1= value( vs.slice(0) );
        JoinDataSet result= new JoinDataSet( v1 );
        for ( int i=1; i<vs.length(); i++ ) {
            result.join( value( vs.slice(i) ) );
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
