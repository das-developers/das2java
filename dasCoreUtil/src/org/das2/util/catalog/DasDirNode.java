package org.das2.util.catalog;

/** All directory node objects can have children
 *
 * @author cwp
 */
public interface DasDirNode extends DasNode {
	
	/** List child nodes of this directory type item
	 * 
	 * @return A list of child nodes.  Return values may be used in the get node function
	 *         below.
	 */
	public String[] list();
	
	/** Get a decendent node given a string
	 * 
	 * @param sName
	 * @return The child node object, or null if no such sub object exits
	 */
	public DasNode resolve(String sName);
	
}
