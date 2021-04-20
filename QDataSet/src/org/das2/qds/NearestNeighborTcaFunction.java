/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.das2.datum.Units;
import org.das2.qds.ops.Ops;

/**
 * return a function based on a QDataSet.  For the rank 1 or 2 dataset with 
 * DEPEND_0, this function returns the nearest neighbor found within the 
 * function, always as a rank 1 dataset.
 * @author jbf
 */
public class NearestNeighborTcaFunction extends AbstractQFunction {

    private final QDataSet dep0;
    private final QDataSet data;
    private QDataSet fill;
    
    /**
     * Create the function
     * @param ds 
     */
    public NearestNeighborTcaFunction( QDataSet ds ) {
        this.dep0= (QDataSet) ds.property(QDataSet.DEPEND_0);
        if ( this.dep0==null ) throw new IllegalArgumentException("dataset does not have DEPEND_0 and cannot be used.");
        this.data= ds;
        exampleInput(); // initialize 
    }
    
    /**
     * return the index of the closest data.
     * @param d
     * @return 
     */
    private int closest( QDataSet d ) {
        return (int)( Ops.findex( dep0, d ).value() + 0.5 );
    }
    
    private DDataSet outputDescriptor=null;
    
    @Override
    public QDataSet value(QDataSet parm) {
        int i= closest( parm.slice(0) );
        if ( i<0 ) return fill;
        if ( i>=data.length() ) return fill;
        QDataSet result;
        if ( data.rank()==1 ) {
            result= Ops.bundle( data.slice( i ) );
        } else {
            result= data.slice( i );
        }
        ((MutablePropertyDataSet)result).putProperty( QDataSet.BUNDLE_0, outputDescriptor );
        return result;
    }

    private final DDataSet inputDescriptor = DDataSet.createRank2(1,0);
        
    @Override
    public final QDataSet exampleInput() {
        QDataSet q = dep0.slice(0);
        //TODO: there might be a problem if the first dep0 value is fill.
        MutablePropertyDataSet ret = (MutablePropertyDataSet) Ops.bundle(q);
        Map<String,Object> p = DataSetUtil.getDimensionProperties( q, new HashMap() );
        for ( Entry<String,Object> e : p.entrySet() ) {
            inputDescriptor.putProperty( e.getKey(), 0, e.getValue() );
        }
        ret.putProperty(QDataSet.BUNDLE_0,inputDescriptor);
        
        QDataSet exampleOutput= data.slice(0);
        if ( exampleOutput.rank()==0 ) {
            Ops.bundle( exampleOutput );
        }
        outputDescriptor= DDataSet.createRank2( exampleOutput.length(), 0 );
        for ( int i=0; i<exampleOutput.length(); i++ ) {
            p = DataSetUtil.getDimensionProperties( exampleOutput.slice(i), new HashMap() );
            for ( Entry<String,Object> e : p.entrySet() ) {
                outputDescriptor.putProperty( e.getKey(), i, e.getValue() );
            }
        }
        
        this.fill= Ops.bundle( DataSetUtil.asDataSet( Double.NaN, (Units)outputDescriptor.property(QDataSet.UNITS, 0) ) );
        for ( int i=1; i<exampleOutput.length(); i++ ) {
            this.fill= Ops.bundle( this.fill, DataSetUtil.asDataSet( Double.NaN, (Units)outputDescriptor.property(QDataSet.UNITS, i) ) );
        }

        return ret;
    }
    
}
