/* File: AttachedRow.java
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
public class AttachedRow extends DasRow {
    
    private DasRow row;
    
    /** Creates a new instance of AttachedRow */
    public AttachedRow(DasRow row, double minimum, double maximum) {
        super(row.getParent(),minimum,maximum);
        this.row= row;
    }
    
    public double getDMaximum() {
        if (row==null) {
            return 0.;
        } else {
            double delta= row.getHeight();
            return row.getDMinimum()+delta*getMaximum();
        }
    }
    
    public double getDMinimum() {
        if (row==null) { // during construction, this is called.
            return 0.;
        } else {
            double delta= row.getHeight();
            return row.getDMinimum()+delta*getMinimum();
        }
    }
    
    public double getHeight() {
        return row.getHeight()*(getMaximum()-getMinimum());
    }
    
    public void addpwUpdateListener(DasUpdateListener l) {
        super.addpwUpdateListener(l);
        row.addpwUpdateListener(l);
    }
    
    public void removepwUpdateListener(DasUpdateListener l) {
        super.removepwUpdateListener(l);
        row.removepwUpdateListener(l);
    }
    
    public void setDMinimum(double value) {
        double delta= row.getHeight();
        setMinimum((value-row.getDMinimum())/delta);
    }
    
    public void setDMaximum(double value) {
        double delta= row.getHeight();
        setMaximum((value-row.getDMinimum())/delta);
    }
    
}
