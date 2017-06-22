/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * reverses the order of the elements of the rank 1 dataset.  If there is a DEPEND_0,
 * this is reversed as well.
 * @author jbf
 */
public class ReverseDataSet extends AbstractDataSet {

    QDataSet source;
    int len;

    public ReverseDataSet( QDataSet source ) {
        this.source= source;
        this.len= source.length();
        if ( source.rank()!=1 ) throw new IllegalArgumentException("only rank 1 supported");
    }

    @Override
    public int length() {
        return len;
    }

    @Override
    public double value(int i) {
        return source.value(len-1-i);
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public Object property(String name) {
        if ( name.equals(QDataSet.DEPEND_0) ) {
            QDataSet dep0= (QDataSet) source.property(QDataSet.DEPEND_0);
            if ( dep0!=null ) {
                return new ReverseDataSet(dep0);
            } else {
                return null;
            }
        } else {
            if ( this.properties.containsKey(name) ) {
                return this.properties.get(name);
            } else {
                return source.property(name);
            }
        }
    }


}
