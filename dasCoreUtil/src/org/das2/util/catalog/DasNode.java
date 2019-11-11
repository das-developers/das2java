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


/** A single node from the das2 federated catalog
 *
 * @author cwp
 */
public interface DasNode
{
	/** Get the node type.
	 * @return A string representing the node type
	 */
	public String type();
	
	/** get the node name
	 * @return  The human readable name of the node, not it's path ID*/
	public String name();
	
	/** get the node path
	 * @return  The catalog path to this node.  For root nodes this is null */
	public String path();
	
	/** A summary of the node
	 * @return A short string describing the node, not an info dump of the contents */
	@Override
	public String toString();
	
	/** Is this object a detached root of a catalog tree.
	 * Note that the build in root URLs are always detached roots because there is no
	 * higher node to find.  A detached source or info node can still be a root node 
	 * <b>without</b> also being a directory.
	 * 
	 * @return true if no higher node is reachable from this one.
	 */
	public boolean isRoot();
	
	/** Can this catalog node provide data
	 * @return true if this node describes one or more data sources
	 */
	public boolean isSrc();
	
	/** Can this catalog node have the sub-nodes?
	 * @return  true if this node can have child nodes, not that it necessarily
	 *          contains any. */
	public boolean isDir();
	
	/** Is this object an information node.
	 * @return true if this node in the catalog provides a description of a mission, 
	 *         spacecraft, instrument, person or any other item category
	 */
	public boolean isInfo();

	/** Return the highest node reachable by this catalog node.  
	 * @return the highest node reachable by this catalog node, which may just be itself.
	 */
	public DasNode getRoot();
	
}
