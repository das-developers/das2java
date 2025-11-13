/* File: DasAxis.java
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
import org.das2.event.TimeRangeSelectionEvent;
import org.das2.event.TimeRangeSelectionListener;
import org.das2.event.HorizontalRangeSelectorMouseModule;
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.DataRangeSelectionListener;
import org.das2.event.VerticalRangeSelectorMouseModule;
import org.das2.event.ZoomPanMouseModule;
import org.das2.datum.format.DefaultDatumFormatterFactory;
import org.das2.datum.DatumRange;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.Units;
import org.das2.datum.DatumVector;
import org.das2.datum.Datum;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.TimeLocationUnits;
import org.das2.DasProperties;
import org.das2.util.GrannyTextRenderer;
import org.das2.util.DasMath;
import org.das2.DasApplication;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import javax.swing.border.*;

import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.*;
import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import org.das2.system.DasLogger;
import java.util.logging.Logger;
import org.das2.DasException;
import org.das2.components.DasProgressWheel;
import org.das2.datum.DatumUtil;
import org.das2.datum.DomainDivider;
import org.das2.datum.DomainDividerUtil;
import org.das2.datum.OrbitDatumRange;
import org.das2.datum.TimeParser;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.das2.datum.format.TimeDatumFormatterFactory;
import org.das2.math.fft.jnt.Factorize;
import org.das2.system.RequestProcessor;
import org.das2.util.LoggerManager;
import org.das2.util.TickleTimer;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.JoinDataSet;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.QFunction;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;

/** 
 * One dimensional axis component that transforms data to device space and back, 
 * and provides controls for navigating the 1-D data space.
 * 
 * TODO: Manual for using the DasAxis.
 * 
 * The DasAxis will automatically pick tick locations and formatting for the
 * ticks.  This is done by looking at the units of the range and selecting
 * an appropriate algorithm for locating and labelling ticks.
 * 
 * TCAs are additional lines added to ticks.  In Space Physics, this is often
 * ephemeris data associated with each time.
 * 
 * The controls for the DasAxis have grown over time.  This outlines:
 * format - manual control of formatting and tick locations including %f and "$Y-$j $H:$M"
 * tickValues - manual control of tick locations, including +10/5 for repeating
 * flipped - min will be at top or right, max will be at bottom or left.
 * lineThickness - 3px will draw a 3-pixel wide line. "" is 1-pixel.
 * 
 * @author eew
 */

public class DasAxis extends DasCanvasComponent implements DataRangeSelectionListener, TimeRangeSelectionListener, Cloneable {

    public static final String PROP_LABEL = "label";
    public static final String PROP_LOG = "log";
    public static final String PROP_OPPOSITE_AXIS_VISIBLE = "oppositeAxisVisible";
    public static final String PROP_BOUNDS = "bounds";
    public static final String PROP_SCAN_RANGE="scanRange";
    public static final String PROP_UNITS = "units";
    public static final String PROPERTY_TICKS = "ticks";
    public static final int MAX_TCA_LINES = 32; // maximum number of TCA lines

    private static final int DEVICE_POSITIVE_LIMIT = 10000;
    /*
     * PUBLIC CONSTANT DECLARATIONS
     */
    /** This value indicates that the axis should be located at the top of its cell */
    public static final int TOP = 1;
    /** This value indicates that the axis should be located at the bottom of its cell */
    public static final int BOTTOM = 2;
    /** This value indicates that the axis should be located to the left of its cell */
    public static final int LEFT = 3;
    /** This value indicates that the axis should be located to the right of its cell */
    public static final int RIGHT = 4;
    /** This value indicates that the axis should be oriented horizontally */
    public static final int HORIZONTAL = BOTTOM;
    /** This value indicates that the axis should be oriented vertically */
    public static final int VERTICAL = LEFT;
    /** This indicate the axis ticks should go up */
    private static final int UP = 995;
    /** This indicates the axis ticks should go down */
    private static final int DOWN = 996;

    /* Constants defining the action commands and labels for the scan/step buttons. */
    private static final String STEP_PREVIOUS_LABEL = "<< step";
    private static final String STEP_NEXT_LABEL = "step >>";
    
    /* GENERAL AXIS INSTANCE MEMBERS */
    protected DataRange dataRange;
    
    // the height of the canvas when we last updated
    private int parentHeight;
    
    // the width of the canvas when we last updated
    private int parentWidth;

    /**
     * get the userDatumFormatter, which converts Datums into Strings.  This
     * can be null if none should be used.
     * @return the userDatumFormatter.
     */
    public DatumFormatter getUserDatumFormatter() {
        return userDatumFormatter;
    }

    /**
     * set the userDatumFormatter, which converts Datums into Strings.  This
     * can be null if none should be used.
     * @param userDatumFormatter  the userDatumFormatter.
     */
    public void setUserDatumFormatter(DatumFormatter userDatumFormatter) {
        logger.log(Level.FINE, "setUserDatumFormatter({0})", userDatumFormatter);
        DatumFormatter old= this.userDatumFormatter;
        this.userDatumFormatter = userDatumFormatter;
        if ( old!=userDatumFormatter ) {
            updateTickV();
        }//TODO: this results in data read on event thread
        SwingUtilities.invokeLater( new Runnable() {
            @Override
            public void run() {
                resize();
                repaint();
            }
        });
    }

    /**
     * if non-null, try to use this formatter.
     */
    private DatumFormatter userDatumFormatter = null;

    /** 
     * set the action for the next button 
     * @param label the label (step or scan)
     * @param abstractAction the action to invoke.
     */
    public void setNextAction( String label, AbstractAction abstractAction) {
        if ( !DasApplication.getDefaultApplication().isHeadless() ) {
            ActionListener[] als= this.stepNext.getActionListeners();
            for ( ActionListener al : als ) {
                this.stepNext.removeActionListener(al);
            }
            this.stepNext.setAction(abstractAction);
            this.stepNext.setText(""+ label +" >>" );
        }
    }

    /** 
     * set the action for the prev button 
     * @param label the label (step or scan)
     * @param abstractAction the action to invoke.
     */
    public void setPreviousAction( String label,AbstractAction abstractAction) {
        if ( !DasApplication.getDefaultApplication().isHeadless() ) {
            ActionListener[] als= this.stepPrevious.getActionListeners();
            for ( ActionListener al : als ) {
                this.stepPrevious.removeActionListener(al);
            }
            this.stepPrevious.setAction(abstractAction);
            this.stepPrevious.setText("<< "+label );
        }
    }

    /**
     * until we switch to java 1.5, use this lock object instead of
     * java.util.concurrent.lock
     */
    public interface Lock {

        public void lock();

        public void unlock();
    }
    
    /**
     * synchronized block for calculating ticks uses this.
     */
    private final Object tickLock= new Object();
    
    /* Affine Transform, dependent on min, max and axis position
     * pixel= at_m * data + at_b
     * where data is data point in linear space (i.e. log property implemented)
     */
    double at_m;
    double at_b;
    private int orientation;
    private int tickDirection = 1;  // 1=down or left, -1=up or right
    protected String axisLabel = "";
    protected TickVDescriptor tickV;
    private boolean autoTickV = true;
    private boolean ticksVisible = true;
    private boolean tickLabelsVisible = true;
    private boolean oppositeAxisVisible = false;
    protected DatumFormatter datumFormatter = DefaultDatumFormatterFactory.getInstance().defaultFormatter();
    private MouseModule zoom = null;
    private PropertyChangeListener dataRangePropertyListener;
    protected JPanel primaryInputPanel;
    protected JPanel secondaryInputPanel;
    private ScanButton stepPrevious;
    private ScanButton stepNext;

    /**
     * limits of the scan range.  Scan buttons will only be shown with within this range.  If not set, then there is no limit to range
     * and the buttons are always available.
     */
    private DatumRange scanRange;
    
    private boolean animated = ("on".equals(DasProperties.getInstance().get("visualCues")));
    
    private Rectangle blLineRect;
    private Rectangle trLineRect;
    /**
     * bottom or left rectangle bounds for the ticks  (green in DEBUG_GRAPHICS)
     */
    private Rectangle blTickRect;  
    /**
     * top or right rectangle bounds for the ticks    (green in DEBUG_GRAPHICS)
     */
    private Rectangle trTickRect;  
    
    /**
     * bottom or left rectangle bounds for the tick labels  (blue in DEBUG_GRAPHICS)
     */
    private Rectangle blLabelRect; 
    /**
     * top or right rectangle bounds for the tick labels    (blue in DEBUG_GRAPHICS)
     */
    private Rectangle trLabelRect;
    /**
     * bottom or left rectangle bounds for the axis label   (grey in DEBUG_GRAPHICS)
     */
    private Rectangle blTitleRect;
    /**
     * top or right rectangle bounds for the axis label     (grey in DEBUG_GRAPHICS)
     */
    private Rectangle trTitleRect; 
    /**
     * this is deprecated, use labelOffset instead.
     */
    private Integer leftXOverride = null;  
    /**
     * empty string means default, or "5em" or "50px" etc.
     */
    private String labelOffset= ""; 
    
    private boolean flipped;
    /* TIME LOCATION UNITS RELATED INSTANCE MEMBERS */
    private javax.swing.event.EventListenerList timeRangeListenerList = null;
    private TimeRangeSelectionEvent lastProcessedEvent = null;
    /* TCA RELATED INSTANCE MEMBERS */
    private QFunction tcaFunction;
    private QDataSet tcaData = null;
    private final Object tcaDataLock= new Object();
    
    private String dataset = "";
    private boolean drawTca;
    
    private TickleTimer tcaTimer;
    
    public static final String PROPERTY_DATUMRANGE = "datumRange";
    /* DEBUGGING INSTANCE MEMBERS */
    
    private static final boolean DEBUG_GRAPHICS = System.getProperty("das2.graph.dasaxis.debuggraphics","false").equals("true");
    
    private static final Color[] DEBUG_COLORS= new Color[]{ org.das2.util.ColorUtil.decodeColor("Purple"),
                        org.das2.util.ColorUtil.decodeColor("Purple").brighter().brighter() };

    int tickLen= 0; // this is reset after sizing.

    /**
     * the tick length specification, in em or pts. 
     */
    String tickLenStr= "0.66em";

    final int TICK_LABEL_GAP_MIN= 4;  // minimum number of pixels to label

    private int debugColorIndex = 0;
    private DasPlot dasPlot;
    private JMenu bookmarksMenu;
    private JMenu backMenu;
    private static final Logger logger = LoggerManager.getLogger("das2.graphics.axis");

    /** 
     * Create an axis object, relating data and canvas pixel coordinates.
     * @param min the minimum value
     * @param max the maximum value
     * @param orientation DasAxis.TOP, DasAxis.BOTTOM, DasAxis.LEFT, DasAxis.RIGHT.
     */
    public DasAxis(Datum min, Datum max, int orientation) {
        this(min, max, orientation, false);
    }

    /** 
     * Create an axis object, relating data and canvas pixel coordinates.
     * @param range the initial range for the axis.
     * @param orientation the position relative to a plot, one of DasAxis.TOP, DasAxis.BOTTOM, DasAxis.LEFT, DasAxis.RIGHT.
     */
    public DasAxis(DatumRange range, int orientation) {
        this(range.min(), range.max(), orientation);
    }

    /** 
     * Create an axis object, relating data and canvas pixel coordinates.
     * @param min the minimum value
     * @param max the maximum value
     * @param orientation the position relative to a plot, one of DasAxis.TOP, DasAxis.BOTTOM, DasAxis.LEFT, DasAxis.RIGHT.
     * @param log if true then the axis is a log axis.
     */
    public DasAxis(Datum min, Datum max, int orientation, boolean log) {
        this(orientation);
        dataRange = new DataRange(this, min, max, log);
        addListenersToDataRange(dataRange, dataRangePropertyListener);
        copyFavorites();
        copyHistory();
    }

    /** 
     * Create an axis object, relating data and canvas pixel coordinates.
     * @param range the range object allowing connections between axes.  A DataRange is a mutable object.
     * @param orientation the position relative to a plot, one of DasAxis.TOP, DasAxis.BOTTOM, DasAxis.LEFT, DasAxis.RIGHT
     */
    protected DasAxis(DataRange range, int orientation) {
        this(orientation);
        dataRange = range;
        addListenersToDataRange(range, dataRangePropertyListener);
        copyFavorites();
        copyHistory();
    }

    private DasAxis(int orientation) {
        super();
        setOpaque(false);
        setOrientationInternal(orientation);
        installMouseModules();
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            backMenu = new JMenu("Back");
            mouseAdapter.addMenuItem(backMenu);
            bookmarksMenu = new JMenu("Bookmarks");
            mouseAdapter.addMenuItem(bookmarksMenu);
        }
        dataRangePropertyListener = createDataRangePropertyListener();
        setLayout(new AxisLayoutManager());
        maybeInitializeInputPanels();
        maybeInitializeScanButtons();
        if ( !DasApplication.getDefaultApplication().isHeadless() ) {
            stepNext.setEnabled( true );
            stepPrevious.setEnabled( true );
        }
        add(primaryInputPanel);
        add(secondaryInputPanel);
        try {
            this.updateTickLength();
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        // this doesn't fire
        this.addPropertyChangeListener( "font", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    updateTickLength();
                } catch (ParseException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
        });

        TickMaster.getInstance().register(this); // weak map will unregister

    }

    private void addListenersToDataRange(DataRange range, PropertyChangeListener listener) {
        range.addPropertyChangeListener(PROP_LOG, listener);
        range.addPropertyChangeListener("minimum", listener);
        range.addPropertyChangeListener("maximum", listener);
        range.addPropertyChangeListener(DataRange.PROPERTY_DATUMRANGE, listener);
        range.addPropertyChangeListener("history", listener);
        range.addPropertyChangeListener("favorites", listener);
    }

    /**
     * add the range to the favorites list, which is accessible from the popup-menu.
     * @param range the range to add.
     */
    public void addToFavorites(final DatumRange range) {
        dataRange.addToFavorites(range);
        copyFavorites();
    }

    /**
     * remove the range from the favorites list, which is accessible from the popup-menu.
     * @param range the range to add.
     */
    public void removeFromFavorites(final DatumRange range) {
        dataRange.removeFromFavorites(range);
        copyFavorites();
    }

    private void copyFavorites() {
        if (DasApplication.getDefaultApplication().isHeadless()) {
            return;
        }
        bookmarksMenu.removeAll();
        List favorites = dataRange.getFavorites();
        for (Iterator i = favorites.iterator(); i.hasNext();) {
            final DatumRange r = (DatumRange) i.next(); // copied code from addToFavorites
            Action action = new AbstractAction(r.toString()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LoggerManager.logGuiEvent(e);
                    DasAxis.this.setDatumRange(r);
                }
            };
            JMenuItem menuItem = new JMenuItem(action);
            bookmarksMenu.add(menuItem);
        }

        bookmarksMenu.add( new JSeparator() );
        Action action = new AbstractAction("bookmark this range") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoggerManager.logGuiEvent(e);                
                DasAxis.this.addToFavorites(DasAxis.this.getDatumRange());
            }
        };
        JMenuItem addItem = new JMenuItem(action);
        bookmarksMenu.add(addItem);

        bookmarksMenu.add( new JSeparator() );
        Action action2 = new AbstractAction("remove bookmark for range") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoggerManager.logGuiEvent(e);
                DasAxis.this.removeFromFavorites(DasAxis.this.getDatumRange());
            }
        };
        JMenuItem rmItem = new JMenuItem(action2);
        bookmarksMenu.add(rmItem);
    }

    private void copyHistory() {
        if (DasApplication.getDefaultApplication().isHeadless()) {
            return;
        }
        if ( enableHistory==false ) {
            return;
        }
        backMenu.removeAll();
        List history = dataRange.getHistory();
        int ii = 0;
        for (Iterator i = history.iterator(); i.hasNext();) {
            final int ipop = ii;
            final DatumRange r = (DatumRange) i.next(); // copied code from addToFavorites
            Action action = new AbstractAction(r.toString()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LoggerManager.logGuiEvent(e);
                    dataRange.popHistory(ipop);
                    DasAxis.this.setDataRangePrev();
                }
            };
            JMenuItem menuItem = new JMenuItem(action);
            backMenu.add(menuItem);
            ii++;
        }
    }

    /**
     * true if the axis built-in history is enabled.
     */
    protected boolean enableHistory = true;
    
    /**
     * true if the axis built-in history is enabled.
     */
    public static final String PROP_ENABLEHISTORY = "enableHistory";

    /**
     * true if the axis built-in history is enabled.
     * @return true if the axis built-in history is enabled.
     */
    public boolean isEnableHistory() {
        return enableHistory;
    }

    /**
     * true if the axis built-in history is enabled.
     * @param enableHistory true if the axis built-in history is enabled.
     */
    public void setEnableHistory(boolean enableHistory) {
        boolean oldEnableHistory = this.enableHistory;
        this.enableHistory = enableHistory;
        if ( ! DasApplication.getDefaultApplication().isHeadless() ) {
            if ( !enableHistory ) {
                getDasMouseInputAdapter().removeMenuItem(backMenu.getText());
            } else {
                getDasMouseInputAdapter().addMenuItem(backMenu);
            }
        }
        firePropertyChange(PROP_ENABLEHISTORY, oldEnableHistory, enableHistory);
    }

    /* PRIVATE INITIALIZATION FUNCTIONS */
    private void maybeInitializeInputPanels() {
        if (primaryInputPanel == null) {
            primaryInputPanel = new JPanel();
            primaryInputPanel.setOpaque(false);
        }
        if (secondaryInputPanel == null) {
            secondaryInputPanel = new JPanel();
            secondaryInputPanel.setOpaque(false);
        }
    }

    private void maybeInitializeScanButtons() {
        if (!DasApplication.getDefaultApplication().isHeadless()) {
            stepPrevious = new DasAxis.ScanButton(STEP_PREVIOUS_LABEL);
            stepNext = new DasAxis.ScanButton(STEP_NEXT_LABEL);
            ActionListener al = createScanActionListener();
            stepPrevious.addActionListener(al);
            stepNext.addActionListener(al);
            add(stepPrevious);
            add(stepNext);
        }
    }

    private ActionListener createScanActionListener() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoggerManager.logGuiEvent(e);                
                String command = e.getActionCommand();
                DasLogger.getLogger(DasLogger.GUI_LOG).log(Level.FINE, "event {0}", command);
                if (command.equals(STEP_PREVIOUS_LABEL)) {
                    if ( scanRange==null || scanRange.intersects(getDatumRange().previous()) ) scanPrevious();
                } else if (command.equals(STEP_NEXT_LABEL)) {
                    if ( scanRange==null || scanRange.intersects(getDatumRange().next()) ) scanNext();
                }
            }
        };
    }

    private PropertyChangeListener createDataRangePropertyListener() {
        return new PropertyChangeListener() {
            @Override            
            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                Object oldValue = e.getOldValue();
                Object newValue = e.getNewValue();
                if (propertyName.equals(PROP_LOG)) {
                    update();
                    firePropertyChange(PROP_LOG, oldValue, newValue);
                    markDirty("transform:"+propertyName+"="+newValue);
                } else if (propertyName.equals("minimum")) {
                    update();
                    firePropertyChange("dataMinimum", oldValue, newValue);
                    markDirty("transform:"+propertyName+"=" + (DasAxis.this.getUnits().createDatum((Number)newValue)));
                } else if (propertyName.equals("maximum")) {
                    update();
                    firePropertyChange("dataMaximum", oldValue, newValue);
                    markDirty("transform:"+propertyName+"=" + (DasAxis.this.getUnits().createDatum((Number)newValue)) );
                } else if (propertyName.equals("favorites")) {
                    copyFavorites();
                } else if (propertyName.equals(DataRange.PROPERTY_DATUMRANGE)) {
                    //if ( UnitsUtil.isTimeLocation( DasAxis.this.getUnits() ) ) {  // Autoplot test018_003.                      
                    //    DatumRange dr= (DatumRange)newValue;
                    //    System.err.println("new range="+dr);                        
                    //    if ( dr.equals( DatumRangeUtil.parseTimeRangeValid("2000-01-03 09:36 to 2000-01-07 00:00")) ) {
                    //       System.err.println("here min");
                    //    }
                    //}
                    update();
                    firePropertyChange(PROPERTY_DATUMRANGE, oldValue, newValue);
                    markDirty("transform:"+propertyName+"="+newValue);
                } else if (propertyName.equals("history")) {
                    if (!dataRange.valueIsAdjusting()) {
                        copyHistory();
                    }
                }
            }
        };
    }

    private void installMouseModules() {
        if (zoom instanceof HorizontalRangeSelectorMouseModule) {
            ((HorizontalRangeSelectorMouseModule) zoom).removeDataRangeSelectionListener(this);
            mouseAdapter.removeMouseModule(zoom);
        } else if (zoom instanceof VerticalRangeSelectorMouseModule) {
            ((VerticalRangeSelectorMouseModule) zoom).removeDataRangeSelectionListener(this);
            mouseAdapter.removeMouseModule(zoom);
        }
        if (isHorizontal()) {
            zoom = new HorizontalRangeSelectorMouseModule(this, this);
            ((HorizontalRangeSelectorMouseModule) zoom).addDataRangeSelectionListener(this);
            mouseAdapter.addMouseModule(zoom);
            mouseAdapter.setPrimaryModule(zoom);

            MouseModule zoomPan = new ZoomPanMouseModule(this, this, null);
            mouseAdapter.addMouseModule(zoomPan);
            mouseAdapter.setSecondaryModule(zoomPan);
        } else {
            zoom = new VerticalRangeSelectorMouseModule(this, this);
            ((VerticalRangeSelectorMouseModule) zoom).addDataRangeSelectionListener(this);
            mouseAdapter.addMouseModule(zoom);
            mouseAdapter.setPrimaryModule(zoom);

            MouseModule zoomPan = new ZoomPanMouseModule(this, null, this);
            mouseAdapter.addMouseModule(zoomPan);
            mouseAdapter.setSecondaryModule(zoomPan);
        }
    }

    /** 
     * Set the axis orientation.  One of: DasAxis.TOP, DasAxis.BOTTOM,
     *  DasAxis.LEFT, DasAxis.RIGHT
     * @param orientation the orientation, one of TOP,BOTTOM,LEFT,RIGHT.
     * @see #TOP
     * @see #BOTTOM
     * @see #LEFT
     * @see #RIGHT
     */
    public void setOrientation(int orientation) {
        boolean oldIsHorizontal = isHorizontal();
        setOrientationInternal(orientation);
        if (oldIsHorizontal != isHorizontal()) {
            installMouseModules();
        }
    }

    /**
     * This is a private internal implementation for
     * {@link #setOrientation(int)}.  This method is provided
     * to avoid calling a non-final non-private instance method
     * from a constructor.  Doing so can create problems if the
     * method is overridden in a subclass.
     *
     * @param orientation TOP,BOTTOM,LEFT,RIGHT
     */
    private void setOrientationInternal(int orientation) {
        this.orientation = orientation;
        switch (orientation) {
            case TOP:
                setTickDirection(UP);
                break;
            case BOTTOM:
                setTickDirection(DOWN);
                break;
            case LEFT:
                setTickDirection(RIGHT);
                break;
            case RIGHT:
                setTickDirection(LEFT);
                break;
            default:
                throw new IllegalArgumentException("Invalid value for orientation");
        }
    }

    /**
     * Set the range for the axis.  Note this is allowed to change the units as well.
     * @param dr the new range.  The range must be interval or ratio measurements.
     */
    public void setDatumRange(DatumRange dr) {
        //System.err.println("setDatumRange("+dr+")");
        if ( dr==null ) {
            throw new NullPointerException("range cannot be null");
        }
        if ( !UnitsUtil.isIntervalOrRatioMeasurement(dr.getUnits()) ) {
            throw new IllegalArgumentException("units cannot be ordinal or nominal");
        }
        if ( dr.width().value()==0 ) {
            throw new IllegalArgumentException("width is zero: "+dr);
        }
        DatumRange oldRange= dataRange.getDatumRange();
        Units oldUnits = getUnits();
        if ( !rangeIsAcceptable(dr) ) { 
            logger.log(Level.WARNING, "invalid range ignored: {0}", dr);
            return;
        }
        if (getUnits().isConvertibleTo(dr.getUnits())) {
            //this.setDataRange(dr.min(), dr.max());
            this.dataRange.setRange(dr);
        } else {
            this.resetRange(dr);
        }
        if ( oldUnits!=dr.getUnits() ) {
            firePropertyChange(PROP_UNITS, oldUnits, dr.getUnits());
        }
        firePropertyChange( PROPERTY_DATUMRANGE, oldRange, dr );
    }

    /**
     * return the current range
     * @return the current range
     */
    public DatumRange getDatumRange() {
        return dataRange.getDatumRange();
    }

    /*
     * Return true if the range is acceptable, having some non-zero length,
     * false otherwise.  Note this method is overriden by DasLabelAxis.
     * @return true if the range is acceptible.
     */
    protected boolean rangeIsAcceptable(DatumRange dr) {
        return dr.min().lt(dr.max());
    }

    /** 
     * Set the data range.  minimum must be less than maximum, but for nominal
     * data they can be equal.
     * @param minimum the minimum value
     * @param maximum the maximum value
     */
    public void setDataRange(Datum minimum, Datum maximum) {

        Units units = dataRange.getUnits();
        if (minimum.getUnits() != units) {
            minimum = minimum.convertTo(units);
            maximum = maximum.convertTo(units);
        }

        DatumRange newRange = new DatumRange(minimum, maximum);
        logger.log(Level.FINE, "enter dasAxis.setDataRange( {0} )", newRange);

        if (!rangeIsAcceptable(newRange)) {
            logger.log(Level.WARNING, "invalid range ignored: {0}", newRange);
            return;
        }

        double min, max, min0, max0;

        min0 = dataRange.getMinimum();
        max0 = dataRange.getMaximum();

        if (dataRange.isLog()) {
            min = Math.log10(minimum.doubleValue(getUnits()));
            max = Math.log10(maximum.doubleValue(getUnits()));
            if ( minimum.doubleValue(getUnits())==0 ) {  // avoid log zero
                min= max/1000;
            }
        } else {
            min = minimum.doubleValue(getUnits());
            max = maximum.doubleValue(getUnits());
        }

        if (!valueIsAdjusting()) {
            animateChange(min0, max0, min, max);
        }
        DatumRange oldRange = dataRange.getDatumRange();
        dataRange.setRange(newRange);

        refreshScanButtons(false);
        
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange(PROPERTY_DATUMRANGE, oldRange, newRange);
    }

    /**
     * clear the internal history.
     */
    public void clearHistory() {
        dataRange.clearHistory();
    }

    private void createAndFireRangeSelectionEvent() {
        if (getUnits() instanceof TimeLocationUnits) {
            logger.fine("firing rangeSelectionEvent");
            TimeRangeSelectionEvent e = new TimeRangeSelectionEvent(this, new DatumRange(this.getDataMinimum(), this.getDataMaximum()));
            fireTimeRangeSelectionListenerTimeRangeSelected(e);
        }
    }

    /** 
     * set the range to the previous interval.
     */
    public void setDataRangePrev() {
        logger.fine("enter dasAxis.setDataRangePrev()");
        DatumRange oldRange = dataRange.getDatumRange();
        double min0 = dataRange.getMinimum();
        double max0 = dataRange.getMaximum();
        dataRange.setRangePrev();
        DatumRange newRange = dataRange.getDatumRange();
        double min1 = dataRange.getMinimum();
        double max1 = dataRange.getMaximum();
        animateChange(min0, max0, min1, max1);
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange(PROPERTY_DATUMRANGE, oldRange, newRange);
    }

    /**
     * set the range to the next interval.  
     */
    public void setDataRangeForward() {
        logger.fine("enter dasAxis.setDataRangeForward()");
        double min0 = dataRange.getMinimum();
        double max0 = dataRange.getMaximum();
        DatumRange oldRange = dataRange.getDatumRange();
        dataRange.setRangeForward();
        DatumRange newRange = dataRange.getDatumRange();
        double min1 = dataRange.getMinimum();
        double max1 = dataRange.getMaximum();
        animateChange(min0, max0, min1, max1);
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange(PROPERTY_DATUMRANGE, oldRange, newRange);
    }

    /** 
     * set the range to min-width to max+width.
     */
    public void setDataRangeZoomOut() {
        logger.fine("enter dasAxis.setDataRangeZoomOut()");
        double t1 = dataRange.getMinimum();
        double t2 = dataRange.getMaximum();
        double width = t2 - t1;
        double min = t1 - width;
        double max = t2 + width;
        animateChange(t1, t2, min, max);
        DatumRange oldRange = dataRange.getDatumRange();
        if ( !DatumRangeUtil.isAcceptable( DatumRange.newDatumRange( min, max, getUnits() ), isLog() ) ) {
            logger.info("zoom out limit");
            return;
        }
        dataRange.setRange(min, max);
        DatumRange newRange = dataRange.getDatumRange();
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange(PROPERTY_DATUMRANGE, oldRange, newRange);
    }

    /** 
     * return the mutable DataRange object.  This is used 
     * to bind axes together by sharing an internal model.
     * @return the DataRange.
     */
    public DataRange getDataRange() {
        return this.dataRange;
    }

    /**
     * @deprecated this is not used.
     */
    protected void deviceRangeChanged() {
    }

    /** 
     * return the minimum value
     * @return the minimum value
     */
    public Datum getDataMinimum() {
        return dataRange.getDatumRange().min();
    }

    /**
     * return the maximum value
     * @return the maximum value
     */
    public Datum getDataMaximum() {
        return dataRange.getDatumRange().max();
    }

    /**
     * This is the preferred method for getting the range of the axis.
     * @return a DatumRange indicating the range of the axis.
     * @deprecated use getDatumRange instead.
     */
    public DatumRange getRange() {
        return dataRange.getDatumRange();
    }

    /** 
     * get the bigger extent of the range of this axis, in the given units.
     * @param units the units which the result should be returned in.
     * @return the value in the units.
     */
    public double getDataMaximum(Units units) {
        return getDataMaximum().doubleValue(units);
    }

    /** 
     * get the smaller extent of the range of this axis, in the given units.
     * @param units the units which the result should be returned in.
     * @return the value in the units.
     */
    public double getDataMinimum(Units units) {
        return getDataMinimum().doubleValue(units);
    }

    /** 
     * set the bigger extent of the range of this axis.
     * @param max the new value
     */
    public void setDataMaximum(Datum max) {
        dataRange.setMaximum(max);
        update();
    }

    /** 
     * set the smaller extent of the range of this axis.
     * @param min the new value
     */
    public void setDataMinimum(Datum min) {
        dataRange.setMinimum(min);
        update();
    }

    /** 
     * return true if the axis is log.
     * @return true if the axis is log.
     */
    public boolean isLog() {
        return dataRange.isLog();
    }

    /** 
     * Set the axis to be log or linear.  If necessary, axis range will be 
     * adjusted to make the range valid.
     * @param log true if the axis should be log.
     */
    public void setLog(boolean log) {
        boolean oldLog = isLog();
        DatumRange range = getDatumRange();
        dataRange.setLog(log);
        update();
        if (log != oldLog) {
            firePropertyChange(PROP_LOG, oldLog, log);
        }
        // switching log can change the axis range.
        if (!range.equals(getDatumRange())) {
            firePropertyChange(PROPERTY_DATUMRANGE, range, getDatumRange());
        }
    }
    
    
    private String reference = "";

    public static final String PROP_REFERENCE = "reference";

    public String getReference() {
        return reference;
    }

    /**
     * draw an optional reference line at the location.  Valid entries
     * can be parsed into a Datum, using the units of the axis.
     * @param reference 
     */
    public void setReference(String reference) {
        String oldReference = this.reference;
        this.reference = reference.trim();
        update();
        firePropertyChange(PROP_REFERENCE, oldReference, reference);
    }
    
    /** 
     * return the units of the axis.
     * @return the units of the axis.
     */
    public Units getUnits() {
        return dataRange.getUnits();
    }

    /**
     * set the units of the axis, which must be convertible from the old units.
     * @param newUnits the units of the axis.
     * @see #resetRange(org.das2.datum.DatumRange) 
     */
    public void setUnits(Units newUnits) {
        dataRange.setUnits(newUnits);
    }
    
    /**
     * limit the scan buttons to operate within this range.
     * Setting this to null will remove the limit.
     * http://sourceforge.net/p/autoplot/bugs/473/
     * @param range null or the range to limit the scanning
     */
    public void setScanRange( DatumRange range ) {
        DatumRange old= this.scanRange;
        this.scanRange= range;
        if ( stepNext!=null ) { // headless will have null scanNext
            SwingUtilities.invokeLater( new Runnable() {
                @Override
                public void run() {
                    DatumRange range= getScanRange();
                    if ( range==null ) {
                        stepNext.setToolTipText(null);
                        stepPrevious.setToolTipText(null);
                    } else {
                        stepNext.setToolTipText("<html><em><sub>scan limited to<br>"+range.toString());
                        stepPrevious.setToolTipText("<html><em><sub>scan limited to<br>"+range.toString());
                    }
                }
            });
        }
        firePropertyChange( PROP_SCAN_RANGE, old, range );
    }

    /**
     * get the limit the scan buttons, which may be null.
     * @return the limit the scan buttons, which may be null.
     */
    public DatumRange getScanRange(  ) {
        return this.scanRange;
    }

    /**
     * set the axis to the new range, allowing the units to change.
     * @param range the new range
     */
    public synchronized void resetRange(DatumRange range) {
        DatumRange oldRange= this.getDatumRange();
        if (range.getUnits() != this.getUnits()) {
            if (dasPlot != null) {
                dasPlot.invalidateCacheImage();
            }
            logger.log(Level.FINEST, "replaceRange({0})", range);
            dataRange.resetRange(range);
            setScanRange(null);
        } else {
            dataRange.setRange(range);
        }
        updateTickV();
        markDirty("range");
        firePropertyChange(PROPERTY_DATUMRANGE, null, range);
        update();
    }

    /** 
     * if true, then the axis and its ticks on the opposite side will also be drawn.
     * @param visible if true, then the axis and its ticks on the opposite side will also be drawn.
     */
    public void setOppositeAxisVisible(boolean visible) {
        if (visible == oppositeAxisVisible) {
            return;
        }
        boolean oldValue = oppositeAxisVisible;
        oppositeAxisVisible = visible;
        revalidate();
        repaint();
        firePropertyChange(PROP_OPPOSITE_AXIS_VISIBLE, oldValue, visible);
    }

    /** 
     * return true if the opposite side is also drawn. 
     * @return true if the opposite side is also drawn. 
     */
    public boolean isOppositeAxisVisible() {
        return oppositeAxisVisible;
    }

    /** 
     * set the label for the axis.
     * 
     * The label for this axis is displayed below the ticks for horizontal axes
     * or to left of the ticks for vertical axes.
     * @param t The new label for this axis
     */
    public void setLabel(String t) {
        logger.log(Level.FINE, "setLabel(\"{0}\")", t);
        if (t == null) {
            throw new NullPointerException("axis label cannot be null");
        }
        Object oldValue = axisLabel;
        axisLabel = t;
        update();
        firePropertyChange(PROP_LABEL, oldValue, t);
    }

    /**
     * get the label for the axis.
     * @return A String instance that contains the title displayed
     *    for this axis, or "" if the axis has no title.
     */
    public String getLabel() {
        return axisLabel;
    }

    /** 
     * true if the axis is animated.  Transitions in axis position are drawn 
     * rapidly to animate the transition.
     * @return true if the axis is animated.
     */
    public boolean isAnimated() {
        return this.animated;
    }

    /** 
     * if true then the axis will be animated.
     * @param animated if true then the axis will be animated.
     */
    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    /**
     * if true then additional ephemeris (TCA) ticks are drawn.
     * @return if true then additional ticks are drawn.
     * @deprecated use isDrawTca()
     */
    public boolean getDrawTca() {
        return isDrawTca();
    }

    /**
     * true if additional tick labels are drawn using the TCA function.
     * @return true if additional ticks will be drawn.
     * @see #setTcaFunction(org.das2.qds.QFunction) 
     */    
    public boolean isDrawTca() {
        return drawTca;
    }
    
    /**
     * true if the TCA data has been loaded and the axis bounds reflect TCA data present.
     * @return true if the TCA data has been loaded and the axis bounds reflect TCA data present.
     */
    public boolean isTcaLoaded() {
        return tcaData!=null;
    }
    
    /**
     * if true then turn on additional tick labels using the TCA function.
     * @param b if true then additional ticks will be drawn.
     * @see #setTcaFunction(org.das2.qds.QFunction) 
     */
    public void setDrawTca(boolean b) {
        boolean oldValue = drawTca;
        if (b && getOrientation() != BOTTOM) {
            throw new IllegalArgumentException("Vertical time axes cannot have annotations");
        }
        if (drawTca == b) {
            return;
        }
        drawTca = b;
        if ( !b ) {
            this.tcaNeedsPainting= false;
            this.tcaIsLoading= false;
        }
        
        markDirty("drawTca");
        update();
        //System.err.println( "line1164 isDirty: "+isDirty() );
        firePropertyChange("showTca", oldValue, b);
    }

    private static QFunction tcaFunction( String dataset ) throws DasException {
        QFunction result= null;
        if ( dataset.startsWith("/") ) {
           throw new IllegalArgumentException("das2 legacy TCA stuff needs to be implemented");
        } else if ( dataset.startsWith("class:") ) {
            try {
                try {
                    // class:org.autoplot.tca.AutoplotTCASource:vap+file:/tmp/foo.txt?rank2=field1-field4&depend0=field0
                    int argPos= dataset.indexOf(':',6);
                    String className;
                    if ( argPos==-1 ) {
                        className= dataset.substring(6);
                        result = (QFunction) Class.forName(className).newInstance();
                    } else {
                        String arg;
                        className= dataset.substring(6,argPos);
                        arg= dataset.substring(argPos+1);
                        try {
                            result = (QFunction) Class.forName(className).getConstructor(String.class).newInstance(arg);
                        } catch ( ClassNotFoundException | IllegalArgumentException | NoSuchMethodException | SecurityException | InvocationTargetException ex ) {
                            throw new DasException(ex);
                        }
                    }
                    
                } catch (InstantiationException | IllegalAccessException ex) {
                    logger.log(Level.SEVERE, ex.getMessage(), ex);
                }
            } catch (ClassNotFoundException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return result;
    }
        
    /**
     * return the path for the TCA dataset.  This will be a das2server data set
     * address, such as http://www-pw.physics.uiowa.edu/das/das2Server?voyager/tca/earth
     * @return the path of the TCA dataset, or "".
     */
    public String getDataPath() {
        return dataset;
    }
    
    /**
     * @see setDrawTca which turns on additional ticks.
     * @param dataset The URL identifier string of a TCA data set, or "" for no TCAs.
     */
    public void setDataPath(String dataset) {
        if (dataset == null) {
            throw new NullPointerException("null dataPath string not allowed");
        }
        Object oldValue = this.dataset;
        if (dataset.equals(this.dataset)) {
            return;
        }
        this.dataset = dataset;

        if (dataset.equals("")) {
            tcaFunction = null;
            this.tcaData= null;
        } else {
            try {
                tcaFunction= tcaFunction( dataset );
                maybeStartTCATimer();
                this.tcaData= null;
                if ( tcaFunction==null ) {
                    throw new IllegalArgumentException("unable to implement tca QFunction: "+dataset );
                }
                //tcaFunction = DataSetDescriptor.create(dataset);
            } catch (org.das2.DasException de) {
                DasApplication.getDefaultApplication().getExceptionHandler().handle(de);
            }
        }
        markDirty("tcaDataPath");
        update();
        firePropertyChange("dataPath", oldValue, dataset);
    }
    
    @Override
    protected void processEvent(AWTEvent e) {
        super.processEvent(e);
        repaint();
    }
    
    private boolean tcaIsLoading;
    
    private boolean tcaNeedsPainting;
    
    @Override
    boolean isDirty() {
        return super.isDirty() || ( drawTca && tcaIsLoading );
    }
        
    /**
     * Start the timer for reloading TCAs if is hasn't been started already.
     * @see #loadTCASoon() 
     */
    private void maybeStartTCATimer() {
        logger.fine("enter maybeStartTcaTimer");
        final DasCanvas lcanvas= getCanvas();
        
        if ( lcanvas==null ) {
            logger.log( Level.FINER, "canvas is not yet set, returning");
            return;
        }
        
        tcaIsLoading= true;
        
        if ( lcanvas.isPendingChanges( tcaLock ) ) {
            logger.fine("tcatimer is already pending");
        } else {
            lcanvas.registerPendingChange( DasAxis.this, tcaLock );
            if ( tcaTimer==null ) {
                tcaTimer= new TickleTimer( 200, new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                                logger.log( Level.FINER, "mstca, lcanvas={0}", lcanvas);
                        if ( lcanvas!=null ) {
                            lcanvas.performingChange( DasAxis.this, tcaLock );
                        } else {
                            maybeStartTCATimer();
                            return;
                        }
                        try {
                            loadTCASoon();
                        } finally {
                            lcanvas.changePerformed( DasAxis.this, tcaLock );
                            tcaNeedsPainting= true;
                            tcaIsLoading= false;
                        }
                    }
                });
            }
        }
        tcaTimer.tickle("startTcaTimer");
        
    }
    
    /** 
     * Add auxiliary data to an axis (usually OrbitAttitude data for a time axis).
     * This function does the same thing as setDataPath, but with a different interface.
     * The QFunction must have one input parameter which will be positions on this axis 
     * (e.g. times from a time axis).
     * @see setDrawTca which turns on additional ticks.
     * @param f will be called upon to generate auxiliary data sets, or null to disable.
     */
    public synchronized void setTcaFunction( QFunction f ) {
        QFunction oldF= this.tcaFunction;
        this.tcaFunction= f;
        maybeStartTCATimer();
        markDirty("tcaFunction");
        update();
                
        this.tcaNeedsPainting= false;
        this.tcaIsLoading= false;

        //System.err.println( "line1320 isDirty: "+isDirty() );
        firePropertyChange("dataSetDescriptor", null, null );
        firePropertyChange("dataPath", null, null );
        firePropertyChange("tcaFunction", oldF, f );

    }

    private int tcaRows = -1;

    public static final String PROP_TCAROWS = "tcaRows";

    /**
     * the explicit number of TCA rows used to allocate space (not including time location).  If negative, this is ignored.
     * @return tcaRows
     */
    public int getTcaRows() {
        return tcaRows;
    }

    /**
     * the explicit number of additional TCA rows used to allocate space (not including time location).  If negative, this is ignored.
     * @param tcaRows 
     */
    public void setTcaRows(int tcaRows) {
        int oldTcaRows = this.tcaRows;
        this.tcaRows = tcaRows;
        update();
        firePropertyChange(PROP_TCAROWS, oldTcaRows, tcaRows);
    }
    
    private String tcaLabels="";

    public static final String PROP_TCALABELS = "tcaLabels";

    public String getTcaLabels() {
        return tcaLabels;
    }

    /**
     * set the TCA data labels explicitly, using a semi-colon delimited list of labels.
     * @param tcaLabels 
     */
    public void setTcaLabels(String tcaLabels) {
        String oldTcaLabels = this.tcaLabels;
        this.tcaLabels = tcaLabels;
        markDirty("tcaFunction");
        update();
        firePropertyChange(PROP_TCALABELS, oldTcaLabels, tcaLabels);
    }

    /**
     * update the TCAs using the QFunction this.tcaFunction.  We get an example
     * input from the function, then call it for each tick.  We add the property
     * "CONTEXT_0" to be a BINS dataset indicating the overall range for the
     * read.
     */
    private void updateTCADataSet() {
        QFunction ltcaFunction= this.tcaFunction; 
        if ( ltcaFunction==null ) {
            this.tcaData= null;
            return;
        }
        
        logger.fine("updateTCADataSet");

        if ( valueIsAdjusting() ) {
            logger.finest("someone is adjusting this, wait until later to call.");
            return; 
        }

        Units u= getUnits();
        DatumVector tickVDV= getTickV().tickV;
        if ( !u.isConvertibleTo(tickVDV.getUnits()) ) {
            return; // transitional state
        }
        double[] ltickV = tickVDV.toDoubleArray(u);

        DDataSet dep0= DDataSet.createRank1(ltickV.length);
        dep0.putProperty(QDataSet.UNITS,u);
            
        logger.log(Level.FINEST, "update for {0} to {1}", new Object[]{tickVDV.get(0), tickVDV.get(tickVDV.getLength()-1)});
        
        try {

            JoinDataSet ltcaData= new JoinDataSet(2);
            QDataSet exampleInput= ltcaFunction.exampleInput();
            ArrayDataSet ex= ArrayDataSet.copy( exampleInput ); // can be rank 0 or rank 1.
            QDataSet bds= (QDataSet) ex.property(QDataSet.BUNDLE_0);
            Units tcaUnits;
            if ( bds==null ) {
                logger.info("no bundle descriptor, dealing with it.");
                tcaUnits= (Units) ex.property( QDataSet.UNITS, 0 );
            } else {
                tcaUnits= (Units)bds.property( QDataSet.UNITS, 0 );
            }
            if ( tcaUnits==null ) tcaUnits=Units.dimensionless;

            UnitsConverter uc;
            if ( !u.isConvertibleTo(tcaUnits) ) {
                logger.info("tca units are not convertable");
                return;
            }
            uc= UnitsConverter.getConverter( u, tcaUnits );

            DatumRange context= getDatumRange(); // this may not contain all the ticks.
            context= DatumRangeUtil.union( context, u.createDatum( uc.convert(ltickV[0]) ) );
            context= DatumRangeUtil.union( context, u.createDatum( uc.convert(ltickV[ltickV.length-1]) ) );
            ex.putProperty(QDataSet.CONTEXT_0, 0, org.das2.qds.DataSetUtil.asDataSet( context ) );
            QDataSet dx= org.das2.qds.DataSetUtil.asDataSet( getDatumRange().width().divide( getColumn().getWidth() ) );
            ex.putProperty( QDataSet.DELTA_PLUS, 0, dx );
            ex.putProperty( QDataSet.DELTA_MINUS, 0, dx );

            QDataSet outDescriptor=null;

            QDataSet ticks1= null;

            JoinDataSet timeDs= new JoinDataSet(ex.rank()+1);
            for ( int i=0; i<ltickV.length; i++ ) {
                ex.putValue( 0,uc.convert(ltickV[i]) ); // this is sloppy if ex.rank() is 0, but it will work for ArrayDataSet.
                timeDs.join( ArrayDataSet.copy(double.class,ex) );
            }
            timeDs.putProperty( QDataSet.BUNDLE_1, timeDs.slice(0).property(QDataSet.BUNDLE_0) );

            QDataSet tickss= ltcaFunction.values(timeDs);
            if ( tickss instanceof JoinDataSet && tickss.length()>0 ) {
                JoinDataSet jds= (JoinDataSet)tickss;
                QDataSet bundle1= (QDataSet) jds.property(QDataSet.BUNDLE_1);
                jds.putProperty( QDataSet.BUNDLE_1, null );
                if ( jds.slice(0).property(QDataSet.BUNDLE_0)==null ) {
                    // whoops, each dataset doesn't have BUNDLE_0, put BUNDLE_1 back.
                    jds.putProperty( QDataSet.BUNDLE_1, bundle1 );
                }
            }

            if ( tickss.rank()!=2 ) {
                throw new IllegalArgumentException("result of tcaFunction value() should be rank 1");
            }
            for ( int i=0; i<ltickV.length; i++ ) {
                QDataSet ticks= tickss.slice(i);
                if ( outDescriptor==null ) {
                    outDescriptor= (QDataSet) ticks.property(QDataSet.BUNDLE_0);
                    if ( outDescriptor!=null ) {
                        int n= outDescriptor.length();
                        if ( outDescriptor.property(QDataSet.NAME,0)==null && // if the bundle descriptor attached to the ticks doesn't have labels, ignore it.
                             outDescriptor.property(QDataSet.LABEL,0)==null && 
                             ( n<1 || ( outDescriptor.property(QDataSet.NAME,n-1)==null && 
                                        outDescriptor.property(QDataSet.LABEL,n-1)==null ) ) ) {
                            outDescriptor= null;
                        }
                    }
                }
                if ( ticks1==null ) ticks1=ticks;
                if ( ticks1.length()==ticks.length() ) { // ensure that it's a qube.
                    ltcaData.join(ticks);
                    dep0.putValue(i,ltickV[i]);
                } else {
                    logger.log(Level.FINER, "skipping irregular record: {0}", ticks);
                }
            }
            if ( tcaLabels.trim().length()>0 ) {
                outDescriptor= Ops.labelsDataset( tcaLabels.split(";") );
                ltcaData.putProperty( QDataSet.DEPEND_1, outDescriptor );                
            }
            
            if ( outDescriptor==null ) {
                outDescriptor= (QDataSet)tickss.property(QDataSet.BUNDLE_1);
            }
            ltcaData.putProperty( QDataSet.BUNDLE_1, outDescriptor ); //labels will come from here, units may be null.
            ltcaData.putProperty( QDataSet.DEPEND_0, dep0 );

            this.tcaData= ltcaData;
            update();
        } catch ( IllegalArgumentException ex ) { //TODO: provide some feedback!
            //EnumerationUnits eu= EnumerationUnits.create("tcafeedback");
            //QDataSet result= Ops.bundle(null,Ops.replicate( Ops.dataset(eu.createDatum("err")), dep0.length() ));
            //this.tcaData= Ops.link( dep0, result );
            logger.log(Level.WARNING, "exception occured while reading tca", ex );
            ex.printStackTrace();
            this.tcaData= null;
            //update();
        }
    }

    /** 
     * TODO: remove this
     * @deprecated this should not be used.
     * @return the orientation BOTTOM TOP LEFT RIGHT.
     */
    public final int getDevicePosition() {
        switch (orientation) {
            case BOTTOM:
                return getRow().getDMaximum();
            case TOP:
                return getRow().getDMinimum();
            case LEFT:
                return getColumn().getDMinimum();
            default:
                return getColumn().getDMaximum();
        }
    }

    /**
     * return the length of the axis in pixels.
     * @return returns the length of the axis in pixels.
     */
    public int getDLength() {
        if (isHorizontal()) {
            return getColumn().getWidth();
        } else {
            return getRow().getHeight();
        }
    }

    /** 
     * return the axis this is attached to.
     * @return the axis this is attached to.
     */
    public DasAxis getMasterAxis() {
        return dataRange.getCreator();
    }

    /** 
     * attach the axis to another axis, so they will both show the same range,
     * as with a stack of time axes or with a slice.
     * @param axis the axis.
     */
    public void attachTo(DasAxis axis) {
        DataRange oldRange = dataRange;
        dataRange = axis.dataRange;
        oldRange.removePropertyChangeListener(PROP_LOG, dataRangePropertyListener);
        oldRange.removePropertyChangeListener("minimum", dataRangePropertyListener);
        oldRange.removePropertyChangeListener("maximum", dataRangePropertyListener);
        oldRange.removePropertyChangeListener(DataRange.PROPERTY_DATUMRANGE, dataRangePropertyListener);
        dataRange.addPropertyChangeListener(PROP_LOG, dataRangePropertyListener);
        dataRange.addPropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("maximum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener(DataRange.PROPERTY_DATUMRANGE, dataRangePropertyListener);
        if (oldRange.isLog() != dataRange.isLog()) {
            firePropertyChange(PROP_LOG, oldRange.isLog(), dataRange.isLog());
        }
        firePropertyChange("minimum", oldRange.getMinimum(), dataRange.getMinimum());
        firePropertyChange("maximum", oldRange.getMaximum(), dataRange.getMaximum());
        copyFavorites();
        copyHistory();
    }

    /** 
     * disconnect this from the common DataRange object that ties this to other
     * axes, and create a new DataRange.
     */
    public void detach() {
        dataRange.removePropertyChangeListener(PROP_LOG, dataRangePropertyListener);
        dataRange.removePropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.removePropertyChangeListener("maximum", dataRangePropertyListener);
        dataRange.removePropertyChangeListener(DataRange.PROPERTY_DATUMRANGE, dataRangePropertyListener);
        DataRange newRange = new DataRange(this, Datum.create(dataRange.getMinimum(), dataRange.getUnits()),
                Datum.create(dataRange.getMaximum(), dataRange.getUnits()),
                dataRange.isLog());
        dataRange = newRange;
        dataRange.addPropertyChangeListener(PROP_LOG, dataRangePropertyListener);
        dataRange.addPropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("maximum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener(DataRange.PROPERTY_DATUMRANGE, dataRangePropertyListener);
        copyFavorites();
        copyHistory();
    }

    /** 
     * return true if this is attached to another axis.
     * @return true if this is attached to another axis.
     */
    public boolean isAttached() {
        return this != getMasterAxis();
    }

    /** 
     * return the current set of ticks.
     * @return the current set of ticks.
     */
    public TickVDescriptor getTickV() {
        if (tickV == null) {
            updateTickV();
        }
        return tickV;
    }

    /**
     * convenience method for manually setting the ticks so the user
     * needn't understand TickVDescriptor.  setTickV(null) can be 
     * used to reset the ticks as well as setTickV(null,null).
     * @param minorTicks the minor ticks, or null (None) for the current minor ticks
     * @param majorTicks the major ticks, or null (None) to automatic.
     * @see #setTickV(org.das2.graph.TickVDescriptor) 
     */
    public void setTickV( double[] minorTicks, double[] majorTicks ) {
        if ( majorTicks==null ) {
            setTickV(null);
            return;
        }
        if ( minorTicks==null ) {
            minorTicks= getTickV().getMinorTicks().toDoubleArray( getUnits() );
        }
        TickVDescriptor tv= new TickVDescriptor( minorTicks, majorTicks, getUnits() );
        setTickV(tv);
    }
    
    /**
     * add the tick to the current set of major ticks.  If the ticks were
     * automatic, they will now be manual.  Note this copies the ticks into
     * a new array, presuming this is done at most a few tens of times.
     * @param majorTick tick in the same units as the axis.
     * @see #setTickV(double[], double[]) 
     */
    public void addTickV( Datum majorTick ) {
        TickVDescriptor tv= getTickV();
        double[] minorTicks= tv.minorTickV.toDoubleArray(tv.units);
        double[] majorTicks= tv.tickV.toDoubleArray(tv.units);
        double[] newMajorTicks= new double[majorTicks.length+1];
        System.arraycopy( majorTicks, 0, newMajorTicks, 0, majorTicks.length );
        newMajorTicks[majorTicks.length]= majorTick.doubleValue(tv.units);
        TickVDescriptor newtv= new TickVDescriptor( minorTicks, newMajorTicks, tv.units );
        DatumFormatter f= getUserDatumFormatter();
        if ( f==null ) f= getDatumFormatter();
        newtv.setFormatter(f);
        setTickV(newtv);
    }
    
    /**
     * Sets the TickVDescriptor for this axis.  If null is passed in, the
     * axis will put into autoTickV mode, where the axis will attempt to
     * determine ticks using an appropriate algorithm.
     *
     * @param tickV the new ticks for this axis, or null for automatic
     */
    public void setTickV(TickVDescriptor tickV) {
        logger.fine("about to lock for setTickV");
        synchronized ( this ) {
            this.tickV = tickV;
            if (tickV == null) {
                autoTickV = true;
                updateTickV();
            } else {
                autoTickV = false;
                this.datumFormatter= resolveFormatter(tickV);
            }
        }
        if ( dasPlot!=null ) dasPlot.invalidateCacheImage();
        update();
    }

    private String tickValues="";

    /**
     * @see #setTickValues(java.lang.String) 
     */
    public static final String PROP_TICKVALUES = "tickValues";

    public String getTickValues() {
        return tickValues;
    }

    /**
     * manually set the tick positions or spacing.  The following are 
     * examples of accepted settings:<table>
     * <tr><td></td><td>empty string is automatic behavior</td></tr>
     * <tr><td>0,45,90,135,180</td><td>explicit tick positions, in axis units</td></tr>
     * <tr><td>+45</td><td>spacing between ticks, parsed with the axis offset units.</td></tr>
     * <tr><td>+20s/4</td><td>every 20 seconds, with four minor divisions.</td></tr>
     * <tr><td>+30s</td><td>30 seconds spacing between ticks</td></tr>
     * <tr><td>*10/2,3,4,5,6,7,8,9</td><td>normal log axis.</td></tr>
     * <tr><td>*1e2/5,10,50</td><td>log major ticks should be every two cycles</td></tr>
     * <tr><td>*5/2,3,4</td><td>log major ticks at 1,5,25/</td></tr>
     * <tr><td>none</td><td>no ticks</td></tr>
     * </table>
     * @see https://cdn.graphpad.com/faq/1910/file/1487logaxes.pdf about log axes.
     * @see GraphUtil#calculateManualTicks(java.lang.String, org.das2.datum.DatumRange, boolean) 
     * @param ticks 
     */
    public void setTickValues(String ticks) {
        String oldTicks = this.tickValues;
        this.tickValues = ticks;
        if ( oldTicks!=null && !oldTicks.equals(ticks) ) {
            firePropertyChange(PROP_TICKVALUES, oldTicks, ticks);
            updateTickV();
            maybeStartTCATimer();
            if ( dasPlot!=null ) dasPlot.invalidateCacheImage();
        }
    }
    
    /**
     * calculate a set of log spaced ticks.
     * @param dr the range.
     * @return the ticks.
     */
    private TickVDescriptor updateTickVLog( DatumRange dr ) {

        GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
        idlt.setString(this.getTickLabelFont(), "10!U-10");

        int nTicksMax;
        if (isHorizontal()) {
            nTicksMax = (int) Math.floor(getColumn().getWidth() / (idlt.getWidth()));
        } else {
            nTicksMax = (int) Math.floor(getRow().getHeight() / (idlt.getHeight()));
        }

        nTicksMax = (nTicksMax < 20) ? nTicksMax : 20;
        if ( nTicksMax<2 ) nTicksMax= 2;

        TickVDescriptor tickV1 = TickVDescriptor.bestTickVLogNew( dr.min(), dr.max(), 2, nTicksMax, true);  //https://github.com/das-developers/das2java/issues/51
        
        return tickV1;

    }

    /**
     * calculate a set of linear spaced ticks.
     * @param dr the range.
     * @return the ticks.
     */
    private TickVDescriptor updateTickVLinear( DatumRange dr ) {

        TickVDescriptor tickV1;

        int nTicksMax;
        int axisSize;
        if (isHorizontal()) {
            int tickSizePixels = (int) (getFontMetrics(getTickLabelFont()).stringWidth("0.0000") * 1.5);
            axisSize = getColumn().getWidth();
            nTicksMax = axisSize / tickSizePixels;
        } else {
            int tickSizePixels = getFontMetrics(getTickLabelFont()).getHeight() + 6;
            axisSize = getRow().getHeight();
            nTicksMax = axisSize / tickSizePixels;
        }

        nTicksMax = (nTicksMax < 7) ? nTicksMax : 7;

        tickV1 = TickVDescriptor.bestTickVLinear( dr.min(), dr.max(), 3, nTicksMax, false);

        DatumFormatter tdf = resolveFormatter(tickV1);

        Rectangle maxBounds = getMaxBounds(tdf, tickV1);

        if (isHorizontal()) {
            int tickSizePixels = (int) (maxBounds.width + getEmSize() * 2);
            nTicksMax = axisSize / tickSizePixels;
        } else {
            int tickSizePixels = (int) (maxBounds.height);
            nTicksMax = axisSize / tickSizePixels;
        }

        tickV1 = TickVDescriptor.bestTickVLinear( dr.min(), dr.max(), 3, nTicksMax, true);

        return tickV1;

    }

    private DatumFormatter resolveFormatter(TickVDescriptor tickV) {
        DatumFormatter udf= getUserDatumFormatter();
        if ( udf == null ) {
            if ( tickV==null ) {
                return DefaultDatumFormatterFactory.getInstance().defaultFormatter();
            } else {
                if ( this.formatString.length()>0 ) {
                    try {
                        if ( TimeParser.isSpec(this.formatString) ) {
                            return TimeDatumFormatterFactory.getInstance().newFormatter(this.formatString);
                        } else {
                            return DefaultDatumFormatterFactory.getInstance().newFormatter(this.formatString);
                        }
                    } catch ( ParseException ex ) {
                        logger.log(Level.WARNING, "unable to parse formatString: {0}", this.formatString);
                        return tickV.getFormatter();
                    }
                } else {
                    return tickV.getFormatter();
                }
            }
        } else {
            return udf;
        }
    }

    private Rectangle getMaxBounds(DatumFormatter tdf, TickVDescriptor tickV) {
        String[] granny = tdf.axisFormat(tickV.tickV, getDatumRange());
        GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
        Rectangle bounds = new Rectangle();
        for (String granny1 : granny) {
            idlt.setString(this.getTickLabelFont(), granny1);
            bounds.add(idlt.getBounds());
        }
        return bounds;
    }

    private boolean hasLabelCollisions(DatumVector major,DatumFormatter df) {
        if (major.getLength() < 2) {
            return false;
        }
        String[] granny = df.axisFormat(major, getDatumRange());
        GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
        Rectangle[] bounds = new Rectangle[granny.length];
        for (int i = 0; i < granny.length; i++) {
            idlt.setString(this.getTickLabelFont(), granny[i]);
            Rectangle bound = idlt.getBounds();
            if (isHorizontal()) {
                bound.translate((int) transform(major.get(i)), 0);
                bound.width += getEmSize();
            } else {
                bound.translate(0, (int) transform(major.get(i)));
                bound.height += getEmSize()/2;
            }
            bounds[i] = bound;
        }
        Rectangle bound = bounds[0];
        boolean intersects = false;
        for (int i = 1; i < bounds.length; i++) {
            if (bounds[i].intersects(bound)) {
                intersects = true;
            }
            bound = bounds[i];
        }
        return intersects;
    }

    /**
     * indicate if the ticks are packed too closely.  Several consecutive
     * ticks must be with 4 pixels for the test to fail so that log spacing
     * is tolerated.
     *
     * @param minor
     * @return
     */
    private boolean hasTickCollisions(DatumVector minor) {
        if (minor.getLength() < 2) {
            return false;
        }
        int x0 = (int) transform(minor.get(0));
        int intersects = 0;
        for (int i = 1; intersects<8 && i < minor.getLength(); i++) {
            int x1 = (int) transform(minor.get(i));
            if ( x1<DEVICE_POSITIVE_LIMIT ) {
                if (Math.abs(x0 - x1) < 6 ) {
                    intersects++;
                } else {
                    intersects= 0;
                }
                x0= x1;
            }
        }
        return intersects>=8;
    }

    private void updateDomainDivider() {
        DatumRange dr = getDatumRange();

        majorTicksDomainDivider= DomainDividerUtil.getDomainDivider( dr.min(), dr.max(), isLog() );

        while ( majorTicksDomainDivider.boundaryCount(dr.min(), dr.max() ) > 100 ) {
            majorTicksDomainDivider= majorTicksDomainDivider.coarserDivider(false);
        }

        DatumVector major = majorTicksDomainDivider.boundaries(dr.min(), dr.max());
        DatumVector major1 = majorTicksDomainDivider.finerDivider(false).boundaries(dr.min(), dr.max());

        DatumFormatter df;
        df = DomainDividerUtil.getDatumFormatter(majorTicksDomainDivider, dr);
        while ( !hasLabelCollisions(major1,df)) {
            majorTicksDomainDivider = majorTicksDomainDivider.finerDivider(false);
            if ( majorTicksDomainDivider.boundaryCount( dr.min(), dr.max() ) <=1 ) {
                continue;
            }
            df = DomainDividerUtil.getDatumFormatter(majorTicksDomainDivider, dr);
            major= major1;
            major1 = majorTicksDomainDivider.finerDivider(false).boundaries(dr.min(), dr.max());
        }

        while ( hasLabelCollisions(major,df)) {
            majorTicksDomainDivider = majorTicksDomainDivider.coarserDivider(false);
            df = DomainDividerUtil.getDatumFormatter(majorTicksDomainDivider, dr);
            major = majorTicksDomainDivider.boundaries(dr.min(), dr.max());
        }

        while (major.getLength() < 2) {
            majorTicksDomainDivider = majorTicksDomainDivider.finerDivider(false);
            major = majorTicksDomainDivider.boundaries(dr.min(), dr.max());
            df = DomainDividerUtil.getDatumFormatter(majorTicksDomainDivider, dr);
        }

        DomainDivider minorTickDivider=  majorTicksDomainDivider;
        DatumVector minor = major;
        DatumVector minor1 = minorTickDivider.finerDivider(true).boundaries(dr.min(), dr.max());
        while ( ! hasTickCollisions(minor1) ) {
            minorTickDivider= minorTickDivider.finerDivider(true);
            minor= minor1;
            minor1= minorTickDivider.finerDivider(true).boundaries(dr.min(), dr.max());
        }
        //while ( ! hasTickCollisions(minor1) ) {
        //if ( ! hasTickCollisions(minor1) ) {
            //minorTickDivider= minorTickDivider.finerDivider(true);
            //minor= minor1;
            //minor1= minorTickDivider.finerDivider(true).boundaries(dr.min(), dr.max());
        //}
        minorTickDivider.boundaries(dr.min(), dr.max());
        this.minorTicksDomainDivider= minorTickDivider;

        this.tickV = TickVDescriptor.newTickVDescriptor(major, minor);
        dividerDatumFormatter= DomainDividerUtil.getDatumFormatter(majorTicksDomainDivider, dr);

        datumFormatter = resolveFormatter(tickV);

    }

    /**
     * calculate a set of linear spaced ticks based on the DomainDivider class.
     * @param dr the range.
     * @return the ticks.
     */
    private TickVDescriptor updateTickVDomainDivider( DatumRange dr ) {

        try {
            long nminor= minorTicksDomainDivider.boundaryCount( dr.min(), dr.max() );
            while ( nminor>=DomainDivider.MAX_BOUNDARIES ) {
                //TODO: what should we do here?  Transitional state?
                return this.tickV;
            }
            DatumVector major = majorTicksDomainDivider.boundaries(dr.min(), dr.max());
            DatumVector minor = minorTicksDomainDivider.boundaries(dr.min(), dr.max());

            TickVDescriptor tickV1 = TickVDescriptor.newTickVDescriptor(major, minor);
            tickV1.datumFormatter= dividerDatumFormatter;

            return tickV1;
        } catch ( InconvertibleUnitsException ex ) {
            // it's okay to do nothing.
            return tickV;
        }
    }
    
    /**
     * calculate a set of time ticks.
     * @param dr the range.
     * @return the ticks.
     */
    private TickVDescriptor updateTickVTime( DatumRange dr ) {

        int nTicksMax;

        Datum pixel = dr.width().divide(getDLength());
        DatumFormatter tdf;

        TickVDescriptor ltickV;
        FontMetrics fm = getFontMetrics(getTickLabelFont());

        if (isHorizontal()) {
            // two passes to avoid clashes -- not guarenteed
            ltickV = TickVDescriptor.bestTickVTime(dr.min().subtract(pixel), dr.max().add(pixel), 3, 8, false);

            tdf = resolveFormatter(ltickV);
            Rectangle bounds = getMaxBounds(tdf, ltickV);

            int tickSizePixels = (int) (bounds.width + getEmSize() * 2);

            if (drawTca) {
                String item = format(99999.99, "(f8.2)");
                int width = fm.stringWidth(item) + (int) (getEmSize() * 2);
                if (width > tickSizePixels) {
                    tickSizePixels = width;
                }
            }

            int axisSize = getColumn().getWidth();
            nTicksMax = Math.max(2, axisSize / tickSizePixels);

            ltickV = TickVDescriptor.bestTickVTime( dr.min(), dr.max(), 2, nTicksMax, false);
            tdf = resolveFormatter(ltickV);

            bounds = getMaxBounds(tdf, ltickV);
            tickSizePixels = (int) (bounds.getWidth() + getEmSize() * 2);

            if (drawTca) {
                String item = format(99999.99, "(f8.2)");
                int width = fm.stringWidth(item);
                if (width > tickSizePixels) {
                    tickSizePixels = width;
                }
            }
            //nTicksMax = (int) Math.floor( 0.2 + 1.* axisSize / tickSizePixels );

            nTicksMax = (nTicksMax > 1 ? nTicksMax : 2);
            nTicksMax = (nTicksMax < 10 ? nTicksMax : 10);

            boolean overlap = true;
            while (overlap && nTicksMax > 2) {

                ltickV = TickVDescriptor.bestTickVTime( dr.min(), dr.max(), 2, nTicksMax, false);

                if (ltickV.getMajorTicks().getLength() <= 1) {
                    // we're about to have an assertion error, time to debug;
                    logger.log(Level.INFO, "about to assert error: {0}", ltickV.getMajorTicks());
                }
                assert (ltickV.getMajorTicks().getLength() > 1);

                tdf = resolveFormatter(ltickV);

                bounds = getMaxBounds(tdf, ltickV);
                tickSizePixels = (int) (bounds.getWidth() + getEmSize() * 2);

                double x0 = transform(ltickV.getMajorTicks().get(0));
                double x1 = transform(ltickV.getMajorTicks().get(1));

                if (x1 - x0 > tickSizePixels) {
                    overlap = false;
                } else {
                    nTicksMax = nTicksMax - 1;
                }
            }
            ltickV = TickVDescriptor.bestTickVTime( dr.min(), dr.max(), 2, nTicksMax, true);

        } else {
            int tickSizePixels = fm.getHeight();
            int axisSize = getRow().getHeight();
            nTicksMax = axisSize / tickSizePixels;

            nTicksMax = (nTicksMax > 1 ? nTicksMax : 2);
            nTicksMax = (nTicksMax < 10 ? nTicksMax : 10);

            ltickV = TickVDescriptor.bestTickVTime( dr.min(), dr.max(), 3, nTicksMax, true);

        }

        datumFormatter = resolveFormatter(ltickV);

        return ltickV;

    }

    /**
     * update the TCA (ephemeris) axis on the current thread.
     * The lock will have pendingChange and changePerformed with it.
     * @see #loadTCASoon() 
     */
    private void loadTCAImmediately( ) {
        logger.fine("enter updateTCAImmediately...");
        synchronized (this) {
            logger.fine("...got lock.");
            final DasProgressWheel tcaProgress= new DasProgressWheel();
            tcaProgress.started();
            Runnable run= () -> {
                tcaProgress.getPanel(DasAxis.this);
            };
            SwingUtilities.invokeLater(run);
            
            try {
                updateTCADataSet();
            } finally {
                tcaProgress.finished();
                repaint();
            }
        }
    }

    final Object tcaLock= "tcaload_"+this.getDasName();
    
    /**
     * update the TCA dataset.  This will load the TCAs on a RequestProcessor thread sometime soon.
     * @see #loadTCAImmediately() 
     * @see #maybeStartTCATimer() 
     */
    private void loadTCASoon() {
        final DasCanvas lcanvas= getCanvas();
        logger.log(Level.FINE, "updateTCASoon {0}", lcanvas);
        if ( lcanvas!=null ) {
            tcaIsLoading= true;
            lcanvas.registerPendingChange( this, tcaLock );
            RequestProcessor.invokeLater(new Runnable() {
                @Override
                public void run() {
                   lcanvas.performingChange( DasAxis.this, tcaLock );
                   try {
                       loadTCAImmediately( );
                   } finally {
                       lcanvas.changePerformed( DasAxis.this, tcaLock );
                       tcaIsLoading= false;
                   }
                }
            }, DasAxis.this );
        }
    }

    /**
     * call-back for TickMaster
     * @param ticks the new ticks.
     */
    protected void resetTickV( TickVDescriptor ticks ) {
        TickVDescriptor oldTicks = this.tickV;
        boolean doUpdateTicks= true;  // false results in failure to update ticks in tests.
        if ( oldTicks!=null && ticks!=null 
                && oldTicks.minorTickV.getLength()==ticks.minorTickV.getLength() ) {
            for ( int i=0; i<ticks.minorTickV.getLength(); i++ ) {
                if ( !oldTicks.minorTickV.get(i).equals(ticks.minorTickV.get(i)) ) {
                    doUpdateTicks= true;
                }
            }
        } else {
            doUpdateTicks= true;
        }

        if ( doUpdateTicks ) {
        
            this.tickV= ticks;
            datumFormatter = resolveFormatter(tickV);
            if ( drawTca && tcaFunction != null ) {  
                final DasCanvas lcanvas= getCanvas();
                if ( lcanvas!=null ) {
                    tcaIsLoading= true;
                    lcanvas.registerPendingChange( DasAxis.this, tcaLock );
                    lcanvas.performingChange( DasAxis.this, tcaLock );
                    tcaTimer.tickle("resetTickV", (PropertyChangeEvent evt) -> {
                        lcanvas.changePerformed( DasAxis.this, tcaLock );
                        tcaIsLoading= false;
                    });
                }
            }
            firePropertyChange(PROPERTY_TICKS, oldTicks, this.tickV);
        
            repaint();
        }
    }

    private boolean getAutoTickV() {
        return autoTickV;
    }
    
    /**
     * return the number of minor ticks for the spacing.  This should be
     * return by searching for the first two factors.
     * @param dt the step size
     * @return the number of minor ticks
     */
    private int updateTickVManualTicksMinor( double dt ) {
        int scale= (int)Math.log10(dt);
        dt= dt/Math.pow(10,scale);
        if ( (int)dt!=dt ) dt= dt*10;
        int idt= (int)dt;
        switch ( idt ) {
            case 1: return 4;
            case 2: return 4;
            case 3: return 3;
            case 4: return 4;
            case 5: return 5;
            case 6: return 3;
            //case 15: return 3;
            //case 45: return 3;
            //case 25: return 5;
            default: {
                int[] factors= Factorize.factor( idt, new int[0] );
                int result= 1;
                for ( int i=0; i<factors.length; i++ ) {
                    if ( result>3 ) return result;
                    result*= factors[i];
                }
                return 1;
            }
        }
    }
    
    protected void updateTickVManualTicks(String lticks) {
        TickVDescriptor ticks= GraphUtil.calculateManualTicks( lticks, this.getDatumRange(), this.isLog() );
        TickMaster.getInstance().offerTickV( this, ticks );
        this.tickV= ticks; // note ticks might be null.
    }
    
    /**
     * recalculate the tick positions.
     */
    protected void updateTickV() {
        boolean lautoTickV= autoTickV;
        String lticks= tickValues;
        
        DatumRange dr= getDatumRange();
        if ( !dr.min().isFinite() || !dr.max().isFinite() ) {
            logger.info( "range is not finite...");
            return; 
        }
        if (!valueIsAdjusting()) {
            if ( getFont()==null ) return;
            
            if ( lticks.trim().length()>0 ) {
                updateTickVManualTicks(lticks);
                if ( this.tickV!=null ) return;
            }
            
            //if ( getCanvas()==null || getCanvas().getHeight()==0 ) return;
            //if ( ( isHorizontal() ? getColumn().getWidth() : getRow().getHeight() ) < 2 ) return; // canvas is not sized yet
            if ( useDomainDivider ) {
                if ( lockDomainDivider ) {
                    logger.finer("domain divider is locked.");
                } else {
                    updateDomainDivider(); //TODO: doesn't consider width of TCAs.
                }
            } else {
                this.majorTicksDomainDivider= null;
            }
            if (lautoTickV) {
                if ( getDatumRange().width().value()==0 ) {
                    throw new IllegalArgumentException("datum range width is zero" );
                }
                if (majorTicksDomainDivider != null) {
                    TickVDescriptor newTicks= updateTickVDomainDivider(dr);
                    TickMaster.getInstance().offerTickV( this, newTicks );
                } else {
                    TickVDescriptor newTicks;
                    String fs= formatString.trim();
                    if ( fs.length()>0 ) {
                        if ( TimeParser.isSpec(fs) ) { // $Y-$j<br>$H:$M:$S
                            newTicks= updateTickVTime(dr);
                        } else if ( fs.toLowerCase().equals("%e") ) {
                            newTicks= updateTickVLog(dr);
                        } else {
                            newTicks= updateTickVLinear(dr);
                        }
                    } else {
                    //synchronized ( tickLock ) { // deadlock observed here with JMC.
                        if (getUnits() instanceof TimeLocationUnits) {
                            newTicks= updateTickVTime(dr);
                        } else if (dataRange.isLog()) {
                            newTicks= updateTickVLog(dr);
                        } else {
                            newTicks= updateTickVLinear(dr);
                        }
                    }
                    //}
                    //resetTickV(newTicks);
                    if ( this.tickV==null ) resetTickV( newTicks );  // transition cases, pngwalk.
                    TickMaster.getInstance().offerTickV( this, newTicks );
                }
            }
        } else {
            if ( lticks.trim().length()>0 ) {
                updateTickVManualTicks(lticks);
                if ( this.tickV!=null ) return;
            }
            if (lautoTickV) {
                try {
                    if (majorTicksDomainDivider != null) {
                        TickVDescriptor newTicks= updateTickVDomainDivider(dr);
                        TickMaster.getInstance().offerTickV( this, newTicks );
                    } else {
                        TickVDescriptor newTicks;
                        if (getUnits() instanceof TimeLocationUnits) {
                            newTicks= updateTickVTime(dr);
                        } else if (dataRange.isLog()) {
                            newTicks= updateTickVLog(dr);
                        } else {
                            newTicks= updateTickVLinear(dr);
                        }
                        //resetTickV(newTicks);
                        if ( this.tickV==null ) this.tickV= newTicks;  // transition cases
                        TickMaster.getInstance().offerTickV( this, newTicks );
                    }
                } catch ( NullPointerException ex ) {
                    logger.log( Level.WARNING, ex.toString(), ex );
                }
            }
        }

    }
//    private String errorMessage;
//
//    /**
//     * checks the validity of the state, setting variable errorMessage to non-null if there is a problem.
//     */
//    private void checkState() {
//        double dmin = getDataMinimum(dataRange.getUnits());
//        double dmax = getDataMaximum(dataRange.getUnits());
//
//        String em = "";
//
//        if (Double.isNaN(dmin)) {
//            em += "dmin is NaN, ";
//        }
//        if (Double.isNaN(dmax)) {
//            em += "dmax is NaN, ";
//        }
//        if (Double.isInfinite(dmin)) {
//            em += "dmin is infinite, ";
//        }
//        if (Double.isInfinite(dmax)) {
//            em += "dmax is infinite, ";
//        }
//        if (dmin >= dmax) {
//            em += "min => max, ";
//        }
//        if (dataRange.isLog() && dmin <= 0) {
//            em += "min<= 0 and log, ";
//        }
//
//        if (em.length() == 0) {
//            this.errorMessage = null;
//        } else {
//            this.errorMessage = em;
//        }
//    }

    /** 
     * paints the axis component.  The tickV's and bounds should be calculated at this point
     * @param graphics the graphics context.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        logger.finest("enter DasAxis.paintComponent");

        DasCanvas canvas= getCanvas();
        
        if (canvas.isValueAdjusting()) {
            return;
        }

        // prevent incorrect clip
        if ( canvas.getHeight()!=parentHeight && parentHeight<100 ) {
            return;
        }
        if ( canvas.getWidth()!=parentWidth && parentWidth<100 ) return;
        
        try {
            updateTickLength();
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
        
        /* This was code was keeping axes from being printed on PC's
        Shape saveClip = null;
        if (getCanvas().isPrintingThread()) {
        saveClip = graphics.getClip();
        graphics.setClip(null);
        }
         */
        if ( this.lineThickness.length()>0 && !this.lineThickness.equals("1px") ) {
            logger.finer("disabling clip for thicker axis");
            //graphics.setClip(null);
            ((Graphics2D)graphics).setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        } 
        
        logger.log(Level.FINEST, "DasAxis clip={0} @ {1},{2}", new Object[]{graphics.getClip(), getX(), getY()});
//  Here's an effective way to debug axis bounds:
//        if ( "axis_0".equals( getDasName() ) ) {
//            System.err.println("DasAxis clip=" + graphics.getClip() + " @ " + getX() + "," + getY());
//            Rectangle rr= graphics.getClip().getBounds();
//            if ( rr.getHeight()==376 ) {
//                System.err.println("  here");
//                //return;
//            } else {
//                System.err.println("  here2");
//                //return;
//            }
//            graphics.drawRoundRect( rr.x, rr.y, rr.width, rr.height, 30, 30 );
//        }

        Graphics2D g = (Graphics2D) graphics.create();
        //g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        //g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        //g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.translate(-getX(), -getY());
        g.setColor(getForeground());
        
        double px= 1.0;
        if ( this.lineThickness.length()>0 && !this.lineThickness.equals("1px") ) {
            px= DasDevicePosition.parseLayoutStr( this.lineThickness, getEmSize(), getCanvas().getWidth(), 1. );
            g.setStroke( new BasicStroke((float)px, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND) );
        }
        int ipx= (int)Math.ceil(px/2);
        
        /* Debugging code */
        /* The compiler will optimize it out if DEBUG_GRAPHICS == false */
        if (DEBUG_GRAPHICS) {
            Stroke stroke0= g.getStroke();
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 1f, new float[]{3f, 3f}, 0f));
            g.setColor(Color.BLUE);
            if (blLabelRect != null) {
                g.draw(blLabelRect);
            }
            g.setColor(Color.RED);
            if (blLineRect != null) {
                g.draw(blLineRect);
            }
            g.setColor(Color.GREEN);
            if (blTickRect != null) {
                g.draw(blTickRect);
            }
            g.setColor(Color.LIGHT_GRAY);
            if (blTitleRect != null) {
                g.draw(blTitleRect);
            }
            g.setColor(Color.BLUE);
            if (trLabelRect != null) {
                g.draw(trLabelRect);
            }
            g.setColor(Color.RED);
            if (trLineRect != null) {
                g.draw(trLineRect);
            }
            g.setColor(Color.GREEN);
            if (trTickRect != null) {
                g.draw(trTickRect);
            }
            g.setColor(Color.LIGHT_GRAY);
            if (trTitleRect != null) {
                g.draw(trTitleRect);
            }
            g.setStroke( stroke0 );
            
            Rectangle r= getBounds();
            if ( r!=null ) {
                g.setColor( org.das2.util.ColorUtil.CHOCOLATE );
                r.width= r.width-1;
                r.height= r.height-1;
                g.draw(r);
            }

            g.setColor( DEBUG_COLORS[ (debugColorIndex++) % 2 ] );
            
        }
        /* End debugging code */

        TickVDescriptor tickV1;
        tickV1= this.tickV; //findbugs IS2_INCONSISTENT_SYNC.  This caused deadlock.  I think accessing tickV once is a correct fix and doesn't need to be synchronized.        
        if (tickV1 == null || tickV1.tickV.getUnits().isConvertibleTo(getUnits())) {
            if (isHorizontal()) {
                paintHorizontalAxis(g);
            } else {
                paintVerticalAxis(g);
            }
        } else {
            if ( getCanvas().isPrintingThread() ) {
                this.updateImmediately();
                g.setClip(null);
                logger.info("calculated ticks on printing thread, this may cause problems");
                if (isHorizontal()) {
                    paintHorizontalAxis(g);
                } else {
                    paintVerticalAxis(g);
                }
                
            }
        }

        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(getX()-ipx, getY()-ipx, getWidth()+2*ipx, getHeight()+2*ipx);
        } else {
            clip = new Rectangle( clip.x-ipx, clip.y-ipx, clip.width+2*ipx, clip.height+2*ipx);
        }

        if (drawTca && getOrientation() == BOTTOM && tcaData != null && blLabelRect != null && blLabelRect.intersects(clip)) {
            drawTCALabels(g);
        }

        boolean drawBounds= false;
        if ( drawBounds ) {
            Rectangle b= getAxisBounds();
            g.setColor( Color.GREEN );
            g.draw( new Rectangle( b.x, b.y, b.width-1, b.height-1 ) );
            g.setFont( Font.decode("sans-10") );
            g.drawString( String.format("%dx%d",b.width,b.height), b.x+2, b.y+b.height-3 );
            
        }

        g.dispose();
        getDasMouseInputAdapter().paint(graphics);
        
    /* This was code was keeping axes from being printed on PC's
    if (getCanvas().isPrintingThread()) {
    g.setClip(saveClip);
    }
     */
    }

    
    private void drawTCALabels(Graphics2D g) {
        // draw the Ephemeris (TCA) labels.  This should be its own routine
        int position = getRow().getDMaximum();
        int DMin = getColumn().getDMinimum();
        Font tickLabelFont = getTickLabelFont();
        FontMetrics tickLabelFontMetrics = getFontMetrics(tickLabelFont);
        int tickLength = this.tickLen;
        int tick_label_gap = this.tickLen/ 2;
        if ( tick_label_gap<TICK_LABEL_GAP_MIN ) tick_label_gap= TICK_LABEL_GAP_MIN;
        int lineHeight = tickLabelFont.getSize() + getLineSpacing();
        int baseLine = position + Math.max( 0,tickLength ) + tick_label_gap + tickLabelFont.getSize();
        int tick_label_gap_2023 = getFontMetrics(tickLabelFont).stringWidth(" ");
        int rightEdge = DMin - tickLabelFontMetrics.stringWidth("0000") - tick_label_gap_2023;
        GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
        /*
        idlt.setString(this.getGraphics(), "SCET");
        int width = (int)Math.ceil(idlt.getWidth());
        int leftEdge = rightEdge - width;
        idlt.draw(g, (float)leftEdge, (float)baseLine);
        */
        int width, leftEdge;
        
        QDataSet ltcaData= tcaData; // local reference for thread safety
        String[] ltcaLabels= this.tcaLabels.split(";");
        if ( ltcaLabels.length==1 && ltcaLabels[0].trim().equals("") ) ltcaLabels=null;
        
        if ( ltcaData==null ) {
            baseLine += lineHeight;
            idlt.setString( g, "tcaData not available" );
            idlt.draw( g, (float)(rightEdge-idlt.getWidth()), (float)baseLine );
        } else {
            QDataSet bds= (QDataSet) ltcaData.property(QDataSet.BUNDLE_1);
            
            int lines;
            if ( bds==null ) {
                logger.fine("expected TCA data to have BUNDLE dataset");
                if ( ltcaLabels==null ) {
                    return;
                } else {
                    lines= ltcaLabels.length;
                }
            } else {
                lines= Math.min( MAX_TCA_LINES, bds.length() );
            }
            
            Units u= getUnits();
            
            if ( UnitsUtil.isRatioMeasurement(u) ) {
                String ss= getLabel();
                ss= ss.replace("%{RANGE}","").trim();
                if ( ss.trim().length()==0 ) {
                    QDataSet dep0= (QDataSet)ltcaData.property(QDataSet.DEPEND_0);
                    String ll= (String)dep0.property(QDataSet.LABEL);
                    if ( ll==null ) ll= (String)dep0.property(QDataSet.NAME);
                    if ( ll==null ) ll= "";
                    ss= ll;
                }
                idlt.setString( g, ss );
                width = (int) Math.floor(idlt.getWidth() + 0.5);
                leftEdge = rightEdge - width;
                idlt.draw(g, (float) leftEdge, (float) baseLine);
            }
            for (int i = 0; i < lines; i++) {
                baseLine += lineHeight;
                String ss;
                if ( bds==null ) {
                    if ( ltcaLabels==null ) {
                        ss= "???";
                    } else {
                        ss= i<ltcaLabels.length ? ltcaLabels[i] :  "???";
                    }
                } else {
                    if ( ltcaLabels==null ) {
                        ss= (String) bds.property( QDataSet.LABEL, i );
                        if ( ss==null ) {
                            ss= (String) bds.property( QDataSet.NAME, i );
                        }
                    } else {
                        ss= i<ltcaLabels.length ? ltcaLabels[i] :  "???";
                    }
                    if ( ss==null ) {
                        logger.info("TCA data has no labels");
                        ss="   ";
                    }
                    if ( ss.contains("%{") ) {
                        u= (Units)bds.property( QDataSet.UNITS, i );
                        ss= resolveString( ss, "UNITS", u.toString() );
                    }
                }
                idlt.setString( g, ss );

                width = (int) Math.floor(idlt.getWidth() + 0.5);
                leftEdge = rightEdge - width;
                //Rectangle2D bounds= idlt.getBounds2D();
                //g.fill(bounds);
                idlt.draw(g, (float) leftEdge, (float) baseLine);
            }
        }
    }

    private String resolveString( String text, String name, String value ) {
        if ( text.contains("%{" ) ) {
            text= text.replaceAll("%\\{"+name+"\\}", value );
        } else if ( text.contains("$(") ) {
            text= text.replaceAll("\\$\\("+name+"\\)", value );
        }
        return text;
    }

    /**
     * resolve %{RANGE} into the formatted time range (for example)
     * %{RANGE_NOORBIT} was a kludge where the orbit number is not identified, 
     * presumably because it is somewhere else on the plot.
     * @return 
     */
    protected String resolveAxisLabel() {
        String sunits= getDatumRange().getUnits().toString();
        if ( sunits.contains("!A") || sunits.contains("!E")  // Make sure the return is present.
                            || sunits.contains("!B") || sunits.contains("!D") ) {
            if ( !sunits.contains("!N") ) {
                sunits= sunits + "!N";
            }
        }
        String result= resolveString( axisLabel, "UNITS", sunits);
        DatumRange dr= getDatumRange();
        String sdr;
        if ( UnitsUtil.isTimeLocation( dr.getUnits() ) ) {
            sdr= DatumRangeUtil.formatTimeRange(dr,true);
        } else {
            sdr= dr.toString();
        }
        if ( dr instanceof OrbitDatumRange ) {
            String abbrevName= ((OrbitDatumRange)dr).toString();
            String[] ss= abbrevName.split(":");
            if ( abbrevName.split(":").length>3 ) {
                abbrevName= ss[0]+":"+ss[ss.length-1];
            }
            result= resolveString( result, "RANGE_NOORBIT", sdr );
            sdr= sdr+" ("+abbrevName +")";
        } else {
            result= resolveString( result, "RANGE_NOORBIT", sdr );
        }
        result= resolveString( result, "RANGE", sdr );
        result= resolveString( result, "SCAN_RANGE", String.valueOf(getScanRange()) );

        return result;
    }
    
    /** 
     * Paint the axis if it is horizontal  
     * @param g the graphics context
     */
    protected void paintHorizontalAxis(Graphics2D g) {
        try {
            Rectangle clip = g.getClipBounds();
            if (clip == null) {
                clip = new Rectangle(getX(), getY(), getWidth(), getHeight());
            }

            boolean loppositeAxisVisible= this.oppositeAxisVisible;
            if ( loppositeAxisVisible ) {
                DasPlot otherPlot= getCanvas().otherPlotOnTop(this);
                if ( otherPlot!=null ) {
                    if ( otherPlot.getXAxis().getOrientation()!=this.getOrientation() &&
                            otherPlot.getXAxis().isVisible() ) {
                        loppositeAxisVisible= false;
                    }
                }
            }
            boolean bottomLine = ((orientation == BOTTOM || loppositeAxisVisible) && blLineRect != null && blLineRect.intersects(clip));
            boolean bottomTicks = ((orientation == BOTTOM || loppositeAxisVisible) && blTickRect != null && blTickRect.intersects(clip));
            boolean bottomTickLabels = ((orientation == BOTTOM && tickLabelsVisible) && blLabelRect != null && blLabelRect.intersects(clip));
            boolean bottomLabel = ((orientation == BOTTOM && !axisLabel.equals("")) ); // && blTitleRect != null && blTitleRect.intersects(clip));
            boolean topLine = ((orientation == TOP || loppositeAxisVisible) && trLineRect != null && trLineRect.intersects(clip));
            boolean topTicks = ((orientation == TOP || loppositeAxisVisible) && trTickRect != null && trTickRect.intersects(clip));
            boolean topTickLabels = ((orientation == TOP && tickLabelsVisible) && trLabelRect != null && trLabelRect.intersects(clip));
            boolean topLabel = ((orientation == TOP && !axisLabel.equals("")) && trTitleRect != null && trTitleRect.intersects(clip));

            int axisOffsetPx= getAxisOffsetPixels();
            
            int topPosition = getRow().getDMinimum();
            int bottomPosition = getRow().getDMaximum() + axisOffsetPx;
            int DMax = getColumn().getDMaximum();
            int DMin = getColumn().getDMinimum();

            TickVDescriptor ticks = getTickV();
            
            if ( getCanvas().isPrintingThread() ) {
                // check that the ticks are up-to-date.  autoplot_test033 showed this was happening.
                if ( ticks!=null ) {
                    DatumVector majorTicks = ticks.getMajorTicks();
                    if ( majorTicks.getLength()>0 ) {
                        DatumRange x= DatumRangeUtil.union( majorTicks.get(0), majorTicks.get(majorTicks.getLength()-1));
                        boolean recalcTicks= false;
                        DatumRange thisDatumRange= this.getDatumRange();
                        if ( x.intersects(thisDatumRange) ) {
                            if ( x.intersection(thisDatumRange).width().divide( thisDatumRange.width() ).value()<0.3 ) {
                                recalcTicks= true;
                            }
                        } else {
                            recalcTicks= true;
                        }
                        
                        if ( recalcTicks ) {
                            logger.fine("last ditch effort to get useful ticks that we didn't get before because of thread order");
                            TickVDescriptor ticks2= TickMaster.getInstance().requestTickV(this);
                            if ( ticks2!=null ) ticks= ticks2;
                            majorTicks = ticks.getMajorTicks();
                            x= DatumRangeUtil.union( majorTicks.get(0), majorTicks.get(majorTicks.getLength()-1));
                            if ( x.intersects(thisDatumRange) ) {
                                if ( x.intersection(thisDatumRange).width().divide( thisDatumRange.width() ).value()<0.3 ) {
                                    recalcTicks= true;
                                }
                            } else {
                                recalcTicks= true;
                            }
                            if ( recalcTicks ) {
                                System.err.println("still doesn't fit, see https://sourceforge.net/p/autoplot/bugs/1820/");
                                ticks2= TickMaster.getInstance().requestTickV(this); // fun grins and debugging.
                            }
                        }
                    }
                } else {
                    throw new IllegalArgumentException("ticks are not calculated");
                }
            }
            
            if ( DMax-DMin < 2 ) {
                return;
            }
            
            if (bottomLine) {
                logger.log(Level.FINER, "draw H bottomLine at {0}", bottomPosition);
                g.drawLine(DMin, bottomPosition, DMax, bottomPosition);
            }
            if (topLine) {
                logger.log(Level.FINER, "draw H topLine at {0}", topPosition);
                g.drawLine(DMin, topPosition, DMax, topPosition);
            }
            
            // if there are reference lines requested, then draw them.
            if ( !(reference.length()==0) ) {
                String[] rr= reference.split(",",-2);
                for ( String r: rr ) {
                    r= r.trim();
                    if ( r.length()>0 ) {
                        try {
                            Datum dreference;
                            dreference = dataRange.getUnits().parse(r);
                            float d= (float)transform(dreference);
                            if ( d>DMin && d<DMax ) {
                                g.draw( new Line2D.Float( d, bottomPosition, d, topPosition ) );
                                //g.drawLine( i, bottomPosition, i, topPosition );
                            }
                        } catch (ParseException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
            
            int tickLengthMajor = tickLen; //labelFont.getSize() * 2 / 3;
            int tickLengthMinor = tickLengthMajor / 2;
            int tickLength;

            if ( ticks==null ) {
                logger.fine("ticks are not ready");
                return;
            }
            
            String[] labels = tickFormatter(ticks.tickV, getDatumRange());

            for (int i = 0; i < ticks.tickV.getLength(); i++) {
                Datum tick1 = ticks.tickV.get(i);
                float ftickPosition= (float)transform(tick1);
                int tickPosition = (int) Math.floor(ftickPosition);
                if (DMin <= ftickPosition && ftickPosition <= DMax) {
                    tickLength = tickLengthMajor;
                    if (bottomTicks && tickLength!=0 ) {
                        logger.log(Level.FINER, "draw H tick at {0}", ftickPosition);
                        g.draw( new Line2D.Float(ftickPosition, bottomPosition, ftickPosition, bottomPosition + tickLength) );
                        //g.drawLine(tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);                        
                    }
                    if (bottomTickLabels) {
                        drawLabel(g, tick1, labels[i], i, tickPosition, bottomPosition + Math.max(0,tickLength) );
                    }
                    if (topTicks && tickLength!=0 ) {
                        g.draw( new Line2D.Float( ftickPosition, topPosition, ftickPosition, topPosition - tickLength + 1 ) );
                        //g.drawLine(tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    }
                    if (topTickLabels) {
                        drawLabel(g, tick1, labels[i], i, tickPosition, topPosition - Math.max(0,tickLength) + 1);
                    }
                }
            }

            for (int i = 0; i < ticks.minorTickV.getLength(); i++) {
                Datum tick = ticks.minorTickV.get(i);
                float ftickPosition= (float)transform(tick);
                int tickPosition = (int) Math.floor(ftickPosition);
                if (DMin <= tickPosition && tickPosition <= DMax) {
                    tickLength = tickLengthMinor;
                    if (bottomTicks && tickLength!=0 ) {
                        g.draw( new Line2D.Float( ftickPosition, bottomPosition, ftickPosition, bottomPosition + tickLength ) );
                        //g.drawLine(tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                    }
                    if (topTicks && tickLength!=0 ) {
                        g.draw( new Line2D.Float( ftickPosition, topPosition, ftickPosition, topPosition - tickLength) );
                        //g.drawLine(tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    }
                }
            }

            boolean debugBoundsBox= false;
            if ( debugBoundsBox ) {
                Color c0= g.getColor();
                g.setColor( Color.MAGENTA );
                g.drawRoundRect( clip.x, clip.y, clip.width-1, clip.height-1, 14, 14 );
                //g.drawLine( clip.x, clip.y+clip.height-1, clip.x+clip.width-1,  clip.y+clip.height-1 );
                g.setColor( c0 );
            }
                    
            if (!axisLabel.equals("") && isTickLabelsVisible() ) {
                Graphics2D g2 = (Graphics2D) g.create();
                int titlePositionOffset = getTitlePositionOffset();
                GrannyTextRenderer gtr = GraphUtil.newGrannyTextRenderer();
                g2.setFont(getLabelFont());
                if ( dasPlot!=null ) {
                    gtr.setAlignment( dasPlot.getMultiLineTextAlignment() );
                } else {
                    gtr.setAlignment( 0.5f );
                }
                String axislabel1= resolveAxisLabel();
                gtr.setString(g2, axislabel1);
                int titleWidth = (int) gtr.getWidth();
                int baseline;
                int leftEdge;
                if ( debugBoundsBox ) { 
                    baseline = bottomPosition + titlePositionOffset;
                    g2.drawString( ">>"+axislabel1+"<<", clip.x, clip.y + g2.getFontMetrics().getHeight() );
                    gtr.draw(g2, (float) clip.x, (float) clip.y + g2.getFontMetrics().getHeight()*2 );
                    g2.drawString( ""+baseline+ " "+bottomPosition+ " " +titlePositionOffset + " "+bottomLabel, clip.x, clip.y + 3*g2.getFontMetrics().getHeight() );
                }
                if (bottomLabel) {
                    leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                    baseline = bottomPosition + titlePositionOffset;

                    boolean drawBack=isOpaqueBackground();
                    if ( drawBack ) {
                        Rectangle2D back= gtr.getBounds2D();
                        back= new Rectangle2D.Double( leftEdge+back.getX(), baseline+back.getY(), back.getWidth(), back.getHeight() );
                        Color c0= g2.getColor();
                        g2.setColor(Color.WHITE);
                        g2.fill(back);
                        g2.setColor(c0);
                    }
                    gtr.draw(g2, (float) leftEdge, (float) baseline);
                }
                if ( debugBoundsBox ) {
                    leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                    baseline = bottomPosition + titlePositionOffset;
                    g2.drawLine( clip.x, clip.y, (int)leftEdge, (int)baseline );
                }
                if (topLabel) {
                    leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                    baseline = topPosition - titlePositionOffset;
                    gtr.draw(g2, (float) leftEdge, (float) baseline);
                }
                g2.dispose();
            }            
        } catch (InconvertibleUnitsException ex) {
            // do nothing
        }
    }

    /**
     * return the offset of the axis from the plot, as specified by the axisOffset property.
     * @return the offset of the axis from the plot
     */
    protected int getAxisOffsetPixels() {
        int axisOffsetPx=0;
                    
        try {
            double[] pos = DasDevicePosition.parseLayoutStr(getAxisOffset());
            if ( pos[0]==0 ) {
                axisOffsetPx = (int) ( Math.round( pos[1]*getEmSize() + pos[2] ) ); // make independent from row layout for initialization.
            } else {
                axisOffsetPx = (int) (Math.round( pos[0]*getRow().getHeight() + pos[1]*getEmSize() + pos[2] ));
            }
        } catch ( ParseException ex ) {
            logger.warning(ex.getMessage());
            return 0;
        }
        return axisOffsetPx;
    }
    
    /**
     * Paint the vertical axis
     * @param g the graphics context
     */
    protected void paintVerticalAxis(Graphics2D g) {
        try {
            Rectangle clip = g.getClipBounds();
            if (clip == null) {
                clip = new Rectangle(getX(), getY(), getWidth(), getHeight());
            }

            boolean loppositeAxisVisible= this.oppositeAxisVisible;
            if ( loppositeAxisVisible ) {
                DasPlot otherPlot= getCanvas().otherPlotOnTop(this);
                if ( otherPlot!=null ) {
                    if ( otherPlot.getYAxis().getOrientation()!=this.getOrientation() &&
                            otherPlot.getXAxis().isVisible() ) {
                        loppositeAxisVisible= false;
                    }
                }
            }            
            boolean leftLine = ((orientation == LEFT || loppositeAxisVisible) && blLineRect != null && blLineRect.intersects(clip));
            boolean leftTicks = ((orientation == LEFT || loppositeAxisVisible) && blTickRect != null && blTickRect.intersects(clip));
            boolean leftTickLabels = ((orientation == LEFT && tickLabelsVisible) && blLabelRect != null && blLabelRect.intersects(clip));
            boolean leftLabel = ((orientation == LEFT && !axisLabel.equals("")) && blTitleRect != null && blTitleRect.intersects(clip));
            boolean rightLine = ((orientation == RIGHT || loppositeAxisVisible) && trLineRect != null && trLineRect.intersects(clip));
            boolean rightTicks = ((orientation == RIGHT || loppositeAxisVisible) && trTickRect != null && trTickRect.intersects(clip));
            boolean rightTickLabels = ((orientation == RIGHT && tickLabelsVisible) && trLabelRect != null && trLabelRect.intersects(clip));
            boolean rightLabel = ((orientation == RIGHT && !axisLabel.equals("")) && trTitleRect != null && trTitleRect.intersects(clip));

            int leftPosition = getColumn().getDMinimum();
            int rightPosition = getColumn().getDMaximum();
            int DMax = getRow().getDMaximum();
            int DMin = getRow().getDMinimum();
            
            TickVDescriptor ticks = getTickV();

            if ( DMax-DMin<2 ) {
                return;
            }
            
            if ( ticks==null ) {
                logger.fine("ticks are not ready");
                return;
            }
            
            int axisOffsetPx= getAxisOffsetPixels();
            if (leftLine) {
                leftPosition= leftPosition - axisOffsetPx;
                logger.log(Level.FINER, "draw V leftline at {0}", leftPosition);
                g.drawLine(leftPosition, DMin, leftPosition, DMax);
            }
            if (rightLine) {
                rightPosition= rightPosition + axisOffsetPx;
                logger.log(Level.FINER, "draw V rightline at {0}", rightPosition);
                g.drawLine(rightPosition, DMin, rightPosition, DMax);
            }
            
            if ( !(reference.length()==0) ) {
                String[] rr= reference.split(",",-2);
                for ( String r: rr ) {
                    r= r.trim();
                    if ( r.length()>0 ) {
                        Datum dreference;
                        try {
                            dreference = dataRange.getUnits().parse(r);
                            float f= (float)transform(dreference);
                            if ( f>DMin && f<DMax ) {
                                g.draw( new Line2D.Float( rightPosition, f, leftPosition, f ) );  
                                //g.drawLine( rightPosition, i, leftPosition, i );  
                            }
                        } catch (ParseException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }            
                                    
            int tickLengthMajor = tickLen;
            int tickLengthMinor = tickLengthMajor / 2;
            int tickLength;

            String[] labels = tickFormatter(ticks.tickV, getDatumRange());
            for (int i = 0; i < ticks.tickV.getLength(); i++) {
                Datum tick1 = ticks.tickV.get(i);
                float ftickPosition= (float)( transform(tick1) );
                int tickPosition = (int) Math.floor( ftickPosition );
                if (DMin <= tickPosition && tickPosition <= DMax) {

                    tickLength = tickLengthMajor;
                    if (leftTicks && tickLength!=0 ) {
                        logger.log(Level.FINER, "draw V tick at {0}", ftickPosition);
                        g.draw( new Line2D.Float( leftPosition, ftickPosition, leftPosition - tickLength, ftickPosition) );
                        //g.drawLine(leftPosition, tickPosition, leftPosition - tickLength, tickPosition);
                    }
                    if (leftTickLabels) {
                        drawLabel(g, tick1, labels[i], i, leftPosition - Math.max( 0,tickLength ), tickPosition);
                    }
                    if (rightTicks && tickLength!=0 ) {
                        g.draw( new Line2D.Float( rightPosition, ftickPosition, rightPosition + tickLength, ftickPosition ) );
                        //g.drawLine(rightPosition, tickPosition, rightPosition + tickLength, tickPosition);
                    }
                    if (rightTickLabels) {
                        drawLabel(g, tick1, labels[i], i, rightPosition + Math.max( 0,tickLength ), tickPosition);
                    }
                }
            }

            for (int i = 0; i < ticks.minorTickV.getLength(); i++) {
                double tick1 = ticks.minorTickV.doubleValue(i, getUnits());
                float ftickPosition= (float)( transform(tick1, ticks.units)+0.0001 );
                //int tickPosition = (int) Math.floor( ftickPosition );
                if (DMin <= ftickPosition && ftickPosition <= DMax) {
                    tickLength = tickLengthMinor;
                    if (leftTicks && tickLength!=0 ) {
                        g.draw( new Line2D.Float(leftPosition, ftickPosition, leftPosition - tickLength, ftickPosition) );
                        //g.drawLine(leftPosition, tickPosition, leftPosition - tickLength, tickPosition);
                    }
                    if (rightTicks && tickLength!=0 ) {
                        g.draw( new Line2D.Float(rightPosition, ftickPosition, rightPosition + tickLength, ftickPosition) );
                        //g.drawLine(rightPosition, tickPosition, rightPosition + tickLength, tickPosition);
                    }
                }
            }


            if (!axisLabel.equals("") && isTickLabelsVisible() ) {
                Graphics2D g2 = (Graphics2D) g.create();
                int titlePositionOffset = getTitlePositionOffset();
                GrannyTextRenderer gtr = GraphUtil.newGrannyTextRenderer();
                g2.setFont(getLabelFont());
                if ( dasPlot!=null ) {
                    gtr.setAlignment(dasPlot.getMultiLineTextAlignment());
                } else {
                    gtr.setAlignment(0.5f);
                }
                gtr.setString(g2, resolveAxisLabel() );
                int titleWidth = (int) gtr.getWidth();
                int baseline;
                int leftEdge;
                if (leftLabel) {
                    g2.rotate(-Math.PI / 2.0);
                    leftEdge = -DMax + (DMax - DMin - titleWidth) / 2;
                    baseline = leftPosition - titlePositionOffset;
                    gtr.draw(g2, (float) leftEdge, (float) baseline);
                }
                if (rightLabel) {
                    if (flipLabel) {
                        g2.rotate(-Math.PI / 2.0);
                        leftEdge = DMin + (DMax - DMin + titleWidth) / 2;
                        baseline = rightPosition + titlePositionOffset;
                        gtr.draw(g2, (float) -leftEdge, (float) baseline);
                        g2.getClipBounds();
                    } else {
                        g2.rotate(Math.PI / 2.0);
                        leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                        baseline = -rightPosition - titlePositionOffset;
                        gtr.draw(g2, (float) leftEdge, (float) baseline);
                    }
                }
                g2.dispose();
            }
        } catch (InconvertibleUnitsException e) {
            // do nothing
        }
    }
    /**
     * allows multiple stacked axes to be manually lined up
     * (if you somehow magically know the offset)
     * @param leftXOverride
     * @deprecated use setLabelOffset instead.
     * @see #setLabelOffset(java.lang.String) 
     */
    public void setLeftXLabelOverride(int leftXOverride) {
    	this.leftXOverride = leftXOverride;
    }
    
    /**
     * explicitly set the position of the label from the axis.
     * For example, "4em" will position the y-axis label 4ems away from the
     * axis.  Empty string is default automatic behavior.
     * @param spec offset string like "5em+5px"
     */
    public void setLabelOffset( String spec ) {
        this.labelOffset= spec;
        update();
    }
    
    /**
     * 
     * @return the label offset.
     */
    public String getLabelOffset( ) {
        return this.labelOffset;
    }

    private String axisOffset = "";

    public static final String PROP_AXISOFFSET = "axisOffset";

    public String getAxisOffset() {
        return axisOffset;
    }

    /**
     * set the offset of the axis from the plot.
     * @param axisOffset 
     */
    public void setAxisOffset(String axisOffset) {
        String oldAxisOffset = this.axisOffset;
        this.axisOffset = axisOffset;
        update();
        firePropertyChange(PROP_AXISOFFSET, oldAxisOffset, axisOffset);
    }

    /** 
     * get the offset of the label from the baseline in pixels.
     * @return the offset.
     */
    protected int getTitlePositionOffset() {
        Font tickLabelFont = getTickLabelFont();
        FontMetrics fm = getFontMetrics(tickLabelFont);
        Font labelFont = getLabelFont();
		int zeroOrPosTickLen= Math.max(0,tickLen);
        //int tickLength = this.tickLen; // tickLabelFont.getSize() * 2 / 3;

        GrannyTextRenderer gtr = GraphUtil.newGrannyTextRenderer();
        if ( dasPlot!=null ) gtr.setAlignment( dasPlot.getMultiLineTextAlignment() );
        gtr.setString(labelFont, axisLabel);

        int offset;

        switch (orientation) {
            case BOTTOM:
                if ( tickV!=null && tickV.minorTickV.getLength()>0 ) {
                    offset = tickLabelFont.getSize() + zeroOrPosTickLen + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize() / 2;
                } else {
                    offset = labelFont.getSize() + labelFont.getSize() / 2;
                }
                if ( drawTca && tcaData != null ) {
                    offset += Math.min( MAX_TCA_LINES, tcaData.length(0) ) * (tickLabelFont.getSize() + getLineSpacing());
                } else if ( formatString.length()>0 ) {
                    offset += blLabelRect.height - ( 2 *( tickLabelFont.getSize() + getLineSpacing() ) );
                }
                if ( labelOffset.length()>0 && axisLabel.length()>0 ) {
                    offset= tickLabelFont.getSize() + (int)DasDevicePosition.parseLayoutStr( labelOffset, getEmSize(), getRow().getHeight(), 0 );
                }
                break;
            case TOP:
                offset = zeroOrPosTickLen + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize() / 2 + (int) gtr.getDescent();
                break;
            case LEFT:
                //offset = zeroOrPosTickLen + (int)this.blLabelRect.getWidth() + fm.stringWidth(" ") + labelFont.getSize() / 2 + (int) gtr.getDescent();
                if ( tickV!=null && tickV.minorTickV.getLength()>0 ) {
                    offset = getColumn().getDMinimum() - blLabelRect.x + labelFont.getSize() / 2 + (int) gtr.getDescent() - getAxisOffsetPixels();
                } else {
                    offset = getColumn().getDMinimum() - blLabelRect.x - getAxisOffsetPixels();
                }
                break;
            default:
                if ( trLabelRect==null ) {
                    offset= 20;
                } else {
                    offset = this.trLabelRect.x + this.trLabelRect.width - getColumn().getDMaximum() + labelFont.getSize() / 2 + (int) (flipLabel ? gtr.getAscent() : gtr.getDescent());
                }   
                break;
        }
        return offset;
    }

    /**
     * calculate the spacing (whitespace) between the TCA items.
     * @return the spacing between lines.
     */
    public int getLineSpacing() {
        return getTickLabelFont().getSize() / 4;
    }

    /** 
     * Draw each tick label of the axis.
     * @param g graphics context
     * @param value value to look up.
     * @param label the label
     * @param index the index.  The default does not use this, but overriding methods may use it.
     * @param x tick position relative to the canvas
     * @param y tick position relative to the canvas
     */
    protected void drawLabel(Graphics2D g, Datum value, String label, int index, int x, int y) {

        if (!tickLabelsVisible) {
            return;
        }

        g.setFont(getTickLabelFont());
        GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
        idlt.setString(g, label);

        int width;
        width = (int) (isHorizontal() ? idlt.getLineOneWidth() : idlt.getWidth());
        int height = (int) idlt.getHeight();
        int ascent = (int) idlt.getAscent();

        int tick_label_gap = tickLen/2; //getFontMetrics(getTickLabelFont()).stringWidth(" ");

        if ( tick_label_gap<TICK_LABEL_GAP_MIN ) tick_label_gap= TICK_LABEL_GAP_MIN;
        
        switch (orientation) {
            case BOTTOM:
                x -= width / 2;
                y += getTickLabelFont().getSize() + tick_label_gap;
                break;
            case TOP:
                x -= width / 2;
                y -= tick_label_gap + idlt.getDescent();
                break;
            case LEFT:
                x -= (width + tick_label_gap);
                y += ascent - height / 2;
                break;
            default:
                x += tick_label_gap;
                y += ascent - height / 2;
                break;
        }
        
        boolean drawBack=isOpaqueBackground();
        if ( drawBack ) { // if we were to prevent ticks from clobbering the background...
            Color c= g.getColor();
            g.setColor( g.getBackground() );
            Rectangle r= idlt.getBounds();
            r.translate( x, y );
            g.fillRoundRect( r.x, r.y, r.width, r.height, 3, 3 );
            g.setColor( c );
        }
        
        idlt.draw(g, x, y);
        if (orientation == BOTTOM && drawTca && tcaData != null) {
            drawTCAItems(g, value, x, y, width);
        }
    }

    /**
     * draw the TCA (Ephemeris) data below the tick. This is done on the event queue.
     * @param g graphics context.
     * @param value value to look up.
     * @param x tick position relative to the canvas
     * @param y tick position relative to the canvas
     * @param width nominal width (?), not used.
     */
    private void drawTCAItems(Graphics g, Datum value, int x, int y, int width) {
        int index;

        int baseLine, leftEdge, rightEdge;
        double pixelSize;
        double tcaValue;

        QDataSet ltcaData= tcaData;
        
        if ( ltcaData == null || ltcaData.length() == 0) {
            return;
        }

        QDataSet dep0= (QDataSet)ltcaData.property(QDataSet.DEPEND_0);
        try { // special code to make a local copy of the dataset needs to copy slices since units can be enumeration or data units.
            //JoinDataSet jds= new JoinDataSet(ltcaData.slice(0));
            //for ( int i=1; i<ltcaData.length(); i++ ) {
            //    jds.join(ltcaData.slice(i));
            //}
            //ltcaData= jds;
        } catch ( IllegalArgumentException ex ) {
            ltcaData= DDataSet.copy(ltcaData);
        }
                

        baseLine = y;
        leftEdge = x;
        rightEdge = leftEdge + width;

        if ( !SemanticOps.getUnits(dep0).isConvertibleTo(value.getUnits() ) ) {
            return;
        }
            
        index = org.das2.qds.DataSetUtil.closestIndex( dep0, value);
        if ( index < 0 || index >= ltcaData.length() ) {
            return;
        }

        pixelSize = getDatumRange().width().divide(getDLength()).doubleValue( getUnits().getOffsetUnits() );

        if (ltcaData.length() == 0) {
            g.drawString("tca data is empty", leftEdge, baseLine);
            return;
        }

        tcaValue = dep0.value(index);

        //Added in to say take nearest nieghbor as long as the distance to the nieghbor is
        //not more than the xtagwidth.
        QDataSet xTagWidth = org.das2.qds.DataSetUtil.guessCadenceNew( dep0, null );
        
        double limit;
        try {
            UnitsConverter uc= UnitsConverter.getConverter( SemanticOps.getUnits(dep0).getOffsetUnits(), getUnits().getOffsetUnits() );
            limit = Math.max( uc.convert( xTagWidth.value() ), pixelSize ); //DANGER: check that this is correct
        } catch ( InconvertibleUnitsException ex ) {
            ex.printStackTrace();
            throw new RuntimeException( ex );
        }

        if (Math.abs(tcaValue - value.doubleValue(getUnits())) > limit) { //TODO:suspicious
            return;
        }

        Font tickLabelFont = getTickLabelFont();
        FontMetrics fm = getFontMetrics(tickLabelFont);
        int lineHeight = tickLabelFont.getSize() + getLineSpacing();

        int lines= Math.min( MAX_TCA_LINES, ltcaData.length(0) );

        for (int i = 0; i < lines; i++) {
            try {
                baseLine += lineHeight;
                QDataSet test1= ltcaData.slice(index);
                QDataSet v1= ArrayDataSet.copy( test1.slice(i) );
                String item;
                try {
                    item= org.das2.qds.DataSetUtil.getStringValue( v1, v1.value() );
                } catch ( IllegalArgumentException ex ) {
                    item= String.valueOf(v1.value());
                }
                width = fm.stringWidth(item);
                leftEdge = rightEdge - width;
                g.drawString(item, leftEdge, baseLine);
            } catch ( RuntimeException ex ) {
                if ( i==0 ) {
                    ex.printStackTrace();
                }
                g.drawString("exception",leftEdge, baseLine);
            }
        }
    }

    /** 
     * get the font for tick labels.  If the component currently has null for the
     * font, then Font.decode("sans-12") is used and a warning logged.
     * @return the font to use for ticks.
     */
    public Font getTickLabelFont() {
        Font f= this.getFont();
        if ( f==null ) {
            //logger.warning("2285: font was null, using sans-12.  Code should check first.");
            f= Font.decode("sans-12");
        }
        return f;
    }

    /** 
     * get the font for labels.  If the component currently has null for the
     * font, then Font.decode("sans-12") is used.
     * @return the font to use for labels.
     */
    public Font getLabelFont() {
        Font f= this.getFont();
        if ( f==null ) {
            logger.warning("2285: font was null, using sans-12");
            f= Font.decode("sans-12");
        }
        
        if ( fontSize.length()>0 && !fontSize.equals("1em") ) {
            try {
                double[] dd= DasDevicePosition.parseLayoutStr(getFontSize());
                if ( dd[1]==1 && dd[2]==0 ) {
                    // do nothing
                } else {
                    double parentSize= f.getSize2D();
                    double newSize= dd[1]*parentSize + dd[2];
                    f= f.deriveFont((float)newSize);    
                    return f;
                }
            } catch (ParseException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        return f;
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

    /**
     * class for storing an axis transform.  This is used to keep track
     * of the cache image in DasPlot.
     */
    public static class Memento {

        private DatumRange range;
        private int dmin,  dmax;
        private boolean log;
        private boolean flipped;
        private boolean horizontal;

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.range != null ? this.range.hashCode() : 0);
            hash = 29 * hash + this.dmin;
            hash = 29 * hash + this.dmax;
            hash = 29 * hash + ( this.log ? 1 : 0 );
            hash = 29 * hash + ( this.flipped ? 1 : 0 );
            hash = 29 * hash + ( this.horizontal ? 1 : 0 );
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if ( o==null || !( o instanceof Memento) ) {
                return false;
            } else {
                Memento m = (Memento) o;
                return this == m || (this.range.equals(m.range) &&
                    this.dmin == m.dmin &&
                    this.dmax == m.dmax &&
                    this.log == m.log &&
                    this.flipped == m.flipped &&
                    this.horizontal == m.horizontal );
            }
        }

        @Override
        public String toString() {
            return (log ? "log " : "") + range.toString() + " (" + ( DatumUtil.asOrderOneUnits(range.width()).toString() ) + ") " + (dmax - dmin) + " pixels @ " + dmin;
        }
    }

    /**
     * get the memento object which identifies the state of the axis transformation.
     * @return the momento.
     */
    public Memento getMemento() {
        Memento result = new Memento();
        result.range = this.getDatumRange();
        if (isHorizontal()) {
            if (getColumn() != DasColumn.NULL) {
                result.dmin = getColumn().getDMinimum();
                result.dmax = getColumn().getDMaximum();
            } else {
                result.dmin = 0;
                result.dmax = 0;
            }
        } else {
            if (getRow() != DasRow.NULL) {
                result.dmin = getRow().getDMinimum();
                result.dmax = getRow().getDMaximum();
            } else {
                result.dmin = 0;
                result.dmax = 0;
            }
        }
        result.log = this.isLog();
        result.flipped = flipped;
        result.horizontal = this.isHorizontal();
        return result;
    }

    /**
     * return the AffineTransform, or null.  The transform will be applied after the input
     * transform is applied.  So to just get the transform, pass in identity.
     * @param memento memento from another axis state.
     * @param at initial transform
     * @return the transform from that state to this state, or null if no transform can be created.
     */
    public AffineTransform getAffineTransform(Memento memento, AffineTransform at) {
        if (at == null) {
            return null;
        }
        if (memento.log != isLog()) {
            return null;
        }
        if (memento.flipped != flipped) {
            return null;
        }
        if (!memento.range.getUnits().isConvertibleTo(getUnits())) {
            return null;
        }

        //TODO: remove cut-n-paste code
        //return getAffineTransform(memento.range, false, at);

        double dmin0, dmax0;
        dmin0 = transform(memento.range.min());
        dmax0 = transform(memento.range.max());

        double scale2 = (0. + getMemento().dmin - getMemento().dmax) / (memento.dmin - memento.dmax);
        double trans2 = -1 * memento.dmin * scale2 + getMemento().dmin;

        if ( dmin0==DEVICE_POSITIVE_LIMIT || dmin0==-DEVICE_POSITIVE_LIMIT | dmax0==DEVICE_POSITIVE_LIMIT | dmax0==DEVICE_POSITIVE_LIMIT ) {
            logger.fine("unable to create transform in getAffineTransform");
        }

        if (!(isHorizontal() ^ flipped)) {
            double tmp = dmin0;
            dmin0 = dmax0;
            dmax0 = tmp;
        }

        if (!isHorizontal()) {
            double dmin1 = getRow().getDMinimum();
            double dmax1 = getRow().getDMaximum();

            double scaley = (dmin0 - dmax0) / (dmin1 - dmax1);
            double transy = -1 * dmin1 * scaley + dmin0;
            at.translate(0., transy);
            at.scale(1., scaley);
            at.translate(0., trans2 );
            at.scale(1., scale2 );
        } else {
            double dmin1 = getColumn().getDMinimum();
            double dmax1 = getColumn().getDMaximum();

            double scalex = (dmin0 - dmax0) / (dmin1 - dmax1);
            double transx = -1 * dmin1 * scalex + dmin0;
            at.translate(transx, 0);
            at.scale(scalex, 1.);
            at.translate( trans2, 0. );
            at.scale( scale2, 1. );

        }

        if (at.getDeterminant() == 0.000) {
            return null;
        } else {
            return at;
        }
    }

    /** 
     * @return a copy of the axis, also cloning the dataRange that backs the axis.
     */
    @Override
    public Object clone() {
        try {
            DasAxis result = (DasAxis) super.clone();
            result.dataRange = (DataRange) result.dataRange.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new Error("Assertion failure");
        }
    }

    private void setTickDirection(int direction) {
        switch (direction) {
            case UP:
            case RIGHT:
                tickDirection = -1;
                break;
            case DOWN:
            case LEFT:
                tickDirection = 1;
                break;
            default:
                throw new IllegalArgumentException("Invalid tick direction");
        }
    }

    /** 
     * calculate the biggest label width
     * @return the width in pixels of the widest label.
     */
    private int getMaxLabelWidth() {
        try {
            Font f = getTickLabelFont();
            TickVDescriptor ticks = getTickV();
            if ( ticks==null ) return 10;
            DatumVector tickv = ticks.tickV;
            int size = Integer.MIN_VALUE;
            for (int i = 0; i < tickv.getLength(); i++) {
                String label = tickFormatter(tickv.get(i));
                GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
                idlt.setString(f, label);
                int labelSize = (int) Math.round(idlt.getWidth());
                if (labelSize > size) {
                    size = labelSize;
                }
            }
            logger.log(Level.FINE, "Max label width: {0}", size);
            return size;
        } catch (InconvertibleUnitsException ex) {
            return 10;
        }
    }

    /** 
     * calculate the biggest label width
     * @param fm the font metrics.
     * @deprecated use getMaxLabelWidth()
     * @see #getMaxLabelWidth() 
     * @return the width in pixels of the widest label.
     */
    protected int getMaxLabelWidth(FontMetrics fm) {
        try {
            TickVDescriptor ticks = getTickV();
            DatumVector tickv = ticks.tickV;
            int size = Integer.MIN_VALUE;
            Graphics g = this.getGraphics();
            for (int i = 0; i < tickv.getLength(); i++) {
                String label = tickFormatter(tickv.get(i));
                GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
                idlt.setString(g, label);
                int labelSize = (int) Math.round(idlt.getWidth());
                if (labelSize > size) {
                    size = labelSize;
                }
            }
            return size;
        } catch (InconvertibleUnitsException ex) {
            return 10;
        }
    }

    /** 
     * reset bounds (and unused transform), invalidate the tickV, etc.
     */
    @Override
    public void resize() {
        resetTransform();
        if ( getFont()==null ) {
            return;
        }
        this.parentHeight= getParent().getHeight();
        this.parentWidth= getParent().getWidth();
        Rectangle oldBounds = this.getBounds();
        Rectangle newBounds = getAxisBounds();
        
        setBounds(newBounds);
        invalidate();
        TickVDescriptor ltickV= tickV;
        if (ltickV == null || ltickV.tickV.getUnits().isConvertibleTo(getUnits())) {
            validate();
        }
        firePropertyChange(PROP_BOUNDS, oldBounds, newBounds);
    }

    /**
     * calculate the bounds of the labels.  This should including regions that 
     * the labels could occupy if the axis were panned, so that result doesn't 
     * change during panning.
     * @param bounds the bounds which will be enlarged, or null.
     * @return Rectangle in the canvas coordinate frame.
     */
    protected Rectangle getLabelBounds(Rectangle bounds) {
        TickVDescriptor ltickV= this.getTickV();
        DatumRange dr= getDatumRange();

        if ( ltickV==null || !ltickV.tickV.getUnits().isConvertibleTo(getUnits() ) ) {
            logger.fine("tickV cannot be used because of units.");
            return bounds;
        }

        String[] labels = tickFormatter( ltickV.tickV, dr );

        GrannyTextRenderer gtr = GraphUtil.newGrannyTextRenderer();

        Font labelFont = this.getLabelFont();
        Font font= this.getFont();

        double dmin, dmax;
        if ( isHorizontal() ) {
            dmin= getColumn().getDMinimum();
            dmax= getColumn().getDMaximum();
        } else {
            dmin= getRow().getDMinimum();
            dmax= getRow().getDMaximum();
        }

        if ( font==null ) return bounds;
        FontMetrics fm= getFontMetrics( font );
        int fontDecent= fm.getMaxDescent();
        if ( fontDecent<0 ) {
            fontDecent= 5-fontDecent;
            logger.finest("negative font descent");
        }

        int axisOffsetPx= getAxisOffsetPixels();
        
        DatumVector ticks = ltickV.tickV;
        for (int i = 0; i < labels.length; i++) {
            Datum d = ticks.get(i);
            if (DatumRangeUtil.sloppyContains(dr, d)) {
                gtr.setString(font, labels[i]);
                Rectangle rmin = gtr.getBounds();
                Rectangle rmax = new Rectangle(rmin);  // same bound, but positioned at the axis max.
                double flw = gtr.getLineOneWidth();

                int tick_label_gap = tickLen/2; //getFontMetrics(getTickLabelFont()).stringWidth(" ");
                if ( tick_label_gap<5 ) tick_label_gap= TICK_LABEL_GAP_MIN;
                int space= tick_label_gap;

                int zeroOrPosTickLen= Math.max(0,tickLen);
                if (isHorizontal()) {
                    if (getOrientation() == BOTTOM) {
                        rmin.translate((int) (dmin - flw / 2), getRow().bottom() + space + zeroOrPosTickLen + labelFont.getSize() + axisOffsetPx );
                        rmax.translate((int) (dmax - flw / 2), getRow().bottom() + space + zeroOrPosTickLen + labelFont.getSize() + axisOffsetPx );
                    } else {
                        rmin.translate((int) (dmin - flw / 2), getRow().top() - space - zeroOrPosTickLen - (int) ( rmin.getHeight()+rmin.y ) ) ;
                        rmax.translate((int) (dmax - flw / 2), getRow().top() - space - zeroOrPosTickLen - (int) ( rmax.getHeight()+rmax.y ) );
                    }
                    if ( bounds==null ) bounds= rmin;
                    bounds.add(rmin);
                    bounds.add(rmax);
                } else {
                    double delta= gtr.getAscent() - gtr.getHeight() / 2;
                    if (getOrientation() == LEFT) {
                        rmin.translate(-(int) rmin.getWidth() - space - zeroOrPosTickLen + getColumn().left() - axisOffsetPx,
                                (int) dmin - (int) delta ); // note that gtr.getBounds() already contains the ascent.
                        rmax.translate(-(int) rmax.getWidth() - space - zeroOrPosTickLen + getColumn().left() - axisOffsetPx,
                                (int) (dmax + fontDecent + 3 ) ); // 3 is fudge
                    } else {
                        rmin.translate( space + zeroOrPosTickLen + getColumn().right() + axisOffsetPx, 
                                (int) dmin + (int)delta );
                        rmax.translate( space + zeroOrPosTickLen + getColumn().right() + axisOffsetPx, 
                                (int) (dmax + fontDecent + 3 )); // 3 is fudge
                    }
                    if ( bounds==null ) bounds= rmin;
                    bounds.add(rmin);
                    bounds.add(rmax);
                }
            }
        }
        return bounds;
    }

    /**
     * Calculate the rectangle that bounds the axis including its labels.  
     * When the axis is drawn on both sides of the plot, this rectangle will 
     * extend across the plot.
     * @return Rectangle containing the axes and its labels.
     */
    public Rectangle getAxisBounds() {
        Rectangle bounds;

        try {
            updateTickLength();
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }

        if (isHorizontal()) {
            // note ephemeris/TCA is calculated below
            bounds = getHorizontalAxisBounds();
        } else {
            bounds = getVerticalAxisBounds();
        }
        
        if (getOrientation() == BOTTOM && isVisible() && isTickLabelsVisible()) {
            QDataSet ltcaData= tcaData;
            String[] ltcaLabels= this.tcaLabels.split(";");
            
            if (drawTca && ( tcaRows>=0 || ltcaLabels.length>1 || ltcaData != null && ltcaData.length() != 0 ) ) {
                int DMin = getColumn().getDMinimum();
                Font tickLabelFont = getTickLabelFont();
                int tick_label_gap_2023 = getFontMetrics(tickLabelFont).stringWidth(" ");
                int lines= tcaRows>=0 ? tcaRows : ( getTickLines() - 1 );
                int tcaHeight = (tickLabelFont.getSize() + getLineSpacing()) * lines;
                int maxLabelWidth = getMaxLabelWidth();
                bounds.height += tcaHeight;
                blLabelRect.height += tcaHeight;
                if (blTitleRect != null) {
                    blTitleRect.y += tcaHeight;
                }
                GrannyTextRenderer idlt = GraphUtil.newGrannyTextRenderer();
                idlt.setString(tickLabelFont, "SCET");
                int tcaLabelWidth = (int) Math.floor(idlt.getWidth() + 0.5);
                QDataSet bds=null;
                if ( ltcaData!=null ) bds= (QDataSet) ltcaData.property(QDataSet.BUNDLE_1);
                if ( bds!=null && lines>bds.length() ) {
                    lines= bds.length();
                }
                for (int i = 0; i < lines; i++) {
                    String ss;
                    if ( bds==null ) {
                        if ( ltcaLabels.length==1 && ltcaLabels[0].trim().equals("") ) {
                            ss= "???";
                        } else {
                            ss= i<ltcaLabels.length ? ltcaLabels[i] :  "???";
                        }
                    } else {
                        if ( ltcaLabels.length==1 && ltcaLabels[0].trim().equals("") ) {
                            ss= (String) bds.property( QDataSet.LABEL, i );
                            if ( ss==null ) {
                                ss= (String) bds.property( QDataSet.NAME, i );
                            }
                        } else {
                            ss= i<ltcaLabels.length ? ltcaLabels[i] :  "???";
                        }
                        if ( ss==null ) {
                            ss= "   ";
                        }
                        if ( ss.contains("%{") ) {
                            Units u= (Units)bds.property( QDataSet.UNITS, i );
                            ss= resolveString( ss, "UNITS", u.toString() );
                        }
                    }
                    if ( ss==null ) ss= "   ";
                    idlt.setString( tickLabelFont, ss );
                    int width = (int) Math.floor(idlt.getWidth() + 0.5);
                    tcaLabelWidth = Math.max(tcaLabelWidth, width);
                }
                FontMetrics tickLabelFontMetrics = getFontMetrics(tickLabelFont);                
                int rightEdgeGap = tickLabelFontMetrics.stringWidth("0000") + tick_label_gap_2023; // rightEdgeGap is the extra amount shifted to make room for numbers
                //rightEdgeGap = tickLabelFontMetrics.stringWidth("00");// + tick_label_gap_2023;  
            
                if (tcaLabelWidth > 0) {
                    tcaLabelWidth += rightEdgeGap;
                    int tcaLabelSpace = DMin - tcaLabelWidth - tick_label_gap_2023;
                    int minX = Math.min(tcaLabelSpace - maxLabelWidth / 2, bounds.x);
                    int maxX = bounds.x + bounds.width;
                    bounds.x = minX;
                    bounds.width = maxX - minX;
                    blLabelRect.x = minX;
                    blLabelRect.width = maxX - minX;
                }
            }
        }

        return bounds;
    }
    
    /**
     * return the number of lines that the ticks use, considering the ephemeris (TCAs) loaded.
     * @return the number of lines that the ticks use.
     */
    public int getTickLines() {
        QDataSet ltcaData= tcaData;
        String ltcaLabels= tcaLabels;
        boolean ldrawTca= drawTca;
        if ( ldrawTca && ltcaData!=null ) {
            int ntca= ltcaData.length();
            int tcaLines= Math.min( MAX_TCA_LINES, Math.max( ltcaData.length(ntca-1), Math.max( ltcaData.length(ntca/2), ltcaData.length(0) ) ) );
            return 1+tcaLines;
        } else if ( ldrawTca && ltcaLabels.trim().length()>0 ) {
            String[] ss= ltcaLabels.split(";");
            return 1+ ss.length;
        } else {
            return 2;
        }
    }

    private Rectangle getHorizontalAxisBounds() {

        boolean bottomTicks = (orientation == BOTTOM || oppositeAxisVisible);  // are there ticks?
        boolean bottomTickLabels = (orientation == BOTTOM && tickLabelsVisible);// are the tick labels visible?
        boolean bottomLabel = (bottomTickLabels && !axisLabel.equals(""));      // is there an axis label?
        boolean topTicks = (orientation == TOP || oppositeAxisVisible);
        boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
        boolean topLabel = (topTickLabels && !axisLabel.equals(""));

        int axisOffsetPx= getAxisOffsetPixels();

        int topPosition = getRow().getDMinimum();
        int bottomPosition = getRow().getDMaximum() + axisOffsetPx;
        DasDevicePosition range = getColumn();
        int DMax = range.getDMaximum();
        int DMin = range.getDMinimum();
        int DWidth = DMax - DMin;
        
        Rectangle bounds;

        // start with the bounds of the base line.
        if (bottomTicks) {
            if (blLineRect == null) {
                blLineRect = new Rectangle();
            }
            blLineRect.setBounds(DMin, bottomPosition, DWidth + 1, 1);
        }
        if (topTicks) {
            if (trLineRect == null) {
                trLineRect = new Rectangle();
            }
            trLineRect.setBounds(DMin, topPosition, DWidth + 1, 1);
        }
        
        if ( reference.length()>0 ) {
            if ( blLineRect!=null ) {
                blLineRect.add( DMin, topPosition );
                blLineRect.add( DMin, bottomPosition );
            }
        }

        double lineThicknessDouble= getLineThicknessDouble(lineThickness);
                
        // length of the ticks, negative if pointing into the plot
        int tickLength= tickLen + ( tickLen<0 ? -1 : 1 ) *(int)( lineThicknessDouble / 2 );
        
        //Add room for ticks
        if (bottomTicks) {
            int x = DMin;
            int y = bottomPosition + 1 - Math.max( -tickLength, 0 );
            int width = DWidth;
            int height = Math.abs( tickLength );
            //The last tick is at position (x + width), so add 1 to width
            if ( isVisible() ) {
                blTickRect = setRectangleBounds(blTickRect, x, y, width + 1, height );
            } else {
                blTickRect = setRectangleBounds(blTickRect, x, y, width + 1, 1 );
            }
        }
        if (topTicks) {
            int x = DMin;
            int y = topPosition - Math.max( 0, tickLength + (int)(lineThicknessDouble/2) );
            int width = DWidth;
            int height = Math.abs( tickLength + (int)(lineThicknessDouble/2) );
            //The last tick is at position (x + width), so add 1 to width
            if ( isVisible() ) {
                trTickRect = setRectangleBounds(trTickRect, x, y, width + 1, height );
            } else {
                trTickRect = setRectangleBounds(trTickRect, x, y, 1, height );
            }
            
        }
        //int maxLabelWidth = getMaxLabelWidth();
        //int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");

        if (bottomTickLabels) {
            blLabelRect = getLabelBounds(new Rectangle(DMin, blTickRect.y, DWidth, 10));
            if ( labelOffset.length()>0 && axisLabel.length()>0 ) {
                blLabelRect.y= (bottomPosition) + (int)DasDevicePosition.parseLayoutStr( labelOffset, getEmSize(), 0, 0 );
            }
        }
        if (topTickLabels) {
            trLabelRect = getLabelBounds(new Rectangle(trTickRect.x, topPosition-10, DWidth, 10));
            if ( labelOffset.length()>0 && axisLabel.length()>0 ) {
                trLabelRect.y= (bottomPosition) - (int)DasDevicePosition.parseLayoutStr( labelOffset, getEmSize(), 0, 0 );
            }
        }

        //Add room for the axis label
        Font labelFont = getLabelFont();

        GrannyTextRenderer gtr = GraphUtil.newGrannyTextRenderer();
        gtr.setString(labelFont, getLabel());
        int labelSpacing = (int) gtr.getHeight() + labelFont.getSize() / 2;

        boolean v= isVisible();

        if (bottomLabel && v ) {
            int x = DMin;
            int y = blLabelRect.y + blLabelRect.height;
            int width = DMax - DMin;
            int height = labelSpacing;
            blTitleRect = setRectangleBounds(blTitleRect, x, y, width, height);
        }
        if (topLabel && v ) {
            int x = DMin;
            int y = trLabelRect.y - labelSpacing;
            int width = DMax - DMin;
            int height = labelSpacing;
            trTitleRect = setRectangleBounds(trTitleRect, x, y, width, height);
        }

        bounds = new Rectangle((orientation == BOTTOM) ? blLineRect : trLineRect);
        if (bottomTicks && v ) {
            bounds.add(blLineRect);
            bounds.add(blTickRect);
        }
        if (bottomTickLabels && v ) {
            bounds.add(blLabelRect);
        }
        if (bottomLabel && v ) {
            bounds.add(blTitleRect);
        }
        if (topTicks && v ) {
            bounds.add(trLineRect);
            bounds.add(trTickRect);
        }
        if (topTickLabels && v ) {
            bounds.add(trLabelRect);
        }
        if (topLabel && v ) {
            bounds.add(trTitleRect);
        }

        //Add room for the scan buttons (if present)
        if (stepPrevious != null && stepNext != null) {
            Dimension prevSize = stepPrevious.getPreferredSize();
            Dimension nextSize = stepPrevious.getPreferredSize();
            int minX = Math.min(DMin - prevSize.width, bounds.x);
            int maxX = Math.max(DMax + nextSize.width, bounds.x + bounds.width);
            bounds.x = minX;
            bounds.width = maxX - minX;
        }

        return bounds;
    }

    private Rectangle getVerticalAxisBounds() {
        boolean leftTicks = (orientation == LEFT || oppositeAxisVisible);    // are there ticks?
        boolean leftTickLabels = (orientation == LEFT && tickLabelsVisible); // are the tick labels visible?
        boolean leftLabel = (orientation == LEFT && !axisLabel.equals(""));  // is there an axis label?
        boolean rightTicks = (orientation == RIGHT || oppositeAxisVisible);
        boolean rightTickLabels = (orientation == RIGHT && tickLabelsVisible);
        boolean rightLabel = (orientation == RIGHT && !axisLabel.equals(""));

        int axisOffsetPx= getAxisOffsetPixels();

        int leftPosition = getColumn().getDMinimum() - axisOffsetPx;
        int rightPosition = getColumn().getDMaximum() + axisOffsetPx;
        int DMax = getRow().getDMaximum();
        int DMin = getRow().getDMinimum();
        int DWidth = DMax - DMin;

        Rectangle bounds;

        if (leftTicks) {
            if (blLineRect == null) {
                blLineRect = new Rectangle();
            }
            blLineRect.setBounds(leftPosition, DMin, 1, DWidth + 1);
        }
        if (rightTicks) {
            if (trLineRect == null) {
                trLineRect = new Rectangle();
            }
            trLineRect.setBounds(rightPosition, DMin, 1, DWidth + 1);
        }

        if ( reference.length()>0 ) {
            if ( blLineRect!=null ) {
                blLineRect.add( rightPosition, DMax );
                blLineRect.add( leftPosition, DMin );
            }
        }
        
        double lineThicknessDouble= getLineThicknessDouble(lineThickness);
                
        int tickLength= tickLen + ( tickLen<0 ? -1 : 1 ) *(int)( lineThicknessDouble / 2 );
        
        //Add room for ticks
        if (leftTicks) {
            int x = leftPosition - Math.max( 0,tickLength );
            int y = DMin;
            int width = Math.abs( tickLength );
            int height = DWidth;
            //The last tick is at position (y + height), so add 1 to height
            blTickRect = setRectangleBounds(blTickRect, x, y, width, height + 1);
        }
        if (rightTicks) {
            int x = rightPosition + 1 + Math.min( 0,tickLength );
            int y = DMin;
            int width = Math.abs( tickLength );
            int height = DWidth;
            //The last tick is at position (y + height), so add 1 to height
            trTickRect = setRectangleBounds(trTickRect, x, y, width, height + 1);
        }

        //int maxLabelWidth = getMaxLabelWidth();
        //int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");

        //Add room for tick labels
        if (leftTickLabels) {
            //int x = blTickRect.x - (maxLabelWidth + tick_label_gap);
            //int y = DMin - tickLabelFont.getSize();
            //int width = maxLabelWidth + tick_label_gap;
            //int height = DMax - DMin + tickLabelFont.getSize() * 2;
            //blLabelRect = setRectangleBounds(blLabelRect, x, y, width, height);
            blLabelRect = getLabelBounds(new Rectangle(blTickRect.x - 10, DMin, 10, DWidth));
            if ( labelOffset.length()>0 && axisLabel.length()>0 ) {
                blLabelRect.x = (leftPosition+1) - (int)DasDevicePosition.parseLayoutStr( labelOffset, getEmSize(), DWidth, 0 );
            }
            if ( leftXOverride != null ) blLabelRect.x = leftXOverride; // deprecated.
        } else if ( leftLabel ) {
            blLabelRect = getLabelBounds(new Rectangle( getColumn().getDMinimum(), DMin, 1, DWidth));
        } else {
           blLabelRect = blTickRect;
        }
        if (rightTickLabels) {
            trLabelRect = getLabelBounds(new Rectangle(trTickRect.x + trTickRect.width, DMin, 10, DWidth));
            if ( labelOffset.length()>0 && axisLabel.length()>0 ) {
                trLabelRect.width = (rightPosition) + (int)DasDevicePosition.parseLayoutStr( labelOffset, getEmSize(), DWidth, 0 ) - rightPosition;
            }
        //int x = trTickRect.x + trTickRect.width;
        //int y = DMin - tickLabelFont.getSize();
        //int width = maxLabelWidth + tick_label_gap;
        //int height = DMax - DMin + tickLabelFont.getSize() * 2;
        //trLabelRect = setRectangleBounds(trLabelRect, x, y, width, height);
        } else {
            trLabelRect = trTickRect;
        }

        //Add room for the axis label
        Font labelFont = getLabelFont();

        GrannyTextRenderer gtr = GraphUtil.newGrannyTextRenderer();
        gtr.setString(labelFont, getLabel());
        int labelSpacing = (int) gtr.getHeight() + labelFont.getSize() / 2;
        if (leftLabel) {
            int x = blLabelRect.x - labelSpacing;
            int y = DMin;
            int width = labelSpacing;
            int height = DWidth;
            blTitleRect = setRectangleBounds(blTitleRect, x, y, width, height);
        }
        if (rightLabel) {
            int x = trLabelRect.x + trLabelRect.width;
            int y = DMin;
            int width = labelSpacing;
            int height = DWidth;
            trTitleRect = setRectangleBounds(trTitleRect, x, y, width, height);
        }

        boolean v= isVisible();

        bounds = new Rectangle((orientation == LEFT) ? blLineRect : trLineRect);
        if (leftTicks && v ) {
            bounds.add(blLineRect);
            bounds.add(blTickRect);
        }
        if (leftTickLabels && v ) {
            bounds.add(blLabelRect);
        }
        if (leftLabel && v ) {
            bounds.add(blTitleRect);
        }
        if (rightTicks && v ) {
            bounds.add(trLineRect);
            bounds.add(trTickRect);
        }
        if (rightTickLabels && v ) {
            bounds.add(trLabelRect);
        }
        if (rightLabel && v ) {
            bounds.add(trTitleRect);
        }

        return bounds;
    }

    private static Rectangle setRectangleBounds(Rectangle rc, int x, int y, int width, int height) {
        if (rc == null) {
            return new Rectangle(x, y, width, height);
        } else {
            rc.setBounds(x, y, width, height);
            return rc;
        }
    }

    /** 
     * returns the orientation of the axis, which is one of BOTTOM,TOP,LEFT or RIGHT.
     * @return BOTTOM,TOP,LEFT or RIGHT.
     */
    public int getOrientation() {
        return orientation;
    }

    /** 
     * test if the axis is horizontal.
     * @return true if the orientation is BOTTOM or TOP.
     */
    public boolean isHorizontal() {
        return orientation == BOTTOM || orientation == TOP;
    }

    /** 
     * return the tick direction, 1=down or left, -1=up or right
     * @return 1=down or left, -1=up or right
     */
    public int getTickDirection() {
        return tickDirection;
    }

    /** 
     * return the formatter which converts Datum tick positions to a string.
     * @return the formatter which converts Datum tick positions to a string.
     */
    public DatumFormatter getDatumFormatter() {
        return datumFormatter;
    }

    /** 
     * Transforms a Datum in data coordinates to a horizontal or vertical
     * position on the parent canvas.
     * @param datum a data value
     * @return Horizontal or vertical position on the canvas.
     * @throws InconvertibleUnitsException 
     */
    public double transform(Datum datum) {
        return transform(datum.doubleValue(getUnits()), getUnits());
    }

    /**
     * this was never utilized, but at_m and at_b could be used to speed up 
     * transformation.
     * @param data the data location
     * @param units the units
     * @return the pixel location.
     */
    protected double transformFast(double data, Units units) {
        if (dataRange.isLog()) {
            if (data <= 0.) {
                data = dataRange.getMinimum() - 3; // TODO verify that dataRange.getMinimum() is log.
            } else {
                data = Math.log10(data);
            }
        }
        double result = at_m * data + at_b;
        return result;
    }

    /** 
     * Transforms a double in the given units in data coordinates to a horizontal or vertical
     * position on the parent canvas.
     * @param data a data value
     * @param units the units of the given data value.
     * @return Horizontal or vertical position on the canvas.
     * @throws InconvertibleUnitsException
     */
    public double transform(double data, Units units) {
        DasDevicePosition range;
        // TODO: consider optimization here
        if (isHorizontal()) {
            range = getColumn();
            return transform(data, units, range.getDMinimum(), range.getDMaximum());
        } else {
            range = getRow();
            return transform(data, units, range.getDMaximum(), range.getDMinimum());
        }
    }

    /** 
     * Transforms the rank 0 dataset a horizontal or vertical
     * position on the parent canvas.  This was introduced to support when
     * the rank 1 iterator would return QDataSets instead of doubles.
     * @param data a data value
     * @param units the units of the given data value.
     * @return Horizontal or vertical position on the canvas.
     * @throws InconvertibleUnitsException
     */
    public double transform( QDataSet data, Units units) {
        DasDevicePosition range;
        double d;
        if ( units==data.property(QDataSet.UNITS) ) {
            d= data.value();
        } else if ( units==Units.dimensionless && data.property(QDataSet.UNITS)==null ) {
            d= data.value(); 
        } else {
            UnitsConverter uc= SemanticOps.getUnits(data).getConverter(units);
            d= uc.convert(data.value());
        }
        if (isHorizontal()) {
            range = getColumn();
            return transform(d, units, range.getDMinimum(), range.getDMaximum());
        } else {
            range = getRow();
            return transform(d, units, range.getDMaximum(), range.getDMinimum());
        }
    }

    /** 
     * Transforms the rank 0 dataset a horizontal or vertical
     * position on the parent canvas. 
     * @param data a data value
     * @return Horizontal or vertical position on the canvas.
     * @throws InconvertibleUnitsException
     */
    public double transform( QDataSet data ) {
        DasDevicePosition range;
        double d=data.value();
        Units units= (Units)data.property(QDataSet.UNITS);
        if ( units==null ) units= Units.dimensionless;
        if (isHorizontal()) {
            range = getColumn();
            return transform(d, units, range.getDMinimum(), range.getDMaximum());
        } else {
            range = getRow();
            return transform(d, units, range.getDMaximum(), range.getDMinimum());
        }
    }    
    
    /**
     * Transforms a double in the given units in data coordinates to a horizontal or vertical
     * position on the parent canvas.
     * @param data a data value
     * @param units the units of the given data value.
     * @param dmin the axis minimum.
     * @param dmax the axis maximum.
     * @return  Horizontal or vertical position on the canvas.
     * @throws InconvertibleUnitsException
     */
    protected double transform(double data, Units units, int dmin, int dmax) {
        if (units != dataRange.getUnits()) {
            data = units.convertDoubleTo(dataRange.getUnits(), data);
        }

        double device_range = (dmax - dmin);
        double result;

        if (dataRange.isLog()) {
            if (data <= 0.) {
                data = -1e308;
            } else {
                data = Math.log10(data);
            }
        }

        double minimum = dataRange.getMinimum();
        double maximum = dataRange.getMaximum();
        double data_range = maximum - minimum;

        if (flipped) {
            result = dmax - (device_range * (data - minimum) / data_range);
        } else {
            result = (device_range * (data - minimum) / data_range) + dmin;
        }

        if (result > DEVICE_POSITIVE_LIMIT) {
            result = DEVICE_POSITIVE_LIMIT;
        }
        if (result < -DEVICE_POSITIVE_LIMIT) {
            result = -DEVICE_POSITIVE_LIMIT;
        }
        return result;
    }

    /**
     * get the range covered by the two points.  This allows clients to
     * get a range without having to worry about the flipped property.
     * 
     * @param idata1 pixel position on the axis, in the canvas frame.
     * @param idata2 pixel position on the axis, in the canvas frame.
     * @return DatumRange implied by the two pixel positions.
     * @throws InconvertibleUnitsException
     */
    public DatumRange invTransform( double idata1, double idata2 ) {
        Datum d1= invTransform(idata1);
        Datum d2= invTransform(idata2);
        if ( d1.lt(d2) ) {
            return new DatumRange(d1,d2);
        } else {
            return new DatumRange(d2,d1);
        }
    }
    
    /**
     * return the data location for the given pixel position.  Plot
     * coordinates have 0,0 at the upper-left hand corner of the screen.
     * @param idata the pixel location on the axis, in the canvas frame.
     * @return the data location.
     * @throws InconvertibleUnitsException
     */
    public Datum invTransform(double idata) {
        double data;
        DasDevicePosition range = (isHorizontal()
                ? (DasDevicePosition) getColumn()
                : (DasDevicePosition) getRow());

        double alpha = (idata - range.getDMinimum()) / (double) getDLength();
        if (!isHorizontal()) {
            alpha = 1.0 - alpha;
        }
        if (flipped) {
            alpha = 1.0 - alpha;
        }

        double minimum = dataRange.getMinimum();
        double maximum = dataRange.getMaximum();
        double data_range = maximum - minimum;
        data = data_range * alpha + minimum;

        double resolution = data_range / getDLength();
        if (dataRange.isLog()) {
            data = Math.pow(10,data);
            resolution = data * (Math.pow(10,resolution) - 1);
        }

        Datum result = Datum.create(data, dataRange.getUnits(), resolution);
         
        return result;
    }

    /**
     * return a label for this datum and visible range. This is intended
     * to be overridden to change behavior.  Note that both tickFormatter methods
     * should be overridden.
     * @param d the location
     * @return string, possibly with Granny control characters.
     */
    protected String tickFormatter(Datum d) {
        return datumFormatter.grannyFormat(d, d.getUnits());
    }

    /**
     * return the tick labels for these datums and visible range.  This is intended
     * to be overridden to change behavior.  Note that both tickFormatter methods
     * should be overridden.
     * @param tickV the ticks
     * @param datumRange the range
     * @return Strings, possibly with Granny control characters.
     */
    protected String[] tickFormatter(DatumVector tickV, DatumRange datumRange) {
        return datumFormatter.axisFormat(tickV, datumRange);
    }

    /**
     * target event handlers to reset the axis bounds.
     * @param e the event 
     */
    @Override
    public void dataRangeSelected(DataRangeSelectionEvent e) {
        this.setDataRange(e.getMinimum(), e.getMaximum());
    }

    /** 
     * Locates the next or previous tick starting at xDatum.
     *
     * @param xDatum  find the tick closest to this.
     * @param direction  -1 previous, 1 next, 0 closest
     * @param minor  find closest minor tick, major if false.
     * @return the closest tick.  If there is no tick in the given direction, then
     *   the behavior is undefined.
     * @deprecated Use getTickVDescriptor.findTick
     */
    public Datum findTick(Datum xDatum, double direction, boolean minor) {
        return getTickV().findTick(xDatum, direction, minor);
    }

    /** 
     * animate the change from one range to another.
     * @param min0 the initial range
     * @param max0 the initial range
     * @param min1 the final range
     * @param max1 the final range
     */
    private void animateChange(double min0, double max0, double min1, double max1) {

        if (animated && EventQueue.isDispatchThread()) {

            logger.fine("animate axis");

            boolean drawTca0 = isDrawTca();
            setDrawTca(false);

            long t0 = System.currentTimeMillis();
            long frames = 0;

            DataRange dataRange0 = dataRange;
            DataRange tempRange = DataRange.getAnimationDataRange(dataRange.getDatumRange(), dataRange.isLog());

            this.dataRange = tempRange;

            double transitionTime = 300; // millis
            //double transitionTime= 1500; // millis
            double alpha = (System.currentTimeMillis() - t0) / transitionTime;

            while (alpha < 1.0) {
                alpha = (System.currentTimeMillis() - t0) / transitionTime;

                final double[] aa = new double[]{0.0, 0.3, 0.85, 1.0};
                final double[] aa1 = new double[]{0.0, 0.05, 0.90, 1.0};

                double f1 = DasMath.findex(aa, alpha, 0);
                double a1 = DasMath.interpolate(aa1, f1);
                double a0 = 1 - a1;

                tempRange.setRange(min0 * a0 + min1 * a1, max0 * a0 + max1 * a1);

                //updateTickV();
                this.paintImmediately(0, 0, this.getWidth(), this.getHeight());

                if (dasPlot != null) {
                    dasPlot.paintImmediately(0, 0, dasPlot.getWidth(), dasPlot.getHeight());
                }
                frames++;
            }

            logger.log(Level.FINE, "animation frames/sec= {0}", (1000. * frames / transitionTime));
            setDrawTca(drawTca0);

            this.dataRange = dataRange0;
        }
    }

    /** 
     * reset the transform and update the tick locations.
     */
    @Override
    protected void updateImmediately() {
        super.updateImmediately();
        logger.log(Level.FINE, "updateImmadiately{0} {1}", new Object[]{getDatumRange(), isLog()});
        resetTransform();
        try {
            updateTickV();
        } catch ( InconvertibleUnitsException ex ) {
            // sometimes the units are changed while the ticks are being calculated.  This whole system needs review, but for now, avoid the RTE.
            updateTickV();
        }
    }

    /**
     * true if the tick labels should be drawn.
     * @return true if the tick labels should be drawn.
     */
    public boolean isTickLabelsVisible() {
        return tickLabelsVisible;
    }

    /** 
     * set true if the tick labels should be drawn.
     * @param b  true if the tick labels should be drawn.
     */
    public void setTickLabelsVisible(boolean b) {
        if (tickLabelsVisible == b) {
            return;
        }
        boolean oldValue = tickLabelsVisible;
        tickLabelsVisible = b;
        update();
        firePropertyChange("tickLabelsVisible", oldValue, b);
    }

    /**
     * perform actions necessary when starting the component, such as handling TCA.
     */
    @Override
    protected void installComponent() {
        super.installComponent();
        QFunction ltcaFunction= this.tcaFunction; 
        if ( ltcaFunction!=null ) {
            maybeStartTCATimer();
        }
    }
  
    /**
     * remove the component row and column update listener.
     */
    @Override
    protected void uninstallComponent() {
        super.uninstallComponent();
        tcaTimer= null; // Thanks, Ed!
    }


    /**
     * create another axis that follows this axis.  It will have the same
     * range and orientation.
     * @return attached axis.
     */
    public DasAxis createAttachedAxis() {
        DasAxis result= new DasAxis(this.dataRange, this.getOrientation());
        result.setScanRange( this.getScanRange() );
        return result;
    }

    /**
     * create another axis that follows this axis.  It will have the same
     * range.
     * @param orientation  the position relative to a plot, one of DasAxis.TOP, DasAxis.BOTTOM, DasAxis.LEFT, DasAxis.RIGHT
     * @return attached axis.
     */
    public DasAxis createAttachedAxis(int orientation) {
        DasAxis result= new DasAxis(this.dataRange, orientation);
        result.setScanRange( this.getScanRange() );
        return result;
    }

    /**
     * set the plot that will get updates
     * @param p plot object.
     */
    public void setPlot(DasPlot p) {
        dasPlot = p;
    }


    /**
     * scan to the previous interval.  If we were looking at a day with fuzz, then
     * scan to the previous day.
     */
    public void scanPrevious() {
        DatumRange dr= getDatumRange();
        if ( dataRange.isLog() ) {
            dr= DatumRangeUtil.rescaleLog( dr, -1., 0 );
        } else {
            dr= dr.previous();
        }
        setDatumRange( dr );
    }

    /**
     * scan to the next interval.  If we were looking at a day with fuzz, then
     * scan to the next day.
     */
    public void scanNext() {
        DatumRange dr= getDatumRange();
        if ( dataRange.isLog() ) {
            dr= DatumRangeUtil.rescaleLog( dr, 1., 2. );
        } else {
            dr= getDatumRange().next();
        }
        setDatumRange( dr );
    }

    /** 
     * get the region containing the axis.
     * @return the region containing the axis.
     */
    @Override
    public Shape getActiveRegion() {
        Rectangle primaryBounds = primaryInputPanel.getBounds();
        primaryBounds.translate(getX(), getY());
        if (oppositeAxisVisible) {
            Rectangle secondaryBounds = secondaryInputPanel.getBounds();
            secondaryBounds.translate(getX(), getY());
            GeneralPath path = new GeneralPath(primaryBounds);
            path.setWindingRule(GeneralPath.WIND_EVEN_ODD);
            path.append(secondaryBounds, false);
            return path;
        } else {
            return primaryBounds;
        }
    }

    /**
     * Adds a MouseWheelListener to the DasAxis.  Special care must be taken
     * with the DasAxis, because it is composed of two sub panels, and their
     * parent panel (this), must not recieve the events.  (This is because
     * the DasPlot between them should get the events, and the DasPlot does
     * not have a simple rectangular boundary.
     * @param l the listener
     */
    @Override
    public void addMouseWheelListener(MouseWheelListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.addMouseWheelListener(l);
        secondaryInputPanel.addMouseWheelListener(l);
    }

    /**
     * remove mouse wheel listener.  
     * @param l the listener
     * @see #maybeInitializeInputPanels() 
     */
    @Override
    public void removeMouseWheelListener(MouseWheelListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.removeMouseWheelListener(l);
        secondaryInputPanel.removeMouseWheelListener(l);
    }

    /**
     * add mouse wheel listener.  
     * @param l the listener
     * @see #maybeInitializeInputPanels() 
     */
    @Override
    public void addMouseListener(MouseListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.addMouseListener(l);
        secondaryInputPanel.addMouseListener(l);
    }

    /**
     * remove mouse motion listener.  
     * @param l the listener
     * @see #maybeInitializeInputPanels() 
     */
    @Override
    public void removeMouseListener(MouseListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.removeMouseListener(l);
        secondaryInputPanel.removeMouseListener(l);
    }

    /** 
     * add mouse motion listener.  
     * @param l the listener
     * @see #maybeInitializeInputPanels() 
     */
    @Override
    public void addMouseMotionListener(MouseMotionListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.addMouseMotionListener(l);
        secondaryInputPanel.addMouseMotionListener(l);
    }

    /** 
     * remove mouse motion listener.  
     * @param l the listener
     * @see #maybeInitializeInputPanels() 
     */
    @Override
    public void removeMouseMotionListener(MouseMotionListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.removeMouseMotionListener(l);
        secondaryInputPanel.removeMouseMotionListener(l);
    }

    /**
     * call back for time range selection event.
     * @param e the event
     */
    @Override
    public void timeRangeSelected(TimeRangeSelectionEvent e) {
        if (e.getSource() != this && !e.equals(lastProcessedEvent)) {
            setDatumRange(e.getRange()); // setDatumRange fires the event
            lastProcessedEvent = e;
        }
    }

    /** 
     * Registers TimeRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addTimeRangeSelectionListener(org.das2.event.TimeRangeSelectionListener listener) {
        if (timeRangeListenerList == null) {
            timeRangeListenerList = new javax.swing.event.EventListenerList();
        }
        timeRangeListenerList.add(org.das2.event.TimeRangeSelectionListener.class, listener);
    }

    /** 
     * Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(org.das2.event.TimeRangeSelectionListener listener) {
        timeRangeListenerList.remove(org.das2.event.TimeRangeSelectionListener.class, listener);
    }

    /** 
     * Notifies all registered listeners about the event.
     * @param event The event to be fired
     */
    private synchronized void fireTimeRangeSelectionListenerTimeRangeSelected(TimeRangeSelectionEvent event) {
        if (timeRangeListenerList == null) {
            return;
        }
        Object[] listeners = timeRangeListenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == org.das2.event.TimeRangeSelectionListener.class) {
                String logmsg = "fire event: " + this.getClass().getName() + "-->" + listeners[i + 1].getClass().getName() + " " + event;
                DasLogger.getLogger(DasLogger.GUI_LOG).fine(logmsg);
                ((org.das2.event.TimeRangeSelectionListener) listeners[i + 1]).timeRangeSelected(event);
            }
        }
    }

    private static final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\([eEfF]\\d+.\\d+\\)");

    private static String format(double d, String f) {
        Matcher m = pattern.matcher(f);
        if (!m.matches()) {
            throw new IllegalArgumentException("\"" + f + "\" is not a valid format specifier");
        }
        int length = Integer.parseInt(f.substring(2, f.indexOf('.')));
        int fracLength = Integer.parseInt(f.substring(f.indexOf('.') + 1, f.indexOf(')')));
        char[] buf = new char[length];
        String result;
        if (f.charAt(1) == 'f' || f.charAt(1) == 'F') {
            int i = 0;
            while (i < length - fracLength - 2) {
                buf[i] = '#';
                i++;
            }
            buf[i] = '0';
            i++;
            buf[i] = '.';
            i++;
            while (i < length) {
                buf[i] = '0';
                i++;
            }
            DecimalFormat form = new DecimalFormat(new String(buf));
            result = form.format(d);
        } else {
            int i = 0;
            while (i < length - fracLength - 6) {
                buf[i] = '#';
                i++;
            }
            buf[i] = '0';
            i++;
            buf[i] = '.';
            i++;
            while (i < length - 5) {
                buf[i] = '0';
                i++;
            }
            buf[i] = 'E';
            buf[i + 1] = (d > -1.0 && d < 1.0 ? '-' : '+');
            buf[i + 2] = '0';
            buf[i + 3] = '0';
            java.text.DecimalFormat form = new java.text.DecimalFormat(new String(buf));
            result = form.format(d);
        }

        if (result.length() > length) {
            java.util.Arrays.fill(buf, '*');
            return new String(buf);
        }

        while (result.length() < length) {
            result = " " + result;
        }
        return result;
    }

    @Override
    public String toString() {
        String retValue;
        retValue = super.toString() + "(" + getUnits() + ")";
        return retValue;
    }

    protected class AxisLayoutManager implements LayoutManager {
        //NOOP

        /** TODO
         * @param name
         * @param comp
         */
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        /** TODO
         * @param parent
         */
        @Override
        public void layoutContainer(Container parent) {
            if (DasAxis.this != parent) {
                throw new IllegalArgumentException();
            }
            if (DasAxis.this.isHorizontal()) {
                horizontalLayout();
            } else {
                verticalLayout();
            }
            if ( tcaRows>=0 ) {
                Rectangle bounds = primaryInputPanel.getBounds();
                int tcaHeight = (getTickLabelFont().getSize() + getLineSpacing()) * Math.min( MAX_TCA_LINES, tcaRows );
                bounds.height += tcaHeight;
                primaryInputPanel.setBounds(bounds);
            } else if (drawTca && getOrientation() == BOTTOM && tcaData != null) {
                Rectangle bounds = primaryInputPanel.getBounds();
                int tcaHeight = (getTickLabelFont().getSize() + getLineSpacing()) * Math.min( MAX_TCA_LINES, tcaData.length(0));
                bounds.height += tcaHeight;
                primaryInputPanel.setBounds(bounds);
            }
        }

        /** TODO */
        protected void horizontalLayout() {
            int topPosition = getRow().getDMinimum() - 1;
            int bottomPosition = getRow().getDMaximum();
            int DMax = getColumn().getDMaximum();
            int DMin = getColumn().getDMinimum();


            boolean bottomTicks = (orientation == BOTTOM || oppositeAxisVisible);
            boolean bottomTickLabels = (orientation == BOTTOM && tickLabelsVisible);
            boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
            Rectangle bottomBounds;
            Rectangle topBounds;
            Font tickLabelFont = getTickLabelFont();
            int tickSize = tickLabelFont.getSize() * 2 / 3;
            //Initialize bounds rectangle

            bottomBounds = new Rectangle(DMin, bottomPosition, DMax - DMin + 1, 1);
            bottomBounds.height += tickSize;    //Add room for ticks
            
            topBounds = new Rectangle(DMin, topPosition, DMax - DMin + 1, 1);
            topBounds.height += tickSize;   //Add room for ticks
            topBounds.y -= tickSize;
            
            int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
            //Add room for tick labels
            if (bottomTickLabels) {
                assert bottomBounds!=null;
                bottomBounds.height += tickLabelFont.getSize() * 3 / 2 + tick_label_gap;
            }
            if (topTickLabels) {
                topBounds.y -= (tickLabelFont.getSize() * 3 / 2 + tick_label_gap);
                topBounds.height += tickLabelFont.getSize() * 3 / 2 + tick_label_gap;
            }

            Rectangle primaryBounds =   ( bottomTicks ? bottomBounds : topBounds);
            Rectangle secondaryBounds = ( bottomTicks ? topBounds : bottomBounds);

            assert primaryBounds!=null;
            
            primaryBounds.translate(-DasAxis.this.getX(), -DasAxis.this.getY());
            if (oppositeAxisVisible) {
                assert secondaryBounds!=null;
                secondaryBounds.translate(-DasAxis.this.getX(), -DasAxis.this.getY());
            }

            primaryInputPanel.setBounds(primaryBounds);
            if (oppositeAxisVisible) {
                secondaryInputPanel.setBounds(secondaryBounds);
            } else {
                secondaryInputPanel.setBounds(-100, -100, 0, 0);
            }

            if (stepPrevious != null && stepNext != null) {
                Dimension preferred = stepPrevious.getPreferredSize();
                int x = DMin - preferred.width - DasAxis.this.getX();
                int y = (orientation == BOTTOM ? bottomPosition : topPosition - preferred.height) - DasAxis.this.getY();
                stepPrevious.setBounds(x, y, preferred.width, preferred.height);
                preferred = stepNext.getPreferredSize();
                x = DMax - DasAxis.this.getX();
                stepNext.setBounds(x, y, preferred.width, preferred.height);
            }
        }

        /** TODO */
        protected void verticalLayout() {
            boolean leftTicks = (orientation == LEFT || oppositeAxisVisible);
            boolean leftTickLabels = (orientation == LEFT && tickLabelsVisible);
            boolean rightTickLabels = (orientation == RIGHT && tickLabelsVisible);
            int leftPosition = getColumn().getDMinimum() - 1;
            int rightPosition = getColumn().getDMaximum();
            int DMax = getRow().getDMaximum();
            int DMin = getRow().getDMinimum();
            Rectangle leftBounds;
            Rectangle rightBounds;
            Font tickLabelFont = getTickLabelFont();
            int tickSize = tickLabelFont.getSize() * 2 / 3;
            //Initialize bounds rectangle(s)
            leftBounds = new Rectangle(leftPosition, DMin, 1, DMax - DMin + 1);
            leftBounds.width += tickSize; //Add room for ticks
            leftBounds.x -= tickSize;

            rightBounds = new Rectangle(rightPosition, DMin, 1, DMax - DMin + 1);
            rightBounds.width += tickSize; //Add room for ticks 
                        
            int maxLabelWidth = getMaxLabelWidth();
            int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
            //Add room for tick labels
            if (leftTickLabels) {
                leftBounds.x -= (maxLabelWidth + tick_label_gap);
                leftBounds.width += maxLabelWidth + tick_label_gap;
            //bounds.y -= tickLabelFont.getSize();
            //bounds.height += tickLabelFont.getSize()*2;
            }
            if (rightTickLabels) {
                assert rightBounds!=null;
                rightBounds.width += maxLabelWidth + tick_label_gap;
            //bounds.y -= tickLabelFont.getSize();
            //bounds.height += tickLabelFont.getSize()*2;
            }

            Rectangle primaryBounds = ( leftTicks ? leftBounds : rightBounds);
            Rectangle secondaryBounds = ( leftTicks ? rightBounds : leftBounds);

            assert primaryBounds!=null;
            
            primaryBounds.translate(-DasAxis.this.getX(), -DasAxis.this.getY());
            if (oppositeAxisVisible) {
                assert secondaryBounds!=null;
                secondaryBounds.translate(-DasAxis.this.getX(), -DasAxis.this.getY());
            }

            primaryInputPanel.setBounds(primaryBounds);
            if (oppositeAxisVisible) {
                secondaryInputPanel.setBounds(secondaryBounds);
            } else {
                secondaryInputPanel.setBounds(-100, -100, 0, 0);
            }
        }

        /** TODO
         * @param parent
         * @return
         */
        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension();
        }

        /** TODO
         * @param parent
         * @return
         */
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension();
        }

        /** TODO
         * @param comp
         */
        @Override
        public void removeLayoutComponent(Component comp) {
        }
    }

    private void refreshScanButtons(boolean reset) {
        if ( stepNext==null ) return; // headless
        if ( scanRange!=null ) {
            if ( !scanRange.getUnits().isConvertibleTo(getDatumRange().getUnits()) ) {
                  scanRange=null;
            }
        }
        if ( reset || stepPrevious.hover ) {
            boolean t= ( scanRange==null || ( false ? scanRange.intersects(getDatumRange().next()) : scanRange.intersects(getDatumRange().previous()) ) );
            stepPrevious.hover= t;
        }
        if ( reset || stepNext.hover ) {
            boolean t= ( scanRange==null || ( true ? scanRange.intersects(getDatumRange().next()) : scanRange.intersects(getDatumRange().previous()) ) );
            stepNext.hover= t;
        }
    }

    /**
     * set the label for the popup button
     * @param label concise label
     * @param tooltip text for popup tooltip
     */
    public void setNextActionLabel( String label, String tooltip ) {
        if ( stepNext!=null ) {
            stepNext.setText(label);
            stepNext.setToolTipText(tooltip);
        }
    }
    
    /**
     * set the label for the popup button
     * @param label concise label
     * @param tooltip text for popup tooltip
     */
    public void setPreviousActionLabel( String label, String tooltip ) {
        if ( stepPrevious!=null ) {
            stepPrevious.setText(label);
            stepPrevious.setToolTipText(tooltip);
        }
    }

    private int repaintCount= 0;
    
    @Override
    public void repaint() {
        //repaintCount++;
        //System.err.println( "DasAxis repaintCount: "+repaintCount );
         super.repaint(); //To change body of generated methods, choose Tools | Templates.
    }
    
    private final class ScanButton extends JButton {

        private boolean hover;
        private boolean pressed;

        private boolean nextButton;
        /** TODO
         * @param text
         */
        public ScanButton(String text) {
            setOpaque(true);
            setContentAreaFilled(false);
            setText(text);
            setFocusable(false);
            nextButton= STEP_NEXT_LABEL.equals(text);

            setBorder(new CompoundBorder(
                    new LineBorder(Color.BLACK),
                    new EmptyBorder(2, 2, 2, 2)));
            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        setForeground(Color.LIGHT_GRAY);
                        pressed = scanRange==null 
                                || ( nextButton 
                                     ? scanRange.intersects(getDatumRange().next()) 
                                     : scanRange.intersects(getDatumRange().previous()) );
                        repaint();
                    }
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        setForeground(Color.BLACK);
                        pressed = false;
                        repaint();
                    }
                }
                @Override
                public void mouseEntered(MouseEvent e) {
                    //hover = scanRange==null 
                    //        || scanRange.width().value()==0 
                    //        || ( nextButton 
                    //             ? scanRange.intersects(getDatumRange().next()) 
                    //             : scanRange.intersects(getDatumRange().previous()) );
                    hover= true;
                    if ( !hover ) {
                        logger.finest("hover false");
                    }
                    repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        /** TODO
         * @param g
         */
        @Override
        protected void paintComponent(Graphics g) {
            if ( getCanvas().isPrintingThread() ) return;
            if (hover || pressed) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(Color.white);
                g2.fillRect(0, 0, getWidth(), getHeight());
                Object aaHint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                Object aaOn = RenderingHints.VALUE_ANTIALIAS_ON;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaOn);
                super.paintComponent(g2);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaHint);
            } else {
                logger.finer("not drawing step button");
            }
        }

        /** TODO
         * @param g
         */
        @Override
        protected void paintBorder(Graphics g) {
            if (hover || pressed) {
                super.paintBorder(g);
            }
        }
    }

    private void updateTickLength() throws ParseException {
        double[] pos = DasDevicePosition.parseLayoutStr(this.tickLenStr);
        if ( pos[0]==0 ) {
            this.tickLen = (int) ( Math.round( pos[1]*getEmSize() + pos[2] ) ); // make independent from row layout for initialization.
        } else {
            this.tickLen = (int) (Math.round( pos[0]*getRow().getHeight() + pos[1]*getEmSize() + pos[2] ));
        }
    }

    /**
     * get the tick length string, for example "0.66em"
     * @return the tick length string
     */
    public String getTickLength() {
        return this.tickLenStr;
    }

    /**
     * set the tick length string, for example
     * "0.33em" "5px" "-0.33em"
     * @param tickLengthStr the tick length string.
     */
    public void setTickLength( String tickLengthStr ) {
        this.tickLenStr= tickLengthStr;
        try {
            updateTickLength();
            resize();
            repaint();
        } catch (ParseException ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
        }
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
        firePropertyChange(PROP_LINETHICKNESS, oldLineThickness, lineThickness);
        repaint();
    }

    public static final String PROP_FLIPPED = "flipped";
    
    /**
     * return true if the axis is flipped.
     * @return true if the axis is flipped.
     */
    public boolean isFlipped() {
        return flipped;
    }

    /** 
     * flip over the axis.
     * @param b 
     */
    public void setFlipped(boolean b) {
        boolean oldFlipped= this.flipped;
        update();
        this.flipped = b;
        firePropertyChange(PROP_FLIPPED,oldFlipped,flipped);
    }
    
    /**
     * the formatString, or "" if default behavior.
     */
    protected String formatString = "";
    
    public static final String PROP_FORMAT = "format";

    /**
     * return the format string for each tick.
     * @return the format string for each tick.
     */
    public String getFormat() {
        return formatString;
    }
    
    /**
     * true means flip the right label so all vertical labels are facing the same direction.
     */
    protected boolean flipLabel = false;
    public static final String PROP_FLIPLABEL = "flipLabel";

    /**
     * true if the right vertical label should be flipped.
     * @return true if the right vertical label should be flipped.
     */
    public boolean isFlipLabel() {
        return flipLabel;
    }

    /**
     * true if the right vertical label should be flipped.
     * @param flipLabel true if the right vertical label should be flipped.
     */
    public void setFlipLabel(boolean flipLabel) {
        boolean oldFlipLabel = this.flipLabel;
        this.flipLabel = flipLabel;
        repaint();
        firePropertyChange(PROP_FLIPLABEL, oldFlipLabel, flipLabel);
    }

    /**
     * the formatter identified to work with the divider 
     */
    protected DatumFormatter dividerDatumFormatter = null;
    public static final String PROP_DIVIDERDATUMFORMATTER = "dividerDatumFormatter";

    public DatumFormatter getDividerDatumFormatter() {
        return dividerDatumFormatter;
    }

    public void setDividerDatumFormatter(DatumFormatter dividerDatumFormatter) {
        DatumFormatter oldDividerDatumFormatter = this.dividerDatumFormatter;
        this.dividerDatumFormatter = dividerDatumFormatter;
        firePropertyChange(PROP_DIVIDERDATUMFORMATTER, oldDividerDatumFormatter, dividerDatumFormatter);
    }

    protected DomainDivider minorTicksDomainDivider = null;
    public static final String PROP_MINORTICKSDOMAINDIVIDER = "minorTicksDomainDivider";

    /**
     * return the domain divider for minor ticks, or null.
     * @return  the domain divider for minor ticks, or null.
     */
    public DomainDivider getMinorTicksDomainDivider() {
        return minorTicksDomainDivider;
    }

    public void setMinorTicksDomainDivider(DomainDivider minorTicksDomainDivider) {
        DomainDivider oldMinorTicksDomainDivider = this.minorTicksDomainDivider;
        this.minorTicksDomainDivider = minorTicksDomainDivider;
        firePropertyChange(PROP_MINORTICKSDOMAINDIVIDER, oldMinorTicksDomainDivider, minorTicksDomainDivider);
    }

    protected DomainDivider majorTicksDomainDivider = null;
    public static final String PROP_MAJORTICKSDOMAINDIVIDER = "majorTicksDomainDivider";

    /**
     * return the domain divider for major ticks, or null.
     * @return  the domain divider for major ticks, or null.
     */
    public DomainDivider getMajorTicksDomainDivider() {
        return majorTicksDomainDivider;
    }

    public void setMajorTicksDomainDivider(DomainDivider majorTicksDomainDivider) {
        DomainDivider oldMajorTicksDomainDivider = this.majorTicksDomainDivider;
        this.majorTicksDomainDivider = majorTicksDomainDivider;
        firePropertyChange(PROP_MAJORTICKSDOMAINDIVIDER, oldMajorTicksDomainDivider, majorTicksDomainDivider);
    }

    protected boolean useDomainDivider = false;
    public static final String PROP_USEDOMAINDIVIDER = "useDomainDivider";

    /**
     * true if the domain divider should be used.  This is a new object that
     * locates ticks.
     * @return true if the domain divider should be used. 
     */
    public boolean isUseDomainDivider() {
        return useDomainDivider;
    }

    /**
     * true if the domain divider should be used.  This is a new object that
     * locates ticks.
     * @param useDomainDivider true if the domain divider should be used. 
     */
    public void setUseDomainDivider(boolean useDomainDivider) {
        boolean oldUseDomainDivider = this.useDomainDivider;
        this.useDomainDivider = useDomainDivider;
        if ( oldUseDomainDivider!=useDomainDivider ) {
            updateTickV();
        }
        firePropertyChange(PROP_USEDOMAINDIVIDER, oldUseDomainDivider, useDomainDivider);
    }
    
    private boolean lockDomainDivider = false;

    public static final String PROP_LOCKDOMAINDIVIDER = "lockDomainDivider";

    public boolean isLockDomainDivider() {
        return lockDomainDivider;
    }

    /**
     * don't allow the domain divider to adjust.
     * @param lockDomainDivider 
     */
    public void setLockDomainDivider(boolean lockDomainDivider) {
        boolean oldLockDomainDivider = this.lockDomainDivider;
        this.lockDomainDivider = lockDomainDivider;
        firePropertyChange(PROP_LOCKDOMAINDIVIDER, oldLockDomainDivider, lockDomainDivider);
    }


    /**
     * set the component visible or invisible. The axis bounds are updated.  So in addition to the JComponent visibility, this
     * also makes a useful property to completely hide the axis.  drawTickLabels hides the labels but still draws the ticks, this
     * completely hides the axis.  Note too that even though it is not visible, tick positions are still updated to support
     * drawing a grid on the plot.
     * @param aFlag
     */
    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        update();
    }

    /**
     * set a hint at the format string.  Examples include:<ul>
     *  <li> 0.000
     *  <li> $H:$M!c$Y-$j
     *  <li> 0
     *  <li> %f
     * </ul>
     * @param formatString
     */
    public void setFormat(String formatString) {
        try {
            String oldFormatString = this.formatString;
            this.formatString = formatString;
            if (formatString.equals("")) {
                setUserDatumFormatter(null);
            } else {
                if ( formatString.contains("$") && !formatString.contains("%") ) {
                    formatString= formatString.replaceAll("\\$","%");
                }
                setUserDatumFormatter(getUnits().getDatumFormatterFactory().newFormatter(formatString));
            }
            updateTickV();
            repaint();
            firePropertyChange(PROP_FORMAT, oldFormatString, formatString);
        } catch (ParseException e) {
            setUserDatumFormatter(null);
        }
    }

    private void resetTransform() {
        DasDevicePosition pos;
        if (isHorizontal()) {
            pos = getColumn();
            if ( pos==DasColumn.NULL) return;
        } else {
            pos = getRow();
            if ( pos==DasRow.NULL) return;
        }
        double dmin = pos.getDMinimum();
        double dmax = pos.getDMaximum();
        if (isFlipped()) {
            double t = dmin;
            dmin = dmax;
            dmax = t;
        }
        double[] at = GraphUtil.getSlopeIntercept(dataRange.getMinimum(), dmin, dataRange.getMaximum(), dmax);
        at_m = at[0];
        at_b = at[1];
    }

    /**
     * get an object to lock the axis for multiple operations.
     * @return 
     */
    public Lock mutatorLock() {
        return dataRange.mutatorLock();
    }

    /**
     * true if a lock is out and an object is rapidly mutating the object.
     * clients listening for property changes can safely ignore property
     * changes while valueIsAdjusting is true, as they should receive a
     * final propertyChangeEvent after the lock is released.  (note it's not
     * clear who is responsible for this.
     * See http://das2.org/wiki/index.php/Das2.valueIsAdjusting)
     * @return true if valueIsAdjusting
     */
    public boolean valueIsAdjusting() {
        return dataRange.valueIsAdjusting();
    }
}
