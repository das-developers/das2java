
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;
import edu.uiowa.physics.pw.das.graph.event.DasUpdateEvent;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.*;

import javax.swing.event.EventListenerList;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.Stack;
import java.util.logging.Logger;

public class DataRange implements Cloneable {

    private DasAxis parent;

    private Units units;

    /* minimum, possibly with log applied */
    private double minimum;

    /* maximum, possibly with log applied */
    private double maximum;

    /* storage for values of temporary invalid states during state transition.*/
    private Datum pendingMin=null, pendingMax=null;

    /* range is the min and max, not in the log space.  This is the range that controls the DataRange, as opposed
       to minimum and maximum, which are simply to implement it. */
    private DatumRange range;

    private boolean log;

    private EventListenerList listenerList = new EventListenerList();

    private Stack history;

    private Stack forwardHistory;

    private List favorites;

    private PropertyChangeSupport propertyChangeDelegate;

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new Error("Assertion failure");
        }
    }

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
        this.range= new DatumRange( min, max );
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
        this.range= range;
        this.minimum= range.min().doubleValue(this.units);
        this.maximum= range.max().doubleValue(this.units);
        if ( isLog() ) {
            this.minimum= DasMath.log10( this.minimum );
            this.maximum= DasMath.log10( this.maximum );
        }
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

    /* @returns the floating point index within the range, where 0.0 indicates
     * @param value is equal to minimum and 1.0 means it is equal to maximum,
     * with log/lin/??? curvature considered.
     */
    public final double findex( double value ) {
        if ( log ) {
            value= DasMath.log10(value);
        }
        return ( value-minimum ) / ( maximum - minimum );
    }

    public Units getUnits() { return units; }

    public DatumRange getDatumRange() { return range; }

    public void setUnits(Units newUnits) {
        if (units.equals(newUnits)) {
            return;
        }

        minimum = units.convertDoubleTo(newUnits, minimum);
        maximum = units.convertDoubleTo(newUnits, maximum);
        units = newUnits;

        clearHistory();

    }

    public void setMinimum( Datum min ) {
        Datum max= pendingMax!=null ? pendingMax : this.range.max();
        if ( min.le( max ) ) {
            setRange( new DatumRange( min, max ) );
        } else {
            this.pendingMin= min;
        }
    }

    public void setMaximum( Datum max ) {
        Datum min= pendingMin!=null ? pendingMin : this.range.min();
        if ( min.le( max ) ) {
            setRange( new DatumRange( min, max ) );
        } else {
            this.pendingMax= max;
        }
    }

    private void reportHistory() {
        Logger log= DasLogger.getLogger( DasLogger.GUI_LOG );
        log.finest("history: "+history.size());
        for ( int i=0; i<history.size(); i++ ) {
            log.finest("   "+history.get(i));
        }
        log.finest("forwardHistory: "+forwardHistory.size());
        log.finest("-------------");
    }

    protected void clearHistory() {
        ArrayList oldHistory= new ArrayList( history );
        history.removeAllElements();
        forwardHistory.removeAllElements();
        firePropertyChange("history", oldHistory, history );
    }

    public void addToFavorites( DatumRange range ) {
        if (favorites==null) favorites= new ArrayList();
        List oldFavorites= new ArrayList(favorites);
        favorites.add( range );
        firePropertyChange("favorites", oldFavorites, favorites );
    }

    public List getFavorites() {
        if ( favorites==null ) {
            return new ArrayList();
        } else {
            return new ArrayList( favorites );
        }
    }

    public List getHistory() {
        if ( history==null ) {
            return new ArrayList();
        } else {
            List result= new ArrayList( history );
            Collections.reverse(result);
            return result.subList(0,Math.min(result.size(),10));
        }

    }

    public void setRange( DatumRange range ) {
        pendingMin= null;
        pendingMax= null;
        setRange( range, true );
    }

    public void setRange( double min, double max ) {
        DatumRange newRange;
        if ( log ) {
            newRange= new DatumRange( DasMath.exp10( min ), DasMath.exp10( max ), units );
        } else {
            newRange= new DatumRange( min, max, units );
        }
        setRange( newRange, true );
    }

    private void setRange( DatumRange range, boolean pushHistory ) {

        if ( range.getUnits()!=this.units ) {
            throw new IllegalArgumentException("units may not be changed");
        }

        if ( pushHistory ) {
            List oldHistory= new ArrayList( history );
            history.push(this.range);
            DasApplication.getDefaultApplication().getLogger( DasApplication.GUI_LOG ).fine( "push history: "+range );
            forwardHistory.removeAllElements();
            firePropertyChange( "history", new ArrayList(), new ArrayList(history) );
        }

        this.range= range;

        double oldMin = minimum;
        double oldMax = maximum;

        minimum = range.min().doubleValue( units );
        maximum = range.max().doubleValue( units );
        if ( log ) {
            minimum= DasMath.log10( minimum );
            maximum= DasMath.log10( maximum );
        }

        fireUpdate();
        if (minimum != oldMin) firePropertyChange("minimum", oldMin, minimum);
        if (maximum != oldMax) firePropertyChange("maximum", oldMax, maximum);
    }

    public void setRangePrev() {
        reportHistory();
        if (!history.isEmpty()) {
            forwardHistory.push( range );
            DatumRange newRange= (DatumRange) history.pop();
            setRange( newRange, false );
            firePropertyChange( "history", null, new ArrayList(history) );
        }
    }

    public void setRangeForward() {
        reportHistory();
        if (!forwardHistory.isEmpty()) {
            List oldHistory= new ArrayList( history );
            history.push( range );
            DatumRange h= (DatumRange) forwardHistory.pop();
            setRange( h, false );
            firePropertyChange("history",oldHistory,history);
        }
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

    public void addUpdateListener(DasUpdateListener l) {
        listenerList.add(DasUpdateListener.class, l);
    }

    public void removeUpdateListener(DasUpdateListener l) {
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

    /**
     * pop ipop items off the history list.  This is used by the history menu
     */
    protected void popHistory( int ipop ) {
        for ( int i=0; i<ipop; i++ ) {
            history.pop();
        }
    }

    /* @returns a dummy DataRange that simply holds min, max.  So that no one is
     * listening to the changes and no history.
     */
    public static class Animation extends DataRange {
        private double minimum, maximum;
        public Animation( DatumRange range, boolean log ) {
            super( null, range.min(), range.max(), log );
        }
        protected void fireUpdate() {};
        public void setRange( double min, double max ) {
            minimum= min;
            maximum= max;
        }
        public double getMinimum() { return minimum; }
        public double getMaximum() { return maximum; }
    }


    public static DataRange getAnimationDataRange( DatumRange range, boolean log ) {
        return new DataRange.Animation( range, log );
    }


}

