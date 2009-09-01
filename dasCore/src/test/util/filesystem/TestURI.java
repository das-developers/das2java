/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.util.filesystem;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;

/**
 *
 * @author jbf
 */
public class TestURI {
    public static void main( String[] args ) throws URISyntaxException, FileSystemOfflineException, IOException {
        URI uri= new URI("http://www.papco.org/data");
        System.err.println(uri.resolve("/foo/data"));

        FileSystem fs= FileSystem.create(uri);
        System.err.println(fs.getLocalRoot());

        System.err.println(fs);

        String[] list= fs.listDirectory("/");
        for ( String s: list ) {
            System.err.println("  "+s);
        }
        
        list= fs.listDirectory("/de/eics/");
        for ( String s: list ) {
            System.err.println("  "+s);
        }
    }
}
