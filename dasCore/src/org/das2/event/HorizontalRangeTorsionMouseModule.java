/*
 * HorizontalRangeTorsionMouseModule.java
 *
 * Created on February 12, 2004, 6:00 PM
 */

package org.das2.event;

import org.das2.datum.Units;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.event.*;
import javax.swing.*;
/**
 *
 * @author  Jeremy
 */
public class HorizontalRangeTorsionMouseModule extends MouseModule {
    
    
    //  this dumb idea was abandoned.
    DasAxis axis;
    double min;
    double max;
    Units units;
    int x0;
    int y0;
    long tt;
    
    double inOutVelocity;
    double inOutPosition;
    double nextPrevVelocity;
    double nextPrevPosition;
    
    static final double inOutFactor= 1e-6;
    static final double nextPrevFactor= 100000;
    
    boolean mouseButtonPressed;
    
    /** Creates a new instance of HorizontalRangeTorsionMouseModule */
    public HorizontalRangeTorsionMouseModule( DasAxis axis ) {
        //super( axis, new PointSlopeDragRenderer(axis), "AxisDriver" );
        this.axis= axis;
    }
    
    public void mousePressed(java.awt.event.MouseEvent e) {
        super.mousePressed(e);
        units= axis.getUnits();
        min= axis.getDataMinimum(units);
        max= axis.getDataMaximum(units);
        x0= e.getX();
        y0= e.getY();
        tt= System.currentTimeMillis();
        inOutPosition=0.;
        nextPrevPosition=0.;
        mouseButtonPressed= true;
    }
    
    void timerTask() {
        if ( mouseButtonPressed ) {
            long dt= System.currentTimeMillis() - tt;
            inOutPosition+= inOutVelocity * dt;
            nextPrevPosition+= nextPrevVelocity * dt;
            tt+= dt;
            reportPosition();            
            startTimer();
            
        }
    }
    
    public void startTimer() {
        int delay = 100; //milliseconds
        ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                timerTask();
            }
        };
        new Timer(delay, taskPerformer).start();
    }
    
    public void mouseDragged(java.awt.event.MouseEvent e) {
        super.mouseDragged(e);
        int dx= e.getX()-x0;
        int dy= e.getY()-y0;
        long dt= System.currentTimeMillis() - tt;
        inOutVelocity= dy * inOutFactor;
        nextPrevVelocity= dx * nextPrevFactor;
        timerTask();
    }
    
    private void reportPosition() {
        double dd= max - min;
        
        double offset= nextPrevPosition;
        double scale= 1 + inOutPosition;
        
        double newMin=  min + offset ;
        double newMax=  min + offset + dd * scale;
        
        //System.out.println( "min: " + newMin + "offset: " + offset + " scale: " + scale );
        //System.out.println( "" + units.createDatum(newMin) + " " + units.createDatum(newMax) );
        axis.setDataRange( units.createDatum(newMin), units.createDatum(newMax) );
    }
    
    public void mouseReleased(java.awt.event.MouseEvent e) {
        super.mouseReleased(e);
        mouseButtonPressed= false;
    }
    
}
