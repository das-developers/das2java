/* File: Orientation.java
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

package edu.uiowa.physics.pw.das.dasml;

/**
 *
 * @author  eew
 */
public final class Orientation implements edu.uiowa.physics.pw.das.components.PropertyEditor.Enumeration {
    
    public static final Orientation HORIZONTAL = new Orientation("horizontal");
    
    public static final Orientation VERTICAL = new Orientation("vertical");
    
    private String description;
    
    private Orientation(String description) {
        this.description = description;
    }
    
    public String toString() {
        return description;
    }
    
    public static Orientation valueOf(String str) {
        if (str.equals("vertical")) {
            return VERTICAL;
        }
        if (str.equals("horizontal")) {
            return HORIZONTAL;
        }
        throw new IllegalArgumentException("Orientation must be either 'horizontal' or 'vertical'");
    }
    
    /** An icon can be provided that will be shown in a list
     * along with the textual description of the element.
     * This method should return <code>null</code> if there
     * is no icon available.
     */
    public javax.swing.Icon getListIcon() {
        return null;
    }
    
}
