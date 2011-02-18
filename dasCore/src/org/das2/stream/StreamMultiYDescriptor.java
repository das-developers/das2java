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
package org.das2.stream;

import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.virbo.dataset.SemanticOps;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class StreamMultiYDescriptor implements SkeletonDescriptor, Cloneable {
    
    private String name = "";
    private Units units = Units.dimensionless;
    private DataTransferType transferType = DataTransferType.SUN_REAL4;
    
    public StreamMultiYDescriptor(Element element) {
        if ( element.getTagName().equals("y") ) {
            processElement(element);
        }
        else {
            processLegacyElement(element);
        }
    }
    
    private void processElement(Element element) {
        String name = element.getAttribute("name");
        if ( name != null) {
            this.name = name;
        }
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
    	String unitsString = element.getAttribute("units");
	if (unitsString != null) {
            units = SemanticOps.lookupUnits(unitsString);
        }
        NamedNodeMap attrs= element.getAttributes();
        for ( int i=0; i<attrs.getLength(); i++ ) {
            Node n= attrs.item(i);
            properties.put( n.getNodeName(), n.getNodeValue() );
        }
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
    
    public DatumVector read(ByteBuffer input) {
        return DatumVector.newDatumVector(new double[]{transferType.read(input)}, units);
    }
    
    public void write(DatumVector input, ByteBuffer output) {
        transferType.write(input.doubleValue(0, units), output);
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("y");
        element.setAttribute("units", units.toString());
        element.setAttribute("type", transferType.toString());
        if ( !name.equals("") ) element.setAttribute( "name", name );
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

