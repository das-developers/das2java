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
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.util.DasDie;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.awt.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.Icon;
import org.w3c.dom.*;
import testNew.graph.ColorBarTest;


/**
 *
 * @author  jbf
 */
public class SpectrogramRenderer extends Renderer implements TableDataSetConsumer, edu.uiowa.physics.pw.das.components.propertyeditor.Displayable {
    
    private DasColorBar colorBar;
    Image plotImage;
    BufferedImage plotImage2;
    
    protected class RebinListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            SpectrogramRenderer.this.plotImage= null;
            SpectrogramRenderer.this.plotImage2= null;            
            update();
        }
    }
    
    RebinListener rebinListener= new RebinListener();
    
    /** Holds value of property rebinner. */
    private RebinnerEnum rebinnerEnum;
    
    public static class RebinnerEnum implements Enumeration {
        DataSetRebinner rebinner;
        String label;
        
        private RebinnerEnum(DataSetRebinner rebinner, String label) {
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
        Graphics2D g2= (Graphics2D)g.create();
        
        AffineTransform at= getAffineTransform( xAxis, yAxis );
        
        if ( at==null ) {
            return; // TODO: consider throwing exception
        }
        if ( !at.isIdentity() ) {
            g2.transform(at);
            g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
        }
        
        if (getDataSet()==null && lastException!=null ) {
            renderException(g2,xAxis,yAxis,lastException);
        } else if (plotImage!=null) {
            int x = xAxis.getColumn().getDMinimum();
            int y = yAxis.getRow().getDMinimum();
            g2.drawImage( plotImage,x,y, getParent() );
        }
        g2.dispose();
    }
    
    int count = 0;
    
    private boolean sliceRebinnedData= true;
    
    public void updatePlotImage( DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor ) throws DasException {        
        try {
            TableDataSet rebinData;
            
            if (monitor != null) {
                if (monitor.isCancelled()) {
                    return;
                } else {
                    monitor.setTaskSize(-1);
                    monitor.started();
                }
            }
            
            int w = xAxis.getColumn().getDMaximum() - xAxis.getColumn().getDMinimum();
            int h = yAxis.getRow().getDMaximum() - yAxis.getRow().getDMinimum();
            
            if (getParent()==null  || w<=1 || h<=1 ) {
                DasDie.println("canvas not useable!!!");
                return;
            }
            
            if ( getDataSet() == null) {
                DasApplication.getDefaultApplication().getLogger( DasApplication.GRAPHICS_LOG ).fine( "got null dataset, setting image to null" );
                Units xUnits = getParent().getXAxis().getUnits();
                Units yUnits = getParent().getYAxis().getUnits();
                Units zUnits = getColorBar().getUnits();
                double[] xTags, yTags;
                xTags = yTags = new double[0];
                double[][] zValues = {yTags};
                rebinData = new DefaultTableDataSet(xTags, xUnits, yTags, yUnits, zValues, zUnits, Collections.EMPTY_MAP);
                plotImage= null;
                rebinData= null;
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
                
                DasApplication.getDefaultApplication().getLogger( DasApplication.GRAPHICS_LOG ).fine( "rebinning to pixel resolution" );
                
                DataSetRebinner rebinner= this.rebinnerEnum.getRebinner();
                //rebinner= new NewAverageTableRebinner();
                
                long t0;
                
                t0= System.currentTimeMillis();
                
                rebinData = (TableDataSet)rebinner.rebin( getDataSet(),xRebinDescriptor, yRebinDescriptor );
//3                SpectrogramRendererDemo.probe.add( "rebin", System.currentTimeMillis()-t0);
                
                /*t0= System.currentTimeMillis();
                
                int[] pix= new int[ w * h ];
                Arrays.fill(pix, 0x00000000);
                
                DasApplication.getDefaultApplication().getLogger( DasApplication.GRAPHICS_LOG ).fine( "converting to pixel map" );
                
                int itable=0;
                int ny= rebinData.getYLength(itable);
                int nx= rebinData.tableEnd(itable)-rebinData.tableStart(itable);
                
                for (int i=rebinData.tableStart(itable); i<rebinData.tableEnd(itable); i++) {
                    for (int j=0; j<rebinData.getYLength(0); j++) {
                        int index= (i-rebinData.tableStart(itable)) + ( ny - j - 1 ) * nx;
                        pix[index]= colorBar.rgbTransform(rebinData.getDouble(i,j,rebinData.getZUnits()),rebinData.getZUnits());
                    }
                }
                
                DasApplication.getDefaultApplication().getLogger( DasApplication.GRAPHICS_LOG ).fine( "creating MemoryImageSource" );
                
                MemoryImageSource mis = new MemoryImageSource( w, h, pix, 0, w );
                plotImage = getParent().createImage(mis);
                
                //long t= System.currentTimeMillis() - t0;
                                
//3                System.out.println("old way: "+t);
//3            SpectrogramRendererDemo.probe.add("old",t); 
                */
 //3               t0=  System.currentTimeMillis();
                plotImage2= (BufferedImage)ColorBarTest.transformSimpleTableDataSet( rebinData, colorBar, plotImage2 );
//3                long t2= System.currentTimeMillis() - t0;
                
//3                System.out.println("new way: "+t2);            
//3                SpectrogramRendererDemo.probe.addOverplot("new","old",t2);
                //System.out.println("     speedup:"+(t-t2)*100/t); 
                
             plotImage= plotImage2;
                
                if ( isSliceRebinnedData() ) {
                    DasApplication.getDefaultApplication().getLogger( DasApplication.GRAPHICS_LOG ).fine("slicing rebin data");
                    super.ds= rebinData;
                }
            }
            
        } finally {
            if (monitor != null) {
                if (monitor.isCancelled()) {
                    return;
                } else {
                    monitor.finished();
                }
            }
            
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
    
}
