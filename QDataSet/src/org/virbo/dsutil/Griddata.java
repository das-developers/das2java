/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dsutil;

import java.util.HashMap;
import java.util.Map;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QubeDataSetIterator;
import org.virbo.dsops.Ops;

/**
 * Codes for interpolating from irregular grids to regular grids.
 * 
 * For these codes we use "trigulations" which are datasets that contain connections
 * of points in other datasets.  These are rank 2 datasets tri[n,3] where the 3 points are
 * indeces of other datasets.
 * 
 * @author jbf
 */
public class Griddata {
    
    /**
     * Perform the interpolation for arbitrary sets of nvert points.
     * @param triangles triangles mesh, presently tri[n,3] but may soon be tri[n,4] as well.
     * @param itriangle rank n mesh identifying the triangle to use
     * @param weights rank n+1 mesh (e.g. weights[m,3]) with a weight for each triangle node.
     * @param buck dependent Z values for each point.
     * @return dataset with the same geometry as itriangle.
     */
    public static QDataSet griddata( QDataSet triangles, QDataSet itriangle, QDataSet weights, QDataSet buck ) {
        ArrayDataSet result= ArrayDataSet.copy(double.class,itriangle);
        QubeDataSetIterator iter= new QubeDataSetIterator(itriangle);
        assert triangles.length()==3;
        int nvert= triangles.length(0); // typically 3
        QDataSet weightsBuck= Ops.valid(buck);
        double fill= -1e38;
        while ( iter.hasNext() ) {
            iter.next();
            double itri= iter.getValue(itriangle);
            double s=0.;
            double w=0.;
            for ( int i=0; i<nvert; i++ ) {
                w+= weights.value( iter.index(0),i );
                double zval= buck.value((int)triangles.value((int)itri));
                double wval= weightsBuck.value((int)triangles.value((int)itri));
                s+= weights.value( iter.index(0),i ) * wval * zval;
            }
            if ( w==0 ) {
                iter.putValue( result, fill );
            } else {
                iter.putValue( result, s/w );
            }
        }
        DataSetUtil.copyDimensionProperties( buck, result );
        result.putProperty( QDataSet.FILL_VALUE, fill );
        return result;
    }
    
    public static QDataSet griddata( QDataSet xx, QDataSet yy, QDataSet buck ) {
        throw new UnsupportedOperationException("not implemented");
    }
    
    /**
     * return a rank 2 dataset [n,3] with the triangles connecting the buckshot data.
     * @param buck dataset[m,2].  The data can have more than two indexes (e.g. dataset[m,3] is fine).
     * @return tri[n,3] 
     */
    public static QDataSet triangulate( QDataSet buck ) {
        throw new UnsupportedOperationException("not implemented");
    }
    
}
