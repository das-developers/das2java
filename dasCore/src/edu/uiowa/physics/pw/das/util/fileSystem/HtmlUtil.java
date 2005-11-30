/*
 * HtmlUtil.java
 *
 * Created on May 14, 2004, 9:06 AM
 */

package edu.uiowa.physics.pw.das.util.fileSystem;

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
        
        InputStream urlStream = url.openStream();
        
        // search the input stream for links
        // first, read in the entire URL
        byte b[] = new byte[10000];
        int numRead = urlStream.read(b);
        StringBuffer contentBuffer = new StringBuffer( contentLength ); 
        contentBuffer.append( new String( b, 0, numRead ) );
        while (numRead != -1) {
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
