/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.util.catalog;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** This represents a catalog node.  It has a 2-phase construction sequence
 * A minimal version may reside in memory representing just a referenced item
 * from a higher level container, or it may be fully realized by a catalog lookup.
 * 
 * Loading the full node is delayed until the functionality of a full definition
 * is needed.  For example a minimal catalog entry will load itself when a list
 * of sub items is called for.  A minimal data source entry will load itself
 * when a data request or interface definition request occurs.
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
public class DasDirNodeCat extends DasAbstractDirNode
{
	private static final Logger LOGGER = org.das2.util.LoggerManager.getLogger(
		"das2.catalog.node" 
	);
	
	JSONObject json = null;
	public static final String TYPE = "Catalog";
	
	// Static names for important json dictionary keys
	static final String KEY_CATALOG = "catalog";
	static final String KEY_TYPE    = "type";
	static final String KEY_NAME    = "name";
	static final String KEY_URLS    = "urls";

	// Phase 1 construction, just let super-class handle it
	public DasDirNodeCat(DasDirNode parent, String name, List<String> locations)
	{
		super(parent, name, locations);
	}
	
	@Override
	public String type() { return TYPE; }

	@Override
	public boolean isSrc() { return false; }
	
	@Override
	public boolean isDir(){ return true; }
	
	@Override
	public boolean isInfo(){ return false; }
	
	@Override
	boolean isLoaded(){ return (json != null); }

	
	protected void initFromJson(JSONObject jo) throws JSONException, ParseException{
		json = jo;
		if(json.has(KEY_CATALOG)){
			JSONObject cat = json.getJSONObject(KEY_CATALOG);
			Iterator<String> keys = cat.sortedKeys();
			while(keys.hasNext()){
				String sChildId = keys.next();
				JSONObject joChild = cat.getJSONObject(sChildId);
				
				String sChildType = joChild.getString(KEY_TYPE);  // Can't be null				
				String sChildName = joChild.optString(KEY_NAME, null);
				JSONArray jaLocs = joChild.optJSONArray(KEY_URLS);
				List<String> lChildLocs = null;
				if(jaLocs != null){
					lChildLocs = new ArrayList<>();
					for(int i = 0; i < jaLocs.length(); ++i){
						lChildLocs.add(jaLocs.getString(i));
					}
				}
				// Make the right kind of child
				DasAbstractNode child = DasNodeFactory.newNode(
					sChildType, this, sChildName, lChildLocs
				);
				
				String sCheck = child.toString();
				
				dSubNodes.put(sChildId, child);
			}
		}
	}
	
	protected void mergeFromJson(JSONObject jo){
		throw new UnsupportedOperationException("Not supported yet.");
	}
	

	@Override
	void load(ProgressMonitor mon) throws ResolutionException {
		for(NodeDefLoc loc: lLocs){
			loc.bLoaded = false;
			loc.bBad = false;
		}
		
		for(int i = 0; i < lLocs.size(); i++){
			NodeDefLoc loc = lLocs.get(i);
			try{
				String sData = DasNodeFactory.getUtf8NodeDef(loc.sUrl, mon);
				JSONObject jo = new JSONObject(sData);
				
				String sVal = jo.getString(KEY_TYPE);
				
				// Using exceptions for flow control... not good.
				if(!sVal.equals(TYPE))
					throw new ResolutionException("Expected type '"+TYPE+"' not '"+sVal+"'", loc.sUrl);
				
				initFromJson(jo);
				loc.bLoaded = true;
				return;
				
			} catch(IOException | JSONException | ParseException | ResolutionException ex){
				loc.bBad = true;
				LOGGER.log(Level.FINE, 
					"Catalog location {0} marked as bad because {1}", 
					new Object[]{loc.sUrl, ex.getMessage()}
				);
				//If this was our last chance, go ahead and raise the exception
				if((i + 1) == lLocs.size()){
					ResolutionException resEx = new ResolutionException(
						"Couldn't load catalog node because "+ex.getMessage(),
						ex, loc.sUrl
					);
					throw resEx;
				}
			}
		} 
	}

	@Override
	boolean merge(ProgressMonitor mon)
	{
		for(NodeDefLoc loc: lLocs){
			if(loc.bLoaded || loc.bBad) continue;
			
			try{
				String sData = DasNodeFactory.getUtf8NodeDef(loc.sUrl, mon);
				JSONObject jo = new JSONObject(sData);
				String sVal = jo.getString(KEY_TYPE);
				
				if(!sVal.equals(TYPE))
					throw new ResolutionException("Expected type '"+TYPE+"' not '"+sVal+"'", loc.sUrl	);
				
				mergeFromJson(jo);
				loc.bLoaded = true;
				return true;
				
			} catch(IOException | JSONException | ResolutionException ex){
				loc.bBad = true;
				LOGGER.log(Level.FINE, 
					"Catalog location {0} marked as bad because {1}", 
					new Object[]{loc.sUrl, ex.getMessage()}
				);
			}
		}
		
		return false;
	}

	@Override
	public DasNode get(String sChildId)
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	boolean parse(String sData, String sUrl) throws ParseException
	{
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
