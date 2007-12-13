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
import edu.uiowa.physics.pw.das.components.propertyeditor.Editable;
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.system.DasLogger;
import java.awt.geom.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.logging.Logger;
import org.w3c.dom.*;

public abstract class Renderer implements DataSetConsumer, Editable {
    
    /**
     * identifies the dataset (in the DataSetDescriptor sense) being plotted
     * by the Renderer.  May be null if no such identifier exists.  See
     * DataSetDescriptor.create( String id ).
     */
    String dataSetId;
    
    /**
     * The dataset that is being plotted by the Renderer.
     */
    protected DataSet ds;
    
    /**
     * Memento for x axis state last time updatePlotImage was called.
     */
    private DasAxis.Memento xmemento;
    
    /**
     * Memento for y axis state last time updatePlotImage was called.
     */
    private DasAxis.Memento ymemento;
    
    /**
     * plot containing this renderer
     */
    DasPlot parent;
    //DasPlot2 parent2;
    
    /**
     * the responsibility of keeping a relevant dataset loaded.  Can be null
     * if a loading mechanism is not used.  The DataLoader will be calling
     * setDataSet and setException.
     */
    DataLoader loader;
    
    /**
     * When a dataset cannot be loaded, the exception causing the failure
     * will be rendered instead.
     */
    protected Exception lastException;
    
    /**
     * This is the exception to be rendered.  This is so if an exception occurs during drawing, then this will be drawn instead.
     */
    protected Exception renderException;
    
    protected Logger logger= DasLogger.getLogger( DasLogger.GRAPHICS_LOG );        

    private String PROPERTY_ACTIVE="active";

    private String PROPERTY_DATASET= "dataset";
    
    protected Renderer( DataSetDescriptor dsd ) {
        this.loader= new XAxisDataLoader( this, dsd );
    }
    
    protected Renderer( DataSet ds ) {
        this.ds= ds;
        this.loader= null;
    }
    
    protected Renderer() {
        this((DataSetDescriptor)null);
    }
    
    public DasPlot getParent() { return this.parent; }
    
    protected void invalidateParentCacheImage() {
        if ( parent!=null ) parent.invalidateCacheImage();
    }
    
    /**
     * returns the current dataset being displayed.
     */
    public DataSet getDataSet() { return this.ds; }
    
    /**
     * return the data for DataSetConsumer, which might be rebinned.
     */
    public DataSet getConsumedDataSet() { return this.ds; }
    
    private boolean dumpDataSet;
    
    /** Getter for property dumpDataSet.
     * @return Value of property dumpDataSet.
     *
     */
    public boolean isDumpDataSet() {
        return this.dumpDataSet;
    }
    
    /** Setter for property dumpDataSet setting this to
     * true causes the dataSet to be dumped.
     * @param dumpDataSet New value of property dumpDataSet.
     *
     */
    public void setDumpDataSet(boolean dumpDataSet) {
        this.dumpDataSet= dumpDataSet;
        if ( dumpDataSet==true ) {
            try {
                if ( ds==null ) {
                    setDumpDataSet(false);
                    throw new DasException("data set is null");
                } else {
                    JFileChooser chooser= new JFileChooser();
                    int xx= chooser.showSaveDialog(this.getParent());
                    if ( xx==JFileChooser.APPROVE_OPTION ) {
                        File file= chooser.getSelectedFile();
                        if ( ds instanceof TableDataSet ) {
                            TableUtil.dumpToAsciiStream((TableDataSet)ds, new FileOutputStream(file) );
                        } else if ( ds instanceof VectorDataSet ) {
                            VectorUtil.dumpToAsciiStream((VectorDataSet)ds, new FileOutputStream(file) );
                        } else {
                            throw new DasException("don't know how to serialize data set: "+ds );
                        }
                    }
                    setDumpDataSet( false );
                }
            } catch ( Exception e ) {
                DasExceptionHandler.handle( e );
            }
            this.dumpDataSet= dumpDataSet;
        }
    }
    
    public void setLastException( Exception e ) {
        this.lastException= e;
        this.renderException= lastException;
    }
    
    public Exception getLastException() {
        return this.lastException;
    }
    
    public void setDataSet(DataSet ds) {
        logger.finer("Renderer.setDataSet: "+ds);
        DataSet oldDs= this.ds;
        
        if ( oldDs!=ds ) {
            this.ds= ds;
            refresh();
            invalidateParentCacheImage();
            propertyChangeSupport.firePropertyChange( PROPERTY_DATASET, oldDs, ds );
        }
    }
    
    public void setException( Exception e ) {
        logger.finer("Renderer.setException: "+e);
        Exception oldException= this.lastException;
        this.lastException= e;
        this.renderException= lastException;
        if ( parent!=null && oldException!=e ) {
            //parent.markDirty();
            //parent.update();
            refresh();
            invalidateParentCacheImage();
        }
        //refresh();
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
        if ( getDataSetDescriptor()==null ) {
            return "";
        } else {
            return getDataSetDescriptor().getDataSetID();
        }
    }
    
    
    
    /*
     * returns the AffineTransform to transform data from the last updatePlotImage call
     * axes (if super.updatePlotImage was called), or null if the transform is not possible.
     *
     * @depricated DasPlot handles the affine transform and previews now.
     */
    protected AffineTransform getAffineTransform( DasAxis xAxis, DasAxis yAxis ) {
        if ( xmemento==null ) {
            logger.fine( "unable to calculate AT, because old transform is not defined." );
            return null;
        } else {
            AffineTransform at= new AffineTransform();
            at= xAxis.getAffineTransform(xmemento,at);
            at= yAxis.getAffineTransform(ymemento,at);
            return at;
        }
    }
    
    /** Render is called whenever the image needs to be refreshed or the content
     * has changed.  This operation should occur with an animation-interactive
     * time scale, and an image should be cached when this is not possible.  The graphics
     * object will have its origin at the upper-left corner of the screen.
     */
    public abstract void render(Graphics g, DasAxis xAxis, DasAxis yAxis, DasProgressMonitor mon);
    
    /**
     * x,y in the canvas coordinate system.
     */
    public boolean acceptContext( int x, int y ) {
        return false;
    }
    
    protected void renderException( Graphics g, DasAxis xAxis, DasAxis yAxis, Exception e ) {
        
        String s;
        String message;
        FontMetrics fm= g.getFontMetrics();
        
        if ( e instanceof NoDataInIntervalException ) {
            s= "no data in interval";
            message= e.getMessage();
        } else {
            s= e.getMessage();
            message= "";
            if ( s == null || s.length() < 10 ) {
                s= e.toString();
            }
        }
        
        if ( !message.equals("") ) {
            s+= ":!c"+message;
        }
        
        parent.postMessage( this, s, DasPlot.ERROR, null, null );
        
    }
    
    /** updatePlotImage is called once the expensive operation of loading
     * the data is completed.  This operation should occur on an interactive
     * time scale.  This is an opportunity to create a cache
     * image of the data with the current axis state, when the render
     * operation cannot operate on an animation interactive time scale.
     * Codes can no longer assume that the xAxis sent to render will be in
     * the same state as it was when updatePlotImage was called, so use
     * the getAffineTransform method.  Only Renderer should call this method!
     */
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, DasProgressMonitor monitor) throws DasException {
    }
    
    protected void refreshImage() {
        if ( getParent()!=null ) {
            refresh();
        }
    }
    
    public void update() {
        if ( getParent()!=null ) getParent().repaint();
        logger.fine("Renderer.update");
        if (parent != null) {
            java.awt.EventQueue eventQueue =
                    Toolkit.getDefaultToolkit().getSystemEventQueue();
            DasRendererUpdateEvent drue = new DasRendererUpdateEvent(parent, this);
            eventQueue.postEvent(drue);
        } else {
            logger.fine("update but parent was null");
        }
    }
    
    /**
     * updateImmediately is called from DasPlot when it gets an update event from the
     * AWT Event thread.  This should trigger a data load and eventually a refresh to
     * render the dataset.
     */
    protected void updateImmediately() {
        logger.finer("entering Renderer.updateImmediately");
        if (parent == null || !parent.isDisplayable() ) {
            return;
        }
        
        // If there's a loader, then tell him he might want to load new data.
        if ( loader!=null ) {
            loader.update();
        }
        
        // The parent has already used an AffineTransform to preview the image, but
        // we might as well re-render using the dataset we have.
        refresh();
    }
    
    /**
     * recalculate the plot image and repaint.  The dataset or exception have
     * been updated, or the axes have changed, so we need to perform updatePlotImage
     * to do the expensive parts of rendering.
     */
    protected void refresh() {
        if ( !isActive() ) return;
        
        logger.fine("entering Renderer.refresh");
        if ( parent==null ) {
            logger.fine("null parent in refresh");
            return;
        }
        if ( !parent.isDisplayable() ) {
            logger.fine("parent not displayable");
            return;
        }
        
        Runnable run= new Runnable() {
            public void run() {
                logger.fine("update plot image");
                try {
                    final DasProgressMonitor progressPanel= DasApplication.getDefaultApplication().getMonitorFactory().getMonitor(parent, "Rebinning data set", "updatePlotImage" );
                    updatePlotImage( parent.getXAxis(), parent.getYAxis(), progressPanel );
                    xmemento= parent.getXAxis().getMemento();
                    ymemento= parent.getYAxis().getMemento();
                    renderException= null;
                } catch ( DasException de ) {
                    // TODO: there's a problem here, that the Renderer can set its own exeception and dataset.  This needs to be addressed, or handled as an invalid state.
                    logger.warning("exception: "+de);
                    ds = null;
                    renderException = de;
                } catch (RuntimeException re) {
                    logger.warning("exception: "+re);
                    renderException= re;
                    parent.invalidateCacheImage();
                    parent.repaint();
                    throw re;
                } finally {
                    // this code used to call finished() on the progressPanel
                }
                
                logger.fine("invalidate parent cacheImage and repaint");
                
                parent.invalidateCacheImage();
                parent.repaint();
            }
        };
        
        //if ( EventQueue.isDispatchThread() ) {
        //    new Thread( run, "updatePlotImage" ).start();
        //} else {
        run.run();
        //}
    }
    
    
    public void setDataSetDescriptor(DataSetDescriptor dsd) {
        if ( loader==null ) {
            logger.warning("installing loader--danger!");
            loader= new XAxisDataLoader( this, dsd );
        }
        if ( loader instanceof XAxisDataLoader ) {
            ((XAxisDataLoader)loader).setDataSetDescriptor(dsd);
            if (parent != null) {
                parent.markDirty();
                parent.update();
            }
            this.ds=null;
        } else {
            throw new RuntimeException("loader is not based on DataSetDescriptor");
        }
        
    }
    
    public DataLoader getDataLoader() {
        return this.loader;
    }
    
    public void setDataSetLoader( DataLoader loader ) {
        this.loader= loader;
        if ( loader!=null ) loader.update();
    }
    
    public DataSetDescriptor getDataSetDescriptor() {
        if ( loader==null ) {
            return null;
        } else {
            if ( this.loader instanceof XAxisDataLoader ) {
                return ((XAxisDataLoader)loader).getDataSetDescriptor();
            } else {
                return null;
            }
        }
    }
    
    protected abstract void installRenderer();
    
    protected abstract void uninstallRenderer();
    
    protected abstract Element getDOMElement( Document document );
    
    private boolean overloading=false;
    
    public boolean isOverloading() {
        return this.overloading;
    }
    public void setOverloading(boolean overloading) {
        this.overloading = overloading;
        update();
    }
    
    /**
     * Holds value of property active.
     */
    private boolean active= true;
    
    /**
     * Getter for property active.
     * @return Value of property active.
     */
    public boolean isActive() {
        return this.active;
    }
    
    /**
     * Setter for property active.
     * @param active New value of property active.
     */
    public void setActive(boolean active) {
        boolean oldValue= this.active;
        this.active = active;
        propertyChangeSupport.firePropertyChange( PROPERTY_ACTIVE, oldValue, active );
        update();
    }

    /**
     * Utility field used by bound properties.
     */
    protected java.beans.PropertyChangeSupport propertyChangeSupport =  new java.beans.PropertyChangeSupport(this);

    /**
     * Adds a PropertyChangeListener to the listener list.
     * @param l The listener to add.
     */
    public void addPropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }

    /**
     * Removes a PropertyChangeListener from the listener list.
     * @param l The listener to remove.
     */
    public void removePropertyChangeListener(java.beans.PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }

}
