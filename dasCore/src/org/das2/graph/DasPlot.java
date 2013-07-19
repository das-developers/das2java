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
package org.das2.graph;

import java.util.logging.Level;
import org.das2.event.MouseModule;
import org.das2.event.HorizontalRangeSelectorMouseModule;
import org.das2.event.LengthDragRenderer;
import org.das2.event.DisplayDataMouseModule;
import org.das2.event.CrossHairMouseModule;
import org.das2.event.BoxZoomMouseModule;
import org.das2.event.VerticalRangeSelectorMouseModule;
import org.das2.event.ZoomPanMouseModule;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.VectorUtil;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.TableUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.DasApplication;
import org.das2.CancelledOperationException;
import org.das2.DasProperties;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.DasExceptionHandler;
import org.das2.util.DnDSupport;
import java.beans.PropertyChangeEvent;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumVector;
import org.das2.graph.dnd.TransferableRenderer;
import org.das2.system.DasLogger;
import java.awt.image.BufferedImage;
import javax.swing.event.MouseInputAdapter;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.das2.DasException;
import org.das2.dataset.DataSetAdapter;
import org.das2.datum.DatumRangeUtil;
import org.das2.graph.DasAxis.Memento;

public class DasPlot extends DasCanvasComponent {

    public static final String PROP_TITLE = "title";
    protected DataSetDescriptor dataSetDescriptor;
    private DasAxis xAxis;
    private DasAxis yAxis;
    DasAxis.Memento xmemento;
    DasAxis.Memento ymemento;
    protected String offsetTime = "";
    protected String plotTitle = "";
    protected boolean displayTitle= true;
    protected double[] psym_x;
    protected double[] psym_y;
    protected RebinListener rebinListener = new RebinListener();
    protected transient PropertyChangeListener ticksListener = new PropertyChangeListener() {

        public void propertyChange(PropertyChangeEvent evt) {
            if (drawGrid || drawMinorGrid) {
                invalidateCacheImage();
            }
        }
    };
    DnDSupport dndSupport;
    final static Logger logger = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);
    private JMenuItem editRendererMenuItem;
    /**
     * cacheImage is a cached image that all the renderers have drawn on.  This
     * relaxes the need for renderers' render method to execute in
     * animation-interactive time.
     */
    boolean cacheImageValid = false;
    BufferedImage cacheImage;
    Rectangle cacheImageBounds;
    /**
     * property preview.  If set, the cache image may be scaled to reflect
     * the new axis position in animation-interactive time.
     */
    boolean preview = false;
    //private int repaintCount = 0;
    //private int paintComponentCount = 0;
    private int titleHeight= 0;

    private boolean drawInactiveInLegend= false;

    public DasPlot(DasAxis xAxis, DasAxis yAxis) {
        super();

        addMouseListener(new MouseInputAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                //if (e.getButton() == MouseEvent.BUTTON3) {
                Renderer r = null;
                int ir;
                synchronized ( DasPlot.this ) {
                    ir = findRendererAt(getX() + e.getX(), getY() + e.getY());
                    if ( ir>-1 ) {
                        r= (Renderer) renderers.get(ir);
                    }
                    if ( r==null ) {
                        for ( int i=renderers.size()-1; i>=0; i-- ) {
                            if ( renderers.get(i).isActive()==false ) {
                                r= renderers.get(i);
                                break;
                            }
                        }
                    }
                    setFocusRenderer(r);
                }
                if (editRendererMenuItem != null) {
                    //TODO: check out SwingUtilities, I think this is wrong:
                    editRendererMenuItem.setText("Renderer Properties");
                    if ( ir>-1 && r!=null ) {
                        editRendererMenuItem.setEnabled(true);
                        editRendererMenuItem.setIcon(r.getListIcon());
                    } else {
                        editRendererMenuItem.setEnabled(false);
                        editRendererMenuItem.setIcon(null);
                    }
                }
            //}
            }
        });

        setOpaque(false);

        this.renderers = new ArrayList();
        this.xAxis = xAxis;
        if (xAxis != null) {
            if (!xAxis.isHorizontal()) {
                throw new IllegalArgumentException("xAxis is not horizontal");
            }
            xAxis.addPropertyChangeListener("dataMinimum", rebinListener);
            xAxis.addPropertyChangeListener("dataMaximum", rebinListener);
            xAxis.addPropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            xAxis.addPropertyChangeListener("log", rebinListener);
            xAxis.addPropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }
        this.yAxis = yAxis;
        if (yAxis != null) {
            if (yAxis.isHorizontal()) {
                throw new IllegalArgumentException("yAxis is not vertical");
            }
            yAxis.addPropertyChangeListener("dataMinimum", rebinListener);
            yAxis.addPropertyChangeListener("dataMaximum", rebinListener);
            yAxis.addPropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            yAxis.addPropertyChangeListener("log", rebinListener);
            yAxis.addPropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }

        if (!"true".equals(DasApplication.getProperty("java.awt.headless", "false"))) {
            addDefaultMouseModules();
        }
    }
    /**
     * returns the Renderer with the current focus.  Clicking on a trace sets the focus.
     */
    protected Renderer focusRenderer = null;
    public static final String PROP_FOCUSRENDERER = "focusRenderer";

    public Renderer getFocusRenderer() {
        return focusRenderer;
    }

    public void setFocusRenderer(Renderer focusRenderer) {
        Renderer oldFocusRenderer = this.focusRenderer;
        this.focusRenderer = focusRenderer;
        firePropertyChange(PROP_FOCUSRENDERER, null, focusRenderer);
        //firePropertyChange(PROP_FOCUSRENDERER, oldFocusRenderer, focusRenderer);
    }

    /**
     * returns the bounds of the legend, or null if there is no legend.
     * @param graphics
     * @param msgx
     * @param msgy
     * @return
     */
    private Rectangle getLegendBounds( Graphics2D graphics, int msgx, int msgy ) {
        int maxIconWidth = 0;

        Rectangle mrect;
        Rectangle boundRect=null;

        int em = (int) getEmSize();

        if ( legendElements==null ) return null;
        if ( graphics==null ) return null;

        for (int i = 0; i < legendElements.size(); i++) {
            LegendElement le = legendElements.get(i);
            if ( ( le.renderer!=null && le.renderer.isActive() ) || drawInactiveInLegend ) {
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                gtr.setString(graphics, String.valueOf(le.label).trim()); // protect from nulls, which seems to happen
                mrect = gtr.getBounds();
                maxIconWidth = Math.max(maxIconWidth, le.icon.getIconWidth());
                int theheight = Math.max(mrect.height, le.icon.getIconHeight());
                mrect.translate(msgx, msgy + (int) gtr.getAscent() );
                mrect.height= theheight;
                if (boundRect == null) {
                    boundRect = mrect;
                } else {
                    boundRect.add(mrect);
                }
                msgy += theheight;
            }
        }

        if ( boundRect==null ) return null;
        int iconColumnWidth = maxIconWidth + em / 4;
        mrect = new Rectangle(boundRect);
        mrect.width += iconColumnWidth;
        if ( legendPosition==LegendPosition.NE || legendPosition==LegendPosition.NW ) {
            mrect.y= yAxis.getRow().getDMinimum() + em/2;
            if ( legendPosition==LegendPosition.NE ) {
                mrect.x = xAxis.getColumn().getDMaximum() - em - mrect.width;

            } else if ( legendPosition==LegendPosition.NW ) {
                mrect.x = xAxis.getColumn().getDMinimum() + em ;
            }
        } else if ( legendPosition==LegendPosition.SE || legendPosition==LegendPosition.SW ) {
            mrect.y=  yAxis.getRow().getDMaximum() - boundRect.height - em; // note em not em/2 is intentional
            if ( legendPosition==LegendPosition.SE ) {
                mrect.x = xAxis.getColumn().getDMaximum() - em - mrect.width;

            } else if ( legendPosition==LegendPosition.SW ) {
                mrect.x = xAxis.getColumn().getDMinimum() + em ;
            }

        } else if ( legendPosition==LegendPosition.OutsideNE ) {
            mrect.x = xAxis.getColumn().getDMaximum() + em + maxIconWidth;
            boundRect.x = mrect.x;
            mrect.y= yAxis.getRow().getDMinimum(); // em/5 determined by experiment.

        } else {
            throw new IllegalArgumentException("not supported: "+legendPosition);
        }

        Rectangle axisBounds= DasDevicePosition.toRectangle( getRow(), getColumn() );
        axisBounds.width= Math.max( axisBounds.width, mrect.x+mrect.width-axisBounds.x ); // don't limit width because of outside NE
        Rectangle2D rr= mrect.createIntersection(axisBounds);

        return new Rectangle( (int)rr.getX(),(int)rr.getY(),(int)rr.getWidth(),(int)rr.getHeight() );
    }


    private void drawLegend(Graphics2D g ) {

        Graphics2D graphics= (Graphics2D) g.create();

        int em;
        int msgx, msgy;

        Color backColor = GraphUtil.getRicePaperColor();
        Rectangle mrect;

        em = (int) getEmSize();

        msgx = xAxis.getColumn().getDMiddle() + em;
        msgy = yAxis.getRow().getDMinimum() + em/2;

        int maxIconWidth= 0;
        for (int i = 0; i < legendElements.size(); i++) {
            LegendElement le = legendElements.get(i);
            maxIconWidth = Math.max(maxIconWidth, le.icon.getIconWidth());
        }

        mrect= getLegendBounds(graphics,msgx,msgy);
        if ( mrect==null ) return; // nothing is active

        msgx= mrect.x;
        msgy= mrect.y;
        if ( legendPosition!=LegendPosition.OutsideNE ) {
            msgx+= maxIconWidth + em/4;
        }

        if ( legendPosition!=LegendPosition.OutsideNE ) {
            graphics.setColor(backColor);
            graphics.fillRoundRect(mrect.x - em / 4, mrect.y - em/4, mrect.width + em / 2, mrect.height + em/2, 5, 5);
            graphics.setColor(getForeground());
            graphics.drawRoundRect(mrect.x - em / 4, mrect.y - em/4, mrect.width + em / 2, mrect.height + em/2, 5, 5);
        }

        for (int i = 0; i < legendElements.size(); i++) {
            LegendElement le = legendElements.get(i);

            if ( ( le.renderer!=null && le.renderer.isActive() ) || drawInactiveInLegend ) {
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                gtr.setString(graphics, String.valueOf(le.label).trim()); // protect from nulls, which seems to happen
                mrect = gtr.getBounds();
                mrect.translate(msgx, msgy + (int) gtr.getAscent());
                int theheight= Math.max(mrect.height, le.icon.getIconHeight());
                int icony= theheight/2 - le.icon.getIconHeight() / 2;  // from top of rectangle
                int texty= theheight/2 - (int)gtr.getHeight() / 2 + (int) gtr.getAscent();
                gtr.draw( graphics, msgx, msgy + texty );
                mrect.height = theheight;
                Rectangle imgBounds = new Rectangle(
                        msgx - (le.icon.getIconWidth() + em / 4),
                        msgy + icony,
                        le.icon.getIconWidth(),
                        le.icon.getIconHeight() );
                boolean printing= true;
                if ( printing ) {
                    le.drawIcon( graphics, msgx - (le.icon.getIconWidth() + em / 4), msgy + icony );
                } else {
                    graphics.drawImage(le.icon.getImage(), imgBounds.x, imgBounds.y, null);
                }
                msgy += mrect.getHeight();
                mrect.add(imgBounds);
                if ( msgy > getRow().bottom() ) break;
                le.bounds = mrect;
            }
        }

        graphics.dispose();
        

    }

     private void drawMessages(Graphics2D g) {

        Graphics2D graphics= (Graphics2D) g.create();
        
        Font font0 = graphics.getFont();
        int msgem = (int) Math.max(8, font0.getSize2D() / 2);
        graphics.setFont(font0.deriveFont((float) msgem));
        int em = (int) getEmSize();

        boolean rightJustify= false;
        int msgx = xAxis.getColumn().getDMinimum() + em;
        int msgy = yAxis.getRow().getDMinimum() + em;

        if ( legendPosition==LegendPosition.NW ) {
            rightJustify= true;
            msgx= xAxis.getColumn().getDMaximum() - em;
        }

        Color warnColor = new Color(255, 255, 100, 200);
        Color severeColor = new Color(255, 140, 140, 200);

        List<Renderer> renderers1=  Arrays.asList( getRenderers() );
        List<MessageDescriptor> lmessages= new ArrayList( this.messages ); // copy to local variable

        for (int i = 0; i < lmessages.size(); i++) {
            MessageDescriptor message = (MessageDescriptor) lmessages.get(i);

            if ( message.messageType<logLevel.intValue() ) {
                continue; // skip this message
            }

            Icon icon=null;
            if ( message.renderer!=null && renderers1.size()>1 ) {
                icon= message.renderer.getListIcon();
            }

            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(graphics, String.valueOf(message.text)); // protect from nulls, which seems to happen

            Rectangle mrect = gtr.getBounds();

            if ( icon!=null && mrect.height<icon.getIconHeight() ) { // spectrograms have icons taller than text
                mrect.height= icon.getIconHeight();
                //TODO: adjust y position: mrect.y= mrect.height / 2 - msgem - msgem ;
            }

            int spc= 2;

            if ( icon!=null ) {
                mrect.width+= icon.getIconWidth() + spc;
            }

            int msgx1= msgx;
            if ( rightJustify ) {
                msgx1= msgx - (int) mrect.getWidth();
            }
            mrect.translate(msgx1, msgy);
            Color backColor = GraphUtil.getRicePaperColor();
            if (message.messageType == DasPlot.WARNING) {
                backColor = warnColor;
            } else if (message.messageType == DasPlot.SEVERE) {
                backColor = severeColor;
            }
            graphics.setColor(backColor);
            graphics.fillRoundRect(mrect.x - em / 4, mrect.y, mrect.width + em / 2, mrect.height, 5, 5);
            graphics.setColor(getForeground());

            if ( icon!=null ) icon.paintIcon( this, g, mrect.x, mrect.y  );
            
            graphics.drawRoundRect(mrect.x - em / 4, mrect.y, mrect.width + em / 2, mrect.height, 5, 5);
            if ( icon!=null ) {
                gtr.draw(graphics, msgx1 + icon.getIconWidth() + spc, msgy);
            } else {
                gtr.draw(graphics, msgx1 , msgy);
            }
            message.bounds = mrect;

            msgy += gtr.getHeight() + msgem / 2;  // findbugs OKAY
        }

        graphics.dispose();
        
    }

    private void maybeDrawGrid(Graphics2D plotGraphics) {
        Color gridColor = new Color(128, 128, 128, 70);
        Color minorGridColor = new Color(128, 128, 128, 40);

        TickVDescriptor xtickv= getXAxis().getTickV();
        TickVDescriptor ytickv= getYAxis().getTickV();

        if (drawMinorGrid) {
            DatumVector xticks = null;
            DatumVector yticks = null;
            if ( xtickv!=null ) {
                xticks = xtickv.getMinorTicks();
            }
            if ( ytickv!=null ) {
                yticks = ytickv.getMinorTicks();
            }
            plotGraphics.setColor(minorGridColor);
            drawGrid(plotGraphics, xticks, yticks);
        }

        if (drawGrid) {
            DatumVector xticks = null;
            DatumVector yticks = null;
            if ( xtickv!=null ) {
                xticks = xtickv.getMajorTicks();
            }
            if ( ytickv!=null ) {
                yticks = ytickv.getMajorTicks();
            }
            plotGraphics.setColor(gridColor);
            drawGrid(plotGraphics, xticks, yticks);
        }

    }

    private void drawCacheImage(Graphics2D plotGraphics) {

        /* clear all the messages */
        messages = new ArrayList();
        legendElements = new ArrayList<LegendElement>();

        if (!gridOver) {
            maybeDrawGrid(plotGraphics);
        }
        drawContent(plotGraphics);

        List<Renderer> renderers1= Arrays.asList(getRenderers());

        boolean noneActive = true;
        for (int i = 0; i < renderers1.size(); i++) {
            Renderer rend = (Renderer) renderers1.get(i);
            if (rend.isActive()) {
                logger.log(Level.FINEST, "rendering #{0}: {1}", new Object[]{i, rend});
                try {
                    rend.render(plotGraphics, xAxis, yAxis, new NullProgressMonitor());
                } catch ( RuntimeException ex ) {
                    logger.log( Level.WARNING, null, ex );
                    postException(rend,ex);
                }
                noneActive = false;
                if (rend.isDrawLegendLabel()) {
                    addToLegend(rend, (ImageIcon) rend.getListIcon(), 0, rend.getLegendLabel());
                }
            } else {
                if (rend.isDrawLegendLabel()) {
                    addToLegend(rend, (ImageIcon) rend.getListIcon(), 0, rend.getLegendLabel() + " (inactive)");
                }
            }
        }

        if (gridOver) {
            maybeDrawGrid(plotGraphics);
        }
        if (renderers1.isEmpty()) {
            postMessage(null, "(no renderers)", DasPlot.INFO, null, null);
            logger.fine("dasPlot has no renderers");
        } else if (noneActive) {
            postMessage(null, "(no active renderers)", DasPlot.INFO, null, null);
        }
    }

    /**
     * return the index of the renderer at canvas location (x,y), or -1 if
     * no renderer is found at the position.
     */
    public int findRendererAt(int x, int y) {
        List<Renderer> renderers1= Arrays.asList(getRenderers());

        for (int i = 0; messages != null && i < messages.size(); i++) {
            MessageDescriptor message = (MessageDescriptor) messages.get(i);
            if ( message.bounds!=null && message.bounds.contains(x, y) && message.renderer != null) {
                int result = renderers1.indexOf(message.renderer);
                if (result != -1) {
                    return result;
                }
            }
        }

        for (int i = 0; legendElements != null && i < legendElements.size(); i++) {
            LegendElement legendElement = legendElements.get(i);
            if ( legendElement.bounds!=null && legendElement.bounds.contains(x, y) && legendElement.renderer != null) {
                int result = renderers1.indexOf(legendElement.renderer);
                if (result != -1) {
                    return result;
                }
            }
        }

        for (int i = renderers1.size() - 1; i >= 0; i--) {
            Renderer rend = (Renderer) renderers1.get(i);
            if (rend.isActive() && rend.acceptContext(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private Action getEditAction() {
        return new AbstractAction("Renderer Properties") {

            public void actionPerformed(ActionEvent e) {
                Point p = getDasMouseInputAdapter().getMousePressPosition();
                int i = findRendererAt(p.x + getX(), p.y + getY());
                if (i > -1) {
                    Renderer rend = getRenderer(i);
                    PropertyEditor editor = new PropertyEditor(rend);
                    editor.showDialog(DasPlot.this);
                }
            }
        };
    }

    private void addDefaultMouseModules() {

        HorizontalRangeSelectorMouseModule hrs =
                new HorizontalRangeSelectorMouseModule(this, xAxis);
        mouseAdapter.addMouseModule(hrs);
        hrs.addDataRangeSelectionListener(xAxis);
        // TODO: support setYAxis, setXAxis

        VerticalRangeSelectorMouseModule vrs =
                new VerticalRangeSelectorMouseModule(this, yAxis);
        mouseAdapter.addMouseModule(vrs);
        vrs.addDataRangeSelectionListener(yAxis);
        // TODO: support setYAxis, setXAxis

        MouseModule x = CrossHairMouseModule.create(this);
        mouseAdapter.addMouseModule(x);

        mouseAdapter.setSecondaryModule(new ZoomPanMouseModule(this, getXAxis(), getYAxis()));

        mouseAdapter.setPrimaryModule(x);

        mouseAdapter.addMouseModule(new BoxZoomMouseModule(this, null, getXAxis(), getYAxis()));
        // TODO: support setYAxis, setXAxis.

        x = new MouseModule(this, new LengthDragRenderer(this, null, null), "Length");
        mouseAdapter.addMouseModule(x);

        x = new DisplayDataMouseModule(this);
        mouseAdapter.addMouseModule(x);

        setEnableRenderPropertiesAction(true);
        
        if (DasApplication.hasAllPermission()) {
            JMenuItem dumpMenuItem = new JMenuItem(DUMP_TO_FILE_ACTION);
            mouseAdapter.addMenuItem(dumpMenuItem);
        }
    }
    
    public Action DUMP_TO_FILE_ACTION = new AbstractAction("Dump Data Set to File") {

        public void actionPerformed(ActionEvent e) {
            List<Renderer> renderers1= Arrays.asList(getRenderers());
            if (renderers1.isEmpty()) {
                return;
            }
            Renderer renderer = (Renderer) renderers1.get(0);
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(DasPlot.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                try {
                    FileChannel out = new FileOutputStream(selected).getChannel();
                    DataSet ds = DataSetAdapter.createLegacyDataSet( renderer.getDataSet() );
                    if (ds instanceof TableDataSet) {
                        TableUtil.dumpToAsciiStream((TableDataSet) ds, out);
                    } else if (ds instanceof VectorDataSet) {
                        VectorUtil.dumpToAsciiStream((VectorDataSet) ds, out);
                    }
                } catch (IOException ioe) {
                    DasExceptionHandler.handle(ioe);
                }
            }
        }
    };

    public void setXAxis(DasAxis xAxis) {
        Object oldValue = this.xAxis;
        Container parent = getParent();
        if (this.xAxis != null) {
            DasProperties.getLogger().fine("setXAxis upsets the dmia");
            if (parent != null) {
                parent.remove(this.xAxis);
            }
            xAxis.removePropertyChangeListener("dataMinimum", rebinListener);
            xAxis.removePropertyChangeListener("dataMaximum", rebinListener);
            xAxis.removePropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            xAxis.removePropertyChangeListener("log", rebinListener);
            xAxis.removePropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }
        this.xAxis = xAxis;
        if (xAxis != null) {
            if (!xAxis.isHorizontal()) {
                throw new IllegalArgumentException("xAxis is not horizontal");
            }
            if (parent != null) {
                parent.add(this.xAxis);
                parent.validate();
            }
            xAxis.addPropertyChangeListener("dataMinimum", rebinListener);
            xAxis.addPropertyChangeListener("dataMaximum", rebinListener);
            xAxis.addPropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            xAxis.addPropertyChangeListener("log", rebinListener);
            xAxis.addPropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }
        if (xAxis != oldValue) {
            firePropertyChange("xAxis", oldValue, xAxis);
        }
    }

    /**
     * Attaches a new axis to the plot.  The old axis is disconnected, and
     * the plot will listen to the new axis.  Also, the row and column for the
     * axis is set, but this might change in the future.  null appears to be
     * a valid input as well.
     *
     * TODO: plot does not seem to be responding to changes in the axis.
     * (goes grey because updatePlotImage is never done.
     */
    public void setYAxis(DasAxis yAxis) {
        Object oldValue = this.yAxis;
        logger.log(Level.FINE, "setYAxis({0}), removes {1}", new Object[]{ yAxis==null ? "null" : yAxis.getName(), this.yAxis});
        Container parent = getParent();
        if (this.yAxis != null) {
            DasProperties.getLogger().fine("setYAxis upsets the dmia");
            if (parent != null) {
                parent.remove(this.yAxis);
            }
            this.yAxis.removePropertyChangeListener("dataMinimum", rebinListener);
            this.yAxis.removePropertyChangeListener("dataMaximum", rebinListener);
            this.yAxis.removePropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            this.yAxis.removePropertyChangeListener("log", rebinListener);
            this.yAxis.removePropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }
        this.yAxis = yAxis;
        if (yAxis != null) {
            if (yAxis.isHorizontal()) {
                throw new IllegalArgumentException("yAxis is not vertical");
            }
            yAxis.setRow(getRow());
            yAxis.setColumn(getColumn());
            if (parent != null) {
                parent.add(this.yAxis);
                parent.validate();
            }
            yAxis.addPropertyChangeListener("dataMinimum", rebinListener);
            yAxis.addPropertyChangeListener("dataMaximum", rebinListener);
            yAxis.addPropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            yAxis.addPropertyChangeListener("log", rebinListener);
            yAxis.addPropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }
        if (yAxis != oldValue) {
            firePropertyChange("yAxis", oldValue, yAxis);
        }
    }

    @Override
    protected void updateImmediately() {
        //paintImmediately(0, 0, getWidth(), getHeight());
        super.updateImmediately();
        logger.finer("DasPlot.updateImmediately");
        List<Renderer> renderers1= Arrays.asList(getRenderers());
        for (int i = 0; i < renderers1.size(); i++) {
            Renderer rend = (Renderer) renderers1.get(i);
            rend.update();
        }
    }

    /*
     * returns the AffineTransform to transform data from the last updatePlotImage call
     * axes (if super.updatePlotImage was called), or null if the transform is not possible.
     */
    protected AffineTransform getAffineTransform(DasAxis xAxis, DasAxis yAxis) {
        if (xmemento == null) {
            logger.fine("unable to calculate AT, because old transform is not defined.");
            return null;
        } else {
            AffineTransform at = new AffineTransform();
            at = xAxis.getAffineTransform(xmemento, at);
            at = yAxis.getAffineTransform(ymemento, at);
            return at;
        }
    }

    /**
     * at.isIdentity returns false if the at is not precisely identity,
     * so this allows for some fuzz.
     */
    private boolean isIdentity(AffineTransform at) {
        return at.isIdentity() ||
                (Math.abs(at.getScaleX() - 1.00) < 0.001 && Math.abs(at.getScaleY() - 1.00) < 0.001 && Math.abs(at.getTranslateX()) < 0.001 && Math.abs(at.getTranslateY()) < 0.001);
    }

    private void paintInvalidScreen(Graphics atGraphics, AffineTransform at) {
        Color c = GraphUtil.getRicePaperColor();
        atGraphics.setColor(c);
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();

        atGraphics.fillRect(x - 1, y - 1, getWidth(), getHeight());
        final boolean debug = false;
        if (debug) {
            atGraphics.setColor(Color.DARK_GRAY);

            atGraphics.drawString("moment...", x + 10, y + 10);
            String atstr = GraphUtil.getATScaleTranslateString(at);

            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(atGraphics, atstr);
            gtr.draw(atGraphics, x + 10, y + 10 + atGraphics.getFontMetrics().getHeight());
        }

        logger.finest(" using cacheImage with ricepaper to invalidate");
    }

    /**
     * reset the bounds for the cache image.  This is a rectangle indicating where
     * the cache image is in the DasCanvas reference frame.  This should be
     * called from a synchronized block that either recreates the cache image
     * or calls invalidateCacheImage, so that the bounds and the image are consistent.
     *
     * @param printing the bounds should be set for printing, which currently disabled the overSize rendering.
     *
     */
    private synchronized void resetCacheImageBounds(boolean printing) {
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        if (overSize && !printing ) {
            cacheImageBounds = new Rectangle();
            cacheImageBounds.width = 16 * getWidth() / 10;
            cacheImageBounds.height = getHeight();
            cacheImageBounds.x = x - 3 * getWidth() / 10;
            cacheImageBounds.y = y - 1;
        } else {
            cacheImageBounds = new Rectangle();
            cacheImageBounds.width = getWidth();
            cacheImageBounds.height = getHeight();
            if ( cacheImageBounds.width==0 || cacheImageBounds.height==0 ) {
                try {
                    System.err.println("cheesy code to fix getHeight=0 when printing");
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    Logger.getLogger(DasPlot.class.getName()).log(Level.SEVERE, null, ex);
                }
                cacheImageBounds.width = getWidth();
                cacheImageBounds.height = getHeight();
            }            
            if ( cacheImageBounds.width==0 || cacheImageBounds.height==0 ) {
                getWidth();
                getHeight();
                throw new IllegalArgumentException("width or height is 0.");
            }
            cacheImage = new BufferedImage(cacheImageBounds.width, cacheImageBounds.height, BufferedImage.TYPE_4BYTE_ABGR);
            cacheImageBounds.x = x - 1;
            cacheImageBounds.y = y - 1;
        }
    }

    /**
     * we need to call each renderer's updatePlotImage method, otherwise we assume
     * the session is interactive.  We call the getCanvas().isPrintingThread because this is very expensive to
     * run when we are actually printing, where we redo updatePlotImage etc, but we might just be looking to
     * create a thumbnail and existing images may work fine.  All this was introduced because Autoplot was calling
     * canvas.getImage to get thumbnails and for its layout tab, and a quick way to get the canvas image was needed.
     * @param g
     */
    @Override
    protected void printComponent(Graphics g) {
        boolean doInvalidate= getCanvas().isPrintingThread();
        if ( doInvalidate ) { // only if we really are printing from the canvas print method.  getImageNonPrint
            resetCacheImageBounds(true);
            List<Renderer> renderers1= Arrays.asList(getRenderers());
            for (int i = 0; i < renderers1.size(); i++) {
                Renderer rend = (Renderer) renderers1.get(i);
                if (rend.isActive()) {
                    logger.log(Level.FINEST, "updating renderer #{0}: {1}", new Object[]{i, rend});
                    try {
                        rend.updatePlotImage(xAxis, yAxis, new NullProgressMonitor());
                    } catch (DasException ex) {
                        Logger.getLogger(DasPlot.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        super.printComponent(g);  // this calls paintComponent
        if ( doInvalidate ) {
            invalidateCacheImage();
        }
    }


    @Override
    protected synchronized void paintComponent(Graphics graphics0) {
        logger.log(Level.FINE, "dasPlot.paintComponent {0}", getDasName());
        if ( getCanvas().isValueAdjusting() ) {
            repaint();
            return;
        }

        if ( isOpaque() ) {
            Color co= graphics0.getColor();
            graphics0.setColor(getBackground());
            Rectangle clip= DasDevicePosition.toRectangle( getRow(),getColumn() );
            int dy= getRow().top()-this.getY();
            graphics0.fillRect( 0, dy, clip.width+1, clip.height+dy );
            graphics0.setColor(co);
        }

        if (!getCanvas().isPrintingThread() && !EventQueue.isDispatchThread()) {
            throw new RuntimeException("not event thread: " + Thread.currentThread().getName());
        }
        //paintComponentCount++;

        if (getCanvas().isPrintingThread()) {
            logger.fine("* printing thread *");
        }

        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();

        int xSize = getColumn().getDMaximum() - x;
        int ySize = getRow().getDMaximum() - y;

        Shape saveClip;
        if (getCanvas().isPrintingThread()) {
            saveClip = graphics0.getClip();
            graphics0.setClip(null);
        } else {
            saveClip = null;
        }

        logger.log(Level.FINEST, "DasPlot clip={0} @ {1},{2}", new Object[]{graphics0.getClip(), getX(), getY()});

        Rectangle clip = graphics0.getClipBounds();
        if (clip != null && (clip.y + getY()) >= (y + ySize)) {
            logger.finer("returning because clip indicates nothing to be done.");
            return;
        }

        boolean disableImageCache = false;

        Graphics2D graphics = (Graphics2D) graphics0.create();

        Rectangle clip0= graphics.getClipBounds();
        Rectangle plotClip= DasDevicePosition.toRectangle( getRow(), getColumn() );
        plotClip.height+=2;
        if ( displayTitle ) plotClip.height+=titleHeight;
        plotClip.width+=2;
        plotClip.translate(-x, -y);
        if ( clip!=null ) plotClip= plotClip.intersection(clip);
        graphics.setClip( plotClip );


        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.translate(-getX(), -getY());

        boolean useCacheImage= cacheImageValid && !getCanvas().isPrintingThread() && !disableImageCache;
        if ( useCacheImage ) {

            Graphics2D atGraphics = (Graphics2D) graphics.create();

            AffineTransform at = getAffineTransform(xAxis, yAxis);
            if (at == null || (preview == false && !isIdentity(at))) {
                atGraphics.drawImage(cacheImage, cacheImageBounds.x, cacheImageBounds.y, cacheImageBounds.width, cacheImageBounds.height, this);
                paintInvalidScreen(atGraphics, at);

            } else {
                if (!at.isIdentity()) {    
                    String atDesc = GraphUtil.getATScaleTranslateString(at);
                    logger.log(Level.FINEST, " using cacheImage w/AT {0}", atDesc);
                    atGraphics.transform(at);
                } else {
                    //atDesc= "identity";
                    logger.log(Level.FINEST, " using cacheImage {0} {1} {2}", new Object[]{cacheImageBounds, xmemento, ymemento});
                }

                //int reduceHeight=  getRow().getDMinimum() - clip.y;
                //if ( reduceHeight>0 ) {
                //    clip.y+= reduceHeight;
                //    clip.height-= reduceHeight;
                //}
                //clip.translate( getX(), getY() );
                //atGraphics.setClip(clip);

                if ( cacheImageBounds.width!=cacheImage.getWidth() ) {
                    logger.log( Level.WARNING, " cbw: {0}  ciw:{1}", new Object[]{cacheImageBounds.width, cacheImage.getWidth()});
                }
                atGraphics.drawImage(cacheImage, cacheImageBounds.x, cacheImageBounds.y, cacheImageBounds.width, cacheImageBounds.height, this);
                    //atGraphics.setClip(null);
                    //return;
                    //graphics.drawString( "cacheImage "+atDesc, getWidth()/2, getHeight()/2 );
            //atGraphics.setClip(null);
            //return;
            //graphics.drawString( "cacheImage "+atDesc, getWidth()/2, getHeight()/2 );

                    }

            atGraphics.dispose();

        } else {  // don't useCacheImage

            synchronized (this) {
                Graphics2D plotGraphics;
                if (getCanvas().isPrintingThread() || disableImageCache) {
                    plotGraphics = (Graphics2D) graphics.create(x - 1, y - 1, xSize + 2, ySize + 2);
                    resetCacheImageBounds(true);
                    logger.finest(" printing thread, drawing");
                } else {
                    if ( getWidth()==0 || getHeight()==0 ) {
                        return;
                    }
                    resetCacheImageBounds(false);                    
                    if ( cacheImageBounds.width==0 || cacheImageBounds.height==0 ) {
                        logger.info("https://sourceforge.net/p/autoplot/bugs/1076/");
                        return;
                    }
                    cacheImage = new BufferedImage(cacheImageBounds.width, cacheImageBounds.height,
                            BufferedImage.TYPE_4BYTE_ABGR);
                    plotGraphics = (Graphics2D) cacheImage.getGraphics();
                    plotGraphics.setBackground(getBackground());
                    plotGraphics.setColor(getForeground());
                    plotGraphics.setRenderingHints(org.das2.DasProperties.getRenderingHints());
                    if (overSize) {
                        plotGraphics.translate(x - cacheImageBounds.x - 1, y - cacheImageBounds.y - 1);
                    }

                    logger.finest(" rebuilding cacheImage");

                }

                plotGraphics.translate(-x + 1, -y + 1);

                // check mementos before drawing.  They should all be the same.  See https://sourceforge.net/tracker/index.php?func=detail&aid=3075655&group_id=199733&atid=970682
                Renderer[] rends= getRenderers();

                //for ( int i=0; i<rends.length; i++ ) {
                //    System.err.println( "renderer #"+i+": " +rends[i] + " ds="+rends[i].getDataSet() );
                //}

                if ( rends.length>0 ) {
                    Memento xmem= getXAxis().getMemento();
                    Memento ymem= getYAxis().getMemento();
                    for ( Renderer r: rends ) {
                        boolean dirt= false;
                        if ( r.getXmemento()==null || !r.getXmemento().equals(xmem) ) dirt= true;
                        if ( r.getYmemento()==null || !r.getYmemento().equals(ymem) ) dirt= true;
                        if ( dirt ) {
                            try {
                                r.updatePlotImage(getXAxis(), getYAxis(), new NullProgressMonitor());
                            } catch (DasException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                }

                drawCacheImage(plotGraphics);
            }


            if ( !disableImageCache && !getCanvas().isPrintingThread() ) {
                cacheImageValid = true;
                //clip.y= Math.max( clip.y, getRow().getDMinimum() );
                //clip.translate( getX(), getY() );
                //graphics.setClip(clip);
                graphics.drawImage(cacheImage, cacheImageBounds.x, cacheImageBounds.y, cacheImageBounds.width, cacheImageBounds.height, this);
                //graphics.drawString( "new image", getWidth()/2, getHeight()/2 );
                //graphics.setClip(null);

                xmemento = xAxis.getMemento();
                ymemento = yAxis.getMemento();

                logger.log(Level.FINEST, "recalc cacheImage, xmemento={0} ymemento={1}", new Object[]{xmemento, ymemento});
            }
        }

        graphics.setColor(getForeground());
        graphics.drawRect(x - 1, y - 1, xSize + 1, ySize + 1);

        if ( clip0!=null ) clip0.translate( getX(), getY() );
        graphics.setClip(clip0);

        if ( displayTitle && plotTitle != null && plotTitle.length() != 0) {
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setAlignment(GrannyTextRenderer.CENTER_ALIGNMENT);
            gtr.setString(graphics, plotTitle);
            int titleWidth = (int) gtr.getWidth();
            int titleX = x + (xSize - titleWidth) / 2;
            int titleY = y - (int) gtr.getDescent() - (int) gtr.getAscent() / 2;
            gtr.draw(graphics, (float) titleX, (float) titleY);
        }

        // --- draw messages ---
        if (messages.size() > 0) {
            drawMessages(graphics);
        }

        if ( legendElements.size() > 0 && displayLegend ) {
            drawLegend(graphics);
        }

        graphics.setClip(null);

        graphics.dispose();

        getDasMouseInputAdapter().paint(graphics0);

        if (saveClip != null) {
            graphics0.setClip(saveClip);
        }
    }

    public void setEnableRenderPropertiesAction(boolean b) {
        if ( b ) {
            this.editRendererMenuItem=new JMenuItem(getEditAction());
            getDasMouseInputAdapter().addMenuItem(editRendererMenuItem);
        } else {
            if ( this.editRendererMenuItem!=null ) {
                getDasMouseInputAdapter().removeMenuItem(this.editRendererMenuItem.getText());
            }
            this.editRendererMenuItem=null;
        }
    }

    /**
     * property enableRenderPropertiesAction means right-clicking on a menu
     * allows access to the renderer.
     * @return
     */
    private boolean isEnableRenderPropertiesAction() {
        return this.editRendererMenuItem!=null;
    }

    private class MessageDescriptor {

        /**
         * the renderer posting the text, or null if the plot owns the text
         */
        Renderer renderer;
        String text;

        /**
         * The severity of the message.  DasPlot.INFO or Level.INFO.intValue().
         */
        int messageType;
        Datum x;
        Datum y;
        Rectangle bounds; // stores the drawn boundaries of the message for context menu.

        MessageDescriptor(Renderer renderer, String text, int messageType, Datum x, Datum y) {
            this.renderer = renderer;
            this.text = text;
            this.messageType = messageType;
            this.x = x;
            this.y = y;
        }
    }

    private class LegendElement {

        ImageIcon icon;
        Renderer renderer;
        String label;
        Rectangle bounds;

        LegendElement(ImageIcon icon, Renderer rend, String label) {
            this.icon = icon;
            this.renderer = rend;
            this.label = label;
        }

        protected void drawIcon(Graphics2D graphics, int x, int y ) {
            renderer.drawListIcon(graphics, x, y);
        }
    }

    /**
     * These levels are now taken from java.util.logging.Level.
     */
    public static final int INFO = Level.INFO.intValue();
    public static final int WARNING = Level.WARNING.intValue();
    public static final int SEVERE = Level.SEVERE.intValue(); // this was ERROR before Feb 2011.

    List messages;
    List<LegendElement> legendElements;

    /**
     * Notify user of an exception, in the context of the plot.  A position in
     * the data space may be specified to locate the text within the data context.
     * Note either or both x or y may be null.  Messages must only be posted while the
     * Renderer's render method is called, not during updatePlotImage.  All messages are
     * cleared before the render step. (TODO:check on this)
     * 
     * @param renderer identifies the renderer posting the exception
     * @param text the text to be displayed, may contain granny text.
     * @param messageType DasPlot.INFO, DasPlot.WARNING, or DasPlot.SEVERE.  (SEVERE was ERROR before)
     * @param x if non-null, the location on the x axis giving context for the text.
     * @param y if non-null, the location on the y axis giving context for the text.
     */
    public void postMessage(Renderer renderer, String message, int messageType, Datum x, Datum y) {
        if ( messages==null ) {
            //system.err.println("don't post messages in updatePlotImage")
        } else {
            messages.add(new MessageDescriptor(renderer, message, messageType, x, y));
        }
    }

    /**
     * Notify user of an exception, in the context of the plot.  A position in
     * the data space may be specified to locate the text within the data context.
     * Note either or both x or y may be null.  Messages must only be posted while the
     * Renderer's render method is called, not during updatePlotImage.  All messages are
     * cleared before the render step. (TODO:check on this)
     *
     * @param renderer identifies the renderer posting the exception
     * @param text the text to be displayed, may contain granny text.
     * @param messageLevel allows java.util.logging.Level to be used, for example Level.INFO, Level.WARNING, and Level.SEVERE
     * @param x if non-null, the location on the x axis giving context for the text.
     * @param y if non-null, the location on the y axis giving context for the text.
     */
    public void postMessage(Renderer renderer, String message, Level messageLevel, Datum x, Datum y) {
        if ( messages==null ) {
            //system.err.println("don't post messages in updatePlotImage")
        } else {
            messages.add(new MessageDescriptor(renderer, message, messageLevel.intValue(), x, y));
        }
    }

    /**
     * notify user of an exception, in the context of the plot.  This is similar
     * to postMessage(renderer, exception.getMessage(), DasPlot.SEVERE, null, null )
     * except that it does catch CancelledOperationExceptions and reduced the
     * severity since the user probably initiated the condition.
     */
    public void postException(Renderer renderer, Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = String.valueOf(exception);
        }
        int errorLevel = SEVERE;
        if (exception instanceof CancelledOperationException) {
            errorLevel = INFO;
            if (exception.getMessage() == null) {
                message = "Operation Cancelled";
            }
        }
        postMessage(renderer, message, errorLevel, null, null);
    }

    /**
     * 
     * @param renderer identifies the renderer adding to the legend
     * @param icon if non-null, an icon to use.  If null, the renderer's icon is used.
     * @param pos integer order parameter, and also identifies item.
     * @param message String message to display.  
     */
    public void addToLegend(Renderer renderer, ImageIcon icon, int pos, String message) {
        legendElements.add(new LegendElement(icon, renderer, message));
    }

    private void drawGrid(Graphics2D g, DatumVector xticks, DatumVector yticks) {
        int xmin = this.cacheImageBounds.x;
        int xmax = this.cacheImageBounds.x + this.cacheImageBounds.width;
        int ymin = this.cacheImageBounds.y;
        int ymax = this.cacheImageBounds.y + this.cacheImageBounds.height;

        if (yticks != null && yticks.getUnits().isConvertableTo(yAxis.getUnits())) {
            for (int i = 0; i < yticks.getLength(); i++) {
                int y = (int) yAxis.transform(yticks.get(i));
                g.drawLine(xmin, y, xmax, y);
            }
        }
        if (xticks != null && xticks.getUnits().isConvertableTo(xAxis.getUnits())) {
            for (int i = 0; i < xticks.getLength(); i++) {
                int x = (int) xAxis.transform(xticks.get(i));
                g.drawLine(x, ymin, x, ymax);
            }
        }
    }

    protected void drawContent(Graphics2D g) {
        // override me to add to the axes.
    }

    @Override
    public void resize() {
        logger.finer("resize DasPlot");
        if (isDisplayable()) {
            Rectangle oldBounds= getBounds();

            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setString(getFont(), getTitle());

            titleHeight = (int) gtr.getHeight() + (int) gtr.getAscent() / 2;

            //if ( this.getDasName().startsWith("plot_3")) {
            //    System.err.println("here");
            //}
            Rectangle legendBounds= getLegendBounds( (Graphics2D) getGraphics(), 0, 0 );

            Rectangle bounds = new Rectangle();
            bounds.x = getColumn().getDMinimum() - 1;
            bounds.y = getRow().getDMinimum() - 1;
            // if legend label is outside the plot, then we'll do something here.  Note this will cause the data to be drawn out-of-bounds as well.

            bounds.width = getColumn().getDMaximum() - bounds.x + 1;
            bounds.height = getRow().getDMaximum() - bounds.y + 1;
            if ( displayTitle && !getTitle().equals("") ) {
                bounds.y -= titleHeight;
                bounds.height += titleHeight;
            }

            if ( legendBounds!=null ) bounds.add(legendBounds);
            
            // TODO check bounds.height<10
            logger.log(Level.FINER, "DasPlot setBounds {0}", bounds);
            if ( !bounds.equals(oldBounds) ) {
                setBounds(bounds);
                SwingUtilities.invokeLater( new Runnable() {
                   public void run() {
                        List<Renderer> renderers1= Arrays.asList(getRenderers());
                        for ( int i=0; i<renderers1.size(); i++ ) {
                            ((Renderer)renderers1.get(i)).refresh();
                        }
                       invalidateCacheImage();
                   }
                });
            }
        }
    }

    /** Sets the title which will be displayed above this plot.
     *
     * @param t The new title for this plot.
     */
    public void setTitle(String t) {
        Object oldValue = plotTitle;
        plotTitle = t;
        if ( t==null ) t="";
        if (getCanvas() != null) {
            FontMetrics fm = getFontMetrics(getCanvas().getFont());
            titleHeight = fm.getHeight() + fm.getHeight() / 2;
            resize();
            invalidateCacheImage();
        }
        if ( !t.equals(oldValue) ) {
            firePropertyChange(PROP_TITLE, oldValue, t);
        }
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


    public static final String PROP_DISPLAYTITLE="displayTitle";

    public boolean isDisplayTitle() {
        return this.displayTitle;
    }

    public void setDisplayTitle(boolean v) {
        boolean old= this.displayTitle;
        this.displayTitle= v;
        firePropertyChange( PROP_DISPLAYTITLE, old, v );
        resize();
        invalidateCacheImage();
    }
    
    public static final String PROP_CONTEXT= "context";
    
    /**
     * convenient place to put the plot context.  The context is used to
     * store the timerange when there is no axis for it, for example, to
     * show the state of data during a range.  This may change to a QDataSet
     * to provide several context dimensions.
     */
    DatumRange context= null;

    public DatumRange getContext() {
        return context;
    }

    public void setContext(DatumRange context) {
        DatumRange old= this.context;
        this.context = context;
        firePropertyChange( PROP_CONTEXT, old, context );
    }

    public static final String PROP_DISPLAY_CONTEXT= "displayContext";

    /**
     * necessary place to put the range of the data actually displayed.  The context is the controller,
     * and the displayContext closes the loop.  This is mostly here to provide legacy support to Autoplot which
     * abused the context property as both a write and read, and note there's a small problem that displayed
     * items may have different display contexts.  So this property should be used carefully, and generally
     * when just one thing is visible.
     */
    DatumRange displayContext= null;

    public DatumRange getDisplayContext() {
        return displayContext;
    }

    public void setDisplayContext(DatumRange displayContext) {
        DatumRange old= this.displayContext;
        this.displayContext = displayContext;
        firePropertyChange( PROP_DISPLAY_CONTEXT, old, displayContext );
    }


    private List<Renderer> renderers = null;

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
        this.dataSetDescriptor = dataSetDescriptor;
        markDirty();
    }

    protected class RebinListener implements java.beans.PropertyChangeListener {

        public void propertyChange(java.beans.PropertyChangeEvent e) {
            logger.log(Level.FINE, "rebin listener got property change: {0}", e.getNewValue());
            //System.err.println("rebin listener " + DasPlot.this + "got property change: "+e.getPropertyName() + "=" + e.getNewValue());
            if ( isotropic && e.getSource() instanceof DasAxis ) {
                DasAxis axis= (DasAxis)e.getSource();
                checkIsotropic( DasPlot.this, axis );
            }
            //vandalize cacheImage so we can see what's happening
            //BufferedImage ci= cacheImage;
            //if ( ci!=null ) {
            //    ci.getGraphics().drawLine(0,0,ci.getWidth(),ci.getHeight());
            //}
            
            markDirty();
            DasPlot.this.update();
        }
    }

    @Override
    protected void installComponent() {
        super.installComponent();
        if (xAxis != null) {
            getCanvas().add(xAxis, getRow(), getColumn());
        }
        if (yAxis != null) {
            getCanvas().add(yAxis, getRow(), getColumn());
        }
        Renderer[] r = getRenderers();
        for (int index = 0; index < r.length; index++) {
            r[index].installRenderer();
        }
        if (!"true".equals(DasApplication.getProperty("java.awt.headless", "false"))) {
            dndSupport = new PlotDnDSupport(getCanvas().dndSupport);
        }
    }

    @Override
    protected void uninstallComponent() {
        super.uninstallComponent();
        if (xAxis != null && xAxis.getCanvas() != null) {
            xAxis.getCanvas().remove(xAxis);
        }
        if (yAxis != null && yAxis.getCanvas() != null) {
            yAxis.getCanvas().remove(yAxis);
        }
        Renderer[] r = getRenderers();
        for (int index = 0; index < r.length; index++) {
            r[index].uninstallRenderer();
        }
    }

    public void addRenderer(Renderer rend) {
        logger.log(Level.FINE, "addRenderer({0})", rend);
        if (rend.parent != null) {
            rend.parent.removeRenderer(rend);
        }
        renderers.add(rend);
        rend.parent = this;
        if (getCanvas() != null) {
            rend.installRenderer();
        }
        rend.update();
        invalidateCacheImage();
    }

    public void addRenderer(int index, Renderer rend) {
        logger.log(Level.FINE, "addRenderer({0},{1})", new Object[]{index, rend});
        if (rend.parent != null) {
            rend.parent.removeRenderer(rend);
        }
        renderers.add(index,rend);
        rend.parent = this;
        if (getCanvas() != null) {
            rend.installRenderer();
        }
        rend.update();
        invalidateCacheImage();
    }

    /**
     * remove the renderer from the stack of renderers.  A warning 
     * is logged if the renderer is not present.
     * @param rend the renderer
     */
    public void removeRenderer(Renderer rend) {
        logger.log( Level.FINE, "removeRenderer({0})", new Object[] { rend } );
        //System.err.println("removeRenderer "+rend);
        if ( !renderers.contains(rend ) ) {
            logger.log(Level.WARNING, "*** plot doesn''t contain renderer: {0}", rend);
        }
        if (getCanvas() != null) {
            rend.uninstallRenderer();
        }
        if ( focusRenderer==rend ) setFocusRenderer(null);
        renderers.remove(rend);
        rend.parent = null;
        invalidateCacheImage();

    }
    
    /**
     * return true if the plot contains the renderer.
     * @param rend the renderer
     * @return true if the plot contains the renderer.
     */
    public boolean containsRenderer( Renderer rend ) {
        return renderers.contains(rend);
    }

    /**
     * remove all the renderers from the dasPlot.
     */
    public synchronized void removeRenderers() {
        if (getCanvas() != null) {
            for ( Renderer rend: renderers ) {
                rend.uninstallRenderer();
                rend.parent = null; // should get GC'd.
            }
            setFocusRenderer(null);
            renderers.clear();
        }
        invalidateCacheImage();

    }

    public static DasPlot createDummyPlot() {
        DasAxis xAxis = new DasAxis(Datum.create(-10), Datum.create(10), DasAxis.HORIZONTAL);
        DasAxis yAxis = new DasAxis(Datum.create(-10), Datum.create(10), DasAxis.VERTICAL);
        DasPlot result = new DasPlot(xAxis, yAxis);
        return result;
    }

    public static DasPlot createPlot(DatumRange xrange, DatumRange yrange) {
        DasAxis xAxis = new DasAxis(xrange, DasAxis.HORIZONTAL);
        DasAxis yAxis = new DasAxis(yrange, DasAxis.VERTICAL);
        DasPlot result = new DasPlot(xAxis, yAxis);
        return result;
    }

    public synchronized Renderer getRenderer(int index) {
        return (Renderer) renderers.get(index);
    }

    public synchronized Renderer[] getRenderers() {
        return (Renderer[]) renderers.toArray(new Renderer[renderers.size()]);
    }


    private class PlotDnDSupport extends org.das2.util.DnDSupport {

        PlotDnDSupport(org.das2.util.DnDSupport parent) {
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
                Renderer r = (Renderer) t.getTransferData(TransferableRenderer.RENDERER_FLAVOR);
                addRenderer(r);
                revalidate();
                success = true;
            } catch (UnsupportedFlavorException ufe) {
            } catch (IOException ioe) {
            }
            return success;
        }

        protected Transferable getTransferable(int x, int y, int action) {
            return null;
        }

        protected void exportDone(Transferable t, int action) {
        }
    }

    @Override
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
    @Override
    protected AWTEvent coalesceEvents(AWTEvent existingEvent, AWTEvent newEvent) {
        if (existingEvent instanceof DasRendererUpdateEvent && newEvent instanceof DasRendererUpdateEvent) {
            DasRendererUpdateEvent e1 = (DasRendererUpdateEvent) existingEvent;
            DasRendererUpdateEvent e2 = (DasRendererUpdateEvent) newEvent;
            if (e1.getRenderer() == e2.getRenderer()) {
                //don't log these because GUI-based log handler causes infinite loop.
                //logger.fine("coalesce update events for renderer "+e1.getRenderer().getId() );
                return existingEvent;
            } else {
                return null;
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
    @Override
    protected void processEvent(AWTEvent e) {
        if (e instanceof DasRendererUpdateEvent) {
            logger.fine("process DasRendererUpdateEvent");
            DasRendererUpdateEvent drue = (DasRendererUpdateEvent) e;
            drue.getRenderer().updateImmediately();
            cacheImageValid = false;
            repaint();
        } else {
            super.processEvent(e);
        }
    }

    @Override
    public void repaint() {
        super.repaint();
        //repaintCount++;
    }

    /**
     * mark the dasPlot's cache image as invalid, forcing it to repaint.
     * TODO: when should a user use invalidateCacheImage vs markDirty?
     */
    public void invalidateCacheImage() {
        if ( cacheImageValid==false ) return;
        cacheImageValid = false;
        super.markDirty();
        update();
    }

    /**
     * introduced to debug Autoplot test018.  This should not be used otherwise.
     * @return true if the cache image is marked as valid.
     */
    public boolean isCacheImageValid() {
        return cacheImageValid;
    }
    
    @Override
    void markDirty() {
        logger.finer("DasPlot.markDirty");
        super.markDirty();
        //if ( isotropic ) {
        //    checkIsotropic( DasPlot.this, null );
        //}
        repaint();
    }


    private LegendPosition legendPosition = LegendPosition.NE;

    public static final String PROP_LEGENDPOSITION = "legendPosition";

    public LegendPosition getLegendPosition() {
        return this.legendPosition;
    }

    public void setLegendPosition(LegendPosition newlegendPosition) {
        LegendPosition oldlegendPosition = legendPosition;
        this.legendPosition = newlegendPosition;
        firePropertyChange(PROP_LEGENDPOSITION, oldlegendPosition, newlegendPosition);
        resize();
        repaint();
    }

    protected boolean displayLegend = true;
    public static final String PROP_DISPLAYLEGEND = "displayLegend";

    public boolean isDisplayLegend() {
        return displayLegend;
    }

    public void setDisplayLegend(boolean displayLegend) {
        boolean oldDisplayLegend = this.displayLegend;
        this.displayLegend = displayLegend;
        firePropertyChange(PROP_DISPLAYLEGEND, oldDisplayLegend, displayLegend);
        resize();
        repaint();
    }



    /**
     * property drawGrid.  If true, faint grey lines continue the axis major
     * ticks across the plot.
     */
    private boolean drawGrid = false;

    /**
     * Getter for property drawGrid.  If true, faint grey lines continue the axis major
     * ticks across the plot.
     * @return Value of property drawGrid.
     */
    public boolean isDrawGrid() {
        return this.drawGrid;
    }

    /**
     * Setter for property drawGrid.  If true, faint grey lines continue the axis major
     * ticks across the plot.
     * @param drawGrid New value of property drawGrid.
     */
    public void setDrawGrid(boolean drawGrid) {
        boolean bOld = this.drawGrid;
        this.drawGrid = drawGrid;
        this.invalidateCacheImage();
        this.repaint();

        if (bOld != drawGrid) {
            firePropertyChange(PROP_DRAWGRID, bOld, drawGrid);
        }
    }
    public static final String PROP_DRAWGRID = "drawGrid";
    private boolean drawMinorGrid;
    public static final String PROP_DRAWMINORGRID = "drawMinorGrid";

    /**
     * Get the value of drawMinorGrid
     *
     * @return the value of drawMinorGrid
     */
    public boolean isDrawMinorGrid() {
        return this.drawMinorGrid;
    }

    /**
     * Set the value of drawMinorGrid
     *
     * @param newdrawMinorGrid new value of drawMinorGrid
     */
    public void setDrawMinorGrid(boolean newdrawMinorGrid) {
        boolean olddrawMinorGrid = drawMinorGrid;
        this.drawMinorGrid = newdrawMinorGrid;
        this.invalidateCacheImage();
        this.repaint();
        firePropertyChange(PROP_DRAWMINORGRID, olddrawMinorGrid, newdrawMinorGrid);
    }
    protected boolean gridOver = true;
    public static final String PROP_GRIDOVER = "gridOver";

    public boolean isGridOver() {
        return gridOver;
    }

    public void setGridOver(boolean gridOver) {
        boolean oldGridOver = this.gridOver;
        this.gridOver = gridOver;
        this.invalidateCacheImage();
        this.repaint();
        firePropertyChange(PROP_GRIDOVER, oldGridOver, gridOver);
    }

    public void setPreviewEnabled(boolean preview) {
        this.preview = preview;
    }

    public boolean isPreviewEnabled() {
        return this.preview;
    }

    /**
     * set the visibility of both the plot and its x and y axes.  Recently,
     * setVisible(v) would do this, but it incorrectly couples the visible properties
     * of the separate components.
     *
     * @param visible
     */
    public void setAxisPlotVisible( boolean visible ) {
        this.setVisible(visible);
        this.xAxis.setVisible(visible);
        this.yAxis.setVisible(visible);
    }

    @Override
    public void setVisible(boolean visible) {
        //bugfix: https://sourceforge.net/tracker/index.php?func=detail&aid=3137434&group_id=199733&atid=970682
        //visible properties must be independent, or bugs will happen.  See setAxisPlotVisible for old behavior.
        super.setVisible(visible);
    }

    protected boolean overSize = false;
    public static final String PROP_OVERSIZE = "overSize";

    public boolean isOverSize() {
        return overSize;
    }

    public void setOverSize(boolean overSize) {
        boolean oldOverSize = this.overSize;
        this.overSize = overSize;
        invalidateCacheImage();
        firePropertyChange(PROP_OVERSIZE, oldOverSize, overSize);
    }

    public static final String PROP_LOG_LEVEL = "logLevel";
    private Level logLevel= Level.INFO;

    public void setLogLevel( Level level ) {
        Level oldLevel= this.logLevel;
        logLevel= level;
        if ( !oldLevel.equals(level) ) {
            repaint();
        }
        firePropertyChange(PROP_LOG_LEVEL, oldLevel, level );
    }

    public Level getLogLevel( ) {
        return logLevel;
    }

    public static String PROP_ISOTROPIC= "isotropic";

    private boolean isotropic= false;

    public boolean isIsotropic() {
        return isotropic;
    }

    public void setIsotropic(boolean isotropic) {
        boolean oldvalud= this.isotropic;
        this.isotropic = isotropic;
        if ( oldvalud!=isotropic ) {
            firePropertyChange(PROP_ISOTROPIC, oldvalud, isotropic );
        }
        if ( isotropic ) checkIsotropic( this, null );
    }

    /**
     * returns the rectangle that renderers should paint so that when they
     * are asked to render, they have everything pre-rendered.  This is
     * the same as the axis bounds them oversize is turned off.
     * 
     * @return
     */
    protected Rectangle getUpdateImageBounds() {
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        cacheImageBounds = new Rectangle();
        if ( overSize ) {
            cacheImageBounds.width = 16 * getWidth() / 10;
            cacheImageBounds.height = getHeight();
            cacheImageBounds.x = x - 3 * getWidth() / 10;
        } else {
            cacheImageBounds.width = getWidth();
            cacheImageBounds.height = getHeight();
            cacheImageBounds.x = x - 1;
        }
        cacheImageBounds.y = y - 1;
        return cacheImageBounds;
    }

    /**
     * returns the position of the cacheImage in the canvas frame of reference.
     * @return Rectangle
     */
    protected Rectangle getCacheImageBounds() {
        return cacheImageBounds;
    }

    /**
     * adjust the plot axes so it remains isotropic.
     * @param axis if non-null, the axis that changed, and the other should be adjusted.
     */
    private void checkIsotropic(DasPlot dasPlot, DasAxis axis) {
        Datum scalex = dasPlot.getXAxis().getDatumRange().width().divide(dasPlot.getXAxis().getDLength());
        Datum scaley = dasPlot.getYAxis().getDatumRange().width().divide(dasPlot.getYAxis().getDLength());

        if ( ! scalex.getUnits().isConvertableTo(scaley.getUnits())
                || dasPlot.getXAxis().isLog()
                || dasPlot.getYAxis().isLog() ) {
            return;
        }

        if ( axis==null ) {
            axis= scalex.gt(scaley) ?  dasPlot.getXAxis()  : dasPlot.getYAxis() ;
        }

        if ( (axis == dasPlot.getXAxis() || axis == dasPlot.getYAxis()) ) {
            DasAxis otherAxis = dasPlot.getYAxis();
            if (axis == dasPlot.getYAxis()) {
                otherAxis = dasPlot.getXAxis();
            }
            Datum scale = axis.getDatumRange().width().divide(axis.getDLength());
            DatumRange otherRange = otherAxis.getDatumRange();
            Datum otherScale = otherRange.width().divide(otherAxis.getDLength());
            double expand = (scale.divide(otherScale).value() - 1) / 2;
            if (Math.abs(expand) > 0.0001) {
                logger.log(Level.FINER, "expand={0} scale={1} otherScale={2}", new Object[]{expand, scale, otherScale});
                DatumRange newOtherRange = DatumRangeUtil.rescale(otherRange, 0 - expand, 1 + expand);
                otherAxis.setDatumRange(newOtherRange);
            }
        }
    }
}
