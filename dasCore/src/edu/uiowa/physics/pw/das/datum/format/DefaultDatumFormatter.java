/* File: DefaultDatumFormatter.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on October 1, 2003, 4:45 PM
 *      by Edward West <eew@space.physics.uiowa.edu>
 *
 * This file is part of the das2 library.
 *
 * das2 is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package edu.uiowa.physics.pw.das.datum.format;

import org.das2.util.NumberFormatUtil;
import org.das2.util.DasMath;
import edu.uiowa.physics.pw.das.datum.*;

import java.text.*;
import java.util.Locale;

/** Formats Datum objects for printing and parses strings to Datum objects.
 *
 * @author  Edward West
 */
public class DefaultDatumFormatter extends DatumFormatter {

    private String formatString;
    private NumberFormat format;

    /** Available for use by subclasses */
    protected DefaultDatumFormatter() {
    }

    /** Creates a new instance of DatumFormatter */
    public DefaultDatumFormatter(String formatString) throws ParseException {
        if (formatString.equals("")) {
            this.formatString = "";
            format = null;
        } else {
            this.formatString = formatString;
            format = NumberFormatUtil.getDecimalFormat(formatString);
        }
    }

    public String format(Datum datum) {
        return format(datum, datum.getUnits()) + " " + datum.getUnits();
    }

    public String format(Datum datum, Units units) {
        double d = datum.doubleValue(units);
        if (Double.isInfinite(d) || Double.isNaN(d)) {
            return "" + d;
        }
        String result;
        if (format == null) {
            double resolution = datum.getResolution(units.getOffsetUnits());
            result = formatLimitedResolution(d, resolution);
        } else {
            result = format.format(datum.doubleValue(units));
        }
        return result;
    }

    @Override
    public String grannyFormat(Datum datum, Units units) {
        String formt = format(datum, units);
        if (formt.indexOf("E") != -1) {
            int iE = formt.indexOf("E");
            StringBuffer granny = new StringBuffer(formt.length() + 4);
            String mant = formt.substring(0, iE);
            if (Double.parseDouble(mant) != 1.0) {
                granny.append(mant).append("\u00d7");
            }
            granny.append("10").append("!A").append(formt.substring(iE + 1)).append("!N");
            formt = granny.toString();
        }
        return formt;
    }

    @Override
    public String[] axisFormat(DatumVector datums, DatumRange context ) {
        Units units= context.getUnits();
        String[] result = new String[datums.getLength()];
        for (int i = 0; i < result.length; i++) {
            result[i] = format(datums.get(i), units);
        }
        boolean hasMant = false;
        for (int i = 0; i < result.length; i++) {
            String res1 = result[i];
            if (res1.indexOf("E") != -1) {
                int iE = res1.indexOf("E");
                String mant = res1.substring(0, iE);
                if (Double.parseDouble(mant) != 1.0) {
                    hasMant = true;
                }
            }
        }
        for (int i = 0; i < result.length; i++) {
            String res1 = result[i];
            if (res1.indexOf("E") != -1) {
                int iE = res1.indexOf("E");
                StringBuffer granny = new StringBuffer(res1.length() + 4);
                String mant = res1.substring(0, iE);

                if (hasMant) {
                    granny.append(mant).append("\u00d7");
                }

                granny.append("10").append("!A").append(res1.substring(iE + 1)).append("!N");
                result[i] = granny.toString();
            }

        }
        return result;
    }

    public String grannyFormat(Datum datum) {
        return grannyFormat(datum, datum.getUnits()) + " " + datum.getUnits();
    }

    public String toString() {
        return formatString;
    }

    private String formatLimitedResolution(double d, double resolution) {
        String result;
        if (resolution == 0. && Double.toString(d).length() > 7) {
            // make the default resolution be 0.01%.
            resolution = d / 10000;
        }
        if (resolution > 0) {
            // 28 -->   scale = -1
            // 2.8 -->  scale = 0
            int scale = (int) Math.ceil(-1 * DasMath.log10(resolution) - 0.00001);
            int exp;
            if (d != 0.) {
                exp = (int) DasMath.log10(Math.abs(d));
            } else {
                exp = 0;
            }
            if (scale >= 0) {
                DecimalFormat f;
                if (exp <= -5 || exp >= 5) {
                    f = NumberFormatUtil.getDecimalFormat("0E0");
                    f.setMinimumFractionDigits(scale + exp - 1);
                    f.setMaximumFractionDigits(scale + exp - 1);
                } else {
                    f = NumberFormatUtil.getDecimalFormat("0");
                    f.setMinimumFractionDigits(scale);
                    f.setMaximumFractionDigits(scale);
                }
                result = f.format(d);
            } else {
                double round = DasMath.exp10(-1 * scale);
                d = Math.round(d / round) * round;
                DecimalFormat f;
                if (exp <= -5 || exp >= 5) {
                    f = NumberFormatUtil.getDecimalFormat("0E0");
                    f.setMinimumFractionDigits(scale + exp + 1);
                    f.setMaximumFractionDigits(scale + exp + 1);
                } else {
                    f = NumberFormatUtil.getDecimalFormat("0");
                }
                result = f.format(d);
            }
        } else {
            result = Double.toString(d);
        }
        return result;
    }
}
