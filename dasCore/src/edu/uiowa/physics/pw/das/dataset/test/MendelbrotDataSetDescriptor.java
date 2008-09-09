/*
 * MendelbrotDataSetDescriptor.java
 *
 * Created on May 11, 2004, 10:26 AM
 */

package edu.uiowa.physics.pw.das.dataset.test;

import org.das2.DasException;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.graph.*;
import org.das2.util.monitor.ProgressMonitor;
import java.beans.*;

/**
 *
 * @author  Jeremy
 */
public class MendelbrotDataSetDescriptor extends DataSetDescriptor {
    /* we store the yaxis to kludge in the yresolution, range */
    DasAxis yAxis;
    
    int limit=200;
    int overSampleFactor= 1;    
    
    /** Creates a new instance of MendelbrotDataSetDescriptor */    
    public MendelbrotDataSetDescriptor( DasAxis yAxis ) {
        this.yAxis= yAxis;
        yAxis.addPropertyChangeListener(getPropertyChangeListener());
        this.setDefaultCaching(false);
    }
    
    private PropertyChangeListener getPropertyChangeListener() {
        return new PropertyChangeListener() {
            public void propertyChange( PropertyChangeEvent e ) {
                fireDataSetUpdateEvent(new DataSetUpdateEvent((Object)MendelbrotDataSetDescriptor.this));
            }
        };
    }
    
    private float punktfarbe(double xwert, double ywert) // color value from 0.0 to 1.0 by iterations
    {
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
    
    
    protected DataSet getDataSetImpl(Datum start, Datum end, Datum resolution, ProgressMonitor monitor) throws DasException {
        
        double xstart, xend, xresolution;
        xstart= start.doubleValue(Units.dimensionless);
        xend= end.doubleValue(Units.dimensionless);
        xresolution= resolution.doubleValue(Units.dimensionless) / overSampleFactor;
        
        double ystart, yend, yresolution;        
        ystart= yAxis.getDataMinimum(Units.dimensionless);        
        yend= yAxis.getDataMaximum(Units.dimensionless);
        int _ny= yAxis.getRow().getHeight();
        yresolution= ( yend-ystart ) / ( _ny * overSampleFactor ); 
                
        int ny= (int)(((yend-ystart)/yresolution));
        int nx= (int)(((xend-xstart)/xresolution));
        
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
            result.setXTagDouble( ix, xstart + ( ix+0.5) * xresolution, Units.dimensionless );            
        }
        
        double[] ytags= new double[ny];
        for ( int iy=0; iy<ny; iy++ ) {
            result.setYTagDouble( 0, iy, ystart + ( iy+0.5 )* yresolution, Units.dimensionless );            
        }
        
        result.setProperty( DataSet.PROPERTY_X_TAG_WIDTH, resolution.multiply(2.) );
        result.setProperty( DataSet.PROPERTY_Y_TAG_WIDTH, Units.dimensionless.createDatum(yresolution).multiply(2.) );
        
        return result;
        
    }
    
    public Units getXUnits() {
        return Units.dimensionless;
    }
    
    public void setLimit( int limit ) {
        if ( this.limit!=limit ) {
            this.limit= limit;
            fireDataSetUpdateEvent( new DataSetUpdateEvent( (Object)this ) );
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
            fireDataSetUpdateEvent( new DataSetUpdateEvent( (Object)this ) );
        }
    }
}
