/*
 * DataLoader.java
 *
 * Created on September 13, 2005, 6:41 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package org.das2.graph;
import org.das2.DasApplication;
import org.das2.dataset.RebinDescriptor;
import org.das2.util.monitor.ProgressMonitor;

/**
 *
 * @author Jeremy
 */
public abstract class DataLoader {
    
    Renderer renderer;
    
    protected ProgressMonitor getMonitor( String description ) {
        return DasApplication.getDefaultApplication().getMonitorFactory()
          .getMonitor( renderer.getParent(), "Loading data set", description );        
    }
    
    protected DataLoader( Renderer renderer ) {
        this.renderer= renderer;
    }
    
    /**
     * an update message sent by the Renderer to indicate that something might have changed.  
     * Presently, the axis will send an update message to the plot, the plot will send an
     * update message to the renderer, who will send an update message to the loader.  
     * THIS WILL PROBABLY CHANGE.
     */
    abstract public void update();
    
    private boolean fullResolution = false;
    public boolean isFullResolution() {
        return fullResolution;
    }
    
    public void setFullResolution(boolean b) {
        if (fullResolution == b) return;
        fullResolution = b;
        update();
    }
    
    // reloadDataSet is a dummy property that is Jeremy's way of telling the thing to
    // reload through the property editor.  calling setReloadDataSet(true) causes the
    // dataset to reload and the image to be redrawn.
    private boolean reloadDataSet;
    public void setReloadDataSet( boolean reloadDataSet ) {
        if ( reloadDataSet ) {
            renderer.setDataSet( null );
            renderer.getParent().markDirty();
            renderer.getParent().update();
            
        }
        reloadDataSet= false;
    }
    
    public boolean isReloadDataSet() {
        return this.reloadDataSet;
    }
    
    protected Renderer getRenderer() {
        return this.renderer;
    }
    
    public static class Request {
        public ProgressMonitor monitor;
        public DasAxis.Memento xmem;
        public DasAxis.Memento ymem;
        public Request( ProgressMonitor mon, DasAxis.Memento xmem, DasAxis.Memento ymem ) {
            this.monitor= mon;
            this.xmem= xmem;
            this.ymem= ymem;
        }
        @Override
        public String toString() {
            return xmem.toString() + " " +ymem.toString();
        }
    }
    
    /**
     * convenient method for getting a RebinDescriptor with one bin per pixel.  -1 is
     * returned by the rebinDescriptor when no bin holds the point.
     */
    protected RebinDescriptor getRebinDescriptor( DasAxis axis ) {
        int npix;
        if ( axis.getOrientation()==DasAxis.VERTICAL ) {
            npix= axis.getRow().getHeight();
        } else {
            npix= axis.getColumn().getWidth();
        }
        
        RebinDescriptor rebinDescriptor;
        rebinDescriptor = new RebinDescriptor(
                axis.getDataMinimum(), axis.getDataMaximum(),
                npix,
                axis.isLog());
        rebinDescriptor.setOutOfBoundsAction( RebinDescriptor.MINUSONE );
        return rebinDescriptor;
    }

    /**
     * If active is false, then no data loading should occur.
     */
    private boolean active= true;

   
    public boolean isActive() {
        return this.active && renderer.isActive();
    }

    public void setActive(boolean active) {
        this.active = active;
        update();
    }
    
}
