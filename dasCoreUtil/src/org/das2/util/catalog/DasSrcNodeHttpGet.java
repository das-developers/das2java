
package org.das2.util.catalog;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONObject;

/**
 *
 * @author cwp
 */
public class DasSrcNodeHttpGet extends DasAbstractNode implements DasSrcNode
{
	private static final Logger LOGGER = org.das2.util.LoggerManager.getLogger(
		"das2.catalog" 
	);

	JSONObject json = null;
	public static final String TYPE = "HttpStreamSrc";
	
	protected String sPath;  // My name from the root location
	protected String sName;  // My human readable name
	protected String sSrcUrl = null;  // Where I came from (if loaded)
	protected Map<String, String> dLocs = new HashMap<>();  // Where I can be loaded from

	public DasSrcNodeHttpGet(DasDirNode parent, String name, List<String> locations)
	{
		super(parent, name, locations);
	}

	@Override
	public String type() { return TYPE;}

	@Override
	public boolean isSrc() { return true; }
	
	@Override
	public boolean isDir(){ return false; }
	
	@Override
	public boolean isInfo(){ return false; }

	@Override
	boolean parse(String sData, String sUrl) throws ParseException
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	
	@Override
	public boolean queryVerify(Map<String, String> dQuery) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public String name() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	boolean isLoaded()
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	void load(ProgressMonitor mon)
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	boolean canMerge()
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	boolean merge(ProgressMonitor mon)
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	protected enum content_fmt {
		JSON, XML
	}
	protected Object oDef = null; // A pointer to the content

}
