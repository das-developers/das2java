/*
 * ArrayMatrix.java
 *
 * Created on March 7, 2005, 6:48 PM
 */

package org.das2.qds.math.matrix;

/**
 *
 * @author eewest
 */
public class ArrayMatrix extends Matrix {
    
    private double[] array;
    
    public ArrayMatrix(int rows, int columns) {
        super(rows, columns);
        this.array = new double[rows * columns];
    }
    
    public ArrayMatrix(double[] array, int rows, int columns) {
        super(rows, columns);
        this.array = array;
        if (rows * columns != array.length) {
            throw new IllegalArgumentException("Array must be (rows * columns) in length");
        }
    }
    
    public ArrayMatrix(Matrix m) {
        super(m.rowCount(), m.columnCount());
        if (m instanceof ArrayMatrix) {
            array = (double[])((ArrayMatrix)m).array.clone();
        }
        else {
            array = new double[nRow * nCol];
            super.copy(m);
        }
    }
    
    public void copy(Matrix m) {
        if (nRow != m.rowCount() || nCol != m.columnCount()) {
            throw new IllegalArgumentException();
        }
        
        if (m instanceof ArrayMatrix) {
            System.arraycopy(((ArrayMatrix)m).array, 0, array, 0, array.length);
        }
        else {
            super.copy(m);
        }
    }

    public double get(int row, int col) {
        return array[row * nCol + col];
    }

    public void rowTimes(int row, double s) {
        for (int iCol = 0; iCol < nCol; iCol++) {
            array[iCol + row * nCol] *= s;
        }
    }

    public void rowTimesAddTo(int srcRow, double s, int dstRow) {
        double[] tmp;
        
        if (srcRow == dstRow) {
            rowTimes(srcRow, s + 1);
        }
        
        for (int iCol = 0; iCol < nCol; iCol++) {
            array[iCol + dstRow * nCol] += array[iCol + srcRow * nCol] * s;
        }
    }

    public void set(int row, int col, double d) {
        array[col + row * nCol] = d;
    }

    public void swapRows(int row1, int row2) {
        double[] tmp;
        int off1, off2;
        
        if (row1 == row2) {
            return;
        }
        
        tmp = new double[nCol];
        off1 = row1 * nCol;
        off2 = row2 * nCol;
        System.arraycopy(array, off1, tmp, 0, nCol);
        System.arraycopy(array, off2, array, off1, nCol);
        System.arraycopy(tmp, 0, array, off2, nCol);
    }
    
}
