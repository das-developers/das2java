/*
 * QuadFitUtil.java
 *
 * Created on March 8, 2005, 5:24 PM
 */

package org.das2.math;

import org.das2.math.matrix.ArrayMatrix;
import org.das2.math.matrix.Matrix;
import org.das2.math.matrix.MatrixUtil;
import java.util.Arrays;

/**
 *
 * @author eew
 */
public class QuadFitUtil {
    
    private QuadFitUtil() {}
    
    public static double[] quadfit(double[] x, double[] y, double[] w) {
        return polyfitw(x, y, w, 2);
    }
    
    public static double[] polyfitw(double[] x, double[] y, double[] w, int degree) {
        int n = x.length;
        int m = degree + 1;
        
        Matrix a = new ArrayMatrix(m, m);
        double[] b = new double[m];
        double[] z = new double[n];
        Arrays.fill(z, 1.0);
        
        a.set(0, 0, total(w));
        b[0] = totalMult(w, y);
        
        for (int p = 1; p <= 2*degree; p++) {
            for (int iz = 0; iz < z.length; iz++) {
                z[iz] *= x[iz];
            }
            if (p < m ) {
                b[p] = totalMult(w, y, z);
            }
            double sum = totalMult(w, z);
            int degreeLTp = Math.min(degree, p);
            for (int j = Math.max(0, p - degree); j <= degreeLTp; j++) {
                a.set(j, p - j, sum);
            }
        }
        
        a = MatrixUtil.inverse(a);
        
        double[] c = new double[m];
        
        MatrixUtil.multiply(new ArrayMatrix(b, 1, m), a, new ArrayMatrix(c, 1, m));
        
        return c;
    }
    
    private static double total(double[] a) {
        double total = 0.0;
        for (int i = 0; i < a.length; i++) {
            total += a[i];
        }
        return total;
    }
    
    private static double totalMult(double[] a, double[] b) {
        double total = 0.0;
        for (int i = 0; i < a.length; i++) {
            total += (a[i] * b[i]);
        }
        return total;
    }
    
    private static double totalMult(double[] a, double[] b, double[] c) {
        double total = 0.0;
        for (int i = 0; i < a.length; i++) {
            total += (a[i] * b[i] * c[i]);
        }
        return total;
    }

    /**
     * The peak of the quadradic.
     * y = c0 + c1*x + c2x^2
     * dy/dx = c1 + 2*c2*x
     * for dy/dx == 0, x = -c1/(2*c2)
     */
    public static double quadPeak(double[] c) {
        if (c.length != 3) {
            throw new IllegalArgumentException("c must have a length of 3");
        }
        return -0.5 * c[1] / c[2];
    }
    
    /*
     * The width only depends on the x^2 coefficient.
     * dy is the delta y at which the half width is measured.
     * w = -((-c2*dy)^(1/2)) / c2
     */
    public static double quadHalfWidth(double[] c, double dy) {
        return Math.sqrt(-c[2]*dy) / -c[2];
    }
    
}
