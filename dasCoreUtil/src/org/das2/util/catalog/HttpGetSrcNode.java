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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONObject;

/**
 *
 * @author cwp
 */
public class HttpGetSrcNode extends AbstractNode implements DasSrcNode
{
	private static final Logger LOGGER = LoggerManager.getLogger("das2.catalog.httpsrc");

	JSONObject json = null;
	public static final String TYPE = "HttpStreamSrc";
	
	protected String sPath;  // My name from the root location
	protected String sName;  // My human readable name
	protected String sSrcUrl = null;  // Where I came from (if loaded)
	protected Map<String, String> dLocs = new HashMap<>();  // Where I can be loaded from

	public HttpGetSrcNode(DasDirNode parent, String name, List<String> locations)
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
	void parse(String sData, String sUrl) throws ParseException
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
