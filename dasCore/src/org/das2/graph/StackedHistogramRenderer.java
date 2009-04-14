/* File: DasStackedHistogramPlot.java
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
import org.das2.event.LengthDragRenderer;
import org.das2.event.CrossHairMouseModule;
import org.das2.components.propertyeditor.Displayable;
import org.das2.dataset.DataSetRebinner;
import org.das2.dataset.NearestNeighborTableRebinner;
import org.das2.dataset.AveragePeakTableRebinner;
import org.das2.dataset.TableDataSetConsumer;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.RebinDescriptor;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.LocationUnits;
import org.das2.datum.DatumRange;
import org.das2.dasml.FormBase;
import org.das2.DasNameException;
import org.das2.DasPropertyException;
import org.das2.util.DasExceptionHandler;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.DasException;
import org.das2.components.HorizontalSpectrogramSlicer;
import org.das2.components.VerticalSpectrogramSlicer;
import org.das2.event.HorizontalSlicerMouseModule;
import org.das2.event.VerticalSlicerMouseModule;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.components.propertyeditor.Enumeration;

import java.awt.*;
import java.awt.geom.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.beans.*;
import java.text.*;
import javax.swing.*;
import org.w3c.dom.*;

/**
 *
 * @author  jbf
 */
public class StackedHistogramRenderer extends org.das2.graph.Renderer implements TableDataSetConsumer, PropertyChangeListener, Displayable  {
    
    private DasLabelAxis yAxis= null;
    private DasAxis zAxis= null;
    private RowRowConnector zAxisConnector= null;
    private DasRow littleRow=null;
    
    private RebinDescriptor xBins= null;
    
    private PeaksIndicator peaksIndicator;
    
    /** Holds value of property sliceRebinnedData. */
    private boolean sliceRebinnedData;
    
    Image plotImage;
    DatumRange imageXRange, imageYRange;
    
    public static class PeaksIndicator implements Enumeration, Displayable {
        
        String id;
        
        PeaksIndicator(String id) {
            this.id= id;
        }
        
        public String toString() {
            return this.id;
        }
        
        public static final PeaksIndicator NoPeaks= new PeaksIndicator("None");
        public static final PeaksIndicator GrayPeaks= new PeaksIndicator("Gray Peaks");
        public static final PeaksIndicator BlackPeaks= new PeaksIndicator("Black Peaks");
        public static final PeaksIndicator MaxLines= new PeaksIndicator("Lines");
        
        public String getListLabel() {
            return this.id;
        }
        
        public javax.swing.Icon getListIcon() {
            return null;
        }
        
    }
    
    protected class RebinListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            update();
        }
    }
    
    RebinListener rebinListener= new RebinListener();
    
    public StackedHistogramRenderer( DasPlot parent, DataSetDescriptor dsd, DasAxis zAxis, DasLabelAxis yAxis ) {
        super();
        
        this.yAxis= yAxis;
        this.zAxis= zAxis;
        
        zAxis.addPropertyChangeListener(rebinListener);
        
        setDataSetDescriptor( dsd );
    }
    
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        Graphics2D g2= (Graphics2D)g.create();
        
        Point2D p;
        if (getDataSet()==null && lastException!=null ) {
            renderException(g2,xAxis,yAxis,lastException);
        } else if (plotImage!=null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
            p= new Point2D.Float( xAxis.getColumn().getDMinimum(), yAxis.getRow().getDMinimum() );
            
            g2.drawImage( plotImage,(int)(p.getX()+0.5),(int)(p.getY()+0.5), getParent() );
            
        }
        g2.dispose();
    }
    
    protected void installRenderer() {
        DasCanvas canvas= parent.getCanvas();
        littleRow= new DasRow( canvas, 0.5,0.6 );
        zAxisConnector= new RowRowConnector( canvas, littleRow, zAxis.getRow(), parent.getColumn(), zAxis.getColumn() );
        zAxisConnector.setVisible(false);
        canvas.add(zAxisConnector);
        
        yAxis.setFloppyItemSpacing(true);
        yAxis.setOutsidePadding(1);
        
        this.peaksIndicator= PeaksIndicator.MaxLines;
        
        DasMouseInputAdapter mouseAdapter = parent.getDasMouseInputAdapter();
        
        //TODO: consider delaying construction of slicers until first event
        VerticalSpectrogramSlicer vSlicer = VerticalSpectrogramSlicer.createSlicer( parent, this );
        VerticalSlicerMouseModule vsl = VerticalSlicerMouseModule.create(this);        
        vsl.addDataPointSelectionListener(vSlicer);
        mouseAdapter.addMouseModule(vsl);
        
        HorizontalSpectrogramSlicer hSlicer = HorizontalSpectrogramSlicer.createSlicer( parent, this);
        HorizontalSlicerMouseModule hsl = HorizontalSlicerMouseModule.create(this);
        hsl.addDataPointSelectionListener(hSlicer);
        mouseAdapter.addMouseModule(hsl);
        
        MouseModule ch= new CrossHairMouseModule(parent,this,parent.getXAxis(), parent.getYAxis());
        mouseAdapter.addMouseModule(ch);
        
        DasPlot p= parent;
        mouseAdapter.addMouseModule( new MouseModule( p, new LengthDragRenderer(p,p.getXAxis(),p.getYAxis()), "Length" ) );
    }
    
    protected void uninstallRenderer() {
    }
    
    
    public void setZAxis(DasAxis zAxis) {
        this.zAxis= zAxis;
        throw new IllegalStateException("not supported");
    }
    
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        // this code was intended to make it so the zaxis component would move up and down with the labelAxis.
      /*  DasLabelAxis axis= (DasLabelAxis)getYAxis();
       
        if ( axis!=null ) {
            if ( getRow()!=DasRow.NULL ) {
                if ( axis.getInterItemSpace() > getRow().getHeight()/3.5 ) {
                    System.out.println("axis spacing exceeds zAxis spacing");
                    int[] labelPositions= axis.getLabelPositions();
                    zAxisComponent.getAxis().getRow().setDPosition( labelPositions[0], labelPositions[1] );
                } else {
                    int xx2= getRow().getDMaximum();
                    int xx1= getRow().getDMinimum();
                    zAxisComponent.getAxis().getRow().setDPosition( xx1, xx2 );
                }
            }
        } */
    }
    
    /**
     * @throws IllegalArgumentException if the yAxis is not an instanceof DasLabelAxis
     */
    public void setYAxis(DasAxis yAxis) {
        if (yAxis instanceof DasLabelAxis) {
            this.yAxis= (DasLabelAxis)yAxis;
            yAxis.addPropertyChangeListener(this);
        } else {
            throw new IllegalArgumentException("You can't call setYAxis for stackedHistogramPlot");
        }
    }
    
    
    synchronized public void updatePlotImage( DasAxis xAxis, DasAxis yAxis_1, ProgressMonitor monitor ) throws DasException {
        super.updatePlotImage( xAxis, yAxis_1, monitor );
        final Color BAR_COLOR= getParent().getForeground();
        final Color GREY_PEAKS_COLOR= Color.GRAY;

        Component parent= getParent();
        Cursor cursor0= parent.getCursor();
        parent.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        
        DasColumn column= xAxis.getColumn();
        DasRow row= yAxis.getRow();
        
        int w= column.getWidth();
        int h= row.getHeight();
        
        if ( w==0 ) return;
        
        //plotImage = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        BufferedImage plotImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = plotImage.createGraphics();
        
        if ( ! isTransparentBackground() ) {
            g.setColor( getParent().getBackground() );
            g.fillRect(0, 0, plotImage.getWidth(), plotImage.getHeight());
        }
        g.translate(-column.getDMinimum(),-row.getDMinimum());
        
        Dimension d;
        
        double iMin= column.getDMinimum();
        double jMin= row.getDMinimum();
        
        RebinDescriptor xbins= new RebinDescriptor(xAxis.getDataMinimum(), xAxis.getDataMaximum(), (int)(Math.abs(column.getWidth())/1)+1, (xAxis.isLog()));
        
        imageXRange= xAxis.getDatumRange();
        imageYRange= yAxis.getDatumRange();
        
        int xDMax= column.getDMaximum();
        int xDMin= column.getDMinimum();
        
        TableDataSet xtysData= (TableDataSet)getDataSet();
        
        if ( xtysData==null ) {
            this.plotImage= null;
            if ( lastException==null ) this.setLastException( new DasException("null data set" ) );
            return;
        }
        
        if ( xtysData.tableCount()==0 ) {
            this.setLastException( new DasException("empty data set") );
            this.ds= null;
            return;
        }
        
        DataSetRebinner rebinner = new Rebinner();
        
        TableDataSet data= (TableDataSet)rebinner.rebin(xtysData, xbins, null);
        TableDataSet peaks= (TableDataSet)data.getPlanarView("peaks");
        TableDataSet weights= (TableDataSet)data.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
        
        DasLabelAxis yAxis= (DasLabelAxis)yAxis_1;
        
        int zmid= zAxis.getRow().getDMiddle();
        boolean haveLittleRow= false;
        
        for (int j = 0; j < data.getYLength(0); j++) {
            
            int yBase;
            Line2D.Float lBase;
            
            if ( j==(data.getYLength(0)-1) ) {   /* Draw top grey line */
                yBase= yAxis.getItemMin(data.getYTagDatum(0, j)); 
                g.setColor(GREY_PEAKS_COLOR);
                g.drawLine(xDMin, yBase, xDMax, yBase );
                g.setColor(BAR_COLOR);
            }
            
            yBase= yAxis.getItemMax(data.getYTagDatum(0, j));
            g.setColor(Color.lightGray);
            g.drawLine(xDMin, yBase, xDMax, yBase );
            g.setColor(BAR_COLOR);
            
            int yBase1= yAxis.getItemMin(data.getYTagDatum(0, j));
            double canvasHeight= parent.getHeight();
            
            if ( !haveLittleRow && yBase1 <= zmid  ) {
                littleRow.setDPosition(yBase1,yBase);
                haveLittleRow= true;
                this.zAxisConnector.setVisible(true);
                this.zAxisConnector.repaint();
            } 
            
            double [] binStarts= xbins.binStarts();
            double [] binStops= xbins.binStops();
            
            int y0= yBase;
            
            int littleRowHeight= yBase - yBase1;
            double zAxisMax= zAxis.getDataMaximum().doubleValue(xtysData.getZUnits());
            double zAxisMin= zAxis.getDataMinimum().doubleValue(xtysData.getZUnits());
            
            if ( yBase1 >= row.getDMinimum() && yBase <= row.getDMaximum() ) {
                for (int ibin=0; ibin < data.getXLength(); ibin++) {
                    int x0= (int)xAxis.transform(binStarts[ibin],xbins.getUnits());
                    int x1;
                    x1=x0+1; // 1 pixel wide
                    double zz= data.getDouble( ibin, j, data.getZUnits() );
                    if ( !( data.getZUnits().isFill(zz) || Double.isNaN(zz) ) ) {
                        int yAvg= (int)zAxis.transform( zz, data.getZUnits(), yBase, yBase1 );
                        yAvg= yAvg > ( y0 - littleRowHeight ) ? yAvg : ( y0 - littleRowHeight );
                        int yHeight= (y0-yAvg)>(0) ? (y0-yAvg) : 0;
                        //yHeight= yHeight < littleRowHeight ? yHeight : littleRowHeight;
                        if ( peaks!=null ) {
                            double peakValue = peaks.getDouble(ibin, j, peaks.getZUnits());
                            if (peakValue <= zAxisMax) {
                                int yMax= (int)zAxis.transform( peakValue, data.getZUnits(), yBase, yBase1 );
                                yMax= (y0-yMax)>(0) ? yMax : (y0);
                                if (peaksIndicator==PeaksIndicator.MaxLines) {
                                    g.drawLine(x0,yMax,x0,yMax);
                                } else if ( peaksIndicator==PeaksIndicator.GrayPeaks ) {
                                    g.setColor(Color.lightGray.brighter());
                                    g.drawLine(x0,yMax,x0,y0);
                                    g.setColor(BAR_COLOR);
                                } else if ( peaksIndicator==PeaksIndicator.BlackPeaks ) {
                                    g.setColor(BAR_COLOR);
                                    g.drawLine(x0,yMax,x0,y0);
                                }
                            }
                        }
                        if ( zz>=zAxisMin ) g.drawLine(x0, yAvg, x0, yAvg+yHeight );
                    }
                }
            }
        }
        
        g.dispose();
        this.plotImage = plotImage;
        parent.setCursor(cursor0);
        getParent().repaint();
        
        if ( sliceRebinnedData ) super.ds= data;
        
    }
    
    public DasAxis getZAxis() {
        return zAxis;
    }
    
    public void setZTitle(String title) {
        getZAxis().setLabel(title);
    }
    
    public class Rebinner implements DataSetRebinner {
        DataSetRebinner highResRebinner;
        DataSetRebinner lowResRebinner;
        Rebinner() {
            highResRebinner= new NearestNeighborTableRebinner();
            //highResRebinner= new AveragePeakTableRebinner();
            lowResRebinner= new AveragePeakTableRebinner();
        }
        
        public DataSet rebin(DataSet ds, RebinDescriptor x, RebinDescriptor y) throws IllegalArgumentException, DasException {
            Datum xwidth= (Datum)ds.getProperty( "xTagWidth" );
            if ( xwidth==null ) xwidth= DataSetUtil.guessXTagWidth((TableDataSet)ds);
            Units rdUnits= x.getUnits();
            if ( rdUnits instanceof LocationUnits ) {
                rdUnits= ((LocationUnits)rdUnits).getOffsetUnits();
            }
            
            try {
                DataSet result;
                if ( x.binWidth() < xwidth.doubleValue(rdUnits) ) {
                    logger.fine("using rebinner "+highResRebinner);
                    result= highResRebinner.rebin( ds, x, y );
                } else {
                    logger.fine("using rebinner "+lowResRebinner);
                    result= lowResRebinner.rebin( ds, x, y );
                }
                return result;
            } catch ( Exception e ) {
                DasExceptionHandler.handle(e);
                return null;
            }
        }
        
    }
    
    /** Getter for property peaksIndicator.
     * @return Value of property peaksIndicator.
     */
    public PeaksIndicator getPeaksIndicator() {
        return this.peaksIndicator;
    }
    
    /** Setter for property peaksIndicator.
     * @param peaksIndicator New value of property peaksIndicator.
     */
    public void setPeaksIndicator(PeaksIndicator peaksIndicator) {
        this.peaksIndicator= peaksIndicator;
        refreshImage();
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
    protected boolean transparentBackground = false;
    public static final String PROP_TRANSPARENTBACKGROUND = "transparentBackground";

    public boolean isTransparentBackground() {
        return transparentBackground;
    }

    public void setTransparentBackground(boolean transparentBackground) {
        boolean oldTransparentBackground = this.transparentBackground;
        this.transparentBackground = transparentBackground;
        propertyChangeSupport.firePropertyChange(PROP_TRANSPARENTBACKGROUND, oldTransparentBackground, transparentBackground);
    }

    public Element getDOMElement(Document document) {
        
        Element element = document.createElement("stackedHistogram");
        element.setAttribute("zAxis", zAxis.getDasName() );
        element.setAttribute("dataSetID", getDataSetID() );
        return element;
    }
    
    public static Renderer processStackedHistogramElement(Element element, DasPlot parent, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        String dataSetID = element.getAttribute("dataSetID");
        
        Renderer renderer = new StackedHistogramRenderer( parent, (DataSetDescriptor)null, (DasAxis)null, (DasLabelAxis)parent.getYAxis() );
        try {
            renderer.setDataSetID(dataSetID);
        } catch (DasException de) {
            DasExceptionHandler.handle(de);
        }
        return renderer;
    }
    
    public String getListLabel() {
        return "stacked histogram";
    }
    
    public Icon getListIcon() {
        return null;
    }
    
}
