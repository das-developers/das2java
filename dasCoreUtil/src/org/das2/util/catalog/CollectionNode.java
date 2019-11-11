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

import java.text.ParseException;
import java.util.List;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONObject;

/** Collection Nodes group data sources that contain roughly equivalent data.
 *
 * There are many different ways to supply the same data.  Das2 servers,
 * HAPI servers, CDF file collections, SQL databases, etc. etc.  This node
 * type adds scientic contact details and a rough description of a dataset
 * but does not provide access methods.  Those are provided by child nodes.
 * 
 * @author cwp
 */
class CollectionNode extends AbstractDirNode {

	JSONObject data;
	static final String TYPE = "Collection";
	
	// Static names for important data dictionary keys
	static final String KEY_CATALOG = "sources";
	static final String KEY_TYPE    = "type";
	static final String KEY_NAME    = "name";
	static final String KEY_URLS    = "urls";
	static final String KEY_SEPARATOR = "separator";
	static final String KEY_TITLE   = "title";
	static final String KEY_VERSION = "version";
	static final String KEY_SCI_CONTACT = "sci_contacts";
	static final String KEY_UNTIS = "units";
	static final String KEY_CONVETION = "convertion";
	static final String KEY_COORDS = "coordinates";
	static final String KEY_DATA = "data";
	
	CollectionNode(DasDirNode parent, String name, List<String> lUrls)
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

	
	@Override
	void load(ProgressMonitor mon) throws DasResolveException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	boolean merge(ProgressMonitor mon) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	void parse(String sData, String sUrl) throws ParseException {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
