/*
 * TransposeRank2DataSet.java
 *
 * Created on December 11, 2007, 10:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 *
 * @author jbf
 */
public class TransposeRank2DataSet extends AbstractDataSet {
    
    QDataSet source;
    
    /** Creates a new instance of TransposeRank2DataSet */
    public TransposeRank2DataSet( QDataSet source ) {
        this.source= source;
        properties.put( QDataSet.DEPEND_0, source.property( QDataSet.DEPEND_1 ) );
        properties.put( QDataSet.DEPEND_1, source.property( QDataSet.DEPEND_0 ) );
    }

    public int rank() {
        return source.rank();
    }


    public double value(int i0, int i1) {
        return source.value( i1, i0 );
    }

    public Object property(String name) {
        Object v= properties.get(name);
        return ( v==null ) ? source.property(name) : v;
    }

    public Object property(String name, int i) {
        Object v= properties.get(name);
        return ( v==null ) ? source.property(name,i) : v;
    }

    public Object property(String name, int i0, int i1) {
        Object v= properties.get(name);
        return ( v==null ) ? source.property(name,i0,i1) : v;
    }

    public int length() {
        return source.length(0);
    }

    public int length(int i) {
        return source.length();
    }
    
}
