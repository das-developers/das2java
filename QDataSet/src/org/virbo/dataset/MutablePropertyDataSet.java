/*
 * MutablePropertyDataSet.java
 *
 * Created on April 3, 2007, 6:21 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * Some QDataSets allow their properties to be changed.  Note scripts should
 * never assume a dataset is mutable, and should call the putProperty method 
 * instead, which will make a copy if necessary.  Note the terms mutable, 
 * writable, and modifiable are all used in this documentation and interchangeable.
 * @see WritableDataSet which allows the values to be modified as well.
 * @author jbf
 */
public interface MutablePropertyDataSet extends QDataSet {
    
    /**
     * assign the name value to the property.  
     * @param name property name like "UNITS" (Use QDataSet.UNITS)
     * @param value the property value.
     * @see org.virbo.dsops.Ops#putProperty(org.virbo.dataset.QDataSet, java.lang.String, java.lang.Object) which properly checks mutability of the dataset.
     * @see org.virbo.dataset.QDataSet#UNITS
     */
    void putProperty( String name, Object value );
    
    /**
     * assign the name value to the property at the slice index.  
     * @param name property name like "UNITS" (Use QDataSet.UNITS)
     * @param index the index of the slice.
     * @param value the property value.
     * @see org.virbo.dsops.Ops#putProperty(org.virbo.dataset.QDataSet, java.lang.String, java.lang.Object) 
     * @see org.virbo.dataset.QDataSet#UNITS
     * @see org.virbo.dataset.QDataSet#property(java.lang.String, int) 
     */
    void putProperty( String name, int index, Object value );
    
    /**
     * mark the dataset as being immutable.  Once this is called, calls to
     * mutating properties will print warning messages for now, but will soon
     * be an error.
     */
    void makeImmutable(); 
    
    /**
     * return true if the dataset has been made immutable.
     * @return true if the dataset has been made immutable.
     */
    boolean isImmutable();
}
