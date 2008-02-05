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
import javax.swing.Action;
import javax.swing.JMenuItem;

/**
 * This component-izes a GrannyTextRenderer, and composes with an Arrow.
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

        Action removeArrowAction= new AbstractAction( "remove arrow" ) {
            public void actionPerformed( ActionEvent e ) {
                pointAt= null;
                repaint();
            }
        };
        
        this.getMouseAdapter().addMenuItem( new JMenuItem( removeArrowAction ) );
        
        Action removeMeAction= new AbstractAction( "remove" ) {
            public void actionPerformed( ActionEvent e ) {
                DasCanvas canvas= getCanvas();
                // TODO: confirm dialog
                canvas.remove( DasAnnotation.this );
                canvas.revalidate();
            }
        };
        
        this.getMouseAdapter().addMenuItem( new JMenuItem( removeMeAction ) );
        
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
        gtr.setString( this.getGraphics(), string );
        this.string= string;
        calcBounds();
        repaint();
    }
    
    public String getText( ) {
        return this.string;
    }
    
    
    public void resize() {
        super.resize();
        this.gtr.setString( this.getGraphics(), this.string );
        setBounds( calcBounds() ) ;
    }
    
    private Rectangle calcBounds() {
        Rectangle r= gtr.getBounds();
        int em= (int)getEmSize() / 2;
        r= new Rectangle( r.x-em-2, r.y-em-2, r.width+2*em+3, r.height+2*em+3 );
        r.translate( getX(), getY() );
        if ( pointAt!=null ) {
            Point head= pointAt.getPoint();
            r.add(head);
        }
        return r;
    }
    
    public void paintComponent( Graphics g1 ) {
        
        // TODO: need to draw based on row, col, not on bounds which may move with arrow.
        
        Graphics2D g= (Graphics2D)g1;
        
        Color fore= g.getColor();
        
        Color canvasColor= getCanvas().getBackground();
        Color back= new Color( canvasColor.getRed(), canvasColor.getGreen(), canvasColor.getBlue(),
           80 * 255 / 100 );
        
        int em= (int)getEmSize() / 2;
        
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        Rectangle r= gtr.getBounds();
        r= new Rectangle( r.x-em+1, r.y-em+1, r.width+2*em-1, r.height+2*em-1 );
        
        r.translate( em, em + (int)gtr.getAscent() );
        g.setColor( back );

       if ( borderType!=BorderType.NONE ) {
            if ( borderType==BorderType.RECTANGLE ) {
                g.fill(r);
            } else if (borderType==BorderType.ROUNDED_RECTANGLE ) {
                g.fillRoundRect( r.x, r.y, r.width, r.height, em*2, em*2 );
            }
        }
        
        g.setColor( fore );
        gtr.draw( g, em, em+(float)gtr.getAscent() );
        
        if ( pointAt!=null ) {
            double em2 = getCanvas().getFont().getSize();
            g.setStroke( new BasicStroke( (float)(em2/8) ) );
            //g.drawLine( r.x, r.y+r.height, r.x+r.width, r.y+r.height );
        
            Point head= pointAt.getPoint();
            head.translate( -getX(), -getY() );
            int tx= Math.min( head.x, r.x+r.width*2/3 );
            tx= Math.max( tx, r.x+r.width*1/3 );
            Point tail= new Point( tx, r.y+r.height );
            Graphics2D g2= (Graphics2D)g.create();
            g2.setClip(null);
            Arrow.paintArrow( g2, head, tail, em2 );
        }
        
        if ( borderType!=BorderType.NONE ) {
            if ( borderType==BorderType.RECTANGLE ) {
                g.draw( r );
            } else if (borderType==BorderType.ROUNDED_RECTANGLE ) {
                g.drawRoundRect( r.x, r.y, r.width, r.height, em*2, em*2 );
            }
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
    
    public enum BorderType { NONE, RECTANGLE, ROUNDED_RECTANGLE }
    
    private BorderType borderType = BorderType.NONE;

    public static final String PROP_BORDERTYPE = "borderType";

    public BorderType getBorderType() {
        return this.borderType;
    }

    public void setBorderType(BorderType newborderType) {
        BorderType oldborderType = borderType;
        this.borderType = newborderType;
        repaint();
        propertyChangeSupport.firePropertyChange(PROP_BORDERTYPE, oldborderType, newborderType);
    }

    private java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

    public void addPropertyChangeListener(java.beans.PropertyChangeListener listener )
    {
        propertyChangeSupport.addPropertyChangeListener( listener );
    }

    public void removePropertyChangeListener(java.beans.PropertyChangeListener listener )
    {
        propertyChangeSupport.removePropertyChangeListener( listener );
    }

}
