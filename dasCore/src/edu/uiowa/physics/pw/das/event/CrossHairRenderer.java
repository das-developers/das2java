/* File: CrossHairRenderer.java
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

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasStackedHistogramPlot;
import edu.uiowa.physics.pw.das.datum.DasFormatter;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetConsumer;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSet;
import edu.uiowa.physics.pw.das.dataset.XTaggedYScanDataSetConsumer;

import java.awt.*;
import java.text.DecimalFormat;

/**
 *
 * @author  eew
 */
public class CrossHairRenderer implements DragRenderer {
    
    protected int xInitial;
    protected int yInitial;
    
    protected DataSet ds;
    protected DasAxis XAxis;
    protected DasAxis YAxis;
    protected DasPlot parent;
    
    private int ix=0; // store the current position within the dataset object
    private int iy=0;
    
    private int context;
    
    private DasFormatter nfx;
    private DasFormatter nfy;
    private DasFormatter nfz;
    
    private FontMetrics fm;
    private int dxMax=-999999;
    private Rectangle dirtyBounds;
    private Point crossHairLocation=null;
    
    private DataSetConsumer dataSetConsumer;
    
    public CrossHairRenderer(  DasPlot parent, DataSetConsumer dataSetConsumer, DasAxis xAxis, DasAxis yAxis ) {
        this.XAxis= xAxis;
        this.YAxis= yAxis;
        this.parent= parent;
        this.dataSetConsumer= dataSetConsumer;
        dirtyBounds= new Rectangle();
    }
    
    public void renderDrag(Graphics g1, Point p1, Point p2) {
        
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHints((RenderingHints)edu.uiowa.physics.pw.das.DasProperties.getRenderingHints());
        ds= dataSetConsumer.getDataSet();
        
        if (crossHairLocation!=null) { //make sure the crosshair is erased
            drawCrossHair(g,crossHairLocation);
        }
        
        if (crossHairLocation==null) {
            
            Datum x= XAxis.invTransform(p2.x+parent.getX());
            Datum y= YAxis.invTransform(p2.y+parent.getY());
            
            nfy= y.getFormatter();
            
            String xAsString;
            nfx= (DasFormatter)x.getFormatter();
            xAsString= nfx.format(x);
            
            
            String yAsString;
            yAsString= nfy.format(y);
            
            String zAsString="";
            String report;
            
            if (ds instanceof XTaggedYScanDataSet) {
                XTaggedYScanDataSet xtyds= (XTaggedYScanDataSet)ds;
                if (x.getUnits()!=xtyds.getXUnits()) {
                    throw new IllegalStateException("x units and dataset x units differ");
                }
                
                if (y.getUnits()!=xtyds.getYUnits()) {
                    throw new IllegalStateException("y units and dataset y units differ");
                }
                
                double xx= x.getValue();
                double yy= y.getValue();
                
                Datum zValue= xtyds.getClosestZValue(new Datum(xx,xtyds.getXUnits()),
                new Datum(yy,xtyds.getYUnits()));
                //                if (Math.abs(xtyds.data[ix].x-xx)>5*xtyds.xSampleWidth) {
                //                    ix= (int)((xx-xtyds.data[0].x)/xtyds.xSampleWidth);
                //                    ix= ix<0?0:ix;
                //                    ix= ix<xtyds.data.length?ix:(xtyds.data.length-1);
                //                }
                //                while (ix<xtyds.data.length && xtyds.data[ix].x<xx) ix++;
                //                ix= ix<xtyds.data.length?ix:(xtyds.data.length-1);
                //                while (ix>(-1) && !(xtyds.data[ix].x<xx)) ix--;
                //                ix= ix<0?0:ix;
                //                while (iy<xtyds.y_coordinate.length && xtyds.y_coordinate[iy]<yy) iy++;
                //                iy= iy<xtyds.y_coordinate.length?iy:(xtyds.y_coordinate.length-1);
                //                while (iy>(-1) && xtyds.y_coordinate[iy]>yy) iy--;
                //                iy= iy<0?0:iy;
                //
                if ( dataSetConsumer instanceof XTaggedYScanDataSetConsumer ) {
                    nfz= (DasFormatter)((XTaggedYScanDataSetConsumer)dataSetConsumer).getZAxis().getFormatter().clone();
                    nfz.setMinimumFractionDigits(nfz.getMinimumFractionDigits()+1);
                } else if ( parent instanceof DasStackedHistogramPlot ) {
                    nfz= (DasFormatter)((DasStackedHistogramPlot) parent).getZAxis().getFormatter().clone();
                    nfz.setMinimumFractionDigits(nfz.getMinimumFractionDigits()+1);
                } else  {
                    nfz= new DasFormatter((DecimalFormat)DecimalFormat.getInstance());
                    nfz.setMaximumFractionDigits(2);
                    nfz.setMinimumFractionDigits(2);
                }
                
                zAsString= nfz.format(zValue.getValue(),zValue.getUnits());
                report= "x:"+xAsString+" y:"+yAsString+" z:"+zAsString;
            } else {
                report= "x:"+xAsString+" y:"+yAsString;
            }
            
            fm= parent.getGraphics().getFontMetrics();
            
            Color color0= g.getColor();
            g.setColor(new Color(255,255,255,200));
            
            Dimension d= parent.getSize();
            
            int dx= fm.stringWidth(report)+6;
            if (dxMax<dx) dxMax=dx;
            int dy= fm.getAscent()+fm.getDescent();
            int xp= p2.x+3;
            int yp= p2.y-3-dy;
            
            if ( (xp+dxMax>d.width-3) && (p2.x-3-dx>0) ) {
                xp= p2.x-3-dx;
            }
            
            if (yp<13) {
                yp= p2.y+3;
            }
            
            dirtyBounds.setRect(xp,yp,dx,dy);
            g.fill(dirtyBounds);
            
            g.setColor(new Color(20,20,20));
            g.drawString(report,xp+3,yp+fm.getAscent());
            g.setColor(color0);
        }
        
        drawCrossHair(g,p2);
        
    }
    
    public MouseDragEvent getMouseDragEvent(Object source, Point p1, Point p2, boolean isModified) {
        return null;
    }
    
    private void drawCrossHair(Graphics g0, Point p) {
        
        Graphics g= g0.create();
        
        g.setColor(new Color(0,0,0));
        g.setXORMode(Color.white);
        
        if (crossHairLocation!=null) {
            if (!crossHairLocation.equals(p)) {
                drawCrossHair(g,crossHairLocation);
            }
        }
        
        Dimension d= parent.getSize();
        g.drawLine((int)0, (int)p.y, (int)d.getWidth(), (int)p.y);
        g.drawLine((int)p.x, (int)0, (int)p.x, (int)d.getHeight() );
        
        if (crossHairLocation!=null) {
            if (crossHairLocation.equals(p)) {
                crossHairLocation=null;
            } else {
                // this shouldn't happen if things are working properly
                edu.uiowa.physics.pw.das.util.DasDie.println("Sorry about the crosshair mess!");
                crossHairLocation=p;
            }
        } else {
            crossHairLocation= p;
        }
        
        g.dispose();
        
    }
    
    public void clear(Graphics g) {
        if (crossHairLocation!=null) {
            drawCrossHair(g,crossHairLocation);
        }
        parent.paintImmediately(dirtyBounds);
        int x=1;
    }
    
    public boolean isXRangeSelection() {
        return false;
    }
    
    public boolean isYRangeSelection() {
        return false;
    }
    
    public boolean isPointSelection() {
        return true;
    }
    
    public boolean isUpdatingDragSelection() {
        return false;
    }
}
