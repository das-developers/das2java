/* File: SeriesRenderer.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.das2.graph;

import java.util.logging.Level;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.DasProperties;
import org.virbo.dataset.DataSetUtil;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.event.DasMouseInputAdapter;
import org.das2.event.LengthDragRenderer;
import org.das2.event.MouseModule;
import org.das2.system.DasLogger;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.das2.dataset.VectorUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.UnitsUtil;
import org.das2.event.CrossHairMouseModule;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dsops.Ops;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * SeriesRender is a high-performance replacement for the SymbolLineRenderer.
 * The SymbolLineRenderer is limited to about 30,000 points, beyond which 
 * contracts for speed start breaking degrading usability.  The goal of the
 * SeriesRenderer is to plot 1,000,000 points without breaking the contracts.
 * 
 * @author  jbf
 */
public class SeriesRenderer extends Renderer {

    private DefaultPlotSymbol psym = DefaultPlotSymbol.CIRCLES;
    private double symSize = 3.0; // radius in pixels

    private double lineWidth = 1.0; // width in pixels

    private boolean histogram = false;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    private FillStyle fillStyle = FillStyle.STYLE_FILL;
    private int renderCount = 0;
    private int updateImageCount = 0;
    private Color color = Color.BLACK;
    private long lastUpdateMillis;
    private boolean antiAliased = "on".equals(DasProperties.getInstance().get("antiAlias"));
    private int firstIndex=-1;/* the index of the first point drawn, nonzero when X is monotonic and we can clip. */
    private int lastIndex=-1;/* the non-inclusive index of the last point drawn. */
    private int firstIndex_v=-1;/* the index of the first point drawn that is visible, nonzero when X is monotonic and we can clip. */
    private int lastIndex_v=-1;/* the non-inclusive index of the last point that is visible. */
    private int dslen=-1; // length of dataset, compare to firstIndex_v.

    boolean unitsWarning= false; // true indicates we've warned the user that we're ignoring units.
    boolean xunitsWarning= false;

    private static final Logger log = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    /**
     * indicates the dataset was clipped by dataSetSizeLimit 
     */
    private boolean dataSetClipped;

    public SeriesRenderer() {
        super();
        updatePsym();
    }
    Image psymImage;
    Image[] coloredPsyms;
    int cmx, cmy;
    FillRenderElement fillElement = new FillRenderElement();
    ErrorBarRenderElement errorElement = new ErrorBarRenderElement();
    PsymConnectorRenderElement psymConnectorElement = new PsymConnectorRenderElement();
    PsymConnectorRenderElement[] extraConnectorElements;
    PsymRenderElement psymsElement = new PsymRenderElement();
    Shape selectionArea;
    public static final String PROPERTY_X_DELTA_PLUS = "X_DELTA_PLUS";
    public static final String PROPERTY_X_DELTA_MINUS = "X_DELTA_MINUS";
    public static final String PROPERTY_Y_DELTA_PLUS = "Y_DELTA_PLUS";
    public static final String PROPERTY_Y_DELTA_MINUS = "Y_DELTA_MINUS";

    interface RenderElement {

        int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon);

        void update(DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon);

        boolean acceptContext(Point2D.Double dp);
    }

    private QDataSet ytagsDataSet( QDataSet ds ) {
        QDataSet vds;
        if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
            vds= SemanticOps.ytagsDataSet(ds);
        } else if ( ds.rank()==2 ) {
            parent.postMessage(this, "dataset is rank 2 and not a bundle", DasPlot.INFO, null, null);
            return null;
        } else {
            vds = (QDataSet) ds;
        }
        return vds;
    }
    /**
     * return the dataset for colors, or null if one is not identified.  This defines a scheme,
     * that the last column of a bundle is the color.
     * @param ds
     * @return
     */
    private QDataSet colorByDataSet( QDataSet ds ) {
        QDataSet colorByDataSet1=null;
        if ( this.colorByDataSetId.length()>0 ) {
            if ( colorByDataSetId.equals(QDataSet.PLANE_0) ) {
                colorByDataSet1= (QDataSet) ds.property(QDataSet.PLANE_0); // maybe they really meant PLANE_0.
                if ( colorByDataSet1==null && ds.rank()==2 ) {
                    colorByDataSet1= DataSetOps.unbundleDefaultDataSet( ds );
                }
            } else {
                if ( ds.rank()==2 ) {
                    colorByDataSet1= DataSetOps.unbundle( ds, colorByDataSetId );
                }
            }
        }
        return colorByDataSet1;
    }



    class PsymRenderElement implements RenderElement {

        int[] colors; // store the color index  of each psym

        double[] dpsymsPath; // store the location of the psyms here.

        int count; // the number of points to plot


        /**
         * render the psyms by stamping an image at the psym location.  The intent is to
         * provide fast rendering by reducing fidelity.
         * @return the number of points drawn, though possibly off screen.
         * On 20080206, this was measured to run at 320pts/millisecond for FillStyle.FILL
         * On 20080206, this was measured to run at 300pts/millisecond in FillStyle.OUTLINE
         */
        private int renderStamp(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {

            DasPlot lparent= parent;
            if ( lparent==null ) return 0;

            QDataSet colorByDataSet=null;
            if ( colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = colorByDataSet( ds );
            }

            if (colorByDataSet != null) {
                for (int i = 0; i < count; i++) {
                    int icolor = colors[i];
                    g.drawImage(coloredPsyms[icolor], (int)dpsymsPath[i * 2] - cmx, (int)dpsymsPath[i * 2 + 1] - cmy, lparent);
                }
            } else {
                try {
                    for (int i = 0; i < count; i++) {
                        g.drawImage(psymImage, (int)dpsymsPath[i * 2] - cmx, (int)dpsymsPath[i * 2 + 1] - cmy, lparent);
                    }
                } catch ( ArrayIndexOutOfBoundsException ex ) {
                    ex.printStackTrace();
                }
            }

            return count;

        }

        /**
         * Render the psyms individually.  This is the highest fidelity rendering, and
         * should be used in printing.
         * @return the number of points drawn, though possibly off screen.
         * On 20080206, this was measured to run at 45pts/millisecond in FillStyle.FILL
         * On 20080206, this was measured to run at 9pts/millisecond in FillStyle.OUTLINE
         */
        private int renderDraw(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            float fsymSize = (float) symSize;

            QDataSet colorByDataSet=null;
            if ( colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = colorByDataSet(ds);
                if ( colorByDataSet!=null ) {
                    if ( colorByDataSet.length()!=dataSet.length()) {
                        throw new IllegalArgumentException("colorByDataSet and dataSet do not have same length");
                    }
                } else {
                    System.err.println("why is colorByDataSetId set?");
                }
            }

            graphics.setStroke(new BasicStroke((float) lineWidth));

            Color[] ccolors = null;
            if (colorByDataSet != null) {
                IndexColorModel icm = colorBar.getIndexColorModel();
                ccolors = new Color[icm.getMapSize()];
                for (int j = 0; j < icm.getMapSize(); j++) {
                    ccolors[j] = new Color(icm.getRGB(j));
                }
            }

            if (colorByDataSet != null) {
                for (int i = 0; i < count; i++) {
                    graphics.setColor(ccolors[colors[i]]);
//                    psym.draw(graphics, ipsymsPath[i * 2], ipsymsPath[i * 2 + 1], fsymSize, fillStyle);
                    psym.draw(graphics, dpsymsPath[i * 2], dpsymsPath[i * 2 + 1], fsymSize, fillStyle);
                }

            } else {
                for (int i = 0; i < count; i++) {
//                    psym.draw(graphics, ipsymsPath[i * 2], ipsymsPath[i * 2 + 1], fsymSize, fillStyle);
                    try {
                        psym.draw(graphics, dpsymsPath[i * 2], dpsymsPath[i * 2 + 1], fsymSize, fillStyle);
                    } catch ( ArrayIndexOutOfBoundsException ex ) {
                        ex.printStackTrace();
                    }
                }
            }

            return count;

        }

        /**
         *
         * @param graphics
         * @param xAxis
         * @param yAxis
         * @param vds
         * @param mon
         * @return the number of points drawn, though possibly off screen.
         */
        public synchronized int render(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            int i;
            if ( vds.rank()!=1 && !SemanticOps.isBundle(vds) ) {
                renderException( graphics, xAxis, yAxis, new IllegalArgumentException("dataset is not rank 1"));
            }
            DasPlot lparent= parent;
            if ( lparent==null ) return 0;
            if (stampPsyms && !lparent.getCanvas().isPrintingThread()) {
                i = renderStamp(graphics, xAxis, yAxis, vds, mon);
            } else {
                i = renderDraw(graphics, xAxis, yAxis, vds, mon);
            }
            return i;
        }

        public synchronized void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            QDataSet colorByDataSet1 = colorByDataSet(dataSet);

            Units cunits = null;
            if (colorByDataSet1 != null) {
                cunits = SemanticOps.getUnits( colorByDataSet1 );
            }

            double x, y;            
            double dx,dy;

            count= 0; // intermediate state
            dpsymsPath = new double[(lastIndex - firstIndex ) * 2];
            colors = new int[lastIndex - firstIndex + 2];

            int index = firstIndex;

            QDataSet vds = null;

            QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
            vds= ytagsDataSet(dataSet);

            if ( xds.rank()==2 && xds.property( QDataSet.BINS_1 )!=null ) {
                xds= Ops.reduceMean(xds,1);
            }
            
            Units xUnits= SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            if ( unitsWarning ) yUnits= yAxis.getUnits();
            if ( xunitsWarning ) xUnits= xAxis.getUnits();

            if ( index<lastIndex ) {
                x = xds.value(index);
                y = vds.value(index);
                dx= xAxis.transform(x, xUnits);
                dy= yAxis.transform(y, yUnits);
            }
            
            double dx0=-99, dy0=-99; //last point.

            QDataSet wds= SemanticOps.weightsDataSet(vds);

            int buffer= (int)Math.ceil( Math.max( 20, getSymSize() ) );
            Rectangle window= DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() );
            window= new Rectangle( window.x-buffer, window.y-buffer, window.width+2*buffer, window.height+2*buffer );
            DasPlot lparent= parent;
            if ( lparent==null ) return;
            if ( lparent.isOverSize() ) {
                window= new Rectangle( window.x- window.width/3, window.y-buffer, 5 * window.width / 3, window.height + 2 * buffer );
                //TODO: there's a rectangle somewhere that is the preveiw.  Use this instead of assuming 1/3 on either side.
            } else {
                window= new Rectangle( window.x- buffer, window.y-buffer, window.width + 2*buffer, window.height + 2 * buffer );
            }

            int i = 0;
            for (; index < lastIndex; index++) {
                x = xds.value(index);
                y = vds.value(index);

                final boolean isValid = wds.value(index)>0 && xUnits.isValid(x);

                dx = xAxis.transform(x, xUnits);
                dy = yAxis.transform(y, yUnits);

                if (isValid && window.contains(dx,dy) ) {
                    if ( simplifyPaths ) {
                        if ( dx==dx0 && dy==dy0 ) continue;
                    }
                    dpsymsPath[i * 2] = dx;
                    dpsymsPath[i * 2 + 1] = dy;
                    if (colorByDataSet1 != null) {
                        colors[i] = colorBar.indexColorTransform( colorByDataSet1.value(index), cunits);
                    }
                    i++;
                }
                dx0= dx;
                dy0= dy;

            }

            count = i;

        }

        public boolean acceptContext(Point2D.Double dp) {
            if (dpsymsPath == null) {
                return false;
            }
            double rad = Math.max(symSize, 5);

            for (int index = firstIndex; index < lastIndex; index++) {
                int i = index - firstIndex;
                if (dp.distance(dpsymsPath[i * 2], dpsymsPath[i * 2 + 1]) < rad) {
                    return true;
                }
            }
            return false;
        }
    }

    class ErrorBarRenderElement implements RenderElement {

        GeneralPath p;

        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            if (p == null) {
                return 0;
            }
            g.draw(p);
            return lastIndex - firstIndex;
        }

        public synchronized void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            QDataSet vds= ytagsDataSet(dataSet);

            QDataSet deltaPlusY = (QDataSet) vds.property( QDataSet.DELTA_PLUS );
            QDataSet deltaMinusY = (QDataSet) vds.property( QDataSet.DELTA_MINUS );

            p = null;

            if (deltaPlusY == null) {
                return;
            }
            if (deltaMinusY == null) {
                return;
            }

            QDataSet xds= SemanticOps.xtagsDataSet(dataSet);

            Units xunits = SemanticOps.getUnits(xds);
            Units yunits = SemanticOps.getUnits(vds);
            
            if ( unitsWarning ) yunits= yAxis.getUnits();
            if ( xunitsWarning ) xunits= xAxis.getUnits();

            Units yoffsetUnits = yunits.getOffsetUnits();

            p = new GeneralPath();
            for (int i = firstIndex; i < lastIndex; i++) {
                float ix = (float) xAxis.transform( xds.value(i), xunits );
                double dp= deltaPlusY.value(i);
                double dm= deltaMinusY.value(i);
                if ( yoffsetUnits.isValid(dp) && yoffsetUnits.isValid(dm) ) {
                    float iym = (float) yAxis.transform( vds.value(i) - dm, yunits );
                    float iyp = (float) yAxis.transform( vds.value(i) + dp, yunits );
                    p.moveTo(ix, iym);
                    p.lineTo(ix, iyp);
                }
            }

        }

        public boolean acceptContext(Point2D.Double dp) {
            return p != null && p.contains(dp.x - 2, dp.y - 2, 5, 5);

        }
    }

    /**
     * isolate allow bad units logic.
     * @param d the datum.
     * @param u target units.
     * @return the double value.
     */
    private double doubleValue( Datum d, Units u ) {
        if ( d.getUnits().isConvertableTo(u) ) {
            return d.doubleValue(u);
        } else {
            try {
                return d.value();
            } catch ( IllegalArgumentException ex ) {
                throw new InconvertibleUnitsException(d.getUnits(),u);
            }
        }
    }

    class PsymConnectorRenderElement implements RenderElement {

        private GeneralPath path1;
        private Color color;  // override default color

        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            if ( vds.rank()!=1 && !SemanticOps.isBundle(vds) ) {
                renderException( g, xAxis, yAxis, new IllegalArgumentException("dataset is not rank 1"));
            }
            if (path1 == null) {
                return 0;
            }
            if (color != null) {
                g.setColor(color);
            }
            psymConnector.draw(g, path1, (float) lineWidth);
            return 0;
        }

        public synchronized void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            QDataSet xds= SemanticOps.xtagsDataSet( dataSet );
            if ( xds.rank()==2 && xds.property( QDataSet.BINS_1 )!=null ) {
                xds= Ops.reduceMean(xds,1);
            }

            QDataSet vds= ytagsDataSet( dataSet );

            if ( vds.rank()>1 ) {
                return;
            }

            QDataSet wds= SemanticOps.weightsDataSet( vds );

            Units xUnits = SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            if ( unitsWarning ) yUnits= yAxis.getUnits();
            if ( xunitsWarning ) xUnits= yAxis.getUnits();

            Rectangle window= DasDevicePosition.toRectangle( yAxis.getRow(), xAxis.getColumn() );
            int buffer= (int)Math.ceil( Math.max( getLineWidth(),10 ) );

            DasPlot lparent= parent;
            if ( lparent==null ) return;
            if ( lparent.isOverSize() ) {
                window= new Rectangle( window.x- window.width/3, window.y-buffer, 5 * window.width / 3, window.height + 2 * buffer );
                //TODO: there's a rectangle somewhere that is the preveiw.  Use this instead of assuming 1/3 on either side.
            } else {
                window= new Rectangle( window.x- buffer, window.y-buffer, window.width + 2*buffer, window.height + 2 * buffer );
            }

            if ( lastIndex-firstIndex==0 ) {
                this.path1= null;
                return;
            }

            int pathLengthApprox= Math.max( 5, 110 * (lastIndex - firstIndex) / 100 );
            GeneralPath newPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );

            Datum sw = SemanticOps.guessXTagWidth(xds,vds);
            double xSampleWidth;
            boolean logStep;
            if ( sw!=null) {
                if ( UnitsUtil.isRatiometric(sw.getUnits()) ) {
                    xSampleWidth = sw.doubleValue(Units.logERatio);
                    logStep= true;
                } else {
                    xSampleWidth = doubleValue( sw, xUnits.getOffsetUnits() );
                    logStep= false;
                }
            } else {
                xSampleWidth= 1e37; // something really big
                logStep= false;
            }


            /* fuzz the xSampleWidth */
            double xSampleWidthExact= xSampleWidth;
            xSampleWidth = xSampleWidth * 1.20;

            double x = Double.NaN;
            double y = Double.NaN;

            double x0 = Double.NaN; /* the last plottable point */
            double y0 = Double.NaN; /* the last plottable point */

            float fx = Float.NaN;
            float fy = Float.NaN;
            float fx0 = Float.NaN;
            float fy0 = Float.NaN;
            boolean visible;  // true if this point can be seen
            boolean visible0; // true if the last point can be seen
            
            int index;

            index = firstIndex;
            x = (double) xds.value(index);
            y = (double) vds.value(index);

            // first point //
            logger.log(Level.FINE, "firstPoint moveTo,LineTo= {0},{1}", new Object[]{x, y});
            try {
                fx = (float) xAxis.transform(x, xUnits);
            } catch ( InconvertibleUnitsException ex ) {
                xunitsWarning= true;
                return;
            }
            try {
                fy = (float) yAxis.transform(y, yUnits);
            } catch ( InconvertibleUnitsException ex ) {
                unitsWarning= true;
                return;
            }

            visible0= window.contains(fx,fy);
            visible= visible0;
            if (histogram) {
                float fx1 = midPoint( xAxis, x, xUnits, xSampleWidthExact, logStep, -0.5 );
                newPath.moveTo(fx1, fy);
                newPath.lineTo(fx, fy);
            } else {
                newPath.moveTo(fx, fy);
                newPath.lineTo(fx, fy);
            }

            x0 = x;
            y0 = y;
            fx0 = fx;
            fy0 = fy;

            index++;

            QDataSet ydc= ArrayDataSet.copy(vds);

            // now loop through all of them. //
            boolean ignoreCadence= ! cadenceCheck;
            boolean isValid= false;

            for (; index < lastIndex; index++) {

                x = xds.value(index);
                y = vds.value(index);

                isValid = wds.value( index )>0 && xUnits.isValid(x);

                fx = (float) xAxis.transform(x, xUnits);
                fy = (float) yAxis.transform(y, yUnits);
                visible= isValid && window.intersectsLine( fx0,fy0, fx,fy );

                if (isValid) {
                    double step= logStep ? Math.log(x/x0) : x-x0;
                    if ( ignoreCadence || step < xSampleWidth) {
                        // draw connect-a-dot between last valid and here
                        if (histogram) {
                            float fx1 = (fx0 + fx) / 2;
                            newPath.lineTo(fx1, fy0);
                            newPath.lineTo(fx1, fy);
                            newPath.lineTo(fx, fy);
                        } else {
                            if ( visible ) {
                                if ( !visible0 ) {
                                    newPath.moveTo(fx0,fy0);
                                }
                                newPath.lineTo(fx, fy); // this is the typical path
                            }

                        }

                    } else {
                        // introduce break in line
                        if (histogram) {
                            float fx1 = (float) xAxis.transform(x0 + xSampleWidthExact / 2, xUnits);
                            newPath.lineTo(fx1, fy0);
                            // there's a bug here if you pan around the dataset shown in SeriesBreakHist.java
                            fx1 = (float) xAxis.transform(x - xSampleWidthExact / 2, xUnits);
                            newPath.moveTo(fx1, fy);
                            newPath.lineTo(fx, fy);

                        } else {
                            if ( visible ) {
                                newPath.moveTo(fx, fy);
                                newPath.lineTo(fx, fy);
                            }
                        }

                    } // else introduce break in line

                    x0 = x;
                    y0 = y;
                    fx0 = fx;
                    fy0 = fy;
                    visible0 = visible;

                } else {
                    if (visible0) {
                        if ( histogram ) {
                            float fx1 = midPoint( xAxis, x0, xUnits, xSampleWidthExact, logStep, 0.5 );
                            newPath.lineTo(fx1, fy0);
                        } else {
                            newPath.moveTo(fx0, fy0); // place holder
                        }
                    }

                }

            } // for ( ; index < ixmax && lastIndex; index++ )

            if ( histogram ) {
                if ( isValid ) {
                    fx = (float) xAxis.transform( x + xSampleWidthExact / 2, xUnits );
                    newPath.lineTo(fx, fy0);
                }
            }

            if (!histogram && simplifyPaths && colorByDataSetId.length()==0 ) {
                //j   System.err.println( "input: " );
                //j   System.err.println( GraphUtil.describe( newPath, true) );
                this.path1= new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );
                int count = GraphUtil.reducePath(newPath.getPathIterator(null), path1 );
                logger.fine( String.format("reduce path in=%d  out=%d\n", lastIndex-firstIndex, count) );
            } else {
                this.path1 = newPath;
            }

        }

        public boolean acceptContext(Point2D.Double dp) {
            if ( path1==null ) return false;
            Rectangle2D hitbox = new Rectangle2D.Double(dp.x-5, dp.y-5, 10f, 10f);
            double[] coords = new double[6];
            PathIterator it = path1.getPathIterator(null);
            it.currentSegment(coords);

            double x1 = coords[0];
            double y1 = coords[1];
            it.next();

            while (!it.isDone()) {
                int segType = it.currentSegment(coords);
                // don't test against SEG_MOVETO; we shouldn't have any others
                if (segType == PathIterator.SEG_LINETO) {
                    if(hitbox.intersectsLine(x1, y1, coords[0], coords[1])) return true;
                }
                x1 = coords[0];
                y1 = coords[1];
                it.next();
            }
            return false;
        }
    }

    private float midPoint(DasAxis axis, double d1, Units units, double delta, boolean ratiometric, double alpha ) {
        float fx1;
        if (axis.isLog() && ratiometric ) {
            fx1 = (float) axis.transform( Math.exp( Math.log(d1) + delta * alpha ), units);
        } else {
            fx1 = (float) axis.transform( d1 + delta * alpha, units);
        }
        return fx1;
    }

    class FillRenderElement implements RenderElement {

        private GeneralPath fillToRefPath1;

        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, QDataSet vds, ProgressMonitor mon) {
            if ( fillToRefPath1==null ) {
                return 0;
            }
            g.setColor(fillColor);
            g.fill(fillToRefPath1);
            return 0;
        }

        public void update(DasAxis xAxis, DasAxis yAxis, QDataSet dataSet, ProgressMonitor mon) {

            if ( lastIndex-firstIndex==0 ) {
                this.fillToRefPath1= null;
                return;
            }

            QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
            QDataSet vds;
            vds = ytagsDataSet( ds );

            Units xUnits = SemanticOps.getUnits(xds);
            Units yUnits = SemanticOps.getUnits(vds);
            if ( unitsWarning ) yUnits= yAxis.getUnits();
            if ( xunitsWarning ) xUnits= xAxis.getUnits();

            QDataSet wds= SemanticOps.weightsDataSet( vds );

            int pathLengthApprox= Math.max( 5, 110 * (lastIndex - firstIndex) / 100 );
            GeneralPath fillPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );

            Datum sw = SemanticOps.guessXTagWidth(xds,dataSet);
            double xSampleWidth;
            boolean logStep;
            if ( sw!=null ) {
                if ( UnitsUtil.isRatiometric(sw.getUnits()) ) {
                    xSampleWidth = sw.doubleValue(Units.logERatio);
                    logStep= true;
                } else {
                    xSampleWidth = doubleValue( sw, xUnits.getOffsetUnits() );
                    logStep= false;
                }
            } else {
                xSampleWidth= 1e37;
                logStep= false;
            }


            /* fuzz the xSampleWidth */
            double xSampleWidthExact= xSampleWidth;
            xSampleWidth = xSampleWidth * 1.20;

            if (reference == null) {
                reference = yUnits.createDatum(yAxis.isLog() ? 1.0 : 0.0);
            }

            double yref = doubleValue( reference, yUnits );

            double x = Double.NaN;
            double y = Double.NaN;

            double x0 = Double.NaN; /* the last plottable point */
            double y0 = Double.NaN; /* the last plottable point */

            float fyref = (float) yAxis.transform(yref, yUnits);
            float fx = Float.NaN;
            float fy = Float.NaN;
            float fx0 = Float.NaN;
            float fy0 = Float.NaN;

            int index;

            index = firstIndex;
            x = (double) xds.value(index);
            y = (double) vds.value(index);

            // first point //
            fx = (float) xAxis.transform(x, xUnits);
            fy = (float) yAxis.transform(y, yUnits);
            if (histogram) {
                float fx1;
                fx1= midPoint( xAxis, x, xUnits, xSampleWidthExact, logStep, -0.5 );
                fillPath.moveTo(fx1-1, fyref); // doesn't line up, -1 is fudge
                fillPath.lineTo(fx1-1, fy);
                fillPath.lineTo(fx, fy);

            } else {
                fillPath.moveTo(fx, fyref);
                fillPath.lineTo(fx, fy);

            }

            x0 = x;
            y0 = y;
            fx0 = fx;
            fy0 = fy;

            if (psymConnector != PsymConnector.NONE || fillToReference) {
                // now loop through all of them. //
                boolean ignoreCadence= ! cadenceCheck;
                for (; index < lastIndex; index++) {

                    x = xds.value(index);
                    y = vds.value(index);

                    final boolean isValid = wds.value( index )>0 && xUnits.isValid(x);

                    fx = (float) xAxis.transform(x, xUnits);
                    fy = (float) yAxis.transform(y, yUnits);

                    if (isValid) {
                        double step= logStep ? Math.log(x/x0) : x-x0;
                        if ( ignoreCadence || step < xSampleWidth) {
                            // draw connect-a-dot between last valid and here
                            if (histogram) {
                                float fx1 = (fx0 + fx) / 2; //sloppy with ratiometric spacing
                                fillPath.lineTo(fx1, fy0);
                                fillPath.lineTo(fx1, fy);
                                fillPath.lineTo(fx, fy);
                            } else {
                                fillPath.lineTo(fx, fy);
                            }

                        } else {
                            // introduce break in line
                            if (histogram) {
                                float fx1 = midPoint( xAxis, x0, xUnits, xSampleWidthExact, logStep, 0.5 );
                                fillPath.lineTo(fx1, fy0);
                                fillPath.lineTo(fx1, fyref);
                                fx1 = midPoint( xAxis, x, xUnits, xSampleWidthExact, logStep, -0.5 );
                                fillPath.moveTo(fx1, fyref);
                                fillPath.lineTo(fx1, fy);
                                fillPath.lineTo(fx, fy);

                            } else {
                                fillPath.lineTo(fx0, fyref);
                                fillPath.moveTo(fx, fyref);
                                fillPath.lineTo(fx, fy);
                            }

                        } // else introduce break in line

                        x0 = x;
                        y0 = y;
                        fx0 = fx;
                        fy0 = fy;

                    }

                } // for ( ; index < ixmax && lastIndex; index++ )

            }

            if ( histogram ) {
                float fx1 = midPoint( xAxis, x0, xUnits, xSampleWidthExact, logStep, 0.5 );
                fillPath.lineTo(fx1, fy0);
                fillPath.lineTo(fx1, fyref);
            } else {
                fillPath.lineTo(fx0, fyref);
            }

            this.fillToRefPath1 = fillPath;

            if (simplifyPaths) {
                GeneralPath newPath= new GeneralPath(GeneralPath.WIND_NON_ZERO, pathLengthApprox );
                int count= GraphUtil.reducePath(fillToRefPath1.getPathIterator(null), newPath );
                fillToRefPath1= newPath;
                logger.fine( String.format("reduce path(fill) in=%d  out=%d\n", lastIndex-firstIndex, count ) );
            }

        }

        public boolean acceptContext(Point2D.Double dp) {
            return fillToRefPath1 != null && fillToRefPath1.contains(dp);
        }
    }

    /**
     * updates the image of a psym that is stamped
     */
    private void updatePsym() {
        if ( !isActive() ) return;
        
        int sx = 6+(int) Math.ceil(symSize + 2 * lineWidth);
        int sy = 6+(int) Math.ceil(symSize + 2 * lineWidth);
        double dcmx, dcmy;
        dcmx = (lineWidth + (int) Math.ceil( ( symSize ) / 2)) +2 ;
        dcmy = (lineWidth + (int) Math.ceil( ( symSize ) / 2)) +2 ;
        BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        Object rendering = antiAliased ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
        g.setColor(color);

        DasPlot lparent= parent;
        if ( lparent==null ) return;

        g.setBackground(lparent.getBackground());
        
        g.setStroke(new BasicStroke((float) lineWidth));

        psym.draw(g, dcmx, dcmy, (float) symSize, fillStyle);
        psymImage = image;

        if (  colorByDataSetId != null && !colorByDataSetId.equals("") ) {
            initColoredPsyms(sx, sy, image, g, lparent, rendering, dcmx, dcmy);
        }

        cmx = (int) dcmx;
        cmy = (int) dcmy;

        update();
    }

    private void initColoredPsyms(int sx, int sy, BufferedImage image, Graphics2D g, DasPlot lparent, Object rendering, double dcmx, double dcmy) {
        IndexColorModel model = colorBar.getIndexColorModel();
        coloredPsyms = new Image[model.getMapSize()];
        for (int i = 0; i < model.getMapSize(); i++) {
            Color c = new Color(model.getRGB(i));
            image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
            g = (Graphics2D) image.getGraphics();
            g.setBackground(lparent.getBackground());
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
            g.setColor(c);
            g.setStroke(new BasicStroke((float) lineWidth));
            psym.draw(g, dcmx, dcmy, (float) symSize, this.fillStyle);
            coloredPsyms[i] = image;
        }
    }

    private void reportCount() {
        //if ( renderCount % 100 ==0 ) {
        //System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
        //new Throwable("").printStackTrace();
        //}
    }

    /**
     * updates firstIndex and lastIndex that point to the part of
     * the data that is plottable.  The plottable part is the part that
     * might be visible while limiting the number of plotted points.
     */
    private synchronized void updateFirstLast(DasAxis xAxis, DasAxis yAxis, QDataSet xds, QDataSet dataSet ) {
        
        int ixmax;
        int ixmin;

        QDataSet yds= dataSet;
        if ( yds.rank()==2 ) {
            yds= DataSetOps.slice1( yds, 0 );
        }

        if ( xds.rank()==2 ) {
            xds= DataSetOps.slice1(xds,0); //BINS dataset
        }

        QDataSet wxds= SemanticOps.weightsDataSet( xds );

        QDataSet wds;
        wds= SemanticOps.weightsDataSet( yds );

        DasPlot lparent= parent;
        if ( lparent==null ) return;

        dslen= xds.length();
        Boolean xMono = SemanticOps.isMonotonic( xds );
        if (xMono != null && xMono.booleanValue()) {
            DatumRange visibleRange = xAxis.getDatumRange();
            Units xdsu= SemanticOps.getUnits(xds);
            if ( !visibleRange.getUnits().isConvertableTo( xdsu ) ) {
                visibleRange= new DatumRange( visibleRange.min().value(), visibleRange.max().value(), xdsu );
            }
            firstIndex_v = DataSetUtil.getPreviousIndex( xds, visibleRange.min());
            lastIndex_v = DataSetUtil.getNextIndex( xds, visibleRange.max()) + 1; // +1 is for exclusive.
            if (lparent.isOverSize()) {
                Rectangle plotBounds = lparent.getUpdateImageBounds();
                if ( plotBounds!=null ) {
                    visibleRange = new DatumRange(xAxis.invTransform(plotBounds.x), xAxis.invTransform(plotBounds.x + plotBounds.width));
                }
                ixmin = DataSetUtil.getPreviousIndex( xds, visibleRange.min());
                ixmax = DataSetUtil.getNextIndex( xds, visibleRange.max()) + 1; // +1 is for exclusive.

            } else {
                ixmin = firstIndex_v;
                ixmax = lastIndex_v;
            }

        } else {

            ixmin = 0;
            ixmax = xds.length();
            firstIndex_v= ixmin;
            lastIndex_v= ixmax;
        }

        double x = Double.NaN;
        double y = Double.NaN;

        int index;

        // find the first valid point, set x0, y0 //
        for (index = ixmin; index < ixmax; index++) {
            x = (double) xds.value(index);
            y = (double) yds.value(index);

            final boolean isValid = wds.value(index)>0 && wxds.value(index)>0 ;
            if (isValid) {
                firstIndex = index;  // TODO: what if no valid points?

                index++;
                break;
            }
        }

        if ( firstIndex==-1 ) { // no valid points
            lastIndex= ixmax;
            firstIndex= ixmax;
        }

        // find the last valid point, minding the dataSetSizeLimit
        int pointsPlotted = 0;
        for (index = firstIndex; index < ixmax && pointsPlotted < dataSetSizeLimit; index++) {
            y = yds.value(index);

            final boolean isValid = wds.value(index)>0 && wxds.value(index)>0;

            if (isValid) {
                pointsPlotted++;
            }
        }

        if (index < ixmax && pointsPlotted == dataSetSizeLimit) {
            dataSetClipped = true;
        }

        lastIndex = index;

        logger.log( Level.FINE, "ds: {0},  firstIndex={1} to lastIndex={2}", new Object[]{ String.valueOf(this.ds), this.firstIndex, this.lastIndex});
    }

    @Override
    public void setActive( boolean active ) {
        super.setActive(active);
        if ( active ) updatePsym();
    }

    public synchronized void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        DasPlot lparent= this.parent;

        logger.log(Level.FINE, "enter {0}.render: {1}", new Object[]{id, String.valueOf(getDataSet()) });
        logger.log( Level.FINE, "ds: {0},  drawing indeces {1} to {2}", new Object[]{ String.valueOf(this.ds), this.firstIndex, this.lastIndex});
        
        if ( lparent==null ) return;
        if ( this.ds == null && lastException != null) {
            lparent.postException(this, lastException);
            return;
        }

        renderCount++;
        reportCount();

        long timer0 = System.currentTimeMillis();

        QDataSet dataSet = getDataSet();

        if (dataSet == null) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            lparent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if (dataSet.rank() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("rank 0 data set");
            lparent.postMessage(this, "rank 0 data set: "+dataSet.toString(), DasPlot.INFO, null, null);
            return;
        }

        if (dataSet.length() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("empty data set");
            lparent.postMessage(this, "empty data set", DasPlot.INFO, null, null);
            return;
        }

        if ( psym== DefaultPlotSymbol.NONE && psymConnector==PsymConnector.NONE ) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("plot symbol and symbol connector are set to none");
            lparent.postMessage(this, "plot symbol and symbol connector are set to none", DasPlot.INFO, null, null);
            return;
        }

        QDataSet tds = null;
        QDataSet vds = null;
        boolean yaxisUnitsOkay = false;
        boolean xaxisUnitsOkay= false;

        QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
        xaxisUnitsOkay = SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits() );
        if ( !SemanticOps.isTableDataSet(dataSet) ) {
            vds= ytagsDataSet(ds);
            yaxisUnitsOkay = SemanticOps.getUnits(vds).isConvertableTo(yAxis.getUnits()); // Ha!  QDataSet makes the code the same

        } else {
            tds = (QDataSet) dataSet;
            yaxisUnitsOkay = SemanticOps.getUnits(tds).isConvertableTo(yAxis.getUnits());
        }

        boolean haveReportedUnitProblem= false;
        if ( !xaxisUnitsOkay && !yaxisUnitsOkay && SemanticOps.getUnits(xds)==SemanticOps.getUnits(vds) && xAxis.getUnits()==yAxis.getUnits() ) {
             if ( unitsWarning ) {
                //UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() )
                lparent.postMessage( this, "axis units changed from \""+SemanticOps.getUnits(vds) + "\" to \"" + yAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                haveReportedUnitProblem= true;
            } else {
                lparent.postMessage( this, "inconvertible axis units", DasPlot.INFO, null, null );
                return;
            }
        }

        if ( !haveReportedUnitProblem && !yaxisUnitsOkay ) {
            if ( unitsWarning ) {
                //UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() )
                lparent.postMessage( this, "yaxis units changed from \""+SemanticOps.getUnits(vds) + "\" to \"" + yAxis.getUnits() + "\"", DasPlot.INFO, null, null );
            } else {
                lparent.postMessage( this, "inconvertible yaxis units", DasPlot.INFO, null, null );
                return;
            }
        }

        if ( !haveReportedUnitProblem && !xaxisUnitsOkay ) {
            if ( xunitsWarning ) {
                //UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() )
                lparent.postMessage( this, "xaxis units changed from \""+SemanticOps.getUnits(xds) + "\" to \"" + xAxis.getUnits() + "\"", DasPlot.INFO, null, null );
            } else {
                lparent.postMessage( this, "inconvertible xaxis units", DasPlot.INFO, null, null );
                return;
            }
        }

        int messageCount= 0;

        logger.log(Level.FINE, "rendering points: {0}  {1}", new Object[]{lastIndex, firstIndex});
        if ( lastIndex == -1 ) {
            if ( messageCount++==0) {
                lparent.postMessage(SeriesRenderer.this, "need to update first/last", DasPlot.INFO, null, null);
            }
            update(); //DANGER: this kludge is not well tested, and may cause problems.  It should be the case that another
                      // update is posted that will resolve this problem, but somehow it's not happening when Autoplot adds a
                      // bunch of panels.
            //System.err.println("need to update first/last bit");
            javax.swing.Timer t= new javax.swing.Timer( 200, new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    update();
                }
            } );
            t.setRepeats(false);
            t.restart();
        }

        if (lastIndex == firstIndex) {
            if ( firstValidIndex==lastValidIndex ) {
                if ( messageCount++==0) lparent.postMessage(SeriesRenderer.this, "dataset contains no valid data", DasPlot.INFO, null, null);
            }
        }

        logger.log(Level.FINE, "render data set {0}", dataSet);

        Graphics2D graphics = (Graphics2D) g.create();

        if (antiAliased) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }

        if (tds != null) {
            if (extraConnectorElements == null) {
                return;
            }
            if ( tds.length(0) != extraConnectorElements.length) {
                return;
            } else {
                QDataSet yds= ytagsDataSet(tds);
                Units yunits= SemanticOps.getUnits(yds);
                int maxWidth = 0;
                for (int j = 0; j < tds.length(0); j++) {
                    String label = String.valueOf( yunits.createDatum(yds.value(j)) ).trim();
                    maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(label));
                }
                for (int j = 0; j < tds.length(0); j++) {
                    vds = DataSetOps.slice1( tds, j );

                    graphics.setColor(color);
                    if (extraConnectorElements[j] != null) {  // thread race

                        extraConnectorElements[j].render(graphics, xAxis, yAxis, vds, mon);

                        String label = String.valueOf( yunits.createDatum(yds.value(j)) ).trim();

                        lparent.addToLegend( this, (ImageIcon)GraphUtil.colorIcon( extraConnectorElements[j].color, 5, 5 ), j, label );
                    }
                }
            }
            graphics.dispose();
            return;
        }

        if (this.fillToReference) {
            fillElement.render(graphics, xAxis, yAxis, vds, mon);
        }


        graphics.setColor(color);
        log.log(Level.FINEST, "drawing psymConnector in {0}", color);

        int connectCount= psymConnectorElement.render(graphics, xAxis, yAxis, vds, mon);
        log.log(Level.FINEST, "connectCount: {0}", connectCount);
        errorElement.render(graphics, xAxis, yAxis, vds, mon);

        int symCount= 0;
        if (psym != DefaultPlotSymbol.NONE) {

            symCount= psymsElement.render(graphics, xAxis, yAxis, vds, mon);
            log.log(Level.FINEST, "symCount: {0}", symCount);
//double simplifyFactor = (double) (  i - firstIndex ) / (lastIndex - firstIndex);
            mon.finished();
        }

//g.drawString( "renderCount="+renderCount+" updateCount="+updateImageCount,xAxis.getColumn().getDMinimum()+5, yAxis.getRow().getDMinimum()+20 );
        long milli = System.currentTimeMillis();
        long renderTime = (milli - timer0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        setRenderPointsPerMillisecond(dppms);

        logger.log(Level.FINER, "render: {0} total:{1} fps:{2} pts/ms:{3}", new Object[]{renderTime, milli - lastUpdateMillis, 1000. / (milli - lastUpdateMillis), dppms});
        lastUpdateMillis = milli;

        if (dataSetClipped) {
            lparent.postMessage(this, "dataset clipped at " + dataSetSizeLimit + " points", DasPlot.WARNING, null, null);
        }

        if ( ( lastIndex_v - firstIndex_v < 2 ) && dataSet.length()>1 ) {
            if ( messageCount++==0) {
                if ( lastIndex_v<2 ) {
                    lparent.postMessage(this, "data starts after range", DasPlot.INFO, null, null);
                } else if ( this.dslen - this.firstIndex_v < 2 ) {
                    lparent.postMessage(this, "data ends before range", DasPlot.INFO, null, null);
                } else {
                    lparent.postMessage(this, "fewer than two points visible", DasPlot.INFO, null, null);
                }
            }
        }

        graphics.dispose();
    }


    /**
     * do the same as updatePlotImage, but use AffineTransform to implement axis transform.
     */
    @Override
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) {
        logger.log(Level.FINE, "enter {0}.updatePlotImage: {1}", new Object[]{id, String.valueOf(getDataSet()) });

        updateImageCount++;

        reportCount();

        try {
            super.updatePlotImage(xAxis, yAxis, monitor);
        } catch (DasException e) {
            // it doesn't throw DasException, but interface requires exception, jbf 5/26/2005
            throw new RuntimeException(e);
        }


        QDataSet dataSet = getDataSet();

        if (dataSet == null ) {
            logger.fine("dataset was null");
            return;
        }

        if ( dataSet.rank() == 0) {
            logger.fine("rank 0 dataset");
            return;
        }

        if ( dataSet.length() == 0) {
            logger.fine("dataset was empty");
            return;
        }

        if ( !this.isActive() ) {
            return;
        }
        
        boolean plottable = true;

        QDataSet tds = null;
        QDataSet vds = null;

        QDataSet xds = SemanticOps.xtagsDataSet(dataSet);
        if ( !SemanticOps.isTableDataSet(dataSet) ) {
            vds= ytagsDataSet(ds);

            unitsWarning= false;
            plottable = SemanticOps.getUnits(vds).isConvertableTo(yAxis.getUnits());
            if ( !plottable ) {
                if ( UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(vds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    plottable= true; // we'll provide a warning
                    unitsWarning= true;
                }
            }

        } else {
            tds = (QDataSet) dataSet;
            plottable = SemanticOps.getUnits(tds).isConvertableTo(yAxis.getUnits());
            if ( !plottable ) {
                if ( UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(tds) ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                    plottable= true; // we'll provide a warning
                    unitsWarning= true;
                }
            }
        }

        plottable = SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits());
        xunitsWarning= false;
        if ( !plottable ) {
            if ( UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(xds) ) && UnitsUtil.isRatioMeasurement( xAxis.getUnits() ) ) {
                plottable= true; // we'll provide a warning
                xunitsWarning= true;
            }
        }

        if (!plottable) {
            return;
        }


        logger.fine("entering updatePlotImage");
        long t0 = System.currentTimeMillis();

        dataSetClipped = false;


        firstIndex= -1;
        lastIndex= -1;
        
        if (vds != null) {

            updateFirstLast(xAxis, yAxis, xds, vds );

            if (fillToReference) {
                fillElement.update(xAxis, yAxis, dataSet, monitor);
            }
            if (psymConnector != PsymConnector.NONE) {
                psymConnectorElement.update(xAxis, yAxis, dataSet, monitor);
            }

            errorElement.update(xAxis, yAxis, dataSet, monitor);
            psymsElement.update(xAxis, yAxis, dataSet, monitor);
            selectionArea= calcSelectionArea( xAxis, yAxis, xds, vds );

        } else if (tds != null) {
            extraConnectorElements = new PsymConnectorRenderElement[tds.length(0)];
            for (int i = 0; i < tds.length(0); i++) {
                extraConnectorElements[i] = new PsymConnectorRenderElement();

                float[] colorHSV = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                if (colorHSV[2] < 0.7f) {
                    colorHSV[2] = 0.7f;
                }
                if (colorHSV[1] < 0.7f) {
                    colorHSV[1] = 0.7f;
                }
                extraConnectorElements[i].color = Color.getHSBColor(i / 6.f, colorHSV[1], colorHSV[2]);
                try {
                    vds = DataSetOps.unbundle(tds,i);
                } catch ( IllegalArgumentException ex ) { // rank 2 depend_1 is one way to get here.
                    vds = DataSetOps.slice1( tds, i);
                    //super.setException(ex);
                    //return;
                }

                if (i == 0) {
                    updateFirstLast(xAxis, yAxis, xds, vds ); // minimal support assumes vert slice data is all valid or all invalid.

                }
                extraConnectorElements[i].update(xAxis, yAxis, vds, monitor);
                if ( i==0 ) selectionArea= calcSelectionArea( xAxis, yAxis, xds, vds );
            }
        } else {
            System.err.println("both tds and vds are null");
        }
        
        if (getParent() != null) {
            getParent().repaint();
        }

        logger.log(Level.FINE, "done updatePlotImage in {0} ms", (System.currentTimeMillis() - t0));
        
        long milli = System.currentTimeMillis();
        long renderTime = (milli - t0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        setUpdatesPointsPerMillisecond(dppms);
    }

    private Shape calcSelectionArea( DasAxis xaxis, DasAxis yaxis, QDataSet xds, QDataSet ds ) {

        Datum widthx;
        if (xaxis.isLog()) {
            widthx = Units.logERatio.createDatum(Math.log(xaxis.getDataMaximum(xaxis.getUnits()) - xaxis.getDataMinimum(xaxis.getUnits())));
        } else {
            widthx = xaxis.getDatumRange().width();
        }
        Datum widthy;
        if (yaxis.isLog()) {
            widthy = Units.logERatio.createDatum(Math.log(yaxis.getDataMaximum(yaxis.getUnits()) - yaxis.getDataMinimum(yaxis.getUnits())));
        } else {
            widthy = yaxis.getDatumRange().width();
        }

        if ( xaxis.getColumn().getWidth()==0 || yaxis.getRow().getHeight()==0 ) return null;
        
        QDataSet ds2= ds;
        if ( this.unitsWarning ) {
            ArrayDataSet ds3= ArrayDataSet.copy(ds);
            ds3.putProperty( QDataSet.UNITS, yaxis.getUnits() );
            ds2= ds3;
        }
        if ( this.xunitsWarning ) {
            ArrayDataSet ds3= ArrayDataSet.copy(xds);
            ds3.putProperty( QDataSet.UNITS, yaxis.getUnits() );
            xds= ds3;
        }
        if ( ds2.rank()==2 ) {
            ds2= DataSetOps.slice1( ds2, 0 );
        }

        try {
            QDataSet reduce = VectorUtil.reduce2D(
                xds, ds2,
                firstIndex,
                Math.min( firstIndex+20000, lastIndex),
                widthx.divide(xaxis.getColumn().getWidth()/5),
                widthy.divide(yaxis.getRow().getHeight()/5)
                );

            GeneralPath path = GraphUtil.getPath(xaxis, yaxis, reduce, histogram, true );

            Shape s = new BasicStroke(5.f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND).createStrokedShape(path);
            return s;

        } catch ( InconvertibleUnitsException ex ) {
            logger.fine("failed to convert units in calcSelectionArea");
            return SelectionUtil.NULL; // transient state, hopefully...

        }

        

    }

    @Override
    protected void installRenderer() {
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            DasPlot lparent= parent;
            if ( lparent==null ) throw new IllegalArgumentException("parent not set");
            DasMouseInputAdapter mouseAdapter = lparent.mouseAdapter;
            DasPlot p = lparent;
            mouseAdapter.addMouseModule(new MouseModule(p, new LengthDragRenderer(p, p.getXAxis(), p.getYAxis()), "Length"));

            MouseModule ch = new CrossHairMouseModule(parent, this, parent.getXAxis(), parent.getYAxis());
            mouseAdapter.addMouseModule(ch);
        }

        updatePsym();
    }

    @Override
    protected void uninstallRenderer() {
        //There's a bug here, that if multiple SeriesRenderers are drawing on a plot, then we want to leave the mouseModules.
        //We can't do that right now...
    }

    public Element getDOMElement(Document document) {
        return null;
    }

    /**
     * get an Icon representing the trace.  This will be an ImageIcon.
     * TODO: cache the result to support use in legend.
     * @return
     */
    @Override
    public javax.swing.Icon getListIcon() {
        Image i = new BufferedImage(15, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();
        drawListIcon( g, 0, 0 );
        return new ImageIcon(i);
    }

    @Override
    public void drawListIcon(Graphics2D g1, int x, int y) {

        Graphics2D g= (Graphics2D) g1.create( x, y, 16, 16 );

        g.setRenderingHints(DasProperties.getRenderingHints());

        DasPlot lparent= parent;
        if ( lparent!=null ) g.setBackground(lparent.getBackground());

        // leave transparent if not white
        if (color.equals(Color.white)) {
            g.setColor(Color.GRAY);
        } else {
            g.setColor(new Color(0, 0, 0, 0));
        }

        g.fillRect(0, 0, 15, 10);

        if (fillToReference) {
            g.setColor(fillColor);
            Polygon p = new Polygon(new int[]{2, 13, 13, 2}, new int[]{3, 7, 10, 10}, 4);
            g.fillPolygon(p);
        }

        g.setColor(color);
        Stroke stroke0 = g.getStroke();
        getPsymConnector().drawLine(g, 2, 3, 13, 7, 1.5f);
        g.setStroke(stroke0);
        psym.draw(g, 7, 5, 3.f, fillStyle);
        return;
    }


    @Override
    public String getListLabel() {
        return String.valueOf(this.getDataSetDescriptor());
    }

    /**
     * trigger render, but not updatePlotImage.
     */
    private void refreshRender() {
        DasPlot lparent= parent;
        if ( lparent!=null ) {
            lparent.invalidateCacheImage();
            lparent.repaint();
        }
    }
    
// ------- Begin Properties ---------------------------------------------  //
    public PsymConnector getPsymConnector() {
        return psymConnector;
    }

    public void setPsymConnector(PsymConnector p) {
        PsymConnector old = this.psymConnector;
        if (!p.equals(psymConnector)) {
            psymConnector = p;
            refreshImage();
            propertyChangeSupport.firePropertyChange("psymConnector", old, p);
        }

    }

    /** Getter for property psym.
     * @return Value of property psym.
     */
    public PlotSymbol getPsym() {
        return this.psym;
    }

    /** Setter for property psym.
     * @param psym New value of property psym.
     */
    public void setPsym(PlotSymbol psym) {
        if (psym == null) {
            throw new NullPointerException("psym cannot be null");
        }

        if (psym != this.psym) {
            Object oldValue = this.psym;
            this.psym = (DefaultPlotSymbol) psym;
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("psym", oldValue, psym);
        }

    }

    /** Getter for property symsize.
     * @return Value of property symsize.
     */
    public double getSymSize() {
        return this.symSize;
    }

    /** Setter for property symsize.
     * @param symSize New value of property symsize.
     */
    public void setSymSize(double symSize) {
        double old = this.symSize;
        if (this.symSize != symSize) {
            this.symSize = symSize;
            setPsym(this.psym);
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("symSize", new Double(old), new Double(symSize));
        }

    }

    /** Getter for property color.
     * @return Value of property color.
     */
    public Color getColor() {
        return color;
    }

    /** Setter for property color.
     * @param color New value of property color.
     */
    public void setColor(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("null color");
        }
        Color old = this.color;
        if (!this.color.equals(color)) {
            this.color = color;
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("color", old, color);
        }

    }

    public double getLineWidth() {
        return lineWidth;
    }

    public void setLineWidth(double f) {
        double old = this.lineWidth;
        if (this.lineWidth != f) {
            lineWidth = f;
            updatePsym();
            refreshRender();
            propertyChangeSupport.firePropertyChange("lineWidth", new Double(old), new Double(f));
        }

    }

    /** Getter for property antiAliased.
     * @return Value of property antiAliased.
     *
     */
    public boolean isAntiAliased() {
        return this.antiAliased;
    }

    /** Setter for property antiAliased.
     * @param antiAliased New value of property antiAliased.
     *
     */
    public void setAntiAliased(boolean antiAliased) {
        boolean old = this.antiAliased;
        this.antiAliased = antiAliased;
        updatePsym();
        refreshImage();
        propertyChangeSupport.firePropertyChange("antiAliased", old, antiAliased);
    }

    public boolean isHistogram() {
        return histogram;
    }

    public void setHistogram(final boolean b) {
        boolean old = b;
        if (b != histogram) {
            histogram = b;
            refreshImage();
            propertyChangeSupport.firePropertyChange("histogram", old, antiAliased);
        }

    }

    /**
     * Holds value of property fillColor.
     */
    private Color fillColor = Color.lightGray;

    /**
     * Getter for property fillReference.
     * @return Value of property fillReference.
     */
    public Color getFillColor() {
        return this.fillColor;
    }

    /**
     * Setter for property fillReference.
     * @param fillReference New value of property fillReference.
     */
    public void setFillColor(Color color) {
        Color old = this.fillColor;
        if (!this.fillColor.equals(color)) {
            this.fillColor = color;
            update();
            propertyChangeSupport.firePropertyChange("fillColor", old, color);
        }

    }
    /**
     * Holds value of property colorByDataSetId.
     */
    private String colorByDataSetId = "";

    /**
     * Getter for property colorByDataSetId.
     * @return Value of property colorByDataSetId.
     */
    public String getColorByDataSetId() {
        return this.colorByDataSetId;
    }

    /**
     * The dataset plane to use to get colors.  If this is null or "", then
     * no coloring is done. (Note the default plane cannot be used to color.)
     * @param colorByDataSetId New value of property colorByDataSetId.
     */
    public void setColorByDataSetId(String colorByDataSetId) {
        String oldVal = this.colorByDataSetId;
        this.colorByDataSetId = colorByDataSetId;
        update();
        if ( !colorByDataSetId.equals("") ) updatePsym();
        propertyChangeSupport.firePropertyChange("colorByDataSetId", oldVal, colorByDataSetId);
    }

    PropertyChangeListener colorBarListener= new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent evt) {
           if (colorByDataSetId != null && !colorByDataSetId.equals("")) {
               update();
           }
        }
    };

    /**
     * Setter for property colorBar.
     * @param colorBar New value of property colorBar.
     */
    @Override
    public void setColorBar(DasColorBar cb) {
        if ( this.colorBar == cb) {
            return;
        }
        if (colorBar != null) {
            colorBar.removePropertyChangeListener( colorBarListener );
        }
        super.setColorBar(cb);
        if (colorBar != null) {
            if (parent != null && parent.getCanvas() != null) {
                parent.getCanvas().add(colorBar);
            }
            colorBar.addPropertyChangeListener( colorBarListener );        
        }
        refreshImage();
        updatePsym();
    }
    /**
     * Holds value of property fillToReference.
     */
    private boolean fillToReference;

    /**
     * Getter for property fillToReference.
     * @return Value of property fillToReference.
     */
    public boolean isFillToReference() {
        return this.fillToReference;
    }

    /**
     * Setter for property fillToReference.
     * @param fillToReference New value of property fillToReference.
     */
    public void setFillToReference(boolean fillToReference) {
        boolean old = this.fillToReference;
        if (this.fillToReference != fillToReference) {
            this.fillToReference = fillToReference;
            update();
            propertyChangeSupport.firePropertyChange("fillToReference", old, fillToReference);
        }

    }
    /**
     * Holds value of property reference.
     */
    private Datum reference = Units.dimensionless.createDatum(0);

    /**
     * Getter for property reference.
     * @return Value of property reference.
     */
    public Datum getReference() {
        return this.reference;
    }

    /**
     * Setter for property reference.
     * @param reference New value of property reference.
     */
    public void setReference(Datum reference) {
        Datum old = this.reference;
        if (!this.reference.equals(reference)) {
            this.reference = reference;
            refreshImage();
            propertyChangeSupport.firePropertyChange("reference", old, reference);
        }

    }

    /**
     * Holds value of property resetDebugCounters.
     */
    private boolean resetDebugCounters;

    /**
     * Getter for property resetDebugCounters.
     * @return Value of property resetDebugCounters.
     */
    public boolean isResetDebugCounters() {
        return this.resetDebugCounters;
    }

    /**
     * Setter for property resetDebugCounters.
     * @param resetDebugCounters New value of property resetDebugCounters.
     */
    public void setResetDebugCounters(boolean resetDebugCounters) {
        if (resetDebugCounters) {
            renderCount = 0;
            updateImageCount = 0;
            update();
        }

    }
    /**
     * Holds value of property simplifyPaths.
     */
    private boolean simplifyPaths = true;

    /**
     * Getter for property simplifyPaths.
     * @return Value of property simplifyPaths.
     */
    public boolean isSimplifyPaths() {
        return this.simplifyPaths;
    }

    /**
     * Setter for property simplifyPaths.
     * @param simplifyPaths New value of property simplifyPaths.
     */
    public void setSimplifyPaths(boolean simplifyPaths) {
        this.simplifyPaths = simplifyPaths;
        refreshImage();
    }
    private boolean stampPsyms = true;
    public static final String PROP_STAMPPSYMS = "stampPsyms";

    public boolean isStampPsyms() {
        return this.stampPsyms;
    }

    public void setStampPsyms(boolean newstampPsyms) {
        boolean oldstampPsyms = stampPsyms;
        this.stampPsyms = newstampPsyms;
        propertyChangeSupport.firePropertyChange(PROP_STAMPPSYMS, oldstampPsyms, newstampPsyms);
        refreshImage();
    }

    public FillStyle getFillStyle() {
        return fillStyle;
    }

    public void setFillStyle(FillStyle fillStyle) {
        this.fillStyle = fillStyle;
        updatePsym();
        refreshImage();
    }

    /**
     * like accept context, but provides a shape to indicate selection.  This
     * should be roughly the same as the locus of points where acceptContext is
     * true.
     * @return
     */
    public Shape selectionArea() {
        return selectionArea==null ? SelectionUtil.NULL : selectionArea;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        boolean accept = false;

        Point2D.Double dp = new Point2D.Double(x, y);

        if (this.fillToReference && fillElement.acceptContext(dp)) {
            accept = true;
        }

        if ((!accept) && psymConnectorElement.acceptContext(dp)) {
            accept = true;
        }

        if ((!accept) && extraConnectorElements != null) {
            for (int j = 0; j < extraConnectorElements.length; j++) {
                if (!accept && extraConnectorElements[j] != null && extraConnectorElements[j].acceptContext(dp)) {
                    accept = true;
                }
            }
        }

        if ((!accept) && psymsElement.acceptContext(dp)) {
            accept = true;
        }

        if ((!accept) && errorElement.acceptContext(dp)) {
            accept = true;
        }

        return accept;
    }
    /**
     * property dataSetSizeLimit is the maximum number of points that will be rendered.
     * This is introduced because large datasets cause java2D plotting to fail.
     * When the size limit is reached, the data is clipped and a message is displayed.
     */
    private int dataSetSizeLimit = 200000;

    /**
     * Getter for property dataSetSizeLimit.
     * @return Value of property dataSetSizeLimit.
     */
    public int getDataSetSizeLimit() {
        return this.dataSetSizeLimit;
    }

    /**
     * Setter for property dataSetSizeLimit.
     * @param dataSetSizeLimit New value of property dataSetSizeLimit.
     */
    public void setDataSetSizeLimit(int dataSetSizeLimit) {
        int oldDataSetSizeLimit = this.dataSetSizeLimit;
        this.dataSetSizeLimit = dataSetSizeLimit;
        refreshImage();
        propertyChangeSupport.firePropertyChange("dataSetSizeLimit", oldDataSetSizeLimit, dataSetSizeLimit );
    }
    private double updatesPointsPerMillisecond;
    public static final String PROP_UPDATESPOINTSPERMILLISECOND = "updatesPointsPerMillisecond";

    public double getUpdatesPointsPerMillisecond() {
        return this.updatesPointsPerMillisecond;
    }

    public void setUpdatesPointsPerMillisecond(double newupdatesPointsPerMillisecond) {
        //double oldupdatesPointsPerMillisecond = updatesPointsPerMillisecond;
        this.updatesPointsPerMillisecond = newupdatesPointsPerMillisecond;
    //propertyChangeSupport.firePropertyChange(PROP_UPDATESPOINTSPERMILLISECOND, oldupdatesPointsPerMillisecond, newupdatesPointsPerMillisecond);
    }
    private double renderPointsPerMillisecond;
    public static final String PROP_RENDERPOINTSPERMILLISECOND = "renderPointsPerMillisecond";

    public double getRenderPointsPerMillisecond() {
        return this.renderPointsPerMillisecond;
    }

    public void setRenderPointsPerMillisecond(double newrenderPointsPerMillisecond) {
        //double oldrenderPointsPerMillisecond = renderPointsPerMillisecond;
        this.renderPointsPerMillisecond = newrenderPointsPerMillisecond;
    //propertyChangeSupport.firePropertyChange(PROP_RENDERPOINTSPERMILLISECOND, oldrenderPointsPerMillisecond, newrenderPointsPerMillisecond);
    }

    public int getFirstIndex() {
        return this.firstIndex;
    }

    public int getLastIndex() {
        return this.lastIndex;
    }

    protected boolean cadenceCheck = true;
    public static final String PROP_CADENCECHECK = "cadenceCheck";

    public boolean isCadenceCheck() {
        return cadenceCheck;
    }

    /**
     * If true, then use a cadence estimate to determine and indicate data gaps.
     * @param cadenceCheck
     */
    public void setCadenceCheck(boolean cadenceCheck) {
        boolean oldCadenceCheck = this.cadenceCheck;
        this.cadenceCheck = cadenceCheck;
        refreshImage();
        propertyChangeSupport.firePropertyChange(PROP_CADENCECHECK, oldCadenceCheck, cadenceCheck);
    }

    @Override
    public boolean acceptsDataSet(QDataSet dataSet) {
        QDataSet ds1= dataSet;
        //QDataSet vds, tds;
        boolean plottable= false;
        if ( !SemanticOps.isTableDataSet(dataSet) ) {
            if ( ds1.rank()==2 && SemanticOps.isBundle(ds1) ) {
            //    vds = DataSetOps.unbundleDefaultDataSet( ds );
            } else if ( ds1.rank()!=1 ) {
                logger.fine("dataset rank error");
                return false;
            }  else {
            //    vds = (QDataSet) dataSet;
            }

            unitsWarning= false;
            plottable = true;

        } else {
            plottable = true;
        }

        return plottable;
    }


}
