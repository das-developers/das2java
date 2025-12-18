/* File: PropertyType.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on February 23, 2004, 11:26 AM
 *      by Edward E. West <eew@space.physics.uiowa.edu>
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
package org.das2.stream;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import org.das2.datum.Datum;
import org.das2.datum.DatumRange;
import org.das2.datum.DatumRangeUtil;
import org.das2.datum.DatumUtil;
import org.das2.datum.Units;

/**
 *
 * @author eew
 */
public enum PropertyType {

    DOUBLE("double"),
    BOOLEAN("boolean"),
    DOUBLE_ARRAY("doubleArray"),
    DATUM("Datum"),
    DATUM_RANGE("DatumRange"),
    INTEGER("int"),
    STRING("String"),
    TIME("Time"),
    TIME_RANGE("TimeRange"),;

    private static final Map map = new HashMap();

    static {
        for (PropertyType t : values()) {
            map.put(t.name, t);
        }
    }

    public static PropertyType getByName(String name) {
        PropertyType result = (PropertyType) map.get(name);
        if (result == null) {
            throw new IllegalArgumentException("Unrecognized property type: " + name);
        }
        return result;
    }

    private final String name;

    /**
     * Creates a new instance of PropertyType
     */
    private PropertyType(String name) {
        this.name = name;
    }

    public Object parse(String s) throws java.text.ParseException {
        switch (this) {
            case STRING: {
                return s;
            }
            case DOUBLE:
                try {
                    return new Double(s);
                } catch (NumberFormatException nfe) {
                    throw new java.text.ParseException(nfe.getMessage(), 0);
                }
            case BOOLEAN:
                return Boolean.valueOf(s);
            case INTEGER:
                try {
                    return Integer.valueOf(s);
                } catch (NumberFormatException nfe) {
                    throw new java.text.ParseException(nfe.getMessage(), 0);
                }
            case DOUBLE_ARRAY:
                try {
                    String[] strings = s.split(",");
                    double[] doubles = new double[strings.length];
                    for (int i = 0; i < strings.length; i++) {
                        doubles[i] = Double.parseDouble(strings[i]);
                    }
                    return doubles;
                } catch (NumberFormatException nfe) {
                    throw new java.text.ParseException(nfe.getMessage(), 0);
                }
            case DATUM: {
                String[] split = s.split("\\s+");
                if (split.length == 1) {
                    return DatumUtil.parse(split[0]);
                }
                if (split.length == 2) {
                    Units units = Units.lookupUnits(split[1]);
                    return units.parse(split[0]);
                }

                //Allow for units that contain single spaces
                StringBuilder bldr = new StringBuilder();
                for (int i = 1; i < split.length; i++) {
                    if (i > 1) {
                        bldr.append(" ");
                    }
                    bldr.append(split[i]);
                }

                Units units = Units.lookupUnits(bldr.toString());
                return units.parse(split[0]);
            }
            case DATUM_RANGE: {
                try {
                    DatumRange dr= DatumRangeUtil.parseDatumRange(s);
                    return dr;
                } catch ( ParseException ex ) {
                }
                String[] split = s.split("\\s+");
                if (split.length < 3) {
                    throw new IllegalArgumentException("Too few tokens in range: '" + s + "'");
                }
                if (!split[1].toLowerCase().equals("to")) {
                    throw new java.text.ParseException("Range '" + s + "' is missing the word 'to'", 0);
                }

                if (split.length == 3) {
                    Datum begin = Units.dimensionless.parse(split[0]);
                    Datum end = Units.dimensionless.parse(split[2]);
                    return new DatumRange(begin, end);
                }

				// New for 2014-06-12, allow units strings to have spaces, thus
                // "V**2 m**-2 Hz**-1" is legal
                StringBuilder bldr = new StringBuilder();
                for (int i = 3; i < split.length; i++) {
                    if (i > 3) {
                        bldr.append(" ");
                    }
                    bldr.append(split[i]);
                }

                Units units = Units.lookupUnits(bldr.toString());
                Datum begin = units.parse(split[0]);
                Datum end = units.parse(split[2]);
                return new DatumRange(begin, end);
            }
            case TIME: {
                return Units.us2000.parse(s);
            }
            case TIME_RANGE: {
                return DatumRangeUtil.parseTimeRange(s);
            }
            default:
                throw new IllegalStateException("unrecognized name: " + name);
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
