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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//import java.util.NoSuchElementException;  <-- important one to remember
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;

/** All nodes which have child catalog nodes should inherit from this package
 * private class.
 * 
 * In addition to the 3-phase construction interface, this abstract class adds the
 * ability to have sub-nodes, which is provided via the resolve() and nearest() 
 * functions.
 * 
 * The two main data members intended for inheritance are dSubNodes and sSep.
 * If derived classes use this sub-node list and fill in their separator string
 * the sub-node resolution can be handled by this class with out overriding 
 * resolve() and nearest().
 * 
 * @author cwp
 */
abstract class AbstractDirNode extends AbstractNode implements DasDirNode
{
	private static final Logger LOGGER = LoggerManager.getLogger( "das2.catalog.absdir" );
	
	static final String DEFAULT_PATH_SEP = "/";
	
	// A dictionary of subnodes 
	protected Map<String, AbstractNode> dSubNodes;
	
	/** The separator string used for sub-paths of this node, may be set to null 
	 * or some other value by some nodes, defaults to "/".
	 */
	protected String sSep;
	
	AbstractDirNode(DasDirNode parent, String name, List<String> lUrls)
	{
		super(parent, name, lUrls);
		dSubNodes = Collections.synchronizedMap(new HashMap<>());
		sSep = DEFAULT_PATH_SEP;
	}
	
	// WARNING: This is called as part of child's toString, don't use toString here
	@Override
	public String childPath(DasNode child){
		String sPath = "";
		if(parent != null) sPath = parent.childPath(this);
		if(sSep != null) sPath += sSep;
		
		for(String sId: dSubNodes.keySet()){
			DasNode node = dSubNodes.get(sId);
			if(node == child){
				sPath += sId;
				return sPath;
			}
		}
		throw new IllegalArgumentException(
			"Object "+child.name()+", is not a member of "+toString()
		);
	}
	
	@Override
	public String[] list() {
		String[] aKeys = dSubNodes.keySet().toArray(new String[0]);
		Arrays.sort(aKeys);
		return aKeys;
	}
	
	@Override
	public DasNode resolve(String sSubPath, ProgressMonitor mon) throws DasResolveException {
		if(!isLoaded()) load(mon);
		
		if(sSubPath == null) return this;
		
		// Remove my seperator from the name then try to find it
		if((sSep != null) && sSep.length() > 0){ 
			if(sSubPath.startsWith(sSep)) sSubPath = sSubPath.substring(sSep.length());
		}
		
		// Get longest match from the list of child names
		String[] aSubs = list();
		String sChild = null;
		for(String sCheck: aSubs){
			if(sSubPath.startsWith(sCheck)){
				if(sChild == null) sChild = sCheck;
				else if(sCheck.length() > sChild.length()) sChild = sCheck;
			}
		}
		
		// If I couldn't find this name in my child list, see if an alternate copy of 
		// myself has it.  We're doing this because it is possible than a remote 
		// replica of a catalog node isn't exactly in sync with the source.  So it's
		// possible that another branch of the catalog actually has this sub item.  Let's
		// give it a shot before giving up.
		if(sChild == null){
			while(canMerge()){  // This node might have extra information in another copy
				if(merge(mon))   // try to merge
					return resolve(sSubPath, mon); // it worked, try to resolve again
			}
		}
		
		if(sChild == null){
			throw new DasResolveException("Cannot resolve (sub)path", sSubPath);
		}
		
		AbstractNode child = dSubNodes.get(sChild);
		
		if(!child.isLoaded()){
			Exception cause = null;
			do{
				try{
					child.load(mon);
					break;
				}
				catch(DasResolveException ex){
					LOGGER.log(Level.FINE, "load failure for child node {0}", ex.getMessage());
					cause = ex;
				}
			} while(canMerge());
			
			// If the node is not loaded now, we have a problem.
			if(!child.isLoaded()){
				throw new DasResolveException("Couldn't load", cause, sSubPath);
			}
		}
		
		// Take the child portion off the string, if it's non-zero length, and the 
		// child is a directory object, resolution should continue
		String sSubSubPath = sSubPath.substring(sChild.length());
		if(sSubSubPath.length() > 0){
			if(child.isDir()){
				AbstractDirNode childDir = (AbstractDirNode)child;
				return childDir.resolve(sSubSubPath, mon);
			}
			throw new DasResolveException(
				"Sub node "+sChild+" is not a directory", sSubSubPath
			);
		}
		
		return child;
	}
	
	// Similar to resolve, but expects to fail at some point and just returns the deepest
	// node that resolved
	@Override
	public DasNode nearest(String sSubPath, ProgressMonitor mon) 
	{
		if(!isLoaded()){
			try{
				load(mon);
			} catch(DasResolveException ex){
				LOGGER.log(Level.INFO, "Couldn't resolve {0} using sources {1}", 
					new Object[]{sSubPath, prettyPrintLoc(null, " ")}
				);
				if(parent != null) return parent;
				else return this;
			}
		}
		
		if(sSubPath == null) return this;
		
		// Remove my seperator from the name then try to find it
		if((sSep != null) && sSep.length() > 0){ 
			if(sSubPath.startsWith(sSep)) sSubPath = sSubPath.substring(sSep.length());
		}
		
		// Get longest match from the list of child names
		String[] aSubs = list();
		String sChild = null;
		for(String sCheck: aSubs){
			if(sSubPath.startsWith(sCheck)){
				if(sChild == null) sChild = sCheck;
				else if(sCheck.length() > sChild.length()) sChild = sCheck;
			}
		}
		
		// If I couldn't find this name in my child list, see if an alternate copy of 
		// myself has it.  We're doing this because it is possible than a remote 
		// replica of a catalog node isn't exactly in sync with the source.  So it's
		// possible that another branch of the catalog actually has this sub item.  Let's
		// give it a shot before giving up.
		if(sChild == null){
			while(canMerge()){  // This node might have extra information in another copy
				if(merge(mon))   // try to merge
					return nearest(sSubPath, mon); // it worked, try to resolve again
			}
		}
		
		if(sChild == null){
			LOGGER.log(Level.FINE, "Cannot resolve (sub)path {0}", sSubPath);
			return this;
		}
		
		AbstractNode node = dSubNodes.get(sChild);
		
		if(!node.isLoaded()){
			do{
				try{
					node.load(mon);
					break;
				}
				catch(DasResolveException ex){
					LOGGER.log(Level.FINE, "load failure for child node {0}", ex.getMessage());
				}
			} while(canMerge());
			
			// If the node is not loaded now, we have a problem.
			if(!node.isLoaded()){
				LOGGER.log(Level.FINE, "Couldn''t load {0}", sSubPath);
				return this;
			}
		}
		
		// Take the child portion off the string, if it's non-zero length, and the 
		// child is a directory object, resolution should continue
		String sSubSubPath = sSubPath.substring(sChild.length());
		if(sSubSubPath.length() > 0){
			if(node.isDir()){
				AbstractDirNode dirNode = (AbstractDirNode)node;
				return dirNode.nearest(sSubSubPath, mon);
			}
		}
		
		return this;
	}
	
	@Override
	public DasNode get(String sChildId){ 
		return dSubNodes.get(sChildId);
	}
}
