/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.virbo.dsutil;

import org.virbo.dataset.DDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.SemanticOps;

/**
 * reduce and standardize code needed to build bundle descriptors used in BUNDLE_1.  This can
 * only build a bundle of rank 1 datasets.
 * 
 * @see org.virbo.dataset.SparseDataSetBuilder which is a more general way of doing this.
 * @see org.virbo.dsutil.DataSetBuilder
 * @author jbf
 */
public class BundleBuilder {

    DDataSet ds;
    boolean readOnly= false;

    /**
     * create the builder for the BundleDescriptor to go into BUNDLE_1 property.
     * @param size number of datasets to be bundled.
     */
    public BundleBuilder( int size ) {
        ds= DDataSet.createRank2(size,1);
    }

    public void putProperty( String name, int index, Object value ) {
        if ( readOnly ) throw new IllegalArgumentException("cannot be used after getDataSet is called");
        SemanticOps.checkPropertyType( name, value, true );
        ds.putProperty( String.format( "%s__%d", name, index ), value );
    }

    public QDataSet getDataSet() {
        return ds;
    }
}
