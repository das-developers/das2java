/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.zip.GZIPInputStream;
import org.das2.util.DeflaterChannel;

/**
 *
 * @author jbf
 */
public class FileSystemUtil {

    /**
     * Dump the contents of the inputstream into a file.  If the inputStream comes
     * from a file, then java.nio is used to transfer the data quickly.
     * @param in
     * @param f
     * @throws java.io.FileNotFoundException
     * @throws java.io.IOException
     */
    public static void dumpToFile( InputStream in, File f ) throws FileNotFoundException, IOException {
        ReadableByteChannel ic = Channels.newChannel(in);
        FileChannel oc = new FileOutputStream(f).getChannel();
        if ( ic instanceof FileChannel ) {
            FileChannel fic= (FileChannel)ic;
            fic.transferTo(0, fic.size(), oc);
            fic.close();
            oc.close();
        } else {
            ByteBuffer buf= ByteBuffer.allocateDirect( 16*1024 );
            while ( ic.read(buf) >= 0 || buf.position() != 0 ) {
                buf.flip();
                oc.write(buf);
                buf.compact();
            }
        }
    }

    public static void main( String[] args ) throws Exception {
        InputStream in= new ByteArrayInputStream( "Hello there".getBytes() );
        dumpToFile( in, new File( "/home/jbf/text.txt" ) );
    }

    /**
     * un-gzip the file.  This is similar to the unix gunzip command.
     * @param fz zipped input file
     * @param file unzipped destination file
     */
    public static void unzip( File fz, File file) throws IOException {
        
        GZIPInputStream in = new GZIPInputStream(new FileInputStream(fz));

        OutputStream out = new FileOutputStream(file);
    
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
}
