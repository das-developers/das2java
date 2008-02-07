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
 *
 * @author jbf
 */
public class SortDataSet extends AbstractDataSet {
    
    QDataSet source;
    QDataSet sort;
            
    public SortDataSet( QDataSet source, QDataSet sort ) {
        this.source= source;
        this.sort= sort;
        properties= new HashMap();
        Object o= source.property( QDataSet.DEPEND_0 );
        if ( o!=null ) properties.put( QDataSet.DEPEND_0, new SortDataSet( (QDataSet)o, sort ) );
        o= source.property( "plane0" );
        if ( o!=null ) properties.put( "plane0", new SortDataSet( (QDataSet)o, sort ) );
        if ( source.rank() > 1 ) throw new IllegalArgumentException("rank not supported");
    }

    public int rank() {
        return source.rank();
    }

    public double value(int i) {
        return source.value( (int)sort.value(i) );
    }

    public double value(int i0, int i1) {
        throw new IllegalArgumentException("rank limit");
    }

    public double value(int i0, int i1, int i2) {
        throw new IllegalArgumentException("rank limit");
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
            return source.property(name,i);
        }
    }

    public Object property(String name, int i0, int i1) {
        if ( properties.containsKey(name) ) {
            return properties.get(name);
        } else {
            return source.property(name,i0,i1);
        }
    }

    public int length() {
        return sort.length();
    }

    public int length(int i) {
        return sort.length(i);
    }

    public int length(int i, int j) {
        return sort.length(i,j);
    }
    
}
