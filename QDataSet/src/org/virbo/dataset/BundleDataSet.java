/*
 * BundleDataSet.java
 *
 * Copied from JoinDataSet in June 2009
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.ArrayList;
import java.util.List;

/**
 * create a higher rank dataset with dim 1 being a bundle dimension.  Each
 * dataset must have the same length.
 *
 * Note this was created before BUNDLE_1 and bundle descriptor datasets were
 * introduced, so this code is suspect.  TODO: review and ensure compatibility
 * with updates to bundle dataset semantics.
 *
 * @author jbf
 */
public class BundleDataSet extends AbstractDataSet {
    
    List<QDataSet> datasets;
    /**
     * rank of the dataset.
     */
    int rank;

    /**
     * the length of each dataset.
     */
    int len0=-1;

    /**
     * return a BundleDataSet for bundling rank 0 datasets.  The result
     * will be a rank 1 datasets with BUNDLE_0 non-null.
     * @return a rank 1 BundleDataSet
     */
    public static BundleDataSet createRank0Bundle() {
        return new BundleDataSet(1);
    }

    /**
     * return a BundleDataSet for bundling rank 1 datasets.  The result
     * will be a rank 2 datasets with BUNDLE_1 non-null.
     * @return a rank 2 BundleDataSet
     */
    public static BundleDataSet createRank1Bundle() {
        return new BundleDataSet(2);
    }

    /** Creates a new instance of BundleDataSet that accepts rank 1 datasets.
     * (Rank 2 and up are not yet supported.)
     */
    public BundleDataSet( ) {
        this(2);
    }

    /** Creates a new instance of BundleDataSet with the given rank.  Rank 1
     * datasets can bundle rank 0 datasets, while rank 2 can only bundle
     * rank 1 datasets with the same depend_0.
     */
    public BundleDataSet( int rank ) {
        if ( rank>2 ) throw new IllegalArgumentException("only rank 1 and 2 are supported.");
        this.rank= rank;
        datasets= new ArrayList<QDataSet>();
        if ( rank==2 ) {
            putProperty( QDataSet.BUNDLE_1, new BundleDescriptor() );
            putProperty( QDataSet.QUBE, Boolean.TRUE );
        } else {
            putProperty( QDataSet.BUNDLE_0, new BundleDescriptor() );
        }

    }
    /**
     * add the dataset to the bundle of datasets.  Currently this implementation only supports rank N-1 datasets (N is this
     * dataset's rank), but the QDataSet spec allows for qube datasets of any rank>1 to be bundled.  This limitation will be removed
     * in a future version.  (Note QDataSet changes http://autoplot.org/QDataSet#2011-Apr-13)
     * 
     * @param ds
     */
    public void bundle( QDataSet ds ) {
        if ( ds.rank()!=this.rank-1 ) throw new IllegalArgumentException("dataset rank must be "+(this.rank-1));
        if ( this.rank>1 ) {
            if ( len0==-1 ) {
                len0= ds.length();
            } else {
                if ( ds.length()!=len0 ) throw new IllegalArgumentException("dataset length is not consistent with the bundle.");
            }
        }
        datasets.add( ds );
    }

    /**
     * allow to simply unbundle the dataset.
     * @param i
     * @return
     */
    QDataSet unbundle(int i) {
        return datasets.get(i);
    }

    public class BundleDescriptor extends AbstractDataSet {

        public int rank() {
            return 2;
        }

        @Override
        public int length() {
            return datasets.size();
        }

        @Override
        public int length(int i) {
            return 0;  // support bundling just rank N-1 datasets.
        }

        @Override
        public Object property(String name, int i) {
            Object v= properties.get( name+"__"+i );
            if ( i>=datasets.size() ) {
                throw new IndexOutOfBoundsException("No dataset at index " + i + " only " + datasets.size() +" datasets." );
            }
            if ( v==null ) {
                return datasets.get(i).property(name);
            } else {
                return v;
            }
        }

        @Override
        public double value(int i0, int i1) {
            // support bundling just rank N-1 datasets.  to support higher rank
            // datasets, this should return the qube dims.
            throw new IndexOutOfBoundsException("length=0");
        }

    }

    public int rank() {
        return rank;
    }

    public double value(int i0) {
        return datasets.get(i0).value();
    }

    public double value(int i0, int i1) {
        return datasets.get(i1).value(i0);
    }

    public Object property(String name, int i0) {
        if ( rank==1 ) {
            return datasets.get(i0).property(name);
        } else {
            return super.property(name);
        }
    }

    public Object property(String name, int i0, int i1) {
        return datasets.get(i0).property(name,i1);
    }

    public int length() {
        return this.rank==2 ? len0 : datasets.size();
    }

    public int length(int i0) {
        return datasets.size();
    }
    
    public String toString() {
        if ( rank==1 ) {
            return DataSetUtil.format(this);
        } else {
             if ( datasets.size()>4 ) {
                return "BundleDataSet["+datasets.size()+" datasets: "+ datasets.get(0)+", "+datasets.get(1)+", ...]";
            } else {
                return "BundleDataSet["+datasets.size()+" datasets: "+ datasets +" ]";
            }
        }
    }

}
