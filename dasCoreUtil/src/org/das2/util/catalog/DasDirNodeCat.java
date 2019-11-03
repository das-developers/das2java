/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.catalog;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONException;
import org.json.JSONObject;

/** This represents a catalog node.  It has a 2-phase construction sequence
 * A minimal version may reside in memory representing just a referenced item
 * from a higher level container, or it may be fully realized by a catalog lookup.
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
 *         format of this data is may be JSON, XML, or some other format. * @author cwp
 */
public class DasDirNodeCat extends DasAbstractNode implements DasDirNode
{
	private static final Logger logger = org.das2.util.LoggerManager.getLogger(
		"das2.catalog.node" 
	);
	
	JSONObject json = null;
	public static final String TYPE_CATALOG = "Catalog";
	
	@Override
	public String type() { return TYPE_CATALOG; }

	@Override
	public boolean isDataSource() { return false; }

	@Override
	public String[] list() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public DasNode subNode(String sName) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
	@Override
	public String name() {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	LoadResult load(String sUrl, ProgressMonitor mon) {
		LoadResult res = new LoadResult();  // Defaults to success
		String sData;
		try {
			// This type of node expects a string data definition
			sData = DasNodeFactory.getUtf8NodeDef(sUrl, mon);
		} catch (IOException ex) {
			res.bSuccess = false;
			res.sFailure = "Couldn't download from "+sUrl;
			res.exFailure = ex;
			return res;
		}
		
		try {
			json = new JSONObject(sData);
		} catch (JSONException ex) {
			res.bSuccess = false;
			res.sFailure = "Text from "+sUrl+" was not a valid JSON file";
			res.exFailure = ex;
			return res;
		}
		
		// Okay, it should be valid JSON data, make sure it has the content we
		// need.
		try {
			String sVal = json.getString("type");
			if(!sVal.equals(TYPE_CATALOG)){
				throw new CatalogException(
					"Expected type '"+TYPE_CATALOG+"' not '"+sVal+"'", sUrl
				);
			}
		} 
		catch (JSONException|CatalogException ex) {
			res.bSuccess = false;
			res.sFailure = "Error in JSON object definition at "+sUrl;
			res.exFailure = ex;
			return res;
		}
		
		return res;
	}
	
}
