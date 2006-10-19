/*
 * Contour.java
 * See http://www.mactech.com/articles/mactech/Vol.13/13.09/ContourPlottinginJava/index.html
 * Created on May 20, 2004, 7:49 PM
 */

package edu.uiowa.physics.pw.das.math;

import edu.uiowa.physics.pw.das.dataset.*;
import edu.uiowa.physics.pw.das.dataset.test.*;
import edu.uiowa.physics.pw.das.datum.*;
import java.awt.*;
import java.io.*;

/**
 * Contouring based on code published in Javatech, Volume 13 Issue 9, "Contour Plotting In Java"
 * by David Rand.  This code is based on Fortran code implementing 1978 article by W. V. Snyder.
 * W. V. Snyder, "Algorithm 531, Contour plotting [J6]," ACM Trans. Math. Softw. 4, 3 (Sept. 1978), 290-294.
 * @author  Owner
 */
public class Contour {
    
    final static public class ContourPlot {
        
        TableDataSet zz;
        Units zunits;
        long idx; /* monotonically increasing X Value */
        
        public ContourPlot( TableDataSet tds, DatumVector contourValues ) {
            super();
            this.zz= tds;
            
            xSteps = tds.getXLength();
            ySteps = tds.getYLength(0);
            
            zunits= tds.getZUnits();
            
            ncv= contourValues.getLength();
            cv= new float[ncv];
            for ( int i=0; i<ncv; i++ ) {
                cv[i]= (float)contourValues.get(i).doubleValue(zunits);
            }
            
        }
        
        /*
         * returns a VectorDataSet with the contours.  The independent variable is just
         * 1,2,3..., Y is the Z value, and planes "X" and "Y" are the X and Y values 
         */
        
        public VectorDataSet performContour() {        
            VectorDataSetBuilder builder= new VectorDataSetBuilder( Units.dimensionless, zz.getZUnits() );
            builder.setProperty( "xTagWidth", Units.dimensionless.createDatum(1) );
            builder.addPlane( "x", zz.getXUnits() );
            builder.addPlane( "y", zz.getYUnits() );
            
            int workLength = 2 * xSteps * ySteps * ncv;
            boolean[] workSpace= new boolean[workLength];
            
            idx=0;
            ContourPlotKernel( builder, workSpace );
            return builder.toVectorDataSet();
        }
        
        //-------------------------------------------------------
        final int sign(int a, int b) {
            a = Math.abs(a);
            if (b < 0)	return -a;
            else		return  a;
        }
        
        
        // Below, constant data members:
        final static boolean	SHOW_NUMBERS	= true;
        final static int	BLANK		= 32,
        PLOT_MARGIN	= 20,
        WEE_BIT		=  3,
        NUMBER_LENGTH	=  3;
        
        // Below, data members which store the grid steps,
        // the z values, the interpolation flag, the dimensions
        // of the contour plot and the increments in the grid:
        int		xSteps, ySteps;
        boolean		logInterpolation = false;
        Dimension	d;
        double		deltaX, deltaY;
        
        // Below, data members, most of which are adapted from
        // Fortran variables in Snyder's code:
        int	ncv;
        int	l1[] = new int[4];
        int	l2[] = new int[4];
        int	ij[] = new int[2];
        int	i1[] = new int[2];
        int	i2[] = new int[2];
        int	i3[] = new int[6];
        int	ibkey,icur,jcur,ii,jj,elle,ix,iedge,iflag,ni,ks;
        int	cntrIndex,prevIndex;  /* contour index */
        int	idir,nxidir,k;
        double	z1,z2;
        double cval; /* current contour value */
        double zMax,zMin;
        double	intersect[]	= new double[4];
        double	xy[]		= new double[2];
        double	prevXY[]	= new double[2];
        float	cv[]		;   /* contour values */
        boolean	jump;
        
        
        final double getYValue( double findex ) {
            findex--;  /* one is first index */
            int indx= (int)findex;
            if ( indx==zz.getYLength(0) ) indx--;
            double fp= findex - indx;
            double y0= zz.getYTagDouble(0, indx, zz.getYUnits() );
            double y1= zz.getYTagDouble(0, indx+1, zz.getYUnits() );
            return y0 + fp * ( y1 - y0 );
        }
        
        final double getXValue( double findex ) {
            findex--;  /* one is first index */
            int indx= (int)findex;
            if ( indx==zz.getXLength() ) indx--;
            double fp= findex - indx;
            double x0= zz.getXTagDouble( indx, zz.getXUnits() );
            double x1= zz.getXTagDouble( indx+1, zz.getXUnits() );
            return x0 + fp * ( x1 - x0 );
        }
        
        //-------------------------------------------------------
        // "DrawKernel" is the guts of drawing and is called
        // directly or indirectly by "ContourPlotKernel" in order
        // to draw a segment of a contour or to set the pen
        // position "prevXY". Its action depends on "iflag":
        //
        // iflag == 1 means Continue a contour
        // iflag == 2 means Start a contour at a boundary
        // iflag == 3 means Start a contour not at a boundary
        // iflag == 4 means Finish contour at a boundary
        // iflag == 5 means Finish closed contour not at boundary
        // iflag == 6 means Set pen position
        //
        // If the constant "SHOW_NUMBERS" is true then when
        // completing a contour ("iflag" == 4 or 5) the contour
        // index is drawn adjacent to where the contour ends.
        //-------------------------------------------------------
        void DrawKernel(VectorDataSetBuilder dsbuilder) {
            int	prevU,prevV,u,v;
            
            if ((iflag == 1) || (iflag == 4) || (iflag == 5)) {
                
            } else {
                idx++ ;
            }
            
            
            dsbuilder.insertY(idx,cval);
            
            dsbuilder.insertY(idx,getXValue(xy[0]),"x");
            dsbuilder.insertY(idx,getYValue(xy[1]),"y");
            
            idx++;
            
        }
        
        //-------------------------------------------------------
        // "DetectBoundary"
        //-------------------------------------------------------
        final void DetectBoundary() {
            ix = 1;
            if (ij[1-elle] != 1) {
                ii = ij[0] - i1[1-elle];
                jj = ij[1] - i1[elle];
                if ( !zunits.isFill(zz.getDouble(ii-1,jj-1,zunits)) ) {
                    ii = ij[0] + i2[elle];
                    jj = ij[1] + i2[1-elle];
                    if ( !zunits.isFill(zz.getDouble(ii-1,jj-1,zunits)) ) ix = 0;
                }
                if (ij[1-elle] >= l1[1-elle]) {
                    ix = ix + 2;
                    return;
                }
            }
            ii = ij[0] + i1[1-elle];
            jj = ij[1] + i1[elle];
            if ( zunits.isFill( zz.getDouble(ii-1,jj-1,zunits) ) ) {
                ix = ix + 2;
                return;
            }
            if ( zunits.isFill( zz.getDouble(ij[0],ij[1],zunits) ) ) ix = ix + 2;
        }
        
        //-------------------------------------------------------
        // "Routine_label_020" corresponds to a block of code
        // starting at label 20 in Synder's subroutine "GCONTR".
        //-------------------------------------------------------
        boolean Routine_label_020() {
            l2[0] =  ij[0];
            l2[1] =  ij[1];
            l2[2] = -ij[0];
            l2[3] = -ij[1];
            idir = 0;
            nxidir = 1;
            k = 1;
            ij[0] = Math.abs(ij[0]);
            ij[1] = Math.abs(ij[1]);
            if ( zunits.isFill( zz.getDouble(ij[0]-1,ij[1]-1,zunits) ) ) {
                elle = idir % 2;
                ij[elle] = sign(ij[elle],l1[k-1]);
                return true;
            }
            elle = 0;
            return false;
        }
        
        //-------------------------------------------------------
        // "Routine_label_050" corresponds to a block of code
        // starting at label 50 in Synder's subroutine "GCONTR".
        //-------------------------------------------------------
        boolean Routine_label_050() {
            while (true) {
                if (ij[elle] >= l1[elle]) {
                    if (++elle <= 1) continue;
                    elle = idir % 2;
                    ij[elle] = sign(ij[elle],l1[k-1]);
                    if (Routine_label_150()) return true;
                    continue;
                }
                ii = ij[0] + i1[elle];
                jj = ij[1] + i1[1-elle];
                if ( zunits.isFill( zz.getDouble(ii-1,jj-1,zunits) ) ) {
                    if (++elle <= 1) continue;
                    elle = idir % 2;
                    ij[elle] = sign(ij[elle],l1[k-1]);
                    if (Routine_label_150()) return true;
                    continue;
                }
                break;
            }
            jump = false;
            return false;
        }
        
        //-------------------------------------------------------
        // "Routine_label_150" corresponds to a block of code
        // starting at label 150 in Synder's subroutine "GCONTR".
        //-------------------------------------------------------
        boolean Routine_label_150() {
            while (true) {
                //------------------------------------------------
                // Lines from z[ij[0]-1][ij[1]-1]
                //	   to z[ij[0]  ][ij[1]-1]
                //	  and z[ij[0]-1][ij[1]]
                // are not satisfactory. Continue the spiral.
                //------------------------------------------------
                if (ij[elle] < l1[k-1]) {
                    ij[elle]++;
                    if (ij[elle] > l2[k-1]) {
                        l2[k-1] = ij[elle];
                        idir = nxidir;
                        nxidir = idir + 1;
                        k = nxidir;
                        if (nxidir > 3) nxidir = 0;
                    }
                    ij[0] = Math.abs(ij[0]);
                    ij[1] = Math.abs(ij[1]);
                    if ( zunits.isFill( zz.getDouble(ij[0]-1,ij[1]-1,zunits) ) ) {
                        elle = idir % 2;
                        ij[elle] = sign(ij[elle],l1[k-1]);
                        continue;
                    }
                    elle = 0;
                    return false;
                }
                if (idir != nxidir) {
                    nxidir++;
                    ij[elle] = l1[k-1];
                    k = nxidir;
                    elle = 1 - elle;
                    ij[elle] = l2[k-1];
                    if (nxidir > 3) nxidir = 0;
                    continue;
                }
                
                if (ibkey != 0) return true;
                ibkey = 1;
                ij[0] = icur;
                ij[1] = jcur;
                if (Routine_label_020()) continue;
                return false;
            }
        }
        
        //-------------------------------------------------------
        // "Routine_label_200" corresponds to a block of code
        // starting at label 200 in Synder's subroutine "GCONTR".
        // It has return values 0, 1 or 2.
        //-------------------------------------------------------
        short Routine_label_200(VectorDataSetBuilder dsbuilder,  boolean workSpace[]) {
            while (true) {
                xy[elle] = 1.0*ij[elle] + intersect[iedge-1];
                xy[1-elle] = 1.0*ij[1-elle];
                workSpace[2*(xSteps*(ySteps*cntrIndex+ij[1]-1)
                +ij[0]-1) + elle] = true;
                DrawKernel(dsbuilder);
                if (iflag >= 4) {
                    icur = ij[0];
                    jcur = ij[1];
                    return 1;
                }
                ContinueContour();
                if (!workSpace[2*(xSteps*(ySteps*cntrIndex
                +ij[1]-1)+ij[0]-1)+elle]) return 2;
                iflag = 5;		// 5. Finish a closed contour
                iedge = ks + 2;
                if (iedge > 4) iedge = iedge - 4;
                intersect[iedge-1] = intersect[ks-1];
            }
        }
        
        //-------------------------------------------------------
        // "CrossedByContour" is true iff the current segment in
        // the grid is crossed by one of the contour values and
        // has not already been processed for that value.
        //-------------------------------------------------------
        boolean CrossedByContour(boolean workSpace[]) {
            ii = ij[0] + i1[elle];
            jj = ij[1] + i1[1-elle];
            z1 = zz.getDouble(ij[0]-1,ij[1]-1,zunits);
            z2 = zz.getDouble(ii-1,jj-1,zunits);
            for (cntrIndex = 0; cntrIndex < ncv; cntrIndex++) {
                int i = 2*(xSteps*(ySteps*cntrIndex+ij[1]-1) + ij[0]-1) + elle;
                
                if (!workSpace[i]) {
                    float x = cv[cntrIndex];
                    if ((x>Math.min(z1,z2)) && (x<=Math.max(z1,z2))) {
                        workSpace[i] = true;
                        return true;
                    }
                }
            }
            return false;
        }
        
        //-------------------------------------------------------
        // "ContinueContour" continues tracing a contour. Edges
        // are numbered clockwise, the bottom edge being # 1.
        //-------------------------------------------------------
        void ContinueContour() {
            short local_k;
            
            ni = 1;
            if (iedge >= 3) {
                ij[0] = ij[0] - i3[iedge-1];
                ij[1] = ij[1] - i3[iedge+1];
            }
            for (local_k = 1; local_k < 5; local_k++)
                if (local_k != iedge) {
                    ii = ij[0] + i3[local_k-1];
                    jj = ij[1] + i3[local_k];
                    z1 = zz.getDouble(ii-1,jj-1,zunits);
                    ii = ij[0] + i3[local_k];
                    jj = ij[1] + i3[local_k+1];
                    z2 = zz.getDouble(ii-1,jj-1,zunits);
                    if ((cval > Math.min(z1,z2) && (cval <= Math.max(z1,z2)))) {
                        if ((local_k == 1) || (local_k == 4)) {
                            double	zz = z2;
                            
                            z2 = z1;
                            z1 = zz;
                        }
                        intersect[local_k-1] = (cval - z1)/(z2 - z1);
                        ni++;
                        ks = local_k;
                    }
                }
            if (ni != 2) {
                //-------------------------------------------------
                // The contour crosses all 4 edges of cell being
                // examined. Choose lines top-to-left & bottom-to-
                // right if interpolation point on top edge is
                // less than interpolation point on bottom edge.
                // Otherwise, choose the other pair. This method
                // produces the same results if axes are reversed.
                // The contour may close at any edge, but must not
                // cross itself inside any cell.
                //-------------------------------------------------
                ks = 5 - iedge;
                if (intersect[2] >= intersect[0]) {
                    ks = 3 - iedge;
                    if (ks <= 0) ks = ks + 4;
                }
            }
            //----------------------------------------------------
            // Determine whether the contour will close or run
            // into a boundary at edge ks of the current cell.
            //----------------------------------------------------
            elle = ks - 1;
            iflag = 1;		// 1. Continue a contour
            jump = true;
            if (ks >= 3) {
                ij[0] = ij[0] + i3[ks-1];
                ij[1] = ij[1] + i3[ks+1];
                elle = ks - 3;
            }
        }
        
        //-------------------------------------------------------
        // "ContourPlotKernel" is the guts of this class and
        // corresponds to Synder's subroutine "GCONTR".
        //-------------------------------------------------------
        void ContourPlotKernel(VectorDataSetBuilder dsbuilder,	boolean workSpace[]) {
            short val_label_200;
            
            l1[0] = xSteps;	l1[1] = ySteps;
            l1[2] = -1;l1[3] = -1;
            i1[0] =	1; i1[1] =  0;
            i2[0] =	1; i2[1] = -1;
            i3[0] =	1; i3[1] =  0; i3[2] = 0;
            i3[3] =	1; i3[4] =  1; i3[5] = 0;
            prevXY[0] = 0.0; prevXY[1] = 0.0;
            xy[0] = 1.0; xy[1] = 1.0;
            cntrIndex = 0;
            prevIndex = -1;
            iflag = 6;
            DrawKernel(dsbuilder);
            icur = Math.max(1, Math.min((int)Math.floor(xy[0]), xSteps));
            jcur = Math.max(1, Math.min((int)Math.floor(xy[1]), ySteps));
            ibkey = 0;
            ij[0] = icur;
            ij[1] = jcur;
            if (Routine_label_020() &&
            Routine_label_150()) return;
            if (Routine_label_050()) return;
            while (true) {
                DetectBoundary();
                if (jump) {
                    if (ix != 0) iflag = 4; // Finish contour at boundary
                    iedge = ks + 2;
                    if (iedge > 4) iedge = iedge - 4;
                    intersect[iedge-1] = intersect[ks-1];
                    val_label_200 = Routine_label_200(dsbuilder,workSpace);
                    if (val_label_200 == 1) {
                        if (Routine_label_020() && Routine_label_150()) return;
                        if (Routine_label_050()) return;
                        continue;
                    }
                    if (val_label_200 == 2) continue;
                    return;
                }
                if ((ix != 3) && (ix+ibkey != 0) && CrossedByContour(workSpace)) {
                    //
                    // An acceptable line segment has been found.
                    // Follow contour until it hits a
                    // boundary or closes.
                    //
                    iedge = elle + 1;
                    cval = cv[cntrIndex];
                    if (ix != 1) iedge = iedge + 2;
                    iflag = 2 + ibkey;
                    intersect[iedge-1] = (cval - z1) / (z2 - z1);
                    val_label_200 = Routine_label_200(dsbuilder,workSpace);
                    if (val_label_200 == 1) {
                        if (Routine_label_020() && Routine_label_150()) return;
                        if (Routine_label_050()) return;
                        continue;
                    }
                    if (val_label_200 == 2) continue;
                    return;
                }
                if (++elle > 1) {
                    elle = idir % 2;
                    ij[elle] = sign(ij[elle],l1[k-1]);
                    if (Routine_label_150()) return;
                }
                if (Routine_label_050()) return;
            }
        }
    }
    
    public static VectorDataSet contour( TableDataSet tds, DatumVector levels ) {
        if ( tds.tableCount()>1 ) {
            throw new IllegalArgumentException("TableDataSet can only have one table");
        }
        Contour.ContourPlot cp= new ContourPlot( tds, levels );
        return cp.performContour();
    }
    
    public static VectorDataSet contour( TableDataSet tds, Datum level ) {
        if ( tds.tableCount()>1 ) {
            throw new IllegalArgumentException("TableDataSet can only have one table");
        }
        Units units= level.getUnits();
        double value= level.doubleValue(units);
        Contour.ContourPlot cp= new ContourPlot( tds, DatumVector.newDatumVector( new double[] { value }, units ) );
        return cp.performContour();
    }
    
    
}
