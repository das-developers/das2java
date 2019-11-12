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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONException;
import org.json.JSONObject;

/** Engine for turning parameter settings into HTTP GET URLs
 *
 * @author cwp
 */
public class HttpGetSrcNode extends AbstractSrcNode
{
	private static final Logger LOGGER = LoggerManager.getLogger("das2.catalog.httpsrc");

	JSONObject data;
	public static final String TYPE = "HttpStreamSrc";
	
	static final String KEY_TYPE    = "type";
	static final String KEY_NAME    = "name";
	static final String KEY_TITLE   = "title";
	static final String KEY_VERSION = "version";
	
	// Top level large objects
	static final String KEY_FORMAT = "format";
	static final String KEY_IFACE  = "interface";
	
	static final String KEY_PROTO  = "protocol";
	static final String KEY_AUTH   = "authentication";
	static final String KEY_URLS   = "base_urls";
	static final String KEY_STYLE  = "convertion";
	static final String KEY_EXAMPLES = "examples";
	
	
	static final String TECH_CONTACT = "tech_contacts";

	
	public HttpGetSrcNode(DasDirNode parent, String name, List<String> locations)
	{
		super(parent, name, locations);
		data = null;
	}
	
	@Override
	public String type() { return TYPE;}
	
	@Override
	boolean isLoaded(){ return data != null; }

	protected void initFromJson(JSONObject jo) throws JSONException, ParseException{
		data = jo;
		
		if(! data.getString(KEY_TYPE).equals(TYPE))
			throw new ParseException("Node type missing or not equal to " + TYPE, -1);
		
	}
	
	private void mergeFromJson(JSONObject jo) throws JSONException, ParseException
	{
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	void parse(String sData, String sUrl) throws ParseException
	{
		JSONObject jo;
		try {
			jo = new JSONObject(sData);
			initFromJson(jo);
		} catch (JSONException ex) {
			ParseException pe = new ParseException("Error reading node data.", -1);
			pe.initCause(ex);
			throw pe;
		}
		
		// Save off the location
		for(NodeDefLoc loc: lLocs){
			if(loc.sUrl.equals(sUrl)){
				loc.bLoaded = true;
				return;
			}
		}
		
		NodeDefLoc loc = new NodeDefLoc(sUrl);
		loc.bLoaded = true;
	}

	@Override
	void load(ProgressMonitor mon) throws DasResolveException
	{
		for(NodeDefLoc loc: lLocs){
			loc.bLoaded = false;
			loc.bBad = false;
		}
		
		for(int i = 0; i < lLocs.size(); i++){
			NodeDefLoc loc = lLocs.get(i);
			try{
				String sData = DasNodeFactory.getUtf8NodeDef(loc.sUrl, mon);
				JSONObject jo = new JSONObject(sData);		
				initFromJson(jo);
				loc.bLoaded = true;
				return;
				
			} catch(IOException | JSONException | ParseException ex){
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
				mergeFromJson(jo);
				loc.bLoaded = true;
				return true;
				
			} catch(IOException | JSONException | ParseException ex){
				loc.bBad = true;
				LOGGER.log(Level.FINE, 
					"Catalog location {0} marked as bad because {1}", 
					new Object[]{loc.sUrl, ex.getMessage()}
				);
			}
		}
		
		return false;
	}

}
