/* File: DefaultComponentDnDSupport.java
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

package org.das2.dasml;

/**
 *
 * @author  eew
 */
class DefaultComponentDnDSupport extends org.das2.util.DnDSupport {
    
    DefaultComponentDnDSupport(java.awt.Component c) {
        this(c, java.awt.dnd.DnDConstants.ACTION_NONE);
    }
    
    /** Creates a new instance of ComponentDnDSupport */
    DefaultComponentDnDSupport(java.awt.Component c, int action) {
        super(c, action, null);
    }
    
    protected int canAccept(java.awt.datatransfer.DataFlavor[] flavors, int x, int y, int action) {
        return -1;
    }
    
    protected void done() {
    }
    
    protected boolean importData(java.awt.datatransfer.Transferable t, int x, int y, int action) {
        return false;
    }
    
    protected java.awt.datatransfer.Transferable getTransferable(int x, int y, int action) {
        return null;
    }
    
    protected void exportDone(java.awt.datatransfer.Transferable t, int action) {
    }
    
}
