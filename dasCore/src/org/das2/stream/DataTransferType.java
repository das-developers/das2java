/* File: DataTransferType.java
 * Copyright (C) 2002-2003 The University of Iowa
 *
 * Created on December 18, 2003, 9:01 AM
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

package org.das2.stream;

import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.datum.format.TimeDatumFormatter;
import org.das2.util.FixedWidthFormatter;
import org.das2.util.NumberFormatUtil;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.ParseException;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author  Edward West
 */
public class DataTransferType {
    
    private static final int I_SUN_REAL4 = 0;
    private static final int I_SUN_REAL8 = 1;
    private static final int I_LITTLE_ENDIAN_REAL4 = 4;
    private static final int I_LITTLE_ENDIAN_REAL8 = 5;
    private static final int I_ASCII = 2;
    private static final int I_TIME = 3;
    
    private static final Map map = new HashMap();
    
    public static final DataTransferType SUN_REAL4 = new DataTransferType("sun_real4", I_SUN_REAL4, 4, false);
    public static final DataTransferType SUN_REAL8 = new DataTransferType("sun_real8", I_SUN_REAL8, 8, false);

    public static final DataTransferType LITTLE_ENDIAN_REAL4 = new DataTransferType("little_endian_real4", I_LITTLE_ENDIAN_REAL4, 4, false);
    public static final DataTransferType LITTLE_ENDIAN_REAL8 = new DataTransferType("little_endian_real8", I_LITTLE_ENDIAN_REAL8, 8, false);
    
    private static final Pattern ASCII_PATTERN = Pattern.compile("ascii([1-9][0-9]?)");
    private static final Pattern TIME_PATTERN = Pattern.compile("time([1-9][0-9]?)");
    
    private final String name;
    
    private final int sizeBytes;
    
    private final boolean ascii;
    
    private final int id;
    
    private DecimalFormat doubleFormatter;
    
    private DataTransferType(String name, int id, int sizeBytes, boolean ascii) {
        this.name = name;
        this.id = id;
        this.sizeBytes = sizeBytes;
        this.ascii = ascii;
        map.put(name, this);
        if (ascii) {
            doubleFormatter = NumberFormatUtil.getDecimalFormat(getFormat(sizeBytes-1));
        }
    }
    
    static class Time extends DataTransferType {
        private Units units;
        private final TimeDatumFormatter formatter;
        private final int sizeBytes;
        
        Time( int size ) {
            super( "time"+size, I_TIME, size, true );
            this.sizeBytes= size; // yuk
            this.units= Units.us2000;
            this.formatter= TimeDatumFormatter.DEFAULT;
        }
        
        public Units getUnits() {
            return units;
        }
        
        /**
         * https://bugs-pw.physics.uiowa.edu/mantis/view.php?id=478
         * @param units 
         */
        public void resetUnits( Units units ) {
            if ( units!=Units.us2000 ) {
                System.err.println("das2stream units set to something other than Units.us2000: See https://bugs-pw.physics.uiowa.edu/mantis/view.php?id=478" );
            }
            this.units= units;
        }
        
        @Override
        public double read(final java.nio.ByteBuffer buffer) {
            try {
                byte[] bytes = new byte[sizeBytes];
                buffer.get(bytes);
                String str = new String(bytes, "ASCII").trim();
                double result = TimeUtil.create( str ).doubleValue(units);
                return result;
            } catch ( UnsupportedEncodingException e ) {
                throw new RuntimeException(e);
            } catch ( ParseException e ) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public void write(double d, java.nio.ByteBuffer buffer) {
            try {
                String s = formatter.format(units.createDatum(d));
                s= s.substring(0, sizeBytes);
                byte[] bytes = s.getBytes("US-ASCII");
                bytes[sizeBytes-1]= 32; // " " space
                buffer.put(bytes);
            } catch ( UnsupportedEncodingException e ) {
                throw new RuntimeException(e);
            }
        }
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * return the size of the item in bytes.
     * @return the size of the item in bytes.
     */
    public int getSizeBytes() {
        return sizeBytes;
    }
    
    public static DataTransferType getByName(String name) {
        DataTransferType type = (DataTransferType)map.get(name);
        if (type == null ) {
            Matcher m = ASCII_PATTERN.matcher(name);
            if (m.matches()) {
                int charCount = Integer.parseInt(m.group(1));
                type = new DataTransferType(name, I_ASCII, charCount, true);
                map.put(name, type);
            } else {
                m= TIME_PATTERN.matcher(name);
                if ( m.matches()) {
                    int charCount = Integer.parseInt(m.group(1));
                    type = new DataTransferType.Time( charCount );
                    map.put(name, type);
                } else {
                    throw new RuntimeException( "Unsupported type: "+name );
                }
            }
        }
        return type;
    }
    
    /**
     * If type terminates a line, then use \n to delineate
     * @return true if ascii
     */
    public boolean isAscii() {
        return ascii;
    }
    
    private static final java.nio.ByteOrder BIG_ENDIAN = java.nio.ByteOrder.BIG_ENDIAN;
    private static final java.nio.ByteOrder LITTLE_ENDIAN = java.nio.ByteOrder.LITTLE_ENDIAN;
    
    /**
     * read the next value from the buffer and return the double representation.
     * @param buffer the buffer positioned at the next item.
     * @return the double representing.
     */
    public double read(final java.nio.ByteBuffer buffer) {
        final java.nio.ByteOrder bo = buffer.order();
        try {
            double result;
            switch(id) {
                case I_SUN_REAL4: {
                    buffer.order(BIG_ENDIAN);
                    result = buffer.getFloat();
                } break;
                case I_SUN_REAL8: {
                    buffer.order(BIG_ENDIAN);
                    result = buffer.getDouble();
                } break;
                case I_LITTLE_ENDIAN_REAL4: {
                    buffer.order(LITTLE_ENDIAN);
                    result = buffer.getFloat();
                } break;
                case I_LITTLE_ENDIAN_REAL8: {
                    buffer.order(LITTLE_ENDIAN);
                    result = buffer.getDouble();
                } break;
                case I_ASCII: {
                    byte[] bytes = new byte[sizeBytes];
                    buffer.get(bytes);
                    String str = new String(bytes, "ASCII").trim();
                    result = Double.parseDouble(str);
                } break;
                default: {
                    throw new IllegalStateException("Invalid id: " + id);
                }
            }
            return result;
        } catch (java.io.UnsupportedEncodingException uee) {
            //NOT LIKELY TO HAPPEN
            throw new RuntimeException(uee);
        } finally {
            buffer.order(bo);
        }
    }
    
    /**
     * write the next value, represented as a double and interpreted with Units,
     * to the buffer.
     * @param d the double representing.
     * @param buffer the buffer positioned at the next item.
     */    
    public void write(double d, java.nio.ByteBuffer buffer) {
        final java.nio.ByteOrder bo = buffer.order();
        try {
            switch(id) {
                case I_SUN_REAL4: {
                    buffer.order(BIG_ENDIAN);
                    buffer.putFloat((float)d);
                } break;
                case I_SUN_REAL8: {
                    buffer.order(BIG_ENDIAN);
                    buffer.putDouble(d);
                } break;
                case I_LITTLE_ENDIAN_REAL4: {
                    buffer.order(LITTLE_ENDIAN);
                    buffer.putFloat((float)d);
                } break;
                case I_LITTLE_ENDIAN_REAL8: {
                    buffer.order(LITTLE_ENDIAN);
                    buffer.putDouble(d);
                } break;
                case I_ASCII: {
                    String s = doubleFormatter.format(d);
                    if ( s.length()==sizeBytes-2 ) {
                        s= " "+s+" ";  // bug 232
                    } else if ( s.length()==sizeBytes-1 ) {
                        s= s+" ";
                    }
                    if (sizeBytes < 10) {
                        s = FixedWidthFormatter.format(s, sizeBytes - 1);
                    }
                    byte[] bytes = s.getBytes("US-ASCII");
                    buffer.put(bytes);
                } break;
                default: {
                    throw new IllegalStateException("Invalid id: " + id);
                }
            }
        } catch (java.io.UnsupportedEncodingException uee) {
            //US-ASCII encoding should be supported by all JVM implementations.
            throw new RuntimeException(uee);
        } finally {
            buffer.order(bo);
        }
    }
    
    private static String getFormat(int length) {
        if (length < 9) {
            return "0.#";
        } else {
            StringBuilder buffer = new StringBuilder(length);
            buffer.append("+0.");
            for (int i = 0; i < length - 7; i++) {
                buffer.append('0');
            }
            buffer.append("E00;-#");
            return buffer.toString();
        }
    }
    
}
