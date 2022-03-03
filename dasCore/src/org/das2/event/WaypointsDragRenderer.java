
package org.das2.event;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.DatumRangeUtil;
import org.das2.graph.DasAxis;
import org.das2.graph.DasCanvasComponent;
import org.das2.qds.IDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;
import org.das2.util.LoggerManager;

/**
 * draw a pathway of a given width including a set of waypoints.
 * @author jbf
 */
public class WaypointsDragRenderer extends AbstractDragRenderer {

    /**
     * draws a boxed region of some vertical width along a path.
     * @param parent
     */
    public WaypointsDragRenderer( DasCanvasComponent parent ) {
        super( parent );
    }
        
    Point2D.Double pointerStart=null;
    Point2D.Double pointerLocation= null;
    
    List<Point2D.Double> wayPoints= new ArrayList<>();
    
    private int width = 20;

    public static final String PROP_WIDTH = "width";

    private static final Logger logger= LoggerManager.getLogger("das2.gui.dmia");
    
    /**
     * get the vertical width in pixels.
     * @return the vertical width 
     */
    public int getWidth() {
        return width;
    }

    /**
     * set the vertical width in pixels.
     * @param width 
     */
    public void setWidth(int width) {
        this.width = width;
        getParent().repaint();
    }

    @Override
    public void clear(Graphics g) {
        wayPoints.clear();
        pointerLocation=null;
        getParent().repaint();
    }

    /**
     * add the current location, recorded from the last renderDrag operation, to the list of way points.
     */
    public void addWayPoint(  ) {
        if ( pointerLocation!=null ) {
            this.wayPoints.add( pointerLocation );
        } else {
            logger.info( "no pointer location");
        }
    }
    
    @Override
    public Rectangle[] renderDrag( Graphics g1, Point p1, Point p2 ) {
        Graphics2D g= (Graphics2D) g1;
        
        GeneralPath gen= new GeneralPath();
        
        gen.moveTo( p1.x, p1.y - width );
        
        
        for ( int i=0; i<wayPoints.size(); i++ ) {
            gen.lineTo( wayPoints.get(i).x, wayPoints.get(i).y-width );
        }
        
        gen.lineTo( p2.x, p2.y-width );
        
        gen.lineTo( p2.x, p2.y+width );

        for ( int i=wayPoints.size()-1; i>=0; i-- ) {
            gen.lineTo( wayPoints.get(i).x, wayPoints.get(i).y+width );
        }
        
        gen.lineTo( p1.x, p1.y + width );
        
        gen.closePath();
        
        Color color0= g.getColor();
        g.setColor(new Color(255,255,255,100));
        g.setStroke(new BasicStroke( 3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ));
        
        g.draw( gen );
        
        g.setStroke(new BasicStroke());
        g.setColor(color0);
        
        g.draw( gen );
        
        pointerStart= new Point2D.Double( p1.x, p1.y );
        pointerLocation= new Point2D.Double( p2.x, p2.y );
        
        return new Rectangle[] {  };
    }

    /**
     * returns a list of indices where the rank 1 yy is within the pathway.  This breaks the package.html guidance
     * that this not work with data and units, and may change.
     * @param xaxis the axis for horizontal transforms
     * @param yaxis the axis for vertical transforms
     * @param xx the rank 0 point for the data
     * @param yy the rank 1 data points
     * @return a list of indices into yy.
     */
    protected QDataSet whereWithin( DasAxis xaxis, DasAxis yaxis, QDataSet xx, QDataSet yy ) {
        if ( xx.rank()==0 ) {
            int index= -1;
            double ix= xaxis.transform(xx);
            
            if ( ix<pointerStart.x ) return IDataSet.createRank1(0);
            
            for ( int i=0; i<wayPoints.size(); i++ ) {
                if ( ix<wayPoints.get(i).x ) {
                    index= i;
                    break;
                }
            }
            if ( index==-1 ) {
                if ( ix<pointerLocation.x ) {
                    index= wayPoints.size();
                }
            }
            if ( yy.rank()==0 ) {
                yy= Ops.join(null,yy);
            }
            if ( yy.rank()==1 ) {
                double alpha; // the normalized location between the two way points
                double y;
                if ( index==0 && wayPoints.isEmpty() ) {
                    alpha= ( ix - pointerStart.x ) / ( pointerLocation.x - pointerStart.x );
                    y= pointerStart.y + alpha * ( pointerLocation.y - pointerStart.y );
                } else if ( index==0 ) {
                    alpha= ( ix - pointerStart.x ) / ( wayPoints.get(index).x - pointerStart.x );
                    y= pointerStart.y + alpha * ( wayPoints.get(index).y - pointerStart.y );
                } else if ( index==wayPoints.size() ) {
                    alpha= ( ix - wayPoints.get(index-1).x ) / ( pointerLocation.x - wayPoints.get(index-1).x );
                    y=  wayPoints.get(index-1).y + alpha * ( pointerLocation.y - wayPoints.get(index-1).y );
                } else if ( index>0 && index<wayPoints.size() ) {
                    alpha= ( ix - wayPoints.get(index-1).x ) / (  wayPoints.get(index).x - wayPoints.get(index-1).x );
                    y=  wayPoints.get(index-1).y + alpha * ( wayPoints.get(index).y - wayPoints.get(index-1).y );
                } else {
                    return IDataSet.createRank1(0);
                }
                double ymin= y - width;
                double ymax= y + width;
                Datum dymin= yaxis.invTransform( ymin );
                Datum dymax= yaxis.invTransform( ymax );
                QDataSet ww= Ops.within( yy, DatumRangeUtil.union( dymin, dymax ) );
                return Ops.where( ww );
            } else {
                throw new IllegalArgumentException("yy must be rank 1");
            }
        } else {
            throw new IllegalArgumentException("xx must be rank 0");
        } 
    }

    /**
     * return a rectangle that bounds the data.
     * @return 
     */
    public Rectangle getBoundingBox() {
        double ymin= pointerStart.y;
        double ymax= pointerStart.y;
        for ( int i=0; i<wayPoints.size(); i++ ) {
            Point2D.Double d= wayPoints.get(i);
            ymin = Math.min( ymin, d.y );
            ymax = Math.max( ymax, d.y );
        }
        ymin = Math.min( ymin, pointerLocation.y );
        ymax = Math.min( ymax, pointerLocation.y );
        Rectangle result= new Rectangle( (int)pointerStart.x,
            (int)(ymin-width), 
            (int)(pointerLocation.x - pointerStart.x), 
            (int)(ymax-ymin+width*2) );
        return result;
    }
}
