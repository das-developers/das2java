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
import org.das2.dataset.VectorUtil;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.TableUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.DasApplication;
import org.das2.CancelledOperationException;
import org.das2.DasProperties;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.DnDSupport;
import java.beans.PropertyChangeEvent;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.components.propertyeditor.PropertyEditor;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumVector;
import org.das2.graph.dnd.TransferableRenderer;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.das2.DasException;
import org.das2.dataset.DataSetAdapter;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.LoggerManager;
import org.das2.event.DasMouseInputAdapter;
import org.das2.graph.DasAxis.Memento;
import org.das2.qds.DataSetUtil;

/**
 * DasPlot is the 2D plot containing a horizontal X axis and vertical Y
 * axis, and a stack of Renderers that paint data onto the plot.  It coordinates
 * calls to each Renderer's updatePlotImage and render methods, and manages
 * the rendered stack image to provide previews of new axis settings.
 * 
 * @author jbf
 */
public class DasPlot extends DasCanvasComponent {

	private static final List<CustomizerKey> CUSTOMIZER_KEYS = new ArrayList<>();
	private static final Map<CustomizerKey, Customizer> PLOT_CUSTOMIZERS = new HashMap<>();

	/**
	 * Return a list of keys of all current customizing objects in the order they would be invoked.
	 * @return the keys
	 */
	public static List<CustomizerKey> getCustomizerKeys() {
		synchronized ( CUSTOMIZER_KEYS ) {
			// Defensive copy to ensure data structures maintain invariants.
			return new ArrayList<>(CUSTOMIZER_KEYS);
		}
	}

	/**
	 * Add a new customizer to the collection of customizers being used when creating
	 * new plots. The new customizer will be invoked last.
	 * @param key the new customizer's lookup key
	 * @param customizer the new customizer
	 */
	public static void addCustomizer(CustomizerKey key, Customizer customizer) {
		synchronized ( CUSTOMIZER_KEYS ) {
			if (PLOT_CUSTOMIZERS.containsKey(key)) {
				// A customizer with this key is already in the list and map.
				// Just replace the customizer but don't tamper with list order.
				PLOT_CUSTOMIZERS.put(key, customizer);
			} else {
				// Add the customizer, and add the key at the end of the key list.
				PLOT_CUSTOMIZERS.put(key, customizer);
				CUSTOMIZER_KEYS.add(key);
			}
		}
	}

	/*
	 * Return the customizer that is associated with the given key.
	 * @param key the key for which to find the cutomizer
	 * @returns the customizer, or null if the customizer is not present
	 */
	public static Customizer getCustomizer(CustomizerKey key) {
		synchronized ( CUSTOMIZER_KEYS ) {
			return PLOT_CUSTOMIZERS.get(key);
		}
	}

	/**
	 * Remove the customizer that is associated with the given key.
	 * @param key the key to the customizer to be removed.
	 */
	public static void removeCustomizer(CustomizerKey key) {
		synchronized ( CUSTOMIZER_KEYS ) {			
			CUSTOMIZER_KEYS.remove(key);
			PLOT_CUSTOMIZERS.remove(key);
		}
	}

	/**
     * title for the plot
     */
    public static final String PROP_TITLE = "title";
    
    private DasAxis xAxis;
    private DasAxis yAxis;
    DasAxis.Memento xmemento;
    DasAxis.Memento ymemento;
    private boolean reduceOutsideLegendTopMargin = false;
    //public String debugString = "";
    private String plotTitle = "";
    
    /**
     * true if the plot title should be displayed.
     */
    protected boolean displayTitle= true;

    /**
     * listens for property changes and triggers the process of updating the plot image.
     */
    protected RebinListener rebinListener = new RebinListener();
    
    /**
     * listens for x and y axis tick changes, and repaints the plot when 
     * drawing grid lines at tick positions.
     */
    protected transient PropertyChangeListener ticksListener = new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (drawGrid || drawMinorGrid) {
                invalidateCacheImage();
            }
        }
    };
    
    DnDSupport dndSupport;
    final static Logger logger = LoggerManager.getLogger("das2.graphics.plot");
    private JMenuItem editRendererMenuItem;
    /**
     * cacheImage is a cached image that all the renderers have drawn on.  This
     * relaxes the need for renderers' render method to execute in
     * animation-interactive time.
     */
    boolean cacheImageValid = false;
    
    /**
     * the rendered data is stored in the cacheImage.  This image may be
     * rescaled to provide immediate previews when axes are changed.
     */
    BufferedImage cacheImage;
    
    /**
     * bounds of the cache image.  
     */
    Rectangle cacheImageBounds;
    
    /**
     * property preview.  If set, the cache image may be scaled to reflect
     * the new axis position in animation-interactive time.
     */
    boolean preview = false;
    
    //private int repaintCount = 0;
    private final AtomicInteger paintComponentCount = new AtomicInteger(0);
    
    /**
     * height of the title in pixels.
     */
    private int titleHeight= 0;

    private boolean drawInactiveInLegend= false;

    /**
     * use this for conditional breakpoints.  Set this to non-null to
     * trigger breakpoint.
     */
    private static String testSentinal= null;
    
    private boolean reluctantLegendIcons= "true".equals( System.getProperty("reluctantLegendIcons","false") );
    
    /**
     * create a new plot with the x and y axes.
     * @param xAxis the horizontal axis
     * @param yAxis the vertical axis
     */
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
            xAxis.addPropertyChangeListener(DasAxis.PROP_FLIPPED,rebinListener);
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
            yAxis.addPropertyChangeListener(DasAxis.PROP_FLIPPED,rebinListener);
            yAxis.addPropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }

        if (!"true".equals(DasApplication.getProperty("java.awt.headless", "false"))) {
            addDefaultMouseModules();
        }

        synchronized ( CUSTOMIZER_KEYS ) {        	
        	for (CustomizerKey k : CUSTOMIZER_KEYS) {
        		PLOT_CUSTOMIZERS.get(k).customize(this);
        	}
        }
    }

    /**
     * returns the Renderer with the current focus.  Clicking on a trace sets the focus.
     */
    protected Renderer focusRenderer = null;
    
    /**
     * property name for the current renderer that the operator has selected.
     */
    public static final String PROP_FOCUSRENDERER = "focusRenderer";

    /**
     * get the current renderer that the operator has selected.
     * @return the current renderer that the operator has selected.
     */
    public Renderer getFocusRenderer() {
        return focusRenderer;
    }

    /**
     * set the current renderer that the operator has selected.
     * @param focusRenderer the current renderer that the operator has selected.
     */
    public void setFocusRenderer(Renderer focusRenderer) {
        Renderer oldFocusRenderer = this.focusRenderer;
        this.focusRenderer = focusRenderer;
        firePropertyChange(PROP_FOCUSRENDERER, null, focusRenderer);
        //firePropertyChange(PROP_FOCUSRENDERER, oldFocusRenderer, focusRenderer);
    }

    /**
     * for multiline labels, the horizontal alignment, where 0 is left, 0.5 is center, and 1.0 is right.
     */
    private float multiLineTextAlignment = 0.f;
    
    /**
     * property name for multiline labels, the horizontal alignment, where 0 is left, 0.5 is center, and 1.0 is right.
     */
    public static final String PROP_MULTILINETEXTALIGNMENT = "multiLineTextAlignment";

    /**
     * get the horizontal alignment for multiline labels, where 0 is left, 0.5 is center, and 1.0 is right.
     * @return the alignment
     */
    public float getMultiLineTextAlignment() {
        return multiLineTextAlignment;
    }
    
    /**
     * set the horizontal alignment for multiline labels, where 0 is left, 0.5 is center, and 1.0 is right.
     * @param multiLineTextAlignment the alignment
     */
    public void setMultiLineTextAlignment(float multiLineTextAlignment) {
        float oldMultiLineTextAlignment = this.multiLineTextAlignment;
        this.multiLineTextAlignment = multiLineTextAlignment;
        firePropertyChange(PROP_MULTILINETEXTALIGNMENT, oldMultiLineTextAlignment, multiLineTextAlignment);
        repaint();
    }
    
    private static final Icon NULL_ICON= new ImageIcon(new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB) );    
    
    
    /**
     * returns the bounds of the legend, or null if there is no legend.
     * TODO: merge with drawLegend, so there are not two similar codes.
     * @param graphics graphics context
     * @param msgx the location of the box
     * @param msgy the location of the box
     * @param llegendElements the elements 
     * @return the bounds
     */
    private Rectangle getLegendBounds( Graphics2D graphics, int msgx, int msgy, List<LegendElement> llegendElements) {
        int maxIconWidth = 0;

        Rectangle mrect;
        Rectangle boundRect=null;

        int em = (int) getEmSize();

        if ( llegendElements==null ) return null;
        if ( graphics==null ) return null;

        String contextStr= this.context==null ? "" : this.context.toString();
        for (LegendElement le : llegendElements) {
            if ( ( le.renderer!=null && le.renderer.isActive() ) || le.icon!=null || drawInactiveInLegend ) { 
                Icon icon= le.icon!=null ? le.icon : le.renderer.getListIcon();
                if ( icon==null ) icon=NULL_ICON;
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                String theLabel= String.valueOf(le.label).trim().replaceAll("%\\{CONTEXT\\}",contextStr);
                gtr.setString(graphics, theLabel); // protect from nulls, which seems to happen
                mrect = gtr.getBounds();
                maxIconWidth = Math.max(maxIconWidth, icon.getIconWidth());
                if ( reluctantLegendIcons ) {
                    if ( llegendElements.size()==1 ) {
                        maxIconWidth = 0;
                    }
                }                
                int theheight = Math.max(mrect.height, icon.getIconHeight());
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
        if ( null == legendPosition ) {
            throw new IllegalArgumentException("not supported: "+legendPosition);
        } else switch (legendPosition) {
            case NE:
            case NW:
                mrect.y= yAxis.getRow().getDMinimum() + em/2;
                if ( legendPosition==LegendPosition.NE ) {
                    mrect.x = xAxis.getColumn().getDMaximum() - em - mrect.width;
                    
                } else if ( legendPosition==LegendPosition.NW ) {
                    mrect.x = xAxis.getColumn().getDMinimum() + em ;
                }   break;
            case SE:
            case SW:
                mrect.y=  yAxis.getRow().getDMaximum() - boundRect.height - em; // note em not em/2 is intentional
                if ( legendPosition==LegendPosition.SE ) {
                    mrect.x = xAxis.getColumn().getDMaximum() - em - mrect.width;
                    
                } else if ( legendPosition==LegendPosition.SW ) {
                    mrect.x = xAxis.getColumn().getDMinimum() + em ;
                }   break;
            case OutsideNE:
                mrect.x = xAxis.getColumn().getDMaximum() + em + maxIconWidth;
                boundRect.x = mrect.x;
                mrect.y= yAxis.getRow().getDMinimum(); // em/5 determined by experiment.
                break;
            default:
                throw new IllegalArgumentException("not supported: "+legendPosition);
        }

        Rectangle axisBounds= DasDevicePosition.toRectangle( getRow(), getColumn() );
        axisBounds.width= Math.max( axisBounds.width, mrect.x+mrect.width-axisBounds.x ); // don't limit width because of outside NE
        Rectangle2D rr= mrect.createIntersection(axisBounds);

        return new Rectangle( (int)rr.getX(),(int)rr.getY(),(int)rr.getWidth(),(int)rr.getHeight() );
    }

    /**
     * draw the legend elements, substituting the plot context if the macro %{CONTEXT} is found.
     * @param g graphics context
     * @param llegendElements the legend elements
     */
    private void drawLegend(Graphics2D g, List<LegendElement> llegendElements ) {

        Graphics2D graphics= (Graphics2D) g.create();

        graphics.setFont( getFont().deriveFont( getFont().getSize2D() + legendRelativeFontSize ) );
        
        int em;
        int msgx, msgy;

        Color backColor = GraphUtil.getRicePaperColor();
        Rectangle mrect;

        em = (int) getEmSize();

        msgx = xAxis.getColumn().getDMiddle() + em;
        msgy = yAxis.getRow().getDMinimum() + em/2;

        int maxIconWidth= 0;
        for (LegendElement le : llegendElements) {
            Icon icon= le.icon!=null ? le.icon : le.renderer.getListIcon();
            if ( icon==null ) icon=NULL_ICON;
            maxIconWidth = Math.max(maxIconWidth, icon.getIconWidth());
            if ( reluctantLegendIcons ) {
                if ( llegendElements.size()==1 ) {
                    maxIconWidth = 0;
                }
            }
        }

        mrect= getLegendBounds(graphics,msgx,msgy, llegendElements);
        if ( mrect==null ) return; // nothing is active
        
        msgx= mrect.x;
        msgy= mrect.y;
        if ( legendPosition!=LegendPosition.OutsideNE ) {
            msgx+= maxIconWidth + em/4;
        }
        
        if ( legendPosition!=LegendPosition.OutsideNE ) {
            Rectangle legendBounds= new Rectangle( mrect.x - em / 4, mrect.y - em/4, mrect.width + em / 2, mrect.height + em/2 );
            int canvasWidth= getParent().getWidth();
            Rectangle clip= legendBounds.intersection( new Rectangle( 0, getRow().getDMinimum(), 2*canvasWidth, getRow().getHeight() ) );
            clip.height+= 1; //TODO lineThickness
            clip.width+= 1;
            graphics.clip( clip );
            graphics.setColor(backColor);
            graphics.fillRoundRect( legendBounds.x, legendBounds.y, legendBounds.width, legendBounds.height, 5, 5);
            graphics.setColor(getForeground());
            graphics.drawRoundRect( legendBounds.x, legendBounds.y, legendBounds.width, legendBounds.height, 5, 5);
        }
        
        String contextStr= this.context==null ? "" : this.context.toString();
        for (LegendElement le : llegendElements) {
            if ( ( le.renderer!=null && le.renderer.isActive() ) || le.icon!=null || drawInactiveInLegend ) {
                Icon icon= le.icon!=null ? le.icon : le.renderer.getListIcon();
                if ( icon==null ) icon=NULL_ICON;
                if ( llegendElements.size()==1 && reluctantLegendIcons ) {
                    icon = NULL_ICON;
                }
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                gtr.setAlignment( GrannyTextRenderer.LEFT_ALIGNMENT );
                String theLabel= String.valueOf(le.label).trim().replaceAll("%\\{CONTEXT\\}",contextStr);
                gtr.setString(graphics, theLabel); // protect from nulls, which seems to happen
                mrect = gtr.getBounds();
                mrect.translate(msgx, msgy + (int) gtr.getAscent());
                int theheight= Math.max(mrect.height, icon.getIconHeight());
                int icony= theheight/2 - icon.getIconHeight() / 2;  // from top of rectangle
                int texty= theheight/2 - (int)gtr.getHeight() / 2 + (int) gtr.getAscent();
                if ( reduceOutsideLegendTopMargin ) texty = theheight/2;
                gtr.draw( graphics, msgx, msgy + texty );
                mrect.height = theheight;
                Rectangle imgBounds = new Rectangle(
                        msgx - (icon.getIconWidth() + em / 4),
                        msgy + icony,
                        icon.getIconWidth(),
                        icon.getIconHeight() );
                if ( le.icon!=null ) {
                    graphics.drawImage( ((ImageIcon)icon).getImage(), imgBounds.x, imgBounds.y, null );
                } else {
                    if ( llegendElements.size()==1 && reluctantLegendIcons ) {
                        
                    } else {
                        le.drawIcon( graphics, msgx - (icon.getIconWidth() + em / 4), msgy + icony );
                    }
                }
                msgy += mrect.getHeight();
                mrect.add(imgBounds);
                if ( msgy > getRow().bottom() ) break;
                le.bounds = mrect;
            }
        }

        graphics.dispose();
        
    }
    
    /**
     * draw the message bubbles.  For the printing thread, we no
     * longer display the bubbles, unless the have Long.MAX_VALUE for the
     * birthmilli.
     * @param g the graphics context.
     */
    private void drawMessages(Graphics2D g, List<MessageDescriptor> lmessages ) {

        Graphics2D graphics= (Graphics2D) g.create();
        
        graphics.clip( DasDevicePosition.toRectangle( getRow(), getColumn() ) );
        
        boolean isPrint= getCanvas().isPrintingThread();
        
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

        long tnow= System.currentTimeMillis();
        
        boolean needRepaintSoon= false;
        long repaintDelay= 0;
        
        for (MessageDescriptor lmessage : lmessages) {
            MessageDescriptor message = (MessageDescriptor) lmessage;
            if ( message.messageType<logLevel.intValue() ) {
                continue; // skip this message
            }
            // https://sourceforge.net/p/autoplot/bugs/1093/: error bubbles must be hidden when printing.
            if ( isPrint ) { 
                if ( message.messageType<printingLogLevel.intValue() && message.birthMilli<Long.MAX_VALUE ) {
                    continue;
                }
            }
            if ( logTimeoutSec < Integer.MAX_VALUE/1000 && message.birthMilli < tnow - logTimeoutSec*1000 ) {
                continue;
            }
            if ( !isPrint && logTimeoutSec < 1000 && message.birthMilli<Long.MAX_VALUE ) {
                needRepaintSoon= true;
                repaintDelay= Math.max( repaintDelay, logTimeoutSec*1000 - ( tnow - message.birthMilli ) );
            }
            Icon icon=null;
            if ( message.renderer!=null && renderers1.size()>1 ) {
                icon= message.renderer.getListIcon();
            }
            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setAlignment( GrannyTextRenderer.LEFT_ALIGNMENT );
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
            if ( icon!=null ) icon.paintIcon( this, graphics, mrect.x, mrect.y  );
            graphics.drawRoundRect(mrect.x - em / 4, mrect.y, mrect.width + em / 2, mrect.height, 5, 5);
            if ( icon!=null ) {
                gtr.draw(graphics, msgx1 + icon.getIconWidth() + spc, msgy);
            } else {
                gtr.draw(graphics, msgx1 , msgy);
            }
            message.bounds = mrect;
            msgy += gtr.getHeight() + msgem / 2;  // findbugs OKAY
        }

        if ( needRepaintSoon ) {
            logger.log( Level.FINER, "need to repaint in {0} ms", repaintDelay);
            ActionListener animate = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent ae) {
                    repaint();
                }
            };
            Timer timer = new Timer( (int)repaintDelay, animate );
            timer.setRepeats(false);
            timer.start();
        }
        
        graphics.dispose();
        
    }

    private void maybeDrawGrid(Graphics2D plotGraphics) {
        Color gridColor;
        Color minorGridColor;
        if ( drawGridColor.getAlpha()>0 ) {
            gridColor= this.drawGridColor;
            minorGridColor = this.drawGridColor;
        } else {
            gridColor= new Color(128, 128, 128, 70);
            minorGridColor = new Color(128, 128, 128, 40);
        }

        DasAxis lxaxis= getXAxis();
        DasAxis lyaxis= getYAxis();
        if ( lxaxis==null || lyaxis==null ) return;
        
        TickVDescriptor xtickv= lxaxis.getTickV();
        TickVDescriptor ytickv= lyaxis.getTickV();

        if (drawMinorGrid && this.plotVisible ) {
            DatumVector xticks = null;
            DatumVector yticks = null;
            if ( xtickv!=null && lxaxis.getOrientation()==DasAxis.BOTTOM ) {
                xticks = xtickv.getMinorTicks();
            }
            if ( ytickv!=null && lyaxis.getOrientation()==DasAxis.LEFT ) {
                yticks = ytickv.getMinorTicks();
            }
            plotGraphics.setColor(minorGridColor);
            drawGrid(plotGraphics, xticks, yticks);
        }

        if (drawGrid && this.plotVisible ) {
            DatumVector xticks = null;
            DatumVector yticks = null;
            if ( xtickv!=null && lxaxis.getOrientation()==DasAxis.BOTTOM ) {
                xticks = xtickv.getMajorTicks();
            }
            if ( ytickv!=null && lyaxis.getOrientation()==DasAxis.LEFT) {
                yticks = ytickv.getMajorTicks();
            }
            plotGraphics.setColor(gridColor);
            drawGrid(plotGraphics, xticks, yticks);
        }

    }

    private void drawDecorator( Graphics2D plotGraphics, Painter p ) {
        try {
            Graphics2D g= (Graphics2D)plotGraphics.create();
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            p.paint(g);
            g.dispose();
        } catch ( Exception ex ) {
            logger.log( Level.WARNING, ex.getMessage(), ex );
        }
    }
    
    /**
     * this is the heart of DasPlot, where each of the renderers is
     * painted on to the cacheImage.
     * @param plotGraphics
     * @param lxaxis
     * @param lyaxis 
     */
    private void drawCacheImage(Graphics2D plotGraphics,DasAxis lxaxis, DasAxis lyaxis ) {

        /* clear all the messages */
        messages = new ArrayList();
        legendElements = new ArrayList<>();

        if (!drawGridOver) {
            maybeDrawGrid(plotGraphics);
        }

        if ( this.bottomDecorator!=null ) drawDecorator(plotGraphics, this.bottomDecorator );
            
        drawContent(plotGraphics); // This is an old hook which is deprecated by bottomDecorator.

        List<Renderer> renderers1= Arrays.asList(getRenderers());

        boolean noneActive = true;
        for (int i = 0; i < renderers1.size(); i++) {
            Renderer rend = (Renderer) renderers1.get(i);
            if (rend.isActive()) {
                logger.log(Level.FINEST, "rendering #{0}: {1}", new Object[]{i, rend});
                try {
                    rend.incrementRenderCount();
                    Painter p= rend.bottomDecorator;
                    if ( p!=null ) drawDecorator(plotGraphics, p );
                    
                    rend.render( (Graphics2D)plotGraphics.create(), lxaxis, lyaxis );
                    
                    p= rend.topDecorator;
                    if ( p!=null ) drawDecorator(plotGraphics, p );
                    
                } catch ( RuntimeException ex ) {
                    logger.log( Level.WARNING, ex.getMessage(), ex );
                    //put breakpoint here:
                    //rend.render(plotGraphics, lxaxis, lyaxis, new NullProgressMonitor());
                    postException(rend,ex);
                }
                noneActive = false;
                if (rend.isDrawLegendLabel()) {
                    addToLegend(rend, null, 0, rend.getLegendLabel());
                }
            } else {
                if (rend.isDrawLegendLabel()) {
                    addToLegend(rend, null, 0, rend.getLegendLabel() + " (inactive)");
                }
            }
        }

        if (drawGridOver) {
            maybeDrawGrid(plotGraphics);
        }
        
        if ( this.topDecorator!=null ) {
            Graphics2D g= (Graphics2D)plotGraphics.create();
            g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
            this.topDecorator.paint(g);
            g.dispose();
        }

        if ( this.isPlotVisible() ) {
            if (renderers1.isEmpty()) {
                postMessage(null, "(no renderers)", DasPlot.INFO, null, null);
                logger.fine("dasPlot has no renderers");
            } else if (noneActive) {
                postMessage(null, "(no active renderers)", DasPlot.INFO, null, null);
            }
        }
    }

    /**
     * return the index of the renderer at canvas location (x,y), or -1 if
     * no renderer is found at the position.
     * @param x the x position on the canvas.
     * @param y the y position on the canvas.  Note 0,0 is the upper left corner.
     * @return the index of the renderer at the position, or -1 if no renderer is at the position.
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
            @Override
            public void actionPerformed(ActionEvent e) {
                Point p = getDasMouseInputAdapter().getMousePressPositionOnCanvas();
                int i = findRendererAt(p.x, p.y);
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

        x = new MouseModule(this, new LengthDragRenderer(this, getXAxis(), getYAxis()), "Length");
        mouseAdapter.addMouseModule(x);

        x = new DisplayDataMouseModule(this);
        mouseAdapter.addMouseModule(x);

        setEnableRenderPropertiesAction(true);
        
        if (DasApplication.hasAllPermission()) {
            JMenuItem dumpMenuItem = new JMenuItem(DUMP_TO_FILE_ACTION);
            mouseAdapter.addMenuItem(dumpMenuItem);
        }
    }
    
    /**
     * Action to dump the first (0th) renderer's data to a file.
     */
    public Action DUMP_TO_FILE_ACTION = new AbstractAction("Dump Data Set to File") {
        @Override
        public void actionPerformed(ActionEvent e) {
            List<Renderer> renderers1= Arrays.asList(getRenderers());
            if (renderers1.isEmpty()) {
                return;
            }
            Renderer renderer = (Renderer) renderers1.get(0);
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter( new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".d2s");
                }
                @Override
                public String getDescription() {
                    return "das2streams";
                } 
            });
            int result = chooser.showSaveDialog(DasPlot.this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                if ( !selected.getName().endsWith(".d2s") ) {
                    selected= new File( selected.getPath() + ".d2s" );
                }
                try {
                    FileChannel out = new FileOutputStream(selected).getChannel();
                    DataSet ds = DataSetAdapter.createLegacyDataSet( DataSetUtil.canonizeFill( renderer.getDataSet() ) );
                    if (ds instanceof TableDataSet) {
                        TableUtil.dumpToAsciiStream((TableDataSet) ds, out);
                    } else if (ds instanceof VectorDataSet) {
                        VectorUtil.dumpToAsciiStream((VectorDataSet) ds, out);
                    }
                } catch (IOException ioe) {
                    DasApplication.getDefaultApplication().getExceptionHandler().handle(ioe);
                }
            }
        }
    };

    /**
     * set the x axis.  This removes property change listeners to the old
     * axis and adds PCLs to the new axis.
     * @param xAxis a horizontal axis.
     */
    public void setXAxis(DasAxis xAxis) {
        Object oldValue = this.xAxis;
        Container parent = getParent();
        if (this.xAxis != null) {
            DasProperties.getLogger().fine("setXAxis upsets the dmia");
            if (parent != null) {
                parent.remove(this.xAxis);
            }
            this.xAxis.removePropertyChangeListener("dataMinimum", rebinListener);
            this.xAxis.removePropertyChangeListener("dataMaximum", rebinListener);
            this.xAxis.removePropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            this.xAxis.removePropertyChangeListener("log", rebinListener);
            this.xAxis.removePropertyChangeListener(DasAxis.PROP_FLIPPED,rebinListener);
            this.xAxis.removePropertyChangeListener(DasAxis.PROPERTY_TICKS, ticksListener);
        }
        this.xAxis = xAxis;
        if (xAxis != null) {
            if (!xAxis.isHorizontal()) {
                throw new IllegalArgumentException("xAxis is not horizontal");
            }
            xAxis.setRow(getRow());
            xAxis.setColumn(getColumn());
            if (parent != null) {
                parent.add(this.xAxis);
                parent.validate();
            }
            xAxis.addPropertyChangeListener("dataMinimum", rebinListener);
            xAxis.addPropertyChangeListener("dataMaximum", rebinListener);
            xAxis.addPropertyChangeListener(DasAxis.PROPERTY_DATUMRANGE, rebinListener);
            xAxis.addPropertyChangeListener("log", rebinListener);
            xAxis.addPropertyChangeListener(DasAxis.PROP_FLIPPED,rebinListener);
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
     * 
     * @param yAxis a vertical axis.
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
            this.yAxis.removePropertyChangeListener(DasAxis.PROP_FLIPPED,rebinListener);
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
            yAxis.addPropertyChangeListener(DasAxis.PROP_FLIPPED,rebinListener);
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
        Renderer[] renderers1= getRenderers();
        for (org.das2.graph.Renderer renderers11 : renderers1) {
            Renderer rend = (Renderer) renderers11;
            if ( rend==null ) {
                logger.info("odd branch presumed to be caused by thread mis-management.");
            } else {
                rend.update();
            }
        }
    }

    /**
     * returns the AffineTransform to transform data from the last updatePlotImage call
     * axes (if super.updatePlotImage was called), or null if the transform is not possible.
     * This is used for the purpose of showing previews.
     * @param xAxis the xaxis of the target coordinate frame
     * @param yAxis the yaxis of the target coordinate frame
     * @return the AffineTransform from the list updatePlotImage call to these
     * axes, if null if this is not possible.
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
     * @param at the transform to be tested.
     */
    private static boolean isIdentity(AffineTransform at) {
        return at.isIdentity() ||
                (Math.abs(at.getScaleX() - 1.00) < 0.001 && Math.abs(at.getScaleY() - 1.00) < 0.001 
                && Math.abs(at.getTranslateX()) < 0.001 && Math.abs(at.getTranslateY()) < 0.001);
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
            gtr.setAlignment(multiLineTextAlignment);
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
     * @param width width in pixels.
     * @param height height in pixels.
     *
     */
    private synchronized void resetCacheImageBounds( boolean printing, int width, int height ) {
        if ( width<=0 || height<=0 ) {
            throw new IllegalArgumentException( "Width ("+width+") and height ("+height+") must be > 0" );
        }
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        
        Rectangle lcacheImageBounds;
        
        if (overSize && !printing ) {
            lcacheImageBounds = new Rectangle();
            lcacheImageBounds.width = 16 * width / 10;
            lcacheImageBounds.height = height;
            lcacheImageBounds.x = x - 3 * width / 10;
            lcacheImageBounds.y = y - 1;
            this.cacheImageBounds= lcacheImageBounds;
            
        } else {
            lcacheImageBounds = new Rectangle();
            lcacheImageBounds.width = width;
            lcacheImageBounds.height = height;
            if ( lcacheImageBounds.width==0 || lcacheImageBounds.height==0 ) {
                try {
                    System.err.println("cheesy code to fix getHeight=0 when printing");
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
                lcacheImageBounds.width = width;
                lcacheImageBounds.height = height;
            }            
            if ( lcacheImageBounds.width==0 || lcacheImageBounds.height==0 ) {
                throw new IllegalArgumentException("width or height is 0.");
            }
            logger.log( Level.FINE, "create cacheImage {0}x{1}", new Object[]{lcacheImageBounds.width, lcacheImageBounds.height});
            BufferedImage lcacheImage = new BufferedImage(lcacheImageBounds.width, lcacheImageBounds.height, BufferedImage.TYPE_4BYTE_ABGR);
            lcacheImageBounds.x = x - 1;
            lcacheImageBounds.y = y - 1;

            this.cacheImageBounds= lcacheImageBounds;
            this.cacheImage= lcacheImage;

        }
    }

    /**
     * we need to call each renderer's updatePlotImage method, otherwise we assume
     * the session is interactive.  We call the getCanvas().isPrintingThread because this is very expensive to
     * run when we are actually printing, where we redo updatePlotImage etc, but we might just be looking to
     * create a thumbnail and existing images may work fine.  All this was introduced because Autoplot was calling
     * canvas.getImage to get thumbnails and for its layout tab, and a quick way to get the canvas image was needed.
     * @param g Java2D graphics context.
     */
    @Override
    protected void printComponent(Graphics g) {
        boolean doInvalidate= getCanvas().isPrintingThread();
        if ( doInvalidate ) { // only if we really are printing from the canvas print method.  getImageNonPrint
            int w= getWidth();
            int h= getHeight();
            if ( w==0 || h==0 ) {
                logger.warning("width or height is zero.  Try printing again.");
                return;
            }
            resetCacheImageBounds(true,w,h);
            
            DasAxis lxaxis= (DasAxis)xAxis.clone(); //TODO: Ed suggests that it might be a clone of the listeners that's causing problems.
            DasAxis lyaxis= (DasAxis)yAxis.clone();
                                
            List<Renderer> renderers1= Arrays.asList(getRenderers());
            for (int i = 0; i < renderers1.size(); i++) {
                Renderer rend = (Renderer) renderers1.get(i);
                if (rend.isActive()) {
                    logger.log(Level.FINEST, "updating renderer #{0}: {1}", new Object[]{i, rend});
                    try {
                        rend.updatePlotImage(lxaxis, lyaxis, new NullProgressMonitor());
                    } catch (DasException ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                    }
                }
            }
        }
        super.printComponent(g);  // this calls paintComponent
        if ( doInvalidate ) {
            invalidateCacheImage();
        }
    }

    /**
     * get the font to use after resizing.
     * @param f0
     * @return 
     */
    private Font getCanvasRenderFont( Font f0 ) {
        try {
            double[] dd= DasDevicePosition.parseLayoutStr(getFontSize());
            if ( dd[1]==1 && dd[2]==0 ) {
                return f0;
            } else {
                Font f= f0;
                double parentSize= f.getSize2D();
                double newSize= dd[1]*parentSize + dd[2];
                f= f.deriveFont((float)newSize);
                return f;
            }
        } catch (ParseException ex) {
            return f0; // and let someone else deal with it!
        }
    }
    
    /**
     * return the Rectangle where data is drawn, useful for clipping.
     * @return Rectangle in canvas coordinates.
     * @see DasDevicePosition#toRectangle(org.das2.graph.DasRow, org.das2.graph.DasColumn) 
     */
    public Rectangle getAxisClip() {
        return DasDevicePosition.toRectangle( getRow(),getColumn() );
    }
    
    @Override
    protected synchronized void paintComponent(Graphics graphics0) {
        logger.log(Level.FINER, "dasPlot.paintComponent {0}", getDasName());
        if ( getCanvas().isValueAdjusting() ) {
            repaint(); // come back soon
            return;
        }

        String localPlotTitle;
        localPlotTitle= getTitle();
        
        double lineThicknessDouble= getLineThicknessDouble(lineThickness);
                
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
        paintComponentCount.incrementAndGet();

        if (getCanvas().isPrintingThread()) {
            logger.fine("* printing thread *");
            if ( testSentinal!=null && this.getName().equals("plot_0" ) ) {
                System.err.println("here we are...");
            }
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

        if ( lineThicknessDouble!=1.f ) {
            if ( lineThicknessDouble>1. ) {
                graphics.setStroke( new BasicStroke((float)lineThicknessDouble,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND) );
            } else {
                graphics.setStroke( new BasicStroke((float)lineThicknessDouble) );
            }
        }

        if ( drawBackground.getAlpha()>0 ) {
            Color c0= graphics0.getColor();
            graphics.setColor(drawBackground);
            Rectangle bckg= DasDevicePosition.toRectangle( getRow(), getColumn() );
            bckg.translate(-x, -getRow().top()+titleHeight );
            graphics.fillRect( bckg.x, bckg.y, bckg.width+1, bckg.height+1 );
            graphics.setColor(c0);
            //graphics.drawRoundRect( bckg.x, bckg.y, bckg.width, bckg.height+2,20,20 );
        }

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.translate(-getX(), -getY());

        Rectangle lcacheImageBounds= this.cacheImageBounds;
        BufferedImage lcacheImage= this.cacheImage;
            
        boolean useCacheImage= cacheImageValid && !getCanvas().isPrintingThread() && !disableImageCache 
                && ( lcacheImageBounds.width==lcacheImage.getWidth() );
        
        Rectangle cacheImageClip= DasDevicePosition.toRectangle( getRow(), getColumn() );
        
        logger.log(Level.FINE, "draw plot useCacheImage: {0}", useCacheImage);
        if ( useCacheImage ) {
            
            Graphics2D atGraphics = (Graphics2D) graphics.create();
            atGraphics.clip(cacheImageClip);

            AffineTransform at = getAffineTransform(xAxis, yAxis);
            if (at == null || (preview == false && !isIdentity(at))) {
                atGraphics.drawImage(lcacheImage, lcacheImageBounds.x, lcacheImageBounds.y, lcacheImageBounds.width, lcacheImageBounds.height, this);
                paintInvalidScreen(atGraphics, at);

            } else {
                if (!at.isIdentity()) {    
                    String atDesc = GraphUtil.getATScaleTranslateString(at);
                    logger.log(Level.FINEST, " using cacheImage w/AT {0}", atDesc);
                    atGraphics.transform(at);
                } else {
                    //atDesc= "identity";
                    logger.log(Level.FINEST, " using cacheImage {0} {1} {2}", new Object[]{lcacheImageBounds, xmemento, ymemento});
                }

                //int reduceHeight=  getRow().getDMinimum() - clip.y;
                //if ( reduceHeight>0 ) {
                //    clip.y+= reduceHeight;
                //    clip.height-= reduceHeight;
                //}
                //clip.translate( getX(), getY() );
                //atGraphics.setClip(clip);

                if ( lcacheImageBounds.width!=lcacheImage.getWidth() ) {
                    logger.log( Level.WARNING, " cbw: {0}  ciw:{1}", new Object[]{lcacheImageBounds.width, lcacheImage.getWidth()});
                }
                
                // Draw the cache image onto the plot.
                atGraphics.drawImage(lcacheImage, lcacheImageBounds.x, lcacheImageBounds.y, lcacheImageBounds.width, lcacheImageBounds.height, this);
                 
            }

            atGraphics.dispose();

        } else {  // don't useCacheImage
            
            synchronized (this) {
                Graphics2D plotGraphics;
                if (getCanvas().isPrintingThread() || disableImageCache) {
                    plotGraphics = (Graphics2D) graphics.create(x - 1, y - 1, xSize + 2, ySize + 2);
                    int w= getWidth();
                    int h= getHeight();
                    if ( w==0 || h==0 ) {
                        return;
                    }
                    resetCacheImageBounds(true,w,h);
                    logger.finest(" printing thread, drawing");
                    lcacheImage= null;
                    lcacheImageBounds= null;
                    
                } else {
                    int w= getWidth();
                    int h= getHeight();
                    if ( w==0 || h==0 ) {
                        return;
                    }
                    resetCacheImageBounds(false,w,h);   
                    lcacheImageBounds= this.cacheImageBounds;
                    if ( lcacheImageBounds.width==0 || lcacheImageBounds.height==0 ) {
                        logger.info("https://sourceforge.net/p/autoplot/bugs/1076/");
                        return;
                    }
                    lcacheImage = new BufferedImage(lcacheImageBounds.width, lcacheImageBounds.height,
                            BufferedImage.TYPE_4BYTE_ABGR);
                    plotGraphics = (Graphics2D) lcacheImage.getGraphics();
                    plotGraphics.setBackground(getBackground());
                    plotGraphics.setColor(getForeground());
                    plotGraphics.setFont(getFont());
                    plotGraphics.setRenderingHints(org.das2.DasProperties.getRenderingHints());
                    if (overSize) {
                        plotGraphics.translate(x - lcacheImageBounds.x - 1, y - lcacheImageBounds.y - 1);
                    }

                    
                    logger.finest(" rebuilding cacheImage");

                }

                plotGraphics.translate(-x + 1, -y + 1);
                plotGraphics.clip(cacheImageClip);
                
                // check mementos before drawing.  They should all be the same.  See https://sourceforge.net/tracker/index.php?func=detail&aid=3075655&group_id=199733&atid=970682
                Renderer[] rends= getRenderers();

                //for ( int i=0; i<rends.length; i++ ) {
                //    System.err.println( "renderer #"+i+": " +rends[i] + " ds="+rends[i].getDataSet() );
                //}

                DasAxis lxaxis= (DasAxis)getXAxis().clone();
                DasAxis lyaxis= (DasAxis)getYAxis().clone();
                
                Memento xmem= lxaxis.getMemento();
                Memento ymem= lyaxis.getMemento();                
                
                if ( rends.length>0 ) {
                    for ( Renderer r: rends ) {
                        boolean dirt= false;
                        if ( r.getXmemento()==null || !r.getXmemento().equals(xmem) ) dirt= true;
                        if ( r.getYmemento()==null || !r.getYmemento().equals(ymem) ) dirt= true;
                        if ( dirt ) {
                            try {
                                logger.log(Level.FINE,"calling updatePlotImage again because of memento");
                                r.updatePlotImage( lxaxis, lyaxis, new NullProgressMonitor());
                            } catch (DasException ex) {
                                ex.printStackTrace();
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                            }
                        } else {
                            logger.log(Level.FINE,"skipping updatePlotImage because memento indicates things are okay");
                        }
                    }
                    //Memento xmem2= getXAxis().getMemento();  // I showed that the mementos don't change.  THIS IS ONLY BECAUSE UPDATES ARE DONE ON THE EVENT THREAD
                    //System.err.println("mementocheck: "+xmem2.equals(xmem));
                }

                drawCacheImage(plotGraphics,lxaxis,lyaxis);
                //Memento xmem2= getXAxis().getMemento();  // I showed that the mementos don't change.  THIS IS ONLY BECAUSE UPDATES ARE DONE ON THE EVENT THREAD.  I'm not sure of this, making a local copy of the axes appears to fix the problem.
                //if ( !xmem2.equals(xmem) ) {
                //    System.err.println("mementocheck: "+xmem2.equals(xmem));
                //}
                
                plotGraphics.dispose();
                
            }


            if ( !disableImageCache && !getCanvas().isPrintingThread() ) {
                assert lcacheImageBounds!=null;
                cacheImageValid = true;
                graphics.drawImage(lcacheImage, lcacheImageBounds.x, lcacheImageBounds.y, lcacheImageBounds.width, lcacheImageBounds.height, this);

                xmemento = xAxis.getMemento();
                ymemento = yAxis.getMemento();

                logger.log(Level.FINEST, "recalc cacheImage, xmemento={0} ymemento={1}", new Object[]{xmemento, ymemento});
                
                cacheImage= lcacheImage;
                cacheImageBounds= lcacheImageBounds;
            }
        }

        graphics.setColor(getForeground());
        
        if ( plotVisible ) {
            graphics.drawRect(x - 1, y - 1, xSize + 1, ySize + 1);
        }
        
        if ( displayTitle && localPlotTitle != null && localPlotTitle.length() != 0) {
            String t= localPlotTitle;
            if ( localPlotTitle.contains("%{CONTEXT}") ) {
                String contextStr= this.context==null ? "" : this.context.toString();
                t= t.replace("%{CONTEXT}",contextStr);
            }
            
            if ( fontSize.length()>0 && !fontSize.equals("1em") ) {
                try {
                    double[] dd= DasDevicePosition.parseLayoutStr(getFontSize());
                    if ( dd[1]==1 && dd[2]==0 ) {
                        // do nothing
                    } else {
                        Font f= graphics.getFont();
                        double parentSize= f.getSize2D();
                        double newSize= dd[1]*parentSize + dd[2];
                        f= f.deriveFont((float)newSize);    
                        graphics.setFont(f);
                    }
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }

            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setAlignment(GrannyTextRenderer.CENTER_ALIGNMENT); // funny I never noticed this was different.
            gtr.setString(graphics, t);
            int titleWidth = (int) gtr.getWidth();
            int titleX = x + (xSize - titleWidth) / 2;
            int titleY = y - (int) gtr.getDescent() - (int) gtr.getAscent() / 2;
            
//            if ( true ) { // bug https://sourceforge.net/p/autoplot/bugs/433/: if we were to prevent ticks from clobbering the background...  This is not trivial though, because the title is below the axis in the Z-order.
//                Color c= graphics.getColor();
//                graphics.setColor(Color.GRAY); //graphics.getBackground() );
//                Rectangle r= gtr.getBounds();
//                r.translate( titleX, titleY );
//                graphics.fillRoundRect( r.x, r.y, r.width, r.height, 3, 3 );
//                graphics.setColor( c );
//            }
            gtr.draw(graphics, (float) titleX, (float) titleY);
        }

        // --- draw messages ---
        List<MessageDescriptor> lmessages= this.messages==null ? null : new ArrayList(this.messages);
        if ( lmessages!=null && lmessages.size() > 0) {
            drawMessages(graphics,lmessages);
        }

        List<LegendElement> llegendElements= this.legendElements==null ? null : new ArrayList(this.legendElements);
        if ( llegendElements!=null && llegendElements.size() > 0 && displayLegend ) {
            drawLegend(graphics,llegendElements);
        }
        
        if ( drawDebugMessages ) {
            int nr= this.getRenderers().length;
            int size=30;
            if ( nr>5 ) {
                size=20;
                graphics.setFont( graphics.getFont().deriveFont(7.f) );
            }
            int ir= 0;
            int xx= x+xSize-100-size/3;
            int yy= y+ySize-nr*size-size/3-graphics.getFontMetrics().getHeight();
            Color c0= graphics.getColor();
            graphics.setColor( new Color( 255, 200, 255, 200 ) );
            graphics.fillRoundRect( xx, yy, 100, nr*size+graphics.getFontMetrics().getHeight(), 10, 10 );
            graphics.setColor(c0);
            graphics.drawRoundRect( xx, yy, 100, nr*size+graphics.getFontMetrics().getHeight(), 10, 10 );
            for ( Renderer r: this.getRenderers() ) {
                GrannyTextRenderer gtr= new GrannyTextRenderer();
                gtr.setString( graphics, String.format( "update: %d!crender: %d!c", r.getUpdateCount(), r.getRenderCount() ) );
                gtr.draw( graphics, xx+10, yy+10+ir*size );
                ir++;
            }
            graphics.drawString( "paint: "+this.paintComponentCount, xx+10, yy+20+ir*30-graphics.getFontMetrics().getHeight() );
        }
        
        graphics.dispose();

        getDasMouseInputAdapter().paint(graphics0);

        if (saveClip != null) {
            graphics0.setClip(saveClip);
        }
    }

    /**
     * add or remove the menu item to get at the renderer properties.
     * Das2 applications generally allow this so that applications don't have
     * to bother handling this.  Others like Autoplot have their own systems 
     * and uses for the click.
     * @param b false to remove the menu item if it has been added.
     */
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

    /**
     * In Autoplot, we need a way to get help releasing all resources.  This
     * clears out all the mouse modules, axis references, etc.  Basically anything
     * that could have a reference to other parts of the system that we know we
     * don't need here, explicitly remove the reference.
     */
    public void releaseAll() {
        uninstallComponent();
        DasMouseInputAdapter dmia= getDasMouseInputAdapter();
        dmia.releaseAll();
        this.xAxis.getDasMouseInputAdapter().releaseAll();
        this.yAxis.getDasMouseInputAdapter().releaseAll();
    }

    /**
     * messages displayed for user, typically on the upper left of the plot.
     */
    private static class MessageDescriptor {

        /**
         * the renderer posting the text, or null if the plot owns the text
         */
        Renderer renderer;
        String text;

        /**
         * The severity of the message.  DasPlot.INFO or Level.INFO.intValue().
         */
        int messageType;
        //Datum x;
        //Datum y;
        Rectangle bounds; // stores the drawn boundaries of the message for context menu.
        
        /**
         * birth milli of the message, used for hiding after a timeout has elapsed.  When this
         * is Long.MAX_VALUE, the message ought never be hidden.
         */
        long birthMilli;

        MessageDescriptor( Renderer renderer, String text, int messageType, Datum x, Datum y ) {
            this.renderer = renderer;
            this.text = text;
            this.messageType = messageType;
            //this.x = x;
            //this.y = y;
            if ( renderer instanceof DigitalRenderer ) {
                this.birthMilli= Long.MAX_VALUE; // TODO: kludge because we don't want timeouts to remove these messages.
            } else {
                this.birthMilli=System.currentTimeMillis();
            }
        }
    }

    /**
     * elements to indicate in the legend, typically on the upper right of the plot.
     */
    private static class LegendElement {

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
     * These levels are now taken from java.util.logging.Level.  Generally
     * INFO messages are displayed.
     */
    public static final int INFO = Level.INFO.intValue();
    
    /**
     * mark the log message as a warning where the operator should be aware.
     */
    public static final int WARNING = Level.WARNING.intValue();
    
    /**
     * mark the log message as severe, where an error condition has occurred.
     */
    public static final int SEVERE = Level.SEVERE.intValue(); // this was ERROR before Feb 2011.

    List<MessageDescriptor> messages;
    List<LegendElement> legendElements;

    /**
     * Notify user of an exception, in the context of the plot.  A position in
     * the data space may be specified to locate the text within the data context.
     * Note either or both x or y may be null.  Messages must only be posted while the
     * Renderer's render method is called, not during updatePlotImage.  All messages are
     * cleared before the render step. (TODO:check on this)
     * 
     * @param renderer identifies the renderer posting the exception
     * @param message the text to be displayed, may contain granny text.
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
     * @param message the text to be displayed, may contain granny text.
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
     * @param renderer the renderer posting the exception.
     * @param exception the exception to post.
     */
    public void postException( Renderer renderer, Exception exception ) {
        String message = exception.getMessage();
        if (message == null || message.length()<7 ) {  // ArrayIndexOutOfBounds message was just "16"
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
     * Add the message to the messages in the legend.
     * @param renderer identifies the renderer adding to the legend
     * @param icon if non-null, an icon to use.  If null, the renderer's icon is used.
     * @param pos integer order parameter, and also identifies item.
     * @param message String message to display.  
     */
    public void addToLegend(Renderer renderer, ImageIcon icon, int pos, String message) {
        legendElements.add(new LegendElement(icon, renderer, message));
    }
    
    /**
     * Used at APL for layout.  This is off by default.  This appears to shift the Y position.
     * @param reduceOutsideLegendTopMargin shift the Y position.
     */
    public void setReduceOutsideLegendTopMargin(boolean reduceOutsideLegendTopMargin) {
    	this.reduceOutsideLegendTopMargin = reduceOutsideLegendTopMargin;
        update();
    }

    /**
     * draw a grid at the xticks and yticks
     * @param g the graphics context.
     * @param xticks the xticks
     * @param yticks the yticks.
     */
    private void drawGrid(Graphics2D g, DatumVector xticks, DatumVector yticks) {
        Rectangle lcacheImageBounds= getCacheImageBounds(); // make a local copy for thread safety.
        
        int xmin = lcacheImageBounds.x;
        int xmax = lcacheImageBounds.x + lcacheImageBounds.width;
        int ymin = lcacheImageBounds.y;
        int ymax = lcacheImageBounds.y + lcacheImageBounds.height;

        if (yticks != null && yticks.getUnits().isConvertibleTo(yAxis.getUnits())) {
            for (int i = 0; i < yticks.getLength(); i++) {
                int y = (int) yAxis.transform(yticks.get(i));
                g.drawLine(xmin, y, xmax, y);
            }
        }
        if (xticks != null && xticks.getUnits().isConvertibleTo(xAxis.getUnits())) {
            for (int i = 0; i < xticks.getLength(); i++) {
                int x = (int) xAxis.transform(xticks.get(i));
                g.drawLine(x, ymin, x, ymax);
            }
        }
    }

    /**
     * this is a stub (empty method) that can be overridden to draw content.  
     * This can be used to add annotations to plots, but see DasCanvas 
     * addTopDecorator as well.  This is drawn above the grid when it is the bottom, 
     * and below the data.
     * @param g the graphics context.
     * @see DasCanvas#addTopDecorator(org.das2.graph.Painter) 
     */
    protected void drawContent(Graphics2D g) {
        // override me to add to the axes.
    }

    /**
     * recalculate the bounds of the component.  TODO: this is used a lot and needs to be explained in more detail.
     */
    @Override
    public void resize() {
        logger.finer("resize DasPlot");
        if (isDisplayable()) {
            Rectangle oldBounds= getBounds();

            GrannyTextRenderer gtr = new GrannyTextRenderer();
            gtr.setAlignment(multiLineTextAlignment);
            
            Font f= getCanvasRenderFont(getFont());
            gtr.setString( f, getTitle());

            titleHeight = (int) gtr.getHeight() + (int) gtr.getAscent() / 2;

            //if ( this.getDasName().startsWith("plot_3")) {
            //    System.err.println("here");
            //}
            List<LegendElement> llegendElements= this.legendElements==null ? null : new ArrayList(this.legendElements);
            Rectangle legendBounds= getLegendBounds((Graphics2D) getGraphics(), 0, 0, llegendElements );

            double lineThicknessDouble= getLineThicknessDouble( lineThickness );        

            Rectangle bounds = new Rectangle();
            bounds.x = getColumn().getDMinimum() - 1 - (int)( lineThicknessDouble/2. );
            bounds.y = getRow().getDMinimum() - 1 - (int)( lineThicknessDouble/2. );
            // if legend label is outside the plot, then we'll do something here.  Note this will cause the data to be drawn out-of-bounds as well.

            bounds.width = getColumn().getDMaximum() - bounds.x + 1 + (int)( lineThicknessDouble / 2 );
            bounds.height = getRow().getDMaximum() - bounds.y + 1 + (int)( lineThicknessDouble / 2 );
            if ( displayTitle && !getTitle().equals("") ) {
                bounds.y -= titleHeight;
                bounds.height += titleHeight;
            }

            if ( legendBounds!=null ) bounds.add(legendBounds);
            
//            if ( isotropic ) {
//                if ( oldBounds.width==bounds.width ) {
//                    System.err.println("*** check isotropic because of resize width");  //TODO: 2202
//                    checkIsotropic( this, xAxis );
//                } else {
//                    System.err.println("*** check isotropic because of resize");  //TODO: 2202
//                    checkIsotropic( this, yAxis );
//                }
//            }
            // TODO check bounds.height<10
            logger.log(Level.FINER, "DasPlot setBounds {0}", bounds);
            if ( !bounds.equals(oldBounds) ) {
                setBounds(bounds);
                SwingUtilities.invokeLater( new Runnable() {
                   @Override
                   public void run() {
                       List<Renderer> renderers1= Arrays.asList(getRenderers());
                       for (org.das2.graph.Renderer renderers11 : renderers1) {
                           ((Renderer) renderers11).refresh();
                       }
                       invalidateCacheImage();
                   }
                });
            }
        }
    }

    /** 
     * Sets the title which will be displayed above this plot.  
     * null or empty string may be used to turn off the title.
     * 
     * @param t The new title for this plot.
     * @see #setDisplayTitle(boolean) 
     */
    public void setTitle(String t) {
        
        Object oldValue;
        synchronized (this) {
            oldValue= plotTitle;
            plotTitle = t;
        }
        
        logger.log(Level.FINE, "setTitle(\"{0}\")", t);
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

    /** 
     * Returns the title of this plot.
     * @return The plot title
     * @see #setTitle(String)
     */
    public synchronized String getTitle() {
        return plotTitle;
    }

    /**
     * turns the plot title off or on.  
     */
    public static final String PROP_DISPLAYTITLE="displayTitle";

    /**
     * return the state of enable/disable display of the title.
     * @return the current state.
     * @see #setDisplayTitle(boolean) 
     */
    public boolean isDisplayTitle() {
        return this.displayTitle;
    }

    /**
     * enable/disable display of the title.  Often the title contains
     * useful information describing the plot content that we want to know during
     * interactive use but not when the plot is printed.
     * @param v true if the title should be displayed.
     * @see #isDisplayTitle() 
     */
    public void setDisplayTitle(boolean v) {
        boolean old= this.displayTitle;
        this.displayTitle= v;
        firePropertyChange( PROP_DISPLAYTITLE, old, v );
        resize();
        invalidateCacheImage();
    }
    
    protected Painter bottomDecorator = null;

    public static final String PROP_BOTTOMDECORATOR = "bottomDecorator";

    public Painter getBottomDecorator() {
        return bottomDecorator;
    }

    /**
     * add additional painting code to the renderer, which is called after 
     * the renderer is called.
     * @param bottomDecorator the Painter to call, or null to clear.
     */
    public void setBottomDecorator(Painter bottomDecorator) {
        Painter oldBottomDecorator = this.bottomDecorator;
        this.bottomDecorator = bottomDecorator;
        firePropertyChange(PROP_BOTTOMDECORATOR, oldBottomDecorator, bottomDecorator);
        repaint();
    }    
    
    
    protected Painter topDecorator = null;

    public static final String PROP_TOPDECORATOR = "topDecorator";

    public Painter getTopDecorator() {
        return topDecorator;
    }

    /**
     * add additional painting code to the renderer, which is called after 
     * the renderer is called.
     * @param topDecorator the Painter to call, or null to clear.
     */
    public void setTopDecorator(Painter topDecorator) {
        Painter oldTopDecorator = this.topDecorator;
        this.topDecorator = topDecorator;
        firePropertyChange(PROP_TOPDECORATOR, oldTopDecorator, topDecorator);
        repaint();
    }    
    
    /**
     * property where the plot context can be stored.  
     * @see #setContext(org.das2.datum.DatumRange) 
     */
    public static final String PROP_CONTEXT= "context";
    
    /**
     * convenient place to put the plot context.  The context is used to
     * store the timerange when there is no axis for it, for example, to
     * show the state of data during a range.  This may change to a QDataSet
     * to provide several context dimensions.
     */
    private DatumRange context= null;

    /**
     * convenient place to put the plot context.  The context is used to
     * store the timerange when there is no axis for it, for example, to
     * show the state of data during a range.  This may change to a QDataSet
     * to provide several context dimensions.
     * @return the context
     * @see #setContext(org.das2.datum.DatumRange) 
     */
    public DatumRange getContext() {
        return context;
    }
    
    /**
     * convenient place to put the plot context.  The context is used to
     * store the timerange when there is no axis for it, for example, to
     * show the state of data during a range.  This may change to a QDataSet
     * to provide several context dimensions.  Note this may be null to support
     * no context.
     * @param context the context
     * @see #getContext() 
     */
    public void setContext(DatumRange context) {
        DatumRange old= this.context;
        this.context = context;
        firePropertyChange( PROP_CONTEXT, old, context );
    }

    /**
     * necessary place to put the range of the data actually displayed.  The context is the controller,
     * and the displayContext closes the loop.  This is mostly here to provide legacy support to Autoplot which
     * abused the context property as both a write and read, and note there's a small problem that displayed
     * items may have different display contexts.  So this property should be used carefully, and generally
     * when just one thing is visible.
     */
    public static final String PROP_DISPLAY_CONTEXT= "displayContext";

    /**
     * @see #PROP_DISPLAY_CONTEXT
     */
    DatumRange displayContext= null;

    /**
     * @see #PROP_DISPLAY_CONTEXT 
     * @return the property value
     */
    public DatumRange getDisplayContext() {
        return displayContext;
    }

    /**
     * set the property
     * @param displayContext null or the display context.
     * @see #PROP_DISPLAY_CONTEXT
     */
    public void setDisplayContext(DatumRange displayContext) {
        DatumRange old= this.displayContext;
        this.displayContext = displayContext;
        firePropertyChange( PROP_DISPLAY_CONTEXT, old, displayContext );
    }


    private List<Renderer> renderers = null;

    /**
     * return the x (horizontal) axis.
     * @return the x axis
     */
    public DasAxis getXAxis() {
        return this.xAxis;
    }

    /**
     * return the y (vertical) axis 
     * @return the y axis
     */
    public DasAxis getYAxis() {
        return this.yAxis;
    }

    protected class RebinListener implements java.beans.PropertyChangeListener {
        @Override
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
            
            markDirty("rebinListener");
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
        for (org.das2.graph.Renderer r1 : r) {
            r1.installRenderer();
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
        for (org.das2.graph.Renderer r1 : r) {
            if ( r1==null ) {
                System.err.println("strange hudson case of NullPointerException"); // I don't see how this could happen.  See http://sarahandjeremy.net:8080/hudson/job/autoplot-test002/8420/console
                continue;
            }
            r1.uninstallRenderer();
        }
    }

    /**
     * add the renderer to the stack of renderers.  It will be the 
     * last element drawn.
     * @param rend the renderer
     */
    public void addRenderer(Renderer rend) {
        logger.log(Level.FINE, "addRenderer({0})", rend);
        if ( rend==null ) {
            throw new NullPointerException("added renderer was null");
        }
        DasPlot parent= rend.getParent();
        if ( parent != null) {
            parent.removeRenderer(rend);
        }
        renderers.add(rend);
        rend.setParent( this );
        if (getCanvas() != null) {
            rend.installRenderer();
        }
        rend.update();
        invalidateCacheImage();
    }

    /**
     * add the renderer to the stack of renderers at the given index.
     * Index 0 is drawn first.  This will insert the renderer at the position.
     * @param index the index, 0 is drawn first.
     * @param rend the renderer
     */
    public void addRenderer(int index, Renderer rend) {
        logger.log(Level.FINE, "addRenderer({0},{1})", new Object[]{index, rend});
        if ( rend==null ) {
            throw new NullPointerException("added renderer was null");
        }
        DasPlot parent= rend.getParent();
        if ( parent != null) {
            parent.removeRenderer(rend);
        }
        renderers.add(index,rend);
        rend.setParent(this);
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
        rend.setParent( null );
        invalidateCacheImage();

    }

    /**
     * since the plotVisible property can be false and the plot having 
     * no renderers, we want the mouse events to pass on to elements below them.
     * @param x the mouse x
     * @param y the mouse y
     * @return true if the plot "contains" this point.
     */
    @Override
    public boolean contains(int x, int y) {
        if ( this.plotVisible==false ) {
            boolean contains= false;
            for ( Renderer r : renderers ) {
                if ( r.isActive() ) {
                    contains= true;
                }
            }
            return contains;
        } else {
            return super.contains(x, y); //To change body of generated methods, choose Tools | Templates.
        }
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
                rend.setParent( null ); // should get GC'd.
            }
            setFocusRenderer(null);
            renderers.clear();
        }
        invalidateCacheImage();

    }

    /**
     * provide convenient method for creating a plot.  For example:
     *<blockquote><pre>{@code
     *DasCanvas c= new DasCanvas(400,400);
     *DasPlot p= DasPlot.createDummyPlot( );
     *c.add(p,DasRow.create(c,0,1),DasColumn.create(c,0,1));
     *JOptionPane.showConfirmDialog(None,c)
     *}</pre></blockquote>
     * @return a DasPlot, reader to be added to a canvas.
     */
    public static DasPlot createDummyPlot() {
        DasAxis xAxis = new DasAxis(Datum.create(-10), Datum.create(10), DasAxis.HORIZONTAL);
        DasAxis yAxis = new DasAxis(Datum.create(-10), Datum.create(10), DasAxis.VERTICAL);
        DasPlot result = new DasPlot(xAxis, yAxis);
        return result;
    }

    /**
     * provide convenient method for creating a plot.  For example:
     *<blockquote><pre>{@code 
     *DasCanvas c= new DasCanvas(400,400);
     *DasPlot p= DasPlot.createPlot( DatumRangeUtil.parseTimeRange('2001'),DatumRange.newDatumRange(0,10,Units.dimensionless) );
     *c.add(p,DasRow.create(c,0,1),DasColumn.create(c,0,1));
     *JOptionPane.showConfirmDialog(None,c)
     *}</pre></blockquote>
     * @param xrange the range for the x axis.
     * @param yrange the range for the y axis
     * @return a DasPlot, reader to be added to a canvas.
     */
    public static DasPlot createPlot(DatumRange xrange, DatumRange yrange) {
        DasAxis xAxis = new DasAxis(xrange, DasAxis.HORIZONTAL);
        DasAxis yAxis = new DasAxis(yrange, DasAxis.VERTICAL);
        DasPlot result = new DasPlot(xAxis, yAxis);
        return result;
    }

    /**
     * return one of the renderers, which paint the data on to the plot.
     * @param index the index of the renderer
     * @return the renderer
     */
    public synchronized Renderer getRenderer(int index) {
        return (Renderer) renderers.get(index);
    }

    /**
     * return a list of the renderers, which paint the data on to the plot.
     * This makes a copy of the renderer array.
     * @return the Renderer
     */
    public synchronized Renderer[] getRenderers() {
        return (Renderer[]) renderers.toArray(new Renderer[renderers.size()]);
    }


    private class PlotDnDSupport extends org.das2.util.DnDSupport {

        PlotDnDSupport(org.das2.util.DnDSupport parent) {
            super(DasPlot.this, DnDConstants.ACTION_COPY_OR_MOVE, parent);
        }

        public void drop(DropTargetDropEvent dtde) {
        }

        @Override
        protected int canAccept(DataFlavor[] flavors, int x, int y, int action) {
            for (DataFlavor flavor : flavors) {
                if (flavor.equals(TransferableRenderer.RENDERER_FLAVOR)) {
                    return action;
                }
            }
            return -1;
        }

        @Override
        protected void done() {
        }

        @Override
        protected boolean importData(Transferable t, int x, int y, int action) {
            boolean success = false;
            try {
                Renderer r = (Renderer) t.getTransferData(TransferableRenderer.RENDERER_FLAVOR);
                addRenderer(r);
                revalidate();
                success = true;
            } catch (UnsupportedFlavorException | IOException ufe) {
            }
            return success;
        }

        @Override
        protected Transferable getTransferable(int x, int y, int action) {
            return null;
        }

        @Override
        protected void exportDone(Transferable t, int action) {
        }
    }

    /**
     * return a Shape containing the plot.
     * @return a shape containing the active plot.
     */
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

    /**
     * request that the plot be repainted.  This contains commented code that
     * counts the number of repaints.
     */
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
        if ( cacheImageValid==false ) {
            logger.fine("cacheImage was already invalid, reposting update.");
        } else {
            cacheImageValid = false;
        }
        super.markDirty("invalidateCacheImage");
        update();
    }

    /**
     * mark the dasPlot's cache image as invalid, but from the update method, so
     * we don't need to update. (Updating results in a loop.)
     */
    public void invalidateCacheImageNoUpdate() {
        cacheImageValid = false;
        //super.markDirty("invalidateCacheImageNoUpdate");
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
        super.markDirty("withRepaint");
        //if ( isotropic ) {
        //    checkIsotropic( DasPlot.this, null );
        //}
        repaint();
    }


    private LegendPosition legendPosition = LegendPosition.NE;

    /**
     * the location of the legend position, which can be one of the four corners, 
     * or outside and to the right of the plot.
     */
    public static final String PROP_LEGENDPOSITION = "legendPosition";

    /**
     * @see #PROP_LEGENDPOSITION
     * @see LegendPosition
     * @return the current legend position
     */
    public LegendPosition getLegendPosition() {
        return this.legendPosition;
    }

    /**
     * @see #PROP_LEGENDPOSITION
     * @param newlegendPosition the legend position
     */
    public void setLegendPosition(LegendPosition newlegendPosition) {
        LegendPosition oldlegendPosition = legendPosition;
        this.legendPosition = newlegendPosition;
        firePropertyChange(PROP_LEGENDPOSITION, oldlegendPosition, newlegendPosition);
        resize();
        repaint();
    }

    /**
     * relative font size for the legend.  For example, -2 will use sans-8 when the
     * plot font is sans-10.
     */
    public static final String PROP_LEGENDRELATIVESIZESIZE= "legendRelativeFontSize";
    
    private int legendRelativeFontSize= 0;
                
    /**
     * @see #PROP_LEGENDRELATIVESIZESIZE
     * @return the current relative size of the legend in points.
     */
    public int getLegendRelativeFontSize() {
        return legendRelativeFontSize;
    }
    
    /**
     * set the relative size of the legend element text.
     * @see #PROP_LEGENDRELATIVESIZESIZE
     * @param size relative size in points.
     */
    public void setLegendRelativeFontSize( int size ) {
        int oldF= this.legendRelativeFontSize;
        this.legendRelativeFontSize= size;
        firePropertyChange( PROP_LEGENDRELATIVESIZESIZE, oldF, size );
        repaint();
    }
    
    private String fontSize = "1em";

    public static final String PROP_FONTSIZE = "fontSize";

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        String oldFontSize = this.fontSize;
        this.fontSize = fontSize;
        firePropertyChange(PROP_FONTSIZE, oldFontSize, fontSize);
    }
    
    private boolean displayLegend = true;
    
    /**
     * true if the legend should be displayed
     */
    public static final String PROP_DISPLAYLEGEND = "displayLegend";

    /**
     * true if the legend should be displayed
     * @return true if the legend should be displayed
     */
    public boolean isDisplayLegend() {
        return displayLegend;
    }

    /**
     * true if the legend should be displayed
     * @param displayLegend true if the legend should be displayed
     */
    public void setDisplayLegend(boolean displayLegend) {
        boolean oldDisplayLegend = this.displayLegend;
        this.displayLegend = displayLegend;
        firePropertyChange(PROP_DISPLAYLEGEND, oldDisplayLegend, displayLegend);
        resize();
        repaint();
    }

    private Color drawBackground = new Color(0, 0, 0, 0);
    
    /**
     * the background should be drawn, if alpha is &gt;0.
     */
    public static final String PROP_DRAWBACKGROUND = "drawBackground";

    /**
     * return the background color, where alpha=0 (transparent) means don't draw the background.
     * @return the background color.
     */
    public Color getDrawBackground() {
        return drawBackground;
    }

    /**
     * if not transparent, draw this background first.
     * @param drawBackground the background, or a color with no opacity (new Color(0,0,0,0)) to turn off.
     */
    public void setDrawBackground(Color drawBackground) {
        Color oldDrawBackground = this.drawBackground;
        this.drawBackground = drawBackground;
        invalidateCacheImage();
        repaint();
        firePropertyChange(PROP_DRAWBACKGROUND, oldDrawBackground, drawBackground);
    }

        
    private Color drawGridColor = new Color(0, 0, 0, 0);
    
    /**
     * if not transparent, draw the grid in this color.  Otherwise
     * the grid is drawn with the tick color.
     */
    public static final String PROP_DRAWGRIDCOLOR = "drawGridColor";

    /**
     * @see #PROP_DRAWGRIDCOLOR
     * @return the grid color
     */
    public Color getDrawGridColor() {
        return drawGridColor;
    }

    /**
     * if not transparent, draw the grid in this color.  Otherwise
     * the grid is drawn with the tick color.
     * @param drawGridColor the color
     */
    public void setDrawGridColor(Color drawGridColor) {
        Color oldDrawGridColor = this.drawGridColor;
        this.drawGridColor = drawGridColor;
        invalidateCacheImage();
        repaint();
        firePropertyChange(PROP_DRAWGRIDCOLOR, oldDrawGridColor, drawGridColor);
    }


    /**
     * property drawGrid.  If true, faint grey lines continue the axis major
     * ticks across the plot.
     */
    private boolean drawGrid = false;

    /**
     * If true, faint grey lines continue the axis major
     * ticks across the plot.
     */
    public static final String PROP_DRAWGRID = "drawGrid";
 
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
    
    private boolean drawMinorGrid;
    
        
    /**
     * If true, faint grey lines continue the axis minor
     * ticks across the plot.
     */
    public static final String PROP_DRAWMINORGRID = "drawMinorGrid";

    /**
     * Get the value of drawMinorGrid
     * @return the value of drawMinorGrid
     * @see #PROP_DRAWMINORGRID
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
    
    private boolean drawGridOver = true;
    
    /**
     * if true, then the grid is on top of the data.
     */
    public static final String PROP_DRAWGRIDOVER = "drawGridOver";

    /**
     * @see #PROP_DRAWGRIDOVER
     * @return true if the grid is on top of the data.
     */
    public boolean isDrawGridOver() {
        return drawGridOver;
    }

    /**
     * @see #PROP_DRAWGRIDOVER
     * @param gridOver if the grid is on top of the data.
     */
    public void setDrawGridOver(boolean gridOver) {
        boolean oldGridOver = this.drawGridOver;
        this.drawGridOver = gridOver;
        this.invalidateCacheImage();
        this.repaint();
        firePropertyChange(PROP_DRAWGRIDOVER, oldGridOver, gridOver);
    }

    private String lineThickness = "1px";

    public static final String PROP_LINETHICKNESS = "lineThickness";

    public String getLineThickness() {
        return lineThickness;
    }

    /**
     * set the thickness of the lines drawn.
     * @param lineThickness 
     */
    public void setLineThickness(String lineThickness) {
        String oldLineThickness = this.lineThickness;
        this.lineThickness = lineThickness;
        this.repaint();
        firePropertyChange(PROP_LINETHICKNESS, oldLineThickness, lineThickness);
    }
    
    /**
     * if true then the data rendering will be scaled immediately to indicate 
     * axis state changes.
     * @param preview true if preview should be enabled.
     */
    public void setPreviewEnabled(boolean preview) {
        this.preview = preview;
    }

    /**
     * true if the data rendering will be scaled immediately to indicate 
     * axis state changes.
     * @return true if preview is enabled.
     */
    public boolean isPreviewEnabled() {
        return this.preview;
    }

    /**
     * set the visibility of both the plot and its x and y axes.  Recently,
     * setVisible(v) would do this, but it incorrectly couples the visible properties
     * of the separate components.
     *
     * @param visible false if the x and y axes, and the plot is visisble. 
     */
    public void setAxisPlotVisible( boolean visible ) {
        this.setVisible(visible);
        this.xAxis.setVisible(visible);
        this.yAxis.setVisible(visible);
    }

    private boolean plotVisible = true;

    public static final String PROP_PLOTVISIBLE = "plotVisible";

    public boolean isPlotVisible() {
        return plotVisible;
    }

    public void setPlotVisible(boolean plotVisible) {
        boolean oldPlotVisible = this.plotVisible;
        this.plotVisible = plotVisible;
        firePropertyChange(PROP_PLOTVISIBLE, oldPlotVisible, plotVisible);
    }


    private boolean overSize = false;
    
    /**
     * boolean property indicating that the data outside the axis
     * bounds is rendered, to smooth animation when the axis is panned.
     */
    public static final String PROP_OVERSIZE = "overSize";

    /**
     * @see #PROP_OVERSIZE
     * @return oversize property
     */
    public boolean isOverSize() {
        return overSize;
    }

    /**
     * @param overSize true means draw outside the axis bounds.
     * @see #PROP_OVERSIZE
     */
    public void setOverSize(boolean overSize) {
        boolean oldOverSize = this.overSize;
        this.overSize = overSize;
        invalidateCacheImage();
        firePropertyChange(PROP_OVERSIZE, oldOverSize, overSize);
    }

    /**
     * the log level for the messages on the screen.
     */
    public static final String PROP_LOG_LEVEL = "logLevel";
    
    private Level logLevel= Level.INFO;
    
    /**
     * @param level where Level.INFO is the default.
     * @see #PROP_LOG_LEVEL
     */
    public void setLogLevel( Level level ) {
        Level oldLevel= this.logLevel;
        logLevel= level;
        if ( !oldLevel.equals(level) ) {
            repaint();
        }
        firePropertyChange(PROP_LOG_LEVEL, oldLevel, level );
    }

    /**
     * @return level 
     * @see #PROP_LOG_LEVEL
     */
    public Level getLogLevel( ) {
        return logLevel;
    }

    private Level printingLogLevel = Level.ALL;
    
    /**
     * the log level to indicate the log level when printing.
     */
    public static final String PROP_PRINTINGLOGLEVEL = "printingLogLevel";

    /**
     * @see #PROP_PRINTINGLOGLEVEL
     * @return the level
     */
    public Level getPrintingLogLevel() {
        return printingLogLevel;
    }

    /**
     * @see #PROP_PRINTINGLOGLEVEL
     * @param printingLogLevel where Level.ALL is the default.
     */
    public void setPrintingLogLevel(Level printingLogLevel) {
        Level oldPrintingLogLevel = this.printingLogLevel;
        this.printingLogLevel = printingLogLevel;
        firePropertyChange(PROP_PRINTINGLOGLEVEL, oldPrintingLogLevel, printingLogLevel);
    }

    /**
     * the number of seconds to allow the log messages to show.
     */
    private int logTimeoutSec = Integer.MAX_VALUE;
    
    /**
     * number of seconds to show the log messages.  If Integer.MAX_VALUE/100 or 
     * greater then there is no timeout.
     */
    public static final String PROP_LOG_TIMEOUT_SEC = "logTimeoutSec";

    /**
     * @see #PROP_LOG_TIMEOUT_SEC
     * @return property value
     */
    public int getLogTimeoutSec() {
        return logTimeoutSec;
    }

    /**
     * @see #PROP_LOG_TIMEOUT_SEC
     * @param logTimeoutSec the property value
     */
    public void setLogTimeoutSec( int logTimeoutSec ) {
        int oldLogTimeoutSec = this.logTimeoutSec;
        this.logTimeoutSec = logTimeoutSec;
        repaint();
        firePropertyChange(PROP_LOG_TIMEOUT_SEC, oldLogTimeoutSec, logTimeoutSec);
    }

    /**
     * true if the x and y scaling (pixel:data) ratio is locked together.
     */
    public static final String PROP_ISOTROPIC= "isotropic";

    private boolean isotropic= false;

    /**
     * @see #PROP_ISOTROPIC
     * @return property value
     */
    public boolean isIsotropic() {
        return isotropic;
    }

    /**
     * set the property value
     * @see #PROP_ISOTROPIC
     * @param isotropic property value
     */
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
     * This returns a copy of the current bounds and may be modified by the 
     * client.  However, this does have the side-effect of recalculating the
     * internal bounds.
     * @return the cache image bounds.
     * @see #getCacheImageBounds which does not recalculate.
     */
    protected Rectangle getUpdateImageBounds() {
        int x = getColumn().getDMinimum();
        int y = getRow().getDMinimum();
        Rectangle lcacheImageBounds = new Rectangle();
        if ( overSize ) {
            lcacheImageBounds.width = 16 * getWidth() / 10;
            lcacheImageBounds.height = getHeight();
            lcacheImageBounds.x = x - 3 * getWidth() / 10;
        } else {
            lcacheImageBounds.width = getWidth();
            lcacheImageBounds.height = getHeight();
            lcacheImageBounds.x = x - 1;
        }
        lcacheImageBounds.y = y - 1;
        
        setCacheImageBounds( lcacheImageBounds );
        
        return lcacheImageBounds;
    }

    /**
     * returns a copy of the position of the cacheImage in the 
     * canvas frame of reference.
     * 
     * @return Rectangle
     */
    protected synchronized Rectangle getCacheImageBounds() {
        Rectangle lcacheImageBounds= new Rectangle(cacheImageBounds);
        return lcacheImageBounds;
    }

    /**
     * there's a deadlock that shows with test140 on ci-pw if this is synchronized.
     * @see https://ci-pw.physics.uiowa.edu/job/autoplot-test140
     * @param cacheImageBounds 
     */
    protected void setCacheImageBounds( Rectangle cacheImageBounds ) {
        this.cacheImageBounds= new Rectangle( cacheImageBounds );
    }
    
    /**
     * adjust the plot axes so it remains isotropic.
     * @param axis if non-null, the axis that changed, and the other should be adjusted.
     */
    private void checkIsotropic(DasPlot dasPlot, DasAxis axis) {
        Datum scalex = dasPlot.getXAxis().getDatumRange().width().divide(dasPlot.getXAxis().getDLength());
        Datum scaley = dasPlot.getYAxis().getDatumRange().width().divide(dasPlot.getYAxis().getDLength());

        if ( ! scalex.getUnits().isConvertibleTo(scaley.getUnits())
                || dasPlot.getXAxis().isLog()
                || dasPlot.getYAxis().isLog() ) {
            return;
        }
        
        if ( dasPlot.getXAxis().getDLength()==1 || dasPlot.getYAxis().getDLength()==1 ) {
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
                logger.log(Level.FINE, "expand={0} scale={1} otherScale={2}", new Object[]{expand, scale, otherScale});  //TODO: 2202
                System.err.println( String.format( "expand=%s scale=%s otherScale=%s axis=%s", new Object[]{expand, scale, otherScale, axis.getDasName() }) ); //TODO: 2202
                DatumRange newOtherRange = DatumRangeUtil.rescale(otherRange, 0 - expand, 1 + expand);
                otherAxis.setDatumRange(newOtherRange);
            }
        }
    }
    
    /**
     * get the diagnostic for the number of times the component was asked to paint
     * itself since the last reset.
     * @return the number of times the component has painted itself.
     */
    public int getPaintCount() {
        return paintComponentCount.intValue();
    }
    
    /**
     * reset the paint counter.
     */
    public void resetPaintCount() {
        Renderer[] lrenderers= getRenderers();
        this.paintComponentCount.set(0);
        for ( Renderer r: lrenderers ) {
            r.resetCounters();
        }
    }
    
    /**
     * draw a purple box in the lower right corner indicating the number
     * of times each renderer has updated, rendered, and the plot itself
     * has painted.
     */
    public String PROP_DRAWDEBUGMESSAGES= "debugMessages";
    
    private boolean drawDebugMessages= false;
    
    /**
     * @see #PROP_DRAWDEBUGMESSAGES
     * @param v true if the messages should be shown. 
     */
    public void setDrawDebugMessages( boolean v ) {
        this.drawDebugMessages= v;
        resetPaintCount();
        repaint();
    }
    
    /**
     * @see #PROP_DRAWDEBUGMESSAGES
     * @return true if the messages should be shown. 
     */
    public boolean isDrawDebugMessages() {
        return this.drawDebugMessages;
    }
    
}
