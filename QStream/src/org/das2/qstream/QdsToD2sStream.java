/* This Java package, org.autoplot.das2Stream is part of the Autoplot application
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
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.das2.datum.LoggerManager;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;
import org.das2.qds.DataSetOps;
import org.das2.qds.MutablePropertyDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;
import org.das2.qds.ops.Ops;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;


/** Base class for QDataSet to das2 stream serializers 
 * 
 */
abstract public class QdsToD2sStream {

	// Private ////////////////////////////////////////////////////////////////
	private static final Logger log = LoggerManager.getLogger("qstream");
	
	// Public interface //////////////////////////////////////////////////////
	
	public static final String FORMAT_2_2 = "2.2";
	public static final String FORMAT_2_3_BASIC = "2.3/basic";
	public static final String FORMAT_2_4_GENERAL = "2.4/general";
	public static final String[] formats = {FORMAT_2_2, FORMAT_2_3_BASIC};
	
	public static final int DEFAUT_FRAC_SEC = 3;
	public static final int DEFAUT_SIG_DIGIT = 5;
	
	public static final int FIXED_PKT_TAGS = 0;  // original tag format 
	public static final int VAR_PKT_TAGS = 1;    // new variable packet tags
	
	/** Initialize a binary QDataSet to das2 stream exporter 
	 */
	public QdsToD2sStream(){ 
		bBinary = true;
	}
	
	/** Initialize a text QDataSet to das2 stream exporter
	 * 
	 * @param genSigDigits The number of significant digits used for general
	 *    text value data output.  If you don't know what else to use, 5 is
	 *    typically a fine precision without being ridiculous.
	 * 
	 * @param fracSecDigits  The number of fractional seconds digits to
	 *    use for ISO-8601 date-time values. The number of fraction seconds
	 *    can be set as low as 0 and as high as 12 (picoseconds).  If
	 *    successive time values vary by less than the specified precision 
	 *    (but not 0, repeats are accepted) then stream writing fails.  Use 3 (i.e.
	 *    microseconds) if you don't know what else to choose.
	 */
	public QdsToD2sStream(int genSigDigits, int fracSecDigits){
		
		bBinary = false;
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
	
	/** Determine if the given dataset can be written
	 * 
	 * @param qds
	 * @return 
	 */
	public abstract boolean canWrite(QDataSet qds);
	
	/** Write the given dataset
	 * 
	 * @param qds
	 * @param os
	 * @return 
	 * @throws java.io.IOException 
	 */
	public abstract boolean write(QDataSet qds, OutputStream os) 
		throws IOException;
	
	// protected //////////////////////////////////////////////////////////
	
	/** Write a UTF-8 header string onto the stream, if packet ID == 0 then
	 * write a stream header
	 * 
	 * @param os The output stream
	 * @param nTagType Either FIXED_PKT_TAGS or VAR_PKT_TAGS
	 * @param nPktId
	 * @param sHdr - The header string to write for this packet id
	 * @throws UnsupportedEncodingException
	 * @throws IOException 
	 */
	static void writeHeader(OutputStream os, int nTagType, int nPktId, String sHdr) 
		throws UnsupportedEncodingException, IOException{
		
		byte[] aHdr = sHdr.getBytes(StandardCharsets.UTF_8);
		String sTag;
		if(nTagType == VAR_PKT_TAGS){
			if(nPktId == 0)
				sTag = String.format("|Hs||%d|", aHdr.length);
			else
				sTag = String.format("|Hx|%d|%d|", nPktId, aHdr.length);
		}
		else{
			sTag = String.format("[%02d]%06d", nPktId, aHdr.length);
		}
		byte[] aTag = sTag.getBytes(StandardCharsets.US_ASCII);
		os.write(aTag);
		os.write(aHdr);
	}
	
	// Handling for text output, throw this back to the user if they want text
	// output.  The reason not to just do our best to figure it out on our own
	// is that it changes the packet header definitions, which we don't want to
	// do when writing cache files.
	protected int nSigDigit = DEFAUT_SIG_DIGIT;
	protected int nSecDigit = DEFAUT_FRAC_SEC;
	protected boolean bBinary;
	
	/** Determine and hold the information needed to transfer values out of a
	 * given QDataSet into a byte buffer.  The byte order is always native 
	 * endian
	 */
	static protected class QdsXferInfo {
		QDataSet qds;
		TransferType transtype;
		
		/** Figure out how to represent the values.  In general everything is 
		 * output as a float unless it's an epoch time type.
		 * 
		 * @param _qds  The QDataset
		 * @param bBinary True if user wanted binary data
		 * @param nGenDigit Number of significant digits for general text values
		 * @param nFracSec Number of fractional seconds for time text values
		 */
		QdsXferInfo(
			QDataSet _qds, boolean bBinary, int nGenSigDigit, int nFracSec
		){
			qds = _qds;
			
			if(bBinary){ nGenSigDigit = -1; nFracSec = -1; }
			
			Units units = (Units) qds.property(QDataSet.UNITS);
			if((units != null) && (units instanceof TimeLocationUnits)){
				if(nFracSec < 0)
					transtype = new DoubleTransferType();
				else
					transtype = new TransferTimeAtPrecision(units, nFracSec);
			}
			else{
				//Non epoch stuff
				Class c = DataSetOps.getComponentType(qds);
				if((c == int.class)||(c == long.class)){
					if(bBinary){
						if(c == int.class) transtype = new IntegerTransferType();
						else transtype = new LongTransferType();
					}
					else{
						// Yea, couldn't be something like QDataSet.max()
						QDataSet dsExt = Ops.extent(qds);
						double rMax = Math.abs(dsExt.value(0));
						double rMin = Math.abs(dsExt.value(1));
						if(rMin > rMax) rMax = rMin;
						
						int nChars = (int)(Math.ceil( Math.log10( rMax) ) + 2);
						transtype = new AsciiIntegerTransferType(nChars);
					}
				}
				else{
					if(nGenSigDigit < 0)
						transtype = new FloatTransferType();
					else
						transtype = new TransferSciNotation(nGenSigDigit);
				}
			}
		}
		
		/** Get a size independent type name 
		 * @return  the type with any size information stripped off */
		public String name(){
			// Since float and double indicate size, use integer names
			// that indicate size as well for consistency.
			if(transtype.name().equals("int8")) return "long";
			else return transtype.name().replaceAll("\\d","");
		}
		
		/** @return the size in bytes of each output value */
		public int size(){ return transtype.sizeBytes(); }
		
		/** Get the number items in a single X-axis slice of this dataset the
		 * X-axis is synonymous with the QDataSet 0th axis for now.
		 * @param i - The X point at which to get the items
		 * @return 
		 */
		public int xSliceItems(int i) throws IOException{
			int nItems = 0;
			switch(qds.rank()){
			case 1: return 1;
			case 2: return qds.length(i);
			case 3:
				for(int j=0; j < qds.length(i); ++j)
					nItems += qds.length(i,j);
				return nItems;
			case 4:
				// Das2 can't really do this, but calc anyway
				for(int j=0; j < qds.length(i); ++j){
					for(int k=0; k < qds.length(i,j); ++k)
						nItems += qds.length(i,j,k);
				}
				return nItems;
			default:
				throw new IOException(String.format(
					"Can't stream rank %d data with this format.", qds.rank()
				));
			}
		}
		
		/** Get the number of bytes needed to hold an x-slice of a single
		 * 
		 * @param i
		 * @return 
		 */
		public int xSliceBytes(int i) throws IOException{
			return transtype.sizeBytes() * xSliceItems(i);
		}
	}
	
	// Hold the information for serializing a single packet and it's dependencies
	static protected class PacketXferInfo {
		Document doc;        // Overall header document
		List<QdsXferInfo> lDsXfer;
		
		PacketXferInfo(Document _doc, List<QdsXferInfo> _lDsXfer ){
			doc = _doc; lDsXfer = _lDsXfer;
		}
		
		int datasets(){ return lDsXfer.size(); }
		
		// Calc length of buffer needed to hold a single slice in the 0th
		// dimension across all qdatasets.
		int xSliceBytes() throws IOException {
			int nLen = 0;
			for(QdsXferInfo qi: lDsXfer) 
				nLen += qi.xSliceBytes(0);
			return nLen;
		}
	}	

	// Send Bundle data
	protected void writeData(
		OutputStream out, int nTagType, int iPktId, PacketXferInfo pktXfer
	) throws IOException {
		
		WritableByteChannel channel = Channels.newChannel(out);
						
		// Make the Xslice record tag.  For now these all have the same length
		// if das2/general format is ever introduced will have to check the 
		// length for each X-slice.
		String sRecTag;
		if(nTagType == VAR_PKT_TAGS){
			sRecTag = String.format("|Dx|%d|%d|", iPktId, pktXfer.xSliceBytes());
		}
		else{
			sRecTag = String.format(":%02d:", iPktId);
		}
		
		byte[] aRecTag = sRecTag.getBytes(StandardCharsets.US_ASCII);
		
		int nBufLen = pktXfer.xSliceBytes() + aRecTag.length;
		byte[] aBuf = new byte[nBufLen];
		ByteBuffer buffer = ByteBuffer.wrap(aBuf);
		buffer.order(ByteOrder.nativeOrder());        // Since we have a choice, use native
		
		buffer.put(aRecTag);                          // tag stays in buffer
		
		int nPkts = pktXfer.lDsXfer.get(0).qds.length();
		for(int iPkt = 0; iPkt < nPkts; ++iPkt){
			
			QdsXferInfo qi = null;
			for(int iDs = 0; iDs < pktXfer.datasets(); ++iDs){
				qi = pktXfer.lDsXfer.get(iDs);
				
				switch(qi.qds.rank()){
				case 1: 
					qi.transtype.write(qi.qds.value(iPkt), buffer); 
					break;
				case 2: 
					// Does not assume cubic, if stream writers expect cubic, they
					// need to check for it before calling this function
					for(int iVal = 0; iVal < qi.qds.length(iPkt); ++iVal)
						qi.transtype.write(qi.qds.value(iPkt, iVal), buffer);
					break;
				case 3: 
					// Does not assume cubic, if stream writers expect cubic, they
					// need to check for it before calling this function
					for(int iVal = 0; iVal < qi.qds.length(iPkt); ++iVal)
						for(int jVal = 0; jVal < qi.qds.length(iPkt, iVal); ++jVal)
							qi.transtype.write(qi.qds.value(iPkt, iVal, jVal), buffer);
					break;
				default:
					assert(false);
				}
			}
			if(qi != null && qi.transtype.isAscii()) 
				buffer.put(nBufLen - 1, (byte)'\n');
			buffer.flip();
			channel.write(buffer);
			buffer.position(aRecTag.length);
		}
	}	
	
	// Secondary helpers functions below here //////////////////////////////////
	
	protected String makeNameFromUnits(Units units)
	{
		if(units == null) return "";
		if(UnitsUtil.isTimeLocation(units)) return "time";
		if(units.isConvertibleTo(Units.meters)) return "length";
		if(units.isConvertibleTo(Units.hertz)) return "frequnecy";
		if(units.isConvertibleTo(Units.eV)) return "energy";
		if(units.isConvertibleTo(Units.degrees)) return "angle";
		if(units.isConvertibleTo(Units.seconds)) return "interval";
		if(units.isConvertibleTo(Units.bytes)) return "size";
		if(units.isConvertibleTo(Units.cm_2s_1keV_1)) return "flux";
		if(units.isConvertibleTo(Units.kelvin)) return "temperature";
		// So where is spectral density? 
		return "";
	}
	
	
	protected boolean _stripDotProps(QDataSet qds){
		// Ephemeris data (x-multi-y) ported in from das1 add many redundant 
		// properties get rid of those if this is a bundle_1 dataset and each 
		// sub-item is rank 1
		
		if(qds.property(QDataSet.BUNDLE_1) != null){
			for(int i = 0; i < qds.length(0); ++i){
				QDataSet ds = DataSetOps.slice1(qds, i);
				if(ds.rank() != 1) return false;
			}
		}
		else{
			return false;
		}
		return true;
	}
	
	static String xmlDocToStr(Document doc)
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
		
	protected Document newXmlDoc() {
		try {
			DocumentBuilder bldr = 
					  DocumentBuilderFactory.newInstance().newDocumentBuilder();
			return bldr.newDocument();
		} catch (ParserConfigurationException pce) {
			throw new RuntimeException(pce);
		}
	}
	
	protected static class Sequence1D {
		String sMinval = null;
		String sInterval = null;
	}
	
	/** Determine if a rank 1 dataset is really just a sequence
	 * 
	 * @param qds
	 * @param rMaxJitter Recommend 1e-4 if no other values comes to mind
	 * @return A Sequence1D object with minval and interval set.
	 */
	protected Sequence1D getSequenceRank1(QDataSet qds, double rMaxJitter)
	{
		if(qds.rank() != 1) return null;
		if(qds.length() < 2) return null;
		double rMin = qds.value(0);
		
		// Try to use sequence representation if we can, allow for jitter in
		// intervals of one part in the max jitter
		double rInterval = (qds.value( qds.length() - 1) - rMin)/(qds.length() - 1);
		double rNextInterval, rAvg, rDelta, rJitter;
		for(int i = 2; i < qds.length(); ++i){
			rNextInterval = qds.value(i) - qds.value(i - 1);
			if(rNextInterval != rInterval){
				rAvg = Math.abs(rNextInterval + rInterval)/2;
				rDelta = Math.abs(rNextInterval - rInterval);
				if(rDelta == 0.0) continue;
				if(rAvg == 0.0)
					return null;
				rJitter =  rDelta/ rAvg;
				if(rJitter > rMaxJitter)
					return null;
			}
		}
		
		// Get my format string
		String sFmt = String.format("%%.%de", nSigDigit - 1);
		
		Sequence1D seq = new Sequence1D();
		seq.sMinval = String.format(sFmt, rMin);
		seq.sInterval = String.format(sFmt, rInterval);
		return seq;
	}
	

	///////////////////////////////////////////////////////////////////////////
	// Das2 specific dataset properties and functions that could just be an add
	// on for qdataset.  These could be move somewhere else.	
	
	/** Determine the name of the das2 axis on which values from a dataset
	 * would typically be plotted.
	 * 
	 * This is a duck-typing check.  Which looks at the number of dependencies 
	 * and planes in a dataset.  If the dataset has a PLANE_0 property, the axis
	 * of the PLANE_0 values is returned instead of the axis of the primary
	 * dataset.
	 * 
	 * @param qds
	 * @return one of "x", "y", "z","w" or null if we can't figure it out.
	 */
	public static String getQdsAxis(QDataSet qds){
		
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
		case 3: return "w";
		case 1: 
			// TODO: Read input to see how x/y/z data are handled
			if(qds.property(QDataSet.DEPEND_0) == null) return "x";
			if(qds.property(QDataSet.PLANE_0) == null) return "y";
			return "z";
		}
		return null;
	}
	
	private static final List<String> lSimpleKeys;
	static {
		lSimpleKeys = new ArrayList<>();
		lSimpleKeys.add(QDataSet.NAME);
		lSimpleKeys.add(QDataSet.UNITS);
		lSimpleKeys.add(QDataSet.FORMAT);
		lSimpleKeys.add(QDataSet.SCALE_TYPE);
		lSimpleKeys.add(QDataSet.LABEL);
		lSimpleKeys.add(QDataSet.DESCRIPTION);
		lSimpleKeys.add(QDataSet.FILL_VALUE);
		lSimpleKeys.add(QDataSet.VALID_MIN);
		lSimpleKeys.add(QDataSet.VALID_MAX);
		lSimpleKeys.add(QDataSet.TYPICAL_MIN);
		lSimpleKeys.add(QDataSet.TYPICAL_MAX);
		lSimpleKeys.add(QDataSet.USER_PROPERTIES);
	}
	
	/** Only copy over the simple properties of a dataset, ignore the structural
	 * items such as depend, offset, axis, reference, bundle etc.
	 * 
	 * @param dsDest
	 * @param dsSrc
	 * @return 
	 */
	protected int copySimpleProps(MutablePropertyDataSet dsDest, QDataSet dsSrc)
	{
		int nAdded = 0;
		Object oVal;
		for(String sKey: lSimpleKeys){
			if( (oVal = dsSrc.property(sKey)) != null){
				dsDest.putProperty(sKey, oVal);
				nAdded += 1;
			}
		}
		return nAdded;
	}
		
}