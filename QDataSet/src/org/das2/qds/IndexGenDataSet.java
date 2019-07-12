
package org.das2.qds;

/**
 * Dataset that simply returns the index as the value.  
 * @author jbf
 */
public final class IndexGenDataSet extends AbstractDataSet {
    
    int length;
    
    /** 
     * Creates a new instance of IndexGenDataSet
     * @param length 
     */
    public IndexGenDataSet( int length ) {
        super();
        this.length= length;
        properties.put( QDataSet.MONOTONIC, Boolean.TRUE );
        properties.put( QDataSet.FORMAT, "%d" );
    }

    @Override
    public int rank() {
        return 1;
    }

    @Override
    public double value(int i) {
        return i;
    }

    @Override
    public int length() {
        return length;
    }

    /**
     * this is used to create a 0,1,2,... index list when DEPEND_1 is rank 2,
     * but this also works for rank 1.
     * @param src
     * @return 
     */
    public static QDataSet lastindex( final QDataSet src ) {
        if ( src.rank()==1 ) {
            return new IndexGenDataSet(src.length());
        }
        AbstractDataSet result= new AbstractDataSet() {
            @Override
            public int rank() {
                return src.rank();
            }

            @Override
            public double value(int i0) {
                return i0;
            }

            @Override
            public double value(int i0, int i1) {
                return i1;
            }

            @Override
            public int length() {
                return src.length();
            }

            @Override
            public int length(int i) {
                return src.length(i);
            }
            
        };
        result.putProperty( QDataSet.FORMAT,  "%d" );
        return result;
    }
}
