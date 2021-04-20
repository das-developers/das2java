
package org.das2.qds;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.das2.datum.Units;

/**
 * grids a bundle of (X,Y,Z) data into a table Z(X,Y).
 * @author jbf
 */
public final class GridDataSet extends AbstractDataSet {

    TreeMap<Integer, Double> xtags;
    TreeMap<Integer, Double> ytags;
    Map<Double,Integer> rxtags;
    Map<Double,Integer> rytags;
    Map<Integer, Double> values;
    int ix;
    int iy;

    private static final double FILL= -1e31;

    /**
     * creates the dataset, initially length is zero.
     */
    public GridDataSet() {
        xtags = new TreeMap<>();
        ix=0;
        rxtags= new HashMap<>();
        ytags = new TreeMap<>();
        iy=0;
        rytags= new HashMap<>();
        values = new HashMap<>();
        properties.put( QDataSet.FILL_VALUE, FILL );
    }

    /**
     * add either rank 1 slice ( x,y,z ) or
     * add rank 2 dataset ( *,3 )
     * @param slice rank 1 or rank 2 bundle
     */
    public void add(QDataSet slice) {
        QDataSet bds=null;
        if ( slice.rank()==1 ) {
            double xx = slice.value(0);
            double yy = slice.value(1);
            double zz = slice.value(2);
            add( xx, yy, zz );
            bds= (QDataSet)slice.property(QDataSet.BUNDLE_0);
        } else if ( slice.rank()==2 ) {
            for ( int i=0; i<slice.length(); i++ ) {
                double xx = slice.value(i,0);
                double yy = slice.value(i,1);
                double zz = slice.value(i,2);
                add( xx, yy, zz );
            }
            bds= (QDataSet)slice.property(QDataSet.BUNDLE_1);
        }
        if ( bds!=null ) {
            for ( int i=0;i<3;i++ ) {
                MutablePropertyDataSet mds;
                switch (i) {
                    case 0:
                        mds= x;
                        break;
                    case 1:
                        mds= y;
                        break; 
                    default:
                        mds= this;
                        break;
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

    /**
     * add the tuple to the data for gridding.  If the x tag or y tag has already been used, then start a new column or row.
     * @param x the x tag
     * @param y the y tag
     * @param z the z value at x,y.
     */
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

    private AbstractDataSet x = new AbstractDataSet() {

        @Override
        public int rank() {
            return 1;
        }

        @Override
        public double value(int i) {
            return xtags.get(i);
        }

        @Override
        public int length() {
            return xtags.size();
        }
    };
    
    private AbstractDataSet y = new AbstractDataSet() {

        @Override
        public int rank() {
            return 1;
        }

        @Override
        public double value(int i) {
            return ytags.get(i);
        }

        @Override
        public int length() {
            return ytags.size();
        }
    };

    @Override
    public Object property(String name) {
        switch (name) {
            case QDataSet.DEPEND_0:
                return x;
            case QDataSet.DEPEND_1:
                return y;
            default:
                return super.property(name);
        }
    }

    @Override
    public double value(int i0, int i1) {
        Double v= values.get( i1 * 10000 + i0 );
        if ( v==null ) {
            return FILL;
        } else {
            return v;
        }
    }
}
