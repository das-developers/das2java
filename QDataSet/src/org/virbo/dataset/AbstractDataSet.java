/*
 * AbstractDataSet.java
 *
 * Created on April 2, 2007, 8:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class to simplify defining datasets.  Implement rank,
 * and override value and length.
 *
 * @author jbf
 */
public abstract class AbstractDataSet implements QDataSet, MutablePropertyDataSet {
    
    protected HashMap properties;
    
    public AbstractDataSet() {
        properties= new HashMap();
    }

    public abstract int rank();

    public double value(int i) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public double value(int i0, int i1) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public double value(int i0, int i1, int i2) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public Object property(String name) {
        return properties.get(name);
    }

    public Object property(String name, int i) {
        return properties.get(name);
    }

    public Object property(String name, int i0, int i1) {
        return properties.get(name);
    }
    
    public void putProperty( String name, Object value ) {
        properties.put( name, value );
    }
    
    public void putProperty( String name, int index, Object value ) {
        properties.put( name, value );
    }
    
    public void putProperty( String name, int index1, int index2, Object value ) {
        properties.put( name, value );
    }
    
    public int length() {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public int length(int i) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public int length(int i, int j) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }
    
    public String toString( ) {
        return DataSetUtil.toString(this);
    }
}
