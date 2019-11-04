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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Static generator functions for das2 federated catalog node objects. 
 * 
 * One of the main purposes of this class is to maintain the node registry.  Since many
 * formally constant responses (such as completions) are now dynamic, something has to
 * keep track of the catalog nodes or they would be re-loaded all the time.  Furthermore
 * some of the catalog nodes (namely SPASE) can be loaded from locations that require 
 * query parameters in the URLs, so we don't want to use the http filesystem objects 
 * because they (AFAIK) would not map URLs like this:
 * 
 *    http://spase-group.org/registry/resolver?t=yes&i=spase://ASWS
 * 
 * to a file object on disk due to the '?' character in the URL.  The das2 catalog is
 * supposed to paper over all kinds of wierd URLs, so directly downloading and caching
 * items seems like the best bet.
 * 
 * @author cwp
 */
public class DasNodeFactory
{
	private static final Logger LOGGER = LoggerManager.getLogger( "das2.catalog" );
	
	
	// This is the supra node, it's not real.  It only has one child, null, that
	// has hard coded lookup URLs
	public class SupraNode extends DasAbstractNode implements DasDirNode{

		@Override
		LoadResult load(String sUrl, ProgressMonitor mon) {
			LoadResult res = new LoadResult();  // Defaults to success
			return res;
		}

		@Override
		public String type() { return "Catalog"; }

		@Override
		public String name() { return null; /* voldemort */ }

		@Override
		public boolean isDataSource() { return false; }

		@Override
		public String[] list() {
			throw new UnsupportedOperationException("Not supported yet.");
		}

		@Override
		public DasNode resolve(String sName) {
			// The main purpose of this fake node, to provide a list of
			throw new UnsupportedOperationException("Not supported yet.");
		}
		
	}
	
	// The, the only, the detached root node map.  Any understood format can be a detached
	// root node.  Root nodes have no path name and cannot have parents, but may have
	// children.
	private static final Map<String, List<DasNode>> ROOT_NODES;
	static{
		ROOT_NODES = Collections.synchronizedMap(new HashMap<String, List<DasNode>>());
	}
	
	// The compiled in default root node locations if user doesn't supply a URL
	public static final String[] DEFAULT_ROOT_URLS = {
		// TODO: Move to supranode data
		"http://das2.org/catalog/index.json",
		"https://raw.githubusercontent.com/das-developers/das-cat/master/cat/index.json"
	};
	
	/** Get the root node list as a string with some separator and prefix
	 * @param sPre A prefix to place before each root node URL, may be null
	 * @param sSep The separator to use between each root node URL, may be null
	 * @return A formatted string containing a list of all root node URLs
	 */
	public static String defRootNodesAsStr(String sPre, String sSep){
		// TODO: move to supra node data
		
		StringBuilder bldr = new StringBuilder();
		for(int i = 0; i < DEFAULT_ROOT_URLS.length; i++){
			if((i > 0)&&(sSep!=null)) bldr.append(sSep);  // prefix sep if needed
			if(sPre != null) bldr.append(sPre);
			bldr.append(DEFAULT_ROOT_URLS[i]);
		}
		return bldr.toString();
	}
	
	
	// Get a dom object from a document in string form
	static Document getXmlDoc(String sData)
		throws IOException, SAXException, ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(sData)));
		return doc;
	}
	
	// TODO: Figure out how to use the progress monitor here
	public static String getUtf8NodeDef(String sUrl, ProgressMonitor mon) 
		throws IOException
	{
		// Would like to use the filesystem objects, but they don't support strange
		// query based URLs as if they were files. --cwp
		
		//FileSystem fs = FileSystem.create(sURL, mon);
		//FileObject fo = fs.getFileObject(sURL);    // This will fail, find out why
		//File file = fo.getFile();
		//String s = FileUtil.readFileToString(file);
		
		BufferedInputStream input = new BufferedInputStream(new URL(sUrl).openStream());
		ByteArrayOutputStream output = new ByteArrayOutputStream(100_000);
		
		byte aBuf[] = new byte[1024];
		int nRead;
		while ((nRead = input.read(aBuf, 0, 1024)) != -1) { output.write(aBuf, 0, nRead); }
		String sThing = output.toString("UTF-8");
		return sThing;
	}
		
	/** Get a node from the global node map by URL. 
	 * 
	 * This function tries to load and return the node for the given URL.  If the file
	 * portion of the node is a recognized filesystem type then that exact URL is 
	 * attempted.  For example:
	 * 
	 *    https://space.physics.uiowa.edu/juno/test/random_source.json
	 * 
	 * would trigger a filesystem type lookup that expects an exact match.  While a URL
	 * such as:
	 * 
	 *   tag:das2.org,2012:test:/uiowa/juno/random_collection/das2
	 * 
	 * For space savings, tag:das2.org,2012: may be left off of the given URLs.
	 * 
	 * If nothing can be matched, null is return.  The resulting parsed node is saved in
	 * a cache to avoid repeated network traffic.
	 * 
	 * @param sUrl
	 * @param mon
	 * @param bReload - Reload the node definition from the original source
	 * @return The node requested, or throws an error
	 */
	public static DasNode getNode(String sUrl, ProgressMonitor mon, boolean bReload) {
		
		// We never put non root nodes in here, so this step is okay
		if(!bReload && ROOT_NODES.containsKey(sUrl))
			return ROOT_NODES.get(sUrl).get(0);
		
		// null URL, go get one of the default roots
		if(sUrl == null){
			// I know that the root nodes are DasDirNodeCat objects.  If this changes will
			// have to update this library
			DasDirNodeCat node = new DasDirNodeCat();
			
			// Clean out the old definitions (if any)
			if(ROOT_NODES.containsKey(null)) ROOT_NODES.remove(null);
			
			for(String sLoc: DEFAULT_ROOT_URLS){
				DasAbstractNode.LoadResult res = node.load(sLoc, mon);
				if(res.bSuccess){
					List<DasNode> list = new ArrayList<>();
					list.add(node);
					ROOT_NODES.put(null, list);
					return node;
				}
			}
			return null;
		}
		
		// See if it matches a filesystem prefix
		
		return null;
	}
	
	/** If the given node lookup fails, attempt to get a higher level catalog node.
	 * 
	 * The operation of this function is the same as getNode if the given URL does 
	 * resolve to a loadable catalog node.
	 * 
	 * @param sUrl An autoplot URL
	 * @param mon
	 * @param bReload
	 * @return null if resolution failed or a DasNode otherwise.  The actual 
	 *        source of the node can be found by DasNode.source().
	 */
	public static DasNode getNearestNode(String sUrl, ProgressMonitor mon, boolean bReload)
	{
		DasNode node = getNode(sUrl, mon, bReload);
		if(node != null) return node;
		
		assert false : "handle resolution failure";
		return null;
	}
	
	// Catalog node URIs, all begin with vap+dfcnode:
		 // 
		 //   http://das2.org/catalog/das2/site/uiowa.json   (root is some file)
		 //   file:///home/cwp/test/uiowa.json               (root is some file)
		 //   dfc:///tag:das2.org,2012:site:/uiowa/juno/survey/wav  (root is main)
		 
		 //try{
		//	File file = DataSetURI.getFile(split.resourceUri, mon);
		//	String s = FileUtil.readFileToString(file);
			
			// See if you can parse the JSON
		//	JSONObject obj = new JSONObject(s);
			
		 //}
		 //catch(Exception ex){
		//	 logger.log(Level.SEVERE, surl, ex);
			// return false;
		// }
		 
		 
		 //If the authority starts with 'tag:' then assume we can look it up in 
		 // the global catalog, otherwise it's just a file to read
		 //if(split.)
		 
		 
		 //try{
		 //mon.setTaskSize(5);
		 //mon.started();
		 
		 //do step
		 
		 //mon.setTaskProgress(0);
		 
		 //monSub = mon.getSubtaskMonitor("Downloading blah");
		 
		 
		 //}
		 //catch{
			 
			 
		 //}
		//finally{
		//	 mon.finished();
		// }
}
