
package org.das2.qds;

import java.util.Map;
import org.das2.qds.ops.Ops;

/**
 * Join of WritableDataSets where each dataset is writable.  Note type checking is
 * only done when the constructor that accepts a dataset, otherwise type checking
 * is not done until the data is accessed.  This was introduced to properly
 * support Ops.copy.
 * @author jbf
 */
public class WritableJoinDataSet extends JoinDataSet implements WritableDataSet {

    public WritableJoinDataSet( int rank ) {
        super(rank);
    }

    public WritableJoinDataSet( QDataSet ds ) {
        super(ds);
        if ( !( ds instanceof WritableDataSet ) ) {
            throw new IllegalArgumentException("dataset must be writable: "+ds );
        }
    }

    /**
     * create a copy of the dataset src, which can be a join of qubes.
     * Note this assumes that each slice is a qube, which was not asserted until now.
     * @param src
     * @return 
     */
    public static WritableDataSet copy(QDataSet src) {
        WritableJoinDataSet result= new WritableJoinDataSet( src.rank() );
        for ( int i=0; i<src.length(); i++ ) {
            QDataSet ds1= src.slice(i);
            if ( !DataSetUtil.isQube(ds1) ) {
                if ( ds1 instanceof JoinDataSet ) {
                    result.join( copy( ds1 ) );
                } else {
                    throw new IllegalArgumentException("src contains slices that are not qubes.");
                }
            }
            result.join( Ops.copy(ds1) );
        }
        Map<String,Object> props= Ops.copyProperties(src);
        for ( Map.Entry<String,Object> en: props.entrySet() ) {
            result.putProperty( en.getKey(), en.getValue() );
        }
        return result;

    }

    @Override
    public void putValue(double d) {
        throw new IllegalArgumentException("rank error, expected "+rank());
    }

    @Override
    public void putValue(int i0, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(d);
    }

    @Override
    public void putValue(int i0, int i1, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(i1,d);
    }

    @Override
    public void putValue(int i0, int i1, int i2, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(i1,i2,d);
    }

    @Override
    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ((WritableDataSet)datasets.get(i0)).putValue(i1,i2,i3,d);
    }

}
