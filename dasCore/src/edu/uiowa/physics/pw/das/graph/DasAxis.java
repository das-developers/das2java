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

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.DasNameException;
import edu.uiowa.physics.pw.das.DasPropertyException;
import edu.uiowa.physics.pw.das.NameContext;
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.border.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import javax.swing.*;
import java.util.*;
import java.util.regex.*;

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
    private int orientation;
    private int tickDirection=1;  // 1=down or left, -1=up or right
    protected String axisLabel = "";
    protected TickVDescriptor tickV;
    private boolean ticksVisible = true;
    private boolean tickLabelsVisible = true;
    private boolean oppositeAxisVisible;
    protected DatumFormatter datumFormatter = DefaultDatumFormatterFactory.getInstance().defaultFormatter();
    private MouseModule zoom=null;
    private PropertyChangeListener dataRangePropertyListener;
    protected JPanel primaryInputPanel;
    protected JPanel secondaryInputPanel;
    private ScanButton scanPrevious;
    private ScanButton scanNext;
    private boolean animated=false;
    
    /* Rectangles representing different areas of the axis */
    private Rectangle blLineRect;
    private Rectangle trLineRect;
    private Rectangle blTickRect;
    private Rectangle trTickRect;
    private Rectangle blLabelRect;
    private Rectangle trLabelRect;
    private Rectangle blTitleRect;
    private Rectangle trTitleRect;

    /* TIME LOCATION UNITS RELATED INSTANCE MEMBERS */
    private javax.swing.event.EventListenerList timeRangeListenerList =  null;
    private TimeRangeSelectionEvent lastProcessedEvent=null;

    /* TCA RELATED INSTANCE MEMBERS */
    private DataSetDescriptor dsd;
    private TCADataSet tcaData;
    private String dataset = "";
    private boolean drawTca;
    private DataRequestThread drt;


    /** TODO
     * @param data
     * @param units
     * @param row
     * @param column
     * @param orientation
     * @param isLog
     * @return
     */
    public static DasAxis create( double [] data, Units units, DasRow row, DasColumn column, int orientation, boolean isLog ) {
        DasAxis result= new DasAxis(Datum.create(0,units),Datum.create(0,units),row,column,orientation,isLog);
        result.setDataRange(data);
        return result;
    }
    
    /** TODO
     * @param min
     * @param max
     * @param row
     * @param column
     * @param orientation
     */
    public DasAxis( Datum min, Datum max, DasRow row, DasColumn column, int orientation ) {
        this(min, max, row, column, orientation, false);
    }
    
    /** TODO
     * @param min
     * @param max
     * @param row
     * @param column
     * @param orientation
     * @param log
     */
    public DasAxis( Datum min, Datum max, DasRow row, DasColumn column, int orientation, boolean log) {
        this(row, column, orientation);
        dataRange = new DataRange(this,min,max,log);
        dataRange.addPropertyChangeListener("log", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("maximum", dataRangePropertyListener);
    }
    
    /** TODO
     * @param range
     * @param row
     * @param column
     * @param orientation
     */
    protected DasAxis(DataRange range, DasRow row, DasColumn column, int orientation) {
        this(row, column, orientation);
        dataRange = range;
        dataRange.addPropertyChangeListener("log", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("maximum", dataRangePropertyListener);
    }
    
    private DasAxis(DasRow row, DasColumn column, int orientation) {
        super(row, column);
        setOpaque(false);
        setOrientationInternal(orientation);
        installMouseModules();
        JMenuItem backMenuItem= new JMenuItem("Back");
        backMenuItem.addActionListener(createActionListener());
        backMenuItem.setActionCommand("back");
        backMenuItem.setToolTipText("undo last operation");
        mouseAdapter.addMenuItem(backMenuItem);
        dataRangePropertyListener = createDataRangePropertyListener();
        setLayout(new AxisLayoutManager());
        maybeInitializeInputPanels();
        maybeInitializeScanButtons();
        add(primaryInputPanel);
        add(secondaryInputPanel);
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
        scanPrevious = new DasAxis.ScanButton(SCAN_PREVIOUS_LABEL);
        scanNext = new DasAxis.ScanButton(SCAN_NEXT_LABEL);
        ActionListener al = createScanActionListener();
        scanPrevious.addActionListener(al);
        scanNext.addActionListener(al);
        add(scanPrevious);
        add(scanNext);
    }
    
    private ActionListener createActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                if (command.equals("back")) {
                    setDataRangePrev();
                }
            }
        };
    }
    
    private ActionListener createScanActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                if (command.equals(SCAN_PREVIOUS_LABEL)) {
                    scanPrevious();
                }
                else if (command.equals(SCAN_NEXT_LABEL)) {
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
                }
                else if (propertyName.equals("minimum")) {
                    update();
                    firePropertyChange("dataMinimum", oldValue, newValue);
                }
                else if (propertyName.equals("maximum")) {
                    update();
                    firePropertyChange("dataMaximum", oldValue, newValue);
                }
                markDirty();
            }
        };
    }

    private void installMouseModules() {
        if (zoom instanceof HorizontalRangeSelectorMouseModule) {
            ((HorizontalRangeSelectorMouseModule)zoom).removeDataRangeSelectionListener(this);
            mouseAdapter.removeMouseModule(zoom);
        }
        else if (zoom instanceof VerticalRangeSelectorMouseModule) {
            ((VerticalRangeSelectorMouseModule)zoom).removeDataRangeSelectionListener(this);
            mouseAdapter.removeMouseModule(zoom);
        }
        if (isHorizontal()) {
            zoom= new HorizontalRangeSelectorMouseModule(this,this);
            ((HorizontalRangeSelectorMouseModule)zoom).addDataRangeSelectionListener(this);
            mouseAdapter.addMouseModule(zoom);
            mouseAdapter.setPrimaryModule(zoom);
        }
        else {
            zoom= new VerticalRangeSelectorMouseModule(this,this);
            ((VerticalRangeSelectorMouseModule)zoom).addDataRangeSelectionListener(this);
            mouseAdapter.addMouseModule(zoom);
            mouseAdapter.setPrimaryModule(zoom);
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
        }
        else if (orientation == BOTTOM) {
            setTickDirection(DOWN);
        }
        else if (orientation == LEFT) {
            setTickDirection(RIGHT);
        }
        else if (orientation == RIGHT) {
            setTickDirection(LEFT);
        }
        else {
            throw new IllegalArgumentException("Invalid value for orientation");
        }
    }
    
    /** TODO
     * @param minimum
     * @param maximum
     */
    public void setDataRange(Datum minimum, Datum maximum) {
        
        if ( ! maximum.gt(minimum) ) {
            DasDie.println(DasDie.CRITICAL, "setDataRange where min > max ignored");
            return;
        }
        
        Units units= dataRange.getUnits();
        if (minimum.getUnits()!=units) {
            minimum.convertTo(units);
            maximum.convertTo(units);
        }
        
        double minimum0= dataRange.getMinimum();
        double maximum0= dataRange.getMaximum();
        animateChange( minimum0, maximum0, minimum.doubleValue(getUnits()), maximum.doubleValue(getUnits()) );
        
        dataRange.setRange( minimum.doubleValue(getUnits()), maximum.doubleValue(getUnits()) );
        update();
        if (getUnits() instanceof TimeLocationUnits) {
            TimeRangeSelectionEvent e= new TimeRangeSelectionEvent(this, this.getDataMinimum(), this.getDataMaximum());
            fireTimeRangeSelectionListenerTimeRangeSelected(e);
        }
    }
    
    /** TODO
     * @param data
     */
    public void setDataRange(double[] data) {
        double min=data[0];
        double max=data[0];
        for (int i=1; i<data.length; i++) {
            min= min<data[i]?min:data[i];
            max= max>data[i]?max:data[i];
        }
        double range=max-min;
        if (isLog()) {
            setDataRange(Datum.create(min,getUnits()), Datum.create(max,getUnits()));
        } else {
            Datum minDatum= Datum.create(min-range*0.05,getUnits());
            Datum maxDatum= Datum.create(max+range*0.05,getUnits());
            setDataRange(minDatum, maxDatum);
        }
    }
    
    /** TODO */
    public void setDataRangePrev() {
        double min0= dataRange.getMinimum();
        double max0= dataRange.getMaximum();
        dataRange.setRangePrev();
        double min1= dataRange.getMinimum();
        double max1= dataRange.getMaximum();
        animateChange(min0,max0,min1,max1);
        update();
    }
    
    /** TODO */
    public void setDataRangeForward() {
        double min0= dataRange.getMinimum();
        double max0= dataRange.getMaximum();
        dataRange.setRangeForward();
        double min1= dataRange.getMinimum();
        double max1= dataRange.getMaximum();
        animateChange(min0,max0,min1,max1);
        update();
    }
    
    /** TODO */
    public void setDataRangeZoomOut() {
        double t1= dataRange.getMinimum();
        double t2= dataRange.getMaximum();
        if (isLog()) {
            t1= Math.log(t1);
            t2= Math.log(t2);
        }
        double delta= t2-t1;
        double min= t1-delta/2;
        double max= t2+delta/2;
        if (isLog()) {
            min= Math.exp(min);
            max= Math.exp(max);
        }
        animateChange(t1,t2,min,max);
        dataRange.setRange(min,max);
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
        Datum result= Datum.create( dataRange.getMinimum(), dataRange.getUnits() );
        // We're going to want to add a decimal place or two here
        return result;
    }
    
    /** TODO
     * @return
     */
    public Datum getDataMaximum() {
        Datum result= Datum.create( dataRange.getMaximum(), dataRange.getUnits() );
        // We're going to want to add a decimal place or two here
        return result;
    }
    
    /** TODO
     * @param units
     * @return
     */
    public double getDataMaximum(Units units) {
        double result;
        if (units!=dataRange.getUnits()) {
            result= dataRange.getUnits().getConverter(units).convert(dataRange.getMaximum());
        } else {
            result= dataRange.getMaximum();
        }
        return result;
    }
    
    /** TODO
     * @param units
     * @return
     */
    public double getDataMinimum(Units units) {
        double result;
        if (units!=dataRange.getUnits()) {
            result= dataRange.getUnits().getConverter(units).convert(dataRange.getMinimum());
        } else {
            result= dataRange.getMinimum();
        }
        return result;
    }

    /** TODO
     * @param max
     */
    public void setDataMaximum(Datum max) {
        dataRange.setMaximum(max.doubleValue(getUnits()));
        update();
    }

    /** TODO
     * @param min
     */
    public void setDataMinimum(Datum min) {
        dataRange.setMinimum(min.doubleValue(getUnits()));
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
    public Units getUnits() {    /** TODO
     * @param min
     */

        return dataRange.getUnits();
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

    public void setDataPath(String dataset) {
        if (dataset == null) throw new NullPointerException("null dataPath string not allowed");
        Object oldValue = this.dataset;
        if (dataset.equals(this.dataset)) return;
        this.dataset=dataset;
        try {
            dsd = DataSetDescriptorUtil.create(dataset);
        }
        catch (edu.uiowa.physics.pw.das.DasException de) {
            DasExceptionHandler.handle(de);
        }
        update();
        firePropertyChange("dataPath", oldValue, dataset);
    }

    private void updateDataSet() {
        if (!drawTca || dataset.equals("") || dsd==null) return;
        double [] tickV = getTickV().tickV;
        Datum data_minimum;
        Datum data_maximum;
        Datum iinterval;
        if (tickV.length == 1) {
            data_minimum = Datum.create(tickV[0],getTickV().units);
            data_maximum = Datum.create(tickV[0], getTickV().units);
            iinterval = data_maximum.subtract(data_minimum);
        }
        else {
            data_minimum = Datum.create(tickV[0],getTickV().units);
            data_maximum = Datum.create(tickV[tickV.length-1],getTickV().units);
            iinterval = (data_maximum.subtract(data_minimum)).divide(tickV.length-1);
        }
        data_maximum= data_maximum.add(iinterval);
        final Datum interval = iinterval;
        tcaData = null;
            DataRequestor requestor = new DataRequestor() {

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
                    tcaData = (TCADataSet)dsFinished;
                    update();
                    edu.uiowa.physics.pw.das.util.DasDie.println("I GOT HERE WITH: " + dsFinished);
                }
            };
            if (drt == null) {
                drt = new DataRequestThread();
            }
            try {
                drt.request(dsd, data_minimum,
                                 data_maximum.add(Datum.create(1.0,Units.seconds)),
                                 interval, requestor, null );
            }
            catch (InterruptedException ie) {
                DasExceptionHandler.handle(ie);
            }

    }

    /** TODO
     * @return
     */
    public final int getDevicePosition() {
        if (orientation == BOTTOM) {
            return getRow().getDMaximum();
        }
        else if (orientation == TOP) {
            return getRow().getDMinimum();
        }
        else if (orientation == LEFT) {
            return getColumn().getDMinimum();
        }
        else {
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
        dataRange.addPropertyChangeListener("log", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("maximum", dataRangePropertyListener);
        if (oldRange.isLog() != dataRange.isLog()) {
            firePropertyChange("log", oldRange.isLog(), dataRange.isLog());
        }
        firePropertyChange("minimum", oldRange.getMinimum(), dataRange.getMinimum());
        firePropertyChange("maximum", oldRange.getMaximum(), dataRange.getMaximum());
    }
    
    /** TODO */
    public void detach() {
        dataRange.removePropertyChangeListener("log", dataRangePropertyListener);
        dataRange.removePropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.removePropertyChangeListener("maximum", dataRangePropertyListener);
        DataRange newRange
        = new DataRange(this,Datum.create(dataRange.getMinimum(), dataRange.getUnits()),
        Datum.create(dataRange.getMaximum(), dataRange.getUnits()),
        dataRange.isLog());
        dataRange = newRange;
        dataRange.addPropertyChangeListener("log", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("minimum", dataRangePropertyListener);
        dataRange.addPropertyChangeListener("maximum", dataRangePropertyListener);
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
    protected TickVDescriptor getTickV() {
        if (tickV == null) updateTickV();
        return tickV;
    }
    
    private void updateTickVLog() {
        
        TickVDescriptor ticks= new TickVDescriptor();
        ticks.units= dataRange.getUnits();
        double min= dataRange.getMinimum();
        double max= dataRange.getMaximum();
        
        double [] result;
        double dMinTick= DasMath.roundNDigits(DasMath.log10(min),4);
        int minTick= (int)Math.ceil(dMinTick);
        double dMaxTick= DasMath.roundNDigits(DasMath.log10(max),4);
        int maxTick= (int)Math.floor(dMaxTick);
        
        int stepSize= 1;
        
        int nTicks= ( maxTick - minTick ) / stepSize + 1;
        
        int pixelsPerTick;
        if (isHorizontal()) {
            pixelsPerTick = getColumn().getWidth() / nTicks;
        }
        else {
            pixelsPerTick = getRow().getHeight() / nTicks;
        }
        
        //if (nTicks>5) {
        if ( pixelsPerTick < 30 && pixelsPerTick>0 ) {
            stepSize= (int) Math.floor( ( maxTick - minTick ) / 5. );
            minTick= (int) Math.ceil( minTick / (float)stepSize ) * stepSize;
            maxTick= (int) Math.floor( maxTick / (float)stepSize ) * stepSize;
            nTicks= ( maxTick - minTick ) / stepSize + 1;
        }
        
        result= new double[nTicks];
        for (int i=0; i<nTicks; i++) {
            result[i]= DasMath.exp10(i*stepSize+minTick);
        }
        
        ticks.tickV= result;
        
        if ( DasMath.log10( max / min ) >= 5 || DasMath.log10( max ) >= 5 || DasMath.log10( min ) <= -5 ) {
            DatumFormatterFactory factory = getUnits().getDatumFormatterFactory();
            if (factory instanceof DefaultDatumFormatterFactory) {
                try {
                    datumFormatter = factory.newFormatter("0E0");
                }
                catch (java.text.ParseException pe) {
                    //If this happened then something is seriously wrong,
                    //so respond accordingly.
                    throw new RuntimeException(pe);
                }
            }
            else {
                datumFormatter = factory.defaultFormatter();
            }
        } else {
            datumFormatter = DatumUtil.bestFormatter(getDataMaximum(), getDataMaximum(), nTicks);
        }
        
        int firstMinorTick= (int)Math.floor(DasMath.log10(min));
        int lastMinorTick= (int)Math.floor(DasMath.log10(max));
        
        int idx=0;
        double [] minorTickV= new double[(lastMinorTick-firstMinorTick+1)*9];
        for ( int i=firstMinorTick; i<=lastMinorTick; i++ ) {
            for ( int j=0; j<9; j++ ) {
                try {
                    minorTickV[idx++]= DasMath.exp10(i) + DasMath.exp10(i) * j;
                } catch ( ArrayIndexOutOfBoundsException e ) {
                    e.printStackTrace(); // does this code need to be here?
                }
            }
        }
        ticks.minorTickV= minorTickV;
        
        if ( minTick>=maxTick ) {
            double[] majorTicks= new double[ ticks.tickV.length + ticks.minorTickV.length ];
            for ( int i=0; i<ticks.tickV.length; i++ ) majorTicks[i]= ticks.tickV[i];
            for ( int i=0; i<ticks.minorTickV.length; i++ ) majorTicks[i+ticks.tickV.length]= ticks.minorTickV[i];
        }
        
        tickV = ticks;
        return;
        
    }
    
    private void updateTickVLinear() {
        
        int nTicksMax;
        int axisSize;
        if (isHorizontal()) {
            int tickSizePixels= getFontMetrics(getTickLabelFont()).stringWidth("0.0000");
            axisSize= getColumn().getWidth();
            nTicksMax= axisSize / tickSizePixels;
        } else {
            int tickSizePixels= getFontMetrics(getTickLabelFont()).getHeight();
            axisSize= getRow().getHeight();
            nTicksMax= axisSize / tickSizePixels;
        }
        
        nTicksMax= (nTicksMax<7)?nTicksMax:7;
        
        TickVDescriptor res= new TickVDescriptor();
        
        res.units= dataRange.getUnits();
        double minimum= dataRange.getMinimum();
        double maximum= dataRange.getMaximum();
        
        double maj= (maximum-minimum)/nTicksMax;
        double mag= Math.exp(Math.log(10)*Math.floor(Math.log(maj)/Math.log(10)));
        double absissa= maj/mag;
        
        if (absissa<1.666) absissa=1.0;
        else if (absissa<3.333) absissa=2.0;
        else absissa=5.0;
        
        double tickSize= absissa * mag;
        double firstTick= tickSize*Math.ceil(minimum/tickSize - 0.01);
        double lastTick= tickSize*Math.floor(maximum/tickSize + 0.01);
        
        int nTicks= 1+(int)Math.round((lastTick-firstTick)/tickSize);
        
        double [] result= new double[nTicks];
        for (int i=0; i<nTicks; i++) result[i]=firstTick+i*tickSize;
        
        res.tickV= result;
        
        double minor;
        if (absissa==5.) {
            minor= tickSize/5;
        } else if ( absissa==2. ) {
            minor= tickSize/2;
        } else {
            minor= tickSize/4;
        }
        
        datumFormatter = DatumUtil.bestFormatter(getDataMinimum(), getDataMaximum(), nTicks);

        double axisLengthData= maximum-minimum;
        double firstMinor= minor * Math.ceil( ( minimum - axisLengthData*0.3 ) / minor );
        double lastMinor= minor * Math.floor( ( maximum + axisLengthData*0.3 ) / minor );
        int nMinor= ( int ) ( ( lastMinor - firstMinor ) / minor + 0.5 );
        double [] minorTickV= new double[ nMinor ];
        for ( int i=0; i<nMinor; i++ ) minorTickV[i]= firstMinor + i*minor;
        
        res.minorTickV= minorTickV;
        
        tickV = res;
        return;
        
    }
    
    private void updateTickVTime() {
        TickVDescriptor res= new TickVDescriptor();
        
        double data_minimum = getDataMinimum().doubleValue(Units.t2000);
        double data_maximum = getDataMaximum().doubleValue(Units.t2000);
        
        double [] tickV;
        
        int nTicksMax;
        if (isHorizontal()) {
            GrannyTextRenderer idlt= new GrannyTextRenderer();
            idlt.setString(this, "0000-00-00");
            int tickSizePixels= (int) idlt.getWidth();
            int axisSize= getColumn().getWidth();
            nTicksMax= axisSize / tickSizePixels;
        } else {
            int tickSizePixels= getFontMetrics(getTickLabelFont()).getHeight();
            int axisSize= getRow().getHeight();
            nTicksMax= axisSize / tickSizePixels;
        }
        nTicksMax= ( nTicksMax>1 ? nTicksMax : 2 ) ;
        nTicksMax= ( nTicksMax<10 ? nTicksMax : 10 ) ;
        
        double[] mags12= {
            0.001, 0.002, 0.005,
            0.01, 0.02, 0.05,
            0.1, 0.2, 0.5,
            1, 2, 5, 10, 30,
            60, 120, 300, 600, 1200,
            3600, 7200, 10800, 14400, 21600, 28800, 43200, //1hr, 2hr, 3hr, 4hr, 6hr, 8hr, 12hr
            86400, 172800, 86400*5, 86400*10
        };
        
        int[] nminor= {
            4, 4, 5,
            4, 4, 5,
            4, 4, 5,
            4, 4, 5, 5, 3,
            6, 4, 5, 5, 4,
            4, 4, 3, 4, 3, 4, 6,
            4, 2, 5, 10
        };
        
        double mag_keep=-1;
        double absissa;
        double mag;
        
        double tickSize, firstTick, lastTick;
        int nTicks;
        double minor;
        
        int i=0;
        int ikeep=-1;
        
        if ((data_maximum-data_minimum)>86400) i=4; // get it past the very small ticks to avoid rollover error
        while (i<mags12.length && ikeep==-1) {
            
            mag= mags12[i];
            
            tickSize= mag;
            
            firstTick= tickSize*Math.ceil((data_minimum)/tickSize);
            lastTick= tickSize*Math.floor((data_maximum)/tickSize);
            
            if ( (lastTick-firstTick)/tickSize > 1000 ) {
                nTicks= 1000; // avoid intger roll-over
            } else {
                nTicks= 1+(int)((lastTick-firstTick)/tickSize);
            }
            
            if (nTicks<nTicksMax) {
                ikeep= i;
            }
            i++;
        }
        
        if (ikeep!=-1) {
            mag_keep= mags12[ikeep];            
            absissa= 1.0;
            
            tickSize= absissa * mag_keep;
            
            firstTick= tickSize*Math.ceil((data_minimum)/tickSize);
            lastTick= tickSize*Math.floor((data_maximum)/tickSize);
            
            nTicks= 1+(int)Math.round((lastTick-firstTick)/tickSize);
            if (nTicks<2) {
                edu.uiowa.physics.pw.das.util.DasDie.println("Only able to find one major tick--sorry! ");
                edu.uiowa.physics.pw.das.util.DasDie.println("please let us know how you entered this condition");
                nTicks=2;
            }
            
            tickV= new double[nTicks];
            for (i=0; i<nTicks; i++)
                tickV[i]=firstTick+i*tickSize;
            
            res.tickV= tickV;
            
            double axisLengthData= ( data_maximum - data_minimum );
            minor= tickSize / nminor[ikeep];            
            double firstMinor= minor * Math.ceil( ( data_minimum - axisLengthData*0.3 ) / minor );
            double lastMinor= minor * Math.floor( ( data_maximum + axisLengthData*0.3 ) / minor );
            int nMinor= ( int ) ( ( lastMinor - firstMinor ) / minor + 0.5 );
            double [] minorTickV= new double[ nMinor ];
            for ( int ii=0; ii<nMinor; ii++ ) minorTickV[ii]= firstMinor + ii*minor;
        
            res.minorTickV= minorTickV;
        
            datumFormatter = DatumUtil.bestFormatter(getDataMinimum(), getDataMaximum(), nTicks );
            
        } else  { // pick off month boundaries
            double [] result= new double[30];
            ArrayList minorTickV= new ArrayList();
            int ir=0;
            Datum current;
            Datum min= Datum.create( data_minimum,Units.t2000 );
            Datum max= Datum.create( data_maximum,Units.t2000 );
            int step;  
            int nstep=1;
            int minorStep;  // step size for minor ticks
            int minorNStep=1;    // multiplier for step size
            if ((data_maximum-data_minimum)<86400*30*6) {  // months
                step= TimeUtil.MONTH;
                minorStep= TimeUtil.DAY;
                minorNStep=1;
            } else if ((data_maximum-data_minimum)<86400*30*25) { // seasons
                step= TimeUtil.QUARTER;
                minorStep= TimeUtil.MONTH;                
            } else if ((data_maximum-data_minimum)<86400*365*6) { // years
                step= TimeUtil.YEAR;
                minorStep= TimeUtil.MONTH;                
            } else {
                // TODO fall back to decimal years
                step= TimeUtil.YEAR;
                minorStep= TimeUtil.YEAR;
                nstep= 2;
            }
            current= TimeUtil.next(step,Datum.create(data_minimum,Units.t2000));
            while(max.gt(current)) {
                result[ir++]= current.doubleValue(Units.t2000);
                current= TimeUtil.next(step,current);
                for (int ii=nstep; ii>1; ii--) current= TimeUtil.next(step,current);
            }
            
            res.tickV= new double[ir];
            for (ir=0; ir<res.tickV.length; ir++) res.tickV[ir]= result[ir];                        
            
            current = TimeUtil.next(minorStep,Datum.create(data_minimum,Units.t2000));
            while(max.gt(current)) {
                minorTickV.add( current );
                current= TimeUtil.next(minorStep,current);
                for (int ii=minorNStep; ii>1; ii--) current= TimeUtil.next(minorStep,current);
            }
            
            res.minorTickV= new double[minorTickV.size()];
            for ( int ii=0; ii<minorTickV.size(); ii++ ) 
                res.minorTickV[ii]= ((Datum)minorTickV.get(ii)).doubleValue(Units.t2000);            
            
            datumFormatter = DatumUtil.bestFormatter( getDataMinimum(), getDataMaximum(), 6 );
        }
        
        UnitsConverter uc= Units.getConverter(Units.t2000,getUnits());
        for (int ii=0; ii<res.tickV.length; ii++) {
            res.tickV[ii]= uc.convert(res.tickV[ii]);
        }
        for (int ii=0; ii<res.minorTickV.length; ii++) {
            res.minorTickV[ii]= uc.convert(res.minorTickV[ii]);
        }        
        res.units= getUnits();
                
        this.tickV = res;
        updateDataSet();
    }
    
    protected void updateTickV() {
        if (getUnits() instanceof TimeLocationUnits) {
            updateTickVTime();
        } else if (dataRange.isLog()) {
            updateTickVLog();
        } else {
            updateTickVLinear();
        }
    }
    
    private double pixelSizeData() {
        Units units= getUnits();
        return ( getDataMaximum().doubleValue(units) - getDataMinimum().doubleValue(units) ) / getDLength();
    }
    
    /** TODO */
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.translate(-getX(), -getY());
        if (isHorizontal()) {
            paintHorizontalAxis(g);
        }
        else {
            paintVerticalAxis(g);
        }
        
        Rectangle clip = g.getClipBounds();
        
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
            
            for (int i = 0; i < tcaData.items; i++) {
                baseLine += lineHeight;
                idlt.setString(this, tcaData.label[i]);
                width = (int)Math.floor(idlt.getWidth() + 0.5);
                leftEdge = rightEdge - width;
                idlt.draw(g, (float)leftEdge, (float)baseLine);
            }
        }
    }
    
    /** Paint the axis if it is horizontal  */
    protected void paintHorizontalAxis(Graphics2D g) {
        Rectangle clip = g.getClipBounds();
        
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
        double[] tickv= ticks.tickV;
        
        if (bottomLine) {
            g.drawLine(DMin,bottomPosition,DMax,bottomPosition);
        }
        if (topLine) {
            g.drawLine(DMin,topPosition,DMax,topPosition);
        }
        
        int tickLengthMajor = labelFont.getSize() * 2 / 3;
        int tickLengthMinor = tickLengthMajor / 2;
        int tickLength;

        for ( int i=0; i<ticks.tickV.length; i++ ) {
            double tick1= ticks.tickV[i];
            if ( tick1>=(dataMin-pixelSizeData()/2) && tick1<=(dataMax+pixelSizeData()/2) ) {
                int tickPosition= (int)Math.floor(transform(tick1,ticks.units) + 0.5);
                tickLength= tickLengthMajor;
                if (bottomTicks) {
                    g.drawLine( tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                    if (bottomTickLabels) {
                        drawLabel(g, tick1, i, tickPosition, bottomPosition + tickLength);
                    }
                }
                if (topTicks) {
                    g.drawLine( tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    if (topTickLabels) {
                        drawLabel(g, tick1, i, tickPosition, topPosition - tickLength);
                    }
                }
            }
        }
        
        for ( int i=0; i<ticks.minorTickV.length; i++ ) {
            double tick1= ticks.minorTickV[i];
            if ( tick1>=dataMin && tick1<=dataMax ) {
                tickLength= tickLengthMinor;
                int tickPosition= (int)Math.floor(transform(tick1,ticks.units) + 0.5);
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
    }
    
    /** Paint the axis if it is vertical  */
    protected void paintVerticalAxis(Graphics2D g) {
        Rectangle clip = g.getClipBounds();
        
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
        double[] tickv= ticks.tickV;
        
        if (leftLine) {
            g.drawLine(leftPosition,DMin,leftPosition,DMax);
        }
        if (rightLine) {
            g.drawLine(rightPosition,DMin,rightPosition,DMax);
        }
        
        int tickLengthMajor= labelFont.getSize()*2/3;
        int tickLengthMinor = tickLengthMajor / 2;
        int tickLength;
        
        for ( int i=0; i<ticks.tickV.length; i++ ) {
            double tick1= ticks.tickV[i];
            if ( tick1>=(dataMin*0.999) && tick1<=(dataMax*1.001) ) {
                int tickPosition= (int)Math.floor(transform(tick1,ticks.units) + 0.5);
                tickLength= tickLengthMajor;
                if (leftTicks) {
                    g.drawLine( leftPosition, tickPosition, leftPosition - tickLength, tickPosition );
                    if (leftTickLabels) {
                        drawLabel(g, tick1, i, leftPosition - tickLength, tickPosition);
                    }
                }
                if (rightTicks) {
                    g.drawLine( rightPosition, tickPosition, rightPosition + tickLength, tickPosition );
                    if (rightTickLabels) {
                        drawLabel(g, tick1, i, rightPosition + tickLength, tickPosition);
                    }
                }
            }
        }
        
        for ( int i=0; i<ticks.minorTickV.length; i++ ) {
            tickLength= tickLengthMinor;
            double tick1= ticks.minorTickV[i];
            if ( tick1>=dataMin && tick1<=dataMax ) {
                int tickPosition= (int)Math.floor(transform(tick1,ticks.units) + 0.5);
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
            gtr.setString(this, axisLabel);
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
        }
        else if (orientation == TOP) {
            offset = tickLength + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize()/2 + (int)gtr.getDescent();
        }
        else if (orientation == LEFT) {
            offset = tickLength + getMaxLabelWidth(fm) + fm.stringWidth(" ") + labelFont.getSize()/2 + (int)gtr.getDescent();
        }
        else {
            offset = tickLength + getMaxLabelWidth(fm) + fm.stringWidth(" ") + labelFont.getSize()/2 + (int)gtr.getDescent();
        }
        if (getOrientation() == BOTTOM) {
            if (drawTca && tcaData != null) {
                offset += tcaData.items * (tickLabelFont.getSize() + getLineSpacing());
            }
            offset += tickLabelFont.getSize() + getLineSpacing();
        }
        return offset;
    }
    
    public int getLineSpacing() {
        return getTickLabelFont().getSize()/4;
    }

    /** TODO */
    protected void drawLabel(Graphics g, double value, int index, int x, int y) {
        
        if (!tickLabelsVisible) return;
        
        String label = tickFormatter(value);
        
        g.setFont(getTickLabelFont());
        GrannyTextRenderer idlt= new GrannyTextRenderer();
        idlt.setString(this,label);
        
        int width = (int) idlt.getWidth();
        int height = (int) idlt.getHeight();
        int ascent = (int) idlt.getAscent();
        
        int tick_label_gap = getFontMetrics(getTickLabelFont()).stringWidth(" ");
        
        if (orientation == BOTTOM) {
            x -= width/2;
            y += getTickLabelFont().getSize() + tick_label_gap;
        }
        else if (orientation == TOP) {
            x -= width/2;
            y -= tick_label_gap + idlt.getDescent();
        }
        else if (orientation == LEFT) {
            x -= (width + tick_label_gap);
            y += ascent - height/2;
        }
        else {
            x += tick_label_gap;
            y += ascent - height/2;
        }
        Color c = g.getColor();
        idlt.draw(g,x,y);
        if (orientation == BOTTOM && drawTca && tcaData != null) {
            drawTCAItems(g, index, x, y, width);
        }
    }
    
    private void drawTCAItems(Graphics g, int index, int x, int y, int width) {        
        int height = getTickLabelFont().getSize();
        int tick_label_gap = getFontMetrics(getTickLabelFont()).stringWidth(" ");
        int baseLine = y;
        int leftEdge = x;
        int rightEdge = leftEdge + width;
        Font tickLabelFont = getTickLabelFont();
        FontMetrics fm = getFontMetrics(tickLabelFont);
        int lineHeight = tickLabelFont.getSize() + getLineSpacing();
        for (int i = 0; i < tcaData.items; i++) {
            baseLine += lineHeight;
            String item = format(tcaData.data[index].y[i], "(f8.2)");
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
    }
    
    /** TODO
     * @return
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error("Assertion failure");
        }
    }
    
    private void setTickDirection(int direction) {
        if (direction ==  UP || direction == RIGHT) {
            tickDirection=-1;
        }
        else if (direction == DOWN || direction == LEFT) {
            tickDirection=1;
        }
        else {
            throw new IllegalArgumentException("Invalid tick direction");
        }
    }
    
    /** TODO
     * @param fm
     * @return
     */
    protected int getMaxLabelWidth(FontMetrics fm) {
        TickVDescriptor ticks = getTickV();
        double[] tickv = ticks.tickV;
        int size = Integer.MIN_VALUE;
        for (int i = 0; i < tickv.length; i++) {
            String label = tickFormatter(tickv[i]);
            int labelSize = fm.stringWidth(label);
            if (labelSize > size) size = labelSize;
        }
        return size;
    }
    
    /** TODO */
    public void resize() {
        setBounds(getAxisBounds());
        invalidate();
        validate();
    }
    
    /** TODO
     * @return
     */
    protected Rectangle getAxisBounds() {
        Rectangle bounds;
        if (isHorizontal()) {
            bounds = getHorizontalAxisBounds();
        }
        else {
            bounds = getVerticalAxisBounds();
        }
        if (getOrientation() == BOTTOM && areTickLabelsVisible()) {
            if (drawTca && tcaData != null) {
                int DMin = getColumn().getDMinimum();
                int DMax = getColumn().getDMaximum();
                Font tickLabelFont = getTickLabelFont();
                int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
                int tcaHeight = (tickLabelFont.getSize() + getLineSpacing())*tcaData.items;
                int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
                bounds.height += tcaHeight;
                blLabelRect.height += tcaHeight;
                blTickRect.y += tcaHeight;
                GrannyTextRenderer idlt = new GrannyTextRenderer();
                idlt.setString(this, "SCET");
                int tcaLabelWidth = (int)Math.floor(idlt.getWidth() + 0.5);
                for (int i = 0; i < tcaData.items; i++) {
                    idlt.setString(this, tcaData.label[i]);
                    int width = (int)Math.floor(idlt.getWidth() + 0.5);
                    tcaLabelWidth = Math.max(tcaLabelWidth, width);
                }
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
        boolean bottomLabel = (orientation == BOTTOM && !axisLabel.equals(""));
        boolean topTicks = (orientation == TOP || oppositeAxisVisible);
        boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
        boolean topLabel = (orientation == TOP && !axisLabel.equals(""));
        
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
            blTickRect = setRectangleBounds(blTickRect, x, y, width, height);
        }
        if (topTicks) {
            int x = DMin;
            int y = topPosition - tickSize;
            int width = DMax - DMin;
            int height = tickSize;
            trTickRect = setRectangleBounds(trTickRect, x, y, width, height);
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
            blTickRect = setRectangleBounds(blTickRect, x, y, width, height);
        }
        if (rightTicks) {
            int x = rightPosition + 1;
            int y = DMin;
            int width = tickSize;
            int height = DMax - DMin;
            trTickRect = setRectangleBounds(trTickRect, x, y, width, height);
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
        }
        else {
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

    /** TODO
     * @param datum
     * @return
     */
    public int transform(Datum datum) {
        return transform( datum.doubleValue(getUnits()), getUnits() );
    }
    
    int transform( double data, Units units ) {
        DasDevicePosition range;
        if (isHorizontal()) {
            range= getColumn();
            return transform( data, units, range.getDMinimum(), range.getDMaximum() );
        } else {
            range= getRow();
            return transform( data, units, range.getDMaximum(), range.getDMinimum() );
        }
    }
    
    int transform( double data, Units units, int dmin, int dmax ) {
        if ( units!=dataRange.getUnits() ) {
            data= Units.getConverter(dataRange.getUnits(),units).convert(data);
        }
        
        int device_range= (dmax - dmin);
        int result;
        
        if (dataRange.isLog()) {
            
            double data_log = Math.log(data);
            double data_log_min = Math.log(dataRange.getMinimum());
            double data_log_max = Math.log(dataRange.getMaximum());
            double data_log_range = data_log_max - data_log_min;
            
            result= (int)(device_range*(data_log-data_log_min)/data_log_range) + dmin;
        }
        else {
            
            double minimum= dataRange.getMinimum();
            double maximum= dataRange.getMaximum();
            double data_range = maximum-minimum;
            result= (int)(device_range*(data-minimum)/data_range ) + dmin;
        }
        
        if ( result > 10000 ) result=10000;
        if ( result < -10000 ) result=-10000;
        return result;
    }
    
    public Datum invTransform(int idata) {
        double data;
        DasDevicePosition range = (isHorizontal()
        ? (DasDevicePosition) getColumn()
        : (DasDevicePosition) getRow());

        double alpha= (idata-range.getDMinimum())/(double)getDLength();
        if ( !isHorizontal() ) alpha= 1.0 - alpha;
        if (dataRange.isLog()) {
            double data_log_min = Math.log(dataRange.getMinimum());
            double data_log_max = Math.log(dataRange.getMaximum());
            double data_log_range = data_log_max - data_log_min;
            double data_log= data_log_range*alpha + data_log_min;            
            data= Math.exp(data_log);
        }
        else {
            double minimum= dataRange.getMinimum();
            double maximum= dataRange.getMaximum();
            double data_range = maximum-minimum;
            data= data_range*alpha + minimum;            
        }
        DatumFormatter formatter = DatumUtil.bestFormatter(getDataMinimum(), getDataMaximum(), getDLength());
        Datum result= Datum.create( data, dataRange.getUnits(), formatter );
        return result;
    }

    /** TODO
     * @param tickv
     * @return
     */
    protected String tickFormatter(double tickv) {
        return datumFormatter.grannyFormat(Datum.create(tickv, getUnits()));
    }
    
    /** TODO
     * @param e
     */
    public void DataRangeSelected(DataRangeSelectionEvent e) {
        this.setDataRange(e.getMinimum(),e.getMaximum());
    }
    
    private Datum findTickLog( Datum xDatum, double direction, boolean minor ) {
        double x= xDatum.doubleValue(tickV.units);
        double result;
        if ( direction>0 ) { //find the smallest tick that is bigger than x.
            result= tickV.tickV[tickV.tickV.length-1] * 10;
            for (int i=tickV.tickV.length-1; i>=0; i--) {
                if ( x<tickV.tickV[i] ) result= tickV.tickV[i];
            }
        } else if ( direction<0) { // find the biggest tick that is smaller than x.
            result= tickV.tickV[0] / 10;
            for (int i=0; i<tickV.tickV.length; i++) {
                if ( x>tickV.tickV[i] ) result= tickV.tickV[i];
            }
        } else {
            result= -999;
            double closestDistance= Double.MAX_VALUE;
            for (int i=0; i<tickV.tickV.length; i++) {
                double dist= Math.abs( Math.log(x) - Math.log(tickV.tickV[i]) );
                if ( dist < closestDistance ) {
                    result= tickV.tickV[i];
                    closestDistance= dist;
                }
            }
        }
        return Datum.create(result,xDatum.getUnits());
    }
    
    /** TODO
     * @param xDatum
     * @param direction
     * @param minor
     * @return
     */
    public Datum findTick(Datum xDatum, double direction, boolean minor) {
        // direction<0 nearest left, direction>0 nearest right, direction=0 nearest.
        if (tickV==null) return xDatum;
        
        double[] ticks= new double[ tickV.tickV.length + tickV.minorTickV.length ];
        for ( int i=0; i<tickV.tickV.length; i++ ) ticks[i]= tickV.tickV[i];
        for ( int i=0; i<tickV.minorTickV.length; i++ ) ticks[i+tickV.tickV.length]= tickV.minorTickV[i];
        
        int iclose=0; double close=Double.MAX_VALUE;
        
        double x= xDatum.doubleValue(tickV.units);
        
        for ( int i=0; i<ticks.length; i++ ) {
            if ( direction<0 && ticks[i] < x && x-ticks[i] < close ) {
                iclose=i;
                close= x-ticks[i];
            } else if ( direction>0 && x < ticks[i] && ticks[i]-x < close ) {
                iclose=i;
                close= ticks[i]-x;
            }
            if ( direction==0 && Math.abs(ticks[i]-x) < close ) {
                iclose= i;
                close= Math.abs(ticks[i]-x);
            }
        }
        
        return Datum.create(ticks[iclose],tickV.units);
        
    }
    
    /** TODO
     * @param min0
     * @param max0
     * @param min1
     * @param max1
     */
    private void animateChange( double min0, double max0, double min1, double max1 ) {
        
        if ( animated ) {
            boolean drawTca0= getDrawTca();
            setDrawTca(false);

            long t0= System.currentTimeMillis();
            
            DataRange dataRange0= dataRange;
            DataRange tempRange=null;
            try { tempRange= (DataRange)dataRange.clone(); }
            catch ( CloneNotSupportedException e ) {
                DasExceptionHandler.handle(e);
                return;
            }
            
            this.dataRange= tempRange;
            
            double transitionTime= 200; // millis
            double alpha= ( System.currentTimeMillis() - t0 ) / transitionTime;
            
            while ( alpha < 1.0 ) {
                alpha= ( System.currentTimeMillis() - t0 ) / transitionTime;
                
                double t= -4 + 8 * alpha;
                double a1= (DasMath.tanh(t)+1)/2;
                double a0= 1-a1;
                
                if (isLog()) {
                    tempRange.setRange(Math.exp(Math.log(min0)*a0+Math.log(min1)*a1),
                    Math.exp(Math.log(max0)*a0+Math.log(max1)*a1));
                } else {
                    tempRange.setRange(min0*a0+min1*a1,
                    max0*a0+max1*a1);
                }
                //updateTickV();
                this.paintImmediately(0,0,this.getWidth(),this.getHeight());
            }
            
            
            setDrawTca(drawTca0);

            this.dataRange= dataRange0;
        }
    }
    
    /** TODO */
    protected void updateImmediately() {
        super.updateImmediately();
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
    static DasAxis processAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws DasPropertyException, DasNameException {
        String name = element.getAttribute("name");
        boolean log = element.getAttribute("log").equals("true");
        double dataMinimum = Double.parseDouble(element.getAttribute("dataMinimum"));
        double dataMaximum = Double.parseDouble(element.getAttribute("dataMaximum"));
        int orientation = parseOrientationString(element.getAttribute("orientation"));
        String rowString = element.getAttribute("row");
        if (!rowString.equals("") || row == null) {
            row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("") || row == null) {
            column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
        }
        DasAxis axis = new DasAxis(Datum.create(dataMinimum), Datum.create(dataMaximum),
        row, column, orientation, log);
        
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
        }
        else if (orientationString.equals("vertical")) {
            return VERTICAL;
        }
        else if (orientationString.equals("left")) {
            return LEFT;
        }
        else if (orientationString.equals("right")) {
            return RIGHT;
        }
        else if (orientationString.equals("top")) {
            return TOP;
        }
        else if (orientationString.equals("bottom")) {
            return BOTTOM;
        }
        else {
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
        }
        else {
            element = document.createElement("axis");
        }
        if (this.isAttached()) {
            element.setAttribute("ref", this.getMasterAxis().getDasName());
        }
        else {
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
    
    public DasAxis createAttachedAxis(DasRow row, DasColumn column) {
        return new DasAxis(this.dataRange, row, column, this.getOrientation());
    }

    /** TODO
     * @param row
     * @param column
     * @param orientation
     * @return
     */
    public DasAxis createAttachedAxis(DasRow row, DasColumn column, int orientation) {
        return new DasAxis(this.dataRange, row, column, orientation);
    }

    /** Process a <code>&lt;attachedaxis&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasAxis processAttachedaxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws DasPropertyException, DasNameException {
        String name = element.getAttribute("name");
        DasAxis ref = (DasAxis)form.checkValue(element.getAttribute("ref"), DasAxis.class, "<attachedaxis>");
        int orientation = (element.getAttribute("orientation").equals("horizontal") ? HORIZONTAL : DasAxis.VERTICAL);
        
        String rowString = element.getAttribute("row");
        if (!rowString.equals("") || row == null) {
            row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("") || column == null) {
            column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
        }
        
        DasAxis axis = ref.createAttachedAxis(row, column, orientation);
        
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
    
    /** TODO
     * @param name
     * @return
     */
    public static DasAxis createNamedAxis(String name) {
        DasAxis axis = new DasAxis(Datum.create(1.0, Units.dimensionless), Datum.create(10.0, Units.dimensionless), null, null, DasAxis.HORIZONTAL);
        if (name == null) {
            name = "axis_" + Integer.toHexString(System.identityHashCode(axis));
        }
        try {
            axis.setDasName(name);
        }
        catch (DasNameException dne) {
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
        }
        else {
            return primaryBounds;
        }
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

    public void TimeRangeSelected(TimeRangeSelectionEvent e) {
        if (false) {
            edu.uiowa.physics.pw.das.util.DasDie.println("received event");
            Graphics2D g= (Graphics2D)getGraphics();
            g.setColor(new Color(0,255,255,200));
            Rectangle dirty= new Rectangle(0,0,getWidth(),getHeight());
            g.fill(dirty);
            try { Thread.sleep(600); } catch ( InterruptedException ie ) {};
            paintImmediately(dirty);
        }
        if (!e.equals(lastProcessedEvent)) {
            setDataRange(e.getStartTime(),e.getEndTime());
            lastProcessedEvent= e;
            fireTimeRangeSelectionListenerTimeRangeSelected(e);
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
        if (false) {
            edu.uiowa.physics.pw.das.util.DasDie.println("firing event");
            Graphics2D g= (Graphics2D)getGraphics();
            g.setColor(new Color(255,255,0,200));
            Rectangle dirty= new Rectangle(0,0,getWidth(),getHeight());
            g.fill(dirty);
            try { Thread.sleep(600); } catch ( InterruptedException e ) {};
            paintImmediately(dirty);
        }
        if (timeRangeListenerList == null) return;
        Object[] listeners = timeRangeListenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class) {
                edu.uiowa.physics.pw.das.util.DasDie.println("fire event: "+this.getClass().getName()+"-->"+listeners[i+1].getClass().getName()+" "+event);
                ((edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener)listeners[i+1]).TimeRangeSelected(event);
            }
        }
    }

    static DasAxis processTimeaxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException, java.text.ParseException {
        String name = element.getAttribute("name");
        Datum timeMinimum = TimeUtil.create(element.getAttribute("timeMinimum"));
        Datum timeMaximum = TimeUtil.create(element.getAttribute("timeMaximum"));
        int orientation = parseOrientationString(element.getAttribute("orientation"));

        String rowString = element.getAttribute("row");
        if (!rowString.equals("") || row == null) {
            row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("") || column == null) {
            column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
        }

        DasAxis timeaxis = new DasAxis(timeMinimum, timeMaximum, row, column, orientation);

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
        }
        else {
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
            }
            else {
                verticalLayout();
            }
            if (drawTca && getOrientation() == BOTTOM && tcaData != null) {
                Rectangle bounds = primaryInputPanel.getBounds();
                int tcaHeight = (getTickLabelFont().getSize() + getLineSpacing())*tcaData.items;
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
            }
            else {
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
            }
            else {
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
    
}
