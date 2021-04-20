/*
 * CompositeMatrix.java
 *
 * Created on March 7, 2005, 8:13 PM
 */

package org.das2.qds.math.matrix;

/** All of the elementary row and column operations are applied to both
 * underlying matrices.  Reads are done from the first matrix.  Writes
 * are not allowed (except those that are side effects of elementary
 * matrix operations).
 *
 * @author Edward West
 */
public class CompositeMatrix extends Matrix {
    
    private Matrix m1;
    private Matrix m2;
    
    public CompositeMatrix(Matrix m1, Matrix m2) {
        super(m1.rowCount(), m1.columnCount());
        if (m1.rowCount() != m2.rowCount() && m1.columnCount() != m2.columnCount()) {
            throw new IllegalArgumentException("m1 and m2 must have the same number of rows and columns");
        }
        this.m1 = m1;
        this.m2 = m2;
    }

    public double get(int row, int col) {
        return m1.get(row, col);
    }

    public void rowTimes(int row, double s) {
        m1.rowTimes(row, s);
        m2.rowTimes(row, s);
    }

    public void rowTimesAddTo(int srcRow, double s, int dstRow) {
        m1.rowTimesAddTo(srcRow, s, dstRow);
        m2.rowTimesAddTo(srcRow, s, dstRow);
    }

    public void set(int row, int col, double d) {
        throw new UnsupportedOperationException("Setting values not supported");
    }

    public void swapRows(int row1, int row2) {
        m1.swapRows(row1, row2);
        m2.swapRows(row1, row2);
    }
    
}
