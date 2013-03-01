/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/** Tiny class to count bytes in a header packet and then write it to a stream
 *
 * @author cwp
 */
public final class DasHdrBuf {
	private int m_nPktId;
	private ByteArrayOutputStream m_buf;

	/** Make a new buffer for QStream/Das2 header data
	 *
	 * @param nPktId The ID for the packets this header describes, must be a number
	 *               between 1 and 99.
	 *
	 * @throws UnsupportedEncodingException
	 */
	public DasHdrBuf(int nPktId) throws UnsupportedEncodingException{
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
	 * @throws UnsupportedEncodingException
	 */
	public void add(String sTxt) throws UnsupportedEncodingException{
		byte[] u1Tmp = sTxt.getBytes("UTF-8");
		m_buf.write(u1Tmp, 0, u1Tmp.length);
	}

	/** Send a completed header out the door.
	 * This has the side effect of clearing the buffer for re-use.
	 * 
	 * @param fOut Any output stream you like
	 */
	public void send(OutputStream fOut) throws UnsupportedEncodingException, IOException{
		byte[] u1Tmp = String.format("[%02d]%06d", m_nPktId, m_buf.size()).getBytes("US-ASCII");
		assert u1Tmp.length == 10;
		fOut.write(u1Tmp);
		m_buf.writeTo(fOut);
		fOut.flush();
		m_buf = new ByteArrayOutputStream();
		add("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
	}
}
