package org.das2.qstream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.das2.datum.Datum;
import org.das2.datum.LoggerManager;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


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
	
	private static final Logger log = LoggerManager.getLogger("qstream");
	
	// Handling for text output, throw this back to the user if they want text
	// output.  The reason not to just do our best to figure it out on our own
	// is that it changes the packet header definitions, which we don't want to
	// do when writing cache files.
	int nSigDigit;
	int nFracSecDigit;
	
	// List of transmitted packet hdrs, index is the packet ID.
	private List<String> lHdrsSent = new ArrayList<>();  
	
	// A das2 limitation, not an arbitrary choice
	private static final int MAX_HDRS = 100;
	
	// Public interface //////////////////////////////////////////////////////
	
	/** Initialize a binary QDataSet to das2 stream exporter */
	public QdsToD2sStream(){ 
		nSigDigit = -1; nFracSecDigit = -1;
	}
	
	/** Initialize a text, or partial text, QDataSet to das2 stream exporter
	 * 
	 * @param nSigDigit The number of significant digits used for general
	 *    data output.  Set this to 1 or less to trigger binary output, use
	 *    a greater than 1 for text output.  If you don't know what else to
	 *    pick, 6 is typically a great precision without being ridiculous.
	 * 
	 * @param nFacSecDigits  The number of fractional seconds digits to
	 *    use for ISO-8601 date-time values. Set this to -1 (or less) for
	 *    binary time output.  The number of fraction seconds can be set
	 *    as low as 0 and as high as 12 (picoseconds).  If successive time
	 *    values vary by less than the specified precision (but not 0,
	 *    repeats are accepted) then stream writing fails.  Use 3 (i.e.
	 *    microseconds) if you don't know what else to choose.
	 */
	public QdsToD2sStream(int genSigDigits, int fracSecDigits){
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
			nFracSecDigit = fracSecDigits;
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
	 * 
	 * @throws javax.xml.transform.TransformerException  if the dom is bad, which
	 *         shouldn't happen
	 * @throws java.io.IOException
	 */
	public boolean write(QDataSet qds, OutputStream os) 
		throws TransformerException, IOException {
		
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
			int iPktId = lHdrsSent.size() + i;
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
		stream.setAttribute("version", "2.2");
		
		Element props = doc.createElement("properties");
		int nProps = 0;
		
		nProps += addStrProp(props, qds, QDataSet.TITLE, "title");
		nProps += addStrProp(props, qds, QDataSet.DESCRIPTION, "summary");
		nProps += addStrProp(props, qds, QDataSet.RENDER_TYPE, "renderer");
		
		String sAxis = qd2DataAxis(qds);
		if(sAxis == null) return null;
		
		nProps += addSimpleProps(props, qds, sAxis);
		if(nProps > 0) stream.appendChild(props);
		
		doc.appendChild(stream);
		return doc;
	}
	
	// Helper structures for making packet headers and writing data ///////////
	
	// Make and hold the transfer information for a single QDS
	private class QdsXferInfo {
		QDataSet qds;
		TransferType transtype;
		String sType;
		
		// Figure out how to represent the values.  In general everything is 
		// output as a float unless it's an epoch time type.
		QdsXferInfo(QDataSet _qds, int nGenDigits, int nFracSec){
			qds = _qds;
				
			Units units = (Units) qds.property(QDataSet.UNITS);
			if((units != null) && (units instanceof TimeLocationUnits)){
				if(nFracSec < 0){ 
					transtype = new DoubleTransferType();
					sType = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 
					     "little_endian_real8" : "sun_real8";
				}
				else{
					transtype = new AsciiVariableTimeTransType(units, nFracSec);
					sType = String.format("time%d", transtype.sizeBytes());
				}
			}
			else{
				//Non epoch stuff
				if(nGenDigits < 0){
					transtype = new FloatTransferType();
					sType = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 
					     "little_endian_real4" : "sun_real4";
				}
				else{
					transtype = new AsciiTransferType(nGenDigits + 7, true);
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
			for(QdsXferInfo qi: lDsXfer) nLen += qi.transtype.sizeBytes();
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
		Document doc = newXmlDoc();
		List<QdsXferInfo> lDsXfer = new ArrayList<>();
		lDsXfer.add(null);  // Depend 0 goes here, if we have one
		lDsXfer.add(null);  // Depend 1 goes here for X,Y,Z datasets
		
		assert(!SemanticOps.isJoin(qds));  //Don't call this for join datasets
		
		Element elPkt = doc.createElement("packet");
		
		String sDataAxis = qd2DataAxis(qds);
		if(sDataAxis == null) return null;   // Can't determine data values axis
		
		List<QDataSet> lDs = new ArrayList<>();
		if(!SemanticOps.isBundle(qds))
			for(int i = 0; i < qds.length(); ++i)
				lDs.add( DataSetOps.slice0(qds, i) );
		else
			lDs.add(qds);
		
		QDataSet dsX0 = null;
		QDataSet dsY0 = null;  // All yTags must match
		for(QDataSet ds: lDs){
			QDataSet dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0);
			if(dep0 != null){
				if(lDsXfer.get(0) == null){
					lDsXfer.set(0, new QdsXferInfo(dep0, nSigDigit, nFracSecDigit));
				}  
				else{
					if(dsX0 != dep0){ 
						log.warning("Multiple independent X-value sets in dataset");
						return null;
					}
				}
			}
		}
		
		return new PacketXferInfo(doc, lDsXfer);
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
	
	private String xmlDocToStr(Document doc) throws TransformerException
	{ 
		// Shamelessly copied from stack overflow user "digitalsanctum" at URL
		// https://stackoverflow.com/questions/315517/is-there-a-more-elegant-way-to-convert-an-xml-document-to-a-string-in-java-than
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		StreamResult result = new StreamResult(new StringWriter());
		DOMSource source = new DOMSource(doc);
		transformer.transform(source, result);
		return result.getWriter().toString();
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
	
	int addStrProp(Element el, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null){
			el.setAttribute(d2key, (String)oProp);
			return 1;
		}
		return 0;
	}
	
	int addUnitsProp(Element el, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null){
			Units units = (Units)oProp;
			el.setAttribute(d2key, units.toString());
			return 1;
		}
		return 0;
	}
	
	int addRealProp(Element el, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null){
			Number num = (Number)oProp;
			el.setAttribute("double:"+d2key, String.format("%.6e", num.doubleValue()));
			return 1;
		}
		return 0;
	}
	
	int addRngProp(Element el, QDataSet qds, String sMinKey, String sMaxKey,
			         String sUnitsKey, String d2key){
		Object oMin, oMax, oUnits;
		oMin = qds.property(sMinKey); oMax = qds.property(sMaxKey);
		oUnits = qds.property(sUnitsKey);
		
		if(oMin == null || oMax == null) return 0;
		Number rMin = (Number)oMin; Number rMax = (Number)oMax;
		
		String sValue;
		if(oUnits != null){
			String sUnits = ((Units)oUnits).toString();
			sValue = String.format("%.6e to %.6e %s", rMax.doubleValue(), 
			                       rMax.doubleValue(), sUnits);
		}
		else
			sValue = String.format("%.6e to %.6e", rMax.doubleValue(), 
			                       rMax.doubleValue());
		
		el.setAttribute("DatumRange:"+d2key, sValue);
		return 1;
	}
	
	// Get all the simple standard properties of a dataset and add these to the
	// attributes of a property element.  Complex properties dependencies and 
	// associated datasets are not handled here.  Returns the number of props
	// added.
	int addSimpleProps(Element el, QDataSet qds, String sAxis)
	{
		int nProps = 0;
		
		nProps += addStrProp(el, qds, QDataSet.FORMAT, sAxis + "Format");
		nProps += addStrProp(el, qds, QDataSet.SCALE_TYPE, sAxis + "ScaleType");
		nProps += addStrProp(el, qds, QDataSet.LABEL, sAxis + "Label");
		nProps += addStrProp(el, qds, QDataSet.DESCRIPTION, sAxis + "Summary");
		
		nProps += addUnitsProp(el, qds, QDataSet.UNITS, sAxis + "Units");
		
		nProps += addRealProp(el, qds, QDataSet.FILL_VALUE, sAxis + "Fill");
		nProps += addRealProp(el, qds, QDataSet.VALID_MIN, sAxis + "ValidMin");
		nProps += addRealProp(el, qds, QDataSet.VALID_MAX, sAxis + "ValidMax");
		
		nProps += addRngProp(el, qds, QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
		                     QDataSet.UNITS, sAxis + "Range");
		
		// The cadence is an odd bird, a QDataSet with 1 item, handle special
		Object obj = qds.property(QDataSet.CADENCE);
		if(obj != null){
			QDataSet dsTmp = (QDataSet)obj;
			Units units = (Units) dsTmp.property(QDataSet.UNITS);
			if(units == null) units = Units.dimensionless;
			Datum dtm = Datum.create(dsTmp.value(), units);
			
			el.setAttribute("Datum:"+ sAxis + "TagWidth", dtm.toString());
			++nProps;
		}
		
		return nProps;
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
	
	public static String qd2DataUnits(QDataSet qds){
		if(SemanticOps.isJoin(qds)) qds = DataSetOps.slice0(qds, 0);
		
		return SemanticOps.getUnits(qds).toString();
	}
	
	
	public class AsciiVariableTimeTransType extends TransferType{
		
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
		public AsciiVariableTimeTransType(Units units, int nFracSec)
		{
			this.units = units;
			
			// yyyy-mm-ddThh:mm:ss.ssssssssssss +1 for space at end
			// 12345678901234567890123456789012
			nSize = 20;
			String sFmt =  "yyyy-MM-dd'T'HH:mm:ss' '";
			String sFill =        "                    ";
			
			if(nFracSec > 0){ 
				sFmt += ".";
				sFill += " ";
			}
			for(int i = 0; i < nFracSec; ++i){ 
				sFmt += "S";
				sFill += " ";
			}
			
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
}
