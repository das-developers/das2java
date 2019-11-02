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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.ProgressMonitor;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/** Static generator for das2 federated catalog node objects.
 *
 * @author cwp
 */
public class DasNodeFactory
{
	private static final Logger LOGGER = LoggerManager.getLogger( "das2.catalog" );
	
	private static final Document getXmlDoc(String sData)
		throws IOException, SAXException, ParserConfigurationException
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.parse(new InputSource(new StringReader(sData)));
		return doc;
	}
	
	public static final String[] aCatRootURL = {
		"http://das2.org/catalog/index.json",
		"https://raw.githubusercontent.com/das-developers/das-cat/master/cat/index.json"
	};
	
	/** Get an new Root catalog node.
	 * 
	 * Any understood format can be a root node.  Root nodes have no path name and cannot
	 * have parents, but may have children.  The global root node exists at 
	 * http://das2.org/catalog with a backup at github incase das2.org is down.
	 * 
	 * @param sUrl The file to load.  The content will be inspected to determine the proper
	 *             node type to build.  
	 * @param mon  A progress monitor to deal with network operations
	 * 
	 * @return A new root node of the appropriate type.
	 * 
	 */
	public static DasNode newRoot(String sUrl, ProgressMonitor mon) 
		throws MalformedURLException, IOException, ParseException 
	{
		//FileSystem fs = FileSystem.create(sURL, mon);
		//FileObject fo = fs.getFileObject(sURL);    // This will fail, find out why
		//File file = fo.getFile();
		//String s = FileUtil.readFileToString(file);
		
		String[] aTry;
		
		if(sUrl != null){ aTry = new String[]{ sUrl }; }
		else{             aTry = aCatRootURL;          }
		
		ParseException pe;
		
		for(int i = 0; i < aTry.length; ++i){
			
			BufferedInputStream input = new BufferedInputStream(new URL(aTry[i]).openStream());
			ByteArrayOutputStream output = new ByteArrayOutputStream(100_000);
		
			byte aBuf[] = new byte[1024];
			int nRead;
			while ((nRead = input.read(aBuf, 0, 1024)) != -1) { output.write(aBuf, 0, nRead); }
			String sNode = output.toString("UTF-8");

			try{
				// See if you can parse as JSON
				JSONObject obj = new JSONObject(sNode);
			} catch(JSONException ex){
				
				try{
					// TODO: see if you can parse it as XML...
					Document doc = getXmlDoc(sNode);
				} catch(Exception ex2){
					pe = new ParseException("Could not parse node data", -1);
					pe.initCause(ex2);
					throw pe;
				}
				
				pe = new ParseException("Could not parse node data", -1);
				pe.initCause(ex);
				throw pe;
			}
		}
		
		return null;
	}
}
