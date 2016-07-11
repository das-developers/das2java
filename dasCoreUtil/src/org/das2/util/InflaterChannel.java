/* File: InflaterChannel.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on March 26, 2004, 10:00 AM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.das2.util;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 *
 * @author  eew
 */
public class InflaterChannel implements ReadableByteChannel {
    
    private boolean closed = false;
    private boolean eof = false;
    private ReadableByteChannel in;
    private Inflater inflater;
    private ByteBuffer buf;
    private byte[] outBuf;
    
    /** Creates a new instance of InflaterChannel */
    public InflaterChannel(ReadableByteChannel in) {
        this.in = in;
        byte[] array = new byte[4096];
        buf = ByteBuffer.wrap(array);
        inflater = new Inflater();
    }
    
    public synchronized void close() throws IOException {
        if (!closed) {
            closed = true;
            in.close();
            in = null;
            inflater.end();
            inflater = null;
            buf = null;
        }
    }
    
    public boolean isOpen() {
        return !closed;
    }
    
    public synchronized int read(ByteBuffer dst) throws IOException {
        int offset, length, totalOut, bytesRead;
        byte[] outBuf;
        try {
            if (closed) { throw new ClosedChannelException(); }
            if (eof && inflater.finished()) {  return -1; }
            if (dst.hasArray()) {
                outBuf = dst.array();
                offset = dst.arrayOffset() + dst.position();
                length = dst.remaining();
            }
            else {
                outBuf = this.outBuf == null
                    ? (this.outBuf = new byte[4096]) : this.outBuf;
                offset = 0;
                length = Math.min(outBuf.length, dst.remaining());
            }
            totalOut = 0;
            while (totalOut < length) {
                if (inflater.needsInput()) {
                    buf.clear();
                    if (in.read(buf) == -1) {
                        eof = true;
                    }
                    buf.flip();
                    inflater.setInput
                        (buf.array(), buf.arrayOffset(), buf.remaining());
                }
                bytesRead = inflater.inflate
                    (outBuf, offset + totalOut, length - totalOut);
                totalOut += bytesRead;
                if (inflater.finished()) { break; }
                if ( eof ) break;
            }
            if (dst.hasArray()) { dst.position(dst.position() + totalOut); }
            else { dst.put(outBuf, 0, totalOut); }
            return totalOut;
        }
        catch (DataFormatException dfe) {
            eof = true;
            close();
            IOException ioe = new IOException(dfe.getMessage());
            ioe.initCause(dfe);
            throw ioe;
        }
    }
    
}
