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

import edu.uiowa.physics.pw.das.datum.Units;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author  eew
 */
public final class PropertyType {
    
    private static final Map map = new HashMap();
    public static final PropertyType DOUBLE = new PropertyType("double");
    public static final PropertyType DOUBLE_ARRAY = new PropertyType("doubleArray");
    public static final PropertyType DATUM = new PropertyType("Datum");
    public static final PropertyType INTEGER = new PropertyType("int");
    
    public static PropertyType getByName(String name) {
        PropertyType result= (PropertyType)map.get(name);
        if ( result==null ) {
            throw new IllegalArgumentException( "Unrecognized property type: "+name );
        }
        return result;
    }
    
    private final String name;
    
    /** Creates a new instance of PropertyType */
    private PropertyType(String name) {
        this.name = name;
        map.put(name, this);
    }
    
    public Object parse(String s) throws java.text.ParseException {
        if (name.equals("double")) {
            try {
                return new Double(s);
            }
            catch (NumberFormatException nfe) {
                throw new java.text.ParseException(nfe.getMessage(), 0);
            }
        }
        else if (name.equals("int")) {
            try {
                return new Integer(s);
            }
            catch (NumberFormatException nfe) {
                throw new java.text.ParseException(nfe.getMessage(), 0);
            }
        }
        else if (name.equals("doubleArray")) {
            try {
                String[] strings = s.split(",");
                double[] doubles = new double[strings.length];
                for (int i = 0; i < strings.length; i++) {
                    doubles[i] = Double.parseDouble(strings[i]);
                }
                return doubles;
            }
            catch (NumberFormatException nfe) {
                throw new java.text.ParseException(nfe.getMessage(), 0);
            }
        }
        else if (name.equals("Datum")) {
            String[] split = s.split("\\s+");
            if (split.length == 1) {
                return Units.dimensionless.parse(split[0]);
            }
            else if (split.length == 2) {
                Units units = Units.getByName(split[1]);
                return units.parse(split[0]);
            }
            else {
                throw new IllegalArgumentException("Too many tokens: '" + s + "'");
            }
        }
        else {
            throw new IllegalStateException("unrecognized name: " + name);
        }
    }
    
    public String toString() {
        return name;
    }
    
}
