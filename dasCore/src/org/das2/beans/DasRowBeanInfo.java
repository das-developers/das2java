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

package org.das2.beans;

/**
 * Bean Info implementation for DasDevicePosition
 *
 * @author Edward West
 */
public class DasRowBeanInfo extends AccessLevelBeanInfo {
    
    private static Property[] properties = {
        new Property("name", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getDasName", "setDasName", null),
        new Property("parent",  AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getParent", null, null),
        new Property("parentRowName",  AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getParentRowName", null, null),        
        new Property("minimum", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getMinimum", "setMinimum", null),
        new Property("maximum", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getMaximum", "setMaximum", null),
        new Property("dminimum", AccessLevel.DASML, "getDMinimum", "setDMinimum", null),
        new Property("dmaximum", AccessLevel.DASML, "getDMaximum", "setDMaximum", null), 
        new Property("emMinimum", AccessLevel.DASML, "getEmMinimum", "setEmMinimum", null),
        new Property("emMaximum", AccessLevel.DASML, "getEmMaximum", "setEmMaximum", null), 
        new Property("ptMinimum", AccessLevel.DASML, "getPtMinimum", "setPtMinimum", null),
        new Property("ptMaximum", AccessLevel.DASML, "getPtMaximum", "setPtMaximum", null), 
    /*    new Property("top", AccessLevel.DASML, "getTop", "setTop", null),
        new Property("bottom", AccessLevel.DASML, "getBottom", "setBottom", null), */
        
    };
    
    public DasRowBeanInfo() {
        super(properties, org.das2.graph.DasRow.class);
    }
    
}
