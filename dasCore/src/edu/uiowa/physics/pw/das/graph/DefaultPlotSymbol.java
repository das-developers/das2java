/*
 * DefaultPlotSymbol.java
 *
 * Created on July 2, 2007, 3:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.DasProperties;
import edu.uiowa.physics.pw.das.components.propertyeditor.Displayable;
import edu.uiowa.physics.pw.das.components.propertyeditor.Enumeration;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * @author jbf
 */
public class DefaultPlotSymbol implements Enumeration, Displayable, PlotSymbol, PathIterable {
    
    GeneralPath path;
    String label;
    Icon icon;
    
    public DefaultPlotSymbol( Shape p, String label ) {
        this.path= new GeneralPath(p);
        this.label= label;
    }
    
    public void draw(Graphics2D g, double x, double y, float size, FillStyle style ) {
        AffineTransform at= AffineTransform.getScaleInstance(size,size);
        at.translate(x/size,y/size);
        if ( style==FillStyle.STYLE_FILL ) {
            g.fill( path.createTransformedShape( at ) );
        } else if ( style==FillStyle.STYLE_OUTLINE ) {
            Color back= g.getBackground();
            Color fore= g.getColor();
            g.setColor( back );
            g.fill( path.createTransformedShape( at ) );
            g.setColor( fore );
            g.draw( path.createTransformedShape( at ) );
        } else if ( style==FillStyle.STYLE_DRAW ) {
            g.draw( path.createTransformedShape( at ) );
        }
    }
    
    public PathIterator pathIterator( AffineTransform at ) {
        return path.getPathIterator(at);
    }

    public Icon getListIcon() {
        if ( icon==null ) {
            Image i= new BufferedImage(10,10,BufferedImage.TYPE_INT_RGB);
            Graphics2D g= ( Graphics2D) i.getGraphics();
            g.setBackground( Color.white );
            g.setRenderingHints(DasProperties.getRenderingHints());
            g.setColor( Color.white );
            g.fillRect(0,0, 10,10);
            g.setColor( Color.black );
            this.draw(g,5,5,6.f,FillStyle.STYLE_OUTLINE );
            this.icon= new ImageIcon(i);
        }
        return icon;
    }


    public String getListLabel() {
        return label;
    }
    
    public String toString() {
        return label;
    }
    
    public static final DefaultPlotSymbol CIRCLES = new DefaultPlotSymbol( new Ellipse2D.Double( -0.5, -0.5, 1., 1. ), "circles" );
    
    public static final DefaultPlotSymbol TRIANGLES;
    static {
        GeneralPath triangle= new GeneralPath();
        float size= 0.7f;
        triangle.moveTo( 0f, -size );
        triangle.lineTo( size, size );
        triangle.lineTo( -size, size );
        triangle.lineTo( 0f, -size );
        TRIANGLES= new DefaultPlotSymbol( triangle, "triangles" );
    }
    
    
    public static final DefaultPlotSymbol CROSS;
    static {
        GeneralPath path= new GeneralPath();
        float size=0.6f;
        path.moveTo( 0, -size );
        path.lineTo( 0, size );
        path.moveTo( -size, 0 );
        path.lineTo( size, 0 );
        CROSS= new DefaultPlotSymbol( path, "crosses" );
    }
    
    public static final DefaultPlotSymbol EX;
    static {
        GeneralPath path= new GeneralPath();
        float size=0.4f;
        path.moveTo( -size, -size );
        path.lineTo( size, size );
        path.moveTo( -size, size );
        path.lineTo( size, -size );
        EX= new DefaultPlotSymbol( path, "exes" );
    }
    
    public static final DefaultPlotSymbol STAR;
    static {
        GeneralPath path= new GeneralPath();
        for ( int i=0; i<11; i++ ) {
            double radius= ( i % 2 == 0 ) ? 1 : 0.5;
            float x= (float)( radius * Math.sin( Math.PI * i * 36 / 180 ) );
            float y= (float)( radius * -1 * Math.cos( Math.PI * i * 36 / 180 ) );
            if ( i==0 ) path.moveTo( x, y ); else path.lineTo( x, y );
        }
        STAR= new DefaultPlotSymbol( path, "stars" );
    }
    
    public static final DefaultPlotSymbol BOX= new DefaultPlotSymbol( new Rectangle.Double( -0.5, -0.5, 1, 1 ), "boxes" );
    
    // temporary //
    public static final DefaultPlotSymbol NONE= new DefaultPlotSymbol( new GeneralPath(), "none" );
    
}
