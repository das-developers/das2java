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
import edu.uiowa.physics.pw.das.components.HorizontalSpectrogramSlicer;
import edu.uiowa.physics.pw.das.components.VerticalSpectrogramAverager;
import edu.uiowa.physics.pw.das.components.VerticalSpectrogramSlicer;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.event.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.awt.image.MemoryImageSource;

public class SpectrogramRenderer extends Renderer implements TableDataSetConsumer {
    
    private DasColorBar colorBar;
    Image plotImage;
    
    protected class RebinListener implements java.beans.PropertyChangeListener {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
            update(getParent().getXAxis(),getParent().getYAxis());
        }
    }
    
    RebinListener rebinListener= new RebinListener();

    protected SpectrogramRenderer(DataSetDescriptor dsd, DasColorBar colorBar ) {
        super( dsd );
        this.colorBar= colorBar;
        if (this.colorBar != null) {
            colorBar.addPropertyChangeListener("dataMinimum", rebinListener);
            colorBar.addPropertyChangeListener("dataMaximum", rebinListener);
            colorBar.addPropertyChangeListener("log", rebinListener);
            colorBar.addPropertyChangeListener("type", rebinListener);
        }
    }

    public SpectrogramRenderer(XTaggedYScanDataSetDescriptor dsd, DasColorBar colorBar) {
        this((DataSetDescriptor)dsd, colorBar);
    }
    
    /** Creates a new instance of SpectrogramRenderer
     * @deprecated use {link
     * #SpectrogramRenderer(edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor,
     * edu.uiowa.physics.pw.das.graph.DasColorBar)}
     */
    public SpectrogramRenderer( DasPlot parent, XTaggedYScanDataSetDescriptor dsd, DasColorBar colorBar ) {
        this((DataSetDescriptor)dsd, colorBar );
        this.parent = parent;
    }
    
    protected SpectrogramRenderer( DasPlot parent, DataSetDescriptor dsd, DasColorBar colorBar ) {
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
        
    public void updatePlotImage( DasAxis xAxis, DasAxis yAxis ) {
               
        XTaggedYScanDataSet rebinData;
        
        int w = xAxis.getColumn().getDMaximum() - xAxis.getColumn().getDMinimum();
        int h = yAxis.getRow().getDMaximum() - yAxis.getRow().getDMinimum();
        
      	if (getParent()==null  || w<=1 || h<=1 ) {
            edu.uiowa.physics.pw.das.util.DasDie.println("canvas not useable!!!");
            return;
        }

        int[] pix= new int[ w * h ];
        java.util.Arrays.fill(pix, 0x00000000);
        
        if ( getDataSet() == null) {
            rebinData = XTaggedYScanDataSet.create(new double[0], new double[0], new float[0][0]);
        }
        else {
            RebinDescriptor xRebinDescriptor;
            xRebinDescriptor = new RebinDescriptor(
            xAxis.getDataMinimum(), xAxis.getDataMaximum(),
            w,
            xAxis.isLog());
            
            RebinDescriptor yRebinDescriptor = new RebinDescriptor(
            yAxis.getDataMinimum(), yAxis.getDataMaximum(),
            h,
            yAxis.isLog());
            
            rebinData = ((XTaggedYScanDataSet)getDataSet()).rebin(xRebinDescriptor, yRebinDescriptor);
        }
        
        DataSet ds= getDataSet();
        //setDataSet(rebinData);  This fires off a new update event -- infinate loop jbf July3,03
        int index=0;
        XTaggedYScan[] weights= rebinData.getWeights();
        for (int iz=rebinData.y_coordinate.length-1;iz>=0; iz--) {
            for (int i=0; i<rebinData.data.length; i++) {
                if (weights[i].z[iz]>0.) {
                    pix[index]= colorBar.itransform(rebinData.data[i].z[iz],rebinData.getZUnits());
                }
                index++;
            }
        }
        
        MemoryImageSource mis = new MemoryImageSource( w, h, pix, 0, w );
        plotImage = getParent().createImage(mis);
        getParent().repaint();
    }
    
    public void setDataSetID(String id) throws edu.uiowa.physics.pw.das.DasException {
        if (id == null) throw new NullPointerException("Null dataPath not allowed");
        if (id.equals("")) {
            setDataSetDescriptor(null);
            return;
        }
        DataSetDescriptor dsd = DataSetDescriptorUtil.create(id);
        if (!(dsd instanceof XTaggedYScanDataSetDescriptor)) {
            throw new DataSetDescriptorNotAvailableException(id + " does not refer to an x-tagged-y-scan data set");
        }
        setDataSetDescriptor(dsd);
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
        
        SpectrogramRenderer renderer = new SpectrogramRenderer(parent, (XTaggedYScanDataSetDescriptor)null, colorbar);
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
}
