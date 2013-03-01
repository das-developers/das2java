/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import org.das2.util.reader.OutputFormat;

/** Tiny class to count packets in a Das2 or QStream and then write it to a stream
 *
 * @author cwp
 */
public final class DasPktBuf {
	private int m_nPktId;
	private OutputFormat m_fmt;
	private ByteArrayOutputStream m_buf;


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
	}

	public void add(byte[] bytes){
		m_buf.write(bytes, 0, bytes.length);
	}

	public void send(OutputStream fOut) throws UnsupportedEncodingException, IOException{
		byte[] u1Tmp;
		switch(m_fmt){
		case DAS3:
			u1Tmp = String.format("|%02d|%06d", m_nPktId, m_buf.size()).getBytes("US-ASCII");
			assert u1Tmp.length == 10;
			break;
		default:
			u1Tmp = String.format(":%02d:", m_nPktId).getBytes("US-ASCII");
			assert u1Tmp.length == 4;
		}
		fOut.write(u1Tmp);
		m_buf.writeTo(fOut);
		m_buf = new ByteArrayOutputStream();
	}

}
