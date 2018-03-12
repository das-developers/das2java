package org.das2.qstream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
 * This is a direct seralization of QDataSet and does not require any legacy
 * Das2 classes.
 */

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
 //       dependency (or statistic)
 //         |
 //         |- ds   Support datasets                      <x> <y> <yscan> <z>
 //  
 //
 // So where do we look for fundamental items such as the xTagWidth?  Well
 // They can be anywhere and you have to crawl the hierarchy to find out.  Also
 // different paths down the hierarchy may lead to different answers.
 
public class QdsToD2sStream {
	
	boolean bText;                        // Flag to send text variants of values
	
	// List of transmitted packet hdrs, index is the packet ID.
	List<String> lHdrsSent = new ArrayList<>();  
	
	// A das2 limitation, not an arbitrary choice
	public static int MAX_HDRS = 100;  
	
	/** Initialize a writer
	 * @param bText If true, format all data values as text before pushing them
	 *        onto the stream
	 */
	public QdsToD2sStream(boolean bText){
		this.bText = bText;
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
	 * @return true if the entire dataset could be written, false otherwise
	 *         a return of false is not used for IO failures but only for
	 *         datasets that are not understood by this code.  
	 * 
	 * @throws javax.xml.transform.TransformerException  if the dom is bad, which
	 *         shouldn't happen
	 * @throws java.io.IOException
	 */
	public boolean write(QDataSet qds, OutputStream os) 
		throws TransformerException, IOException {
		
		if(! canWrite(qds)) return false;   // Try not to create invalid output
		
		Document doc; // XML document
		String sPktHdr;
		List<String> lHdrsToSend = new ArrayList<>();
		
		if(lHdrsSent.isEmpty()){
			if((doc = makeStreamHdr(qds)) == null) return false;
			lHdrsToSend.add( xmlDocToStr(doc) );
		}
		
		// Take advantage of the fact that we have all the data here to place all
		// headers first.  This way someone can use a text editor to inspect the
		// format, even for binary streams
		if(SemanticOps.isJoin(qds)){
			for(int i = 0; i < qds.length(); ++i){
				QDataSet ds = DataSetOps.slice0(qds, i);
				if((doc = makePktHdr(ds)) == null) return false;
				sPktHdr = xmlDocToStr(doc);
				if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
			}
		}
		else{
			if((doc = makePktHdr(qds)) == null) return false;
			sPktHdr = xmlDocToStr(doc);
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
		if(SemanticOps.isJoin(qds)){
			for(int i = 0; i < qds.length(); ++i){
				QDataSet ds = DataSetOps.slice0(qds, i);
				if((doc = makePktHdr(ds)) == null) return false;
				sPktHdr = xmlDocToStr(doc);
				
				iPktId = lHdrsSent.indexOf(sPktHdr);
				writeData(iPktId, ds);
			}
			return true;  // Done with bundle
		}
		
		if((doc = makePktHdr(qds)) == null) return false;
		sPktHdr = xmlDocToStr(doc);
				
		iPktId = lHdrsSent.indexOf(sPktHdr);
		writeData(iPktId, qds);
		return true;
	}
	
	// Top level helper functions below here //////////////////////////////////
	
	
	///////////////////////////////////////////////////////////////////////////
	// Make header for join
	
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
		
		String sAxis = null;
		if(!SemanticOps.isJoin(qds) && !SemanticOps.isBundle(qds) && 
			(qds.property(QDataSet.PLANE_0) != null)){
			
			// Top level dataset is hiding in a plane, pull it out.
			qds = (QDataSet)qds.property(QDataSet.PLANE_0);
			sAxis = "z";
		}
		if(sAxis == null) sAxis = getPropAxis(qds);
		if(sAxis == null) return null;
		
		nProps += addSimpleProps(props, qds, sAxis);
		if(nProps > 0) stream.appendChild(props);
		
		doc.appendChild(stream);
		return doc;
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Make header for bundle 
	
	private Document makePktHdr(QDataSet qds) {
		Document doc = newXmlDoc();
		
		assert(!SemanticOps.isJoin(qds));  //Don't call this for join datasets
		
		Element packet = doc.createElement("packet");
		
		// Find dep0
		QDataSet dep0 = findDep0(qds);
		String sType = null;
		String sUnits = null;
		if(dep0 != null){
			// It is legal to have a stream with no <x>, just really odd.
			Element X = doc.createElement("x");
			X.setAttribute("type", sType);
			X.setAttribute("units", sUnits);
			Element props = doc.createElement("properties");
			if(addSimpleProps(props, dep0, "x") > 0)
				X.appendChild(props);
			packet.appendChild(X);
		}
		
		if(SemanticOps.isBundle(qds)){
		
			for(int i = 0; i < qds.length(); ++i){
				QDataSet dsPlane = DataSetOps.slice0(qds, i);
				
				//They may have bundled in depend 0, don't send that
				if(dsPlane == dep0) continue;
				
				
			}
		}
		else{
			
		}
		
		return doc;
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Send Bundle data
	
	private void writeData(int iPktId, QDataSet ds) {
		
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
		
		byte[] aRec = sHdr.getBytes(StandardCharsets.UTF_8);
		byte[] aPktHdr = String.format("[%02d]%06d", nPktId, aRec.length).
				                  getBytes(StandardCharsets.US_ASCII);
		os.write(aPktHdr);
		os.write(aRec);
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
	
	// Based on the number of dependencies, get the property axis for top-level
	// properties of this dataset, returns one of "x", "y", "z" or null if 
	// we can't figure it out.
	String getPropAxis(QDataSet qds){
		
		//If join just look at the first element of the join
		if(SemanticOps.isJoin(qds)) qds = DataSetOps.slice0(qds, 0);
		//If see if the bundle is bigger than size 1.  If so assume
		if(SemanticOps.isBundle(qds)){
			if(qds.rank() == 1){  //trival bundle
				qds = DataSetOps.slice0(qds, 0);
				if(qds.property(QDataSet.PLANE_0) != null)
					throw new UnsupportedOperationException(
						"Don't use this function with QDataSets that have Z values "+
						"hiding in PLANE_0"
				);
				if(qds.rank() == 1) return "y";
				if(qds.rank() == 2) return "z";
				return null;
			}
			else{
				// I'm continuing the false assmption that rank = paramenter space
				// dimensionality which is the the core of the CDF/QDataSet problem.
				// We will have to face this head on someday.  The only indicator
				// that a rank 1 item is actually a Z value is the existance of
				// a PLANE_0 property.
				int nMaxRank = 0;
				for(int i = 0; i < qds.length(); ++i){
					QDataSet ds = DataSetOps.slice0(qds, i);
					int nRank = ds.rank();
					if(ds.property(QDataSet.PLANE_0) != null)
						throw new UnsupportedOperationException(
							"Don't use this function with QDataSets that have Z "+
							"values hiding in PLANE_0"
						);			
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
		// rank 1 with a depend_0 and plane_0 -> Y (rem to pull out plane for Z's)
		// rank 2 -> Z
		if(qds.property(QDataSet.PLANE_0) != null)
			throw new UnsupportedOperationException(
				"Don't use this function with QDataSets that have Z "+
				"values hiding in PLANE_0"
			);			
		
		// All this guessing could be avoided if the dimensionality were 
		// explicitly denoted in the dataset object, das2 general streams address
		// this problem, of course they will probably never get used.
		switch(qds.rank()){
		case 0: return "x";
		case 2: return "z";
		case 1: 
			if(qds.property(QDataSet.DEPEND_0) == null) return "x";
			return "y";
		}
		return null;
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

	private QDataSet findDep0(QDataSet qds){
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
}
