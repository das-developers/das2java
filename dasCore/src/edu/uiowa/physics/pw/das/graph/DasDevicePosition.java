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
    
    protected transient DasCanvas parent;
    private double minimum;
    private double maximum;
    private String dasName;
    private transient PropertyChangeSupport propertyChangeDelegate;
    
    protected EventListenerList listenerList = new EventListenerList();
    
    public DasDevicePosition(DasCanvas parent, double minimum, double maximum) {
        if ( minimum > maximum ) { 
            throw new IllegalArgumentException( "minimum>maximum" );
        }
        this.parent = parent;
        this.minimum = minimum;
        this.maximum = maximum;
        this.dasName = "dp_" + Integer.toString(this.hashCode());
        this.propertyChangeDelegate = new PropertyChangeSupport(this);
        if (parent != null) {
            parent.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent e) {
                    firePropertyChange("dMinimum", 0.0, getDMinimum());
                    firePropertyChange("dMaximum", 0.0, getDMaximum());
                    fireUpdate();
                }
            });
            parent.addDevicePosition(this);
        }
    }
    
    public void setDasName(String name) throws edu.uiowa.physics.pw.das.DasNameException {
        if (name.equals(dasName)) {
            return;
        }
        String oldName = dasName;
        dasName = name;
        DasApplication app = parent.getDasApplication();
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
    
    public int getDMinimum() {
        return (int)(minimum*getDeviceSize());
    }
    
    public int getDMaximum() {
        return (int)(maximum*getDeviceSize());
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
        this.minimum = Math.min(minimum, maximum);
        this.maximum = Math.max(maximum, maximum);
        if (oldMin != this.minimum) {
            firePropertyChange("minimum", oldMin, this.minimum);
            firePropertyChange("dMinimum", oldMin * getDeviceSize(), getDMinimum());
        }
        if (oldMax != this.maximum) {
            firePropertyChange("maximum", oldMax, this.maximum);
            firePropertyChange("dMaximum", oldMax * getDeviceSize(), getDMaximum());
        }
        fireUpdate();
    }
    
    public void setDPosition( int minimum, int maximum) {
        setPosition((double)minimum / getDeviceSize(), (double)maximum / getDeviceSize());
    }
    
    public void setMaximum(double maximum) {
        if (maximum == this.maximum) {
            return;
        }
        if (maximum < this.minimum) {
            setPosition(maximum, this.minimum);
        }
        else {
            double oldValue = this.maximum;
            this.maximum = maximum;
            firePropertyChange("maximum", oldValue, maximum);
            firePropertyChange("dMaximum", oldValue * getDeviceSize(), getDMaximum());
            fireUpdate();
        }
    }
    
    public void setDMaximum( int maximum) {
        setMaximum( (double)maximum / getDeviceSize());
    }
    
    public void setMinimum( double minimum) {
        if (minimum == this.minimum) {
            return;
        }
        if (minimum > this.maximum) {
            setPosition(this.maximum, minimum);
        }
        else {
            double oldValue = this.minimum;
            this.minimum = minimum;
            firePropertyChange("minimum", oldValue, minimum);
            firePropertyChange("dMinimum", oldValue * getDeviceSize(), getDMinimum());
            fireUpdate();
        }
    }
    
    public void setDMinimum( int minimum) {
        setMinimum( (double)minimum / getDeviceSize());
    }
    
    public void translate(double nDelta) {
        setPosition(this.minimum + nDelta, this.maximum + nDelta);
    }
    
    public void dTranslate( int delta ) {
        setDPosition( getDMinimum() + delta, getDMaximum() + delta);
    }
    
    public DasCanvas getParent() {
        return this.parent;
    }
    
    public void setParent(DasCanvas parent) {
        this.parent= parent;
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
    
    protected abstract int getDeviceSize();
    
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
    
}
