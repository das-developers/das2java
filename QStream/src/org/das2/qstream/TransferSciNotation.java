/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qstream;



import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/* Custom transfer type for exponential notation, use a small 'e'
 * and don't print + signs.
 */
class TransferSciNotation extends TransferType {

		final int nLen;
		private String sFmt;

		public TransferSciNotation(int nSigDigits) {
			if (nSigDigits < 2 || nSigDigits > 17) {
				throw new IllegalArgumentException(String.format(
						  "Significant digits for output must be between 2 and 17 "
						  + "inclusive, recieved %d", nSigDigits
				));
			}
			// 7 = room for sign(1), decimal(1), exponent (4) and trailing space(1)
			nLen = nSigDigits + 7;
			sFmt = String.format("%%.%de", nSigDigits - 1);
		}

		@Override
		public void write(double rVal, ByteBuffer buffer) {
			String sVal = String.format(sFmt, rVal);
			if (rVal >= 0.0) {
				buffer.put((byte) 32);  // take up room used by - sign
			}
			buffer.put(sVal.getBytes(StandardCharsets.US_ASCII));
			buffer.put((byte) 32);
		}

		@Override
		public double read(ByteBuffer buffer) {
			byte[] bytes = new byte[nLen];
			buffer.get(bytes);
			String str;
			try {
				str = new String(bytes, StandardCharsets.US_ASCII).trim();
				return Double.parseDouble(str);
			} catch (NumberFormatException ex) {
				return Double.NaN;
			}
		}
		
		@Override
		public int sizeBytes()   { return nLen; }
		@Override
		public boolean isAscii() { return true; }
		@Override
		public String name()     { return "ascii" + nLen; }
	}