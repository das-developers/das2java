/*
 * MendelbrotDataSetDescriptor.java
 *
 * Created on May 11, 2004, 10:26 AM
 */

package org.das2.dataset.test;

import org.das2.DasException;
import org.das2.dataset.DataSet;
import org.das2.datum.Units;
import org.das2.graph.DasAxis;
import org.das2.graph.DasPlot;
import org.das2.graph.DataLoader;
import org.das2.graph.Renderer;
import org.das2.system.DasLogger;
import org.das2.system.RequestProcessor;
import org.das2.util.monitor.ProgressMonitor;
import java.util.logging.Logger;
import org.das2.dataset.DataSetDescriptor;
import org.das2.dataset.RebinDescriptor;
import org.das2.dataset.WritableTableDataSet;
import org.das2.qds.DDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.ops.Ops;


/**
 *
 * @author  Jeremy
 */
public class MendelbrotDataLoader extends DataLoader {
    /* we store the yaxis to kludge in the yresolution, range */
    
    int limit=200;
    int overSampleFactor= 1;
    
    Request currentRequest;
    Request completedRequest;
    
    Logger logger= DasLogger.getLogger(DasLogger.GRAPHICS_LOG, "MendelBrotDataLoader" );
    
    /** Creates a new instance of MendelbrotDataSetDescriptor */
    public MendelbrotDataLoader( Renderer r ) {
        super(r);
    }
    
    private float punktfarbe(double xwert, double ywert) {// color value from 0.0 to 1.0 by iterations
        double r = 0.0, i = 0.0, m = 0.0;
        int j = 0;
        final int MAX=limit;
        
        while ((j < MAX) && (m < 4.0)) {
            j++;
            m = r * r - i * i;
            i = 2.0 * r * i + ywert;
            r = m + xwert;
        }
        if ( j==MAX ) j=0;
        return (float)j / (float)MAX;
    }
    
    public void update( ) {
        if ( isActive() ) {
            DasPlot p= getRenderer().getParent();
            
            if ( p==null ) return;
            
            DasAxis xAxis= p.getXAxis();
            DasAxis yAxis= p.getYAxis();
            
            if ( xAxis.valueIsAdjusting() || yAxis.valueIsAdjusting() ) return;
            
            final RebinDescriptor xRebinDescriptor = new RebinDescriptor(
                    xAxis.getDataMinimum(), xAxis.getDataMaximum(),
                    xAxis.getColumn().getWidth(),
                    xAxis.isLog());
            
            final RebinDescriptor yRebinDescriptor = new RebinDescriptor(
                    yAxis.getDataMinimum(), yAxis.getDataMaximum(),
                    yAxis.getRow().getHeight(),
                    yAxis.isLog());
            
            if ( currentRequest!=null ) {
                if ( ! ( xAxis.getMemento().equals( currentRequest.xmem ) || yAxis.getMemento().equals( currentRequest.ymem ) ) ) {
                    logger.fine( "cancel old request" );
                    currentRequest.monitor.cancel();
                } else {
                    logger.fine( "ignore repeat request" );
                    return; // ignore the repeated request
                }
            }
            
            final String taskDescription= "mendelbrot x:"+xAxis.getMemento()+" y:"+ yAxis.getMemento();
            
            if ( completedRequest!=null ) {
                if ( ( xAxis.getMemento().equals( completedRequest.xmem ) && yAxis.getMemento().equals( completedRequest.ymem ) ) ) {
                    logger.fine( "ignore satisfied request "+taskDescription );
                    return;
                }
            }
            
            currentRequest= new DataLoader.Request( getMonitor(taskDescription), xAxis.getMemento(), yAxis.getMemento() );
            
            Runnable run= new Runnable() {
                public void run( ) {
                    try {
                        logger.fine( "calculate dataset for "+taskDescription );
                        QDataSet result= getDataSet( xRebinDescriptor, yRebinDescriptor, getMonitor(taskDescription) , taskDescription );
                        System.err.println( result.property("TaskDescription") );
                        getRenderer().setDataSet( result );
                        completedRequest= currentRequest;
                        logger.fine( "completed "+taskDescription );
                        currentRequest= null;
                    } catch ( DasException e ) {
                        getRenderer().setException( e );
                    }
                    
                }
            };
            RequestProcessor.invokeAfter( run, this.getRenderer() );
        }
    }
    
    private QDataSet getDataSet( RebinDescriptor ddx, RebinDescriptor ddy, ProgressMonitor monitor, String desc) throws DasException {
        
        double xstart, xend, xresolution;
        xstart= ddx.binCenter(0, Units.dimensionless );
        xend= ddx.binCenter( ddx.numberOfBins()-1, Units.dimensionless );
        xresolution= ddx.binWidth() / overSampleFactor;
        
        double ystart, yend, yresolution;
        ystart= ddy.binCenter(0, Units.dimensionless );
        yend= ddy.binCenter( ddy.numberOfBins()-1, Units.dimensionless );
        yresolution= ddy.binWidth() / overSampleFactor;
        
        int ny= (int)(1.5+((yend-ystart)/yresolution));
        int nx= (int)(1.5+((xend-xstart)/xresolution));

        DDataSet result= DDataSet.createRank2(nx, ny);

        monitor.setTaskSize(ny);
        monitor.started();
        for ( int iy=0; iy<ny; iy++ ) {
            if ( monitor.isCancelled() ) break;
            monitor.setTaskProgress(iy);
            for ( int ix=0; ix<nx; ix++ ) {
                result.putValue( ix,iy, (double)punktfarbe( xstart + ix*xresolution, ystart + iy*yresolution ) );
            }
        }
        monitor.finished();

        result.putProperty( QDataSet.DEPEND_0, Ops.taggen( xstart, xresolution, nx, Units.dimensionless ) );
        result.putProperty( QDataSet.DEPEND_1, Ops.taggen( ystart, yresolution, ny, Units.dimensionless ) );
        
        result.putProperty( "TaskDescription", desc );

        return result;
        
    }
    
    public void setLimit( int limit ) {
        if ( this.limit!=limit ) {
            this.limit= limit;
            update();
        }
    }
    
    public int getLimit() {
        return this.limit;
    }
    
    /**
     * Getter for property overSampleFactor.
     * @return Value of property overSampleFactor.
     */
    public int getOverSampleFactor() {
        return this.overSampleFactor;
    }
    
    /**
     * Setter for property overSampleFactor.
     * @param overSampleFactor New value of property overSampleFactor.
     */
    public void setOverSampleFactor(int overSampleFactor) {
        if ( this.overSampleFactor!=overSampleFactor ) {
            this.overSampleFactor = overSampleFactor;
            completedRequest=null;
            update();
        }
    }
    
    public DataSetDescriptor getDataSetDescriptor() {
        return null;
    }
    
    public void setDataSetDescriptor(DataSetDescriptor dsd) {
    }
    
}
