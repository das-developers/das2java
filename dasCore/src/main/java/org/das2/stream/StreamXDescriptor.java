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
package org.das2.stream;

import org.das2.datum.Datum;
import org.das2.datum.DatumVector;
import org.das2.datum.TimeLocationUnits;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class StreamXDescriptor implements SkeletonDescriptor, Cloneable {
    
    private Datum base;
    
    private Units baseUnits = Units.us2000;
    
    private Units units = null;
    
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
        if ( type instanceof DataTransferType.Time ) {
            units= ((DataTransferType.Time)type).getUnits();
        } else {
            String unitsString = element.getAttribute("units");
            units = Units.lookupUnits(unitsString);
        }
           
        String baseString = element.getAttribute("base");
        if (baseString != null && !baseString.equals("")) {
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
    
    public void setBase(Datum base) {
        this.base = base;
    }
    
    public int getSizeBytes() {
        return transferType.getSizeBytes();
    }
    
    public Units getUnits() {
        return units;
    }
    
    /* Units must be set now!!! */
    public void setUnits(Units units) {
        this.units = units;
    }
    
    public void setDataTransferType(DataTransferType transferType) {
        this.transferType = transferType;
        if ( transferType instanceof DataTransferType.Time ) {
            if ( units==null ) throw new IllegalArgumentException("please set the units first!!!");
            ((DataTransferType.Time)transferType).units= units;
        }
    }
    
    public DataTransferType getDataTransferType() {
        return transferType;
    }
    
    
    public Datum readDatum(ByteBuffer input) {
        return Datum.create(transferType.read(input), units);
    }
    
    public DatumVector read(ByteBuffer input) {
        return DatumVector.newDatumVector(new double[]{transferType.read(input)}, units);
    }
    
    public void writeDatum(Datum datum, ByteBuffer output) {
        transferType.write(datum.doubleValue(units), output);
    }
    
    public void write(DatumVector input, ByteBuffer output) {
        transferType.write(input.doubleValue(0, units), output);
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("x");
        if (base != null) {
            element.setAttribute("base", base.toString());
        }
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

    Map properties= new HashMap();
    
    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Map getProperties() {
        return new HashMap(properties);
    }
    
}

