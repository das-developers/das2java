
package org.das2.graph;

import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import org.das2.datum.Datum;
import org.das2.datum.Units;

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
    
    public DataGeneralPathBuilder( DasAxis xaxis, DasAxis yaxis ) {
        this.gp= new GeneralPath();
        this.xaxis= xaxis;
        this.yaxis= yaxis;
        this.xunits= xaxis.getUnits();
        this.yunits= yaxis.getUnits();
    }
    
    public void addDataPoint( boolean valid, Datum x, Datum y ) {
        if ( pen==PEN_UP ) {
            if ( valid ) {
                gp.moveTo( xaxis.transform(x), yaxis.transform(y) );
                pen= PEN_DOWN;
                pendingx= x;
                pendingy= y;
            } else {
                pen= PEN_UP;
            }
        } else {
            if ( valid ) {
                gp.lineTo( xaxis.transform(x), yaxis.transform(y) );
            } else {
                if ( pendingx!=null ) {
                    gp.lineTo( xaxis.transform(pendingx), yaxis.transform(pendingy) );
                }
                pen= PEN_UP;
            }
            pendingx=null;
            pendingy=null;            
        }
    }
    
    public void addDataPoint( boolean valid, double x, double y ) {
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
    }
    
    public GeneralPath getGeneralPath() {
        return gp;
    }
    
    public PathIterator getPathIterator() {
        return gp.getPathIterator(null);
    }
    
}
