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
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.LoggerManager;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.ArrayDataSet;
import org.das2.qds.BundleDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	
	// Property strings for offset and reference arrays, could be a general
	// QDataSet thing if someone wanted it
	
	// Goes in the place of depend_0, says values are an offset the offset 
	// values should have a property call reference that points to the 
	// dataset they offset
	public static final String OFFSET_1 = "OFFSET_1";   
	public static final String OFFSET_2 = "OFFSET_2";
	public static final String OFFSET_3 = "OFFSET_3";
	public static final String REFERENCE = "REFERENCE";
	public static final String AXIS      = "AXIS";
	
	public static final double DEF_SEQ_JITTER = 1e-3;
	
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
	
	/** Write a QDataSet as a das2.3 stream
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
		
		Document doc = _makeStreamHdr(qds); // XML document
		if(doc == null) return false;
		lHdrsToSend.add( xmlDocToStr(doc) );
		
		Element elStreamProps = null;
		Element elStream = doc.getDocumentElement();
		NodeList nl = elStream.getElementsByTagName("properties");
		if((nl != null)&&(nl.getLength() > 0))
			elStreamProps = (Element) nl.item(0);
		
		// Take advantage of the fact that we have all the data here to place all
		// headers first.  This way someone can use a text editor to inspect the
		// top of the stream, even for binary streams
		PacketXferInfo pi;
		List<PacketXferInfo> lPi = new ArrayList<>();
	
		// See if the join dataset can be dropped to just a normal dataset
		// by introducing references and offests
		double rMaxSeqJitter = DEF_SEQ_JITTER;
		List<QDataSet> lDs = _maybeCollapseSeprableJoin(qds);
		qds = lDs.get(0);  // This is silly, but I can't use a bundle here
		QDataSet dsRef = null;
		if(lDs.size() > 1){ 
			dsRef = lDs.get(1);
			// If coordinate values are generated by splitting a huge 
			// reference from a small offset, allow conversion of
			// more jitter values to squences
			rMaxSeqJitter = 0.05; 
		}
			
		if(SemanticOps.isJoin(qds)){
			
			for(int i = 0; i < qds.length(); ++i){
				QDataSet ds = DataSetOps.slice0(qds, i);
				pi = _makePktXferInfo(ds, null, elStreamProps, rMaxSeqJitter);
				if(pi == null) return false;
				else lPi.add(pi);
				sPktHdr = xmlDocToStr(pi.doc);
				lHdrsToSend.add(sPktHdr);
			}
		}
		else{
			pi = _makePktXferInfo(qds, dsRef, elStreamProps, rMaxSeqJitter);
			if(pi == null) return false;
			else lPi.add(pi);
			sPktHdr = xmlDocToStr(pi.doc);
			lHdrsToSend.add(sPktHdr);
		}
		
		for(int iPktId = 0; iPktId < lHdrsToSend.size(); ++iPktId)
			writeHeader(os, VAR_PKT_TAGS, iPktId, lHdrsToSend.get(iPktId));
		
		// Now write the data, find out packet ID by our header value
		int iPktId;
		for(PacketXferInfo pktinfo: lPi){
			sPktHdr = xmlDocToStr(pktinfo.doc);
			iPktId = lHdrsToSend.indexOf(sPktHdr);
			writeData(os, VAR_PKT_TAGS, iPktId, pktinfo);
		}
		
		return true;
	}
	
	// Top level helper functions and structures //////////////////////////////
	
	boolean _canWriteNonJoin(QDataSet qds){
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
	
	// See if a join dataset is only a join because the depend_1 keeps changing
	// due to a reference point change, not due to a depend_1 length change, or
	// the deltas between depend_1 points.  This is common for MEX/MARSIS
	// radargrams and Juno/Waves HFWBR spectra.  It likely occurs in other 
	// datasets as well.
	
	// Would like to just return a dataset here, but bundles are too
	// restrictive to work here.
	List<QDataSet> _maybeCollapseSeprableJoin(QDataSet dsJoin) throws IOException
	{
		List<QDataSet> lDs = new ArrayList<>();
		lDs.add(dsJoin);
		
		if(!SemanticOps.isJoin(dsJoin)) return lDs;
		if(dsJoin.length() < 2) return lDs;  
		
		QDataSet dsBeg = DataSetOps.slice0(dsJoin, 0);
		
		// TODO: Deal with higher ranks, depend2 etc.
		if(dsBeg.rank() != 2) return lDs;
		
		// TODO: Make work with bundles
		if(dsBeg instanceof BundleDataSet) return lDs; 
		
		// Collect the depend_1's as a rank2 array, then see if it's separable
		QDataSet dsBegDep1 = (QDataSet)dsBeg.property(QDataSet.DEPEND_1);
		if((dsBegDep1 == null)||(dsBegDep1.rank() != 1)) return lDs;
		Units units = (Units)dsBegDep1.property(QDataSet.UNITS);
		String sUnitsBegDep1 = (units != null ? units.toString() : "");
		
		ArrayDataSet dsAllDep1 = ArrayDataSet.createRank2(
			DataSetOps.getComponentType(dsBegDep1), dsJoin.length(), 
			dsBeg.length(0)
		);
		copySimpleProps(dsAllDep1, dsBegDep1);
		
		for(int j = 0; j < dsBegDep1.length(); ++j)      // First set of dep1
			dsAllDep1.putValue(0, j, dsBegDep1.value(j));
		
		int nTotalLen = dsBeg.length();                  // Accumlate total length in X
		for(int J = 1; J < dsJoin.length(); ++J){        // Loop over the rest
			QDataSet ds = DataSetOps.slice0(dsJoin, J);
			
			if((ds.rank() != dsBeg.rank()) || (ds.length(0) != dsBeg.length(0)))
				return lDs;
			
			QDataSet dep1 = (QDataSet)ds.property("DEPEND_1");
			
			//If dep1 missing or has a different shape than first dep1
			if((dep1 == null) || (dep1.rank() != dsBegDep1.rank()) || 
			   (dep1.length() != dsAllDep1.length(0)) )  return lDs;
			
			//If dep1 has different units, just bail instead of collapsing
			units = (Units)dep1.property(QDataSet.UNITS);
			String sUnits = (units != null ? units.toString() : "");
						
			if(!sUnitsBegDep1.equals(sUnits)) return lDs;
			
			for(int j = 0; j < dsBegDep1.length(); ++j) 
				dsAllDep1.putValue(J, j, dep1.value(j));
			
			nTotalLen += ds.length();
		}
		
		// The big problem with formats that don't separate fine changes
		// from bulk changes is that the delta dep1 is hidden inside a
		// much bigger reference value.  Thus the delta dep1 jitter is
		// high.  Accept a much larger change in dep1 deltas than we
		// would normally consider acceptable. 0.05 instead of 1e-5
		SeparablePair pair = _separable0(dsAllDep1, 0.05);
		if(pair == null) return lDs;
		
		// ////
		// Yay! The accumlated dep1's are separable... collapse the Join.
		// ///
		
		// The offset is an Y coordinate, save that fact
		pair.offset.putProperty(AXIS, "y");
		
		ArrayDataSet dsAll = ArrayDataSet.createRank2(
			DataSetOps.getComponentType(dsBeg), nTotalLen, dsBeg.length(0)
		);
		copySimpleProps(dsAll, dsBeg);
		
		ArrayDataSet dsRef = ArrayDataSet.createRank1(
			DataSetOps.getComponentType(pair.reference), nTotalLen
		);
		copySimpleProps(dsRef, pair.reference);
		
		ArrayDataSet dsAllDep0 = null;
		QDataSet dsDep0 = (QDataSet)dsBeg.property(QDataSet.DEPEND_0); 
		if( dsDep0 != null){
			dsAllDep0 = ArrayDataSet.createRank1(
				DataSetOps.getComponentType(dsDep0), nTotalLen
			);
			copySimpleProps(dsAllDep0, dsDep0);
		}
		
		int nOffset = 0;
		for(int J = 0; J < dsJoin.length(); ++J){
			QDataSet ds = DataSetOps.slice0(dsJoin, J);
			QDataSet dep0 = (QDataSet)ds.property(QDataSet.DEPEND_0);
			for(int i = 0; i < ds.length(); ++i){
				for(int j = 0; j < ds.length(0); ++j){
					dsAll.putValue(i + nOffset, j, ds.value(i, j));
				}
				if(dsAllDep0 != null)
					dsAllDep0.putValue(i + nOffset, dep0.value(i));    // indivdual dep0
				dsRef.putValue(i + nOffset, pair.reference.value(J)); // repeated ref vals
			}
			nOffset += ds.length();
		}
		
		copySimpleProps(dsAll, dsBeg);
		if(dsAllDep0 != null){
			dsRef.putProperty(QDataSet.DEPEND_0, dsAllDep0);
			dsAll.putProperty(QDataSet.DEPEND_0, dsAllDep0);
		}
		if(dsAll.property(OFFSET_1) != null)
				throw new IOException(String.format("dataset %s already has an "+
					"offset_1, looks like we need to use planes", dsAll.toString()
				));
		dsAll.putProperty(OFFSET_1, pair.offset);
		
		// Output a bundle of <y><zset>?  I would like too.  They are co-varying
		// in depend 0, so it would be handy.  But bundles imply that the rank of
		// each sub-item is the same (see TailBundleDataSet.bundle(), 
		// BundleDataSet.bundle() ).  This restriction breaks the simple co-varying
		// collection mentality that is needed here, so we can't use a bundle. 
		// --cwp
		lDs.clear();
		lDs.add(dsAll);
		lDs.add(dsRef);
		return lDs;
	}
	
	static class SeparablePair {
		ArrayDataSet reference;
		ArrayDataSet offset;
		SeparablePair(ArrayDataSet dsRef, ArrayDataSet dsOffset){
			reference = dsRef;
			offset = dsOffset;
			offset.putProperty(REFERENCE, dsRef);
		}
	}
		
	/** see if a dataset is separable into a rank1 reference dataset
	 * and a rank N-1 offset dataset.  This check is made so that we can
	 * put N + M values into the stream instead of N*M.  For datasets
	 * like the Juno waves HFWBR spectra this cuts the size of the stream
	 * in half, without compression
	 * 
	 * @param ds The dataset to check, must be rank 2 or higher
	 * 
	 * @param rMaxJitter how close must comparable second index be 
	 *        as a fraction of the average be considered the same value.
	 *        This is called epsilon in many numerical functions.  If you
	 *        don't have a preference try 1e-5.
	 * 
	 * @return null if not separable, a list of two qdataset objects if
	 *         separable.  The second object with be of a rank one 
	 *         lower than the initial dataset.
	 */
	SeparablePair _separable0(QDataSet ds, double rMaxJitter){
		if(ds.rank() < 2) return null;
		
		// Save all the J index deltas.  We want to average over them to
		// make as smooth a final sequence as possible.
		double[][] aOffsets = new double[ds.length()][ds.length(0)];
		ArrayDataSet dsRef = ArrayDataSet.createRank1(double.class, ds.length());
		
		int i,j;
		// Subtract the initial value of each row from the rest of the row
		// values, (save off the reference points in new dataset)
		for(i = 0; i < ds.length(); ++i){
			for(j = 0; j < ds.length(0); ++j)
				aOffsets[i][j] = ds.value(i,j) - ds.value(i,0);
					
			dsRef.putValue(i, ds.value(i,0));
		}
		
		// Get a single row of the average value of the offsets
		double[] aAvgOffset = new double[ds.length(0)];
		for(j = 0; j < ds.length(0); ++j){
			aAvgOffset[j] = 0.0;
			for(i = 0; i < ds.length(); ++i)
				aAvgOffset[j] += aOffsets[i][j];
			aAvgOffset[j] /= ds.length();
		}
		
		// Look at the jitter from the average, see if it's too high
		double rAvg, rDelta, rJitter;
		for(i = 0; i < ds.length(); ++i){
			for(j = 0; j < ds.length(0); ++j){
				rAvg   = Math.abs(aAvgOffset[j] + aOffsets[i][j]) / 2; 
				rDelta = Math.abs(aAvgOffset[j] - aOffsets[i][j]);
				if(rDelta == 0.0) continue;
				if(rAvg == 0.0)
					return null;
				rJitter = rDelta / rAvg;
				if(rJitter > rMaxJitter)
					return null;
			}
		}
		
		// Looks to be separable, 
		ArrayDataSet dsOffset = ArrayDataSet.createRank1(double.class, ds.length(0));
		for(j = 0; j < ds.length(0); ++j){
			dsOffset.putValue(j, aAvgOffset[j]);
		}
		copySimpleProps(dsRef, ds);
		copySimpleProps(dsOffset, ds);
		
		// If we are using times for the reference points, then get the offset units
		// for the time values
		Units units = (Units)ds.property(QDataSet.UNITS);
		if((units != null)&&(UnitsUtil.isTimeLocation(units))){
			dsOffset.putProperty(QDataSet.UNITS, units.getOffsetUnits());
		}
		
		return new SeparablePair(dsRef, dsOffset);
	}	
	
	// Make header for join, <stream>
	
	// For non-bundle datasets a lot of the properties can be placed in the
	// stream header and it's common to do so.  I am not using any of the 
	// defined strings from org.das2.DataSet since I'm assuming that package
	// is going away.
	Document _makeStreamHdr(QDataSet qds) {
		Document doc = newXmlDoc();
		
		Element stream = doc.createElement("stream");
		stream.setAttribute("version", FORMAT_2_3_BASIC);
		
		
		//TODO: make props class that has a parent so that ycoords
		//     (for example) can consult "y" which can consult <stream>
		//     to see if a property has already been applied.
		Element props = doc.createElement("properties");
		int nProps = 0;
		
		nProps += _addStrProp(null, props, qds, QDataSet.TITLE, "title");
		nProps += _addStrProp(null, props, qds, QDataSet.DESCRIPTION, "summary");
		nProps += _addStrProp(null, props, qds, QDataSet.RENDER_TYPE, "renderer");
		
		String sAxis = getQdsAxis(qds);
		if(sAxis == null) return null;
		
		nProps += _addSimpleProps(null, props, qds, sAxis);
		
		// If the user_properties are present, add them in
		Map<String, Object> dUser;
		dUser = (Map<String, Object>)qds.property(QDataSet.USER_PROPERTIES);
		
		boolean bStripDot = _stripDotProps(qds);
		if(dUser != null) nProps += _addPropsFromMap(null, props, dUser, bStripDot);
		
		// If the ISTP-CDF metadata weren't so long, we'd include these as well
		// maybe a Das 2.3 sub tag is appropriate for them, ignore for now...
		
		if(nProps > 0) stream.appendChild(props);
		
		doc.appendChild(stream);
		return doc;
	}
	
	// Make header for bundle, <packet>
	//
	// For each real dataset (not a bundle)
	//
	// 1. inspect it's dependencies  
	//    Save off dependencies as follows:
	//       dep0 --> <x>
	//
	//       dep1 --> If depend_1 is rank 2 and separable into a reference and 
	//             offset.  Save the reference as <y>, make the offset the
	//             <ycoords>, otherwise ignore, it will be in the packet header
	//
	//       dep2 --> If depend 2 is rank 2 or above, through a "fixme" error,
	//             otherwise ignore, it will be in the packet header
	// 
	// 2. Look at the rank of the dataset.
	//      Rank 1: Look for the plane_0 ds.  Save the regular dataset as
	//              <y>, the plane_0 as <z>.  Some third plane would be
	//              needed for <w>.  Maybe if the plane_0 had a plane_0?
	//
	//      Rank 2: Run SemanticOps.isRank2Waveform()  If true use <yset>
	//              with <xcoord use="offset" from depend_1
	//              If false use <zset> with <ycoord use="center", unless
	//              determined as separable in step 1 above
	//
	//      Rank 3: Use <wset>   
	
	PacketXferInfo _makePktXferInfo(
		QDataSet qds, QDataSet dsRef, Element elStreamProp, double rMaxSeqJitter
	) throws IOException {
		
		List<QdsXferInfo> lDsXfer = new ArrayList<>();
		Document doc = newXmlDoc();
		Element elPkt = doc.createElement("packet");
		doc.appendChild(elPkt);
		
		assert(!SemanticOps.isJoin(qds));  //Don't call this for join datasets
		
		QDataSet dep0;
		QDataSet dep1;
		QDataSet dep2;
		MutablePropertyDataSet dsSwap;
		
		// We go through the list of datasets multiple times, picking out the 
		// items we care about.  In each stage lDsToRead is the list to process,
		// lDsRemain are the remaining items
		List<QDataSet> lDsToRead = new ArrayList<>();
		List<QDataSet> lDsRemain = new ArrayList<>(); 
		
		// A) Break out the bundles
		if(qds instanceof BundleDataSet){
			for(int i = 0; i < qds.length(0); ++i)
				lDsToRead.add( ((BundleDataSet)qds).unbundle(i) );
		}
		else{
			lDsToRead.add(qds);
		}
		
		// <x>: Handle depend_0, does not remove any datasets
		for(QDataSet ds: lDsToRead){
			// Maybe the bundle's members depend_0 is <x>
			if( (dep0 = (QDataSet) ds.property(QDataSet.DEPEND_0)) != null){
				
				if(lDsXfer.isEmpty()){
					_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "x", dep0);
				}
				else{
					if(dep0 != lDsXfer.get(0).qds){
						// This is weird, but go ahead...
						_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "x", dep0);
					}
				}
				
				lDsRemain.add(ds);  // Used the depend0, but keep top level dataset
			}
			else{
				if((ds.rank() == 1)&&(ds.property(QDataSet.DEPEND_0) == null))
					// Rank 1 with no depend 0 is an extra <x> physdim
					_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "x", ds);
			}
		}
		
		// <yset>, handle anything with OFFSET_1.  Since the only way to tell 
		//         that a ds should have OFFSET_1 is to check the waveform flag
		//         this is basically just waveform handling.  Rework if OFFSET_1
		//         becames accepted as a qds property.
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		MutablePropertyDataSet dep1Swap;
		for(QDataSet ds: lDsToRead){
			if(SemanticOps.isRank2Waveform(ds)){
				//Rename the DEPEND_1 property to OFFSET_1
				// TODO: Find out if this is changing the original dataset
				dsSwap = DataSetOps.makePropertiesMutable(ds);
				
				dep1 = (QDataSet)dsSwap.property(QDataSet.DEPEND_1);
				dep1Swap = DataSetOps.makePropertiesMutable(dep1);
				dsSwap.putProperty(QDataSet.DEPEND_1, null);
				dep1Swap.putProperty(AXIS, "x");
				dep1Swap.putProperty(REFERENCE, dsSwap.property(QDataSet.DEPEND_0));
				dsSwap.putProperty(OFFSET_1, dep1Swap);
				
				_addPhysicalDimension(
					elStreamProp, elPkt, lDsXfer, "yset", dsSwap, rMaxSeqJitter
				);
			}
			else
				lDsRemain.add(ds);
		}
		
		// <y> part0, Grab our out-of-band join collapse reference if one was
		//    supplied.  Again, I know this is a kludgy way to get this done.
		if(dsRef != null){
			_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "y", dsRef);
		}
		
		// <y> part1, Check for separable rank > 1 depend1 
		//            (i.e. wandering spectrograms).
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if((dep1 = (QDataSet) ds.property(QDataSet.DEPEND_1)) == null){
				lDsRemain.add(ds);
				continue;
			} 
			if((dep1.rank() < 2)){ lDsRemain.add(ds); continue;}
			
			// Sequence jitter is expected to be lower than separation jitter
			SeparablePair pair = _separable0(dep1, rMaxSeqJitter*10);
			if( pair != null){
				_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "y", pair.reference);
				pair.offset.putProperty(AXIS, "y");
							
				// TODO: Find out if this is changing the original dataset, because we
				//       don't want to do that!
				dsSwap = DataSetOps.makePropertiesMutable(ds);
				dsSwap.putProperty(QDataSet.DEPEND_1, null);
				if(dsSwap.property(OFFSET_1) != null)
					throw new IOException(String.format("dataset %s already has an "+
						"offset_1, looks like we need to use planes", dsSwap.toString()
					));
				dsSwap.putProperty(OFFSET_1, pair.offset);
				lDsRemain.add(dsSwap);  // Still need to output data part.
				
				// Since we separated coordinates, allow more jitter in sequence
				// conversion
				if(rMaxSeqJitter < 0.02) rMaxSeqJitter = 0.02;
			}
			else{
				// <yset> revisited - Depend_1 is rank 2 and could not be
				// separated, output it as a <yset>
				_addPhysicalDimension(
					elStreamProp, elPkt, lDsXfer, "yset", dep1, rMaxSeqJitter
				);
				lDsRemain.add(ds); 
			}
		}
		
		// <y> part2, Output or save for later remaining rank 1 datasets
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		QDataSet dsZ = null;
		QDataSet dsSubZ;
		for(QDataSet ds: lDsToRead){
			if(ds.rank() > 1){ lDsRemain.add(ds); continue; } 
			
			_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "y", ds);
			
			// Look for hidden Z's
			if( (dsZ = (QDataSet) ds.property(QDataSet.PLANE_0)) != null){
				// If plane0 is a bundle we have multiple Z's
				if(SemanticOps.isBundle(dsZ)){
					for(int i = 0; i < dsZ.length(); ++i){
						dsSubZ = DataSetOps.slice0(dsZ, i);
						lDsRemain.add(dsSubZ);
					}
				}
				else{
					lDsRemain.add(dsZ);
				}
			}
		}
		
		// <zset>, Output or save for later remaining rank 2 datasets
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if(ds.rank() == 2) 
				_addPhysicalDimension(
					elStreamProp, elPkt, lDsXfer, "zset", ds, rMaxSeqJitter
				);
			else
				lDsRemain.add(ds);
			// Look for bundle here???
		}
		
		// <z> part1, Check for separable rank > 1 (i.e. wandering volumes)
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if((dep2 = (QDataSet) ds.property(QDataSet.DEPEND_2)) == null){
				lDsRemain.add(ds);
				continue;
			} 
			if((dep2.rank() < 2)){ lDsRemain.add(ds); continue;}
			
			SeparablePair pair = _separable0(dep2, 1e-5);
			if( pair != null){
				_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "z", pair.reference);
							
				// TODO: Find out if this is changing the original dataset, because we
				//       don't want to do that!
				dsSwap = DataSetOps.makePropertiesMutable(ds);
				dsSwap.putProperty(QDataSet.DEPEND_2, null);
				if(dsSwap.property(OFFSET_2) != null)
					throw new IOException(String.format("dataset %s already has an "+
						"offset_2, looks like we need to use planes", dsSwap.toString()
					));
				dsSwap.putProperty(OFFSET_2, pair.offset);
				pair.offset.putProperty(AXIS, "z");
				lDsRemain.add(dsSwap);  // Still need to output data part.
				
				// Since we separated coordinates, allow more jitter in sequence
				// conversion
				if(rMaxSeqJitter < 0.02) rMaxSeqJitter = 0.02;
			}
		}
		
		// <z> part2, Output any saved <z> sets
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if(ds.rank() > 1){ lDsRemain.add(ds); continue; } 
			
			_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "z", ds);
			
			// Look for hidden w's?  nah, not yet.
			// QDataSet dsW = (QDataSet) ds.property(QDataSet.PLANE_0)
		}
		
		// <wset> Output any rank 3 datasets
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if(ds.rank() != 3){ lDsRemain.add(ds); continue; }
			
			_addPhysicalDimension(elStreamProp, elPkt, lDsXfer, "wset", ds, rMaxSeqJitter);
			
			// TODO: Check for separability
		}
		
		// If I had saved off <w> sets, they would be output here...
		
		// Check to see if anything was forgotten
		if(!lDsRemain.isEmpty()){
			throw new IOException(
				"das2.3 output bug, the following dataset (and maybe more) were not "+
				"output: '" + lDsRemain.get(0).toString() + "'"
			);
		}
		
		return new PacketXferInfo(doc, lDsXfer); /* Headers and data xfer */
	}
	
	int _addPhysicalDimension(
		Element elStreamProps, Element elPkt, List<QdsXferInfo> lXfer, String sAxis, 
		QDataSet ds
	) throws IOException{
		return _addPhysicalDimension(elStreamProps, elPkt, lXfer, sAxis, ds, DEF_SEQ_JITTER);
	}
	
	/** Add a new phys-dim to the packet header and record the transfer info for the
	 * corresponding data packets.
	 * 
	 * @param elParent - The overall stream properties.  Used to make sure
	 *         properties aren't repeated unless necessary
	 * @param elPkt
	 * @param lXfer
	 * @param sAxis - one of x, yset, y, zset, z, wset, w
	 * @param ds
	 * @param rMaxSeqJitter - Max jitter from average before collapsing coordinate 
	 *           values to a sequence
	 * @return
	 * @throws IOException 
	 */
	int _addPhysicalDimension(
		Element elParent, Element elPkt, List<QdsXferInfo> lXfer, String sAxis, 
		QDataSet ds,
		double rMaxSeqJitter
	) throws IOException {
		int nArrays = 0;
		
		boolean bSet = sAxis.endsWith("set");
		
		Document doc = elPkt.getOwnerDocument();
		Element elPdim = doc.createElement(sAxis);
		elPkt.appendChild(elPdim);
		QdsXferInfo xfer = new QdsXferInfo(ds, bBinary, nSigDigit, nSecDigit);
		lXfer.add(xfer);
		
		String sPdim = _getPhysDim(elPkt, ds, sAxis);
		elPdim.setAttribute("pdim", sPdim);
		
		if(bSet) elPdim.setAttribute("nitems", String.format("%d",xfer.xSliceItems(0)));
		
		// make the first array
		Element elAry = doc.createElement("array");
		elAry.setAttribute("type", _makeTypeFromXfer(xfer));
		Units units = (Units)ds.property(QDataSet.UNITS);
		elAry.setAttribute("units", units != null ? units.toString() : "");
		elAry.setAttribute("use", "center"); // check on this.
		elPdim.appendChild(elAry);
		nArrays += 1;
		
		// Add arrays for the stats planes, if present
		QDataSet dsStats;
		QdsXferInfo xferStats;
		
		for(String sProp: aStdPlaneProps){
			if((dsStats = (QDataSet)ds.property(sProp)) == null) continue;
			
			// TODO: Some of these properties aren't covaring datasets, add checks
			// here and insert values into properties array instead.
			if(dsStats.rank() != ds.rank()){ 
				throw new IOException(String.format(
					"Statistics dataset '%s' for main dataset '%s' is not the same rank",
					ds.toString(), dsStats.toString()
				));
			}
			
			xferStats = new QdsXferInfo(dsStats, bBinary, nSigDigit, nSecDigit);
			elAry = doc.createElement("array");
			elAry.setAttribute("use", _statsName(sProp));
			elAry.setAttribute("type", _makeTypeFromXfer(xferStats));
			elPdim.appendChild(elAry);
			nArrays += 1;
		}
		
		// Add coordinate dimensions, if this is a set.  Need to add planes
		// in here if offsets can be in multiple directions
		String[][] aCoordProps = {
			{QDataSet.DEPEND_1, OFFSET_1, "y"}, {QDataSet.DEPEND_2, OFFSET_2, "z"}
		};
		for(String[] aProps: aCoordProps){
			String sDep     = aProps[0];
			String sOff     = aProps[1];
			String sCoordAx = aProps[2];
			QDataSet dsCoords;
			Element elCoord = null;
			Element elProps = null;
			
			// Cascade these, depepends go first 
			if((dsCoords = (QDataSet)ds.property(sDep)) != null){
				if(elCoord == null) elCoord = doc.createElement(sCoordAx + "coord");
				elCoord.setAttribute("pdim", _getPhysDim(elPkt, dsCoords, sCoordAx));
				_addValsToCoord(elCoord, dsCoords, "center", rMaxSeqJitter);
				elProps = doc.createElement("properties");
				
				if( _addSimpleProps(elParent, elProps, dsCoords, sCoordAx) == 0)
					elProps = null;
				
				// TODO: Add statistics such as bandwidth etc.
			}
			
			if((dsCoords = (QDataSet)ds.property(sOff)) != null){
				QDataSet dsRef = (QDataSet)dsCoords.property(REFERENCE);
				String sCoordName = (String)dsCoords.property(AXIS) + "coord";
				String sDim = _getPhysDim(elPkt, dsRef, (String)dsCoords.property(AXIS));
				if(elCoord == null) elCoord = doc.createElement(sCoordName);
				elCoord.setAttribute("pdim", sDim);
				_addValsToCoord(elCoord, dsCoords, "offset", rMaxSeqJitter);
				
				// TODO: Check for offset planes (or whatever mechanisim holds
				// offsets in more than one physicsal dimension.
				
				// If depend_N didn't have properties, add mine if I have them
				if(elProps == null){
					elProps = doc.createElement("properties");
					if(_addSimpleProps(elParent, elProps, dsCoords, sCoordAx) == 0)
						elProps = null;
				}
			}
			
			if((elCoord != null)&&(elProps != null)) 
				elCoord.appendChild(elProps);
			if(elCoord != null)
				elPdim.appendChild(elCoord);
		}
			
		// Now for the properties
		Element elProps = doc.createElement("properties");
		String sAx = sAxis.substring(0, 1);
		if(_addSimpleProps(elParent, elProps, ds, sAx) != 0)
			elPdim.appendChild(elProps);
		
		return nArrays;
	}
	
	// Return 1 if a coordinate set was added to the give element
	// If the data values look like a sequence then a sequence is attached,
	// otherwise the values are dumped
	int _addValsToCoord(
		Element el, QDataSet ds, String sUse, double rMaxSeqJitter
	) throws IOException{
		Document doc = el.getOwnerDocument();
		Units units = (Units)ds.property(QDataSet.UNITS);
		String sUnits = (units != null ? units.toString() : "");
			
		//TODO: Handle rank2 sequences.  These may have a repeat count
		if(ds.rank() > 1){
			throw new IOException("TODO: Handle N-D coordinates");
		}
		Sequence1D seq = getSequenceRank1(ds, rMaxSeqJitter);
		if(seq != null){
			Element elSeq = doc.createElement("sequence");
			elSeq.setAttribute("use", sUse);
			elSeq.setAttribute("minval", seq.sMinval);
			elSeq.setAttribute("interval", seq.sInterval);
			elSeq.setAttribute("units", sUnits);
			el.appendChild(elSeq);
			return 1;
		}
		
		// Not a sequence, have to print the values
		Element elVals = doc.createElement("values");
		elVals.setAttribute("use", sUse);
		elVals.setAttribute("units", sUnits);
		String sVals = _getValueSet(ds);
		elVals.setTextContent(sVals);
		el.appendChild(elVals);
		return 1;
	}
		
	String _getPhysDim(Element elPkt, QDataSet ds, String sAxis) throws IOException
	{
		// Insure we have a name: ds.NAME -> Units -> just number
		// If the name is empty, make one up based on the units if you can
		
		String sName = (String)ds.property(QDataSet.NAME);
		if(sName != null) return sName;
		
		Units units = (Units)ds.property(QDataSet.UNITS);
		sName = makeNameFromUnits(units);
		if(sName.length() >= 0) return sName;
		
		if((sAxis == null)||(sAxis.length() == 0)){
			throw new IOException(
				"Can't find physical dimension name by axis search, no axis specified"
			);
		}
		int n = elPkt.getElementsByTagName(sAxis).getLength();
		return String.format("%s_%d", sAxis.toUpperCase(), n);
	}
	
	String _makeTypeFromXfer(QdsXferInfo xfer) throws IOException
	{
		String sName = xfer.name();
		int nSz = xfer.size();
		String sReal = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 
					     "little_endian_real" : "big_endian_real";
		String sInt = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? 
					     "little_endian_int" : "big_endian_int";
		
		switch(sName){
			case "double": return String.format("%s8", sReal);
			case "float":  return String.format("%s4", sReal);
			case "ascii":  return String.format("ascii%d", nSz);
			case "time":   return String.format("time%d", nSz);
			case "char":   return String.format("char%d", nSz);
			case "int":    return String.format("%s4", sInt);
			case "long":   return String.format("%s8", sInt);
			default:
				// TODO: What kind of exception should I throw here?
				throw new IOException(String.format(
					"das2.3 streams cannot transmit data values of type %s%d",
					sName, nSz
				));
		}
	}
	
	// List of properties that should generate a plane
	static final String[] aStdPlaneProps = {
		QDataSet.BIN_MIN, QDataSet.BIN_MAX, QDataSet.BIN_MINUS, QDataSet.BIN_PLUS,
		QDataSet.DELTA_MINUS, QDataSet.DELTA_PLUS, QDataSet.WEIGHTS
	};
	
	static String _statsName(String sProp){
		switch(sProp){
		case QDataSet.BIN_MIN: return "min";
		case QDataSet.BIN_MAX: return "max";
		case QDataSet.BIN_MINUS: return "min";
		case QDataSet.BIN_PLUS: return "max";
		case QDataSet.DELTA_MINUS: return "min_error";
		case QDataSet.DELTA_PLUS:  return "max_error";
		case QDataSet.WEIGHTS:     return "count";
		default: return "unknown";
		}
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
	
	int _addBoolProp(Element parent, Element props, String sName, Object oValue){
		String sValue = (Boolean)oValue ? "true" : "false";
		return _addChildProp(parent, props, sName, "boolean", sValue);
	}
	
	int _addStrProp(
		Element parent, Element props, QDataSet qds, String qkey, String d2key
	){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return _addStrProp(parent, props, d2key, oProp);
		return 0;
	}
	int _addStrProp(Element parent, Element props, String sName, Object oValue)
	{
		
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
		
		return _addChildProp(parent, props, sName, null, sOutput);
	}
	
	int _addRealProp(
		Element parent, Element props, QDataSet qds, String qkey, String d2key
	){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return _addRealProp(parent, props, d2key, oProp);
		return 0;
	}
	int _addRealProp(Element parent, Element props, String sName, Object oValue)
	{
		Number num = (Number)oValue;
		String sVal = String.format("%.6e", num.doubleValue());
		return _addChildProp(parent, props, sName, "double", sVal);
	}
	
	int _addDatumProp(
		Element parent, Element props, QDataSet qds, String qkey, String d2key
	){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return _addDatumProp(parent, props, d2key, oProp);
		return 0;
	}
	int _addDatumProp(Element parent, Element props, String sName, Object oValue)
	{
		Datum datum = (Datum)oValue;
		return _addChildProp(parent, props, sName, "Datum", datum.toString());
	}
	
	int _addRngProp(
		Element parent, Element props, QDataSet qds, String sMinKey, String sMaxKey,
		String sUnitsKey, String d2key
	){
		Object oMin, oMax, oUnits;
		oMin = qds.property(sMinKey); oMax = qds.property(sMaxKey);
		oUnits = qds.property(sUnitsKey);
		
		if(oMin == null || oMax == null) return 0;
		double rMin = ((Number)oMin).doubleValue(); 
		double rMax = ((Number)oMax).doubleValue();
		
		DatumRange dr;
		if(oUnits != null)
			dr = new DatumRange(rMin, rMax, (Units)oUnits);
		else
			dr = new DatumRange(rMin, rMax, Units.dimensionless);
		
		return _addChildProp(parent, props, d2key, "DatumRange", dr.toString());
	}
	int _addRngProp(Element parent, Element props, String sName, Object oValue)
	{
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
		
		return _addChildProp(parent, props, sName, "DatumRange", sOutput);
	}
	
	int _addChildProp(
		Element parent, Element props, String sName, String sType, String sVal
	){
		//See if the parent properties object already has this, if so
		//don't add it
		if(parent != null){
			NodeList nl = parent.getElementsByTagName("p");
			for(int i = 0; i < nl.getLength(); ++i){
				Element elParProp = (Element)nl.item(i);
				String sParName = elParProp.getAttribute("name");
				if(!sParName.equals(sName)) continue;
				
				String sParVal = elParProp.getTextContent();
				if(sParVal.equals(sVal)) return 0;
			}
		}
		
		Document doc = props.getOwnerDocument();
		Element prop = doc.createElement("p");
		prop.setAttribute("name", sName);
		if(sType != null) prop.setAttribute("type", sType);
		Node text = doc.createTextNode(sVal);
		prop.appendChild(text);
		props.appendChild(prop);
		return 1;
	}
	
	// Get all the simple standard properties of a dataset and add these to the
	// children to a property element.  Complex properties dependencies and 
	// associated datasets are not handled here.  Returns the number of props
	// added.
	//
	// Don't re-add properties that are already present in parent props
	int _addSimpleProps(Element parent, Element props, QDataSet qds, String sAxis)
	{
		int nProps = 0;
		
		nProps += _addStrProp(parent, props, qds, QDataSet.FORMAT, sAxis + "Format");
		nProps += _addStrProp(parent, props, qds, QDataSet.SCALE_TYPE, sAxis + "ScaleType");
		nProps += _addStrProp(parent, props, qds, QDataSet.LABEL, sAxis + "Label");
		nProps += _addStrProp(parent, props, qds, QDataSet.DESCRIPTION, sAxis + "Summary");
		
		nProps += _addRealProp(parent, props, qds, QDataSet.FILL_VALUE, sAxis + "Fill");
		nProps += _addRealProp(parent, props, qds, QDataSet.VALID_MIN, sAxis + "ValidMin");
		nProps += _addRealProp(parent, props, qds, QDataSet.VALID_MAX, sAxis + "ValidMax");
		
		nProps += _addRngProp(parent, props, qds, QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
		                     QDataSet.UNITS, sAxis + "Range");
		
		// The cadence is an odd bird, a QDataSet with 1 item, handle special
		Object obj = qds.property(QDataSet.CADENCE);
		if(obj != null){
			QDataSet dsTmp = (QDataSet)obj;
			Units units = (Units) dsTmp.property(QDataSet.UNITS);
			if(units == null) units = Units.dimensionless;
			Datum dtm = Datum.create(dsTmp.value(), units);
			
			nProps += _addChildProp(parent, props, sAxis+"TagWidth", "Datum", dtm.toString());
		}
		
		return nProps;
	}
	
	// Get all the user_data properties that don't conflict with any properties
	// already present.  These don't care about prepending x,y,z axis identifiers
	// to the attribute tag, though they may have them and that's okay.
	int _addPropsFromMap(
		Element parent, Element props, Map<String, Object> dMap, boolean bStripDot
	){
		
		if(dMap == null) return 0;
		int nAdded = 0;
		
		for(Map.Entry<String,Object> ent: dMap.entrySet()){
			String sKey = ent.getKey();
			if(bStripDot && sKey.contains(".")) continue;
			
			//for das2.3 ask about child elements
			NodeList nl = props.getElementsByTagName("p");
			boolean bHasItAlready = false;
			for(int i = 0; i < nl.getLength(); ++i){
				Element el = (Element)nl.item(i);
				if(el.hasAttribute("name")){
					String sName = el.getAttribute("name");
					if(sKey.equals(sName)){
						bHasItAlready = true; 
						break; 
					}
				}
			}
			if(bHasItAlready) continue;
						
			Object oVal = ent.getValue();
			if(oVal instanceof Boolean)
				nAdded += _addBoolProp(parent, props, sKey, oVal);
			else
				if(oVal instanceof String)
					nAdded += _addStrProp(parent, props, sKey, oVal);
				else
					if(oVal instanceof Number)
						nAdded += _addRealProp(parent, props, sKey, oVal);
					else
						if(oVal instanceof Datum)
							nAdded += _addDatumProp(parent, props, sKey, oVal);
						else
							if(oVal instanceof DatumRange)
								nAdded += _addRngProp(parent, props, sKey, oVal);
			
		}
		return nAdded;
	}
	
}