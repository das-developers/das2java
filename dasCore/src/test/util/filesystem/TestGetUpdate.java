/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.util.filesystem;

import java.net.URI;
import org.das2.util.filesystem.FileObject;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.filesystem.FileSystemSettings.Persistence;

/**
 *
 * @author jbf
 */
public class TestGetUpdate {
    public static void main( String[] args ) throws Exception {
        testHttp();
    }

    private static void testHttp() throws Exception {
        FileSystem.settings().setPersistence( Persistence.EXPIRES );
        FileSystem hfs= FileSystem.create( new URI( "http://www-pw.physics.uiowa.edu/~jbf/temp/") );
        String[] ss= hfs.listDirectory("/");
        for ( int i=0; i<ss.length; i++ ) {
            final String file = ss[i];
            System.err.println( "-- "+file+" --");
            final FileObject fo = hfs.getFileObject(file);
            System.err.println( "isLocal: "+ fo.isLocal() );
            System.err.println( "lastModified: "+ fo.lastModified() );
            System.err.println( "file: "+ fo.getFile() );
        }
    }
}
