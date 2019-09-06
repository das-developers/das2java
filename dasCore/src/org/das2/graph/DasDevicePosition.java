/* File: DasDevicePosition.java
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

import org.das2.DasApplication;
import org.das2.graph.event.DasUpdateEvent;
import org.das2.graph.event.DasUpdateListener;
import java.beans.PropertyChangeEvent;

import javax.swing.event.EventListenerList;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.ParseException;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.components.propertyeditor.Editable;
import org.das2.system.MutatorLock;
import org.das2.util.DebugPropertyChangeSupport;
import org.das2.util.LoggerManager;

/**
 * DasRows and DasColumns are both DasDevidePositions that lay out the
 * canvas.  Any object on the DasCanvas have a row and column object to indicate
 * the position of the object.
 * @author  jbf
 */
public abstract class DasDevicePosition implements Editable, java.io.Serializable {
    
    private final static Logger logger= LoggerManager.getLogger("das2.graphics");
    
    public static final String PROP_DMAXIMUM = "dMaximum";
    public static final String PROP_DMINIMUM = "dMinimum";
    public static final String PROP_EMMAXIMUM = "emMaximum";
    public static final String PROP_EMMINIMUM = "emMinimum";
    public static final String PROP_MAXIMUM = "maximum";
    public static final String PROP_MINIMUM = "minimum";
    public static final String PROP_PTMAXIMUM = "ptMaximum";
    public static final String PROP_PTMINIMUM = "ptMinimum";
    
    protected transient DasCanvas canvas;
    protected transient DasDevicePosition parent;
    
    private double minimum;
    private double maximum;
    
    private boolean isWidth;
    
    private String dasName;
    private transient PropertyChangeSupport propertyChangeDelegate;
    
    protected EventListenerList listenerList = new EventListenerList();
    
    private final PropertyChangeListener canvasFontListener= new PropertyChangeListener() {
        @Override
        public void propertyChange( PropertyChangeEvent ev ) {
            if ( DasDevicePosition.this.emMinimum!=0 || DasDevicePosition.this.emMaximum!=0 ) {
                revalidate();
            }
        }
    };
    
    private final PropertyChangeListener canvasListener= new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            revalidate();
        }
    };
    
    private final ComponentAdapter componentAdapter= new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            revalidate();
        }
    };
    
    /**
     * create the DasDevicePosition.  Typically the DasRow or DasColumn 
     * constructor is used.
     * @param canvas the canvas to which the position refers.
     * @param isWidth true if the DasDevicePosition is a column not a row.
     * @param parent null or the parent to which this position is relative.
     * @param minimum normal position with respect to the canvas or parent if non-null.
     * @param maximum normal position with respect to the canvas or parent if non-null.
     * @param emMinimum em offset from the minimum position, in canvas font heights.
     * @param emMaximum em offset from the maximum position, in canvas font heights.
     * @param ptMinimum point offset from the minimum position, note points are the same as pixels.
     * @param ptMaximum point offset from the maximum position, note points are the same as pixels.
     */
    protected DasDevicePosition( DasCanvas canvas, boolean isWidth, DasDevicePosition parent,
            double minimum, double maximum,
            double emMinimum, double emMaximum,
            int ptMinimum, int ptMaximum ) {
        if ( minimum > maximum ) {
            throw new IllegalArgumentException( "minimum>maximum" );
        }
        
        // isNull indicates this is the NULL row or column.
        boolean isNull= ( canvas==null ) && ( parent==null );
        
        if ( parent!=null ) {
            canvas= parent.getCanvas();
            isWidth= parent.isWidth;
        }
        
        if ( canvas==null && ( ! isNull ) ) {  
            throw new IllegalArgumentException("parent cannot be null");
        }
        
        if ( isNull ) {
            logger.finest( "null canvas and null parent is allowed if you know what you are doing.");
        }
        
        this.canvas = canvas;
        this.parent= parent;
        this.minimum = minimum;
        this.maximum = maximum;
        this.emMinimum= emMinimum;
        this.emMaximum= emMaximum;
        this.ptMinimum= ptMinimum;
        this.ptMaximum= ptMaximum;
        this.isWidth = isWidth;
        
        this.dasName = DasApplication.getDefaultApplication().suggestNameFor(this);

        logger.log(Level.FINER, "ADD  {0} {1} {2}", new Object[]{dasName, canvas, parent});
        
        this.propertyChangeDelegate = new DebugPropertyChangeSupport(this);
        if ( parent!=null ) {
            parent.addPropertyChangeListener( canvasListener );
        } else {
            if (canvas != null) {
                canvas.addComponentListener( componentAdapter );
                canvas.addPropertyChangeListener( "font", canvasFontListener );
                canvas.addDevicePosition(this); //TODO: it's interesting that this only happens for the parent devicePosition, not the kids.
            }
        }
        if ( !isNull ) {
            revalidate();
        }
    }
    
    /**
     * remove the listeners so that the DasRow or DasColumn can be garbage collected.
     */
    public void removeListeners() {
        logger.log(Level.FINER, "RM   {0}", dasName);
        if ( parent!=null ) {
            parent.removePropertyChangeListener( canvasListener );
        } else {
            if ( canvas!=null ) {
                canvas.removeComponentListener( componentAdapter );
                canvas.removePropertyChangeListener( "font", canvasFontListener );
            }
        }
    }
    
    /**
     * parse the format string into a pixel count.  Convenience method.
     * parseFormatStr(s) will throw a parse exception and should be used to 
     * verify strings.
     * @param s The string, like "5em+3pt"
     * @param em the em height of the font,
     * @param widthHeight the width or height of the dimension.
     * @param fail the value to return if the parsing fails.
     * @return the length in pixels (or points).
     */
    public static double parseLayoutStr( String s, double em, int widthHeight, double fail ) {
        try {
            double [] r= parseLayoutStr(s);
            return widthHeight * r[0] + em * r[1] + r[2];
        } catch ( ParseException ex ) {
            return fail;
        }
    }
    
    /**
     * Calls parseLayoutStr
     * @param s
     * @return
     * @deprecated use parseLayoutStr.
     * @throws ParseException 
     */
    public static double[] parseFormatStr( String s ) throws ParseException {
        return parseLayoutStr(s);
    }
    
    /**
     * parse position strings like "100%-5em+4pt" into [ npos, emoffset, pt_offset ].
     * Note px is acceptable, but pt is proper. 
     * Ems are rounded to the nearest hundredth.
     * Percents are returned as normal (0-1) and rounded to the nearest thousandth.
     * @param s string containing the position string.
     * @return three-element array of parsed [ npos, emoffset, pt_offset ]
     * @throws java.text.ParseException
     * @see formatFormatStr
     */
    public static double[] parseLayoutStr( String s ) throws ParseException {
        double[] result= new double[] { 0, 0, 0 };
        StringTokenizer tok= new StringTokenizer( s, "%emptx", true );
        int pos=0;
        while ( tok.hasMoreTokens() ) {
            String ds= tok.nextToken();
            pos+=ds.length();
            double d= Double.parseDouble(ds);
            String u=null;
            try {
                u = tok.nextToken();
            } catch (NoSuchElementException e) {
                if ( s.trim().equals("0") ) {
                    return new double[] { 0,0,0 };
                } else {
                    throw new ParseException("missing units in format string: "+s,0);
                }
            }
            pos+=u.length();
            if ( u.charAt(0)=='%' ) {
                result[0]= d/100.;
            } else if ( u.equals("e") ) {
                String s2= tok.nextToken();
                if ( !s2.equals("m") ) throw new ParseException( "expected m following e",pos);
                pos+= s2.length();
                result[1]= d;
            } else if ( u.equals("p") ) {
                String s2= tok.nextToken();
                if ( !( s2.equals("t") || s2.equals("x") ) ) throw new ParseException( "expected t following p",pos);
                pos+= s2.length();
                result[2]= d;
            }
        }
        result[0]= Math.round(result[0]*1000)/1000.;
        result[1]= Math.round(result[1]*10)/10.;
        return result;
    }

    /**
     * formats the three position specifiers efficiently.
     * @param arr three-element array [ npos, emoffset, pt_offset ].
     * @return String like "100%-5em+4pt"
     * @see #parseFormatStr(java.lang.String) 
     * @see #formatLayoutStr(org.das2.graph.DasDevicePosition, boolean) which contains repeated code.
     */
    public static String formatFormatStr( double[] arr ) {
        StringBuilder buf= new StringBuilder();
        if ( arr[0]!=0 ) buf.append( String.format( Locale.US, "%.2f%%", arr[0]*100 ) );
        if ( arr[1]!=0 ) buf.append( String.format(Locale.US, "%+.1fem", arr[1] ) );
        if ( arr[2]!=0 ) buf.append( String.format(Locale.US, "%+dpt", (int)arr[2] ) );
        return buf.toString();
    }

    /**
     * parses the layout string, which contains both the minimum and maximum
     * positions, and configures the row or column.  Note that for rows,
     * 0% refers to the top of the canvas or parent row.
     * @param pos row or column to assign values.
     * @param spec string like "0%+2em,100%-5em+4pt"
     * @throws ParseException when the string cannot be parsed.
     */
    public static void parseLayoutStr( DasDevicePosition pos, String spec ) throws ParseException {
        String[] ss= spec.split(",");
        double[] pmin= parseLayoutStr( ss[0] );
        double[] pmax= parseLayoutStr( ss[1] );
        
        MutatorLock lock= pos.mutatorLock();
        lock.lock();
        //try {
            pos.setMinimum(pmin[0]);
            pos.setEmMinimum(pmin[1]);
            pos.setPtMinimum((int)pmin[2]);
            pos.setMaximum(pmax[0]);
            pos.setEmMaximum(pmax[1]);
            pos.setPtMaximum((int)pmax[2]);        
        //} finally {
            lock.unlock();
        //}
    }
    
    /**
     * formats the row or column position into a string like
     * @param pos the row or column
     * @param min true if the minimum boundary is to be formatted, false if the maximum boundary is to be formatted.
     * @return String like "100%-5em+4pt"
     * @see #formatFormatStr(double[]) which contains repeated code.
     */
    public static String formatLayoutStr( DasDevicePosition pos, boolean min ) {
        StringBuilder buf= new StringBuilder();
        if ( min ) {
            if ( pos.getMinimum()!=0 ) buf.append( String.format( Locale.US, "%.2f%%", pos.getMinimum()*100 ) );
            if ( pos.getEmMinimum()!=0 ) buf.append( String.format(Locale.US, "%+.1fem", pos.getEmMinimum() ) );
            if ( pos.getPtMinimum()!=0 ) buf.append( String.format(Locale.US, "%+dpt", pos.getPtMinimum() ) );
        } else {
            if ( pos.getMaximum()!=0 ) buf.append( String.format(Locale.US, "%.2f%%", pos.getMaximum()*100 ) );
            if ( pos.getEmMaximum()!=0 ) buf.append( String.format(Locale.US, "%+.1fem", pos.getEmMaximum() ) );
            if ( pos.getPtMaximum()!=0 ) buf.append( String.format(Locale.US, "%+dpt", pos.getPtMaximum() ) );
        }
        if ( buf.length()==0 ) return "0%";
        return buf.toString();
    }

    
    public DasDevicePosition(DasCanvas parent, double minimum, double maximum, boolean width) {
        this( parent, width, null, minimum, maximum, 0., 0., 0, 0 );
    }
    
    protected DasCanvas getCanvas() {
        return this.canvas;
    }
    
    /**
     * set the name associated with this object.
     * @param name the name associated with this object
     * @throws org.das2.DasNameException 
     */
    public void setDasName(String name) throws org.das2.DasNameException {
        if (name.equals(dasName)) {
            return;
        }
        String oldName = dasName;
        dasName = name;
        DasApplication app = canvas.getDasApplication();
        if (app != null) {
            app.getNameContext().put(name, this);
            if (oldName != null) {
                app.getNameContext().remove(oldName);
            }
        }
        this.firePropertyChange("name", oldName, name);
    }
    
    /**
     * get the name associated with this object.
     * @return the name associated with this object.
     */
    public String getDasName() {
        return dasName;
    }
    
    /**
     * returns the em size for the canvas.  We define the em size as the 
     * height of the font.
     * @return the em height in points.
     */
    public int getEmSize() {
        return canvas.getFont().getSize();
    }
    
    private int getParentMin() {
        if ( parent==null ) {
            return 0;
        } else {
            return parent.getDMinimum();
        }
    }
    
    private int getParentMax() {
        if ( parent==null ) {
            return isWidth ? canvas.getWidth() : canvas.getHeight();
        } else {
            return parent.getDMaximum();
        }
    }
    
    /**
     * dMinimum and dMaximum are the position in pixel space.
     */
    private int dMinimum, dMaximum;
    
    /**
     * recalculates dMinimum and dMaximum based on the new values, and 
     * checks for correctness.  Note if dMaximum&lt;=dMinimum, we define 
     * dMaximum= dMinimum+1.
     */
    protected final void revalidate() {
        if ( parent!=null ) {
            parent.revalidate();
        }
        int oldmin= dMinimum;
        int oldmax= dMaximum;
        dMinimum= (int)( getParentMin() + minimum*getDeviceSize() + getEmSize() * emMinimum + ptMinimum );
        dMaximum= (int)( getParentMin() + maximum*getDeviceSize() + getEmSize() * emMaximum + ptMaximum );
        if ( dMaximum<=dMinimum ) dMaximum= dMinimum+1;
        if ( dMinimum!=oldmin ) firePropertyChange(  PROP_DMINIMUM, oldmin ,dMinimum);
        if ( dMaximum!=oldmax ) firePropertyChange(  PROP_DMAXIMUM, oldmax ,dMaximum);
        if ( dMinimum!=oldmin || dMaximum!=oldmax ) fireUpdate();
        canvas.repaint();
    }
    
    /**
     * returns the pixel position of the minimum of the Row/Column.  This is
     * the left side of a column and the top of a row.
     * @return the pixel position (pixel=point for now) 
     */
    public int getDMinimum() {
        if ( canvas==null && parent==null ) {
            String type= isWidth ? "column" : "row";
            throw new RuntimeException( type+" was not set before layout, having no canvas or parent "+type );
        }
        return dMinimum;
    }
    
    /**
     * returns the pixel position of the maximum of the Row/Column.  This is
     * the right side of a column and the bottom of a row.
     * @return the pixel position (pixel=point for now) 
     */
    public int getDMaximum() {
        if ( canvas==null && parent==null ) {
            String type= isWidth ? "column" : "row";
            throw new RuntimeException( type+" was not set before layout, having no canvas or parent "+type);
        }
        return dMaximum;
    }
    
    /**
     * return the normal position control of the top/left.
     * @return the normal position control of the top/left.
     */
    public double getMinimum() {
        return minimum;
    }

    /**
     * return the normal position control of the bottom/right.
     * @return the normal position control of the bottom/right.
     */
    public double getMaximum() {
        return maximum;
    }

    /**
     * set the new normal location of both the min and max in one operation.
     * @param minimum the top or left
     * @param maximum the bottom or right
     */
    private void setPosition(double minimum, double maximum) {
        double oldMin = this.minimum;
        double oldMax = this.maximum;
        this.minimum = Math.min(minimum, maximum);
        this.maximum = Math.max(minimum, maximum);
        revalidate();
        if (oldMin != this.minimum) {
            firePropertyChange( PROP_MINIMUM, oldMin, this.minimum);
        }
        if (oldMax != this.maximum) {
            firePropertyChange( PROP_MAXIMUM, oldMax, this.maximum);
        }
    }

    /**
     * set the new pixel location of both the min and max in one operation.
     * @param minimum the top or left
     * @param maximum the bottom or right
     */
    public void setDPosition( int minimum, int maximum) {
        int pmin= getParentMin();
        int pmax= getParentMax();
        int em= getEmSize();
        int length= pmax - pmin;
        double nmin= ( minimum -  emMinimum * em - ptMinimum - pmin ) / length;
        double nmax= ( maximum -  emMaximum * em - ptMaximum - pmin ) / length;
        setPosition( nmin, nmax );
    }
    
    /**
     * set the normal position of the minimum of the row or column.  
     * For a row, this is the bottom.  For a column, this is the right side.
     * @param maximum normal (0-1) position
     */
    public void setMaximum(double maximum) {
        if (maximum == this.maximum) {
            return;
        }
        if (maximum < this.minimum) {
            setPosition(maximum, this.minimum);
        } else {
            double oldValue = this.maximum;
            this.maximum = maximum;
            firePropertyChange( PROP_MAXIMUM, oldValue, maximum);
            revalidate();
        }
    }


    /**
     * set the new pixel position of the bottom/right boundary.  
     * em and pt offsets are not modified, and the normal position 
     * is recalculated.
     * @param maximum new pixel maximum
     */
    public void setDMaximum( int maximum) {
        int pmin= getParentMin();
        int pmax= getParentMax();
        int em= getEmSize();
        int length= pmax - pmin;
        double n= ( maximum - emMaximum * em - ptMaximum ) / length;
        setMaximum( n );
    }
    
    /**
     * set the normal position of the minimum of the row or column.  
     * For a row, this is the top.  For a column, this is the left side.
     * @param minimum normal (0-1) position
     */
    public void setMinimum( double minimum) {
        if (minimum == this.minimum) {
            return;
        }
        if (minimum > this.maximum) {
            setPosition(this.maximum, minimum);
        } else {
            double oldValue = this.minimum;
            this.minimum = minimum;
            firePropertyChange( PROP_MINIMUM, oldValue, minimum);
            revalidate();
        }
    }
    
    /**
     * set the new pixel position of the top/left boundary.  em and pt offsets
     * are not modified, and the normal position is recalculated.
     * @param minimum new pixel minimum
     */    
    public void setDMinimum( int minimum) {
        int pmin= getParentMin();
        int pmax= getParentMax();
        int em= getEmSize();
        int length= pmax - pmin;
        double n= ( minimum - emMinimum * em - ptMinimum ) / length;
        setMinimum( n );
    }
    
    /**
     * return the parent canvas.
     * @return the parent canvas.
     */
    public DasCanvas getParent() {
        return this.canvas;
    }
    
    /**
     * set the parent canvas.
     * @param parent canvas. 
     */
    public void setParent(DasCanvas parent) {
        this.canvas= parent;
        fireUpdate();
    }
    
    private boolean valueIsAdjusting= false;
    
    /**
     * get a lock for this object, used to mutate a number of properties
     * as one atomic operation.
     * @return a lock
     */
    protected synchronized MutatorLock mutatorLock() {
        return new MutatorLock() {
            @Override
            public void lock() {
                if ( isValueIsAdjusting() ) {
                    System.err.println("lock is already set!");
                }
                valueIsAdjusting= true;
            }
            @Override
            public void unlock() {
                valueIsAdjusting= false;
                propertyChangeDelegate.firePropertyChange( "mutatorLock", "locked", "unlocked");
            }
        };
    }
    
    /**
     * add an update listener
     * @param l update listener
     */
    public void addUpdateListener(DasUpdateListener l) {
        listenerList.add(DasUpdateListener.class, l);
        if ( listenerList.getListenerCount()>100 ) {
            logger.log(Level.WARNING, "I think I found a leak in {0}", this.getDasName());
        }
    }
    
    /**
     * remove an update listener
     * @param l update listener
     */
    public void removeUpdateListener(DasUpdateListener l) {
        int n0= listenerList.getListenerCount();
        listenerList.remove(DasUpdateListener.class, l);
        if ( n0>0 && listenerList.getListenerCount()==n0 ) {
            logger.fine("nothing was removed...");
        }
    }
    
    /**
     * fire an update to all listeners.
     */
    protected void fireUpdate() {
        DasUpdateEvent e = new DasUpdateEvent(this);
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==DasUpdateListener.class) {
                ((DasUpdateListener)listeners[i+1]).update(e);
            }
        }
    }
    
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeDelegate.addPropertyChangeListener(listener);
    }
    
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeDelegate.addPropertyChangeListener(propertyName, listener);
    }
    
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeDelegate.removePropertyChangeListener(propertyName, listener);
    }
    
    public void removePropertyChangeListener( PropertyChangeListener listener) {
        propertyChangeDelegate.removePropertyChangeListener(listener);
    }

    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        firePropertyChange(propertyName,
                (oldValue ? Boolean.TRUE : Boolean.FALSE),
                (newValue ? Boolean.TRUE : Boolean.FALSE));
    }
    
    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        firePropertyChange(propertyName, Integer.valueOf(oldValue), Integer.valueOf(newValue) );
    }
    
    protected void firePropertyChange(String propertyName, long oldValue, long newValue) {
        firePropertyChange(propertyName, Long.valueOf(oldValue), Long.valueOf(newValue) );
    }
    
    protected void firePropertyChange(String propertyName, float oldValue, float newValue) {
        firePropertyChange(propertyName, new Float(oldValue), new Float(newValue));
    }
    
    protected void firePropertyChange(String propertyName, double oldValue, double newValue) {
        firePropertyChange(propertyName, new Double(oldValue), new Double(newValue));
    }
    
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeDelegate.firePropertyChange(propertyName, oldValue, newValue);
    }
    
    /**
     * return the size in pixels (or points)
     * @return the size in pixels (or points)
     */
    protected int getDeviceSize() {
        return getParentMax() - getParentMin();
    }
    
    /**
     * convenience method for creating a rectangle from a row and column.
     * The rectangle will be in canvas pixel coordinates.
     * @param row row describing the top and bottom of the box.
     * @param column column describing the left and right sides of the box.
     * @return rectangle in canvas pixel coordinates.
     */
    public static java.awt.Rectangle toRectangle(DasRow row, DasColumn column) {
        int xmin=column.getDMinimum();
        int ymin=row.getDMinimum();
        return new java.awt.Rectangle(xmin,ymin,
                column.getDMaximum()-xmin,
                row.getDMaximum()-ymin);
    }
    
    /**
     * return a human-readable string representing the object for debugging.
     * @return a human-readable string representing the object for debugging.
     */
    @Override
    public String toString() {
        //String format="%.1f%%%+.1fem%+dpt";
        //String smin= String.format(format, minimum*100, emMinimum, ptMinimum );
        //String smax= String.format(format, maximum*100, emMaximum, ptMaximum );
        String t= getClass().getSimpleName();
        if ( canvas==null && parent==null ) {
            return t + " " + getDasName() + " unattached";
        } else {
            return t + " " + getDasName() + " " +  formatLayoutStr(this, true) + "," +formatLayoutStr(this, false)  + " [dpos=" + getDMinimum() + "," + getDMaximum() + "]";
        }
    }
    
    /**
     * returns true if ( getDMinimum() &lt;= x ) &amp;&amp; ( x &lt;= getDMaximum() );
     * @param x the pixel position
     * @return  true if ( getDMinimum() &lt;= x ) &amp;&amp; ( x &lt;= getDMaximum() );
     */
    public boolean contains( int x ) {
        return ( getDMinimum() <= x ) && ( x <= getDMaximum() );
    }
    
    /**
     * returns pixel position (device position) of the the middle of the row or column 
     * @return pixel position (device position) of the the middle of the row or column 
     */
    public int getDMiddle() {
        return (getDMinimum()+getDMaximum())/2;
    }
    
    /**
     * property emMinimum, the em (font height * 2/3) offset from the minimum
     */
    private double emMinimum;
    
    /**
     * return the em offset that controls the position of the top/left boundary.
     * @return the em offset that controls the position of the top/left boundary.
     */
    public double getEmMinimum() {
        return this.emMinimum;
    }
    
    /**
     * set the em offset that controls the position of the top/left boundary.
     * @param emMinimum the em offset.
     */
    public void setEmMinimum(double emMinimum) {
        double oldValue= this.emMinimum;
        this.emMinimum = emMinimum;
        firePropertyChange( PROP_EMMINIMUM, oldValue, emMinimum);
        revalidate();
    }
    
    /**
     * property emMaximum, the em (font height) offset from the maximum
     */
    private double emMaximum;
    
    /**
     * return the em offset that controls the position of the bottom/right boundary.
     * @return the em offset that controls the position of the bottom/right boundary.
     */
    public double getEmMaximum() {
        return this.emMaximum;
    }
    
    /**
     * set the em offset that controls the position of the bottom/right boundary.
     * @param emMaximum the em offset.
     */
    public void setEmMaximum(double emMaximum) {
        double oldValue= this.emMaximum;
        this.emMaximum = emMaximum;
        firePropertyChange( PROP_EMMAXIMUM, oldValue, emMaximum);
        revalidate();
    }
    
    private int ptMinimum;
    
    /**
     * return the points offset that controls the position of the top/left boundary.
     * @return the points offset
     */
    public int getPtMinimum() {
        return this.ptMinimum;
    }
    
    /**
     * set the points offset that controls the position of the top/left boundary.
     * @param ptMinimum the points offset
     */
    public void setPtMinimum(int ptMinimum) {
        int oldValue= this.ptMinimum;
        this.ptMinimum = ptMinimum;
        firePropertyChange( PROP_PTMINIMUM, oldValue, ptMinimum);
        revalidate();
    }
    
    /**
     * property ptMaximum, the pixel offset from the maximum
     */
    private int ptMaximum=0;
    
    /**
     * return the points offset that controls the position of the bottom/right boundary.
     * @return the points offset that controls the position of the bottom/right boundary.
     */
    public int getPtMaximum() {
        return this.ptMaximum;
    }
    
    /**
     * set the pt offset that controls the position of the bottom/right boundary.
     * @param ptMaximum the em offset.
     */
    public void setPtMaximum(int ptMaximum) {
        int oldValue= this.ptMaximum;
        this.ptMaximum = ptMaximum;
        firePropertyChange( PROP_PTMAXIMUM, oldValue, ptMaximum);
        revalidate();
    }

    /**
     * set all three as one atomic operation
     * @param norm normal position from 0 to 1.
     * @param em em offset from the normal position.
     * @param pt points offset from the normal position.
     */
    public void setMin( double norm, double em, int pt ) {
        double[] old= new double[ ] { this.minimum, this.emMinimum, this.ptMinimum };
        this.minimum= norm;
        this.emMinimum= em;
        this.ptMinimum= pt;
        firePropertyChange(PROP_PTMINIMUM, old[2], pt );
        firePropertyChange(PROP_EMMINIMUM, old[1], em );
        firePropertyChange(PROP_MINIMUM, old[0], norm );
        revalidate();
    }

    /**
     * set all three as one atomic operation
     * @param norm normal position from 0 to 1.
     * @param em em offset from the normal position.
     * @param pt points offset from the normal position.
     */
    public void setMax( double norm, double em, int pt ) {
        double[] old= new double[ ] { this.maximum, this.emMaximum, this.ptMaximum };
        this.maximum= norm;
        this.emMaximum= em;
        this.ptMaximum= pt;
        firePropertyChange(PROP_PTMAXIMUM, old[2], pt );
        firePropertyChange(PROP_EMMAXIMUM, old[1], em );
        firePropertyChange(PROP_MAXIMUM, old[0], norm );
        revalidate();
    }
    
    /**
     * return the parent, or null.  If parent is non-null, then position is 
     * relative to the parent.
     * @return the parent, or null.
     */
    public DasDevicePosition getParentDevicePosition() {
        return this.parent;
    }

    /**
     * return true if the value is currently adjusting because a
     * mutator lock is out.
     * @return true if the value is currently adjusting.
     */
    public boolean isValueIsAdjusting() {
        return valueIsAdjusting;
    }
    
}
