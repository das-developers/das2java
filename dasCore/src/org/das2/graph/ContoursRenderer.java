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
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.datum.Datum;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.DataSetUtil;

/**
 * Renderer for making contour plots
 * @author jbf
 */
public class ContoursRenderer extends Renderer {

    /** Creates a new instance of ContoursRenderer */
    public ContoursRenderer() {
    }
    GeneralPath[] paths;
    String[] pathLabels;

    public synchronized void render(Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        DasPlot lparent= getParent();
        
        Graphics2D g = (Graphics2D) g1.create();

        if (lparent.getCanvas().isAntiAlias()) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }

        QDataSet tds = (QDataSet) getDataSet();

        if (tds == null) {
            lparent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if (paths == null) {
            return;
        }
        g.setColor(color);
        g.setStroke( new BasicStroke((float)lineThick) );
                
        if (drawLabels) {
            Area labelClip = paintLabels(g);
            
            Shape rclip = g.getClip() == null ? new Rectangle(lparent.getX(), lparent.getY(), lparent.getWidth(), lparent.getHeight()) : g.getClip();
            Area clip = new Area(rclip);
            clip.subtract(labelClip);
            g.setClip(clip);
            //g.draw( labelClip );

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
        super.updatePlotImage(xAxis, yAxis, monitor);

        QDataSet tds = (QDataSet) getDataSet();

        if (tds == null) {
            return;
        }
        tds = new ClippedTableDataSet(tds, xAxis.getDatumRange(), yAxis.getDatumRange());
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

        final boolean rebin= false;
        if (rebin) {
            RebinDescriptor xRebinDescriptor;
            xRebinDescriptor = new RebinDescriptor(
                    xAxis.getDataMinimum(), xAxis.getDataMaximum(),
                    xAxis.getWidth() / 2,
                    xAxis.isLog());

            RebinDescriptor yRebinDescriptor = new RebinDescriptor(
                    yAxis.getDataMinimum(), yAxis.getDataMaximum(),
                    yAxis.getHeight() / 2,
                    yAxis.isLog());


            Datum xTagWidth = DataSetUtil.asDatum( org.virbo.dataset.DataSetUtil.guessCadenceNew( SemanticOps.xtagsDataSet(tds), null ) );
            Datum yTagWidth = DataSetUtil.asDatum( org.virbo.dataset.DataSetUtil.guessCadenceNew( SemanticOps.ytagsDataSet(tds), null ) );

            if ( xTagWidth.gt(xRebinDescriptor.binWidthDatum())) {
                xRebinDescriptor = null;
            }
            if ( yTagWidth.gt(yRebinDescriptor.binWidthDatum())) {
                yRebinDescriptor = null;
            }
            AverageTableRebinner rebinner = new AverageTableRebinner();
            rebinner.setInterpolate(false);

            if (xRebinDescriptor != null || yRebinDescriptor != null) {
                tds = rebinner.rebin(tds, xRebinDescriptor, yRebinDescriptor);
            }
        }

        QDataSet vds = Contour.contour(tds, DDataSet.wrap(dv.toDoubleArray(units) ) );
        //TODO: why do we contour each time???

        paths = new GeneralPath[dv.getLength()];

        double d0 = units.getFillDouble();
        int ii = -1;

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

        double slen = 0.; // path length

        float fx0 = 0f, fy0 = 0f; // for calculating path length

        NumberFormat nf = new DecimalFormat("0.00");

        for (int i = 0; i < zds.length(); i++) {
            double d = zds.value(i);
            int n = (int) ids.value(i);

            float fx = (float) xAxis.transform( xds.value(i), xunits );
            float fy = (float) yAxis.transform( yds.value(i), yunits );

            if (d != d0) {
                ii++;
                
                if ( currentPath!=null && simplifyPaths ) {
                    GeneralPath newPath= new GeneralPath();
                    int points=GraphUtil.reducePath( currentPath.getPathIterator(null), newPath );
                    list.set( list.indexOf(currentPath), newPath );
                }
                
                currentPath = new GeneralPath();
                list.add(currentPath);
                labels.add(nf.format(d));

                d0 = d;
                currentPath.moveTo(fx, fy);
                slen = 0.;
                fx0 = fx;
                fy0 = fy;
            } else if (n != (n0 + 1)) {
                currentPath.moveTo(fx, fy);
                fx0 = fx;
                fy0 = fy;
                slen = 0.;
            } else {
                currentPath.lineTo(fx, fy);
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
        propertyChangeSupport.firePropertyChange(PROP_LINETHICK, oldlineThick, newlineThick);
    }

    
}
