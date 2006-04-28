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

package edu.uiowa.physics.pw.das.beans;

import edu.uiowa.physics.pw.das.beans.AccessLevelBeanInfo.AccessLevel;
import edu.uiowa.physics.pw.das.beans.AccessLevelBeanInfo.Property;
import edu.uiowa.physics.pw.das.components.propertyeditor.EnumerationEditor;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;

/**
 * BeanInfo class for DasColorBar
 *
 * @author Edward West
 */
public class DasColorBarBeanInfo extends AccessLevelBeanInfo {
    
    protected static final Property[] properties = {
        new Property("type", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getType", "setType", EnumerationEditor.class)
    };
    
    public DasColorBarBeanInfo() {
        super(properties, edu.uiowa.physics.pw.das.graph.DasColorBar.class);
    }
    
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
