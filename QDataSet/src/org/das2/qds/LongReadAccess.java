
package org.das2.qds;

/**
 * Provide access to the long array which backs the data.
 * For example, this is used with LongDataSet to 
 * provide access to the original TT2000 values of a 
 * CDF file.
 * 
 * @author jbf
 */
public interface LongReadAccess {
    public long lvalue();
    public long lvalue(int i0);
    public long lvalue(int i0, int i1);
    public long lvalue(int i0, int i1, int i2);
    public long lvalue(int i0, int i1, int i2, int i3);
}
