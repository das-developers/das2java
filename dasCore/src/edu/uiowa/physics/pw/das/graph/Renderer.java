/* File: Renderer.java
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

package edu.uiowa.physics.pw.das.graph;
 
import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.components.PropertyEditor;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.dataset.*;

import javax.swing.*;
import java.awt.*;
import java.io.InterruptedIOException;

public abstract class Renderer implements DataSetConsumer, PropertyEditor.Editable {
    
    // dsd reloads ds when plot params change.
    private DataSetDescriptor dsd;
    
    // avoid get/set methods unless you know what you're doing.
    private DataSet ds;
    DasPlot parent;
    
    private DasProgressPanel progressPanel;
    private DataRequestThread drt;

    protected Exception lastException;
    
    protected Renderer(DataSetDescriptor dsd) {
        this.dsd = dsd;
    }
    
    protected Renderer(DataSet ds) {
        this((ds == null ? (DataSetDescriptor)null : new ConstantDataSetDescriptor(ds)));
    }
    
    /** Creates a new instance of Renderer
     * @deprecated use {@line #Renderer(edu.uiowa.physics.pw.das.dataset.DataSetDescriptor)}
     */
    protected Renderer(DasPlot parent, DataSetDescriptor dsd) {
        this(dsd);
        this.parent= parent;
    }
    
    /**
     * @deprecated use {@link #Renderer(edu.uiowa.physics.pw.das.dataset.DataSet)}
     */
    protected Renderer(DasPlot parent, DataSet ds) {
        this((ds == null ? (DataSetDescriptor)null : new ConstantDataSetDescriptor(ds)));
        this.parent = parent;
    }
    
    public DasPlot getParent() { return this.parent; }
    
    public DataSet getDataSet() { return this.ds; }
    
    public void setDataSet(DataSet ds) {
        setDataSetDescriptor(new ConstantDataSetDescriptor(ds));
    } 
    
    public void setDataSetID(String id) throws edu.uiowa.physics.pw.das.DasException {
        if (id == null) throw new NullPointerException("Null dataPath not allowed");
        if (id.equals("")) {
            setDataSetDescriptor(null);
            return;
        }
        DataSetDescriptor dsd = DataSetDescriptor.create(id);
        setDataSetDescriptor(dsd);
    }
    
    public String getDataSetID() {
        if (dsd == null) {
            return "";
        }
        return dsd.getDataSetID();
    }
    
    public abstract void render(Graphics g, DasAxis xAxis, DasAxis yAxis);
    
    public void renderException( Graphics g, DasAxis xAxis, DasAxis yAxis, Exception e ) {
        int x= (int)xAxis.getColumn().getDMiddle();
        int y= (int)yAxis.getRow().getDMiddle();
        
        String s= e.toString();
        FontMetrics fm= g.getFontMetrics();
        
        if ( e instanceof NoDataInIntervalException ) {
            s= "no data in interval";
        }
        
        int width= fm.stringWidth(s);
        
        Color color0= g.getColor();
        g.setColor(Color.lightGray);
        g.drawString(s,x-width/2,y);
        g.setColor(color0);        
    }
    
    protected void loadDataSet(final DasAxis xAxis, final DasAxis yAxis) {
        
        if (parent == null || !parent.isDisplayable() || dsd == null) return;
        
        if (xAxis instanceof DasTimeAxis) {

            lastException= null;
            
            final DasPlot parent= getParent();
            final Cursor cursor0= null;
            if (parent != null) {
                parent.getCursor();
                parent.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            }
        
            if (parent != null) {
                ((DasCanvas)parent.getParent()).lockDisplay(this);
            }
            
            Datum dataRange1 = xAxis.getDataMaximum().subtract(xAxis.getDataMinimum());
            double dataRange= dataRange1.convertTo(Units.seconds).doubleValue();
            double deviceRange = Math.floor(xAxis.getColumn().getDMaximum() + 0.5) - Math.floor(xAxis.getColumn().getDMinimum() + 0.5);
            double resolution =  dataRange/deviceRange;
            DasTimeAxis taxis = (DasTimeAxis)xAxis;
            if (progressPanel == null) {
                progressPanel = new DasProgressPanel();
                ((Container)(((DasCanvas)parent.getParent()).getGlassPane())).add(progressPanel);
            }
            progressPanel.setSize(progressPanel.getPreferredSize());
            
            int x= (int)xAxis.getColumn().getDMiddle();
            int y= (int)xAxis.getRow().getDMiddle();
            
            progressPanel.setLocation( x - progressPanel.getWidth()/2,
                                       y - progressPanel.getHeight()/2 );
            dsd.addDasReaderListener(progressPanel);
            DataRequestor requestor = new DataRequestor() {
                
                public void currentByteCount(int byteCount) {
                }
                public void totalByteCount(int byteCount) {
                }
                public void exception(Exception exception) {
                    if (!(exception instanceof InterruptedIOException)) {
                        if (exception instanceof edu.uiowa.physics.pw.das.DasException ) {
                            lastException= exception; 
                            finished(null);
                        } else {
                            Object[] message = {"Error reading data set", new JEditorPane("text/html", exception.getMessage())};
                            ((JEditorPane)message[1]).setEditable(false);
                            //JOptionPane.showMessageDialog(DasPlot.this, message);
                            DasExceptionHandler.handle(exception);
                            finished(null);
                        }
                    }
                }
                public void finished(DataSet dsFinished) {
                    if (dsd != null) dsd.removeDasReaderListener(progressPanel);
                    progressPanel.setVisible(false);
                    if ( parent != null) {
                        parent.setCursor(cursor0);
                    }                    
                    ds= dsFinished;
                    updatePlotImage(xAxis,yAxis);
                    if ( parent!= null) {
                        ((DasCanvas)parent.getParent()).freeDisplay(this);
                    }
                    edu.uiowa.physics.pw.das.util.DasDie.println("I GOT HERE WITH: " + ds);
                }
            };
            if (drt == null) {
                drt = new DataRequestThread();
            }
            try {
                drt.request(dsd, "", taxis.getTimeMinimum(), taxis.getTimeMaximum(), resolution, requestor);
            }
            catch (InterruptedException ie) {
                DasExceptionHandler.handle(ie);
            }
            
        } else {
            if ( ( dsd instanceof ConstantDataSetDescriptor ) ) {
                try {
                    ds= dsd.getDataSet( null, null, null );
                    updatePlotImage(xAxis,yAxis);
                } catch ( edu.uiowa.physics.pw.das.DasException e ) {
                    DasExceptionHandler.handle(e);
                }
            } else {
                throw new AssertionError( "xAxis not a timeAxis, and DataSetDescriptor is not constant" );
            }       
        }
    }
        
    public abstract void updatePlotImage(DasAxis xAxis, DasAxis yAxis);
    
    public void update(DasAxis xAxis, DasAxis yAxis) {
        edu.uiowa.physics.pw.das.util.DasDie.println("in Render.update()");
        loadDataSet(xAxis,yAxis);
    }
    
    void setDataSetDescriptor(DataSetDescriptor dsd) {
        this.dsd = dsd;
        if (parent != null) {
            parent.markDirty();
            parent.update();
        }
    }
    
    protected abstract void installRenderer();
    
    protected abstract void uninstallRenderer();
}
