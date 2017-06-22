/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.das2.datum.Units;

/**
 * grids (X,Y,Z) data into a table Z(X,Y)
 * @author jbf
 */
public class GridDataSet extends AbstractDataSet {

    TreeMap<Integer, Double> xtags;
    TreeMap<Integer, Double> ytags;
    Map<Double,Integer> rxtags;
    Map<Double,Integer> rytags;
    Map<Integer, Double> values;
    int ix;
    int iy;

    private static final double fill= -1e31;

    public GridDataSet() {
        xtags = new TreeMap<Integer, Double>();
        ix=0;
        rxtags= new HashMap<Double,Integer>();
        ytags = new TreeMap<Integer, Double>();
        iy=0;
        rytags= new HashMap<Double,Integer>();
        values = new HashMap<Integer, Double>();
        properties.put( QDataSet.FILL_VALUE, fill );
    }

    /**
     * add either rank 1 slice ( x,y,z ) or
     * add rank 2 dataset ( *,3 )
     * @param slice
     */
    public void add(QDataSet slice) {
        QDataSet bds=null;
        if ( slice.rank()==1 ) {
            double x = slice.value(0);
            double y = slice.value(1);
            double z = slice.value(2);
            add( x, y, z );
            bds= (QDataSet)slice.property(QDataSet.BUNDLE_0);
        } else if ( slice.rank()==2 ) {
            for ( int i=0; i<slice.length(); i++ ) {
                double x = slice.value(i,0);
                double y = slice.value(i,1);
                double z = slice.value(i,2);
                add( x, y, z );
            }
            bds= (QDataSet)slice.property(QDataSet.BUNDLE_1);
        }
        if ( bds!=null ) {
            for ( int i=0;i<3;i++ ) {
                MutablePropertyDataSet mds;
                if ( i==0 ) {
                    mds= x;
                } else if ( i==1 ) {
                    mds= y;
                } else {
                    mds= this;
                } 
                
                Units u;
                String s;
                u= (Units) bds.property( QDataSet.UNITS, i );
                if ( u!=null ) mds.putProperty(QDataSet.UNITS, u );
                s= (String)bds.property( QDataSet.LABEL, i );
                if ( s!=null ) mds.putProperty(QDataSet.LABEL, s );
                s= (String)bds.property( QDataSet.TITLE, i );
                if ( s!=null ) mds.putProperty(QDataSet.TITLE, s );
                
            }
        }

    }

    public void add( double x, double y, double z ) {

        int iix, iiy;
        if ( !rxtags.containsKey(x) ) {
            xtags.put(ix,x);
            rxtags.put(x,ix);
            iix= ix;
            ix++;
        } else {
            iix= rxtags.get(x);
        }

        if ( !rytags.containsKey(y) ) {
            ytags.put(iy,y);
            rytags.put(y,iy);
            iiy= iy;
            iy++;
        } else {
            iiy= rytags.get(y);
        }

        values.put( iiy*10000 + iix, z );
        
    }

    @Override
    public int rank() {
        return 2;
    }

    @Override
    public int length() {
        return xtags.size();
    }

    @Override
    public int length(int i) {
        return ytags.size();
    }

    AbstractDataSet x = new AbstractDataSet() {

        public int rank() {
            return 1;
        }

        public double value(int i) {
            return xtags.get(i);
        }

        public int length() {
            return xtags.size();
        }
    };
    AbstractDataSet y = new AbstractDataSet() {

        public int rank() {
            return 1;
        }

        public double value(int i) {
            return ytags.get(i);
        }

        public int length() {
            return ytags.size();
        }
    };

    @Override
    public Object property(String name) {
        if (name.equals(QDataSet.DEPEND_0)) {
            return x;
        } else if (name.equals(QDataSet.DEPEND_1)) {
            return y;
        } else {
            return super.property(name);
        }
    }

    @Override
    public double value(int i0, int i1) {
        Double v= values.get( i1 * 10000 + i0 );
        if ( v==null ) {
            return fill;
        } else {
            return v;
        }
    }
}
