/* File: DeflaterChannel.java
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
import java.util.zip.Deflater;

/**
 *
 * @author  eew
 */
public class DeflaterChannel implements WritableByteChannel {
    
    private boolean closed = false;
    private WritableByteChannel out;
    private Deflater deflater;
    private ByteBuffer buf;
    private byte[] inBuf;
    
    public DeflaterChannel(WritableByteChannel out) {
        this.out = out;
        byte[] array = new byte[4096];
        buf = ByteBuffer.wrap(array);
        deflater = new Deflater();
    }
    
    public void flush() throws IOException {
        int byteCount;
        deflater.finish();
        while ((byteCount = deflater.deflate(buf.array())) != 0) {
            buf.position(0).limit(byteCount);
            while(buf.hasRemaining()) {
                out.write(buf);
            }
        }
    }
    
    public synchronized void close() throws IOException {
        if (!closed) {
            flush();
            closed = true;
            out.close();
            out = null;
            deflater.end();
            deflater = null;
            buf = null;
        }
    }
    
    public boolean isOpen() {
        return !closed;
    }
    
    public synchronized int write(ByteBuffer src) throws IOException {
        byte[] inBuf;
        int offset, length;
        if (src.hasArray()) {
            inBuf = src.array();
            offset = src.arrayOffset();
            length = src.remaining();
            src.position(src.position() + src.remaining());
        }
        else {
            if (this.inBuf == null) {
                this.inBuf = new byte[4096];
            }
            inBuf = this.inBuf;
            offset = 0;
            length = Math.min(inBuf.length, src.remaining());
            src.get(inBuf, 0, length);
        }
        deflater.setInput(inBuf, offset, length);
        while (!deflater.needsInput()) {
            int byteCount = deflater.deflate(buf.array());
            buf.position(0).limit(byteCount);
            while (buf.hasRemaining()) { out.write(buf); }
        }
        return length;
    }
    
}
