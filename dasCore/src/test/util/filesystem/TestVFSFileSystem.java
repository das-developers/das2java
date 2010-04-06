/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.util.filesystem;

import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.net.URI;
import org.das2.util.filesystem.FileObject;
import org.das2.util.filesystem.FileSystem;
import org.das2.util.filesystem.VFSFileObject;
import org.das2.util.filesystem.VFSFileSystemFactory;

/**
 *
 * @author jbf
 */
public class TestVFSFileSystem {
    public static void main( String[] args ) {
        try {
            FileSystem.registerFileSystemFactory("sftp", new VFSFileSystemFactory());
            URI uri = new URI("sftp://USER@MY.HOST.NAME:PORT/");  // or USER:PASS
            System.out.println("Scheme: " + uri.getScheme());
            System.out.println("Host: " + uri.getHost());
            System.out.println("Port: " + uri.getPort());
            System.out.println("String: " + uri.toString());
            
            FileSystem fs = FileSystem.create(uri);

//            FileObject fo = fs.getFileObject("/home/ed/elite.xml");
//            System.out.println("Stats for file /home/ed/elite.xml");
//            System.out.println("Modified: " + fo.lastModified());
//            System.out.println("File size: " + fo.getSize());

//            File localFile = fo.getFile();
//            FileReader reader = new FileReader(localFile);
//
//            char[] buf = new char[256];
//            while ( reader.read(buf) >= 0 ) System.out.print(buf);

            VFSFileObject vfo = (VFSFileObject) fs.getFileObject("/home/ed/created_by_vfs.txt");
            vfo.createFile();
            PrintStream os = new PrintStream(vfo.getOutputStream(false));
            os.println("This is a test.  I sure hope it works.");
            os.println(new java.util.Date().toString());
            os.close();
            vfo.close();

        } catch(Exception e) {
            e.printStackTrace();
        }

    }
}
