/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.das2.datum.CalendarTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**  Generates a selector set by parsing a list of parameters which could be from a
 * command line.  This is intended mostly to help with command line readers.  But could
 * be used else where.
 *
 * If the generator is put into Das2 Server compatible mode, the first not keyword=value
 * pair is parsed as a start time, and the second is parsed as an end time.
 *
 * @author cwp
 */
public class DataSource {

	/////////////////////////////////////////////////////////////////////////////////////
	// Static Utilities //

	/** Line wrap function, taken from
	 *   http://progcookbook.blogspot.com/2006/02/text-wrapping-function-for-java.html
	 * and then customized a little
	 *
	 * @param sText - The text to wrap
	 * @param nLineLen - The length of each lines text area
	 * @param sPrefix - A prefix string, to be added to each line, if not null
	 * @return
	 */
	private static String[] wrapText(String sText, int nLineLen, String sPrefix){
		// return empty array for null text
		if(sText == null){
			return new String[]{};
		}

		// Strip out all the newlines and tabs that might happen to be in the text
		sText = sText.replaceAll("\t\r\n", "");

		// Collapse 2+ spaces to a single space
		sText = sText.replaceAll("\\s+", " ");

		// return text if len is zero or less
		if(nLineLen <= 0){
			return new String[]{sText};
		}

		// return text if less than length
		if(sText.length() <= nLineLen){
			if(sPrefix == null)
				return new String[]{sText};
			else
				return new String[]{sPrefix + sText};
		}

		char[] chars = sText.toCharArray();
		Vector lines = new Vector();
		StringBuffer line = new StringBuffer();
		StringBuffer word = new StringBuffer();

		for(int i = 0; i < chars.length; i++){
			word.append(chars[i]);

			if(chars[i] == ' '){
				if((line.length() + word.length()) > nLineLen){
					lines.add(line.toString());
					line.delete(0, line.length());
				}

				line.append(word);
				word.delete(0, word.length());
			}
		}

		// handle any extra chars in current word
		if(word.length() > 0){
			if((line.length() + word.length()) > nLineLen){
				lines.add(line.toString());
				line.delete(0, line.length());
			}
			line.append(word);
		}

		// handle extra line
		if(line.length() > 0){
			lines.add(line.toString());
		}

		String[] lRet = new String[lines.size()];
		int c = 0; // counter
		if(sPrefix == null){
			for(Enumeration e = lines.elements(); e.hasMoreElements(); c++){
				lRet[c] = (String) e.nextElement();
			}
		}
		else{
			for(Enumeration e = lines.elements(); e.hasMoreElements(); c++){
				lRet[c] = sPrefix + (String) e.nextElement();
			}
		}

		return lRet;
	}

	/** Helper to make digging out the value of sub-elements less wordy */
	private static String getSubElementValue(Element element, String sSubName){
		return element.getElementsByTagName(sSubName).item(0).getFirstChild().getNodeValue();
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Per Instance //

	String m_sDsidUrl = null;
	CalendarTime m_ctLoad = null;
	List<SelectorTplt> m_lSelTplts = null;
	Document m_dsid = null;
	String m_sDas2TimeKey = null;

	/** Create a selector generator and initialize it from an XML datasource specification.
	 * that expects all elements of the list to conform
	 * to the das3 style command line interface.
	 */
	public DataSource(String sDsidUrl) throws SAXException, IOException{

		
		m_sDsidUrl = sDsidUrl;

		m_lSelTplts = new LinkedList<SelectorTplt>();

		// Okay, sDsidUrl is supposed to be a DSID file, let's validate it.
		SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		URL url = RunRdr.class.getResource("/schema/das3_dsid-0.2.xsd");
		Schema schema = null;
		try{
			schema = sf.newSchema(url);
		}
		catch(SAXException ex){
			throw new RuntimeException(ex);
		}

		Validator valer = schema.newValidator();
		
		Source src = new StreamSource(sDsidUrl);
		m_ctLoad = CalendarTime.now();
		valer.validate(src);

		//Okay, the document validated, let's use it to get work done...
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

		dbf.setValidating(false);
		dbf.setCoalescing(true);

		DocumentBuilder db;
		try{
			db = dbf.newDocumentBuilder();
		}
		catch(ParserConfigurationException ex){
			throw new RuntimeException(ex);
		}

		m_dsid = db.parse(sDsidUrl);


		// Parse the XML info into selector templates...
		throw new NotImplementedException();
	}

	@Override
	public String toString(){
		return String.format("%s @ %s", m_sDsidUrl,
			                  m_ctLoad.toISO8601(CalendarTime.Resolution.SECOND));
	}
	

	/** Get basic information on this datasource, formatted as appropriate for generic
	 * text display.
	 *
	 * @return A multi-line text string suitable for display with a fixed width font.
	 */
	public String getInfo(){

		StringWriter strWriter = new StringWriter();
		PrintWriter out = new PrintWriter(strWriter);

		Element top = m_dsid.getDocumentElement();
		String sName = top.getAttribute("name");
		String sDesc = getSubElementValue(top, "description").trim();
		String sSummary = getSubElementValue(top, "summary").trim();
		String[] lLines;

		out.print(sName + "\n");
		lLines = wrapText(sSummary, 75, "   ");
		for(String sLine: lLines) out.print(sLine+"\n");
		out.print("\n");

		out.print("Description:\n");
		lLines = wrapText(sDesc, 75, "   ");
		for(String sLine: lLines) out.print(sLine + "\n");
		out.print("\n");

		NodeList lDims = top.getElementsByTagName("dimension");

		if(lDims.getLength() < 2)
			out.print("Output:\n");
		else
			out.print("Outputs:\n");

		if((lDims != null)&&(lDims.getLength() > 0)){
			for(int i = 0; i < lDims.getLength(); i++){
				Element dim = (Element) lDims.item(i);
				String sDimName = dim.getAttribute("name");
				String sDimQuant = dim.getAttribute("quantity");
				String sDimUnit = dim.getAttribute("unit");

				if(sDimQuant.substring(0, 1).toLowerCase().matches("[aeiour]"))
					out.printf("   %s, an %s", sDimName, sDimQuant, sDimUnit);
				else
					out.printf("   %s, a %s", sDimName, sDimQuant, sDimUnit);

				if(! sDimUnit.toLowerCase().equals("n/a") )
					out.printf(" in %s", sDimUnit);
				out.printf("\n");

				lLines = wrapText(dim.getTextContent(), 75, "      ");
				for(String sLine: lLines) out.print(sLine+"\n");
				out.print("\n");
			}
		}

		// Finally, send the maintainer info
		Element elMain = (Element) top.getElementsByTagName("maintainer").item(0);
		out.print("Maintainer:\n");
		out.printf("   %s <%s>\n\n", elMain.getAttribute("name"),
			               elMain.getAttribute("email"));

		return strWriter.toString();
	}

	/** Get command line argument help for this data source
	 *
	 * @return A multi-line text string suitable for display with a fixed width font.
	 */
	public String getHelp(){
		
		return null;
	}

	/** Get basic information on this datasource, formatted as appropriate for generic
	 * text display.
	 * @return The multi-line information string.
	 */

	/** Allow for Das2 Style start and end time specification.
	 *  */
	public void setDas2Compatible(String sTimeKey){
		if(sTimeKey == null)
			throw new NullPointerException("Time key argument is null");
		m_sDas2TimeKey = sTimeKey;
	}


	/** Check a query against the datasource definition
	 *
	 * @param lArgs
	 * @return True if the query should run, or False if the query is not a valid request
	 *         for this data source.
	 */
	boolean validateQuery(List<String> lArgs){

		return false;
	}

	/** Parse a query into a selector set*/
	List<Selector> parseQuery(List<String> lArgs){

		return null;
	}

	/** Instantiate a reader to produce the query results
	 *
	 * @return
	 */
	Reader newReader(){

		return null;
	}
}
