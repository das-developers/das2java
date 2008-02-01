/*
 * Slice0DataSet.java
 *
 * Created on February 1, 2007, 10:41 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * Wraps a rank N dataset, slicing on an index of the first dimension to make a rank N-1 dataset.
 * This is currently used to implement DataSetOps.slice0().
 * 
 * @author jbf
 */
public class Slice0DataSet implements QDataSet {
    
    final QDataSet ds;
    final int index;
    
    /** Creates a new instance of Slice0DataSet */
    public Slice0DataSet( QDataSet source, int index ) {
        this.ds= source;
        this.index= index;
    }
    
    public int rank() {
        return ds.rank()-1;
    }
    
    public double value(int i) {
        return ds.value( index, i );
    }
    
    public double value(int i0, int i1) {
        return ds.value( index, i0, i1 );
    }
    
    public double value(int i0, int i1, int i2) {
        throw new IllegalArgumentException("rank limit");
    }
    
    public Object property(String name) {
        if ( name.startsWith("DEPEND_") ) {
            if ( name.equals( DEPEND_1) ) name= DEPEND_2;
            if ( name.equals( DEPEND_0) ) name= DEPEND_1;
            return ds.property(name);
        } else {
            return ds.property(name,index);
        }
    }
    
    public Object property(String name, int i) {
        return ds.property(name, index, i );
    }
    
    public Object property(String name, int i0, int i1) {
        throw new IllegalArgumentException("rank limit");
    }
    
    public int length() {
        return ds.length(index);
    }
    
    public int length(int i) {
        return ds.length(index,i);
    }
    
    public int length(int i, int j) {
        throw new IllegalArgumentException("rank limit");
    }
    
    public String toString( ) {
        return DataSetUtil.toString(this);
    }
}
