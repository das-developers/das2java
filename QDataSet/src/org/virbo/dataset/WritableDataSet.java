/*
 * WritableDataSet.java
 *
 * Created on January 25, 2007, 10:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 *   
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * putValue method implementations should check isMutable() method and should call 
 * @author jbf
 */
public interface WritableDataSet extends MutablePropertyDataSet {
    void putValue( double d );
    void putValue( int i0, double d );
    void putValue( int i0, int i1, double d );
    void putValue( int i0, int i1, int i2, double d );
    void putValue( int i0, int i1, int i2, int i3, double d);
}
