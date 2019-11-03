package org.das2.util.catalog;

import org.das2.util.monitor.ProgressMonitor;

/**  All nodes loadable by the DasNodeFactory should inherit from this abstract class.
 * Since java interfaces cannot define package private members, I have to resort to an
 * abstract class in order to have the type system enforce the fact that all nodes must
 * provide a package private load() memeber.
 *
 * @author cwp
 */
public abstract class DasAbstractNode implements DasNode {
	
	protected DasDirNode parent = null;
	
	public class LoadResult{
		boolean bSuccess = true;     // The result of loading the object.
		String sFailure = null;      // Any error messages about why the load failed
		Throwable exFailure = null;  // An exception that cause the loading to fail
	}
	
	/** Using the provided string definition, convert a stub node into a full node. 
	 * All concrete nodes must have this.
	 */
	abstract LoadResult load(String sUrl, ProgressMonitor mon);
	
}
