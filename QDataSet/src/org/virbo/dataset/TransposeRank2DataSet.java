/*
 * TransposeRank2DataSet.java
 *
 * Created on December 11, 2007, 10:19 PM
 */

package org.virbo.dataset;

/**
 * old dataset type transposes a rank 2 dataset with DEPEND_1 and DEPEND_0.
 * @author jbf
 */
public class TransposeRank2DataSet extends AbstractDataSet {
    
    QDataSet source;
    
    /** Creates a new instance of TransposeRank2DataSet */
    public TransposeRank2DataSet( QDataSet source ) {
        this.source= source;
        QDataSet dep1=  (QDataSet) source.property( QDataSet.DEPEND_1 );
        if ( dep1==null ) dep1= new IndexGenDataSet( source.length(0) ); // This is necessary, because otherwise DEPEND_0 gets plugged in.
        properties.put( QDataSet.DEPEND_0, dep1 );
        properties.put( QDataSet.DEPEND_1, source.property( QDataSet.DEPEND_0 ) );
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
