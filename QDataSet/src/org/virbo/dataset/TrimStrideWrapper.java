/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 * Wraps rank N dataset to present a dataset with the same rank that is a subset of
 * wrapped dataset.
 *
 * Note this was used before trim() was a native operator for datasets, and it
 * should be used when stride==1.
 * @author jbf
 */
public class TrimStrideWrapper extends AbstractDataSet {
    QDataSet ds;
    int[] offset;
    int[] len;
    int[] stride;
    int[] qube;
    
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

    public void setTrim( int dim, Number start, Number stop, Number stride ) {
        int sstart= start == null ? 0 : start.intValue();
        this.stride[dim]= stride == null ? 1 : stride.intValue();
        int sstop= stop==null ? qube[dim] : stop.intValue();
        if ( sstop<0 ) sstop= qube[dim] + sstop; 
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
    }

    @Override
    public int rank() {
        return ds.rank();
    }
    public double value(int i0, int i1, int i2, int i3) {
        return ds.value( offset[0]+stride[0]*i0, offset[1]+stride[1]*i1, offset[2]+stride[2]*i2, offset[3]+stride[3]*i3 );
    }

    public double value(int i0, int i1, int i2) {
        return ds.value( offset[0]+stride[0]*i0, offset[1]+stride[1]*i1, offset[2]+stride[2]*i2 );
    }

    public double value(int i0, int i1) {
        return ds.value( offset[0]+stride[0]*i0, offset[1]+stride[1]*i1 );
    }

    public double value(int i0) {
        return ds.value( offset[0]+stride[0]*i0 );
    }

    public int length(int i, int j, int k ) {
        return len[3];
    }

    public int length(int i, int j) {
        return len[2];
    }

    public int length(int i) {
        return len[1];
    }

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
