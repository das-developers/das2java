/* File: StreamXDescriptor.java
 * Copyright (C) 2002-2003 University of Iowa
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
import edu.uiowa.physics.pw.das.datum.LocationUnits;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.datum.UnitsConverter;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;

public class StreamXDescriptor implements SkeletonDescriptor {
    
    Datum baseTime;
    
    ArrayList records;
    
    double current;
    
    UnitsConverter uc;
    Units offsetUnits;
    
    String name="";
    
    StreamXDescriptor( Node node, StreamDescriptor streamDescriptor ) {
        records= new ArrayList();        
        this.baseTime= streamDescriptor.getStartTime();        
        Node attrNode;
        offsetUnits= null;
        if ( (attrNode=node.getAttributes().getNamedItem("units"))!=null ) {
            String unitsStr= attrNode.getNodeValue();
            if ( !unitsStr.equals("seconds") ) {
                throw new IllegalStateException("X units are not seconds!!!");
            } else {
                offsetUnits= Units.seconds;
            }
        }
        NamedNodeMap attr= node.getAttributes();
        if ( ( attrNode=attr.getNamedItem("name") ) != null ) {
            name= attrNode.getNodeValue();
        } else {
            name= "";
        }
        if ( baseTime != null ) {
            uc= UnitsConverter.getConverter(offsetUnits,((LocationUnits)baseTime.getUnits()).getOffsetUnits());
        } else {
            uc= null;
        }
    }
    
    public int getSizeBytes() {
        return 8;
    }
    
    public int getNumRecords() {
        return records.size();
    }
    
    public void read(byte[] buf, int offset, int length) {
        // offset should be zero for this case, since the X is special in that it's needed for the YScans
        java.nio.ByteBuffer nbuf= ByteBuffer.wrap(buf);        
        DoubleBuffer doubleBuffer=nbuf.asDoubleBuffer();
        doubleBuffer.position(offset/8);        
        current= doubleBuffer.get();
        records.add(baseTime.add(current,Units.seconds));
    }
    
    public DataSet asDataSet(Datum[] timeTags) {
        return null;
    }
    
    public Datum[] getValues() {
        return (Datum[])records.toArray(new Datum[records.size()]);
    }
    
    public String getName() {
        return name;
    }
    
}

