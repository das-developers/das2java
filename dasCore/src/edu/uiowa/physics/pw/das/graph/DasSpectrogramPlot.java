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

import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.dataset.*;
import org.w3c.dom.*;

/**
 *
 * @author  jbf
 */
public class DasSpectrogramPlot extends edu.uiowa.physics.pw.das.graph.DasPlot implements DasZAxisPlot
{
    
    private TableDataSet rebinData;
    
    private SpectrogramRenderer renderer;
    
    /** Creates a new instance of DasSpectrogramPlot */
    public static DasSpectrogramPlot create(DasCanvas parent, TableDataSet Data) {
        
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.80);
        
        double [] x;
        double [] y;
        
        int nx= Data.getXLength();
        int ny= Data.getYLength(0);
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= Data.getXTagDouble(i, Data.getXUnits());
        }
        
        y= new double[ny];
        for (int j = 0; j < ny; j++) {
            y[j] = Data.getYTagDouble(0, j, Data.getYUnits());
        }
        
        double z_fill = Double.NaN;
        
        double [] zl= new double[nx*ny];
        int iz= 0;
        for (int i=0; i<nx; i++) {
            for (int j=0; j<ny; j++) {
                double zValue = Data.getDouble(i, j, Data.getZUnits());
                if (!Double.isNaN(zValue))
                    zl[iz++]= zValue;
            }
        }
        double [] z= new double[iz];
        System.arraycopy(zl, 0, z, 0, iz);
        
        DasColorBar colorBar= new DasColorBar(Datum.create(0,Data.getZUnits()),Datum.create(0,Data.getZUnits()),false);
        colorBar.setColumn(DasColorBar.getColorBarColumn(column));
        colorBar.setDataRange(z);
        DasAxis xAxis = new DasAxis(Datum.create(0,Data.getXUnits()),Datum.create(0,Data.getXUnits()),DasAxis.HORIZONTAL,false);
        xAxis.setDataRange(x);
        DasAxis yAxis = new DasAxis(Datum.create(0,Data.getYUnits()),Datum.create(0,Data.getYUnits()),DasAxis.VERTICAL,false);
        xAxis.setDataRange(x);
        
        DasSpectrogramPlot result= new DasSpectrogramPlot(Data, xAxis, yAxis, row, column, colorBar );
        
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
        this((DataSetDescriptor)null, null, null, null);
    }
    
    public DasSpectrogramPlot(TableDataSet data, DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column, DasColorBar colorBar) {
        this((data==null ? null : new ConstantDataSetDescriptor(data)),
        xAxis, yAxis, colorBar);
    }
    
    public DasSpectrogramPlot(DataSetDescriptor dataSetDescriptor, DasAxis xAxis, DasAxis yAxis, DasColorBar colorBar) {
        super(xAxis,yAxis);
        renderer= new SpectrogramRenderer( this, dataSetDescriptor, colorBar );
        this.addRenderer(renderer); 
    }    
    
    public TableDataSet getRebinData() {
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
    
    private static TableDataSet createEmptyDataSet() {
        Units xUnits, yUnits, zUnits;
        xUnits = yUnits = zUnits = Units.dimensionless;
        double[] xTags, yTags;
        xTags = yTags = new double[0];
        double[][] zValues = {};
        TableDataSet ds = new DefaultTableDataSet(xTags, xUnits, yTags, yUnits, zValues, zUnits, java.util.Collections.EMPTY_MAP);
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
                    xAxis = processXAxisElement((Element)node, form);
                }
                else if (node.getNodeName().equals("yAxis")) {
                    yAxis = processYAxisElement((Element)node, form);
                }
                else if (node.getNodeName().equals("zAxis")) {
                    colorbar = processZAxisElement((Element)node, form);
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
        
        DasSpectrogramPlot plot = new DasSpectrogramPlot((DataSetDescriptor)null, xAxis, yAxis, colorbar);
        
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
    
    private static DasAxis processXAxisElement(Element element, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element)node;
                if (node.getNodeName().equals("axis")) {
                    e.setAttribute("orientation", "horizontal");
                    return DasAxis.processAxisElement(e, form);
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    e.setAttribute("orientation", "horizontal");
                    return DasAxis.processTimeaxisElement(e, form);
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    e.setAttribute("orientation", "horizontal");
                    return DasAxis.processAttachedaxisElement(e, form);
                }
            }
        }
        return null;
    }
    
    private static DasAxis processYAxisElement(Element element, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element)node;
                if (node.getNodeName().equals("axis")) {
                    e.setAttribute("orientation", "vertical");
                    return DasAxis.processAxisElement(e, form);
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    e.setAttribute("orientation", "vertical");
                    return DasAxis.processTimeaxisElement(e, form);
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    e.setAttribute("orientation", "vertical");
                    return DasAxis.processAttachedaxisElement(e, form);
                }
            }
        }
        return null;
    }
    
    private static DasColorBar processZAxisElement(Element element, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
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
