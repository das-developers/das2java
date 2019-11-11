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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.json.JSONException;
import org.json.JSONObject;


/** Static generator functions for das2 federated catalog node objects. 
 * 
 * One of the main purposes of this class is to maintain the root node registry.  Since
 * many formally constant responses (such as completions) are now dynamic, something has
 * to keep track of the catalog nodes or they would be re-loaded all the time.
 * Furthermore some of the catalog nodes (namely SPASE) can be loaded from locations that
 * require query parameters in the URLs, so we don't want to use the http filesystem
 * objects because they (AFAIK) would not map URLs like this:
 * 
 *    http://spase-group.org/registry/resolver?t=yes&i=spase://ASWS
 * 
 * to a file object on disk due to the '?' character in the URL.  The das2 catalog is
 * supposed to paper over all kinds of weird URLs, so directly downloading and caching
 * items seems like the best bet.
 * 
 * @author cwp
 */
public class DasNodeFactory
{
	private static final Logger LOGGER = LoggerManager.getLogger( "das2.catalog" );
	
	// The, the only, the detached root node map.  Any understood format can be a detached
	// root node.  Root nodes have no path name and cannot have parents, but may have
	// children.
	private static final Map<String, AbstractNode> ROOT_NODES;
	static{
		ROOT_NODES = Collections.synchronizedMap(new HashMap<String, AbstractNode>());
		
		// Add the built-in root node
		List<String> lUrls = new ArrayList<>();
		lUrls.add("http://das2.org/catalog/index.json");
		lUrls.add("https://raw.githubusercontent.com/das-developers/das-cat/master/cat/index.json");
		ROOT_NODES.put(null, new CatalogNode(null, null, lUrls));
	}
	
	// The starting path for the das2 source catalog
	public static final String DAS_ROOT_PATH = "tag:das2.org,2012:";
	
	// The heart of the factory, preform phase 1 construction of a node given a type
	// name
	static AbstractNode newNode(
		String sType, DasDirNode parent, String sName, List<String> lLocs
	) throws ParseException {
		switch(sType){
			case CatalogNode.TYPE:
				return new CatalogNode(parent, sName, lLocs);
			case HttpGetSrcNode.TYPE:
				return new HttpGetSrcNode(parent, sName, lLocs);
			
			// TODO: Add collection type here...
				
		}
		throw new ParseException("Unknown node type '"+sType+"'.", -1);
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
	
	static DasNode getDetachedRoot(String sUrl, ProgressMonitor mon, boolean bReload) 
		throws ParseException, IOException
	{
		
		// It's a standalone root node, see if we've already been asked for this one.
		if(!bReload && ROOT_NODES.containsKey(sUrl)) return ROOT_NODES.get(sUrl);
		
		// Just going around seeing who can parse it, start first with JSON nodes
		String sData = getUtf8NodeDef(sUrl, mon);
		AbstractNode node;
		if(sData.startsWith("{")){
			JSONObject json;
			String sType;
			try{
				json = new JSONObject(sData);
				sType = json.getString("type");
			} catch(JSONException ex){
				ParseException pe = new ParseException(
					"Error reading "+sUrl+": "+ex.getMessage(), -1
				);
				pe.initCause(ex);
				throw pe;
			}
			
			// Could be a Catalog, Collection, or HttpStreamSrc all of which are JSON data
			switch(sType){
				case CatalogNode.TYPE:
					node = new CatalogNode(null,  null, null);
					node.parse(sData, sUrl);
					ROOT_NODES.put(sUrl, node);
					return node;
					
				case HttpGetSrcNode.TYPE:
					node = new HttpGetSrcNode(null, null, null);
					node.parse(sData, sUrl);
					ROOT_NODES.put(sUrl, node);
					return node;
				default:
					throw new ParseException("Unknown node type '"+sType+"' at "+sUrl+".", -1);
			}
		}
		
		// Well that didn't work, try to parse it as XML.
		if(sData.startsWith("<?xml")){
			Document doc;
			try{
				doc = getXmlDoc(sData);
			} catch(SAXException | ParserConfigurationException ex){
				ParseException pe = new ParseException(
					"Error reading "+sUrl+": "+ex.getMessage(), -1
				);
				pe.initCause(ex);
				throw pe;
			}
			
			// I don't have any catalog types that support XML...yet
			throw new UnsupportedOperationException("SPASE catalog objects not yet supported");
		}
		
		throw new ParseException("Couldn't determine node type of document at "+sUrl, -1);
	}
		
	/** Get a node from the global node map by URL. 
	 * 
	 * This function tries to load and return the node for the given URL.  If the file
 portion of the node is a recognized filesystem type then that exact URL is 
 attempted.  For example:
 
    https://space.physics.uiowa.edu/juno/test/random_source.data
 
 would trigger a filesystem type lookup that expects an exact match.  While a URL
 such as:
 
   tag:das2.org,2012:test:/uiowa/juno/random_collection/das2
 
 For space savings, tag:das2.org,2012: may be left off of the given URLs.
 
 If nothing can be matched, null is return.  The resulting parsed node is saved in
 a cache to avoid repeated network traffic.
	 * 
	 * @param sUrl
	 * @param mon
	 * @param bReload - Reload the node definition from the original source
	 * @return The node requested, or throws an error
	 * @throws org.das2.util.catalog.DasResolveException
	 * @throws java.io.IOException
	 * @throws java.text.ParseException
	 */
	public static DasNode getNode(String sUrl, ProgressMonitor mon, boolean bReload) 
		throws DasResolveException, IOException, ParseException {
		
		// null URL, go get one of the default roots
		if(sUrl == null || (sUrl.length() == 0)){
			AbstractNode node = ROOT_NODES.get(null);
			if(!node.isLoaded()) node.load(mon);
			return node;
		}
			
		// If this starts 'site' or 'test' it's one of our convienence paths, make it 
		// an absolute path
		if(sUrl.startsWith("site") || sUrl.startsWith("test"))
			sUrl = DAS_ROOT_PATH + sUrl;
		
		// If this starts with 'tag:' it's a network catalog node, resolve it
		if(sUrl.startsWith("tag:")){
			AbstractDirNode node = (AbstractDirNode) ROOT_NODES.get(null);
			if(!node.isLoaded()) node.load(mon);
			return node.resolve(sUrl, mon);
		}
		
		// Try to see if it will load as a detached root
		return getDetachedRoot(sUrl, mon, bReload);
	}
	
	/** Kind of like traceroute, try to resolve successively longer paths until
	 * you get to one that fails.  For filesystem type URLS (http:, file:, etc.)
	 * this is the same as getNode().
	 * 
	 * @param sUrl An autoplot URL
	 * @param mon
	 * @param bReload
	 * @return The nearest loadable DasNode for the path specified.
	 * @throws org.das2.util.catalog.DasResolveException if a Filesystem style URL cannot
	 *         be loaded as a dasnode.
	 * 
	 */
	public static DasNode getNearestNode(String sUrl, ProgressMonitor mon, boolean bReload) 
		throws DasResolveException 
	{
		DasNode node;
		
		// Get simple filsystem style lookups out of the way first
		// FIXME: Relpace prefix list below with filesystem registry listing
		if((sUrl != null)&&(sUrl.length() > 5)){
			String aFileSysPrefixes[] = {"file:", "http:", "https:", "ftp:"};
			for (String sPrefix: aFileSysPrefixes){
				if(!sUrl.toLowerCase().startsWith(sPrefix)) continue;
				
				try{
					// Exact node match is all we've got...
					node = getNode(sUrl, mon, bReload);
					return node;
				} catch(IOException | ParseException ex){
					DasResolveException re;
					re = new DasResolveException("Could not resolve URL to node", sUrl);
					re.initCause(ex);
					throw re;
				}
			}
		}
		
		// Assume it's a catalog path and try for exact resolution, exception handling is
		// different here because we do have a fallback point if nothing else works
		try{
			node = getNode(sUrl, mon, bReload);
			return node;
		} catch(IOException | ParseException | DasResolveException ex){
			LOGGER.log(Level.FINE, 
				"Exact resolution of {0} failed due to {1}, looking for longest resolvable path", 
				new Object[]{sUrl, ex.getMessage()}
			);
		}
		
		// Going to need the root node now
		AbstractDirNode root = (AbstractDirNode) ROOT_NODES.get(null);
		if(!root.isLoaded()) try{
			root.load(mon);
		} catch(DasResolveException ex){
			LOGGER.log(
				Level.INFO, "Root node could not be resolved tried {0}", 
				root.prettyPrintLoc(null, " ")
			);
		}
		
		if(sUrl == null || (sUrl.length() == 0))  return root;
		
		// Handle convienence URLs
		if(sUrl.startsWith("site") || sUrl.startsWith("test"))
			sUrl = DAS_ROOT_PATH + sUrl;
		
		return root.nearest(sUrl, mon);		
	}
	
		 
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
