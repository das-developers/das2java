package org.das2.qstream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.LoggerManager;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;


 // In general QDataSet conglomerations have the following possible structure
 // though many multi-element items may collapse to a single item:
 //
 // ds            is maybe list of 1-N datasets (join)  <stream>
 //  |
 // join
 //  |
 //  |- ds        is maybe a list of 1-N datasets (bundle)   <packet>
 //      |
 //    bundle
 //      |
 //      |- ds    maybe has dependencies and stats (regular)  <y> <yscan> <z>
 //         |
 //       dependency, statistic or plane
 //         |
 //         |- ds   Support datasets                      <x> <y> <yscan> <z>
 //  
 //
 // So where do we look for fundamental items such as the xTagWidth?  Well
 // They can be anywhere and you have to crawl the hierarchy to find out.  Also
 // different paths down the hierarchy may lead to different answers.

/** Write QDataSets that vary over at most 2 independent variables as a Das2 
 * stream.
 * Since Das2 streams can have at most 2 independent variables, higher
 * dimensional (not necessarily higher rank) QDataSets can't be written using
 * this code.  Streams that can be written have the following plane structure
 * 
 *   X Y [Y Y Y Y ... ]
 *   X YScan [YScan YScan YScan ... ]
 *   X Y Z [Z Z Z Z ... ]
 * 
 * All binary output is streamed in machine-native order.  Since datasets written
 * on one architecture are most likely to be read on the same architecture this
 * choice causes the least amount of byte swapping.  
 * 
 * This is a direct seralization of QDataSet and does not require any legacy
 * das2 dataset classes such as VectorDataset or TableDataset.
 */
public class QdsToD2sStream {
	
	public static final String FORMAT_2_2 = "2.2";
	public static final String FORMAT_2_3 = "2.3";
	public static final String[] formats = {FORMAT_2_2, FORMAT_2_3};
	
	private boolean bDas23;
	
	private String VERSION;
	private String X;
	private String Y;
	private String XSCAN;
	private String Z;
	private String YSCAN;
	private String TYPE;
	private String YTAGS;
	private String XTAGS;
	
	private static final Logger log = LoggerManager.getLogger("qstream");
	
	// Handling for text output, throw this back to the user if they want text
	// output.  The reason not to just do our best to figure it out on our own
	// is that it changes the packet header definitions, which we don't want to
	// do when writing cache files.
	int nSigDigit;
	int nSecDigit;
	
	// List of transmitted packet hdrs, index is the packet ID.
	private List<String> lHdrsSent = new ArrayList<>();  
	
	// A das2 limitation, not an arbitrary choice
	private static final int MAX_HDRS = 100;
	
	// Public interface //////////////////////////////////////////////////////
	
	/** Initialize a binary QDataSet to das2 stream exporter 
	 * @param version The output version to write, valid choices are defined 
	 *        in the static formats array
	 */
	public QdsToD2sStream(String version){ 
		nSigDigit = -1; nSecDigit = -1;
		bDas23 = version.equals(FORMAT_2_3);
		_setNames(bDas23);
	}
	
	/** Initialize a text, or partial text, QDataSet to das2 stream exporter
	 * 
	 * @param version The output version to write, valid choices are defined 
	 *        in the static formats array
	 * 
	 * @param genSigDigits The number of significant digits used for general
	 *    data output.  Set this to 1 or less to trigger binary output, use
	 *    a greater than 1 for text output.  If you don't know what else to
	 *    pick, 6 is typically a great precision without being ridiculous.
	 * 
	 * @param fracSecDigits  The number of fractional seconds digits to
	 *    use for ISO-8601 date-time values. Set this to -1 (or less) for
	 *    binary time output.  The number of fraction seconds can be set
	 *    as low as 0 and as high as 12 (picoseconds).  If successive time
	 *    values vary by less than the specified precision (but not 0,
	 *    repeats are accepted) then stream writing fails.  Use 3 (i.e.
	 *    microseconds) if you don't know what else to choose.
	 */
	public QdsToD2sStream(String version, int genSigDigits, int fracSecDigits){
		
		bDas23 = version.equals(FORMAT_2_3);
		_setNames(bDas23);
		if(genSigDigits > 1){
			if(genSigDigits > 16){
				throw new IllegalArgumentException(String.format(
					"Number of significant digits in the output must be between 2 "
					+ "and 17, received %d", genSigDigits
				));
			}
			nSigDigit = genSigDigits;
		}
		
		if(fracSecDigits >= 0){
			if(fracSecDigits < 0 || fracSecDigits > 12){
				throw new IllegalArgumentException(String.format(
					"Number of fractional seconds digits in the output must be "
					+ "between 0 and 12 inclusive, received %d", fracSecDigits
				));
			}
			nSecDigit = fracSecDigits;
		}
	}
	
	final void _setNames(boolean _bDas23){
		if(_bDas23){
			VERSION = "2.3";
			X = "X"; Y = "Y"; XSCAN = "YofX"; Z = "Z"; YSCAN = "ZofXY"; 
			TYPE = "format"; YTAGS = "yValues"; XSCAN = "YofX"; 
			XTAGS = "xOffsets";
		}
		else{
			VERSION = "2.2";
			X = "x"; Y = "y"; XSCAN = null;   Z = "z"; YSCAN = "yscan"; 
			TYPE = "type"; YTAGS = "yTags"; XSCAN = null; 
			XTAGS = null;
		}
	}
	
	/** Determine if a given dataset be serialized as a das2 stream
	 * @param qds The dataset to write
	 * @return true if this dataset can be serialized as a das2 stream, false
	 *         otherwise
	 */
	public boolean canWrite(QDataSet qds){
		// Joins are typically just appends.  We can use different packet types so
		// that's okay as long as we don't run out of packet IDs
		int nTypes = 0;
		if(SemanticOps.isJoin(qds)){
			for(int i = 0; i < qds.length(); ++i){
				if(!canWriteNonJoin(DataSetOps.slice0(qds, i ))) return false;
				if(nTypes++ > 99) return false;
			}
			return true;
		}
		return canWriteNonJoin(qds);
	}
	
	/** Write a QDataSet as a das2 stream
	 * 
	 * To test whether it looks like this code could stream a dataset use the
	 * canWrite() function.  This function may be called multiple times to add
	 * additional data to the stream.  If a compatable header has already been
	 * emitted for a particular dataset it is not re-sent.
	 * 
	 * @param qds The dataset to write, may have join's bundles etc. but no
	 *        rank 3 or higher component datasets.
	 * 
	 * @param os an open output stream, which is not closed by this code.
	 * 
	 * @return true if the entire dataset could be written, false otherwise.
	 *         IO failures do not return false, they throw.  False simply 
	 *         means that this code does not know how (or can't) stream the 
	 *         given dataset.  Since deeper inspection occurs when actually
	 *         writing the data then when testing, false may be returned even
	 *         if canWrite() returned true.
	 * @throws java.io.IOException
	 */
	public boolean write(QDataSet qds, OutputStream os) 
		throws IOException {
		
		if(! canWrite(qds)) return false;   // Try not to create invalid output
		
		String sPktHdr;
		List<String> lHdrsToSend = new ArrayList<>();
		
		Document doc; // XML document
		if(lHdrsSent.isEmpty()){
			if((doc = makeStreamHdr(qds)) == null) return false;
			lHdrsToSend.add( xmlDocToStr(doc) );
		}
		
		// Take advantage of the fact that we have all the data here to place all
		// headers first.  This way someone can use a text editor to inspect the
		// top of the stream, even for binary streams
		
		PacketXferInfo pi;
		List<PacketXferInfo> lPi = new ArrayList<>();
	
		if(SemanticOps.isJoin(qds)){
			for(int i = 0; i < qds.length(); ++i){
				QDataSet ds = DataSetOps.slice0(qds, i);
				if((pi = makePktHdr(ds)) == null) return false;
				else lPi.add(pi);
				sPktHdr = xmlDocToStr(pi.doc);
				if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
			}
		}
		else{
			if((pi = makePktHdr(qds)) == null) return false;
			else lPi.add(pi);
			sPktHdr = xmlDocToStr(pi.doc);
			if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
		}
		
		for(int i = 0; i < lHdrsToSend.size(); ++i){
			int iPktId = lHdrsSent.size();  // So zero is always stream header
			if(iPktId >= MAX_HDRS) return false;
			
			writeHeader(os, iPktId, lHdrsToSend.get(i));
			lHdrsSent.add(lHdrsToSend.get(i));
		}
		
		// Now write the data, find out packet ID by our header value
		int iPktId;
		for(PacketXferInfo pktinfo: lPi){
			sPktHdr = xmlDocToStr(pktinfo.doc);
			iPktId = lHdrsSent.indexOf(sPktHdr);
			writeData(iPktId, pktinfo, os);
		}
		
		return true;
	}
	
	// Top level helper functions and structures //////////////////////////////
	
	protected boolean canWriteNonJoin(QDataSet qds){
		// Bundles are used all over the place.  They just represent items that 
		// are covarying in the depend coordinates.  To determine this, bust the
		// bundle apart and track dependencies. 
		QDataSet dsDep;
		QDataSet dsDep0 = null;
		QDataSet dsDep1 = null;
		
		if(qds.property(QDataSet.BUNDLE_1) != null){
			for(int i = 0; i < qds.length(); ++i){
				QDataSet ds = DataSetOps.slice0(qds, i);
				
				// Ain't gonna handle bundles of bundles today
				if(ds.property(QDataSet.BUNDLE_1) != null) return false;
				
				// Can't do higher order sets in das2 simple streams, requires 
				// das2 general streams
				if(ds.rank() > 2) return false;  
				
				if(ds.rank() > 1){
					// Blind cast, is there any way to check this first?
					if( (dsDep = (QDataSet)ds.property(QDataSet.DEPEND_1)) != null){
						if(dsDep1 == null) dsDep1 = dsDep;
						else{
							// More than one dep1 running around
							if(dsDep != dsDep1) return false;
						}
					}
					
					// For now no one has come up with a concise way to say that 
					// one yscan is the "frequency" set for a second yscan, we should
					// do this but for now the answer is no, can't stream it
					if(dsDep.rank() > 1) return false;
				}
				if( (dsDep = (QDataSet)ds.property(QDataSet.DEPEND_0)) != null){
					if(dsDep0 == null) dsDep0 = dsDep;
					else{
						// More than one dep0 running around
						if(dsDep != dsDep0) return false;
					}
				}
				
				return true;  // Bundle looks representable
			}
		}
		
		// Not a bundle, just need to have a low enough rank for the main datasets
		// and the physical value index tags
		if(qds.rank() > 2) return false;
		
		if( (dsDep = (QDataSet) qds.property(QDataSet.DEPEND_1)) != null){
			if(dsDep.rank() > 1) return false;
		}
		
		// Ignore the following cases.  If they are doing this they should just
		// use QStream.
		String[] lTmp = {QDataSet.BUNDLE_0, QDataSet.BUNDLE_2, QDataSet.BUNDLE_3};
		for(String s: lTmp) if(qds.property(s) != null) return false;
		
		return true;  // We ran the gauntlet
	}
	
	// Make header for join, <stream>
	
	// For non-bundle datasets a lot of the properties can be placed in the
	// stream header and it's common to do so.  I am not using any of the 
	// defined strings from org.das2.DataSet since I'm assuming that package
	// is going away.
	private Document makeStreamHdr(QDataSet qds) {
		Document doc = newXmlDoc();
		
		Element stream = doc.createElement("stream");
		stream.setAttribute("version", VERSION);
		
		Element props = doc.createElement("properties");
		int nProps = 0;
		
		nProps += addStrProp(props, qds, QDataSet.TITLE, "title");
		nProps += addStrProp(props, qds, QDataSet.DESCRIPTION, "summary");
		nProps += addStrProp(props, qds, QDataSet.RENDER_TYPE, "renderer");
		
		String sAxis = qd2DataAxis(qds);
		if(sAxis == null) return null;
		
		nProps += addSimpleProps(props, qds, sAxis);
		
		// If the user_properties are present, add them in
		Map<String, Object> dUser;
		dUser = (Map<String, Object>)qds.property(QDataSet.USER_PROPERTIES);
		if(dUser != null) nProps += addPropsFromMap(props, dUser);
		
		// If the ISTP-CDF metadata weren't so long, we'd include these as well
		// maybe a Das 2.3 sub tag is appropriate for them, ignore for now...
		
		if(nProps > 0) stream.appendChild(props);
		
		doc.appendChild(stream);
		return doc;
	}
	
	// Helper structures for making packet headers and writing data ///////////
	
	// Make and hold the transfer information for a single QDS
	private final static int X_PLANE = 1;
	private final static int Y_PLANE = 2;
	private final static int Z_PLANE = 3;
	private final static int YSCAN_PLANE = 4;
	
	private class QdsXferInfo {
		QDataSet qds;
		TransferType transtype;
		String sType;
		int nPlane;
		
		String   sSource;     // Only used to indicate plane + stats groups
		String   sOperation;  // Only used to indicate satatistic type
		
		// Figure out how to represent the values.  In general everything is 
		// output as a float unless it's an epoch time type.
		QdsXferInfo(QDataSet _qds, int nGenDigits, int nFracSec, int _nPlane){
			this(_qds, nGenDigits, nFracSec, _nPlane, null, null);
		}
		
		// Extra constructor for statistics datasets.
		QdsXferInfo(QDataSet _qds, int nGenSigDigit, int nFracSec, int _nPlane, 
		            String _sSource, String sOp){

			sSource = _sSource;
			sOperation = sOp;
			qds = _qds;
			nPlane = _nPlane;
				
			Units units = (Units) qds.property(QDataSet.UNITS);
			if((units != null) && (units instanceof TimeLocationUnits)){
				if(nFracSec < 0){ 
					transtype = new DoubleTransferType();
					sType = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 
					     "little_endian_real8" : "sun_real8";
				}
				else{
					transtype = new D2TextTimeTransfer(units, nFracSec);
					sType = String.format("time%d", transtype.sizeBytes());
				}
			}
			else{
				//Non epoch stuff
				if(nGenSigDigit < 0){
					transtype = new FloatTransferType();
					sType = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 
					     "little_endian_real4" : "sun_real4";
				}
				else{
					transtype = new D2SciNoteTransfer(nGenSigDigit);
					sType = String.format("ascii%d", transtype.sizeBytes());
				}
			}
		}
	}
	
	// Hold the information for serializing a single packet and it's dependencies
	private class PacketXferInfo {
		Document doc;        // Overall header document
		List<QdsXferInfo> lDsXfer;
		
		PacketXferInfo(Document _doc, List<QdsXferInfo> _lDsXfer ){
			doc = _doc; lDsXfer = _lDsXfer;
		}
		
		int datasets(){ return lDsXfer.size(); }
		
		// Calc length of buffer needed to hold a single slice in the 0th
		// dimension across all qdatasets.
		int slice0Length() {
			int nLen = 0;
			for(QdsXferInfo qi: lDsXfer){ 
				int nItems = 1;
				if(qi.qds.rank() > 1) nItems = qi.qds.length(0);
				nLen += qi.transtype.sizeBytes() * nItems;
			}
			return nLen;
		}
	}
	
	// Make header for bundle, <packet>
	
	// For each member of the bundle
	//
	// 1. Get it's depend0, this is the X axis, if it's not the first make sure
	//    that the new depend0 is not different from the first one.  
	//
	// 2. If the member has no depend0 then it is an extra X axis, place it 
	//    after any X that arises as a depend 0
	//
	// 3. If it has a depend1, assume it's a <yscan>, if not it's a <y>.  
	//
	// <yscan> branch
	//
	//    a. See if depend1 has a depend0, we don't have a way to save these
	//       at this time.
	//
	//    b. See if it has a plane_0, this would mean it's a 4-D dataset,
	//       we don't have a way to deal with those at this time.
	//
	//    c. See if the depend1's can be saved as a sequence, if not save
	//       as ytags
	// 
	// <y> branch
	//
	//    a. See if it has a plane_0, if so plane_0 is a <z> item.  I don't
	//       think there is a pattern established yet for multi <z>'s but check
	//       that <z> is not a bundle.
	//
	// 4. If it has a BIN_MAX, BIN_MIN, DELTA_PLUS, DELTA_MINUS properties.  
	//    all these cause the creation of extra <y> or <yscans> with the same
	//    SOURCE and (if needed yTags) properties
	
	
	private PacketXferInfo makePktHdr(QDataSet qds) {
		
		List<QdsXferInfo> lDsXfer = new ArrayList<>();
		
		assert(!SemanticOps.isJoin(qds));  //Don't call this for join datasets
		
		QDataSet dep0;
		List<QDataSet> lDsIn = new ArrayList<>();
		List<QDataSet> lDsOut = new ArrayList<>();
		
		if(SemanticOps.isBundle(qds)){
			// Primary <X> handling: Maybe the bundle's depend_0 is <x>
			if( (dep0 = (QDataSet) qds.property(QDataSet.DEPEND_0)) != null)
				lDsXfer.add(new QdsXferInfo(dep0, nSigDigit, nSecDigit, X_PLANE));
			
			for(int i = 0; i < qds.length(); ++i)
				lDsIn.add( DataSetOps.slice0(qds, i) );
		}
		else{
			lDsIn.add(qds);
		}
		
		// Handle <x> planes
		for(QDataSet ds: lDsIn){
			// Maybe the bundle's members depend_0 is <x>
			if( (dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0)) != null){
				
				if(lDsXfer.isEmpty()){
					lDsXfer.add(new QdsXferInfo(dep0, nSigDigit, nSecDigit, X_PLANE));
				}
				else{
					if(dep0 != lDsXfer.get(0).qds){
						log.warning("Multiple independent depend_0 datasets in bundle");
						return null;
					}
				}
				lDsOut.add(ds);  // Used the depend0, but keep top level dataset
			}
			else{
				// Rank 1 with no depend 0 is an <x> plane
				if(lDsXfer.isEmpty() && (ds.rank() == 1))
					lDsXfer.add(new QdsXferInfo(ds, nSigDigit, nSecDigit, X_PLANE));
				// no-add
			}
		}
		
		// Handle <y> planes, these are always rank 1 and have a depend 0, if
		// you see a PLANE_0 save it out as a Z plane.
		String sFallBackSrcName;  // Correlating source fallback name
		int nNewPlanes;
		lDsIn = lDsOut;
		lDsOut = new ArrayList<>();
		QDataSet dsZ = null;
		QDataSet dsSubZ = null;
		int nYs = 0;
		int nZs = 0;
		for(QDataSet ds: lDsIn){
			if(ds.rank() > 1){ lDsOut.add(ds);  continue; }  //<yscan>
			
			assert(ds.property(QDataSet.DEPEND_0) != null);
			
			// Once we hit a Z plane we're done with y's and yscans
			if(dsZ != null){
				log.warning("Multiple Y planes encountered in X,Y,Z dataset");
				return null;
			}
			
			sFallBackSrcName = String.format("Y_%d", nYs);
			nNewPlanes = _addPlaneWithStats(
				lDsXfer, ds, nSigDigit, nSecDigit, Y_PLANE, sFallBackSrcName
			);
			if(nNewPlanes == 0) return null;
			nYs += nNewPlanes;
		
			if( (dsZ = (QDataSet) ds.property(QDataSet.PLANE_0)) != null){
				// If plane0 is a bundle we have multiple Z's
				if(SemanticOps.isBundle(dsZ)){
					for(int i = 0; i < dsZ.length(); ++i){
						dsSubZ = DataSetOps.slice0(dsZ, i);
						sFallBackSrcName = String.format("Z_%d", nZs);
						nNewPlanes = _addPlaneWithStats(
							 lDsXfer, dsSubZ, nSigDigit, nSecDigit, Z_PLANE, sFallBackSrcName
						);
						if(nNewPlanes == 0) return null;
						nZs += nNewPlanes;
					}
				}
				else{
					sFallBackSrcName = String.format("Z_%d", nZs);
					nNewPlanes = _addPlaneWithStats(
						lDsXfer, dsZ, nSigDigit, nSecDigit, Z_PLANE, sFallBackSrcName
					);
					if(nNewPlanes == 0) return null;
					nZs += nNewPlanes;
				}
			}
		}
		
		// Handle <yscan> planes.
		lDsIn = lDsOut;
		QDataSet dsYTags = null;
		QDataSet dep1;
		int nYScans = 0;
		for(QDataSet ds: lDsIn){
			if(ds.rank() > 2){ assert(false); return null; }
			if(dsZ != null){
				log.warning("YScan planes not allowed in the same packets as Z planes");
				return null;
			}
			
			if( (dep1 = (QDataSet)ds.property(QDataSet.DEPEND_1)) != null){
				if(dsYTags == null){ 
					dsYTags = dep1;
				}
				else{
					if(dsYTags != dep1){
						log.warning("Independent Y values for different rank-2 "
								      + " datasets in the same bundle");
						return null;
					}
				}
			}
			
			// Have our YTags
			sFallBackSrcName = String.format("YScan_%d", nYScans);
			nNewPlanes = _addPlaneWithStats(
				lDsXfer, ds, nSigDigit, nSecDigit, YSCAN_PLANE, sFallBackSrcName
			);
			if(nNewPlanes < 1) return null;
			nYScans += nNewPlanes;
		}
		
		// Now make the packet header
		Document doc; 
		if( (doc = _makePktHdrFromXfer(lDsXfer, dsYTags)) == null) return null;
		
		return new PacketXferInfo(doc, lDsXfer);
	}
	
	// Helper to setup transfer info for a plane and it's companion statistics
	// planes
	
	int _addPlaneWithStats(
		List<QdsXferInfo> lDsXfer, QDataSet dsPrimary, int nSigDigit, 
		int nSecDigit, int nPlane, String sFallBackSrc
	){
		QdsXferInfo xferPrimary;
		xferPrimary = new QdsXferInfo(dsPrimary, nSigDigit, nSecDigit, nPlane);
		lDsXfer.add(xferPrimary);
		
		// See if source was set.
		Map<String, Object> dUserProps;
		dUserProps = (Map<String,Object>) dsPrimary.property(QDataSet.USER_PROPERTIES);
		String sSource = null;
		if(dUserProps != null){
			if(dUserProps.containsKey("source"))
				sSource = (String)dUserProps.get("source");
		}
		else{
			sSource = (String)dsPrimary.property(QDataSet.NAME);
			if(sSource == null) sSource = sFallBackSrc;
		}
			
		// See if BIN_MIN or BIN_MAX datasets are present, these are planes with
		// the same rank as the initial plane.
		QDataSet dsStats;
		String sErr = "Statistics dataset is a different rank than the average dataset";
		String[] aProps = {
			QDataSet.BIN_MIN, QDataSet.BIN_MAX, QDataSet.BIN_MINUS, QDataSet.BIN_PLUS
		};
		int nStatsPlanes = 0;
		for(String sProp: aProps){
			if((dsStats = (QDataSet)dsPrimary.property(sProp)) != null){
				if(dsStats.rank() != dsPrimary.rank()){ 
					log.warning(sErr); 
					return 0; 
				}
				lDsXfer.add(new QdsXferInfo(dsStats, nSigDigit, nSecDigit, nPlane,
			                               sSource, sProp));
				++nStatsPlanes;
			}
		}
		if(nStatsPlanes > 0) xferPrimary.sSource = sSource;
		return 1 + nStatsPlanes;
	}
	
	Document _makePktHdrFromXfer(List<QdsXferInfo> lDsXfer, QDataSet dsYTags)
	{
		Document doc = newXmlDoc();
		Element elPkt = doc.createElement("packet");
		
		Element elPlane;
		int nProps;
		int nYs = 0;
		int nYscans = 0;
		int nZs = 0;
		
		YTagStrings yts = null;
		if(dsYTags != null) yts = _getYTagStrings(dsYTags, nSigDigit);
	
		for(QdsXferInfo xfer: lDsXfer){
			
			Element elProps = doc.createElement("properties");  // May not get used
			String sName = (String)xfer.qds.property(QDataSet.NAME);
			Units units  = (Units)xfer.qds.property(QDataSet.UNITS);
			String sUnits = "";
			if(units != null) sUnits = units.toString();
						
			switch(xfer.nPlane){
			
			case X_PLANE:
				elPlane = doc.createElement(X);
				elPlane.setAttribute("units", sUnits);
				if(sName != null) elPlane.setAttribute("group", sName);
				
				nProps = addSimpleProps(elProps, xfer.qds, "x");
				break;
			
			case Y_PLANE:
				elPlane = doc.createElement(Y);
				elPlane.setAttribute("units", sUnits);
				if(sName == null) sName = String.format("Y_%d", nYs);
				elPlane.setAttribute("group", sName);
				
				nProps = addSimpleProps(elProps, xfer.qds, "y");
				++nYs;
				break;
			
			case Z_PLANE:
				elPlane = doc.createElement(Z);
				elPlane.setAttribute("units", sUnits);
				if(sName == null) sName = String.format("Z_%d", nZs);
				elPlane.setAttribute("group", sName);
				
				nProps = addSimpleProps(elProps, xfer.qds, "z");
				++nZs;
				break;
			
			case YSCAN_PLANE:
				elPlane = doc.createElement(YSCAN);
				if(bDas23) elPlane.setAttribute("units", sUnits);
				else elPlane.setAttribute("zUnits", sUnits);
				
				if(sName == null) sName = String.format("YScan_%d", nYscans);
				elPlane.setAttribute("group", sName);
				
				//Extra stuff for YScans
				if(dsYTags == null){
					log.warning("Missing yTags dataset for yScan dataset");
					return null;
				}
				elPlane.setAttribute("nitems", String.format("%d",dsYTags.length()));
				
				units  = (Units)dsYTags.property(QDataSet.UNITS);
				sUnits = "";
				if(units != null) sUnits = units.toString();
				
				if(yts != null){
					if(yts.sYTags != null){
						if(bDas23){ 
							valueListChild(elPlane, YTAGS, sUnits, yts.sYTags);
						}
						else {
							elPlane.setAttribute(YTAGS, yts.sYTags);
							elPlane.setAttribute("yUnits", sUnits);
						}
					}
					else{
						elPlane.setAttribute("yUnits", sUnits);
						elPlane.setAttribute("yTagInterval", yts.sYTagInterval);
						elPlane.setAttribute("yTagMin", yts.sYTagMin);
					}
				}
				else{
					assert(false);
					log.warning("No YTags, for output yscans");
					return null;
				}
				
				nProps = addSimpleProps(elProps, dsYTags, "y");
				nProps += addSimpleProps(elProps, xfer.qds, "z");
				++nYscans;
				break;
				
			default:
				assert(false); return null;
			}
			
			// Common stuff here
			elPlane.setAttribute(TYPE, xfer.sType);
			
			// Linking stats planes and averages planes if source is given
			if(xfer.sSource != null){
				// Both averages plane and stats planes have a source set
				elProps.setAttribute("source", xfer.sSource);
				++nProps;
				if(xfer.sOperation != null){
					// But only the stats planes have an operation set
					elProps.setAttribute("operation", xfer.sOperation);
					++nProps;
				}
			}
			if(nProps > 0) elPlane.appendChild(elProps);
			elPkt.appendChild(elPlane);
		}
		
		doc.appendChild(elPkt);
		return doc;
	}
	
	// Getting string representation for ytags
	private class YTagStrings {
		String sYTagInterval = null;
		String sYTagMin = null;
		String sYTags = null;
	}
	
	YTagStrings _getYTagStrings(QDataSet qds, int nFracDigits){
		YTagStrings strs = new YTagStrings();
		
		if(qds.rank() != 1){
			log.warning("YTags must have rank 1 for now.");
			return null;
		}
		
		// Get my format string
		String sFmt = String.format("%%.%de", 4);
		
		double rMin = qds.value(0);
		if(qds.length() == 0){
			strs.sYTags = String.format(sFmt, rMin);
			return strs;
		}

		// Try to use sequence representation if we can, allow for jitter in
		// intervals of one part in 100,000.
		boolean bUseInterval = true;
		double rInterval = qds.value(1) - rMin;
		double rNextInterval, rAvg, rJitter;
		for(int i = 2; i < qds.length(); ++i){
			rNextInterval = qds.value(i) - qds.value(i - 1);
			rAvg = Math.abs(rNextInterval + rInterval)/2;
			rJitter = Math.abs(rNextInterval - rInterval) / rAvg;
			if(rJitter > 1e-5){
				bUseInterval = false;
				break;
			}
		}
		
		if(bUseInterval){
			strs.sYTagMin = String.format(sFmt, rMin);
			strs.sYTagInterval = String.format(sFmt, rInterval);
		}
		else{
			StringBuilder sb = new StringBuilder();
			// For Das 2.3 use whitespace to format the list nicely unless we are running
			// low on space
			String sNL = "\n            ";
			if(bDas23){
				sb.append(sNL);
				sb.append(String.format(sFmt, rMin));
				for(int i = 1; i < qds.length(); ++i){	
					sb.append(",");
					if( i % 8 == 0) sb.append(sNL);
					else sb.append(" ");
					sb.append(String.format(sFmt, qds.value(i)));
				}
				sb.append("\n        ");
			}
			else{
				sb.append(String.format(sFmt, rMin));
				for(int i = 1; i < qds.length(); ++i)
					sb.append(",").append(String.format(sFmt, qds.value(i)));
			}
			strs.sYTags = sb.toString();
		}
		
		return strs;
	}
	
	void valueListChild(
		Element elPlane, String sElement, String sUnits, String sValues
	){
		Document doc = elPlane.getOwnerDocument();
		Element el = doc.createElement(sElement);
		el.setAttribute("units", sUnits);
		Node text = doc.createTextNode(sValues);
		el.appendChild(text);
		elPlane.appendChild(el);
	}
	
	// Send Bundle data
	private void writeData(int iPktId, PacketXferInfo pktXfer, OutputStream out) 
		throws IOException {
		
		WritableByteChannel channel = Channels.newChannel(out);
		
		int nBufLen = pktXfer.slice0Length() + 4;
		byte[] aBuf = new byte[nBufLen];
		ByteBuffer buffer = ByteBuffer.wrap(aBuf);
		buffer.order(ByteOrder.nativeOrder());        // Since we have a choice, use native
		String sPktId = String.format(":%02d:", iPktId);
		buffer.put(sPktId.getBytes(StandardCharsets.US_ASCII));      // tag stays in buffer
		
		int nPkts = pktXfer.lDsXfer.get(0).qds.length();
		
		for(int iPkt = 0; iPkt < nPkts; ++iPkt){
			
			QdsXferInfo qi = null;
			for(int iDs = 0; iDs < pktXfer.datasets(); ++iDs){
				qi = pktXfer.lDsXfer.get(iDs);
				
				switch(qi.qds.rank()){
				case 1: qi.transtype.write(qi.qds.value(iPkt), buffer); break;
				case 2: 
					// Checked when making packet header that the data are a qube
					// since that's all das2 streams support, hence length(0) below.
					for(int iVal = 0; iVal < qi.qds.length(0); ++iVal)
						qi.transtype.write(qi.qds.value(iPkt, iVal), buffer);
					break;
				default:
					assert(false);
				}
			}
			if(qi != null && qi.transtype.isAscii()) 
				buffer.put(nBufLen - 1, (byte)'\n');
			buffer.flip();
			channel.write(buffer);
			buffer.position(4);
		}
	}	
	
	
	// Secondary helpers functions below here //////////////////////////////////
	
	private String xmlDocToStr(Document doc)
	{ 	
		DOMImplementation imp = doc.getImplementation();
		DOMImplementationLS ls = (DOMImplementationLS)imp.getFeature("LS", "3.0");
		LSSerializer serializer = ls.createLSSerializer();
		//DOMStringList props = serializer.getDomConfig().getParameterNames();
		serializer.getDomConfig().setParameter("format-pretty-print", true);
		serializer.getDomConfig().setParameter("xml-declaration", false);
		String sDoc = serializer.writeToString(doc);		
		return sDoc;
	}
	
	// Write a UTF-8 header string onto the stream
	private void writeHeader(OutputStream os, int nPktId, String sHdr) 
		throws UnsupportedEncodingException, IOException{
		
		byte[] aHdr = sHdr.getBytes(StandardCharsets.UTF_8);
		String sTag = String.format("[%02d]%06d", nPktId, aHdr.length);
		byte[] aTag = sTag.getBytes(StandardCharsets.US_ASCII);
		os.write(aTag);
		os.write(aHdr);
	}
	
	private Document newXmlDoc() {
		try {
			DocumentBuilder bldr = 
					  DocumentBuilderFactory.newInstance().newDocumentBuilder();
			return bldr.newDocument();
		} catch (ParserConfigurationException pce) {
			throw new RuntimeException(pce);
		}
	}
	
	int addBoolProp(Element props, String sName, Object oValue){
		String sValue = (Boolean)oValue ? "true" : "false";
		if(bDas23) _addChildProp(props, sName, "boolean", sValue);
		else props.setAttribute(sName, sValue);
		return 1;
	}
	
	int addStrProp(Element props, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return addStrProp(props, d2key, oProp);
		return 0;
	}
	int addStrProp(Element props, String sName, Object oValue){
		
		// Special handling for substitutions, Das2 Streams flatten metadata
		// so if a %{USER_PROPERTIES.thing} substitution string is being saved, 
		// flatten it back to just %{thing} so that it works when read again
		String sInput = (String)oValue;
		
		Pattern p = Pattern.compile("%\\{ *USER_PROPERTIES\\.");
		Matcher m = p.matcher(sInput);
		StringBuffer sb = new StringBuffer();
		while(m.find()) m.appendReplacement(sb, "%{");
		m.appendTail(sb);
		String sOutput = sb.toString();
		
		if(bDas23) _addChildProp(props, sName, null, sOutput);
		else props.setAttribute(sName, sOutput);
		return 1;
	}
	
	int addRealProp(Element props, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return addRealProp(props, d2key, oProp);
		return 0;
	}
	int addRealProp(Element props, String sName, Object oValue){
		Number num = (Number)oValue;
		String sVal = String.format("%.6e", num.doubleValue());
		if(bDas23) 	_addChildProp(props, sName, "double", sVal);
		else  props.setAttribute("double:"+sName, sVal);
		return 1;
	}
	
	int addDatumProp(Element props, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return addDatumProp(props, d2key, oProp);
		return 0;
	}
	int addDatumProp(Element props, String sName, Object oValue){
		Datum datum = (Datum)oValue;
		if(bDas23) 	_addChildProp(props, sName, "Datum", datum.toString());
		else  props.setAttribute("Datum:"+sName, datum.toString());
		return 1;
	}
	
	int addRngProp(Element props, QDataSet qds, String sMinKey, String sMaxKey,
			         String sUnitsKey, String d2key){
		Object oMin, oMax, oUnits;
		oMin = qds.property(sMinKey); oMax = qds.property(sMaxKey);
		oUnits = qds.property(sUnitsKey);
		
		if(oMin == null || oMax == null) return 0;
		Number rMin = (Number)oMin; Number rMax = (Number)oMax;
		
		String sValue;
		if(oUnits != null){
			String sUnits = ((Units)oUnits).toString();
			sValue = String.format("%.6e to %.6e %s", rMin.doubleValue(), 
			                       rMax.doubleValue(), sUnits);
		}
		else
			sValue = String.format("%.6e to %.6e", rMin.doubleValue(), 
			                       rMax.doubleValue());
		
		if(bDas23) 	_addChildProp(props, d2key, "DatumRange", sValue);
		else props.setAttribute("DatumRange:"+d2key, sValue);
		return 1;
	}
	int addRngProp(Element props, String sName, Object oValue){
		DatumRange rng = (DatumRange)oValue;
		
		// Work around a bug in DatumRange.  DatumRange can not read it's own
		// output.  For example a time range becomes:
		//    range.toString() => "2017-09-01 9:00 to 10:00"
		// But the input reader expects:
		//   "2017-09-01T9:00 to 2017-09-01T10:00 UTC"
		// or it fails
		
		Units units = rng.getUnits();
		String sOutput;
		if( units instanceof TimeLocationUnits){
			Datum dmMin = rng.min();
			Datum dmMax = rng.max();
			sOutput = String.format(
				"%s to %s UTC", dmMin.toString().replaceAll("Z", ""),
				dmMax.toString().replaceAll("Z", "")
			);
		}
		else{
			sOutput = rng.toString();
		}
		
		if(bDas23) 	_addChildProp(props, sName, "DatumRange", sOutput);
		else  props.setAttribute("DatumRange:"+sName, sOutput);
		return 1;
	}
	
	void _addChildProp(Element props, String sName, String sType, String sVal){
		Document doc = props.getOwnerDocument();
		Element prop = doc.createElement("prop");
		prop.setAttribute("name", sName);
		if(sType != null) prop.setAttribute("type", sType);
		Node text = doc.createTextNode(sVal);
		prop.appendChild(text);
		props.appendChild(prop);
	}
	
	// Get all the simple standard properties of a dataset and add these to the
	// attributes of a property element.  Complex properties dependencies and 
	// associated datasets are not handled here.  Returns the number of props
	// added.
	int addSimpleProps(Element props, QDataSet qds, String sAxis)
	{
		int nProps = 0;
		
		nProps += addStrProp(props, qds, QDataSet.FORMAT, sAxis + "Format");
		nProps += addStrProp(props, qds, QDataSet.SCALE_TYPE, sAxis + "ScaleType");
		nProps += addStrProp(props, qds, QDataSet.LABEL, sAxis + "Label");
		nProps += addStrProp(props, qds, QDataSet.DESCRIPTION, sAxis + "Summary");
		
		nProps += addRealProp(props, qds, QDataSet.FILL_VALUE, sAxis + "Fill");
		nProps += addRealProp(props, qds, QDataSet.VALID_MIN, sAxis + "ValidMin");
		nProps += addRealProp(props, qds, QDataSet.VALID_MAX, sAxis + "ValidMax");
		
		nProps += addRngProp(props, qds, QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
		                     QDataSet.UNITS, sAxis + "Range");
		
		// The cadence is an odd bird, a QDataSet with 1 item, handle special
		Object obj = qds.property(QDataSet.CADENCE);
		if(obj != null){
			QDataSet dsTmp = (QDataSet)obj;
			Units units = (Units) dsTmp.property(QDataSet.UNITS);
			if(units == null) units = Units.dimensionless;
			Datum dtm = Datum.create(dsTmp.value(), units);
			
			if(bDas23) _addChildProp(props, sAxis+"TagWidth", "Datum", dtm.toString());
			else props.setAttribute("Datum:"+ sAxis + "TagWidth", dtm.toString());
			++nProps;
		}
		
		return nProps;
	}
	
	// Get all the user_data properties that don't conflict with any properties
	// already present.  These don't care about prepending x,y,z axis identifiers
	// to the attribute tag, though they may have them and that's okay.
	int addPropsFromMap(Element props, Map<String, Object> dMap){
		
		if(dMap == null) return 0;
		int nAdded = 0;
		
		for(Entry<String,Object> ent: dMap.entrySet()){
			String sKey = ent.getKey();
			
			// For das2.2 ask about attributes, for das2.3 ask about child elements
			if(bDas23){
				NodeList nl = props.getElementsByTagName("prop");
				boolean bHasItAlready = false;
				for(int i = 0; i < nl.getLength(); ++i){
					Element el = (Element)nl.item(i);
					if(el.hasAttribute(sKey)){ bHasItAlready = true; break; }
				}
				if(bHasItAlready) continue;
			}
			else{
				if(props.hasAttribute(sKey)) continue;
			}
			
			Object oVal = ent.getValue();
			if(oVal instanceof Boolean)
				nAdded += addBoolProp(props, sKey, oVal);
			else
				if(oVal instanceof String)
					nAdded += addStrProp(props, sKey, oVal);
				else
					if(oVal instanceof Number)
						nAdded += addRealProp(props, sKey, oVal);
					else
						if(oVal instanceof Datum)
							nAdded += addDatumProp(props, sKey, oVal);
						else
							if(oVal instanceof DatumRange)
								nAdded += addRngProp(props, sKey, oVal);
			
		}
		return nAdded;
	}
	

	///////////////////////////////////////////////////////////////////////////
	// Das2 specific dataset information functions that could just be an add on
	// for qdataset.  These could be move somewhere else.  If we were writing
	// in D universal function call syntax would allow for:
	//   
	//   qds.qd2DataAxis();
	
	/** Determine the name of the das2 axis on which values from a dataset
	 * would typically be plotted.
	 * 
	 * This is a duck-typing check.  Which looks at the number of dependencies 
	 * and planes in a dataset.  If the dataset has a PLANE_0 property, the axis
	 * of the PLANE_0 values is returned instead of the axis of the primary
	 * dataset.  The output of this function is an educated guess since coordinates
	 * are not denoted as separate from data values in QDataSets or CDFs.
	 * 
	 * @param qds
	 * @return one of "x", "y", "z" or null if we can't figure it out.
	 */
	public static String qd2DataAxis(QDataSet qds){
		
		//If join just look at the first element of the join
		if(SemanticOps.isJoin(qds)) qds = DataSetOps.slice0(qds, 0);
		
		//If see if the bundle is bigger than size 1.
		if(SemanticOps.isBundle(qds)){
			if(qds.rank() == 1){  //trival bundle
				qds = DataSetOps.slice0(qds, 0);
				// now handle as single dataset below
			}
			else{
				// I'm continuing the false assmption that paramenter space 
				// dimensionality ~= rank which is the the core of the CDF/QDataSet
				// path dataset problem.   We will have to face this head on someday.
				// The only indicator that a rank 1 item is actually a Z value is the
				// existance of a PLANE_0 property.
				int nMaxRank = 0;
				for(int i = 0; i < qds.length(); ++i){
					QDataSet ds = DataSetOps.slice0(qds, i);
					int nRank = ds.rank();
					if(ds.property(QDataSet.PLANE_0) != null)
						//throw new UnsupportedOperationException(
						//	"Cannot determine the canonical axis for bundles which contain "+
						//	"datasets which have a PLANE_0 property."
						//);
						return null;
					
					if(nRank > nMaxRank) nMaxRank = ds.rank();
				}
				if(nMaxRank == 1) return "y";
				if(nMaxRank == 2) return "z";
				return null;
			}
		}
		
		//Okay, not a bundle.  To decide our axis just check our dependencies
		//rank, and planes.  Here's the swiss cheese check:
		// rank 0 -> X
		// rank 1 with no depend_0 -> X
		// rank 1 with a depend_0 -> Y
		// rank 1 with a depend_0 and plane_0 -> Z (rem to pull out plane for Z's)
		// rank 2 -> Z
		
		switch(qds.rank()){
		case 0: return "x";
		case 2: return "z";
		case 1: 
			if(qds.property(QDataSet.DEPEND_0) == null) return "x";
			if(qds.property(QDataSet.PLANE_0) == null) return "y";
			return "z";
		}
		return null;
	}	
	
	// Custom ISO time transfer type handles fractional seconds at arbitrary
	// precision instead of ms, microsec, and nanosec only
	private class D2TextTimeTransfer extends TransferType{
		
		Units units;
		DatumFormatter formatter;
		byte[] aFill;
		int nSize;
		
		/** Create a time datum formatter with a variable length number of
		 * seconds 
		 * 
		 * @param units the units of the double values to convert to an ascii
		 *        time
		 * @param nFracSec Any value from 0 to 12 (picoseconds) inclusive.
		 */
		public D2TextTimeTransfer(Units units, int nFracSec)
		{
			this.units = units;
			
			// yyyy-mm-ddThh:mm:ss.ssssssssssss +1 for space at end
			// 12345678901234567890123456789012
			nSize = 20;
			String sFmt =  "yyyy-MM-dd'T'HH:mm:ss";
			String sFill = "                   ";
			
			if(nFracSec > 0){ 
				sFmt += ".";
				sFill += " ";
			}
			for(int i = 0; i < nFracSec; ++i){ 
				sFmt += "S";
				sFill += " ";
			}
			sFmt += " "; sFill += " ";
			
			aFill = sFill.getBytes(StandardCharsets.US_ASCII);
			try{
				formatter = new TimeDatumFormatter(sFmt);
			}
			catch(ParseException ex){
				throw new RuntimeException(ex);
			}
			
			if(nFracSec > 0) ++nSize;  //decimal point
			nSize += nFracSec;
		}
		
		@Override
		public void write(double rVal, ByteBuffer buffer) {
			if(units.isFill(rVal)){
				buffer.put(aFill);
			}
			else{
				String sOut = formatter.format(units.createDatum(rVal));
				buffer.put(sOut.getBytes(StandardCharsets.US_ASCII));
			}
		}

		@Override
		public double read(ByteBuffer buffer){
			try{
				byte[] aBuf = new byte[nSize];
				buffer.get(aBuf);
				String sTime = new String(aBuf, "US-ASCII").trim();
				double result = TimeUtil.create(sTime).doubleValue(units);
				return result;
			}
			catch(UnsupportedEncodingException | ParseException e){
				throw new RuntimeException(e);
			}
		}

		@Override
		public int sizeBytes(){ return nSize; }

		@Override
		public boolean isAscii(){ return true; }

		@Override
		public String name(){ return String.format("time%d", nSize); }
	}

	// Custom local transfer type for exponential notation, use a small 'e'
	// and don't print + signs.
	private class D2SciNoteTransfer extends TransferType {

		final int nLen;
		private String sFmt;

		public D2SciNoteTransfer(int nSigDigits) {
			if (nSigDigits < 2 || nSigDigits > 17) {
				throw new IllegalArgumentException(String.format(
						  "Significant digits for output must be between 2 and 17 "
						  + "inclusive, recieved %d", nSigDigits
				));
			}
			// 7 = room for sign(1), decimal(1), exponent (4) and trailing space(1)
			nLen = nSigDigits + 7;
			sFmt = String.format("%%.%de", nSigDigits - 1);
		}

		@Override
		public void write(double rVal, ByteBuffer buffer) {
			String sVal = String.format(sFmt, rVal);
			if (rVal >= 0.0) {
				buffer.put((byte) 32);  // take up room used by - sign
			}
			buffer.put(sVal.getBytes(StandardCharsets.US_ASCII));
			buffer.put((byte) 32);
		}

		@Override
		public double read(ByteBuffer buffer) {
			byte[] bytes = new byte[nLen];
			buffer.get(bytes);
			String str;
			try {
				str = new String(bytes, StandardCharsets.US_ASCII).trim();
				return Double.parseDouble(str);
			} catch (NumberFormatException ex) {
				return Double.NaN;
			}
		}
		
		@Override
		public int sizeBytes()   { return nLen; }
		@Override
		public boolean isAscii() { return true; }
		@Override
		public String name()     { return "ascii" + nLen; }
	}
}