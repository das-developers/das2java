/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.das2.util.filesystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

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
            while ( ic.read(buf) >= 0 ) {
                buf.flip();
                oc.write(buf);
                buf.compact();
            }
            while ( buf.hasRemaining() ) {
                oc.write(buf);
            }
        }
    }
}
