/* This Java package, org.das2.qstream is part of the Autoplot application
 *
 * Autoplot is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation, with the Classpath exception below.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * containing the Classpath exception clause along with this library; if not, write
 * to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 *
 * Classpath Exception
 * -------------------
 * The copyright holders designate this particular java package as subject to the
 * "Classpath" exception as provided here.
 *
 * Linking this package statically or dynamically with other modules is making a
 * combined work based on this package.  Thus, the terms and conditions of the GNU
 * General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this package give you
 * permission to link this package with independent modules to produce an
 * application, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting application under terms of your choice,
 * provided that you also meet, for each independent module, the terms and
 * conditions of the license of that module.  An independent module is a module
 * which is not derived from or based on this package.  If you modify this package,
 * you may extend this exception to your version of the package, but you are not
 * obligated to do so.  If you do not wish to do so, delete this exception
 * statement from your version.
 */
package org.das2.qstream;

// In general QDataSet conglomerations have the following possible structure

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.LoggerManager;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.Units;
import org.das2.qds.BundleDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import static org.das2.qstream.QdsToD2sStream.getQdsAxis;

 // though many multi-element items may collapse to a single item:
 //
 // ds     is maybe list of 1-N datasets (join)  <stream>
 //  |
 // join
 //  |
 //  |- ds     is maybe a list of 1-N datasets (bundle)   <packet>
 //      |
 //    bundle
 //      |
 //      |- ds   <x> <yset> <y> <zset> <z> <wset> <w>
 //         |
 //       dependency, statistic or plane
 //         |
 //         |- ds   Support datasets                  <x> <y> <z> <xcoord> <ycoord>
 //  
 //
 
/** Write QDataSets that vary over at most 3 independent variables as a Das2 
 * stream.
 * 
 * This is the general structure of arrays streamed in a das2.3 stream
 * 
 * x [yset yset ...] [y y ...] [zset zset ...] [z z ...] [wset wset ...] [w w ...]
 * 
 * Though it's unlikely that all possible components will ever be needed to
 * describe a single dataset.
 * 
 * All binary output is streamed in machine-native order.  Since datasets written
 * on one architecture are most likely to be read on the same architecture this
 * choice causes the least amount of byte swapping.  
 * 
 * This is a direct serialization of QDataSet and does not require any legacy
 * das2 classes
 */
public class QdsToDas23 extends QdsToD2sStream {
	
	private static final Logger log = LoggerManager.getLogger("qstream");
	
	// List of transmitted Hx hdrs, index is the packet ID.
	private List<String> lHdrsSent = new ArrayList<>();
	
	public QdsToDas23(){
		super();
	}	
	
	public QdsToDas23(int genSigDigits, int fracSecDigits){
		super(genSigDigits, fracSecDigits);
	}
	
	/** Determine if a given dataset be serialized as a das2.3/basic stream
	 * @param qds The dataset to write
	 * @return true if this dataset can be serialized as a das2.3/basic stream,
	 *         false otherwise
	 */
	@Override
	public boolean canWrite(QDataSet qds){
		// Joins are typically just appends.
		if(SemanticOps.isJoin(qds)){
			for(int i = 0; i < qds.length(); ++i){
				if(!_canWriteNonJoin(DataSetOps.slice0(qds, i ))) return false;
			}
			return true;
		}
		return _canWriteNonJoin(qds);
	}
	
	/** Write a QDataSet as a das2 stream
	 * 
	 * To test whether it looks like this code could stream a dataset use the
	 * canWrite() function.  This function may be called multiple times to add
	 * additional data to the stream.  If a compatible header has already been
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
	@Override
	public boolean write(QDataSet qds, OutputStream os) 
		throws IOException {
		
		if(! canWrite(qds)) return false;   // Try not to create invalid output
		
		String sPktHdr;
		List<String> lHdrsToSend = new ArrayList<>();
		
		Document doc; // XML document
		if(lHdrsSent.isEmpty()){
			if((doc = _makeStreamHdr(qds)) == null) return false;
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
				if((pi = _makePktHdr(ds)) == null) return false;
				else lPi.add(pi);
				sPktHdr = xmlDocToStr(pi.doc);
				if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
			}
		}
		else{
			if((pi = _makePktHdr(qds)) == null) return false;
			else lPi.add(pi);
			sPktHdr = xmlDocToStr(pi.doc);
			if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
		}
		
		for(int i = 0; i < lHdrsToSend.size(); ++i){
			int iPktId = lHdrsSent.size();  // So zero is always stream header
			
			writeHeader(os, VAR_PKT_TAGS, iPktId, lHdrsToSend.get(i));
			lHdrsSent.add(lHdrsToSend.get(i));
		}
		
		// Now write the data, find out packet ID by our header value
		int iPktId;
		for(PacketXferInfo pktinfo: lPi){
			sPktHdr = xmlDocToStr(pktinfo.doc);
			iPktId = lHdrsSent.indexOf(sPktHdr);
			writeData(os, VAR_PKT_TAGS, iPktId, pktinfo);
		}
		
		return true;
	}
	
	// Top level helper functions and structures //////////////////////////////
	
	protected boolean _canWriteNonJoin(QDataSet qds){
		// Bundles are used all over the place.  They aren't real datasets just
		// collections of real datasets that are covarying in thier depents.
		// To determine this, bust the bundle apart and track dependencies. 
		
		if(qds instanceof BundleDataSet){
			for(int i = 0; i < qds.length(0); ++i){
				QDataSet ds = ((BundleDataSet)qds).unbundle(i);
				
				// Ain't gonna handle bundles of bundles today
				if(ds instanceof BundleDataSet) return false;
				
				// Can't do rank 4 das2.2 streams unless we break them down into
				// subunits.  Not sure how that would work at this point
				if(ds.rank() > 3) return false;  
			}
			return true;  // Bundle looks representable I guess
		}
		
		// Not a bundle, just need to have a low enough rank for the main datasets
		// and the physical value index tags
		if(qds.rank() > 3) return false;
		
		return true;  // We ran the gauntlet, though not much was tested
	}
	
	
	// Make header for join, <stream>
	
	// For non-bundle datasets a lot of the properties can be placed in the
	// stream header and it's common to do so.  I am not using any of the 
	// defined strings from org.das2.DataSet since I'm assuming that package
	// is going away.
	private Document _makeStreamHdr(QDataSet qds) {
		Document doc = newXmlDoc();
		
		Element stream = doc.createElement("stream");
		stream.setAttribute("version", FORMAT_2_3_BASIC);
		
		Element props = doc.createElement("properties");
		int nProps = 0;
		
		nProps += addStrProp(props, qds, QDataSet.TITLE, "title");
		nProps += addStrProp(props, qds, QDataSet.DESCRIPTION, "summary");
		nProps += addStrProp(props, qds, QDataSet.RENDER_TYPE, "renderer");
		
		String sAxis = getQdsAxis(qds);
		if(sAxis == null) return null;
		
		nProps += addSimpleProps(props, qds, sAxis);
		
		// If the user_properties are present, add them in
		Map<String, Object> dUser;
		dUser = (Map<String, Object>)qds.property(QDataSet.USER_PROPERTIES);
		
		boolean bStripDot = _stripDotProps(qds);
		if(dUser != null) nProps += addPropsFromMap(props, dUser, bStripDot);
		
		// If the ISTP-CDF metadata weren't so long, we'd include these as well
		// maybe a Das 2.3 sub tag is appropriate for them, ignore for now...
		
		if(nProps > 0) stream.appendChild(props);
		
		doc.appendChild(stream);
		return doc;
	}
	
	// Make header for bundle, <packet>
	
	// To do this we need to find all the physical dimension in play.  This is 
	// difficult as multiple physdims can be glopped together into a bundle and
	// there are three different types of bundles and some of these have planes!
	//
	// Most of the logic here is from the 2.2 converter, even though 2.3 has more
	// output capabilities.  It will have to grow case-by-case over time since I
	// just can't make sense out of the QDataSet conglomerations.
	
	// For each real dataset (not a bundle)
	//
	// 1. Get it's depend0, this is the X axis.  If there is more than one X-axis
	//    then use xcoord attributes to reference the right one.
	//
	// 2. If the member has no depend0 then it is an extra X axis
	//
	// 2. If it has a depend1, it is either a <yset>, <zset> or <wset>
	//
	// <yset><zset> branch
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
	
	
	private PacketXferInfo _makePktHdr(QDataSet qds) {
		
		List<QdsXferInfo> lDsXfer = new ArrayList<>();
		
		assert(!SemanticOps.isJoin(qds));  //Don't call this for join datasets
		
		QDataSet dep0;
		List<QDataSet> lDsIn = new ArrayList<>();
		List<QDataSet> lDsOut = new ArrayList<>();
		
		// Bundle is usually short for BUNDLE_1
		if(SemanticOps.isBundle(qds)){
			// Primary <X> handling: Maybe the bundle's depend_0 is <x>
			if( (dep0 = (QDataSet) qds.property(QDataSet.DEPEND_0)) != null)
				lDsXfer.add(new QdsXferInfo(dep0, bBinary, nSigDigit, nSecDigit));
			
			for(int i = 0; i < qds.length(0); ++i)
				lDsIn.add( DataSetOps.slice1(qds, i) );
		}
		else{
			lDsIn.add(qds);
		}
		
		// Handle <x> planes
		for(QDataSet ds: lDsIn){
			// Maybe the bundle's members depend_0 is <x>
			if( (dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0)) != null){
				
				if(lDsXfer.isEmpty()){
					lDsXfer.add(new QdsXferInfo(dep0, bBinary, nSigDigit, nSecDigit));
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
					lDsXfer.add(new QdsXferInfo(ds, bBinary, nSigDigit, nSecDigit));
				// no-add
			}
		}
		
		// Handle <y> & <z> planes, these are always rank 1 and have a depend 0, if
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
			
			//nNewPlanes = _addPlaneWithStats(
			//	lDsXfer, ds, nSigDigit, nSecDigit, Y_EL, sFallBackSrcName
			//);
			//if(nNewPlanes == 0) return null;
			//nYs += nNewPlanes;
		
			if( (dsZ = (QDataSet) ds.property(QDataSet.PLANE_0)) != null){
				// If plane0 is a bundle we have multiple Z's
				if(SemanticOps.isBundle(dsZ)){
					for(int i = 0; i < dsZ.length(); ++i){
						dsSubZ = DataSetOps.slice0(dsZ, i);
						sFallBackSrcName = String.format("Z_%d", nZs);
						//nNewPlanes = _addPlaneWithStats(
						//	 lDsXfer, dsSubZ, nSigDigit, nSecDigit, Z_EL, sFallBackSrcName
						//);
				//		if(nNewPlanes == 0) return null;
//						nZs += nNewPlanes;
					}
				}
				else{
					sFallBackSrcName = String.format("Z_%d", nZs);
//					nNewPlanes = _addPlaneWithStats(
//						lDsXfer, dsZ, nSigDigit, nSecDigit, Z_EL, sFallBackSrcName
//					);
//					if(nNewPlanes == 0) return null;
//					nZs += nNewPlanes;
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
			//sFallBackSrcName = String.format("%s_%d", bDas23 ? "Zset":"YScan", nYScans);
			//nNewPlanes = _addPlaneWithStats(
			//	lDsXfer, ds, nSigDigit, nSecDigit, ZSET_EL, sFallBackSrcName
			//);
			//if(nNewPlanes < 1) return null;
			//nYScans += nNewPlanes;
		}
		
		// Now make the packet header
		Document doc;
		//doc = _make23PktHdrFromXfer(lDsXfer, dsYTags);
		//if(doc == null) return null;
		
		
		//return new PacketXferInfo(doc, lDsXfer);
		return null;
	}

	

	String _getValueSet(QDataSet ds){
		String sFmt = String.format("%%.%de", nSigDigit);
		StringBuilder sb = new StringBuilder();
		String sNL = "\n            ";
		
		sb.append(sNL);
		sb.append(String.format(sFmt, ds.value(0)));
		for(int i = 1; i < ds.length(); ++i){	
			sb.append(",");
			if( i % 8 == 0) sb.append(sNL);
			else sb.append(" ");
			sb.append(String.format(sFmt, ds.value(i)));
		}
		sb.append("\n        ");
		return sb.toString();
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
	
	int addBoolProp(Element props, String sName, Object oValue){
		String sValue = (Boolean)oValue ? "true" : "false";
		_addChildProp(props, sName, "boolean", sValue);
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
		
		_addChildProp(props, sName, null, sOutput);
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
		_addChildProp(props, sName, "double", sVal);
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
		_addChildProp(props, sName, "Datum", datum.toString());
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
		
		_addChildProp(props, d2key, "DatumRange", sValue);
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
		
		_addChildProp(props, sName, "DatumRange", sOutput);
		return 1;
	}
	
	void _addChildProp(Element props, String sName, String sType, String sVal){
		Document doc = props.getOwnerDocument();
		Element prop = doc.createElement("p");
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
			
			_addChildProp(props, sAxis+"TagWidth", "Datum", dtm.toString());
			++nProps;
		}
		
		return nProps;
	}
	
	// Get all the user_data properties that don't conflict with any properties
	// already present.  These don't care about prepending x,y,z axis identifiers
	// to the attribute tag, though they may have them and that's okay.
	int addPropsFromMap(Element props, Map<String, Object> dMap, boolean bStripDot){
		
		if(dMap == null) return 0;
		int nAdded = 0;
		
		for(Map.Entry<String,Object> ent: dMap.entrySet()){
			String sKey = ent.getKey();
			if(bStripDot && sKey.contains(".")) continue;
			
			//for das2.3 ask about child elements
			NodeList nl = props.getElementsByTagName("prop");
			boolean bHasItAlready = false;
			for(int i = 0; i < nl.getLength(); ++i){
				Element el = (Element)nl.item(i);
				if(el.hasAttribute(sKey)){ bHasItAlready = true; break; }
			}
			if(bHasItAlready) continue;
						
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
	
}