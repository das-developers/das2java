/* File: ByteBufferInputStream.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 4, 2004, 9:34 AM
 *      by Edward West <eew@space.physics.uiowa.edu>
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;

/** An input stream that wraps an NIO ByteBuffer.  Reading from this stream
 * will update the ByteBuffers position.  Calling mark() on this input stream
 * will set the mark on the underlying buffer.
 *
 * @author  eew
 */
public class ByteBufferInputStream extends InputStream {
    
    private ByteBuffer buffer;
    
    /** Creates a new instance of ByteBufferInputStream */
    public ByteBufferInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }
    
    public ByteBuffer getByteBuffer() {
        return buffer;
    }
    
    public int read() {
        if (buffer.hasRemaining()) {
            return 0xFF & buffer.get();
        }
        else {
            return -1;
        }
    }
    
    public void close() {
        //Do nothing
    }
    
    public long skip(long n) {
        if (n > buffer.remaining()) {
            long skipped = buffer.remaining();
            buffer.position(buffer.limit());
            return skipped;
        }
        else {
            buffer.position(buffer.position() + (int)n);
            return n;
        }
        
    }
    
    public void reset() throws IOException {
        try {
            buffer.reset();
        }
        catch (InvalidMarkException ime) {
            IOException ioe = new IOException(ime.getMessage());
            ioe.initCause(ime);
            throw ioe;
        }
    }
    
    public int available() {
        return buffer.remaining();
    }
    
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }
    
    public boolean markSupported() {
        return true;
    }
    
    public void mark(int readlimit) {
        buffer.mark();
    }
    
    public int read(byte[] b, int off, int len) {
        if (buffer.hasRemaining()) {
            int bytesRead = Math.min(len, buffer.remaining());
            buffer.get(b, off, bytesRead);
            return bytesRead;
        }
        else {
            return -1;
        }
    }
    
}
