/* File: StreamDescriptor.java
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.dataset.*;
import org.apache.xml.serialize.*;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringBufferInputStream;
import java.io.StringWriter;

/** Represents the global properties of the stream, that are accessible to
 * datasets within.
 * @author jbf
 */
public class StreamDescriptor {
    
    NamedNodeMap attrNode;
    
    /** Creates a new instance of StreamProperties */
    public StreamDescriptor(Node docNode) {
        this.attrNode= docNode.getAttributes();
    }
    
    public String getAttribute(String attr) {
        String result="";
        if ( attrNode.getNamedItem(attr)!=null ) {
            result= attrNode.getNamedItem(attr).getNodeValue();
        }
        return result;
    }
    
    public Datum getStartTime() {
        try {
            return TimeUtil.create(getAttribute("startTime"));
        } catch ( java.text.ParseException ex ) {
            throw new IllegalStateException( "startTime is not a valid time" );
        }
    }
    
    public Datum getEndTime() {
        try {
            return TimeUtil.create(getAttribute("endTime"));
        } catch ( java.text.ParseException ex ) {
            throw new IllegalStateException( "endTime is not a valid time" );
        }
    }
    
    public boolean isCompressed() {
        return getAttribute("compression").equals("gzip");
    }
    
    private static void validate( Document document ) throws java.text.ParseException, DasStreamFormatException {
        NamedNodeMap attr1= document.getAttributes();
        Node s;
        s= attr1.getNamedItem("startTime");
        if ( s==null ) {
            throw new DasStreamFormatException( "startTime not provided" );
        } else {
            TimeUtil.create(s.getNodeValue());
        }
        s= attr1.getNamedItem("endTime");
        if ( s==null ) {
            throw new DasStreamFormatException( "startTime not provided" );
        } else {
            TimeUtil.create(s.getNodeValue());
        }
    }
    
    public static Document parseHeader(String header) throws DasIOException, DasStreamFormatException {
        DocumentBuilder builder;
        try {
            builder= DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch ( ParserConfigurationException ex ) {
            throw new IllegalStateException(ex.getMessage());
        }
        Document document=null;
        try {
            document= builder.parse(new StringBufferInputStream(header));
        } catch ( SAXException ex ) {
            System.out.println(ex);
            throw new DasIOException(ex.getMessage());
        } catch ( IOException ex) {
            throw new DasIOException(ex.getMessage());
        }
        try {
            validate(document);
        } catch ( java.text.ParseException e ) {
            throw new DasStreamFormatException( e.getMessage() );
        } 
        return document;
    }
    
    public static String createHeader(Document document) throws DasIOException {
        StringWriter writer= new StringWriter();
        OutputFormat format= new OutputFormat();
        format.setOmitXMLDeclaration(true);
        format.setEncoding("UTF-8");
        XMLSerializer serializer= new XMLSerializer(writer, format);
        try {
            serializer.serialize(document);
        } catch ( IOException ex) {
            throw new DasIOException(ex.getMessage());
        }
        String result= writer.toString();
        return result;
    }
    
}
