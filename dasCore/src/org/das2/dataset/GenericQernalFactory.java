/*
 * GenericQernalFactory.java
 *
 * Created on October 7, 2005, 1:58 PM
 *
 *
 */

package org.das2.dataset;

import org.das2.dataset.QernalTableRebinner.Qernal;
import org.das2.datum.Datum;

/**
 *
 * @author Jeremy
 */
public class GenericQernalFactory implements QernalTableRebinner.QernalFactory {
    class GenericQernal implements QernalTableRebinner.Qernal {
        double[][] qernal;
        int dx0,dx1;
        int dy0,dy1;
        int nx, ny;
        GenericQernal( double[][] qernal, int dx0, int dy0, int nx, int ny ) {
            this.qernal= qernal;
            this.dx0= dx0;
            this.dy0= dy0;
            this.dy1= qernal[0].length-dy0-1;
            this.dx1= qernal.length-dx0-1;
            this.nx= nx;
            this.ny= ny;
        }
        
        public void apply( int x, int y, double value, double weight, double[][]ss, double[][]ww ) {
            int x0,x1;
            int y0,y1;
            x0= x-dx0;
            x1= x+dx1+1;
            y0= y-dy0;
            y1= y+dy1+1;
            if (x0<0) x0=0; else if (x0>nx) x0=nx;  // trim to visible portion
            if (x1<0) x1=0; else if (x1>nx) x1=nx;
            if (y0<0) y0=0; else if (y0>ny) y0=ny;
            if (y1<0) y1=0; else if (y1>ny) y1=ny;
            
            for ( int i=x0; i<x1; i++ ) {
                for ( int j=y0; j<y1; j++ ) {
                    if ( weight > ww[i][j] ) {
                        try {
                            double w= weight * qernal[i-x+dx0][j-y+dy0];
                            ss[i][j]+= value * w;
                            ww[i][j]+= w;
                        } catch ( ArrayIndexOutOfBoundsException e ) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            
        }
    }
    
    public QernalTableRebinner.Qernal getQernal( RebinDescriptor ddx, RebinDescriptor ddy, Datum xTagWidth, Datum yTagWidth ) {
        double[][] qernal= new double[5][5];
        qernal[1][4]= 1.0;
        qernal[3][4]= 1.0;
        qernal[0][1]= 1.0;
        qernal[4][1]= 1.0;
        qernal[1][0]= 1.0;
        qernal[2][0]= 1.0;
        qernal[3][0]= 1.0;        
        return new GenericQernal( qernal, 2, 2, ddx.numberOfBins(), ddy.numberOfBins() );
    }
    
}
