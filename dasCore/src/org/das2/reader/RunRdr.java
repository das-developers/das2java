/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/** Provide a command line interface for executing readers that implement the DataStreamSrc
 * interface.
 *
 * The basic command line supported is:
 *
 * java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE key1=value1 key2=value2 ...
 *
 * if FILE is "-h", "--help", "help" and no key, value pairs are specified then basic
 * help text is generated.  To get command line options for a specific reader:
 *
 * java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE help
 *
 * to get information on a specific reader:
 *
 * java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE info
 *
 * @author cwp
 */
public class RunRdr{

	/* The error return values */
	public static final int NO_DSID_SPECIFIED = 3;
	public static final int DSID_INVALID      = 4;
	public static final int DSID_IOERROR      = 6;
	public static final int BAD_SCHEMA_FILE   = 42;


	/** Line wrap function, taken from
	 *   http://progcookbook.blogspot.com/2006/02/text-wrapping-function-for-java.html
	 * and then customized a little
	 *
	 * @param sText - The text to wrap
	 * @param nLineLen - The length of each lines text area
	 * @param sPrefix - A prefix string, to be added to each line, if not null
	 * @return
	 */
	public static String[] wrapText(String sText, int nLineLen, String sPrefix){
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
	/** Print an informational summary of the reader, but don't document all the command
	 * line arguments.
	 * @param logger
	 * @param dsid
	 * @return
	 */
	private static int info(Logger logger, Document dsid){
		Element top = dsid.getDocumentElement();
		String sName = top.getAttribute("name");
		String sDesc = getSubElementValue(top, "description").trim();
		String sSummary = getSubElementValue(top, "summary").trim();
		String[] lLines;

		System.out.print(sName + "\n");
		lLines = wrapText(sSummary, 75, "   ");
		for(String sLine: lLines) System.out.print(sLine+"\n");
		System.out.print("\n");

		System.out.print("Description:\n");
		lLines = wrapText(sDesc, 75, "   ");
		for(String sLine: lLines) System.out.print(sLine + "\n");
		System.out.print("\n");

		NodeList lDims = top.getElementsByTagName("dimension");

		if(lDims.getLength() < 2)
			System.out.print("Output:\n");
		else
			System.out.print("Outputs:\n");

		if((lDims == null)||(lDims.getLength() == 0)){
			System.out.print("     ERROR: Output quantities not specified!\n");
			return DSID_INVALID;
		}
		else{
			for(int i = 0; i < lDims.getLength(); i++){
				Element dim = (Element) lDims.item(i);
				String sDimName = dim.getAttribute("name");
				String sDimQuant = dim.getAttribute("quantity");
				String sDimUnit = dim.getAttribute("unit");
				
				if(sDimQuant.substring(0, 1).toLowerCase().matches("[aeiour]"))
					System.out.printf("   %s, an %s", sDimName, sDimQuant, sDimUnit);
				else
					System.out.printf("   %s, a %s", sDimName, sDimQuant, sDimUnit);

				if(! sDimUnit.toLowerCase().equals("n/a") )
					System.out.printf(" in %s", sDimUnit);
				System.out.printf("\n");

				lLines = wrapText(dim.getTextContent(), 75, "      ");
				for(String sLine: lLines) System.out.print(sLine+"\n");
				System.out.print("\n");
			}
		}

		// Finally, send the maintainer info
		Element elMain = (Element) top.getElementsByTagName("maintainer").item(0);
		System.out.print("Maintainer:\n");
		System.out.printf("   %s <%s>\n\n", elMain.getAttribute("name"),
			               elMain.getAttribute("email"));
		return 0;
	}

	/////////////////////////////////////////////////////////////////////////////////////
	/** Print help information for this readers command line */
	private static int help(Logger logger, Document dsid, String sTitle){

		Element top = dsid.getDocumentElement();
		if(sTitle == null){
			String sName = top.getAttribute("name");
			System.out.printf("%s: command line data selectors\n", sName);
		}

		Element elSelectors = (Element) top.getElementsByTagName("selectors").item(0);
		elSelectors.getChildNodes();

		
		return 0;
	}

	/** Run the thing. */
	private static int run(Logger logger, Document dsid){
		throw new UnsupportedOperationException("Not yet implemented");
	}


	/** A Main function for running an arbitrary stream source */
	static public void main( String[] lArgs) throws ParserConfigurationException{

		Logger logger = Logger.getLogger("ReaderRunner");

		// Make the logger output prettier, since these messages may be seen by an end user
		logger.setUseParentHandlers(false);
		ConsoleHandler hndlr = new ConsoleHandler();
		hndlr.setFormatter(new RdrLogFormatter());
		logger.addHandler(hndlr);
		logger.setLevel(Level.INFO);
		hndlr.setLevel(Level.ALL);  // don't do extra filtering on stuff sent to us by
		                            // the main logger

		// If nothing is specified, just provide a hint
		if(lArgs.length == 0){

			System.err.print("Data source ID file wasn't specified, use -h for help.\n");

			// Supposedly return values 1 and 2, and values 126 and above mean something for
			// the shell itself and shouldn't be used by application programs. See:
			// http://www.tldp.org/LDP/abs/html/exitcodes.html#EXITCODESREF
			System.exit(NO_DSID_SPECIFIED);
		}

		// If args[0] is special, provide more help
		for(String sTest: new String[]{"-h", "--help", "help"}){
			if(sTest.equals(lArgs[0].toLowerCase())){
				System.err.print(
  "\nRunRdr - Load and run a Das2 StreamSource from the command line.\n"
+ "\n"
+ "Usage:\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE [info]\n"
+ "\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE help\n"
+ "\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE key1=val1 key2=val2 ...\n"
+ "\n"
+ "Description:\n"
+ "  RunRdr parses a given Data Source ID file, loads the StreamSource Java class\n"
+ "  given in the file, parses the command line arguments into data selection\n"
+ "  parameters, and then runs the reader.\n"
+ "\n"
+ "  Two special arguments are supported 'help' and 'info'.  'Help' provides details on\n"
+ "  how to run the reader, and 'info' describes the data generated by the reader.\n"
+ "  If the only the DSID_FILE is supplied on the command line, then the program runs as\n"
+ "  if \"DSID_FILE info\" were the command line arguments.\n"
+ "\n"
				);
				System.exit(0); //Getting help is a normal thing to do.
			}
		}

		// Okay, args[0] is supposed to be a DSID file, let's validate it.
		DataSource ds;
		try{
			ds = new DataSource(lArgs[0]);
		}
		catch(SAXException ex){
			logger.log(Level.SEVERE, "DSID file "+lArgs[0] +" didn't pass validation, reason:\n\t"+
				        ex.getMessage());
			System.exit(DSID_INVALID);
		}
		catch(IOException ex){
			logger.log(Level.SEVERE, "Couldn't load DSID file, "+ex.toString());
			System.exit(DSID_IOERROR);
		}

		// Treat no arguments the same as asking for 'info'
	/*	if((lArgs.length == 1)||(lArgs[1].toLowerCase().equals("info")))
			System.exit(info(logger,dsid));

		// Putting 'help' anywhere will trigger the help function
		for(String sArg: lArgs){
			if(sArg.toLowerCase().equals("help"))
				System.exit(help(logger, dsid));
		}

		// Putting 'info' anywhere will trigger the help function
		for(String sArg: lArgs){
			if(sArg.toLowerCase().equals("info"))
				System.exit(info(logger, dsid));
		}

		// Standard run
		int nRet = run(logger, dsid);
		System.exit(nRet);
	  */

		System.exit(55);
	}
}
