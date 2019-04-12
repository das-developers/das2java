/* File: ToAscii.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 17, 2004, 11:36 AM
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

package org.das2.stream;

import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 *
 * @author  eew
 */
public class ToAscii implements StreamHandler {
    
    private Map descriptors = new IdentityHashMap();
    private StreamHandler handler;
    
    /** Creates a new instance of ToAscii */
    public ToAscii(StreamHandler handler) {
        this.handler = handler;
    }
    
    public void packet(PacketDescriptor pd, Datum xTag, DatumVector[] vectors) throws StreamException {
        PacketDescriptor outpd = (PacketDescriptor)descriptors.get(pd);
        handler.packet(outpd, xTag, vectors);
    }

    public void packetDescriptor(PacketDescriptor pd) throws StreamException {
        DataTransferType ascii24 = DataTransferType.getByName("ascii24");
        DataTransferType ascii10 = DataTransferType.getByName("ascii10");
        PacketDescriptor outpd = (PacketDescriptor)pd.clone();
        outpd.getXDescriptor().setDataTransferType(ascii24);
        for (int i = 0; i < outpd.getYCount(); i++) {
            if (outpd.getYDescriptor(i) instanceof StreamScalarDescriptor) {
                ((StreamScalarDescriptor)outpd.getYDescriptor(i)).setDataTransferType(ascii10);
            }
            else if (outpd.getYDescriptor(i) instanceof StreamYScanDescriptor) {
                ((StreamYScanDescriptor)outpd.getYDescriptor(i)).setDataTransferType(ascii10);
            }
        }
        handler.packetDescriptor(outpd);
        descriptors.put(pd, outpd);
    }
    
    public void streamClosed(StreamDescriptor sd) throws StreamException {
        handler.streamClosed(sd);
    }
    
    public void streamDescriptor(StreamDescriptor sd) throws StreamException {
        handler.streamDescriptor(sd);
    }
    
    public void streamException(StreamException se) throws StreamException {
        handler.streamException(se);
    }
    
    public void streamComment(StreamComment se) throws StreamException {
        handler.streamComment(se);
    }
    
    public void packet(PacketDescriptor pd, DatumVector vector) throws StreamException {
    }
    
}
