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

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.UnitsConverter;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.client.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;

public class StreamMultiYDescriptor extends XMultiYDataSetDescriptor implements SkeletonDescriptor {
    
    int nitems;
    
    ArrayList records;
    StreamDescriptor stream;
    String name;
    
    public StreamMultiYDescriptor(Node node, StreamDescriptor stream ) {
        super(null);
        
        this.stream= stream;
        if ( node.getNodeName()!="YMulti" ) {
            throw new IllegalArgumentException("xml tree root node is not the right type");
        }
        NamedNodeMap attr= node.getAttributes();
        Node attrNode;
        if ( ( attrNode=attr.getNamedItem("nitems") ) != null ) {
            nitems= Integer.parseInt(attrNode.getNodeValue());
        } 
        if ( ( attrNode=attr.getNamedItem("name") ) != null ) {
            name= attrNode.getNodeValue();
        } else {
            name= "";
        }
    }
    
    public int getSizeBytes() {
        return nitems*8;
    }
    
    public void read(byte[] buf, int offset, int length) {
        double[] dbuf= new double[nitems];
        java.nio.ByteBuffer nbuf= ByteBuffer.wrap(buf);
        DoubleBuffer doubleBuffer= nbuf.asDoubleBuffer();
        doubleBuffer.position(offset/8);
        doubleBuffer.get(dbuf);
        records.add(new XMultiY(-1e31,dbuf));
    }
    
    public int getNumRecords() {
        return records.size();
    }
    
    public DataSet asDataSet(Datum[] timeTags) {
        XMultiYDataSet ds= new XMultiYDataSet(this,stream.getStartTime(),
                                                stream.getEndTime());
        ds.data= (XMultiY[])records.toArray(new XMultiY[records.size()]);        
        for ( int i=0; i<ds.data.length; i++ ) {
            ds.data[i].x= timeTags[i].doubleValue(ds.getXUnits());
        }
        ds.setName(name);
        return ds;
    }
    
    public String getName() {
        return this.name;
    }
    
}

