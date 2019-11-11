package org.das2.util.catalog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;

/** Add the child node list functionality on top of an Abstract Leaf Node
 *  Package private to avoid leaking implementation details
 * 
 * @author cwp
 */
abstract class DasAbstractDirNode extends DasAbstractNode implements DasDirNode
{
	private static final Logger LOGGER = LoggerManager.getLogger( "das2.catalog" );
	
	protected Map<String, DasAbstractNode> dSubNodes;
	
	// The separator string used for sub-paths of this node, may be null.
	protected String sSep;
	
	public DasAbstractDirNode(DasDirNode parent, String name, List<String> locations)
	{
		super(parent, name, locations);
		dSubNodes = new HashMap<>();
	}
	
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
			"Object "+child.toString()+", is not a member of "+toString()
		);
	}
	
	@Override
	public String[] list() {
		String[] aKeys = dSubNodes.keySet().toArray(new String[0]);
		Arrays.sort(aKeys);
		return aKeys;
	}
	
	@Override
	public DasNode resolve(String sPath, ProgressMonitor mon) throws ResolutionException {
		if(!isLoaded()) load(mon);
		
		if(sPath == null) return this;
		
		// Remove my seperator from the name then try to find it
		if((sSep != null) && sSep.length() > 0){ 
			if(sPath.startsWith(sSep)) sPath = sPath.substring(sSep.length());
		}
		
		// Get longest match from the list of child names
		String[] aSubs = list();
		String sChild = null;
		for(String sCheck: aSubs){
			if(sPath.startsWith(sCheck)){
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
					return resolve(sPath, mon); // it worked, try to resolve again
			}
		}
		
		if(sChild == null){
			throw new ResolutionException("Cannot resolve (sub)path", sPath);
		}
		
		DasAbstractNode node = dSubNodes.get(sChild);
		
		if(!node.isLoaded()){
			Exception cause = null;
			do{
				try{
					node.load(mon);
					break;
				}
				catch(ResolutionException ex){
					LOGGER.log(Level.FINE, "load failure for child node {0}", ex.getMessage());
					cause = ex;
				}
			} while(canMerge());
			
			// If the node is not loaded now, we have a problem.
			if(!node.isLoaded()){
				throw new ResolutionException("Couldn't load", cause, sPath);
			}
		}
		
		// Take the child portion off the string, if it's non-zero length, and the 
		// child is a directory object, resolution should continue
		String sSubPath = sPath.substring(sChild.length());
		if(sSubPath.length() > 0){
			if(node.isDir()){
				DasAbstractDirNode dirNode = (DasAbstractDirNode)node;
				return dirNode.resolve(sSubPath, mon);
			}
			throw new ResolutionException("Sub node "+sChild+" is not a directory", sSubPath);
		}
		
		return node;
	}
	
	// Similar to resolve, but expects to fail at some point and just returns the deepest
	// node that resolved
	@Override
	public DasNode nearest(String sPath, ProgressMonitor mon) 
	{
		if(!isLoaded()){
			try{
				load(mon);
			} catch(ResolutionException ex){
				LOGGER.log(Level.INFO, "Couldn't resolve {0} using sources {1}", 
					new Object[]{sPath, prettyPrintLoc(null, " ")}
				);
				if(parent != null) return parent;
				else return this;
			}
		}
		
		if(sPath == null) return this;
		
		// Remove my seperator from the name then try to find it
		if((sSep != null) && sSep.length() > 0){ 
			if(sPath.startsWith(sSep)) sPath = sPath.substring(sSep.length());
		}
		
		// Get longest match from the list of child names
		String[] aSubs = list();
		String sChild = null;
		for(String sCheck: aSubs){
			if(sPath.startsWith(sCheck)){
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
					return nearest(sPath, mon); // it worked, try to resolve again
			}
		}
		
		if(sChild == null){
			LOGGER.log(Level.FINE, "Cannot resolve (sub)path {0}", sPath);
			return this;
		}
		
		DasAbstractNode node = dSubNodes.get(sChild);
		
		if(!node.isLoaded()){
			do{
				try{
					node.load(mon);
					break;
				}
				catch(ResolutionException ex){
					LOGGER.log(Level.FINE, "load failure for child node {0}", ex.getMessage());
				}
			} while(canMerge());
			
			// If the node is not loaded now, we have a problem.
			if(!node.isLoaded()){
				LOGGER.log(Level.FINE, "Couldn''t load {0}", sPath);
				return this;
			}
		}
		
		// Take the child portion off the string, if it's non-zero length, and the 
		// child is a directory object, resolution should continue
		String sSubPath = sPath.substring(sChild.length());
		if(sSubPath.length() > 0){
			if(node.isDir()){
				DasAbstractDirNode dirNode = (DasAbstractDirNode)node;
				return dirNode.nearest(sSubPath, mon);
			}
		}
		
		return this;
	}
}
