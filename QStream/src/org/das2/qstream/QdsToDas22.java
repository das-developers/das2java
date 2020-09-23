/* This Java package, org.das2.qstream is part of the Autoplot application
 *
 * Copyright (C) 2018 Chris Piker <chris-piker@uiowa.edu>
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
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
import static org.das2.qstream.QdsToD2sStream.getQdsAxis;

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
 * This is a direct serialization of QDataSet and does not require any legacy
 * das2 dataset classes such as VectorDataset or TableDataset.
 */
public class QdsToDas22 extends QdsToD2sStream {
	
	private static final Logger log = LoggerManager.getLogger("qstream");
	
	// A das2 limitation, not an arbitrary choice
	private static final int MAX_HDRS = 100;

	// List of transmitted packet hdrs, index is the packet ID.
	private List<String> lHdrsSent = new ArrayList<>();
	
	public QdsToDas22(){
		super();
	}	
	
	public QdsToDas22(int genSigDigits, int fracSecDigits){
		super(genSigDigits, fracSecDigits);
	}
	
	/** Determine if a given dataset be serialized as a das2.2 stream
	 * @param qds The dataset to write
	 * @return true if this dataset can be serialized as a das2.2 stream, false
	 *         otherwise
	 */
	@Override
	public boolean canWrite(QDataSet qds){
		// Joins are typically just appends.  We can use different packet types so
		// that's okay as long as we don't run out of packet IDs
		int nTypes = 0;
		if(SemanticOps.isJoin(qds)){
			for(int i = 0; i < qds.length(); ++i){
				if(!_canWriteNonJoin(DataSetOps.slice0(qds, i ))) return false;
				if(nTypes++ > 99) return false;
			}
			return true;
		}
		return _canWriteNonJoin(qds);
	}
	
	/** Write a QDataSet as a das2.2 stream
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
		
		PacketXferInfo pxi;
		List<PacketXferInfo> lPi = new ArrayList<>();
	
		if(SemanticOps.isJoin(qds)){
			for(int i = 0; i < qds.length(); ++i){
				QDataSet ds = DataSetOps.slice0(qds, i);
				if((pxi = _makePktXferInfo(ds)) == null) return false;
				else lPi.add(pxi);
				sPktHdr = xmlDocToStr(pxi.doc);
				if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
			}
		}
		else{
			if((pxi = _makePktXferInfo(qds)) == null) return false;
			else lPi.add(pxi);
			sPktHdr = xmlDocToStr(pxi.doc);
			if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
		}
		
		for(int i = 0; i < lHdrsToSend.size(); ++i){
			int iPktId = lHdrsSent.size();  // So zero is always stream header
			if(iPktId >= MAX_HDRS) return false;
			
			writeHeader(os, FIXED_PKT_TAGS, iPktId, lHdrsToSend.get(i));
			lHdrsSent.add(lHdrsToSend.get(i));
		}
		
		// Now write the data, find out packet ID by our header value
		int iPktId;
		for(PacketXferInfo pktinfo: lPi){
			sPktHdr = xmlDocToStr(pktinfo.doc);
			iPktId = lHdrsSent.indexOf(sPktHdr);
			writeData(os, FIXED_PKT_TAGS, iPktId, pktinfo);
		}
		
		return true;
	}
	
	// Top level helper functions and structures //////////////////////////////
	
	protected boolean _canWriteNonJoin(QDataSet qds) {
		// Bundles are used all over the place.  They just represent items that 
		// are covarying in the depend coordinates.  To determine this, bust the
		// bundle apart and track dependencies. 
		QDataSet dsDep;
		QDataSet dsDep0 = null;
		QDataSet dsDep1 = null;

		if(qds instanceof BundleDataSet){
			for (int i = 0; i < qds.length(0); ++i) {
				QDataSet ds = ((BundleDataSet)qds).unbundle(i);
				
				// Ain't gonna handle bundles of bundles today
				if(ds instanceof BundleDataSet) return false;

				// Can't do rank 3+ in das2.2
				if (ds.rank() > 2) return false;
				
				if (ds.rank() > 1) {
					// Blind cast, is there any way to check this first?
					dsDep = (QDataSet) ds.property(QDataSet.DEPEND_1);
					if (dsDep != null) {
						if (dsDep1 == null) {
							dsDep1 = dsDep;
							log.log(Level.FINE, "dsDep1: {0}", dsDep1);
						} else {
							// More than one dep1 running around
							if (dsDep != dsDep1) {
								return false;
							}
						}
					}

					// For now no one has come up with a concise way to say that 
					// one yscan is the "frequency" set for a second yscan, we should
					// do this but for now the answer is no, can't stream it
					if (dsDep != null && dsDep.rank() > 1) {
						return false;
					}
				}
				if ((dsDep = (QDataSet) ds.property(QDataSet.DEPEND_0)) != null) {
					if (dsDep0 == null) {
						dsDep0 = dsDep;
						log.log(Level.FINE, "dsDep0: {0}", dsDep0);
					} else {
						// More than one dep0 running around
						if (dsDep != dsDep0) {
							return false;
						}
					}
				}
			}
			return true;  // Bundle looks representable
		}

		// Not a bundle, just need to have a low enough rank for the main datasets
		// and the physical value index tags
		if (qds.rank() > 2) return false;

		if ((dsDep = (QDataSet) qds.property(QDataSet.DEPEND_1)) != null) {
			if (dsDep.rank() > 1) return false;
		}

		// Ignore the following cases, since I don't know what they mean
		String[] lTmp = {QDataSet.BUNDLE_0, QDataSet.BUNDLE_2, QDataSet.BUNDLE_3};
		for (String s : lTmp) {
			if (qds.property(s) != null) {
				return false;
			}
		}

		return true;  // We ran the gauntlet
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Make header for join, <stream>
	
	// For non-bundle datasets a lot of the properties can be placed in the
	// stream header and it's common to do so.  I am not using any of the 
	// defined strings from org.das2.DataSet since I'm assuming that package
	// is going away.
	private Document _makeStreamHdr(QDataSet qds) {
		Document doc = newXmlDoc();
		
		Element stream = doc.createElement("stream");
		stream.setAttribute("version", FORMAT_2_2);
		
		Element props = doc.createElement("properties");
		int nProps = 0;
		
		nProps += addStrProp(props, qds, QDataSet.TITLE, "title");
		nProps += addStrProp(props, qds, QDataSet.DESCRIPTION, "summary");
		nProps += addStrProp(props, qds, QDataSet.RENDER_TYPE, "renderer");
		
		String sAxis = getQdsAxis(qds);
		if(sAxis == null) return null;
		
		nProps += _addSimpleProps(props, qds, sAxis);
		
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
	
	
	///////////////////////////////////////////////////////////////////////////
	// Make <packet> header for bundle and transfer info in parallel
	//
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
	
	
	private PacketXferInfo _makePktXferInfo(QDataSet qds) throws IOException {
		
		List<QdsXferInfo> lDsXfer = new ArrayList<>();
		Document doc = newXmlDoc();
		Element elPkt = doc.createElement("packet");
		doc.appendChild(elPkt);
		
		assert(!SemanticOps.isJoin(qds));  //Don't call this for join datasets
		
		QDataSet dep0;
		
		// We go through the list of datasets multiple times, picking out the 
		// items we care about.  In each stage lDsToRead is the list to process,
		// lDsRemain are the remaining items
		List<QDataSet> lDsToRead = new ArrayList<>();
		List<QDataSet> lDsRemain = new ArrayList<>(); 
		
		if(qds instanceof BundleDataSet){
			// Primary <X> handling: Maybe the bundle's depend_0 is <x>
			if( (dep0 = (QDataSet) qds.property(QDataSet.DEPEND_0)) != null)
				_addPhysicalDimension(elPkt, lDsXfer, "x", dep0);
			
			for(int i = 0; i < qds.length(0); ++i)
				lDsToRead.add( ((BundleDataSet)qds).unbundle(i) );
		}
		else{
			lDsToRead.add(qds);
		}
		
		// Handle <x> planes
		for(QDataSet ds: lDsToRead){
			// Maybe the bundle's members depend_0 is <x>
			if( (dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0)) != null){
				
				if(lDsXfer.isEmpty()){
					_addPhysicalDimension(elPkt, lDsXfer, "x", dep0);
				}
				else{
					if(dep0 != lDsXfer.get(0).qds){
						log.warning("Multiple independent depend_0 datasets in bundle");
						return null;
					}
				}
				lDsRemain.add(ds);  // Used the depend0, but keep top level dataset
			}
			else{
				// Rank 1 with no depend 0 is an <x> plane
				if(lDsXfer.isEmpty() && (ds.rank() == 1))
					_addPhysicalDimension(elPkt, lDsXfer, "x", ds);
			}
		}
		
		// Handle <y><z> planes, these are always rank 1 and have a depend 0, if
		// you see a PLANE_0 save it out as a Z plane.
		int nNewArrays;
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		QDataSet dsZ = null;
		QDataSet dsSubZ;
		
		for(QDataSet ds: lDsToRead){
			if(ds.rank() > 1){ lDsRemain.add(ds);  continue; }  //<yscan>
			
			assert(ds.property(QDataSet.DEPEND_0) != null);
			
			// Once we hit a Z plane we're done with y's and yscans
			if(dsZ != null){
				log.warning("Multiple Y planes encountered in X,Y,Z dataset");
				return null;
			}
						
			nNewArrays = _addPhysicalDimension(elPkt, lDsXfer, "y", ds);
			if(nNewArrays == 0) return null;
		
			if( (dsZ = (QDataSet) ds.property(QDataSet.PLANE_0)) != null){
				// If plane0 is a bundle we have multiple Z's
				if(SemanticOps.isBundle(dsZ)){
					for(int i = 0; i < dsZ.length(); ++i){
						dsSubZ = DataSetOps.slice0(dsZ, i);
						
						nNewArrays = _addPhysicalDimension(elPkt, lDsXfer, "z", dsSubZ);
						if(nNewArrays == 0) return null;
					}
				}
				else{
					nNewArrays = _addPhysicalDimension(elPkt, lDsXfer, "z", dsZ);;
					if(nNewArrays == 0) return null;
				}
			}
		}
		
		// Handle <yscan> planes.
		lDsToRead = lDsRemain;
		QDataSet dsYTags = null;
		QDataSet dep1;
		for(QDataSet ds: lDsToRead){
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
			
			nNewArrays = _addPhysicalDimension(elPkt, lDsXfer, "yscan", ds);
			if(nNewArrays < 1) return null;
		}
		
		return new PacketXferInfo(doc, lDsXfer); /* Headers and data xfer */
	}
	
	/** Add a new phys-dim to the packet header and record the transfer info for the
	 * corresponding data packets
	 * 
	 * @param elPkt
	 * @param lXfer
	 * @param sAxis - one of "x", "y", "z" or "yscan"
	 * @param ds
	 * @return
	 * @throws IOException 
	 */
	private int _addPhysicalDimension(
		Element elPkt, List<QdsXferInfo> lXfer, String sAxis, QDataSet ds
	) throws IOException {
		
		int nArrays = 0;
			
		Document doc = elPkt.getOwnerDocument();
		Element el = doc.createElement(sAxis);
		elPkt.appendChild(el);
		nArrays++;
		QdsXferInfo xfer = new QdsXferInfo(ds, bBinary, nSigDigit, nSecDigit);
		lXfer.add(xfer);
		
		String sName = _getName(elPkt, ds, sAxis);
		el.setAttribute("name", sName);
		el.setAttribute("type", _makeTypeFromXfer(xfer));
		
		Units units = (Units)ds.property(QDataSet.UNITS);
		String sTag = sAxis.equals("yscan") ? "zUnits" : "units";
		el.setAttribute(sTag, units != null ? units.toString() : "");
		
		// Now handle the properties, add in extras if specified
		Element elProps = doc.createElement("properties");
		int nProps = 0;
		if(sAxis.equals("yscan")){
			nProps += _addSimpleProps(elProps, ds, "z");
			nProps += _yTagsNProps(ds, el, elProps);  // Gets Y-Tags Attribs
		}
		else{
			nProps += _addSimpleProps(elProps, ds, sAxis);
		}
		if(nProps > 0) el.appendChild(elProps);
		
		// Look to see if we are going to be adding in any stats planes
		QDataSet dsStats;
		QdsXferInfo xferStats;
		Element elStats;
		String sErr = "Statistics dataset is a different rank than the average dataset";
		
		for(String sProp: aStdPlaneProps){
			if((dsStats = (QDataSet)ds.property(sProp)) != null){
				if(dsStats.rank() != ds.rank()){ 
					log.warning(sErr); 
					continue;
				}
				
				xferStats = new QdsXferInfo(dsStats, bBinary, nSigDigit, nSecDigit);
				elStats = doc.createElement(sAxis);
				elPkt.appendChild(elStats);
				lXfer.add(xferStats);
				nArrays++;
				
				Units unitsStats = (Units)ds.property(QDataSet.UNITS);
		
				elStats.setAttribute("name", sName + "." + _statsName(sProp));
				
				sTag = sAxis.equals("yscan") ? "zUnits" : "units";
				elStats.setAttribute(sTag, unitsStats != null ? unitsStats.toString() : "");
				
				elStats.setAttribute("type", _makeTypeFromXfer(xferStats));
		
				// Now handle the properties
				Element elStatsProps = doc.createElement("properties");
				elStatsProps.setAttribute("source", sName);
				elStatsProps.setAttribute("operation", sProp);
				
				if(sAxis.equals("yscan")){
					_addSimpleProps(elStatsProps, dsStats, "z");
					_yTagsNProps(dsStats, elStats, elStatsProps);
				}
				else{
					_addSimpleProps(elStatsProps, dsStats, sAxis);
				}
				
				elStats.appendChild(elStatsProps);
			}
		}
		
		return nArrays;
	}
	
	private String _getName(Element elPkt, QDataSet ds, String sPlane)
	{
		// Insure we have a name: ds.NAME -> Units -> just number
		// If the name is empty, make one up based on the units if you can
		
		String sName = (String)ds.property(QDataSet.NAME);
		if(sName != null) return sName;
		
		Units units = (Units)ds.property(QDataSet.UNITS);
		sName = makeNameFromUnits(units);
		if(sName.length() >= 0) return sName;
		
		int n = elPkt.getElementsByTagName(sPlane).getLength();
		return String.format("%s_%d", sPlane.toUpperCase(), n);
	}
	
	private String _makeTypeFromXfer(QdsXferInfo xfer) throws IOException
	{
		String sName = xfer.name();
		int nSz = xfer.size();
		String sReal = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 
					     "little_endian_real" : "sun_real";
		
		switch(sName){
			case "double": return String.format("%s8", sReal);
			case "float":  return String.format("%s4", sReal);
			case "ascii":  return String.format("ascii%d", nSz);
			case "time":   return String.format("time%d", nSz);
			default:
				// TODO: What kind of exception should I throw here?
				throw new IOException(String.format(
					"das2.2 streams cannot transmit data values of type %s%d",
					sName, nSz
				));
		}
	}
	
	private String _statsName(String sProp){
		switch(sProp){
		case QDataSet.BIN_MIN: return "min";
		case QDataSet.BIN_MAX: return "max";
		case QDataSet.BIN_MINUS: return "minus";
		case QDataSet.BIN_PLUS: return "plus";
		default: return "unknown";
		}
	}
	
	/** Add the y-tags into a yscan and it's properties into the plane props
	 * 
	 * @param ds - The dataset, will look for it's depend 1
	 * @param el - The element to get the yTags (or yTagMin, Interval)
	 * @param elProps - The element to get the y properties
	 * @return The number of properties added
	 */
	private int _yTagsNProps(QDataSet ds, Element el, Element elProps) throws IOException
	{
		int nItems = ds.length(0);
		el.setAttribute("nitems", String.format("%d", nItems));
		
		QDataSet dsYTags = (QDataSet)ds.property(QDataSet.DEPEND_1);
		
		// If no yTags, then just set min == 0 and interval to 1.0.
		if(dsYTags == null){
			el.setAttribute("yTagMin", "0.0");
			el.setAttribute("yTagInterval", "1.0");
			return 0;
		}
		
		if(dsYTags.rank() != 1)
			throw new IOException(
				"das2.2 YTags must be rank 1.  (Hint: Dataset may be exportable using das2.3/basic)"
			);
		
		// If ds is actually at 1-D sequence, just save that instead of all the
		// tags.
		Sequence1D seq = getSequenceRank1(dsYTags, 1e-4);
		if(seq != null){
			el.setAttribute("yTagMin", seq.sMinval);
			el.setAttribute("yTagInterval", seq.sInterval);
		}
		else{
			String sFmt = String.format("%%.%de", nSigDigit);
			StringBuilder sb = new StringBuilder();
		
			sb.append(String.format(sFmt, dsYTags.value(0)));
			for(int i = 1; i < dsYTags.length(); ++i)
				sb.append(",").append(String.format(sFmt, dsYTags.value(i)));
			
			el.setAttribute("yTags", sb.toString());
		}
		
		Units yunits = (Units)dsYTags.property(QDataSet.UNITS);
		if(yunits != null) el.setAttribute("yUnits", yunits.toString());
		else el.setAttribute("yUnits", "");
		
		return _addSimpleProps(elProps, dsYTags, "y");
	}

	int addBoolProp(Element props, String sName, Object oValue){
		String sValue = (Boolean)oValue ? "true" : "false";
		props.setAttribute(sName, sValue);
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
		
		props.setAttribute(sName, sOutput);
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
		props.setAttribute("double:"+sName, sVal);
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
		props.setAttribute("Datum:"+sName, datum.toString());
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
		
		props.setAttribute("DatumRange:"+d2key, sValue);
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
		
		props.setAttribute("DatumRange:"+sName, sOutput);
		return 1;
	}
	
	
	// Get all the simple standard properties of a dataset and add these to the
	// attributes of a property element.  Complex properties dependencies and 
	// associated datasets are not handled here.  Returns the number of props
	// added.
	int _addSimpleProps(Element props, QDataSet qds, String sAxis)
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
			
			props.setAttribute("Datum:"+ sAxis + "TagWidth", dtm.toString());
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
			
			if(props.hasAttribute(sKey)) continue;
			
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
