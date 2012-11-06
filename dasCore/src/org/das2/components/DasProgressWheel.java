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
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.Timer;
import org.das2.util.LoggerManager;
import org.das2.util.monitor.NullProgressMonitor;

/**
 *
 * @author jbf
 */
public class DasProgressWheel extends NullProgressMonitor {

    private static final Logger logger= LoggerManager.getLogger("das2.graphics.progress");

    class MyPanel extends JComponent {

        @Override
        protected void paintComponent(Graphics g1) {
            Graphics2D g2 = (Graphics2D) g1;

            String txt= ""+DasProgressWheel.this.getTaskProgress()+" of "+DasProgressWheel.this.getTaskSize();
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);

            logger.log(Level.FINEST, "painting {0}", txt);

            Color c= Color.GRAY;
            //g2.setColor(new Color(0xdcFFFFFF, true));
            g2.setColor(c);
            g2.getClip();

            int h=16;
            int r= 8;

            double a= ( System.currentTimeMillis()-t0 )/5000. * 2 * Math.PI;
            double da= 30*Math.PI/180;
            
            Rectangle rect = g2.getClipBounds();
            GeneralPath gp= new GeneralPath();
            gp.moveTo( (float)(r-r*Math.sin(a)), (float)(r-r*(Math.cos(a))) );
            gp.lineTo( (float)(r+r*Math.cos(a)), (float)(r+r*(Math.sin(a))) );
            gp.lineTo( (float)(r+r*Math.cos(a+da)), (float)(r+r*(Math.sin(a+da))) );
            gp.lineTo( (float)(r-r*Math.sin(a)), (float)(r-r*(Math.cos(a))) );
            if (rect == null) {
                g2.fill( gp );
            } else {
                g2.fill( gp );
            }
            this.setToolTipText( txt );
            super.paintComponent(g1);

        }

    }    // provides details button, which shows who creates and who consumes the ProgressPanel

    private void init() {
        Container parentComponent= thePanel.getParent();
        if (parentComponent != null) {
            int x = parentComponent.getBounds().x + parentComponent.getBounds().width/2;
            int y = parentComponent.getBounds().y + parentComponent.getBounds().height/2;
            thePanel.setLocation(x - thePanel.getWidth() / 2, y - thePanel.getHeight() / 2);
        }
        timer= new Timer( 300, new ActionListener() {
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
        timer.stop();
        theParent.remove(thePanel);
    }

    @Override
    public void cancel() {
        super.cancel();
        timer.stop();
        theParent.remove(thePanel);
    }

    JComponent thePanel;

    /**
     * this is the component containing the monitor, generally a glass pane.
     */
    JComponent theParent;
    int c= 0;
    Timer timer;
    long t0= System.currentTimeMillis();


    public JComponent getPanel( JComponent parent ) {
        if ( thePanel==null ) {
            thePanel= new MyPanel();
            thePanel.setBounds( new Rectangle(0,0,16,16) );
            parent.add(thePanel);
            theParent= parent;
            init();
        }
        return thePanel;
    }


}
