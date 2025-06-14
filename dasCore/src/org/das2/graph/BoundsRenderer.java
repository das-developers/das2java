
package org.das2.graph;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Map;
import org.das2.datum.UnitsUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.examples.Schemes;
import org.das2.qds.ops.Ops;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.qds.SemanticOps;

/**
 * Draw the region bounded by the dataset.  If the dataset is a bounding box, the box is drawn.  If the
 * data is a rank 2 time series with bins (N by 2), then it is drawn.  This supports the following
 * types:<ul>
 * <li> bounding box
 * <li> array of bounding box
 * <li> array of bins
 * </ul>
 * @author jbf
 * @see org.das2.qds.examples.Schemes#isBoundingBox(org.das2.qds.QDataSet) 
 */
public class BoundsRenderer extends Renderer {

    private void expectDs() {
        getParent().postException( this, new IllegalArgumentException("Expect rank 2 bins or rank 3 array of bins dataset") );
    }

    @Override
    public boolean acceptsDataSet(QDataSet ds) {
        if ( ds.rank()==1 || Schemes.isBoundingBox(ds) || Schemes.isArrayOfBoundingBox(ds) ) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean acceptContext(int x, int y) {
        return context==null ? false : context.contains(x, y);
    }
    
    public Area selectionArea() {
        if ( context==null ) {
            return null;
        } else {            
            return new Area(context);
        }
    }

    @Override
    public Icon getListIcon() {
        Image i = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();

        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        
        Shape pbox= new Rectangle2D.Double(0,0,15,15);
        
        GeneralPath p= new GeneralPath();
        p.append( pbox.getPathIterator(null), true );
        p.closePath();
        
        GraphUtil.fillWithTexture(g, p, fillColor, fillTexture );
        g.draw(p);
            
        return new ImageIcon(i);
    }

    @Override
    public String getListLabel() {
        return "" + ( getLegendLabel().length()> 0 ? getLegendLabel() +" " : "bounds" );
    }
    
    
    private GeneralPath context=null;
    
    public static QDataSet doAutorange( QDataSet ds ) {
        if ( ds.length()==0 ) {
            return null;
        }
        if ( Schemes.isBoundingBox(ds) ) {
            QDataSet xxx= Ops.slice0( ds,0 );
            QDataSet yyy= Ops.slice0( ds,1 );
            xxx= Ops.rescaleRangeLogLin( xxx, -0.1, 1.1 );
            yyy= Ops.rescaleRangeLogLin( yyy, -0.1, 1.1 );
            return Ops.join( xxx, yyy );            
        } else if ( Schemes.isArrayOfBoundingBox(ds) ) {
            QDataSet xx=null;
            QDataSet yy=null;
            for ( int i=0; i<ds.length(); i++ ) {
                QDataSet ds1= ds.slice(i);
                QDataSet xxx= Ops.slice0( ds1,0 );
                QDataSet yyy= Ops.slice0( ds1,1 );
                xx= ( xx==null ? xxx : Ops.extent( xx, xxx ) );
                yy= ( yy==null ? yyy : Ops.extent( yy, yyy ) );
            }
            xx= Ops.rescaleRangeLogLin( xx, -0.1, 1.1 );
            yy= Ops.rescaleRangeLogLin( yy, -0.1, 1.1 );
            return Ops.join( xx, yy );
        } else {
            QDataSet xxx= Ops.slice1( ds,0 );
            QDataSet yyy= Ops.slice1( ds,1 );
            QDataSet yext= Ops.extent( yyy );
            QDataSet xext= Ops.extent( xxx );
            yext= Ops.rescaleRangeLogLin( yext, -0.1, 1.1 );
            xext= Ops.rescaleRangeLogLin( xext, -0.1, 1.1 );
            return Ops.join( xext, yext );
        }
    }
    
    
    private Color color = new Color( 0, 0, 0 );

    private Color fillColor= new Color( 0, 0, 0, 128 );
    
    public static final String PROP_COLOR = "color";

    public static final String PROP_FILL_COLOR = "fillColor";
        @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "fillColor", encodeColorControl(fillColor) );
        controls.put( "color", encodeColorControl(color) );
        if ( !fillTexture.isEmpty() ) controls.put( "fillTexture", fillTexture );
        if ( polar ) controls.put( "polar", encodeBooleanControl( polar ) );
        return Renderer.formatControl(controls);
    }
    

    @Override
    public void setControl(String s) {
        String oldControl= getControl();
        super.setControl(s);
        this.color= getColorControl( "color", color );
        this.fillColor= getColorControl( "fillColor", fillColor );
        this.fillTexture= getControl( "fillTexture", fillTexture );
        this.polar= getBooleanControl( "polar", false );
        if ( !oldControl.equals(s) ) {
            updateCacheImage();
        }
    }    
    
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        Color old = this.color;
        this.color = color;
        if ( !color.equals(old) ) {
            updateCacheImage();
        }
        propertyChangeSupport.firePropertyChange(PROP_COLOR, old, color);
    }

    public Color getFillColor() {
        return fillColor;
    }
    
    public void setFillColor( Color color ) {
        Color old = this.fillColor;
        this.fillColor = color;
        if ( !color.equals(old) ) {
            updateCacheImage();
        }
        propertyChangeSupport.firePropertyChange(PROP_COLOR, old, color );
    }
    
    private String fillTexture = "solid"; // solid, hash, none

    public static final String PROP_FILLTEXTURE = "fillTexture";

    public String getFillTexture() {
        return fillTexture;
    }

    public void setFillTexture(String fillTexture) {
        String old = this.fillTexture;
        this.fillTexture = fillTexture;
        if ( !old.equals(fillTexture) ) {
            updateCacheImage();
        }
        propertyChangeSupport.firePropertyChange(PROP_FILLTEXTURE, old, fillTexture);
    }

    private boolean polar = false;

    public static final String PROP_POLAR = "polar";

    public boolean isPolar() {
        return polar;
    }

    /**
     * if true then transform render the data in polar coordinates
     * @param polar 
     */
    public void setPolar(boolean polar) {
        boolean old = this.polar;
        this.polar = polar;
        if ( old!=polar ) {
            updateCacheImage();
        }
        propertyChangeSupport.firePropertyChange(PROP_POLAR, old, polar);
    }

    @Override
    public void render(Graphics2D g1, DasAxis xAxis, DasAxis yAxis ) {
        Graphics2D g= (Graphics2D)g1;
        QDataSet d= this.getDataSet();
        if ( d==null ) {
            if ( getLastException()!=null ) {
                renderException(g, xAxis, yAxis, lastException);
            } else {
                getParent().postMessage(this, "no data set", DasPlot.INFO, null, null);
            }
            return;
        }
        if ( d.length()==0 ) return;
        if ( !( d.rank()==1 || d.rank()==2 || d.rank()==3 ) ) {
            expectDs();
            return;
        }
        QDataSet mins;
        QDataSet maxs;
        if ( Schemes.isBoundingBox(d) || Schemes.isBoundingBox(d.slice(0) )) {
            if ( d.rank()==2 ) { 
                // make it a single-element rank 3
                d= Ops.join( null, d );
            }
            GeneralPath pbox= new GeneralPath();
            
            for ( int i=0; i<d.length(); i++ ) {
            //mins= Ops.dataset( ds.slice(0).slice(0), ds.slice(1).slice(0) )
                QDataSet d1= d.slice(i);
                Path2D.Double p= new Path2D.Double();
                if ( polar ) { 
                    double x0= xAxis.transform(0,xAxis.getUnits());
                    double y0= yAxis.transform(0,yAxis.getUnits());
                    QDataSet radDs= d1.slice(1); // 2.8830 to 22.495 
                    double v1= radDs.slice(0).value();
                    double v2= radDs.slice(1).value();
                    QDataSet angds= d1.slice(0); // 22.363 to 78.915
                    double a0= Ops.datum( angds.slice(0) ).value( ); // Units.radians );
                    double a1= Ops.datum( angds.slice(1) ).value( ); // Units.radians );
                    double r0x= ( xAxis.transform(v1,xAxis.getUnits()) ) - x0; // inner ring radius at y=0
                    double r0y= y0 - ( yAxis.transform(v1,yAxis.getUnits()) ); // inner ring radius at x=0, equal to r0x when isotropic (round)
                    double r1x= ( xAxis.transform(v2,xAxis.getUnits()) ) - x0; // outer ring radius at y=0
                    double r1y= y0 - ( yAxis.transform(v2,yAxis.getUnits()) ); // outer ring radius at x=0, equal to r1x when isotropic (round)

                    //GeneralPath gp= new GeneralPath( GeneralPath.WIND_NON_ZERO,6);

                    p.moveTo( x0 + cos(a0) * r0x,  y0 - sin(a0) * r0y );
                    p.lineTo( x0 + cos(a0) * r1x,  y0 - sin(a0) * r1y );

                    Arc2D arc0 = new Arc2D.Double( x0-r1x, y0-r1y, r1x*2, r1y*2, Math.toDegrees(a0), Math.toDegrees(a1-a0), Arc2D.OPEN );
                    p.append( arc0.getPathIterator(null), true );
                    //p.lineTo( x0 + cos(a1) * r1x, y0 - sin(a1) * r1y );
                    p.lineTo( x0 + cos(a1) * r0x, y0 - sin(a1) * r0y );

                    Arc2D arc1 = new Arc2D.Double( x0-r0x, y0-r0y, r0x*2, r0y*2, Math.toDegrees(a1), Math.toDegrees(a0-a1), Arc2D.OPEN );
                    p.append( arc1.getPathIterator(null), true );
                    p.lineTo( x0 + cos(a0) * r0x,  y0 - sin(a0) * r0y );

                } else {
                    double x0= xAxis.transform(d1.slice(0).slice(0));
                    double x1= xAxis.transform(d1.slice(0).slice(1));
                    double y0= yAxis.transform(d1.slice(1).slice(0));
                    double y1= yAxis.transform(d1.slice(1).slice(1));
                    p.moveTo( x0, y1 );
                    p.lineTo( x1, y1 );
                    p.lineTo( x1, y0 );
                    p.lineTo( x0, y0 );
                    p.lineTo( x0, y1 );
                }
                pbox.append( p, false );

            }
            GraphUtil.fillWithTexture(g, pbox, fillColor, fillTexture);
            g.setColor( this.getColor() );
            g.draw(pbox);
            context= pbox;
        } else if ( d.rank()==1 ) {
            QDataSet xx= SemanticOps.xtagsDataSet(d);
            QDataSet yy= d;
            GeneralPath path= GraphUtil.getPath( xAxis,yAxis,
                Ops.link( xx, yy ), false,false);
            path.closePath();
            GraphUtil.fillWithTexture(g, path, fillColor, fillTexture);
            g.setColor( this.getColor() );
            g.draw(path);
            context= path;
        } else if ( Schemes.isTrajectory(ds) ) { 
            QDataSet xx= Ops.slice1( ds, 0 );
            QDataSet yy= Ops.slice1( ds, 1 );
            GeneralPath path= GraphUtil.getPath( xAxis,yAxis,
                xx, yy, false,false);
            path.closePath();
            GraphUtil.fillWithTexture(g, path, this.fillColor, this.fillTexture);
            g.setColor( this.getColor() );
            g.draw(path);
            context= path;
            
        } else {
            mins= Ops.slice1( d,0 );
            maxs= Ops.slice1( d,1 );
            if ( mins.property(QDataSet.UNITS)==null && UnitsUtil.isRatioMeasurement(yAxis.getUnits()) ) {
                mins= Ops.putProperty( mins, QDataSet.UNITS, yAxis.getUnits() );
            }
            if ( maxs.property(QDataSet.UNITS)==null && UnitsUtil.isRatioMeasurement(yAxis.getUnits()) ) {
                maxs= Ops.putProperty( maxs, QDataSet.UNITS, yAxis.getUnits() );
            }       
            QDataSet s1= mins.trim(0,1);
            GeneralPath path= GraphUtil.getPath(xAxis,yAxis,
                Ops.append(mins,Ops.append(Ops.reverse(maxs),s1)),false,false);
            path.closePath();
            GraphUtil.fillWithTexture(g, path, this.fillColor, fillTexture );
            g.setColor( this.getColor() );
            g.draw(path);
            context= path;
        }
        
    }

    
    
}
