
package org.das2.graph;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import org.das2.datum.Datum;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.Units;
import org.das2.datum.UnitsUtil;

/**
 * introduce class to handle the task of creating GeneralPaths from a
 * series of data points.  We notice that creating the GeneralPath is 
 * plenty fast, and it's rendering it that is slow, so this will allow for
 * future optimizations.
 * 
 * @see script:sftp://jbf@nudnik.physics.uiowa.edu/home/jbf/project/juno/users/masafumi/20171114/dataMasherDemo.jy
 * 
 * @author jbf
 */
public class DataGeneralPathBuilder {
    
    private final GeneralPath gp;
    private final DasAxis xaxis;
    private final DasAxis yaxis;
    private final Units xunits;
    private final Units yunits;
    
    private static final Object PEN_UP= "penup";
    private static final Object PEN_DOWN= "pendown";
    
    private Object pen= PEN_UP;
    
    private Datum pendingx= null;
    private Datum pendingy= null;
    
    private double lastx=-Double.MAX_VALUE;
    private double cadence=0.0;
    private boolean logStep= false;
    
    public DataGeneralPathBuilder( DasAxis xaxis, DasAxis yaxis ) {
        this.gp= new GeneralPath();
        this.xaxis= xaxis;
        this.yaxis= yaxis;
        this.xunits= xaxis.getUnits();
        this.yunits= yaxis.getUnits();
    }

    /**
     * set the limit where two points are considered adjacent, or null
     * if no check should be done.
     * @param sw 
     */
    public void setCadence(Datum sw) {
        if ( sw==null ) {
            this.cadence= 0.;
            this.logStep= false;
        } else {
            if ( UnitsUtil.isRatiometric( sw.getUnits() ) ) {
                this.cadence= sw.multiply(1.2).doubleValue(Units.logERatio);
                this.logStep= true;
            } else {
                try {
                    this.cadence= sw.multiply(1.2).doubleValue(xunits.getOffsetUnits());
                } catch ( InconvertibleUnitsException ex ) {
                    this.cadence= sw.multiply(1.2).doubleValue(sw.getUnits());
                }
                
            }
        }
    }

    public void addDataPoint( boolean valid, Datum x, Datum y ) {
        double xx= x.doubleValue(xunits);
        double yy= y.doubleValue(yunits);
        addDataPoint( valid, xx, yy );
    }
    
    public void addDataPoint( boolean valid, double x, double y ) {
        if ( this.cadence>0 && pen==PEN_DOWN ) {
            double step= logStep ? Math.log(x/lastx) :  x-lastx ;
            if ( step > this.cadence ) {
                pen= PEN_UP;
            }
        }
        if ( pen==PEN_UP ) {
            if ( valid ) {
                gp.moveTo( xaxis.transform(x,xunits), yaxis.transform(y,yunits) );
                pen= PEN_DOWN;
                pendingx= xunits.createDatum(x);
                pendingy= yunits.createDatum(y);
            } else {
                pen= PEN_UP;
            }
        } else {
            if ( valid ) {
                gp.lineTo( xaxis.transform(x,xunits), yaxis.transform(y,yunits) );
            } else {
                if ( pendingx!=null ) {
                    gp.lineTo( xaxis.transform(pendingx), yaxis.transform(pendingy) );
                }
                pen= PEN_UP;
            }
            pendingx=null;
            pendingy=null;            
        }
        lastx= x;
    }
    
    public GeneralPath getGeneralPath() {
        return gp;
    }
    
    public PathIterator getPathIterator() {
        return gp.getPathIterator(null);
    }
    
}
