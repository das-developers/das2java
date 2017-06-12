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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.das2.util.LoggerManager;

/**
 * Abstract class to simplify defining datasets.  This handles the
 * properties for implementations, and they
 * need only implement rank, and override the corresponding value and length
 * methods.
 *
 * @author jbf
 */
public abstract class AbstractDataSet implements QDataSet, MutablePropertyDataSet {

    private static final Logger logger= LoggerManager.getLogger("qdataset");
    protected HashMap<String,Object> properties;
    private boolean immutable= false;
    
    public AbstractDataSet() {
        properties= new HashMap<String,Object>();
    }

    @Override
    public abstract int rank();

    @Override
    public double value() {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }
    
    @Override
    public double value(int i0) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    @Override
    public double value(int i0, int i1) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    @Override
    public double value(int i0, int i1, int i2) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    @Override
    public Object property(String name) {
        return properties.get(name);
    }

    @Override
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
        String[] props= DataSetUtil.correlativeProperties();
        for (String prop : props) {
            if (name.equals(prop)) {
                if ( value!=null && !( value instanceof QDataSet ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a QDataSet (%s)", name, value.toString() ) );
                }
            }
        }
        if ( name.equals(QDataSet.DEPEND_0) ) {
            if ( value!=null && !( value instanceof QDataSet ) ) {
                logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a QDataSet (%s)", name, value.toString() ) );
            }
        }
        if ( name.equals(QDataSet.UNITS) ) {
            if ( value!=null && !( value instanceof Units ) ) {
                logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not as Unit (%s)", name, value.toString() ) );
            }
        }
    }

    @Override
    public void putProperty( String name, Object value ) {
        checkImmutable();
        checkPropertyType( name, value );
        properties.put( name, value );
        if ( name.equals( QDataSet.DEPEND_0 ) && value!=null ) {
            if ( value instanceof QDataSet ) { // BUNDLES can have string value here
                QDataSet dep0= ((QDataSet)value);
                if ( this.rank()>0 && dep0.length()!=this.length() ) {
                    logger.log(Level.WARNING, "DEPEND_0 is incorrect length, its length is {0} should be {1}", new Object[]{dep0.length(), this.length()});
                }
            } else if ( value instanceof String ) {
                logger.warning("Use DEPENDNAME_0 instead of DEPEND_0");
            }
        } else if ( name.equals( QDataSet.DEPEND_1 ) && value!=null ) {
            if ( this.rank()<=1 ) {
                logger.warning("DEPEND_1 was set on dataset of rank 0 or rank 1.  Ignoring...");
            } else {
                if ( value instanceof QDataSet ) { // BUNDLES can have string value here
                    QDataSet dep1= ((QDataSet)value);
                    if ( this.rank()>0 && this.length()>0 && dep1.rank()==1 && dep1.length()!=this.length(0) ) {
                        logger.log(Level.WARNING, "DEPEND_1 is incorrect length, its length is {0} should be {1}", new Object[]{dep1.length(), this.length(0)});
                    }
                } else if ( value instanceof String ) {
                    logger.warning("Use DEPENDNAME_1 instead of DEPEND_1");
                }
            }
        }
    }
    
    @Override
    public void putProperty( String name, int index, Object value ) {
        checkImmutable();
        properties.put( name + "__" + index, value );
    }

    @Override
    public void makeImmutable() {
        immutable= true;
    }
    
    @Override
    public boolean isImmutable() {
        return this.immutable;
    }
    
    /**
     * Implementations should call this when attempts to modify the dataset 
     * are performed, so that it is guaranteed that it is safe to modify.  
     * This will throw an exception once things look stable.  
     * Note there are scripts in the wild that abuse mutability that need to be 
     * fixed before this can be done.
     */
    protected final void checkImmutable() {
        if ( immutable ) {
            throw new IllegalArgumentException("dataset has been marked as immutable");
        }
    } 
        
    @Override
    public int length() {
        throw new IllegalArgumentException("rank error, rank 0 datasets have no length()");
    }

    @Override
    public int length(int i0) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    @Override
    public int length(int i0, int i1) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    @Override
    public int length(int i0, int i1, int i2) {
        throw new IllegalArgumentException("rank error, expected "+rank()+", NAME="+this.property(QDataSet.NAME) );
    }

    @Override
    public <T> T capability(Class<T> clazz) {
        return null;
    }

    @Override
    public QDataSet slice(int i) {
        return new Slice0DataSet(this, i);
    }

    @Override
    public QDataSet trim(int start, int end) {
        if ( start==0 && end==length() ) {
            return this;
        } else {
            return new TrimDataSet( this, start, end );
        }
    }

    /**
     * return a human-readable string representation of this QDataSet.
     * @return a human-readable string representation of this QDataSet.
     * @see org.virbo.dataset.DataSetUtil#toString(org.virbo.dataset.QDataSet) 
     */
    @Override
    public String toString( ) {
        return DataSetUtil.toString(this);
    }
}
