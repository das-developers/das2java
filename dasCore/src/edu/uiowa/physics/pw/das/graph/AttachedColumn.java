/* File: AttachedColumn.java
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
import edu.uiowa.physics.pw.das.graph.event.DasUpdateListener;

/**
 *
 * @author  jbf
 */
public class AttachedColumn extends DasColumn {
    
    private DasColumn column;
    
    /** Creates a new instance of AttachedColumn */
    public AttachedColumn(DasColumn column, double minimum, double maximum) {
        super(column.getParent(),minimum,maximum);
        this.column= column;
    }
    
    public double getDMaximum() {
        if ( column==null ) {
            return 0.;
        } else {
            double delta= column.getWidth();
            return column.getDMinimum()+delta*getMaximum();
        }
    }
    
    public double getDMinimum() {
        if ( column==null ) { // called during construction
            return 0.;
        } else {
            double delta= column.getWidth();
            return column.getDMinimum()+delta*getMinimum();
        }
    }
    
    public void setDMinimum(double minimum) {
        double delta= column.getWidth();
        this.setMinimum((minimum-column.getDMinimum())/delta);
        fireUpdate();
    }
    
    public void setDMaximum(double maximum) {
        double delta= column.getWidth();
        this.setMaximum((maximum-column.getDMinimum())/delta);
        fireUpdate();
    }
    
    
    public void setDPosition(double minimum, double maximum) {
        double delta= column.getWidth();
        this.setMinimum((minimum-column.getDMinimum())/delta);
        this.setMaximum((maximum-column.getDMinimum())/delta);
        fireUpdate();
    }
    
    public double getWidth() {
        return column.getWidth()*(getMaximum()-getMinimum());
    }
    
    public void addpwUpdateListener(DasUpdateListener l) {
        super.addpwUpdateListener(l);
        column.addpwUpdateListener(l);
    }
    
    public void removepwUpdateListener(DasUpdateListener l) {
        super.removepwUpdateListener(l);
        column.removepwUpdateListener(l);
    }
}
