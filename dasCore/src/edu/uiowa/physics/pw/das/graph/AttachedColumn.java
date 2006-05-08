/* File: AttachedColumn.java
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
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;

/**
 * An AttachedColumn is a column whose position is defined relative to another column.  
 * An example application is attaching a ColorBar to a DasPlot so that if the plot is resized, 
 * then the ColorBar moves along with it.
 */
public class AttachedColumn extends DasColumn {
    
    private DasColumn column;
    
    /**
     * Creates a new instance of AttachedColumn.
     * @param column The column to follow.
     * @param minimum the minimum (left) position, with respect to @param column.  1.0 refers 
     * to the right side, 0.0 refers to the left side. So for example,
     * 1.01 is a reasonable setting for a color bar.
     * @param maximum the maximum (right) position, with respect to @param column.  1.0 refers 
     * to the right side, 0.0 refers to the left side. So for example,
     * 1.05 is a reasonable setting for a color bar.
     */
    public AttachedColumn(DasColumn column, double minimum, double maximum) {
        super(column.getParent(),minimum,maximum);
        this.column= column;
    }
    
    public DasColumn getColumn() {
        return this.column;
    }
    
    public void setColumn( DasColumn column ) {
        if ( this.getParent() != column.getParent() ) throw new IllegalArgumentException("column is from wrong canvas");
        this.column= column;
    }
    
    /** Gets the maximum for the column in the device space.
     * @return the maximum value for the column in the device space.
     */
    public int getDMaximum() {
        if ( column==null ) {
            return 0;
        }
        int delta= column.getWidth();
        return column.getDMinimum()+(int)(delta*getMaximum());
    }
    
    /** Gets the minimum for the column in the device space.
     * @return the minimum for the column in the device space.
     */
    public int getDMinimum() {
        if ( column==null ) {
            return 0;
        }
        double delta= column.getWidth();
        return column.getDMinimum()+(int)(delta*getMinimum());        
    }
    
    /**
     * Sets the minimum for the column in the device space.
     * @param minimum the new minimum value for the column.  This value is converted to be with
     * respect to the parent column.
     */
    public void setDMinimum(int minimum) {
        double delta= column.getWidth();
        this.setMinimum((minimum-column.getDMinimum())/delta);
        fireUpdate();
    }
    
    /**
     * Sets the maximum for the column in the device space.
     * @param maximum The new maximum value for the column. This value is converted to be with
     * respect to the parent column.
     */
    public void setDMaximum(int maximum) {
        double delta= column.getWidth();
        this.setMaximum((maximum-column.getDMinimum())/delta);
        fireUpdate();
    }
    
    
    /**
     * Sets the minimum and maximum for the column in the device space.
     * @param minimum the new minimum for the column.
     * @param maximum the new maximum for the column. These values are converted to be with
     * respect to the parent column.
     */
    public void setDPosition(int minimum, int maximum) {
        double delta= column.getWidth();
        this.setMinimum((minimum-column.getDMinimum())/delta);
        this.setMaximum((maximum-column.getDMinimum())/delta);
        fireUpdate();
    }
    
    /**
     * Gets the width of the column.
     * @return the width of the column, in pixels.
     */
    public int getWidth() {
        return (int)(column.getWidth()*(getMaximum()-getMinimum()));
    }
    
    /** Adds a listener to the column to receive DasUpdateEvents.
     * @param l the new listener.
     */
    public void addpwUpdateListener(DasUpdateListener l) {
        super.addpwUpdateListener(l);
        column.addpwUpdateListener(l);
    }
    
    /** Removes a listener from the column to stop receving DasUpdateEvents.
     * @param l the listener to be removed.
     */
    public void removepwUpdateListener(DasUpdateListener l) {
        super.removepwUpdateListener(l);
        column.removepwUpdateListener(l);
    }
}
