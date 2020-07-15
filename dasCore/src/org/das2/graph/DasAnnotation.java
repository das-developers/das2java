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
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.LoggerManager;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;

/**
 * This makes a DasCanvasComponent for GrannyTextRenderer, and 
 * optionally adds an arrow to point at things.
 * 
 * TODO: See http://autoplot.org//developer.annotations
 * 
 * @author Jeremy
 */
public class DasAnnotation extends DasCanvasComponent {

    private static final Logger logger= LoggerManager.getLogger("das2.graph.annotation");
    
    String templateString;
    GrannyTextRenderer gtr;
    BufferedImage img;

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
                this.gtr = new GrannyTextRenderer();
            }
        } else {
            this.gtr = new GrannyTextRenderer();
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

        MouseModule mm = new MoveComponentMouseModule(this) {
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
                    setPointAtX(x);
                    setPointAtY(y);
                    
                    if ( getAnchorType()==AnchorType.CANVAS ) {
                        setXrange( new DatumRange(x,x) );
                        setYrange( new DatumRange(y,y) );
                    } else if ( getAnchorType()==AnchorType.DATA ) {
                        int dx = (int)( plot.getXAxis().transform(x) - plot.getXAxis().transform(oldx) );
                        int dy = (int)( plot.getYAxis().transform(y) - plot.getYAxis().transform(oldy) );
                        adjustAnchorOffset( -dx, -dy );
                        resize();
                    } else if ( getAnchorType()==AnchorType.PLOT ) {
                        int dx = (int)( plot.getXAxis().transform(x) - plot.getXAxis().transform(oldx) );
                        int dy = (int)( plot.getYAxis().transform(y) - plot.getYAxis().transform(oldy) );
                        adjustAnchorOffset( -dx, -dy );
                        resize();
                    }
                    setShowArrow(true);
                }
            }            
        };
        this.getDasMouseInputAdapter().addMouseModule(pointAtMouseModule);

    }

    private void adjustDataRanges( int dx, int dy ) {
        
    }
    
    private void adjustAnchorOffset( int dx, int dy ) {
        if ( anchorPosition==AnchorPosition.NW ) {
        } else if ( anchorPosition==AnchorPosition.N ) {
        } else if ( anchorPosition==AnchorPosition.OutsideN ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.NE ) {
            dx= -dx;
        } else if ( anchorPosition==AnchorPosition.OutsideNE ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideSE ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideNW ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.OutsideSW ) {
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
        } else if ( anchorPosition==AnchorPosition.Center ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.W ) {
            dy= -dy;
        } else if ( anchorPosition==AnchorPosition.E ) {
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
            this.setAnchorOffset(offset);
        } else {
            try {
                String[] ss= offset.split(",",-2);
                double[] dd;
                dd= DasDevicePosition.parseLayoutStr(ss[0]);
                dd[1]= dd[1] + dx/em;
                ss[0]= DasDevicePosition.formatFormatStr(dd);
                dd= DasDevicePosition.parseLayoutStr(ss[1]);
                dd[1]= dd[1] + dy/em;
                ss[1]= DasDevicePosition.formatFormatStr(dd);
                offset= ss[0]+","+ss[1];
                this.setAnchorOffset(offset);
            } catch (ParseException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

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
                        gtr= new GrannyTextRenderer();
                        gtr.setString( this.getGraphics(), getString() );
                    }
                } else {
                    gtr= new GrannyTextRenderer();
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
                gtr= new GrannyTextRenderer();
                gtr.setString( this.getGraphics(), url );
            }
        }
        this.url = url;
        firePropertyChange(PROP_URL, oldUrl, url);
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
            r.add( r.x+r.width+1, r.y+r.height+1 );
            if ( anchorType==AnchorType.CANVAS || plot==null ) {
                r= r.intersection( new Rectangle(0,0,getCanvas().getWidth(),getCanvas().getHeight()) ); // clip at canvas boundaries
            } else {
                r= r.intersection( DasDevicePosition.toRectangle( plot.getRow(), plot.getColumn() ) ); // clip at plot boundaries
            }
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
            int headx;
            int heady;
            if ( plot!=null ) {
                try {
                    headx= (int)plot.getXAxis().transform(pointAtX);
                } catch ( InconvertibleUnitsException ex ) {
                    headx= (int)plot.getXAxis().transform(pointAtX.value(),plot.getXAxis().getUnits());
                }
                try {
                    heady= (int)plot.getYAxis().transform(pointAtY);
                } catch ( InconvertibleUnitsException ex ) {
                    heady= (int)plot.getYAxis().transform(pointAtY.value(),plot.getYAxis().getUnits());
                }                    
                r.add( headx, heady );
            }
        }
        int s= Math.max( getFont().getSize()/5, 3 );
        Rectangle result= new Rectangle( r.x-s, r.y-s, r.width+s*2+1, r.height+s*2+1 );
        
        logger.exiting( "DasAnnotation","calcBounds", result);
        return result;
    }

    @Override
    public void paintComponent(Graphics g1) {
        
        Graphics2D g = (Graphics2D) g1.create(); 
        
        double em2 = g.getFont().getSize();
        
        Stroke stroke0= g.getStroke();
        
        g.setStroke(new BasicStroke((float) (em2 / 8), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
        
        g.translate( -getX(), -getY() );

        if ( anchorType!=AnchorType.CANVAS && plot!=null ) {
            Rectangle r= DasDevicePosition.toRectangle( plot.getRow(), plot.getColumn() );
            g.setClip( r );
        }
        
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

        int em = (int) getEmSize() / 2;

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

            int headx= 0;
            int heady= 0;
            if ( plot!=null ) {
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
            }
            
            Point head = new Point(headx,heady);
            
            Graphics2D g2 = (Graphics2D) g.create();
            
//            if ( anchorType==AnchorType.CANVAS ) {
//                g2.setClip(null);
//            } else {
//                g2.setClip( g.getClip() );
//            }
            Point2D tail2d= new Point2D.Double( r.x + r.width/2, r.y + r.height/2 );
            Point2D head2d= new Point2D.Double( head.x, head.y );
            Rectangle2D rect2d= new Rectangle2D.Double(r.x, r.y, r.width, r.height );
            Point2D p2d= GraphUtil.lineRectangleIntersection( tail2d, head2d, rect2d );
            Point p= p2d==null ? head : new Point( (int)p2d.getX(), (int)p2d.getY() );
            
            g2.setStroke( new BasicStroke( (float) (em2/4), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND ) );
            
            if ( pointAtOffset.length()>0 ) {
                Line2D line= new Line2D.Double( head.x, head.y, p.x, p.y );
                double lengthPixels= GraphUtil.parseLayoutLength( pointAtOffset, line.getP1().distance(line.getP2()), getEmSize() );
                Line2D newLine= GraphUtil.shortenLine(line, lengthPixels, 0 );
                head= new Point( (int)newLine.getP1().getX(), (int)newLine.getP1().getY() );
            }
                                    
            Color glowColor= getCanvas().getBackground();
            g2.setColor( new Color( glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 128 ) );
            Arrow.paintArrow(g2, head, p, em2, this.arrowStyle );

            g2.setStroke( stroke0 );            
            g2.setColor( fore );
            Arrow.paintArrow(g2, head, p, em2, this.arrowStyle );

            g2.dispose();
        }

        g.setColor(back);

        if ( gtr==null || !getString().equals("") ) {
            if (borderType == BorderType.RECTANGLE || borderType == BorderType.NONE) {
                g.fill(r);
            } else if (borderType == BorderType.ROUNDED_RECTANGLE) {
                g.fillRoundRect(r.x, r.y, r.width, r.height, em * 2, em * 2);
            }

            g.setColor(ltextColor);

            if ( gtr!=null ) {
                try {
                    gtr.draw(g, r.x+em, r.y + em + (float) gtr.getAscent() );
                } catch ( IllegalArgumentException ex ) {
                    ex.printStackTrace();
                    return;
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
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g.drawImage( localImage, r.x+em, r.y+em, newWidth, newHeight, this );        
                    } else {
                        BufferedImage resized = org.das2.util.ImageUtil.getScaledInstance( localImage, (int)Math.sqrt( newWidth*newWidth + newHeight*newHeight ) );
                        g.drawImage( resized, r.x+em, r.y+em, this );                        
                    }
                    

                } else {
                    g.drawImage( localImage, r.x+em, r.y+em, this );
                }
            }
        
            g.setColor(fore);
                
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
                    if ( anchorRect.width==0 ) {
                        g.draw( new Line2D.Double( anchorRect.x, anchorRect.y, anchorRect.x, anchorRect.y+anchorRect.height ) );
                    } else if ( anchorRect.height==0 ) {
                        g.draw( new Line2D.Double( anchorRect.x, anchorRect.y, anchorRect.x+anchorRect.width, anchorRect.y ) );
                    } else {
                        g.draw(anchorRect);
                    }
                } else if ( anchorBorderType==BorderType.ROUNDED_RECTANGLE ) {
                    g.drawRoundRect(anchorRect.x, anchorRect.y, anchorRect.width, anchorRect.height, em * 2, em * 2);
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

    /**
     * return the bounds of that thing we are anchored to.  Note 
     * AnchorType.DATA is treated the same as AnchorType.PLOT, but the thought
     * is that it could look at the renderer's data.
     * @return the bounds of that thing we are anchored to.
     */
    private Rectangle getAnchorBounds() {
        Rectangle anchorRect= new Rectangle();
        if ( ( anchorType==AnchorType.PLOT || anchorType==AnchorType.DATA ) && plot!=null && xrange!=null && yrange!=null ) {
            if ( anchorBorderType==BorderType.NONE && showArrow ) { // this is really confusing, when you can't see the anchor.
                try {
                    anchorRect.x= (int)(plot.getXAxis().transform( pointAtX ) );
                } catch ( InconvertibleUnitsException ex ) {
                    if ( pointAtX.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement(plot.getXAxis().getUnits())  ) {
                        anchorRect.x= (int)(plot.getXAxis().transform( pointAtX.value(), plot.getXAxis().getUnits() ) );
                    } else {
                        logger.info("unable to convert x units for annotation");
                        anchorRect.x= getColumn().getDMiddle();
                    }
                }
                try {
                    anchorRect.y= (int)(plot.getYAxis().transform( pointAtY ) );
                } catch ( InconvertibleUnitsException ex ) {
                    if ( pointAtY.getUnits()==Units.dimensionless && UnitsUtil.isRatioMeasurement(plot.getYAxis().getUnits()) ) {
                        anchorRect.y= (int)(plot.getYAxis().transform( pointAtY.value(), plot.getYAxis().getUnits() ) );
                    } else {
                        logger.info("unable to convert y units for annotation");
                        anchorRect.y= getRow().getDMiddle();
                    }
                }
                anchorRect.width= 1;
                anchorRect.height= 1;
            } else {
                anchorXToData(anchorRect);
                anchorYToData(anchorRect);
            }
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

        int em = (int) getEmSize();
        
        Rectangle anchor= getAnchorBounds();
                        
        Rectangle r;
        if ( gtr==null ) {
            if ( img==null ) {
                r= new Rectangle( 0, 0, (int)(100* scale), (int)(100* scale) );
            } else {
                r= new Rectangle( 0, 0, (int)(img.getWidth() * scale), (int)(img.getHeight()*scale) );
            }
        } else {
            r= gtr.getBounds();
        }

        int xoffset=0;
        int yoffset=0;
        
        if ( anchorOffset.length()>0 ) {
            String[] ss= anchorOffset.split(",");
            if ( ss.length==2 ) {
                double[] dd;
                try {
                    dd= DasDevicePosition.parseLayoutStr(ss[0]);
                    xoffset= (int)( getCanvas().getWidth() * dd[0] + em * dd[1] + dd[2] );
                    dd= DasDevicePosition.parseLayoutStr(ss[1]);
                    yoffset= (int)( getCanvas().getHeight() * dd[0] + em * dd[1] + dd[2] );
                } catch ( NumberFormatException | ParseException ex ) {
                    logger.log( Level.WARNING, null, ex );
                    xoffset= 0;
                    yoffset= 0;
                }                
            } else {
                logger.log(Level.WARNING, "anchorOffset is misformatted: {0}", anchorOffset);
            }
        }

        if ( null!=anchorPosition ) switch (anchorPosition) {
            case NW:
                r.x = anchor.x + em + xoffset ;
                r.y = anchor.y + em + yoffset ;
                break;
            case OutsideN:
                r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
                r.y = anchor.y - (int)r.getHeight() - yoffset ;
                break;
            case OutsideS:
                r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
                r.y = anchor.y + anchor.height + em + yoffset ;
                break;
            case OutsideE:
                r.x = anchor.x + anchor.width + em + xoffset;
                r.y = anchor.y + anchor.height/2 - (int)( r.getHeight() / 2 ) - yoffset;
                break;
            case OutsideW:
                r.x = anchor.x - r.width - xoffset;
                r.y = anchor.y + anchor.height/2 - (int)( r.getHeight() / 2 ) - yoffset;
                break;
            case NE:
                r.x = anchor.x + anchor.width - r.width - xoffset;
                r.y = anchor.y + em + yoffset ;
                break;
            case OutsideNE:
                r.x = anchor.x + anchor.width + em + xoffset;
                r.y = anchor.y + em + yoffset;
                break;
            case OutsideSE:
                r.x = anchor.x + anchor.width + em + xoffset;
                r.y = anchor.y + anchor.height - r.height - yoffset;
                break;
            case OutsideNW:
                r.x = anchor.x - r.width - xoffset;
                r.y = anchor.y + em + yoffset;
                break;
            case OutsideSW:
                r.x = anchor.x - r.width - xoffset;
                r.y = anchor.y + anchor.height - r.height - yoffset;
                break;
            case SW:
                r.x = anchor.x + em + xoffset;
                r.y = anchor.y + anchor.height - r.height - yoffset;
                break;
            case SE:
                r.x = anchor.x + anchor.width - r.width - xoffset;
                r.y = anchor.y + anchor.height - r.height - yoffset;
                break;
            case OutsideNNW:
                r.x = anchor.x + em + xoffset ;
                r.y = anchor.y - (int)r.getHeight() - yoffset ;
                break;
            case OutsideNNE:
                r.x = anchor.x + anchor.width - r.width - xoffset;
                r.y = anchor.y - (int)r.getHeight() - yoffset ;
                break;
            case OutsideSSW:
                r.x = anchor.x + em + xoffset ;
                r.y = anchor.y + anchor.height + em + yoffset ;
                break;
            case OutsideSSE:
                r.x = anchor.x + anchor.width - r.width - xoffset;
                r.y = anchor.y + anchor.height + em + yoffset ;
                break;
            case Center:
                r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
                r.y = anchor.y + anchor.height/2 - (int)( r.getHeight() / 2 ) - yoffset;
                break;
            case N:
                r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
                r.y = anchor.y + em + yoffset ;
                break;
            case W:
                r.x = anchor.x + em + xoffset;
                r.y = anchor.y + anchor.height/2 - (int)( r.getHeight() / 2 ) - yoffset;
                break;
            case E:
                r.x = anchor.x + anchor.width  - r.width - xoffset;
                r.y = anchor.y + anchor.height/2 - (int)( r.getHeight() / 2 ) - yoffset;
                break;
            case S:
                r.x = anchor.x + anchor.width/2 - (int)( r.getWidth() / 2 ) + xoffset ;
                r.y = anchor.y + anchor.height - r.height - yoffset;
                break;
            default:
                break;
        }
        
        if ( gtr==null ) {
            r.x-= em/2;
            r.y-= em/2;
            r.width+= em;
            r.height+= em;            
        } else {
            r.x-= em;
            r.y-= em;
            r.width+= em;
            r.height+= em;
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
        p.getXAxis().addPropertyChangeListener(plotListener);
        p.getYAxis().addPropertyChangeListener(plotListener);
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
