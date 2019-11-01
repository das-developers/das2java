/* Copyright (C) 2019 Chris Piker 
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
 * GNU General Public Library License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 
 * USA
 */
package org.das2.util.catalog;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;

/** A single node from the das2 federated catalog
 * This represents a catalog node.  It has a 2-phase construction sequence
 * A minimal version may be in memory representing just a referenced item from
 * a higher level container, or it may be fully realized by a catalog lookup.
 * 
 * Loading the full node is delayed until the functionality of a full definition
 * is needed.  For example a minimal catalog entry will load itself when a list
 * of sub items is called for.  A minimal data source entry will load itself
 * when a data request or interface defintion request occurs.
 * 
 * All Nodes have the following data members:
 *   path - The Path URI of this object.  This is a conceptual location, not a physical
 *          path.  Even nodes loaded from a local file can have a path URI.  A root node
 *          for a given tree has a 'null' path URI.  You can't navigate above a root node,
 *          but, depending on the node type, you can navigate down.
 * 
 *   type - The type of object, cannot be null
 * 
 *   name - The name of the object
 * 
 *   url  - Where the item was loaded from (if fully realized)
 * 
 *   locs - Where you can get the full item definition from if needed
 * 
 *   data - The string information read in to generate the node.  The actual
 *         format of this data is may be JSON, XML, or some other format.
 *
 * @author cwp
 */
public abstract class DasNode
{
	private static final Logger logger = org.das2.util.LoggerManager.getLogger(
		"das2.catalog.node" 
	);

	protected String sPath;  // My name from the root location
	protected String sName;  // My human readable name
	protected String sSrcUrl = null;  // Where I came from (if loaded)
	protected Map<String, String> dLocs = new HashMap<>();  // Where I can be loaded from
	
	protected enum content_fmt {
		JSON, XML
	}
	protected Object oDef = null; // A pointer to the content
	
	
	/** Create a root catalog node, possibly using the federated catalog 
	 * s
	 * @param sPath - The Catalog path name for this node
	 * @param sURI - Make a root node using this particular resource URL
	 * @param mon
	 * @return 
	 */
	public static DasNode rootNode(String sPath, String sURI, ProgressMonitor mon)
	{
		
		return null;
	}
	
	
	public DasNode(String sURL){
		
	}
	
	
	/** Get the catalog node type.
	 * @return A string representing the node type
	 */
	public abstract String type();
	
	/** Convert a stub node into a full node.
	 * 
	 * This method does nothing if called on a fully loaded node.  Internally the
	 * list possible locations (dLocs) is used to load the full definition.
	 * If an error is encountered on the first URL, subsequent URLs are attempted
	 * until the list is exhausted or a the node loads correctly.
	 */
	protected void load(){
		
		
		
	}
	
	
}
