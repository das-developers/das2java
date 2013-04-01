/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * Builder for SparseDataSets.  This was introduced to fix the problem where
 * we wish to calculate the SparseDataSet in one pass, but we can't do this because
 * PyQDataSets obscure the type.
 * @author jbf
 */
public class SparseDataSetBuilder {
    
    private SparseDataSet ds;
    
    public SparseDataSetBuilder( int rank ) {
        ds= SparseDataSet.createRank(rank);
    }
    
    public SparseDataSet getDataSet() {
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
        ds.putProperty( name, value );
    }

    public void putProperty(String name, int index, Object value) {
        ds.putProperty( name, index, value );
    }

    public void setLength( int i0 ) {
        ds.setLength(i0);
    }
  
    public void setQube( int[] qube ) {
        ds.setQube(qube);
    }    
    
}
