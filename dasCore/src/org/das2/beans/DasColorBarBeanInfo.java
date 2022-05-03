/* File: DasColorBarBeanInfo.java
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

import org.das2.beans.AccessLevelBeanInfo.AccessLevel;
import org.das2.beans.AccessLevelBeanInfo.Property;
import java.beans.BeanInfo;
import org.das2.components.propertyeditor.EnumerationEditor;

/**
 * BeanInfo class for DasColorBar
 *
 * @author Edward West
 */
public class DasColorBarBeanInfo extends AccessLevelBeanInfo {
    
    private static final Property[] properties = {
        new Property("type", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getType", "setType", EnumerationEditor.class),
        new Property("fillColor", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getFillColor", "setFillColor", null ),
        new Property("specialColors",AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getSpecialColors", "setSpecialColors", null )
    };
    
    public DasColorBarBeanInfo() {
        super(properties, org.das2.graph.DasColorBar.class);
    }
    
    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        BeanInfo[] additional = {
            new DasAxisBeanInfo(),
            new DasCanvasComponentBeanInfo(), 
        };
        return additional;
        
        /*try {
            BeanInfo[] additional = {
                Introspector.getBeanInfo( DasAxis.class ),
            };
            return additional;
        } catch ( IntrospectionException e ) {
            throw new RuntimeException(e);
        }*/
    }
    
}
