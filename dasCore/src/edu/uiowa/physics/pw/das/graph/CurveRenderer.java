/* File: TickCurveRenderer.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on November 3, 2003, 11:43 AM by __FULLNAME__ <__EMAIL__>
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

import org.das2.DasException;
import org.das2.util.monitor.ProgressMonitor;
import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.Enumeration;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author  jbf
 */
public class CurveRenderer extends Renderer {
       
    private String xplane;
    private String yplane;
    
    private Units xunits; // xUnits of the axis
    private Units yunits; // yUnits of the axis
    private double[][] idata;  // data transformed to pixel space
    
    private boolean antiAliased= true;
    private SymColor color= SymColor.black;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    private Psym psym = Psym.NONE;
    private double symSize = 1.0; // radius in pixels
    private float lineWidth = 1.5f; // width in pixels
    
    private GeneralPath path;
        
    /** The dataset descriptor should return a Vector data set with planes identified
     *  by xplane and yplane.
     */
    public CurveRenderer( DataSetDescriptor dsd, String xplane, String yplane ) {
        super(dsd);
        
        setLineWidth( 1.0f );
        
        this.xplane= xplane;
        this.yplane= yplane;
    }
    
    protected void uninstallRenderer() {
    }
    
    protected void installRenderer() {
    }
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        long timer0= System.currentTimeMillis();
        
        VectorDataSet dataSet= (VectorDataSet)getDataSet();
        
        if (dataSet == null || dataSet.getXLength() == 0) {
            return;
        }
        
         VectorDataSet xds= (VectorDataSet)dataSet.getPlanarView(xplane);
         VectorDataSet yds= (VectorDataSet)dataSet.getPlanarView(yplane);
        
        Graphics2D graphics= (Graphics2D) g1.create();
        
        RenderingHints hints0= graphics.getRenderingHints();
        if ( antiAliased ) {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        }
                
        graphics.setColor(color.toColor());
        
        if (path != null) {
            psymConnector.draw(graphics, path, (float)lineWidth);
        }
        
        Dimension d;
        
        double xmin, xmax, ymin, ymax;
        
        edu.uiowa.physics.pw.das.datum.Units xUnits= xAxis.getUnits();
        edu.uiowa.physics.pw.das.datum.Units yUnits= yAxis.getUnits();
        
        Rectangle r= g1.getClipBounds();
        
        if ( r==null ) {
            xmax= xAxis.getDataMaximum().doubleValue(xUnits);
            xmin= xAxis.getDataMinimum().doubleValue(xUnits);
            ymax= yAxis.getDataMaximum().doubleValue(yUnits);
            ymin= yAxis.getDataMinimum().doubleValue(yUnits);
        } else {
            xmin= xAxis.invTransform((int)r.getX()).doubleValue(xUnits);
            xmax= xAxis.invTransform((int)(r.getX()+r.getWidth())).doubleValue(xUnits);
            ymin= yAxis.invTransform((int)r.getY()).doubleValue(yUnits);
            ymax= yAxis.invTransform((int)(r.getY()+r.getHeight())).doubleValue(yUnits);
        }                                
        
        
        for (int index = 0; index < xds.getXLength(); index++) {
            if ( ! yUnits.isFill(yds.getDouble(index,yUnits)) ) {
                double i = xAxis.transform(xds.getDouble(index,xUnits),xUnits);
                double j = yAxis.transform(yds.getDouble(index,yUnits),yUnits);
                if ( Double.isNaN(j) ) {
                    //DasApplication.getDefaultApplication().getDebugLogger().warning("got NaN");
                } else {
                    psym.draw( g1, i, j, (float)symSize );
                }
            }
        }
                
        graphics.setRenderingHints(hints0);
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        super.updatePlotImage( xAxis, yAxis, monitor );
        
        VectorDataSet dataSet= (VectorDataSet)getDataSet();
        
        if (dataSet == null || dataSet.getXLength() == 0) {
            return;
        }
        
        VectorDataSet xds= (VectorDataSet)dataSet.getPlanarView(xplane);
        VectorDataSet yds= (VectorDataSet)dataSet.getPlanarView(yplane);
        
        path= GraphUtil.getPath( xAxis, yAxis, xds, yds, false );
    }
    
    /** Getter for property lineWidth.
     * @return Value of property lineWidth.
     *
     */
    public double getLineWidth() {
        return this.lineWidth;
    }
    
    /** Setter for property lineWidth.
     * @param lineWidth New value of property lineWidth.
     *
     */
    public void setLineWidth(double lineWidth) {
        this.lineWidth = (float)lineWidth;
    }
    
    protected org.w3c.dom.Element getDOMElement(org.w3c.dom.Document document) {
        throw new UnsupportedOperationException();
    }
    
        public PsymConnector getPsymConnector() {
        return psymConnector;
    }
    
    public void setPsymConnector(PsymConnector p) {
        psymConnector = p;
        refreshImage();
    }

    /** Getter for property psym.
     * @return Value of property psym.
     */
    public Psym getPsym() {
        return this.psym;
    }
        
    
    /** Setter for property psym.
     * @param psym New value of property psym.
     */
    public void setPsym(Psym psym) {
        if (psym == null) throw new NullPointerException("psym cannot be null");
        Object oldValue = this.psym;
        this.psym = psym;
        refreshImage();
    }
    
}
