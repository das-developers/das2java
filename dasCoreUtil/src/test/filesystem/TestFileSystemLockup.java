/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.filesystem.FileSystem.FileSystemOfflineException;

/**
 *
 * @author jbf
 */
public class TestFileSystemLockup {

    public static void main( String[] args ) {
        final long t0= System.currentTimeMillis();

        System.err.println("The old filesystem would only allow one FS to be created at a time, possibly locking out others.");
        System.err.println("hard-coding delays in create simulates the problems seen.");
        System.err.println("hard-coding delays in getFile shows things are okay if done later.");

        for ( int i=0; i<7; i++ ) {
            final int fi= i;
            Runnable run= new Runnable() {
                public void run() {
                    try {
                        FileSystem fs;
                        if ( fi>2 ) {
                            fs= FileSystem.create("file:/home/jbf/temp/fstest/fs3");
                        } else {
                            fs= FileSystem.create("file:/home/jbf/temp/fstest/fs" + fi);
                        }
                        long t1= System.currentTimeMillis();
                        File ff= fs.getFileObject("afile").getFile();
                        long ts0= System.currentTimeMillis() - t0;
                        long ts1= System.currentTimeMillis() - t1;
                        System.err.printf( "%6.3f %6.3f %s %d\n", ts0/1000., ts1/1000., ff, fs.hashCode() );
                    } catch (FileNotFoundException ex) {
                        Logger.getLogger(TestFileSystemLockup.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (FileSystemOfflineException ex) {
                        Logger.getLogger(TestFileSystemLockup.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(TestFileSystemLockup.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (IOException ex) {
                        Logger.getLogger(TestFileSystemLockup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            };
            new Thread(run).start();

        }
    }
}
