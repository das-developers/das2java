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
import edu.uiowa.physics.pw.das.system.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.io.InterruptedIOException;

public abstract class Renderer implements DataSetConsumer, Editable, DataSetUpdateListener {
    
    // dsd reloads ds when plot params change.
    private DataSetDescriptor dsd;
    
    // avoid get/set methods unless you know what you're doing.
    protected DataSet ds;
    
    private boolean fullResolution = false;
    
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
    
    public boolean isFullResolution() {
        return fullResolution;
    }
    
    public void setFullResolution(boolean b) {
        if (fullResolution == b) return;
        fullResolution = b;
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
        String message;
        FontMetrics fm= g.getFontMetrics();
        
        if ( e instanceof NoDataInIntervalException ) {
            s= "no data in interval";
            message= e.getMessage();             
        } else {
            s= e.getMessage();      
            message= "";
            if ( s == null || s.equals("") ) {
                s= e.toString();
            }
        }
        
        if ( !message.equals("") ) {
            s+= ":!c"+message;
        }
        
        GrannyTextRenderer gtr= new GrannyTextRenderer();
        gtr.setString( parent, s );
        gtr.setAlignment(GrannyTextRenderer.CENTER_ALIGNMENT);
        
        int width= (int)gtr.getWidth();
        
        Color color0= g.getColor();
        g.setColor(Color.lightGray);
        gtr.draw(g,x-width/2,y);
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
            return;
        }
        lastException= null;
        
        Runnable request = new Runnable() {
            public void run() {
                Datum resolution;
                Datum dataRange1 = xAxis.getDataMaximum().subtract(xAxis.getDataMinimum());

                double deviceRange = Math.floor(xAxis.getColumn().getDMaximum() + 0.5) - Math.floor(xAxis.getColumn().getDMinimum() + 0.5);
                if (fullResolution) {
                    resolution = null;
                }
                else {
                    resolution =  dataRange1.divide(deviceRange);
                }
            
                if ( deviceRange==0.0 ) {
                    // this conidition occurs sometimes at startup, it's not known why
                    return;
                }
            
                if (progressPanel == null) {
                    progressPanel = DasProgressPanel.createComponentPanel(parent,"Loading data set");                
                } else {
                    progressPanel.setLabel("Loading data set" );
                }
                progressPanel.cancel();
                
                dsd.requestDataSet(xAxis.getDataMinimum(), xAxis.getDataMaximum(), resolution, progressPanel);
            }
        };

        //Give the user something pretty to look at.
        //This will scale the current data, or move it off screen
        //as a sort of preview.
        try {
            updatePlotImage(xAxis, yAxis, null);
        }
        catch (DasException de) {
            //We don't care if this throws an exception.
        }

        RequestProcessor.invokeLater(request, getParent().getCanvas());
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
                update();
            }
            getParent().repaint();
        }
    }
    
    public void update() {
        if (parent != null) {
            java.awt.EventQueue eventQueue =
            Toolkit.getDefaultToolkit().getSystemEventQueue();
            DasRendererUpdateEvent drue = new DasRendererUpdateEvent(parent, this);
            eventQueue.postEvent(drue);
        }
    }
    
    protected void updateImmediately() {
        DasAxis xAxis = parent.getXAxis();
        DasAxis yAxis = parent.getYAxis();
        loadDataSet(xAxis,yAxis);
    }
    
    public void dataSetUpdated( DataSetUpdateEvent e ) {
        //updateImmediately();
        if (e.getException() != null) {
            Exception exception = e.getException();
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
        try {
            ds= e.getDataSet();
            if (progressPanel != null) {
                progressPanel.setLabel("Rebinning data set");
            }
            updatePlotImage(parent.getXAxis(), parent.getYAxis(), progressPanel);
            if (parent != null) {
                parent.repaint();
            }
        }
        catch (DasException de) {
            ds = null;
            lastException = de;
        }
        catch (RuntimeException re) {
            ds = null;
            throw re;
        }
        finally {
            if (progressPanel != null) {
                progressPanel.finished();
            }
        }
    }
    
    public void setDataSetDescriptor(DataSetDescriptor dsd) {
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
