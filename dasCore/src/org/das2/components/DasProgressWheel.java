/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.components;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.das2.DasApplication;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.AbstractProgressMonitor;
//import org.das2.util.monitor.ProgressMonitor;

/**
 * Small 16x16 pixel progress wheel, designed intentionally for loading TCAs with X axis.
 * @author jbf
 */
public class DasProgressWheel extends AbstractProgressMonitor {

    //Used to keep track of who is starting but not finishing.
    //private static final Map<ProgressMonitor,StackTraceElement[]> mons= new HashMap();
    
    private static final Logger logger= LoggerManager.getLogger("das2.graphics.progress");
    private static final int SIZE= 16;
    private static final int HIDE_MS= 300;

    public DasProgressWheel() {
        //mons.put( this, Thread.currentThread().getStackTrace() );
    }
    
    class MyPanel extends JComponent {

        @Override
        protected void paintComponent(Graphics g1) {
            
            if ( DasApplication.getDefaultApplication().isHeadless() ) {
                logger.info("suppressing paintComponent because graphics is headless.");
                return;
            }
            
            Graphics2D g2 = (Graphics2D) g1;

            String txt= ""+DasProgressWheel.this.getTaskProgress()+" of "+DasProgressWheel.this.getTaskSize();
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);


            logger.log(Level.FINEST, "painting {0}", txt);

            Color c= Color.BLUE;
            g2.setColor(c);
            g2.getClip();

            int r= SIZE/2;

            if (  System.currentTimeMillis()-t0 < HIDE_MS ) {
                return;
            }

            double a= ( System.currentTimeMillis()-t0 )/5000. * 2 * Math.PI;
            double da= 30*Math.PI/180;
            
            GeneralPath gp= new GeneralPath();
            gp.moveTo( (float)(r-r*Math.cos(a+da)), (float)(r-r*Math.sin(a+da)) );
            gp.lineTo( (float)(r+r*Math.cos(a+da)), (float)(r+r*(Math.sin(a+da))) );
            gp.lineTo( (float)(r+r*Math.cos(a-da)), (float)(r+r*(Math.sin(a-da))) );
            gp.lineTo( (float)(r-r*Math.cos(a-da)), (float)(r-r*Math.sin(a-da)) );
            gp.lineTo( (float)(r-r*Math.cos(a+da)), (float)(r-r*Math.sin(a+da)) );
            g2.fill( gp );
            this.setToolTipText( "<html>" + txt + "<br><b>" + DasProgressWheel.this.getLabel() + "</b><br>" + DasProgressWheel.this.getProgressMessage() );
            super.paintComponent(g1);

        }

    }    // provides details button, which shows who creates and who consumes the ProgressPanel

    private void init() {
        Container parentComponent= thePanel.getParent();
        if (parentComponent != null) {
            int x = 0;
            int y = 0;
            thePanel.setLocation( x, y );
        }
        timer= new Timer( 100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                c++;
                thePanel.repaint();
                logger.finest("repaint");
            }
        } );
        timer.setRepeats(true);
        timer.start();

    }

    @Override
    public void finished() {
        super.finished();
        if ( timer!=null ) {
            timer.stop();
        }
        Runnable run= new Runnable() {
            @Override
            public void run() {
                if ( thePanel!=null ) thePanel.setVisible(false);
                if ( theParent!=null ) {
                    theParent.remove(thePanel);
                    theParent.repaint(); 
                }
            }
        };
        SwingUtilities.invokeLater(run);
        //mons.remove( this );
    }

    @Override
    public void cancel() {
        super.cancel();
        timer.stop();
        finished();
    }

    JComponent thePanel;

    /**
     * this is the component containing the monitor, generally a glass pane.
     */
    JComponent theParent;
    int c= 0;
    Timer timer;
    long t0= System.currentTimeMillis();

    /**
     * return the small component that visually shows the wheel.
     * @param parent
     * @return 
     */
    public JComponent getPanel( final JComponent parent ) {
        if ( thePanel==null && !( isFinished() || isCancelled() ) ) {
            thePanel= new MyPanel();
            thePanel.setBounds( new Rectangle(0,0,SIZE,SIZE) );
            parent.add(thePanel);
            theParent= parent;
            init();
        }
        return thePanel;
    }


}
