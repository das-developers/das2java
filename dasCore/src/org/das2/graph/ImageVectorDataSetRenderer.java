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

import javax.swing.Icon;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.RebinDescriptor;
import org.virbo.dataset.DataSetUtil;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.DasException;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;
import javax.swing.ImageIcon;
import org.das2.dataset.NoDataInIntervalException;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.FDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.SemanticOps;

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
    QDataSet hist;
    private Color color = Color.BLACK;
    private int ixstepLimitSq=1000000;  /** pixels, limit of x increment before line break */
    private Datum xres= null; /** size used to set ixstepLimitSq */
    private Shape selectionArea;

    public ImageVectorDataSetRenderer(DataSetDescriptor dsd) {
        super(dsd);
    }

    @Override
    public Icon getListIcon() {
        BufferedImage i = new BufferedImage(15, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();

        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        if ( parent!=null ) g.setBackground(parent.getBackground());

        // leave transparent if not white
        if (color.equals(Color.white)) {
            g.setColor(Color.GRAY);
        } else {
            g.setColor(new Color(0, 0, 0, 0));
        }

        g.fillRect(0, 0, 15, 10);

        g.setColor(color);

        Stroke stroke0 = g.getStroke();
        g.setStroke( new BasicStroke( 0.5f ) );
        g.drawLine( 2, 3, 13, 7 );
        g.setStroke(stroke0);

        i.setRGB( 7, 5, color.getRGB() );

        return new ImageIcon(i);

    }


    public synchronized void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        if ( ds==null ) {
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        QDataSet xds = SemanticOps.xtagsDataSet(ds);
        QDataSet vds;

        if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
            vds = DataSetOps.unbundleDefaultDataSet( ds );
        } else {
            vds = (QDataSet) ds;
        }


        if ( !xAxis.getUnits().isConvertableTo( SemanticOps.getUnits((QDataSet) xds) ) ) {
            parent.postMessage(this, "inconvertible xaxis units", DasPlot.INFO, null, null);
            return;
        }

        if ( !yAxis.getUnits().isConvertableTo( SemanticOps.getUnits((QDataSet) vds) )) {
            parent.postMessage(this, "inconvertible yaxis units", DasPlot.INFO, null, null);
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
                } else if (getDataSet().length() == 0) {
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

    }

    private void ghostlyImage2(DasAxis xAxis, DasAxis yAxis, QDataSet ds, Rectangle plotImageBounds2) {
        int ny = plotImageBounds2.height;
        int nx = plotImageBounds2.width;

        logger.fine("create Image");
        BufferedImage image = new BufferedImage(nx, ny, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(color);
        g.setStroke(new BasicStroke(1.f / saturationHitCount));

        g.translate( -plotImageBounds2.x, -plotImageBounds2.y);
        
        imageXRange= GraphUtil.invTransformRange( xAxis, plotImageBounds2.x, plotImageBounds2.x+plotImageBounds2.width );
        imageYRange= GraphUtil.invTransformRange( yAxis, plotImageBounds2.y, plotImageBounds2.y+plotImageBounds2.height );

        DatumRange visibleRange = imageXRange;

        QDataSet xds = SemanticOps.xtagsDataSet(ds);
        QDataSet vds;

        if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
            vds = DataSetOps.unbundleDefaultDataSet( ds );
        } else {
            vds = (QDataSet) ds;
        }

        boolean xmono = Boolean.TRUE == SemanticOps.isMonotonic(xds);

        int firstIndex = xmono ? DataSetUtil.getPreviousIndex(xds, visibleRange.min()) : 0;
        int lastIndex = xmono ? ( DataSetUtil.getNextIndex(xds, visibleRange.max()) + 1 ) : xds.length();

        final int STATE_LINETO = -991;
        final int STATE_MOVETO = -992;

        int state = STATE_MOVETO;

        // TODO: data breaks
        int ix0 = 0, iy0 = 0;
        if (vds.length() > 0) {
            QDataSet wds = DataSetUtil.weightsDataSet(vds);
            Units dsunits= SemanticOps.getUnits(vds);
            Units xunits= SemanticOps.getUnits(xds);
            for (int i = firstIndex; i < lastIndex; i++) {
                boolean isValid = wds.value(i)>0;
                if (!isValid) {
                    state = STATE_MOVETO;
                } else {
                    int iy = (int) yAxis.transform( vds.value(i), dsunits );
                    int ix = (int) xAxis.transform( xds.value(i), xunits );
                    if ( (ix-ix0)*(ix-ix0) > ixstepLimitSq ) state=STATE_MOVETO;
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
        selectionArea= null;
        
    }

    private QDataSet histogram(RebinDescriptor ddx, RebinDescriptor ddy, QDataSet ds) {
        ddx.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        ddy.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        FDataSet tds = FDataSet.createRank2( ddx.numberOfBins(), ddy.numberOfBins() );

        if (ds.length() > 0) {

            QDataSet xds = SemanticOps.xtagsDataSet(ds);
            QDataSet vds;

            if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
                vds = DataSetOps.unbundleDefaultDataSet( ds );
            } else {
                vds = (QDataSet) ds;
            }

            QDataSet wds= SemanticOps.weightsDataSet( vds );

            Units xunits = SemanticOps.getUnits(xds);
            Units yunits = SemanticOps.getUnits(vds);

            boolean xmono = SemanticOps.isMonotonic(xds);

            int firstIndex = xmono ? DataSetUtil.getPreviousIndex(xds,  ddx.binStart(0) ) : 0;
            int lastIndex = xmono ? DataSetUtil.getNextIndex(xds, ddx.binStop(ddx.numberOfBins() - 1) ) : vds.length()-1;

            int i = firstIndex;
            int n = lastIndex;
            for (; i <= n; i++) {
                boolean isValid = wds.value(i)>0;
                if ( isValid ) {
                    int ix = ddx.whichBin(xds.value(i), xunits);
                    int iy = ddy.whichBin(vds.value(i), yunits);
                    if (ix != -1 && iy != -1) {
                        double d = tds.value(ix, iy);
                        tds.putValue( ix, iy, d+1 );
                    }
                }
            }
        }
        return tds;
    }

    private void ghostlyImage(DasAxis xAxis, DasAxis yAxis, QDataSet ds, Rectangle plotImageBounds2) {
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


        QDataSet newHist = histogram(ddx, ddy, ds);
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
        int colorInt = color.getRGB() & 0x00ffffff;
        // Following code for scatter plot temporarily removed
        // Need to add switch between scatter plot and envelope
        /*for (int i = 0; i < w; i++) {
        for (int j = 0; j < h; j++) {
        int index = (i - 0) + (h - j - 1) * w;
        // alpha=0 for transparent, alpha=255 for opaque
        int alpha = 255 * (int) newHist.getDouble(i, j, Units.dimensionless) / saturationHitCount;

        int icolor = (alpha << 24) | colorInt;
        raster[index] = icolor;
        }
        }*/

        //show envelope

        QDataSet xds= SemanticOps.xtagsDataSet( ds );
        boolean xmono = SemanticOps.isMonotonic(xds);

        int envelopeColor = ( 128 << 24) | colorInt;  // 50% alpha
        if ( envelope==1 ) envelopeColor= ( 128/saturationHitCount << 24) | colorInt;  // 50% alpha
        for (int i=0; i<w; i++) {
            int ymin = -1;
            int ymax = -1; //ymax is inclusive
            for (int j=0; j<h; j++) {
                if (newHist.value(i,j) > 0) {
                    if (ymin<0) ymin = j;
                    ymax = j;
                }
            }
            if (ymin >= 0) {
                for (int j=ymin; j<=ymax; j++) {
                     int index = i + (h-j-1) * w;
                     if ( !xmono || (!(envelope==2) && ( envelope==0 || newHist.value(i,j) > 0 ) )) {
                         int alpha = 255 * (int) newHist.value(i,j) / saturationHitCount;
                         if (alpha>255) alpha = 255;  //Clip alpha; it's only 8 bits!
                         raster[index] =  (alpha << 24) | colorInt;
                     }  else {
                        raster[index] = envelopeColor;
                     }
                }
            }
        }

        plotImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        selectionArea= null;
        WritableRaster r = plotImage.getRaster();
        r.setDataElements(0, 0, w, h, raster);

        imageXRange = xrange;
        imageYRange = yrange;
    }

    @Override
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, org.das2.util.monitor.ProgressMonitor monitor) throws DasException {

        super.updatePlotImage(xAxis, yAxis, monitor);

        QDataSet ds1 = getDataSet();
        if (ds1 == null) {
            return;
        }

        QDataSet xds= SemanticOps.xtagsDataSet( ds );

        if (!xAxis.getUnits().isConvertableTo( SemanticOps.getUnits(xds) )) {
            parent.postMessage(this, "inconvertible xaxis units", DasPlot.INFO, null, null);
            return;
        }

        if (!yAxis.getUnits().isConvertableTo( SemanticOps.getUnits(ds1) )) {
            parent.postMessage(this, "inconvertible yaxis units", DasPlot.INFO, null, null);
            return;
        }

        plotImageBounds = parent.getCacheImageBounds();
        if ( plotImageBounds==null ) {
            //transient state in parent component.  TODO: fix these
            return;
        }

        DatumRange visibleRange = xAxis.getDatumRange(); 

        boolean xmono = SemanticOps.isMonotonic(xds);

        
        int firstIndex, lastIndex;
        if ( xmono ) {
            firstIndex = DataSetUtil.getPreviousIndex(xds, visibleRange.min());
            lastIndex = DataSetUtil.getNextIndex(xds, visibleRange.max()) ;
            Datum xres1= visibleRange.width().divide(xAxis.getDLength());
            if ( xAxis.isLog() ) {
                ixstepLimitSq= 100000000;
                xres= null;
            } else {
                RankZeroDataSet d= DataSetUtil.guessCadenceNew(xds,ds1);
                if ( d!=null ) {
                    Datum sw = DataSetUtil.asDatum( d ); //TODO! check ratiometric
                    Datum xmax= xAxis.getDataMaximum();
                    int ixstepLimit= 0;
                    if ( UnitsUtil.isRatiometric(sw.getUnits())) {
                        ixstepLimit= 1 + (int) (xAxis.transform(xmax) - xAxis.transform(xmax.divide(sw)));
                    } else {
                        ixstepLimit= 1 + (int) (xAxis.transform(xmax) - xAxis.transform(xmax.subtract(sw)));
                    }
                    ixstepLimitSq= ixstepLimit * ixstepLimit;
                    xres= xres1;
                } else {
                    ixstepLimitSq= 100000000;
                }
            }
        } else {
            firstIndex = 0;
            lastIndex = ds1.length();
            ixstepLimitSq= 100000000;
            xres= null;
        }

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

    public void setColor(Color color) {
        this.color = color;
        refreshImage();
    }

    public Color getColor() {
        return color;
    }

    protected int envelope = 0;
    public static final String PROP_ENVELOPE = "envelope";

    public int getEnvelope() {
        return envelope;
    }

    /**
     * 0=none. 1=faint envelope with points on top.  2=only envelope
     * @param envelope
     */
    public void setEnvelope(int envelope) {
        int oldEnvelope = this.envelope;
        this.envelope = envelope;
        refreshImage();
        propertyChangeSupport.firePropertyChange(PROP_ENVELOPE, oldEnvelope, envelope);
        //meanCount=0;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        if ( plotImage==null ) return false;
        Shape s= selectionArea();
        if ( s.contains(x, y) ) {
            return true;
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

    /**
     * this is very challenging!
     * 
     */
    synchronized void calcSelectionArea() {
        //System.err.println("in calc selection area");
        long t0= System.currentTimeMillis();
        if ( plotImage==null ) return;
        int w= plotImage.getWidth();
        int h= plotImage.getHeight();
        int imagex = (int)parent.getCacheImageBounds().getX(); //TODO: there is a bug here, with vap+cdaweb:ds=THE_L2_FBK&id=the_fb_hff&timerange=2007-02-24 I noticed it didn't line up when overrendering is on.
        int imagey = (int)parent.getCacheImageBounds().getY();
        GeneralPath result= new GeneralPath();
        for ( int i=0; i<w; i+=5 ) {
            for ( int j=0; j<h; j+=5 ) {
                int n=0;
                int x= 0;
                int y= 0;
                for ( int ii=0; ii<10; ii++ ) {
                    for ( int jj=0; jj<10; jj++ ) {
                        if ( i+ii<w && j+jj<h ) {
                            if ( ( plotImage.getRGB(i+ii,j+jj) & 0xff000000 ) != 0 ) {
                                n++;
                                x+= ii;
                                y+= jj;
                            }
                        }
                    }
                }
                if ( n>0 ) {
                    result.append( new Rectangle( (int)( i+((float)x)/n+imagex-5 ), (int)( j+((float)y)/n+imagey-5 ), 10, 10 ), true );
                }
            }
        }
        selectionArea= result;
        //System.err.println("done in calc selection area " + ( System.currentTimeMillis()-t0) + "ms");
    }

    Shape selectionArea() {
        if ( selectionArea==null ) {
            calcSelectionArea();
        }
        return selectionArea;
    }
}
