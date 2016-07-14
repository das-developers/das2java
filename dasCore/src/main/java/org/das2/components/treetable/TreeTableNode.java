/* File: TreeTableNode.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on January 28, 2004, 10:18 AM
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

package org.das2.components.treetable;

import javax.swing.tree.TreeNode;

/**
 *
 * @author  eew
 */
public interface TreeTableNode extends TreeNode {
    
    boolean isCellEditable(int columnIndex);
    Object getValueAt(int columnIndex);
    void setValueAt(Object value, int columnIndex);
    Class getColumnClass(int columnIndex);
    int getColumnCount();
    String getColumnName(int columnIndex);
}
