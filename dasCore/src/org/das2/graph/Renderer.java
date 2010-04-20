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
package org.das2.graph;

import org.das2.dataset.NoDataInIntervalException;
import org.das2.dataset.DataSetConsumer;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.VectorUtil;
import org.das2.dataset.TableDataSet;
import org.das2.dataset.DataSet;
import org.das2.dataset.TableUtil;
import org.das2.dataset.VectorDataSet;
import org.das2.DasApplication;
import org.das2.DasException;
import org.das2.util.DasExceptionHandler;
import java.beans.PropertyChangeListener;
import org.das2.util.monitor.ProgressMonitor;
import org.das2.components.propertyeditor.Editable;
import org.das2.system.DasLogger;
import java.awt.geom.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.logging.Logger;
import org.das2.components.propertyeditor.Displayable;
import org.das2.dataset.DataSetUtil;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;

public abstract class Renderer implements DataSetConsumer, Editable, Displayable {

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

    /**
     * keep track of first and last valid points of the dataset to simplify
     * subclass code and allow them to check if there are any valid points.
     */
    protected int firstValidIndex=-1;
    protected int lastValidIndex=-1;

    protected static Logger logger = DasLogger.getLogger(DasLogger.RENDERER_LOG);
    private String PROPERTY_ACTIVE = "active";
    private String PROPERTY_DATASET = "dataSet";

    protected Renderer(DataSetDescriptor dsd) {
        this.loader = new XAxisDataLoader(this, dsd);
    }

    protected Renderer(DataSet ds) {
        this.ds = ds;
        this.loader = null;
    }

    protected Renderer() {
        this((DataSetDescriptor) null);
    }

    public DasPlot getParent() {
        return this.parent;
    }

    /**
     * find the first and last valid data points.  This is an inexpensive
     * calculation which is only done when the dataset changes.  It improves
     * update and render codes by allowing them to skip initial fill data
     * and more accurately report the presence of off-screen valid data.
     * preconditions: setDataSet is called with null or non-null dataset.
     * postconditions: firstValid and lastValid are set.  In the case of a
     * null dataset, firstValid and lastValid are set to 0.
     */
    private void updateFirstLastValid() {
        if ( ds==null ) {
            firstValidIndex=0;
            lastValidIndex=0;
            return;
        } else {
            if (ds instanceof TableDataSet) {
                firstValidIndex= 0;
                lastValidIndex= ds.getXLength();
            } else {
                VectorDataSet vds= (VectorDataSet)ds;
                Units u= vds.getYUnits();
                firstValidIndex= -1;
                lastValidIndex= -1;
                for ( int i=0; firstValidIndex==-1 && i<ds.getXLength(); i++ ) {
                    if ( !u.isFill(i) ) firstValidIndex=i;
                }
                for ( int i=ds.getXLength()-1; lastValidIndex==-1 && i>=0; i-- ) {
                    if ( !u.isFill(i) ) lastValidIndex=i+1;
                }
            }
        }
    }

    protected void invalidateParentCacheImage() {
        if (parent != null) parent.invalidateCacheImage();
    }

    /**
     * returns the current dataset being displayed.
     */
    public DataSet getDataSet() {
        return this.ds;
    }

    /**
     * return the data for DataSetConsumer, which might be rebinned.
     */
    public DataSet getConsumedDataSet() {
        return this.ds;
    }
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
        this.dumpDataSet = dumpDataSet;
        if (dumpDataSet == true) {
            try {
                if (ds == null) {
                    setDumpDataSet(false);
                    throw new DasException("data set is null");
                } else {
                    JFileChooser chooser = new JFileChooser();
                    int xx = chooser.showSaveDialog(this.getParent());
                    if (xx == JFileChooser.APPROVE_OPTION) {
                        File file = chooser.getSelectedFile();
                        if (ds instanceof TableDataSet) {
                            TableUtil.dumpToAsciiStream((TableDataSet) ds, new FileOutputStream(file));
                        } else if (ds instanceof VectorDataSet) {
                            VectorUtil.dumpToAsciiStream((VectorDataSet) ds, new FileOutputStream(file));
                        } else {
                            throw new DasException("don't know how to serialize data set: " + ds);
                        }
                    }
                    setDumpDataSet(false);
                }
            } catch (Exception e) {
                DasExceptionHandler.handle(e);
            }
            this.dumpDataSet = dumpDataSet;
        }
    }

    public void setLastException(Exception e) {
        this.lastException = e;
        this.renderException = lastException;
    }

    public Exception getLastException() {
        return this.lastException;
    }

    public void setDataSet(DataSet ds) {
        logger.fine("Renderer.setDataSet "+id+": " + ds);

        DataSet oldDs = this.ds;

        if (oldDs != ds) {
            synchronized(this) {
                updateFirstLastValid();
                this.ds = ds;
            }
            refresh();
            //update();
            invalidateParentCacheImage();
            propertyChangeSupport.firePropertyChange(PROPERTY_DATASET, oldDs, ds);
        }
    }

    public void setException(Exception e) {
        logger.fine("Renderer.setException: " + e);
        Exception oldException = this.lastException;
        this.lastException = e;
        this.renderException = lastException;
        if (parent != null && oldException != e) {
            //parent.markDirty();
            //parent.update();
            update();
            //refresh();
            invalidateParentCacheImage();
        }
    //refresh();
    }

    public void setDataSetID(String id) throws org.das2.DasException {
        if (id == null) throw new NullPointerException("Null dataPath not allowed");
        if (id.equals("")) {
            setDataSetDescriptor(null);
            return;
        }
        try {
            DataSetDescriptor dsd = DataSetDescriptor.create(id);
            setDataSetDescriptor(dsd);
        } catch (DasException ex) {
            ex.printStackTrace();
            throw ex;
        }
    }

    public String getDataSetID() {
        if (getDataSetDescriptor() == null) {
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
    protected AffineTransform getAffineTransform(DasAxis xAxis, DasAxis yAxis) {
        if (xmemento == null) {
            logger.fine("unable to calculate AT, because old transform is not defined.");
            return null;
        } else {
            AffineTransform at = new AffineTransform();
            at = xAxis.getAffineTransform(xmemento, at);
            at = yAxis.getAffineTransform(ymemento, at);
            return at;
        }
    }

    /** Render is called whenever the image needs to be refreshed or the content
     * has changed.  This operation should occur with an animation-interactive
     * time scale, and an image should be cached when this is not possible.  The graphics
     * object will have its origin at the upper-left corner of the screen.
     */
    public abstract void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon);

    /**
     * Returns true if the render thinks it can provide the context for a point.  That is,
     * the renderer affected that point, or nearby points.  For example, this is used currently to provide
     * a way for the operator to click on a plot and directly edit the renderer who drew the pixel.
     *
     * @param x the x coordinate in the canvas coordinate system.  
     * @param y the y coordinate in the canvas coordinate system.  
     */
    public boolean acceptContext(int x, int y) {
        return false;
    }

    protected void renderException(Graphics g, DasAxis xAxis, DasAxis yAxis, Exception e) {

        String s;
        String message;
        FontMetrics fm = g.getFontMetrics();

        if (e instanceof NoDataInIntervalException) {
            s = "no data in interval";
            message = e.getMessage();
        } else {
            s = e.getMessage();
            message = "";
            if (s == null || s.length() < 10) {
                s = e.toString();
            }
        }

        if (!message.equals("")) {
            s += ":!c" + message;
        }

        parent.postMessage(this, s, DasPlot.ERROR, null, null);

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
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
    }

    protected void refreshImage() {
        if (getParent() != null) {
            refresh();
        }
    }

    /**
     * Something has changed with the Render, and the plot should come back
     * to allow this render to repaint.  Its cacheImage is invalidated and a
     * repaint is posted on the event thread.
     */
    public void update() {
        if (getParent() != null) getParent().repaint();
        logger.fine("Renderer.update "+id);
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
        if (parent == null || !parent.isDisplayable()) {
            return;
        }

        // If there's a loader, then tell him he might want to load new data.
        if (loader != null) {
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
        if (!isActive()) return;

        logger.fine("entering Renderer.refresh");
        if (parent == null) {
            logger.fine("null parent in refresh");
            return;
        }
        if (!parent.isDisplayable()) {
            logger.fine("parent not displayable");
            return;
        }

        Runnable run = new Runnable() {

            public void run() {
                logger.fine("update plot image for "+id);
                DasPlot lparent= parent;
                try {
                    if (lparent != null) {
                        final ProgressMonitor progressPanel = DasApplication.getDefaultApplication().getMonitorFactory().getMonitor(parent, "Rebinning data set", "updatePlotImage");
                        updatePlotImage(lparent.getXAxis(), lparent.getYAxis(), progressPanel);
                        xmemento = lparent.getXAxis().getMemento();
                        ymemento = lparent.getYAxis().getMemento();
                        renderException = null;
                    } else {
                        return;
                    }
                } catch (DasException de) {
                    // TODO: there's a problem here, that the Renderer can set its own exception and dataset.  This needs to be addressed, or handled as an invalid state.
                    logger.warning("exception: " + de);
                    ds = null;
                    renderException = de;
                } catch (RuntimeException re) {
                    logger.warning("exception: " + re);
                    re.printStackTrace();
                    renderException = re;
                    if ( lparent!=null ) {
                        lparent.invalidateCacheImage();
                        lparent.repaint();
                    }
                    throw re;
                } finally {
                    // this code used to call finished() on the progressPanel
                }

                logger.fine("invalidate parent cacheImage and repaint");

                lparent.invalidateCacheImage();
                lparent.repaint();
            }
        };

        boolean async = false;  // updating is done on the event thread...
        if (EventQueue.isDispatchThread()) {
            if (async) {
                new Thread(run, "updatePlotImage").start();
            } else {
                run.run();
            }
        } else {
            run.run();
        }
    }

    public void setDataSetDescriptor(DataSetDescriptor dsd) {
        if (loader == null) {
            logger.warning("installing loader--danger!");
            loader = new XAxisDataLoader(this, dsd);
        }
        if (loader instanceof XAxisDataLoader) {
            ((XAxisDataLoader) loader).setDataSetDescriptor(dsd);
            if (parent != null) {
                parent.markDirty();
                parent.update();
            }
            this.ds = null;
        } else {
            throw new RuntimeException("loader is not based on DataSetDescriptor");
        }

    }

    public DataLoader getDataLoader() {
        return this.loader;
    }

    public void setDataSetLoader(DataLoader loader) {
        this.loader = loader;
        if (loader != null) loader.update();
    }

    public DataSetDescriptor getDataSetDescriptor() {
        if (loader == null) {
            return null;
        } else {
            if (this.loader instanceof XAxisDataLoader) {
                return ((XAxisDataLoader) loader).getDataSetDescriptor();
            } else {
                return null;
            }
        }
    }

    protected void installRenderer() {
        // override me
    }

    protected void uninstallRenderer() {
        // override me
    }

    private boolean overloading = false;

    public boolean isOverloading() {
        return this.overloading;
    }

    public void setOverloading(boolean overloading) {
        this.overloading = overloading;
        update();
    }


    /**
     * display the renderer.  This is allows a renderer to be disabled without removing it from the application.
     */
    public static final String PROP_ACTIVE = "active";

    private boolean active = true;

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        boolean oldValue = this.active;
        this.active = active;
        if ( oldValue!=active ) update();
        propertyChangeSupport.firePropertyChange(PROPERTY_ACTIVE, oldValue, active);
    }

     /**
     * If non-null and non-zero-length, use this label to describe the renderer
     * in the plot's legend.
     */
    public static final String PROP_LEGENDLABEL = "legendLabel";

    protected String legendLabel = "";

    public String getLegendLabel() {
        return legendLabel;
    }

    public void setLegendLabel(String legendLabel) {
        String oldLegendLabel = this.legendLabel;
        this.legendLabel = legendLabel;
        propertyChangeSupport.firePropertyChange(PROP_LEGENDLABEL, oldLegendLabel, legendLabel);
        refreshImage();
    }

    protected boolean drawLegendLabel = false;
    public static final String PROP_DRAWLEGENDLABEL = "drawLegendLabel";

    public boolean isDrawLegendLabel() {
        return drawLegendLabel;
    }

    public void setDrawLegendLabel(boolean drawLegendLabel) {
        boolean oldDrawLegendLabel = this.drawLegendLabel;
        this.drawLegendLabel = drawLegendLabel;
        propertyChangeSupport.firePropertyChange(PROP_DRAWLEGENDLABEL, oldDrawLegendLabel, drawLegendLabel);
        refreshImage();
    }

    protected String id = "rend";
    public static final String PROP_ID = "id";

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String oldId = this.id;
        this.id = id;
        propertyChangeSupport.firePropertyChange(PROP_ID, oldId, id);
    }

    /**
     * return a 16x16 icon representing the renderer.  Subclasses that do not override this
     * will have an empty icon displayed.
     * @return
     */
    public Icon getListIcon() {
        return new ImageIcon( new BufferedImage( 16, 16, BufferedImage.TYPE_INT_ARGB ) );
    }

    /**
     * return a short label for the renderer.
     * @return
     */
    public String getListLabel() {
        StringBuffer l= new StringBuffer( getLegendLabel() );
        if ( this.getDataSetDescriptor()!=null ) {
            if ( l.length()>0 ) {
                l.append( " ("+this.getDataSetDescriptor() +")" );
            }
        }
        if ( l.length()==0 ) {
            l.append( this.getClass().getName() );
        }
        return l.toString();
    }


    /**
     * Utility field used by bound properties.
     */
    protected java.beans.PropertyChangeSupport propertyChangeSupport = new java.beans.PropertyChangeSupport(this);

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

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }
}
