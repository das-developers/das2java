/*
 * BundleDataSet.java
 *
 * Copied from JoinDataSet in June 2009
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.das2.qds;

import java.util.ArrayList;
import java.util.List;

/**
 * create a high rank dataset the last dimension being the bundle.  Each
 * dataset must have the same length.
 *
 * Modification History:
 *   2015-10-30: copied from BundleDataSet
 *   See https://sourceforge.net/p/autoplot/feature-requests/267/
 * @author jbf
 */
public final class TailBundleDataSet extends AbstractDataSet {
    
    List<QDataSet> datasets;
    
    /**
     * rank of the dataset.
     */
    int rank;
    
    /**
     * dimensions of each dataset.
     */
    int[] qube;

    /** Creates a new instance of BundleDataSet with the given rank.  Rank 1
     * datasets can bundle rank 0 datasets, while rank 2 can only bundle
     * rank 1 datasets with the same depend_0.
     * @param rank rank of the bundle.
     */
    public TailBundleDataSet( int rank ) {
        this.rank= rank;
        datasets= new ArrayList<QDataSet>();
        if ( rank==4 ) {
            putProperty( QDataSet.BUNDLE_3, new BundleDescriptor() );
            putProperty( QDataSet.QUBE, Boolean.TRUE );            
        } else if ( rank==3 ) {
            putProperty( QDataSet.BUNDLE_2, new BundleDescriptor() );
            putProperty( QDataSet.QUBE, Boolean.TRUE );            
        } else if ( rank==2 ) {
            putProperty( QDataSet.BUNDLE_1, new BundleDescriptor() );
            putProperty( QDataSet.QUBE, Boolean.TRUE );
        } else {
            putProperty( QDataSet.BUNDLE_0, new BundleDescriptor() );
        }

    }

    /**
     * create a bundle with the first dataset.  The result will have 
     * rank N+1 where ds has rank N.
     * @param ds rank N dataset.
     */
    public TailBundleDataSet( QDataSet ds ) {
        this( ds.rank()+1 );
        bundle(ds);
    }

    /**
     * add the dataset to the bundle of datasets.  Currently this implementation only supports rank N-1 datasets (N is this
     * dataset's rank), but the QDataSet spec allows for qube datasets of any rank&gt;1 to be bundled.  This limitation will be removed
     * in a future version.  (Note QDataSet changes http://autoplot.org/QDataSet#2011-Apr-13)
     * 
     * @param ds
     */
    public void bundle( QDataSet ds ) {
        if ( ds.rank()!=this.rank-1 ) {
            throw new IllegalArgumentException("dataset rank must be "+(this.rank-1));
        }
        if ( this.rank>1 ) {
            if ( this.qube!=null ) {
                int[] qube= DataSetUtil.qubeDims(ds);
                if ( qube[0]!=this.qube[0] ) {
                    throw new IllegalArgumentException( String.format( "dataset length (%d) is not consistent with the bundle's length (%d)", ds.length(), this.qube[0]) );
                }
            } else {
                this.qube= DataSetUtil.qubeDims(ds);
            }
        }
        datasets.add( ds );
    }

    /**
     * allow to simply unbundle the dataset.
     * @param i the index.
     * @return the dataset at i.
     */
    public QDataSet unbundle(int i) {
        return datasets.get(i);
    }

    /**
     * special dataset describing the bundled datasets in a BundleDataSet.
     */
    public class BundleDescriptor extends AbstractDataSet {

        @Override
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
                if ( DataSetUtil.isInheritedProperty(name) ) { // UNITS, LABEL, TITLE, etc.
                    return datasets.get(i).property(name);
                } else {
                    // the property DEPENDNAME_0 should be used in a bundle descriptor.  This property should not
                    // be here, because we must be able to transfer BUNDLE_1 to BUNDLE_0.
                    return null;
                }
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

        @Override
        public String toString() {
            StringBuilder names= new StringBuilder();
            String s= (String) datasets.get(0).property(QDataSet.NAME);
            names.append( s==null ? "data" : s );
            for ( int i=1; i<datasets.size(); i++ ) {
                s= (String)datasets.get(i).property(QDataSet.NAME);
                names.append(",").append( s==null ? "data" : s );
            }
            return "BundleDescriptor[ "+names.toString()+"]";
        }
    }

    @Override
    public int rank() {
        return rank;
    }

    @Override
    public double value(int i0) {
        if ( this.rank!=1 ) {
            throw new IllegalArgumentException("rank 1 access on rank "+this.rank+" bundle dataset");
        }
        return datasets.get(i0).value();
    }

    @Override
    public double value(int i0, int i1) {
        if ( this.rank!=2 ) {
            throw new IllegalArgumentException("rank 2 access on rank "+this.rank+" bundle dataset");
        }
        return datasets.get(i1).value(i0);
    }

    @Override
    public double value(int i0, int i1, int i2) { // experimental
        if ( this.rank!=3 ) {
            throw new IllegalArgumentException("rank 3 access on rank "+this.rank+" bundle dataset");
        }
        return datasets.get(i2).value(i0,i1);
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        if ( this.rank!=4 ) {
            throw new IllegalArgumentException("rank 4 access on rank "+this.rank+" bundle dataset");
        }        
        return datasets.get(i3).value(i0,i1,i2);
    }

    @Override
    public Object property(String name, int i0) {
        if ( rank==1 ) {
            return datasets.get(i0).property(name);
        } else {
            Object result= super.property(name);
            if ( result==null ) {
                return null;
            } else {
                return result;
            }
        }
    }

    @Override
    public int length() {
        if ( this.rank==1 ) {
            return datasets.size();
        } else {
            return qube[0];
        }
    }

    @Override
    public int length(int i0) {
        if ( this.rank()==2 ) {
            return datasets.size();
        } else {
            return qube[1];
        }    
    }

    @Override
    public int length(int i0,int i1) { // experimental https://sourceforge.net/tracker/?func=detail&atid=970685&aid=3545095&group_id=199733
        if ( this.rank()==3 ) {
            return datasets.size();
        } else {
            return qube[2];
        }
    }

    @Override
    public int length(int i0,int i1, int i2) {
        if ( this.rank()==4 ) {
            return datasets.size();
        } else {
            return qube[3];
        }
    }

    @Override
    public String toString() {
        if ( rank==1 ) {
            return DataSetUtil.format(this);
        } else {
            QDataSet dep0= (QDataSet) this.property(QDataSet.DEPEND_0);
            String dep0name= "";
            if (dep0 != null) {
                dep0name = (String) dep0.property(QDataSet.NAME);
                if ( dep0name==null ) dep0name=""; else dep0name=dep0name+"=";
            }
            String ss= DataSetUtil.toString(qube);
            ss= ss.substring(1,ss.length()-1);
            return "TailBundleDataSet["+dep0name + ss + "," + datasets.size()+" datasets]";
        }
    }

}
