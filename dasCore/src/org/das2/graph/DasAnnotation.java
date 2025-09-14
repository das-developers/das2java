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
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.das2.datum.DatumRange;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.LoggerManager;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import static org.das2.graph.AnchorPosition.NW;
import static org.das2.graph.AnchorPosition.OutsideE;
import static org.das2.graph.AnchorPosition.OutsideW;

/**
 * This makes a DasCanvasComponent for GrannyTextRenderer, and 
 * optionally adds an arrow to point at things.
 * 
 * TODO: See https://autoplot.org/developer.annotations
 * 
 * @author Jeremy
 */
public class DasAnnotation extends DasCanvasComponent {

    private static final Logger logger= LoggerManager.getLogger("das2.graph.annotation");
    
    private static final boolean DEBUG_GRAPHICS = System.getProperty("das2.graph.dasannotation.debuggraphics","false").equals("true");
        
    String templateString;
    GrannyTextRenderer gtr;
    BufferedImage img;
    
    /**
     * if true, then a resize has been posted.
     */
    boolean boundsCalculated= false;

    private Map<String,GrannyTextRenderer.Painter> painters= new HashMap<>();
    
    /**
     * add a painter for the grannyTextRenderer.  This is done by associating
     * a Painter code with an id, and the id is used within the annotation string.
     * @param id id for the painter, where the id is found in the granny text string
     * @param p the painter code which draws on a graphics context.
     * @see org.das2.util.GrannyTextRenderer#addPainter(java.lang.String, org.das2.util.GrannyTextRenderer.Painter) 
     */
    public void addPainter( String id, GrannyTextRenderer.Painter p ) {
        painters.put( id, p );
        for ( Entry<String,GrannyTextRenderer.Painter> ee: painters.entrySet() ) {
            if ( gtr!=null ) gtr.addPainter( ee.getKey(), ee.getValue() );
        }
    }
    
    /**
     * remove the painter with the given id.
     * @param id id for the painter, where the id is found in the granny text string
     */
    public void removePainter( String id ) {
        painters.remove(id);
    }
    
    /**
     * remove all the painters
     */
    public void clearPainters() {
        painters.clear();
    }
    
    /**
     * point at this thing
     */
    private DasAnnotation.PointDescriptor pointAt;
    //private final MouseModule arrowToMouseModule;

    /** 
     * Create the annotation
     * @param string the message, which may contain %p which will be replaced with a label. */
    public DasAnnotation(String string) {
        super();
        
        if ( string.startsWith("http:" ) ) {
            this.gtr= null;
            try {
                this.img= ImageIO.read( new URL(string) );
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
                this.gtr = GraphUtil.newGrannyTextRenderer();
            }
        } else {
            this.gtr = GraphUtil.newGrannyTextRenderer();
            this.gtr.setString( getFont(), "" );
        }
        this.templateString = string;

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

        MouseModule mm = new MoveComponentMouseModule( this ) {
            Point p0;
            
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e); 
                p0= e.getPoint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p= e.getPoint();
                int dx = p.x - p0.x;
                int dy = p.y - p0.y;
                adjustAnchorOffset( dx, dy );
                resize();
                repaint();                    
            }
        };
        mm.setLabel("Move Annotation");
        
        mm.setDragRenderer( new MoveComponentMouseModule.MoveRenderer(this) {
            @Override
            public Rectangle[] renderDrag(Graphics g1, Point p1, Point p2) {
                int dx = p2.x - p1.x;
                int dy = p2.y - p1.y;
                String s= calculateAnchorOffset(dx,dy);
                Rectangle bounds = getActiveRegion().getBounds();
                bounds.translate(p2.x - p1.x, p2.y - p1.y);
                Graphics2D g2= (Graphics2D)g1.create();
                g2.setColor( Color.GRAY );
                g2.drawString( s, bounds.x, bounds.y );
                if ( getBackground().getRed()>128 ) {
                    g2.setColor( Color.LIGHT_GRAY );
                } else {
                    g2.setColor( Color.DARK_GRAY );
                }
                Line2D line = calculateAnchorLine( bounds, dx,dy);
                g2.draw(line);
                return super.renderDrag(g1, p1, p2); 
            }
            
        });
        
        this.getDasMouseInputAdapter().setPrimaryModule(mm);


        MouseModule pointAtMouseModule= new MouseModule( this, new ArrowDragRenderer(), "Point At" ) {
            Point p0;
            
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e); 
                if ( plot!=null ) {
                    p0= e.getPoint();
                    p0= SwingUtilities.convertPoint( DasAnnotation.this, p0, plot.getCanvas() );
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if ( plot!=null ) {
                    Datum oldx= getPointAtX();
                    Datum oldy= getPointAtY();
                    Datum x= plot.getXAxis().invTransform(e.getX()+getX());
                    Datum y= plot.getYAxis().invTransform(e.getY()+getY());
                    
                    if ( e.isShiftDown() ) {
                        String xs= getReferenceX().trim();
                        if ( xs.length()>0 ) {
                            setReferenceX( xs + ";" + x.toString() );
                        } else {
                            setReferenceX( getPointAtX().toString() + ";" + x.toString() );
                        }
                        String ys= getReferenceY().trim();
                        if ( ys.length()>0 ) {
                            setReferenceY( ys + ";" + y.toString() );
                        } else {
                            setReferenceY( getPointAtY().toString() + ";" + y.toString() );
                        }
                    } else {
                        setReferenceX("");
                        setReferenceY("");
                    }
                    
                    setPointAtX(x);
                    setPointAtY(y);

                    resize();

                    setShowArrow(true);
                }
            }            
        };
        this.getDasMouseInputAdapter().addMouseModule(pointAtMouseModule);

    }
    
    private void adjustDataRanges( int dx, int dy ) {
        
    }
    
    private Line2D calculateAnchorLine( Rectangle bounds, int dx, int dy ) {
        
        int anchorX, anchorY;
        Rectangle anchor= getAnchorBounds();
        
        int rectX, rectY;
        
        switch (anchorPosition) {
            case NW:
            case SW:
            case W:                
                anchorX = anchor.x;
                rectX= bounds.x;
                break;
            case OutsideN:
            case OutsideS:
            case Center:
            case N:
            case S:
                anchorX = anchor.x + anchor.width/2;
                rectX= bounds.x + bounds.width/2;
                break;
            case OutsideE:
                anchorX= anchor.x + anchor.width;
                rectX= bounds.x + bounds.width;
                break;
            case OutsideW:
                anchorX= anchor.x;
                rectX= bounds.x;
                break;
            case NE:
            case SE:
            case E:
            case OutsideNE:
            case OutsideSE:
                anchorX = anchor.x + anchor.width;
                rectX= bounds.x + bounds.width;
                break;
            case OutsideNW:
            case OutsideSW:
                anchorX = anchor.x;
                rectX= bounds.x;
                break;
            case OutsideNNW:
            case OutsideSSW:
                anchorX = anchor.x;
                rectX= bounds.x;
                break;
            case OutsideNNE:
            case OutsideSSE:
                anchorX = anchor.x + anchor.width;
                rectX= bounds.x + bounds.width;
                break;
            default:
                anchorX = 0;
                rectX= bounds.x;
                break;
        }
        
        switch (anchorPosition) {
            case NW:
            case NE:
            case N:
                anchorY = anchor.y;
                rectY= bounds.y;
                break;
            case OutsideN:
                anchorY = anchor.y;
                rectY= bounds.y;
                break;
            case OutsideS:
            case OutsideSSW:
            case OutsideSSE:
                anchorY = anchor.y + anchor.height ;
                rectY= bounds.y + bounds.height;
                break;
            case OutsideE:
            case OutsideW:
                anchorY = anchor.y + anchor.height/2;
                rectY= bounds.y + bounds.height/2;
                break;
            case OutsideNE:
            case OutsideNW:
                anchorY = anchor.y;
                rectY= bounds.y;
                break;
            case OutsideSE:
            case OutsideSW:
            case SW:
            case SE: 
            case S:
                anchorY = anchor.y + anchor.height;
                rectY= bounds.y + bounds.height;
                break;
            case OutsideNNW:
            case OutsideNNE:
                anchorY = anchor.y;
                rectY= bounds.y;
                break;
            case Center:
            case W:
            case E:
                anchorY = anchor.y + anchor.height/2;
                rectY= bounds.y + bounds.height/2;
                break;
            default:
                anchorY = 0;
                rectY= bounds.y;
                break;
        }
        
        
        return new Line2D.Double( anchorX, anchorY, rectX, rectY );

    }
    
    private String calculateAnchorOffset( int dx, int dy ) {
        if ( anchorPosition==AnchorPosition.NW ) {
        } else if ( anchorPosition==AnchorPosition.N ) {
        } else if ( anchorPosition==AnchorPosition.OutsideN ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.NE ) {
            dx= -dx;
        } else if ( anchorPosition==AnchorPosition.OutsideNE ) {
        } else if ( anchorPosition==AnchorPosition.OutsideSE ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideNW ) {
            dx= -dx;
        } else if ( anchorPosition==AnchorPosition.OutsideSW ) {
            dx= -dx;
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.SW ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.SE ) {
            dx= -dx;
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideNNW ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideNNE ) {
            dx= -dx;
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideSSE ) {
            dx= -dx;
        } else if ( anchorPosition==AnchorPosition.Center ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.W ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.E ) {
            dx= -dx;
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideW ) {
            dx= -dx;
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideE ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.S ) {
            dy= -dy;
        }

        String offset= getAnchorOffset();
        double em = getEmSize();
        if ( offset.trim().length()==0 ) {
            offset= String.format( Locale.US, "%.2fem,%.2fem", dx/em, dy/em );
            return offset;
        } else {
            try {
                String[] ss= offset.split(",",-2);
                double[] dd;
                dd= DasDevicePosition.parseLayoutStr(ss[0]);
                dd[1]= dd[1] + dx/em;
                ss[0]= DasDevicePosition.formatLayoutStr(dd);
                dd= DasDevicePosition.parseLayoutStr(ss[1]);
                dd[1]= dd[1] + dy/em;
                ss[1]= DasDevicePosition.formatLayoutStr(dd);
                offset= ss[0]+","+ss[1];
                return offset;
                
            } catch (ParseException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
        return offset; // there's some parse error, don't modify
    }
    
    private void adjustAnchorOffset( int dx, int dy ) {
        String newOffset= calculateAnchorOffset( dx, dy );
        this.setAnchorOffset(newOffset);
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

    public static final String PROP_TEXT = "text";
    
    /**
     * Set the text, which can be Granny Text, or image URL. (Image URL is 
     * deprecated, use url property instead.)  If the url property has length>0,
     * then this is ignored.
     * URLs must start with http:, https:, or file:.
     *
     * @param string the text
     * @see GrannyTextRenderer
     */
    public void setText(String string) {
        String oldValue= this.templateString;
        this.templateString = string;
        if ( this.getGraphics()!=null ) {
            if ( url.length()==0 ) {
                if ( string.startsWith("http:") || string.startsWith("https:" ) || string.startsWith("file:" ) ) {
                    try {
                        img= ImageIO.read(new URL(string));
                        gtr= null;
                    } catch ( IOException ex ) {
                        gtr= GraphUtil.newGrannyTextRenderer();
                        gtr.setString( this.getGraphics(), getString() );
                    }
                } else {
                    gtr= GraphUtil.newGrannyTextRenderer();
                    gtr.setString( this.getGraphics(), getString() );
                    for ( Entry<String,GrannyTextRenderer.Painter> ee: painters.entrySet() ) {
                        gtr.addPainter( ee.getKey(), ee.getValue() );
                    }
                }
            }
            resize();
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

    private String url = "";

    public static final String PROP_URL = "url";

    public String getUrl() {
        return url;
    }

    /**
     * set the URL to the location of a png or jpg file.  If this is set,
     * then the text property is ignored.  
     * @param url 
     */
    public void setUrl(String url) {
        String oldUrl = this.url;
        if ( url.length()==0 ) {
            this.url= url;
            setText( getText() );
        } else {
            try {
                img= ImageIO.read(new URL(url));
                gtr= null;
            } catch ( IOException ex ) {
                gtr= GraphUtil.newGrannyTextRenderer();
                gtr.setString( this.getGraphics(), url );
            }
        }
        this.url = url;
        firePropertyChange(PROP_URL, oldUrl, url);
    }
    
    private String padding = "0.5em";

    /**
     * the amount of space between the text and the border, measured
     * in ems, pts, pxs, or percents.
     * @see GraphUtil#parseLayoutLength(java.lang.String, double, double) 
     */
    public static final String PROP_PADDING = "padding";

    public String getPadding() {
        return padding;
    }

    public void setPadding(String padding) {
        String oldPadding = this.padding;
        this.padding = padding;
        firePropertyChange(PROP_PADDING, oldPadding, padding);
        resize();
    }

    /**
     * the scale for the image, 0.5 is half-size, 2.0 is double.
     */
    private double scale = 1.0;

    public static final String PROP_SCALE = "scale";

    public double getScale() {
        return scale;
    }

    /**
     * set the amount to scale the image by, if using URL to point at an image, where 0.5 is half of the
     * original image size.
     * @param scale 
     */
    public void setScale(double scale) {
        double oldScale = this.scale;
        this.scale = scale;
        firePropertyChange(PROP_SCALE, oldScale, scale);
        resize();
    }

    @Override
    public void resize() {
        Font f= getFont();
        if ( f!=null ) {
            super.resize();
            Font thefont= f;
            if ( fontSize>0 ) thefont= f.deriveFont(fontSize);
            if ( this.gtr!=null ) {
                this.gtr.setString( thefont, getString() );
            }
            Rectangle r= calcBounds();

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
     * This item should only accept mouse events on the bubble
     * @param x
     * @param y
     * @return 
     */
    @Override
    public boolean contains(int x, int y) {
        return acceptContext( x+getX(), y+getY() );
    }

    private Rectangle calcBoundForPoint( Rectangle r, Datum pointAtX, Datum pointAtY ) {
        double headx, heady;
        try {
            headx= (int)plot.getXAxis().transform(pointAtX);
        } catch ( InconvertibleUnitsException ex ) {
            headx= (int)plot.getXAxis().transform(pointAtX.doubleValue(pointAtX.getUnits()),plot.getXAxis().getUnits());
        }
        try {
            heady= (int)plot.getYAxis().transform(pointAtY);
        } catch ( InconvertibleUnitsException ex ) {
            heady= (int)plot.getYAxis().transform(pointAtY.doubleValue(pointAtY.getUnits()),plot.getYAxis().getUnits());
        }          
        float s= fontSize;
        r.add( headx-s, heady-s );
        r.add( headx+s, heady+s );
        return r;
    }

    /**
     * calculate the bounds in the canvas coordinate system.
     * @return 
     */
    private Rectangle calcBounds() {
        logger.entering("DasAnnotation", "calcBounds" );
        Rectangle r = (Rectangle)getActiveRegion();
        if (pointAt != null) {
            Point head = pointAt.getPoint();
            r.add(head);
        }
        if ( anchorBorderType!=BorderType.NONE ) {
            Rectangle anchorRect= getAnchorBounds();
            r.add(anchorRect);
        }
        if ( showArrow ) {
            if ( plot!=null ) {
                r= calcBoundForPoint( r, pointAtX, pointAtY );
            }
        }
        
        if ( referenceX.trim().length()>0 || referenceY.trim().length()>0 ) {
            String[] xx= referenceX.split("[;,]",-2);
            String[] yy= referenceY.split("[;,]",-2);
            int n= Math.max( xx.length, yy.length );
            if ( xx.length>1 && yy.length>1 && xx.length!=yy.length ) {
                logger.warning("x and y reference count is different");
            } else {
                for ( int i=0; i<n; i++ ) {
                    try {
                        String xs= ( xx.length==1 ) ? xx[0] : xx[i];
                        Datum xd= xrange.getUnits().parse( xs );
                        String ys= ( yy.length==1 ) ? yy[0] : yy[i];
                        Datum yd= yrange.getUnits().parse( ys );
                        r= calcBoundForPoint( r, xd, yd );
                    } catch (ParseException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        
        int s= Math.max( getFont().getSize()/5, 3 );
        r= new Rectangle( r.x-s, r.y-s, r.width+s*2+1, r.height+s*2+1 );
        r.add( r.x+r.width+1, r.y+r.height+1 );
        
        int vmin, vmax;
        int hmin, hmax;
        
        if ( anchorType==AnchorType.CANVAS || plot==null ) {
            hmin= 0;
            hmax= getCanvas().getWidth();
        } else {
            if ( anchorPosition==AnchorPosition.OutsideE 
                    || anchorPosition==AnchorPosition.OutsideN 
                    || anchorPosition==AnchorPosition.OutsideS 
                    || anchorPosition==AnchorPosition.OutsideW 
                    || anchorPosition==AnchorPosition.OutsideNE 
                    || anchorPosition==AnchorPosition.OutsideNNE
                    || anchorPosition==AnchorPosition.OutsideNNW
                    || anchorPosition==AnchorPosition.OutsideNW
                    || anchorPosition==AnchorPosition.OutsideSE 
                    || anchorPosition==AnchorPosition.OutsideSSE
                    || anchorPosition==AnchorPosition.OutsideSSW
                    || anchorPosition==AnchorPosition.OutsideSW 
                    ) {
                hmin= 0;
                hmax= getCanvas().getWidth();
            } else {
                hmin= plot.getColumn().getDMinimum();
                hmax= plot.getColumn().getDMaximum();        
            }
        }
        if ( splitAnchorType ) {
            if ( verticalAnchorType==AnchorType.CANVAS || plot==null ) {
                vmin= 0;
                vmax= getCanvas().getHeight();
            } else {
                vmin= plot.getRow().getDMinimum();
                vmax= plot.getRow().getDMaximum();
            }
        } else {
            if ( anchorType==AnchorType.CANVAS || plot==null ) {
                vmin= 0;
                vmax= getCanvas().getHeight();
            } else {
                if ( anchorPosition==AnchorPosition.OutsideE 
                    || anchorPosition==AnchorPosition.OutsideN 
                    || anchorPosition==AnchorPosition.OutsideS 
                    || anchorPosition==AnchorPosition.OutsideW 
                    || anchorPosition==AnchorPosition.OutsideNE 
                    || anchorPosition==AnchorPosition.OutsideNNE
                    || anchorPosition==AnchorPosition.OutsideNNW
                    || anchorPosition==AnchorPosition.OutsideNW
                    || anchorPosition==AnchorPosition.OutsideSE 
                    || anchorPosition==AnchorPosition.OutsideSSE
                    || anchorPosition==AnchorPosition.OutsideSSW
                    || anchorPosition==AnchorPosition.OutsideSW 
                    ) {
                    vmin= 0;
                    vmax= getCanvas().getHeight();
                } else {
                    vmin= plot.getRow().getDMinimum();
                    vmax= plot.getRow().getDMaximum();
                }
            }
        }
        Rectangle clip= new Rectangle( hmin, vmin, hmax-hmin, vmax-vmin );
        r= r.intersection( clip ); 
            
        boundsCalculated= true;
        
        logger.exiting( "DasAnnotation","calcBounds", r);
        return r;
    }
    
    @Override
    public void paintComponent(Graphics g1) {
        
        Graphics2D g = (Graphics2D) g1.create(); 

        if ( DEBUG_GRAPHICS ) {
            Rectangle r= getBounds();
            Graphics2D g_ = (Graphics2D) g.create();
            g_.translate( -getX(), -getY() );
            g_.setClip(null);
            g_.setColor( org.das2.util.ColorUtil.STEEL_BLUE );
            g_.drawRect( r.x, r.y, r.width, r.height );
            g_.dispose();
        }
        
        //System.err.println(">>>"+g1.getFont().getSize());
        
        double em2 = GraphUtil.parseLayoutLength( lineThickness, 0, g1.getFont().getSize() );
        int rounds= g1.getFont().getSize();
        
        Stroke stroke0= g.getStroke();
        
        g.setStroke(new BasicStroke((float)em2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
        
        g.translate( -getX(), -getY() );

        Rectangle clip= calcBounds();
        g.setClip( clip );
        
        Color fore = getCanvas().getForeground();
        Color ltextColor= fore;
        Color back= getCanvas().getBackground();
        
        if ( isOverrideColors() ) {
            fore= getForeground();
            ltextColor= textColor;
            back= getBackground();
        }
         
//        if ( anchorType==AnchorType.CANVAS ) {
//            back= Color.PINK;
//        } else if ( anchorType==AnchorType.PLOT ) {
//            back= org.das2.util.ColorUtil.decodeColor("Lavender");
//        } else if ( anchorType==AnchorType.DATA ) {
//            back= org.das2.util.ColorUtil.decodeColor("LemonChiffon");
//        }
        
        if ( fontSize>0 ) g.setFont( getFont().deriveFont(fontSize) );

        int em = (int) GraphUtil.parseLayoutLength( padding, clip.width, getEmSize() );

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if ( gtr!=null ) gtr.setString( g, getString() );
        Rectangle r;
        
        try {
            r= getAnnotationBubbleBounds();
        } catch ( IllegalArgumentException ex ) {
            ex.printStackTrace();
            return;
        }
        
        if ( anchorPosition==AnchorPosition.N 
            || anchorPosition==AnchorPosition.OutsideN 
            || anchorPosition==AnchorPosition.Center 
            || anchorPosition==AnchorPosition.S  ) {
            if ( gtr!=null ) gtr.setAlignment( GrannyTextRenderer.CENTER_ALIGNMENT );
        }
        
        if ( showArrow ) {
            paintOneArrow(g, r, Math.max(12,em2*8), stroke0, fore, pointAtX, pointAtY );
        }
        
        if ( referenceX.length()>0 || referenceY.length()>0 ) {
            String[] xx= referenceX.split("[;,]",-2);
            String[] yy= referenceY.split("[;,]",-2);
            int n= Math.max( xx.length, yy.length );
            if ( xx.length>1 && yy.length>1 && xx.length!=yy.length ) {
                logger.warning("x and y reference count is different");
            } else {
                for ( int i=0; i<n; i++ ) {
                    try {
                        String xs= ( xx.length==1 ) ? xx[0] : xx[i];
                        Datum xd= xrange.getUnits().parse( xs );
                        String ys= ( yy.length==1 ) ? yy[0] : yy[i];
                        Datum yd= yrange.getUnits().parse( ys );
                        paintOneArrow(g, r, Math.max(12,em2*8), stroke0, fore, xd, yd );
                    } catch (ParseException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

        g.setColor(back);

        if ( anchorBackground.getAlpha()>0 ) {
            Color c0= g.getColor();
            g.setColor( anchorBackground );
            Rectangle anchorRect= getAnchorBounds();
            if ( anchorBorderType== BorderType.RECTANGLE || anchorBorderType==BorderType.NONE ) {
                if ( anchorRect.width==0 ) {
                    g.fill( new Line2D.Double( anchorRect.x, anchorRect.y, anchorRect.x, anchorRect.y+anchorRect.height ) );
                } else if ( anchorRect.height==0 ) {
                    g.fill( new Line2D.Double( anchorRect.x, anchorRect.y, anchorRect.x+anchorRect.width, anchorRect.y ) );
                } else {
                    g.fill(anchorRect);
                }
            } else if ( anchorBorderType==BorderType.ROUNDED_RECTANGLE ) {
                g.fillRoundRect(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height, (int)rounds, (int)rounds);
            }
            g.setColor( c0 );
        }
            
        if ( gtr==null || !getString().equals("") ) {
            if (borderType == BorderType.RECTANGLE || borderType == BorderType.NONE) {
                g.fill(r);
            } else if (borderType == BorderType.ROUNDED_RECTANGLE) {
                g.fillRoundRect(r.x, r.y, r.width, r.height, (int)em2*8, (int)em2*8 );
            }

            g.setColor(ltextColor);

            Graphics2D gtext= (Graphics2D) g.create();
                    
            Rectangle bb= getAnnotationBubbleBounds();
            int rot= rotate % 360;
            if ( rot<-90 ) rot= rot+360;
            if ( rot>180 ) rot= rot-360;
            
            double ascent= gtr==null ? g.getFontMetrics().getAscent() : gtr.getAscent();
            
            if ( rot==-90 ) {
                gtext.translate( bb.x + bb.width - ascent, bb.y );
                gtext.rotate( -rotate*Math.PI/180. );

            } else if ( rot==90  ) {
                gtext.translate( bb.x + ascent, bb.y + bb.height );
                gtext.rotate( -rotate*Math.PI/180. );

            } else if ( rot==180 ) {
                gtext.translate( bb.x, bb.y + em + (float) ascent );
                double midx= bb.width/2;
                double midy= bb.height/2;
                gtext.rotate( -rotate*Math.PI/180., midx, midy );
                gtext.translate( 0, bb.height );
                
            } else if ( rot==0 ) {
                gtext.translate( bb.x, bb.y + em + (float) ascent );
            
            }
    
            // add sunburst pattern while I try to figure out rotation. TODO: remove
            if ( false ) { //rotate!=0 ) {
                gtext.setColor( Color.LIGHT_GRAY );
                for ( int i=0; i<360; i+=10 ) {
                    double a= i*Math.PI/180;
                    double ix= Math.cos(a);
                    double iy= Math.sin(a);
                    gtext.drawLine( 0, 0, (int)(200*ix), (int)(200*iy) );
                }
                System.err.println("done drawing sunburst");
            }
            
            gtext.setColor(ltextColor);

            if ( gtr!=null ) {
                try {
                    
                    gtr.draw(gtext, em, 0 );
                    
                } catch ( IllegalArgumentException ex ) {
                    gtr.setString( gtext.getFont(), getText() );
                    gtr.draw(gtext, r.x+em, r.y + em + (float) gtr.getAscent() );
                }
            } else {
                BufferedImage localImage= img;
                if ( localImage==null ) {
                    try {
                        localImage= ImageIO.read( DasAnnotation.class.getResource("/images/grey100.png") );
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, null, ex); // this shouldn't happen.
                        return;
                    }
                }
                if ( scale!=1.0 ) {
                    int newWidth= (int)(localImage.getWidth()*scale);
                    int newHeight= (int)(localImage.getHeight()*scale);
                    
                    boolean printing= getCanvas().isPrintingThread();
                    if ( printing ) {
                        gtext.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        gtext.drawImage( localImage, r.x+em, r.y+em, newWidth, newHeight, this );        
                    } else {
                        BufferedImage resized = org.das2.util.ImageUtil.getScaledInstance( localImage, (int)Math.sqrt( newWidth*newWidth + newHeight*newHeight ) );
                        gtext.drawImage( resized, r.x+em, r.y+em, this );                        
                    }
                    

                } else {
                    gtext.drawImage( localImage, r.x+em, r.y+em, this );
                }
            }
        
            gtext.setColor(fore);
                
            if (borderType != BorderType.NONE) {
                if (borderType == BorderType.RECTANGLE) {
                    g.draw(bb);
                } else if (borderType == BorderType.ROUNDED_RECTANGLE) {
                    g.drawRoundRect(bb.x, bb.y, bb.width, bb.height, (int)rounds, (int)rounds);
                } else if (borderType==BorderType.UNDERSCORE ) {
                    int y= bb.y+bb.height;
                    g.drawLine( bb.x+em, y, bb.x + bb.width-(int)em2, y );
                }
            }
            
            if ( anchorBorderType!=BorderType.NONE ) {
                Rectangle anchorRect= getAnchorBounds();
                if ( anchorBorderType== BorderType.RECTANGLE ) {
                    if ( anchorRect.width==0 ) {
                        g.draw( new Line2D.Double( anchorRect.x, anchorRect.y, anchorRect.x, anchorRect.y+anchorRect.height ) );
                    } else if ( anchorRect.height==0 ) {
                        g.draw( new Line2D.Double( anchorRect.x, anchorRect.y, anchorRect.x+anchorRect.width, anchorRect.y ) );
                    } else {
                        g.draw(anchorRect);
                    }
                } else if ( anchorBorderType==BorderType.ROUNDED_RECTANGLE ) {
                    g.drawRoundRect(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height, rounds, rounds);
                } else if ( anchorBorderType==BorderType.UNDERSCORE ) {
                    g.draw(anchorRect);
                }
            }
        }
        
        // this was useful for debugging.
//        Graphics2D g0 = (Graphics2D) g.create(); 
//        Rectangle clip= g0.getClipBounds();
//        if ( clip!=null ) {
//            g0.setColor(Color.ORANGE );
//            g0.drawRoundRect( clip.x, clip.y, clip.width-1, clip.height-1, 5, 5 );
//        }
        
        g.dispose();
        
        getDasMouseInputAdapter().paint(g1);

    }

    private void paintOneArrow(Graphics2D g, Rectangle r, double em2, Stroke stroke0, Color fore, Datum x, Datum y ) {
        int headx= 0;
        int heady= 0;
        if ( plot!=null ) {
            try {
                headx= (int)plot.getXAxis().transform(x);
            } catch ( InconvertibleUnitsException ex ) {
                headx= (int)plot.getXAxis().transform(x.doubleValue(x.getUnits()),plot.getXAxis().getUnits());
            }
            try {
                heady= (int)plot.getYAxis().transform(y);
            } catch ( InconvertibleUnitsException ex ) {
                heady= (int)plot.getYAxis().transform(y.doubleValue(y.getUnits()),plot.getYAxis().getUnits());
            }
        }
        
        Point head = new Point(headx,heady);
        
        Graphics2D g2 = (Graphics2D) g.create();

        Point2D tail2d= new Point2D.Double( r.x + r.width/2, r.y + r.height/2 );
        Point2D head2d= new Point2D.Double( head.x, head.y );
        Rectangle2D rect2d= new Rectangle2D.Double(r.x, r.y, r.width, r.height );
        Point2D p2d= GraphUtil.lineRectangleIntersection( tail2d, head2d, rect2d );
        Point p= p2d==null ? head : new Point( (int)p2d.getX(), (int)p2d.getY() );


        Point head0 = new Point( head );

        if ( showArrow ) {
            
            if ( pointAtOffset.length()>0 ) {
                Line2D line= new Line2D.Double( head.x, head.y, p.x, p.y );
                double lengthPixels= GraphUtil.parseLayoutLength( pointAtOffset, line.getP1().distance(line.getP2()), getEmSize() );
                Line2D newLine= GraphUtil.shortenLine(line, lengthPixels, 0 );
                head= new Point( (int)newLine.getP1().getX(), (int)newLine.getP1().getY() );
            }
            
            Color glowColor= getCanvas().getBackground();
            
            if ( gtr.isGlow() ) {
                // match the 1-pixel glow around the text
                float linethink= (float) ( (em2 / 12) );
                if ( linethink<2 ) linethink=2;
                g2.setStroke( new BasicStroke( linethink, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
                g2.setColor( glowColor );
                Arrow.paintArrow(g2, head, p, em2, this.arrowStyle );
            } else {
                g2.setStroke( new BasicStroke( (float) (em2/4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
                g2.setColor( new Color( glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 128 ) );
                Arrow.paintArrow(g2, head, p, em2, this.arrowStyle );
            }
            g2.setStroke( stroke0 );
            g2.setColor( fore );
            Arrow.paintArrow(g2, head, p, em2, this.arrowStyle );
            
        }

        if ( DefaultPlotSymbol.NONE!=symbol ) {
            symbol.draw( g2, head0.x, head0.y, fontSize/3.f, FillStyle.STYLE_SOLID );
        }
        
        g2.dispose();
    }

    /**
     * return the bounds of that thing we are anchored to.  Note 
     * AnchorType.DATA is treated the same as AnchorType.PLOT, but the thought
     * is that it could look at the renderer's data.
     * @return the bounds of that thing we are anchored to.
     */
    private Rectangle getAnchorBounds() {
        Rectangle anchorRect= new Rectangle();
        if ( ( anchorType==AnchorType.DATA ) ) {
            if ( plot!=null && xrange!=null && yrange!=null ) {
                anchorXToData(anchorRect);
                anchorYToData(anchorRect);
                if ( splitAnchorType ) {
                    if ( verticalAnchorType==AnchorType.CANVAS ) {
                        anchorRect.y= getRow().getDMinimum();
                        anchorRect.height= getRow().getHeight();
                    }
                }
            } else {
                anchorRect= DasDevicePosition.toRectangle( getRow(), getColumn() );
                if ( splitAnchorType ) {
                    if ( verticalAnchorType==AnchorType.DATA ) {
                        anchorYToData(anchorRect);
                    }
                }
            }
        } else if ( anchorType==AnchorType.PLOT && plot!=null ) {
            anchorRect= DasDevicePosition.toRectangle( plot.getRow(), plot.getColumn() );
            if ( splitAnchorType ) {
                if ( verticalAnchorType==AnchorType.DATA ) { //TODO: Hmm...
                    anchorYToData(anchorRect);
                }
            }
        } else {
            anchorRect= DasDevicePosition.toRectangle( getRow(), getColumn() );
            if ( splitAnchorType ) {
                if ( verticalAnchorType==AnchorType.DATA ) {
                    anchorYToData(anchorRect);
                }
            }
        }
        return anchorRect;
    }

            
    /**
     * Anchor the horizontal components of the bounding box to the xrange value.
     * @param anchorRect 
     */
    private void anchorXToData(Rectangle anchorRect) {
        try {
            anchorRect.x= (int)(plot.getXAxis().transform(xrange.min()));
            int x1= (int)(plot.getXAxis().transform(xrange.max()));
            if ( x1<anchorRect.x ) {
                int t= anchorRect.x;
                anchorRect.x= x1;
                x1= t;
            }
            anchorRect.width= x1- anchorRect.x;
        } catch ( InconvertibleUnitsException ex ) {
            if ( xrange.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement(plot.getXAxis().getUnits())  ) {
                anchorRect.x= (int)(plot.getXAxis().transform(xrange.min().value(),plot.getXAxis().getUnits()));
                int x1= (int)(plot.getXAxis().transform(xrange.max().value(),plot.getXAxis().getUnits()));
                if ( x1<anchorRect.x ) {
                    int t= anchorRect.x;
                    anchorRect.x= x1;
                    x1= t;
                }
                anchorRect.width= x1- anchorRect.x;
            } else {
                if ( xrange.min().getUnits()==Units.dimensionless && xrange.min().value()==0.0 ) {
                    logger.fine("unable to convert x units for annotation, transitional state");
                } else {
                    logger.info("unable to convert x units for annotation");
                }
                anchorRect.x= getColumn().getDMinimum();
                anchorRect.width= getColumn().getWidth();
            }
        }
    }
        
    /**
     * Anchor the vertical components of the bounding box to the yrange value.
     * @param anchorRect 
     */
    private void anchorYToData(Rectangle anchorRect) {
        try {
            anchorRect.y= (int)(plot.getYAxis().transform(yrange.min()));
            int y1= (int)(plot.getYAxis().transform(yrange.max()));
            if ( y1<anchorRect.y ) {
                int t= anchorRect.y;
                anchorRect.y= y1;
                y1= t;
            }
            anchorRect.height= y1- anchorRect.y;
        } catch ( InconvertibleUnitsException ex ) {
            if ( yrange.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement(plot.getYAxis().getUnits())  ) {
                anchorRect.y= (int)(plot.getYAxis().transform(yrange.min().value(),plot.getYAxis().getUnits()));
                int y1= (int)(plot.getYAxis().transform(yrange.max().value(),plot.getYAxis().getUnits()));
                if ( y1<anchorRect.y ) {
                    int t= anchorRect.y;
                    anchorRect.y= y1;
                    y1= t;
                }
                anchorRect.height= y1- anchorRect.y;
            } else {
                if ( yrange.min().getUnits()==Units.dimensionless && yrange.min().value()==0.0 ) {
                    logger.fine("unable to convert y units for annotation, transitional state");
                } else {
                    logger.info("unable to convert y units for annotation");
                }
                anchorRect.y= getRow().getDMinimum();
                anchorRect.height= getRow().getHeight();
            }
        }
    }
    
    /**
     * return the bounds in the canvas coordinate frame.
     * @return the bounds in the canvas coordinate frame.
     */
    private Rectangle getAnnotationBubbleBounds() {

        Rectangle r = getAnnotationBubbleBoundsNoRotation();
               
        if ( rotate!=0 ) {
            int rot= rotate % 360;
            if ( rot==90 || rot==270 || rot==-90 ) {
                Rectangle nr= new Rectangle();
                switch ( anchorPosition ) {
                    case Center:
                        nr.x= r.x + r.width/2 - r.height/2;
                        nr.y= r.y + r.height/2 - r.width/2;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case NE:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case NW:
                        nr.x= r.x;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case SW:
                        nr.x= r.x;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case SE:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;                             
                    case N:
                        nr.x= r.x + r.width/2 - r.height/2;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case E:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y + r.height/2 - r.width/2;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case W:
                        nr.x= r.x;
                        nr.y= r.y + r.height/2 - r.width/2;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case S:
                        nr.x= r.x + r.width/2 - r.height/2;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;                        
                    case OutsideN:
                        nr.x= r.x + r.width/2 - r.height/2;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideS:
                        nr.x= r.x + r.width/2 - r.height/2;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;                        
                    case OutsideE:
                        nr.x= r.x;
                        nr.y= r.y + r.height/2 - r.width/2;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideW:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y + r.height/2 - r.width/2;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideNNE:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideNNW:
                        nr.x= r.x;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideSSE:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideSSW:
                        nr.x= r.x;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                        
                    case OutsideNE:
                        nr.x= r.x;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideNW:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideSE:
                        nr.x= r.x;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;
                    case OutsideSW:
                        nr.x= r.x + r.width - r.height;
                        nr.y= r.y + r.height - r.width;
                        nr.width= r.height;
                        nr.height= r.width;
                        break;                        
                    default:
                        logger.info("this rotation is not supported");
                }
                r= nr;
            }
        }
        
        return r;
    }

    private Rectangle getAnnotationBubbleBoundsNoRotation() {
        Rectangle anchor= getAnchorBounds();
        DasCanvas canvas= getCanvas();
        Rectangle r;
        if ( gtr==null ) {
            if ( img==null ) {
                r= new Rectangle( 0, 0, (int)(100* scale), (int)(100* scale) );
            } else {
                r= new Rectangle( 0, 0, (int)(img.getWidth() * scale), (int)(img.getHeight()*scale) );
            }
        } else {
            try {
                r= gtr.getBounds(); 
            } catch ( IllegalArgumentException ex ) {
                int em1 = (int)getEmSize();
                r= new Rectangle( 0, 0, em1, em1 ); // possibly not initialized yet.
            }
        }
        
        int em = (int)getEmSize(); // (int)GraphUtil.parseLayoutLength( padding, r.width, getEmSize() );
        
        int xoffset=0;
        int yoffset=0;
        if ( anchorOffset.length()>0 ) {
            String[] ss= anchorOffset.split(",");
            if ( ss.length==2 ) {
                double[] dd;
                try {
                    if ( canvas==null ) {
                        xoffset= 0;
                        yoffset= 0;
                    } else {
                        dd= DasDevicePosition.parseLayoutStr(ss[0]);
                        xoffset= (int)( canvas.getWidth() * dd[0] + em * dd[1] + dd[2] );
                        dd= DasDevicePosition.parseLayoutStr(ss[1]);
                        yoffset= (int)( canvas.getHeight() * dd[0] + em * dd[1] + dd[2] );
                    }
                } catch ( NumberFormatException | ParseException ex ) {
                    logger.log( Level.WARNING, null, ex );
                    xoffset= 0;
                    yoffset= 0;
                }                
            } else {
                logger.log(Level.WARNING, "anchorOffset is misformatted: {0}", anchorOffset);
            }
        }
        
        int ipadding= (int)GraphUtil.parseLayoutLength( padding, r.width, getEmSize() );

        r.width+= ipadding*2;
        r.height+= ipadding*2;
            
        switch (anchorPosition) {
            case NW:
            case SW:
            case W:                
                r.x = anchor.x + xoffset ;
                break;
            case OutsideN:
            case OutsideS:
            case Center:
            case N:
            case S:
                r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
                break;
            case OutsideE:
                r.x = anchor.x + anchor.width + xoffset;
                break;
            case OutsideW:
                r.x = anchor.x - r.width - xoffset;
                break;
            case NE:
            case SE:
            case E:
                r.x = anchor.x + anchor.width - r.width - xoffset;
                break;
            case OutsideNE:
            case OutsideSE:
                r.x = anchor.x + anchor.width +  xoffset;
                break;
            case OutsideNW:
            case OutsideSW:
                r.x = anchor.x - r.width - xoffset;
                break;
            case OutsideNNW:
            case OutsideSSW:
                r.x = anchor.x +  xoffset ;
                break;
            case OutsideNNE:
            case OutsideSSE:
                r.x = anchor.x + anchor.width - r.width - xoffset;
                break;
            default:
                break;
        }
        
        switch (anchorPosition) {
            case NW:
            case NE:
            case N:
                r.y = anchor.y + yoffset ;
                break;
            case OutsideN:
                r.y = anchor.y - (int)r.getHeight() - yoffset ;
                break;
            case OutsideS:
            case OutsideSSW:
            case OutsideSSE:
                r.y = anchor.y + anchor.height + yoffset ;
                break;
            case OutsideE:
            case OutsideW:
                r.y = anchor.y + anchor.height/2 - (int)( r.getHeight() / 2 ) - yoffset;
                break;
            case OutsideNE:
            case OutsideNW:
                r.y = anchor.y + yoffset ;
                break;
            case OutsideSE:
            case OutsideSW:
            case SW:
            case SE: 
            case S:
                r.y = anchor.y + anchor.height - r.height - yoffset;
                break;
            case OutsideNNW:
            case OutsideNNE:
                r.y = anchor.y - (int)r.getHeight() - yoffset ;
                break;
            case Center:
            case W:
            case E:
                r.y = anchor.y + anchor.height/2 - (int)( r.getHeight() / 2 ) - yoffset;
                break;
            default:
                break;
        }
                
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
        if ( this.gtr!=null ) {
            this.gtr.setString( this.getFont(), getString() );
        }
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
        if ( gtr!=null ) gtr.setString( g, getString() );
        resize();
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

    /**
     * get the location within the box where the annotation will be drawn.
     * @return anchorPosition 
     */    
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
    
    PropertyChangeListener plotListener= new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            logger.finest("plot change, resizing");
            if ( evt.getPropertyName().equals("paintingForPrint") ) {
                logger.finest("plot change is trivial, ignoring");
            } else {
                Runnable run= new Runnable() {
                    public void run() {
                        resize();
                    }
                };
                if ( SwingUtilities.isEventDispatchThread() ) {
                    run.run();
                } else {
                    SwingUtilities.invokeLater(run);
                }
            }
        }
    };
            
    public void setPlot( DasPlot p ) {
        if ( this.plot!=null ) {
            this.plot.getXAxis().removePropertyChangeListener(plotListener);
            this.plot.getYAxis().removePropertyChangeListener(plotListener);
        }
        if ( p!=null ) {
            p.getXAxis().addPropertyChangeListener(plotListener);
            p.getYAxis().addPropertyChangeListener(plotListener);
        }
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
        private Datum pointAtX = Datum.create(0);

    public static final String PROP_POINTATX = "pointAtX";

    public Datum getPointAtX() {
        return pointAtX;
    }

    public void setPointAtX(Datum pointAtX) {
        Datum oldPointAtX = this.pointAtX;
        this.pointAtX = pointAtX;
        firePropertyChange(PROP_POINTATX, oldPointAtX, pointAtX);
    }

    private Datum pointAtY = Datum.create(0);

    public static final String PROP_POINTATY = "pointAtY";

    public Datum getPointAtY() {
        return pointAtY;
    }

    public void setPointAtY(Datum pointAtY) {
        Datum oldPointAtY = this.pointAtY;
        this.pointAtY = pointAtY;
        firePropertyChange(PROP_POINTATY, oldPointAtY, pointAtY);
    }
    
    private String referenceX = "";

    public static final String PROP_REFERENCEX = "referenceX";

    public String getReferenceX() {
        return referenceX;
    }

    /**
     * single or semicolon-separated list of values.
     * @param referenceX 
     */
    public void setReferenceX(String referenceX) {
        String oldReferenceX = this.referenceX;
        this.referenceX = referenceX;
        if ( !oldReferenceX.equals(this.referenceX) ) { 
            if ( boundsCalculated==true ) {
                boundsCalculated= false;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        resize();
                    }
                } );
            }
            repaint();
        }
        firePropertyChange(PROP_REFERENCEX, oldReferenceX, referenceX);
    }

    private String referenceY = "";

    public static final String PROP_REFERENCEY = "referenceY";

    public String getReferenceY() {
        return referenceY;
    }

    /**
     * single or semicolon-separated list of values.
     * @param referenceY 
     */
    public void setReferenceY(String referenceY) {
        String oldReferenceY = this.referenceY;
        this.referenceY = referenceY;
        if ( !oldReferenceY.equals(this.referenceY ) ) { 
            if ( boundsCalculated==true ) {
                boundsCalculated= false;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        resize();
                    }
                } );
            }
            repaint();
        }
        firePropertyChange(PROP_REFERENCEY, oldReferenceY, referenceY);
    }
    
    private int rotate = 0;

    public static final String PROP_ROTATE = "rotate";

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        int oldRotate = this.rotate;
        this.rotate = rotate;
        if ( boundsCalculated==true ) {
            boundsCalculated= false;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    resize();
                }
            } );
        }
        repaint();
        firePropertyChange(PROP_ROTATE, oldRotate, rotate);
    }

    public static final String PROP_GLOW = "glow";

    public boolean isGlow() {
        if ( this.gtr==null ) {
            return false;
        } else {
            return this.gtr.isGlow();
        }
    }

    public void setGlow(boolean glow) {
        boolean oldGlow = isGlow();
        if ( this.gtr!=null ) {
            this.gtr.setGlow(glow);
            firePropertyChange(PROP_GLOW, oldGlow, glow);
        } 
    }
    
    private PlotSymbol symbol = DefaultPlotSymbol.NONE;

    public static final String PROP_SYMBOL = "symbol";

    public PlotSymbol getSymbol() {
        return symbol;
    }

    /**
     * set the symbol used to mark the reference locations
     * @param symbol 
     */
    public void setSymbol(PlotSymbol symbol) {
        PlotSymbol oldSymbol = this.symbol;
        this.symbol = symbol;
        firePropertyChange(PROP_SYMBOL, oldSymbol, symbol);
    }
    
    private String pointAtOffset="";

    public static final String PROP_POINTATOFFSET = "pointAtOffset";

    public String getPointAtOffset() {
        return pointAtOffset;
    }

    public void setPointAtOffset(String pointAtOffset) {
        String oldPointAtOffset = this.pointAtOffset;
        this.pointAtOffset = pointAtOffset;
        firePropertyChange(PROP_POINTATOFFSET, oldPointAtOffset, pointAtOffset);
    }

    private boolean showArrow = false;

    public static final String PROP_SHOWARROW = "showArrow";

    public boolean isShowArrow() {
        return showArrow;
    }

    public void setShowArrow(boolean showArrow) {
        boolean oldShowArrow = this.showArrow;
        this.showArrow = showArrow;
        firePropertyChange(PROP_SHOWARROW, oldShowArrow, showArrow);
    }

    
    private BorderType anchorBorderType = BorderType.NONE;

    public static final String PROP_ANCHORBORDERTYPE = "anchorBorderType";

    public BorderType getAnchorBorderType() {
        return anchorBorderType;
    }

    public void setAnchorBorderType(BorderType anchorBorderType) {
        BorderType oldAnchorBorderType = this.anchorBorderType;
        this.anchorBorderType = anchorBorderType;
        repaint();
        firePropertyChange(PROP_ANCHORBORDERTYPE, oldAnchorBorderType, anchorBorderType);
    }

    private Color anchorBackground = new Color(0, 0, 0, 0);

    public static final String PROP_ANCHORBACKGROUND = "anchorBackground";

    public Color getAnchorBackground() {
        return anchorBackground;
    }

    public void setAnchorBackground(Color anchorBackground) {
        Color oldAnchorBackground = this.anchorBackground;
        this.anchorBackground = anchorBackground;
        repaint();
        firePropertyChange(PROP_ANCHORBACKGROUND, oldAnchorBackground, anchorBackground);
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
    
    private boolean splitAnchorType = false;

    public static final String PROP_SPLITANCHORTYPE = "splitAnchorType";

    public boolean isSplitAnchorType() {
        return splitAnchorType;
    }

    public void setSplitAnchorType(boolean splitAnchorType) {
        boolean oldSplitAnchorType = this.splitAnchorType;
        this.splitAnchorType = splitAnchorType;
        firePropertyChange(PROP_SPLITANCHORTYPE, oldSplitAnchorType, splitAnchorType);
    }

    private AnchorType verticalAnchorType = AnchorType.CANVAS;

    public static final String PROP_VERTICALANCHORTYPE = "verticalAnchorType";

    public AnchorType getVerticalAnchorType() {
        return verticalAnchorType;
    }

    /**
     * when splitAnchorType==True, use this for the vertical position instead
     * of the anchorType property.  
     * @param verticalAnchorType 
     */
    public void setVerticalAnchorType(AnchorType verticalAnchorType) {
        AnchorType oldVerticalAnchorType = this.verticalAnchorType;
        this.verticalAnchorType = verticalAnchorType;
        repaint();
        firePropertyChange(PROP_VERTICALANCHORTYPE, oldVerticalAnchorType, verticalAnchorType);
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

    
    private String lineThickness = "1.5px";

    public static final String PROP_LINETHICKNESS = "lineThickness";

    public String getLineThickness() {
        return lineThickness;
    }

    public void setLineThickness(String lineThickness) {
        String oldLineThickness = this.lineThickness;
        this.lineThickness = lineThickness;
        repaint();
        firePropertyChange(PROP_LINETHICKNESS, oldLineThickness, lineThickness);
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
        resize();
    }
}
