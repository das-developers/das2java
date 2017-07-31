/* File: DasAxisBeanInfo.java
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

import java.beans.BeanInfo;

/**
 * BeanInfo class for org.das2.graph.DasAxis.
 *
 * @author Edward West
 * @see org.das2.graph.DasAxis
 */
public class DasAxisBeanInfo extends AccessLevelBeanInfo {
    
    private static final Property[] properties = {
        new Property("datumRange", AccessLevel.END_USER, "getDatumRange", "setDatumRange", null ),
        new Property("dataMaximum", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getDataMaximum", "setDataMaximum", null),
        new Property("dataMinimum", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getDataMinimum", "setDataMinimum", null),
        new Property("flipped", AccessLevel.DASML, "isFlipped", "setFlipped", null),
        new Property("label", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getLabel", "setLabel", null),
        new Property("log", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "isLog", "setLog", null),
        new Property("foreground", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getForeground", "setForeground", null),
        new Property("units", AccessLevel.DASML, "getUnits", null, null),
        new Property("format", AccessLevel.DASML, "getFormat", "setFormat", null),
        new Property("tickLabelsVisible", AccessLevel.DASML, "isTickLabelsVisible", "setTickLabelsVisible", null),
        new Property("tickLength",AccessLevel.DASML, "getTickLength", "setTickLength", null),
        new Property("oppositeAxisVisible", AccessLevel.DASML, "isOppositeAxisVisible", "setOppositeAxisVisible", null),
        new Property("useDomainDivider", AccessLevel.DASML, "isUseDomainDivider", "setUseDomainDivider", null),
        new Property("flipLabel", AccessLevel.DASML, "isFlipLabel", "setFlipLabel", null),
        new Property("animated", AccessLevel.DASML, "isAnimated", "setAnimated", null),
        new Property("dataPath", AccessLevel.DASML, "getDataPath", "setDataPath", null),
        new Property("showTca", AccessLevel.DASML, "getDrawTca", "setDrawTca", null),
        new Property("scanRange", AccessLevel.DASML, "getScanRange", "setScanRange", null),
    };
    
    public DasAxisBeanInfo() {
        super(properties, org.das2.graph.DasAxis.class);
    }
    
    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        java.beans.BeanInfo[] additional = {
            new DasCanvasComponentBeanInfo()
        };
        return additional ;
    }
}
