/* File: XTaggedYScanDataSet.java
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

import edu.uiowa.physics.pw.das.graph.*;
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.dataset.ConstantXTaggedYScanDataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.RebinDescriptor;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScan;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

/**
 *
 * @author  eew
 */
public class XTaggedYScanDataSet extends DataSet implements java.io.Serializable {
    
    //static final long serialVersionUID = 2277565010159376982L;
    
    public double x_sample_width;
    
    public double[] y_coordinate;
    
    private float z_fill = Float.NaN;
    
    public String description = "";
    
    public String x_parameter = "";
    
    public String x_unit = "";
    
    public String y_parameter = "";
    
    public String y_unit = "";
    
    public String z_parameter = "";
    
    public String z_unit = "";
    
    private Units yUnits= null;
    private Units zUnits= null;
    
    public XTaggedYScan[] data;
    
    private Hashtable auxData;
    
    public double ymin, ymax;
    public boolean yIsLog;
    
    public double zmin, zmax;
    public boolean zIsLog;
    
    private DasAxis xAxis = null;
    private DasAxis yAxis = null;
    
    public double xSampleWidth;
    
    /** Holds value of property nnRebin. */
    private boolean nnRebin;
    
    public XTaggedYScanDataSet(XTaggedYScanDataSetDescriptor dataSetDescriptor ) {
        super(dataSetDescriptor);
        //if (!(dataSetDescriptor instanceof XTaggedYScanDataSetDescriptor))
        //    throw new IllegalArgumentException("dataSetDescriptor is not a XTaggedYScanDataSetDescriptor");
        if (dataSetDescriptor!=null) {
            yUnits= dataSetDescriptor.getYUnits();
            zUnits= dataSetDescriptor.getZUnits();
        } else {
            yUnits= Units.dimensionless;
            zUnits= Units.dimensionless;
        }
        auxData= new Hashtable();
    }
    
    public XTaggedYScanDataSet(XTaggedYScanDataSetDescriptor dataSetDescriptor, DasDate start, DasDate end) {
        super(dataSetDescriptor,start,end);
        //if (!(dataSetDescriptor instanceof XTaggedYScanDataSetDescriptor))
        //    throw new IllegalArgumentException("dataSetDescriptor is not a XTaggedYScanDataSetDescriptor");
        if (dataSetDescriptor!=null) {
            yUnits= dataSetDescriptor.getYUnits();
            zUnits= dataSetDescriptor.getZUnits();
        } else {
            yUnits= Units.dimensionless;
            zUnits= Units.dimensionless;
        }
        auxData= new Hashtable();
    }
    
    public static XTaggedYScanDataSet create( XTaggedYScanDataSetDescriptor dataSetDescriptor,
    XTaggedYScan[] data ) {
        double xMin= Double.MAX_VALUE;
        double xMax= Double.MIN_VALUE;
        for (int i=0; i<data.length; i++) {
            xMin= ( data[i].x<xMin ? data[i].x : xMin );
            xMax= ( data[i].x>xMax ? data[i].x : xMax );
        }
        XTaggedYScanDataSet result=
        new XTaggedYScanDataSet( dataSetDescriptor );
        if ( dataSetDescriptor.getXUnits() instanceof TimeLocationUnits ) {
            result.setStartTime( DasDate.create( new TimeDatum( xMin, dataSetDescriptor.getXUnits() ) ));
            result.setEndTime( DasDate.create( new TimeDatum( xMax, dataSetDescriptor.getXUnits() ) ));
        }
        result.data= data;
        result.y_coordinate= dataSetDescriptor.y_coordinate;
        result.yUnits= dataSetDescriptor.getYUnits();
        result.setXUnits(dataSetDescriptor.getXUnits());
        result.zUnits= dataSetDescriptor.getZUnits();
        
        return result;
    }
    
    public String toString() {
        return getClass().getName() + "[Description = \"" + description +
        "\" Start time = " + startTime + " End time = " + endTime + "]";
    }
    
    public void setZFill(float zfill) {
        if ( Float.isNaN(zfill) ) throw new IllegalArgumentException("zFill cannot be NaN");
        this.z_fill= zfill;
        if (data == null) return;
        XTaggedYScan[] weights= getWeights();
        for (int i=0; i<data.length; i++) {
            for (int j=0; j<y_coordinate.length; j++) {
                if (data[i].z[j]==z_fill) weights[i].z[j]=0.f;
            }
        }
        setWeights(weights);
    }
    
    public float getZFill() {
        return this.z_fill;
    }
    
    public XTaggedYScanDataSet rebin(RebinDescriptor ddX, RebinDescriptor ddY) {
        
        edu.uiowa.physics.pw.das.util.DasDie.println(" rebin: " + x_sample_width + " to " + ddX.binWidth() );
        XTaggedYScanDataSet result= this.binAverage(ddX,ddY);
        result.setNnRebin(this.isNnRebin());
        result.fillInterpolateY(ddY.isLog);
        result.fillInterpolateX();
        
        return result;
    }
    
    public XTaggedYScan[] getWeights() {
        XTaggedYScan[] weights;
        if ( auxData.containsKey("weights") ) {
            weights= (XTaggedYScan[]) auxData.get("weights");
        } else {
            weights= new XTaggedYScan[data.length];
            
            for (int i=0; i<weights.length; i++) {
                float[] weights1= new float[y_coordinate.length];
                float zFill= this.getZFill();
                for (int j=0; j<y_coordinate.length; j++) {
                    weights1[j]= ( data[i].z[j]==zFill ) ? 0.f : 1.0f;
                }
                weights[i]= new XTaggedYScan(data[i].x,weights1);
            }
            auxData.put("weights", weights);
        }
        
        return weights;
    }
    
    public void setWeights(XTaggedYScan[] weights) {
        if ( weights.length!=data.length || (weights.length > 0 && weights[0].z.length!=data[0].z.length) ) {
            throw new IllegalArgumentException("weights array doesn't match dimensions of data");
        }
        auxData.put("weights", weights);
    }
    
    /** returns peaks for the dataset, and creates a set equal to the
     * data values if peaks is not defined.
     */    
    public XTaggedYScan[] getPeaks() {
        if ( auxData.containsKey("peaks") ) {
            return (XTaggedYScan[]) auxData.get("peaks");
        } else {
            XTaggedYScan[] peaks= new XTaggedYScan[data.length];
            
            for (int i=0; i<peaks.length; i++) {
                float[] peaks1= new float[y_coordinate.length];
                for (int j=0; j<y_coordinate.length; j++) {
                    peaks1[j]= data[i].z[j];
                }
                peaks[i]= new XTaggedYScan(data[i].x,peaks1);
            }
            auxData.put("peaks", peaks);
            return peaks;
        }
    }
    
    public void setPeaks(XTaggedYScan[] peaks) {
        if ( peaks.length!=data.length || ( peaks.length>0 && peaks[0].z.length!=data[0].z.length ) ) {
            throw new IllegalArgumentException("peaks array doesn't match dimensions of data");
        }
        auxData.put("peaks", peaks);
    }
    
    public XTaggedYScanDataSet binAverageX( RebinDescriptor ddX ) {
        int ny= y_coordinate.length;
        int nx= data.length;
        
        double[] xBinCenters= ddX.binCenters();
        
        float zFill= -1e31f;
        
        XTaggedYScan[] newData= null;
        
        XTaggedYScan[] peaks=null;
        XTaggedYScan[] newPeaks=null;
        
        XTaggedYScan[] weights=null;
        XTaggedYScan[] newWeights=null;
        
        newData= new XTaggedYScan[ddX.nBin];
        newWeights= new XTaggedYScan[ddX.nBin];
        weights= getWeights();
        
        for ( int k=0; k<ddX.nBin; k++ ) {
            float[] z= new float[ny];
            Arrays.fill(z,0.f);
            newData[k]= new XTaggedYScan( xBinCenters[k],z);
            float[] zw= new float[ny];
            Arrays.fill(zw,0.f);
            newWeights[k]= new XTaggedYScan(xBinCenters[k],zw);
        }
        
        if ( auxData.containsKey("peaks") ) {
            peaks= (XTaggedYScan[])auxData.get("peaks");
            newPeaks= new XTaggedYScan[ddX.nBin];
            for ( int k=0; k<ddX.nBin; k++ ) {
                float[] z= new float[ny];
                Arrays.fill(z,zFill);
                newPeaks[k]= new XTaggedYScan(xBinCenters[k],z);
            }
        }
        
        for ( int i=0; i<nx; i++ ) {
            int ibinx= ddX.whichBin(data[i].x,getXUnits());
            if (ibinx>=0 && ibinx<newData.length) {
                for (int j=0; j<ny; j++) {
                    newData[ibinx].z[j]+= data[i].z[j] * weights[i].z[j]; 
                    newWeights[ibinx].z[j]+= weights[i].z[j];
                    if ( peaks!=null ) {
                        newPeaks[ibinx].z[j]=
                        newPeaks[ibinx].z[j] > peaks[i].z[j] ? newPeaks[ibinx].z[j] : peaks[i].z[j];
                    }
                }
            }
        }
        
        for ( int k=0; k<ddX.nBin; k++ ) {
            for ( int j=0; j<ny; j++) {
                if ( newWeights[k].z[j]>0.f ) {
                    newData[k].z[j]= newData[k].z[j] / newWeights[k].z[j];
                }
            }
        }
        
        XTaggedYScanDataSet result=
        XTaggedYScanDataSet.create( (XTaggedYScanDataSetDescriptor)getDataSetDescriptor(), newData );
        result.setWeights(newWeights);
        if ( newPeaks!=null ) result.setPeaks(newPeaks);
        result.xSampleWidth= ddX.binWidth();
        
        return result;
    }
    
    public XTaggedYScanDataSet binAverage(RebinDescriptor ddX, RebinDescriptor ddY) {
        
        long timer= System.currentTimeMillis();
        
        XTaggedYScanDataSet result=
        new XTaggedYScanDataSet((XTaggedYScanDataSetDescriptor)this.getDataSetDescriptor(), this.startTime,this.endTime);
        
        result.x_sample_width= x_sample_width;
        result.xSampleWidth= xSampleWidth;
        result.z_fill= z_fill;
        
        result.zUnits= zUnits;
        result.yUnits= yUnits;
        
        double[] XbinCenters= ddX.binCenters();
        int nx= ddX.numberOfBins();
        int ny= ddY.numberOfBins();
        
        XTaggedYScan[] rebinData= new XTaggedYScan[nx];
        XTaggedYScan[] rebinWeights= new XTaggedYScan[nx];
        float[] zeros= new float[ny];
        for (int i=0; i<nx; i++) {
            rebinData[i]= new XTaggedYScan(XbinCenters[i],zeros);
            rebinWeights[i]= new XTaggedYScan(XbinCenters[i],zeros);
        }
        
        result.data= rebinData;
        
        double[] ycoordinate= new double[ddY.numberOfBins()];
        double[] ddYbinCenters= ddY.binCenters();
        for (int i=0; i<ycoordinate.length; i++) {
            ycoordinate[i]= ddYbinCenters[i];
        }
        result.y_coordinate= ycoordinate;
        
        XTaggedYScan[] weights=getWeights();
        
        int [] ibiny= new int[y_coordinate.length];
        for (int j=0; j<y_coordinate.length; j++)
            ibiny[j]= ddY.whichBin(y_coordinate[j],yUnits);
        
        for (int i=0; i<data.length; i++) {
            int ibinx= ddX.whichBin(data[i].x,getXUnits());
            if (ibinx>=0 && ibinx<nx) {
                for (int j=0; j<y_coordinate.length; j++) {
                    if (ibiny[j]>=0 &&ibiny[j]<ny) {
                        if ( weights!=null ) {
                            rebinData[ibinx].z[ibiny[j]]=
                            rebinData[ibinx].z[ibiny[j]] + data[i].z[j] * weights[i].z[j];
                            rebinWeights[ibinx].z[ibiny[j]]=
                            rebinWeights[ibinx].z[ibiny[j]] + weights[i].z[j];
                        } else { // this is dead code
                            rebinData[ibinx].z[ibiny[j]]=
                            rebinData[ibinx].z[ibiny[j]] + data[i].z[j] * 1.0f;
                            rebinWeights[ibinx].z[ibiny[j]]=
                            rebinWeights[ibinx].z[ibiny[j]] + 1.0f;
                        }
                    }
                }
            }
        }
        
        for (int i=0; i<nx; i++) {
            for (int j=0; j<ny; j++) {
                if (rebinWeights[i].z[j]>0.) {
                    rebinData[i].z[j]= rebinData[i].z[j] / rebinWeights[i].z[j];
                } else {
                    rebinData[i].z[j]= z_fill;
                }
            }
        }
        
        result.setWeights(rebinWeights);
        
        return result;
    }
    
    protected void fillInterpolateY(boolean isLogY) {
        
        boolean nearestNeighbor=isNnRebin();
        
        int ny= y_coordinate.length;
        int nx= data.length;
        float a1;
        float a2;
        int[] i1= new int[ny];
        int[] i2= new int[ny];
        
        double [] y_temp= new double[y_coordinate.length];
        if (isLogY) {
            for (int j=0; j<ny; j++) y_temp[j]= Math.log(y_coordinate[j]);
        } else {
            for (int j=0; j<ny; j++) y_temp[j]= y_coordinate[j];
        }
        
        XTaggedYScan[] weights= getWeights();
        
        for (int i=0; i<nx; i++) {
            int ii1= -1;
            int ii2= -1;
            for (int j=0; j<ny; j++) {
                if (weights[i].z[j]>0. && ii1==(j-1)) { // ho hum another valid point
                    i1[j]= -1;
                    i2[j]= -1;
                    ii1=j;
                } else if (weights[i].z[j]>0. && ii1==-1) { // first valid point
                    ii1=j;
                } else if (weights[i].z[j]>0. && ii1<(j-1)) { // bracketed a gap, interpolate
                    if ((ii1>-1)) {   // need restriction on Y gap size
                        i1[j]= -1;
                        i2[j]= -1;
                        for (int jj=j-1; jj>=ii1; jj--) {
                            ii2= j;
                            i1[jj]= ii1;
                            i2[jj]= ii2;
                        }
                        ii1= j;
                    }
                } else {
                    i1[j]=-1;
                    i2[j]=-1;
                }
            }
            
            
            for (int j=0; j<ny; j++) {
                if (i1[j]!=-1) {
                    a2= (float)((y_temp[j]-y_temp[i1[j]]) / (y_temp[i2[j]]-y_temp[i1[j]]));
                    if (nearestNeighbor) a2= (a2<0.5f)?0.f:1.f;
                    
                    a1= 1.f-a2;
                    
                    data[i].z[j]=    data[i].z[i1[j]]*a1 +    data[i].z[i2[j]]*a2;
                    weights[i].z[j]= weights[i].z[i1[j]]*a1 + weights[i].z[i2[j]]*a2; //approximate
                }
            }
        }
    }
    
    protected void fillInterpolateX() {
        
        boolean nearestNeighbor= isNnRebin();
        int nx= data.length;
        int ny= y_coordinate.length;
        float a1;
        float a2;
        int[] i1= new int[nx];
        int[] i2= new int[nx];
        
        double[] x_temp= new double[nx];
        for (int i=0; i<nx; i++) x_temp[i]= data[i].x;
        
        XTaggedYScan[] weights= getWeights();
        
        for (int j=0; j<ny; j++) {
            int ii1= -1;
            int ii2= -1;
            for (int i=0; i<nx; i++) {
                
                if (weights[i].z[j]>0. && ii1==(i-1)) { // ho hum another valid point
                    i1[i]= -1;
                    i2[i]= -1;
                    ii1=i;
                } else if (weights[i].z[j]>0. && ii1==-1) { // first valid point
                    ii1=i;
                } else if (weights[i].z[j]>0. && ii1<(i-1)) { // bracketed a gap, interpolate
                    if (ii1>-1) {
                        i1[i]= -1;
                        i2[i]= -1;
                        for (int ii=i-1; ii>ii1; ii--) {
                            ii2= i;
                            i1[ii]= ii1;
                            i2[ii]= ii2;
                        }
                        ii1= i;
                    }
                } else {
                    i1[i]=-1;
                    i2[i]=-1;
                }
            }
            
            for (int i=0; i<nx; i++) {
                
                if ((i1[i]!=-1) && (x_temp[i2[i]]-x_temp[i1[i]])<xSampleWidth*2) {
                    a2= (float)((x_temp[i]-x_temp[i1[i]]) / (x_temp[i2[i]]-x_temp[i1[i]]));
                    if (nearestNeighbor) a2= (a2<0.5f)?0.f:1.0f;
                    a1= 1.f-a2;
                    data[i].z[j]=    data[i1[i]].z[j]*a1 +    data[i2[i]].z[j]*a2;
                    weights[i].z[j]= weights[i1[i]].z[j]*a1 + weights[i2[i]].z[j]*a2; //approximate
                }
            }
        }
    }
    
    
    public XTaggedYScanDataSet append(XTaggedYScanDataSet data1) {
        
        if (getXUnits()!=data1.getXUnits()) throw new IllegalArgumentException("xValues have different units");
        if (yUnits!=data1.yUnits) throw new IllegalArgumentException("yValues have different units");
        if (zUnits!=data1.zUnits) throw new IllegalArgumentException("zValues have different units");
        
        if (this.y_coordinate.length!=data1.y_coordinate.length) {
            throw new IllegalArgumentException("incompatible data sets have differing y_coordinates");
        } else {
            for (int j=0; j<data1.y_coordinate.length; j++) {
                if (this.y_coordinate[j]!=data1.y_coordinate[j]) {
                    throw new IllegalArgumentException("incompatible data sets have differing y_coordinates");
                }
            }
        }
        
        XTaggedYScanDataSet result=
        new XTaggedYScanDataSet((XTaggedYScanDataSetDescriptor)this.getDataSetDescriptor(),this.getStartTime(),data1.getEndTime());
        
        int nd1= this.data.length;
        
        // throw out records of append dataset that are within the old dataset, so that the
        // data[*].x is monotonically increasing.
        
        if (this.data.length == 0) {
            return data1;
        }
        
        int appendStart=0;
        while ( appendStart < data1.data.length-1
            && data1.data[appendStart].x < this.data[nd1-1].x ) {
            appendStart++;
        }
        
        if ( appendStart==data1.data.length-1 ) {
            return this;
            
        } else {
            
            int nd2= data1.data.length - appendStart;
            
            XTaggedYScan[] newData= new XTaggedYScan[nd1+nd2];
            
            for (int i=0; i<nd1; i++) newData[i]= this.data[i];            
            for (int i=0; i<nd2; i++) newData[i+nd1]= data1.data[i+appendStart];
            result.data= newData;
            
            if (this.auxData!=null) {
                Object[] keys= this.auxData.keySet().toArray();
                for ( int ikey=0; ikey<keys.length; ikey++ ) {
                    Object key1= keys[ikey];
                    if (data1.auxData.containsKey(key1)) {
                        XTaggedYScan[] aux1= (XTaggedYScan[])this.auxData.get(key1);
                        XTaggedYScan[] aux2= (XTaggedYScan[])data1.auxData.get(key1);
                        XTaggedYScan[] newAux= new XTaggedYScan[nd1+nd2];
                        for (int i=0; i<nd1; i++) newAux[i]= aux1[i];
                        for (int i=0; i<nd2; i++) newAux[i+nd1]= aux2[i];
                        result.auxData.put(key1,newAux);
                    }
                }
            }
                        
            result.x_sample_width= Math.max( this.x_sample_width, data1.x_sample_width );
            result.xSampleWidth= Math.max( this.xSampleWidth, data1.xSampleWidth );
            result.y_coordinate= this.y_coordinate;
            
            return result;
        }
    }
    
    public static XTaggedYScanDataSet create(double[] x, double[] y, float[][] z) {
        return create(x, Units.dimensionless, y, Units.dimensionless, z, Units.dimensionless);
    }
    
    public static XTaggedYScanDataSet create(double[] x, Units xUnits,
    double[] y, Units yUnits,
    float[][] z, Units zUnits) {
        if (x.length!=z.length) throw new IllegalArgumentException("x.length!=z.length");
        if (z.length!=0 && y.length!=z[0].length) throw new IllegalArgumentException("y.length!=z[0].length");
        XTaggedYScanDataSet result= new XTaggedYScanDataSet(null);
        result.data= new XTaggedYScan[x.length];
        for (int i=0; i<x.length; i++) {
            result.data[i]= new XTaggedYScan(x[i],z[i]);
        }
        result.y_coordinate= (double[])y.clone();
        float zMin = -Float.MAX_VALUE;
        result.setZFill(zMin);
        result.setXUnits(xUnits);
        return result;
    }
    
    public DasAxis createXAxis( DasRow row, DasColumn column ) {
        double [] x;
        
        int nx= this.data.length;
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= this.data[i].x;
        }
        
        boolean isLog;
        Units units= getXUnits();
        if ( ( x[x.length-1] / x[0] ) > 1e3 && x[0] > 0. && ! ( units instanceof LocationUnits ) ) {
            isLog=true;
        } else {
            isLog=false;
        }
        
        if ( ! (getXUnits() instanceof TimeLocationUnits ) ) {
            return DasAxis.create(x,getXUnits(),row,column,DasAxis.HORIZONTAL,false);
        } else {
            return DasTimeAxis.create( x, getXUnits(),row, column, DasAxis.HORIZONTAL );
        }
    }
    
    public DasAxis createYAxis( DasRow row, DasColumn column ) {
        
        double [] y;
        y= this.y_coordinate;
        
        boolean isLog;
        Units units= getYUnits();
        if ( ( y[y.length-1] / y[0] ) > 1e3 && y[0] > 0.  && ! ( units instanceof LocationUnits ) ) {
            isLog=true;
        } else {
            isLog=false;
        }
        return DasAxis.create(y,yUnits,row,column,DasAxis.VERTICAL,isLog);
        
    }
    
    public DasAxis getXAxis( DasRow row, DasColumn column ) {
        if (xAxis==null)
            this.xAxis= createXAxis(row,column);
        return this.xAxis;
    }
    
    public void setXAxis(DasAxis axis) {
        this.xAxis= axis;
    }
    
    public DasAxis getYAxis( DasRow row, DasColumn column ) {
        if (yAxis==null)
            this.yAxis= createYAxis(row,column);
        return this.yAxis;
    }
    
    public void setYAxis(DasAxis axis) {
        this.yAxis= axis;
    }
    
    public Units getYUnits() {
        return yUnits;
    }
    
    public Units getZUnits() {
        return zUnits;
    }
    
    public void dumpToStream( OutputStream out ) {
        UnitsConverter uc= ((LocationUnits)getXUnits()).getOffsetUnits().getConverter(Units.seconds);
        
        PrintStream pout= new PrintStream(out);
        pout.println("# Start Time: "+getStartTime());
        pout.println("# End Time: "+getEndTime());
        pout.println("# xSampleWidth: "+uc.convert(xSampleWidth));
        pout.println("# X is first value, offset in seconds from Start Time.");
        pout.println("#");
        pout.println("# File created on: "+TimeDatum.now().toString()+" UT");
        String tab= "\011";
        pout.print("yValues:"+tab);
        for (int i=0; i<y_coordinate.length; i++)
            pout.print(""+y_coordinate[i]+tab);
        pout.println();
        
        TimeDatum timeBaseDatum= TimeDatum.create(getStartTime());
        double timeBase= timeBaseDatum.convertTo(getXUnits()).doubleValue();
        for (int j=0; j<data.length; j++) {
            pout.print(""+uc.convert(data[j].x-timeBase)+tab);
            for (int i=0; i<y_coordinate.length; i++) {
                pout.print(""+data[j].z[i]+tab);
            }
            pout.println();
        }
    }
    
    public void visualize() {
        DasCanvas canvas= new DasCanvas(640,480);
        DasRow row= DasRow.create(canvas);
        DasColumn col= DasColumn.create(canvas);
        DasAxis xAxis= createXAxis(row,col);
        DasAxis yAxis= createYAxis(row,col);
        DasColorBar colorBar= new DasColorBar(new Datum(1e-15),new Datum(1e-13),row,DasColorBar.getColorBarColumn(col),true);
        SpectrogramRenderer rend= new SpectrogramRenderer( new ConstantXTaggedYScanDataSetDescriptor(this), colorBar );
        DasPlot plot= new DasPlot(xAxis,yAxis,row,col);
        plot.addRenderer(rend);
        canvas.addCanvasComponent(plot);
        JFrame jFrame= new JFrame("Visualize()");
        JPanel panel= new JPanel(new BorderLayout());
        panel.add(canvas,BorderLayout.CENTER);
        jFrame.setContentPane(panel);
        jFrame.pack();
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
    
    public Datum getClosestZValue(Datum x, Datum y) {
        long timer0= System.currentTimeMillis();
        if (!x.getUnits().equals(getXUnits()) || !y.getUnits().equals(getYUnits())) {
            throw new IllegalArgumentException("x and y units must be the same as the units for this dataset");
        }
        XTaggedYScan key = new XTaggedYScan(x.getValue(), new float[0]);
        int xIndex = Arrays.binarySearch(data, key, new XTagComparator());
        if (xIndex < 0) {
            int gtIndex = -(xIndex + 1);
            if (gtIndex == 0) {
                xIndex = 0;
            }
            else if (gtIndex == data.length) {
                xIndex = data.length - 1;
            }
            else {
                xIndex = gtIndex;
            }
        }
        int yIndex = Arrays.binarySearch(y_coordinate, y.getValue());
        if (yIndex < 0) {
            int gtIndex = -(yIndex + 1);
            if (gtIndex == 0) {
                yIndex = 0;
            }
            else if (gtIndex == data.length) {
                yIndex = data.length - 1;
            }
            else {
                yIndex = gtIndex;
            }
        }
        Datum d=new Datum(data[xIndex].z[yIndex], zUnits);
        
        return d;
    }
    
    public XTaggedYScanDataSet binAverageX( Datum periodDatum ) {
        // mimic behavior of server-side tav
        double start= data[0].x;
        Units offsetUnits= ((LocationUnits)getXUnits()).getOffsetUnits();
        UnitsConverter uc= periodDatum.getUnits().getConverter(offsetUnits);
        double period= periodDatum.convertTo(offsetUnits).doubleValue();
        LinkedList dataList= new LinkedList();
        LinkedList weightsList= new LinkedList();
        int idx= 0;
        
        int theBin= -1;
        
        XTaggedYScan num= new XTaggedYScan(0,new float[data[0].z.length]);
        XTaggedYScan sum= new XTaggedYScan(0,new float[data[0].z.length]);
        
        XTaggedYScan[] weights= getWeights();
        
        while (idx<data.length) {
            
            XTaggedYScan buf= data[idx];
            XTaggedYScan weightsBuf= weights[idx];
            int bin= (int) ( ( buf.x - start ) / period );
            if ( bin!=theBin) {
                
                if (num.x!=0) {
                    for  ( int j=0; j<sum.z.length; j++) {
                        sum.z[j]= sum.z[j] / num.z[j];
                    }
                    sum.x= start + ((double)theBin + 0.5) * period;
                    dataList.add(sum);
                    weightsList.add(num);
                }
                
                theBin= bin;
                
                num= new XTaggedYScan(0,new float[data[0].z.length]);
                sum= new XTaggedYScan(0,new float[data[0].z.length]);
                
            }
            
            if ( bin >=0 ) {
                num.x++;
                for ( int j=0; j<sum.z.length; j++ ) {
                    if ( buf.z[j] != getZFill() ) {
                        sum.z[j]+= buf.z[j] * weightsBuf.z[j];
                        num.z[j]+= weightsBuf.z[j];
                    }
                }
            }
            idx++;
        }
        
        if (num.x!=0) {
            for  ( int j=0; j<sum.z.length; j++) {
                sum.z[j]= sum.z[j] / num.z[j];
            }
            sum.x= start + ((double)theBin + 0.5) * period;
        }
        
        XTaggedYScanDataSet ds = new XTaggedYScanDataSet( (XTaggedYScanDataSetDescriptor)this.getDataSetDescriptor(), startTime, endTime);
        ds.xSampleWidth = (period * 2.0 < xSampleWidth ? xSampleWidth : period * 2.0);
        ds.x_sample_width = ((LocationUnits)getXUnits()).getOffsetUnits().getConverter(Units.seconds).convert(ds.xSampleWidth);
        ds.data = new XTaggedYScan[dataList.size()];
        dataList.toArray(ds.data);
        XTaggedYScan[] dsWeights = new XTaggedYScan[weightsList.size()];
        weightsList.toArray(dsWeights);
        ds.setWeights(dsWeights);
        ds.y_coordinate = y_coordinate;
        
        return ds;
        
    }
    
    /** Getter for property nnRebin.
     * @return Value of property nnRebin.
     */
    public boolean isNnRebin() {
        return this.nnRebin;
    }
    
    /** Setter for property nnRebin.
     * @param nnRebin New value of property nnRebin.
     */
    public void setNnRebin(boolean nnRebin) {
        this.nnRebin = nnRebin;
    }
    
    public int sizeBytes() {
        return data.length * ( this.y_coordinate.length * 4 + 8 );
    }
    
    private static class XTagComparator implements java.util.Comparator {
        
        /** Compares its two arguments for order.  Returns a negative integer,
         * zero, or a positive integer as the first argument is less than, equal
         * to, or greater than the second.<p>
         *
         * The implementor must ensure that <tt>sgn(compare(x, y)) ==
         * -sgn(compare(y, x))</tt> for all <tt>x</tt> and <tt>y</tt>.  (This
         * implies that <tt>compare(x, y)</tt> must throw an exception if and only
         * if <tt>compare(y, x)</tt> throws an exception.)<p>
         *
         * The implementor must also ensure that the relation is transitive:
         * <tt>((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0))</tt> implies
         * <tt>compare(x, z)&gt;0</tt>.<p>
         *
         * Finally, the implementer must ensure that <tt>compare(x, y)==0</tt>
         * implies that <tt>sgn(compare(x, z))==sgn(compare(y, z))</tt> for all
         * <tt>z</tt>.<p>
         *
         * It is generally the case, but <i>not</i> strictly required that
         * <tt>(compare(x, y)==0) == (x.equals(y))</tt>.  Generally speaking,
         * any comparator that violates this condition should clearly indicate
         * this fact.  The recommended language is "Note: this comparator
         * imposes orderings that are inconsistent with equals."
         *
         * @param o1 the first object to be compared.
         * @param o2 the second object to be compared.
         * @return a negative integer, zero, or a positive integer as the
         * 	       first argument is less than, equal to, or greater than the
         * 	       second.
         * @throws java.lang.ClassCastException if the arguments' types prevent them from
         * 	       being compared by this Comparator.
         */
        public int compare(Object o1, Object o2) {
            XTaggedYScan a = (XTaggedYScan)o1;
            XTaggedYScan b = (XTaggedYScan)o2;
            return (a.x < b.x ? -1 : (a.x > b.x ? 1 : 0));
        }
        
    }
    
}
