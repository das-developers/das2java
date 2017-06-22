/* File: StreamHandler.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 11, 2004, 10:57 AM
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

package org.das2.qstream;

import java.nio.ByteBuffer;

/**
 * This describes an object that is able to receive a stream from StreamTool.readStream, which breaks the stream up
 * into descriptors and packets.  This was copied from das2's das2stream library to create QStream, which is to replace it.
 * See http://das2.org/wiki/index.php/Das2/QStreams
 * 
 * @author  Jeremy Faden, Ed West wrote original das2stream library
 */
public interface StreamHandler {

    /**
     * initial description of the stream.  This contains global properties of the stream.
     * TODO: should codes expect only one of these?  I think most probably do, but the spec says we should be able to pick up
     * in the middle, implying that steamDescriptors may be resent.
     * @param sd
     * @throws StreamException
     */
    void streamDescriptor(StreamDescriptor sd) throws StreamException;

    /**
     * description of a new packet type. This packetDescriptor will also be sent as packets of data are received.
     * @param pd
     * @throws StreamException
     */
    void packetDescriptor(PacketDescriptor pd) throws StreamException;

    /**
     * receive a data packet from the stream.  The packet descriptor is used to describe the packet contents,
     * and the ByteBuffer contains the bytes.  The byte buffer will have its position at the beginning of the data, and the limit
     * will be the end of the data.  Note for filters, the buffer must be reset!
     * @param pd PacketDescriptor describing the data.
     * @param data ByteBuffer containing the data.
     * @throws StreamException when something is wrong with the content of the stream.
     */
    void packet(PacketDescriptor pd, ByteBuffer data ) throws StreamException;

    /**
     * indicates the stream end is encountered.
     * @param sd
     * @throws StreamException
     */
    void streamClosed(StreamDescriptor sd) throws StreamException;

    /**
     * This is used to indicate an exception occurred in the source.
     * @param se
     * @throws StreamException
     */
    void streamException(StreamException se) throws StreamException;

    /**
     * comments on the stream.
     * @param sd
     */
    void streamComment(StreamComment sd) throws StreamException;
    
    
}
