/*
 * SineWaveTable.java
 *
 * Created on December 7, 2004, 1:41 PM
 */

package org.das2.dataset.test;

/**
 *
 * @author  Jeremy
 */
public class SineWaveTable extends FunctionTableDataSet {
    
    /** Creates a new instance of SineWaveTable */
    public SineWaveTable( int size ) {        
        super(size,size);
        this.xtags= size;
        this.ytags= 10;
    }    
    
    public double getDoubleImpl(int i, int j, org.das2.datum.Units units) {
        return Math.sin( i/25.*2*Math.PI + j*1/3.*Math.PI );
    }
    
}
