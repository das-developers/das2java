/*
 * DataLoader.java
 *
 * Created on September 10, 2005, 5:28 AM
 *
 * Remove the data loading responsibilities from the Renderer, and introduce
 * pluggable strategies for data loading.
 *
 */

package org.das2.graph;

import java.util.logging.Level;
import org.das2.CancelledOperationException;
import org.das2.datum.CacheTag;
import org.das2.dataset.DataSet;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.DataSetUpdateEvent;
import org.das2.dataset.DataSetUpdateListener;
import org.das2.dataset.DataSetUtil;
import org.das2.dataset.NoDataInIntervalException;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.graph.DataLoader.Request;
import org.das2.datum.DatumUtil;
import org.das2.stream.StreamException;
import org.das2.system.DasLogger;
import org.das2.util.DasExceptionHandler;
import org.das2.util.monitor.ProgressMonitor;
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.das2.DasApplication;
import org.das2.dataset.DataSetAdapter;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;

/**
 *
 * @author Jeremy
 */
public class XAxisDataLoader extends DataLoader implements DataSetUpdateListener  {
    
    DasAxis xaxis;
    DataSetDescriptor dsd;
    ProgressMonitor progressMonitor;
    private static final Logger logger= DasLogger.getLogger( DasLogger.GRAPHICS_LOG, "XAxisDataLoader" );
    
    Request currentRequest;
    List unsolicitedRequests;
    
    final Object lockObject= new Object();
    
    /** Creates a new instance of DataLoader */
    public XAxisDataLoader( Renderer r, DataSetDescriptor dsd ) {
        super(r);
        this.dsd= dsd;
        if ( dsd!=null ) dsd.addDataSetUpdateListener( this );
        unsolicitedRequests= new ArrayList();
    }
    
    public void update() {
        if ( isActive() ) {
            logger.finer("enter XAxisDataLoader.update");
            DasPlot p= getRenderer().getParent();
            if ( p==null ) {
                logger.fine("plot is null, no need to load");
            } else {
                DasAxis xAxis = p.getXAxis();
                DasAxis yAxis = p.getYAxis();
                if ( xAxis.valueIsAdjusting()==false && yAxis.valueIsAdjusting()==false ) {
                    loadDataSet( xAxis, yAxis );
                }
            }
        } else {
            logger.finer("enter XAxisDataLoader.update, ignored not active");
        }
    }
    
    /* requests a reload of data, indicating its current data set in case it's
     * suitable.
     */
    private void loadDataSet( DasAxis xAxis, DasAxis yAxis ) {
        
        logger.log( Level.FINE, "render requests dataset for x:{0} y:{1}", new Object[]{xAxis.getMemento(), yAxis.getMemento()});
        
        if ( xaxis==null ) this.xaxis= xAxis;
        
        if ( xaxis.getColumn()==DasColumn.NULL ) {
            logger.fine("column not set yet");
            return;
        }
        
        if ( dsd==null ) {
            logger.fine("dsd is null, nothing to do");
            return;
        }
        
        synchronized (lockObject) {
            if ( currentRequest!=null ) {
                if ( ! xAxis.getMemento().equals( currentRequest.xmem ) ) {
                    logger.log( Level.FINE, "cancel old request: {0}", currentRequest);
                    ProgressMonitor monitor= currentRequest.monitor;
                    currentRequest= null;
                    monitor.cancel();
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
            
            DatumRange loadRange= xAxis.getDatumRange();
            
            CacheTag cacheTag= new CacheTag( loadRange, resolution );
            if ( dsd.getDataSetCache().haveStored(dsd, cacheTag) ) {
                renderer.setDataSet( DataSetAdapter.create( dsd.getDataSetCache().retrieve( dsd, cacheTag ) ) );
                currentRequest= null;
                
            } else {
                
                progressMonitor = getMonitor( "dsd.requestDataSet "+dsd+":"+loadRange+" @ "+ 
                        ( resolution==null ? "intrinsic" : ""+DatumUtil.asOrderOneUnits(resolution) ) );
                
                parent.repaint( 0, 0, parent.getWidth(), parent.getHeight() );
                
                //if ( renderer.isOverloading() ) loadRange= loadRange.rescale(-1,2);
                logger.log(Level.FINE, "request data from dsd: {0} @ {1}", new Object[]{loadRange, resolution});
                
                currentRequest= new Request( progressMonitor, xAxis.getMemento(), yAxis.getMemento() );
                
                dsd.requestDataSet( loadRange.min(), loadRange.max(), resolution, progressMonitor, parent.getCanvas() );
                // the request will come back with a DataSetUpdated event
            }
        }
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
                e instanceof StreamException ||
                e instanceof CancelledOperationException ;
        if ( result==false ) {
            result= e.getCause() instanceof InterruptedIOException;
        }
        return result;
    }
    
    public void dataSetUpdated( DataSetUpdateEvent e ) {
        
        synchronized ( lockObject ) {
            if ( renderer.getDataLoader()!=this ) return; // see bug 233
            
            logger.log(Level.FINE, "got dataset update:{0}", e);
            // TODO make sure Exception is cleared--what if data set is non-null but Exception is as well?
            if ( e.getException()!=null && e.getDataSet()!=null ) {
                throw new IllegalStateException("both exception and data set");
            } else if (e.getException() != null) {
                logger.log(Level.FINE, "got dataset update exception: "+e.getException().getMessage(), e.getException());
                Exception exception = e.getException();
                if ( !rendererHandlesException(exception) ) {
                    DasApplication.getDefaultApplication().getExceptionHandler().handle(exception);
                }
                
                ProgressMonitor mon= e.getMonitor();
                if ( currentRequest!=null ) {
                    if ( mon==null || mon==currentRequest.monitor ) {
                        renderer.setException( exception );
                        renderer.setDataSet(null);
                        logger.log(Level.FINE, "current request completed w/exception: "+e.getException().getMessage(), currentRequest);
                        currentRequest=null;
                    } else {
                        logger.fine("got exception but not for currentRequest " );
                    }
                } else {
                    logger.fine("got exception but currentRequest " );
                }
                
                
                if ( !rendererHandlesException(exception)  ) {
                    DasApplication.getDefaultApplication().getExceptionHandler().handle(exception);
                }
                
            } else if ( e.getDataSet()==null ) {
                // this indicates that the DataSetDescriptor has changed, and that the
                // renderer needs to reread the data.  Cause this by invalidating the
                // component.
                logger.fine("got dataset update notification (no dataset).");
                loadDataSet( renderer.getParent().getXAxis(), renderer.getParent().getYAxis() );
                return;
            } else {
                if ( currentRequest==null ) {
                    logger.fine( "ignore update w/dataset, currentRequest=null" );
                    // note this is hiding a bug.  Why did the dataset continue to load after we
                    // cancelled it? --jbf
                } else {
                    QDataSet ds= e.getDataSet();
                    ProgressMonitor mon= e.getMonitor();
                    if ( mon==null || currentRequest.monitor==mon ) {
                        logger.log(Level.FINE, "got dataset update w/dataset: {0}", ds);
                        if ( ds!=null ) {
                            if ( ds.length()>0 ) {
                                logger.log(Level.FINE, "  ds range: {0}", Ops.extent(ds).slice(0) );
                            } else {
                                logger.fine("  ds range: (empty)" );
                            }
                        }
                        renderer.setDataSet( ds );

                        logger.log(Level.FINE, "current request completed w/dataset: {0}", currentRequest.xmem);
                        currentRequest=null;
                    } else {
                        logger.log(Level.FINE, "got dataset update w/dataset but not my monitor: {0}", ds);
                    }
                }
            }
        }
    }
    
    
    public void setDataSetDescriptor( DataSetDescriptor dsd ) {
        logger.log(Level.FINE, "set dsd: {0}", dsd);
        if ( this.dsd!=null ) this.dsd.removeDataSetUpdateListener(this);
        this.dsd = dsd;
        if ( dsd!=null ) dsd.addDataSetUpdateListener(this);
        update();
    }
    
    public DataSetDescriptor getDataSetDescriptor() {
        return this.dsd;
    }
    
    @Override
    public void setReloadDataSet(boolean reloadDataSet) {
        super.setReloadDataSet(reloadDataSet);
        this.dsd.reset();
    }
    
    //TODO: this shadows the same property of the super class.  This should be cleaned up.
    private boolean fullResolution = false;
    @Override
    public boolean isFullResolution() {
        return fullResolution;
    }
    
    @Override
    public void setFullResolution(boolean b) {
        if (fullResolution == b) return;
        fullResolution = b;
    }
    
    public Request getCurrentRequest() {
        return this.currentRequest;
    }
    
    public Request[] getUnsolicitedRequests() {
        return (Request[])this.unsolicitedRequests.toArray( new Request[unsolicitedRequests.size()] );
    }
    
    public Request getUnsolicitedRequests( int i ) {
        return (Request)unsolicitedRequests.get(i);
    }
    
}
