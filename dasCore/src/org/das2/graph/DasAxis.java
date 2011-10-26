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
import org.das2.dataset.DataSetUpdateEvent;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.VectorDataSet;
import org.das2.dataset.DataSetUpdateListener;
import org.das2.dataset.DataSetUtil;
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
import org.das2.util.DasExceptionHandler;
import org.das2.util.DasMath;
import org.das2.util.monitor.NullProgressMonitor;
import org.das2.DasApplication;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import javax.swing.border.*;

import java.awt.geom.GeneralPath;
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
import javax.management.ReflectionException;
import org.das2.DasException;
import org.das2.datum.DatumUtil;
import org.das2.datum.DomainDivider;
import org.das2.datum.DomainDividerUtil;
import org.das2.datum.UnitsConverter;
import org.das2.datum.UnitsUtil;
import org.virbo.dataset.ArrayDataSet;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.QFunction;
import org.virbo.dataset.SemanticOps;

/** 
 * One dimensional axis component that transforms data to device space and back, 
 * and provides controls for navigating the 1-D data space.
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

    private static final int MAX_TCA_LINES=10; // maximum number of TCA lines
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
    /**  */
    public static final int UP = 995;
    /**  */
    public static final int DOWN = 996;

    /* Constants defining the action commands and labels for the scan buttons. */
    private static final String SCAN_PREVIOUS_LABEL = "<< scan";
    private static final String SCAN_NEXT_LABEL = "scan >>";

    /* GENERAL AXIS INSTANCE MEMBERS */
    protected DataRange dataRange;

    public DatumFormatter getUserDatumFormatter() {
        return userDatumFormatter;
    }

    public void setUserDatumFormatter(DatumFormatter userDatumFormatter) {
        DatumFormatter old= this.userDatumFormatter;
        this.userDatumFormatter = userDatumFormatter;
        if ( old!=userDatumFormatter ) updateTickV();//TODO: this results in data read on event thread
            SwingUtilities.invokeLater( new Runnable() {
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
     * until we switch to java 1.5, use this lock object instead of
     * java.util.concurrent.lock
     */
    public interface Lock {

        public void lock();

        public void unlock();
    }
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
    private ScanButton scanPrevious;
    private ScanButton scanNext;

    /**
     * limits of the scan range.  Scan buttons will only be shown with within this range.  If not set, then there is no limit to range
     * and the buttons are always available.
     */
    private DatumRange scanRange;
    
    private boolean animated = ("on".equals(DasProperties.getInstance().get("visualCues")));
    /* Rectangles representing different areas of the axis */
    private Rectangle blLineRect;
    private Rectangle trLineRect;
    private Rectangle blTickRect;
    private Rectangle trTickRect;
    private Rectangle blLabelRect;
    private Rectangle trLabelRect;
    private Rectangle blTitleRect;
    private Rectangle trTitleRect;
    /** TODO: Currently under implemented! */
    private boolean flipped;
    /* TIME LOCATION UNITS RELATED INSTANCE MEMBERS */
    private javax.swing.event.EventListenerList timeRangeListenerList = null;
    private TimeRangeSelectionEvent lastProcessedEvent = null;
    /* TCA RELATED INSTANCE MEMBERS */
    private QFunction tcaFunction;
    private QDataSet tcaData = null;
    private String dataset = "";
    private boolean drawTca;
    public static final String PROPERTY_DATUMRANGE = "datumRange";
    /* DEBUGGING INSTANCE MEMBERS */
    private static boolean DEBUG_GRAPHICS = false;
    private static final Color[] DEBUG_COLORS;

    int tickLen= 0; // this is reset after sizing.
    String tickLenStr= "0.66em";

    final int TICK_LABEL_GAP_MIN= 4;  // minimum number of pixels to label

    static {
        if (DEBUG_GRAPHICS) {
            DEBUG_COLORS = new Color[]{
                        Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                        Color.GRAY, Color.CYAN, Color.MAGENTA, Color.YELLOW,};
        } else {
            DEBUG_COLORS = null;
        }
    }
    private int debugColorIndex = 0;
    private DasPlot dasPlot;
    private JMenu bookmarksMenu;
    private JMenu backMenu;
    private static final Logger logger = DasLogger.getLogger(DasLogger.GRAPHICS_LOG);

    /** TODO
     * @param min
     * @param max
     * @param orientation DasAxis.VERTICAL, DasAxis.HORIZONTAL, DasAxis.RIGHT, etc.
     */
    public DasAxis(Datum min, Datum max, int orientation) {
        this(min, max, orientation, false);
    }

    public DasAxis(DatumRange range, int orientation) {
        this(range.min(), range.max(), orientation);
    }

    /** TODO
     * @param min
     * @param max
     * @param orientation
     * @param log
     */
    public DasAxis(Datum min, Datum max, int orientation, boolean log) {
        this(orientation);
        dataRange = new DataRange(this, min, max, log);
        addListenersToDataRange(dataRange, dataRangePropertyListener);
        copyFavorites();
        copyHistory();
    }

    /** TODO
     * @param range
     * @param orientation
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
            scanNext.setEnabled( true );
            scanPrevious.setEnabled( true );
        }
        add(primaryInputPanel);
        add(secondaryInputPanel);
        try {
            this.updateTickLength();
        } catch (ParseException ex) {
            Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
        }
        // this doesn't fire
        this.addPropertyChangeListener( "font", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    updateTickLength();
                } catch (ParseException ex) {
                    Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    private void addListenersToDataRange(DataRange range, PropertyChangeListener listener) {
        range.addPropertyChangeListener(PROP_LOG, listener);
        range.addPropertyChangeListener("minimum", listener);
        range.addPropertyChangeListener("maximum", listener);
        range.addPropertyChangeListener(DataRange.PROPERTY_DATUMRANGE, listener);
        range.addPropertyChangeListener("history", listener);
        range.addPropertyChangeListener("favorites", listener);
    }

    public void addToFavorites(final DatumRange range) {
        dataRange.addToFavorites(range);
        copyFavorites();
    }

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

                public void actionPerformed(ActionEvent e) {
                    DasAxis.this.setDatumRange(r);
                }
            };
            JMenuItem menuItem = new JMenuItem(action);
            bookmarksMenu.add(menuItem);
        }

        bookmarksMenu.add( new JSeparator() );
        Action action = new AbstractAction("bookmark this range") {
            public void actionPerformed(ActionEvent e) {
                DasAxis.this.addToFavorites(DasAxis.this.getDatumRange());
            }
        };
        JMenuItem addItem = new JMenuItem(action);
        bookmarksMenu.add(addItem);

        bookmarksMenu.add( new JSeparator() );
        Action action2 = new AbstractAction("remove bookmark for range") {
            public void actionPerformed(ActionEvent e) {
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

                public void actionPerformed(ActionEvent e) {
                    dataRange.popHistory(ipop);
                    DasAxis.this.setDataRangePrev();
                }
            };
            JMenuItem menuItem = new JMenuItem(action);
            backMenu.add(menuItem);
            ii++;
        }
    }

    protected boolean enableHistory = true;
    public static final String PROP_ENABLEHISTORY = "enableHistory";

    public boolean isEnableHistory() {
        return enableHistory;
    }

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
            scanPrevious = new DasAxis.ScanButton(SCAN_PREVIOUS_LABEL);
            scanNext = new DasAxis.ScanButton(SCAN_NEXT_LABEL);
            ActionListener al = createScanActionListener();
            scanPrevious.addActionListener(al);
            scanNext.addActionListener(al);
            add(scanPrevious);
            add(scanNext);
        }
    }

    private ActionListener createScanActionListener() {
        return new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                DasLogger.getLogger(DasLogger.GUI_LOG).fine("event " + command);
                if (command.equals(SCAN_PREVIOUS_LABEL)) {
                    if ( scanRange==null || scanRange.intersects(getDatumRange().previous()) ) scanPrevious();
                } else if (command.equals(SCAN_NEXT_LABEL)) {
                    if ( scanRange==null || scanRange.intersects(getDatumRange().next()) ) scanNext();
                }
            }
        };
    }

    private PropertyChangeListener createDataRangePropertyListener() {
        return new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();
                Object oldValue = e.getOldValue();
                Object newValue = e.getNewValue();
                if (propertyName.equals(PROP_LOG)) {
                    update();
                    firePropertyChange(PROP_LOG, oldValue, newValue);
                } else if (propertyName.equals("minimum")) {
                    update();
                    firePropertyChange("dataMinimum", oldValue, newValue);
                } else if (propertyName.equals("maximum")) {
                    update();
                    firePropertyChange("dataMaximum", oldValue, newValue);
                } else if (propertyName.equals("favorites")) {
                    copyFavorites();
                } else if (propertyName.equals(DataRange.PROPERTY_DATUMRANGE)) {
                    update();
                    firePropertyChange(PROPERTY_DATUMRANGE, oldValue, newValue);
                } else if (propertyName.equals("history")) {
                    if (!dataRange.valueIsAdjusting()) {
                        copyHistory();
                    }
                }
                markDirty();
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

    /** TODO
     * @param orientation
     */
    public void setOrientation(int orientation) {
        boolean oldIsHorizontal = isHorizontal();
        setOrientationInternal(orientation);
        if (oldIsHorizontal != isHorizontal()) {
            installMouseModules();
        }
    }

    /* This is a private internal implementation for
     * {@link #setOrientation(int)}.  This method is provided
     * to avoid calling a non-final non-private instance method
     * from a constructor.  Doing so can create problems if the
     * method is overridden in a subclass.
     */
    private void setOrientationInternal(int orientation) {
        this.orientation = orientation;
        if (orientation == TOP) {
            setTickDirection(UP);
        } else if (orientation == BOTTOM) {
            setTickDirection(DOWN);
        } else if (orientation == LEFT) {
            setTickDirection(RIGHT);
        } else if (orientation == RIGHT) {
            setTickDirection(LEFT);
        } else {
            throw new IllegalArgumentException("Invalid value for orientation");
        }
    }

    public void setDatumRange(DatumRange dr) {
        //System.err.println("setDatumRange("+dr+")");
        if (getUnits().isConvertableTo(dr.getUnits())) {
            this.setDataRange(dr.min(), dr.max());
        } else {
            Units oldUnits = getUnits();
            this.resetRange(dr);
            firePropertyChange(PROP_UNITS, oldUnits, dr.getUnits());
        }

    }

    public DatumRange getDatumRange() {
        return dataRange.getDatumRange();
    }

    /*
     * @returns true is the range is acceptible, false otherwise.  This method
     * is overriden by DasLabelAxis.
     */
    protected boolean rangeIsAcceptable(DatumRange dr) {
        return dr.min().lt(dr.max());
    }

    /** TODO
     * @param minimum
     * @param maximum
     */
    public void setDataRange(Datum minimum, Datum maximum) {

        Units units = dataRange.getUnits();
        if (minimum.getUnits() != units) {
            minimum = minimum.convertTo(units);
            maximum = maximum.convertTo(units);
        }

        DatumRange newRange = new DatumRange(minimum, maximum);
        logger.fine("enter dasAxis.setDataRange( " + newRange + " )");

        if (!rangeIsAcceptable(newRange)) {
            logger.warning("invalid range ignored");
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

    /** TODO */
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

    /** TODO */
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

    /** TODO */
    public void setDataRangeZoomOut() {
        logger.fine("enter dasAxis.setDataRangeZoomOut()");
        double t1 = dataRange.getMinimum();
        double t2 = dataRange.getMaximum();
        double delta = t2 - t1;
        double min = t1 - delta;
        double max = t2 + delta;
        animateChange(t1, t2, min, max);
        DatumRange oldRange = dataRange.getDatumRange();
        if ( !DatumRangeUtil.isAcceptable( DatumRange.newDatumRange( min, max, getUnits() ), isLog() ) ) {
            System.err.println("zoom out limit");
            return;
        }
        dataRange.setRange(min, max);
        DatumRange newRange = dataRange.getDatumRange();
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange(PROPERTY_DATUMRANGE, oldRange, newRange);
    }

    /** TODO
     * @return
     */
    public DataRange getDataRange() {
        return this.dataRange;
    }

    /** TODO */
    protected void deviceRangeChanged() {
    }

    /** TODO
     * @return
     */
    public Datum getDataMinimum() {
        return dataRange.getDatumRange().min();
    }

    /** TODO
     * @return
     */
    public Datum getDataMaximum() {
        return dataRange.getDatumRange().max();
    }

    /*
     *
     */
    /**
     * This is the preferred method for getting the range of the axis.
     * @return a DatumRange indicating the range of the axis.
     * @deprecated use getDatumRange instead.
     */
    public DatumRange getRange() {
        return dataRange.getDatumRange();
    }

    /** TODO
     * @param units
     * @return
     */
    public double getDataMaximum(Units units) {
        return getDataMaximum().doubleValue(units);
    }

    /** TODO
     * @param units
     * @return
     */
    public double getDataMinimum(Units units) {
        return getDataMinimum().doubleValue(units);
    }

    /** TODO
     * @param max
     */
    public void setDataMaximum(Datum max) {
        dataRange.setMaximum(max);
        update();
    }

    /** TODO
     * @param min
     */
    public void setDataMinimum(Datum min) {
        dataRange.setMinimum(min);
        update();
    }

    /** TODO
     * @return
     */
    public boolean isLog() {
        return dataRange.isLog();
    }

    /** 
     * Set the axis to be log or linear.  If necessary, axis range will be 
     * adjusted to make the range valid.
     * @param log
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

    /** TODO
     * @return
     */
    public Units getUnits() {
        return dataRange.getUnits();
    }

    public void setUnits(Units newUnits) {
        dataRange.setUnits(newUnits);
    }

    /**
     * limit the scan buttons to operate within this range.
     * http://sourceforge.net/tracker/index.php?func=detail&aid=3059009&group_id=199733&atid=970682
     * @param range
     */
    public void setScanRange( DatumRange range ) {
        DatumRange old= this.scanRange;
        this.scanRange= range;
        firePropertyChange( PROP_SCAN_RANGE, old, range );
    }

    /**
     * get the limit the scan buttons, which may be null.
     */
    public DatumRange getScanRange(  ) {
        return this.scanRange;
    }

    /**
     * changes the units of the axis to a new unit.
     */
    public synchronized void resetRange(DatumRange range) {
        DatumRange oldRange= this.getDatumRange();
        if (range.getUnits() != this.getUnits()) {
            if (dasPlot != null) {
                dasPlot.invalidateCacheImage();
            }
            logger.finest("replaceRange(" + range + ")");
            dataRange.resetRange(range);
        } else {
            dataRange.setRange(range);
        }
        updateTickV();
        markDirty();
        firePropertyChange(PROPERTY_DATUMRANGE, oldRange, range);
        update();
    }

    /** TODO
     * @param visible
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

    /** TODO
     * @return
     */
    public boolean isOppositeAxisVisible() {
        return oppositeAxisVisible;
    }

    /** Mutator method for the title property of this axis.
     *
     * The title for this axis is displayed below the ticks for horizontal axes
     * or to left of the ticks for vertical axes.
     * @param t The new title for this axis
     */
    public void setLabel(String t) {
        if (t == null) {
            throw new NullPointerException("axis label cannot be null");
        }
        Object oldValue = axisLabel;
        axisLabel = t;
        update();
        firePropertyChange(PROP_LABEL, oldValue, t);
    }

    /**
     * Accessor method for the title property of this axis.
     *
     * @return A String instance that contains the title displayed
     *    for this axis, or <code>null</code> if the axis has no title.
     */
    public String getLabel() {
        return axisLabel;
    }

    /** Getter for property animated.
     * @return Value of property animated.
     */
    public boolean isAnimated() {
        return this.animated;
    }

    /** Setter for property animated.
     * @param animated new value of property animated.
     */
    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    public boolean getDrawTca() {
        return drawTca;
    }

    public void setDrawTca(boolean b) {
        boolean oldValue = drawTca;
        if (b && getOrientation() != BOTTOM) {
            throw new IllegalArgumentException("Vertical time axes cannot have annotations");
        }
        if (drawTca == b) {
            return;
        }
        drawTca = b;
        markDirty();
        update();
        firePropertyChange("showTca", oldValue, b);
    }

    public String getDataPath() {
        return dataset;
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
                    String arg= null;
                    if ( argPos==-1 ) {
                        className= dataset.substring(6);
                        result = (QFunction) Class.forName(className).newInstance();
                    } else {
                        className= dataset.substring(6,argPos);
                        arg= dataset.substring(argPos+1);
                        try {
                            result = (QFunction) Class.forName(className).getConstructor(String.class).newInstance(arg);
                        } catch ( Exception ex ) { //TODO: more precise
                            throw new DasException(ex);
                        }
                    }
                    
                } catch (InstantiationException ex) {
                    Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
                    ex.printStackTrace();
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
                    ex.printStackTrace();
                }
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
                ex.printStackTrace();
            }
        }
        return result;
    }
    
    /**
     *
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
        } else {
            try {
                tcaFunction= tcaFunction( dataset );
                if ( tcaFunction==null ) {
                    throw new IllegalArgumentException("unable to implement tca QFunction: "+dataset );
                }
                //tcaFunction = DataSetDescriptor.create(dataset);
            } catch (org.das2.DasException de) {
                DasExceptionHandler.handle(de);
            }
        }
        markDirty();
        update();
        firePropertyChange("dataPath", oldValue, dataset);
    }

    /** Add auxilary data to an axis (usually OrbitAttitude data for a time axis).
     * This function does the same thing as setDataPath, but with a different interface.
     * @param will be called upon to generate auillary data sets.  To avoid nonsensical
     * graphs the X axis for this dataset must be the same as the that handed to the
     * renderer.
     */
    public void setDataSetDescriptor(DataSetDescriptor dsdAux) {
        if (dsdAux == null) {
            throw new NullPointerException("null DataSetDescriptor not allowed");
        }

        //TODO: adapt old to new
         throw new IllegalArgumentException("need to implement");
    }

    /** Add auxilary data to an axis (usually OrbitAttitude data for a time axis).
     * This function does the same thing as setDataPath, but with a different interface.
     * @param will be called upon to generate auxillary data sets.  To avoid nonsensical
     * graphs the X axis for this dataset must be the same as the that handed to the
     * renderer.
     */
    public void setTcaFunction( QFunction f ) {
        QFunction oldF= this.tcaFunction;
        this.tcaFunction= f;

        markDirty();
        update();

        firePropertyChange("dataSetDescriptor", null, null );
        firePropertyChange("dataPath", null, null );
        firePropertyChange("tcaFunction", oldF, f );

    }

    /**
     * update the TCAs using the QFunction this.tcaFunction.  We get an example
     * input from the function, then call it for each tick.  We add the property
     * "CONTEXT_0" to be a BINS dataset indicating the overall range for the
     * read.
     */
    private void updateTCADataSet() {
        QFunction ltcaFunction= this.tcaFunction;
        if ( ltcaFunction==null ) return;
        
        logger.fine("updateTCADataSet");
        double[] tickV = getTickV().tickV.toDoubleArray(getUnits());
        tcaData = null;

        JoinDataSet ltcaData= new JoinDataSet(2);
        ArrayDataSet ex= ArrayDataSet.copy( ltcaFunction.exampleInput() );
        QDataSet bds= (QDataSet) ex.property(QDataSet.BUNDLE_0);
        Units tcaUnits;
        if ( bds==null ) {
            System.err.println("no bundle descriptor, dealing with it.");
            tcaUnits= (Units) ex.property( QDataSet.UNITS, 0 );
        } else {
            tcaUnits= (Units)bds.property( QDataSet.UNITS, 0 );
        }

        UnitsConverter uc;
        if ( !getUnits().isConvertableTo(tcaUnits) ) {
            System.err.println("tca units are not convertable");
            return;
        }
        uc= UnitsConverter.getConverter( getUnits(), tcaUnits );

        DatumRange context= getDatumRange(); // this may not contain all the ticks.
        context= DatumRangeUtil.union( context, getUnits().createDatum( uc.convert(tickV[0]) ) );
        context= DatumRangeUtil.union( context, getUnits().createDatum( uc.convert(tickV[tickV.length-1]) ) );
        ex.putProperty( QDataSet.CONTEXT_0, 0, org.virbo.dataset.DataSetUtil.asDataSet( context ) );
        QDataSet dx= org.virbo.dataset.DataSetUtil.asDataSet( getDatumRange().width().divide( getColumn().getWidth() ) );
        ex.putProperty( QDataSet.DELTA_PLUS, 0, dx );
        ex.putProperty( QDataSet.DELTA_MINUS, 0, dx );

        DDataSet dep0= DDataSet.createRank1(tickV.length);
        dep0.putProperty(QDataSet.UNITS,getUnits());

        QDataSet outDescriptor=null;

        for ( int i=0; i<tickV.length; i++ ) {
            ex.putValue( 0,uc.convert(tickV[i]) );
            QDataSet ticks= ltcaFunction.value(ex);
            if ( outDescriptor==null ) {
                outDescriptor= (QDataSet) ticks.property(QDataSet.BUNDLE_0);
            }
            ltcaData.join(ticks);
            dep0.putValue(i,tickV[i]);
        }
        ltcaData.putProperty( QDataSet.BUNDLE_1, outDescriptor );
        ltcaData.putProperty( QDataSet.DEPEND_0, dep0 );

        this.tcaData= ltcaData;
        
    }

    /** TODO
     * @return
     */
    public final int getDevicePosition() {
        if (orientation == BOTTOM) {
            return getRow().getDMaximum();
        } else if (orientation == TOP) {
            return getRow().getDMinimum();
        } else if (orientation == LEFT) {
            return getColumn().getDMinimum();
        } else {
            return getColumn().getDMaximum();
        }
    }

    /**
     * @return returns the length in pixels of the axis.
     */
    public int getDLength() {
        if (isHorizontal()) {
            return getColumn().getWidth();
        } else {
            return getRow().getHeight();
        }
    }

    /** TODO
     * @return
     */
    public DasAxis getMasterAxis() {
        return dataRange.getCreator();
    }

    /** TODO
     * @param axis
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

    /** TODO */
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

    /** TODO
     * @return
     */
    public boolean isAttached() {
        return this != getMasterAxis();
    }

    /** TODO
     * @return
     */
    public synchronized TickVDescriptor getTickV() {
        if (tickV == null) {
            updateTickV();
        }
        return tickV;
    }

    /**
     * Sets the TickVDescriptor for this axis.  If null is passed in, the
     * axis will put into autoTickV mode, where the axis will attempt to
     * determine ticks using an appropriate algortithm.
     *
     * @param tickV the new ticks for this axis, or null
     */
    public synchronized void setTickV(TickVDescriptor tickV) {
        checkTickV(tickV);
        this.tickV = tickV;
        if (tickV == null) {
            autoTickV = true;
            updateTickV();
        } else {
            autoTickV = false;
        }
        update();
    }

    /** TODO: implement this
     */
    private void checkTickV(TickVDescriptor tickV) throws IllegalArgumentException {
    }

    private void updateTickVLog() {

        GrannyTextRenderer idlt = new GrannyTextRenderer();
        idlt.setString(this.getTickLabelFont(), "10!U-10");

        int nTicksMax;
        if (isHorizontal()) {
            nTicksMax = (int) Math.floor(getColumn().getWidth() / (idlt.getWidth()));
        } else {
            nTicksMax = (int) Math.floor(getRow().getHeight() / (idlt.getHeight()));
        }

        nTicksMax = (nTicksMax < 7) ? nTicksMax : 7;

        tickV = TickVDescriptor.bestTickVLogNew(getDataMinimum(), getDataMaximum(), 3, nTicksMax, true);
        datumFormatter = resolveFormatter(tickV);

        return;

    }

    private void updateTickVLinear() {
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

        this.tickV = TickVDescriptor.bestTickVLinear(getDataMinimum(), getDataMaximum(), 3, nTicksMax, false);

        DatumFormatter tdf = resolveFormatter(tickV);

        boolean once = true;

        while (once) {

            Rectangle maxBounds = getMaxBounds(tdf, tickV);

            if (isHorizontal()) {
                int tickSizePixels = (int) (maxBounds.width + getEmSize() * 2);
                nTicksMax = axisSize / tickSizePixels;
            } else {
                int tickSizePixels = (int) (maxBounds.height);
                nTicksMax = axisSize / tickSizePixels;
            }

            this.tickV = TickVDescriptor.bestTickVLinear(getDataMinimum(), getDataMaximum(), 3, nTicksMax, true);
            datumFormatter = resolveFormatter(tickV);

            once = false;
        }

        return;

    }

    private DatumFormatter resolveFormatter(TickVDescriptor tickV) {
        return getUserDatumFormatter() == null ? tickV.getFormatter() : getUserDatumFormatter();
    }

    private Rectangle getMaxBounds(DatumFormatter tdf, TickVDescriptor tickV) {
        String[] granny = tdf.axisFormat(tickV.tickV, getDatumRange());
        GrannyTextRenderer idlt = new GrannyTextRenderer();
        Rectangle bounds = new Rectangle();
        for (int i = 0; i < granny.length; i++) {
            idlt.setString(this.getTickLabelFont(), granny[i]);
            bounds.add(idlt.getBounds());
        }
        return bounds;
    }

    private boolean hasLabelCollisions(DatumVector major,DatumFormatter df) {
        if (major.getLength() < 2) {
            return false;
        }
        String[] granny = df.axisFormat(major, getDatumRange());
        GrannyTextRenderer idlt = new GrannyTextRenderer();
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
            if ( x1<10000 ) {
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

    private void updateTickVDomainDivider() {
        DatumRange dr = getDatumRange();

        try {
            long nminor= minorTicksDomainDivider.boundaryCount( dr.min(), dr.max() );
            while ( nminor>=DomainDivider.MAX_BOUNDARIES ) {
                //TODO: what should we do here?  Transitional state?
                return;
            }
            DatumVector major = majorTicksDomainDivider.boundaries(dr.min(), dr.max());
            DatumVector minor = minorTicksDomainDivider.boundaries(dr.min(), dr.max());

            this.tickV = TickVDescriptor.newTickVDescriptor(major, minor);
            this.tickV.datumFormatter= dividerDatumFormatter;

            datumFormatter = resolveFormatter(tickV);
        } catch ( InconvertibleUnitsException ex ) {
            // it's okay to do nothing.
        }
    }

    private void updateTickVTime() {

        int nTicksMax;

        DatumRange dr = getDatumRange();
        Datum pixel = dr.width().divide(getDLength());
        DatumFormatter tdf;

        if (isHorizontal()) {
            // two passes to avoid clashes -- not guarenteed
            tickV = TickVDescriptor.bestTickVTime(dr.min().subtract(pixel), dr.max().add(pixel), 3, 8, false);

            tdf = resolveFormatter(tickV);
            Rectangle bounds = getMaxBounds(tdf, tickV);

            int tickSizePixels = (int) (bounds.width + getEmSize() * 2);

            if (drawTca) {
                FontMetrics fm = getFontMetrics(getTickLabelFont());
                String item = format(99999.99, "(f8.2)");
                int width = fm.stringWidth(item) + (int) (getEmSize() * 2);
                if (width > tickSizePixels) {
                    tickSizePixels = width;
                }
            }

            int axisSize = getColumn().getWidth();
            nTicksMax = Math.max(2, axisSize / tickSizePixels);

            tickV = TickVDescriptor.bestTickVTime(getDataMinimum(), getDataMaximum(), 2, nTicksMax, false);
            tdf = resolveFormatter(tickV);

            bounds = getMaxBounds(tdf, tickV);
            tickSizePixels = (int) (bounds.getWidth() + getEmSize() * 2);

            if (drawTca) {
                FontMetrics fm = getFontMetrics(getTickLabelFont());
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

                tickV = TickVDescriptor.bestTickVTime(getDataMinimum(), getDataMaximum(), 2, nTicksMax, false);

                if (tickV.getMajorTicks().getLength() <= 1) {
                    // we're about to have an assertion error, time to debug;
                    System.err.println("about to assert error: " + tickV.getMajorTicks());
                }
                assert (tickV.getMajorTicks().getLength() > 1);

                tdf = resolveFormatter(tickV);

                bounds = getMaxBounds(tdf, tickV);
                tickSizePixels = (int) (bounds.getWidth() + getEmSize() * 2);

                double x0 = transform(tickV.getMajorTicks().get(0));
                double x1 = transform(tickV.getMajorTicks().get(1));

                if (x1 - x0 > tickSizePixels) {
                    overlap = false;
                } else {
                    nTicksMax = nTicksMax - 1;
                }
            }
            tickV = TickVDescriptor.bestTickVTime(getDataMinimum(), getDataMaximum(), 2, nTicksMax, true);

        } else {
            int tickSizePixels = getFontMetrics(getTickLabelFont()).getHeight();
            int axisSize = getRow().getHeight();
            nTicksMax = axisSize / tickSizePixels;

            nTicksMax = (nTicksMax > 1 ? nTicksMax : 2);
            nTicksMax = (nTicksMax < 10 ? nTicksMax : 10);

            tickV = TickVDescriptor.bestTickVTime(getDataMinimum(), getDataMaximum(), 3, nTicksMax, true);

        }

        datumFormatter = resolveFormatter(tickV);

        if (drawTca && tcaFunction != null) {
            updateTCADataSet();
        }
    }

    public synchronized void updateTickV() {
        if (!valueIsAdjusting()) {
            if ( getTickLabelFont()==null ) return;
            //if ( getCanvas()==null || getCanvas().getHeight()==0 ) return;
            //if ( ( isHorizontal() ? getColumn().getWidth() : getRow().getHeight() ) < 2 ) return; // canvas is not sized yet
            if ( useDomainDivider ) {
                updateDomainDivider(); //TODO: doesn't consider width of TCAs.
            } else {
                this.majorTicksDomainDivider= null;
            }
            if (autoTickV) {
                TickVDescriptor oldTicks = this.tickV;
                if (majorTicksDomainDivider != null) {
                    updateTickVDomainDivider();
                } else {
                    if (getUnits() instanceof TimeLocationUnits) {
                        updateTickVTime();
                    } else if (dataRange.isLog()) {
                        updateTickVLog();
                    } else {
                        updateTickVLinear();
                    }
                }
                if (drawTca && tcaFunction != null) {
                    updateTCADataSet();
                }

                firePropertyChange(PROPERTY_TICKS, oldTicks, this.tickV);
            }
            repaint();
        } else {
            if (autoTickV) {
                try {
                    if (majorTicksDomainDivider != null) {
                        updateTickVDomainDivider();
                        if (drawTca && tcaFunction != null) {
                            updateTCADataSet();
                        }
                    } else {
                        if (getUnits() instanceof TimeLocationUnits) {
                            updateTickVTime();
                        } else if (dataRange.isLog()) {
                            updateTickVLog();
                        } else {
                            updateTickVLinear();
                        }
                    }
                    repaint();
                } catch ( NullPointerException ex ) {
                    ex.printStackTrace(); // why do we get this now?
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

    /** paints the axis component.  The tickV's and bounds should be calculated at this point */
    protected void paintComponent(Graphics graphics) {
        logger.finest("enter DasAxis.paintComponent");

        if (getCanvas().isValueAdjusting()) {
            return;
        }
        
        try {
            updateTickLength();
        } catch (ParseException ex) {
            Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        /* This was code was keeping axes from being printed on PC's
        Shape saveClip = null;
        if (getCanvas().isPrintingThread()) {
        saveClip = graphics.getClip();
        graphics.setClip(null);
        }
         */
        logger.finest("DasAxis clip=" + graphics.getClip() + " @ " + getX() + "," + getY());
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

        /* Debugging code */
        /* The compiler will optimize it out if DEBUG_GRAPHICS == false */
        if (DEBUG_GRAPHICS) {
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_BUTT, BasicStroke.CAP_BUTT, 1f, new float[]{3f, 3f}, 0f));
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
            g.setStroke(new BasicStroke(1f));
            g.setColor(DEBUG_COLORS[debugColorIndex]);
            debugColorIndex++;
            if (debugColorIndex >= DEBUG_COLORS.length) {
                debugColorIndex = 0;
            }
        }
        /* End debugging code */

        TickVDescriptor tickV1;
        synchronized (this) {
            tickV1= this.tickV;
        }
        if (tickV1 == null || tickV1.tickV.getUnits().isConvertableTo(getUnits())) {
            if (isHorizontal()) {
                paintHorizontalAxis(g);
            } else {
                paintVerticalAxis(g);
            }
        } else {
            if ( getCanvas().isPrintingThread() ) {
                this.updateImmediately();
                g.setClip(null);
                System.err.println("calculated ticks on printing thread, this may cause problems");
                //System.err.println("inconvertible units on printing thread!");
                if (isHorizontal()) {
                    paintHorizontalAxis(g);
                } else {
                    paintVerticalAxis(g);
                }
                
            }
        }

        Rectangle clip = g.getClipBounds();
        if (clip == null) {
            clip = new Rectangle(getX(), getY(), getWidth(), getHeight());
        }

        if (drawTca && getOrientation() == BOTTOM && tcaData != null && blLabelRect != null && blLabelRect.intersects(clip)) {

            int position = getRow().getDMaximum();
            int DMin = getColumn().getDMinimum();
            Font tickLabelFont = getTickLabelFont();
            FontMetrics tickLabelFontMetrics = getFontMetrics(tickLabelFont);
            int tickLength = tickLabelFont.getSize() * 2 / 3;
            int tick_label_gap = tickLabelFontMetrics.stringWidth(" ");
            int lineHeight = tickLabelFont.getSize() + getLineSpacing();

            int baseLine = position + tickLength + tick_label_gap + tickLabelFont.getSize();
            int rightEdge = DMin - tickLabelFontMetrics.stringWidth("0000") - tick_label_gap;

            GrannyTextRenderer idlt = new GrannyTextRenderer();
            /*
            idlt.setString(this.getGraphics(), "SCET");
            int width = (int)Math.ceil(idlt.getWidth());
            int leftEdge = rightEdge - width;
            idlt.draw(g, (float)leftEdge, (float)baseLine);
             */


            int width, leftEdge;

            if ( tcaData==null ) {
                baseLine += lineHeight;
                idlt.setString( g, "tcaData not available" );
                idlt.draw( graphics, (float)(rightEdge-idlt.getWidth()), (float)baseLine );
            } else {
                QDataSet bds= (QDataSet) tcaData.property(QDataSet.BUNDLE_1);
                if ( bds==null ) System.err.println("expected TCA data to have BUNDLE dataset");
                int lines= Math.min( MAX_TCA_LINES, tcaData.length(0) );
                for (int i = 0; i < lines; i++) {
                    baseLine += lineHeight;
                    if ( bds==null ) {
                        idlt.setString( g, "???" );
                    } else {
                        String label=  (String) bds.property( QDataSet.LABEL, i ) ;
                        if ( label==null ) {
                            idlt.setString( g, "????" ); // This shouldn't happen, but does...  We need to check earlier
                        } else {
                            idlt.setString( g, label );
                        }
                    }
                    width = (int) Math.floor(idlt.getWidth() + 0.5);
                    leftEdge = rightEdge - width;
                    idlt.draw(g, (float) leftEdge, (float) baseLine);
                }
            }
        }

        boolean drawBounds= false;
        if ( drawBounds ) {
            Rectangle b= getAxisBounds();
            g.setColor( Color.GREEN );
            g.draw( new Rectangle( b.x, b.y, b.width-1, b.height-1 ) );
        }

        g.dispose();
        getDasMouseInputAdapter().paint(graphics);

    /* This was code was keeping axes from being printed on PC's
    if (getCanvas().isPrintingThread()) {
    g.setClip(saveClip);
    }
     */
    }

    private String resolveAxisLabel() {
        String result= axisLabel;
        if ( result.contains("%{" ) ) {
            result= result.replaceAll("%\\{UNITS\\}", getUnits().toString() );
            result= result.replaceAll("%\\{RANGE\\}", getDatumRange().toString() );
            result= result.replaceAll("%\\{SCAN_RANGE\\}", String.valueOf(getScanRange()) );
        }
        return result;
    }

    /** Paint the axis if it is horizontal  */
    protected void paintHorizontalAxis(Graphics2D g) {
        try {
            Rectangle clip = g.getClipBounds();
            if (clip == null) {
                clip = new Rectangle(getX(), getY(), getWidth(), getHeight());
            }

            boolean bottomLine = ((orientation == BOTTOM || oppositeAxisVisible) && blLineRect != null && blLineRect.intersects(clip));
            boolean bottomTicks = ((orientation == BOTTOM || oppositeAxisVisible) && blTickRect != null && blTickRect.intersects(clip));
            boolean bottomTickLabels = ((orientation == BOTTOM && tickLabelsVisible) && blLabelRect != null && blLabelRect.intersects(clip));
            boolean bottomLabel = ((orientation == BOTTOM && !axisLabel.equals("")) && blTitleRect != null && blTitleRect.intersects(clip));
            boolean topLine = ((orientation == TOP || oppositeAxisVisible) && trLineRect != null && trLineRect.intersects(clip));
            boolean topTicks = ((orientation == TOP || oppositeAxisVisible) && trTickRect != null && trTickRect.intersects(clip));
            boolean topTickLabels = ((orientation == TOP && tickLabelsVisible) && trLabelRect != null && trLabelRect.intersects(clip));
            boolean topLabel = ((orientation == TOP && !axisLabel.equals("")) && trTitleRect != null && trTitleRect.intersects(clip));

            int topPosition = getRow().getDMinimum() - 1;
            int bottomPosition = getRow().getDMaximum();
            int DMax = getColumn().getDMaximum();
            int DMin = getColumn().getDMinimum();

            TickVDescriptor ticks = getTickV();

            if (bottomLine) {
                g.drawLine(DMin, bottomPosition, DMax, bottomPosition);
            }
            if (topLine) {
                g.drawLine(DMin, topPosition, DMax, topPosition);
            }

            int tickLengthMajor = tickLen; //labelFont.getSize() * 2 / 3;
            int tickLengthMinor = tickLengthMajor / 2;
            int tickLength;

            String[] labels = tickFormatter(ticks.tickV, getDatumRange());

            for (int i = 0; i < ticks.tickV.getLength(); i++) {
                Datum tick1 = ticks.tickV.get(i);
                int tickPosition = (int) Math.floor(transform(tick1));
                if (DMin <= tickPosition && tickPosition <= DMax) {
                    tickLength = tickLengthMajor;
                    if (bottomTicks) {
                        g.drawLine(tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                    }
                    if (bottomTickLabels) {
                        drawLabel(g, tick1, labels[i], i, tickPosition, bottomPosition + Math.max(0,tickLength) );
                    }
                    if (topTicks) {
                        g.drawLine(tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    }
                    if (topTickLabels) {
                        drawLabel(g, tick1, labels[i], i, tickPosition, topPosition - Math.max(0,tickLength) + 1);
                    }
                }
            }

            for (int i = 0; i < ticks.minorTickV.getLength(); i++) {
                Datum tick = ticks.minorTickV.get(i);
                int tickPosition = (int) Math.floor(transform(tick));
                if (DMin <= tickPosition && tickPosition <= DMax) {
                    tickLength = tickLengthMinor;
                    if (bottomTicks) {
                        g.drawLine(tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                    }
                    if (topTicks) {
                        g.drawLine(tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    }
                }
            }

            if (!axisLabel.equals("")) {
                Graphics2D g2 = (Graphics2D) g.create();
                int titlePositionOffset = getTitlePositionOffset();
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                String axislabel1= resolveAxisLabel();
                gtr.setString(g2, axislabel1);
                int titleWidth = (int) gtr.getWidth();
                int baseline;
                int leftEdge;
                g2.setFont(getLabelFont());
                if (bottomLabel) {
                    leftEdge = DMin + (DMax - DMin - titleWidth) / 2;
                    baseline = bottomPosition + titlePositionOffset;
                    gtr.draw(g2, (float) leftEdge, (float) baseline);
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

    /** Paint the axis if it is vertical  */
    protected void paintVerticalAxis(Graphics2D g) {
        try {
            Rectangle clip = g.getClipBounds();
            if (clip == null) {
                clip = new Rectangle(getX(), getY(), getWidth(), getHeight());
            }

            boolean leftLine = ((orientation == LEFT || oppositeAxisVisible) && blLineRect != null && blLineRect.intersects(clip));
            boolean leftTicks = ((orientation == LEFT || oppositeAxisVisible) && blTickRect != null && blTickRect.intersects(clip));
            boolean leftTickLabels = ((orientation == LEFT && tickLabelsVisible) && blLabelRect != null && blLabelRect.intersects(clip));
            boolean leftLabel = ((orientation == LEFT && !axisLabel.equals("")) && blTitleRect != null && blTitleRect.intersects(clip));
            boolean rightLine = ((orientation == RIGHT || oppositeAxisVisible) && trLineRect != null && trLineRect.intersects(clip));
            boolean rightTicks = ((orientation == RIGHT || oppositeAxisVisible) && trTickRect != null && trTickRect.intersects(clip));
            boolean rightTickLabels = ((orientation == RIGHT && tickLabelsVisible) && trLabelRect != null && trLabelRect.intersects(clip));
            boolean rightLabel = ((orientation == RIGHT && !axisLabel.equals("")) && trTitleRect != null && trTitleRect.intersects(clip));

            int leftPosition = getColumn().getDMinimum() - 1;
            int rightPosition = getColumn().getDMaximum();
            int DMax = getRow().getDMaximum();
            int DMin = getRow().getDMinimum();

            TickVDescriptor ticks = getTickV();

            if (leftLine) {
                g.drawLine(leftPosition, DMin, leftPosition, DMax);
            }
            if (rightLine) {
                g.drawLine(rightPosition, DMin, rightPosition, DMax);
            }

            int tickLengthMajor = tickLen;
            int tickLengthMinor = tickLengthMajor / 2;
            int tickLength;

            String[] labels = tickFormatter(ticks.tickV, getDatumRange());
            for (int i = 0; i < ticks.tickV.getLength(); i++) {
                Datum tick1 = ticks.tickV.get(i);
                int tickPosition = (int) Math.floor(transform(tick1)+0.0001);
                if (DMin <= tickPosition && tickPosition <= DMax) {

                    tickLength = tickLengthMajor;
                    if (leftTicks) {
                        g.drawLine(leftPosition, tickPosition, leftPosition - tickLength, tickPosition);
                    }
                    if (leftTickLabels) {
                        drawLabel(g, tick1, labels[i], i, leftPosition - Math.max( 0,tickLength ), tickPosition);
                    }
                    if (rightTicks) {
                        g.drawLine(rightPosition, tickPosition, rightPosition + tickLength, tickPosition);
                    }
                    if (rightTickLabels) {
                        drawLabel(g, tick1, labels[i], i, rightPosition + Math.max( 0,tickLength ), tickPosition);
                    }
                }
            }

            for (int i = 0; i < ticks.minorTickV.getLength(); i++) {
                tickLength = tickLengthMinor;
                double tick1 = ticks.minorTickV.doubleValue(i, getUnits());
                int tickPosition = (int) Math.floor(transform(tick1, ticks.units)+0.0001);
                if (DMin <= tickPosition && tickPosition <= DMax) {
                    tickLength = tickLengthMinor;
                    if (leftTicks) {
                        g.drawLine(leftPosition, tickPosition, leftPosition - tickLength, tickPosition);
                    }
                    if (rightTicks) {
                        g.drawLine(rightPosition, tickPosition, rightPosition + tickLength, tickPosition);
                    }
                }
            }


            if (!axisLabel.equals("")) {
                Graphics2D g2 = (Graphics2D) g.create();
                int titlePositionOffset = getTitlePositionOffset();
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                gtr.setString(g2, resolveAxisLabel() );
                int titleWidth = (int) gtr.getWidth();
                int baseline;
                int leftEdge;
                g2.setFont(getLabelFont());
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

    /** TODO
     * @return
     */
    protected int getTitlePositionOffset() {
        Font tickLabelFont = getTickLabelFont();
        FontMetrics fm = getFontMetrics(tickLabelFont);
        Font labelFont = getLabelFont();
        int tickLength = tickLabelFont.getSize() * 2 / 3;

        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(labelFont, axisLabel);

        int offset;

        if (orientation == BOTTOM) {
            offset = tickLabelFont.getSize() + tickLength + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize() / 2;
        } else if (orientation == TOP) {
            offset = tickLength + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize() / 2 + (int) gtr.getDescent();
        } else if (orientation == LEFT) {
            //offset = tickLength + (int)this.blLabelRect.getWidth() + fm.stringWidth(" ") + labelFont.getSize() / 2 + (int) gtr.getDescent();
            offset = getColumn().getDMinimum() - blLabelRect.x + labelFont.getSize() / 2 + (int) gtr.getDescent();
        } else {
            offset = this.trLabelRect.x + this.trLabelRect.width - getColumn().getDMaximum() + labelFont.getSize() / 2 + (int) (flipLabel ? gtr.getAscent() : gtr.getDescent());
        /*if ( !flipLabel ) {
        offset = tickLength + (int)this.trLabelRect.getWidth() + fm.stringWidth(" ") + labelFont.getSize() / 2 + (int) gtr.getDescent();
        } else {
        offset = tickLength + (int)this.trLabelRect.getWidth() + fm.stringWidth(" ") + labelFont.getSize() / 2 + (int) gtr.getAscent();
        offset= this.trLabelRect.x + this.trLabelRect.width - getColumn().getDMaximum() + labelFont.getSize() / 2
        }*/
        }
        if (getOrientation() == BOTTOM && drawTca && tcaData != null) {
            offset += Math.min( MAX_TCA_LINES, tcaData.length(0) ) * (tickLabelFont.getSize() + getLineSpacing());
        }
        return offset;
    }

    public int getLineSpacing() {
        return getTickLabelFont().getSize() / 4;
    }

    /** 
     * Draw each label of the axis.
     */
    protected void drawLabel(Graphics2D g, Datum value, String label, int index, int x, int y) {

        if (!tickLabelsVisible) {
            return;
        }

        g.setFont(getTickLabelFont());
        GrannyTextRenderer idlt = new GrannyTextRenderer();
        idlt.setString(g, label);

        int width;
        width = (int) (isHorizontal() ? idlt.getLineOneWidth() : idlt.getWidth());
        int height = (int) idlt.getHeight();
        int ascent = (int) idlt.getAscent();

        int tick_label_gap = tickLen/2; //getFontMetrics(getTickLabelFont()).stringWidth(" ");

        if ( tick_label_gap<TICK_LABEL_GAP_MIN ) tick_label_gap= TICK_LABEL_GAP_MIN;
        
        if (orientation == BOTTOM) {
            x -= width / 2;
            y += getTickLabelFont().getSize() + tick_label_gap;
        } else if (orientation == TOP) {
            x -= width / 2;
            y -= tick_label_gap + idlt.getDescent();
        } else if (orientation == LEFT) {
            x -= (width + tick_label_gap);
            y += ascent - height / 2;
        } else {
            x += tick_label_gap;
            y += ascent - height / 2;
        }
        idlt.draw(g, x, y);
        if (orientation == BOTTOM && drawTca && tcaData != null) {
            drawTCAItems(g, value, x, y, width);
        }
    }

    private void drawTCAItems(Graphics g, Datum value, int x, int y, int width) {
        int index;

        int baseLine, leftEdge, rightEdge;
        double pixelSize;
        double tcaValue;

        if (tcaData == null || tcaData.length() == 0) {
            return;
        }

        QDataSet dep0= (QDataSet)tcaData.property(QDataSet.DEPEND_0);

        baseLine = y;
        leftEdge = x;
        rightEdge = leftEdge + width;

        index = org.virbo.dataset.DataSetUtil.closestIndex( dep0, value);
        if ( index < 0 || index >= tcaData.length() ) {
            return;
        }

        pixelSize = getDatumRange().width().divide(getDLength()).doubleValue( getUnits().getOffsetUnits() );

        if (tcaData.length() == 0) {
            g.drawString("tca data is empty", leftEdge, baseLine);
            return;
        }

        tcaValue = dep0.value(index);

        //Added in to say take nearest nieghbor as long as the distance to the nieghbor is
        //not more than the xtagwidth.
        QDataSet xTagWidth = org.virbo.dataset.DataSetUtil.guessCadenceNew( dep0, null );
        
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

        int lines= Math.min( MAX_TCA_LINES, tcaData.length(0) );

        for (int i = 0; i < lines; i++) {
            try {
                baseLine += lineHeight;
                QDataSet v1= tcaData.slice(index).slice(i);
                String item;
                item= org.virbo.dataset.DataSetUtil.getStringValue( v1, v1.value() );
                width = fm.stringWidth(item);
                leftEdge = rightEdge - width;
                g.drawString(item, leftEdge, baseLine);
            } catch ( RuntimeException ex ) {
                g.drawString("except!c"+ex.getMessage(),leftEdge, baseLine);
            }
        }
    }

    /** TODO
     * @return
     */
    public Font getTickLabelFont() {
        return this.getFont();
    }

    /** TODO
     * @param tickLabelFont
     */
    public void setTickLabelFont(Font tickLabelFont) {
    }

    /** TODO
     * @return
     */
    public Font getLabelFont() {
        return this.getFont();
    }

    /** TODO
     * @param labelFont
     */
    public void setLabelFont(Font labelFont) {
        // TODO: whah?--jbf
    }

    public static class Memento {

        private DatumRange range;
        private int dmin,  dmax;
        private boolean log;
        private boolean flipped;
        private DasAxis axis;

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + (this.range != null ? this.range.hashCode() : 0);
            hash = 29 * hash + this.dmin;
            hash = 29 * hash + this.dmax;
            hash = 29 * hash + ( this.log ? 1 : 0 );
            hash = 29 * hash + ( this.flipped ? 1 : 0 );
            hash = 29 * hash + (this.axis != null ? this.axis.hashCode() : 0);
            return hash;
        }

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
                    this.axis == m.axis);
            }
        }

        public String toString() {
            return (log ? "log " : "") + range.toString() + " (" + ( DatumUtil.asOrderOneUnits(range.width()).toString() ) + ") " + (dmax - dmin) + " pixels @ " + dmin;
        }
    }

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
        result.axis = this;
        return result;
    }

    /**
     * return the AffineTransform, or null.  The transform will be applied after the input
     * transform is applied.  So to just get the transform, pass in identity.
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
        if (!memento.range.getUnits().isConvertableTo(getUnits())) {
            return null;
        }

        //TODO: remove cut-n-paste code
        //return getAffineTransform(memento.range, false, at);

        double dmin0, dmax0;
        dmin0 = transform(memento.range.min());
        dmax0 = transform(memento.range.max());

        double scale2 = (0. + getMemento().dmin - getMemento().dmax) / (memento.dmin - memento.dmax);
        double trans2 = -1 * memento.dmin * scale2 + getMemento().dmin;

        if ( dmin0==10000 || dmin0==-10000 | dmax0==10000 | dmax0==10000 ) {
            System.err.println("unable to create transform");
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

    /** TODO
     * @return
     */
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
        if (direction == UP || direction == RIGHT) {
            tickDirection = -1;
        } else if (direction == DOWN || direction == LEFT) {
            tickDirection = 1;
        } else {
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
            DatumVector tickv = ticks.tickV;
            int size = Integer.MIN_VALUE;
            for (int i = 0; i < tickv.getLength(); i++) {
                String label = tickFormatter(tickv.get(i));
                GrannyTextRenderer idlt = new GrannyTextRenderer();
                idlt.setString(f, label);
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

    /** TODO
     * @param fm
     * @deprecated use getMaxLabelWidth()
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
                GrannyTextRenderer idlt = new GrannyTextRenderer();
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

    /** TODO */
    public void resize() {
        resetTransform();
        if ( getFont()==null ) {
            return;
        }
        Rectangle oldBounds = this.getBounds();
        setBounds(getAxisBounds());
        //setBounds(getAxisBoundsNew());
        invalidate();
        if (tickV == null || tickV.tickV.getUnits().isConvertableTo(getUnits())) {
            validate();
        }
        firePropertyChange(PROP_BOUNDS, oldBounds, getBounds());
    }

    /**
     * calculate the bounds of the labels.  This should including regions that 
     * the labels could occupy if the axis were panned, so that result doesn't 
     * change during panning.
     * 
     * @return Rectangle in the canvas coordinate frame.
     */
    protected synchronized Rectangle getLabelBounds(Rectangle bounds) {
        if ( this.getTickV()==null || !this.getTickV().tickV.getUnits().isConvertableTo(getUnits() ) ) {
            return bounds;
        }

        String[] labels = tickFormatter(this.getTickV().tickV, getDatumRange());

        GrannyTextRenderer gtr = new GrannyTextRenderer();

        Font labelFont = this.getLabelFont();

        double dmin, dmax;
        if ( isHorizontal() ) {
            dmin= getColumn().getDMinimum();
            dmax= getColumn().getDMaximum();
        } else {
            dmin= getRow().getDMinimum();
            dmax= getRow().getDMaximum();
        }

        DatumVector ticks = this.getTickV().tickV;
        for (int i = 0; i < labels.length; i++) {
            Datum d = ticks.get(i);
            DatumRange dr = getDatumRange();
            if (DatumRangeUtil.sloppyContains(dr, d)) {
                gtr.setString(labelFont, labels[i]);
                Rectangle rmin = gtr.getBounds();
                Rectangle rmax = new Rectangle(rmin);  // same bound, but positioned at the axis max.
                double flw = gtr.getLineOneWidth();

                int tick_label_gap = tickLen/2; //getFontMetrics(getTickLabelFont()).stringWidth(" ");
                if ( tick_label_gap<5 ) tick_label_gap= TICK_LABEL_GAP_MIN;
                int space= tick_label_gap;

                int zeroOrPosTickLen= Math.max(0,tickLen);
                if (isHorizontal()) {
                    if (getOrientation() == BOTTOM) {
                        rmin.translate((int) (dmin - flw / 2), getRow().bottom() + space + zeroOrPosTickLen + labelFont.getSize());
                        rmax.translate((int) (dmax - flw / 2), getRow().bottom() + space + zeroOrPosTickLen + labelFont.getSize());
                    } else {
                        rmin.translate((int) (dmin - flw / 2), getRow().top() - space - zeroOrPosTickLen - (int) rmin.getHeight());
                        rmax.translate((int) (dmax - flw / 2), getRow().top() - space - zeroOrPosTickLen - (int) rmax.getHeight());
                    }
                    if ( bounds==null ) bounds= rmin;
                    bounds.add(rmin);
                    bounds.add(rmax);
                } else {
                    if (getOrientation() == LEFT) {
                        rmin.translate(-(int) rmin.getWidth() - space - zeroOrPosTickLen + getColumn().left(),
                                (int) (dmin + getEmSize() / 2));
                        rmax.translate(-(int) rmax.getWidth() - space - zeroOrPosTickLen + getColumn().left(),
                                (int) (dmax + getEmSize() / 2));
                    } else {
                        rmin.translate( space + zeroOrPosTickLen + getColumn().right(), (int) (dmin + getEmSize() / 2));
                        rmax.translate( space + zeroOrPosTickLen + getColumn().right(), (int) (dmax + getEmSize() / 2));
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
    protected Rectangle getAxisBounds() {
        Rectangle bounds;
        
        try {
            updateTickLength();
        } catch (ParseException ex) {
            Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (isHorizontal()) {
            bounds = getHorizontalAxisBounds();
        } else {
            bounds = getVerticalAxisBounds();
        }
        if (getOrientation() == BOTTOM && isTickLabelsVisible()) {
            if (drawTca && tcaData != null && tcaData.length() != 0) {
                int DMin = getColumn().getDMinimum();
                Font tickLabelFont = getTickLabelFont();
                int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
                int lines= Math.min( MAX_TCA_LINES, tcaData.length(0) );
                int tcaHeight = (tickLabelFont.getSize() + getLineSpacing()) * lines;
                int maxLabelWidth = getMaxLabelWidth();
                bounds.height += tcaHeight;
                blLabelRect.height += tcaHeight;
                if (blTitleRect != null) {
                    blTitleRect.y += tcaHeight;
                }
                GrannyTextRenderer idlt = new GrannyTextRenderer();
                idlt.setString(tickLabelFont, "SCET");
                int tcaLabelWidth = (int) Math.floor(idlt.getWidth() + 0.5);
                QDataSet bds= (QDataSet) tcaData.property(QDataSet.BUNDLE_1);
                for (int i = 0; i < lines; i++) {
                    String ss;
                    if ( bds==null ) {
                        ss= "???";
                    } else {
                        ss=  (String) bds.property( QDataSet.LABEL, i );
                    }
                    if ( ss==null ) ss= "   ";
                    idlt.setString( tickLabelFont, ss );
                    int width = (int) Math.floor(idlt.getWidth() + 0.5);
                    tcaLabelWidth = Math.max(tcaLabelWidth, width);
                }
                tcaLabelWidth += 50;
                if (tcaLabelWidth > 0) {
                    int tcaLabelSpace = DMin - tcaLabelWidth - tick_label_gap;
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

    private Rectangle getHorizontalAxisBounds() {
        int topPosition = getRow().getDMinimum() - 1;
        int bottomPosition = getRow().getDMaximum();
        DasDevicePosition range = getColumn();
        int DMax = range.getDMaximum();
        int DMin = range.getDMinimum();
        int DWidth = DMax - DMin;

        boolean bottomTicks = (orientation == BOTTOM || oppositeAxisVisible);
        boolean bottomTickLabels = (orientation == BOTTOM && tickLabelsVisible);
        boolean bottomLabel = (bottomTickLabels && !axisLabel.equals(""));
        boolean topTicks = (orientation == TOP || oppositeAxisVisible);
        boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
        boolean topLabel = (topTickLabels && !axisLabel.equals(""));

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

        //Add room for ticks
        if (bottomTicks) {
            int x = DMin;
            int y = bottomPosition + 1 - Math.max( -tickLen, 0 );
            int width = DWidth;
            int height = Math.abs( tickLen );
            //The last tick is at position (x + width), so add 1 to width
            blTickRect = setRectangleBounds(blTickRect, x, y, width + 1, height );
        }
        if (topTicks) {
            int x = DMin;
            int y = topPosition - Math.max( 0, tickLen );
            int width = DWidth;
            int height = Math.abs( tickLen );
            //The last tick is at position (x + width), so add 1 to width
            trTickRect = setRectangleBounds(trTickRect, x, y, width + 1, height );
        }
        //int maxLabelWidth = getMaxLabelWidth();
        //int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");

        if (bottomTickLabels) {
            blLabelRect = getLabelBounds(new Rectangle(DMin, blTickRect.y, DWidth, 10));
        //int x = DMin - maxLabelWidth / 2;
        //int y = blTickRect.y + blTickRect.height;
        //int width = DMax - DMin + maxLabelWidth;
        //int height = tickLabelFont.getSize() * 3 / 2 + tick_label_gap;
        //blLabelRect = setRectangleBounds(blLabelRect, x, y, width, height);
        }
        if (topTickLabels) {
            trLineRect = getLabelBounds(new Rectangle(DMin, topPosition - 10, DWidth, 10));
        //int x = DMin - maxLabelWidth / 2;
        //int y = topPosition - (tickLabelFont.getSize() * 3 / 2 + tick_label_gap + 1);
        //int width = DMax - DMin + maxLabelWidth;
        //int height = tickLabelFont.getSize() * 3 / 2 + tick_label_gap;
        //trLabelRect = setRectangleBounds(trLabelRect, x, y, width, height);
        }

        //Add room for the axis label
        Font labelFont = getLabelFont();

        if ( labelFont==null ) {
            return new Rectangle();
        }

        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(labelFont, getLabel());
        int labelSpacing = (int) gtr.getHeight() + labelFont.getSize() / 2;
        if (bottomLabel) {
            int x = DMin;
            int y = blLabelRect.y + blLabelRect.height;
            int width = DMax - DMin;
            int height = labelSpacing;
            blTitleRect = setRectangleBounds(blTitleRect, x, y, width, height);
        }
        if (topLabel) {
            int x = DMin;
            int y = trLabelRect.y - labelSpacing;
            int width = DMax - DMin;
            int height = labelSpacing;
            trTitleRect = setRectangleBounds(trTitleRect, x, y, width, height);
        }

        bounds = new Rectangle((orientation == BOTTOM) ? blLineRect : trLineRect);
        if (bottomTicks) {
            bounds.add(blLineRect);
            bounds.add(blTickRect);
        }
        if (bottomTickLabels) {
            bounds.add(blLabelRect);
        }
        if (bottomLabel) {
            bounds.add(blTitleRect);
        }
        if (topTicks) {
            bounds.add(trLineRect);
            bounds.add(trTickRect);
        }
        if (topTickLabels) {
            bounds.add(trLabelRect);
        }
        if (topLabel) {
            bounds.add(trTitleRect);
        }

        //Add room for the scan buttons (if present)
        if (scanPrevious != null && scanNext != null) {
            Dimension prevSize = scanPrevious.getPreferredSize();
            Dimension nextSize = scanPrevious.getPreferredSize();
            int minX = Math.min(DMin - prevSize.width, bounds.x);
            int maxX = Math.max(DMax + nextSize.width, bounds.x + bounds.width);
            bounds.x = minX;
            bounds.width = maxX - minX;
        }

        return bounds;
    }

    private Rectangle getVerticalAxisBounds() {
        boolean leftTicks = (orientation == LEFT || oppositeAxisVisible);
        boolean leftTickLabels = (orientation == LEFT && tickLabelsVisible);
        boolean leftLabel = (orientation == LEFT && !axisLabel.equals(""));
        boolean rightTicks = (orientation == RIGHT || oppositeAxisVisible);
        boolean rightTickLabels = (orientation == RIGHT && tickLabelsVisible);
        boolean rightLabel = (orientation == RIGHT && !axisLabel.equals(""));

        int leftPosition = getColumn().getDMinimum() - 1;
        int rightPosition = getColumn().getDMaximum();
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

        //Add room for ticks
        if (leftTicks) {
            int x = leftPosition - Math.min( 0,tickLen );
            int y = DMin;
            int width = Math.abs( tickLen );
            int height = DWidth;
            //The last tick is at position (y + height), so add 1 to height
            blTickRect = setRectangleBounds(blTickRect, x, y, width, height + 1);
        }
        if (rightTicks) {
            int x = rightPosition + 1 + Math.min( 0,tickLen );
            int y = DMin;
            int width = Math.abs( tickLen );
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
        }
        if (rightTickLabels) {
            trLabelRect = getLabelBounds(new Rectangle(trTickRect.x + trTickRect.width, DMin, 10, DWidth));
        //int x = trTickRect.x + trTickRect.width;
        //int y = DMin - tickLabelFont.getSize();
        //int width = maxLabelWidth + tick_label_gap;
        //int height = DMax - DMin + tickLabelFont.getSize() * 2;
        //trLabelRect = setRectangleBounds(trLabelRect, x, y, width, height);
        }

        //Add room for the axis label
        Font labelFont = getLabelFont();
        if ( labelFont==null ) {
            return new Rectangle();
        }

        GrannyTextRenderer gtr = new GrannyTextRenderer();
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

        bounds = new Rectangle((orientation == LEFT) ? blLineRect : trLineRect);
        if (leftTicks) {
            bounds.add(blLineRect);
            bounds.add(blTickRect);
        }
        if (leftTickLabels) {
            bounds.add(blLabelRect);
        }
        if (leftLabel) {
            bounds.add(blTitleRect);
        }
        if (rightTicks) {
            bounds.add(trLineRect);
            bounds.add(trTickRect);
        }
        if (rightTickLabels) {
            bounds.add(trLabelRect);
        }
        if (rightLabel) {
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

    /** TODO
     * @return
     */
    public int getTickDirection() {
        return tickDirection;
    }

    /** TODO
     * @return
     */
    public DatumFormatter getDatumFormatter() {
        return datumFormatter;
    }

    /** Transforms a Datum in data coordinates to a horizontal or vertical
     * position on the parent canvas.
     * @param datum a data value
     * @return Horizontal or vertical position on the canvas.
     */
    public double transform(Datum datum) {
        return transform(datum.doubleValue(getUnits()), getUnits());
    }

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

    /** Transforms a double in the given units in data coordinates to a horizontal or vertical
     * position on the parent canvas.
     * @param data a data value
     * @param units the units of the given data value.
     * @return Horizontal or vertical position on the canvas.
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

        if (result > 10000) {
            result = 10000;
        }
        if (result < -10000) {
            result = -10000;
        }
        return result;
    }

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
     * to be overriden to change behavior.  Note that both tickFormatter methods
     * should be overridden.
     * @param tickv
     * @return string, possibly with Granny control characters.
     */
    protected String tickFormatter(Datum d) {
        // TODO: label the axis with the Unit!
        return datumFormatter.grannyFormat(d, d.getUnits());

    }

    /**
     * return the tick labels for these datums and visible range.  This is intended
     * to be overriden to change behavior.  Note that both tickFormatter methods
     * should be overridden.
     * @param tickV
     * @param datumRange
     * @return Strings, possibly with Granny control characters.
     */
    protected String[] tickFormatter(DatumVector tickV, DatumRange datumRange) {
        return datumFormatter.axisFormat(tickV, datumRange);
    }

    /** TODO
     * @param e
     */
    public void dataRangeSelected(DataRangeSelectionEvent e) {
        this.setDataRange(e.getMinimum(), e.getMaximum());
    }

    /** TODO
     * @param xDatum
     * @param direction
     * @param minor
     * @return
     *
     * @depricated. Use getTickVDescriptor.findTick
     */
    public Datum findTick(Datum xDatum, double direction, boolean minor) {
        return getTickV().findTick(xDatum, direction, minor);
    }

    /** TODO
     * @param min0
     * @param max0
     * @param min1
     * @param max1
     */
    private void animateChange(double min0, double max0, double min1, double max1) {

        if (animated && EventQueue.isDispatchThread()) {




            logger.fine("animate axis");

            boolean drawTca0 = getDrawTca();
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

            logger.fine("animation frames/sec= " + (1000. * frames / transitionTime));
            setDrawTca(drawTca0);

            this.dataRange = dataRange0;
        }
    }

    /** TODO */
    protected void updateImmediately() {
        super.updateImmediately();
        logger.finer("updateImmadiately" + getDatumRange() + " " + isLog());
        resetTransform();
        updateTickV();
    }

    /** TODO
     * @return
     * @deprecated use isTickLabelsVisible
     */
    public boolean areTickLabelsVisible() {
        return tickLabelsVisible;
    }

    /**
     * true if the tick labels should be drawn.
     * @return
     */
    public boolean isTickLabelsVisible() {
        return tickLabelsVisible;
    }

    /** TODO
     * @param b
     */
    public void setTickLabelsVisible(boolean b) {
        if (tickLabelsVisible == b) {
            return;
        }
        boolean oldValue = ticksVisible;
        tickLabelsVisible = b;
        update();
        firePropertyChange("tickLabelsVisible", oldValue, b);
    }

    /** TODO */
    protected void installComponent() {
        super.installComponent();
    }

    /** TODO */
    protected void uninstallComponent() {
        super.uninstallComponent();
    }



    public DasAxis createAttachedAxis() {
        return new DasAxis(this.dataRange, this.getOrientation());
    }

    /** TODO
     * @param row
     * @param column
     * @param orientation
     * @return
     */
    public DasAxis createAttachedAxis(int orientation) {
        return new DasAxis(this.dataRange, orientation);
    }


    public void setPlot(DasPlot p) {
        dasPlot = p;
    }


    /**
     * scan to the previous interval.  If we were looking at a day with fuzz, then
     * scan to the previous day.
     * TODO: this should check for any ordinal, for example months, hours, or 10 kg.
     */
    public void scanPrevious() {
        Datum delta = (getDataMaximum().subtract(getDataMinimum())).multiply(1.0);
        if ( UnitsUtil.isTimeLocation( getDataMinimum().getUnits() ) ) {
            double days= delta.doubleValue(Units.days);
            if ( days>0.5 && DasMath.modp( days, 1. ) < 0.1 ) {
                delta= Units.days.createDatum( Math.round( delta.doubleValue(Units.days) ) );
            }
        }
        Datum tmin = getDataMinimum().subtract(delta);
        Datum tmax = getDataMaximum().subtract(delta);
        setDataRange(tmin, tmax);
    }

    /**
     * scan to the next interval.  If we were looking at a day with fuzz, then
     * scan to the next day.
     */
    public void scanNext() {
        Datum delta = (getDataMaximum().subtract(getDataMinimum())).multiply(1.0);
        if ( UnitsUtil.isTimeLocation( getDataMinimum().getUnits() ) ) {
            double days= delta.doubleValue(Units.days);
            if ( days>0.5 && DasMath.modp( days, 1. ) < 0.1 ) {
                delta= Units.days.createDatum( Math.round( delta.doubleValue(Units.days) ) );
            }
        }
        Datum tmin = getDataMinimum().add(delta);
        Datum tmax = getDataMaximum().add(delta);
        setDataRange(tmin, tmax);
    }

    /** TODO
     * @return
     */
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
     */
    public void addMouseWheelListener(MouseWheelListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.addMouseWheelListener(l);
        secondaryInputPanel.addMouseWheelListener(l);
    }

    public void removeMouseWheelListener(MouseWheelListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.removeMouseWheelListener(l);
        secondaryInputPanel.removeMouseWheelListener(l);
    }

    /** TODO
     * @param l
     */
    public void addMouseListener(MouseListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.addMouseListener(l);
        secondaryInputPanel.addMouseListener(l);
    }

    /** TODO
     * @param l
     */
    public void removeMouseListener(MouseListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.removeMouseListener(l);
        secondaryInputPanel.removeMouseListener(l);
    }

    /** TODO
     * @param l
     */
    public void addMouseMotionListener(MouseMotionListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.addMouseMotionListener(l);
        secondaryInputPanel.addMouseMotionListener(l);
    }

    /** TODO
     * @param l
     */
    public void removeMouseMotionListener(MouseMotionListener l) {
        maybeInitializeInputPanels();
        primaryInputPanel.removeMouseMotionListener(l);
        secondaryInputPanel.removeMouseMotionListener(l);
    }

    public void timeRangeSelected(TimeRangeSelectionEvent e) {
        if (e.getSource() != this && !e.equals(lastProcessedEvent)) {
            setDatumRange(e.getRange()); // setDatumRange fires the event
            lastProcessedEvent = e;
        }
    }

    /** Registers TimeRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addTimeRangeSelectionListener(org.das2.event.TimeRangeSelectionListener listener) {
        if (timeRangeListenerList == null) {
            timeRangeListenerList = new javax.swing.event.EventListenerList();
        }
        timeRangeListenerList.add(org.das2.event.TimeRangeSelectionListener.class, listener);
    }

    /** Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(org.das2.event.TimeRangeSelectionListener listener) {
        timeRangeListenerList.remove(org.das2.event.TimeRangeSelectionListener.class, listener);
    }

    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireTimeRangeSelectionListenerTimeRangeSelected(TimeRangeSelectionEvent event) {
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
        public void addLayoutComponent(String name, Component comp) {
        }

        /** TODO
         * @param parent
         */
        public void layoutContainer(Container parent) {
            if (DasAxis.this != parent) {
                throw new IllegalArgumentException();
            }
            if (DasAxis.this.isHorizontal()) {
                horizontalLayout();
            } else {
                verticalLayout();
            }
            if (drawTca && getOrientation() == BOTTOM && tcaData != null) {
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
            boolean topTicks = (orientation == TOP || oppositeAxisVisible);
            boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
            Rectangle bottomBounds = null;
            Rectangle topBounds = null;
            Font tickLabelFont = getTickLabelFont();
            int tickSize = tickLabelFont.getSize() * 2 / 3;
            //Initialize bounds rectangle
            if (bottomTicks) {
                bottomBounds = new Rectangle(DMin, bottomPosition, DMax - DMin + 1, 1);
            }
            if (topTicks) {
                topBounds = new Rectangle(DMin, topPosition, DMax - DMin + 1, 1);
            }
            //Add room for ticks
            if (bottomTicks) {
                bottomBounds.height += tickSize;
            }
            if (topTicks) {
                topBounds.height += tickSize;
                topBounds.y -= tickSize;
            }
            int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
            //Add room for tick labels
            if (bottomTickLabels) {
                bottomBounds.height += tickLabelFont.getSize() * 3 / 2 + tick_label_gap;
            }
            if (topTickLabels) {
                topBounds.y -= (tickLabelFont.getSize() * 3 / 2 + tick_label_gap);
                topBounds.height += tickLabelFont.getSize() * 3 / 2 + tick_label_gap;
            }

            Rectangle primaryBounds = (orientation == BOTTOM ? bottomBounds : topBounds);
            Rectangle secondaryBounds = (orientation == BOTTOM ? topBounds : bottomBounds);

            primaryBounds.translate(-DasAxis.this.getX(), -DasAxis.this.getY());
            if (oppositeAxisVisible) {
                secondaryBounds.translate(-DasAxis.this.getX(), -DasAxis.this.getY());
            }

            primaryInputPanel.setBounds(primaryBounds);
            if (oppositeAxisVisible) {
                secondaryInputPanel.setBounds(secondaryBounds);
            } else {
                secondaryInputPanel.setBounds(-100, -100, 0, 0);
            }

            if (scanPrevious != null && scanNext != null) {
                Dimension preferred = scanPrevious.getPreferredSize();
                int x = DMin - preferred.width - DasAxis.this.getX();
                int y = (orientation == BOTTOM ? bottomPosition : topPosition - preferred.height) - DasAxis.this.getY();
                scanPrevious.setBounds(x, y, preferred.width, preferred.height);
                preferred = scanNext.getPreferredSize();
                x = DMax - DasAxis.this.getX();
                scanNext.setBounds(x, y, preferred.width, preferred.height);
            }
        }

        /** TODO */
        protected void verticalLayout() {
            boolean leftTicks = (orientation == LEFT || oppositeAxisVisible);
            boolean leftTickLabels = (orientation == LEFT && tickLabelsVisible);
            boolean rightTicks = (orientation == RIGHT || oppositeAxisVisible);
            boolean rightTickLabels = (orientation == RIGHT && tickLabelsVisible);
            int leftPosition = getColumn().getDMinimum() - 1;
            int rightPosition = getColumn().getDMaximum();
            int DMax = getRow().getDMaximum();
            int DMin = getRow().getDMinimum();
            Rectangle leftBounds = null;
            Rectangle rightBounds = null;
            Font tickLabelFont = getTickLabelFont();
            int tickSize = tickLabelFont.getSize() * 2 / 3;
            //Initialize bounds rectangle(s)
            if (leftTicks) {
                leftBounds = new Rectangle(leftPosition, DMin, 1, DMax - DMin + 1);
            }
            if (rightTicks) {
                rightBounds = new Rectangle(rightPosition, DMin, 1, DMax - DMin + 1);
            }
            //Add room for ticks
            if (leftTicks) {
                leftBounds.width += tickSize;
                leftBounds.x -= tickSize;
            }
            if (rightTicks) {
                rightBounds.width += tickSize;
            }
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
                rightBounds.width += maxLabelWidth + tick_label_gap;
            //bounds.y -= tickLabelFont.getSize();
            //bounds.height += tickLabelFont.getSize()*2;
            }

            Rectangle primaryBounds = (orientation == LEFT ? leftBounds : rightBounds);
            Rectangle secondaryBounds = (orientation == LEFT ? rightBounds : leftBounds);

            primaryBounds.translate(-DasAxis.this.getX(), -DasAxis.this.getY());
            if (oppositeAxisVisible) {
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
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension();
        }

        /** TODO
         * @param parent
         * @return
         */
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension();
        }
        //NOOP

        /** TODO
         * @param comp
         */
        public void removeLayoutComponent(Component comp) {
        }
    }

    private void refreshScanButtons(boolean reset) {
        if ( scanNext==null ) return; // headless
        if ( scanRange!=null ) {
            if ( !scanRange.getUnits().isConvertableTo(getDatumRange().getUnits()) ) {
                  scanRange=null;
            }
        }
        if ( reset || scanPrevious.hover ) {
            boolean t= ( scanRange==null || ( false ? scanRange.intersects(getDatumRange().next()) : scanRange.intersects(getDatumRange().previous()) ) );
            scanPrevious.hover= t;
        }
        if ( reset || scanNext.hover ) {
            boolean t= ( scanRange==null || ( true ? scanRange.intersects(getDatumRange().next()) : scanRange.intersects(getDatumRange().previous()) ) );
            scanNext.hover= t;
        }
    }

    private class ScanButton extends JButton {

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
            nextButton= SCAN_NEXT_LABEL.equals(text);

            setBorder(new CompoundBorder(
                    new LineBorder(Color.BLACK),
                    new EmptyBorder(2, 2, 2, 2)));
            this.addMouseListener(new MouseAdapter() {

                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        setForeground(Color.LIGHT_GRAY);
                        pressed = scanRange==null || ( nextButton ? scanRange.intersects(getDatumRange().next()) : scanRange.intersects(getDatumRange().previous()) );
                        repaint();
                    }
                }

                public void mouseReleased(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        setForeground(Color.BLACK);
                        pressed = false;
                        repaint();
                    }
                }

                public void mouseEntered(MouseEvent e) {
                    hover = scanRange==null || ( nextButton ? scanRange.intersects(getDatumRange().next()) : scanRange.intersects(getDatumRange().previous()) );
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        /** TODO
         * @param g
         */
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
            }
        }

        /** TODO
         * @param g
         */
        protected void paintBorder(Graphics g) {
            if (hover || pressed) {
                super.paintBorder(g);
            }
        }
    }

    private void updateTickLength() throws ParseException {
        double[] pos = DasDevicePosition.parseFormatStr(this.tickLenStr);
        if ( pos[0]==0 ) {
            this.tickLen = (int) ( Math.round( pos[1]*getEmSize() + pos[2] ) ); // make independent from row layout for initialization.
        } else {
            this.tickLen = (int) (Math.round( pos[0]*getRow().getHeight() + pos[1]*getEmSize() + pos[2] ));
        }
    }

    public String getTickLength() {
        return this.tickLenStr;
    }

    /**
     * "0.33em" "5px" "-0.33em"
     * @param tickLengthStr
     */
    public void setTickLength( String tickLengthStr ) {
        this.tickLenStr= tickLengthStr;
        try {
            updateTickLength();
            resize();
            repaint();
        } catch (ParseException ex) {
            Logger.getLogger(DasAxis.class.getName()).log(Level.SEVERE, null, ex);
            ex.printStackTrace();
        }
    }

    public boolean isFlipped() {
        return flipped;
    }

    public void setFlipped(boolean b) {
        update();
        this.flipped = b;
    }
    protected String formatString = "";
    public static final String PROP_FORMATSTRING = "formatString";

    public String getFormat() {
        return formatString;
    }
    protected boolean flipLabel = false;
    public static final String PROP_FLIPLABEL = "flipLabel";

    public boolean isFlipLabel() {
        return flipLabel;
    }

    public void setFlipLabel(boolean flipLabel) {
        boolean oldFlipLabel = this.flipLabel;
        this.flipLabel = flipLabel;
        repaint();
        firePropertyChange(PROP_FLIPLABEL, oldFlipLabel, flipLabel);
    }

    /** the formatter identified to work with the divider */
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

    public boolean isUseDomainDivider() {
        return useDomainDivider;
    }

    public void setUseDomainDivider(boolean useDomainDivider) {
        boolean oldUseDomainDivider = this.useDomainDivider;
        this.useDomainDivider = useDomainDivider;
        if ( oldUseDomainDivider!=useDomainDivider ) {
            updateTickV();
        }
        firePropertyChange(PROP_USEDOMAINDIVIDER, oldUseDomainDivider, useDomainDivider);
    }


    /**
     * set a hint at the format string.  Examples include:
     *   0.000
     *   %H:%M!c%Y-%j
     * @param formatString
     */
    public void setFormat(String formatString) {
        try {
            String oldFormatString = this.formatString;
            this.formatString = formatString;
            if (formatString.equals("")) {
                setUserDatumFormatter(null);
            } else {
                setUserDatumFormatter(getUnits().getDatumFormatterFactory().newFormatter(formatString));
            }
            updateTickV();
            repaint();
            firePropertyChange(PROP_FORMATSTRING, oldFormatString, formatString);
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

    public Lock mutatorLock() {
        return dataRange.mutatorLock();
    }

    /**
     * true if a lock is out and an object is rapidly mutating the object.
     * clients listening for property changes can safely ignore property
     * changes while valueIsAdjusting is true, as they should receive a
     * final propertyChangeEvent after the lock is released.  (note it's not
     * clear who is responsible for this.
     * See http://www.das2.org/wiki/index.php/Das2.valueIsAdjusting)
     */
    public boolean valueIsAdjusting() {
        return dataRange.valueIsAdjusting();
    }
}
