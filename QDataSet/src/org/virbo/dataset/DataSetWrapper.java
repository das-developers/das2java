/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * Wraps a dataset, making the properties mutable.  This is also indended to be base class for extension.
 * @author jbf
 */
public class DataSetWrapper extends AbstractDataSet {
    QDataSet ds;
    public DataSetWrapper( QDataSet ds ) {
        this.ds= ds;
        for ( int i=0; i<ds.rank(); i++ ) {
            QDataSet dep= (QDataSet) ds.property("DEPEND_"+i);
            if ( dep!=null && !(dep instanceof MutablePropertyDataSet) ) {
                properties.put( "DEPEND_"+i, new DataSetWrapper(dep) );
            }
        }
        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            QDataSet dep= (QDataSet) ds.property("PLANE_"+i);
            if ( dep!=null && !(dep instanceof MutablePropertyDataSet) ) {
                properties.put( "PLANE_"+i, new DataSetWrapper(dep) );
            } else {
                if ( dep==null ) {
                    break;
                }
            }
        }
    }

    public double value(int i0, int i1, int i2) {
        return ds.value(i0, i1, i2);
    }

    public double value(int i0, int i1) {
        return ds.value(i0, i1);
    }

    public double value(int i) {
        return ds.value(i);
    }

    public int rank() {
        return ds.rank();
    }

    public Object property(String name, int i0, int i1) {
        Object v= properties.get(name);
        return v!=null ? v : ds.property(name, i0, i1);
    }

    public Object property(String name, int i) {
        Object v= properties.get(name);
        return v!=null ? v : ds.property(name, i);
    }

    public Object property(String name) {
        Object v= properties.get(name);
        return v!=null ? v : ds.property(name);
    }

    public int length(int i, int j) {
        return ds.length(i, j);
    }

    public int length(int i) {
        return ds.length(i);
    }

    public int length() {
        return ds.length();
    }
    

}
