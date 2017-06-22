
package org.das2.qds;

/**
 * Increase the rank by repeating at any of the 4 indeces.  ReplicateDataSet
 * only allows repeats on the zeroth index, while this allows any index.
 * @author faden@cottagesystems.com
 */
public final class RepeatIndexDataSet extends AbstractDataSet {

    QDataSet source;
    
    int count;
    int insertIndex;
    
    /**
     * Create the dataset.
     * @param s the source.
     * @param insertIndex the index to insert.
     * @param length the number of times to repeat
     */
    public RepeatIndexDataSet( QDataSet s, int insertIndex, int length ) {
        this.source= s;
        if ( s.rank()>3 ) {
            throw new IllegalArgumentException("rank must be less than 4");
        }
        this.count= length;
        this.insertIndex= insertIndex;
        if ( DataSetUtil.isQube(s) ) {
            this.putProperty( QDataSet.QUBE, Boolean.TRUE );
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
        if ( insertIndex==1 ) {
            return source.value(i0);
        } else {
            return source.value(i1);
        }
    }

    @Override
    public double value(int i0, int i1, int i2) {
        assert insertIndex>=0;
        assert insertIndex<4;
        switch ( insertIndex ) {
            case 0:
                return source.value(i1,i2);
            case 1:
                return source.value(i0,i2);
            case 2:
                return source.value(i0,i1);
        }
        throw new RuntimeException("implementation error");
    }
    
    @Override
    public double value(int i0, int i1, int i2, int i3 ) {
        assert insertIndex>=0;
        assert insertIndex<4;
        switch ( insertIndex ) {
            case 0:
                return source.value(i1,i2,i3);
            case 1:
                return source.value(i0,i2,i3);
            case 2:
                return source.value(i0,i1,i3);
            case 3:
                return source.value(i0,i1,i2);
        }
        throw new RuntimeException("implementation error");
    }
    
    @Override
    public int length() {
        if ( insertIndex==0 ) {
            return count;
        } else {
            return source.length();
        }
    }
    
    @Override
    public int length(int i) {
        switch ( insertIndex ) {
            case 0:
                return source.length();
            case 1:
                return count;
            case 2:
                return source.length(i);                
            case 3:
                return source.length(i);
        }
        throw new RuntimeException("implementation error");
    }
    
    @Override
    public int length(int i0, int i1 ) {
        switch ( insertIndex ) {
            case 0:
                return source.length(i0);
            case 1:
                return source.length(i0);
            case 2:
                return count;
            case 3:
                return source.length(i0,i1);
        }
        throw new RuntimeException("implementation error");
    }
    
    @Override
    public int length(int i0, int i1, int i2 ) {
        assert insertIndex>=0;
        assert insertIndex<4;
        switch ( insertIndex ) {
            case 0:
                return source.length(i1,i2);
            case 1:
                return source.length(i0,i2);
            case 2:
                return source.length(i0,i1);
            case 3:
                return count;
        }
        throw new RuntimeException("implementation error");
    }

    @Override
    public Object property(String name) {
        Object o= super.property(name);
        return o==null ? source.property(name) : o;
    }

    @Override
    public Object property(String name, int i) {
        Object o= super.property(name);
        if ( o!=null ) return o;
        if ( insertIndex==0 ) {
            return source.property(name);
        } else {
            return source.property(name,i);
        }
    }

}
