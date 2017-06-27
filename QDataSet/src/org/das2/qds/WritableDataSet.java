/*
 * WritableDataSet.java
 *
 * Created on January 25, 2007, 10:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * Some QDataSets are be mutable as well, meaning their values can be assigned
 * as well as read.  In addition to the value() method they have putValue() methods.
 * These datasets cannot be written to once they are made 
 * immutable by calling the makeImmutable() of MutablePropertyDataSet, and clients 
 * must check the isImmutable flag or call Ops.maybeCopy when they need write 
 * access to the data.
 *   
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * @author jbf
 * @see org.das2.qds.ops.Ops#maybeCopy(org.das2.qds.QDataSet) 
 * @see MutablePropertyDataSet#isImmutable() 
 */
public interface WritableDataSet extends MutablePropertyDataSet {
    /**
     * put a value into the rank 0 dataset.
     * @param d the value
     * @throws IllegalArgumentException if the dataset immutable or is not rank 0.
     */
    void putValue( double d );

    /**
     * put a value into the rank 1 dataset.
     * @param i0 the index
     * @param d the value
     * @throws IllegalArgumentException if the dataset immutable or is not rank 1.
     */
    void putValue( int i0, double d );

    /**
     * put a value into the rank 2 dataset.
     * @param i0 the index
     * @param i1 the index
     * @param d the value
     * @throws IllegalArgumentException if the dataset immutable or is not rank 2.
     */
    void putValue( int i0, int i1, double d );

    /**
     * put a value into the rank 3 dataset.
     * @param i0 the index
     * @param i1 the index
     * @param i2 the index
     * @param d the value
     * @throws IllegalArgumentException if the dataset immutable or is not rank 3.
     */
    void putValue( int i0, int i1, int i2, double d );
       
    /**
     * put a value into the rank 4 dataset.
     * @param i0 the index
     * @param i1 the index
     * @param i2 the index
     * @param i3 the index
     * @param d the value
     * @throws IllegalArgumentException if the dataset immutable or is not rank 4.
     */
    void putValue( int i0, int i1, int i2, int i3, double d);
}
