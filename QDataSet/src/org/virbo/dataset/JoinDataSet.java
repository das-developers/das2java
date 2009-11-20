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
        datasets= new ArrayList<QDataSet>();
    }

    /**
     * add the dataset to this set of joined datasets.
     * @param ds rank N-1 dataset where N is the rank of this JoinDataSet.
     * @throws IllegalArgumentException if the dataset rank is not consistent with the other datasets.
     */
    public void join( QDataSet ds ) {
        if ( ds.rank()!=this.rank-1 ) throw new IllegalArgumentException("dataset rank must be "+(this.rank-1));
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


    public int length() {
        return datasets.size();
    }

    public int length(int i0) {
        return datasets.get(i0).length();
    }

    public int length(int i0, int i1) {
        return datasets.get(i0).length(i1);
    }

    public int length(int i0, int i1, int i2 ) {
        return datasets.get(i0).length(i1,i2);
    }

    public String toString() {
        if ( datasets.size()>4 ) {
            return "JoinDataSet["+datasets.size()+" datasets: "+ datasets.get(0)+", "+datasets.get(1)+", ...]";
        } else {
            return "JoinDataSet["+datasets.size()+" datasets: "+ datasets +" ]";
        }
    }

    public JoinDataSet trim( int imin, int imax ) {
        datasets= datasets.subList(imin, imax);
        return this;
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
            Pattern indexPropPattern= Pattern.compile("([a-zA-Z]*)\\[(\\d+)\\]");
            for ( Entry<String,Object> e : properties.entrySet() ) {
                Matcher m= indexPropPattern.matcher(e.getKey());
                if ( m.matches() && Integer.parseInt(m.group(2))==idx ) {
                    mpds.putProperty( m.group(1), e.getValue() );
                } else {
                    if ( result.property( e.getKey()) == null ) {
                        mpds.putProperty( e.getKey(), e.getValue() );
                    }
                }
                final Object dep1 = properties.get(QDataSet.DEPEND_1);
                if ( dep1 !=null ) {
                    mpds.putProperty( QDataSet.DEPEND_0,dep1);
                }
            }
        }
        return result;
    }
}
