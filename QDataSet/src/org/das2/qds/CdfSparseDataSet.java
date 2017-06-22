
package org.das2.qds;

import java.util.Arrays;
import org.das2.qds.ops.Ops;

/**
 * dataset for modeling when data values repeat.  Instead of storing 
 * copies of the data, the get method looks up the index.  For example:
 * <pre>
 * ds= CdfSparseDataSet(1,200)
 * ds.putValues( 0, dataset(1) )
 * ds.putValues( 10, dataset(2) )
 * ds.putValues( 110, dataset(3) )
 * plot( ds )
 * </pre>
 * @author faden@cottagesystems.com
 */
public class CdfSparseDataSet extends AbstractDataSet {

    int rank;
    int length;
    int[] indexes;
    QDataSet[] dss;
    int lastIndex;
            
    /**
     * create the DataSet with the given length.
     * @param rank the result rank.
     * @param length the result length.
     */
    public CdfSparseDataSet( int rank, int length ) {
        this.rank= rank;
        this.length= length;
        indexes= new int[] { length };
        dss= new QDataSet[] { null };
    }

    @Override
    public int rank() {
        return this.rank;
    }

    /**
     * insert these values into the dataset.
     * @param i0 the insertion index, these must be inserted in order.
     * @param ds the dataset for this index and those after.
     */
    public synchronized void putValues( int i0, QDataSet ds ) {
        if ( isImmutable() ) throw new IllegalArgumentException("dataset has been made immutable");
        if ( ds instanceof MutablePropertyDataSet ) {
            if ( !((MutablePropertyDataSet)ds).isImmutable() ) {
                ds= Ops.copy(ds);
            }
        }
        int ll= indexes.length;
        if ( ll<2 ) {
            if ( i0!=0 ) {
                throw new IllegalArgumentException("first insert must have insertion index 0.");
            }
        } else {
            if ( i0<indexes[ll-2] ) {
                throw new IllegalArgumentException("indexes must be inserted in order.");
            }
        }
        indexes= Arrays.copyOf( indexes, ll+1 );
        indexes[ll-1]= i0;
        indexes[ll]= length;
        dss= Arrays.copyOf( dss, ll+1 );
        dss[ll-1]= ds;
        dss[ll]= null;
    }
        
    /**
     * find the dataset index for this index.
     * @param i0 the index for the dataset.
     * @return the internal index.
     */
    private synchronized int indexInternal( int i0 ) {
        if ( lastIndex>=0 ) {
            if ( indexes[lastIndex]<=i0 && i0<indexes[lastIndex+1] ) {
                return lastIndex;
            }
        }
        int i= Arrays.binarySearch( indexes, i0 );
        if ( i<-1 ) { 
            lastIndex= -(i) - 2;
        } else if ( i>-1 ) {
            lastIndex= i;
        } else {
            throw new IllegalArgumentException("zeroth index must be specified.");
        }
        return lastIndex;
    }
    
    /**
     * return the number of different datasets.
     * @return the number of different datasets.
     */
    public int datasetCount() {
        return dss.length-1;
    }
    
    /**
     * allow clients to reset the length.
     * @param length 
     */
    public void setLength( int length ) {
        if ( isImmutable() ) throw new IllegalArgumentException("dataset has been made immutable");
        this.length= length;
    }
    
    @Override
    public int length() {
        return this.length;
    }

    @Override
    public int length(int i0) {
        int i= indexInternal(i0);
        return dss[i].length();
    }

    @Override
    public int length(int i0, int i1) {
        int i= indexInternal(i0);
        return dss[i].length(i1);
    }
    
    @Override
    public int length(int i0, int i1,int i2) {
        int i= indexInternal(i0);
        return dss[i].length(i1,i2);
    }

    @Override
    public double value(int i0) {
        int i= indexInternal(i0);
        return dss[i].value();
    }

    @Override
    public double value(int i0,int i1) {
        int i= indexInternal(i0);
        return dss[i].value(i1);
    }
    
    @Override
    public double value(int i0,int i1, int i2) {
        int i= indexInternal(i0);
        return dss[i].value(i1,i2);
    }

    @Override
    public double value(int i0,int i1, int i2, int i3 ) {
        int i= indexInternal(i0);
        return dss[i].value(i1,i2,i3);
    }

    @Override
    public QDataSet slice(int i0) {
        int i= indexInternal(i0);
        return dss[i];
    }
            
    @Override
    public QDataSet trim(int i0, int i1) {
        int ii0= indexInternal(i0);
        int ii1= indexInternal(i1);
        CdfSparseDataSet result= new CdfSparseDataSet(rank,i1-i0);
        if ( indexes[ii0]!=i0 ) {
            result.putValues(0,dss[ii0]);
        }
        for ( int ii=ii0+1; ii<=ii1; ii++ ) {
            result.putValues(indexes[ii]-i0,dss[ii]);
        }
        return result;
    }
}
