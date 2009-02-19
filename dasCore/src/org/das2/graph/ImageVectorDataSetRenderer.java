/*
 * ImageVectorDataSetRenderer.java
 *
 * This renderer can handle vector data sets with tens of thousands of points
 * by histogramming the points and then creating a greyscale spectrogram of
 * the histogram.  The property "saturationHitCount" defines the number of pixel
 * hits that will make the pixel black.  In the future, this may be modified to
 * support color, alpha channel, and connected psyms.
 *
 * Created on April 14, 2005, 8:45 PM
 */
package org.das2.graph;

import org.das2.dataset.WritableTableDataSet;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.RebinDescriptor;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.DasException;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import org.das2.dataset.DataSet;
import org.das2.dataset.NoDataInIntervalException;

/**
 *
 * @author Jeremy
 */
public class ImageVectorDataSetRenderer extends Renderer {

    GeneralPath path;
    //SymbolLineRenderer highResRenderer;
    Datum xTagWidth;
    BufferedImage plotImage;
    Rectangle plotImageBounds;
    DatumRange imageXRange;
    DatumRange imageYRange;
    TableDataSet hist;

    /** Creates a new instance of LotsaPointsRenderer */
    public ImageVectorDataSetRenderer(DataSetDescriptor dsd) {
        super(dsd);
    }

    private void renderGhostly(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis) {
        Graphics2D g2 = (Graphics2D) g1.create();

        if (getDataSet() == null && lastException != null) {
            renderException(g2, xAxis, yAxis, lastException);
        } else if (plotImage != null) {
            Point2D p;

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            p = new Point2D.Double(xAxis.transform(imageXRange.min()), yAxis.transform(imageYRange.max()));

            g2.drawImage(plotImage, (int) (p.getX() + 0.5), (int) (p.getY() + 0.5), getParent());

        }
        g2.dispose();
    }

    public synchronized void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        if ( ds==null ) {
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }
        if (!xAxis.getUnits().isConvertableTo(ds.getXUnits())) {
            parent.postMessage(this, "inconvertable xaxis units", DasPlot.INFO, null, null);
            return;
        }

        if (!yAxis.getUnits().isConvertableTo(ds.getYUnits())) {
            parent.postMessage(this, "inconvertable yaxis units", DasPlot.INFO, null, null);
            return;
        }
        Graphics2D g2 = (Graphics2D) g1;
        if (plotImage == null) {
            if (lastException != null) {
                if (lastException instanceof NoDataInIntervalException) {
                    parent.postMessage(this, "no data in interval:!c" + lastException.getMessage(), DasPlot.WARNING, null, null);
                } else {
                    parent.postException(this, lastException);
                }
            } else {
                if (getDataSet() == null) {
                    parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
                } else if (getDataSet().getXLength() == 0) {
                    parent.postMessage(this, "empty data set", DasPlot.INFO, null, null);
                }
            }
        } else if (plotImage != null) {
            Point2D p;
            p = new Point2D.Float(plotImageBounds.x, plotImageBounds.y);
            int x = (int) (p.getX() + 0.5);
            int y = (int) (p.getY() + 0.5);
            if (parent.getCanvas().isPrintingThread() && print300dpi) {
                AffineTransformOp atop = new AffineTransformOp(AffineTransform.getScaleInstance(4, 4), AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                BufferedImage image300 = atop.filter((BufferedImage) plotImage, null);
                AffineTransform atinv;
                try {
                    atinv = atop.getTransform().createInverse();
                } catch (NoninvertibleTransformException ex) {
                    throw new RuntimeException(ex);
                }
                atinv.translate(x * 4, y * 4);
                g2.drawImage(image300, atinv, getParent());
            } else {
                g2.drawImage(plotImage, x, y, getParent());
            }
        }

        //renderGhostly(g1, xAxis, yAxis);
    }

    private void ghostlyImage2(DasAxis xAxis, DasAxis yAxis, VectorDataSet ds, Rectangle plotImageBounds2) {
        int ny = plotImageBounds2.height;
        int nx = plotImageBounds2.width;

        logger.fine("create Image");
        BufferedImage image = new BufferedImage(nx, ny, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.black);
        g.setStroke(new BasicStroke(1.f / saturationHitCount));

        g.translate( -plotImageBounds2.x, -plotImageBounds2.y);

        
        
        imageXRange= GraphUtil.invTransformRange( xAxis, plotImageBounds2.x, plotImageBounds2.x+plotImageBounds2.width );
        imageYRange= GraphUtil.invTransformRange( yAxis, plotImageBounds2.y, plotImageBounds2.y+plotImageBounds2.height );

        DatumRange visibleRange = imageXRange;

        //if ( isOverloading() ) visibleRange= visibleRange.rescale(-1,2);

        boolean xmono = Boolean.TRUE == ds.getProperty(DataSet.PROPERTY_X_MONOTONIC);

        int firstIndex = xmono ? DataSetUtil.getPreviousColumn(ds, visibleRange.min()) : 0;
        int lastIndex = xmono ? DataSetUtil.getNextColumn(ds, visibleRange.max()) : ds.getXLength();

        final int STATE_LINETO = -991;
        final int STATE_MOVETO = -992;

        int state = STATE_MOVETO;

        // TODO: data breaks
        int ix0 = 0, iy0 = 0;
        if (ds.getXLength() > 0) {
            for (int i = firstIndex; i <= lastIndex; i++) {
                if (ds.getDatum(i).isFill()) {
                    state = STATE_MOVETO;
                } else {
                    int iy = (int) yAxis.transform(ds.getDatum(i));
                    int ix = (int) xAxis.transform(ds.getXTagDatum(i));
                    switch (state) {
                        case STATE_MOVETO:
                            g.fillRect(ix, iy, 1, 1);
                            ix0 = ix;
                            iy0 = iy;
                            break;
                        case STATE_LINETO:
                            g.draw(new Line2D.Float(ix0, iy0, ix, iy));
                            g.fillRect(ix, iy, 1, 1);
                            ix0 = ix;
                            iy0 = iy;
                            break;
                    }
                    state = STATE_LINETO;
                }
            }
        }

        logger.fine("done");
        plotImage = image;
        
    }

    private TableDataSet histogram(RebinDescriptor ddx, RebinDescriptor ddy, VectorDataSet ds) {
        ddx.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        ddy.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        WritableTableDataSet tds = WritableTableDataSet.newSimple(ddx.numberOfBins(), ddx.getUnits(),
                ddy.numberOfBins(), ddy.getUnits(), Units.dimensionless);

        if (ds.getXLength() > 0) {
            Units xunits = ddx.getUnits();
            Units yunits = ddy.getUnits();
            Units zunits = Units.dimensionless;

            int i = DataSetUtil.getPreviousColumn(ds, ddx.binStart(0));
            int n = DataSetUtil.getNextColumn(ds, ddx.binStop(ddx.numberOfBins() - 1));
            for (; i <= n; i++) {
                int ix = ddx.whichBin(ds.getXTagDouble(i, xunits), xunits);
                int iy = ddy.whichBin(ds.getDouble(i, yunits), yunits);
                if (ix != -1 && iy != -1) {
                    double d = tds.getDouble(ix, iy, zunits);
                    tds.setDouble(ix, iy, d + 1, zunits);
                }
            }
        }
        return tds;
    }

    private void ghostlyImage(DasAxis xAxis, DasAxis yAxis, VectorDataSet ds, Rectangle plotImageBounds2) {
        RebinDescriptor ddx;

        DatumRange xrange = new DatumRange(xAxis.invTransform(plotImageBounds2.x),
                xAxis.invTransform(plotImageBounds2.x + plotImageBounds2.width));
        DatumRange yrange = new DatumRange(yAxis.invTransform(plotImageBounds2.y + plotImageBounds2.height),
                yAxis.invTransform(plotImageBounds2.y));

        ddx = new RebinDescriptor(
                xrange.min(),
                xrange.max(),
                plotImageBounds2.width,
                xAxis.isLog());

        RebinDescriptor ddy = new RebinDescriptor(
                yrange.min(),
                yrange.max(),
                plotImageBounds2.height,
                yAxis.isLog());


        TableDataSet hist = histogram(ddx, ddy, ds);
        //WritableTableDataSet whist= (WritableTableDataSet)hist;

        /* double histMax= TableUtil.tableMax(hist, Units.dimensionless);
        for ( int i=0; i<whist.getXLength(); i++ ) {
        for ( int j=0, n=whist.getYLength(0); j<n; j++ ) {
        double d= whist.getDouble( i, j, Units.dimensionless );
        if ( d > 0 && d < histMax*floorFactor )
        whist.setDouble( i,j, histMax*floorFactor, Units.dimensionless );
        }
        }  */


        int h = ddy.numberOfBins();
        int w = ddx.numberOfBins();

        int[] raster = new int[h * w];

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int index = (i - 0) + (h - j - 1) * w;
                int alpha = 255 - (256 * (int) hist.getDouble(i, j, Units.dimensionless) / saturationHitCount);
                if (alpha < 0) {
                    alpha = 0;
                }

                int icolor = (alpha << 16) + (alpha << 8) + alpha;
                raster[index] = icolor;
            }
        }

        plotImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        WritableRaster r = plotImage.getRaster();
        r.setDataElements(0, 0, w, h, raster);

        imageXRange = xrange;
        imageYRange = yrange;
    }

    @Override
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, org.das2.util.monitor.ProgressMonitor monitor) throws DasException {
        super.updatePlotImage(xAxis, yAxis, monitor);

        long t0 = System.currentTimeMillis();

        VectorDataSet ds1 = (VectorDataSet) getDataSet();
        if (ds1 == null) {
            return;
        }
        if (!xAxis.getUnits().isConvertableTo(ds1.getXUnits())) {
            parent.postMessage(this, "inconvertable xaxis units", DasPlot.INFO, null, null);
            return;
        }

        if (!yAxis.getUnits().isConvertableTo(ds1.getYUnits())) {
            parent.postMessage(this, "inconvertable yaxis units", DasPlot.INFO, null, null);
            return;
        }

        plotImageBounds = parent.getCacheImageBounds();

        DatumRange visibleRange = xAxis.getDatumRange();

        boolean xmono = Boolean.TRUE == ds1.getProperty(DataSet.PROPERTY_X_MONOTONIC);

        int firstIndex = xmono ? DataSetUtil.getPreviousColumn(ds1, visibleRange.min()) : 0;
        int lastIndex = xmono ? DataSetUtil.getNextColumn(ds1, visibleRange.max()) : ds1.getXLength();

        if ((lastIndex - firstIndex) > 20 * xAxis.getColumn().getWidth()) {
            logger.fine("rendering with histogram");
            ghostlyImage(xAxis, yAxis, ds1, plotImageBounds);
        } else {
            logger.fine("rendinging with lines");
            ghostlyImage2(xAxis, yAxis, ds1, plotImageBounds);
        }
        logger.fine("done updatePlotImage");

    }
    int saturationHitCount = 5;

    public void setSaturationHitCount(int d) {
        if (d > 10) {
            d = 10;
        }
        this.saturationHitCount = d;
        this.update();
    }

    public int getSaturationHitCount() {
        return this.saturationHitCount;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        int i0 = Math.max(x - 2, 0);
        int j0 = Math.max(y - 2, 0);
        int i1 = Math.min(x + 3, plotImage.getWidth());
        int j1 = Math.min(y + 3, plotImage.getHeight());
        for (int i = i0; i < i1; i++) {
            for (int j = j0; j < j1; j++) {
                if (this.plotImage.getRGB(i, j) < 0xffffff) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * Holds value of property print300dpi.
     */
    private boolean print300dpi;

    /**
     * Getter for property draw300dpi.
     * @return Value of property draw300dpi.
     */
    public boolean isPrint300dpi() {
        return this.print300dpi;
    }

    /**
     * Setter for property draw300dpi.
     * @param print300dpi New value of property draw300dpi.
     */
    public void setPrint300dpi(boolean print300dpi) {
        this.print300dpi = print300dpi;
    }
}
