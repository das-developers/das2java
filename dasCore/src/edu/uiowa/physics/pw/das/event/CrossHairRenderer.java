/* File: CrossHairRenderer.java
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

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DasStackedHistogramPlot;
import edu.uiowa.physics.pw.das.datum.format.*;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.client.*;

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
    
    private DatumFormatter nfx;
    private DatumFormatter nfy;
    private DatumFormatter nfz;
    
    private FontMetrics fm;
    private int dxMax=-999999;
    private Rectangle dirtyBounds;
    private Rectangle hDirtyBounds;
    private Rectangle vDirtyBounds;
    private Point crossHairLocation=null;
    
    private DataSetConsumer dataSetConsumer;
    
    public CrossHairRenderer(  DasPlot parent, DataSetConsumer dataSetConsumer, DasAxis xAxis, DasAxis yAxis ) {
        this.XAxis= xAxis;
        this.YAxis= yAxis;
        this.parent= parent;
        this.dataSetConsumer= dataSetConsumer;
        dirtyBounds= new Rectangle();
        hDirtyBounds = new Rectangle();
        vDirtyBounds = new Rectangle();
    }
    
    public void renderDrag(Graphics g1, Point p1, Point p2) {
        
        Graphics2D g= (Graphics2D)g1;
        g.setRenderingHints((RenderingHints)edu.uiowa.physics.pw.das.DasProperties.getRenderingHints());
        ds= dataSetConsumer.getDataSet();
        
        /*
        if (crossHairLocation!=null) { //make sure the crosshair is erased
            drawCrossHair(g,crossHairLocation);
        }
         */
        
        if (crossHairLocation==null) {
            
            Datum x= XAxis.invTransform(p2.x+parent.getX());
            Datum y= YAxis.invTransform(p2.y+parent.getY());
            
            nfy= y.getFormatter();
            
            String xAsString;
            nfx = x.getFormatter();
            xAsString= nfx.format(x);
            
            
            String yAsString;
            yAsString= nfy.format(y);
            
            String zAsString="";
            String report;
            
            if (ds instanceof TableDataSet) {
                TableDataSet tds= (TableDataSet)ds;
                Datum zValue= TableUtil.closestDatum(tds,x,y);
                
                try {
                    if ( dataSetConsumer instanceof TableDataSetConsumer ) {
                        nfz= ((TableDataSetConsumer)dataSetConsumer).getZAxis().getDatumFormatter();
                        nfz = DefaultDatumFormatterFactory.getInstance().newFormatter(nfz.toString() + "0");
                    } else if ( parent instanceof DasStackedHistogramPlot ) {
                        nfz = ((DasStackedHistogramPlot) parent).getZAxis().getDatumFormatter();
                        nfz = DefaultDatumFormatterFactory.getInstance().newFormatter(nfz.toString() + "0");
                    } else  {
                        nfz = DefaultDatumFormatterFactory.getInstance().newFormatter("0.00");
                    }
                }
                catch (java.text.ParseException pe) {
                    edu.uiowa.physics.pw.das.DasProperties.getLogger().severe("failure to create formatter");
                    DasAxis axis = ((TableDataSetConsumer)dataSetConsumer).getZAxis();
                    axis.getUnits().getDatumFormatterFactory().defaultFormatter();
                }
                /* TODO: zValue.toString() should work fine */
                if ( zValue.isFill() ) {
                    zAsString= "fill";
                } else {
                    zAsString= nfz.format(zValue);
                }
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
        
        g.setColor(Color.black);
        //g.setXORMode(Color.white);
        
        /*
        if (crossHairLocation!=null) {
            if (!crossHairLocation.equals(p)) {
                drawCrossHair(g,crossHairLocation);
            }
        }
         */
        
        Dimension d= parent.getSize();
        hDirtyBounds.setBounds(0, p.y, d.width, 1);
        g.drawLine( 0,  p.y,  d.width,  p.y);
        vDirtyBounds.setBounds(p.x, 0, 1, d.height);
        g.drawLine( p.x,  0,  p.x,  d.height );
        
        /*
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
         */
        
        g.dispose();
        
    }
    
    public void clear(Graphics g) {
        if (crossHairLocation!=null) {
            drawCrossHair(g,crossHairLocation);
        }
        parent.paintImmediately(dirtyBounds);
        parent.paintImmediately(hDirtyBounds);
        parent.paintImmediately(vDirtyBounds);
        //sorry about that...
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
