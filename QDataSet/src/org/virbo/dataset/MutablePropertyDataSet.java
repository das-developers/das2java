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
 *
 * @author jbf
 */
public interface MutablePropertyDataSet extends QDataSet {
    void putProperty( String name, Object value );
    void putProperty( String name, int index, Object value );
    void putProperty( String name, int index1, int index2, Object value );
}
