/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dataset;

/**
 *
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
        putProperty( QDataSet.NAME, ds.property(QDataSet.NAME ) );
        putProperty( QDataSet.UNITS, ds.property(QDataSet.UNITS ) );
        putProperty( QDataSet.FILL_VALUE, ds.property(QDataSet.FILL_VALUE) );
        putProperty( QDataSet.VALID_MIN, ds.property(QDataSet.VALID_MIN) );
        putProperty( QDataSet.VALID_MAX, ds.property(QDataSet.VALID_MAX) );
        
    }

    public void setTrim( int dim, Integer start, Integer stop, Integer stride ) {
        this.offset[dim]= start == null ? 0 : start;
        this.stride[dim]= stride == null ? 1 : stride;
        int sstop= stop==null ? qube[dim] : stop;
        this.len[dim]= ( sstop - this.offset[dim] ) / this.stride[dim];
        QDataSet dep= (QDataSet) ds.property("DEPEND_"+dim);
        if ( dep!=null && dep.rank()==1 ) {
            TrimStrideWrapper depw= new TrimStrideWrapper( dep );
            depw.setTrim( 0, start, stop, stride );
            putProperty( "DEPEND_"+dim, depw );
        }
    }
    
    @Override
    public int rank() {
        return ds.rank();
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

    public int length(int i, int j) {
        return len[2];
    }

    public int length(int i) {
        return len[1];
    }

    public int length() {
        return len[0];
    }
    
}
