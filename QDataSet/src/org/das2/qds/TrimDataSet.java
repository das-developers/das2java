/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

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

        if ( ds.rank()>4 ) {
            throw new IllegalArgumentException("rank>4 not supported");
        }
        if ( ds.rank()<1 ) {
            throw new IllegalArgumentException("trim called on a rank 0 dataset");
        }
        
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
        
        QDataSet bundle0= (QDataSet) ds.property(QDataSet.BUNDLE_0);
        if (bundle0 != null) {
            putProperty(QDataSet.BUNDLE_0, new TrimDataSet( bundle0, start, stop ) );
        }

        QDataSet dep1= (QDataSet) ds.property(QDataSet.DEPEND_1);
        if ( dep1!=null && dep1.rank()==2 ) {
            putProperty( QDataSet.DEPEND_1, new TrimDataSet( dep1, start, stop ) );
        } else {
            if ( dep1!=null ) putProperty( QDataSet.DEPEND_1, dep1 );
        }

        QDataSet dep2= (QDataSet) ds.property(QDataSet.DEPEND_2);
        if ( dep2!=null && dep2.rank()==2 ) {
            putProperty( QDataSet.DEPEND_2, new TrimDataSet( dep2, start, stop ) );
        } else {
            if ( dep2!=null ) putProperty( QDataSet.DEPEND_2, dep2 );
        }
        
        QDataSet dep3= (QDataSet) ds.property(QDataSet.DEPEND_3);
        if ( dep3!=null && dep3.rank()==2 ) {
            putProperty( QDataSet.DEPEND_3, new TrimDataSet( dep3, start, stop ) );
        } else {
            if ( dep3!=null ) putProperty( QDataSet.DEPEND_3, dep3 );
        }        
        
        QDataSet bds1= (QDataSet) ds.property(QDataSet.BUNDLE_1);
        if ( bds1!=null ) {
            putProperty( QDataSet.BUNDLE_1, bds1 );
        }

        QDataSet bds2= (QDataSet) ds.property(QDataSet.BUNDLE_2);
        if ( bds2!=null ) {
            putProperty( QDataSet.BUNDLE_2, bds2 );
        }

        QDataSet bds3= (QDataSet) ds.property(QDataSet.BUNDLE_3);
        if ( bds3!=null ) {
            putProperty( QDataSet.BUNDLE_3, bds3 );
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

        for ( String p1 : p ) {
            QDataSet delta = (QDataSet) ds.property(p1);
            if (delta!=null && delta.rank()>0) {
                putProperty(p1, new TrimDataSet(delta,start,stop));
            }
        }
        
    }

    @Override
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

    @Override
    public double value(int i0, int i1, int i2, int i3 ) {
        return ds.value(i0 + offset, i1, i2, i3 );
    }

    @Override
    public Object property(String name) {
        Object p= super.property( name );
        if ( p!=null ) return p;
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return ds.property(name);
        } else {
            return null;
        }
    }

    @Override
    public Object property(String name, int i) {
        Object p= super.property( name, i );
        if ( p!=null ) return p;
        if ( DataSetUtil.isInheritedProperty(name) ) {
            return ds.property(name, i + offset);
        } else {
            return null;
        }
    }

    @Override
    public int length() {
        return len;
    }

    @Override
    public int length(int i0) {
        return ds.length(offset + i0);
    }

    @Override
    public int length(int i0, int i1) {
        return ds.length(offset + i0, i1);
    }

    @Override
    public int length(int i0, int i1, int i2 ) {
        return ds.length(offset + i0, i1, i2);
    }
    
    @Override
    public QDataSet slice(int i) {
        return new Slice0DataSet( ds, offset + i );
    }

    @Override
    public QDataSet trim(int start, int end) {
        if ( start==0 && end==len ) {
            return this;
        }
        if ( true ) {
            if ( start>len ) throw new IndexOutOfBoundsException("start="+start+" > "+len );
            if ( start<0 ) throw new IndexOutOfBoundsException("start="+start+" < 0");
            if ( end>len ) throw new IndexOutOfBoundsException("end="+end+" > "+len );
            if ( end<0 ) throw new IndexOutOfBoundsException("end="+end+" < 0");
            if ( start>end ) throw new IllegalArgumentException("trim called with start>end: "+start +">"+end);
        }        
        return new TrimDataSet( ds, start+offset, end+offset );
    }


}
