/* File: SymbolLineRendererBeanInfo.java
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
import org.das2.components.propertyeditor.EnumerationEditor;

public class SymbolLineRendererBeanInfo extends AccessLevelBeanInfo {
    
    private static final Property[] properties = {
        new Property("id", AccessLevel.DASML, "getId", null, null ),
        new Property("psym", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getPsym", "setPsym", EnumerationEditor.class),
        new Property("psymConnector", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getPsymConnector", "setPsymConnector", EnumerationEditor.class),
        new Property("histogram", AccessLevel.DASML, "isHistogram", "setHistogram", null),
        new Property("color", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getColor", "setColor", null),
        new Property("lineWidth", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getLineWidth", "setLineWidth", null),
        new Property("symSize", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getSymSize", "setSymSize", null),
        new Property("antiAliased", AccessLevel.DASML, "isAntiAliased", "setAntiAliased", null),
    };
    
    public SymbolLineRendererBeanInfo() {
        super(properties, org.das2.graph.SymbolLineRenderer.class);
    }
    
    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        BeanInfo[] additional = {
            new RendererBeanInfo()
        };
        return additional;
    }
    
}
