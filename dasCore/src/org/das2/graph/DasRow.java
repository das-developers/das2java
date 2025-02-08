/* File: DasRow.java
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

package org.das2.graph;

import java.text.ParseException;

/**
 * DasRow object represents the vertical position on the canvas.
 * @author  jbf
 */
public class DasRow extends DasDevicePosition {
    
    /**
     * create a DasRow with the normal position and no offsets.
     * @param parent the canvas where this lives.
     * @param top the top of the row in normal coordinates.
     * @param bottom the bottom of the row in normal (0-1.) coordinates 
     */
    public DasRow( DasCanvas parent, double top, double bottom) {
        super(parent,top,bottom,false );
    }
    
    /**
     * create a DasRow
     * @param canvas the canvas where this lives.
     * @param parent the parent row or null to which this is relative.
     * @param nMin normal position of the top with respect to the canvas or parent if non-null.
     * @param nMax normal position of the bottom with respect to the canvas or parent if non-null.
     * @param emMin em offset of the top from the minimum position, in canvas font heights.
     * @param emMax em offset of the bottom from the maximum position, in canvas font heights.
     * @param ptMin point offset of the top from the minimum position, note points are the same as pixels.
     * @param ptMax point offset of the bottom from the maximum position, note points are the same as pixels.
     */
    public DasRow( DasCanvas canvas, DasRow parent, double nMin, double nMax, 
            double emMin, double emMax, int ptMin, int ptMax ) {
        super( canvas, false, parent, nMin, nMax, emMin, emMax, ptMin, ptMax );
    }
        
    /**
     * makes a new DasRow by parsing a string like "100%-5em+3pt" to get the offsets.
     * The three qualifiers are "%", "em", and "pt", but "px" is allowed as well 
     * as surely people will use that by mistake.  If an offset or the normal position
     * is not specified, then 0 is used.
     *
     * @param canvas the canvas for the layout, ignored when a parent DasRow is used.
     * @param parent if non-null, this DasRow is specified with respect to parent.
     * @param minStr a string like "0%+5em"
     * @param maxStr a string like "100%-7em"
     * @return a new DasRow.
     * @throws IllegalArgumentException if the strings cannot be parsed
     */
    public static DasRow create( DasCanvas canvas, DasRow parent, String minStr, String maxStr ) {
        double[] min, max;
        try {
            min= parseLayoutStr( minStr );
        } catch ( ParseException e ) {
            throw new IllegalArgumentException("unable to parse min: \""+minStr+"\"");
        }
        try {
            max= parseLayoutStr( maxStr );
        } catch ( ParseException e ) {
            throw new IllegalArgumentException("unable to parse max: \""+maxStr+"\"");
        }
        return new DasRow( canvas, parent, min[0], max[0], min[1], max[1], (int)min[2], (int)max[2] );
    }
    
    /**
     * placeholder for unassigned value.
     */
    public static final DasRow NULL= new DasRow(null,null,0,0,0,0,0,0);
    
    /**
     * @deprecated This created a row that was not attached to anything, so
     * it was simply a convenience method that didn't save much effort.
     */
    public DasRow createSubRow(double ptop, double pbottom) {
        double top= getMinimum();
        double bottom= getMaximum();
        double delta= top-bottom;
        return new DasRow(getCanvas(),bottom+ptop*delta,bottom+pbottom*delta);
    }
    
    /**
     * return the height in points (pixels) of the row.
     * @return the height in points (pixels) of the row.
     */
    public int getHeight() {
        return getDMaximum()-getDMinimum();
    }
    
    /**
     * @deprecated use new DasRow(parent,0.1,0.9);
     */
    public static DasRow create(DasCanvas parent) {
        return new DasRow(parent,0.1,0.9);
    }
    
    /**
     * @deprecated a convenience method should be added.
     * @param parent
     * @param iplot
     * @param nplot
     * @return 
     */
    public static DasRow create( DasCanvas parent, int iplot, int nplot ) {
        double min= 0.1 + iplot * ( 0.8 ) / nplot;
        double max= 0.099 + ( iplot + 1 ) * ( 0.8 ) / nplot;
        return new DasRow( parent, min, max );
    }
    
    /**
     * create a row that is positioned relative to this row.
     * @param ptop the normal position
     * @param pbottom the normal position
     * @return the new row.
     */
    public DasRow createAttachedRow(double ptop, double pbottom) {
        return new DasRow(null,this,ptop,pbottom,0,0,0,0);
    }
            
    /**
     * return the device location of the top of the row.  
     * @return the device location of the top of the row.  
     */
    public int top() {
        return getDMinimum();
    }
    
    /**
     * return the device location of the bottom (non-inclusive) of the row.
     * @return the device location of the bottom (non-inclusive) of the row.
     */
    public int bottom() {
        return getDMaximum();
    }

    /**
     * reset the parent to be this new DasColumn, or null.
     * @param r the new parent row, or null.
     */
    public void setParentRow( DasRow r ) {
        setParentDevicePosition(r);
    }
    
    /**
     * return the name of the column this is attached to, or empty string.
     * @return  the name of the column this is attached to, or empty string.
     */
    public String getParentRowName( ) {
        return this.getParentDasName();
    }
    
}
