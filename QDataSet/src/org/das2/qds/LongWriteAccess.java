
package org.das2.qds;

/**
 * Provide write access to the long array which backs the data.
 * For example, this could be used with LongDataSet to 
 * provide access to the original TT2000 values of a 
 * CDF file.
 * 
 * @author jbf
 */
public interface LongWriteAccess {
    public void putLValue( long value );
    public void putLValue(int i0, long value);
    public void putLValue(int i0, int i1, long value);
    public void putLValue(int i0, int i1, int i2, long value);
    public void putLValue(int i0, int i1, int i2, int i3, long value);
}
