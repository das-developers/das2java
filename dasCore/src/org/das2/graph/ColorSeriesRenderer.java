
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import org.das2.datum.Units;
import org.das2.qds.JoinDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 * 
 * Draw a series line connecting data points.
 * @author jbf
 */
public class ColorSeriesRenderer extends Renderer {

    DasAxis xaxis=null, yaxis=null;
    
    public static QDataSet doAutorange( QDataSet ds ) {

        QDataSet xrange;
        QDataSet yrange;

        ds= makeDataCanonical( ds );
        
        xrange= Ops.extent( Ops.slice1( ds, 0 ) );
        xrange= Ops.rescaleRange( xrange, -0.1, 1.1 );

        yrange= Ops.extent( Ops.slice1( ds, 1 ) );
        yrange= Ops.rescaleRange( yrange, -0.1, 1.1 );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }
    
    @Override
    public boolean acceptsDataSet(QDataSet ds) {
        ds= makeDataCanonical( getDataSet() );
        if ( ds.rank()!=2 ) {
            return false;
        }
        if ( ds.length(0)!=3 ) {
            return false;
        }
        return true;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        QDataSet ds= makeDataCanonical( getDataSet() );
        QDataSet xds= Ops.slice1( ds, 0 );
        QDataSet yds= Ops.slice1( ds, 1 );
        GeneralPath path= GraphUtil.getPath( 
                xaxis, yaxis, xds, yds, GraphUtil.CONNECT_MODE_SERIES, false );
        return path.intersects( new Rectangle2D.Double( x-5, y-5, 10, 10 ) );
    }

    /**
     * returns a bundle of x,y,z
     * @param ds bundle of x,y,z or y[x,PLANE_0=z]
     * @return 
     */
    private static QDataSet makeDataCanonical( QDataSet ds ) {
        if ( ds.rank()==2 ) {
            return ds;
        } else {
            return Ops.bundle( (QDataSet)ds.property(QDataSet.DEPEND_0), 
                    ds, 
                    (QDataSet) ds.property(QDataSet.PLANE_0) );
        }
    }
    
    @Override
    public void setDataSet(QDataSet ds) {
        super.setDataSet(ds); 
        QDataSet ds1= makeDataCanonical(ds);
        if ( ds1.rank()!=2 ) {
            throw new IllegalArgumentException("data should be rank 2");
        }
        if ( ds1.length(0)!=3 ) {
            throw new IllegalArgumentException("data should be n,3: X,Y,Color");
        }
    }

    
    @Override
    public void render(Graphics2D g, DasAxis xAxis, DasAxis yAxis) {
        QDataSet ds= makeDataCanonical(getDataSet());
        this.xaxis= xAxis;
        this.yaxis= yAxis;
        if ( colorBar==null ) {
            getParent().postException( this, 
                    new IllegalArgumentException("colorbar has not been set") );
            return;
        }
        Units u= (Units) ds.slice(0).slice(2).property(QDataSet.UNITS);
        Units xu= (Units) ds.slice(0).slice(0).property(QDataSet.UNITS);
        xu= xu==null ? Units.dimensionless : xu;
        Units yu= (Units) ds.slice(0).slice(1).property(QDataSet.UNITS);
        yu= yu==null ? Units.dimensionless : yu;
        Line2D.Double line= new Line2D.Double();
        double x0;
        double y0;
        if ( ds.length()>0 ) {
            x0= xAxis.transform( ds.value(0,0), xu );
            y0= yAxis.transform( ds.value(0,1), yu );
        } else {
            return;
        }
        for ( int i=1; i<ds.length(); i++ ) {
            double c= ds.value(i-1, 2);
            if ( u==Units.rgbColor ) {
                g.setColor( new Color((int)c) );
            } else {
                int ic= colorBar.rgbTransform( c, u );
                g.setColor( new Color(ic) );
            }
            line.x1= x0;
            line.y1= y0;
            line.x2= xAxis.transform( (ds.value(i,0)+ds.value(i-1,0))/2, xu );
            line.y2= yAxis.transform( (ds.value(i,1)+ds.value(i-1,1))/2, yu );
            g.draw( line );
            
            c= ds.value(i,2);
            if ( u==Units.rgbColor ) {
                g.setColor( new Color((int)c) );
            } else {
                int ic= colorBar.rgbTransform( c, u );
                g.setColor( new Color(ic) );
            }
            line.x1= line.x2;
            line.y1= line.y2;
            line.x2= xAxis.transform( ds.value(i,0), xu );
            line.y2= yAxis.transform( ds.value(i,1), yu );
            x0= line.x2;
            y0= line.y2;
            g.draw( line );
            
        }
    }
    
}
