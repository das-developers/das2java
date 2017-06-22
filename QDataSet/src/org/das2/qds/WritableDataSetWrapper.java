/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import org.das2.qds.ops.Ops;

/**
 * Jython needs all datasets to be writable, and this provides the write capability while avoiding unnecessary
 * copies when the dataset is never mutated.
 * @author jbf
 * @see DataSetWrapper
 */
public class WritableDataSetWrapper extends AbstractDataSet implements WritableDataSet {

    private QDataSet rods;
    private WritableDataSet ds;
    
    /**
     * create the WritableDataSetWrapper
     * @param ds the dataset.  
     */
    public WritableDataSetWrapper( QDataSet ds ) {
        this.rods= ds;
        this.ds= null;
    }
    
    
    /**
     * return the rank of the dataset.
     * @return the rank
     */
    @Override
    public int rank() {
        return rods.rank();
    }

    
    /**
     * return the value of this rank 0 dataset.
     * @return the value
     */
    @Override
    public double value(  ) {
        return ds!=null ? ds.value() : rods.value();
    }
    
    /**
     * return the value at this index.
     * @param i0 index 0
     * @return the value
     */
    @Override
    public double value( int i0 ) {
        return ds!=null ? ds.value(i0) : rods.value(i0);
    }
    
    /**
     * return the value at this index.
     * @param i0 index 0
     * @param i1 index 1
     * @return the value
     */
    @Override
    public double value( int i0, int i1 ) {
        return ds!=null ? ds.value(i0,i1) : rods.value(i0,i1);
    }
    
    /**
     * return the value at this index.
     * @param i0 index 0
     * @param i1 index 1
     * @param i2 index 2
     * @return the value
     */
    @Override
    public double value( int i0, int i1, int i2 ) {
        return ds!=null ? ds.value(i0,i1,i2) : rods.value(i0,i1,i2);
    }
    
    /**
     * return the value at this index.
     * @param i0 index 0
     * @param i1 index 1
     * @param i2 index 2
     * @param i3 index 3
     * @return the value
     */
    @Override
    public double value( int i0, int i1, int i2, int i3 ) {
        return ds!=null ? ds.value(i0,i1,i2,i3) : rods.value(i0,i1,i2,i3);
    }
    
    private void initWritableDataSet() {
        ds= Ops.copy(rods);
    }
    
    /**
     * insert the new value at this index.  If this is the first write to the dataset, 
     * then a writable dataset is created.
     * @param d the value
     */    
    @Override
    public void putValue(double d) {
        if ( ds==null ) initWritableDataSet();
        ds.putValue(d);
    }

    /**
     * insert the new value at this index.  If this is the first write to the dataset, 
     * then a writable dataset is created.
     * @param i0 index 0 
     * @param d the value
     */    
    @Override
    public void putValue(int i0, double d) {
        if ( ds==null ) initWritableDataSet();
        ds.putValue(i0,d);
    }

    /**
     * insert the new value at this index.  If this is the first write to the dataset, 
     * then a writable dataset is created.
     * @param i0 index 0 
     * @param i1 index 1
     * @param d the value
     */    
    @Override
    public void putValue(int i0, int i1, double d) {
        if ( ds==null ) initWritableDataSet();
        ds.putValue(i0,i1,d);
    }

    /**
     * insert the new value at this index.  If this is the first write to the dataset, 
     * then a writable dataset is created.
     * @param i0 index 0 
     * @param i1 index 1
     * @param i2 index 2
     * @param d the value
     */
    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        if ( ds==null ) initWritableDataSet();
        ds.putValue(i0,i1,i2,d);
    }

    /**
     * insert the new value at this index.  If this is the first write to the dataset, 
     * then a writable dataset is created.
     * @param i0 index 0 
     * @param i1 index 1
     * @param i2 index 2
     * @param i3 index 3
     * @param d the value
     */    
    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        if ( ds==null ) initWritableDataSet();
        ds.putValue(i0,i1,i2,i3,d);
    }

    /**
     * return the length the dimension at the index.
     * @return the length of the dimension.
     */
    @Override
    public int length() {
        return ds.length(); 
    }

    /**
     * return the length the dimension at the index.
     * @param i0 index 0 
     * @return the length of the dimension.
     */
    @Override
    public int length(int i0) {
        return ds.length(i0); 
    }

    /**
     * return the length the dimension at the index.
     * @param i0 index 0 
     * @param i1 index 1 
     * @return the length of the dimension.
     */
    @Override
    public int length(int i0, int i1) {
        return ds.length(i0, i1); 
    }

    /**
     * return the length the dimension at the index.
     * @param i0 index 0 
     * @param i1 index 1 
     * @param i2 index 2 
     * @return the length of the dimension.
     */
    @Override
    public int length(int i0, int i1, int i2) {
        return ds.length(i0, i1, i2); 
    }
    
    
}
