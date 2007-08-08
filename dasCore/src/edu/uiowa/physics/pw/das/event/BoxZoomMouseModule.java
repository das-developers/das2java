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
    JCheckBox autoUpdateCB, constrainProportionsCB;
    BoxZoomDialog bzdialog;
    
    boolean autoUpdate= false;
    boolean constrainProportions= false;
    
    /** Creates a new instance of BoxZoomMouseModule */
    public BoxZoomMouseModule( DasCanvasComponent parent, DataSetConsumer consumer, DasAxis xAxis, DasAxis yAxis ) {
        super( parent, consumer, xAxis, yAxis );
        setLabel("Box Zoom");
    }
    
    JDialog getDialog() {
        if ( dialog==null ) {
            dialog= new JDialog( (Frame)null );
            dialog.setLocationRelativeTo( parent );
            
            Container content= dialog.getContentPane();
            bzdialog= new BoxZoomDialog( this );
            content.add( bzdialog );
            
            dialog.pack();
        }
        
        bzdialog.setAutoBoxZoom( autoUpdate );
        bzdialog.setDisablePopup( popupDisabled );
        bzdialog.setConstrainProportions( constrainProportions );
        
        if ( !popupDisabled || !autoUpdate ) dialog.setVisible(true);
        
        return dialog;
    }
    
    protected void guiChanged() {
        autoUpdate= bzdialog.isAutoBoxZoom(  );
        popupDisabled= bzdialog.isDisablePopup( );
        constrainProportions = bzdialog.isConstrainProportions( );
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
    
    protected void zoomBox() {
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
            
            if ( ! autoUpdate ) {
                getDialog();
                bzdialog.setXRange( xrange.toString() );
                bzdialog.setYRange(yrange.toString());
            } else {
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
    
    /**
     * Getter for property autoUpdate.
     * @return Value of property autoUpdate.
     */
    public boolean isAutoUpdate() {
        return this.autoUpdate;
    }
    
    /**
     * Setter for property autoUpdate.
     * @param autoUpdate New value of property autoUpdate.
     */
    public void setAutoUpdate(boolean autoUpdate) {
        if ( bzdialog!=null ) bzdialog.setAutoBoxZoom( autoUpdate );
        this.autoUpdate = autoUpdate;
    }
    
    /**
     * Getter for property constrainProportions.
     * @return Value of property constrainProportions.
     */
    public boolean isConstrainProportions() {
        return this.constrainProportions;
    }
    
    /**
     * Setter for property constrainProportions.
     * @param constrainProportions New value of property constrainProportions.
     */
    public void setConstrainProportions(boolean constrainProportions) {
        if ( bzdialog!=null ) bzdialog.setConstrainProportions(constrainProportions);
        this.constrainProportions = constrainProportions;
    }
    
    /**
     * Holds value of property popupDisabled.
     */
    private boolean popupDisabled;
    
    /**
     * Getter for property popupDisabled.
     * @return Value of property popupDisabled.
     */
    public boolean isPopupDisabled() {
        return this.popupDisabled;
    }
    
    /**
     * Setter for property popupDisabled.
     * @param popupDisabled New value of property popupDisabled.
     */
    public void setPopupDisabled(boolean popupDisabled) {
         if ( bzdialog!=null ) bzdialog.setDisablePopup(popupDisabled);
        this.popupDisabled = popupDisabled;
    }
    
    
    
    
    
}
