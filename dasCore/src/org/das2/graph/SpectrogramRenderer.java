/* File: SpectrogramRenderer.java
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
import org.das2.event.DasMouseInputAdapter;
import org.das2.event.MouseModule;
import org.das2.event.HorizontalSlicerMouseModule;
import org.das2.event.HorizontalDragRangeSelectorMouseModule;
import org.das2.event.VerticalSlicerMouseModule;
import org.das2.event.CrossHairMouseModule;
import org.das2.components.propertyeditor.Enumeration;
import org.das2.dataset.DataSetRebinner;
import org.das2.dataset.NoDataInIntervalException;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.AverageTableRebinner;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.RebinDescriptor;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.components.HorizontalSpectrogramSlicer;
import org.das2.components.VerticalSpectrogramAverager;
import org.das2.components.VerticalSpectrogramSlicer;
import org.das2.datum.DatumRange;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.*;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import org.das2.datum.Datum;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;

/**
 * Renderer for spectrograms.  A setting for rebinning data controls how data is binned into pixel space,
 * for example by nearest neighbor or interpolation.
 * @author  jbf
 */
public class SpectrogramRenderer extends Renderer implements TableDataSetConsumer, org.das2.components.propertyeditor.Displayable {

    final private Object lockObject = new Object();
    private DasColorBar colorBar;
    private Image plotImage;
    /**
     * bounds of the image, in the canvas frame.  Note that this can be bigger than the canvas itsself, for example if overrendering is on.
     */
    private Rectangle plotImageBounds;
    
    private byte[] raster;
    private int rasterWidth,  rasterHeight;
    private int validCount;
    
    DatumRange imageXRange;
    DatumRange imageYRange;
    DasAxis.Memento xmemento, ymemento, cmemento;
    int updateImageCount = 0, renderCount = 0;
    private QDataSet rebinDataSet;  // simpleTableDataSet at pixel resolution

    private String xrangeWarning= null; // if non-null, print out of bounds warning.

    boolean unitsWarning= false; // true indicates we've warned the user that we're ignoring units.
    
    protected class RebinListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            update();
//            refreshImage();
        }
    }
    RebinListener rebinListener = new RebinListener();

    /** Holds value of property rebinner. */
    private RebinnerEnum rebinnerEnum;

    public static class RebinnerEnum implements Enumeration {

        DataSetRebinner rebinner;
        String label;

        public RebinnerEnum(DataSetRebinner rebinner, String label) {
            this.rebinner = rebinner;
            this.label = label;
        }
        public static final RebinnerEnum binAverage = new RebinnerEnum(new AverageTableRebinner(), "binAverage");
        //public static final RebinnerEnum nearestNeighbor = new RebinnerEnum(new NearestNeighborTableRebinner(), "nearestNeighbor");
        public static final RebinnerEnum nearestNeighbor;
        public static final RebinnerEnum binAverageNoInterpolate;
        public static final RebinnerEnum binAverageNoInterpolateNoEnlarge;
        

        static {
            AverageTableRebinner rebinner = new AverageTableRebinner();
            rebinner.setInterpolate(false);
            binAverageNoInterpolate = new RebinnerEnum(rebinner, "noInterpolate");

            rebinner = new AverageTableRebinner();
            rebinner.setInterpolate(false);
            rebinner.setEnlargePixels(false);
            binAverageNoInterpolateNoEnlarge = new RebinnerEnum(rebinner, "noInterpolateNoEnlarge");
            
            rebinner = new AverageTableRebinner();
            rebinner.setInterpolateType( AverageTableRebinner.Interpolate.NearestNeighbor );
            nearestNeighbor = new RebinnerEnum(rebinner, "nearestNeighbor");
        }

        /*public static final RebinnerEnum binAverage= new RebinnerEnum(new AverageTableRebinner(),"binAverage");
        public static final RebinnerEnum nearestNeighbor;
        public static final RebinnerEnum binAverageNoInterpolate= new RebinnerEnum(new AverageNoInterpolateTableRebinner(),"noInterpolate");
        public static final RebinnerEnum binAverageNoInterpolateNoEnlarge;
        static {
        AverageNoInterpolateTableRebinner rebin= new AverageNoInterpolateTableRebinner();
        rebin.setNearestNeighbor(true);
        nearestNeighbor= new RebinnerEnum(rebin, "nearestNeighbor");
        AverageTableRebinner rebinner;
        rebinner = new AverageTableRebinner();
        rebinner.setInterpolate(false);
        rebinner.setEnlargePixels(false);
        binAverageNoInterpolateNoEnlarge = new RebinnerEnum(rebinner, "noInterpolateNoEnlarge");
        }*/
        public Icon getListIcon() {
            return new ImageIcon(SpectrogramRenderer.class.getResource("/images/icons/rebin." + label + ".png"));
        }

        public String toString() {
            return this.label;
        }

        DataSetRebinner getRebinner() {
            return this.rebinner;
        }
    }

    public SpectrogramRenderer(DataSetDescriptor dsd, DasColorBar colorBar) {
        super(dsd);
        this.colorBar = colorBar;
        if (this.colorBar != null) {
            colorBar.addPropertyChangeListener("dataMinimum", rebinListener);
            colorBar.addPropertyChangeListener("dataMaximum", rebinListener);
            colorBar.addPropertyChangeListener("log", rebinListener);
            colorBar.addPropertyChangeListener(DasColorBar.PROPERTY_TYPE, rebinListener);
            colorBar.addPropertyChangeListener(DasColorBar.PROPERTY_FILL_COLOR, rebinListener);
        }
        setRebinner(SpectrogramRenderer.RebinnerEnum.binAverage);
    }

    /** Creates a new instance of SpectrogramRenderer
     * @deprecated use {link
     * #SpectrogramRenderer(org.das2.dataset.DataSetDescriptor,
     * org.das2.graph.DasColorBar)}
     */
    public SpectrogramRenderer(DasPlot parent, DataSetDescriptor dsd, DasColorBar colorBar) {
        this(dsd, colorBar);
        this.parent = parent;
    }

    public DasAxis getZAxis() {
        return colorBar; //.getAxis();
    }

    public DasColorBar getColorBar() {
        return colorBar;
    }

    public void setColorBar(DasColorBar cb) {
        if (colorBar == cb) {
            return;
        }
        if (colorBar != null) {
            colorBar.removePropertyChangeListener("dataMinimum", rebinListener);
            colorBar.removePropertyChangeListener("dataMaximum", rebinListener);
            colorBar.removePropertyChangeListener("log", rebinListener);
            colorBar.removePropertyChangeListener(DasColorBar.PROPERTY_TYPE, rebinListener);
            colorBar.removePropertyChangeListener(DasColorBar.PROPERTY_FILL_COLOR, rebinListener);
            if (parent != null && parent.getCanvas() != null) {
                parent.getCanvas().remove(colorBar);
            }
        }
        colorBar = cb;
        if (colorBar != null) {
            colorBar.addPropertyChangeListener("dataMinimum", rebinListener);
            colorBar.addPropertyChangeListener("dataMaximum", rebinListener);
            colorBar.addPropertyChangeListener("log", rebinListener);
            colorBar.addPropertyChangeListener(DasColorBar.PROPERTY_TYPE, rebinListener);
            colorBar.addPropertyChangeListener(DasColorBar.PROPERTY_FILL_COLOR, rebinListener);
            if (parent != null && parent.getCanvas() != null) {
                parent.getCanvas().add(colorBar);
            }
        }
    }

    private QDataSet bounds( QDataSet ds ) {
        return DataSetOps.dependBounds(ds);
    }

    public synchronized void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        logger.finer("entering SpectrogramRenderer.render");
        Graphics2D g2 = (Graphics2D) g;

        if ( parent==null ) return;
        
        renderCount++;
        reportCount();
        synchronized (lockObject) {
            if (plotImage == null) {
                if (lastException != null) {
                    if (lastException instanceof NoDataInIntervalException) {
                        parent.postMessage(this, "no data in interval:!c" + lastException.getMessage(), DasPlot.WARNING, null, null);
                    } else {
                        parent.postException(this, lastException);
                    }
                } else {
                    QDataSet zds= getDataSet();
                    QDataSet xds=null, yds=null;

                    if ( zds==null ) {
                        parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
                    } else {
                        if ( zds.rank()==2 ) {
                            xds= SemanticOps.xtagsDataSet(zds);
                            yds= SemanticOps.ytagsDataSet(zds);
                        } else if ( zds.rank()==3 ) {
                            xds= SemanticOps.xtagsDataSet(zds.slice(0));
                            yds= SemanticOps.ytagsDataSet(zds.slice(0));
                        }

                        if ( getDataSet().length() == 0 ) {
                            parent.postMessage(this, "empty data set", DasPlot.INFO, null, null);

                        } else {
                            boolean xunitsOkay= SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits()) ;
                            boolean yunitsOkay= SemanticOps.getUnits(yds).isConvertableTo(yAxis.getUnits());
                            String message= null;
                            if ( !xunitsOkay && !yunitsOkay ) {
                                message= "xaxis and yaxis units";
                            } else  if ( !xunitsOkay ) {
                                message= "xaxis units";
                            } else if ( !yunitsOkay ) {
                                message= "yaxis units";
                            }
                            if ( message!=null ) {
                                parent.postMessage(this, "inconvertible "+message, DasPlot.INFO, null, null);
                            }
                            if ( !SemanticOps.isTableDataSet( zds ) ) {
                                if ( !SemanticOps.isBundle( zds ) ) {
                                    parent.postMessage(this, "expected table dataset", DasPlot.INFO, null, null );
                                    return;
                                } else {
                                    zds= SemanticOps.getDependentDataSet(zds);
                                }
                            }
                        }
                    }
                }
            } else if (plotImage != null) {
                if ( unitsWarning ) {
                    QDataSet zds= getDataSet();
                    QDataSet xds=null, yds=null;
                    if ( zds.rank()==2 ) {
                        xds= SemanticOps.xtagsDataSet(zds);
                        yds= SemanticOps.ytagsDataSet(zds);
                    } else if ( zds.rank()==3 ) {
                        xds= SemanticOps.xtagsDataSet(zds.slice(0));
                        yds= SemanticOps.ytagsDataSet(zds.slice(0));
                    } else {
                        throw new IllegalArgumentException("only rank 2 and rank 3 supported");
                    }
                    if ( ! SemanticOps.getUnits(yds).isConvertableTo(yAxis.getUnits()) ) {
                        parent.postMessage( this, "yaxis units changed from \""+SemanticOps.getUnits(yds)+"\" to \"" + yAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                    }
                    if ( ! SemanticOps.getUnits(xds).isConvertableTo(xAxis.getUnits()) ) {
                        parent.postMessage( this, "xaxis units changed from \""+SemanticOps.getUnits(xds)+"\" to \"" + xAxis.getUnits() + "\"", DasPlot.INFO, null, null );
                    }
                    if ( ! SemanticOps.getUnits(zds).isConvertableTo(colorBar.getUnits()) ) {
                        parent.postMessage( this, "zaxis units changed from \""+SemanticOps.getUnits(zds)+"\" to \"" + colorBar.getUnits() + "\"", DasPlot.INFO, null, null );
                    }
                }
                
                Point2D p;
                p = new Point2D.Float( plotImageBounds.x, plotImageBounds.y );
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
                if ( validCount==0 ) {
                    QDataSet bounds= bounds(ds);
                    DatumRange xdr= org.virbo.dataset.DataSetUtil.asDatumRange( bounds.slice(0), true );
                    DatumRange ydr= org.virbo.dataset.DataSetUtil.asDatumRange( bounds.slice(1), true );
                    if ( xAxis.getDatumRange().intersects(xdr) && yAxis.getDatumRange().intersects(ydr) ) {
                        parent.postMessage(this, "dataset contains no valid data", DasPlot.INFO, null, null );
                    } else {
                        parent.postMessage(this, "dataset is outside of axis range", DasPlot.INFO, null, null );
                    }
                }

                if ( xrangeWarning!=null ) {
                    parent.postMessage(this, "no data in interval:!c" + xrangeWarning, DasPlot.WARNING, null, null);
                }

            }
        }
    }
    
    int count = 0;
    private boolean sliceRebinnedData = true;

    /**
     * make the pixel array, possibly recycling the old map.
     * @param rebinData
     * @param pix
     * @return
     */
    private static byte[] makePixMap( QDataSet rebinData, byte[] pix ) {
        logger.fine("converting to pixel map");
        int ny = rebinData.length(0);
        int nx = rebinData.length();

        pix = new byte[nx * ny];
        return pix;
    }

    /**
     * transforms the simpleTableDataSet into a Raster byte array.  The rows of
     * the table are adjacent in the output byte array.
     */
    private static int transformSimpleTableDataSet( QDataSet rebinData, DasColorBar cb, boolean flipY, byte[] pix ) {

        if ( rebinData.rank()!=2 ) throw new IllegalArgumentException("rank 2 expected");

        logger.fine("converting to pixel map");

        int ny = rebinData.length(0);
        int nx = rebinData.length();
        int icolor;

        Units units = SemanticOps.getUnits(rebinData);
        if ( !units.isConvertableTo( cb.getUnits() ) ) {
            // we'll print a warning later
            units= cb.getUnits();
        }

        QDataSet wds = SemanticOps.weightsDataSet( rebinData );

        Arrays.fill(pix, (byte) cb.getFillColorIndex());

        int validCount= 0;

        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                if ( wds.value( i, j ) > 0.) {
                    int index;
                    if (flipY) {
                        index = i + j * nx;
                    } else {
                        index = (i - 0) + (ny - j - 1) * nx;
                    }
                    icolor = cb.indexColorTransform( rebinData.value(i,j), units );
                    pix[index] = (byte) icolor;
                    validCount++;
                }
            }
        }
        if ( validCount==0 ) {
            logger.fine("dataset contains no valid data");
        }

        return validCount;
    }

    private void reportCount() {
        if (updateImageCount % 10 == 0) {
            //System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );    
        }

    }

    private static RebinDescriptor convertUnitsTo( RebinDescriptor in, Units newUnits ) {
        RebinDescriptor result= new RebinDescriptor(
                                newUnits.createDatum(in.binStarts()[0]),
                                newUnits.createDatum(in.binStops()[in.numberOfBins()-1]),
                                in.numberOfBins(), in.isLog() );
        return result;
    }

    /**
     * compare the two datums, possibly ignoring units if the two datums are ratio measurements.
     * @param a
     * @param b
     * @return
     */
    private int compare( Datum a, Datum b ) {
        if ( a.getUnits().isConvertableTo(b.getUnits()) ) {
            return a.compareTo(b);
        } else {
            if ( UnitsUtil.isRatioMeasurement(a.getUnits()) && UnitsUtil.isRatioMeasurement(b.getUnits())) {
                return Double.compare( a.value(), b.value() );
            } else {
                throw new InconvertibleUnitsException(a.getUnits(),b.getUnits());
            }
        }
    }

    public synchronized void updatePlotImage( DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor ) throws DasException {
        logger.finer("entering SpectrogramRenderer.updatePlotImage");
        updateImageCount++;
        reportCount();
        DasPlot lparent= getParent();

        xrangeWarning= null;
        
        if (lparent==null ) return;
        
        final QDataSet fds= this.ds; // make a local copy for thread safety.

        byte[] lraster= this.raster;  // make a local copy for thread safety.

        try {
            try {

                BufferedImage plotImage2;  // index color model

                Units xunits=null, yunits=null;

                if ( fds!=null && fds.length()>0 ) {
                    if ( fds.rank()==2 ) {
                        xunits= SemanticOps.getUnits( SemanticOps.xtagsDataSet(fds) );
                        if ( SemanticOps.isJoin(fds) ) {//TODO: vap+junorpw:file:///media/mini/ct/pw/juno/oct2010/test_2010_10_26T20.lrs.ucal
                            yunits= SemanticOps.getUnits( SemanticOps.xtagsDataSet(fds.slice(0)) );
                        } else {
                            yunits= SemanticOps.getUnits( SemanticOps.ytagsDataSet(fds) );
                        }
                    } else {
                        xunits= SemanticOps.getUnits( SemanticOps.xtagsDataSet(fds.slice(0)) );
                        yunits= SemanticOps.getUnits( SemanticOps.ytagsDataSet(fds.slice(0)) );
                    }
                }

                synchronized (lockObject) {

                    Rectangle plotImageBounds2= lparent.getUpdateImageBounds();

                    if ( lraster != null
                            && xmemento != null && ymemento != null 
                            && xAxis.getMemento().equals(xmemento)                             
                            && yAxis.getMemento().equals(ymemento) 
                            && colorBar.getMemento().equals(cmemento)
                            && plotImageBounds2.width==rasterWidth  // TODO: figure out how plotImageBounds2 and xmemento get out of sync.
                            && plotImageBounds2.height==rasterHeight ) {
                        logger.fine("same xaxis, yaxis, reusing raster");
                    } else {

                        if ( plotImageBounds2==null || plotImageBounds2.width <= 1 || plotImageBounds2.height <= 1) {
                            logger.finest("canvas not useable!!!");
                            return;
                        }

                        if (fds == null) {
                            logger.fine("got null dataset, setting image to null");
                            plotImage = null;
                            plotImageBounds= null;
                            rebinDataSet = null;
                            imageXRange = null;
                            imageYRange = null;
                            lparent.repaint();
                            return;

                        }

                        if (fds.length() == 0) {
                            logger.fine("got empty dataset, setting image to null");
                            plotImage = null;
                            plotImageBounds= null;
                            rebinDataSet = null;
                            imageXRange = null;
                            imageYRange = null;
                            lparent.repaint();
                            return;
                        }

                        if ( fds.rank()==2 ) {
                            xunits= SemanticOps.getUnits( SemanticOps.xtagsDataSet(fds) );
                            if ( SemanticOps.isJoin(fds) ) {//TODO: vap+junorpw:file:///media/mini/ct/pw/juno/oct2010/test_2010_10_26T20.lrs.ucal
                                yunits= SemanticOps.getUnits( SemanticOps.xtagsDataSet(fds.slice(0)) );
                            } else {
                                yunits= SemanticOps.getUnits( SemanticOps.ytagsDataSet(fds) );
                            }
                        } else {
                            xunits= SemanticOps.getUnits( SemanticOps.xtagsDataSet(fds.slice(0)) );
                            yunits= SemanticOps.getUnits( SemanticOps.ytagsDataSet(fds.slice(0)) );
                        }

                        unitsWarning= false;
                        if ( !xunits.isConvertableTo(xAxis.getUnits()) ) {
                            if ( UnitsUtil.isRatioMeasurement( xunits ) && UnitsUtil.isRatioMeasurement( xAxis.getUnits() ) ) {
                                unitsWarning= true;
                            } else {
                                logger.fine("dataset units are incompatable with x axis.");
                                plotImage = null;
                                plotImageBounds= null;
                                return;
                            }
                        }


                        if ( !yunits.isConvertableTo(yAxis.getUnits()) ) {
                             if ( UnitsUtil.isRatioMeasurement( yunits ) && UnitsUtil.isRatioMeasurement( yAxis.getUnits() ) ) {
                                unitsWarning= true;
                            } else {
                                logger.fine("dataset units are incompatable with y axis.");
                                plotImage = null;
                                plotImageBounds= null;
                                return;
                            }
                        }

                        if ( !( SemanticOps.isTableDataSet(fds) ) ) {
                            logger.fine("dataset is not TableDataSet.");
                            plotImage = null;
                            plotImageBounds= null;
                            return;
                        }

                        boolean plottable = false;
                        plottable = SemanticOps.getUnits(fds).isConvertableTo(colorBar.getUnits());
                        if ( !plottable ) {
                            if ( UnitsUtil.isRatioMeasurement( SemanticOps.getUnits(fds) ) && UnitsUtil.isRatioMeasurement( colorBar.getUnits() ) ) {
                                plottable= true; // we'll provide a warning
                                unitsWarning= true;
                            }
                        }
                        
                        RebinDescriptor xRebinDescriptor;
                        xRebinDescriptor = new RebinDescriptor(
                                xAxis.invTransform(plotImageBounds2.x),
                                xAxis.invTransform(plotImageBounds2.x+plotImageBounds2.width),
                                plotImageBounds2.width,
                                xAxis.isLog());

                        RebinDescriptor yRebinDescriptor = new RebinDescriptor(
                                yAxis.invTransform(plotImageBounds2.y+plotImageBounds2.height),
                                yAxis.invTransform(plotImageBounds2.y),
                                plotImageBounds2.height,
                                yAxis.isLog());

                        if ( !xunits.isConvertableTo(xAxis.getUnits()) ) {
                            xRebinDescriptor= convertUnitsTo(xRebinDescriptor, xunits);
                        }

                        if ( !yunits.isConvertableTo(yAxis.getUnits()) ) {
                            yRebinDescriptor= convertUnitsTo(yRebinDescriptor, yunits);
                        }

                        imageXRange = xAxis.getDatumRange();
                        imageYRange = yAxis.getDatumRange();

                        logger.log(Level.FINE, "rebinning to pixel resolution: {0}  {1}", new Object[]{xRebinDescriptor, yRebinDescriptor});
                        logger.log(Level.FINE, "rebinning to pixel resolution: {0}", plotImageBounds2);

                        DataSetRebinner rebinner = this.rebinnerEnum.getRebinner();

                        //long t0;

                        //t0 = System.currentTimeMillis();

                        QDataSet bounds= bounds(fds);

                        Datum start = Datum.create(  bounds.value(0,0), xunits );
                        Datum end = Datum.create( bounds.value(0,1), xunits );
                        if ( compare( start, imageXRange.max() )> 0 ) {
                            xrangeWarning= "data starts after range";
                        } else if ( compare( end, imageXRange.min() ) < 0 ) {
                            xrangeWarning= "data ends before range";
                        }
                        //t0= System.currentTimeMillis();
                        try {
                            rebinDataSet = (QDataSet) rebinner.rebin( fds, xRebinDescriptor, yRebinDescriptor );
                        } catch ( RuntimeException ex ) {
                            ex.printStackTrace(); //TODO: catch this...  See sftp://jbf@papco.org/home/jbf/ct/autoplot/script/bugs/3237397/gapsTest.jy
                            return;
                        }
                        //System.err.println( "rebin (ms): " + ( System.currentTimeMillis()-t0) );
                        xmemento = xAxis.getMemento();
                        ymemento = yAxis.getMemento();
                        cmemento = colorBar.getMemento();

                        logger.log(Level.FINE, "rebinning to pixel resolution: {0}  {1}", new Object[]{xmemento, ymemento});

                        lraster = makePixMap( rebinDataSet, lraster );

                        //t0= System.currentTimeMillis();
                        try {
                            validCount= transformSimpleTableDataSet(rebinDataSet, colorBar, yAxis.isFlipped(), lraster );
                        } catch ( InconvertibleUnitsException ex ) {
                            System.err.println("zunits="+ SemanticOps.getUnits(fds)+"  colorbar="+colorBar.getUnits() );
                            return;
                        }
                        //System.err.println( "transformSimpleTable (ms): " + ( System.currentTimeMillis()-t0) );

                        rasterWidth = plotImageBounds2.width;
                        rasterHeight = plotImageBounds2.height;
                        raster= lraster;
                    }

                    IndexColorModel model = colorBar.getIndexColorModel();
                    plotImage2 = new BufferedImage(plotImageBounds2.width, plotImageBounds2.height, BufferedImage.TYPE_BYTE_INDEXED, model);

                    WritableRaster r = plotImage2.getRaster();

                    try {
                        if ( plotImageBounds2.width==rasterWidth && plotImageBounds2.height==rasterHeight ) {
                            try {
                                r.setDataElements(0, 0, rasterWidth, rasterHeight, lraster);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.err.println("avoided raster ArrayIndex... track this down sometime...");
                        }
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        throw ex;
                    }
                    plotImage = plotImage2;
                    plotImageBounds= plotImageBounds2;
                    raster= lraster;

                    Rectangle rr= DasDevicePosition.toRectangle( parent.getRow(), parent.getColumn() );

                    if ( fds!=null ) {
                        QDataSet bounds= bounds(fds);
                        DatumRange xdr= org.virbo.dataset.DataSetUtil.asDatumRange( bounds.slice(0), true );
                        DatumRange ydr= org.virbo.dataset.DataSetUtil.asDatumRange( bounds.slice(1), true );
                        if ( xunits!=null && !xunits.isConvertableTo(xAxis.getUnits()) ) {
                            xdr= new DatumRange( xdr.min().doubleValue(xdr.getUnits()), xdr.max().doubleValue(xdr.getUnits()),xAxis.getUnits() );
                        }
                        if ( yunits!=null && !yunits.isConvertableTo(yAxis.getUnits()) ) {
                            ydr= new DatumRange( ydr.min().doubleValue(ydr.getUnits()), ydr.max().doubleValue(ydr.getUnits()),yAxis.getUnits() );
                        }
                        double[] yy= GraphUtil.transformRange( yAxis, ydr );
                        double[] xx= GraphUtil.transformRange( xAxis, xdr );
                        selectionArea= rr.intersection( new Rectangle( (int)xx[0], (int)yy[0], (int)(xx[1]-xx[0]), (int)(yy[1]-yy[0]) ) );
                    }

                } //synchronized (lockObject)
            } catch (InconvertibleUnitsException ex) {
                logger.fine("inconvertible units, setting image to null");
                ex.printStackTrace();
                plotImage = null;
                plotImageBounds= null;
                rebinDataSet = null;
                imageXRange = null;
                imageYRange = null;
                if (this.getLastException() == null) setException(ex);
                lparent.repaint();
                return;
            }

        } catch (NullPointerException ex) {
            ex.printStackTrace();
        } catch (NoDataInIntervalException e) {
            lastException = e;
            plotImage = null;
        } finally {
            lparent.repaint();
        }
    }

    protected void installRenderer() {
        if (parent != null && parent.getCanvas() != null) {
            if (colorBar != null) {
                if (colorBar.getColumn() == DasColumn.NULL) {
                    DasColumn column = parent.getColumn();
                    colorBar.setColumn(new DasColumn(null, column, 1.0, 1.0, 1, 2, 0, 0));
                }
                parent.getCanvas().add(colorBar, parent.getRow(), colorBar.getColumn());
                if (!"true".equals(DasApplication.getProperty("java.awt.headless", "false"))) {
                    DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
                    VerticalSpectrogramSlicer vSlicer =
                            VerticalSpectrogramSlicer.createSlicer(parent, this);
                    VerticalSlicerMouseModule vsl = VerticalSlicerMouseModule.create(this);
                    vsl.addDataPointSelectionListener(vSlicer);
                    mouseAdapter.addMouseModule(vsl);

                    HorizontalSpectrogramSlicer hSlicer = HorizontalSpectrogramSlicer.createSlicer(parent, this);
                    HorizontalSlicerMouseModule hsl = HorizontalSlicerMouseModule.create(this);
                    hsl.addDataPointSelectionListener(hSlicer);
                    mouseAdapter.addMouseModule(hsl);

                    VerticalSpectrogramAverager vAverager = VerticalSpectrogramAverager.createAverager(parent, this);
                    HorizontalDragRangeSelectorMouseModule vrl = new HorizontalDragRangeSelectorMouseModule(parent, this, parent.getXAxis());
                    //vrl.setLabel("Vertical Averager");
                    vrl.addDataRangeSelectionListener(vAverager);
                    mouseAdapter.addMouseModule(vrl);

                    MouseModule ch = new CrossHairMouseModule(parent, this, parent.getXAxis(), parent.getYAxis());
                    mouseAdapter.addMouseModule(ch);

                }
            }
        }
    }

    protected void uninstallRenderer() {
//        if (colorBar != null && colorBar.getCanvas() != null) {
//            // count the number of Renderers pointing to the colorbar.  Remove it if it's the only one.
//            DasCanvas c= colorBar.getCanvas();
//            boolean othersUse= false;
//            for ( DasCanvasComponent cc: c.getCanvasComponents() ) {
//                if ( cc instanceof DasPlot ) {
//                    Renderer[] rr= ((DasPlot)cc).getRenderers();
//                    for ( Renderer r1: rr ) {
//                        if ( r1 instanceof SpectrogramRenderer && r1!=this ) {
//                            if ( ((SpectrogramRenderer)r1).getColorBar()==colorBar ) {
//                                othersUse= true;
//                            }
//                        }
//                    }
//                }
//            }
//            if ( !othersUse ) {
//                colorBar.getCanvas().remove(colorBar);
//            }
//        }

        if (!"true".equals(DasApplication.getProperty("java.awt.headless", "false"))) {
            //TODO: remove slicers.  Note if two spectrograms, then we'll have problems.
            //DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
            //mouseAdapter.removeMouseModule( mouseAdapter.getModuleByLabel("Vertical Slice") );
            //mouseAdapter.removeMouseModule( mouseAdapter.getModuleByLabel("Horizontal Slice") );
            //mouseAdapter.removeMouseModule( mouseAdapter.getModuleByLabel("Horizontal Drag Range") );

        }
    }


    /** Getter for property rebinner.
     * @return Value of property rebinner.
     *
     */
    public RebinnerEnum getRebinner() {
        return this.rebinnerEnum;
    }

    /** Setter for property rebinner.
     * @param rebinnerEnum New value of property rebinner.
     *
     */
    public void setRebinner(RebinnerEnum rebinnerEnum) {
        RebinnerEnum old = this.rebinnerEnum;
        if (old != rebinnerEnum) {
            this.rebinnerEnum = rebinnerEnum;
            this.raster = null;
            this.plotImage = null;
            refreshImage();
            propertyChangeSupport.firePropertyChange("rebinner", old, rebinnerEnum);
        }
    }

    /** Getter for property sliceRebinnedData.
     * @return Value of property sliceRebinnedData.
     *
     */
    public boolean isSliceRebinnedData() {
        return this.sliceRebinnedData;
    }

    /** Setter for property sliceRebinnedData.
     * @param sliceRebinnedData New value of property sliceRebinnedData.
     *
     */
    public void setSliceRebinnedData(boolean sliceRebinnedData) {
        this.sliceRebinnedData = sliceRebinnedData;
    }

    public String getListLabel() {
        return "spectrogram";
    }

    public Icon getListIcon() {
        return rebinnerEnum.getListIcon();
    }

    public QDataSet getConsumedDataSet() {
        if (sliceRebinnedData) {
            return rebinDataSet;
        } else {
            return this.ds;
        }
    }

    public void setDataSet(QDataSet ds) {
        QDataSet oldDs = this.ds;
        if (parent != null && oldDs != ds) {
            this.raster = null;
            // TODO: preserve plotImage until updatePlotImage is done
            this.plotImage = null;
        }
        super.setDataSet(ds);
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

    private Shape selectionArea=null;
    /**
     * return the shape or null.
     * @return
     */
    public Shape selectionArea() {
        return selectionArea;
    }

    public boolean acceptContext(int x, int y) {
        return selectionArea!=null && selectionArea.contains(x, y);
    }
    /**
     * Holds value of property fillColor.
     */
    private Color fillColor = Color.GRAY;

    /**
     * Getter for property fillColor.
     * @return Value of property fillColor.
     */
    public Color getFillColor() {
        return this.fillColor;
    }

    /**
     * Setter for property fillColor.
     * @param fillColor New value of property fillColor.
     */
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }
}
