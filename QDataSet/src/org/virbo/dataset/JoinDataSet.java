/*
 * JoinDataSet.java
 *
 * Created on April 27, 2007, 10:52 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Create a higher rank dataset with dim 0 being a JOIN dimension.  Join implies
 * that the joined datasets occupy the same physical dimension, and this can
 * be thought of as the "array of" index.  Note DEPEND_0 is treated as a
 * special case of join.
 * Note this dataset is mutable, and clients should not mutate it once the reference is made public.
 * @author jbf
 */
public class JoinDataSet extends AbstractDataSet {

    /**
     * if we haven't joined any datasets, then report this size when length(i) is
     * queried.  QDataSet supports DataSet[0,20,20].  Somewhere there is code
     * that assumes a qube...
     */
    private static final int NO_DATASET_SIZE = 10;
    
    List<QDataSet> datasets;
    /**
     * rank of the dataset.  Joined DataSets should have rank rank-1.
     */
    int rank;
    
    /** Creates a new instance of JoinDataSet 
     * @param rank The rank of the JoinDataSet.  Each dataset joined must have rank <tt>rank</tt>-1.
     * 
     */
    public JoinDataSet( int rank ) {
        this.rank= rank;
        putProperty(QDataSet.JOIN_0,  DDataSet.create( new int[0] ) ); // rank 0 dataset allows properties like CACHE_TAG to be stored here.
        //putProperty(QDataSet.JOIN_0,  "DEPEND_1" );
        datasets= new ArrayList<QDataSet>();
    }

    /**
     * create a new JoinDataSet, and join the first dataset.
     *    ds1= Ops.rand(30);
     *    jds= new JoinDataSet( ds1 );
     *    assert( ds1.rank()==1 );
     *    assert( jds.rank()==2 );
     *    assert( jds.slice(0).equals(ds1) );
     * @param ds1
     */
    public JoinDataSet( QDataSet ds1 ) {
        this( ds1.rank()+1 );
        join(ds1);
    }

    /**
     * copy the JoinDataSet without copying each dataset it contains.
     * @param joinDataSet
     * @return
     */
    public static JoinDataSet copy(JoinDataSet joinDataSet) {
        JoinDataSet result= new JoinDataSet(joinDataSet.rank());
        result.datasets.addAll( joinDataSet.datasets );
        DataSetUtil.putProperties( DataSetUtil.getProperties(joinDataSet), result );
        result.putProperty(QDataSet.DEPEND_0, joinDataSet.property(QDataSet.DEPEND_0));
        result.putProperty(QDataSet.JOIN_0, joinDataSet.property(QDataSet.JOIN_0) ); //might need to clear it if DEPEND_0 is set.
        return result;
    }


    /**
     * copy all the records into this JoinDataSet.  Note this is
     * a shallow copy, and changes to one of the element datasets is visible
     * in both JoinDataSets.
     * TODO: this is probably under implemented, for example element properties.
     * @param ds1
     */
    public void joinAll( JoinDataSet ds1 ) {
        for ( int j=0; j<ds1.length(); j++ ) {
            join(ds1.slice(j));
        }
        QDataSet dep0= (QDataSet) ds1.property(QDataSet.DEPEND_0);
        if ( dep0!=null ) {
            QDataSet thisDep0= (QDataSet) this.property(QDataSet.DEPEND_0);
            if ( thisDep0!=null ) {
                DDataSet dd= (DDataSet) DDataSet.maybeCopy( thisDep0 );
                DDataSet dd1= (DDataSet) DDataSet.maybeCopy(dep0);
                if ( !dd.canAppend(dd1) ) {
                    dd.grow( dd.length() + dd1.length() );
                }
                dd.append( dd1 );
                this.putProperty( QDataSet.DEPEND_0,dd );
            } else {
                throw new IllegalArgumentException("joinAll datasets one has depend_0 but other doesn't");
            }
        }
    }

    /**
     * add the dataset to this set of joined datasets.
     * @param ds rank N-1 dataset where N is the rank of this JoinDataSet.
     * @throws IllegalArgumentException if the dataset rank is not consistent with the other datasets.
     */
    public final void join( QDataSet ds ) {
        if ( ds.rank()!=this.rank-1 ) {
            throw new IllegalArgumentException("dataset rank must be "+(this.rank-1)+", it is rank "+ds.rank() );
        }
//TODO  JoinDataSet is used produce a bounds object of BINS datasets with different units.  Should this be allowed?  "strict" JOINs won't allow this.
//        QDataSet units= (QDataSet) property(QDataSet.UNITS);
//        if ( units==null ) {
//            properties.put( QDataSet.UNITS, ds.property(QDataSet.UNITS) );
//        } else {
//            if ( units!=ds.property(QDataSet.UNITS) ) throw new IllegalArgumentException("joined dataset has units: "+ds.property(QDataSet.UNITS)+ " and this has units: "+units );
//        }
        datasets.add( ds );
    }

    public int rank() {
        return rank;
    }

    @Override
    public double value(int i0) {
        return datasets.get(i0).value();
    }

    @Override
    public double value(int i0, int i1) {
        return datasets.get(i0).value(i1);
    }

    @Override
    public double value(int i0, int i1, int i2) {
        return datasets.get(i0).value(i1,i2);
    }

    @Override
    public double value(int i0, int i1, int i2, int i3 ) {
        return datasets.get(i0).value(i1,i2,i3);
    }

    @Override
    public Object property(String name, int i0) {
        String sname= name + "__" + i0;
        Object result= properties.get(sname);
        if ( result==null ) {
            return datasets.get(i0).property(name);
        } else {
            return result;
        }
    }

    /**
     * We override putProperty here because we remove the JOIN_0 if DEPEND_0 is set.
     * @param name
     * @param value
     */
    @Override
    public final void putProperty(String name, Object value) {
        super.putProperty(name, value);
        if ( name.equals(QDataSet.DEPEND_0) ) {
            super.putProperty(QDataSet.JOIN_0, null);
        }
    }

    @Override
    public int length() {
        return datasets.size();
    }

    @Override
    public int length(int i0) {
        if ( datasets.size()==0 && i0==0 ) {
            return NO_DATASET_SIZE;
        }
        return datasets.get(i0).length();
    }

    @Override
    public int length(int i0, int i1) {
        if ( datasets.size()==0 && i0==0 ) {
            return NO_DATASET_SIZE;
        }
        return datasets.get(i0).length(i1);
    }

    @Override
    public int length(int i0, int i1, int i2 ) {
        if ( datasets.size()==0 && i0==0 ) {
            return NO_DATASET_SIZE;
        }
        return datasets.get(i0).length(i1,i2);
    }

    @Override
    public String toString() {
        return DataSetUtil.toString(this);
    }

    /**
     * clean up this trim.  This was implemented before QDataSet.trim was introduced.
     * @param imin
     * @param imax
     * @return
     */
    @Override
    public JoinDataSet trim( int imin, int imax ) {
        JoinDataSet result= new JoinDataSet(this.rank);
        result.datasets= new ArrayList<QDataSet>(imax-imin);
        result.datasets.addAll( this.datasets.subList(imin, imax) );
        result.properties.putAll( this.properties );
        QDataSet dep0= (QDataSet) property( QDataSet.DEPEND_0 );
        if ( dep0!=null ) result.properties.put( QDataSet.DEPEND_0, dep0.trim(imin,imax) );
        return result;
    }

    /**
     * slice the dataset by returning the joined dataset at this index.  If the
     * dataset is a MutablePropertiesDataSet, then add the properties of this
     * join dataset to it. 
     * The result is made a mutable properties dataset if it is not already, the danger here is that we may mutate the original data.
     * Capabilities will fix this.
     * @param idx
     * @return
     */
    @Override
    public QDataSet slice( int idx ) {
        QDataSet result= datasets.get(idx);
        if ( !( result instanceof MutablePropertyDataSet ) ) {
            result= DataSetOps.makePropertiesMutable(result);
        }
        if ( result instanceof MutablePropertyDataSet ) {
            MutablePropertyDataSet mpds= (MutablePropertyDataSet)result;
            Map<String,Object> props= DataSetOps.sliceProperties0( idx, properties );
            if ( props.size()>0 ) {
                //System.err.println("slice result is being mutated with "+props );
            }
            //Object odep0= props.get( QDataSet.DEPEND_0 );
            //if ( odep0!=null ) {
            //    props.put( QDataSet.DEPEND_0, null ); //TODO: we can put DEPEND_0 in.
            //}
            DataSetUtil.putProperties(props, mpds); //TODO: this is a little dangerous because we mutate the original datasets.
        }
        return result;
    }
}
