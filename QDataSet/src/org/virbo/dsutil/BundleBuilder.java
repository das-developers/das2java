/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;

/**
 * reduce and standardize code needed to build bundle descriptors used in BUNDLE_1.
 * @author jbf
 */
public class BundleBuilder {

    DDataSet ds;
    boolean readOnly= false;

    public BundleBuilder( int size ) {
        ds= DDataSet.createRank2(size,1);
    }

    public void putProperty( String name, int index, Object value ) {
        if ( readOnly ) throw new IllegalArgumentException("cannot be used after getDataSet is called");
        ds.putProperty( String.format( "%s__%d", name, index ), value );
    }

    public QDataSet getDataSet() {
        return ds;
    }
}
