/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset.examples;

import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.virbo.dataset.JoinDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dsops.Ops;

/**
 * For the various QDataSet schemes, show examples and documentation for
 * each.  This was motivated when trying to describe the output of 
 * org.das2.graph.ContoursRenderer.doAutorange()
 * @author jbf 
 */
public class Schemes {
    
    /**
     * return a bounding box for the data.  This is a rank 2 dataset where
     * ds[0,:] shows the bounds for the first dimension and ds[1,:] shows the
     * bounds for the second dimension.  Therefor ds[0,0] is the minumum extent
     * of the first dimension, and ds[0,1] is the maximum.
     * Note this can be extended to any number
     * of dimensions (cube or hypercube).
     * 
     * Note,
     *<blockquote><pre><small>{@code
     *from org.virbo.dataset.examples import Schemes
     *ds= Schemes.boundingBox()
     *print asDatumRange(ds.slice(0))
     *}</small></pre></blockquote>
     * 
     * @return a bounding box for the data.
     * @see org.virbo.dataset.DataSetUtil#asDatumRange(org.virbo.dataset.QDataSet) 
     */
    public static QDataSet boundingBox( ) {
        try {
            QDataSet xx= Ops.timegen( "2015-02-20T00:30", "60 seconds", 1440 );
            QDataSet yy= Ops.linspace( 14., 16., 1440 );
            JoinDataSet bds= new JoinDataSet(2);
            bds.join( Ops.extent( xx ) );
            bds.join( Ops.extent( yy ) );
            bds.makeImmutable();
            return bds;
        } catch (ParseException ex) {
            throw new RuntimeException(ex);
        }
    }
    
}
