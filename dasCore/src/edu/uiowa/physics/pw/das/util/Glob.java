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

import java.io.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.util.regex.*;

/**
 * known bug: *.java matches ".java". The unix glob behavior is to
 * require that a leading . must be explicitly matched.
 * @author jbf
 */
public class Glob {
    
    /**
     *
     * @param regex
     * @return
     */    
    public static String getParentDirectory( String regex ) {
        String[] s= regex.split( "/" );
        String dirRegex;
        if ( s.length>1 ) {
            dirRegex= s[0];
            for ( int i=1; i<s.length-1; i++ ) {
                dirRegex+= "/"+s[i];
            }
        } else {
            dirRegex= null;
        }
        String fileRegex= s[s.length-1];
        return dirRegex;
    }
    
    /**
     *
     * @param glob
     * @return
     */    
    public static boolean isRoot( String glob ) {        
        File root= new File(glob);
        if ( root.getParentFile()==null ) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     *
     * @param glob
     * @return
     */    
    public static Pattern getPattern( String glob ) {
        final String regex= glob.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");
        final Pattern absPattern= Pattern.compile(regex);        
        return absPattern;
    }
    
    /**
     *
     * @param glob
     * @return
     */    
    public static File[] unGlob( String glob ) {
        return unGlob( glob, false );
    }
    
    private static File[] unGlob( String glob, final boolean directoriesOnly ) {       
        if ( File.separatorChar=='\\' ) glob= glob.replaceAll( "\\\\", "/" );
        String parentGlob= getParentDirectory( glob );
        File[] files;
        if ( parentGlob!=null ) {
            if ( isRoot( parentGlob ) ) {                
                File rootFile= new File(parentGlob);
                if ( rootFile.exists() ) {
                    files= new File[] { rootFile };
                } else {
                    throw new IllegalArgumentException("root does not exist: "+glob);
                }
            } else {
                files= unGlob( parentGlob, true );
            }
            
            
        } else {
            files= new File[] { new File(".") };
        }                
                
        final String regex= glob.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");
        final Pattern absPattern= Pattern.compile(regex);
        List list= new ArrayList();
        for ( int i=0; i<files.length; i++ ) {            
            File[] files1= files[i].listFiles( new FileFilter() {
                public boolean accept( File file ) {
                    String s= file.toString();
                    if ( File.separatorChar=='\\' ) s= s.replaceAll("\\\\", "/");
                    return absPattern.matcher(s).matches() && ( !directoriesOnly || file.isDirectory() ) ;
                }
            } );
            if ( files1==null ) {
                throw new RuntimeException(""); /* files[i] is not a directory */
            }
            list.addAll( Arrays.asList(files1) );
        }        
        return (File[])list.toArray(new File[list.size()]);
        
    }    
    
    
}
