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
 * DataSet that allows its properties to be changed.  
 * @author jbf
 */
public interface MutablePropertyDataSet extends QDataSet {
    void putProperty( String name, Object value );
    void putProperty( String name, int index, Object value );
    
    /**
     * mark the dataset as being immutable.  Once this is called, calls to
     * mutating properties will print warning messages for now, but will soon
     * be an error.
     */
    void makeImmutable(); 
    
    /**
     * return true if the dataset has been made immutable.
     * @return 
     */
    boolean isImmutable();
}
