/*
 * ContoursRenderer.java
 *
 * Created on December 7, 2007, 2:47 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.das2.graph;

import org.das2.DasException;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import org.das2.qds.math.Contour;
import java.awt.BasicStroke;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.JoinDataSet;
import org.das2.qds.ops.Ops;
import org.jdesktop.beansbinding.Converter;

/**
 * Renderer for making contour plots.  
 * @author jbf
 */
public class ContoursRenderer extends Renderer {

    public ContoursRenderer() {
    }
    
    GeneralPath[] paths;
    String[] pathLabels;

    Converter fontConverter= null;
    
    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param ds the dataset.
     * @return a bounding box
     * @see org.das2.qds.examples.Schemes#boundingBox() 
     */
    public static QDataSet doAutorange( QDataSet ds ) {

        QDataSet xds;
        QDataSet yds;

        if ( ds.rank()!=2 ) {
            throw new IllegalArgumentException("ds rank must be 2");
        }
        
        xds= SemanticOps.xtagsDataSet(ds);
        yds= SemanticOps.ytagsDataSet(ds);

        QDataSet xrange= doRange( xds );
        QDataSet yrange= doRange( yds );

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }

    private static QDataSet doRange( QDataSet xds ) {
        QDataSet xrange= Ops.extent(xds);
        if ( xrange.value(1)==xrange.value(0) ) {
            if ( !"log".equals( xrange.property(QDataSet.SCALE_TYPE)) ) {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)-1, xrange.value(1)+1 } ).setUnits( SemanticOps.getUnits(xrange) );
            } else {
                xrange= DDataSet.wrap( new double[] { xrange.value(0)/10, xrange.value(1)*10 } ).setUnits( SemanticOps.getUnits(xrange) );
            }
        }
        xrange= Ops.rescaleRangeLogLin(xrange, -0.1, 1.1 );
        return xrange;
    }    

    @Override
    public void setParent(DasPlot parent) {
        fontConverter= GraphUtil.getFontConverter( parent, "sans-9" );
        super.setParent(parent); //To change body of generated methods, choose Tools | Templates.
    }
    
    
    
    /**
     * return false if the inputs are okay, true if there's no data, etc.
     * @param lparent the parent
     * @return false if the inputs are okay, true if there's no data
     */
    private boolean checkInputs( DasPlot lparent ) {
        QDataSet tds = (QDataSet) getDataSet();
        
        if (tds == null) {
            lparent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return true;
        }
        if (tds.rank()!=2 ) {
            lparent.postMessage(this, "dataset must be rank 2", DasPlot.INFO, null, null);
            return true;
        }

        if ( contoursDs==null ) {
            return true;
        }
        
        if (paths == null) { // findbugs experiment: does a single read, which should be thread-safe, trigger findbugs IS2_INCONSISTENT_SYNC?
            return true;
        }
        
        return false;
    }
    
    @Override
    public synchronized void render(Graphics g1, DasAxis xAxis, DasAxis yAxis) {
        
        DasPlot lparent= getParent();
        if ( lparent==null ) return; // ???
        
        Graphics2D g = (Graphics2D) g1;
        if ( ds==null ) {
            lparent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }
        if ( ds.rank()!=2 ) {
            lparent.postMessage(this, "dataset rank must be 2", DasPlot.INFO, null, null);
            return;
        }
        if ( contoursDs.length()==0 ) {
            lparent.postMessage(this, "no contours are found", DasPlot.INFO, null, null);
            return;
        }
        
        QDataSet _xds= SemanticOps.xtagsDataSet(ds);
        if ( _xds.rank()!=1 ) {
            lparent.postMessage(this, "xtags must be rank 1", DasPlot.INFO, null, null);
            return;
        }
        QDataSet _yds= SemanticOps.ytagsDataSet(ds);
        if ( _yds.rank()!=1 ) {
            lparent.postMessage(this, "ytags must be rank 1", DasPlot.INFO, null, null);
            return;
        }

        if ( paths==null ) {
            return;
        }
        
        if (lparent.getCanvas().isAntiAlias()) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        if ( checkInputs(lparent) ) return;
        
        g.setColor(color);
        g.setStroke( new BasicStroke((float)lineThick) );
                
        if (drawLabels) {
             Area labelClip = paintLabels(g);
            
            Shape rclip = g.getClip() == null ? new Rectangle(lparent.getX(), lparent.getY(), lparent.getWidth(), lparent.getHeight()) : g.getClip();
            Area clip = new Area(rclip);
            clip.subtract(labelClip);
            g.setClip(clip);

        }

        for (GeneralPath path : paths) {
            if (path != null) {
                g.draw(path);
            }
        }

    }

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.contours= getControl(CONTROL_KEY_LEVELS, contours );
        this.drawLabels= getBooleanControl(CONTROL_KEY_LABELS, drawLabels );
        this.lineThick= getDoubleControl( CONTROL_KEY_LINE_THICK, lineThick );
        this.labelCadence= getControl(CONTROL_KEY_LABEL_CADENCE, labelCadence );
        this.color= getColorControl( CONTROL_KEY_COLOR,  color );
        this.format= getControl( CONTROL_KEY_FORMAT, format );
        setFontSize( getControl( CONTROL_KEY_FONT_SIZE, fontSize) );
        setLabelOrient( getControl( CONTROL_KEY_LABEL_ORIENT, labelOrient ) );
        updateContours();
    }
    
    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put(CONTROL_KEY_LEVELS, contours );
        controls.put(CONTROL_KEY_LABELS, encodeBooleanControl( drawLabels ) );
        controls.put( CONTROL_KEY_LINE_THICK, String.valueOf(lineThick) );
        controls.put(CONTROL_KEY_LABEL_CADENCE, String.valueOf(labelCadence) );
        controls.put( CONTROL_KEY_COLOR, encodeColorControl( color ) );
        controls.put( CONTROL_KEY_FORMAT, format );
        controls.put( CONTROL_KEY_FONT_SIZE, fontSize );
        controls.put( CONTROL_KEY_LABEL_ORIENT, labelOrient );
        return Renderer.formatControl(controls);
    }
    
    public static final String CONTROL_KEY_LEVELS = "levels";
    public static final String CONTROL_KEY_LABELS = "labels";
    public static final String CONTROL_KEY_LABEL_CADENCE = "labelCadence";
    public static final String CONTROL_KEY_FORMAT = "format";
    public static final String CONTROL_KEY_LABEL_ORIENT = "labelOrient";
    
    @Override
    public boolean acceptsDataSet(QDataSet ds) {
        if ( ds==null ) return true;
        if ( ds.rank()!=2 ) return false;
        QDataSet xds= SemanticOps.xtagsDataSet(ds);
        if ( xds.rank()!=1 ) return false;
        QDataSet yds= SemanticOps.ytagsDataSet(ds);
        if ( yds.rank()!=1 ) return false;
        return true;
    }

    
    @Override
    public void setDataSet(QDataSet ds) {
        super.setDataSet(ds); //To change body of generated methods, choose Tools | Templates.
        if ( acceptsDataSet(ds) ) {
            updateContours();
        }        
    }

    private QDataSet contoursDs; // the contours
    
    private synchronized void updateContours() {
        QDataSet tds= (QDataSet) getDataSet();
        if ( tds==null ) {
            contoursDs= null;
            return;
        }
        
        if ( tds.rank()==2 && tds.length(0)==3 && tds.property(QDataSet.DEPEND_0)!=null ) {
            // hey it was already done...
            logger.fine("contour was already performed");
            contoursDs= tds;
            return;
        }
        
        Units units = SemanticOps.getUnits(tds);

        String[] cons = this.contours.trim().split(",");
        double[] dcons = new double[cons.length];
        for (int i = 0; i < cons.length; i++) {
            if (cons[i].trim().equals("")) {
                continue;
            }
            double c = Double.parseDouble(cons[i]);
            dcons[i] = c;
        }
        DatumVector dv = DatumVector.newDatumVector(dcons, units );        
        contoursDs= Contour.contour(tds, DDataSet.wrap(dv.toDoubleArray(units) ) );
    }
    
    private String fontSize = "8pt";

    public static final String PROP_FONTSIZE = "fontSize";

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        String oldFontSize = this.fontSize;
        this.fontSize = fontSize;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_FONTSIZE, oldFontSize, fontSize);
    }

    /**
     * format, empty string means use the default format.
     */
    public static final String PROP_FORMAT= "format";

    private String format="";

    public String getFormat() {
        return format;
    }

    /**
     * explicitly set the format.
     * format is found there.
     * @param value
     */
    public void setFormat(String value) {
        String oldValue= this.format;
        this.format = value;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_FORMAT, oldValue, value );
        propertyChangeSupport.firePropertyChange(PROP_CONTROL, null, getControl() );
    }

    /**
     * preference for orientation of labels, if any.  One of "", "up"
     */
    private String labelOrient = "";

    public static final String PROP_LABELORIENT = "labelOrient";

    public String getLabelOrient() {
        return labelOrient;
    }

    public void setLabelOrient(String labelOrient) {
        String oldLabelOrient = this.labelOrient;
        this.labelOrient = labelOrient;
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_LABELORIENT, oldLabelOrient, labelOrient);
    }

    private double getPixelLength( String s, double em ) {
        try {
            double[] dd= DasDevicePosition.parseLayoutStr((String)s);
            if ( dd[1]==1 && dd[2]==0 ) {
                return em;
            } else {
                double parentSize= em;
                double newSize= dd[1]*parentSize + dd[2];
                return (float)newSize;
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
            return 0.f;
        }
    }
    
    /**
     * returns clip, in the canvas reference frame
     * @param g the graphics context.
     * @return the bounds for the area affected.
     */
    private Area paintLabels(final Graphics2D g) {

        Area clip = new Area();

        // do labels
        AffineTransform at0 = g.getTransform();
        
        String lfontSize= fontSize;
        if ( lfontSize.length()==0 ) lfontSize= "8pt";
        Font font = getParent().getFont().deriveFont( ((Number)fontConverter.convertForward(lfontSize)).floatValue() );
        if ( font.getSize2D()==0.0 ) { // typo
            logger.info("parsed font size is 0.0, using 8pt");
            font= font.deriveFont(8.f);
        }

        g.setFont(font);

        GeneralPath[] lpaths= getPaths();
        
        double labelCadencePixels= getPixelLength( labelCadence, font.getSize2D() );
            
        double minLength= 20;
        for (int i = 0; i < lpaths.length; i++) {
            if (lpaths[i] == null) {
                continue;
            }
            String label = pathLabels[i];
            GeneralPath p = lpaths[i];

            if (p != null) {

                PathIterator it1 = p.getPathIterator(null);
                PathIterator it2 = p.getPathIterator(null);

                while (!it1.isDone()) {

                    double len = GraphUtil.pointsAlongCurve(it1, null, null, null, true);

                    int nlabel = 1 + (int) Math.floor( len / labelCadencePixels );

                    double phase = (len - ( nlabel-1 )  * labelCadencePixels ) / 2;

                    if ( len < minLength ) {
                        //advance it2.
                        GraphUtil.pointsAlongCurve(it2, null, null, null, true);

                    } else {
                        double[] lens = new double[nlabel*2];
                        double labelWidth=10; // approx.
                        if ( labelWidth > labelCadencePixels ) labelWidth= labelCadencePixels * 0.99;
                        for (int ilabel = 0; ilabel < nlabel; ilabel++) {
                            lens[ilabel*2] = phase + labelCadencePixels * ilabel;
                            lens[ilabel*2+1] = phase + labelCadencePixels * ilabel + labelWidth;
                        }
                        Point2D.Double[] points = new Point2D.Double[nlabel*2];
                        double[] orient = new double[nlabel*2];

                        //advance it2.
                        GraphUtil.pointsAlongCurve(it2, lens, points, orient, true);
                        
                        if ( labelOrient.equals("N") ) {
                            for (int ilabel = 0; ilabel < nlabel*2; ilabel++) {
                                if ( Math.abs(orient[ilabel])>Math.PI/2) orient[ilabel]+=Math.PI;
                            }                
                        }

                        FontMetrics fm= g.getFontMetrics(font);
                        
                        for (int ilabel = 0; ilabel < nlabel; ilabel++) {
                            AffineTransform at = new AffineTransform();
                            at.translate(points[ilabel*2].x, points[ilabel*2].y);
                            //double dx= points[ilabel*2+1].x - points[ilabel*2].x;
                            //double dy= points[ilabel*2+1].y - points[ilabel*2].y;
                            //double orient1= Math.atan2(dy,dx);
                            at.rotate(orient[ilabel*2]);
                            at.translate( 0, fm.getAscent()/2-1 );
                            //at.rotate(orient1);
                            

                            Rectangle2D sbounds = g.getFontMetrics().getStringBounds(label, g);
                            double w = sbounds.getWidth();
                            double emw= fm.getAscent()/3.; // space to on left and right of the label.

                            sbounds= new Rectangle2D.Double( sbounds.getX(), sbounds.getY(), w+emw, sbounds.getHeight() );
                            GeneralPath rect = new GeneralPath(sbounds);
                            rect.transform(AffineTransform.getTranslateInstance( -w / 2, 0));
                            rect.transform(at);
                            clip.add(new Area(rect));

                            AffineTransform gat= new AffineTransform(at0);
                            gat.concatenate(at);
                            
                            g.setTransform( gat );
                            g.setColor(color);
                            g.drawString(label, (int) ( (-w / 2) + emw/2 ), 0);
                        }
                    }
                }
            }
        }
        g.setTransform(at0);

        return clip;
    }


    @Override
    public Icon getListIcon() {
        return new ImageIcon(ContoursRenderer.class.getResource("/images/icons/contoursRenderer.png"));
    }

    @Override
    public String getListLabel() {
        return "" + ( getLegendLabel().length()> 0 ? getLegendLabel() +" " : "contours" );
    }

    @Override
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        
        super.incrementUpdateCount();

        QDataSet tds= getDataSet();
        if ( tds==null ) {
            return;
        }

        Units units = SemanticOps.getUnits( tds );

        double d0 = units.getFillDouble();
        
        if ( contoursDs==null ) {
            return;
        }

        if ( contoursDs.length()==0 ) {
            return;
        }
        
        QDataSet xds = (QDataSet) DataSetOps.unbundle(contoursDs, 0 );
        QDataSet yds = (QDataSet) DataSetOps.unbundle(contoursDs, 1 );
        QDataSet zds=  (QDataSet) DataSetOps.unbundle(contoursDs, 2 );
        QDataSet ids=  SemanticOps.xtagsDataSet(zds);

        Units xunits = xAxis.getUnits();
        Units yunits = yAxis.getUnits();

        ArrayList list = new ArrayList();
        ArrayList labels = new ArrayList();

        GeneralPath currentPath = null;

        int n0 = 0; // node counter.  Breaks are indicated by increment, so keep track of the last node.

        String form= getFormat();
        if (form.length()==0 ) form= "%.2f";
        
        Units zunits= SemanticOps.getUnits(zds);
        char c;
        try {
            c = DigitalRenderer.typeForFormat(form);
        } catch ( IllegalArgumentException ex ) {
            c = 'f';
        }
        
        for (int i = 0; i < zds.length(); i++) {
            double d = zds.value(i);
            int n = (int) ids.value(i);

            float fx = (float) xAxis.transform( xds.value(i), xunits );
            float fy = (float) yAxis.transform( yds.value(i), yunits );

            if (d != d0) {
                if ( currentPath!=null && simplifyPaths ) {
                    GeneralPath newPath= new GeneralPath();
                    GraphUtil.reducePath( currentPath.getPathIterator(null), newPath );
                    list.set( list.indexOf(currentPath), newPath );
                }
                
                currentPath = new GeneralPath();
                list.add(currentPath);
                labels.add( DigitalRenderer.formatDatum( form, zunits.createDatum(d), c ) );

                d0 = d;
                currentPath.moveTo(fx, fy);

            } else if (n != (n0 + 1)) {
                if ( currentPath!=null ) currentPath.moveTo(fx, fy);

            } else {
                if ( currentPath!=null ) currentPath.lineTo(fx, fy);
            }
            n0 = n;
        }

        paths = (GeneralPath[]) list.toArray(new GeneralPath[list.size()]);
        pathLabels = (String[]) labels.toArray(new String[labels.size()]);
    }
    
    /**
     * the contour locations, a comma-separated list
     */
    private String contours = "-.7,-.6,-.5,-.4,-.3,-.2,-.1,0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9";

    /**
     * return the contour locations, a comma-separated list
     * @return the contour locations, a comma-separated list
     */
    public String getContours() {
        return this.contours;
    }

    /**
     * set the contour locations, a comma-separated list
     * @param contours the contour locations, a comma-separated list
     */
    public void setContours(String contours) {
        String oldContours = this.contours;
        this.contours = contours;
        updateContours();
        update();
        propertyChangeSupport.firePropertyChange("contours", oldContours, contours);
    }
    
    /**
     * the inter-label distance, such as "100px" or "50em".
     */
    private String labelCadence = "100px";

    /**
     * return the inter-label distance, in ems.
     * @return get the inter-label distance, in ems.
     */
    public String getLabelCadence() {
        return this.labelCadence;
    }

    /**
     * set the inter-label distance, in ems.
     * @param labelCadence the inter-label distance, in ems.
     */
    public void setLabelCadence(String labelCadence) {
        String oldLabelCadence = this.labelCadence;
        this.labelCadence = labelCadence;
        update();
        propertyChangeSupport.firePropertyChange(CONTROL_KEY_LABEL_CADENCE, oldLabelCadence, labelCadence );
    }

    private synchronized GeneralPath[] getPaths() {
        return paths;
    }
            
    @Override
    public boolean acceptContext(int x, int y) {
        GeneralPath[] lpaths= getPaths();
        if (lpaths == null) {
            return false;
        }
        for (GeneralPath lpath : lpaths) {
            if (lpath != null) {
                if (lpath.intersects(x - 2, y - 2, 5, 5)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * true if labels should be drawn.
     */
    private boolean drawLabels;

    /**
     * true if labels should be drawn.
     * @return true if labels should be drawn.
     */
    public boolean isDrawLabels() {
        return this.drawLabels;
    }

    /**
     * true if labels should be drawn.
     * @param drawLabels true if labels should be drawn.
     */
    public void setDrawLabels(boolean drawLabels) {
        boolean oldDrawLabels = this.drawLabels;
        this.drawLabels = drawLabels;
        update();
        propertyChangeSupport.firePropertyChange("drawLabels", oldDrawLabels, drawLabels );
    }
    
    /**
     * the color for contour lines
     */
    private Color color = Color.BLACK;

    /**
     * Get the color for contour lines
     * @return the color for contour lines
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Set the color for contour lines
     * @param color the color for contour lines
     */
    public void setColor(Color color) {
        Color oldColor = this.color;
        this.color = color;
        update();
        propertyChangeSupport.firePropertyChange("color", oldColor, color);
    }

    /**
     * true if we should reduce paths to remove features that fall within a pixel, etc.
     */
    private boolean simplifyPaths = true;

    public static final String PROP_SIMPLIFYPATHS = "simplifyPaths";

    /**
     * return true if we should reduce paths to remove features that fall within a pixel, etc.
     * @return true if we should reduce paths
     */
    public boolean isSimplifyPaths() {
        return this.simplifyPaths;
    }

    /**
     * set to true if we should reduce paths to remove features that fall within a pixel, etc.
     * @param newsimplifyPaths true if we should reduce paths
     */
    public void setSimplifyPaths(boolean newsimplifyPaths) {
        boolean oldsimplifyPaths = simplifyPaths;
        this.simplifyPaths = newsimplifyPaths;
        update();
        propertyChangeSupport.firePropertyChange(PROP_SIMPLIFYPATHS, oldsimplifyPaths, newsimplifyPaths);
    }

    /**
     * the line thickness in pixels.
     */
    private double lineThick = 1.0;

    /**
     * handle for the property lineThick.
     */
    public static final String PROP_LINETHICK = "lineThick";

    /**
     * get the line thickness in pixels.
     * @return the line thickness in pixels.
     */
    public double getLineThick() {
        return this.lineThick;
    }

    /**
     * set the line thickness in pixels.
     * @param newlineThick the line thickness in pixels.
     */
    public void setLineThick(double newlineThick) {
        double oldlineThick = lineThick;
        this.lineThick = newlineThick;
        update();
        propertyChangeSupport.firePropertyChange(PROP_LINETHICK, oldlineThick, newlineThick);
    }

}
