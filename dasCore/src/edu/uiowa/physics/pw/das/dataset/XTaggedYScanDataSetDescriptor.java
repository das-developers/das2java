/* File: XTaggedYScanDataSetDescriptor.java
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
import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.stream.MultiPlanarDataSet;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Map;

/**
 *
 * @author  jbf
 */
public class XTaggedYScanDataSetDescriptor extends DataSetDescriptor {
    
    public float z_fill = Float.NaN;
    
    public String y_parameter = "";
    public String y_unit = "";
    public Units yUnits;
    
    public String z_parameter = "";
    public String z_unit = "";
    public Units zUnits;
    
    public double[] y_coordinate;
    
    public int ny = -1;
    
    public int items = -1;
    
    private XTaggedYScanDataSetCache dataCache;
    
    protected XTaggedYScanDataSetDescriptor() {
        super(Units.us2000);
        yUnits= Units.dimensionless;
        zUnits= Units.dimensionless;
    }
    
    /** Creates a new instance of XTaggedYScanDataSetDescriptor */
    protected XTaggedYScanDataSetDescriptor(Map properties) {
        
        super(Units.us2000);
        if (properties!=null) setProperties(properties);
        yUnits= Units.dimensionless;
        zUnits= Units.dimensionless;
        
    }
    
    protected void setProperties( Map properties ) {
        super.setProperties(properties);
        if (properties.containsKey("y_parameter")) {
            y_parameter = (String)properties.get("y_parameter");
        }
        if (properties.containsKey("y_unit")) {
            y_unit = (String)properties.get("y_unit");
        }
        if (properties.containsKey("z_parameter")) {
            z_parameter = (String)properties.get("z_parameter");
        }
        if (properties.containsKey("z_unit")) {
            z_unit = (String)properties.get("z_unit");
        }
        if (properties.containsKey("z_fill")) {
            z_fill = ((Float)properties.get("z_fill")).floatValue();
        }
        if (properties.containsKey("y_coordinate")) {
            y_coordinate = (double[])properties.get("y_coordinate");
        }
        if (properties.containsKey("ny")) {
            ny = ((Integer)properties.get("ny")).intValue();
        }
        if (properties.containsKey("items")) {
            items =  ((Integer)properties.get("items")).intValue();
        }
    }
    
    public DataSet getDataSet(Datum start, Datum end, Datum resolution, DasProgressMonitor monitor) throws DasException {
        Datum res= resolution;
        Datum sbResolution= resolution ;
        
        sbResolution= sbResolution.gt( this.getXSampleWidth() ) ? sbResolution : this.getXSampleWidth();
        
        InputStream in;
        if (isServerSideReduction()) {
            in= standardDataStreamSource.getReducedInputStream( this, start, end, resolution );
            res= ( resolution.gt( this.getXSampleWidth() ) ? resolution : getXSampleWidth() );
        } else {
            in= standardDataStreamSource.getInputStream( this, start, end );
            res= getXSampleWidth();
        }
        in = new DasProgressMonitorInputStream(in, monitor);
        XTaggedYScanDataSet ds
            = (XTaggedYScanDataSet) getDataSet(in,start,end, resolution);
        ds.x_sample_width= res.doubleValue(Units.seconds);        
        ds.setXSampleWidth( res );
        
        return ds;
    }
    
    protected DataSet getDataSet(InputStream in0, Datum start, Datum end, Datum resolution) throws DasException {
        
        int elementCount, elementSize;
        
        XTaggedYScanDataSet ds = new XTaggedYScanDataSet(this,start, end );
        float[] data;
        ds.description = description;
        ds.x_parameter = x_parameter;
        ds.x_unit = x_unit;
        
        ds.x_sample_width= x_sample_width;
        UnitsConverter uc= UnitsConverter.getConverter( Units.seconds, ((LocationUnits)getXUnits()).getOffsetUnits() );
        ds.xSampleWidth= uc.convert(ds.x_sample_width);
        
        ds.y_parameter = y_parameter;
        ds.y_unit = y_unit;
        ds.y_coordinate = (double[])y_coordinate.clone();
        ds.z_parameter = z_parameter;
        ds.z_unit = z_unit;
        ds.setZFill(z_fill);
        
        PushbackInputStream in;
        // try {
        in= new PushbackInputStream(in0,50);
        //  } catch ( IOException ex ) {
        //     ex.printStackTrace();
        //  }
        
        boolean isMultiPlanarDataSetStream;
        try {
            isMultiPlanarDataSetStream= MultiPlanarDataSet.isMultiPlanarDataSetStream(in);
        } catch (DasException ex) {
            isMultiPlanarDataSetStream= false; 
        }
        
        if ( isMultiPlanarDataSetStream ) {
            MultiPlanarDataSet mpds= new MultiPlanarDataSet();
            try {
                try {
                    mpds.read(in, null );
                } catch ( ClassCastException ex ) {
                    System.out.println(ex);
                }
                ds= (XTaggedYScanDataSet)mpds.getPrimaryDataSet();
                ds.setDataSetDescriptor(this);
            } catch ( DasException ex ) {
                ex.printStackTrace();
            }
            
        } else {
            
            data = readFloats(in,start, end);
                        
            Datum timeBase= start.convertTo(ds.getXUnits());
            
            elementSize = y_coordinate.length + 1;
            elementCount = data.length / elementSize;
            ds.data = new XTaggedYScan[elementCount];
            double timeBaseValue= timeBase.doubleValue(ds.getXUnits());
            
            for (int i = 0; i < elementCount; i++) {
                //edu.uiowa.physics.pw.das.util.DasDie.print("\rCreating dataset object..."+((i+1)*100/elementCount)+"%");
                ds.data[i] = new XTaggedYScan();
                ds.data[i].x = timeBaseValue + uc.convert(data[i*elementSize]);
                ds.data[i].z = new float[elementSize - 1];
                System.arraycopy(data, i*elementSize + 1, ds.data[i].z, 0, elementSize - 1);
            }
            
        }
        return ds;
        
    }
    
    public Units getYUnits() {
        return this.yUnits;
    }
    
    public DasAxis getZAxis(DasRow row, DasColumn column) {
        return new DasAxis(Datum.create(0,getZUnits()), Datum.create(10,getZUnits()),row,column,DasAxis.VERTICAL,false);
    }
    
    public Renderer getRenderer(DasPlot plot) {
        DasAxis zAxis= getZAxis(plot.getRow(),plot.getColumn());
        DasColorBar colorBar= new DasColorBar(zAxis.getDataMinimum(),zAxis.getDataMaximum(),
        plot.getRow(), DasColorBar.getColorBarColumn(plot.getColumn()), zAxis.isLog() );
        Renderer result= new SpectrogramRenderer( plot, this, colorBar );
        return result;
    }
    
    public Units getZUnits() {
        return this.zUnits;
    }
    
    
}
