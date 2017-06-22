/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.qds;

/**
 * Wraps rank N qube dataset to present a dataset with the same rank that is a subset of
 * wrapped dataset.
 *
 * Note this was used before trim() was a native operator for datasets, and it
 * should be used when the zeroth dimension is trimmed with stride==1.
 * @author jbf
 */
public class TrimStrideWrapper extends AbstractDataSet {
    QDataSet ds;
    int[] offset;
    int[] len;
    int[] stride;
    int[] qube;
    
    /**
     * construct a wrapper with no trimming by default.  setTrim is
     * called to trim a dimension.
     * 
     * @param ds the source dataset backing up the trimmed one.
     */
    public TrimStrideWrapper( QDataSet ds ) {
        this.ds= ds;
        len= DataSetUtil.qubeDims(ds);
        qube= DataSetUtil.qubeDims(ds);
        offset= new int[ds.rank()];
        stride= new int[ds.rank()];
        for ( int i=0; i<ds.rank(); i++ ) stride[i]= 1;
        if ( DataSetUtil.isQube(ds) ) {
            this.putProperty( QDataSet.QUBE, Boolean.TRUE );
        }
        DataSetUtil.copyDimensionProperties( ds, this );
    }

    /**
     * trim the dimension.  Null indexes indicate the default should be 
     * used.  Negative indeces are relative to the length of the dimension.
     * @param dim the index, between 0 and rank.
     * @param start index to start from, null for 0, or negative.
     * @param stop exclusive index to end from, null for qube[dim], or negative.
     * @param stride the step size, or null for 1.  TODO: I doubt this can be negative, but this needs verification.
     */
    public void setTrim( int dim, Number start, Number stop, Number stride ) {
        if ( isImmutable() ) {
            throw new IllegalArgumentException("data set is immutable");
        }
        int sstart= start == null ? 0 : start.intValue();
        this.stride[dim]= stride == null ? 1 : stride.intValue();
        int sstop= stop==null ? qube[dim] : stop.intValue();
        if ( sstop>qube[dim] ) {
            throw new IndexOutOfBoundsException("stop is greater than qube dimension: "+stop + ">" + qube[dim]);
        }
        if ( sstop<0 ) sstop= qube[dim] + sstop; // danger
        if ( sstart<0 ) sstart= qube[dim] + sstart;
        this.offset[dim]= sstart;
        this.len[dim]= (int)Math.ceil ( 1.*( sstop - sstart ) / this.stride[dim] );
        QDataSet dep= (QDataSet) ds.property("DEPEND_"+dim);
        if ( dep!=null && dep.rank()==1 ) {
            TrimStrideWrapper depw= new TrimStrideWrapper( dep );
            depw.setTrim( 0, start, stop, stride );
            putProperty( "DEPEND_"+dim, depw );
        } else if (  dep!=null && dep.rank()==2 ) {
            if ( dim==0 ) {
                TrimStrideWrapper depw= new TrimStrideWrapper( dep );
                depw.setTrim( 0, start, stop, stride );
                putProperty( "DEPEND_"+dim, depw );
            } else {
                TrimStrideWrapper depw= new TrimStrideWrapper( dep ); //TODO: verify this.
                depw.setTrim( 1, start, stop, stride );
                putProperty( "DEPEND_"+dim, depw );
            }
        }
        
        for ( int i=0; i<QDataSet.MAX_RANK; i++ ) {
            if ( i!=dim ) {
                QDataSet depi= (QDataSet) ds.property("DEPEND_"+i);
                if ( depi!=null ) {
                    putProperty( "DEPEND_"+i, depi );
                }
                depi= (QDataSet) ds.property("BUNDLE_"+i);
                if ( depi!=null ) {
                    putProperty( "BUNDLE_"+i, depi );
                }
                depi= (QDataSet) ds.property("BINS_"+i);
                if ( depi!=null ) {
                    putProperty( "BINS_"+i, depi );
                }
            }
        }
    }

    @Override
    public int rank() {
        return ds.rank();
    }
    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return ds.value( offset[0]+stride[0]*i0, offset[1]+stride[1]*i1, offset[2]+stride[2]*i2, offset[3]+stride[3]*i3 );
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return ds.value( offset[0]+stride[0]*i0, offset[1]+stride[1]*i1, offset[2]+stride[2]*i2 );
    }

    @Override
    public double value(int i0, int i1) {
        return ds.value( offset[0]+stride[0]*i0, offset[1]+stride[1]*i1 );
    }

    @Override
    public double value(int i0) {
        return ds.value( offset[0]+stride[0]*i0 );
    }

    @Override
    public int length(int i, int j, int k ) {
        return len[3];
    }

    @Override
    public int length(int i, int j) {
        return len[2];
    }

    @Override
    public int length(int i) {
        return len[1];
    }

    @Override
    public int length() {
        return len[0];
    }

    @Override
    public Object property(String name) {
        if ( name.startsWith("PLANE_" ) ) {
            QDataSet plane= (QDataSet) ds.property(name);
            if ( plane==null ) return null;
            TrimStrideWrapper wrap= new TrimStrideWrapper(plane);
            wrap.setTrim(0, offset[0], len[0], stride[0] );
            return wrap;
        } else {
            return super.property(name);
        }
    }


    
}
