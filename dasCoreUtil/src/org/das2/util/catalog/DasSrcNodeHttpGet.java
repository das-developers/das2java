
package org.das2.util.catalog;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author cwp
 */
public class DasSrcNodeHttpGet extends DasAbstractNode implements DasSrcNode
{
	private static final Logger LOGGER = org.das2.util.LoggerManager.getLogger(
		"das2.catalog.node" 
	);

	protected String sPath;  // My name from the root location
	protected String sName;  // My human readable name
	protected String sSrcUrl = null;  // Where I came from (if loaded)
	protected Map<String, String> dLocs = new HashMap<>();  // Where I can be loaded from

	@Override
	public String type() { return "HttpGetSrc";}

	@Override
	public boolean isDataSource() { return true; }

	
	@Override
	public boolean queryVerify(Map<String, String> dQuery) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String name() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	LoadResult load(String sUrl, ProgressMonitor mon) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	protected enum content_fmt {
		JSON, XML
	}
	protected Object oDef = null; // A pointer to the content

}
