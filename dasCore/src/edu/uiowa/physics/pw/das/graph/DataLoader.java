/*
 * DataLoader.java
 *
 * Created on September 13, 2005, 6:41 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.RebinDescriptor;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;

/**
 *
 * @author Jeremy
 */
public abstract class DataLoader {
    
    Renderer renderer;
    
    protected DasProgressMonitor getMonitor() {
        return DasProgressPanel.createComponentPanel( renderer.getParent(), "Loading data set" );
    }
    
    protected DataLoader( Renderer renderer ) {
        this.renderer= renderer;
    }
    
    // an update message that something might have changed
    abstract public void update();
    
    private boolean fullResolution = false;
    public boolean isFullResolution() {
        return fullResolution;
    }
    
    public void setFullResolution(boolean b) {
        if (fullResolution == b) return;
        fullResolution = b;
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
    
    public class Request {
        public DasProgressMonitor monitor;
        public DasAxis.Memento xmem;
        public DasAxis.Memento ymem;
        public Request( DasProgressMonitor mon, DasAxis.Memento xmem, DasAxis.Memento ymem ) {
            this.monitor= mon;
            this.xmem= xmem;
            this.ymem= ymem;
        }
        public String toString() {
            return xmem.toString();
        }
    }
    
    /**
     * convenient method for getting a rebindescriptor with one bin per pixel.  -1 is
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
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
        update();
    }
    
}
