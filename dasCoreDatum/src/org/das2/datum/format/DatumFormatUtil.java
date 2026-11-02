package org.das2.datum.format;

import java.math.BigDecimal ;
import java.math.MathContext;
import java.math.RoundingMode;
import org.das2.datum.Datum;
import org.das2.datum.Units;

/**
 *
 * @author jbf
 */
public class DatumFormatUtil {

    
    /**
     * Round away insignificant floating-point noise when
     * the value is within 0.000001% of a simpler decimal.
     *
     * @param value the original value
     * @return the simplified value
     */
    private static double roundNearDecimal(double value) {
        if (!Double.isFinite(value) || value == 0.0) {
            return value;
        }

        double tolerance = Math.abs(value) * 1.0e-8;

        // Try increasingly precise decimal representations.
        for (int digits = 1; digits <= 15; digits++) {
            BigDecimal rounded = BigDecimal.valueOf(value)
                    .round(new MathContext(digits, RoundingMode.HALF_UP));

            double candidate = rounded.doubleValue();

            if (Math.abs(value - candidate) <= tolerance) {
                return candidate;
            }
        }

        return value;
    }
    /**
     * Return a common formatter for two double values.Preserves their decimal significant digits, padding with 
     * zeros where necessary.
     *
     * @param A first value
     * @param B second value
     * @param u
     * @return a formatter suitable for both values
     */
    public static DatumFormatter bestFormatter( Datum A, Datum B, Units u ) {

        double a= A.doubleValue(u);
        double b= B.doubleValue(u);
        
        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return new FormatStringFormatter("%g", false);
        }

        a= roundNearDecimal(a);
        b= roundNearDecimal(b);
        
        BigDecimal aa = BigDecimal.valueOf(a).stripTrailingZeros();
        BigDecimal bb = BigDecimal.valueOf(b).stripTrailingZeros();

        int significantDigits = Math.max(
                aa.precision(), bb.precision());

        // Number of decimal places needed for fixed notation.
        int decimalPlaces = Math.max(
                Math.max(0, aa.scale()),
                Math.max(0, bb.scale()));

        double max = Math.max(Math.abs(a), Math.abs(b));
        double min = Math.min(Math.abs(a), Math.abs(b));

        // Scientific notation for very large or small numbers,
        // or when the two magnitudes differ substantially.
        boolean scientific = max >= 1.0e6
                || (min > 0.0 && min < 1.0e-4)
                || (min > 0.0 && max / min >= 1.0e4);

        if (scientific) {
            return new FormatStringFormatter(
                    "%." + (significantDigits - 1) + "e", false, true);
        }

        return new FormatStringFormatter(
                "%." + decimalPlaces + "f", false);
    }
    
    private static void doTest(Datum a, Datum b) {
        DatumFormatter df= bestFormatter(a, b, a.getUnits());
        System.out.println( df.format(a,a.getUnits()) + " " + df.format(b,b.getUnits()) );
    }
    
    public static void main(String[] args) { 
        doTest( Datum.create(0.04), Datum.create(900) );
        doTest( Datum.create(-0.9999999999987663), Datum.create(1.9999999999987663) );
        doTest( Datum.create(-0.9999999999987663e-10), Datum.create(1.9999999999987663e-10) );
    }
}
