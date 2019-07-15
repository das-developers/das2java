/*
 * PoissonDistribution.java
 *
 * Created on October 10, 2005, 7:49 PM
 *
 * Adapted from stocc.cpp.  Agner Fog GNU Public License.
 */

package org.das2.math;

import java.util.Random;

/**
 * Class for generating numbers from a Poisson distribution.
 * @author Jeremy
 */
public class PoissonDistribution {
    
    private static Fac fac= new Fac();
    
    private static class Fac {
        private final static int FAK_LEN=1024;
        private final double[] fac_table;
        private static boolean initialized= false;
        private static final double        // coefficients in Stirling approximation
                C0 =  0.918938533204672722,   // ln(sqrt(2*pi))
                C1 =  1./12.,
                C3 = -1./360.;
        Fac() {
            fac_table= new double[FAK_LEN]; // table of ln(n!):
        }
        
        private void init() {
            double sum = fac_table[0] = 0.;
            for (int i=1; i<FAK_LEN; i++) {
                sum += Math.log(i);
                fac_table[i] = sum;
            }
            initialized = true;
        }
        
        // log factorial function. gives natural logarithm of n!
        public double lnFac(int n) {
            if (n < FAK_LEN) {
                if (n <= 1) {
                    if (n < 0) throw new IllegalArgumentException("Parameter negative in LnFac function");
                    return 0;
                }
                if (!initialized) { // first time. Must initialize table
                    init();
                }
                return fac_table[n];
            } else {
                // not found in table. use Stirling approximation
                double  n1, r;
                n1 = n;  r  = 1. / n1;
                return (n1 + 0.5)*Math.log(n1) - n1 + C0 + r*(C1 + r*r*C3);
            }
        }
        
    }
    
    static PoissonInver poissonInver= new PoissonInver();
    
    private static class PoissonInver {
        double p_L_last=-1;  // previous value of L
        double p_f0;         // cache f0( L )
        /**
         * This subfunction generates a random variate with the poisson
         * distribution using inversion by the chop down method (PIN).
         *
         * Execution time grows with L. Gives overflow for L > 80.
         *
         * The value of bound must be adjusted to the maximal value of L.
         */
        private int PoissonInver(double L, Random random ) {
            
            final int bound = 130;             // safety bound. Must be > L + 8*sqrt(L).
            double r;                          // uniform random number
            double f;                          // function value
            int x;                           // return value
            
            if (L != p_L_last) {               // set up
                p_L_last = L;
                p_f0 = Math.exp(-L);
            }                 // f(0) = probability of x=0
            
            while (true) {
                r = random.nextDouble();  x = 0;  f = p_f0;
                do {                        // recursive calculation: f(x) = f(x-1) * L / x
                    r -= f;
                    if (r <= 0) return x;
                    x++;
                    f *= L;
                    r *= x;}                       // instead of f /= x
                while (x <= bound);
            }
        }
        
    }
    
    static PoissonRatioUniforms poissonRatioUniforms= new PoissonRatioUniforms();
    
    /**
     * This subfunction generates a random variate with the poisson
     * distribution using the ratio-of-uniforms rejection method (PRUAt).
     *
     * Execution time does not depend on L, except that it matters whether L
     * is within the range where ln(n!) is tabulated.
     *
     * Reference: E. Stadlober: "The ratio of uniforms approach for generating
     * discrete random variates". Journal of Computational and Applied Mathematics,
     * vol. 31, no. 1, 1990, pp. 181-189.
     */
    private static class PoissonRatioUniforms {
        private static final double SHAT1 = 2.943035529371538573;    // 8/e
        private static final double SHAT2 = 0.8989161620588987408;   // 3-sqrt(12/e)
        
        private static double p_L_last = -1.0;            // previous L cache tag
        private static double p_a;                       // hat center
        private static double p_h;                       // hat width
        private static double p_g;                       // ln(L)
        private static double p_q;                       // value at mode
        private static int p_bound;                    // upper bound
        private static int mode;                              // mode
        
        private int PoissonRatioUniforms(double L, Random random ) {
            double u;                                // uniform random
            double lf;                               // ln(f(x))
            double x;                                // real sample
            int k;                                 // integer sample
            
            if (p_L_last != L) {
                p_L_last = L;                           // Set-up
                p_a = L + 0.5;                          // hat center
                mode = (int)L;                        // mode
                p_g  = Math.log(L);
                p_q = mode * p_g - fac.lnFac(mode);         // value at mode
                p_h = Math.sqrt(SHAT1 * (L+0.5)) + SHAT2;    // hat width
                p_bound = (int)(p_a + 6.0 * p_h);
            }    // safety-bound
            
            while(true) {
                u = random.nextDouble();
                if (u == 0) continue;                   // avoid division by 0
                x = p_a + p_h * (random.nextDouble() - 0.5) / u;
                if (x < 0 || x >= p_bound) continue;    // reject if outside valid range
                k = (int)(x);
                lf = k * p_g - fac.lnFac(k) - p_q;
                if (lf >= u * (4.0 - u) - 3.0) break;   // quick acceptance
                if (u * (u - lf) > 1.0) continue;       // quick rejection
                if (2.0 * Math.log(u) <= lf) break;}         // final acceptance
            return(k);
        }
    }
    
    /**
     * This function generates a random variate with the poisson distribution.
     *
     * Uses inversion by chop-down method for L &lt; 17, and ratio-of-uniforms
     * method for L &ge; 17.
     *
     * For L &lt; 1.E-6 numerical inaccuracy is avoided by direct calculation.
     * For L &gt; 2E9 too big--throws IllegalArgumentException
     * @param L
     * @param random
     * @return 
     */
    public static int poisson( double L,Random random ) {
        
        
        //------------------------------------------------------------------
        //                 choose method
        //------------------------------------------------------------------
        if (L < 17) {
            if (L < 1.E-6) {
                if (L == 0) return 0;
                if (L < 0) throw new IllegalArgumentException("Parameter negative in poisson function");
                
                //--------------------------------------------------------------
                // calculate probabilities
                //--------------------------------------------------------------
                // For extremely small L we calculate the probabilities of x = 1
                // and x = 2 (ignoring higher x). The reason for using this
                // method is to prevent numerical inaccuracies in other methods.
                //--------------------------------------------------------------
                return PoissonLow(L,random);
            } else {
                
                //--------------------------------------------------------------
                // inversion method
                //--------------------------------------------------------------
                // The computation time for this method grows with L.
                // Gives overflow for L > 80
                //--------------------------------------------------------------
                return poissonInver.PoissonInver(L,random);
            }
        }
        
        else {
            if (L > 2.E9) throw new IllegalArgumentException("Parameter too big in poisson function");
            
            //----------------------------------------------------------------
            // ratio-of-uniforms method
            //----------------------------------------------------------------
            // The computation time for this method does not depend on L.
            // Use where other methods would be slower.
            //----------------------------------------------------------------
            return poissonRatioUniforms.PoissonRatioUniforms(L,random);
        }
    }
    
    /**
     * This subfunction generates a random variate with the poisson
     * distribution for extremely low values of L.
     *
     * The method is a simple calculation of the probabilities of x = 1
     * and x = 2. Higher values are ignored.
     *
     * The reason for using this method is to avoid the numerical inaccuracies
     * in other methods.
     */
    private static int PoissonLow( double L, Random random ) {
        double d, r;
        d = Math.sqrt(L);
        if ( random.nextDouble() >= d ) return 0;
        r = random.nextDouble() * d;
        if (r > L * (1.-L)) return 0;
        if (r > 0.5 * L*L * (1.-L)) return 1;
        return 2;}
    
    
    
}
