/*
 * HtmlUtil.java
 *
 * Created on May 14, 2004, 9:06 AM
 */

package edu.uiowa.physics.pw.das.util.webFileSystem;

import java.io.*;
import java.net.*;
import java.util.*;

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
        
        URLConnection urlConnection = url.openConnection();
        
        urlConnection.setAllowUserInteraction(false);
        
        InputStream urlStream = url.openStream();
        
        // search the input stream for links
        // first, read in the entire URL
        byte b[] = new byte[1000];
        int numRead = urlStream.read(b);
        String content = new String(b, 0, numRead);
        while (numRead != -1) {
            numRead = urlStream.read(b);
            if (numRead != -1) {
                String newContent = new String(b, 0, numRead);
                content += newContent;
            }
        }
        urlStream.close();
                
        String lowerCaseContent = content.toLowerCase();
        
        int index = 0;
        while ((index = lowerCaseContent.indexOf("<a", index)) != -1) {
            if ((index = lowerCaseContent.indexOf("href", index)) == -1)
                break;
            if ((index = lowerCaseContent.indexOf("=", index)) == -1)
                break;
            
            index++;
            String remaining = content.substring(index);
            
            StringTokenizer st = new StringTokenizer(remaining, "\t\n\r\">#");
            String strLink = st.nextToken();
            
            URL urlLink=null;
            try {
                urlLink = new URL(url, strLink);
                strLink = urlLink.toString();
            } catch (MalformedURLException e) {
                System.err.println("bad URL: "+urlLink);
                continue;
            }
            
            if ( urlLink.toString().startsWith(url.toString()) && null==urlLink.getQuery() ) {
                 urlList.add( urlLink );
            }           
            
        }
                
        return (URL[]) urlList.toArray( new URL[urlList.size()] );
    }
        
    public static void htmlUtilTest() throws Exception {        
        URL[] urls= HtmlUtil.getDirectoryListing( new URL( "http://www-pw.physics.uiowa.edu/voyager/local1/DATA/" ) );        
        //URL[] urls= HtmlUtil.getDirectoryListing( new URL( "http://www.sarahandjeremy.net/~jbf" ) );
        for ( int i=0; i<urls.length; i++ ) {
            System.out.println(""+urls[i]);
        }
    }
    
    public static void main( String[] args ) throws Exception {
        htmlUtilTest();
    }
}
