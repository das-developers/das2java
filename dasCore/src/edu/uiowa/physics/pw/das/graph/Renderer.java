/* File: Renderer.java
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

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.components.propertyeditor.Editable;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.stream.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.io.InterruptedIOException;

public abstract class Renderer implements DataSetConsumer, Editable, DataSetUpdateListener {
    
    // dsd reloads ds when plot params change.
    private DataSetDescriptor dsd;
    
    // avoid get/set methods unless you know what you're doing.
    protected DataSet ds;
    
    DasPlot parent;
    
    private DasProgressPanel progressPanel;
    private DataRequestThread drt;
    
    protected Exception lastException;
    
    protected Renderer(DataSetDescriptor dsd) {
        this.dsd = dsd;
        if ( dsd!=null ) dsd.addDataSetUpdateListener(this);
    }
    
    protected Renderer(DataSet ds) {
        this((ds == null ? (DataSetDescriptor)null : new ConstantDataSetDescriptor(ds)));
    }
    
    protected Renderer() {
        this((DataSetDescriptor)null);
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
    
    private boolean dumpDataSet;
    /** Getter for property dumpDataSet.
     * @return Value of property dumpDataSet.
     *
     */
    public boolean isDumpDataSet() {
        return this.dumpDataSet;
    }
    
    /** Setter for property dumpDataSet.
     * @param dumpDataSet New value of property dumpDataSet.
     *
     */
    public void setDumpDataSet(boolean dumpDataSet) {
        try {
            this.dumpDataSet= dumpDataSet;
            if ( dumpDataSet ) {
                System.out.println("Dumping data set");
                JFileChooser chooser= new JFileChooser();
                int xx= chooser.showSaveDialog(this.getParent());
                if ( xx==JFileChooser.APPROVE_OPTION ) {
                    File file= chooser.getSelectedFile();
                    if ( ds instanceof TableDataSet ) {
                        TableUtil.dumpToAsciiStream((TableDataSet)ds, new FileOutputStream(file) );
                    } else if ( ds instanceof VectorDataSet ) {
                        VectorUtil.dumpToAsciiStream((VectorDataSet)ds, new FileOutputStream(file) );
                    } else {
                        throw new DasException("don't know how to serialize data set" );
                    }
                }
                setDumpDataSet( false );
            }
        } catch ( Exception e ) {
            DasExceptionHandler.handle( e );
        }
        this.dumpDataSet= dumpDataSet;
    }
    
    public void setLastException( Exception e ) {
        this.lastException= e;
    }
    
    public Exception getLastException() {
        return this.lastException;
    }
    
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
    
    
    /** Render is called whenever the image needs to be refreshed or the content
     * has changed.  This operation should occur with an animation-interactive
     * time scale, and an image should be cached when this is not possible.
     */
    public abstract void render(Graphics g, DasAxis xAxis, DasAxis yAxis);
    
    public void renderException( Graphics g, DasAxis xAxis, DasAxis yAxis, Exception e ) {
        int x= xAxis.getColumn().getDMiddle();
        int y= yAxis.getRow().getDMiddle();
        
        String s;
        FontMetrics fm= g.getFontMetrics();
        
        if ( e instanceof NoDataInIntervalException ) {
            s= "no data in interval";
        } else {
            s= e.getMessage();
            if ( "".equals(s) ) {
                s= e.toString();
            }
        }
        
        int width= fm.stringWidth(s);
        
        Color color0= g.getColor();
        g.setColor(Color.lightGray);
        g.drawString(s,x-width/2,y);
        g.setColor(color0);
    }
    
    protected void loadDataSet(final DasAxis xAxis, final DasAxis yAxis) {
        
        if (parent == null || !parent.isDisplayable() || dsd == null) return;
        
        if ( dsd instanceof ConstantDataSetDescriptor ) {
            try {
                ds= dsd.getDataSet( null, null, null, null );
                updatePlotImage(xAxis,yAxis, progressPanel);
                return;
            } catch ( DasException exception ) {
                if (exception instanceof edu.uiowa.physics.pw.das.DasException ) {
                    lastException= exception;
                    ds= null;
                }
                DasExceptionHandler.handle(exception);
            }
            
        } else {
            
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
            
            Datum resolution;
            Datum dataRange1 = xAxis.getDataMaximum().subtract(xAxis.getDataMinimum());
            
            double deviceRange = Math.floor(xAxis.getColumn().getDMaximum() + 0.5) - Math.floor(xAxis.getColumn().getDMinimum() + 0.5);
            resolution =  dataRange1.divide(deviceRange);
            
            if ( deviceRange==0.0 ) {
                // this conidition occurs sometimes at startup, it's not known why
                return;
            }
            
            if (progressPanel == null) {
                progressPanel = new DasProgressPanel("Loading data set");
                ((Container)(((DasCanvas)parent.getParent()).getGlassPane())).add(progressPanel);
            } else {
                progressPanel.setLabel("Loading data set" );
            }
            progressPanel.cancel();
            progressPanel.setSize(progressPanel.getPreferredSize());
            
            int x= xAxis.getColumn().getDMiddle();
            int y= xAxis.getRow().getDMiddle();
            
            progressPanel.setLocation( x - progressPanel.getWidth()/2,
            y - progressPanel.getHeight()/2 );
            
            DataRequestor requestor = new DataRequestor() {
                public void exception(Exception exception) {
                    try {
                        if (!(exception instanceof InterruptedIOException) &&
                        !( ( exception instanceof StreamException) && (!( ((StreamException)exception).getCause() instanceof InterruptedIOException ) ) ) ) {
                            if (exception instanceof edu.uiowa.physics.pw.das.DasException ) {
                                lastException= exception;
                            }
                            if ( ! ( ( exception instanceof NoDataInIntervalException ) )  ){
                                DasExceptionHandler.handle(exception);
                            }
                            
                        }
                    }
                    finally {
                        finished(null);
                    }
                }
                public void finished(DataSet dsFinished) {
                    try {
                        if ( parent != null) {
                            parent.setCursor(cursor0);
                        }
                        ds= dsFinished;
                        progressPanel.setLabel("Rebinning data set");
                        updatePlotImage(xAxis,yAxis, progressPanel);
                        if ( parent!= null) {
                            ((DasCanvas)parent.getParent()).freeDisplay(this);
                        }
                    }
                    catch (DasException de) {
                        ds = null;
                        lastException = de;
                    }
                    catch (RuntimeException e) {
                        ds = null;
                        throw e;
                    }
                    finally {
                        progressPanel.finished();
                    }
                }
            };
            if (drt == null) {
                drt = new DataRequestThread();
            }
            try {
                progressPanel.setLabel("Loading Data Set" );
                drt.request(dsd, xAxis.getDataMinimum(), xAxis.getDataMaximum(), resolution, requestor, progressPanel);
                //This just gives the user something pretty to look at while data is loading.
                updatePlotImage(xAxis, yAxis, null);
            }
            catch (DasException de) {
                //Do nothing, whatever is displayed will be replaced.
            }
            catch (InterruptedException ie) {
                DasExceptionHandler.handle(ie);
            }
        }
    }
    
    /** updatePlotImage is called once the expensive operation of loading
     * the data is completed.  This operation should occur on an interactive
     * time scale.  This is an opportunity to create a cache
     * image of the data with the current axis state, when the render
     * operation cannot operation on an animation interactive time scale.
     *
     */
    public abstract void updatePlotImage(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) throws DasException ;
    
    protected void refreshImage() {
        if ( getParent()!=null ) {
            getParent().markDirty();
            if ( this instanceof SpectrogramRenderer ) {
                DasAxis xaxis= getParent().getXAxis();
                DasAxis yaxis= getParent().getYAxis();
                update(xaxis,yaxis);
            }
            getParent().repaint();
        }
    }
    
    public void update(DasAxis xAxis, DasAxis yAxis) {
        loadDataSet(xAxis,yAxis);
    }
    
    public void dataSetUpdated( DataSetUpdateEvent e ) {
        DasAxis xAxis= parent.getXAxis();
        DasAxis yAxis= parent.getYAxis();
        loadDataSet(xAxis, yAxis);
    }
    
    void setDataSetDescriptor(DataSetDescriptor dsd) {
        if ( this.dsd!=null ) this.dsd.removeDataSetUpdateListener(this);
        this.dsd = dsd;
        if ( dsd!=null ) dsd.addDataSetUpdateListener(this);
        if (parent != null) {
            parent.markDirty();
            parent.update();
        }
        ds = null;
    }
    
    public DataSetDescriptor getDataSetDescriptor() {
        return this.dsd;
    }
    
    protected abstract void installRenderer();
    
    protected abstract void uninstallRenderer();
}
