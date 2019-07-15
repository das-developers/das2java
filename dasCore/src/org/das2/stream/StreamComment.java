/* File: StreamException.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 11, 2004, 11:03 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
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

package org.das2.stream;

import org.w3c.dom.*;

/**
 *
 * @author  jbf
 */
public class StreamComment {
    
    Element element;
    
    public static final String TYPE_TASK_SIZE="taskSize";
    public static final String TYPE_TASK_PROGRESS="taskProgress";
    public static final String TYPE_LOG="log:(.*)";
    
    public StreamComment( Element element ) {
        this.element= element;
    }
        
    public String getType() { return element.getAttribute("type"); }
    public String getValue() { return element.getAttribute("value"); }
    
    @Override
    public String toString() { 
        return "stream comment: "+getType()+"="+getValue();
    }
}
