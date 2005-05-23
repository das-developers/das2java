/*
 * BoxZoomMouseModule.java
 *
 * Created on May 20, 2005, 12:21 PM
 */

package edu.uiowa.physics.pw.das.event;

import edu.uiowa.physics.pw.das.*;
import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.datum.*;
import edu.uiowa.physics.pw.das.graph.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author Jeremy
 */
public class BoxZoomMouseModule extends BoxRangeSelectorMouseModule {
    
    DatumRange xrange, yrange;
    JDialog dialog;
    JLabel xrangeLabel, yrangeLabel;
    boolean autoUpdate= false;
    boolean constrainProportions= false;
    
    /** Creates a new instance of BoxZoomMouseModule */
    public BoxZoomMouseModule( DasCanvasComponent parent, DataSetConsumer consumer, DasAxis xAxis, DasAxis yAxis ) {
        super( parent, consumer, xAxis, yAxis );
        setLabel("Box Zoom");
    }
    
    JDialog getDialog() {
        if ( dialog==null ) {
            dialog= new JDialog( DasApplication.getDefaultApplication().getMainFrame() );
            Container content= dialog.getContentPane();
            Box b= Box.createVerticalBox();
            content.add(b);
            
            b.add( xrangeLabel= new JLabel("X: ") );
            xrangeLabel.setPreferredSize( new Dimension( 110, 20 ) );
            b.add( yrangeLabel= new JLabel("Y: ") );
            yrangeLabel.setPreferredSize( new Dimension( 110, 20 ) );
            
            JPanel zoomButtonPanel= new JPanel();
            zoomButtonPanel.setLayout(new BorderLayout());
            JButton zx = new JButton(getZoomXAction());
            JButton zy = new JButton(getZoomYAction());
            Insets i = zy.getMargin();
            i.left = 5;
            i.right = 5;
            zy.setMargin(i);
            JButton zb = new JButton(getZoomBoxAction());
            zoomButtonPanel.add(zy, BorderLayout.WEST);
            zoomButtonPanel.add(zb, BorderLayout.CENTER);
            zoomButtonPanel.add(zx, BorderLayout.SOUTH);
            
            b.add(zoomButtonPanel);
            
            final JCheckBox cb= new JCheckBox( "auto box zoom" );
            b.add( cb );
            cb.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    autoUpdate= cb.isSelected();
                }
            } );
            
            if ( xAxis.getUnits()==yAxis.getUnits() ) {
                final JCheckBox cp= new JCheckBox( "constrain proportions" );
                b.add(cp);
                cp.addActionListener( new ActionListener() {
                    public void actionPerformed( ActionEvent e ) {
                        constrainProportions= cp.isSelected();
                    }
                } );                
            }
            dialog.pack();
        }
        dialog.setVisible(true);
        
        return dialog;
    }
    
    Action getZoomYAction() {
        return new AbstractAction("<html><center>z<br>o<br>o<br>m<br>Y</center></html>") {
            public void actionPerformed( ActionEvent e ) {
                if ( yrange!=null ) yAxis.setDatumRange(yrange);
            }
        };
    }
    
    Action getZoomXAction() {
        return new AbstractAction("zoom X") {
            public void actionPerformed( ActionEvent e ) {
                if ( xrange!=null ) xAxis.setDatumRange(xrange);
            }
        };
    }
    
    Action getZoomBoxAction() {
        return new AbstractAction("<html><center>Zoom<br>Box</center></html>") {
            public void actionPerformed( ActionEvent e ) {
                zoomBox();
            }
        };
    }
    
    private void zoomBox() {
        if ( yrange!=null ) yAxis.setDatumRange(yrange);
        if ( xrange!=null ) xAxis.setDatumRange(xrange);
    }
    
    public void mouseRangeSelected(MouseDragEvent e0) {
        if ( e0 instanceof MouseBoxEvent ) {
            MouseBoxEvent e= (MouseBoxEvent)e0;
            
            xrange= new DatumRange( xAxis.invTransform(e.getXMinimum()), xAxis.invTransform(e.getXMaximum()) );
            yrange= new DatumRange( yAxis.invTransform(e.getYMaximum()), yAxis.invTransform(e.getYMinimum()) );
            
            if ( constrainProportions ) {
                double aspect= yAxis.getHeight() / (double)xAxis.getWidth();
                DatumRange mx= new DatumRange( e.getXMinimum(), e.getXMaximum(), Units.dimensionless );
                DatumRange my= new DatumRange( e.getYMinimum(), e.getYMaximum(), Units.dimensionless );                
                double mouseAspect= my.width().divide(mx.width()).doubleValue(Units.dimensionless);
                if ( mouseAspect > aspect ) {
                    double f= mouseAspect / aspect;
                    mx= mx.rescale( 0.5-f/2, 0.5+f/2 );
                } else {
                    double f= aspect / mouseAspect;
                    my= my.rescale( 0.5-f/2, 0.5+f/2 );
                }
                xrange= new DatumRange( xAxis.invTransform(mx.min().doubleValue(Units.dimensionless)), 
                        xAxis.invTransform(mx.max().doubleValue(Units.dimensionless)) );
                yrange= new DatumRange( yAxis.invTransform(my.max().doubleValue(Units.dimensionless)), 
                        yAxis.invTransform(my.min().doubleValue(Units.dimensionless)) );                                        
            } else {                
                xrange= new DatumRange( xAxis.invTransform(e.getXMinimum()), xAxis.invTransform(e.getXMaximum()) );
                yrange= new DatumRange( yAxis.invTransform(e.getYMaximum()), yAxis.invTransform(e.getYMinimum()) );
            }
            
            getDialog();
            
            xrangeLabel.setText( "X: "+xrange.toString() );
            yrangeLabel.setText( "Y: "+yrange.toString() );
            
            if ( autoUpdate ) {
                zoomBox();
            }
        } else if ( e0.isGesture() ) {
            if ( e0.getGesture()==Gesture.ZOOMOUT ) {
                xAxis.setDataRangeZoomOut();
                yAxis.setDataRangeZoomOut();
            } else if ( e0.getGesture()==Gesture.BACK ) {
                xAxis.setDataRangePrev();
                yAxis.setDataRangePrev();
            } else if ( e0.getGesture()==Gesture.FORWARD ) {
                xAxis.setDataRangeForward();
                yAxis.setDataRangeForward();
            }
        }
        
    }
    
    
    
    
    
}
