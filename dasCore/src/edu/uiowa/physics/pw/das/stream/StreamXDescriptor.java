/* File: StreamXDescriptor.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
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
package edu.uiowa.physics.pw.das.stream;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import org.w3c.dom.*;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;

public class StreamXDescriptor implements SkeletonDescriptor {
    
    private Datum base;
    
    private Units baseUnits = Units.us2000;
    
    private Units units = Units.seconds;
    
    private DataTransferType transferType = DataTransferType.SUN_REAL4;
    
    public StreamXDescriptor() {
    }
    
    public StreamXDescriptor( Element element ) {
        if ( element.getTagName().equals("x") ) {
            processElement(element);
        }
        else {
            processLegacyElement(element);
        }
    }
    
    private void processElement( Element element ) {
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        if ( type != null ) {
            transferType = type;
        }
        else {
            throw new RuntimeException("Illegal transfer type: " + typeStr);
        }
        String baseString = element.getAttribute("base");
        if (baseString != null) {
            if (baseUnits instanceof TimeLocationUnits) {
                base = TimeUtil.createValid(baseString);
            }
        }
    }
    
    private void processLegacyElement( Element element ) {
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        if (type != null) {
            transferType = type;
        }
        else {
            throw new RuntimeException("Illegal transfer type: " + typeStr);
        }
    }
    
    public Datum getBase() {
        return base;
    }
    
    public int getSizeBytes() {
        return transferType.getSizeBytes();
    }
    
    public Units getUnits() {
        return units;
    }
    
    public void setUnits(Units units) {
        this.units = units;
    }
    
    public void setDataTransferType(DataTransferType transferType) {
        this.transferType = transferType;
    }
    
    public DataTransferType getDataTransferType() {
        return transferType;
    }
    
    public void read(ByteBuffer input, double[] output, int offset) {
        output[offset] = transferType.read(input);
    }
    
    public void write(double[] input, int offset, ByteBuffer output) {
        transferType.write(input[offset], output);
    }
    
}

