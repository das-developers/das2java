/*
 * WebFile.java
 *
 * Created on May 14, 2004, 10:06 AM
 */

package edu.uiowa.physics.pw.das.util.webFileSystem;

import java.io.*;
import java.net.*;

/**
 *
 * @author  Jeremy
 */
public class WebFile extends File {    
    HttpWebFileSystem wfs;
    String pathname;
    
    public String[] list() {
        File[] list= listFiles();
        String result[]= new String[list.length];
        for ( int i=0; i<list.length; i++ ) {
            result[i]= list[i].toString();
        }
        return result;
    }
    
    public boolean canWrite() {
        return false;
    }
            
    public File[] listFiles() {
        try {
            File[] retValue;
            URL[] list= HtmlUtil.getDirectoryListing( new URL( wfs.getRoot().toString()+pathname ) );
            if ( list.length>100 ) {
                throw new IllegalStateException( "URL list is very long, refusing to transfer" );
            }
            File[] result= new File[list.length];
            for ( int i=0; i<list.length; i++ ) {
                URL url= list[i];
                String localName= wfs.getLocalName(url);
                result[i]= new WebFile( wfs, localName );                
            }
            return result;       
        } catch ( MalformedURLException e ) {
            wfs.handleException(e);
            return new File[0];
        } catch ( IOException e ) {
            wfs.handleException(e);
            return new File[0];
        }
    }
    
    protected WebFile( HttpWebFileSystem wfs, String pathname ) {
        super( wfs.getLocalRoot(), pathname );
        this.wfs= wfs;
        this.pathname= pathname;
        if ( !canRead() ) {
            if ( wfs.isDirectory( pathname ) ) {
                mkdirs();
            } else {
                try {
                    wfs.transferFile(pathname,this);
                } catch ( IOException e ) {
                    wfs.handleException(e);
                }
            }
        }
    }
    
    
}
