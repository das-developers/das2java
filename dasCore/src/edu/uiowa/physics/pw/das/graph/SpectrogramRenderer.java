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

/**
 *
 * @author  jbf
 */

import edu.uiowa.physics.pw.das.client.DataSetDescriptorNotAvailableException;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.components.*;
import edu.uiowa.physics.pw.das.util.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.image.MemoryImageSource;

public class SpectrogramRenderer extends Renderer implements TableDataSetConsumer {
    
    private DasColorBar colorBar;
    Image plotImage;
    DataSet rebinDataSet;
    
    protected class RebinListener implements java.beans.PropertyChangeListener {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
            update(getParent().getXAxis(),getParent().getYAxis());
        }
    }
    
    RebinListener rebinListener= new RebinListener();
    
    /** Holds value of property rebinner. */
    private RebinnerEnum rebinnerEnum;
    
    public static class RebinnerEnum implements PropertyEditor.Enumeration {
        DataSetRebinner rebinner;
        String label;
        
        private RebinnerEnum(DataSetRebinner rebinner, String label) {
            this.rebinner= rebinner;
            this.label= label;
        }
        public static RebinnerEnum binAverage= new RebinnerEnum(new AverageTableRebinner(),"binAverage");
        public static RebinnerEnum nearestNeighbor= new RebinnerEnum(new NearestNeighborTableRebinner(),"nearestNeighbor");        
        public javax.swing.Icon getListIcon() {
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
        if (plotImage!=null) {
            int x = xAxis.getColumn().getDMinimum();
            int y = yAxis.getRow().getDMinimum();
            g.drawImage( plotImage,x,y, getParent() );
        }
        if (getDataSet()==null && lastException!=null ) {
            renderException(g,xAxis,yAxis,lastException);
        }
    }
    
    int count = 0;
    
    public void updatePlotImage( DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor ) {
        
        TableDataSet rebinData;
        
        if (monitor != null) {
            if (monitor.isCancelled()) {
                return;
            }
            else {
                monitor.setTaskSize(-1);
                monitor.started();
            }
        }
        
        int w = xAxis.getColumn().getDMaximum() - xAxis.getColumn().getDMinimum();
        int h = yAxis.getRow().getDMaximum() - yAxis.getRow().getDMinimum();
        
        if (getParent()==null  || w<=1 || h<=1 ) {
            edu.uiowa.physics.pw.das.util.DasDie.println("canvas not useable!!!");
            return;
        }
        
        int[] pix= new int[ w * h ];
        java.util.Arrays.fill(pix, 0x00000000);
        
        if ( getDataSet() == null) {
            Units xUnits = getParent().getXAxis().getUnits();
            Units yUnits = getParent().getYAxis().getUnits();
            Units zUnits = getColorBar().getUnits();
            double[] xTags, yTags;
            xTags = yTags = new double[0];
            double[][] zValues = {yTags};
            rebinData = new DefaultTableDataSet(xTags, xUnits, yTags, yUnits, zValues, zUnits, java.util.Collections.EMPTY_MAP);
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
            
            DataSetRebinner rebinner= this.rebinnerEnum.getRebinner();
            
            rebinData = (TableDataSet)rebinner.rebin(getDataSet(),xRebinDescriptor, yRebinDescriptor);
            
        }
                
        //TableDataSet weights= (TableDataSet)rebinData.getPlanarView("weights");
        int itable=0;
        int ny= rebinData.getYLength(itable);
        int nx= rebinData.tableEnd(itable)-rebinData.tableStart(itable);
        
        for (int i=rebinData.tableStart(itable); i<rebinData.tableEnd(itable); i++) {
            for (int j=0; j<rebinData.getYLength(0); j++) {
                int index= (i-rebinData.tableStart(itable)) + ( ny - j - 1 ) * nx;
                pix[index]= colorBar.itransform(rebinData.getDouble(i,j,rebinData.getZUnits()),rebinData.getZUnits());
            }
        }
        
        if (monitor != null) {
            if (monitor.isCancelled()) {
                return;
            }
            else {
                monitor.finished();
            }
        }

        MemoryImageSource mis = new MemoryImageSource( w, h, pix, 0, w );
        plotImage = getParent().createImage(mis);
        getParent().repaint();
        rebinDataSet = rebinData;
    }
    
    public void setDataSetDescriptor(DataSetDescriptor dsd) {
        super.setDataSetDescriptor(dsd);
        rebinDataSet = null;
    }
    
    protected void installRenderer() {
        if (parent != null && parent.getCanvas() != null) {
            if (colorBar != null) {
                if (colorBar.getRow() == null || colorBar.getColumn() == null) {
                    colorBar.setRow(parent.getRow());
                    DasColumn column = parent.getColumn();
                    double columnWidth = column.getMaximum() - column.getMinimum();
                    double cbMin = column.getMaximum() + columnWidth/5.0;
                    double cbMax = cbMin + columnWidth/10.0;
                    if (cbMax > (1.0 - columnWidth/5.0)) {
                        double cbWidth = (1.0 - column.getMaximum())/3.0;
                        cbMin = column.getMaximum() + cbWidth/2.0;
                        cbMax = cbMin + cbWidth;
                    }
                    colorBar.setColumn(new DasColumn(parent.getCanvas(), cbMin, cbMax));
                }
                parent.getCanvas().add(colorBar);
                if (!"true".equals(System.getProperty("java.awt.headless"))) {
                    DasMouseInputAdapter mouseAdapter = parent.mouseAdapter;
                    VerticalSpectrogramSlicer vSlicer=
                    VerticalSpectrogramSlicer.createPopupSlicer(parent, this, 640, 480);
                    VerticalSlicerMouseModule vsl = VerticalSlicerMouseModule.create(this);
                    vsl.addDataPointSelectionListener(vSlicer);
                    mouseAdapter.addMouseModule(vsl);
                    
                    HorizontalSpectrogramSlicer hSlicer
                    = HorizontalSpectrogramSlicer.createPopupSlicer(parent, this, 640, 480);
                    HorizontalSlicerMouseModule hsl = HorizontalSlicerMouseModule.create(this);
                    hsl.addDataPointSelectionListener(hSlicer);
                    mouseAdapter.addMouseModule(hsl);
                    
                    VerticalSpectrogramAverager vAverager
                    = VerticalSpectrogramAverager.createPopupAverager(parent, this, 640, 480);
                    HorizontalDragRangeSelectorMouseModule vrl = new HorizontalDragRangeSelectorMouseModule(parent,this,parent.getXAxis());
                    vrl.setLabel("Vertical Averager");
                    vrl.addDataRangeSelectionListener(vAverager);
                    mouseAdapter.addMouseModule(vrl);
                    
                    MouseModule ch= new CrossHairMouseModule(parent,this,parent.getXAxis(), parent.getYAxis());
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
    
    public static SpectrogramRenderer processSpectrogramElement(Element element, DasPlot parent, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        String dataSetID = element.getAttribute("dataSetID");
        DasColorBar colorbar = null;
        
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element && node.getNodeName().equals("zAxis")) {
                colorbar = processZAxisElement((Element)node, parent.getRow(), parent.getColumn(), form);
            }
        }
        
        if (colorbar == null) {
            try {
                colorbar = (DasColorBar)form.checkValue(element.getAttribute("colorbar"), DasColorBar.class, "<colorbar>");
            }
            catch (edu.uiowa.physics.pw.das.DasPropertyException dpe) {
                dpe.setPropertyName("colorbar");
                throw dpe;
            }
        }
        
        SpectrogramRenderer renderer = new SpectrogramRenderer(parent, null, colorbar);
        try {
            renderer.setDataSetID(dataSetID);
        }
        catch (edu.uiowa.physics.pw.das.DasException de) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(de);
        }
        return renderer;
    }
    
    private static DasColorBar processZAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("colorbar")) {
                    return DasColorBar.processColorbarElement((Element)node, row, column, form);
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
    
    public DataSet getDataSet() {
        if (rebinDataSet == null) {
            return super.getDataSet();
        }
        else {
            return rebinDataSet;
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
     * @param rebinner New value of property rebinner.
     *
     */
    public void setRebinner( RebinnerEnum rebinnerEnum) {
        this.rebinnerEnum = rebinnerEnum;
    }
    
}
