/* File: DasPNGConstants.java
 * Copyright (C) 2002-2003 The University of Iowa
 * Created by: Jeremy Faden <jbf@space.physics.uiowa.edu>
 *             Jessica Swanner <jessica@space.physics.uiowa.edu>
 *             Edward E. West <eew@space.physics.uiowa.edu>
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

package org.das2.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author  eew
 */
public abstract class DasPNGConstants {
    
    /*
     * Chunk type constants
     */
    static final String CHUNK_TYPE_IHDR = "IHDR";
    static final String CHUNK_TYPE_PLTE = "PLTE";
    static final String CHUNK_TYPE_IDAT = "IDAT";
    static final String CHUNK_TYPE_IEND = "IEND";
    static final String CHUNK_TYPE_bKGD = "bKGD";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_cHRM = "cHRM";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_gAMA = "gAMA";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_hIST = "hIST";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_pHYs = "pHYs";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_sBIT = "sBIT";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_tEXT = "tEXt";
    static final String CHUNK_TYPE_tIME = "tIME";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_tRNS = "tRNS";  //CURRENTLY UNSUPPORTED
    static final String CHUNK_TYPE_zTXT = "zTXt";  //CURRENTLY UNSUPPORTED

    public static final int DEFAULT_GAMMA = 45000;  //gamma of .45 (multiplied by 100000)

    public static final String KEYWORD_TITLE = "Title";
    public static final String KEYWORD_AUTHOR = "Author";
    public static final String KEYWORD_DESCRIPTION = "Description";
    public static final String KEYWORD_COPYRIGHT = "Copyright";
    public static final String KEYWORD_CREATION_TIME = "Creation Time";
    public static final String KEYWORD_SOFTWARE = "Software";
    public static final String KEYWORD_DISCLAIMER = "Disclaimer";
    public static final String KEYWORD_WARNING = "Warning";
    public static final String KEYWORD_SOURCE = "Source";
    public static final String KEYWORD_COMMENT = "Comment";
    
    /**
     * dasCanvas has a method for generating a JSON string describing the plots,
     * and this is the tag that data should be inserted with.
     */
    public static final String KEYWORD_PLOT_INFO = "plotInfo";
    
    protected HashMap textMap = new HashMap();
    
    protected int gamma = DEFAULT_GAMMA;
    
    DasPNGConstants() {
    }
    
    /** Returns an unmodifiable java.util.List containing the contents of
     * all of the tEXT chunks with the specified keyword.  The return value
     * is guaranteed to be non-null.  If no tEXT chunks with the specified
     * keyword exist, then an empty list is returned.
     * @param keyword the specified keyword
     * @return a java.util.List of the contents of tEXT chunks.
     */
    public List getText(String keyword) {
        List list = (List)textMap.get(keyword);
        if (list == null) {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList(list);
    }
    
    protected static byte[] getISO8859_1Bytes(String header) {
        try {
            return header.getBytes("ISO-8859-1");
        }
        catch (java.io.UnsupportedEncodingException uee) {
            throw new AssertionError(uee);
        }
    }
    
    public int getGamma() {
        return gamma;
    }
    
}
