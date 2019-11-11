/* Copyright (C) 2019 Chris Piker 
 *
 * This file is part of the das2 Core library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public Library License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 
 * USA
 */

package org.das2.util.catalog;

import org.das2.util.monitor.ProgressMonitor;

/** All directory node objects can have children
 *
 * @author cwp
 */
public interface DasDirNode extends DasNode {
	
	/** List child nodes of this directory type item
	 * 
	 * @return A list of child node string ids.  Return values may be used in the get node
	 *         function below.
	 */
	public String[] list();
	
	/** Get a child node by it's ID.
	 * 
	 * @param sChildId
	 * @return The child node, or null if no child node has the id sChildId
	 */
	public DasNode get(String sChildId);
	
	/** Given a child object get the complete path to it
	 * @param child The child object for which the path is desired.  
	 * @return 
	 */
	public String childPath(DasNode child);
	
	/** Walk down a given sub-path and retrieve a fully constructed descendant node.
	 *
	 * @param sSubPath A sub-path to resolve into a fully loaded node.  If this node is a 
	 *        root catalog, then the sub-path is the complete path.
	 * @param mon A progress monitor since sub-node lookup can involve network operations
	 * @return The full constructed child node object.
	 * @throws org.das2.util.catalog.DasResolveException If the given sub-path could not
	 *         be resolved by the catalog.
	 */
	public DasNode resolve(String sSubPath, ProgressMonitor mon)
		throws DasResolveException;
	
	/** Walk down a given sub-path as far as possible and retrieve the closest descendant
	 * FIXME: Find a better name for this function, maybe resolveDeepest
	 * 
	 * @param sSubPath A sub-path to resolve into a fully loaded node.  If this node is a
	 *        root catalog, then the sub-path is the complete path.
	 * @param mon A progress monitor since sub-node lookup can involve network operations
	 * @return The deepest resolvable node which may just be "this", or even the "parent"
	 *         in cases where this node is a stub that can't even resolve itself.
	 */
	public DasNode nearest(String sSubPath, ProgressMonitor mon);
}
