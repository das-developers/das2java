package org.das2.util.catalog;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import org.das2.util.monitor.ProgressMonitor;

/**  All nodes loadable by the DasNodeFactory should inherit from this package private
 * abstract class.  It supports the general 2-phase construction.  The information in
 * a catalog directory listing is sufficient to provide for basic construction.  
 * Afterwards if more information is needed the node's load() member can be called to
 * finish the job.
 *
 * @author cwp
 */
abstract class DasAbstractNode implements DasNode {
	
	protected String sName; //
	protected DasDirNode parent;
	
	class NodeDefLoc{
		String sUrl;      /* The simple URL such as http://das2.org/catalog/das/uiowa.json */
		boolean bLoaded;  /* If true I've pulled info from this location */
		boolean bBad;     /* This location is known not to load */
		NodeDefLoc(String _sUrl){
			bLoaded = false;
			bBad = false;
			sUrl = _sUrl;
		}
	}
	
	protected List<NodeDefLoc> lLocs;
	
	
	/** Phase 1 construction for a node.  The information in a catalog listing is
	 * sufficient to construct a node.  
	 * 
	 * @param parent The parent method if any.  For root nodes this is null.
	 * @param name   The human readable name of this object within it's parent.  May be
	 *               null.  And the object may change it's name upon load!
	 * @param locations A list of URLs from which the full definition of this item may
	 *               be loaded for phase-2 construction, this should *NOT* be null.
	 */
	DasAbstractNode(DasDirNode parent, String name, List<String> locations)
	{
		this.parent = parent;
		sName = name;
		lLocs = new ArrayList<NodeDefLoc>();
		if(locations != null){
			for(String sLoc: locations){
				lLocs.add(new NodeDefLoc(sLoc));
			}
		}
	}
	
	@Override
	public String name() {return sName; }
	
	// Reverse resolution, ask my parent for my full name
	@Override
	public String path() {
		if(parent != null) return parent.childPath(this);
		return null;
	}
	
	@Override
	public String toString(){
		if(isRoot()) return type() + " @ (detached)";
		else return type() + " @ " + parent.childPath(this);
	}
	
	/** Append another location onto the stack of locations that my define this source */
	void addLocation(String sUrl){
		lLocs.add(new NodeDefLoc(sUrl));
	}
	
	/** Get the root node list as a string with some separator and prefix
	 * @param sPre A prefix to place before each root node URL, may be null
	 * @param sSep The separator to use between each root node URL, may be null
	 * @return A formatted string containing a list of all root node URLs
	 */
	public String prettyPrintLoc(String sPre, String sSep){
		// TODO: move to supra node data
		
		StringBuilder bldr = new StringBuilder();
		for(int i = 0; i < lLocs.size(); i++){
			if((i > 0)&&(sSep!=null)) bldr.append(sSep);  // prefix sep if needed
			if(sPre != null) bldr.append(sPre);
			bldr.append(lLocs.get(i));
		}
		return bldr.toString();
	}
	
	/** Does this node have a full definition
	 * @return True if this node successfully passed the second construction stage.
	 */
	abstract boolean isLoaded();
	
	/** Phase 2 construction for the node.  Actually get the full definition from
	 * any remote location.  This will trigger a re-load if the node is already 
	 * loaded.
	 * @param mon A human amusement device incase network operations are taking a while.
	 */
	abstract void load(ProgressMonitor mon) throws ResolutionException;
	
	/** If there is another source of information for this node and you might be able
	 * to load it, return true.  This default version just looks to see if any of the
	 * source URLs haven't been loaded or haven't been marked as bad.  You may want to
	 * override it for leaf nodes.
	 */
	boolean canMerge(){
		for(NodeDefLoc loc: lLocs){
			if(! loc.bLoaded && !loc.bBad) return true;
		}
		return false;
	}
	
	/** Potential Phase 3 construction, merge information with another definition. 
	 * @return true if more information was added, false otherwise.
	 */
	abstract boolean merge(ProgressMonitor mon);
	
	// See definition in DasNode interface
	@Override
	public boolean isRoot(){ return (parent == null); }

	// See definition in DasNode interface
	@Override
	public DasNode getRoot()
	{
		if(parent == null) return this;
		return parent.getRoot();
	}
	
	// Side loading data
	abstract boolean parse(String sData, String sUrl) throws ParseException;
}
