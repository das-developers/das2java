/* File: StreamYScanDescriptor.java
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

public class StreamYScanDescriptor implements SkeletonDescriptor, Cloneable {

    private Units yUnits = Units.dimensionless;
    private Units zUnits = Units.dimensionless;
    private double[] yTags;
    private int nitems;
    private String name;
    private DataTransferType transferType = DataTransferType.SUN_REAL4;

    public StreamYScanDescriptor( Element element ) {
        if ( element.getTagName().equals("yscan") ) {
            processElement(element);
        }
        else {
            processLegacyElement(element);
        }
    }
    
    private void processElement(Element element) {
        nitems = Integer.parseInt(element.getAttribute("nitems"));
        String yTagsText = element.getAttribute("yTags");
        if (yTagsText != null) {
            yTags = new double[nitems];
            int parseInt = 0;
            String[] tokens = yTagsText.split("\\s*,\\s*");
            for (int i = 0; i < nitems; i++) {
                yTags[i] = Double.parseDouble(tokens[i]);
            }
            System.out.println();
        }
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        if (type != null) {
            transferType = type;
        }
        else {
            throw new RuntimeException("Illegal transfer type: " + typeStr);
        }
        String name = element.getAttribute("name");
        if ( name != null ) {
            this.name = name;
        }
    }
    
    private void processLegacyElement(Element element) {
        try {
            if ( !element.getTagName().equals("YScan") ) {
                throw new IllegalArgumentException("xml tree root node is not the right type. "+
                "Node type is: "+element.getTagName());
            }
            nitems= Integer.parseInt(element.getAttribute("nitems"));
            if ( element.getAttribute("yCoordinate") != null ) {
                String yCoordinateString= element.getAttribute("yCoordinate");
                yTags= new double[nitems];            
                int parseIdx=0;
                for (int i=0; i<nitems-1; i++) {                    
                    int toIdx= yCoordinateString.indexOf(",",parseIdx)-1;
                    yTags[i]= Double.parseDouble(yCoordinateString.substring(parseIdx,toIdx));
                    parseIdx= toIdx+2;
                }
                yTags[nitems-1]= Double.parseDouble(yCoordinateString.substring(parseIdx));
            } 
            String typeStr = element.getAttribute("type");
            DataTransferType type = DataTransferType.getByName(typeStr);
            if (type != null) {
                transferType = type;
            }
            else {
                throw new RuntimeException("Illegal transfer type: " + typeStr);
            }
        } catch ( NumberFormatException ex ) {
            throw new IllegalArgumentException("Error in das2stream at yCoordinate");
        }
        if ( element.getAttribute("name") != null ) {
            name= element.getAttribute("name");
        } else {
            name="";
        }
    }
    
    public StreamYScanDescriptor() {
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double[] getYTags() {
        return (double[])yTags.clone();
    }
    
    public void setYCoordinates(double[] yCoordinates) {
        this.yTags = (double[])yCoordinates.clone();
        this.nitems = yCoordinates.length;
    }
    
    public int getNItems() {
        return nitems;
    }
    
    public Units getYUnits() {
        return yUnits;
    }
    
    public Units getZUnits() {
        return zUnits;
    }

    public void setDataTransferType(DataTransferType transferType) {
        this.transferType = transferType;
    }
    
    public DataTransferType getDataTransferType() {
        return transferType;
    }
    
    public int getSizeBytes() {
        return nitems * transferType.getSizeBytes();
    }
    
    public void read(ByteBuffer input, double[] output, int offset) {
        for (int i = 0; i < nitems; i++) {
            output[offset + i] = transferType.read(input);
        }
    }
    
    public void write(double[] input, int offset, ByteBuffer output) {
        for (int i = 0; i < nitems; i++) {
            transferType.write(input[offset + i], output);
        }
    }
    
    public Element getDOMElement(Document document) {
        Element element = document.createElement("yscan");
        element.setAttribute("nitems", String.valueOf(nitems));
        element.setAttribute("yTags", toString(yTags));
        element.setAttribute("yUnits", yUnits.toString());
        element.setAttribute("zUnits", zUnits.toString());
        element.setAttribute("type", transferType.toString());
        return element;
    }
    
    private static String toString(double[] d) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(d[0]);
        for (int i = 1; i < d.length; i++) {
            buffer.append(", ").append(d[i]);
        }
        return buffer.toString();
    }
    
    public Object clone() {
        try {
            StreamYScanDescriptor clone = (StreamYScanDescriptor)super.clone();
            clone.yTags = (double[])this.yTags.clone();
            return clone;
        }
        catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }
    
}

