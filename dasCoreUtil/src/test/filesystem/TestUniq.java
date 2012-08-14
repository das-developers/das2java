/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.filesystem;

import java.net.UnknownHostException;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;

/**
 * This should demo that the same object is returned.
 * @author jbf
 */
public class TestUniq {
    public static void main( String[] args ) throws FileSystemOfflineException, UnknownHostException {
        FileSystem fs1= FileSystem.create("http://sarahandjeremy.net/~jbf/1wire/data/2012");
        FileSystem fs2= FileSystem.create("http://sarahandjeremy.net/~jbf/1wire/data/2012/");

        System.err.println( fs1==fs2 );

    }
}
