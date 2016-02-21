/* Copyright (C) 2003-2008 The University of Iowa 
 *
 * This file is part of the Das2 <www.das2.org> utilities library.
 *
 * Das2 utilities are free software: you can redistribute and/or modify them
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Das2 utilities are distributed in the hope that they will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * as well as the GNU General Public License along with Das2 utilities.  If
 * not, see <http://www.gnu.org/licenses/>.
 *
 * HtmlUtil.java
 *
 * Created on May 14, 2004, 9:06 AM
 */

package org.das2.util.filesystem;

import org.das2.util.Base64;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author  Jeremy
 */
public class HtmlUtil {
    
    public static boolean isDirectory( URL url ) {
        String file= url.getFile();
        return file.charAt(file.length()-1) != '/';
    }
    
    public static URL[] getDirectoryListing( URL url ) throws IOException {
        FileSystem.logger.finer("listing "+url);
        
        String file= url.getFile();
        if ( file.charAt(file.length()-1)!='/' ) {
            url= new URL( url.toString()+'/' );
        }
        
        ArrayList urlList= new ArrayList();
        
        long t0= System.currentTimeMillis();
        URLConnection urlConnection = url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        
        int contentLength=10000;
        
        //System.err.println("connected in "+( System.currentTimeMillis() - t0 )+" millis" );
        if ( url.getUserInfo()!=null ) {
            String encode= new String( Base64.encodeBytes(url.getUserInfo().getBytes()) );
            urlConnection.setRequestProperty("Authorization", "Basic " + encode);
        }
        InputStream urlStream = urlConnection.getInputStream();
        
        // search the input stream for links
        // first, read in the entire URL
        byte b[] = new byte[10000];
        int numRead = urlStream.read(b);
        StringBuffer contentBuffer = new StringBuffer( contentLength );
        contentBuffer.append( new String( b, 0, numRead ) );
        while (numRead != -1) {
            FileSystem.logger.finest("download listing");
            numRead = urlStream.read(b);
            if (numRead != -1) {
                String newContent = new String(b, 0, numRead);
                contentBuffer.append( newContent );
            }
        }
        urlStream.close();
        
        // System.err.println("read listing data in "+( System.currentTimeMillis() - t0 )+" millis" );
        String content= contentBuffer.toString();
        
        String hrefRegex= "(?i)href\\s*=\\s*([\"'])(.+?)\\1";
        Pattern hrefPattern= Pattern.compile( hrefRegex );
        
        Matcher matcher= hrefPattern.matcher( content );
        
        while ( matcher.find() ) {
            FileSystem.logger.finest("parse listing");
            String strLink= matcher.group(2);
            URL urlLink= null;
            
            try {
                urlLink = new URL(url, strLink);
                strLink = urlLink.toString();
            } catch (MalformedURLException e) {
                System.err.println("bad URL: "+url+" "+strLink);
                continue;
            }
            
            if ( urlLink.toString().startsWith(url.toString()) && null==urlLink.getQuery() ) {
                urlList.add( urlLink );
            }
        }
        
        return (URL[]) urlList.toArray( new URL[urlList.size()] );
    }
    
}