
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import org.das2.datum.Units;
import org.das2.qds.DataSetUtil;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import org.das2.util.GrannyTextRenderer;

/**
 * render a 2-D triangular mesh.  This was introduced just to identify
 * this schema.  This
 * renderer needs rank 3 data with 2 bundled datasets, the
 * points and the triangles which connect them.  When a rank 1
 * dataset with a triangleMesh as DEPEND_0 is passed in, then colors
 * or text will be drawn.
 * @author jbf
 */
public class TriangleMeshRenderer extends Renderer {
    
    /**
     * return true if the renderer accepts data in this form.  
     * @param ds
     * @return 
     */
    public static boolean acceptsData( QDataSet ds ) {
        if ( ds.rank()==1 ) {
            QDataSet tris= (QDataSet) ds.property(QDataSet.DEPEND_0);
            if ( tris==null ) {
                return false;
            } else {
                return Schemes.isTriangleMesh(tris);
            }
        } else {
            return Schemes.isTriangleMesh(ds);
        }
    }

    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param ds the dataset to autorange.
     * @return a bounds dataset, where result[0,:] is the xrange and result[1,:] is the yrange.
     */
    public static QDataSet doAutorange( QDataSet ds ) {
        QDataSet tris;
        if ( ds.rank()==1 ) {
            tris= (QDataSet) ds.property(QDataSet.DEPEND_0);
        } else {
            tris= ds;
        }
        QDataSet xy= tris.slice(0);
        
        QDataSet xrange= Ops.extent(Ops.slice1( xy,0 ));
        QDataSet yrange= Ops.extent(Ops.slice1( xy,1 ));
        
        xrange= Ops.rescaleRangeLogLin( xrange, -0.1, 1.1 );
        yrange= Ops.rescaleRangeLogLin( yrange, -0.1, 1.1 );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }
        
    @Override
    public void render(Graphics2D g, DasAxis xaxis, DasAxis yaxis) {
        
        QDataSet ds= getDataSet();
        
        QDataSet tri, xy;
        QDataSet dependentVariable;
        
        if ( Schemes.isTriangleMesh(ds) ) {
            tri= ds.slice(1);
            xy= ds.slice(0);
            dependentVariable= null;
        } else {
            dependentVariable= ds;
            ds= (QDataSet) ds.property( QDataSet.DEPEND_0 );
            tri= ds.slice(1);
            xy= ds.slice(0);
        }
        
        g.setColor(Color.GRAY);
               
        double[] xx= new double[xy.length()];
        double[] yy= new double[xy.length()];
        
        Units xunits= SemanticOps.getUnits(xy.slice(0).slice(0));
        Units yunits= SemanticOps.getUnits(xy.slice(0).slice(1));
        
        for ( int i=0; i<xy.length(); i++ ) {
            xx[i]= xaxis.transform( xy.value(i,0), xunits );
            yy[i]= yaxis.transform( xy.value(i,1), yunits );
        }
        
        Color[] cc= null;
        String[] ss= null;
        
        if ( dependentVariable!=null ) {
            Units zunits= SemanticOps.getUnits(dependentVariable);
            if ( zunits==Units.rgbColor ) {
                cc= new Color[tri.length()];
                for ( int i=0; i<cc.length; i++ ) {
                    cc[i]= new Color((int)dependentVariable.value(i));
                }
            } else { 
                ss= new String[tri.length()];
                for ( int i=0; i<ss.length; i++ ) {
                    ss[i]= DataSetUtil.toString(dependentVariable.slice(i));
                }
            }
        } else {
            cc= new Color[tri.length()];
            for ( int i=0; i<cc.length; i++ ) {
                cc[i]= Color.LIGHT_GRAY;
            }
        }
        
        for ( int i=0; i<tri.length(); i++ ) {
            QDataSet tri1= tri.slice(i);
            int i1= (int)tri1.value(0);
            int i2= (int)tri1.value(1);
            int i3= (int)tri1.value(2);
            if ( cc!=null ) {
                g.setColor(cc[i]);
                GeneralPath gp= new GeneralPath();
                gp.moveTo( xx[i1], yy[i1] );
                gp.lineTo( xx[i2], yy[i2] );
                gp.lineTo( xx[i3], yy[i3] );
                gp.lineTo( xx[i1], yy[i1] );
                g.fill(gp);
            } else if ( ss!=null ) {
                double x= ( xx[i1] + xx[i2] + xx[i3] ) / 3;
                double y= ( yy[i1] + yy[i2] + yy[i3] ) / 3;
                GrannyTextRenderer gtr= new GrannyTextRenderer();
                gtr.setString(g, ss[i]);
                gtr.draw( g, (int)(x-gtr.getWidth()/2), (int)(y+gtr.getAscent()/2) );
            }
        }

    }
}
