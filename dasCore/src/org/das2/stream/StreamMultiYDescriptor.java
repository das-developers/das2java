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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.LoggerManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StreamMultiYDescriptor implements SkeletonDescriptor, Cloneable {

    private static Logger logger = LoggerManager.getLogger("das2.d2s.multiy");
    
	//private static final String g_sCkAry[] = {"name","type","units"};
	private static final String g_sCkAry[] = {"type"}; //TODO: "name" needs to be required.  JBF turned this check off before a release to fix his hudson tests.
    
	 private String name = "";
    private Units units = Units.dimensionless;
    private DataTransferType transferType = DataTransferType.SUN_REAL4;

    public StreamMultiYDescriptor(Element element) throws StreamException {
        if (element.getTagName().equals("y")) {
            processElement(element);
        } else {
            processLegacyElement(element);
        }
    }

    private void processElement(Element element) throws StreamException 
	 {	 
        logger.log(Level.FINE, "processElement {0} name={1}", new Object[]{element, element.getAttribute("name")});
        
		 //name, units, and type are required, though they can be null
		 for(String s: g_sCkAry){
			 if(! element.hasAttribute(s) )
			 throw new StreamException("Das2 Stream Format error: Required Attribute '"+s+
				                        "' missing in <" + element.getTagName()+"> plane.");
		 }
         if ( !element.hasAttribute("units") ) {
             logger.warning("required attribute units is missing, using dimensionless.");
         }
		 
		 //element.getAttribute returns empty string if attr is not specified
		 //so safe to just use it directly
        name = element.getAttribute("name");
        
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        String unitsString = element.getAttribute("units");
        units = Units.lookupUnits(unitsString);
        
        logger.log(Level.FINER, "element y has {0} attributes", element.getAttributes().getLength());
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node n = attrs.item(i);
            properties.put(n.getNodeName(), n.getNodeValue());
        }

        NodeList nl= element.getChildNodes(); // quick and dirty schema checking.  Assert only node under can be "properties"
        List<String> kids= new ArrayList<>();
            
        for ( int i=0; i<nl.getLength(); i++ ) {
            if ( nl.item(i) instanceof Element ) {
                kids.add( ((Element)nl.item(i)).getNodeName() );
            }
        }
        kids.remove("properties");
        for ( String kid: kids ) {
            logger.log(Level.WARNING, "found invalid node under y: {0}", kid);            
        }

        nl = element.getElementsByTagName("properties");
        logger.log(Level.FINER, "element y has {0} properties", nl.getLength());
        for (int i = 0; i < nl.getLength(); i++) {
            Element el = (Element) nl.item(i);
				Map<String,Object> m = StreamTool.processPropertiesElement(el);
				
				// make sure we don't conflict with the 3 reserved properites, 
				// name, type and units
				for(String sPropName: m.keySet()){
					if(sPropName.equals("name")||sPropName.equals("type")||
						sPropName.equals("units"))
						throw new StreamException("Can't use reserved property names 'name'"+
							"'type' or 'units' in side a y-plane properties element.");
				}
				properties.putAll(m);
        }

        if (type != null) {
            transferType = type;
        } else {
            throw new RuntimeException("Illegal transfer type: " + typeStr);
        }

    }

    private void processLegacyElement(Element element) {
        logger.log(Level.FINE, "processLegacyElement {0} name={1}", new Object[]{element, element.getAttribute("name")});
        if (element.getAttribute("name") != null) {
            name = element.getAttribute("name");
        } else {
            name = "";
        }
        String typeStr = element.getAttribute("type");
        DataTransferType type = DataTransferType.getByName(typeStr);
        if (type != null) {
            transferType = type;
        } else {
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
        if (!name.equals("")) {
            element.setAttribute("name", name);
        }
        return element;
    }

	 public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new RuntimeException(cnse);
        }
    }

    Map properties = new HashMap();

    public Object getProperty(String name) {
        return properties.get(name);
    }

    public Map getProperties() {
        return new HashMap(properties);
    }

}
