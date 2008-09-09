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

package edu.uiowa.physics.pw.das.graph;

import org.das2.beans.DasCanvasComponentBeanInfo;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;
import org.das2.components.propertyeditor.EnumerationEditor;

/**
 * BeanInfo class for DasColorBar
 *
 * @author Edward West
 */
public class DasAnnotationBeanInfo extends SimpleBeanInfo {
    
    private static PropertyDescriptor[] properties;
    static {
        try {
            properties = new PropertyDescriptor[] {
                new PropertyDescriptor("text", DasAnnotation.class),
                new PropertyDescriptor("borderType", DasAnnotation.class),
                new PropertyDescriptor("arrowStyle", DasAnnotation.class ),
                new PropertyDescriptor("fontSize", DasAnnotation.class ),
            };
            properties[1].setPropertyEditorClass( EnumerationEditor.class );
            properties[2].setPropertyEditorClass( EnumerationEditor.class );
            System.err.println("yeah!!!");
        } catch ( IntrospectionException e) {
            e.printStackTrace();
            properties= null;
        }

    }
    

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return properties;
    }
    
    @Override
    public BeanInfo[] getAdditionalBeanInfo() {
        BeanInfo[] additional = {
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
