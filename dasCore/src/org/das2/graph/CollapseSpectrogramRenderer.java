/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.graph;

import java.util.Map;
import org.das2.dataset.DataSetDescriptor;
import org.das2.datum.DatumRange;
import org.das2.datum.Units;
import org.das2.event.DataRangeSelectionEvent;
import org.das2.event.DataRangeSelectionListener;
import org.das2.qds.DDataSet;
import org.das2.qds.DataSetOps;
import org.das2.qds.DataSetUtil;
import org.das2.qds.QDataSet;
import org.das2.qds.WritableDataSet;
import org.das2.qds.ops.Ops;
import static org.das2.qds.ops.Ops.copy;
import static org.das2.qds.ops.Ops.copyProperties;

/**
 *
 * @author leiffert
 */
public class CollapseSpectrogramRenderer extends SpectrogramRenderer {
    
    public CollapseSpectrogramRenderer(DataSetDescriptor dsd, DasColorBar colorBar) {
        super(dsd, colorBar);
    }
    
    private QDataSet collapseDataSet;
    
    private int minIndex;
    private int maxIndex;
    private int collapseDimension = 2;
    
    private int prevMinIndex;
    private int prevMaxIndex;

    // Put in set/get data range and make indices private
    
    private DatumRange dataRange;

    public DatumRange getDataRange() {
        return dataRange;
    }

    public void setDataRange(DatumRange dataRange) {
        this.dataRange = dataRange;
        prevMinIndex = minIndex;
        prevMaxIndex = maxIndex;
        QDataSet minIndexDs = Ops.findex(getDataSet().property("DEPEND_" + collapseDimension), dataRange.min());
        minIndex = (int) Math.round(minIndexDs.value());
        QDataSet maxIndexDs = Ops.findex(getDataSet().property("DEPEND_" + collapseDimension), dataRange.max());
        maxIndex = (int) Math.round(maxIndexDs.value());
        averageCollapseDataSet();
    }

    public int getCollapseDimension() {
        return collapseDimension;
    }

    public void setCollapseDimension(int collapseDimension) {
        if(collapseDimension < 0 || collapseDimension > 2){
            throw new IllegalArgumentException("collapse dimension must be 0, 1, or 2");
        }
       
        this.collapseDimension = collapseDimension;
        
    }
    
    private DataRangeSelectionListener dataRangeSelectionListener = new DataRangeSelectionListener() {
            @Override
            public void dataRangeSelected(DataRangeSelectionEvent e) {
                setDataRange(e.getDatumRange());
            }
        };
    
    public DataRangeSelectionListener sliceRangeListener(){
        return dataRangeSelectionListener;
    }
    
    private void dataBounds(QDataSet qds){
        double min = Double.MAX_VALUE; 
        double max = -Double.MIN_VALUE;
        if(qds.rank() == 1){
            for(int i = 0; i < qds.length(); i++){
                if(qds.value(i) < min)
                    min = qds.value(i);
                if(qds.value(i) > max)
                    max = qds.value(i);
            }
        }else if(qds.rank() ==2){
            for(int i = 0; i < qds.length(); i++){
                for(int j = 0; j < qds.length(0); j++){
                    if(qds.value(i,j) < min){
                        min = qds.value(i,j);
                    }
                    if(qds.value(i,j) > max){
                        max = qds.value(i,j);
                    }
                }
            }
        }else{
            throw new IllegalArgumentException("Ds rank should be 1 or 2.");
        }
        Units units = (Units) qds.property(QDataSet.UNITS);

    }

    /*
        @param k - the index along the collapse dimension
        @param i - the index along the first dimension of the collapse dataset
        @param j - the index along the second dimension of the collapse dataset
    */
    private static double value(QDataSet ds, int dim, int i, int j, int k ){
        switch(dim){
            case 0:
                return ds.value(k, i, j);
            case 1:
                return ds.value(i, k, j);
            case 2:
                return ds.value(i, j, k);
            default:
                throw new IllegalStateException("Collapse dimension must be 0, 1, or 2.");
        }
    }
    
    private void averageCollapseDataSet(){


        QDataSet uncollapsedDs = getDataSet();
        WritableDataSet ds;
        switch(collapseDimension){
            case 0:
                ds = DDataSet.createRank2(uncollapsedDs.length(0),uncollapsedDs.length(0,0));
                break;
            case 1:
                ds = DDataSet.createRank2(uncollapsedDs.length(), uncollapsedDs.length(0,0));
                break;
            case 2: 
                ds = DDataSet.createRank2(uncollapsedDs.length(), uncollapsedDs.length(0));
                break;
            default:
                throw new IllegalStateException("collapse dimension must be 0, 1, or 2.");
                
        }
        
        // Fancy trick to see if they overlap
        if( collapseDataSet == null || (maxIndex - prevMinIndex)*(prevMaxIndex - minIndex) <= 0 ){
            for (int i = 0; i < ds.length(); i++) {
                for (int j = 0; j < ds.length(0); j++) {
                    double sum = 0;
                    for(int k = minIndex; k <=maxIndex; k++){
                        
                        sum += value(uncollapsedDs, collapseDimension, i,j,k);
                    }
                    ds.putValue(i,j,sum / (maxIndex - minIndex + 1));
                }
            }
        }else if(minIndex == maxIndex){
            for(int i = 0; i < ds.length(); i++){
                for(int j = 0; j < ds.length(0); j++){
                    double value = value(uncollapsedDs, collapseDimension, i, j, minIndex);
                    ds.putValue(i,j,value);
//                    ds.putValue(i,j,uncollapsedDs.value(i,j,minIndex));
                }
            }
        }else{
            for (int i = 0; i < ds.length(); i++) {
                for (int j = 0; j < ds.length(0); j++) {
                    ds.putValue(i, j, collapseDataSet.value(i, j));
                    //Doing the running average of the minIndexes
                    if (prevMinIndex > minIndex) {

                        for (int k = prevMinIndex; k > minIndex; k--) {
                            int n = prevMaxIndex - k + 1;
//                            double newValue = (uncollapsedDs.value(i, j, k - 1) + n * ds.value(i, j)) / (n + 1);
                            double newValue = (value(uncollapsedDs, collapseDimension, i, j, k-1) + n * ds.value(i, j)) / (n + 1);
                            ds.putValue(i, j, newValue);
                        }

                    } else if (prevMinIndex < minIndex) {
                        for (int k = prevMinIndex; k < minIndex; k++) {
                            int n = prevMaxIndex - k + 1;
//                            double newValue = (n * ds.value(i, j) - uncollapsedDs.value(i, j, k)) / (n - 1);
                            double newValue = (n * ds.value(i, j) - value(uncollapsedDs,collapseDimension, i, j, k)) / (n - 1);
                            ds.putValue(i, j, newValue);
                        }
                    }

                    //Doing the running average of the maxIndexes
                    if (prevMaxIndex > maxIndex) {
                        for (int k = prevMaxIndex; k > maxIndex; k--) {
                            int n = k - minIndex + 1;
//                            double newValue = (n * ds.value(i, j) - uncollapsedDs.value(i, j, k)) / (n - 1);
                            double newValue = (n * ds.value(i, j) - value(uncollapsedDs, collapseDimension, i, j, k)) / (n - 1);
                            ds.putValue(i, j, newValue);
                        }

                    } else if (prevMaxIndex < maxIndex) {
                        for (int k = prevMaxIndex; k < maxIndex; k++) {
                            int n = k - minIndex + 1;
//                            double newValue = (n * ds.value(i, j) + uncollapsedDs.value(i, j, k + 1)) / (n + 1);
                            double newValue = (n * ds.value(i, j) + value(uncollapsedDs, collapseDimension, i, j, k+1)) / (n + 1);
                            ds.putValue(i, j, newValue);
                        }
                    }
                }

            }
        }
        
        Map<String,Object> props= DataSetUtil.getProperties(uncollapsedDs);
        props= DataSetOps.sliceProperties( props, collapseDimension );
        DataSetUtil.putProperties( props, ds );
        collapseDataSet = ds;

        clearPlotImage();
        update();
        
    }
    
    
    
    @Override
    public void setDataSet(QDataSet qds){
       
        //set min max based on ds
        super.setDataSet(qds);
        QDataSet depDs;
        
        prevMinIndex = -1;
        prevMaxIndex = -1;
        
        if(collapseDimension == 0){
            depDs = (QDataSet) ds.property(QDataSet.DEPEND_0);
            setDataRange(new DatumRange(depDs.value(0), depDs.value(depDs.length() - 1), (Units) depDs.property(QDataSet.UNITS)));
        }
        else if(collapseDimension == 1){
            depDs = (QDataSet) ds.property(QDataSet.DEPEND_1);
            setDataRange(new DatumRange(depDs.value(0), depDs.value(depDs.length() - 1), (Units) depDs.property(QDataSet.UNITS)));
        }
        else if(collapseDimension == 2){
            depDs = (QDataSet) ds.property(QDataSet.DEPEND_2);
            setDataRange(new DatumRange(depDs.value(0), depDs.value(depDs.length() - 1), (Units) depDs.property(QDataSet.UNITS)));
        }else{
            throw new IllegalArgumentException("Only supports up to dimension 2");
        }
        
    }
    
    @Override
    protected QDataSet getInternalDataSet(){
      return collapseDataSet;  
    }
}
