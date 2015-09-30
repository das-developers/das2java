/*
 * DasAnnotation.java
 *
 * Created on December 20, 2004, 2:32 PM
 */
package org.das2.graph;

import org.das2.util.GrannyTextRenderer;
import org.das2.datum.Datum;
import org.das2.event.ArrowDragRenderer;
import org.das2.event.MouseModule;
import org.das2.event.MoveComponentMouseModule;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;

/**
 * This makes a DasCanvasComponent for GrannyTextRenderer, and 
 * optionally adds an arrow to point at things.
 * 
 * TODO: See http://autoplot.org//developer.annotations
 * 
 * @author Jeremy
 */
public class DasAnnotation extends DasCanvasComponent {

    String templateString;
    GrannyTextRenderer gtr;

    /**
     * point at this thing
     */
    private DasAnnotation.PointDescriptor pointAt;
    private final MouseModule arrowToMouseModule;

    /** 
     * Create the annotation
     * @param string the message, which may contain %p which will be replaced with a label. */
    public DasAnnotation(String string) {
        super();
        this.gtr = new GrannyTextRenderer();
        this.templateString = string;

        Action removeArrowAction = new AbstractAction("remove arrow") {
            @Override
            public void actionPerformed(ActionEvent e) {
                pointAt = null;
                repaint();
            }
        };

        this.getDasMouseInputAdapter().addMenuItem(new JMenuItem(removeArrowAction));

        Action removeMeAction = new AbstractAction("remove") {
            @Override
            public void actionPerformed(ActionEvent e) {
                DasCanvas canvas = getCanvas();
                // TODO: confirm dialog
                canvas.remove(DasAnnotation.this);
                canvas.revalidate();
            }
        };

        this.getDasMouseInputAdapter().addMenuItem(new JMenuItem(removeMeAction));

        MouseModule mm = new MoveComponentMouseModule(this);
        this.getDasMouseInputAdapter().setPrimaryModule(mm);

        arrowToMouseModule = createArrowToMouseModule(this);
        this.getDasMouseInputAdapter().setSecondaryModule(arrowToMouseModule);
    }

    /**
     * create a PointDescriptor using and x and y Datum.
     */
    public static class DatumPairPointDescriptor implements PointDescriptor {

        DasPlot p;
        Datum x;
        Datum y;

        public DatumPairPointDescriptor(DasPlot p, Datum x, Datum y) {
            this.x = x;
            this.y = y;
            this.p = p;
        }

        @Override
        public Point getPoint() {
            int ix = (int) (p.getXAxis().transform(x));
            int iy = (int) (p.getYAxis().transform(y));
            return new Point(ix, iy);
        }

        @Override
        public String getLabel() {
            return "" + x + "," + y;
        }
    }

    private MouseModule createArrowToMouseModule(final DasAnnotation anno) {
        return new MouseModule(DasAnnotation.this, new ArrowDragRenderer(), "Point At") {

            Point head;
            Point tail;

            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                tail= e.getPoint();
                tail.translate(anno.getX(), anno.getY());
                Rectangle r= DasAnnotation.this.getActiveRegion().getBounds();
                if ( !r.contains(tail) ) {
                    tail= null;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);
                if ( tail==null ) return;
                head = e.getPoint();
                head.translate(anno.getX(), anno.getY());
                DasCanvasComponent c = parent.getCanvas().getCanvasComponentAt(head.x, head.y);
                if (c instanceof DasPlot) {
                    final DasPlot p = (DasPlot) c;
                    final Datum x = p.getXAxis().invTransform(head.x);
                    final Datum y = p.getYAxis().invTransform(head.y);
                    anno.setPointAt(new DatumPairPointDescriptor(p, x, y));
                    setBounds(calcBounds());
                }

            }
        };
    }

    /**
     * handle for the property text
     */
    public static final String PROP_TEXT = "text";
    
    /**
     * Set the text, which can be Granny Text.
     * @param string the text
     * @see GrannyTextRenderer
     */
    public void setText(String string) {
        String oldValue= this.templateString;
        this.templateString = string;
        if ( this.getGraphics()!=null ) {
            gtr.setString( this.getGraphics(), getString() );
            calcBounds();
        }
        firePropertyChange( PROP_TEXT, oldValue, string );
        repaint();

    }

    /**
     * get the text, which can be Granny Text.
     * @return the text.
     * @see GrannyTextRenderer
     */
    public String getText() {
        return templateString;
    }

    @Override
    public void resize() {
        super.resize();
        if ( this.getGraphics()!=null ) {
            Graphics g= this.getGraphics();
            if ( fontSize>0 ) g.setFont( getFont().deriveFont(fontSize) );
            this.gtr.setString( g, getString() );
            Rectangle r= calcBounds();
            r.add( r.x+r.width+1, r.y+r.height+1 );
            setBounds(r);
        }
    }

    @Override
    public Shape getActiveRegion() {
        Rectangle r = getAnnotationBubbleBounds();
        return r;
    }

    @Override
    public boolean acceptContext(int x, int y) {
	if ( getActiveRegion().contains( x, y ) ) {
	    return true;
	} else if ( pointAt!=null ) {
	    if ( pointAt.getPoint().distance(x,y) < 5 ) {
		return true;
	    }
	}
	return false;
    }

    /**
     * calculate the bounds in the canvas coordinate system.
     * @return 
     */
    private Rectangle calcBounds() {
        Rectangle r = (Rectangle)getActiveRegion();
        if (pointAt != null) {
            Point head = pointAt.getPoint();
            r.add(head);
        }
        
        return r;
    }

    @Override
    public void paintComponent(Graphics g1) {

        Graphics2D g = (Graphics2D) g1.create(); 
        
        g.translate( -getX(), -getY() );

        Color fore = g.getColor();

        Color canvasColor = getCanvas().getBackground();
        Color back = new Color(canvasColor.getRed(), canvasColor.getGreen(), canvasColor.getBlue(),
                80 * 255 / 100);
        if ( fontSize>0 ) g.setFont( getFont().deriveFont(fontSize) );

        int em = (int) getEmSize() / 2;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gtr.setString( g, getString() );
        Rectangle r;
        
        r= getAnnotationBubbleBounds();
        
        //r = new Rectangle(r.x - em + 1, r.y - em + 1, r.width + 2 * em - 1, r.height + 2 * em - 1);
        
        //r.translate( em, em + (int) gtr.getAscent());
        g.setColor(back);

        if (borderType == BorderType.RECTANGLE || borderType == BorderType.NONE) {
            g.fill(r);
        } else if (borderType == BorderType.ROUNDED_RECTANGLE) {
            g.fillRoundRect(r.x, r.y, r.width, r.height, em * 2, em * 2);
        }

        g.setColor(fore);
        
        gtr.draw(g, r.x+em, r.y + em + (float) gtr.getAscent() );

        if (pointAt != null) {
            double em2 = getCanvas().getFont().getSize();
            g.setStroke(new BasicStroke((float) (em2 / 8)));
            //g.drawLine( r.x, r.y+r.height, r.x+r.width, r.y+r.height );

            Point head = pointAt.getPoint();
            head.translate(-getColumn().getDMinimum(), -getRow().getDMinimum());
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setClip(null);
            //Arrow.paintArrow(g2, head, tail, em2);
            
            Point2D tail2d= new Point2D.Double( r.x + r.width/2, r.y + r.y + r.height/2 );
            Point2D head2d= new Point2D.Double( head.x, head.y );
            Rectangle2D rect2d= new Rectangle2D.Double(r.x, r.y, r.width, r.height );
            Point2D p2d= GraphUtil.lineRectangleIntersection( tail2d, head2d, rect2d );
            Point p= p2d==null ? head : new Point( (int)p2d.getX(), (int)p2d.getY() );
            Arrow.paintArrow(g2, head, p, em2, this.arrowStyle );
            g2.dispose();
        }

        if (borderType != BorderType.NONE) {
            if (borderType == BorderType.RECTANGLE) {
                g.draw(r);
            } else if (borderType == BorderType.ROUNDED_RECTANGLE) {
                g.drawRoundRect(r.x, r.y, r.width, r.height, em * 2, em * 2);
            }

        }

        /*r= DasColumn.toRectangle( getRow(), getColumn() );
        r.translate( -getX(), -getY() );
        r.width--;
        r.height--;
        ((Graphics2D)g1).draw( r );
         */
        
        g.dispose();
        
        getDasMouseInputAdapter().paint(g1);

    }

    /**
     * return the bounds in the canvas coordinate frame.
     * @return the bounds in the canvas coordinate frame.
     */
    private Rectangle getAnnotationBubbleBounds() {
        int em = (int) getEmSize() / 2;
        
        Rectangle r;
        r= gtr.getBounds();
        if ( anchorPosition==AnchorPosition.NW ) {
            r.x = getColumn().getDMinimum() + em;
            r.y = getRow().getDMinimum() + em;
        } else if ( anchorPosition==AnchorPosition.NE ) {
            r.x = getColumn().getDMaximum() - em - r.width;
            r.y = getRow().getDMinimum() +em;
        } else if ( anchorPosition==AnchorPosition.OutsideNE ) {
            r.x = getColumn().getDMaximum() + em;
            r.y = getRow().getDMinimum() +em;
        } else if ( anchorPosition==AnchorPosition.SW ) {
            r.x = getColumn().getDMinimum() + em;
            r.y = getRow().getDMaximum()-em-r.height;
        } else if ( anchorPosition==AnchorPosition.SE ) {
            r.x = getColumn().getDMaximum() - em - r.width;
            r.y = getRow().getDMaximum()-em-r.height;
        }
        
        r.x-= em;
        r.y-= em;
        r.width+= em*2;
        r.height+= em*2;
        
        return r;
    }

    /**
     * something to point at
     */
    public interface PointDescriptor {

        Point getPoint();

        String getLabel();
    }

    /**
     * set the thing to point at.  If %p will be replaced by p.getLabel()
     * @param p the thing.
     */
    public void setPointAt(PointDescriptor p) {
        this.pointAt = p;
        repaint();
    }

    /**
     * return the thing we are pointing at.
     * @return the thing we are pointing at.
     */
    public PointDescriptor getPointAt() {
        return this.pointAt;
    }

    private String getString() {
        String s = templateString;
        if (this.templateString != null && this.templateString.contains("%") && pointAt!=null ) {
            s = templateString.replace("%p", pointAt.getLabel() );
        }
        return s;
    }

    @Override
    protected void installComponent() {
        super.installComponent();
        this.gtr.setString( this.getFont(), getString() );
    }

    /**
     * the font size in points, or zero if we should use the canvas size.
     */
    float fontSize= 0.f;
    
    /**
     * the font size in points.  If zero, then use the canvas size.
     * @return the font size in points.
     */
    public float getFontSize() {
        return fontSize;
    }

    /**
     * the font size in pixels.
     */
    public static String PROP_FONT_SIZE= "fontSize";
    
    /**
     * override the canvas font size.  If zero, then use the canvas size, 
     * otherwise, use this size.
     * @param fontSize New value of property fontSize.
     */
    public void setFontSize(float fontSize) {
        float oldsize= this.fontSize;
        this.fontSize= fontSize;
        Font f = getFont();
        if (f == null) {
            if ( getCanvas()==null ) return;
            f = getCanvas().getBaseFont();
        }
        if ( fontSize>0 ) f= f.deriveFont(fontSize);
        Font newFont= f;
        Graphics g= this.getGraphics();
        if ( g==null ) return;
        g.setFont(newFont);
        gtr.setString( g, getString() );
        setBounds(calcBounds());
        repaint();

        firePropertyChange( PROP_FONT_SIZE, oldsize, fontSize );
    }

    
    /**
     * the current border type.
     */
    private BorderType borderType = BorderType.NONE;
    
    /**
     * handle for the property borderType
     */
    public static final String PROP_BORDERTYPE = "borderType";

    /**
     * the border type
     * @return the border type
     */
    public BorderType getBorderType() {
        return this.borderType;
    }

    /**
     * set the border type
     * @param newborderType the border type
     */
    public void setBorderType(BorderType newborderType) {
        BorderType oldborderType = borderType;
        this.borderType = newborderType;
        repaint();

        firePropertyChange(PROP_BORDERTYPE, oldborderType, newborderType);
    }
    
    private AnchorPosition anchorPosition = AnchorPosition.NW;

    public static final String PROP_ANCHORPOSITION = "anchorPosition";

    public AnchorPosition getAnchorPosition() {
        return anchorPosition;
    }

    public void setAnchorPosition(AnchorPosition anchorPosition) {
        AnchorPosition oldAnchorPosition = this.anchorPosition;
        this.anchorPosition = anchorPosition;
        firePropertyChange(PROP_ANCHORPOSITION, oldAnchorPosition, anchorPosition);
    }

    private Arrow.HeadStyle arrowStyle = Arrow.HeadStyle.DRAFTING;

    public static final String PROP_ARROWSTYLE = "arrowStyle";

    /**
     * get the arrow style 
     * @return the arrow style
     */
    public Arrow.HeadStyle getArrowStyle() {
        return this.arrowStyle;
    }

    /**
     * set the arrow style 
     * @param newarrowStyle the arrow style
     */
    public void setArrowStyle( Arrow.HeadStyle newarrowStyle) {
        Arrow.HeadStyle oldarrowStyle = arrowStyle;
        this.arrowStyle = newarrowStyle;
        repaint();
        firePropertyChange(PROP_ARROWSTYLE, oldarrowStyle, newarrowStyle);
    }

    
}
