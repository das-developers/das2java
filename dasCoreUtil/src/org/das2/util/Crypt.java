/* File: crypt.java
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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author  jbf
 */
public class Crypt {        
    
    /** Creates a new instance of crypt */
    private Crypt() {       
    }
    
    public static String crypt(java.lang.String s) {
        return JCrypt.crypt("do", s);
//        try {            
//            return new String(MessageDigest.getInstance("MD5").digest(s.getBytes()));
//        } catch ( NoSuchAlgorithmException ex ) {
//            RuntimeException e= new IllegalStateException( "MD5 algorythm not available" );
//            e.initCause(ex);
//            throw e;
//        }
    }
    
    public static void main(String args[]) throws Exception {
        String arg;
        if(args.length >= 1) {
            arg= args[0];
        } else {
            arg= "ask1st";
            Logger.getLogger("das2.anon").finest("java crypt <clear_password>");
            Logger.getLogger("das2.anon").log(Level.FINEST, "  using {0}", arg);
        }
        System.out.println(
        "[" + arg + "] => [" +
        Crypt.crypt(arg) + "]"
        );
        
        //byte[] bytes= MessageDigest.getInstance("MD5").digest(arg.getBytes());
        //String[] hex= new String[] { "0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f" };
        //for ( int i=0; i<bytes.length; i++ ) {
        //    System.out.println("" + hex[( bytes[i]&0xF0 )>>4] + hex[( (bytes[i]&0x0F) )] + " ");
        //}
        
    }
}
