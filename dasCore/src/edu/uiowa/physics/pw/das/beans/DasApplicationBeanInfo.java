/* File: RendererBeanInfo.java
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

import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.beans.AccessLevelBeanInfo.AccessLevel;
import edu.uiowa.physics.pw.das.beans.AccessLevelBeanInfo.Property;



public class DasApplicationBeanInfo extends AccessLevelBeanInfo {
    DasApplication app;
    
    private static final Property[] properties = {
        new Property("reloadLoggingProperties", AccessLevel.DASML, "isReloadLoggingProperties", "setReloadLoggingProperties", null),
        new Property("headless", AccessLevel.DASML, "isHeadless", null, null),
        new Property("applet", AccessLevel.DASML, "isApplet", null, null),
        new Property("dataSetCache", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getDataSetCache", null, null),                        
        new Property("inputStreamMeter", AccessLevel.DASML, "getInputStreamMeter", null, null),                        
        new Property("monitorManager", AccessLevel.DASML, "getMonitorFactory", null, null),                        
        new Property("das2Version", AccessLevel.DASML, "getDas2Version", null, null),        
    };
    
    public DasApplicationBeanInfo() {
        super(properties, edu.uiowa.physics.pw.das.DasApplication.class);
    }
    
}
