/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.qdataset.schemes.impl;

import org.qdataset.schemes.EnhancedXYData;
import org.qdataset.schemes.XYData;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;

/**
 *
 * @author jbf
 */
public class DataFactory {

    public static XYData getXYData( QDataSet ds ) {
        if ( ds.rank()!=1 ) throw new IllegalArgumentException("rank 1");
        return new XYDataImpl(ds);
    }
    
    public static XYData getXYData( QDataSet x, QDataSet y ) {
        if ( x.rank()!=1 ) throw new IllegalArgumentException("rank 1");
        if ( y.rank()!=1 ) throw new IllegalArgumentException("rank 1");
        if ( x.length()!=y.length() ) throw new IllegalArgumentException("x length != y length");
        
        return new XYDataImpl( Ops.link( x, y ) );
    }

    public static EnhancedXYData getEnhancedXYData( QDataSet ds ) {
        return new EnhancedXYDataImpl(ds);
    }
}
