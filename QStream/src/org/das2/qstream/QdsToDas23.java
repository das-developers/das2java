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
import javax.imageio.IIOException;
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
	
		// See if the join dataset can be dropped to just a normal dataset
		// by introducing references and offests
		List<QDataSet> lDs = _maybeCollapseSeprableJoin(qds);
		qds = lDs.get(0);  // This is silly, but I can't use a bundle here
		QDataSet dsRef = null;
		if(lDs.size() > 1) dsRef = lDs.get(1);
			
		if(SemanticOps.isJoin(qds)){
			
			for(int i = 0; i < qds.length(); ++i){
				QDataSet ds = DataSetOps.slice0(qds, i);
				if((pi = _makePktXferInfo(ds, null)) == null) return false;
				else lPi.add(pi);
				sPktHdr = xmlDocToStr(pi.doc);
				if(!lHdrsSent.contains(sPktHdr)) lHdrsToSend.add(sPktHdr);
			}
		}
		else{
			if((pi = _makePktXferInfo(qds, dsRef)) == null) return false;
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
	
	// See if a join dataset is only a join because the depend_1 keeps changing
	// due to a reference point change, not due to a depend_1 length change, or
	// the deltas between depend_1 points.  This is common for MEX/MARSIS
	// radargrams and Juno/Waves HFWBR spectra.  It likely occurs in other 
	// datasets as well.
	
	// Would like to just return a dataset here, but bundles are too
	// restrictive to work here.
	private List<QDataSet> _maybeCollapseSeprableJoin(QDataSet dsJoin)
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
		
		ArrayDataSet dsAllDep1 = ArrayDataSet.createRank2(
			DataSetOps.getComponentType(dsBegDep1), dsJoin.length(), 
			dsBeg.length(0)
		);
		
		for(int j = 0; j < dsBegDep1.length(); ++j)      // First set of dep1
			dsAllDep1.putValue(0, j, dsBegDep1.value(j));
		
		int nTotalLen = dsBeg.length();
		for(int J = 1; J < dsJoin.length(); ++J){        // Loop over the rest
			QDataSet ds = DataSetOps.slice0(dsJoin, J);
			
			if((ds.rank() != dsBeg.rank()) || (ds.length(0) != dsBeg.length(0)))
				return lDs;
			
			QDataSet dep1 = (QDataSet)ds.property("DEPEND_1");
			
			//If dep1 missing or has a different shape than first dep1
			if((dep1 == null) || (dep1.rank() != dsBegDep1.rank()) || 
			   (dep1.length() != dsAllDep1.length(0)) )  return lDs;
			
			for(int j = 0; j < dsBegDep1.length(); ++j) 
				dsAllDep1.putValue(J, j, dsBegDep1.value(j));
			
			nTotalLen += ds.length();
		}
		
		List<QDataSet> lSep = _separable0(dsAllDep1, 1e-5);
		if(lSep == null) return lDs;
		
		// Okay, it's separable into reference + offset, so collapse the join
		// TODO: Check for property loss.
		ArrayDataSet dsAll = ArrayDataSet.createRank2(
			DataSetOps.getComponentType(dsBeg), nTotalLen, dsBeg.length(0)
		);
		
		ArrayDataSet dsRef = ArrayDataSet.createRank1(
			DataSetOps.getComponentType(lSep.get(0)), nTotalLen
		);
		
		ArrayDataSet dsAllDep0 = null;
		QDataSet dsDep0 = (QDataSet)dsBeg.property(QDataSet.DEPEND_0); 
		if( dsDep0 != null)
			dsAllDep0 = ArrayDataSet.createRank1(
				DataSetOps.getComponentType(dsDep0), nTotalLen
			);
		
		int nOffset = 0;
		for(int J = 0; J < dsJoin.length(); ++J){
			QDataSet ds = DataSetOps.slice0(dsJoin, J);
			QDataSet dep0 = (QDataSet)ds.property(QDataSet.DEPEND_0);
			for(int i = 0; i < ds.length(); ++i){
				for(int j = 0; j < ds.length(0); ++j){
					dsAll.putValue(i + nOffset, j, ds.value(i, j));
				}
				if(dsAllDep0 != null)
					dsAllDep0.putValue(i + nOffset, dep0.value(i));  // indivdual dep0
				dsRef.putValue(i + nOffset, lSep.get(0).value(J));  // repeated ref vals
			}
			nOffset += ds.length();
		}
		
		_copySimpleProps(dsAll, dsBeg);
		if(dsAllDep0 != null){
			dsRef.putProperty(QDataSet.DEPEND_0, dsAllDep0);
			dsAll.putProperty(QDataSet.DEPEND_0, dsAllDep0);
		}
		dsAll.putProperty(OFFSET_1, lSep.get(1));
		
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
	private List<QDataSet> _separable0(QDataSet ds, double rMaxJitter){
		if(ds.rank() < 2) return null;
		
		// Save all the J index deltas.  We want to average over them to
		// make as smooth a final sequence as possible.
		double[][] aDeltas = new double[ds.length()][ds.length(0) - 1];
		ArrayDataSet dsRef = ArrayDataSet.createRank1(double.class, ds.length());
		
		dsRef.putValue(0, ds.value(0,0));
		
		for(int j = 1; j < ds.length(0); ++j)
			aDeltas[0][j - 1] = ds.value(0, j ) - ds.value(0, j-1);
		
		double rDelta;
		double rAvg;
		double rJitter;
		for(int i = 1; i < ds.length(); ++i){
			for(int j = 1; j < ds.length(0); ++j){
				rDelta = ds.value(i,j) - ds.value(i, j-1);
				if(rDelta != aDeltas[0][j-1]){
					rAvg = Math.abs(aDeltas[0][j-1] + rDelta)/2;
					if(rAvg == 0.0) return null;
					rJitter = Math.abs(rDelta - aDeltas[0][j-1]) / rAvg;
					if(rJitter > rMaxJitter) return null;
				}
				aDeltas[i][j-1] = rDelta;
			}
			dsRef.putValue(i, ds.value(i,0));
		}
		
		// Looks to be separable, smooth the deltas
		ArrayDataSet dsOffset = ArrayDataSet.createRank1(double.class, ds.length(0));
		dsOffset.putValue(0, 0.0);
		double rSum;
		for(int j = 1; j < ds.length(0); ++j){
			rSum = 0;
			for(int i = 0; i < ds.length(); ++i)
				rSum += aDeltas[i][j-1];
			dsOffset.putValue(j, rSum / ds.length());
		}
		
		_copySimpleProps(dsRef, ds);
		_copySimpleProps(dsOffset, ds);
		// If we are using times for the reference points, then get the offset units
		// for the time values
		Units units = (Units)ds.property(QDataSet.UNITS);
		if((units != null)&&(UnitsUtil.isTimeLocation(units))){
			dsOffset.putProperty(QDataSet.UNITS, units.getOffsetUnits());
		}
		
		List<QDataSet> lSep = new ArrayList<>();
		lSep.add(dsRef);
		lSep.add(dsOffset);
		return lSep;
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
		
		nProps += _addStrProp(props, qds, QDataSet.TITLE, "title");
		nProps += _addStrProp(props, qds, QDataSet.DESCRIPTION, "summary");
		nProps += _addStrProp(props, qds, QDataSet.RENDER_TYPE, "renderer");
		
		String sAxis = getQdsAxis(qds);
		if(sAxis == null) return null;
		
		nProps += _addSimpleProps(props, qds, sAxis);
		
		// If the user_properties are present, add them in
		Map<String, Object> dUser;
		dUser = (Map<String, Object>)qds.property(QDataSet.USER_PROPERTIES);
		
		boolean bStripDot = _stripDotProps(qds);
		if(dUser != null) nProps += _addPropsFromMap(props, dUser, bStripDot);
		
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
	
	private PacketXferInfo _makePktXferInfo(QDataSet qds, QDataSet dsRef)
		throws IOException {
		
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
				_addPhysicalDimension(elPkt, lDsXfer, "x", dep0);
				lDsRemain.add(ds);  // Used the depend0, but keep top level dataset
			}
			else{
				if(ds.rank() == 1)  // Rank 1 with no depend 0 is an <x> physdim
					_addPhysicalDimension(elPkt, lDsXfer, "x", ds);
			}
		}
		
		// <yset>, handle anything with OFFSET_1.  Since the only way to tell 
		//         that a ds should have OFFSET_1 is to check the waveform flag
		//         this is basically just waveform handling.  Rework if OFFSET_1
		//         becames accepted as a qds property.
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if(SemanticOps.isRank2Waveform(ds))
				_addPhysicalDimension(elPkt, lDsXfer, "yset", ds);
			else
				lDsRemain.add(ds);
		}
		
		// <y> part0, Grab our out-of-band join collapse reference if one was
		//    supplied.  Again, I know this is a kludgy way to get this done.
		if(dsRef != null){
			_addPhysicalDimension(elPkt, lDsXfer, "y", dsRef);
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
			
			List<QDataSet> lSep = _separable0(dep1, 1e-5);
			if( lSep != null){
				_addPhysicalDimension(elPkt, lDsXfer, "y", lSep.get(0));
							
				// TODO: Find out if this is changing the original dataset, because we
				//       don't want to do that!
				dsSwap = DataSetOps.makePropertiesMutable(ds);
				dsSwap.putProperty(QDataSet.DEPEND_1, null);
				dsSwap.putProperty(OFFSET_1, lSep.get(1));
				lDsRemain.add(dsSwap);  // Still need to output data part.
			}
		}
		
		// <y> part2, Output or save for later remaining rank 1 datasets
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		QDataSet dsZ = null;
		QDataSet dsSubZ;
		for(QDataSet ds: lDsToRead){
			if(ds.rank() > 1){ lDsRemain.add(ds); continue; } 
			
			_addPhysicalDimension(elPkt, lDsXfer, "y", ds);
			
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
				_addPhysicalDimension(elPkt, lDsXfer, "zset", ds);
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
			
			List<QDataSet> lSep = _separable0(dep2, 1e-5);
			if( lSep != null){
				_addPhysicalDimension(elPkt, lDsXfer, "z", lSep.get(0));
							
				// TODO: Find out if this is changing the original dataset, because we
				//       don't want to do that!
				dsSwap = DataSetOps.makePropertiesMutable(ds);
				dsSwap.putProperty(QDataSet.DEPEND_2, null);
				dsSwap.putProperty(OFFSET_2, lSep.get(1));
				lDsRemain.add(dsSwap);  // Still need to output data part.
			}
		}
		
		// <z> part2, Output any saved <z> sets
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if(ds.rank() > 1){ lDsRemain.add(ds); continue; } 
			
			_addPhysicalDimension(elPkt, lDsXfer, "z", ds);
			
			// Look for hidden w's?  nah, not yet.
			// QDataSet dsW = (QDataSet) ds.property(QDataSet.PLANE_0)
		}
		
		// <wset> Output any rank 3 datasets
		lDsToRead = lDsRemain;
		lDsRemain = new ArrayList<>();
		for(QDataSet ds: lDsToRead){
			if(ds.rank() != 3){ lDsRemain.add(ds); continue; }
			
			_addPhysicalDimension(elPkt, lDsXfer, "wset", ds);
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
	
	/** Add a new phys-dim to the packet header and record the transfer info for the
	 * corresponding data packets
	 * 
	 * @param elPkt
	 * @param lXfer
	 * @param sAxis - one of x, yset, y, zset, z, wset, w
	 * @param ds
	 * @return
	 * @throws IOException 
	 */
	private int _addPhysicalDimension(
		Element elPkt, List<QdsXferInfo> lXfer, String sAxis, QDataSet ds
	) throws IOException {
		
		return 0;
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
	
	void _valueListChild(
		Element elPlane, String sElement, String sUnits, String sValues
	){
		Document doc = elPlane.getOwnerDocument();
		Element el = doc.createElement(sElement);
		el.setAttribute("units", sUnits);
		Node text = doc.createTextNode(sValues);
		el.appendChild(text);
		elPlane.appendChild(el);
	}
	
	int _addBoolProp(Element props, String sName, Object oValue){
		String sValue = (Boolean)oValue ? "true" : "false";
		_addChildProp(props, sName, "boolean", sValue);
		return 1;
	}
	
	int _addStrProp(Element props, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return QdsToDas23.this._addStrProp(props, d2key, oProp);
		return 0;
	}
	int _addStrProp(Element props, String sName, Object oValue){
		
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
	
	int _addRealProp(Element props, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return QdsToDas23.this._addRealProp(props, d2key, oProp);
		return 0;
	}
	int _addRealProp(Element props, String sName, Object oValue){
		Number num = (Number)oValue;
		String sVal = String.format("%.6e", num.doubleValue());
		_addChildProp(props, sName, "double", sVal);
		return 1;
	}
	
	int _addDatumProp(Element props, QDataSet qds, String qkey, String d2key){
		Object oProp;
		if((oProp = qds.property(qkey)) != null)
			return QdsToDas23.this._addDatumProp(props, d2key, oProp);
		return 0;
	}
	int _addDatumProp(Element props, String sName, Object oValue){
		Datum datum = (Datum)oValue;
		_addChildProp(props, sName, "Datum", datum.toString());
		return 1;
	}
	
	int _addRngProp(Element props, QDataSet qds, String sMinKey, String sMaxKey,
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
	int _addRngProp(Element props, String sName, Object oValue){
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
	// children to a property element.  Complex properties dependencies and 
	// associated datasets are not handled here.  Returns the number of props
	// added.
	int _addSimpleProps(Element props, QDataSet qds, String sAxis)
	{
		int nProps = 0;
		
		nProps += _addStrProp(props, qds, QDataSet.FORMAT, sAxis + "Format");
		nProps += _addStrProp(props, qds, QDataSet.SCALE_TYPE, sAxis + "ScaleType");
		nProps += _addStrProp(props, qds, QDataSet.LABEL, sAxis + "Label");
		nProps += _addStrProp(props, qds, QDataSet.DESCRIPTION, sAxis + "Summary");
		
		nProps += _addRealProp(props, qds, QDataSet.FILL_VALUE, sAxis + "Fill");
		nProps += _addRealProp(props, qds, QDataSet.VALID_MIN, sAxis + "ValidMin");
		nProps += _addRealProp(props, qds, QDataSet.VALID_MAX, sAxis + "ValidMax");
		
		nProps += _addRngProp(props, qds, QDataSet.TYPICAL_MIN, QDataSet.TYPICAL_MAX,
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
	int _addPropsFromMap(Element props, Map<String, Object> dMap, boolean bStripDot){
		
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
				nAdded += _addBoolProp(props, sKey, oVal);
			else
				if(oVal instanceof String)
					nAdded += QdsToDas23.this._addStrProp(props, sKey, oVal);
				else
					if(oVal instanceof Number)
						nAdded += QdsToDas23.this._addRealProp(props, sKey, oVal);
					else
						if(oVal instanceof Datum)
							nAdded += QdsToDas23.this._addDatumProp(props, sKey, oVal);
						else
							if(oVal instanceof DatumRange)
								nAdded += QdsToDas23.this._addRngProp(props, sKey, oVal);
			
		}
		return nAdded;
	}
	
}