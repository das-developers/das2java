/* File: Glob.java
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

package edu.uiowa.physics.pw.das.util;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author  jbf
 */
public class Glob {
    
    /** Creates a new instance of Glob */
    private Glob() {
    }
    
    public static File[] unGlob( String pattern ) {
        String[] s= pattern.split(File.separator);
        String filePattern= s[s.length-1];        
        File dir= new File(pattern.substring(0,pattern.length()-filePattern.length()-File.separator.length()));
        final String regex= filePattern.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");        
        FilenameFilter filter= new FilenameFilter() {
            public boolean accept( File dir, String filename ) {
                return filename.matches(regex);
            }
        };
        return dir.listFiles(filter);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        File[] files= unGlob("/home/jbf/*.dat");
        for ( int i=0; i<files.length; i++ ) System.out.println(files[i]);
        files= unGlob("/home/jbf/?.???");
        for ( int i=0; i<files.length; i++ ) System.out.println(files[i]);
    }
    
}
