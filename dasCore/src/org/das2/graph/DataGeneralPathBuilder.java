
package org.das2.graph;

import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.datum.Datum;
import org.das2.datum.InconvertibleUnitsException;
import org.das2.datum.LoggerManager;
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
public final class DataGeneralPathBuilder {
    
    private static final Logger logger= LoggerManager.getLogger("das2.graph.renderer.path");
    
    private final GeneralPath gp;
    private int pointCount=0;
    
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
    
    private double lastIX= -Double.MAX_VALUE; // always the position of the data, not the histogram corners
    private double lastIY= -Double.MAX_VALUE;

    private double lastDrawnX= -Double.MAX_VALUE;
    private double lastDrawnY= -Double.MAX_VALUE;
    
    private double cadence=0.0; // this is the cadence used to identify breaks in the data.
    private double cadenceExact= 1e38; // this is the cadence requested by the client
    private boolean logStep= false;
    private boolean histogramFillFlag;
    
    //private Datum checkx= null;
    
    public DataGeneralPathBuilder( DasAxis xaxis, DasAxis yaxis ) {
        this.gp= new GeneralPath();
        //this.gp= new GraphUtil.DebuggingGeneralPath();
        this.xaxis= xaxis;
        this.yaxis= yaxis;
        this.xunits= xaxis.getUnits();
        this.yunits= yaxis.getUnits();
        logger.fine( "-----" );
//        try {
//            checkx= null; //Units.t2000.parse("2018-02-07T19:00");
//        } catch (ParseException ex) {
//            Logger.getLogger(DataGeneralPathBuilder.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    /**
     * set the limit where two points are considered adjacent, or null
     * if no check should be done.
     * @param sw 
     */
    public void setCadence(Datum sw) {
        if ( sw==null ) {
            this.cadence= 0.;
            this.cadenceExact= 1e38;
            this.logStep= false;
        } else {
            if ( UnitsUtil.isRatiometric( sw.getUnits() ) ) {
                this.cadence= sw.multiply(1.2).doubleValue(Units.logERatio);
                this.cadenceExact= sw.doubleValue(Units.logERatio);
                this.logStep= true;
            } else {
                try {
                    this.cadence= sw.multiply(1.2).doubleValue(xunits.getOffsetUnits());
                    this.cadenceExact= sw.doubleValue(xunits.getOffsetUnits());
                } catch ( InconvertibleUnitsException ex ) {
                    this.cadence= sw.multiply(1.2).doubleValue(sw.getUnits());
                }
                
            }
        }
    }
    
    /**
     * return the x cadence used as a double.  This is should interpreted with
     * the xaxis units and with isCadenceRatiometric.
     * @return the cadence as a double, or 1e38 if there is no cadence.
     */
    public double getCadenceDouble() {
        return this.cadenceExact;
    }
    
    private boolean histogramMode = false;

    public static final String PROP_HISTOGRAM_MODE = "histogramMode";

    public boolean isHistogramMode() {
        return histogramMode;
    }

    public void setHistogramMode(boolean histogramMode) {
        this.histogramMode = histogramMode;
    }

    /**
     * return the units of the xaxis.
     * @return the units of the xaxis.
     */
    public Units getXUnits() {
        return this.xunits;
    }
    
    /**
     * return the units of the yaxis.
     * @return the units of the yaxis.
     */
    public Units getYUnits() {
        return this.yunits;
    }
    
    /**
     * return true if the spacing in x has been identified as ratiometric 
     * (linearly spaced on a log axis).  Note this
     * is for the data, which is not necessarily the x axis setting.
     * @return true if the data has been marked as ratiometric.
     */
    public boolean isCadenceRatiometric() {
        return this.logStep;
    }

    /**
     * add a data point to the curve.
     * @param valid if invalid, then break the line at this point.
     * @param x the x value
     * @param y the y value
     */    
    public void addDataPoint( boolean valid, Datum x, Datum y ) {
        double xx= x.doubleValue(xunits);
        double yy= y.doubleValue(yunits);
        addDataPoint( valid, xx, yy );
    }
    
    /**
     * kludge to tell the builder to subtract a half cadence from the first point
     */
    public void setHistogramFillFlag() {
        histogramFillFlag= true;
    }
    
    /**
     * add a point to the curve, where x and y are the magnitude in data coordinates.
     * @param valid if invalid, then break the line at this point.
     * @param x the x value in xaxis units.
     * @param y the y value in yaxis units.
     */
    public void addDataPoint( boolean valid, double x, double y ) {
        pointCount++;
        if ( pointCount==1 ) {
            logger.fine("pathBuilder got first point");
            System.err.println( String.format( "%5s %8s %8s %8s %8s %8s", "pc", "x", "y", "pendingX", "pendingY", "lastx" ) );
            System.err.println( String.format( "%5s %8s %8s %8s %8s %8s", "-----", "--------","--------","--------","--------", "--------" ) );
        }
        System.err.println( String.format( "%5d %8s %8s %8s %8s %8s", pointCount, x, y, pendingx, pendingy, lastx ) );
        
        if ( lastx>x ) {
            logger.log(Level.FINE, "data step back: {0} -> {1}", new Object[]{xunits.createDatum(lastx), xunits.createDatum(x)});
        }
        //if ( checkx !=null && x>checkx.doubleValue(xunits) ) {
        //    System.err.println("here stop at "+xunits.createDatum(x));
        //}
        if ( this.cadence>0 && pen==PEN_DOWN ) {
            double step= logStep ? Math.log(x/lastx) :  x-lastx ;
            if ( step > this.cadence ) {
                if ( pendingx!=null ) {
                    lastDrawnX=xaxis.transform(pendingx);
                    lastDrawnY=yaxis.transform(pendingy);
                    gp.lineTo( lastDrawnX, lastDrawnX );
                }
                pen= PEN_UP;
            }
        }
        if ( pen==PEN_UP ) {
            if ( valid ) {
                if ( histogramMode ) {
                    double iulx= xaxis.transform(x-this.cadenceExact/2,xunits); // upper-left corner of peak
                    double iy= yaxis.transform(y,yunits);
                    lastDrawnX= iulx;
                    gp.moveTo( iulx, iy );
                    lastIX= xaxis.transform(x,xunits);
                    lastIY= iy;
                } else {
                    gp.moveTo( xaxis.transform(x,xunits), yaxis.transform(y,yunits) );
                }
                pen= PEN_DOWN;
                pendingx= xunits.createDatum(x);
                pendingy= yunits.createDatum(y);
            } else {
                // nothing needs to be done
            }
        } else if ( pen==PEN_DOWN ) { 
            if ( valid ) {
                if ( histogramMode ) {
                    double iy= yaxis.transform(y,yunits);
                    double ix;
                    if ( histogramFillFlag ) {
                        gp.lineTo( lastDrawnX, iy );
                        lastDrawnY= iy;
                        histogramFillFlag= false;
                        lastIX= xaxis.transform(x,xunits);
                    } else {
                        ix= xaxis.transform(x,xunits);
                        lastDrawnX= (lastIX+ix)/2; 
                        lastDrawnY= lastIY;
                        gp.lineTo( lastDrawnX, lastDrawnY );
                        lastDrawnX= (lastIX+ix)/2;
                        if ( lastIX>-Double.MAX_VALUE ) {
                            lastDrawnX= (lastIX+ix)/2;
                            lastDrawnY= iy;
                            gp.lineTo( lastDrawnX, lastDrawnY );
                        }
                        lastIX= ix;
                    }
                    lastIY= iy;
                    pendingx= xunits.createDatum(x);
                    pendingy= yunits.createDatum(y);
                } else {
                    lastDrawnX= xaxis.transform(x,xunits);
                    lastDrawnY= yaxis.transform(y,yunits);
                    gp.lineTo( lastDrawnX, lastDrawnY );
                    pendingx=null;
                    pendingy=null;            
                }
            } else { // not valid
                if ( histogramMode ) {
                    if ( pendingx!=null ) {
                        double iPendingX= xaxis.transform(pendingx);
                        double ix= xaxis.transform(x,xunits);
                        lastDrawnX= ( iPendingX + ix ) / 2;
                        lastDrawnY= yaxis.transform(pendingy);
                        gp.lineTo( lastDrawnX, lastDrawnY );
                    }
                } else {
                    if ( pendingx!=null ) {
                        lastDrawnX= xaxis.transform(pendingx);
                        lastDrawnY= yaxis.transform(pendingy);
                        gp.lineTo( lastDrawnX, lastDrawnY );
                    }
                }
                pen= PEN_UP;
                pendingx=null;
                pendingy=null;            
            }
            
        }
        lastx= x;
    }
    
    /**
     * get the generalPath for drawing.  This may affect the
     * path, when histogramMode is true.
     * @return the generalPath for drawing.
     */
    public GeneralPath getGeneralPath() {
        if ( histogramMode ) {
            if ( lastIX>-Double.MAX_VALUE ) {
                double ix= xaxis.transform(lastx+this.cadenceExact/2,xunits);
                gp.lineTo( ix, lastIY );
            }
        }
        //return this.gp.getGeneralPath();
        return gp;
    }
    
    /**
     * this is added so that the fill-to-zero code can add the returns.
     * @return 
     */
    public Point2D getLastDrawnPoint() {
        return new Point2D.Double( lastDrawnX, lastDrawnY );
    }
    
    /**
     * get the path iterator.
     * @return the path iterator.
     */
    public PathIterator getPathIterator() {
        return gp.getPathIterator(null);
    }
    
}
