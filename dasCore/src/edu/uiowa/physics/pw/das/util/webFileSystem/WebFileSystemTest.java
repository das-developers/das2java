/*
 * WebFileSystemTest.java
 *
 * Created on May 13, 2004, 1:44 PM
 */

package edu.uiowa.physics.pw.das.util.webFileSystem;

import java.io.*;
import java.net.*;

/**
 *
 * @author  Jeremy
 */
public class WebFileSystemTest {

    private void test1() throws Exception {
        WebFileSystem wfs= WebFileSystem.create( new URL( "http://www-pw.physics.uiowa.edu/voyager/local1/" ) );
        File f= wfs.getFile( "DATA/EVENTS1" );      
        
        System.out.println(f);
        File[] fs= f.listFiles();
        for ( int i=0; i<fs.length; i++ ) {
            System.out.println( "  "+fs[i] );
        }
        BufferedReader r= new BufferedReader( new FileReader( fs[2] ) );
        String s= r.readLine();
        while ( s!=null ) {
            System.out.println( ">>>"+s );
            s= r.readLine();
        }
    }
    
    private void test2() throws Exception {
        WebFileSystem wfs= WebFileSystem.create( new URL( "file:///j:/voyager/DATA" ) );
        File f= wfs.getFile( "DATA/EVENTS1" );      
        
        System.out.println(f);
        File[] fs= f.listFiles();
        for ( int i=0; i<fs.length; i++ ) {
            System.out.println( "  "+fs[i] );
        }        
    }
    
    private WebFileSystemTest() throws Exception {
        test2();
    }
    
    public static void main(String[] args) throws Exception {
        new WebFileSystemTest();
        System.out.println("done");
    }
    
}
