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
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class StreamXDescriptor implements SkeletonDescriptor, Cloneable {
    
    private Datum base;
    
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
        transferType = type;
        if ( type instanceof DataTransferType.Time ) {
            String unitsString = element.getAttribute("units");
            try {
                units = Units.lookupTimeUnits(unitsString);
                ((DataTransferType.Time)type).resetUnits(units);
            } catch (ParseException ex) {
                throw new IllegalArgumentException(ex);
            }
            //units= ((DataTransferType.Time)type).getUnits();
            if ( units==null ) throw new NullPointerException("units set to null");
        } else {
            String unitsString = element.getAttribute("units");
            units = Units.lookupUnits(unitsString);
            if ( units==null ) throw new NullPointerException("units set to null");
        }
           
        String baseString = element.getAttribute("base");
        if (baseString != null && !baseString.equals("")) {
            base = TimeUtil.createValid(baseString);
        }
    }
    
    private void processLegacyElement( Element element ) {
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        transferType = type;
    }
    
    public Datum getBase() {
        return base;
    }
    
    public void setBase(Datum base) {
        this.base = base;
    }
    
    @Override
    public int getSizeBytes() {
        return transferType.getSizeBytes();
    }
    
    public Units getUnits() {
        return units;
    }
    
    /* Units must be set now!!! */
    public void setUnits(Units units) {
        this.units = units;
        if ( units==null ) throw new NullPointerException("units set to null");
    }
    
    public void setDataTransferType(DataTransferType transferType) {
        this.transferType = transferType;
        if ( transferType instanceof DataTransferType.Time ) {
            if ( units==null ) throw new IllegalArgumentException("please set the units first!!!");
            ((DataTransferType.Time)transferType).resetUnits( units );
        }
    }
    
    public DataTransferType getDataTransferType() {
        return transferType;
    }
    
    
    public Datum readDatum(ByteBuffer input) {
        int p= input.position();
        try {
            return Datum.create(transferType.read(input), units);
        } catch ( NullPointerException ex ) {
            System.err.println("** Strange null pointer exception that shows up in hudson test: http://sarahandjeremy.net:8080/hudson/job/autoplot-test140/");
            System.err.println("transferType: "+transferType);
            System.err.println("input: "+input);
            System.err.println("first four: "+(int)input.get(0)+" "+(int)input.get(1)+" "+(int)input.get(2)+" "+(int)input.get(3) );
            System.err.println("pointer pos: "+p);
            System.err.println("pointer four: "+(int)input.get(p+0)+" "+(int)input.get(p+1)+" "+(int)input.get(p+2)+" "+(int)input.get(p+3) );
            System.err.println("units: "+units);
            ex.printStackTrace();            
            throw ex;
        }
    }
    
    @Override
    public DatumVector read(ByteBuffer input) {
        return DatumVector.newDatumVector(new double[]{transferType.read(input)}, units);
    }
    
    public void writeDatum(Datum datum, ByteBuffer output) {
        transferType.write(datum.doubleValue(units), output);
    }
    
    @Override
    public void write(DatumVector input, ByteBuffer output) {
        transferType.write(input.doubleValue(0, units), output);
    }
    
    @Override
    public Element getDOMElement(Document document) {
        Element element = document.createElement("x");
        if (base != null) {
            element.setAttribute("base", base.toString());
        }
        element.setAttribute("units", units.toString());
        element.setAttribute("type", transferType.toString());
        return element;
    }
    
    @Override
    public Object clone() {
        try {
            return super.clone();
        }
        catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    Map properties= new HashMap();
    
    @Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

    @Override
    public Map getProperties() {
        return new HashMap(properties);
    }
    
}

