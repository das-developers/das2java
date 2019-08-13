
package org.das2.qds;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;
import org.das2.qds.ops.Ops;

/**
 * create a higher rank dataset with dim 1 being a bundle dimension.  Each
 * dataset must have the same length.
 *
 * Note this was created before BUNDLE_1 and bundle descriptor datasets were
 * introduced, so this code is suspect.  TODO: review and ensure compatibility
 * with updates to bundle dataset semantics.
 *
 * Copied from JoinDataSet in June 2009
 * Modification History:<ul>
 * <li> 2012-07-17: allow rank 2 datasets to be bundled.  TODO: really?  code doesn't look like it!
 * <li> See https://sourceforge.net/p/autoplot/feature-requests/267/
 * </ul>
 * @author jbf
 */
public final class BundleDataSet extends AbstractDataSet {
    
    private static final Logger logger= LoggerManager.getLogger("qdataset");
    
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
     * @param rank rank of the bundle.
     */
    public BundleDataSet( int rank ) {
        this.rank= rank;
        datasets= new ArrayList<>();
        if ( rank>=2 ) {
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
    public BundleDataSet( QDataSet ds ) {
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
            if ( len0==-1 ) {
                len0= ds.length();
                //QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0 ); // See https://sourceforge.net/p/autoplot/bugs/1639/
                //if ( dep0!=null ) putProperty( QDataSet.DEPEND_0, dep0 );
            } else {
                if ( ds.length()!=len0 ) throw new IllegalArgumentException( String.format( "dataset length (%d) is not consistent with the bundle's length (%d)", ds.length(), len0) );
            }
        }
        // This can't be done carelessly, because test037_breakCounter_2 shows a problem.
//        QDataSet dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
//        QDataSet thisDep0= (QDataSet) super.property( QDataSet.DEPEND_0 );
//        if ( thisDep0==null && dep0!=null ) {   
//            putProperty( QDataSet.DEPEND_0, dep0 );
//        } else if ( thisDep0!=null && dep0!=null ) {
//            if ( thisDep0.length()>0 && !Ops.equivalent( thisDep0, dep0 ) ) {
//                logger.warning("bundled datasets do not have the same timetags");
//            }
//        }
        datasets.add( ds );
        
    }

    /**
     * allow to simply unbundle the dataset.  If the bundle has DEPEND_0, then
     * DEPEND_0 may be added to the result.
     * @param i the index.
     * @return the dataset at i.
     */
    public QDataSet unbundle(int i) {
        QDataSet result= datasets.get(i);
        QDataSet dep0= (QDataSet) super.property(QDataSet.DEPEND_0);
        if ( dep0!=null && result.property(QDataSet.DEPEND_0)==null ) {
            result= Ops.putProperty( result, QDataSet.DEPEND_0, dep0 );
        }
        // check this datasets properties to see if there's additional properties which have been
        // added to the data.
//        QDataSet bds= (QDataSet) this.property( QDataSet.BUNDLE_1 );
//        if ( bds!=null ) {
//            bds= bds.slice(i);
//            Map<String,Object> p= DataSetUtil.getProperties(bds);
//            if ( result instanceof MutablePropertyDataSet ) {
//                MutablePropertyDataSet mpds= (MutablePropertyDataSet)result;
//                if ( !mpds.isImmutable() ) {
//                    DataSetUtil.putProperties( p, mpds ); // I may regret this in the future...
//                } else {
//                    mpds= DataSetOps.makePropertiesMutable( result );
//                    DataSetUtil.putProperties( p, mpds ); 
//                }
//            } else {
//                MutablePropertyDataSet mpds= DataSetOps.makePropertiesMutable( result );
//                DataSetUtil.putProperties( p, mpds ); 
//            }
//        }
        return result;
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
            Object v= super.property(name,i);
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
        return datasets.get(i1).value(i0,i2);
    }

    @Override
    public double value(int i0, int i1, int i2, int i3) {
        if ( this.rank!=4 ) {
            throw new IllegalArgumentException("rank 4 access on rank "+this.rank+" bundle dataset");
        }        
        return datasets.get(i1).value(i0,i2,i3);
    }

    @Override
    public Object property(String name, int i0) {
        if ( rank==1 ) {
            return datasets.get(i0).property(name);
        } else {
            Object result= super.property(name);
            if ( result==null ) {
                return null;
//                try {
//                    return datasets.get(i0).property(name);
//                } catch ( Exception ex ) {
//                    return null;
//                }
            } else {
                return result;
            }
        }
    }

    @Override
    public int length() {
        return this.rank==2 ? len0 : datasets.size();
    }

    @Override
    public int length(int i0) {
        return datasets.size();
    }

    @Override
    public int length(int i0,int i1) { // experimental https://sourceforge.net/tracker/?func=detail&atid=970685&aid=3545095&group_id=199733
        return datasets.get(i0).length(i1);
    }

    @Override
    public int length(int i0,int i1, int i2) {
        return datasets.get(i0).length(i1,i2);
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
            return "BundleDataSet["+dep0name + len0 + "," + datasets.size()+" datasets]";
        }
    }

}
