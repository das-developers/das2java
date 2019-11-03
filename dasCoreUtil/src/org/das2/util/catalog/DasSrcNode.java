package org.das2.util.catalog;

import java.util.Map;

/** Source nodes add extra functions to generic catalog nodes
 *
 * @author cwp
 */
public interface DasSrcNode extends DasNode {

	/** Determine if the given list of query parameters are valid
	 * 
	 * @param params A map of key, value query parameters
	 * @return True if this set of paramters is valid, false otherwise
	 */
	public boolean queryVerify(Map<String, String> dQuery);

	
}
