/*
 * DataLoader.java
 *
 * Created on September 10, 2005, 5:28 AM
 *
 * Remove the data loading responsibilities from the Renderer, and introduce
 * pluggable strategies for data loading.
 *
 */

package edu.uiowa.physics.pw.das.graph;

import edu.uiowa.physics.pw.das.CancelledOperationException;
import edu.uiowa.physics.pw.das.DasApplication;
import edu.uiowa.physics.pw.das.components.DasProgressPanel;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.DataSetUpdateEvent;
import edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener;
import edu.uiowa.physics.pw.das.dataset.DataSetUtil;
import edu.uiowa.physics.pw.das.dataset.NoDataInIntervalException;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.stream.StreamException;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.util.DasExceptionHandler;
import edu.uiowa.physics.pw.das.util.DasProgressMonitor;
import java.awt.Component;
import java.awt.Container;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Jeremy
 */
public class XAxisDataLoader extends DataLoader implements DataSetUpdateListener  {
    
    DasAxis xaxis;
    DataSetDescriptor dsd;
    DasProgressMonitor progressMonitor;
    Logger logger;
    
    Request currentRequest;
    List unsolicitedRequests;
    
    /** Creates a new instance of DataLoader */
    public XAxisDataLoader( Renderer r, DataSetDescriptor dsd ) {
        super(r);
        this.dsd= dsd;
        this.logger= DasLogger.getLogger( DasLogger.GRAPHICS_LOG );
        if ( dsd!=null ) dsd.addDataSetUpdateListener( this );
    }
    
    public void update() {
        if ( isActive() ) {
            DasPlot p= getRenderer().getParent();
            if ( p==null ) {
                DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("plot is null, no need to load");
            } else {
                DasAxis xAxis = p.getXAxis();
                DasAxis yAxis = p.getYAxis();
                loadDataSet( xAxis, yAxis );
            }
        }
    }
    
    /* requests a reload of data, indicating its current data set in case it's
     * suitable.
     */
    private void loadDataSet( DasAxis xAxis, DasAxis yAxis ) {
        DasLogger.getLogger( DasLogger.GRAPHICS_LOG ).fine( "render requests dataset for x:"+xAxis.getMemento() + " y:"+yAxis.getMemento());
        if ( xaxis==null ) this.xaxis= xAxis;
        if ( dsd==null ) {
            DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("dsd is null, nothing to do");
            return;
        }
        
        if ( currentRequest!=null ) {
            if ( ! xAxis.getMemento().equals( currentRequest.xmem ) ) {
                logger.fine( "cancel old request" );
                currentRequest.monitor.cancel();
            } else {
                logger.fine( "ignore repeat request" );
                return; // ignore the repeated request
            }
        }
        
        Datum resolution;
        Datum dataRange1 = xAxis.getDataMaximum().subtract(xAxis.getDataMinimum());
        
        double deviceRange = Math.floor(xAxis.getColumn().getDMaximum() + 0.5) - Math.floor(xAxis.getColumn().getDMinimum() + 0.5);
        if ( isFullResolution() ) {
            resolution = null;
        } else {
            resolution =  dataRange1.divide(deviceRange);
        }
        
        if ( deviceRange==0.0 ) {
            // this condition occurs sometimes at startup, it's not known why
            return;
        }
        
        DasPlot parent= renderer.getParent();
        
        progressMonitor = getMonitor();
        
        parent.paintImmediately( 0, 0, parent.getWidth(), parent.getHeight() );
        
        DatumRange loadRange= xAxis.getDatumRange();
        //if ( renderer.isOverloading() ) loadRange= loadRange.rescale(-1,2);
        DasApplication.getDefaultApplication().getLogger(DasApplication.GRAPHICS_LOG).info("request data from dsd: "+loadRange+" @ "+resolution);
        
        currentRequest= new Request( progressMonitor, xAxis.getMemento(), yAxis.getMemento() );
        
        dsd.requestDataSet( loadRange.min(), loadRange.max(), resolution, progressMonitor, parent.getCanvas() );
        // the request will come back with a DataSetUpdated event
    }
    
        /*
         * If an exception is handled by the Renderer putting the exception in place of the data,
         * then return true here.  If the exception is more exceptional and we really need to get
         * user's attention, return false.
         */
    private boolean rendererHandlesException( Exception e ) {
        boolean result=
                e instanceof InterruptedIOException ||
                e instanceof NoDataInIntervalException ||                
                e instanceof CancelledOperationException ;
        return result;
    }
    
    public void dataSetUpdated( DataSetUpdateEvent e ) {
        //updateImmediately();
        
        DasApplication.getDefaultApplication().getLogger(DasApplication.GRAPHICS_LOG).info("got dataset update:"+e);
        // TODO make sure Exception is cleared--what if data set is non-null but Exception is as well?
        if ( e.getException()!=null && e.getDataSet()!=null ) {
            throw new IllegalStateException("both exception and data set");
        } else if (e.getException() != null) {
            DasApplication.getDefaultApplication().getLogger(DasApplication.GRAPHICS_LOG).info("got dataset update exception: "+e.getException());
            Exception exception = e.getException();
            if ( !rendererHandlesException(exception) ) {
                DasExceptionHandler.handle(exception);
            }
                        
            renderer.setException( exception );
            renderer.setDataSet(null);
            
            logger.fine("current request completed w/exception: " + currentRequest );
            
            if (!(exception instanceof InterruptedIOException) &&
                    !( ( exception instanceof StreamException) && 
                    (!( ((StreamException)exception).getCause() instanceof InterruptedIOException ) ) ) ) {
                if ( !rendererHandlesException(exception)  ) {
                    DasExceptionHandler.handle(exception);
                }
            }
            
        } else if ( e.getDataSet()==null ) {
            // this indicates that the DataSetDescriptor has changed, and that the
            // renderer needs to reread the data.  Cause this by invalidating the
            // component.
            logger.info("got dataset update notification (no dataset).");
            loadDataSet( renderer.getParent().getXAxis(), renderer.getParent().getYAxis() );
            return;
        } else {
            DataSet ds= e.getDataSet();
            logger.info("got dataset update w/dataset: "+ds);
            if ( ds!=null ) {
                if ( ds.getXLength()>0 ) {
                    DasApplication.getDefaultApplication().getLogger(DasApplication.GRAPHICS_LOG).info("  ds range: "+DataSetUtil.xRange(ds) );
                } else {
                    DasApplication.getDefaultApplication().getLogger(DasApplication.GRAPHICS_LOG).info("  ds range: (empty)" );
                }
            }
            renderer.setDataSet( ds );
        }
        
        if ( currentRequest!=null ) {
            logger.fine("current request completed w/dataset: " + currentRequest.xmem );
        } else {
            logger.fine( "this is an unexpected state!" );
        }
        currentRequest=null;
            
    }
    
    
    public void setDataSetDescriptor( DataSetDescriptor dsd ) {
        DasLogger.getLogger(DasLogger.GRAPHICS_LOG).fine("set dsd: "+dsd);
        if ( this.dsd!=null ) this.dsd.removeDataSetUpdateListener(this);
        this.dsd = dsd;
        if ( dsd!=null ) dsd.addDataSetUpdateListener(this);
        update();
    }
    
    public DataSetDescriptor getDataSetDescriptor() {
        return this.dsd;
    }
    
    public void setReloadDataSet(boolean reloadDataSet) {
        super.setReloadDataSet(reloadDataSet);
        this.dsd.reset();
    }
    
    private boolean fullResolution = false;
    public boolean isFullResolution() {
        return fullResolution;
    }
    
    public void setFullResolution(boolean b) {
        if (fullResolution == b) return;
        fullResolution = b;
    }
    
}
