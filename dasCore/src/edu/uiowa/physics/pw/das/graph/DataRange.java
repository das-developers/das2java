
/* File: DataRange.java
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

import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateEvent;
import edu.uiowa.physics.pw.das.util.*;

import javax.swing.event.EventListenerList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Stack;

public class DataRange implements Cloneable {
    
    private DasAxis parent;
    
    private Units units;
    
    private double minimum;
    
    private double maximum;
    
    private boolean log;
    
    private EventListenerList listenerList = new EventListenerList();
    
    private Stack history;
    
    private Stack forwardHistory;
    
    private PropertyChangeSupport propertyChangeDelegate;
    
    public DataRange( DasAxis parent, Datum min, Datum max, boolean log ) {
        if (min.gt(max)) throw new IllegalArgumentException("data min on axis is greater than data max");
        if (!min.isValid()) throw new IllegalArgumentException("data_minimum on axis is NaN");
        if (!max.isValid()) throw new IllegalArgumentException("data_maximum on axis is NaN");
        if (min.getUnits()!=max.getUnits())  throw new IllegalArgumentException("units don't match on range");
        this.parent= parent;
        units= min.getUnits();
        if ( log ) {
            minimum = DasMath.log10(min.doubleValue(units));
            maximum = DasMath.log10(max.doubleValue(units));
        } else {
            minimum = min.doubleValue(units);
            maximum = max.doubleValue(units);
        }
        this.log = log;
        history = new Stack();
        forwardHistory = new Stack();
        propertyChangeDelegate = new PropertyChangeSupport(this);
    }
    
    public boolean isLog() {
        return log;
    }
    
    /*
     * need some method for changing the axis units...
     */
    public void resetRange( DatumRange range ) {
        this.units= range.getUnits();
        this.minimum= range.min().doubleValue(this.units); 
        this.maximum= range.max().doubleValue(this.units); 
        fireUpdate();
    }
    
    public void setLog(boolean log) {
        /*
         * propose new logic for going between lin/log axes:
         * to log: pick first minor tick greater than zero.
         * to lin: pick first minor tick in linear space less than min.
         */
        
        if (this.log==log) return;
        boolean oldLog = this.log;
        if (log) {
            if (minimum<=0. || maximum <=0.) {
                return;
            } else {
                this.minimum= DasMath.log10(minimum);
                this.maximum= DasMath.log10(maximum);
            }
        } else {
            this.minimum= DasMath.exp10(minimum);
            this.maximum= DasMath.exp10(maximum);
        }
        clearHistory();
        this.log=log;
        firePropertyChange("log", oldLog, log);
        fireUpdate();
    }
    
    public DasAxis getCreator() {
        return parent;
    }
    
    public double getMinimum() { return minimum; }
    
    public double getMaximum() { return maximum; }
    
    public Units getUnits() { return units; }
    
    public void setUnits(Units newUnits) {
        if (units.equals(newUnits)) {
            return;
        }
        
        minimum = units.convertDoubleTo(newUnits, minimum);
        maximum = units.convertDoubleTo(newUnits, maximum);
        units = newUnits;
        
        clearHistory();
        
    }
    
    public void setMinimum(double min) {
        setRange(min, maximum);
    }
    
    public void setMaximum(double max) {
        setRange(minimum, max);
    }
    
    private void reportHistory() {
        edu.uiowa.physics.pw.das.util.DasDie.println("history: "+history.size());
        for ( int i=0; i<history.size(); i++ ) {
            edu.uiowa.physics.pw.das.util.DasDie.print("   "+((Object[])history.get(i))[0]+" - ");
            edu.uiowa.physics.pw.das.util.DasDie.println("   "+((Object[])history.get(i))[1]);
        }
        edu.uiowa.physics.pw.das.util.DasDie.println("forwardHistory: "+forwardHistory.size());
        edu.uiowa.physics.pw.das.util.DasDie.println("-------------");
    }
    
    protected void clearHistory() {
        history.removeAllElements();
        forwardHistory.removeAllElements();
    }
    
    public void setRange( double min, double max ) {
        boolean pushStack= true;
        
        if ( pushStack ) {
            Datum h[] = new Datum[2];
            if (minimum!=maximum) {  //  kludge for create() method
                h[0] = Datum.create(minimum,units);
                h[1] = Datum.create(maximum,units);
                history.push(h);
//                reportHistory();
            }
            forwardHistory.removeAllElements();
        }
        
        double oldMin = minimum;
        double oldMax = maximum;
        
        minimum = min;
        maximum = max;
        fireUpdate();
        if (minimum != oldMin) firePropertyChange("minimum", oldMin, minimum);
        if (maximum != oldMax) firePropertyChange("maximum", oldMax, maximum);
    }
    
    public void setRangePrev() {
        double oldMin = minimum;
        double oldMax = maximum;
        reportHistory();
        if (!history.isEmpty()) {
            forwardHistory.push( new Datum [] {Datum.create(minimum,units), Datum.create(maximum,units)} );
            Datum [] h= (Datum[]) history.pop();
            
            if (h[0].getUnits()!=units) {
                h[0]= h[0].convertTo(units);
                h[1]= h[1].convertTo(units);
            }
            minimum = h[0].doubleValue(units);
            maximum = h[1].doubleValue(units);
            
            fireUpdate();
        }
        if (minimum != oldMin) firePropertyChange("minimum", oldMin, minimum);
        if (maximum != oldMax) firePropertyChange("maximum", oldMax, maximum);
        reportHistory();
    }
    
    public void setRangeForward() {
        double oldMin = minimum;
        double oldMax = maximum;
        reportHistory();
        if (!forwardHistory.isEmpty()) {
            history.push( new Datum [] {Datum.create(minimum,units), Datum.create(maximum,units)} );
            Datum [] h= (Datum[]) forwardHistory.pop();
            
            if (h[0].getUnits()!=units) {
                h[0]= h[0].convertTo(units);
                h[1]= h[1].convertTo(units);
            }
            minimum = h[0].doubleValue(units);
            maximum = h[1].doubleValue(units);
            
            fireUpdate();
        }
        reportHistory();
        if (minimum != oldMin) firePropertyChange("minimum", oldMin, minimum);
        if (maximum != oldMax) firePropertyChange("maximum", oldMax, maximum);
        reportHistory();
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
    
    public DataRange getAnimationDataRange() {
        Datum min= Datum.create( this.getMinimum(), this.getUnits() );
        Datum max= Datum.create( this.getMaximum(), this.getUnits() );
        return new DataRange( this.parent, min, max, this.isLog() ) {
            protected void fireUpdate() {};
            public void setRange( double min, double max ) {
                minimum= min;
                maximum= max;
            }
            public double getMinimum() { return minimum; }
            public double getMaximum() { return maximum; }
        };
    }
    
    
}

