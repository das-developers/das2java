/* File: DasColumn.java
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
 * DasColumn object represents the horizontal position on the canvas.
 * @author  jbf
 */
public class DasColumn extends DasDevicePosition {
    
    /**
     * create a DasColumn with the normal position and no offsets.
     * @param parent the canvas where this lives.
     * @param nMin normal position of the left with respect to the canvas.
     * @param nMax normal position of the right with respect to the canvas
     */
    public DasColumn(DasCanvas parent, double nMin, double nMax) {
        super(parent,nMin,nMax,true);
    }
    
    /**
     * create a DasColumn
     * @param canvas the canvas where this lives.
     * @param parent the parent row or null to which this is relative.
     * @param nMin normal position of the left with respect to the canvas or parent if non-null.
     * @param nMax normal position of the right with respect to the canvas or parent if non-null.
     * @param emMin em offset of the left from the minimum position, in canvas font heights.
     * @param emMax em offset of the right from the maximum position, in canvas font heights.
     * @param ptMin point offset of the left from the minimum position, note points are the same as pixels.
     * @param ptMax point offset of the right from the maximum position, note points are the same as pixels.
     */
    public DasColumn( DasCanvas canvas, DasColumn parent, double nMin, double nMax,
            double emMin, double emMax, int ptMin, int ptMax ) {
        super( canvas, true, parent, nMin, nMax, emMin, emMax, ptMin, ptMax );
    }
    
    /**
     * makes a new DasColumn by parsing a string like "100%-5em+3pt" to get the offsets.
     * The three qualifiers are "%", "em", and "pt", but "px" is allowed as well 
     * as surely people will use that by mistake.  If an offset or the normal position
     * is not specified, then 0 is used.
     *
     * @param canvas the canvas for the layout, ignored when a parent DasColumn is used.
     * @param parent if non-null, this DasColumn is specified with respect to parent.
     * @param minStr a string like "0%+5em"
     * @param maxStr a string like "100%-7em"
     * @return a new DasColumn
     * @throws IllegalArgumentException if the strings cannot be parsed
     */
    public static DasColumn create( DasCanvas canvas, DasColumn parent, String minStr, String maxStr ) {
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
        return new DasColumn( canvas, parent, min[0], max[0], min[1], max[1], (int)min[2], (int)max[2] );
    }
    
    /**
     * placeholder for unassigned value.
     */
    public static final DasColumn NULL= new DasColumn(null,null,0,0,0,0,0,0);
    
    /**
     * return the width in points (pixels) of the column.
     * @return the width in points (pixels) of the column.
     */
    public int getWidth() {
        return getDMaximum()-getDMinimum();
    }
    
    /**
     * create a DasColumn 
     * @param parent the canvas where this lives.
     * @return DasColumn
     */
    public static DasColumn create(DasCanvas parent) {
        return new DasColumn(parent,null,0.0,1.0,5,-3,0,0);
    }
  
    /**
     * create a column that is positioned relative to this column.
     * @param pleft the normal position
     * @param pright the normal position
     * @return the new row.
     */    
    public DasColumn createAttachedColumn(double pleft, double pright) {
        return new DasColumn(null,this,pleft,pright,0,0,0,0);
    }
        
    /**
     * return pixel location of the left of the column.
     * @return pixel location of the left of the column.
     */
    public int left() {
        return getDMinimum();
    }
    
    /**
     * return pixel location the right (non-inclusive) of the column.
     * @return pixel location the right (non-inclusive) of the column.
     */
    public int right() {
        return getDMaximum();
    }
    
    /**
     * reset the parent to be this new DasColumn, or null.
     * @param c the new parent column, or null.
     */
    public void setParentColumn( DasColumn c ) {
        setParentDevicePosition(c);
    }
    
    /**
     * return the name of the column this is attached to, or empty string.
     * @return  the name of the column this is attached to, or empty string.
     */
    public String getParentColumnName( ) {
        return this.getParentDasName();
    }
}
