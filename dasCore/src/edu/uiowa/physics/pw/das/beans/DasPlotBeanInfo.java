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

public class DasPlotBeanInfo extends AccessLevelBeanInfo {
    
    private static final Property[] properties = {
        new Property("title", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getTitle", "setTitle", null),
        new Property("drawGrid", AccessLevel.DASML, "isDrawGrid", "setDrawGrid", null),
        new Property("drawMinorGrid", AccessLevel.DASML, "isDrawMinorGrid", "setDrawMinorGrid", null),
        new Property("preview", AccessLevel.DASML, "isPreviewEnabled", "setPreviewEnabled", null ),
        new Property("oversize", AccessLevel.DASML, "isOverSize", "setOverSize", null ),
        new Property("renderers", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getRenderers", null, "getRenderer", null, null),        
        new Property("xAxis", AccessLevel.DASML,  PersistenceLevel.PERSISTENT, "getXAxis", "setXAxis", null),
        new Property("yAxis", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getYAxis", "setYAxis", null),
    };
    
    public DasPlotBeanInfo() {
        super(properties, edu.uiowa.physics.pw.das.graph.DasPlot.class);
    }
    
    public java.beans.BeanInfo[] getAdditionalBeanInfo() {
        BeanInfo[] additional = {
            new DasCanvasComponentBeanInfo()
        };
        return additional;
    }
    
}
