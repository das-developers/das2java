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
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create a higher rank dataset with dim 0 being a join dimension.  Join implies
 * that the joined datasets occupy the same physical dimension.
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
        putProperty(QDataSet.JOIN_0,"DEPEND_1");
        
        datasets= new ArrayList<QDataSet>();
    }

    public static JoinDataSet copy(JoinDataSet joinDataSet) {
        JoinDataSet result= new JoinDataSet(joinDataSet.rank());
        result.datasets.addAll( joinDataSet.datasets );
        DataSetUtil.putProperties( DataSetUtil.getProperties(joinDataSet), result );
        result.putProperty(QDataSet.DEPEND_0, joinDataSet.property(QDataSet.DEPEND_0));
        result.putProperty(QDataSet.JOIN_0, joinDataSet.property(QDataSet.JOIN_0) );//TODO: this seems redundant, it should already be set by the constructor.
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
    public void join( QDataSet ds ) {
        if ( ds.rank()!=this.rank-1 ) {
            throw new IllegalArgumentException("dataset rank must be "+(this.rank-1)+", it is rank "+ds.rank() );
        }
        datasets.add( ds );
    }

    public int rank() {
        return rank;
    }

    public double value(int i0) {
        return datasets.get(i0).value();
    }

    public double value(int i0, int i1) {
        return datasets.get(i0).value(i1);
    }

    public double value(int i0, int i1, int i2) {
        return datasets.get(i0).value(i1,i2);
    }

    public double value(int i0, int i1, int i2, int i3 ) {
        return datasets.get(i0).value(i1,i2,i3);
    }

    public Object property(String name, int i0) {
        String sname= name + "[" + i0 + "]";
        Object result= properties.get(sname);
        if ( result==null ) {
            return datasets.get(i0).property(name);
        } else {
            return result;
        }
    }

    public Object property(String name, int i0, int i1) {
        return datasets.get(i0).property(name,i1);
    }

    public Object property(String name, int i0, int i1, int i2 ) {
        return datasets.get(i0).property(name,i1,i2);
    }

    public Object property(String name, int i0, int i1, int i2, int i3 ) {
        return datasets.get(i0).property(name,i1,i2,i3);
    }

    @Override
    public void putProperty(String name, int index, Object value) {
        String sname= name + "[" + index + "]";
        properties.put(sname, value);
    }

    public void putProperty(String name, Object value) {
        super.putProperty(name, value);
        if ( name.equals(QDataSet.DEPEND_0) ) {
            super.putProperty(QDataSet.JOIN_0, null);
        }
    }

    public int length() {
        return datasets.size();
    }

    public int length(int i0) {
        if ( datasets.size()==0 && i0==0 ) {
            return NO_DATASET_SIZE;
        }
        return datasets.get(i0).length();
    }

    public int length(int i0, int i1) {
        if ( datasets.size()==0 && i0==0 ) {
            return NO_DATASET_SIZE;
        }
        return datasets.get(i0).length(i1);
    }

    public int length(int i0, int i1, int i2 ) {
        if ( datasets.size()==0 && i0==0 ) {
            return NO_DATASET_SIZE;
        }
        return datasets.get(i0).length(i1,i2);
    }

    public String toString() {
        if ( datasets.size()>4 ) {
            return "JoinDataSet["+datasets.size()+" datasets: "+ datasets.get(0)+", "+datasets.get(1)+", ...]";
        } else {
            return "JoinDataSet["+datasets.size()+" datasets: "+ datasets +" ]";
        }
    }

    /**
     * clean up this trim.  This was implemented before QDataSet.trim was introduced.
     * @param imin
     * @param imax
     * @return
     */
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
     * join dataset to it. //TODO: danger, the properties should be set regardless.
     * //this relies on just about every dataset being mpds.  Capabilities
     * will fix this.
     * @param idx
     * @return
     */
    public QDataSet slice( int idx ) {
        QDataSet result= datasets.get(idx);
        if ( result instanceof MutablePropertyDataSet ) {
            MutablePropertyDataSet mpds= (MutablePropertyDataSet)result;
            Pattern indexPropPattern= Pattern.compile("([a-zA-Z]*)\\[(\\d+)\\]"); // e.g. DEPEND_0[3]
            for ( Entry<String,Object> e : properties.entrySet() ) {
                Matcher m= indexPropPattern.matcher(e.getKey());
                if ( m.matches() && Integer.parseInt(m.group(2))==idx ) {
                    mpds.putProperty( m.group(1), e.getValue() );
                } else {
                    if ( e.getKey().equals("JOIN_0") || e.getKey().equals("DEPEND_0") || e.getKey().equals("BUNDLE_0") || e.getKey().equals("RENDER_TYPE") ) { //DEPEND_0 and BUNDLE_0 for good measure //TODO: use util dimensionProperties array
                        // don't copy these!!!
                    } else if ( result.property( e.getKey()) == null ) {
                        mpds.putProperty( e.getKey(), e.getValue() );
                    }
                }
                final Object dep1 = properties.get(QDataSet.DEPEND_1);
                if ( dep1 !=null ) {
                    mpds.putProperty( QDataSet.DEPEND_0,dep1);
                    mpds.putProperty( QDataSet.DEPEND_1,null);//TODO: this seems a little nasty.   Juno was getting datasets with both DEPEND_0 and DEPEND_1 set.
                }
            }
        }
        return result;
    }
}
