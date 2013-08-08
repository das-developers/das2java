/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.virbo.dataset;

/**
 * efficient dataset that has no properties and one value.  This can be used as a marker
 * interface too, where the client can inspect the type and just get the valid value once.
 * See also ReplicateDataSet and TagGenDataSet
 * @author jbf
 */
public final class ConstantDataSet implements QDataSet {

    public ConstantDataSet( int len, double value ) {
        this( 1, len, 1, 1, 1, value );
    }
    
    private ConstantDataSet( int rank, int len0, int len1, int len2, int len3, double value ) {
        this.rank= rank;
        this.len0= len0;
        this.len1= len1;
        this.len2= len2;
        this.len3= len3;
        this.value= value;
    }
    
    int rank;
    int len0;
    int len1;
    int len2;
    int len3;
    double value;
    
    @Override
    public int rank() {
        return rank;
    }

    public double value() {
        return value;
    }

    public double value(int i0) {
        return value;
    }

    public double value(int i0, int i1) {
        return value;
    }

    public double value(int i0, int i1, int i2) {
        return value;
    }

    public double value(int i0, int i1, int i2, int i3) {
        return value;
    }

    public Object property(String name) {
        return null;
    }

    public Object property(String name, int i) {
        return null;
    }

    public int length() {
        return len0;
    }
    
    public int length(int i) {
        return len1;
    }

    public int length(int i, int j) {
        return len2;
    }

    public int length(int i, int j, int k) {
        return len3;
    }

    public QDataSet slice(int i) {
        return new ConstantDataSet( rank-1, len1, len2, len3, 1, value );
    }

    public QDataSet trim(int start, int end) {
        return new ConstantDataSet( rank-1, end-start, len1, len2, len3, value );
    }

    public <T> T capability(Class<T> clazz) {
        return null;
    }
    
}
