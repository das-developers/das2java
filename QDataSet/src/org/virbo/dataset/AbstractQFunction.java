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

    public abstract QDataSet value(QDataSet parm);

    public QDataSet values(QDataSet vs ) {
        QDataSet v1= value( vs.slice(0) );
        JoinDataSet result= new JoinDataSet( v1 );
        for ( int i=1; i<vs.length(); i++ ) {
            result.join( value( vs.slice(i) ) );
        }
        return result;
    }

    public abstract QDataSet exampleInput();

    public QDataSet exampleOutput() {
        return value( exampleInput() );
    }

}
