/* File: DasPlot.java
 * Copyright (C) 2002-2003 University of Iowa
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
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.graph.dnd.TransferableRenderer;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.util.DnDSupport;
import edu.uiowa.physics.pw.das.util.GrannyTextRenderer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;

public class DasPlot extends DasCanvasComponent implements DataSetConsumer {
    
    protected DataSetDescriptor dataSetDescriptor;
    
    protected DataSet Data;
    private DasAxis xAxis;
    private DasAxis yAxis;
    
    protected String offsetTime = "";
    protected String plotTitle = "";
        
    protected double [] psym_x;
    protected double [] psym_y;
    
    protected Image plotImage;
    
    protected RebinListener rebinListener = new RebinListener();
    
    DnDSupport dndSupport;
    
    private DasPlot() {
        super();
        setOpaque(true);
    }
    
    public DasPlot(DasAxis xAxis, DasAxis yAxis, DasRow row, DasColumn column ) {
        super(row,column);
        setOpaque(false);
        this.renderers= new ArrayList();
        this.xAxis = xAxis;
        if (xAxis != null) {
            if (!xAxis.isHorizontal())
                throw new IllegalArgumentException("xAxis is not horizontal");
            xAxis.addPropertyChangeListener("dataMinimum", rebinListener);
            xAxis.addPropertyChangeListener("dataMaximum", rebinListener);
            xAxis.addPropertyChangeListener("log", rebinListener);
        }
        this.yAxis = yAxis;
        if (yAxis != null) {
            if (yAxis.isHorizontal())
                throw new IllegalArgumentException("yAxis is not vertical");
            yAxis.addPropertyChangeListener("dataMinimum", rebinListener);
            yAxis.addPropertyChangeListener("dataMaximum", rebinListener);
            yAxis.addPropertyChangeListener("log", rebinListener);
        }
        
        if (!"true".equals(System.getProperty("java.awt.headless"))) {
            
            HorizontalRangeSelectorMouseModule hrs=
            new HorizontalRangeSelectorMouseModule(this, xAxis);
            mouseAdapter.addMouseModule(hrs);
            mouseAdapter.setPrimaryModule(hrs);
            hrs.addDataRangeSelectionListener(xAxis);
            
            VerticalRangeSelectorMouseModule vrs=
            new VerticalRangeSelectorMouseModule(this,yAxis);
            mouseAdapter.addMouseModule(vrs);
            vrs.addDataRangeSelectionListener(yAxis);
            
            MouseModule x= new CrossHairMouseModule(this,xAxis,yAxis);
            mouseAdapter.addMouseModule(x);
            mouseAdapter.setSecondaryModule(x);
        }
    }
    
    public DataSet getDataSet() {
        return Data;
    }
    
    public DataSet getData() {
        return Data;
    }
    
    public void setXRange( double x1, double x2 ) {
        setXRange( new Datum(x1), new Datum(x2) );
    }
    
    public void setXRange(Datum x1, Datum x2) {
        xAxis.setDataRange(x1,x2);
    }
    
    public void setYRange( double y1, double y2 ) {
        setYRange( new Datum(y1), new Datum(y2) );
    }
    
    public void setYRange(Datum y1, Datum y2) {
        yAxis.setDataRange(y1,y2);
    }
    
    public void setDPosition(double xStart, double yStart, double xSize, double ySize) {
        if (getRow()==null) setRow(new DasRow((DasCanvas)this.getParent(),0.0, 0.0));
        if (getColumn()==null) setColumn(new DasColumn((DasCanvas)this.getParent(),0.0, 0.0));
        getRow().setDPosition(yStart,yStart+ySize);
        getColumn().setDPosition(xStart,xStart+xSize);
    }
    
    public void setPosition(double xStartN, double yStartN, double xSizeN, double ySizeN) {
        if (getRow()==null) setRow(new DasRow((DasCanvas)this.getParent(),0.0, 0.0));
        if (getColumn()==null) setColumn(new DasColumn((DasCanvas)this.getParent(),0.0, 0.0));
        getRow().setPosition(yStartN,yStartN+ySizeN);
        getColumn().setPosition(xStartN,xStartN+xSizeN);
    }
    
    public void setPosition( DasRow row, DasColumn column ) {
        setRow(row);
        setColumn(column);
    }
    
    public void setRow(DasRow row) {
        /*
        if (getRow() != null)
            getRow().removepwUpdateListener(rebinListener);
         */
        super.setRow(row);
        /*
        if (row != null)
            row.addpwUpdateListener(rebinListener);
         */
        if (xAxis!=null) xAxis.setRow(row);
        if (yAxis!=null) yAxis.setRow(row);
    }
    
    public void setColumn(DasColumn column) {
        /*
        if (getColumn() != null)
            getColumn().removepwUpdateListener(rebinListener);
         */
        super.setColumn(column);
        /*
        if (column != null)
            column.addpwUpdateListener(rebinListener);
         */
        if (xAxis!=null) xAxis.setColumn(column);
        if (yAxis!=null) yAxis.setColumn(column);
    }
    
    public void setXAxis(DasAxis xAxis) {
        Object oldValue = this.xAxis;
        Container parent = getParent();
        if (this.xAxis != null) {
            edu.uiowa.physics.pw.das.util.DasDie.println("setXAxis upsets the dmia");
            if (parent != null) parent.remove(this.xAxis);
            xAxis.removePropertyChangeListener("minimum", rebinListener);
            xAxis.removePropertyChangeListener("maximum", rebinListener);
            xAxis.removePropertyChangeListener("log", rebinListener);
        }
        this.xAxis =xAxis;
        if(xAxis != null) {
            if (!xAxis.isHorizontal()) {
                throw new IllegalArgumentException("xAxis is not horizontal");
            }
            if (parent != null) parent.add(this.xAxis);
            xAxis.addPropertyChangeListener("minimum", rebinListener);
            xAxis.addPropertyChangeListener("maximum", rebinListener);
            xAxis.addPropertyChangeListener("log", rebinListener);
        }
        if (xAxis != oldValue) firePropertyChange("xAxis", oldValue, xAxis);
    }
    
    public void setYAxis(DasAxis yAxis) {
        Object oldValue = this.yAxis;
        Container parent = getParent();
        if (this.yAxis != null) {
            edu.uiowa.physics.pw.das.util.DasDie.println("setYAxis upsets the dmia");
            if (parent != null) parent.remove(this.yAxis);
            yAxis.removePropertyChangeListener("minimum", rebinListener);
            yAxis.removePropertyChangeListener("maximum", rebinListener);
            yAxis.removePropertyChangeListener("log", rebinListener);
        }
        this.yAxis = yAxis;
        if (yAxis != null) {
            if (yAxis.isHorizontal()) {
                throw new IllegalArgumentException("yAxis is not vertical");
            }
            if (parent != null) parent.add(this.yAxis);
            yAxis.addPropertyChangeListener("minimum", rebinListener);
            yAxis.addPropertyChangeListener("maximum", rebinListener);
            yAxis.addPropertyChangeListener("log", rebinListener);
        }
        if (yAxis != oldValue) firePropertyChange("yAxis", oldValue, yAxis);
    }
    
    protected void drawInvalid() {
        //  grey out the content while it is not consistent with the axes.
        Graphics2D g= (Graphics2D)getGraphics();
        
        if (g==null) return;
        
        g.translate(-getX(), -getY());
        
        java.awt.geom.Rectangle2D.Double r= DasRow.toRectangle(getRow(),getColumn());
        r.width= r.width-1;
        r.height= r.height-1;
        r.y= r.y+1;
        Color c0= g.getColor();
        g.setColor(new Color(245,245,245,220)); // mostly opaque
        
        g.fill(r);
        g.dispose();
    }
    
    protected void updateImmediately() {
        if (dataSetDescriptor==null) {
            edu.uiowa.physics.pw.das.util.DasDie.println("DataSetDescriptor is null");
        } else {
            loadDataSet();
        }
        for (int i=0; i<renderers.size(); i++) {
            Renderer rend= (Renderer)renderers.get(i);
            rend.update(xAxis,yAxis);
        }
    }
    
    DataRequestThread drt;
    
    DasProgressPanel progressPanel;
    
    protected void loadDataSet() {
        if (getXAxis() instanceof DasTimeAxis) {
            
            final Component parent= getParent();
            final Cursor cursor0= null;
            if (parent != null) {
                parent.getCursor();
                parent.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            }
            
            //drawInvalid();
            
            if (parent != null) {
                ((DasCanvas)parent).lockDisplay(this);
            }
            
            Datum dataRange1 = getXAxis().getDataMaximum().subtract(getXAxis().getDataMinimum());
            double dataRange= dataRange1.convertTo(Units.seconds).doubleValue();
            double deviceRange = Math.floor(getColumn().getDMaximum() + 0.5) - Math.floor(getColumn().getDMinimum() + 0.5);
            double resolution =  dataRange/deviceRange;
            DasTimeAxis taxis = (DasTimeAxis)getXAxis();
            if (progressPanel == null) {
                progressPanel = new DasProgressPanel();
                ((Container)((DasCanvas)getParent()).getGlassPane()).add(progressPanel);
            }
            progressPanel.setSize(progressPanel.getPreferredSize());
            
            if (this.getX()==0) {
                progressPanel.setVisible(false);
            } else {
                progressPanel.setLocation(this.getX() + (this.getWidth()-progressPanel.getWidth())/2,
                this.getY() + (this.getHeight()-progressPanel.getHeight())/2);
            }
            dataSetDescriptor.addDasReaderListener(progressPanel);
            dataSetDescriptor.setProgressIndicator(progressPanel);
            DataRequestor requestor = new DataRequestor() {
                public void currentByteCount(int byteCount) {
                }
                public void totalByteCount(int byteCount) {
                }
                public void exception(Exception exception) {
                    if (!(exception instanceof InterruptedIOException)) {
                        Object[] message = {"Error reading data set", new JEditorPane("text/html", exception.getMessage())};
                        ((JEditorPane)message[1]).setEditable(false);
                        //JOptionPane.showMessageDialog(DasPlot.this, message);
                        DasExceptionHandler.handle(exception);
                        finished(null);
                    }
                }
                public void finished(DataSet ds) {
                    if (dataSetDescriptor != null) dataSetDescriptor.removeDasReaderListener(progressPanel);
                    progressPanel.setVisible(false);
                    if (parent != null) {
                        parent.setCursor(cursor0);
                    }
                    Data = ds;
                    updatePlotImage();
                    if (parent != null) {
                        ((DasCanvas)parent).freeDisplay(this);
                    }
                }
            };
            if (drt == null) {
                drt = new DataRequestThread();
            }
            try {
                drt.request(dataSetDescriptor, "", taxis.getTimeMinimum(), taxis.getTimeMaximum(), resolution, requestor);
            }
            catch (InterruptedException ie) {
                DasExceptionHandler.handle(ie);
            }
            //dataSetDescriptor.getDataSet("", taxis.getTimeMinimum(), taxis.getTimeMaximum(), resolution );
            
        } else {
            if ( ( dataSetDescriptor instanceof ConstantDataSetDescriptor ) ) {
                try {
                    Data= dataSetDescriptor.getDataSet( null, null, null );
                    updatePlotImage();
                } catch ( DasException e ) {
                    DasExceptionHandler.handle(e);
                }
            } else {
                throw new AssertionError( "axis not a timeAxis, and DataSetDescriptor is not constant" );
            }
        }
    }
    
    protected void updatePlotImage() {}
    
    protected void paintComponent(Graphics graphics1) {
        
        Graphics2D graphics= (Graphics2D)graphics1;
        graphics.setRenderingHints(edu.uiowa.physics.pw.das.DasProperties.getRenderingHints());
        Dimension d;
        
        int x = (int)Math.floor(getColumn().getDMinimum() + 0.5);
        int y = (int)Math.floor(getRow().getDMinimum() + 0.5);
        int xSize= (int)Math.floor(getColumn().getDMaximum() + 0.5) - x;
        int ySize= (int)Math.floor(getRow().getDMaximum() + 0.5) - y;
        
        graphics.translate(-getX(), -getY());
        
        Graphics2D plotGraphics = (Graphics2D)graphics.create(x-1, y-1, xSize+2, ySize+2);
        plotGraphics.translate(-x + 1, -y + 1);
        
        //Color color0= plotGraphics.getColor();
        //plotGraphics.setColor(Color.lightGray);
        //plotGraphics.fill(DasRow.toRectangle(getRow(),getColumn()));
        //plotGraphics.setColor(color0);
        
        drawContent(plotGraphics);
        
        for ( int i=0; i<renderers.size(); i++ ) {
            Renderer rend= (Renderer)renderers.get(i);
            rend.render(plotGraphics,xAxis,yAxis);
        }
        
        graphics.setColor(Color.black);
        graphics.drawRect(x-1, y-1, xSize + 1, ySize + 1);
        //graphics.drawLine(x, y, x+xSize, y);
        //graphics.drawLine(x + xSize-1, y, x + xSize-1, y+ySize);
        
        if (plotTitle != null && plotTitle.length() != 0) {
            Font font = getFont();
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setAlignment(GrannyTextRenderer.CENTER_ALIGNMENT);
            gtr.setString(this, plotTitle);
            int titleWidth = (int)gtr.getWidth();
            DasColumn column = getColumn();
            int titleX = x + (xSize-titleWidth)/2;
            int titleY = y - (int)gtr.getDescent() - (int)gtr.getAscent() / 2;
            gtr.draw(graphics, (float)titleX, (float)titleY);
        }
        
    }
    
    protected void drawContent(Graphics2D g) {
        //if (plotImage==null) updateImmediately();
        if (plotImage!=null) {
            int x = (int)Math.floor(getColumn().getDMinimum() + 0.5);
            int y = (int)Math.floor(getRow().getDMinimum() + 0.5);
            g.drawImage(plotImage, x, y+1,this);
        }
    }
    
    public void resize() {
        Font f = getFont();
        
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(this, getTitle());
        
        int titleHeight = (int)gtr.getHeight() + (int)gtr.getAscent() / 2;
        
        Rectangle bounds = new Rectangle();
        bounds.x = (int)Math.floor(getColumn().getDMinimum() + 0.5) - 1;
        bounds.y = (int)Math.floor(getRow().getDMinimum() + 0.5) - 1;
        bounds.width = (int)Math.floor(getColumn().getDMaximum() + 0.5) - bounds.x + 1;
        bounds.height = (int)Math.floor(getRow().getDMaximum() + 0.5) - bounds.y + 1;
        if (!getTitle().equals("")) {
            bounds.y -= titleHeight;
            bounds.height += titleHeight;
        }
        setBounds(bounds);
    }
    
    /** Sets the title which will be displayed above this plot.
     *
     * @param plotTitle The new title for this plot.
     */
    public void setTitle(String t) {
        Object oldValue = plotTitle;
        plotTitle = t;
        FontMetrics fm = getFontMetrics(getFont());
        int titleHeight = fm.getHeight() + fm.getHeight()/2;
        resize();
        repaint(0,0,getWidth(),titleHeight);
        if (t != oldValue) firePropertyChange("title", oldValue, t);
    }
    
    /** Returns the title of this plot.
     *
     * @see #setTitle(String)
     *
     * @return The plot title
     */
    public String getTitle() {
        return plotTitle;
    }
    
    private int button = 0;
    private Point zoomStart;
    private Point zoomEnd;
    private boolean isShiftDown;
    
    private List renderers = null;
    
    public DasAxis getXAxis() {
        return this.xAxis;
    }
    
    public DasAxis getYAxis() {
        return this.yAxis;
    }
    
    /** Getter for property dataSetDescriptor.
     * @return Value of property dataSetDescriptor.
     */
    public DataSetDescriptor getDataSetDescriptor() {
        return dataSetDescriptor;
    }
    
    /** Setter for property dataSetDescriptor.
     * @param dataSetDescriptor New value of property dataSetDescriptor.
     */
    public void setDataSetDescriptor(DataSetDescriptor dataSetDescriptor) {
        this.dataSetDescriptor= dataSetDescriptor;
        markDirty();
    }
    
    public void setData(DataSet ds) {
        this.Data= ds;
        markDirty();
    }
    
    protected class RebinListener implements java.beans.PropertyChangeListener {
        public void propertyChange(java.beans.PropertyChangeEvent e) {
            markDirty();
            DasPlot.this.update();
        }
    }
    
    protected void installComponent() {
        super.installComponent();
        if (xAxis != null) getParent().add(xAxis);
        if (yAxis != null) getParent().add(yAxis);
        Renderer[] r = getRenderers();
        for (int index = 0; index < r.length; index++) {
            r[index].installRenderer();
        }
        if (!"true".equals(System.getProperty("java.awt.headless"))) {
            dndSupport = new PlotDnDSupport(getCanvas().dndSupport);
        }
    }
    
    protected void uninstallComponent() {
        super.uninstallComponent();
        if (xAxis != null) xAxis.getCanvas().remove(xAxis);
        if (yAxis != null) yAxis.getCanvas().remove(yAxis);
        Renderer[] r = getRenderers();
        for (int index = 0; index < r.length; index++) {
            r[index].uninstallRenderer();
        }
    }
    
    public void addRenderer(Renderer rend) {
        if (rend.parent != null) {
            rend.parent.removeRenderer(rend);
        }
        renderers.add(rend);
        rend.parent = this;
        if (getCanvas() != null) {
            rend.installRenderer();
        }
    }
    
    public void removeRenderer(Renderer rend) {
        if (getCanvas() != null) {
            rend.uninstallRenderer();
        }
        renderers.remove(rend);
        rend.parent = null;
    }
    
    public static DasPlot createDummyPlot( DasRow row, DasColumn column) {
        DasAxis xAxis= new DasAxis( new Datum(0), new Datum(10), row, column, DasAxis.HORIZONTAL );
        DasAxis yAxis= new DasAxis( new Datum(0), new Datum(10), row, column, DasAxis.VERTICAL );
        DasPlot result= new DasPlot(xAxis,yAxis,row,column);
        return result;
    }
    
    public Renderer getRenderer(int index) {
        return (Renderer)renderers.get(index);
    }
    
    public Renderer[] getRenderers() {
        return (Renderer[])renderers.toArray(new Renderer[0]);
    }
    
    public static DasPlot processPlotElement(Element element, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        String name = element.getAttribute("name");
        
        DasRow row = (DasRow)form.checkValue(element.getAttribute("row"), DasRow.class, "<row>");
        DasColumn column = (DasColumn)form.checkValue(element.getAttribute("column"), DasColumn.class, "<column>");
        
        DasAxis xAxis = null;
        DasAxis yAxis = null;

        //Get the axes
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
            }
        }
        
        if (xAxis == null) {
            xAxis = (DasAxis)form.checkValue(element.getAttribute("xAxis"), DasAxis.class, "<axis> or <timeaxis>");
        }
        if (yAxis == null) {
            yAxis = (DasAxis)form.checkValue(element.getAttribute("yAxis"), DasAxis.class, "<axis> or <timeaxis>");
        }

        DasPlot plot = new DasPlot(xAxis, yAxis, row, column);
        plot.setTitle(element.getAttribute("title"));

        plot.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, plot);
        
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNodeName().equals("renderers")) {
                    processRenderersElement((Element)node, plot, form);
                }
            }
        }
        
        return plot;
    }
    
    private static DasAxis processXAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element)node;
                if (node.getNodeName().equals("axis")) {
                    DasAxis axis = DasAxis.processAxisElement(e, row, column, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    DasAxis axis = DasTimeAxis.processTimeaxisElement(e, row, column, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    DasAxis axis = DasAxis.processAttachedaxisElement(e, row, column, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                }
            }
        }
        return null;
    }
    
    private static DasAxis processYAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element)node;
                if (node.getNodeName().equals("axis")) {
                    DasAxis axis = DasAxis.processAxisElement(e, row, column, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    DasAxis axis = DasTimeAxis.processTimeaxisElement(e, row, column, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    DasAxis axis = DasAxis.processAttachedaxisElement(e, row, column, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                }
            }
        }
        return null;
    }
    
    private static void processRenderersElement(Element element, DasPlot parent, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        NodeList children = element.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element) {
                if (node.getNodeName().equals("spectrogram")) {
                    parent.addRenderer(SpectrogramRenderer.processSpectrogramElement((Element)node, parent, form));
                }
                else if (node.getNodeName().equals("lineplot")) {
                    parent.addRenderer(SymbolLineRenderer.processLinePlotElement((Element)node, parent, form));
                }
            }
        }
    }
    
    public Element getDOMElement(Document document) {
        
        Element element = document.createElement("plot");
        element.setAttribute("name", getDasName());
        element.setAttribute("row", getRow().getDasName());
        element.setAttribute("column", getColumn().getDasName());
        element.setAttribute("title", getTitle());
        
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

        Renderer[] renderers = getRenderers();
        if (renderers.length > 0) {
            Element renderersChild = document.createElement("renderers");
            for (int index = 0; index < renderers.length; index++) {
                if (renderers[index] instanceof SpectrogramRenderer) {
                    renderersChild.appendChild(((SpectrogramRenderer)renderers[index]).getDOMElement(document));
                }
                else if (renderers[index] instanceof SymbolLineRenderer) {
                    renderersChild.appendChild(((SymbolLineRenderer)renderers[index]).getDOMElement(document));
                }
            }
            element.appendChild(renderersChild);
        }
        return element;
    }
    
    public static DasPlot createNamedPlot(String name) {
        DasAxis xAxis = DasTimeAxis.createNamedTimeAxis(null);
        xAxis.setOrientation(DasAxis.BOTTOM);
        DasAxis yAxis = DasAxis.createNamedAxis(null);
        yAxis.setOrientation(DasAxis.LEFT);
        DasPlot plot = new DasPlot(xAxis, yAxis, null, null);
        if (name == null) {
            name = "plot_" + Integer.toHexString(System.identityHashCode(plot));
        }
        try {
            plot.setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        return plot;
    }
    
    private class PlotDnDSupport extends edu.uiowa.physics.pw.das.util.DnDSupport {
        
        PlotDnDSupport(edu.uiowa.physics.pw.das.util.DnDSupport parent) {
            super(DasPlot.this, DnDConstants.ACTION_COPY_OR_MOVE, parent);
        }
        
        public void drop(DropTargetDropEvent dtde) {
        }
        
        protected int canAccept(DataFlavor[] flavors, int x, int y, int action) {
            for (int i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(TransferableRenderer.RENDERER_FLAVOR)) {
                    return action;
                }
            }
            return -1;
        }
        
        protected void done() {
        }
        
        protected boolean importData(Transferable t, int x, int y, int action) {
            boolean success = false;
            try {
                Renderer r = (Renderer)t.getTransferData(TransferableRenderer.RENDERER_FLAVOR);
                addRenderer(r);
                revalidate();
                success = true;
            }
            catch (UnsupportedFlavorException ufe) {
            }
            catch (IOException ioe) {
            }
            return success;
        }
        
        protected Transferable getTransferable(int x, int y, int action) {
            return null;
        }
        
        protected void exportDone(Transferable t, int action) {
        }
        
    }
    
    public ProgressIndicator getProgressIndicator() {
        return progressPanel;
    }
    
    public Shape getActiveRegion() {
        return getBounds();
    }
    
}
