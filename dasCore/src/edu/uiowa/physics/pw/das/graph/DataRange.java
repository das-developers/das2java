/* File: DataRange.java
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

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateEvent;

import javax.swing.event.EventListenerList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Stack;

public class DataRange implements Cloneable {
    
    private DasAxis parent;
    
    private edu.uiowa.physics.pw.das.datum.Units units;
    
    private double minimum;
    
    private double maximum;
    
    private boolean log;
    
    private EventListenerList listenerList = new EventListenerList();
    
    private Stack history;
    
    private Stack forwardHistory;
    
    private PropertyChangeSupport propertyChangeDelegate;
    
    public DataRange( DasAxis parent, edu.uiowa.physics.pw.das.datum.Datum min, edu.uiowa.physics.pw.das.datum.Datum max, boolean log ) {
        if (min.gt(max)) throw new IllegalArgumentException("data min on axis is greater than data max");
        if (!min.isValid()) throw new IllegalArgumentException("data_minimum on axis is NaN");
        if (!max.isValid()) throw new IllegalArgumentException("data_maximum on axis is NaN");
        if (min.getUnits()!=max.getUnits())  throw new IllegalArgumentException("units don't match on range");
        this.parent= parent;
        units= min.getUnits();
        minimum = min.getValue();
        maximum = max.getValue();
        this.log = log;
        history = new Stack();
        forwardHistory = new Stack();
        propertyChangeDelegate = new PropertyChangeSupport(this);
    }
    
    public boolean isLog() {
        return log;
    }
    
    public void setLog(boolean log) {
        if (this.log==log) return;
        boolean oldLog = this.log;
        if (log) {
            if (minimum<=0. || maximum <=0.) {
                return;
            }
        }
        this.log=log;
        firePropertyChange("log", oldLog, log);
        fireUpdate();
    }
    
    public DasAxis getCreator() {
        return parent;
    }
    
    public double getMinimum() { return minimum; }
    
    public double getMaximum() { return maximum; }
    
    public edu.uiowa.physics.pw.das.datum.Units getUnits() { return units; }
    
    public void setMinimum(double min) {
        if (min>maximum) {
            if (isLog()) {
                setRange(min, min*10);
            } else {
                setRange(min, min+1.);
            }
        }
        else {
            setRange(min, maximum);
        }
    }
    
    public void setMaximum(double max) {
        if (max<minimum) {
            if (isLog()) {
                setRange(max/10.,max);
            } else {
                setRange(max-1.,max);
            }
        }
        else {
            setRange(minimum, max);
        }
    }
    
    public void setRange(double min, double max) {
        if ( min>max ) {
            edu.uiowa.physics.pw.das.util.DasDie.println("min>max in DasAxis.DataRange.setRange");
            if (isLog()) {
                max= min*10.;
            } else {
                max= min+1.;
            }
        }
        if ( isLog() ) {
            if (min<0.) min=minimum;
            if (max<0.) max=maximum;
        }
        double oldMin = minimum;
        double oldMax = maximum;
        edu.uiowa.physics.pw.das.datum.Datum h[] = new edu.uiowa.physics.pw.das.datum.Datum[2];
        if (minimum!=maximum) {  //  kludge for create() method
            h[0] = new edu.uiowa.physics.pw.das.datum.Datum(minimum,units);
            h[1] = new edu.uiowa.physics.pw.das.datum.Datum(maximum,units);
            history.push(h);
        }
        forwardHistory.removeAllElements();
        minimum = min;
        maximum = max;
        fireUpdate();
        if (minimum != oldMin) firePropertyChange("minimum", oldMin, minimum);
        if (maximum != oldMax) firePropertyChange("maximum", oldMax, maximum);
    }
    
    public void setRangePrev() {
        double oldMin = minimum;
        double oldMax = maximum;
        edu.uiowa.physics.pw.das.util.DasDie.println("history: "+history.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("forwardHistory: "+forwardHistory.size());
        if (!history.isEmpty()) {
            forwardHistory.push( new edu.uiowa.physics.pw.das.datum.Datum [] {new edu.uiowa.physics.pw.das.datum.Datum(minimum,units), new edu.uiowa.physics.pw.das.datum.Datum(maximum,units)} );
            edu.uiowa.physics.pw.das.datum.Datum [] h= (edu.uiowa.physics.pw.das.datum.Datum[]) history.pop();
            
            if (h[0].getUnits()!=units) {
                h[0]= h[0].convertTo(units);
                h[1]= h[1].convertTo(units);
            }
            minimum = h[0].getValue();
            maximum = h[1].getValue();
            
            fireUpdate();
        }
        if (minimum != oldMin) firePropertyChange("minimum", oldMin, minimum);
        if (maximum != oldMax) firePropertyChange("maximum", oldMax, maximum);
        edu.uiowa.physics.pw.das.util.DasDie.println("history: "+history.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("forwardHistory: "+forwardHistory.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("-------------");
    }
    
    public void setRangeForward() {
        double oldMin = minimum;
        double oldMax = maximum;
        edu.uiowa.physics.pw.das.util.DasDie.println("history: "+history.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("forwardHistory: "+forwardHistory.size());
        if (!forwardHistory.isEmpty()) {
            history.push( new edu.uiowa.physics.pw.das.datum.Datum [] {new edu.uiowa.physics.pw.das.datum.Datum(minimum,units), new edu.uiowa.physics.pw.das.datum.Datum(maximum,units)} );
            edu.uiowa.physics.pw.das.datum.Datum [] h= (edu.uiowa.physics.pw.das.datum.Datum[]) forwardHistory.pop();
            
            if (h[0].getUnits()!=units) {
                h[0]= h[0].convertTo(units);
                h[1]= h[1].convertTo(units);
            }
            minimum = h[0].getValue();
            maximum = h[1].getValue();
            
            fireUpdate();
        }
        edu.uiowa.physics.pw.das.util.DasDie.println("history: "+history.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("forwardHistory: "+forwardHistory.size());
        if (minimum != oldMin) firePropertyChange("minimum", oldMin, minimum);
        if (maximum != oldMax) firePropertyChange("maximum", oldMax, maximum);
        edu.uiowa.physics.pw.das.util.DasDie.println("history: "+history.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("forwardHistory: "+forwardHistory.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("-------------");
    }
    
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeDelegate.addPropertyChangeListener(propertyName, listener);
    }
    
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeDelegate.addPropertyChangeListener(propertyName, listener);
    }
    
    protected void firePropertyChange(String propertyName, double oldValue, double newValue) {
        firePropertyChange(propertyName, new Double(oldValue), new Double(newValue));
    }
    
    protected void firePropertyChange(String propertyName, boolean oldValue, boolean newValue) {
        firePropertyChange(propertyName, (oldValue ? Boolean.TRUE : Boolean.FALSE), (newValue ? Boolean.TRUE : Boolean.FALSE));
    }
    
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeDelegate.firePropertyChange(propertyName, oldValue, newValue);
    }
    
    public void addpwUpdateListener(DasUpdateListener l) {
        listenerList.add(DasUpdateListener.class, l);
    }
    
    public void removepwUpdateListener(DasUpdateListener l) {
        listenerList.remove(DasUpdateListener.class, l);
    }
    
    protected void fireUpdate() {
        Object[] listeners = listenerList.getListenerList();
        DasUpdateEvent e = new DasUpdateEvent(this);
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==DasUpdateListener.class) {
                ((DasUpdateListener)listeners[i+1]).update(e);
            }
        }
    }
    
    public void popHistory() {
        if (!history.empty()) {
            history.pop();
        }
    }
    
    public Object clone() throws CloneNotSupportedException {
        DataRange result= (DataRange) super.clone();
        result.history= (Stack)history.clone();
        result.forwardHistory= (Stack)forwardHistory.clone();
        return result;
    }
    
}

