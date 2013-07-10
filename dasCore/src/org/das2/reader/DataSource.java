/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
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

	CalendarTime m_ctLoad = null;
	List<SelectorTplt> m_lSelTplts = null;

	/** Create a selector generator and initialize it from an XML datasource specification.
	 * that expects all elements of the list to conform
	 * to the das3 style command line interface.
	 */
	public DataSource(String sDsidUrl) throws SAXException, IOException{

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

		Document dsid = null;
		dsid = db.parse(sDsidUrl);
		
	}

	/** Allow for Das2 Style start and end time specification.
	 *  */
	public void setDas2Compatible(String sTimeKey){
		
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
