/* File: crypt.java
 * Copyright (C) 2002-2003 University of Iowa
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

package edu.uiowa.physics.pw.das.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author  jbf
 */
public class crypt {
    
    /** Creates a new instance of crypt */
    public crypt() {
    }
    
    public static String crypt(java.lang.String s) {
        return JCrypt.crypt("do",s);
    }
    
    public static void main(String args[]) {
        if(args.length >= 1) {
            edu.uiowa.physics.pw.das.util.DasDie.println
            (
             "[" + args[0] + "] => [" +
            crypt.crypt(args[0]) + "]"
            );
        } else {
            edu.uiowa.physics.pw.das.util.DasDie.println("java crypt <clear_password>");
        }
        
        try {
            MessageDigest g= MessageDigest.getInstance("MD5");
            edu.uiowa.physics.pw.das.util.DasDie.println(g.digest(args[0].getBytes()).toString());
        } catch ( NoSuchAlgorithmException e ) {
            edu.uiowa.physics.pw.das.util.DasDie.println(e);
        }
        
    }
}
