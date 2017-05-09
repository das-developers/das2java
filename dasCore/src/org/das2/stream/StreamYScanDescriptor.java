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
package org.das2.stream;

import org.das2.datum.DatumVector;
import org.das2.datum.Units;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class StreamYScanDescriptor implements SkeletonDescriptor, Cloneable {

	private static final Logger logger = LoggerManager.getLogger("das2.d2s.yscan");
	private static final String g_sCkAry[] = {
		"name","type","nitems","yTags","yTagInterval","yTagMin","yTagMax","yUnits","zUnits"
	};
	
    private Units yUnits = Units.dimensionless;
    private Units zUnits = Units.dimensionless;
    private double[] yTags;
    private int nitems;
    private String name = "";
    private DataTransferType transferType = DataTransferType.SUN_REAL4;
	 Map properties= new HashMap();

    public StreamYScanDescriptor( Element element ) throws StreamException {
        if ( element.getTagName().equals("yscan") ) {
            processElement(element);
        }
        else {
            processLegacyElement(element);
        }
    }
   
	// make y tags using interval specification.  This version can walk down 1 of there
	// paths: Calc tags from 0, calc tags form a minimum value, calc tags from a 
	// maximum value
	private double[] makeYtagsUsingInterval(Element element, int nItems, double rInterval) 
		throws StreamException
	{
		String sMin = element.getAttribute("yTagMin");
		String sMax = element.getAttribute("yTagMax");
		int i;
		double[] lYtags = new double[nItems];
		if( sMin.isEmpty() && sMax.isEmpty()){
			for(i = 0; i<nItems;i++) lYtags[i] = rInterval*i;
		}
		else{
			if(sMax.isEmpty()){
				double rMin = Double.parseDouble(sMin);
				for(i = 0; i<nItems;i++) lYtags[i] = rInterval*i + rMin;
			}
			else{
				double rMax = Double.parseDouble(sMin);
				for(i = 0; i<nItems;i++) lYtags[(nItems - 1) - i] = rMax - rInterval*i;
			}
		}
		return lYtags;
	}
	 
	private void processElement(Element element) throws StreamException{

		boolean doCheck = false;
		if(doCheck){
		 //name, units, nitems, yUnits, yTags, zUnits are required, though they can be null
			//name, units, and type are required, though they can be null
			for(String s : g_sCkAry){
				if(!element.hasAttribute(s)){
					throw new StreamException("Das2 Stream Format error: Required Attribute '" + s
						+ "' missing in <" + element.getTagName() + "> plane.");
				}
			}
		}

		nitems = Integer.parseInt(element.getAttribute("nitems"));
		if(nitems < 1){
			throw new StreamException("yscan 'nitems' value is less than 1");
		}
		String yTagsText = element.getAttribute("yTags");
		if(yTagsText.length() > 0){ // 
			yTags = new double[nitems];
			String[] tokens = yTagsText.split("\\s*,\\s*");
			for(int i = 0; i < nitems; i++){
				yTags[i] = Double.parseDouble(tokens[i]);
			}
		}
		else{
			// See if yTagInterval is in use instead
			String sInterval = element.getAttribute("yTagInterval");
			if(sInterval.length() > 0){
				double rInterval = Double.parseDouble(sInterval);
				yTags = makeYtagsUsingInterval(element, nitems, rInterval);
			}
			else{
				// Just default to index entries
				yTags = new double[nitems];
				for(int i = 0; i < nitems; i++) 	yTags[i] = i;
			}
		}
		  
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        if (type != null) {
            transferType = type;
        }
        else {
            throw new RuntimeException("Illegal transfer type: " + typeStr);
        }
		  
	String yUnitsString = element.getAttribute("yUnits");
	if (yUnitsString != null) {
            yUnits = Units.lookupUnits(yUnitsString);
        }
	
        String zUnitsString = element.getAttribute("zUnits");
        if (zUnitsString != null) {
            zUnits = Units.lookupUnits(zUnitsString);
        }
		  
        String lname = element.getAttribute("name");
        if ( lname != null ) {
            this.name = lname;
        }
		  
		NodeList nl = element.getElementsByTagName("properties");
		logger.log(Level.FINER, "element y has {0} properties", nl.getLength());
		for (int i = 0; i < nl.getLength(); i++) {
			Element el = (Element) nl.item(i);
			Map<String,Object> m = StreamTool.processPropertiesElement(el);
				
			// make sure we don't conflict with the 6 reserved properites above
			for(String sPropName: m.keySet()){
				for(String sReserved: g_sCkAry){
					if(sPropName.equals(sReserved))
						throw new StreamException("Can't use reserved property name '"+
							sReserved + "inside a <yscan> plane properties element.");
				}
			}
			properties.putAll(m);
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
    
    public void setZUnits(Units units) {
        zUnits = units;
    }
    
    public void setYCoordinates(DatumVector yCoords) {
        yUnits = yCoords.getUnits();
        yTags = yCoords.toDoubleArray(yUnits);
        nitems = yTags.length;
    }

    public void setDataTransferType(DataTransferType transferType) {
        this.transferType = transferType;
    }
    
    public DataTransferType getDataTransferType() {
        return transferType;
    }
    
	@Override
    public int getSizeBytes() {
        return nitems * transferType.getSizeBytes();
    }
    
    private double[] values;
    
	@Override
    public DatumVector read(ByteBuffer input) {
        if (values == null) {
            values = new double[nitems];
        }
        for (int i = 0; i < nitems; i++) {
            values[i] = transferType.read(input);
        }
        return DatumVector.newDatumVector(values, zUnits);
    }
    
	@Override
    public void write(DatumVector input, ByteBuffer output) {
        values = input.toDoubleArray(values, zUnits);
        for (int i = 0; i < nitems; i++) {
            transferType.write(values[i], output);
        }
    }
    
	@Override
    public Element getDOMElement(Document document) {
        Element element = document.createElement("yscan");
        element.setAttribute("nitems", String.valueOf(nitems));
        element.setAttribute("yTags", toString(yTags));
        element.setAttribute("yUnits", yUnits.toString());
        element.setAttribute("zUnits", zUnits.toString());
        element.setAttribute("type", transferType.toString());
        if ( name!=null && !name.equals("") ) element.setAttribute( "name", name );        
        return element;
    }
    
    private static String toString(double[] d) {
        StringBuilder buffer = new StringBuilder();
        buffer.append(d[0]);
        for (int i = 1; i < d.length; i++) {
            buffer.append(", ").append(d[i]);
        }
        return buffer.toString();
    }
    
    @Override
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
    
    @Override
    public String toString() {
        return "<yScan nitems="+nitems+">";
    }
    
	@Override
    public Object getProperty(String name) {
        return properties.get(name);
    }

	@Override
    public Map getProperties() {
        return new HashMap(properties);
    }

}

