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
import org.das2.dataset.AverageTableRebinner;
import org.das2.dataset.ClippedTableDataSet;
import org.das2.dataset.RebinDescriptor;
import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import org.virbo.math.Contour;
import java.awt.BasicStroke;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.Color;
import java.awt.Font;
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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.datum.Datum;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dsops.Ops;

/**
 * Renderer for making contour plots.  This still shows a bug 
 * https://sourceforge.net/p/autoplot/bugs/1105/
 * as of 20130913, and should not be used.
 * @author jbf
 */
public class ContoursRenderer extends Renderer {

    /** Creates a new instance of ContoursRenderer */
    public ContoursRenderer() {
    }
    GeneralPath[] paths;
    String[] pathLabels;

    /**
     * autorange on the data, returning a rank 2 bounds for the dataset.
     *
     * @param fillDs
     * @return
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
    
    /**
     * return false if the inputs are okay, true if there's no data, etc.
     * @param lparent
     * @return 
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

        if ( vds==null ) {
            return true;
        }
        
        if (paths == null) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public synchronized void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        DasPlot lparent= getParent();
        
        Graphics2D g = (Graphics2D) g1.create();

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

        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null) {
                g.draw(paths[i]);
            }
        }

    }

    @Override
    public void setControl(String s) {
        super.setControl(s);
        this.contours= getControl( "levels", contours );
        this.drawLabels= getBooleanControl( "labels", drawLabels );
        this.lineThick= getDoubleControl( PROP_LINETHICK, lineThick );
        this.labelCadence= getDoubleControl( "labelCadence", labelCadence );
        this.color= getColorControl( "color",  color );
        updateContours();
    }
    
    @Override
    public String getControl() {
        Map<String,String> controls= new LinkedHashMap();
        controls.put( "levels", contours );
        controls.put( "labels", setBooleanControl( drawLabels ) );
        controls.put( PROP_LINETHICK, String.valueOf(lineThick) );
        controls.put( "labelCadence", String.valueOf(labelCadence) );
        controls.put( CONTROL_KEY_COLOR, setColorControl( color ) );
        return Renderer.formatControl(controls);
    }

    @Override
    public void setDataSet(QDataSet ds) {
        super.setDataSet(ds); //To change body of generated methods, choose Tools | Templates.
        updateContours();
    }

    QDataSet vds; // the contours
    
    private synchronized void updateContours() {
        QDataSet tds= (QDataSet) getDataSet();
        if ( tds==null ) {
            vds= null;
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
        vds= Contour.contour(tds, DDataSet.wrap(dv.toDoubleArray(units) ) );
    }
    
    /**
     * returns clip, in the canvas reference frame
     */
    private Area paintLabels(final Graphics2D g) {

        Area clip = new Area();

        Font font0 = g.getFont();

        // do labels
        AffineTransform at0 = g.getTransform();
        
        Font font = font0.deriveFont(8f);

        g.setFont(font);

        double minLength= 20;
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] == null) {
                continue;
            }
            String label = pathLabels[i];
            GeneralPath p = paths[i];

            if (p != null) {

                PathIterator it1 = p.getPathIterator(null);
                PathIterator it2 = p.getPathIterator(null);

                while (!it1.isDone()) {

                    double len = GraphUtil.pointsAlongCurve(it1, null, null, null, true);

                    int nlabel = 1 + (int) Math.floor( len / this.labelCadence );

                    double phase = (len - ( nlabel-1 )  * labelCadence ) / 2;

                    if ( len < minLength ) {
                        //advance it2.
                        GraphUtil.pointsAlongCurve(it2, null, null, null, true);

                    } else {
                        double[] lens = new double[nlabel*2];
                        double labelWidth=10; // approx.
                        if ( labelWidth > labelCadence ) labelWidth= labelCadence * 0.99;
                        for (int ilabel = 0; ilabel < nlabel; ilabel++) {
                            lens[ilabel*2] = phase + labelCadence * ilabel;
                            lens[ilabel*2+1] = phase + labelCadence * ilabel + labelWidth;
                        }
                        Point2D.Double[] points = new Point2D.Double[nlabel*2];
                        double[] orient = new double[nlabel*2];

                        //advance it2.
                        GraphUtil.pointsAlongCurve(it2, lens, points, orient, true);

                        for (int ilabel = 0; ilabel < nlabel; ilabel++) {
                            AffineTransform at = new AffineTransform();
                            at.translate(points[ilabel*2].x, points[ilabel*2].y);
                            //double dx= points[ilabel*2+1].x - points[ilabel*2].x;
                            //double dy= points[ilabel*2+1].y - points[ilabel*2].y;
                            //double orient1= Math.atan2(dy,dx);
                            at.rotate(orient[ilabel*2]);
                            //at.rotate(orient1);
                            

                            Rectangle2D sbounds = g.getFontMetrics().getStringBounds(label, g);
                            double w = sbounds.getWidth();

                            GeneralPath rect = new GeneralPath(sbounds);
                            rect.transform(AffineTransform.getTranslateInstance(-w / 2, 0));
                            rect.transform(at);
                            clip.add(new Area(rect));

                            AffineTransform gat= new AffineTransform(at0);
                            gat.concatenate(at);
                            
                            g.setTransform( gat );
                            g.setColor(color);
                            g.drawString(label, (int) (-w / 2), 0);
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
        
        if ( vds==null ) {
            return;
        }

        QDataSet xds = (QDataSet) DataSetOps.unbundle( vds, 0 );
        QDataSet yds = (QDataSet) DataSetOps.unbundle( vds, 1 );
        QDataSet zds=  (QDataSet) DataSetOps.unbundle( vds, 2 );
        QDataSet ids=  SemanticOps.xtagsDataSet(zds);

        Units xunits = xAxis.getUnits();
        Units yunits = yAxis.getUnits();

        ArrayList list = new ArrayList();
        ArrayList labels = new ArrayList();

        GeneralPath currentPath = null;

        int n0 = 0; // node counter.  Breaks are indicated by increment, so keep track of the last node.

        NumberFormat nf = new DecimalFormat("0.00");

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
                labels.add(nf.format(d));

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
     * Holds value of property contours.
     */
    private String contours = "-.7,-.6,-.5,-.4,-.3,-.2,-.1,0,0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8,0.9";

    /**
     * Getter for property contours.
     * @return Value of property contours.
     */
    public String getContours() {
        return this.contours;
    }

    /**
     * Setter for property contours.
     * @param contours New value of property contours.
     */
    public void setContours(String contours) {
        String oldContours = this.contours;
        this.contours = contours;
        updateContours();
        update();
        propertyChangeSupport.firePropertyChange("contours", oldContours, contours);
    }
    /**
     * property labelCadence, the inter-label distance, in ems.
     */
    private double labelCadence = 100;

    /**
     * Getter for property labelCadence.
     * @return Value of property labelCadence.
     */
    public double getLabelCadence() {
        return this.labelCadence;
    }

    /**
     * Setter for property labelCadence.
     * @param labelCadence New value of property labelCadence.
     */
    public void setLabelCadence(double labelCadence) {
        double oldLabelCadence = this.labelCadence;
        this.labelCadence = labelCadence;
        update();
        propertyChangeSupport.firePropertyChange("labelCadence", new Double(oldLabelCadence), new Double(labelCadence));
    }

    @Override
    public boolean acceptContext(int x, int y) {
        if (paths == null) {
            return false;
        }
        for (int i = 0; i < paths.length; i++) {
            if (paths[i] != null) {
                if (paths[i].intersects(x - 2, y - 2, 5, 5)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Holds value of property drawLabels.
     */
    private boolean drawLabels;

    /**
     * Getter for property drawLabels.
     * @return Value of property drawLabels.
     */
    public boolean isDrawLabels() {
        return this.drawLabels;
    }

    /**
     * Setter for property drawLabels.
     * @param drawLabels New value of property drawLabels.
     */
    public void setDrawLabels(boolean drawLabels) {
        boolean oldDrawLabels = this.drawLabels;
        this.drawLabels = drawLabels;
        update();
        propertyChangeSupport.firePropertyChange("drawLabels", oldDrawLabels, drawLabels );
    }
    /**
     * Holds value of property color.
     */
    private Color color = Color.BLACK;

    /**
     * Getter for property color.
     * @return Value of property color.
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Setter for property color.
     * @param color New value of property color.
     */
    public void setColor(Color color) {
        Color oldColor = this.color;
        this.color = color;
        update();
        propertyChangeSupport.firePropertyChange("color", oldColor, color);
    }

    private boolean simplifyPaths = true;

    public static final String PROP_SIMPLIFYPATHS = "simplifyPaths";

    public boolean isSimplifyPaths() {
        return this.simplifyPaths;
    }

    public void setSimplifyPaths(boolean newsimplifyPaths) {
        boolean oldsimplifyPaths = simplifyPaths;
        this.simplifyPaths = newsimplifyPaths;
        update();
        propertyChangeSupport.firePropertyChange(PROP_SIMPLIFYPATHS, oldsimplifyPaths, newsimplifyPaths);
    }

    
    private double lineThick = 1.0;

    public static final String PROP_LINETHICK = "lineThick";

    public double getLineThick() {
        return this.lineThick;
    }

    public void setLineThick(double newlineThick) {
        double oldlineThick = lineThick;
        this.lineThick = newlineThick;
        update();
        propertyChangeSupport.firePropertyChange(PROP_LINETHICK, oldlineThick, newlineThick);
    }

    
}
