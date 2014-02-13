/* File: DasCanvasBeanInfo.java
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

import java.beans.MethodDescriptor;
import java.util.Arrays;

/**
 *
 * @author  eew
 */
public class DasCanvasBeanInfo extends AccessLevelBeanInfo {
    
    private static Property[] properties = {
        new Property("name", AccessLevel.ALL, PersistenceLevel.PERSISTENT, "getDasName", "setDasName", null),
        new Property("fitted", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "isFitted", "setFitted", null),
        new Property("width", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getPreferredWidth", "setPreferredWidth", null),
        new Property("height", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getPreferredHeight", "setPreferredHeight", null),
        new Property("backgroundColor", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getBackground", "setBackground", null), 
        new Property("foregroundColor", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getForeground", "setForeground", null), 
        new Property("baseFont", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getBaseFont", "setBaseFont", null),
        new Property("printTag", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getPrintingTag", "setPrintingTag", null ),
        new Property("textAntiAlias", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "isTextAntiAlias", "setTextAntiAlias", null ),
        new Property("antiAlias", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "isAntiAlias", "setAntiAlias", null ),
        new Property("components", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getCanvasComponents", null, "getCanvasComponents", null, null ), 
        new Property("application", AccessLevel.DASML, PersistenceLevel.PERSISTENT, "getApplication", null, null),
    };
    
    private static MethodDescriptor[] methods;
    static {
        try {
            methods = new MethodDescriptor[1];
            Class[] writeToPngParams = { String.class };
            methods[0] = new MethodDescriptor(org.das2.graph.DasCanvas.class.getMethod("writeToPng", writeToPngParams));
        }
        catch (NoSuchMethodException nsme) {
            IllegalStateException ise = new IllegalStateException(nsme.getMessage());
            ise.initCause(nsme);
            throw ise;
        }
    }
    
    public DasCanvasBeanInfo() {
        super(properties, org.das2.graph.DasCanvas.class);
    }
    
    public MethodDescriptor[] getMethodDescriptors() {
        return Arrays.copyOf( methods,methods.length );
    }
    
}
