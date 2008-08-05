/* File: DasCanvasComponentBeanInfo.java
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

import edu.uiowa.physics.pw.das.graph.DasCanvasComponent;

import java.beans.MethodDescriptor;

public class DasCanvasComponentBeanInfo extends AccessLevelBeanInfo {
    
    private static Property[] properties = {
        new Property("name", AccessLevel.ALL, PersistenceLevel.PERSISTENT, "getDasName", "setDasName", null),
        new Property("row", AccessLevel.ALL, PersistenceLevel.PERSISTENT, "getRow", "setRow", null),
        new Property("column", AccessLevel.ALL, PersistenceLevel.PERSISTENT, "getColumn", "setColumn", null),
        new Property("mouseAdapter", AccessLevel.ALL, PersistenceLevel.PERSISTENT, "getDasMouseInputAdapter", "setDasMouseInputAdapter", null)
    };
    
    private static MethodDescriptor[] methods;
    static {
        try {
            methods = new MethodDescriptor[1];
            methods[0] = new MethodDescriptor(DasCanvasComponent.class.getMethod("update"));
        }
        catch (NoSuchMethodException nsme) {
            IllegalStateException ise = new IllegalStateException(nsme.getMessage());
            ise.initCause(nsme);
            throw ise;
        }
    }
    
    public DasCanvasComponentBeanInfo() {
        super(properties, edu.uiowa.physics.pw.das.graph.DasCanvasComponent.class);
    }
    
    public MethodDescriptor[] getMethodDescriptors() {
        return methods;
    }
    
}
