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

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.Editable;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateEvent;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;
import java.beans.PropertyChangeEvent;

import javax.swing.event.EventListenerList;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 *
 * @author  jbf
 */
public abstract class DasDevicePosition implements Editable, java.io.Serializable {
    
    protected transient DasCanvas canvas;
    protected transient DasDevicePosition parent;
    
    private double minimum;
    private double maximum;
    
    private boolean isWidth;
    
    private String dasName;
    private transient PropertyChangeSupport propertyChangeDelegate;
    
    protected EventListenerList listenerList = new EventListenerList();
    
    private PropertyChangeListener canvasListener= new PropertyChangeListener() {
        public void propertyChange( PropertyChangeEvent ev ) {
            if ( DasDevicePosition.this.emMinimum!=0 || DasDevicePosition.this.emMaximum!=0 ) {
                revalidate();
            }
        }
    };
    
    protected DasDevicePosition( DasCanvas canvas, boolean isWidth, DasDevicePosition parent,
            double minimum, double maximum,
            double emMinimum, double emMaximum,
            int ptMinimum, int ptMaximum ) {
        if ( minimum > maximum ) {
            throw new IllegalArgumentException( "minimum>maximum" );
        }
        
        boolean isNull= ( canvas==null ) || ( parent==null );
        
        if ( parent!=null ) {
            canvas= parent.getCanvas();
            isWidth= parent.isWidth;
        }
        
        if ( canvas==null & ( ! isNull ) ) {
            throw new IllegalArgumentException("parent cannot be null");
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
        this.propertyChangeDelegate = new PropertyChangeSupport(this);
        if ( parent!=null ) {
            parent.addPropertyChangeListener( new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    revalidate();
                }
            } );
        } else {
            if (canvas != null) {
                canvas.addComponentListener(new ComponentAdapter() {
                    public void componentResized(ComponentEvent e) {
                        revalidate();
                    }
                });
                canvas.addPropertyChangeListener( "font", canvasListener );
                canvas.addDevicePosition(this);
            }
        }
        
    }
    
    public DasDevicePosition(DasCanvas parent, double minimum, double maximum, boolean width) {
        this( parent, width, null, minimum, maximum, 0., 0., 0, 0 );
    }
    
    protected DasCanvas getCanvas() {
        return this.canvas;
    }
    
    public void setDasName(String name) throws edu.uiowa.physics.pw.das.DasNameException {
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
    
    public String getDasName() {
        return dasName;
    }
    
    private int getEmSize() {
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
    
    private int dMinimum, dMaximum;
    
    /**
     * recalculates dMinimum and dMaximum becased on the new values, and checks for
     * correctness.  Note if dMaximum<=dMinimum, we define dMaximum= dMinimum+1.
     * This was originally introduced with the thought that this would only be
     * called when the properties
     */
    protected void revalidate() {
        int oldmin= dMinimum;
        int oldmax= dMaximum;
        dMinimum= (int)( getParentMin() + minimum*getDeviceSize() + getEmSize() * emMinimum + ptMinimum );
        dMaximum= (int)( getParentMin() + maximum*getDeviceSize() + getEmSize() * emMaximum + ptMaximum );
        if ( dMaximum<=dMinimum ) dMaximum= dMinimum+1;
        if ( dMinimum!=oldmin ) firePropertyChange( "dMinimum", oldmin, dMinimum );
        if ( dMaximum!=oldmax ) firePropertyChange( "dMaximum", oldmax, dMaximum );
        if ( dMinimum!=oldmin || dMaximum!=oldmax ) fireUpdate();
    }
    
    /**
     * returns the pixel position of the minimum of the Row/Column.  This is
     * the left side of a column and the top of a row.
     */
    public int getDMinimum() {
        if ( canvas==null && parent==null ) {
            String type= isWidth ? "column" : "row";
            throw new RuntimeException("null "+type+", "+type+" was not set before layout");
        }
        return dMinimum;
    }
    
    /**
     * returns the pixel position of the maximum of the Row/Column.  This is
     * the right side of a column and the bottom of a row.
     */
    public int getDMaximum() {
        if ( canvas==null && parent==null ) {
            String type= isWidth ? "column" : "row";
            throw new RuntimeException("null "+type+", "+type+" was not set before layout");
        }
        return dMaximum;
    }
    
    public double getMinimum() {
        return minimum;
    }
    
    public double getMaximum() {
        return maximum;
    }
    
    public void setPosition(double minimum, double maximum) {
        double oldMin = this.minimum;
        double oldMax = this.maximum;
        double doldMin = this.minimum;
        double doldMax = this.maximum;
        this.minimum = Math.min(minimum, maximum);
        this.maximum = Math.max(maximum, maximum);
        revalidate();
        if (oldMin != this.minimum) {
            firePropertyChange("minimum", oldMin, this.minimum);
        }
        if (oldMax != this.maximum) {
            firePropertyChange("maximum", oldMax, this.maximum);
        }
    }
    
    public void setDPosition( int minimum, int maximum) {
        int pmin= getParentMin();
        int pmax= getParentMax();
        int em= getEmSize();
        int length= pmax - pmin;
        double nmin= ( minimum -  emMinimum * em - ptMinimum ) / length;
        double nmax= ( maximum -  emMaximum * em - ptMaximum ) / length;
        setPosition( nmin, nmax );
    }
    
    public void setMaximum(double maximum) {
        if (maximum == this.maximum) {
            return;
        }
        if (maximum < this.minimum) {
            setPosition(maximum, this.minimum);
        } else {
            double oldValue = this.maximum;
            this.maximum = maximum;
            firePropertyChange("maximum", oldValue, maximum);
            revalidate();
        }
    }
    
    public void setDMaximum( int maximum) {
        int pmin= getParentMin();
        int pmax= getParentMax();
        int em= getEmSize();
        int length= pmax - pmin;
        double n= ( maximum - emMaximum * em - ptMaximum ) / length;
        setMaximum( n );
    }
    
    public void setMinimum( double minimum) {
        if (minimum == this.minimum) {
            return;
        }
        if (minimum > this.maximum) {
            setPosition(this.maximum, minimum);
        } else {
            double oldValue = this.minimum;
            this.minimum = minimum;
            firePropertyChange("minimum", oldValue, minimum);
            revalidate();
        }
    }
    
    public void setDMinimum( int minimum) {
        int pmin= getParentMin();
        int pmax= getParentMax();
        int em= getEmSize();
        int length= pmax - pmin;
        double n= ( minimum - emMinimum * em - ptMinimum ) / length;
        setMinimum( n );
    }
    
    public DasCanvas getParent() {
        return this.canvas;
    }
    
    public void setParent(DasCanvas parent) {
        this.canvas= parent;
        fireUpdate();
    }
    
    public void addpwUpdateListener(DasUpdateListener l) {
        listenerList.add(DasUpdateListener.class, l);
    }
    
    public void removepwUpdateListener(DasUpdateListener l) {
        listenerList.remove(DasUpdateListener.class, l);
    }
    
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
        propertyChangeDelegate.addPropertyChangeListener(propertyName, listener);
    }
    
    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        firePropertyChange(propertyName,
                (oldValue ? Boolean.TRUE : Boolean.FALSE),
                (newValue ? Boolean.TRUE : Boolean.FALSE));
    }
    
    protected void firePropertyChange(String propertyName, int oldValue, int newValue) {
        firePropertyChange(propertyName, new Integer(oldValue), new Integer(newValue));
    }
    
    protected void firePropertyChange(String propertyName, long oldValue, long newValue) {
        firePropertyChange(propertyName, new Long(oldValue), new Long(newValue));
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
    
    protected int getDeviceSize() {
        return getParentMax() - getParentMin();
    }
    
    public static java.awt.Rectangle toRectangle(DasRow row, DasColumn column) {
        int xmin=column.getDMinimum();
        int ymin=row.getDMinimum();
        return new java.awt.Rectangle(xmin,ymin,
                column.getDMaximum()-xmin,
                row.getDMaximum()-ymin);
    }
    
    public String toString() {
        return getClass().getName() + "[minimum=" + getDMinimum() + " maximum=" + getDMaximum() + "]";
    }
    
    public boolean contains( int x ) {
        return ( getDMinimum() <= x ) && ( x <= getDMaximum() );
    }
    
    public int getDMiddle() {
        return (getDMinimum()+getDMaximum())/2;
    }
    
    /**
     * property emMinimum, the em (font height * 2/3) offset from the minimum
     */
    private double emMinimum;
    
    public double getEmMinimum() {
        return this.emMinimum;
    }
    
    public void setEmMinimum(double emMinimum) {
        double oldValue= this.emMinimum;
        this.emMinimum = emMinimum;
        firePropertyChange("emMinimum", oldValue, emMinimum);
        revalidate();
    }
    
    /**
     * property emMaximum, the em (font height * 2/3) offset from the maximum
     */
    private double emMaximum;
    
    public double getEmMaximum() {
        return this.emMaximum;
    }
    
    public void setEmMaximum(double emMaximum) {
        double oldValue= this.emMaximum;
        this.emMaximum = emMaximum;
        firePropertyChange("emMaximum", oldValue, emMaximum);
        revalidate();
    }
    
    /**
     * property ptMinimum, the pixel offset from the minimum
     */
    private int ptMinimum;
    
    public int getPtMinimum() {
        return this.ptMinimum;
    }
    
    public void setPtMinimum(int ptMinimum) {
        int oldValue= this.ptMinimum;
        this.ptMinimum = ptMinimum;
        firePropertyChange("ptMinimum", oldValue, ptMinimum);
        revalidate();
    }
    
    /**
     * property ptMaximum, the pixel offset from the maximum
     */
    private int ptMaximum=0;
    
    public int getPtMaximum() {
        return this.ptMaximum;
    }
    public void setPtMaximum(int ptMaximum) {
        int oldValue= this.ptMaximum;
        this.ptMaximum = ptMaximum;
        firePropertyChange("ptMaximum", oldValue, ptMaximum);
        revalidate();
    }
    
    public DasDevicePosition getParentDevicePosition() {
        return this.parent;
    }
    
}
