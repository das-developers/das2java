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
	
	/** Walk down a give sub-path and retrieve a fully constructed descendant node.
	 *
	 * @param sPath A sub-path to resolve into a fully loaded node.  If this node is a 
	 *        root catalog, then the sub-path is the complete path.
	 * @param mon A progress monitor since sub-node lookup can involve network operations
	 * @return The full constructed child node object, or null if no such fully loadable
	 *         sub object exits.
	 * @throws org.das2.util.catalog.ResolutionException If the given sub-path could not
	 *         be resolved by the catalog.
	 */
	public DasNode resolve(String sPath, ProgressMonitor mon)
		throws ResolutionException;
	
}
