/* File: StreamMultiYDescriptor.java
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

import edu.uiowa.physics.pw.das.datum.Units;
import java.nio.ByteBuffer;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class StreamMultiYDescriptor implements SkeletonDescriptor, Cloneable {
    
    private String name = "";
    private Units units = Units.dimensionless;
    private DataTransferType transferType = DataTransferType.SUN_REAL4;
    
    public StreamMultiYDescriptor(Element element) {
        if ( element.getTagName().equals("y") ) {
        }
        else {
        }
    }
    
    private void processElement(Element element) {
        String name = element.getAttribute("name");
        if ( name != null) {
            this.name = name;
        }
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        if (type != null) {
            transferType = type;
        }
        else {
            throw new RuntimeException("Illegal transfer type: " + typeStr);
        }
    }
    
    private void processLegacyElement(Element element) {
        if ( element.getAttribute("name") != null ) {
            name= element.getAttribute("name");
        } else {
            name= "";
        }
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        if (type != null) {
            transferType = type;
        }
        else {
            throw new RuntimeException("Illegal transfer type: " + typeStr);
        }
    }
    
    public StreamMultiYDescriptor() {
    }
    
    public Units getUnits() {
        return units;
    }
    
    public void setUnits(Units units) {
        this.units = units;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public int getSizeBytes() {
        return transferType.getSizeBytes();
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
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("y");
        element.setAttribute("units", units.toString());
        element.setAttribute("type", transferType.toString());
        return element;
    }
    
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }
    
}

