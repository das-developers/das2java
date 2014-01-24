/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.DasException;
import org.das2.datum.Datum;
import org.das2.util.monitor.ProgressMonitor;

/**
 * Render introduced to clean up how rendering is done.  Back in the
 * old days, updatePlotImage was called off the event thread, and when
 * it was done we would call render.  This allowed tasks that could be 
 * performed in parallel to be done so, and rendering would be improved.
 * At some point this was lost, and much of the work is done on the 
 * event thread.  It used to be that render would always be called to paint
 * the image, but then the plot cacheImage was introduced.
 * 
 * @author jbf
 */
public class SimpleRenderer extends Renderer {

    private long t0= System.currentTimeMillis();
    
    private long birthMilli;
    private long dt;
    
    private String updateThreadName;
    
    private Datum datax,datay;
    private final long us;
    private final long rs;
    

    /**
     * create a SimpleRenderer, which is a circle at x,y 
     * with delay of us milliseconds when updating, and 
     * a delay of rs milliseconds when rendering.
     * @param x the x position.
     * @param y the y position.
     * @param us the number of milliseconds to take when updating, negative means no delay.
     * @param rs the number of milliseconds to take when rendering, negative means no delay.
     */
    public SimpleRenderer( Datum x, Datum y, long us, long rs ) {
        this.datax= x;
        this.datay= y;
        this.us= us;
        this.rs= rs;
        if ( us>10000 ) throw new IllegalArgumentException("us must be <10000");
        if ( rs>1000 ) throw new IllegalArgumentException("rs must be <1000");
    }
    
    @Override
    public void render(Graphics g, DasAxis xAxis, DasAxis yAxis, ProgressMonitor mon) {
        DasPlot parent= getParent();
        
        String s;
        int dx, dy;
        
        int x= (int)xAxis.transform(datax) + (int)( Math.random()*10 ) -5 ;
        int y= (int)yAxis.transform(datay) + (int)( Math.random()*10 ) -5 ;
        g.setColor( Color.decode("0x0000FF") );
        g.fillOval( x-50, y-50, 100, 100 );
        g.setColor( Color.decode("0xaaaaFF") );
        g.fillOval( x-47, y-47, 94, 94 );
        g.setFont( Font.decode("sans-20") );
        s = String.format("%6.2fs",birthMilli/1000.);
        dx= g.getFontMetrics().stringWidth(s);
        dy= g.getFontMetrics().getAscent();
        g.setColor( parent.getBackground() );
        g.drawString( s, x-dx/2, y+dy/2 );
        
        g.setFont( Font.decode("sans-10") );
        s = String.format("%6dms",dt);
        dx= g.getFontMetrics().stringWidth(s);
        dy= g.getFontMetrics().getAscent();
        g.drawString( s, x-dx/2, y+2*dy );
        
        dx= g.getFontMetrics().stringWidth(updateThreadName);
        g.drawString( updateThreadName, x-dx/2, y-dy );
                
        if ( rs>0 ) {
            try {
                Thread.sleep(rs);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimpleRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void updatePlotImage(DasAxis xAxis, DasAxis yAxis, ProgressMonitor monitor) throws DasException {
        birthMilli= System.currentTimeMillis() - t0;
        if ( us>0 ) {
            try {
                Thread.sleep(us);
            } catch (InterruptedException ex) {
                Logger.getLogger(SimpleRenderer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        dt= System.currentTimeMillis() - t0 - birthMilli;
        updateThreadName= Thread.currentThread().getName();
    }
   
    
    
}
