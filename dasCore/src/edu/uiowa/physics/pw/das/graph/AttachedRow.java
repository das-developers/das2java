/* File: AttachedRow.java
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

/** TODO */
public class AttachedRow extends DasRow {
    
    private DasRow row;
    
    /** Creates a new instance of AttachedRow
     * TODO
     * @param row the new row.
     * @param minimum the new minimum value for the row in the device space.
     * @param maximum the new maximum value for the row in the device space.
     */
    public AttachedRow(DasRow row, double minimum, double maximum) {
        super(row.getParent(),minimum,maximum);
        this.row= row;
    }
    
    /** Gets the maximum for the row in the device space.
     * @return the maximum value for the row in the device space.
     */    
    public int getDMaximum() {
        if (row==null) {
            return 0;
        } else {
            int delta= row.getHeight();
            return row.getDMinimum()+(int)(delta*getMaximum());
        }
    }
    
    /** Gets the minimum for the row in the device space.
     * @return the minimum value for the row in the device space.
     */    
    public int getDMinimum() {
        if (row==null) { // during construction, this is called.
            return 0;
        } else {
            double delta= row.getHeight();
            return row.getDMinimum()+(int)(delta*getMinimum());
        }
    }
    
    /** Gets the height of the row in the device space.
     * @return the height of the row in the device space.
     */    
    public int getHeight() {
        return (int)(row.getHeight()*(getMaximum()-getMinimum()));
    }
    
    /** Adds a listener to the row to receive DasUpdateEvents.
     * @param l he new listener.
     */    
    public void addpwUpdateListener(DasUpdateListener l) {
        super.addpwUpdateListener(l);
        row.addpwUpdateListener(l);
    }
    
    /** Removes a listener from the row to stop receving DasUpdateEvents.
     * @param l the listener to be removed.
     */    
    public void removepwUpdateListener(DasUpdateListener l) {
        super.removepwUpdateListener(l);
        row.removepwUpdateListener(l);
    }
    
    /** Sets the minimum value for the row in the device space.
     * @param value the new minimum value of the row in the device space.
     */    
    public void setDMinimum(int value) {
        double delta= row.getHeight();
        setMinimum((value-row.getDMinimum())/delta);
    }
    
    /** Sets the maximum value of the row in the device space.
     * @param value the new maximum value of the row in the device space.
     */    
    public void setDMaximum(int value) {
        double delta= row.getHeight();
        setMaximum((value-row.getDMinimum())/delta);
    }
    
    public void setDPosition(int minimum, int maximum) {
        double delta= row.getHeight();
        this.setMinimum((minimum-row.getDMinimum())/delta);
        this.setMaximum((maximum-row.getDMinimum())/delta);
        fireUpdate();
    }
}
