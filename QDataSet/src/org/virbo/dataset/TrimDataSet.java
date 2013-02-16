/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * Implements Trim operation by wrapping dataset.
 * Supports rank 2 depend_1 datasets.  Supports CONTEXT_0, DataSetUtil.correlativeProperties()
 * @author jbf
 */
public class TrimDataSet extends AbstractDataSet {

    final int offset;
    final int len;
    final QDataSet ds;

    public TrimDataSet(QDataSet ds, int start, int stop) {

     //TODO: uncomment and test this.
     //   if ( ds instanceof TrimDataSet ) {
     //       TrimDataSet trds= ((TrimDataSet)ds);
     //       this.ds= trds.ds;
     //       this.offset= trds.offset + start;
     //       this.len= stop-start;
     //   } else {
            this.ds = ds;
            this.offset = start;
            this.len = stop - start;
     //   }

        QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
        if (dep0 != null) {
            putProperty(QDataSet.DEPEND_0, new TrimDataSet( dep0, start, stop ) );
        }

        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( dep1!=null && dep1.rank()==2 ) {
                putProperty( QDataSet.DEPEND_1, new TrimDataSet( dep1, start, stop ) );
        }

        for ( int i=0; i<QDataSet.MAX_PLANE_COUNT; i++ ) {
            String prop= "PLANE_"+i;
            QDataSet plane0= (QDataSet) ds.property( prop );
            if ( plane0!=null ) {
                if ( plane0.rank()<1 ) {
                    putProperty( prop, plane0 );
                } else {
                    putProperty( prop, new TrimDataSet( plane0, start, stop ) );
                }
            } else {
                break;
            }
        }

        String[] p= DataSetUtil.correlativeProperties();

        for ( int i=0; i<p.length; i++ ) {
            QDataSet delta= (QDataSet) ds.property( p[i] );
            if ( delta!=null && delta.rank()>0 ) {
                putProperty( p[i], new TrimDataSet(delta,start,stop) );
            }
        }
        
    }

    public int rank() {
        return ds.rank();
    }

    @Override
    public double value(int i) {
        return ds.value(offset + i);
    }

    @Override
    public double value(int i0, int i1) {
        return ds.value(i0 + offset, i1);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return ds.value(i0 + offset, i1, i2);
    }

    public Object property(String name) {
        Object p= super.property( name );
        if ( p!=null ) return p;
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return ds.property(name);
        } else {
            return null;
        }
    }

    public Object property(String name, int i) {
        Object p= super.property( name, i );
        if ( p!=null ) return p;
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return (p != null) ? p : ds.property(name, i + offset);
        } else {
            return null;
        }
    }

    public int length() {
        return len;
    }

    public int length(int i0) {
        return ds.length(offset + i0);
    }

    public int length(int i0, int i1) {
        return ds.length(offset + i0, i1);
    }

    @Override
    public QDataSet slice(int i) {
        return new Slice0DataSet( ds, offset + i );
    }

    @Override
    public QDataSet trim(int start, int end) {
        if ( end>=len ) throw new IllegalArgumentException("end>len");
        if ( start<0 ) throw new IllegalArgumentException("start<0" );
        return new TrimDataSet( ds, start+offset, start+offset+end );
    }


}
