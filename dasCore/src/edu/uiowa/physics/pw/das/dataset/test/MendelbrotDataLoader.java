/*
 * MendelbrotDataSetDescriptor.java
 *
 * Created on May 11, 2004, 10:26 AM
 */

package edu.uiowa.physics.pw.das.dataset.test;

import org.das2.DasException;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import edu.uiowa.physics.pw.das.graph.DataLoader;
import edu.uiowa.physics.pw.das.graph.Renderer;
import edu.uiowa.physics.pw.das.system.DasLogger;
import edu.uiowa.physics.pw.das.system.RequestProcessor;
import org.das2.util.monitor.ProgressMonitor;
import java.util.logging.Logger;


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
                        DataSet result= getDataSet( xRebinDescriptor, yRebinDescriptor, getMonitor(taskDescription) , taskDescription );
                        System.err.println( result.getProperty("TaskDescription") );
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
    
    private DataSet getDataSet( RebinDescriptor ddx, RebinDescriptor ddy, ProgressMonitor monitor, String desc) throws DasException {
        
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
        
        WritableTableDataSet result=  WritableTableDataSet.newSimple( nx, Units.dimensionless, ny, Units.dimensionless, Units.dimensionless );
        
        double[][] z= new double[nx][ny];
        
        monitor.setTaskSize(ny);
        monitor.started();
        for ( int iy=0; iy<ny; iy++ ) {
            if ( monitor.isCancelled() ) break;
            monitor.setTaskProgress(iy);
            for ( int ix=0; ix<nx; ix++ ) {
                result.setDouble(ix,iy, (double)punktfarbe( xstart + ix*xresolution, ystart + iy*yresolution ),Units.dimensionless );
            }
        }
        monitor.finished();
        
        double[] xtags= new double[nx];
        for ( int ix=0; ix<nx; ix++ ) {
            // it's important that the xtag be in the center of the bin!
            result.setXTagDouble( ix, xstart + ( ix ) * xresolution, Units.dimensionless );
        }
        
        double[] ytags= new double[ny];
        for ( int iy=0; iy<ny; iy++ ) {
            result.setYTagDouble( 0, iy, ystart + ( iy  )* yresolution, Units.dimensionless );
        }
        
        result.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, Units.dimensionless.createDatum(yresolution*2) );
        result.setProperty( DataSet.PROPERTY_Y_TAG_WIDTH, Units.dimensionless.createDatum(yresolution*2.) );
        result.setProperty( "TaskDescription", desc );
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
