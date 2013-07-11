/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	public static final int DATA_FILE_IOERROR = 7;
	public static final int OUTPUT_IOERROR    = 8;
	public static final int BAD_QUERY         = 10;
	public static final int BAD_SCHEMA_FILE   = 42;


	/////////////////////////////////////////////////////////////////////////////////////
	
	/** A Main function for running an arbitrary stream source */
	@SuppressWarnings("ManualArrayToCollectionCopy")
	static public void main( String[] vArgs){

		List<String> lArgs = new LinkedList<String>();
		for(String sArg: vArgs) lArgs.add(sArg);

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
		if(lArgs.isEmpty()){

			System.err.print("Data source ID file wasn't specified, use -h for help.\n");

			// Supposedly return values 1 and 2, and values 126 and above mean something for
			// the shell itself and shouldn't be used by application programs. See:
			// http://www.tldp.org/LDP/abs/html/exitcodes.html#EXITCODESREF
			System.exit(NO_DSID_SPECIFIED);
		}

		// If args[0] is special, provide more help
		for(String sTest: new String[]{"-h", "--help", "help"}){
			if(sTest.equals(lArgs.get(0).toLowerCase())){
				System.err.print(
  "\nRunRdr - Load and run a Das2 StreamSource from the command line.\n"
+ "\n"
+ "Usage:\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE [--info]\n"
+ "\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE --help\n"
+ "\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE key1=val1 key2=val2 ...\n"
+ "\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE --das2time=key start stop key1=val1 key2=val2 ...\n"
+ "\n"
+ "Description:\n"
+ "  RunRdr parses a given Data Source ID file, loads the StreamSource Java class\n"
+ "  given in the file, parses the command line arguments into data selection\n"
+ "  parameters, and then runs the reader.\n"
+ "\n"
+ "  Three special arguments are supported '--help', '--info' and '--das2times'.  '--help'\n"
+ "  provides details on how to run the reader. '--info' describes the data generated \n"
+ "  by the reader.  Putting '--das2times' in the query string will cause the reader to \n"
+ "  to interperate arguments with out an equals sign to be start and end times.  This\n"
+ "  allows the reader to be compatible with Das2 server reader call semantics.\n"
+ "\n"
+ "  If the only the DSID_FILE is supplied on the command line, then the program runs as\n"
+ "  if \"DSID_FILE info\" were the command line arguments.\n"
+ "\n"
				);
				System.exit(0); //Getting help is a normal thing to do.
			}
		}

		// Okay, args[0] is supposed to be a DSID file, let's validate it.
		DataSource ds = null;
		try{
			ds = new DataSource(lArgs.get(0));
		}
		catch(SAXException ex){
			logger.log(Level.SEVERE, "DSID file "+lArgs.get(0) +" didn't pass validation, "
				        + "reason:\n\t"+ ex.getMessage());
			System.exit(DSID_INVALID);
		}
		catch(IOException ex){
			logger.log(Level.SEVERE, "Couldn't load DSID file, "+ex.toString());
			System.exit(DSID_IOERROR);
		}

		// Treat no arguments the same as asking for 'info'
		if(lArgs.size() == 1){
			String sInfo = ds.getInfo();
			System.err.print(sInfo);
			System.exit(0);
		}
		
		
		// Putting 'help' anywhere will trigger the help function
		for(String sArg: lArgs){
			if((sArg.toLowerCase().equals("--help"))||(sArg.toLowerCase().equals("-h"))){
				String sHelp = ds.getHelp();
				System.err.print(sHelp);
				System.exit(0);
			}
		}
		
		
		// Putting 'info' anywhere will trigger the help function
		for(String sArg: lArgs){
			if((sArg.toLowerCase().equals("--info"))||(sArg.toLowerCase().equals("-i"))){
				String sInfo = ds.getInfo();
				System.err.print(sInfo);
				System.exit(0);
			}
		}

		// Check to see if --das2 is in the arg list, if so set the query parser to be
		// das2 compatible
		int iPop = -1;
		for(String sArg: lArgs){
			iPop += 1;
			if(sArg.toLowerCase().contains("--das2times=")){
				int iTmp = sArg.indexOf('=');
				if(iTmp == sArg.length() - 1){
					logger.log(Level.SEVERE, "Missing time argument name in '"+sArg+"'");
					System.exit(BAD_QUERY);
				}
				String sTimeKey = sArg.substring(iTmp+1);
				ds.setDas2Compatible(sTimeKey);
				lArgs.remove(iPop);
				break;
			}
		}
		

		// Standard run
		List<String> lQueryArgs = lArgs.subList(1, lArgs.size());
		List<Selector> lQuery = null;
		try{
			lQuery = ds.parseQuery(lQueryArgs);
		}
		catch(BadQueryException ex){
			logger.log(Level.SEVERE, "Bad query: " + ex.getMessage());
			System.exit(BAD_QUERY);
		}

		try{
			try{
				Reader rdr = ds.newReader();
				rdr.retrieve(lQuery, OutputFormat.QSTREAM, System.out, logger);
			}
			catch(IOException ex){
				logger.log(Level.SEVERE, null, ex);
				System.exit(DATA_FILE_IOERROR);
			}
			catch(NoDataException ex){
				String sMsg = "No records within the selection interval";

				logger.log(Level.WARNING, sMsg, ex);
				DasHdrBuf buf = new DasHdrBuf(0);
				buf.add("<stream dataset_id=\"ds_0\" />\n");
				buf.send(System.out);
				buf.add("<exception type=\"NoDataInInterval\" message=\""+sMsg+"\"/>\n");
				buf.send(System.out);
			}
			catch(BadQueryException ex){
				logger.log(Level.SEVERE, "Internal error: BadQueryException, {0}.  "
					           + "please notifiy the reader maintainer.", ex.toString());
				System.exit(BAD_QUERY);
			}
		}
		catch(IOException ex){
			logger.log(Level.SEVERE, ex.toString());
			System.exit(OUTPUT_IOERROR);
		}

		System.exit(0);
	}
}
