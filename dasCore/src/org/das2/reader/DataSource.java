/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

		sText = sText.trim();

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
	// Inner class to handle selection choices, or just simple selections

	private static enum NodeType{TEMPLATE,CHOICE};
	private class SelectorNode{
		NodeType type;
		Object item;

		private SelectorNode(SelectorTplt tplt){
			type = NodeType.TEMPLATE;
			item = tplt;
		}

		private SelectorNode(List<SelectorTplt> list){
			type = NodeType.CHOICE;
			item = list;
		}

		private SelectorTplt getTemplate(){
			if(type == NodeType.CHOICE)
				throw new IllegalStateException("This is a choice node, not a template node");
			return (SelectorTplt)item;
		}

		private void addChoice(SelectorTplt selectorTplt){
			if(type == NodeType.TEMPLATE)
				throw new IllegalStateException("This is a template node, not a choice node");
			List<SelectorTplt> list = (List<SelectorTplt>)item;
			list.add(selectorTplt);
		}

		private List<SelectorTplt> getChoices(){
			if(type == NodeType.TEMPLATE)
				throw new IllegalStateException("This is a template node, not a choice node");
			return (List<SelectorTplt>)item;
		}
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// Per Instance //

	String m_sDsidUrl = null;
	CalendarTime m_ctLoad = null;
	List<SelectorNode> m_lSelNodes = null;
	Document m_dsid = null;
	String m_sName = null;
	String m_sDas2TimeKey = null;

	/** Create a selector generator and initialize it from an XML datasource specification.
	 * that expects all elements of the list to conform
	 * to the das3 style command line interface.
	 */
	public DataSource(String sDsidUrl) throws SAXException, IOException{

		
		m_sDsidUrl = sDsidUrl;

		m_lSelNodes = new LinkedList<SelectorNode>();

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

		Element elTop = m_dsid.getDocumentElement();

		// Save our name
		m_sName = elTop.getAttribute("name");

		// Get the Selectors node
		Element elSelTop = (Element) elTop.getElementsByTagName("selectors").item(0);

		NodeList nl = elSelTop.getChildNodes();
		for(int i = 0; i < nl.getLength(); i++){
			Node node = nl.item(i);
			if(node.getNodeType() != Node.ELEMENT_NODE) continue;
			Element el = (Element)node;
			
			// Handle looping over selectors in a choice
			String sElTagName = el.getTagName();
			if(sElTagName.equals("choice")){

				SelectorNode sn = new SelectorNode(new LinkedList<SelectorTplt>());
				NodeList nlSub = el.getChildNodes();
				for(int j = 0; j < nlSub.getLength(); j++){
					Node nodeSub = nlSub.item(j);
					if(nodeSub.getNodeType() != Node.ELEMENT_NODE) continue;
					Element elSub = (Element)nodeSub;
					sn.addChoice(new SelectorTplt(elSub));
				}
				m_lSelNodes.add(sn);
				continue;
			}

			m_lSelNodes.add(new SelectorNode(new SelectorTplt(el)));
		}
	}

	@Override
	public String toString(){
		return String.format("%s @ %s", m_sDsidUrl,
			                  m_ctLoad.toISO8601(CalendarTime.Resolution.SECOND));
	}

	/////////////////////////////////////////////////////////////////////////////////////
	// User readable information output //

	/** Get basic information on this datasource, formatted as appropriate for generic
	 * text display.
	 *
	 * @return A multi-line text string suitable for display with a fixed width font.
	 */
	public String getInfo(){
		return getInfo(false);
	}

	/** Get full information on this datasource, formatted as appropriate for generic
	 * text display.
	 *
	 * @return A multi-line text string suitable for display with a fixed width font.
	 */
	public String getHelp(){
		return getInfo(true);
	}

	protected String getInfo(boolean bParamHelp){

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

		if(bParamHelp){
			out.print("Data Selection Parameters:\n");
			String sParamHelp = getParamHelp();
			out.print(sParamHelp);
		}

		if(!bParamHelp){
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
		}

		// Finally, send the maintainer info
		Element elMain = (Element) top.getElementsByTagName("maintainer").item(0);
		out.print("Maintainer:\n");
		out.printf("   %s <%s>\n\n", elMain.getAttribute("name"),
			               elMain.getAttribute("email"));

		return strWriter.toString();
	}

	/** Get command line argument help for this data source.
	 * The help displayed will change based on wether setDas2Compatible has been set
	 * for the datasource.
	 *
	 * @return A multi-line text string suitable for display with a fixed width font.
	 */
	public String getParamHelp(){
		
		StringWriter strWriter = new StringWriter();
		PrintWriter out = new PrintWriter(strWriter);
		String[] lLines;

		for(SelectorNode sn: m_lSelNodes){

			if(sn.type == NodeType.TEMPLATE){

				SelectorTplt tplt = sn.getTemplate();

				//If this is a constant value, don't report it to the user
				if(tplt.isConstant()) continue;

				String sUnitStr = "";
				if(!tplt.getUnitStr().equals("")) sUnitStr = "("+tplt.getUnitStr()+")";

				if(tplt.getType() == Selector.Type.RANGE)
					out.printf("   %s:beg=%s, %s:end=%s %s\n", tplt.getKey(), tplt.getValTpltStr(),
						        tplt.getKey(), tplt.getValTpltStr(), sUnitStr);
				else
					out.printf("   %s=%s %s\n", tplt.getKey(), tplt.getValTpltStr(), sUnitStr);

				lLines = wrapText(tplt.getSummary(), 75, "      ");
				for(String sLine: lLines) out.print(sLine+"\n");

				if(tplt.hasDescription()){
					lLines = wrapText(tplt.getDescription(), 75, "      ");
					for(String sLine: lLines) out.print(sLine+"\n");
				}

				out.print("\n");
				continue;
			}

			// This is a choice, so go through the sub list twice, once to display
			// command strings, the second time to display help text.
			int nCount = 0;
			for(SelectorTplt tplt: sn.getChoices()){
				if(nCount > 0)
					out.print("      --or--\n");

				String sUnitStr = "";
				if(!tplt.getUnitStr().equals("")) sUnitStr = "("+tplt.getUnitStr()+")";

				if(tplt.getType() == Selector.Type.RANGE)
					out.printf("   %s:beg=%s, %s:end=%s %s\n", tplt.getKey(), tplt.getValTpltStr(),
						        tplt.getKey(), tplt.getValTpltStr(), sUnitStr);
				else
					out.printf("   %s=%s %s\n", tplt.getKey(), tplt.getValTpltStr(), sUnitStr);
				nCount += 1;
			}
			out.print("\n");

			for(SelectorTplt tplt: sn.getChoices()){
				out.printf("      %s\n", tplt.getKey());
				lLines = wrapText(tplt.getSummary(), 75, "         ");
				for(String sLine: lLines) out.print(sLine+"\n");

				if(tplt.hasDescription()){
					lLines = wrapText(tplt.getDescription(), 75, "         ");
					for(String sLine: lLines) out.print(sLine+"\n");
				}
				out.print("\n");
			}
		}

		return strWriter.toString();
	}

	
	/////////////////////////////////////////////////////////////////////////////////////
	/** Allow for Das2 Style start and end time specification.
	 *  */
	public void setDas2Compatible(String sTimeKey){
		if(sTimeKey == null)
			throw new NullPointerException("Time key argument is null");
		m_sDas2TimeKey = sTimeKey;
	}


	/////////////////////////////////////////////////////////////////////////////////////
	// Running Queries //

	/** Convert a das2 style command line into a das3 one.
	 * @param lArgs
	 * @return
	 */
	private List<String> convertFromDas2(List<String> lArgs) throws BadQueryException{
		List<String> lOut = new LinkedList<String>();

		// Stage 1, break out the third arg, if needed
		for(String sArg: lArgs){
			sArg = sArg.trim();
			
			//Take off quotes around the arg if needed
			sArg = sArg.replaceAll("^\"|\"$","");

			// Whitespace seperated string are broken into list of strings.
			lOut.addAll(Arrays.asList(sArg.split("\\s+")));
		}

		// Stage 2, convert the first two args that don't have an equals sign into
		// the time parameter.
		int nTimesConverted = 0;
		for(int i = 0; i < lOut.size(); i++){
			if(nTimesConverted > 1) break;

			String sArg = lOut.get(i);
			if(sArg.indexOf('=') == -1){
				if(nTimesConverted == 0)
					lOut.set(i, m_sDas2TimeKey + ":beg=" + sArg);
				else
					lOut.set(i, m_sDas2TimeKey + ":end=" + sArg);
				nTimesConverted += 1;
			}
		}

		if(nTimesConverted < 2)
			throw new BadQueryException("Couldn't find das2 BEGIN and END time parameters. "
				+ " Hint: New style time selection isn't compatible with das2 compatiablity mode");

		return lOut;
	}

	// Check to see if arguments are well formed.  Be careful anything can appear after
	// the = sign, even another equals
	private void checkArg(String sArg) throws BadQueryException{

		sArg = sArg.trim();

		if(sArg.length() < 3)
			throw new BadQueryException("Argument is to short to be valid '"+sArg+"'");

		int iEquals = sArg.indexOf('=');
		if(iEquals < 0)
			throw new BadQueryException("Missing equals '=' seperator in argument '"+sArg+"'");

		if(iEquals == 0)
			throw new BadQueryException("Key missing in argument '"+sArg+"'");

		if(iEquals == (sArg.length() - 1))
			throw new BadQueryException("Value missing in argument '"+sArg+"'");

		String sKey = sArg.substring(0, iEquals);
		String sVal = sArg.substring(iEquals+1, sArg.length());

		// See if the key is split
		int iColon = sKey.indexOf(':');  // Sounds like the new medical device from Apple
		if(iColon < 0) return;

		if((iColon == 0)||(iColon == (sKey.length() - 1))||(sKey.length() < 3))
			throw new BadQueryException("Bad range key in argument '"+sArg+"'");

		String sSide = sKey.substring(iColon + 1, sKey.length());
		sKey = sKey.substring(0, iColon);

		if((!sSide.equals("beg"))&&(!sSide.equals("end")))
			throw new BadQueryException("Range side '"+sSide+"' is invalid in argument '"+sArg+"'");

		return;
	}

	// Get the key part
	private String getKey(String sArg) throws BadQueryException{
		String[] lParts = sArg.split("[:=]");
		if(lParts.length < 2)
			throw new BadQueryException("Syntax error in argument '"+sArg+"'");
		return lParts[0];
	}

	// Get the value part
	private String getVal(String sArg){
		return sArg.split("=")[1];
	}

	// is this a range argument?
	private boolean isRange(String sArg){
		String[] lParts = sArg.split("[:=]");
		return (lParts.length >= 3);
	}

	// Find the template that goes with this argument
	private SelectorTplt getTemplate(String sKey) throws BadQueryException{

		for(SelectorNode node: m_lSelNodes){
			if(node.type == NodeType.CHOICE){
				for(SelectorTplt tplt: node.getChoices()){
					if(tplt.getKey().equals(sKey))
						return tplt;
				}
				continue;
			}
			
			SelectorTplt tplt = node.getTemplate();
			if(tplt.getKey().equals(sKey))
				return tplt;
		}

		throw new BadQueryException("Data selection key '"+sKey+"' is not defined for this datasource");
	}
	
	/** Parse a query into a selector set and add in the Constant selectors
	 *
	 * @param lArgs A list of selection arguments
	 * @return A list of selectors suitable for passing to Reader.retrieve or Reader.connect
	 * @throws BadQueryException
	 */
	public List<Selector> parseQuery(List<String> lArgs) throws BadQueryException, ReaderDefException{

		List<Selector> lSel = new LinkedList<Selector>();

		//If we are set in das2 compatible mode, preprocess the query.
		if(m_sDas2TimeKey != null)
			lArgs = convertFromDas2(lArgs);

		int iCurArg = 0;
		while(iCurArg < lArgs.size()){

			String sArg = lArgs.get(iCurArg);

			// Check to see if the argument is well-formed
			checkArg(sArg);

			// See if this key is used for any of our templates
			String sKey = getKey(lArgs.get(iCurArg));
			SelectorTplt tplt = getTemplate(sKey);
			if(tplt == null)
				throw new BadQueryException("The key '"+sKey+"' isn't defined for Datasource "
				                            + m_sName);

			// If this is one part of a range arg, find the matching end and pull it out of
			// the arg list.  This assumes that 
			if(! isRange(sArg) ){
				lSel.add( tplt.mkSelector(getVal(sArg)));
			}
			else{
				int iEndArg = -1;

				// If my current item contains "end" then I'm looking for "beg" and vice-versa
				String sSide = "end";
				if(sArg.contains(":end=")) sSide = "beg";

				for(int j = iCurArg + 1; j < lArgs.size(); j++){
					if(lArgs.get(j).startsWith(sKey)){
						if(!lArgs.get(j).startsWith(sKey+":"+sSide+"="))
							throw new BadQueryException("Syntax error in parameter '"+lArgs.get(j)+"'");

						iEndArg = j;
					}
				}

				if(iEndArg == -1)
					throw new BadQueryException("Ending '"+sKey+"' range missing");

				String sStopArg = lArgs.get(iEndArg);
				lSel.add( tplt.mkRangeSelector( getVal(sArg), getVal(sStopArg)));
				lArgs.remove(iEndArg);
			}

			// Move on to the next one, if available
			iCurArg += 1;
		}

		// Add in the constant selectors from the XML definition
		for(SelectorNode node: m_lSelNodes){
			if(node.type == NodeType.TEMPLATE){
				SelectorTplt tplt = node.getTemplate();
				if(tplt.isConstant()) lSel.add(tplt.mkSelector());
			}
			else{
				//Have to iterate over a choice node
				for(SelectorTplt tplt: node.getChoices()){
					if(tplt.isConstant()) lSel.add(tplt.mkSelector());
				}
			}
		}

		return lSel;
	}

	/** Instantiate a reader to produce the query results
	 *
	 * @return
	 */
	public Reader newReader() throws UnsupportedOperationException, ReaderDefException
	{

		// Get the class and the list off Jars to add to the path.
		Element elTop = m_dsid.getDocumentElement();
		Element elRdr = (Element) elTop.getElementsByTagName("reader").item(0);

		// Is this a java reader?
		NodeList nl = elRdr.getElementsByTagName("javaClass");
		if(nl.getLength() == 0){
			throw new UnsupportedOperationException("Only JAVA readers are currently supporetd"
				+ "but data source "+m_sDsidUrl+" doesn't specify a java based reader.");
		}
		Element elJavaClass = (Element) nl.item(0);
		String sClassName = getSubElementValue(elJavaClass, "class");

		nl = elJavaClass.getElementsByTagName("classpathext");
		ClassLoader loader;
		if(nl.getLength() == 0){
			// Well, they say that this reader type is alread on the class path, let's trust em
			loader = Thread.currentThread().getContextClassLoader();
		}
		else{
			URL[] aUrls = new URL[nl.getLength()];
			for(int i = 0; i< nl.getLength(); i++){
				Element elPathExt = (Element) nl.item(i);
				String sPathExt = elPathExt.getFirstChild().getNodeValue();
				try{
					aUrls[i] = new URL(sPathExt);
				}
				catch(MalformedURLException ex){
					throw new ReaderDefException("Path extension '"+sPathExt+"' is not a valid URL", ex);
				}
			}

			loader = new URLClassLoader(aUrls);
		}
		

		Class rdrClass;
		try{
			rdrClass = loader.loadClass(sClassName);
		}
		catch(ClassNotFoundException ex){
			throw new ReaderDefException("Class "+ sClassName +" was not found in the classpath", ex);
		}
		
		if( ! Reader.class.isAssignableFrom(rdrClass) ){
			throw new ReaderDefException("Class " + rdrClass.getSimpleName() +
				               " doesn't support the 'org.das2.reader.Reader' interface.");
		}

		Reader rdrInst = null;
		try{
			rdrInst = (Reader) rdrClass.newInstance();
		}
		catch(InstantiationException ex){
			throw new ReaderDefException("Can't make an instance of '"+rdrClass.getSimpleName()+"'", ex);
		}
		catch(IllegalAccessException ex){
			throw new ReaderDefException("Can't make an instance of '"+rdrClass.getSimpleName()+"'", ex);
		}
		return rdrInst;
	}
}
