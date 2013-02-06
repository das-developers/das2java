/*
 * ContourMeDataSet.java
 *
 * Created on June 24, 2004, 9:23 PM
 */

package org.das2.dataset.test;

/**
 *
 * @author  jbf
 */
public class ContourMeDataSet extends FunctionTableDataSet {
    
    /** Creates a new instance of ContourMeDataSet */
    public ContourMeDataSet() {
        //xtags=101;
        //ytags=101;
        super(31,31);
        xtags=31;
        ytags=31;
        fillCache();
    }
    
    public double getDoubleImpl(int i, int j, org.das2.datum.Units units) {
        i= i - this.xtags / 2;
        j= j - this.ytags / 2;
        double d= (i*i+j*j);
        //double a= Math.atan2(j,i);
        //return d + xtags/40*Math.cos( a * 20 );
        return d;
    }
    
}
