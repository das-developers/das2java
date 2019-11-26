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

public class TickCurveRendererBeanInfo extends AccessLevelBeanInfo {
    
    private static Property[] properties = {        
        new Property("TickStyle", AccessLevel.DASML, "getTickStyle", "setTickStyle", null),
        new Property("LineWidth", AccessLevel.DASML, "getLineWidth", "setLineWidth", null),
        new Property("TickLength", AccessLevel.DASML, "getTickLength", "setTickLength", null),
        new Property("TickSpacing", AccessLevel.DASML, "getTickSpacing", "setTickSpacing", null),
        new Property("TickValues",AccessLevel.DASML, "getTickValues", "setTickValues", null),
        new Property("color", AccessLevel.DASML, "getColor", "setColor", null),
    };
    
    public TickCurveRendererBeanInfo() {
        super(properties, org.das2.graph.TickCurveRenderer.class);
    }
    
}
