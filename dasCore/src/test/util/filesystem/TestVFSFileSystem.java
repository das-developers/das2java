/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package test.util.filesystem;

import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import org.das2.util.filesystem.VFSFileObject;
import org.das2.util.filesystem.VFSFileSystem;

/**
 *
 * @author jbf
 */
public class TestVFSFileSystem {
    public static void main( String[] args ) {
        try {
//            FileSystem.registerFileSystemFactory("sftp", new VFSFileSystemFactory());
           // URI uri = new URI("file:///home/jbf/temp/fs/");  // or USER:PASS
            //URI uri= new URI( "sftp://jbf@klunk.physics.uiowa.edu/home/jbf/temp/" );
            //new URI( "ftp://anonymous:hi@127.0.0.1/");
            URI uri= new URI( "ftp://127.0.0.1/temp/" );
            //URI uri= new URI( "ftp://papco:foo@127.0.0.1/" );
            //URI uri = new URI("sftp://jbf@192.168.0.203:/temp");  // or USER:PASS
            
//            System.out.println("Scheme: " + uri.getScheme());
//            System.out.println("Host: " + uri.getHost());
//            System.out.println("Port: " + uri.getPort());
//            System.out.println("String: " + uri.toString());

//            FileSystem fs2= FileSystem.create(uri);
            //VFSFileSystemFactory factory = new VFSFileSystemFactory();
            VFSFileSystem fs = VFSFileSystem.createVFSFileSystem(uri, false);


            for (String s: fs.listDirectory("/")) {
                System.out.println(s);
            }

            System.err.println( fs.getFileObject("foo2.txt").exists() );
            VFSFileObject vfo= (VFSFileObject) fs.getFileObject("foo2.txt");

            OutputStream out= vfo.getOutputStream(false);
            String msg= "Current Time:\n"+ new Date()+"\n";
            out.write( msg.getBytes() );
            out.close();

            fs.close();

//            FileObject fo = fs.getFileObject("/home/ed/elite.xml");
//            System.out.println("Stats for file /home/ed/elite.xml");
//            System.out.println("Modified: " + fo.lastModified());
//            System.out.println("File size: " + fo.getSize());

//            File localFile = fo.getFile();
//            FileReader reader = new FileReader(localFile);
//
//            char[] buf = new char[256];
//            while ( reader.read(buf) >= 0 ) System.out.print(buf);

//            VFSFileObject vfo = (VFSFileObject) fs.getFileObject("/home/ed/created_by_vfs.txt");
//            vfo.createFile();
//            PrintStream os = new PrintStream(vfo.getOutputStream(false));
//            os.println("This is a test.  I sure hope it works.");
//            os.println(new java.util.Date().toString());
//            os.close();
//            vfo.close();
//            ((VFSFileSystem)fs).close();

        } catch(Exception e) {
            e.printStackTrace();
        }

    }
}
