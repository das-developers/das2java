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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.*;
import edu.uiowa.physics.pw.das.components.propertyeditor.Enumeration;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.util.*;
import java.awt.*;
import java.awt.geom.*;

/**
 *
 * @author  jbf
 */
public class CurveRenderer extends Renderer {
    
    private Stroke stroke;
    
    private String xplane;
    private String yplane;
    
    private VectorDataSet xds;
    private VectorDataSet yds;
    private Units xunits; // xUnits of the axis
    private Units yunits; // yUnits of the axis
    private double[][] idata;  // data transformed to pixel space
    
    /** Holds value of property lineWidth. */
    private double lineWidth;
    
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
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis) {
        Graphics2D g= (Graphics2D)g1;
        g.setStroke( stroke );
        
        DataSet ds= getDataSet();
        
        Datum xTagWidth;
        if ( ( xTagWidth=(Datum)ds.getProperty("xTagWidth") ) == null ) {
            xTagWidth= DataSetUtil.guessXTagWidth(ds);
        }
        
        xds= (VectorDataSet) ds.getPlanarView(xplane);
        yds= (VectorDataSet) ds.getPlanarView(yplane);
        xunits= xds.getYUnits();
        yunits= yds.getYUnits();
        
        idata= new double[2][xds.getXLength()];
        for ( int i=0; i<xds.getXLength(); i++ ) {
            idata[0][i]= xAxis.transform(xds.getDouble(i,xunits),xunits);
            idata[1][i]= yAxis.transform(yds.getDouble(i,yunits),yunits);
        }
        
        for ( int i=1; i<xds.getXLength(); i++ ) {
            if ( ds.getXTagDatum(i).subtract( ds.getXTagDatum(i-1) ) .le(xTagWidth ) )
                g.drawLine((int)idata[0][i-1],(int)idata[1][i-1],(int)idata[0][i],(int)idata[1][i]);
        }
        
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis) {
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
        this.lineWidth = lineWidth;
        stroke= new BasicStroke((float)lineWidth);
    }
    
    public DataSet getDataSet() {
        DataSetDescriptor dsd= getDataSetDescriptor();
        if ( ! ( dsd instanceof ConstantDataSetDescriptor )) {
            throw new IllegalStateException( "only ConstantDataSetDescriptors for now!" );
        } else {
            try {
                return dsd.getDataSet(null, null, null, null );
            } catch ( DasException e ) {
                throw new RuntimeException(e);
            }
        }
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) {
    }
    
    protected org.w3c.dom.Element getDOMElement(org.w3c.dom.Document document) {
        return null;
    }    
    
}
