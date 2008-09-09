/*
 * DistTableDataSet.java
 *
 * Created on May 21, 2004, 2:58 PM
 */

package org.das2.dataset.test;

/**
 *
 * @author  Jeremy
 */
public class DistTableDataSet extends FunctionTableDataSet {
/*    on_error,2              ;Return to caller if an error occurs
x=findgen(n)            ;Make a row
x = (x < (n-x)) ^ 2     ;column squares
if n_elements(m) le 0 then m = n

a = FLTARR(n,m,/NOZERO) ;Make array

for i=0L, m/2 do begin  ;Row loop
        y = sqrt(x + i^2) ;Euclidian distance
        a[0,i] = y      ;Insert the row
        if i ne 0 then a[0, m-i] = y ;Symmetrical
        endfor
return,a
end */

    /** Creates a new instance of DistTableDataSet */
    public DistTableDataSet( int size ) {
        super( size,size );
        fillCache();
    }
    
    public double getDoubleImpl(int i, int j, org.das2.datum.Units units) {
        int m= xtags/2;
        int n= ytags/2;         
        if ( i<m ) {
            if ( j<n ) {
                return Math.sqrt( (i)*(i) + (j)*(j) );
            } else {
                return Math.sqrt( (i)*(i) + (ytags-j)*(ytags-j) );
            }
        } else {
            if ( j<n ) {
                return Math.sqrt( (xtags-i)*(xtags-i) + (j)*(j) );
            } else {
                return Math.sqrt( (xtags-i)*(xtags-i) + (ytags-j)*(ytags-j) );
            }
        }
        
    }
        
}
