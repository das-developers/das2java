/* File: XMultiYDataSetDescriptor.java
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

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.DasException;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasColumn;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasRow;
import edu.uiowa.physics.pw.das.datum.Datum;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 *
 * @author  jbf
 */
public class XMultiYDataSetDescriptor extends DataSetDescriptor {
    
    public double y_fill = Double.NaN;
    
    public String y_parameter = "";
    public String y_unit = "";
    
    public int ny = -1;
    
    public int items = -1;
    
    private Units yUnits;
    
    /** Creates a new instance of XMultiYDataSetDescriptor */
    
    protected XMultiYDataSetDescriptor(Hashtable properties) {
        super( Units.us2000 );
        
        yUnits= Units.dimensionless;
        
        if ( properties != null ) setProperties(properties);
    }    
    
    public void setProperties(Hashtable properties) {
        super.setProperties(properties);
        
        if (properties.containsKey("description")) {
            description= (String)properties.get("description");
        }
        if (properties.containsKey("form")) {
            form = (String)properties.get("form");
        }
        if (properties.containsKey("reader")) {
            reader = (String)properties.get("reader");
        }
        if (properties.containsKey("x_parameter")) {
            x_parameter = (String)properties.get("x_parameter");
        }
        if (properties.containsKey("x_unit")) {
            x_unit= (String)properties.get("x_unit");
        }
        if (properties.containsKey("y_parameter")) {
            y_parameter = (String)properties.get("y_parameter");
        }
        if (properties.containsKey("y_unit")) {
            y_unit = (String)properties.get("y_unit");
        }
        if (properties.containsKey("x_sample_width")) {
            x_sample_width = ((Double)properties.get("x_sample_width")).doubleValue();
        }
        if (properties.containsKey("y_fill")) {
            y_fill = ((Double)properties.get("y_fill")).doubleValue();
        }
        if (properties.containsKey("ny")) {
            ny = ((Integer)properties.get("ny")).intValue();
        }
        if (properties.containsKey("items")) {
            items = ((Integer)properties.get("items")).intValue();
        }
    }
    
    public boolean isTCA() {
        return ny == -1;
    }
    
    protected DataSet getDataSet(InputStream in, Datum start, Datum end, Datum resolution) throws DasException {
        
        int elementCount, elementSize;
                
        if ( !isTCA() ) {
            XMultiYDataSet ds = new XMultiYDataSet( this, start, end );
            double[] data;
            ds.description = description;
            ds.x_parameter = x_parameter;
            ds.x_unit = x_unit;
            ds.x_sample_width = x_sample_width;
            UnitsConverter uc= Units.getConverter(Units.seconds,((LocationUnits)getXUnits()).getOffsetUnits());
            ds.xSampleWidth= uc.convert(x_sample_width);
            ds.y_parameter = y_parameter;
            ds.y_unit = y_unit;
            ds.y_fill = y_fill;
            ds.ny = ny;
            elementSize = ny + 1;
        
            Datum timeBase= start.convertTo(ds.getXUnits());
            
            data = readDoubles(in, start, end);
            elementCount = data.length / elementSize;
            ds.data = new XMultiY[elementCount];
            
            double timeBaseValue= timeBase.doubleValue(ds.getXUnits());
            
            for (int i = 0; i < elementCount; i++) {
                ds.data[i] = new XMultiY();
                ds.data[i].x = timeBaseValue + uc.convert(data[i*elementSize]);
                ds.data[i].y = new double[elementSize - 1];
                System.arraycopy(data, i*elementSize + 1, ds.data[i].y, 0, elementSize - 1);
            }
            
            return ds;
        } else {
            TCADataSet ds = new TCADataSet(this,start, end);
            double[] data;
            ds.description = description;
            ds.items = items;
            ds.label = new String[items];
            Arrays.fill(ds.label, "");
            String[] keys = getPropertyNames();
            Pattern l = Pattern.compile("label\\(\\d+\\)");
            for (int keyIndex = 0; keyIndex < keys.length; keyIndex++) {
                String k = keys[keyIndex];
                Matcher m = l.matcher(keys[keyIndex]);
                if (m.matches()) {
                    int index = Integer.parseInt(k.substring(6,k.length()-1).trim());
                    ds.label[index] = getProperty(k).toString();
                }
            }
            elementSize = items + 1;
            
            data = readDoubles(in, start, end);
            elementCount = data.length/elementSize;
            ds.data = new XMultiY[elementCount];
            for (int i = 0; i < elementCount; i++) {
                ds.data[i] = new XMultiY();
                ds.data[i].x = data[i*elementSize];
                ds.data[i].y = new double[elementSize - 1];
                System.arraycopy(data, i*elementSize + 1, ds.data[i].y, 0, elementSize - 1);
            }
            
            return ds;
        }
    }
    
    public Units getYUnits() {
        return yUnits;
    }
    
    public DasAxis getDefaultYAxis(DasRow row, DasColumn col) {
        return new DasAxis(Datum.create(0,getYUnits()), Datum.create(10,getYUnits()), row, col, DasAxis.VERTICAL);
    }
    
    public edu.uiowa.physics.pw.das.graph.Renderer getRenderer(edu.uiowa.physics.pw.das.graph.DasPlot plot) {
        return new edu.uiowa.physics.pw.das.graph.SymbolLineRenderer(plot,this);
    }
 
    public edu.uiowa.physics.pw.das.graph.DasPlot getPlot( DasRow row, DasColumn col ) {
        return new DasPlot(this.getDefaultXAxis(row,col),this.getDefaultYAxis(row,col),row,col);
    }
}
