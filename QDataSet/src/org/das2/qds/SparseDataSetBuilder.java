/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.das2.qds;

/**
 * Builder for SparseDataSets.  This was introduced to fix the problem where
 * we wish to calculate the SparseDataSet in one pass, but we can't do this because
 * PyQDataSets obscure the type and there was no way to set the length.
 * 
 * This is also useful for building the BundleDescriptor datasets that describe how to 
 * unpack rank 2 datasets with the BUNDLE_1 property.
 * @author jbf
 */
public class SparseDataSetBuilder {
    
    private SparseDataSet ds;
    
    public SparseDataSetBuilder( int rank ) {
        ds= SparseDataSet.createRank(rank);
    }
    
    public SparseDataSet getDataSet() {
        if ( ds.length0==-1 ) {
            throw new IllegalArgumentException("setLength was never called");
        }
        return ds;
    }

    public void putValue(double d) {
        ds.putValue(d);
    }

    public void putValue(int i0, double d) {
        ds.putValue( i0, d );
    }

    public void putValue(int i0, int i1, double d) {
        ds.putValue( i0, i1, d );
    }

    public void putValue(int i0, int i1, int i2, double d) {
        ds.putValue( i0, i1, i2, d );
    }

    public void putValue(int i0, int i1, int i2, int i3, double d) {
        ds.putValue( i0, i1, i2, i3, d );
    }

    public void putProperty(String name, Object value) {
        SemanticOps.checkPropertyType( name, value, true );
        ds.putProperty( name, value );
    }

    public void putProperty(String name, int index, Object value) {
        SemanticOps.checkPropertyType( name, value, true );
        ds.putProperty( name, index, value );
    }

    /**
     * set the length of the zeroth dimension.  Other dimensions have length set implicitly by the highest value set.
     * If this is not set explicitly, then it will be implicit as well.
     * @param i0
     */
    public void setLength( int i0 ) {
        ds.setLength(i0);
    }
  
    /**
     * make this a qube dataset, where all the lengths are the same.
     * implicitly this calls setLength(qube[0]).
     * @param qube 
     */    
    public void setQube( int[] qube ) {
        ds.setQube(qube);
    }    
    
}
