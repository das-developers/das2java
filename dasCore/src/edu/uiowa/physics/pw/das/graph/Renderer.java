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
import edu.uiowa.physics.pw.das.util.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.system.*;
import java.awt.geom.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.logging.Logger;
import org.w3c.dom.*;

public abstract class Renderer implements DataSetConsumer, Editable {

    // dsd reloads ds when plot params change.
    //  private DataSetDescriptor dsd;
    String dataSetId;

    // avoid get/set methods unless you know what you're doing.
    protected DataSet ds;

    private DasAxis.Memento xmemento;
    private DasAxis.Memento ymemento;

    DasPlot parent;
    DataLoader loader;

    protected Exception lastException;

    protected Logger logger= DasLogger.getLogger( DasLogger.GRAPHICS_LOG );

    protected Renderer( DataSetDescriptor dsd ) {
        // this.dsd = dsd;
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
    }

    public Exception getLastException() {
        return this.lastException;
    }

    public void setDataSet(DataSet ds) {
        logger.finer("Renderer.setDataSet: "+ds);
        this.ds= ds;
        refresh();
        invalidateParentCacheImage();
    }

    public void setException( Exception e ) {
        logger.finer("Renderer.setException: "+e);
        this.lastException= e;
        refresh();
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
            DasApplication.getDefaultApplication().getLogger( DasApplication.GRAPHICS_LOG )
            .fine( "unable to calculate AT, because old transform is not defined." );
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
     * time scale, and an image should be cached when this is not possible.
     */
    public abstract void render(Graphics g, DasAxis xAxis, DasAxis yAxis);

    protected void renderException( Graphics g, DasAxis xAxis, DasAxis yAxis, Exception e ) {

        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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

    /*
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

        final DasProgressMonitor progressPanel= DasApplication.getDefaultApplication().getMonitorFactory().getMonitor(parent, "Rebinning data set", "updatePlotImage" );

        Runnable run= new Runnable() {
            public void run() {
                logger.fine("update plot image");
                try {
                    updatePlotImage( parent.getXAxis(), parent.getYAxis(), progressPanel );
                    xmemento= parent.getXAxis().getMemento();
                    ymemento= parent.getYAxis().getMemento();
                    parent.repaint();
                } catch ( DasException de ) {
                    logger.warning("exception: "+de);
                    ds = null;
                    lastException = de;
                } catch (RuntimeException re) {
                    ds = null;
                    throw re;
                } finally {
                    if (progressPanel != null) {
                        logger.fine("progressPanel.finished()");
                        progressPanel.finished();
                    }
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
            ds = null;
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
        this.active = active;
        update();
    }

}
