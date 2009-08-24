/*
 * SortDataSet.java
 *
 * Created on April 2, 2007, 8:52 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.HashMap;

/**
 * wraps QDataSet, rearranging the elements of the first index as specified
 * by a rank 1 data set of indeces
 * 
 * @author jbf
 */
public class SortDataSet extends AbstractDataSet {
    
    QDataSet source;
    QDataSet sort;
            
    /**
     * creates the SortDataSet
     * @param source rank N dataset.  Supports plane_0.  Supports rank 2 Depend_1.
     * @param sort the indeces of the sort. 
     */
    public SortDataSet( QDataSet source, QDataSet sort ) {
        this.source= source;
        this.sort= sort;
        properties= new HashMap();
        QDataSet dep0= (QDataSet) source.property( QDataSet.DEPEND_0 );
        if ( dep0!=null ) properties.put( QDataSet.DEPEND_0, new SortDataSet( dep0, sort ) );
        QDataSet p0= (QDataSet) source.property( QDataSet.PLANE_0 );
        if ( p0!=null ) properties.put( QDataSet.PLANE_0, new SortDataSet( p0, sort ) );
        QDataSet dep1= (QDataSet) source.property( QDataSet.DEPEND_1 );
        if ( dep1!=null && dep1.rank()>1 ) {
            properties.put( QDataSet.DEPEND_1, new SortDataSet( dep1, sort ) );
        }
    }

    public int rank() {
        return source.rank();
    }

    public double value(int i) {
        return source.value( (int)sort.value(i) );
    }

    public double value(int i0, int i1) {
        return source.value( (int)sort.value(i0), i1 );
    }

    public double value(int i0, int i1, int i2) {
        return source.value( (int)sort.value(i0), i1, i2 );
    }

    public Object property(String name) {
        if ( properties.containsKey(name) ) {
            return properties.get(name);
        } else {
            return source.property(name);
        }
    }

    public Object property(String name, int i) {
        if ( properties.containsKey(name) ) {
            return properties.get(name);
        } else {
            return source.property(name,(int)sort.value(i));
        }
    }

    public Object property(String name, int i0, int i1) {
        if ( properties.containsKey(name) ) {
            return properties.get(name);
        } else {
            return source.property(name,(int)sort.value(i0),i1);
        }
    }

    public int length() {
        return sort.length();
    }

    public int length(int i) {
        return source.length( (int)sort.value(i) );
    }

    public int length(int i, int j) {
        return source.length( (int)sort.value(i), j );
    }
    
}
