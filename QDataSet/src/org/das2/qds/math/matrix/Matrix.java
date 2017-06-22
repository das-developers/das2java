/*
 * Matrix.java
 *
 * Created on March 7, 2005, 6:33 PM
 */

package org.das2.qds.math.matrix;

import java.text.DecimalFormat;

/**
 *
 * @author eewest
 */
public abstract class Matrix {
    
    protected final int nRow;
    
    protected final int nCol;
    
    protected Matrix(int rows, int columns) {
        nRow = rows;
        nCol = columns;
    }
    
    public int rowCount() {
        return nRow;
    }
    
    public int columnCount() {
        return nCol;
    }
    
    public void copy(Matrix m) {
        for (int iRow = 0; iRow < nRow; iRow++) {
            for (int iCol = 0; iCol < nCol; iCol++) {
                set(iRow, iCol, m.get(iRow, iCol));
            }
        }
    }
    
    public abstract double get(int row, int col);
    
    public abstract void set(int row, int col, double d);
    
    public abstract void swapRows(int row1, int row2);
    
    public abstract void rowTimes(int row, double s);
    
    public abstract void rowTimesAddTo(int srcRow, double s, int dstRow);
    
    public String toString() {
        StringBuffer buf= new StringBuffer();
        buf.append("\n");
        for ( int i=0; i<rowCount(); i++ ) {
            for ( int j=0; j<columnCount(); j++ ) {
                buf.append( "\t" + get(i,j)  );
            }
            buf.append( "\n" );
        }
        return buf.toString();
    }
}
