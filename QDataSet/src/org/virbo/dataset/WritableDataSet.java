/*
 * WritableDataSet.java
 *
 * Created on January 25, 2007, 10:03 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import org.das2.datum.Datum;
import org.das2.datum.Units;

/**
 *   
 * Mutable datasets warning: No dataset should be mutable once it is accessible to the
 * rest of the system.  This would require clients make defensive copies which would 
 * seriously degrade performance.  
 *
 * @author jbf
 */
public interface WritableDataSet extends MutablePropertyDataSet {
    void putValue( int i0, double d );
    void putValue( int i0, int i1, double d );
    void putValue( int i0, int i1, int i2, double d );
    void putProperty( String name, Object object );
    void putProperty( String name, int i0, Object object );
    void putProperty( String name, int i0, int i1, Object object );
}
