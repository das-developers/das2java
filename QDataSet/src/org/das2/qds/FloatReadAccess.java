
package org.das2.qds;

/**
 * Provide access to the float array which backs the data.
 * For example, this is used with FloatDataSet to 
 * provide access to the original float values of a CDF to avoid
 * noise from rounding.
 * 
 * @author jbf
 */
public interface FloatReadAccess {
    public float fvalue();
    public float fvalue(int i0);
    public float fvalue(int i0, int i1);
    public float fvalue(int i0, int i1, int i2);
    public float fvalue(int i0, int i1, int i2, int i3);
}
