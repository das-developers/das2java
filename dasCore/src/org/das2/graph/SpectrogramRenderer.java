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
import org.das2.dataset.TableDataSet;
import org.das2.dataset.RebinDescriptor;
import org.das2.dataset.DataSet;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.components.HorizontalSpectrogramSlicer;
import org.das2.components.VerticalSpectrogramAverager;
import org.das2.components.VerticalSpectrogramSlicer;
import org.das2.datum.DatumRange;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.system.DasLogger;
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
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.das2.dataset.DataSetUtil;

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
    private TableDataSet rebinDataSet;  // simpleTableDataSet at pixel resolution

    protected class RebinListener implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent e) {
            update();
            refreshImage();
        }
    }
    RebinListener rebinListener = new RebinListener();
    private static Logger logger = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
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
                    if (getDataSet() == null) {
                        parent.postMessage(this, "no data set", DasPlot.INFO, null, null);
                    } else if (getDataSet().getXLength() == 0) {
                        parent.postMessage(this, "empty data set", DasPlot.INFO, null, null);
                    } else {
                        TableDataSet ds= (TableDataSet)getDataSet();
                        if ( !ds.getZUnits().isConvertableTo(colorBar.getUnits()) ) {
                            parent.postMessage(this, "inconvertible colorbar units", DasPlot.INFO, null, null);
                        }
                        if ( !ds.getYUnits().isConvertableTo(yAxis.getUnits()) ) {
                            parent.postMessage(this, "inconvertible yaxis units", DasPlot.INFO, null, null);
                        }
                        if ( !ds.getXUnits().isConvertableTo(xAxis.getUnits()) ) {
                            parent.postMessage(this, "inconvertible xaxis units", DasPlot.INFO, null, null);
                        }
                    }
                }
            } else if (plotImage != null) {
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
                    DatumRange xdr= DataSetUtil.xRange(ds);
                    DatumRange ydr= DataSetUtil.yRange(ds);
                    if ( xAxis.getDatumRange().intersects(xdr) && yAxis.getDatumRange().intersects(ydr) ) {
                        parent.postMessage(this, "dataset contains no valid data", DasPlot.INFO, null, null );
                    } else {
                        parent.postMessage(this, "dataset is outside of axis range", DasPlot.INFO, null, null );
                    }
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
    private static byte[] makePixMap( TableDataSet rebinData, byte[] pix ) {
        logger.fine("converting to pixel map");
        //TableDataSet weights= (TableDataSet)rebinData.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        int itable = 0;
        int ny = rebinData.getYLength(itable);
        int nx = rebinData.tableEnd(itable) - rebinData.tableStart(itable);

        pix = new byte[nx * ny];
        return pix;
    }

    /**
     * transforms the simpleTableDataSet into a Raster byte array.  The rows of
     * the table are adjacent in the output byte array.
     */
    private static int transformSimpleTableDataSet( TableDataSet rebinData, DasColorBar cb, boolean flipY, byte[] pix ) {

        if (rebinData.tableCount() > 1) {
            throw new IllegalArgumentException("TableDataSet contains more than one table");
        }
        logger.fine("converting to pixel map");
        //TableDataSet weights= (TableDataSet)rebinData.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        int itable = 0;
        int ny = rebinData.getYLength(itable);
        int nx = rebinData.tableEnd(itable) - rebinData.tableStart(itable);
        int icolor;

        Units units = cb.getUnits();

        TableDataSet weights = (TableDataSet) rebinData.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);

        Arrays.fill(pix, (byte) cb.getFillColorIndex());

        int validCount= 0;

        for (int i = rebinData.tableStart(itable); i < rebinData.tableEnd(itable); i++) {
            for (int j = 0; j < ny; j++) {
                if (weights == null || weights.getDouble(i, j, Units.dimensionless) > 0.) {
                    int index;
                    if (flipY) {
                        index = i + j * nx;
                    } else {
                        index = (i - 0) + (ny - j - 1) * nx;
                    }
                    icolor = cb.indexColorTransform(rebinData.getDouble(i, j, units), units);
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

    public synchronized void updatePlotImage( DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor ) throws DasException {
        logger.finer("entering SpectrogramRenderer.updatePlotImage");
        updateImageCount++;
        reportCount();
        DasPlot lparent= getParent();

        if (lparent==null ) return;
        
        final DataSet fds= this.ds; // make a local copy for thread safety.

        byte[] lraster= this.raster; // make a local copy for thread safety.

        try {
            try {

                BufferedImage plotImage2;  // index color model

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

                        if (fds.getXLength() == 0) {
                            logger.fine("got empty dataset, setting image to null");
                            plotImage = null;
                            plotImageBounds= null;
                            rebinDataSet = null;
                            imageXRange = null;
                            imageYRange = null;
                            lparent.repaint();
                            return;
                        }

                        if (!fds.getXUnits().isConvertableTo(xAxis.getUnits())) {
                            logger.fine("dataset units are incompatable with x axis.");
                            plotImage = null;
                            plotImageBounds= null;
                            return;
                        }


                        if (!fds.getYUnits().isConvertableTo(yAxis.getUnits())) {
                            logger.fine("dataset units are incompatable with y axis.");
                            plotImage = null;
                            plotImageBounds= null;
                            return;
                        }

                        if (!((TableDataSet)fds).getZUnits().isConvertableTo(colorBar.getUnits()) ) {
                            logger.fine("dataset units are incompatable with colorbar.");
                            plotImage = null;
                            plotImageBounds= null;
                            return;                            
                        }
                        //System.err.println("zunits="+((TableDataSet)fds).getZUnits()+"  colorbar="+colorBar.getUnits() );
                        
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

                        imageXRange = xAxis.getDatumRange();
                        imageYRange = yAxis.getDatumRange();

                        logger.fine("rebinning to pixel resolution: "+ xRebinDescriptor + "  " + yRebinDescriptor );
                        logger.fine("rebinning to pixel resolution: "+ plotImageBounds2 );

                        DataSetRebinner rebinner = this.rebinnerEnum.getRebinner();

                        long t0;

                        t0 = System.currentTimeMillis();

                        rebinDataSet = (TableDataSet) rebinner.rebin(fds, xRebinDescriptor, yRebinDescriptor);

                        xmemento = xAxis.getMemento();
                        ymemento = yAxis.getMemento();
                        cmemento = colorBar.getMemento();

                        logger.fine("rebinning to pixel resolution: "+ xmemento + "  " + ymemento );

                        lraster = makePixMap( rebinDataSet, lraster );

                        try {
                            validCount= transformSimpleTableDataSet(rebinDataSet, colorBar, yAxis.isFlipped(), lraster );
                        } catch ( InconvertibleUnitsException ex ) {
                            System.err.println("zunits="+((TableDataSet)fds).getZUnits()+"  colorbar="+colorBar.getUnits() );
                            return;
                        }

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
                    DatumRange xdr= DataSetUtil.xRange( fds );
                    DatumRange ydr= DataSetUtil.yRange( fds );
                    double[] yy= GraphUtil.transformRange( yAxis, ydr );
                    double[] xx= GraphUtil.transformRange( xAxis, xdr );
                    selectionArea= rr.intersection( new Rectangle( (int)xx[0], (int)yy[0], (int)(xx[1]-xx[0]), (int)(yy[1]-yy[0]) ) );

                }
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
        if (colorBar != null && colorBar.getCanvas() != null) {
            colorBar.getCanvas().remove(colorBar);
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

    public DataSet getConsumedDataSet() {
        if (sliceRebinnedData) {
            return rebinDataSet;
        } else {
            return this.ds;
        }
    }

    public void setDataSet(DataSet ds) {
        DataSet oldDs = this.ds;
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
