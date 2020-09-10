
package org.das2.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
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
    Units u;
    
    private String lineThick = "";

    public static final String PROP_LINETHICK = "lineThick";

    public String getLineThick() {
        return lineThick;
    }

    public void setLineThick(String lineThick) {
        String oldLineThick = this.lineThick;
        this.lineThick = lineThick;
        getParent().invalidateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_LINETHICK, oldLineThick, lineThick);
    }

    
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

    private void popNext( Graphics2D g, LinkedList<Point2D.Double> list, double c ) {
        GeneralPath p= new GeneralPath( GeneralPath.WIND_EVEN_ODD, 3 );
        Point2D.Double p1= list.pop();
        p.moveTo( p1.x, p1.y );
        Point2D.Double p2= list.pop();
        p.lineTo( p2.x, p2.y );
        Point2D.Double p3= list.peek();
        p.lineTo( p3.x, p3.y );
        if ( u==Units.rgbColor ) {
            g.setColor( new Color((int)c) );
        } else {
            int ic= colorBar.rgbTransform( c, u );
            g.setColor( new Color(ic) );
        }
        g.draw( p );
        
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
        
        double thick= this.lineThick.trim().length()==0 ? 
                1.0 :
                GraphUtil.parseLayoutLength( this.lineThick, xAxis.getDLength(), getParent().getEmSize() );
        
        g.setStroke( new BasicStroke((float)thick) );
        
        u= (Units) ds.slice(0).slice(2).property(QDataSet.UNITS);
        Units xu= (Units) ds.slice(0).slice(0).property(QDataSet.UNITS);
        xu= xu==null ? Units.dimensionless : xu;
        Units yu= (Units) ds.slice(0).slice(1).property(QDataSet.UNITS);
        yu= yu==null ? Units.dimensionless : yu;
        Line2D.Double line= new Line2D.Double();
        Point2D.Double p0= new Point2D.Double();
        
        LinkedList<Point2D.Double> list= new LinkedList<>();
        
        double x0;
        double y0;
        if ( ds.length()>0 ) {
            x0= xAxis.transform( ds.value(0,0), xu );
            y0= yAxis.transform( ds.value(0,1), yu );
            list.add( new Point2D.Double( x0, y0 ) );
        } else {
            return;
        }
        double c;
        for ( int i=1; i<ds.length(); i++ ) {
            c= ds.value(i-1, 2);
            x0= xAxis.transform( (ds.value(i,0)+ds.value(i-1,0))/2, xu );
            y0= yAxis.transform( (ds.value(i,1)+ds.value(i-1,1))/2, yu );
            list.add( new Point2D.Double( x0, y0 ) );
            
            if ( i==1 ) {
                list.add( new Point2D.Double( x0, y0 ) );
                popNext( g, list, ds.value(0, 2) );
            }
            if ( list.size()>=3 ) {
                popNext( g, list, c );
            }
            
            x0= xAxis.transform( ds.value(i,0), xu );
            y0= yAxis.transform( ds.value(i,1), yu );
            list.add( new Point2D.Double( x0, y0 ) );
            
        }
        
        c= ds.value(ds.length()-1, 2);
        while ( list.size()<3 ) {
            list.add( new Point2D.Double( x0, y0 ) );
        }
        popNext( g, list, c );
    }
    
}
