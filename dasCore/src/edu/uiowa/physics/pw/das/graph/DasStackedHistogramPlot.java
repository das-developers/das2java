/* File: DasStackedHistogramPlot.java
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

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.components.HorizontalSpectrogramSlicer;
import edu.uiowa.physics.pw.das.components.VerticalSpectrogramSlicer;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.event.HorizontalSlicerMouseModule;
import edu.uiowa.physics.pw.das.event.VerticalSlicerMouseModule;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.TimeLocationUnits;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.client.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.stream.*;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author  jbf
 */
public class DasStackedHistogramPlot extends edu.uiowa.physics.pw.das.graph.DasPlot implements edu.uiowa.physics.pw.das.graph.DasZAxisPlot, DataSetUpdateListener {
    
    private ZAxisComponent zAxisComponent= null;
    private RebinDescriptor xBins= null;
    
    private DasCanvas parent;
    private PeaksIndicator peaksIndicator;
    
    public static class PeaksIndicator implements edu.uiowa.physics.pw.das.components.PropertyEditor.Enumeration {
        
        String id;
        
        PeaksIndicator(String id) {
            this.id= id;
        }
        
        public String toString() {
            return this.id;
        }
        
        public static final PeaksIndicator GrayPeaks= new PeaksIndicator("Gray Peaks");
        public static final PeaksIndicator NoPeaks= new PeaksIndicator("None");
        public static final PeaksIndicator MaxLines= new PeaksIndicator("Lines");
        
        public javax.swing.Icon getListIcon() {
            return null;
        }
        
    }
    
    public static DasStackedHistogramPlot create(DasCanvas parent, TableDataSet Data) {
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.90);
        
        double [] x;
        
        int nx= Data.getXLength();
        int ny= Data.getYLength(0);
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= Data.getXTagDouble(i, Data.getXUnits());
        }
        
        double [] z= new double[nx*ny];
        int iz= 0;
        for (int i=0; i<nx; i++) {
            for (int j=0; j<ny; j++) {
                z[iz++]= Data.getDouble(i, j, Data.getZUnits());
            }
        }
        
        DasAxis ZAxis= DasAxis.create(z,Data.getZUnits(),
        new AttachedRow(row,0.,0.6),new AttachedColumn(column,1.05,1.10),
        DasAxis.RIGHT,true);
        
        DasAxis XAxis = DasAxis.create(x,Data.getXUnits(),row,column,DasAxis.HORIZONTAL,false);
        
        DasStackedHistogramPlot plot = DasStackedHistogramPlot.create(parent, Data, XAxis, ZAxis, row, column);
        
        return plot;
    }
    
    
    public static DasStackedHistogramPlot create(DasCanvas parent, TableDataSet data,
    DasAxis xAxis, DasAxis zAxis,
    DasRow row, DasColumn column) {
        Datum[] datums= new Datum[data.getYLength(0)];
        Units units= data.getYUnits();
        for ( int i = 0; i < data.getYLength(0); i++ ) {
            datums[i]= data.getXTagDatum(i);
        }
        DataSetDescriptor dsd = new ConstantDataSetDescriptor(data);
        DasLabelAxis yAxis = new DasLabelAxis(datums,row,column,DasAxis.VERTICAL);
        return new DasStackedHistogramPlot( dsd, xAxis, yAxis, zAxis, row, column );
    }
    
    public static DasStackedHistogramPlot create( DataSetDescriptor dsd, DasAxis xAxis, DasAxis zAxis, DasRow row, DasColumn column) {
        if (dsd instanceof StreamDataSetDescriptor) {
            StreamDataSetDescriptor sdsd = (StreamDataSetDescriptor)dsd;
            if (sdsd.getProperty("form").equals("x_tagged_y_scan")) {
                PacketDescriptor pd = sdsd.getDefaultPacketDescriptor();
                StreamYScanDescriptor yscan = (StreamYScanDescriptor)pd.getYDescriptors().get(0);
                Datum[] datums= new Datum[yscan.getNItems()];
                Units units= yscan.getYUnits();
                double[] y_coordinate = yscan.getYCoordinates();
                for ( int i=0; i<y_coordinate.length; i++ ) {
                    datums[i]= Datum.create(y_coordinate[i],units);
                }
                return new DasStackedHistogramPlot( dsd, xAxis, new DasLabelAxis(datums,row,column,DasAxis.VERTICAL), zAxis, row, column );
            }
        }
        else {
            double[] y_coordinate = (double[])dsd.getProperty("y_coordinate");
            Datum[] datums= new Datum[y_coordinate.length];
            Units units= Units.dimensionless;
            for ( int i=0; i<y_coordinate.length; i++ ) {
                datums[i]= Datum.create(y_coordinate[i],units);
            }
            return new DasStackedHistogramPlot( dsd, xAxis, new DasLabelAxis(datums,row,column,DasAxis.VERTICAL), zAxis, row, column );
        }
        return null;
    }
    
    public DasStackedHistogramPlot( DataSetDescriptor dsd, DasAxis xAxis, DasLabelAxis yAxis, DasAxis zAxis, DasRow row, DasColumn column) {
        super(xAxis, yAxis, row, column);
        this.zAxisComponent = new ZAxisComponent(this,zAxis);
        this.setZAxis(zAxis);
        this.peaksIndicator= PeaksIndicator.MaxLines;
        
        VerticalSpectrogramSlicer vSlicer
        = VerticalSpectrogramSlicer.createPopupSlicer(this, 640, 480);
        VerticalSlicerMouseModule vsl = VerticalSlicerMouseModule.create(this);
        vsl.addDataPointSelectionListener(vSlicer);
        mouseAdapter.addMouseModule(vsl);
        
        HorizontalSpectrogramSlicer hSlicer
        = HorizontalSpectrogramSlicer.createPopupSlicer(this, 640, 480);
        HorizontalSlicerMouseModule hsl = HorizontalSlicerMouseModule.create(this);
        hsl.addDataPointSelectionListener(hSlicer);
        mouseAdapter.addMouseModule(hsl);
        
        setDataSetDescriptor( dsd );
    }
        
    public static DasStackedHistogramPlot create( DasCanvas parent, DataSetDescriptor dsd, Datum startTime, Datum endTime ) throws edu.uiowa.physics.pw.das.DasException {
        TableDataSet ds= (TableDataSet)dsd.getDataSet(startTime,endTime, null, null);
        DasStackedHistogramPlot result= DasStackedHistogramPlot.create(parent,ds);
        result.setDataSetDescriptor(dsd);        
        return result;
    }   
    
    public void setZAxis(DasAxis zAxis) {
        if (zAxis.isHorizontal())
            throw new IllegalArgumentException("ZAxis is not VERTICAL");
        
        DasAxis oldZAxis= zAxisComponent.getAxis();
        
        if (oldZAxis != null) {
            if (parent != null) parent.remove(oldZAxis);
            oldZAxis.removePropertyChangeListener("dataMinimum", rebinListener);
            oldZAxis.removePropertyChangeListener("dataMaximum", rebinListener);
            oldZAxis.removePropertyChangeListener("log", rebinListener);
        }
        
        if (parent!=null) parent.addCanvasComponent(zAxis);
        zAxis.addPropertyChangeListener("dataMinimum", rebinListener);
        zAxis.addPropertyChangeListener("dataMaximum", rebinListener);
        zAxis.addPropertyChangeListener("log", rebinListener);
        
        getYAxis().addPropertyChangeListener("dataMinimum",zAxisComponent);
        getYAxis().addPropertyChangeListener("dataMaximum",zAxisComponent);
        getYAxis().addPropertyChangeListener("log",zAxisComponent);
    }
    
    public void setYAxis(DasAxis YAxis) {
        if (YAxis instanceof DasLabelAxis)
            super.setYAxis(YAxis);
        else
            edu.uiowa.physics.pw.das.util.DasDie.die("You can't call setYAxis for stackedHistogramPlot");
    }
    
    protected void updatePlotImage() {
        
        Component parent= getParent();
        Cursor cursor0= parent.getCursor();
        parent.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        
        int w= getColumn().getWidth();
        int h= getRow().getHeight();
        
        plotImage = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = (Graphics2D)plotImage.getGraphics();
        g.translate(-getColumn().getDMinimum(),-getRow().getDMinimum());
        
        Dimension d;
        
        double iMin= getColumn().getDMinimum();
        double jMin= getRow().getDMinimum();
        
        RenderingHints hints0= g.getRenderingHints();
        RenderingHints hints=
        new RenderingHints( RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_OFF );
        g.setRenderingHints(hints);
        
        DasAxis xaxis= getXAxis();
        RebinDescriptor xbins= new RebinDescriptor(xaxis.getDataMinimum(), xaxis.getDataMaximum(), (int)(Math.abs(getColumn().getWidth())/1)+1, (getXAxis().isLog()));
        
        int xDMax= getColumn().getDMaximum();
        int xDMin= getColumn().getDMinimum();
        
        //boolean drawnZAxis=false;
        //DasRow drawZAxisRow=null;
        //DasAxis drawThisZAxis=null;
        
        TableDataSet xtysData= (TableDataSet)Data;
        
        if ( Data==null ) {
            edu.uiowa.physics.pw.das.util.DasDie.println("null data set");
            return;
        }
        
        //xtysData.getPeaks();  // create peaks if it doesn't have them already
        DataSetRebinner rebinner = new Rebinner();
        TableDataSet data= (TableDataSet)rebinner.rebin(xtysData, xbins, null);
        TableDataSet peaks= (TableDataSet)data.getPlanarView("peaks");
        TableDataSet weights= (TableDataSet)data.getPlanarView("weights");
        double zzFill= data.getZUnits().getFillDouble();
        
        Rectangle2D.Double r= new Rectangle2D.Double();
        Line2D.Double l= new Line2D.Double();
        Rectangle2D.Double rmax= new Rectangle2D.Double();
        
        for (int j = 0; j < data.getYLength(0); j++) {
            DasLabelAxis yAxis= (DasLabelAxis)getYAxis();
            
            int yBase= yAxis.getItemMax(data.getYTagDatum(0, j));
            
            Line2D.Float lBase= new Line2D.Float( (float)xDMin, (float)yBase, (float)xDMax, (float)yBase );
            g.setColor(Color.lightGray);
            g.draw(lBase);
            g.setColor(Color.darkGray);
            
            int yBase1= yAxis.getItemMin(data.getYTagDatum(0, j));
            double canvasHeight= parent.getHeight();
            DasRow littleRow= new DasRow(getCanvas(),yBase1/canvasHeight,yBase/canvasHeight);
            
            zAxisComponent.setLittleRow(littleRow);
            
            double [] binStarts= xbins.binStarts();
            double [] binStops= xbins.binStops();
            
            int y0= yBase;
            
            int littleRowHeight= littleRow.getHeight();
            double zAxisMax= zAxisComponent.getAxis().getDataMaximum().doubleValue(xtysData.getZUnits());
            
            for (int ibin=0; ibin < data.getXLength(); ibin++) {
                int x0= getXAxis().transform(binStarts[ibin],xbins.getUnits());
                int x1;
                x1=x0+1; // 1 pixel wide
                double zz= data.getDouble( ibin, j, data.getZUnits() );
                if ( zz!=zzFill ) {
                    int yAvg= zAxisComponent.transform( zz, data.getZUnits() );
                    yAvg= yAvg > ( y0 - littleRowHeight ) ? yAvg : ( y0 - littleRowHeight );
                    int yHeight= (y0-yAvg)>(0) ? (y0-yAvg) : 0;
                    yHeight= yHeight < littleRowHeight ? yHeight : littleRowHeight;
                    double peakValue = peaks.getDouble(ibin, j, peaks.getZUnits());
                    if (peakValue <= zAxisMax) {
                        int yMax= zAxisComponent.transform(peakValue, peaks.getZUnits());
                        yMax= (y0-yMax)>(0) ? yMax : (y0);
                        if (peaksIndicator==PeaksIndicator.MaxLines) {
                            l.setLine(x0,yMax,x0,yMax);
                            g.drawLine(x0,yMax,x0,yMax);
                        } else if ( peaksIndicator==PeaksIndicator.GrayPeaks ) {
                            rmax.setRect(x0,yMax,(x1-x0),y0-yMax);
                            g.setColor(Color.lightGray);
                            g.fill(rmax);
                            g.setColor(Color.darkGray);
                        }
                    }
                    r.setRect(x0,yAvg,(x1-x0),yHeight);
                    g.fill(r);
                }
            }
            
        }
        
        g.setRenderingHints(hints0);
        
        g.dispose();
        parent.setCursor(cursor0);
    }
    
    public void setData(TableDataSet Data) {
        super.setData(Data);
    }
    
    public DasAxis getZAxis() {
        return this.zAxisComponent.getAxis();
    }
    
    public void setZTitle(String title) {
        getZAxis().setLabel(title);
    }
    
    public class ZAxisComponent extends DasCanvasComponent implements java.beans.PropertyChangeListener {
        private DasAxis zAxis;
        private DasPlot parent;
        private DasRow rowLittle;
        private DasRow rowLittleDoc;
        private DasRow row0;
        
        public ZAxisComponent(DasPlot parent, DasAxis zAxis) {
            this.zAxis= zAxis;
            row0= zAxis.getRow();
            setRow(parent.getRow());
            setColumn(parent.getColumn());
            this.parent= parent;
            setBounds(getBounds());
        }
        
        public Rectangle getBounds(){
            int x1= parent.getColumn().getDMaximum();
            int x2= zAxis.getColumn().getDMaximum();
            int ylow1= parent.getRow().getDMaximum();
            int ylow2= zAxis.getRow().getDMaximum();
            int yhigh1= parent.getRow().getDMinimum();
            int yhigh2= zAxis.getRow().getDMinimum();
            
            Rectangle result= new Rectangle( x1, yhigh1-1, (x2-x1), (ylow1-yhigh1+2));
            
            result.add(x2,yhigh2-1);
            result.add(x2,ylow2+1);
            
            return result;
        }
        
        protected DasAxis getAxis() {
            return this.zAxis;
        }
        
        public void setLittleRow(DasRow row) {
            rowLittle= row;
            int zAxisMid= zAxis.getRow().getDMiddle();
            if (row.contains(zAxisMid))  {
                rowLittleDoc= row;
            }
        }
        
        public int transform( double x, Units units ) {
            int result= zAxis.transform(x,units,rowLittle.getDMaximum(),rowLittle.getDMinimum());
            return result;
        }
        
        public void resize() {
            setBounds(getBounds());
        }
        
        protected void paintComponent(Graphics g1) {
            
            if ( rowLittleDoc==null ) {
                return;
            }
            
            Graphics2D g= (Graphics2D)g1;
            g.translate(-getX(), -getY());
            
            int hlen=3;
            
            int x1= parent.getColumn().getDMaximum()+hlen;
            int x2= zAxis.getColumn().getDMaximum()-hlen;
            int ylow1= rowLittleDoc.getDMaximum();
            int ylow2= zAxis.getRow().getDMaximum();
            int yhigh1= rowLittleDoc.getDMinimum();
            int yhigh2= zAxis.getRow().getDMinimum();
            
            g.setColor(Color.lightGray);
            g.draw(new java.awt.geom.Line2D.Double(x1-hlen,ylow1,x1,ylow1));
            g.draw(new java.awt.geom.Line2D.Double(x2,ylow2,x2+hlen,ylow2));
            g.draw(new java.awt.geom.Line2D.Double(x1,ylow1,x2,ylow2));
            g.draw(new java.awt.geom.Line2D.Double(x1-hlen,yhigh1,x1,yhigh1));
            g.draw(new java.awt.geom.Line2D.Double(x2,yhigh2,x2+hlen,yhigh2));
            g.draw(new java.awt.geom.Line2D.Double(x1,yhigh1,x2,yhigh2));
        }
        
        public void propertyChange(java.beans.PropertyChangeEvent propertyChangeEvent) {
            update();
        }
        
        protected void installComponent() {
            super.installComponent();
            getCanvas().addCanvasComponent(zAxis);
        }
        
    }
    
    public class Rebinner implements DataSetRebinner {
        DataSetRebinner highResRebinner;
        DataSetRebinner lowResRebinner;
        Rebinner() { 
            highResRebinner= new NearestNeighborTableRebinner();
            //highResRebinner= new AveragePeakTableRebinner();
            lowResRebinner= new AveragePeakTableRebinner();            
        }
            
        public DataSet rebin(DataSet ds, RebinDescriptor x, RebinDescriptor y) throws IllegalArgumentException {
            Datum xwidth= (Datum)ds.getProperty( "xTagWidth" );
            if ( xwidth==null ) xwidth= TableUtil.guessXTagWidth((TableDataSet)ds);
            Units rdUnits= x.getUnits();
            if ( rdUnits instanceof LocationUnits ) {
                rdUnits= ((LocationUnits)rdUnits).getOffsetUnits();
            }
            if ( x.binWidth() < xwidth.doubleValue(rdUnits) ) {
                return highResRebinner.rebin( ds, x, y );
            } else {
                return lowResRebinner.rebin( ds, x, y );
            }
        }
        
    }
    
    protected void installComponent() {
        super.installComponent();
        getCanvas().addCanvasComponent(zAxisComponent);
    }
    
    /** Getter for property peaksIndicator.
     * @return Value of property peaksIndicator.
     */
    public PeaksIndicator getPeaksIndicator() {
        return this.peaksIndicator;
    }
    
    /** Setter for property peaksIndicator.
     * @param peaksIndicator New value of property peaksIndicator.
     */
    public void setPeaksIndicator(PeaksIndicator peaksIndicator) {
        this.peaksIndicator= peaksIndicator;
        updatePlotImage();
        repaint();
    }
    
    public void dataSetUpdated(DataSetUpdateEvent e) {
        this.markDirty();
        update();       
        repaint();
    }
    
    public void setDataSetDescriptor(DataSetDescriptor dataSetDescriptor) {
        super.setDataSetDescriptor(dataSetDescriptor);
        dataSetDescriptor.addDataSetUpdateListener( this );        
    }
    
}
