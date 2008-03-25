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

package edu.uiowa.physics.pw.das.graph;

import org.das2.util.monitor.NullProgressMonitor;
import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.DasNameException;
import edu.uiowa.physics.pw.das.DasPropertyException;
import edu.uiowa.physics.pw.das.NameContext;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.system.UserMessageCenter;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

import javax.swing.border.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.*;
import javax.swing.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import edu.uiowa.physics.pw.das.system.DasLogger;
import java.util.logging.Logger;

/** TODO
 * @author eew
 */
public class DasAxis extends DasCanvasComponent implements DataRangeSelectionListener, TimeRangeSelectionListener, Cloneable {
    
    
    /*
     * PUBLIC CONSTANT DECLARATIONS
     */
    
    /** This value indicates that the axis should be located at the top of its cell */
    public static final int TOP = 1;
    
    /** This value indicates that the axis should be located at the bottom of its cell */
    public static final int BOTTOM = 2;
    
    /** This value indicates that the axis should be located to the left of its cell */
    public static final int LEFT = 3;
    
    /** This value indicateds that the axis should be located to the right of its cell */
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
    
    private String PROPERTY_TICKS= "ticks";
    
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
    private int tickDirection=1;  // 1=down or left, -1=up or right
    protected String axisLabel = "";
    protected TickVDescriptor tickV;
    protected boolean autoTickV = true;
    private boolean ticksVisible = true;
    private boolean tickLabelsVisible = true;
    private boolean oppositeAxisVisible= false;
    protected DatumFormatter datumFormatter = DefaultDatumFormatterFactory.getInstance().defaultFormatter();
    
    private MouseModule zoom=null;
    private PropertyChangeListener dataRangePropertyListener;
    protected JPanel primaryInputPanel;
    protected JPanel secondaryInputPanel;
    private ScanButton scanPrevious;
    private ScanButton scanNext;
    private boolean animated= ("on".equals(DasProperties.getInstance().get("visualCues")));
    
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
    private javax.swing.event.EventListenerList timeRangeListenerList =  null;
    private TimeRangeSelectionEvent lastProcessedEvent=null;
    
    /* TCA RELATED INSTANCE MEMBERS */
    private DataSetDescriptor dsd;
    private VectorDataSet[] tcaData = new VectorDataSet[0];
    private String dataset = "";
    private boolean drawTca;
    private DataRequestThread drt;
    
    /* DEBUGGING INSTANCE MEMBERS */
    private static final boolean DEBUG_GRAPHICS = false;
    private static final Color[] DEBUG_COLORS;
    
    /* true if a lock is out, and an object is animation-interactively adjusting
     * me
     */
    private boolean valueIsAdjusting= false;
    
    public static String PROPERTY_DATUMRANGE="datumRange";
    
    static {
        if (DEBUG_GRAPHICS) {
            DEBUG_COLORS = new Color[] {
                Color.BLACK, Color.RED, Color.GREEN, Color.BLUE,
                Color.GRAY, Color.CYAN, Color.MAGENTA, Color.YELLOW,
            };
        } else {
            DEBUG_COLORS = null;
        }
    }
    private int debugColorIndex = 0;
    
    private DasPlot dasPlot;
    private JMenu favoritesMenu;
    private JMenu backMenu;
    
    private static final Logger logger= DasLogger.getLogger( DasLogger.GRAPHICS_LOG );
    
    /** TODO
     * @param min
     * @param max
     * @param orientation DasAxis.VERTICAL, DasAxis.HORIZONTAL, DasAxis.RIGHT, etc.
     */
    public DasAxis( Datum min, Datum max, int orientation ) {
        this(min, max, orientation, false);
    }
    
    /** TODO
     * @param min
     * @param max
     * @param orientation
     * @param log
     */
    public DasAxis( Datum min, Datum max, int orientation, boolean log) {
        this(orientation);
        dataRange = new DataRange(this,min,max,log);
        addListenersToDataRange( dataRange, dataRangePropertyListener );
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
        addListenersToDataRange( range, dataRangePropertyListener );
        copyFavorites();
        copyHistory();
    }
    
    private void addListenersToDataRange( DataRange range, PropertyChangeListener listener ) {
        range.addPropertyChangeListener("log", listener);
        range.addPropertyChangeListener("minimum", listener);
        range.addPropertyChangeListener("maximum", listener);
        range.addPropertyChangeListener(DataRange.PROPERTY_DATUMRANGE,listener);
        range.addPropertyChangeListener("history", listener);
        range.addPropertyChangeListener("favorites", listener);
    }
    
    public DasAxis( DatumRange range, int orientation ) {
        this( range.min(), range.max(), orientation );
    }
    
    private DasAxis(int orientation) {
        super();
        setOpaque(false);
        setOrientationInternal(orientation);
        installMouseModules();
        if ( ! DasApplication.getDefaultApplication().isHeadless() ) {
            backMenu= new JMenu("Back");
            mouseAdapter.addMenuItem(backMenu);
            favoritesMenu= new JMenu("Favorites");
            mouseAdapter.addMenuItem(favoritesMenu);
        }
        dataRangePropertyListener = createDataRangePropertyListener();
        setLayout(new AxisLayoutManager());
        maybeInitializeInputPanels();
        maybeInitializeScanButtons();
        add(primaryInputPanel);
        add(secondaryInputPanel);
    }
    
    public void addToFavorites( final DatumRange range ) {
        dataRange.addToFavorites(range);
        copyFavorites();
    }
    
    private void copyFavorites() {
        if ( DasApplication.getDefaultApplication().isHeadless() ) return;
        favoritesMenu.removeAll();
        List favorites= dataRange.getFavorites();
        for ( Iterator i=favorites.iterator(); i.hasNext(); ) {
            final DatumRange r= (DatumRange)i.next(); // copied code from addToFavorites
            Action action= new AbstractAction(r.toString()) {
                public void actionPerformed( ActionEvent e ) {
                    DasAxis.this.setDatumRange(r);
                }
            };
            JMenuItem menuItem= new JMenuItem(action);
            favoritesMenu.add(menuItem);
        }
        Action action= new AbstractAction("add to favorites") {
            public void actionPerformed( ActionEvent e ) {
                DasAxis.this.addToFavorites(DasAxis.this.getDatumRange());
            }
        };
        JMenuItem addItem= new JMenuItem(action);
        favoritesMenu.add(addItem);
    }
    
    private void copyHistory( ) {
        if ( DasApplication.getDefaultApplication().isHeadless() ) return;
        backMenu.removeAll();
        List history= dataRange.getHistory();
        int ii=0;
        for ( Iterator i=history.iterator(); i.hasNext(); ) {
            final int ipop= ii;
            final DatumRange r= (DatumRange)i.next(); // copied code from addToFavorites
            Action action= new AbstractAction(r.toString()) {
                public void actionPerformed( ActionEvent e ) {
                    dataRange.popHistory(ipop);
                    DasAxis.this.setDataRangePrev();
                }
            };
            JMenuItem menuItem= new JMenuItem(action);
            backMenu.add(menuItem);
            ii++;
        }
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
        if ( ! DasApplication.getDefaultApplication().isHeadless() ) {
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
                DasLogger.getLogger(DasLogger.GUI_LOG).info("event "+command);
                if (command.equals(SCAN_PREVIOUS_LABEL)) {
                    scanPrevious();
                } else if (command.equals(SCAN_NEXT_LABEL)) {
                    scanNext();
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
                if (propertyName.equals("log")) {
                    update();
                    firePropertyChange("log", oldValue, newValue);
                } else if (propertyName.equals("minimum")) {
                    update();
                    firePropertyChange("dataMinimum", oldValue, newValue);
                } else if (propertyName.equals("maximum")) {
                    update();
                    firePropertyChange("dataMaximum", oldValue, newValue);
                } else if ( propertyName.equals("favorites") ) {
                    copyFavorites();
                } else if ( propertyName.equals(DataRange.PROPERTY_DATUMRANGE) ) {
                    update();
                    firePropertyChange(PROPERTY_DATUMRANGE,oldValue,newValue);
                } else if ( propertyName.equals("history") ) {
                    if ( !dataRange.valueIsAdjusting() ) copyHistory();
                }
                markDirty();
            }
        };
    }
    
    private void installMouseModules() {
        if (zoom instanceof HorizontalRangeSelectorMouseModule) {
            ((HorizontalRangeSelectorMouseModule)zoom).removeDataRangeSelectionListener(this);
            mouseAdapter.removeMouseModule(zoom);
        } else if (zoom instanceof VerticalRangeSelectorMouseModule) {
            ((VerticalRangeSelectorMouseModule)zoom).removeDataRangeSelectionListener(this);
            mouseAdapter.removeMouseModule(zoom);
        }
        if (isHorizontal()) {
            zoom= new HorizontalRangeSelectorMouseModule(this,this);
            ((HorizontalRangeSelectorMouseModule)zoom).addDataRangeSelectionListener(this);
            mouseAdapter.addMouseModule(zoom);
            mouseAdapter.setPrimaryModule(zoom);
            
            MouseModule zoomPan= new ZoomPanMouseModule(this,null);
            mouseAdapter.addMouseModule( zoomPan );
            mouseAdapter.setSecondaryModule(zoomPan);
        } else {
            zoom= new VerticalRangeSelectorMouseModule(this,this);
            ((VerticalRangeSelectorMouseModule)zoom).addDataRangeSelectionListener(this);
            mouseAdapter.addMouseModule(zoom);
            mouseAdapter.setPrimaryModule(zoom);
            
            MouseModule zoomPan=  new ZoomPanMouseModule( null, this );
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
    
    public void setDatumRange( DatumRange dr ) {
        this.setDataRange( dr.min(), dr.max() );
    }
    
    public DatumRange getDatumRange() {
        return dataRange.getDatumRange();
    }
    
    /*
     * @returns true is the range is acceptible, false otherwise.  This method
     * is overriden by DasLabelAxis.
     */
    protected boolean rangeIsAcceptable( DatumRange dr ) {
        return dr.min().lt( dr.max() );
    }
    
    /** TODO
     * @param minimum
     * @param maximum
     */
    public void setDataRange(Datum minimum, Datum maximum) {
        
        Units units= dataRange.getUnits();
        if (minimum.getUnits()!=units) {
            minimum= minimum.convertTo(units);
            maximum= maximum.convertTo(units);
        }
        
        DatumRange newRange= new DatumRange( minimum, maximum );
        logger.fine( "enter dasAxis.setDataRange( "+newRange+" )" );
        
        if ( ! rangeIsAcceptable( newRange ) ) {
            logger.warning( "invalid range ignored" );
            return;
        }
        
        double min, max, min0, max0;
        
        min0= dataRange.getMinimum();
        max0= dataRange.getMaximum();
        
        if ( dataRange.isLog() ) {
            min= DasMath.log10( minimum.doubleValue( getUnits() ) );
            max= DasMath.log10( maximum.doubleValue( getUnits() ) );
        } else {
            min= minimum.doubleValue( getUnits() );
            max= maximum.doubleValue( getUnits() );
        }
        
        if ( !valueIsAdjusting() ) animateChange( min0, max0, min, max );
        DatumRange oldRange= dataRange.getDatumRange();
        dataRange.setRange( newRange );
        
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange( PROPERTY_DATUMRANGE, oldRange, newRange );
    }
    
    public void clearHistory() {
        dataRange.clearHistory();
    }
    
    private void createAndFireRangeSelectionEvent() {
        if (getUnits() instanceof TimeLocationUnits) {
            logger.fine( "firing rangeSelectionEvent" );
            TimeRangeSelectionEvent e= new TimeRangeSelectionEvent(this, new DatumRange( this.getDataMinimum(), this.getDataMaximum() ) );
            fireTimeRangeSelectionListenerTimeRangeSelected(e);
        }
    }
    
    /** TODO */
    public void setDataRangePrev() {
        logger.fine( "enter dasAxis.setDataRangePrev()" );
        DatumRange oldRange= dataRange.getDatumRange();
        double min0= dataRange.getMinimum();
        double max0= dataRange.getMaximum();
        dataRange.setRangePrev();
        DatumRange newRange= dataRange.getDatumRange();
        double min1= dataRange.getMinimum();
        double max1= dataRange.getMaximum();
        animateChange(min0,max0,min1,max1);
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange( PROPERTY_DATUMRANGE, oldRange, newRange );
    }
    
    /** TODO */
    public void setDataRangeForward() {
        logger.fine( "enter dasAxis.setDataRangeForward()" );
        double min0= dataRange.getMinimum();
        double max0= dataRange.getMaximum();
        DatumRange oldRange= dataRange.getDatumRange();
        dataRange.setRangeForward();
        DatumRange newRange= dataRange.getDatumRange();
        double min1= dataRange.getMinimum();
        double max1= dataRange.getMaximum();
        animateChange(min0,max0,min1,max1);
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange( PROPERTY_DATUMRANGE, oldRange, newRange );
    }
    
    /** TODO */
    public void setDataRangeZoomOut() {
        logger.fine( "enter dasAxis.setDataRangeZoomOut()" );
        double t1= dataRange.getMinimum();
        double t2= dataRange.getMaximum();
        double delta= t2-t1;
        double min= t1-delta;
        double max= t2+delta;
        animateChange(t1,t2,min,max);
        DatumRange oldRange= dataRange.getDatumRange();
        dataRange.setRange(min,max);
        DatumRange newRange= dataRange.getDatumRange();
        update();
        createAndFireRangeSelectionEvent();
        firePropertyChange( PROPERTY_DATUMRANGE, oldRange, newRange );
    }
    
    /** TODO
     * @return
     */
    public DataRange getDataRange() {
        return this.dataRange;
    }
    
    /** TODO */
    protected void deviceRangeChanged() {}
    
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
        dataRange.setMaximum( max );
        update();
    }
    
    /** TODO
     * @param min
     */
    public void setDataMinimum(Datum min) {
        dataRange.setMinimum( min );
        update();
    }
    
    /** TODO
     * @return
     */
    public boolean isLog() {
        return dataRange.isLog();
    }
    
    /** TODO
     * @param log
     */
    public void setLog(boolean log) {
        boolean oldLog = isLog();
        dataRange.setLog(log);
        update();
        if (log != oldLog) firePropertyChange("log", oldLog, log);
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
     * changes the units of the axis to a new unit, probably breaking all sorts of things!
     */
    public void resetRange( DatumRange range ) {
        if ( range.getUnits()!=this.getUnits() ) {
            if ( dasPlot!=null ) dasPlot.invalidateCacheImage();
            logger.finest( "replaceRange("+range+")");
            dataRange.resetRange( range );
        } else {
            dataRange.setRange( range );
        }
        updateTickV();
        markDirty();
        update();
    }
    
    /** TODO
     * @param visible
     */
    public void setOppositeAxisVisible(boolean visible) {
        if (visible == oppositeAxisVisible) return;
        boolean oldValue = oppositeAxisVisible;
        oppositeAxisVisible = visible;
        revalidate();
        repaint();
        firePropertyChange("oppositeAxisVisible", oldValue, visible);
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
        if (t == null) throw new NullPointerException("axis label cannot be null");
        Object oldValue = axisLabel;
        axisLabel = t;
        update();
        firePropertyChange("label", oldValue, t);
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
        if (b && getOrientation() != BOTTOM)
            throw new IllegalArgumentException("Vertical time axes cannot have annotations");
        if (drawTca==b) return;
        drawTca = b;
        markDirty();
        update();
        firePropertyChange("showTca", oldValue, b);
    }
    
    public String getDataPath() {
        return dataset;
    }
    
    /**
     *
     * @param dataset The URL identifier string of a TCA data set, or "" for no TCAs.
     */
    public void setDataPath(String dataset) {
        if (dataset == null) throw new NullPointerException("null dataPath string not allowed");
        Object oldValue = this.dataset;
        if (dataset.equals(this.dataset)) return;
        this.dataset=dataset;
        if (dataset.equals("")) {
            dsd = null;
        } else {
            try {
                dsd = DataSetDescriptor.create(dataset);
            } catch (edu.uiowa.physics.pw.das.DasException de) {
                DasExceptionHandler.handle(de);
            }
        }
        markDirty();
        update();
        firePropertyChange("dataPath", oldValue, dataset);
    }
	 
	 /** Add auxilary data to an axis (usually OA data for a time axis).
	  * This function does the same thing as setDataPath, but with a different interface.
	  * @param will be called upon to generate auillary data sets.  To avoid nonsensical
	  * graphs the X axis for this dataset must be the same as the that handed to the
	  * renderer.
	  */
	 public void setDataSetDescriptor(DataSetDescriptor dsdAux) {
		if(dsdAux == null) throw new NullPointerException("null DataSetDescriptor not allowed");
		
		DataSetDescriptor oldVal = dsd;
		dsd = dsdAux;
		markDirty();
		update();
		
		String oldDataset = dataset;
		dataset = dsd.getDataSetID();
		firePropertyChange("dataSetDescriptor", oldVal, dsd);
		firePropertyChange("dataPath", oldDataset, dataset);
	}
    
    private final DataSetUpdateListener tcaListener= new DataSetUpdateListener() {
        public void dataSetUpdated(DataSetUpdateEvent e) {
            VectorDataSet ds = (VectorDataSet)e.getDataSet();
            if ( ds==null ) {
                UserMessageCenter.getDefault().notifyUser(DasAxis.this,e.getException());
                return;
            }
            logger.fine("got TCADataSet");
            List itemList = (List)ds.getProperty("plane-list");
            VectorDataSet[] newData = new VectorDataSet[itemList.size()];
            newData[0] = ds;
            for (int i = 1; i < itemList.size(); i++) {
                newData[i] = (VectorDataSet)ds.getPlanarView((String)itemList.get(i));
            }
            tcaData = newData;
            update();
        }
    };
    
    private void updateTCADataSet() {
        logger.fine("updateTCADataSet");
        double [] tickV = getTickV().tickV.toDoubleArray(getUnits());
        Datum data_minimum;
        Datum data_maximum;
        Datum iinterval;
        if (tickV.length == 1) {
            data_minimum = Datum.create(tickV[0],getTickV().units);
            data_maximum = Datum.create(tickV[0], getTickV().units);
            iinterval = data_maximum.subtract(data_minimum);
        } else {
            data_minimum = Datum.create(tickV[0],getTickV().units);
            data_maximum = Datum.create(tickV[tickV.length-1],getTickV().units);
            iinterval = (data_maximum.subtract(data_minimum)).divide(tickV.length-1);
        }
        data_maximum= data_maximum.add(iinterval);
        final Datum interval = iinterval;
        tcaData = null;
        
        this.dsd.requestDataSet( data_minimum, data_maximum.add(Datum.create(1.0,Units.seconds)), interval, new NullProgressMonitor(), getCanvas(), tcaListener );
        
/*        DataRequestor requestor = new DataRequestor() {
 
            public void currentByteCount(int byteCount) {
            }
            public void totalByteCount(int byteCount) {
            }
            public void exception(Exception exception) {
                if (!(exception instanceof java.io.InterruptedIOException)) {
                    if (exception instanceof edu.uiowa.physics.pw.das.DasException ) {
                        DasExceptionHandler.handle(exception);
                        finished(null);
                    } else {
                        Object[] message = {"Error reading data set", new javax.swing.JEditorPane("text/html", exception.getMessage())};
                        ((javax.swing.JEditorPane)message[1]).setEditable(false);
                        //JOptionPane.showMessageDialog(DasPlot.this, message);
                        DasExceptionHandler.handle(exception);
                        finished(null);
                    }
                }
            }
            public void finished(DataSet dsFinished) {
                if (dsFinished != null) {
                    VectorDataSet ds = (VectorDataSet)dsFinished;
                    List itemList = (List)ds.getProperty("plane-list");
                    VectorDataSet[] newData = new VectorDataSet[itemList.size()];
                    newData[0] = ds;
                    for (int i = 1; i < itemList.size(); i++) {
                        newData[i] = (VectorDataSet)ds.getPlanarView((String)itemList.get(i));
                    }
                    tcaData = newData;
                    update();
                }
            }
        };
        if (drt == null) {
            drt = new DataRequestThread();
        }
        try {
            drt.request(dsd, data_minimum,
                    data_maximum.add(Datum.create(1.0,Units.seconds)),
                    interval, requestor, null );
        } catch (InterruptedException ie) {
            DasExceptionHandler.handle(ie);
        }
 */
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
        if (isHorizontal())
            return getColumn().getWidth();
        else
            return getRow().getHeight();
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
        oldRange.removePropertyChangeListener("log", dataRangePropertyListener);
        oldRange.removePropertyChangeListener("minimum", dataRangePropertyListener);
        oldRange.removePropertyChangeListener("maximum", dataRangePropertyListener);
        oldRange.removePropertyChangeListener(DataRange.PROPERTY_DATUMRANGE,dataRangePropertyListener);
        dataRange.addPropertyChangeListener("log", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("maximum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener(DataRange.PROPERTY_DATUMRANGE,dataRangePropertyListener);
        if (oldRange.isLog() != dataRange.isLog()) {
            firePropertyChange("log", oldRange.isLog(), dataRange.isLog());
        }
        firePropertyChange("minimum", oldRange.getMinimum(), dataRange.getMinimum());
        firePropertyChange("maximum", oldRange.getMaximum(), dataRange.getMaximum());
        copyFavorites();
        copyHistory();
    }
    
    /** TODO */
    public void detach() {
        dataRange.removePropertyChangeListener("log", dataRangePropertyListener);
        dataRange.removePropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.removePropertyChangeListener("maximum", dataRangePropertyListener);
        dataRange.removePropertyChangeListener(DataRange.PROPERTY_DATUMRANGE, dataRangePropertyListener);
        DataRange newRange
                = new DataRange(this,Datum.create(dataRange.getMinimum(), dataRange.getUnits()),
                Datum.create(dataRange.getMaximum(), dataRange.getUnits()),
                dataRange.isLog());
        dataRange = newRange;
        dataRange.addPropertyChangeListener("log", dataRangePropertyListener);
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
    public TickVDescriptor getTickV() {
        if (tickV == null) updateTickV();
        return tickV;
    }
    
    /**
     * Sets the TickVDescriptor for this axis.  If null is passed in, the
     * axis will put into autoTickV mode, where the axis will attempt to
     * determine ticks using an appropriate algortithm.
     *
     * @param tickV the new ticks for this axis, or null
     */
    public void setTickV(TickVDescriptor tickV) {
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
        
        double min= getDataMinimum().doubleValue(getUnits());
        double max= getDataMaximum().doubleValue(getUnits());
        
        double dMinTick= DasMath.roundNFractionalDigits(DasMath.log10(min),4);
        int minTick= (int)Math.ceil(dMinTick);
        double dMaxTick= DasMath.roundNFractionalDigits(DasMath.log10(max),4);
        int maxTick= (int)Math.floor(dMaxTick);
        
        GrannyTextRenderer idlt= new GrannyTextRenderer( );
        idlt.setString(this, "10!U-10");
        
        int nTicksMax;
        if ( isHorizontal() ) {
            nTicksMax= (int)Math.floor( getColumn().getWidth() / ( idlt.getWidth() ) );
        } else {
            nTicksMax= (int)Math.floor( getRow().getHeight() / ( idlt.getHeight() ) );
        }
        
        nTicksMax= (nTicksMax<7)?nTicksMax:7;
        
        tickV= TickVDescriptor.bestTickVLogNew( getDataMinimum(), getDataMaximum(), 3, nTicksMax );
        datumFormatter= tickV.getFormatter();
        
        return;
        
    }
    
    private void updateTickVLinear() {
        int nTicksMax;
        int axisSize;
        if (isHorizontal()) {
            int tickSizePixels= (int)(getFontMetrics(getTickLabelFont()).stringWidth("0.0000")*1.5) ;
            axisSize= getColumn().getWidth();
            nTicksMax= axisSize / tickSizePixels;
        } else {
            int tickSizePixels= getFontMetrics(getTickLabelFont()).getHeight() + 6;
            axisSize= getRow().getHeight();
            nTicksMax= axisSize / tickSizePixels;
        }
        
        nTicksMax= (nTicksMax<7)?nTicksMax:7;
        
        this.tickV= TickVDescriptor.bestTickVLinear( getDataMinimum(), getDataMaximum(), 3, nTicksMax );
        
        datumFormatter= tickV.getFormatter();
        
        boolean once=true;
        
        while( once ) {
            Rectangle maxBounds= new Rectangle(0,0,0,0);
            DatumVector majorTicks= tickV.getMajorTicks();
            for ( int i=0; i<majorTicks.getLength(); i++ ) {
                GrannyTextRenderer gtr= new GrannyTextRenderer( );
                gtr.setString(this,datumFormatter.grannyFormat(majorTicks.get(i)));
                Rectangle r= gtr.getBounds();
                maxBounds.add(r);
            }
            
            if ( isHorizontal() ) {
                int tickSizePixels= (int) ( maxBounds.width  + getEmSize() * 2 );
                nTicksMax= axisSize / tickSizePixels;
            } else {
                int tickSizePixels= (int) ( maxBounds.height );
                nTicksMax= axisSize / tickSizePixels;
            }
            
            this.tickV= TickVDescriptor.bestTickVLinear( getDataMinimum(), getDataMaximum(), 3, nTicksMax );
            datumFormatter= tickV.getFormatter();
            
            once= false;
        }
        
        
        return;
        
    }
    
    private void updateTickVTime() {
        
        int nTicksMax;
        TickVDescriptor saveTickV;  // use these if overlap
        
        DatumRange dr= getDatumRange();
        Datum pixel= dr.width().divide( getDLength() );
        
        if (isHorizontal()) {
            // two passes to avoid clashes -- not guarenteed
            tickV= TickVDescriptor.bestTickVTime( dr.min().subtract(pixel), dr.max().add(pixel), 3, 8 );
            Datum atick= tickV.getMajorTicks().get(0);
            String granny= tickV.getFormatter().grannyFormat(atick);
            
            GrannyTextRenderer idlt= new GrannyTextRenderer();
            idlt.setString(this, granny );
            int tickSizePixels= (int) idlt.getWidth();
            
            if ( drawTca ) {
                FontMetrics fm = getFontMetrics(getTickLabelFont());
                String item = format( 99999.99, "(f8.2)");
                int width = fm.stringWidth(item);
                if ( width>tickSizePixels ) tickSizePixels= width;
            }
            
            int axisSize= getColumn().getWidth();
            nTicksMax= axisSize / tickSizePixels;
            
            tickV= TickVDescriptor.bestTickVTime( getDataMinimum(), getDataMaximum(), 3, nTicksMax );
            datumFormatter= tickV.getFormatter();
            atick= tickV.getMajorTicks().get(0);
            granny= tickV.getFormatter().grannyFormat(atick);
            
            idlt.setString(this, granny );
            tickSizePixels= (int) idlt.getWidth();
            if ( drawTca ) {
                FontMetrics fm = getFontMetrics(getTickLabelFont());
                String item = format( 99999.99, "(f8.2)");
                int width = fm.stringWidth(item);
                if ( width>tickSizePixels ) tickSizePixels= width;
            }
            nTicksMax= axisSize / tickSizePixels;
            
            nTicksMax= ( nTicksMax>1 ? nTicksMax : 2 ) ;
            nTicksMax= ( nTicksMax<10 ? nTicksMax : 10 ) ;
            
            boolean overlap= true;
            while ( overlap && nTicksMax>2 ) {
                
                tickV= TickVDescriptor.bestTickVTime( getDataMinimum(), getDataMaximum(), 2, nTicksMax );
                atick= tickV.getMajorTicks().get(0);
                
                if ( tickV.getMajorTicks().getLength()<= 1 ) {
                    // we're about to have an assertion error, time to debug;
                    System.err.println("about to assert error: "+tickV.getMajorTicks());
                }
                assert ( tickV.getMajorTicks().getLength()>1 );
                
                granny= tickV.getFormatter().grannyFormat(atick);
                
                idlt.setString(this.getGraphics(), granny );
                tickSizePixels= (int) ( idlt.getWidth() + getEmSize() * 2 );
                
                double x0= transform( tickV.getMajorTicks().get(0) );
                double x1= transform( tickV.getMajorTicks().get(1) );
                
                if ( x1-x0 > tickSizePixels ) {
                    overlap= false;
                } else {
                    nTicksMax= nTicksMax - 1;
                }
            }
            
        } else {
            int tickSizePixels= getFontMetrics(getTickLabelFont()).getHeight();
            int axisSize= getRow().getHeight();
            nTicksMax= axisSize / tickSizePixels;
            
            nTicksMax= ( nTicksMax>1 ? nTicksMax : 2 ) ;
            nTicksMax= ( nTicksMax<10 ? nTicksMax : 10 ) ;
            
            tickV= TickVDescriptor.bestTickVTime( getDataMinimum(), getDataMaximum(), 3, nTicksMax );
            
        }
        
        datumFormatter= tickV.getFormatter();
        
        if ( drawTca && !dataset.equals("") && dsd!=null) updateTCADataSet();
    }
    
    public void updateTickV() {
        if (autoTickV) {
            TickVDescriptor oldTicks= this.tickV;
            if (getUnits() instanceof TimeLocationUnits) {
                updateTickVTime();
            } else if (dataRange.isLog()) {
                updateTickVLog();
            } else {
                updateTickVLinear();
            }
            firePropertyChange( PROPERTY_TICKS, oldTicks, this.tickV );
        }
        
    }
    
    private String errorMessage;
    
    /**
     * checks the validity of the state, setting variable errorMessage to non-null if there is a problem.
     */
    private void checkState() {
        double dmin=  getDataMinimum(dataRange.getUnits());
        double dmax= getDataMaximum(dataRange.getUnits());
        
        String em="";
        
        if ( Double.isNaN(dmin) ) em+="dmin is NaN, ";
        if ( Double.isNaN(dmax) ) em+="dmax is NaN, ";
        
        if ( Double.isInfinite(dmin) ) em+="dmin is infinite, ";
        if ( Double.isInfinite(dmax) ) em+="dmax is infinite, ";
        
        if ( dmin>=dmax ) {
            em+= "min => max, ";
        }
        if ( dataRange.isLog() && dmin<=0 ) {
            em+= "min<= 0 and log, ";
        }
        
        if (em.length()==0 ) {
            this.errorMessage= null;
        } else {
            this.errorMessage= em;
        }
    }
    
    /** paints the axis component.  The tickV's and bounds should be calculated at this point */
    protected void paintComponent(Graphics graphics) {
        logger.info("enter DasAxis.paintComponent");
        /* This was code was keeping axes from being printed on PC's
        Shape saveClip = null;
        if (getCanvas().isPrintingThread()) {
            saveClip = graphics.getClip();
            graphics.setClip(null);
        }
         */
        logger.info( "DasAxis clip="+ graphics.getClip() );
        
        Graphics2D g = (Graphics2D)graphics.create();
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
            if (blLabelRect != null) g.draw(blLabelRect);
            g.setColor(Color.RED);
            if (blLineRect != null) g.draw(blLineRect);
            g.setColor(Color.GREEN);
            if (blTickRect != null) g.draw(blTickRect);
            g.setColor(Color.LIGHT_GRAY);
            if (blTitleRect != null) g.draw(blTitleRect);
            g.setColor(Color.BLUE);
            if (trLabelRect != null) g.draw(trLabelRect);
            g.setColor(Color.RED);
            if (trLineRect != null) g.draw(trLineRect);
            g.setColor(Color.GREEN);
            if (trTickRect != null) g.draw(trTickRect);
            g.setColor(Color.LIGHT_GRAY);
            if (trTitleRect != null) g.draw(trTitleRect);
            g.setStroke(new BasicStroke(1f));
            g.setColor(DEBUG_COLORS[debugColorIndex]);
            debugColorIndex++;
            if (debugColorIndex >= DEBUG_COLORS.length) { debugColorIndex = 0; }
        }
        /* End debugging code */
        
        if ( tickV==null || tickV.tickV.getUnits().isConvertableTo( getUnits() ) ) {
            if (isHorizontal()) {
                paintHorizontalAxis(g);
            } else {
                paintVerticalAxis(g);
            }
        } else {
            //System.err.println("inconvertable units");
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
            int maxLabelWidth = getMaxLabelWidth(tickLabelFontMetrics);
            
            int baseLine = position + tickLength + tick_label_gap + tickLabelFont.getSize();
            int rightEdge = DMin - tickLabelFontMetrics.stringWidth("0000") - tick_label_gap;
            
            GrannyTextRenderer idlt = new GrannyTextRenderer();
            /*
            idlt.setString(this, "SCET");
            int width = (int)Math.ceil(idlt.getWidth());
            int leftEdge = rightEdge - width;
            idlt.draw(g, (float)leftEdge, (float)baseLine);
             */
            int width, leftEdge;
            
            for (int i = 0; i < tcaData.length; i++) {
                baseLine += lineHeight;
                idlt.setString(this, (String)tcaData[i].getProperty("label"));
                width = (int)Math.floor(idlt.getWidth() + 0.5);
                leftEdge = rightEdge - width;
                idlt.draw(g, (float)leftEdge, (float)baseLine);
            }
        }
        
        g.dispose();
        getDasMouseInputAdapter().paint(graphics);
        
        /* This was code was keeping axes from being printed on PC's
        if (getCanvas().isPrintingThread()) {
            g.setClip(saveClip);
        }
         */
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
            int DMax= getColumn().getDMaximum();
            int DMin= getColumn().getDMinimum();
            
            Font labelFont = getTickLabelFont();
            
            double dataMax= dataRange.getMaximum();
            double dataMin= dataRange.getMinimum();
            
            TickVDescriptor ticks= getTickV();
            
            if (bottomLine) {
                g.drawLine(DMin,bottomPosition,DMax,bottomPosition);
            }
            if (topLine) {
                g.drawLine(DMin,topPosition,DMax,topPosition);
            }
            
            int tickLengthMajor = labelFont.getSize() * 2 / 3;
            int tickLengthMinor = tickLengthMajor / 2;
            int tickLength;
            
            for ( int i=0; i<ticks.tickV.getLength(); i++ ) {
                double tick1= ticks.tickV.doubleValue(i, getUnits());
                int tickPosition= (int)Math.floor(transform(tick1,getUnits()));
                if ( DMin <= tickPosition && tickPosition <= DMax ) {
                    tickLength= tickLengthMajor;
                    if (bottomTicks) {
                        g.drawLine( tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                    }
                    if (bottomTickLabels) {
                        drawLabel(g, tick1, i, tickPosition, bottomPosition + tickLength);
                    }
                    if (topTicks) {
                        g.drawLine( tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    }
                    if (topTickLabels) {
                        drawLabel(g, tick1, i, tickPosition, topPosition - tickLength + 1);
                    }
                }
            }
            
            for ( int i=0; i<ticks.minorTickV.getLength(); i++ ) {
                Datum tick= ticks.minorTickV.get(i);
                int tickPosition= (int)Math.floor(transform(tick));
                if ( DMin <= tickPosition && tickPosition <= DMax ) {
                    tickLength= tickLengthMinor;
                    if (bottomTicks) {
                        g.drawLine( tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                    }
                    if (topTicks) {
                        g.drawLine( tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    }
                }
            }
            
            if (!axisLabel.equals("")) {
                Graphics2D g2 = (Graphics2D)g.create();
                int titlePositionOffset = getTitlePositionOffset();
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                gtr.setString(this, axisLabel);
                int titleWidth = (int)gtr.getWidth();
                int baseline;
                int leftEdge;
                g2.setFont(getLabelFont());
                if (bottomLabel) {
                    leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                    baseline = bottomPosition + titlePositionOffset;
                    gtr.draw(g2, (float)leftEdge, (float)baseline);
                }
                if (topLabel) {
                    leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                    baseline = topPosition - titlePositionOffset;
                    gtr.draw(g2, (float)leftEdge, (float)baseline);
                }
                g2.dispose();
            }
        } catch ( InconvertibleUnitsException ex ) {
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
            int DMax= getRow().getDMaximum();
            int DMin= getRow().getDMinimum();
            
            Font labelFont = getTickLabelFont();
            
            double dataMax= dataRange.getMaximum();
            double dataMin= dataRange.getMinimum();
            
            TickVDescriptor ticks= getTickV();
            
            if (leftLine) {
                g.drawLine(leftPosition,DMin,leftPosition,DMax);
            }
            if (rightLine) {
                g.drawLine(rightPosition,DMin,rightPosition,DMax);
            }
            
            int tickLengthMajor= labelFont.getSize()*2/3;
            int tickLengthMinor = tickLengthMajor / 2;
            int tickLength;
            
            for ( int i=0; i<ticks.tickV.getLength(); i++ ) {
                double tick1= ticks.tickV.doubleValue(i, getUnits());
                int tickPosition= (int)Math.floor(transform(tick1,getUnits()));
                if ( DMin <= tickPosition && tickPosition <= DMax ) {
                    
                    tickLength= tickLengthMajor;
                    if (leftTicks) {
                        g.drawLine( leftPosition, tickPosition, leftPosition - tickLength, tickPosition );
                    }
                    if (leftTickLabels) {
                        drawLabel(g, tick1, i, leftPosition - tickLength, tickPosition);
                    }
                    if (rightTicks) {
                        g.drawLine( rightPosition, tickPosition, rightPosition + tickLength, tickPosition );
                    }
                    if (rightTickLabels) {
                        drawLabel(g, tick1, i, rightPosition + tickLength, tickPosition);
                    }
                }
            }
            
            for ( int i=0; i<ticks.minorTickV.getLength(); i++ ) {
                tickLength= tickLengthMinor;
                double tick1= ticks.minorTickV.doubleValue(i, getUnits());
                int tickPosition= (int)Math.floor(transform(tick1,ticks.units));
                if ( DMin <= tickPosition && tickPosition <= DMax ) {
                    tickLength= tickLengthMinor;
                    if (leftTicks) {
                        g.drawLine( leftPosition, tickPosition, leftPosition - tickLength, tickPosition  );
                    }
                    if (rightTicks) {
                        g.drawLine( rightPosition, tickPosition, rightPosition + tickLength, tickPosition  );
                    }
                }
            }
            
            
            if (!axisLabel.equals("")) {
                Graphics2D g2 = (Graphics2D)g.create();
                int titlePositionOffset = getTitlePositionOffset();
                GrannyTextRenderer gtr = new GrannyTextRenderer();
                gtr.setString(this.getGraphics(), axisLabel);
                int titleWidth = (int)gtr.getWidth();
                int baseline;
                int leftEdge;
                g2.setFont(getLabelFont());
                if (leftLabel) {
                    g2.rotate(-Math.PI/2.0);
                    leftEdge = -DMax + (DMax-DMin - titleWidth)/2;
                    baseline = leftPosition - titlePositionOffset;
                    gtr.draw(g2, (float)leftEdge, (float)baseline);
                }
                if (rightLabel) {
                    g2.rotate(Math.PI/2.0);
                    leftEdge = DMin + (DMax-DMin - titleWidth)/2;
                    baseline = - rightPosition - titlePositionOffset;
                    gtr.draw(g2, (float)leftEdge, (float)baseline);
                }
                g2.dispose();
            }
        } catch ( InconvertibleUnitsException e ) {
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
        gtr.setString(this, axisLabel);
        
        int offset;
        
        if (orientation == BOTTOM) {
            offset = tickLabelFont.getSize() + tickLength + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize()/2;
        } else if (orientation == TOP) {
            offset = tickLength + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize()/2 + (int)gtr.getDescent();
        } else if (orientation == LEFT) {
            offset = tickLength + getMaxLabelWidth(fm) + fm.stringWidth(" ") + labelFont.getSize()/2 + (int)gtr.getDescent();
        } else {
            offset = tickLength + getMaxLabelWidth(fm) + fm.stringWidth(" ") + labelFont.getSize()/2 + (int)gtr.getDescent();
        }
        if (getOrientation() == BOTTOM && drawTca && tcaData != null) {
            offset += tcaData.length * (tickLabelFont.getSize() + getLineSpacing());
            offset += tickLabelFont.getSize() + getLineSpacing();
        }
        return offset;
    }
    
    public int getLineSpacing() {
        return getTickLabelFont().getSize()/4;
    }
    
    /** TODO */
    protected void drawLabel(Graphics graphics, double value, int index, int x, int y) {
        
        Graphics2D g = (Graphics2D)graphics;
        
        AffineTransform atSave = g.getTransform();
        
        if (!tickLabelsVisible) return;
        
        String label = tickFormatter(value);
        
        g.setFont(getTickLabelFont());
        GrannyTextRenderer idlt= new GrannyTextRenderer();
        idlt.setString(this,label);
        
        int width = (int) idlt.getLineOneWidth();
        int height = (int) idlt.getHeight();
        int ascent = (int) idlt.getAscent();
        
        int tick_label_gap = getFontMetrics(getTickLabelFont()).stringWidth(" ");
        
        if (orientation == BOTTOM) {
            x -= width/2;
            y += getTickLabelFont().getSize() + tick_label_gap;
        } else if (orientation == TOP) {
            x -= width/2;
            y -= tick_label_gap + idlt.getDescent();
        } else if (orientation == LEFT) {
            x -= (width + tick_label_gap);
            y += ascent - height/2;
        } else {
            x += tick_label_gap;
            y += ascent - height/2;
        }
        Color c = g.getColor();
        idlt.draw(g,x,y);
        if (orientation == BOTTOM && drawTca && tcaData != null) {
            drawTCAItems(g, value, x, y, width);
        }
    }
    
    private void drawTCAItems(Graphics g, double value, int x, int y, int width) {
        int index;
        int height;
        int tick_label_gap;
        int baseLine, leftEdge, rightEdge;
        double pixelSize;
        double tcaValue;
        
        if (tcaData == null || tcaData.length == 0) {
            return;
        }
        
        height = getTickLabelFont().getSize();
        tick_label_gap = getFontMetrics(getTickLabelFont()).stringWidth(" ");
        baseLine = y;
        leftEdge = x;
        rightEdge = leftEdge + width;
        
        index = DataSetUtil.closestColumn(tcaData[0], value, getUnits());
        if (index < 0 || index > tcaData[0].getXLength()) {
            return;
        }
        pixelSize = getDatumRange().width().divide(getDLength()).doubleValue(
			                                                   getUnits().getOffsetUnits());
        tcaValue = tcaData[0].getXTagDouble(index, getUnits());
		  
		  //Added in to say take nearest nieghbor as long as the distance to the nieghbor is
		  //not more than the xtagwidth.
		  double xTagWidth = DataSetUtil.guessXTagWidth(tcaData[0]).doubleValue(
			                  getUnits().getOffsetUnits());
		  double limit = Math.max(xTagWidth, pixelSize);
		  
        if (Math.abs(tcaValue - value) > limit) {
            return;
        }
        
        Font tickLabelFont = getTickLabelFont();
        FontMetrics fm = getFontMetrics(tickLabelFont);
        int lineHeight = tickLabelFont.getSize() + getLineSpacing();
        for (int i = 0; i < tcaData.length; i++) {
            baseLine += lineHeight;
            String item = format(tcaData[i].getDouble(index, tcaData[i].getYUnits()), "(f8.2)");
            width = fm.stringWidth(item);
            leftEdge = rightEdge - width;
            g.drawString(item, leftEdge, baseLine);
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
    
    public class Memento {
        private DatumRange range;
        private int dmin, dmax;
        private boolean log;
        private boolean flipped;
        private DasAxis axis;
        public boolean equals( Object o ) {
            Memento m= (Memento)o;
            return this==m || (
                    this.range.equals(m.range) &&
                    this.dmin==m.dmin &&
                    this.dmax==m.dmax &&
                    this.log==m.log &&
                    this.flipped==m.flipped &&
                    this.axis==m.axis );
        }
        public String toString() {
            return ( log ? "log " : "" ) + range.toString()+" "+(dmax-dmin)+" pixels @ "+dmin;
        }
    }
    
    
    public Memento getMemento() {
        Memento result= new Memento();
        result.range= this.getDatumRange();
        if ( isHorizontal() ) {
            if ( getColumn()!=DasColumn.NULL ) {
                result.dmin= getColumn().getDMinimum();
                result.dmax= getColumn().getDMaximum();
            } else {
                result.dmin= 0;
                result.dmax= 0;
            }
        } else {
            if ( getRow()!=DasRow.NULL ) {
                result.dmin= getRow().getDMinimum();
                result.dmax= getRow().getDMaximum();
            } else {
                result.dmin= 0;
                result.dmax= 0;
            }
        }
        result.log= this.isLog();
        result.flipped = flipped;
        result.axis= this;
        return result;
    }
    
    /**
     * return the AffineTransform, or null.  The transform will be applied after the input
     * transform is applied.  So to just get the transform, pass in identity.
     */
    public AffineTransform getAffineTransform( Memento memento, AffineTransform at ) {
        if ( at==null ) return null;
        if ( memento.log!=isLog() ) return null;
        if ( memento.flipped!=flipped ) return null;
        if ( ! memento.range.getUnits().isConvertableTo(getUnits() ) ) return null;
        
        double dmin0= transform(memento.range.min());  // old axis in new axis space
        double dmax0= transform(memento.range.max());
        if (!(isHorizontal() ^ flipped)) {
            double tmp = dmin0;
            dmin0 = dmax0;
            dmax0 = tmp;
        }
        
        if ( !isHorizontal() ) {
            double dmin1= getRow().getDMinimum();
            double dmax1= getRow().getDMaximum();
            double scaley= ( dmin0 - dmax0 ) / ( dmin1 - dmax1 );
            double transy= -1* dmin1 * scaley + dmin0;
            at.translate( 0., transy );
            at.scale( 1., scaley );
            
            double swingscaley= ( dmin1 - dmax1 ) / ( memento.dmin - memento.dmax );
            double swingtransy= -1* memento.dmin * swingscaley + dmin1;
            
            //at.translate( 0., -swingtransy );
            //at.scale( 1., swingscaley );
            
            //if ( dmin1!=memento.dmin ) {
            //     System.err.println( ""+dmin1 +"  " + memento.dmin + ( dmin1!=memento.dmin )  );
            //}
            
        } else {
            double dmin1= getColumn().getDMinimum();
            double dmax1= getColumn().getDMaximum();
            
            double scalex= ( dmin0 - dmax0 ) / ( dmin1 - dmax1 );
            double transx= -1* dmin1 * scalex + dmin0;
            at.translate( transx, 0 );
            at.scale( scalex, 1. );
            
            //double swingscalex= ( dmin1 - dmax1 ) / ( memento.dmin - memento.dmax );
            //double swingtransx= -1* memento.dmin * swingscalex + dmin1;
            
            //at.translate( -swingtransx, 0 );
            //at.scale( swingscalex, 1. );
            
            //if ( dmin1!=memento.dmin ) {
            //    System.err.println( ""+dmin1 +"  " + memento.dmin + ( dmin1!=memento.dmin )  );
            //}
            
        }
        
        if ( at.getDeterminant() == 0.000 ) {
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
            DasAxis result= (DasAxis)super.clone();
            result.dataRange= (DataRange)result.dataRange.clone();
            return result;
        } catch (CloneNotSupportedException e) {
            throw new Error("Assertion failure");
        }
    }
    
    private void setTickDirection(int direction) {
        if (direction ==  UP || direction == RIGHT) {
            tickDirection=-1;
        } else if (direction == DOWN || direction == LEFT) {
            tickDirection=1;
        } else {
            throw new IllegalArgumentException("Invalid tick direction");
        }
    }
    
    /** TODO
     * @param fm
     * @return the width in pixels of the widest label.
     */
    protected int getMaxLabelWidth(FontMetrics fm) {
        try {
            TickVDescriptor ticks = getTickV();
            DatumVector tickv = ticks.tickV;
            int size = Integer.MIN_VALUE;
            Graphics g= this.getGraphics();
            for (int i = 0; i < tickv.getLength(); i++) {
                String label = tickFormatter(tickv.doubleValue(i, getUnits()));
                GrannyTextRenderer idlt = new GrannyTextRenderer();
                idlt.setString( g, label);
                int labelSize = (int)Math.round(idlt.getWidth());
                if (labelSize > size) size = labelSize;
            }
            return size;
        } catch ( InconvertibleUnitsException ex ) {
            return 10;
        }
    }
    
    /** TODO */
    public void resize() {
        resetTransform();
        setBounds(getAxisBounds());
        invalidate();
        if ( tickV==null || tickV.tickV.getUnits().isConvertableTo(getUnits())) validate();
    }
    
    /** TODO
     * @return
     */
    protected Rectangle getAxisBounds() {
        Rectangle bounds;
        if (isHorizontal()) {
            bounds = getHorizontalAxisBounds();
        } else {
            bounds = getVerticalAxisBounds();
        }
        if (getOrientation() == BOTTOM && areTickLabelsVisible()) {
            if (drawTca && tcaData != null && tcaData.length != 0) {
                int DMin = getColumn().getDMinimum();
                int DMax = getColumn().getDMaximum();
                Font tickLabelFont = getTickLabelFont();
                int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
                int tcaHeight = (tickLabelFont.getSize() + getLineSpacing())*tcaData.length;
                int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
                bounds.height += tcaHeight;
                blLabelRect.height += tcaHeight;
                if ( blTitleRect != null ) blTitleRect.y += tcaHeight;
                GrannyTextRenderer idlt = new GrannyTextRenderer();
                idlt.setString(this, "SCET");
                int tcaLabelWidth = (int)Math.floor(idlt.getWidth() + 0.5);
                for (int i = 0; i < tcaData.length; i++) {
                    idlt.setString(this, (String)tcaData[i].getProperty("label"));
                    int width = (int)Math.floor(idlt.getWidth() + 0.5);
                    tcaLabelWidth = Math.max(tcaLabelWidth, width);
                }
                tcaLabelWidth += 50;
                if (tcaLabelWidth > 0) {
                    int tcaLabelSpace = DMin - tcaLabelWidth - tick_label_gap;
                    int minX = Math.min(tcaLabelSpace - maxLabelWidth/2, bounds.x);
                    int maxX = bounds.x + bounds.width;
                    bounds.x = minX;
                    bounds.width = maxX - minX;
                    blLabelRect.x = minX;
                    blLabelRect.width = maxX - minX;
                }
            }
            bounds.height += getTickLabelFont().getSize() + getLineSpacing();
            if (getTickDirection() == -1) {
                bounds.y -= getTickLabelFont().getSize() + getLineSpacing();
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
        
        boolean bottomTicks = (orientation == BOTTOM || oppositeAxisVisible);
        boolean bottomTickLabels = (orientation == BOTTOM && tickLabelsVisible);
        boolean bottomLabel = (bottomTickLabels && !axisLabel.equals(""));
        boolean topTicks = (orientation == TOP || oppositeAxisVisible);
        boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
        boolean topLabel = (topTickLabels && !axisLabel.equals(""));
        
        Rectangle bounds;
        
        Font tickLabelFont = getTickLabelFont();
        
        int tickSize = tickLabelFont.getSize() * 2 / 3;
        
        if (bottomTicks) {
            if (blLineRect == null) {
                blLineRect= new Rectangle();
            }
            blLineRect.setBounds(DMin, bottomPosition, DMax-DMin + 1, 1);
        }
        if (topTicks) {
            if (trLineRect== null) {
                trLineRect = new Rectangle();
            }
            trLineRect.setBounds(DMin, topPosition, DMax-DMin + 1, 1);
        }
        
        //Add room for ticks
        if (bottomTicks) {
            int x = DMin;
            int y = bottomPosition + 1;
            int width = DMax - DMin;
            int height = tickSize;
            //The last tick is at position (x + width), so add 1 to width
            blTickRect = setRectangleBounds(blTickRect, x, y, width + 1, height);
        }
        if (topTicks) {
            int x = DMin;
            int y = topPosition - tickSize;
            int width = DMax - DMin;
            int height = tickSize;
            //The last tick is at position (x + width), so add 1 to width
            trTickRect = setRectangleBounds(trTickRect, x, y, width + 1, height);
        }
        
        int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
        int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
        
        if (bottomTickLabels) {
            int x = DMin - maxLabelWidth/2;
            int y = blTickRect.y + blTickRect.height;
            int width = DMax - DMin + maxLabelWidth;
            int height = tickLabelFont.getSize()*3/2 + tick_label_gap;
            blLabelRect = setRectangleBounds(blLabelRect, x, y, width, height);
        }
        if (topTickLabels) {
            int x = DMin - maxLabelWidth/2;
            int y = topPosition - (tickLabelFont.getSize()*3/2 + tick_label_gap + 1);
            int width = DMax - DMin + maxLabelWidth;
            int height = tickLabelFont.getSize()*3/2 + tick_label_gap;
            trLabelRect = setRectangleBounds(trLabelRect, x, y, width, height);
        }
        
        //Add room for the axis label
        Font labelFont = getLabelFont();
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(this, getLabel());
        int labelSpacing = (int)gtr.getHeight() + labelFont.getSize()/2;
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
        int DMax= getRow().getDMaximum();
        int DMin= getRow().getDMinimum();
        
        Rectangle bounds;
        
        Font tickLabelFont = getTickLabelFont();
        
        int tickSize = tickLabelFont.getSize() * 2 / 3;
        
        if (leftTicks) {
            if (blLineRect == null) {
                blLineRect = new Rectangle();
            }
            blLineRect.setBounds(leftPosition, DMin, 1, DMax-DMin + 1);
        }
        if (rightTicks) {
            if (trLineRect == null) {
                trLineRect = new Rectangle();
            }
            trLineRect.setBounds(rightPosition, DMin, 1, DMax-DMin + 1);
        }
        
        //Add room for ticks
        if (leftTicks) {
            int x = leftPosition - tickSize;
            int y = DMin;
            int width = tickSize;
            int height = DMax - DMin;
            //The last tick is at position (y + height), so add 1 to height
            blTickRect = setRectangleBounds(blTickRect, x, y, width, height + 1);
        }
        if (rightTicks) {
            int x = rightPosition + 1;
            int y = DMin;
            int width = tickSize;
            int height = DMax - DMin;
            //The last tick is at position (y + height), so add 1 to height
            trTickRect = setRectangleBounds(trTickRect, x, y, width, height + 1);
        }
        
        int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
        int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
        
        //Add room for tick labels
        if (leftTickLabels) {
            int x = blTickRect.x - (maxLabelWidth + tick_label_gap);
            int y = DMin - tickLabelFont.getSize();
            int width = maxLabelWidth + tick_label_gap;
            int height = DMax - DMin + tickLabelFont.getSize()*2;
            blLabelRect = setRectangleBounds(blLabelRect, x, y, width, height);
        }
        if (rightTickLabels) {
            int x = trTickRect.x + trTickRect.width;
            int y = DMin - tickLabelFont.getSize();
            int width = maxLabelWidth + tick_label_gap;
            int height = DMax - DMin + tickLabelFont.getSize()*2;
            trLabelRect = setRectangleBounds(trLabelRect, x, y, width, height);
        }
        
        //Add room for the axis label
        Font labelFont = getLabelFont();
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(this, getLabel());
        int labelSpacing = (int)gtr.getHeight() + labelFont.getSize()/2;
        if (leftLabel) {
            int x = blLabelRect.x - labelSpacing;
            int y = DMin;
            int width = labelSpacing;
            int height = DMax-DMin;
            blTitleRect = setRectangleBounds(blTitleRect, x, y, width, height);
        }
        if (rightLabel) {
            int x = trLabelRect.x + trLabelRect.width;
            int y = DMin;
            int width = labelSpacing;
            int height = DMax - DMin;
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
    
    /** TODO
     * @return
     */
    public int getOrientation() {
        return orientation;
    }
    
    
    /** TODO
     * @return
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
        return transform( datum.doubleValue(getUnits()), getUnits() );
    }
    
    protected double transformFast( double data, Units units ) {
        if ( dataRange.isLog() ) {
            if ( data<=0. ) {
                data= dataRange.getMinimum() - 3; // TODO verify that dataRange.getMinimum() is log.
            } else {
                data= DasMath.log10(data);
            }
        }
        double result= at_m * data + at_b;
        return result;
    }
    
    /** Transforms a double in the given units in data coordinates to a horizontal or vertical
     * position on the parent canvas.
     * @param data a data value
     * @param units the units of the given data value.
     * @return Horizontal or vertical position on the canvas.
     */
    protected double transform( double data, Units units ) {
        DasDevicePosition range;
        // TODO: consider optimization here
        if (isHorizontal()) {
            range= getColumn();
            return transform( data, units, range.getDMinimum(), range.getDMaximum() );
        } else {
            range= getRow();
            return transform( data, units, range.getDMaximum(), range.getDMinimum() );
        }
    }
    
    protected double transform( double data, Units units, int dmin, int dmax ) {
        if ( units!=dataRange.getUnits() ) {
            data= units.convertDoubleTo(dataRange.getUnits(), data);
        }
        
        double device_range= (dmax - dmin);
        double result;
        
        if ( dataRange.isLog() ) {
            if ( data<=0. ) {
                data= -1e308;
            } else {
                data= DasMath.log10(data);
            }
        }
        
        double minimum= dataRange.getMinimum();
        double maximum= dataRange.getMaximum();
        double data_range = maximum-minimum;
        
        if (flipped) {
            result = dmax - (device_range*(data-minimum)/data_range );
        } else {
            result= (device_range*(data-minimum)/data_range ) + dmin;
        }
        
        if ( result > 10000 ) {
            result=10000;
        }
        if ( result < -10000 ) {
            result=-10000;
        }
        return result;
    }
    
    public Datum invTransform(double idata) {
        double data;
        DasDevicePosition range = (isHorizontal()
        ? (DasDevicePosition) getColumn()
        : (DasDevicePosition) getRow());
        
        double alpha= (idata-range.getDMinimum())/(double)getDLength();
        if ( !isHorizontal() ) alpha= 1.0 - alpha;
        if ( flipped ) alpha = 1.0 - alpha;
        DatumFormatter formatter;
        double minimum= dataRange.getMinimum();
        double maximum= dataRange.getMaximum();
        double data_range = maximum-minimum;
        data= data_range*alpha + minimum;
        
        double resolution=  data_range / getDLength();
        if ( dataRange.isLog() ) {
            data= DasMath.exp10(data);
            resolution= data * ( DasMath.exp10(resolution) - 1 );
        }
        
        Datum result= Datum.create( data, dataRange.getUnits(), resolution );
        
        return result;
    }
    
    /** TODO
     * @param tickv
     * @return
     */
    protected String tickFormatter(double tickv) {
        // TODO: label the axis with the Unit!
        return datumFormatter.grannyFormat(Datum.create(tickv, getUnits()),getUnits());
        
    }
    
    /** TODO
     * @param e
     */
    public void dataRangeSelected(DataRangeSelectionEvent e) {
        this.setDataRange(e.getMinimum(),e.getMaximum());
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
        return getTickV().findTick( xDatum, direction, minor );
    }
    
    /** TODO
     * @param min0
     * @param max0
     * @param min1
     * @param max1
     */
    private void animateChange( double min0, double max0, double min1, double max1 ) {
        
        if ( animated && EventQueue.isDispatchThread() ) {
            
            
            
            
            logger.fine( "animate axis" );
            
            boolean drawTca0= getDrawTca();
            setDrawTca(false);
            
            long t0= System.currentTimeMillis();
            long frames=0;
            
            DataRange dataRange0= dataRange;
            DataRange tempRange= DataRange.getAnimationDataRange( dataRange.getDatumRange(),dataRange.isLog() );
            
            this.dataRange= tempRange;
            
            double transitionTime= 300; // millis
            //double transitionTime= 1500; // millis
            double alpha= ( System.currentTimeMillis() - t0 ) / transitionTime;
            
            while ( alpha < 1.0 ) {
                alpha= ( System.currentTimeMillis() - t0 ) / transitionTime;
                
                final double[] aa= new double[] { 0.0, 0.3, 0.85, 1.0 };
                final double[] aa1= new double[] { 0.0, 0.05, 0.90, 1.0 };
                
                double f1= DasMath.findex( aa, alpha, 0 );
                double a1= DasMath.interpolate( aa1, f1 );
                double a0= 1-a1;
                
                tempRange.setRange( min0*a0+min1*a1, max0*a0+max1*a1 );
                
                //updateTickV();
                this.paintImmediately(0,0,this.getWidth(),this.getHeight());
                
                if ( dasPlot!=null ) dasPlot.paintImmediately( 0,0,dasPlot.getWidth(), dasPlot.getHeight() );
                frames++;
            }
            
            logger.fine( "animation frames/sec= "+( 1000.*frames/transitionTime ) ) ;
            setDrawTca(drawTca0);
            
            this.dataRange= dataRange0;
        }
    }
    
    /** TODO */
    protected void updateImmediately() {
        super.updateImmediately();
        logger.finer(""+getDatumRange()+" "+isLog());
        resetTransform();
        updateTickV();
    }
    
    /** TODO
     * @return
     */
    public boolean areTickLabelsVisible() {
        return tickLabelsVisible;
    }
    
    /** TODO
     * @param b
     */
    public void setTickLabelsVisible(boolean b) {
        if (tickLabelsVisible == b) return;
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
    
    /** Process an <code>&lt;axis&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasAxis processAxisElement(Element element, FormBase form) throws DasPropertyException, DasNameException, ParseException {
        String name = element.getAttribute("name");
        boolean log = element.getAttribute("log").equals("true");
        Datum dataMinimum;
        Datum dataMaximum;
        if ("TIME".equals(element.getAttribute("units"))) {
            String min = element.getAttribute("dataMinimum");
            String max = element.getAttribute("dataMaximum");
            dataMinimum = (min == null || min.equals("") ? TimeUtil.create("1979-02-26") : TimeUtil.create(min));
            dataMaximum = (max == null || max.equals("") ? TimeUtil.create("1979-02-27") : TimeUtil.create(max));
        } else {
            String min = element.getAttribute("dataMinimum");
            String max = element.getAttribute("dataMaximum");
            dataMinimum = (min == null || min.equals("") ? Datum.create(1.0) : Datum.create(Double.parseDouble(min)));
            dataMaximum = (max == null || max.equals("") ? Datum.create(10.0) : Datum.create(Double.parseDouble(max)));
        }
        int orientation = parseOrientationString(element.getAttribute("orientation"));
        DasAxis axis = new DasAxis(dataMinimum, dataMaximum, orientation, log);
        String rowString = element.getAttribute("row");
        if (!rowString.equals("")) {
            DasRow row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
            axis.setRow(row);
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("")) {
            DasColumn column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
            axis.setColumn(column);
        }
        
        axis.setLabel(element.getAttribute("label"));
        axis.setOppositeAxisVisible(!element.getAttribute("oppositeAxisVisible").equals("false"));
        axis.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));
        
        axis.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, axis);
        
        return axis;
    }
    
    /** TODO
     * @param i
     * @return
     */
    protected static String orientationToString(int i) {
        switch (i) {
            case TOP:
                return "top";
            case BOTTOM:
                return "bottom";
            case LEFT:
                return "left";
            case RIGHT:
                return "right";
            default: throw new IllegalStateException("invalid orienation: " + i);
        }
    }
    
    /** TODO
     * @param orientationString
     * @return
     */
    protected static int parseOrientationString(String orientationString) {
        if (orientationString.equals("horizontal")) {
            return HORIZONTAL;
        } else if (orientationString.equals("vertical")) {
            return VERTICAL;
        } else if (orientationString.equals("left")) {
            return LEFT;
        } else if (orientationString.equals("right")) {
            return RIGHT;
        } else if (orientationString.equals("top")) {
            return TOP;
        } else if (orientationString.equals("bottom")) {
            return BOTTOM;
        } else {
            throw new IllegalArgumentException("Invalid orientation: " + orientationString);
        }
    }
    
    /** TODO
     * @param document
     * @return
     */
    public Element getDOMElement(Document document) {
        Element element;
        if (this.isAttached()) {
            element = document.createElement("attachedaxis");
        } else {
            element = document.createElement("axis");
        }
        if (this.isAttached()) {
            element.setAttribute("ref", this.getMasterAxis().getDasName());
        } else {
            String minimumStr = getDataMinimum().toString();
            element.setAttribute("dataMinimum", minimumStr);
            String maximumStr = getDataMaximum().toString();
            element.setAttribute("dataMaximum", maximumStr);
        }
        
        element.setAttribute("name", getDasName());
        element.setAttribute("row", getRow().getDasName());
        element.setAttribute("column", getColumn().getDasName());
        
        element.setAttribute("label", getLabel());
        element.setAttribute("log", Boolean.toString(isLog()));
        element.setAttribute("tickLabelsVisible", Boolean.toString(areTickLabelsVisible()));
        element.setAttribute("oppositeAxisVisible", Boolean.toString(isOppositeAxisVisible()));
        element.setAttribute("animated", Boolean.toString(isAnimated()));
        element.setAttribute("orientation", orientationToString(getOrientation()));
        
        return element;
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
    
    /** Process a <code>&lt;attachedaxis&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasAxis processAttachedaxisElement(Element element, FormBase form) throws DasPropertyException, DasNameException {
        String name = element.getAttribute("name");
        DasAxis ref = (DasAxis)form.checkValue(element.getAttribute("ref"), DasAxis.class, "<attachedaxis>");
        int orientation = (element.getAttribute("orientation").equals("horizontal") ? HORIZONTAL : DasAxis.VERTICAL);
        
        DasAxis axis = ref.createAttachedAxis(orientation);
        
        String rowString = element.getAttribute("row");
        if (!rowString.equals("")) {
            DasRow row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
            axis.setRow(row);
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("")) {
            DasColumn column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
            axis.setColumn(column);
        }
        
        axis.setDataPath(element.getAttribute("dataPath"));
        axis.setDrawTca(element.getAttribute("showTca").equals("true"));
        axis.setLabel(element.getAttribute("label"));
        axis.setOppositeAxisVisible(!element.getAttribute("oppositeAxisVisible").equals("false"));
        axis.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));
        
        axis.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, axis);
        
        return axis;
    }
    
    public void setPlot(DasPlot p ) {
        dasPlot= p;
    }
    
    /** TODO
     * @param name
     * @return
     */
    public static DasAxis createNamedAxis(String name) {
        DasAxis axis = new DasAxis(Datum.create(1.0, Units.dimensionless), Datum.create(10.0, Units.dimensionless), DasAxis.HORIZONTAL);
        if (name == null) {
            name = "axis_" + Integer.toHexString(System.identityHashCode(axis));
        }
        try {
            axis.setDasName(name);
        } catch (DasNameException dne) {
            DasExceptionHandler.handle(dne);
        }
        return axis;
    }
    
    /** TODO */
    public void scanPrevious() {
        Datum delta= ( getDataMaximum().subtract(getDataMinimum())).multiply(1.0);
        Datum tmin= getDataMinimum().subtract(delta);
        Datum tmax= getDataMaximum().subtract(delta);
        setDataRange(tmin, tmax);
    }
    
    /** TODO */
    public void scanNext() {
        Datum delta= ( getDataMaximum().subtract(getDataMinimum())).multiply(1.0);
        Datum tmin= getDataMinimum().add(delta);
        Datum tmax= getDataMaximum().add(delta);
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
    public void addMouseWheelListener( MouseWheelListener l ) {
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
        if ( e.getSource()!=this && !e.equals(lastProcessedEvent)) {
            setDatumRange(e.getRange()); // setDatumRange fires the event
            lastProcessedEvent= e;
        }
    }
    
    /** Registers TimeRangeSelectionListener to receive events.
     * @param listener The listener to register.
     */
    public synchronized void addTimeRangeSelectionListener(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener listener) {
        if (timeRangeListenerList == null ) {
            timeRangeListenerList = new javax.swing.event.EventListenerList();
        }
        timeRangeListenerList.add(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener listener) {
        timeRangeListenerList.remove(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Notifies all registered listeners about the event.
     *
     * @param event The event to be fired
     */
    private void fireTimeRangeSelectionListenerTimeRangeSelected(TimeRangeSelectionEvent event) {
        if (timeRangeListenerList == null) return;
        Object[] listeners = timeRangeListenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class) {
                String logmsg= "fire event: "+this.getClass().getName()+"-->"+listeners[i+1].getClass().getName()+" "+event;
                DasLogger.getLogger( DasLogger.GUI_LOG ).fine( logmsg );
                ((edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener)listeners[i+1]).timeRangeSelected(event);
            }
        }
    }
    
    static DasAxis processTimeaxisElement(Element element, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        String name = element.getAttribute("name");
        Datum timeMinimum = TimeUtil.create(element.getAttribute("timeMinimum"));
        Datum timeMaximum = TimeUtil.create(element.getAttribute("timeMaximum"));
        int orientation = parseOrientationString(element.getAttribute("orientation"));
        
        DasAxis timeaxis = new DasAxis(timeMinimum, timeMaximum, orientation);
        
        String rowString = element.getAttribute("row");
        if (!rowString.equals("")) {
            DasRow row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
            timeaxis.setRow(row);
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("")) {
            DasColumn column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
            timeaxis.setColumn(column);
        }
        
        timeaxis.setDataPath(element.getAttribute("dataPath"));
        timeaxis.setDrawTca(element.getAttribute("showTca").equals("true"));
        timeaxis.setLabel(element.getAttribute("label"));
        timeaxis.setOppositeAxisVisible(!element.getAttribute("oppositeAxisVisible").equals("false"));
        timeaxis.setTickLabelsVisible(!element.getAttribute("tickLabelsVisible").equals("false"));
        
        timeaxis.setDasName(name);
        DasApplication app = form.getDasApplication();
        NameContext nc = app.getNameContext();
        nc.put(name, timeaxis);
        
        return timeaxis;
    }
    
    private static final java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\([eEfF]\\d+.\\d+\\)");
    
    private static String format(double d, String f) {
        Matcher m = pattern.matcher(f);
        if (!m.matches()) throw new IllegalArgumentException("\"" + f + "\" is not a valid format specifier");
        int length = Integer.parseInt(f.substring(2, f.indexOf('.')));
        int fracLength = Integer.parseInt(f.substring(f.indexOf('.')+1, f.indexOf(')')));
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
        
        while (result.length() < length)
            result = " " + result;
        
        return result;
    }
    
    public String toString() {
        String retValue;
        retValue = super.toString()+"("+getUnits()+")";
        return retValue;
    }
    
    protected class AxisLayoutManager implements LayoutManager {
        
        //NOOP
        /** TODO
         * @param name
         * @param comp
         */
        public void addLayoutComponent(String name, Component comp) {}
        
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
                int tcaHeight = (getTickLabelFont().getSize() + getLineSpacing())*tcaData.length;
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
                bottomBounds = new Rectangle(DMin, bottomPosition, DMax-DMin + 1, 1);
            }
            if (topTicks) {
                topBounds = new Rectangle(DMin, topPosition, DMax-DMin + 1, 1);
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
                bottomBounds.height += tickLabelFont.getSize()*3/2 + tick_label_gap;
            }
            if (topTickLabels) {
                topBounds.y -= (tickLabelFont.getSize()*3/2 + tick_label_gap);
                topBounds.height += tickLabelFont.getSize()*3/2 + tick_label_gap;
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
            int DMax= getRow().getDMaximum();
            int DMin= getRow().getDMinimum();
            Rectangle leftBounds = null;
            Rectangle rightBounds = null;
            Font tickLabelFont = getTickLabelFont();
            int tickSize = tickLabelFont.getSize() * 2 / 3;
            //Initialize bounds rectangle(s)
            if (leftTicks) {
                leftBounds = new Rectangle(leftPosition, DMin, 1, DMax - DMin + 1);
            }
            if (rightTicks) {
                rightBounds = new Rectangle(rightPosition, DMin, 1, DMax-DMin + 1);
            }
            //Add room for ticks
            if (leftTicks) {
                leftBounds.width += tickSize;
                leftBounds.x -= tickSize;
            }
            if (rightTicks) {
                rightBounds.width += tickSize;
            }
            int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
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
        public void removeLayoutComponent(Component comp) {}
        
    }
    
    private static class ScanButton extends JButton {
        private boolean hover;
        private boolean pressed;
        /** TODO
         * @param text
         */
        public ScanButton(String text) {
            setOpaque(true);
            setContentAreaFilled(false);
            setText(text);
            setFocusable(false);
            setBorder(new CompoundBorder(
                    new LineBorder(Color.BLACK),
                    new EmptyBorder(2,2,2,2)));
            this.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        setForeground(Color.LIGHT_GRAY);
                        pressed = true;
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
                    hover = true;
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
            if (hover || pressed) {
                Graphics2D g2 = (Graphics2D)g;
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
    
    public boolean isFlipped() {
        return flipped;
    }
    
    public void setFlipped(boolean b) {
        update();
        this.flipped = b;
    }
    
    private void resetTransform() {
        DasDevicePosition pos;
        if ( isHorizontal() ) {
            pos= getColumn();
        } else {
            pos= getRow();
        }
        double dmin= pos.getDMinimum();
        double dmax= pos.getDMaximum();
        if ( isFlipped() ) {
            double t= dmin;
            dmin= dmax;
            dmax= t;
        }
        double [] at= GraphUtil.getSlopeIntercept( dataRange.getMinimum(), dmin, dataRange.getMaximum(), dmax );
        at_m= at[0];
        at_b= at[1];
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
