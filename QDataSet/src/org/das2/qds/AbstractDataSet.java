/*
 * AbstractDataSet.java
 *
 * Created on April 2, 2007, 8:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Units;
import org.das2.qds.examples.Schemes;
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
    boolean hasIndexedProperties= false;
    protected HashMap<String,Object> properties;
    private boolean immutable= false;
    
    public AbstractDataSet() {
        properties= new HashMap<>();
    }

    @Override
    public abstract int rank();

    @Override
    public double value() {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }
    
    @Override
    public String svalue() {
        Units u= (Units) property(QDataSet.UNITS);
        if ( u==null ) {
            // https://github.com/autoplot/dev/blob/master/bugs/2024/20240116/svalueBug.jy
            return String.valueOf(value());
        } else {
            return u.createDatum(value()).toString();
        }
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
        Object r= null;
        if ( hasIndexedProperties ) {
            String pname= name + "__" + i;
            r= properties.get( pname );
        }
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
     * @param name property name
     * @param value property value
     */
    private static void checkPropertyType( String name, Object value ) {
        switch ( name ) {
            case QDataSet.DELTA_MINUS:
            case QDataSet.DELTA_PLUS: 
            case QDataSet.BIN_MINUS:
            case QDataSet.BIN_PLUS:
            case QDataSet.WEIGHTS:
                if ( value!=null && !( value instanceof QDataSet ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a QDataSet (%s)", name, value.toString() ) );
                }
                return;
            case QDataSet.DEPEND_0:
            case QDataSet.DEPEND_1:
            case QDataSet.DEPEND_2:
            case QDataSet.DEPEND_3:
            case QDataSet.BUNDLE_0:
            case QDataSet.BUNDLE_1:
            case QDataSet.BUNDLE_2:
            case QDataSet.BUNDLE_3:
                if ( value!=null && !( value instanceof QDataSet ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a QDataSet (%s)", name, value.toString() ) );
                }
                return;
            case QDataSet.UNITS:
                if ( value!=null && !( value instanceof Units ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a Unit (%s)", name, value.toString() ) );
                }
                return;
            case QDataSet.NAME:
            case QDataSet.LABEL:
            case QDataSet.TITLE:
            case QDataSet.FORMAT:
            case QDataSet.SCALE_TYPE:
            case QDataSet.METADATA_MODEL:
            case QDataSet.BINS_0:
            case QDataSet.BINS_1:
                if ( value!=null && !( value instanceof String ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a String (%s)", name, value.toString() ) );
                }
                return;
            case QDataSet.VALID_MIN:
            case QDataSet.VALID_MAX:
            case QDataSet.TYPICAL_MIN:
            case QDataSet.TYPICAL_MAX:
            case QDataSet.FILL_VALUE:
                if ( value!=null && !( value instanceof Number ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a Number (%s)", name, value.toString() ) );
                }
                return;                
            case QDataSet.USER_PROPERTIES:
            case QDataSet.METADATA:
                if ( value!=null && !( value instanceof Map ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a Map (%s)", name, value.toString() ) );
                }
                return;                
            case QDataSet.QUBE:
                if ( value!=null && !( value instanceof Boolean ) ) {
                    logger.warning( String.format( "AbstractDataSet.checkPropertyType: %s is not a Boolean (%s)", name, value.toString() ) );
                }
                return;
            default:
                return;
        }
    }

    @Override
    public void putProperty( String name, Object value ) {
        checkImmutable();

        String propCheckName= name;        
        int i__= name.indexOf("__");
        if ( i__>-1 ) {
            hasIndexedProperties= true;
            propCheckName= name.substring(0,i__);
        }
        if ( value!=null ) checkPropertyType( propCheckName, value );

        if ( name.startsWith("DEPEND_") ) {        
            if ( name.equals( QDataSet.DEPEND_0 ) && value!=null ) {
                if ( value instanceof QDataSet ) { // BUNDLES can have string value here -- TODO: this hasn't been true for 10 years DEPENDNAME property
                    QDataSet dep0= ((QDataSet)value);
                    if ( this.rank()>0 && dep0.length()!=this.length() ) {
                        if ( Schemes.isPolyMesh(dep0) ) {
                            int n= dep0.slice(1).length();
                            if ( n!=this.length() ) {
                                logger.log(Level.WARNING, "DEPEND_0 PolyMesh has incorrect length, its length is {0} should be {1}", new Object[]{n, this.length()});
                            }
                        } else {
                            logger.log(Level.WARNING, "DEPEND_0 is incorrect length, its length is {0} should be {1}", new Object[]{dep0.length(), this.length()});
                        }
                    }
                    if ( this==value ) {
                        logger.log(Level.WARNING, "{0} is self-referential, causing infinite loop", name);
                        logger.log(Level.WARNING, "ignoring putProperty call" );
                        return;
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
            if ( ( name.charAt(7)-'0') > 0 ) {
                if ( value instanceof QDataSet ) {
                    if ( ((QDataSet)value).rank()>1 ) {
                        //Object o= properties.remove(QDataSet.QUBE); // BufferDataSet automatically sets QUBE to be true.
                        //if ( o!=null ) {
                        //    logger.log(Level.FINER, "removing QUBE property (if any) because high rank {0}", name);
                        //}
                        logger.log(Level.FINER, "high rank 2 {0} might imply this is not a QUBE.", name);
                    }
                }
            }
        }
        properties.put( name, value );
        
    }
    
    @Override
    public void putProperty( String name, int index, Object value ) {
        checkImmutable();
        hasIndexedProperties= true;
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
     * @see org.das2.qds.DataSetUtil#toString(org.das2.qds.QDataSet) 
     */
    @Override
    public String toString( ) {
        return DataSetUtil.toString(this);
    }
}
