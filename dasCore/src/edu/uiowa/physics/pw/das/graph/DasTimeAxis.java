/* File: DasTimeAxis.java
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
import edu.uiowa.physics.pw.das.dasml.FormBase;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.event.TimeRangeSelectionEvent;
import edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener;
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.util.GrannyTextRenderer;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.dataset.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author  eew
 */
public class DasTimeAxis extends DasAxis implements Cloneable, TimeRangeSelectionListener {
    
    private int context;
    
    private XMultiYDataSetDescriptor dsd;
    private TCADataSet tcaData;
    private String dataset = "";
    private boolean error;
    private String errorString;
    private boolean drawTca;
    private JButton fred;
    
    /** Utility field used by event firing mechanism. */
    private javax.swing.event.EventListenerList listenerList =  null;
    
    //Initialization Block: executes after super constructor and member
    //initializers but before constructor.  Executes regardless of constructor
    //called
    {
        setLayout(new TimeAxisLayoutManager());
    }
    
    public static DasTimeAxis create( double[] timeOffset, Units units, DasRow row, DasColumn column, int orientation) {
        return (DasTimeAxis)DasAxis.create( timeOffset, units, row, column, orientation, false );
    }
    
    protected DasTimeAxis( DataRange dataRange, DasRow row, DasColumn column, int orientation ) {
        super( dataRange, row, column, orientation);
        maybeInitializeScanButtons();
        setAnimated(true);
    }
    
    public DasTimeAxis( DasDate timeBase, DasDate timeMax, DasRow row, DasColumn column, int orientation) {
        this((TimeDatum)TimeUtil.create(timeBase), (TimeDatum)TimeUtil.create(timeMax), row, column, orientation);
    }
    
    public DasTimeAxis( TimeDatum startt, TimeDatum endt, DasRow row, DasColumn column, int orientation) {
        super(startt, endt, row, column, orientation, false);
        maybeInitializeScanButtons();
        setAnimated(true);
    }
    
    /** Constructs a DasTimeAxis that displays TCA data from the given data path that is
     * obtained by the specified reader.
     *
     * @param reader TCADataSet reader used to retreive data sets.
     * @param dataPath The data path used to retrieve TCA data
     * @param timeBase The start of the time interval this axis represents.
     * @param timeMax The end of the time interval this axis represents
     * @param row The row in which this axis resides
     * @param column The column in which this axis resides
     * @param orientation The orientation of this axis
     * (Either DasAxis.HORIZONTAL or DasAxis.VERTICAL)
     * @deprecated use DasTimeAxis(String, DasDate, DasDate, DasRow, DasColumn, int)
     */
    public DasTimeAxis(String dataPath, DasDate timeBase, DasDate timeMax,
    DasRow row, DasColumn column, int orientation) {
        this(timeBase, timeMax, row, column, orientation );
        setDataPath(dataPath);
        setDrawTca(true);
        setAnimated(true);
    }
    
    public DasTimeAxis(XMultiYDataSetDescriptor dsd, DasDate timeBase, DasDate timeMax, DasRow row, DasColumn column, int orientation) {
        this(timeBase, timeMax, row, column, orientation);
        this.dsd = dsd;
    }
    
    public DasAxis createAttachedAxis(DasRow row, DasColumn column) {
        return new DasTimeAxis(this.dataRange, row, column, this.getOrientation());
    }
    
    public DasAxis createAttachedAxis(DasRow row, DasColumn column, int orientation) {
        return new DasTimeAxis(this.dataRange, row, column, orientation);
    }
    
    public void setLog(boolean b) {
        throw new UnsupportedOperationException("The log property of a DasTimeAxis instance cannot be changed");
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
            dsd = (XMultiYDataSetDescriptor)DataSetDescriptor.create(dataset);
        }
        catch (edu.uiowa.physics.pw.das.DasException de) {
            DasExceptionHandler.handle(de);
        }
        update();
        firePropertyChange("dataPath", oldValue, dataset);
    }
    
    public void setDataRange( DasDate timeMin, DasDate timeMax ) {
        super.setDataRange( TimeUtil.create(timeMin), TimeUtil.create(timeMax) );
        TimeRangeSelectionEvent e= new TimeRangeSelectionEvent(this,(TimeDatum)this.getDataMinimum(),(TimeDatum)this.getDataMaximum());
        fireTimeRangeSelectionListenerTimeRangeSelected(e);
    }
    
    public DasDate getTimeMaximum() {
        return DasDate.create((TimeDatum)getDataMaximum());
    }
    
    public DasDate getTimeMinimum() {
        return DasDate.create((TimeDatum)getDataMinimum());
    }
    
    public void setTimeMaximum(DasDate d) {
        setDataMaximum(TimeUtil.create(d));
    }
    
    public void setTimeMinimum(DasDate d) {
        setDataMinimum(TimeUtil.create(d));
    }
    
    public void updateTickV() {
        tickVDescriptor res= new tickVDescriptor();
        
        double data_minimum = getDataMinimum().doubleValue(Units.t2000);
        double data_maximum = getDataMaximum().doubleValue(Units.t2000);
        
        double [] tickV;
        
        int nTicksMax;
        if (isHorizontal()) {
            GrannyTextRenderer idlt= new GrannyTextRenderer();
            idlt.setString(this, "0000-00-00");
            int tickSizePixels= (int) idlt.getWidth();
            int axisSize= (int)this.getColumn().getWidth();
            nTicksMax= axisSize / tickSizePixels;
        } else {
            int tickSizePixels= getFontMetrics(getTickLabelFont()).getHeight();
            int axisSize= (int)this.getRow().getHeight();
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
            86400, 172800, 86400*10, 86400*15, 86400*30
        };
        
        int[] nminor= {
            4, 4, 5,
            4, 4, 5,
            4, 4, 5,
            4, 4, 5, 5, 3,
            6, 4, 5, 5, 4,
            4, 4, 3, 4, 3, 4, 6,
            4, 4, 2, 2, 3 
        };
        
        double mag_keep=-1;
        double absissa;
        double mag;
        
        double tickSize, firstTick, lastTick;
        int nTicks;
        
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
            res.minor= mags12[ikeep]/nminor[ikeep];
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
            
            setFormatter( Datum.getFormatter( getDataMinimum(), getDataMaximum(), nTicks ) );
            
        } else  { // pick off month boundaries
            double [] result= new double[30];
            int ir=0;
            DasDate current;
            DasDate min= DasDate.create( (TimeDatum)Datum.create(data_minimum,Units.t2000));
            DasDate max= DasDate.create( (TimeDatum)Datum.create(data_maximum,Units.t2000));
            int step;
            int nstep=1;
            if ((data_maximum-data_minimum)<86400*30*6) {  // months
                step= DasDate.MONTH;
                res.minor= 86400*10;
            } else if ((data_maximum-data_minimum)<86400*30*25) { // seasons
                step= DasDate.QUARTER;
                res.minor= 30*86400;
            } else if ((data_maximum-data_minimum)<86400*365*6) { // years
                step= DasDate.YEAR;
                res.minor= 365.25/4*86400; 
            } else {
                step= DasDate.YEAR;
                res.minor= 365*86400;
                nstep= 2;
            }
            current= DasDate.create((TimeDatum)Datum.create(data_minimum,Units.t2000)).next(step);
            while(max.subtract(current)>0) {
                result[ir++]= TimeUtil.create(current).doubleValue(Units.t2000);
                current= current.next(step);
                for (int ii=nstep; ii>1; ii--) current= current.next(step);
            }
            
            tickV= new double[ir];
            for (ir=0; ir<tickV.length; ir++) tickV[ir]= result[ir];
            
            res.tickV= tickV;
            
            setFormatter( Datum.getFormatter( getDataMinimum(), getDataMaximum(), 6 ) );
        }
        
        UnitsConverter uc= Units.getConverter(Units.t2000,getUnits());
        for (int ii=0; ii<res.tickV.length; ii++) {
            res.tickV[ii]= uc.convert(res.tickV[ii]);
        }
        Units units= getUnits();
        Units offsetUnits= ((TimeLocationUnits)getUnits()).getOffsetUnits();
        uc= Units.getConverter( Units.seconds, ((TimeLocationUnits)getUnits()).getOffsetUnits() );
        res.minor= Units.getConverter( Units.seconds, ((TimeLocationUnits)getUnits()).getOffsetUnits() ).convert( res.minor );
        res.units= getUnits();
        
        context= (tickV.length < 2
        ? DasDate.context.getContextFromSeconds(1.0)
        : DasDate.context.getContextFromSeconds(tickV[1]-tickV[0]));
        
        this.tickV = res;
        updateDataSet();
    }
    
    public int getContext() {
        return context;
    }
    
    
    TimeRangeSelectionEvent lastProcessedEvent=null;
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
        if (listenerList == null ) {
            listenerList = new javax.swing.event.EventListenerList();
        }
        listenerList.add(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
    }
    
    /** Removes TimeRangeSelectionListener from the list of listeners.
     * @param listener The listener to remove.
     */
    public synchronized void removeTimeRangeSelectionListener(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener listener) {
        listenerList.remove(edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class, listener);
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
        if (listenerList == null) return;
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener.class) {
                edu.uiowa.physics.pw.das.util.DasDie.println("fire event: "+this.getClass().getName()+"-->"+listeners[i+1].getClass().getName()+" "+event);
                ((edu.uiowa.physics.pw.das.event.TimeRangeSelectionListener)listeners[i+1]).TimeRangeSelected(event);
            }
        }
    }
    
    protected void deviceRangeChanged() {
        update();
    }
    
    DataRequestThread drt;
    
    private void updateDataSet() {
        if (!drawTca || dataset.equals("") || dsd==null) return;
        double [] tickV = getTickV().tickV;
        Units units= getTickV().units;
        final Datum data_minimum;
        final Datum data_maximum;
        Datum iinterval;
        if (tickV.length == 1) {
            data_minimum = Datum.create(tickV[0],getTickV().units);
            data_maximum = Datum.create(tickV[0] + 1.0, getTickV().units);
            iinterval = data_maximum.subtract(data_minimum);
        }
        else {
            data_minimum = Datum.create(tickV[0],getTickV().units);
            data_maximum = Datum.create(tickV[tickV.length-1],getTickV().units);
            iinterval = (data_maximum.subtract(data_minimum)).divide(tickV.length-1);
        }
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
                drt.request(dsd, new Double(interval.doubleValue(Units.seconds)),
                                 data_minimum,
                                 data_maximum.add(Datum.create(1.0,Units.seconds)), 
                                 Datum.create(0.0,Units.seconds), requestor);
            }
            catch (InterruptedException ie) {
                DasExceptionHandler.handle(ie);
            }
        /*
        try {
            DataSet x= dsd.getDataSet(
                new Double(interval.convertTo(Units.seconds).getValue()),
                DasDate.create(data_minimum),
                DasDate.create((TimeDatum)data_maximum.add(new Datum(1.0,Units.seconds)))
            );
            tcaData = (TCADataSet)x;
        }
        catch (edu.uiowa.physics.pw.das.DasException de) {
            error = true;
            errorString = de.getMessage();
            DasExceptionHandler.handle(de);
        }
         */
    }
    
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D)graphics.create();
        super.paintComponent(g);
        g.translate(-getX(), -getY());
        
        if (drawTca && getOrientation() == BOTTOM && tcaData != null) {
            
            int position = (int)Math.round(getRow().getDMaximum());
            int DMin = (int)Math.round(getColumn().getDMinimum());
            Font tickLabelFont = getTickLabelFont();
            FontMetrics tickLabelFontMetrics = getFontMetrics(tickLabelFont);
            int tickLength = tickLabelFont.getSize() * 2 / 3;
            int tick_label_gap = tickLabelFontMetrics.stringWidth(" ");
            int lineHeight = tickLabelFont.getSize() + getLineSpacing();
            int maxLabelWidth = getMaxLabelWidth(tickLabelFontMetrics);
            
            int baseLine = position + tickLength + tick_label_gap + tickLabelFont.getSize();
            int rightEdge = DMin - maxLabelWidth/2 - tick_label_gap;
            
            GrannyTextRenderer idlt = new GrannyTextRenderer();
            idlt.setString(this, "SCET");
            int width = (int)Math.ceil(idlt.getWidth());
            int leftEdge = rightEdge - width;
            idlt.draw(g, (float)leftEdge, (float)baseLine);
            
            for (int i = 0; i < tcaData.items; i++) {
                baseLine += lineHeight;
                idlt.setString(this, tcaData.label[i]);
                width = (int)Math.floor(idlt.getWidth() + 0.5);
                leftEdge = rightEdge - width;
                idlt.draw(g, (float)leftEdge, (float)baseLine);
            }
        }
    }
    
    protected int getMaxLabelWidth(FontMetrics fm) {
        return Math.max(fm.stringWidth("0000-00-00"), fm.stringWidth("-00000.00"));
    }
    
    protected void drawLabel(Graphics g, double value, int index, int x, int y) {
        
        String label = tickFormatter(value);
        int width = getFontMetrics(getTickLabelFont()).stringWidth(label);
        int height = getTickLabelFont().getSize();
        int tick_label_gap = getFontMetrics(getTickLabelFont()).stringWidth(" ");
        int baseLine;
        int leftEdge;
        
        if (getOrientation() == BOTTOM) {
            leftEdge = x - width/2;
            baseLine = y + height + tick_label_gap;
        }
        else if (getOrientation() == TOP) {
            leftEdge = x - width/2;
            baseLine = y - tick_label_gap;
        }
        else if (getOrientation() == LEFT) {
            leftEdge = x - (width + tick_label_gap);
            baseLine = y + height/2;
        }
        else {
            leftEdge = x + tick_label_gap;
            baseLine = y + height/2;
        }
        
        g.drawString(label, leftEdge, baseLine);
        
        if (drawTca && getOrientation() == BOTTOM && tcaData != null) {
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
            TimeDatum date = (TimeDatum)Datum.create(value,getUnits());
            DasTimeFormatter nf= (DasTimeFormatter)getFormatter();
            if (TimeUtil.getSecondsSinceMidnight(date) == 0 &&
            nf.getContext() == TimeContext.HOURS ) {
                DasTimeFormatter nfdate= new DasTimeFormatter(TimeContext.DAYS);
                label = nfdate.format(date);
                width = getFontMetrics(getTickLabelFont()).stringWidth(label);
                leftEdge = x - width/2;
                baseLine += height + getLineSpacing();
                g.drawString(label, leftEdge, baseLine);
            }
        }
        else if (getOrientation() == BOTTOM && areTickLabelsVisible()) {
            TimeDatum date = (TimeDatum)Datum.create(value,getUnits());
            DasTimeFormatter nf= (DasTimeFormatter)getFormatter();
            if (TimeUtil.getSecondsSinceMidnight(date) == 0 &&
            nf.getContext() == TimeContext.HOURS ) {
                DasTimeFormatter nfdate= new DasTimeFormatter(TimeContext.DAYS);
                label = nfdate.format(date);
                width = getFontMetrics(getTickLabelFont()).stringWidth(label);
                leftEdge = x - width/2;
                baseLine = y + height*2 + tick_label_gap + getLineSpacing();
                g.drawString(label, leftEdge, baseLine);
            }
        }
        
    }
    
    private static final Pattern pattern = Pattern.compile("\\([eEfF]\\d+.\\d+\\)");
    
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
            DecimalFormat form = new DecimalFormat(new String(buf));
            result = form.format(d);
        }
        
        if (result.length() > length) {
            Arrays.fill(buf, '*');
            return new String(buf);
        }
        
        while (result.length() < length)
            result = " " + result;
        
        return result;
    }
    
    protected Rectangle getAxisBounds() {
        Rectangle bounds = super.getAxisBounds();
        
        if (getOrientation() == BOTTOM && areTickLabelsVisible()) {
            if (drawTca && tcaData != null) {
                int DMin = (int)Math.round(getColumn().getDMinimum());
                int DMax = (int)Math.round(getColumn().getDMaximum());
                Font tickLabelFont = getTickLabelFont();
                int tick_label_gap = getFontMetrics(tickLabelFont).stringWidth(" ");
                int tcaHeight = (tickLabelFont.getSize() + getLineSpacing())*tcaData.items;
                int maxLabelWidth = getMaxLabelWidth(getFontMetrics(tickLabelFont));
                bounds.height += tcaHeight;
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
                }
            }
            bounds.height += getTickLabelFont().getSize() + getLineSpacing();
            if (getTickDirection() == -1)
                bounds.y -= getTickLabelFont().getSize() + getLineSpacing();
            
        }
        
        return bounds;
    }
    
    protected int getTitlePositionOffset() {
        int offset = super.getTitlePositionOffset();
        if (getOrientation() == BOTTOM) {
            Font tickLabelFont = getTickLabelFont();
            if (drawTca && tcaData != null) {
                offset += tcaData.items * (tickLabelFont.getSize() + getLineSpacing());
            }
            offset += tickLabelFont.getSize() + getLineSpacing();
        }
        return offset;
    }
    
    protected void updateImmediately() {
        super.updateImmediately();
        //getDataSet();
    }
    
    public int getLineSpacing() {
        return getTickLabelFont().getSize()/4;
    }

    static DasTimeAxis processTimeaxisElement(Element element, DasRow row, DasColumn column, FormBase form) throws edu.uiowa.physics.pw.das.DasPropertyException, edu.uiowa.physics.pw.das.DasNameException {
        String name = element.getAttribute("name");
        DasDate timeMinimum = new DasDate(element.getAttribute("timeMinimum"));
        DasDate timeMaximum = new DasDate(element.getAttribute("timeMaximum"));
        int orientation = parseOrientationString(element.getAttribute("orientation"));
    
        String rowString = element.getAttribute("row");
        if (!rowString.equals("") || row == null) {
            row = (DasRow)form.checkValue(rowString, DasRow.class, "<row>");
        }
        String columnString = element.getAttribute("column");
        if (!columnString.equals("") || column == null) {
            column = (DasColumn)form.checkValue(columnString, DasColumn.class, "<column>");
        }
    
        DasTimeAxis timeaxis = new DasTimeAxis(timeMinimum, timeMaximum, row, column, orientation);
    
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
    
    public Element getDOMElement(Document document) {
        Element element;
        if (this.isAttached()) {
            element = document.createElement("attachedaxis");
            element.setAttribute("ref", this.getMasterAxis().getDasName());
        }
        else {
            element = document.createElement("timeaxis");
            String minimumStr = getDataMinimum().toString();
            element.setAttribute("timeMinimum", minimumStr);
            String maximumStr = getDataMaximum().toString();
            element.setAttribute("timeMaximum", maximumStr);
        }

        element.setAttribute("name", getDasName());
        element.setAttribute("row", getRow().getDasName());
        element.setAttribute("column", getColumn().getDasName());

        element.setAttribute("label", getLabel());
        element.setAttribute("log", Boolean.toString(areTickLabelsVisible()));
        element.setAttribute("oppositeAxisVisible", Boolean.toString(isOppositeAxisVisible()));
        element.setAttribute("animated", Boolean.toString(isAnimated()));
        element.setAttribute("dataPath", getDataPath());
        element.setAttribute("showTca", Boolean.toString(getDrawTca()));
        element.setAttribute("orientation", orientationToString(getOrientation()));
        
        return element;
    }

    public static DasTimeAxis createNamedTimeAxis(String name) {
        DasTimeAxis axis = new DasTimeAxis((TimeDatum)TimeUtil.create("2000-1-1"), (TimeDatum)TimeUtil.create("2000-1-2"), null, null, DasAxis.HORIZONTAL);
        if (name == null) {
            name = "timeaxis_" + Integer.toHexString(System.identityHashCode(axis));
        }
        try {
            axis.setDasName(name);
        }
        catch (edu.uiowa.physics.pw.das.DasNameException dne) {
            edu.uiowa.physics.pw.das.util.DasExceptionHandler.handle(dne);
        }
        return axis;
    }
    
    protected class TimeAxisLayoutManager extends AxisLayoutManager {
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);
            if (drawTca && getOrientation() == BOTTOM && tcaData != null) {
                Rectangle bounds = primaryInputPanel.getBounds();
                int tcaHeight = (getTickLabelFont().getSize() + getLineSpacing())*tcaData.items;
                bounds.height += tcaHeight;
                primaryInputPanel.setBounds(bounds);
            }
        }
    }
}
