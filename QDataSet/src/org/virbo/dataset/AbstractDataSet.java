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
        String pname= name + "__" + i;
        Object r= properties.get( pname );
        if ( r!=null ) {
            return r;
        } else {
            if ( DataSetUtil.isInheritedProperty(name) ) {
                return properties.get(name);
            } else {
                return null;
            }
        }
    }

    /**
     * print a warning message when the property is not the correct type.
     * @param name
     * @param value
     */
    private void checkPropertyType( String name, Object value ) {
        if ( name.equals(QDataSet.DELTA_MINUS) || name.equals(QDataSet.DELTA_PLUS)
                || name.equals(QDataSet.DEPEND_0) ) {
            if ( value!=null && !( value instanceof QDataSet ) ) {
                System.err.println( String.format( "AbstractDataSet.checkPropertyType: %s is not a QDataSet", name, value ) );
            }
        } 
    }

    public void putProperty( String name, Object value ) {
        checkPropertyType( name, value );
        properties.put( name, value );
        if ( name.equals( QDataSet.DEPEND_0 ) && value!=null ) {
            if ( value instanceof QDataSet ) { // BUNDLES can have string value here
                QDataSet dep0= ((QDataSet)value);
                if ( this.rank()>0 && dep0.length()!=this.length() ) {
                    System.err.println("DEPEND_0 is incorrect length, its length is "+dep0.length()+ " should be "+this.length() );
                }
            } else if ( value instanceof String ) {
                System.err.println("Use DEPENDNAME_0 instead of DEPEND_0");
            }
        } else if ( name.equals( QDataSet.DEPEND_1 ) && value!=null ) {
            if ( this.rank()<=1 ) {
                System.err.println("DEPEND_1 was set on dataset of rank 0 or rank 1.  Ignoring...");
            } else {
                if ( value instanceof QDataSet ) { // BUNDLES can have string value here
                    QDataSet dep1= ((QDataSet)value);
                    if ( this.rank()>0 && this.length()>0 && dep1.length()!=this.length(0) ) {
                        System.err.println("DEPEND_1 is incorrect length, its length is "+dep1.length()+ " should be "+this.length() );
                    }
                } else if ( value instanceof String ) {
                    System.err.println("Use DEPENDNAME_1 instead of DEPEND_1");
                }
            }
        }
    }
    
    public void putProperty( String name, int index, Object value ) {
        properties.put( name + "__" + index, value );
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
        throw new IllegalArgumentException("rank error, expected "+rank()+", NAME="+this.property(QDataSet.NAME) );
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
