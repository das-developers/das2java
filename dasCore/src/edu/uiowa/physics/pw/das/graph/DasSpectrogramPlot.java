/* File: DasSpectrogramPlot.java
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

import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.dataset.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author  jbf
 */
public class DasSpectrogramPlot extends edu.uiowa.physics.pw.das.graph.DasPlot implements DasZAxisPlot
{
    
    private XTaggedYScanDataSet rebinData;
    
    private SpectrogramRenderer renderer;
    
    /** Creates a new instance of DasSpectrogramPlot */
    public static DasSpectrogramPlot create(DasCanvas parent, XTaggedYScanDataSet Data) {
        
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.80);
        
        double [] x;
        double [] y;
        
        int nx= Data.data.length;
        int ny= Data.y_coordinate.length;
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= Data.data[i].x;
        }
        
        y= Data.y_coordinate;
        
        float z_fill= Data.getZFill();
        
        double [] zl= new double[nx*ny];
        int iz= 0;
        for (int i=0; i<nx; i++) {
            for (int j=0; j<ny; j++) {
                if (Data.data[i].z[j] != z_fill)
                    zl[iz++]= Data.data[i].z[j];
            }
        }
        double [] z= new double[iz];
        for (int i=0; i<iz; i++) z[i]= zl[i];
        
        DasColorBar colorBar= new DasColorBar(Datum.create(0,Data.getZUnits()),Datum.create(0,Data.getZUnits()),row,DasColorBar.getColorBarColumn(column),false);
        colorBar.setDataRange(z);
        
        DasSpectrogramPlot result= new DasSpectrogramPlot(Data,
        Data.getXAxis(row,column),
        Data.getYAxis(row,column),
        row, column,
        colorBar );
        
        return result;
    }
    
    /**
     * Default constructor.
     * This constructor creates an instance of <code>DasSpectrogramPlot</code>.
     * The xAxis, yAxis, row, column, and colorBar properties are all <code>null</code>
     * The data property is set to a default value (containing no data displayable).
     * The row, column, xAxis, yAxis, and colorBar properties must all be set to valid values
     * before this pwSpectrogram plot can be displayed.
     *
     * @see #setRow(DasRow)
     * @see #setColumn(DasColumn)
     * @see #setXAxis(DasAxis)
     * @see #setYAxis(DasAxis)
     * @see #setColorBar(DasColorBar)
     */
    public DasSpectrogramPlot() {
        this((XTaggedYScanDataSetDescriptor)null, null, null, null, null, null);
    }
    
    public DasSpectrogramPlot(XTaggedYScanDataSet data,
    DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column, DasColorBar colorBar) {
        this((data==null ? null : new ConstantDataSetDescriptor(data)),
        xAxis, yAxis, row, column, colorBar);
    }
    
    public DasSpectrogramPlot(XTaggedYScanDataSetDescriptor dataSetDescriptor,
        DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column, DasColorBar colorBar) {
            this((DataSetDescriptor)dataSetDescriptor, xAxis, yAxis, row, column, colorBar);
    }
    
    protected DasSpectrogramPlot(DataSetDescriptor dataSetDescriptor,
    DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column, DasColorBar colorBar) {
        
        super(xAxis,yAxis,row,column);
        
        renderer= new SpectrogramRenderer( this, dataSetDescriptor, colorBar );
        this.addRenderer(renderer); 

    }    
    
    public XTaggedYScanDataSet getRebinData() {
        return rebinData;
    }
    
    /** Getter for property colorBar.
     * @return Value of property colorBar.
     */
    public DasColorBar getColorBar() {
        return renderer.getColorBar();
    }
    
    /** Setter for property colorBar.
     * @param colorBar New value of property colorBar.
     */
    public void setColorBar(DasColorBar colorBar) {
        //Noop
    }
    
    public void setData(XTaggedYScanDataSet Data) {
        this.Data= Data;
    }
    
    /**
     * Mutator method for the <code>dataPath</code> property.
     * This method will not {@linkplain #update() update} the plot.
     *
     * @param dsdf the new value for the <code>dataPath</code> property.
     */
    public void setDataSetID(String id) throws edu.uiowa.physics.pw.das.DasException {
        renderer.setDataSetID(id);
        markDirty();
        update();
    }
    
    /**
     * Accessor method for the <code>dataPath</code> property.
     *
     * @return the value of the <code>dataPath</code> property.
     */
    public String getDataSetID() {
        return renderer.getDataSetID();
    }
    
    private static XTaggedYScanDataSet createEmptyDataSet() {
        XTaggedYScanDataSet ds = new XTaggedYScanDataSet(null);
        ds.data = new XTaggedYScan[0];
        ds.y_coordinate = new double[0];
        return ds;
    }
    
    public SpectrogramRenderer getRenderer() {
        return renderer;
    }
    
    public void setZAxis(DasAxis zAxis) {
        //NOOP
    }
    
    public DasAxis getZAxis() {
        return renderer.getColorBar();
    }
    
    protected void installComponent() {
        super.installComponent();
    }

    /** Process a <code>&lt;spectrogram&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasSpectrogramPlot processSpectrogramElement(Element element, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        String name = element.getAttribute("name");
        
        DasRow row = (DasRow)form.checkValue(element.getAttribute("row"), DasRow.class, "<row>");
        DasColumn column = (DasColumn)form.checkValue(element.getAttribute("column"), DasColumn.class, "<column>");
        
        DasAxis xAxis = null;
        DasAxis yAxis = null;
        DasColorBar colorbar = null;
        
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("xAxis")) {
                    xAxis = processXAxisElement((Element)node, row, column, form);
                }
                else if (node.getNodeName().equals("yAxis")) {
                    yAxis = processYAxisElement((Element)node, row, column, form);
                }
                else if (node.getNodeName().equals("zAxis")) {
                    colorbar = processZAxisElement((Element)node, row, column, form);
                }
            }
        }
        
        if (xAxis == null) {
            xAxis = (DasAxis)form.checkValue(element.getAttribute("xAxis"), DasAxis.class, "<axis> or <timeaxis>");
        }
        if (yAxis == null) {
            yAxis = (DasAxis)form.checkValue(element.getAttribute("yAxis"), DasAxis.class, "<axis> or <timeaxis>");
        }
        if (colorbar == null) {
            colorbar = (DasColorBar)form.checkValue(element.getAttribute("colorbar"), DasColorBar.class, "<colorbar>");
        }
        
        DasSpectrogramPlot plot = new DasSpectrogramPlot((XTaggedYScanDataSet)null, xAxis, yAxis, row, column, colorbar);
        
        plot.setTitle(element.getAttribute("title"));
        try {
            plot.setDataSetID(element.getAttribute("dataPath"));
        }
        catch (edu.uiowa.physics.pw.das.DasException de) {
            DasExceptionHandler.handle(de);
        }
        
        try {
            plot.setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        
        return plot;
    }    
    
    private static DasAxis processXAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element)node;
                if (node.getNodeName().equals("axis")) {
                    e.setAttribute("orientation", "horizontal");
                    return DasAxis.processAxisElement(e, row, column, form);
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    e.setAttribute("orientation", "horizontal");
                    return DasTimeAxis.processTimeaxisElement(e, row, column, form);
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    e.setAttribute("orientation", "horizontal");
                    return DasAxis.processAttachedaxisElement(e, row, column, form);
                }
            }
        }
        return null;
    }
    
    private static DasAxis processYAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element)node;
                if (node.getNodeName().equals("axis")) {
                    e.setAttribute("orientation", "vertical");
                    return DasAxis.processAxisElement(e, row, column, form);
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    e.setAttribute("orientation", "vertical");
                    return DasTimeAxis.processTimeaxisElement(e, row, column, form);
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    e.setAttribute("orientation", "vertical");
                    return DasAxis.processAttachedaxisElement(e, row, column, form);
                }
            }
        }
        return null;
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
        element.setAttribute("name", getDasName());
        element.setAttribute("row", getRow().getDasName());
        element.setAttribute("column", getColumn().getDasName());
        element.setAttribute("title", getTitle());
        element.setAttribute("dataPath", getDataSetID());
        
        Element xAxisChild = document.createElement("xAxis");
        Element xAxisElement = getXAxis().getDOMElement(document);
        xAxisElement.removeAttribute("orientation");
        if (xAxisElement.getAttribute("row").equals(getRow().getDasName())) {
            xAxisElement.removeAttribute("row");
        }
        if (xAxisElement.getAttribute("column").equals(getColumn().getDasName())) {
            xAxisElement.removeAttribute("column");
        }
        xAxisChild.appendChild(xAxisElement);
        element.appendChild(xAxisChild);
        
        Element yAxisChild = document.createElement("yAxis");
        Element yAxisElement = getYAxis().getDOMElement(document);
        yAxisElement.removeAttribute("orientation");
        if (yAxisElement.getAttribute("row").equals(getRow().getDasName())) {
            yAxisElement.removeAttribute("row");
        }
        if (yAxisElement.getAttribute("column").equals(getColumn().getDasName())) {
            yAxisElement.removeAttribute("column");
        }
        yAxisChild.appendChild(yAxisElement);
        element.appendChild(yAxisChild);
        
        Element zAxisChild = document.createElement("zAxis");
        Element zAxisElement = getColorBar().getDOMElement(document);
        if (zAxisElement.getAttribute("row").equals(getRow().getDasName())) {
            zAxisElement.removeAttribute("row");
        }
        if (zAxisElement.getAttribute("column").equals(getColumn().getDasName())) {
            zAxisElement.removeAttribute("column");
        }
        zAxisChild.appendChild(zAxisElement);
        element.appendChild(zAxisChild);
        
        return element;
    }
    
}
