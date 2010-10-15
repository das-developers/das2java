/* File: SpectrogramRenderer.java
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

package org.das2.graph;

import org.das2.dataset.DataSetRebinner;
import org.das2.dataset.AverageTableRebinner;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DefaultTableDataSet;
import org.das2.dataset.RebinDescriptor;
import org.das2.util.DasMath;
import org.das2.DasException;
import org.das2.datum.Units;
import org.das2.util.DasDie;
import org.das2.util.monitor.ProgressMonitor;
import java.awt.*;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.geom.*;
import java.awt.image.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;

/**
 *
 * @author  jbf
 */
public class StippledTableRenderer extends Renderer {
    
    Image plotImage;
    
    Units zUnits= Units.dimensionless;
    
    protected class RebinListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            update();
        }
    }
    
    RebinListener rebinListener= new RebinListener();
    
    public StippledTableRenderer(DataSetDescriptor dsd ) {
        super( dsd );
    }
    
    
    /** Creates a new instance of SpectrogramRenderer
     * @deprecated use {link
     * #SpectrogramRenderer(org.das2.dataset.DataSetDescriptor,
     * org.das2.graph.DasColorBar)}
     */
    public StippledTableRenderer(DasPlot parent, DataSetDescriptor dsd ) {
        this( dsd );
        this.parent = parent;
    }
    
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        Graphics2D g2= (Graphics2D)g.create();
        
        if (getDataSet()==null && lastException!=null ) {
            renderException(g2,xAxis,yAxis,lastException);
        } else if (plotImage!=null) {
            Point2D p;
            p= new Point2D.Float( xAxis.getColumn().getDMinimum(), yAxis.getRow().getDMinimum() );
            int x= (int)(p.getX()+0.5);
            int y= (int)(p.getY()+0.5);
            
            g2.drawImage( plotImage,x,y, getParent() );
        }
        g2.dispose();
    }
    
    int count = 0;
    
    private boolean sliceRebinnedData= true;
    
    public void updatePlotImage( DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor ) throws DasException {
        super.updatePlotImage( xAxis, yAxis, monitor );
        try {
            TableDataSet rebinData;
            
            if (monitor != null) {
                if (monitor.isCancelled()) {
                    return;
                } else {
                    monitor.setTaskSize(-1);
                    monitor.started();
                }
            }
            
            int w = xAxis.getColumn().getDMaximum() - xAxis.getColumn().getDMinimum();
            int h = yAxis.getRow().getDMaximum() - yAxis.getRow().getDMinimum();
            
            if (getParent()==null  || w<=1 || h<=1 ) {
                DasDie.println("canvas not useable!!!");
                return;
            }
            
            if ( getDataSet() == null) {
                Units xUnits = getParent().getXAxis().getUnits();
                Units yUnits = getParent().getYAxis().getUnits();
                
                double[] xTags, yTags;
                xTags = yTags = new double[0];
                double[][] zValues = {yTags};
                rebinData = new DefaultTableDataSet(xTags, xUnits, yTags, yUnits, zValues, zUnits, Collections.EMPTY_MAP);
                plotImage= null;
                rebinData= null;
                getParent().repaint();
                return;
            } else {
                RebinDescriptor xRebinDescriptor;
                xRebinDescriptor = new RebinDescriptor(
                        xAxis.getDataMinimum(), xAxis.getDataMaximum(),
                        w,
                        xAxis.isLog());
                
                RebinDescriptor yRebinDescriptor = new RebinDescriptor(
                        yAxis.getDataMinimum(), yAxis.getDataMaximum(),
                        h,
                        yAxis.isLog());
                
                DataSetRebinner rebinner= new AverageTableRebinner();
                
                rebinData = (TableDataSet)rebinner.rebin(getDataSet(),xRebinDescriptor, yRebinDescriptor);
                
                //TableDataSet weights= (TableDataSet)rebinData.getPlanarView(DataSet.PROPERTY_PLANE_WEIGHTS);
                int itable=0;
                int ny= rebinData.getYLength(itable);
                int nx= rebinData.tableEnd(itable)-rebinData.tableStart(itable);
                
                
                BufferedImage image= new BufferedImage( w, h, BufferedImage.TYPE_4BYTE_ABGR );
                Graphics2D g= (Graphics2D)image.getGraphics();
                g.setColor( Color.BLACK );
                
                Units zunits= rebinData.getZUnits();
                
                double maxn=0;
                for (int i=rebinData.tableStart(itable); i<rebinData.tableEnd(itable); i++) {
                    for (int j=0; j<ny; j++) {
                        
                        double z= rebinData.getDouble(i,j,zUnits);
                        if ( zunits.isFill(z) ) continue;
                        z*= 200;
                        
                        double n= DasMath.log10(Math.max(z,0.000001));
                        if ( n>maxn ) maxn=n;
                        if ( Math.random() < n ) {
                            g.fillRect( i,ny-j,1,1);
                        }
                    }
                }
                plotImage= image;
            }
            
        } finally {
            if (monitor != null) {
                if (monitor.isCancelled()) {
                    return;
                } else {
                    monitor.finished();
                }
            }
            
            getParent().repaint();
        }
    }
    
    protected void installRenderer() {
    }
    
    protected void uninstallRenderer() {
    }
    
    
    /** Getter for property sliceRebinnedData.
     * @return Value of property sliceRebinnedData.
     *
     */
    public boolean isSliceRebinnedData() {
        return this.sliceRebinnedData;
    }
    
    /** Setter for property sliceRebinnedData.
     * @param sliceRebinnedData New value of property sliceRebinnedData.
     *
     */
    public void setSliceRebinnedData(boolean sliceRebinnedData) {
        this.sliceRebinnedData = sliceRebinnedData;
    }
    
}
