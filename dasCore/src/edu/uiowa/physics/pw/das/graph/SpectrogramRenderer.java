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

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.DasNameException;
import edu.uiowa.physics.pw.das.DasPropertyException;
import edu.uiowa.physics.pw.das.components.HorizontalSpectrogramSlicer;
import edu.uiowa.physics.pw.das.components.VerticalSpectrogramAverager;
import edu.uiowa.physics.pw.das.components.VerticalSpectrogramSlicer;
import edu.uiowa.physics.pw.das.components.propertyeditor.*;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.awt.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.Arrays;
import java.util.logging.Logger;
import javax.swing.Icon;
import org.w3c.dom.*;

/**
 *
 * @author  jbf
 */
public class SpectrogramRenderer extends Renderer implements TableDataSetConsumer, edu.uiowa.physics.pw.das.components.propertyeditor.Displayable {
    
    private Object lockObject= new Object();
    private DasColorBar colorBar;
    private Image plotImage;
    private byte[] raster;
    
    DatumRange imageXRange;
    DatumRange imageYRange;
    DasAxis.Memento xmemento, ymemento, cmemento;
    
    int updateImageCount=0, renderCount=0;
    
    private TableDataSet rebinDataSet;  // simpleTableDataSet at pixel resolution
    
    protected class RebinListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            update();
        }
    }
    
    RebinListener rebinListener= new RebinListener();
    
    private static Logger logger= DasLogger.getLogger( DasLogger.GRAPHICS_LOG );
    
    /** Holds value of property rebinner. */
    private RebinnerEnum rebinnerEnum;
    
    public static class RebinnerEnum implements Enumeration {
        DataSetRebinner rebinner;
        String label;
        
        public RebinnerEnum(DataSetRebinner rebinner, String label) {
            this.rebinner= rebinner;
            this.label= label;
        }
        
        
        public static final RebinnerEnum binAverage= new RebinnerEnum(new AverageTableRebinner(),"binAverage");
        public static final RebinnerEnum nearestNeighbor= new RebinnerEnum(new NearestNeighborTableRebinner(),"nearestNeighbor");
        public static final RebinnerEnum binAverageNoInterpolate;
        public static final RebinnerEnum binAverageNoInterpolateNoEnlarge;
        static {
            AverageTableRebinner rebinner= new AverageTableRebinner();
            rebinner.setInterpolate(false);
            binAverageNoInterpolate= new RebinnerEnum(rebinner,"noInterpolate");
            
            rebinner = new AverageTableRebinner();
            rebinner.setInterpolate(false);
            rebinner.setEnlargePixels(false);
            binAverageNoInterpolateNoEnlarge = new RebinnerEnum(rebinner, "noInterpolateNoEnlarge");
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
            return null;
        }
        public String toString() {
            return this.label;
        }
        DataSetRebinner getRebinner() {
            return this.rebinner;
        }
    }
    
    public SpectrogramRenderer(DataSetDescriptor dsd, DasColorBar colorBar ) {
        super( dsd );
        this.colorBar= colorBar;
        if (this.colorBar != null) {
            colorBar.addPropertyChangeListener("dataMinimum", rebinListener);
            colorBar.addPropertyChangeListener("dataMaximum", rebinListener);
            colorBar.addPropertyChangeListener("log", rebinListener);
            colorBar.addPropertyChangeListener("type", rebinListener);
        }
        setRebinner(SpectrogramRenderer.RebinnerEnum.binAverage);
    }
    
    
    /** Creates a new instance of SpectrogramRenderer
     * @deprecated use {link
     * #SpectrogramRenderer(edu.uiowa.physics.pw.das.dataset.DataSetDescriptor,
     * edu.uiowa.physics.pw.das.graph.DasColorBar)}
     */
    public SpectrogramRenderer( DasPlot parent, DataSetDescriptor dsd, DasColorBar colorBar ) {
        this( dsd, colorBar );
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
            colorBar.removePropertyChangeListener("type", rebinListener);
            if (parent != null && parent.getCanvas() != null) {
                parent.getCanvas().remove(colorBar);
            }
        }
        colorBar = cb;
        if (colorBar != null) {
            colorBar.addPropertyChangeListener("dataMinimum", rebinListener);
            colorBar.addPropertyChangeListener("dataMaximum", rebinListener);
            colorBar.addPropertyChangeListener("log", rebinListener);
            colorBar.addPropertyChangeListener("type", rebinListener);
            if (parent != null && parent.getCanvas() != null) {
                parent.getCanvas().add(colorBar);
            }
        }
    }
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis) {
        logger.finer("entering SpectrogramRenderer.render");
        Graphics2D g2= (Graphics2D)g.create();
        
        renderCount ++;
        reportCount();
        synchronized (lockObject ) {
            if ( plotImage==null && lastException!=null ) {
                renderException(g2,xAxis,yAxis,lastException);
            } else if (plotImage!=null) {
                Point2D p;
                p= new Point2D.Float( xAxis.getColumn().getDMinimum(), yAxis.getRow().getDMinimum() );
                g2.drawImage( plotImage,(int)(p.getX()+0.5),(int)(p.getY()+0.5), getParent() );
            }
        }
        g2.dispose();
    }
    
    int count = 0;
    
    private boolean sliceRebinnedData= true;
    
    /**
     * transforms the simpleTableDataSet into a Raster byte array.  The rows of
     * the table are adjacent in the output byte array.
     */
    private static byte[] transformSimpleTableDataSet( TableDataSet rebinData, DasColorBar cb ) {
        
        if ( rebinData.tableCount() > 1 ) throw new IllegalArgumentException("TableDataSet contains more than one table");
        logger.fine( "converting to pixel map" );
        //TableDataSet weights= (TableDataSet)rebinData.getPlanarView("weights");
        int itable=0;
        int ny= rebinData.getYLength(itable);
        int h= ny;
        int nx= rebinData.tableEnd(itable)-rebinData.tableStart(itable);
        int w= nx;
        int icolor;
        
        Units units= cb.getUnits();
        int ncolor= cb.getType().getColorCount();
        
        TableDataSet weights= (TableDataSet)rebinData.getPlanarView("weights");
        
        byte[] pix= new byte[ nx * ny ];
        Arrays.fill( pix, (byte)cb.getFillColorIndex() );
        
        for (int i=rebinData.tableStart(itable); i<rebinData.tableEnd(itable); i++) {
            for (int j=0; j<ny; j++) {
                if ( weights==null || weights.getDouble(i, j,Units.dimensionless) > 0. ) {
                    int index= (i-0) + ( ny - j - 1 ) * nx;
                    icolor= (int)cb.indexColorTransform(rebinData.getDouble(i,j,units), units );
                    pix[index]= (byte) icolor;
                }
            }
        }
        
        return pix;
    }
    
    private void reportCount() {
       // System.err.println("  updates: "+updateImageCount+"   renders: "+renderCount );
    }
    
    public void updatePlotImage( DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor ) throws DasException {
        logger.finer("entering SpectrogramRenderer.updatePlotImage");
        updateImageCount++;
        reportCount();
        try {
            int w = xAxis.getColumn().getDMaximum() - xAxis.getColumn().getDMinimum();
            int h = yAxis.getRow().getDMaximum() - yAxis.getRow().getDMinimum();
            
            if ( raster!=null && xmemento!=null && ymemento!=null
                    && xAxis.getMemento().equals( xmemento ) && yAxis.getMemento().equals( ymemento )
                    && colorBar.getMemento().equals( cmemento ) ) {
                logger.fine("same xaxis, yaxis, reusing raster");
                
            } else {
                
                if (getParent()==null  || w<=1 || h<=1 ) {
                    logger.finest("canvas not useable!!!");
                    return;
                }
                
                if ( this.ds == null) {
                    logger.fine( "got null dataset, setting image to null" );
                    plotImage= null;
                    rebinDataSet= null;
                    imageXRange= null;
                    imageYRange= null;
                    getParent().repaint();
                    return;
                    
                } else {
                    
                    RebinDescriptor xRebinDescriptor;
                    xRebinDescriptor = new RebinDescriptor(
                            xAxis.getDataMinimum(), xAxis.getDataMaximum(),
                            w,
                            xAxis.isLog());
                    
                    RebinDescriptor yRebinDescriptor = new RebinDescriptor(
                            yAxis.getDataMinimum(), yAxis.getDataMaximum(),
                            h,
                            yAxis.isLog());
                    
                    imageXRange= xAxis.getDatumRange();
                    imageYRange= yAxis.getDatumRange();
                    
                    logger.fine( "rebinning to pixel resolution" );
                    
                    DataSetRebinner rebinner= this.rebinnerEnum.getRebinner();
                    
                    long t0;
                    
                    t0= System.currentTimeMillis();
                    
                    rebinDataSet = (TableDataSet)rebinner.rebin( this.ds, xRebinDescriptor, yRebinDescriptor );
                    
                    xmemento= xAxis.getMemento();
                    ymemento= yAxis.getMemento();
                    cmemento= colorBar.getMemento();
                    
                    raster= transformSimpleTableDataSet( rebinDataSet, colorBar );
                    
                }
            }
            
            BufferedImage plotImage2;  // index color model
            
            IndexColorModel model= colorBar.getIndexColorModel();
            plotImage2= new BufferedImage( w, h, BufferedImage.TYPE_BYTE_INDEXED, model );
            
            WritableRaster r= plotImage2.getRaster();
            
            r.setDataElements( 0,0,w,h,raster);
            
            synchronized ( lockObject ) {
                plotImage= plotImage2;
            }
        } catch ( NoDataInIntervalException e ) {
            lastException= e;
            plotImage= null;
        } finally {
            getParent().repaint();
        }
    }
    
    protected void installRenderer() {
        if (parent != null && parent.getCanvas() != null) {
            if (colorBar != null) {
                if (colorBar.getColumn() == DasColumn.NULL ) {
                    DasColumn column = parent.getColumn();
                    colorBar.setColumn( column.createAttachedColumn( 1.05, 1.10 ) );
                }
                parent.getCanvas().add(colorBar, parent.getRow(), colorBar.getColumn());
                if (!"true".equals(System.getProperty("java.awt.headless"))) {
                    DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
                    VerticalSpectrogramSlicer vSlicer=
                            VerticalSpectrogramSlicer.createSlicer(parent, this);
                    VerticalSlicerMouseModule vsl = VerticalSlicerMouseModule.create(this);
                    vsl.addDataPointSelectionListener(vSlicer);
                    mouseAdapter.addMouseModule(vsl);
                    
                    HorizontalSpectrogramSlicer hSlicer = HorizontalSpectrogramSlicer.createSlicer(parent, this);
                    HorizontalSlicerMouseModule hsl = HorizontalSlicerMouseModule.create(this);
                    hsl.addDataPointSelectionListener(hSlicer);
                    mouseAdapter.addMouseModule(hsl);
                    
                    VerticalSpectrogramAverager vAverager = VerticalSpectrogramAverager.createAverager(parent, this);
                    HorizontalDragRangeSelectorMouseModule vrl = new HorizontalDragRangeSelectorMouseModule(parent,this,parent.getXAxis());
                    //vrl.setLabel("Vertical Averager");
                    vrl.addDataRangeSelectionListener(vAverager);
                    mouseAdapter.addMouseModule(vrl);
                    
                    MouseModule ch= new CrossHairMouseModule(parent,this,parent.getXAxis(), parent.getYAxis());
                    mouseAdapter.addMouseModule(ch);
                    
                    ch= new DumpToFileMouseModule( parent, this, parent.getXAxis(), parent.getYAxis() );
                    mouseAdapter.addMouseModule(ch);
                    
                    DasPlot p= parent;
                    mouseAdapter.addMouseModule( new MouseModule( p, new LengthDragRenderer( p,p.getXAxis(),p.getYAxis()), "Length" ) );
                    
                    mouseAdapter.addMouseModule( new BoxZoomMouseModule( p, this, p.getXAxis(), p.getYAxis() ) );
                    
                }
            }
        }
    }
    
    protected void uninstallRenderer() {
        if (colorBar != null && colorBar.getCanvas() != null) {
            colorBar.getCanvas().remove(colorBar);
        }
    }
    
    public static SpectrogramRenderer processSpectrogramElement(Element element, DasPlot parent, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        String dataSetID = element.getAttribute("dataSetID");
        DasColorBar colorbar = null;
        
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && node.getNodeName().equals("zAxis")) {
                colorbar = processZAxisElement((Element)node, form);
            }
        }
        
        if (colorbar == null) {
            try {
                colorbar = (DasColorBar)form.checkValue(element.getAttribute("colorbar"), DasColorBar.class, "<colorbar>");
            } catch (DasPropertyException dpe) {
                dpe.setPropertyName("colorbar");
                throw dpe;
            }
        }
        
        SpectrogramRenderer renderer = new SpectrogramRenderer(parent, null, colorbar);
        try {
            renderer.setDataSetID(dataSetID);
        } catch (DasException de) {
            DasExceptionHandler.handle(de);
        }
        return renderer;
    }
    
    private static DasColorBar processZAxisElement(Element element, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("colorbar")) {
                    return DasColorBar.processColorbarElement((Element)node, form);
                }
            }
        }
        return null;
    }
    
    public Element getDOMElement(Document document) {
        
        Element element = document.createElement("spectrogram");
        element.setAttribute("dataSetID", getDataSetID());
        
        Element zAxisChild = document.createElement("zAxis");
        Element zAxisElement = getColorBar().getDOMElement(document);
        if (zAxisElement.getAttribute("row").equals(getParent().getRow().getDasName())) {
            zAxisElement.removeAttribute("row");
        }
        if (zAxisElement.getAttribute("column").equals(getParent().getColumn().getDasName())) {
            zAxisElement.removeAttribute("column");
        }
        zAxisChild.appendChild(zAxisElement);
        element.appendChild(zAxisChild);
        
        return element;
    }
    
    /** Getter for property rebinner.
     * @return Value of property rebinner.
     *
     */
    public RebinnerEnum getRebinner() {
        return this.rebinnerEnum;
    }
    
    /** Setter for property rebinner.
     * @param rebinner New value of property rebinner.
     *
     */
    public void setRebinner( RebinnerEnum rebinnerEnum) {
        this.rebinnerEnum = rebinnerEnum;
        this.raster= null;
        this.plotImage= null;
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
    
    public String getListLabel() {
        return "spectrogram";
    }
    
    public Icon getListIcon() {
        return null;
    }
    
    public DataSet getDataSet() {
        if ( sliceRebinnedData ) {
            return rebinDataSet;
        } else {
            return this.ds;
        }
    }
    
    public void setDataSet(DataSet ds) {
        this.raster= null;
        // TODO: preserve plotImage until updatePlotImage is done
        this.plotImage= null;
        super.setDataSet(ds);
    }
    
}
