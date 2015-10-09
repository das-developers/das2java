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
import java.text.ParseException;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.das2.datum.DatumRange;
import org.das2.datum.LoggerManager;
import org.das2.datum.Units;

/**
 * This makes a DasCanvasComponent for GrannyTextRenderer, and 
 * optionally adds an arrow to point at things.
 * 
 * TODO: See http://autoplot.org//developer.annotations
 * 
 * @author Jeremy
 */
public class DasAnnotation extends DasCanvasComponent {

    private static final Logger logger= LoggerManager.getLogger("das.graph.annotation");
    
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
        if ( this.getGraphics()!=null ) {
            super.resize();
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
        if ( anchorBorderType!=BorderType.NONE ) {
            Rectangle anchorRect= getAnchorBounds();
            r.add(anchorRect);
        }
        
        return r;
    }

    @Override
    public void paintComponent(Graphics g1) {

        Graphics2D g = (Graphics2D) g1.create(); 
        
        g.translate( -getX(), -getY() );

        Color fore = getCanvas().getForeground();
        Color ltextColor= fore;
        Color back= getCanvas().getBackground();
        
        if ( isOverrideColors() ) {
            fore= getForeground();
            ltextColor= textColor;
            back= getBackground();
        }
                
        if ( fontSize>0 ) g.setFont( getFont().deriveFont(fontSize) );

        int em = (int) getEmSize() / 2;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gtr.setString( g, getString() );
        Rectangle r;
        
        r= getAnnotationBubbleBounds();
        
        if ( anchorPosition==AnchorPosition.N || anchorPosition==AnchorPosition.OutsideN ) {
            gtr.setAlignment( GrannyTextRenderer.CENTER_ALIGNMENT );
        }
        
        //r = new Rectangle(r.x - em + 1, r.y - em + 1, r.width + 2 * em - 1, r.height + 2 * em - 1);
        
        //r.translate( em, em + (int) gtr.getAscent());
        g.setColor(back);

        if (borderType == BorderType.RECTANGLE || borderType == BorderType.NONE) {
            g.fill(r);
        } else if (borderType == BorderType.ROUNDED_RECTANGLE) {
            g.fillRoundRect(r.x, r.y, r.width, r.height, em * 2, em * 2);
        }

        g.setColor(ltextColor);

        gtr.draw(g, r.x+em, r.y + em + (float) gtr.getAscent() );

        g.setColor(fore);
        
        if (pointAt != null) {
            double em2 = g.getFont().getSize();
            //g.setStroke(new BasicStroke((float) (em2 / 8)));
            //g.drawLine( r.x, r.y+r.height, r.x+r.width, r.y+r.height );

            Point head = pointAt.getPoint();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setClip(null);
            //Arrow.paintArrow(g2, head, tail, em2);
            
            Point2D tail2d= new Point2D.Double( r.x + r.width/2, r.y + r.height/2 );
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

        if ( anchorBorderType!=BorderType.NONE ) {
            Rectangle anchorRect= getAnchorBounds();
            if ( anchorBorderType== BorderType.RECTANGLE ) {
                g.draw(anchorRect);
            } else if ( anchorBorderType==BorderType.ROUNDED_RECTANGLE ) {
                g.drawRoundRect(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height, em * 2, em * 2);
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
     * return the bounds of that thing we are anchored to.
     * @return 
     */
    private Rectangle getAnchorBounds() {
        Rectangle anchorRect= new Rectangle();
        if ( anchorType==AnchorType.DATA && plot!=null && xrange!=null && yrange!=null ) {
            anchorRect.x= (int)(plot.getXAxis().transform(xrange.min()));
            anchorRect.y= (int)(plot.getYAxis().transform(yrange.min()));
            int x1= (int)(plot.getXAxis().transform(xrange.max()));
            int y1= (int)(plot.getYAxis().transform(yrange.max()));
            if ( x1<anchorRect.x ) {
                int t= anchorRect.x;
                anchorRect.x= x1;
                x1= t;
            }
            if ( y1<anchorRect.y ) {
                int t= anchorRect.y;
                anchorRect.y= y1;
                y1= t;
            }
            anchorRect.width= x1- anchorRect.x;
            anchorRect.height= y1- anchorRect.y;
            
        } else {
            anchorRect= DasDevicePosition.toRectangle( getRow(), getColumn() );
        }
        return anchorRect;
    }
    
    /**
     * return the bounds in the canvas coordinate frame.
     * @return the bounds in the canvas coordinate frame.
     */
    private Rectangle getAnnotationBubbleBounds() {
        int em = (int) getEmSize() / 2;
        
        Rectangle anchor= getAnchorBounds();
                        
        Rectangle r;
        r= gtr.getBounds();

        int xoffset=0;
        int yoffset=0;
        
        if ( anchorOffset.length()>0 ) {
            String[] ss= anchorOffset.split(",");
            if ( ss.length==2 ) {
                try {
                    double[] dd= DasDevicePosition.parseLayoutStr(ss[0]);
                    xoffset= (int)( getCanvas().getWidth() * dd[0] + em * dd[1] + dd[2] );
                    dd= DasDevicePosition.parseLayoutStr(ss[1]);
                    yoffset= (int)( getCanvas().getHeight() * dd[0] + em * dd[1] + dd[2] );
                } catch ( ParseException ex ) {
                    logger.warning("anchorOffset parse");
                }
            } else {
                logger.warning("anchorOffset");
            }
        }

        if ( anchorPosition==AnchorPosition.NW ) {
            r.x = anchor.x + em + xoffset ;
            r.y = anchor.y + em + yoffset ;
        } else if ( anchorPosition==AnchorPosition.N ) {
            r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
            r.y = anchor.y + em + yoffset ;
        } else if ( anchorPosition==AnchorPosition.OutsideN ) {
            r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
            r.y = anchor.y - (int)r.getHeight() - em - yoffset ;
        } else if ( anchorPosition==AnchorPosition.NE ) {
            r.x = anchor.x + anchor.width - em - r.width - xoffset;
            r.y = anchor.y + em + yoffset ;
        } else if ( anchorPosition==AnchorPosition.OutsideNE ) {
            r.x = anchor.x + anchor.width + em + xoffset;
            r.y = anchor.y + em - yoffset;
        } else if ( anchorPosition==AnchorPosition.SW ) {
            r.x = anchor.x + em + xoffset;
            r.y = anchor.y + anchor.height - em - r.height - yoffset;
        } else if ( anchorPosition==AnchorPosition.SE ) {
            r.x = anchor.x + anchor.width - em - r.width - xoffset;
            r.y = anchor.y + anchor.height - em - r.height - yoffset;
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
    public static final String PROP_FONT_SIZE= "fontSize";
    
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
    
    public static final String PROP_BORDERTYPE = "borderType";

    /**
     * the border type
     * @return the border type
     */
    public BorderType getBorderType() {
        return this.borderType;
    }

    /**
     * set the border type to NONE, rounded rectangle, etc.
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

    /**
     * set the location within the box where the annotation will be drawn.
     * @param anchorPosition 
     */
    public void setAnchorPosition(AnchorPosition anchorPosition) {
        AnchorPosition oldAnchorPosition = this.anchorPosition;
        this.anchorPosition = anchorPosition;
        firePropertyChange(PROP_ANCHORPOSITION, oldAnchorPosition, anchorPosition);
    }
    
    private DasPlot plot;
    
    public void setPlot( DasPlot p ) {
        this.plot= p;
    }
    
    private DatumRange xrange= DatumRange.newDatumRange(0,10,Units.dimensionless);

    public static final String PROP_XRANGE = "xrange";

    public DatumRange getXrange() {
        return xrange;
    }

    public void setXrange(DatumRange xrange) {
        DatumRange oldXrange = this.xrange;
        this.xrange = xrange;
        resize();
        repaint();
        firePropertyChange(PROP_XRANGE, oldXrange, xrange);
    }

    private DatumRange yrange= DatumRange.newDatumRange(0,10,Units.dimensionless);;

    public static final String PROP_YRANGE = "yrange";

    public DatumRange getYrange() {
        return yrange;
    }

    public void setYrange(DatumRange yrange) {
        DatumRange oldYrange = this.yrange;
        this.yrange = yrange;
        resize();
        repaint();        
        firePropertyChange(PROP_YRANGE, oldYrange, yrange);
    }

    private BorderType anchorBorderType = BorderType.NONE;

    public static final String PROP_ANCHORBORDERTYPE = "anchorBorderType";

    public BorderType getAnchorBorderType() {
        return anchorBorderType;
    }

    public void setAnchorBorderType(BorderType anchorBorderType) {
        BorderType oldAnchorBorderType = this.anchorBorderType;
        this.anchorBorderType = anchorBorderType;
        firePropertyChange(PROP_ANCHORBORDERTYPE, oldAnchorBorderType, anchorBorderType);
    }

    private AnchorType anchorType = AnchorType.CANVAS;

    public static final String PROP_ANCHORTYPE = "anchorType";

    public AnchorType getAnchorType() {
        return anchorType;
    }

    public void setAnchorType(AnchorType anchorType) {
        AnchorType oldAnchorType = this.anchorType;
        this.anchorType = anchorType;
        firePropertyChange(PROP_ANCHORTYPE, oldAnchorType, anchorType);
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
     * set the arrow style to BIG,SMALL,DRAFTING.
     * @param newarrowStyle the arrow style
     */
    public void setArrowStyle( Arrow.HeadStyle newarrowStyle) {
        Arrow.HeadStyle oldarrowStyle = arrowStyle;
        this.arrowStyle = newarrowStyle;
        repaint();
        firePropertyChange(PROP_ARROWSTYLE, oldarrowStyle, newarrowStyle);
    }

    private boolean overrideColors = false;

    public static final String PROP_OVERRIDECOLORS = "overrideColors";

    public boolean isOverrideColors() {
        return overrideColors;
    }

    /**
     * true will use the colors specified, otherwise the canvas colors are used.
     * @param overrideColors 
     */
    public void setOverrideColors(boolean overrideColors) {
        boolean oldOverrideColors = this.overrideColors;
        this.overrideColors = overrideColors;
        firePropertyChange(PROP_OVERRIDECOLORS, oldOverrideColors, overrideColors);
    }

    private Color textColor = new Color(0, 0, 0, 0);

    public static final String PROP_TEXTCOLOR = "textColor";

    public Color getTextColor() {
        return textColor;
    }

    /**
     * the color of the text, or if transparent then the border
     * color should be used.
     *
     * @param textColor 
     */
    public void setTextColor(Color textColor) {
        Color oldTextColor = this.textColor;
        this.textColor = textColor;
        repaint();
        firePropertyChange(PROP_TEXTCOLOR, oldTextColor, textColor);
    }
       
    private String anchorOffset="";

    public static final String PROP_ANCHOROFFSET = "anchorOffset";

    public String getAnchorOffset() {
        return anchorOffset;
    }

    /**
     * the offset in x and y for the text bubble from the anchor.  For
     * example, "4em,4em" will place a OutsideNE label 4ems up and 4ems over 
     * from the default.
     * 
     * @param anchorOffset 
     */
    public void setAnchorOffset(String anchorOffset) {
        String oldAnchorOffset = this.anchorOffset;
        this.anchorOffset = anchorOffset;
        firePropertyChange(PROP_ANCHOROFFSET, oldAnchorOffset, anchorOffset);
    }

    
}
