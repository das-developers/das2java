/* File: DasAxis.java
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
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.event.*;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.util.GrannyTextRenderer;
import edu.uiowa.physics.pw.das.datum.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;

/** TODO
 * @author eew
 */
public class DasAxis extends DasCanvasComponent implements DataRangeSelectionListener, Cloneable {
    
    /** TODO */    
    public static final int TOP = 1;
    /** TODO */    
    public static final int BOTTOM = 2;
    /** TODO */    
    public static final int LEFT = 3;
    /** TODO */    
    public static final int RIGHT = 4;
    /** TODO */    
    public static final int HORIZONTAL = BOTTOM;
    /** TODO */    
    public static final int VERTICAL = LEFT;
    /** TODO */    
    public static final int UP = 995;
    /** TODO */    
    public static final int DOWN = 996;
    
    private static final String SCAN_PREVIOUS_LABEL = "<< scan";
    private static final String SCAN_NEXT_LABEL = "scan >>";
    
    /** TODO */    
    protected DataRange dataRange;
    private DasCanvas parent;
    private boolean visible;
    private int orientation;
    private int tickDirection=1;  // 1=down or left, -1=up or right
    
    private boolean ticksVisible = true;
    
    private boolean tickLabelsVisible = true;
    
    /** TODO */    
    protected String axisLabel = "";
    
    private DasFormatter nf;
    
    private MouseModule zoom=null;
    
    private boolean oppositeAxisVisible;
    
    private PropertyChangeListener dataRangePropertyListener;
    
    /** TODO */    
    protected JPanel primaryInputPanel;
    /** TODO */    
    protected JPanel secondaryInputPanel;
    
    /** TODO */    
    protected ScanButton scanPrevious;
    /** TODO */    
    protected ScanButton scanNext;
    
    /** TODO */    
    protected static class tickVDescriptor {
        double [] tickV=null;
        double minor= 1.; // length of the minor ticks.
        boolean isLog=false;  // if true, then minor is wrt next less than major.
        boolean labelMinor=false; // used for log when there is less than one cycle.
        DasFormatter nf=null;
        
        Units units=null;
        
        /** Returns a String representation of the tickVDescriptor.
         * @return a String representation of the tickVDescriptor.
         */        
        public String toString() {
            String s="tickV=[";
            for (int i=0; i<tickV.length; i++) s+=tickV[i]+", ";
            s+="],minor="+minor;
            return s;
        }
    }
    
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
        DasAxis result;
        if ( units instanceof TimeLocationUnits ) {
            result= new DasTimeAxis((TimeDatum)Datum.create(0,units),(TimeDatum)Datum.create(0,units),row,column,orientation);
        } else {
            result= new DasAxis(Datum.create(0,units),Datum.create(0,units),row,column,orientation,isLog);
        }
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
        visible=true;
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
        add(primaryInputPanel);
        add(secondaryInputPanel);
    }
    
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
    
    /** TODO */    
    protected void maybeInitializeScanButtons() {
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
     * @param minimum
     * @param maximum
     */    
    public void setDataRange(Datum minimum, Datum maximum) {
        
        if ( ! maximum.gt(minimum) ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(edu.uiowa.physics.pw.das.util.DasDie.CRITICAL, "setDataRange where min > max ignored");
            return;
        }
        
        Units units= dataRange.getUnits();
        if (minimum.getUnits()!=units) {
            minimum.convertTo(units);
            maximum.convertTo(units);
        }
        
        double minimum0= dataRange.getMinimum();
        double maximum0= dataRange.getMaximum();
        animateChange( minimum0, maximum0, minimum.getValue(), maximum.getValue() );
        
        dataRange.setRange( minimum.getValue(), maximum.getValue() );
        update();
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
    
    /** TODO */    
    public void setDeviceRange(double minimum, double maximum) {
        if (isHorizontal()) {
            getColumn().setDPosition(minimum, maximum);
        }
        else {
            getRow().setDPosition(minimum, maximum);
        }
        deviceRangeChanged();
    }
    
    /** TODO */    
    protected void deviceRangeChanged() {}
    
    /** TODO
     * @return
     */    
    public Datum getDataMinimum() {
        Datum result= Datum.create( dataRange.getMinimum(), dataRange.getUnits() );
        // We're going to want to add a decimal place or two here
        result.setFormatter(nf);
        return result;
    }
    
    /** TODO
     * @return
     */    
    public Datum getDataMaximum() {
        Datum result= Datum.create( dataRange.getMaximum(), dataRange.getUnits() );
        // We're going to want to add a decimal place or two here
        result.setFormatter(nf);
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
      * @return
      */     
    public double getDeviceMinimum() {
        if (isHorizontal())
            return getColumn().getDMinimum();
        else return getRow().getDMinimum();
    }
    
    /** TODO
     * @return
     */    
    public double getDeviceMaximum() {
        if (isHorizontal())
            return getColumn().getDMaximum();
        else return getRow().getDMaximum();
    }
    
    /** TODO
     * @return
     */    
    public final double getDevicePosition() {
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
     * @param row
     * @param column
     * @return
     */    
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
        if (this instanceof DasTimeAxis ^ axis instanceof DasTimeAxis) {
            throw new IllegalArgumentException("Cannot attach a time axis to a non-time axis");
        }
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
            = new DataRange(this,new Datum(dataRange.getMinimum(), dataRange.getUnits()),
                            new Datum(dataRange.getMaximum(), dataRange.getUnits()),
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
    
    /** TODO */    
    protected tickVDescriptor tickV;
    
    /** TODO
     * @return
     */    
    protected tickVDescriptor getTickV() {
        if (tickV == null) updateTickV();
        return tickV;
    }
    
    /** TODO */    
    protected void updateTickV() {
        if (dataRange.isLog()) {
            
            tickVDescriptor ticks= new tickVDescriptor();
            ticks.units= dataRange.getUnits();
            double min= dataRange.getMinimum();
            double max= dataRange.getMaximum();
            
            ticks.isLog= true;
            
            double [] result;
            double dMinTick= edu.uiowa.physics.pw.das.util.DasMath.roundNDigits(Math.log(min)/Math.log(10),4);
            int minTick= (int)Math.ceil(dMinTick);
            double dMaxTick= edu.uiowa.physics.pw.das.util.DasMath.roundNDigits(Math.log(max)/Math.log(10),4);
            int maxTick= (int)Math.floor(dMaxTick);
            
            int stepSize= 1;
            
            int nTicks= ( maxTick - minTick ) / stepSize + 1;
            
            int pixelsPerTick;
            if (isHorizontal()) {
                pixelsPerTick = (int)getColumn().getWidth() / nTicks;
            }
            else {
                pixelsPerTick = (int)getRow().getHeight() / nTicks;
            }
            
            //if (nTicks>5) {
            if (pixelsPerTick < 30) {
                stepSize= (int) Math.floor( ( maxTick - minTick ) / 5. );
                minTick= (int) Math.ceil( minTick / (float)stepSize ) * stepSize;
                maxTick= (int) Math.floor( maxTick / (float)stepSize ) * stepSize;
                nTicks= ( maxTick - minTick ) / stepSize + 1;
                ticks.minor= 0.;  // don't draw minor ticks
            } else {
                ticks.minor= 1.;
            }
            
            result= new double[nTicks];
            for (int i=0; i<nTicks; i++) {
                result[i]= Math.exp((i*stepSize+minTick)*Math.log(10));
            }
            
            if (minTick>=maxTick) ticks.labelMinor= true;
            ticks.tickV= result;
            
            if ( edu.uiowa.physics.pw.das.util.DasMath.log10( max / min ) >= 5 ||
                 edu.uiowa.physics.pw.das.util.DasMath.log10( max ) >= 5 ||
                 edu.uiowa.physics.pw.das.util.DasMath.log10( min ) <= -5 ) {
                setFormatter( new DasFormatter( new DecimalFormat("0E0") ) );
            } else {
                DasFormatter nf= new DasFormatter( new DecimalFormat() );
                if (minTick<0) {
                    nf.setMinimumFractionDigits(-1*minTick);
                }
                setFormatter( nf );
            }
            
            ticks.nf= nf;
            
            tickV = ticks;
            return;
        }
        else {
            
            int nTicksMax;
            int axisSize;
            if (isHorizontal()) {
                int tickSizePixels= getFontMetrics(getTickLabelFont()).stringWidth("0.0000");
                axisSize= (int)this.getColumn().getWidth();
                nTicksMax= axisSize / tickSizePixels;
            } else {
                int tickSizePixels= getFontMetrics(getTickLabelFont()).getHeight();
                axisSize= (int)this.getRow().getHeight();
                nTicksMax= axisSize / tickSizePixels;
            }
            
            nTicksMax= (nTicksMax<7)?nTicksMax:7;
            
            tickVDescriptor res= new tickVDescriptor();
            
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
            
            if (absissa==5.) {
                res.minor= tickSize/5;
            } else if ( absissa==2. ) {
                res.minor= tickSize/2;
            } else {
                res.minor= tickSize/4;
            }
            
            int nPixels= axisSize;
            nf= Datum.getFormatter( getDataMinimum(), getDataMaximum(), nTicks );
            
            res.nf=nf;
            
            tickV = res;
            return;
        }
    }
    
    /** TODO */    
    protected void paintComponent(Graphics graphics) {
        Rectangle clip = graphics.getClipBounds();
        if (clip != null && clip.width <= 1 && clip.height <= 1) {
            return;
        }
        Graphics2D g = (Graphics2D)graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        if (isHorizontal()) {
            paintHorizontalAxis(g);
        }
        else {
            paintVerticalAxis(g);
        }
    }
    
    /** TODO */    
    protected void paintHorizontalAxis(Graphics2D g) {
        Rectangle bounds = getBounds();
        g.translate(-bounds.x, -bounds.y);
        
        boolean bottomTicks = (orientation == BOTTOM || oppositeAxisVisible);
        boolean bottomTickLabels = (orientation == BOTTOM && tickLabelsVisible);
        boolean bottomLabel = (orientation == BOTTOM && !axisLabel.equals(""));
        boolean topTicks = (orientation == TOP || oppositeAxisVisible);
        boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
        boolean topLabel = (orientation == TOP && !axisLabel.equals(""));
        
        int topPosition = (int)Math.round(getRow().getDMinimum()) - 1;
        int bottomPosition = (int)Math.round(getRow().getDMaximum());
        int DMax= (int)Math.round(getColumn().getDMaximum());
        int DMin= (int)Math.round(getColumn().getDMinimum());
        
        Font labelFont = getTickLabelFont();
        FontMetrics labelFontMetrics = getFontMetrics(labelFont);
        
        double dataMax= dataRange.getMaximum();
        double dataMin= dataRange.getMinimum();
        
        tickVDescriptor ticks= getTickV();
        double[] tickv= ticks.tickV;
        
        if (! (this instanceof DasTimeAxis )) {
            for (int i=0; i<tickv.length; i++) {
                double tickv3= (double)edu.uiowa.physics.pw.das.util.DasMath.roundNDigits(tickv[i],3);
                tickv[i]= tickv3;
            }
        }
        
        if (bottomTicks) {
            g.drawLine(DMin,bottomPosition,DMax,bottomPosition);
        }
        if (topTicks) {
            g.drawLine(DMin,topPosition,DMax,topPosition);
        }
        
        int tickLengthMajor = labelFont.getSize() * 2 / 3;
        int tickLengthMinor = tickLengthMajor / 2;
        int tickLength;
        int tickv_length= (tickv==null) ? 0 : tickv.length;
        
        // i=-1 indicates draw the minor ticks up to the first major tick (if any)
        for (int i=-1; i<tickv_length; i++) {
            String tickLabel;
            double minorTickV;
            double minorTickStop;
            double minorTickStart;
            double minorTickStep;
            if (i>=0) {
                minorTickStart= tickv[i];
                minorTickStop= ((i+1)<tickv_length) ? tickv[i+1] : dataMax;
                minorTickStep= (ticks.isLog) ? ticks.minor*tickv[i] : ticks.minor;
            } else {
                if (ticks.isLog) {
                    minorTickStop= (tickv_length>0) ? tickv[i+1] : dataMax;
                    if ( ticks.minor!=0. ) {
                        minorTickStep=
                        Math.exp(Math.log(10)*(int)Math.floor(Math.log(dataMin)/Math.log(10)));
                    } else {
                        minorTickStep=0.;
                    }
                    minorTickStart= Math.ceil( dataMin / minorTickStep ) * minorTickStep;
                } else {
                    minorTickStep= ticks.minor;
                    int nMinor= (int)(Math.floor((tickv[0]-dataMin)/minorTickStep));
                    minorTickStart= tickv[0]-nMinor*minorTickStep;
                    minorTickStop= tickv[0];
                }
            }
            int nMinor;
            if (minorTickStep==0.) {
                nMinor=1;
            } else {
                if ( minorTickStart > minorTickStop ) {
                    if ((( minorTickStop - minorTickStart ) / minorTickStep )>-0.01) {
                        minorTickStop= minorTickStart;
                    }
                }
                nMinor= 1+(int)( Math.floor(( minorTickStop - minorTickStart ) / minorTickStep ));
            }            
            
            for (int j = 0; j < nMinor; j++) {
                minorTickV= minorTickStart+j*minorTickStep;
                int tickPosition= (int)Math.floor(transform(minorTickV,ticks.units) + 0.5);
                if (ticks.labelMinor || ((j == 0) && (i >= 0))) {
                    tickLabel= tickFormatter(minorTickV);
                    tickLength= tickLengthMajor;
                    if (bottomTicks) {
                        g.drawLine( tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);    
                        if (bottomTickLabels) {
                            drawLabel(g, minorTickV, i, tickPosition, bottomPosition + tickLength);
                        }
                    }
                    if (topTicks) {
                        g.drawLine( tickPosition, topPosition, tickPosition, topPosition - tickLength);                    
                        if (topTickLabels) {
                            drawLabel(g, minorTickV, i, tickPosition, topPosition - tickLength);
                        }
                    }
                }
                else {
                    tickLength= tickLengthMinor;
                    if (bottomTicks) {
                        g.drawLine( tickPosition, bottomPosition, tickPosition, bottomPosition + tickLength);
                    }
                    if (topTicks) {
                        g.drawLine( tickPosition, topPosition, tickPosition, topPosition - tickLength);
                    }
                }
            } // for (int j=0; j<nMinor; j++)
        } // for (int i; i<tickv_length; i++)
        
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
    
    /** TODO */    
    protected void paintVerticalAxis(Graphics2D g) {
        Rectangle bounds = getBounds();
        g.translate(-bounds.x, -bounds.y);
        
        boolean leftTicks = (orientation == LEFT || oppositeAxisVisible);
        boolean leftTickLabels = (orientation == LEFT && tickLabelsVisible);
        boolean leftLabel = (orientation == LEFT && !axisLabel.equals(""));
        boolean rightTicks = (orientation == RIGHT || oppositeAxisVisible);
        boolean rightTickLabels = (orientation == RIGHT && tickLabelsVisible);
        boolean rightLabel = (orientation == RIGHT && !axisLabel.equals(""));
        
        int leftPosition = (int)Math.round(getColumn().getDMinimum()) - 1;
        int rightPosition = (int)Math.round(getColumn().getDMaximum());
        int DMax= (int)Math.round(getRow().getDMaximum());
        int DMin= (int)Math.round(getRow().getDMinimum());
        
        Font labelFont = getTickLabelFont();
        FontMetrics labelFontMetrics = getFontMetrics(labelFont);
        
        double dataMax= dataRange.getMaximum();
        double dataMin= dataRange.getMinimum();
        
        tickVDescriptor ticks= getTickV();
        double[] tickv= ticks.tickV;
        
        if (! (this instanceof DasTimeAxis )) {
            for (int i=0; i<tickv.length; i++) {
                double tickv3= (double)edu.uiowa.physics.pw.das.util.DasMath.roundNDigits(tickv[i],3);
                tickv[i]= tickv3;
            }
        }
        
        if (leftTicks) {
            g.drawLine(leftPosition,DMin,leftPosition,DMax);
        }
        if (rightTicks) {
            g.drawLine(rightPosition,DMin,rightPosition,DMax);
        }
        
        int tickLengthMajor= labelFont.getSize()*2/3;
        int tickLengthMinor;
        tickLengthMinor= tickLengthMajor * 2 / 3;
        int tickLength;
        
        int tickv_length= (tickv==null) ? 0 : tickv.length;
        
        // i=-1 indicates draw the minor ticks up to the first major tick (if any)
        for (int i=-1; i<tickv_length; i++) {
            String tickLabel;
            double minorTickV;
            double minorTickStop;
            double minorTickStart;
            double minorTickStep;
            if (i>=0) {
                minorTickStart= tickv[i];
                minorTickStop= ((i+1)<tickv_length) ? tickv[i+1] : dataMax;
                minorTickStep= (ticks.isLog) ? ticks.minor*tickv[i] : ticks.minor;
            } else {
                if (ticks.isLog) {
                    minorTickStop= (tickv_length>0) ? tickv[i+1] : dataMax;
                    if ( ticks.minor!=0. ) {
                        minorTickStep=
                        Math.exp(Math.log(10)*(int)Math.floor(Math.log(dataMin)/Math.log(10)));
                    } else {
                        minorTickStep=0.;
                    }
                    minorTickStart= Math.ceil( dataMin / minorTickStep ) * minorTickStep;
                } else {
                    minorTickStep= ticks.minor;
                    int nMinor= (int)(Math.floor((tickv[0]-dataMin)/minorTickStep));
                    minorTickStart= tickv[0]-nMinor*minorTickStep;
                    minorTickStop= tickv[0];
                }
            }
            int nMinor;
            if (minorTickStep==0.) {
                nMinor=1;
            } else {
                if ( minorTickStart > minorTickStop ) {
                    if ((( minorTickStop - minorTickStart ) / minorTickStep )>-0.01) {
                        minorTickStop= minorTickStart;
                    }
                }
                nMinor= 1+(int)( Math.floor(( minorTickStop - minorTickStart ) / minorTickStep ));
            }            
            
            for (int j = 0; j < nMinor; j++) {
                minorTickV= minorTickStart+j*minorTickStep;
                int tickPosition= (int)Math.floor(transform(minorTickV,ticks.units) + 0.5);
                if (ticks.labelMinor || ((j == 0) && (i >= 0))) {
                    tickLabel= tickFormatter(minorTickV);
                    tickLength= tickLengthMajor;
                    if (leftTicks) {
                        g.drawLine( leftPosition, tickPosition, leftPosition - tickLength, tickPosition );
                        if (leftTickLabels) {
                            drawLabel(g, minorTickV, i, leftPosition - tickLength, tickPosition);
                        }
                    }
                    if (rightTicks) {
                        g.drawLine( rightPosition, tickPosition, rightPosition + tickLength, tickPosition );
                        if (rightTickLabels) {
                            drawLabel(g, minorTickV, i, rightPosition + tickLength, tickPosition);
                        }
                    }
                }
                else {
                    tickLength= tickLengthMinor;
                    if (leftTicks) {
                        g.drawLine( leftPosition, tickPosition, leftPosition - tickLength, tickPosition  );
                    }
                    if (rightTicks) {
                        g.drawLine( rightPosition, tickPosition, rightPosition + tickLength, tickPosition  );
                    }
                }
            } // for (int j=0; j<nMinor; j++)
        } // for (int i; i<tickv_length; i++)
        
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
        
        if (orientation == BOTTOM) {
            return tickLabelFont.getSize() + tickLength + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize()/2;
        }
        else if (orientation == TOP) {
            return tickLength + fm.stringWidth(" ") + labelFont.getSize() + labelFont.getSize()/2 + (int)gtr.getDescent();
        }
        else if (orientation == LEFT) {
            return tickLength + getMaxLabelWidth(fm) + fm.stringWidth(" ") + labelFont.getSize()/2 + (int)gtr.getDescent();
        }
        else {
            return tickLength + getMaxLabelWidth(fm) + fm.stringWidth(" ") + labelFont.getSize()/2 + (int)gtr.getDescent();
        }
    }
    
    /** TODO */    
    protected void drawLabel(Graphics g, double value, int index, int x, int y) {
        
        if (!tickLabelsVisible) return;
        
        String label = tickFormatter(value);
        
        Font font0= g.getFont();
        
        g.setFont(getTickLabelFont());
        GrannyTextRenderer idlt= new GrannyTextRenderer();
        idlt.setString(this,label);
        
        int width = (int) idlt.getWidth();
        int height = (int) idlt.getHeight();
        int ascent = (int) idlt.getAscent();
        
        int tick_label_gap = getFontMetrics(getTickLabelFont()).stringWidth(" ");
        
        if (orientation == BOTTOM) {
            x -= width/2;
            y += ascent + tick_label_gap;
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
        idlt.draw(g,x,y);
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
        tickVDescriptor ticks = getTickV();
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
    public boolean isHorizontal() {
        return orientation == BOTTOM || orientation == TOP;
    }
    
    /** TODO
     * @return
     */    
    protected Rectangle getAxisBounds() {
        if (isHorizontal()) {
            return getHorizontalAxisBounds();
        }
        else {
            return getVerticalAxisBounds();
        }
    }
    
    private Rectangle getHorizontalAxisBounds() {
        int topPosition = (int)Math.round(getRow().getDMinimum()) - 1;
        int bottomPosition = (int)Math.round(getRow().getDMaximum());
        DasDevicePosition range = getColumn();
        int DMax = (int)Math.round(range.getDMaximum());
        int DMin = (int)Math.round(range.getDMinimum());
        
        boolean bottomTicks = (orientation == BOTTOM || oppositeAxisVisible);
        boolean bottomTickLabels = (orientation == BOTTOM && tickLabelsVisible);
        boolean bottomLabel = (orientation == BOTTOM && !axisLabel.equals(""));
        boolean topTicks = (orientation == TOP || oppositeAxisVisible);
        boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
        boolean topLabel = (orientation == TOP && !axisLabel.equals(""));
        
        Rectangle bounds;
        
        Font tickLabelFont = getTickLabelFont();
        
        int tickSize = tickLabelFont.getSize() * 2 / 3;
        
        //Initialize bounds rectangle
        if (oppositeAxisVisible) {
            bounds = new Rectangle(DMin, topPosition, DMax-DMin + 1, bottomPosition - topPosition);
        }
        else if (orientation == BOTTOM) {
            bounds = new Rectangle(DMin, bottomPosition, DMax-DMin + 1, 1);
        }
        else if (orientation == TOP) {
            bounds = new Rectangle(DMin, topPosition, DMax-DMin + 1, 1);
        }
        else {
            throw new IllegalStateException("Illegal axis orientation: " + orientation);
        }
        
        //Add room for ticks
        if (bottomTicks) {
            bounds.height += tickSize;
        }
        if (topTicks) {
            bounds.height += tickSize;
            bounds.y -= tickSize;
        }
        
        int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
        int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
        
        //Add room for tick labels
        if (bottomTickLabels) {
            bounds.height += tickLabelFont.getSize()*3/2 + tick_label_gap;
            bounds.x -= maxLabelWidth/2;
            bounds.width += maxLabelWidth;
        }
        if (topTickLabels) {
            bounds.y -= (tickLabelFont.getSize()*3/2 + tick_label_gap);
            bounds.height += tickLabelFont.getSize()*3/2 + tick_label_gap;
            bounds.x -= maxLabelWidth/2;
            bounds.width += maxLabelWidth;
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
        
        //Add room for the axis label
        Font labelFont = getLabelFont();
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(this, getLabel());
        int labelSpacing = (int)gtr.getHeight() + labelFont.getSize()/2;
        if (bottomLabel) {
            bounds.height += labelSpacing;
        }
        if (topLabel) {
            bounds.y -= labelSpacing;
            bounds.height += labelSpacing;
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
        
        int leftPosition = (int)Math.round(getColumn().getDMinimum()) - 1;
        int rightPosition = (int)Math.round(getColumn().getDMaximum());
        int DMax= (int)Math.round(getRow().getDMaximum());
        int DMin= (int)Math.round(getRow().getDMinimum());
        
        Rectangle bounds;
        
        Font tickLabelFont = getTickLabelFont();
        
        int tickSize = tickLabelFont.getSize() * 2 / 3;
        
        //Initialize bounds rectangle
        if (oppositeAxisVisible) {
            bounds = new Rectangle(leftPosition, DMin, rightPosition - leftPosition, DMax - DMin + 1);
        }
        else if (orientation == LEFT) {
            bounds = new Rectangle(leftPosition, DMin, 1, DMax-DMin + 1);
        }
        else if (orientation == RIGHT) {
            bounds = new Rectangle(rightPosition, DMin, 1, DMax-DMin + 1);
        }
        else {
            throw new IllegalStateException("Illegal axis orientation: " + orientation);
        }
        
        //Add room for ticks
        if (leftTicks) {
            bounds.width += tickSize; // = new Rectangle(position-tickSize, DMin, tickSize, DMax-DMin);
            bounds.x -= tickSize;
        }
        if (rightTicks) {
            bounds.width += tickSize; // = new Rectangle(position, DMin, tickSize, DMax-DMin);
        }
        
        int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
        int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
        
        //Add room for tick labels
        if (leftTickLabels) {
            bounds.x -= (maxLabelWidth + tick_label_gap);
            bounds.width += maxLabelWidth + tick_label_gap;
            bounds.y -= tickLabelFont.getSize();
            bounds.height += tickLabelFont.getSize()*2;
        }
        if (rightTickLabels) {
            bounds.width += maxLabelWidth + tick_label_gap;
            bounds.y -= tickLabelFont.getSize();
            bounds.height += tickLabelFont.getSize()*2;
        }
        
        //Add room for the axis label
        Font labelFont = getLabelFont();
        GrannyTextRenderer gtr = new GrannyTextRenderer();
        gtr.setString(this, getLabel());
        int labelSpacing = (int)gtr.getHeight() + labelFont.getSize()/2;
        if (leftLabel) {
            bounds.x -= labelSpacing;
            bounds.width += labelSpacing;
        }
        if (rightLabel) {
            bounds.width += labelSpacing;
        }
        
        return bounds;
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
    public DasFormatter getFormatter() {
        
        if (nf==null) {
            updateTickV();
            /*if ( isLog() ) {
                double max= dataRange.maximum;
                double min= dataRange.minimum;
                if ( edu.uiowa.physics.pw.das.util.DasMath.log10( max / min ) >= 5 ) {
                    setFormatter( new DasFormatter( new DecimalFormat("0E0") ) );
                } else {
                    DasFormatter nf= new DasFormatter( new DecimalFormat() );
                    //if (minTick<0) {
                    //    nf.setMinimumFractionDigits(-1*minTick);
                    //}
                    setFormatter( nf );
                }
            } else {
                setFormatter( getDataMinimum().getFormatter(getDataMaximum(),6) );
            }*/
        }
        return this.nf;
    }
    
    /** TODO
     * @return
     */    
    public int getTickDirection() {
        return tickDirection;
    }
    
    
    /** TODO
     * @param datum
     * @return
     */    
    public double transform(Datum datum) {
        return transform( datum.getValue(), datum.getUnits() );
    }
    
    double transform( double data, Units units ) {
        DasDevicePosition range;
        if (isHorizontal()) {
            range= getColumn();
            return transform( data, units, range.getDMinimum(), range.getDMaximum() );
        } else {
            range= getRow();
            return transform( data, units, range.getDMaximum(), range.getDMinimum() );
        }                    
    }
        
    double transform( double data, Units units, double dmin, double dmax ) {
        if ( units!=dataRange.getUnits() ) {
            data= Units.getConverter(dataRange.getUnits(),units).convert(data);
        }
               
        double device_range= dmax - dmin;
        double result;
                
        if (dataRange.isLog()) {

            double data_log = Math.log(data);
            double data_log_min = Math.log(dataRange.getMinimum());
            double data_log_max = Math.log(dataRange.getMaximum());
            double data_log_range = data_log_max - data_log_min;
            
            result= device_range*(data_log-data_log_min)/data_log_range + dmin;
        }
        else {
            
            double minimum= dataRange.getMinimum();
            double maximum= dataRange.getMaximum();
            double data_range = maximum-minimum;
            result= device_range*(data-minimum)/data_range + dmin;
        }
        
        if ( result > 10000 ) result=10000;
        if ( result < -10000 ) result=-10000;
        return result;
    }
    
    /** TODO
     * @param idata
     * @return
     */    
    public Datum invTransform(double idata) {
        double data;
        int nPixels;
        if (dataRange.isLog()) {
            DasDevicePosition range = (isHorizontal()
            ? (DasDevicePosition) getColumn()
            : (DasDevicePosition) getRow());
            double device_range = range.getDMaximum() - range.getDMinimum();
            nPixels= (int)device_range;
            double data_log_min = Math.log(dataRange.getMinimum());
            double data_log_max = Math.log(dataRange.getMaximum());
            double data_log_range = data_log_max - data_log_min;
            double data_log= (isHorizontal()
            ? data_log_min+data_log_range*(idata-range.getDMinimum())/device_range
            : data_log_min+data_log_range*(1-(idata-range.getDMinimum())/device_range));
            data= Math.exp(data_log);
        }
        else {
            DasDevicePosition range = (isHorizontal()
            ? (DasDevicePosition) getColumn()
            : (DasDevicePosition) getRow());
            double device_range = range.getDMaximum()-range.getDMinimum();
            nPixels= (int)device_range;
            double minimum= dataRange.getMinimum();
            double maximum= dataRange.getMaximum();
            double data_range = maximum-minimum;
            data= (isHorizontal()
            ? data_range*(idata-range.getDMinimum())/device_range + minimum
            : data_range*(1-(idata-range.getDMinimum())/device_range) + minimum);
        }
        Datum result= Datum.create( data, dataRange.getUnits() );
        result.setFormatter( getDataMinimum().getFormatter( getDataMaximum(), nPixels ) );
        return result;
    }
    
    private int button;
    private boolean isShiftDown;
    private Point zoomStart;
    private Point zoomEnd;
    
    private boolean animated=false;
    
    /** TODO
     * @param tickv
     * @return
     */    
    protected String tickFormatter(double tickv) {
        if (nf==null) {
            edu.uiowa.physics.pw.das.util.DasDie.println("this");
        }
        return nf.grannyFormat(tickv,getUnits());
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
    
    /** TODO
     * @param max
     */    
    public void setDataMaximum(Datum max) {
        max= max.convertTo(dataRange.getUnits());
        dataRange.setMaximum(max.getValue());
        update();
    }
    
    /** TODO
     * @param min
     */    
    public void setDataMinimum(Datum min) {
        min= min.convertTo(dataRange.getUnits());
        dataRange.setMinimum(min.getValue());
        update();
    }
    
    /** TODO
     * @param e
     */    
    public void DataRangeSelected(DataRangeSelectionEvent e) {
        this.setDataRange(e.getMinimum(),e.getMaximum());
    }
    
    private Datum findTickLog( Datum xDatum, double direction, boolean minor ) {
        double x= xDatum.getValue();
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
        return new Datum(result,xDatum.getUnits());
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
     
        if (xDatum.getUnits()!=getUnits()) {
            xDatum= xDatum.convertTo(getUnits());
        }
        
        if (isLog()) return findTickLog(xDatum,direction,minor);
        
        double x= xDatum.getValue();
        
        double stepSize;
        if (minor) {
            stepSize= tickV.minor;
        } else {
            stepSize= (tickV.tickV[1]-tickV.tickV[0]);
        }
        double tickv0= tickV.tickV[0];
        
        double result;
        if (direction<0.) {
            result= stepSize * Math.floor( ( x - tickv0 ) / stepSize ) + tickv0;
        } else if (direction>0.) {
            result= stepSize * Math.ceil( ( x - tickv0 ) / stepSize ) + tickv0;
        } else {
            result= stepSize * Math.round( ( x - tickv0 ) / stepSize ) + tickv0;
        }
        return Datum.create(result,getUnits());
    }
    
    /** TODO
     * @param min0
     * @param max0
     * @param min1
     * @param max1
     */    
    public void animateChange( double min0, double max0, double min1, double max1 ) {
        
        if ( animated ) {
            boolean drawTca0=false;
            if ( this instanceof DasTimeAxis ) {
                drawTca0= ((DasTimeAxis)this).getDrawTca();
                ((DasTimeAxis)this).setDrawTca(false);
            }
        
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
                double a1= (edu.uiowa.physics.pw.das.util.DasMath.tanh(t)+1)/2;
                double a0= 1-a1;
                
                if (isLog()) {
                    tempRange.setRange(Math.exp(Math.log(min0)*a0+Math.log(min1)*a1),
                    Math.exp(Math.log(max0)*a0+Math.log(max1)*a1));
                } else {
                    tempRange.setRange(min0*a0+min1*a1,
                    max0*a0+max1*a1);
                }
                updateTickV();
                this.paintImmediately(0,0,this.getWidth(),this.getHeight());
            }
            
            
            if ( this instanceof DasTimeAxis ) {
                ((DasTimeAxis)this).setDrawTca(drawTca0);
            }
            
            this.dataRange= dataRange0;
        }
    }
    
    /** TODO
     * @return
     */    
    public Units getUnits() {
        return dataRange.getUnits();
    }
    
    /** TODO */    
    protected void updateImmediately() {
        super.updateImmediately();
        updateTickV();       
    }
    
    /*  FOLLOWING NOT FULLY IMPLEMENTED
    public boolean areTicksVisible() {
        return ticksVisible;
    }
     
    public void setTicksVisible(boolean b) {
        if (ticksVisible == b) return;
        boolean oldValue = ticksVisible;
        ticksVisible = b;
        update();
        firePropertyChange("ticksVisible", oldValue, b);
    }
     */
    
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
    
    /** TODO
     * @param nf
     */    
    public void setFormatter(DasFormatter nf) {
        this.nf= nf;
    }
    
    /** TODO */    
    protected void installComponent() {
        super.installComponent();
    }
    
    /** TODO */    
    protected void uninstallComponent() {
        super.uninstallComponent();
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
    
    /** Process an <code>&lt;axis&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasAxis processAxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
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
        DasAxis axis = new DasAxis(new Datum(dataMinimum), new Datum(dataMaximum),
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
    
    /** Process a <code>&lt;attachedaxis&gt;</code> element.
     *
     * @param element The DOM tree node that represents the element
     */
    static DasAxis processAttachedaxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
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
        
        if (axis instanceof DasTimeAxis) {
            ((DasTimeAxis)axis).setDataPath(element.getAttribute("dataPath"));
            ((DasTimeAxis)axis).setDrawTca(element.getAttribute("showTca").equals("true"));
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
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        return axis;
    }
    
    /** TODO
     * @return
     */    
    public DataRange getDataRange() {
        return this.dataRange;
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
            java.awt.geom.GeneralPath path = new java.awt.geom.GeneralPath(primaryBounds);
            path.setWindingRule(java.awt.geom.GeneralPath.WIND_EVEN_ODD);
            path.append(secondaryBounds, false);
            return path;
        }
        else {
            return primaryBounds;
        }
    }
    
    /** TODO */    
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
        }
        
        /** TODO */        
        protected void horizontalLayout() {
            int topPosition = (int)Math.round(getRow().getDMinimum()) - 1;
            int bottomPosition = (int)Math.round(getRow().getDMaximum());
            int DMax = (int)Math.round(getColumn().getDMaximum());
            int DMin = (int)Math.round(getColumn().getDMinimum());

            boolean bottomTicks = (orientation == BOTTOM || oppositeAxisVisible);
            boolean bottomTickLabels = (orientation == BOTTOM && tickLabelsVisible);
            boolean bottomLabel = (orientation == BOTTOM && !axisLabel.equals(""));
            boolean topTicks = (orientation == TOP || oppositeAxisVisible);
            boolean topTickLabels = (orientation == TOP && tickLabelsVisible);
            boolean topLabel = (orientation == TOP && !axisLabel.equals(""));
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
            int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
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
            boolean leftLabel = (orientation == LEFT && !axisLabel.equals(""));
            boolean rightTicks = (orientation == RIGHT || oppositeAxisVisible);
            boolean rightTickLabels = (orientation == RIGHT && tickLabelsVisible);
            boolean rightLabel = (orientation == RIGHT && !axisLabel.equals(""));
            int leftPosition = (int)Math.round(getColumn().getDMinimum()) - 1;
            int rightPosition = (int)Math.round(getColumn().getDMaximum());
            int DMax= (int)Math.round(getRow().getDMaximum());
            int DMin= (int)Math.round(getRow().getDMinimum());
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
    
    /** TODO
     * @author eew
     */
    protected static class ScanButton extends JButton {
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
            setBorder(new javax.swing.border.CompoundBorder(
                new javax.swing.border.LineBorder(Color.BLACK),
                new javax.swing.border.EmptyBorder(2,2,2,2)));
            addMouseListener(new MouseAdapter() {
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
