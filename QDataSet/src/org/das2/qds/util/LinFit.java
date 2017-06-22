package org.das2.qds.util;

import org.das2.datum.Datum;
import org.das2.datum.Units;
import org.das2.qds.DataSetUtil;
import org.das2.qds.FDataSet;
import org.das2.qds.QDataSet;
import org.das2.qds.SemanticOps;

/**
 * Linear Fit routine.  This will allow errors on the Y values, and
 * will report chi squared.
 * 
 * Borrowed from pamguard, https://sourceforge.net/projects/pamguard/.
 * @author Doug Gillespie
 * Simple linear regression. Fitting line y = a + bx
 * 
 * Modified to use QDataSet, Jeremy Faden
 * Singleton class
 */
public class LinFit {

    private static final int ITMAX = 100;
    private static final double EPS = 3.0e-7;
    private static final double FPMIN = 1.0e-30;
    private double a,  b,  siga,  sigb,  chi2,  q;
    private QDataSet x,  y,  sig;
    private int nData;
    private boolean doneErrors;
    double wt, t, sxoss, sx = 0., sy = 0., st2 = 0., ss, sigdat;
    int mwt = 0;

    /**
     * do fit with uniform weights or weight=0 where fill is found.
     * @param x
     * @param y 
     */
    public LinFit( QDataSet x, QDataSet y ) {
        FDataSet wds= FDataSet.createRank1( x.length() );
        QDataSet wdsx= DataSetUtil.weightsDataSet(x);
        QDataSet wdsy= DataSetUtil.weightsDataSet(y);
        for ( int i=0; i<x.length(); i++ ) {
            wds.putValue( i, ( wdsx.value(i)==0 || wdsy.value(i)==0 ? 0 : 1 ) );
        }
        doFit(x, y, wds );
    }

    /**
     * do fit with weights.  X and Y must not contain fill. where sig&gt;0.
     * @param x the x data
     * @param y the y data
     * @param sig the error bar, or zero for fill.
     */
    public LinFit( QDataSet x, QDataSet y, QDataSet sig ) {
        doFit(x, y, sig);
    }

    private void doFit( QDataSet x, QDataSet y, QDataSet sig ) {
        if ( x.rank()!=1 || y.rank()!=1 ) {
            throw new IllegalArgumentException("x and y must be rank 1");
        }
        this.x = x;
        this.y = y;
        this.nData = x.length();
        this.sig = sig;
        doneErrors = false;

        int i;
        if (sig != null) {
            mwt = nData;
        } else {
            mwt= 0;
        }
        b = 0.;
        if (mwt > 0) {                 // Accumalative sums
            ss = 0.;
            for (i = 0; i < nData; i++) {     // with weights
                if ( sig.value(i)>0 ) {
                    wt = 1. / SQR(sig.value(i));
                    ss += wt;
                    sx += x.value(i) * wt;
                    sy += y.value(i) * wt;
                }
            }
        } else {
            for (i = 0; i < nData; i++) {
                sx += x.value(i);                    // without weights
                sy += y.value(i);
            }
            ss = nData;
        }
        sxoss = sx / ss;
        if (mwt > 0) {
            for (i = 0; i < nData; i++) {
                t = (x.value(i) - sxoss) / sig.value(i);
                st2 += t * t;
                b += t * y.value(i) / sig.value(i);
            }
        } else {
            for (i = 0; i < nData; i++) {
                t = x.value(i) - sxoss;
                st2 += t * t;
                b += t * y.value(i);
            }
        }
        b /= st2;
        a = (sy - sx * b) / ss;
    }

    private void doErrors() {
        int i;
        siga = Math.sqrt((1. + sx * sx / (ss * st2)) / ss);
        sigb = Math.sqrt(1. / st2);
        chi2 = q = 0.;
        if (nData <= 2) {
            return;
        }
        if (mwt == 0) {
            for (i = 0; i < nData; i++) {
                chi2 += SQR(y.value(i) - (a) - (b) * x.value(i));
            }
            q = 1.;
            sigdat = Math.sqrt((chi2) / (nData - 2));
            siga *= sigdat;
            sigb *= sigdat;
        } else {
            for (i = 0; i < nData; i++) {
                chi2 += SQR((y.value(i) - a - b * x.value(i)) / sig.value(i));
            }
            q = gammq(0.5 * (nData - 2), 0.5 * (chi2));
        }
        doneErrors = true;
    }

    /**
     * return the result A of the fit y = A + B * x
     * @return the result A of the fit y = A + B * x
     */
    public double getA() {
        if (doneErrors == false) {
            doErrors();
        }
        return a;
    }

    /**
     * return the result B of the fit y = A + B * x
     * @return the result B of the fit y = A + B * x
     */
    public double getB() {
        if (doneErrors == false) {
            doErrors();
        }
        return b;
    }
	
	/**
	 * return the slope as a datum with units of Yunits/Xunits.  Note
	 * the current version of the library is unable to do many unit 
	 * calculations.
	 * @return 
	 */
	public Datum getSlope() {
		Units xunits= SemanticOps.getUnits(x).getOffsetUnits();
		Units yunits= SemanticOps.getUnits(y).getOffsetUnits();
		return yunits.divide( getB(), 1, xunits );
	}

	/**
	 * return the intercept as a datum with units of y.
	 * @return 
	 */
	public Datum getIntercept() {
		Units yunits= SemanticOps.getUnits(y).getOffsetUnits();
		return yunits.createDatum( getA() );		
	}
	
    /**
     * return Chi-Squared result from the fit.
     */
    public double getChi2() {
        if (doneErrors == false) {
            doErrors();
        }
        return chi2;
    }

    public double getQ() {
        if (doneErrors == false) {
            doErrors();
        }
        return q;
    }

    public double getSiga() {
        if (doneErrors == false) {
            doErrors();
        }
        return siga;
    }

    public double getSigb() {
        if (doneErrors == false) {
            doErrors();
        }
        return sigb;
    }

    double gammq(double a, double x) {
        // incomplete gamma function Q(a,x) = 1 - P(a,x)
        Double gamser = 0.;
        Double gammcf = 0.;
        Double gln = 0.;

        if (x < 0. || a <= 0.) {
//			MessageBox(NULL, "Invalid arguments in Gamma function",
//			"Routine gammq", MB_ICONHAND);
            return 0;
        }
        if (x < (a + 1.0)) {
            gamser= gser(gamser, a, x, gln);
            return 1.0 - gamser;
        } else {
            gammcf= gcf(gammcf, a, x, gln);
            return gammcf;
        }
    }

    double gser(Double gamser, double a, double x, Double gln) {
        int n;
        double sum, del, ap;

        gln = gammln(a);
        if (x <= 0.0) {
            if (x < 0.0) throw new IllegalArgumentException("x less than 0 in routine gser");
            gamser = 0.0;
            return gamser;
        } else {
            ap = a;
            del = sum = 1.0 / a;
            for (n = 1; n <= ITMAX; n++) {
                ++ap;
                del *= x / ap;
                sum += del;
                if (Math.abs(del) < Math.abs(sum) * EPS) {
                    gamser = sum * Math.exp(-x + a * Math.log(x) - (gln));
                    return gamser;
                }
            }
            throw new IllegalArgumentException( "a too large, ITMAX too small in routine gser" );
        }
    }

    double gcf(Double gammcf, double a, double x, Double gln) {
        int i;
        double an, b, c, d, del, h;

        gln = gammln(a);
        b = x + 1.0 - a;
        c = 1.0 / FPMIN;
        d = 1.0 / b;
        h = d;
        for (i = 1; i <= ITMAX; i++) {
            an = -i * (i - a);
            b += 2.0;
            d = an * d + b;
            if (Math.abs(d) < FPMIN) {
                d = FPMIN;
            }
            c = b + an / c;
            if (Math.abs(c) < FPMIN) {
                c = FPMIN;
            }
            d = 1.0 / d;
            del = d * c;
            h *= del;
            if (Math.abs(del - 1.0) < EPS) {
                break;
            }
        }
        //if (i > ITMAX) nrerror("a too large, ITMAX too small in gcf");
        gammcf = Math.exp(-x + a * Math.log(x) - (gln)) * h;
        return gammcf;
    }

    double gammln(double xx) {
        double x, y, tmp, ser;
        final double[] cof = {76.18009172947146, -86.50532032941677,
            24.01409824083091, -1.231739572450155,
            0.1208650973866179e-2, -0.5395239384953e-5
        };
        int j;

        y = x = xx;
        tmp = x + 5.5;
        tmp -= (x + 0.5) * Math.log(tmp);
        ser = 1.000000000190015;
        for (j = 0; j <= 5; j++) {
            ser += cof[j] / ++y;
        }
        return -tmp + Math.log(2.5066282746310005 * ser / x);
    }

    private final double SQR(double f) {
        return f * f;
    }
    
}