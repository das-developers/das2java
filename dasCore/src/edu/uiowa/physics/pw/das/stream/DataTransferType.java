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

package edu.uiowa.physics.pw.das.stream;

import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author  Edward West
 */
public final class DataTransferType {
    
    private static final int I_SUN_REAL4 = 0;
    private static final int I_SUN_REAL8 = 1;
    private static final int I_ASCII = 2;
    
    private static final Map map = new HashMap();

    public static final DataTransferType SUN_REAL4 = new DataTransferType("sun_real4", I_SUN_REAL4, 4, false);
    
    public static final DataTransferType SUN_REAL8 = new DataTransferType("sun_real8", I_SUN_REAL8, 8, false);
    
    private static final Pattern ASCII_PATTERN = Pattern.compile("ascii([1-9][0-9]?)");
    
    private final String name;
    
    private final int sizeBytes;
    
    private final boolean ascii;
    
    private final int id;
    
    private DataTransferType(String name, int id, int sizeBytes, boolean ascii) {
        this.name = name;
        this.id = id;
        this.sizeBytes = sizeBytes;
        this.ascii = ascii;
        map.put(name, this);
    }
    
    public String toString() {
        return name;
    }
    
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
            }
        }
        return type;
    }
    
    public boolean isAscii() {
        return ascii;
    }

    private static final java.nio.ByteOrder BIG_ENDIAN = java.nio.ByteOrder.BIG_ENDIAN;
    private static final java.nio.ByteOrder LITTLE_ENDIAN = java.nio.ByteOrder.LITTLE_ENDIAN;
    
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
        }
        catch (java.io.UnsupportedEncodingException uee) {
            //NOT LIKELY TO HAPPEN
            throw new RuntimeException(uee);
        }
        finally {
            buffer.order(bo);
        }
    }
    
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
                case I_ASCII: {
                    if (true) {
                        //I included the 'if (true)' so that the compiler would not
                        //complain that the following break is an unreachable statement.
                        //I did not want to remove the break since there is no guarantee
                        //that I or somebody else would remember to put it back when this
                        //section is implemented.  With out the break this case would
                        //fall through to the next.  We don't want that to happen. -eew
                        throw new UnsupportedOperationException("Cannot output ascii values yet");
                    }
                } break;
                default: {
                    throw new IllegalStateException("Invalid id: " + id);
                }
            }
        }
        finally {
            buffer.order(bo);
        }
    }
    
}
