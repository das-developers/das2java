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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import javax.swing.Icon;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.RebinDescriptor;
import org.virbo.dataset.DataSetUtil;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.datum.Datum;
import org.das2.DasException;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import org.das2.dataset.NoDataInIntervalException;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import static org.das2.graph.Renderer.logger;
import org.virbo.dataset.AbstractDataSet;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.FDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.RankZeroDataSet;
import org.virbo.dataset.SemanticOps;
import org.virbo.dataset.WeightsDataSet;
import org.virbo.dsops.Ops;

/**
 *
 * @author Jeremy
 */
public class ImageVectorDataSetRenderer extends Renderer {

    BufferedImage plotImage;
    Rectangle plotImageBounds;
    DatumRange imageXRange;
    private Color color = Color.BLACK;
    private int ixstepLimitSq=1000000;  /** pixels, limit of x increment before line break */

    /**
     * pixels, limit of x increment before line break
     */
    private Shape selectionArea;

    public ImageVectorDataSetRenderer(DataSetDescriptor dsd) {
        super(dsd);
    }

    @Override
    public Icon getListIcon() {
        BufferedImage i = new BufferedImage(15, 10, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) i.getGraphics();

        DasPlot parent= getParent();
        
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

    //TODO: other Renderers use similar code and probably don't handle all-fill properly.
    private static QDataSet doRange( QDataSet xds ) {
        QDataSet xrange= Ops.extent(xds);
        if ( xrange.value(1)==xrange.value(0) ) {
            QDataSet wds=  WeightsDataSet.applyRules( xds,xrange );
            if ( wds.value(0)*wds.value(1) ==0 ) {
                xrange= DDataSet.createRank1Bins( 0, 10, SemanticOps.getUnits(xrange) );
                return xrange;
            } else {
                if ( !"log".equals( xrange.property(QDataSet.SCALE_TYPE)) ) {
                    Units xunits= SemanticOps.getUnits(xrange);
                    if ( UnitsUtil.isTimeLocation(xunits) ) {
                        Datum dx= Units.nanoseconds.createDatum(1000).convertTo(xunits.getOffsetUnits());
                        xrange= DDataSet.createRank1Bins( xrange.value(0)-dx.value(),xrange.value(1)+dx.value(), xunits);
                    } else {
                        xrange= DDataSet.createRank1Bins( xrange.value(0)-1, xrange.value(1)+1, xunits );
                    }
                } else {
                    xrange= DDataSet.createRank1Bins(  xrange.value(0)/10, xrange.value(1)*10, SemanticOps.getUnits(xrange) );
                }
            }
        }
        boolean isLog= "log".equals( xrange.property(QDataSet.SCALE_TYPE));
        if ( isLog && xrange.value(0)==0 ) {
            //xrange= Ops.rescaleRangeLogLin( xrange, -0.1, 1.1 );
            //xrange= Ops.rescaleRange( xrange, 0, 1.1 );
        } else {
            xrange= Ops.rescaleRangeLogLin( xrange, -0.1, 1.1 );
        }
        return xrange;
    }

    private static QDataSet fastRank2Range( QDataSet ds ) {
        double min= Double.POSITIVE_INFINITY;
        double max= Double.NEGATIVE_INFINITY;
        QDataSet wds= DataSetUtil.weightsDataSet(ds);
        for ( int i=0; i<ds.length(); i++ ) {
            int n= ds.length(i);
            for ( int j=0; j<n; j++ ) {
                double w= wds.value(i,j);
                if ( w>0 ) {
                    double d= ds.value(i,j);
                    min= d < min ? d : min;
                    max= d > max ? d : max;
                }
            }
        }
        Units u= SemanticOps.getUnits(ds);
        DDataSet result= DDataSet.createRank1(2);
        result.putValue( 0, min );
        result.putValue( 1, max );
        result.putProperty( QDataSet.UNITS, u );
        return Ops.rescaleRangeLogLin( result, -0.1, 1.1 );
    }
    
    public static QDataSet doAutorange( QDataSet ds ) {

        QDataSet xrange;
        QDataSet yrange;

        QDataSet xds= SemanticOps.xtagsDataSet(ds);

        if ( SemanticOps.isRank2Waveform(ds) ) {
            yrange= fastRank2Range(ds);
            QDataSet offsrange= doRange( (QDataSet) ds.property(QDataSet.DEPEND_1) );
            xrange= doRange( xds );
            xrange= Ops.add( xrange, offsrange );
            
        } else if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
            QDataSet vds= DataSetOps.unbundleDefaultDataSet( ds );
            yrange= doRange( vds );
            xrange= doRange( xds );
        } else {
            yrange= doRange( ds );
            xrange= doRange( xds );
        }
        

        JoinDataSet bds= new JoinDataSet(2);
        bds.join(xrange);
        bds.join(yrange);

        return bds;

    }

    @Override
    public synchronized void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {

        long t0= System.currentTimeMillis();

        logger.fine("entering ImageVectorDataSetRenderer.render");
        
        DasPlot parent= getParent();
        
        if ( ds==null ) {
            parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
            return;
        }

        QDataSet xds = SemanticOps.xtagsDataSet(ds);
        
        Units yunits;
        if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
            QDataSet vds = DataSetOps.unbundleDefaultDataSet( ds );
            yunits= SemanticOps.getUnits( vds );
        } else {
            QDataSet vds = ds;
            yunits= SemanticOps.getUnits( vds );
        }

        if ( !xAxis.getUnits().isConvertibleTo( SemanticOps.getUnits((QDataSet) xds) ) ) {
            parent.postMessage(this, "inconvertible xaxis units", DasPlot.INFO, null, null);
        }

        if ( !yAxis.getUnits().isConvertibleTo( yunits ) ) {
            parent.postMessage(this, "inconvertible yaxis units", DasPlot.INFO, null, null);
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
            int x = (int) (p.getX());  
            int y = (int) (p.getY());
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

        logger.log(Level.FINE, "done ImageVectorDataSetRenderer.render {0} ms", ( System.currentTimeMillis()-t0 ));

    }

    /**
     * render each point with a 1-pixel dot and line connecting.
     * @param xAxis the xAxis
     * @param yAxis the yAxis
     * @param ds rank 2 bundle or rank 1 dataset.
     * @param plotImageBounds2 the bounds 
     */
    private void renderPointsOfRank1(DasAxis xAxis, DasAxis yAxis, QDataSet ds, Rectangle plotImageBounds2) {
        int ny = plotImageBounds2.height;
        int nx = plotImageBounds2.width;

        logger.log(Level.FINE, "create Image (renderPointsOfRank1): nx={0} ny={1}", new Object[]{nx, ny});

        BufferedImage image = new BufferedImage(nx, ny, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(color);
        g.setStroke(new BasicStroke(.15f / saturationHitCount));

        g.translate( -plotImageBounds2.x, -plotImageBounds2.y);

        imageXRange= GraphUtil.invTransformRange( xAxis, plotImageBounds2.x, plotImageBounds2.x+plotImageBounds2.width );

        DatumRange visibleRange = imageXRange;

        QDataSet xds = SemanticOps.xtagsDataSet(ds);
        QDataSet vds;

        if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
            vds= DataSetOps.unbundleDefaultDataSet( ds );
        } else {
            vds= ds;
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
            if ( !dsunits.isConvertibleTo(yAxis.getUnits()) ) {
                dsunits= yAxis.getUnits();
            }
            if ( !xunits.isConvertibleTo(xAxis.getUnits() ) ) {
                xunits= xAxis.getUnits();
            }
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
        synchronized (this) {
            plotImage = image;
            selectionArea= null;
        }

    }


    /**
     * Super-fast implementation for rank 2 waveform dataset, where DEPEND_1 is the offset from DEPEND_0.
     * Each point is painted, so this assumes this can be done quickly.
     * 
     * @param xAxis the x-axis
     * @param yAxis the y-axis
     * @param ds a rank 2 waveform dataset
     * @param plotImageBounds2 the boundaries.
     */
    private void renderPointsOfRank2Waveform(DasAxis xAxis, DasAxis yAxis, QDataSet ds, Rectangle plotImageBounds2) {
        int ny = plotImageBounds2.height;
        int nx = plotImageBounds2.width;

        logger.log(Level.FINE, "create Image (ghostlyImageRank2): nx={0} ny={1}", new Object[]{nx, ny});
        BufferedImage image = new BufferedImage(nx, ny, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(color);
        g.setStroke(new BasicStroke(1.f / saturationHitCount));

        g.translate( -plotImageBounds2.x, -plotImageBounds2.y);
        
        imageXRange= GraphUtil.invTransformRange( xAxis, plotImageBounds2.x, plotImageBounds2.x+plotImageBounds2.width );

        DatumRange visibleRange = imageXRange;

        QDataSet xds = SemanticOps.xtagsDataSet(ds);

        boolean xmono = Boolean.TRUE == SemanticOps.isMonotonic(xds);

        int firstIndex = xmono ? DataSetUtil.getPreviousIndex(xds, visibleRange.min()) : 0;
        int lastIndex = xmono ? ( DataSetUtil.getNextIndex(xds, visibleRange.max()) + 1 ) : xds.length();

        final int STATE_LINETO = -991;
        final int STATE_MOVETO = -992;

        int state = STATE_MOVETO;

        int ix0 = 0, iy0 = 0;
        if (xds.length() > 0) {
            QDataSet wds = DataSetUtil.weightsDataSet(ds);
            Units dsunits= SemanticOps.getUnits(ds);
            Units xunits= SemanticOps.getUnits(xds);
            
            if ( !dsunits.isConvertibleTo(yAxis.getUnits()) ) {
                dsunits= yAxis.getUnits();
            }
            if ( !xunits.isConvertibleTo(xAxis.getUnits() ) ) {
                xunits= xAxis.getUnits();
            }

            ArrayDataSet xoffsets= ArrayDataSet.copy( ( QDataSet) ds.property(QDataSet.DEPEND_1) );
            if ( xoffsets.rank()>1 ) throw new IllegalArgumentException("rank 2 DEPEND_1 not supported");
            final UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(xoffsets), SemanticOps.getUnits(xds).getOffsetUnits() );

            if ( uc!=UnitsConverter.IDENTITY ) { // RPW Group has packets with 208896 elements, so this is non-trivial.
                for ( int j=0; j<xoffsets.length(); j++ ) {
                    xoffsets.putValue( j, uc.convert( xoffsets.value(j) ) );
                }
            }
            
            int xmin= xAxis.getColumn().getDMinimum()-xAxis.getColumn().getWidth();
            int xmax= xAxis.getColumn().getDMaximum()+xAxis.getColumn().getWidth();
            
            for (int i = firstIndex; i < lastIndex; i++) {
                int nj= ds.length(i);

                
                for ( int j=0; j<nj; j++ ) {
                    boolean isValid = wds.value(i,j)>0;
                    if (!isValid) {
                        state = STATE_MOVETO;
                    } else {
                        int iy = (int) yAxis.transform( ds.value(i,j), dsunits );
                        int ix = (int) xAxis.transform( xds.value(i) + xoffsets.value(j), xunits );
                        if ( (ix-ix0)*(ix-ix0) > ixstepLimitSq ) state=STATE_MOVETO;
                        switch (state) {
                            case STATE_MOVETO:
                                if ( ix>xmin && ix<xmax ) {
                                    g.fillRect(ix, iy, 1, 1);
                                }
                                state= STATE_LINETO;
                                ix0 = ix;
                                iy0 = iy;
                                break;
                            case STATE_LINETO:
                                if ( ix>xmin && ix<xmax ) {
                                    g.draw(new Line2D.Float(ix0, iy0, ix, iy));
                                    g.fillRect(ix, iy, 1, 1);
                                }
                                ix0 = ix;
                                iy0 = iy;
                                break;
                        }
                    }
                }
            }
        }

        logger.fine("done");
        synchronized (this) {
            plotImage = image;
            selectionArea= null;
        }
        
    }

    private QDataSet histogram(RebinDescriptor ddx, RebinDescriptor ddy, QDataSet ds) {
        logger.fine("histogram");
        ddx.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        ddy.setOutOfBoundsAction(RebinDescriptor.MINUSONE);
        FDataSet tds = FDataSet.createRank2( ddx.numberOfBins(), ddy.numberOfBins() );

        if (ds.length() > 0) {

            QDataSet xds = SemanticOps.xtagsDataSet(ds);
            QDataSet vds;
            ArrayDataSet xoffsets= null;

            boolean isWaveform= false;
            if ( ds.rank()==2 && SemanticOps.isBundle(ds) ) {
                vds = DataSetOps.unbundleDefaultDataSet( ds );
            } else if ( SemanticOps.isRank2Waveform(ds) ) {
                vds= ds;
                xds = SemanticOps.xtagsDataSet(ds);
                isWaveform= true;
                xoffsets= ArrayDataSet.copy( ( QDataSet) ds.property(QDataSet.DEPEND_1) );
                if ( xoffsets.rank()>1 ) throw new IllegalArgumentException("rank 2 DEPEND_1 not supported");
                final UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(xoffsets), SemanticOps.getUnits(xds).getOffsetUnits() );
                if ( !uc.equals( UnitsConverter.IDENTITY ) ) {
                    for ( int j=0; j<xoffsets.length(); j++ ) {
                        xoffsets.putValue( j, uc.convert( xoffsets.value(j) ) );
                    }
                }
            } else {
                vds = (QDataSet) ds;
            }

            QDataSet wds= SemanticOps.weightsDataSet( vds );

            Units xunits = SemanticOps.getUnits(xds);
            Units yunits = SemanticOps.getUnits(vds);

            if ( !yunits.isConvertibleTo(ddy.getUnits()) ) {
                yunits= ddy.getUnits();
            }
            if ( !xunits.isConvertibleTo(ddx.getUnits() ) ) {
                xunits= ddx.getUnits();
            }
            boolean xmono = SemanticOps.isMonotonic(xds);

            int firstIndex = xmono ? DataSetUtil.getPreviousIndex(xds,  ddx.binStart(0) ) : 0;
            int lastIndex = xmono ? DataSetUtil.getNextIndex(xds, ddx.binStop(ddx.numberOfBins() - 1) ) : vds.length()-1;

            int i = firstIndex;
            int n = lastIndex;
            int nj= isWaveform ? vds.length(i) : 1;

            if ( isWaveform ) {
                int ix0= ddx.whichBin( xds.value(i) + xoffsets.value(0), xunits );
                int ix1= ddx.whichBin( xds.value(i) + xoffsets.value(xoffsets.length()-1), xunits );
                if ( ix0==-1 && i+1<n ) {
                    ix0= ddx.whichBin( xds.value(i+1) + xoffsets.value(0), xunits );
                    ix1= ddx.whichBin( xds.value(i+1) + xoffsets.value(xoffsets.length()-1), xunits );
                }
                
                boolean wowReduce= ix0!=-1 && ix0==ix1;

                if ( wowReduce ) {
                    logger.fine("wowReduce");
                    for (; i <= n; i++) {
                        int ix = ddx.whichBin( xds.value(i), xunits );
                        for ( int j=0; j<nj; j++ ) {
                            boolean isValid = wds.value(i,j)>0;
                            if ( isValid ) {
                                int iy = ddy.whichBin( vds.value(i,j), yunits );
                                if (ix != -1 && iy != -1) {
                                    double d = tds.value(ix, iy);
                                    tds.putValue( ix, iy, d+1 );
                                }
                            }
                        }
                    }
                } else {
                    Units targetXUnits= ddx.getUnits();
                    UnitsConverter xuc= xunits.getConverter(targetXUnits);
                    Units targetYUnits= ddy.getUnits();
                    UnitsConverter yuc= yunits.getConverter(targetYUnits);
                    for (; i <= n; i++) {
                        for ( int j=0; j<nj; j++ ) {
                            boolean isValid = wds.value(i,j)>0;
                            if ( isValid ) {
                                int ix = ddx.whichBin( xuc.convert( xds.value(i) + xoffsets.value(j) ), targetXUnits );
                                int iy = ddy.whichBin( yuc.convert( vds.value(i,j) ), targetYUnits );
                                if (ix != -1 && iy != -1) {
                                    double d = tds.value(ix, iy);
                                    tds.putValue( ix, iy, d+1 );
                                }
                            }
                        }
                    }
                }
            } else {
                Units targetXUnits= ddx.getUnits();
                UnitsConverter xuc= xunits.getConverter(targetXUnits);
                Units targetYUnits= ddy.getUnits();
                UnitsConverter yuc= yunits.getConverter(targetYUnits);                
                for (; i <= n; i++) {
                    boolean isValid = wds.value(i)>0;
                    if ( isValid ) {
                        int ix = ddx.whichBin( xuc.convert( xds.value(i) ), targetXUnits);
                        int iy = ddy.whichBin( yuc.convert( vds.value(i) ), targetYUnits);
                        if (ix != -1 && iy != -1) {
                            double d = tds.value(ix, iy);
                            tds.putValue( ix, iy, d+1 );
                        }
                    }
                }
            }
        }
        return tds;
    }

    /**
     * This is the typical route, where we are making a 2-D histogram of the data in pixel space, and
     * using that with the saturation count to shade each pixel.
     * @param xAxis the x-axis
     * @param yAxis the y-axis
     * @param ds the rank 2 waveform dataset, or rank 2 bundle, or rank 1 dataset.
     * @param plotImageBounds2 the bounds.
     */
    private void renderHistogram(DasAxis xAxis, DasAxis yAxis, QDataSet ds, Rectangle plotImageBounds2) {
        DatumRange xrange = GraphUtil.invTransformRange( xAxis, plotImageBounds2.x, plotImageBounds2.x + plotImageBounds2.width);
        DatumRange yrange = GraphUtil.invTransformRange( yAxis, plotImageBounds2.y + plotImageBounds2.height,plotImageBounds2.y);

        RebinDescriptor ddx = new RebinDescriptor(
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
        if ( yAxis.isFlipped() && xAxis.isFlipped() ) {
            newHist= DataSetOps.applyIndex( newHist, 1, Ops.linspace( newHist.length(0)-1, 0, newHist.length(0) ), false );
            newHist= DataSetOps.applyIndex( newHist, 0, Ops.linspace( newHist.length()-1, 0, newHist.length() ), false );
        } else if ( yAxis.isFlipped() ) {
            newHist= DataSetOps.applyIndex( newHist, 1, Ops.linspace( newHist.length(0)-1, 0, newHist.length(0) ), false );
        } else if ( xAxis.isFlipped() ) {
            newHist= DataSetOps.applyIndex( newHist, 0, Ops.linspace( newHist.length()-1, 0, newHist.length() ), false );
        }
        
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

        logger.log(Level.FINE, "ghostlyImage: h={0} w={1}", new Object[]{h, w});

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

        BufferedImage plotImage1 = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        selectionArea= null;
        WritableRaster r = plotImage1.getRaster();
        r.setDataElements(0, 0, w, h, raster);

        synchronized (this) {
            plotImage= plotImage1;
        }
        imageXRange = xrange;
    }

    @Override
    public synchronized void updatePlotImage(DasAxis xAxis, DasAxis yAxis, org.das2.util.monitor.ProgressMonitor monitor) throws DasException {

        //System.err.println("enter updatePlotImage");
        long t0= System.currentTimeMillis();

        super.incrementUpdateCount();

        logger.fine("entering ImageVectorDataSetRenderer.updatePlotImage");
        
        super.updatePlotImage(xAxis, yAxis, monitor);

        QDataSet ds1 = getDataSet();
        if (ds1 == null) {
            return;
        }

        QDataSet xds= SemanticOps.xtagsDataSet( ds );
        
        DasPlot parent= getParent();
        if ( parent==null ) return;

        if (!xAxis.getUnits().isConvertibleTo( SemanticOps.getUnits(xds) )) {
            parent.postMessage(this, "inconvertible xaxis units", DasPlot.INFO, null, null);
        }

        if (!yAxis.getUnits().isConvertibleTo( SemanticOps.getUnits(ds1) )) {
            parent.postMessage(this, "inconvertible yaxis units", DasPlot.INFO, null, null);
        }

        plotImageBounds= parent.getUpdateImageBounds();
        //plotImageBounds = parent.getCacheImageBounds();
        if ( plotImageBounds==null ) {
            //transient state in parent component.  TODO: fix these
            return;
        }

        DatumRange visibleRange = xAxis.getDatumRange(); 

        boolean xmono = SemanticOps.isMonotonic(xds);

        
        int firstIndex, lastIndex;
        if ( xmono ) {
            try {
                firstIndex = DataSetUtil.getPreviousIndex(xds, visibleRange.min());
                lastIndex = DataSetUtil.getNextIndex(xds, visibleRange.max()) ;
            } catch ( InconvertibleUnitsException ex ) {
                parent.postMessage(this, "inconvertible xaxis units", DasPlot.INFO, null, null );                
                return;
            }
            if ( xAxis.isLog() ) {
                ixstepLimitSq= 100000000;
            } else {
                RankZeroDataSet d;
                if ( SemanticOps.isRank2Waveform(ds1) ) {
                    d= DataSetUtil.guessCadenceNew( (QDataSet) ds1.property(QDataSet.DEPEND_1), null );
                } else {
                    d= DataSetUtil.guessCadenceNew(xds,ds1);
                }
                if ( d!=null ) {
                    Datum sw = DataSetUtil.asDatum( d ); //TODO! check ratiometric
                    Datum xmax= xAxis.getDataMaximum();
                    int ixstepLimit;
                    if ( UnitsUtil.isRatiometric(sw.getUnits())) {
                        ixstepLimit= 1 + (int) (xAxis.transform(xmax) - xAxis.transform(xmax.divide(sw)));
                    } else {
                        ixstepLimit= 1 + (int) (xAxis.transform(xmax) - xAxis.transform(xmax.subtract(sw)));
                    }
                    ixstepLimitSq= ixstepLimit * ixstepLimit;
                } else {
                    ixstepLimitSq= 100000000;
                }
            }
        } else {
            firstIndex = 0;
            lastIndex = ds1.length();
            ixstepLimitSq= 100000000;
        }

        int nj= 1;
        if ( SemanticOps.isRank2Waveform(ds1) ) nj= ds1.length(0);

        if ( nj*(lastIndex-firstIndex) > 20 * xAxis.getColumn().getWidth()) {
            if ( lastIndex==firstIndex+1 ) {
                renderPointsOfRank2Waveform(xAxis, yAxis, ds1, plotImageBounds);
            } else {
                renderHistogram(xAxis, yAxis, ds1, plotImageBounds);
            }
        } else {
            if ( SemanticOps.isRank2Waveform(ds1) ) {
                renderPointsOfRank2Waveform(xAxis, yAxis, ds1, plotImageBounds);
            } else {
                if ( ds.rank()==1 || ( ds.rank()==2 && SemanticOps.isBundle(ds) ) ) {
                    renderPointsOfRank1(xAxis, yAxis, ds1, plotImageBounds);
                } else {
                    parent.postMessage(this, "dataset must be rank 1, rank 2 waveform, or rank 2 bundle", DasPlot.INFO, null, null);
                }
            }
        }

        logger.log(Level.FINE, "done updatePlotImage {0} ms", ( System.currentTimeMillis()-t0 ));

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
        updateCacheImage();
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
        updateCacheImage();
        propertyChangeSupport.firePropertyChange(PROP_ENVELOPE, oldEnvelope, envelope);
        //meanCount=0;
    }

    @Override
    public boolean acceptContext(int x, int y) {
        BufferedImage im= getPlotImage();
        if ( im==null ) return false;
        Shape s= selectionArea();
        return s.contains(x, y);
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

    private synchronized BufferedImage getPlotImage() {
        return this.plotImage;
    }
    
    /**
     * calculate the area that describes roughly where the data lie.  The
     * variable "selectionArea" is set.
     *
     * This is fast, less than 50ms with 5 million points.  When the image gets big, this gets slow...
     */
    private void calcSelectionArea() {
        BufferedImage lplotImage= getPlotImage(); // make local copy
        logger.finer("in calc selection area");
        long t0= System.currentTimeMillis();
        if ( lplotImage==null ) return;
        int w= lplotImage.getWidth();
        int h= lplotImage.getHeight();
        DasPlot parent= getParent();
        int imagex = (int)parent.getCacheImageBounds().getX();
        int parentx= parent.getX();
        int dx= parent.getColumn().getDMinimum() - imagex;
        int parenty = (int)parent.getCacheImageBounds().getY();
        int overx= imagex - parentx;
        GeneralPath result= new GeneralPath();
        int d= 5;
        int dd= 5;
        if ( w*h>100000 ) {
            d= 10;
            dd= 10; // this is used to evaluate mouse click focus as well.
        }
        if ( w*h>500000 ) d= 30;

        for ( int i=0; i<w; i+=d ) {
            for ( int j=0; j<h; j+=d ) {
                int n=0;
                int x= 0;
                int y= 0;
                for ( int ii=0; ii<d; ii++ ) {
                    for ( int jj=0; jj<d; jj++ ) {
                        if ( i+ii<w && j+jj<h ) {
                            if ( ( lplotImage.getRGB(i+ii,j+jj) & 0xff000000 ) != 0 ) {
                                n++;
                                x+= ii;
                                y+= jj;
                            }
                        }
                    }
                }
                if ( n>0 ) {
                    // why 2*overx?  I have no idea...
                    result.append( new Rectangle( (int)( 2* overx + i+((float)x)/n+parentx-dd/2 + dx ), (int)( j+((float)y)/n+parenty-dd/2 ), dd, dd ), true );
                }
            }
        }
        selectionArea= result;
        logger.log(Level.FINER, "done in calc selection area {0}ms", ( System.currentTimeMillis()-t0));
    }

    /**
     * return a Shape object showing where the data lie and focus should be accepted.
     * @return
     */
    Shape selectionArea() {
        if ( selectionArea==null ) {
            calcSelectionArea();
        }
        return selectionArea==null ? SelectionUtil.NULL : selectionArea;
    }
}
