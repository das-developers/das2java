/*
 * NNQernalFactory.java
 *
 * Created on October 7, 2005, 12:06 PM
 *
 *
 */

package edu.uiowa.physics.pw.das.dataset;

import edu.uiowa.physics.pw.das.dataset.QernalTableRebinner.Qernal;
import edu.uiowa.physics.pw.das.datum.Datum;
import edu.uiowa.physics.pw.das.datum.Units;
import edu.uiowa.physics.pw.das.datum.UnitsUtil;
import edu.uiowa.physics.pw.das.util.DasMath;

/**
 *
 * @author Jeremy
 */
public class NNQernalFactory implements QernalTableRebinner.QernalFactory {
    
    class NNQernal implements QernalTableRebinner.Qernal {
        int dx0,dx1;
        int dy0,dy1;
        int nx,ny;     // number of elements in each dimension 
        private NNQernal( int dx0, int dx1, int dy0, int dy1, int nx, int ny ) {
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
                    if ( weight > ww[i][j] ) {
                        ss[i][j]= value * weight;
                        ww[i][j]= weight;
                    }
                }
            }
        }
    }
    
    // special case for 1-pixel Quernal
    class NNQernalOne implements QernalTableRebinner.Qernal {
        int nx, ny;
        private NNQernalOne( int nx, int ny ) {
            this.nx= nx;
            this.ny= ny;
        }
        public void apply( int x, int y, double value, double weight, double[][]ss, double[][]ww ) {
            if ( x>=0 && x<nx && y>=0 && y<ny && weight > ww[x][y] ) {
                ss[x][y]= value * weight;
                ww[x][y]= weight;
            }
        }
    }
    
    public Qernal getQernal( RebinDescriptor ddx, RebinDescriptor ddy, Datum xTagWidth, Datum yTagWidth ) {
        Datum d= ddx.binCenter(0);
        int i= ddx.whichBin( d.add(xTagWidth).doubleValue(d.getUnits()), d.getUnits() );
        int dx0= i/2;
        int dx1= i/2;
        int dy0,dy1;
        if ( UnitsUtil.isRatiometric(yTagWidth.getUnits()) ) {
            if (!ddy.isLog() ) throw new IllegalArgumentException("need log axis");
            d= ddy.binCenter(0);
            double f= yTagWidth.doubleValue( Units.log10Ratio );
            i= ddy.whichBin( d.multiply( DasMath.exp10(f) ).doubleValue(d.getUnits()), d.getUnits() );
            dy0= i/2;
            dy1= (i+1)/2;
        } else {
            d= ddy.binCenter(0);
            i= ddy.whichBin( d.add(yTagWidth).doubleValue(d.getUnits()), d.getUnits() );
            dy0= i/2;
            dy1= (i+1)/2;
        }
        if ( dx0==0 && dx1==0 && dy0==0 && dy1==0 ) {
            return new NNQernalOne( ddx.numberOfBins(), ddy.numberOfBins() );
        } else {
            return new NNQernal( dx0, dx1, dy0, dy1, ddx.numberOfBins(), ddy.numberOfBins() );
        }
    }
}
