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

package org.das2.graph;

import org.das2.dataset.DataSetDescriptor;
import org.das2.DasException;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.*;
import java.awt.geom.*;
import org.das2.qds.DataSetOps;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 * Old renderer that really doesn't do anything that the SeriesRenderer can do.
 * @author  jbf
 */
public class CurveRenderer extends Renderer {
       
    private String xplane;
    private String yplane;
        
    private boolean antiAliased= true;
    private SymColor color= SymColor.black;
    private PsymConnector psymConnector = PsymConnector.SOLID;
    private Psym psym = Psym.NONE;
    private double symSize = 1.0; // radius in pixels
    private float lineWidth = 1.5f; // width in pixels
    
    private GeneralPath path;
        
    /** The dataset descriptor should return a rank 2 QDataSet with time for
     * and a bundle descriptor for BUNDLE_1.  DataSetOps.unbundle is used
     * to extract the xplane and yplane components.
     *
     */
    public CurveRenderer( DataSetDescriptor dsd, String xplane, String yplane ) {
        super(dsd);
        
        setLineWidth( 1.0f );
        
        this.xplane= xplane;
        this.yplane= yplane;
    }
    
    
    public void render(java.awt.Graphics g1, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        
        QDataSet dataSet= getDataSet();
        
        if (dataSet == null || dataSet.length() == 0) {
            return;
        }

        QDataSet xds;
        if ( xplane!=null && xplane.length()>1 ) {
            xds= DataSetOps.unbundle( dataSet, xplane );
        } else if ( dataSet.rank()==1 ) {
            xds= SemanticOps.xtagsDataSet(dataSet);
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        
        QDataSet yds;
        if ( yplane!=null && yplane.length()>1 ) {
            yds= DataSetOps.unbundle( dataSet, yplane );
        } else if ( dataSet.rank()==1 ) {
            yds= dataSet;
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        QDataSet wds= SemanticOps.weightsDataSet(xds);

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
        
        org.das2.datum.Units xUnits= xAxis.getUnits();
        org.das2.datum.Units yUnits= yAxis.getUnits();
        
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
        
        
        for (int index = 0; index < xds.length(); index++) {
            if ( wds.value(index)>0  ) {
                double i = xAxis.transform(xds.value(index),xUnits);
                double j = yAxis.transform(yds.value(index),yUnits);
                psym.draw( g1, i, j, (float)symSize );
            }
        }
                
        graphics.setRenderingHints(hints0);
    }
    
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        super.updatePlotImage( xAxis, yAxis, monitor );
        
        QDataSet dataSet= getDataSet();
        
        if (dataSet == null || dataSet.length() == 0) {
            return;
        }
        
        QDataSet xds;
        if ( xplane!=null && xplane.length()>1 ) {
            xds= DataSetOps.unbundle( dataSet, xplane );
        } else if ( dataSet.rank()==1 ) {
            xds= SemanticOps.xtagsDataSet(dataSet);
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        
        QDataSet yds;
        if ( yplane!=null && yplane.length()>1 ) {
            yds= DataSetOps.unbundle( dataSet, yplane );
        } else if ( dataSet.rank()==1 ) {
            yds= dataSet;
        } else {
            throw new IllegalArgumentException("rank must be 1 or xplane identified");
        }
        
        path= GraphUtil.getPath( xAxis, yAxis, xds, yds, false, false );
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
        updateCacheImage();
    }
    
    protected org.w3c.dom.Element getDOMElement(org.w3c.dom.Document document) {
        throw new UnsupportedOperationException();
    }
    
        public PsymConnector getPsymConnector() {
        return psymConnector;
    }
    
    public void setPsymConnector(PsymConnector p) {
        psymConnector = p;
        updateCacheImage();
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
        updateCacheImage();
    }
    
}
