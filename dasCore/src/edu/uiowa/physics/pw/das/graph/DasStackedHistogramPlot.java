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
import edu.uiowa.physics.pw.das.util.DasDate;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.TimeDatum;
import edu.uiowa.physics.pw.das.datum.TimeLocationUnits;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author  jbf
 */
public class DasStackedHistogramPlot extends edu.uiowa.physics.pw.das.graph.DasPlot implements edu.uiowa.physics.pw.das.graph.DasZAxisPlot {
    
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
    
    public static DasStackedHistogramPlot create(DasCanvas parent, XTaggedYScanDataSet Data) {
        DasRow row= new DasRow(parent,.05,.85);
        DasColumn column= new DasColumn(parent,0.15,0.90);
        
        double [] x;
        
        int nx= Data.data.length;
        int ny= Data.y_coordinate.length;
        
        x= new double[nx];
        for (int i=0; i<nx; i++) {
            x[i]= Data.data[i].x;
        }
        
        double [] z= new double[nx*ny];
        int iz= 0;
        for (int i=0; i<nx; i++) {
            for (int j=0; j<ny; j++) {
                z[iz++]= Data.data[i].z[j];
            }
        }
        
        DasAxis ZAxis= DasAxis.create(z,Data.getZUnits(),
        new AttachedRow(row,0.,0.6),new AttachedColumn(column,1.05,1.10),
        DasAxis.RIGHT,true);
        
        DasAxis XAxis;
        if (Data.getXUnits() instanceof TimeLocationUnits) {
            XAxis = DasAxis.create(x,Data.getXUnits(),row,column,DasAxis.HORIZONTAL,false);
        } else {
            XAxis = DasTimeAxis.create(x,Data.getXUnits(),row,column,DasAxis.HORIZONTAL);
        }
        
        DasStackedHistogramPlot plot = DasStackedHistogramPlot.create(parent, Data, XAxis, ZAxis, row, column);
        
        return plot;
    }
    
    
    public static DasStackedHistogramPlot create(DasCanvas parent, XTaggedYScanDataSet data,
    DasAxis xAxis, DasAxis zAxis,
    DasRow row, DasColumn column) {
        Datum[] datums= new Datum[data.y_coordinate.length];
        Units units= data.getYUnits();
        for ( int i=0; i<data.y_coordinate.length; i++ ) datums[i]= Datum.create(data.y_coordinate[i],units);
        return new DasStackedHistogramPlot(  new ConstantDataSetDescriptor(data),
        xAxis,
        new DasLabelAxis(datums,row,column,DasAxis.VERTICAL),
        zAxis,
        row,
        column );
    }
    
    public static DasStackedHistogramPlot create( XTaggedYScanDataSetDescriptor dsd,
    DasAxis xAxis, DasAxis zAxis,
    DasRow row, DasColumn column) {
        Datum[] datums= new Datum[dsd.y_coordinate.length];
        Units units= dsd.getYUnits();
        for ( int i=0; i<dsd.y_coordinate.length; i++ ) datums[i]= Datum.create(dsd.y_coordinate[i],units);
        return new DasStackedHistogramPlot( dsd, xAxis, new DasLabelAxis(datums,row,column,DasAxis.VERTICAL), zAxis, row, column );
    }
    
    public DasStackedHistogramPlot( XTaggedYScanDataSetDescriptor dsd,
    DasAxis xAxis, DasLabelAxis yAxis, DasAxis zAxis,
    DasRow row, DasColumn column) {
        this( (DataSetDescriptor)dsd, xAxis, yAxis, zAxis, row, column );
    }
    
    private DasStackedHistogramPlot( DataSetDescriptor dsd,
    DasAxis xAxis, DasLabelAxis yAxis, DasAxis zAxis,
    DasRow row, DasColumn column ) {
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
        
    }
    
    public static DasStackedHistogramPlot create( DasCanvas parent, XTaggedYScanDataSetDescriptor dsd,
    Datum startTime, Datum endTime ) throws edu.uiowa.physics.pw.das.DasException {
        XTaggedYScanDataSet ds= (XTaggedYScanDataSet)dsd.getDataSet("",startTime,endTime);
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
        
        int w= (int)getColumn().getWidth();
        int h= (int)getRow().getHeight();
        
        plotImage = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        
        Graphics2D g = (Graphics2D)plotImage.getGraphics();
        g.translate(-(int)getColumn().getDMinimum(),-(int)getRow().getDMinimum());
        
        Dimension d;
        
        double iMin= getColumn().getDMinimum();
        double jMin= getRow().getDMinimum();
        
        RenderingHints hints0= g.getRenderingHints();
        RenderingHints hints=
        new RenderingHints( RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_OFF );
        g.setRenderingHints(hints);
        
        DasAxis xaxis= getXAxis();
        RebinDescriptor xbins=
        new RebinDescriptor(
        xaxis.getDataMinimum(),
        xaxis.getDataMaximum(),
        (int)(Math.abs(getColumn().getDMaximum()-getColumn().getDMinimum())/1)+1,
        (getXAxis().isLog())
        );
        
        float xDMax= (float)getColumn().getDMaximum();
        float xDMin= (float)getColumn().getDMinimum();
        
        //boolean drawnZAxis=false;
        //DasRow drawZAxisRow=null;
        //DasAxis drawThisZAxis=null;
        
        XTaggedYScanDataSet xtysData= (XTaggedYScanDataSet)Data;
        
        if ( Data==null ) {
            edu.uiowa.physics.pw.das.util.DasDie.println("null data set");
            return;
        }
        
        xtysData.getPeaks();  // create peaks if it doesn't have them already
        XTaggedYScanDataSet rebinDs= xtysData.binAverageX(xbins);
        XTaggedYScan[] data= rebinDs.data;
        XTaggedYScan[] peaks= rebinDs.getPeaks();
        XTaggedYScan[] weights= rebinDs.getWeights();
        
        for (int j=0; j<xtysData.y_coordinate.length; j++) {
            DasLabelAxis yAxis= (DasLabelAxis)getYAxis();
            
            double yBase= getYAxis().transform(xtysData.y_coordinate[j],xtysData.getYUnits());
            
            Line2D.Float lBase= new Line2D.Float( xDMin, (float)yBase, xDMax, (float)yBase );
            g.setColor(Color.lightGray);
            g.draw(lBase);
            g.setColor(Color.darkGray);
            
            double yBase1= yBase - yAxis.getInterItemSpace();
            double canvasHeight= parent.getHeight();
            DasRow littleRow= new DasRow(getCanvas(),yBase1/canvasHeight,yBase/canvasHeight);
            
            zAxisComponent.setLittleRow(littleRow);
            
            //            for (int i=0; i<xtysData.data.length; i++) {
            //                int ibin= xbins.whichBin(xtysData.data[i].x,Data.getXUnits());
            //                if (ibin!=-1) {
            //                    double z= xtysData.data[i].z[j];
            //                    if (!Double.isNaN(z) && z!=xtysData.getZFill()){
            //                        zAvgS[ibin]+= z;
            //                        zAvgN[ibin]++;
            //                        zMax[ibin]= zMax[ibin]>z ? zMax[ibin] : z;
            //                    }
            //                }
            //            }
            
            double [] binStarts= xbins.binStarts();
            double [] binStops= xbins.binStops();
            
            double y0= yBase;
            Rectangle2D.Double r= new Rectangle2D.Double();
            Line2D.Double l= new Line2D.Double();
            Rectangle2D.Double rmax= new Rectangle2D.Double();
            
            int littleRowHeight= (int)littleRow.getHeight();
            double zAxisMax= zAxisComponent.getAxis().getDataMaximum().doubleValue(xtysData.getZUnits());
            double dd= TimeUtil.create("2003-7-12").doubleValue((getXAxis().getUnits()));
            int ibinMax= 0;
            for (int ibin=0; ibin<data.length; ibin++) {
                double x0= (int)getXAxis().transform(binStarts[ibin],xbins.getUnits());
                double x1;
                x1=x0+1; // 1 pixel wide
                if (weights[ibin].z[j]>0) {
                    if (ibin>ibinMax) ibinMax= ibin;
                }
                if (true) {
                    double yAvg= zAxisComponent.transform(data[ibin].z[j],xtysData.getZUnits());
                    yAvg= yAvg > ( y0 - littleRowHeight ) ? yAvg : ( y0 - littleRowHeight );
                    double yHeight= (y0-yAvg)>(0) ? (y0-yAvg) : 0;
                    yHeight= yHeight < littleRowHeight ? yHeight : littleRowHeight;
                    if (peaks[ibin].z[j]<=zAxisMax) {
                        double yMax= zAxisComponent.transform(peaks[ibin].z[j],xtysData.getZUnits());
                        yMax= (y0-yMax)>(0) ? yMax : (y0);
                        if (peaksIndicator==PeaksIndicator.MaxLines) {
                            l.setLine(x0,yMax,x0,yMax);
                            g.drawLine((int)x0,(int)yMax,(int)x0,(int)yMax);
                            //g.draw(l);
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
    
    public void setData(XTaggedYScanDataSet Data) {
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
            double x1= parent.getColumn().getDMaximum();
            double x2= zAxis.getColumn().getDMaximum();
            double ylow1= parent.getRow().getDMaximum();
            double ylow2= zAxis.getRow().getDMaximum();
            double yhigh1= parent.getRow().getDMinimum();
            double yhigh2= zAxis.getRow().getDMinimum();
            
            Rectangle result= new Rectangle((int)x1,(int)yhigh1-1,(int)(x2-x1),(int)(ylow1-yhigh1+2));
            
            result.add(x2,yhigh2-1);
            result.add(x2,ylow2+1);
            
            return result;
        }
        
        protected DasAxis getAxis() {
            return this.zAxis;
        }
        
        public void setLittleRow(DasRow row) {
            rowLittle= row;
            double zAxisMid= ( zAxis.getRow().getDMinimum() + zAxis.getRow().getDMaximum() ) / 2;
            if (row.contains(zAxisMid))  {
                rowLittleDoc= row;
            }
        }
        
        public double transform( double x, Units units ) {
            double result= zAxis.transform(x,units,rowLittle.getDMaximum(),rowLittle.getDMinimum());
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
            
            double x1= parent.getColumn().getDMaximum()+hlen;
            double x2= zAxis.getColumn().getDMaximum()-hlen;
            double ylow1= rowLittleDoc.getDMaximum();
            double ylow2= zAxis.getRow().getDMaximum();
            double yhigh1= rowLittleDoc.getDMinimum();
            double yhigh2= zAxis.getRow().getDMinimum();
            
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
    
}
