/* File: DasSelectionEvent.java
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

package edu.uiowa.physics.pw.das.event;

import java.awt.*;
import java.util.EventObject;

/**
 *
 * @author  eew
 */
public class DasSelectionEvent extends EventObject
{

    /** Type-safe enumeration class for selection type contants. */
    public static class Type
    {
	public static final Type AREA_SELECTION =
	    new Type("AREA_SELECTION", false);
	public static final Type POINT_SELECTION =
	    new Type("POINT_SELECTION", true);
	public static final Type VERTICAL_SLICE_SELECTION =
	    new Type("VERTICAL_SLICE_SELECTION", true);
	public static final Type HORIZONTAL_SLICE_SELECTION =
	    new Type("HORIZONTAL_SLICE_SELECTION", true);
	public static final Type VERTICAL_RANGE_SELECTION =
	    new Type("VERTICAL_RANGE_SELECTION", false);
	public static final Type HORIZONTAL_RANGE_SELECTION =
	    new Type("HORIZONTAL_RANGE_SELECTION", false);
	public static final Type NO_SELECTION =
	    new Type("NO_SELECTION", false);

	private String type;
	private boolean single;

	private Type(String type, boolean single)
	{ this.type = type; this.single = single; }
	public String toString() { return type; }
	public boolean isSingleSelection() { return single; }
	public boolean equals(Object o) { return this==o; }
    }
    
    protected Point dot;
    protected Point mark;
    protected boolean isShiftDown;
    protected boolean clearSelection;
    protected DasSelectionEvent.Type selectionType;

    private Point selectionEnd;    
    
    private DasSelectionEvent.Type selectionMode = DasSelectionEvent.Type.POINT_SELECTION;
    
    private Point selectionStart;
    
    /** Creates a new instance of DasSelectionEvent
     *
     * @param source The source of the event.
     * @param ds The DataSet object associated with the source.
     * @param selectionType The type of selection.
     * @param isShiftDown <code>true</code> if the shift buttons was
     *    down when the selection was made, <code>false</code> otherwise.
     * @param dot The point at which the selection started.
     * @param mark The point at which the selection ended, or the point
     *   of the selection for single selections.
     */
    public DasSelectionEvent(Object source,
			    DasSelectionEvent.Type selectionType,
			    boolean isShiftDown,
			    Point dot, Point mark)
    {
        super(source);
        this.selectionType = selectionType;
        this.isShiftDown = isShiftDown;
	this.dot = new Point(dot);
	this.mark = new Point(mark);
        this.clearSelection = false;
    }

    public Point getDot()
    {
	return new Point(dot);
    }

    public Point getMark()
    {
	return new Point(mark);
    }

    public int getDotX()
    {
	return dot.x;
    }

    public int getDotY()
    {
	return dot.y;
    }

    public int getMarkX()
    {
	return mark.x;
    }

    public int getMarkY()
    {
	return mark.y;
    }
    
    public boolean isShiftDown()
    {
        return isShiftDown;
    }
    
    public boolean shouldClearSelection()
    {
        return clearSelection;
    }
    
    public void clearSelection()
    {
        clearSelection = true;
    }
    
    public DasSelectionEvent.Type getSelectionType()
    {
        return selectionType;
    }
}
