
package org.das2.qds;

/**
 * efficient dataset that has no properties and one value.  This can be used as a marker
 * interface too, where the client can inspect the type and just get the valid value once.
 * See also ReplicateDataSet and TagGenDataSet
 * @author jbf
 */
public final class ConstantDataSet implements QDataSet {

    /**
     * create a rank 1 dataset of the given length.
     * @param len
     * @param value 
     */
    public ConstantDataSet( int len, double value ) {
        this( 1, len, 1, 1, 1, value );
    }
    
    /**
     * create a constant dataset with the value and having the dimensions
     * given in qube.
     * @param value the value for the dataset.
     * @param qube number of elements in each dimension
     * @return the dataset.
     */
    public static ConstantDataSet create( double value, int[] qube ) {
        switch ( qube.length ) {
            case 0: return new ConstantDataSet(0,1,1,1,1,value);
            case 1: return new ConstantDataSet(1,qube[0],1,1,1,value);
            case 2: return new ConstantDataSet(2,qube[0],qube[1],1,1,value);
            case 3: return new ConstantDataSet(3,qube[0],qube[1],qube[2],1,value);
            case 4: return new ConstantDataSet(4,qube[0],qube[1],qube[2],qube[3],value);
            default: throw new IllegalArgumentException("unsupported rank: "+qube.length);
        }
    }
    
    private ConstantDataSet( int rank, int len0, int len1, int len2, int len3, double value ) {
        this.rank= rank;
        this.len0= len0;
        this.len1= len1;
        this.len2= len2;
        this.len3= len3;
        this.value= value;
    }
    
    int rank;
    int len0;
    int len1;
    int len2;
    int len3;
    double value;
    
    @Override
    public int rank() {
        return rank;
    }

    @Override
    public double value() {
        return value;
    }

    @Override
    public String svalue() {
        return String.valueOf(value);
    }

    @Override
    public double value(int i0) {
        return value;
    }

    @Override
    public double value(int i0, int i1) {
        return value;
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return value;
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        return value;
    }

    @Override
    public Object property(String name) {
        return null;
    }

    @Override
    public Object property(String name, int i) {
        return null;
    }

    @Override
    public int length() {
        return len0;
    }
    
    @Override
    public int length(int i) {
        return len1;
    }

    @Override
    public int length(int i, int j) {
        return len2;
    }

    @Override
    public int length(int i, int j, int k) {
        return len3;
    }

    @Override
    public QDataSet slice(int i) {
        return new ConstantDataSet( rank-1, len1, len2, len3, 1, value );
    }

    @Override
    public QDataSet trim(int start, int end) {
        return new ConstantDataSet( rank-1, end-start, len1, len2, len3, value );
    }

    @Override
    public <T> T capability(Class<T> clazz) {
        return null;
    }
    
}
