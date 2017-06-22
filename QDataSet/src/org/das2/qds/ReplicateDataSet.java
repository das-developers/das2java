
package org.das2.qds;

/**
 * repeats a dataset n times.  The result has the first index being the
 * repeat index.
 * 
 * @author jbf
 */
public class ReplicateDataSet extends AbstractDataSet {
    int len0;
    QDataSet source;
    
    public ReplicateDataSet( QDataSet s, int len0 ) {
        this.len0= len0;
        this.source= s;
        if ( s.rank()>3 ) {
            throw new IllegalArgumentException("rank must be less than 4");
        }
    }
    
    @Override
    public int rank() {
        return source.rank()+1;
    }
    
    @Override
    public double value(int il) {
        return source.value();
    }

    @Override
    public double value(int i0, int i1) {
        return source.value(i1);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return source.value(i1, i2);
    }
    
    @Override
    public double value(int i0, int i1, int i2, int i3 ) {
        return source.value(i1, i2, i3);
    }
    
    @Override
    public int length() {
        return len0;
    }
    @Override
    public int length(int i) {
        return source.length();
    }
    @Override
    public int length(int i0, int i1 ) {
        return source.length(i1);
    }
    @Override
    public int length(int i0, int i1, int i2 ) {
        return source.length(i1,i2);
    }

    @Override
    public Object property(String name) {
        if ( name.equals("DEPEND_0" ) ) {
            return super.property(name);
        } else if ( name.matches("DEPEND_\\d") ) {
            return source.property("DEPEND_"+ ( Integer.parseInt(name.substring(7))-1 ) );
        } else if ( name.matches("BINS_\\d" ) ) {
            return source.property("BINS_"+ ( Integer.parseInt(name.substring(5))-1 ) );
        } else if ( name.matches("BUNDLE_\\d" ) ) {
            return source.property("BUNDLE_"+ ( Integer.parseInt(name.substring(7))-1 ) );
        } else {
            Object o= super.property(name);
            return o==null ? source.property(name) : o;
        }
    }

    @Override
    public Object property(String name, int i) {
        return source.property(name);
    }

    
}
