/* File: FormWindowBeanInfo.java
 * Copyright (C) 2002-2003 University of Iowa
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

package edu.uiowa.physics.pw.das.dasml;

import edu.uiowa.physics.pw.das.beans.AccessLevelBeanInfo;

import java.beans.MethodDescriptor;

/**
 *
 * @author  eew
 */
public class FormWindowBeanInfo extends AccessLevelBeanInfo {
    
    private static Property[] properties = {
        new Property("name", AccessLevel.ALL, "getDasName", "setDasName", null),
        new Property("visible", AccessLevel.DASML, "isWindowVisible", "setWindowVisible", null),
        new Property("title", AccessLevel.ALL, "getTitle", "setTitle", null)
    };
    
    private static MethodDescriptor[] methods;
    
    static {
        methods = new MethodDescriptor[1];
        try {
            methods[0] = new MethodDescriptor(FormWindow.class.getMethod("pack", null));
        }
        catch (NoSuchMethodException nsme) {
            IllegalStateException ise = new IllegalStateException(nsme.getMessage());
            ise.initCause(nsme);
            throw ise;
        }
        
    }
    
    public FormWindowBeanInfo() {
        super(properties, FormWindow.class);
    }
    
    public MethodDescriptor[] getMethodDescriptors() {
        return methods;
    }
    
}
