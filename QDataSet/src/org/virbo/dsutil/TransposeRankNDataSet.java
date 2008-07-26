/*
 * TransposeRank2DataSet.java
 *
 * Created on December 11, 2007, 10:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import org.virbo.dataset.*;

/**
 * wrap a qube dataset to transpose the indeces.  Brute force implementation.  No copying.
 * @author jbf
 */
public class TransposeRankNDataSet extends AbstractDataSet {
    
    QDataSet source;
    int[] order;
    int[] qube;
    final int shuffleType;
    private final int SHUFFLE_123= 0; //brute force, good luck with rank 4.
    private final int SHUFFLE_132= 1;
    private final int SHUFFLE_213= 2;
    private final int SHUFFLE_231= 3;
    private final int SHUFFLE_312= 4;
    private final int SHUFFLE_321= 5;
    
    
    public TransposeRankNDataSet( QDataSet source, int[] order ) {
        this.source= source;
        this.order= order;
        this.qube= DataSetUtil.qubeDims(source);
        
        for ( int i=0; i<source.rank(); i++ ) {
            QDataSet depi=  (QDataSet) source.property( "DEPEND_"+order[i] );
            if ( depi!=null ) properties.put( "DEPEND_"+i, depi );
        }
        
        if ( order[0]==0 ) {
            if ( order[1]==1 ) {
                shuffleType= SHUFFLE_123;
            } else {
                shuffleType= SHUFFLE_132;
            }
        } else if ( order[0]==1 ) {
            if ( order[1]==0 ) {
                shuffleType= SHUFFLE_213;
            } else {
                shuffleType= SHUFFLE_231;
            }
        } else {
            if ( order[1]==0 ) {
                shuffleType= SHUFFLE_312;
            } else {
                shuffleType= SHUFFLE_321;
            }
        } 
    }

    public int rank() {
        return source.rank();
    }

    @Override
    public double value(int i) {
        return super.value(i);
    }

    public double value(int i0, int i1) {
        return source.value( i0, i1 );
    }

    @Override
    public double value(int i1, int i2, int i3) {
        // see you on dailywtf...
        switch ( shuffleType ) {
            case SHUFFLE_123: return source.value( i1, i2, i3);
            case SHUFFLE_132: return source.value( i1, i3, i2);
            case SHUFFLE_213: return source.value( i2, i1, i3);
            case SHUFFLE_231: return source.value( i2, i3, i1);
            case SHUFFLE_312: return source.value( i3, i1, i2);
            case SHUFFLE_321: return source.value( i3, i2, i1);
            default: throw new RuntimeException("implementation error");
        }
    }


    public Object property(String name) {
        Object v= properties.get(name);
        return ( v==null ) ? source.property(name) : v;
    }

    public Object property(String name, int i) {
        Object v= properties.get(name);
        return ( v==null ) ? source.property(name,i) : v;
    }

    public Object property(String name, int i0, int i1) {
        Object v= properties.get(name);
        return ( v==null ) ? source.property(name,i0,i1) : v;
    }

    public int length() {
        return qube[order[0]];
    }

    public int length(int i) {
        return qube[order[1]];
    }
    
    public int length( int i, int j ) {
        return qube[order[2]];
    }

}
