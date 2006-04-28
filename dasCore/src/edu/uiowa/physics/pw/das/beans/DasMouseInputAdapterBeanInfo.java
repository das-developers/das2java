/* File: DasPlotBeanInfo.java
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

import java.beans.BeanInfo;

public class DasMouseInputAdapterBeanInfo extends AccessLevelBeanInfo {
    
    private static final Property[] properties = {
        new Property("mouseModules", AccessLevel.DASML, "getMouseModules", null, "getMouseModule", null, null),
        //new Property("hoverHighlite", AccessLevel.DASML, "isHoverHighlite", "setHoverHighlite", null ),
        new Property("primaryModule", AccessLevel.DASML, PersistenceLevel.PERSISTENT,
                "getPrimaryModuleLabel", "setPrimaryModuleByLabel", null ),
        new Property("secondaryModule", AccessLevel.DASML, PersistenceLevel.PERSISTENT,
                "getSecondaryModuleLabel", "setSecondaryModuleByLabel", null ) };
    
    
    public DasMouseInputAdapterBeanInfo() {
        super(properties, edu.uiowa.physics.pw.das.event.DasMouseInputAdapter.class);
    }
    
}
