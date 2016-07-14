/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/** Tiny class to count bytes in a header packet and then write it to a stream
 *
 * @author cwp
 */
public final class DasHdrBuf {
	private int m_nPktId;
	private ByteArrayOutputStream m_buf;

	private static final Charset m_csUtf8 = Charset.forName("UTF-8");
	private static final Charset m_csAscii = Charset.forName("US-ASCII");

	/** Make a new buffer for QStream/Das2 header data
	 *
	 * @param nPktId The ID for the packets this header describes, must be a number
	 *               between 1 and 99.
	 *
	 */
	public DasHdrBuf(int nPktId){
		if((nPktId < 0 )||(nPktId > 99)){
			throw new IllegalArgumentException("Illegal packet id "+nPktId+".  Legal "+
				"das/qstream packet IDs range from 1 and 99, inclusive.");
		}
		m_nPktId = nPktId;
		m_buf = new ByteArrayOutputStream();
		add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}

	/** Add more text data to the header.
	 *
	 * @param sTxt The text to add, will be encoded as UTF-8 for storage
	 */
	public void add(String sTxt){
		byte[] u1Tmp = sTxt.getBytes(m_csUtf8);
		m_buf.write(u1Tmp, 0, u1Tmp.length);
	}

	/** Send a completed header out the door.
	 * This has the side effect of clearing the buffer for re-use.
	 * 
	 * @param fOut Any output stream you like
	 */
	public void send(OutputStream fOut) throws IOException{
		byte[] u1Tmp = String.format("[%02d]%06d", m_nPktId, m_buf.size()).getBytes(m_csAscii);
		assert u1Tmp.length == 10;
		fOut.write(u1Tmp);
		m_buf.writeTo(fOut);
		fOut.flush();
		m_buf = new ByteArrayOutputStream();
		add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}
}
