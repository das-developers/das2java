/*
 * ZoomPanMouseModule.java
 *
 * Created on August 7, 2007, 8:53 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.DatumRange;
import edu.uiowa.physics.pw.das.datum.DatumRangeUtil;
import edu.uiowa.physics.pw.das.event.MouseModule;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.DasPlot;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 *
 * @author jbf
 */
public class ZoomPanMouseModule extends MouseModule {
    
    DasAxis xAxis;
    DasAxis yAxis;
    
    Point p0;
    DatumRange xAxisRange0;
    DatumRange yAxisRange0;
    
    /** Creates a new instance of ZoomPanMouseModule */
    public ZoomPanMouseModule( DasAxis horizontalAxis, DasAxis verticalAxis ) {
        this.xAxis= horizontalAxis;
        this.yAxis= verticalAxis;
    }
    
    public void mouseWheelMoved(MouseWheelEvent e) {
        double nmin, nmax;
        if ( e.getWheelRotation()==-1 ) {
            nmin= 0.20;
            nmax= 0.80;
        } else {
            nmin= -0.25;
            nmax= 1.25;
        }
        if ( xAxis!=null ) {
            DatumRange dr;
            if ( xAxis.isLog() ) {
                dr= DatumRangeUtil.rescaleLog( xAxis.getDatumRange(), nmin, nmax );
            } else {
                dr= DatumRangeUtil.rescale( xAxis.getDatumRange(), nmin, nmax );
            }
            xAxis.setDatumRange( dr );
        }
        if ( yAxis!=null ) {
            DatumRange dr;
            if ( yAxis.isLog() ) {
                dr= DatumRangeUtil.rescaleLog( yAxis.getDatumRange(), nmin, nmax );
            } else {
                dr= DatumRangeUtil.rescale( yAxis.getDatumRange(), nmin, nmax );
            }
            yAxis.setDatumRange( dr );
        }
        super.mouseWheelMoved(e);
    }
    
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
    }
    
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        Point p2= e.getPoint();
        if ( xAxis!=null ) {
            DatumRange dr;
            if ( xAxis.isLog() ) {
                Datum delta= xAxis.invTransform( p0.getX() ).divide( xAxis.invTransform( p2.getX() ) );
                dr= new DatumRange( xAxisRange0.min().multiply(delta), xAxisRange0.max().multiply(delta) );
            } else {
                Datum delta= xAxis.invTransform( p0.getX() ).subtract( xAxis.invTransform( p2.getX() ) );
                dr= new DatumRange( xAxisRange0.min().add(delta), xAxisRange0.max().add(delta) );
            }
            xAxis.setDatumRange( dr );
        }
        if ( yAxis!=null ) {
            DatumRange dr;
            if ( yAxis.isLog() ) {
                Datum ydelta= yAxis.invTransform( p0.getY() ).divide( yAxis.invTransform( p2.getY() ) );
                dr= new DatumRange( yAxisRange0.min().multiply(ydelta), yAxisRange0.max().multiply(ydelta) );
            } else {
                Datum ydelta= yAxis.invTransform( p0.getY() ).subtract( yAxis.invTransform( p2.getY() ) );
                dr= new DatumRange( yAxisRange0.min().add(ydelta), yAxisRange0.max().add(ydelta) );
            }
            yAxis.setDatumRange( dr );
        }
    }
    
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        p0= e.getPoint();
        if ( xAxis!=null ) xAxisRange0= xAxis.getDatumRange();
        if ( yAxis!=null ) yAxisRange0= yAxis.getDatumRange();
    }
    
    
    
}
