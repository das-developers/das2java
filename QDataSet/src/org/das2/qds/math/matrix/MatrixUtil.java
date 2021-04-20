/*
 * MatrixUtil.java
 *
 * Created on March 7, 2005, 10:38 AM
 */

package org.das2.qds.math.matrix;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 *
 * @author eew
 */
public final class MatrixUtil {
    
    private static final DecimalFormat format = new DecimalFormat(" 0.0##;-");
    
    /** Creates a new instance of MatrixUtil */
    private MatrixUtil() {
    }
    
    public static void print(Matrix m, PrintStream out) {
        int nRow = m.rowCount();
        int nCol = m.columnCount();
        for (int iRow = 0; iRow < nRow; iRow++) {
            out.print("[");
            for (int iCol = 0; iCol < nCol; iCol++) {
                out.print(format.format(m.get(iRow, iCol)));
                out.print('\t');
            }
            out.println("]");
        }
    }
    
    public static Matrix inverse(Matrix m) {
        Matrix orig;
        Matrix inv;
        Matrix both;
        int nRow;
        
        if (m.columnCount() != m.rowCount()) {
            throw new IllegalArgumentException("m must be a square matrix");
        }
        
        nRow = m.rowCount();
        orig = new ArrayMatrix(m);
        inv = identity(nRow);
        both = new CompositeMatrix(orig, inv);
        
        for (int iRow = 0; iRow < nRow; iRow++) {
            if (both.get(iRow, iRow) == 0.0) {
                pivot(both, iRow);
            }
            both.rowTimes(iRow, 1.0 / both.get(iRow, iRow));
            for (int i = 0; i < nRow; i++) {
                if (i != iRow) {
                    double scale = -both.get(i, iRow);
                    both.rowTimesAddTo(iRow, scale, i);
                }
            }
        }
        
        return inv;
    }
    
    public static void pivot(Matrix m, final int row) {
        int nRow = m.rowCount();
        for (int iRow = row + 1; iRow < nRow; iRow++) {
            if (m.get(iRow, row) != 0.0) {
                m.swapRows(row, iRow);
                return;
            }
            print(m, System.err);
            throw new IllegalArgumentException("Can't pivot");
        }
    }
    
    public static Matrix identity(final int rows) {
        Matrix m = new ArrayMatrix(new double[rows * rows], rows, rows);
        for (int iRow = 0; iRow < rows; iRow++) {
            m.set(iRow, iRow, 1.0);
        }
        return m;
    }
    
    public static Matrix multiply(Matrix m1, Matrix m2) {
        Matrix res = new ArrayMatrix(m1.rowCount(), m2.columnCount());
        multiply(m1, m2, res);
        return res;
    }
    
    public static void multiply(Matrix m1, Matrix m2, Matrix res) {
        int nRow, nCol, nInner;
        
        if (m1.columnCount() != m2.rowCount()) {
            throw new IllegalArgumentException("");
        }
        
        nRow = m1.rowCount();
        nCol = m2.columnCount();
        nInner = m1.columnCount();
        
        if (nRow != res.rowCount() || nCol != res.columnCount());
        
        for (int iRow = 0; iRow < nRow; iRow++) {
            for (int iCol = 0; iCol < nCol; iCol++) {
                double d = 0;
                for (int iInner = 0; iInner < nInner; iInner++) {
                    d += m1.get(iRow, iInner) * m2.get(iInner, iCol);
                }
                res.set(iRow, iCol, d);
            }
        }
    }

}
