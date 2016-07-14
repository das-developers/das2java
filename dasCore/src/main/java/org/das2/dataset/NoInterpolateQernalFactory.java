/*
 * NNQernalFactory.java
 *
 * Created on October 7, 2005, 12:06 PM
 *
 *
 */

package org.das2.dataset;

import org.das2.dataset.QernalTableRebinner.Qernal;
import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.util.DasMath;

/**
 *
 * @author Jeremy
 */
public class NoInterpolateQernalFactory implements QernalTableRebinner.QernalFactory {
    
    class EnlargeQernal implements QernalTableRebinner.Qernal {
        int dx0,dx1;
        int dy0,dy1;
        int nx,ny;     // number of elements in each dimension
        private EnlargeQernal( int dx0, int dx1, int dy0, int dy1, int nx, int ny ) {
            this.dx0= dx0;
            this.dx1= dx1;
            this.dy0= dy0;
            this.dy1= dy1;
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
                    ss[i][j]+= value * weight;
                    ww[i][j]+= weight;
                }
            }
        }
    }
    
    // special case for 1-pixel Quernal
    class NoEnlargeQernal implements QernalTableRebinner.Qernal {
        int nx, ny;
        private NoEnlargeQernal( int nx, int ny ) {
            this.nx= nx;
            this.ny= ny;
        }
        public void apply( int x, int y, double value, double weight, double[][]ss, double[][]ww ) {
            if ( x>=0 && x<nx && y>=0 && y<ny ) {
                ss[x][y]+= value * weight;
                ww[x][y]+= weight;
            }
        }
    }
    
    public Qernal getQernal( RebinDescriptor ddx, RebinDescriptor ddy, Datum xTagWidth, Datum yTagWidth ) {
        //return new NoEnlargeQernal( ddx.numberOfBins(), ddy.numberOfBins() );
        return new EnlargeQernal( 1, 1, 1, 1, ddx.numberOfBins(), ddy.numberOfBins() );        
    }
}
