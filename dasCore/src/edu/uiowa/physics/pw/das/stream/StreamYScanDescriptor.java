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

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.TimeDatum;
import edu.uiowa.physics.pw.das.datum.UnitsConverter;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScan;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSet;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetDescriptor;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class StreamYScanDescriptor extends XTaggedYScanDataSetDescriptor implements SkeletonDescriptor {
    
    int nitems=-999;
    double[] yCoordinate;
    TimeDatum startTime, endTime;
    
    ArrayList records;
    String name;
    
    public StreamYScanDescriptor( Node node, StreamDescriptor stream ) {
        super(null);
        if ( !node.getNodeName().equals("YScan") ) {
            throw new IllegalArgumentException("xml tree root node is not the right type. "+
            "Node type is: "+node.getNodeName());
        }
        NamedNodeMap attr= node.getAttributes();
        Node attrNode;
        this.startTime= (TimeDatum)stream.getStartTime();
        this.endTime= (TimeDatum)stream.getEndTime();
        
        if ( ( attrNode=attr.getNamedItem("nitems") ) != null ) {
            nitems= Integer.parseInt(attrNode.getNodeValue());
        }
        if ( ( attrNode=attr.getNamedItem("yCoordinate") ) != null ) {
            String yCoordinateString= attrNode.getNodeValue();
            try {
                yCoordinate= new double[nitems];            
                int parseIdx=0;
                for (int i=0; i<nitems-1; i++) {                    
                    int toIdx= yCoordinateString.indexOf(",",parseIdx)-1;
                    yCoordinate[i]= Double.parseDouble(yCoordinateString.substring(parseIdx,toIdx));
                    parseIdx= toIdx+2;
                }
                yCoordinate[nitems-1]= Double.parseDouble(yCoordinateString.substring(parseIdx));
                y_coordinate= yCoordinate;
            } catch ( NumberFormatException ex ) {
                throw new IllegalArgumentException("Error in das2stream at yCoordinate");
            }
        } 
        if ( ( attrNode=attr.getNamedItem("name") ) != null ) {
            name= attrNode.getNodeValue();
        } else {
            name="";
        }
        records= new ArrayList();
    }
    
    public int getSizeBytes() {
        return nitems*4;
    }
    
    public void read(byte[] buf, int offset, int length) {
        float[] fbuf= new float[nitems];
        java.nio.ByteBuffer nbuf= ByteBuffer.wrap(buf);  
        FloatBuffer floatBuffer=  nbuf.asFloatBuffer();
        floatBuffer.position(offset/4); // requires word-alignment
        floatBuffer.get(fbuf);
        records.add(new XTaggedYScan(-1e31,fbuf));
    }
    
    public int getNumRecords() {
        return records.size();
    }
    
    public DataSet asDataSet(Datum[] xValues) {
        if ( xValues.length!=records.size() ) {
            throw new IllegalArgumentException("Number of xValues doesn't match number of records");
        }        
        XTaggedYScanDataSet ds= XTaggedYScanDataSet.create(this,(XTaggedYScan[])records.toArray(new XTaggedYScan[records.size()]));
        ds.setStartTime(DasDate.create(startTime));
        ds.setEndTime(DasDate.create(endTime));
        ds.y_coordinate= yCoordinate;
        
        UnitsConverter uc=null;
        if ( xValues.length>0 ) uc= xValues[0].getUnits().getConverter(this.getXUnits());
        for ( int i=0; i<ds.data.length; i++ ) {
            ds.data[i].x= uc.convert(xValues[i].getValue());
        }
        ds.setName(name);
        return ds;
    }
    
    public String getName() {
        return this.name;
    }
    
}

