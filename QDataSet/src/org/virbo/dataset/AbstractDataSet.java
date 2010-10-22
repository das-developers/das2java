/*
 * AbstractDataSet.java
 *
 * Created on April 2, 2007, 8:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.HashMap;

/**
 * Abstract class to simplify defining datasets.  Implement rank,
 * and override value and length.
 *
 * @author jbf
 */
public abstract class AbstractDataSet implements QDataSet, MutablePropertyDataSet {
    
    protected HashMap<String,Object> properties;
    
    public AbstractDataSet() {
        properties= new HashMap<String,Object>();
    }

    public abstract int rank();

    public double value() {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }
    
    public double value(int i0) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public double value(int i0, int i1) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public double value(int i0, int i1, int i2) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public double value(int i0, int i1, int i2, int i3) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public Object property(String name) {
        return properties.get(name);
    }

    public Object property(String name, int i) {
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return properties.get(name);
        } else {
            return null;
        }
    }

    public Object property(String name, int i0, int i1) {
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return properties.get(name);
        } else {
            return null;
        }
    }

    public Object property(String name, int i0, int i1, int i2) {
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return properties.get(name);
        } else {
            return null;
        }
    }

    public Object property(String name, int i0, int i1, int i2, int i3) {
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return properties.get(name);
        } else {
            return null;
        }
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

    public void putProperty( String name, int index1, int index2, int index3, Object value) {
        properties.put( name, value );
    }

    public void putProperty( String name, int index1, int index2, int index3, int index4, Object value) {
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

    public int length(int i, int j, int k) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    public <T> T capability(Class<T> clazz) {
        return null;
    }

    public QDataSet slice(int i) {
        return new Slice0DataSet(this, i);
    }

    public QDataSet trim(int start, int end) {
        return new TrimDataSet( this, start, end );
    }


    @Override
    public String toString( ) {
        return DataSetUtil.toString(this);
    }
}
