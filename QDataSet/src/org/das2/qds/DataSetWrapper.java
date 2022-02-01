package org.das2.qds;

/**
 * Wraps a dataset, making the properties mutable.  This is also intended to be base class for extension.
 * @author jbf
 * @see WritableDataSetWrapper which makes the values writable.
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
        for ( int i=0; i<ds.rank(); i++ ) {
            QDataSet dep= (QDataSet) ds.property("BUNDLE_"+i);
            if ( dep!=null && !(dep instanceof MutablePropertyDataSet) ) {
                properties.put( "BUNDLE_"+i, new DataSetWrapper(dep) );
            } else {
                if ( dep==null ) {
                    break;
                }
            }
        }
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return ds.value(i0, i1, i2, i3);
    }
    
    @Override
    public double value(int i0, int i1, int i2) {
        return ds.value(i0, i1, i2);
    }

    @Override
    public double value(int i0, int i1) {
        return ds.value(i0, i1);
    }

    @Override
    public double value(int i) {
        return ds.value(i);
    }

    @Override
    public double value() {
        return ds.value();
    }

    @Override
    public int rank() {
        return ds.rank();
    }

    @Override
    public Object property(String name, int i) {
        Object v= super.property( name, i );
        return v!=null ? v : ds.property(name, i);
    }

    @Override
    public Object property(String name) {
        Object v= super.property( name );
        return v!=null ? v : ds.property(name);
    }

    @Override
    public int length(int i, int j, int k) {
        return ds.length(i, j, k);
    }

    @Override
    public int length(int i, int j) {
        return ds.length(i, j);
    }

    @Override
    public int length(int i) {
        return ds.length(i);
    }

    @Override
    public int length() {
        return ds.length();
    }
    
    @Override
    public QDataSet slice(int i) {
        return new DataSetWrapper(ds.slice(i));
    }

    @Override
    public QDataSet trim(int start, int end) {
        return new DataSetWrapper(ds.trim(start, end)); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

}
