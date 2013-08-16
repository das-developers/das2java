/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.IOException;
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
	public static final int BAD_DSID_FILE     = 50;
	public static final int NOT_YET_IMPLEMENTED = 99;


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
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE [--log=LEVEL] [--info] [--help]\n"
+ "\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE [--log=LEVEL] key1.OP.val1 key2.OP.val2 ...\n"
+ "\n"
+ "  java -cp dasCore.jar org.das2.reader.RunRdr DSID_FILE [--log=LEVEL] --das2time.EQ.key start stop key1.OP.val1 key2.OP.val2 ...\n"
+ "\n"
+ "Description:\n"
+ "  RunRdr parses a given Data Source ID file, loads the StreamSource Java class\n"
+ "  given in the file, parses the command line arguments into data selection\n"
+ "  parameters, and then runs the reader.  In general command line options take the\n"
+ "  form:\n"
+ "\n"
+ "     KEYWORD.OP.VALUE\n"
+ "\n"
+ "  Where '.OP.' is one of: .eq. .ne. .lt. .gt. .le. .ge. .beg. .end.\n"
+ "  These are the standard FORTRAN numeric operators plus beg and end, .beg. is just\n"
+ "  a synonym for .ge. and .end. is a synonym for .lt.\n"
+ "\n"
+ "  Four special arguments are supported '--log' --help', '--info' and '--das2times'.\n"
+ "\n"
+ "  --log=LEVEL\n"
+ "     Set the logging verbosity.  Log output is sent to the Standard Error channel.\n"
+ "     Use one of: 'error', 'warning', 'info', 'debug', 'trace'.  Error displays the\n"
+ "     least amount of information, finest the most.  The default is info.\n"
+ "\n"
+ "  --help\n"
+ "     Provide details on how to run the reader.\n"
+ "\n"
+ "  --info\n"
+ "    Describes the data source, including the type of data provided.\n"
+ "\n"
+ "  --das2times.eq.KEY\n"
+ "    Turn on Das2 reader command line compatibility.  This will cause the first two\n"
+ "    arguments in the query string that don't have an operator, and which are not\n"
+ "    recognized as a special argument to be treated as a range data selector with\n"
+ "    the key name KEY.  Typically KEY is 'scet'.\n"
+ "\n"
+ "  If the only the DSID_FILE is supplied on the command line, then the program runs as\n"
+ "  if \"DSID_FILE --info\" were the command line arguments.\n"
+ "\n"
				);
				System.exit(0); //Getting help is a normal thing to do.
			}
		}

		// Okay, args[0] is supposed to be a DSID file, let's validate it.
		DsidRdrSource ds = null;
		try{
			ds = new DsidRdrSource(lArgs.get(0));
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
			if(sArg.toLowerCase().contains("--das2times.eq.")){
				int iTmp = sArg.indexOf(".eq.");
				if(iTmp == sArg.length() - 4){
					logger.log(Level.SEVERE, "Missing time argument name in '"+sArg+"'");
					System.exit(BAD_QUERY);
				}
				String sTimeKey = sArg.substring(iTmp+4);
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
			logger.log(Level.SEVERE, "Bad query: " + ex.getMessage(), ex);
			System.exit(BAD_QUERY);
		}
		catch(ReaderDefException ex){
			logger.log(Level.SEVERE, "Bad Setup: " + ex.getMessage());
			System.exit(BAD_DSID_FILE);
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
				logger.log(Level.SEVERE, "Query problem: "+ ex.getMessage());
				System.exit(BAD_QUERY);
			}
			catch(UnsupportedOperationException ex){
				logger.log(Level.SEVERE, "That feature is not yet implemented: " + ex.toString());
				System.exit(NOT_YET_IMPLEMENTED);
			}
			catch(ReaderDefException ex){
				logger.log(Level.SEVERE, "Data Source XML file error: " + ex.toString());
				System.exit(BAD_DSID_FILE);
			}
		}
		catch(IOException ex){
			logger.log(Level.SEVERE, ex.toString());
			System.exit(OUTPUT_IOERROR);
		}

		System.exit(0);
	}
}
