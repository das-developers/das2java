/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qstream;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.format.DatumFormatter;
import org.das2.datum.format.TimeDatumFormatter;

/** Custom ISO time transfer type handles fractional seconds at arbitrary
 * precision instead of ms, microsec, and nanosec only
 */
class TransferTimeAtPrecision extends TransferType {

	Units units;
	DatumFormatter formatter;
	byte[] aFill;
	int nSize;

	/**
	 * Create a time datum formatter with a variable length number of seconds
	 *
	 * @param units the units of the double values to convert to an ascii time
	 * @param nFracSec Any value from 0 to 12 (picoseconds) inclusive.
	 */
	public TransferTimeAtPrecision(Units units, int nFracSec) {
		this.units = units;

		// yyyy-mm-ddThh:mm:ss.ssssssssssss +1 for space at end
		// 12345678901234567890123456789012
		nSize = 20;
		StringBuilder sFmt = new StringBuilder("yyyy-MM-dd'T'HH:mm:ss");
		StringBuilder sFill = new StringBuilder("                   ");

		if (nFracSec > 0) {
			sFmt.append(".");
			sFill.append(" ");
		}
		for (int i = 0; i < nFracSec; ++i) {
			sFmt.append("S");
			sFill.append(" ");
		}
		sFmt.append(" ");
		sFill.append(" ");

		aFill = sFill.toString().getBytes(StandardCharsets.US_ASCII);
		try {
			formatter = new TimeDatumFormatter(sFmt.toString());
		} catch (ParseException ex) {
			throw new RuntimeException(ex);
		}

		if (nFracSec > 0) {
			++nSize;  //decimal point
		}
		nSize += nFracSec;
	}

	@Override
	public void write(double rVal, ByteBuffer buffer) {
		if (units.isFill(rVal)) {
			buffer.put(aFill);
		} else {
			String sOut = formatter.format(units.createDatum(rVal));
			buffer.put(sOut.getBytes(StandardCharsets.US_ASCII));
		}
	}

	@Override
	public double read(ByteBuffer buffer) {
		try {
			byte[] aBuf = new byte[nSize];
			buffer.get(aBuf);
			String sTime = new String(aBuf, "US-ASCII").trim();
			double result = TimeUtil.create(sTime).doubleValue(units);
			return result;
		} catch (UnsupportedEncodingException | ParseException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int sizeBytes() {
		return nSize;
	}

	@Override
	public boolean isAscii() {
		return true;
	}

	@Override
	public String name() {
		return String.format("time%d", nSize);
	}
}
