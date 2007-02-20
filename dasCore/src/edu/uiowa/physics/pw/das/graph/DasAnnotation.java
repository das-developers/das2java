/*
 * DasAnnotation.java
 *
 * Created on December 20, 2004, 2:32 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;

/**
 * This component-izes a GrannyTextRenderer, composes with an Arrow, and
 * @author Jeremy
 */
public class DasAnnotation extends DasCanvasComponent {
    
    String string;
    GrannyTextRenderer gtr;
    
    /**
     * point at this thing
     */
    private DasAnnotation.PointDescriptor pointAt;
    
    /** Creates a new instance of DasAnnotation */
    public DasAnnotation( String string ) {
        super();
        this.gtr= new GrannyTextRenderer();
        this.string= string;
    }
    
    public void setText( String string ) {
        gtr.setString(this, string);
        this.string= string;
        repaint();
    }
    
    public String getText( ) {
        return this.string;
    }
    
    
    public void resize() {
        super.resize();
        setBounds( DasDevicePosition.toRectangle( getRow(), getColumn() ) ) ;
    }
    
    public void paintComponent( Graphics g1 ) {
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        Rectangle r= gtr.getBounds();
        r.translate( 0,(int)gtr.getAscent() );
        g.setColor( new Color(230,230,230,100) );
        g.fill(r);
        g.setColor( Color.black );
        gtr.draw(g,0,(float)gtr.getAscent());
        
        if ( pointAt!=null ) {
            double em = getCanvas().getFont().getSize();
            
            g.setStroke( new BasicStroke( (float)(em/8) ) );
            g.drawLine( r.x, r.y+r.height, r.x+r.width, r.y+r.height );
        
            Point head= pointAt.getPoint();
            head.translate( -getX(), -getY() );
            int tx= Math.min( head.x, r.x+r.width*2/3 );
            tx= Math.max( tx, r.x+r.width*1/3 );
            Point tail= new Point( tx, r.y+r.height );
            Graphics2D g2= (Graphics2D)g.create();
            g2.setClip(null);
            Arrow.paintArrow( g2, head, tail, em );
        }
        getMouseAdapter().paint(g);
    }
    
    public interface PointDescriptor {
        Point getPoint();
    }
    
    public void setPointAt( PointDescriptor p  ) {
        this.pointAt= p;
    }
    
    public PointDescriptor getPointAt() {
        return this.pointAt;
    }
    
    protected void installComponent() {
        super.installComponent();
        this.gtr.setString( this, this.string );
    }
    
    /**
     * Getter for property fontSize.
     * @return Value of property fontSize.
     */
    public float getFontSize() {
        return getFont().getSize();
    }
    
    /**
     * Setter for property fontSize.
     * @param fontSize New value of property fontSize.
     */
    public void setFontSize(float fontSize) {
        setFont( getFont().deriveFont(fontSize) );
        repaint();
    }
}
