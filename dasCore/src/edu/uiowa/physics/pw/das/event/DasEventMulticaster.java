/* File: DasEventMulticaster.java
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

package edu.uiowa.physics.pw.das.event;

import java.util.EventListener;

/**
 *
 * @author  eew
 */
public class DasEventMulticaster extends java.awt.AWTEventMulticaster
    implements DataPointSelectionListener, DataRangeSelectionListener,
    TimeRangeSelectionListener {

    /** Creates a new instance of DasEventMultiCaster */
    protected DasEventMulticaster(EventListener a, EventListener b) {
        super(a, b);
    }

    public void DataPointSelected(DataPointSelectionEvent e) {
        ((DataPointSelectionListener)a).DataPointSelected(e);
        ((DataPointSelectionListener)b).DataPointSelected(e);
    }

    public void DataRangeSelected(DataRangeSelectionEvent e) {
        ((DataRangeSelectionListener)a).DataRangeSelected(e);
        ((DataRangeSelectionListener)b).DataRangeSelected(e);
    }

    public void TimeRangeSelected(TimeRangeSelectionEvent e) {
        ((TimeRangeSelectionListener)a).TimeRangeSelected(e);
        ((TimeRangeSelectionListener)b).TimeRangeSelected(e);
    }

    public static DataPointSelectionListener add(DataPointSelectionListener a, DataPointSelectionListener b) {
        if (a == null) return b;
        if (b == null) return a;
        return new DasEventMulticaster(a, b);
    }    
        
    public static DataRangeSelectionListener add(DataRangeSelectionListener a, DataRangeSelectionListener b) {
        if (a == null) return b;
        if (b == null) return a;
        return new DasEventMulticaster(a, b);
    }
    
    public static TimeRangeSelectionListener add(TimeRangeSelectionListener a, TimeRangeSelectionListener b) {
        if (a == null) return b;
        if (b == null) return a;
        return new DasEventMulticaster(a, b);
    }
    
    public static DataPointSelectionListener remove(DataPointSelectionListener a, DataPointSelectionListener b) {
        if (a instanceof DasEventMulticaster) {
            return (DataPointSelectionListener)((DasEventMulticaster)a).remove(b);
        }
        return (a == b ? null : a);
    }
    
    public static DataRangeSelectionListener remove(DataRangeSelectionListener a, DataRangeSelectionListener b) {
        if (a instanceof DasEventMulticaster) {
            return (DataRangeSelectionListener)((DasEventMulticaster)a).remove(b);
        }
        return (a == b ? null : a);
    }
    
    public static TimeRangeSelectionListener remove(TimeRangeSelectionListener a, TimeRangeSelectionListener b) {
        if (a instanceof DasEventMulticaster) {
            return (TimeRangeSelectionListener)((DasEventMulticaster)a).remove(b);
        }
        return (a == b ? null : a);
    }
    
    protected EventListener remove(EventListener listener) {
        if (listener == a) return b;
        if (listener == b) return a;
        EventListener aa;
        EventListener bb;
        if (a instanceof DasEventMulticaster) {
            aa = ((DasEventMulticaster)a).remove(listener);
        }
        else {
            aa = a;
        }
        if (b instanceof DasEventMulticaster) {
            bb = ((DasEventMulticaster)b).remove(listener);
        }
        else {
            bb = b;
        }
        if (bb == b && aa == a) return this;
        return new DasEventMulticaster(aa, bb);
    }
    
    public String toString() {
        
        return "[" + a + "," + b + "]";
        
    }

}
