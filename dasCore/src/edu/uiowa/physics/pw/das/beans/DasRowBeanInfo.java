/* File: DasRowBeanInfo.java
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

package edu.uiowa.physics.pw.das.beans;

/**
 * Bean Info implementation for DasDevicePosition
 *
 * @author Edward West
 */
public class DasRowBeanInfo extends AccessLevelBeanInfo {
    
    private static Property[] properties = {
        new Property("name", AccessLevel.DASML, "getDasName", "setDasName", null),
        new Property("minimum", AccessLevel.DASML, "getMinimum", "setMinimum", null),
        new Property("maximum", AccessLevel.DASML, "getMaximum", "setMaximum", null),
        new Property("Dminimum", AccessLevel.DASML, "getDMinimum", "setDMinimum", null),
        new Property("Dmaximum", AccessLevel.DASML, "getDMaximum", "setDMaximum", null), 
    /*    new Property("top", AccessLevel.DASML, "getTop", "setTop", null),
        new Property("bottom", AccessLevel.DASML, "getBottom", "setBottom", null), */
        
    };
    
    public DasRowBeanInfo() {
        super(properties, edu.uiowa.physics.pw.das.graph.DasRow.class);
    }
    
}
