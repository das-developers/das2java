/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.charset.Charset;

/** Tiny class to count packets in a Das2 or QStream and then write it to a stream
 *
 * @author cwp
 */
public final class DasPktBuf {
	private int m_nPktId;
	private OutputFormat m_fmt;
	private ByteArrayOutputStream m_buf;

	private static final Charset m_csAscii = Charset.forName("US-ASCII");

	/** Make a new buffer for Das2/QStream packet data
	 *  This version does not prepend lengths to the packet ID tags.
	 *
	 * @param nPktId The ID for this packets.  This must match an ID previously sent for
	 *               one of the header packets and so must be a number between 1 and 99.
	 */
	public DasPktBuf(int nPktId){
		this(nPktId, OutputFormat.DAS2);
	}

	/** Make a new buffer for Das2/QStream/Das3 packet data
	 *
	 * @param nPktId The ID for this packets.  This must match an ID previously sent for
	 *               one of the header packets and so must be a number between 1 and 99.
	 *
	 * @param fmt The format of the output data.  Das3 formats expect lengths on all
	 *            packets
	 */
	public DasPktBuf(int nPktId, OutputFormat fmt){
		if((nPktId < 0 )||(nPktId > 99)){
			throw new IllegalArgumentException("Illegal packet id "+nPktId+".  Legal "+
				"das/qstream packet IDs range from 1 and 99, inclusive.");
		}
		m_nPktId = nPktId;
		m_buf = new ByteArrayOutputStream();
		m_fmt = fmt;
	}

	/** Add generic bytes to the packet
	 *
	 * @param bytes The bytes to add
	 */
	public void add(byte[] bytes){
		m_buf.write(bytes, 0, bytes.length);
	}

	/** Add an array of floats to the packet with selectable byte order.
	 * @param lFloats the floats to add to the packet
	 * @param bo define whether bytes should be sent in big-endian or little endian
	 *           format.  Warning: It's up to you to make sure that the byte order
	 *           matches the value in the 'byte_order' attribute for the stream header.
	 */
	public void addFloats(float[] lFloats, ByteOrder bo){

		if((bo == ByteOrder.LITTLE_ENDIAN)&&(m_fmt == OutputFormat.DAS1))
			throw new IllegalArgumentException("DAS1 streams only support big endian values");

		//Successfully do nothing
		if(lFloats.length == 0) return;

		byte[] lBytes = new byte[lFloats.length * 4];
		ByteBuffer bbTmp = ByteBuffer.wrap( lBytes );
		bbTmp.order(bo);
		FloatBuffer fbTmp = bbTmp.asFloatBuffer();
		fbTmp.put(lFloats);
		m_buf.write(lBytes, 0, lBytes.length);
	}

	/** Add a single float to the packet with selectable byte order
	 *
	 * @param lDoubles the value to add to the packet
	 * @param bo define whether bytes should be sent in big-endian or little endian
	 *           format.  Warning: It's up to you to make sure that the byte order
	 *           matches the value in the 'byte_order' attribute for the stream header.
	 */
	public void addDoubles(double[] lDoubles, ByteOrder bo){
		if((bo == ByteOrder.LITTLE_ENDIAN)&&(m_fmt == OutputFormat.DAS1))
			throw new IllegalArgumentException("DAS1 streams only support big endian values");

		if(m_fmt == OutputFormat.DAS1)
			throw new IllegalStateException("DAS1 streams do not support double precision values");

		//Successfully do nothing
		if(lDoubles.length == 0) return;

		byte[] lBytes = new byte[lDoubles.length * 8];
		ByteBuffer bbTmp = ByteBuffer.wrap( lBytes );
		bbTmp.order(bo);
		DoubleBuffer dbTmp = bbTmp.asDoubleBuffer();
		dbTmp.put(lDoubles);
		m_buf.write(lBytes, 0, lBytes.length);

	}

	public void addLongs(long[] lLongs, ByteOrder bo){
		if((bo == ByteOrder.LITTLE_ENDIAN)&&(m_fmt == OutputFormat.DAS1))
			throw new IllegalArgumentException("DAS1 streams only support big endian values");

		if(m_fmt != OutputFormat.QSTREAM)
			throw new IllegalStateException("Only QSTREAM format supports long integers.");

		//Successfully do nothing
		if(lLongs.length == 0) return;

		byte[] lBytes = new byte[lLongs.length * 8];
		ByteBuffer bbTmp = ByteBuffer.wrap( lBytes );
		bbTmp.order(bo);
		LongBuffer lbTmp = bbTmp.asLongBuffer();
		lbTmp.put(lLongs);
		m_buf.write(lBytes, 0, lBytes.length);
	}

	/** Encode a string in US-ASCII format, and add it to the output buffer
	 *
	 * @param s The string to encode
	 */
	public void addAscii(String s){
		byte[] lBytes = s.getBytes(m_csAscii);
		m_buf.write(lBytes, 0, lBytes.length);
	}

	/** Kick the encoded packet data onto the stream.
	 * @param fOut Where to write the packet
	 * @throws IOException
	 */
	public void send(OutputStream fOut) throws IOException{
		byte[] u1Tmp;
		switch(m_fmt){
		case DAS3:
			u1Tmp = String.format("|%02d|%06d", m_nPktId, m_buf.size()).getBytes(m_csAscii);
			assert u1Tmp.length == 10;
			break;
		default:
			u1Tmp = String.format(":%02d:", m_nPktId).getBytes(m_csAscii);
			assert u1Tmp.length == 4;
		}
		fOut.write(u1Tmp);
		m_buf.writeTo(fOut);
		m_buf = new ByteArrayOutputStream();
	}
	

}
