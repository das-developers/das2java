/* File: DasPlot.java
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
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.graph.dnd.TransferableRenderer;
import edu.uiowa.physics.pw.das.util.*;
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
import java.awt.event.*;
import java.io.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.*;
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
    
    public DasPlot(DasAxis xAxis, DasAxis yAxis) {
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
            
            JMenuItem dumpMenuItem= new JMenuItem("Dump Data Set To File");
            dumpMenuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (renderers.isEmpty()) return;
                    Renderer renderer = (Renderer)renderers.get(0);
                    JFileChooser chooser = new JFileChooser();
                    int result = chooser.showSaveDialog(DasPlot.this);
                    if (result == JFileChooser.APPROVE_OPTION) {
                        File selected = chooser.getSelectedFile();
                        try {
                            FileChannel out = new FileOutputStream(selected).getChannel();
                            DataSet ds = renderer.getDataSet();
                            if (ds instanceof TableDataSet) {
                                TableUtil.dumpToAsciiStream((TableDataSet)ds, out);
                            }
                            else if (ds instanceof VectorDataSet) {
                                VectorUtil.dumpToAsciiStream((VectorDataSet)ds, out);
                            }
                        }
                        catch (IOException ioe) {
                            DasExceptionHandler.handle(ioe);
                        }
                    }
                }
            });
            mouseAdapter.addMenuItem(dumpMenuItem);
        }
    }
    
    public DataSet getDataSet() {
        return Data;
    }
    
    public DataSet getData() {
        return Data;
    }
    
    public void setXAxis(DasAxis xAxis) {
        Object oldValue = this.xAxis;
        Container parent = getParent();
        if (this.xAxis != null) {
            DasProperties.getLogger().fine("setXAxis upsets the dmia");
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
            DasProperties.getLogger().fine("setYAxis upsets the dmia");
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
        
        java.awt.Rectangle r= DasRow.toRectangle(getRow(),getColumn());
        r.width= r.width-1;
        r.height= r.height-1;
        r.y= r.y+1;
        g.setColor(new Color(245,245,245,220)); // mostly opaque
        
        g.fill(r);
        g.dispose();
    }
    
    protected void updateImmediately() {
        if (dataSetDescriptor==null) {
        } else {
            loadDataSet();
        }
        for (int i=0; i<renderers.size(); i++) {
            Renderer rend= (Renderer)renderers.get(i);
            rend.update();
        }
    }
    
    DataRequestThread drt;
    
    DasProgressPanel progressPanel;
    
    protected void loadDataSet() {
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
        double dataRange= dataRange1.doubleValue(Units.seconds);
        double deviceRange = Math.floor(getColumn().getDMaximum() + 0.5) - Math.floor(getColumn().getDMinimum() + 0.5);
        double resolution =  dataRange/deviceRange;
        if (progressPanel == null) {
            progressPanel = DasProgressPanel.createComponentPanel(this,"loading data set");
        }
        
        DataRequestor requestor = new DataRequestor() {
            public void exception(Exception exception) {
                if (!(exception instanceof InterruptedIOException)) {
                    DasExceptionHandler.handle(exception);
                    finished(null);
                }
            }
            public void finished(DataSet ds) {
                progressPanel.setVisible(false);
                if (parent != null) {
                    parent.setCursor(cursor0);
                }
                Data = ds;
                try {
                    updatePlotImage();
                }
                catch (DasException de) {
                    DasExceptionHandler.handle(de);
                }
                if (parent != null) {
                    ((DasCanvas)parent).freeDisplay(this);
                }
            }
        };
        if (drt == null) {
            drt = new DataRequestThread();
        }
        try {
            drt.request(dataSetDescriptor, xAxis.getDataMinimum(), xAxis.getDataMaximum(), Datum.create(resolution,Units.seconds), requestor, progressPanel);
            try {
                updatePlotImage();
            } catch ( DasException de ) {
                DasExceptionHandler.handle(de);
            }
        }
        catch (InterruptedException ie) {
            DasExceptionHandler.handle(ie);
        }
    }
    
    protected void updatePlotImage() throws DasException {}
    
    protected void paintComponent(Graphics graphics1) {
        
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        int xSize= getColumn().getDMaximum() - x;
        int ySize= getRow().getDMaximum() - y;
        
        Rectangle clip= graphics1.getClipBounds();
        if ( ( clip.y + getY() ) >= ( y + ySize ) ) {
            return;
        }
        
        Graphics2D graphics= (Graphics2D)graphics1;
        
        graphics.setRenderingHints(edu.uiowa.physics.pw.das.DasProperties.getRenderingHints());
        graphics.translate(-getX(), -getY());
        
        Graphics2D plotGraphics = (Graphics2D)graphics.create(x-1, y-1, xSize+2, ySize+2);
        plotGraphics.translate(-x + 1, -y + 1);
        
        drawContent(plotGraphics);
        
        for ( int i=0; i<renderers.size(); i++ ) {
            Renderer rend= (Renderer)renderers.get(i);
            rend.render(plotGraphics,xAxis,yAxis);
        }
        
        graphics.setColor(Color.black);
        graphics.drawRect(x-1, y-1, xSize + 1, ySize + 1);
        
        if (plotTitle != null && plotTitle.length() != 0) {
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setAlignment(GrannyTextRenderer.CENTER_ALIGNMENT);
            gtr.setString(this, plotTitle);
            int titleWidth = (int)gtr.getWidth();
            int titleX = x + (xSize-titleWidth)/2;
            int titleY = y - (int)gtr.getDescent() - (int)gtr.getAscent() / 2;
            gtr.draw(graphics, (float)titleX, (float)titleY);
        }
        
    }
    
    protected void drawContent(Graphics2D g) {
        //if (plotImage==null) updateImmediately();
        if (plotImage!=null) {
            int x = getColumn().getDMinimum();
            int y = getRow().getDMinimum();
            g.drawImage(plotImage, x, y+1,this);
        }
    }
    
    public void resize() {
        if (isDisplayable()) {
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(this, getTitle());
            
            int titleHeight = (int)gtr.getHeight() + (int)gtr.getAscent() / 2;
            
            Rectangle bounds = new Rectangle();
            bounds.x = getColumn().getDMinimum() - 1;
            bounds.y = getRow().getDMinimum() - 1;
            bounds.width = getColumn().getDMaximum() - bounds.x + 1;
            bounds.height = getRow().getDMaximum() - bounds.y + 1;
            if (!getTitle().equals("")) {
                bounds.y -= titleHeight;
                bounds.height += titleHeight;
            }
            setBounds(bounds);
        }
    }
    
    /** Sets the title which will be displayed above this plot.
     *
     * @param t The new title for this plot.
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
            //            DasApplication.getDefaultApplication().getLogger().info("rebin listener got property change: "+e.getNewValue());
            markDirty();
            DasPlot.this.update();
        }
    }
    
    protected void installComponent() {
        super.installComponent();
        if (xAxis != null) getCanvas().add(xAxis, getRow(), getColumn());
        if (yAxis != null) getCanvas().add(yAxis, getRow(), getColumn());
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
        markDirty();
        repaint();
    }
    
    public void removeRenderer(Renderer rend) {
        if (getCanvas() != null) {
            rend.uninstallRenderer();
        }
        renderers.remove(rend);
        rend.parent = null;
    }
    
    public static DasPlot createDummyPlot() {
        DasAxis xAxis= new DasAxis( Datum.create(-10), Datum.create(10), DasAxis.HORIZONTAL );
        DasAxis yAxis= new DasAxis( Datum.create(-10), Datum.create(10), DasAxis.VERTICAL );
        DasPlot result= new DasPlot(xAxis,yAxis);
        return result;
    }
    
    public Renderer getRenderer(int index) {
        return (Renderer)renderers.get(index);
    }
    
    public Renderer[] getRenderers() {
        return (Renderer[])renderers.toArray(new Renderer[0]);
    }
    
    public static DasPlot processPlotElement(Element element, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
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
        
        DasPlot plot = new DasPlot(xAxis, yAxis);
        plot.setTitle(element.getAttribute("title"));
        plot.setDasName(name);
        plot.setRow(row);
        plot.setColumn(column);
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
    
    private static DasAxis processXAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                Element e = (Element)node;
                if (node.getNodeName().equals("axis")) {
                    DasAxis axis = DasAxis.processAxisElement(e, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    DasAxis axis = DasAxis.processTimeaxisElement(e, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    DasAxis axis = DasAxis.processAttachedaxisElement(e, form);
                    if (!axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.HORIZONTAL);
                    }
                    return axis;
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
                    DasAxis axis = DasAxis.processAxisElement(e, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("timeaxis")) {
                    DasAxis axis = DasAxis.processTimeaxisElement(e, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                }
                else if (node.getNodeName().equals("attachedaxis")) {
                    DasAxis axis = DasAxis.processAttachedaxisElement(e, form);
                    if (axis.isHorizontal()) {
                        axis.setOrientation(DasAxis.VERTICAL);
                    }
                    return axis;
                }
            }
        }
        return null;
    }
    
    private static void processRenderersElement(Element element, DasPlot parent, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
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
        DasAxis xAxis = DasAxis.createNamedAxis(null);
        xAxis.setOrientation(DasAxis.BOTTOM);
        DasAxis yAxis = DasAxis.createNamedAxis(null);
        yAxis.setOrientation(DasAxis.LEFT);
        DasPlot plot = new DasPlot(xAxis, yAxis);
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
    
    public DasProgressMonitor getDasProgressMonitor() {
        return progressPanel;
    }
    
    public Shape getActiveRegion() {
        return getBounds();
    }
    
    /** Potentially coalesce an event being posted with an existing
     * event.  This method is called by <code>EventQueue.postEvent</code>
     * if an event with the same ID as the event to be posted is found in
     * the queue (both events must have this component as their source).
     * This method either returns a coalesced event which replaces
     * the existing event (and the new event is then discarded), or
     * <code>null</code> to indicate that no combining should be done
     * (add the second event to the end of the queue).  Either event
     * parameter may be modified and returned, as the other one is discarded
     * unless <code>null</code> is returned.
     * <p>
     * This implementation of <code>coalesceEvents</code> coalesces
     * <code>DasUpdateEvent</code>s, returning the existingEvent parameter
     *
     * @param  existingEvent  the event already on the <code>EventQueue</code>
     * @param  newEvent       the event being posted to the
     * 		<code>EventQueue</code>
     * @return a coalesced event, or <code>null</code> indicating that no
     * 		coalescing was done
     */
    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        if (existingEvent instanceof DasRendererUpdateEvent && newEvent instanceof DasRendererUpdateEvent) {
            DasRendererUpdateEvent e1 = (DasRendererUpdateEvent)existingEvent;
            DasRendererUpdateEvent e2 = (DasRendererUpdateEvent)newEvent;
            if (e1.getRenderer() == e2.getRenderer()) {
                return existingEvent;
            }
        }
        return super.coalesceEvents(existingEvent, newEvent);
    }

    /** Processes events occurring on this component. By default this
     * method calls the appropriate
     * <code>process&lt;event&nbsp;type&gt;Event</code>
     * method for the given class of event.
     * <p>Note that if the event parameter is <code>null</code>
     * the behavior is unspecified and may result in an
     * exception.
     *
     * @param     e the event
     * @see       java.awt.Component#processComponentEvent
     * @see       java.awt.Component#processFocusEvent
     * @see       java.awt.Component#processKeyEvent
     * @see       java.awt.Component#processMouseEvent
     * @see       java.awt.Component#processMouseMotionEvent
     * @see       java.awt.Component#processInputMethodEvent
     * @see       java.awt.Component#processHierarchyEvent
     * @see       java.awt.Component#processMouseWheelEvent
     * @see       #processDasUpdateEvent
     */
    protected void processEvent(AWTEvent e) {
        super.processEvent(e);
        if (e instanceof DasRendererUpdateEvent) {
            DasRendererUpdateEvent drue = (DasRendererUpdateEvent)e;
            drue.getRenderer().updateImmediately();
        }
    }
}
