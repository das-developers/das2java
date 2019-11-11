/* Copyright (C) 2019 Chris Piker 
 *
 * This file is part of the das2 Core library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public Library License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307 
 * USA
 */
package org.das2.util.catalog;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** These nodes represent a general directory of nodes.
 * 
 * No particlar sub items are expected, though the data themselves must be 
 * in a particular JSON format.  Catalog nodes can contain catalog nodes
 * to generate a hierachy.
 * 
 * FIXME: Add reference to Catalog node JSON format documentation or JSON schema
 * once it exists
 * 
 * @author cwp
 */
class CatalogNode extends AbstractDirNode
{
	private static final Logger LOGGER = LoggerManager.getLogger("das2.catalog.catalog" );
	
	JSONObject data;
	static final String TYPE = "Catalog";
	
	// Static names for important data dictionary keys
	static final String KEY_CATALOG = "catalog";
	static final String KEY_TYPE    = "type";
	static final String KEY_NAME    = "name";
	static final String KEY_URLS    = "urls";
	static final String KEY_SEPARATOR = "separator";
	static final String KEY_TITLE   = "title";
	static final String KEY_VERSION = "version";

	// Phase 1 construction, just let super-class handle it
	CatalogNode(DasDirNode parent, String name, List<String> lUrls)
	{
		super(parent, name, lUrls);
		data = null;
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
	boolean isLoaded(){ return (data != null); }

	
	protected void initFromJson(JSONObject jo) throws JSONException, ParseException{
		data = jo;
		if(data.has(KEY_CATALOG)){
			JSONObject cat = data.getJSONObject(KEY_CATALOG);
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
				AbstractNode child = DasNodeFactory.newNode(
					sChildType, this, sChildName, lChildLocs
				);
				
				dSubNodes.put(sChildId, child);
			}
		}
		
		// Work around odd implementation for the JSON library.  If an item is 
		// explicitly set to null, optString() fails to return the actual value 
		// in the file, instead it returns the opt value supplied to as the 
		// default for optString().  Not sure why they made null the same as
		// no value.  They are different.  --cwp
		if(data.has(KEY_SEPARATOR)){
			if(data.isNull(KEY_SEPARATOR)) sSep = null;
			else sSep = data.getString(KEY_SEPARATOR);
		}
	}
	
	protected void mergeFromJson(JSONObject jo){
		throw new UnsupportedOperationException("Not supported yet.");
	}
	

	@Override
	void load(ProgressMonitor mon) throws DasResolveException {
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
				if(!sVal.equals( type() ))
					throw new DasResolveException("Expected type '"+TYPE+"' not '"+sVal+"'", loc.sUrl);
				
				initFromJson(jo);
				loc.bLoaded = true;
				return;
				
			} catch(IOException | JSONException | ParseException | DasResolveException ex){
				loc.bBad = true;
				LOGGER.log(Level.FINE, 
					"Catalog location {0} marked as bad because {1}", 
					new Object[]{loc.sUrl, ex.getMessage()}
				);
				//If this was our last chance, go ahead and raise the exception
				if((i + 1) == lLocs.size()){
					DasResolveException resEx = new DasResolveException(
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
					throw new DasResolveException("Expected type '"+TYPE+"' not '"+sVal+"'", loc.sUrl	);
				
				mergeFromJson(jo);
				loc.bLoaded = true;
				return true;
				
			} catch(IOException | JSONException | DasResolveException ex){
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
	void parse(String sData, String sUrl) throws ParseException
	{
		
		JSONObject jo;
		String sType;
		try {
			jo = new JSONObject(sData);
			sType = jo.getString(KEY_TYPE);
		} catch (JSONException ex) {
			ParseException pe = new ParseException("Error reading node data.", -1);
			pe.initCause(ex);
			throw pe;
		}
		
		// Using exceptions for flow control... not good.
		if(!sType.equals(TYPE))
			throw new ParseException("Expected type '"+TYPE+"' not '"+sType+"'", -1);
				
		try {
			initFromJson(jo);
		} catch (JSONException ex) {
			ParseException pe = new ParseException("Error reading node data.", -1);
			pe.initCause(ex);
			throw pe;
		}
	}
	
}
