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

package org.das2.util.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import javax.swing.filechooser.FileFilter;

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
    private static String getParentDirectory( String regex ) {
        String[] s= regex.split( "/" );
        StringBuilder dirRegex;
        if ( s.length>1 ) {
            dirRegex= new StringBuilder(s[0]);
            for ( int i=1; i<s.length-1; i++ ) {
                dirRegex.append( "/") .append( s[i] );
            }
            return dirRegex.toString();
        } else {
            return s[0];
        }
    }
    
    /**
     *
     * @param glob
     * @return
     */
    private static boolean isRoot( String glob ) {
        File root= new File(glob);
        if ( root.getParentFile()==null ) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * converts a glob into a Pattern.
     * @param glob a string like '*.dat'
     * @return a Pattern for the glob, for example *.dat -> .*\.dat
     */
    public static Pattern getPattern( String glob ) {
        final String regex= getRegex( glob );
        final Pattern absPattern= Pattern.compile(regex);
        return absPattern;
    }
    
    /**
     * converts a glob into a regex.
     * @param glob, like 'foo*.dat'
     * @return the regular expression that implements, like 'foo.*\.dat'
     */
    public static String getRegex( String glob ) {
        StringTokenizer tk= new StringTokenizer(glob,"*?.+\\", true);
        StringBuilder result= new StringBuilder();
        while ( tk.hasMoreElements() ) {
            String nt= tk.nextToken();
            if ( nt.equals("*") ) {
                result.append(".*");
            } else if ( nt.equals("?") ) {
                result.append(".");
            } else if ( nt.equals(".") ) {
                result.append("\\.");
            } else if ( nt.equals("+") ) {
                result.append("\\+");
            } else if ( nt.equals("\\") ) {
                result.append("\\\\");
            } else {
                result.append(nt);
            }
        }
        return result.toString();
        //return glob.replaceAll("\\.","\\\\.").replaceAll("\\*","\\.\\*").replaceAll("\\?","\\.");
    }
    
    /**
     * converts regex into a glob, as best it can.
     * @param regex regular expression like "foo.*\\.dat"
     * @return a glob like "foo*.dat"
     */
    public static String getGlobFromRegex( String regex ) {
        String glob= regex;
        glob= glob.replaceAll("\\.\\*","*");
        glob= glob.replaceAll("\\\\.",".");
        return glob;
    }
    
    /**
     * unglob the glob into an array of the matching FileObjects.
     * See sftp://papco.org/home/jbf/ct/autoplot/script/demos/filesystem/unGlobDemo.jy
     * @param fs the filesystem
     * @param glob the glob, such as '/*.gif'
     * @return an array of FileObjects that match the glob.
     * @throws java.io.IOException
     */
    public static FileObject[] unGlob( FileSystem fs, String glob ) throws IOException {
        return unGlob( fs, glob, false );
    }
    
    /**
     * unglob the glob into an array of the matching FileObjects.
     * @param fs the filesystem
     * @param glob the glob, which must start with /.
     * @param directoriesOnly
     * @return
     * @throws IOException 
     */
    private static FileObject[] unGlob( FileSystem fs, String glob, final boolean directoriesOnly ) throws IOException {
        if ( File.separatorChar=='\\' ) glob= glob.replaceAll( "\\\\", "/" );
        String parentGlob= getParentDirectory( glob );
        FileObject[] files;
        if ( parentGlob!=null ) {
            if ( isRoot( parentGlob ) ) {
                FileObject rootFile= fs.getFileObject(parentGlob);
                if ( rootFile.exists() ) {
                    files= new FileObject[] { rootFile };
                } else {
                    throw new IllegalArgumentException("root does not exist: "+glob);
                }
            } else {
                files= unGlob( fs, parentGlob, true );
            }
        } else {
            throw new IllegalArgumentException("absolute files only");
        }
        
        final String regex= getRegex( glob );
        final Pattern absPattern= Pattern.compile(regex);
        List list= new ArrayList();
        for (FileObject file1 : files) {
            FileObject[] files1 = ((FileObject) file1).getChildren();
            for (FileObject file : files1) {
                if ( file!=null ) { // TODO: there are nulls when you list /tmp, why?
                    String s= file.getNameExt();
                    if ( absPattern.matcher(s).matches() && ( !directoriesOnly || file.isFolder() ) ) {
                        list.add( file );
                    }
                }
            }
        }
        return (FileObject[])list.toArray(new FileObject[list.size()]);
    }
    
    public static FileFilter getGlobFileFilter( final String glob ) {
        final Pattern pattern= getPattern(glob);
        FileFilter f;
        return new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pattern.matcher( pathname.getName() ).matches( );
            }
            @Override
            public String getDescription() {
                return glob;
            }
        };
    }
    
}
