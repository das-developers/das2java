/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds.util;

import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.QubeDataSetIterator;
import org.das2.qds.ops.Ops;

/**
 * Codes for interpolating from irregular grids to regular grids.
 * 
 * For these codes we use "trigulations" which are datasets that contain connections
 * of points in other datasets.  These are rank 2 datasets tri[n,3] where the 3 points are
 * indeces of other datasets.
 * 
 * Triangulations are generalized, where the typical case is a set of 3-node triangles.  However,
 * 2-point "triangles" can be used for linear interpolation, 4-point squares can be used for bilinear 
 * interpolation, and 4-points for 3-D triangle interpolation. 
 * 
 * DO NOT USE THIS CLASS, IT IS STILL UNDER DEVELOPMENT!  This is barely tested for the nfree=1-D case!
 * 
 * @author jbf
 */
public class Griddata {
    
    /**
     * Perform the interpolation for arbitrary sets of nvert points.
     * @param triangles triangles mesh, presently tri[n,3] but may soon be tri[n,4] as well.
     * @param itriangle rank n mesh identifying the triangle to use
     * @param weights rank n+1 mesh (e.g. weights[m,3]) with a weight for each triangle node.
     * @param zbuck dependent Z values for each point.
     * @return dataset with the same geometry as itriangle.
     */
    public static QDataSet griddata( QDataSet triangles, QDataSet itriangle, QDataSet weights, QDataSet zbuck ) {
        ArrayDataSet result= ArrayDataSet.copy(double.class,itriangle);
        QubeDataSetIterator iter= new QubeDataSetIterator(itriangle);
        int nvert= triangles.length(0); // typically 3
        assert nvert<=3;
        QDataSet weightsBuck= Ops.valid(zbuck);
        double fill= -1e38;
        while ( iter.hasNext() ) {
            iter.next();
            double itri= iter.getValue(itriangle);
            double s=0.;
            double w=0.;
            for ( int i=0; i<nvert; i++ ) {
                double w1= weights.value( iter.index(0), i );
                int itri1= (int)triangles.value((int)itri,i);
                double zval;
                double wval;
                wval= weightsBuck.value( itri1 );
                wval*= w1;
                if ( wval>0 ) {
                    zval= zbuck.value( itri1 );
                    w+= wval;
                    s+= wval * zval;
                } else if (  wval<0 ) {
                    throw new IllegalArgumentException("negative weights not allowed at index "+ iter.index(0) );
                }
            }
            if ( w==0 ) {
                iter.putValue( result, fill );
            } else {
                iter.putValue( result, s/w );
            }
        }
        DataSetUtil.copyDimensionProperties( zbuck, result );
        result.putProperty( QDataSet.FILL_VALUE, fill );
        return result;
    }
    
    public static QDataSet griddata( QDataSet xx, QDataSet yy, QDataSet buck ) {
        throw new UnsupportedOperationException("not implemented");
    }
    
    /**
     * return a rank 2 dataset [n,nvert] with the "triangles" connecting the buckshot data.
     * @param buck dataset[m,nvert-1].  
     * @return tri[n,3] 
     */
    public static QDataSet triangulate( QDataSet buck ) {
        int nfree= buck.length(0);
        if ( nfree==1 ) {
            buck= Ops.unbundle(buck,0);
            QDataSet ss= Ops.sort(buck);
            QDataSet result= Ops.bundle( ss.trim(0,ss.length()-1), ss.trim(1,ss.length()) );
            return result;
        } else {
            throw new IllegalArgumentException("nfree("+nfree+") must be one (for now)");
        }
    }

    /**
     * for each element of grid, identify the matching triangle.
     * @param buck the data (e.g. buck[o,nfree])
     * @param tri the triangulation (e.g. tri[n,free+1])
     * @param grid the values to locate (e.g. grid[m,nfree])
     * @return the locations itri[o]
     */
    public static QDataSet triLocate(QDataSet buck, QDataSet tri, QDataSet grid ) {
        int nfree= buck.length(0);
        if ( nfree==1 ) {
            ArrayDataSet result= ArrayDataSet.createRank1( int.class, grid.length() );
            buck= Ops.unbundle(buck,0);
            for ( int i=0; i<grid.length(); i++ ) { // TODO: brute-force O(n**2) needs to be rewritten
                double d= grid.value(i);
                result.putValue( i, -1e38 );
                for ( int j=0; j<tri.length(); j++ ) {
                    if ( d>=buck.value((int)tri.value(j,0)) && d<buck.value((int)tri.value(j,1)) ) {
                        result.putValue( i, j );
                        break;
                    }
                }
            }
            return result;
        } else {
            throw new IllegalArgumentException("nfree("+nfree+") must be one (for now)");
        }
    }
    
    /** 
     * return the weights for each point of the grid.  Let nfree be the number of independent data dimensions.
     * 
     * @param buck the data (e.g. buck[o,nfree])
     * @param tri the triangulation (e.g. tri[n,nfree+1])
     * @param grid rank 2 dataset (e.g. grid[m,nfree])
     * @param itri the triangle to use for each point of grid. (e.g. itri[m])
     * @return rank 2 (e.g. weights[m,2])
     */
    public static QDataSet weights( QDataSet buck, QDataSet tri, QDataSet grid, QDataSet itri ) {
        int nfree= buck.length(0);
        if ( nfree==1 ) { // 
            DDataSet result= DDataSet.createRank2( grid.length(), nfree+1 );
            buck= Ops.unbundle(buck,0);
            for ( int i=0; i<grid.length(); i++ ) {
                int itri1= (int)itri.value(i);
                int tri1= (int) tri.value(itri1,0); // index of the left point.
                int tri2= (int) tri.value(itri1,1); // index of the right point.
                double len= Math.abs( buck.value( tri2 ) - buck.value(tri1) );
                double l1= grid.value(i,0);
                result.putValue( i, 0, Math.abs(buck.value(tri2)-l1)/(len) );
                result.putValue( i, 1, Math.abs(l1-buck.value(tri1))/(len) );
            }
            return result;
        } else {
            throw new IllegalArgumentException("nfree("+nfree+") must be one (for now)");
        }
        
    }
}
