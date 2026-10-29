package org.das2.datum.format;

import java.math.BigDecimal ;

/**
 *
 * @author jbf
 */
public class DatumFormatUtil {

    /**
     * Return a common formatter for two double values. Preserves their decimal significant digits, padding with zeros where
     * necessary.
     *
     * @param a first value
     * @param b second value
     * @return a formatter suitable for both values
     */
    public static DatumFormatter bestFormatter(double a, double b) {

        if (!Double.isFinite(a) || !Double.isFinite(b)) {
            return new FormatStringFormatter("%g", false);
        }

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
                    "%." + (significantDigits - 1) + "e", false);
        }

        return new FormatStringFormatter(
                "%." + decimalPlaces + "f", false);
    }
}
