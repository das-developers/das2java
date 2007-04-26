/*
 * DasAnnotation.java
 *
 * Created on December 20, 2004, 2:32 PM
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.event.ArrowDragRenderer;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.event.MoveComponentMouseModule;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.JMenuItem;

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
        this.getMouseAdapter().addMenuItem( new JMenuItem( new AbstractAction( "remove" ) {
            public void actionPerformed( ActionEvent e ) {
                DasCanvas canvas= getCanvas();
                // TODO: confirm dialog
                canvas.remove( DasAnnotation.this );
                canvas.revalidate();
            }
        } ) );
        MouseModule mm= new MoveComponentMouseModule( this );
        this.getMouseAdapter().addMouseModule( mm );
        this.getMouseAdapter().setPrimaryModule( mm );
        
        mm= createArrowToMouseModule( this );
        this.getMouseAdapter().addMouseModule( mm );
        this.getMouseAdapter().setSecondaryModule( mm );
    }
    
    private MouseModule createArrowToMouseModule( final DasAnnotation anno ) {
        return new MouseModule( DasAnnotation.this, new ArrowDragRenderer(), "Point At" ) {
            Point head;
            Point tail;
            
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                tail= e.getPoint();
            }

            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                head= e.getPoint();
                head.translate( anno.getX(), anno.getY() );
                DasCanvasComponent c= parent.getCanvas().getCanvasComponentAt( head.x, head.y );
                if ( c instanceof DasPlot ) {
                    final DasPlot p= (DasPlot) c;
                    final Datum x= p.getXAxis().invTransform( head.x );
                    final Datum y= p.getYAxis().invTransform( head.y );
                    anno.setPointAt( new DasAnnotation.PointDescriptor() {
                        public Point getPoint() {
                            int ix= (int)(p.getXAxis().transform(x));
                            int iy= (int)(p.getYAxis().transform(y));
                            return new Point(ix,iy);
                        }
                    } );
                    setBounds( calcBounds() );
                }
            }

        };
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
        this.gtr.setString( this, this.string );
        setBounds( calcBounds() ) ;
    }
    
    private Rectangle calcBounds() {
        Rectangle bounds= gtr.getBounds();
        bounds.translate( getX(), getY() );
        if ( pointAt!=null ) {
            Point head= pointAt.getPoint();
            //bounds.add(head);
        }
        return bounds;
    }
    
    public void paintComponent( Graphics g1 ) {
        
        // TODO: need to draw based on row, col, not on bounds which may move with arrow.
        
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
