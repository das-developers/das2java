/*
 * TransposeRank2DataSet.java
 *
 * Created on December 11, 2007, 10:19 PM
 */

package org.das2.qds;

/**
 * old dataset type transposes a rank 2 dataset with DEPEND_1 and DEPEND_0.
 * @author jbf
 */
public class TransposeRank2DataSet extends AbstractDataSet {
    
    QDataSet source;
    
    /** Creates a new instance of TransposeRank2DataSet */
    public TransposeRank2DataSet( QDataSet source ) {
        this.source= source;
        QDataSet dep0=  (QDataSet) source.property( QDataSet.DEPEND_0 );
        QDataSet dep1=  (QDataSet) source.property( QDataSet.DEPEND_1 );
        QDataSet bund0=  (QDataSet) source.property( QDataSet.BUNDLE_0 );
        QDataSet bund1=  (QDataSet) source.property( QDataSet.BUNDLE_1 );
        if ( dep1!=null ) properties.put( QDataSet.DEPEND_0, dep1 );
        if ( dep0!=null ) properties.put( QDataSet.DEPEND_1, dep0 );
        if ( bund1!=null ) properties.put( QDataSet.BUNDLE_0, bund1 );
        if ( bund0!=null ) properties.put( QDataSet.BUNDLE_1, bund0 );
    }

    public int rank() {
        return source.rank();
    }


    @Override
    public double value(int i0, int i1) {
        return source.value( i1, i0 );
    }

    @Override
    public Object property(String name) {
        Object v= properties.get(name);
        if ( v!=null ) {
            return v;
        } else {
            if ( DataSetUtil.isInheritedProperty(name) ) {
                return source.property(name);
            } else {
                return null;
            }
        }
    }

    @Override
    public Object property(String name, int i) {
        Object v= properties.get(name);
        if ( v!=null ) {
            return v;
        } else {
            if ( DataSetUtil.isInheritedProperty(name) ) {
                return source.property(name,i);
            } else {
                return null;
            }
        }
    }

    @Override
    public int length() {
        return source.length(0);
    }

    @Override
    public int length(int i) {
        return source.length();
    }
    
}
