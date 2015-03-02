/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.das2.util.monitor.NullProgressMonitor;

/**
 * One-line Label component prints the update progress to a
 * JLabel component.  This is either created by the client and set with 
 * setLabelComponent, or this class will also create one with getLabelComponent.
 * 
 * @author jbf
 */
public class DasProgressLabel extends NullProgressMonitor {
        
        private int ndot= 2;
        private String taskLabel= "";
        private JLabel label=null;

        Timer repaintTimer= new Timer( 333,new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                String p;
                if ( getTaskSize()==-1 ) {
                    p= "";
                } else {
                    p= "" + getTaskProgress()+"/"+getTaskSize();
                }
                ndot++;
                if ( ndot==4 ) ndot=1;
                if ( label!=null ) {
                    if ( isFinished() ) {
                        label.setText( "<html><i>&nbsp;" + taskLabel + "...finished</i></html>" );
                    } else {
                        label.setText( "<html><i>&nbsp;" + taskLabel + "...".substring(0,ndot) + "   ".substring(ndot,3)+p+"</i></html>" );
                    }
                }
            }
        } );
        
        /**
         * create the DasProgessLabel with the given label for the task.
         * @param taskLabel the label for the task.
         */
        public DasProgressLabel( String taskLabel ) {
            repaintTimer.setRepeats(true);
            repaintTimer.start();
            this.taskLabel= taskLabel;
        }
        
        /**
         * get the assigned label component, or create one if one has not been assigned.
         * @return the assigned label component
         */
        public synchronized JLabel getLabelComponent() {
            if ( label==null ) {
                label= new JLabel();
            }
            return label;
        }
        
        /**
         * set the label to use for the progress monitor.
         * @param label the assigned label component
         */
        public synchronized void setLabelComponent( JLabel label ) {
            this.label= label;
        }
        
        
        @Override
        public void finished() {
            super.finished();
            repaintTimer.setRepeats(false);
            repaintTimer.stop();
            SwingUtilities.invokeLater( new Runnable() {
                @Override
                public void run() {
                     if ( label!=null ) label.setText( "<html><em>&nbsp;" + taskLabel + "...finished</em></html>" );
                }
            });
        }
        
}
