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
package edu.uiowa.physics.pw.das.graph;

import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.DasProperties;
import edu.uiowa.physics.pw.das.components.propertyeditor.Displayable;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetUtil;
import edu.uiowa.physics.pw.das.dataset.TableDataSet;
import edu.uiowa.physics.pw.das.dataset.VectorDataSet;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.DatumRangeUtil;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.event.DasMouseInputAdapter;
import edu.uiowa.physics.pw.das.event.LengthDragRenderer;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.das2.util.monitor.ProgressMonitor;
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
public class SeriesRenderer extends Renderer implements Displayable {

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
    private int firstIndex;/* the index of the first point drawn, nonzero when X is monotonic and we can clip. */

    private int lastIndex;/* the non-inclusive index of the last point drawn. */

    boolean updating = false;
    private Logger log = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
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
    public static final String PROPERTY_X_DELTA_PLUS = "X_DELTA_PLUS";
    public static final String PROPERTY_X_DELTA_MINUS = "X_DELTA_MINUS";
    public static final String PROPERTY_Y_DELTA_PLUS = "Y_DELTA_PLUS";
    public static final String PROPERTY_Y_DELTA_MINUS = "Y_DELTA_MINUS";

    interface RenderElement {

        int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon);

        void update(DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon);

        boolean acceptContext(Point2D.Double dp);
    }

    class PsymRenderElement implements RenderElement {

        protected GeneralPath psymsPath; // store the location of the psyms here.
        int[] colors; // store the color index  of each psym
        int[] ipsymsPath; // store the location of the psyms here, evens=x, odds=y
        int count; // the number of points to plot

        /**
         * render the psyms by stamping an image at the psym location.  The intent is to
         * provide fast rendering by reducing fidelity.
         * On 20080206, this was measured to run at 320pts/millisecond for FillStyle.FILL
         * On 20080206, this was measured to run at 300pts/millisecond in FillStyle.OUTLINE
         */
        private int renderStamp(Graphics2D g, DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon) {

            VectorDataSet colorByDataSet = null;
            if (colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = (VectorDataSet) vds.getPlanarView(colorByDataSetId);
            }

            if (colorByDataSet != null) {
                for (int i = 0; i < count; i++) {
                    int icolor = colors[i];
                    g.drawImage(coloredPsyms[icolor], ipsymsPath[i * 2] - cmx, ipsymsPath[i * 2 + 1] - cmy, parent);
                }
            } else {
                for (int i = 0; i < count; i++) {
                    g.drawImage(psymImage, ipsymsPath[i * 2] - cmx, ipsymsPath[i * 2 + 1] - cmy, parent);
                }
            }

            return count;

        }

        /**
         * Render the psyms individually.  This is the highest fidelity rendering, and
         * should be used in printing.
         * On 20080206, this was measured to run at 45pts/millisecond in FillStyle.FILL
         * On 20080206, this was measured to run at 9pts/millisecond in FillStyle.OUTLINE
         */
        private int renderDraw(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, VectorDataSet dataSet, ProgressMonitor mon) {

            float fsymSize = (float) symSize;

            if (colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = (VectorDataSet) dataSet.getPlanarView(colorByDataSetId);
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
                    psym.draw(graphics, ipsymsPath[i * 2], ipsymsPath[i * 2 + 1], fsymSize, fillStyle);
                }

            } else {
                for (int i = 0; i < count; i++) {
                    psym.draw(graphics, ipsymsPath[i * 2], ipsymsPath[i * 2 + 1], fsymSize, fillStyle);
                }
            }

            return count;

        }

        public synchronized int render(Graphics2D graphics, DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon) {
            int i;
            if (stampPsyms && !parent.getCanvas().isPrintingThread()) {
                i = renderStamp(graphics, xAxis, yAxis, vds, mon);
            } else {
                i = renderDraw(graphics, xAxis, yAxis, vds, mon);
            }
            return i;
        }

        public synchronized void update(DasAxis xAxis, DasAxis yAxis, VectorDataSet dataSet, ProgressMonitor mon) {

            VectorDataSet colorByDataSet = null;
            Units cunits = null;
            if (colorByDataSetId != null && !colorByDataSetId.equals("")) {
                colorByDataSet = (VectorDataSet) dataSet.getPlanarView(colorByDataSetId);
                if (colorByDataSet != null) {
                    cunits = colorByDataSet.getYUnits();
                }
            }


            Units xUnits = xAxis.getUnits();
            Units yUnits = yAxis.getUnits();

            double x, y;
            int fx, fy;

            psymsPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (lastIndex - firstIndex) / 100);
            ipsymsPath = new int[(lastIndex - firstIndex) * 2];
            colors = new int[lastIndex - firstIndex + 2];

            int index = firstIndex;

            x = dataSet.getXTagDouble(index, xUnits);
            y = dataSet.getDouble(index, yUnits);
            fx = (int) xAxis.transform(x, xUnits);
            fy = (int) yAxis.transform(y, yUnits);

            int i = 0;
            for (; index < lastIndex; index++) {
                x = dataSet.getXTagDouble(index, xUnits);
                y = dataSet.getDouble(index, yUnits);

                final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);

                fx = (int) xAxis.transform(x, xUnits);
                fy = (int) yAxis.transform(y, yUnits);

                if (isValid) {
                    ipsymsPath[i * 2] = fx;
                    ipsymsPath[i * 2 + 1] = fy;
                    if (colorByDataSet != null) {
                        colors[i] = colorBar.indexColorTransform(colorByDataSet.getDouble(index, cunits), cunits);
                    }
                    i++;
                }
            }

            count = i;

        }

        public boolean acceptContext(Point2D.Double dp) {
            if (ipsymsPath == null) {
                return false;
            }
            double rad = Math.max(symSize, 5);

            for (int index = firstIndex; index < lastIndex; index++) {
                int i = index - firstIndex;
                if (dp.distance(ipsymsPath[i * 2], ipsymsPath[i * 2 + 1]) < rad) {
                    return true;
                }
            }
            return false;
        }
    }

    class ErrorBarRenderElement implements RenderElement {

        GeneralPath p;

        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon) {
            if (p == null) {
                return 0;
            }
            g.draw(p);
            return lastIndex - firstIndex;
        }

        public void update(DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon) {
            VectorDataSet deltaPlusY = (VectorDataSet) vds.getPlanarView(PROPERTY_Y_DELTA_PLUS);
            VectorDataSet deltaMinusY = (VectorDataSet) vds.getPlanarView(PROPERTY_Y_DELTA_MINUS);

            p = null;

            if (deltaMinusY == null) {
                return;
            }
            if (deltaMinusY == null) {
                return;
            }
            Units xunits = vds.getXUnits();
            Units yunits = vds.getYUnits();
            Units yoffsetUnits = yunits.getOffsetUnits();

            p = new GeneralPath();
            for (int i = firstIndex; i < lastIndex; i++) {
                float ix = (float) xAxis.transform(vds.getXTagDouble(i, xunits), xunits);
                float iym = (float) yAxis.transform(vds.getDouble(i, yunits) - deltaMinusY.getDouble(i, yoffsetUnits), yunits);
                float iyp = (float) yAxis.transform(vds.getDouble(i, yunits) + deltaPlusY.getDouble(i, yoffsetUnits), yunits);
                p.moveTo(ix, iym);
                p.lineTo(ix, iyp);
            }

        }

        public boolean acceptContext(Point2D.Double dp) {
            return p != null && p.contains(dp.x - 2, dp.y - 2, 5, 5);

        }
    }

    class PsymConnectorRenderElement implements RenderElement {

        private GeneralPath path1;
        private Color color;  // override default color

        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon) {
            if (path1 == null) {
                return 0;
            }
            if (color != null) {
                g.setColor(color);
            }
            psymConnector.draw(g, path1, (float) lineWidth);
            return 0;
        }

        public void update(DasAxis xAxis, DasAxis yAxis, VectorDataSet dataSet, ProgressMonitor mon) {
            Units xUnits = xAxis.getUnits();
            Units yUnits = yAxis.getUnits();

            GeneralPath newPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (lastIndex - firstIndex) / 100);

            Datum sw = DataSetUtil.guessXTagWidth(dataSet);
            double xSampleWidth = sw.doubleValue(xUnits.getOffsetUnits());

            /* fuzz the xSampleWidth */
            xSampleWidth = xSampleWidth * 1.10;

            double x = Double.NaN;
            double y = Double.NaN;

            double x0 = Double.NaN; /* the last plottable point */
            double y0 = Double.NaN; /* the last plottable point */

            float fx = Float.NaN;
            float fy = Float.NaN;
            float fx0 = Float.NaN;
            float fy0 = Float.NaN;

            int index;

            index = firstIndex;
            x = (double) dataSet.getXTagDouble(index, xUnits);
            y = (double) dataSet.getDouble(index, yUnits);

            // first point //
            logger.fine("firstPoint moveTo,LineTo= " + x + "," + y);
            fx = (float) xAxis.transform(x, xUnits);
            fy = (float) yAxis.transform(y, yUnits);
            if (histogram) {
                float fx1 = (float) xAxis.transform(x - xSampleWidth / 2, xUnits);
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

            // now loop through all of them. //

            for (; index < lastIndex; index++) {

                x = dataSet.getXTagDouble(index, xUnits);
                y = dataSet.getDouble(index, yUnits);

                final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);

                fx = (float) xAxis.transform(x, xUnits);
                fy = (float) yAxis.transform(y, yUnits);

                //double tx= xAxis.transformFast( x, xUnits );

                //System.err.println( ""+(float)tx+ "   " + fx );

                if (isValid) {
                    if ((x - x0) < xSampleWidth) {
                        // draw connect-a-dot between last valid and here
                        if (histogram) {
                            float fx1 = (fx0 + fx) / 2;
                            newPath.lineTo(fx1, fy0);
                            newPath.lineTo(fx1, fy);
                            newPath.lineTo(fx, fy);
                        } else {
                            newPath.lineTo(fx, fy); // this is the typical path

                        }

                    } else {
                        // introduce break in line
                        if (histogram) {
                            float fx1 = (float) xAxis.transform(x0 + xSampleWidth / 2, xUnits);
                            newPath.lineTo(fx1, fy0);

                            fx1 = (float) xAxis.transform(x - xSampleWidth / 2, xUnits);
                            newPath.moveTo(fx1, fy);
                            newPath.lineTo(fx, fy);

                        } else {
                            newPath.moveTo(fx, fy);
                            newPath.lineTo(fx, fy);
                        }

                    } // else introduce break in line

                    x0 = x;
                    y0 = y;
                    fx0 = fx;
                    fy0 = fy;

                } else {
                    newPath.moveTo(fx0, fy0); // place holder

                }

            } // for ( ; index < ixmax && lastIndex; index++ )


            if (!histogram && simplifyPaths && colorByDataSet == null) {
                //j   System.err.println( "input: " );
                //j   System.err.println( GraphUtil.describe( newPath, true) );
                this.path1 = GraphUtil.reducePath(newPath.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex));
            } else {
                this.path1 = newPath;
            }

        }

        public boolean acceptContext(Point2D.Double dp) {
            return this.path1 != null && path1.intersects(dp.x - 5, dp.y - 5, 10, 10);
        }
    }

    class FillRenderElement implements RenderElement {

        private GeneralPath fillToRefPath1;

        public int render(Graphics2D g, DasAxis xAxis, DasAxis yAxis, VectorDataSet vds, ProgressMonitor mon) {
            g.setColor(fillColor);
            g.fill(fillToRefPath1);
            return 0;
        }

        public void update(DasAxis xAxis, DasAxis yAxis, VectorDataSet dataSet, ProgressMonitor mon) {
            Units xUnits = xAxis.getUnits();
            Units yUnits = yAxis.getUnits();

            GeneralPath fillPath = new GeneralPath(GeneralPath.WIND_NON_ZERO, 110 * (lastIndex - firstIndex) / 100);

            Datum sw = DataSetUtil.guessXTagWidth(dataSet);
            double xSampleWidth = sw.doubleValue(xUnits.getOffsetUnits());

            /* fuzz the xSampleWidth */
            xSampleWidth = xSampleWidth * 1.10;

            if (reference != null && reference.getUnits() != yAxis.getUnits()) {
                // switch the units to the axis units.
                reference = yAxis.getUnits().createDatum(reference.doubleValue(reference.getUnits()));
            }

            if (reference == null) {
                reference = yUnits.createDatum(yAxis.isLog() ? 1.0 : 0.0);
            }

            double yref = (double) reference.doubleValue(yUnits);

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
            x = (double) dataSet.getXTagDouble(index, xUnits);
            y = (double) dataSet.getDouble(index, yUnits);

            // first point //
            fx = (float) xAxis.transform(x, xUnits);
            fy = (float) yAxis.transform(y, yUnits);
            if (histogram) {
                float fx1 = (float) xAxis.transform(x - xSampleWidth / 2, xUnits);
                fillPath.moveTo(fx1, fyref);
                fillPath.lineTo(fx1, fy);
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

                for (; index < lastIndex; index++) {

                    x = dataSet.getXTagDouble(index, xUnits);
                    y = dataSet.getDouble(index, yUnits);

                    final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);

                    fx = (float) xAxis.transform(x, xUnits);
                    fy = (float) yAxis.transform(y, yUnits);

                    if (isValid) {
                        if ((x - x0) < xSampleWidth) {
                            // draw connect-a-dot between last valid and here
                            if (histogram) {
                                float fx1 = (fx0 + fx) / 2;
                                fillPath.lineTo(fx1, fy0);
                                fillPath.lineTo(fx1, fy);
                                fillPath.lineTo(fx, fy);
                            } else {
                                fillPath.lineTo(fx, fy);
                            }

                        } else {
                            // introduce break in line
                            if (histogram) {
                                float fx1 = (float) xAxis.transform(x0 + xSampleWidth / 2, xUnits);
                                fillPath.lineTo(fx1, fy0);
                                fillPath.lineTo(fx1, fyref);
                                fx1 = (float) xAxis.transform(x - xSampleWidth / 2, xUnits);
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

            fillPath.lineTo(fx0, fyref);
            this.fillToRefPath1 = fillPath;

            if (simplifyPaths) {
                fillToRefPath1 = GraphUtil.reducePath(fillToRefPath1.getPathIterator(null), new GeneralPath(GeneralPath.WIND_NON_ZERO, lastIndex - firstIndex));
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
        int sx = (int) Math.ceil(symSize + 2 * lineWidth);
        int sy = (int) Math.ceil(symSize + 2 * lineWidth);
        double dcmx, dcmy;
        dcmx = (lineWidth + (int) (symSize / 2)) + 0.5;
        dcmy = (lineWidth + (int) (symSize / 2)) + 0.5;
        BufferedImage image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        Object rendering = antiAliased ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
        g.setColor(color);
        if (parent != null) {
            g.setBackground(parent.getBackground());
        }

        g.setStroke(new BasicStroke((float) lineWidth));

        psym.draw(g, dcmx, dcmy, (float) symSize, fillStyle);
        psymImage = image;

        if (colorBar != null) {
            IndexColorModel model = colorBar.getIndexColorModel();
            coloredPsyms = new Image[model.getMapSize()];
            for (int i = 0; i < model.getMapSize(); i++) {
                Color c = new Color(model.getRGB(i));
                image = new BufferedImage(sx, sy, BufferedImage.TYPE_INT_ARGB);
                g = (Graphics2D) image.getGraphics();
                if (parent != null) {
                    g.setBackground(parent.getBackground());
                }

                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, rendering);
                g.setColor(c);
                g.setStroke(new BasicStroke((float) lineWidth));

                psym.draw(g, dcmx, dcmy, (float) symSize, this.fillStyle);
                coloredPsyms[i] = image;
            }

        }

        cmx = (int) dcmx;
        cmy =
                (int) dcmy;

        refresh();
    }

    private void reportCount() {
        //if ( renderCount % 100 ==0 ) {
        //System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
        //new Throwable("").printStackTrace();
        //}
    }

    public synchronized void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        if (this.ds == null && lastException != null) {
            parent.postException(this, lastException);
            return;
        }

        renderCount++;
        reportCount();

        long timer0 = System.currentTimeMillis();

        DataSet dataSet = getDataSet();

        if (dataSet == null) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("null data set");
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        if (dataSet.getXLength() == 0) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("empty data set");
            parent.postMessage(this, "empty data set", DasPlot.INFO, null, null);
            return;
        }

        TableDataSet tds = null;
        VectorDataSet vds = null;
        boolean plottable = false;

        if (dataSet instanceof VectorDataSet) {
            vds = (VectorDataSet) dataSet;
            plottable = dataSet.getYUnits().isConvertableTo(yAxis.getUnits());
        } else if (dataSet instanceof TableDataSet) {
            tds = (TableDataSet) dataSet;
            plottable = tds.getZUnits().isConvertableTo(yAxis.getUnits());
        }
        plottable = plottable && dataSet.getXUnits().isConvertableTo(xAxis.getUnits());

        if (!plottable) {
            return;
        }

        logger.fine("rendering points: " + lastIndex + "  " + firstIndex);
        if (lastIndex == firstIndex) {
            parent.postMessage(SeriesRenderer.this, "dataset contains no valid data", DasPlot.INFO, null, null);
        }

        logger.fine("render data set " + dataSet);

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
            if (tds.getYLength(0) != extraConnectorElements.length) {
                return;
            } else {
                int maxWidth = 0;
                for (int j = 0; j < tds.getYLength(0); j++) {
                    String label = String.valueOf(tds.getYTagDatum(0, j));
                    maxWidth = Math.max(maxWidth, g.getFontMetrics().stringWidth(label));
                }
                for (int j = 0; j < tds.getYLength(0); j++) {
                    vds = tds.getYSlice(j, 0);

                    graphics.setColor(color);
                    if (extraConnectorElements[j] != null) {  // thread race
                        extraConnectorElements[j].render(graphics, xAxis, yAxis, vds, mon);

                        int myIndex = j;

                        int ix = (int) (xAxis.getColumn().getDMaximum() - maxWidth - parent.getEmSize());
                        int iy = (int) (yAxis.getRow().getDMinimum() + parent.getEmSize() * (0.5 + myIndex));
                        g.setColor(extraConnectorElements[j].color);
                        g.fillRect(ix, iy, 5, 5);
                        String label = String.valueOf(tds.getYTagDatum(0, j));
                        g.setColor(color);
                        g.drawString(label, ix + (int) (parent.getEmSize() / 2), iy + (int) (parent.getEmSize() / 2));
                    }
                }
            }
            return;
        }

        if (this.fillToReference) {
            fillElement.render(graphics, xAxis, yAxis, vds, mon);
        }


        graphics.setColor(color);
        log.finest("drawing psymConnector in " + color);

        psymConnectorElement.render(graphics, xAxis, yAxis, vds, mon);


        errorElement.render(graphics, xAxis, yAxis, vds, mon);

        if (psym != DefaultPlotSymbol.NONE) {

            psymsElement.render(graphics, xAxis, yAxis, vds, mon);

//double simplifyFactor = (double) (  i - firstIndex ) / (lastIndex - firstIndex);

            mon.finished();
        }

//g.drawString( "renderCount="+renderCount+" updateCount="+updateImageCount,xAxis.getColumn().getDMinimum()+5, yAxis.getRow().getDMinimum()+20 );
        long milli = System.currentTimeMillis();
        long renderTime = (milli - timer0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        setRenderPointsPerMillisecond(dppms);

        logger.finer("render: " + renderTime + " total:" + (milli - lastUpdateMillis) + " fps:" + (1000. / (milli - lastUpdateMillis)) + " pts/ms:" + dppms);
        lastUpdateMillis = milli;

        if (dataSetClipped) {
            parent.postMessage(this, "dataset clipped at " + dataSetSizeLimit + " points", DasPlot.WARNING, null, null);
        }
    }

    /**
     * updates firstIndex and lastIndex that point to the part of
     * the data that is plottable.  The plottable part is the part that 
     * might be visible while limiting the number of plotted points.
     */
    private void updateFirstLast( DasAxis xAxis, DasAxis yAxis, VectorDataSet dataSet ) {

        Units xUnits = xAxis.getUnits();
        Units yUnits = yAxis.getUnits();

        int ixmax;
        int ixmin;

        Boolean xMono = (Boolean) dataSet.getProperty(DataSet.PROPERTY_X_MONOTONIC);
        if (xMono != null && xMono.booleanValue()) {
            DatumRange visibleRange = xAxis.getDatumRange();
            if (parent.isOverSize()) {
                Rectangle plotBounds= parent.getCacheImageBounds();
                visibleRange= new DatumRange( xAxis.invTransform( plotBounds.x ), xAxis.invTransform( plotBounds.x+plotBounds.width ) );
            
            }
            ixmin = DataSetUtil.getPreviousColumn(dataSet, visibleRange.min());
            ixmax = DataSetUtil.getNextColumn(dataSet, visibleRange.max()) + 1; // +1 is for exclusive.
        } else {
            ixmin = 0;
            ixmax = dataSet.getXLength();
        }

        Datum sw = DataSetUtil.guessXTagWidth(dataSet);
        double xSampleWidth = sw.doubleValue(xUnits.getOffsetUnits());

        /* fuzz the xSampleWidth */
        xSampleWidth = xSampleWidth * 1.10;

        double x = Double.NaN;
        double y = Double.NaN;

        int index;

        // find the first valid point, set x0, y0 //
        for (index = ixmin; index < ixmax; index++) {
            x = (double) dataSet.getXTagDouble(index, xUnits);
            y = (double) dataSet.getDouble(index, yUnits);

            final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);
            if (isValid) {
                firstIndex = index;  // TODO: what if no valid points?
                index++;
                break;
            }
        }

        // find the last valid point, minding the dataSetSizeLimit
        int pointsPlotted = 0;
        for (index = firstIndex; index < ixmax && pointsPlotted < dataSetSizeLimit; index++) {
            y = dataSet.getDouble(index, yUnits);

            final boolean isValid = yUnits.isValid(y) && xUnits.isValid(x);

            if (isValid) {
                pointsPlotted++;
            }
        }

        if (index < ixmax && pointsPlotted == dataSetSizeLimit) {
            dataSetClipped = true;
        }

        lastIndex = index;

    }

    /**
     * do the same as updatePlotImage, but use AffineTransform to implement axis transform.
     */
    @Override
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) {

        updating = true;

        updateImageCount++;

        reportCount();

        try {
            super.updatePlotImage(xAxis, yAxis, monitor);
        } catch (DasException e) {
            // it doesn't throw DasException, but interface requires exception, jbf 5/26/2005
            throw new RuntimeException(e);
        }


        DataSet dataSet = getDataSet();

        if (dataSet == null || dataSet.getXLength() == 0) {
            return;
        }

        TableDataSet tds = null;
        VectorDataSet vds = null;
        boolean plottable = false;

        if (dataSet instanceof VectorDataSet) {
            vds = (VectorDataSet) dataSet;
            plottable = dataSet.getYUnits().isConvertableTo(yAxis.getUnits());
        } else if (dataSet instanceof TableDataSet) {
            tds = (TableDataSet) dataSet;
            plottable = tds.getZUnits().isConvertableTo(yAxis.getUnits());
        }
        plottable = plottable && dataSet.getXUnits().isConvertableTo(xAxis.getUnits());

        if (!plottable) {
            return;
        }


        logger.fine("entering updatePlotImage");
        long t0 = System.currentTimeMillis();

        dataSetClipped = false;


        if (vds != null) {
            updateFirstLast(xAxis, yAxis, vds);

            if (fillToReference) {
                fillElement.update(xAxis, yAxis, vds, monitor);
            }
            if (psymConnector != PsymConnector.NONE) {
                psymConnectorElement.update(xAxis, yAxis, vds, monitor);
            }

            errorElement.update(xAxis, yAxis, vds, monitor);
            psymsElement.update(xAxis, yAxis, vds, monitor);

        } else if (tds != null) {
            extraConnectorElements = new PsymConnectorRenderElement[tds.getYLength(0)];
            for (int i = 0; i < tds.getYLength(0); i++) {
                extraConnectorElements[i] = new PsymConnectorRenderElement();

                float[] colorHSV = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
                if (colorHSV[2] < 0.7f) {
                    colorHSV[2] = 0.7f;
                }
                if (colorHSV[1] < 0.7f) {
                    colorHSV[1] = 0.7f;
                }
                extraConnectorElements[i].color = Color.getHSBColor(i / 6.f, colorHSV[1], colorHSV[2]);
                vds = tds.getYSlice(i, 0);

                if (i == 0) {
                    updateFirstLast(xAxis, yAxis, vds); // minimal support assumes vert slice data is all valid or all invalid.
                }
                extraConnectorElements[i].update(xAxis, yAxis, vds, monitor);
            }
        }

        if (getParent() != null) {
            getParent().repaint();
        }

        logger.fine("done updatePlotImage in " + (System.currentTimeMillis() - t0) + " ms");
        updating = false;
        long milli = System.currentTimeMillis();
        long renderTime = (milli - t0);
        double dppms = (lastIndex - firstIndex) / (double) renderTime;

        setUpdatesPointsPerMillisecond(dppms);
    }

    protected void installRenderer() {
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
            DasPlot p = parent;
            mouseAdapter.addMouseModule(new MouseModule(p, new LengthDragRenderer(p, p.getXAxis(), p.getYAxis()), "Length"));
        }

        updatePsym();
    }

    protected void uninstallRenderer() {
    }

    public Element getDOMElement(Document document) {
        return null;
    }

    public javax.swing.Icon getListIcon() {
        Image i = new BufferedImage(15, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();
        g.setRenderingHints(DasProperties.getRenderingHints());
        g.setBackground(parent.getBackground());

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
        return new ImageIcon(i);
    }

    public String getListLabel() {
        return String.valueOf(this.getDataSetDescriptor());
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
            refreshImage();
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
            refreshImage();
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
            refreshImage();
            propertyChangeSupport.firePropertyChange("color", old, color);
            updatePsym();
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
            refreshImage();
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
     * Holds value of property selected.
     */
    private boolean selected;

    /**
     * Getter for property selected.
     * @return Value of property selected.
     */
    public boolean isSelected() {
        return this.selected;
    }

    /**
     * Setter for property selected.
     * @param selected New value of property selected.
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
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
            refresh();
            propertyChangeSupport.firePropertyChange("fillColor", old, color);
        }

    }
    /**
     * Holds value of property colorByDataSetId.
     */
    private String colorByDataSetId = null;

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
        refresh();
        propertyChangeSupport.firePropertyChange("colorByDataSetId", oldVal, colorByDataSetId);
    }
    /**
     * Holds value of property colorBar.
     */
    private DasColorBar colorBar;

    /**
     * Getter for property colorBar.
     * @return Value of property colorBar.
     */
    public DasColorBar getColorBar() {
        return this.colorBar;
    }

    /**
     * Setter for property colorBar.
     * @param colorBar New value of property colorBar.
     */
    public void setColorBar(DasColorBar colorBar) {
        this.colorBar = colorBar;
        colorBar.addPropertyChangeListener(new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                if (colorByDataSetId != null && !colorByDataSetId.equals("")) {
                    refresh();
                }
            }
        });
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
            refresh();
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
     * Holds value of property colorByDataSet.
     */
    private edu.uiowa.physics.pw.das.dataset.VectorDataSet colorByDataSet;

    /**
     * Getter for property colorByDataSet.
     * @return Value of property colorByDataSet.
     */
    public edu.uiowa.physics.pw.das.dataset.VectorDataSet getColorByDataSet() {
        return this.colorByDataSet;
    }

    /**
     * Setter for property colorByDataSet.
     * @param colorByDataSet New value of property colorByDataSet.
     */
    public void setColorByDataSet(edu.uiowa.physics.pw.das.dataset.VectorDataSet colorByDataSet) {
        this.colorByDataSet = colorByDataSet;
        refreshImage();
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
            refresh();
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

    @Override
    public boolean acceptContext(int x, int y) {
        boolean accept = false;

        Point2D.Double dp = new Point2D.Double(x, y);

        if ( this.fillToReference && fillElement.acceptContext(dp) ) {
            accept = true;
        }

        if ((!accept) && psymConnectorElement.acceptContext(dp)) {
            accept = true;
        }

        if ((!accept) && extraConnectorElements != null) {
            for (int j = 0; j < extraConnectorElements.length; j++) {
                if (!accept && extraConnectorElements[j]!=null && extraConnectorElements[j].acceptContext(dp)) {
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
        propertyChangeSupport.firePropertyChange("dataSetSizeLimit", new Integer(oldDataSetSizeLimit), new Integer(dataSetSizeLimit));
    }
    private double updatesPointsPerMillisecond;
    public static final String PROP_UPDATESPOINTSPERMILLISECOND = "updatesPointsPerMillisecond";

    public double getUpdatesPointsPerMillisecond() {
        return this.updatesPointsPerMillisecond;
    }

    public void setUpdatesPointsPerMillisecond(double newupdatesPointsPerMillisecond) {
        double oldupdatesPointsPerMillisecond = updatesPointsPerMillisecond;
        this.updatesPointsPerMillisecond = newupdatesPointsPerMillisecond;
        propertyChangeSupport.firePropertyChange(PROP_UPDATESPOINTSPERMILLISECOND, oldupdatesPointsPerMillisecond, newupdatesPointsPerMillisecond);
    }
    private double renderPointsPerMillisecond;
    public static final String PROP_RENDERPOINTSPERMILLISECOND = "renderPointsPerMillisecond";

    public double getRenderPointsPerMillisecond() {
        return this.renderPointsPerMillisecond;
    }

    public void setRenderPointsPerMillisecond(double newrenderPointsPerMillisecond) {
        double oldrenderPointsPerMillisecond = renderPointsPerMillisecond;
        this.renderPointsPerMillisecond = newrenderPointsPerMillisecond;
        propertyChangeSupport.firePropertyChange(PROP_RENDERPOINTSPERMILLISECOND, oldrenderPointsPerMillisecond, newrenderPointsPerMillisecond);
    }

    public int getFirstIndex() {
        return this.firstIndex;
    }

    public int getLastIndex() {
        return this.lastIndex;
    }
}
