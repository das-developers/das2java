/*
 * AxisAutoRangeController.java
 *
 * Created on August 24, 2005, 1:03 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.util;

import edu.uiowa.physics.pw.das.dataset.DataSet;
import edu.uiowa.physics.pw.das.dataset.DataSetDescriptor;
import edu.uiowa.physics.pw.das.dataset.DataSetUpdateListener;
import edu.uiowa.physics.pw.das.graph.DasAxis;
import edu.uiowa.physics.pw.das.graph.GraphUtil;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Jeremy
 */
public class AxisAutoRangeController implements DataSetUpdateListener, PropertyChangeListener {
    DataSetDescriptor dsd;
    DasAxis xAxis, yAxis, zAxis;
    DasAxis myXAxis, myYAxis, myZAxis;
    Map xManualProps, yManualProps, zManualProps;
    
    // if this is true, then propertyChanges were caused by this controller, otherwise they appear to be manual
    boolean isAutomaticPropertyChange;
    
    public AxisAutoRangeController( DasAxis xAxis, DasAxis yAxis, DasAxis zAxis, DataSetDescriptor dsd ) {
        this.xAxis= xAxis;
        this.yAxis= yAxis;
        this.zAxis= zAxis;
        this.dsd= dsd;
        dsd.addDataSetUpdateListener(this);
        if ( xAxis!=null ) xAxis.addPropertyChangeListener(this);
        if ( yAxis!=null ) yAxis.addPropertyChangeListener(this);
        if ( zAxis!=null ) zAxis.addPropertyChangeListener(this);
        xManualProps= new HashMap();
        yManualProps= new HashMap();
        zManualProps= new HashMap();
        
        isAutomaticPropertyChange= false;
    }
    
    public void dataSetUpdated(edu.uiowa.physics.pw.das.dataset.DataSetUpdateEvent e) {
        DataSet ds= e.getDataSet();
        if ( ds==null ) return;
        
        isAutomaticPropertyChange= true;
        
        if ( xAxis!=null ) {
            DasAxis newAxis= GraphUtil.guessXAxis(ds);
            if ( !xManualProps.containsKey("dataMinimum") ) xAxis.setDatumRange(newAxis.getDatumRange());
            if ( !xManualProps.containsKey("log") ) xAxis.setLog(newAxis.isLog());
            myXAxis= newAxis;
        }
        
        if ( yAxis!=null ) {
            DasAxis newAxis= GraphUtil.guessYAxis(ds);
            if ( !yManualProps.containsKey("dataMinimum") ) yAxis.setDatumRange(newAxis.getDatumRange());
            if ( !yManualProps.containsKey("log") ) yAxis.setLog(newAxis.isLog());
            if ( !yManualProps.containsKey("label") ) yAxis.setLabel( newAxis.getLabel() );
            myYAxis= newAxis;
        }
        
        if ( zAxis!=null ) {
            DasAxis newAxis= GraphUtil.guessZAxis(ds);
            if ( !zManualProps.containsKey("dataMinimum") ) zAxis.setDatumRange(newAxis.getDatumRange());
            if ( !zManualProps.containsKey("log") ) zAxis.setLog(newAxis.isLog());
            if ( !zManualProps.containsKey("label") ) zAxis.setLabel( newAxis.getLabel() );
            myZAxis= newAxis;
        }
        
        isAutomaticPropertyChange= false;
    }
    
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        if ( isAutomaticPropertyChange==false ) {
            if ( e.getSource()==xAxis ) {
                xManualProps.put( e.getPropertyName(), e.getNewValue() );                
            } else if ( e.getSource()==yAxis ) {
                yManualProps.put( e.getPropertyName(), e.getNewValue() );
            } else if ( e.getSource()==zAxis ) {
                zManualProps.put( e.getPropertyName(), e.getNewValue() );
            }
        }
    }
    
}
